# Agent Tool Calling（C2）· Design

> Change ID：`agent-tool-calling`
> 阶段：**规划闸（闸门 1）—— Q1–Q5 已定稿** —— 本文不授权写业务代码
> 配套：`proposal.md`（范围与验收）、`tasks.md`（实施切片）、`specs/**`（契约 delta）
> 协议定稿：Agent 提议走**原生 OpenAI-compatible Function Calling + DeepSeek strict mode**，无降级路径（决策 1）

---

## 1. 架构总览

C2 在 C1 的 `agent` 包内增量新增一个执行层，**不重构** C1 已有的 6 个类与 4 个端点。

```text
┌─────────────────────────── 前端（record-editor 内浮层） ───────────────────────────┐
│ AgentChatSheet.vue ── 新增「工具提议确认条」（克制单行 + 好 / 先不用）              │
│ stores/agentChat.ts ── 新增 pendingToolCall 状态 + confirmToolCall action           │
│ services/agentService.ts ── 新增 confirmToolCall(sessionId, toolCallId, decision)   │
└───────────────────────────────────────┬────────────────────────────────────────────┘
                                        │ POST /api/agent/sessions/{sid}/tool-calls/{tid}/confirm
                                        ▼
┌─────────────────────────── 后端 AgentController（新增 1 端点） ─────────────────────┐
│                          AgentChatService.confirmToolCall(...)                      │
└───────────────────────────────────────┬────────────────────────────────────────────┘
                                        ▼
      ┌──────────────── 新增：Tool 层（com.flashback.agent.tool） ─────────────────┐
      │ AgentToolRegistry     白名单唯一事实源：工具名 → 规格；并生成 FC JSON Schema│
      │ AgentToolSchemaFactory 由 registry 产出 strict mode 合规的 tools 数组       │
      │ AgentToolProposal     由 message.tool_calls 解析出的结构化提议              │
      │ AgentToolValidator    白名单校验 + 参数类型/边界校验 + 记录归属预检         │
      │ AgentToolExecutor     执行分发：调用既有 RecordService / TagService          │
      │ AgentToolOutcome      执行结果（EXECUTED/FAILED/REJECTED + 原因）           │
      └───────────────────────────────────┬────────────────────────────────────────┘
                                          ▼
      ┌──────────── 既有业务层（不新增绕过路径，继承全部校验） ────────────────────┐
      │ RecordService.update / 新增 appendContent / appendTags   ← ensureDraft 生效  │
      │ TagService.listEnabled                                                       │
      └──────────────────────────────────────────────────────────────────────────────┘

      持久化：agent_session / agent_message（不动） + agent_tool_call（新增）
```

### 关键不变量

1. **模型永远不能直接执行**。`tool_calls` 只被解析成 `AgentToolProposal` 并落库为 `PROPOSED`；执行只能由用户确认请求驱动。**后端不在生成回复的同一处理过程内回灌 tool 结果给模型**——这是与通用 FC 循环的关键差异，见决策 9。
2. **白名单是代码级且服务端强制的**。`AgentToolRegistry` 是唯一事实源，下发给 provider 的 `tools` schema 由它生成；strict mode 下 provider 服务端会校验该 schema，工具名走 `enum`，参数类型由服务端约束，prompt 与白名单无从漂移。
3. **执行不绕业务层**。所有写操作走 `RecordService`，`ensureDraft` 与 `requireOwnedRecord` 天然生效（F13）。
4. **阶段推进权仍在后端**。工具确认不推进 `AgentStage`、不增加 `turnCount`，避免和 C1 的失败重试语义（F11）互相污染。
5. **无协议降级**。FC 不可用（model 不支持 / schema 被拒 / 响应结构缺失）→ 显式 `UNAVAILABLE`，不存在第二条提议解析路径。
6. **单轮至多一个提议**。`tool_calls` 可能返回多个，后端只取第一个合法提议、其余记审计后丢弃（决策 10）。

---

## 2. 数据流

### 2.1 提议产生（在既有 `POST /messages` 一轮内）

