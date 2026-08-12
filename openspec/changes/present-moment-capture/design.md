# Design：Present Moment Capture（P3.1）

> 本设计依赖 `proposal.md` 与四份 spec delta。用户已于 2026-08-10 批准闸门 1 并授权闸门 2；Gate 3a 真实 MySQL、Gate 3b 真实对象存储均于 2026-08-12 授权并完成，Gate 3c 微信开发者工具 / 真机于同日授权并进入人工验收。

## 1. 推荐摘要

P3.1 使用**技术恢复 DRAFT + 用户记录 SAVED + 可选 SEALED / UNLOCKED**的单一状态机：

- 新增 `RecordStatus.SAVED` 与 `RecordType.MOMENT`；
- 保留 `POST /api/records` 作为技术 DRAFT 创建，新增幂等 `POST /api/records/{id}/save`；
- 保存条件只看非空文字或 AVAILABLE 图片/声音；
- DRAFT 使用显式 `draft_expires_at` 做 7 天滑动恢复，不进入普通记录表面；
- SAVED 是用户记录，可继续编辑；SEALED / UNLOCKED 保持不可变；
- seal 改为只接受 SAVED；“交给时间”只在保存后出现；
- 旧有效 DRAFT 迁移到 SAVED，旧类型不改写；
- 写作引导和可逆 tool write 允许 DRAFT / SAVED，但不改 Agent Prompt、provider 或调用预算；
- E0 没有胜者，前端只采用安静页面内反馈、无声音、无自动跳转的最低基线。

## 2. 状态模型

### 2.1 Canonical 状态

```text
                         用户主动保存
      ┌─────────────────────────────────────────┐
      │                                         ▼
   DRAFT ────────────────────────────────────► SAVED
     │                                            │
     │ 7 天无活动                                  │ 用户主动设置未来时间并封存
     ▼                                            ▼
过期清理重试锚点                              SEALED
                                                  │ unlockAt 到达
                                                  ▼
                                              UNLOCKED
```

状态职责：

| 状态 | 用户是否视为完整记录 | 普通列表可见 | 可编辑 | 可封存 | 过期 |
|---|---|---|---|---|---|
| `DRAFT` | 否，仅未完成恢复 | 否 | 是 | 否 | 最近活动后 7 天 |
| `SAVED` | 是 | 是 | 是 | 是 | 否 |
| `SEALED` | 是 | 是 | 否 | 已封存 | 否 |
| `UNLOCKED` | 是 | 是 | 否 | 已抵达 | 否 |

`DRAFT` 仍可被 recovery UI 发现，但不能在首页卡片、我的记录或时光轴中伪装为完整记录。

### 2.2 状态转换

- create：新技术 DRAFT，`recordType=MOMENT`，`draftExpiresAt=now+7d`；
- draft activity：正文、位置、附件 commit/delete、封面、标签或 Agent 可逆写入成功后刷新期限；
- save：DRAFT 满足成立条件时原子转 SAVED，并清空 `draftExpiresAt`；
- save retry：SAVED 返回当前 detail，作为幂等成功；
- save rejected：SEALED / UNLOCKED 拒绝，不回退状态；
- edit：DRAFT 可保持未成立；SAVED 每次变更后仍必须满足成立条件；
- seal：只允许 SAVED，要求未来 `unlockAt`；
- unlock：沿用现有 SEALED -> UNLOCKED scheduler；
- expired draft cleanup：只处理过期 DRAFT，不影响 SAVED 及以后状态。

## 3. 保存成立条件

### 3.1 单一权威

backend 提供单一 `RecordSaveEligibility`（具体类名可实现期调整）判断：

```text
hasText = trim(content) 非空
hasMedia = 至少一个 owner-scoped attachment
           且 status=AVAILABLE
           且 type in {IMAGE, VOICE}

eligible = hasText OR hasMedia
```

以下不能单独成立：title、recordType、location、cover、tag、lifeNode、unlockAt、coreQuestion、AI summary、beliefThen、pending/failed/deleted attachment。

frontend 可以做相同预检以提供即时反馈，但 backend 判断是权威；不允许只靠按钮禁用守规则。

