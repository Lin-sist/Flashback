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
