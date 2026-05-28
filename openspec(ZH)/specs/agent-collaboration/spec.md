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
