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

后端 SHALL 以显式状态维护多轮对话。P4.1 之后，新 WRITING_GUIDANCE 会话 SHALL 使用 `WITNESS`、`CLOSING`、`ENDED`；REVIEW_CHAT SHALL 继续使用 `REVIEW`、`ENDED`。`OPENING`、`EMOTION`、`CONFUSION`、`CORE_QUESTION`、`EXPECTATION` SHALL 只为历史 session/message/trace 读取兼容保留，生产路径 SHALL NOT 再产生固定四阶段推进。

#### Scenario: 写作对话正常推进

- GIVEN 一个 `ACTIVE` WRITING_GUIDANCE 会话处于 `WITNESS`
- WHEN 用户提交一轮未表达结束意图的回答
- THEN 后端 SHALL 持久化该用户消息与轮次序号
- AND 后端 SHALL 按 conversation intent 计算本轮 turn policy 与问题上限
- AND 会话 SHALL 保持 `WITNESS`，不得推进到情绪、困惑、核心问题或期待阶段

#### Scenario: 用户给出极短回答

- GIVEN 用户在 WRITING_GUIDANCE 中提交去空白长度不超过 4 的回答
- WHEN 后端计算本轮策略
- THEN 策略 SHALL 为 `REFLECT_ONLY` 且问题上限为 0
- AND 后端 SHALL NOT 把该回答视为需要同阶段再问一次的回避
- AND `stage_reask_count` SHALL 保持为 0

#### Scenario: 用户表达结束意图

- GIVEN 一个 `ACTIVE` 会话
- WHEN 用户表达不想继续
- THEN 会话 SHALL 进入 `CLOSING`
- AND Agent SHALL 以温和方式收束并保留已产生素材
- AND Agent SHALL NOT 提出新问题或挽留用户

#### Scenario: 达到会话轮次上限

- GIVEN 会话轮次达到配置上限
- WHEN 用户再次提交消息
- THEN 后端 SHALL 强制进入 `CLOSING`
- AND 后端 SHALL NOT 无限延长对话
- AND 收束回复的问题上限 SHALL 为 0

#### Scenario: 会话已结束

- GIVEN 会话处于 `ENDED`
- WHEN 用户尝试追加消息
- THEN 后端 SHALL 拒绝该操作
- AND 用户 MAY 开启一个新会话

#### Scenario: 读取历史阶段事实

- GIVEN 历史 message 或 trace 使用旧写作引导阶段值
- WHEN P4.1 后端读取该历史事实
- THEN 后端 SHALL 保持可解析
- AND 后端 SHALL NOT 回写或伪造其历史 stage
- AND 新对话 SHALL NOT 因历史值恢复固定阶段序列

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

### Requirement: Writing Guidance Must Target Editable Records Only

写作引导对话 SHALL 只作用于用户拥有的 active DRAFT 或 SAVED 记录。SEALED / UNLOCKED SHALL NOT 进入写作引导；UNLOCKED 继续使用独立 REVIEW_CHAT。

#### Scenario: 对话关联 active DRAFT

- GIVEN 一个已登录用户拥有未过期的 DRAFT
- WHEN 该用户以该记录开启写作引导
- THEN 后端 SHALL 允许开启会话
- AND 写作引导产生的可逆修改 SHALL 保持记录为 DRAFT，直到用户显式保存

#### Scenario: 对话关联 SAVED

- GIVEN 一个已登录用户拥有 SAVED 记录
- WHEN 该用户以该记录开启写作引导
- THEN 后端 SHALL 允许开启会话
- AND 用户显式确认的正文/标签修改 SHALL 保持记录为 SAVED
- AND 修改后 SHALL 继续满足 P3.1 保存成立条件

#### Scenario: 写作引导关联已封存或已解锁记录

- GIVEN 目标记录处于 SEALED 或 UNLOCKED
- WHEN 用户尝试以该记录开启写作引导
- THEN 后端 SHALL 拒绝该操作
- AND 封存后的不可变契约 SHALL 保持不变

#### Scenario: 回看对话的记录状态要求

- GIVEN 目标记录处于 DRAFT、SAVED 或 SEALED
- WHEN 用户尝试以该记录开启 REVIEW_CHAT
- THEN 后端 SHALL 拒绝该操作
- AND 尚未解锁的记录内容 SHALL NOT 经由回看对话被提前读到

#### Scenario: 过期技术草稿

- GIVEN 目标记录处于 DRAFT 但 recovery expiry 已到
- WHEN 用户尝试开启或恢复写作引导
- THEN 后端 SHALL 拒绝或返回安全的不可恢复结果
- AND SHALL NOT 通过 Agent 旁路复活过期草稿

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

> C3 修订：本 scenario 原文「历史记录检索与跨记录关联 SHALL 留给后续独立 change」
> 已由 C3a `agent-memory-retrieval` 落实，改写为 C1 阶段范围声明 + 指向 C3 记忆条款。

- GIVEN 仅 C1 实现存在
- WHEN Agent 组装上下文
- THEN 该阶段的上下文 SHALL NOT 包含跨记录的历史检索结果
- AND 跨记录检索与注入 SHALL 自 C3 起由本 spec 的记忆条款约束

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

### Requirement: Agent Tool Execution Must Reuse Editable Record Business Paths

工具执行 SHALL 复用 RecordService 的 owner、active/editable state 与 P3.1 eligibility 权威，SHALL NOT 存在仅 Agent 可用的保存、封存或状态绕过。

#### Scenario: 目标记录仍为 active DRAFT

- GIVEN 会话绑定的记录处于未过期 DRAFT
- WHEN 用户确认执行允许的可逆写工具
- THEN 后端 SHALL 通过既有记录业务路径完成写入
- AND SHALL 刷新恢复期限
- AND SHALL NOT 自动将记录转为 SAVED

#### Scenario: 目标记录为 SAVED

- GIVEN 会话绑定的记录处于 SAVED
- WHEN 用户确认执行允许的可逆写工具
- THEN 后端 SHALL 通过既有记录业务路径完成写入
- AND 记录 SHALL 保持 SAVED
- AND 执行后 SHALL 继续满足文字或 AVAILABLE 媒体成立条件

#### Scenario: 目标记录已封存或已解锁

- GIVEN 会话绑定的记录处于 SEALED 或 UNLOCKED
- WHEN 用户确认执行写工具
- THEN 后端 SHALL 拒绝该操作并返回显式失败
- AND SHALL NOT 修改记录、位置、附件、封面或标签

#### Scenario: Agent 不拥有生命周期命令

- GIVEN P3.1 引入 save 与 SAVED -> SEALED 状态迁移
- WHEN 审查 Agent tool registry 与执行器
- THEN Agent SHALL NOT 获得 save、seal、delete、attachment、location 或 cover 工具
- AND 用户 SHALL 亲自完成保存与封存决定

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

> C3 修订：本 scenario 原文「历史记录检索与跨记录关联 SHALL 留给后续独立 change」
> 已由 C3a 落实。注意工具**正文参数**的来源约束并未放宽——
> 自 C3 起进入用户正文的文本仍只认当前会话层，见下文「进入用户正文的文本的来源层」。

- GIVEN C2 的工具白名单已生效
- WHEN Agent 组装上下文或构造工具参数
- THEN 工具参数的来源 SHALL 只来自当前会话、当前草稿与本会话的工具执行结果
- AND 跨记录检索结果 SHALL NOT 成为工具正文参数的合法来源
- AND 上下文中的跨记录检索 SHALL 自 C3 起由本 spec 的记忆条款约束

#### Scenario: C2 范围内的护栏深度

> C4 修订：本 scenario 原文「系统化 hardening 留给后续独立 change」已由 C4 落实。

- GIVEN C2 的工具白名单已生效
- WHEN 评估内容合规防御深度
- THEN 该阶段 SHALL NOT 包含后置内容过滤或违规降级机制
- AND 工具参数内容的忠实度判定 SHALL 自 C4 起由本 spec 的忠实度条款约束

#### Scenario: C2 范围内的可观测能力

> C5 修订：原文「决策链路可查询 SHALL 留给后续独立 change」已由 C5 落实。