```text
用户消息 → AgentStageMachine.decide（不变）
        → AgentPromptBuilder 组装 messages（新增：最近工具执行结果摘要 + 读工具预注入内容）
        → AgentToolSchemaFactory 由 registry 生成 tools 数组（strict: true）
        → AgentModelClient.completeWithTools(messages, tools)
             POST {baseUrl}/chat/completions
             body: { model, messages, tools, stream: false }   ← 不带 response_format
        → 解析 choices[0].message
             ├─ content     → Agent 自然语言回复（走既有 enforceReplyLength 裁剪）
             └─ tool_calls  → 0 个 → 与 C1 行为完全一致
                              ≥1 个 → 取第一个 → AgentToolValidator 校验
                                        ├─ 不通过 → 丢弃提议，只保留 content，落审计 REJECTED_BY_GUARD
                                        └─ 通过   → 落 agent_tool_call(status=PROPOSED)
                                                  → AgentSessionVO.pendingToolCall 下发
```

**要点一**：提议校验失败**不**让整轮失败。Agent 该说的话照说，只是没有确认条。模型幻觉一个工具名不该毁掉用户这一轮的对话体验。

**要点二**：`content` 与 `tool_calls` 在同一响应中并存（proposal F24）。但 FC 场景下模型可能只给 `tool_calls` 而 `content` 为空——此时后端使用提议自带的 `askText` 参数作为对话回复（每个工具 schema 都要求该参数），避免出现「有确认条但 Agent 没说话」的空白气泡。这是把「提议话术」也纳入 strict schema 约束的原因。

**要点三**：`response_format` 在此路径**不下发**。既有 `complete()`（三个单轮 AI 端点在用）保持 `json_object` 不变。

### 2.2 确认与执行（新端点）

```text
POST /sessions/{sid}/tool-calls/{tid}/confirm  { decision: ACCEPT | REJECT }
  → requireOwnedSession（沿用 C1 归属校验）
  → 取 agent_tool_call，校验 session 归属一致
  → status != PROPOSED → 幂等返回当前状态（不重复执行）
  → decision=REJECT → status=REJECTED，记录零变更
  → decision=ACCEPT → AgentToolExecutor 执行
        ├─ 成功 → status=EXECUTED，结果摘要回注对话上下文
        └─ 失败 → status=FAILED + failureType，显式告知，记录零变更
  → 返回 AgentSessionVO（含更新后的 pendingToolCall / toolCallResult）
```

### 2.3 结果回注对话

执行结果以**结构化摘要**（非日记原文）追加进下一轮 prompt 的上下文段，例如「上一步已为该记录追加标签：工作焦虑」。这样 Agent 下一轮能说「加好了」而不是重复提议。

不落 `agent_message` 表（避免撞 F10 唯一键，且工具事件不是对话原文），改由 `AgentPromptBuilder` 在组装时从 `agent_tool_call` 读最近若干条 `EXECUTED/FAILED` 记录拼接。

---

## 3. 白名单设计（Q2 定稿）

| 工具名 | 类型 | 参数 | 前置校验 | 语义 |
|---|---|---|---|---|
| `append_record_content` | 写 | `text`、`askText` | 归属 + DRAFT + 会话已绑定 recordId | 追加到正文末尾（`\n\n` 分隔），**不覆写** |
| `add_record_tags` | 写 | `tagIds`、`askText` | 归属 + DRAFT + 合并后总数 ≤ 20（F14 约束） | 在既有标签基础上**追加**，不清空、不创建新标签 |
| `propose_unlock_at` | 写 | `unlockAt`、`askText` | 归属 + DRAFT + 晚于当前时间 | 只写 `unlock_at` 字段，**不封存**；封存仍需用户去编辑页确认 |
| `list_available_tags` | 读 | —（后端预注入，不注册为 FC tool） | 无 | 启用标签清单随 prompt 注入，供模型选取 |
| `read_draft_snapshot` | 读 | —（后端预注入，不注册为 FC tool） | 归属 + 会话已绑定 recordId | 当前草稿正文摘要与已有标签随 prompt 注入 |

**明确排除**（代码级不注册，模型即使提议也会被拒）：`seal`、`delete`、解锁相关、`later-reflection`、`location`、`cover`、附件全部端点、`unlock-reminder-authorization`、标签创建。

### 3.1 读工具为何不注册为 FC tool

两个读工具的数据量都很小且**每轮都需要**（标签清单、草稿快照）。若注册为 FC tool，模型要先发一轮 `tool_calls` 拿数据、后端回灌、再发一轮才能提议——在「后端不做单轮内 FC 循环」的约束下（决策 9）这做不到。所以它们退化为 prompt 预注入：`AgentToolRegistry` 仍声明它们（作为白名单与审计的完整视图），但 `AgentToolSchemaFactory` 只把**写工具**放进下发的 `tools` 数组。

