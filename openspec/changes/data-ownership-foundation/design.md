# Design：Data Ownership Foundation（P3.2）

## 1. Design Intent

数据所有权能力的核心不是“多一个危险按钮”，而是让用户能够拿回、删除并验证自己的数据，同时让远端对象、数据库关联数据和 Agent 派生数据没有静默遗留。

本设计以三个原则收口：

1. **用户明确发起**：Agent、定时任务或默认设置都不能替用户决定导出或删除。
2. **失败可见、操作可恢复**：删除和导出不返回 mock success；中断后保留 owner-scoped 重试锚点。
3. **最小暴露**：导出包属于用户，日志和执行证据不属于用户内容的第二份副本。

## 2. Current Facts

- record 当前状态为 DRAFT / SAVED / SEALED / UNLOCKED；现有 DELETE 只允许 DRAFT。
- record、location、attachment、tag relation、reply、reminder、unlock notice、Agent session/message/tool/trace 已有 owner 或 record/session 外键基础。
- 数据库 cascade 不能删除 Qiniu/S3-compatible 远端对象。
- P3.1 `DraftCleanupWorker` 已证明 provider delete → not-found 幂等 → DB delete 的窄路径可行，但它只覆盖过期 DRAFT。
- H0 已从真实用户中心移除假“数据备份”入口；未注册页面文件仍含固定日期、固定数量、iCloud、PDF/纯文本等演示文案。
- Preview service mutation 已统一 fail-closed。

## 3. Recommended Contract For Gate 1

以下 exact 名称是本轮推荐契约，只有用户批准 Gate 1 后才成为实现依据。

### 3.1 Operation types

- `EXPORT`
- `DELETE_RECORD`
- `CLEAR_ALL_RECORDS`

### 3.2 Operation statuses

- `PREPARED`：删除 intent 已创建，尚未确认；
- `PENDING`：已确认、等待 worker；
- `RUNNING`：正在构建导出或逐记录删除；
- `RETRY_REQUIRED`：存在可恢复失败，未声称完成；
- `SUCCEEDED`：导出包可下载，或目标记录已全部删除；
- `FAILED`：不可恢复的契约/数据一致性错误，需要人工审查；
- `EXPIRED`：未确认 intent 或导出临时包已过期。

状态只能按预期状态更新。客户端重试同一 intent 或 operation 不创建第二次不可逆操作。

### 3.3 Sealed export policy

- `RESPECT_SEAL`：默认。对尚未解锁的 SEALED 记录只导出状态、创建/封存/预计解锁时间与说明占位，不导出正文、位置、媒体、封面、AI 字段或回信。
- `FULL_CONTENT`：用户在导出前明确选择后，导出其全部 owned 数据；这只是取回副本，不修改 record 状态、不提前解锁产品内页面。

### 3.4 API surface

- `GET /api/data-ownership/summary`
  - 返回 owner 的状态计数、媒体字节估算、是否存在运行中操作；不返回他人数据。
- `POST /api/data-ownership/export-operations`
  - body：`sealedContentPolicy`；返回 operation。
- `GET /api/data-ownership/operations/{operationId}`
  - 返回 owner-scoped 状态、计数、类型化失败原因、是否可重试/下载。
- `GET /api/data-ownership/export-operations/{operationId}/download`
  - 仅 `SUCCEEDED` 且未过期时返回 ZIP；每次下载再次校验 owner。
- `POST /api/data-ownership/deletion-intents`
  - body：`scope=RECORD|ALL_RECORDS` 与可选 `recordId`；返回随机 intent、快照数量、过期时间和所需确认短语。
- `POST /api/data-ownership/deletion-operations`
  - body：`intentId`、`confirmationText`；确认成功后返回 operation。
- `POST /api/data-ownership/operations/{operationId}/retry`
  - 仅 owner 且状态为 `RETRY_REQUIRED` 时有效。

当前 `DELETE /api/records/{id}` 不再允许直接删除数据库聚合。实现时将 frontend 的“放弃草稿”迁移到 data ownership 删除流程；旧接口在兼容期返回明确的迁移错误，不再执行不清理远端对象的删除。

## 4. Persistence Model

### 4.1 `data_operation`

建议字段：

- `id`、`user_id`、`operation_type`、`status`；
- `sealed_content_policy`（仅 EXPORT）；
- `total_items`、`processed_items`、`failed_items`；
- `confirmation_nonce_hash`、`confirmation_expires_at`（仅删除 intent）；
- `artifact_token`、`artifact_expires_at`（仅成功导出；随机、不含文件系统绝对路径）；
- `failure_code`（封闭枚举，不含用户内容或 storage key）；
- `created_at`、`confirmed_at`、`started_at`、`completed_at`、`updated_at`。

