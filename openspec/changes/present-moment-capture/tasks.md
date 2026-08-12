# Tasks：Present Moment Capture（P3.1）

> 当前阶段：实现、Gate 3a 真实 MySQL与 Gate 3b 真实对象存储验收完成，Gate 3c 微信开发者工具 / 真机验收执行中。闸门 1 已批准、闸门 2 已授权、Gate 3a/Gate 3b 已完成，Gate 3c 已于 2026-08-12 授权。

## 阶段 0：规划与范围闸

- [x] **T-01** 读取 `AGENTS.md`、`.ai/ACTIVE_TASK.md`、冻结蓝图 v2.0、核心产品定义 v0.1 与 Type C checklist
- [x] **T-02** 确认开刀前 `ACTIVE_TASK=IDLE`、工作树 clean、HEAD=`2d9544a`、同名 change 不存在
- [x] **T-03** 确认 H0 已实现/验收，E0 为 `INCONCLUSIVE / SKIPPED` 而非 PASS
- [x] **T-04** 核对 accepted `backend-core`、`miniapp-core`、`agent-runtime`、`v2-product-scope` specs
- [x] **T-05** 核对当前 enum、DTO、controller/service/mapper、record editor、媒体、位置、封面、Preview 与 Agent 状态门
- [x] **T-06** 记录现状能力五态与 P3.1 / P3.2 / P4.x 边界
- [x] **T-07** 创建 proposal、design、tasks 与四份 delta
- [x] **T-08** 记录规划期外调预算 0；OpenSpec CLI 缺失只做文件级校验
- [x] **T-09 GATE 1** 用户批准 proposal / design / tasks / delta 与 N1–N11（2026-08-10，按推荐方案）
- [x] **T-10** 若用户修改 exact enum/API/TTL/清理/交互决策，同步全部 artifacts 后重新请求 Gate 1（N/A：用户未修改推荐方案）

## 阶段 1：实现授权与 baseline

- [x] **T-11 GATE 2** 用户明确授权按 tasks 修改业务代码（2026-08-10）
- [x] **T-12** 更新 proposal Gate State、`.ai/ACTIVE_TASK.md` Current Progress 与 AGENT_LOG，记录实现授权但不扩大外调
- [x] **T-13** 运行 P3.1 focused 既有测试与 backend 全量 Maven baseline，记录 suites/tests/failures/errors/skipped（focused 7 suites / 126 tests；full 87 suites / 664 tests / 8 skipped，零失败/错误）
- [x] **T-14** 运行 frontend `vue-tsc --noEmit`、标准 mp-weixin build 与 Preview build baseline（全部 PASS）
- [x] **T-15** 固定本轮路径 allowlist；确认 package/lockfile、deployment、monitoring、archive 与冻结蓝图不在范围
- [x] **T-16 GATE 3a** 在任何真实 MySQL DDL / 数据审计前单独获得授权（2026-08-12）
- [x] **T-17** Gate 3a 通过后先做只读聚合：DRAFT 总数、正文有效数、AVAILABLE 媒体数、异常数与旧类型分布；不读取/输出原文（3 条 DRAFT 均有有效文字、0 条含 owner-scoped AVAILABLE 媒体、0 条空白异常、0 条附件 owner/orphan 异常；旧类型均为 FUTURE_LETTER）

实现 allowlist：

- `backend/sql/mysql/` 的 P3.1 增量脚本与 `schema.mysql.sql`；
- `backend/src/main` 中 record / attachment / location / cover / tag / Agent editable-state / narrow draft cleanup 直接相关文件；
- `backend/src/test` 与 `backend/src/test/resources/schema.sql` 的 P3.1 回归；
- `frontend/src` 中 record types/service/store、record editor/detail/list/home/timeline/card、Preview fixtures 与现有 Agent entry 直接相关文件；
- 本 change artifacts、`.ai/ACTIVE_TASK.md` 与 append-only `.ai/AGENT_LOG.md`。

