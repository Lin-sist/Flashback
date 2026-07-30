# Agent Observability（C5）

> Change ID：`agent-observability`
> Type：**C**
> 阶段：**闸门 1 已批准（2026-07-30，N1–N7 按推荐定稿）；闸门 2 已授权，实现进行中**
> 开工锚点：`a834d85`
> 上游方向：`Docs/agent-iteration/roadmap/iteration-blueprint.md` v1.1 §4 C5（已冻结）
> 前置：C1 / C2 / C4 / C3a / C3b 全部归档，delta 均已接受进 baseline
> 回归基线：后端 **496 tests PASS / 2 skipped**（2 skipped 为环境门控的真实 provider 探针）

---

## 1. Why Now

C1–C3 把 Agent 的能力堆到了「有 Loop、有工具、有护栏、有记忆、能回看」。代价是：**一轮对话现在要经过七八个决策点，而这些决策点对开发者是黑箱**。

具体到已经发生过的事：

- C3b 闸门 3 时，用户手验报「系统异常: api/agent/sessions」。当时能拿到的只有一条通用 500，**排查靠的是人去猜「哪一步可能挂了」**，最后发现是本地缺 `purpose` 列。如果那一轮的链路有痕迹，问题在第一分钟就能定位到 mapper 层。
- C3b 验证时间归属护栏时，**第一版实验取错了样本**——取「最后一轮」回复做剥离，而它恰好没在复述（`memoryOnlyRun=0`），不翻转是样本选错而非护栏失效。发现这一点的唯一办法是探针里临时打印指标。这类指标本该常驻。
- C3b 归档时留下一条残余：**回看的 tool_calls fail-closed 分支未活体触发**，正确性只由单测覆盖。它是概率性行为，不值得单开 change 去逼它发生——但只要链路留痕，它真发生的那一次就会被记下来。

所以这一刀要补的不是新能力，而是**把已有的七八个决策点从黑箱变成可读的轨迹**。它同时是 Phase 1 的收官条件：蓝图 v1.2 草案 §0.2 明确「C5 归档后做校准会再冻结 v1.2」。

**为什么现在做是安全的。** C5 不新增任何用户可见行为，前端零改动，Agent 的对话逻辑一行不改——它只是在既有链路上挂痕迹。风险集中在两处，都在下面明确列出：一是隐私（痕迹必须不含原文），二是存储选型与蓝图缓解措施之间存在一处**真实张力**，需要用户裁决（§3.1）。

---

## 2. 现状事实（能力五态）

> 事实来源：`service/impl/AgentChatServiceImpl.java`、`agent/**`、`controller/api/AgentController.java`、`security/**`、`config/WebMvcConfig.java`、`config/AppAgentProperties.java`、`sql/mysql/c1~c3-*.sql`、`src/test/resources/schema.sql`、`application.yml`、`openspec/specs/**`

### 2.1 已有的「痕迹」现状

| # | 事实 | 状态 |
|---|---|---|
| V1 | 全部埋点是 SLF4J 占位符文本日志。**无 JSON encoder、无 logback-spring.xml、无自定义 pattern**；`logging` 配置只有 `application-{dev,prod}.yml` 里的 level | `confirmed` |
| V2 | **无 trace/requestId/correlationId 机制；无 MDC；无 AOP**（`@Aspect`/`@Around` 全仓零命中）。唯一拦截器 `JwtAuthenticationInterceptor` 不埋点 | `confirmed` |
| V3 | 现有埋点共 9 处：`AgentChatServiceImpl.logProviderIssue`（operation/stage/provider/durationMs/cause）、fail-closed warn（sessionId/turnNo/mode/count）、memory 关闭 info、memory 异常 warn、`AgentToolCoordinator.logToolCall`、`AgentToolExecutor.logOutcome`、`AgentGuardrailDowngrade.trace`、三个 checker 的开关/异常日志、`MySqlMemoryPort` debug | `confirmed` |
| V4 | **`AgentGuardrailDowngrade.trace(path, sessionId, turnNo, verdict)` 的形参已含 sessionId/turnNo，但两个调用点（`applyReplyGuardrail` / `applyMaterialGuardrail`）全部传 null**。即回复与素材路径的降级痕迹**当前无法关联到会话与轮次** | `confirmed` |
| V5 | 三个 guardrail checker 内部的 fail-closed（`CHECK_ERROR`）日志无 sessionId | `confirmed` |
| V6 | `AgentStageDecision` 的 `Reason` 枚举（ADVANCE / REASK / USER_FINISH_INTENT / TURN_LIMIT_REACHED / CLOSED）**已存在且注释明写「便于后续可观测（C5）复用」，但当前从不落库、不进日志** | `confirmed` |
| V7 | provider 调用耗时**只在失败路径**被计算并打印（`logProviderIssue`）。成功路径的 `startedAt` 被丢弃，无耗时痕迹 | `confirmed` |
| V8 | `AgentModelClient` **不解析 provider 返回的 `usage`**（token 用量）。全仓 `prompt_tokens` / `total_tokens` 零命中 | `confirmed` |

