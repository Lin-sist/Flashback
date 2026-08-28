# Proposal：Memory Agency（P4.2）

## 1. Summary

P4.2 把现有“配置开启且有 cue 就自动参考过去”的跨记录记忆，改成用户可见、按会话授权、可追溯并可撤销的能力：默认只使用当前记录与当前会话；用户主动开启“本次可参考过去记录”后，才允许检索符合 owner/status/记录排除边界的其他记录。

本 change 规划以下能力：

- 跨记录检索与注入默认严格为零；
- WRITING_GUIDANCE 与 REVIEW_CHAT 都可按当前 session 单独授权，回看目标 UNLOCKED 记录仍是默认上下文；
- 用户能看到本轮实际进入 prompt 的来源记录，并回到对应记录；
- 用户能关闭会话授权、把单条记录标为“不再供 Agent 参考”，或补充“只代表当时，不代表现在”的说明；
- 删除、排除和撤销只影响未来轮次，不虚假承诺抹除已发生的 provider 调用；
- 不把 AI 摘要、推测或画像自动写成用户事实。

本轮只建立 Gate 1 规划工件。规划批准不自动授权业务实现、真实 provider、真实 MySQL、微信开发者工具/真机、delta acceptance、归档、commit、push、部署或发布。

## 2. Why Now

- P4.1 `witness-agent-alignment` 已归档，`.ai/ACTIVE_TASK.md` 已回到 `IDLE`；冻结蓝图下一项为 P4.2。
- P4.2 的硬依赖 P4.1、C3 记忆底座与 C9 时间边界均已进入 accepted specs。
- 当前 `application.yml` 的 `AGENT_MEMORY_ENABLED` 默认值为 true；只要 Runtime 提取到关键词或标签 cue，就会直接调用 `MemoryPort` 检索并注入其他记录。
- 当前 session/record 没有用户授权、单条排除或用户解释字段；trace 只有检索/注入数量和长度，用户无法知道本轮实际用了哪条记录。
- 微信登录报错已定位并恢复：本机 backend 未运行导致 `/api/auth/wechat-login` 失败；启动 backend 后真实登录与首页数据链路通过。该环境故障不是 P4.2 业务缺陷，不应混入本 change 实现。

## 3. User Story

> 改前，Agent 可能因为配置和一段相似表达自动提到过去，用户不知道它为什么知道，也不能只撤掉这次授权或排除某条记录。
>
> 改后，默认只看这一刻。用户主动开启“本次可参考过去记录”后，Agent 才能在当前会话参考符合边界的其他记录；如果确实使用了过去，用户能看到来源、回到原记录，并可关闭授权、排除、解释或删除。

## 4. Goals

1. 将“用户可见的 session 授权”置于运维配置开关之前：任一为关闭都不得跨记录检索。
2. 新建和历史 session 均默认 `crossRecordMemoryEnabled=false`，不以旧配置或曾经命中过记忆为由继承授权。
3. REVIEW_CHAT 默认只读取目标 UNLOCKED 记录；其他历史与 WRITING_GUIDANCE 一样需要 session 授权。
4. 只展示本轮实际注入 prompt 的来源记录，不展示候选、分数、命中数、关键词或片段文本。
5. 允许 owner 在记录级排除未来 Agent 参考，并保存一段仅由用户提交的时间语境说明。
6. 撤销 session 授权、排除记录或删除记录后，下一轮立即不再检索/注入对应历史。
7. 保留 C3 的可替换 `MemoryPort`、LIKE 检索字段/权重/索引边界和 C9 时间归属，不借机升级向量检索。
8. 保留 P4.1 witness role、LISTEN/UNTANGLE、工具/素材确认、用户原文忠实度、韧性、Preview fail-closed 与 owner scope。
9. 以真实 MySQL 验证授权、来源、撤销、删除与 owner/status 隔离；微信侧验证授权和来源交互，但不要求真实 provider 才能证明数据库/编排不变量。

## 5. Non-goals

