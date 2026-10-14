# Active Task

## Status

`ACTIVE`

当前唯一 active Type C change 为 `frontend-visual-language-evolution`。2026-10-13 用户确认按当前 demo 状态收口：登录 HTML 暂缓、翻页暂停推广、玻璃回弹仅候选；授权在同一 change 重写正式迁移阶段并按历史修改本地分组提交。新 proposal/design/tasks/delta 已形成，正式迁移完整规划 Gate 1 待审，Gate 2 未启动；历史授权/未完成证据保留，不接受 delta、不归档、不启动业务实现。
- Active change：`openspec/changes/frontend-visual-language-evolution/`
- 最近归档：`openspec/changes/archive/2026-08-29-time-chapter-foundation/`
- 前一归档：`openspec/changes/archive/2026-08-28-safety-response-minimum/`
- 当前闸门：本轮收口决定与文档/本地提交已授权；当前正式迁移以 F 系列为唯一入口，完整规划 Gate 1 与新任务 Gate 2 分列待办；demo R/M 待验证和 L 暂缓保留，不补勾旧四方向任务
- 提交责任：2026-10-13 本轮用户明确授权 Agent 按历史修改分组本地 add/commit；未授权 push/PR/部署/发布。后续正式实施提交责任另行登记
- 外部调用：仅公开 Inspora/Awwwards 研究；Open Design Cloud 未启动；`open-design` MCP 已可调用，Local Codex 递归加载该 MCP 导致的桌面接管/EPIPE 冲突已做本地隔离修复；当前任务的旧 MCP transport 已关闭，原型重试仍须新任务；真实用户内容、真实 Agent provider、对象存储调用均为 0

**Phase 1（M4 → C1 → C2 → C4 → C3a → C3b → C5）已全部完成。**
**Phase 2 第一刀 C6 `agent-eval-framework` 已于 2026-07-31 归档。**
**Phase 2 第二刀 C7 `agent-reflection-loop` 已于 2026-08-03 归档。**
**Phase 2 第三刀 C8 `agent-resilience` 已于 2026-08-08 归档。**
**Phase 2 第四刀 C9 `agent-temporal-intelligence` 已于 2026-08-08 归档。**

## Previous Completed

- Change：`data-ownership-foundation`（P3.2，**v2.0 核心体验第二刀**）
- 位置：`openspec/changes/archive/2026-08-12-data-ownership-foundation/`
- 开工锚点 `efd2618`｜主实现提交 `7b48086`｜真实验收提交 `907feb4`
- 结果：owner-scoped durable operation、离线可读/媒体可校验导出、双封存策略、四状态单删、clear-all 固定范围与 mutation freeze、远端对象优先删除及重试恢复；五份 delta 已接受进 baseline
- 闸门 3：真实 MySQL、私有对象存储与微信开发者工具完整矩阵 PASS；合成对象、账号、记录、operation 与 artifact 全部清理；真实 Agent provider 调用 0
- 验收保留：T-36 不可压缩真实媒体内存/磁盘峰值 PARTIAL；物理真机与 OpenSpec CLI SKIPPED；本地小样本不等于生产 SLA

- Change：`present-moment-capture`（P3.1，**v2.0 核心体验第一刀**）
- 位置：`openspec/changes/archive/2026-08-12-present-moment-capture/`
- 开工锚点 `2d9544a`｜主实现提交 `3793f1a`
- 结果：`SAVED` / `MOMENT`、显式保存、7 天恢复 DRAFT、可编辑 SAVED、图片/声音/位置与既有封存边界形成真实纵切；四份 delta 已接受进 baseline
- 闸门 3：真实 MySQL、私有对象存储、微信人工功能矩阵 PASS；真实 Agent provider 调用保持 0
- 验收保留：E0 目标用户理解 `INCONCLUSIVE / SKIPPED`，Agent 真实 provider 与 OpenSpec CLI 均未验证；本地通过不等于生产 SLA

- Change：`agent-temporal-intelligence`（C9，**Phase 2 第四刀**）
- 位置：`openspec/changes/archive/2026-08-08-agent-temporal-intelligence/`
- 结果：确定性时间距离、旁支记忆衰减、证据门控 recurrence hint 与 `TEMPORAL_OVERREACH` fail-closed
- 闸门 3：真实 provider 固定合成探针 6/6 PASS；真实 MySQL owner/status/time/decay/recurrence PASS
- 验收保留：eligible 输出实际 `hintUsed=false`；微信真机无可控环境 SKIPPED；OpenSpec CLI 不在 PATH

- Change：`agent-resilience`（C8，**Phase 2 第三刀**）
- 位置：`openspec/changes/archive/2026-08-08-agent-resilience/`
- 结果：8 类封闭失败 taxonomy、共享 24000ms provider-work budget、零自动 retry、阶段化固定失败模板与无内容 trace
- 闸门 3：真实 DeepSeek 固定合成探针 6/6 PASS；真实 MySQL 同轮 retry/attempt trace PASS
- 验收保留：微信真机因本机无可控环境 SKIPPED；OpenSpec CLI 不在 PATH，使用文件级校验

- Change：`agent-reflection-loop`（C7，**Phase 2 第二刀**）
- 位置：`openspec/changes/archive/2026-08-03-agent-reflection-loop/`
- 结果：仅非 `CLOSING` reply 的 `MISSING_TIME_ATTRIBUTION` 可受控重写一次；material、tool、
  `CLOSING` 与 provider error 不开环；后端全量 **622 tests PASS / 4 skipped**
- 验收保留：真实 MySQL、闸门 3 provider/真机、人评锚点、OpenSpec CLI validate 均未执行，详见 closeout

- Change：`agent-eval-framework`（C6，**Phase 2 第一刀**）
- 位置：`openspec/changes/archive/2026-07-31-agent-eval-framework/`
- 开工锚点 `486ca95`｜实现提交 `aedab6c`（+ `f2c998b` hash 补录）
- 结果：**离线、零外调、`src/main` 零改动**的评测框架落地并归档。
  轨迹级不变量（硬失败）+ 快照回归比对（需人确认），23 条合成用例、23 条带留痕基线。
  delta 已接受进 baseline：`agent-runtime`（1 条 MODIFIED——C5「范围内的评估能力」改为指向 C6，
  保留范围声明不删 + 6 条新增）、`backend-core`（5 条）、`agent-collaboration`（3 条）；
  `v2-product-scope` 与 `miniapp-core` 无 delta
- 验证：后端 **606 tests PASS / 4 skipped，BUILD SUCCESS**
  （536 基线 + 70 新增，**零回归、既有断言零修改**；4 skipped 仍是原有环境门控探针，未新增跳过）
- **闸门 3：未申请**（外调预算 0，全程未启用任何真实 provider 探针，实测外调 0 次）。
  用户 2026-07-31 表述为「闸门 3 通过」，但本刀并无外调可授权，故如实记为**未申请**
- **三项未完成项如实登记**（详见 closeout §3，**不得在后续文档写成已完成**）：
  ① 快照指标在真实 provider 下的稳定性**未验证**；
  ② 话术质量人评锚点**为空**（结构已就位，建议顺带在 C7 闸门 3 填）；
  ③ 仓库**无 CI**，交付的是「一条 maven 命令可跑」，**不是** CI 门槛
- **实现期五处发现**（详见 closeout §4）：基线实测 536/4 非 534/3；护栏一处 n-gram 覆盖边界
  （未校准，只钉成回归）；修掉一条自己写的「恒成立所以什么都没测」的假用例；
  snakeyaml 自动把 timestamp 转 Date；`ArgumentCaptor` 不适合多轮取轨迹
- 更早：`agent-observability`（C5，Phase 1 收官刀，位置
  `openspec/changes/archive/2026-07-30-agent-observability/`；闸门 3 真实调用 6 次，
  耗时 min 4571 / avg 6476 / max 8467ms；归档后修掉三个 Type B）、
  `agent-review-chat`（C3b）、`agent-memory-retrieval`（C3a）、`agent-guardrails-hardening`（C4）、
  `agent-tool-calling`（C2）、`agent-runtime-mvp`（C1）、`m4-real-capability-completion`

## Direction Layer

- **当前权威蓝图**：`Docs/agent-iteration/roadmap/iteration-blueprint.md` **v2.0 已冻结**（2026-08-09）
- **上游产品宪章**：`Docs/agent-iteration/roadmap/core-product-definition.md` **v0.1 已确认**（2026-08-08）
- **历史蓝图**：`Docs/agent-iteration/roadmap/iteration-blueprint-v1.2.md`（Phase 1 / Phase 2，已完成、只读）
- 主线进度：M4 → C1 → C2 → C4 → C3a → C3b → C5（Phase 1 收官）→ **C6/C7/C8/C9 已归档**
- **Phase 2 定案序**：~~C6 agent-eval-framework~~ → ~~C7 agent-reflection-loop~~ →
  ~~C8 agent-resilience~~ → ~~C9 agent-temporal-intelligence~~
- **v2.0 冻结序列**：H0 `truth-surface-cleanup` → E0 `capture-ritual-prototype` →
  P3.1 `present-moment-capture` → P3.2 `data-ownership-foundation` →
  P4.1 `witness-agent-alignment` → P4.2 `memory-agency` → R1 `safety-response-minimum` →
  E1 `time-chapter-prototype`（有正证据才进入 P5.x）
- **历史方向状态**：H0 已完成；E0 因没有真实参与者以 `INCONCLUSIVE / SKIPPED` 收口，
  未选出 A/B/C 胜者；P3.1、P3.2、P4.1、P4.2 与 R1 均已归档。冻结序列下一项为
  E1 `time-chapter-prototype`；开始前必须新建独立规划 change，旧 Optional C0/C10/C11 继续证据触发
- **C7 开工前必须注意的三件事**（来自 C6 closeout §9）：
  1. **以实测值重算类大小论证**：`AgentChatServiceImpl` 实测 **1274 行**，
     蓝图 §3.2 记的 1183 行已过时（C6 登记的勘误，蓝图已冻结未改）
  2. **人评锚点建议顺带在 C7 闸门 3 填**——C7 必然要真实联调重写路径，
     同一批真实产出既验重写效果又能当语言质量锚点，比单独申请一轮外调更省
  3. **C7 若改变编排行为，C6 的快照会变，那是预期的**——须在 `baselineNote` 里
     写明由 C7 改的，而不是把数字改成当前值了事
- **C6 已为 C7 备好前置**：反思环的价值主张是「重写后质量更好」，
  这个主张现在第一次可以被证伪（D30 排序决策的兑现）
- **Optional（不排主线）**：C0 平台升级（Boot 4.x/Java 21，Phase 2 完工后再议）、
  C10 语气标定、C11 上下文架构——均需证据触发
- **对外叙事文档**：`Docs/agent-iteration/narrative/agent-tech-story.md`（D33：每刀归档时更新对应段落；
  **§1–§9 已写**，§10 持续追加）

### v1.2 冻结的关键决策（D25–D33，开 C6 前必读）

| # | 决策 |
|---|---|
| D25 | Phase 2 以**能力叙事**为主驱动；未上线，不为想象中的生产故障提前投入 |
| D26 | 「前沿」限定在 **Agent 层**；平台升级降为 Optional C0 |
| D27 | **不引入图框架**；改为引入受控环 + 留可讲述 ADR |
| D28 | 反思环判定源**复用 C4 确定性护栏**，不新起 LLM 自检器 |
| D29 | 重写指令**只回传违规类型**，不携带候选文本片段 |
| D30 | **Eval 先于反思环**：先建量尺，再改模型输出行为 |
| D31 | **LLM-as-Judge 排除在 C6 之外**（隐私外发 + 预算 + 不可复现） |
| D32 | Eval 覆盖**轨迹不变量 + 回归比对**，不做绝对质量判分 |
| D33 | 叙事文档是每刀**固定收尾产物** |

## Source Of Truth

- `AGENTS.md`
- `Docs/agent-iteration/roadmap/core-product-definition.md`（**v0.1 已确认**；长期产品方向）
- `Docs/agent-iteration/roadmap/iteration-blueprint.md`（**v2.0 已冻结**；核心体验与信任兑现序列）
- `Docs/agent-iteration/roadmap/iteration-blueprint-v1.2.md`（历史只读；C1–C9 能力叙事）
- `openspec/project.md`
- `openspec/changes/archive/2026-08-29-time-chapter-foundation/`（P5.x accepted / archived）
- `openspec/changes/archive/2026-08-28-safety-response-minimum/`（R1 accepted / archived）
- `openspec/changes/archive/2026-08-28-memory-agency/`（P4.2 accepted / archived）
- `openspec/changes/archive/2026-08-25-witness-agent-alignment/`（P4.1 accepted / archived）
- `openspec/changes/archive/2026-08-12-data-ownership-foundation/`（P3.2 accepted / archived）
- `openspec/changes/archive/2026-08-12-present-moment-capture/`（P3.1 archived）
- `openspec/specs/agent-runtime/spec.md`（含 C1–C9 与 P3.1 的 Agent 兼容契约）
- `openspec/specs/backend-core/spec.md`（含 M4、C1–C9 与 P3.1 当下记录契约）
- `openspec/specs/miniapp-core/spec.md`（含 M4、相关 Agent 阶段与 P3.1 小程序契约）
- `openspec/specs/v2-product-scope/spec.md`（含 M4、C1–C9 相关范围与 P3.1 产品范围）
- `openspec/specs/agent-collaboration/spec.md`（含 C5 + C6 + C7 + C8 + **C9**）
- `openspec/changes/archive/2026-08-08-agent-temporal-intelligence/`（C9 archived）
- `openspec/changes/archive/2026-08-08-agent-resilience/`（C8 archived）
- 开工清单：`Docs/agent-iteration/workflow/prompt-snippets/type-c-checklist.md`

## Current Progress

- **This session**: 2026-10-14 — **恢复并完成昨天的F-04：正式迁移规划/历史提交收口**
  - **Authorization / Scope**：用户要求继续昨天未完成任务；延续已明确的同一change文档收口与Agent本地分组提交授权。正式代码、外调、push、delta接受/归档权限不扩大。
  - **Progress**：F-01—F-04完成。已核对历史报告daa8ba8、demo成果78eb150、正式规划1180946；1180946提交后工作区clean，记录为内容规划基线；本条与tasks/log的收尾提交自身hash由git log查证。旧T/R/M/L状态保持，不将收口当实际验收。
  - **Verification PASS**：18/18当前工件/范围复核，历史快照字节/原报告/日志历史前缀一致；当前暂存/工作区diff检查通过；相对b6afb01的frontend/backend/package/accepted specs/archive无修改；15个HTML链接检查无缺失；新增登录HTML不存在。原报告6处既有行尾空白保留，累计历史格式检查不宣称全PASS。
  - **SKIPPED / Risks**：OpenSpec CLI仍不在PATH；今日未复跑昨日已通过的前端type-check，未做Standard/Preview构建、微信/真机、实际demo动效或真实服务联调。玻璃回弹候选、翻页不推广、登录HTML暂缓；原浏览器file拒绝不绕过。
  - **Next**：当前完整proposal/design/tasks/miniapp delta交付审阅，下一项F-10（Gate 1）；F-11（Gate 2）须另行明确后才修改正式前端。唯一active不变，不恢复IDLE或启动历史Optional。

- **This session**: 2026-10-13 — **demo 阶段按现状收口；正式迁移规划重写与历史成果入库**
  - **Type / Authorization**：Type C 规划收口，同一 active change。用户确认单方向/登录 HTML 暂缓/翻页暂停推广/回弹候选，并授权文档重写与 Agent 本地分组提交；无正式业务实现、真实外调、push、delta 接受或归档授权。
  - **Progress**：F-01—F-03完成；保存重写前四工件到history/2026-10-13-before-migration-rewrite，重写完整proposal/design/tasks/miniapp delta与A–E映射/文件范围/验收/回退。F-04提交与核对进行中；F-10完整规划批准、F-11实现授权pending。旧T/R/M/L状态不补勾。
  - **Current scope**：十四页 demo 保留现状；新登录HTML不存在，线稿/薄雾仅历史草案；look/writing翻页只保留局部研究代码，不推广；玻璃回弹仍待目标平台验证。正式登录沿用现有代码，本轮无frontend/backend修改。
  - **Verification**：现状type-check已通过（可用Node直接运行项目vue-tsc --noEmit）；最终工件/日志只追加/范围/静态/提交检查待记录结果。Git本轮开工b6afb01、16 tracked修改+8 untracked，暂存空。
  - **SKIPPED / Risks**：OpenSpec CLI不在PATH；Standard/Preview构建、微信/真机、最新demo实际渲染/IME/动效性能与真实服务联调均未补验。原本地浏览器拒绝不绕过；源码/合成证据不冒充设备/后端PASS。
  - **Next**：先完成F-04并留下clean HEAD；后续从F-10审阅当前完整正式迁移规划开始，经F-11才启动代码。历史报告只作时点快照，不重宣称其测试数值已复验。


