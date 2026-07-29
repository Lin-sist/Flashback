# V2 Product Scope Spec Delta：agent-memory-retrieval（C3 前半刀）

> 本文件是 delta。范围：记忆能力的产品行为与气质边界。

---

## ADDED Requirements

### Requirement: Agent Memory Must Feel Like A Friend Remembering, Not A System Analyzing

记忆能力 SHALL 服务于共情式陪伴，SHALL NOT 表现为对用户的分析。

#### Scenario: Agent 关联历史感受

- GIVEN 用户此刻表达的内容与其过去的记录相关
- WHEN Agent 回应
- THEN Agent MAY 以朋友的方式提起那时候的事
- AND Agent SHALL 说清那是过去哪个时候的事
- AND Agent SHALL NOT 对用户做归类、画像或诊断

#### Scenario: 用户不接续历史话题

- GIVEN Agent 提起了一段过去的记录
- WHEN 用户没有接续该话题
- THEN Agent SHALL NOT 反复追问同一条线索

#### Scenario: 记忆不产生主动打扰

- GIVEN 记忆能力已启用
- WHEN 用户未主动开启对话
- THEN 系统 SHALL NOT 因检索到相关历史而弹窗、推送或自动展开对话

#### Scenario: 记忆不对用户暴露检索过程

- GIVEN 某一轮对话使用了历史记录
- WHEN 用户看到 Agent 的回复
- THEN 用户 SHALL NOT 看到检索命中数量、记录清单或任何检索状态提示

### Requirement: Sealed Records Must Not Be Disclosed Ahead Of Their Unlock Moment

封存尚未解锁的记录内容 SHALL NOT 经由 Agent 提前呈现给用户。

#### Scenario: Agent 关联未解锁的封存记录

- GIVEN 用户存在已封存但尚未到解锁时刻的记录
- WHEN Agent 组装记忆上下文
- THEN 该记录 SHALL NOT 被纳入
- AND 用户 SHALL NOT 通过 Agent 提前读到其中的内容

#### Scenario: 时间回看的产品语义

- GIVEN 记忆能力已启用
- WHEN 评估封存与解锁的产品语义
- THEN 「把回答权交给时间」的语义 SHALL 保持不变
- AND 记忆能力 SHALL NOT 成为提前拆封的旁路

### Requirement: Memory Must Not Rewrite The User's Timeline

记忆 SHALL 只用于理解用户，SHALL NOT 把过去的表达写进此刻的记录。

#### Scenario: 过去的表达与此刻的记录

- GIVEN 记忆片段已被 Agent 读取
- WHEN 记录正文被追加内容
- THEN 追加的内容 SHALL 只来自用户在本次对话中的表达
- AND 过去记录中的句子 SHALL NOT 被搬进此刻的记录正文

### Requirement: Memory Scope Must Exclude Profiling And Visualization

记忆能力 SHALL NOT 演变为分析型产品能力。

#### Scenario: 范围外的产品能力

- GIVEN 记忆能力已落地
- WHEN 审查产品范围
- THEN 情绪轨迹可视化、用户画像、标签自动归类与行为评分 SHALL NOT 被实现
- AND 三个一级 Tab 与用户可见命名 SHALL 保持不变
