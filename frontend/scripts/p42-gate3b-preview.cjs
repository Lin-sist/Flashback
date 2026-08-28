const assert = require('node:assert/strict')

const automatorPath = process.env.P42_AUTOMATOR_PATH
const wsEndpoint = process.env.P42_AUTOMATOR_WS || 'ws://[::1]:9420'
if (!automatorPath) throw new Error('P4.2 Gate 3b Preview automation environment is incomplete')

const automator = require(automatorPath)

async function main() {
  const miniProgram = await automator.connect({ wsEndpoint })
  try {
    await miniProgram.callWxMethod('removeStorageSync', 'flashback:token')
    await miniProgram.callWxMethod('setStorageSync', 'flashback:preview-session', {
      enabled: true,
      enteredAt: Date.now(),
    })
    await miniProgram.evaluate(() => {
      const original = wx.request
      globalThis.__p42RequestCount = 0
      globalThis.__p42AgentRequestCount = 0
      wx.request = options => {
        globalThis.__p42RequestCount += 1
        if (/\/api\/(agent|records\/[^/]+\/agent-memory-policy)/.test(options?.url || '')) {
          globalThis.__p42AgentRequestCount += 1
        }
        return original(options)
      }
    })

    let page = await miniProgram.reLaunch('/pages/record-editor/index?id=101')
    assert.ok(page)
    await page.waitFor(1600)
    const entry = await page.$('.agent-entry')
    assert.ok(entry)
    await entry.tap()
    await page.waitFor(400)
    assert.equal((await page.$$('.agent-layer')).length, 0)
    assert.equal((await page.$$('.memory-consent')).length, 0)
    assert.equal((await page.$$('.source-chip')).length, 0)

    page = await miniProgram.reLaunch('/pages/record-detail/index?id=101')
    assert.ok(page)
    await page.waitFor(1200)
    const policy = await page.$('.memory-policy')
    assert.ok(policy)
    const policyRow = await page.$('.memory-policy-row')
    const policyNote = await page.$('.memory-policy-note')
    const policySave = await page.$('.memory-policy-save')
    await policyRow.tap()
    await policyNote.input('合成 Preview 说明')
    await policySave.tap()
    await page.waitFor(400)
    assert.equal((await page.$$('.source-chip')).length, 0)

    const counts = await miniProgram.evaluate(() => ({
      total: globalThis.__p42RequestCount,
      memoryAgency: globalThis.__p42AgentRequestCount,
    }))
    console.log(`P42WECHAT PREVIEW COUNTS total=${counts.total} memoryAgency=${counts.memoryAgency}`)
    assert.equal(counts.total, 0)
    assert.equal(counts.memoryAgency, 0)
    console.log('P42WECHAT PREVIEW PASS entry=true sheet=false authorizationRequests=0 policyRequests=0 sources=false totalRequests=0 memoryAgencyRequests=0')
  } finally {
    miniProgram.disconnect()
  }
}

main().catch(error => {
  console.error(`P42WECHAT PREVIEW FAIL ${error.message}`)
  process.exitCode = 1
})
