const assert = require('node:assert/strict')

const automatorPath = process.env.P42_AUTOMATOR_PATH
const wsEndpoint = process.env.P42_AUTOMATOR_WS || 'ws://[::1]:9420'
if (!automatorPath) throw new Error('P4.2 Gate 3b Standard automation environment is incomplete')

const automator = require(automatorPath)
const apiBase = 'http://127.0.0.1:8080'
const delay = ms => new Promise(resolve => setTimeout(resolve, ms))

async function waitUntil(check, message, timeout = 20000) {
  const started = Date.now()
  while (Date.now() - started < timeout) {
    if (await check()) return
    await delay(300)
  }
  throw new Error(message)
}

async function waitForElement(page, selector, message, timeout = 20000) {
  let found = null
  await waitUntil(async () => {
    found = await page.$(selector)
    return Boolean(found)
  }, message, timeout)
  return found
}

async function login(miniProgram) {
  await miniProgram.callWxMethod('removeStorageSync', 'flashback:token')
  await miniProgram.callWxMethod('removeStorageSync', 'flashback:preview-session')
  const page = await miniProgram.reLaunch('/pages/login/index')
  assert.ok(page)
  const loginButton = await waitForElement(page, '.wechat-login', 'WeChat login action did not render')
  await loginButton.tap()
  let token = ''
  await waitUntil(async () => {
    token = await miniProgram.callWxMethod('getStorageSync', 'flashback:token')
    return typeof token === 'string' && token.length > 0
  }, 'real WeChat login did not produce an authenticated token')
  await waitUntil(async () => (await miniProgram.currentPage())?.path === 'pages/home/index',
    'real WeChat login navigation did not settle')
  await delay(500)
  return token
}

async function api(token, path) {
  const response = await fetch(`${apiBase}${path}`, {
    headers: { Authorization: `Bearer ${token}` },
  })
  const payload = await response.json()
  assert.equal(response.status, 200)
  assert.equal(payload.code, 0)
  return payload.data
}

async function findEditableRecord(token) {
  for (const status of ['SAVED', 'DRAFT']) {
    const page = await api(token, `/api/records?pageNum=1&pageSize=20&status=${status}`)
    if (Array.isArray(page?.list) && page.list.length > 0) return page.list[0].id
  }
  throw new Error('real authenticated account has no editable record for read-only UI verification')
}

function sessionResult(recordId, enabled, { failed = false } = {}) {
  const now = '2026-08-27T00:00:00'
  const session = {
    sessionId: 9423001,
    recordId,
    purpose: 'WRITING_GUIDANCE',
    conversationIntent: 'LISTEN',
    stage: 'WITNESS',
    sessionStatus: 'ACTIVE',
    turnCount: 1,
    maxTurns: 8,
    canContinue: true,
    crossRecordMemoryEnabled: enabled,
    messages: [{
      id: 9424001,
      role: 'ASSISTANT',
      turnNo: 1,
      stage: 'WITNESS',
      content: '合成见证回应。',
      createdAt: now,
      memorySources: [
        {
          recordId,
          sourceKind: 'CROSS_RECORD',
          displayTitle: '合成来源',
          occurredAt: now,
          contextNote: null,
          available: true,
        },
        {
          recordId: null,
          sourceKind: 'CROSS_RECORD',
          displayTitle: null,
          occurredAt: null,
          contextNote: null,
          available: false,
        },
      ],
    }],
    materialDraft: null,
    source: 'P42_GATE3B_SYNTHETIC',
    status: 'SUCCESS',
    message: null,
    pendingToolCall: null,
    lastToolCallResult: null,
  }
  return {
    data: failed
      ? { code: 503, message: '合成授权保存失败', data: null }
      : { code: 0, message: 'success', data: session },
    statusCode: failed ? 503 : 200,
    header: {},
    cookies: [],
    errMsg: 'request:ok',
  }
}

