# Frontend Visual Foundation Spec / 前端视觉基础规格

## Purpose / 目的

在 M1 实施开始前，定义 V2.0 frontend visual foundation 的已接受基线。

## Requirements

### Requirement: Visual Foundation Must Support The V2.0 Core Expression / 视觉基础 MUST 支撑 V2.0 核心表达

frontend visual foundation MUST 支撑“写下此刻”的表达，以及安静、私密、克制的产品感受。

#### Scenario: User enters a core page / 用户进入核心页面

- **WHEN** 用户进入 home、editor、records、timeline 或 review
- **THEN** 视觉系统强化书写、保存、等待、抵达与回看
- **AND** 避免吵闹的 gamified、social-feed 或 dashboard-heavy presentation

### Requirement: M1 Must Use The Paper And Vermilion Visual Direction / M1 MUST 使用宣纸朱砂视觉方向

M1 MUST 使用 `Docs/design/home-v2/时光回序_设计规范.md` 中的宣纸朱砂 visual direction。

#### Scenario: M1 visual direction is reviewed / M1 视觉方向被审查

- **WHEN** 审查 M1 UI 与 tokens
- **THEN** 它们遵循 paper-like background、ink text、克制的 serif-led feeling 与 vermilion accent direction
- **AND** 旧的 blue-gray V1 system 只作为历史实现上下文，而不是目标风格

### Requirement: M1 Must Follow Canonical HTML And Design Spec / M1 MUST 以定稿 HTML 和设计规范为准

M1 MUST use `visual-reference-map.md` to identify the exact canonical HTML file for each primary page, and MUST use `Docs/design/home-v2/时光回序_设计规范.md` as the shared visual language reference.

#### Scenario: Agent prepares visual changes / Agent 准备视觉修改

- **WHEN** Agent 修改 M1 页面视觉、布局或组件气质
- **THEN** it opens `visual-reference-map.md`
- **AND** it opens the exact canonical HTML file for the target page
- **AND** it uses `Docs/design/home-v2/时光回序_设计规范.md` only as the shared visual system reference
- **AND** it MUST NOT treat `Docs/design/home-v2/*.html` as a loose inspiration set
- **AND** it MUST NOT 自行重新设计视觉风格、布局语言或组件气质

### Requirement: M1 Must Stay Frontend Visual Only / M1 MUST 保持为前端视觉范围

M1 MUST 只修改 frontend visual foundation 与相关 demo-safe presentation behavior。

#### Scenario: M1 implementation is reviewed / M1 实现被审查

- **WHEN** 检查 M1 changes
- **THEN** 不存在 backend、database、subscription-message implementation、deployment、monitoring、admin portal 或 AI capability changes
- **AND** record state business rules 未被修改

### Requirement: Visual Tokens Must Be Centralized / Visual Tokens MUST 集中管理

M1 implementation MUST 建立或更新集中化 visual token layer，用于 colors、typography、spacing、radius、shadows 与 status presentation。

#### Scenario: A page uses shared styling / 页面使用共享样式

- **WHEN** 页面需要通用视觉值
- **THEN** 它使用 shared token layer，而不是引入无关的一次性值

### Requirement: M1 Must Stabilize Locally Before Extracting Components / M1 MUST 局部稳定后再抽组件

M1 MUST 先完成局部页面视觉还原并稳定，再抽取 tokens / shared components。

#### Scenario: Agent plans component extraction / Agent 计划组件抽取

- **WHEN** Agent 准备抽取 tokens、page shell 或 shared components
- **THEN** 对应视觉结构已经在局部页面中按定稿 HTML 和设计规范稳定
- **AND** 抽象只来自已经稳定复用的结构
- **AND** Agent MUST NOT 一开始大规模抽象，或先搭组件库再强行套页面

### Requirement: M1 May Adjust Microcopy For Visual Coherence / M1 MAY 调整微文案以保持视觉一致

当调整可见 microcopy 有助于 V2.0 表达与命名时，M1 MAY 进行调整。

#### Scenario: M1 updates page presentation / M1 更新页面呈现

- **WHEN** 页面标题、动作标签、状态标签、空状态或 demo hints 与 V2.0 命名或语气冲突
- **THEN** M1 可以调整它们
- **AND** 调整不引入新的 business behavior

### Requirement: Page Shell And Navigation Must Stay Consistent / Page Shell 与 Navigation MUST 保持一致

M1 implementation MUST 保留 mini program navigation model，同时让 page shell、safe area、top bar 与 bottom navigation 在视觉上保持一致。

#### Scenario: Primary tab page renders / 一级 Tab 页面渲染

- **WHEN** 显示 homepage、timeline 或 user center
- **THEN** primary bottom navigation 保持可用且一致

#### Scenario: Secondary page renders / 二级页面渲染

- **WHEN** 显示 record editor、record list、detail/review 或 settings-style pages
- **THEN** 页面使用正确的 secondary navigation behavior
- **AND** 不引入新的 first-level tab

### Requirement: Page Shell And Navigation Must Prevent Content Overlap / Page Shell 与导航 MUST 防止内容遮挡

Primary tab pages MUST reserve enough bottom space for bottom navigation and device safe area.

#### Scenario: Primary tab page renders and scrolls / 一级 Tab 页面渲染并滚动

