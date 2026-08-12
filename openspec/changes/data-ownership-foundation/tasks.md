# Tasks：Data Ownership Foundation（P3.2）

> 当前阶段：Gate 1 与 Gate 2 已于 2026-08-12 获批，进入离线实现；Gate 3a/3b/3c 均未授权。

## 阶段 0：Readiness 与规划闸

- [x] **T-01** 读取 `AGENTS.md`、`.ai/ACTIVE_TASK.md`、冻结蓝图 v2.0、核心产品定义 v0.1 与 Type C checklist
- [x] **T-02** 确认 P3.1 已归档、`ACTIVE_TASK=IDLE`、工作树 clean、HEAD=`efd2618`
- [x] **T-03** 核对 `openspec/changes/`：M1/M3 为早期历史工件，不是当前 active 指针；同名 P3.2 change 不存在
- [x] **T-04** 核对 accepted backend-core、miniapp-core、v2-product-scope、agent-runtime、agent-collaboration specs
- [x] **T-05** 核对当前 DELETE、record/attachment/location、object storage、Agent cascade、Preview 与用户中心事实
- [x] **T-06** 记录 confirmed / partial / planned / out_of_scope / unknown 五态
- [x] **T-07** 创建 proposal、design、tasks 与五份 delta
- [x] **T-08** 记录规划期外部调用预算 0、真实 provider 预算 0、默认用户手动提交
- [x] **T-09** 尝试 OpenSpec CLI scaffold；CLI 未安装，记 `SKIPPED` 并采用仓库既有文件级结构
- [x] **T-10** 文件级核对 artifacts、Requirement/Scenario、链接、范围与隐私边界
- [x] **T-11 GATE 1** 用户批准 proposal / design / tasks / 五份 delta 与 design 决策 1–12（2026-08-12，“授权实现”）
- [x] **T-12** 用户如修改 exact API、operation enum、24h TTL、sealed policy、clear-all freeze 或确认方式，同步全部 artifacts 后重新请求 Gate 1（N/A：用户按原规划授权）

## 阶段 1：实现授权与 baseline

- [x] **T-13 GATE 2** 用户明确授权按 tasks 修改业务代码（2026-08-12）
- [x] **T-14** 更新 proposal Gate State、ACTIVE_TASK Current Progress 与 AGENT_LOG，记录 Gate 2 但不扩大 Gate 3
- [x] **T-15** 运行 backend focused/full Maven baseline，记录 suites/tests/failures/errors/skipped（full：92 suites / 688 tests / 0 failures / 0 errors / 9 skipped）
- [x] **T-16** 运行 frontend type-check、standard mp-weixin 与 Preview build baseline（全部 PASS）
- [x] **T-17** 建立 P3.2 路径 allowlist，确认 package/lockfile、deployment、monitoring、admin、archive、冻结蓝图不在范围
- [x] **T-18** 对真实 MySQL、对象存储、微信文件交付分别保持 Gate 3a/3b/3c；未授权时测试必须默认 skip

实现 allowlist：

- `backend/sql/mysql/` 的 P3.2 增量脚本与 schema 同步；
- backend data ownership operation/export/deletion、record/attachment/object storage、关联 mapper 与直接测试；
- frontend data ownership service/types/store、用户中心入口、data-backup 页面、record detail/editor 删除入口与直接测试；
- 本 change artifacts、`.ai/ACTIVE_TASK.md`、append-only `.ai/AGENT_LOG.md`。

denylist：package/lockfile、deployment、monitoring、admin、account deletion、真实 provider/Prompt/memory/guardrail/reflection、archive、accepted baseline specs（验收接受 delta 前）、冻结蓝图、无关视觉重构。

## 阶段 2：Operation schema 与契约（TDD）

- [x] **T-19 RED** schema contract：`data_operation`、`data_operation_record`、owner/index/expected-state/TTL 约束当前应失败
- [x] **T-20** 新增 P3.2 MySQL 增量脚本，含只读 preflight、DDL、索引、外键与聚合 postflight
- [x] **T-21** 同步 `schema.mysql.sql`、H2 schema 与 domain/mapper enum
- [x] **T-22 RED** operation type/status、owner isolation、active operation uniqueness、intent expiry、idempotent confirm/retry
- [x] **T-23 GREEN** 实现 operation repository/service 与封闭 failure taxonomy
- [x] **T-24 RED/GREEN** 对 confirmed deletion 目标建立 query/mutation lock；普通 list/timeline/detail 不展示目标，写入 fail-closed
- [x] **T-25** 迁移现有 `DELETE /api/records/{id}`：不再直接数据库删除；frontend 同步前保持明确兼容错误