约束：

- owner + active destructive operation 唯一；
- owner + active export operation 唯一；
- confirmation 明文不持久化，只保存随机 nonce 的 hash；
- operation row 不保存日记原文、位置、媒体 key、signed URL 或下载 token 明文。

### 4.2 `data_operation_record`

建议字段：

- `operation_id`、`user_id`、`record_id`；
- `item_status=PENDING|RUNNING|RETRY_REQUIRED|SUCCEEDED|FAILED`；
- `attempt_count`、`next_attempt_at`、`failure_code`、`updated_at`。

用途：

- 删除确认时按 owner snapshot 固定目标集合；
- 为每条记录提供中断恢复、重试和进度；
- `record_id` 删除后允许置空或保留不可反查的完成锚点，不能因 cascade 抹掉操作进度；
- 普通 record 查询与 mutation 必须排除/拒绝已进入已确认删除 operation 的记录。

### 4.3 No record status expansion

P3.2 不向 `RecordStatus` 增加 `DELETING`。删除属于数据操作状态，不是用户记忆生命周期状态，避免破坏 DRAFT/SAVED/SEALED/UNLOCKED 的既有语义和筛选契约。

## 5. Export Architecture

### 5.1 Flow

1. API 校验 authenticated owner、策略、互斥 operation；创建 `PENDING` EXPORT。
2. worker 建立 owner snapshot，并把 operation 改为 `RUNNING`。
3. 只读加载 record 聚合、位置、附件、标签、回信与 record-linked Agent 数据。
4. 对每个私有媒体通过 provider 读取原始字节；失败时删除 partial artifact，operation 进入 `RETRY_REQUIRED`。
5. 在 backend 私有临时目录构建 ZIP，完成后原子 rename；manifest 校验全部媒体 SHA-256 和字节数。
6. operation 进入 `SUCCEEDED`，生成随机 artifact token，默认 24 小时到期。
7. 下载 endpoint 再次校验 owner 与 expiry；清理任务删除过期 artifact 并把状态改为 `EXPIRED`。

### 5.2 Package layout

```text
flashback-export/
├─ index.html
├─ records/
│  └─ *.md
├─ media/
│  └─ <stable-relative-name>
├─ agent/
│  └─ *.md
├─ manifest.json
└─ README.txt
```

- `index.html` 为单文件、无 CDN、无 fetch、无外部字体/脚本，链接使用包内相对路径。
- `records/*.md` 明确分区：用户原文、用户填写上下文、时间/状态、位置、附件引用、AI 派生字段。
- `agent/*.md` 只放用户可见的 Agent 会话，按角色和时间标注；不得把 assistant 内容写成用户原话。
- tool/trace 等内部技术数据默认只在 `manifest.json` 以种类、计数和删除覆盖范围说明，不输出 prompt、provider response、瞬态工具参数或脱敏策略内部细节。
- `manifest.json` 包含 schema version、生成时间、策略、record/media/agent counts、文件相对路径、字节数与 SHA-256。
- `README.txt` 解释 sealed policy、DRAFT 标记、Agent 分区、未包含的账号级/运维级数据，以及如何离线打开。

### 5.3 Temporary artifact safety

- 默认 TTL：24 小时；配置只能缩短或在单独决策后调整，不能无限保留。
- 临时目录不在仓库、静态资源目录或普通日志目录；文件名由随机 token 派生。
- 构建失败、中断、过期和下载后清理均不得打印文件内容、绝对路径、媒体 key 或 download token。
- 本设计不是生产备份；backend 临时 artifact 不作为唯一长期副本。

## 6. Deletion Architecture

### 6.1 Prepare and confirm

1. 用户选择单条记录或清除全部。
2. backend 创建短期 `PREPARED` intent，并按 owner 返回快照数量；clear-all 使用明确短语“清除全部记录”。
3. frontend 二次展示不可恢复、导出建议和范围；用户输入/确认后提交 intent。
4. backend 原子校验 owner、nonce hash、expiry、snapshot 与活动 operation，然后创建 item 并转 `PENDING`。

单条删除仍需要两步确认，但不要求再次输入账号密码，因为当前同时支持密码账号与微信身份，强行要求密码会让微信用户无解。认证会话 + 短期随机 intent + UI 明确确认构成本阶段的统一确认边界。

### 6.2 Per-record worker

