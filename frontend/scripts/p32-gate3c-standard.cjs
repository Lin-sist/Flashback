const assert = require('node:assert/strict')

const automatorPath = process.env.P32_AUTOMATOR_PATH
const token = process.env.P32_GATE3C_TOKEN
const wsEndpoint = process.env.P32_AUTOMATOR_WS || 'ws://[::1]:9420'
if (!automatorPath || !token) {
  throw new Error('P3.2 Gate 3c automation environment is incomplete')
}

const automator = require(automatorPath)
const apiBase = 'http://127.0.0.1:8080'
const recordIds = [9932611, 9932612, 9932613, 9932614]
const resumeAfterTwoDeletes = process.env.P32_GATE3C_RESUME === '1'

const delay = (ms) => new Promise(resolve => setTimeout(resolve, ms))

async function api(path) {
  const response = await fetch(`${apiBase}${path}`, {
    headers: { Authorization: `Bearer ${token}` },
  })
  const payload = await response.json()
  return { status: response.status, payload }
}

async function totalRecords() {
  const { status, payload } = await api('/api/data-ownership/summary')
  assert.equal(status, 200)
  assert.equal(payload.code, 0)
  return Object.values(payload.data.recordCounts).reduce((sum, count) => sum + Number(count), 0)
}

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

async function waitForText(page, selector, pattern, message, timeout = 15000) {
  let found = null
  await waitUntil(async () => {
    found = await page.$(selector)
    return found && pattern.test(await found.text())
  }, message, timeout)
  return found
}

