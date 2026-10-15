// 固定合成会话；执行实际SFC脚本和编译模板事件，零业务网络。
const assert = require('node:assert/strict'), fs = require('node:fs'), path = require('node:path'), vm = require('node:vm')
const vue = require('vue'), ts = require('typescript')
const { parse } = require('vue/compiler-sfc'), { compile } = require(require.resolve('@vue/compiler-dom', { paths: [path.dirname(require.resolve('vue'))] }))
const root = path.resolve(__dirname, '..')
function mount(name, initial = {}) {
  const { descriptor } = parse(fs.readFileSync(path.join(root, 'src/pages/record-editor/components', name + '.vue'), 'utf8'))
  const events = [], navigation = []
  const props = vue.reactive({ visible: true, session: null, loading: false, sending: false, finishing: false, confirmingToolCall: false, switchingIntent: false, switchingMemoryAuthorization: false, errorMessage: '', ...initial })
  const exports = name === 'AgentChatSheet' ? 'input, scrollAnchor, messages, material, isEnded, awaitingRetry, canSend, phaseText, currentIntent, canSwitchIntent, switchIntent, memoryEnabled, canEnableMemory, memoryHint, toggleMemoryAuthorization, assistantSources, sourceKicker, sourceLabel, openSource, pendingToolCall, toolCallNotice, toolCallFailed, messageKey, submit, emit' : 'emit'
  const js = ts.transpileModule(descriptor.scriptSetup.content + '\nglobalThis.surface = {' + exports + '}', { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText
  const context = { exports: {}, defineProps: () => props, defineEmits: () => (...args) => events.push(args), uni: { navigateTo: x => navigation.push(x.url) }, require: spec => {
    if (spec === 'vue') return vue
    if (spec === '../../../utils') return { formatYearMonth: () => '2000年01月' }
    throw Error('Unexpected dependency ' + spec)
  } }
  vm.runInNewContext(js, context)
  const scope = context.surface
  const render = new Function('Vue', compile(descriptor.template.content, { mode: 'function', isCustomElement: () => true }).code)(vue)
  const target = vue.proxyRefs({ ...scope })
  const ctx = new Proxy(target, { has: (_, key) => key !== '_Vue' && key !== '_cache', get: (obj, key) => key === Symbol.unscopables ? undefined : key in obj ? obj[key] : props[key] })
  function nodes() { const all = []; function walk(n) { if (!n || typeof n !== 'object') return; all.push(n); if (Array.isArray(n.children)) n.children.forEach(walk) }; walk(render(ctx, [])); return all }
  const select = cls => nodes().filter(n => vue.normalizeClass(n.props?.class).split(' ').includes(cls))
  const tap = cls => { const n = select(cls)[0]; assert.ok(n, 'Missing ' + cls); n.props.onTap({ stopPropagation() {} }) }
  return { props, scope, events, navigation, nodes, select, tap }
}
const session = (extra = {}) => ({ sessionStatus: 'ACTIVE', stage: 'LISTENING', conversationIntent: 'LISTEN', messages: [], crossRecordMemoryEnabled: false, ...extra })
let passed = 0
function check(name, fn) { fn(); passed++; console.log('PASS ' + name) }
check('选择与取消仍发出真实意图枚举', () => {
  const c = mount('AgentIntentChooser'); c.select('intent-option').forEach(n => n.props.onTap()); c.tap('intent-mask')
  assert.equal(JSON.stringify(c.events), JSON.stringify([['choose', 'LISTEN'], ['choose', 'UNTANGLE'], ['close']]))
})
check('无会话加载/失败仍显示两个意图，加载不可切换且选中态遵循入口选择', () => {
  const c = mount('AgentChatSheet', { session: null, loading: true, selectedIntent: 'UNTANGLE' })
  assert.equal(c.select('intent-switch-item').length, 2)
  assert.equal(c.scope.currentIntent.value, 'UNTANGLE')
  c.select('intent-switch-item').forEach(n => n.props.onTap())
  assert.equal(c.events.length, 0)
  c.props.loading = false; c.props.errorMessage = 'synthetic start failure'
  assert.equal(c.select('intent-switch-item').length, 2)
  c.select('intent-switch-item').forEach(n => n.props.onTap())
  assert.equal(c.events.length, 0)
  c.props.errorMessage = ''; c.props.session = session()
  assert.equal(c.scope.currentIntent.value, 'LISTEN')
  c.select('intent-switch-item')[1].props.onTap()
  assert.equal(JSON.stringify(c.events), JSON.stringify([['switchIntent', 'UNTANGLE']]))
  c.props.switchingIntent = true; c.select('intent-switch-item')[0].props.onTap()
  assert.equal(c.events.length, 1)
  c.props.session.sessionStatus = 'ENDED'; assert.equal(c.select('intent-switch-item').length, 0)
})
check('输入栏发送后清空；忙碌、失败待重试和结束禁止新发送', () => {
  const c = mount('AgentChatSheet', { session: session() }); c.scope.input.value = '  synthetic message  '; c.tap('send-btn'); c.tap('send-btn')
  assert.equal(JSON.stringify(c.events), JSON.stringify([['send', 'synthetic message']]))
  for (const state of [{ sending: true }, { sending: false, errorMessage: 'synthetic failure', session: session({ messages: [{ role: 'USER' }] }) }, { errorMessage: '', session: session({ sessionStatus: 'ENDED' }) }]) {
    Object.assign(c.props, state); c.scope.input.value = 'blocked'; c.scope.submit(); assert.equal(c.events.length, 1); assert.equal(c.scope.input.value, 'blocked')
  }
})
check('记忆默认关闭；切换忙碌及失败待重试不可新启用，已开启可撤销', () => {
  const c = mount('AgentChatSheet', { session: session() }); assert.equal(c.scope.memoryEnabled.value, false); c.tap('memory-consent')
  c.props.switchingMemoryAuthorization = true; c.scope.toggleMemoryAuthorization(); c.props.switchingMemoryAuthorization = false
  c.props.errorMessage = 'synthetic failure'; c.props.session.messages = [{ role: 'USER' }]; c.scope.toggleMemoryAuthorization(); assert.equal(c.events.length, 1)
  c.props.session.crossRecordMemoryEnabled = true; c.scope.toggleMemoryAuthorization()
  assert.equal(JSON.stringify(c.events), JSON.stringify([['switchMemoryAuthorization', true], ['switchMemoryAuthorization', false]]))
})
check('重试、工具确认、素材选择和收束均保留用户动作', () => {
  const c = mount('AgentChatSheet', { session: session({ pendingToolCall: { status: 'PROPOSED', askText: 'synthetic proposal' }, materialDraft: 'synthetic material' }), errorMessage: 'synthetic failure' })
  assert.equal(c.events.length, 0)
  for (const cls of ['retry-link', 'tool-secondary', 'tool-primary', 'material-secondary', 'material-primary', 'finish-link']) c.tap(cls)
  assert.equal(JSON.stringify(c.events), JSON.stringify([['retry'], ['confirmToolCall', 'REJECT'], ['confirmToolCall', 'ACCEPT'], ['discardMaterial'], ['useMaterial', 'synthetic material'], ['finish']]))
})
check('可用来源可达、删除来源拒绝；倾诉/理一理映射保持', () => {
  const c = mount('AgentChatSheet', { session: session({ messages: [{ id: 1, turnNo: 1, role: 'ASSISTANT', content: 'synthetic', memorySources: [{ available: true, sourceKind: 'CROSS_RECORD', recordId: 41 }, { available: false, sourceKind: 'CROSS_RECORD', recordId: 42 }] }] }) })
  c.select('source-chip').forEach(n => n.props.onTap({ stopPropagation() {} })); assert.deepEqual(c.navigation, ['/pages/record-detail/index?id=41&source=archive'])
  c.select('intent-switch-item').forEach(n => n.props.onTap()); assert.equal(JSON.stringify(c.events), JSON.stringify([['switchIntent', 'LISTEN'], ['switchIntent', 'UNTANGLE']]))
})
check('百条长消息仍在滚动区；结束后隐藏输入与记忆开关', () => {
  const c = mount('AgentChatSheet', { session: session({ messages: Array.from({ length: 100 }, (_, id) => ({ id, turnNo: id, role: 'ASSISTANT', content: 'synthetic long message '.repeat(50) })) }) })
  assert.equal(c.select('message-list')[0].type, 'scroll-view'); assert.equal(c.select('message-row').length, 100); assert.equal(c.select('composer').length, 1)
  c.props.session.sessionStatus = 'ENDED'; assert.equal(c.select('composer').length, 0); assert.equal(c.select('memory-consent').length, 0); assert.equal(c.select('ended-note').length, 1)
})
console.log('AGENT SHEETS ' + passed + '/' + passed + ' PASS; synthetic only; real provider/network=0')
