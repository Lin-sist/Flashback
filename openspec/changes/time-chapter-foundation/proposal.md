# Proposal：Time Chapter Foundation（P5.x）

## 1. Summary

P5.x 为 Flashback 增加最小、用户定义的“时间篇章”：片段仍先独立成立，用户在保存之后主动把一条或多条完整记录组成篇章；篇章可以命名、写可选自述、结束、重新打开、浏览和删除，但不评价进度、结果或成败。

本 change 只建立规划工件。规划批准前不修改业务代码；规划批准也不自动授权实现、真实 MySQL、微信开发者工具、delta acceptance、归档、push、部署或发布。

## 2. Why Now

- P3.1/P3.2 已建立独立片段、记录状态与数据所有权，P4.1/P4.2/R1 已完成 Agent 信任边界；当前 `ACTIVE_TASK=IDLE`。
- E1 已完成产品负责人盘问、A/B/C throwaway 原型和内部可操作性走查，但真实参与者为 0，研究结论诚实保持 `INCONCLUSIVE`。
- 产品负责人于 2026-08-28 明确声明没有可用真实用户，决定豁免“先有正证据”进入条件，先完成最小产品，再以未来真实使用优化。
- 当前 backend、frontend 和 accepted specs 均没有时间篇章实体、API 或真实页面，不能通过局部前端状态冒充完成。

该豁免是产品治理决定，不是用户研究结果。P5.x 的验收只能证明契约、实现和真实依赖可用，不能宣称目标用户价值已经验证。

## 3. User Story

> 改前，用户留下的片段只能逐条浏览或借助标签、时间范围寻找；当用户自己意识到“这些时刻属于同一段生活”时，没有一个由自己命名、可结束、可重新打开且不评价成败的容器。
>
> 改后，片段仍先独立保存。用户可从已有完整记录主动组成篇章，在“我的记录”中浏览“记录 / 篇章”，管理归属与生命周期；删除篇章只删除容器，不删除任何记录。

## 4. Goals

1. 建立 owner-scoped 时间篇章及成员关系，第一版每条记录属于零个或一个主篇章。
2. 只允许 SAVED、SEALED、UNLOCKED 成为成员；技术 DRAFT 不可加入、创建或计数。
3. 创建必须至少包含一条完整记录；后续被动空篇章保留，不自动删除。
4. 支持必填名称、可选用户自述、同名篇章，以及进行中/已结束生命周期。
5. 已结束篇章必须重新打开后才能加入记录；结束不改变成员记录状态或可编辑性。
6. 支持主动加入、移出和显式确认转移；批量操作事务原子、重试幂等、冲突不静默覆盖。
7. 使用成员记录 `createdAt` 计算并明确标注“片段覆盖时间”，不冒充生活真实起止。
8. 在“我的记录”内增加“记录 / 篇章”二级切换，不新增第四个一级 Tab。
9. 篇章删除前显示成员数量与“只删除篇章，不删除记录”；删除后记录完整保留。
10. 将篇章元数据和关系纳入既有全量导出/清除全部语义，但不复制记录正文。
11. Preview 只展示固定合成篇章，所有 mutation fail-closed；Agent 与篇章完全解耦。

## 5. Non-goals

- 一条记录属于多个篇章、子篇章、篇章合并或拆分；
- 空篇章预创建、自动归入、保存成功后推荐、AI 命名/摘要/封面；
- 目标、进度、完成度、结果、成功/失败、提醒、阶段预测或人生诊断；
- 主观生活起止日期、拖拽编排、自定义时间线、多次结束—重开历史；
- 分享、协作、公开页面、订阅、商业化或第四个一级 Tab；
- 修改记录正文、位置、附件、封面、时间、封存或解锁规则；
- 新增 Agent tool、memory、prompt、provider 调用或分析 dashboard；
- deployment、monitoring、admin portal、SMS、生产发布或大规模 backend/frontend 重写；
- package/lockfile 变更。

## 6. Current Capability Classification

### confirmed

- 记录已有 DRAFT/SAVED/SEALED/UNLOCKED 状态、owner scope、createdAt、分页列表、详情和数据所有权操作。
- SAVED 可编辑；SEALED/UNLOCKED 正文、位置、附件、封面保持既有不可变边界。
- “我的记录”真实 authenticated path 与 Preview 已隔离，Preview mutation 可 fail-closed。
- 全量导出、单条删除、清除全部和关联数据清理已有 P3.2 基础。

### partial

