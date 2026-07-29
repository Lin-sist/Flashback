# Agent Runtime Spec Delta：agent-review-chat（C3 后半刀）

> 本文件是 delta。范围：友人回看对话（会话模式、无阶段轮次、无工具、无素材、来源分层）。

---

## MODIFIED Requirements

### 修订 1：`Writing Guidance Must Target Draft Records Only`

原条款把「Agent 对话只作用于草稿」写成了 Runtime 级约束。自 C3b 起对话按用途分模式，
草稿约束仍然完整适用于**写作引导**，回看则只作用于已解锁记录。原条款标题语义不变，
但需补充回看的对应约束。

#### Scenario: 对话关联草稿记录

- GIVEN 一个已登录用户拥有某 DRAFT 记录
- WHEN 该用户以该记录开启写作引导对话
- THEN 后端 SHALL 允许开启会话

#### Scenario: 写作引导对话关联已封存或已解锁记录

> C3b 修订：措辞收紧为「写作引导」，因为回看对话的合法对象恰恰是已解锁记录。
> 写作引导的约束本身**未被放宽**。

- GIVEN 目标记录处于 SEALED 或 UNLOCKED
- WHEN 用户尝试以该记录开启**写作引导**对话
- THEN 后端 SHALL 拒绝该操作
- AND 封存后的不可变契约 SHALL 保持不变

#### Scenario: 回看对话的记录状态要求

- GIVEN 目标记录处于 DRAFT 或 SEALED
- WHEN 用户尝试以该记录开启回看对话
- THEN 后端 SHALL 拒绝该操作
- AND 尚未解锁的记录内容 SHALL NOT 经由回看对话被提前读到

### 修订 1b：新增「回看对话的记录状态要求」scenario

与修订 1 同属一个 Requirement：写作引导的约束收紧措辞后，需补一条回看侧的对应约束，
否则「回看只能作用于 UNLOCKED」在契约上无处体现。

### 修订 2：C3a 条款「C3a 范围内的回看对话」

#### Scenario: C3a 范围内的回看对话

> C3b 修订：本 scenario 原文「回看多轮对话 SHALL 留给 C3 后半刀」已由 C3b 落实，
> 改写为 C3a 阶段范围声明 + 指向本刀条款。

- GIVEN 仅 C3 前半刀实现存在
- WHEN 审查会话用途
- THEN 该阶段的后端 SHALL NOT 提供作用于已解锁记录的对话行为
- AND 回看对话 SHALL 自 C3 后半刀起由本 spec 的回看条款约束

---

## ADDED Requirements

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

#### Scenario: 本刀范围内的可观测能力

- GIVEN 回看对话已落地
- WHEN 评估可观测能力
- THEN 本刀 SHALL NOT 提供决策链路查询端点或可观测界面
- AND 决策链路可查询 SHALL 留给后续独立 change

#### Scenario: 本刀范围内的引导与素材质量

- GIVEN 回看对话已落地
- WHEN 评估写作引导的提问策略与素材合成策略
- THEN 本刀 SHALL NOT 修改它们

#### Scenario: 本刀范围内的检索能力

- GIVEN 回看复用记忆检索
- WHEN 审查检索实现
- THEN 本刀 SHALL NOT 修改检索的字段范围、权重或索引结构

#### Scenario: 本刀范围内的工具白名单

- GIVEN 回看对话已落地
- WHEN 审查 Agent 可达的后端写操作
- THEN 可达范围 SHALL NOT 超出既有白名单
- AND 本刀 SHALL NOT 新增任何工具