- GIVEN 工具审计数据已落库
- WHEN 评估可观测能力
- THEN C2 SHALL NOT 提供决策链路查询端点或可观测界面
- AND 决策链路可查询 SHALL 自 C5 起由本 spec 的决策轨迹条款约束


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

> C3 修订（实质改写）：C4 原文禁止来源集合包含跨记录检索结果。
> 自 C3a 起来源分层，且两层权限不对等——记忆层只能支撑对话中的复述，
> 不能作为进入用户正文的依据。更严的正文约束见下一条 scenario。

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

> C3 修订：护栏来源集合自 C3a 起分层，见上文「来源集合的边界」。

- GIVEN 仅 C4 实现存在
- WHEN 护栏执行判定
- THEN 该阶段判定所用的来源 SHALL 只来自当前会话
- AND 来源集合的分层 SHALL 自 C3 起由本 spec 的来源边界条款约束

#### Scenario: C4 范围内的可观测能力

> C5 修订：同上，已由 C5 落实。

- GIVEN 护栏痕迹已落地
- WHEN 评估可观测能力
- THEN C4 SHALL NOT 提供决策链路查询端点或可观测界面
- AND 决策链路可查询 SHALL 自 C5 起由本 spec 的决策轨迹条款约束

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


## Accepted From C3a agent-memory-retrieval

> 来源：`openspec/changes/archive/2026-07-29-agent-memory-retrieval/`（C3 前半刀，2026-07-29 用户验收）。
> C3 依蓝图 §4 拆为两刀：本段为前半刀，范围是历史记录检索 + 写作引导注入 + 分层来源 + 时间归属护栏；
> 友人回看多轮对话留给后半刀 `agent-review-chat`，决策链路可观测留给 C5。
> 上文 C1 / C2 / C4 三条「范围内的记忆能力」与 C4「来源集合的边界」已按 C3 修订。
> **闸门 3 未执行**：时间归属阈值未经真实样本校准（残余风险 R8），归档时用户明确同意延后至 C3 两刀全部完成后合并进行。

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

#### Scenario: 检索线索缺失

- GIVEN 本轮没有可用的检索线索
- WHEN 后端组装上下文
- THEN 后端 SHALL NOT 发起检索
- AND 后端 SHALL NOT 退化为按时间顺序返回最近的记录

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

#### Scenario: 检索排除当前会话绑定的记录

- GIVEN 会话已绑定某条记录
- WHEN 后端为该会话检索历史记录
- THEN 该记录本身 SHALL NOT 出现在检索结果中

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
- AND 拒绝原因 SHALL 可与「模型虚构内容」区分
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

#### Scenario: 措辞巧合不触发时间归属要求

- GIVEN 仅被记忆层覆盖的连续片段短于配置阈值
- WHEN 后端执行时间归属判定
- THEN 后端 SHALL 放行该回复

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

#### Scenario: 带距离感地引用记忆

- GIVEN Agent 回复引用了一条带 `occurredAt` 的记忆片段
- WHEN backend 组装时间上下文
- THEN SHALL 保留该片段既有月份级 `timeLabel`
- AND SHALL 以确定性的距离层级补充“最近、数月前或更早”的语义
- AND SHALL NOT 让相对距离替代或模糊准确的来源锚点

#### Scenario: 时间未知或位于未来

- GIVEN 记忆片段缺少发生时间或发生时间晚于当前 `Clock`
- WHEN backend 计算时间距离
- THEN 距离层级 SHALL 为 `UNKNOWN`
- AND Agent SHALL NOT 获得虚假的天数、月份或“已经过去多久”结论
- AND 既有“以前/那时候”安全归属仍可使用

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

### Requirement: Agent Session Must Declare Its Purpose

Agent 会话 SHALL 携带用途标识，以便不同场景复用同一会话模型。

#### Scenario: 写作引导会话的用途

- GIVEN 写作引导会话被创建
- WHEN 会话被持久化
- THEN 会话 SHALL 标记为写作引导用途

#### Scenario: 用途标识的向后兼容

- GIVEN 存在引入用途标识之前创建的会话
- WHEN 后端读取这些会话
- THEN 后端 SHALL 将其视为写作引导用途
- AND 既有会话的读写行为 SHALL 保持不变

### Requirement: Memory Retrieval Must Exclude Review Chat And Observability Queries

C3 前半刀 SHALL 限定在检索、注入、分层来源与时间归属护栏范围内。

#### Scenario: C3a 范围内的回看对话

> C3b 修订：本 scenario 原文「回看多轮对话 SHALL 留给 C3 后半刀」已由 C3b 落实，
> 改写为 C3a 阶段范围声明 + 指向本 spec 的回看条款。

- GIVEN 仅 C3 前半刀实现存在
- WHEN 审查会话用途
- THEN 该阶段的后端 SHALL NOT 提供作用于已解锁记录的对话行为
- AND 写作引导对话 SHALL 仍只作用于可编辑的草稿记录
- AND 回看对话 SHALL 自 C3 后半刀起由本 spec 的回看条款约束

#### Scenario: C3a 范围内的可观测能力

> C5 修订：同上，已由 C5 落实。

- GIVEN 记忆检索与判定痕迹已落地
- WHEN 评估可观测能力
- THEN C3 前半刀 SHALL NOT 提供决策链路查询端点或可观测界面
- AND 决策链路可查询 SHALL 自 C5 起由本 spec 的决策轨迹条款约束

#### Scenario: C3a 范围内的引导与素材质量

- GIVEN 记忆已注入上下文
- WHEN 评估引导话术与素材合成质量
- THEN C3 前半刀 SHALL NOT 修改引导阶段的提问策略
- AND C3 前半刀 SHALL NOT 修改素材合成策略

#### Scenario: C3a 范围内的工具白名单

- GIVEN 记忆能力已落地
- WHEN 审查 Agent 可达的后端写操作
- THEN 可达范围 SHALL NOT 超出 C2 已接受的白名单
- AND C3 前半刀 SHALL NOT 新增任何工具

#### Scenario: 记忆对用户的可见性

- GIVEN 某一轮对话注入了记忆片段
- WHEN 结果返回给前端
- THEN 用户 SHALL NOT 被告知本轮是否检索或注入了历史记录
- AND 前端 SHALL NOT 收到命中的记录标识列表


## Accepted From C3b agent-review-chat

> 来源：`openspec/changes/archive/2026-07-29-agent-review-chat/`（C3 后半刀，2026-07-29 用户验收）。
> 范围：友人回看对话——会话模式、无阶段轮次、完全无工具、不产素材、被回看内容的来源层。
> 上文「写作引导对话关联已封存或已解锁记录」与「C3a 范围内的回看对话」两条已按 C3b 修订。
> **C3 两刀至此全部完成**；决策链路可观测留给 C5。
>
> 闸门 3 已执行（与 C3a 合并）：真实 provider 15 次调用 + 微信真机手验。
> 时间归属护栏的**误伤方向 0 次、拦截方向已活体验证**（同一句真实模型回复剥离时间指示语后
> 由放行翻转为违规），R8 关闭；回看的 tool_calls fail-closed 分支**未被真实触发**，仅单测覆盖。

### Requirement: Agent Conversations Must Declare A Mode Derived From Session Purpose

对话行为的差异 SHALL 由会话用途派生的模式统一决定，SHALL NOT 分散判定。

#### Scenario: 模式决定的行为维度

- GIVEN 一个 Agent 会话
- WHEN 后端编排该会话的一轮对话
- THEN 模式 SHALL 决定目标记录要求的状态、是否由阶段机推进、是否下发工具、是否产出素材
- AND 后端 SHALL NOT 在这些维度上各自独立判断会话用途

#### Scenario: 用途缺省

- GIVEN 开启会话的请求未指定用途
- WHEN 后端处理该请求
- THEN 会话 SHALL 被视为写作引导用途
- AND 既有调用方 SHALL NOT 需要修改

#### Scenario: 会话恢复的用途隔离

- GIVEN 同一条记录上存在不同用途的进行中会话
- WHEN 用户以某一用途开启或恢复会话
- THEN 后端 SHALL 只恢复该用途下的会话
- AND 后端 SHALL NOT 返回其他用途的会话

### Requirement: Review Chat Must Support Passive Multi Turn Conversation On Unlocked Records

