# Delta Spec：agent-runtime（P4.1 Witness Agent Alignment）

## MODIFIED Requirements

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

## ADDED Requirements

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