1. item 以 expected state 抢占为 `RUNNING`。
2. 加载 owner-scoped record 与全部 attachment rows，包括已标记 DELETED 但仍可能有远端对象的行。
3. 删除每个 provider object；success 或 not-found 均视为该对象已清理。
4. 任一 provider 暂时失败：保留 record、attachment、operation item，写封闭 failure code，进入 `RETRY_REQUIRED`。
5. 全部远端对象确认不存在后，在数据库事务内重新校验 owner、operation 与 mutation lock，再删除 record。
6. 数据库外键级联清理 location、attachment、record_tag、reply、reminder、unlock notice、record-linked agent session/message/tool/trace。
7. 逐表 count 断言发现关联残留时 operation 不得声称成功。

如果进程在步骤 3 与步骤 5 之间中断，重试会把 provider not-found 视为幂等成功，再完成数据库删除。

### 6.3 Clear all and concurrency

- clear-all 确认时创建该 user 的 record snapshot；
- 当 operation 为 `PENDING`、`RUNNING` 或 `RETRY_REQUIRED` 时，create/update/save/seal/media/location/cover/tag/Agent record mutation 全部 fail-closed，避免出现快照外新数据；`SUCCEEDED` 后解除，`FAILED` 只能在明确告知仍有数据后人工解除；
- 读取页面可以显示操作状态，但普通 list/timeline/detail 不再展示已确认删除的目标记录；
- EXPORT 与 destructive operation 对同一 user 互斥，避免导出过程中记录或媒体消失；
- operation retry 复用原 snapshot，不扩大到确认后新建的数据。

### 6.4 DRAFT discard migration

- 现有编辑器“放弃未完成记录”改用统一删除 intent/operation；
- 不再直接调用当前会丢失远端清理锚点的 DELETE 逻辑；
- 无内容且从未持久化的本地编辑状态仍可本地关闭，不创建 operation。

## 7. Agent And Derived Data Boundary

- Agent registry、tool executor、Prompt 不新增 export/delete/clear-all 工具。
- Agent 可以用自然语言告诉用户去“数据与所有权”页面，但不得声称已经执行。
- record-linked `agent_session` 删除后，message/tool/trace 必须通过外键或显式 owner-scoped cleanup 消失。
- record-linked reminder、unlock notice 与解锁提醒授权随 record 聚合清理；这不等于注销微信身份、撤销账号级平台授权或实现 notification center。
- 若发现 `record_id` 为空或错误但语义上属于目标 record 的历史派生数据，实施期先建立只读一致性审计；不能猜测关联、不能按文本内容匹配、不能把日记原文写入修复日志。
- `FULL_CONTENT` 导出只包含用户可见会话；内部 eval fixtures、prompt、guardrail policy、provider response、secret 和运行日志不属于用户导出包。

## 8. Mini Program Flow

### 8.1 Entry and page

- 个人中心新增“数据与所有权”入口，复用 `pages/user-center/data-backup/index` 路由或在实现前按 `pages.json` 最小迁移；用户可见标题统一为“数据与所有权”。
- 删除固定备份时间、固定数量、iCloud、自动备份、立即备份、从备份恢复、PDF/纯文本等假能力。
- 页面只显示 backend summary 与真实 operation：导出副本、删除单条、清除全部、进行中/失败/重试。

### 8.2 Export UX

- 默认勾选“尊重尚未到达的封存内容”；用户可主动选择“完整取回”。
- 开始后展示真实状态和计数，不用动画时长冒充进度。
- 只有 backend `SUCCEEDED` 才出现“保存导出包”；微信端必须以真实文件保存/打开或转存结果验收。
- 如果当前微信环境不能可靠保存 ZIP，Gate 3c 记 FAIL/阻塞，不能用 H5、桌面下载或截图代替。

### 8.3 Deletion UX

- 任意状态 record detail 提供“删除这条记录”；SEALED/UNLOCKED 的不可编辑不等于不可删除。
- clear-all 展示记录数、媒体估算与“建议先导出”，要求输入明确确认短语。
- operation 失败时展示“仍有数据未清理”与重试，不显示“已删除”。
- clear-all 进行中禁止进入新建/编辑/Agent 写入，并解释原因。

### 8.4 Preview

- Preview 可以展示页面结构和说明，但 summary 使用明确 demo 数据，所有 export/deletion/confirm/retry/download 都 fail-closed。
- Preview 不生成本地假 ZIP、不修改 Preview fixtures、不显示真实成功 toast。

## 9. Failure Taxonomy

建议封闭 failure code：