回看对话 SHALL 在已解锁记录上提供被动召唤的多轮对话。

#### Scenario: 用户主动开启回看对话

- GIVEN 一个已登录用户拥有某 UNLOCKED 记录
- WHEN 该用户主动开启回看对话
- THEN 后端 SHALL 开启或恢复该用户在该记录上的回看会话
- AND Agent SHALL 发出第一句

#### Scenario: 回看对话必须绑定记录

- GIVEN 开启回看对话的请求未指定记录
- WHEN 后端处理该请求
- THEN 后端 SHALL 拒绝该操作

#### Scenario: 用户未主动触发

- GIVEN 用户进入已解锁记录页面但未点击回看对话入口
- WHEN 页面加载完成
- THEN Agent SHALL NOT 自动开启会话
- AND 系统 SHALL NOT 弹窗、推送或自动展开对话界面

#### Scenario: 跨用户访问回看会话

- GIVEN 目标记录或会话属于其他用户
- WHEN 一个已登录用户尝试开启、读取或追加消息
- THEN 后端 SHALL 拒绝该操作或返回安全的未找到响应

### Requirement: Review Chat Must Not Be Driven By The Writing Guidance Stage Machine

回看对话 SHALL NOT 经写作引导阶段机推进。

#### Scenario: 回看对话的阶段

- GIVEN 一个进行中的回看会话
- WHEN 用户提交若干轮消息
- THEN 会话阶段 SHALL 保持为回看专用的固定阶段
- AND 阶段追问计数 SHALL NOT 被回看逻辑改写
- AND Agent SHALL NOT 按引导阶段顺序推进话题

#### Scenario: 回看对话的轮次上限

- GIVEN 回看会话存在独立的轮次上限配置
- WHEN 轮次达到该上限
- THEN 后端 SHALL 温和收束并结束会话
- AND 后端 SHALL NOT 无限延长对话

#### Scenario: 用户主动结束回看对话

- GIVEN 一个进行中的回看会话
- WHEN 用户主动结束
- THEN 会话 SHALL 进入已结束状态
- AND 已结束后追加消息 SHALL 被拒绝

#### Scenario: 阶段机对回看阶段的处理

- GIVEN 回看专用阶段被误传入写作引导阶段机
- WHEN 阶段机计算推进
- THEN 阶段机 SHALL 拒绝该输入
- AND 阶段机 SHALL NOT 将其静默按引导阶段处理

### Requirement: Review Chat Must Have No Tools And No Material

回看对话 SHALL NOT 具备任何工具能力，且 SHALL NOT 产出可回填正文的素材。

#### Scenario: 回看对话不下发工具

- GIVEN 一个回看会话已绑定记录
- WHEN 后端向 provider 发起该轮对话请求
- THEN 请求 SHALL NOT 携带任何工具定义
- AND 该判定 SHALL 由会话模式给出，SHALL NOT 依赖会话是否绑定记录

#### Scenario: 模型在回看中返回工具提议

- GIVEN 回看对话未下发工具定义
- WHEN provider 仍返回工具调用
- THEN 后端 SHALL 丢弃该提议
- AND 后端 SHALL NOT 持久化待确认提议
- AND 前端 SHALL NOT 收到确认入口
- AND 该轮 Agent 回复 SHALL 正常返回

#### Scenario: 回看对话不产出素材

- GIVEN 一个回看会话正常进行或结束
- WHEN 结果返回给前端
- THEN 素材草稿 SHALL 为空
- AND 用户 SHALL NOT 看到回填正文的入口

#### Scenario: 回看对话结束后的记录

- GIVEN 一次回看对话完整进行并结束
- WHEN 检查目标记录
- THEN 记录正文 SHALL 逐字保持不变
- AND 位置、附件与封面 SHALL 保持不变
- AND 记录状态 SHALL 保持为已解锁

#### Scenario: 用户在回看中要求修改或删除

- GIVEN 用户在回看对话中要求改写正文或删除记录
- WHEN Agent 回应
- THEN Agent SHALL 只说明并引导用户自行处理
- AND Agent SHALL NOT 谎称已完成该操作
- AND 记录 SHALL 保持不变

### Requirement: Reviewed Record Content Must Belong To The Memory Source Layer

被回看记录自身的内容 SHALL 属于记忆层，SHALL NOT 属于当前会话层。

#### Scenario: 被回看记录内容的来源层

- GIVEN 回看对话注入了被回看记录的内容
- WHEN 分层来源集合被组装
- THEN 该内容 SHALL 进入记忆层
- AND 该内容 SHALL NOT 进入当前会话层

#### Scenario: 复述被回看记录的内容

- GIVEN Agent 回复中复述了被回看记录的内容
- WHEN 该回复不包含指明时间的表述
- THEN 后端 SHALL 以安全兜底回复替换该回复

#### Scenario: 注入字段的时间语义一致性

- GIVEN 后端为回看对话选取可注入的记录字段
- WHEN 字段被选取
- THEN 注入内容 SHALL 只包含封存时刻的表达与当时的整理
- AND 解锁之后写下的内容 SHALL NOT 与之混入同一来源层

#### Scenario: 回看复用同一记忆检索能力

- GIVEN 回看对话需要关联该用户的其他历史记录
- WHEN 后端检索历史
- THEN 后端 SHALL 复用既有的记忆检索接口
- AND 后端 SHALL NOT 另建一套检索实现

### Requirement: Review Chat Must Inherit All Existing Guardrails

回看对话 SHALL 完整继承既有护栏，SHALL NOT 放宽任何一层。

#### Scenario: 护栏层级

- GIVEN 回看对话已启用
- WHEN 审查护栏
- THEN 忠实度判定、诊断检查、代决检查、伪引用严判、时间归属检查与回复长度上限 SHALL 全部生效
- AND 各层阈值 SHALL 保持与引入回看之前一致

#### Scenario: 回看对话的失败语义

- GIVEN provider 不可用或调用失败
- WHEN 用户提交一轮消息
- THEN 后端 SHALL 返回显式不可用或失败状态
- AND 已提交的用户消息 SHALL 被保留
- AND 该轮 Agent 回复 SHALL NOT 被持久化
- AND 用户 MAY 对同一轮重试

#### Scenario: 回看对话的日志边界

- GIVEN 后端记录回看对话的运行日志
- WHEN 日志被写出
- THEN 日志 SHALL 只包含结构化元数据
- AND 日志 SHALL NOT 包含对话原文或记录原文

### Requirement: Review Chat Must Exclude Observability Queries And Guidance Tuning

C3 后半刀 SHALL 限定在回看对话范围内。

#### Scenario: C3b 范围内的可观测能力

> C5 修订：同上，已由 C5 落实。

- GIVEN 回看对话已落地
- WHEN 评估可观测能力
- THEN C3 后半刀 SHALL NOT 提供决策链路查询端点或可观测界面
- AND 决策链路可查询 SHALL 自 C5 起由本 spec 的决策轨迹条款约束

#### Scenario: C3b 范围内的引导与素材质量

- GIVEN 回看对话已落地
- WHEN 评估写作引导的提问策略与素材合成策略
- THEN C3 后半刀 SHALL NOT 修改它们

#### Scenario: C3b 范围内的检索能力

- GIVEN 回看复用记忆检索
- WHEN 审查检索实现
- THEN C3 后半刀 SHALL NOT 修改检索的字段范围、权重或索引结构

#### Scenario: C3b 范围内的工具白名单

- GIVEN 回看对话已落地
- WHEN 审查 Agent 可达的后端写操作
- THEN 可达范围 SHALL NOT 超出既有白名单
- AND C3 后半刀 SHALL NOT 新增任何工具

## Accepted From C5 agent-observability

> 来源：`openspec/changes/archive/2026-07-30-agent-observability/`（C5，2026-07-30 用户验收）。
> 范围：Agent 决策轨迹（thought → action → observation）的采集、隐私、查询与配置。
> **本刀不改任何对话行为**——阶段推进、提示词、护栏阈值、记忆检索、工具白名单、回看逻辑均未修改。
> 上文 C2 / C4 / C3a / C3b 四条「范围内的可观测能力」scenario 已按 C5 修订。
> **Phase 1（C1–C5）至此全部完成。**
>
> 闸门 3 已执行：真实 provider 6 次调用（预算 10）。轨迹三段齐备、耗时量级合理、
> 真实产出下隐私复核通过；**回看 fail-closed 仍未活体触发，如实记为未验证**。

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

