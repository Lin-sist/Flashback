# Mini Program Core Spec / 小程序核心规格

## Purpose / 目的

定义《时光回序》用户侧 mini program 的稳定核心，使后续 V2.0 工作能够保持产品闭环与页面职责不偏移。

## Requirements

### Requirement: Product Must Center On Writing The Present / 产品 MUST 围绕“写下此刻”

mini program MUST 将“写下此刻”呈现为主要产品动作与情绪中心。

#### Scenario: User opens the home page / 用户打开首页

- **WHEN** 用户进入主体验
- **THEN** 页面将“写下当下”呈现为最清晰的可用动作
- **AND** 文案与布局避免把产品表达成对未来的焦虑

### Requirement: Core Loop Must Remain Complete / 核心闭环 MUST 保持完整

mini program MUST 支持从书写到时间回看的可演示闭环。

#### Scenario: Demonstration flow / 演示流程

- **WHEN** 演示沿着 V2.0 主路径进行
- **THEN** 它可以经过首页、新建记录、保存此刻 / 交给时间、我的记录或时光轴、抵达记录、时间回看，以及可选回信

### Requirement: V2.0 Naming Must Be Consistent / V2.0 命名 MUST 保持一致

面向用户的 V2.0 mini program MUST 在相关界面使用“我的记录”“时光轴”“时间回看”这些名称。

#### Scenario: Agent updates visible copy / Agent 更新可见文案

- **WHEN** 可见导航、标题、按钮或空状态涉及记录、时间线或回看
- **THEN** 文案使用“我的记录”“时光轴”“时间回看”
- **AND** 除非后续 spec 重新启用，否则“我的档案”“时间轴”“解锁页”“回看页”等旧名称视为历史命名

### Requirement: Record States Must Stay Legible / 记录状态 MUST 保持可读

用户侧体验 MUST 区分草稿、已封存、已解锁 / 已抵达记录。

#### Scenario: User browses records / 用户浏览记录

- **WHEN** 页面展示不同状态的记录
- **THEN** 每种状态在视觉与文字上都可理解
- **AND** 已封存记录不能表现得像草稿一样可编辑

### Requirement: Optional Reply Must Stay Optional / 回信 MUST 保持可选

时间回看后的回信 MUST 保持为可选动作，而不是强制完成步骤。

#### Scenario: User reviews an arrived record / 用户回看已抵达记录

- **WHEN** 用户进入时间回看页面
- **THEN** 用户可以理解“那时的我 / 现在的我”
- **AND** 用户可以不被强制写回信而离开

### Requirement: Page Responsibilities Must Stay Separated / 页面职责 MUST 保持分离

mini program 主要页面 MUST 保持清晰且互不混淆的职责。

#### Scenario: Agent changes a page / Agent 修改页面

- **WHEN** Agent 修改 homepage、record editor、record list、timeline、detail/review 或 user center
- **THEN** 页面仍然与其在核心闭环中的角色保持一致
- **AND** 页面不能被改造成无关的 admin、analytics 或通用 content-feed surface

### Requirement: One-Click Preview May Support The Demo Loop / 一键预览 MAY 支持演示闭环

V2.0 mini program MAY 提供 no-login preview path 用于演示。

#### Scenario: User enters preview mode / 用户进入预览模式

- **WHEN** 用户进入 one-click preview / no-login demo path
- **THEN** mini program 可以使用 preview data 演示核心闭环
- **AND** preview path 不表示生产认证被绕过

## Accepted From M4 Real Capability Completion

> Accepted and archived on 2026-07-27 from openspec/changes/archive/2026-07-27-m4-real-capability-completion/. Wording below is the accepted M4 delta requirement body.

### Requirement: M4 Real Mode Must Use Backend-Backed Data

The Mini Program SHALL use backend-backed data in authenticated real mode for M4 core surfaces.

#### Scenario: Authenticated user opens home, timeline, or time review

- GIVEN the user is authenticated and not in explicit preview mode
- WHEN the user opens home cards, timeline, record detail, or time review
- THEN the Mini Program SHALL request real backend data
- AND it SHALL NOT display preview/mock data as the real user state

#### Scenario: Preview mode is used

- GIVEN the user explicitly enters preview mode
- WHEN preview data is displayed
- THEN the Mini Program MAY use curated preview data
- AND preview state SHALL remain distinguishable from authenticated real mode

