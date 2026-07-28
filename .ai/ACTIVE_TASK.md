# Active Task

## Status

`IDLE`

当前无活动 Type C change。开始新的重大实现前，必须先创建 OpenSpec change 并更新本文件。

## Previous Completed

- Change：`agent-guardrails-hardening`（C4）
- 位置：`openspec/changes/archive/2026-07-28-agent-guardrails-hardening/`
- 结果：忠实度双指标闸 + 后置内容检查 + 违规降级 + 护栏规则单一声明源已实现，2026-07-28 验收归档；delta 已接受进 baseline（`agent-runtime` / `backend-core` / `v2-product-scope` 各追加「Accepted From C4」段落，并修订 C1 / C2 两条「护栏深度」scenario）
- 验证：backend **397 tests PASS / 1 skipped**（339 基线未回归且未改动任何既有断言；skipped 为环境变量门控的闸门 3 探针）；R1 真实样本回归 PASS；双指标必要性 PASS；不误伤 PASS；fail-closed PASS；真实 DeepSeek 联调 **4 次 / 预算 30**
- **闸门 3 诚实结论**：误伤方向已验证（4 个真实样本全部判忠实且离阈值有余量），**拦截方向未活体验证**（本轮未复现 R1 型增写）
- 实测推翻规划初值：`min-coverage` 0.60 → **0.35**（合法整理实测覆盖率仅 0.500）
- 实现期补记 design 决策 13：引号片段需专用严判据（11 字伪造引用曾从短文本跳过判定的缝隙漏放）
- 更早：`agent-tool-calling`（C2，`archive/2026-07-28-agent-tool-calling/`）、`agent-runtime-mvp`（C1）、`m4-real-capability-completion`

## Direction Layer

- **迭代蓝图**：`Docs/agent-iteration/roadmap/iteration-blueprint.md` v1.1 已冻结（方向未改，C4 前移已登记于 §3.2 + §7）
- 主线进度：M4 → C1 → C2 → **C4 已归档** → 下一刀 **C3 `agent-memory-and-review`** → C5
- C3 / C5 均只硬依赖 C1，无其他硬依赖

## Source Of Truth (when IDLE)

- `AGENTS.md`
- `openspec/project.md`
- `openspec/specs/agent-runtime/spec.md`（含已接受 C1 + C2 + C4 条款，Agent 核心契约）
- `openspec/specs/backend-core/spec.md`（含已接受 M4 + C1 + C2 + C4 条款）
- `openspec/specs/miniapp-core/spec.md`（含已接受 M4 + C1 + C2 条款）
- `openspec/specs/v2-product-scope/spec.md`（含已接受 M4 + C1 + C2 + C4 条款）
- `openspec/specs/agent-collaboration/spec.md`
- 方向：`Docs/agent-iteration/roadmap/iteration-blueprint.md`（已冻结 v1.1）
- 开工清单：`Docs/agent-iteration/workflow/prompt-snippets/type-c-checklist.md`

## Current Progress

- **Last session**: 2026-07-28 — C4 全流程完成：规划闸（Q1–Q7 定稿）→ 实现（新增 `agent/guardrail` 包 9 类 + 三条路径接入）→ 闸门 3 真实联调 → delta 接受 → 归档
- **Blocked on**: none
- **Next step**: 用户授权后启动 **C3 `agent-memory-and-review` 规划闸**；规划批准前禁止业务代码
- **Commit**: C4 规划 `a2c1075`、实现与归档见后续 commit；**未 push**，`main` 领先 `origin/main`

## Carry-over For C3（下一刀的输入）

- **C3 体量提示**：蓝图 §4 C3 含三个子能力（Memory 检索、友人回看对话、跨记录关联），已预留拆分退路（`agent-memory-retrieval` + `agent-review-chat`），拆分须更新蓝图 §7
- **待确认项**：P4（Memory 检索实现：MySQL FULLTEXT / LIKE / 外部引擎）、P5（友人回看对话 UI 形式）
- **C4 对 C3 的约束（重要）**：C4 的忠实度闸来源集合**只含当前会话的用户消息**。C3 引入历史记录检索后，须明确决定「历史记录中的用户原话是否算合法来源」——
  - 若算，则跨记录引用可通过忠实度闸，但要防止「把三个月前的话当成此刻说的」；
  - 若不算，则 Agent 在对话中引用历史记录时可能被忠实度闸误判。
  - **这是 C3 规划阶段必须回答的问题，不得默认沿用 C4 行为**
- **蓝图 D7 边界**：Memory 用简单检索，**不做**第二套企业 RAG / 向量中台

## Residual / Carry-over（技术与环境）

- **[R7｜C4 新增] 忠实度闸拦截能力未活体验证**：闸门 3 未复现 R1 型增写（模型在素材不足时选择「少说」而非补话）。阈值为本地样本 + 4 个真实样本标定，样本量小；增写是概率性行为。**建议后续任一阶段真机手验时顺带观察，不单独开 change**
- **已接受残余风险（C4）**：大量复用用户原话词汇的虚构可能同时通过双指标，不声称杜绝
- **[R2] 引导问题突兀 + 素材拼接生硬**：用户明确要求延后到 C1–C5 全部完工后统一优化。C4 未动引导 prompt 与素材合成策略
- **[R3] 微信端到端工具链路真机手验未走通**：C2 遗留（T-40~T-42），C4 期间用户决定不手验，继续延后
- **[R6｜待用户执行] 凭证轮换**：`AI_API_KEY` / `S3_ACCESS_KEY` / `S3_SECRET_KEY` / `S3_SESSION_TOKEN` / `WECHAT_MINI_PROGRAM_SECRET`。轮换后建议删除 `backend/start-dev-wechat.local.ps1.bak`（含旧明文，已 gitignore）
- **闸门 3 探针**：`C4RealProviderProbeTest` 由 `C4_REAL_PROBE=1` 门控，默认跳过；后续阶段可复用做真实观察
- **历史数据**：`agent_message` 中 6 条 C2 修复前的 JSON 包裹消息（id 13/15/17/19/20/21），本地既有数据
- **本地环境**：MySQL80 StartType=Manual，重启后需手动启动
- **secret 读取方式**：`backend/secrets.local.env`（gitignore）+ `Get-LocalSecret`，缺键快速失败
- **M4 carry-over**：MySQL `EXPLAIN` timeline（Type B，不阻塞主线）
- **Kiro 诊断误报**：`design.md` / `tasks.md` 报「缺少 Kiro Spec 章节」。本项目按规则使用 OpenSpec 而非 `.kiro/specs/`，**不修**

## Out Of Scope While Idle

- 不要在没有新 Type C 的情况下改 Agent runtime / 工具 / 护栏 / AI 业务代码
- 不要把 `项目初始分析.md` 的 P0 表直接当 ACTIVE scope
- 不要并行复活 M1/M3 作为隐式 active change
- 不要跳过三道闸门直接实现 C3 / C5
- 不要在 C3 之外顺手改引导 / 素材 prompt（R2 已明确延后）
