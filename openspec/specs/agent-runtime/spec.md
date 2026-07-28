# Agent Runtime Spec / Agent 运行时规格

## Purpose / 目的

承载产品 Agent 的核心契约：对话 Runtime、阶段推进、护栏边界、失败语义与隐私约束。随 C1–C5 演进。

> 已接受来源：`openspec/changes/archive/2026-07-27-agent-runtime-mvp/`（C1，2026-07-27 用户验收）。
> C1 范围为 Runtime 基底 + 「写下此刻」多轮引导 + 最小护栏；Tool Calling / Memory / 系统化 hardening / 可观测分别留给 C2–C5。

## Requirements

### Requirement: Agent Runtime Must Be Passively Summoned

Agent SHALL 只在用户明确操作时参与对话。

#### Scenario: 用户主动开启对话

- GIVEN 一个已登录用户正在编辑草稿记录
- WHEN 用户主动点击对话入口
- THEN 后端 SHALL 开启或恢复该用户在该记录上的会话
- AND Agent SHALL 发出第一句引导

#### Scenario: 用户未主动触发

- GIVEN 用户进入记录编辑页但未点击对话入口
- WHEN 页面加载完成
- THEN Agent SHALL NOT 自动开启会话
- AND 系统 SHALL NOT 弹窗、推送或自动展开对话界面

### Requirement: Agent Runtime Must Support Multi-Turn Sessions With Explicit Stages

后端 SHALL 以显式阶段状态机维护多轮对话，阶段为 `OPENING`、`EMOTION`、`CONFUSION`、`CORE_QUESTION`、`EXPECTATION`、`CLOSING`、`ENDED`。

#### Scenario: 对话正常推进

- GIVEN 一个 `ACTIVE` 会话处于某个引导阶段
- WHEN 用户提交一轮回答
- THEN 后端 SHALL 持久化该用户消息与轮次序号
- AND 后端 SHALL 按状态机规则计算下一阶段
- AND 后端 SHALL 返回当前阶段、轮次与 Agent 回复

#### Scenario: 用户连续给出极短回答

- GIVEN 用户在同一阶段连续给出被判定为回避的极短回答
- WHEN 追问次数达到该阶段上限
- THEN 状态机 SHALL 前进到下一阶段或收束
- AND Agent SHALL NOT 在同一阶段继续逼问

#### Scenario: 用户表达结束意图

- GIVEN 一个 `ACTIVE` 会话
- WHEN 用户表达不想继续
- THEN 状态机 SHALL 进入 `CLOSING`
- AND Agent SHALL 以温和方式收束并保留已产生素材

#### Scenario: 达到会话轮次上限

- GIVEN 会话轮次达到配置上限
- WHEN 用户再次提交消息
- THEN 后端 SHALL 强制进入 `CLOSING`
- AND 后端 SHALL NOT 无限延长对话

#### Scenario: 会话已结束

- GIVEN 会话处于 `ENDED`
- WHEN 用户尝试追加消息
- THEN 后端 SHALL 拒绝该操作
- AND 用户 MAY 开启一个新会话

### Requirement: Agent Session Must Be Owner Scoped

会话与消息 SHALL 严格按用户隔离。

#### Scenario: 本人访问自己的会话

- GIVEN 一个已登录用户拥有某会话
- WHEN 该用户读取会话或追加消息
- THEN 后端 SHALL 允许该操作

#### Scenario: 跨用户访问会话

- GIVEN 某会话属于其他用户
- WHEN 一个已登录用户尝试读取或追加消息
- THEN 后端 SHALL 拒绝该操作或返回安全的未找到响应
- AND 后端 SHALL NOT 泄露该会话是否存在或其内容

#### Scenario: 未登录访问

- GIVEN 请求没有有效凭证
- WHEN 访问任一 Agent 会话端点
- THEN 后端 SHALL 返回未授权

### Requirement: Agent Must Not Modify User Content Or Records

Agent SHALL NOT 直接改写用户原文或触发记录写操作。

#### Scenario: 对话产生素材草稿

- GIVEN 会话进入 `CLOSING` 并整理出素材草稿
- WHEN 素材返回给前端
- THEN 素材 SHALL 仅作为候选内容呈现
- AND 只有用户显式确认后才 SHALL 通过既有记录更新接口写入正文

#### Scenario: 用户拒绝使用素材

- GIVEN 素材草稿已呈现
- WHEN 用户选择不使用
- THEN 记录正文 SHALL 保持不变
- AND 会话 SHALL 正常结束