> C6 修订：原文「评估能力 SHALL 留给后续独立 change」已由 C6 `agent-eval-framework` 落实。
> 保留阶段范围声明而非删除——范围声明本身是历史事实，删掉就看不出能力何时到位
> （沿用 C5 修订 C2 / C4 / C3a / C3b 四条时的同一做法）。

- GIVEN 轨迹已携带版本锚点
- WHEN 评估回归能力
- THEN C5 SHALL NOT 提供黄金集、评估运行器或质量评分
- AND 可回归的评测能力 SHALL 自 C6 起由本 spec 的评测条款约束
- AND 绝对质量评分 SHALL 仍然不被提供

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

---

## Accepted From C6 agent-eval-framework

> 来源：`openspec/changes/archive/2026-07-31-agent-eval-framework/`（C6，2026-07-31 用户验收）。
> 范围：Agent 编排行为的可回归性——不变量断言、快照回归比对、评测的隐私与诚实边界。
> **本刀不改任何 Agent 行为，且 `src/main` 零改动**——护栏阈值、提示词、阶段推进、
> 记忆检索、工具白名单、回看逻辑、轨迹采集点均未修改。
> 上文 C5「范围内的评估能力」scenario 已按 C6 修订。
> **Phase 2 第一刀。**
>
> **闸门 3：未申请**（外调预算 0，全程未启用任何真实 provider 探针）。
> 因此以下三项如实记为未完成，**不得在后续文档中写成已完成**：
> ① 快照指标在真实 provider 下的稳定性**未验证**；
> ② 话术质量的人评锚点**为空**（结构已就位，建议顺带在 C7 闸门 3 填）；
> ③ 仓库**无 CI**，交付的是「一条 maven 命令可跑」，**不是** CI 门槛。
>
> 验证基线：后端 **606 tests PASS / 4 skipped**（536 基线 + 70 新增，零回归，既有断言零修改）。

### Requirement: Orchestration Invariants Must Be Asserted Offline

Agent 编排行为的不变量 SHALL 由可离线执行的评测断言，SHALL NOT 依赖人工阅读回复判断。

#### Scenario: 评测的执行条件

- GIVEN 评测资产存在
- WHEN 评测被执行
- THEN 评测 SHALL 在不调用真实 provider 的情况下运行
- AND 评测 SHALL NOT 依赖真实数据库或外部服务
- AND 评测 SHALL 随后端既有测试的默认执行路径一并运行

#### Scenario: 断言对象

- GIVEN 一轮对话已由评测驱动完成
- WHEN 评测做出判定
- THEN 评测 SHALL 断言该轮的决策轨迹信号
- AND 评测 SHALL NOT 以回复文本的措辞作为通过条件

#### Scenario: 阶段推进的不变量

- GIVEN 写作引导的一组用例被执行
- WHEN 检查阶段判定序列
- THEN 阶段的来源、去向与判定结论序列 SHALL 合法
- AND 同一阶段的追问 SHALL NOT 超过既有实现约定的上限
- AND 轮次上限到达的收束 SHALL 可被触发

#### Scenario: 记忆注入的不变量

- GIVEN 记忆检索与注入已发生
- WHEN 检查注入规模
- THEN 注入的片段数 SHALL NOT 超过配置的片段上限
- AND 注入的总长度 SHALL NOT 超过由片段上限与单片段长度上限派生的界
- AND 该派生界 SHALL 被如实标注为派生值，SHALL NOT 被表述为一个已存在的配置项

#### Scenario: 记忆三态的不变量

> 实现期实测印证了这条为何必须落在不变量层：「检索成功但无命中」与
> 「能力被关闭」两种情形的**快照指标完全相同**，区别只存在于轨迹的检索状态字段上。
> 仅靠快照回归比对无法区分它们。

- GIVEN 某一轮没有产生记忆片段
- WHEN 评测检查该轮
- THEN 「能力被关闭」「检索失败」「检索成功但无命中」三种情形 SHALL 可被区分
- AND 评测 SHALL NOT 将它们视为同一种通过条件
- AND 该区分 SHALL 由不变量断言承担，SHALL NOT 仅依赖快照比对

#### Scenario: 输出克制的不变量

- GIVEN 某轮回复已产出
- WHEN 检查长度约束
- THEN 回复长度 SHALL NOT 超过配置的回复长度上限

#### Scenario: 护栏与降级的不变量

- GIVEN 一组已知应被拦截的候选输出
- WHEN 评测驱动它们经过护栏
- THEN 各层判定结论 SHALL 与期望一致
- AND 应当降级的路径 SHALL 发生降级
- AND 不下发工具的模式下返回的工具提议 SHALL 被丢弃

#### Scenario: 不变量失败的处置

- GIVEN 某条不变量断言失败
- WHEN 开发者处理该失败
- THEN 该失败 SHALL 被视为缺陷
- AND 系统 SHALL NOT 提供任何刷新或接受该失败的手段

#### Scenario: 不依赖用例声明的通用不变量

> 存在理由：写在用例文件里的期望依赖作者记得声明。
> 一批通用底线放在断言层统一执行，可使任何新增用例自动受其保护。

- GIVEN 任意一条评测用例被执行
- WHEN 不变量被校验
- THEN 长度上限、追问上限、注入派生上限、判定结论取值范围、轮次序号单调性
  与版本锚点在位 SHALL 被无条件校验
- AND 这些校验 SHALL NOT 依赖用例逐条声明

#### Scenario: 期望键的拼写

> 守的是「静默失效」：拼错的期望键若被忽略，用例文件里写着一条期望，
> 实际什么都没验，而测试是绿的。

- GIVEN 某条用例声明了一个未被断言层消费的期望键
- WHEN 该用例被执行
- THEN 评测 SHALL 失败
- AND 评测 SHALL NOT 静默忽略该期望

### Requirement: Regression Baselines Must Be Comparable And Traceable

评测 SHALL 提供可比对的基线，且基线的变更 SHALL 留下可审计的说明。

#### Scenario: 基线比对

- GIVEN 某次改动前已存在基线
- WHEN 评测在改动后被执行
- THEN 评测 SHALL 报告哪些指标相对基线发生变化
- AND 报告 SHALL 同时给出基线值与当前值

#### Scenario: 基线的版本归属

- GIVEN 某条基线被记录
- WHEN 检查该基线
- THEN 该基线 SHALL 记录定基线时的提示词版本与护栏规则版本
- AND 基线 SHALL 可按版本锚点分组比对

#### Scenario: 基线变更须留痕

- GIVEN 某条基线的指标值被更新
- WHEN 该更新被提交
- THEN 对应的基线说明 SHALL 一并更新
- AND 只更新指标值而不更新说明的情形 SHALL 被评测拦截
- AND 该拦截 SHALL 由机制保证，SHALL NOT 仅依赖文档约定

#### Scenario: 基线变更留痕机制的完整性

> 若校验值只由指标派生，则「改指标并同步改说明」这一**正确**流程
> 与「只改指标」会得到相同的校验值，机制形同虚设。

- GIVEN 基线变更留痕由校验值机制保证
- WHEN 只有基线说明被修改而指标未变
- THEN 校验值 SHALL 同样发生变化
- AND 该机制自身 SHALL 被测试直接验证

#### Scenario: 基线更新的操作方式

- GIVEN 某条基线需要更新
- WHEN 开发者执行更新
- THEN 系统 SHALL NOT 提供自动重写全部基线的开关
- AND 更新 SHALL 保持为一个需要人工判断的动作
- AND 评测 SHALL 在失败信息中给出可供人工确认后粘贴的新值

#### Scenario: 基线的孤儿条目

- GIVEN 某条用例已被删除而其基线条目仍存在
- WHEN 评测被执行
- THEN 评测 SHALL 失败
- AND 理由 SHALL 是孤儿基线会让「该维度仍有回归守护」成为假象

### Requirement: Evaluation Must Not Change Agent Behavior

评测 SHALL 只观测编排行为，SHALL NOT 为可评测性而改变它。

#### Scenario: 评测与生产路径的边界

