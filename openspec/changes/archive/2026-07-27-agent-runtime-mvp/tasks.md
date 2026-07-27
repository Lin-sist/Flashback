# Agent Runtime MVP · Tasks（C1）

> 当前阶段：**规划闸（闸门 1）待批准**。
> T0 之前的任务只允许写 OpenSpec / `.ai` 文档，**禁止改业务代码**。
> 完成一项立即 `- [ ]` → `- [x]`，并按 `AGENTS.md` 追加 `.ai/AGENT_LOG.md`。

---

## P0 · 规划（闸门 1 前）

- [x] T-01 现状扫描：AI 单轮链路、provider 适配层、AI 端点与前端调用、Redis 使用情况、schema 风格
- [x] T-02 记录开工锚点（HEAD `b6140b3`，工作区干净）
- [x] T-03 创建 `openspec/changes/agent-runtime-mvp/`
- [x] T-04 `proposal.md`：Why Now / Goals / Non-goals / 用户故事 / 能力五态 / 外调预算 / 提交责任
- [x] T-05 `design.md`：架构 / 数据流 / 状态机 / API 草案 / 持久化 / 隐私 / 验证策略 / **决策记录 9 条**
- [x] T-06 `tasks.md`（本文件）
- [x] T-07 spec delta：`agent-runtime`（新）+ `backend-core` + `miniapp-core` + `v2-product-scope`
- [x] T-08 更新 `.ai/ACTIVE_TASK.md`：Status=ACTIVE、指针、Current Progress 初始化
- [x] T-09 追加 `.ai/AGENT_LOG.md` 规划条目
- [x] **T-10 闸门 1：用户已批准规划**（2026-07-27），Q1–Q4 全部按推荐方案定稿
  - [x] Q1 持久化方案 → **MySQL 新表** `agent_session` / `agent_message`
  - [x] Q2 对话消息 → **落原文**，按高敏业务数据对待
  - [x] Q3 spec delta 落点 → **新建 `agent-runtime`** + 三份 baseline 最小 delta
  - [x] Q4 对话 UI 形态 → **记录编辑页内半屏浮层**

---

## T0 · 实现授权检查点（闸门 2）

- [x] **T0 用户已明确给出实现授权**（2026-07-27）。闸门 3（真实 provider 联调）仍未授权。

---

## P1 · 后端 Runtime 基底

- [x] T-11 DDL 增量脚本：`agent_session` / `agent_message`（`backend/sql/mysql/c1-agent-runtime.sql` + `schema.mysql.sql` + 测试 H2 `schema.sql`），沿用现有命名与 FK CASCADE
- [x] T-12 domain + mapper + mapper XML（`AgentSession` / `AgentMessage` / `AgentStage` / `AgentSessionStatus` / `AgentMessageRole`）
- [x] T-13 `AgentStageMachine`：阶段推进纯逻辑（无 IO），推进原因由 `AgentStageDecision` 承载
- [x] T-14 `AgentStageMachine` 单测：正常推进、短回答追问上限、结束意图、轮次上限强制收束（10 tests）
- [x] T-15 `AgentPromptBuilder`：system prompt + 5 条最小护栏 + 阶段目标 + 上下文滑动窗口（7 tests）
- [x] T-16 `AgentGuardrailPolicy`：回复长度上限与句末裁剪（C1 唯一代码级硬护栏，5 tests）
- [x] T-17 `AgentChatService(+Impl)` + `AgentModelClient` + `AgentMockResponder`：会话编排、provider 调用、状态映射 `SUCCESS|UNAVAILABLE|FAILED`
- [x] T-18 失败语义单测：provider 未配置 → `UNAVAILABLE`；调用/解析失败 → `FAILED`；user 消息保留、assistant 不落库；同轮重试不重复落库（19 tests）
- [x] T-19 `AgentController` + DTO/VO：4 个端点（sessions / get / messages / finish），纳入 `/api/**` 鉴权
- [x] T-20 集成测试：`AgentControllerAuthIntegrationTest`（7）+ `AgentRuntimeIntegrationTest`（2，真实 H2 + MyBatis + mock provider 串联多轮/恢复/结束）
- [x] T-21 日志只输出结构化元数据（operation/stage/provider/durationMs/cause），对话原文与日记原文不进日志

