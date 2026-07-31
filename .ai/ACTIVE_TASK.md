# Active Task

## Status

`IDLE`

当前无活动 Type C change。开始新的重大实现前，必须先创建 OpenSpec change 并更新本文件。

**Phase 1（M4 → C1 → C2 → C4 → C3a → C3b → C5）已全部完成。**
**蓝图 v1.2 已于 2026-07-30 校准并冻结。**
下一动作是 **C6 `agent-eval-framework` 规划闸（闸门 1）**，见下文 Direction Layer。

## Previous Completed

- Change：`agent-observability`（C5，Phase 1 收官刀）
- 位置：`openspec/changes/archive/2026-07-30-agent-observability/`
- 结果：Agent 决策轨迹（thought → action → observation）落地并归档。delta 已接受进 baseline：
  `agent-runtime`（四条「范围内的可观测能力」scenario 修订 + 8 条新增）、
  `backend-core`（7 条 + 一条 Type B 超时条款）、`agent-collaboration`（3 条，该 spec 首次承载产品 Agent 条款）、
  `v2-product-scope`（2 条）；`miniapp-core` 无 delta（前端零改动）
- 验证：后端 **534 tests PASS / 3 skipped**（496 基线 + 37 新增，零回归；3 skipped 为环境门控的真实 provider 探针）；
  **既有断言零修改**；本地 DDL 已执行且幂等已验证
- 闸门 3 已执行：真实调用 **6 次 / 预算 10**。轨迹三段齐备；耗时 min 4571 / avg 6476 / max 8467ms；
  真实产出下隐私复核 `leaked=false`；**fail-closed 仍未活体触发，如实记为未验证**
- **归档后随即修掉一个 Type B**：手验报 `request: fail timeout`（详见下文 Residual 与 AGENT_LOG）
- 更早：`agent-review-chat`（C3b）、`agent-memory-retrieval`（C3a）、`agent-guardrails-hardening`（C4）、
  `agent-tool-calling`（C2）、`agent-runtime-mvp`（C1）、`m4-real-capability-completion`

## Direction Layer

- **当前权威蓝图**：`Docs/agent-iteration/roadmap/iteration-blueprint.md` **v1.2 已冻结**（2026-07-30）
- 主线进度：M4 → C1 → C2 → C4 → C3a → C3b → **C5 已归档，Phase 1 收官**
- **Phase 2 定案序**：`C6 agent-eval-framework` → `C7 agent-reflection-loop` →
  `C8 agent-resilience` → `C9 agent-temporal-intelligence`（一次一个 ACTIVE）
- **下一动作：C6 规划闸**。建 `openspec/changes/agent-eval-framework/`，写
  proposal / design / tasks + delta，**闸门 1 待用户批准**。开工前读蓝图 §4.2 意图卡片
- **Optional（不排主线）**：C0 平台升级（Boot 4.x/Java 21，Phase 2 完工后再议）、
  C10 语气标定、C11 上下文架构——均需证据触发
- **对外叙事文档**：`Docs/agent-iteration/narrative/agent-tech-story.md`（D33：每刀归档时更新对应段落；
  §7 待 C6 补、§8 待 C7 补）

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

## Source Of Truth (when IDLE)

- `AGENTS.md`
- `Docs/agent-iteration/roadmap/iteration-blueprint.md`（**v1.2 已冻结**；Phase 2 方向与意图卡片）
- `openspec/project.md`
- `openspec/specs/agent-runtime/spec.md`（含 C1 + C2 + C4 + C3a + C3b + **C5**，Agent 核心契约）
- `openspec/specs/backend-core/spec.md`（含 M4 + C1 + C2 + C4 + C3a + C3b + **C5**）
- `openspec/specs/miniapp-core/spec.md`（含 M4 + C1 + C2 + C3b；**C5 无 delta**）
- `openspec/specs/v2-product-scope/spec.md`（含 M4 + C1 + C2 + C4 + C3a + C3b + **C5**）
- `openspec/specs/agent-collaboration/spec.md`（含 **C5**）
- 开工清单：`Docs/agent-iteration/workflow/prompt-snippets/type-c-checklist.md`

