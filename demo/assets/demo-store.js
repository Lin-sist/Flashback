/* Demo only. No business API, long-term storage, or content logging. */
(function (root) {
  'use strict';
  const KEY = 'flashback.demo.v1.session';
  const LIMITS = Object.freeze({ title: 80, body: 4000 });
  const empty = () => ({ schemaVersion: 1, draft: null, savedRecord: null, lastSaveReceipt: null });
  const fail = error => ({ ok: false, error });
  const validText = value => value && typeof value.title === 'string' && typeof value.body === 'string'
    && Array.from(value.title).length <= LIMITS.title && Array.from(value.body).length <= LIMITS.body;
  const validDate = value => typeof value === 'string' && /^\d{4}-\d{2}-\d{2}T/.test(value)
    && Number.isFinite(Date.parse(value));
  function validate(state) {
    if (!state || state.schemaVersion !== 1) return 'version';
    if (state.draft !== null && !validText(state.draft)) return 'corrupt';
    const record = state.savedRecord;
    if (record !== null && (!validText(record) || record.id !== 'demo-latest' || !record.body.trim()
      || !validDate(record.createdAt) || !validDate(record.updatedAt)
      || !Number.isSafeInteger(record.revision) || record.revision < 1)) return 'corrupt';
    if (state.lastSaveReceipt !== null && (!record || state.lastSaveReceipt !== record.revision)) return 'corrupt';
    return null;
  }
  function classify(error) {
    if (error && error.name === 'QuotaExceededError') return 'quota';
    return 'unavailable';
  }
  function createStore(getStorage, now = () => new Date().toISOString()) {
    function read() {
      try {
        const raw = getStorage().getItem(KEY);
        if (raw === null) return { ok: true, state: empty() };
        let state;
        try { state = JSON.parse(raw); } catch (_) { return fail('corrupt'); }
        const error = validate(state);
        return error ? fail(error) : { ok: true, state };
      } catch (error) { return fail(classify(error)); }
    }
    function write(state) {
      const error = validate(state);
      if (error) return fail(error);
      try {
        const storage = getStorage();
        const raw = JSON.stringify(state);
        storage.setItem(KEY, raw);
        if (storage.getItem(KEY) !== raw) return fail('verify');
        return { ok: true, state };
      } catch (error) { return fail(classify(error)); }
    }
    function checkInput(text, requireBody) {
      if (!text || typeof text.title !== 'string' || typeof text.body !== 'string') return 'corrupt';
      if (!validText(text)) return 'limit';
      return requireBody && !text.body.trim() ? 'empty' : null;
    }
    function saveDraft(text) {
      const error = checkInput(text, false);
      if (error) return fail(error);
      const result = read();
      if (!result.ok) return result;
      return write({ ...result.state, draft: { title: text.title, body: text.body } });
    }
    function save(text) {
      const error = checkInput(text, true);
      if (error) return fail(error);
      const result = read();
      if (!result.ok) return result;
      const previous = result.state.savedRecord;
      const timestamp = now();
      const record = {
        id: 'demo-latest', title: text.title, body: text.body,
        createdAt: previous ? previous.createdAt : timestamp,
        updatedAt: timestamp, revision: previous ? previous.revision + 1 : 1
      };
      return write({ schemaVersion: 1, draft: { title: text.title, body: text.body },
        savedRecord: record, lastSaveReceipt: record.revision });
    }
    function consumeReceipt() {
      const result = read();
      if (!result.ok || !result.state.lastSaveReceipt) return { ...result, animate: false };
      const written = write({ ...result.state, lastSaveReceipt: null });
      return { ...written, animate: written.ok };
    }
    function clear() {
      try {
        const storage = getStorage();
        storage.removeItem(KEY);
        return storage.getItem(KEY) === null ? { ok: true, state: empty() } : fail('verify');
      } catch (error) { return fail(classify(error)); }
    }
    return Object.freeze({ read, saveDraft, save, consumeReceipt, clear });
  }
  root.FlashbackDemoStore = Object.freeze({ KEY, LIMITS, createStore });
})(globalThis);
