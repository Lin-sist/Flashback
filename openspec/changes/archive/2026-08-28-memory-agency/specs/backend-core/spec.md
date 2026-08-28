# Delta Spec：backend-core（P4.2 Memory Agency）

## ADDED Requirements

### Requirement: Session Memory Authorization Must Be Persisted And Owner Scoped

后端 SHALL 将跨记录记忆授权作为 session 业务状态持久化，缺省与历史值均为 false。

#### Scenario: 新建或迁移 session

- GIVEN 新 session 被创建或历史 session 被迁移
- WHEN authorization 未被用户显式开启
- THEN `cross_record_memory_enabled` SHALL 为 false
- AND session VO SHALL 返回 false

#### Scenario: 修改授权

- GIVEN owner 拥有 ACTIVE session
- WHEN 调用 `PUT /api/agent/sessions/{sessionId}/memory-authorization`
- THEN 后端 SHALL 幂等保存 boolean 并返回真实状态
- AND SHALL NOT 调用 provider、推进 turn/stage、改变 intent、执行工具或生成素材

#### Scenario: 非法修改

- GIVEN session 已 END、属于其他用户或请求值缺失/非法
- WHEN 请求修改授权
- THEN 后端 SHALL fail-closed
- AND SHALL NOT 泄露 session、record、message 或 authorization 状态

#### Scenario: Pending retry

- GIVEN session 最后一条用户消息等待 retry
- WHEN 用户修改 authorization
- THEN 后端 SHALL 允许关闭以收窄权限、拒绝开启以避免隐式扩大 pending turn 上下文
- AND retry SHALL 使用执行时持久化的当前授权

### Requirement: Record Memory Policy Must Be User Controlled Metadata

每条记录 SHALL 支持 owner 控制的排除标记与可选时间语境说明；AI SHALL 无写权限。

#### Scenario: 保存 policy

- GIVEN owner 调用 `PUT /api/records/{recordId}/agent-memory-policy`
- WHEN body 包含 excluded boolean 与合法 contextNote/null
- THEN 后端 SHALL 全量替换 policy 并返回 record detail
- AND blank note SHALL 归一为 null，非空 note SHALL 不超过 255 字

#### Scenario: 封存状态

- GIVEN record 为 DRAFT、SAVED、SEALED 或 UNLOCKED
- WHEN owner 只修改 Agent memory policy
- THEN 后端 MAY 更新该同意/解释元数据
- AND SHALL NOT 修改或披露 SEALED content、location、attachments、cover 或其他封存字段

#### Scenario: AI 无权写入

- GIVEN provider reply、material 或 tool proposal 包含对用户的推测
- WHEN 后端处理这些结果
- THEN SHALL NOT 自动修改 excluded/contextNote
- AND 既有工具白名单 SHALL NOT 新增 policy 写工具

### Requirement: Actual Memory Sources Must Be Durable Without Copying Content

后端 SHALL 按 assistant message 持久化实际来源关系，并保持 message/source 一致性。

#### Scenario: Schema

- GIVEN P4.2 migration 执行
- WHEN 建立 source persistence
- THEN SHALL 存储 user/session/assistant-message/source-record/source-kind/created-at 等结构化关系
- AND SHALL NOT 存储 fragment、content、summary、note 快照、keywords、score、hit reason、prompt 或 reply

#### Scenario: 同一事务

- GIVEN assistant message 与 source rows 构成同一轮成功结果
- WHEN 任一持久化步骤失败
- THEN 主业务事务 SHALL 整体回滚并允许既有 retry 语义处理
- AND source 写入 SHALL NOT 使用会与外层父行锁竞争的独立事务

#### Scenario: Message source response

- GIVEN owner 读取 session messages
- WHEN assistant message 有来源关联
- THEN message VO SHALL 返回 owner 当前仍可见的 source metadata
- AND user message 或无来源 assistant message SHALL 返回空数组

#### Scenario: 旧 message

- GIVEN message 创建于 P4.2 之前
- WHEN migration 或会话恢复
- THEN 后端 SHALL NOT 根据 trace count 或重新检索回填具体来源
- AND memorySources SHALL 为空

### Requirement: Source Resolution Must Respect Owner Status And Deletion

来源解析和检索 SHALL 在每次读取时执行 owner/status/排除/删除边界。

#### Scenario: 跨用户来源

- GIVEN source relation、session 或 record 不属于当前用户
- WHEN 用户读取来源
- THEN 后端 SHALL 按未找到处理且不返回 metadata

#### Scenario: 被封存记录

- GIVEN record 为 SEALED
- WHEN MemoryPort 选候选
- THEN record SHALL 被排除
- AND policy endpoint SHALL NOT 成为读取内容的旁路

#### Scenario: 删除后的历史来源

- GIVEN source record 已删除且外键关系被置空
- WHEN owner 恢复旧 assistant message
- THEN source SHALL 返回 available=false 与空 record metadata
- AND 后端 SHALL NOT 从快照或日志恢复标题/正文/摘要

### Requirement: Memory Migration Must Be Idempotent And Verifiable On Real MySQL

P4.2 schema 变更 SHALL 提供幂等增量脚本并同步 H2 测试 schema；外键/事务结论必须经真实 MySQL 验证。

#### Scenario: 重复迁移

- GIVEN migration 已执行一次
- WHEN 同一脚本再次执行
- THEN schema 与默认值 SHALL 保持正确且不破坏现有数据

#### Scenario: Real MySQL evidence

- GIVEN 用户已单独授权 Gate 3a
- WHEN 验证 migration、rollback、delete SET NULL 与 owner/status
- THEN SHALL 只使用合成数据并最终清理
- AND H2 PASS SHALL NOT 被描述为真实 MySQL PASS
