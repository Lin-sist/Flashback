# Agent Runtime Spec Delta（C2 `agent-tool-calling`）

> 承载 C2 的 Agent 工具调用主契约。C1 已接受条款除本文 MODIFIED 段落外全部保持不变。
> 待规划闸批准；批准并实现验收后才接受进 `openspec/specs/agent-runtime/spec.md`。

## ADDED Requirements

### Requirement: Tool Proposals Must Use Native Provider Function Calling Without Fallback Protocol

Agent 工具提议 SHALL 经由 provider 原生 function calling 机制表达，SHALL NOT 存在替代的自研提议协议。

#### Scenario: 下发工具定义

- GIVEN 工具白名单已声明
- WHEN 后端向 provider 发起 Agent 对话请求
- THEN 请求 SHALL 携带由白名单生成的工具定义
- AND 工具定义 SHALL 是白名单的派生物，SHALL NOT 与白名单产生偏离

#### Scenario: 解析模型响应

- GIVEN provider 返回一次对话响应
- WHEN 后端解析该响应
- THEN 自然语言回复 SHALL 取自响应的消息内容字段
- AND 工具提议 SHALL 取自响应的原生工具调用字段

#### Scenario: 响应只含工具提议而无自然语言回复

- GIVEN provider 返回了工具提议但消息内容为空
- WHEN 后端构造该轮回复
- THEN 后端 SHALL 使用提议自带的提议话术作为该轮回复
- AND 该轮 SHALL NOT 出现空白的 Agent 回复

#### Scenario: provider 不支持工具调用

- GIVEN 配置的模型不在已确认支持工具调用的范围内，或 provider 拒绝工具定义
- WHEN 用户提交一轮消息
- THEN 后端 SHALL NOT 下发工具定义
- AND 后端 SHALL NOT 改用任何替代的提议协议来模拟工具调用
- AND 对话 SHALL 退回不含工具提议的行为，或返回显式不可用状态

#### Scenario: 单轮返回多个工具提议

- GIVEN provider 在一轮中返回多个工具调用
- WHEN 后端处理该轮
- THEN 后端 SHALL 只保留第一个通过校验的提议
- AND 其余提议 SHALL 被丢弃并记入审计
- AND 前端 SHALL 至多看到一个待确认提议

### Requirement: Tool Execution Must Not Occur Within Reply Generation

后端 SHALL NOT 在生成 Agent 回复的同一处理过程中执行工具或将工具结果回灌给模型。

#### Scenario: 收到工具提议的当次处理

- GIVEN provider 在某轮返回工具提议
- WHEN 后端处理该轮
- THEN 后端 SHALL 在持久化提议后结束该轮处理
- AND 后端 SHALL NOT 在该轮内执行该工具
- AND 后端 SHALL NOT 在该轮内向 provider 追加工具结果消息并再次请求

#### Scenario: 每轮对话的 provider 请求次数

- GIVEN 用户提交一轮消息
- WHEN 后端完成该轮处理
- THEN 该轮用于生成回复的 provider 请求次数 SHALL 保持有界且可预测
- AND 后端 SHALL NOT 进入由模型驱动的工具调用循环

### Requirement: Agent Tools Must Be Restricted To A Code Level Allowlist

后端 SHALL 以代码级白名单定义 Agent 可用工具集合，白名单外的后端写操作 SHALL NOT 对 Agent 可达。

#### Scenario: 模型提议白名单内的工具

- GIVEN 一个 `ACTIVE` 会话已绑定可编辑草稿记录
- WHEN 模型输出中包含白名单内的工具提议且参数合法
- THEN 后端 SHALL 持久化该提议为待确认状态
- AND 后端 SHALL 将该提议随会话状态返回给前端

#### Scenario: 模型提议白名单外的工具

- GIVEN 模型输出中包含未注册的工具名
- WHEN 后端处理该轮
- THEN 后端 SHALL 拒绝该提议
- AND 后端 SHALL NOT 向前端下发该提议
- AND 该轮 Agent 回复 SHALL 正常返回，不因提议无效而整轮失败

#### Scenario: 模型提议参数非法

- GIVEN 提议的工具在白名单内但参数缺失、类型错误或超出边界
- WHEN 后端校验该提议
- THEN 后端 SHALL 拒绝该提议
- AND 后端 SHALL NOT 执行任何写操作

#### Scenario: provider 侧无法表达的参数边界

- GIVEN 某些长度或数量边界无法由 provider 侧的结构约束表达
- WHEN 后端校验提议参数
- THEN 后端 SHALL 在代码层完成这些边界校验
- AND 后端 SHALL NOT 因 provider 已做结构校验而跳过业务边界校验

