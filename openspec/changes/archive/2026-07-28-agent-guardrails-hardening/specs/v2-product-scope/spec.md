# V2 Product Scope Spec Delta（C4 `agent-guardrails-hardening`）

> 承载 C4 的产品行为与 Agent 气质约束。
> 技术契约见本 change 的 `specs/agent-runtime/spec.md` 与 `specs/backend-core/spec.md`。
> 待规划闸批准；批准并实现验收后才接受进 `openspec/specs/v2-product-scope/spec.md`。

## ADDED Requirements

### Requirement: User Original Expression Must Remain The Only Source Of Record Content

进入用户记录正文的文字 SHALL 只源于用户自己的表达。

#### Scenario: Agent 协助整理后的正文

- GIVEN 用户通过与 Agent 的对话产生了可回填正文的内容
- WHEN 该内容进入记录正文
- THEN 正文中 SHALL NOT 出现用户未曾表达过的观点、情绪或事实
- AND 用户既有正文 SHALL 逐字保持不变

#### Scenario: 用户回看已封存记录

- GIVEN 用户在未来解锁并回看一条记录
- WHEN 用户阅读正文
- THEN 正文 SHALL 只呈现用户当时自己说过的内容
- AND 产品 SHALL NOT 让用户读到由 Agent 代写的心情

#### Scenario: Agent 声称帮用户整理

- GIVEN Agent 在对话中表示可以帮用户整理已说过的内容
- WHEN 该整理结果被提供
- THEN 整理 SHALL 限于语序、冗余与措辞层面的组织
- AND 整理 SHALL NOT 补写用户没有表达的内容

### Requirement: Agent Must Stay Empathetic Without Diagnosing Or Claiming Actions

Agent SHALL 在共情的同时不做诊断、不声称代替用户完成决策。

#### Scenario: 用户描述疑似心理困扰

- GIVEN 用户描述了疑似心理困扰的感受
- WHEN Agent 回应
- THEN Agent SHALL 以共情方式回应
- AND Agent SHALL NOT 给出病症判断或医学建议

#### Scenario: 用户自己使用病症词

- GIVEN 用户在自己的表达中使用了某个病症词
- WHEN Agent 回应并复述用户的说法
- THEN 该复述 SHALL 被允许
- AND 产品 SHALL NOT 因此让 Agent 回避用户的感受

#### Scenario: 用户请求 Agent 代为封存

- GIVEN 用户在对话中请求 Agent 代为封存、解锁或删除记录
- WHEN Agent 回应
- THEN Agent SHALL 只建议用户自行确认
- AND Agent SHALL NOT 声称已经完成该操作

### Requirement: Guardrail Enforcement Must Stay Invisible And Non Disruptive

护栏生效 SHALL NOT 破坏对话体验，也 SHALL NOT 向用户暴露内部判定过程。

#### Scenario: 某次提议被护栏拦下

- GIVEN Agent 的一次行动提议因内容不忠实被拦下
- WHEN 用户继续这次对话
- THEN 用户 SHALL 只感知到这一轮没有出现行动建议
- AND 对话 SHALL 正常继续
- AND 已产生的内容 SHALL NOT 丢失

#### Scenario: 某轮回复被降级

- GIVEN 某轮 Agent 回复因越界被替换为安全兜底回复
- WHEN 用户读到该回复
- THEN 回复 SHALL 保持安静克制的语气
- AND 产品 SHALL NOT 向用户展示护栏判定过程或违规提示

#### Scenario: 护栏偶尔误伤合法整理

- GIVEN 一次合法的整理被护栏误判
- WHEN 用户继续使用
- THEN 后果 SHALL 限于少一次行动建议或少一段候选素材
- AND 用户记录 SHALL NOT 因此发生任何非预期变更

### Requirement: Guardrails Hardening Must Not Introduce Scoring Or Monitoring Surfaces

护栏加固 SHALL NOT 引入面向用户的评分、诊断或监控界面。

#### Scenario: 审查产品可见范围

- GIVEN C4 实现完成
- WHEN 审查用户可见界面
- THEN 三个一级 Tab 与用户可见命名 SHALL 保持不变
- AND 产品 SHALL NOT 新增情绪评分、心理诊断或 Agent 行为监控界面

#### Scenario: 被动召唤原则

- GIVEN 护栏检出一次越界
- WHEN 后端完成降级处理
- THEN 产品 SHALL NOT 因此向用户发起推送、弹窗或未请求的提示
