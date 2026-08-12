# Present Moment Capture（P3.1）

> Type C 已批准规划。change-id：`present-moment-capture`。
> 开工锚点：`2d9544a`。提交责任：**Agent 提交**（不含 push）。
> 用户于 2026-08-10 批准闸门 1 并授权闸门 2；Gate 3a 真实 MySQL 于 2026-08-12 授权并完成。Gate 3b 对象存储与 Gate 3c 微信真机仍未授权。

## 1. Why Now

H0 已收口 authenticated real path 与 Preview 的能力诚实性。E0 已完成 A / B / C 低成本原型和内部可操作性走查，但因为没有真实目标用户或参与者，最终只能诚实收口为 `INCONCLUSIVE / SKIPPED`；没有变体胜者，也没有用户理解 PASS。

这不改变当前产品契约中的三处确定问题：

1. 用户主动保存后，记录仍是用户可见的 `DRAFT`；
2. backend `CreateRecordRequest` / `UpdateRecordRequest` 与 service 都要求正文非空，frontend 也要求“先写下正文”才能添加图片或声音；
3. 新建记录默认 `FUTURE_LETTER`，分类、人生节点、Agent、AI 与解锁时间在保存前共同占据编辑器主路径。

冻结蓝图已经确认 P3.1 的硬目标是“文字、图片、声音任一种保存后独立成立”。E0 是建议依赖而非硬依赖；产品负责人已接受带着交互证据缺口进入规划，但没有授权把任一 throwaway 变体直接实现。

## 2. Readiness Verdict

**GO（仅规划）**：

- `.ai/ACTIVE_TASK.md` 在开刀前为 `IDLE`；
- H0 已由 `c48b13a` 实现，并由 `a9a3dea` 补录独立验收证据；
- E0 原型由 `9cab113` 提交，观察准备由 `2d9544a` 提交，结果现已如实标为 `INCONCLUSIVE / SKIPPED`；
- 开刀前工作树 clean，HEAD 为 `2d9544a`，本地 `main` 比 `origin/main` 超前 4 个提交，未 push；
- `openspec/changes/present-moment-capture/` 在开刀前不存在，当前没有其他 active Type C；
- 冻结蓝图把 H0 作为 P3.1 硬依赖、E0 作为建议依赖，并已冻结 D37–D44 的最低产品语义；
- `openspec` CLI 不在 PATH，本轮沿用仓库既有 change 结构并做 Requirement / Scenario 文件级校验，不能声称 CLI validate PASS。

## 3. E0 证据边界

### confirmed

- A / B / C 原型可以运行，三种反馈与层级确实不同；
- 安静、无声音、无自动跳转的保存反馈与核心产品气质一致；
- 产品负责人接受“不等待不可获得的参与者，继续进入 P3.1 规划”。

### unknown

- 真实目标用户是否能注意到并理解“这一刻已经留下”；
- toast、持续状态条或底部层哪一种最清楚；
- 标题、分类、人生节点、Agent 与时间入口的最终折叠层级；
- 用户是否会把保存后的次级入口理解为新的完成义务。

### planning consequence

P3.1 只继承蓝图 D43 / D44 的最低基线，不宣称选择 A、B 或 C，不把具体动效与层级写成已验证事实。

## 4. 现状事实（能力五态）

### confirmed

1. backend 与 frontend 的记录状态均为 `DRAFT / SEALED / UNLOCKED`，没有“用户已保存但仍可编辑”的独立状态。
2. backend 与 frontend 的记录类型均为 `FUTURE_LETTER / NODE_RECORD / EMOTION_NOTE`，新编辑器默认 `FUTURE_LETTER`。
3. `POST /api/records` 创建 DRAFT，`PUT /api/records/{id}` 只更新 DRAFT，`POST /api/records/{id}/seal` 直接从 DRAFT 封存。
4. `CreateRecordRequest.content` 与 `UpdateRecordRequest.content` 使用 `@NotBlank`，service 也执行非空正文校验。
5. frontend `ensureDraftForAuxiliaryEdit` 在没有 record id 时要求正文非空，因此图片和声音不能独立建立记录。
6. 图片/声音在 backend 校验为 `AVAILABLE` 后已有真实私有对象存储元数据，可作为保存成立条件。
7. location、attachments、cover、tag、Agent 写作引导与 tool write 当前都以 DRAFT 作为可编辑状态。
8. SEALED / UNLOCKED 的正文、位置、附件与封面不可变，解锁调度与回看链路已经存在。
9. authenticated real path 使用 backend；Preview 已明确只读并与真实 session 隔离。
10. `record.content` 为 `TEXT NOT NULL`，但可以安全保存空字符串；status / record_type 是 VARCHAR，不是数据库 enum。