#### Scenario: 读取型工具的数据获取

- GIVEN Agent 需要可选标签清单或当前草稿快照
- WHEN 后端组装对话上下文
- THEN 这些数据 SHALL 由后端主动注入上下文
- AND 后端 SHALL NOT 要求模型通过工具调用来获取它们

#### Scenario: 提议内容无法解析

- GIVEN provider 返回的提议结构无法解析
- WHEN 后端处理该轮
- THEN 后端 SHALL 按无提议处理
- AND 后端 SHALL NOT 猜测工具名或补全参数

### Requirement: Agent Must Never Execute Tools Without Explicit User Confirmation

工具执行 SHALL 只能由用户确认后的独立请求触发，Agent SHALL NOT 在生成回复的同一处理过程中执行任何工具。

#### Scenario: 用户确认执行

- GIVEN 存在一个待确认的工具提议
- WHEN 用户显式确认执行
- THEN 后端 SHALL 执行该工具
- AND 后端 SHALL 将该提议标记为已执行
- AND 后端 SHALL 返回执行结果

#### Scenario: 用户拒绝执行

- GIVEN 存在一个待确认的工具提议
- WHEN 用户显式拒绝
- THEN 后端 SHALL 将该提议标记为已拒绝
- AND 目标记录 SHALL 保持完全不变
- AND 会话 SHALL 可继续

#### Scenario: 用户既不确认也不拒绝

- GIVEN 存在一个待确认的工具提议
- WHEN 用户直接提交下一轮消息或结束会话
- THEN 该提议 SHALL NOT 被执行
- AND 目标记录 SHALL 保持完全不变

#### Scenario: 生成回复的同一处理过程中

- GIVEN 后端正在生成某一轮的 Agent 回复
- WHEN 模型输出包含工具提议
- THEN 后端 SHALL NOT 在该处理过程中执行任何记录写操作
- AND 写操作 SHALL 只发生在后续的用户确认请求中

### Requirement: Tool Confirmation Must Be Owner Scoped And Idempotent

工具提议的确认 SHALL 严格按用户隔离，并且对重复确认幂等。

#### Scenario: 本人确认自己会话中的提议

- GIVEN 一个已登录用户拥有该会话与该提议
- WHEN 该用户确认或拒绝该提议
- THEN 后端 SHALL 允许该操作

#### Scenario: 跨用户确认提议

- GIVEN 该提议属于其他用户，或该提议不属于所指定的会话
- WHEN 一个已登录用户尝试确认或拒绝
- THEN 后端 SHALL 拒绝该操作或返回安全的未找到响应
- AND 后端 SHALL NOT 泄露该提议是否存在或其内容

#### Scenario: 未登录确认提议

- GIVEN 请求没有有效凭证
- WHEN 访问工具确认端点
- THEN 后端 SHALL 返回未授权

#### Scenario: 重复确认同一提议

- GIVEN 某提议已处于已执行、已失败或已拒绝状态
- WHEN 用户再次确认该提议
- THEN 后端 SHALL NOT 再次执行该工具
- AND 后端 SHALL 返回该提议的当前状态
- AND 目标记录 SHALL NOT 产生重复内容或重复标签

### Requirement: Tool Execution Must Inherit Existing Record Constraints

工具执行 SHALL 复用既有记录业务路径，并继承其归属校验与状态约束，SHALL NOT 存在仅 Agent 可用的旁路。

#### Scenario: 目标记录仍为草稿

- GIVEN 会话绑定的记录处于 DRAFT
- WHEN 用户确认执行写工具
- THEN 后端 SHALL 通过既有记录业务路径完成写入

#### Scenario: 目标记录已封存或已解锁

- GIVEN 会话绑定的记录在提议后被封存，或处于 SEALED / UNLOCKED
- WHEN 用户确认执行写工具
- THEN 后端 SHALL 拒绝该执行
- AND 后端 SHALL 返回明确的失败原因
- AND 封存后的不可变契约 SHALL 保持不变

#### Scenario: 目标记录不属于当前用户

- GIVEN 提议参数指向不属于当前用户的记录
- WHEN 用户确认执行
- THEN 后端 SHALL 拒绝该执行

#### Scenario: 会话未绑定记录

- GIVEN 会话没有绑定任何记录
- WHEN 需要记录上下文的写工具被确认
- THEN 后端 SHALL 拒绝该执行
- AND 后端 SHALL NOT 创建新记录以满足该工具

### Requirement: Content And Tag Tools Must Only Append

正文与标签类工具 SHALL 只追加，SHALL NOT 覆写、替换或删除用户既有内容。

