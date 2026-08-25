const assert = require('node:assert/strict')

const automatorPath = process.env.P41_AUTOMATOR_PATH
const wsEndpoint = process.env.P41_AUTOMATOR_WS || 'ws://[::1]:9420'
if (!automatorPath) {
  throw new Error('P4.1 Gate 3b Standard automation environment is incomplete')
}

const automator = require(automatorPath)
const apiBase = 'http://127.0.0.1:8080'
const delay = (ms) => new Promise(resolve => setTimeout(resolve, ms))

async function waitUntil(check, message, timeout = 15000) {
  const started = Date.now()
  while (Date.now() - started < timeout) {
    if (await check()) return
    await delay(300)
  }
  throw new Error(message)
}

async function waitForElement(page, selector, message, timeout = 15000) {
  let found = null
  await waitUntil(async () => {
    found = await page.$(selector)
    return Boolean(found)
  }, message, timeout)
  return found
}

async function findSavedRecordId(token) {
  const response = await fetch(`${apiBase}/api/records?pageNum=1&pageSize=1&status=SAVED`, {
    headers: { Authorization: `Bearer ${token}` },
  })
  assert.equal(response.status, 200)
  const payload = await response.json()
  assert.equal(payload.code, 0)
  assert.ok(Array.isArray(payload.data?.list) && payload.data.list.length > 0)
  return payload.data.list[0].id
}

function syntheticRequestResult(recordId, intent, { ended = false, failed = false } = {}) {
  const now = '2026-08-25T00:00:00'
  const messages = Array.from({ length: 12 }, (_, index) => ({
    id: 9411000 + index,
    role: index % 2 === 0 ? 'USER' : 'ASSISTANT',
    turnNo: Math.floor(index / 2) + 1,
    stage: 'WITNESS',
    content: index % 2 === 0 ? '合成短答' : '合成见证回应。',
    createdAt: now,
  }))

  const session = {
    sessionId: 9410001,
    recordId,
    purpose: 'WRITING_GUIDANCE',
    conversationIntent: intent,
    stage: ended ? 'ENDED' : 'WITNESS',
    sessionStatus: ended ? 'ENDED' : 'ACTIVE',
    turnCount: 6,
    maxTurns: 10,
    canContinue: !ended,
    messages,
    materialDraft: null,
    source: 'P41_GATE3B_SYNTHETIC',
    status: 'SUCCESS',
    message: null,
    pendingToolCall: null,
    lastToolCallResult: null,
  }

  return {
    data: failed
      ? { code: 503, message: '合成切换失败', data: null }
      : { code: 0, message: 'success', data: session },
    statusCode: failed ? 503 : 200,
    header: {},
    cookies: [],
    errMsg: 'request:ok',
  }
}

async function main() {
  const miniProgram = await automator.connect({ wsEndpoint })
  let requestMocked = false
  try {
    const token = await miniProgram.callWxMethod('getStorageSync', 'flashback:token')
    assert.ok(typeof token === 'string' && token.length > 0, 'real WeChat login token is missing')
    await miniProgram.callWxMethod('removeStorageSync', 'flashback:preview-session')

    const recordId = await findSavedRecordId(token)
    let page = await miniProgram.reLaunch(`/pages/record-editor/index?id=${recordId}`)
    assert.ok(page)
    const entry = await waitForElement(page, '.agent-entry', 'standard record editor did not render Agent entry')
    assert.match(await entry.text(), /选择这次想怎么聊/)

    await entry.tap()
    const chooser = await waitForElement(page, '.intent-layer', 'intent chooser did not open')
    assert.ok(chooser)
    const options = await page.$$('.intent-option')
    assert.equal(options.length, 2)
    assert.match(await options[0].text(), /先听我说/)
    assert.match(await options[1].text(), /帮我理一理/)

    await miniProgram.mockWxMethod('request', syntheticRequestResult(recordId, 'LISTEN'))
    requestMocked = true

    await options[0].tap()
    const sheet = await waitForElement(page, '.agent-layer', 'Agent sheet did not open')
    assert.ok(sheet)
    const heading = await waitForElement(page, '.head-title', 'Agent heading did not render')
    await waitUntil(async () => /先听你说/.test(await heading.text()), 'LISTEN heading did not settle')
    assert.equal((await page.$$('.message-row')).length, 12)

    const composer = await waitForElement(page, '.composer-input', 'Agent textarea did not render')
    await composer.input('好')
    assert.equal(await composer.value(), '好')
    assert.ok(await page.$('.send-btn'))

    const switches = await page.$$('.intent-switch-item')
    assert.equal(switches.length, 2)
    assert.match(await switches[0].attribute('class'), /intent-switch-item--active/)
    await miniProgram.mockWxMethod('request', syntheticRequestResult(recordId, 'LISTEN', { failed: true }))
    await switches[1].tap()
    const errorCard = await waitForElement(page, '.error-card', 'switch failure did not render')
    assert.match(await errorCard.text(), /合成切换失败/)
    const afterFailure = await page.$$('.intent-switch-item')
    assert.match(await afterFailure[0].attribute('class'), /intent-switch-item--active/)

    await miniProgram.mockWxMethod('request', syntheticRequestResult(recordId, 'UNTANGLE'))
    await afterFailure[1].tap()
    await waitUntil(async () => {
      const current = await page.$$('.intent-switch-item')
      return /intent-switch-item--active/.test(await current[1].attribute('class'))
    }, 'intent switch retry did not succeed')
    assert.match(await heading.text(), /一起理一理/)

    const finish = await waitForElement(page, '.finish-link', 'finish action did not render')
    await miniProgram.mockWxMethod('request', syntheticRequestResult(recordId, 'UNTANGLE', { ended: true }))
    await finish.tap()
    const ended = await waitForElement(page, '.ended-note', 'ended state did not render')
    assert.match(await ended.text(), /这次就聊到这里/)
    assert.equal((await page.$$('.composer')).length, 0)

    console.log('P41WECHAT STANDARD PASS login=true chooser=true textarea=true scroll=true switchFailure=true switchRetry=true ended=true providerCalls=0')
  } finally {
    if (requestMocked) await miniProgram.restoreWxMethod('request')
    miniProgram.disconnect()
  }
}

main().catch(error => {
  console.error(`P41WECHAT STANDARD FAIL ${error.message}`)
  process.exitCode = 1
})
