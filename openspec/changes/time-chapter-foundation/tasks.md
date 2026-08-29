# Tasks：Time Chapter Foundation（P5.x）

> 当前阶段：Gate 1 `APPROVED`（2026-08-28）；Gate 2 `AUTHORIZED`（2026-08-28）。Gate 3、delta acceptance、归档与发布仍未授权。

## 阶段 0：Readiness 与规划闸

- [x] **T-01** 读取 AGENTS、ACTIVE_TASK、Type C checklist、冻结蓝图、E1 决策/结果与 accepted specs
- [x] **T-02** 确认 `ACTIVE_TASK=IDLE`、工作树 clean、同名 change 不存在，记录开工锚点 `ed98298`
- [x] **T-03** 登记 E1 仍为 `INCONCLUSIVE`，以及产品负责人对正证据进入条件的显式豁免
- [x] **T-04** 核对当前 record schema/API、我的记录、详情、Preview 与 P3.2 数据所有权集成点
- [x] **T-05** 完成 confirmed/partial/planned/unknown/out_of_scope 五态
- [x] **T-06** 创建 proposal、design、tasks 与 backend-core/miniapp-core/v2-product-scope 三份 delta
- [x] **T-07** 冻结推荐 API、DTO、数据模型、并发、删除、导出、Preview 与 Agent 边界供 Gate 1 审核
- [x] **T-08** 规划期真实 provider/MySQL/微信/对象存储调用预算 0
- [x] **T-09** OpenSpec CLI 不在 PATH，记录 CLI validation `SKIPPED` 并采用文件级 scaffold
- [x] **T-10** 更新 ACTIVE_TASK 指向本 change，初始化 Current Progress
- [x] **T-11** 文件级检查 artifacts、Requirement/Scenario、链接、路径范围与隐私边界
- [x] **T-12 GATE 1** 用户于 2026-08-28 批准 proposal/design/tasks、三份 delta、决策 1–12，以及名称 100 字、自述 1000 字、单批 100 条、expectedVersion、fromChapterId 显式转移等推荐契约

## 阶段 1：实现授权与 baseline

- [x] **T-13 GATE 2** 用户于 2026-08-28 明确授权按本 tasks 修改业务代码；Gate 3、真实 MySQL、微信开发者工具、物理真机、delta acceptance、归档、push、部署与发布仍未授权
- [x] **T-14** 已登记实现 allowlist/denylist，更新 ACTIVE_TASK 与 AGENT_LOG；不扩大 Gate 3
- [x] **T-15** backend baseline：focused 6 suites / 82 tests / 0 failures / 0 errors / 0 skipped；full 108 suites / 782 tests / 0 failures / 0 errors / 15 skipped（本地 H2/default-gated tests）
- [x] **T-16** frontend baseline：bundled Node 运行 `vue-tsc --noEmit`、Standard `mp-weixin` build、Preview `mp-weixin` build 均 PASS
- [x] **T-17** 已确认 package/lockfile、deployment、monitoring、admin、Agent runtime/prompt/tool/memory 不在本 change 实现范围

实现 allowlist：P5.x MySQL/H2 schema；time chapter domain/mapper/service/controller/DTO/VO/tests；record query VO 的 nullable chapter summary；P3.2 export/delete integration；frontend chapter types/service/store/pages/components、record-list/detail 局部入口、pages.json；本 change、ACTIVE_TASK、append-only AGENT_LOG。

## 阶段 2：Schema 与 domain（TDD）

- [x] **T-18 RED** H2/MySQL schema contract：chapter 字段、status、version、FK、unique record membership 与索引
- [x] **T-19 GREEN** 新增幂等 P5.x MySQL 增量脚本并同步 schema.mysql.sql/H2 schema（真实 migration 未执行）
- [x] **T-20 RED/GREEN** `TimeChapterStatus`、domain、mapper 与 enum type handler exact-match
- [x] **T-21 RED/GREEN** owner scope、同名允许、name/note 规范化与长度、ACTIVE/endedAt 不变量
- [x] **T-22 RED/GREEN** relation owner 一致、DRAFT 拒绝、record_id 唯一主归属与 FK cascade
- [x] **T-23** 使用 injected Clock；时间测试不得依赖系统当前时间