### 3.2 SAVED 编辑后的不变量

允许用户清空 SAVED 正文，前提是仍有 AVAILABLE 图片或声音；允许删除附件，前提是仍有非空文字或另一 AVAILABLE 媒体。

任何操作若会使 `eligible=false`：

- backend 在同一业务操作内拒绝；
- 已有正文、附件、cover 与状态保持不变；
- frontend 显示“至少留下一句话、一张图片或一段声音”，不把记录悄悄降回 DRAFT。

## 4. 数据模型与迁移

### 4.1 Enum 与列

```text
RecordStatus += SAVED
RecordType   += MOMENT
record.draft_expires_at DATETIME NULL
record.record_type DEFAULT 'MOMENT'
```

`record.content` 保持 `TEXT NOT NULL`，无正文时存空字符串，避免把 nullable 语义扩散到全部读取链路。DTO 去掉 create 的 `@NotBlank`，service 统一规范化为 `""`；update 允许空字符串，但 SAVED 最终不变量仍由 eligibility 守护。

### 4.2 迁移顺序

迁移脚本必须可先只读审计，再执行：

1. 增加 `draft_expires_at` nullable 列，并把 `record_type` 默认改为 `MOMENT`；
2. 发布能读取 `SAVED / MOMENT` 的应用版本前，不执行状态数据迁移；
3. 将 `status='DRAFT'` 且 trim(content) 非空或存在 AVAILABLE IMAGE/VOICE 的记录改为 SAVED；
4. 迁移到 SAVED 时保留 record_type、原文、时间、location、attachments、cover、tags、Agent session，清空 draft_expires_at；
5. 空白且无 AVAILABLE 媒体的异常 DRAFT 保持 DRAFT，并赋 `now+7d`；
6. 记录迁移前后聚合计数，不记录用户 id、record id、原文、标题、location、storage key 或媒体元数据；
7. 在真实 MySQL 上验证枚举读取、索引计划、迁移幂等性与回滚步骤。

状态列是 VARCHAR，因此不需要数据库 enum 变更。实现必须同步 `backend/sql/mysql` 的增量 DDL、`schema.mysql.sql` 与测试 `schema.sql`，并以当前仓库实际 schema 约定为准。

### 4.3 恢复期限

- DRAFT 创建时：`draft_expires_at = clock.now + 7 days`；
- DRAFT 成功活动后：刷新为 `clock.now + 7 days`；
- SAVED / SEALED / UNLOCKED：必须为 null；
- recovery 查询只返回 `draft_expires_at > now`；
- 过期 DRAFT 对普通 detail/update/upload 路径视为不可继续，避免过期后被无意复活；
- Clock 必须注入，测试不能依赖系统当前时间。

## 5. API 与 DTO

### 5.1 保留与新增路径

| 路径 | P3.1 语义 |
|---|---|
| `POST /api/records` | 创建 active DRAFT；content 可空，recordType 缺省为 MOMENT |
| `PUT /api/records/{id}` | 更新 owner 的 active DRAFT 或 SAVED；SAVED 更新后仍须 eligible |
| `POST /api/records/{id}/save` | 新增；DRAFT -> SAVED，SAVED 幂等返回，其他状态拒绝 |
| `POST /api/records/{id}/seal` | 只接受 SAVED；要求未来 unlockAt |
| `GET /api/records?status=DRAFT` | 仅供 recovery UI，返回 owner 的 active DRAFT |
| `GET /api/records`（无 DRAFT filter） | 只返回 SAVED / SEALED / UNLOCKED |
| location / attachment / cover / tag / Agent write | DRAFT / SAVED 可变，SEALED / UNLOCKED 拒绝 |
| `DELETE /api/records/{id}` | P3.1 仅保留放弃 active DRAFT；任意状态删除留 P3.2 |

不新增“save=true”隐式参数，不让普通 update 顺便改变状态。

### 5.2 DTO 兼容

