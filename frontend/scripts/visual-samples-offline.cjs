// 固定合成状态、零网络；执行真实 SFC 脚本及记录服务，不代替微信渲染。
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse } = require('vue/compiler-sfc')
const root = path.resolve(__dirname, '..')
const src = path.join(root, 'src')
let passed = 0

function harness() {
  const storage = new Map([['flashback:token', 'synthetic-offline-token']])
  const requests = [], toasts = [], navigation = [], hooks = {}
  let response = () => ({ code: 0, data: {} })
  const uni = {
    getStorageSync: key => storage.get(key),
    setStorageSync: (key, value) => storage.set(key, value),
    removeStorageSync: key => storage.delete(key),
    showToast: x => toasts.push(x.title),
    reLaunch: x => navigation.push(x.url),
    navigateTo: x => navigation.push(x.url),
    switchTab: x => navigation.push(x.url),
    hideTabBar: () => {},
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
    vm.runInNewContext(js, { module: mod, exports: mod.exports, require: localRequire, uni, __env: { VITE_PREVIEW_MODE: 'true' } }, { filename: path.relative(root, file) })
    return mod.exports
  }
  const service = moduleFrom(path.join(src, 'services/recordService.ts')).recordService
  const preview = moduleFrom(path.join(src, 'features/preview/preview-session.ts'))
  const mutation = moduleFrom(path.join(src, 'features/data-ownership/mutation-state.ts'))
  const utils = moduleFrom(path.join(src, 'utils/index.ts'))
  function page(name, services = {}, store = {}) {
    const file = path.join(src, 'pages', name, 'index.vue')
    const descriptor = parse(fs.readFileSync(file, 'utf8')).descriptor
    const exports = name === 'home'
      ? 'loadHomeSummary, draftState, sealedState, unlockedState, latestDraft, draftCount, latestSealed, showArrivalCard, goDraftEntry, goEditor'
      : name === 'timeline'
        ? 'loadTimeline, timelineGroups, timelineLoadFailed, loadMoreFailed, currentPage, loading, loadingMore, appliedFilters, hasMore, decoratedGroups, applyFilters, draftTagId, availableTags, filterPanelVisible, filterApplyFailed, openNode'
        : 'form, saveDraft, savedFeedback, recordStatus, loading, recordId, pendingImageUploads, pendingVoiceUploads, attachments, hasDirtyChanges, markSnapshot'
    const source = descriptor.scriptSetup.content.replaceAll('import.meta.env', '__env') + '\nglobalThis.surface = {' + exports + '}\n'
    const js = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 } }).outputText
    const deps = spec => {
      if (spec === 'vue') return { ...vue, onUnmounted() {} }
      if (spec === '@dcloudio/uni-app') return Object.fromEntries(['onShow', 'onHide', 'onLoad', 'onUnload'].map(key => [key, fn => { hooks[key] = fn }]))
      if (spec.endsWith('.vue')) return { default: {} }
      if (spec === '../../services') return { recordService: service, tagService: { getTags: async () => [] }, ...services }
      if (spec === '../../stores') return { useRecordStore: () => store, useTagStore: () => ({}), useAgentChatStore: () => ({}) }
      if (spec === '../../types') return moduleFrom(path.join(src, 'types/index.ts'))
      if (spec === '../../utils') return utils
      if (spec.endsWith('preview-session')) return preview
      if (spec.endsWith('useRecordCoverUrls')) return { useRecordCoverUrls: () => ({ coverUrls: {}, coverErrors: {}, loadCovers: async () => {}, markCoverFailed() {} }) }
      throw new Error('Unexpected page dependency: ' + spec)
    }
    const sandbox = { exports: {}, require: deps, uni, __env: {}, setInterval: () => 0, clearInterval() {}, setTimeout, clearTimeout }
    vm.runInNewContext(js, sandbox, { filename: name + '/index.vue' })
    return sandbox.surface
  }
  return { page, service, mutation, storage, requests, toasts, navigation, setResponse: fn => { response = fn } }
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
  console.log('OFFLINE ' + passed + '/' + passed + ' PASS; real network/provider/storage calls=0; WeChat rendering unverified')
}
main().catch(error => { console.error(error); process.exitCode = 1 })
