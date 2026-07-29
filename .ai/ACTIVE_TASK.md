# Active Task

## Status

`ACTIVE`（实现已完成 T-02~T-19；闸门 3 未申请）

## Current Change

- Change：`agent-memory-retrieval`（C3 前半刀）
- 位置：`openspec/changes/agent-memory-retrieval/`
- 开工锚点：`9e747fd`
- 阶段：**闸门 1、2 已通过**；阶段 1–5 全部完成，后端 **472 tests PASS / 1 skipped**；闸门 3（真实联调）**未申请**
- 提交责任：**用户手动提交**（Agent 不执行 `git add` / `commit` / `push`）
- 外调预算：实现阶段实际 **0 次**；闸门 3 单独授权后 ≤ 20 次真实 provider 请求

## C3 拆分声明（2026-07-29 用户批准）

蓝图 C3 `agent-memory-and-review` 依 §4「可选拆分退路」拆为两刀：

1. **`agent-memory-retrieval`（本刀）**：检索 + 写作引导注入 + MemoryPort + 跨记录关联能力 + 分层来源 + 时间归属护栏
2. **`agent-review-chat`（后一刀）**：回看会话（`purpose=REVIEW_CHAT`）+ `ReviewChatSheet` 浮层，消费同一 MemoryPort

蓝图 §7 修订记录待本刀批准后更新（tasks T-27）。R2 引导 / 素材策略**两刀均不动**。

## 规划闸已定稿决策（2026-07-29 用户）

| # | 决策 |
|---|---|
| Q1 | 拆两刀，先 Memory |
| Q2 | **B（精化）**：合法来源 = 当前会话 USER 全文 + 本轮**实际注入**的 memory 片段原文（非全库历史）；片段带 `recordId` + 时间锚点；引用须时间归属；引号严判仍适用 |
| Q3 | **①**：标签 + 时间窗 + `title` / `core_question` / `ai_summary` / `belief_then` LIKE；不加 `content` FULLTEXT；无外部引擎；**默认不扫 `content`**；权重待 T-01 覆盖率实测后定 |
| Q4 | 复用 `agent_session` 加 `purpose=WRITING_GUIDANCE｜REVIEW_CHAT`；回看无阶段机、自由多轮 + 轮次上限、stage 固定常量；不另建表 |
| Q5 | 后一刀：`record-detail` 独立 `ReviewChatSheet`，与回应浮层互斥；可抽共享消息壳，不复用带工具确认的 `AgentChatSheet`；不做视觉重建 |
| Q6 | 回看**完全无工具**；delta 明示；Runtime 不挂 tools；冒出 tool call 则 fail-closed |
| Q7 | 提交责任=用户手动提交；外调：memory 刀 20 次、review 刀另 15–20 次；闸门 3 单独授权 |

## N1–N5 已定稿（2026-07-29 用户按推荐批准）

| # | 决策项 | 定稿 |
|---|---|---|
| N1 | 分层来源实现形态 | **(b)** 新建 `AgentLayeredCorpus` 包装（纯增量，保基线） |
| N2 | 检索的记录状态范围 | **(b)** 排除 SEALED 未解锁 |
| N3 | 是否留「扫 content」开关 | **(a)** 完全不做 |
| N4 | `agent_session.purpose` 列在哪一刀建 | **(a)** 本刀建列，不实现 REVIEW_CHAT 行为 |
| N5 | 是否向前端下发「用了 memory」标识 | **(a)** 不下发（前端零改动，已核对） |

## Source Of Truth

- `AGENTS.md`
- `openspec/changes/agent-memory-retrieval/`（proposal / design / tasks / specs delta）
- `openspec/specs/agent-runtime/spec.md`（含 C1 + C2 + C4 已接受条款）
- `openspec/specs/backend-core/spec.md`、`miniapp-core/spec.md`、`v2-product-scope/spec.md`、`agent-collaboration/spec.md`
- `openspec/project.md`
- 方向：`Docs/agent-iteration/roadmap/iteration-blueprint.md`（已冻结 v1.1）
- 开工清单：`Docs/agent-iteration/workflow/prompt-snippets/type-c-checklist.md`