- `CreateRecordRequest.content`：允许 null/blank，service 规范化为空字符串；
- `CreateRecordRequest.recordType`：允许缺省，service canonical default 为 MOMENT；
- `UpdateRecordRequest.content`：允许 blank，但字段仍参与 full update；
- `UpdateRecordRequest.recordType`：保持必填，避免全量 update 意外清空类型；
- `RecordDetailVO / RecordListItemVO / TimelineItemVO`：复用现有 status/recordType 字段承载新 enum，不新增 `isSaved` 或重复状态布尔值；
- `SaveRecordRequest`：无请求体；save 使用当前持久化状态，避免更新与状态迁移在两个权威中重复；
- 错误继续使用既有 `BAD_REQUEST / NOT_FOUND` 外壳，具体用户文案由现有错误映射处理，不新增基础错误协议。

## 6. 可编辑矩阵

| 能力 | DRAFT | SAVED | SEALED | UNLOCKED |
|---|---|---|---|---|
| 正文/标题/类型/人生节点 | 可改 | 可改且保持 eligible | 拒绝 | 拒绝 |
| location | 可改 | 可改 | 拒绝 | 拒绝 |
| attachment add/delete | 可改 | 可改且保持 eligible | 拒绝 | 拒绝 |
| cover | 可改 | 可改 | 拒绝 | 拒绝 |
| tags | 可改 | 可改 | 拒绝 | 拒绝 |
| 写作引导 Agent | 可用 | 可用 | 拒绝 | 用 REVIEW_CHAT 而非写作引导 |
| Agent 可逆 tool write | 可用 | 可用且保持 eligible | 拒绝 | 拒绝 |
| seal | 拒绝 | 可用 | 拒绝 | 拒绝 |
| 普通列表展示 | 不展示 | 展示 | 展示 | 展示 |

实现时应把 `ensureDraft` 拆成语义明确的 `ensureActiveDraft`、`ensureEditableRecord`、`ensureSaved` 等单一权威，禁止在 controller、service、mapper 和 Agent 各自维护不同状态集合。

Mapper 的 update/delete/transition 必须在 WHERE 中带 owner + expected status，不能只做 service 前置检查。

## 7. 恢复草稿与清理

### 7.1 前端恢复

```text
进入新建编辑器
  -> 查询最近 active DRAFT（status=DRAFT, pageSize=1）
  -> 无草稿：展示空编辑器
  -> 有草稿：克制提示“继续上次未完成的记录 / 放弃”
       ├─ 继续：加载 DRAFT，刷新期限
       └─ 放弃：调用既有 DELETE，仅允许 DRAFT
```

恢复提示不能使用“已保存记录”措辞，也不能把 DRAFT 放进“我的记录”计数。

关闭页面时：

- 没有任何文字、媒体或辅助字段：不创建 DRAFT；
- 有未确认内容：创建/更新 DRAFT 后离开，不显示“这一刻已经留下”；
- 媒体正在上传：提示等待或明确放弃，不把 pending 媒体计为已保存；
- 已是 SAVED 且有合法修改：按显式保存/更新语义处理，不能静默降级为 DRAFT。

### 7.2 过期清理

新增窄 scheduler，批量选择 `status=DRAFT AND draft_expires_at<=now`：

1. owner/status/expiry 条件再次确认；
2. 对 persisted AVAILABLE/DELETED attachment 使用其 storage provider 做幂等删除；
3. 对象已不存在视为清理完成；
4. 任一远端删除失败时保留 record/attachment DB 行，下一轮重试；
5. 全部远端对象清理完成后再删除 DRAFT，依赖 FK cascade 清理 location/tag/attachment metadata；
6. 并发活动通过 expected expiry/status 条件阻止误删刚被刷新或已 SAVED 的记录；
7. 日志只记录批次计数、成功/失败计数与类型化失败类别，不记录内容、key、URL 或凭据。

这是技术 DRAFT 的窄清理，不等同 P3.2 的任意状态删除与完整数据所有权编排。

## 8. Frontend 主路径

### 8.1 信息层级

P3.1 不复制 E0 变体。推荐可逆基线：

```text
首页：留下此刻
  -> 编辑器主区：文字 / 图片 / 声音
  -> 主动作：留下这一刻
  -> 页面内状态：这一刻已经留下
  -> 用户可以离开
  -> 次级可选：还想补充 / 交给时间 / 主动打开 Agent
```

