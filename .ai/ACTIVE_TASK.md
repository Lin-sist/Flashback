# Active Task

## Status

`IDLE`

当前无活动 Type C change。开始新的重大实现前，必须先创建 OpenSpec change 并更新本文件。

## Previous Completed

- Change：`m4-real-capability-completion`
- 位置：`openspec/changes/archive/2026-07-27-m4-real-capability-completion/`
- 结果：M4 核心能力准生产可用已实现并归档；delta 已接受进 baseline。
- 残留：timeline MySQL `EXPLAIN` carry-over（Type B 可补，不阻塞主线）。

## Direction Layer

- **迭代蓝图已冻结**：`Docs/agent-iteration/roadmap/iteration-blueprint.md` **v1.1**
- 默认主线顺序：C1 `agent-runtime-mvp` → C2 → C3 → C4 → C5（规则见蓝图 §3.2）
- 蓝图不授权直接写代码；下一刀应先做 C1 的 **proposal/design/tasks/delta 规划闸**

## Source Of Truth (when IDLE)

- `AGENTS.md`
- `openspec/project.md`
- `openspec/specs/backend-core/spec.md`（含已接受 M4 条款）
- `openspec/specs/miniapp-core/spec.md`（含已接受 M4 条款）
- `openspec/specs/v2-product-scope/spec.md`（含已接受 M4 条款）
- `openspec/specs/agent-collaboration/spec.md`
- 方向：`Docs/agent-iteration/roadmap/iteration-blueprint.md`（**已冻结 v1.1**）
- 工作流：`Docs/agent-iteration/workflow/`
- 开工清单：`Docs/agent-iteration/workflow/prompt-snippets/type-c-checklist.md`

## Current Progress

- **Last session**: 2026-07-27 — 工作流终验 + 蓝图 v1.1 冻结；Lincheck/Linsist 加入 `.gitignore`
- **Completed**: M4 archive；blueprint freeze
- **Blocked on**: none
- **Next step**: 用户授权后启动 C1 `agent-runtime-mvp` **规划**（proposal/design/决策记录/tasks/delta）；规划批准前禁止业务代码
- **SKIPPED / residual**: MySQL `EXPLAIN` timeline（carry-over）

## Out Of Scope While Idle

- 不要在没有新 Type C 的情况下改 AI/Agent runtime 业务代码
- 不要把 `项目初始分析.md` 的 P0 表直接当 ACTIVE scope
- 不要并行复活 M1/M3 作为隐式 active change
- 不要跳过三道闸门直接实现 C1–C5
