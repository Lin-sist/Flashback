# Active Task

## Status

`ACTIVE`

- Change：`agent-guardrails-hardening`（C4）
- 位置：`openspec/changes/agent-guardrails-hardening/`
- 阶段：**规划闸（闸门 1）—— 已产出 proposal / design / tasks / spec delta，待用户批准 Q1–Q7 并放行**
- 开工锚点：`b64296d`（工作树干净；仅未跟踪 `.kiro/skills/`，非本 change 产物）
- **本阶段零业务代码**。闸门 2（实现）与闸门 3（真实外调）需另行授权

## Why C4 Now（顺序前移）

按蓝图 §3.2「若出现 Agent 气质越界则允许将 C4 前移」，**C4 已前移至 C3 之前**（2026-07-28 用户批准）。
触发实证＝C2 闸门 3 真实联调中工具参数改写并增写用户原话（详见下方 R1）。
蓝图 §7 修订记录与 §3.2 已同步登记本次调整。

主线进度：M4 → C1 → C2 → **C4（进行中，规划闸）** → C3 → C5

## Previous Completed

- Change：`agent-tool-calling`（C2）
- 位置：`openspec/changes/archive/2026-07-28-agent-tool-calling/`
- 结果：原生 function calling 工具调用 + 代码级白名单 + 二段式用户确认已实现，2026-07-28 验收归档；delta 已接受进 baseline
- 验证：backend **339 tests PASS**（C1 基线 254 未回归）；frontend type-check + `build:mp-weixin` PASS；真实 MySQL DDL PASS；真实 DeepSeek 原生 FC 与 strict mode 均 PASS；外调 6 次 / 预算 45
- 提交：`6c363f6`、`98c764a`、`8587893`、`b64296d`（收口归档）
- 未完成项：微信端到端工具链路真机手验（T-40~T-42）→ 已移交 C4 的 T-48 补齐
- 更早：`agent-runtime-mvp`（C1）、`m4-real-capability-completion`

## Direction Layer

- **迭代蓝图**：`Docs/agent-iteration/roadmap/iteration-blueprint.md` v1.1 已冻结（方向未改，仅次序调整）
- C2/C3/C4/C5 彼此无硬依赖，均只硬依赖 C1，故本次前移不破坏依赖链

## Source Of Truth

- `AGENTS.md`
- **active change**：`openspec/changes/agent-guardrails-hardening/`（proposal / design / tasks / specs）
- `openspec/specs/agent-runtime/spec.md`（含已接受 C1 + C2 条款；C4 将 MODIFIED 其中两条「护栏深度留给后续 change」的 scenario）
- `openspec/specs/backend-core/spec.md`、`miniapp-core/spec.md`、`v2-product-scope/spec.md`
- 方向：`Docs/agent-iteration/roadmap/iteration-blueprint.md`
- 开工清单：`Docs/agent-iteration/workflow/prompt-snippets/type-c-checklist.md`

## Current Progress

- **Last session**: 2026-07-28 — C4 规划闸产出完成：`proposal.md`（含 R1 归因、能力五态 G1–G27、Q1–Q7、验收 25 条）、`design.md`（架构 + 忠实度双指标机制 + 决策记录 10 条）、`tasks.md`（T-00~T-55 + 范围守护）、spec delta 三份（`agent-runtime` 含 2 条 MODIFIED、`backend-core`、`v2-product-scope`）；蓝图 §3.2 / §7 已登记 C4 前移
- **Blocked on**: **闸门 1 待批准**——用户需对 Q1–Q7 定稿（核心是 Q1 忠实度判定机制），并放行进入闸门 2
- **Next step**: 用户确认 Q1–Q7 → 勾选 T-02 / T-02d → 取得实现授权（T-03）后按 tasks A 段起步（忠实度判定核心，纯逻辑零外调）
- **Commit**: C4 规划文档待提交（提交责任＝Agent 代为提交）；`main` 仍领先 `origin/main`，**未 push**

## C4 待用户确认（Q1–Q7 摘要，详见 proposal §8）

