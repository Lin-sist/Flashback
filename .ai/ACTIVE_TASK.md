# Active Task

## Status

`ACTIVE`（规划闸 · 闸门 1 待批准）

## Current Change

- Change：`agent-review-chat`（C3 后半刀）
- 位置：`openspec/changes/agent-review-chat/`
- 开工锚点：`b76f221`
- 阶段：**规划闸产出完成，N1–N6 已定稿；闸门 2 实现授权待用户明确允许**。授权前禁止改业务代码
- 提交责任：**用户手动提交**（除用户当轮明确授权外，Agent 不执行 `git add` / `commit` / `push`）
- 外调预算：规划 0；实现 0（走 mock）；闸门 3 单独授权后 ≤ 20 次

## C3 拆分与进度

蓝图 C3 `agent-memory-and-review` 拆两刀（2026-07-29 用户批准，蓝图 §7 已登记）：

| 刀 | Change | 状态 |
|---|---|---|
| C3a | `agent-memory-retrieval` | **已归档** `archive/2026-07-29-agent-memory-retrieval/`，delta 已接受进 baseline |
| C3b | `agent-review-chat` | **本刀**，规划闸中 |

执行顺序：M4 → C1 → C2 → C4 → **C3a ✅ → C3b** → C5。

## 闸门 3 延后（重要）

**C3a 归档时用户明确同意跳过闸门 3**，改为 C3 两刀全部完成后**合并进行**一次真实联调。

后果已诚实登记，不得当作已验证：
- **R8**：时间归属阈值 `min-memory-only-run-for-attribution=8` 未经真实样本校准，误伤与拦截两方向均未活体验证，**已随 baseline 生效**
- **R7**（C4 遗留）：忠实度闸拦截方向未活体验证
- 合并联调时的观察项：C3a 的 T-20~T-23 + 本刀的联调项，预算合并计算

## C3a 已定稿、对本刀有约束力的决策

| # | 定稿 | 对本刀的含义 |
|---|---|---|
| Q4 | 复用 `agent_session` + `purpose=WRITING_GUIDANCE｜REVIEW_CHAT`；不另建表 | 列已建（默认 `WRITING_GUIDANCE`），**本刀无需 DDL**，只加行为分支 |
| Q4 | 回看**无阶段机**，自由多轮 + 轮次上限，`stage` 固定常量 | 不复用 `AgentStageMachine` 六阶段 |
| Q5 | `record-detail` 独立 `ReviewChatSheet`，与回应浮层**互斥**；可抽共享消息壳，不复用带工具确认的 `AgentChatSheet`；不做视觉重建 | 前端有改动，需 `type-check` + `build:mp-weixin` |
| Q6 | 回看**完全无工具**；delta 明示；Runtime 不挂 tools；冒出 tool call **fail-closed** | 见下方 carry-over 第 2 条的陷阱 |
| Q2 | 来源分层已落地，记忆层须带时间归属 | 回看同样复述历史，`AgentTimeAttributionChecker` 直接适用 |

## 本刀必须处理的 carry-over（来自 C3a closeout §5）

1. **`REVIEW_CHAT` 目前零行为分支**，且有测试 `AgentMemoryIntegrationTest.shouldNotCreateAnyReviewChatSession` 断言「不存在任何该用途会话」。本刀实现时**需要修改该断言**——属预期变更，不是回归，但必须在 AGENT_LOG 显式披露。
2. **`buildToolContext` 的陷阱**：它当前只按「有无 `recordId`」决定是否下发 tools。回看会话**恰好绑定一条记录**，所以「回看无工具」**不会自动成立**，必须按 `purpose` 显式短路，否则会给回看误发 tools。规划时须写进 tasks，否则实现期极易漏。
3. **DRAFT 硬校验须 MODIFIED**：`AgentChatServiceImpl.requireOwnedRecordIfPresent` 拒绝非 DRAFT，且 baseline `agent-runtime` 有条款 `Writing Guidance Must Target Draft Records Only`，C3a 的 delta 又新增了「C3a 范围内的回看对话」范围声明。这两处都要以 MODIFIED 修订，**不得悄悄放宽**。
4. **回看会话的轮次与结束语义**需自定义（无阶段机），不能借用写作引导的 `AgentStageDecision`。
5. **MemoryPort 复用**：`retrieve(MemoryQuery)` 签名已含 `purpose`，回看传 `REVIEW_CHAT`。**不得另起一套检索实现**。
6. **素材语义**：回看对象是已解锁记录，封存后 `location` / `attachments` / `cover` 不可变，正文亦不应被回看对话追加。本刀须明确回看**不产出可回填正文的素材**。
7. **R3**（C2 遗留）微信真机工具链路手验未走通，本刀有 UI 改动，**在本刀补齐**。