## 阶段 3：离线导出（TDD）

- [x] **T-26 RED** export API owner scope、策略校验、并发互斥、status/download expiry
- [x] **T-27 GREEN** 实现 summary、export operation、status、download 与 retry API
- [x] **T-28 RED** package exact tree：index.html、records、media、agent、manifest、README
- [x] **T-29 GREEN** 使用 JDK `ZipOutputStream` 与私有临时目录构建原子 artifact，不新增依赖
- [x] **T-30 RED/GREEN** `index.html` 无 CDN/fetch/外部字体脚本，所有链接为包内相对路径
- [x] **T-31 RED/GREEN** records Markdown 区分用户原文、用户字段、AI 派生字段；DRAFT 标注未完成
- [x] **T-32 RED/GREEN** agent Markdown 与 records 物理分区，按角色/时间标注，不导出 prompt/provider response/瞬态工具参数
- [x] **T-33 RED/GREEN** `RESPECT_SEAL` 遮蔽未解锁 SEALED 内容；`FULL_CONTENT` 完整取回但不改 record 状态
- [x] **T-34 RED/GREEN** 媒体按 provider 私有读取，manifest 记录相对路径、bytes、SHA-256、类型与归属
- [x] **T-35 RED/GREEN** partial artifact、失败、24h expiry 与过期清理；日志不含绝对路径、token、key 或内容
- [ ] **T-36** PARTIAL：合成 18 个媒体 / 301,989,888 logical bytes 构包 PASS，artifact 299,156 bytes / 963ms；样本高度可压缩且未取得构建过程磁盘峰值，不声称完成真实媒体边界或生产 SLA

## 阶段 4：任意状态删除与清除全部（TDD）

- [x] **T-37 RED** deletion intent：RECORD/ALL_RECORDS、owner、snapshot count、nonce hash、expiry、confirmation text
- [x] **T-38 GREEN** 实现 prepare/confirm API，confirm 重试不得创建第二次不可逆 operation
- [x] **T-39 RED** DRAFT/SAVED/SEALED/UNLOCKED 单条删除均进入同一 operation；cross-owner/expired intent 拒绝
- [x] **T-40 RED** worker 读取全部 attachment rows，provider success/not-found 后才允许 DB delete
- [x] **T-41 RED** provider failure 保留 record/item 并进入 RETRY_REQUIRED；重试从原 snapshot 继续
- [x] **T-42 GREEN** 实现 per-record worker 与 expected-state 防并发误删
- [x] **T-43 RED/GREEN** 在“远端已删、DB 未删”中断点恢复：not-found 幂等后完成 DB delete
- [x] **T-44 RED/GREEN** record 删除后 location/attachment/tag/reply/reminder/unlock notice 无残留
- [x] **T-45 RED/GREEN** record-linked agent session/message/tool/trace 无残留；发现不可归属派生数据 fail-closed
- [x] **T-46 RED/GREEN** clear-all 按确认时 owner snapshot；确认后 record/Agent mutation 冻结，完成后解除
- [x] **T-47 RED/GREEN** export 与 destructive operation owner-scoped 互斥；retry 不扩大原授权范围
- [x] **T-48** 将编辑器“放弃草稿”和详情页删除统一迁移到 operation；删除旧直接 DB 路径
- [x] **T-49** 清理证据只记录 operation type、计数、failure code、attempt、耗时，不记录 id/content/location/key/URL

## 阶段 5：Mini Program 真实页面

- [x] **T-50** 在 `pages.json` 与个人中心注册“数据与所有权”真实入口，保留三个一级 Tab
- [x] **T-51** 重写未注册 data-backup 页面，移除固定日期/数量、iCloud、自动备份、恢复、PDF/纯文本等假能力
- [x] **T-52** 接入 backend summary、export policy、operation status、download、retry
- [x] **T-53** 默认选择“尊重封存”，完整取回需用户主动选择并看到说明
- [x] **T-54** 记录详情对四种状态提供真实删除入口；SEALED/UNLOCKED 不因删除能力获得编辑旁路
- [x] **T-55** clear-all 展示真实 snapshot count、建议先导出、确认短语与不可恢复说明
- [x] **T-56** operation 进行中展示真实计数；RETRY_REQUIRED 明示仍有数据未清理，不显示成功
- [x] **T-57** clear-all 期间前端禁用新建/编辑/Agent 写入；backend 仍为权威拦截
- [x] **T-58** Preview 展示只读说明，export/delete/confirm/retry/download 全部 fail-closed，真实调用数 0
- [x] **T-59** 验证“我的记录、时光轴、时间回看”命名与现有页面气质，不做 major visual reconstruction