## Current Progress

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
- **Blocked on**: none
- **Next step**: **开 C6 `agent-eval-framework` 规划闸**。建 change 目录写
  proposal / design / tasks + delta 建议 → 闸门 1 待批准。
  开工前读蓝图 §4.2（含 6 项目标、8 个维度表、快照分层、P8）

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
  正确性仅由单测覆盖。属概率性行为，**不单开 change**；C5 已做到「它真发生时能被记下」
- **[待用户决定] `schema.mysql.sql` 落后于增量脚本**：补齐它需要同时补 C2 + C3 + C5 三刀的表与列，
  属独立 Type B。C5 刻意未动（只加 C5 会造出「有 C5 表却无 C2 表」的更怪状态）
- **[新] 轨迹写在业务事务之后**：理论上存在「业务成功但轨迹丢失」的窗口（进程崩溃）。
  可接受——轨迹是辅助设施，而原方案的代价是每轮卡 50 秒
- **[R2] 引导话术与素材合成质量**：**已定案由 C6 重建基线后再谈**（D30/D32）。
  C5 的版本锚点使「优化前后可对比」第一次成为可能；在 C6 落地前不要动 prompt
  （宪法 §7.3：无 Eval 情况下大改 prompt 上线属禁止项）
- **[已关闭] 三个 Type B 的真机复验**：用户 2026-07-30 执行，全部 PASS
- **[已关闭] MySQL 上轨迹落库未联调**：已联调，并由此发现且修复了 50 秒锁等待根因
- **[R9] 检索相关性弱**：标签 + 说明性字段 LIKE，无权重 / 分词 / 向量。升级留独立 change
- **[R6｜待用户执行] 凭证轮换**：`AI_API_KEY` / `S3_ACCESS_KEY` / `S3_SECRET_KEY` / `S3_SESSION_TOKEN` /
  `WECHAT_MINI_PROGRAM_SECRET`。轮换后建议删除 `backend/start-dev-wechat.local.ps1.bak`（含旧明文，已 gitignore）
- **探针资产**：`C3RealProviderProbeTest`（`C3_REAL_PROBE=1`）、`C4RealProviderProbeTest`（`C4_REAL_PROBE=1`）、
  `C5RealProviderProbeTest`（`C5_REAL_PROBE=1`）。全部默认跳过。C5 探针另有一处形态差异：
  它是 `@SpringBootTest` 走完整 `sendMessage`，因为要验的是编排层有没有漏采集
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

## 未跟踪 / 未提交产物（不要擅自提交或移动）

- `Docs/agent-iteration/architecture/`（三份文件；v1.2 冻结时已校准，仍未 tracked）
- `Docs/agent-iteration/narrative/agent-tech-story.md`（**新建，v1.2 收尾产物**）
- `.kiro/skills/`
- `Docs/agent-iteration/roadmap/iteration-blueprint-v1.2-draft.md`
  （**内容已迁入 `iteration-blueprint.md` v1.2，可由用户手动删除**；Agent 不动未跟踪文件）
- v1.2 冻结带来的未提交改动：`AGENTS.md`、`openspec/project.md`、
  `.kiro/steering/{rules,structure,product,tech}.md`、`Docs/agent-iteration/README.md`、
  `roadmap/README.md`、`roadmap/iteration-blueprint.md`、`workflow/agent-control-model.md`
- `backend/run-c5-probe.local.ps1`（已 gitignore；闸门 3 探针运行脚本）
- `backend/probe-turn-latency.local.ps1`（已 gitignore；一轮耗时排查脚本，
  签本地 dev JWT 打真实链路。定位 50 秒锁等待时用的就是它，保留供将来复用）

## Out Of Scope While Idle

- 不要在没有新 Type C 的情况下改 Agent runtime / 工具 / 护栏 / 记忆 / 回看 / 轨迹 / AI 业务代码
- **C6 须先过闸门 1**：可以建 change 目录写 proposal / design / tasks，但**规划批准前禁止改业务代码**
- **不要动 prompt / 护栏阈值 / 引导策略**：R2 的基线要等 C6 建好（宪法 §7.3）
- 不要并行复活已归档 change 作为隐式 active change
- 不要在未获授权时发起真实 provider 调用（探针默认门控跳过，勿擅自设置 `C*_REAL_PROBE=1`）
- 不要修改 `openspec/changes/archive/**`——归档即历史，含其中的 v1.1 引用
