# V2 Product Scope Spec / V2 产品范围规格

## Purpose / 目的

定义 V2.0 是什么、不是什么，避免执行 Agent 将 V3.0 或生产上线职责拉入演示版重构。

## Requirements

### Requirement: V2.0 Must Be Treated As A Demo Version / V2.0 MUST 被视为演示版本

V2.0 MUST 按照“写下此刻 · 时间回看演示版”进行规划。

#### Scenario: Agent proposes work / Agent 提出工作

- **WHEN** Agent 提出或实现 V2.0 工作
- **THEN** 工作应优先服务于连贯、可演示的用户侧体验
- **AND** 完成标准不依赖生产上线基础设施

### Requirement: OpenSpec Must Be The Highest Priority For V2.0 / OpenSpec MUST 是 V2.0 最高优先级

当 V2.0 OpenSpec 文档与旧 `Docs/**` 文件冲突时，OpenSpec MUST 覆盖旧文档。

#### Scenario: Old documentation conflicts with OpenSpec / 旧文档与 OpenSpec 冲突

- **WHEN** Agent 发现 OpenSpec 与旧文档冲突
- **THEN** Agent 遵循 OpenSpec
- **AND** 只有在 OpenSpec 缺失或模糊时，才将冲突记录为待确认问题

### Requirement: WeChat Mini Program Is The Acceptance Client / WeChat Mini Program MUST 是验收客户端

除非后续 change 明确扩展范围，否则 V2.0 验收 MUST 聚焦 WeChat Mini Program client。

#### Scenario: Work is verified / 工作被验证

- **WHEN** 检查 V2.0 用户可见变更
- **THEN** 验证目标是 mini program client
- **AND** desktop web 或 admin screens 不能替代 mini program 验收

### Requirement: H5 And Web User Clients Are Historical References / H5 和 Web 用户端 MUST 仅作为历史参考

H5 与 Web 用户侧方案 MUST NOT 被视为 V2.0 验收目标。

#### Scenario: Agent sees an old H5/Web recommendation / Agent 看到旧 H5/Web 建议

- **WHEN** 旧文档建议 H5、Web 或响应式用户端
- **THEN** Agent 将该建议视为历史信息
- **AND** 保持 V2.0 验收聚焦 WeChat Mini Program

### Requirement: Admin And Production Launch Items Must Remain Out Of V2.0 By Default / Admin 与生产上线项默认 MUST 保持在 V2.0 外

Production launch、management/admin portal、deployment hardening 和 online monitoring 默认 MUST 保持在 V2.0 范围外。

#### Scenario: Agent encounters V3.0-like work / Agent 遇到类似 V3.0 的工作

- **WHEN** 任务暗示真实 WeChat launch、admin portal、production deployment 或 monitoring
- **THEN** Agent 将其记录为后续阶段事项
- **AND** 除非后续 OpenSpec change 明确纳入，否则不在 V2.0 下实现

### Requirement: WeChat Subscription Messages Belong To V2.0 But Not M1 / WeChat subscription messages 属于 V2.0 但不属于 M1

V2.0 MUST 在后续模块包含 WeChat subscription message 工作，但 M1 MUST NOT 实现它。

#### Scenario: M1 frontend visual work references time delivery / M1 前端视觉引用时间抵达

- **WHEN** M1 使用“交给时间”或“记录抵达”等文案
- **THEN** 它可以为后续 subscription-message 工作准备视觉语言
- **AND** 它不实现 subscription-message authorization、backend delivery 或 production notification behavior

#### Scenario: A later V2.0 notification module is proposed / 后续 V2.0 通知模块被提出

- **WHEN** 后续 OpenSpec change 将 WeChat subscription messages 纳入范围
- **THEN** 该模块可以定义 V2.0 的 frontend authorization、backend delivery 与 fallback rules

### Requirement: V2.0 Can Upgrade Frontend And Backend Across Separate Modules / V2.0 MAY 按模块升级 frontend 与 backend

完整 V2.0 计划 MAY 同时包含 frontend 与 backend 改进，但每个模块 MUST 尊重自身声明的边界。

#### Scenario: M1 frontend visual work is active / M1 前端视觉工作处于 active 状态

- **WHEN** active change 是 M1 frontend visual foundation
- **THEN** backend、database、production launch 与 business-rule changes 保持在范围外

### Requirement: V2.0 May Use No-Login Preview For Demonstration / V2.0 MAY 使用免登录预览演示