## 阶段 6：自动化、范围与隐私验证

- [x] **T-60** backend focused tests PASS，再运行 full Maven；记录真实数值与 skipped 原因
- [x] **T-61** frontend type-check、standard mp-weixin、Preview build PASS
- [x] **T-62** schema/migration/domain/mapper/DTO/VO/frontend type exact-match
- [x] **T-63** owner/status/intent/operation/mutation-lock/export/delete/cascade 全链路回归
- [x] **T-64** ZIP 解包结构、HTML 离线、Markdown 可读、manifest SHA-256/bytes 自动校验
- [x] **T-65** C1–C9 focused/full regression；Agent tool registry 不新增 export/delete，真实 provider 调用 0
- [x] **T-66** `git diff --check`、path allowlist、package/lockfile 零变化、credential/privacy 增量扫描
- [x] **T-67** OpenSpec 文件级校验：5 specs、Requirement/Scenario、链接、ACTIVE_TASK；CLI 缺失记 SKIPPED
- [x] **T-68** 更新 ACTIVE_TASK Current Progress 与 append-only AGENT_LOG

## 阶段 7：Gate 3 真实依赖验收（分别授权）

- [ ] **T-69 GATE 3a** 用户授权真实 MySQL 只读 preflight、DDL/migration、删除/cascade 与中断恢复探针
- [ ] **T-70** Gate 3a preflight 只输出 schema、外键、状态计数与孤儿聚合，不读取/输出用户原文、位置或 key
- [ ] **T-71** 真实 MySQL migration 幂等；单条与 clear-all 合成聚合删除/恢复 PASS
- [ ] **T-72 GATE 3b** 用户授权私有对象存储合成图片/WAV 的导出、下载、删除、not-found/retry 与 finally cleanup
- [ ] **T-73** 真实对象存储导出 bytes/SHA-256 PASS；删除/中断恢复 PASS；合成对象与 artifact 全部清理
- [ ] **T-74 GATE 3c** 用户授权微信开发者工具/真机文件保存与完整交互矩阵
- [ ] **T-75** 微信端 export 生成并保存/打开或转存真实 ZIP；不能用桌面下载或 build 代替
- [ ] **T-76** 微信端四状态单删、clear-all 强确认、写入冻结、失败重试、Preview 只读逐项记录
- [ ] **T-77** 真实 Agent provider 调用保持 0；本 change 不申请 provider 外调

## 阶段 8：验收、delta acceptance 与归档

- [ ] **T-78** 用户审查实现 diff、真实/跳过证据、导出样包与 remaining risks
- [ ] **T-79** 用户明确验收后，才接受五份 delta 进 baseline
- [ ] **T-80** 写 closeout，如实区分 H2/build、真实 MySQL、对象存储、微信与生产 SLA
- [ ] **T-81** 归档 change，ACTIVE_TASK 回到 IDLE，追加 AGENT_LOG；不改写历史
- [ ] **T-82** 按提交责任处理 commit；push/deploy/release 仍需独立授权

## 范围守护自检

- [x] 没有实现账号注销、备份恢复、iCloud/云同步、订阅付费、PDF-only 或生产备份
- [x] 没有给 Agent 增加 export/delete/clear-all 工具或改变 Prompt/provider/memory/guardrail/reflection
- [x] 没有把 SEALED/UNLOCKED 删除能力变成内容、位置、附件或封面的编辑旁路
- [x] 没有修改三个一级 Tab、canonical naming、package/lockfile、deployment、monitoring、admin 或冻结蓝图
- [x] 没有把 DB cascade、H2、build 或桌面 ZIP 冒充远端对象/微信真实证据
- [x] 没有记录日记原文、媒体内容、位置、storage key、signed URL、download token、secret、prompt/provider response
