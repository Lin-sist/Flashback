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

---

## Accepted From C6 agent-eval-framework

> 来源：`openspec/changes/archive/2026-07-31-agent-eval-framework/`（C6，2026-07-31 用户验收）。
> 范围：评测与样本的协作规范——真实样本的处置、基线更新的协作约束、
> 以及「评测结果不得成为顺手改行为的依据」。
> 延续上文 C5 段落的风格：本 spec 自 C5 起承载产品 Agent 条款。

### Requirement: Evaluation Samples Must Not Enter Collaboration Artifacts

评测用到的真实样本 SHALL NOT 进入任何协作产物。

#### Scenario: 真实样本的处置

- GIVEN 某次评测使用了用户真实日记作为本地输入
- WHEN 记录执行证据
- THEN 执行日志 SHALL 只记录用例标识、维度与判定结论
- AND 执行日志 SHALL NOT 包含样本内容

#### Scenario: 基线与报告作为协作产物

- GIVEN 基线文件与失败报告会被审查
- WHEN 它们被写出
- THEN 它们 SHALL 只包含结构化标识与数值指标
- AND 它们 SHALL NOT 包含用例输入文本

#### Scenario: 对外叙事中的评测内容

- GIVEN 评测能力会被写入对外叙事文档
- WHEN 描述评测
- THEN 描述 SHALL 只使用维度名称、机制与结论
- AND 描述 SHALL NOT 包含真实样本、secret 或本机绝对路径

### Requirement: Baseline Updates Must Be Deliberate And Attributed

基线的更新 SHALL 是一个有归属、可审查的动作。

#### Scenario: 更新基线时的协作义务

- GIVEN 某次改动导致基线指标变化
- WHEN 更新基线
- THEN 更新方 SHALL 在基线说明中记录本次更新归属于哪一个 change
- AND 更新方 SHALL 在执行证据中说明该变化为预期

#### Scenario: 不得静默刷新

- GIVEN 评测因基线不符而失败
- WHEN 处理该失败
- THEN 更新方 SHALL NOT 在未说明原因的情况下把基线改成当前值
- AND 更新方 SHALL NOT 通过绕过或禁用评测使其通过

#### Scenario: 不变量失败的处理义务

- GIVEN 某条不变量断言失败
- WHEN 处理该失败
- THEN 处理方 SHALL 将其视为缺陷并定位原因
- AND 处理方 SHALL NOT 通过放宽该不变量使其通过

### Requirement: Evaluation Findings Must Not Trigger Unauthorized Behavior Changes

评测揭示的问题 SHALL NOT 成为在同一刀内顺手修改 Agent 行为的依据。

#### Scenario: 评测发现缺陷时的范围约束

- GIVEN 建立评测的过程中发现了某个编排缺陷
- WHEN 决定如何处置
- THEN 处置 SHALL 限于如实记录该发现
- AND 修复 SHALL 由独立变更承担，除非用户明确纳入当前变更

#### Scenario: 阈值校准的授权边界

- GIVEN 评测使某个未校准的阈值第一次具备校准条件
- WHEN 决定是否校准
- THEN 建立评测的变更 SHALL NOT 同时校准该阈值
- AND 校准 SHALL 作为独立事项交由用户决定

#### Scenario: 评测能力的如实描述

- GIVEN 评测运行在替身与 mock 路径上
- WHEN 向用户或对外描述其覆盖范围
- THEN 描述 SHALL 明确它覆盖编排逻辑而非模型语言质量
- AND 未验证项与未填充项 SHALL 被如实标注
- AND 描述 SHALL NOT 让读者以为修改提示词已因此变得安全