- **This session**: 2026-10-13 — **登录页修订图与薄雾入场规划，未实施**
  - **User direction**：用户解除此前登录暂缓，偏向第二构图并要求收敛艳色/笔触/按钮颜色；提出淡粉雾扩散→留白→首页。当前翻页反馈不流畅，停止推广；玻璃回弹反馈尚可，保留候选，不将其当全平台验收。
  - **Scope / Progress**：Type C规划，沿用唯一active change；新增demo/LOGIN_PLAN.md、增补proposal/design/tasks/miniapp delta；生成单张修订图exec-2bb5ebea-f3d5-431a-b384-32e5e477e9ed.png。L-01完成，L-02规划批准与L-03实施授权分别pending；未新增login HTML或改首页/index/样式/脚本。R/M进度与待补证据不变。
  - **Plan**：拟新增登录视觉demo，真实认证/注册/微信/SMS预算0；测试输入只在DOM，不写凭证；进入明确预览后一次薄雾/暖纸白/首页接续。减动态、无脚本、素材失败、重复操作、中断/BFCache均需可用终态；D16—D18记录取舍。
  - **Verification**：修订图直接查看并展示；规划工件与范围检查见最新日志。实际HTML/转场/小屏/键盘/性能尚未实现或验证；旧file访问拒绝不绕过，OpenSpec CLI验证待补。
  - **Next**：先由用户确认修订视觉与完整新增规划（L-02）；实施授权（L-03）独立登记后按L-04—L-10推进。不是重新申请已获R/M实施权限，不自动提交/迁移/发布。

- **This session**: 2026-10-13 — **回看/书写两页动效样本实施，观感待用户复核**
  - **Authorization**：用户在完整翻页/玻璃调研后明确「可以先尝试落地几页看看落实效果」。M-01记录具体切片接受；M-02记录两页demo实施授权。仅demo动效，提交默认用户手动。
  - **Progress**：M-01—M-06/M-08共7/8完成；M-07保留SKIPPED/pending；既有R批13/20与7项实际验证待补保持不变。
  - **Changes**：look/writing显式加载motion-samples CSS/JS；620ms双面纸页开启/纸边微折/移动阴影/主动重播；药丸轻压、240ms恢复与触点光；长文/减动态/无3D静态降级，离页/后台/滚动/字体/resize/BFCache/偏好切换清理。存储/保存接线、其它页与图片未改；规划/miniapp delta只增补两页动效范围，D13—D15记录取舍。
  - **Verification PASS**：21项事件/计时器/既有flow接线合成用例；当前HTML重建fixtures后的26项Storage/简化DOM回归；15HTML/128本地链接、CSS解析、JS语法、23个保护文件哈希不变、git diff --check。无真实用户原文/日志/网络/provider/对象存储/新依赖。
  - **SKIPPED**：旧file访问策略拒绝仍有效，未用替代地址/工具/服务绕过；没有新实际页面截图、连续动作、系统偏好、触控、性能、微信或真机证据。OpenSpec CLI未执行，仅工件结构检查；不称真实软纸卷曲、实体折射或视觉定稿。
  - **Next**：用户刷新demo查看look「再翻开一次」及writing保存按钮；在获准环境补M-07与R的旧视觉验证，再依据观感调整参数。无需重新申请本批实施许可；不自动推广其它页、提交或迁移生产。

- **This session**: 2026-10-13 — **五核心页 demo 实施第一版，视觉验收待补**
  - **Authorization**：用户在完整规划交付后明确「可以开始在demo实现了，我已经备份了demo」；分别登记 R-02 规划接受、R-03 demo 实施授权。无需再次申请实施；不含 git add/commit/push/生产迁移/真实外调。
  - **Progress**：R-01—R-04、R-07/R-08、R-10—R-14、R-18/R-20 共13项完成（实现/静态或合成证据）；R-05/R-06/R-09/R-15—R-17/R-19 共7项保留 pending，未声明20/20。
  - **Changes**：五页+index、共享tokens/components/motion、新demo-store/demo-flow；16px/400正文/必读辅助墨、核心页纸纹与玻璃影微调、可滚壳层/独立动作区、本地输入/保存/回看/草稿恢复/清除。除无滤镜霜卡保护性共享CSS降级，九页HTML未动；照片未改。
  - **Verification PASS**：26项Storage/简化DOM合成用例；15个HTML、124个本地资源链接结构检查；JS语法；9页HTML/2张图片/2份既有untracked文档哈希一致；正文/辅助色对代表性实色纸底4.708—5.376:1；无真实业务请求、长期存储或正文日志；git diff --check。
  - **SKIPPED**：既有浏览器连接/file://策略拒绝仍未解除，本轮未用替代地址/工具/服务绕过；无实际渲染、参数并排、软键盘、字体回退、玻璃/减少动态运行、微信/真机证据；OpenSpec CLI不可用。原型为可审阅实现草稿，未视觉定稿。
  - **Next**：在访问被明确允许的环境中补实际五页/九页回归与参数选择、动效/输入手验，然后用户验收；不再次索取已授实施许可，不扩写九页功能或自动提交。

- **This session**: 2026-10-13 — **五核心页 demo 精修与 sessionStorage 文字演示规划增补（未实现）**
  - **Type / Scope**：Type C 规划，沿用唯一 active change；只修改规划/任务/delta/进度/日志。详细规划：`demo/REFINEMENT_PLAN.md` v1.0。
  - **用户已确认**：home/writing/saved/timeline/look 首轮精修，其余九页回归；同时接通输入/保存/回看；sessionStorage 同标签页跨页与刷新。参数、单条演示模型、容量与完整验收仍待规划批准。
  - **Progress**：R-01 完成；R-02 规划批准、R-03 独立实现授权待确认；R-04—R-20 尚未开始。proposal/design/tasks/miniapp delta 新增附录，历史 T-* 不回改；accepted specs 未动。
  - **研究**：Apple 材质层次、MDN 背景模糊/动态视口/安全区/键盘/存储/字体、W3C 中文排版/对比/点击区/重排与 web.dev 动画；证据与采用方案写入规划。
  - **验证边界**：本地页面捕获仍受既有浏览器安全策略拒绝，禁止换地址/服务/工具绕过；无本轮视觉/存储运行/微信/真机证据。OpenSpec CLI 未发现，采用工件结构与范围检查；SKIPPED 不视为通过。
  - **Next**：用户分别批准增补规划（R-02）与授权实现（R-03）后，先做 R-04 文件锚点、R-05 获准环境稳定截图；不能获得图则保留视觉验收阻塞，再按规划限制推进。

- **This session**: 2026-09-06 — **方向转向：锁定「信笺×微液态玻璃」，brief 重写 v3.0；分阶段提交 v2.1–v2.3 积压**
  - **Trigger**：用户评审第四轮原型（仓外 flashback-four-directions4.html）后仍不满意且无法具体化；经 grill 式访谈定位为"感受层"缺口——参照物不在 A/B/C/D 候选集内，而是现行 2.0 前端的书信质感（折角信卡、红圆印封/阅、存档编号、封存霜卡、竖排侧注、"时间的旅人"语汇）；用户确认以 2.0 为基底、在宣纸极简与液态玻璃之间找平衡，并授权停止四方向选择
  - **访谈裁决（全部经用户逐条确认）**：玻璃剂量地图（纸=世界材质/玻璃=动作与暂时之物/霜=被封存时间；药丸=动作、取景框=位置；Tab 与浮层轻玻璃；内容卡禁玻璃；本轮无深色模式）；首页融合结构（2.0 叙事＋取景框空位，v4 降级为元素供体）；信笺家具 7 条（红印升格、折角限权、编号统一汉字式、倒计时改静态期待、MEMORY 删、竖排侧注软文案、预计解锁保留）；个人中心门厅（数字并成保管事实句、阶段总结改"整理成一封近况"、托管凭证卡配"守"印）；书写页附言收敛；保存"落印"与抵达"融霜"两枚签名动效；Tab"我的"恢复规范名"个人中心"
  - **Changes**：`OPENDESIGN_BRIEF.md` 重写 v3.0（单方向精修：材质故事/剂量地图/家具规范/动效规范/九屏要求/材质细则/合规命名/五帧+三张材质放大稿交付定义）；`.gitignore` 忽略 `2.0页面效果/`（该目录含疑似真实日记原文截图，仅限本机参考，永不入库）
  - **Git**：用户本轮明确授权 commit（不含 push）。分五笔按内容提交：A `d6931d9` brief 迭代至 v2.3；B `9b4bc4f` 补录 v2.1–v2.3 评审记录；C `7f680ac` gitignore 隔离截图目录；D brief v3.0；E 本条与 AGENT_LOG 记录；工作树恢复 clean
  - **Boundary**：Type B 规划文档修订；frontend/backend/package/lockfile/accepted baseline 无 diff；真实 provider/对象存储调用 0；截图真实文字未复述进任何 tracked 文件
  - **Next step**：用户复核 brief v3.0 文本 → 下一轮 Open Design（Local Codex）按 §10 生成单方向五帧＋三张材质放大稿 → 用户确认后按既有 Gate 2 授权进入实现

- **This session**: 2026-08-30 — **brief 升版 v2.3：首页头部词语/中部死区/个人中心重设计/Apple 工艺基准**
  - **Trigger**：用户评审第三轮原型（仓外 flashback-four-directions3.html），指出：“我的记录”一词出现在首页头部即是突兀来源（词错非仅字号错）；书写面与最近留下卡片之间死区过大（源码为 `.recent{margin-top:auto}` 撑空）；个人中心形同敷衍（裸列表＋箭头的 iOS 设置皮肤）；决定采用 Open Design 的 Apple 设计系统重做一轮
  - **Agent 立场**：借 Apple 工艺（8pt 网格、字阶对比、控件一致性、动效纪律），不借 Apple 身份（#0071E3 蓝、#F5F5F7 冷灰、SF Pro 替宋体、iOS 控件皮肤）——蓝图 §9.4 冻结的纸暖气质与朱砂符号不可让渡
  - **Changes**：OPENDESIGN_BRIEF.md 升版 v2.3——§11.1 新增第 7 条（“我的记录”退出首页头部，顶部仅小号“时光回序”字标或留空）与第 8 条（中部死区禁令：最近留下紧跟书写面 24–28px，余白留底部）；§12.7 重写为个人中心重新设计（三区结构：身份与保管陈述/记录与时间/数据与信任，数据与所有权为突出信任锚点，禁 iOS 设置皮肤）；新增 §13 Apple 工艺基准映射（§13.1 借工艺三条、§13.2 不借身份四条、§13.3 强制令牌覆盖）；§0 优先级改为附录间以编号大者为准；§11.5 自检范围扩至 §13
  - **Boundary**：Type B 规划文档修订；仅改 brief 与 .ai 记录；frontend/backend/package/lockfile/accepted baseline 无 diff；真实用户内容、secret、provider/对象存储调用均为 0
  - **Commit**：pending；v2.1/v2.2/v2.3 三轮 brief 修订均未提交，积压等待用户指示（push 未授权）
  - **Next step**：用户以 Apple 设计系统运行 v2.3 修订 prompt；本轮应作为收敛轮，完成后按 STYLE_DIRECTIONS 五条标准锁定主方向，避免原型无限迭代

- **This session**: 2026-08-30 — **brief 升版 v2.2：质感工艺清单与个人中心切片补齐**
  - **Trigger**：用户评审第二轮原型（仓外 flashback-four-directions2.html），认定整体较好了但细节不足、偏朴素；首页“我的记录”与“人”按钮突兀、标语被改得过小（走了另一个极端）；时光轴等页面也有不同程度问题；并确认个人中心页未做
  - **Diagnosis**：源码核实——头像按钮为汉字“人”冒充图标；标语被并入 11px meta 行（§11.1.2“一行小字”措辞被极端执行，指令责任在 Agent）；色阶差约 1–3%、字阶挤平、Tab 激活态朱砂被改中性、A 方向照片带 saturate(.76) 压灰滤镜、阴影 4–6% 骑墙、间距等距——六笔工艺欠账构成“朴素”观感；时光轴章节处三重重读叠加
  - **Changes**：OPENDESIGN_BRIEF.md 升版 v2.2——§11.1 第 2 条改为承诺句三档层级（主角/中层 14px 宋体独立行/meta 11px，精确到数值）；§11.1 新增第 6 条页头降噪（线性 SVG 人形图标禁汉字冒充、标题降 12–13px muted）；§11.3 新增第 5 条章节重音收敛；新增 §12 质感工艺（色阶/字阶/朱砂点位含 Tab 激活态恢复/照片还原/阴影定档 10–14%/间距节奏）与 §12.7 个人中心切片（身份区＋安静入口列表，禁设置后台感）；§4 artifact 扩为四张关键界面；§0 优先级补 §12 优于 §11
  - **Boundary**：Type B 规划文档修订；仅改 brief 与 .ai 记录；frontend/backend/package/lockfile/accepted baseline 无 diff；真实用户内容、secret、provider/对象存储调用均为 0
  - **Commit**：pending；含上一轮 v2.1 修订仍未提交，等待用户指示（push 未授权）
  - **Next step**：用户将 v2.2 修订 prompt 交给设计 agent 执行第二轮提质与个人中心补齐；方向选择子闸仍 pending

- **This session**: 2026-08-30 — **brief 升版 v2.1：并入首页/时光轴第一轮迭代附录**
  - **Trigger**：用户评审第一轮四方向原型（仓外 flashback-four-directions.html），认定整体大进步、编辑器时刻卡片语言成立，但观感未达标：首页书写面层级倒置、图片/声音无可供性、“最近留下”过简；时光轴脊柱日期与节点互相遮挡、条目展示过简
  - **Diagnosis**：对原型源码核实，theme B 脊柱日期标签（left:-52px、宽 38px）与方块节点（left:-23px）重叠约 8px，属布局缺陷；首页为承诺句 32px 占主角、书写面退化为横线＋灰 placeholder 的层级倒置
  - **Changes**：OPENDESIGN_BRIEF.md 升版 v2.1——新增 §11 迭代附录：11.0 两句验收标准（首页=被标出的空位、时光轴=节奏跟随内容）；11.1 首页五条（书写面=位置本体、承诺句降级、媒体可供性、最近留下=时刻卡片、空状态）；11.2 时刻卡片全局组件与禁则；11.3 时光轴四条（脊柱重构两方案、节点最多两态、内容形态节奏、分割线退役）；11.4 动效边界（签名动效=时间轴本身，禁视差堆叠/滚动劫持/循环动画）；11.5 逐条自检；§0 优先级补 §11 从属于 §2
  - **Boundary**：Type B 规划文档修订；仅改 brief 与 .ai 记录；四方向结构与既有边界未变；frontend/backend/package/lockfile/accepted baseline 无 diff；真实用户内容、secret、provider/对象存储调用均为 0
  - **Commit**：pending；本轮修订尚未提交，等待用户指示（push 未授权）
  - **Next step**：用户将修订 prompt 交给设计 agent，按 §11 只修订首页与时光轴；方向选择子闸仍 pending

- **This session**: 2026-08-30 — **OPENDESIGN_BRIEF 升版 v2：并入“位置/房间”气质底线与必读文件**
  - **Trigger**：用户评审 gpt5.6sol + Open Design 三稿首页，判定杂志/出版物隐喻气质错位（出版物家具、已保存记录占据视觉中心、满页分隔线、纸上留白稿混入馆藏语汇）；经宪章对照讨论后决定新开会话重新设计
  - **Changes**：`Docs/design/frontend-visual-language-evolution/OPENDESIGN_BRIEF.md` 升版 v2——新增 §0 必读文件（宪章重点章节、蓝图仅 §9.4、STYLE_DIRECTIONS）与冲突优先级；§2 四方向共同产品气质底线 6 条（首页是位置/房间与 3 秒检验、留下此刻为视觉重心、最近片段“安放不展出”、系统声音最小化、时间只以现在出现、强调色只服务动作）；§3 上一轮失败教训；§9 Avoid 扩展出版物家具等禁项；§10 增加 §2 逐条自检
  - **Boundary**：Type B 规划文档修订；四方向结构、固定导航命名、动效目标、合成内容边界未变；STYLE_DIRECTIONS.md 与其余规划文件未改；frontend/backend/package/lockfile/accepted baseline 无 diff；真实用户内容、secret、provider/对象存储调用均为 0
  - **Commit**：pending；默认用户手动提交
  - **Next step**：用户新开 Open Design 会话，先令其通读 brief §0 三个文件再按 v2 重新生成四方向可比原型；方向选择子闸仍 pending，主方向锁定后才落既有 Gate 2 实现授权