## Source Of Truth

- `AGENTS.md`
- `openspec/specs/agent-runtime/spec.md`（含 C1 + C2 + C4 + **C3a** 已接受条款）
- `openspec/specs/backend-core/spec.md`（含 M4 + C1 + C2 + C4 + **C3a**）
- `openspec/specs/miniapp-core/spec.md`（含 M4 + C1 + C2；**本刀会新增回看 UI 条款**）
- `openspec/specs/v2-product-scope/spec.md`（含 M4 + C1 + C2 + C4 + **C3a**）
- `openspec/specs/agent-collaboration/spec.md`、`openspec/project.md`
- 方向：`Docs/agent-iteration/roadmap/iteration-blueprint.md`（已冻结 v1.1，§7 含 C3 拆刀登记）
- 开工清单：`Docs/agent-iteration/workflow/prompt-snippets/type-c-checklist.md`

## N1–N6 已定稿（2026-07-29 用户按推荐批准）

| # | 定稿 |
|---|---|
| N1 | 同一 `AgentChatServiceImpl` + purpose 分支，**差异收敛到单一模式判定点**（不散落 `if`） |
| N2 | `stage` 用专用常量 `REVIEW`；结束 = 轮次上限或用户 `finish`；轮次上限单列配置默认 6 |
| N3 | 注入 `content` + `ai_summary` + `belief_then`（**不注入** `reality_later` / `reply`），全部进 MEMORY 层 |
| N4 | 时间归属阈值**不动**，靠合并联调实测；不为回看单开阈值，不在回看关掉该检查 |
| N5 | 复用 `POST /api/agent/sessions`，请求体加 `purpose`（缺省 `WRITING_GUIDANCE`） |
| N6 | `selectActiveByUserAndRecord` 补 purpose 谓词 |

## Current Progress

- **Last session**: 2026-07-29 —
  ① C3a 全流程完成并归档（分层来源 + 时间归属护栏 + MemoryPort + MySQL 检索 + 注入接入；472 tests PASS / 1 skipped；5 个 commit；T-01 覆盖率补测；delta 接受进 baseline）
  ② C3b 规划闸产出：`proposal.md`（现状 V1–V23 + 37 条验收）、`design.md`（**11 条决策记录**，按用户要求不铺架构图）、`tasks.md`（T-01~T-40，8 阶段 + 范围守护自检）
  ③ N1–N6 按推荐定稿
- **Blocked on**: 闸门 2 实现授权
- **Next step**: 获授权后按 tasks 实现，第一步是 T-01 单一模式判定点（design 决策 1 的硬约束）
- **Commit**: C3a 六个 commit 已落（`db2174f` / `8fdcdc1` / `131dfd3` / `7bf9190` / `4bb4515` / `b76f221`）；C3b 规划产物待提交；**未 push**

## 本刀最易漏的三处（实现期逐条核对）

1. **`buildToolContext` 会给出错误答案**（tasks T-09）：它只按「有无 recordId」判断是否下发 tools，而回看会话恰好绑定一条记录 → **「回看无工具」不会自动成立**，必须按 purpose 显式短路
2. **`CLOSING` 阶段会触发素材生成**（tasks T-11）：这是决策 2 不复用 `CLOSING` 常量的原因之一；回看路径须短路 `generateMaterial`
3. **`AgentStage` 新增 `REVIEW` 后要逐个检查既有 switch**（tasks T-02）：`stageGoal` / `buildTurnInstruction` / `AgentStageMachine`，不靠 default 混过去

