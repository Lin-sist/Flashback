# Proposal：Data Ownership Foundation（P3.2）

## 1. Summary

P3.2 将 Flashback 当前“看起来能备份、实际上没有真实结果”的残留页面，替换为真实、owner-scoped、可验证的数据所有权能力：

- 导出离线可读的完整副本；
- 删除任意状态的单条记录；
- 清除当前账号下的全部记录；
- 在删除记录前可靠清理私有对象存储，并在失败或中断后可恢复；
- 同步清理位置、附件、回信、提醒、Agent 会话、消息、工具调用与轨迹等关联数据；
- 在小程序提供真实的“数据与所有权”页面，Preview 始终只读。

本 change 只建立 P3.2 规划工件。未获得 Gate 1 规划批准前，不修改业务代码；规划批准也不自动授权实现、真实 MySQL、对象存储、微信外调、push、部署或发布。

## 2. Why Now

- P3.1 `present-moment-capture` 已归档，`ACTIVE_TASK` 已回到 `IDLE`，冻结蓝图的下一候选正是 P3.2。
- 当前后端只允许删除 DRAFT；SAVED、SEALED、UNLOCKED 没有用户可用的删除语义。
- 当前 DRAFT 删除直接删除数据库记录，数据库级联不会删除远端私有对象，存在对象遗留风险。
- `frontend/src/pages/user-center/data-backup/index.vue` 仍保留固定备份时间、固定数量、iCloud、PDF/纯文本等演示内容；H0 已通过移除路由和入口避免其对真实用户可见，但真实能力仍未建立。
- 记录、附件、位置、回信、提醒、Agent 会话/消息/工具调用/轨迹已有 owner 与外键基础，适合在不做大规模重写的前提下补齐可靠所有权编排。

## 3. User Story

> 改前，用户可能看到过“数据备份 / 导出 / 清除全部”的设计，但真实路径没有可下载结果，封存或已解锁记录也不能删除。
>
> 改后，用户可以拿到一个离线可读、媒体可校验、Agent 内容明确分区的副本；也可以由自己确认删除任意记录或清除全部记录，并看到失败重试状态，而不是得到假成功。

## 4. Goals

1. 建立一个 owner-scoped 的数据操作模型，统一承载导出、单条删除与清除全部。
2. 导出包至少包含 `index.html`、`records/*.md`、`media/`、`agent/`、`manifest.json`、`README.txt`，无需网络即可阅读。
3. 用户原文、明确填写字段、位置、附件与必要元数据进入导出；AI/Agent 内容单独标注、单独存放，不混成用户原话。
4. SEALED 内容由用户在 `RESPECT_SEAL` 与 `FULL_CONTENT` 之间显式选择；默认尊重封存。
5. 任意状态记录删除与清除全部采用可重试操作；远端对象删除成功或确认不存在后，才删除数据库聚合。
6. 删除完成后，关联位置、附件、标签关系、回信、提醒、解锁证据、Agent 会话、消息、工具调用与轨迹不得残留。
7. 清除全部确认后冻结该用户的记录写入，直到操作完成或进入明确的可重试状态，避免“清除全部”仍留下并发新记录。
8. 小程序真实展示操作范围、进度、失败与重试；Preview 不产生导出文件、不删除数据。
9. 日志、任务证据和 tracked files 不写入用户日记原文、媒体内容、位置详情、storage key、signed URL、下载 token 或 secret。

## 5. Non-goals

- 账号注销、用户账号生命周期与身份解绑；
- 生产级备份、灾难恢复、iCloud、跨端同步、自动云备份或从备份恢复；
- PDF-only 导出、富排版出版、公开分享页；
- 订阅付费、套餐、空间计费或营销入口；
- Agent 代替用户执行删除、清除全部或导出；
- 新增 Agent Prompt、provider、memory、reflection、评分、诊断、STT 或声音分析能力；
- deployment、monitoring、admin portal、SMS、production notification center；
- package/lockfile 修改、大规模 backend rewrite 或 major frontend visual reconstruction。

## 6. Current Capability Classification

### confirmed

- authenticated record detail/list/timeline 均按 `userId` 做 owner scope；Preview mutation 已 fail-closed。
- 记录关联表已有数据库外键级联基础；Agent session、message、tool call、turn trace 均带 user/record/session 归属。
- Qiniu 与 S3-compatible provider 都实现了私有对象删除；对象不存在可以被区分为幂等成功条件。
- P3.1 已实现对过期 DRAFT 的“先删远端对象、后删数据库；失败保留重试锚点”。
- 小程序已有用户中心、记录详情和真实 authenticated service 层，可承载新入口。

### partial

- `DELETE /api/records/{id}` 仅允许 DRAFT，且当前路径没有先清理远端附件。
- 单附件删除已有远端删除语义，但只适用于 DRAFT/SAVED 编辑态，不等于删除整个记录聚合。
- 数据库级联覆盖大部分关联数据，但生产 schema 由基线与多份增量脚本共同构成，实施前仍须逐表核对真实 MySQL。
- `data-backup` 页面文件存在，但路由与真实用户入口已被 H0 移除；页面内容仍是固定演示状态。

### planned