- **This session**: 2026-08-30 — **“纸上留白”Local Codex 重试因 daemon restart 失败**
  - **Requested scope**：继续现有“纸上留白”方向，只生成首页、记录编辑器、时光轴三个移动端关键帧；固定合成内容；不修改 Flashback 业务代码
  - **Mode / brief**：保持 Local Codex，不进入 Open Design Cloud 登录或 credits；需求卡再次错误回填“网页应用”，生成 prompt 已以用户明确确认的“移动应用/小程序应用、一个核心流程、精致产品界面”覆盖
  - **Execution**：新建独立项目 `Flashback 纸上留白移动端原型 2026-08-30 重试`，启动一次新的逻辑生成；未复用上一轮失败 run 或空项目
  - **Generation result FAILED**：run 在约 6 秒后因 daemon restart 以 `DAEMON_RESTARTED` 终止，`artifactCount=0`；独立复核项目文件 0、`entryFile=null`，无 Preview/Studio 成品链接
  - **Verification boundary**：未生成首页/编辑器/时光轴、两项动效或静态降级说明；T-17/T-18/T-19 继续 pending，不以空项目、需求确认或失败 run 作为视觉证据
  - **Scope safety**：未修改 frontend/backend/package/lockfile/accepted baseline；未使用真实日记、真实媒体、账号、位置或 secret；未调用 Open Design Cloud、backend provider 或对象存储
  - **Commit**：pending；本次失败检查点不提交、不 push
  - **Next step**：不得自动创建第三次逻辑生成；先稳定 Open Design daemon 生命周期，再由用户确认重试 Local Codex，或由用户明确选择切换到 Open Design Cloud / secure BYOK

- **This session**: 2026-08-30 — **“纸上留白”Local Codex 生成链路已打通，单次 run 因本地运行时关闭而终止**
  - **Requested scope**：读取现有 `OPENDESIGN_BRIEF.md`、`PLAN.md`、`STYLE_DIRECTIONS.md`；只生成首页、记录编辑器、时光轴三个移动端关键帧；固定合成内容；不修改 Flashback 业务代码
  - **Mode / brief**：保持 Local Codex，不进入 Open Design Cloud 登录或 credits；用户已确认“移动应用/小程序应用、一个核心流程、精致产品界面”。需求卡 skip 路径错误回填网页默认值，生成 prompt 已按用户明确选择校正为移动端，不重复创建第二份 brief
  - **MCP verification PASS**：新任务可调用完整 Open Design 工具；Local Codex 可用且已认证；T-15b 完成
  - **Generation result CANCELED**：新建 `Flashback 纸上留白移动端原型` 项目并启动唯一一次生成；运行约 48 秒后 Open Design 本地运行时关闭，任务以 `daemon_shutdown` 终止，`artifactCount=0`、项目文件 0、无 entry file、无 Preview/Studio 成品链接
  - **Verification boundary**：Local Codex 已实际启动并产生模型输出，但未写入原型文件；不得把空项目、运行中 Studio 地址或需求确认写成已生成关键帧，T-17/T-18/T-19 保持 pending
  - **Scope safety**：未调用 Open Design Cloud account/credits；未修改 frontend/backend/package/lockfile/accepted baseline；未使用真实日记、真实媒体、账号、位置或 secret；backend provider 与对象存储调用为 0
  - **Commit**：pending；用户允许按需中文 commit，本次失败检查点不提交、不 push
  - **Next step**：由用户明确要求重试后，保持同一人类可读 brief、Local Codex、移动端与合成内容边界，建立新的逻辑生成；不得静默切换 Open Design Cloud 或以手写原型冒充 Open Design 结果

- **This session**: 2026-08-30 — **“纸上留白”Local Codex 原型生成受本地运行时阻断**
  - **Requested scope**：只生成首页、记录编辑器、时光轴三个移动端关键帧；固定合成内容；不修改 Flashback 业务代码
  - **Mode**：用户明确选择 Local Codex，不使用 Open Design Cloud；需求卡已确认并校正为“移动应用/小程序应用、一个核心流程、精致产品界面”，未进入 Cloud 登录或 credits 流程
  - **MCP verification**：当前任务已可调用 `open-design` 的 brief 工具，但 project/agent/run 工具无法连接 `127.0.0.1:7456`，因此 T-15b 仍未完成
  - **Root cause**：旧条目用系统 Node 24 启动官方 daemon CLI，`better-sqlite3` 的 ABI 145 与 Node ABI 137 不兼容；官方 `--headless --mcp-install codex` 已将单个 MCP 条目改回签名 `Open Design.exe`
  - **Remaining blocker**：当前任务仍持有旧 MCP 进程快照；桌面实例使用动态本地端口，无法被本任务中的旧代理访问。安全检查拒绝强制结束所有同名进程，避免中断潜在未保存会话
  - **Verification SKIPPED**：未创建 Open Design project、未调用 `start_run`、未生成关键帧/预览链接、未创建 demo 目录
  - **Scope safety**：frontend/backend/package/lockfile/accepted baseline 均未修改；真实用户日记、媒体、账号、secret、真实 Agent provider 与对象存储调用均为 0
  - **Next step**：在新 Codex 任务加载修正后的签名 MCP 入口后，复用本轮已确认的人类可读 brief，重新建立一次 attributed workflow 并生成“纸上留白”三关键帧

- **This session**: 2026-08-30 — **Open Design MCP 全局注册完成，等待新任务热加载**
  - **User state**：用户确认 Open Design 已登录并完成 Codex 配置，客户端可选择 5.6 Sol
  - **Installer finding**：Open Design 0.21.0 官方安装器最初写入 `command=od`，但真实用户 PATH 无 `od`，该条目无法启动；未保留这个不可执行配置
  - **MCP fix**：仅替换 `open-design` 条目，改用 `C:\Program Files\nodejs\node.exe` 启动安装包内 `daemon-cli.mjs mcp`；其它 MCP 条目保持不变
  - **Verification PASS**：真实用户 `CODEX_HOME=C:\Users\Lin\.codex`、config parse ok、4 个 MCP；`codex mcp get open-design --json` 显示 enabled=true，官方 stdio 启动器帮助可解析
  - **Hot-load boundary**：当前任务工具快照尚无 `open-design` callable；按插件要求需启动一个新 Codex 任务完成 T-15b
  - **Scope safety**：未修改 frontend/backend/package/lockfile/accepted baseline；未读取或写入 Open Design/Codex credential，未调用模型生成、对象存储、commit、push 或部署

- **This session**: 2026-08-30 — **Gate 1 / Gate 2 已授权，Open Design 与方向选择前置阻断**
  - **Authorization**：用户明确批准 Gate 1 规划工件和 Gate 2；实现授权只适用于最终选定方向，不包含 commit、push、PR、deploy、release、delta acceptance 或 archive
  - **Open Design check**：插件技能包存在，但 `codex mcp get open-design --json` 返回未注册；未检测到 Open Design/`od` 可执行程序，官方下载安装页已再次请求在 Codex 右侧打开
  - **Gate ordering**：四方向原型、评审与主方向锁定尚未完成；不把 Gate 2 授权解释为 Agent 可以任选四个方向直接实现
  - **Execution**：本轮没有调用 Open Design Cloud、没有消耗 account/credits、没有修改前端业务代码
  - **Next step**：安装并注册 Open Design 后启动新任务；确认 brief，生成四方向同切片原型，锁定主方向后按既有 Gate 2 授权开工

- **This session**: 2026-08-30 — **`frontend-visual-language-evolution` Gate 1 规划草案已建立**
  - **Readiness**：从 `ACTIVE_TASK=IDLE` 建立唯一 active Type C change；既有 M1 固定高保真合同保持只读，本 change 作为多方向视觉语言探索的后继规划
  - **Research**：以公开页面整理 Awwwards 的编辑索引、时间滚动、文字揭示、卡片堆叠与图像转场；Inspora 动态画廊受 Vercel checkpoint 限制，逐项视觉结论保持未核验
  - **Artifacts**：完成 proposal/design/tasks、miniapp-core 与 v2-product-scope delta，以及 `Docs/design/frontend-visual-language-evolution/` 下四份规划、方向、动效与 Open Design brief
  - **Directions**：候选为“纸上留白 / 私人馆藏 / 生命剪贴 / 暮色回声”；使用相同首页、记录编辑器、时光轴切片和合成内容比较，不作为四套生产主题
  - **Open Design**：官方下载安装页已请求在 Codex 右侧打开；当前任务未发现 `open-design` MCP，Cloud 生成、account/credits 使用与任何正式原型均未启动
  - **Validation**：9 个必需规划文件、7 Requirements / 11 Scenarios、14 done / 29 pending tasks、尾随空白、credential pattern、`git diff --check` 文件级验证 PASS；OpenSpec CLI 不在 PATH，CLI validation `SKIPPED`
  - **Boundary**：未修改前端业务代码、package/lockfile、backend、Agent、accepted baseline、部署或冻结蓝图；真实用户内容、真实媒体、secret 与 provider/object storage 外调均为 0
  - **Blocked on**：当前任务无法调用 Open Design；需安装/注册完成后在新任务确认 brief，再生成四方向可比原型并由用户锁定主方向
  - **Next step**：完成 T-15b～T-23 并由用户锁定方向；Gate 2 已授权，方向子闸完成后即可按既有授权修改业务代码

- **This session**: 2026-08-29 — **P5.x `time-chapter-foundation` ACCEPTED / ARCHIVED**
  - 复核唯一 active change、clean `main...origin/main`、实现提交、Gate 3 证据、tasks 与 delta 后，确认本轮工程迭代完成
  - 三份 delta exact-copy 接受进 `backend-core`、`miniapp-core` 与 `v2-product-scope` baseline
  - closeout 已写；change 归档至 `openspec/changes/archive/2026-08-29-time-chapter-foundation/`；状态恢复 `IDLE`
  - 物理真机与 OpenSpec CLI 保持 `SKIPPED`；E1/目标用户价值保持 `INCONCLUSIVE`；本机证据不外推为生产 SLA
  - 本轮未 `git add`、commit、push、PR、deploy 或 release

- **This session**: 2026-08-29 — **P5.x `time-chapter-foundation` Gate 3a/3b PASS**
  - **Gate 3a MySQL**：用户以管理员权限启动 MySQL80 后，P5 migration 连续两次 PASS；information_schema 验证 2 表/13 字段、预期索引、3 个 CASCADE 复合/owner FK、CHECK、InnoDB/utf8mb4 exact-match
  - **Real probe**：新增默认关闭的 `P5RealMySqlTimeChapterProbeTest`；真实 owner/unique/FK/transaction rollback、创建/加入/显式转移/结束/重开/删篇章、导出、删记录与 clear-all PASS；最终 synthetic users/records/chapters/relations/operations/artifacts 全为 0
  - **Gate 3b Standard**：微信开发者工具 Stable v2.01.2510290，Standard 以两个独立 automation 会话完成篇章主矩阵和记录归属矩阵；scripted API 只证明 UI 状态机，不冒充真实 backend/provider
  - **Gate 3b Preview**：固定篇章列表/详情与记录归属面板可见；创建、编辑、加入/转移/移出、结束/重开、删除均 fail-closed；两个独立 app-context 计数切片均 `totalRequests=0`、`timeChapterRequests=0`
  - **Final regression**：backend 113 suites / 801 tests / 0 failures / 0 errors / 16 skipped；frontend type-check、Standard/Preview build PASS；`git diff --check`、denylist、Agent diff、credential scan PASS
  - **Cleanup**：临时 `miniprogram-automator` 目录已删除，9420/9421 无监听；MySQL80 保持用户启动后的运行状态；真实 provider、对象存储与外部研究调用为 0
  - **Evidence boundary**：物理真机 `SKIPPED`；OpenSpec CLI 不在 PATH；E1/目标用户价值仍 `INCONCLUSIVE`；开发者工具与工程 PASS 不外推为真机、用户价值或生产 SLA
  - **Commit**：`f4a088f feat(time-chapter): 完成P5时间篇章基础`；未 push
  - **Next**：T-69 等待用户 review；T-70～T-72 仍需 delta acceptance/archive 单独授权

- **This session**: 2026-08-29 — **P5.x `time-chapter-foundation` Gate 3a/3b AUTHORIZED / ENVIRONMENT BLOCKED**
  - **Authorization**：用户明确授权 Gate 3 与 Git；按既有提交责任解释为 Agent 本地 commit，不含 push/PR/deploy/release，也不包含 delta acceptance 或 archive
  - **Preflight**：OpenSpec CLI 仍不在 PATH；本机 MySQL 8.0.41 与微信开发者工具均已定位，8080/3306/9420/9421 初始均未监听
  - **Environment blocker**：`MySQL80` 已安装但停止；`Start-Service` 与 `net start` 均因管理员权限被拒，直接运行 `mysqld` 因 Data 目录 `OS errno 13` 退出，因此真实 migration、InnoDB 探针和依赖 backend 的 Standard 矩阵尚未执行，不能写为 PASS
  - **Contract fix**：Gate 3 schema exact-match 预检发现并补齐 `time_chapter(user_id,created_at,id)` 索引，MySQL migration、完整 schema、H2 schema 与契约测试已同步；`P5SchemaContractTest` PASS
  - **Evidence boundary**：真实 MySQL/Standard 仍 pending；物理真机无已连接可控设备，`SKIPPED`；Agent provider、对象存储、外部研究调用均为 0
  - **Commit**：pending；待 Gate 3 完成后精确暂存并本地提交，不 push
  - **Next**：由用户在管理员 PowerShell 执行 `Start-Service MySQL80`，确认 3306 已监听后继续 T-63/T-64/T-66；不得以 H2/build/开发者工具 readiness 替代真实结果

- **This session**: 2026-08-28 — **P5.x `time-chapter-foundation` Gate 2 IMPLEMENTATION AUTHORIZED**
  - **Authorization**：用户明确批准按 `tasks.md` 修改业务代码；Gate 3 真实 MySQL、微信开发者工具、物理真机、delta acceptance、归档、push、PR、deploy、release 均未授权
  - **Implementation allowlist**：P5.x MySQL/H2 schema；time chapter domain/mapper/service/controller/DTO/VO/tests；record query VO 的 nullable chapter summary；P3.2 export/delete integration；frontend chapter types/service/store/pages/components、record-list/detail 局部入口、pages.json；本 change、ACTIVE_TASK、append-only AGENT_LOG
  - **Implementation denylist**：真实 MySQL/微信/真机/对象存储/provider/research；delta acceptance、archive、push/PR/deploy/release；package/lockfile、deployment、monitoring、admin；Agent runtime/tool/prompt/memory/safety/provider；record 正文/位置/附件/封面/时间与 SEALED/UNLOCKED 规则；第四一级 Tab、自动归入、AI 命名/摘要/封面、目标/进度/结果/评分
  - **Baseline**：backend focused 6 suites / 82 tests / 0 failures / 0 errors / 0 skipped；实现后 full 111 suites / 797 tests / 0 failures / 0 errors / 15 skipped（本地 H2/default-gated tests）；frontend `vue-tsc --noEmit`、Standard/Preview `mp-weixin` build PASS
  - **Scope check**：T-17 PASS；package/lockfile、deployment、monitoring、admin、Agent runtime/prompt/tool/memory 无 diff；前端 bundled `pnpm` 因会删除重装现有 node_modules 未执行，使用已存在 `.bin` 直接验证
  - **Verification boundary**：仅执行本地离线/H2、编译、构建、静态范围/隐私检查；真实依赖与真实交互按用户授权保持 SKIPPED
  - **Commit**：pending；沿用 Agent 本地提交责任，不 push
  - **Current result**：T-18～T-61b 已完成 Gate 2 离线实现、代码审查与修复；复合 owner FK、全局 chapter→record 锁顺序、并发 409、Preview 会话互斥、分页续载、endedAt 与冲突刷新均已补齐；真实 InnoDB race/deadlock、真实 Preview request count 仍待 Gate 3 授权；T-62～T-73 保持未执行
  - **Final verification**：审查修复 focused 6 suites / 25 tests / 0 failures / 0 errors / 0 skipped；full 112 suites / 800 tests / 0 failures / 0 errors / 15 skipped；frontend type-check、Standard/Preview build 与 `git diff --check` PASS
  - **Remaining boundary**：OpenSpec CLI、真实 MySQL/微信开发者工具/物理真机、真实工具 request count、delta acceptance、archive、push、PR、deploy、release 均保持 SKIPPED/未执行