### Requirement: M4 Record Editor Must Support Real Location

The Mini Program SHALL support real record location input in the record editor.

#### Scenario: User adds current location

- GIVEN the user is editing a DRAFT record
- WHEN the user chooses current location and grants permission
- THEN the Mini Program SHALL save location through the backend-supported real path

#### Scenario: User picks location from map

- GIVEN the user is editing a DRAFT record
- WHEN the user selects a location from the map picker
- THEN the Mini Program SHALL save the selected location through the backend-supported real path

#### Scenario: User enters location manually

- GIVEN the user is editing a DRAFT record
- WHEN the user types a manual location
- THEN the Mini Program SHALL save the manual location through the backend-supported real path
- AND coordinates SHALL NOT be required for manual input

#### Scenario: Location is unavailable or denied

- GIVEN current location permission is denied or unavailable
- WHEN the user is editing a record
- THEN the Mini Program SHALL allow map picker or manual input where possible
- AND it SHALL NOT block writing the record solely because location is unavailable

### Requirement: M4 Record Editor Must Support Real Image Attachments

The Mini Program SHALL support real image attachments for DRAFT records.

#### Scenario: User adds images

- GIVEN the user is editing a DRAFT record
- WHEN the user selects images within accepted limits
- THEN the Mini Program SHALL compress images by default
- AND upload them through the backend-provided object-storage authorization
- AND show them as available only after backend verification succeeds

#### Scenario: User previews images

- GIVEN a record has available image attachments
- WHEN the user taps an image
- THEN the Mini Program SHALL allow image preview if access authorization succeeds

#### Scenario: User deletes draft image

- GIVEN the user is editing a DRAFT record with an image attachment
- WHEN the user deletes the image
- THEN the Mini Program SHALL call the supported backend path
- AND update the local UI only after the mutation succeeds or show a failure state

### Requirement: M4 Record Editor Must Support Real Voice Attachments

The Mini Program SHALL support real voice attachments as raw audio files.

#### Scenario: User records voice

- GIVEN the user is editing a DRAFT record
- WHEN the user records voice within accepted limits
- THEN the Mini Program SHALL upload the raw voice file through the backend-provided object-storage authorization
- AND show it as available only after backend verification succeeds

#### Scenario: User plays voice

- GIVEN a record has an available voice attachment
- WHEN the user taps play
- THEN the Mini Program SHALL play the voice file if access authorization succeeds

#### Scenario: User re-records or deletes draft voice

- GIVEN the user is editing a DRAFT record with a voice attachment
- WHEN the user re-records or deletes it
- THEN the Mini Program SHALL use supported backend mutation paths
- AND SHALL NOT allow the mutation after the record is sealed

### Requirement: M4 Cover Must Be Selected From Image Attachments

The Mini Program SHALL support record cover selection from the current record's image attachments.

#### Scenario: User selects cover

- GIVEN the user is editing a DRAFT record with at least one available image attachment
- WHEN the user chooses "添加封面" or changes cover
- THEN the Mini Program SHALL allow selecting one of that record's image attachments
- AND save the cover through the backend-supported real path

#### Scenario: No image exists

- GIVEN the DRAFT record has no image attachment
- WHEN the user attempts to add a cover
- THEN the Mini Program SHALL guide the user to add an image first
- AND SHALL NOT upload a standalone cover image in M4

#### Scenario: Record is sealed or unlocked

- GIVEN a record is SEALED or UNLOCKED
- WHEN the record is displayed
- THEN cover is read-only
- AND cover mutation controls SHALL NOT be shown as available actions

### Requirement: M4 Timeline And Home Should Show Real Covers

Timeline and home record cards SHALL show the selected cover when available in real mode.

#### Scenario: Record has cover

- GIVEN a real-mode timeline or home card represents a record with cover
- WHEN the card is rendered
- THEN the Mini Program SHALL display the cover through private-access-safe media access

#### Scenario: Record has no cover

- GIVEN a real-mode timeline or home card represents a record without cover
- WHEN the card is rendered
- THEN the Mini Program SHALL show a safe fallback visual
- AND it SHALL NOT substitute unrelated preview media

### Requirement: M4 Timeline Must Provide Calm Filtered Browsing

