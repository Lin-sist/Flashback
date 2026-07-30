# Agent Runtime Spec Delta：agent-observability（C5）

> 本文件是 delta。范围：Agent 决策轨迹（thought → action → observation）的采集、隐私边界、查询与配置。
> **本刀不改任何对话行为**，故不涉及阶段推进、prompt、护栏阈值、记忆检索、工具白名单、回看逻辑的任何条款。
> 四条既有「范围内的可观测能力」scenario 因本刀落地而须修订（下文 MODIFIED）。

---

## MODIFIED Requirements

> 以下四条 scenario 分散在 C2 / C4 / C3a / C3b 四个「Accepted From」段落中，
> 原文均以「决策链路可查询 SHALL 留给后续独立 change」收尾。C5 即那个 change，
> 故四条统一改为**阶段范围声明 + 指向本刀条款**，而非删除。
> 保留它们的理由与 C3b 修订 C3a 条款时相同：范围声明本身是历史事实，删掉就看不出能力何时到位。

### 修订 1：`Agent Tool Calling Must Exclude Memory Post Filtering And Observability Queries` 的可观测 scenario

#### Scenario: C2 范围内的可观测能力

> C5 修订：原文「决策链路可查询 SHALL 留给后续独立 change」已由 C5 落实。

- GIVEN 工具审计数据已落库
- WHEN 评估可观测能力
- THEN C2 SHALL NOT 提供决策链路查询端点或可观测界面
- AND 决策链路可查询 SHALL 自 C5 起由本 spec 的决策轨迹条款约束

### 修订 2：`Guardrails Hardening Must Exclude Memory Observability Queries And Guidance Tuning` 的可观测 scenario

#### Scenario: C4 范围内的可观测能力

> C5 修订：同修订 1。

- GIVEN 护栏痕迹已落地
- WHEN 评估可观测能力
- THEN C4 SHALL NOT 提供决策链路查询端点或可观测界面
- AND 决策链路可查询 SHALL 自 C5 起由本 spec 的决策轨迹条款约束

### 修订 3：`Memory Retrieval Must Exclude Review Chat And Observability Queries` 的可观测 scenario

#### Scenario: C3a 范围内的可观测能力

> C5 修订：同修订 1。

- GIVEN 记忆检索与判定痕迹已落地
- WHEN 评估可观测能力
- THEN C3 前半刀 SHALL NOT 提供决策链路查询端点或可观测界面
- AND 决策链路可查询 SHALL 自 C5 起由本 spec 的决策轨迹条款约束

### 修订 4：`Review Chat Must Exclude Observability Queries And Guidance Tuning` 的可观测 scenario

#### Scenario: C3b 范围内的可观测能力

> C5 修订：同修订 1。

- GIVEN 回看对话已落地
- WHEN 评估可观测能力
- THEN C3 后半刀 SHALL NOT 提供决策链路查询端点或可观测界面
- AND 决策链路可查询 SHALL 自 C5 起由本 spec 的决策轨迹条款约束

---

## ADDED Requirements

### Requirement: Each Conversation Turn Must Leave A Decision Trace

一轮对话 SHALL 留下一条覆盖 thought / action / observation 三段的结构化决策轨迹。

#### Scenario: 正常完成一轮对话

- GIVEN 一个已登录用户提交一轮消息且对话正常完成
- WHEN 该轮结束
- THEN 后端 SHALL 留下一条决策轨迹
- AND 该轨迹 SHALL 包含会话标识、轮次、尝试序号、会话用途派生的模式与阶段
- AND 该轨迹 SHALL 包含模型标识、provider 调用耗时与调用结果

#### Scenario: 开场不构成一轮

> 开场没有与之配对的用户消息（轮次为 0），它不是「一轮对话」。
> 为它造一条轨迹会破坏「一轮一条」的语义，使按轮次读取轨迹时多出一条无对应输入的记录。
> 开场的 provider 结果与护栏判定仍由既有结构化日志覆盖。

- GIVEN 一个会话刚被开启且 Agent 已发出开场
- WHEN 检查该会话的轨迹
- THEN 开场 SHALL NOT 产生一条轮次轨迹

#### Scenario: thought 段的内容

- GIVEN 后端处理某一轮对话
- WHEN 轨迹被写出
- THEN 轨迹 SHALL 包含阶段判定的结论标识
- AND 轨迹 SHALL 包含记忆检索的执行状态与命中数量
- AND 轨迹 SHALL 包含实际注入的记忆片段数量与总长度

#### Scenario: 提示词组装的规模

> 条件限定为「发生了提示词组装」：mock provider 路径不组装提示词，
> 此时轨迹里没有该步骤是正确的，不是采集遗漏。