明确 denylist：package/lockfile、deployment、monitoring、admin、archive、accepted baseline specs、冻结蓝图、无关 Agent Prompt/provider/memory/guardrail/eval snapshot。

## 阶段 2：DDL 与迁移优先

- [x] **T-18 RED** 增加 schema contract 测试：`draft_expires_at`、MOMENT 默认、SAVED 读取与索引/列同步当前应失败
- [x] **T-19** 新增 P3.1 MySQL 增量脚本，包含只读 preflight、列/default、有效 DRAFT -> SAVED、异常 DRAFT TTL 与聚合 postflight
- [x] **T-20** 同步 `backend/sql/mysql/schema.mysql.sql` 与 `backend/src/test/resources/schema.sql`
- [x] **T-21** Gate 3a 通过后，在真实 MySQL **先执行 DDL/迁移**，再启动依赖新 schema 的应用代码；记录回滚与幂等检查（3 条有效旧 DRAFT -> SAVED，原类型保留；第二次执行结果稳定；后端 list/timeline HTTP 200；补偿式回滚条件已记录）
- [x] **T-22 GREEN** schema contract、enum round-trip、旧类型保留与 migration fixture PASS
- [x] **T-23** 验证迁移日志/脚本输出只有聚合计数，不含 user/record id、原文、位置、storage key 或 URL

## 阶段 3：Record enum、DTO 与状态机（TDD）

- [x] **T-24 RED** `RecordStatus.SAVED`、`RecordType.MOMENT`、新建默认 MOMENT 用例
- [x] **T-25 GREEN** 同步 backend/frontend enum、status/type mapping 与 Preview fixtures
- [x] **T-26 RED** create active DRAFT 可空正文、7 天 expiry、owner 隔离、旧 client payload 兼容
- [x] **T-27 GREEN** 调整 create DTO/service/mapper；空正文规范化为 `""`，recordType 缺省为 MOMENT
- [x] **T-28 RED** eligibility matrix：text/image/voice 正例；blank/pending/failed/deleted/title/location/tag/AI 反例
- [x] **T-29 GREEN** 建立 backend 单一 `RecordSaveEligibility` 权威，查询 owner-scoped AVAILABLE IMAGE/VOICE
- [x] **T-30 RED** save transition：DRAFT -> SAVED、SAVED 幂等、SEALED/UNLOCKED/expired/cross-owner 拒绝
- [x] **T-31 GREEN** 新增 `POST /api/records/{id}/save` 与 expected-state mapper update；成功清空 draft expiry
- [x] **T-32 RED** seal 只允许 SAVED 且 unlockAt 在未来；DRAFT/SEALED/UNLOCKED 拒绝
- [x] **T-33 GREEN** 将 seal transition 改为 SAVED -> SEALED，保留现有解锁 scheduler 与不变性

## 阶段 4：SAVED 可编辑矩阵（TDD）

- [x] **T-34 RED** DRAFT/SAVED 正文、类型、人生节点、location、cover、tags 的允许矩阵；SEALED/UNLOCKED 拒绝
- [x] **T-35 GREEN** 将 `ensureDraft` 拆成 active-draft/editable/saved 等语义门，mapper WHERE 同步 expected statuses
- [x] **T-36 RED** SAVED 清空正文但仍有媒体 PASS；无媒体时 FAIL 且旧正文不变
- [x] **T-37 RED** SAVED 删除最后媒体但仍有正文 PASS；正文为空时 FAIL 且媒体/cover 不变
- [x] **T-38 GREEN** 在正文更新与 attachment delete 的事务边界执行最终 eligibility，禁止 SAVED 静默降回 DRAFT
- [x] **T-39** 允许 DRAFT/SAVED 的 location、attachment、cover、tag 更新刷新 active DRAFT expiry；SAVED 不写 expiry
- [x] **T-40** 保持 SEALED/UNLOCKED 的正文、位置、附件、封面不可变，新增回归覆盖直接 API 绕过