### 2.2 已有的可复用资产

| # | 事实 | 状态 |
|---|---|---|
| V9 | `AgentGuardrailVerdict.metrics()` / `reason()` 已是**脱敏后可直接持久化**的形态（coverage / maxUncoveredRun / checkedLength + 结构化短标识） | `confirmed` |
| V10 | `AgentToolArgsDigest` 是既有「摘要而非原文」范式：`text:len=<n>,sha256=<12 位前缀>`，算法不可用时返回 `unavailable`，**绝不回退原文** | `confirmed` |
| V11 | 结构化枚举齐备：`AgentGuardrailViolation`（6 值）、`AgentToolValidationResult` 的 10 个 rejectReason 常量、`AgentToolCallStatus`（5 值）、`AgentStage`（8 值）、`AgentChatMode`（2 值） | `confirmed` |
| V12 | 一轮对话的天然关联键已存在：`(session_id, turn_no)` + `agent_session.purpose` | `confirmed` |
| V13 | 探针范式可复用：`C3RealProviderProbeTest` / `C4RealProviderProbeTest`，`@EnabledIfEnvironmentVariable` 门控，只打印指标不打印原文；`pom.xml` 无 surefire excludes，纯靠 env 门控 | `confirmed` |

### 2.3 现有持久化能否承载 trace

| # | 事实 | 状态 |
|---|---|---|
| V14 | `agent_tool_call` 已覆盖**工具路径**的 action/observation：`tool_name` / `turn_no` / `args_digest` / `status` / `failure_type` / `result_summary` / 时间戳 | `confirmed` |
| V15 | **但它只覆盖工具路径**。回复路径与素材路径的降级（`reply:*` / `reply-attribution:*` / `material`）**没有任何持久化**，只在 warn 日志里 | `confirmed` |
| V16 | guardrail 的数值指标（coverage / maxUncoveredRun / checkedLength）**完全不落库** | `confirmed` |
| V17 | **thought 侧全部不落库**：阶段判定 reason、记忆命中数与片段长度、prompt 组装规模、provider 耗时 | `confirmed` |
| V18 | `agent_message` **不能**承载多条痕迹：唯一键 `uk_agent_message_session_turn_role` 限制同 session+turn+role 只能一行，且该键是 C1 失败重试幂等的实现基石，**不得改动** | `confirmed` |
| V19 | 新表需同步三处：`backend/sql/mysql/<新脚本>.sql`、`backend/sql/mysql/schema.mysql.sql`、`backend/src/test/resources/schema.sql` | `confirmed` |

### 2.4 查询侧与鉴权现状

| # | 事实 | 状态 |
|---|---|---|
| V20 | `/admin/**` 的鉴权链路**已完整可用**：`WebMvcConfig` 已注册路径、`JwtAuthenticationInterceptor` 对 `/admin` 前缀强制 `AuthRole.ADMIN`，否则 `ForbiddenException` | `confirmed` |
| V21 | 但 `com.flashback.controller.admin` **只有 `package-info.java`，无任何 controller**——`/admin/**` 是一条已铺好却从未使用的路 | `confirmed` |
| V22 | **`AuthRole.ADMIN` 没有任何签发路径**：`UserServiceImpl.buildLoginResponse` 固定签 `AuthRole.USER`，全仓无其他 `createToken` 调用点。即**当前无法通过正常登录取得 ADMIN token**——admin 端点若做出来，只能在集成测试里手工造 token，真实环境不可达 | `confirmed` |
| V23 | `AGENTS.md` Non-Negotiable：「不改 deployment、monitoring、admin portal…（除非独立 change 明确纳入）」。C5 若要用 `/admin/**`，须在本 proposal 显式纳入并获批 | `confirmed` |
| V24 | 除 `/actuator/health` 出现在拦截器白名单外，`application*.yml` 无 actuator 配置，无其他运维端点 | `confirmed` |

