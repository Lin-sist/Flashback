# Agent Tool Calling（C2）

> Change ID：`agent-tool-calling`
> Type：**C**
> 阶段：**规划闸（闸门 1）—— Q1–Q5 已定稿，待用户批准规划**
> 开工锚点：`63d1767`（工作区干净）
> 上游方向：`Docs/agent-iteration/roadmap/iteration-blueprint.md` v1.1 §4 C2（已冻结）
> 前置：C1 `agent-runtime-mvp` 已于 2026-07-27 验收归档，`ACTIVE_TASK=IDLE`

---

## 1. Why Now

C1 已交付 Agent Runtime 基底：会话、轮次、显式阶段状态机、最小护栏、失败显式语义，并已接受进 baseline（`openspec/specs/agent-runtime/spec.md`）。蓝图 v1.1 §3.2 的默认执行顺序下一刀就是 C2，且 C1 手验未观察到气质越界，无需把 C4 前移。

现在的断点是：**Agent 能聊，但聊完不能做事**。C1 里 Agent 唯一能落地的产物是「素材草稿」，而且回填动作完全由前端在用户点「用作正文」后自己拼字符串再走 `persistDraft()`——Agent 本身对后端零写权限（这是 C1 有意的范围约束，见 `agent-runtime` spec「Must Exclude Tools Memory And Post Filtering」）。

结果就是蓝图 C2 用户故事描述的坏事：Agent 说「要不要给这条加个'工作焦虑'标签」，用户得关掉对话浮层、回编辑器、找到标签选择器、手动勾选。对话里产生的意图，落不成行动。

C2 的价值是补上 Agent 的「手」：在对话中识别可执行意图 → 向用户提议 → 用户确认后由后端受控执行 → 结果回到对话上下文。同时这是 C3（Memory）、C5（可观测）都要复用的执行层。

---

## 2. 现状事实（能力五态）

> 事实来源：`backend/src/main/java/com/flashback/agent/**`、`service/impl/AgentChatServiceImpl.java`、`controller/api/AgentController.java`、`controller/api/RecordController.java`、`service/impl/RecordServiceImpl.java`、`controller/api/TagController.java`、`config/AppAiProperties.java`、`config/AppAgentProperties.java`、`backend/sql/mysql/c1-agent-runtime.sql`、`frontend/src/services/agentService.ts`、`frontend/src/stores/agentChat.ts`、`frontend/src/pages/record-editor/components/AgentChatSheet.vue`

### 2.1 Agent Runtime（C1 产出）

| # | 事实 | 状态 |
|---|---|---|
| F1 | Agent 端点共 4 个：`POST /api/agent/sessions`、`GET /api/agent/sessions/{id}`、`POST /api/agent/sessions/{id}/messages`、`POST /api/agent/sessions/{id}/finish`，全部返回 `ApiResponse<AgentSessionVO>` | `confirmed` |
| F2 | `AgentSessionVO` 字段：`sessionId / recordId / stage / sessionStatus / turnCount / maxTurns / canContinue / messages / materialDraft / source / status / message`；`status ∈ SUCCESS / UNAVAILABLE / FAILED` | `confirmed` |
| F3 | 阶段状态机在后端，不交给模型：`AgentStageMachine.decide(...)` 返回 `AgentStageDecision(nextStage, stageReaskCount, reason)`；阶段序列 `EMOTION → CONFUSION → CORE_QUESTION → EXPECTATION → CLOSING → ENDED` | `confirmed` |
| F4 | `AgentModelClient.complete(messages)` 固定 body `{model, messages, response_format: json_object, stream: false}`，只解析 `choices[0].message.content`，再 `extractText(content, field)` 取 `reply` / `material` | `confirmed` |
| F5 | 后端源码中**零处**出现 `tools` / `tool_choice` / `function_call` / `tool_calls`。原生 Function Calling 在本仓库**从未构造过、也从未验证过** | `confirmed` |
| F5b | `application.yml` / `-dev` / `-prod` 的 `app.ai.model` 默认值均为 `deepseek-v4-pro`，`base-url` 默认 `https://api.deepseek.com` | `confirmed` |
| F6 | provider 枚举 `MOCK / DEEPSEEK / OPENAI_COMPATIBLE`（`AppAiProperties.Provider`）；mock 分支由 `AgentMockResponder` 本地生成回复与素材 | `confirmed` |
| F7 | `AgentChatServiceImpl` 源码注释显式声明「C1 不调用任何记录写操作，Tool Calling 留给 C2」 | `confirmed` |
| F8 | 护栏为 `AgentGuardrailPolicy`：`guardrailClause()` 五条 system prompt 约束（不诊断 / 不覆写 / 建议不代决 / 被动陪伴 / 输出克制）+ `enforceReplyLength()` 代码级硬裁剪。**无后置内容过滤、无违规降级**（C4 范围） | `confirmed` |
| F9 | `AppAgentProperties`（prefix `app.agent`）：`maxTurnsPerSession=8`、`maxReplyChars=120`、`maxUserInputChars=1000`、`contextMessageWindow=12`、`draftExcerptChars=300`；无凭证字段（复用 `app.ai`） | `confirmed` |
| F10 | `agent_message` 有唯一键 `uk_agent_message_session_turn_role(session_id, turn_no, role)`；`AgentMessageRole` 只有 `USER / ASSISTANT`，**无 TOOL / SYSTEM 角色** | `confirmed` |
| F11 | C1 的失败重试语义已入 baseline：同一轮失败后须以相同内容重试，不重复落库、不推进阶段、不增加轮次 | `confirmed` |
| F12 | 无任何工具调用审计表（不存在 `agent_tool_call` 之类） | `confirmed` |