The Mini Program SHALL support focused timeline filtering without turning the page into a dashboard or general-purpose search surface.

#### Scenario: User applies filters

- GIVEN the user opens the timeline filter sheet
- WHEN the user selects at most one tag and a year, month, or exact day and applies the filter
- THEN the Mini Program SHALL request page 1 using the accepted tag/date query
- AND tag and date selections SHALL combine with AND semantics
- AND the page SHALL show a compact applied-filter summary

#### Scenario: User resets filters

- GIVEN timeline filters are active
- WHEN the user resets them
- THEN the Mini Program SHALL clear tag/date selections
- AND restart unfiltered timeline loading from page 1

#### Scenario: User loads more records

- GIVEN the current timeline page reports `hasMore`
- WHEN the user reaches the load-more boundary
- THEN the Mini Program SHALL request the next page
- AND merge repeated year-month groups without duplicate record ids

#### Scenario: Filtered result is empty or fails

- GIVEN the user applies a valid filter
- WHEN no records match or the request fails
- THEN the Mini Program SHALL distinguish a filtered empty state from a retryable request failure
- AND it SHALL NOT replace real data with preview records

#### Scenario: Preview timeline is filtered

- GIVEN the user explicitly entered preview mode
- WHEN the user applies or resets timeline filters or loads another page
- THEN preview data SHALL follow the same query and pagination semantics
- AND preview state SHALL remain isolated from authenticated real mode

### Requirement: M4 Time Review Must Show Real Location And Media

Time review SHALL show real location and media for unlocked records when present.

#### Scenario: User opens unlocked time review

- GIVEN an authenticated user owns an UNLOCKED record with location, image attachments, voice attachments, or cover
- WHEN the user opens 时间回看
- THEN the Mini Program SHALL display those assets as read-only context
- AND image preview and voice playback SHALL use authorized real media access

#### Scenario: Media access fails

- GIVEN media exists but signed URL generation, loading, or playback fails
- WHEN the user views time review
- THEN the Mini Program SHALL show a retryable or understandable failure state
- AND it SHALL NOT silently replace the media with preview data

## Accepted From C1 Agent Runtime MVP

> Accepted and archived on 2026-07-27 from openspec/changes/archive/2026-07-27-agent-runtime-mvp/. Wording below is the accepted C1 delta requirement body. Agent 完整契约见 `openspec/specs/agent-runtime/spec.md`。

### Requirement: Record Editor Must Provide A Passive Agent Conversation Entry

记录编辑页 SHALL 提供一个用户主动触发的 Agent 对话入口，且不改变编辑器主路径。

#### Scenario: 用户打开对话

- GIVEN 一个已登录用户在编辑草稿记录
- WHEN 用户点击对话入口
- THEN 小程序 SHALL 以半屏浮层形式展示对话界面
- AND 用户 SHALL 能随时关闭该浮层

#### Scenario: 用户未触发对话

- GIVEN 用户进入记录编辑页
- WHEN 页面完成加载
- THEN 小程序 SHALL NOT 自动展开对话界面
- AND 小程序 SHALL NOT 弹窗提示用户与 Agent 对话

#### Scenario: 对话界面在消息累积后仍可操作

- GIVEN 对话消息数量已超过浮层可视高度
- WHEN 用户继续对话
- THEN 消息区 SHALL 在浮层内部滚动
- AND 输入区与发送操作 SHALL 保持在可视区内可点击
- AND 输入区 SHALL NOT 覆盖已有消息内容

#### Scenario: 三 Tab 与既有命名

- GIVEN Agent 对话入口已上线
- WHEN 检查一级导航与用户可见命名
- THEN 首页、时光轴、个人中心三个一级 Tab SHALL 保持不变
- AND 我的记录、时光轴、时间回看等 V2.0 命名 SHALL 保持不变

### Requirement: Agent Conversation Must Support Interruption And Recovery

对话 SHALL 可中断并可恢复。

#### Scenario: 用户中断后重新进入

- GIVEN 用户在对话进行中关闭了浮层
- WHEN 用户再次点击对话入口
- THEN 小程序 SHALL 恢复该记录上进行中的会话与已有消息
- AND 用户 MAY 选择结束当前会话后重新开始

#### Scenario: 会话已结束后再次进入

