# Delta Spec：backend-core（P4.1 Witness Agent Alignment）

## ADDED Requirements

### Requirement: Writing Guidance Conversation Intent Must Be Persisted With Owner Scope

后端 SHALL 将 WRITING_GUIDANCE conversation intent 作为 owner-scoped session 业务状态持久化；允许值 SHALL 仅为 `LISTEN` 与 `UNTANGLE`。

#### Scenario: 新建写作引导会话

- GIVEN 已认证用户为自己 owned 的 active DRAFT 或 SAVED 开启 WRITING_GUIDANCE
- WHEN 请求携带有效 conversation intent
- THEN 后端 SHALL 将该值持久化到 `agent_session`
- AND session VO SHALL 返回相同值

#### Scenario: 旧客户端未传意图

- GIVEN WRITING_GUIDANCE start 请求未携带 conversation intent
- WHEN 后端创建或恢复会话
- THEN 后端 SHALL 使用 `LISTEN`
- AND 后端 SHALL NOT 继续以缺省固定阶段访谈代替用户选择

#### Scenario: 回看会话

- GIVEN session purpose 为 REVIEW_CHAT
- WHEN 会话被持久化或返回
- THEN conversation intent SHALL 为 null
- AND 后端 SHALL NOT 给回看会话伪造 LISTEN 或 UNTANGLE

#### Scenario: 跨用户读取

- GIVEN 目标 session 属于其他用户
- WHEN 当前用户读取或尝试修改 conversation intent
- THEN 后端 SHALL 拒绝或返回安全的未找到响应
- AND SHALL NOT 泄露 session intent、message 或 record 内容

### Requirement: Session Start And Intent Switch Endpoints Must Be Backend Authoritative

既有 session start endpoint SHALL 接受 WRITING_GUIDANCE conversation intent；独立 intent switch endpoint SHALL 只改变同一 ACTIVE 会话的意图。

#### Scenario: 开启会话携带意图

- GIVEN `POST /api/agent/sessions` 请求为 WRITING_GUIDANCE
- WHEN body 携带 `conversationIntent=LISTEN|UNTANGLE`
- THEN 后端 SHALL 创建或恢复相应 owner-scoped 会话
- AND intent SHALL 成为后续 turn policy 的权威来源

#### Scenario: REVIEW_CHAT 携带写作意图

- GIVEN session start purpose 为 REVIEW_CHAT
- WHEN 请求同时携带 conversation intent
- THEN 后端 SHALL 返回参数错误
- AND SHALL NOT 静默忽略该歧义字段

#### Scenario: 显式切换意图

- GIVEN owner 拥有一个 ACTIVE WRITING_GUIDANCE session
- WHEN 调用 `PUT /api/agent/sessions/{sessionId}/intent` 携带有效新值
- THEN 后端 SHALL 幂等持久化新值并返回更新后的 session
- AND SHALL NOT 调用 provider、推进 turn/stage、执行工具或生成素材

#### Scenario: 不允许切换的状态

- GIVEN session 已 END、purpose 为 REVIEW_CHAT、属于其他用户或上一轮用户消息仍等待 retry
- WHEN 调用 intent switch
- THEN 后端 SHALL fail-closed 并保持原 intent
- AND 用户 SHALL 收到稳定、非基础设施泄露的错误

### Requirement: Witness Session Migration Must Preserve Historical Facts

P4.1 schema migration SHALL 使 WRITING_GUIDANCE intent 与 WITNESS stage 可恢复，同时不得改写历史 message/trace 事实。

#### Scenario: Schema 增量

- GIVEN 当前 `agent_session` 不含 conversation intent
- WHEN P4.1 migration 执行
- THEN SHALL 新增可表达 LISTEN/UNTANGLE 且允许 REVIEW_CHAT 为 null 的列与约束
- AND baseline MySQL schema、H2 schema、domain、mapper、DTO、VO SHALL exact-match

#### Scenario: 历史写作会话回填

- GIVEN 历史 WRITING_GUIDANCE session 缺少 intent
- WHEN migration 或兼容读取执行
- THEN intent SHALL 归一为 LISTEN
- AND ACTIVE session 当前 stage SHALL 归一为 WITNESS，stage reask count SHALL 为 0

#### Scenario: 历史消息与轨迹

- GIVEN 历史 message/trace 携带 EMOTION、CONFUSION、CORE_QUESTION 或 EXPECTATION
- WHEN P4.1 migration 执行
- THEN 这些 rows SHALL 保持原值
- AND 后端 SHALL 继续安全读取它们

#### Scenario: Migration 验证边界

- GIVEN 实现者验证真实 MySQL migration
- WHEN Gate 3c 未授权
- THEN 真实 schema/preflight SHALL 保持 SKIPPED
- AND H2 或文件检查 SHALL NOT 被描述为真实 MySQL PASS

### Requirement: Witness Turn Policy And Question Enforcement Must Be Backend Side

conversation intent、turn policy、问题上限与最终 enforcement SHALL 由 backend 决定；frontend SHALL 只呈现选择与结果。

#### Scenario: Frontend 篡改阶段或上限

- GIVEN 客户端提交消息时附带 stage、turn policy 或 question limit
- WHEN 后端处理请求
- THEN 后端 SHALL 忽略或拒绝这些非契约字段
- AND SHALL 只使用持久化 intent 与 backend 配置计算策略

#### Scenario: Provider 返回过多问题

- GIVEN provider 文本超过 backend 计算的问题上限
- WHEN reply pipeline 处理该文本
- THEN SHALL 进入 typed reflection 或最终 fallback
- AND 未经 enforcement 的文本 SHALL NOT 返回 frontend

#### Scenario: Structured evidence

- GIVEN 后端记录 intent switch、turn policy 或 question violation
- WHEN 日志或 trace 被写出
- THEN SHALL 只记录 enum、limit、outcome、version 与计数
- AND SHALL NOT 记录 conversation/diary/model text、prompt、memory fragment 或 secret
