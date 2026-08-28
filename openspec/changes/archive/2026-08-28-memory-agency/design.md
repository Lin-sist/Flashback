# Design：Memory Agency（P4.2）

## 1. Context And Constraints

当前调用链是 `AgentChatServiceImpl -> MemoryPort -> MySqlMemoryPort`。Runtime 在 config enabled 时先从会话提取关键词/标签，再检索其他记录；`TemporalPolicyResult.injectedFragments()` 是真正进入 prompt 和 guardrail memory layer 的列表。`MemoryFragment` 已包含 source `recordId`，但来源只存在于内存中。

约束：

- 用户可见授权必须比配置开关更严格；
- 目标 REVIEW_CHAT 记录与“其他历史”必须分层，不能因默认关闭跨记录授权而让回看失效；
- 不能复制高敏 fragment/摘要/正文到新表、日志或 trace；
- source evidence 必须代表实际注入，而不是候选或检索命中；
- 删除/撤销对未来立即生效，但不能虚构对既往 provider 调用的技术撤回；
- H2 不能证明 MySQL 外键、锁和事务时序，真实 MySQL 是必须证据；
- 不改 C3 的 LIKE 检索策略、字段权重、时间窗与 C9 阈值。

## 2. Domain Model

### 2.1 Session authorization

`AgentSession.crossRecordMemoryEnabled` 是当前 session 的用户授权事实，默认 false。它既不是模型偏好，也不是全局账户设置；ENDED session 只读保留历史状态，不能再切换。

有效门控：

```text
crossRecordRetrievalAllowed
  = backendConfig.memory.enabled
  && session.status == ACTIVE
  && session.crossRecordMemoryEnabled
```

任一为 false 时，cue extraction 和 `MemoryPort.retrieve` 都不执行，以便“授权关闭=0 检索”可被直接验证。

### 2.2 Record policy

- `agentMemoryExcluded`：用户撤回该记录未来作为跨记录来源的许可；默认 false。
- `agentMemoryContextNote`：用户自己的时间语境说明，最多 255 字；null/blank 归一为 null。

这两项是同意/解释元数据，不修改记录正文，也不属于封存后禁止修改的 location/attachments/cover。owner 可以在 DRAFT/SAVED/SEALED/UNLOCKED 设置；对 SEALED 的操作只更新元数据，不返回或解封内容。

### 2.3 Actual source evidence

建议新表：

```text
agent_memory_source
- id BIGINT PK
- user_id BIGINT NOT NULL
- session_id BIGINT NOT NULL
- assistant_message_id BIGINT NOT NULL
- source_record_id BIGINT NULL
- source_kind VARCHAR(24) NOT NULL  // REVIEW_TARGET | CROSS_RECORD
- created_at DATETIME NOT NULL
```

约束/索引：

- unique `(assistant_message_id, source_record_id, source_kind)`；
- user/session/message 外键 cascade；source record 外键 `ON DELETE SET NULL`；
- owner scope 查询同时校验 `user_id`、session owner 与当前 record owner；
- 不存 source title/content/fragment/summary/note 快照，不存 keywords/score/hit reason。

源记录删除后，历史 assistant message 只显示“来源记录已删除或不可用”，不能重建旧链接或泄露旧信息。该表保留“本轮曾使用一个来源”的诚实历史，但不保留内容副本。

## 3. API Contract

### 3.1 Session authorization

```http
PUT /api/agent/sessions/{sessionId}/memory-authorization
{ "crossRecordMemoryEnabled": true|false }
```

返回 `AgentSessionVO`。仅 owner + ACTIVE；未知/缺失字段 fail-closed。切换不调 provider、不产生 message/turn/source、不改变 intent/stage/tool/material。若上一轮用户消息等待 retry，允许关闭但不允许开启：关闭能收窄授权，开启会改变 pending retry 的上下文边界；retry 固定使用执行时的当前授权。

`AgentSessionVO` 新增非空 boolean。旧客户端忽略字段；旧数据库行 migration 后为 false。

### 3.2 Record memory policy

```http
PUT /api/records/{recordId}/agent-memory-policy
{
  "excluded": true|false,
  "contextNote": "这只代表当时，不代表现在" | null
}
```

返回 `RecordDetailVO`，字段名为 `agentMemoryExcluded` / `agentMemoryContextNote`。请求是全量 policy 替换而不是 PATCH，避免省略字段含义不明。最大 255 字，拒绝控制字符；只存用户提交文本。

### 3.3 Message sources

`AgentMessageVO` 新增 `memorySources: []`，每项：

```text
recordId: number | null
sourceKind: REVIEW_TARGET | CROSS_RECORD
displayTitle: string | null
occurredAt: datetime | null
contextNote: string | null
available: boolean
```