#### Scenario: 用户在对话中请求重要操作

- GIVEN 用户在对话中要求封存、解锁或删除记录
- WHEN Agent 回应
- THEN Agent SHALL 只给出建议并引导用户自行确认
- AND Agent SHALL NOT 代为执行该操作

### Requirement: Agent Runtime Must Embed Minimum Guardrails

C1 SHALL 在 system prompt 中内嵌最小护栏，并在代码层强制回复长度上限。

#### Scenario: 用户描述疑似心理困扰

- GIVEN 用户输入包含疑似心理困扰的描述
- WHEN Agent 回应
- THEN Agent SHALL 以共情方式回应
- AND Agent SHALL NOT 给出心理诊断或医学建议

#### Scenario: Agent 回复过长

- GIVEN provider 返回的回复超过配置的长度上限
- WHEN 后端处理该回复
- THEN 后端 SHALL 将回复裁剪到上限内
- AND 返回内容 SHALL 保持语义完整可读

#### Scenario: 尝试诱导 Agent 改写原文

- GIVEN 用户或输入内容试图让 Agent 替换、修正用户原文
- WHEN Agent 回应
- THEN Agent SHALL 保留用户原文不变
- AND 引用用户表达时 SHALL 原样引用

### Requirement: Agent Runtime Must Fail Explicitly Without Fake Success

Agent 对话 SHALL 在 provider 不可用或失败时返回显式状态，不得伪装成功。

#### Scenario: provider 未配置

- GIVEN AI provider 的 base URL、model 或 API key 缺失
- WHEN 用户提交一轮消息
- THEN 后端 SHALL 返回显式不可用状态
- AND 后端 SHALL NOT 返回本地生成的回复冒充 Agent 真实回应

#### Scenario: provider 调用或解析失败

- GIVEN provider 调用超时、报错或返回内容无法解析
- WHEN 后端处理该轮
- THEN 后端 SHALL 返回显式失败状态
- AND 已提交的用户消息 SHALL 被保留
- AND 该轮 Agent 回复 SHALL NOT 被持久化
- AND 用户 MAY 对同一轮重试

#### Scenario: Agent 不可用时的记录生命周期

- GIVEN Agent 对话不可用或失败
- WHEN 用户保存草稿或封存记录
- THEN 记录保存与封存 SHALL 正常完成
- AND Agent 可用性 SHALL NOT 成为记录生命周期的依赖

### Requirement: Failed Turn Must Be Retryable Without Duplication

一轮回复失败后 SHALL 支持原样重试，且不产生重复数据或状态漂移。

#### Scenario: 用户以相同内容重试失败的一轮

- GIVEN 某轮用户消息已落库但该轮 Agent 回复失败
- WHEN 用户以相同内容重试该轮
- THEN 后端 SHALL NOT 重复持久化该用户消息
- AND 后端 SHALL NOT 再次推进阶段或增加轮次计数
- AND 成功后该轮 Agent 回复 SHALL 使用同一轮次序号

#### Scenario: 用户在未完成的一轮中改变内容

- GIVEN 某轮用户消息已落库但该轮 Agent 回复失败
- WHEN 用户提交与该轮不同的内容
- THEN 后端 SHALL 拒绝该请求并提示先重试原消息

#### Scenario: 恢复到未完成的一轮

- GIVEN 会话中最后一条用户消息尚无同轮 Agent 回复
- WHEN 用户读取或恢复该会话
- THEN 后端 SHALL 返回显式失败状态并提示需要重试
- AND 后端 SHALL NOT 将该会话报告为成功状态

### Requirement: Writing Guidance Must Target Draft Records Only

写作引导对话 SHALL 只作用于可编辑的草稿记录。

#### Scenario: 对话关联草稿记录

- GIVEN 一个已登录用户拥有某 DRAFT 记录
- WHEN 该用户以该记录开启对话
- THEN 后端 SHALL 允许开启会话

#### Scenario: 对话关联已封存或已解锁记录

- GIVEN 目标记录处于 SEALED 或 UNLOCKED
- WHEN 用户尝试以该记录开启写作引导对话
- THEN 后端 SHALL 拒绝该操作
- AND 封存后的不可变契约 SHALL 保持不变

### Requirement: Agent Conversation Content Must Stay In Business Storage Only

对话原文与用户日记原文 SHALL 只存在于业务存储中。

#### Scenario: 记录 Agent 运行日志

