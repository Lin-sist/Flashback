# Active Task

## Status

`ACTIVE`

- Change：`agent-guardrails-hardening`（C4）
- 位置：`openspec/changes/agent-guardrails-hardening/`
- 阶段：**实现已完成（闸门 2 内）—— 待用户验收；闸门 3（真实外调）未授权**
- 开工锚点：`b64296d`；规划闸提交 `a2c1075`
- Q1–Q7 已于 2026-07-28 按推荐全部定稿；闸门 2 已授权并完成后端实现
- **闸门 3 未授权** → 全程零外调，真实 provider 复现 R1 与阈值校准仍待授权

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

- **Last session**: 2026-07-28 — C4 规划闸 + 实现全部完成
  - 规划：proposal / design（决策记录 10 条）/ tasks / spec delta 三份；蓝图 §3.2 + §7 登记 C4 前移；提交 `a2c1075`
  - 实现：新建 `agent/guardrail/` 包 8 个类（归一化、来源语料、覆盖画像、忠实度判定、内容检查、规则声明源、判定结果、降级处置）；接入工具提议 / 素材 / 回复三条路径；护栏规则从三处收敛到单一声明源
  - 验证：**396 tests / 0 failures**（339 基线不回归，未改动任何 C1/C2 既有断言）；全程零外调
  - **实测校准推翻一项规划初值**：`min-coverage` 0.60 → 0.35（合法整理实测覆盖率仅 0.500，0.60 会误伤正常能力）
  - **实现期补记 design 决策 13**：引号片段需专用严判据——实测发现 11 字伪造引用会从「短文本跳过覆盖率判定」的缝隙漏放
- **Blocked on**: 无（闸门 3 未授权属预期，非阻塞）
- **Next step**: 用户验收 diff；随后可选择 ① 授权闸门 3 做真实 provider 复现 R1 + 阈值校准 + 微信手验（含补 C2 遗留 T-40~T-42），或 ② 直接进入收口与归档
- **Commit**: 规划 `a2c1075`；实现待提交（提交责任＝Agent 代为提交）；`main` 领先 `origin/main`，**未 push**

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