只有 assistant message 可返回 sources。`displayTitle/occurredAt/contextNote` 在读响应时从当前 owner-scoped record 解析，不写快照。删除/转为不可用时只返回 null + `available=false`。不返回 fragment、摘要、分数、关键词、命中理由或候选数量。

## 4. Runtime And Transaction Flow

```text
current session/history
        |
        +--> REVIEW_CHAT target (default, UNLOCKED only)
        |
session authorization + backend kill switch
        |
        +--> cue extraction --> MemoryPort --> eligible cross-record fragments
                                      |
                           temporal policy final injected list
                                      |
                          prompt / layered corpus / guardrails
                                      |
                        assistant message persistence
                                      |
                  source rows from the same final injected list
```

不变量：

1. 授权 false 时 `MemoryPort` 调用次数为 0；
2. REVIEW_TARGET 不受 cross-record 开关影响，但必须仍为 owner 的 UNLOCKED；
3. source rows 与最终 injected list exact-match；检索候选和被 temporal policy 丢弃的片段不落 source；
4. provider/guardrail/message persistence 失败时不产生成功 source；
5. assistant message 成功但 source persistence 失败不得对用户谎称“未使用”。实现需让 message + source 处于同一主事务，失败时本轮整体返回 FAILED/可重试；禁止 source 辅助事务与外层互锁；
6. 重试同一 turn/attempt 不得重复 source rows；
7. trace 可增加 `authorizationEnabled/sourceCount/revokedAtExecution` 等结构化字段，但不记录 source record ID 列表或内容。

## 5. Retrieval Eligibility

`selectMemoryCandidates` 保持现有 owner predicate、SEALED 排除、删除中排除、current record 排除、time window、tag/keyword cue 与 LIKE 字段；仅追加无条件 `agent_memory_excluded = false`。

上下文说明不会进入检索关键词或相关性排序。若记录被选作来源，note 可与时间标签一并进入 prompt，明确它是用户后来补充的说明；note 不取代原 fragment，也不得被表述为模型结论。

## 6. Revocation And Deletion Semantics

- 关闭 session 授权：提交成功后的下一次 turn 不提 cue、不检索、不注入其他记录；过去 message 的 source evidence 保留。
- 排除记录：policy commit 后任何新查询均排除；已经打开但未开始事务的 turn 读取最新值；不承诺撤回已经发出的 provider 请求。
- 删除记录：durable delete 的 mutation freeze 继续适用；进入删除操作后即从候选排除；完成后 source FK 置 null，历史来源不可跳转。
- 恢复授权/取消排除：只允许未来轮次重新参与，不回算旧回复。

前端文案必须说“之后不再参考”，不得说“已经从模型记忆中彻底删除”或“撤回了过去发送的数据”。

## 7. Privacy And Security

- 新表只存关系元数据，禁止复制用户日记、AI 摘要、fragment、prompt、回复、关键词或 token；
- source API 必须 owner-scoped，跨用户按未找到处理；
- SEALED 记录不能作为 source，也不能因 policy API 返回内容；
- record note 是用户数据，不写普通日志、trace、baseline 或测试 fixture；测试只用合成短句；
- dev profile MyBatis DEBUG 参数日志风险不在本刀实现，但 Gate 3 启动前必须确保测试使用合成数据并停止临时 backend 后报告风险。

## 8. Frontend Design

- 授权控件放在现有 Agent sheet 内，紧邻 privacy note/intent switch；默认 off，不使用预选、红点或催促文案；
- 开启说明：“只在这次对话里，参考你可见且未排除的过去记录”；关闭说明：“默认只看当前记录和这次对话”；
- sources 挂在对应 assistant bubble 下，只有非空才展示；chip 标题优先当前 record title，空标题用月份，不展示内容片段；
- 点击 available source 使用既有 record detail 路由；unavailable 不可点击；
- 记录详情中的 policy 是轻量可撤销控件，不放入全局 settings，也不做“记忆管理中心”；
- context note 使用用户可编辑 textarea，保存前后都不触发 provider；
- Preview 不创建真实 session，不调用授权/policy/source endpoint，不伪造“参考了过去”。

## 9. Compatibility And Migration

- 新 session 列 NOT NULL DEFAULT FALSE，先加列/回填/校验；重复 migration 幂等；
- 新 record 字段默认 false/null，不重写任何现有内容或 AI 字段；
- 新 source table 对既有 message 不回填：历史上无法证明“实际注入了哪些”，不得根据 trace count 或重新检索伪造；
- frontend 对旧 backend 缺字段按 false/[] fail-closed；backend 对旧 frontend 不传授权请求时保持 false；
- application config 默认值可保持 true，语义降为“允许该能力运行的 kill switch”，绝不能替代 session consent。

## 10. Verification Strategy

### Gate 2 offline

