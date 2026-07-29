# Active Task

## Status

`IDLE`

当前无活动 Type C change。开始新的重大实现前，必须先创建 OpenSpec change 并更新本文件。

## Previous Completed

- Change：`agent-review-chat`（C3 后半刀）
- 位置：`openspec/changes/archive/2026-07-29-agent-review-chat/`
- 结果：友人回看对话已实现并归档，delta 已接受进 baseline（`agent-runtime` / `backend-core` / `miniapp-core` / `v2-product-scope` 各追加「Accepted From C3b」段落；`agent-runtime` 另修订两条 scenario）
- 验证：backend **496 tests PASS / 2 skipped**（472 基线未回归；2 skipped 为两个环境变量门控的真实 provider 探针）；前端 `type-check` + `build:mp-weixin` PASS；闸门 3 真实调用 **15 次 / 预算 20**；**微信真机手验 PASS**
- **C3 两刀至此全部完成**：`agent-memory-retrieval`（C3a，`archive/2026-07-29-agent-memory-retrieval/`）+ `agent-review-chat`（C3b）
- 更早：`agent-guardrails-hardening`（C4）、`agent-tool-calling`（C2）、`agent-runtime-mvp`（C1）、`m4-real-capability-completion`

## Direction Layer

- **迭代蓝图**：`Docs/agent-iteration/roadmap/iteration-blueprint.md` v1.1 已冻结（C4 前移与 C3 拆两刀均已登记于 §7）
- 主线进度：M4 → C1 → C2 → C4 → C3a → **C3b 已归档** → 下一刀 **C5 `agent-observability`**
- C5 只硬依赖 C1，无其他硬依赖
- 蓝图 v1.2 草案（未跟踪文件）建议 C5 归档后再做一次校准并冻结 v1.2；**当前权威仍是 v1.1**

## Source Of Truth (when IDLE)

- `AGENTS.md`
- `openspec/project.md`
- `openspec/specs/agent-runtime/spec.md`（含已接受 C1 + C2 + C4 + C3a + C3b 条款，Agent 核心契约）
- `openspec/specs/backend-core/spec.md`（含 M4 + C1 + C2 + C4 + C3a + C3b）
- `openspec/specs/miniapp-core/spec.md`（含 M4 + C1 + C2 + **C3b**）
- `openspec/specs/v2-product-scope/spec.md`（含 M4 + C1 + C2 + C4 + C3a + C3b）
- `openspec/specs/agent-collaboration/spec.md`
- 方向：`Docs/agent-iteration/roadmap/iteration-blueprint.md`（已冻结 v1.1）
- 开工清单：`Docs/agent-iteration/workflow/prompt-snippets/type-c-checklist.md`

## Current Progress

- **Last session**: 2026-07-29 — C3 两刀全流程完成
  - C3a：分层来源 + 时间归属护栏 + `MemoryPort` + MySQL 检索 + 写作引导注入；T-01 覆盖率实测；归档
  - C3b：`AgentChatMode` 单一模式判定点 + 回看会话（无阶段机 / 无工具 / 无素材）+ `ReviewChatSheet`；归档
  - 闸门 3 合并执行：真实 provider 15 次 + 微信真机手验 PASS
- **Blocked on**: none
- **Next step**: 用户授权后启动 **C5 `agent-observability` 规划闸**；规划批准前禁止业务代码
- **Commit**: C3b 六个 commit 已落（`b6327df` / `fe7e644` / `ec76e8d` / `8a0d02b` / `0eb4d7a` / `df88795`）+ 归档提交；**未 push**，`main` 领先 `origin/main`

## C3 闸门 3 的关键结论（对 C5 有参考价值）

- **时间归属护栏两个方向都验过了**：误伤 0 次（9 轮观察，memory-only 片段 0~22 字，多次超阈值 8 说明判定被真实触发）；
  拦截方向用「取真实模型回复、只删时间指示语、其余逐字不动」的变换验证，判定由放行翻转为 `missing-time-attribution`。
  **R8 关闭**，且顺带补上 C4 遗留的 R7（C4 只验到误伤方向）
- **核实方法值得沿用**：不能只看 `attribution=null` 的汇总就判定护栏有效——放行可能是「模型真的说清了时间」，
  也可能是「词表偶然命中」，两者含义相反。实测额外打印命中词，确认命中的是「那时/过去/以前/你说过/四月/去年」等真实表述
- **真机证据**：Agent 自发说「去年六月你想坚持锻炼与学习，现在你在跑步、去健身房、学编程」——带时间归属，形态正确

## Residual / Carry-over