- GIVEN 该记录上的会话已结束
- WHEN 用户再次点击对话入口
- THEN 小程序 SHALL 开启一个新的会话

### Requirement: Agent Material Must Require Explicit User Confirmation

对话产生的素材 SHALL 只在用户显式确认后写入记录正文。

#### Scenario: 用户确认使用素材

- GIVEN 对话结束并给出素材草稿
- WHEN 用户选择用作正文
- THEN 小程序 SHALL 通过既有记录更新流程写入正文
- AND 已有正文 SHALL 被保留，素材以追加方式写入

#### Scenario: 用户拒绝使用素材

- GIVEN 素材草稿已展示
- WHEN 用户选择不使用
- THEN 正文 SHALL 保持不变

### Requirement: Agent Conversation Must Show Explicit Unavailable And Failure States

对话失败 SHALL 以克制方式明确告知用户。

#### Scenario: Agent 不可用或失败

- GIVEN 后端返回不可用或失败状态
- WHEN 小程序处理该响应
- THEN 小程序 SHALL 展示克制的不可用或失败提示并允许重试
- AND 小程序 SHALL NOT 展示本地生成的内容冒充 Agent 回复
- AND 用户已输入的内容 SHALL NOT 丢失

#### Scenario: 上一轮回复未完成时继续输入

- GIVEN 上一轮用户消息已提交但 Agent 回复失败
- WHEN 用户回到该会话
- THEN 小程序 SHALL 提示需要先重试上一轮
- AND 输入新内容 SHALL 被暂时禁用直至该轮完成

#### Scenario: 演示模式下的 Agent 入口

- GIVEN 当前处于显式演示模式且没有真实登录凭证
- WHEN 用户触发 Agent 对话
- THEN 小程序 SHALL NOT 访问真实 Agent 服务
- AND 演示行为 SHALL 与已认证真实路径保持隔离

## Accepted From C2 agent-tool-calling

> 来源：`openspec/changes/archive/2026-07-28-agent-tool-calling/`（C2，2026-07-28 用户验收）。
> C2 范围为原生 function calling 工具调用 + 代码级白名单 + 二段式用户确认；
> Memory / 系统化护栏 hardening / 决策链路可观测分别留给 C3 / C4 / C5。

### Requirement: Tool Proposal Must Be Presented As An Explicit Confirmation Affordance

前端 SHALL 在既有 Agent 对话浮层内以显式确认控件呈现工具提议，SHALL NOT 自动执行。

#### Scenario: 收到工具提议

- GIVEN 用户正在与 Agent 对话
- WHEN 后端返回一个待确认的工具提议
- THEN 前端 SHALL 在对话浮层内展示该提议与接受、拒绝两个选项
- AND 前端 SHALL NOT 在用户未选择前发起执行请求

#### Scenario: 用户接受提议

- GIVEN 一个待确认提议已展示
- WHEN 用户点击接受
- THEN 前端 SHALL 调用工具确认端点
- AND 执行成功后前端 SHALL 使编辑器中的正文与标签与后端状态保持一致

#### Scenario: 用户拒绝提议

- GIVEN 一个待确认提议已展示
- WHEN 用户点击拒绝
- THEN 前端 SHALL 通知后端该提议被拒绝
- AND 编辑器中的正文与标签 SHALL 保持不变

#### Scenario: 用户重复点击接受

- GIVEN 一次确认请求正在进行中
- WHEN 用户再次点击接受
- THEN 前端 SHALL NOT 重复发起执行请求

### Requirement: Tool Execution Failure Must Be Surfaced To The User

工具执行失败 SHALL 对用户明确可见，SHALL NOT 静默或显示为成功。

#### Scenario: 执行因记录已封存而失败

- GIVEN 目标记录在提议之后已被封存
- WHEN 用户接受该提议
- THEN 前端 SHALL 展示明确的失败原因
- AND 前端 SHALL NOT 提示操作已完成

#### Scenario: 执行因服务不可用而失败

- GIVEN 后端返回不可用或失败状态
- WHEN 前端处理该响应
- THEN 前端 SHALL 展示可读的失败提示
- AND 对话浮层 SHALL 保持可用，已产生的素材 SHALL NOT 丢失

### Requirement: Tool Confirmation Must Stay Within Existing Editor Surface And Preview Isolation