### 2.5 范围外

| # | 事实 | 状态 |
|---|---|---|
| V25 | Eval 框架（黄金集 + runner + 回归） | `out_of_scope`（C6，蓝图 v1.2 草案 §4.4） |
| V26 | 错误分类、降级模板、多 provider 路由 | `out_of_scope`（C7） |
| V27 | 时间距离话术、记忆衰减、周期模式 | `out_of_scope`（C8） |
| V28 | R2 引导话术与素材合成质量优化 | `out_of_scope`（用户已明确延后到 C1–C5 全部完工后统一处理） |
| V29 | R9 检索相关性升级（权重 / 分词 / 向量） | `out_of_scope`（独立 change） |
| V30 | 用户可见的分析页、情绪轨迹、实时告警、APM 大盘、A/B 框架 | `out_of_scope`（蓝图 C5 非目标 + `AGENTS.md`） |

> **诚实性声明**：V22 是本刀最重要的一条事实发现，它直接决定 N4 的答案——「做一个 admin 查询端点」听起来顺理成章，但在当前认证实现下它**在真实环境不可达**。V4/V5 是既有埋点的真实缺陷（不是新需求），本刀顺手补齐属于 C5 的天然范围。V8 标 `confirmed` 但含义是「没有 token 用量数据可记」，若要记须新增解析——见 N6。

---

## 3. 两处必须由用户裁决的岔路

### 3.1 痕迹存哪里（P7，蓝图 §2.2 指定在 C5 design 回答）

**这里存在一处与蓝图缓解措施的真实张力，必须先讲清。**

蓝图 v1.1 §4 C5 的风险栏写着：「存储方案复杂度 → **MVP 可用结构化 JSON 日志文件，不需要专用存储**」。但同一张卡片的目标 2 与验收项 2 要求「提供开发者**可查询**的接口或日志解析工具」、「至少 3 个场景的链路追踪示例」。

这两条在当前工程条件下**不能同时便宜地成立**：

- 本地是 Windows + 单机 MySQL，**没有 ELK / Loki / 任何日志聚合**（V1、V24）。JSON 日志文件要「按 session 查询」，意味着要自己写一个日志解析工具去 grep + 拼接。
- 更关键的是 C6 的依赖。蓝图 v1.2 草案 §4.3 要求 C5「为 C6 预留 `trace_id` / `prompt_version` / `policy_version` / `model` 字段」，C6 要拿这些字段做**变更前后的回归对比**。落在文件里，对比就得靠解析脚本；落在表里，一条 SQL 就够。

| 方向 | 做法 | 代价 |
|---|---|---|
| **A｜新建 MySQL 表** | 新建 `agent_trace_*`，随 session/user 级联删除 | 多一张表 + 一次 DDL（含 DDL 的 change 有流程教训，须列为实现期第一步）；写入放大（每轮多 1 次 insert） |
| **B｜结构化 JSON 日志文件**（蓝图原缓解） | 新增 logback appender 输出单行 JSON | 「可查询」要自己写解析工具；C6 的字段关联要靠解析；**且会新增 logback 配置**，触碰「不改 monitoring」的边缘 |
| **C｜扩 `agent_tool_call` 的列** | 复用既有表 | **不可行**：该表只在有工具提议时才有行（V14/V15），回复路径与 thought 侧根本没有行可挂；且回看模式完全无工具 |

**我的推荐：A。** 决定性理由是 C 已被事实排除，而 A 与 B 的差别落在「可查询」这个**验收项本身**上——B 要额外造一个解析工具才能达到 A 天然具备的能力，总成本更高而不是更低。蓝图那条缓解写在 C5 还很远的时候，当时不知道 C6 会要求字段级关联。

**这是对已冻结蓝图缓解措施的一处偏离，按 `AGENTS.md` 不能由我自行决定。** 若用户选 B，我按 B 做，但请知悉「可查询」的实现成本会转移到解析工具上。

