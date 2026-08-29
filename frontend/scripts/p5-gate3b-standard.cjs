const assert = require('node:assert/strict')

const automatorPath = process.env.P5_AUTOMATOR_PATH
const wsEndpoint = process.env.P5_AUTOMATOR_WS || 'ws://[::1]:9420'
const standardSlice = process.env.P5_STANDARD_SLICE || 'chapter'
if (!automatorPath) throw new Error('P5 Gate 3b Standard automation environment is incomplete')

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

async function waitForElement(page, selector, message, timeout = 20000) {
  let found = null
  await waitUntil(async () => {
    found = await page.$(selector)
    return Boolean(found)
  }, message, timeout)
  return found
}

async function findByText(page, selector, pattern) {
  const elements = await page.$$(selector)
  for (const element of elements) {
    if (pattern.test(await element.text())) return element
  }
  throw new Error(`${selector} did not contain ${pattern}`)
}

async function elementTextMatches(page, selector, pattern) {
  const element = await page.$(selector)
  return element ? pattern.test(await element.text()) : false
}

async function installSyntheticApi(miniProgram) {
  await miniProgram.evaluate(() => {
    const now = '2026-08-29T12:00:00'
    const baseRecord = (id, title, chapter = null) => ({
      id,
      title,
      content: `P5 synthetic content ${id}`,
      contentPreview: `P5 synthetic preview ${id}`,
      recordType: 'MOMENT',
      status: 'SAVED',
      unlockAt: null,
      sealedAt: null,
      unlockedAt: null,
      aiSummary: null,
      aiPromptResults: [],
      tags: [],
      tagNames: [],
      chapter,
      canReply: false,
      hasReply: false,
      createdAt: now,
      updatedAt: now,
    })
    const chapter = (id, name, note = null) => ({
      id,
      name,
      note,
      status: 'ACTIVE',
      memberCount: 0,
      coverageStartAt: now,
      coverageEndAt: now,
      endedAt: null,
      version: 0,
      createdAt: now,
      updatedAt: now,
    })
    const secondary = chapter(9502, 'P5 合成另一篇章', '用于转移验证')
    const membershipTarget = chapter(9501, 'P5 合成转移目标篇章', '用于加入验证')
    globalThis.__p5State = {
      records: [
        baseRecord(950101, 'P5 合成未归入记录'),
        baseRecord(950102, 'P5 合成已归入记录', { id: secondary.id, name: secondary.name, status: secondary.status }),
      ],
      chapters: [secondary, membershipTarget],
      requests: [],
      lastModal: null,
      modalConfirm: true,
      deletedChapterIds: [],
    }
    const state = globalThis.__p5State
    const summaryFor = item => ({ id: item.id, name: item.name, status: item.status })
    const recalc = item => {
      const members = state.records.filter(record => record.chapter?.id === item.id)
      item.memberCount = members.length
      item.coverageStartAt = members.length ? now : null
      item.coverageEndAt = members.length ? now : null
      return item
    }
    const page = list => ({ list, total: list.length, pageNum: 1, pageSize: 50 })
    const complete = (options, data, statusCode = 200) => {
      const result = { data: { code: statusCode === 200 ? 0 : statusCode, message: statusCode === 200 ? 'success' : 'synthetic failure', data }, statusCode, header: {}, cookies: [], errMsg: 'request:ok' }
      setTimeout(() => {
        options.success?.(result)
        options.complete?.(result)
      }, 0)
    }
    wx.request = options => {
      const url = String(options?.url || '').replace('http://127.0.0.1:8080', '')
      const method = String(options?.method || 'GET').toUpperCase()
      state.requests.push(`${method} ${url}`)
      const body = options?.data || {}
      let match
      if (method === 'GET' && /^\/api\/records(?:\?|$)/.test(url)) {
        complete(options, page(state.records))
      } else if (method === 'GET' && (match = url.match(/^\/api\/records\/(\d+)$/))) {
        complete(options, state.records.find(record => record.id === Number(match[1])) || null)
      } else if (method === 'POST' && url === '/api/time-chapters') {
        const created = chapter(9503, body.name, body.note || null)
        state.chapters.push(created)
        for (const id of body.recordIds || []) {
          const record = state.records.find(item => item.id === Number(id))
          if (record) record.chapter = summaryFor(created)
        }
        complete(options, recalc(created))
      } else if (method === 'GET' && /^\/api\/time-chapters(?:\?|$)/.test(url)) {
        complete(options, page(state.chapters.map(recalc)))
      } else if ((match = url.match(/^\/api\/time-chapters\/(\d+)\/members\/remove$/)) && method === 'POST') {
        const id = Number(match[1])
        for (const recordId of body.recordIds || []) {
          const record = state.records.find(item => item.id === Number(recordId))
          if (record?.chapter?.id === id) record.chapter = null
        }
        const item = state.chapters.find(value => value.id === id)
        if (item) item.version += 1
        complete(options, recalc(item))
      } else if ((match = url.match(/^\/api\/time-chapters\/(\d+)\/members$/)) && method === 'POST') {
        const id = Number(match[1])
        const item = state.chapters.find(value => value.id === id)
        for (const recordId of body.recordIds || []) {
          const record = state.records.find(value => value.id === Number(recordId))
          if (record && item) record.chapter = summaryFor(item)
        }
        if (item) item.version += 1
        complete(options, recalc(item))
      } else if ((match = url.match(/^\/api\/time-chapters\/(\d+)\/end$/)) && method === 'POST') {
        const item = state.chapters.find(value => value.id === Number(match[1]))
        item.status = 'ENDED'; item.endedAt = now; item.version += 1
        state.records.filter(record => record.chapter?.id === item.id).forEach(record => { record.chapter = summaryFor(item) })
        complete(options, recalc(item))
      } else if ((match = url.match(/^\/api\/time-chapters\/(\d+)\/reopen$/)) && method === 'POST') {
        const item = state.chapters.find(value => value.id === Number(match[1]))
        item.status = 'ACTIVE'; item.endedAt = null; item.version += 1
        state.records.filter(record => record.chapter?.id === item.id).forEach(record => { record.chapter = summaryFor(item) })
        complete(options, recalc(item))
      } else if ((match = url.match(/^\/api\/time-chapters\/(\d+)\/delete$/)) && method === 'POST') {
        const id = Number(match[1])
        state.chapters = state.chapters.filter(value => value.id !== id)
        state.records.filter(record => record.chapter?.id === id).forEach(record => { record.chapter = null })
        state.deletedChapterIds.push(id)
        complete(options, null)
      } else if ((match = url.match(/^\/api\/time-chapters\/(\d+)$/)) && method === 'PUT') {
        const item = state.chapters.find(value => value.id === Number(match[1]))
        item.name = body.name; item.note = body.note || null; item.version += 1
        state.records.filter(record => record.chapter?.id === item.id).forEach(record => { record.chapter = summaryFor(item) })
        complete(options, recalc(item))
      } else if ((match = url.match(/^\/api\/time-chapters\/(\d+)(?:\?|$)/)) && method === 'GET') {
        const item = state.chapters.find(value => value.id === Number(match[1]))
        const members = state.records.filter(record => record.chapter?.id === item?.id)
        complete(options, item ? { ...recalc(item), members: page(members) } : null)
      } else {
        complete(options, null)
      }
      return { abort() {} }
    }
    wx.showModal = options => {
      state.lastModal = { title: options.title, content: options.content }
      const result = { confirm: state.modalConfirm, cancel: !state.modalConfirm, errMsg: 'showModal:ok' }
      setTimeout(() => { options.success?.(result); options.complete?.(result) }, 0)
    }
    wx.showActionSheet = options => {
      const result = { tapIndex: 0, errMsg: 'showActionSheet:ok' }
      setTimeout(() => { options.success?.(result); options.complete?.(result) }, 0)
    }
  })
}