- `AUTHORIZATION_CHANGED`
- `OPERATION_CONFLICT`
- `SNAPSHOT_CHANGED`
- `REMOTE_OBJECT_DELETE_FAILED`
- `REMOTE_OBJECT_READ_FAILED`
- `DATABASE_DELETE_FAILED`
- `DERIVED_DATA_REMAINS`
- `ARTIFACT_BUILD_FAILED`
- `ARTIFACT_EXPIRED`
- `LOCAL_STORAGE_UNAVAILABLE`
- `INVARIANT_VIOLATION`

用户界面只显示克制、可行动的说明；日志只记录 code、operation type、计数、attempt 与耗时，不记录内容和 key。

## 10. Verification Strategy

### 10.1 Offline / H2

- operation 状态迁移、owner 隔离、intent expiry、幂等 confirm/retry；
- clear-all snapshot、写入冻结、export/delete 互斥；
- ZIP exact tree、无外链 HTML、Markdown 分区、manifest SHA-256；
- sealed 两策略、DRAFT 标记、Agent 内容分区；
- provider success/not-found/failure、进程中断点、DB failure 后重试；
- 关联表残留检查与 Preview mutation 0。

### 10.2 Gate 3a：real MySQL

- 只读 schema/外键/孤儿聚合 preflight；
- migration 幂等；
- 单条与 clear-all 的 record-linked cascade；
- 中断/重启后 operation/item 恢复；
- 不读取或输出日记原文、位置、storage key。

### 10.3 Gate 3b：real private object storage

- 合成文本/图片/WAV 导出并校验字节和 SHA-256；
- success、not-found、provider failure/retry；
- partial ZIP 与合成对象最终清理；
- 真实 Agent provider 调用保持 0。

### 10.4 Gate 3c：WeChat

- 导出策略、生成、保存/打开或转存；
- DRAFT/SAVED/SEALED/UNLOCKED 单条删除；
- clear-all 强确认、写入冻结、失败重试；
- Preview 全只读；
- build/H2/桌面下载不能替代微信端文件交付证据。

## 11. Scope Safety

- 保留三个一级 Tab 与“我的记录、时光轴、时间回看”命名。
- SEALED/UNLOCKED 的内容、位置、附件、封面仍不可修改；删除是独立的数据所有权操作，不是编辑旁路。
- 不把删除或导出交给 Agent。
- 不做账号注销、云备份、恢复、订阅收费、生产发布或新 AI 能力。
- 不改 package/lockfile；优先 JDK 与现有 Uniapp/Spring 能力。

## 12. 决策记录

### 决策 1：导出和删除使用同步接口还是持久化 operation

1. **面临的选择**：单次 HTTP 同步完成；只在前端显示本地进度；后端持久化可恢复 operation。
2. **选了哪个 + 为什么**：推荐持久化 operation。媒体读取/删除可能跨 provider 且耗时，clear-all 还会跨多条记录；只有持久状态才能在进程中断后继续并避免假成功。
3. **放弃的代价**：同步接口容易超时且无法区分“客户端断开”和“服务端完成”；本地进度在重启后丢失，也不能作为删除证据。

### 决策 2：是否给 RecordStatus 增加 DELETING

1. **面临的选择**：增加 `DELETING`；复用 `DRAFT`；用独立 operation/item 表示删除状态。
2. **选了哪个 + 为什么**：推荐独立 operation/item。DRAFT/SAVED/SEALED/UNLOCKED 是记忆生命周期，删除是数据操作，两者职责不同。
3. **放弃的代价**：新增 RecordStatus 会扩散到列表、时光轴、Agent 状态门和历史迁移；复用 DRAFT 会破坏用户完成语义。

### 决策 3：远端对象与数据库记录谁先删

1. **面临的选择**：先 DB cascade；先远端对象、后 DB；先复制 key 到永久删除日志再删 DB。
2. **选了哪个 + 为什么**：推荐先远端对象确认成功/不存在，再删 DB，并保留 operation/item/record 作为重试锚点。
3. **放弃的代价**：先删 DB 会失去 key 和 owner 关联并留下不可追踪对象；永久复制 key 会形成新的敏感数据存储。

### 决策 4：clear-all 期间是否允许继续写记录

1. **面临的选择**：允许并只删确认瞬间 snapshot；不断扩大 snapshot；确认后临时冻结该用户 record mutation。
2. **选了哪个 + 为什么**：推荐临时冻结。它让“清除全部”的结果可解释、可验证，同时不把 operation 变成无限追赶新数据的任务。
3. **放弃的代价**：允许写入会让用户看到“全部清除”后仍有记录；动态扩大目标可能删除确认之后的新内容，越过原授权。

### 决策 5：导出包在哪里生成

