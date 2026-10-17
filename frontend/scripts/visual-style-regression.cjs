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
    const normalize = s => s.replace(/\.data-v-[\w-]+/g, '').replace(/::(before|after)\b/g, ':$1')
    if (rule.selectors.some(s => normalize(s) === normalize(selector))) {
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
  ['home', '.bottom-nav', 'background', 'rgba(250,247,242,.64)'],
  ['record-editor', '.letter-body', 'background', '#faf7f2'],
  ['record-editor', '.seal-btn', 'background', '#f3eee4'],
  ['timeline', '.page', 'background', '#faf7f2'],
  ['timeline', '.bottom-nav', 'background', 'rgba(250,247,242,.64)'],
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
check('书写工具可放入窄屏，且底部双入口仍在正常流中', () => {
  const file = 'pages/record-editor/index.wxss'
  const padding = property(file, '.page-body', 'padding').split(/\s+/)
  const available = 750 - 2 * parseFloat(padding[1])
  const tools = parseFloat(property(file, '.attach-bar', 'min-width'))
  assert.ok(tools <= available, 'tools exceed content width')
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
check('新建/恢复/已留下工具独立一行，双动作同宽同高并容纳最长文案', () => {
  const file = 'pages/record-editor/index.wxss'
  assert.equal(property(file, '.editor-toolbar--dual', 'flex-direction'), 'column')
  assert.equal(property(file, '.editor-toolbar--dual .editor-actions', 'width'), '100%')
  assert.equal(property(file, '.editor-toolbar--dual .seal-btn', 'flex'), '1')
  assert.equal(property(file, '.editor-toolbar--dual .seal-btn', 'min-height'), '48px')
  const gap = parseFloat(property(file, '.editor-toolbar--dual .editor-actions', 'gap'))
  for (const width of [320,375,390,430]) {
    const available = width * (750 - 128) / 750
    const button = (available - gap * width / 750) / 2
    assert.ok(button >= 6 * 28 * width / 750 + 2 * 24 * width / 750 + 24 * width / 750, 'writing action content clipped at ' + width)
  }
})
check('封存确认有静态遮罩和可滚动纸面，滚轮与确认不同时遮挡', () => {
  const file = 'pages/record-editor/index.wxss'
  assert.equal(property(file, '.seal-layer', 'position'), 'fixed')
  assert.equal(property(file, '.seal-panel', 'overflow-y'), 'auto')
  assert.match(property(file, '.seal-panel', 'padding'), /safe-area-inset-bottom/)
  assert.ok(property(file, '.seal-mask', 'background'))
  assert.match(sourceFile('pages/record-editor/index.vue'), /v-if="showSealSheet && !showUnlockPicker && canPrepareSeal"/)
})
const source = parse(sourceFile('components/common/AppTopSafeBar.vue')).descriptor.scriptSetup.content
function titleGeometry(width, leftSlot, rightSlot, paddingX, sideWidth, centerTitle = false, requiredRpx = 4 * 34 + 4 * 4, capsuleReserve = 96) {
  const script = ts.transpileModule(source + '\nglobalThis.testStyles = {titleStyle, rightRailStyle};', {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 },
  }).outputText
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
  const required = requiredRpx * width / 750
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
check('回看长对话只滚动消息区，底部输入/授权与安全区不被挤走', () => {
  const file = 'pages/record-detail/components/ReviewChatSheet.wxss'
  assert.equal(property(file, '.review-sheet', 'box-sizing'), 'border-box')
  assert.equal(property(file, '.message-list', 'min-height'), '0')
  assert.equal(property(file, '.message-list', 'height'), '0')
  for (const selector of ['.composer', '.memory-consent', '.sheet-head', '.sheet-handle', '.composer-send', '.privacy-note']) {
    assert.equal(property(file, selector, 'flex-shrink'), '0')
  }
  assert.match(property(file, '.review-sheet', 'padding'), /safe-area-inset-bottom/)
  assert.equal(property(file, '.composer-input-bar', 'min-width'), '0')
  assert.equal(property(file, '.composer-input', 'width'), '0')
  assert.equal(property(file, '.source-label', 'overflow-wrap'), 'anywhere')
  css(file).walkDecls(/backdrop-filter/, () => { throw new Error('review requires blur') })
})
check('回看原生发送图标与纸条折角进入构建，消息来源不再成为横向列', () => {
  const file = 'pages/record-detail/components/ReviewChatSheet.wxss'
  assert.equal(property(file, '.message-row', 'flex-direction'), 'column')
  assert.equal(property(file, '.message-bubble', 'box-sizing'), 'border-box')
  assert.equal(property(file, '.message-bubble::after', 'pointer-events'), 'none')
  assert.equal(property(file, '.source-chip', 'max-width'), '100%')
  for (const part of ['top', 'right', 'tail', 'left', 'fold']) assert.match(property(file, '.plane-line--' + part, 'transform'), /rotate/)
})
check('个人中心按390px demo转译头像/边距/入口比例，模糊只允许导航增强', () => {
  const file = 'pages/user-center/index.wxss'
  const px = rpx => parseFloat(rpx) * 390 / 750
  assert.ok(Math.abs(px(property(file, '.avatar-wrap', 'width')) - 100) < 1)
  assert.ok(Math.abs(px(property(file, '.center-content', 'padding').split(/\s+/)[1]) - 28) < 1)
  assert.ok(Math.abs(px(property(file, '.entrance', 'min-height')) - 58) < 1)
  assert.ok(Math.abs(px(property(file, '.entrance', 'font-size')) - 16) < 1)
  assert.equal(property(file, '.entrance', 'width'), '100%')
  assert.equal(property(file, '.entrance', 'box-shadow'), 'none')
  assert.equal((property(file, '.entrance', 'background-image').match(/linear-gradient/g) || []).length, 8)
  for (const selector of ['.page', '.avatar', '.voucher', '.bottom-nav']) assert.ok(property(file, selector, 'background'))
  css(file).walkRules(rule => rule.walkDecls(/backdrop-filter/, () => {
    assert.ok(rule.selectors.every(s => s.replace(/\.data-v-[\w-]+/g, '') === '.bottom-nav'), 'blur outside navigation')
    assert.equal(rule.parent.type, 'atrule'); assert.equal(rule.parent.name, 'supports')
  }))
})
check('个人中心昵称/计数可换行，托管卡与三Tab窄屏可达且留出真实安全区', () => {
  const file = 'pages/user-center/index.wxss'
  assert.equal(property(file, '.scroll-body', 'height'), '0')
  assert.equal(property(file, '.scroll-body', 'min-height'), '0')
  assert.equal(property(file, '.profile-name', 'overflow-wrap'), 'anywhere')
  assert.equal(property(file, '.custody', 'overflow-wrap'), 'anywhere')
  assert.equal(property(file, '.voucher-copy', 'min-width'), '0')
  assert.equal(property(file, '.bottom-nav-shell', 'flex-shrink'), '0')
  assert.match(property(file, '.bottom-nav', 'padding-bottom'), /safe-area-inset-bottom/)
  assert.equal(property(file, '.nav-item', 'min-width'), '0')
  assert.equal(property(file, '.bottom-nav', 'border-radius'), '24rpx 24rpx 0 0')
  const actions = 2 * parseFloat(property(file, '.voucher-button', 'min-width')) + parseFloat(property(file, '.voucher-actions', 'gap'))
  assert.ok(actions < 750 - 108 - 84, 'voucher actions exceed card width')
})
check('所有权390px demo的页边/印章/导出按钮比例与四条括线进入静态WXSS', () => {
  const file = 'pages/user-center/data-backup/index.wxss'
  const px = rpx => parseFloat(rpx) * 390 / 750
  assert.ok(Math.abs(px(property(file, '.ownership-content', 'padding').split(/\s+/)[1]) - 28) < 1)
  assert.ok(Math.abs(px(property(file, '.custody-seal', 'width')) - 56) < 1)
  assert.ok(Math.abs(px(property(file, '.export-entry', 'min-height')) - 52) < 1)
  assert.equal((property(file, '.export-entry', 'background-image').match(/linear-gradient/g) || []).length, 4)
  assert.equal(property(file, '.voucher', 'border-radius'), '2rpx')
  assert.equal(property(file, '.voucher-fold', 'pointer-events'), 'none')
  assert.ok(property(file, '.voucher-redline', 'background'))
  css(file).walkDecls(/backdrop-filter/, () => { throw Error('ownership requires backdrop blur') })
})
check('所有权主页面/操作面板可滚动，页尾与确认输入留安全区，标题按胶囊预留居中', () => {
  const file = 'pages/user-center/data-backup/index.wxss'
  for (const selector of ['.scroll', '.panel-scroll']) {
    assert.equal(property(file, selector, 'height'), '0'); assert.equal(property(file, selector, 'min-height'), '0')
  }
  assert.equal(property(file, '.ownership-content', 'min-height'), '100%')
  assert.equal(property(file, '.motto', 'margin-top'), 'auto')
  assert.match(property(file, '.motto', 'padding'), /safe-area-inset-bottom/)
  assert.match(property(file, '.action-panel', 'padding'), /safe-area-inset-bottom/)
  assert.equal(property(file, '.phrase', 'overflow-wrap'), 'anywhere')
  assert.equal(property(file, '.action-layer', 'position'), 'fixed')
  for (const width of [320,375,390,430]) {
    const titleRpx = width <= 340 ? 6 * (26 + 2) : width <= 375 ? 6 * (28 + 5) : 6 * (29 + 8)
    titleGeometry(width, true, false, '30rpx', '128rpx', true, titleRpx, 106)
  }
})
check('篇章390px demo标题/页边/正文/信笺折角按比例进入静态WXSS', () => {
  const file = 'pages/time-chapter-detail/index.wxss', px = rpx => parseFloat(rpx) * 390 / 750
  assert.ok(Math.abs(px(property(file, '.chapter-content', 'padding').split(/\s+/)[1]) - 28) < 1)
  assert.ok(Math.abs(px(property(file, '.chapter-name', 'font-size')) - 26) < 1)
  assert.ok(Math.abs(px(property(file, '.chapter-status', 'min-height')) - 28) < 1)
  assert.ok(Math.abs(px(property(file, '.chapter-note', 'font-size')) - 16.5) < 1)
  assert.equal(property(file, '.chapter-note', 'line-height'), '2.1')
  assert.equal(property(file, '.chapter-note', 'white-space'), 'pre-wrap')
  assert.equal(property(file, '.chapter-name', 'overflow-wrap'), 'anywhere')
  assert.equal(property(file, '.member-preview', '-webkit-line-clamp'), '2')
  assert.equal(property(file, '.member-fold', 'pointer-events'), 'none')
  assert.equal(property(file, '.member-card', 'background'), '#fcf9f4')
  assert.ok(property(file, '.member-fold', 'background'))
  css(file).walkDecls(/backdrop-filter/, () => { throw Error('chapter requires backdrop blur') })
})
check('篇章滚动正文/成员选择可收缩、底部三等分动作不遮挡末项且留安全区', () => {
  const file = 'pages/time-chapter-detail/index.wxss'
  for (const selector of ['.chapter-scroll', '.picker-scroll']) {
    assert.equal(property(file, selector, 'height'), '0'); assert.equal(property(file, selector, 'min-height'), '0')
  }
  assert.equal(property(file, '.chapter-actions-shell', 'flex-shrink'), '0')
  assert.match(property(file, '.chapter-actions-shell', 'padding'), /safe-area-inset-bottom/)
  assert.match(property(file, '.member-sheet', 'padding'), /safe-area-inset-bottom/)
  assert.equal(property(file, '.chapter-action', 'flex'), '1')
  assert.equal(property(file, '.chapter-action', 'min-width'), '0')
  assert.equal(property(file, '.chapter-actions', 'border-radius'), '24rpx')
  for (const width of [320,375,390,430]) {
    const availableRpx = (750 - 48 - 4) / 3
    assert.ok(4 * 27 * 1.18 + 12 < availableRpx, 'four-character action exceeds its third at ' + width)
    titleGeometry(width, true, true, '54rpx', '96rpx', false, 0, 106)
  }
})
check('近况信笺按390px demo转译边距/标题/行距与两动作，折角和红线不拦截操作', () => {
  const file = 'pages/user-center/index.wxss', px = rpx => parseFloat(rpx) * 390 / 750
  assert.ok(Math.abs(px(property(file, '.summary-sheet', 'left')) - 44) < 1)
  assert.ok(Math.abs(px(property(file, '.summary-sheet', 'padding').split(/\s+/)[0]) - 30) < 1)
  assert.ok(Math.abs(px(property(file, '.summary-title', 'font-size')) - 26) < 1)
  assert.ok(Math.abs(px(property(file, '.stage-summary-text', 'font-size')) - 14) < 1)
  assert.equal(property(file, '.stage-summary-text', 'line-height'), '2.15')
  assert.equal(property(file, '.stage-summary-text', 'white-space'), 'pre-wrap')
  assert.equal(property(file, '.summary-fold', 'pointer-events'), 'none')
  assert.equal(property(file, '.summary-redline', 'pointer-events'), 'none')
  assert.equal(property(file, '.summary-sheet', 'background'), '#fcf9f4')
  assert.equal(property(file, '.summary-layer', 'position'), 'fixed')
  assert.ok(parseInt(property(file, '.summary-layer', 'z-index')) > parseInt(property(file, '.bottom-nav-shell', 'z-index')))
})
check('近况长信只滚动正文/来源，收起与重新整理保持可见，小屏与安全区有高度余量', () => {
  const file = 'pages/user-center/index.wxss'
  assert.equal(property(file, '.summary-scroll', 'height'), '0')
  assert.equal(property(file, '.summary-scroll', 'min-height'), '0')
  assert.equal(property(file, '.summary-actions', 'flex-shrink'), '0')
  assert.match(property(file, '.summary-sheet', 'max-height'), /safe-area-inset-bottom/)
  assert.equal(property(file, '.stage-summary-meta', 'overflow-wrap'), 'anywhere')
  for (const [width,height] of [[320,568],[375,667],[390,844],[430,932]]) {
    const scale = width / 750, sheetWidth = 750 - 2 * 84, contentWidth = sheetWidth - 108
    const buttonsRpx = (5 + 2) * 26 * 1.12 + 2 * 80 + 4 + 27
    assert.ok(buttonsRpx < contentWidth, 'busy buttons overflow at ' + width)
    const actualHeight = Math.min(980 * scale, height * .7085 - 38 * scale - 34)
    const availableScroll = actualHeight - (108 + 50 * 1.4 + 38 + 34 + 73) * scale
    assert.ok(availableScroll > 100, 'letter body too short at ' + width)
  }
})
check('三个Tab半透明导航贴屏幕底边，背景覆盖安全区且末项有独立滚动余量', () => {
  for (const page of ['home','timeline','user-center']) {
    const file = 'pages/' + page + '/index.wxss', tree = css(file)
    assert.equal(property(file, '.bottom-nav-shell', 'position'), 'fixed')
    for (const edge of ['left','right','bottom']) assert.equal(property(file, '.bottom-nav-shell', edge), '0')
    assert.equal(property(file, '.bottom-nav-shell', 'background'), 'transparent')
    assert.equal(property(file, '.bottom-nav-shell', 'padding'), '0')
    assert.match(property(file, '.bottom-nav', 'padding-bottom'), /safe-area-inset-bottom/)
    assert.equal(property(file, '.bottom-nav', 'box-sizing'), 'content-box')
    assert.equal(property(file, '.bottom-nav', 'border-radius'), '24rpx 24rpx 0 0')
    assert.equal(property(file, '.bottom-nav-shell', 'pointer-events'), 'none')
    assert.equal(property(file, '.bottom-nav', 'pointer-events'), 'auto')
    assert.equal(property(file, '.scroll-body', 'height'), '0')
    assert.equal(property(file, '.scroll-body', 'min-height'), '0')
    let fallback, enhancement
    tree.walkRules(rule => {
      if (!rule.selectors.some(s => s.replace(/\.data-v-[\w-]+/g, '') === '.bottom-nav')) return
      const supported = rule.parent.type === 'atrule' && rule.parent.name === 'supports'
      rule.walkDecls('background', d => { if (supported) enhancement = d.value; else fallback = d.value })
    })
    assert.equal(fallback, 'rgba(247,242,233,.9)')
    assert.equal(enhancement, 'rgba(250,247,242,.64)')
    assert.match(property(file, '.bottom-nav', 'backdrop-filter'), /blur\(18px\)/)
    assert.match(property(file, '.bottom-nav', '-webkit-backdrop-filter'), /blur\(18px\)/)
    const reserve = page === 'home' ? property(file, '.home-content', 'padding') : property(file, '.nav-safe-area', 'height')
    assert.match(reserve, /190rpx/); assert.match(reserve, /safe-area-inset-bottom/)
    const navHeight = parseFloat(property(file, '.bottom-nav', 'min-height'))
    assert.ok(190 > navHeight + 1, 'last content remains behind navigation')
  }
  assert.ok(+property('pages/timeline/index.wxss', '.filter-layer', 'z-index') > +property('pages/timeline/index.wxss', '.bottom-nav-shell', 'z-index'))
  assert.ok(+property('pages/user-center/index.wxss', '.summary-layer', 'z-index') > +property('pages/user-center/index.wxss', '.bottom-nav-shell', 'z-index'))
})
check('时光轴窄轴保留文字宽度，短标题点击区域足够，长标题自然增长', () => {
  const file = 'pages/timeline/index.wxss'
  const rpx = (selector, prop) => parseFloat(property(file, selector, prop))
  const inlineMargin = parseFloat(property(file, '.timeline-wrap', 'padding').split(/\s+/)[1])
  const rail = rpx('.tl-item', 'padding-left')
  const padding = rpx('.card', 'padding') * 2
  for (const width of [320, 375, 390, 430]) {
    const content = (750 - inlineMargin * 2 - rail - padding - 2) * width / 750
    assert.ok(content >= 230, 'title squeezed on ' + width + 'px viewport')
    const shortHeight = padding + 2 + 24 * 1.5 + 36 * 1.55 + rpx('.card-title', 'margin-top')
    assert.ok(shortHeight * width / 750 >= 44, 'short card touch target too small')
  }
  assert.equal(property(file, '.entry-body', 'min-width'), '0')
  assert.equal(property(file, '.card-title', 'word-break'), 'break-all')
  assert.ok(+property(file, '.card-title', 'line-height') >= 1.5)
  assert.equal(property(file, '.card', 'box-shadow'), 'none')
  assert.equal(property(file, '.entry-meta', 'display'), 'flex')
  css(file).walkRules(rule => {
    if (rule.selector.includes('.card-title')) {
      assert.ok(!rule.nodes.some(d => d.type === 'decl' && ['height', 'max-height', '-webkit-line-clamp'].includes(d.prop)), 'title is clipped')
    }
  })
  assert.equal(property(file, '.tl-item--last .entry-thread', 'display'), 'none')
})
check('时光轴日期/标题/状态在各自实色纸面达到4.5:1，窄屏小字有下限', () => {
  const file = 'pages/timeline/index.wxss'
  const luminance = color => {
    assert.match(color, /^#[0-9a-f]{6}$/i)
    const rgb = [1, 3, 5].map(i => parseInt(color.slice(i, i + 2), 16) / 255)
      .map(c => c <= 0.04045 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4)
    return rgb[0] * .2126 + rgb[1] * .7152 + rgb[2] * .0722
  }
  const contrast = (a, b) => {
    const l = [luminance(a), luminance(b)].sort((x, y) => y - x)
    return (l[0] + .05) / (l[1] + .05)
  }
  const text = selector => withoutTokens(property(file, selector, 'color'))
  const paper = withoutTokens(property(file, '.page', 'background'))
  const card = withoutTokens(property(file, '.card', 'background'))
  const badge = withoutTokens(property(file, '.entry-status', 'background'))
  for (const [selector, background] of [
    ['.card-title', card], ['.entry-date', card], ['.entry-context', card],
    ['.entry-status', badge], ['.entry-status--sealed', badge], ['.year-heading', paper],
    ['.entry-arrow', card], ['.card-img-error', withoutTokens(property(file, '.card-photo', 'background'))],
  ]) assert.ok(contrast(text(selector), background) >= 4.5, selector + ' fails on ' + background)
  let smallTextRule = false
  css(file).walkAtRules('media', rule => {
    if (!rule.params.includes('360px')) return
    rule.walkRules(child => {
      if (child.selector.includes('.entry-date') && child.selector.includes('.entry-context')) {
        smallTextRule = child.nodes.some(d => d.prop === 'font-size' && d.value === '12px')
      }
    })
  })
  assert.ok(smallTextRule, 'small-screen captions have no 12px floor')
})
if (failures) { console.error('STYLE REGRESSION failures=' + failures); process.exitCode = 1 }
else console.log('STYLE REGRESSION PASS; compiled CSS/geometry only, WeChat drawing still requires manual confirmation')
