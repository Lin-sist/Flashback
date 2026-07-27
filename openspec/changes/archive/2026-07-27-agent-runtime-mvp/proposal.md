# Agent Runtime MVP（C1）

> Change ID：`agent-runtime-mvp`
> Type：**C**
> 阶段：**规划闸（闸门 1）待批准**
> 开工锚点：`b6140b3`（工作区干净）
> 上游方向：`Docs/agent-iteration/roadmap/iteration-blueprint.md` v1.1 §4 C1（已冻结）

---

## 1. Why Now

M4 已归档（`openspec/changes/archive/2026-07-27-m4-real-capability-completion/`），核心 Mini Program 能力准生产可用，`ACTIVE_TASK=IDLE`。蓝图 v1.1 已冻结，C2–C5 全部硬依赖 C1，所以 C1 是唯一可开的下一刀。

当前产品的「AI」是单轮工具：一次请求 → 一次响应 → 结束。用户在新建记录时面对空白页，点「AI 写作提示」拿到 3 条静态问题列表，仍然不知道从何写起。Agent 不知道用户此刻在什么状态，也无法追问。

C1 的价值是把「单轮 prompt 工具」变成「可多轮、被动召唤、有克制护栏的 Agent Runtime 基底」，并用「写下此刻」这一个真实场景验证这个基底可用。

---

## 2. 现状事实（能力五态）

> 事实来源：`AiServiceImpl.java`、`AiController.java`、`AppAiProperties.java`、`application.yml`、`backend/pom.xml`、`frontend/src/services/aiService.ts`、`frontend/src/pages/record-editor/index.vue`、`backend/sql/mysql/schema.mysql.sql`

