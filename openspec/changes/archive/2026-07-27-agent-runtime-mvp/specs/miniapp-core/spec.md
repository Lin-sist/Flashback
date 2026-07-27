# Miniapp Core Spec Delta（C1 `agent-runtime-mvp`）

> 本 delta 只在 `miniapp-core` 留最小可检索条款；Agent 完整契约见 `specs/agent-runtime/spec.md`。

## ADDED Requirements

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
- AND 写入前 SHALL NOT 覆盖用户已写内容而不经用户确认

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

#### Scenario: 演示模式下的 Agent 入口

- GIVEN 当前处于显式演示模式且没有真实登录凭证
- WHEN 用户触发 Agent 对话
- THEN 小程序 SHALL NOT 访问真实 Agent 服务
- AND 演示行为 SHALL 与已认证真实路径保持隔离
