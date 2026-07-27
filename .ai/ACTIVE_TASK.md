# Active Task

## Status

`ACTIVE`

## Task

- Change：`agent-tool-calling`（C2）
- 位置：`openspec/changes/agent-tool-calling/`
- 阶段：**规划闸（闸门 1）—— Q1–Q5 已定稿，待用户批准放行**，**零业务代码**
- 协议定稿：原生 OpenAI-compatible Function Calling + DeepSeek strict mode，**无降级路径**
- 目标：Agent 可在对话中调用受白名单约束的后端工具，对话过程自然产生行动（提议 → 用户确认 → 受控执行 → 结果回注）
- 开工锚点：`63d1767`（工作区干净）
- 上游方向：`Docs/agent-iteration/roadmap/iteration-blueprint.md` v1.1 §4 C2（已冻结）

## Gate Status

| 闸门 | 状态 |
|---|---|
| 1 规划批准 | **已放行**（2026-07-27），Q1–Q5 已定稿 |
| 2 实现授权 | **已取得**（2026-07-27）；后端 + 前端实现完成，本地验证全绿 |
| 3 外调授权 | **未取得** —— 真实 provider FC 联调与微信手验尚未进行；**手验前须先确认本地 `AI_PROVIDER` 取值**（C1 偏差不得重演） |

> 提交责任：**已变更为 Agent 代为提交**（用户授权），验收仍由用户进行。`push` 未授权。

## 规划闸决策（已定稿）

| # | 决策项 | 定稿结论 |
|---|---|---|
| Q1 | 提议协议 | **原生 OpenAI-compatible Function Calling + DeepSeek strict mode，唯一路径、无降级**。FC 不可用即显式 `UNAVAILABLE` |
| Q2 | Tool 白名单 | 写工具 `append_record_content` / `add_record_tags` / `propose_unlock_at` 下发为 FC tools；读工具 `list_available_tags` / `read_draft_snapshot` 改为 **prompt 预注入**（不注册为 FC tool）；seal/delete/unlock/location/cover/attachment/later-reflection/标签创建全部代码级排除 |
| Q3 | 持久化落点 | 新表 `agent_tool_call`（不动 `agent_message` 唯一键） |
| Q4 | 执行入口 | 新增 `POST /api/agent/sessions/{sid}/tool-calls/{tid}/confirm` |
| Q5 | spec delta 落点 | agent-runtime + backend-core + miniapp-core + v2-product-scope |

### Q1 定稿的关键约束

- **不做单轮内 FC 循环**：`tool_calls` 只转成待确认提议，本轮结束；执行在用户确认的独立请求里（design 决策 9）
- **单轮至多一个提议**：多个 `tool_calls` 只取第一个合法项，其余记审计（design 决策 10）
- **strict mode 不支持** `maxLength` / `maxItems` / `minItems` → 长度与数量边界必须留在 `AgentToolValidator` 代码层（design §3.2）
- **既有链路不碰**：`complete()` 与三个单轮 AI 端点继续走 `response_format=json_object`
- **不引入 MCP / Spring AI / LangChain4j**（design 决策 11，方向记入 proposal §12）

> Q2 附带确认项：标签「追加」需在 `RecordService` 新增 `appendTags`，因既有 `PUT /api/records/{id}` 是全量重绑且要求 content 非空（design 决策 5）。

## Previous Completed

- Change：`agent-runtime-mvp`（C1）
- 位置：`openspec/changes/archive/2026-07-27-agent-runtime-mvp/`
- 结果：Backend Agent Runtime 基底 + 「写下此刻」多轮引导 + 最小护栏内嵌，2026-07-27 用户真机验收通过并归档；delta 已接受进 baseline（新建 `agent-runtime` capability，另在 `backend-core` / `miniapp-core` / `v2-product-scope` 追加接受段落）
- 验证：backend 254 tests PASS；frontend type-check + `build:mp-weixin` PASS；真实 MySQL DDL 已执行校验；真实 DeepSeek 4 轮联调成功；微信手验通过
- 提交：`602b31b`（用户授权 Agent 代为提交；未 push）
- 流程偏差：闸门 3 未事前取得——手验时本地 `AI_PROVIDER=deepseek` 导致手验即真实外调。已披露并记入 AGENT_LOG；**C2 须避免重演**
- 更早：`m4-real-capability-completion`（`archive/2026-07-27-m4-real-capability-completion/`）

## Direction Layer

