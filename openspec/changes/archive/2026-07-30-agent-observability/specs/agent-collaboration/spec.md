# Agent Collaboration Spec Delta：agent-observability（C5）

> 本文件是 delta。范围：决策轨迹作为**协作证据**时的脱敏与使用规范。
> 落点依据：蓝图 v1.1 §5 指定 C5 的协作规范落在本 spec。
> 注意：本 spec 的 baseline 目前**没有** Accepted From 段落（C1–C3 的 Agent 条款实际全部落在 `agent-runtime`），
> 故本 delta 是本 spec 第一次承载产品 Agent 相关条款，全部为 ADDED。

---

## ADDED Requirements

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