- GIVEN 评测需要驱动 mock 路径产不出的场景
- WHEN 评测装配替身
- THEN 替身 SHALL 只存在于测试范围
- AND 评测 SHALL NOT 修改在生产路径上被使用的组件

#### Scenario: 评测发现的问题

- GIVEN 评测揭示了某个编排行为的缺陷
- WHEN 处理该缺陷
- THEN 建立评测的同一变更 SHALL NOT 顺手修改 Agent 行为、提示词或护栏阈值
- AND 行为修改 SHALL 由后续独立变更承担

#### Scenario: 阈值校准的边界

- GIVEN 评测已能支撑阈值校准
- WHEN 审查本阶段范围
- THEN 建立评测的变更 SHALL NOT 校准任何护栏阈值
- AND 基线 SHALL 记录校准前的当前状态

### Requirement: Evaluation Must Not Leak Diary Or Case Content

评测产物 SHALL 只包含结构化标识与数值指标。

#### Scenario: 基线与报告的内容边界

- GIVEN 用例输入包含文本
- WHEN 基线与失败报告被写出
- THEN 它们 SHALL NOT 包含用例输入文本
- AND 它们 SHALL NOT 包含用户日记原文、对话原文或记忆片段内容

#### Scenario: 判定指标的形状

- GIVEN 护栏判定指标被用于断言
- WHEN 评测校验该指标
- THEN 评测 SHALL 断言该指标整体符合纯数值形状
- AND 评测 SHALL NOT 仅以列举若干禁止词的方式校验

#### Scenario: 真实样本的处置

- GIVEN 评测可使用真实样本作为输入
- WHEN 该样本存在于工作区
- THEN 该样本 SHALL NOT 进入 tracked files
- AND 该样本缺失时评测 SHALL 静默跳过相关用例而非失败
- AND 入库的用例 SHALL 只使用合成内容

#### Scenario: 入库用例缺失

- GIVEN 入库的合成用例文件缺失
- WHEN 评测被执行
- THEN 评测 SHALL 失败
- AND 评测 SHALL NOT 静默跳过

#### Scenario: 用例解析不可用

- GIVEN 用例文件的解析能力不可用
- WHEN 评测被执行
- THEN 评测 SHALL 明确失败
- AND 评测 SHALL NOT 表现为「全部用例通过」

### Requirement: Evaluation Must State Its Honest Boundary

评测 SHALL 显式声明它覆盖什么、不覆盖什么。

#### Scenario: 语言质量的覆盖边界

- GIVEN 评测在替身与 mock 路径上运行
- WHEN 描述评测的能力
- THEN 评测 SHALL 被表述为覆盖编排逻辑的不变量与回归比对
- AND 评测 SHALL NOT 被表述为对模型语言质量的判定
- AND 语言质量 SHALL 由真实探针的小样本人评锚定

#### Scenario: 人评锚点尚未填充

- GIVEN 人评锚点的结构已就位而内容为空
- WHEN 审查该维度
- THEN 该维度 SHALL 被如实标注为锚点未填充
- AND 空的锚点 SHALL NOT 被视为该维度已覆盖
- AND 该标注 SHALL 由测试保证其存在，SHALL NOT 仅写在文档中

#### Scenario: 人评锚点的内容边界

- GIVEN 人评锚点将被填充
- WHEN 定义其字段
- THEN 锚点 SHALL 只记录评级、理由标签与当时的版本锚点
- AND 锚点 SHALL NOT 承载 Agent 回复原文或用户日记原文

#### Scenario: 未验证项的表述

- GIVEN 评测未在真实 provider 下验证指标稳定性
- WHEN 记录验证结果
- THEN 该项 SHALL 被如实记为未验证
- AND 该项 SHALL NOT 被表述为已通过

### Requirement: Evaluation Must Exclude Judging Reflection Resilience And Temporal Intelligence

C6 SHALL 限定在不变量断言、回归比对与评测隐私范围内。

#### Scenario: C6 范围内的评分能力

- GIVEN 评测已能断言编排不变量
- WHEN 评估质量判定能力
- THEN C6 SHALL NOT 引入第二个模型对输出打分
- AND C6 SHALL NOT 产出绝对质量分数
- AND C6 SHALL NOT 提供质量看板或对比实验框架

#### Scenario: C6 范围内的输出行为

- GIVEN C6 已建立回归基线
- WHEN 审查 C6 自身交付的 Agent 输出行为
- THEN C6 SHALL NOT 引入不合格输出的重写环
- AND 受控重写能力 SHALL 自 C7 起由本 spec 的 reflection 条款约束

#### Scenario: C6 范围内的韧性与时间智能

- GIVEN 评测覆盖了失败路径的不变量
- WHEN 评估失败处置与时间感知能力
- THEN C6 SHALL NOT 引入错误分类策略、降级模板或多 provider 路由
- AND C6 SHALL NOT 引入时间距离话术或记忆衰减策略
- AND 两者 SHALL 留给后续独立 change

#### Scenario: C6 范围内的用户可见能力

- GIVEN 评测面向开发者
- WHEN 审查用户可见界面
- THEN C6 SHALL NOT 新增任何用户可见能力

#### Scenario: C6 范围内的引导与素材质量

- GIVEN 评测已能比对引导话术改动前后的指标
- WHEN 评估引导话术与素材合成质量
- THEN C6 SHALL NOT 修改引导阶段的提问策略
- AND C6 SHALL NOT 修改素材合成策略
- AND 质量优化 SHALL 在基线就位之后由独立安排承担

## Accepted From C7 agent-reflection-loop

> Accepted on 2026-08-03. C7 只实现非 `CLOSING` reply 的受控一次重写；
> material、tool、CLOSING 与 provider error 均不开环。

### Requirement: Reflection Must Be A Bounded Backend Controlled Loop

Agent reflection SHALL 是后端控制的单次环，模型不得决定是否继续或继续次数。

#### Scenario: 首次生成通过

- GIVEN initial provider 输出通过当前路径的全部护栏
- WHEN 本轮完成
- THEN 系统 SHALL 直接返回该输出
- AND SHALL NOT 发起 reflection 调用

#### Scenario: 允许恢复的违规

- GIVEN 非 `CLOSING` reply 命中 `MISSING_TIME_ATTRIBUTION`
- WHEN 系统判断该违规允许恢复
- THEN 系统 MAY 发起一次且仅一次 reflection 调用
- AND 最大次数 SHALL 由后端常量约束为 1

#### Scenario: 重写仍不合格

- GIVEN reflection 输出仍未通过完整护栏
- WHEN 系统决定终态
- THEN 系统 SHALL 停止继续调用 provider
- AND reply SHALL 使用既有本地安全兜底

### Requirement: Reflection Eligibility Must Be Path Specific And Closed

允许 reflection 的违规集合 SHALL 是封闭且按路径声明的，不得通过默认分支扩张。

#### Scenario: Reply eligible set

- GIVEN 一条非 `CLOSING` reply 被检查
- WHEN 其违反时间归属要求
- THEN `MISSING_TIME_ATTRIBUTION` MAY 触发一次 reflection
- AND reply SHALL NOT 因 C7 新增全量忠实度检查

#### Scenario: CLOSING 与 material 不开环

- GIVEN `CLOSING` reply 后仍需生成 material，或 material 命中 `UNFAITHFUL`
- WHEN 系统处置该输出
- THEN SHALL NOT 发起 reflection
- AND material SHALL 继续使用既有直接丢弃语义
- AND 单轮 provider 调用预算 SHALL NOT 因 C7 超过 2

#### Scenario: 不可恢复违规

- GIVEN 任一路径命中 `FABRICATED_QUOTE`、`DIAGNOSTIC`、`FAKE_ACTION` 或 `CHECK_ERROR`
- WHEN 系统处置该违规
- THEN SHALL NOT 发起 reflection
- AND SHALL 立即沿用既有 fail-closed 终态

#### Scenario: Tool proposal 不开环

- GIVEN 工具提议因 `UNFAITHFUL` 或其他护栏原因被拒绝
- WHEN 本轮继续处理
- THEN SHALL NOT 为该提议发起 reflection
- AND SHALL 复用既有拒绝与二段确认边界