### 3.2 「可查询」以什么形式暴露（蓝图验收项 2）

V22 让这件事的答案和直觉相反。

| 方向 | 做法 | 代价 |
|---|---|---|
| **A｜只落库，不加端点** | 提供仓内只读查询 SQL 脚本 + 集成测试演示按 session 取链路 | 「查询接口」变成「查询脚本」，对蓝图验收项是弱化解释 |
| **B｜`/admin/agent/traces` 只读端点** | 复用既有 `/admin/**` 鉴权（V20） | **端点在真实环境不可达**（V22：无 ADMIN 签发路径）。要让它可达就得改认证签发逻辑——那是动鉴权，风险远超 C5 本身，且触碰 `AGENTS.md` admin portal 边界（V23） |
| **C｜`/api/agent/sessions/{id}/trace` 限本人** | 复用既有 JWT | **直接违反 C5 非目标**「不面向终端用户展示（不破坏朋友的交互感）」。即便加 debug 开关，也是在产品 API 上开了一个内部数据的口子 |

**我的推荐：A。** 理由是 B 会产出一个不可达的端点——那是死代码，而且为了让它可达要去动 ADMIN 签发，属于把认证风险塞进一个可观测 change。A 的「弱化」是措辞层面的：蓝图原文是「查询接口**或**日志解析工具」，只读 SQL 脚本 + 可复用的 mapper 查询方法落在「或」的后半句里，且集成测试会实证「按 session 取到完整链路」。

若用户认为端点必须有，我建议的次优是 **B 的窄版本**：只做 `/admin/**` 下的只读端点，**不改任何签发逻辑**，明确记录「本端点当前只能由集成测试与未来的 ADMIN 签发能力访问」——把不可达如实写进 spec 与 AGENT_LOG，不假装它能用。

---

## 4. Goals

本刀 SHALL 实现：

1. **每轮一条决策轨迹**：一次用户消息从进入到返回，其 thought / action / observation 全过程留下一条可读、可按会话查询的结构化痕迹。
2. **thought 侧补全**（当前完全空白，V17）：阶段判定 reason、记忆检索命中与注入规模、prompt 组装规模、模式（写作引导 / 回看）。
3. **action 侧补全**：provider 调用（模型、耗时、成功与否、异常类型）、tool_calls 数量与处置（含 fail-closed 丢弃）。
4. **observation 侧补全**：六层护栏各自的判定结论与数值指标、降级是否发生及发生在哪条路径、工具执行结果状态。
5. **补齐既有埋点缺口**：`AgentGuardrailDowngrade.trace` 的 sessionId/turnNo 不再传 null（V4）；checker 内部 fail-closed 可关联会话（V5）。
6. **可查询**：按 `sessionId` 取回该会话的完整决策轨迹，形式按 N4 定稿。
7. **隐私零妥协**：痕迹只含结构化标识、数值指标、长度与哈希前缀。**日记原文、对话原文、记忆片段、候选文本一律不进痕迹**。
8. **为 C6 预留关联字段**：`trace_id` / `prompt_version` / `policy_version` / `model`（版本字段的产生方式见 N6）。
9. **至少 3 个场景的链路示例**（蓝图验收项）：正常成一轮、护栏降级一轮、provider 失败一轮。
10. **开关与保留**：可 backend-side 配置关闭；关闭时留痕说明未生效（沿用 C3a memory 开关的既有语义，不静默）。

---

## 5. Non-Goals（本刀明确不做）

