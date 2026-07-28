# Agent Tool Calling（C2）· Tasks

> Change ID：`agent-tool-calling`
> 阶段：**规划闸（闸门 1）—— Q1–Q5 已定稿，待用户批准放行**
> 协议定稿：原生 OpenAI-compatible Function Calling + DeepSeek strict mode，**无降级路径**
> 规则：闸门 1 未批准前，**T-03 之后的一切业务代码任务禁止开工**；闸门 2 未取得前不写业务代码；闸门 3 未取得前不做真实外调。
> 完成即把 `- [ ]` 改为 `- [x]`，并按 `AGENTS.md` 要求追加 `.ai/AGENT_LOG.md`。

---

## Gate 0 · 规划闸（本阶段）

- [x] T-00 创建 `openspec/changes/agent-tool-calling/`，产出 proposal / design / tasks / spec delta
- [x] T-01 Q1–Q5 定稿（2026-07-27 用户确认）
  - [x] Q1 提议协议 → **原生 FC + strict mode，无降级**（design 决策 1、9、10、11）
  - [x] Q2 白名单范围 → 3 写工具下发为 FC tools；2 读工具改为 prompt 预注入（design §3.1）
  - [x] Q3 持久化落点 → 新表 `agent_tool_call`
  - [x] Q4 执行入口 → 新增 confirm 端点
  - [x] Q5 spec delta 落点 → agent-runtime + backend-core + miniapp-core + v2-product-scope
- [x] T-02 更新 `.ai/ACTIVE_TASK.md`：Status=ACTIVE、指针指向本 change、初始化 Current Progress
- [x] **T-02b 闸门 1：用户批准规划放行**（2026-07-27）

> **本阶段结束标志**：零业务代码改动。

---

## Gate 1 · 实现授权后（闸门 2）

- [x] T-03 取得用户明确 **实现授权**（闸门 2）（2026-07-27；同时授权 Agent 代为提交，验收由用户最后进行）
  - 注意：闸门 3（真实 provider 外调）**未授权** → 实现期全部走 mock provider，零外调

### A. 后端 Tool 层骨架（纯逻辑，零外调）

- [x] T-04 新建 `agent/tool/AgentToolName` 枚举与 `AgentToolSpec`：工具名、参数规格、是否写操作、是否下发为 FC tool、面向模型的描述
- [x] T-05 新建 `agent/tool/AgentToolRegistry`：白名单唯一事实源；提供 `find(name)`、`functionCallingTools()`（仅写工具）、`preInjectedReadTools()`
- [x] T-05b 新建 `agent/tool/AgentToolSchemaFactory`：由 registry 生成 strict mode 合规的 `tools` 数组（全属性 `required`、`additionalProperties: false`、工具名 `enum`、`unlockAt` 用 `pattern`；**不使用** `maxLength` / `maxItems` / `minItems`）
- [x] T-06 新建 `agent/tool/AgentToolProposal`（由 `tool_calls` 解析：工具名 + 参数 + `askText`）与 `AgentToolOutcome`（状态 + 失败类型 + 结果摘要）
- [x] T-07 新建 `agent/tool/AgentToolValidator`：白名单校验、strict 无法覆盖的边界校验（`text` 长度、`tagIds` 数量与启用集、`unlockAt` 晚于当前时间）、排除项拒绝（seal/delete/unlock/location/cover/attachment/later-reflection/标签创建）
- [x] T-08 单测：白名单命中/未命中、参数缺失/越界、排除项被拒、`tool_calls` 缺失或结构异常时按无提议处理且本轮 `content` 正常返回
- [x] T-08b 单测：生成的 schema 满足 strict 全部约束；多个 `tool_calls` 时只取第一个合法提议、其余记审计（design 决策 10）

### B. 持久化

- [x] T-09 新增 `backend/sql/mysql/c2-agent-tool-call.sql`：`agent_tool_call` 表（`CREATE TABLE IF NOT EXISTS`，含 user_id/session_id/record_id/tool_name/args_digest/status/failure_type/时间列 + 归属索引 + FK CASCADE + 隐私注释）
- [x] T-10 同步 `backend/src/test/resources/schema.sql` 的 H2 建表
- [x] T-11 新增 entity `domain/AgentToolCall` + `AgentToolCallMapper` + XML（insert / selectByIdAndUserId / selectRecentBySessionId / updateStatus）
- [x] T-12 单测/集成测试：落库、归属查询、状态流转 `PROPOSED → EXECUTED/FAILED/REJECTED`