### Requirement: Reflection Instructions Must Be Typed And Content Free

reflection 指令 SHALL 仅由违规类型映射到固定要求，不得携带待重写文本片段。

#### Scenario: 指令生成

- GIVEN 一个允许恢复的 violation enum
- WHEN 系统构造 reflection 指令
- THEN 指令 SHALL 来自固定后端映射
- AND 映射输入 SHALL NOT 包含候选文本、用户原话、记忆片段、未覆盖片段或 prompt 全文

#### Scenario: 未知或不可恢复类型

- GIVEN violation 不在封闭 eligible 集合中
- WHEN 系统查询重写要求
- THEN SHALL 返回不可重写
- AND SHALL NOT 存在“默认全部重试”的分支

### Requirement: Reflection Must Preserve Existing Guardrail Authority

重写后 SHALL 重新运行当前路径的全部确定性护栏，reflection 不得替代或绕过 checker。

#### Scenario: Reply re-check

- GIVEN reply 已完成 reflection
- WHEN 判定能否展示
- THEN SHALL 重新运行 content 与 time attribution checks
- AND SHALL 继续执行既有长度硬上限

#### Scenario: 护栏边界不被放宽

- GIVEN C7 引入 reflection
- WHEN 审查 C4 契约
- THEN 护栏阈值、词表、来源集合与 fail-closed 语义 SHALL 保持不变

### Requirement: Reflection Must Not Become Error Retry Or Tool Loop

#### Scenario: Provider failure

- GIVEN initial 或 reflection provider 调用失败或返回无效内容
- WHEN 系统处置
- THEN SHALL NOT 因 C7 再次调用 provider
- AND provider 错误分类与一般重试 SHALL 留给 C8

#### Scenario: Reflection call tools

- GIVEN reply 进入 reflection
- WHEN 第二次 provider 调用被构造
- THEN tools SHALL 为空
- AND strict tool calling SHALL 关闭
- AND SHALL NOT 产生第二组工具提议

#### Scenario: 最终本地兜底

- GIVEN reply 最终采用本地安全兜底
- WHEN 处理 initial tool calls
- THEN initial tool calls SHALL 被丢弃
- AND 本地兜底 SHALL NOT 与来自不合格 generation 的提议共同展示

### Requirement: Reflection Must Be Observable Without Becoming A New Attempt

#### Scenario: Attempt identity

- GIVEN 一次业务请求内部发生 reflection
- WHEN 轨迹被持久化
- THEN SHALL 只有一条 turn trace
- AND `turnNo` 与 `attemptNo` SHALL 保持该请求的既有值
- AND reflection SHALL NOT 调用下一 attempt 分配逻辑

#### Scenario: Phase visibility

- GIVEN initial 与 reflection 均调用 provider
- WHEN 开发者读取结构化轨迹
- THEN provider steps SHALL 区分 `initial` 与 `reflection`
- AND SHALL 能区分重写成功、最终兜底、最终丢弃与 provider failure
- AND 轨迹 SHALL NOT 包含任何候选文本

#### Scenario: Outcome semantics

- GIVEN reflection 后输出通过护栏
- WHEN 汇总本轮 outcome
- THEN outcome SHALL 为成功而非降级
- AND reflection 发生事实 SHALL 由 steps 表达

### Requirement: Reflection Must Remain Comparable Through C6 Evaluation

#### Scenario: 回归执行

- GIVEN C7 改变编排行为
- WHEN 后端测试运行
- THEN C6 全部硬不变量 SHALL 继续执行
- AND 既有断言 SHALL NOT 被削弱或删除

#### Scenario: 预期快照变化

- GIVEN 某快照因 C7 合法改变
- WHEN 人工接受该变化
- THEN `baselineNote` SHALL 同步写明 C7 与变化原因
- AND checksum SHALL 按 C6 机制同步更新
- AND 系统 SHALL NOT 提供自动刷新入口

## Accepted From C8 agent-resilience

> Accepted on 2026-08-08. C8 第一阶段建立封闭失败分类、共享 provider-work deadline、
> 零自动 retry 与阶段化失败呈现；多 provider、熔断、缓存与 C9 时间智能仍不在本范围。

### Requirement: Reflection Must Not Become Error Retry Or Tool Loop

#### Scenario: Provider failure

- GIVEN initial 或 reflection provider 调用失败或返回无效内容
- WHEN 系统处置
- THEN SHALL 按 C8 封闭 taxonomy 分类
- AND SHALL NOT 在同一请求自动再次调用 provider
- AND 用户 MAY 通过既有入口主动重试同一轮

### Requirement: Provider Failures Must Use A Closed Typed Taxonomy

#### Scenario: 已知错误来源

- GIVEN provider 调用发生 timeout、throttling、auth/config、upstream unavailable、invalid response、request rejected 或 interrupted
- WHEN backend 分类该失败
- THEN SHALL 使用封闭的稳定 category
- AND SHALL NOT 通过异常 message 或 response body 关键词完成核心分类

#### Scenario: 未知错误

- GIVEN 错误无法映射到已知类别
- WHEN 形成失败终态
- THEN category SHALL 为 `UNKNOWN`
- AND SHALL NOT 因 unknown 自动重试 provider

### Requirement: Every Agent Orchestration Must Share One Provider Work Deadline

#### Scenario: 多 provider 子调用

- GIVEN 一次 Agent HTTP 编排可能包含 initial/reflection 或 reply/material
- WHEN 发起每个 provider 子调用
- THEN 所有子调用 SHALL 共享同一 request-scope provider-work budget
- AND 每次 timeout SHALL NOT 超过当前剩余预算与单次 provider 上限的较小值

#### Scenario: 预算耗尽

- GIVEN 整轮剩余预算已不足以安全发起下一次调用
- WHEN reflection 或 material 准备调用 provider
- THEN 系统 SHALL 不发起该调用
- AND SHALL 形成类型化 timeout/deadline-exhausted 终态

### Requirement: C8 Must Not Add Automatic Provider Retry

#### Scenario: 暂态错误

- GIVEN provider 返回 timeout、429、连接失败或 5xx
- WHEN C8 第一阶段处置
- THEN SHALL NOT 在同一请求自动 retry
- AND 用户 MAY 通过既有同轮重试入口稍后主动重试

#### Scenario: 调用数上限

- GIVEN C7 reflection 与 CLOSING material 语义保持不变
- WHEN C8 执行
- THEN 非 `CLOSING` reply provider calls SHALL 不超过 2
- AND `CLOSING` reply + material provider calls SHALL 不超过 2
- AND finish material-only provider calls SHALL 不超过 1

### Requirement: Provider Failure Must Remain Distinct From Guardrail Fallback

#### Scenario: provider 调用失败

- GIVEN provider 未生成可用回复
- WHEN 返回该轮结果
- THEN status SHALL 为 `FAILED` 或 `UNAVAILABLE`
- AND Assistant message SHALL NOT 被持久化
- AND 本地失败模板 SHALL NOT 被冒充为 provider 正常回复

#### Scenario: 护栏本地兜底

- GIVEN provider 已返回内容但确定性护栏决定降级
- WHEN 用户收到既有安全兜底
- THEN 既有 `DOWNGRADED` 轨迹语义 SHALL 保持
- AND provider failure taxonomy SHALL NOT 覆盖或弱化护栏违规类型

### Requirement: Failure Presentation Must Be Stage Aware And Backend Controlled

#### Scenario: 固定模板映射

- GIVEN opening、普通 turn 或 closing/material 路径失败
- WHEN backend 生成用户可见 message
- THEN SHALL 仅按 operation/stage/category 从固定映射选择
- AND SHALL NOT 拼接用户文本、prompt、异常 message 或 provider response

#### Scenario: 产品气质

- GIVEN 用户看到失败提示
- WHEN 阅读该提示
- THEN 文案 SHALL 克制、温和且说明已提交内容未丢（适用时）
- AND SHALL NOT 使用诊断、技术术语、后台恢复承诺或虚假共情

### Requirement: Failure Classification Must Be Observable Without Content Leakage

#### Scenario: turn trace

- GIVEN provider failure 发生在 initial/reflection/material
- WHEN 写结构化轨迹
- THEN SHALL 记录 phase、category、transient 与 budget 状态
- AND SHALL NOT 记录 exception message、HTTP body、prompt、用户文本或 provider response