- 离线导出包、导出任务状态、短期私有下载与过期清理；
- 任意状态单条删除、清除全部、显式确认、进度、重试与并发写入门；
- “数据与所有权”页面及记录详情中的真实删除入口；
- P3.2 的真实 MySQL、对象存储和微信端验收矩阵。

### unknown

- 当前真实 MySQL 中各历史增量脚本的逐表/逐外键一致性，须在 Gate 3a 只读 preflight 后确认；
- 当前真实对象存储中的对象规模、失败分布与 provider 兼容性，须在 Gate 3b 使用合成对象验证；
- 当前微信开发者工具/真机对 ZIP 保存、再次打开或转存的最终交互，须在 Gate 3c 人工验证，不能由 build 代替；
- 大体量导出在当前机器上的耗时和磁盘峰值，须以合成边界数据测量，不能提前声称 SLA。

### out_of_scope

- 账号注销、生产备份/恢复、云同步、收费订阅、PDF 出版、对外分享、生产 SLA。

## 7. Proposed Scope

### 7.1 Backend

- 新增 data ownership operation 与 record item 持久化模型；
- 新增 summary、export、download、deletion intent/confirm、status、retry API；
- 统一删除编排，迁移/关闭当前不清理远端对象的直接 DRAFT 删除路径；
- 新增导出构建器、删除 worker、操作互斥与过期清理；
- 使用 JDK ZIP/HTML/JSON 能力，不新增第三方打包依赖。

### 7.2 Export

- 默认 `RESPECT_SEAL`；用户可明确选择 `FULL_CONTENT`；
- DRAFT 标注为“未完成草稿”，SAVED/SEALED/UNLOCKED 保留真实状态；
- 原文与用户字段放在 `records/`，Agent 会话放在 `agent/`，内部技术轨迹只以必要、脱敏的说明或元数据呈现；
- 媒体使用稳定离线相对路径，并在 manifest 中记录 SHA-256、字节数、媒体类型与所属记录；
- 导出临时文件默认 24 小时过期，下载必须再次校验 owner。

### 7.3 Deletion

- 单条记录与清除全部共用显式 intent + confirm 流程；
- 确认后的目标记录从常规页面隐藏并拒绝继续写入；
- 对每个附件执行真实 provider 删除；成功或 not-found 后再删除 record 聚合；
- 中断、超时和 provider failure 保留 operation/item 重试锚点，不写假成功；
- 清除全部按确认时的 owner snapshot 建立任务，并在任务结束前阻止该用户创建或修改记录。

### 7.4 Mini Program

- 在个人中心新增“数据与所有权”真实入口；
- 重写现有未注册 `data-backup` 页面，不复用固定日期、数量、iCloud、自动备份或 PDF 文案；
- 记录详情对 DRAFT/SAVED/SEALED/UNLOCKED 提供真实删除入口；
- 提供导出策略选择、操作进度、失败原因类别、重试与 clear-all 强确认；
- Preview 显示只读说明并拒绝全部操作。

### 7.5 Agent And Derived Data

- Agent 不获得导出或删除工具；重要操作继续只能由用户在产品 UI 明确确认；
- 导出可包含用户可见的 Agent 会话，但必须与用户原文物理分区；
- 删除记录时，record-linked session/message/tool/trace 必须一起清理；发现无法归属的派生数据时 fail-closed 并进入修复/重试，不静默遗留。

## 8. Spec Delta Map

- `backend-core`：操作模型、导出、任意状态删除、对象清理、并发与隐私；
- `miniapp-core`：数据与所有权页面、导出/删除交互、Preview 边界；
- `v2-product-scope`：P3.2 数据主权成立与账号注销/生产备份边界；
- `agent-runtime`：Agent 内容导出分区、record-linked 派生数据删除、禁止 Agent 代执行；
- `agent-collaboration`：导出结构/哈希、删除中断恢复、真实依赖与隐私证据规范。

## 9. Evidence Plan

- 文件级：delta 结构、Requirement/Scenario、链接、状态指针、路径 allowlist、privacy/credential scan；
- 离线/H2：owner、状态机、幂等、操作互斥、ZIP 结构、HTML 离线、Markdown、manifest hash、失败恢复；
- build：backend focused/full Maven、frontend type-check、standard/Preview mp-weixin build；
- Gate 3a：真实 MySQL schema/preflight、迁移、级联与中断恢复；
- Gate 3b：私有对象存储合成媒体导出、hash、删除/not-found/retry 与清理；
- Gate 3c：微信端导出保存/打开或转存、任意状态单删、清除全部、失败与 Preview 矩阵；
- 真实 Agent provider 调用预算为 0，本 change 不需要 provider 验收。

## 10. Responsibility And Gate State

- 开工锚点：`efd2618`。
- 提交责任：用户于 2026-08-12 明确授权 Agent commit；仍不包含 push、PR、部署或发布。
- 规划期外部调用预算：0；未连接真实 MySQL、对象存储、微信或 Agent provider。
- OpenSpec CLI：当前 shell 未安装，`openspec new change` 已失败；本轮采用仓库既有文件级 scaffold，并将 CLI validation 记为 `SKIPPED`。
- Gate 1：用户于 2026-08-12 以“授权实现”批准本 proposal、design、tasks、五份 delta 与推荐决策。
- Gate 2：用户于 2026-08-12 明确授权按 `tasks.md` 实现。
- Gate 3a/3b/3c：均未授权。