- GIVEN 后端记录一次 Agent 交互的运行日志
- WHEN 日志被写出
- THEN 日志 SHALL 只包含结构化元数据，例如会话标识、阶段、轮次、provider、耗时与异常类型
- AND 日志 SHALL NOT 包含对话原文或用户日记原文

#### Scenario: provider 凭证边界

- GIVEN AI provider 凭证存在
- WHEN 检查前端代码与 tracked files
- THEN 凭证 SHALL NOT 出现在其中
- AND 凭证 SHALL 只从 backend-side 配置读取

### Requirement: Agent Runtime MVP Must Exclude Tools Memory And Post Filtering

> C2 修订：本条款中的「工具调用留给后续独立 change」已由 C2 落实，工具边界改由下文
> 工具相关条款约束；此处保留为 C1 历史范围约束。

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

> C4 修订：本 scenario 原文「后置输出过滤与违规降级留给后续独立 change」已由 C4 落实，
> 改写为 C1 阶段范围声明 + 指向 C4 护栏条款。

- GIVEN 仅 C1 实现存在
- WHEN 评估护栏防御深度
- THEN 该阶段的护栏 SHALL 仅由 system prompt 约束与回复长度裁剪构成
- AND 后置输出检查与违规降级 SHALL 自 C4 起由本 spec 的护栏条款约束

## Accepted From C2 agent-tool-calling

> 来源：`openspec/changes/archive/2026-07-28-agent-tool-calling/`（C2，2026-07-28 用户验收）。
> C2 范围为原生 function calling 工具调用 + 代码级白名单 + 二段式用户确认；
> Memory / 系统化护栏 hardening / 决策链路可观测分别留给 C3 / C4 / C5。

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

> C4 修订：本 scenario 原文「系统化 hardening 留给后续独立 change」已由 C4 落实。

- GIVEN C2 的工具白名单已生效
- WHEN 评估内容合规防御深度
- THEN 该阶段 SHALL NOT 包含后置内容过滤或违规降级机制
- AND 工具参数内容的忠实度判定 SHALL 自 C4 起由本 spec 的忠实度条款约束

#### Scenario: C2 范围内的可观测能力

- GIVEN 工具审计数据已落库
- WHEN 评估可观测能力
- THEN C2 SHALL NOT 提供决策链路查询端点或可观测界面
- AND 决策链路可查询 SHALL 留给后续独立 change


## Accepted From C4 agent-guardrails-hardening

> 来源：openspec/changes/archive/2026-07-28-agent-guardrails-hardening/（C4，2026-07-28 用户验收）。
> C4 范围为忠实度判定 + 后置内容检查 + 违规降级 + 护栏规则可维护化；Memory 与决策链路可观测分别留给 C3 / C5。
> 上文 C1 / C2 的两条「护栏深度」scenario 已按 C4 修订。

### Requirement: Model Produced Text Entering User Content Must Pass A Deterministic Faithfulness Check

任何将进入用户记录正文的模型产出文本 SHALL 先通过确定性的来源忠实度判定；判定 SHALL NOT 依赖再一次模型调用。

#### Scenario: 候选文本增写了用户没有表达过的内容

- GIVEN 一个 `ACTIVE` 会话已绑定可编辑草稿记录
- WHEN 模型产出的候选文本中包含来源集合里没有对应来源的连续内容
- THEN 后端 SHALL 判定该候选文本不忠实
- AND 该候选文本 SHALL NOT 进入用户记录正文
- AND 该候选文本 SHALL NOT 作为待确认提议下发给前端

#### Scenario: 候选文本只是整理了用户已表达的内容

- GIVEN 候选文本对用户已表达内容做了语序调整、口头语删减或标点变化
- WHEN 后端执行忠实度判定
- THEN 后端 SHALL 判定该候选文本忠实
- AND 该候选文本 SHALL 正常进入既有的用户确认流程

#### Scenario: 判定依据

- GIVEN 后端执行忠实度判定
- WHEN 判定结果被计算
- THEN 判定 SHALL 同时考量候选文本被来源集合覆盖的整体比例
- AND 判定 SHALL 同时考量候选文本中最长的连续未被覆盖片段
- AND 单一的整体比例 SHALL NOT 作为唯一判据

#### Scenario: 判定的可复现性

- GIVEN 同一候选文本与同一来源集合
- WHEN 忠实度判定被重复执行
- THEN 判定结果 SHALL 保持一致
- AND 判定 SHALL NOT 产生对外部服务的调用

#### Scenario: 来源集合的边界