## Residual / Carry-over（技术与环境）

- **[R8｜C3a] 时间归属阈值未校准**：见上「闸门 3 延后」，已随 baseline 生效
- **[R9｜C3a] 检索相关性弱**：标签 + 说明性字段 LIKE，无权重、无分词、无向量（蓝图已接受）
- **[C3a 实测] 本地 `tag` 表 0 行、`record_tag` 0 绑定**：标签关联路径在当前数据下零命中。**闸门 3 若要验标签关联，须先建标签并绑定记录**
- **[C3a 实测] `core_question` 本地 0% 非空**：该字段在检索与取材中恒不贡献（降级逻辑自动跳过，无需处置）
- **[C3a 实测] 覆盖率**：26 条记录，`ai_summary` 62% / `belief_then` 62% / `title` 85% / 任一说明性字段 85%；状态 DRAFT 2 + UNLOCKED 24 + SEALED 0
- **[待执行] 生产库 DDL**：`backend/sql/mysql/c3-agent-memory.sql` 尚未在本地 MySQL 执行（测试走 H2）。真机手验前需执行
- **[R7｜C4] 忠实度闸拦截方向未活体验证**
- **[R2] 引导问题突兀 + 素材拼接生硬**：用户要求延后到 C1–C5 全部完工后统一优化。**本刀同样不动**
- **[R3｜C2] 微信端到端工具链路真机手验未走通**：本刀有 UI 改动，**在本刀补齐**
- **[R6｜待用户执行] 凭证轮换**：`AI_API_KEY` / `S3_ACCESS_KEY` / `S3_SECRET_KEY` / `S3_SESSION_TOKEN` / `WECHAT_MINI_PROGRAM_SECRET`。轮换后建议删除 `backend/start-dev-wechat.local.ps1.bak`（含旧明文，已 gitignore）
- **闸门 3 探针**：`C4RealProviderProbeTest` 由 `C4_REAL_PROBE=1` 门控，默认跳过；合并联调可复用
- **历史数据**：`agent_message` 中 6 条 C2 修复前的 JSON 包裹消息（id 13/15/17/19/20/21）
- **本地环境**：MySQL80 StartType=Manual，当前 Running；DB 密码为本地默认值，由启动脚本参数注入，**不写入任何 tracked file**
- **secret 读取方式**：`backend/secrets.local.env`（gitignore）+ `Get-LocalSecret`，缺键快速失败
- **[流程教训｜2026-07-29] 不得使用波及未跟踪文件的 git 操作**：本轮曾用 `git stash push --include-untracked` 意外收走用户的 `iteration-blueprint-v1.2-draft.md`、`Docs/agent-iteration/architecture/`、`.kiro/skills/`（已按字节校验恢复）。此后一律只用显式 `git add <path>`，不使用 stash / clean / reset --hard 等全局操作
- **未跟踪的非本轮产物**（不要擅自提交或移动）：`Docs/agent-iteration/architecture/`、`Docs/agent-iteration/roadmap/iteration-blueprint-v1.2-draft.md`、`.kiro/skills/`，以及 `Docs/agent-iteration/README.md` 与 `roadmap/README.md` 的未提交改动
- **Kiro 诊断误报**：`design.md` / `tasks.md` 报「缺少 Kiro Spec 章节」。本项目按规则使用 OpenSpec 而非 `.kiro/specs/`，**不修**
- **M4 carry-over**：MySQL `EXPLAIN` timeline（Type B，不阻塞主线）

## Out Of Scope（本刀）

- 不做决策链路查询端点或可观测面板（C5）
- 不改引导 prompt 提问策略、不改素材合成策略（R2）
- 不动 C3a 的检索实现（复用 MemoryPort，不另起一套）
- 不扩工具白名单；回看**完全无工具**
- 不放宽任何 C4 / C3a 护栏阈值
- 不做视觉大改；不改三 Tab 与用户可见命名
- 不并行开第二个 active Type C