### 3.2 strict mode schema 约束（依据 proposal F23）

strict mode 有几条硬限制直接影响 schema 写法：

- `object` 的全部属性必须 `required`，且 `additionalProperties: false`
- `string` **不支持** `minLength` / `maxLength`；可用 `pattern` 与 `format`
- `array` **不支持** `minItems` / `maxItems`

因此**长度与数量边界无法交给 provider 校验**，必须留在 `AgentToolValidator` 里做代码级二次校验：

| 约束 | 落点 |
|---|---|
| 工具名合法 | schema `enum`（服务端） |
| `text` 非空且 ≤ 上限字符 | `AgentToolValidator`（代码级，因 strict 不支持 maxLength） |
| `tagIds` 元素为整数 | schema `array` + `integer`（服务端） |
| `tagIds` 数量 1–5、⊆ 启用集、合并后 ≤ 20 | `AgentToolValidator`（代码级，因 strict 不支持 maxItems） |
| `unlockAt` 形如 ISO 本地日期时间 | schema `pattern`（服务端） |
| `unlockAt` 晚于当前时间 | `AgentToolValidator` + `RecordService`（代码级） |
| `askText` 非空且 ≤ `maxReplyChars` | `AgentGuardrailPolicy.enforceReplyLength`（复用 C1） |

结论：strict mode 把**类型与形状**前移到服务端，**业务边界**仍归后端。两者叠加后的防御强度高于 C1 的 `json_object` 单层解析。

### 3.3 FC 可用性判定（依据 proposal F29）

新增配置项（`app.agent` 下，无凭证）：

| 配置 | 用途 |
|---|---|
| `tool-calling-enabled` | 总开关；关闭时 Agent 退回 C1 纯对话行为（**不是**降级到自研提议协议） |
| `strict-mode-enabled` | 是否启用 strict mode（需配合 `/beta` base URL） |
| `strict-mode-base-url` | strict mode 专用 base URL，默认空；为空且 `strict-mode-enabled=true` 时视为配置错误 |
| `function-calling-models` | 已确认支持 FC 的 model 白名单；当前 `app.ai.model` 不在其中时 `tool-calling` 不生效并记结构化日志 |

`AgentModelClient.unavailableReason()` 扩展一个工具维度的判定，沿用 C1 的显式不可用语义。

### 3.4 为什么 `propose_unlock_at` 可以进白名单

它写的是「解锁时间」这个可逆的草稿字段，记录仍是 DRAFT、仍可继续改、仍需用户自己点封存。它不构成「代替用户做重要决策」。真正不可逆的那一步（seal）依然由用户在编辑页完成。规划闸已判定不越界；若手验体感越界，实现期可从 registry 移除，其余设计不受影响。

---

## 4. 验证策略

| 层 | 手段 | 外调 |
|---|---|---|
| 白名单 / 参数校验 / `tool_calls` 解析 | JUnit 5 + AssertJ 纯单测（沿用 C1 的 `agent/*Test` 风格） | 0 |
| **tools schema 生成** | 单测断言生成的 schema 满足 strict 约束：全属性 `required`、`additionalProperties: false`、未使用 `maxLength` / `maxItems` / `minItems` | 0 |
| **FC 可用性判定** | 单测：model 不在 `function-calling-models` 时不下发 tools；`strict-mode-enabled` 但 base URL 为空时报配置错误 | 0 |
| 幂等 / 状态机 / 失败语义 | Mockito 单测 + `@SpringBootTest` + H2 集成测试（沿用 `AgentRuntimeIntegrationTest` 模式） | 0 |
| 端到端提议→确认→执行 | mock provider 集成测试（`AgentMockResponder` 伪造 `tool_calls` 响应形状） | 0 |
| **无降级路径** | 代码审查 + 测试：FC 不可用时返回显式 `UNAVAILABLE`，仓库内不存在第二条提议解析实现 | 0 |
| 鉴权 / 越权 | MockMvc + `@MockBean`（沿用 `AgentControllerAuthIntegrationTest`） | 0 |
| 前端 | `type-check` + `build:mp-weixin` | 0 |
| 真实 provider FC 联调 | 闸门 3 授权后真实调用：strict schema 被服务端接受、`tool_calls` 与 `content` 并存、提议时机、`content` 为空时 `askText` 兜底 | ≤ 45 |
| 微信手验 | 真机：提议 / 确认 / 拒绝 / 重复确认 / 封存后失败 | 含在上一行预算内 |