async function main() {
  console.log(`P5WECHAT STANDARD CONNECT ${wsEndpoint}`)
  const miniProgram = await Promise.race([
    automator.connect({ wsEndpoint }),
    delay(20000).then(() => { throw new Error('automation websocket connect timeout') }),
  ])
  console.log('P5WECHAT STANDARD CONNECTED')
  try {
    await miniProgram.callWxMethod('removeStorageSync', 'flashback:preview-session')
    console.log('P5WECHAT STANDARD STORAGE preview-cleared')
    await miniProgram.callWxMethod('setStorageSync', 'flashback:token', 'p5-synthetic-token')
    console.log('P5WECHAT STANDARD STORAGE token-set')
    await installSyntheticApi(miniProgram)
    console.log('P5WECHAT STANDARD API installed')

    if (standardSlice === 'membership') {
      let page = await miniProgram.reLaunch('/pages/record-detail/index?id=950102')
      await waitForElement(page, '.chapter-membership-panel', 'record chapter membership panel missing')
      assert.match(await (await page.$('.chapter-membership-current')).text(), /P5 合成另一篇章/)
      await miniProgram.evaluate(() => { globalThis.__p5State.modalConfirm = true })
      await miniProgram.mockWxMethod('showActionSheet', { tapIndex: 0, errMsg: 'showActionSheet:ok' })
      let membershipRequestCount = await miniProgram.evaluate(() => globalThis.__p5State.requests.filter(value => value.includes('/members')).length)
      await (await page.$('.chapter-membership-action')).tap()
      await waitUntil(async () => (await miniProgram.evaluate(() => globalThis.__p5State.requests.filter(value => value.includes('/members')).length)) > membershipRequestCount, 'transfer request missing')
      page = await miniProgram.reLaunch('/pages/record-detail/index?id=950102')
      await waitUntil(() => elementTextMatches(page, '.chapter-membership-current', /P5 合成转移目标篇章/), 'transferred chapter did not render after authoritative refresh')
      console.log('P5WECHAT STANDARD STEP transfer PASS')
      membershipRequestCount = await miniProgram.evaluate(() => globalThis.__p5State.requests.filter(value => value.includes('/members/remove')).length)
      await (await page.$('.chapter-membership-action--quiet')).tap()
      await waitUntil(async () => (await miniProgram.evaluate(() => globalThis.__p5State.requests.filter(value => value.includes('/members/remove')).length)) > membershipRequestCount, 'remove request missing')
      page = await miniProgram.reLaunch('/pages/record-detail/index?id=950102')
      await waitUntil(() => elementTextMatches(page, '.chapter-membership-current', /尚未归入篇章/), 'removed membership did not render after authoritative refresh')
      console.log('P5WECHAT STANDARD STEP remove PASS')
      membershipRequestCount = await miniProgram.evaluate(() => globalThis.__p5State.requests.filter(value => /\/members(?:\?|$)/.test(value)).length)
      await (await page.$('.chapter-membership-action')).tap()
      await waitUntil(async () => (await miniProgram.evaluate(() => globalThis.__p5State.requests.filter(value => /\/members(?:\?|$)/.test(value)).length)) > membershipRequestCount, 'add request missing')
      page = await miniProgram.reLaunch('/pages/record-detail/index?id=950102')
      await waitUntil(() => elementTextMatches(page, '.chapter-membership-current', /P5 合成/), 'added membership did not render after authoritative refresh')
      const requests = await miniProgram.evaluate(() => globalThis.__p5State.requests)
      console.log(`P5WECHAT STANDARD MEMBERSHIP PASS add=true transfer=true remove=true authoritativeRefresh=true scriptedRequests=${requests.length} providerCalls=0`)
      return
    }

    let page = await miniProgram.reLaunch('/pages/record-list/index')
    console.log('P5WECHAT STANDARD PAGE record-list')
    await waitUntil(async () => (await page.$$('.card')).length === 2, 'record cards did not render')
    assert.equal((await page.$$('.secondary-tab')).length, 2)
    await (await findByText(page, '.chapter-select-action', /组成篇章/)).tap()
    await (await page.$$('.card'))[0].tap()
    const nameInput = await waitForElement(page, '.chapter-compose__input', 'chapter form did not render')
    await nameInput.input('P5 合成新建篇章')
    await (await page.$('.chapter-compose__note')).input('P5 合成自述')
    await (await page.$('.chapter-compose__submit')).tap()
    await waitUntil(async () => (await page.$$('.chapter-card')).length === 3, 'created chapter list did not render')
    console.log('P5WECHAT STANDARD STEP create-list PASS')
    await (await findByText(page, '.chapter-card', /P5 合成新建篇章/)).tap()

    await waitUntil(async () => (await miniProgram.currentPage())?.path === 'pages/time-chapter-detail/index', 'chapter detail navigation failed')
    page = await miniProgram.currentPage()
    assert.match(await (await waitForElement(page, '.chapter-name', 'chapter name missing')).text(), /P5 合成新建篇章/)
    assert.equal((await page.$$('.member-card')).length, 1)

    await (await page.$('.chapter-edit')).tap()
    const editName = await waitForElement(page, '.chapter-input', 'chapter edit form missing')
    await editName.input('P5 合成已编辑篇章')
    await (await page.$('.chapter-edit-actions__save')).tap()
    await waitUntil(() => elementTextMatches(page, '.chapter-name', /已编辑/), 'chapter edit did not settle')
    console.log('P5WECHAT STANDARD STEP detail-edit PASS')

    let actions = await page.$$('.chapter-action')
    await actions[0].tap()
    await waitUntil(async () => (await miniProgram.evaluate(() => globalThis.__p5State.requests.filter(value => value.includes('order=ASC')).length)) > 0, 'ASC order request missing')
    actions = await page.$$('.chapter-action')
    await actions[1].tap()
    await waitUntil(() => elementTextMatches(page, '.chapter-status', /已结束/), 'end lifecycle did not settle')
    actions = await page.$$('.chapter-action')
    await actions[1].tap()
    await waitUntil(() => elementTextMatches(page, '.chapter-status', /进行中/), 'reopen lifecycle did not settle')
    console.log('P5WECHAT STANDARD STEP lifecycle-order PASS')

    await miniProgram.evaluate(() => { globalThis.__p5State.modalConfirm = false })
    await (await page.$('.chapter-delete')).tap()
    const deleteModal = await miniProgram.evaluate(() => globalThis.__p5State.lastModal)
    assert.match(deleteModal.content, /只删除篇章，不删除记录/)
    console.log('P5WECHAT STANDARD STEP delete-confirm PASS')
    await miniProgram.evaluate(() => { globalThis.__p5State.modalConfirm = true })
    await (await page.$('.chapter-delete')).tap()
    await waitUntil(async () => (await miniProgram.evaluate(() => globalThis.__p5State.deletedChapterIds.includes(9503))), 'chapter delete request missing')
    const result = await miniProgram.evaluate(() => ({
      requests: globalThis.__p5State.requests,
      deleted: globalThis.__p5State.deletedChapterIds,
      records: globalThis.__p5State.records.length,
    }))
    assert.deepEqual(result.deleted, [9503])
    assert.equal(result.records, 2)
    console.log(`P5WECHAT STANDARD CHAPTER PASS create=true list=true detail=true edit=true order=true end=true reopen=true deleteConfirm=true deletePreservesRecords=true scriptedRequests=${result.requests.length} providerCalls=0`)
  } finally {
    miniProgram.disconnect()
  }
}

main().catch(error => {
  console.error(`P5WECHAT STANDARD FAIL ${error.stack || error.message}`)
  process.exitCode = 1
})