- **This session**: 2026-08-28 — **P5.x `time-chapter-foundation` Gate 1 PLANNING READY**
  - **Product governance**：真实参与者仍为 0，E1 保持 `INCONCLUSIVE`；产品负责人显式豁免正证据进入条件，决定先完成最小产品、再以未来真实使用优化
  - **Planning artifacts**：proposal、design、tasks 与 backend-core/miniapp-core/v2-product-scope 三份 delta 已创建
  - **Recommended contract**：独立 chapter/relation 表、第一版 record 唯一主篇章、ACTIVE/ENDED、expectedVersion、显式 source transfer、原子批量、coverage 查询聚合、删除容器不删记录、导出/clear-all 集成
  - **Frontend boundary**：“我的记录”内记录/篇章二级切换、独立详情、次级归属入口；三个一级 Tab 保持；Preview 固定合成且 mutation fail-closed
  - **Agent/privacy**：Agent 完全解耦，provider/object storage/research 调用预算 0；日志不记录 name/note/title/content/location/media key/URL
  - **Validation**：文件级 6 artifacts、12 Requirements / 40 Scenarios、决策 1–12、scope/privacy 与 `git diff --check` PASS；OpenSpec CLI 不在 PATH，CLI validation SKIPPED
  - **Authorization**：Gate 1 已于 2026-08-28 获批，包含 proposal/design/tasks、三份 delta、决策 1–12、名称 100 字、自述 1000 字、单批 100 条、expectedVersion 与 fromChapterId 显式转移；Gate 2/3、业务代码、真实 MySQL/微信、delta acceptance、archive、push/PR/deploy/release 均未授权
  - **Planning commit**：`4454beb docs(openspec): 规划P5时间篇章基础`（未 push）
  - **Next step**：等待用户单独授予 Gate 2；获批前不得运行实现 baseline 或修改业务代码

- **This session**: 2026-08-28 — **P4.2 与 R1 收口，ACTIVE_TASK 恢复 IDLE**
  - **Authorization**：用户明确授予 Gate 1–3，允许完成当前 P4.2 收尾，并规划、实现、验证与归档下一阶段 R1；commit/push/PR/deploy/release 未获独立授权
  - P4.2 ACCEPTED / ARCHIVED：五域 23 Requirements / 64 Scenarios exact-copy 进入 baseline；既有 Gate 3a MySQL 与 Gate 3b 微信开发者工具证据保持原边界
  - R1 Gate 1：完成官方地区资源复核、proposal/design/tasks 与五域 delta；决策为当前输入、高精度、确定性本地抢占、不诊断、不人工接管
  - R1 Gate 2：新增封闭 safety policy/decision/rule、本地固定响应、provider/memory/tool/material/source 零调用分支、无文本 trace 与 C6 固定场景
  - R1 Gate 3：最终 backend 108 suites / 782 tests / 0 failures / 0 errors / 15 skipped；frontend type-check、Standard/Preview build PASS
  - R1 provider 边界：安全正例本地抢占，普通固定合成边界 provider calls=1；本刀真实 provider 总调用 1，不外推为真实危机效果或生产 SLA
  - R1 ACCEPTED / ARCHIVED：五域 9 Requirements / 14 Scenarios exact-copy 进入 baseline；`closeout.md` 保留物理真机、临床效果、跨语言漏报与资源时效风险
  - OpenSpec CLI 不在 PATH，CLI validation SKIPPED；使用 artifacts/tasks/delta/Requirement/Scenario/exact-copy 文件级验证
  - **Commit**：`4ed09a7 feat(agent): 完成P4.2与R1验收归档`；未 push/PR/deploy/release
  - **Next step**：当前 IDLE。冻结序列下一项 E1 仍须重新规划与授权，本轮 Gate 1–3 不继承

- **This session**: 2026-08-27 — **P4.2 Gate 3a/3b 真实验收检查点**
  - **Authorization**：用户明确授权 Gate 3a 与 Gate 3b；范围不含真实 provider、delta acceptance、archive、stage/commit、push、PR、deploy/release
  - Gate 3a PASS：本机 MySQL 8.0.41 preflight 正常；`p42-memory-agency.sql` 连续执行两次 PASS；schema、default、FK `SET NULL` exact-match
  - Gate 3a synthetic probe PASS：session authorization default/off/on/revoke、owner scope、status/exclusion/deletion eligibility、runtime future-turn effect、source resolution、删除后 unavailable、assistant/source 同事务 rollback 全部通过
  - Gate 3a cleanup PASS：finally 后 synthetic user/record/session/message/source/operation 聚合均为 0；首次增强探针因合成 title 未命中完整 cue 而失败，cleanup 仍执行，修正 fixture 后通过
  - Gate 3b Developer Tools Standard PASS：真实微信登录边界；默认关闭、开启/关闭、失败保持原状态、来源 available/unavailable、来源跳转、record policy/note、删除入口均通过；业务响应为 scripted interception，provider 调用 0
  - Gate 3b Developer Tools Preview PASS：entry=true、sheet=false；app-context 计数 total requests=0、memory-agency requests=0；首次断言错误地要求 Preview 隐藏本地 policy，按实际契约改为验证交互不发请求后通过
  - Gate 3b evidence boundary：物理真机 SKIPPED；开发者工具 PASS 不外推为真机 PASS，scripted response 不冒充真实 provider 或真实语言质量
  - 收口回归：backend 106 suites / 756 tests / 0 failures / 0 errors / 14 skipped；新增真实 MySQL probe 默认关闭；frontend Standard/Preview build PASS
  - 环境 cleanup：验收 backend 与微信开发者工具均已停止，8080 不再监听；repo 临时 pnpm store 与仓外 automator 临时目录已移除
  - OpenSpec CLI 不在 PATH，CLI validation SKIPPED；继续采用文件级 artifact/delta/task 检查
  - **Commit**：pending；未 stage/commit/push/PR/deploy/release
  - **Blocked on**：无 Gate 3a/3b 阻断；T-53 仍为 PARTIAL，物理真机仍为 SKIPPED
  - **Next step**：用户审查 Gate 3 证据与 remaining risks；如需继续，须单独授权 delta acceptance/archive，commit 仍需单独授权

- **This session**: 2026-08-27 — **P4.2 Gate 2 离线实现审查点**
  - backend：完成 session 级默认关闭授权、config/consent 双闸、record 排除与用户语境说明、实际来源关系及 owner/status/delete fail-closed 解析；授权切换与 source 写入均检查受影响行数
  - runtime：REVIEW_CHAT 目标与跨记录来源分层；Prompt 与 source row 共享同一 final injected list；撤销授权后下一轮不再跨记录检索
  - frontend：完成 Agent 授权控件、record policy/note、来源 chip 与 Preview fail-closed；修复回看浮层 `scroll-view` 标签闭合错误，并让 policy 保存失败恢复后端权威状态
  - C6/直接测试：增加 config×consent、撤销、排除、回看目标、实际 source、owner/status/privacy 等覆盖；合法快照变化带 P4.2 baselineNote
  - backend full：105 suites / 755 tests / 0 failures / 0 errors / 13 skipped；frontend type-check、Standard/Preview `mp-weixin` build PASS；`git diff --check` 与范围检查 PASS
  - OpenSpec CLI 不在 PATH，CLI validation SKIPPED；采用 8 artifacts/五域 delta/Requirement/Scenario 文件级检查
  - **Evidence boundary**：接手时实现改动已存在，无法倒签实现前 backend/frontend baseline；真实 MySQL migration/rollback/SET NULL、微信请求计数与物理真机均未执行；真实 provider 调用 0
  - **Remaining Gate 2 gap**：T-53 的 revoke/delete/owner/status 有直接测试、transaction 有结构检查，但尚未全部提升为 C6 fixed fixtures；T-52 真实 Preview request=0 留待 Gate 3b
  - **Commit**：pending；用户明确本轮不 commit，未 stage/push/PR/deploy/release
  - **Blocked on**：无实现阻断；等待用户审查 diff，并决定是否继续补齐 T-53 或分别授权 Gate 3a/Gate 3b
  - **Next step**：优先补齐 T-53 fixed fixture matrix；之后如需真实验收，Gate 3a MySQL 与 Gate 3b 微信分别授权，不得直接接受 delta 或归档

- **This session**: 2026-08-25 — **P4.2 GATE 1 PLANNING READY**
  - 微信登录报错已先行排查并恢复：根因为本机 backend 未运行/8080 无监听；启动 ignored 本地脚本后真实微信登录与首页数据链路 PASS，不是登录业务代码缺陷
  - P4.1 已完成 Gate 3、delta acceptance、归档与本地提交；HEAD=`42548ce`，P4.2 开工时工作树 clean
  - P4.2 硬依赖 P4.1/C3/C9 已满足；当前事实为 config=true + cue 即自动跨记录检索，session/record 无用户授权/排除字段，trace 无用户可见实际来源
  - 已创建 proposal、design、tasks 与 agent-runtime/backend-core/miniapp-core/v2-product-scope/agent-collaboration 五份 delta
  - 推荐决策：session 授权默认 false；config 与 consent 取 AND；回看目标与其他历史分层；record exclusion + 用户 context note；per-assistant-message 结构化 source 关系且不复制内容；删除后 SET NULL/unavailable；message/source 同主事务；旧消息不回填；provider 预算 0
  - 文件级结构、operation、Requirement/Scenario、link/scope/privacy 与 `git diff --check` PASS；OpenSpec CLI 不在 PATH，CLI validation SKIPPED
  - **Authorization**：仅 Gate 1 规划；Gate 2/3、真实 MySQL/微信、delta acceptance、archive、commit/push/deploy/release 均未授权
  - **Blocked on**：用户审查并批准/修改 design D1–D12 与五域 delta
  - **Next step**：Gate 1 获批后仍须单独取得 Gate 2，才可按 tasks TDD 实现

- **This session**: 2026-08-25 — **P4.1 ACCEPTED / ARCHIVED；ACTIVE_TASK=IDLE**
  - 微信登录报错定位为本机 backend 未运行、8080 无监听；使用 ignored 本地启动脚本恢复后，真实微信登录链路 PASS
  - Gate 3b：automation 正确分离 IDE `--port 9421` 与 `--auto-port 9420`，使用 IPv6 `ws://[::1]:9420`；Standard/Preview 开发者工具矩阵 PASS，新增 provider 调用 0；物理真机 SKIPPED
  - Gate 3c：MySQL 8.0.41 migration 连续两次、schema/恢复/switch/review-null/owner-scope 合成探针 PASS；finally cleanup 后合成数据为 0
  - 收口回归：backend 104 suites / 728 tests / 0 failures / 0 errors / 13 skipped；frontend type-check、standard/Preview build PASS；`git diff --check` PASS
  - 五份 delta 已接受进 baseline并 exact-copy PASS；closeout 已写；change 归档至 `openspec/changes/archive/2026-08-25-witness-agent-alignment/`
  - 真实 provider 总调用保持 8 次硬上限；tracked evidence 不含用户/模型文本、token、openid、prompt 或 secret
  - **Commit**：`357521c test(agent): 完成P4.1真实验收并归档`（未 push）
  - **Blocked on**：none
  - **Next step**：若继续冻结序列，只能从 IDLE 新建 P4.2 独立规划 change；不得直接实现

- **This session**: 2026-08-25 — **P4.1 Gate 3b/3c retry 已授权，等待本机交互解阻**
  - 用户明确授权完成上一轮列出的 P4.1 收口任务；范围继续包含 Gate 3b retry、Gate 3c、delta acceptance、closeout、归档与 Agent 本地提交，不含 push、PR、部署或发布
  - Gate 3c：普通、提升沙箱与 UAC 管理员 PowerShell 启动均未使 MySQL80 离开 `Stopped`；3306 未监听，preflight/migration/合成会话仍未执行，数据库读取/写入/改动 0
  - Gate 3b：微信开发者工具 9420 IDE server 已成功监听；automation 因开发者工具登录过期返回 `code 10`，官方二维码登录会话已启动并等待用户扫码，standard / Preview UI 矩阵尚未执行
  - 本轮未调用真实 Agent provider，Gate 3a 已用尽的 8-call 预算保持不变；未读取或记录用户/模型/数据库内容、prompt、账号、二维码内容或 secret
  - **Commit**：`dd49600 docs(agent): 记录P4.1真实依赖重试阻断`（active checkpoint；未 push）
  - **Blocked on**：用户扫码登录微信开发者工具；用户在管理员 PowerShell 中成功执行 `Start-Service MySQL80`
  - **Next step**：确认微信已登录且 MySQL80/3306 已运行后，继续 Gate 3b/3c；两项通过前不得接受 delta 或归档

- **This session**: 2026-08-17 — **P4.1 Gate 3a PASS；Gate 3b FAIL；Gate 3c BLOCKED**
  - Gate 3a：2 canary + 6 fixed synthetic scenarios，真实 provider 8 calls / 0 reflection / hard max 8；问题上限、长度与关系/结论 marker 自动边界 PASS
  - 结构化人评：witness role、user control、question restraint、brief restraint、uncertainty、no conclusion 六项均 PASS；仓库不保存合成输入或模型回复
  - Gate 3b：微信 CLI 成功开启 9420 automation，但客户端首次连接 `ws://127.0.0.1:9420` 失败；依照本轮 fail-stop 约定未改 endpoint 重试，standard / Preview UI 矩阵未执行
  - Gate 3c：MySQL80 为 Stopped，系统拒绝启动服务，127.0.0.1:3306 未监听；preflight/migration/合成会话均未执行，数据库改动 0
  - checkpoint 回归：backend 103 suites / 727 tests / 0 failures / 0 errors / 12 skipped；frontend type-check、standard / Preview mp-weixin build PASS；`git diff --check` PASS
  - 新增第 12 个 skipped 为默认关闭的 `P41RealProviderProbeTest`；真实 provider 已在显式 Gate 3a 命令中单独 8/8 PASS，不把默认 skip 写成真实证据
  - delta acceptance / closeout / archive：未执行；不能把部分 Gate 3 证据写成 P4.1 完成
  - **Commit**：`09304a1 feat(agent): 实现P4.1见证者对齐并记录验收状态`（active checkpoint；未 push）
  - **Blocked on**：用户重新授权 Gate 3b retry；用户在管理员权限下启动 MySQL80 后重新授权 Gate 3c
  - **Next step**：等待用户重新授权 Gate 3b retry；用户在管理员权限下启动 MySQL80 后重新授权 Gate 3c；保持 P4.1 active

- **This session**: 2026-08-17 — **P4.1 Gate 3 / acceptance / archive / local commit 已授权**
  - 用户明确要求完成上一轮列出的 P4.1 收口工作并记得提交 commit；授权范围据此限定为 Gate 3a 固定合成真实 provider（总调用 <=8）、Gate 3b 微信开发者工具/真机、Gate 3c 真实 MySQL、delta acceptance、归档与 Agent 本地提交
  - push、PR、部署、发布仍未授权；真实样本仅使用合成内容，证据不记录 prompt、用户/模型文本、会话内容或 secret
  - **Commit**：pending（Agent commit；不 push）
  - **Blocked on**：none
  - **Next step**：按 3a/3c/3b 分支分别执行；任一真实调用或 migration 失败即停止该分支，不自动 retry/cleanup/切换配置

