const assert = require('node:assert/strict')

const automatorPath = process.env.P32_AUTOMATOR_PATH
const wsEndpoint = process.env.P32_AUTOMATOR_WS || 'ws://[::1]:9420'
if (!automatorPath) {
  throw new Error('P3.2 Gate 3c Preview automation environment is incomplete')
}

const automator = require(automatorPath)
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

async function main() {
  const miniProgram = await automator.connect({ wsEndpoint })
  try {
    await miniProgram.callWxMethod('removeStorageSync', 'flashback:token')
    await miniProgram.callWxMethod('setStorageSync', 'flashback:preview-session', {
      enabled: true,
      enteredAt: Date.now(),
    })
    const savedBefore = await miniProgram.callWxMethod('getSavedFileList')
    const savedBeforeCount = savedBefore.fileList?.length ?? 0

    let page = await miniProgram.reLaunch('/pages/user-center/data-backup/index')
    assert.ok(page)
    await page.waitFor(1400)
    const stateList = await waitForElement(page, '.state-list', 'Preview ownership summary did not render')
    const stateText = await stateList.text()
    assert.match(stateText, /未完成 1/)
    assert.match(stateText, /已保存 2/)
    assert.match(stateText, /封存中 2/)
    assert.match(stateText, /已抵达 1/)

    const exportButton = await waitForElement(page, '.primary-button', 'Preview export action did not render')
    await exportButton.tap()
    await page.waitFor(400)
    assert.equal((await page.$$('.operation-card')).length, 0)

    const options = await page.$$('.option')
    assert.equal(options.length, 2)
    await options[1].tap()
    assert.match(await options[1].attribute('class'), /selected/)
    await exportButton.tap()
    await page.waitFor(400)
    assert.equal((await page.$$('.operation-card')).length, 0)

    const clearButton = await waitForElement(page, '.danger-button', 'Preview clear-all action did not render')
    await clearButton.tap()
    await page.waitFor(400)
    assert.equal((await page.$$('.confirm-box')).length, 0)

    page = await miniProgram.reLaunch('/pages/record-detail/index?id=101')
    assert.ok(page)
    await page.waitFor(1400)
    const deleteButton = await waitForElement(page, '.ownership-delete', 'Preview record delete action did not render')
    assert.match(await deleteButton.text(), /删除这条记录/)
    await deleteButton.tap()
    await page.waitFor(400)
    assert.ok(await page.$('.ownership-delete'))

    const savedAfter = await miniProgram.callWxMethod('getSavedFileList')
    assert.equal(savedAfter.fileList?.length ?? 0, savedBeforeCount)
    console.log('P32WECHAT PREVIEW PASS summary=demo exportCalls=0 deleteCalls=0 clearAllCalls=0 downloadFiles=0')
  } finally {
    miniProgram.disconnect()
  }
}

main().catch(error => {
  console.error(`P32WECHAT PREVIEW FAIL ${error.message}`)
  process.exitCode = 1
})