### partial

1. DRAFT 同时承担“用户认为已保存的记录”和“未完成技术恢复草稿”，职责耦合为 `partial`。
2. 用户可以保存文字记录，但反馈为“草稿已保存”，当下完成语义为 `partial`。
3. 图片与声音上传真实可用，但必须先写正文，独立片段能力为 `partial`。
4. 首页、列表、时光轴可展示记录，但尚不理解 SAVED / MOMENT，能力为 `partial`。
5. 关闭编辑器会自动持久化正文 DRAFT，但没有明确过期、恢复入口或用户不可见边界，恢复能力为 `partial`。

### planned

1. 新增 canonical backend/frontend 状态 `SAVED` 与类型 `MOMENT`。
2. 增加显式 `DRAFT -> SAVED` 保存动作；文字非空或至少一个 AVAILABLE 图片/声音即可成立。
3. DRAFT 只作为 7 天滑动恢复草稿，不进入普通首页、列表或时光轴。
4. SAVED 可继续编辑正文、位置、附件、封面、标签与可逆 Agent 写入；SEALED / UNLOCKED 继续不可变。
5. 封存改为 `SAVED -> SEALED`，且“交给时间”只在保存后作为次级路径出现。
6. 新记录默认 `MOMENT`；旧三类和历史记录类型不批量改写。
7. 迁移现有有效 DRAFT 到 SAVED；空白且没有 AVAILABLE 媒体的异常 DRAFT 保留为短期恢复草稿。
8. authenticated real path 全链路支持 SAVED / MOMENT；Preview 继续只读并明确为示例数据。

### unknown

1. 真实 MySQL 中 DRAFT 总量、空白异常量、含 AVAILABLE 媒体量与历史类型分布尚未在本 change 验证。
2. 真实对象存储下“只有图片 / 只有声音”的上传、提交、显式保存、编辑与失败恢复尚未验证。
3. 微信开发者工具 / 真机上的录音、图片、返回、恢复提示、安全区与键盘行为尚未验证。
4. E0 的真实用户理解证据仍为空，保存反馈和渐进披露只能视为 provisional。

### out_of_scope

- 任意状态单条删除、清除全部、完整导出、远端对象删除恢复与数据所有权页面（P3.2）；
- Agent Prompt、阶段策略、记忆、语气、工具白名单扩张或 provider 行为（P4.1 / P4.2）；
- 时间篇章、自动分类、自动标签、STT、声音语义分析、AI 评分、诊断或 dashboard；
- 新 Tab、社交、分享、公开发现、通知中心、部署、监控、admin portal 或大规模视觉重建；
- 把 E0 原型代码直接复制进生产；
- package / lockfile 或大规模 backend rewrite。

## 5. Goals

1. 把技术恢复 DRAFT 与用户已经完成的 SAVED 明确分开。
2. 让非空文字、至少一张 AVAILABLE 图片或至少一段 AVAILABLE 原始声音任一种都能独立保存。
3. 让新记录默认成为普通 `MOMENT`，不强制用户先选择旧三类。
4. 让 SAVED 在首页、我的记录、时光轴与详情中成为正常用户记录，并保持可编辑。
5. 让封存成为保存后的自愿次级动作，不再与第一次完成记录耦合。
6. 提供 7 天短期恢复草稿，关闭页面时不把未确认内容冒充用户记录。
7. 迁移现有有效 DRAFT，不丢失原文、附件、位置、封面、标签或 Agent 关联。
8. 保持 Preview 隔离、owner 隔离、私有媒体与封存后不可变契约。