- **This session**: 2026-08-12 — **P4.1 Gate 2 离线实现完成，等待用户审查 / Gate 3**
  - backend：新增 `LISTEN|UNTANGLE` session intent、WITNESS 编排、owner-scoped intent switch、legacy ACTIVE 写作会话归一、typed 0/1 question policy、一次 reflection/fallback 与结构化 trace；REVIEW_CHAT intent 从数据库重读仍保持 null
  - Prompt：角色从“朋友”收敛为有温度的见证者，移除固定四阶段目标/默认追问；LISTEN 与极短输入 0 问，UNTANGLE 正常输入至多 1 问，停止/轮次上限 0 问
  - frontend：既有入口先显示两项同权选择，选择前不 start；会话内后端权威切换、失败不乐观更新；移除阶段旅程标题并保持 Preview fail-closed
  - C6：新增 intent/短答/question-limit 固定合成用例；硬不变量逐 turn 校验 witness policy 与 question limit；预期 baseline 变化逐项写 `P4.1 witness-agent-alignment:` note 并同步 checksum
  - 最终 backend full：102 suites / 726 tests / 0 failures / 0 errors / 11 skipped；frontend type-check、standard/Preview mp-weixin build PASS；`git diff --check`、allowlist、package/lockfile、credential/privacy 增量扫描 PASS
  - OpenSpec CLI 不在 PATH，CLI validation SKIPPED；真实 provider、真实 MySQL migration、微信开发者工具/真机均未执行，scripted/build 不证明自然度或真实依赖
  - T-16 保留 PARTIAL（实现前未单独留 standard/Preview baseline）；T-55 保留 SKIPPED（微信 textarea/keyboard/scroll/ended/error 交互待 Gate 3b）
  - **Commit**：pending（默认用户手动提交；未 stage/commit）
  - **Blocked on**：none
  - **Next step**：用户审查实现 diff 与分层证据；如继续，分别授权 Gate 3a provider、Gate 3b 微信、Gate 3c MySQL；不得直接接受 delta 或归档

- **This session**: 2026-08-12 — **P4.1 readiness GO，Gate 1 规划工件已创建**
  - 用户独立授权进入 P4.1 `witness-agent-alignment` 规划；准入确认 `ACTIVE_TASK` 原为 IDLE、工作树 clean、HEAD=`0aea558`
  - P4.1 硬依赖 P3.1 `present-moment-capture` 与 C6 `agent-eval-framework` 均已归档/接受；同名 active change 原先不存在
  - 当前事实：Prompt 仍自称“一个朋友”；WRITING_GUIDANCE 固定推进 EMOTION → CONFUSION → CORE_QUESTION → EXPECTATION；极短输入允许同阶段再问一次；前端标题暴露阶段旅程
  - 已创建 proposal、design、tasks 与 agent-runtime / backend-core / miniapp-core / v2-product-scope / agent-collaboration 五份 delta
  - 推荐主契约：`LISTEN|UNTANGLE` 会话意图、单一 `WITNESS` 新会话阶段、LISTEN 0 问题、UNTANGLE 至多 1 问、极短输入 0 问、可随时结束、typed question enforcement + 一次 reflection
  - 兼容边界：旧客户端缺省 LISTEN；旧 active 写作会话归一为 LISTEN/WITNESS；历史 message/trace stage 不回写；REVIEW_CHAT 无写作 intent
  - C6 baseline 变化必须使用 `P4.1 witness-agent-alignment:` baselineNote；scripted PASS 不证明语言质量
  - 规划期外部调用 0；OpenSpec CLI 不在 PATH，文件级 scaffold 与 delta title/operation/Scenario 校验 PASS，CLI validation `SKIPPED`
  - **Commit**：pending（默认用户手动提交）
  - **Blocked on**：用户 Gate 1 审查与批准 design 决策 1–12
  - **Next step**：用户批准或修改 exact enum/API/schema/migration、0/1 问题上限和 Gate 3a 预算；未获 Gate 2 前不改业务代码

- **This session**: 2026-08-12 — **P4.1 Gate 1 / Gate 2 获批，离线实现启动**
  - 用户明确“批准决策，可以进入实现”：批准 proposal、design、tasks、五份 delta 与设计决策 1–12，并授权按 tasks 修改业务代码
  - 授权不包含真实 provider、真实 MySQL、微信开发者工具/真机、delta acceptance、归档、stage/commit、push、PR、部署或发布
  - 实施顺序：intent/session contract → witness orchestration → Prompt/question enforcement/trace → Mini Program → C6/full regression
  - **Commit**：pending（默认用户手动提交）
  - **Blocked on**：none
  - **Next step**：完成 backend/frontend baseline 后按 allowlist 实现；Gate 3a/3b/3c 保持关闭

- **This session**: 2026-08-12 — **P3.2 delta accepted、closeout 与归档完成**
  - 用户明确批准归档当前阶段；五份纯 ADDED delta 接受进 baseline：backend-core 7、miniapp-core 4、v2-product-scope 3、agent-runtime 3、agent-collaboration 3，共 20 Requirements / 51 Scenarios
  - 新增 `closeout.md`，如实分层记录 H2/build、真实 MySQL、私有对象存储、微信开发者工具与生产 SLA 边界
  - change 归档至 `openspec/changes/archive/2026-08-12-data-ownership-foundation/`；`ACTIVE_TASK` 回到 `IDLE`
  - delta exact-copy、新 requirement 标题唯一性、archive 结构、相对链接、隐私/credential 与 `git diff --check` 文件级校验 PASS
  - **PARTIAL / SKIPPED**：T-36 不可压缩真实媒体峰值保持未勾选；物理真机与 OpenSpec CLI 未验证；真实 Agent provider 调用 0
  - **Commit**：`a24f550 feat: 接受并归档P3.2数据所有权基础`（不 push）
  - **Blocked on**：none
  - **Next step**：等待用户独立授权 P4.1 规划；不默认启动

- **This session**: 2026-08-12 — **P3.2 Gate 3a/3b/3c 真实依赖验收 PASS，等待用户审查**
  - 用户明确授权 Gate 3a/3b/3c：允许真实 MySQL preflight/migration/删除恢复、私有对象存储合成媒体导出删除清理、微信开发者工具/真机交互矩阵
  - 仅使用合成内容和可清理对象；证据只记录 schema、状态、计数、hash 与结果，不记录日记原文、位置、storage key、URL、token 或 secret
  - 真实 Agent provider 预算继续为 0；push、PR、部署、发布、delta acceptance、closeout 与归档未授权
  - Gate 3a PASS：MySQL 8.0.41；preflight 仅含 schema/状态/孤儿聚合，关联 orphan/owner mismatch 均为 0；migration 连续两次成功
  - Gate 3a 真实探针：单条删除、cross-owner 拒绝、clear-all 固定快照、mutation freeze、record-linked cascade、RUNNING 中断恢复均 PASS；postflight 2 tables / 4 FKs / 9 indexes，合成 user/record/operation/item 均为 0
  - Gate 3b PASS：真实私有 S3-compatible 合成 PNG/WAV 导出，ZIP 中原始 bytes 与 manifest SHA-256 一致；真实读取鉴权失败进入 RETRY_REQUIRED，恢复后成功
  - Gate 3b 删除覆盖 success、not-found + stale restart、鉴权失败重试；24h artifact 到期路径验证为 EXPIRED；合成对象、artifact、user/record/operation 最终全部清理
  - Gate 3c standard PASS：微信开发者工具中写入冻结与 RETRY_REQUIRED 重试、默认 `RESPECT_SEAL` / 显式 `FULL_CONTENT` 导出、DRAFT/SAVED/SEALED/UNLOCKED 单删、clear-all 强确认逐项通过
  - Gate 3c 文件交付 PASS：通过微信 `wx.saveFile` 保存真实 ZIP，saved file 大小大于 0，验收后移除；不是桌面下载或 build 替代
  - Gate 3c Preview PASS：页面显示 1/2/2/1 演示计数；导出、单删、clear-all 均 fail-closed，saved file 数不变；同一验收时间窗真实 MySQL 新增 operation 为 0
  - Gate 3c cleanup PASS：2 个导出 artifact、合成账号、record 与 operation 全部清理；真实 MySQL 聚合归零
  - Gate 3 后全量回归：backend 99 suites / 703 tests / 0 failures / 0 errors / 11 skipped（2 个真实探针默认 skip）；frontend type-check、standard/Preview build PASS
  - 工具诊断：开发者工具从 standard 切换 Preview 后需清理 compile/file cache 才加载新构建；刷新后页面路径与只读矩阵 PASS，未修改业务代码
  - **Commit**：`907feb4 test: 完成P3.2真实依赖验收`（Gate 3 evidence checkpoint；不 push）
  - **Blocked on**：none
  - **Next step**：等待用户审查 Gate 3 证据与 remaining risks；未授权 delta acceptance、closeout 或归档

- **This session**: 2026-08-12 — **P3.2 Gate 2 离线实现完成，等待 Gate 3 / 用户审查**
  - 用户明确“授权实现，并且可以为我提交commit”：批准原规划与五份 delta，授权按 tasks 修改业务代码并由 Agent commit
  - 授权不包含真实 MySQL、对象存储、微信副作用验收、真实 Agent provider、push、部署或发布
  - 已实现 durable owner-scoped operation、数据库级 active operation 唯一约束、短期 hash 确认、expected-state 状态迁移、stale operation 恢复与 24h artifact 清理
  - 已实现 `RESPECT_SEAL` / `FULL_CONTENT` 离线 ZIP：records/media/agent 物理分区、manifest bytes/SHA-256、无 CDN/fetch index、私有 owner-scoped 下载
  - 已实现任意状态单删/clear-all：远端对象 success/not-found 后 DB cascade，provider failure 保留 item 重试锚点，owner mismatch fail-closed；旧 DELETE 不再直删
  - clear-all 期间普通 record list/timeline/detail 隐藏目标，record/attachment/Agent mutation 后端冻结；前端有辅助拦截但以后端为权威
  - Mini Program 个人中心已注册“数据与所有权”；移除假 iCloud/自动备份/恢复/固定数量，接入 summary/export/status/download/retry/强确认；详情四状态删除与草稿放弃统一 operation；Preview fail-closed
  - 最终离线验证：backend 97 suites / 701 tests / 0 failures / 0 errors / 9 skipped；frontend type-check、standard/Preview mp-weixin build PASS；`git diff --check` 与范围/隐私扫描 PASS
  - 合成边界 PARTIAL：18 个媒体 / 301,989,888 logical bytes，artifact 299,156 bytes / 963ms；样本高度可压缩且未测构建过程磁盘峰值，不代表真实媒体性能或生产 SLA
  - **SKIPPED**：OpenSpec CLI 不在 PATH；真实 MySQL migration/锁语义、真实对象存储读删/失败恢复、微信文件保存/完整交互均待 Gate 3a/3b/3c
  - **Commit**：`7b48086 feat: 实现P3.2数据所有权基础`（implementation checkpoint；不 push）
  - **Blocked on**：none
  - **Next step**：提交实现 checkpoint 后等待用户审查；如继续，分别授权 Gate 3a/3b/3c，不得直接接受 delta 或归档

- **This session**: 2026-08-12 — **P3.2 readiness GO，Gate 1 规划工件已创建**
  - 只读准入：P3.1 已归档、`ACTIVE_TASK=IDLE`、工作树 clean、HEAD=`efd2618`；冻结蓝图下一候选为 P3.2
  - `openspec/changes/` 中 M1/M3 为早期历史工件，不是当前 active 指针；本轮将 P3.2 设为唯一 active change
  - 当前事实：任意状态导出/删除/clear-all 均未实现；现有 DELETE 仅限 DRAFT 且直接删 DB，不能证明远端对象清理；假 data-backup 页面文件存在但已从真实路由移除
  - 已创建 proposal、design、tasks 与 backend-core / miniapp-core / v2-product-scope / agent-runtime / agent-collaboration 五份 delta
  - 推荐主契约：durable owner-scoped operation；`RESPECT_SEAL` 默认 / `FULL_CONTENT` 显式选择；远端对象 success/not-found 后再删 DB；clear-all 确认后冻结 record mutation；导出临时 ZIP 默认 24h
  - planning 外部调用 0、真实 Agent provider 预算 0；OpenSpec CLI 未安装，`openspec new change` 失败，文件级 scaffold 完成，CLI validation `SKIPPED`
  - **Commit**：pending（默认用户手动提交）
  - **Blocked on**：用户 Gate 1 审查与批准
  - **Next step**：用户批准/修改 design 决策 1–12 与五份 delta；未获 Gate 2 前不改业务代码

- **This session**: 2026-08-12 — **P3.1 delta accepted、closeout 与归档完成**
  - 用户明确批准归档当前阶段；T-82～T-86 完成，四份 delta 按 requirement 精确接受进 baseline：backend-core 6 MODIFIED + 3 ADDED、miniapp-core 6 MODIFIED + 5 ADDED、agent-runtime 2 MODIFIED + 1 ADDED、v2-product-scope 5 ADDED
  - 新增 `closeout.md`，如实保留 E0 `INCONCLUSIVE / SKIPPED`、真实 Agent provider 0 调用、OpenSpec CLI SKIPPED，以及本地 MySQL/对象存储/微信证据不等于生产 SLA 的边界
  - change 归档至 `openspec/changes/archive/2026-08-12-present-moment-capture/`；`ACTIVE_TASK` 回到 `IDLE`
  - 文件级校验：4 specs / 28 Requirements / 98 Scenarios；delta exact-copy PASS，未引入新的重复 requirement 标题；所有 86 个任务均完成
  - **Commit**：`5f8b623 feat: 接受并归档P3.1当下记录`（不 push）
  - **Blocked on**: none
  - **Next step**：若进入 P3.2 `data-ownership-foundation`，必须重新获得独立规划授权；push/deploy/release 仍须单独授权

- **This session**: 2026-08-12 — **P3.1 Gate 3c 人工矩阵完成，Agent mock 原因确认**
  - 用户在微信端报告“目前仅有 Agent 对话是用不了的，其他都正常”；结合上一轮明确的 8 项清单，登记文字/图片/声音独立保存、恢复、SAVED 编辑、保存后交给时间、权限拒绝与上传失败/重试为人工 PASS，T-79 完成
  - 截图中的 Agent 文案为 C8 固定 opening fail-closed 模板；同期日志确认请求到达后端、WRITING_GUIDANCE 会话创建成功，opening 与用户主动 retry 均为 `provider=mock`、`auth-configuration`、0ms
  - JVM 系统属性确认本轮验收进程由我们主动强制 `app.ai.provider=mock`，因此 Agent 不可用是守住 P3.1 真实 AI 调用 0 的预期结果，不是微信网络、会话状态或 P3.1 Agent 兼容回归
  - 未切换真实 DeepSeek：Gate 3c 授权不自动包含真实 provider 外调；如需恢复/验证 Agent，必须另行授权真实 provider 调用后重启 backend
  - E0 用户理解继续为 `INCONCLUSIVE / SKIPPED`；功能矩阵 PASS 不倒填用户访谈
  - **Commit**：`1d2223e docs: 完成P3.1 Gate 3c验收`（不 push）
  - **Blocked on**: none
  - **Next step**：用户审查全部实现、真实证据与 remaining risks；确认后再接受 delta、写 closeout 并归档

- **This session**: 2026-08-12 — **P3.1 Gate 3c 已授权，等待微信人工矩阵结果**
  - 用户明确批准 Gate 3c；T-78 已完成，T-79 保持未勾选，直到文字/图片/声音独立保存、恢复、SAVED 编辑、保存后封存、权限拒绝与上传失败逐项获得人工结果
  - MySQL80 与 Redis 服务运行；后端在 8080 启动成功，未登录请求返回预期 401，启动日志无 ERROR/Exception
  - 通过运行中 JVM 系统属性确认 `app.ai.provider=mock`，unlock/draft cleanup cron 均为 `-`；真实 AI provider 调用预算继续为 0
  - 微信开发者工具 CLI 已打开 `frontend/dist/dev/mp-weixin` 当前构建并启用 automation；生成物包含“留下这一刻”“这一刻已经留下”“继续上次未完成”“交给时间”和录音权限失败分支
  - 当前仓库/本机没有可调用的 `miniprogram-automator` 客户端；相册/麦克风权限、录音、扬声器播放及真实交互不能由后端测试替代，须用户在开发者工具或真机操作
  - E0 用户理解继续为 `INCONCLUSIVE / SKIPPED`，即使 T-79 功能全通过也不倒填用户访谈 PASS
  - **Commit**：`087d22e docs: 启动P3.1 Gate 3c人工验收`（不 push）
  - **Blocked on**: 用户完成 Gate 3c 人工矩阵并报告逐项结果
  - **Next step**：保持本地后端运行，用户按最短清单操作；收到结果后读取同一启动日志、登记 T-79 与 remaining risks