## Current Progress

- **Last session**: 2026-07-29 — 规划闸（proposal / design 13 条决策 / tasks / 三份 delta）→ 闸门 1+2 批准 → 实现阶段 1–5 全部完成
  - 新增：`AgentLayeredCorpus`、`AgentTimeAttributionChecker`、`agent/memory` 包（`MemoryPort` / `MemoryQuery` / `MemoryFragment` / `MySqlMemoryPort` / `MemoryCueExtractor`）、`AgentSessionPurpose`、`c3-agent-memory.sql`
  - 改造：`AgentCoverageProfile.longestExclusiveRun`、`AgentSourceCorpus.merge`、`AgentPromptBuilder.buildMemorySupplement` + 五参重载、`AgentToolValidator` 分层校验、`AgentChatServiceImpl` 接入检索、`AgentGuardrailRules` 加时间归属词表与记忆文案
  - 回归：**472 tests PASS / 1 skipped**（397 基线 + 75 新增），既有断言零修改
- **Blocked on**:
  - **T-01 覆盖率实测**：MySQL80 已启动，但 `root` 空密码被拒；DB 凭证由 `start-dev.ps1` 启动参数提供，不在 `secrets.local.env`。**需用户提供密码或自行执行统计**。已通过「四字段并列 LIKE + 固定取材优先级」设计使其不再阻塞实现（详见 tasks T-01）
  - **闸门 3 未申请**：T-20~T-23 待授权
- **已收口**：`closeout.md`（含偏离规划 5 处、残余风险、给后一刀 7 条 carry-over）、蓝图 §7 拆刀登记 + `ai_summary` 事实修正
- **实现期自我修正**：三个既有文件曾被编辑器自动格式化（4 空格 → 8 空格缩进），diff 虚增到 229/529/374 行；已从 HEAD 恢复并改用脚本直写重做，实际改动回落到 39/5/4 行
- **Next step**: ① 用户验收 diff；② 提供 DB 凭证补 T-01；③ 授权闸门 3 做真实联调与时间归属阈值校准；④ 真机前执行 `backend/sql/mysql/c3-agent-memory.sql`
- **Commit**: **pending**（提交责任=用户手动提交，Agent 未执行任何 git 写操作）

## 本刀的关键约束（实现期须逐条守）

- **不变量 1**：MEMORY 层只含**本轮实际注入**的片段；检索到但被条数上限丢弃的不算来源
- **不变量 2**：进入 `record.content` 的文本**恒只认 SESSION 层**，不可配置
- **不变量 3**：检索失败 fail-open（不注入、对话继续）；护栏判定失败 fail-closed
- **不变量 4**：不放宽 `minCoverage=0.35` / `maxUncoveredRun=12` / `minCheckedLength=12` / `QUOTE_MIN_COVERAGE=0.80`
- **spec 债**：须以 MODIFIED 修订 `agent-runtime` 中四条「跨记录检索留给后续 change」的 scenario（C1 / C2 / C4 记忆能力 + C4「来源集合的边界」），不得绕过
- **隐私**：memory 片段是其他记录的日记原文，不落库 / 不入日志 / 不进审计 / 不进痕迹

## Previous Completed

- `agent-guardrails-hardening`（C4，`archive/2026-07-28-agent-guardrails-hardening/`）：忠实度双指标闸 + 后置内容检查 + 违规降级 + 护栏规则单一声明源；backend **397 tests PASS / 1 skipped**；真实 DeepSeek 4 次 / 预算 30；实测把 `min-coverage` 从 0.60 下调到 0.35
- `agent-tool-calling`（C2，`archive/2026-07-28-agent-tool-calling/`）
- `agent-runtime-mvp`（C1，`archive/2026-07-27-agent-runtime-mvp/`）
- `m4-real-capability-completion`（`archive/2026-07-27-m4-real-capability-completion/`）