### C. 执行层（复用既有业务层，不绕 `ensureDraft`）

- [x] T-13 `RecordService` 新增 `appendContent(userId, id, text)`：`requireOwnedRecord` + `ensureDraft` + 追加不覆写（`\n\n` 分隔）
- [x] T-14 `RecordService` 新增 `appendTags(userId, id, tagIds)`：`requireOwnedRecord` + `ensureDraft` + 启用标签校验 + 合并去重 + 总数 ≤ 20
- [x] T-15 `RecordService` 新增 `updateUnlockAt(userId, id, unlockAt)`（若 Q2 保留 `propose_unlock_at`）：`ensureDraft` + 必须晚于当前时间 + **不触发封存**
- [x] T-16 新建 `agent/tool/AgentToolExecutor`：按工具名分发到上述方法，产出 `AgentToolOutcome`
- [x] T-17 单测：SEALED / UNLOCKED 记录上执行被拒；跨用户执行被拒；正文追加后原文逐字不变；标签追加后原标签仍在且未创建新标签
- [x] T-18 单测：执行失败返回显式失败语义，记录零变更，不谎报成功

### D. 会话集成与确认端点

- [x] T-18b `AgentModelClient` 新增 `completeWithTools(messages, tools)`：body `{model, messages, tools, stream:false}`（**不带** `response_format`），解析 `message.content` 与 `message.tool_calls`；既有 `complete()` 保持不动
- [x] T-18c `AppAgentProperties` 新增 `tool-calling-enabled` / `strict-mode-enabled` / `strict-mode-base-url` / `function-calling-models`；`AgentModelClient.unavailableReason()` 扩展工具维度判定（model 不在 FC 白名单 → 不下发 tools 并记结构化日志）
- [x] T-18d 单测：FC 不可用时返回显式 `UNAVAILABLE`；确认仓库内**不存在**第二条提议解析路径（无降级）
- [x] T-19 `AgentChatServiceImpl.sendMessage`：走 `completeWithTools` → 解析 `tool_calls` → 校验 → 落 `PROPOSED` → 下发；提议无效时不影响本轮回复；`content` 为空时以 `askText` 作为回复兜底
- [x] T-20 `AgentChatService` 新增 `confirmToolCall(userId, sessionId, toolCallId, decision)`：会话归属 + toolCall 归属 + 幂等（非 PROPOSED 直接返回当前状态）
- [x] T-21 `AgentController` 新增 `POST /api/agent/sessions/{sessionId}/tool-calls/{toolCallId}/confirm`；请求 DTO `AgentToolCallConfirmRequest{ decision }`
- [x] T-22 `AgentSessionVO` 向后兼容新增 `pendingToolCall` / `lastToolCallResult`；C1 既有字段语义不变
- [x] T-23 确认动作**不推进** `AgentStage`、**不增加** `turnCount`（design 决策 8）
- [x] T-24 `AgentPromptBuilder`：预注入读工具内容（启用标签清单 + 草稿快照）+ 最近工具执行结果摘要；沿用 `AgentGuardrailPolicy.guardrailClause()`；**不再**在 prompt 里手写工具清单（改由 tools schema 承担）
- [x] T-25 `AgentMockResponder`：mock provider 侧伪造 `tool_calls` 响应形状，供零外调端到端测试
- [x] T-26 MockMvc 测试：未登录 401、跨用户访问、非法 toolCallId、重复确认幂等
- [x] T-27 集成测试（mock provider + H2）：提议 → 确认 → 执行 → 结果回注 → 下一轮 Agent 感知；以及提议 → 拒绝 → 记录零变更
- [x] T-28 回归：后端既有测试全绿（C1 基线 254 项不回归）

### E. 前端