- **This session**: 2026-08-12 — **P3.1 Gate 3b 真实对象存储验收完成**
  - 用户明确授权使用本地配置的私有对象存储执行可清理的合成图片和短音频探针；未扩大到 Gate 3c、真实 AI provider、push/deploy/release、delta acceptance 或归档
  - 配置 preflight PASS：ignored 本地启动脚本与 secret 文件提供有效 S3-compatible 私有存储配置；只核对存在性/非空性，没有输出或写入 credential
  - 新增显式 `P31_STORAGE_PROBE=1` 门控的真实探针；默认关闭时跳过且不启动外部调用，执行时使用真实 MySQL/对象存储并强制 AI mock
  - 合成 PNG：authorize/upload/commit AVAILABLE/save/private read 字节一致/SAVED edit/删除 PASS；固定短 WAV：upload/commit AVAILABLE/save/private read 字节一致/标准音频解码/删除 PASS
  - fail-closed：仅 upload authorization 的 pending 媒体和 missing object commit 均不能 save；过期 DRAFT 的远端删除成功、对象已不存在幂等清理、模拟鉴权失败 RETRY/恢复后 DELETED 均 PASS
  - 真实探针 **1 test / 0 failures / 0 errors / 0 skipped**；输出 `image/voice/pending/missing/cleanupSuccess/cleanupAbsent/cleanupRetry=true`，随后探针对象全部不存在，合成 user/record/attachment 聚合均为 0
  - 默认 focused **19 tests / 0 failures / 0 errors / 1 skipped**；默认 backend full **92 suites / 688 tests / 0 failures / 0 errors / 9 skipped**；真实 AI provider 调用 0
  - 微信扬声器实际播放、相册/麦克风权限、上传失败 UI、恢复/SAVED 编辑/封存完整矩阵仍属 Gate 3c；E0 用户理解仍为 `INCONCLUSIVE / SKIPPED`
  - OpenSpec CLI 仍不在 PATH，validation 记 SKIPPED；只同步 active change artifacts 与任务状态
  - **Commit**：`375d2c6 test: 完成P3.1真实对象存储验收`（不 push）
  - **Blocked on**: none
  - **Next step**：用户审查 Gate 3b 证据；如继续微信开发者工具/真机完整矩阵，需单独批准 Gate 3c

- **This session**: 2026-08-12 — **微信开发者工具窄验证登记 + Gate 3b readiness**
  - 用户确认 Gate 3a 后微信开发者工具“使用正常”；登记为页面访问/数据同步恢复的人工证据，不扩写为 Gate 3c 全矩阵 PASS
  - T-78/T-79 继续未勾选：文字-only、图片-only、声音-only、恢复草稿、SAVED 编辑、保存后封存、权限拒绝和上传失败尚未逐项报告
  - Gate 3b 零外调 readiness：provider-neutral Qiniu/S3 实现、私有访问契约、上传授权/commit/delete 路径与离线测试均存在；当前 Codex 进程未发现 Qiniu 或 S3 必填环境变量，且没有现成 P3.1 真实存储 probe
  - 未连接对象存储、未读取/输出 AK/SK、未创建合成对象或数据库记录；真实 AI provider 调用 0
  - **Commit**：`f3ee925 docs: 记录P3.1微信验证与Gate3b准备度`（不 push）
  - **Blocked on**: Gate 3b 需要用户明确授权；执行时还需要在启动后端的同一环境中提供可用的私有对象存储本地配置
  - **Next step**：用户明确批准 Gate 3b 后，先做 provider 配置 presence/可用性 preflight，再以合成图片和短音频执行可清理探针；Gate 3c 仍独立

- **This session**: 2026-08-12 — **P3.1 Gate 3a 真实 MySQL preflight 与迁移完成**
  - 用户明确批准 Gate 3a；范围仅含本机真实 MySQL 聚合审计、DDL/迁移、迁移后 backend 读取验证与证据更新，不含对象存储、微信真机、provider、push、部署、发布、delta acceptance 或归档
  - 迁移前后端 8080 未监听；preflight schema 为未迁移状态：`draft_expires_at` 列/索引均不存在、record_type 默认 NODE_RECORD、SAVED=0；数据库 UTC 偏移 +28800 秒，与新加坡 UTC+8 一致
  - preflight 仅输出聚合：3 条 DRAFT 均有有效文字、0 条 owner-scoped AVAILABLE 媒体、0 条空白异常、0 条 AVAILABLE 媒体 orphan/owner mismatch；旧 DRAFT 类型均为 FUTURE_LETTER
  - 执行前发现脚本会在重复运行时顺延 DRAFT expiry；先增加契约断言观察到 1 test FAIL，再将更新收窄为 `draft_expires_at IS NULL`，契约测试转绿后才执行真实迁移
  - 真实迁移 PASS：3 条有效旧 DRAFT -> SAVED，原 FUTURE_LETTER 保留；新增 nullable DATETIME 列和 `(status,draft_expires_at)` 索引，record_type 默认改为 MOMENT；非 DRAFT expiry 异常为 0
  - 幂等重跑 PASS：第二次脚本执行不改变聚合状态；强制索引 EXPLAIN 命中 `idx_record_status_draft_expires`；补偿式回滚仅在停服且确认没有新写入时可执行，本轮无需回滚
  - 迁移后 backend 以 dev + AI mock、unlock/draft cleanup cron 禁用启动；合成 ADMIN 身份的 list/timeline 均为 HTTP 200 / API code 0；诊断进程随后停止，临时日志已清理，真实 provider 调用 0
  - focused 回归：P31 schema 1 + present-moment integration 13 + draft cleanup 4，合计 **18 tests / 0 failures / 0 errors / 0 skipped**
  - OpenSpec CLI 仍不在 PATH，validation 记 SKIPPED；Gate 3b/3c、E0 用户理解、delta acceptance、closeout 与归档仍未执行
  - **Commit**：`250b42b fix: 完成P3.1真实MySQL迁移`（不 push）
  - **Blocked on**: none
  - **Next step**：用户在微信开发者工具重新启动本地后端并刷新小程序验证数据页；如需图片/声音真实链路或完整真机清单，分别授权 Gate 3b/3c

- **This session**: 2026-08-10 — **P3.1 闸门 2 实现完成，等待用户审查**
  - 用户批准的 P3.1 vertical slice 已实现：`SAVED` / `MOMENT`、显式幂等 `/save`、文字或 AVAILABLE 图片/声音 eligibility、7 天恢复 DRAFT、窄过期清理、DRAFT/SAVED 可编辑矩阵与 SAVED 后封存
  - frontend 已接通“留下这一刻”、保存内联反馈、恢复草稿二选一、media-first、SAVED 展示/继续补充/交给时间；Preview 保持只读 fail-closed
  - Agent 仅扩展 WRITING_GUIDANCE 的 DRAFT/SAVED 状态资格；REVIEW_CHAT、Prompt、provider、memory、guardrails、reflection、预算与 eval snapshots 外部语义不变
  - MySQL 增量脚本已生成但未在真实库执行；脚本 pre/postflight 只输出聚合计数，不读取或打印用户原文、位置、storage key 或 URL
  - TDD RED 证据覆盖 enum/schema、save、seal、SAVED 更新/附件、DRAFT visibility/expiry/cleanup 与 Agent mode；实现后 focused 回归 PASS
  - backend full：**91 suites / 687 tests / 0 failures / 0 errors / 8 skipped**；frontend `vue-tsc --noEmit`、标准 mp-weixin build、Preview build 全部 PASS
  - OpenSpec 文件级校验：4 specs / 28 Requirements / 98 Scenarios，任务、delta、实现与 active pointer 对齐；CLI 不在 PATH，validation 仍记 SKIPPED
  - 闸门 3a/3b/3c 仍未授权：真实 MySQL migration、真实对象存储、微信开发者工具/真机全部 SKIPPED；E0 目标用户理解仍为 INCONCLUSIVE / SKIPPED
  - scope guard PASS：未改 package/lockfile、deployment、monitoring、archive、accepted baseline、冻结蓝图或无关 Agent 语义；未实现 P3.2 删除/导出/清除全部
  - **Commit**：pending（Agent commit；不 push）
  - **Blocked on**: none（真实依赖证据须另行通过对应闸门）
  - **Next step**：用户审查实现 diff 与证据；闸门 3、delta acceptance、closeout、归档均不自动执行

- **This session**: 2026-08-10 — **P3.1 闸门 1 批准 + 闸门 2 实现授权**
  - 用户批准 proposal / design / tasks / 四份 delta 与 N1–N11，并明确允许开始实现
  - 授权范围：backend/frontend/SQL 代码与离线、H2、type-check、build 验证；提交责任继续为 Agent commit
  - 闸门 3a/3b/3c 均未授权：不连接真实 MySQL、不调用真实对象存储、不执行微信开发者工具/真机验收
  - P3.1 真实 AI provider 调用预算保持 0；不设置任何 real probe 环境变量
  - OpenSpec CLI 仍不在 PATH，apply/status/validate 继续记 SKIPPED，改用 change artifacts 和文件级任务跟踪
  - baseline：backend focused **7 suites / 126 tests / 0 failures / 0 errors / 0 skipped**；backend full **87 suites / 664 tests / 0 failures / 0 errors / 8 skipped**
  - frontend baseline：bundled Node 下 `vue-tsc --noEmit`、标准 mp-weixin build、Preview build 全部 PASS；build 的 `os - Alias not found` 为退出码 0 的环境噪声
  - 实现 allowlist 已写入 `tasks.md`；package/lockfile、deployment、monitoring、archive、accepted baseline、冻结蓝图与无关 Agent 语义为 denylist
  - **Commit**：pending（不 push）
  - **Next step**：按 TDD 顺序实现 T-18 起的 schema、状态机与业务任务

- **This session**: 2026-08-10 — **P3.1 `present-moment-capture` 规划闸启动**
  - readiness：开刀前 `ACTIVE_TASK=IDLE`、工作树 clean、HEAD 为 `2d9544a`；H0 已完成，当前无其他 active Type C
  - E0 因没有真实用户或参与者以 `INCONCLUSIVE / SKIPPED` 收口：有效参与者 0，不选 A/B/C 胜者，不倒填用户理解 PASS
  - 新建 `openspec/changes/present-moment-capture/`：proposal / design / tasks，以及 `backend-core` / `miniapp-core` / `agent-runtime` / `v2-product-scope` 四份 delta
  - N1–N11 推荐：`SAVED`、默认 `MOMENT`、幂等显式 save、文字或 AVAILABLE 图片/声音成立、7 天恢复 DRAFT、窄过期清理、DRAFT/SAVED 可编辑、SAVED 后封存、渐进披露、E0 交互细节 provisional、任意记录删除留 P3.2
  - 规划期真实 MySQL、对象存储、provider、微信真机调用预算为 0；没有修改业务代码、依赖、lockfile 或 accepted baseline
  - OpenSpec CLI 不在 PATH；仅执行仓库结构、Requirement/Scenario、任务 ID、范围与 Git 文件级校验，不声称 CLI validate PASS
  - **Gate state**：闸门 1 等待用户批准全部 artifacts 与 N1–N11；闸门 2、闸门 3 均未授权
  - **Commit**：pending（Agent commit 已授权；不 push）
  - **Blocked on**: 闸门 1 用户 review，不是实现阻塞
  - **Next step**: 用户批准或调整规划；只有另行明确授权闸门 2 后才按 `tasks.md` 修改业务代码

- **This session**: 2026-08-08 — **C9 `agent-temporal-intelligence` 闸门 3 验收归档**
  - 用户明确通过闸门 3 并授权收口归档；Git 仍由 Agent 提交，未授权 push、部署或发布
  - 真实 provider：六个固定合成场景 6/6 PASS，总调用恰为批准上限 6；所有输出通过 temporal overreach checker
  - recurrence eligible 场景实际 `hintUsed=false`：eligible prompt 已执行且安全，但真实模型采纳提示无正证据
  - 真实 MySQL：可清理合成用户与不同年龄记录验证 owner/status/focal 排除、24 个月窗口、
    recent/distant/long-ago 衰减与 recurrence eligibility PASS；`finally` 清理 PASS
  - 微信真机 SKIPPED：本机未发现微信开发者工具或可控真机环境；未以 H2/scripted/build 冒充
  - 五份 delta 已接受进 baseline；新增 closeout 并更新叙事 §10；change 归档至
    `openspec/changes/archive/2026-08-08-agent-temporal-intelligence/`；`ACTIVE_TASK=IDLE`
  - OpenSpec CLI 不在 PATH；使用 Requirement/Scenario 与 archive 文件级校验，不声称 CLI PASS
  - **Commit**：`14ec5f8 feat: 完成C9闸门3并归档`；未 push
  - **Blocked on**: none
  - **Next step**: Phase 2 已按冻结序列完成；后续 Optional C0/C10/C11 需证据触发并重新走独立规划闸

- **This session**: 2026-08-08 — **C9 `agent-temporal-intelligence` 规划闸启动**
  - readiness：开刀前 `ACTIVE_TASK=IDLE`、C8 已归档、Git clean、蓝图 v1.2 指向 C9；开工锚点 `544e9ea`
  - 新建 `openspec/changes/agent-temporal-intelligence/`：proposal / design / tasks +
    `agent-runtime` / `backend-core` / `v2-product-scope` / `miniapp-core` / `agent-collaboration` 五份 delta
  - 规划期事实：现有 `MemoryFragment` 已有 `occurredAt/timeLabel`；C3a 默认 24 个月、最多 3 片段，
    SQL 按 created_at 倒序；C3b 已区分回看目标记录与旁支检索，但尚无显式 distance/decay/pattern policy
  - N1–N8 推荐：30/180 天 distance bands；旁支片段 100/75/50% 字符预算且最低 40；focal review record 不衰减；
    recurrence 仅 REVIEW_CHAT + 显式比较 cue + 2 个不同旁支记录 + span≥90 天；overreach 直接兜底不 reflection
  - 契约边界：API/DTO/DDL/mapper SQL/frontend 页面与字段零变化；不新增 provider 调用，不做 dashboard/评分/诊断
  - OpenSpec CLI 不在 PATH；使用仓库既有结构与文件级检查，不能声称 CLI validate PASS
  - 规划期真实 provider/MySQL/真机调用预算 0；规划时默认用户手动提交，本轮后续已改为 Agent commit
  - 用户 2026-08-08 批准闸门 1（N1–N8 按推荐方案）、授权闸门 2，并授权 Agent commit；
    闸门 3 / push / 部署 / 发布未授权
  - T-09 baseline：focused PASS；后端全量 **81 suites / 645 tests / 0 failures / 0 errors / 6 skipped**
  - Maven 3.9.9 离线须加 `-Daether.localRepositoryManager=simple` 才能读取既有缓存；未下载依赖
  - **实现完成**：确定性 30/180 天距离分层、旁支记忆 100/75/50% 字符预算、focal 豁免、
    REVIEW_CHAT + cue + 两条不同旁支记录 + 90 天跨度的窄 recurrence 门槛，以及 `TEMPORAL_OVERREACH` fail-closed
  - **编排边界**：衰减后的同一列表同时进入 reply prompt 与 `AgentLayeredCorpus`；未新增 provider 调用；
    temporal disabled 不注入 supplement、不衰减、不生成 hint、不运行新增 checker
  - **验证**：C9 focused PASS；后端全量 **85 suites / 662 tests / 0 failures / 0 errors / 6 skipped**；
    前端 `vue-tsc --noEmit` 与 mp-weixin build PASS；`git diff --check`、路径与敏感标记扫描 PASS
  - **C6 基线审阅**：仅 `memory-long-fragment-must-be-truncated` 的注入字符由 120 合法变为 90；
    `baselineNote` 与 checksum 已同步，其余阶段、调用数和回复指标不变
  - **SKIPPED**：真实 provider、真实 MySQL、微信真机（闸门 3 未授权）；OpenSpec CLI 不在 PATH，
    仅完成 5 specs / 20 Requirements / 45 Scenarios 文件级校验
  - **Commit**：`65e18e0 feat: 实现C9时间智能策略`；未 push
  - **Blocked on**: none
  - **Next step**: 用户 review 当前实现；闸门 3、接受 delta 与归档均需后续明确授权

- **This session**: 2026-08-08 — **C8 `agent-resilience` 闸门 3 验收归档**
  - 用户授权真实 provider / MySQL 验收、归档与 Agent 提交；未授权 push、部署或发布
  - 真实 DeepSeek 使用固定合成短文本：2 次 canary 为 1378ms / 1656ms；2 组双调用为
    2898ms / 3531ms；总计 6/6 成功，未触发停止条件
  - 真实 MySQL：同一 pending turn 仅一条 USER message；attempt 1 为
    `UNAVAILABLE/auth-configuration`，attempt 2 为 `SUCCESS`；合成数据已清理
  - 微信真机 SKIPPED：本机无微信开发者工具或可控真机环境；未以构建/scripted 冒充真机
  - 四份 delta 已接受进 baseline；新增 closeout，change 归档至
    `openspec/changes/archive/2026-08-08-agent-resilience/`；`ACTIVE_TASK=IDLE`
  - OpenSpec CLI 不在 PATH；使用 Requirement 对齐与 archive 文件级验证，不声称 CLI PASS
  - **Blocked on**: none
  - **Next step**: 如需继续，先开 C9 `agent-temporal-intelligence` 独立规划闸