- GIVEN 后端为某一轮组装提示词
- WHEN 轨迹被写出
- THEN 轨迹 SHALL 包含组装的消息条数与是否含各补充段
- AND 轨迹 SHALL NOT 包含组装后的提示词全文

#### Scenario: action 段的内容

- GIVEN 后端完成一次 provider 调用
- WHEN 轨迹被写出
- THEN 轨迹 SHALL 包含该次调用的耗时，无论调用成功或失败
- AND 轨迹 SHALL 包含模型返回的工具提议数量与其处置结论

#### Scenario: observation 段的内容

- GIVEN 某一轮的护栏判定已完成
- WHEN 轨迹被写出
- THEN 轨迹 SHALL 包含各层判定的结论标识与数值指标
- AND 轨迹 SHALL 标明判定发生在哪一道闸
- AND 轨迹 SHALL 包含该轮是否发生降级及降级路径

#### Scenario: 回复被长度上限裁剪

> 裁剪不构成降级——内容仍是 provider 的产出，只是被截短。
> 但它须可见：排查「Agent 的话为何断在半句」时这是直接答案。

- GIVEN 某轮回复超出长度硬上限并被裁剪
- WHEN 轨迹被写出
- THEN 轨迹 SHALL 记录该次裁剪及裁剪前后长度
- AND 该轮结论 SHALL NOT 因裁剪而被记为降级

#### Scenario: 阶段判定结论的复用

- GIVEN 写作引导的阶段推进已产生判定结论
- WHEN 轨迹记录该轮的 thought 段
- THEN 轨迹 SHALL 使用既有的阶段判定结论标识
- AND 后端 SHALL NOT 为可观测另造一套并行的阶段语义

#### Scenario: 无阶段机模式的轨迹

- GIVEN 一个回看会话不经阶段机推进
- WHEN 轨迹被写出
- THEN 轨迹 SHALL 标明该轮所处的模式
- AND 轨迹 SHALL NOT 伪造一个不存在的阶段判定结论

### Requirement: Decision Traces Must Cover Early Exit Paths

轨迹 SHALL 覆盖提前返回的路径，SHALL NOT 只在完全成功时才产生。

#### Scenario: provider 调用失败

- GIVEN 某一轮的 provider 调用失败
- WHEN 该轮提前返回
- THEN 后端 SHALL 留下该轮的轨迹，包含异常类型与耗时
- AND 既有的失败重试语义 SHALL 保持不变

#### Scenario: 同一轮的重试

- GIVEN 某一轮因 provider 失败后被重试
- WHEN 检查该会话的轨迹
- THEN 两次尝试 SHALL 可被区分
- AND 重试 SHALL NOT 被记录为一个新的轮次

#### Scenario: 回复被降级为安全兜底

- GIVEN 某轮回复被护栏降级
- WHEN 轨迹被写出
- THEN 轨迹 SHALL 标明违规类型与降级路径
- AND 轨迹 SHALL 可区分该回复来自本地兜底而非 provider 正常产出

#### Scenario: 工具提议被 fail-closed 丢弃

- GIVEN 某个模式不下发工具而模型仍返回工具提议
- WHEN 该提议被丢弃
- THEN 轨迹 SHALL 记录该次丢弃事件

#### Scenario: 记忆未命中、检索失败与记忆未生效

- GIVEN 某一轮的记忆检索没有产生片段
- WHEN 轨迹被写出
- THEN 轨迹 SHALL 可区分「检索成功但无命中」「检索失败」「记忆能力被配置关闭」三种情形
- AND 后端 SHALL NOT 将它们记录为同一种情形

#### Scenario: 素材产出失败

- GIVEN 某轮的素材生成因 provider 失败而未产出
- WHEN 轨迹被写出
- THEN 轨迹 SHALL 记录该次失败
- AND 该轮结论 SHALL NOT 因此被记为失败，因为素材是可选产物而对话本身已成功

### Requirement: Decision Traces Must Exclude Diary And Conversation Content

轨迹 SHALL 只包含结构化标识、数值指标与不可还原的摘要。

#### Scenario: 轨迹的内容边界

- GIVEN 一轮对话涉及用户日记原文、对话原文与记忆片段
- WHEN 该轮轨迹被写出
- THEN 轨迹 SHALL NOT 包含用户日记原文
- AND 轨迹 SHALL NOT 包含对话原文
- AND 轨迹 SHALL NOT 包含记忆片段内容
- AND 轨迹 SHALL NOT 包含护栏候选文本或未覆盖片段的内容

#### Scenario: 涉及文本的字段

- GIVEN 轨迹需要表达某段文本的规模
- WHEN 该字段被写出
- THEN 该字段 SHALL 以长度或不可还原的哈希前缀表达
- AND 该字段 SHALL NOT 保存该文本原文

#### Scenario: provider 原始响应

- GIVEN 后端收到 provider 的响应
- WHEN 轨迹被写出
- THEN 轨迹 SHALL NOT 包含 provider 响应体原文

