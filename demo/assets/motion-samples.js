(function () {
  'use strict';
  if (!document.body.classList.contains('demo-motion')) return;

  const reduced = window.matchMedia('(prefers-reduced-motion: reduce)');
  const glass = Array.from(document.querySelectorAll('.motion-glass'));
  const resets = [];
  glass.forEach(button => {
    let pointer = null, keyboard = false, timer = null;
    function reset() {
      clearTimeout(timer); timer = null;
      pointer = null; keyboard = false;
      button.classList.remove('is-pressed', 'is-releasing');
    }
    function press(event) {
      if (button.disabled || reduced.matches) return false;
      clearTimeout(timer); timer = null;
      button.classList.remove('is-releasing');
      if (Number.isFinite(event.clientX) && Number.isFinite(event.clientY)) {
        const rect = button.getBoundingClientRect();
        if (rect.width && rect.height) {
          const clamp = value => Math.max(5, Math.min(95, value));
          button.style.setProperty('--glass-x', clamp((event.clientX - rect.left) / rect.width * 100) + '%');
          button.style.setProperty('--glass-y', clamp((event.clientY - rect.top) / rect.height * 100) + '%');
        }
      } else {
        button.style.removeProperty('--glass-x');
        button.style.removeProperty('--glass-y');
      }
      button.classList.add('is-pressed');
      return true;
    }
    function release() {
      const pressed = button.classList.contains('is-pressed');
      button.classList.remove('is-pressed');
      pointer = null; keyboard = false;
      if (!pressed || button.disabled || reduced.matches) { reset(); return; }
      button.classList.add('is-releasing');
      timer = setTimeout(() => {
        button.classList.remove('is-releasing'); timer = null;
      }, 280);
    }
    button.addEventListener('pointerdown', event => {
      if (!event.isPrimary || event.button !== 0) return;
      if (press(event)) pointer = event.pointerId;
    });
    window.addEventListener('pointerup', event => {
      if (pointer !== null && event.pointerId === pointer) release();
    });
    window.addEventListener('pointercancel', event => {
      if (pointer !== null && event.pointerId === pointer) reset();
    });
    button.addEventListener('pointerleave', () => { if (pointer !== null) reset(); });
    button.addEventListener('keydown', event => {
      if ((event.key === ' ' || event.key === 'Enter') && !event.repeat) keyboard = press(event);
    });
    button.addEventListener('keyup', event => {
      if (keyboard && (event.key === ' ' || event.key === 'Enter')) release();
    });
    button.addEventListener('blur', reset);
    resets.push(reset);
  });

  const scene = document.querySelector('[data-motion="paper"]');
  const replay = document.querySelector('[data-motion="replay"]');
  const tools = document.querySelector('[data-motion="tools"]');
  const label = replay && replay.querySelector('.motion-glass-label');
  const supports3d = typeof CSS !== 'undefined' && CSS.supports('perspective', '1100px') &&
    CSS.supports('transform-style', 'preserve-3d');
  let paperTimer = null;
  function finishPaper() {
    clearTimeout(paperTimer); paperTimer = null;
    if (scene) scene.classList.remove('is-turning');
    if (replay) replay.removeAttribute('aria-busy');
  }
  function eligiblePaper() {
    return scene && supports3d && !reduced.matches && !document.hidden &&
      !scene.closest('[hidden]') && scene.offsetHeight > 0 && scene.offsetHeight <= 620;
  }
  function syncPaper() {
    if (!scene || !replay || !tools) return;
    const eligible = eligiblePaper();
    if (!eligible) finishPaper();
    tools.hidden = false; replay.hidden = false;
    replay.disabled = !eligible;
    label.textContent = eligible ? '再翻开一次' : '信纸已展开';
  }
  function openPaper() {
    syncPaper();
    if (!eligiblePaper() || scene.classList.contains('is-turning')) return;
    scene.classList.add('is-turning');
    replay.setAttribute('aria-busy', 'true');
    // 即使 animationend 没有触发，也不能让装饰纸面长期覆盖正文。
    paperTimer = setTimeout(finishPaper, 720);
  }
  if (scene && replay && tools && label) {
    replay.addEventListener('click', openPaper);
    scene.addEventListener('animationend', event => {
      if (event.animationName === 'sample-paper-open') finishPaper();
    });
    scene.addEventListener('pointerdown', finishPaper, { passive: true });
    scene.addEventListener('focusin', finishPaper);
    const reader = document.querySelector('main.page');
    if (reader) reader.addEventListener('scroll', finishPaper, { passive: true });
    window.addEventListener('resize', () => { finishPaper(); syncPaper(); });
    if (document.fonts) document.fonts.addEventListener('loadingdone', () => { finishPaper(); syncPaper(); });
    openPaper();
  }
  function stop() { finishPaper(); resets.forEach(reset => reset()); }
  function preferenceChanged() { stop(); syncPaper(); }
  if (reduced.addEventListener) reduced.addEventListener('change', preferenceChanged);
  else reduced.addListener(preferenceChanged);
  window.addEventListener('pagehide', stop);
  window.addEventListener('pageshow', event => {
    if (event.persisted) { stop(); syncPaper(); }
  });
  document.addEventListener('visibilitychange', () => {
    if (document.hidden) stop();
    else syncPaper();
  });
})();