#### Scenario: 记录已有正文时追加素材

- GIVEN 目标草稿已有正文
- WHEN 追加正文的工具被执行
- THEN 既有正文 SHALL 逐字保持不变
- AND 新内容 SHALL 追加在既有正文之后

#### Scenario: 记录已有标签时追加标签

- GIVEN 目标草稿已有标签
- WHEN 追加标签的工具被执行
- THEN 既有标签 SHALL 全部保留
- AND 新标签 SHALL 追加到既有标签集合
- AND 重复标签 SHALL NOT 产生重复绑定

#### Scenario: 标签不在启用集合内

- GIVEN 提议的标签不存在或未启用
- WHEN 该工具被确认执行
- THEN 后端 SHALL 拒绝该执行
- AND 后端 SHALL NOT 创建新标签

#### Scenario: 试图通过工具改写用户原文

- GIVEN 用户或模型试图通过工具替换、精简或润色既有正文
- WHEN 后端处理该提议
- THEN 后端 SHALL 拒绝该提议
- AND 用户原文 SHALL 保持不变

### Requirement: Irreversible And Out Of Scope Operations Must Not Be Tool Reachable

不可逆操作与范围外操作 SHALL NOT 出现在工具白名单中，Agent SHALL 只能以自然语言建议用户自行确认。

#### Scenario: 用户在对话中要求封存

- GIVEN 用户在对话中要求封存、解锁或删除记录
- WHEN Agent 回应
- THEN Agent SHALL 只给出建议并引导用户自行确认
- AND 后端 SHALL NOT 提供可完成封存、解锁或删除的工具

#### Scenario: 模型提议触碰不可逆操作

- GIVEN 模型提议封存、解锁、删除、后来其实、解锁提醒授权、位置、封面或附件相关操作
- WHEN 后端校验该提议
- THEN 后端 SHALL 拒绝该提议
- AND 后端 SHALL NOT 下发任何可执行该操作的确认入口

#### Scenario: 审查工具可达范围

- GIVEN C2 实现完成
- WHEN 审查 Agent 可达的后端写操作
- THEN 可达范围 SHALL 限于白名单内的草稿字段追加与设置
- AND 任何白名单外的记录写操作 SHALL NOT 可由 Agent 触发

### Requirement: Tool Execution Must Fail Explicitly Without Fake Success

工具执行 SHALL 在失败时返回显式失败语义，SHALL NOT 静默失败或谎报成功。

#### Scenario: 工具执行被业务校验拒绝

- GIVEN 工具执行触发了记录状态或参数校验失败
- WHEN 后端返回结果
- THEN 后端 SHALL 返回明确的失败状态与可读原因
- AND 后端 SHALL NOT 返回成功
- AND 目标记录 SHALL 保持不变

#### Scenario: Agent 不可用时的工具与记录生命周期

- GIVEN Agent 对话或工具执行不可用
- WHEN 用户保存草稿或封存记录
- THEN 记录保存与封存 SHALL 正常完成
- AND Agent 可用性 SHALL NOT 成为记录生命周期的依赖

#### Scenario: 工具执行失败后的会话

- GIVEN 某次工具执行失败
- WHEN 用户继续对话
- THEN 会话 SHALL 保持可用
- AND 已产生的素材 SHALL NOT 丢失

### Requirement: Tool Outcomes Must Be Fed Back Into Conversation Context

工具执行结果 SHALL 以结构化摘要回注对话上下文，使 Agent 后续回复能够感知已发生的行动。

#### Scenario: 执行成功后的下一轮

- GIVEN 某工具已成功执行
- WHEN 用户提交下一轮消息
- THEN 组装的上下文 SHALL 包含该次执行的结构化摘要
- AND Agent SHALL NOT 重复提议同一个已完成的行动

#### Scenario: 回注内容的隐私边界

- GIVEN 工具结果被回注上下文
- WHEN 构造回注摘要
- THEN 摘要 SHALL 只包含结构化事实，例如工具名、目标记录标识与结果状态
- AND 摘要 SHALL NOT 额外复制用户日记原文之外的推断性结论

### Requirement: Tool Call Audit Must Exclude Diary And Conversation Content

工具提议与执行 SHALL 留下结构化审计记录，且 SHALL NOT 包含用户日记原文或对话原文。

#### Scenario: 提议与执行被审计

- GIVEN 一次工具提议产生并被确认或拒绝
- WHEN 后端持久化审计记录
- THEN 记录 SHALL 携带所属用户标识、会话标识、工具名、状态与时间
- AND 用户被删除时审计记录 SHALL 被级联清理

#### Scenario: 审计记录中的参数

