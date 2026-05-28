# M1 Frontend Visual Foundation Design / M1 前端视觉基础设计

## Design Intent / 设计意图

M1 SHOULD make the Mini Program feel like a quiet paper room where the user can write down the present and later return to it through time.

M1 is not a theme skin, color replacement, token-only refactor, or minor polish pass. M1 is a high-fidelity visual translation of the exact finalized HTML files listed in `visual-reference-map.md`.

The visual foundation serves the V2.0 loop:

```text
写下此刻 -> 保存此刻 / 交给时间 -> 我的记录 / 时光轴 -> 记录抵达 -> 时间回看 -> 可选回信
```

## Current Baseline / 当前基线

- Stack: Uniapp + Vue 3 + Pinia.
- Target: WeChat Mini Program.
- Pages are declared in `frontend/src/pages.json`.
- Shared UI is under `frontend/src/components/common`.
- Existing visual tokens are in `frontend/src/styles/tokens.css`.
- Preview/demo data exists in `frontend/src/features/preview`.
- Existing frontend MAY provide routing, data shape, state display, and business-safe behavior.
- Existing frontend MUST NOT override finalized HTML when layout, hierarchy, density, or navigation behavior conflicts.

## Confirmed Visual Direction / 已确认视觉方向

M1 uses the V2 paper/vermilion direction in `Docs/design/home-v2/时光回序_设计规范.md`.

M1 MUST use:

- `openspec/changes/m1-frontend-visual-foundation/visual-reference-map.md`
- the exact canonical HTML listed for the target page
- `Docs/design/home-v2/时光回序_设计规范.md`
- the matching screenshot reference when available

Agents MUST NOT treat `Docs/design/home-v2/*.html` as a loose inspiration set. The exact canonical file in `visual-reference-map.md` is the page target.

## High-Fidelity Translation Contract / 高保真转译合同

High fidelity MUST include:

- page hierarchy
- visual center of gravity
- spacing rhythm
- typography hierarchy
- card and surface composition
- action priority
- navigation placement
- scroll behavior
- safe-area behavior
- visual density
- paper/vermilion emotional tone

M1 MUST NOT be implemented as:

- color-only refactor
- token-only refactor
- old V1 structure skinning
- component-library-first implementation
- Agent-designed alternative style
- generic dashboard, content feed, or unrelated settings redesign

When current frontend page structure, hierarchy, cards, spacing, navigation, or scroll behavior conflicts with finalized HTML, the finalized HTML MUST win.

## Canonical Reference Workflow / 定稿参考流程

Before modifying any page, the implementation Agent MUST:

1. Open `visual-reference-map.md`.
2. Open the target page exact canonical HTML.
3. Identify page sections, visual hierarchy, main action, typography levels, spacing rhythm, surface style, navigation behavior, and safe-area assumptions.
4. Compare the current frontend page against the canonical HTML.
5. Record which old structures MUST be replaced rather than restyled.
6. Restore the page locally and stabilize it visually.
7. Extract tokens / shared components only after page-level restoration is stable.

The Agent MUST NOT begin M1 by editing global tokens and applying them to the old page skeleton.

## Primary Page High-Fidelity Contracts / 一级页面高保真合同

### Home High-Fidelity Contract / 首页高保真合同

Home MUST follow `Docs/design/home-v2/首页.html`.

Home MUST preserve:

- quiet paper-like full-page background
- Mini Program safe-area / top chrome awareness
- centered product identity `时光回序`
- hero question `今天的你，想留下些什么？`
- supporting copy under the hero question
- dominant writing card / writing action area
- `写下此刻` as the primary action concept, even when the HTML uses a poetic CTA label such as `提笔书写`
- calm bottom archive / arrival / stat summary
- bottom navigation with no overlap

Home FAILS M1 review if:

- old homepage structure remains and only colors are changed
- writing action is visually weak
- page looks like a generic dashboard
- bottom nav overlaps statistics, arrival content, actions, or scroll content

### Timeline High-Fidelity Contract / 时光轴高保真合同

Timeline MUST follow `Docs/design/home-v2/时光回序_时光轴页面.html`.

Timeline MUST preserve:

- top safe area
- search / filter affordance placement
- title `时间长廊`
- explanatory copy and decorative line when present
- month/date-based grouping or chronological grouping from the canonical HTML
- compact record cards
- timestamp / status / title hierarchy
- light vertical time marker rhythm
- bottom helper / empty text if present
- bottom navigation safe padding

Timeline FAILS M1 review if:

- it keeps an older large letter-card / folded-paper timeline when canonical HTML uses compact grouped timeline cards
- it becomes a dense feed or dashboard
- timestamp, status, and title hierarchy does not match the canonical rhythm
- final timeline item or helper text is covered by bottom navigation

### User Center High-Fidelity Contract / 个人中心高保真合同

User Center MUST follow `Docs/design/home-v2/个人主页.html` together with `Docs/design/home-v2/个人中心.png`.

If the HTML and screenshot differ, the Agent MUST stop and report `NEEDS_OWNER_CONFIRMATION` instead of choosing one silently.

When the canonical screenshot or HTML shows the `PERSONAL CONTROL` layout, that layout is canonical. Keeping an older avatar-centered profile layout MUST fail M1 review.

User Center MUST preserve:

- finalized top identity structure from the canonical HTML
- `PERSONAL CONTROL` style if present in the confirmed canonical HTML
- username and tagline hierarchy
- two statistic cards
- grouped settings sections
- quiet icon containers and row dividers
- about/version section
- bottom navigation safe spacing

For the current confirmed canonical file, the profile identity area in `个人主页.html` is canonical. If a later owner-confirmed canonical file uses a newer `PERSONAL CONTROL` layout, keeping an older avatar-centered profile layout MUST fail.

User Center FAILS M1 review if:

- it keeps an old profile/settings structure when it conflicts with the confirmed canonical HTML
- it merely recolors the old settings page
- grouped settings and statistics do not match the canonical visual hierarchy
- bottom navigation collides with the about/version section

## Bottom Navigation And Scroll Safety / 底部导航与滚动安全

Primary tab pages MUST reserve enough bottom space for bottom navigation and device safe area.

- Last visible content block MUST remain fully readable when scrolled to the bottom.
- Bottom navigation MUST NOT overlap text, cards, statistics, helper copy, actions, or settings rows.
- WeChat Mini Program safe area and iOS home indicator MUST be considered.
- Floating or translucent bottom navigation MUST be tested against long content states.
- Bottom nav labels SHOULD preserve the canonical quiet typography and selected-state vermilion dot.

A primary page FAILS M1 review if the bottom navigation covers the final record, statistic area, about/version section, helper text, or primary action.

## Visual Fidelity Tolerance / 视觉还原偏差原则

Small Mini Program rendering differences are allowed when caused by safe-area APIs, platform font fallback, or unsupported browser CSS features.

Tolerance does not allow structural drift:

- page hierarchy MUST match
- action priority MUST match
- typography levels MUST be visibly equivalent
- spacing rhythm MUST preserve the same visual density
- colors MUST derive from canonical HTML / design spec
- card radius, paper texture, vermilion accents, thin dividers, and surface softness MUST preserve the same language
- large structural differences are not allowed

Any intentional deviation MUST be documented in the visual comparison evidence.

## Token And Shared Component Extraction / Token 与共享组件沉淀

Tokens and shared components SHOULD be extracted after Home、Timeline、User Center are page-level stable.

Tokens MAY cover paper background, ink text, vermilion accents, spacing, typography, radius, shadow, status presentation, and safe-area spacing. They MUST be derived from restored pages and design spec, not invented before page restoration.

Shared shell and navigation MAY be extracted only after the primary page behavior proves stable across scroll states.

## Secondary Page Alignment / 二级页面对齐

Secondary pages SHOULD align with the paper/vermilion system after primary pages are stable:

- 我的记录
- 新建记录
- 时间回看 / record detail
- settings-style user-center pages

Secondary pages MUST remain secondary. They MUST NOT expand M1 scope enough to block primary page high-fidelity acceptance.

## Constraints / 约束

- MUST NOT modify backend、database、schema, or production infrastructure.
- MUST NOT modify record status business rules.
- MUST NOT add first-level tabs beyond 首页、时光轴、个人中心.
- MUST NOT add admin portal behavior.
- MUST NOT use H5/Web as the V2.0 acceptance target.
- MUST NOT implement WeChat subscription messages in M1.
- MUST NOT modify AI capability.
- MUST NOT implement real MAP / IMAGE / VOICE features.
- MUST remain compatible with WeChat Mini Program limitations.

## Verification Strategy / 验证策略

Later implementation SHOULD verify:

- Home、Timeline、User Center visual comparison against canonical HTML / screenshot.
- Bottom navigation no-overlap when scrolled to the bottom.
- WeChat Mini Program build when feasible.
- Diff scope excludes backend、database、admin、deployment、monitoring、business-rule、AI、subscription-message, and real multimedia implementation files.

Skipped verification MUST include a reason.