工具确认交互 SHALL 保持在既有记录编辑页对话浮层内，并遵循既有 preview 隔离约定。

#### Scenario: 确认交互的界面位置

- GIVEN 工具确认能力已上线
- WHEN 用户在记录编辑页与 Agent 对话
- THEN 确认交互 SHALL 出现在既有对话浮层内
- AND SHALL NOT 新增一级 Tab、页面路由或全局弹窗

#### Scenario: preview 会话下的工具确认

- GIVEN 当前为未认证的 preview 会话
- WHEN 触发工具确认请求
- THEN 前端 SHALL 拒绝该请求且 SHALL NOT 访问真实服务
- AND 前端 SHALL NOT 以本地伪造结果冒充真实执行成功


## Accepted From C3b agent-review-chat

> 来源：`openspec/changes/archive/2026-07-29-agent-review-chat/`（C3 后半刀，2026-07-29 用户验收）。
> 范围：时间回看页的回看对话入口与浮层。既有 4 条 Agent UI 条款以「记录编辑页」为主语，本段为新增而非改写。
> 微信真机手验已通过（2026-07-29 用户执行）。

### Requirement: Unlocked Record Page Must Provide A Passive Review Chat Entry

时间回看页 SHALL 提供一个用户主动触发的回看对话入口。

#### Scenario: 已解锁记录上的入口

- GIVEN 用户打开一条已解锁的记录
- WHEN 页面渲染完成
- THEN 页面 SHALL 展示一个克制的回看对话入口
- AND 小程序 SHALL NOT 自动开启对话
- AND 小程序 SHALL NOT 弹窗或自动展开对话界面

#### Scenario: 未解锁记录上的入口

- GIVEN 用户打开一条草稿或已封存未解锁的记录
- WHEN 页面渲染完成
- THEN 页面 SHALL NOT 展示回看对话入口

#### Scenario: 入口不抢占既有主动作

- GIVEN 该记录可以留下回应
- WHEN 页面展示回看对话入口
- THEN 留下回应 SHALL 仍是视觉上的主动作
- AND 三个一级 Tab 与 V2.0 用户可见命名 SHALL 保持不变

### Requirement: Review Chat Sheet Must Be Mutually Exclusive With The Reply Sheet

回看对话浮层与回应浮层 SHALL 互斥。

#### Scenario: 打开回看对话时

- GIVEN 回应浮层处于打开状态
- WHEN 用户打开回看对话
- THEN 回应浮层 SHALL 被关闭

#### Scenario: 打开回应浮层时

- GIVEN 回看对话浮层处于打开状态
- WHEN 用户打开回应浮层
- THEN 回看对话浮层 SHALL 被关闭

#### Scenario: 回应功能本身

- GIVEN 回看对话已上线
- WHEN 用户留下回应
- THEN 回应的既有行为与限制 SHALL 保持不变

### Requirement: Review Chat Sheet Must Not Offer Tool Confirmation Or Material Fill Back

回看对话浮层 SHALL NOT 提供工具确认与素材回填入口。

#### Scenario: 浮层的结构

- GIVEN 回看对话浮层被打开
- WHEN 审查其界面元素
- THEN 界面 SHALL NOT 包含工具确认条
- AND 界面 SHALL NOT 包含把内容写入记录正文的入口
- AND 这些元素 SHALL 在结构上不存在，而非被隐藏

#### Scenario: 对话不会改动记录

- GIVEN 用户完成一次回看对话
- WHEN 用户返回记录页面
- THEN 记录内容 SHALL 保持不变
- AND 小程序 SHALL 让用户知道这段对话不会改动该记录

### Requirement: Review Chat Must Show Explicit Unavailable And Failure States

回看对话的失败 SHALL 以克制方式明确告知用户。

#### Scenario: 服务不可用

- GIVEN 后端返回不可用状态
- WHEN 用户打开回看对话
- THEN 小程序 SHALL 明确告知当前聊不了
- AND 小程序 SHALL NOT 展示本地生成的回复冒充 Agent 回应

#### Scenario: 某一轮失败

- GIVEN 某一轮对话失败
- WHEN 用户查看对话
- THEN 小程序 SHALL 提供重试入口
- AND 已发出的用户消息 SHALL NOT 丢失

#### Scenario: 会话已结束