function detailResult(detail, excluded, contextNote, { failed = false } = {}) {
  return {
    data: failed
      ? { code: 503, message: '合成策略保存失败', data: null }
      : { code: 0, message: 'success', data: {
          ...detail,
          agentMemoryExcluded: excluded,
          agentMemoryContextNote: contextNote,
        } },
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
    const token = await login(miniProgram)
    const recordId = await findEditableRecord(token)

    let page = await miniProgram.reLaunch(`/pages/record-editor/index?id=${recordId}`)
    assert.ok(page)
    let entry = null
    try {
      entry = await waitForElement(page, '.agent-entry', 'record editor did not render Agent entry')
    } catch (error) {
      const current = await miniProgram.currentPage()
      const states = await page.$$('.state-paper')
      const bodies = await page.$$('.letter-wrap')
      console.log(`P42WECHAT STANDARD DIAG path=${current?.path || '<unknown>'} states=${states.length} bodies=${bodies.length}`)
      throw error
    }
    await entry.tap()
    const options = await page.$$('.intent-option')
    assert.equal(options.length, 2)

    await miniProgram.mockWxMethod('request', sessionResult(recordId, false))
    requestMocked = true
    await options[0].tap()
    await waitForElement(page, '.agent-layer', 'Agent sheet did not open')
    const memoryConsent = await waitForElement(page, '.memory-consent', 'memory consent control did not render')
    let memorySwitch = await page.$('.memory-consent .memory-switch')
    assert.doesNotMatch(await memorySwitch.attribute('class'), /memory-switch--on/)
    assert.match(await memoryConsent.text(), /默认只看当前记录和这次对话/)

    const sourceChips = await page.$$('.source-chip')
    assert.equal(sourceChips.length, 2)
    assert.match(await sourceChips[0].text(), /合成来源/)
    assert.match(await sourceChips[1].text(), /来源记录已删除或不可用/)
    await sourceChips[1].tap()
    assert.equal((await miniProgram.currentPage()).path, 'pages/record-editor/index')

    await miniProgram.mockWxMethod('request', sessionResult(recordId, true))
    await memoryConsent.tap()
    await waitUntil(async () => {
      memorySwitch = await page.$('.memory-consent .memory-switch')
      return /memory-switch--on/.test(await memorySwitch.attribute('class'))
    }, 'memory authorization did not reflect backend-enabled state')
    assert.equal((await page.$$('.message-row')).length, 1)

    await miniProgram.mockWxMethod('request', sessionResult(recordId, true, { failed: true }))
    await memoryConsent.tap()
    await page.waitFor(500)
    memorySwitch = await page.$('.memory-consent .memory-switch')
    assert.match(await memorySwitch.attribute('class'), /memory-switch--on/)

    await miniProgram.mockWxMethod('request', sessionResult(recordId, false))
    await memoryConsent.tap()
    await waitUntil(async () => {
      memorySwitch = await page.$('.memory-consent .memory-switch')
      return !/memory-switch--on/.test(await memorySwitch.attribute('class'))
    }, 'memory authorization did not reflect backend-disabled state')

    await miniProgram.restoreWxMethod('request')
    requestMocked = false
    const availableChip = (await page.$$('.source-chip'))[0]
    await availableChip.tap()
    await waitUntil(async () => (await miniProgram.currentPage())?.path === 'pages/record-detail/index',
      'available source did not navigate to record detail')
    page = await miniProgram.currentPage()
    const policy = await waitForElement(page, '.memory-policy', 'record memory policy did not render')
    assert.match(await policy.text(), /之后不再参考，不会撤回已经发出去的那一轮/)
    assert.ok(await page.$('.ownership-delete'))

    const realDetail = await api(token, `/api/records/${recordId}`)
    const originalExcluded = realDetail.agentMemoryExcluded === true
    const syntheticExcluded = !originalExcluded
    const syntheticNote = '合成时间语境说明'
    const policyRow = await page.$('.memory-policy-row')
    const policyNote = await page.$('.memory-policy-note')
    const policySave = await page.$('.memory-policy-save')
    await policyRow.tap()
    await policyNote.input(syntheticNote)
    await miniProgram.mockWxMethod('request', detailResult(realDetail, syntheticExcluded, syntheticNote))
    requestMocked = true
    await policySave.tap()
    await page.waitFor(500)
    let policySwitch = await page.$('.memory-policy-row .memory-switch')
    assert.equal(/memory-switch--on/.test(await policySwitch.attribute('class')), syntheticExcluded)
    assert.equal(await policyNote.value(), syntheticNote)

    await policyRow.tap()
    await policyNote.input('合成失败值')
    await miniProgram.mockWxMethod('request', detailResult(realDetail, !syntheticExcluded, null, { failed: true }))
    await policySave.tap()
    await page.waitFor(500)
    policySwitch = await page.$('.memory-policy-row .memory-switch')
    assert.equal(/memory-switch--on/.test(await policySwitch.attribute('class')), syntheticExcluded)
    assert.equal(await policyNote.value(), syntheticNote)

    console.log('P42WECHAT STANDARD PASS login=true defaultOff=true enable=true disable=true failureRollback=true sources=true unavailable=true sourceNavigation=true policy=true note=true deleteSurface=true providerCalls=0')
  } finally {
    if (requestMocked) await miniProgram.restoreWxMethod('request')
    miniProgram.disconnect()
  }
}

main().catch(error => {
  console.error(`P42WECHAT STANDARD FAIL ${error.message}`)
  process.exitCode = 1
})