- TDD 覆盖 auth=false 时 cue extractor/MemoryPort/provider context source 为 0；
- auth=true 时 final injected list 与 source rows exact-match；
- revoke/exclude/delete/retry/transaction/owner/status/review target；
- C6 固定合成不变量与 baselineNote；
- backend focused/full、frontend type-check、standard/Preview build；
- diff/path/package/privacy/credential checks。

### Gate 3a real MySQL

- schema preflight；migration 连续两次；
- 只建合成 users/records/sessions/messages/sources；
- 验 false=0、true 命中、撤销、排除、删除 FK SET NULL、owner/status、rollback、cleanup；
- 不读取/打印真实正文、消息、note、record IDs 或用户标识。

### Gate 3b WeChat

- Standard：默认 off、开启/关闭、失败保留、source chip/跳转、record exclude/note、删除后 unavailable；
- Preview：真实 Agent/授权/policy/source 请求数均为 0；
- scripted Agent response 只证明 UI 编排，不冒充 provider 自然度；物理真机未做则记 SKIPPED。

### Provider budget

P4.2 的核心 claim 是授权与数据流不变量，可通过离线 spy + 真实 MySQL + 微信交互证明。默认真实 provider 预算 0；若实现后另有语言质量疑问，须新申请小样本预算与人评目标。

## 11. Risks And Mitigations

- **source 与 message 不一致**：从同一 final injected list 派生，并在同一事务落库。
- **删除外键互锁**：禁止 source 使用 `REQUIRES_NEW`；真实 MySQL 专测并发/回滚时序。
- **旧历史来源无法重建**：明确不回填，避免把推断写成事实。
- **用户误以为全局删除模型记忆**：文案限定“之后/未来轮次”，说明既往调用不可撤回。
- **note 被模型当作永久人格事实**：标记为用户后来说明，只随该 source 使用，不参与排序/画像。
- **配置开关被误当授权**：测试同时覆盖 config/session 四象限，UI 只读 session state。
- **LIKE 相关性弱**：保持 R9，P4.2 不升级检索。

## 12. Decision Records

### D1｜授权粒度：session，不做全局永久授权

- 决策：每个 session 独立 boolean，默认 false。
- 原因：符合“本次可参考”，最小化授权范围；避免用户忘记永久开关。
- 代价：新 session 需重新开启。

### D2｜配置开关与用户授权采用 AND

- 决策：config 是 kill switch，session 是 consent；两者同时 true 才检索。
- 原因：运维开关不能替代用户可见授权。

### D3｜回看目标与其他历史分层

- 决策：REVIEW_TARGET 默认可用，CROSS_RECORD 受 session 授权。
- 原因：用户主动打开某条 UNLOCKED 记录就是对目标记录的明确上下文选择，不等于授权所有历史。

### D4｜记录 policy 两字段合并为一个全量 PUT

- 决策：`excluded + contextNote` 由同一 owner-scoped endpoint 全量替换。
- 原因：同属“这条记录未来如何被 Agent 理解”的用户控制，避免 PATCH 省略语义。

### D5｜用户说明保存自由文本，不由 AI 生成

- 决策：nullable 255 字用户文本；不自动总结、不推断。
- 原因：满足“只代表当时”的纠正权，同时避免创建画像/事实抽取系统。

### D6｜来源按 assistant message 持久化

- 决策：新结构化关联表，不只依赖 trace 或临时 response。
- 原因：恢复会话后仍能诚实展示每轮实际来源；trace 的职责是观测，不是用户产品数据。

### D7｜来源不复制内容快照

- 决策：只存 ID/类型/关系；展示时 owner-scoped 读取当前 metadata。
- 原因：降低高敏数据复制面，让删除真实生效。

### D8｜源记录删除使用 SET NULL + unavailable

- 决策：保留“曾有来源”的历史事实，但清除链接和内容元数据。
- 原因：既不伪造从未使用，也不让删除后的内容可恢复。

### D9｜授权 false 时连 cue extraction 都不执行

- 决策：在 cue extractor 与 MemoryPort 之前短路。
- 原因：将“0 检索”做成可测硬不变量，并减少当前表达的派生处理。

### D10｜message 与 source 同主事务

- 决策：不使用独立辅助事务；任一失败整体可重试。
- 原因：用户来源可见性属于核心诚实契约，不能 fail-open 丢证据；同时规避已知 MySQL 父行锁自锁风险。

### D11｜不回填旧 message 来源

- 决策：P4.2 前的消息 `memorySources=[]`。
- 原因：历史只有数量，无法证明具体实际来源；重新检索会伪造历史。

### D12｜P4.2 默认不做真实 provider Gate

- 决策：核心 Gate 使用离线数据流断言、真实 MySQL 和微信 UI；provider 预算 0。
- 原因：本刀验的是 consent/source/revocation，不声称优化语言质量；避免无关外调与高敏数据面扩大。