## P2 · 前端对话入口

- [x] T-22 `services/agentService.ts`（沿用 `httpClient` 与 preview 拒绝策略）
- [x] T-23 `stores/agentChat.ts`：会话 / 消息 / 阶段 / loading / sending / 失败态 / 重试
- [x] T-24 `pages/record-editor/components/AgentChatSheet.vue`：半屏浮层，被动触发，可关闭
- [x] T-25 编辑页接入「让它陪你聊一会儿」入口（不弹窗、不自动展开、不改主路径视觉）
- [x] T-26 中断恢复：重新进入时恢复 `ACTIVE` 会话；半轮未完成时显式提示重试
- [x] T-27 素材回填：用户显式确认后经**已有** `persistDraft` 追加写入；拒绝则仅结束，正文不变
- [x] T-28 失败态与重试 UI（`UNAVAILABLE` / `FAILED` 文案克制；待重试时禁用输入）

## P3 · 验证

- [x] T-29 `mvn -B test`（backend 全量）：**254 tests / 0 failures / 0 errors**（新增 43）
- [x] T-30 前端 `type-check` PASS + `build:mp-weixin` **Build complete**
- [x] T-31 mock provider 下端到端串联验证（`AgentRuntimeIntegrationTest` 覆盖开场→多轮→恢复→结束→落库计数）
- [x] T-32 **闸门 3 · 偏差记录**：未取得事前显式授权。用户手验时本地脚本 `AI_PROVIDER=deepseek`，导致手验即真实 provider 调用（约 4 轮用户消息 / 7 条 Agent 回复，远低于 30 次预算上限）。**事后向用户披露并记入 AGENT_LOG，流程上应先取得授权再联调。**
- [x] T-33 真实 provider 多轮联调：DeepSeek 实际返回 4 轮引导，`reply` JSON 全部解析成功，日志无 `Agent provider issue`；真实数据落库 3 sessions / 11 messages
- [x] T-34 微信小程序手验：登录 → 开启对话 → 连续 4 轮推进 → 阶段正确到 `EXPECTATION`；**发现并修复浮层布局缺陷**（详见 T-34a）
- [x] T-34a 缺陷修复：`AgentChatSheet` 因 `max-height` + `min-height:360rpx` 导致 scroll-view 不内部滚动、composer 被顶出可视区（无法发送）且原生 textarea 层级覆盖消息（重叠）。改固定 `height:78vh` + `min-height:0` + 头尾 `flex-shrink:0`，复验通过
- [x] T-35 最小护栏手验：真实回复均为 1–2 句短问句、无诊断词、未改写用户原文；长度裁剪未被触发（provider 自发输出已在上限内）
- [x] T-36 scope safety check：无 Tool 调用、无 Memory、无后置过滤、三 Tab 与 V2.0 命名未变、无 package/lockfile 改动（sass 依赖已按规则规避）

## P4 · 收口

- [x] T-37 追加 `.ai/AGENT_LOG.md` 实现与验证证据（`Commit: pending`）
- [x] T-38 更新 `ACTIVE_TASK` Current Progress
- [x] T-39 输出 Required Output（modified files / what changed / verification / skipped 原因 / `git diff --stat` / scope safety / risks）
- [x] T-40 用户验收（2026-07-27）→ delta 接受进 `openspec/specs/` → 归档 `archive/2026-07-27-agent-runtime-mvp/` → `ACTIVE_TASK=IDLE`

## 遗留（不阻塞归档）

- 素材回填在 `record_id IS NULL` 且正文为空时会因内容校验失败而报错；手验未走到收束阶段，未触发。属已知缺口，可 Type B 修或纳入 C2。
- `agent_message` 已存有真实日记语义数据，后续本地重置数据库时需注意。