**回归底线**：C1 基线 254 项后端测试必须全绿。任何为 C2 便利而修改 C1 已有断言的行为，都视为契约回归，需在 AGENT_LOG 显式披露并请示。

**strict mode 失败的处置**：若闸门 3 验证中 provider 拒绝我们的 schema，处置顺序是「修 schema」→「关 `strict-mode-enabled` 仅用普通 FC」→ 二者都不成立才升级为架构问题请示。**任何情况下都不退回自研 JSON 提议协议**（决策 1）。

---

## 5. 隐私与安全

- `agent_tool_call` 长期只存：工具名、参数的**结构化摘要**（如 `tagIds`、`text` 的字符数与哈希前缀，而非正文原文）、状态、失败类型、时间戳。理由见决策 6。
- **例外与其边界（实现期新增，见决策 12）**：`pending_args` 列在提议**待确认期间**保存执行所需入参，提议一经终结即由 SQL 置 NULL。因此审计表不留日记文本的**长期**副本。
- 应用日志只出结构化元数据（sessionId / toolCallId / tool / status / failureType / 耗时），沿用 C1 已接受的日志契约。
- 新端点在 `/api/**` JWT 链路内，`@CurrentUser` + 会话归属 + toolCall 归属三重校验。
- 无新增 secret；provider 凭证仍只来自 `app.ai` backend config。strict mode 的 `/beta` base URL 是**地址**而非凭证，仍走 backend config。
- 白名单排除项覆盖了全部对象存储副作用路径（附件 / 封面），C2 不引入任何 OSS 调用。
- **FC 特有的暴露面**：`append_record_content` 的 `text` 参数会随 `tool_calls` 从 provider 返回，本质上仍是用户表达的再组织，与 C1 已接受的「素材草稿」同级——不新增外发内容类型，仍不落审计原文（决策 6）。
- **prompt injection 边界**：用户日记正文会作为只读参考进入 prompt（C1 已有行为）。若正文中包含诱导性指令试图触发工具，防线是白名单 + 二段式确认，而非内容识别——即便模型被诱导提议，也只能提议白名单内工具，且必须用户点确认才执行。这是选二段式的额外收益（决策 2），系统化的注入用例测试属 C4。

---

## 6. 决策记录

### 决策 1：Agent 提议工具用「扩展 JSON 输出」还是「原生 Function Calling」

1. **面临的选择**：(a) 扩展既有 JSON 协议，在 `{"reply":...}` 上加 `proposal` 字段，继续用 `response_format=json_object`；(b) 用 OpenAI 原生 `tools`，解析 `choices[0].message.tool_calls`，并启用 DeepSeek strict mode 让服务端校验 schema；(c) 双路径——原生优先、失败静默降级到 JSON 协议。
2. **选了哪个 + 为什么**：**(b)，2026-07-27 用户在规划闸定稿**。规划初稿曾推荐 (a)，理由是「`tools` 与 `json_object` 共存性未知、风险落在实现期」；查阅官方文档后该前提不成立，四条事实推翻了它：其一，共存性根本不是问题——FC 路径下工具提议在 `message.tool_calls`、自然语言回复在 `message.content`，二者天然并存，不需要 `json_object`（F24 已由 `unknown` 转为 `confirmed`）。其二，官方 Tool Calls 示例用的就是 `deepseek-v4-pro`，而这正是本仓库 `app.ai.model` 的默认值（F5b），不是不支持 FC 的 reasoner 系。其三，**strict mode 直接消掉了 (a) 的唯一优势论据**——我原本用「解析失败降级为只有 reply」来缓解 prompt 遵从度问题（M4 观察到 json_object 下 5 次有 1 次无效），但 strict mode 由服务端校验 JSON Schema，白名单里的工具名可以用 `enum` 约束、`tagIds` 用 `array`、`unlockAt` 用 `pattern`，于是**白名单从一句 prompt 提示变成服务端强制的类型约束**，可靠性反过来高于 (a)。其四，长期地基：`tools` 原生支持多工具并行（C3 的检索工具与写工具可同数组声明），`tool_call_id` + `role: "tool"` 正是 C5 要记的 thought→action→observation 三段结构的标准落点，而 MCP、Spring AI、LangChain4j 的抽象层统统建立在 OpenAI tools schema 上——选 (a) 会在 C3、C5 和未来每一次框架接入时都需要额外适配。改动面也比预想小：新增 tools 路径方法即可，既有 `complete()` 与三个单轮 AI 端点完全不碰，C1 的 254 项测试基本不受冲击。
3. **放弃的代价**：选 (a) 的代价现在看是三重的——技术上放弃 strict mode 的服务端类型校验（等于把已有的强约束换成弱约束）、演进上给 C3/C5/MCP 都留一层要拆的私有适配、叙事上「自研 JSON 提议协议」的技术含量其实**低于**原生 FC，却容易被读成「不知道有 FC 才这么做」。选 (b) 的真实代价是引入两项本仓库未验证的依赖：strict mode 是 Beta 且要求 `/beta` base URL（与生产默认 `AI_BASE_URL` 不同，须独立可配），以及必须显式约束「哪些 model 支持 FC」——F29 记录了 R1 曾明确不支持、distill 变体有返回空 `tool_calls` 的第三方报告，不能假设任意 OPENAI_COMPATIBLE provider 都行。这两项都落在闸门 3 验证。选 (c) 的代价最大：既要维护两条解析路径与两套 mock，又与已接受 baseline「不得 mock success 冒充真实成功」的守护意图相悖——静默降级会制造「用户以为在跑 FC、实际在跑另一套解析」的模糊状态，所以本 change 明确**不做降级**，FC 不可用即显式 `UNAVAILABLE`（沿用 C1 已有失败语义）。

