# Frontend Visual Foundation Delta / 前端视觉基础变更

## ADDED Requirements

### Requirement: M1 Must Be High-Fidelity Translation, Not Skinning / M1 MUST 是高保真转译而不是换肤

M1 MUST translate finalized HTML into WeChat Mini Program pages with high fidelity. M1 MUST NOT pass as color-only, typography-only, radius-only, shadow-only, token-only, or old-structure skinning work.

#### Scenario: Skin-only work is reviewed / 审查只换肤的实现

- **WHEN** M1 work changes colors, typography tokens, radius, or shadows while the page composition remains materially different from canonical HTML
- **THEN** the work is marked incomplete
- **AND** the page MUST be rebuilt to match canonical hierarchy, spacing, action priority, and visual density

#### Scenario: Existing frontend conflicts with canonical HTML / 现有前端与定稿 HTML 冲突

- **WHEN** current frontend structure, card layout, spacing, navigation, or scroll behavior conflicts with the canonical HTML
- **THEN** the canonical HTML MUST win
- **AND** existing frontend may only be reused for routing, data shape, state display, or business-safe behavior

### Requirement: Canonical HTML References Must Be Explicit / 定稿 HTML 引用 MUST 明确

M1 MUST use exact canonical HTML files listed in `visual-reference-map.md`. The broad glob `Docs/design/home-v2/*.html` is insufficient as an implementation instruction.

#### Scenario: Agent starts page work / Agent 开始页面工作

- **WHEN** an Agent begins work on Home、Timeline、or User Center
- **THEN** it opens `visual-reference-map.md` and the exact canonical HTML for that page
- **AND** it uses the matching screenshot and design spec as supporting references

#### Scenario: Canonical file is ambiguous / 定稿文件不明确

- **WHEN** multiple plausible HTML references exist for the same page
- **THEN** `visual-reference-map.md` MUST mark the page as `NEEDS_OWNER_CONFIRMATION`
- **AND** implementation MUST NOT invent a canonical choice without owner confirmation

### Requirement: Home Must Match Finalized Home Reference / 首页 MUST 匹配定稿首页

Home MUST match `Docs/design/home-v2/首页.html` at page hierarchy, visual center, typography hierarchy, spacing rhythm, writing action priority, paper/vermilion tone, and bottom navigation behavior.

#### Scenario: Home is reviewed / 审查首页

- **WHEN** the Home page is compared with the finalized Home HTML and screenshot
- **THEN** it presents centered `时光回序`, the hero question `今天的你，想留下些什么？`, supporting copy, dominant writing action, calm paper surface, and bottom arrival/stat summary
- **AND** it preserves the canonical hierarchy and visual density

#### Scenario: Home keeps old structure / 首页保留旧结构

- **WHEN** Home keeps the old homepage structure and only changes colors or tokens
- **THEN** M1 Home is marked failed
- **AND** the old skeleton MUST be replaced where it conflicts with the finalized Home HTML

### Requirement: Timeline Must Match Finalized Timeline Reference / 时光轴 MUST 匹配定稿时光轴

Timeline MUST match `Docs/design/home-v2/时光回序_时光轴页面.html` at top safe area, search/filter placement, `时间长廊` title treatment, chronological grouping, compact record cards, timestamp/status/title hierarchy, vertical marker rhythm, and bottom helper text.

#### Scenario: Timeline is reviewed / 审查时光轴

- **WHEN** the Timeline page is compared with the finalized Timeline HTML and screenshot
- **THEN** it presents the canonical topbar, `时间长廊`, compact chronological record cards, status hierarchy, and light vertical time marker rhythm
- **AND** final timeline content remains readable above bottom navigation

#### Scenario: Timeline keeps old large letter-card structure / 时光轴保留旧大型信件卡

- **WHEN** Timeline keeps an older large letter-card or folded-paper timeline structure while canonical HTML uses compact grouped timeline cards
- **THEN** M1 Timeline is marked failed
- **AND** the timeline MUST be restored to the canonical compact grouped rhythm

### Requirement: User Center Must Match Finalized User Center Reference / 个人中心 MUST 匹配定稿个人中心

User Center MUST match the canonical User Center file listed in `visual-reference-map.md` at identity structure, username/tagline hierarchy, statistics, settings groups, icon containers, row dividers, about/version section, and bottom navigation spacing.

#### Scenario: User Center is reviewed / 审查个人中心

- **WHEN** the User Center page is compared with `Docs/design/home-v2/个人主页.html` and `Docs/design/home-v2/个人中心.png`
- **THEN** it presents the finalized identity structure, two statistic cards, grouped settings sections, quiet icon containers, and about/version section
- **AND** it preserves canonical spacing and bottom navigation safety