- GIVEN 回看会话已结束
- WHEN 用户查看浮层
- THEN 小程序 SHALL 收起输入区
- AND 小程序 SHALL 以温和方式说明对话已结束

### Requirement: Review Chat Must Not Expose Retrieval Internals

回看对话 SHALL NOT 向用户暴露检索过程。

#### Scenario: 界面上的检索信息

- GIVEN 某一轮对话使用了历史记录
- WHEN 用户查看回复
- THEN 用户 SHALL NOT 看到命中数量、记录清单或任何检索状态提示

## Accepted From C8 agent-resilience

> Accepted on 2026-08-08. C8 不新增前端字段或页面；小程序继续使用既有 status/message、
> error card 与主动同轮重试入口。

### Requirement: Agent Conversation Must Show Explicit Unavailable And Failure States

#### Scenario: Agent 不可用或失败

- GIVEN backend 返回不可用或失败状态
- WHEN 小程序处理该响应
- THEN 小程序 SHALL 展示克制、温和且可理解的失败提示
- AND SHALL 保留既有“再试一次”入口以完成 pending 用户轮次
- AND 小程序 SHALL NOT 展示本地生成内容冒充 Agent 回复
- AND 用户已输入的内容 SHALL NOT 丢失

#### Scenario: 契约保持

- GIVEN C8 增加 backend 内部错误分类
- WHEN 小程序处理 Agent response
- THEN SHALL 继续只依赖既有 status/message 与 session/message 状态
- AND SHALL NOT 要求新增 failure category 或 retryable 字段

### Requirement: Agent Failure UI Must Not Expose Infrastructure Details

#### Scenario: 技术失败类别

- GIVEN backend 内部分类为 timeout、429、auth/config、5xx、invalid response 或其他错误
- WHEN 小程序展示错误卡片
- THEN SHALL NOT 展示 HTTP status、provider、endpoint、鉴权、配置或异常类名
- AND SHALL 使用 backend 提供的克制 message 与既有 retry 机制

### Requirement: Provider Failure Must Preserve The Recoverable Conversation

#### Scenario: pending 用户轮次

- GIVEN provider failure 后最后一条消息仍是用户消息
- WHEN 用户看到失败状态
- THEN 错误卡片与已经提交的用户消息 SHALL 保留
- AND “再试一次”入口 SHALL 保留
- AND 新输入 SHALL 继续禁用直至该轮完成
- AND 小程序 SHALL NOT 清空会话、用户输入或回退到 Preview mock success

### Requirement: Resilience Must Stay Inside The Existing Agent Surface

#### Scenario: UI 范围

- GIVEN C8 被实现
- WHEN 审查小程序变更
- THEN SHALL 只调整既有 Agent 浮层的错误态与重试条件
- AND SHALL NOT 新增页面、一级 Tab、诊断 dashboard、技术状态灯或 major visual reconstruction

## Accepted From C9 Agent Temporal Intelligence

### Requirement: Temporal Replies Must Reuse Existing Conversation Surfaces

#### Scenario: 时间感知回复

- GIVEN backend 返回一条通过护栏的时间感知 Agent 回复
- WHEN小程序展示该回复
- THEN SHALL 复用既有写作引导或回看对话消息表面
- AND SHALL NOT 为 distance、pattern、score 或 trend 新增卡片、徽标、图表、字段或页面

#### Scenario: temporal overreach 降级

- GIVEN backend 因 `TEMPORAL_OVERREACH` 返回既有安全兜底
- WHEN 小程序展示结果
- THEN SHALL 按现有消息/状态契约呈现
- AND SHALL NOT 暴露内部违规类型、匹配词或检索证据

### Requirement: Time Review UI Must Remain Quiet And User Initiated

#### Scenario: 回看入口与浮层

- GIVEN C9 已启用
- WHEN 用户打开一条 UNLOCKED 记录
- THEN 既有回看入口、回应主动作与浮层互斥行为 SHALL 保持
- AND 小程序 SHALL NOT 自动开启对话、主动展示模式分析或新增时间智能入口

#### Scenario: 无新增分析表面

- GIVEN temporal policy 识别到有限重复证据
- WHEN 页面渲染
- THEN SHALL NOT 展示情绪趋势、周期、评分、成长报告或跨记录清单
- AND 三个一级 Tab 与 V2.0 用户可见命名 SHALL 保持不变