| # | 事实 | 状态 |
|---|---|---|
| F1 | 所有 AI 能力为单轮 `prompt → response`：`generateWritingPrompts` / `summarizeRecord` / `generateStageSummary` 各自构造 messages → `invokeChatCompletion` → 解析 JSON → 返回 VO。**无会话、无轮次、无上下文累积** | `confirmed` |
| F2 | `invokeChatCompletion(List<Map<String,String>> messages)` 已是 OpenAI-compatible `/chat/completions` 形状，固定 `response_format=json_object`、`stream=false`、超时来自 `app.ai.timeout-millis` | `confirmed` |
| F3 | provider 适配层存在：`MOCK` / `DEEPSEEK` / `OPENAI_COMPATIBLE`（`AppAiProperties.Provider`）；secret 只从 backend config 注入（`AI_API_KEY` 环境变量） | `confirmed` |
| F4 | 现有 AI 返回状态机为 `SUCCESS` / `UNAVAILABLE` / `FAILED` / `FALLBACK`，前端 `aiService.ts` 已按此判定 | `confirmed` |
| F5 | 现有端点：`POST /api/ai/writing-prompts`、`POST /api/ai/summarize-record`、`POST /api/stage-summaries/generate`，均在 `/api/**` JWT 鉴权链路内 | `confirmed` |
| F6 | 前端 `aiService.ts` 在「无 token + preview session」时直接 reject，不访问真实 AI | `confirmed` |
| F7 | `record-editor/index.vue` 已有 `organizeBeliefThen()` 调 `summarizeRecord`，是**用户主动点击**触发，无自动弹窗 | `confirmed` |
| F8 | `spring-boot-starter-data-redis` 已在 `pom.xml`，`application-dev/prod.yml` 已配 host/port/database，但**代码中零处使用**（无 `RedisTemplate` 引用），Redis 连通性从未被业务验证 | `confirmed` |
| F9 | MySQL 是唯一被验证的业务持久化；表命名 snake_case、含 `user_id` 归属列、FK `ON DELETE CASCADE`、时间列走 `Asia/Shanghai` 业务时区 | `confirmed` |
| F10 | DeepSeek 官方文档声明支持 OpenAI-compatible Function Calling（[DeepSeek API Docs · Tool Calls](https://api-docs.deepseek.com/guides/tool_calls)，内容已改写以符合授权要求）。**但本仓库从未验证过**，且 C1 不使用 FC | `partial` |
| F11 | 现有 AI prompt 全部内联在 `AiServiceImpl` 的 Java text block，无外部模板文件 | `confirmed` |
| F12 | 对话状态持久化方案（MySQL 新表 / Redis / 内存 session） | `unknown` → 本 proposal §7 给出推荐，**待用户在规划闸确认** |
| F13 | 多轮对话中 provider 输出稳定性。M4 联调记录显示 `summarize-record` 五次调用出现 1 次「返回内容无效」，说明结构化输出并非 100% 稳定 | `partial` |
| F14 | Agent 决策链路可观测（thought/action/observation） | `out_of_scope`（C5） |
| F15 | Memory / 历史记录检索 | `out_of_scope`（C3） |
| F16 | Tool Calling | `out_of_scope`（C2） |

---

## 3. Goals

C1 SHALL 实现：

1. **后端 Agent Runtime 基底**：会话（session）+ 轮次（turn）+ 阶段状态机 + 上下文维护，作为 C2–C5 的公共底座。
2. **一个真实场景**：「写下此刻」多轮写作引导——按 `情绪 → 困惑 → 核心问题 → 期望` 逐步引导，而非一次性给出问题列表。
3. **被动召唤入口**：用户在记录编辑页主动点击「让它帮我写」才开启对话；不弹窗、不推送、不自动展开。
4. **最小护栏内嵌**（system prompt 级，5 条）：不诊断、不覆写用户原文、建议不代决、被动召唤、输出克制。
5. **可中断 + 素材保留**：用户随时可结束对话；对话中产生的内容可作为草稿素材带回编辑器，由**用户决定**是否写入正文。
6. **失败显式**：provider 未配置或调用失败时返回明确 `UNAVAILABLE` / `FAILED`，不用 mock 冒充成功，不阻塞记录保存与封存。

---

## 4. Non-Goals（本 change 明确不做）

- **不做 Tool Calling / Function Calling**（C2）。Agent 在 C1 内不能触发任何后端写操作（除自身会话与消息落库）。
- **不做 Memory / 历史记录检索 / 跨记录关联**（C3）。C1 上下文只来自当前会话与当前草稿。
- **不做系统化 Guardrails hardening**（C4）：不做后置输出过滤、不做违规降级机制、不建边界用例测试集。C1 只有 system prompt 单层。
- **不做 Agent 决策链路可观测**（C5）。
- **不做主动推送 / 弹窗 / 未请求的分析**。
- **不改三 Tab 结构**（首页 / 时光轴 / 个人中心）。
- **不改 V2.0 用户可见命名**（我的记录 / 时光轴 / 时间回看）。
- **不改现有三个 AI 端点的请求/响应契约**，也不删除现有「AI 写作提示 / 整理你当时以为」入口。
- **不做 Agent 自动改写或替换用户正文**——素材回填必须由用户显式确认。
- **不做流式输出（SSE / streaming）**。
- **不做 speech-to-text / voice AI / 情绪评分 / 诊断 dashboard**。
- **不做大规模 backend rewrite**——新增 `agent` 模块，不重构 `AiServiceImpl` 已有三个方法的行为。
- **不做前端视觉大改**——对话 UI 是编辑页内的克制浮层/面板，不改编辑器主路径视觉。
- **不改 package / lockfile**（详见 §7 决策依赖）。
- **不做 admin / 部署 / 监控 / 通知 / 设置页**。

---

## 5. 用户故事

**改前**：用户点「新建记录」，面对空白正文。点「AI 写作提示」得到 3 条固定风格的问题（例如「你此刻最担心的是什么？」）。用户看完还是不知道怎么写，AI 也不知道用户回答了什么，没有下一步。

**改后**：用户点「让它帮我写」，Agent 开口问一句「今天是什么让你想写下这一刻？」。用户回一句「工作上有点撑不住」。Agent 不诊断、不说教，接着追问「是具体某件事，还是那种一直压着的感觉？」。三到四轮后，Agent 把这段对话里用户自己说过的话整理成一小段素材，用户点「用作正文」才写进编辑器；点「先不用」则只结束对话。用户中途关掉浮层，下次可以接着聊或重新开始。

---

## 6. 场景边界（Agent 气质对齐）

| 场景 | C1 期望行为 |
|---|---|
| 用户只回「嗯」「不知道」 | 换一个更小的切口再问一次；连续两次极短回答后主动收束，不逼问 |
| 用户说「不想聊了」 | 优雅结束（「好的，这些已经很好了。」），保留已有素材 |
| 用户描述疑似心理困扰 | 共情回应，**不**给诊断词、不给医学建议、不建议就医之外的判断 |
| 用户写两行，Agent 想长篇分析 | 回复长度与用户表达相称；单条回复有硬上限 |
| 用户说「帮我封存吧」 | 只能建议「要不要现在封存？可以在编辑页确认」，**不**调用封存 |
| provider 挂掉 | 明确告知不可用，对话可重试，已有素材不丢，记录保存/封存不受影响 |

---

## 7. 待用户在规划闸确认的决策

以下 4 项影响实现，**未确认前不进入闸门 2**。推荐方案与取舍理由见 `design.md` §决策记录。

| # | 待确认 | 推荐 | 备选 |
|---|---|---|---|
| **Q1**（蓝图 P2） | 对话状态持久化 | **MySQL 新表** `agent_session` + `agent_message` | Redis session（starter 已在但零使用、连通性未验证）；纯内存（重启即丢） |
| **Q2** | 对话消息是否落库存正文 | **落库**：素材保留与中断恢复需要它；按业务数据对待（与 `record.content` 同级），严禁进日志/telemetry/tracked files | 只存摘要（会破坏「原话回看」与素材保真） |
| **Q3** | spec delta 落点 | 新建 `openspec/specs/agent-runtime/`（主契约）+ `backend-core` / `miniapp-core` / `v2-product-scope` 各一小段 delta | 全部塞进 `backend-core`（会让 baseline 混杂 Agent 契约） |
| **Q4** | 对话 UI 形态 | 记录编辑页内**半屏浮层**，被动触发，可关闭 | 独立页面（会新增路由与返回态复杂度）；页内内联（挤压编辑器主路径） |

> 蓝图 P1（provider 是否支持 FC）：调研结论为 DeepSeek 官方声明支持 OpenAI-compatible tool calls（F10），但**本仓库未验证**，且 C1 不使用 FC。**结论：不阻塞 C1，验证留 C2。**

---

## 8. 外调预算

| 阶段 | 外调 | 预算 |
|---|---|---|
| 规划闸（本阶段） | 无 | **0**（仅读代码与公开文档） |
| 实现（闸门 2 后） | 默认 `app.ai.provider=mock`，全部走本地 mock provider 与单元/集成测试 | **0** |
| 真实联调（闸门 3 单独授权后） | 真实 DeepSeek / OpenAI-compatible 多轮调用 | 建议上限 **≤ 30 次请求**、单次超时 ≤ 10s，仅用测试账号自造内容，**不使用用户真实日记** |

`git push` / 部署 / 发布：本 change 不申请。

---

## 9. 提交责任

**用户手动提交**（默认）。Agent 不执行 `git add` / `commit` / `push`。

---

## 10. 验收标准（C1 完成判定）

1. 后端存在独立 `agent` 模块，未改动 `AiServiceImpl` 现有三个方法的对外行为。
2. 会话可创建、可多轮追加、可结束；同一用户的会话严格隔离，跨用户访问返回安全的未找到/拒绝。
3. 阶段状态机按 `情绪 → 困惑 → 核心问题 → 期望 → 收束` 推进，且不可跳过必要收束。
4. 单条 Agent 回复长度受硬上限约束；system prompt 明确禁止诊断与覆写原文。
5. Agent 在 C1 内不调用任何记录写操作 API（可由代码审查与测试证明）。
6. provider 未配置 / 调用失败 → 显式 `UNAVAILABLE` / `FAILED`，不返回 mock 成功，不影响记录保存与封存。
7. 会话中断后可恢复或重新开始；素材回填需用户显式确认。
8. secret 未出现在前端代码或 tracked files。
9. 后端单元测试覆盖状态机推进、轮次上限、越权访问、失败路径；集成测试用 mock provider 覆盖多轮 API。
10. 前端 type-check / Mini Program 构建通过（可行时）。
11. 微信小程序手验：开启对话 → 多轮 → 中断 → 恢复 → 素材回填 → 拒绝回填，证据写入 `.ai/AGENT_LOG.md`。
12. 最小护栏手验至少覆盖：不诊断、不覆写、输出克制三项。
13. 用户日记原文与对话原文未出现在应用日志中。

---

## 11. 建议实现顺序

1. 规划闸批准（Q1–Q4 定稿）。
2. 后端：会话/消息模型 + 持久化 + 状态机（纯逻辑，可单测）。
3. 后端：Agent 对话服务 + system prompt 与最小护栏 + provider 调用复用 `invokeChatCompletion` 形状。
4. 后端：Agent API 端点 + 鉴权 + 越权/限流/轮次上限。
5. 前端：service + store + 半屏对话浮层 + 被动入口。
6. 前端：中断恢复 + 素材回填确认 + 失败态。
7. mock provider 下端到端验证 → 闸门 3 授权后真实联调 → 微信手验。
8. 输出 Required Output 字段，更新 `ACTIVE_TASK` 与 `AGENT_LOG`。