### 决策 2：工具执行是「Agent 单轮内直接执行」还是「二段式提议 + 用户确认」

1. **面临的选择**：(a) Agent 判断需要就直接执行，执行结果写进回复；(b) 二段式——Agent 只提议，用户点确认后由独立请求执行；(c) 按工具风险分级，读工具直接执行、写工具需确认。
2. **选了哪个 + 为什么**：(b)。`AGENTS.md` 与 baseline `agent-runtime` spec 都写明「Agent SHALL 只给出建议并引导用户自行确认」「SHALL NOT 代为执行」，而蓝图 §6.3 把「代替用户做重要决策」列为绝对禁止。二段式是把这条产品约束**结构化落进 API 形状**——不是靠 prompt 自觉，而是物理上不存在「模型直接触发写操作」的代码路径。
3. **放弃的代价**：选 (a) 会让「建议不代决」退化成 prompt 层的君子协定，模型一次幻觉就能改用户数据，且违反已接受 baseline，属于契约回归。选 (c) 表面合理，但读工具若允许模型自主调用，就需要在单轮内做「模型请求 → 后端执行 → 再回模型」的多跳循环，等于把 Runtime 复杂度提前引入，而 C2 的两个读工具（标签清单、草稿快照）完全可以在组装 prompt 时**由后端主动预注入**，根本不需要模型来「调用」。这也是本 change 把读工具定位成「registry 声明 + 后端预注入」而非「模型自主调用」的原因。

### 决策 3：白名单里放哪些工具，为什么把 seal 排除

1. **面临的选择**：(a) 只放 `add_record_tags`，最小验证 Tool 链路；(b) 放 3 写 + 2 读（含 `propose_unlock_at`），排除 seal/delete/unlock；(c) 放开含 seal，理由是「用户确认过就等于用户自己封的」。
2. **选了哪个 + 为什么**：(b)。(b) 覆盖了三种不同形状的写操作——文本追加、集合追加、字段设置——足以证明 Tool 层的通用性，同时每一个都作用在**可逆的 DRAFT 字段**上。seal 被排除的关键不是「用户没确认」，而是**封存是产品里唯一不可逆的一步**：封存后 location/attachments/cover 立即冻结（`AGENTS.md` Non-Negotiable），正文不可再改。这一步的仪式感属于用户，Agent 参与其中会破坏产品气质。
3. **放弃的代价**：选 (a) 只做一个工具，白名单机制、幂等、结果回注这些设计的必要性无法被验证，C2 会显得像给标签功能加了个语音遥控器。选 (c) 会同时违反 `AGENTS.md`「封存后禁止修改」的守护意图与蓝图 C2 非目标「Agent 不得代替用户做封存/解锁等重要决策」，即便加了确认条也是把不可逆操作的入口交给了模型的提议逻辑——一旦提议话术有歧义，用户可能在没完全理解的情况下点了「好」，然后记录永久锁死。

