# V2 Product Scope Spec / V2 产品范围规格

## Purpose / 目的

定义 V2.0 是什么、不是什么，避免执行 Agent 将 V3.0 或生产上线职责拉入演示版重构。

## Requirements

### Requirement: V2.0 Must Be Treated As A Demo Version / V2.0 MUST 被视为演示版本

V2.0 MUST 按照“写下此刻 · 时间回看演示版”进行规划。

#### Scenario: Agent proposes work / Agent 提出工作

- **WHEN** Agent 提出或实现 V2.0 工作
- **THEN** 工作应优先服务于连贯、可演示的用户侧体验
- **AND** 完成标准不依赖生产上线基础设施

### Requirement: OpenSpec Must Be The Highest Priority For V2.0 / OpenSpec MUST 是 V2.0 最高优先级

当 V2.0 OpenSpec 文档与旧 `Docs/**` 文件冲突时，OpenSpec MUST 覆盖旧文档。

#### Scenario: Old documentation conflicts with OpenSpec / 旧文档与 OpenSpec 冲突

- **WHEN** Agent 发现 OpenSpec 与旧文档冲突
- **THEN** Agent 遵循 OpenSpec
- **AND** 只有在 OpenSpec 缺失或模糊时，才将冲突记录为待确认问题

### Requirement: WeChat Mini Program Is The Acceptance Client / WeChat Mini Program MUST 是验收客户端

除非后续 change 明确扩展范围，否则 V2.0 验收 MUST 聚焦 WeChat Mini Program client。

#### Scenario: Work is verified / 工作被验证

- **WHEN** 检查 V2.0 用户可见变更
- **THEN** 验证目标是 mini program client
- **AND** desktop web 或 admin screens 不能替代 mini program 验收

### Requirement: H5 And Web User Clients Are Historical References / H5 和 Web 用户端 MUST 仅作为历史参考

H5 与 Web 用户侧方案 MUST NOT 被视为 V2.0 验收目标。

#### Scenario: Agent sees an old H5/Web recommendation / Agent 看到旧 H5/Web 建议

- **WHEN** 旧文档建议 H5、Web 或响应式用户端
- **THEN** Agent 将该建议视为历史信息
- **AND** 保持 V2.0 验收聚焦 WeChat Mini Program

### Requirement: Admin And Production Launch Items Must Remain Out Of V2.0 By Default / Admin 与生产上线项默认 MUST 保持在 V2.0 外

Production launch、management/admin portal、deployment hardening 和 online monitoring 默认 MUST 保持在 V2.0 范围外。

#### Scenario: Agent encounters V3.0-like work / Agent 遇到类似 V3.0 的工作

- **WHEN** 任务暗示真实 WeChat launch、admin portal、production deployment 或 monitoring
- **THEN** Agent 将其记录为后续阶段事项
- **AND** 除非后续 OpenSpec change 明确纳入，否则不在 V2.0 下实现

### Requirement: WeChat Subscription Messages Belong To V2.0 But Not M1 / WeChat subscription messages 属于 V2.0 但不属于 M1

V2.0 MUST 在后续模块包含 WeChat subscription message 工作，但 M1 MUST NOT 实现它。

#### Scenario: M1 frontend visual work references time delivery / M1 前端视觉引用时间抵达

- **WHEN** M1 使用“交给时间”或“记录抵达”等文案
- **THEN** 它可以为后续 subscription-message 工作准备视觉语言
- **AND** 它不实现 subscription-message authorization、backend delivery 或 production notification behavior

#### Scenario: A later V2.0 notification module is proposed / 后续 V2.0 通知模块被提出

- **WHEN** 后续 OpenSpec change 将 WeChat subscription messages 纳入范围
- **THEN** 该模块可以定义 V2.0 的 frontend authorization、backend delivery 与 fallback rules

### Requirement: V2.0 Can Upgrade Frontend And Backend Across Separate Modules / V2.0 MAY 按模块升级 frontend 与 backend

完整 V2.0 计划 MAY 同时包含 frontend 与 backend 改进，但每个模块 MUST 尊重自身声明的边界。

#### Scenario: M1 frontend visual work is active / M1 前端视觉工作处于 active 状态

- **WHEN** active change 是 M1 frontend visual foundation
- **THEN** backend、database、production launch 与 business-rule changes 保持在范围外

