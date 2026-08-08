# Backend Core Spec Delta：agent-resilience（C8）

> 规划草案。范围：deadline 配置、client 异常边界、API 兼容字段与持久化边界。

## MODIFIED Requirements

### 修订：`Agent Conversation Client Timeout Must Exceed Backend AI Timeout`

#### Scenario: 多调用 Agent 请求

> C8 修订：旧条款中的 backend AI timeout 不能只理解为“单次 provider request timeout”；多调用路径必须受更小的整轮 deadline 约束。

- GIVEN 一次 Agent 请求可能执行两个 provider 子调用
- WHEN 配置 backend 与 frontend timeout
- THEN backend SHALL 具有小于 frontend 30000ms 的整轮 provider-work deadline
- AND 单次 provider timeout SHALL NOT 被误当成整轮请求上限
- AND frontend SHALL 预留 backend 返回结构化失败响应的时间

## ADDED Requirements

### Requirement: Provider Client Must Preserve Failure Identity Without Leaking Payloads

#### Scenario: HTTP 与解析边界

- GIVEN provider 返回 HTTP 错误或无效 2xx 响应
- WHEN `AgentModelClient` 抛出类型化失败
- THEN SHALL 保留稳定 category 与必要的内部 status identity
- AND SHALL NOT 在日志、trace 或 API 暴露 response body、request body、endpoint、credential 或异常 message

#### Scenario: interrupted

- GIVEN provider 调用线程被中断
- WHEN backend 捕获该失败
- THEN SHALL 恢复线程中断标记
- AND SHALL NOT 自动重试

### Requirement: Agent Provider Work Budget Must Be Request Scoped

#### Scenario: budget 配置

- GIVEN C8 默认整轮 provider-work budget 为 24000ms
- WHEN Agent request 开始
- THEN SHALL 创建一个 request-scope budget
- AND reply、reflection 与 material SHALL 共享该 budget
- AND SHALL NOT 通过为子流程创建新 budget 绕过总上限

#### Scenario: 单次上限

- GIVEN `app.ai.timeout-millis` 仍为 20000ms
- WHEN 发起下一 provider call
- THEN 实际 timeout SHALL 为单次上限与剩余整轮预算的较小值
- AND frontend 30000ms 与纯 DB request timeout SHALL 保持不变

### Requirement: Existing Agent Failure Contract Must Remain Stable

#### Scenario: backend response

- GIVEN Agent 返回 SUCCESS、UNAVAILABLE 或 FAILED
- WHEN 构造 `AgentSessionVO`
- THEN SHALL 继续使用既有 SUCCESS/UNAVAILABLE/FAILED 与 message 字段
- AND SHALL NOT 新增 retryable/failure-category 等外部字段
- AND SHALL NOT 暴露 HTTP status、异常类、provider body 或内部 endpoint

#### Scenario: pending turn

- GIVEN provider failure 后用户消息已持久化且 Assistant 未落库
- WHEN 用户主动重试
- THEN SHALL 复用同一 turn 与既有 attempt 语义
- AND SHALL NOT 重复插入用户消息或推进阶段机

### Requirement: Material Failure Must Not Break Record Lifecycle

#### Scenario: closing material 超时或失败

- GIVEN reply 已成功且 optional material 调用失败或预算耗尽
- WHEN 会话收束
- THEN Assistant reply 与 session end SHALL 保持成功
- AND material draft SHALL 为空
- AND turn outcome SHALL NOT 被反转为 provider FAILED

### Requirement: C8 Must Reuse Existing Storage And Dependencies

#### Scenario: 数据与构建边界

- GIVEN C8 被实现
- WHEN 审查 schema 与构建文件
- THEN SHALL NOT 新增数据库表或列
- AND SHALL 复用现有 `cause_type` / steps JSON 保存类型化分类
- AND SHALL NOT 新增 Maven dependency、provider credential、package 或 lockfile 改动