标题、旧记录类型、人生节点、地点、标签、Agent 与解锁时间放入可选区，不参与保存 eligibility。最终折叠方式、持续时间和动效仍标为 provisional。

### 8.2 用户可见状态

| backend | 用户文案 |
|---|---|
| DRAFT | 不作为记录状态展示；恢复提示为“上次未完成” |
| SAVED | “已留下” |
| SEALED | “封存中” |
| UNLOCKED | “已抵达” |

普通首页、列表与时光轴默认展示 SAVED / SEALED / UNLOCKED。详情页对 SAVED 提供编辑入口；SEALED / UNLOCKED 继续只读。

### 8.3 保存反馈

- 使用页面内、安静、可被读屏读取的状态区域；
- 文案基线：“这一刻已经留下”；
- 不播放声音、不震动、不自动跳页面；
- 不自动展开 Agent、分享、封存或其他补充项；
- 网络失败时保持当前输入和媒体状态，明确可重试，不显示假成功；
- E0 没有用户证据，因此不把 toast、底部 sheet 或某个动画写成 acceptance。

## 9. Agent 兼容

P3.1 不改 Prompt、护栏、memory、provider 或 trace 语义，只调整记录状态资格：

- `WRITING_GUIDANCE` 接受 DRAFT / SAVED；
- `REVIEW_CHAT` 继续只接受 UNLOCKED；
- C2 tool proposal 仍只包含可逆正文/标签/解锁时间类动作，但其业务执行允许 DRAFT / SAVED；
- 对 SAVED 执行工具后必须保持 eligibility；
- Agent 不能调用 save/seal/delete，不能代替用户完成“这一刻已经留下”；
- material 只有用户显式确认后才写入，写入 DRAFT 不自动 save，写入 SAVED 不改变 SAVED；
- 既有 provider call count、C6 snapshot 和 C8 budget 不应因状态扩展发生语义变化。

## 10. Preview 与 session 隔离

- authenticated real path 必须调用新 backend lifecycle；
- Preview 继续 read-only，任何 create/update/save/seal/attachment/location/cover/Agent mutation 均 fail-closed；
- Preview fixtures MAY 展示 MOMENT / SAVED 示例，但必须保留“概念预览 · 示例数据 · 只读”；
- Preview 的 SAVED 示例不构成真实 migration、对象存储或用户理解证据；
- real login / WeChat login 继续清除 Preview session，Preview 不持有真实 token。

## 11. 验证策略

### 11.1 离线与 H2

- 状态机：create DRAFT、save、幂等 save、SAVED edit、seal、unlock；
- eligibility：text/image/voice 正例，pending/failed/title/location/AI 反例；
- SAVED 不变量：清空正文/删除最后媒体的交叉组合；
- owner/status 条件、DRAFT expiry、并发刷新/清理 expected-state；
- legacy migration fixture、MOMENT 默认、旧类型保留；
- Agent DRAFT/SAVED 写作引导与 tool write，SEALED/UNLOCKED 拒绝；
- Preview mutation fail-closed；
- 日志/trace/exception 不含原文、storage key、URL、prompt/provider response。

### 11.2 真实 MySQL（闸门 3）

Gate 3a 于 2026-08-12 完成。执行前后均只保留聚合证据：迁移前 3 条 DRAFT 全部为有效文字记录、无空白异常和附件 owner/orphan 异常；迁移后 3 条成为 SAVED 且保留 FUTURE_LETTER，schema/default/index/UTC+8、第二次执行稳定性和 backend list/timeline 读取 PASS。没有创建合成数据库行。真实并发 refresh/save 与 cleanup race 仍只由自动化测试覆盖，不扩写为真实并发证据。

- 迁移前只读计数与迁移后状态/type/expiry 聚合；
- content 空字符串、AVAILABLE attachment EXISTS、owner/status 索引与事务语义；
- DRAFT expiry race：清理选中后被刷新或 save 时不能误删；
- 脚本重复运行结果稳定；
- 合成数据 finally 清理。

### 11.3 真实对象存储（闸门 3）