### 决策 4：提议与执行记录存哪里——复用 `agent_message` 还是新表

1. **面临的选择**：(a) 复用 `agent_message`，新增 `TOOL` role；(b) 新建 `agent_tool_call` 表；(c) 只放内存/会话字段，不持久化。
2. **选了哪个 + 为什么**：(b)。硬性障碍是 `agent_message` 上的唯一键 `uk_agent_message_session_turn_role(session_id, turn_no, role)`（F10）——同一轮同一 role 只能有一条，而一轮里可能先提议、再拒绝、再产生新提议。要走 (a) 就得改这个唯一键，而它正是 C1 失败重试幂等性（F11）的实现基石，动它等于动已接受契约。另外提议有自己的状态机（PROPOSED → EXECUTED/FAILED/REJECTED），塞进对话原文表会让两种语义纠缠。
3. **放弃的代价**：选 (a) 需要变更 C1 已验收的唯一键约束，回归风险直接落在「失败重试不重复落库」这条已接受 spec 上，代价远超新建一张表。选 (c) 会让幂等无从实现（用户重复点确认就会重复执行）、审计无从留痕、C5 可观测失去数据源，且服务重启后待确认提议凭空消失。

### 决策 5：标签「追加」怎么实现——复用全量重绑端点还是新增 service 方法

1. **面临的选择**：(a) Tool 层先读当前 `tagIds`，合并后调既有 `RecordService.update`（全量重绑，F14）；(b) 在 `RecordService` 新增 `appendTags(userId, id, tagIds)` 专用方法；(c) Tool 层直接操作 `RecordTagMapper`。
2. **选了哪个 + 为什么**：(b)。(a) 的问题是 `UpdateRecordRequest` 要求 content 非空且会一并覆写标题、类型等字段——Tool 只想加标签却被迫提交整个记录快照，一旦并发（用户在编辑器里同时改正文）就会用旧快照覆盖新内容，属于静默数据丢失。(b) 把「只追加标签」表达成一个语义精确的方法，内部仍走 `requireOwnedRecord` + `ensureDraft`，是既有校验的延伸而非绕过。同理 `append_record_content` 也走新增的 `appendContent` 方法，在后端保证「追加不覆写」，而不是像 C1 那样依赖前端拼字符串（F22）。
3. **放弃的代价**：选 (a) 引入并发覆写风险，且把 Tool 的失败原因和「正文为空」这类无关校验绑在一起（正好撞上 ACTIVE_TASK 里记的 C1 遗留缺陷）。选 (c) 绕过 service 层就绕过了 `ensureDraft`，封存不可变约束会出现一个只有 Agent 能走的后门——这是本 change 最不能犯的错误。

### 决策 6：审计记录里存不存工具参数原文

1. **面临的选择**：(a) 完整存参数 JSON（含 `append_record_content` 的 text 原文）；(b) 只存结构化摘要（工具名、tagIds、text 长度 + 哈希前缀）；(c) 什么都不存，只存工具名与状态。
2. **选了哪个 + 为什么**：(b)。`append_record_content` 的 text 直接来自用户日记语境，属于 `AGENTS.md` 定义的高敏数据。已接受的 `agent-runtime` spec 规定日记原文与对话原文「只存在于业务存储中」——`agent_message` 和 `record.content` 是被授权的业务存储，审计表不是。存摘要既能支撑幂等判重（哈希）与问题定位（长度、工具、失败类型），又不制造第二份原文副本。
3. **放弃的代价**：选 (a) 等于在审计表里复制一份日记原文，扩大了泄露面（备份、导出、后续 C5 可观测查询都会碰到它），违背最小暴露原则。选 (c) 会让重复确认无法判重、失败无法定位，审计等于摆设，C5 也拿不到有用数据。

### 决策 7：为什么不在 C2 顺手做后置输出过滤