- **This session**: 2026-08-03 — **C8 `agent-resilience` 规划闸启动**
  - readiness：开刀前 `ACTIVE_TASK=IDLE`、C7 已归档、Git clean、蓝图 v1.2 指向 C8；开工锚点 `fb68082`
  - 新建 `openspec/changes/agent-resilience/`：proposal / design / tasks +
    `agent-runtime` / `backend-core` / `miniapp-core` / `agent-collaboration` 四份 delta；
    `v2-product-scope` 明确无 delta
  - **规划期关键事实修正**：`app.ai.timeout-millis=20000` 是每次 `HttpRequest` 的 timeout，
    不是整轮 request deadline；C7 双调用理论上可叠到约 40s，已超过 frontend 30s 等待窗口
  - P14 推荐：整轮 provider-work budget 24000ms；每次调用取 `min(20000ms, remaining)`；
    预算耗尽不发下一调用；frontend 30000ms 与纯 DB 10000ms 保持不变
  - 第一阶段推荐零自动 retry；现有用户主动同轮 retry 保留；provider failure 仍显式 FAILED/UNAVAILABLE，
    本地温暖模板只进入失败 message，不持久化为 Assistant 假回复
  - API/DTO/frontend 推荐零字段变化；技术分类只留 backend，既有用户主动同轮 retry 保持
  - OpenSpec CLI 不在 PATH；已改用仓库既有结构与文件级验证，不能声称 CLI validate PASS
  - 用户 2026-08-03 批准 N1–N6 推荐方案并明确授权开始实现；Git 仍由用户手动提交
  - T-08 实现前 baseline：显式本机 Maven repository/settings 离线运行，
    **74 suites / 622 tests / 0 failures / 0 errors / 4 skipped**；默认 Maven 仓库缺 parent 的失败仅属环境解析
  - **实现完成**：8 类封闭 failure taxonomy、类型化 provider exception、request-scope 24000ms
    provider-work budget、阶段化失败 message、initial/reflection/material 脱敏 trace/log 已接入；零自动 retry
  - **契约边界**：未新增 API/DTO/frontend 字段；未改 DDL、pom/package/lockfile、deployment、monitoring、C9
  - **验证**：focused PASS；后端全量 **79 suites / 643 tests / 0 failures / 0 errors / 4 skipped**；
    前端 type-check 与 `build:mp-weixin` PASS；C6/C7 快照零变化
  - **SKIPPED**：真实 MySQL、闸门 3 provider/真机、OpenSpec CLI；真实 provider 调用 0 次
  - **Blocked on**: none（等待用户验收；Git 仍由用户手动提交）
  - **Next step**: 用户 review；通过后再决定是否归档，以及是否另行开放真实 MySQL / 闸门 3

- **This session**: 2026-08-03 — **C7 `agent-reflection-loop` 验收归档**
  - 用户在已知真实 MySQL、闸门 3 provider/真机、人评锚点与 OpenSpec CLI validate 均未执行后明确要求归档
  - 四份 delta 已接受进 baseline；`miniapp-core` 无 delta
  - `tasks.md` 将未授权外调逐项记为 SKIPPED，不伪记 PASS；新增 `closeout.md`
  - 叙事 §8 已补，架构/入口状态对齐到 C8；冻结蓝图未修改
  - **Blocked on**: none
  - **Next step**: 如需继续，先做 C8 `agent-resilience` 只读 readiness 与规划闸

- **Previous session**: 2026-08-02 — **C7 `agent-reflection-loop` 完成 reply-only 实现与离线验证**
  - readiness：开刀前 Git clean、`ACTIVE_TASK=IDLE`、C6 已归档、蓝图 v1.2 明确 C7 为下一刀，C4 + C6 硬依赖满足
  - 新建 `openspec/changes/agent-reflection-loop/`：proposal / design / tasks +
    `agent-runtime` / `backend-core` / `v2-product-scope` / `agent-collaboration` 四份 delta；
    `miniapp-core` 明确无 delta
  - **规划期事实修正**：普通 reply 当前不做全量忠实度检查，故不会产生 `UNFAITHFUL`；
    最终按用户裁决只保留非 CLOSING reply 的 `MISSING_TIME_ATTRIBUTION` 窄环，material 不开环
  - P13 推荐定案：reflection 属同一业务 attempt 内的 provider 子调用；一轮一条 trace，
    `attemptNo` 不增加，steps 用 `initial|reflection` 区分
  - 调用预算：最大重写 1 次；provider failure / invalid / `CHECK_ERROR` 不重试；
    闸门 3 先最多 2 次 canary、总上限 6；未授权不得执行
  - OpenSpec CLI 不在 PATH，未运行 CLI scaffold/validate；沿用仓库既有 change 结构并做文件级校验
  - 当前 checkout 后端全量复验：显式指定本机 Maven repository/settings 后
    **606 tests PASS / 4 skipped，0 failures / 0 errors**；默认 `mvn -q -o test` 因默认本地仓库
    缺 Spring Boot parent 在 POM 解析阶段失败，未进入编译，二者已分开记账
  - 2026-08-02 用户已批准闸门 1、授权闸门 2 与本次 Git 提交；闸门 3 / push / 部署仍未授权
  - 实现前 focused baseline（C4/C5/C6 指定测试）PASS；开工锚点 `b459b8f`
  - **实现前新发现的设计冲突**：CLOSING 一轮当前已经依次生成 reply 与 material，正常即 2 次 provider；
    若 material `UNFAITHFUL` 再 reflection，会达到 **3 次调用**，违反已批准 delta 的“单轮最多 2 次”，
    且按 C5 平均 6476ms 推算约 19.4s，几乎顶满 backend 20s。业务代码尚未修改
  - **用户裁决（2026-08-02）**：按推荐方案收窄为 reply-only。
    仅非 CLOSING reply 的 `MISSING_TIME_ATTRIBUTION` 开环；material `UNFAITHFUL` 保持现有丢弃；
    CLOSING reply 也不开环，确保 reply + material 单轮仍最多 2 次调用。闸门 3 预算同步收窄为 6
  - **实现**：新增类型化 `AgentReflectionPolicy` 与最小 `AgentReplyPipeline`；reflection 固定一次、
    `tools=[]` / strict=false，成功时保留 initial tool calls，最终兜底时丢弃；CLOSING、material、tool、
    provider failure/invalid 均不开环
  - **轨迹**：provider step 新增 `phase=initial|reflection`；顶层耗时累加；新增脱敏
    `reflection-decision` / `reflection-result` / `reflection-provider-failed`，仍是一轮一条 trace
  - **C6**：合成用例由 23 增至 28；只更新 C7 合法改变/新增的 5 条快照，均同步写明
    `baselineNote` 与 checksum；baseline guard 与隐私测试 PASS
  - **验证**：focused tests PASS；后端全量 **622 tests PASS / 4 skipped，0 failures / 0 errors**；
    `git diff --check` PASS。真实 provider、真机与真实 MySQL reflection 联调未执行
  - **Blocked on**: none
  - **Next step**: 等待用户验收；闸门 3 / 真实 MySQL 联调另行授权或安排

- **Last session**: 2026-07-30 — C5 全流程完成并归档，随后修掉三个 Type B，真机复验全部 PASS
  - 规划：30 条现状事实（V1–V30）、11 条决策、四份 delta；N1–N7 按推荐定稿
  - 实现：`agent/trace/` 五个类 + 实体/mapper/XML + DDL + 9 条排查查询 + 38 项测试
  - 既有缺陷补齐：**V4**（降级痕迹此前恒传 null sessionId/turnNo）；**V5** 改用轨迹解决，未动 checker 签名
  - 闸门 3 执行完毕；delta 接受进 baseline；归档
  - **三个 Type B（均已真机验证 PASS）**：
    1. `ce4638f` 请求超时：前端 30s / 后端 20s，**顺序不可颠倒**
    2. `b6bcdd5` 输入框无法聚焦：关闭手势移到独立背景层（textarea 原生组件事件穿透 catchtap）
    3. `87cb29e` **每轮卡满 50 秒**：C5 自身引入的自锁，轨迹落库延后到事务提交后
- **This session**: 2026-07-30 — **蓝图 v1.2 校准会完成并冻结**（Type A 讨论 + Type B 文档落地）
  - 十问逐支定案 → D25–D33 九条新决策；序列定为 C6 → C7 → C8 → C9（新增 C7 反思环，编号顺移）
  - 消化五条实测证伪的前提（含新发现：steering 声称 Spring Security，实际全仓零匹配）
  - `iteration-blueprint.md` 升 v1.2 已冻结；11 处活文档引用同步；archive 内的 v1.1 引用**未动**（归档即历史）
  - 新建 `narrative/agent-tech-story.md`（§1–§6、§10 已写，§7/§8 待 C6/C7 补）
  - 未提交（默认用户手动提交）
- **This session**: 2026-07-31 — **C6 `agent-eval-framework` 规划闸执行完毕**（Type C 规划期，零业务代码）
  - 建 `openspec/changes/agent-eval-framework/`：`proposal.md`（36 条现状事实 E1–E36 + 8 个待裁决项 N1–N8 + 37 条验收）、
    `design.md`（12 条决策记录）、`tasks.md`（34 项 + 范围守护自检）、三份 delta 建议
    （`agent-runtime` 1 MODIFIED + 6 ADDED、`backend-core` 5 ADDED、`agent-collaboration` 3 ADDED）
  - delta 落点按蓝图 §5 的 C6 行：`v2-product-scope` 与 `miniapp-core` **无 delta**
  - **规划期核出五条对既有认知的修正**，全部已写进 proposal（详见 AGENT_LOG 同日条目）：
    1. **E7｜mock 分支根本不组装 prompt** → 「用 `AgentMockResponder` 跑评测」这条最直觉的路
       天然缺上下文组装维度，且跑不出任何降级轨迹。**这直接决定 N3 选 `AgentModelClient` 层而非给
       `AgentMockResponder` 抽接口**——后者看着更正统，实际覆盖面更小
    2. **E20｜仓库无 CI**（无 `.github/`，workflow 零命中）→ 架构宪法 §3.6 写的「CI 可跑子集」
       **当前无处兑现**。本刀只能交付「一条 maven 命令可跑」，已列为硬性诚实项（验收 34）
    3. **E21｜蓝图写的 `local-samples.yaml` 当前不被任何 gitignore 规则覆盖**；且仓库无通用
       `*.local.*` 规则。→ N5 建议改用通配命名（C5 已因「点名单个文件」吃过一次教训）
    4. **E24/E25｜两处断言可及性边界**：追问上限是代码常量（`MAX_REASK_PER_STAGE=1`）不可按用例调；
       无聚合记忆字符预算配置项，「注入预算」只能表达为**派生上限**并须如实标注
    5. **E28｜蓝图 §3.2 的 1183 行已过时，实测 1274 行**（C5 后 +91）。按 §0.4「规划期判断实现期须复核」
       登记勘误；**不改已冻结蓝图**，建议 C7 规划时以实测值重算论证
  - **本刀的关键性质：`src/main` 零改动**。四条既有事实刚好够用（22 协作者全构造注入、
    收集器读取器全 public、`persist` 单一出口可 `@Mock`、junit-params 与 snakeyaml 已在测试 classpath）
    → 不改 pom、不加 DDL、不为可测性动生产代码。这是 C5「收集器 + 单一落库出口」纪律的副产品
  - 未提交（默认用户手动提交）
- **This session（续）**: 2026-07-31 — **闸门 1 批准 + 闸门 2 授权后完成全部实现**（T-01 ~ T-28）
  - 实现顺序按 tasks 走：`.gitignore` 先行并验证 → harness → scripted 替身 → 用例与 runner →
    八维度不变量 → 快照与防橡皮图章 → 隐私 → 锚点结构 → 基线 → 回归
  - **实现期发现并处理的五件事**（详见 AGENT_LOG 同日第二条）：
    1. **E37｜回归基线实测是 536 / 4，不是规划期沿用的 534 / 3**。AGENT_LOG 里其实早有
       536 / 4，是 ACTIVE_TASK 顶部与蓝图摘要没跟上。**已修本文件**，蓝图不动（已冻结）
    2. **护栏的一处真实边界**：「用户自己说过的病名可以复述」的成立条件比直觉窄——
       取决于 Agent 是否连带复用了周边 4-gram。同一输入下只改复述措辞即被判 `diagnostic`。
       属 n-gram 方案固有性质与 C4 刻意选的误伤方向，**本刀不校准阈值，只把边界钉成回归**
    3. **自己写的一条假用例**：截断用例最初样本 111 字（< 120 上限），「不超过 120」恒成立
       而什么都没验。已改成远超上限的文本 + `injectedCharsExactly` 断言恰好等于 120
    4. **snakeyaml 会把未加引号的 timestamp 自动转成 `java.util.Date`**，
       `LocalDateTime.parse` 拿到 `"Sun Mar 15 05:00:00 SGT 2026"` 而失败。两头都处理了：
       解析层兼容 `Date`，用例里也加引号——只做后者等于留一条「靠人记得加引号」的规矩
    5. **`ArgumentCaptor` 不适合多轮取轨迹**：captor 拿到的是同一个可变对象的引用，
       多轮时读到的全是最后一轮终态。改用手写 `RecordingTraceSink` 按 persist 顺序存下
  - 未提交（本轮已获授权可提交）
- **已提交**: `aedab6c`（用户当轮授权 commit；**未 push**）。提交后已复跑全量测试，
  BUILD SUCCESS——git 对 YAML 做 LF→CRLF 规范化，故提交后必须复验一次解析仍正常
- **This session（收口）**: 2026-07-31 — **C6 验收通过并归档，Phase 2 开局完成**
  - delta 接受进 baseline：`agent-runtime` 1 MODIFIED + 6 ADDED、`backend-core` 5 ADDED、
    `agent-collaboration` 3 ADDED；`v2-product-scope` 与 `miniapp-core` 确认无 delta
  - change 目录用 `git mv` 移入 `archive/2026-07-31-agent-eval-framework/`（保留历史）
  - `closeout.md` 写就；叙事文档 §7 已在实现期补完（D33 固定收尾项）
  - **闸门 3 的处理**：用户表述为「闸门 3 通过」，但本刀**并无外调可授权**（预算 0、
    实测 0 次），故在 closeout 与 tasks 中如实记为**未申请**而非「已通过」。
    由此产生的未验证项（快照指标在真实 provider 下的稳定性）单独登记
- **Blocked on**: none
- **Next step**: **开 C7 `agent-reflection-loop` 规划闸**。建 change 目录写
  proposal / design / tasks + delta 建议 → 闸门 1 待批准。
  开工前读蓝图 §4.3 意图卡片，并注意 Direction Layer 里列的三件事
  （类大小用实测 1274 行、锚点顺带在闸门 3 填、快照变化须写 `baselineNote`）

## C6 的关键结论（对 C7 有直接价值）

- **量尺已就位，R2 第一次可以谈优化了**。基线定于优化**之前**，记的是「当前状态」而非
  「理想状态」——这一点写在 `eval/baseline/snapshots.yaml` 文件头，避免将来被误读
- **C7 改编排行为时，C6 快照会变，那是预期的**。正确做法是确认变化符合预期后手工更新，
  并在 `baselineNote` 写明由 C7 改的；**不得**把数字改成当前值了事
  （checksum 由「指标 + 说明」共同派生，只改数字会被拦住）
- **`AgentChatServiceImpl` 实测 1274 行**（蓝图记 1183 行已过时）。C7 要在这个类里
  掉转依赖方向，论证须用实测值
- **走 mock provider 分支评不到上下文组装**：生产代码在 `isMockProvider()` 处直接 return，
  压根不组装 prompt。C7 若要为重写路径写测试，同样要走非 mock 分支才看得到 `prompt` 步骤
- **替身设计的一条经验**：替身替掉的越多，被覆盖的生产代码越少。C6 的 provider 替身
  继承生产类只覆写两个真正发请求的方法，其余判定（配置可用性、FC 白名单）全走生产逻辑
- **护栏一处未校准的 n-gram 覆盖边界**已钉成回归用例。C7 的重写指令若改变复述措辞的形态，
  这条用例会有反应

## C5 的关键结论（对 v1.2 校准与 C6 有直接价值）

