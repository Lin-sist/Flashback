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

### Requirement: M1 Must Follow Finalized HTML And Design Spec / M1 MUST 以定稿 HTML 和设计规范为准

M1 MUST 以 `Docs/design/home-v2/*.html` 定稿页面和 `Docs/design/home-v2/时光回序_设计规范.md` 作为视觉实现依据。

#### Scenario: Agent prepares visual changes / Agent 准备视觉修改

- **WHEN** Agent 修改 M1 页面视觉、布局或组件气质
- **THEN** 它依据 `Docs/design/home-v2/*.html` 和 `Docs/design/home-v2/时光回序_设计规范.md`
- **AND** 它 MUST NOT 自行重新设计视觉风格、布局语言或组件气质

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

### Requirement: M1 Must Be Demonstrable In WeChat Mini Program / M1 MUST 可在 WeChat Mini Program 中演示

M1 implementation MUST 能够在 WeChat Mini Program target 中验证。

#### Scenario: M1 is completed / M1 完成

- **WHEN** 实现完成
- **THEN** 在可行时执行 mini program build 或等价本地验证
- **AND** 任何未验证约束都需要明确报告