V2.0 MAY 支持 one-click preview / no-login demo mode 用于演示。

#### Scenario: Demo mode is used / 使用演示模式

- **WHEN** mini program 作为 V2.0 demo 展示
- **THEN** 它可以使用现有 preview mechanism，而不是要求真实登录
- **AND** demo path 与 production readiness 保持清晰区分

## Accepted From M4 Real Capability Completion

> Accepted and archived on 2026-07-27 from openspec/changes/archive/2026-07-27-m4-real-capability-completion/. Wording below is the accepted M4 delta requirement body.

### Requirement: M4 Must Target Near Production Usability For Core User Functions

M4 SHALL be treated as a real-capability completion milestone for V2.0 Mini Program core functions.

#### Scenario: Agent implements M4 work

- WHEN an Agent starts M4 implementation
- THEN it SHALL prioritize real authenticated user capability over local-only demo behavior
- AND it SHALL keep completion focused on the Mini Program core loop

#### Scenario: Production platform work is proposed

- WHEN work suggests deployment hardening, monitoring, alerting, incident response, admin portal, or settings-page enhancements
- THEN the Agent SHALL defer it outside M4 unless a new OpenSpec change explicitly includes it

### Requirement: M4 May Retain Explicit Preview Mode

M4 MAY retain one-click preview for demonstration, but preview MUST be separated from real user behavior.

#### Scenario: Preview remains available

- WHEN the user enters explicit preview mode
- THEN the Mini Program MAY use preview data to demonstrate the core flow
- AND this SHALL NOT be treated as evidence that the real authenticated path works

#### Scenario: Real mode is used

- WHEN an authenticated user uses M4 core surfaces
- THEN the Mini Program SHALL use real backend-backed data and real integration states
- AND mock success SHALL NOT be accepted as M4 completion

### Requirement: M4 Storage Scope Is Configurable Private Object Storage

M4 SHALL use a provider-neutral backend contract for record media, with Qiniu and S3-compatible object storage as supported implementations.

#### Scenario: Media storage is implemented

- WHEN images, voice files, or covers are implemented
- THEN they SHALL use the configured object-storage provider with private bucket assumptions
- AND backend-controlled upload authorization and private-access-safe media retrieval SHALL be used

#### Scenario: Active storage provider is changed

- WHEN backend configuration selects Qiniu or an S3-compatible provider
- THEN new uploads SHALL switch provider without changing the attachment APIs or Mini Program business flow
- AND provider-specific features unrelated to record attachment storage SHALL remain out of M4 scope

### Requirement: M4 Voice Scope Is Raw Audio Storage Only

M4 SHALL store and play voice files, but SHALL NOT transcribe or semantically analyze voice.

#### Scenario: Voice feature is implemented

- WHEN the user records voice for a record
- THEN the system SHALL save the raw voice file and allow playback
- AND it SHALL NOT require speech-to-text, transcript search, or voice AI analysis for M4 acceptance

### Requirement: M4 Cover Scope Is Attachment-Based

M4 SHALL support record cover selection only from image attachments already associated with the record.

#### Scenario: User wants a cover

- WHEN a cover is added or changed
- THEN the selected cover SHALL come from the record's own image attachments
- AND a standalone cover upload flow SHALL remain outside M4

### Requirement: M4 Must Preserve Core Product Naming And Tone

M4 SHALL keep the V2.0 Mini Program oriented around private writing and time review.

#### Scenario: Agent updates M4 UI or copy

- WHEN visible copy references records, timeline, review, media, location, or AI
- THEN it SHALL remain quiet, private, and user-centered
- AND it SHALL preserve canonical naming such as "我的记录", "时光轴", and "时间回看"
- AND it SHALL NOT turn the product into a dashboard, social feed, content platform, or admin workflow

### Requirement: M4 Timeline Filtering Must Remain Focused

M4 SHALL improve timeline browsing for larger personal record collections through focused filters and incremental loading.

#### Scenario: Timeline filtering scope is implemented

- WHEN timeline filtering is added
- THEN it SHALL support one tag plus created-time year/month/day selection and pagination
- AND it SHALL preserve the timeline's quiet browsing role
- AND multiple-tag boolean search, keyword search, state/type filtering, and persisted filter preferences SHALL remain outside this M4 addition

## Accepted From C1 Agent Runtime MVP

