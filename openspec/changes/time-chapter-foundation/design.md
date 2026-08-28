# Design：Time Chapter Foundation（P5.x）

## 1. Context And Invariants

时间篇章是记录之外的 owner-managed 容器。记录仍是最小完整单位；加入、移出、转移、结束、重开或删除篇章都不得修改记录正文、标题、类型、状态、位置、附件、封面、标签或时间字段。

强制不变量：

1. DRAFT 永不成为成员；SAVED/SEALED/UNLOCKED 均可成为成员。
2. 第一版一条记录最多一个主篇章，数据库唯一约束为最终权威。
3. 创建时至少一条成员；后续因删除/转移形成的空篇章保留。
4. 已结束篇章不可加入成员；必须先重新打开。
5. 转移必须携带用户已看到的原篇章 ID，过期确认不得静默覆盖。
6. 批量成员命令全部成功或全部回滚，重复提交不产生重复关系。
7. 覆盖时间来自成员记录 `created_at` 的 MIN/MAX，只称“片段覆盖时间”。
8. Preview mutation、cross-owner 请求和无法判断真实状态的冲突全部 fail-closed。

## 2. Architecture

```text
Mini Program
  ├─ 我的记录：记录 / 篇章
  ├─ 记录详情：加入 / 移出 / 转移
  └─ 篇章详情：编辑 / 排序 / 结束 / 重开 / 删除
          │ authenticated JSON API
          ▼
TimeChapterController
  └─ TimeChapterService（owner + transaction + state authority）
       ├─ TimeChapterMapper
       ├─ TimeChapterRecordMapper
       ├─ RecordMapper（owner/status/createdAt lock read）
       └─ DataExport / DataDeletion integration
          │
          ▼
MySQL: time_chapter + time_chapter_record
```

不把 `chapterId` 加进 `record` 表。关系拥有独立生命周期，避免篇章管理成为 SEALED/UNLOCKED 的内容编辑旁路，也为未来多篇章留下可迁移空间而不在第一版开放。

## 3. Data Model

### 3.1 `time_chapter`

| 字段 | 规划契约 |
|---|---|
| `id` | BIGINT PK AUTO_INCREMENT |
| `user_id` | BIGINT NOT NULL，FK user ON DELETE CASCADE |
| `name` | VARCHAR(100) NOT NULL，trim 后 1–100 字符，不唯一 |
| `note` | VARCHAR(1000) NULL，trim 后 0–1000 字符 |
| `status` | VARCHAR(20) NOT NULL：`ACTIVE` / `ENDED` |
| `ended_at` | DATETIME NULL；ACTIVE 必须为 NULL，ENDED 必须非 NULL |
| `version` | BIGINT NOT NULL DEFAULT 0，乐观并发版本 |
| `created_at` | DATETIME NOT NULL |
| `updated_at` | DATETIME NOT NULL |

索引：`(user_id,status,updated_at,id)`、`(user_id,created_at,id)`。不存 cover、summary、progress、result、subjective start/end。

### 3.2 `time_chapter_record`

| 字段 | 规划契约 |
|---|---|
| `chapter_id` | BIGINT NOT NULL，FK time_chapter ON DELETE CASCADE |
| `record_id` | BIGINT NOT NULL，FK record ON DELETE CASCADE，UNIQUE |
| `user_id` | BIGINT NOT NULL，用于 owner 一致性与索引 |
| `added_at` | DATETIME NOT NULL |

主键 `(chapter_id,record_id)`，唯一键 `(record_id)` 保证第一版零或一个主篇章，索引 `(user_id,chapter_id,added_at)`。service 在写入前同时锁定 chapter 与 record，并验证二者 owner 相同；数据库约束防止绕过 service。

### 3.3 派生字段

`memberCount`、`coverageStartAt`、`coverageEndAt` 查询时从关系与 `record.created_at` 聚合，不冗余持久化。空篇章返回 count=0、coverage=null。排序稳定键为 `record.created_at, record.id`。