- **[R2] 引导话术与素材合成质量**：用户 2026-07-29 真机后再次确认「体验比之前好不少，Agent 有点『说人话』了，但还需要进步，当前够用」。
  **仍延后到 C1–C5 全部完工后统一优化**，C5 同样不动。用户已表示「后面具体再聊」
- **[新] 回看 fail-closed 分支未活体触发**：真实联调中模型未在无工具模式下返回 tool_calls，该分支正确性仅由单测覆盖。
  概率性行为，不单独开 change；**C5 若能记录这类事件，正好可补上**
- **[R9] 检索相关性弱**：标签 + 说明性字段 LIKE，无权重 / 分词 / 向量（蓝图 C3 风险栏已接受）。升级留独立 change
- **[C3a 实测] 本地 `tag` 表 0 行、`record_tag` 0 绑定**：标签关联路径在当前数据下零命中（非代码缺陷）
- **[C3a 实测] `core_question` 本地 0% 非空**：检索与取材中恒不贡献，固定优先级降级自动跳过
- **[C3a 实测] 字段覆盖率**：26 条记录，`ai_summary` 62% / `belief_then` 62% / `title` 85% / 任一说明性字段 85%
- **[R6｜待用户执行] 凭证轮换**：`AI_API_KEY` / `S3_ACCESS_KEY` / `S3_SECRET_KEY` / `S3_SESSION_TOKEN` / `WECHAT_MINI_PROGRAM_SECRET`。
  轮换后建议删除 `backend/start-dev-wechat.local.ps1.bak`（含旧明文，已 gitignore）
- **探针资产**：`C4RealProviderProbeTest`（`C4_REAL_PROBE=1`）、`C3RealProviderProbeTest`（`C3_REAL_PROBE=1`），默认跳过，C5 可复用其「只打印结构化指标、不打印原文」的形态
- **历史数据**：`agent_message` 中 6 条 C2 修复前的 JSON 包裹消息（id 13/15/17/19/20/21）
- **本地环境**：MySQL80 StartType=Manual；C1/C2/C3 的增量 DDL 均已执行完毕
- **secret 读取方式**：`backend/secrets.local.env`（gitignore）+ `Get-LocalSecret`，缺键快速失败；DB 密码为本地默认值，由启动脚本参数注入，**不写入任何 tracked file**
- **M4 carry-over**：MySQL `EXPLAIN` timeline（Type B，不阻塞主线）
- **Kiro 诊断误报**：`design.md` / `tasks.md` 报「缺少 Kiro Spec 章节」。本项目按规则使用 OpenSpec 而非 `.kiro/specs/`，**不修**

## 流程教训（后续 change 须遵守）

- **含 DDL 的 change 必须把「本地执行 DDL」列为实现期第一步**，而不是联调前置。
  C3b 曾因此让用户手验报「系统异常: api/agent/sessions」——且因为 mapper 列清单缺列，
  **写作引导对话也一起 500**，波及既有功能，而报错表现只是通用 500
- **不得使用波及未跟踪文件的 git 操作**：曾用 `git stash push --include-untracked` 意外收走用户的
  `iteration-blueprint-v1.2-draft.md`、`Docs/agent-iteration/architecture/`、`.kiro/skills/`（已按字节校验恢复）。
  一律只用显式 `git add <path>`，不使用 stash / clean / reset --hard
- **警惕编辑器自动格式化造成的 diff 污染**：若某文件 `git diff --stat` 比预期改动量大一个数量级，
  先怀疑格式化或行尾变化（用 `--ignore-all-space` / `--ignore-cr-at-eol` 对比），不要当成真实改动接受
- **验证拦截方向必须先确认样本确实处于该被拦的状态**：C3b 曾取「最后一轮」回复做剥离实验，
  而它恰好没在复述（memory-only=0），不翻转是样本选错而非护栏失效

## 未跟踪的非本轮产物（不要擅自提交或移动）

- `Docs/agent-iteration/architecture/`
- `Docs/agent-iteration/roadmap/iteration-blueprint-v1.2-draft.md`
- `.kiro/skills/`
- `Docs/agent-iteration/README.md` 与 `roadmap/README.md` 的未提交改动

## Out Of Scope While Idle

- 不要在没有新 Type C 的情况下改 Agent runtime / 工具 / 护栏 / 记忆 / 回看 / AI 业务代码
- 不要跳过三道闸门直接实现 C5
- 不要并行复活已归档 change 作为隐式 active change
- 不要在 C5 之外顺手改引导 / 素材 prompt（R2 已明确延后）