## 6. Non-Goals

1. 不在 P3.1 实现 SAVED / SEALED / UNLOCKED 任意删除或全量清理；这属于 P3.2。
2. 不改变对象存储 provider、object key、signed URL、附件限额或私有桶策略。
3. 不修改 Agent Prompt、反思、记忆、护栏、provider 调用次数或 C6 baseline 语义。
4. 不新增“保存评分”、打卡、完成度、情绪标签或催促提醒。
5. 不根据 E0 无结论原型冻结最终动效、声音或复杂保存后流程。
6. 不改变三个一级 Tab 和“我的记录、时光轴、时间回看”命名。

## 7. 用户故事

### 故事 A：一句话已经成立

- 改前坏事：用户主动保存后仍看到“草稿”，不确定是否还必须分类、封存或继续对话。
- 改后不同：一句非空文字可以明确成为 `SAVED`，页面内安静显示“这一刻已经留下”，用户可以立即离开。

### 故事 B：一张图片已经成立

- 改前坏事：用户选择图片时被要求先写正文，图片本身不能成为完整记录。
- 改后不同：编辑器先建立技术恢复 DRAFT，图片校验为 AVAILABLE 后即可显式保存为 `MOMENT / SAVED`，正文可以为空。

### 故事 C：一段声音已经成立

- 改前坏事：用户录下原始声音后仍必须补正文，并可能误以为需要 AI 分析。
- 改后不同：AVAILABLE 原始声音本身满足保存条件，不转写、不分析，也不要求 Agent。

### 故事 D：未确认内容只是恢复草稿

- 改前坏事：关闭页面会把内容永久保存成用户可见 DRAFT，技术恢复与产品完成混在一起。
- 改后不同：未主动保存的内容只进入最长 7 天的技术恢复 DRAFT，普通列表不可见；重新进入时可继续或放弃。

### 故事 E：保存后仍可选择时间

- 改前坏事：封存与未来解锁时间在第一次完成记录前造成压力。
- 改后不同：先保存为 SAVED，记录已经成立；用户之后可选择“交给时间”，不选择也没有缺失步骤。

## 8. 建议待裁决项

| 编号 | 推荐方案 | 备选与代价 |
|---|---|---|
| N1 状态名 | canonical enum 使用 `SAVED`，用户文案使用“已留下” | `RECORDED` 更偏系统术语，且与蓝图/中文反馈不如 SAVED 直连 |
| N2 默认类型 | 新增 `MOMENT` 并作为新记录默认；旧三类原样保留 | 复用 `EMOTION_NOTE` 会把普通生活片段误缩为情绪；批量改旧类型会改写历史语义 |
| N3 显式保存 API | 新增幂等 `POST /api/records/{id}/save`；先更新 DRAFT，再转 SAVED | 创建即 SAVED 无法支持先上传媒体；用 PUT 隐式切状态会让重试和审计不清楚 |
| N4 保存成立条件 | trim 后非空文字，或至少一个 `AVAILABLE` IMAGE / VOICE；pending/failed 媒体、标题、位置、标签、AI 字段不计 | 只看前端可绕过；pending 计入会产生“显示已保存但媒体不存在”的假成功 |
| N5 恢复期限 | `draft_expires_at` 记录最后活动后 7 天；DRAFT 不进入普通列表 | 只靠 `updated_at` 无法覆盖附件活动；localStorage 不能承接真实媒体与跨端恢复 |
| N6 过期清理 | 仅清理过期 DRAFT；远端媒体删除成功/不存在后再删 DB，失败保留锚点下轮重试 | 直接 DB cascade 会遗留私有对象；完整任意状态删除队列属于 P3.2 |
| N7 可编辑矩阵 | DRAFT / SAVED 可改正文、位置、附件、封面、标签及可逆 Agent 写入；SEALED / UNLOCKED 不可变 | SAVED 不可编辑违背蓝图；只放开正文会形成媒体/位置语义不一致 |
| N8 封存起点 | 只允许 `SAVED -> SEALED`，继续要求未来 unlockAt | DRAFT 直封会绕过“主动保存”；自动封存违反用户主动选择 |
| N9 前端层级 | 主区只保留文字、图片、声音和“留下这一刻”；其余放入克制的可选区，“交给时间”保存后出现 | 维持当前全量首屏继续制造负担；直接采用 C 的底部层会把无用户证据写成结论 |
| N10 E0 债务 | 只采用页面内静默反馈、无声音、无自动跳转；具体动效与层级标为 provisional | 声称 A/B/C 胜者会伪造用户证据；永远阻塞 P3.1 又让确定的核心能力无法前进 |
| N11 删除边界 | P3.1 只允许放弃/过期技术 DRAFT；任意用户记录删除留 P3.2 | 提前做全量删除会卷入远端对象、回信、Agent 会话和失败恢复，扩大一刀 |