1. **面临的选择**：(a) 既然要解析模型输出，顺手把 C4 的后置内容过滤一起做；(b) 严格守住 C2 边界，只做工具相关的校验（白名单 + 参数），内容合规仍只有 C1 的 system prompt 单层。
2. **选了哪个 + 为什么**：(b)。已接受的 `agent-runtime` spec 明确写着「C1 SHALL NOT 包含后置输出过滤或违规降级机制，系统化 hardening SHALL 留给后续独立 change」，而蓝图把这件事指派给 C4 并要求配套边界用例测试集与违规降级设计。在 C2 里做半套过滤，会造成 C4 启动时「现状事实」含糊——既不是没有，也不成体系，反而更难验收。注意区分：C2 的白名单拒绝是**工具权限**校验（拒绝一个不存在的工具名），不是**内容合规**过滤（拦截诊断性措辞），两者不重叠。
3. **放弃的代价**：C2 期间模型仍可能在提议话术里说出越界表达（例如带诊断色彩地解释「为什么建议加焦虑标签」），只有 system prompt 与长度硬上限兜着。这个风险 C1 已作为「已接受风险」记录在 ACTIVE_TASK，C2 沿用同一立场；若手验中出现明显越界，按蓝图 §3.2 的调整规则，可以把 C4 提到 C3 之前处理。

### 决策 8：工具确认要不要推进阶段 / 计入轮次

1. **面临的选择**：(a) 确认执行视为一轮，`turnCount + 1` 并推进 `AgentStage`；(b) 确认不影响阶段与轮次，只改 toolCall 状态。
2. **选了哪个 + 为什么**：(b)。C1 已接受的失败重试语义（F11）建立在「一轮 = 一条用户消息 + 一条 Agent 回复」的严格配对上，`uk_agent_message_session_turn_role` 是它的物理保证。若确认动作也占轮次，就会出现「有 turn_no 但没有配对用户消息」的空洞轮次，直接破坏「恢复到未完成的一轮」这条已接受 scenario 的判定逻辑。另外把确认计入 `maxTurnsPerSession=8` 会让用户每同意一次工具就损失一轮对话额度，体验上莫名其妙。
3. **放弃的代价**：选 (b) 的代价是「工具执行」在对话时间线上没有独立的消息气泡，需要靠前端把结果渲染成轻量提示而非消息条——这是刻意的，工具结果不是 Agent 说的话，把它伪装成对话消息反而会让「Agent 说了什么」这份记录不再纯粹。

### 决策 9：是否在单轮内做完整的 FC 循环（模型请求 → 后端执行 → 结果回灌模型）

1. **面临的选择**：(a) 标准 FC 循环——收到 `tool_calls` 就执行，把结果以 `role: "tool"` 回灌，再让模型基于结果继续说话，直到没有 tool_calls；(b) 单跳——`tool_calls` 只转成待确认提议，本轮到此结束，执行发生在用户确认的独立请求里，结果在**下一轮** prompt 中以摘要形式出现。
2. **选了哪个 + 为什么**：(b)。(a) 是 FC 的教科书用法，但它与本产品的核心约束正面冲突：循环里的执行是**模型驱动**的，用户没有插入确认的位置，这直接违反已接受 baseline「Agent SHALL NOT 代为执行」与决策 2 的二段式。换句话说，采用原生 FC 的**协议**（tools schema、tool_calls 结构）不等于必须采用它的**控制流**。(b) 保留了协议的全部长期收益（schema 服务端校验、多工具声明、C5 可用的 `tool_call_id`），同时把控制权从模型收回到用户手上。附带好处是每轮请求次数恒定为 1，token 与延迟可预测，不会出现循环失控。
3. **放弃的代价**：(b) 的代价是 Agent 在提议的那一轮里**不知道执行结果**，所以它当轮只能说「要不要……？」而不能说「我已经加好了，另外……」——衔接感弱于标准循环。缓解是 2.3 的结果回注：下一轮 prompt 带上执行摘要，Agent 能自然接上。另一个代价是 `role: "tool"` 这个标准消息角色在 C2 里用不上，C5 记链路时需要自己把 `agent_tool_call` 映射成 observation 段。选 (a) 的代价则是把不可控的模型驱动执行放进产品，且一次对话可能触发多次真实外调，预算与失败面都不可预测——对一个「安静克制」的日记产品是过度工程。

### 决策 10：单轮返回多个 `tool_calls` 时怎么办

1. **面临的选择**：(a) 全部落成多个待确认提议，前端列出多条确认条；(b) 只取第一个合法提议，其余记审计后丢弃；(c) 整轮判为无效提议。
2. **选了哪个 + 为什么**：(b)。产品气质要求「输出克制」，一次对话里塞三条确认条会让浮层变成待办清单，这与「共情型朋友」相悖。(b) 也让前端状态简单——`pendingToolCall` 是单值而非列表，幂等边界清晰。丢弃的提议仍进审计，便于观察模型是否倾向批量提议。
3. **放弃的代价**：选 (a) 会引入「部分确认 / 部分拒绝」的组合状态，幂等与结果回注都要按提议粒度维护，复杂度上升而用户价值可疑。选 (c) 过于严苛——模型多提一个工具就丢掉整轮提议，用户体感是 Agent 忽然变笨。(b) 的代价是若模型确实想做两件事，用户这一轮只会看到一件；但下一轮 Agent 仍可再提，损失有限。