## 阶段 3：Backend 查询与命令（TDD）

- [x] **T-24 RED/GREEN** owner chapter 分页、ACTIVE/ENDED 分组排序、空态与 cross-owner safe-not-found
- [x] **T-25 RED/GREEN** detail 成员分页、createdAt+id 正倒序、memberCount 与 coverage MIN/MAX
- [x] **T-26 RED/GREEN** 从 1–100 条 SAVED/SEALED/UNLOCKED 创建；DRAFT/空选择整体拒绝
- [x] **T-27 RED/GREEN** 修改 name/note；ACTIVE/ENDED 均允许，expectedVersion 冲突拒绝
- [x] **T-28 RED/GREEN** end/reopen 幂等、endedAt/current status、version 与错误语义
- [x] **T-29 RED/GREEN** 批量加入：无归属加入、目标内幂等、ENDED 拒绝
- [x] **T-30 RED/GREEN** 显式转移：准确 fromChapterId、原/目标 owner、陈旧确认拒绝
- [x] **T-31 RED/GREEN** 批量移出：两状态均允许、不在目标幂等、被动空篇章保留
- [x] **T-32 RED/GREEN** 全有或全无事务、确定性 ID 锁顺序、unique/version 并发冲突与回滚（真实 InnoDB race 留 Gate 3）
- [x] **T-33 RED/GREEN** 删除容器/关系而保留所有 record；真实 memberCount/version 校验
- [x] **T-34** controller/DTO/VO/API exact-match，错误使用稳定类别且不泄露用户内容
- [x] **T-35** RecordListItemVO/RecordDetailVO 增加 nullable chapter summary，不改变正文与 record updatedAt

## 阶段 4：数据所有权与生命周期集成

- [x] **T-36 RED/GREEN** 删除单条 record 级联关系，篇章保留并实时重算 count/coverage
- [x] **T-37 RED/GREEN** clear-all 覆盖 owner chapter/relations，不留下 cross-owner/orphan
- [x] **T-38 RED/GREEN** 导出增加 chapters/index.json 与 README，只有元数据/member IDs，不复制正文
- [x] **T-39 RED/GREEN** RESPECT_SEAL/FULL_CONTENT 不改变 chapter 元数据且不提前泄露 SEALED 正文
- [x] **T-40** 日志只记录结构化 ID/status/count/result/conflict/duration，不记录 name/note/title/content/location/key/URL

## 阶段 5：Mini Program 真实体验

- [x] **T-41** 新增 chapter types/service/store，所有 mutation 在 Preview 入口 fail-closed
- [x] **T-42** “我的记录”加入“记录 / 篇章”二级切换，不新增一级 Tab
- [x] **T-43** 记录列表多选组成篇章；保存成功后无自动提示、推荐或归入
- [x] **T-44** 创建表单只含必填名称、可选自述；至少一条记录；同名允许
- [x] **T-45** 篇章列表按进行中/已结束分组，显示数量与“片段覆盖时间”
- [x] **T-46** 独立篇章详情展示成员、正/倒序、编辑名称/自述、结束/重开
- [x] **T-47** 记录详情提供次级加入/移出/转移；SEALED/UNLOCKED 不出现内容编辑旁路
- [x] **T-48** 转移确认显示原篇章/目标篇章；取消不变，冲突刷新最新真实状态
- [x] **T-49** 删除确认显示成员数量和“只删除篇章，不删除记录”；完成后记录仍可浏览
- [x] **T-50** 被动空篇章、加载/空/失败/重试状态，不显示假成功
- [x] **T-51** Preview 固定合成篇章可浏览；所有 mutation 代码级 request count=0（真实工具计数留 Gate 3b）
- [x] **T-52** 保持三个一级 Tab、canonical naming 与既有页面气质，不做 major visual reconstruction