#### Scenario: opening

- GIVEN opening 发生在 turnNo=0 且不属于用户轮次
- WHEN 记录失败
- THEN SHALL 使用脱敏结构化日志
- AND SHALL NOT 为可观测性伪造 turn trace row

## Accepted From C9 Agent Temporal Intelligence

### Requirement: Temporal Distance Must Be Deterministic And Clock Driven

#### Scenario: 距离层级边界

- GIVEN 固定 `Clock` 与一条已知发生时间的记忆片段
- WHEN backend 计算时间距离
- THEN 0–30 天 SHALL 为 `RECENT`
- AND 31–180 天 SHALL 为 `DISTANT`
- AND 181 天以上 SHALL 为 `LONG_AGO`
- AND 同一输入与时钟 SHALL 始终得到同一结果

#### Scenario: 零额外调用

- GIVEN temporal policy 被启用
- WHEN 一轮写作引导或回看对话被编排
- THEN 时间距离 SHALL 在 backend 本地计算
- AND SHALL NOT 为分类、衰减或 pattern eligibility 新增 provider 调用

### Requirement: Older Ancillary Memories Must Receive A Smaller Injection Budget

#### Scenario: 旁支记忆衰减

- GIVEN memory port 返回一条关联历史片段
- WHEN temporal policy 形成最终注入片段
- THEN `RECENT` SHALL 使用既有单片段字符上限的 100%
- AND `DISTANT` SHALL 使用 75%
- AND `LONG_AGO` / `UNKNOWN` SHALL 使用 50%
- AND 任一片段预算 SHALL 不低于 40 字且不超过既有配置上限

#### Scenario: 用户主动打开的回看目标记录

- GIVEN 用户主动打开某条 UNLOCKED 记录并进入回看对话
- WHEN backend 注入该目标记录自身的片段
- THEN 目标记录 SHALL 保持既有 `review.record-excerpt-chars` 预算
- AND SHALL NOT 因距离久远而被当作旁支记忆衰减

#### Scenario: 业务记录与检索范围

- GIVEN temporal 衰减已启用
- WHEN 审查记录与检索行为
- THEN SHALL NOT 删除、修改、过期或隐藏任何业务记录
- AND SHALL NOT 改变 memory port 的 24 个月窗口、owner/status 隔离、匹配字段或无线索不查询语义

### Requirement: Recurrence Hints Must Be Evidence Gated And User Invited

#### Scenario: 允许形成重复主题提示

- GIVEN 会话模式为 `REVIEW_CHAT`
- AND 当前用户消息明确表达比较、再次发生或回看变化的意图
- AND 至少两个不同的旁支历史记录具有已知发生时间且跨度不小于 90 天
- WHEN temporal policy 评估 pattern eligibility
- THEN MAY 形成每轮最多一个无内容的 recurrence hint
- AND hint SHALL 只允许 Agent 说“似乎不止一次出现过”并邀请用户自己判断

#### Scenario: 证据不足

- GIVEN 会话不是回看模式、用户未表达比较意图、仅有目标记录、旁支记录少于两个或时间跨度不足
- WHEN temporal policy 评估 pattern eligibility
- THEN SHALL NOT 形成 recurrence hint
- AND Agent SHALL NOT 为显得有时间智能而主动归纳模式

#### Scenario: 不得声称周期结论

- GIVEN recurrence hint 已形成
- WHEN Agent 回应
- THEN SHALL NOT 声称“每年、每隔固定时间、总是、必然、越来越严重/改善”
- AND SHALL NOT 推断原因、预测未来、给出评分或替用户定义成长/退步

### Requirement: Temporal Overreach Must Fail Closed Without Expanding Reflection

#### Scenario: 检出分析化时间话术

- GIVEN provider 回复包含百分比/评分、绝对频率、必然因果、心理诊断、趋势结论或预测式时间话术
- WHEN deterministic temporal checker 执行
- THEN SHALL 返回内部 `TEMPORAL_OVERREACH` 违规
- AND SHALL 使用既有安全兜底替换该回复
- AND SHALL NOT 持久化越界 Assistant 内容

#### Scenario: reflection 与调用预算

- GIVEN `TEMPORAL_OVERREACH` 被检出
- WHEN backend 决定后续路径
- THEN SHALL NOT 进入 C7 reflection
- AND 非 CLOSING、CLOSING+material 与 finish 的既有 provider 调用上限 SHALL 保持不变

### Requirement: Temporal Context Must Preserve Existing Source And Content Boundaries

#### Scenario: temporal hint 的生命周期

- GIVEN backend 为一轮对话形成距离层级、衰减预算或 recurrence hint
- WHEN prompt、来源集合与持久化被组装
- THEN temporal hint SHALL 只存在于本轮内存与 provider prompt
- AND SHALL NOT 进入用户记录、素材、工具参数、agent message、日志或新的持久化位置

#### Scenario: 既有护栏 authority

- GIVEN temporal policy 已启用
- WHEN Agent 回复或工具提议被检查
- THEN 忠实度、诊断、代决、伪引用、时间归属、工具白名单与长度上限 SHALL 全部保持
- AND temporal policy SHALL NOT 扩大 memory layer 成为用户正文合法来源

## Accepted From P3.1 Present Moment Capture

### Requirement: P3.1 Status Expansion Must Not Change Agent Generation Semantics

#### Scenario: Provider 与 Prompt 边界

- GIVEN 写作引导现在可绑定 DRAFT 或 SAVED
- WHEN Agent 编排一轮回复
- THEN SHALL 复用既有 Prompt、阶段、memory、guardrail、reflection 与 provider contract
- AND SHALL NOT 因 P3.1 新增 provider 调用、自动 retry、分析能力或用户可见状态字段

#### Scenario: Evaluation baseline

- GIVEN P3.1 实现只改变记录状态资格
- WHEN C6 eval 与既有 Agent 回归运行
- THEN provider call count、阶段不变量、护栏与隐私规则 SHALL 保持
- AND 任何 snapshot 文本变化 SHALL 被视为意外 scope drift，除非用户另行批准独立 Agent change

#### Scenario: 用户原文与媒体隐私

- GIVEN Agent 与 DRAFT 或 SAVED 记录交互
- WHEN 日志、trace、eval 或 exception 被写入
- THEN SHALL 继续遵守既有无原文边界
- AND SHALL NOT 记录附件内容、storage key、signed URL 或 P3.1 migration/cleanup 私人数据

## Accepted From P3.2 Data Ownership Foundation

### Requirement: Agent Content In Export Must Remain Distinguishable From User Original

User-visible Agent session content MAY be included in a complete ownership export, but SHALL remain physically and semantically separate from user-authored record content.

#### Scenario: Export includes an Agent session

- **GIVEN** an exported record has owner-visible Agent messages
- **WHEN** the package is built
- **THEN** messages SHALL be written under the Agent section or directory with role and time attribution
- **AND** assistant text SHALL NOT be represented as user-authored diary content

#### Scenario: Internal Agent runtime data is considered

- **GIVEN** prompts, provider responses, guardrail internals, transient tool arguments, eval fixtures, or secrets exist
- **WHEN** the user export is built
- **THEN** they SHALL NOT be copied into the package
- **AND** necessary tool/trace coverage MAY be described only through non-content metadata and counts

### Requirement: Record Deletion Must Remove Record-Linked Agent Data

Deleting a record SHALL remove its record-linked Agent sessions and all dependent messages, tool calls, and turn traces.

#### Scenario: Record with Agent history is deleted

- **GIVEN** the owned record has Agent sessions, messages, tool calls, or traces
- **WHEN** its deletion operation succeeds
- **THEN** all record-linked Agent data SHALL be absent
- **AND** the operation SHALL NOT report success if an associated row remains

#### Scenario: Historical derived data cannot be safely attributed

- **GIVEN** a historical Agent row appears related but lacks a trustworthy record association
- **WHEN** deletion consistency is checked
- **THEN** the system SHALL fail closed or record a repair-required failure
- **AND** SHALL NOT infer linkage from diary text, prompt text, or other sensitive content

### Requirement: Ownership Actions Must Stay Outside Agent Tools