### 决策 11：为什么本 change 不引入 MCP 或 Spring AI

1. **面临的选择**：(a) 直接把工具暴露为 MCP server，走行业标准协议；(b) 引入 Spring AI 的 tool callback 抽象，少写 provider 适配代码；(c) 手写 FC 接入，不引入框架与额外协议。
2. **选了哪个 + 为什么**：(c)。MCP 解决的是**跨客户端、跨供应商的工具复用**——让任意 MCP 客户端都能调用你的工具。而 Flashback 的工具是单一后端服务给自己的 Agent 用的、且必须绑定 JWT 用户上下文与二段式确认，没有第二个消费方，引入 MCP 只会多一层没有消费者的协议边界。多份 2026 年分析也指出瓶颈已从协议连通性转向工具质量、多用户授权与审计可靠性——而这三件正是 C2 要做的事。Spring AI 同理：它的价值在于屏蔽多 provider 差异，而本仓库当前只对接 OpenAI-compatible 一种形状，`AgentModelClient` 已经是很薄的一层；引入框架会改动 `pom.xml`（`AGENTS.md` 约束「不改 package / lockfile 除非明确要求」）并把 C1 已验证的调用链换掉，属于 backend rewrite 的方向。
3. **放弃的代价**：短期看，工具接入代码要自己写、自己测，没有框架的现成 tool callback。长期看，若未来要接第二个 provider 或把工具开放给外部客户端，就得补这一层——但因为 C2 选了原生 FC schema（决策 1），`AgentToolRegistry` 产出的 JSON Schema 与 MCP 的 tool 定义、Spring AI 的 tool metadata 是同构的，那次迁移是**平滑替换**而非重写。这正是决策 1 长期理由的兑现路径。这个方向已记入 `proposal.md` §12，留待独立 change 讨论。

### 决策 12：待确认提议的执行参数存在哪里（实现期新增）

> 本条在闸门 2 实现期产生：规划时决策 6 定了「审计只存摘要」，但落地时才暴露一个规划未回答的问题——
> 执行 `append_record_content` 需要原始 `text`，而摘要不可还原。按 checklist，实现期出现的真实岔路口须补记决策。

1. **面临的选择**：(a) 前端在 confirm 时把参数回传给后端；(b) 存进服务端内存（如按 toolCallId 缓存）；(c) 在 `agent_tool_call` 上加一个**瞬态**列 `pending_args`，只在 `PROPOSED` 期间有值，提议终结时由 SQL 置 NULL。
2. **选了哪个 + 为什么**：(c)。(a) 是最省事的做法，但它把执行入参的可信来源交给了客户端——用户可以在确认时替换 `text` 或 `tagIds`，等于绕过 `AgentToolValidator` 的白名单与边界校验，也让「Agent 提议了什么」与「实际执行了什么」不再一致，审计随之失去意义。这与决策 5 拒绝旁路 SQL 是同一条理由：**校验点必须唯一**。(b) 在单机能跑通，但重启即丢待确认提议，多实例下确认请求可能落到没有缓存的节点上，属于「测试能过、生产会坏」的设计。(c) 用一个用完即清的列同时满足三件事：执行拿得到入参、跨重启与多实例可用、审计表不留日记文本的长期副本。清空动作放在 `updateStatusIfProposed` 的同一条 UPDATE 里，与状态流转原子完成，不存在「已终结但参数还在」的中间态。
3. **放弃的代价**：选 (c) 的代价是日记文本在待确认窗口内确实存在于审计表中——虽是瞬态，但严格说这个窗口内存在第二份副本，比决策 6 的理想状态弱一档。缓解是窗口极短（用户当场决定）且终结即清，并已在 DDL 注释与 spec scenario 中显式声明该边界，而不是悄悄放宽。选 (a) 的代价是白名单形同虚设，属于本 change 最不能犯的错误。选 (b) 的代价是把一个已知的分布式缺陷带进生产。