> Accepted and archived on 2026-07-27 from openspec/changes/archive/2026-07-27-agent-runtime-mvp/. Wording below is the accepted C1 delta requirement body. Agent 完整契约见 `openspec/specs/agent-runtime/spec.md`。

### Requirement: Agent Must Behave As A Restrained Empathetic Companion

Agent 的产品行为 SHALL 符合安静、私密、克制、温柔的产品气质。

#### Scenario: Agent 参与写作引导

- GIVEN 用户主动召唤 Agent 协助书写当下
- WHEN Agent 回应
- THEN Agent SHALL 以温和提问逐步引导用户展开表达
- AND 回复长度 SHALL 与用户表达相称
- AND Agent SHALL NOT 长于用户表达或显得比用户更懂用户

#### Scenario: 用户希望停止

- GIVEN 用户表达不想继续对话
- WHEN Agent 回应
- THEN Agent SHALL 优雅收束
- AND Agent SHALL NOT 继续追问或催促

### Requirement: Agent Must Not Diagnose Overwrite Or Decide For The User

Agent SHALL NOT 越出朋友式陪伴的边界。

#### Scenario: 涉及心理状态的表达

- GIVEN 用户描述情绪困扰
- WHEN Agent 回应
- THEN Agent SHALL 共情回应
- AND Agent SHALL NOT 做心理诊断、医学建议或病症判断

#### Scenario: 涉及记录重要操作

- GIVEN 用户在对话中提到封存、解锁或删除
- WHEN Agent 回应
- THEN Agent MAY 给出建议
- AND Agent SHALL NOT 代替用户执行这些操作

#### Scenario: 涉及用户原文

- GIVEN 用户已写下正文
- WHEN Agent 参与
- THEN 用户原文 SHALL 保持不变
- AND 任何内容写入正文 SHALL 需要用户显式确认

### Requirement: Agent Must Remain Out Of Prohibited Product Directions

Agent 能力 SHALL NOT 引入被产品范围禁止的方向。

#### Scenario: 检查 Agent 能力边界

- GIVEN Agent Runtime 已上线
- WHEN 审查其产品能力
- THEN Agent SHALL NOT 包含语音转写、语音 AI 分析、情绪评分、诊断或效率仪表盘
- AND Agent SHALL NOT 引入社交动态或分享能力
- AND Agent SHALL NOT 主动推送、弹窗或提供未经请求的分析

## Accepted From C2 agent-tool-calling

> 来源：`openspec/changes/archive/2026-07-28-agent-tool-calling/`（C2，2026-07-28 用户验收）。
> C2 范围为原生 function calling 工具调用 + 代码级白名单 + 二段式用户确认；
> Memory / 系统化护栏 hardening / 决策链路可观测分别留给 C3 / C4 / C5。

### Requirement: Agent May Turn Conversation Into Action Only With User Consent

Agent SHALL 可以在对话中把建议变成行动，但每一次行动 SHALL 由用户当场同意。

#### Scenario: 对话中产生可执行建议

- GIVEN 用户正在与 Agent 进行写作引导对话
- WHEN Agent 判断存在可以帮用户完成的小动作
- THEN Agent SHALL 以一句克制的提议询问用户
- AND 行动 SHALL 只在用户同意后发生

#### Scenario: 用户不想要这个行动

- GIVEN Agent 给出了一个行动提议
- WHEN 用户表示不需要
- THEN 记录 SHALL 保持不变
- AND Agent SHALL NOT 追问同一个提议

### Requirement: Agent Must Remain A Friend Not A Manager

Agent SHALL 保持建议不代决的气质，SHALL NOT 接管属于用户的决定。

#### Scenario: 涉及封存这类不可逆决定

- GIVEN 用户在对话中提到想封存这条记录
- WHEN Agent 回应
- THEN Agent SHALL 建议用户自己去确认
- AND Agent SHALL NOT 代为完成封存

#### Scenario: 涉及用户原文

- GIVEN 用户请求 Agent 修改已经写下的文字
- WHEN Agent 回应
- THEN 用户原文 SHALL 保持不变
- AND Agent SHALL 只能追加用户自己表达过的内容

#### Scenario: 行动提议的表达长度

- GIVEN Agent 给出行动提议
- WHEN 提议呈现给用户
- THEN 提议 SHALL 简短克制
- AND SHALL NOT 附带分析、评分或诊断性说明

### Requirement: Agent Tool Scope Must Stay Within Product Boundaries

Agent 可执行的行动范围 SHALL 限于低风险、可逆的草稿整理动作。

