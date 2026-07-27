# Active Task

## Status

`IDLE`

当前无活动 Type C change。开始新的重大实现前，必须先创建 OpenSpec change 并更新本文件。

## Previous Completed

- Change：`m4-real-capability-completion`
- 位置：`openspec/changes/archive/2026-07-27-m4-real-capability-completion/`
- 结果：M4 核心能力准生产可用（真实 AI、对象存储附件、位置、封面、时光轴筛选分页、preview 隔离、时间回看真实数据等）已实现并通过用户手验；delta 已接受进 baseline；正式手动归档于 2026-07-27。
- 残留：真实 MySQL timeline `EXPLAIN` 为 **carry-over residual**（MySQL80 未启动/无提权），不阻塞 M4 归档；有环境后用 Type B 补证据即可。
- M1 / M3 目录仍可能未归档：与 M4 主线脱钩，另排治理，**不得**当作当前 active 实现源。

## Source Of Truth (when IDLE)

- `AGENTS.md`
- `openspec/project.md`
- `openspec/specs/backend-core/spec.md`（含已接受 M4 条款）
- `openspec/specs/miniapp-core/spec.md`（含已接受 M4 条款）
- `openspec/specs/v2-product-scope/spec.md`（含已接受 M4 条款）
- `openspec/specs/agent-collaboration/spec.md`
- 方向参考（**未冻结不得执行**）：`Docs/agent-iteration/roadmap/iteration-blueprint.md`
- 工作流参考：`Docs/agent-iteration/workflow/`

## Current Progress

> IDLE 时本段仅作下一会话提示，不指向 active change。

- **Last session**: 2026-07-27 — M4 真相对齐与正式归档
- **Completed**: M4 archive + baseline delta accept
- **Blocked on**: none for M4 product scope
- **Next step**: 用户审阅并让 Claude **修订/冻结** `iteration-blueprint.md`；冻结前 **禁止** 开 post-M4 Agent 主线实现。可选 Type B：MySQL EXPLAIN 残留证据。
- **SKIPPED / residual**: MySQL `EXPLAIN` timeline query（carry-over）

## Out Of Scope While Idle

- 不要在没有新 Type C 的情况下改 AI/Agent runtime 业务代码
- 不要把 `项目初始分析.md` 或未冻结蓝图当作 ACTIVE scope
- 不要并行复活 M1/M3 作为隐式 active change