- **provider 耗时首次有数据**：min 4571 / avg 6476 / max 8467ms。这项数据 C5 之前完全不存在
  （成功路径的 `startedAt` 被直接丢弃）。它同时是 Type B 超时缺陷的定位依据，
  也是 **C7 反思环预算**与 **C8 韧性**设计的输入（v1.2 编号：韧性已从 C7 顺移至 C8）
- **版本锚点由内容哈希派生**，改文案自动变化。C6 的回归比对可直接按
  `prompt_version` / `policy_version` 分组（`c5-trace-queries.sql` 第 7 条）
- **`AuthRole.ADMIN` 全仓无签发路径**（`UserServiceImpl` 固定签 `USER`）→
  `/admin/**` 下的端点在真实环境不可达。**任何未来 change 若打算做 admin 端点，须先解决签发问题**
- **`schema.mysql.sql` 只到 C1**：既无 `agent_tool_call`（C2）也无 `agent_session.purpose`（C3）。
  项目既有约定是全量脚本不随增量维护。**待用户决定是否另开 Type B 补齐**（见 Residual）
- 两处已获批的对已冻结蓝图的偏离：存储用 MySQL 表而非 JSON 日志文件；默认全量不采样

## 实测证伪 / 修正的前提（**已于 v1.2 冻结时消化，见蓝图 §2.3**）

以下五条已写入蓝图 §2.3 并落到相应文档，此处保留供快速查阅：

1. **`/admin` 端点方案不可行**：`AuthRole.ADMIN` 全仓无签发路径（`UserServiceImpl` 固定签 `USER`），
   该路径下的端点在真实环境不可达。任何未来 change 若要做 admin 端点，须先解决签发问题
2. **`schema.mysql.sql` 不随增量维护**：它只到 C1（无 C2 表、无 C3 列、无 C5 表）。
   规划期曾断言「新表需同步三处」，与项目既有约定不符
3. **H2 集成测试不足以验证锁 / 外键 / 事务边界**：C5 的 50 秒锁等待缺陷在 H2 上**不可能复现**。
   → **已写入蓝图 §0.4：「真实联调」定义收紧为包含真实 MySQL**
4. **R2 的优化基线须重建**：此前「引导话术生硬」的判断样本受 55 秒延迟污染。
   → **已定案由 C6 重建基线**（D30/D32），不再靠手验找感觉
5. **认证不基于 Spring Security**（校准会新发现）：`springframework.security` 全仓零匹配、
   pom 无 security starter，实为 jjwt + 自研过滤器。→ **已修 `.kiro/steering/tech.md`**

**附带事实**：`pom.xml` 含 `spring-boot-starter-data-redis` 且 dev/prod yml 有 redis 配置段，
但 main 代码零消费（会话走 MySQL）。标记 `partial`，不在 Phase 2 处理

## Residual / Carry-over

- **[R10] 回看 fail-closed 仍未活体触发**：C3b 3 轮 + C5 6 轮共 9 轮观察，模型均未在无工具模式返回提议。
  属概率性行为，**不单开 change**；C5 做到「它真发生时能被记下」，
  **C6 又让它的正确性多一层常驻回归**（评测里能稳定驱动该分支）。
  但**未活体触发这个事实未变**，不得因为多了回归就写成已验证
- **[待用户决定] `schema.mysql.sql` 落后于增量脚本**：补齐它需要同时补 C2 + C3 + C5 三刀的表与列，
  属独立 Type B。C5 刻意未动（只加 C5 会造出「有 C5 表却无 C2 表」的更怪状态）
- **[新] 轨迹写在业务事务之后**：理论上存在「业务成功但轨迹丢失」的窗口（进程崩溃）。
  可接受——轨迹是辅助设施，而原方案的代价是每轮卡 50 秒
- **[R2｜量尺已就位，可以开始谈优化] 引导话术与素材合成质量**：C6 已建成基线（D30/D32 兑现）。
  **基线定于优化之前**，记的是「当前状态」而非「理想状态」（写在
  `eval/baseline/snapshots.yaml` 文件头）。现在改 prompt 前后第一次可比对——
  宪法 §7.3 那条「无 Eval 情况下大改 prompt 上线」的禁令已具备解除条件。
  改动后快照会变，须确认符合预期再手工更新并在 `baselineNote` 写明由哪一刀改的
- **[新｜C6 未完成项] 快照指标在真实 provider 下的稳定性未验证**：C6 外调预算 0。
  它其实不构成风险——快照的用途是「同一份确定性输入下改动前后是否变化」，
  真实 provider 从来不在这个用途里。**如实登记，不得写成已验证**
- **[新｜C6 未完成项] 话术质量人评锚点为空**：`eval/baseline/narrative-anchors.yaml`
  结构已就位、内容为空，且「空≠已覆盖」由测试守着。填它需真实产出 + 人评，
  **建议顺带在 C7 闸门 3 做**（同一批真实产出既验重写又当锚点）
- **[C6 登记｜待用户决定] `minMemoryOnlyRunForAttribution=8` 未校准**（代码注释明写；
  对比 `minCoverage=0.35` 有真实样本校准记录）。**另新增一条**：护栏「只在新增区段匹配」
  的成立条件依赖 4-gram 覆盖，复述用户说过的病名时是否放行取决于有没有连带复用周边措辞
  ——已钉成回归用例。两者 C6 都**刻意不校准**：建量尺和用量尺改参数混在一起，
  就分不清指标变化是哪个原因造成的（R2 那次误判的同型错误）。属独立事项
- **[C6 登记｜待用户决定] 仓库无 CI**：无 `.github/`，全仓 workflow 零命中。
  架构宪法 §3.6 的「CI 可跑子集」当前无落点，C6 交付的是「一条 maven 命令可跑」。
  未顺手建（撞「不改 deployment / monitoring」，且涉及跑在哪 / 密钥怎么给 / 失败怎么处理）
- **[新｜C6] 基线手工更新的规模上限**：刻意不提供自动重写开关（有了它，「评测红了」
  的最短路径就变成「跑个命令让它变绿」）。用例大幅增长后批量更新会烦；届时若加开关，
  须**同时**配「说明未变更则失败」的守护——**不得先给出口再补守护**
- **[新｜C6] snakeyaml 是传递依赖**（经 `spring-boot-starter`），非本项目直接声明。
  已加 `NoClassDefFoundError` 兜底明确失败，绝不静默跳过用例——否则某次 starter 升级后
  会变成最坏形态：**绿灯但什么都没测**
- **[已关闭] 三个 Type B 的真机复验**：用户 2026-07-30 执行，全部 PASS
- **[已关闭] MySQL 上轨迹落库未联调**：已联调，并由此发现且修复了 50 秒锁等待根因
- **[R9] 检索相关性弱**：标签 + 说明性字段 LIKE，无权重 / 分词 / 向量。升级留独立 change
- **[R6｜待用户执行] 凭证轮换**：`AI_API_KEY` / `S3_ACCESS_KEY` / `S3_SECRET_KEY` / `S3_SESSION_TOKEN` /
  `WECHAT_MINI_PROGRAM_SECRET`。轮换后建议删除 `backend/start-dev-wechat.local.ps1.bak`（含旧明文，已 gitignore）
- **探针资产**：`C3RealProviderProbeTest`（`C3_REAL_PROBE=1`）、`C4RealProviderProbeTest`（`C4_REAL_PROBE=1`）、
  `C5RealProviderProbeTest`（`C5_REAL_PROBE=1`）、`C5MysqlTraceProbeTest`（`C5_MYSQL_PROBE=1`）。
  **共 4 个，全部默认跳过**（这也是当前基线 4 skipped 的来源）。C5 探针另有一处形态差异：
  它是 `@SpringBootTest` 走完整 `sendMessage`，因为要验的是编排层有没有漏采集。
  **C6 未新增探针**（0 外调）
- **[新｜C6] 评测资产**：`backend/src/test/java/com/flashback/agent/eval/`（10 个类 + 5 个测试类）、
  `backend/src/test/resources/eval/`（4 份用例 + 快照基线 + 空锚点）。
  **离线、零外调、默认随 `mvn -q test` 一起跑**，不需要任何环境变量
- **[新｜C6] 本地真实样本入口**：`backend/src/test/resources/eval/samples.local.yaml`
  （**当前不存在**；已被 `.gitignore` 的 `*.local.yaml` 通配覆盖并用 `git check-ignore` 验证过）。
  缺失时评测静默跳过相关用例，这正是别人 clone 仓库时的状态
- **本地联调脚本**：`backend/run-c5-probe.local.ps1`（已 gitignore；`.gitignore` 的 `*.local.ps1`
  规则由 C5 从「只点名单个文件」改为通配）
- **[C3a 实测] 本地 `tag` 表 0 行**、`core_question` 0% 非空、26 条记录中 `ai_summary` / `belief_then` 各 62%
- **历史数据**：`agent_message` 中 6 条 C2 修复前的 JSON 包裹消息（id 13/15/17/19/20/21）
- **本地环境**：MySQL80 StartType=Manual；C1/C2/C3/**C5** 的增量 DDL 均已执行完毕
- **secret 读取方式**：`backend/secrets.local.env`（gitignore）+ `Get-LocalSecret`，缺键快速失败
- **M4 carry-over**：MySQL `EXPLAIN` timeline（Type B，不阻塞主线）
- **Kiro 诊断误报**：change 的 `design.md` / `tasks.md` 报「缺少 Kiro Spec 章节」。
  本项目按规则使用 OpenSpec 而非 `.kiro/specs/`，**不修**

## 流程教训（后续 change 须遵守）

- **含 DDL 的 change 必须把「本地执行 DDL」列为实现期第一步**，而不是联调前置。
  C3b 曾因此让用户手验报「系统异常: api/agent/sessions」——且因为 mapper 列清单缺列，
  **写作引导对话也一起 500**，波及既有功能，而报错表现只是通用 500。C5 已按此执行
- **不得使用波及未跟踪文件的 git 操作**：曾用 `git stash push --include-untracked` 意外收走用户的
  `iteration-blueprint-v1.2-draft.md`、`Docs/agent-iteration/architecture/`、`.kiro/skills/`（已按字节校验恢复）。
  一律只用显式 `git add <path>`，不使用 stash / clean / reset --hard
- **警惕编辑器自动格式化造成的 diff 污染**：若某文件 `git diff --stat` 比预期改动量大一个数量级，
  先怀疑格式化或行尾变化（用 `--ignore-all-space` / `--ignore-cr-at-eol` 对比），不要当成真实改动接受
- **验证拦截方向必须先确认样本确实处于该被拦的状态**：C3b 曾取「最后一轮」回复做剥离实验，
  而它恰好没在复述（memory-only=0），不翻转是样本选错而非护栏失效
- **[新｜C5] 前后端超时必须有明确的先后关系，不能相等**：相等时前端总是先断，
  后端精心设计的显式失败语义会被网络层错误覆盖。凡新增调用 AI 的前端请求，
  须显式指定超时且大于后端 `app.ai.timeout-millis`
- **[新｜C5] 规划期的「须同步三处」类判断要在实现期复核**：C5 规划时断言新表需同步三份 schema，
  实测发现 `schema.mysql.sql` 只到 C1，项目既有约定与规划假设不符。**不要把假设写成事实**
- **[新｜C5 最重要的一条] 涉及锁 / 外键 / 事务边界的改动，H2 全绿不构成验证**：
  C5 的轨迹落库在 H2 上 37 项测试全绿，却在真实 MySQL 上让每轮对话卡满 50 秒
  （`REQUIRES_NEW` 与外层事务争 `agent_session` 的父行锁）。H2 没有 InnoDB 行级锁语义，
  **该缺陷在 H2 上不可能复现**。此类改动的联调必须打真实 MySQL；
  写回归时优先断言**调用时机 / 不变量**而非结果，这样在 H2 上也能守住
- **[新｜C5] `REQUIRES_NEW` 不等于「不影响外层事务」**：它挂起外层事务但**不释放外层已持有的锁**。
  若新事务要碰同一批行（尤其经由外键），就会与自己的外层事务死等。
  想做「业务提交后再做副作用」应用 `TransactionSynchronization` 回调，不是换传播级别
- **[新] fail-open 只保证不报错，不保证不阻塞**：C5 的轨迹写入确实 fail-open，
  但它在失败**之前**先卡了 50 秒。评估「某个辅助设施失败是否影响用户」时，
  必须同时看**失败前的等待成本**，不能只看失败后的处置
- **[新｜C6] 警惕「恒成立所以什么都没测」的断言**：C6 写的一条截断用例样本只有 111 字，
  而上限是 120——「不超过 120」恒为真，那条用例实际什么都没验。
  验「某个上限真的生效」要断言**恰好等于上限**，不是「不超过上限」。
  同型问题也出现在验证拦截方向时（C3b 的样本选错）：**先确认样本真的处于该被测的状态**
- **[新｜C6] 替身替掉的越多，被覆盖的生产代码越少**：极端情况下断言的是自己的替身。
  C6 的 provider 替身继承生产类只覆写两个真正发网络请求的方法，
  「配置是否可用」「model 是否在白名单内」这些判定仍走生产逻辑；
  记忆检索用真实 port + mock mapper，使「注入不超预算」断言的是 port 真的会收口
- **[新｜C6] 摘要类数字要定期复核**：`ACTIVE_TASK` 顶部与蓝图记的回归基线 534/3
  已过时（实测 536/4），而 AGENT_LOG 里其实早有正确值——**是摘要没跟上明细**。
  每刀开工时顺手核一次自己要引用的数字
- **[新｜C6] `ArgumentCaptor` 不适合取多次调用中的可变对象**：captor 持有的是引用，
  多轮时读到的全是最后一轮的终态。要按顺序看每一轮，用能在调用时刻存快照的替身

## 未跟踪 / 未提交产物（不要擅自提交或移动）

> **2026-07-31 复核修正**：本节此前列的多项已在 `e5722d8` / `486ca95` 两个提交中入库，
> 不再是未跟踪产物。以下为 `git status` 实测的当前状态。

**当前未跟踪（实测）**：

- `.kiro/skills/`（**唯一一项**。C6 提交时刻意未纳入——它是工作区既有产物，不属那一刀范围）

**已 gitignore 的本地脚本（不入库，保留）**：

- `backend/run-c5-probe.local.ps1`（闸门 3 探针运行脚本）
- `backend/probe-turn-latency.local.ps1`（一轮耗时排查脚本，签本地 dev JWT 打真实链路。
  定位 50 秒锁等待时用的就是它，保留供将来复用）
- `backend/start-dev-wechat.local.ps1` 与其 `.bak`（`.bak` 含旧明文，待 R6 轮换后删除）

**已入库，本节不再挂账**：`Docs/agent-iteration/architecture/`（三份）、
`narrative/agent-tech-story.md`、v1.2 冻结带来的 8 处引用同步改动。
`roadmap/iteration-blueprint-v1.2-draft.md` **已不存在**（内容已迁入正式蓝图，草稿已删）。

## Global Guardrails After P3.1

- 当前 `ACTIVE_TASK=IDLE`；不得沿用 P3.1、C9 或 H0 授权启动 P3.2 或其他业务实现
- **改 prompt / 护栏阈值现在有条件了，但仍须走 Type C**：C6 的基线让改动可比对，
  这解除的是宪法 §7.3 的技术前提，**不是**流程要求。改动后快照会变，
  须确认符合预期再手工更新并在 `baselineNote` 写明由哪一刀改的
- **不要校准阈值**（含 `minMemoryOnlyRunForAttribution` 与 C6 登记的 n-gram 覆盖边界）——
  属独立事项，待用户决定
- **不要静默刷新 C6 的快照基线**：checksum 由「指标 + 说明」共同派生，只改数字会被拦住；
  也不要为图方便加自动重写开关（若要加，须同时配「说明未变更则失败」的守护）
- 不做 LLM-as-Judge / 绝对评分 / A/B 框架 / 质量看板（D31/D32）
- 不借已归档 C9 顺手扩展 C8 韧性、多 provider、检索重写或前端分析面板
- 不引入 CI 配置（属独立决策）
- 不要并行复活已归档 change 作为隐式 active change
- 不要在未获新授权时发起真实 provider / MySQL 调用（6 个探针默认门控跳过，勿擅自设置
  `C*_REAL_PROBE=1` / `C*_MYSQL_PROBE=1`）
- 不要修改 `openspec/changes/archive/**`——归档即历史，含其中的 v1.1 引用与 C6 的勘误登记
- 不修改已冻结蓝图（C6 登记的两处勘误——1274 行、536/4 基线——只留在 change 内）