Export, record deletion, and clear-all SHALL remain user-confirmed product actions and SHALL NOT be added to the Agent tool registry.

#### Scenario: User requests deletion in chat

- **GIVEN** a user asks Agent to export, delete a record, or clear all records
- **WHEN** Agent responds
- **THEN** it MAY explain how to reach the real “数据与所有权” page
- **AND** SHALL NOT invoke, propose, or claim execution of the operation

#### Scenario: Model proposes an ownership tool

- **GIVEN** a provider output invents an export or destructive tool call
- **WHEN** the runtime validates the proposal
- **THEN** it SHALL reject the proposal through existing fail-closed boundaries
- **AND** record and operation state SHALL remain unchanged

### Requirement: Writing Guidance Sessions Must Declare A User Controlled Conversation Intent

每个 WRITING_GUIDANCE 会话 SHALL 携带 `LISTEN` 或 `UNTANGLE` conversation intent；该值 SHALL 来自用户选择而非模型推断。

#### Scenario: 用户选择先听我说

- GIVEN 用户主动打开 WRITING_GUIDANCE 入口
- WHEN 用户选择“先听我说”
- THEN session intent SHALL 为 `LISTEN`
- AND 后端 SHALL 使用 `REFLECT_ONLY` 策略

#### Scenario: 用户选择帮我理一理

- GIVEN 用户主动打开 WRITING_GUIDANCE 入口
- WHEN 用户选择“帮我理一理”
- THEN session intent SHALL 为 `UNTANGLE`
- AND 正常输入 MAY 使用 `MAY_ASK_ONE` 策略

#### Scenario: 用户切换意图

- GIVEN 一个 owner-scoped ACTIVE WRITING_GUIDANCE 会话
- WHEN 用户显式切换 conversation intent
- THEN 后端 SHALL 持久化新 intent
- AND 切换 SHALL NOT 调用 provider、推进 turn/stage、执行工具或生成素材

#### Scenario: 回看对话

- GIVEN 一个 REVIEW_CHAT 会话
- WHEN 后端编排或返回该会话
- THEN 会话 SHALL NOT 伪造 WRITING_GUIDANCE conversation intent
- AND REVIEW_CHAT 的 REVIEW stage、无工具、无素材契约 SHALL 保持

### Requirement: Witness Turn Policy Must Bound Questions Per Turn

后端 SHALL 在 provider 调用前为每轮产生 `REFLECT_ONLY`、`MAY_ASK_ONE` 或 `CLOSE` typed policy，并给出可验证的问题上限。

#### Scenario: LISTEN 普通输入

- GIVEN conversation intent 为 `LISTEN`
- WHEN 用户提交任意未结束输入
- THEN turn policy SHALL 为 `REFLECT_ONLY`
- AND Agent 回复 SHALL 包含 0 个问题

#### Scenario: UNTANGLE 普通输入

- GIVEN conversation intent 为 `UNTANGLE`
- AND 用户输入不是极短回答或结束意图
- WHEN 后端计算本轮策略
- THEN turn policy SHALL 为 `MAY_ASK_ONE`
- AND Agent 回复 SHALL 至多包含 1 个具体、可跳过的问题
- AND Agent MAY 选择不提问

#### Scenario: UNTANGLE 极短输入

- GIVEN conversation intent 为 `UNTANGLE`
- AND 用户输入去空白长度不超过 4
- WHEN 后端计算本轮策略
- THEN turn policy SHALL 为 `REFLECT_ONLY`
- AND Agent SHALL NOT 继续盘问或要求用户解释短答

#### Scenario: 任意意图结束

- GIVEN 用户表达结束或会话达到轮次上限
- WHEN 后端计算本轮策略
- THEN turn policy SHALL 为 `CLOSE`
- AND 问题上限 SHALL 为 0

### Requirement: Witness Aligned Replies Must Pass Deterministic Question Enforcement

模型回复 SHALL 在返回用户前经过与 turn policy 对应的问题数量检查；该检查 SHALL 位于 backend 生产 pipeline，且 SHALL NOT 外调新的分类服务。

#### Scenario: 回复符合问题上限

- GIVEN provider 回复的问题数量未超过本轮 0 或 1 上限
- WHEN backend 执行 question enforcement
- THEN 回复 MAY 继续经过既有忠实度、内容、时间与长度护栏
- AND P4.1 SHALL NOT 因此跳过任何既有护栏层

#### Scenario: 回复问题超限

- GIVEN provider 回复的问题数量超过本轮上限
- WHEN backend 执行 question enforcement
- THEN SHALL 产生 typed violation `EXCESSIVE_QUESTIONS`
- AND 该 violation MAY 按既有 C7 机制触发至多一次 reflection
- AND reflection 指令 SHALL NOT 包含用户文本或候选回复

#### Scenario: Reflection 后仍超限

- GIVEN `EXCESSIVE_QUESTIONS` reflection 后仍超过上限
- WHEN backend 形成最终回复
- THEN SHALL 使用 backend-owned、无问题的克制 fallback
- AND fallback SHALL NOT 冒充正常 provider 输出
- AND trace SHALL 记录最终降级但不记录候选文本

#### Scenario: 问号计数边界

- GIVEN 回复包含中文、英文、混合或连续问号
- WHEN backend 计算问题数量
- THEN 连续问号 SHALL 归一为同一问句边界
- AND 规则 SHALL 由固定测试覆盖

### Requirement: Agent Must Behave As A Witness Without Relationship Claims

Agent SHALL 以有温度的见证者身份回应，SHALL NOT 通过朋友、伴侣或拟人关系承诺换取持续互动。

#### Scenario: 用户表达一段当下经历

- GIVEN 用户主动与 Agent 对话
- WHEN Agent 回应
- THEN Agent SHALL 先回应用户已经表达的内容
- AND Agent MAY 承认自己可能理解得不完全
- AND Agent SHALL NOT 抢先定义用户真正的情绪、核心问题或期待

#### Scenario: 形成关系期待的措辞

- GIVEN Agent 组装 system role 与 turn instruction
- WHEN Prompt 被审查
- THEN SHALL NOT 自称用户的朋友、伴侣或最懂用户的对象
- AND SHALL NOT 承诺一直陪伴、主动关心、等待用户或比现实关系更可靠

#### Scenario: 人格和结论边界

- GIVEN 用户描述一次行为或一段感受
- WHEN Agent 回应
- THEN SHALL NOT 使用“你总是”或把单次表达固化为人格、阶段、诊断
- AND SHALL NOT 要求用户得出结论、变得积极或规划未来

#### Scenario: Review chat role

- GIVEN 用户主动开启 REVIEW_CHAT
- WHEN Agent 回应
- THEN 同一 witness role 与不抢解释权边界 SHALL 适用
- AND C3b 的无阶段、无工具、无素材与时间归属边界 SHALL 保持

### Requirement: Witness Alignment Must Preserve Existing Agent Boundaries

P4.1 SHALL 只改变角色、写作引导编排、问题约束和对应 UI，不得削弱 C2–C9 与 P3.1/P3.2 的已接受契约。

#### Scenario: 工具与素材

- GIVEN witness 对话产生工具提议或收束素材
- WHEN 这些内容可能改变记录
- THEN 工具执行与素材写回 SHALL 继续要求用户显式确认
- AND 用户原文、SEALED location/attachment/cover 不变性 SHALL 保持

#### Scenario: 记忆与时间

- GIVEN 既有 C3/C9 路径向对话提供历史片段
- WHEN P4.1 组装回复
- THEN 时间归属、来源层、不可提前拆封、不得写入当前正文等边界 SHALL 继续成立
- AND P4.1 SHALL NOT 修改跨记录检索授权或实现 P4.2

#### Scenario: 失败与重试

- GIVEN provider 不可用、超时、无效响应或 reflection 失败
- WHEN 系统处理该轮
- THEN C8 failure taxonomy、共享 deadline、零自动错误 retry 与 pending turn recovery SHALL 保持

#### Scenario: Preview

- GIVEN 当前只有 Preview session 而无真实登录凭证
- WHEN 用户尝试开始或切换 witness 对话
- THEN 真实 Agent API 调用 SHALL 为 0
- AND 系统 SHALL NOT 用本地生成内容或假状态冒充成功