Gate 3b 于 2026-08-12 完成。探针由显式 `P31_STORAGE_PROBE=1` 门控，默认回归保持跳过和零外调；执行时使用 ignored 本地配置、真实 MySQL 与私有 S3-compatible 对象存储，并强制 AI mock。证据只输出场景布尔值和聚合清理结果，不输出 user/record id、object key、signed URL、bucket 或 credential。

- 合成 PNG 图片-only 完成 upload authorization -> upload -> commit AVAILABLE -> save -> private read/字节一致 -> SAVED edit；
- 固定合成短 WAV 声音-only 完成 upload authorization -> upload -> commit AVAILABLE -> save -> private read/字节一致，并通过 JVM 标准音频解码；微信扬声器播放与权限体验仍由 Gate 3c 验证；
- pending/不存在对象不得 save；
- 过期 DRAFT 删除远端对象后清 DB；对象已不存在按幂等成功处理；模拟远端鉴权失败时保留重试锚点，恢复配置后可清理；
- finally 验证所有探针对象不存在，合成 user/record/attachment 聚合均为 0；
- 不记录 key、signed URL 或 secret；真实 AI provider 调用为 0。

### 11.4 微信开发者工具 / 真机（闸门 3）

- 文字-only、图片-only、声音-only；
- 返回/重进恢复，保存后离开，SAVED 再编辑，保存后交给时间；
- 麦克风/相册权限拒绝、上传失败、媒体处理中返回；
- Preview 只读与 authenticated real path 隔离；
- 只报告任务完成与误解，不把 E0 倒填为用户访谈。

### 11.5 静态范围

- frontend type-check 与 mp-weixin build；
- backend focused 与全量 Maven；
- schema / mapper / DTO / VO / enum / Preview fixture exact-match；
- `git diff --check`、path allowlist、凭据/隐私增量扫描；
- OpenSpec Requirement / Scenario、delta 落点与 ACTIVE_TASK 指针文件级校验；CLI 缺失记 SKIPPED。

## 12. 决策记录

### 决策 1：用户完成状态叫 SAVED 还是 RECORDED

1. **面临的选择**：新增 `SAVED`；新增 `RECORDED`；继续复用 DRAFT 只换用户文案。
2. **选了哪个 + 为什么**：推荐 `SAVED`。它直接表达用户主动完成保存，与蓝图状态轴、API 动作和“已留下”中文文案能形成一一映射。
3. **放弃的代价**：`RECORDED` 更像采集过程且中文映射不稳定；复用 DRAFT 会继续把技术恢复与用户完成耦合，无法形成可信契约。

### 决策 2：新记录默认类型是否复用旧三类

1. **面临的选择**：新增 MOMENT；默认 EMOTION_NOTE；继续默认 FUTURE_LETTER；批量把旧记录改成 MOMENT。
2. **选了哪个 + 为什么**：推荐新增 MOMENT，只用于新记录默认。普通生活片段不一定是情绪、节点或写给未来，独立类型最符合核心产品定义。
3. **放弃的代价**：复用旧类型会继续强迫错误分类；批量改历史类型会重写用户当时选择的语义。

### 决策 3：保存是独立命令还是 update 的隐式副作用

1. **面临的选择**：新增 `/save`；PUT update 自动转 SAVED；POST create 直接生成 SAVED。
2. **选了哪个 + 为什么**：推荐独立、幂等 `/save`。用户主动完成动作与状态迁移明确，媒体上传可先依赖 DRAFT id，网络重试也有清楚语义。
3. **放弃的代价**：update 隐式转状态会把编辑和承诺混在一起；create 直接 SAVED 无法支持 media-first 和崩溃恢复。

### 决策 4：保存资格由什么证据决定

1. **面临的选择**：只要任意字段非空；正文或客户端选中媒体；正文或 backend 已验证 AVAILABLE 媒体。
2. **选了哪个 + 为什么**：推荐 backend 的非空正文或 AVAILABLE IMAGE/VOICE。它只承诺已经持久化、可访问的用户证据，且能被 API 直接调用时守住。
3. **放弃的代价**：任意字段会让标题/标签成为空记录；客户端选中或 pending 媒体可能上传失败，形成假保存。

### 决策 5：恢复草稿期限用 updated_at 还是独立列

