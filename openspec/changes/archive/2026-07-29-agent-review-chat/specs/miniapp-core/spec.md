# Miniapp Core Spec Delta：agent-review-chat（C3 后半刀）

> 本文件是 delta。范围：时间回看页的回看对话入口与浮层。
> 既有 4 条 Agent UI 条款以「记录编辑页」为主语，本刀**新增**而非改写它们。

---

## ADDED Requirements

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