## 4. API And DTO Contract

以下是 Gate 1 推荐精确契约；用户批准规划后冻结。所有路径继续使用既有 authenticated `ApiResponse` 外壳。

| Method / Path | 语义 |
|---|---|
| `GET /api/time-chapters` | owner 分页；可选 `status=ACTIVE|ENDED`，进行中优先 |
| `POST /api/time-chapters` | 从至少一条完整记录创建 |
| `GET /api/time-chapters/{id}` | owner 详情；成员按 `order=DESC|ASC` 分页 |
| `PUT /api/time-chapters/{id}` | 修改 name/note，要求 `expectedVersion` |
| `POST /api/time-chapters/{id}/members` | 批量加入或显式转移，要求 `expectedVersion` |
| `POST /api/time-chapters/{id}/members/remove` | 批量移出，要求 `expectedVersion` |
| `POST /api/time-chapters/{id}/end` | ACTIVE→ENDED，要求 `expectedVersion` |
| `POST /api/time-chapters/{id}/reopen` | ENDED→ACTIVE，要求 `expectedVersion` |
| `POST /api/time-chapters/{id}/delete` | 删除容器/关系，要求 `expectedVersion` |

请求基线：

- `CreateTimeChapterRequest`：`name`、`note?`、`recordIds`（1–100，去重后仍至少 1）、`transfers[]`；
- `UpdateTimeChapterRequest`：`name`、`note?`、`expectedVersion`；
- `ChangeChapterMembersRequest`：`recordIds`（1–100）、`transfers[]`、`expectedVersion`；
- `TransferConfirmation`：`recordId`、`fromChapterId`；
- 生命周期/删除命令：`expectedVersion`。

响应基线：

- `TimeChapterSummaryVO`：id/name/note/status/memberCount/coverageStartAt/coverageEndAt/endedAt/version/createdAt/updatedAt；
- `TimeChapterDetailVO`：summary + 分页 members；member 复用记录列表安全字段并包含当前 chapter summary；
- `RecordListItemVO` 与 `RecordDetailVO` 增加 nullable `chapter` summary，只包含 id/name/status，不进入正文或导出正文。

状态或归属已变化时使用既有 conflict/error 外壳返回稳定错误类别，并要求客户端刷新最新详情。错误、日志和 trace 不回显 name、note、记录正文或成员标题。

## 5. Command Semantics

### 5.1 创建

1. 规范化 name/note、去重 recordIds；
2. 按 record ID 升序锁定全部记录并验证 owner、非 DRAFT、未处于 P3.2 删除冻结；
3. 对已有归属逐条核对 `transfers` 的 `fromChapterId`；缺失或不匹配则整体拒绝；
4. 创建 ACTIVE chapter，再原子插入/转移全部关系；
5. 任一冲突回滚，禁止留下空的新篇章。

### 5.2 加入与转移

- 目标必须 ACTIVE；ENDED 返回需重新打开的稳定错误。
- 无归属记录直接加入；已在目标内视为幂等；已在其他篇章必须有准确 transfer confirmation。
- 事务锁按 chapter ID、record ID 升序取得；`expectedVersion` 或 source 不匹配时全部回滚并提示刷新。
- 成功后只改变关系和 chapter version/updatedAt，不更新 record 行。

### 5.3 移出

- ACTIVE/ENDED 均允许移出；不在目标篇章内视为幂等。
- 移出最后成员时保留空篇章，coverage 变为 null。
- 不更新 record 的内容、状态或 updatedAt。

### 5.4 结束与重开

- end：ACTIVE→ENDED，写 injected Clock 的 endedAt；重复 end 返回当前状态而不生成历史事件。
- reopen：ENDED→ACTIVE，清空 endedAt；重复 reopen 返回当前状态。
- 名称/自述在两种状态均可修改。

### 5.5 删除

前端先展示详情中的真实 memberCount 和固定确认语义。backend 在 owner/version 校验后删除 chapter；FK 只清关系，不删除 record。进行中与已结束均可删除。