### 2.2 可作为 Tool 的既有后端能力

| # | 事实 | 状态 |
|---|---|---|
| F13 | 记录状态枚举 `RecordStatus = DRAFT / SEALED / UNLOCKED`；写操作的不可变闸在 `RecordServiceImpl.ensureDraft(Record, String)`（附件侧另有 `RecordAttachmentServiceImpl.ensureDraft`） | `confirmed` |
| F14 | `PUT /api/records/{id}` → `RecordService.update(userId, id, UpdateRecordRequest)`：`requireOwnedRecord` + `ensureDraft` + content 非空校验；`tagIds` 走 `validateTagIdsExist` + `rebindRecordTags` **全量重绑**（不是增量追加） | `confirmed` |
| F15 | 标签**写操作不存在**：`TagController` 只有 `GET /api/tags?type=`，`TagService` 只有 `listEnabled(TagType)`。给记录打标签只能经 `PUT /api/records/{id}` 传 `tagIds`（`@Size(max=20)`）；创建新标签的 service/端点未找到 | `confirmed` |
| F16 | `POST /api/records/{id}/seal` → `seal(userId, id)`：`ensureDraft` + 正文非空 + `unlockAt` 非空且晚于当前时间 | `confirmed` |
| F17 | 其余记录写操作：`DELETE /{id}`、`PUT/DELETE /{id}/location`、`PUT /{id}/cover`、`PUT /{id}/later-reflection`、`PUT /{id}/unlock-reminder-authorization`、附件 4 个端点，均有归属 + 状态校验 | `confirmed` |
| F18 | 读能力：`GET /api/records`、`/unlocked`、`/timeline`、`/{id}`，均经 `requireOwnedRecord` 或用户维度分页 | `confirmed` |

### 2.3 前端

| # | 事实 | 状态 |
|---|---|---|
| F19 | `agentService.ts` 4 个方法一对一映射 F1 的 4 个端点；preview 模式（无 token + 有 preview session）直接 reject，不打真实服务 | `confirmed` |
| F20 | `stores/agentChat.ts`：state `session/loading/sending/finishing/errorMessage`，actions `clear/applySession/startOrResume/send/finish/retry` | `confirmed` |
| F21 | `AgentChatSheet.vue` 为半屏浮层，emit `close/send/finish/retry/use-material/discard-material` | `confirmed` |
| F22 | 素材回填在 `record-editor/index.vue` 的 `persistAgentMaterial()`：前端把素材**追加**到 `form.content`（已有正文时 `\n\n` 拼接，不覆盖），再走既有 `persistDraft()` | `confirmed` |

### 2.4 待定与范围外