- 向量数据库、全文索引、分词器、知识图谱或检索相关性重写；
- 用户画像、长期人格、自动阶段总结、情绪趋势、模式诊断、评分或成长报告；
- 全局永久授权、默认勾选、跨 session 继承或后台隐式授权；
- LLM-as-Judge、额外 provider 自检、为本 change 校准 C3/C9 阈值；
- 把 AI 摘要/推测自动升级成用户确认事实，或把过去记录搬进当前正文；
- 新 Agent 工具、工具白名单扩张、主动召回、关系养成或建议型产品；
- 独立 settings 页面、一级 Tab、管理后台、deployment/monitoring、package/lockfile；
- 修复 dev profile MyBatis DEBUG 可能输出 SQL 参数的问题；该项是独立 Type B 隐私风险。

## 6. Current Capability Classification

### confirmed

- C3 已提供 owner/status 过滤、可替换 `MemoryPort`、有 cue 才查询、受限片段数/长度和不读取正文的 MySQL LIKE 检索。
- C3/C9 已提供分层来源、时间归属、不得提前拆封、不得将过去写入当前正文与确定性后置护栏。
- REVIEW_CHAT 只允许目标 UNLOCKED 记录，目标记录自身进入 memory layer；写作与回看均复用同一 Runtime。
- P4.1 已提供 witness role、LISTEN/UNTANGLE、结束/恢复/失败边界；Preview 不访问真实 Agent。
- C5 trace 不记录记忆内容，只记录结构化检索/注入指标。

### partial

- backend-side 配置可以全局关闭记忆，但它是运维 kill switch，不是用户可见授权。
- `MemoryFragment` 已携带 `recordId`，足以建立实际来源关联，但当前只在内存中使用，没有用户可查询的来源证据。
- 删除记录已通过 owner-scoped durable delete 清理关联业务数据，但尚未定义“历史来源证据在源记录删除后如何显示”。

### planned

- session 级 `crossRecordMemoryEnabled` 的 API、持久化、恢复、撤销和 frontend exact-match；
- record 级 `agentMemoryExcluded` 与 `agentMemoryContextNote` 用户控制元数据；
- 每轮实际注入来源的结构化持久化与 per-message 来源 VO；
- source chip、跳转、授权开关、排除与用户说明 UI；
- C6 离线不变量、真实 MySQL probe、微信 Standard/Preview 矩阵。

### unknown

- 真实 MySQL 当前历史 session/record 是否已有可复用字段；只允许在实现授权后的 Gate 3 以 schema/聚合方式核对，不读取正文或消息。
- 微信物理真机上 source chip、授权开关与 textarea/keyboard 的触感；开发者工具不能替代物理真机。
- 当前 provider 在开启记忆后是否自然表达来源与时间距离；本 change 的核心验收不依赖语言质量，若未来要做人评须另申请真实 provider 预算。

### out_of_scope

- R1 安全响应、E1/P5 时间篇章、向量检索、画像/分析、dev 日志收口、生产 SLA 与发布。

## 7. Proposed Scope

### 7.1 Session authorization

- `agent_session.cross_record_memory_enabled BOOLEAN NOT NULL DEFAULT FALSE`；历史行回填 false；
- `PUT /api/agent/sessions/{sessionId}/memory-authorization` 只允许 owner 修改 ACTIVE session，body 为 `crossRecordMemoryEnabled`；
- 切换幂等、纯数据库操作，不调 provider、不推进 turn、不改 intent/stage/tool/material；
- start/resume/get/turn VO 返回真实授权状态；客户端不得覆盖 backend authority；
- `AGENT_MEMORY_ENABLED` 保留为 backend kill switch，但 config=true 且 session=false 时检索调用数必须为零。

### 7.2 Record policy and user explanation

- `record.agent_memory_excluded BOOLEAN NOT NULL DEFAULT FALSE`；
- `record.agent_memory_context_note VARCHAR(255) NULL`，只接受 owner 显式提交的文本；AI 不得生成或自动更新；
- `PUT /api/records/{id}/agent-memory-policy` 幂等更新两项用户控制元数据；任何记录状态都允许 owner 改同意元数据，但不得因此读取 SEALED 内容或改变封存字段；
- 记录详情返回 policy；列表/时间轴不新增分析性标识；
- 被排除记录不得进入候选集，删除后自然从未来检索消失。

### 7.3 Actual source evidence

