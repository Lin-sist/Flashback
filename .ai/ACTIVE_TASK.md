# Active Task

## Status

`ACTIVE`

- Change：`agent-observability`（C5）
- 位置：`openspec/changes/agent-observability/`
- 阶段：**实现与验证完成，待用户验收**（闸门 1 已批准 + 闸门 2 已授权，2026-07-30）
- 开工锚点：`a834d85`
- **闸门 3 未授权、未执行**：T-35~T-37（真实 provider 轨迹完整性、耗时量级、fail-closed 活体触发）全部未做
- 产物：`proposal.md` / `design.md`（11 条决策）/ `tasks.md`（T-01~T-40 已完成，T-41 待验收）/ `closeout.md` / 四份 delta
- delta 落点：`agent-runtime`（4 条 MODIFIED + 8 条 ADDED）、`backend-core`（7）、`agent-collaboration`（3）、`v2-product-scope`（2）；`miniapp-core` **无 delta**（前端零改动）
- 验证：后端 **533 tests PASS / 2 skipped**（496 基线 + 37 新增，零回归）；**既有断言零修改**；本地 DDL 已执行且幂等已验证

### N1–N7 定稿（用户按推荐批准）

MySQL 表 / 每轮一条聚合 + 单一落库出口 / 全量不采样 / 只落库不加端点 / 补齐 V4·V5 / 版本由内容哈希派生 / 级联删除 + 保留期 + 手动清理

### 两处已获批的对已冻结蓝图的偏离

- **存储**：蓝图 v1.1 §4 C5 风险栏写「MVP 可用结构化 JSON 日志文件」，实际建 MySQL 表。
  依据：蓝图同卡片要求「可查询」而本地无日志聚合；C6 要求字段级关联
- **采样**：蓝图缓解措施提「可配置采样率」，实际默认全量无采样。
  依据：采样会制造排查盲区——最想看的那一轮可能恰好没被采到

### 规划期核实到的关键事实（决定了 N4 不做端点）

- **`AuthRole.ADMIN` 全仓没有任何签发路径**：`UserServiceImpl.buildLoginResponse` 固定签 `AuthRole.USER`，
  全仓无其他 `createToken` 调用点。因此 `/admin/**` 下的查询端点**在真实环境不可达**——
  这推翻了「做个 admin 端点」的直觉方案。改为只落库 + `c5-trace-queries.sql` + 集成测试实证

## Previous Completed

- Change：`agent-review-chat`（C3 后半刀）
- 位置：`openspec/changes/archive/2026-07-29-agent-review-chat/`
- 结果：友人回看对话已实现并归档，delta 已接受进 baseline（`agent-runtime` / `backend-core` / `miniapp-core` / `v2-product-scope` 各追加「Accepted From C3b」段落；`agent-runtime` 另修订两条 scenario）
- 验证：backend **496 tests PASS / 2 skipped**（472 基线未回归；2 skipped 为两个环境变量门控的真实 provider 探针）；前端 `type-check` + `build:mp-weixin` PASS；闸门 3 真实调用 **15 次 / 预算 20**；**微信真机手验 PASS**
- **C3 两刀至此全部完成**：`agent-memory-retrieval`（C3a，`archive/2026-07-29-agent-memory-retrieval/`）+ `agent-review-chat`（C3b）
- 更早：`agent-guardrails-hardening`（C4）、`agent-tool-calling`（C2）、`agent-runtime-mvp`（C1）、`m4-real-capability-completion`

## Direction Layer

- **迭代蓝图**：`Docs/agent-iteration/roadmap/iteration-blueprint.md` v1.1 已冻结（C4 前移与 C3 拆两刀均已登记于 §7）
- 主线进度：M4 → C1 → C2 → C4 → C3a → C3b 已归档 → **C5 `agent-observability` 规划闸中**
- C5 只硬依赖 C1，无其他硬依赖；依赖前提已满足
- 蓝图 v1.2 草案（未跟踪文件）建议 C5 归档后再做一次校准并冻结 v1.2；**当前权威仍是 v1.1**
- **C5 是 Phase 1 收官刀**：归档后进入 v1.2 校准会（草案 §0.2 与 §8 的清单）

## Source Of Truth

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

- **This session**: 2026-07-30 — C5 规划闸 + 实现全流程完成
  - 规划：30 条现状事实（V1–V30）、11 条决策、T-01~T-41、四份 delta；N1–N7 按推荐定稿
  - 实现：`agent/trace/` 五个类 + 实体/mapper/XML + DDL + 9 条排查查询 + 37 项测试
  - thought / action / observation 三段齐备；**`AgentStageDecision.Reason` 第一次被真正使用**；
    **成功路径的 provider 耗时不再被丢弃**
  - 既有缺陷补齐：**V4**（降级痕迹此前恒传 null sessionId）；**V5** 改用轨迹解决，未动 checker 签名
  - 三处与规划不符已如实记录（见 `closeout.md` §4）：V19 被证伪、不需要哈希前缀、记忆采集拆两步
- **Blocked on**: 待用户验收 diff
- **Next step**: 验收 → delta 接受进 baseline（**`agent-runtime` 四条 MODIFIED 要逐条落，它们分散在四个「Accepted From」段落**）→ 归档 → `ACTIVE_TASK` → IDLE → **Phase 1 收官，进入蓝图 v1.2 校准会**
- **Last session**: 2026-07-29 — C3 两刀全流程完成
  - C3a：分层来源 + 时间归属护栏 + `MemoryPort` + MySQL 检索 + 写作引导注入；T-01 覆盖率实测；归档
  - C3b：`AgentChatMode` 单一模式判定点 + 回看会话（无阶段机 / 无工具 / 无素材）+ `ReviewChatSheet`；归档
  - 闸门 3 合并执行：真实 provider 15 次 + 微信真机手验 PASS
- **Commit**: C3b 六个 commit 已落（`b6327df` / `fe7e644` / `ec76e8d` / `8a0d02b` / `0eb4d7a` / `df88795`）+ 归档提交；**未 push**，`main` 领先 `origin/main`。C5 规划产物 **未提交**

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

## Out Of Scope（C5 待验收期间）

- **闸门 3 仍未授权**：不得发起真实 provider 调用、不得真机联调
- 不要在验收前归档，也不要自行把 delta 接受进 baseline
- 不要并行开 C6（蓝图 v1.2 草案 §10 明确「C6 不要偷跑」）
- 不要在 C5 之外顺手改引导 / 素材 prompt（R2 已明确延后）
- 不改 Agent 对话行为、不改前端、不改认证签发、不引入 logback / 日志聚合 / actuator / 定时任务
- **不得使用波及未跟踪文件的 git 操作**（不用 stash / clean / reset --hard）