## 6. Record And Data Ownership Integration

- P3.2 删除单条记录时，关系由 FK CASCADE 删除；篇章保留并实时聚合剩余成员。
- clear-all 删除 owner 记录时，同时删除 owner 的全部篇章；完成后不得留下跨 owner 关系。
- 导出包增加 `chapters/index.json` 与可读 `chapters/README.md`：包含 chapter ID、name、note、status、createdAt、endedAt、明确标注的 coverage 和 member record IDs；不复制正文、位置、媒体或 Agent 内容。
- `RESPECT_SEAL` / `FULL_CONTENT` 只影响既有 record 内容导出，不影响篇章元数据；SEALED 关系不会提前暴露正文。

## 7. Frontend Information Architecture

```text
三个一级 Tab 保持不变
  └─ 我的记录
      ├─ 记录
      │   ├─ 普通浏览/筛选
      │   └─ 多选 → 组成篇章
      └─ 篇章
          ├─ 进行中
          └─ 已结束

记录详情 → 次级：加入 / 移出 / 转移篇章
篇章详情 → 编辑名称/自述、正倒序、结束/重开、删除
```

保存记录成功后不弹加入提示、不推荐篇章。转移确认必须显示原篇章和目标篇章名称；取消保持原状态。篇章详情不显示封面、进度、结果、AI 摘要或主观起止日期。

## 8. Preview, Privacy And Agent

- Preview fixture 可有固定合成篇章及合成成员；所有 mutation 在 service 调用前 fail-closed，真实 chapter request count=0。
- backend 日志仅记录 operation、结构化 ID、状态、数量、结果、冲突类别和耗时；不记录 name、note、标题、正文、位置、媒体 key/URL。
- Agent runtime、prompt、memory source、tool registry 与 safety policy 零修改；篇章不进入 Agent context。

## 9. Verification Strategy

### 9.1 Offline / H2

- schema/FK/unique/index/domain/mapper exact-match；
- owner isolation、DRAFT rejection、同名、非空创建、被动空篇章；
- add/remove/transfer 的原子性、幂等、source confirmation、version conflict 和锁顺序；
- ACTIVE/ENDED、endedAt、编辑、重新打开；
- coverage MIN/MAX、createdAt+id 正倒序与分页稳定性；
- 删除 chapter 不删 record、删除 record 不删 chapter、clear-all 和 export 集成；
- Preview zero request、日志隐私与 Agent zero change。

### 9.2 Gate 3

- 3a MySQL：迁移两次、schema exact-match、合成 owner/record/chapter 创建/转移/删除/clear-all/export，finally 清理为 0；
- 3b 微信开发者工具：Standard 的记录/篇章切换、创建、加入、转移、结束、重开、删除；Preview 固定展示且 mutation request=0；
- 物理真机无设备时 `SKIPPED`；不得以开发者工具替代。

## 10. 决策记录

### 决策 1：没有真实用户时是否进入 P5.x

1. **面临的选择**：继续无限等待；把内部走查伪装成正证据；由产品负责人显式豁免并保留证据缺口。
2. **选了哪个 + 为什么**：选择显式豁免。用户明确要求先完成产品，同时仍能诚实保留 E1 `INCONCLUSIVE`。
3. **放弃的代价**：继续等待会阻断产品形成；伪造证据会破坏事实边界和未来复盘。

### 决策 2：篇章关系存独立表而不是 record.chapter_id

1. **面临的选择**：直接给 record 加 chapter_id；独立关系表且第一版唯一 record_id；第一版直接多对多。
2. **选了哪个 + 为什么**：独立关系表 + 唯一 record_id，既保持外部关系语义，也让未来多篇章可通过迁移放开唯一键。
3. **放弃的代价**：直接字段耦合记录模型并让封存边界模糊；立即多对多扩大 UI、并发和删除复杂度。

### 决策 3：篇章状态只使用 ACTIVE / ENDED