async function main() {
  console.log('P32WECHAT STANDARD stage=connect')
  const miniProgram = await automator.connect({ wsEndpoint })
  try {
    console.log('P32WECHAT STANDARD stage=session')
    await miniProgram.callWxMethod('setStorageSync', 'flashback:token', token)
    await miniProgram.callWxMethod('removeStorageSync', 'flashback:preview-session')

    let page
    if (!resumeAfterTwoDeletes) {
      page = await miniProgram.reLaunch('/pages/user-center/data-backup/index')
      assert.ok(page)
      await page.waitFor(1800)
      const operationCard = await page.$('.operation-card')
      assert.match(await operationCard.text(), /仍有数据未完成清理/)

      const activeBeforeRetry = (await api('/api/data-ownership/summary')).payload.data.activeOperation
      assert.equal(activeBeforeRetry.status, 'RETRY_REQUIRED')
      assert.equal(activeBeforeRetry.operationType, 'CLEAR_ALL_RECORDS')

      console.log('P32WECHAT STANDARD stage=freeze')
      page = await miniProgram.reLaunch('/pages/record-editor/index?id=9932611')
      assert.ok(page)
      await page.waitFor(1600)
      const beforeBlockedSave = await totalRecords()
      const editor = await waitForElement(page, '.editor-field', 'editor input did not render')
      await editor.input('synthetic mutation must remain blocked')
      const save = await waitForElement(page, '.seal-btn', 'save button did not render')
      await save.tap()
      await page.waitFor(800)
      assert.equal(await totalRecords(), beforeBlockedSave)
      const unchanged = await api('/api/records/9932611')
      assert.equal(unchanged.status, 200)
      assert.equal(unchanged.payload.data.content, 'synthetic draft')

      console.log('P32WECHAT STANDARD stage=retry')
      page = await miniProgram.reLaunch('/pages/user-center/data-backup/index')
      assert.ok(page)
      await page.waitFor(1200)
      const retryCard = await waitForElement(page, '.operation-card', 'retry operation card did not render')
      const retryButton = await retryCard.$('button')
      assert.ok(retryButton)
      await retryButton.tap()
      await waitUntil(async () => {
        const result = await api(`/api/data-ownership/operations/${activeBeforeRetry.id}`)
        return result.payload.data.status === 'SUCCEEDED'
      }, 'retry UI did not succeed')
      await waitForText(page, '.operation-title', /清理已完成/, 'retry success was not visible')
      assert.equal(await totalRecords(), 6)

      console.log('P32WECHAT STANDARD stage=export')
      const savedBefore = await miniProgram.callWxMethod('getSavedFileList')
      const savedBeforeCount = savedBefore.fileList?.length ?? 0
      const options = await page.$$('.option')
      assert.equal(options.length, 2)
      assert.match(await options[0].attribute('class'), /selected/)
      const exportButton = await waitForElement(page, '.primary-button', 'export button did not render')
      await exportButton.tap()
      await waitForText(page, '.operation-title', /导出包已生成/, 'default export did not succeed')
      const buttonsAfterExport = await page.$$('button')
      const saveExport = await Promise.all(buttonsAfterExport.map(async button => ({ button, text: await button.text() })))
        .then(items => items.find(item => item.text.includes('保存导出包'))?.button)
      assert.ok(saveExport)
      await saveExport.tap()
      await waitUntil(async () => {
        const result = await miniProgram.callWxMethod('getSavedFileList')
        return (result.fileList?.length ?? 0) > savedBeforeCount
      }, 'WeChat saveFile did not persist the ZIP')
      const savedAfter = await miniProgram.callWxMethod('getSavedFileList')
      const saved = savedAfter.fileList[savedAfter.fileList.length - 1]
      assert.ok(saved.size > 0)
      await miniProgram.callWxMethod('removeSavedFile', { filePath: saved.filePath })

      const optionsForFull = await page.$$('.option')
      await optionsForFull[1].tap()
      assert.match(await optionsForFull[1].attribute('class'), /selected/)
      const fullExportButton = await waitForElement(page, '.primary-button', 'full export button did not render')
      await fullExportButton.tap()
      await waitForText(page, '.operation-title', /导出包已生成/, 'full export did not succeed')
    }

    console.log('P32WECHAT STANDARD stage=four-status-delete')
    await miniProgram.mockWxMethod('showModal', {
      errMsg: 'showModal:ok',
      confirm: true,
      cancel: false,
    })
    const pendingRecordIds = resumeAfterTwoDeletes ? recordIds.slice(2) : recordIds
    for (const recordId of pendingRecordIds) {
      page = await miniProgram.reLaunch(`/pages/record-detail/index?id=${recordId}`)
      assert.ok(page)
      await page.waitFor(1300)
      const deleteButton = await waitForElement(page, '.ownership-delete', `record ${recordId} delete action did not render`)
      assert.match(await deleteButton.text(), /删除这条记录/)
      await deleteButton.tap()
      await waitUntil(async () => (await api(`/api/records/${recordId}`)).status === 404,
        `record ${recordId} was not deleted through Mini Program detail flow`)
      await delay(700)
    }
    await miniProgram.restoreWxMethod('showModal')
    assert.equal(await totalRecords(), 2)

    console.log('P32WECHAT STANDARD stage=clear-all')
    page = await miniProgram.reLaunch('/pages/user-center/data-backup/index')
    assert.ok(page)
    await page.waitFor(1400)
    const stateList = await waitForElement(page, '.state-list', 'record state summary did not render')
    assert.match(await stateList.text(), /已保存 1/)
    assert.match(await stateList.text(), /已抵达 1/)
    const clearButton = await waitForElement(page, '.danger-button', 'clear-all action did not render')
    await clearButton.tap()
    const phraseElement = await waitForElement(page, '.phrase', 'clear-all intent was not rendered')
    const phrase = await phraseElement.text()
    assert.match(phrase, /^清除全部记录 /)
    const input = await page.$('.confirm-input')
    assert.ok(input)
    await input.input(phrase)
    const confirmButton = await page.$('.danger-button.solid')
    assert.ok(confirmButton)
    await confirmButton.tap()
    await waitUntil(async () => await totalRecords() === 0, 'clear-all UI did not delete its fixed scope')
    await waitForText(page, '.operation-title', /清理已完成/, 'clear-all success was not visible')

    console.log(`P32WECHAT STANDARD PASS resume=${resumeAfterTwoDeletes} page=true fourStatesDeleted=true clearAll=true`)
  } finally {
    miniProgram.disconnect()
  }
}

main().catch(error => {
  console.error(`P32WECHAT STANDARD FAIL ${error.message}`)
  process.exitCode = 1
})