#### Scenario: 轨迹的可见性边界

- GIVEN 轨迹已落地
- WHEN 结果返回给前端
- THEN 产品接口 SHALL NOT 返回轨迹数据
- AND 用户 SHALL NOT 被告知任何内部判定过程

### Requirement: Trace Recording Must Not Break The Conversation

轨迹写入 SHALL 是 fail-open 的，SHALL NOT 影响用户这一轮对话的结果。

#### Scenario: 轨迹写入失败

- GIVEN 轨迹写入过程发生异常
- WHEN 用户这一轮对话正在处理
- THEN 该轮对话 SHALL 正常完成
- AND 后端 SHALL 记录结构化痕迹说明轨迹写入失败

#### Scenario: 轨迹写入与业务数据的事务边界

- GIVEN 一轮对话的用户消息与 Agent 回复已持久化
- WHEN 轨迹写入失败
- THEN 已持久化的用户消息与 Agent 回复 SHALL NOT 被回滚

### Requirement: Observability Must Be Configurable Without Silent Degradation

可观测能力 SHALL 由 backend-side 配置控制，关闭时 SHALL NOT 静默。

#### Scenario: 可观测能力被关闭

- GIVEN 可观测能力被配置关闭
- WHEN 用户与 Agent 对话
- THEN 行为 SHALL 等价于引入决策轨迹之前
- AND 后端 SHALL 留下结构化痕迹说明可观测能力未生效
- AND 后端 SHALL NOT 静默表现为轨迹无数据

#### Scenario: 采集范围

- GIVEN 可观测能力处于启用状态
- WHEN 用户提交一轮消息
- THEN 该轮 SHALL 被采集
- AND 采集 SHALL NOT 按比例丢弃轮次

### Requirement: Decision Traces Must Carry Version Anchors For Later Regression

轨迹 SHALL 携带可用于后续回归比对的版本锚点。

#### Scenario: 版本锚点字段

- GIVEN 某一轮轨迹被写出
- WHEN 检查该轨迹
- THEN 轨迹 SHALL 包含轨迹标识、模型标识、提示词版本与护栏规则版本

#### Scenario: 版本锚点与内容的一致性

- GIVEN 提示词模板或护栏规则文案被修改
- WHEN 新的轨迹被写出
- THEN 对应的版本锚点值 SHALL 发生变化
- AND 版本锚点 SHALL NOT 依赖人工维护而与实际内容脱节

### Requirement: Decision Traces Must Be Queryable By Session

轨迹 SHALL 可按会话取回，供开发者排查。

#### Scenario: 按会话取回轨迹

- GIVEN 某会话已产生多轮轨迹
- WHEN 按会话标识查询
- THEN 后端 SHALL 返回该会话按轮次有序的完整轨迹

#### Scenario: 轨迹的归属边界

- GIVEN 轨迹按用户归属存储
- WHEN 某用户被删除
- THEN 其轨迹 SHALL 被级联清理

### Requirement: Observability Must Exclude Evaluation Resilience And Temporal Intelligence

C5 SHALL 限定在决策轨迹的采集、隐私、查询与配置范围内。

#### Scenario: C5 范围内的对话行为

- GIVEN 决策轨迹已落地
- WHEN 审查 Agent 的对话行为
- THEN C5 SHALL NOT 修改阶段推进、提示词内容、护栏阈值、记忆检索、工具白名单或回看逻辑
- AND 轨迹 SHALL 只观测行为，SHALL NOT 改变行为

#### Scenario: C5 范围内的评估能力

- GIVEN 轨迹已携带版本锚点
- WHEN 评估回归能力
- THEN C5 SHALL NOT 提供黄金集、评估运行器或质量评分
- AND 评估能力 SHALL 留给后续独立 change

#### Scenario: C5 范围内的韧性与时间智能

- GIVEN 轨迹记录了失败与耗时
- WHEN 评估失败处置与时间感知能力
- THEN C5 SHALL NOT 引入错误分类策略、降级模板或多 provider 路由
- AND C5 SHALL NOT 引入时间距离话术或记忆衰减策略
- AND 两者 SHALL 留给后续独立 change

#### Scenario: C5 范围内的用户可见能力

- GIVEN 轨迹面向开发者
- WHEN 审查用户可见界面
- THEN C5 SHALL NOT 新增任何用户可见能力
- AND C5 SHALL NOT 提供实时告警或质量看板

#### Scenario: C5 范围内的引导与素材质量

- GIVEN 轨迹可反映引导与素材的判定结果
- WHEN 评估引导话术与素材合成质量
- THEN C5 SHALL NOT 修改引导阶段的提问策略
- AND C5 SHALL NOT 修改素材合成策略
- AND 质量优化 SHALL 留给后续独立安排
