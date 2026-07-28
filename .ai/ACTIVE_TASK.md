# Active Task

## Status

`IDLE`

当前无活动 Type C change。开始新的重大实现前，必须先创建 OpenSpec change 并更新本文件。

## Previous Completed

- Change：`agent-tool-calling`（C2）
- 位置：`openspec/changes/archive/2026-07-28-agent-tool-calling/`
- 结果：原生 function calling 工具调用 + 代码级白名单 + 二段式用户确认已实现，2026-07-28 验收归档；delta 已接受进 baseline（`agent-runtime` / `backend-core` / `miniapp-core` / `v2-product-scope` 各追加「Accepted From C2」段落）
- 验证：backend **339 tests PASS**（C1 基线 254 未回归且未改动任何 C1 断言）；frontend type-check + `build:mp-weixin` PASS；真实 MySQL DDL PASS；真实 DeepSeek **原生 FC 与 strict mode 均 PASS**（本仓库首次实测）；外调 6 次 / 预算 45
- 提交：`6c363f6`（实现）、`98c764a`（JSON 缺陷修复 + 闸门 3）、`8587893`（secret 外移）
- 流程改善：本次闸门 3 **事前确认本地 `AI_PROVIDER` 后才联调**，未重演 C1 的顺序偏差
- 未完成项：微信端到端工具链路真机手验（T-40~T-42），已在 tasks 与 closeout 中显式标注未勾选
- 更早：`agent-runtime-mvp`（C1，`archive/2026-07-27-agent-runtime-mvp/`）、`m4-real-capability-completion`

## Direction Layer

- **迭代蓝图**：`Docs/agent-iteration/roadmap/iteration-blueprint.md` **v1.1 已冻结**
- 主线进度：M4 → C1 → **C2 已归档** → 下一刀 **C4（已决定前移）** → C3 → C5
- **顺序调整（2026-07-28 用户批准）**：按蓝图 §3.2「若出现 Agent 气质越界则允许将 C4 前移」，C2 真实联调已取得越界实证（工具参数改写并增写用户原话），故 **C4 前移至 C3 之前**。蓝图 §7 修订记录待 C4 proposal 阶段一并更新
- C2/C3/C4/C5 彼此无硬依赖，均只硬依赖 C1，故此调整不破坏依赖链

## Source Of Truth (when IDLE)

- `AGENTS.md`
- `openspec/project.md`
- `openspec/specs/agent-runtime/spec.md`（含已接受 C1 + C2 条款，Agent 核心契约）
- `openspec/specs/backend-core/spec.md`（含已接受 M4 + C1 + C2 条款）
- `openspec/specs/miniapp-core/spec.md`（含已接受 M4 + C1 + C2 条款）
- `openspec/specs/v2-product-scope/spec.md`（含已接受 M4 + C1 + C2 条款）
- `openspec/specs/agent-collaboration/spec.md`
- 方向：`Docs/agent-iteration/roadmap/iteration-blueprint.md`（已冻结 v1.1）
- 工作流：`Docs/agent-iteration/workflow/`
- 开工清单：`Docs/agent-iteration/workflow/prompt-snippets/type-c-checklist.md`

## Current Progress

- **Last session**: 2026-07-28 — C2 全流程完成：规划闸（Q1 协议翻转）→ 实现 → JSON 缺陷修复 → 闸门 3 真实 FC/strict 验证 → secret 外移 → delta 接受 → 归档
- **Blocked on**: none
- **Next step**: 用户授权后启动 **C4 `agent-guardrails-hardening` 规划闸**（proposal/design/决策记录/tasks/delta）；规划批准前禁止业务代码
- **Commit**: C2 全部改动已提交（三个 commit）；**未 push**，`main` 领先 `origin/main`

## Carry-over For C4（前移后的首要输入）

- **[R1｜C4 核心动机] 工具参数改写并增写用户原话**：C2 闸门 3 真实联调实测——用户原话为「我学的是软件工程，一直想做后端」+「刚才说的这些我觉得挺重要的，想留下来」，模型返回的 `text` 增写了「但最近心里有点空，不知道该不该继续沿着这条路走下去，方向是不是对的，自己也说不清楚」（**用户从未说过**），`askText` 自称「我帮你把这两句整理了一下」
  - 触碰 `AGENTS.md` Non-Negotiable「不改写用户原文」与 `agent-runtime` spec「引用用户表达时 SHALL 原样引用」
  - **C2 的白名单与二段式确认拦不住它**：二者校验「能否执行」，不校验「参数内容是否忠实」。这正是 C2 design 决策 7 划定的边界
  - C4 需要回答的核心问题：**如何机械地判定「忠实」**。候选方向（未批准）：与会话历史做子串/覆盖率比对、对增写部分做显式拒绝、或将参数改为「用户消息 id 引用」而非自由文本
- **[R4] 内容合规仍为单层**：C1/C2 期间仅 system prompt + 长度裁剪，无后置检查、无违规降级、无边界用例集
- **[R5] `propose_unlock_at` 边界**：是否越过「建议不代决」待体感确认，可随时从 registry 移除

## Carry-over For 后续统一优化（用户明确要求延后）

- **[R2] 引导问题突兀 + 素材拼接生硬**：2026-07-28 手验实例——天气比喻开场，用户答不上并反问「你为什么要这样问」，最终素材把用户的**反问与困惑本身**也拼进正文。两层成因：① 引导策略在受阻时未退回具体情境（与 `stageGoal`「不要问抽象的大问题」的意图相悖）；② 素材合成不区分「用户的表达」与「用户对提问的抵触」
  - 用户判断：接近大模型微调范畴，**C1–C5 全部完工后统一收集逐个优化**

## Residual / Carry-over（技术与环境）

- **[R3] 微信端到端工具链路真机手验未走通**：C2 手验期间运行实例未重启，注意力集中在 JSON 缺陷上。下次真机验证时补 T-40~T-42
- **历史数据**：`agent_message` 中 6 条修复前产生的 JSON 包裹消息（id 13/15/17/19/20/21），属本地既有数据，不会自动清理
- **[R6｜待用户执行] 凭证轮换**：`AI_API_KEY` / `S3_ACCESS_KEY` / `S3_SECRET_KEY` / `S3_SESSION_TOKEN` / `WECHAT_MINI_PROGRAM_SECRET`。原因＝C2 期间 Agent 的 grep 范围过宽，曾将前三项打印到终端输出。轮换后建议删除 `backend/start-dev-wechat.local.ps1.bak`（含旧明文，已 gitignore）
- **本地环境**：MySQL80 StartType=Manual，重启后需手动启动
- **secret 读取方式**：已改为 `backend/secrets.local.env`（gitignore）+ `Get-LocalSecret`，缺键快速失败；模板见 `secrets.local.env.example`
- **M4 carry-over**：MySQL `EXPLAIN` timeline（Type B，不阻塞主线）
- **Kiro 诊断误报**：`design.md` / `tasks.md` 报「缺少 Kiro Spec 章节」。本项目按规则使用 OpenSpec 而非 `.kiro/specs/`，C1/C2 同款文档报同样诊断，**不修**

## Out Of Scope While Idle

- 不要在没有新 Type C 的情况下改 Agent runtime / 工具 / AI 业务代码
- 不要把 `项目初始分析.md` 的 P0 表直接当 ACTIVE scope
- 不要并行复活 M1/M3 作为隐式 active change
- 不要跳过三道闸门直接实现 C3–C5
- 不要在 C4 之外顺手改素材 / 引导 prompt（R2 已明确延后）
