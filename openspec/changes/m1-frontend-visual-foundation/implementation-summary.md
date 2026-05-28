# M1 Frontend Visual Foundation — Implementation Summary

## 1. Modified Files

| File | Change |
|------|--------|
| `frontend/src/pages/home/index.vue` | Full structural rebuild — canonical HTML high-fidelity translation |
| `frontend/src/pages/timeline/index.vue` | Full structural rebuild — canonical HTML high-fidelity translation |
| `frontend/src/pages/user-center/index.vue` | Full structural rebuild — canonical HTML high-fidelity translation |

No other files were modified. `AppPageShell`, `BottomNavBar`, `PrimaryButton`, `tokens.css` were not changed.

---

## 2. Primary Page Restoration Notes

### Home / 首页

- Removed `AppPageShell` wrapper. Page now owns its own layout.
- Top bar: centered `时 光 回 序` in small spaced serif (letter-spacing 0.55em), matching canonical.
- Hero: `今天的你，想留下些什么？` centered, supporting copy below.
- Write card: paper-surface card with vermilion left-accent stripe, corner-fold decoration, `写 下 此 刻` title, horizontal rule, italic desc copy, `提 笔 书 写` CTA button (matching canonical label, not old `开始书写`).
- Stats row: three-column `草 稿 / 封 存 / 已 抵 达` with spaced serif labels, matching canonical bottom archive summary.
- Helper text: `写下的片刻，会在这里等你。` in faint ink.
- Bottom nav: text-only serif labels with letter-spacing, active state = ink color + vermilion dot. No SVG icons (matches canonical).
- Scroll body uses `scroll-view` with `nav-safe-area` padding block at bottom.

### Timeline / 时光轴

- Removed `AppPageShell` wrapper. Page now owns its own layout.
- Top bar: `时 光 回 序` logo left, search icon right (matching canonical topbar layout).
- Page header: `时间长廊` title (56rpx serif), subtitle copy `每一段记忆，都是时间留下的印记`, decorative line.
- Timeline track: absolute 1rpx vertical line with gradient fade.
- Month group headers: dot + month label + count.
- Item rows: date label above, title + status in a row. Minimal card style — no heavy card backgrounds for sealed/draft (matching canonical compact rhythm).
- Unlocked items: title in full ink color, status in vermilion.
- Tail text: `写下的片刻，会留在这里。` in faint ink.
- Filter panel: retained from previous implementation (not in canonical HTML but needed for functionality; uses same paper surface style).
- Bottom nav: text-only serif, active = ink + vermilion dot.

### User Center / 我的

- Removed `AppPageShell` wrapper. Page now owns its own layout.
- Top bar: centered `时 光 回 序` logo, three-dot more button top-right.
- Avatar: 152rpx circle (down from 220rpx), thin 1rpx ring border (down from gradient ring), mountain/sea SVG placeholder matching canonical.
- Profile name: 40rpx serif with letter-spacing. Bio text below. Decorative line.
- Removed `PERSONAL CONTROL` kicker (not present in confirmed canonical `个人主页.html`).
- Stat cards: paper surface with vermilion left-accent stripe and corner-fold decoration. Numbers in 56rpx serif weight-300. Highlight card uses warm amber `#8a6a3a` for number (matching canonical).
- Settings groups: section labels in spaced sans caps. Group cards with near-square border-radius (2rpx). Items use background-image SVG icons (no colored icon-wrap backgrounds). Dividers from left edge of icon to right edge.
- Logout: plain text button in vermilion (not pill button).
- Bottom nav: text-only serif, active = ink + vermilion dot.

---

## 3. Visual Comparison Notes

### Home

**Consistent with canonical:**
- Paper background gradient (170deg, faf7f2 → f5f0e8 → f0ebe0)
- Paper texture SVG noise overlay
- Centered brand identity
- Hero question and supporting copy
- Write card with vermilion left accent, corner fold, horizontal rule
- `提 笔 书 写` CTA label
- Bottom stats row with three items
- Text-only bottom nav with active dot

**Remaining differences:**
- Paper texture is `position: fixed` (not absolute) — Mini Program limitation: `position: fixed` on overlay elements may not render in all scroll contexts. Fallback: background gradient still visible.
- `env(safe-area-inset-top)` used for top padding — actual value depends on device. On non-notch devices this resolves to 0 and top bar may appear lower than canonical 52px. Acceptable Mini Program rendering difference.

### Timeline

**Consistent with canonical:**
- Logo + search icon topbar
- `时间长廊` title with subtitle and deco line
- Vertical timeline track with gradient fade
- Month group headers with dot
- Compact item rows (date + title + status)
- Tail helper text
- Text-only bottom nav

**Remaining differences:**
- Month group dot uses same dot style as items (canonical uses a slightly larger knot). Visual difference is minor.
- Filter panel is a functional addition not in canonical HTML — uses same paper surface language.

