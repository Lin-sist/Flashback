// 构建 CSS 与实际顶部组件几何回归；不读取截图/真实记录，不代替微信绘制。
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const { execFileSync } = require('node:child_process')
const ts = require('typescript')
const vue = require('vue')
const { parse } = require('vue/compiler-sfc')
const postcss = require(require.resolve('postcss', { paths: [path.dirname(require.resolve('vite/package.json'))] }))
const root = path.resolve(__dirname, '..')
const dist = path.join(root, 'dist/build/mp-weixin')
const baseline = process.argv[2] === '--baseline' ? process.argv[3] : null
function sourceFile(file) {
  return baseline
    ? execFileSync('git', ['show', baseline + ':frontend/src/' + file], { cwd: root, encoding: 'utf8', stdio: ['pipe', 'pipe', 'ignore'] })
    : fs.readFileSync(path.join(root, 'src', file), 'utf8')
}
let failures = 0
function check(name, fn) {
  try { fn(); console.log('PASS ' + name) }
  catch (error) { failures++; console.error('FAIL ' + name + ': ' + error.message) }
}
function css(file) {
  if (!baseline) return postcss.parse(fs.readFileSync(path.join(dist, file), 'utf8'))
  // 旧提交源码作差分；当前版本检查真实构建 WXSS，两者都不冒充原生绘制。
  const source = file === 'app.wxss'
    ? sourceFile('styles/tokens.css') + parse(sourceFile('App.vue')).descriptor.styles[0].content
    : parse(sourceFile(file.replace(/\.wxss$/, '.vue'))).descriptor.styles[0].content
  return postcss.parse(source)
}
function property(file, selector, prop) {
  let value
  css(file).walkRules(rule => {
    if (rule.selectors.some(s => s.replace(/\.data-v-[\w-]+/g, '') === selector)) {
      rule.walkDecls(prop, decl => { value = decl.value })
    }
  })
  assert.ok(value, file + ' missing ' + selector + ' ' + prop)
  return value
}
// CSS 自定义变量缺失时只允许有效备用值；RGBA/阴影的内层括号不能截断。
function withoutTokens(value) {
  while (value.includes('var(')) {
    const start = value.indexOf('var(')
    let depth = 1, end = start + 4, comma = -1
    for (; end < value.length; end++) {
      if (value[end] === '(') depth++
      if (value[end] === ')' && --depth === 0) break
      if (value[end] === ',' && depth === 1 && comma < 0) comma = end
    }
    if (comma < 0) return null
    const fallback = value.slice(comma + 1, end).trim()
    if (!fallback) return null
    value = value.slice(0, start) + fallback + value.slice(end + 1)
  }
  return value
}
check('app.wxss 的 token 声明不混入 Web 根选择器', () => {
  let declarations = 0
  css('app.wxss').walkRules(rule => {
    if (rule.nodes.some(n => n.type === 'decl' && n.prop.startsWith('--fb-'))) {
      assert.ok(rule.selectors.every(s => s === 'page' || /^\.[\w-]+$/.test(s)), 'unsupported token selector ' + rule.selector)
      declarations++
    }
  })
  assert.ok(declarations > 0)
})
for (const [page, selector, prop, expected] of [
  ['home', '.page', 'background', '#faf7f2'],
  ['home', '.capture', 'background', '#f3eee4'],
  ['home', '.bottom-nav', 'background', '#f7f2e9'],
  ['record-editor', '.letter-body', 'background', '#faf7f2'],
  ['record-editor', '.seal-btn', 'background', '#f3eee4'],
  ['timeline', '.page', 'background', '#faf7f2'],
  ['timeline', '.bottom-nav', 'background', '#f7f2e9'],
]) {
  check(page + ' ' + selector + ' 变量不可用时保留纸/动作底色', () => {
    assert.equal(withoutTokens(property('pages/' + page + '/index.wxss', selector, prop)), expected)
  })
}
check('变量不可用时首页边线/投影/宋体仍有有效备用值', () => {
  for (const [selector, prop] of [['.capture', 'border'], ['.capture', 'box-shadow'], ['.page', 'font-family']]) {
    assert.ok(withoutTokens(property('pages/home/index.wxss', selector, prop)), selector + ' ' + prop + ' lost')
  }
})
check('无媒体仍有可输入书写空间，真实媒体可占用纸面且不叠加虚构占位', () => {
  const file = 'pages/record-editor/index.wxss'
  const pixels = (value, width, height) => {
    const expression = value.replace(/calc\(/g, '(').replace(/([\d.]+)vh/g, (_, n) => String(Number(n) * height / 100)).replace(/([\d.]+)rpx/g, (_, n) => String(Number(n) * width / 750)).replace(/px/g, '')
    assert.ok(/^[\d\s().+*/-]+$/.test(expression))
    return Function('return ' + expression)()
  }
  for (const [width, height] of [[320,568], [375,812], [390,844], [430,932]]) {
    const field = pixels(property(file, '.editor-field', 'min-height'), width, height)
    const mediaField = pixels(property(file, '.editor-field--with-media', 'min-height'), width, height)
    assert.ok(field >= height * .3 && field <= height * .48, 'editable space outside usable range')
    assert.ok(mediaField < field && mediaField >= 75, 'real media layout has no readable text area')
    assert.equal(property(file, '.letter-content', 'min-height'), '0')
  }
})
check('详情公共按钮在变量不可用时仍有可见背景', () => {
  assert.equal(withoutTokens(property('components/common/PrimaryButton.wxss', '.primary-btn', 'background')), '#b5352a')
})
check('书写工具与保存按钮可在窄屏并排，且底部操作仍在正常流中', () => {
  const file = 'pages/record-editor/index.wxss'
  const padding = property(file, '.page-body', 'padding').split(/\s+/)
  const available = 750 - 2 * parseFloat(padding[1])
  const tools = parseFloat(property(file, '.attach-bar', 'min-width'))
  const save = parseFloat(property(file, '.seal-btn', 'width'))
  const gap = parseFloat(property(file, '.editor-toolbar', 'gap'))
  assert.ok(tools + save + gap <= available, 'toolbar exceeds content width')
  assert.equal(property(file, '.editor-toolbar', 'flex-wrap'), 'wrap')
  assert.ok(!/\.(?:bottom-area|editor-toolbar)[^{]*\{[^}]*position:(?:fixed|absolute)/.test(fs.readFileSync(path.join(dist, file), 'utf8')))
})
check('首页已留下使用整宽轻纸卡，长标题省略且动作不收缩', () => {
  const file = 'pages/home/index.wxss'
  assert.equal(property(file, '.saved-entry', 'width'), '100%')
  assert.equal(property(file, '.saved-entry', 'box-sizing'), 'border-box')
  assert.equal(withoutTokens(property(file, '.saved-entry', 'background')), '#fcf9f4')
  assert.equal(property(file, '.saved-entry-title', 'text-overflow'), 'ellipsis')
  assert.equal(property(file, '.saved-entry .entry-action', 'flex-shrink'), '0')
})
check('已留下工具独立一行，双动作同宽同高并留出间隔', () => {
  const file = 'pages/record-editor/index.wxss'
  assert.equal(property(file, '.editor-toolbar--saved', 'flex-direction'), 'column')
  assert.equal(property(file, '.editor-toolbar--saved .editor-actions', 'width'), '100%')
  assert.equal(property(file, '.editor-toolbar--saved .seal-btn', 'flex'), '1')
  assert.equal(property(file, '.editor-toolbar--saved .seal-btn', 'min-height'), '48px')
  const gap = parseFloat(property(file, '.editor-toolbar--saved .editor-actions', 'gap'))
  for (const width of [320,375,390,430]) {
    const available = width * (750 - 128) / 750
    const button = (available - gap * width / 750) / 2
    assert.ok(button >= 4 * 28 * width / 750 + 2 * 24 * width / 750 + 22 * width / 750, 'saved action content clipped at ' + width)
  }
})
check('封存确认有静态遮罩和可滚动纸面，滚轮与确认不同时遮挡', () => {
  const file = 'pages/record-editor/index.wxss'
  assert.equal(property(file, '.seal-layer', 'position'), 'fixed')
  assert.equal(property(file, '.seal-panel', 'overflow-y'), 'auto')
  assert.match(property(file, '.seal-panel', 'padding'), /safe-area-inset-bottom/)
  assert.ok(property(file, '.seal-mask', 'background'))
  assert.match(sourceFile('pages/record-editor/index.vue'), /v-if="showSealSheet && !showUnlockPicker && recordStatus === RecordStatus.SAVED"/)
})
const source = parse(sourceFile('components/common/AppTopSafeBar.vue')).descriptor.scriptSetup.content
function titleGeometry(width, leftSlot, rightSlot, paddingX, sideWidth, centerTitle = false) {
  const script = ts.transpileModule(source + '\nglobalThis.testStyles = {titleStyle, rightRailStyle};', {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 },
  }).outputText
  const capsuleReserve = 96
  const sandbox = {
    exports: {},
    defineProps: () => ({ paddingX, sideWidth, centerTitle, title: '时光回序' }),
    withDefaults: p => p,
    require: spec => spec === 'vue'
      ? { ...vue, useSlots: () => ({ ...(leftSlot ? { left() {} } : {}), ...(rightSlot ? { right() {} } : {}) }) }
      : { useWechatNavMetrics: () => ({ navBarHeight: vue.ref(44), navBarTotalHeight: vue.ref(64), rightSafeWidth: vue.ref(capsuleReserve), statusBarHeight: vue.ref(20), windowWidth: vue.ref(width) }) },
  }
  vm.runInNewContext(script, sandbox)
  const style = sandbox.testStyles.titleStyle.value
  const px = value => {
    const expression = value.replace(/calc\(/g, '(').replace(/([\d.]+)rpx/g, (_, n) => String(Number(n) * width / 750)).replace(/([\d.]+)%/g, (_, n) => String(Number(n) * width / 100)).replace(/px/g, '')
    assert.ok(/^[\d\s().+*/-]+$/.test(expression))
    return Function('return ' + expression)()
  }
  const titleWidth = style.width && style.width !== 'auto' ? px(style.width) : width - px(style.left) - px(style.right)
  const required = (4 * 34 + 4 * 4) * width / 750
  assert.ok(titleWidth >= required, width + 'px title region ' + titleWidth.toFixed(1) + 'px < ' + required.toFixed(1) + 'px')
  if (style.right) assert.ok(px(style.right) >= capsuleReserve, 'title overlaps capsule')
  if (leftSlot && style.left) assert.ok(px(style.left) >= px(paddingX) + px(sideWidth), 'title overlaps left action')
  if (centerTitle) {
    const center = px(style.left) + titleWidth / 2
    assert.ok(Math.abs(center - width / 2) < 0.1, 'title is not centered in the screen')
  }
}
for (const width of [320, 360, 375, 390, 430]) {
  check(width + 'px 首页/筛选标题可容纳四字且避开操作与胶囊', () => {
    titleGeometry(width, false, false, '56rpx', '88rpx', true)
    titleGeometry(width, true, false, '56rpx', '104rpx', true)
    titleGeometry(width, true, true, '40rpx', '88rpx')
  })
}
check('Agent消息增长只滚动消息区，不挤走输入栏', () => {
  const file = 'pages/record-editor/components/AgentChatSheet.wxss'
  assert.match(property(file, '.agent-sheet', 'height'), /^\d+vh$/)
  assert.equal(property(file, '.message-list', 'height'), '0')
  assert.equal(property(file, '.message-list', 'min-height'), '0')
  assert.equal(property(file, '.composer', 'flex-shrink'), '0')
  assert.equal(property(file, '.sheet-head', 'flex-shrink'), '0')
  assert.equal(property(file, '.send-btn', 'flex-shrink'), '0')
})
check('Agent遮罩与纸底不依赖滤镜，输入区保留底部安全区', () => {
  for (const name of ['AgentChatSheet', 'AgentIntentChooser']) {
    const tree = css('pages/record-editor/components/' + name + '.wxss')
    tree.walkDecls(/backdrop-filter/, () => { throw new Error('native sheet requires blur') })
  }
  assert.match(property('pages/record-editor/components/AgentChatSheet.wxss', '.agent-sheet', 'padding'), /safe-area-inset-bottom/)
  assert.match(property('pages/record-editor/components/AgentIntentChooser.wxss', '.intent-panel', 'padding'), /safe-area-inset-bottom/)
})
check('封存霜卡整宽渐进遮盖、标题和参考居中且仅装饰可柔化', () => {
  const file = 'pages/record-detail/index.wxss'
  assert.equal(property(file, '.sealed-card', 'width'), '100%')
  assert.equal(property(file, '.sealed-card', 'box-sizing'), 'border-box')
  assert.equal(property(file, '.sealed-media', 'width'), '100%')
  assert.equal(property(file, '.sealed-quote__text', 'text-align'), 'center')
  assert.equal(property(file, '.sealed-reference-toggle', 'justify-content'), 'center')
  assert.match(property(file, '.sealed-frost', 'background'), /gradient/)
  assert.equal(property(file, '.sealed-frost', 'top'), '0')
  assert.equal(property(file, '.sealed-meta__location', 'white-space'), 'nowrap')
  assert.equal(property(file, '.sealed-meta__location', 'text-overflow'), 'ellipsis')
  css(file).walkRules(rule => {
    if (rule.selector.includes('.sealed-')) rule.walkDecls(/filter/, () => { assert.ok(rule.selector.includes('.sealed-quote__text') || rule.selector.includes('.sealed-body-ink'), 'blur outside title/decorative ink') })
  })
})
check('封存媒体可横排且窄屏换行，不因组件host缩成窄列', () => {
  const file = 'pages/record-detail/components/ReadOnlyRecordMedia.wxss'
  assert.equal(property(file, '.readonly-media--sealed', 'box-sizing'), 'border-box')
  assert.equal(property(file, '.readonly-media--sealed .readonly-fragments', 'flex-wrap'), 'wrap')
  const thumb = parseFloat(property(file, '.readonly-media--sealed .readonly-image-item', 'width'))
  const gap = parseFloat(property(file, '.readonly-media--sealed .readonly-fragments', 'gap'))
  assert.ok(2 * thumb + 2 * gap + 220 <= 750 - 2 * 56, 'two photos and voice exceed paper width')
})
check('回看标题居中、正文保留换行、媒体host整宽与回应正常流', () => {
  const file = 'pages/record-detail/index.wxss'
  assert.equal(property(file, '.archive-page--review .unlock-quote', 'text-align'), 'center')
  assert.equal(property(file, '.archive-page--review .unlock-card-text', 'white-space'), 'pre-wrap')
  assert.equal(property(file, '.unlock-media', 'width'), '100%')
  assert.equal(property(file, '.unlock-cta', 'position'), 'relative')
  assert.equal(property(file, '.archive-page--review .unlock-cta', 'border-radius'), '50rpx')
})
check('回看媒体独立双列，不改SAVED默认三列或封存排版', () => {
  const file = 'pages/record-detail/components/ReadOnlyRecordMedia.wxss'
  assert.match(property(file, '.readonly-media--review .readonly-image-grid', 'grid-template-columns'), /repeat\(2,/)
  assert.match(property(file, '.readonly-image-grid', 'grid-template-columns'), /repeat\(3,/)
  assert.equal(property(file, '.readonly-media--review .readonly-image-item', 'height'), '184rpx')
  assert.equal(property(file, '.readonly-media--review', 'box-sizing'), 'border-box')
})
if (failures) { console.error('STYLE REGRESSION failures=' + failures); process.exitCode = 1 }
else console.log('STYLE REGRESSION PASS; compiled CSS/geometry only, WeChat drawing still requires manual confirmation')