1. **面临的选择**：localStorage；复用 record.updated_at；新增 draft_expires_at。
2. **选了哪个 + 为什么**：推荐 `draft_expires_at`。媒体 commit、location、cover 等活动未必同步 record.updated_at，显式期限才能形成可审计、可并发守护的 7 天语义。
3. **放弃的代价**：localStorage 无法承接真实媒体或跨端；updated_at 容易被无关更新延长或漏掉真实活动。

### 决策 6：过期草稿能否只删数据库

1. **面临的选择**：DB cascade 后不管远端对象；先删远端成功再删 DB；把所有删除都提前扩成 P3.2 队列。
2. **选了哪个 + 为什么**：推荐 P3.1 内只做窄 DRAFT 重试清理：远端成功/不存在后再删 DB，失败保留行作为重试锚点。这守住隐私又不扩成全状态删除平台。
3. **放弃的代价**：只删 DB 会遗留无法追踪的私人对象；提前做 P3.2 会卷入封存、回信、Agent 会话、清除全部和导出，超出一刀。

### 决策 7：SAVED 是否允许继续编辑全部上下文

1. **面临的选择**：SAVED 不可编辑；只允许正文；允许正文、位置、附件、封面、标签与可逆 Agent 写入。
2. **选了哪个 + 为什么**：推荐完整可编辑矩阵，并由 backend 维持 eligibility。SAVED 表示“已经成立”而非“冻结”，封存才是不变性边界。
3. **放弃的代价**：不可编辑会把普通保存变成不可逆；只放正文会让媒体、位置、封面与状态语义裂开。

### 决策 8：E0 无结论时采用哪个生产交互

1. **面临的选择**：直接选 A/B/C；无限期阻塞 P3.1；只采用冻结蓝图最低反馈并保持细节 provisional。
2. **选了哪个 + 为什么**：推荐第三种。核心能力已有产品定义与蓝图依据，E0 又不是硬依赖；但没有用户证据就不能声称某个变体获胜。
3. **放弃的代价**：直接选胜者会伪造研究；无限期等待不存在的用户会阻断确定的核心能力。

### 决策 9：为什么 P3.1 不实现所有状态删除

1. **面临的选择**：按 D42 一次实现任意删除；只做 DRAFT 放弃/过期；完全不清理草稿。
2. **选了哪个 + 为什么**：推荐只做技术 DRAFT 的必要清理。蓝图 P3.1 意图卡明确把全量删除列为非目标，P3.2 承担关联数据、远端对象、清除全部与失败恢复。
3. **放弃的代价**：提前全做会吞并 P3.2；完全不清理又违反短期恢复和私人数据最小保留。

### 决策 10：记录内容与媒体是否进入清理/迁移日志

1. **面临的选择**：记录详细 row、key 和失败上下文；记录 id 便于排查；只记录聚合计数与类型化失败类别。
2. **选了哪个 + 为什么**：推荐只记录聚合和无内容类别。用户日记与私有媒体是高敏数据，迁移/清理可通过合成探针和数据库计数验证，无需把内容复制到日志。
3. **放弃的代价**：详细日志会形成新的敏感副本；record id 与 storage key 组合也会扩大可关联性和泄露面。

## 13. 未决与 Gate State

- N1–N11、`draft_expires_at`、`/save`、7 天滑动期限、SAVED 可编辑矩阵与窄 DRAFT 清理已于 2026-08-10 按推荐方案通过闸门 1；
- E0 精确交互仍为 unknown，不会在 Gate 1 后变成 confirmed；
- 闸门 2 已于 2026-08-10 授权，允许按 `tasks.md` 修改 backend/frontend/SQL 并执行离线/H2/build 验证；
- 闸门 2 范围内实现已完成并通过自动化回归，当前等待用户审查；
- Gate 3a 与 Gate 3b 已于 2026-08-12 授权并完成；Gate 3c 已于 2026-08-12 授权，当前等待用户完成权限、媒体播放与交互矩阵后登记；
- 任一 exact API、字段、状态或清理语义在用户 Gate 1 修改后，必须同步 proposal、design、tasks 与 delta，不能只改其中一份。
