# Active Task

## Status

`IDLE`

当前无活动 Type C change。开始新的重大实现前，必须先创建 OpenSpec change 并更新本文件。

**Phase 1（M4 → C1 → C2 → C4 → C3a → C3b → C5）已全部完成。**
下一动作是**蓝图 v1.2 校准会**（不是新 Type C），见下文 Direction Layer。

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

- **当前权威蓝图**：`Docs/agent-iteration/roadmap/iteration-blueprint.md` **v1.1 已冻结**
- 主线进度：M4 → C1 → C2 → C4 → C3a → C3b → **C5 已归档，Phase 1 收官**
- **下一动作：蓝图 v1.2 校准会**。v1.2 草案（`iteration-blueprint-v1.2-draft.md`，**未跟踪文件**）
  §0.2 与 §8 列了校准清单，要点：
  1. 用 C3 / C5 的归档事实重刷「Phase 1 完成事实」表与漂移登记
  2. 删除已被证伪的假设（本轮实测证伪了两条：`schema.mysql.sql` 三处同步、`/admin` 端点可行性）
  3. 用户确认 Phase 2 默认顺序（草案建议 C6 → C7 → C8）
  4. 校准后提升为正式 v1.2 并更新 `AGENTS.md` / 本文件的 Direction 引用
- **C6 不要偷跑**：草案 §10 明确要求先冻结 v1.2

## Source Of Truth (when IDLE)

- `AGENTS.md`
- `openspec/project.md`
- `openspec/specs/agent-runtime/spec.md`（含 C1 + C2 + C4 + C3a + C3b + **C5**，Agent 核心契约）
- `openspec/specs/backend-core/spec.md`（含 M4 + C1 + C2 + C4 + C3a + C3b + **C5**）
- `openspec/specs/miniapp-core/spec.md`（含 M4 + C1 + C2 + C3b；**C5 无 delta**）
- `openspec/specs/v2-product-scope/spec.md`（含 M4 + C1 + C2 + C4 + C3a + C3b + **C5**）
- `openspec/specs/agent-collaboration/spec.md`（含 **C5**）
- 方向：`Docs/agent-iteration/roadmap/iteration-blueprint.md`（已冻结 v1.1）
- 开工清单：`Docs/agent-iteration/workflow/prompt-snippets/type-c-checklist.md`

## Current Progress

- **Last session**: 2026-07-30 — C5 全流程完成并归档，另修一个 Type B
  - 规划：30 条现状事实（V1–V30）、11 条决策、四份 delta；N1–N7 按推荐定稿
  - 实现：`agent/trace/` 五个类 + 实体/mapper/XML + DDL + 9 条排查查询 + 37 项测试
  - 既有缺陷补齐：**V4**（降级痕迹此前恒传 null sessionId/turnNo）；**V5** 改用轨迹解决，未动 checker 签名
  - 闸门 3 执行完毕；delta 接受进 baseline；归档；`ACTIVE_TASK` → IDLE
  - **Type B**：Agent 请求超时修复（前端 30s / 后端 20s，顺序不可颠倒）
- **Blocked on**: none
- **Next step**: 蓝图 v1.2 校准会（Type A 讨论为主，不是新 Type C）。
  **须先由用户真机复验 Type B 修复**是否解决 `request: fail timeout`

## C5 的关键结论（对 v1.2 校准与 C6 有直接价值）

- **provider 耗时首次有数据**：min 4571 / avg 6476 / max 8467ms。这项数据 C5 之前完全不存在
  （成功路径的 `startedAt` 被直接丢弃）。它同时是 Type B 超时缺陷的定位依据，
  也是 C7（韧性）设计的输入
- **版本锚点由内容哈希派生**，改文案自动变化。C6 的回归比对可直接按
  `prompt_version` / `policy_version` 分组（`c5-trace-queries.sql` 第 7 条）
- **`AuthRole.ADMIN` 全仓无签发路径**（`UserServiceImpl` 固定签 `USER`）→
  `/admin/**` 下的端点在真实环境不可达。**任何未来 change 若打算做 admin 端点，须先解决签发问题**
- **`schema.mysql.sql` 只到 C1**：既无 `agent_tool_call`（C2）也无 `agent_session.purpose`（C3）。
  项目既有约定是全量脚本不随增量维护。**待用户决定是否另开 Type B 补齐**（见 Residual）
- 两处已获批的对已冻结蓝图的偏离：存储用 MySQL 表而非 JSON 日志文件；默认全量不采样

## Residual / Carry-over

- **[新｜待用户执行] Type B 超时修复的真机复验**：本地 `type-check` / `build:mp-weixin` / 后端回归均 PASS，
  但 `request: fail timeout` 是否消失**只能由真机确认**
- **[新｜待用户决定] `schema.mysql.sql` 落后于增量脚本**：补齐它需要同时补 C2 + C3 + C5 三刀的表与列，
  属独立 Type B。C5 刻意未动（只加 C5 会造出「有 C5 表却无 C2 表」的更怪状态）
- **[R10] 回看 fail-closed 仍未活体触发**：C3b 3 轮 + C5 6 轮共 9 轮观察，模型均未在无工具模式返回提议。
  正确性仅由单测覆盖。属概率性行为，**不单开 change**；C5 已做到「它真发生时能被记下」
- **[新] C5 探针写入 H2 而非 MySQL**：闸门 3 验的是「真实模型 + 真实编排 + 真实落库」，换掉的只是数据库实例。
  MySQL 上的轨迹落库未经真实联调，但 DDL 已执行且 mapper 无 H2 特有语法
- **[R2] 引导话术与素材合成质量**：**Phase 1 已完工，该项的延后条件已解除**。
  用户曾说「后面具体再聊」。C5 上线后第一次具备「优化前后可对比」的条件（按版本锚点分组）
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

## 未跟踪的非本轮产物（不要擅自提交或移动）

- `Docs/agent-iteration/architecture/`
- `Docs/agent-iteration/roadmap/iteration-blueprint-v1.2-draft.md`
- `.kiro/skills/`
- `Docs/agent-iteration/README.md` 与 `roadmap/README.md` 的未提交改动
- `backend/run-c5-probe.local.ps1`（已 gitignore）

## Out Of Scope While Idle

- 不要在没有新 Type C 的情况下改 Agent runtime / 工具 / 护栏 / 记忆 / 回看 / 轨迹 / AI 业务代码
- **不要并行开 C6**：草案 §10 明确要求先冻结 v1.2
- 不要并行复活已归档 change 作为隐式 active change
- 不要在未获授权时发起真实 provider 调用（探针默认门控跳过，勿擅自设置 `C*_REAL_PROBE=1`）
- 不要擅自把 v1.2 草案提升为冻结版本——校准会须用户参与并确认
