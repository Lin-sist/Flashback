# Agent Collaboration Spec / Agent 协作规格

## Purpose / 目的

记录人类与 AI 协作者在 V2.0 规划、规格、执行与审查中的职责分工。

## Requirements

### Requirement: Agents Must Follow Declared Roles / Agent MUST 遵循声明角色

Agent MUST 尊重 `AGENTS.md` 中记录的协作角色。

#### Scenario: Agent receives a V2.0 task / Agent 收到 V2.0 任务

- **WHEN** Agent 收到任务
- **THEN** 它识别任务属于规划、规格、执行、审查、文案还是 UI reference work
- **AND** 在没有用户明确指示时，避免接管其他角色职责

### Requirement: Implementation Must Be Driven By OpenSpec Changes / 实现 MUST 由 OpenSpec changes 驱动

重要的 V2.0 实现 MUST 基于 active OpenSpec change，并具备 proposal、design、tasks 与 spec delta。

#### Scenario: Engineering agent starts work / 工程 Agent 开始工作

- **WHEN** 工程 Agent 开始实现 V2.0 模块
- **THEN** 它读取相关 `openspec/changes/<change-id>/` artifacts
- **AND** 它检查 `openspec/specs` 中已接受的 specs

### Requirement: OpenSpec Must Remain Iterative / OpenSpec MUST 保持可迭代

Agent MUST 将 OpenSpec 视为当前最高优先级事实源，而不是永久冻结的 artifact。

#### Scenario: User confirms a new decision / 用户确认新决策

- **WHEN** 用户确认会改变范围、命名、设计或执行顺序的 V2.0 决策
- **THEN** Agent 在实现继续前更新相关 OpenSpec artifacts
- **AND** 旧文档保持为历史参考，除非被更新或重新确认

### Requirement: Reviews Must Check Scope Drift / 审查 MUST 检查范围漂移

审查 Agent MUST 检查代码或设计变更是否偏离 active OpenSpec scope。

#### Scenario: Codex reviews an implementation / Codex 审查实现

- **WHEN** Codex 审查 V2.0 实现
- **THEN** 它优先报告 scope drift，再讨论风格偏好
- **AND** 它指出非预期的 backend、database、business logic 或 production-launch changes

### Requirement: Inspiration Must Not Override Specs / 灵感材料 MUST NOT 覆盖 specs

Claude、v0 或其他 inspiration/reference outputs MUST NOT 覆盖已接受的 OpenSpec requirements。

#### Scenario: Reference output conflicts with OpenSpec / 参考输出与 OpenSpec 冲突

- **WHEN** 生成的 UI 或文案参考与 OpenSpec 冲突
- **THEN** OpenSpec 仍然是 source of truth
- **AND** 冲突被记录为问题或未来 proposal，而不是被静默实现

## Accepted From C5 agent-observability

> 来源：`openspec/changes/archive/2026-07-30-agent-observability/`（C5，2026-07-30 用户验收）。
> 范围：决策轨迹作为**协作证据**时的脱敏与使用规范（蓝图 v1.1 §5 指定落点）。
> 说明：本 spec 在 C5 之前没有承载过产品 Agent 条款——C1–C3 的 Agent 条款实际全部落在
> `agent-runtime`。以下为本 spec 第一次承载该主题。

### Requirement: Observability Data Must Not Leak Diary Content Into Collaboration Artifacts

决策轨迹用于排查与证据留存时，SHALL NOT 把用户日记原文带入任何协作产物。

#### Scenario: 轨迹被引用进执行证据

- **WHEN** Agent 把某次排查结论写入 `.ai/AGENT_LOG.md` 或 change 文档
- **THEN** 它只引用结构化标识、数值指标与会话轮次
- **AND** 它 SHALL NOT 粘贴日记原文、对话原文或记忆片段内容

#### Scenario: 轨迹被引用进对话回复

- **WHEN** Agent 在与用户交流中说明某轮发生了什么
- **THEN** 它以结构化事实描述，例如命中条数、判定结论、耗时
- **AND** 它 SHALL NOT 复述用户日记内容作为说明材料

#### Scenario: 探针与联调输出

- **WHEN** Agent 运行真实 provider 探针并记录结果
- **THEN** 输出只包含指标与判定结论
- **AND** 输出 SHALL NOT 包含候选文本原文或用户真实日记

### Requirement: Observability Must Serve Developers Not Product Users

可观测能力 SHALL 只服务于开发者排查，SHALL NOT 被转化为用户可见能力。

#### Scenario: 有人提议把轨迹展示给用户

- **WHEN** 出现「让用户看到 Agent 的思考过程」之类的提议
- **THEN** 该提议被记录为独立 proposal 而非直接实现
- **AND** 既有约束「用户 SHALL NOT 被告知护栏内部判定过程」保持有效

### Requirement: Observability Findings Must Not Trigger Unauthorized Behavior Changes

轨迹揭示的问题 SHALL NOT 成为在同一刀内顺手修改 Agent 行为的依据。

#### Scenario: 轨迹揭示引导话术或检索质量问题

- **WHEN** 轨迹显示某层判定频繁触发或检索命中率低
- **THEN** Agent 把结论记为残余风险或后续 change 的输入
- **AND** 它 SHALL NOT 在可观测 change 内调整提示词、阈值或检索策略

#### Scenario: 轨迹揭示的问题需要立即处理

- **WHEN** 轨迹揭示的问题严重到需要立即修复
- **THEN** Agent 停下并向用户说明，由用户决定是否开新 change 或扩大当前范围
- **AND** 它 SHALL NOT 自行扩大已批准的范围