| # | 决策项 | 推荐 |
|---|---|---|
| Q1 | 忠实度判定机制 | 机械覆盖率双指标闸（覆盖率 + 最长连续未覆盖片段）；否决 LLM-as-judge；消息 id 引用记为后续可选加强 |
| Q2 | 覆盖范围 | 工具 `text` + 素材草稿 + `askText`（后者宽判定） |
| Q3 | 违规处置 | 分路径：提议拒绝 / 素材丢弃 / 回复兜底 |
| Q4 | 阈值可配置性 | `app.agent.guardrail.*` + 保守初值 + 闸门 3 校准 |
| Q5 | 诊断 / 代决 / 相称性 | 规则单一声明源 + 只查「新增区段」；相称性不纳入 |
| Q6 | 痕迹落点与前端可见性 | 复用 `agent_tool_call.failure_type` + 结构化日志；前端不可见 |
| Q7 | spec delta 落点 | `agent-runtime` + `backend-core` + `v2-product-scope`（Q6 若选可见则补 `miniapp-core`） |
| 附带 | R5 `propose_unlock_at` 去留 | 保留（写的是可逆草稿字段，不触发封存） |
| 附带 | R3 是否在 C4 闸门 3 补 C2 真机手验 | 是（tasks T-48） |

## Carry-over 处理状态

- **[R1｜C4 核心动机] 工具参数改写并增写用户原话**：C2 闸门 3 实测——用户原话为「我学的是软件工程，一直想做后端」+「刚才说的这些我觉得挺重要的，想留下来」，模型返回的 `text` 增写「但最近心里有点空，不知道该不该继续沿着这条路走下去，方向是不是对的，自己也说不清楚」（**用户从未说过**），`askText` 自称「我帮你把这两句整理了一下」
  - **状态**：已作为 C4 的核心问题纳入规划（proposal §1 / §3、design §3、tasks T-08 为其回归用例）。**待 C4 验收后关闭**
- **[R4] 内容合规仍为单层**：C1/C2 期间仅 system prompt + 长度裁剪 → **已纳入 C4 范围**（后置检查 + 违规降级 + 边界用例集），待 C4 验收后关闭
- **[R5] `propose_unlock_at` 边界**：**待 Q 确认关闭**（C4 推荐保留，design 决策 10）
- **[R3] 微信端到端工具链路真机手验未走通**：**已排入 C4 tasks T-48**（C4 闸门 3 一并补齐）
- **[R2] 引导问题突兀 + 素材拼接生硬**：用户明确延后到全部阶段完工后统一优化。**C4 明确不做**（Non-Goals + design 决策 7）；design §3.5 只说明「顺语序算忠实、增写不算」的边界关系，不实施任何 prompt 调整

## Residual / Carry-over（技术与环境）

- **历史数据**：`agent_message` 中 6 条修复前产生的 JSON 包裹消息（id 13/15/17/19/20/21），本地既有数据，不会自动清理
- **[R6｜待用户执行] 凭证轮换**：`AI_API_KEY` / `S3_ACCESS_KEY` / `S3_SECRET_KEY` / `S3_SESSION_TOKEN` / `WECHAT_MINI_PROGRAM_SECRET`。原因＝C2 期间 Agent 的 grep 范围过宽，曾将前三项打印到终端输出。**建议在 C4 闸门 3 前完成**；轮换后建议删除 `backend/start-dev-wechat.local.ps1.bak`（含旧明文，已 gitignore）
- **本地环境**：MySQL80 StartType=Manual，重启后需手动启动
- **secret 读取方式**：`backend/secrets.local.env`（gitignore）+ `Get-LocalSecret`，缺键快速失败；模板见 `secrets.local.env.example`
- **M4 carry-over**：MySQL `EXPLAIN` timeline（Type B，不阻塞主线）
- **Kiro 诊断误报**：`design.md` / `tasks.md` 报「缺少 Kiro Spec 章节」。本项目按规则使用 OpenSpec 而非 `.kiro/specs/`，C1/C2 同款文档报同样诊断，**不修**

## Out Of Scope While C4 Active

- **闸门 1 批准前不写任何业务代码**
- 不做 Memory / 历史检索 / 跨记录关联（C3）
- 不做决策链路查询端点 / 可观测面板（C5）
- 不调引导 prompt 话术、不改素材合成策略（R2 已延后）
- 不扩大工具白名单（只可能收紧）
- 不引入第三方分词 / 相似度依赖，不改 package / lockfile
- 不并行复活 M1/M3 作为隐式 active change
