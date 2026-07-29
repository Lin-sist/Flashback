# Agent Runtime Spec Delta：agent-memory-retrieval（C3 前半刀）

> 本文件是 delta，不是完整 spec。批准并验收后合入 `openspec/specs/agent-runtime/spec.md`。
> 范围：Memory 检索注入 + 分层来源集合 + 时间归属护栏。回看会话留给 `agent-review-chat`。

---

## MODIFIED Requirements

### 修订 1：C1 条款「C1 范围内的记忆能力」

原 scenario 保留为 C1 阶段范围声明，追加指向本刀的一行。

#### Scenario: C1 范围内的记忆能力

> C3 修订：本 scenario 原文「历史记录检索与跨记录关联 SHALL 留给后续独立 change」已由本刀落实。

- GIVEN 仅 C1 实现存在
- WHEN Agent 组装上下文
- THEN 该阶段的上下文 SHALL NOT 包含跨记录的历史检索结果
- AND 跨记录检索与注入 SHALL 自 C3 起由本 spec 的记忆条款约束

### 修订 2：C2 条款「C2 范围内的记忆能力」

#### Scenario: C2 范围内的记忆能力

> C3 修订：同上。

- GIVEN C2 的工具白名单已生效
- WHEN Agent 组装上下文或构造工具参数
- THEN 工具参数的来源 SHALL 只来自当前会话、当前草稿与本会话的工具执行结果
- AND 跨记录检索结果 SHALL NOT 成为工具参数的合法来源
- AND 上下文中的跨记录检索 SHALL 自 C3 起由本 spec 的记忆条款约束

### 修订 3：C4 条款「C4 范围内的记忆能力」

#### Scenario: C4 范围内的记忆能力

> C3 修订：护栏来源集合自本刀起分层，见下文「分层来源」条款。

- GIVEN 仅 C4 实现存在
- WHEN 护栏执行判定
- THEN 该阶段判定所用的来源 SHALL 只来自当前会话
- AND 来源集合的分层 SHALL 自 C3 起由本 spec 的分层来源条款约束

### 修订 4：C4 忠实度条款下的「来源集合的边界」

本条实质改写。原措辞禁止来源集合包含跨记录检索结果；自本刀起改为分层表述，并保留更严的正文约束。

#### Scenario: 来源集合的边界

- GIVEN 后端构造忠实度判定的来源集合
- WHEN 来源集合被组装
- THEN 来源集合 SHALL 分为当前会话层与记忆层
- AND 当前会话层 SHALL 只包含当前会话中该用户发出的消息
- AND 记忆层 SHALL 只包含本轮实际注入上下文的历史记录片段
- AND 记忆层 SHALL NOT 包含检索到但未注入的片段
- AND 记忆层 SHALL NOT 包含该用户以外任何用户的内容
- AND 来源集合 SHALL NOT 包含 Agent 自己产出的表达

#### Scenario: 进入用户正文的文本的来源层

- GIVEN 一段模型产出文本将进入用户记录正文
- WHEN 忠实度判定被执行
- THEN 合法来源 SHALL 只有当前会话层
- AND 记忆层 SHALL NOT 作为该判定的合法来源
- AND 该约束 SHALL NOT 可由配置放宽

---

## ADDED Requirements

### Requirement: Agent Must Be Able To Retrieve The User's Own Historical Records

Agent SHALL 能检索该用户自己的历史记录，并将检索结果作为理解材料注入对话上下文。

#### Scenario: 检索命中相关历史记录

- GIVEN 一个已登录用户拥有若干历史记录
- WHEN 后端为某一轮对话组装上下文
- THEN 后端 SHALL 基于标签、时间范围与记录的结构化字段检索该用户的历史记录
- AND 命中的片段 SHALL 携带记录标识与时间锚点
- AND 注入的片段数量与单片段长度 SHALL 受 backend-side 配置约束

#### Scenario: 检索无命中

- GIVEN 检索未命中任何历史记录
- WHEN 上下文被组装
- THEN 上下文 SHALL NOT 包含记忆段
- AND Agent SHALL NOT 编造历史关联

#### Scenario: 检索严格按用户隔离

- GIVEN 其他用户存在高度相关的记录
- WHEN 后端为当前用户检索
- THEN 检索结果 SHALL 只包含当前用户自己的记录
- AND 后端 SHALL NOT 返回任何其他用户的内容

#### Scenario: 检索的记录状态范围

- GIVEN 该用户存在已封存但尚未解锁的记录
- WHEN 后端检索历史记录
- THEN 已封存未解锁的记录 SHALL NOT 被检索命中
- AND 封存的产品语义 SHALL 保持不变

#### Scenario: 检索的字段范围

- GIVEN 后端执行历史记录检索
- WHEN 检索条件被构造
- THEN 检索 SHALL 基于标签、时间范围与记录的标题、核心问题、结构化摘要等字段
- AND 检索 SHALL NOT 默认匹配记录正文

#### Scenario: 检索失败或超时

- GIVEN 历史记录检索发生异常或超时
- WHEN 后端处理该轮
- THEN 上下文 SHALL NOT 包含记忆段
- AND 该轮对话 SHALL 正常进行
- AND 护栏判定 SHALL NOT 因检索失败而放宽

#### Scenario: 记忆能力的可替换性

- GIVEN 记忆检索以抽象接口形式暴露给 Runtime
- WHEN 检索实现被替换
- THEN 调用方 SHALL NOT 需要修改
- AND 后续消费记忆的场景 SHALL 复用同一接口

### Requirement: Memory Fragments Must Not Become User Content

注入的记忆片段 SHALL 只作为理解材料，SHALL NOT 进入用户记录正文。