## Residual / Carry-over（技术与环境）

- **[R7｜C4] 忠实度闸拦截能力未活体验证**：闸门 3 未复现 R1 型增写。本刀闸门 3 可顺带观察，不单独开验收项
- **已接受残余风险（C4）**：大量复用用户原话词汇的虚构可能同时通过双指标
- **[R2] 引导问题突兀 + 素材拼接生硬**：用户要求延后到 C1–C5 全部完工后统一优化。**本刀与 `agent-review-chat` 均不动**
- **[R3] 微信端到端工具链路真机手验未走通**：C2 遗留（T-40~T-42）。本刀前端零改动，**不承接**；留到 `agent-review-chat`（有 UI 改动）一并手验
- **[R6｜待用户执行] 凭证轮换**：`AI_API_KEY` / `S3_ACCESS_KEY` / `S3_SECRET_KEY` / `S3_SESSION_TOKEN` / `WECHAT_MINI_PROGRAM_SECRET`。轮换后建议删除 `backend/start-dev-wechat.local.ps1.bak`（含旧明文，已 gitignore）
- **闸门 3 探针**：`C4RealProviderProbeTest` 由 `C4_REAL_PROBE=1` 门控，默认跳过；本刀可复用做真实观察
- **[R8｜C3 新增] 时间归属阈值未经真实样本校准**：`guardrail.min-memory-only-run-for-attribution=8` 是保守推断值（短于 8 字的记忆命中视为措辞巧合）。误伤与拦截两个方向均未活体验证，闸门 3 待办（T-21）
- **[R9｜C3 新增] 检索相关性弱**：标签 + 说明性字段 LIKE，无字段权重、无分词、无向量。蓝图 C3 风险栏已接受（「初期可接受；后续独立 change 升级」），closeout 须诚实记录真实观感，不粉饰
- **[新] `ai_summary` 覆盖率未知**：由前端回传写入而非后端自动生成，本地实际覆盖率**未测**（T-01 因 DB 凭证受阻）。若覆盖率低，片段会大量降级到 `belief_then` / `core_question` / `title`，信息密度下降
- **[新] DB 凭证获取方式**：`root` 空密码被拒；DB 密码由 `start-dev.ps1` / `start-dev-wechat.local.ps1` 的启动参数传入，**不在** `secrets.local.env`（该文件只有 `AI_API_KEY` / `WECHAT_MINI_PROGRAM_SECRET`）。需要直连 DB 的任务须用户提供
- **历史数据**：`agent_message` 中 6 条 C2 修复前的 JSON 包裹消息（id 13/15/17/19/20/21）
- **本地环境**：MySQL80 StartType=Manual，**当前 Running**（用户 2026-07-29 手动启动）
- **secret 读取方式**：`backend/secrets.local.env`（gitignore）+ `Get-LocalSecret`，缺键快速失败
- **[待执行] 生产库 DDL**：`backend/sql/mysql/c3-agent-memory.sql` 尚未在本地 MySQL 执行（测试走 H2 `schema.sql`）。真机联调前需执行
- **M4 carry-over**：MySQL `EXPLAIN` timeline（Type B，不阻塞主线）
- **Kiro 诊断误报**：`design.md` / `tasks.md` 报「缺少 Kiro Spec 章节」。本项目按规则使用 OpenSpec 而非 `.kiro/specs/`，**不修**

## Out Of Scope（本刀）

- 不做回看会话 / 回看 UI（后一刀 `agent-review-chat`）
- 不做决策链路查询端点或可观测面板（C5）
- 不改引导 prompt 提问策略、不改素材合成策略（R2）
- 不加 FULLTEXT / ngram、不扫 `content`、不引外部检索引擎、不引 embedding
- 不扩工具白名单、不放宽任何 C4 阈值
- 不并行开第二个 active Type C