## 阶段 6：自动化、范围与隐私验证

- [x] **T-53** backend focused tests PASS；审查修复后 full Maven：112 suites / 800 tests / 0 failures / 0 errors / 15 skipped（本地 H2/default-gated tests）
- [x] **T-54** frontend type-check、Standard/Preview build PASS
- [x] **T-55** schema/domain/mapper/DTO/VO/frontend types/API exact-match
- [x] **T-56** owner/status/create/edit/lifecycle/member/transfer/delete/export 全链路回归
- [x] **T-57** 离线覆盖重复请求、陈旧 version/source 与数据库 unique 边界；真实 InnoDB unique race/死锁重试保持 Gate 3 SKIPPED
- [x] **T-58** C1–R1 回归；Agent runtime/tool/prompt/memory/provider 行为零变化
- [x] **T-59** Preview 代码级 request=0、privacy/credential scan、package/lockfile/denylist 零变化（真实工具计数留 Gate 3b）
- [x] **T-60** OpenSpec 文件级 artifact/delta/Requirement/Scenario 校验；CLI 缺失保持 SKIPPED
- [x] **T-61** 更新 ACTIVE_TASK Current Progress 与 append-only AGENT_LOG
- [x] **T-61a 审查修复** relation 使用复合 owner FK；transfer 按全部 chapter ID 升序后再锁 record；数据库并发异常返回稳定 409；补 cross-owner/锁顺序/冲突测试
- [x] **T-61b 审查修复** Preview session/token 互斥且 chapter service 只读优先；篇章/成员支持续载；ENDED 详情显示 endedAt；陈旧 source 冲突刷新

## 阶段 7：Gate 3 真实依赖验收（分别授权）

- [x] **T-62 GATE 3a** 用户授权真实 MySQL preflight/migration/合成 chapter 探针（2026-08-29 已授权）
- [x] **T-63** MySQL migration 连续两次、schema exact-match、owner/unique/FK/index 与 rollback PASS
- [x] **T-64** 合成创建/加入/转移/结束/重开/删除 record/chapter/clear-all/export PASS，finally 数据为 0
- [x] **T-65 GATE 3b** 用户授权微信开发者工具 Standard/Preview 验收（2026-08-29 已授权）
- [x] **T-66** Standard 完整交互矩阵 PASS；Preview 固定展示且所有 mutation request=0
- [x] **T-67** 物理真机有设备才执行，否则 `SKIPPED`；本轮没有已连接的可控设备，明确记 `SKIPPED`，开发者工具不得冒充真机
- [x] **T-68** Agent provider、对象存储、外部研究调用保持 0

## 阶段 8：验收、接受与归档

- [ ] **T-69** 用户审查实现 diff、自动化/真实/跳过证据与 remaining risks
- [ ] **T-70** 用户明确接受后，将三份 delta exact-copy 合入 accepted baseline
- [ ] **T-71** 写 closeout，区分产品负责人豁免、工程 PASS、真实依赖与用户价值未知
- [ ] **T-72** 用户授权后归档 change，ACTIVE_TASK→IDLE，追加 AGENT_LOG
- [x] **T-73** 已完成本地提交 `f4a088f feat(time-chapter): 完成P5时间篇章基础`；未 push/PR/deploy/release

## 范围守护自检

- [x] 没有多篇章、子篇章、自动归入、AI 命名/摘要/封面、目标/进度/结果/评分
- [x] 没有修改 record 正文/位置/附件/封面/时间或 SEALED/UNLOCKED 不可变规则
- [x] 没有新增第四个一级 Tab或改 canonical naming
- [x] 没有修改 Agent runtime/tool/prompt/memory/safety/provider
- [x] 没有 package/lockfile、deployment、monitoring、admin 或生产发布改动
- [x] 没有把产品负责人豁免、自动化或开发者工具 PASS 写成目标用户正证据