## 9. 外调预算与闸门

- **规划期外调预算：0**。不连接真实 MySQL、对象存储、provider 或微信真机。
- 闸门 1 只批准 proposal / design / tasks / delta 与 N1–N11。
- 闸门 1 通过不等于可写业务代码；须另行获得闸门 2。
- 闸门 2 不自动包含真实 MySQL、真实对象存储或微信真机；这些属于闸门 3。
- P3.1 不需要新增真实 AI provider 调用；如实现回归触及 Agent，只运行离线既有测试，真实 provider 预算保持 0。
- 闸门 3 建议使用可清理合成记录，不发送用户日记；对象存储只上传合成图片与合成短音频。

## 10. 验收标准

1. backend/frontend canonical enums 包含 `SAVED` 与 `MOMENT`，旧状态与旧类型保持可读。
2. `POST /api/records/{id}/save` 对 owner-scoped active DRAFT 执行原子 `DRAFT -> SAVED`，对 SAVED 幂等，对 SEALED / UNLOCKED 拒绝。
3. 保存条件为非空文字或至少一个 AVAILABLE IMAGE / VOICE；pending、failed、deleted 媒体及其他元数据不能单独成立。
4. 图片-only、声音-only、文字-only 三条 authenticated real path 均可保存，不要求标题、分类、人生节点、Agent、地点、标签或 unlockAt。
5. 新记录默认为 MOMENT；历史 `FUTURE_LETTER / NODE_RECORD / EMOTION_NOTE` 不批量改写。
6. SAVED 可编辑；若操作会让 SAVED 同时失去有效文字与全部 AVAILABLE 媒体，backend 原子拒绝且旧状态不变。
7. location、attachments、cover、tags 与可逆 Agent 写路径一致允许 DRAFT / SAVED，继续拒绝 SEALED / UNLOCKED。
8. seal 只接受 SAVED，继续要求未来 unlockAt，并保持正文、位置、附件、封面不可变。
9. ordinary page/timeline/home 查询不展示 DRAFT；显式 recovery 查询只返回 owner 的未过期 DRAFT。
10. DRAFT 使用 `draft_expires_at`，最近一次合法草稿活动将期限刷新为当前时间后 7 天；SAVED 及以后该字段为空。
11. 过期 DRAFT 清理不因 DB cascade 遗留已知远端对象；远端失败时保留可重试锚点，不记录 storage key 或私人内容。
12. 迁移将现有有效 DRAFT 转为 SAVED；空白且无 AVAILABLE 媒体的异常项保持 DRAFT 并赋 7 天期限；只记录聚合数量。
13. 首页、我的记录、时光轴和详情正确显示 SAVED / MOMENT；用户可见状态文案为“已留下”，不显示技术 DRAFT。
14. 保存成功只显示安静的页面内“这一刻已经留下”；无默认声音、无自动封存、分享、Agent 或页面跳转。
15. “交给时间”只在 SAVED 后作为次级动作；用户不选择时记录仍完整成立。
16. 关闭未确认编辑只形成技术恢复 DRAFT；重新进入可恢复或放弃，不宣称已经保存为用户记录。
17. Preview 继续只读、标识明确、数据与 authenticated session 隔离；不得用 Preview success 证明真实链路。
18. owner/status 条件必须落在 mapper/service 权威层；跨用户 record、attachment、location、cover 访问继续 fail-closed。
19. DDL、测试 schema、enum、mapper、DTO/VO、service、frontend types/store/pages 与 Preview fixtures 同步，不能只改一层。
20. backend focused/full tests、frontend type-check/build、真实 MySQL 迁移、真实对象存储三种保存链路与微信端操作按证据层级分别报告。
21. 用户日记、声音内容、图片内容、storage key、signed URL、secret 不进入普通日志、tracked evidence 或迁移日志。
22. `git diff --check`、范围 allowlist、增量凭据/隐私扫描与 Requirement/Scenario 文件级校验 PASS。
23. E0 继续报告 `INCONCLUSIVE / SKIPPED`，不能因 P3.1 实现而倒填为用户验证通过。
24. OpenSpec CLI 缺失时只报告文件级结构与 delta 对齐，不声称 CLI validation PASS。