1. **面临的选择**：Mini Program 客户端拼包；上传成对象存储永久 artifact；backend 私有临时目录生成短期 ZIP。
2. **选了哪个 + 为什么**：推荐 backend 私有临时 ZIP，默认 24 小时过期。后端已有 owner 和私有媒体读取能力，JDK 可完成 ZIP，无需把所有敏感数据长期复制到对象存储。
3. **放弃的代价**：客户端拼包受内存、文件 API 和后台中断影响；永久 artifact 会制造新的全量敏感副本和生命周期问题。

### 决策 6：封存内容能否在导出中完整取回

1. **面临的选择**：永远隐藏 SEALED 内容；永远完整导出；默认尊重封存并允许用户显式完整取回。
2. **选了哪个 + 为什么**：推荐双策略，默认 `RESPECT_SEAL`。封存是产品内时间承诺，但数据所有权又要求用户能拿回自己的数据；显式选择同时守住两者。
3. **放弃的代价**：永远隐藏会削弱数据主权；永远导出会在用户无意识时绕过封存承诺。

### 决策 7：用户原文与 Agent 内容如何放在导出包

1. **面临的选择**：混在同一正文；只导出用户原文；`records/` 与 `agent/` 物理分区并在 manifest 标明来源。
2. **选了哪个 + 为什么**：推荐物理分区。用户必须能清楚分辨自己写过的内容和模型生成/参与的内容。
3. **放弃的代价**：混排会把 Agent 文本误认成用户原话；完全排除 Agent 又不是完整的数据副本。

### 决策 8：删除确认是否强制再次输入密码

1. **面临的选择**：要求账号密码；只做一个前端弹窗；authenticated session + 短期 backend intent + clear-all 确认短语。
2. **选了哪个 + 为什么**：推荐第三种。当前既有密码账号也有微信身份，密码复核无法覆盖所有用户；backend intent 能抵抗重复提交并锁定范围。
3. **放弃的代价**：强制密码会阻断微信用户；纯前端弹窗无法防止过期、重放或范围变化。

### 决策 9：现有 DRAFT DELETE 是否继续直删数据库

1. **面临的选择**：保持旧逻辑；只在有附件时走新逻辑；所有已持久化记录统一走 ownership operation。
2. **选了哪个 + 为什么**：推荐全部统一。即使当前草稿没有附件，未来并发上传和关联数据也会让双路径产生不同保证。
3. **放弃的代价**：保持旧逻辑会继续存在远端对象遗留窗口；条件分支会产生难以验证的两套删除语义。

### 决策 10：是否在 P3.2 同时做账号注销和备份恢复

1. **面临的选择**：一起实现；只做账号注销；两者都留给独立 Type C。
2. **选了哪个 + 为什么**：推荐都不做。账号生命周期牵涉身份、openid、授权和全部 user-level 数据；恢复会引入冲突合并、版本迁移和新的写入面，均超出 P3.2 的记录所有权基础。
3. **放弃的代价**：一起做会把一刀扩成账户平台与灾备系统，无法以真实证据收口。

### 决策 11：删除/导出日志记录多少上下文

1. **面临的选择**：记录原文/key/路径方便排错；记录 recordId；只记录 operation、计数、类型化 failure code 与耗时。
2. **选了哪个 + 为什么**：推荐第三种。合成探针、owner-scoped 数据断言和 manifest hash 足以验证，不需要制造高敏日志副本。
3. **放弃的代价**：详细日志会泄露日记、位置或对象 key；recordId 与其他上下文结合也会扩大可关联性。

### 决策 12：Agent 是否获得数据所有权工具

1. **面临的选择**：允许 Agent 直接导出/删除；允许提议后确认；完全不进入 tool registry，只引导用户到真实页面。
2. **选了哪个 + 为什么**：推荐完全不进入 tool registry。导出和删除是高敏、不可逆或大范围操作，应由用户在专门 UI 查看范围并确认。
3. **放弃的代价**：Agent 直接执行违反既有不可逆操作边界；对话内确认难以展示完整范围、进度和失败恢复。

## 13. Unresolved Evidence, Not Contract Guessing

- 微信端 ZIP 保存/打开/转存的 exact API 与体验在 Gate 3c 前保持 `unknown`；实施时必须以当前 Uniapp/微信环境验证，不能在规划阶段写成 confirmed。
- 真实 MySQL 的历史增量一致性与孤儿数据只允许在 Gate 3a 通过后做只读聚合审计。
- 大体量导出性能与临时磁盘边界需要合成边界测试后再给出限制；当前不承诺生产 SLA。
- 用户若在 Gate 1 修改 API 名称、24h TTL、sealed policy、clear-all 冻结策略或确认方式，proposal、design、tasks 与全部 delta 必须同步修改。
