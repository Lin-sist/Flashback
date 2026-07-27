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

C1 SHALL 限定在对话 Runtime 与最小护栏范围内。

#### Scenario: C1 范围内的工具调用

- GIVEN C1 实现完成
- WHEN 审查 Agent 行为
- THEN Agent SHALL NOT 调用除自身会话与消息持久化以外的任何后端写操作
- AND Tool Calling SHALL 留给后续独立 change

#### Scenario: C1 范围内的记忆能力

- GIVEN 用户与 Agent 对话
- WHEN Agent 组装上下文
- THEN 上下文 SHALL 只来自当前会话与当前草稿内容
- AND 历史记录检索与跨记录关联 SHALL 留给后续独立 change

#### Scenario: C1 范围内的护栏深度

- GIVEN C1 的最小护栏已内嵌
- WHEN 评估护栏防御深度
- THEN C1 SHALL NOT 包含后置输出过滤或违规降级机制
- AND 系统化 hardening SHALL 留给后续独立 change
