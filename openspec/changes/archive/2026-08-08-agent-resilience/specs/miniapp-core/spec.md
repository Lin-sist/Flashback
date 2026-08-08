# Miniapp Core Spec Delta：agent-resilience（C8）

> 规划草案。范围：既有 Agent 浮层内的失败提示与条件重试入口。

## MODIFIED Requirements

### 修订：`Agent Conversation Must Show Explicit Unavailable And Failure States`

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

## ADDED Requirements

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