## 11. Spec Delta 落点

- `backend-core`：SAVED/MOMENT、显式保存、保存校验、恢复 DRAFT、迁移、可编辑矩阵与封存起点。
- `miniapp-core`：当下保存主路径、图片/声音独立成立、恢复提示、SAVED 展示、保存后次级封存与 Preview 隔离。
- `agent-runtime`：写作引导与可逆工具从“仅 DRAFT”调整为“DRAFT 或 SAVED”，不改 Prompt/provider/调用预算。
- `v2-product-scope`：当下即完整、非强制字段、克制保存反馈、E0 未验证边界与 P3.2/P4.x 非目标。

## 12. 关键风险

1. E0 没有真实用户证据；N9/N10 只能是可逆基线，不是体验验收。
2. 状态扩展会同时触及 record、媒体、位置、封面、Agent、筛选、Preview 与迁移，假 vertical slice 会留下契约裂缝。
3. 过期 DRAFT 清理涉及真实对象存储；H2 或 DB cascade 不能证明远端对象已删除。
4. MySQL 历史 DRAFT 可能含异常数据；迁移必须先只读计数，再执行可回滚、可重复核对的脚本。
5. SAVED 媒体-only 会暴露现有详情/卡片对正文非空的隐藏假设，需覆盖空正文展示。
6. 放开 SAVED 编辑时，删除最后一个有效媒体与清空正文之间存在跨资源不变量，必须由 backend 权威校验。
7. Agent 写作引导从单状态变为状态集合，若只改 UI 或 mode enum 会出现旁路或误拒绝。
8. P3.1 与 P3.2 删除范围容易混淆；本 change 不承诺任意状态删除或完整远端删除编排。

## 13. Gate State

- readiness：**GO（仅规划）**。
- E0：**INCONCLUSIVE / SKIPPED**；不是 PASS。
- 闸门 1（规划批准）：**已批准**（2026-08-10；N1–N11 与全部 artifacts 按推荐方案）。
- 闸门 2（实现授权）：**已授权**（2026-08-10；允许按 `tasks.md` 修改业务代码并执行离线/H2/build 验证）。
- 闸门 2 实现状态：**已完成，等待用户审查**（backend full 91 suites / 687 tests；frontend type-check 与标准/Preview build PASS）。
- Gate 3a（真实 MySQL）：**已授权并完成**（2026-08-12）。preflight 仅输出聚合：3 条 DRAFT 均有有效文字、0 条空白异常、0 条 owner/orphan 媒体异常；迁移后 3 条均为 SAVED，原 FUTURE_LETTER 类型保留，`draft_expires_at` 列、复合索引、MOMENT 默认值、UTC+8 与重复执行稳定性 PASS；迁移后真实 backend list/timeline HTTP 200。
- Gate 3b（真实对象存储）：**未授权**。
- Gate 3c（微信开发者工具 / 真机）：**未授权**。
- Git：规划文档按既有授权由 Agent commit；`push` 未授权。