- GIVEN 后端构造忠实度判定的来源集合
- WHEN 来源集合被组装
- THEN 来源集合 SHALL 只包含当前会话中该用户发出的消息
- AND 来源集合 SHALL NOT 包含跨记录的历史检索结果
- AND 来源集合 SHALL NOT 包含 Agent 自己产出的表达

#### Scenario: 判定过程本身失败

- GIVEN 忠实度判定过程发生异常
- WHEN 后端处理该候选文本
- THEN 后端 SHALL 按不忠实处理
- AND 后端 SHALL NOT 因判定失败而放行未经判定的候选文本

### Requirement: Unfaithful Tool Proposals Must Be Rejected Without Breaking The Turn

不忠实的工具提议 SHALL 在落为待确认提议之前被拒绝，且 SHALL NOT 导致该轮对话失败。

#### Scenario: 工具正文参数不忠实

- GIVEN 模型在某轮返回了一个白名单内的工具提议
- WHEN 该提议的正文参数被判定不忠实
- THEN 后端 SHALL 拒绝该提议
- AND 后端 SHALL 以结构化审计记录该次拒绝及其原因
- AND 该轮 Agent 自然语言回复 SHALL 正常返回
- AND 前端 SHALL NOT 收到待确认提议

#### Scenario: 忠实度校验的位置

- GIVEN 后端校验一条工具提议
- WHEN 校验流程执行
- THEN 忠实度判定 SHALL 与白名单、参数类型和业务边界校验处于同一校验环节
- AND 后端 SHALL NOT 存在跳过忠实度判定即可产生待确认提议的路径

#### Scenario: 提议话术中的伪引用

- GIVEN 提议话术中包含引号包裹的、声称来自用户的片段
- WHEN 该片段在来源集合中没有对应来源
- THEN 后端 SHALL 拒绝该提议
- AND 后端 SHALL NOT 在确认入口上展示该片段

### Requirement: Unfaithful Material Draft Must Not Be Offered To The User

不忠实的素材草稿 SHALL NOT 呈现给用户作为可回填正文的候选。

#### Scenario: 素材草稿增写了用户没有表达过的内容

- GIVEN 会话进入收束并产出素材草稿
- WHEN 该素材草稿被判定不忠实
- THEN 后端 SHALL NOT 返回该素材草稿
- AND 会话 SHALL 正常结束
- AND 用户 SHALL NOT 看到该素材的回填入口

#### Scenario: 素材缺失时的会话生命周期

- GIVEN 素材草稿因不忠实而被丢弃
- WHEN 用户查看会话结果
- THEN 会话状态 SHALL 与素材生成失败时保持一致的语义
- AND 记录正文 SHALL 保持不变

### Requirement: Agent Replies Must Pass Post Generation Content Checks With Downgrade

Agent 回复 SHALL 在返回前经过后置内容检查；检出越界时 SHALL 降级为安全兜底回复。

#### Scenario: Agent 在自己新增的表述中给出诊断

- GIVEN 模型回复中由 Agent 新增的表述包含诊断性判断或医学建议
- WHEN 后端处理该回复
- THEN 后端 SHALL 以安全兜底回复替换该回复
- AND 后端 SHALL 以结构化痕迹记录该次降级

#### Scenario: 用户自己提到病症词而 Agent 共情复述

- GIVEN 用户在自己的表达中使用了某个病症词
- WHEN Agent 回复中该词出现在有来源的区段内
- THEN 后端 SHALL NOT 判定该回复越界
- AND Agent 的共情回应 SHALL 正常返回

#### Scenario: Agent 谎报已代替用户执行不可逆操作

- GIVEN Agent 回复声称已经完成封存、解锁或删除
- WHEN 后端处理该回复
- THEN 后端 SHALL 以安全兜底回复替换该回复
- AND 记录 SHALL 保持不变

#### Scenario: 兜底回复不得冒充模型正常输出

- GIVEN 某轮回复被降级为安全兜底回复
- WHEN 检查该轮的结构化痕迹
- THEN 痕迹 SHALL 可区分该回复来自本地兜底而非 provider 正常产出
- AND 后端 SHALL NOT 将降级报告为一次正常成功的模型回复

#### Scenario: 长度硬上限在多层叠加后仍生效

- GIVEN 后置内容检查已启用
- WHEN 回复经过检查与降级处理
- THEN 回复长度上限 SHALL 仍然生效
- AND 既有的长度裁剪行为 SHALL NOT 被削弱

### Requirement: Guardrail Layers Must Be Additive And Declared In One Source