- **不改 Agent 任何对话行为**：阶段推进、prompt 内容、护栏阈值、记忆检索、工具白名单、回看逻辑**一行不改**。C5 只挂痕迹。
- **不做 Eval 框架**（C6）：不建黄金集、不做 runner、不做 judge。本刀只**预留**关联字段。
- **不做错误分类与降级模板**（C7）、**不做时间智能**（C8）。
- **不做用户可见的任何东西**：**前端零改动**。不做分析页、情绪轨迹、trace 展示。
- **不做实时告警、APM 大盘、A/B 框架、外部 telemetry SaaS 接入**。
- **不改认证与签发逻辑**：不新增 ADMIN 签发路径、不改 `JwtTokenProvider` / `UserServiceImpl` / 拦截器（V22 的后果如实记录，不靠改认证绕开）。
- **不改 deployment / monitoring 配置**：不引入 logback-spring.xml、不接日志聚合、不配 actuator（若 N1 选 B 则此项需用户一并放开）。
- **不动 R2**（引导与素材质量）、**不动 R9**（检索相关性）、**不放宽任何护栏阈值**。
- **不改 `uk_agent_message_session_turn_role`**（V18，C1 幂等基石）。
- **不做大规模 backend rewrite**：不重构既有编排骨架，痕迹以「收集器 + 单一落库点」方式挂载。
- **不引入新依赖**：`pom.xml` / `package` / lockfile 不改。
- **不改三 Tab、不改用户可见命名**。
- **不把日记原文 / 对话原文 / 记忆片段写入痕迹、日志或外发**。
- **不做 speech-to-text / voice AI / 情绪评分 / 诊断 dashboard**。

---

## 6. 用户故事

> C5 的「用户」是开发者，不是产品用户。这一点本身就是它的非目标之一。

**改前**：用户报「Agent 回复很奇怪」或「点开对话报系统异常」。开发者手上有：一条通用 500、几行分散的 warn 日志、以及日志里那些**传了 null 的 sessionId**（V4）。要复现就得改代码加打印、重启、再让用户复现一次——C3b 的排查过程就是这样，而根因只是本地缺一列。

**改后**：拿到出问题的 sessionId，一条查询取回那几轮的轨迹：第 3 轮走的是回看模式、记忆命中 2 条共 88 字、prompt 组装 6 条消息、provider 耗时 2840ms 成功、模型返回 1 个 tool_calls 被 fail-closed 丢弃、时间归属检查 `maxUncoveredRun=15` 判定 `missing-time-attribution`、回复被替换为本地兜底。问题在第一分钟落到具体那一步，且**全程没有一个字的日记原文**。

---

## 7. 场景边界（隐私 + 气质对齐）

| 场景 | 期望行为 |
|---|---|
| 正常完成一轮写作引导 | 留下一条完整轨迹：模式、阶段判定 reason、记忆规模、prompt 规模、provider 耗时、护栏各层通过、无降级 |
| 回复被护栏降级为兜底 | 轨迹标明降级路径与违规类型 + 数值指标，且可区分「本地兜底」与「provider 正常产出」 |
| provider 失败 | 轨迹记异常类型与耗时；**用户消息仍保留可重试的既有语义不变** |
| 同轮重试 | 轨迹能看出这是同一轮的第二次尝试，而不是新的一轮 |
| 回看中模型返回 tool_calls | 轨迹记下 fail-closed 丢弃事件（**这正好补上 C3b 未活体触发的那条残余**） |
| 记忆检索无命中 | 轨迹记命中 0，与「记忆开关关闭」可区分 |
| 记忆开关被关闭 | 轨迹显式记「未生效」，不表现为无命中（沿用 C3a 既有语义） |
| 可观测开关被关闭 | 行为等价于引入 C5 之前，且留痕说明未生效，不静默 |
| 轨迹里出现日记原文 | **不允许**——须有测试直接断言痕迹字段不含原文 |
| 终端用户请求看轨迹 | 产品 API 不暴露；用户不被告知任何内部判定过程（沿用 C4 既有条款） |
| 用户被删除 | 其轨迹随之级联清理 |
| 轨迹写入自身失败 | **fail-open**：不得让「记不下痕迹」把用户这一轮对话搞挂 |
| 轨迹表持续增长 | 有可配置的保留期与清理手段（不引入定时任务，见 N7） |

---

## 8. 待用户在规划闸确认（N1–N7）

