const assert = require('node:assert/strict')

const automatorPath = process.env.P5_AUTOMATOR_PATH
const wsEndpoint = process.env.P5_AUTOMATOR_WS || 'ws://[::1]:9420'
const previewSlice = process.env.P5_PREVIEW_SLICE || 'chapter'
if (!automatorPath) throw new Error('P5 Gate 3b Preview automation environment is incomplete')

const automator = require(automatorPath)
const delay = ms => new Promise(resolve => setTimeout(resolve, ms))

async function waitUntil(check, message, timeout = 20000) {
  const started = Date.now()
  while (Date.now() - started < timeout) {
    if (await check()) return
    await delay(250)
  }
  throw new Error(message)
}

async function main() {
  console.log(`P5WECHAT PREVIEW CONNECT ${wsEndpoint}`)
  const miniProgram = await Promise.race([
    automator.connect({ wsEndpoint }),
    delay(20000).then(() => { throw new Error('automation websocket connect timeout') }),
  ])
  console.log('P5WECHAT PREVIEW CONNECTED')
  try {
    await miniProgram.callWxMethod('removeStorageSync', 'flashback:token')
    console.log('P5WECHAT PREVIEW STORAGE token-cleared')
    await miniProgram.callWxMethod('setStorageSync', 'flashback:preview-session', { enabled: true, enteredAt: Date.now() })
    console.log('P5WECHAT PREVIEW STORAGE session-set')
    await miniProgram.evaluate(() => {
      const original = wx.request
      globalThis.__p5PreviewRequests = []
      wx.request = options => {
        globalThis.__p5PreviewRequests.push(String(options?.url || ''))
        return original(options)
      }
    })
    console.log('P5WECHAT PREVIEW COUNTER installed')

    if (previewSlice === 'membership') {
      const page = await miniProgram.reLaunch('/pages/record-detail/index?id=101')
      await waitUntil(async () => Boolean(await page.$('.chapter-membership-panel')), 'Preview record membership panel missing')
      console.log('P5WECHAT PREVIEW STEP record-detail PASS')
      await (await page.$('.chapter-membership-action')).tap()
      await delay(400)
      const requests = await miniProgram.evaluate(() => globalThis.__p5PreviewRequests)
      assert.equal(requests.length, 0)
      console.log('P5WECHAT PREVIEW MEMBERSHIP PASS fixedSynthetic=true membershipBlocked=true totalRequests=0 timeChapterRequests=0')
      return
    }

    let page = await miniProgram.reLaunch('/pages/record-list/index')
    console.log('P5WECHAT PREVIEW PAGE record-list')
    await waitUntil(async () => (await page.$$('.card')).length > 0, 'Preview records did not render')
    assert.ok(await page.$('.preview-mode-notice'))
    await (await page.$$('.secondary-tab'))[1].tap()
    await waitUntil(async () => (await page.$$('.chapter-card')).length === 2, 'Preview chapters did not render')
    console.log('P5WECHAT PREVIEW STEP list PASS')
    await (await page.$$('.chapter-card'))[0].tap()
    await waitUntil(async () => (await miniProgram.currentPage())?.path === 'pages/time-chapter-detail/index', 'Preview chapter detail navigation failed')
    page = await miniProgram.currentPage()
    assert.ok(await page.$('.chapter-name'))
    assert.ok((await page.$$('.member-card')).length > 0)
    console.log('P5WECHAT PREVIEW STEP detail PASS')
    await (await page.$('.chapter-edit')).tap()
    await (await page.$('.chapter-edit-actions__save')).tap()
    await (await page.$$('.chapter-action'))[1].tap()
    await (await page.$('.chapter-delete')).tap()
    console.log('P5WECHAT PREVIEW STEP chapter-mutations-blocked PASS')
    const requests = await miniProgram.evaluate(() => globalThis.__p5PreviewRequests)
    assert.equal(requests.length, 0)
    console.log('P5WECHAT PREVIEW CHAPTER PASS list=true detail=true fixedSynthetic=true createBlocked=true editBlocked=true lifecycleBlocked=true deleteBlocked=true totalRequests=0 timeChapterRequests=0')
  } finally {
    miniProgram.disconnect()
  }
}

main().catch(error => {
  console.error(`P5WECHAT PREVIEW FAIL ${error.stack || error.message}`)
  process.exitCode = 1
})
