# Mini Program Core Spec / 小程序核心规格

## Purpose / 目的

定义《时光回序》用户侧 mini program 的稳定核心，使后续 V2.0 工作能够保持产品闭环与页面职责不偏移。

## Requirements

### Requirement: Product Must Center On Writing The Present / 产品 MUST 围绕“写下此刻”

mini program MUST 将“写下此刻”呈现为主要产品动作与情绪中心。

#### Scenario: User opens the home page / 用户打开首页

- **WHEN** 用户进入主体验
- **THEN** 页面将“写下当下”呈现为最清晰的可用动作
- **AND** 文案与布局避免把产品表达成对未来的焦虑

### Requirement: Core Loop Must Remain Complete / 核心闭环 MUST 保持完整

mini program MUST 支持从书写到时间回看的可演示闭环。

#### Scenario: Demonstration flow / 演示流程

- **WHEN** 演示沿着 V2.0 主路径进行
- **THEN** 它可以经过首页、新建记录、保存此刻 / 交给时间、我的记录或时光轴、抵达记录、时间回看，以及可选回信

### Requirement: V2.0 Naming Must Be Consistent / V2.0 命名 MUST 保持一致

面向用户的 V2.0 mini program MUST 在相关界面使用“我的记录”“时光轴”“时间回看”这些名称。

#### Scenario: Agent updates visible copy / Agent 更新可见文案

- **WHEN** 可见导航、标题、按钮或空状态涉及记录、时间线或回看
- **THEN** 文案使用“我的记录”“时光轴”“时间回看”
- **AND** 除非后续 spec 重新启用，否则“我的档案”“时间轴”“解锁页”“回看页”等旧名称视为历史命名

### Requirement: Record States Must Stay Legible / 记录状态 MUST 保持可读

用户侧体验 MUST 区分草稿、已封存、已解锁 / 已抵达记录。

#### Scenario: User browses records / 用户浏览记录

- **WHEN** 页面展示不同状态的记录
- **THEN** 每种状态在视觉与文字上都可理解
- **AND** 已封存记录不能表现得像草稿一样可编辑

### Requirement: Optional Reply Must Stay Optional / 回信 MUST 保持可选

时间回看后的回信 MUST 保持为可选动作，而不是强制完成步骤。

#### Scenario: User reviews an arrived record / 用户回看已抵达记录

- **WHEN** 用户进入时间回看页面
- **THEN** 用户可以理解“那时的我 / 现在的我”
- **AND** 用户可以不被强制写回信而离开

### Requirement: Page Responsibilities Must Stay Separated / 页面职责 MUST 保持分离

mini program 主要页面 MUST 保持清晰且互不混淆的职责。

#### Scenario: Agent changes a page / Agent 修改页面

- **WHEN** Agent 修改 homepage、record editor、record list、timeline、detail/review 或 user center
- **THEN** 页面仍然与其在核心闭环中的角色保持一致
- **AND** 页面不能被改造成无关的 admin、analytics 或通用 content-feed surface

### Requirement: One-Click Preview May Support The Demo Loop / 一键预览 MAY 支持演示闭环

V2.0 mini program MAY 提供 no-login preview path 用于演示。

#### Scenario: User enters preview mode / 用户进入预览模式

- **WHEN** 用户进入 one-click preview / no-login demo path
- **THEN** mini program 可以使用 preview data 演示核心闭环
- **AND** preview path 不表示生产认证被绕过
