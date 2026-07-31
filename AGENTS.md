# AGENTS.md

## Project

- 项目：时光回序 Flashback V2.0。
- 技术栈：Uniapp + Vue 3 + Pinia + WeChat Mini Program；backend 为 Spring Boot / MyBatis / MySQL 方向。
- 当前阶段：**Phase 2 已开局**（Phase 1：M4 → C1 → C2 → C4 → C3a → C3b → C5 全部归档；Phase 2 首刀 **C6 `agent-eval-framework` 已于 2026-07-31 归档**）。方向蓝图 **v1.2 已冻结**（2026-07-30）；Phase 2 序列为 ~~C6 Eval~~ → C7 Reflection → C8 Resilience → C9 Temporal，**默认下一刀 `agent-reflection-loop` 规划闸**。
- 产品初心：帮助用户写下当下，并让未来的自己重新理解这一刻；不是效率仪表盘、社交流或管理后台。

## Source of Truth

- OpenSpec 是事实源；旧 `Docs/**` 只在不冲突时参考。
- 每轮任务必须先读 `.ai/ACTIVE_TASK.md`。`IDLE` 时不得假装存在 active change。
- **工作流方法论（给人与 Agent 的完整说明）**：`Docs/agent-iteration/workflow/`。  
  其中 Type 分级细节、六步闭环、决策记录模板、操作性 checklist 以该目录为准；**本文件只保留强制摘要**。与本文件冲突时，以本文件的 Non-Negotiable 与 Source of Truth 为准。
- **已接受 baseline specs（含 M4）**：
  - `openspec/project.md`
  - `openspec/specs/backend-core/spec.md`
  - `openspec/specs/miniapp-core/spec.md`
  - `openspec/specs/v2-product-scope/spec.md`
  - `openspec/specs/agent-collaboration/spec.md`
- **M4 归档位置（历史，非 active）**：`openspec/changes/archive/2026-07-27-m4-real-capability-completion/`
- **方向蓝图（已冻结 v1.2）**：`Docs/agent-iteration/roadmap/iteration-blueprint.md`  
  - 批准方向、依赖、气质与意图卡片；**仍不授权直接改业务代码**  
  - 实现必须新建 `openspec/changes/<change-id>/` 并走三道闸门

## Non-Negotiable Rules

- 保留三个一级 Tab： 首页、时光轴、个人中心。
- V2.0 用户可见命名：我的记录、时光轴、时间回看。
- Preview 可以保留，但必须与 authenticated real user path 隔离；真实路径不能用 mock success 冒充真实成功。
- 对象存储 AK/SK、AI API key 等 secret 只能存在于 backend-side config / local secret，不得进入 frontend 或 tracked files。
- 封存后禁止修改 location、attachments、cover。
- 不做 speech-to-text、voice transcription、voice AI analysis、complex AI scoring / diagnosis / dashboard（除非未来独立 OpenSpec change 明确批准且不违背产品初心）。
- 不改 deployment、monitoring、admin portal、SMS、production notification center、campaign delivery、settings page（除非独立 change 明确纳入）。
- 不做大规模 backend rewrite。
- 不做 major frontend visual reconstruction。
- 不改 package / lockfile，除非任务明确要求并说明原因。
- 不确定的 backend API 契约、字段命名、持久化方式、枚举语义、object key policy、signed URL 过期策略、provider 配置、前端可见状态，必须先向用户确认，不要主观猜测。
- 用户日记原文是高敏数据：不得写入普通日志、tracked files 或未授权外发。

## Task Classification (Type A / B / C)

开始写代码或改 OpenSpec 前，必须判定任务类型。**拿不准时按 Type C 处理，或先 Type A 向用户确认。**