- 新增 `agent_memory_source`，按 `session_id + assistant_message_id + source_record_id` 记录实际进入本轮 prompt 的来源关联；
- 只持久化 ID、owner、时间等结构化元数据，不复制 fragment、摘要、正文、关键词、分数、命中原因或 provider prompt；
- 来源只绑定成功持久化的 assistant message；失败/未调用/未注入不制造来源；
- `AgentMessageVO.memorySources` 返回该 assistant message 实际来源；源记录存在且仍归 owner 时可跳转，删除后返回不可用占位且不得泄露旧标题/内容；
- REVIEW_CHAT 目标记录自身也作为实际来源展示；“跨记录授权”只控制其他记录。

### 7.4 Runtime order

1. 校验 owner、session status/purpose 与 provider 可用性；
2. 总是组装当前会话；REVIEW_CHAT 只在目标记录仍为 owner 的 UNLOCKED 时组装目标记录；
3. 仅当 backend config 与 session authorization 同时为 true 时提取 cue 并调用 `MemoryPort`；
4. `MemoryPort` SQL 无条件排除 `agent_memory_excluded=true`、SEALED、删除中/删除后、非 owner、当前记录；
5. temporal policy 决定最终 injected list；来源证据必须从这一份最终 injected list 派生；
6. prompt 成功并持久化 assistant message 后，在同一业务结果中持久化来源关联；
7. 撤销/排除/删除只影响后续 turn，不宣称撤回已发送给 provider 的内容。

### 7.5 Mini Program

- Agent sheet 内提供非默认勾选的“本次可参考过去记录”；关闭状态文案明确“默认只看这次对话/当前记录”；
- 切换失败保持旧状态并明确提示；请求中禁用重复操作；
- assistant message 下只在有实际来源时显示“参考了过去的记录”及轻量 source chip；点击回到自己可见的记录；
- 记录详情提供“不再供 Agent 参考”和“只代表当时，不代表现在”的用户说明入口；
- Preview 不显示假来源，不产生真实授权/策略请求；不新增一级 Tab、全局设置或分析页面。

## 8. Spec Delta Map

- `agent-runtime`：授权前置、最终注入列表与来源同源、撤销/排除/删除即时性、无 AI 事实持久化；
- `backend-core`：session/record/schema/API、source evidence、owner/status/transaction contract；
- `miniapp-core`：会话授权、实际来源、记录排除/说明、失败与 Preview 交互；
- `v2-product-scope`：默认此刻优先、过去参与的许可、可见和可撤销边界；
- `agent-collaboration`：C6、真实 MySQL、微信、隐私/证据与外调诚实边界。

## 9. Evidence Plan

- 文件级：artifacts、delta operations、Requirement/Scenario、链接、ACTIVE_TASK、allowlist、隐私/凭证扫描；
- 离线：授权关闭时 MemoryPort 调用 0；开启/关闭、legacy false、排除、source exact-match、删除、owner/status、transaction failure、Preview；
- C6：新增 authorization-off/enable/revoke/exclude/delete/review-target/source fixtures；任何合法 baseline 变化写 `baselineNote=P4.2 memory-agency: <reason>`；
- 真实 MySQL：migration 连续两次、schema exact-match、合成 owner/session/record/source、撤销/排除/删除与 cleanup；
- 微信：Standard 验授权、来源跳转、排除/说明、错误恢复；Preview 验真实请求数 0；开发者工具与物理真机分开报告；
- frontend/backend build：focused/full Maven，type-check，standard/Preview mp-weixin build；
- 真实 provider：规划与核心 Gate 3 预算均为 0；不把 scripted response 或 prompt interception 写成真实语言质量。

## 10. Responsibility And Gate State

- 开工锚点：`42548ce`。
- 当前提交责任：用户手动提交；P4.1 的 Agent commit 授权不继承到 P4.2。
- 规划期外部调用预算：0；未连接真实 provider、MySQL、微信或其他外部服务。
- OpenSpec CLI：当前 shell 未安装；采用仓库既有文件级 scaffold，CLI validation 记为 `SKIPPED`。
- Gate 1：等待用户批准 proposal、design、tasks、五份 delta 与设计决策 1–12。
- Gate 2：未授权；Gate 1 批准后仍须用户明确允许实现。
- Gate 3：未授权；真实 MySQL、微信和任何外部副作用须分别授权。
- commit/push/PR/deploy/release：均未授权。