#### Scenario: 审查 Agent 可执行的行动

- GIVEN 工具调用能力已上线
- WHEN 审查 Agent 可执行的行动清单
- THEN 清单 SHALL 只包含作用于草稿的可逆动作
- AND SHALL NOT 包含封存、解锁、删除、位置、封面或附件相关动作

#### Scenario: 三 Tab 与用户可见命名

- GIVEN 工具调用能力已上线
- WHEN 用户浏览小程序
- THEN 首页、时光轴、个人中心三个一级 Tab SHALL 保持不变
- AND 我的记录、时光轴、时间回看的用户可见命名 SHALL 保持不变

#### Scenario: 被动召唤原则

- GIVEN 工具调用能力已上线
- WHEN 用户没有主动与 Agent 对话
- THEN 系统 SHALL NOT 主动弹出行动提议
- AND 系统 SHALL NOT 推送或自动执行任何行动

## Accepted From C4 agent-guardrails-hardening

> 来源：openspec/changes/archive/2026-07-28-agent-guardrails-hardening/（C4，2026-07-28 用户验收）。
> 产品行为与 Agent 气质约束：进入正文的文字只能源于用户自己的表达；护栏对用户不可见。

### Requirement: User Original Expression Must Remain The Only Source Of Record Content

进入用户记录正文的文字 SHALL 只源于用户自己的表达。

#### Scenario: Agent 协助整理后的正文

- GIVEN 用户通过与 Agent 的对话产生了可回填正文的内容
- WHEN 该内容进入记录正文
- THEN 正文中 SHALL NOT 出现用户未曾表达过的观点、情绪或事实
- AND 用户既有正文 SHALL 逐字保持不变

#### Scenario: 用户回看已封存记录

- GIVEN 用户在未来解锁并回看一条记录
- WHEN 用户阅读正文
- THEN 正文 SHALL 只呈现用户当时自己说过的内容
- AND 产品 SHALL NOT 让用户读到由 Agent 代写的心情

#### Scenario: Agent 声称帮用户整理

- GIVEN Agent 在对话中表示可以帮用户整理已说过的内容
- WHEN 该整理结果被提供
- THEN 整理 SHALL 限于语序、冗余与措辞层面的组织
- AND 整理 SHALL NOT 补写用户没有表达的内容

### Requirement: Agent Must Stay Empathetic Without Diagnosing Or Claiming Actions

Agent SHALL 在共情的同时不做诊断、不声称代替用户完成决策。

#### Scenario: 用户描述疑似心理困扰

- GIVEN 用户描述了疑似心理困扰的感受
- WHEN Agent 回应
- THEN Agent SHALL 以共情方式回应
- AND Agent SHALL NOT 给出病症判断或医学建议

#### Scenario: 用户自己使用病症词

- GIVEN 用户在自己的表达中使用了某个病症词
- WHEN Agent 回应并复述用户的说法
- THEN 该复述 SHALL 被允许
- AND 产品 SHALL NOT 因此让 Agent 回避用户的感受

#### Scenario: 用户请求 Agent 代为封存

- GIVEN 用户在对话中请求 Agent 代为封存、解锁或删除记录
- WHEN Agent 回应
- THEN Agent SHALL 只建议用户自行确认
- AND Agent SHALL NOT 声称已经完成该操作

### Requirement: Guardrail Enforcement Must Stay Invisible And Non Disruptive

护栏生效 SHALL NOT 破坏对话体验，也 SHALL NOT 向用户暴露内部判定过程。

#### Scenario: 某次提议被护栏拦下

- GIVEN Agent 的一次行动提议因内容不忠实被拦下
- WHEN 用户继续这次对话
- THEN 用户 SHALL 只感知到这一轮没有出现行动建议
- AND 对话 SHALL 正常继续
- AND 已产生的内容 SHALL NOT 丢失

#### Scenario: 某轮回复被降级

- GIVEN 某轮 Agent 回复因越界被替换为安全兜底回复
- WHEN 用户读到该回复
- THEN 回复 SHALL 保持安静克制的语气
- AND 产品 SHALL NOT 向用户展示护栏判定过程或违规提示

#### Scenario: 护栏偶尔误伤合法整理

- GIVEN 一次合法的整理被护栏误判
- WHEN 用户继续使用
- THEN 后果 SHALL 限于少一次行动建议或少一段候选素材
- AND 用户记录 SHALL NOT 因此发生任何非预期变更

