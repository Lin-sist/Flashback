# Delta Spec：v2-product-scope（P4.2 Memory Agency）

## ADDED Requirements

### Requirement: The Present Moment Must Be The Default Context

产品 SHALL 默认让当前记录与当前会话优先，过去记录不得在用户不知情时参与当前理解。

#### Scenario: 用户未授权过去

- GIVEN 用户只是写下或谈论此刻
- WHEN Agent 回应
- THEN Agent SHALL 只依据当前记录与当前会话
- AND SHALL NOT 因配置、相似词或历史命中自动引用其他记录

#### Scenario: 用户主动授权

- GIVEN 用户明确开启“本次可参考过去记录”
- WHEN 当前 session 继续
- THEN 符合边界的过去记录 MAY 参与理解
- AND 授权 SHALL 不跨 session 永久继承

### Requirement: Past Records May Participate Only With Visibility And Revocability

过去参与 Agent 理解 SHALL 同时具备用户许可、实际来源可见与未来可撤销能力。

#### Scenario: 实际使用可见

- GIVEN Agent 本轮实际使用一条过去记录
- WHEN 用户查看回复
- THEN 用户 SHALL 能看到并在可用时回到该来源
- AND 产品 SHALL NOT 暴露分数、命中数、关键词或内部检索逻辑

#### Scenario: 用户关闭或排除

- GIVEN 用户关闭本次授权或排除某条记录
- WHEN 未来轮次开始
- THEN 对应过去 SHALL 不再参与
- AND 产品 SHALL NOT 把撤销设计成永久设置或高成本流程

#### Scenario: 用户删除记录

- GIVEN 用户删除一条曾被参考的记录
- WHEN 后续对话或旧来源被查看
- THEN 该记录 SHALL 不再参与未来理解
- AND 旧来源 SHALL 不成为恢复已删除内容的旁路

### Requirement: User Corrections Must Outrank AI Inferences

用户对过去表达的解释 SHALL 保持为用户自己的语境；AI 推测不得自动成为长期事实。

#### Scenario: 只代表当时

- GIVEN 用户说明一条记录只代表当时、不代表现在
- WHEN Agent 未来参考该记录
- THEN SHALL 保留该时间距离与用户解释权
- AND SHALL NOT 用过去表达定义用户当前人格或状态

#### Scenario: AI 推测长期模式

- GIVEN Agent 从多条记录生成了阶段、趋势或人格推测
- WHEN 对话结束
- THEN 推测 SHALL NOT 自动持久化为用户事实
- AND SHALL NOT 形成画像、诊断、评分或成长报告

### Requirement: Memory Agency Must Preserve Witness And Ownership Boundaries

P4.2 SHALL 扩大用户控制而不是扩大 Agent 权力。

#### Scenario: 记录正文与工具

- GIVEN 过去记录已获授权并被 Agent 读取
- WHEN 生成回复、素材或工具参数
- THEN 过去内容 SHALL NOT 被写入当前记录正文
- AND 工具/素材的显式确认与用户原文忠实度 SHALL 保持

#### Scenario: 回看目标

- GIVEN 用户主动打开一条自己的 UNLOCKED 记录进行回看
- WHEN 未额外授权其他历史
- THEN Agent MAY 参考目标记录自身
- AND SHALL NOT 扩展到其他记录

#### Scenario: 产品范围

- GIVEN P4.2 完成
- WHEN 审查能力边界
- THEN SHALL NOT 新增向量检索、画像、趋势、自动总结、全局永久授权或 LLM-as-Judge
- AND SHALL 保持三个一级 Tab、canonical naming 与 witness role