- **迭代蓝图已冻结**：`Docs/agent-iteration/roadmap/iteration-blueprint.md` **v1.1**
- 主线进度：C1 已归档 → **C2 `agent-tool-calling` 规划中** → C3 → C4 → C5（规则见蓝图 §3.2）
- 蓝图 §3.2 允许在气质越界时将 C4 前移；C1 手验未观察到越界，按默认顺序执行 C2

## Source Of Truth

- `AGENTS.md`
- `openspec/changes/agent-tool-calling/`（**active change**：proposal / design / tasks / specs delta）
- `openspec/specs/agent-runtime/spec.md`（C1 已接受的 Agent 核心契约，C2 的直接基础）
- `openspec/specs/backend-core/spec.md`（含已接受 M4 + C1 条款）
- `openspec/specs/miniapp-core/spec.md`（含已接受 M4 + C1 条款）
- `openspec/specs/v2-product-scope/spec.md`（含已接受 M4 + C1 条款）
- `openspec/specs/agent-collaboration/spec.md`
- `openspec/project.md`
- 方向：`Docs/agent-iteration/roadmap/iteration-blueprint.md`（**已冻结 v1.1**）
- 工作流：`Docs/agent-iteration/workflow/`
- 开工清单：`Docs/agent-iteration/workflow/prompt-snippets/type-c-checklist.md`

## Current Progress

- **Last session**: 2026-07-27 — C2 规划闸 + 闸门 2 实现全部完成。规划阶段 Q1 经官方文档核查后由「自研 JSON 协议」翻转为「原生 FC + strict mode」；实现阶段完成 Tool 层、持久化、执行层、会话集成、确认端点与前端确认交互
- **Completed**: T-00 ~ T-36 全部勾选（规划 T-00~T-02b；实现 T-03~T-36）
- **Blocked on**: **闸门 3 外调授权** —— 真实 provider FC 联调（T-37~T-43）无法在授权前进行
- **Next step**: 用户给出闸门 3 授权 → 启动本地 MySQL 执行 `c2-agent-tool-call.sql` → 真实 DeepSeek FC 联调（≤ 45 次）→ 微信手验 → 验收归档
- **Verification**: backend `mvn -B test` **329 PASS**（C1 基线 254 + C2 新增 75，零失败）；frontend `type-check` PASS、`build:mp-weixin` PASS
- **Commit**: 由 Agent 代为提交（见 AGENT_LOG 补录 hash）；**未 push**
- **实现期新增决策**：design 决策 12（待确认提议的执行参数存放方式 → 瞬态 `pending_args` 列，终结即清），已同步 spec delta
- **已知无需修复**：`design.md` / `tasks.md` 在 Kiro 诊断中报「缺少 Kiro Spec 章节」。本项目按规则使用 OpenSpec 而非 `.kiro/specs/`，已归档并验收的 C1 同款文档报同样的诊断，**属误报，不修**

## Residual / Carry-over

- **C1 遗留**：素材回填在 `record_id IS NULL` 且正文为空时会因内容校验失败报错 → 已纳入 C2 设计（后端 `appendContent` 路径，`design.md` 决策 5）
- **C1 已接受风险**：最小护栏仅 system prompt + 长度裁剪单层，C4 系统化补齐；C2 沿用同一立场
- **C1 已接受风险**：真实 provider 仅验 4 轮，长会话稳定性待 C2 观察
- **M4 carry-over**：MySQL `EXPLAIN` timeline（Type B，不阻塞主线）
- **本地环境**：MySQL80 StartType=Manual，重启后需手动启动；`backend/start-dev-wechat.local.ps1` 明文存放 secret（已 gitignore，但建议轮换并改为环境变量读取）
- **本地数据**：`agent_message` 已存真实对话数据，重置数据库时注意
- **未 push**：`main` 领先 `origin/main`（C1 提交），待用户自行决定推送

## Out Of Scope While This Change Is Active

- 未获闸门 1 批准前不写任何业务代码
- 不做 Memory / 历史记录检索 / 跨记录关联（C3）
- 不做后置内容过滤 / 违规降级 / 边界用例集（C4）
- 不做决策链路查询端点或可观测界面（C5）
- 不让 Agent 触达 seal / delete / unlock / location / cover / attachment / later-reflection / 标签创建
- 不改 C1 四个既有 Agent 端点的字段语义，不改 `/api/ai/**` 与 `/api/stage-summaries/**` 契约
- 不引入 FC → 自研 JSON 提议协议的降级路径
- 不在生成回复的同一处理过程内执行工具或回灌 tool 结果
- 不引入 MCP / Spring AI / LangChain4j
- 不改三 Tab、不改用户可见命名、不做视觉大改、不改 package / lockfile