### Requirement: Guardrails Hardening Must Not Introduce Scoring Or Monitoring Surfaces

护栏加固 SHALL NOT 引入面向用户的评分、诊断或监控界面。

#### Scenario: 审查产品可见范围

- GIVEN C4 实现完成
- WHEN 审查用户可见界面
- THEN 三个一级 Tab 与用户可见命名 SHALL 保持不变
- AND 产品 SHALL NOT 新增情绪评分、心理诊断或 Agent 行为监控界面

#### Scenario: 被动召唤原则

- GIVEN 护栏检出一次越界
- WHEN 后端完成降级处理
- THEN 产品 SHALL NOT 因此向用户发起推送、弹窗或未请求的提示


## Accepted From C3a agent-memory-retrieval

> 来源：`openspec/changes/archive/2026-07-29-agent-memory-retrieval/`（C3 前半刀，2026-07-29 用户验收）。
> 范围：记忆能力的产品行为与气质边界。友人回看对话的产品行为留给 C3 后半刀。

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


## Accepted From C3b agent-review-chat

> 来源：`openspec/changes/archive/2026-07-29-agent-review-chat/`（C3 后半刀，2026-07-29 用户验收）。
> 范围：友人回看对话的产品行为与气质边界。**C3 两刀至此全部完成。**

### Requirement: Time Review Must Offer A Friend To Talk To, Not Just A Summary

时间回看 SHALL 在既有结构化摘要之外提供一个可以聊聊的对象。

#### Scenario: 解锁后的回看体验

- GIVEN 一条记录已经抵达并解锁
- WHEN 用户读完那时写下的内容
- THEN 用户 MAY 主动开启一段回看对话
- AND 既有的结构化摘要与回应能力 SHALL 保持不变

#### Scenario: 回看对话的气质

- GIVEN 用户在回看对话中说话
- WHEN Agent 回应
- THEN Agent SHALL 以朋友的方式陪他重新理解那时的自己
- AND Agent SHALL NOT 替他下结论
- AND Agent SHALL NOT 做心理诊断或归类
- AND Agent 的回复长度 SHALL 与用户的表达相称

#### Scenario: 被动召唤

- GIVEN 回看对话已上线
- WHEN 用户未主动开启
- THEN 系统 SHALL NOT 弹窗、推送或自动展开对话
- AND 系统 SHALL NOT 因记录解锁而主动发起对话

#### Scenario: 尊重结束

- GIVEN 用户不想继续这段回看对话
- WHEN 用户结束或不再回应
- THEN Agent SHALL 优雅收束
- AND Agent SHALL NOT 追问或挽留

### Requirement: Review Chat Must Not Alter What Was Written Back Then

回看对话 SHALL NOT 改变那时写下的内容。

#### Scenario: 那时的记录与此刻的对话

- GIVEN 用户在回看对话中说了很多此刻的想法
- WHEN 对话结束
- THEN 那条记录的正文 SHALL 逐字保持不变
- AND 此刻的整理 SHALL NOT 被写进那条记录
- AND 封存后不可变的位置、附件与封面 SHALL 保持不变

#### Scenario: 时间感的完整性

- GIVEN 用户几个月后再次打开这条记录
- WHEN 用户阅读它
- THEN 用户 SHALL 能确定读到的是当时写下的内容
- AND 记录中 SHALL NOT 混入回看时补写的句子

#### Scenario: 引用那时的话

- GIVEN Agent 在回看对话中提起那时写下的内容
- WHEN Agent 表达
- THEN Agent SHALL 说清那是过去哪个时候的事
- AND Agent SHALL NOT 让过去的表达听起来像用户此刻说的

### Requirement: Review Chat Scope Must Exclude Analysis And Advice Products

回看对话 SHALL NOT 演变为分析或建议型产品能力。

#### Scenario: 范围外的产品能力

- GIVEN 回看对话已落地
- WHEN 审查产品范围
- THEN 成长报告、情绪轨迹图表、行为建议清单与心理评估 SHALL NOT 被实现
- AND 回看对话 SHALL NOT 产生可分享的对外内容
- AND 三个一级 Tab 与 V2.0 用户可见命名 SHALL 保持不变

## Accepted From C5 agent-observability

> 来源：`openspec/changes/archive/2026-07-30-agent-observability/`（C5，2026-07-30 用户验收）。
> C5 **不新增任何用户可见能力**，前端零改动，因此本段全部是「不做什么」的边界声明。
> `miniapp-core` 无 C5 delta。