- **WHEN** Home、Timeline、or User Center renders
- **THEN** primary bottom navigation remains available and visually consistent
- **AND** scrollable content reserves enough bottom space for the navigation and device safe area
- **AND** bottom navigation MUST NOT overlap text, cards, actions, helper copy, statistics, settings rows, or about/version sections
- **AND** the final content block remains fully readable when the page is scrolled to the bottom

### Requirement: M1 Must Include Visual Comparison Evidence / M1 MUST 包含视觉对照证据

M1 completion MUST include page-level visual review evidence for Home、Timeline、and User Center.

#### Scenario: M1 is submitted for review / M1 提交审查

- **WHEN** M1 is marked complete
- **THEN** the review MUST include implemented Mini Program screenshots or equivalent visual evidence for Home、Timeline、and User Center
- **AND** each screenshot MUST be compared against the canonical HTML and/or final screenshot reference
- **AND** remaining visual differences MUST be listed
- **AND** any intentional deviation MUST be justified as a WeChat Mini Program constraint or documented product decision

### Requirement: M1 Must Be High-Fidelity Translation, Not Skinning / M1 MUST 是高保真转译而不是换肤

M1 MUST translate the finalized HTML pages listed in `visual-reference-map.md` into WeChat Mini Program pages with high fidelity.

#### Scenario: Existing frontend layout conflicts with finalized HTML / 现有前端结构与定稿 HTML 冲突

- **WHEN** the existing frontend page hierarchy, spacing, card structure, action priority, or navigation placement differs from the finalized HTML
- **THEN** the implementation MUST change the frontend page structure to match the finalized HTML
- **AND** it MUST NOT keep the old structure merely by changing colors, fonts, shadows, or tokens

#### Scenario: Agent applies only theme changes / Agent 只做主题替换

- **WHEN** M1 changes only colors, typography tokens, radius, shadows, or minor component styling
- **AND** the page composition still differs from the finalized HTML
- **THEN** M1 MUST be considered incomplete

### Requirement: Canonical HTML References Must Be Explicit / 定稿 HTML 引用 MUST 明确

M1 MUST use `visual-reference-map.md` to identify exact canonical HTML files for each primary page.

#### Scenario: Agent prepares to modify a primary page / Agent 准备修改一级页面

- **WHEN** Agent modifies Home、Timeline、or User Center
- **THEN** the exact canonical HTML file for that page MUST be listed in `visual-reference-map.md`
- **AND** a generic `Docs/design/home-v2/*.html` reference is insufficient for implementation
- **AND** historical or alternate drafts MUST NOT be used as the target unless explicitly marked canonical

### Requirement: Home Must Match Finalized Home Reference / 首页 MUST 匹配定稿首页

Home MUST match the finalized Home HTML listed in `visual-reference-map.md`.

#### Scenario: Home page is reviewed / 审查首页

- **WHEN** Home renders in WeChat Mini Program
- **THEN** the page hierarchy, hero question, supporting copy, primary writing card, action priority, bottom statistics/archive area, background, typography hierarchy, and bottom navigation behavior match the finalized Home reference
- **AND** `写下此刻` remains the strongest action concept
- **AND** bottom navigation does not overlap the statistics area or scroll content

#### Scenario: Home keeps old structure / 首页保留旧结构

- **WHEN** Home still uses the old page skeleton while only changing colors or decorative styles
- **THEN** Home MUST fail M1 visual review

### Requirement: Timeline Must Match Finalized Timeline Reference / 时光轴 MUST 匹配定稿时光轴

Timeline MUST match the finalized Timeline HTML listed in `visual-reference-map.md`.

#### Scenario: Timeline page is reviewed / 审查时光轴

- **WHEN** Timeline renders in WeChat Mini Program
- **THEN** the title area, search/filter affordance, month grouping, vertical time rhythm, record card density, timestamp/status/title hierarchy, bottom helper text, and bottom navigation behavior match the finalized Timeline reference
- **AND** it remains a quiet time corridor rather than a feed, dashboard, or old folded-letter layout

#### Scenario: Timeline bottom content is scrolled / 时光轴滚动到底部

- **WHEN** the user scrolls Timeline to the bottom
- **THEN** the final record card and any bottom helper text remain fully readable
- **AND** bottom navigation does not cover or compete with them

### Requirement: User Center Must Match Finalized User Center Reference / 个人中心 MUST 匹配定稿个人中心

User Center MUST match the finalized User Center HTML listed in `visual-reference-map.md`.

#### Scenario: User Center page is reviewed / 审查个人中心

- **WHEN** User Center renders in WeChat Mini Program
- **THEN** the top identity area, personal-control title treatment, username/tagline hierarchy, statistic cards, grouped settings sections, about/version row, icon treatment, spacing rhythm, and bottom navigation behavior match the finalized User Center reference

#### Scenario: User Center keeps old profile layout / 个人中心保留旧个人页结构

- **WHEN** User Center keeps the old avatar-centered profile/paper-card structure while only changing colors or card styling
- **THEN** User Center MUST fail M1 visual review

### Requirement: M1 Must Be Demonstrable In WeChat Mini Program / M1 MUST 可在 WeChat Mini Program 中演示

M1 implementation MUST 能够在 WeChat Mini Program target 中验证。

#### Scenario: M1 is completed / M1 完成

- **WHEN** 实现完成
- **THEN** 在可行时执行 mini program build 或等价本地验证
- **AND** 任何未验证约束都需要明确报告
