# Frontend Visual Foundation Delta / 前端视觉基础变更

## ADDED Requirements

### Requirement: M1 Must Use The Confirmed Paper And Vermilion Direction / M1 MUST 使用已确认的宣纸朱砂方向

在 page-specific polish 被视为完成前，M1 MUST 在 mini program 中一致应用已确认的 宣纸朱砂 visual direction。

#### Scenario: Visual direction is reviewed / 视觉方向被审查

- **WHEN** 审查者检查 M1 work
- **THEN** UI 呈现为一个一致的产品方向
- **AND** 旧的 V1 blue-gray system 只作为历史代码库上下文，而不是目标风格

### Requirement: M1 Must Follow Finalized HTML And Design Spec / M1 MUST 以定稿 HTML 和设计规范为准

M1 MUST 以 `Docs/design/home-v2/*.html` 定稿页面和 `Docs/design/home-v2/时光回序_设计规范.md` 作为视觉实现依据。

#### Scenario: Agent changes visual presentation / Agent 修改视觉呈现

- **WHEN** Agent 修改 M1 页面视觉、布局语言或组件气质
- **THEN** 变更可以追溯到 `Docs/design/home-v2/*.html` 定稿页面或 `Docs/design/home-v2/时光回序_设计规范.md`
- **AND** Agent MUST NOT 自行重新设计视觉方向、布局语言或组件气质

### Requirement: M1 Must Centralize Visual Tokens Before Broad Page Polish / 大范围页面 polish 前 M1 MUST 集中 visual tokens

在接受大范围 page-level restyling 前，M1 MUST 为重复出现的视觉值定义 shared visual tokens。

#### Scenario: Page style uses common values / 页面样式使用通用值

- **WHEN** 页面需要 colors、typography、spacing、radius、shadows 或 status styling
- **THEN** 它使用 shared tokens 或有明确理由的 local values
- **AND** 重复值不会在页面之间逐页复制

### Requirement: M1 Must Stabilize Local UI Before Extracting Components / M1 MUST 局部稳定后再抽组件

M1 MUST 先完成局部页面或局部区域的视觉还原与稳定，再抽取 tokens / shared components。

#### Scenario: Agent extracts tokens or components / Agent 抽取 tokens 或组件

- **WHEN** Agent 准备抽取 tokens、page shell 或 shared components
- **THEN** 对应 UI 已经按定稿 HTML 与设计规范在局部页面中稳定
- **AND** 抽象来自已经稳定复用的结构
- **AND** Agent MUST NOT 一开始大规模抽象，或先搭组件库再强行套页面

### Requirement: M1 Must Preserve Mini Program Navigation Boundaries / M1 MUST 保留小程序导航边界

M1 MUST 保留现有 primary/secondary page navigation model。

#### Scenario: Primary navigation is inspected / 一级导航被检查

- **WHEN** 检查 mini program primary tabs
- **THEN** 只有 首页、时光轴、个人中心 是 first-level tabs

#### Scenario: Secondary pages are inspected / 二级页面被检查

- **WHEN** 检查 record editor、record list、detail/review 或 settings pages
- **THEN** 它们不会作为新的 first-level tabs 出现
- **AND** 它们使用合适的 secondary navigation patterns

### Requirement: M1 Must Use V2.0 Visible Naming / M1 MUST 使用 V2.0 可见命名

M1 MUST 在 visible UI copy 中使用 我的记录、时光轴、时间回看。

#### Scenario: Visible copy is reviewed / 可见文案被审查

- **WHEN** 审查 page titles、navigation labels、action labels、empty states 或 demo hints
- **THEN** 在适用处使用 V2.0 名称：我的记录、时光轴、时间回看
- **AND** 旧名称只在 internal route 或 code identifiers 中保留

### Requirement: M1 May Adjust Microcopy / M1 MAY 调整微文案

M1 MAY 调整 microcopy，使其与 V2.0 naming and tone 对齐。

#### Scenario: Existing copy conflicts with V2.0 tone / 现有文案与 V2.0 语气冲突

- **WHEN** 现有页面文案听起来像 V1、admin tooling、generic management 或旧命名
- **THEN** M1 可以更新文案
- **AND** 更新不改变 business logic

### Requirement: M1 Must Make Record Statuses Visually Legible / M1 MUST 让记录状态视觉可读

M1 MUST 在不改变 status business rules 的前提下，让 draft、sealed 与 unlocked/arrived records 在视觉上可区分。

#### Scenario: Mixed record statuses are displayed / 混合状态记录被展示

- **WHEN** list、timeline、home summary 或 detail view 展示记录
- **THEN** draft、sealed 与 unlocked/arrived states 可以一眼理解
- **AND** sealed records 不会被表现成 editable drafts

### Requirement: M1 Must Keep The Review Reply Optional / M1 MUST 保持回看后的回信可选

M1 MUST 将时间回看后的 reply 呈现为可选。

#### Scenario: User reaches a review page / 用户到达回看页面

- **WHEN** 用户阅读 arrived/unlocked record
- **THEN** 页面支持理解过去与现在的对照
- **AND** 在状态允许时，reply 只作为可选后续动作提供

### Requirement: M1 Must Not Introduce Non-Frontend Scope / M1 MUST NOT 引入非前端范围

M1 MUST NOT 引入 backend、database、admin、subscription-message implementation、AI capability、deployment、monitoring、真实 MAP / IMAGE / VOICE features 或 record-state business-rule changes。

#### Scenario: M1 diff is reviewed / M1 diff 被审查

- **WHEN** 检查 changed files
- **THEN** 只修改 frontend visual foundation 与 OpenSpec-related files
- **AND** 除非后续 OpenSpec change 明确批准，否则任何 non-frontend change 都视为 scope drift

### Requirement: M1 Must Use Existing Preview As The Initial Demo Path / M1 MUST 使用现有 preview 作为初始演示路径

M1 MUST 将 `frontend/src/features/preview` 视为初始 V2.0 demo data path。

#### Scenario: No-login demo is reviewed / 免登录演示被审查

- **WHEN** 使用 one-click preview / no-login demo mode
- **THEN** 除非后续 OpenSpec change 替换，否则它依赖现有 preview mechanism
- **AND** 它与 production authentication readiness 保持清晰区分

### Requirement: M1 Must Be Verifiable Against The WeChat Mini Program Target / M1 MUST 面向 WeChat Mini Program target 可验证

M1 MUST 包含面向 mini program 的 verification。

#### Scenario: M1 is ready for review / M1 准备审查

- **WHEN** 实现者报告完成
- **THEN** 在可行时包含 type check 与 mini program build/smoke-check results
- **AND** 任何跳过的检查都列出具体原因