#### Scenario: User Center keeps conflicting old profile layout / 个人中心保留冲突旧布局

- **WHEN** User Center keeps an old avatar-centered profile layout while the owner-confirmed canonical HTML uses a newer personal-control layout
- **THEN** M1 User Center is marked failed
- **AND** the page MUST follow the owner-confirmed canonical file instead of the old profile skeleton

#### Scenario: User Center is merely recolored / 个人中心只是重配色

- **WHEN** User Center settings, statistics, and about/version structure are merely recolored but remain materially different from canonical HTML
- **THEN** M1 User Center is marked failed
- **AND** the page MUST be restored structurally before tokens are extracted

### Requirement: Page Shell And Navigation Must Prevent Content Overlap / 页面壳层与导航 MUST 防止内容遮挡

Primary tab pages MUST reserve enough bottom space for bottom navigation and device safe area. Bottom navigation MUST NOT overlap content.

#### Scenario: Primary page scrolls to bottom / 一级页面滚动到底部

- **WHEN** Home、Timeline、or User Center is scrolled to the bottom on a WeChat Mini Program target or equivalent visual check
- **THEN** the final visible content block remains fully readable
- **AND** bottom navigation does not cover records, statistics, helper text, actions, settings rows, or about/version content

#### Scenario: Bottom navigation overlaps content / 底部导航遮挡内容

- **WHEN** bottom navigation covers the final record, statistics area, about section, helper copy, action, or settings row
- **THEN** M1 is marked failed
- **AND** the page shell MUST add safe-area-aware bottom reserve spacing and retest long content states

### Requirement: M1 Must Include Visual Comparison Evidence / M1 MUST 包含视觉对照证据

M1 completion MUST include visual comparison evidence for Home、Timeline、and User Center against canonical HTML and/or screenshot references.

#### Scenario: M1 is reported complete / M1 报告完成

- **WHEN** implementation claims M1 primary pages are complete
- **THEN** Home、Timeline、and User Center each include visual comparison evidence
- **AND** missing evidence makes M1 incomplete even if code has changed

#### Scenario: Rendering differences remain / 存在渲染差异

- **WHEN** Mini Program rendering differs from the finalized HTML because of platform limitations, font fallback, or safe-area behavior
- **THEN** the difference MUST be documented in the visual review evidence
- **AND** the difference MUST NOT be a structural change in page hierarchy, action priority, or visual density

### Requirement: Tokens And Shared Components Must Follow Stable Pages / Token 与共享组件 MUST 后于稳定页面

M1 MUST restore Home、Timeline、and User Center at page level before extracting broad visual tokens or shared components.

#### Scenario: Agent extracts tokens / Agent 抽取 token

- **WHEN** an Agent prepares to define shared tokens, page shell, bottom navigation, or reusable visual components
- **THEN** the relevant primary page UI has already been restored and visually stabilized against canonical HTML
- **AND** component-library-first implementation or token-first application to old pages is incomplete

### Requirement: M1 Must Preserve Scope Boundaries / M1 MUST 保持范围边界

M1 MUST NOT introduce backend、database、schema、admin、deployment、monitoring、AI、subscription-message、business-rule, or real MAP / IMAGE / VOICE implementation changes.

#### Scenario: M1 diff is reviewed / 审查 M1 diff

- **WHEN** changed files are reviewed
- **THEN** changes are limited to frontend visual work and OpenSpec documentation allowed by the change
- **AND** any backend、database、schema、admin、deployment、monitoring、AI、subscription-message、business-rule, or real multimedia implementation change is scope drift unless a later OpenSpec change explicitly approves it

### Requirement: M1 Must Preserve Mini Program Navigation Boundaries / M1 MUST 保留小程序导航边界

M1 MUST keep only 首页、时光轴、个人中心 as primary tab pages, with 我的记录、新建记录、时间回看, and settings-style pages remaining secondary.

#### Scenario: Primary navigation is inspected / 检查一级导航

- **WHEN** Mini Program primary tabs are inspected
- **THEN** only 首页、时光轴、个人中心 appear as first-level tabs
- **AND** secondary pages are not promoted to primary tabs

### Requirement: M1 Must Use V2.0 Visible Naming / M1 MUST 使用 V2.0 可见命名

M1 MUST use V2.0 visible naming where applicable: 我的记录、时光轴、时间回看.

#### Scenario: Visible copy is reviewed / 审查可见文案

- **WHEN** page titles, navigation labels, action labels, empty states, or demo hints are reviewed
- **THEN** applicable copy uses 我的记录、时光轴、时间回看
- **AND** old names may remain only in internal route or code identifiers when changing them would be outside M1
