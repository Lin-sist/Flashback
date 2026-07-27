# Backend Core Spec Delta（C1 `agent-runtime-mvp`）

> 本 delta 只在 `backend-core` 留最小可检索条款；Agent 完整契约见 `specs/agent-runtime/spec.md`。

## ADDED Requirements

### Requirement: Agent Conversation Endpoints Must Be Authenticated And Owner Scoped

后端 SHALL 在 `/api/agent/**` 下提供 Agent 对话端点，纳入既有 `/api/**` 鉴权链路，并使用既有统一响应与错误码体系。

#### Scenario: 已登录用户访问 Agent 端点

- GIVEN 一个携带有效凭证的用户
- WHEN 该用户开启会话、读取会话、追加消息或结束会话
- THEN 后端 SHALL 校验会话归属于该用户
- AND 响应 SHALL 使用既有统一响应包装

#### Scenario: 未登录或跨用户访问 Agent 端点

- GIVEN 请求没有有效凭证，或目标会话属于其他用户
- WHEN 访问任一 Agent 对话端点
- THEN 后端 SHALL 返回未授权、拒绝或安全的未找到响应
- AND 后端 SHALL NOT 泄露其他用户的会话内容

### Requirement: Agent Runtime Must Not Change Existing AI Endpoint Contracts

引入 Agent Runtime SHALL NOT 改变既有单轮 AI 能力的对外契约。

#### Scenario: 既有 AI 端点被调用

- GIVEN Agent Runtime 已上线
- WHEN 客户端调用既有写作提示、记录整理或阶段总结端点
- THEN 请求与响应契约 SHALL 保持不变
- AND 既有的成功、不可用、失败与本地兜底语义 SHALL 保持不变

### Requirement: Agent Session Persistence Must Follow Owner Scoped Business Storage

Agent 会话与消息 SHALL 作为归属用户的业务数据持久化。

#### Scenario: 会话数据被持久化

- GIVEN 用户开启 Agent 会话并产生消息
- WHEN 后端持久化会话与消息
- THEN 每条记录 SHALL 携带所属用户标识
- AND 时间语义 SHALL 使用既有业务时区约定
- AND 用户被删除时其会话与消息 SHALL 被级联清理

#### Scenario: 会话内容出现在日志中

- GIVEN 后端记录 Agent 相关日志
- WHEN 日志被写出
- THEN 日志 SHALL NOT 包含对话原文或用户日记原文