| # | 决策项 | 候选 | 我的推荐 |
|---|---|---|---|
| **N1** | 痕迹存哪里（P7，§3.1，**含对蓝图缓解措施的偏离**） | (a) 新建 MySQL 表；(b) 结构化 JSON 日志文件（蓝图原缓解）；(c) 扩 `agent_tool_call` | **(a)**。(c) 已被事实排除（回复路径与 thought 侧无行可挂，回看更是完全无工具）；(b) 要额外造解析工具才能达到 (a) 天然具备的「可查询」，且会新增 logback 配置触碰 monitoring 边界。**但这是偏离已冻结蓝图缓解措施，须您裁决** |
| **N2** | 记录粒度 | (a) 每轮一条聚合记录，步骤明细以结构化 JSON 存单列；(b) 每个步骤一行事件；(c) 两级（轮次表 + 事件表） | **(a)**。理由是埋点必须收敛：(b) 会让 7~9 个 insert 散落在编排各处，重演 C4 决策 5 修掉的「规则分散」问题，且 provider 失败等早退路径极易漏写。(a) 用一个 per-turn 收集器兜住全部步骤，**落库只有一个出口**。(c) 是 (b) 的加强版，代价同理且多一张表 |
| **N3** | 采样策略（蓝图缓解提到「可配置采样率」） | (a) 默认全量，只提供总开关；(b) 默认全量 + 可配采样率；(c) 默认采样 | **(a)**。采样的坏处很具体：排查时最想看的那一轮，恰好可能没被采到。当前是单人本地开发 + 单机 MySQL，每轮一行的写入量完全不构成问题。**这也是对蓝图缓解措施的一处轻微偏离**，一并请您确认 |
| **N4** | 「可查询」怎么暴露（§3.2） | (a) 只落库 + 只读查询脚本 + 集成测试演示；(b) `/admin/agent/traces` 只读端点；(c) 产品 API 下的本人 trace 端点 | **(a)**。(c) 直接违反 C5 非目标；(b) 因 V22 会产出**真实环境不可达的端点**，要让它可达就得改 ADMIN 签发——把认证风险塞进可观测 change，不划算。若您坚持要端点，建议 B 的窄版本并**如实记录不可达**，不假装可用 |
| **N5** | 是否补齐 V4/V5 的既有埋点缺口 | (a) 补（纳入 C5）；(b) 不补，另开 Type B | **(a)**。V4 是「降级痕迹关联不到会话」，它正是 C5 要解决的问题本身，放到别处等于把 C5 做一半。改动是给两个调用点传已有的变量，风险极低 |
| **N6** | `prompt_version` / `policy_version` 怎么产生（C6 依赖） | (a) 手工维护版本常量，改文案时人工 bump；(b) 由 prompt 模板与规则文案**内容哈希**自动派生短版本；(c) 本刀不做，留给 C6 | **(b)**。(a) 的失效方式很典型——改了文案忘了 bump，于是 C6 拿到「版本没变但行为变了」的脏数据，比没有版本更糟。(b) 让版本与内容强绑定，改文案自动变。(c) 会让 C6 无法关联，违背蓝图对 C5 的预留要求 |
| **N7** | 轨迹的保留与清理 | (a) 级联删除 + 可配保留天数 + 手动清理 SQL 脚本；(b) 加定时任务自动清理；(c) 不做清理 | **(a)**。(b) 要引入调度，触碰「不改 deployment / monitoring」；(c) 在单机 MySQL 上迟早变成问题。(a) 把策略讲清、把工具给到，执行时机交给人 |

### 附带需在规划批准时一并确认的事

- **DDL 前置**（若 N1 选 a）：按 C3b 的流程教训，「本地执行 DDL」必须是**实现期第一步**，不是联调前置。C3b 就是因为把 DDL 留到联调前，导致手验时写作引导对话一起 500。
- **闸门 3 是否需要**：C5 前端零改动、对话行为不变，因此**不需要微信真机手验**。真实 provider 调用只用于确认「真实链路下轨迹是否完整、耗时字段是否合理、fail-closed 是否被真实触发」，申请 **≤ 10 次**。若您认为 mock 已足够，可不开闸门 3——请明确。
- **是否顺带解析 provider `usage`**（V8）：token 用量对 C6 的成本回归有价值，但它需要动 `AgentModelClient` 的响应解析。我倾向**本刀不做**，在 trace 里留好字段位置，等 C6 真正需要时再填。请确认这个取舍。

---

## 9. 外调预算

| 阶段 | 外调 | 预算 |
|---|---|---|
| 规划闸（本阶段） | 无 | **0** |
| 实现（闸门 2 后） | 全部走 `app.ai.provider=mock` + 单元/集成测试 | **0** |
| 联调（闸门 3 单独授权后） | 真实链路下轨迹完整性、耗时字段合理性、fail-closed 是否活体触发 | **≤ 10 次** |

**本地环境提醒**：

