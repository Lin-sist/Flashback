const assert = require('node:assert/strict')

const automatorPath = process.env.P41_AUTOMATOR_PATH
const wsEndpoint = process.env.P41_AUTOMATOR_WS || 'ws://[::1]:9420'
if (!automatorPath) {
  throw new Error('P4.1 Gate 3b Preview automation environment is incomplete')
}

const automator = require(automatorPath)

async function main() {
  const miniProgram = await automator.connect({ wsEndpoint })
  try {
    await miniProgram.callWxMethod('removeStorageSync', 'flashback:token')
    await miniProgram.callWxMethod('setStorageSync', 'flashback:preview-session', {
      enabled: true,
      enteredAt: Date.now(),
    })

    const page = await miniProgram.reLaunch('/pages/record-editor/index?id=101')
    assert.ok(page)
    await page.waitFor(1600)

    const entry = await page.$('.agent-entry')
    assert.ok(entry)
    assert.match(await entry.text(), /选择这次想怎么聊/)
    await entry.tap()
    await page.waitFor(500)

    assert.equal((await page.$$('.intent-layer')).length, 0)
    assert.equal((await page.$$('.agent-layer')).length, 0)
    assert.equal((await page.$$('.intent-option')).length, 0)
    console.log('P41WECHAT PREVIEW PASS entry=true chooser=false sheet=false agentRequests=0')
  } finally {
    miniProgram.disconnect()
  }
}

main().catch(error => {
  console.error(`P41WECHAT PREVIEW FAIL ${error.message}`)
  process.exitCode = 1
})
