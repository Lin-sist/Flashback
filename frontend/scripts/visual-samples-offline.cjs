// 固定合成状态、零网络；执行真实 SFC 脚本及记录服务，不代替微信渲染。
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse } = require('vue/compiler-sfc')
const { compile } = require(require.resolve('@vue/compiler-dom', { paths: [path.dirname(require.resolve('vue'))] }))
const root = path.resolve(__dirname, '..')
const src = path.join(root, 'src')
let passed = 0

function harness() {
  const storage = new Map([['flashback:token', 'synthetic-offline-token']])
  const requests = [], toasts = [], navigation = [], timers = [], hooks = {}, actionSheets = [], modals = []
  let response = () => ({ code: 0, data: {} })
  const uni = {
    getStorageSync: key => storage.get(key),
    setStorageSync: (key, value) => storage.set(key, value),
    removeStorageSync: key => storage.delete(key),
    showToast: x => toasts.push(x.title),
    reLaunch: x => navigation.push(x.url),
    navigateTo: x => navigation.push(x.url),
    navigateBack: () => navigation.push('back'),
    switchTab: x => navigation.push(x.url),
    hideTabBar: () => {},
    showActionSheet: options => { actionSheets.push(options) },
    showModal: options => { modals.push(options) },
    getRecorderManager: () => ({ onStart() {}, onStop() {}, onError() {} }),
    request(options) {
      requests.push({ url: options.url, method: options.method })
      const result = response(options)
      if (result instanceof Error) options.fail({ errMsg: result.message })
      else options.success({ statusCode: 200, data: result })
    },
  }
  const cache = new Map()
  function moduleFrom(file) {
    if (cache.has(file)) return cache.get(file).exports
    const mod = { exports: {} }
    cache.set(file, mod)
    const source = fs.readFileSync(file, 'utf8').replaceAll('import.meta.env', '__env')
    const js = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 } }).outputText
    const localRequire = spec => {
      if (!spec.startsWith('.')) return require(spec)
      const target = path.resolve(path.dirname(file), spec)
      if (target.endsWith('.vue')) return { default: {} }
      const resolved = [target + '.ts', path.join(target, 'index.ts')].find(fs.existsSync)
      if (!resolved) throw new Error('Unresolved local module: ' + spec)
      return moduleFrom(resolved)
    }
    vm.runInNewContext(js, { module: mod, exports: mod.exports, require: localRequire, uni, Error, __env: { VITE_PREVIEW_MODE: 'true' } }, { filename: path.relative(root, file) })
    return mod.exports
  }
  const service = moduleFrom(path.join(src, 'services/recordService.ts')).recordService
  const ownershipService = moduleFrom(path.join(src, 'services/dataOwnershipService.ts')).dataOwnershipService
  const chapterService = moduleFrom(path.join(src, 'services/timeChapterService.ts')).timeChapterService
  const preview = moduleFrom(path.join(src, 'features/preview/preview-session.ts'))
  const mutation = moduleFrom(path.join(src, 'features/data-ownership/mutation-state.ts'))
  const utils = moduleFrom(path.join(src, 'utils/index.ts'))
  function page(name, services = {}, store = {}) {
    const file = path.join(src, 'pages', name, 'index.vue')
    const descriptor = parse(fs.readFileSync(file, 'utf8')).descriptor
    const names = [], ast = ts.createSourceFile(file, descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
    const collect = node => { if (ts.isIdentifier(node)) names.push(node.text); else for (const element of node.elements) if (element.name) collect(element.name) }
    for (const statement of ast.statements) {
      if (ts.isVariableStatement(statement)) for (const declaration of statement.declarationList.declarations) collect(declaration.name)
      if (ts.isImportDeclaration(statement) && statement.importClause && !statement.importClause.isTypeOnly) {
        const clause = statement.importClause
        if (clause.name) names.push(clause.name.text)
        if (clause.namedBindings && ts.isNamedImports(clause.namedBindings)) for (const element of clause.namedBindings.elements) if (!element.isTypeOnly) names.push(element.name.text)
      }
    }
    const exports = names.join(',')
    const source = descriptor.scriptSetup.content.replaceAll('import.meta.env', '__env') + '\nglobalThis.surface = {' + exports + '}\n'
    const js = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 } }).outputText
    const deps = spec => {
      if (spec === 'vue') return { ...vue, onUnmounted() {} }
      if (spec === '@dcloudio/uni-app') return Object.fromEntries(['onShow', 'onHide', 'onLoad', 'onUnload'].map(key => [key, fn => { hooks[key] = fn }]))
      if (spec.endsWith('.vue')) return { default: {} }
      if (spec === '../../services' || spec === '../../../services') return { recordService: service, dataOwnershipService: ownershipService, timeChapterService: chapterService, tagService: { getTags: async () => [] }, ...services }
      if (spec === '../../stores') return { useRecordStore: () => store, useUserStore: () => store, useTimeChapterStore: () => store, useTagStore: () => ({}), useAgentChatStore: () => ({}) }
      if (spec === '../../types' || spec === '../../../types') return moduleFrom(path.join(src, 'types/index.ts'))
      if (spec === '../../utils' || spec === '../../../utils') return utils
      if (spec.endsWith('preview-session')) return preview
      if (spec.endsWith('useRecordCoverUrls')) return { useRecordCoverUrls: () => ({ coverUrls: {}, coverErrors: {}, loadCovers: async () => {}, markCoverFailed() {} }) }
      throw new Error('Unexpected page dependency: ' + spec)
    }
    const sandbox = { exports: {}, require: deps, uni, Error, __env: {}, setInterval: () => 0, clearInterval() {}, setTimeout: (fn, delay) => { timers.push({ fn, delay }); return timers.length }, clearTimeout() {} }
    vm.runInNewContext(js, sandbox, { filename: name + '/index.vue' })
    const scope = sandbox.surface
    const render = new Function('Vue', compile(descriptor.template.content, { mode: 'function', isCustomElement: tag => !/^[A-Z]/.test(tag) }).code)(vue)
    const values = vue.proxyRefs({ ...scope })
    const ctx = new Proxy(values, { has: (_, key) => key !== '_Vue' && key !== '_cache', get: (target, key) => key === Symbol.unscopables ? undefined : target[key] })
    scope.$nodes = () => {
      const nodes = []
      function walk(node) { if (!node || typeof node !== 'object') return; nodes.push(node); if (Array.isArray(node.children)) node.children.forEach(walk) }
      walk(render(ctx, [])); return nodes
    }
    scope.$select = name => scope.$nodes().filter(node => vue.normalizeClass(node.props?.class).split(' ').includes(name))
    return scope
  }
  return { page, service, ownershipService, chapterService, uni, mutation, storage, requests, toasts, navigation, timers, actionSheets, modals, hooks, setResponse: fn => { response = fn } }
}

const deferred = () => {
  let resolve, reject
  const promise = new Promise((a, b) => { resolve = a; reject = b })
  return { promise, resolve, reject }
}
async function check(name, fn) {
  await fn()
  passed++
  console.log('PASS ' + name)
}
const item = (id, status = 'SAVED') => ({ id, status, title: '固定合成标题', createdAt: '2026-10-01T12:00:00', recordType: 'MOMENT', tagNames: [] })
const timelineResult = (ids, pageNum = 1, hasMore = false) => ({ groups: [{ yearMonth: '2026-10', items: ids.map(id => item(id)) }], total: 3, pageNum, hasMore })