- MySQL80 `StartType=Manual`
- C1/C2/C3 增量 DDL 均已执行完毕；**本刀若新增 DDL，须在实现期第一步执行并验证幂等**
- **R6 凭证轮换仍待用户执行**（`AI_API_KEY` / `S3_*` / `WECHAT_MINI_PROGRAM_SECRET`）；建议闸门 3 前完成。轮换后可删 `backend/start-dev-wechat.local.ps1.bak`（含旧明文，已 gitignore）

`git push` / 部署 / 发布：本 change **不申请**。

---

## 10. 提交责任

**用户手动提交**（默认）。除用户当轮明确授权外，Agent 不执行 `git add` / `commit` / `push`。

---

## 11. 验收标准

### 轨迹完整性

1. 一轮正常写作引导对话产生一条完整轨迹，含模式、阶段判定 reason、记忆规模、prompt 规模、provider 结果与耗时、护栏各层结论。
2. 一轮回看对话同样产生完整轨迹，且能区分模式。
3. provider 失败的一轮留下轨迹（异常类型 + 耗时），且**既有失败重试语义完全不变**（用户消息保留、Agent 回复不落库、同轮可重试不重复计数）。
4. 同轮重试可从轨迹中辨识，不被误记为新的一轮。
5. 回看中 tool_calls 被 fail-closed 丢弃时，轨迹记下该事件（**C3b 残余的补位**）。
6. 记忆「无命中」与「开关关闭」在轨迹中可区分。
7. 至少 3 个场景的链路示例有测试实证：正常轮、护栏降级轮、provider 失败轮。

### thought / action / observation 三段齐备

8. thought：阶段判定 reason 落痕（V6 的枚举首次被使用）。
9. action：provider 调用的模型、耗时、成功与否落痕；**成功路径的耗时不再被丢弃**（V7）。
10. observation：六层护栏（忠实度 / 诊断代决 / 伪引用 / 时间归属 / 长度 / 工具参数忠实）各自的结论与数值指标落痕。
11. 降级可区分「本地兜底」与「provider 正常产出」（沿用 C4 既有条款，不得回退）。

### 既有埋点缺口补齐

12. `AgentGuardrailDowngrade.trace` 的两个调用点传入真实 sessionId 与 turnNo，不再为 null（V4）。
13. checker 内部 fail-closed（`CHECK_ERROR`）可关联到会话（V5）。

### 隐私（零妥协）

14. 轨迹字段**不含**日记原文、对话原文、记忆片段内容、候选文本、未覆盖片段内容。**须有测试直接断言**，而非仅靠代码审查。
15. 涉及文本的字段只以长度 + 哈希前缀表达（沿用 `AgentToolArgsDigest` 范式，V10）。
16. 终端用户不被暴露任何内部判定过程；产品 API 不返回轨迹。
17. secret 未进入痕迹、前端代码或 tracked files。

### 可查询与可配置

18. 按 `sessionId` 可取回该会话完整轨迹（形式按 N4 定稿），有测试实证。
19. 可观测开关关闭时行为等价于引入 C5 之前，且留痕说明未生效，**不静默**。
20. 轨迹写入失败时 **fail-open**：用户这一轮对话正常完成，有测试。
21. 用户被删除时其轨迹级联清理（若 N1 选 a）。
22. `trace_id` / `prompt_version` / `policy_version` / `model` 字段就位；版本字段按 N6 定稿方式产生，且有测试证明「改文案 → 版本变化」（若选 b）。

### 范围守护

23. **Agent 对话行为零改动**：阶段推进、prompt 文案、护栏阈值、记忆检索、工具白名单、回看逻辑均未修改，可由 diff 与既有测试全绿共同证明。
24. **前端零改动**。
25. 未改认证与签发逻辑（V22 如实记录，不靠改认证绕开）。
26. 未做 C6 Eval / C7 韧性 / C8 时间智能；未动 R2 / R9；未放宽任何阈值。
27. 未改 `uk_agent_message_session_turn_role`；未引入新依赖；`pom.xml` / `package` / lockfile 未改。
28. 未引入 logback 配置 / 日志聚合 / actuator（若 N1 选 b 则按您放开的范围执行）。

### 回归