- “我的记录”已有状态筛选、搜索和列表，但没有记录多选、二级“篇章”切换或篇章详情。
- 记录详情已有多种次级操作，但没有篇章归属入口。
- 数据导出已有记录与 Agent 分区，但没有篇章元数据和关系文件。

### planned

- `time_chapter` / `time_chapter_record` schema、domain、mapper、service、controller 与 DTO/VO；
- owner-scoped 查询、创建、编辑、结束、重开、删除、批量加入/移出/转移；
- “我的记录”二级切换、批量组成篇章、篇章列表/详情与单条记录归属入口；
- 导出、删除记录、清除全部、Preview 和隐私日志集成；
- H2、真实 MySQL、Standard/Preview 微信开发者工具验收矩阵。

### unknown

- 真实 MySQL 当前 schema 与新增唯一键/外键在历史数据下的迁移结果；
- 微信端多选、转移确认和长篇章列表在真实交互中的最终可用性；
- 没有真实用户证据，篇章是否产生价值、分类压力和长期留存增益仍未知；
- 生产规模下的最大成员数量与分页性能，没有证据时不声明 SLA。

### out_of_scope

- 多篇章、自动化/AI、协作分享、生命周期历史、生产发布与用户价值结论。

## 7. Proposed Scope

### 7.1 Backend

- 新增时间篇章与唯一主归属关系；全部 query/mutation 以 authenticated owner 为边界。
- 用事务与行锁实现创建、批量加入、移出、转移、结束、重开和删除；冲突返回最新真实状态，不部分成功。
- 列表/详情返回状态、成员数、片段覆盖时间、当前结束时间和成员分页，不复制记录正文到篇章表。
- 记录删除依赖关系级联并重算派生统计；篇章删除只清容器/关系；清除全部覆盖篇章。
- 既有数据导出增加篇章元数据和成员 ID 关系，记录正文仍只存在 records 分区。

### 7.2 Mini Program

- “我的记录”内增加“记录 / 篇章”二级切换；记录页支持多选“组成篇章”。
- 新增篇章列表与独立详情页；按记录原有时间正序/倒序展示成员。
- 记录详情提供次级加入、移出或转移入口，不干扰保存和回看主路径。
- 明确展示进行中/已结束、片段覆盖时间、转移来源/目标和删除影响数量。
- Preview 可展示固定合成篇章，但创建、编辑、归属、生命周期和删除全部拒绝真实调用。

### 7.3 Product And Agent Boundary

- 篇章归属是用户管理的外部关系，不是记录正文；SEALED/UNLOCKED 可以改变归属，但不能借此修改记录。
- Agent 不读取篇章来推断人生阶段，不命名、不推荐、不自动归入，也不获得篇章工具。
- 标签与篇章独立存在，不互相转换。

## 8. Spec Delta Map

- `backend-core`：owner-scoped 模型、生命周期、成员事务、查询/覆盖时间、删除与数据所有权集成；
- `miniapp-core`：“我的记录”二级结构、主动组成/加入/转移、篇章详情、删除确认与 Preview；
- `v2-product-scope`：时间篇章最小产品范围、E1 豁免证据边界与明确非目标。

## 9. Evidence Plan

- 文件级：artifact、Requirement/Scenario、路径 allowlist、契约映射、隐私与 credential 增量扫描；
- backend/H2：schema、owner、状态、非空创建、唯一归属、原子批量、幂等、并发冲突、覆盖时间、删除/导出集成；
- build：backend focused/full Maven，frontend type-check、Standard/Preview mp-weixin build；
- Gate 3a：真实 MySQL migration 幂等、owner/FK/unique、创建/转移/删除与 P3.2 集成合成探针；
- Gate 3b：微信开发者工具 Standard/Preview 完整交互矩阵；物理真机无条件时记 `SKIPPED`；
- Agent provider、对象存储和外部研究调用预算均为 0。

## 10. Responsibility And Gate State

- Change ID：`time-chapter-foundation`。
- 开工锚点：`ed98298`。
- 提交责任：沿用用户授权的 Agent 本地提交；不 push。
- E1：`INCONCLUSIVE`；2026-08-28 产品负责人显式豁免正证据进入条件，不伪造用户验证。
- OpenSpec CLI：不在 PATH；采用仓库既有文件级 scaffold，CLI validation 记 `SKIPPED`。
- Gate 1：`APPROVED`；用户于 2026-08-28 接受 proposal/design/tasks、三份 delta、决策 1–12 与全部推荐精确契约。
- Gate 2：`NOT AUTHORIZED`；Gate 1 批准后仍须用户明确授权实现。
- Gate 3、delta acceptance、archive、push、PR、deploy、release：均未授权。