async function main() {
  await check('首页加载成功、空态与独立失败', async () => {
    const h = harness()
    const p = h.page('home', { recordService: {
      getRecordList: status => status === 'SAVED' ? Promise.resolve({ total: 1, list: [item(1)] }) : Promise.reject(new Error('synthetic failure')),
      getUnlockedRecords: async () => ({ total: 0, list: [] }),
    } })
    await p.loadHomeSummary()
    assert.equal(p.draftState.value, 'ready')
    assert.equal(p.draftCount.value, 1)
    assert.equal(p.sealedState.value, 'error')
    assert.equal(p.showArrivalCard.value, false)
    assert.equal(p.unlockedState.value, 'ready')
    p.goDraftEntry()
    assert.match(h.navigation[0], /record-editor\/index\?id=1/)
  })
  await check('首页等待服务响应时保留加载态', async () => {
    const h = harness(), wait = deferred()
    const p = h.page('home', { recordService: { getRecordList: () => wait.promise, getUnlockedRecords: () => wait.promise } })
    const work = p.loadHomeSummary()
    assert.equal(p.draftState.value, 'loading')
    assert.equal(p.sealedState.value, 'loading')
    wait.resolve({ total: 0, list: [] })
    await work
    assert.equal(p.draftState.value, 'ready')
    assert.equal(p.latestDraft.value, null)
  })
  await check('时光轴分页去重、失败保留与重试', async () => {
    const h = harness()
    let result = timelineResult([1, 2], 1, true)
    const p = h.page('timeline', { recordService: { getTimeline: async () => { if (result instanceof Error) throw result; return result } } })
    await p.loadTimeline()
    result = new Error('synthetic page failure')
    assert.equal(await p.loadTimeline({ append: true }), false)
    assert.equal(p.loadMoreFailed.value, true)
    assert.equal(p.currentPage.value, 1)
    assert.equal(p.timelineGroups.value[0].items.length, 2)
    result = timelineResult([2, 3], 2)
    await p.loadTimeline({ append: true })
    assert.deepEqual(Array.from(p.timelineGroups.value[0].items, x => x.id), [1, 2, 3])
    assert.equal(p.loadMoreFailed.value, false)
    assert.equal(p.hasMore.value, false)
  })
  await check('筛选失败保留原条件与记录', async () => {
    const h = harness()
    let failed = false
    const p = h.page('timeline', { recordService: { getTimeline: async () => { if (failed) throw new Error('synthetic filter failure'); return timelineResult([1]) } } })
    await p.loadTimeline()
    p.draftTagId.value = 7
    p.filterPanelVisible.value = true
    failed = true
    await p.applyFilters()
    assert.equal(p.filterApplyFailed.value, true)
    assert.equal(p.filterPanelVisible.value, true)
    assert.equal(p.appliedFilters.value.tagId, undefined)
    assert.equal(p.timelineGroups.value[0].items[0].id, 1)
  })
  await check('旧时光轴响应不能覆盖新条件结果', async () => {
    const h = harness(), old = deferred(), latest = deferred()
    let count = 0
    const p = h.page('timeline', { recordService: { getTimeline: () => ++count === 1 ? old.promise : latest.promise } })
    const a = p.loadTimeline(), b = p.loadTimeline({ filters: { tagId: 7 } })
    latest.resolve(timelineResult([2])); await b
    old.resolve(timelineResult([1])); await a
    assert.equal(p.timelineGroups.value[0].items[0].id, 2)
    assert.equal(p.appliedFilters.value.tagId, 7)
  })
  await check('节点保留状态与详情导航', async () => {
    const h = harness()
    const p = h.page('timeline', { recordService: { getTimeline: async () => ({ ...timelineResult([]), groups: [{ yearMonth: '2026-10', items: [item(1), item(2, 'SEALED'), item(3, 'UNLOCKED')] }] }) } })
    await p.loadTimeline()
    assert.deepEqual(Array.from(p.decoratedGroups.value[0].items, x => x.kind), ['saved', 'sealed', 'unlocked'])
    p.openNode(item(2, 'SEALED'))
    assert.match(h.navigation[0], /record-detail\/index\?id=2/)
  })
  await check('轻纸片分页仍合并同月，仅跨年显示年份，不重排或增加请求', async () => {
    const h = harness()
    let calls = 0
    const p = h.page('timeline', { recordService: { getTimeline: async () => {
      calls++
      return calls === 1 ? timelineResult([1], 1, true) : {
        ...timelineResult([], 2), total: 4, groups: [
          { yearMonth: '2026-10', items: [item(1), item(2)] },
          { yearMonth: '2026-09', items: [{ ...item(3), createdAt: '2026-09-05T09:08:00' }] },
          { yearMonth: '2025-12', items: [{ ...item(4), createdAt: '2025-12-31T22:10:00' }] },
        ],
      }
    } } })
    await p.loadTimeline(); await p.loadTimeline({ append: true })
    assert.equal(calls, 2)
    assert.deepEqual(Array.from(p.decoratedGroups.value, x => [x.yearText, x.showYear, x.items.length]), [
      ['2026', true, 2], ['2026', false, 1], ['2025', true, 1],
    ])
    assert.equal(p.$select('year-heading').length, 2)
    assert.equal(p.$select('tl-item').length, 4)
    assert.deepEqual(Array.from(p.$select('entry-date'), x => x.children), ['10月1日 · 12:00', '10月1日 · 12:00', '9月5日 · 09:08', '12月31日 · 22:10'])
    assert.equal(p.$select('tl-item--last').length, 1)
    assert.equal(h.requests.length, 0)
  })
  await check('四态保持原入口，无正文接口不伪造摘句/临近抵达，日期缺失不补今天', async () => {
    const h = harness()
    const records = ['SAVED', 'SEALED', 'UNLOCKED', 'DRAFT'].map((status, i) => ({
      ...item(i + 1, status), title: '固定合成长标题'.repeat(12),
      lifeNodeLabel: '固定合成节点', tagNames: ['固定标签', '固定标签'],
    }))
    const p = h.page('timeline', { recordService: { getTimeline: async () => ({
      ...timelineResult([]), groups: [{ yearMonth: '2026-10', items: records }],
    }) } })
    await p.loadTimeline()
    assert.deepEqual(Array.from(p.$select('entry-status'), x => x.children), ['封存中', '已抵达', '草稿'])
    assert.equal(p.$select('card-footer-tag').length, 0)
    assert.equal(p.$select('seal').length, 0)
    assert.equal(p.$select('entry-meta').length, 4)
    assert.equal(p.$select('card').length, 4)
    assert.match(p.$select('tl-item')[0].props['aria-label'], /已留下/)
    assert.ok(p.$select('card-title').every(x => x.children === records[0].title))
    assert.ok(p.$select('entry-context').every(x => x.children === '固定合成节点 · 固定标签'))
    assert.equal(p.$select('card-photo').length, 0)
    for (const node of p.$select('tl-item')) node.props.onTap()
    assert.deepEqual(h.navigation, [1, 2, 3, 4].map(id => `/pages/${id === 4 ? 'record-editor' : 'record-detail'}/index?id=${id}&source=timeline`))
    assert.equal(p.decorateYear('2026-99'), '日期待确认')
    const invalid = p.decorateItem({ ...item(9), createdAt: 'invalid', title: '  ', lifeNodeLabel: undefined, tagNames: [] })
    assert.equal(invalid.dateLabel, '日期待确认'); assert.equal(invalid.contextText, '')
    assert.equal(invalid.title, '未命名片段'); assert.equal(h.requests.length, 0)
  })
  await check('照片仅沿原cover URL展示，错误有占位且无图记录保持纯文字', async () => {
    const h = harness()
    const p = h.page('timeline', { recordService: { getTimeline: async () => ({
      ...timelineResult([]), groups: [{ yearMonth: '2026-10', items: [
        item(1), { ...item(2), cover: { id: 22 } }, { ...item(3, 'SEALED'), cover: { id: 33 } },
      ] }],
    }) } })
    await p.loadTimeline()
    p.coverUrls[2] = '/synthetic-cover.svg'
    p.coverErrors[3] = '固定合成错误'
    assert.equal(p.$select('card-photo').length, 2)
    assert.equal(p.$select('card-cover-image').length, 1)
    assert.equal(p.$select('card-cover-image')[0].props.src, '/synthetic-cover.svg')
    assert.equal(p.$select('card-img-error')[0].children, '封面暂不可用')
    assert.equal(p.$select('card').length, 3)
    delete p.coverUrls[2]
    assert.equal(p.$select('card-cover-image').length, 0)
    assert.equal(p.$select('card-img-fallback').length, 2)
    assert.equal(h.requests.length, 0)
  })
  await check('跨月稀疏短文字不依赖封面或月标题，整条仍可进入原记录', async () => {
    const h = harness()
    let calls = 0
    const titles = ['树影', '雨停以后', '周末']
    const p = h.page('timeline', { recordService: { getTimeline: async () => {
      calls++
      return { ...timelineResult([]), total: 3, groups: titles.map((title, i) => ({
        yearMonth: `2026-0${9 - i}`, items: [{ ...item(i + 1), title, createdAt: `2026-0${9 - i}-02T18:06:00` }],
      })) }
    } } })
    await p.loadTimeline()
    assert.equal(p.$select('year-heading').length, 1)
    assert.deepEqual(Array.from(p.$select('card-title'), x => x.children), titles)
    for (const selector of ['card', 'entry-date', 'entry-arrow']) assert.equal(p.$select(selector).length, 3)
    for (const selector of ['month-heading', 'date-rail', 'entry-context', 'entry-status', 'card-photo', 'card-footer-tag']) assert.equal(p.$select(selector).length, 0)
    for (const node of p.$select('tl-item')) node.props.onTap()
    assert.equal(h.navigation.length, 3)
    assert.equal(calls, 1); assert.equal(h.requests.length, 0)
  })
  for (const failureStage of ['create', 'save', null]) {
    await check('编辑器保存链路 ' + (failureStage || '成功与忙碌防重'), async () => {
      const h = harness(), saveWait = deferred()
      let creates = 0, saves = 0
      const store = {
        async createDraft() { creates++; if (failureStage === 'create') throw new Error('synthetic create failure'); return { id: 99, status: 'DRAFT' } },
        async saveRecord() { saves++; if (failureStage === 'save') throw new Error('synthetic save failure'); return saveWait.promise },
      }
      const p = h.page('record-editor', {}, store)
      p.form.content = '固定合成测试文字'
      const work = p.saveDraft()
      await Promise.resolve(); await Promise.resolve()
      if (!failureStage) {
        assert.equal(p.loading.value, true)
        assert.equal(p.savedFeedback.value, false)
        await p.saveDraft()
        assert.equal(creates, 1); assert.equal(saves, 1)
        saveWait.resolve({ id: 99, status: 'SAVED' })
      }
      await work
      assert.equal(p.savedFeedback.value, !failureStage)
      assert.equal(p.form.content, '固定合成测试文字')
      assert.equal(p.loading.value, false)
      assert.equal(h.navigation.length, 0)
      if (failureStage) assert.equal(h.toasts.length, 1)
      else { assert.equal(p.recordStatus.value, 'SAVED'); assert.equal(p.hasDirtyChanges(), false) }
    })
  }
  await check('收起附言后保存仍保留节点与已有附言数据', async () => {
    const h = harness()
    let payload
    const p = h.page('record-editor', {}, {
      async createDraft(value) { payload = JSON.parse(JSON.stringify(value)); return { id: 99, status: 'DRAFT' } },
      async saveRecord() { return { id: 99, status: 'SAVED' } },
    })
    Object.assign(p.form, { content: '固定合成正文', recordType: 'NODE_RECORD', lifeNodeType: 'OTHER', lifeNodeCustomLabel: '固定合成节点', beliefThen: '固定合成附言', tagIds: [7] })
    p.appendixOpen.value = true
    p.appendixOpen.value = false
    await p.saveDraft()
    assert.equal(payload.recordType, 'NODE_RECORD')
    assert.equal(payload.lifeNodeType, 'OTHER')
    assert.equal(payload.lifeNodeCustomLabel, '固定合成节点')
    assert.equal(payload.beliefThen, '固定合成附言')
    assert.deepEqual(payload.tagIds, [7])
    assert.equal(p.form.beliefThen, '固定合成附言')
    assert.equal(p.savedFeedback.value, true)
  })
  await check('编辑器空内容、未完成媒体与Preview均不保存', async () => {
    const h = harness()
    const p = h.page('record-editor', {}, { createDraft() { throw new Error('must not persist') } })
    await p.saveDraft()
    p.form.content = '固定合成测试文字'
    p.pendingImageUploads.value.push({ localId: 'synthetic' })
    await p.saveDraft()
    p.pendingImageUploads.value = []
    h.storage.delete('flashback:token')
    h.storage.set('flashback:preview-session', { enabled: true })
    await p.saveDraft()
    assert.equal(p.savedFeedback.value, false)
    assert.equal(h.toasts.length, 3)
    assert.equal(h.requests.length, 0)
  })
  await check('首页轻信笺展示真实SAVED标题与入口，加载失败仍可重试', async () => {
    const h = harness(), p = h.page('home', { recordService: { getRecordList: async status => ({ total: status === 'SAVED' ? 1 : 0, list: status === 'SAVED' ? [{ id: 99, title: 'synthetic saved title' }] : [] }), getUnlockedRecords: async () => ({ total: 0, list: [] }) } })
    await p.loadHomeSummary()
    assert.equal(p.$select('saved-entry').length, 1); assert.equal(p.$select('saved-entry-title')[0].children, 'synthetic saved title')
    p.$select('saved-entry')[0].props.onTap(); assert.match(h.navigation.at(-1), /record-editor\/index\?id=99/)
    p.draftState.value = 'error'; assert.equal(p.$select('saved-entry').length, 0); assert.equal(p.$select('retry').length, 1)
    p.draftState.value = 'ready'; p.latestDraft.value = null; assert.equal(p.$select('saved-entry').length, 0); assert.equal(h.requests.length, 0)
  })
  await check('新建/恢复/已留下共享双入口，普通保存不自动开封存浮层', async () => {
    const h = harness(), calls = [], p = h.page('record-editor', {}, { createDraft: async () => ({ id: 99, status: 'DRAFT' }), saveRecord: async () => { calls.push('save'); return { id: 99, status: 'SAVED' } } })
    p.form.content = 'synthetic save content'
    assert.equal(p.$select('seal-entry').length, 1); assert.equal(p.$select('save-entry').length, 1)
    assert.equal(p.$select('save-entry')[0].children.at(-1).children, '留下这一刻')
    assert.equal(p.$select('editor-toolbar--dual').length, 1)
    p.recordId.value = 77; assert.equal(p.$select('seal-entry').length, 1); p.recordId.value = null
    await p.$select('save-entry')[0].props.onTap()
    assert.equal(p.$select('seal-entry').length, 1); assert.equal(p.$select('save-entry')[0].children.at(-1).children, '保存补充')
    assert.equal(p.$select('unlock-bar').length, 0); assert.equal(p.savedFeedback.value, true); assert.equal(p.showSealSheet.value, false); assert.equal(h.navigation.length, 0); assert.deepEqual(calls, ['save'])
    for (const status of ['SEALED', 'UNLOCKED']) { p.recordStatus.value = status; p.openSealSheet(); assert.equal(p.showSealSheet.value, false); assert.equal(p.$select('seal-entry').length, 0) }
  })
  await check('新建/恢复时间准备与滚轮取消零写入，临时时间不污染表单', () => {
    for (const id of [null, 99]) {
      const h = harness(), calls = [], p = h.page('record-editor', {}, { createDraft() { calls.push('create'); throw Error('must not persist') }, updateDraft() { calls.push('update'); throw Error('must not persist') } })
      p.recordId.value = id; p.form.content = 'synthetic draft content'; p.markSnapshot()
      p.$select('seal-entry')[0].props.onTap()
      assert.equal(p.$select('seal-panel').length, 1); assert.equal(p.$select('seal-save').length, 1); assert.equal(p.$select('seal-confirm').length, 0)
      p.$select('seal-time-entry')[0].props.onTap(); assert.equal(p.$select('seal-panel').length, 0)
      p.$nodes().find(node => node.type === 'DateTimeWheelPicker').props.onCancel(); assert.equal(p.$select('seal-panel').length, 1)
      p.openSealTimePicker(); p.onUnlockConfirm(String(new Date().getFullYear() + 1) + '-06-01 12:00')
      assert.equal(p.form.unlockAtInput, ''); assert.equal(p.hasDirtyChanges(), false)
      p.$select('seal-mask')[0].props.onTap(); assert.equal(p.showSealSheet.value, false)
      p.openSealSheet(); assert.equal(p.sealTimeDraft.value, ''); p.$select('seal-close')[0].props.onTap()
      assert.equal(p.recordStatus.value, 'DRAFT'); assert.equal(p.recordId.value, id); assert.equal(calls.length, 0); assert.equal(h.requests.length, 0)
    }
  })
  await check('新建/恢复面板先显式保存，真实SAVED后第二次点击才封存且全程防重', async () => {
    for (const id of [null, 99]) {
      const h = harness(), calls = [], saveWait = deferred(), sealWait = deferred(); let status = 'DRAFT'
      const p = h.page('record-editor', {}, {
        createDraft: async payload => { calls.push(['create', payload.unlockAt]); return { id: 99, status } },
        updateDraft: async (id, payload) => { calls.push(['update', payload.unlockAt]); return { id, status } },
        saveRecord: async id => { calls.push(['save', id]); const result = await saveWait.promise; status = result.status; return result },
        sealRecord: async id => { calls.push(['seal', id]); return sealWait.promise },
        updateUnlockReminderAuthorization: async () => ({ id: 99, status: 'SEALED' }),
      })
      p.recordId.value = id; p.form.content = 'synthetic two-stage content'; p.openSealSheet()
      p.onUnlockConfirm(String(new Date().getFullYear() + 1) + '-06-01 12:00')
      await p.confirmSealSheet(); assert.equal(calls.length, 0)
      const save = p.$select('seal-save')[0].props.onTap()
      for (let tick = 0; tick < 8 && !calls.some(x => x[0] === 'save'); tick++) await Promise.resolve()
      assert.equal(p.loading.value, true); assert.equal(p.recordStatus.value, 'DRAFT'); assert.equal(p.savedFeedback.value, false)
      assert.equal(p.$select('seal-confirm').length, 0); assert.equal(p.$select('seal-save')[0].props.disabled, true)
      const count = calls.length; await p.$select('seal-save')[0].props.onTap(); await p.confirmSealSheet(); p.closeSealSheet()
      assert.equal(calls.length, count); assert.equal(p.showSealSheet.value, true)
      saveWait.resolve({ id: 99, status: 'SAVED' }); await save
      assert.equal(p.recordStatus.value, 'SAVED'); assert.equal(p.$select('seal-save').length, 0); assert.equal(p.$select('seal-confirm').length, 1)
      assert.equal(p.showSealSheet.value, true); assert.equal(p.form.unlockAtInput, ''); assert.equal(calls.filter(x => x[0] === 'seal').length, 0)
      assert.equal(calls[0][1], null); assert.equal(h.navigation.length, 0)
      const seal = p.$select('seal-confirm')[0].props.onTap()
      for (let tick = 0; tick < 8 && !calls.some(x => x[0] === 'seal'); tick++) await Promise.resolve()
      const sealingCount = calls.length; await p.confirmSealSheet(); p.closeSealSheet(); assert.equal(calls.length, sealingCount)
      sealWait.resolve({ id: 99, status: 'SEALED' }); await seal
      assert.equal(p.recordStatus.value, 'SEALED'); assert.equal(p.showSealSheet.value, false)
      assert.equal(calls.filter(x => x[0] === 'save').length, 1); assert.equal(calls.filter(x => x[0] === 'seal').length, 1)
      assert.deepEqual(calls.map(x => x[0]), [id ? 'update' : 'create', 'save', 'update', 'seal'])
      assert.equal(h.requests.length, 0)
    }
  })
  await check('准备面板保存创建/确认失败保输入可重试；保存后取消保留真实SAVED', async () => {
    for (const failure of ['create', 'save']) {
      const h = harness(), calls = []; let fail = true
      const p = h.page('record-editor', {}, {
        createDraft: async () => { calls.push('create'); if (fail && failure === 'create') throw Error('synthetic create failure'); return { id: 99, status: 'DRAFT' } },
        updateDraft: async () => { calls.push('update'); return { id: 99, status: 'DRAFT' } },
        saveRecord: async () => { calls.push('save'); if (fail && failure === 'save') throw Error('synthetic save failure'); return { id: 99, status: 'SAVED' } },
        sealRecord: async () => { calls.push('seal'); throw Error('must not seal') },
      })
      p.form.content = 'synthetic retained input'; p.openSealSheet(); const time = String(new Date().getFullYear() + 1) + '-06-01 12:00'; p.onUnlockConfirm(time)
      await p.$select('seal-save')[0].props.onTap()
      assert.equal(p.recordStatus.value, 'DRAFT'); assert.equal(p.showSealSheet.value, true); assert.equal(p.savedFeedback.value, false)
      assert.equal(p.form.content, 'synthetic retained input'); assert.equal(p.sealTimeDraft.value, time); assert.equal(p.form.unlockAtInput, '')
      assert.equal(p.$select('seal-confirm').length, 0); assert.equal(h.toasts.length, 1)
      fail = false; await p.$select('seal-save')[0].props.onTap()
      assert.equal(p.recordId.value, 99); assert.equal(p.recordStatus.value, 'SAVED'); assert.equal(p.savedFeedback.value, true)
      p.closeSealSheet(); assert.equal(p.recordStatus.value, 'SAVED'); assert.equal(p.form.unlockAtInput, '')
      assert.equal(calls.includes('seal'), false); assert.equal(h.navigation.length, 0); assert.equal(h.requests.length, 0)
    }
  })
  await check('新建准备面板的空内容/媒体处理中/失败媒体与Preview仍拒绝保存', async () => {
    for (const state of ['empty', 'image', 'voice', 'recording', 'preview']) {
      const h = harness(), calls = [], p = h.page('record-editor', {}, { createDraft() { calls.push('create'); throw Error('must not persist') }, sealRecord() { calls.push('seal'); throw Error('must not seal') } })
      p.form.content = state === 'empty' ? '' : 'synthetic guarded content'
      p.form.title = 'synthetic title is not evidence'
      if (state === 'image') p.pendingImageUploads.value.push({ localId: 'synthetic pending image', status: 'failed' })
      if (state === 'voice') p.pendingVoiceUploads.value.push({ localId: 'synthetic pending voice', status: 'failed' })
      if (state === 'recording') p.voiceRecording.value = true
      if (state === 'preview') { h.storage.delete('flashback:token'); h.storage.set('flashback:preview-session', { enabled: true }) }
      p.openSealSheet(); await p.$select('seal-save')[0].props.onTap(); await p.confirmSealSheet()
      assert.equal(p.recordStatus.value, 'DRAFT'); assert.equal(p.showSealSheet.value, true); assert.equal(p.savedFeedback.value, false)
      assert.equal(p.form.unlockAtInput, ''); assert.equal(calls.length, 0); assert.equal(h.requests.length, 0); assert.equal(h.toasts.length, 1)
    }
  })
  await check('图片/声音单独保存与无解封时间仍可完成准备面板的显式保存', async () => {
    for (const type of ['IMAGE', 'VOICE']) {
      const h = harness(), calls = [], p = h.page('record-editor', {}, { createDraft: async () => ({ id: 99, status: 'DRAFT' }), saveRecord: async () => { calls.push('save'); return { id: 99, status: 'SAVED' } } })
      p.attachments.value = [{ id: 1, type, status: 'AVAILABLE', sizeBytes: 10 }]
      p.openSealSheet(); assert.equal(p.sealTimeDraft.value, ''); await p.$select('seal-save')[0].props.onTap()
      assert.equal(p.recordStatus.value, 'SAVED'); assert.equal(p.showSealSheet.value, true); assert.equal(p.$select('seal-confirm')[0].props.disabled, true)
      assert.deepEqual(calls, ['save']); assert.equal(p.form.content, ''); assert.equal(h.requests.length, 0)
    }
  })
  await check('新建显式保存后封存失败保持SAVED，重试只走原封存路径不重复save', async () => {
    const h = harness(), calls = []; let fail = true
    const p = h.page('record-editor', {}, {
      createDraft: async () => ({ id: 99, status: 'DRAFT' }),
      saveRecord: async () => { calls.push('save'); return { id: 99, status: 'SAVED' } },
      updateDraft: async () => { calls.push('update'); return { id: 99, status: 'SAVED' } },
      sealRecord: async () => { calls.push('seal'); if (fail) throw Error('synthetic seal failure'); return { id: 99, status: 'SEALED' } },
      updateUnlockReminderAuthorization: async () => ({ id: 99, status: 'SEALED' }),
    })
    p.form.content = 'synthetic retained before sealing'; p.openSealSheet(); p.onUnlockConfirm(String(new Date().getFullYear() + 1) + '-06-01 12:00')
    await p.$select('seal-save')[0].props.onTap(); await p.$select('seal-confirm')[0].props.onTap()
    assert.equal(p.recordStatus.value, 'SAVED'); assert.equal(p.showSealSheet.value, true); assert.equal(p.form.content, 'synthetic retained before sealing')
    assert.equal(p.$select('seal-save').length, 0); fail = false; await p.$select('seal-confirm')[0].props.onTap()
    assert.deepEqual(calls, ['save', 'update', 'seal', 'update', 'seal']); assert.equal(p.recordStatus.value, 'SEALED'); assert.equal(p.showSealSheet.value, false)
  })
  await check('封存浮层选择时间后取消不改表单，滚轮取消返回确认层且零请求', () => {
    const h = harness(), p = h.page('record-editor')
    p.recordStatus.value = 'SAVED'; p.recordId.value = 99; p.form.content = 'synthetic saved content'; p.form.unlockAtInput = ''; p.markSnapshot()
    p.$select('seal-entry')[0].props.onTap(); assert.equal(p.$select('seal-panel').length, 1)
    p.$select('seal-time-entry')[0].props.onTap(); assert.equal(p.$select('seal-panel').length, 0)
    p.$nodes().find(node => node.type === 'DateTimeWheelPicker').props.onCancel(); assert.equal(p.$select('seal-panel').length, 1)
    p.openSealTimePicker(); p.onUnlockConfirm(String(new Date().getFullYear() + 1) + '-06-01 12:00')
    assert.equal(p.form.unlockAtInput, ''); assert.equal(p.hasDirtyChanges(), false)
    p.$select('seal-mask')[0].props.onTap(); assert.equal(p.showSealSheet.value, false); assert.equal(h.requests.length, 0)
    p.openSealSheet(); assert.equal(p.sealTimeDraft.value, '')
  })
  await check('封存确认拒绝无效时间、Preview和未完成媒体，不绕过原封存校验', async () => {
    const h = harness(), calls = [], p = h.page('record-editor', {}, { updateDraft() { calls.push('update'); throw Error('must not persist') } })
    p.recordStatus.value = 'SAVED'; p.recordId.value = 99; p.form.content = 'synthetic guarded content'; p.openSealSheet()
    await p.confirmSealSheet(); p.sealTimeDraft.value = '2000-01-01 12:00'; await p.confirmSealSheet()
    assert.equal(p.form.unlockAtInput, ''); assert.equal(p.showSealSheet.value, true)
    p.sealTimeDraft.value = String(new Date().getFullYear() + 1) + '-06-01 12:00'
    h.storage.delete('flashback:token'); h.storage.set('flashback:preview-session', { enabled: true }); await p.confirmSealSheet(); assert.equal(p.form.unlockAtInput, '')
    h.storage.set('flashback:token', 'synthetic-token'); p.pendingVoiceUploads.value.push({ localId: 'synthetic pending' }); await p.confirmSealSheet()
    assert.equal(p.recordStatus.value, 'SAVED'); assert.equal(p.showSealSheet.value, true); assert.equal(h.toasts.length, 4); assert.equal(h.requests.length, 0); assert.equal(calls.length, 0)
  })
  await check('封存失败保留正文时间可重试，忙碌防重，响应成功才离开', async () => {
    const h = harness(), calls = [], wait = deferred(); let fail = true
    const p = h.page('record-editor', {}, {
      updateDraft: async (id, payload) => { calls.push(['update', id, payload.unlockAt]); return { id, status: 'SAVED' } },
      sealRecord: async id => { calls.push(['seal', id]); if (fail) throw Error('synthetic seal failure'); return wait.promise },
      updateUnlockReminderAuthorization: async () => ({ id: 99, status: 'SEALED' }),
    })
    p.recordStatus.value = 'SAVED'; p.recordId.value = 99; p.form.content = 'synthetic retained content'; p.openSealSheet()
    p.sealTimeDraft.value = String(new Date().getFullYear() + 1) + '-06-01 12:00'; await p.confirmSealSheet()
    assert.equal(p.recordStatus.value, 'SAVED'); assert.equal(p.showSealSheet.value, true); assert.equal(p.form.content, 'synthetic retained content'); assert.equal(h.navigation.length, 0)
    fail = false; const work = p.confirmSealSheet(); for (let tick = 0; tick < 8 && calls.length < 4; tick++) await Promise.resolve()
    const count = calls.length; await p.confirmSealSheet(); p.closeSealSheet(); assert.equal(calls.length, count); assert.equal(p.showSealSheet.value, true); assert.equal(p.$select('seal-confirm')[0].props.disabled, true)
    wait.resolve({ id: 99, status: 'SEALED' }); await work
    assert.equal(p.recordStatus.value, 'SEALED'); assert.equal(p.showSealSheet.value, false); assert.equal(h.navigation.length, 0)
    assert.equal(h.timers.at(-1).delay, 300); h.timers.at(-1).fn(); assert.equal(h.navigation.at(-1), '/pages/home/index')
  })
  await check('真实recordService拒绝所有Preview记录写入且零请求', async () => {
    const h = harness()
    h.storage.delete('flashback:token')
    h.storage.set('flashback:preview-session', { enabled: true })
    const actions = [() => h.service.createDraft({}), () => h.service.updateDraft(1, { content: '', recordType: 'MOMENT' }), () => h.service.saveRecord(1), () => h.service.sealRecord(1), () => h.service.updateLocation(1, { source: 'MANUAL' }), () => h.service.deleteLocation(1), () => h.service.updateCover(1, { attachmentId: null }), () => h.service.updateLaterReflection(1, ''), () => h.service.updateAgentMemoryPolicy(1, false, null), () => h.service.updateUnlockReminderAuthorization(1, 'SKIPPED')]
    for (const action of actions) await assert.rejects(action(), /只读/)
    await h.service.getTimeline()
    assert.equal(h.requests.length, 0)
  })
  await check('认证优先于Preview、保存走真实服务入口', async () => {
    const h = harness()
    h.storage.set('flashback:preview-session', { enabled: true })
    h.setResponse(() => ({ code: 0, data: { id: 99, status: 'SAVED' } }))
    const saved = await h.service.saveRecord(99)
    assert.equal(saved.status, 'SAVED')
    assert.equal(h.requests.length, 1)
    assert.match(h.requests[0].url, /\/api\/records\/99\/save$/)
    assert.equal(h.requests[0].method, 'POST')
  })
  await check('真实httpClient清除冻结拒绝记录写入', async () => {
    const h = harness()
    h.mutation.rememberDataOperation({ operationType: 'CLEAR_ALL_RECORDS', status: 'RUNNING' })
    await assert.rejects(h.service.saveRecord(99), /暂时不能/)
    assert.equal(h.requests.length, 0)
  })
  await check('个人中心沿真实用户与原计数语义展示，初始加载不冒充零记录', async () => {
    const h = harness(), wait = deferred(), calls = [], p = h.page('user-center', { recordService: {
      getRecordList: async (status, page) => { calls.push([status, page.pageSize]); return { total: 2 } },
      getUnlockedRecords: async (page, size) => { calls.push(['UNLOCKED', size]); return { total: 12 } },
    } }, { fetchUserInfo: () => wait.promise })
    const work = p.loadCenterData()
    assert.equal(p.$select('state-block').length, 1); assert.equal(p.$select('custody').length, 0); assert.equal(p.$select('voucher').length, 0)
    wait.resolve({ nickname: 'synthetic name', avatar: 'synthetic-avatar' }); await work
    assert.equal(p.$select('profile-name')[0].children, 'synthetic name'); assert.equal(p.$select('avatar-img')[0].props.src, 'synthetic-avatar')
    assert.equal(p.displayArchivedCount.value, '14'); assert.equal(p.displaySealedCount.value, '2')
    assert.match(p.$select('custody')[0].children, /14.*2/); assert.deepEqual(calls, [['SEALED', 1], ['UNLOCKED', 1]])
    assert.equal(p.$select('entrance').length, 3); assert.equal(p.$select('stats-row').length, 0); assert.equal(p.$select('stage-summary-body').length, 0)
    p.$select('avatar-img')[0].props.onError(); assert.equal(p.$select('avatar-img').length, 0)
    assert.equal(p.$select('voucher-title')[0].children, '托管凭证'); assert.equal(h.requests.length, 0)
  })
  await check('个人中心失败/重试与旧资料提示保留，零计数与长昵称来自实际响应', async () => {
    const h = harness(); let fail = true
    const p = h.page('user-center', { recordService: { getRecordList: async () => ({ total: 0 }), getUnlockedRecords: async () => ({ total: 0 }) } }, { fetchUserInfo: async () => { if (fail) throw Error('synthetic failure'); return { nickname: 'synthetic name '.repeat(40), avatar: '' } } })
    await p.loadCenterData(); assert.equal(p.$select('state-action').length, 1); assert.equal(p.$select('entrance').length, 0); assert.equal(p.profileReady.value, false)
    fail = false; await p.$select('state-action')[0].props.onTap()
    assert.equal(p.$select('profile-name')[0].children, 'synthetic name '.repeat(40)); assert.equal(p.displayArchivedCount.value, '0'); assert.equal(p.$select('avatar-img').length, 0)
    fail = true; await p.loadCenterData(); assert.equal(p.$select('stale-notice').length, 1); assert.equal(p.$select('entrance').length, 3)
    fail = false; await p.$select('stale-action')[0].props.onTap(); assert.equal(p.$select('stale-notice').length, 0); assert.equal(h.requests.length, 0)
  })
  await check('个人中心三入口及托管/关于/退出沿既有路径，点击导出清理不直接写入', async () => {
    const h = harness(); let generated = 0, loggedOut = 0
    const p = h.page('user-center', { recordService: { getRecordList: async () => ({ total: 0 }), getUnlockedRecords: async () => ({ total: 0 }) }, stageSummaryService: { generate: async () => { generated++; return { status: 'SUCCESS', summary: 'synthetic summary' } } } }, { fetchUserInfo: async () => ({ username: 'synthetic user' }), logout: () => { loggedOut++ } })
    await p.loadCenterData(); assert.equal(generated, 0)
    p.$select('entrance--records')[0].props.onTap(); p.$select('entrance--review')[0].props.onTap()
    assert.deepEqual(h.navigation, ['/pages/record-list/index', '/pages/timeline/index'])
    for (const cls of ['voucher-main', 'voucher-export', 'voucher-clear']) p.$select(cls)[0].props.onTap()
    assert.deepEqual(h.navigation.slice(2), Array(3).fill('/pages/user-center/data-backup/index'))
    p.$select('about-entry')[0].props.onTap(); assert.equal(h.actionSheets.length, 1); assert.equal(loggedOut, 0)
    h.actionSheets[0].success({ tapIndex: 0 }); assert.equal(h.navigation.at(-1), '/pages/user-center/about/index')
    h.actionSheets[0].success({ tapIndex: 1 }); assert.equal(loggedOut, 1)
    await p.$select('entrance--summary')[0].props.onTap(); assert.equal(generated, 1); assert.equal(p.$select('stage-summary-body').length, 1)
    const tabs = p.$select('nav-item'); tabs[0].props.onTap(); tabs[1].props.onTap(); assert.equal(tabs[2].children[0].children, '个人中心')
    assert.deepEqual(h.navigation.slice(-2), ['/pages/home/index', '/pages/timeline/index']); assert.equal(h.requests.length, 0)
  })
  await check('个人中心近况防重、成功/FALLBACK来源与失败保留，Preview不生成', async () => {
    const h = harness(), wait = deferred(); let generate = () => wait.promise, count = 0
    const p = h.page('user-center', { recordService: { getRecordList: async () => ({ total: 0 }), getUnlockedRecords: async () => ({ total: 0 }) }, stageSummaryService: { generate: async () => { count++; return generate() } } }, { fetchUserInfo: async () => ({ nickname: 'synthetic user' }) })
    await p.loadCenterData()
    const work = p.$select('entrance--summary')[0].props.onTap(); assert.equal(p.$select('entrance--summary')[0].props.disabled, true)
    await p.generateStageSummary(); assert.equal(count, 1); assert.equal(p.$select('stage-summary-body').length, 0)
    wait.resolve({ status: 'SUCCESS', summary: 'synthetic AI summary', recordCount: 3, unlockedCount: 2, lifeNodeCount: 1 }); await work
    assert.equal(p.$select('stage-summary-text')[0].children, 'synthetic AI summary'); assert.match(p.$select('stage-summary-meta')[0].children, /AI 整理/)
    generate = async () => ({ status: 'FALLBACK', summary: 'synthetic fallback', recordCount: 1, unlockedCount: 0, lifeNodeCount: 0, message: 'synthetic fallback notice' }); await p.generateStageSummary()
    assert.match(p.$select('stage-summary-meta')[0].children, /本地整理/); assert.equal(h.toasts.at(-1), 'synthetic fallback notice')
    generate = async () => { throw Error('synthetic timeout') }; await p.generateStageSummary(); assert.equal(p.summaryLoading.value, false)
    assert.equal(p.$select('stage-summary-text')[0].children, 'synthetic fallback'); assert.match(h.toasts.at(-1), /超时/)
    h.storage.delete('flashback:token'); h.storage.set('flashback:preview-session', { enabled: true }); const previous = count
    await p.generateStageSummary(); assert.equal(count, previous); assert.equal(h.requests.length, 0)
  })
  await check('个人中心无会话只走登录，Preview保留用户资料与真实动作拒绝边界', async () => {
    const h = harness(); let reads = 0
    const p = h.page('user-center', { recordService: { getRecordList: async () => ({ total: 0 }), getUnlockedRecords: async () => ({ total: 0 }) } }, { fetchUserInfo: async () => { reads++; return { nickname: 'synthetic preview' } } })
    h.storage.delete('flashback:token'); await p.loadCenterData(); assert.equal(reads, 0); assert.equal(h.navigation.at(-1), '/pages/login/index')
    h.storage.set('flashback:preview-session', { enabled: true }); await p.loadCenterData(); assert.equal(reads, 1); assert.equal(p.nickname.value, 'synthetic preview')
    await p.generateStageSummary(); assert.match(h.toasts.at(-1), /演示/); assert.equal(h.requests.length, 0)
  })
  const ownershipOperation = (extra = {}) => ({ id: 9, operationType: 'EXPORT', status: 'SUCCEEDED', totalItems: 19, processedItems: 19, failedItems: 0, retryable: false, downloadable: true, ...extra })
  const ownershipSummary = (activeOperation = null) => ({ recordCounts: { DRAFT: 1, SAVED: 3, SEALED: 1, UNLOCKED: 14 }, mediaBytes: 3145728, activeOperation })
  await check('所有权简洁主画面使用真实保管计数；打开/关闭面板不触发导出清理', async () => {
    const h = harness(), wait = deferred(), calls = [], p = h.page('user-center/data-backup', { dataOwnershipService: { summary: () => wait.promise, createExport: () => calls.push('export'), prepareDeletion: () => calls.push('prepare') } })
    const work = p.loadSummary(); assert.equal(p.$select('export-entry')[0].props.disabled, true); assert.equal(p.$select('summary-card').length, 0)
    assert.equal(p.summary.value, null); p.openPanel('export'); assert.equal(p.activePanel.value, null)
    wait.resolve(ownershipSummary()); await work
    assert.equal(p.custodyCount.value, 15); assert.equal(p.inTransitCount.value, 1); assert.equal(p.totalRecords.value, 19)
    assert.equal(p.$select('summary-card').length, 0); assert.equal(p.$select('action-panel').length, 0); assert.equal(p.$select('operation-card').length, 0)
    p.$select('export-entry')[0].props.onTap(); assert.equal(p.policy.value, 'RESPECT_SEAL'); assert.equal(p.$select('option').length, 2); assert.equal(p.$select('state-list').length, 1)
    p.$select('action-mask')[0].props.onTap(); p.$select('clear-entry')[0].props.onTap(); assert.equal(p.$select('prepare-clear').length, 1)
    p.$select('panel-close')[0].props.onTap(); assert.deepEqual(calls, []); assert.equal(h.requests.length, 0)
    p.openBuildInfo(); p.goBack(); assert.deepEqual(h.navigation, ['/pages/user-center/about/index', 'back'])
  })
  await check('所有权两种封存策略只在确认生成时提交，响应决定下载/有效期且保存失败可重试', async () => {
    const h = harness(), calls = [], wait = deferred(); let op = null, generate = () => wait.promise
    const p = h.page('user-center/data-backup', { dataOwnershipService: {
      summary: async () => ownershipSummary(op),
      createExport: async policy => { calls.push(['export', policy]); op = await generate(); return op },
      download: async id => { calls.push(['download', id]); return 'synthetic-export.zip' },
    } })
    await p.loadSummary(); p.openPanel('export')
    const work = p.$select('primary-button')[0].props.onTap(); assert.equal(p.loading.value, true); assert.equal(p.$select('download-export').length, 0)
    await p.startExport(); p.closePanel(); assert.equal(p.activePanel.value, 'export'); assert.equal(calls.length, 1)
    wait.resolve(ownershipOperation({ artifactExpiresAt: '2099-06-01T12:00:00' })); await work
    assert.match(p.exportValidityText.value, /2099/); assert.equal(p.operation.value.id, 9); assert.equal(p.loading.value, false)
    h.uni.saveFile = options => options.fail(); await p.$select('download-export')[0].props.onTap(); assert.match(h.toasts.at(-1), /未保存/)
    h.uni.saveFile = options => options.success(); await p.downloadExport(); assert.equal(h.toasts.at(-1), '导出包已保存')
    p.$select('option')[1].props.onTap(); assert.equal(p.policy.value, 'FULL_CONTENT')
    generate = async () => ownershipOperation({ id: 10, sealedContentPolicy: 'FULL_CONTENT' }); await p.$select('primary-button')[0].props.onTap()
    assert.deepEqual(calls.filter(x => x[0] === 'export'), [['export', 'RESPECT_SEAL'], ['export', 'FULL_CONTENT']]); assert.equal(h.requests.length, 0)
  })
  await check('所有权导出失败无假成功，过期不下载且服务端各操作阶段可见', async () => {
    const h = harness(), p = h.page('user-center/data-backup', { dataOwnershipService: { summary: async () => ownershipSummary(), createExport: async () => { throw Error('synthetic export failure') }, download: () => { throw Error('must not download') } } })
    await p.loadSummary(); p.openPanel('export'); await p.startExport()
    assert.equal(p.operation.value, null); assert.equal(p.activePanel.value, 'export'); assert.equal(p.loading.value, false); assert.equal(p.$select('download-export').length, 0)
    assert.equal(h.toasts.at(-1), 'synthetic export failure')
    p.operation.value = ownershipOperation({ status: 'EXPIRED', downloadable: false }); assert.match(p.exportValidityText.value, /已过期/); await p.downloadExport()
    for (const [status, label] of [['PREPARED', '等待确认'], ['PENDING', '等待处理'], ['RUNNING', '正在处理'], ['RETRY_REQUIRED', '仍有数据未完成清理'], ['FAILED', '操作未完成']]) {
      p.operation.value = ownershipOperation({ status, downloadable: false, retryable: status === 'RETRY_REQUIRED', failureCode: 'SYNTHETIC_CODE' })
      assert.equal(p.$select('operation-title')[0].children, label); assert.equal(p.$select('operation-code').length, 1)
    }
    assert.equal(h.requests.length, 0)
  })
  await check('所有权清理保留服务端短语和两次明确确认，准备/确认防重且取消可继续', async () => {
    const h = harness(), calls = [], prepareWait = deferred(), confirmWait = deferred(); let op = null, snapshot = ownershipSummary()
    const intent = ownershipOperation({ operationType: 'CLEAR_ALL_RECORDS', status: 'PREPARED', downloadable: false, confirmationText: 'synthetic clear phrase 19' })
    const p = h.page('user-center/data-backup', { dataOwnershipService: {
      summary: async () => ({ ...snapshot, activeOperation: op }),
      prepareDeletion: async scope => { calls.push(['prepare', scope]); return prepareWait.promise },
      confirmDeletion: async (id, text) => { calls.push(['confirm', id, text]); op = await confirmWait.promise; return op },
    } })
    await p.loadSummary(); p.openPanel('clear'); assert.equal(calls.length, 0)
    const prepare = p.$select('prepare-clear')[0].props.onTap(); assert.equal(p.preparingClear.value, true)
    await p.requestClearConfirmation(); p.closePanel(); assert.equal(calls.length, 1); assert.equal(p.activePanel.value, 'clear')
    prepareWait.resolve(intent); await prepare; assert.equal(p.$select('phrase')[0].children, intent.confirmationText)
    p.confirmationInput.value = 'wrong'; await p.$select('solid')[0].props.onTap(); assert.equal(calls.length, 1); assert.equal(h.toasts.at(-1), '确认短语不匹配')
    p.closePanel(); assert.equal(p.clearIntent.value.id, 9); p.$select('resume-clear')[0].props.onTap(); p.confirmationInput.value = '  ' + intent.confirmationText + '  '
    const confirm = p.$select('solid')[0].props.onTap(); await p.confirmClearAndClose(); p.closePanel(); assert.equal(p.activePanel.value, 'clear'); assert.equal(calls.length, 2)
    confirmWait.resolve(ownershipOperation({ operationType: 'CLEAR_ALL_RECORDS', status: 'PENDING', downloadable: false, processedItems: 0 })); await confirm
    assert.equal(p.clearIntent.value, null); assert.equal(p.activePanel.value, null); assert.equal(p.totalRecords.value, 19); assert.equal(p.operationText.value, '等待处理')
    assert.deepEqual(calls, [['prepare', 'ALL_RECORDS'], ['confirm', 9, intent.confirmationText]])
    snapshot = { recordCounts: { DRAFT: 0, SAVED: 0, SEALED: 0, UNLOCKED: 0 }, mediaBytes: 0 }; op = ownershipOperation({ operationType: 'CLEAR_ALL_RECORDS', status: 'SUCCEEDED', downloadable: false })
    await p.$select('operation-refresh')[0].props.onTap(); assert.equal(p.totalRecords.value, 0); assert.equal(p.operationText.value, '清理已完成'); assert.equal(h.requests.length, 0)
  })
  await check('所有权准备/清理失败保输入与意图，恢复后重试沿原operation id', async () => {
    const h = harness(), calls = []; let prepareFails = true, confirmFails = true, op = null
    const intent = ownershipOperation({ operationType: 'CLEAR_ALL_RECORDS', status: 'PREPARED', downloadable: false, confirmationText: 'synthetic confirmation' })
    const p = h.page('user-center/data-backup', { dataOwnershipService: {
      summary: async () => ownershipSummary(op),
      prepareDeletion: async () => { calls.push('prepare'); if (prepareFails) throw Error('synthetic prepare failure'); return intent },
      confirmDeletion: async () => { calls.push('confirm'); if (confirmFails) throw Error('synthetic confirmation failure'); op = ownershipOperation({ operationType: 'CLEAR_ALL_RECORDS', status: 'RETRY_REQUIRED', retryable: true, downloadable: false }); return op },
      retry: async id => { calls.push(['retry', id]); op = ownershipOperation({ operationType: 'CLEAR_ALL_RECORDS', status: 'SUCCEEDED', downloadable: false }); return op },
    } })
    await p.loadSummary(); p.openPanel('clear'); await p.requestClearConfirmation(); assert.equal(p.clearIntent.value, null); assert.equal(p.preparingClear.value, false)
    prepareFails = false; await p.requestClearConfirmation(); p.confirmationInput.value = intent.confirmationText; await p.confirmClearAndClose()
    assert.equal(p.activePanel.value, 'clear'); assert.equal(p.clearIntent.value.id, 9); assert.equal(p.confirmationInput.value, intent.confirmationText)
    confirmFails = false; await p.confirmClearAndClose(); assert.equal(p.activePanel.value, null); assert.equal(p.$select('operation-retry').length, 1)
    await p.$select('operation-retry')[0].props.onTap(); assert.equal(p.operationText.value, '清理已完成'); assert.deepEqual(calls, ['prepare', 'prepare', 'confirm', 'confirm', ['retry', 9]])
    assert.equal(h.requests.length, 0)
  })
  await check('所有权Preview实际service拒绝导出/清理/重试/下载，正常打开选择不发请求', async () => {
    const h = harness(); h.storage.delete('flashback:token'); h.storage.set('flashback:preview-session', { enabled: true })
    const p = h.page('user-center/data-backup'); await p.loadSummary(); assert.equal(p.totalRecords.value, 6)
    p.openPanel('export'); await p.startExport(); p.policy.value = 'FULL_CONTENT'; await p.startExport(); assert.equal(p.operation.value, null)
    p.closePanel(); p.openPanel('clear'); await p.requestClearConfirmation(); assert.equal(p.clearIntent.value, null)
    for (const call of [() => h.ownershipService.confirmDeletion(9, 'synthetic'), () => h.ownershipService.retry(9), () => h.ownershipService.download(9)]) await assert.rejects(call(), /只读/)
    assert.equal(h.requests.length, 0); assert.ok(h.toasts.every(x => x.includes('只读')))
  })
  await check('所有权未登录不读服务，加载失败可重试且无记录不能准备清理', async () => {
    const h = harness(); let fail = true, reads = 0, prepares = 0
    const p = h.page('user-center/data-backup', { dataOwnershipService: { summary: async () => { reads++; if (fail) throw Error('synthetic unavailable'); return { recordCounts: {}, mediaBytes: 0 } }, prepareDeletion: async () => { prepares++ } } })
    h.storage.delete('flashback:token'); await p.loadSummary(); assert.equal(reads, 0); assert.equal(h.navigation.at(-1), '/pages/login/index')
    h.storage.set('flashback:token', 'synthetic-token'); await p.loadSummary(); assert.equal(p.$select('summary-retry').length, 1); assert.equal(p.$select('clear-entry')[0].props.disabled, true)
    fail = false; await p.$select('summary-retry')[0].props.onTap(); p.openPanel('clear'); assert.equal(p.$select('prepare-clear')[0].props.disabled, true)
    await p.requestClearConfirmation(); assert.equal(prepares, 0); assert.equal(p.clearIntent.value, null); assert.equal(h.requests.length, 0)
    h.uni.navigateBack = options => options.fail(); p.goBack(); assert.equal(h.navigation.at(-1), '/pages/user-center/index')
  })
  const chapterDetail = () => ({
    id: 7, name: '固定合成篇章', note: '合成自述第一行\n合成自述第二行', status: 'ACTIVE', memberCount: 2,
    version: 12, createdAt: '2026-10-01T12:00:00', updatedAt: '2026-10-01T12:00:00',
    members: { list: [{ ...item(101), contentPreview: '合成摘要\n第二行' }, item(102, 'SEALED')], total: 2, pageNum: 1, pageSize: 50 },
  })
  const chapterSetup = (services = {}, detail = chapterDetail()) => {
    const h = harness(), reads = []
    const store = vue.reactive({ detail, loading: false, fetchDetail: async (...args) => { reads.push(args) } })
    const p = h.page('time-chapter-detail', services, store); p.chapterId.value = 7
    return { h, p, store, reads }
  }
  await check('篇章默认纸面使用真实自述/数量/信笺，长文、空态和已结束状态', async () => {
    const { p, store } = chapterSetup()
    assert.equal(p.$select('chapter-name')[0].children, '固定合成篇章')
    assert.equal(p.$select('chapter-note')[0].children, store.detail.note)
    assert.match(p.$select('chapter-meta')[0].children, /2 篇此刻/)
    assert.equal(p.$select('member-card').length, 2)
    assert.equal(p.$select('member-preview')[0].children, '合成摘要\n第二行')
    assert.equal(p.$select('chapter-action').length, 3)
    assert.equal(p.$select('chapter-edit-form').length, 0)
    assert.equal(p.$select('chapter-delete').length, 0)
    store.detail.name = '合成长名称'.repeat(20); store.detail.note = '合成长自述\n'.repeat(100)
    assert.equal(p.$select('chapter-name')[0].children, store.detail.name)
    assert.equal(p.$select('chapter-note')[0].children, store.detail.note)
    store.detail.status = 'ENDED'; store.detail.endedAt = '2026-10-02T12:00:00'
    assert.equal(p.$select('chapter-status')[0].children, '已结束')
    assert.equal(p.$select('add-entry')[0].props.disabled, true)
    assert.equal(p.$select('remove-entry')[0].props.disabled, false)
    assert.equal(p.$select('lifecycle-entry')[0].children, '重新打开')
    store.detail.memberCount = 0; store.detail.members.list = []; store.detail.members.total = 0; store.detail.note = null
    assert.equal(p.$select('chapter-note').length, 0); assert.equal(p.$select('chapter-empty').length, 1)
    assert.equal(p.$select('remove-entry')[0].props.disabled, true)
  })
  await check('篇章无效参数/跨id缓存/加载失败/认证隔离与分页排序接线', async () => {
    const { h, p, store, reads } = chapterSetup()
    h.hooks.onLoad({ id: 'invalid' }); assert.equal(p.loadFailed.value, true); assert.equal(reads.length, 0)
    store.detail.id = 8; assert.equal(p.chapter.value, null); store.loading = true; assert.equal(p.$select('chapter-state').length, 1)
    store.loading = false; store.detail = null; let failed = true
    store.fetchDetail = async (...args) => { reads.push(args); if (failed) throw Error('synthetic chapter failure'); store.detail = chapterDetail() }
    await p.retry(); assert.equal(p.loadFailed.value, true); assert.match(h.toasts.at(-1), /synthetic/)
    failed = false; await p.retry(); assert.equal(p.loadFailed.value, false); assert.equal(p.chapter.value.id, 7)
    p.openMore(); h.actionSheets.at(-1).success({ tapIndex: 1 }); await Promise.resolve()
    assert.equal(p.order.value, 'ASC'); assert.deepEqual(reads.at(-1), [7, 'ASC', 1, 50])
    store.detail.members.total = 3
    await p.loadMoreMembers(); assert.deepEqual(reads.at(-1), [7, 'ASC', 2, 50, true])
    const count = reads.length; store.loading = true; await p.loadMoreMembers(); assert.equal(reads.length, count)
    store.loading = false; p.openRecord(item(101)); p.openRecord(item(102, 'DRAFT')); p.closePage()
    assert.deepEqual(h.navigation.slice(-3), ['/pages/record-detail/index?id=101&source=archive', '/pages/record-editor/index?id=102&source=archive', 'back'])
    h.storage.delete('flashback:token'); await p.loadDetail(); assert.equal(h.navigation.at(-1), '/pages/login/index'); assert.equal(reads.length, count)
  })
  await check('篇章更多保留编辑/信息，保存失败保输入/版本，等待不可重复保存', async () => {
    const wait = deferred(), writes = []
    const { h, p, store } = chapterSetup({ timeChapterService: { update: (...args) => { writes.push(args); return wait.promise } } })
    p.openMore(); h.actionSheets.at(-1).success({ tapIndex: 2 })
    assert.match(h.modals.at(-1).content, /建立于/); assert.match(h.modals.at(-1).content, /覆盖时间待形成/)
    p.openMore(); h.actionSheets.at(-1).success({ tapIndex: 0 }); assert.equal(p.editing.value, true)
    p.editName.value = '  合成改名  '; p.editNote.value = ' 合成自述 '
    const work = p.submitEdit(); await p.submitEdit(); assert.equal(writes.length, 1)
    assert.equal(p.$select('chapter-actions').length, 0)
    assert.equal(writes[0][1].expectedVersion, 12); assert.equal(writes[0][1].name, '合成改名')
    assert.equal(store.detail.name, '固定合成篇章')
    wait.reject(Error('synthetic version conflict')); await work
    assert.equal(p.editing.value, true); assert.equal(p.editName.value, '  合成改名  ')
    p.cancelEdit(); assert.equal(p.editing.value, false); assert.equal(store.detail.name, '固定合成篇章')
  })
  await check('篇章结束/重开与删除仍明确确认、带版本且只删除篇章', async () => {
    const writes = []
    const { h, p, store } = chapterSetup({ timeChapterService: {
      end: async (id, data) => { writes.push(['end', id, data.expectedVersion]); store.detail.status = 'ENDED' },
      reopen: async (id, data) => { writes.push(['reopen', id, data.expectedVersion]); store.detail.status = 'ACTIVE' },
      delete: async (id, data) => { writes.push(['delete', id, data.expectedVersion]) },
    } })
    p.confirmLifecycle('ENDED'); h.modals.at(-1).success({ confirm: false }); assert.equal(writes.length, 0)
    p.confirmLifecycle('ENDED'); h.modals.at(-1).success({ confirm: true }); await new Promise(setImmediate)
    assert.equal(p.statusText.value, '已结束'); assert.deepEqual(writes[0], ['end', 7, 12])
    p.confirmLifecycle('ACTIVE'); h.modals.at(-1).success({ confirm: true }); await new Promise(setImmediate)
    assert.equal(p.statusText.value, '进行中')
    p.openMore(); h.actionSheets.at(-1).success({ tapIndex: 3 }); assert.match(h.modals.at(-1).content, /只删除篇章，不删除记录/)
    await h.modals.at(-1).success({ confirm: false }); assert.equal(writes.length, 2)
    p.confirmDelete(); await h.modals.at(-1).success({ confirm: true }); assert.deepEqual(writes[2], ['delete', 7, 12]); assert.equal(h.navigation.at(-1), 'back')
  })
  await check('篇章添加分页/失败重试，不含草稿或本篇章；转移必须确认，响应前无假归属', async () => {
    const wait = deferred(), queries = [], writes = []
    let fail = true
    const { h, p, store } = chapterSetup({
      recordService: { getRecordList: async (status, query) => {
        queries.push([status, query.pageNum]); if (fail) throw Error('synthetic picker failure')
        return query.pageNum === 1 ? { list: [item(201, 'DRAFT'), { ...item(202), chapter: { id: 7 } }], pageNum: 1, total: 3 }
          : { list: [{ ...item(203, 'UNLOCKED'), chapter: { id: 9, name: '合成来源篇章', status: 'ENDED' } }], pageNum: 2, total: 3 }
      } },
      timeChapterService: { addMembers: (...args) => { writes.push(args); return wait.promise } },
    })
    await p.$select('add-entry')[0].props.onTap(); assert.equal(p.memberMode.value, 'add'); assert.equal(writes.length, 0)
    assert.equal(p.candidateFailed.value, true); fail = false; await p.loadMorePicker()
    assert.equal(p.pickerRecords.value.length, 0); assert.equal(p.pickerHasMore.value, true)
    await p.loadMorePicker(); assert.deepEqual(queries, [['ALL', 1], ['ALL', 1], ['ALL', 2]])
    assert.equal(p.$select('picker-record').length, 1)
    p.chooseMember(p.pickerRecords.value[0]); assert.match(h.modals.at(-1).content, /合成来源篇章/); assert.equal(writes.length, 0)
    await h.modals.at(-1).success({ confirm: false }); assert.equal(p.memberMode.value, 'add')
    p.chooseMember(p.pickerRecords.value[0]); const work = h.modals.at(-1).success({ confirm: true })
    p.chooseMember(p.pickerRecords.value[0]); p.closeMemberPicker(); assert.equal(writes.length, 1)
    assert.equal(writes[0][0], 7); assert.equal(writes[0][1].expectedVersion, 12)
    assert.equal(writes[0][1].transfers[0].fromChapterId, 9); assert.equal(writes[0][1].recordIds[0], 203)
    assert.equal(store.detail.memberCount, 2); assert.equal(p.memberMode.value, 'add')
    wait.resolve({}); await work; assert.equal(p.memberMode.value, null); assert.equal(h.requests.length, 0)
  })
  await check('篇章已结束仍可移出：取消零写入、冲突重新读取、不乐观删记录', async () => {
    const writes = [], { h, p, store, reads } = chapterSetup({ timeChapterService: {
      removeMembers: async (...args) => { writes.push(args); throw Error('synthetic version conflict') },
    } })
    store.detail.status = 'ENDED'
    await p.openMemberPicker('add'); assert.equal(p.memberMode.value, null)
    await p.$select('remove-entry')[0].props.onTap(); assert.equal(p.pickerRecords.value.length, 2)
    p.chooseMember(p.pickerRecords.value[0]); assert.match(h.modals.at(-1).content, /不会删除记录/)
    await h.modals.at(-1).success({ confirm: false }); assert.equal(writes.length, 0)
    p.chooseMember(p.pickerRecords.value[0]); await h.modals.at(-1).success({ confirm: true })
    assert.equal(writes.length, 1); assert.equal(writes[0][1].expectedVersion, 12); assert.equal(writes[0][1].recordIds[0], 101)
    assert.equal(store.detail.members.list.length, 2); assert.equal(p.memberMode.value, null)
    assert.equal(reads.length, 1); assert.match(h.toasts.at(-1), /conflict/); assert.equal(h.requests.length, 0)
  })
  await check('篇章Preview只读、实际service拒绝所有成员/生命周期/编辑删除变更', async () => {
    const { h, p } = chapterSetup()
    h.storage.delete('flashback:token'); h.storage.set('flashback:preview-session', { enabled: true })
    await p.openMemberPicker('add'); await p.openMemberPicker('remove')
    p.openMore(); h.actionSheets.at(-1).success({ tapIndex: 0 })
    p.confirmLifecycle('ENDED'); p.confirmDelete()
    assert.equal(p.editing.value, false); assert.equal(p.memberMode.value, null); assert.equal(h.modals.length, 0)
    for (const action of ['update', 'addMembers', 'removeMembers', 'end', 'reopen', 'delete']) await assert.rejects(h.chapterService[action](7, { expectedVersion: 12 }), /只读/)
    assert.equal(h.requests.length, 0); assert.ok(h.toasts.every(x => x.includes('只读')))
  })
  await check('篇章加入和移出成功后只靠重新查询更新，普通加入没有隐式转移', async () => {
    const writes = [], current = chapterDetail()
    const { h, p, store } = chapterSetup({
      recordService: { getRecordList: async () => ({ list: [item(301)], total: 1, pageNum: 1 }) },
      timeChapterService: {
        addMembers: async (id, data) => { writes.push(['add', id, data]); current.memberCount = 3; current.version = 13; current.members.list.push(item(301)); current.members.total = 3 },
        removeMembers: async (id, data) => { writes.push(['remove', id, data]); current.memberCount = 2; current.version = 14; current.members.list = current.members.list.filter(row => row.id !== data.recordIds[0]); current.members.total = 2 },
      },
    })
    store.fetchDetail = async () => { store.detail = JSON.parse(JSON.stringify(current)) }
    await p.openMemberPicker('add'); p.chooseMember(p.pickerRecords.value[0])
    assert.equal(writes.length, 0); await h.modals.at(-1).success({ confirm: true })
    assert.equal(writes[0][2].transfers.length, 0); assert.equal(p.chapter.value.version, 13); assert.match(p.$select('chapter-meta')[0].children, /3 篇此刻/)
    await p.openMemberPicker('remove'); p.chooseMember(p.pickerRecords.value.find(row => row.id === 301))
    await h.modals.at(-1).success({ confirm: true })
    assert.equal(writes[1][2].expectedVersion, 13); assert.equal(p.chapter.value.version, 14)
    assert.equal(p.members.value.length, 2); assert.equal(p.memberMode.value, null); assert.equal(h.navigation.length, 0)
  })
  await check('篇章候选等待防重/禁止误关闭，关闭零写入；失败状态不串到移出面板', async () => {
    const wait = deferred(); let queries = 0
    const { h, p } = chapterSetup({ recordService: { getRecordList: () => { queries++; return wait.promise } } })
    const work = p.openMemberPicker('add'); await p.openMemberPicker('add'); await p.loadMorePicker(); p.closeMemberPicker()
    assert.equal(queries, 1); assert.equal(p.memberMode.value, 'add')
    wait.reject(Error('synthetic initial failure')); await work; assert.equal(p.candidateFailed.value, true)
    p.closeMemberPicker(); assert.equal(p.memberMode.value, null)
    await p.openMemberPicker('remove'); assert.equal(p.candidateFailed.value, false); assert.equal(p.$select('picker-more').length, 0)
    p.closeMemberPicker(); assert.equal(h.requests.length, 0); assert.equal(h.modals.length, 0)
  })
  await check('近况书信真实响应后打开，正文与服务端日期保留，收起/重开不重复生成', async () => {
    const h = harness(); let generated = 0
    const result = { status: 'SUCCESS', summary: '合成书信第一段\n\n合成书信第二段。'.repeat(50), generatedAt: '2026-10-12T09:30:00', recordCount: 6, unlockedCount: 2, lifeNodeCount: 1 }
    const p = h.page('user-center', { stageSummaryService: { generate: async () => { generated++; return result } } })
    assert.equal(p.$select('summary-layer').length, 0)
    await p.openStageSummary()
    assert.equal(generated, 1); assert.equal(p.$select('summary-sheet').length, 1)
    assert.equal(p.$select('stage-summary-text')[0].children, result.summary)
    assert.match(p.$select('summary-date')[0].children, /2026/); assert.match(p.$select('stage-summary-meta')[0].children, /6 条记录.*2 次抵达.*1 个节点.*AI 整理/)
    assert.equal(p.$select('scroll-body')[0].props['scroll-y'], false)
    assert.equal(p.$select('summary-title')[0].children, '近况')
    let stopped = false; p.$select('summary-sheet')[0].props.onTap({ stopPropagation() { stopped = true } })
    assert.equal(stopped, true); assert.equal(p.summaryOpen.value, true)
    p.$select('summary-close')[0].props.onTap(); assert.equal(p.$select('summary-layer').length, 0)
    assert.equal(p.$select('scroll-body')[0].props['scroll-y'], true)
    assert.equal(p.stageSummary.value.summary, result.summary)
    await p.openStageSummary(); assert.equal(generated, 1); assert.equal(p.$select('summary-scroll')[0].key, 1)
    p.$select('summary-layer')[0].props.onTap(); assert.equal(p.summaryOpen.value, false)
    h.storage.delete('flashback:token'); h.storage.set('flashback:preview-session', { enabled: true })
    await p.openStageSummary(); assert.equal(p.summaryOpen.value, false); assert.equal(generated, 1)
    assert.equal(h.requests.length, 0); assert.equal(h.navigation.length, 0)
  })
  await check('近况重新整理明确触发、防重与忙碌关闭保护，失败保留旧信，fallback不假标AI', async () => {
    const h = harness(), wait = deferred(); let count = 0
    let generate = async () => { throw Error('synthetic initial failure') }
    const p = h.page('user-center', { stageSummaryService: { generate: async () => { count++; return generate() } } })
    await p.openStageSummary(); assert.equal(p.summaryOpen.value, false); assert.equal(p.stageSummary.value, null)
    generate = async () => ({ status: 'SUCCESS', summary: '合成旧信', generatedAt: '2026-10-10T10:00:00', recordCount: 1, unlockedCount: 0, lifeNodeCount: 0 })
    await p.openStageSummary(); const oldDate = p.$select('summary-date')[0].children
    generate = () => wait.promise
    const work = p.$select('summary-regenerate')[0].props.onTap()
    await p.generateStageSummary(); p.closeStageSummary(); p.$select('summary-layer')[0].props.onTap()
    assert.equal(count, 3); assert.equal(p.summaryOpen.value, true)
    assert.equal(p.$select('summary-regenerate')[0].props.disabled, true); assert.equal(p.$select('summary-close')[0].props.disabled, true)
    assert.equal(p.$select('stage-summary-text')[0].children, '合成旧信')
    wait.reject(Error('synthetic timeout')); await work
    assert.equal(p.$select('stage-summary-text')[0].children, '合成旧信'); assert.equal(p.$select('summary-date')[0].children, oldDate)
    assert.equal(p.summaryRevision.value, 1); assert.equal(p.summaryLoading.value, false)
    generate = async () => ({ status: 'FALLBACK', summary: '合成本地整理', recordCount: 0, unlockedCount: 0, lifeNodeCount: 0 })
    await p.$select('summary-regenerate')[0].props.onTap()
    assert.equal(p.$select('summary-date').length, 0); assert.match(p.$select('stage-summary-meta')[0].children, /本地整理/)
    assert.equal(p.$select('summary-scroll')[0].key, 2); assert.equal(h.requests.length, 0)
  })
  console.log('OFFLINE ' + passed + '/' + passed + ' PASS; real network/provider/storage calls=0; WeChat rendering unverified')
}
main().catch(error => { console.error(error); process.exitCode = 1 })