| 类型 | 何时 | OpenSpec change | ACTIVE_TASK | AGENT_LOG |
|---|---|---|---|---|
| **A 只读** | 扫描、解释、对照、规划讨论 | 不创建 | 不改 | 通常不写 |
| **B 小修** | 文案、注释、已有契约内低风险 bugfix、纯方法论文档、M4 EXPLAIN 残留证据 | 不创建 | 不改（除非用户要求） | **必须追加** |
| **C 重大** | 用户可见能力；API/DTO/持久化/状态机/权限；AI 行为语义；存储/附件/位置/封存契约；跨模块主题；**产品 Agent runtime/tools/memory/guardrails** | **必须** proposal/design/tasks/delta | **指向该 change** | **必须追加** |

Type C 同时最多 **一个** active change。`ACTIVE_TASK=IDLE` 时禁止业务实现「假装在做某个未声明 change」。

## Gates (强制)

三道闸门分离，**不得合并默认放行**：

1. **规划批准**：Type C 的 proposal/design（含决策记录）/tasks/delta 经用户确认前，禁止改业务代码。
2. **实现授权**：规划批准 ≠ 可写代码；须用户明确允许按 tasks 实现。
3. **外调 / 副作用授权**：真实批量 AI 调用、真实对象存储联调、`push`、部署、发布须单独授权。实现授权不包含这些。

提交责任事前二选一：`用户手动提交`（默认）或 `Agent 提交`。未明确时 Agent **不得** `git add` / `commit` / `push`。

## Session Handoff (跨会话)

每个 Type C 会话开始时：读 `AGENTS.md` → `.ai/ACTIVE_TASK.md`（含 **Current Progress**）→ active change 的 `tasks.md` → 最近相关 `AGENT_LOG` 条目。

每个 Type C 会话结束或暂停时：更新 `ACTIVE_TASK.md` 的 **Current Progress**（完成了哪些 task、阻塞、下一步、待补 SKIPPED 验证），并追加 `AGENT_LOG`。

能力五态用于现状扫描与 proposal：`confirmed` / `partial` / `planned` / `out_of_scope` / `unknown`。**禁止把 unknown 写成 confirmed。**

## Context Budget Rules

- 不要全仓库扫描。
- 先读 `.ai/ACTIVE_TASK.md`。
- 只读本轮任务列出的文件。
- 需要额外文件时先说明原因。
- 每轮只解决当前 task，不顺手重构。

## Required Evidence

- 所有实现记录、验证结果、跳过验证原因、手动微信验证结果都必须写入 `.ai/AGENT_LOG.md`。
- AGENT_LOG **只追加、不改写历史**。推荐结构化条目（见 `.ai/AGENT_LOG.md` 顶部模板）：Scope / Changes / Verification(PASS|FAIL|SKIPPED+原因) / Risks / Commit。
- 执行时写 `Commit: pending`；提交后可另条补录真实 hash，不回改旧文。
- 不得写入 API key、token、密码、用户日记原文。
- Type C 的 **决策取舍**写在 `design.md` 的决策记录中，不塞进 AGENT_LOG。决策记录格式见 `Docs/agent-iteration/workflow/prompt-snippets/design-decision-record.md`。

## Required Output

每个 Agent 完成后必须输出：

- modified files
- what changed
- verification result
- skipped verification reason, if any
- `git diff --stat`
- scope safety check
- remaining risks

## OpenSpec Boundary

- 只有范围、canonical mapping、验收标准发生变化时才修改 OpenSpec。
- 普通 bugfix / 样式精修不需要修改 OpenSpec，只需要写入 `.ai/AGENT_LOG.md`。
- Type C 操作性清单：`Docs/agent-iteration/workflow/prompt-snippets/type-c-checklist.md`。
- Type B 操作性清单：`Docs/agent-iteration/workflow/prompt-snippets/type-b-checklist.md`。
- 长期方向蓝图：`Docs/agent-iteration/roadmap/iteration-blueprint.md`（**v1.2 已冻结**；批准方向与气质约束，仍不授权直接改业务代码）。
- 对外叙事（面试向，非执行契约）：`Docs/agent-iteration/narrative/agent-tech-story.md`。**禁止写入用户日记原文、secret、本机绝对路径。**
- `Docs/agent-iteration/项目初始分析.md` 是产品方向评估草稿，**不是**已批准 roadmap 或 OpenSpec。