## 阶段 5：恢复草稿与过期清理（TDD）

- [x] **T-41 RED** 普通 list/home/timeline 默认排除 DRAFT；显式 `status=DRAFT` 只返回 owner 的未过期 DRAFT
- [x] **T-42 GREEN** 实现 query-level DRAFT visibility / expiry 规则，不让技术 DRAFT 进入用户记录总数
- [x] **T-43 RED** DRAFT create/update/media/location/cover/tag/Agent 活动按注入 Clock 滑动刷新 7 天；过期不复活
- [x] **T-44 GREEN** 建立单一 draft activity touch 路径，避免 expiry 更新散落
- [x] **T-45 RED** cleanup：无媒体过期 DRAFT 删除；有媒体先删远端；远端失败保留 DB；对象不存在幂等成功
- [x] **T-46 GREEN** 新增窄 DRAFT cleanup scheduler，使用 status+expiry expected condition 防止与 refresh/save 竞态误删
- [x] **T-47** 清理日志只含 batch/success/failure/retry counts 与类型化类别；敏感内容、key、URL、credential 为 0
- [x] **T-48** 确认任意 SAVED/SEALED/UNLOCKED 单条删除、清除全部与完整删除队列仍留 P3.2

## 阶段 6：Agent 状态兼容（TDD）

- [x] **T-49 RED** WRITING_GUIDANCE 对 DRAFT/SAVED 可开会话，SEALED/UNLOCKED 拒绝；REVIEW_CHAT 仍只接受 UNLOCKED
- [x] **T-50 GREEN** 将单一 required status 改为类型化 allowed-status policy，不复制状态集合到 controller
- [x] **T-51 RED** 可逆 tool write 对 DRAFT/SAVED 可执行并保持 eligibility；SEALED/UNLOCKED 拒绝
- [x] **T-52 GREEN** 复用 RecordService 权威路径；Agent 不获得 save/seal/delete/attachment/location/cover 旁路
- [x] **T-53** 验证 Prompt、guardrails、memory、reflection、provider call count、C8 budget 与 trace 外部语义不变
- [x] **T-54** 审查 C6 snapshots；P3.1 不应因纯状态资格产生回复质量基线变化，任何变化须停止并解释

## 阶段 7：Frontend 当下保存主路径

- [x] **T-55** 新记录默认 MOMENT；主区只保留文字/图片/声音与“留下这一刻”，旧类型等进入可选区
- [x] **T-56** 移除 media-first 的“先写正文”门；先创建技术 DRAFT，再走既有真实 upload/commit
- [x] **T-57** 接入显式 save：先持久化当前编辑，再调用 `/save`；pending/failed media 不显示成功
- [x] **T-58** 页面内展示“这一刻已经留下”，无声音、无震动、无自动跳转/封存/分享/Agent
- [x] **T-59** 保存失败保留输入与媒体状态，提供重试；不得使用本地 mock success
- [x] **T-60** 关闭未确认内容只形成 recovery DRAFT；无内容不建草稿；媒体处理中返回明确受控
- [x] **T-61** 新建编辑器发现 active DRAFT 时提供“继续上次未完成 / 放弃”，不把它叫已保存记录
- [x] **T-62** SAVED 详情/编辑/list/home/timeline 与状态 label “已留下”接通；空正文 media-only 卡片有诚实 fallback
- [x] **T-63** “交给时间”仅在 SAVED 后作为次级入口；继续要求未来时间，未选择也不制造未完成态
- [x] **T-64** Preview 保持 read-only 与明确标识；fixtures 可展示 MOMENT/SAVED，但所有 mutation fail-closed
- [x] **T-65** 三个一级 Tab 与“我的记录、时光轴、时间回看”命名保持；不做 major visual reconstruction

## 阶段 8：自动化回归与范围守护