### Requirement: Agent Observability Must Stay Invisible To Product Users

Agent 决策轨迹 SHALL 是工程能力，SHALL NOT 改变任何用户可见的产品体验。

#### Scenario: 引入可观测后的用户体验

- GIVEN 决策轨迹已落地
- WHEN 用户使用写作引导或时间回看
- THEN 用户可见的界面、文案与交互 SHALL 与引入之前完全一致
- AND 用户 SHALL NOT 看到任何轨迹、指标或判定过程

#### Scenario: 三个一级 Tab 与用户可见命名

- GIVEN 可观测能力已落地
- WHEN 审查产品结构
- THEN 首页、时光轴、个人中心三个一级 Tab SHALL 保持不变
- AND 我的记录、时光轴、时间回看的用户可见命名 SHALL 保持不变

#### Scenario: Agent 的气质

- GIVEN Agent 的每一步现在都被记录
- WHEN 用户与 Agent 对话
- THEN Agent 的回复长度、语气与克制程度 SHALL 与引入之前一致
- AND Agent SHALL NOT 因为可观测而变得更像一个系统组件

### Requirement: Observability Must Not Become A Product Analytics Surface

可观测能力 SHALL NOT 演化为面向用户的分析或诊断能力。

#### Scenario: 情绪与行为分析

- GIVEN 轨迹包含每轮的判定与指标
- WHEN 评估可以从中衍生什么
- THEN 产品 SHALL NOT 由此生成情绪轨迹、趋势评分或用户画像
- AND 产品 SHALL NOT 由此生成任何诊断性结论

#### Scenario: 使用数据的呈现

- GIVEN 轨迹记录了对话轮次、耗时与失败
- WHEN 评估是否呈现给用户
- THEN 产品 SHALL NOT 向用户呈现使用统计或效率看板
- AND 这与产品「不是效率仪表盘」的定位一致

#### Scenario: 主动打扰

- GIVEN 轨迹可识别出用户的对话频率与模式
- WHEN 评估是否据此触达用户
- THEN 产品 SHALL NOT 据此发起推送、弹窗或提醒
- AND 被动召唤的约束 SHALL 保持不变
## Accepted From C7 agent-reflection-loop

> Accepted on 2026-08-03. 范围：不新增界面的用户可见 reply-only 终态行为。

### Requirement: Recoverable Guardrail Failures May Be Rewritten Once Before Fallback

#### Scenario: 回看回复缺少时间归属

- GIVEN Agent 复述了过去记录但没有说明时间归属
- WHEN 后端护栏判定为 `MISSING_TIME_ATTRIBUTION`
- THEN 系统 MAY 在同一轮内要求模型重写一次
- AND 重写要求 SHALL 只要求补明过去时间归属
- AND 用户 SHALL 只看到最终合格回复或既有安全兜底

#### Scenario: 收束素材保持既有边界

- GIVEN Agent 整理的素材包含用户未表达的内容
- WHEN 后端护栏判定为 `UNFAITHFUL`
- THEN 系统 SHALL NOT 在 C7 为素材发起 reflection
- AND 素材 SHALL 被丢弃
- AND 不合格素材 SHALL NOT 进入记录正文

#### Scenario: 严重越界或检查异常

- GIVEN Agent 输出命中诊断、代决、伪引用或检查异常
- WHEN 后端处置
- THEN 系统 SHALL NOT 为该输出提供第二次机会
- AND SHALL 继续使用既有 fail-closed 行为

### Requirement: Reflection Must Not Change Product Surface Or Temperament

#### Scenario: 用户界面

- GIVEN C7 被实现
- WHEN 用户使用小程序
- THEN SHALL NOT 新增 reflection 状态、按钮、页面、提示灯或技术术语
- AND 三个一级 Tab 与 V2 用户可见命名 SHALL 保持不变

#### Scenario: 产品气质

- GIVEN 模型被要求重写
- WHEN 生成最终回复或素材
- THEN 重写 SHALL 只为更忠实或更明确时间归属
- AND SHALL NOT 以更热情、更长、更诊断化或更主动为目标

#### Scenario: 失败终态

- GIVEN reflection 未成功
- WHEN 用户收到终态
- THEN reply SHALL 使用既有本地安全兜底；material SHALL 继续按既有规则保持缺失
- AND 系统 SHALL NOT 把本地兜底伪装成模型正常输出