29. 后端既有测试全绿：**496 tests PASS / 2 skipped 基线**，既有断言**零修改**。若某条必须改，须停下请示。
30. 若新增 DDL：脚本幂等、已在本地 MySQL 执行并验证、`schema.mysql.sql` 与 `src/test/resources/schema.sql` 三处同步（V19）。

### 闸门 3（若授权）

31. 真实链路下轨迹完整性实测。
32. 耗时字段在真实 provider 下的量级合理（与 C3b 观察到的秒级响应对齐）。
33. fail-closed 若仍未活体触发，**诚实记为未活体验证**，不得写成已验证。

---

## 12. 建议实现顺序

1. 规划闸批准（N1–N7 定稿）。
2. **若含 DDL：第一步就在本地 MySQL 执行并验证幂等**（C3b 流程教训），同步 `schema.mysql.sql` 与测试 `schema.sql`。
3. 痕迹收集器 + 单一落库出口（N2 的硬约束：埋点不散落）。
4. thought 侧接入：模式、阶段 reason、记忆规模、prompt 规模。
5. action 侧接入：provider 结果与耗时（含成功路径，V7）、tool_calls 处置与 fail-closed。
6. observation 侧接入：六层护栏结论与指标；**顺手补齐 V4/V5**。
7. 版本字段（N6）+ `trace_id` 生成。
8. 开关、fail-open、保留期与清理手段（N3/N7）。
9. 查询侧（N4 定稿形式）+ 3 个场景的链路示例测试。
10. 隐私断言测试（验收 14 是本刀最硬的一条）。
11. 回归 496 → spec delta → 闸门 3（若授权）→ 收口。

---

## 13. spec delta 落点

| spec | 内容 |
|---|---|
| `agent-runtime` | **ADDED** 决策轨迹条款（三段齐备、隐私边界、fail-open、开关不静默、版本字段）；**MODIFIED 四条**「C2/C4/C3a/C3b 范围内的可观测能力」scenario——它们目前都写着「决策链路可查询 SHALL 留给后续独立 change」，C5 落地后须改为指向本刀条款 |
| `backend-core` | 轨迹的持久化与查询契约、级联清理、配置约束、日志隐私条款的延伸 |
| `agent-collaboration` | 可观测规范：痕迹的脱敏要求与「痕迹只服务开发者」的协作约束（蓝图 §5 指定落点） |
| `v2-product-scope` | 一条产品边界：可观测能力 SHALL NOT 面向终端用户呈现 |
| `miniapp-core` | **无 delta**（前端零改动） |

> 四条 MODIFIED scenario 是本刀 delta 的主要工作量，且它们分散在 `agent-runtime` 的四个「Accepted From」段落里，须逐条核对，不能漏。

---

## 14. 关键风险

| 风险 | 缓解 |
|---|---|
| **隐私泄漏——痕迹里混入原文**（本刀最高风险） | 沿用 `AgentToolArgsDigest` 的「长度 + 哈希前缀」范式；痕迹字段类型只允许结构化标识与数值；验收 14 要求**测试直接断言**而非代码审查 |
| **埋点散落导致早退路径漏记**（provider 失败、同轮重试、fail-closed 都是早退） | N2 定为「收集器 + 单一落库出口」；早退路径各有独立验收项（3/4/5） |
| **痕迹写入失败反而搞挂对话** | fail-open 硬约束 + 验收 20 的测试；沿用 C3a 记忆检索 fail-open 的既有做法 |
| **DDL 未执行导致真机报错**（C3b 已踩过，且当时波及既有功能） | 列为实现顺序第 2 步而非联调前置；脚本幂等；三处 schema 同步 |
| **「可查询」被做成不可达的端点**（V22） | N4 推荐 (a)；若选 (b) 则如实记录不可达，不改认证绕开 |
| **与蓝图缓解措施偏离（存储 + 采样）** | §3.1 与 N1/N3 显式呈现张力并请示，不自行决定 |
| **顺手改了 Agent 行为** | 验收 23 要求 diff 与既有测试全绿共同证明；496 基线既有断言零修改 |
| **版本字段靠人 bump 而失效** | N6 推荐内容哈希派生，并要求测试证明「改文案 → 版本变」 |
| **轨迹表增长** | N7 的保留期 + 清理脚本；级联删除 |
| **写入放大影响响应耗时** | 每轮 1 次 insert；可配开关；若实测有影响则在联调中记录并请示 |