- [x] T-29 `services/agentService.ts` 新增 `confirmToolCall(sessionId, toolCallId, decision)`，沿用 preview 隔离（`requireRealSession`）
- [x] T-30 `stores/agentChat.ts` 新增 `pendingToolCall` 状态与 `confirmToolCall` action，含 loading 与错误态
- [x] T-31 `AgentChatSheet.vue` 新增克制的确认条（单行提议话术 + 「好」/「先不用」），emit 新事件；不改浮层整体视觉结构
- [x] T-32 `record-editor/index.vue`：确认成功后刷新草稿表单（正文/标签），避免前端表单与后端状态漂移
- [x] T-33 失败态：执行失败显示明确原因（含「记录已封存」）；重复点击防抖
- [x] T-34 `type-check` + `build:mp-weixin` 通过

### F. Spec delta 校对

- [x] T-35 核对实现与 `specs/agent-runtime/spec.md` delta 一致；有偏差先请示再改 spec
- [x] T-36 核对 `backend-core` / `miniapp-core` / `v2-product-scope` delta 与实际契约一致

---

## Gate 2 · 外调授权后（闸门 3）

- [x] T-37 取得用户明确 **外调授权**（闸门 3），确认预算上限 ≤ 45 次；**并先确认本地 `AI_PROVIDER` 取值**（C1 流程偏差不得重演）
- [x] T-38 启动本地 MySQL80（StartType=Manual），执行 `c2-agent-tool-call.sql` 并校验表结构
- [x] T-39 真实 provider FC 联调：strict schema 被服务端接受、`tool_calls` 与 `content` 并存确认、`content` 为空时 `askText` 兜底、连续多轮中的提议时机
- [x] T-39b strict mode 失败时按 design §4 处置顺序：修 schema → 关 `strict-mode-enabled` → 升级请示；**任何情况下不退回自研 JSON 提议协议**
- [ ] T-40 微信小程序手验：提议 → 确认执行 → 拒绝 → 重复确认幂等 → 记录已封存时执行失败
  - **归档时仍未完成（刻意不勾选）**：手验期间运行实例未重启，注意力集中在 JSON 显示缺陷上，真机工具链路未走通。后端侧已由 mock 集成测试 + 真实 FC 探针分别覆盖。见 closeout §3 SKIPPED 与 §6 R3
- [ ] T-41 气质手验：Agent 未自动封存、未改写原文、提议话术克制
  - **归档时未完成，且已发现反例**：真实返回的工具参数改写并增写用户原话（closeout §6 R1）→ 移交 C4
- [ ] T-42 验证审计表与应用日志均无日记原文 / 对话原文
  - **归档时未在真实库核验**（真机链路未走通，`agent_tool_call` 真实库无数据）；H2 集成测试已断言摘要脱敏与 `pending_args` 终结后为 NULL
- [x] T-43 全部验证结果（PASS/FAIL/SKIPPED+原因）追加 `.ai/AGENT_LOG.md`

---

## Gate 3 · 收口

- [x] T-44 输出 `AGENTS.md` Required Output 全字段（modified files / what changed / verification / skipped reason / `git diff --stat` / scope safety / remaining risks）
- [x] T-45 用户审 diff 与验收
- [x] T-46 delta 接受进 `openspec/specs/`
- [x] T-47 change 归档到 `openspec/changes/archive/<date>-agent-tool-calling/` + 写 `closeout.md`
- [x] T-48 `.ai/ACTIVE_TASK.md` → IDLE，Current Progress 归档
- [x] T-49 提交责任**已于 2026-07-27 变更为 Agent 代为提交**（用户授权，验收仍由用户进行）；`push` 未授权，不执行

---

## 范围守护（每个 task 完成时自检）

- 未让 Agent 获得 seal / delete / unlock / location / cover / attachment / later-reflection 任何路径
- 未做后置内容过滤或违规降级（C4）
- 未做 Memory / 历史检索（C3）
- 未做决策链路查询端点（C5）
- 未改 C1 四个既有端点的字段语义
- 未改既有 `complete()` 与三个单轮 AI 端点的 `json_object` 链路
- 未引入 FC → 自研 JSON 提议协议的降级路径
- 未在生成回复的同一处理过程内执行工具或回灌 tool 结果（design 决策 9）
- 未引入 MCP / Spring AI / LangChain4j（design 决策 11）
- 未改三 Tab、未改用户可见命名、未做视觉大改
- 未改 package / lockfile
- 未把日记原文写进审计表或日志