1. **面临的选择**：无状态；ACTIVE/ENDED；增加 ARCHIVED/DELETED/PAUSED 历史状态。
2. **选了哪个 + 为什么**：只用 ACTIVE/ENDED，与“暂时告一段落、可重开”的用户语义完全对应。
3. **放弃的代价**：无状态无法约束结束后加入；更多状态会把生活容器变成工作流管理。

### 决策 4：重开清空当前 endedAt，不建立生命周期事件表

1. **面临的选择**：保留全部结束历史；只保留当前结束时间；复制新篇章。
2. **选了哪个 + 为什么**：只保留当前结束时间，符合第一版明确边界并减少错误叙事。
3. **放弃的代价**：事件历史增加模型/UI；复制会破坏“重新打开原篇章”。

### 决策 5：覆盖时间查询聚合而不冗余存储

1. **面临的选择**：每次查询 MIN/MAX；在 chapter 冗余维护；让用户填写主观日期。
2. **选了哪个 + 为什么**：查询聚合，成员变化后天然正确且不冒充生活起止。
3. **放弃的代价**：冗余字段需要复杂一致性维护；主观日期违反第一版范围。

### 决策 6：转移采用显式 source confirmation

1. **面临的选择**：静默移动；只靠前端确认布尔值；提交 recordId + fromChapterId 并由事务复核。
2. **选了哪个 + 为什么**：提交准确来源并复核，既能显示真实转移，又能阻止陈旧页面覆盖新归属。
3. **放弃的代价**：静默移动伤害信任；单一布尔值不能证明用户确认的是哪个来源。

### 决策 7：批量成员命令采用事务全有或全无

1. **面临的选择**：逐条部分成功；后台异步任务；同步事务原子提交。
2. **选了哪个 + 为什么**：同步事务，首版最多 100 条且用户需要明确结果。
3. **放弃的代价**：部分成功难以恢复和解释；异步任务增加状态机而无必要证据。

### 决策 8：使用 version 与稳定锁顺序处理并发

1. **面临的选择**：最后写入覆盖；仅数据库唯一键报错；乐观 version + 行锁 + 刷新冲突。
2. **选了哪个 + 为什么**：version 与稳定锁顺序，避免静默覆盖并保留数据库最终约束。
3. **放弃的代价**：最后写入会丢用户操作；只返回唯一键异常无法提供可恢复语义。

### 决策 9：删除篇章直接删除容器而不是软删除

1. **面临的选择**：软删除；新增删除 operation；owner/version 校验后的事务硬删除。
2. **选了哪个 + 为什么**：事务硬删除，记录完整保留且没有远端对象，符合明确确认后的简单语义。
3. **放弃的代价**：软删除引入恢复与隐藏状态；operation 对无外部副作用的容器过重。

### 决策 10：把篇章纳入既有导出与 clear-all

1. **面临的选择**：首版忽略；复制每条正文进篇章；只导出元数据与成员 ID 关系。
2. **选了哪个 + 为什么**：导出元数据/关系并接入 clear-all，兑现数据所有权且不复制敏感正文。
3. **放弃的代价**：忽略会造成不完整副本；复制正文会扩大重复与泄露面。

### 决策 11：不让 Agent 参与篇章

1. **面临的选择**：AI 自动归入/命名；Agent 只读篇章；Agent 完全解耦。
2. **选了哪个 + 为什么**：完全解耦，篇章是用户当前解释，不是系统推断的人生标签。
3. **放弃的代价**：自动化制造分类压力；只读也会把未经授权的容器语义带入对话。

### 决策 12：局部扩展“我的记录”而不重建导航

1. **面临的选择**：新增第四个一级 Tab；重建整个记录信息架构；在“我的记录”增加二级切换和独立详情。
2. **选了哪个 + 为什么**：局部扩展，保持三个一级 Tab 和既有产品命名，同时给篇章足够独立空间。
3. **放弃的代价**：第四 Tab 违反硬规则；整体重建超出范围并增加回归风险。