护栏 SHALL 以叠加方式存在，且规则 SHALL 收敛到单一声明源。

#### Scenario: 新增后置检查后的既有护栏

- GIVEN 后置内容检查与忠实度判定已引入
- WHEN 审查护栏体系
- THEN system prompt 级护栏、工具白名单、二段式用户确认与回复长度上限 SHALL 全部保持原有效力
- AND 后端 SHALL NOT 因引入后置检查而放宽其中任何一层

#### Scenario: 护栏规则的声明位置

- GIVEN 护栏规则同时用于 system prompt 文案与后置检查
- WHEN 审查规则来源
- THEN 两者 SHALL 派生自同一份声明
- AND 护栏规则 SHALL NOT 分散在多处彼此独立维护

#### Scenario: 护栏规则包含正向行为

- GIVEN 护栏规则被声明
- WHEN 审查规则内容
- THEN 规则 SHALL 同时表达 Agent 可以做什么
- AND 规则 SHALL NOT 仅由禁止清单构成

#### Scenario: 护栏阈值的可配置性

- GIVEN 忠实度判定与后置检查存在阈值
- WHEN 阈值被配置
- THEN 阈值 SHALL 来自 backend-side 配置
- AND 配置 SHALL NOT 引入任何新的凭证字段

### Requirement: Guardrail Traces Must Exclude Diary And Conversation Content

护栏判定与降级 SHALL 留下结构化痕迹，且 SHALL NOT 包含用户日记原文或对话原文。

#### Scenario: 忠实度判定留痕

- GIVEN 一次忠实度判定完成
- WHEN 痕迹被写出
- THEN 痕迹 SHALL 只包含结构化指标与判定结论，例如覆盖比例、最长未覆盖片段长度、违规类型
- AND 痕迹 SHALL NOT 包含候选文本、用户原话或未覆盖片段的内容

#### Scenario: 来源集合的生命周期

- GIVEN 后端为判定构造了来源集合
- WHEN 判定结束
- THEN 来源集合 SHALL NOT 被持久化
- AND 来源集合 SHALL NOT 被写入日志或外发

#### Scenario: 降级痕迹的可见性边界

- GIVEN 某次越界被检出并降级
- WHEN 结果返回给前端
- THEN 用户 SHALL NOT 被告知护栏内部判定过程
- AND 开发者可见的痕迹 SHALL 只存在于后端结构化记录中

### Requirement: Guardrail Boundary Cases Must Be Covered By A Regression Suite

护栏 SHALL 具备可持续回归的边界用例集。

#### Scenario: 边界用例覆盖范围

- GIVEN C4 实现完成
- WHEN 审查测试资产
- THEN 边界用例集 SHALL 覆盖诱导诊断的输入、试图让 Agent 改写用户原文的注入型输入、过长输出与试图让 Agent 代替用户决策的输入
- AND 边界用例集 SHALL 包含已观测到的增写用户原话的真实样本回归

#### Scenario: 回归用例的执行条件

- GIVEN 边界用例集存在
- WHEN 用例被执行
- THEN 用例 SHALL 可在不调用真实 provider 的情况下运行

### Requirement: Guardrails Hardening Must Exclude Memory Observability Queries And Guidance Tuning

C4 SHALL 限定在忠实度判定、后置内容检查、违规降级与护栏可维护化范围内。

#### Scenario: C4 范围内的记忆能力

- GIVEN 用户与 Agent 对话
- WHEN 护栏执行判定
- THEN 判定所用的来源 SHALL 只来自当前会话
- AND 历史记录检索与跨记录关联 SHALL 留给后续独立 change

#### Scenario: C4 范围内的可观测能力

- GIVEN 护栏痕迹已落地
- WHEN 评估可观测能力
- THEN C4 SHALL NOT 提供决策链路查询端点或可观测界面
- AND 决策链路可查询 SHALL 留给后续独立 change

#### Scenario: C4 范围内的引导与素材质量

- GIVEN 护栏已系统化
- WHEN 评估引导话术与素材合成质量
- THEN C4 SHALL NOT 修改引导阶段的提问策略
- AND C4 SHALL NOT 修改素材合成策略
- AND 质量优化 SHALL 留给后续独立安排

#### Scenario: C4 范围内的工具白名单

- GIVEN 护栏加固完成
- WHEN 审查 Agent 可达的后端写操作
- THEN 可达范围 SHALL NOT 超出 C2 已接受的白名单
- AND C4 SHALL NOT 新增任何工具