### Requirement: V2.0 May Use No-Login Preview For Demonstration / V2.0 MAY 使用免登录预览演示

V2.0 MAY 支持 one-click preview / no-login demo mode 用于演示。

#### Scenario: Demo mode is used / 使用演示模式

- **WHEN** mini program 作为 V2.0 demo 展示
- **THEN** 它可以使用现有 preview mechanism，而不是要求真实登录
- **AND** demo path 与 production readiness 保持清晰区分

## Accepted From M4 Real Capability Completion

> Accepted and archived on 2026-07-27 from openspec/changes/archive/2026-07-27-m4-real-capability-completion/. Wording below is the accepted M4 delta requirement body.

### Requirement: M4 Must Target Near Production Usability For Core User Functions

M4 SHALL be treated as a real-capability completion milestone for V2.0 Mini Program core functions.

#### Scenario: Agent implements M4 work

- WHEN an Agent starts M4 implementation
- THEN it SHALL prioritize real authenticated user capability over local-only demo behavior
- AND it SHALL keep completion focused on the Mini Program core loop

#### Scenario: Production platform work is proposed

- WHEN work suggests deployment hardening, monitoring, alerting, incident response, admin portal, or settings-page enhancements
- THEN the Agent SHALL defer it outside M4 unless a new OpenSpec change explicitly includes it

### Requirement: M4 May Retain Explicit Preview Mode

M4 MAY retain one-click preview for demonstration, but preview MUST be separated from real user behavior.

#### Scenario: Preview remains available

- WHEN the user enters explicit preview mode
- THEN the Mini Program MAY use preview data to demonstrate the core flow
- AND this SHALL NOT be treated as evidence that the real authenticated path works

#### Scenario: Real mode is used

- WHEN an authenticated user uses M4 core surfaces
- THEN the Mini Program SHALL use real backend-backed data and real integration states
- AND mock success SHALL NOT be accepted as M4 completion

### Requirement: M4 Storage Scope Is Configurable Private Object Storage

M4 SHALL use a provider-neutral backend contract for record media, with Qiniu and S3-compatible object storage as supported implementations.

#### Scenario: Media storage is implemented

- WHEN images, voice files, or covers are implemented
- THEN they SHALL use the configured object-storage provider with private bucket assumptions
- AND backend-controlled upload authorization and private-access-safe media retrieval SHALL be used

#### Scenario: Active storage provider is changed

- WHEN backend configuration selects Qiniu or an S3-compatible provider
- THEN new uploads SHALL switch provider without changing the attachment APIs or Mini Program business flow
- AND provider-specific features unrelated to record attachment storage SHALL remain out of M4 scope

### Requirement: M4 Voice Scope Is Raw Audio Storage Only

M4 SHALL store and play voice files, but SHALL NOT transcribe or semantically analyze voice.

#### Scenario: Voice feature is implemented

- WHEN the user records voice for a record
- THEN the system SHALL save the raw voice file and allow playback
- AND it SHALL NOT require speech-to-text, transcript search, or voice AI analysis for M4 acceptance

### Requirement: M4 Cover Scope Is Attachment-Based

M4 SHALL support record cover selection only from image attachments already associated with the record.

#### Scenario: User wants a cover

- WHEN a cover is added or changed
- THEN the selected cover SHALL come from the record's own image attachments
- AND a standalone cover upload flow SHALL remain outside M4

### Requirement: M4 Must Preserve Core Product Naming And Tone

M4 SHALL keep the V2.0 Mini Program oriented around private writing and time review.

#### Scenario: Agent updates M4 UI or copy

- WHEN visible copy references records, timeline, review, media, location, or AI
- THEN it SHALL remain quiet, private, and user-centered
- AND it SHALL preserve canonical naming such as "我的记录", "时光轴", and "时间回看"
- AND it SHALL NOT turn the product into a dashboard, social feed, content platform, or admin workflow

### Requirement: M4 Timeline Filtering Must Remain Focused

M4 SHALL improve timeline browsing for larger personal record collections through focused filters and incremental loading.

#### Scenario: Timeline filtering scope is implemented

- WHEN timeline filtering is added
- THEN it SHALL support one tag plus created-time year/month/day selection and pagination
- AND it SHALL preserve the timeline's quiet browsing role
- AND multiple-tag boolean search, keyword search, state/type filtering, and persisted filter preferences SHALL remain outside this M4 addition
