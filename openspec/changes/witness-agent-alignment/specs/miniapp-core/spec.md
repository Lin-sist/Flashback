# Delta Spec：miniapp-core（P4.1 Witness Agent Alignment）

## MODIFIED Requirements

### Requirement: Record Editor Must Provide A Passive Agent Conversation Entry

记录编辑页 SHALL 在 active DRAFT 或 SAVED 上提供用户主动触发的 witness 写作帮助入口，且不改变“留下此刻 → 保存 → 结束”的主路径。真正开启 provider 会话前，用户 SHALL 明确选择“先听我说”或“帮我理一理”。

#### Scenario: 用户打开写作帮助

- GIVEN 一个已登录用户在编辑 active DRAFT 或 SAVED 记录
- WHEN 用户主动点击对话入口
- THEN 小程序 SHALL 先展示两项同等权重的 conversation intent 选择
- AND 选择前 SHALL NOT 请求后端生成 Agent opening
- AND 选择后 SHALL 以既有半屏浮层展示 witness 对话
- AND 用户 SHALL 能随时关闭

#### Scenario: 用户未触发对话

- GIVEN 用户进入记录编辑页
- WHEN 页面完成加载或记录保存成功
- THEN 小程序 SHALL NOT 自动展开选择或对话
- AND SHALL NOT 把 Agent 表现为完成记录的必经步骤

#### Scenario: 用户未作选择

- GIVEN intent 选择面板已展示
- WHEN 用户关闭或离开而没有选择
- THEN 小程序 SHALL NOT 创建或恢复 provider 会话
- AND SHALL NOT 用默认倒计时、预选项或关系文案推动选择

#### Scenario: 不可编辑状态

- GIVEN 记录为 SEALED 或 UNLOCKED
- WHEN 页面决定 Agent 入口
- THEN SHALL NOT 提供 WRITING_GUIDANCE 入口
- AND UNLOCKED MAY 继续使用既有独立 REVIEW_CHAT 入口

## ADDED Requirements

### Requirement: Witness Entry Must Give Equal Weight To Listening And Untangling

小程序 SHALL 以简短、非诊断、非关系化的方式解释两种意图，不得暗示“帮我理一理”更完整或更正确。

#### Scenario: 展示意图选择

- GIVEN 用户主动打开 witness entry
- WHEN 选择面板显示
- THEN SHALL 展示“先听我说”与“帮我理一理”
- AND 两项 SHALL 具有同等视觉权重且默认均未选中
- AND 文案 SHALL NOT 使用 AI 朋友、最懂你、一直陪伴或成长分析叙事

#### Scenario: 先听我说

- GIVEN 用户选择“先听我说”
- WHEN start 请求发出
- THEN 请求 SHALL 携带 `conversationIntent=LISTEN`
- AND UI SHALL 说明这一方式以听见和回应为主，不主动提问

#### Scenario: 帮我理一理

- GIVEN 用户选择“帮我理一理”
- WHEN start 请求发出
- THEN 请求 SHALL 携带 `conversationIntent=UNTANGLE`
- AND UI SHALL 说明每次至多问一个可跳过的问题，不要求结论

### Requirement: Witness Conversation Surface Must Not Display A Preset Stage Journey

写作对话浮层 SHALL 显示当前用户意图与结束状态，而不是情绪、困惑、核心问题、期待的预设旅程。

#### Scenario: LISTEN 会话

- GIVEN session intent 为 LISTEN 且 session active
- WHEN 浮层显示 header
- THEN 标题 SHALL 为“先听你说”或等义克制文案
- AND SHALL NOT 显示阶段进度、情绪分类或目标达成状态

#### Scenario: UNTANGLE 会话

- GIVEN session intent 为 UNTANGLE 且 session active
- WHEN 浮层显示 header
- THEN 标题 SHALL 为“一起理一理”或等义克制文案
- AND SHALL NOT 显示“找到真正问题”或“未来期待”为必经目标

#### Scenario: 会话结束

- GIVEN session 已进入 CLOSING 或 ENDED
- WHEN 浮层呈现终态
- THEN MAY 显示“说到这里已经很好”
- AND SHALL NOT 催促继续、要求总结或制造未完成焦虑

### Requirement: User Must Be Able To Switch Witness Intent Explicitly

ACTIVE WRITING_GUIDANCE 浮层 SHALL 提供轻量的 intent 切换；该操作 SHALL 保持用户可见、可失败且不假装生成新回复。

#### Scenario: 切换成功

- GIVEN 用户正在自己的 ACTIVE WRITING_GUIDANCE session
- WHEN 用户从 LISTEN 切换到 UNTANGLE 或反向切换
- THEN 小程序 SHALL 调用 owner-scoped intent endpoint
- AND 成功后 SHALL 显示 backend 返回的真实 intent
- AND 历史消息 SHALL 保持

#### Scenario: 切换失败

- GIVEN intent switch 请求失败或被 backend 拒绝
- WHEN 小程序处理响应
- THEN UI SHALL 保持原 intent
- AND SHALL 显示克制、可行动的失败提示
- AND SHALL NOT 显示假成功或本地先改后不回滚

#### Scenario: 切换请求进行中

- GIVEN intent switch 正在提交
- WHEN 用户再次点击
- THEN 重复提交 SHALL 被禁用
- AND 消息、工具、素材或 finish 操作 SHALL NOT 被误触发

### Requirement: Brief Answers And Ending Must Remain Low Pressure

小程序 SHALL 允许用户只说很短的话或随时结束，不得用 UI 把对话重新变成访谈流程。

#### Scenario: 用户给出极短回答

- GIVEN 用户发送一个极短回答
- WHEN 后端返回 witness reply
- THEN UI SHALL 正常呈现该回复
- AND SHALL NOT 追加“再多说一点”、阶段未完成或必须回答的前端提示

#### Scenario: 用户主动结束

- GIVEN 任一 ACTIVE WRITING_GUIDANCE session
- WHEN 用户点击“先聊到这里”
- THEN 小程序 SHALL 允许从当前 witness 状态结束
- AND SHALL NOT 要求先完成问题、阶段或总结

#### Scenario: 关闭后恢复

- GIVEN 用户关闭浮层但未结束 session
- WHEN 用户再次打开并选择一种 intent
- THEN 小程序 SHALL 恢复 existing session 与消息
- AND 显式选择的新 intent MAY 更新该会话
- AND SHALL NOT 创建重复 opening 或丢失待 retry 状态

### Requirement: Witness UI Must Preserve Confirmation Failure And Preview Boundaries

P4.1 UI SHALL 复用既有 Agent surface，不得绕过工具、素材、失败或 Preview 契约。

#### Scenario: 工具或素材出现

- GIVEN backend 返回 pending tool proposal 或 material draft
- WHEN 浮层呈现
- THEN 既有显式确认/拒绝交互 SHALL 保持
- AND intent 选择 SHALL NOT 被视为执行工具或写入正文的同意

#### Scenario: Provider 不可用

- GIVEN start、turn 或 reflection 失败
- WHEN 小程序处理响应
- THEN SHALL 呈现既有显式 unavailable/failure/retry 状态
- AND SHALL NOT 用 witness 文案掩盖失败或冒充成功回复

#### Scenario: Preview

- GIVEN 当前只有 Preview session 而无真实登录凭证
- WHEN 用户尝试选择、开始或切换 witness intent
- THEN service boundary SHALL fail-closed
- AND 真实 Agent API 调用数 SHALL 为 0
- AND 小程序 SHALL NOT 生成本地假会话