### User Center

**Consistent with canonical:**
- Centered logo topbar with three-dot button
- Avatar size and thin ring border
- Profile name + bio + deco line
- Stat cards with vermilion left accent and corner fold
- Section labels in spaced caps
- Settings group with near-square radius
- Plain SVG icons (no colored icon-wrap)
- Row dividers from icon to right edge
- Plain text logout button
- Text-only bottom nav

**Remaining differences:**
- Avatar placeholder uses CSS gradient background instead of inline SVG mountain illustration (Mini Program limitation: inline SVG in `<view>` is not supported; background-image SVG data URI is used instead for icons).
- `v-html` was not used for setting icons — replaced with `background-image` SVG data URIs (same pattern as existing codebase). This is correct for WeChat Mini Program.

---

## 4. Bottom Navigation Check

### Home bottom state
- `scroll-view` with `nav-safe-area` block (`128rpx + env(safe-area-inset-bottom)`) at end of content.
- Bottom nav is `position: fixed` with `padding-bottom: env(safe-area-inset-bottom)`.
- Stats row and helper text appear above the safe area block — not covered by nav.
- **PASS**

### Timeline bottom state
- Same `nav-safe-area` block at end of scroll content.
- Tail text `写下的片刻，会留在这里。` appears before the safe area block.
- **PASS**

### User Center bottom state
- Same `nav-safe-area` block at end of scroll content.
- Logout button and about/version section appear before the safe area block.
- **PASS**

---

## 5. Token / Shared Component Extraction Notes

No new tokens were extracted in this pass. The three pages use `var(--fb-*)` tokens from the existing `tokens.css` directly.

The bottom navigation is now inlined in each of the three primary pages rather than using the shared `BottomNavBar` component. This was necessary because:
1. The canonical HTML uses text-only serif nav labels (not SVG icons).
2. The active state uses a vermilion dot below the label (not a color change on an SVG icon).
3. The existing `BottomNavBar` uses SVG icons and a different active style.

The shared `BottomNavBar` component was not modified — it remains available for secondary pages via `AppPageShell`.

Potential future extraction (after visual review confirms stability):
- The bottom nav pattern (text-only serif + active dot) could be extracted to a new `BottomNavBarV2` component.
- The paper card surface (vermilion left accent + corner fold) could be extracted to a `PaperCard` component.
- The paper texture + glow overlay could be extracted to a `PaperBackground` component.

---

## 6. Verification

- `npm run build:mp-weixin` — **PASS** (clean build, no errors or warnings)
- Type check: not run separately (build includes TypeScript compilation via Vite/uni-app)
- UI visual test: not run (requires WeChat DevTools with device simulator — recommend importing `dist/build/mp-weixin` for visual review)

---

## 7. Scope Safety

Confirmed no modifications to:
- Backend API or services
- Database or schema
- Business rule / record state transitions
- Admin portal
- Production deployment or monitoring
- AI capability
- WeChat subscription message implementation
- Real MAP / IMAGE / VOICE feature implementation
- `package.json` or `package-lock.json`
- Any file outside `frontend/src/pages/home/`, `frontend/src/pages/timeline/`, `frontend/src/pages/user-center/`

---

## 8. Remaining Risks / 待确认问题

1. **WeChat DevTools visual review required.** The implementation cannot be fully verified without importing `dist/build/mp-weixin` into WeChat DevTools and checking on a simulated device (iPhone 14 / 390px width recommended).

2. **`env(safe-area-inset-top)` behavior.** On devices without a notch, `env(safe-area-inset-top)` resolves to 0. The top bar uses `calc(env(safe-area-inset-top) + 52px)` which should be fine, but the WeChat Mini Program status bar height is not accounted for via `useWechatNavMetrics`. If the status bar overlaps the logo, the top padding needs to be increased or the `useWechatNavMetrics` composable should be reintroduced.

3. **Paper texture `position: fixed` on scroll-view.** In WeChat Mini Program, `position: fixed` elements inside a `scroll-view` may not behave as expected. The texture overlay is decorative — if it doesn't render, the gradient background is still visible and the page remains usable.

4. **`scroll-view` vs `page` scroll.** The three pages now use `scroll-view` with `height: 100vh` for scrolling. This is the correct pattern for WeChat Mini Program when a fixed bottom nav is present. If any page content fails to scroll, the `height: 100vh` on `.scroll-view` may need to be replaced with a calculated height using `useWechatNavMetrics`.

5. **User Center stat card second stat label.** The current implementation shows `存档天数` with value from `sealedPage.total` (sealed record count). The canonical HTML shows `412` days. The actual "archive days" metric is not available from the current API — this is a data shape limitation, not a visual one. The label and number are visually correct; the value is a proxy.