- [x] **T-66** backend P3.1 focused tests PASS，再运行全量 Maven；记录 **91 suites / 687 tests / 0 failures / 0 errors / 8 skipped**
- [x] **T-67** frontend `vue-tsc --noEmit`、标准 mp-weixin build、Preview build PASS
- [x] **T-68** migration/schema/enum/mapper/DTO/VO/frontend types/store/page/Preview fixture exact-match 审查
- [x] **T-69** owner/status/expiry、SAVED eligibility、SEALED immutability、Preview isolation、media-only 空正文全链路回归
- [x] **T-70** C1–C9 Agent focused/full regression，确认无新增 provider 调用、无 snapshot 静默刷新
- [x] **T-71** `git diff --check`、path allowlist、package/lockfile 零变化与增量 credential/privacy scan
- [x] **T-72** OpenSpec 文件级校验：4 specs、28 Requirements、98 Scenarios、delta/implementation exact-match、ACTIVE_TASK links；CLI 缺失记 SKIPPED
- [x] **T-73** 更新 `.ai/ACTIVE_TASK.md` Current Progress 与 append-only `.ai/AGENT_LOG.md`

## 阶段 9：闸门 3 真实依赖验收（仅分别授权后）

> 2026-08-12 人工窄证据：用户确认 Gate 3a 后微信开发者工具“使用正常”。该证据只确认当前页面访问/数据同步恢复，不足以勾选 Gate 3c 或 T-79 的文字、图片、声音、恢复、SAVED 编辑、封存及失败路径完整矩阵。

- [x] **T-74 GATE 3b** 用户授权真实对象存储合成探针与清理范围（2026-08-12）
- [x] **T-75** 合成图片-only：authorize/upload/commit AVAILABLE/save/edit/private read PASS，finally 清理 PASS
- [x] **T-76** 固定合成短 WAV 声音-only：upload/commit AVAILABLE/save/private read/标准音频解码 PASS，finally 清理 PASS；微信扬声器播放仍属 Gate 3c
- [x] **T-77** pending/missing object save FAIL；过期 DRAFT 远端删除成功/不存在/失败重试语义 PASS
- [x] **T-78 GATE 3c** 用户授权微信开发者工具 / 真机文字、图片、声音、恢复、保存后封存验证（2026-08-12）
- [ ] **T-79** 微信端三种独立保存、返回恢复、SAVED 编辑、保存后交给时间、权限拒绝/上传失败路径逐项记录
- [x] **T-80** E0 用户理解仍记 SKIPPED；真机功能 PASS 不得冒充目标用户访谈 PASS
- [x] **T-81** P3.1 真实 AI provider 调用保持 0；本 change 不申请 provider 外调预算

## 阶段 10：验收与收口

- [ ] **T-82** 用户审查实现 diff、真实/跳过证据与 remaining risks
- [ ] **T-83** 仅在用户明确验收后接受四份 delta 进 baseline；普通 bugfix 不重写范围
- [ ] **T-84** 写 `closeout.md`，将 E0 `INCONCLUSIVE`、MySQL/对象存储/真机证据边界如实保留
- [ ] **T-85** 归档 change，`ACTIVE_TASK` 回到 IDLE，追加 AGENT_LOG；不改写历史日志
- [ ] **T-86** 按提交责任 commit；push/deploy/release 仍须另行授权

## 范围守护自检

- [x] 没有实现 P3.2 的导出、任意状态删除、清除全部、账号注销或完整数据删除编排
- [x] 没有修改 Agent Prompt、护栏阈值、memory、provider、reflection、调用预算或新增 AI 能力
- [x] 没有新增 STT、声音分析、自动分类、自动标签、评分、诊断、dashboard、推送或设置页
- [x] 没有修改三个一级 Tab、canonical naming、Preview 隔离或 SEALED/UNLOCKED 不变性
- [x] 没有把 E0 内部走查、H2、build 或真机功能测试冒充目标用户理解证据
- [x] 没有记录用户日记、声音内容、图片内容、storage key、signed URL、secret、prompt/provider response
- [x] 没有修改 archive、冻结蓝图、package/lockfile、deployment、monitoring 或无关代码