#### Scenario: 模型把记忆片段整理进工具正文参数

- GIVEN 记忆片段已注入本轮上下文
- WHEN 模型产出的工具正文参数内容来自记忆片段而非当前会话表达
- THEN 后端 SHALL 判定该提议不忠实并拒绝
- AND 前端 SHALL NOT 收到待确认提议

#### Scenario: 模型把记忆片段整理进素材草稿

- GIVEN 记忆片段已注入本轮上下文
- WHEN 素材草稿的内容来自记忆片段而非当前会话表达
- THEN 后端 SHALL NOT 返回该素材草稿
- AND 记录正文 SHALL 保持不变

### Requirement: Memory References Must Carry Explicit Time Attribution

Agent 复述记忆片段内容时 SHALL 明确该内容属于过去的哪个时候。

#### Scenario: Agent 带时间归属地引用记忆

- GIVEN Agent 回复中引用了记忆片段的内容
- WHEN 该回复同时包含指明时间的表述
- THEN 后端 SHALL 放行该回复

#### Scenario: Agent 不带时间归属地引用记忆

- GIVEN Agent 回复中存在只被记忆层覆盖、不被当前会话层覆盖的连续片段
- WHEN 该回复不包含指明时间的表述
- THEN 后端 SHALL 以安全兜底回复替换该回复
- AND 后端 SHALL 以结构化痕迹记录该次降级

#### Scenario: 时间归属判定的性质

- GIVEN 时间归属判定被执行
- WHEN 判定结果被计算
- THEN 判定 SHALL 是确定性的
- AND 判定 SHALL NOT 产生对外部服务的调用
- AND 判定过程异常时 SHALL 按违规处理

#### Scenario: 伪引用严判对记忆层同样生效

- GIVEN Agent 表述中包含引号包裹的、声称来自用户的片段
- WHEN 该片段在当前会话层与记忆层中均无对应来源
- THEN 后端 SHALL 按既有伪引用规则拒绝或降级
- AND 引用类判定的严格程度 SHALL NOT 因引入记忆层而被放宽

### Requirement: Existing Guardrail Strength Must Not Be Weakened By Memory

引入记忆层 SHALL NOT 削弱任何既有护栏。

#### Scenario: 既有阈值与层级

- GIVEN 记忆层已引入
- WHEN 审查护栏配置与实现
- THEN 忠实度阈值、回复长度上限、工具白名单与二段式用户确认 SHALL 全部保持原有效力
- AND 后端 SHALL NOT 因引入记忆层而放宽其中任何一项

#### Scenario: 记忆总开关关闭

- GIVEN 记忆能力的总开关被关闭
- WHEN 用户与 Agent 对话
- THEN 行为 SHALL 等价于未引入记忆层之前
- AND 后端 SHALL 留下结构化痕迹说明记忆能力未生效

### Requirement: Memory Fragments Must Stay In Memory And Business Storage Only

记忆片段是其他记录的日记原文，SHALL 只存在于内存与既有业务存储中。

#### Scenario: 记忆片段的持久化边界

- GIVEN 记忆片段被检索并注入上下文
- WHEN 会话消息、工具审计或护栏痕迹被写出
- THEN 这些记录 SHALL NOT 包含记忆片段的内容
- AND 记忆片段 SHALL NOT 被复制到任何新的持久化位置

#### Scenario: 记忆相关的运行日志

- GIVEN 后端记录一次记忆检索或时间归属判定的运行日志
- WHEN 日志被写出
- THEN 日志 SHALL 只包含结构化元数据，例如命中条数、片段总长度、判定结论与违规类型
- AND 日志 SHALL NOT 包含记忆片段内容或用户日记原文

#### Scenario: 分层来源集合的生命周期

- GIVEN 后端为判定构造了分层来源集合
- WHEN 判定结束
- THEN 两层来源 SHALL NOT 被持久化
- AND 两层来源 SHALL NOT 被写入日志或外发

### Requirement: Memory Retrieval Must Exclude Review Chat And Observability Queries

本刀 SHALL 限定在检索、注入、分层来源与时间归属护栏范围内。

#### Scenario: 本刀范围内的回看对话

- GIVEN 记忆能力已落地
- WHEN 审查会话用途
- THEN 后端 SHALL NOT 提供作用于已解锁记录的对话行为
- AND 写作引导对话 SHALL 仍只作用于可编辑的草稿记录
- AND 回看多轮对话 SHALL 留给后续独立 change

#### Scenario: 本刀范围内的可观测能力

- GIVEN 记忆检索与判定痕迹已落地
- WHEN 评估可观测能力
- THEN 本刀 SHALL NOT 提供决策链路查询端点或可观测界面
- AND 决策链路可查询 SHALL 留给后续独立 change

#### Scenario: 本刀范围内的引导与素材质量

- GIVEN 记忆已注入上下文
- WHEN 评估引导话术与素材合成质量
- THEN 本刀 SHALL NOT 修改引导阶段的提问策略
- AND 本刀 SHALL NOT 修改素材合成策略

#### Scenario: 本刀范围内的工具白名单

- GIVEN 记忆能力已落地
- WHEN 审查 Agent 可达的后端写操作
- THEN 可达范围 SHALL NOT 超出 C2 已接受的白名单
- AND 本刀 SHALL NOT 新增任何工具

#### Scenario: 记忆对用户的可见性

- GIVEN 某一轮对话注入了记忆片段
- WHEN 结果返回给前端
- THEN 用户 SHALL NOT 被告知本轮是否检索或注入了历史记录
- AND 前端 SHALL NOT 收到命中的记录标识列表
