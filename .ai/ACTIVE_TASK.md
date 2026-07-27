# Active Task

## Status

`IDLE`

当前无活动 Type C change。开始新的重大实现前，必须先创建 OpenSpec change 并更新本文件。

## Previous Completed

- Change：`agent-runtime-mvp`（C1）
- 位置：`openspec/changes/archive/2026-07-27-agent-runtime-mvp/`
- 结果：Backend Agent Runtime 基底 + 「写下此刻」多轮引导 + 最小护栏内嵌已实现，2026-07-27 用户真机验收通过并归档；delta 已接受进 baseline（新建 `agent-runtime` capability，另在 `backend-core` / `miniapp-core` / `v2-product-scope` 追加接受段落）。
- 验证：backend 254 tests PASS；frontend type-check + `build:mp-weixin` PASS；真实 MySQL DDL 已执行校验；真实 DeepSeek 4 轮联调成功；微信手验通过（修复浮层布局缺陷后）。
- 流程偏差：**闸门 3 未事前取得**——手验时本地 `AI_PROVIDER=deepseek` 导致手验即真实外调（用量远低于 30 次预算）。已披露并记入 AGENT_LOG。
- 更早：`m4-real-capability-completion`（`archive/2026-07-27-m4-real-capability-completion/`）

## Direction Layer

- **迭代蓝图已冻结**：`Docs/agent-iteration/roadmap/iteration-blueprint.md` **v1.1**
- 主线进度：**C1 已完成归档** → 下一刀 C2 `agent-tool-calling` → C3 → C4 → C5（规则见蓝图 §3.2）
- 蓝图不授权直接写代码；C2 须新建 `openspec/changes/agent-tool-calling/` 并走三道闸门
- 蓝图 §3.2 允许在 C1 出现气质越界时将 C4 前移至 C2 之前；本次 C1 手验未观察到越界，暂按默认顺序

## Source Of Truth (when IDLE)

- `AGENTS.md`
- `openspec/project.md`
- `openspec/specs/backend-core/spec.md`（含已接受 M4 + C1 条款）
- `openspec/specs/miniapp-core/spec.md`（含已接受 M4 + C1 条款）
- `openspec/specs/v2-product-scope/spec.md`（含已接受 M4 + C1 条款）
- `openspec/specs/agent-runtime/spec.md`（**C1 新建**，Agent 核心契约）
- `openspec/specs/agent-collaboration/spec.md`
- 方向：`Docs/agent-iteration/roadmap/iteration-blueprint.md`（**已冻结 v1.1**）
- 工作流：`Docs/agent-iteration/workflow/`
- 开工清单：`Docs/agent-iteration/workflow/prompt-snippets/type-c-checklist.md`

## Current Progress

- **Last session**: 2026-07-27 — C1 `agent-runtime-mvp` 全流程完成：规划闸 → 实现 → 本地验证 → 真机手验 → 缺陷修复 → 验收 → baseline 接受 → 归档
- **Completed**: C1 tasks T-01 ~ T-40 全部勾选
- **Blocked on**: none
- **Next step**: 用户授权后启动 C2 `agent-tool-calling` **规划闸**（proposal/design/决策记录/tasks/delta）；规划批准前禁止业务代码
- **Commit**: C1 全部改动仍为 **未提交**（提交责任＝用户手动提交）

## Residual / Carry-over

- **C1 遗留**：素材回填在 `record_id IS NULL` 且正文为空时会因内容校验失败报错（手验未触发）→ Type B 修或纳入 C2
- **C1 已接受风险**：最小护栏仅 system prompt + 长度裁剪单层，C4 系统化补齐
- **M4 carry-over**：MySQL `EXPLAIN` timeline（Type B，不阻塞主线）
- **本地环境**：MySQL80 StartType=Manual，重启后需手动启动；`backend/start-dev-wechat.local.ps1` 明文存放 secret（已 gitignore，但建议轮换并改为环境变量读取）
- **本地数据**：`agent_message` 已存真实对话数据，重置数据库时注意

## Out Of Scope While Idle

- 不要在没有新 Type C 的情况下改 Agent runtime / AI 业务代码
- 不要把 `项目初始分析.md` 的 P0 表直接当 ACTIVE scope
- 不要并行复活 M1/M3 作为隐式 active change
- 不要跳过三道闸门直接实现 C2–C5