| # | 事实 | 状态 |
|---|---|---|
| F23 | DeepSeek 官方文档的 Tool Calls 示例即使用 `deepseek-v4-pro`（与 F5b 的本仓库默认 model 一致），并提供 **strict mode（Beta）**：`base_url` 改为 `https://api.deepseek.com/beta`、每个 function 设 `strict: true`，服务端校验用户提供的 JSON Schema，不合规或含不支持类型则报错。strict 模式支持 `object` / `string` / `number` / `integer` / `boolean` / `array` / `enum` / `anyOf`，`string` 支持 `pattern` 与 `format` 但**不支持** `minLength` / `maxLength`，`array` **不支持** `minItems` / `maxItems`，`object` 须全部属性 `required` 且 `additionalProperties: false`（[DeepSeek API Docs · Tool Calls](https://api-docs.deepseek.com/guides/tool_calls)，内容已改写以符合授权要求） | `partial`（官方文档确认支持，**本仓库一次未跑过**，须闸门 3 验证） |
| F24 | `tools` 与 `response_format: json_object` 的共存性 —— **已不再是决策依赖项**：原生 FC 路径下工具提议走 `choices[0].message.tool_calls`，自然语言回复走 `choices[0].message.content`，二者在同一响应中并存，不需要 json_object | `confirmed`（依据 F23 官方响应结构） |
| F25 | Tool 白名单具体范围（蓝图 P3） | **已由规划闸 Q2 定稿**（见 §7）；定稿前为 `unknown` |
| F29 | DeepSeek reasoner / distill 系模型的 FC 支持存在历史坑：R1 曾明确不支持（[DeepSeek-R1 issue #9](https://github.com/deepseek-ai/DeepSeek-R1/issues/9)），另有第三方报告称 distill 变体会返回空 `tool_calls`（[Fireworks 博客](https://fireworks.ai/blog/deepseek-models)）。**不得假设任意 OPENAI_COMPATIBLE provider / 任意 model 都支持 FC** | `partial`（非官方源，按风险提示对待） |
| F30 | 第三方报告称 V4 在 streaming + auto 模式下 `tool_calls` 解析不稳，切 `stream=false` 显著缓解（[vLLM issue #40801](https://github.com/vllm-project/vllm/issues/40801)）。本仓库 `AgentModelClient` 已固定 `stream: false`（F4），**保持不动** | `partial`（非官方源，按风险提示对待） |
| F26 | Memory / 历史记录检索 | `out_of_scope`（C3） |
| F27 | 系统化 Guardrails hardening（后置过滤、违规降级、边界用例集） | `out_of_scope`（C4） |
| F28 | 决策链路 thought→action→observation 可查询 | `out_of_scope`（C5） |

> **诚实性声明**：
> - F5、F12、F15 是「代码中确认不存在」，非推测。
> - **F23 是本 change 最关键的未验证依赖**：原生 FC 与 strict mode 由官方文档确认支持，但**本仓库一次都没跑过**，因此标 `partial` 而非 `confirmed`。闸门 3 验证通过前，任何文档不得把它写成已验证。
> - F29、F30 来自第三方来源，按风险提示而非事实对待。
> - F24 已由 `unknown` 转为 `confirmed`，依据是 F23 的官方响应结构（`tool_calls` 与 `content` 分属不同字段），**不依赖本仓库实测**。
> - F25 已由规划闸 Q2 定稿。

---

## 3. Goals

C2 SHALL 实现：

1. **原生 Function Calling 接入**：以 OpenAI-compatible `tools` + strict mode 作为 Agent 提议工具的唯一路径；工具 schema 由后端白名单生成并交 provider 服务端校验。
2. **Tool 注册与白名单机制**：后端集中声明 Agent 可见的工具集合，白名单外的一切后端写操作对 Agent 不可达；白名单是代码级强约束，不是 prompt 提示。
3. **二段式执行协议**：Agent 只能**提议**（propose）工具调用，工具的实际执行必须由用户在对话中显式确认后的独立请求触发。Agent 单次回复不得同时完成提议与执行，后端也不做单轮内的 FC 循环。
4. **Tool 执行层**：确认后的调用复用既有 `RecordService` / `TagService` 业务方法执行，继承其归属校验、状态校验与封存不可变约束，不新增绕过路径的 SQL。
5. **结果回注对话**：执行结果（成功 / 失败 / 已失效）写回会话上下文，Agent 下一轮可以基于「刚刚做了什么」继续说话。
6. **失败显式**：工具执行失败告知用户失败原因，不静默、不谎报成功、不把失败包装成「已完成」。
7. **审计留痕**：每次提议与执行落库为结构化审计记录（工具名、参数摘要、状态、失败类型），为 C5 可观测打底；不写入日记原文。

---

## 4. Non-Goals（本 change 明确不做）

- **不做 Memory / 历史记录检索 / 跨记录关联**（C3）。C2 的工具参数只来自当前会话与当前草稿。
- **不做系统化 Guardrails hardening**（C4）：不做后置内容过滤、不做违规降级机制、不建边界用例测试集。C2 只在既有 `AgentGuardrailPolicy` 上追加「工具相关」的 prompt 约束与代码级白名单。
- **不做决策链路查询接口 / 可观测面板**（C5）。C2 只落审计数据，不做查询端点。
- **不让 Agent 代替用户做重要决策**：封存（seal）、解锁、删除、后来其实、解锁提醒授权**一律不进白名单**，Agent 只能用自然语言建议并引导用户自行去编辑页确认。
- **不让 Agent 改写或覆盖用户已有正文**：正文类工具只允许**追加**，不允许替换、精简、润色、纠错既有内容。
- **不做位置、附件、封面类工具**（涉及封存不可变契约与对象存储副作用）。
- **不新增标签创建能力**：Agent 只能在既有启用标签集合中选择，不得创建新标签（避免 Agent 污染全局标签字典）。
- **不做自动执行 / 静默执行 / 批量执行**：无「用户已授权则以后不再询问」的免确认模式。
- **不做主动推送 / 弹窗**（被动召唤原则延续）。
- **不改三 Tab 结构**，不改 V2.0 用户可见命名（我的记录 / 时光轴 / 时间回看）。
- **不改 C1 已接受的 4 个 Agent 端点的既有字段语义**（只做向后兼容的字段新增）。
- **不改现有 `/api/ai/**`、`/api/stage-summaries/**` 三个单轮 AI 端点的契约**。
- **不做流式输出（SSE / streaming）**——另有第三方报告称 streaming 会加剧 `tool_calls` 解析不稳（F30），本 change 固定 `stream: false`。
- **不做 FC 到自研 JSON 协议的静默降级**：FC 不可用即显式 `UNAVAILABLE`，不维护第二条提议解析路径。
- **不改既有三个单轮 AI 端点的 `response_format=json_object` 链路**——原生 FC 只用于 Agent 对话路径。
- **不引入 MCP / Spring AI / LangChain4j 等 Agent 框架**（见 §12 长期演进备注）。
- **不做大规模 backend rewrite**：在既有 `agent` 包内增量新增，不重构 `RecordServiceImpl` 已有方法的对外行为。
- **不做前端视觉大改**：确认交互在既有 `AgentChatSheet.vue` 浮层内表达，不新增页面与路由。
- **不改 package / lockfile**。
- **不做 speech-to-text / voice AI / 情绪评分 / 诊断 dashboard**；不做 admin / 部署 / 监控 / 通知 / 设置页。

---

## 5. 用户故事

**改前**：用户和 Agent 聊完工作压力，Agent 说「你可以给这条记录加个'焦虑'标签」。用户关掉对话浮层，回到编辑器，翻标签选择器，找到「焦虑」，勾上，保存。Agent 说过的话和用户做的事之间没有连接——Agent 是个只会出主意的旁观者。

**改后**：Agent 说「我觉得这条和你说的工作压力有关，要不要加个'工作焦虑'的标签？」，浮层里出现一条克制的确认条：**加标签「工作焦虑」** ／ 「好」·「先不用」。用户点「好」，后端校验记录仍是草稿、标签在启用集合内、归属属于本人，然后追加标签（不动已有标签），把结果回注对话，Agent 接一句「加好了」。用户点「先不用」，什么都不发生，对话继续。如果记录在这期间已被封存，用户会看到明确的失败原因，而不是一句假的「已完成」。

---

## 6. 场景边界（Agent 气质对齐）

| 场景 | C2 期望行为 |
|---|---|
| Agent 想加标签 | 提议 + 等确认；用户不确认则永不执行 |
| Agent 想把素材写进正文 | 只能**追加**到正文末尾，且需用户确认；不得替换或改写已有文字 |
| 用户说「帮我封存吧」 | seal 不在白名单：Agent 只能建议用户去编辑页自己确认，**不**提议 seal 工具 |
| 用户说「把我刚才那段改通顺点」 | 拒绝改写原文，可以提议追加一段用户自己说过的内容 |
| 模型提议了白名单外的工具名 | 后端直接拒绝该提议，不下发确认条，按无提议处理，并记审计 |
| 用户连点两次确认 | 幂等：同一提议只执行一次，第二次返回已处理，不产生重复标签/重复正文 |
| 记录在提议后被封存 | 执行时 `ensureDraft` 拒绝，返回明确失败原因，会话不崩、素材不丢 |
| provider 挂掉 | 沿用 C1：`UNAVAILABLE` / `FAILED` 显式返回；已产生的提议不丢，记录保存与封存不受影响 |
| 工具执行失败 | 明确告知失败，不重试轰炸、不谎报成功 |

---

## 7. 待用户在规划闸确认的决策

以下 5 项影响实现，**未确认前不进入闸门 2**。取舍理由见 `design.md` §决策记录。

## 7. 规划闸决策（已定稿）

> 2026-07-27 用户在规划闸对 Q1–Q5 定稿。取舍理由见 `design.md` §决策记录。

| # | 决策项 | **定稿结论** |
|---|---|---|
| **Q1**（蓝图 P1，F23/F24/F29/F30） | Agent 提议工具的表达方式 | **原生 OpenAI-compatible Function Calling + strict mode**，作为 Agent 提议的**唯一**路径。不做「FC 失败静默降级到自研 JSON 协议」——FC 不可用时返回显式 `UNAVAILABLE`。理由见 design 决策 1 |
| **Q2**（蓝图 P3，F25） | Tool 白名单范围 | **3 个写工具**：`append_record_content`、`add_record_tags`、`propose_unlock_at`；**2 个读工具**：`list_available_tags`、`read_draft_snapshot`（后端预注入，非模型自主调用）。seal / delete / unlock / location / cover / attachment / later-reflection / 标签创建**全部代码级排除** |
| **Q3**（F10/F12） | 提议与执行的持久化落点 | **新表 `agent_tool_call`**（审计 + 幂等 + 状态机）；`agent_message` 表结构与唯一键**不动** |
| **Q4** | 执行入口的 API 形态 | **新增 1 个端点** `POST /api/agent/sessions/{sessionId}/tool-calls/{toolCallId}/confirm`（body 含 `decision: ACCEPT/REJECT`）；C1 四端点契约不变，`AgentSessionVO` 只做向后兼容字段新增 |
| **Q5** | spec delta 落点 | `agent-runtime`（主契约）+ `backend-core`（Tool 执行层与白名单）+ `miniapp-core`（确认交互）+ `v2-product-scope`（对话中产生行动的产品行为） |

### Q1 定稿的实现影响

| 项 | 影响 |
|---|---|
| `AgentModelClient` | **新增** tools 路径方法（构造 `tools` + 解析 `message.tool_calls` / `message.content`）；既有 `complete()` 保持不动 |
| `/api/ai/**`、`/api/stage-summaries/**` | **完全不碰**，继续走 `response_format=json_object`；C1 的 254 项测试基本不受冲击 |
| `AgentToolRegistry` | 职责从「生成 prompt 文本」升级为「生成 JSON Schema」；白名单仍是唯一事实源，且由 provider 服务端校验后变为更硬的类型约束 |
| `AgentMockResponder` | 需可伪造 `tool_calls` 响应形状（新增，非改写） |
| 配置 | strict mode 需 `/beta` base URL，与生产默认 `AI_BASE_URL` 不同 → 独立可配开关；并新增「model 是否支持 FC」的显式配置约束（F29） |
| 外调预算 | 30 → **45 次**（见 §8） |

### Q2 附带确认项

`add_record_tags` 的「追加」语义与 F14 现状冲突——`PUT /api/records/{id}` 是全量重绑 `tagIds` 且要求 content 非空。故在 `RecordService` **新增** `appendTags` 专用方法，而非复用 update。同理 `append_record_content` 新增 `appendContent`。取舍见 design 决策 5。

### Q2 保留待观察项

`propose_unlock_at` 是否越过「建议不代决」边界，规划闸判断为**不越界**（只写可逆的草稿字段，不封存）。若手验中体感越界，可在实现期从 registry 移除，其余设计不受影响。

---

## 8. 外调预算

| 阶段 | 外调 | 预算 |
|---|---|---|
| 规划闸（本阶段） | 无 | **0**（仅读本仓库代码 + 已引用的公开文档） |
| 实现（闸门 2 后） | 默认 `app.ai.provider=mock`，全部走 mock provider + 单元/集成测试 | **0** |
| 真实联调（闸门 3 单独授权后） | 真实 DeepSeek 多轮 + 原生 FC 提议验证 + strict mode schema 校验 | 上限 **≤ 45 次请求**（Q1 定稿为原生 FC，含 strict schema 被服务端接受/拒绝的验证、提议时机观察、`tool_calls` 与 `content` 并存确认）、单次超时 ≤ 10s。仅用测试账号自造内容，**不使用用户真实日记** |

**本地环境提醒**（来自 `ACTIVE_TASK` Residual）：本机 MySQL80 StartType=Manual，真实联调前需手动启动；`backend/start-dev-wechat.local.ps1` 明文存放 secret（已 gitignore），若本 change 期间要真实联调，建议先轮换并改为环境变量读取。

`git push` / 部署 / 发布：本 change **不申请**。

---

## 9. 提交责任

**Agent 代为提交**（2026-07-27 用户授权变更，原为用户手动提交）。验收仍由用户进行。
`push` / 部署 / 发布**未授权**，不执行。

---

## 10. 验收标准（C2 完成判定）

1. Agent 提议经由原生 `tools` 参数下发、经 `message.tool_calls` 解析；strict mode 下 schema 由 provider 服务端校验通过。
2. FC 不可用（model 不支持 / 服务端拒绝 schema / 响应缺失 `tool_calls` 结构）时返回显式状态，且**不存在**降级到自研 JSON 提议协议的代码路径。
3. 后端**不在**生成回复的同一处理过程内执行工具或回灌 tool 结果；每轮生成回复的 provider 请求次数有界（design 决策 9）。
4. 单轮返回多个 `tool_calls` 时只保留第一个合法提议，其余记审计（design 决策 10）。
5. 存在集中的 Tool 白名单声明；白名单外工具名的提议被后端拒绝，且不会下发给前端。
6. Agent 无法在单次回复中直接完成写操作——执行必须经用户确认的独立请求（可由代码审查 + 测试证明）。
7. strict mode 无法表达的边界（`text` 长度、`tagIds` 数量、`unlockAt` 时序）在代码层完成校验，未因服务端已校验结构而跳过。
8. 三个写工具的执行全部经既有 `RecordService` 路径，继承归属校验与 `ensureDraft`；SEALED / UNLOCKED 记录上的工具执行被拒绝。
9. 正文类工具只追加不覆写：执行后原有正文逐字保持不变。
10. 标签类工具只追加不清空：执行后原有标签仍在，且不创建新标签。
11. seal / delete / unlock / location / cover / attachment / later-reflection 在任何路径下都不可被 Agent 触发。
12. 同一提议重复确认幂等：不产生重复正文段落或重复标签。
13. 用户拒绝提议后记录零变更，会话可继续。
14. 工具执行失败返回明确失败语义，不谎报成功；记录保存与封存不受 Agent 可用性影响。
15. 审计记录落库，且**不含**日记原文与对话原文（参数摘要脱敏）。
16. C1 已接受契约未回归：4 个既有端点、失败重试语义、`maxTurns` 上限、跨用户隔离全部保持；既有 `complete()` 与三个单轮 AI 端点的 `json_object` 链路未改。
17. 后端单元测试覆盖白名单拒绝、幂等、状态校验拒绝、失败路径、schema 生成合规；集成测试用 mock provider 覆盖「提议 → 确认 → 执行 → 结果回注」全链路。
18. 后端既有测试全绿（C1 基线 254 项不回归）。
19. 前端 type-check + `build:mp-weixin` 通过。
20. 微信小程序手验：提议 → 确认执行 → 拒绝 → 重复确认 → 记录已封存时执行失败，证据写入 `.ai/AGENT_LOG.md`。
21. 手验证明 Agent 未出现代决行为（不自动封存、不改写原文）。
22. secret 未出现在前端代码或 tracked files；strict mode 的 `/beta` 地址走 backend config 且未新增凭证字段。

---

## 11. 建议实现顺序

1. 规划闸批准（Q1–Q5 已定稿，待放行）。
2. 后端：Tool 白名单声明 + schema 生成 + 参数校验 + `tool_calls` 解析（纯逻辑，可单测，零外调）。
3. 后端：`agent_tool_call` 持久化（MySQL DDL + H2 测试 schema + entity/mapper）。
4. 后端：Tool 执行层（复用 `RecordService` / `TagService`）+ 幂等 + 状态校验 + 失败语义。
5. 后端：`completeWithTools` + FC 可用性判定配置；确认端点 + 鉴权 + 越权/幂等测试；`AgentSessionVO` 向后兼容字段新增。
6. 后端：prompt 层预注入读工具内容与执行结果摘要；mock provider 侧伪造 `tool_calls`。
7. 前端：service + store + 浮层内确认条 + 失败态与拒绝态。
8. mock provider 下端到端验证 → 闸门 3 授权后真实联调 → 微信手验。
9. 输出 Required Output 字段，更新 `.ai/ACTIVE_TASK.md` 与 `.ai/AGENT_LOG.md`。

---

## 12. 长期演进备注（不属于本 change 范围）

> 用户已明确：蓝图与选型均为阶段性的，后续会持续向更有说服力的技术方向迭代。本节只记录**已识别的演进方向与观察依据**，供未来独立 change 讨论，**不授权在 C2 内实施**。

| 方向 | 与 C2 的关系 | 观察依据 |
|---|---|---|
| **MCP（Model Context Protocol）** | C2 选原生 FC 而非自研协议，使未来把 `AgentToolRegistry` 的 schema 复用为 MCP server 的 tool 定义成为**平滑迁移**而非重写 | MCP 自 2024-11 发布后采纳快速上升；第三方统计口径差异较大（企业生产采纳从 28% 到 78% 不等，且部分高值缺乏可靠来源），宜按「方向确定、具体比例不可靠」对待。多份 2026 年分析同时指出瓶颈已从协议连通性转向**工具质量、多用户授权、审计与执行可靠性** |
| **Spring AI** | C2 手写 `AgentModelClient` 不引入框架；若未来接入，C2 的白名单 + 二段式确认语义可作为其 tool callback 的上层约束 | 有报道称 Spring AI 已于 2025 年达到生产就绪并成为 Spring Boot 侧接入 LLM 的常见选择；同时指出 Agent 安全正成为新的问题面 |
| **Memory 检索的存储选型（C3）** | 蓝图 D7 定的是「MySQL 简单检索，不做向量中台」。但需注意 MySQL 的向量能力弱于同类：多方分析认为 PostgreSQL + pgvector 与 MongoDB 已有生产级向量方案，而 MySQL 侧仍在探索（云托管 MySQL 有 GA 版 vector search，但依赖特定维护版本）。若 C3 之后想升级语义检索，可能面临「留在 MySQL 做关键词/全文」与「引入 pgvector 或专用向量库」的分岔 | 见上 |

**共同结论**：C2 选原生 FC 的长期价值不只是叙事，而是让 tool schema、`tool_call_id`、`role: "tool"` 这三个标准结构成为 C3（多工具并行检索）、C5（thought→action→observation 链路）与未来 MCP / 框架接入的共同地基。自研 JSON 协议会在每一步都需要额外适配。

> 引用内容均已改写以符合授权要求；第三方来源的采纳率与结论按提示而非事实对待。
