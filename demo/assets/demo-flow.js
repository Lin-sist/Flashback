(function () {
  'use strict';
  if (!globalThis.FlashbackDemoStore) return;
  const page = document.body.dataset.page;
  const store = FlashbackDemoStore.createStore(() => window.sessionStorage);
  const find = role => document.querySelector('[data-role="' + role + '"]');
  const text = (role, value) => { const node = find(role); if (node) node.textContent = value; };
  const hide = (role, hidden) => { const node = find(role); if (node) node.hidden = hidden; };
  const messages = {
    unavailable: '本标签页无法使用演示存储。请保留当前输入，在支持同源会话存储的环境中再试。',
    quota: '演示存储空间不足，当前输入仍在。可先清除本轮演示后重试。',
    corrupt: '演示数据无法读取。请回原型导航清除本轮演示后重试。',
    version: '演示数据版本不同。请回原型导航清除本轮演示后重试。',
    verify: '未能确认写入，当前输入仍在；没有跳转到保存成功页。',
    limit: '标题最多 80 字，正文最多 4000 字。请缩短后再保存，原文不会自动截断。',
    empty: '先写下一句测试文字，再留下这一刻。'
  };
  function status(message, error = false) {
    const node = find('status');
    if (node) { node.textContent = message; node.dataset.error = String(error); }
  }
  function date(value) {
    return new Intl.DateTimeFormat('zh-CN', { month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit', hour12: false }).format(new Date(value));
  }
  const dynamic = new URLSearchParams(location.search).get('demo') === 'latest';
  function emptyView(result) {
    hide('record-view', true);
    hide('empty-view', false);
    status(result.ok ? '本标签页还没有演示记录，请先去书写。' : messages[result.error], !result.ok);
  }
  function renderSaved() {
    if (!dynamic) return;
    const result = store.read();
    if (!result.ok || !result.state.savedRecord) { emptyView(result); return; }
    const record = result.state.savedRecord;
    hide('empty-view', true); hide('record-view', false);
    text('record-title', record.title || '未命名的此刻'); text('record-body', record.body);
    text('record-time', '本地留下于 ' + date(record.updatedAt));
    text('confirmation', '这一刻已留在本标签页');
    const stamp = find('stamp');
    stamp.classList.remove('m-stamp');
    const receipt = store.consumeReceipt();
    if (receipt.animate) stamp.classList.add('m-stamp');
    status(receipt.ok ? '本地演示已保存，不代表正式账号数据。' : '记录已保存；本次动画回执未能更新。', !receipt.ok);
  }
  function renderLook() {
    if (!dynamic) return;
    const result = store.read();
    if (!result.ok || !result.state.savedRecord) { emptyView(result); return; }
    const record = result.state.savedRecord;
    hide('empty-view', true); hide('record-view', false);
    text('record-title', record.title || '未命名的此刻'); text('record-body', record.body);
    text('archive-line', '本标签页 · 文字演示'); text('archive-season', date(record.createdAt));
    text('reader-text', '刚刚的你 · 本地演示记录');
    text('record-where', '这条演示仅包含文字，未添加位置、照片或声音。');
    hide('fragments', true); hide('reply', true); hide('chat', true); hide('respond', true);
    hide('edit-link', false);
    status('原文字已从本标签页演示存储读取。');
  }
  function renderTimeline() {
    const item = find('demo-entry');
    const result = store.read();
    item.hidden = true;
    if (!result.ok) { status(messages[result.error], true); return; }
    const record = result.state.savedRecord;
    if (!record) { status('以下为固定合成样本；你留下的演示文字会出现在此处。'); return; }
    item.hidden = false;
    text('demo-date', date(record.updatedAt) + ' · 本地演示');
    text('demo-title', record.title || '未命名的此刻');
    text('demo-excerpt', Array.from(record.body).slice(0, 80).join('') + (Array.from(record.body).length > 80 ? '…' : ''));
    status('已留下 1 条本地演示记录，以下仍为固定合成样本。');
  }
  function renderHome() {
    const result = store.read();
    const record = result.ok && result.state.savedRecord;
    hide('home-demo', !record);
    if (record) text('home-demo-title', record.title || '未命名的此刻');
    if (!result.ok) status(messages[result.error], true);
  }
  let flushDraft = null;
  let dirty = false;
  function bindWriting() {
    const title = find('title-input'), body = find('body-input'), button = find('save');
    const initial = { title: title.value, body: body.value };
    let timer = null, composing = false, saving = false;
    const input = () => ({ title: title.value, body: body.value });
    function sizeBody() {
      body.style.height = 'auto';
      body.style.height = Math.min(Math.max(body.scrollHeight, 92), 420) + 'px';
    }
    flushDraft = () => {
      clearTimeout(timer);
      if (composing) return false;
      if (!dirty) return true;
      const result = store.saveDraft(input());
      if (!result.ok) { status(messages[result.error], true); return false; }
      dirty = false;
      status('测试草稿已留在本标签页；点「留下这一刻」确认保存。');
      return true;
    };
    const changed = event => {
      dirty = true; sizeBody();
      clearTimeout(timer);
      if (!composing && !event.isComposing) timer = setTimeout(flushDraft, 350);
    };
    [title, body].forEach(node => {
      node.addEventListener('input', changed);
      node.addEventListener('compositionstart', () => { composing = true; clearTimeout(timer); });
      node.addEventListener('compositionend', () => { composing = false; changed({ isComposing: false }); });
    });
    document.querySelectorAll('a[href]').forEach(link => link.addEventListener('click', event => {
      if (!flushDraft()) event.preventDefault();
    }));
    button.disabled = false;
    button.addEventListener('click', () => {
      if (saving || composing) return;
      clearTimeout(timer); saving = true; button.disabled = true;
      const result = store.save(input());
      if (result.ok) { dirty = false; location.assign('saved.html?demo=latest'); return; }
      status(messages[result.error], true); saving = false; button.disabled = false;
      if (result.error === 'empty') body.focus();
    });
    window.addEventListener('pagehide', () => { if (dirty && !composing) flushDraft(); });
    document.addEventListener('visibilitychange', () => { if (document.hidden && dirty && !composing) flushDraft(); });
    function restore() {
      if (dirty) { sizeBody(); return; }
      const result = store.read();
      if (!result.ok) { status(messages[result.error], true); sizeBody(); return; }
      const data = result.state.draft || result.state.savedRecord;
      title.value = data ? data.title : initial.title;
      body.value = data ? data.body : initial.body;
      status(data ? '已恢复本标签页的测试文字。' : '本地演示 · 不上传，请只输入测试文字。');
      sizeBody();
    }
    restore();
    window.addEventListener('pageshow', restore);
  }
  function bindClear(role, prompt, after) {
    const button = find(role);
    if (!button) return;
    button.disabled = false;
    button.addEventListener('click', () => {
      if (!window.confirm(prompt)) return;
      const result = store.clear();
      if (!result.ok) { status(messages[result.error], true); return; }
      status('本轮演示已清除。其它已开页面请刷新。');
      if (after) after();
    });
  }
  if (page === 'writing') bindWriting();
  if (page === 'saved' && dynamic) {
    bindClear('delete-record', '只删除本标签页的演示记录和草稿，不影响正式账号数据。确认删除？', () => { location.assign('home.html'); });
  }
  if (page === 'gallery') bindClear('reset', '清除本标签页的演示记录和草稿？只影响本轮演示。');
  const render = { home: renderHome, saved: renderSaved, timeline: renderTimeline, look: renderLook }[page];
  if (render) { render(); window.addEventListener('pageshow', event => { if (event.persisted) render(); }); }
  if ((page === 'saved' || page === 'look') && dynamic) {
    const heading = find(page === 'saved' ? 'confirmation' : 'record-title');
    if (heading && !find('record-view').hidden) heading.focus();
  }
})();