- GIVEN 工具参数包含来自用户表达的文本
- WHEN 审计记录被写出
- THEN 审计记录 SHALL 只保存该文本的结构化摘要
- AND 审计记录 SHALL NOT 长期保存该文本原文

#### Scenario: 待确认期间的执行参数

- GIVEN 一条提议处于待确认状态且执行需要原始参数
- WHEN 后端持久化该提议
- THEN 执行参数 MAY 以瞬态形式保存，直到该提议被确认、拒绝或失败
- AND 执行参数 SHALL NOT 由客户端在确认时回传
- AND 后端 SHALL NOT 依据客户端提供的参数执行工具

#### Scenario: 提议终结后的执行参数

- GIVEN 一条提议已进入已执行、已失败或已拒绝状态
- WHEN 检查该提议的持久化记录
- THEN 瞬态执行参数 SHALL 已被清除
- AND 仅结构化摘要 SHALL 被长期保留

#### Scenario: 工具相关日志

- GIVEN 后端记录工具提议或执行的运行日志
- WHEN 日志被写出
- THEN 日志 SHALL 只包含结构化元数据，例如会话标识、提议标识、工具名、状态、失败类型与耗时
- AND 日志 SHALL NOT 包含对话原文或用户日记原文

### Requirement: Tool Confirmation Must Not Advance Conversation Stage Or Turn Count

工具确认 SHALL NOT 改变会话阶段或轮次计数。

#### Scenario: 确认执行后的会话进度

- GIVEN 一个处于某引导阶段的 `ACTIVE` 会话
- WHEN 用户确认或拒绝一个工具提议
- THEN 会话阶段 SHALL 保持不变
- AND 轮次计数 SHALL 保持不变
- AND 会话轮次上限 SHALL NOT 因工具确认而被消耗

#### Scenario: 工具确认与失败重试语义

- GIVEN 某轮 Agent 回复失败且需要重试
- WHEN 用户在该状态下确认一个既有工具提议
- THEN 失败轮的重试语义 SHALL 保持不变
- AND 工具确认 SHALL NOT 创建没有配对用户消息的轮次

### Requirement: Agent Tool Calling Must Exclude Memory Post Filtering And Observability Queries

C2 SHALL 限定在工具白名单、二段式确认与受控执行范围内。

#### Scenario: C2 范围内的记忆能力

- GIVEN 用户与 Agent 对话
- WHEN Agent 组装上下文或构造工具参数
- THEN 上下文与参数 SHALL 只来自当前会话、当前草稿与本会话的工具执行结果
- AND 历史记录检索与跨记录关联 SHALL 留给后续独立 change

#### Scenario: C2 范围内的护栏深度

- GIVEN C2 的工具白名单已生效
- WHEN 评估内容合规防御深度
- THEN C2 SHALL NOT 包含后置内容过滤或违规降级机制
- AND 系统化 hardening SHALL 留给后续独立 change

#### Scenario: C2 范围内的可观测能力

- GIVEN 工具审计数据已落库
- WHEN 评估可观测能力
- THEN C2 SHALL NOT 提供决策链路查询端点或可观测界面
- AND 决策链路可查询 SHALL 留给后续独立 change

## MODIFIED Requirements

### Requirement: Agent Runtime MVP Must Exclude Tools Memory And Post Filtering

> C1 原条款中的「工具调用留给后续独立 change」已由本 change 落实。此处收窄该条款范围为 C1 历史约束，并把工具边界移交上文 ADDED 条款。

C1 SHALL 限定在对话 Runtime 与最小护栏范围内；工具调用边界自 C2 起由本 spec 的工具相关条款约束。

#### Scenario: C1 范围内的工具调用

- GIVEN 仅 C1 实现存在，工具白名单尚未引入
- WHEN 审查 Agent 行为
- THEN Agent SHALL NOT 调用除自身会话与消息持久化以外的任何后端写操作

#### Scenario: C2 之后的工具调用

- GIVEN 工具白名单已引入
- WHEN 审查 Agent 行为
- THEN Agent 可达的后端写操作 SHALL 限于白名单内工具
- AND 执行 SHALL 需用户显式确认

#### Scenario: C1 范围内的记忆能力

- GIVEN 用户与 Agent 对话
- WHEN Agent 组装上下文
- THEN 上下文 SHALL NOT 包含跨记录的历史检索结果
- AND 历史记录检索与跨记录关联 SHALL 留给后续独立 change

#### Scenario: C1 范围内的护栏深度

- GIVEN 最小护栏已内嵌
- WHEN 评估护栏防御深度
- THEN 后置输出过滤与违规降级机制 SHALL 留给后续独立 change
