# Agent Runtime MVP · Design（C1）

> 状态：**规划闸（闸门 1）待批准**。本文不授权写业务代码。
> 上游：`proposal.md`；方向：蓝图 v1.1 §4 C1、§6 Agent 气质。

---

## 1. 架构

新增一个后端模块，不动现有 AI 三个方法：

```
frontend
  pages/record-editor/index.vue
    └─ components/AgentChatSheet.vue        半屏浮层（被动触发）
  stores/agentChat.ts                       会话状态 / 轮次 / loading / 失败态
  services/agentService.ts                  /api/agent/** 调用（沿用 httpClient + preview 拒绝）

backend  com.flashback
  controller/api/AgentController            /api/agent/**（JWT，/api/** 链路内）
  service/AgentChatService(+Impl)           会话编排：取会话 → 追加用户消息 → 组装上下文 → 调 provider → 落 Agent 回复
  agent/AgentStageMachine                   阶段推进纯逻辑（无 IO，可单测）
  agent/AgentPromptBuilder                  system prompt + 最小护栏 + 阶段提示
  agent/AgentGuardrailPolicy                C1 仅承载常量与长度上限（C4 在此扩展）
  service/impl/AiServiceImpl                复用 invokeChatCompletion 形状（不改现有三方法行为）
  domain + mapper                           AgentSession / AgentMessage
```

**分层原则**：状态机是纯函数式推进（输入当前阶段 + 用户输入特征 → 下一阶段），不碰 HTTP、不碰 DB；编排层负责 IO。这样阶段规则可以在没有 provider 的情况下被单测覆盖。

---

## 2. 数据流（一次用户发言）

```
用户在浮层输入
  → POST /api/agent/sessions/{id}/messages
  → 鉴权 + 归属校验（session.user_id == 当前 userId）
  → 校验：会话未结束 / 未超轮次上限 / 内容长度合法
  → 落 user 消息（turn_no+1）
  → AgentStageMachine 计算目标阶段
  → AgentPromptBuilder 组装 messages[]：
        system（角色 + 5 条最小护栏 + 长度上限 + 当前阶段目标）
      + 会话近 N 轮消息（滑动窗口，超窗只保留最近轮次；C1 不做摘要压缩）
      + 可选草稿正文片段（用户已写的正文，只读引用，不改写）
  → provider 调用（复用 OpenAI-compatible /chat/completions 形状）
  → 解析 → 长度裁剪 → 落 assistant 消息
  → 返回 { sessionId, stage, turnNo, reply, status, canFinish }
```

失败分支：provider 未配置 → `UNAVAILABLE`；调用/解析失败 → `FAILED`。两种情况下 **user 消息已落库**（用户的话不能丢），assistant 消息不落库，前端可对同一轮重试。

---

## 3. 状态机

阶段：`OPENING → EMOTION → CONFUSION → CORE_QUESTION → EXPECTATION → CLOSING → ENDED`

推进规则（C1 保持可解释、不依赖模型自判）：
- 每阶段默认 1 轮，用户回答过短（判定为「回避」）时同阶段最多再追问 1 次，随后前进，避免逼问。
- 用户显式表达结束意图 → 直接跳 `CLOSING`。
- 达到会话轮次上限 → 强制 `CLOSING`。
- `CLOSING` 产出素材草稿（由用户已说内容整理），进入 `ENDED`。
- `ENDED` 会话只读，不可再追加消息；用户可开新会话。

一个草稿记录同时最多一个 `ACTIVE` 会话；再次点击入口时恢复该会话（中断恢复）。

---

## 4. API 契约草案（规划稿，实现前以本节为准）

统一走既有 `ApiResponse` 包装与 `ErrorCode`（`40000/40100/40300/40400/50000`），不新增错误码体系。

| 方法 | 路径 | 用途 |
|---|---|---|
| POST | `/api/agent/sessions` | 开启或恢复会话（`recordId` 可选，指向草稿记录） |
| GET | `/api/agent/sessions/{sessionId}` | 拉取会话与消息（中断恢复） |
| POST | `/api/agent/sessions/{sessionId}/messages` | 发送一轮用户消息，返回 Agent 回复 |
| POST | `/api/agent/sessions/{sessionId}/finish` | 用户主动结束，返回素材草稿 |

`status` 沿用现有 AI 语义 `SUCCESS | UNAVAILABLE | FAILED`（C1 不引入 `FALLBACK`，理由见决策 5）。

素材回填由前端把 `materialDraft` 交给用户确认，确认后走**已有**记录更新接口写入正文；Agent 侧不直接写记录。

---

## 5. 持久化草案

```sql
agent_session(
  id, user_id, record_id NULL, stage, status,           -- status: ACTIVE|ENDED
  turn_count, last_active_at, created_at, updated_at,
  KEY (user_id, status), KEY (record_id),
  FK user_id -> user(id) ON DELETE CASCADE,
  FK record_id -> record(id) ON DELETE CASCADE
)

agent_message(
  id, session_id, user_id, role,                        -- role: USER|ASSISTANT
  turn_no, stage, content, created_at,
  UNIQUE (session_id, turn_no, role),
  KEY (session_id, id),
  FK session_id -> agent_session(id) ON DELETE CASCADE,
  FK user_id -> user(id) ON DELETE CASCADE
)
```

命名、`user_id` 归属列、FK CASCADE、`Asia/Shanghai` 时间语义与现有 `record_*` 表一致。DDL 落 `backend/sql/mysql/`，与 M3/M4 一样提供增量脚本。

---

## 6. 最小护栏（C1 单层）

写入 system prompt 的 5 条：

1. **不诊断**：不使用心理/医学诊断词，不做病症判断或医学建议。
2. **不覆写**：不改写、不替换、不"修正"用户原文；引用只能原样引用。
3. **建议不代决**：封存/解锁/删除只能建议，由用户在编辑页确认。
4. **被动召唤**：不主动开启新话题分析用户，不催促。
5. **输出克制**：单条回复不超过设定字数上限；不长于用户表达。

代码侧仅做一件硬约束：**回复长度上限裁剪**（可测）。语义类护栏在 C1 只靠 prompt，其防御深度不足是**已接受的风险**，C4 补后置检查与违规降级。

---

## 7. 隐私

- 对话原文与日记原文按业务数据对待，只进 `agent_message` / `record`，**不进** 应用日志、telemetry、tracked files。
- 日志只记结构化元数据：`sessionId` / `stage` / `turnNo` / `provider` / `durationMs` / 异常类名（沿用 `AiServiceImpl` 现有 warn 日志风格）。
- provider secret 仍只从 backend config 注入。
- 会话严格 `user_id` 隔离；跨用户访问返回安全的未找到或拒绝，不泄露存在性。

---

## 8. 验证策略

| 层 | 手段 | 授权 |
|---|---|---|
| 状态机 | 单元测试：阶段推进、短回答追问上限、结束意图、轮次上限强制收束 | 闸门 2 |
| 编排/护栏 | 单元测试：长度裁剪、prompt 含 5 条护栏、失败时 user 消息保留而 assistant 不落库 | 闸门 2 |
| API | 集成测试（mock provider）：多轮、恢复、结束、未登录 401、跨用户拒绝、`ENDED` 拒绝追加 | 闸门 2 |
| 前端 | type-check + Mini Program 构建 | 闸门 2 |
| 真实 provider | 多轮联调，观察结构化输出稳定性（F13 已知不稳） | **闸门 3** |
| 微信手验 | 开启 → 多轮 → 中断 → 恢复 → 回填/拒绝回填；护栏三项 | 闸门 2（手验）/ 闸门 3（真实 provider） |

---

## 决策记录

### 决策 1：对话状态持久化落在哪里（蓝图 P2）

1. **面临的选择**：① MySQL 新表 `agent_session`/`agent_message`；② Redis session（`spring-boot-starter-data-redis` 已在 `pom.xml`、dev/prod 已配 host/port）；③ 纯内存 Map。
2. **选了哪个 + 为什么**：**MySQL 新表**。三个理由：会话消息在 C1 就是产品数据（中断恢复、素材保留都依赖它），不是缓存；MySQL 是本仓库唯一被真实验证过的业务持久化，而 Redis 虽已声明依赖却**零处使用、连通性从未被业务验证**（F8），C1 不该同时赌两个新东西；C3 的 Memory 检索、C5 的链路追溯都更可能建立在可查询的关系表上。
3. **放弃的代价**：选 Redis 会把「验证一条从未跑过的基础设施」塞进 C1，失败时无法区分是 Runtime bug 还是连接问题，且过期语义会让「几天后回来接着聊」变成隐式数据丢失；选纯内存则后端重启即丢会话，与「可中断、可恢复」目标直接冲突，也无法在多实例下工作。写表的代价是多一次 schema 变更与写放大，可接受。

### 决策 2：对话消息是否落原文

1. **面临的选择**：① 落完整原文；② 只落 Agent 侧摘要；③ 只落最近 N 轮、更早的丢弃。
2. **选了哪个 + 为什么**：**落完整原文，按高敏业务数据对待**。素材保留要求"用户自己说过的话"能被原样整理，摘要会丢真、也会引入模型二次改写风险（与「不覆写」护栏冲突）。安全边界靠约束落点：只进业务表，不进日志/telemetry/tracked files，不外发。
3. **放弃的代价**：只存摘要会让中断恢复后的对话上下文失真，用户看到的不是自己写的话；只留最近 N 轮会让长对话的收束素材缺前半段。落原文的代价是敏感数据面扩大一张表，用 §7 的落点约束与 `user_id` 隔离对冲。

### 决策 3：Runtime 是新模块还是改造 `AiServiceImpl`

1. **面临的选择**：① 新增 `agent` 模块 + 新端点，复用底层 HTTP 调用形状；② 把 `AiServiceImpl` 扩成多轮通用引擎，现有三个方法作为特例；③ 直接在 `AiController` 上加多轮参数。
2. **选了哪个 + 为什么**：**新增 `agent` 模块**。现有三个 AI 方法是 M4 已接受的 baseline 契约且前端在用，把它们改成通用引擎的特例等于在没有验收压力的地方制造回归风险，也逼近 AGENTS 禁止的「大规模 backend rewrite」。新模块可独立测试、独立回滚，C2 的 Tool 层也更容易挂在同一模块。
3. **放弃的代价**：会有少量重复（provider 调用与错误映射的相似逻辑）。若选方案 ②，风险是 M4 已验收的写作提示/整理行为被牵连改变；若选方案 ③，`/api/ai/**` 会同时承载单轮与多轮两种语义，契约将变得难以解释。重复代码的代价用「复用 `invokeChatCompletion` 形状而非复制其配置读取」来压到最小。

### 决策 4：状态机用显式阶段推进，而不是让模型自己决定进度

1. **面临的选择**：① 后端显式阶段机（EMOTION→CONFUSION→…）；② 单个长 system prompt 让模型自行掌握引导节奏；③ 让模型每轮返回结构化 `nextStage` 由后端采纳。
2. **选了哪个 + 为什么**：**后端显式阶段机**。F13 已表明该 provider 的结构化输出不是 100% 稳定，把流程控制权交给模型意味着"引导节奏"不可测；显式阶段机让「追问上限」「强制收束」「不逼问」这些气质要求变成可单测的规则，也让 C5 的链路追溯有确定的 state 可记录。
3. **放弃的代价**：显式阶段机的对话会比模型自由发挥略显规整，灵活度低。选 ② 会让"连续追问导致用户被逼问"这类气质越界无法在测试里复现；选 ③ 则把稳定性风险放在关键路径上，模型一次格式错误就会让阶段错乱，还得再写一套兜底——复杂度反而更高。

### 决策 5：C1 不引入 `FALLBACK` 状态

1. **面临的选择**：① 只用 `SUCCESS | UNAVAILABLE | FAILED`；② 沿用现有四态，provider 失败时给一条本地兜底回复（类似 `fallback.writing-prompts`）。
2. **选了哪个 + 为什么**：**只用三态**。单轮提示词的本地兜底是无害的静态文案，但多轮对话里的"本地兜底回复"会让用户以为 Agent 在回应他——这正是 AGENTS 禁止的「mock success 冒充真实成功」的软性版本。失败就说失败，允许重试，已说的话不丢。
3. **放弃的代价**：provider 不稳时用户会更频繁看到"暂时聊不了"，体验不如有兜底顺滑。但把假回应写进对话历史会污染素材整理的输入，代价更大。

### 决策 6：为什么 C1 不做 Tool Calling（out_of_scope 边界）

1. **面临的选择**：① C1 只做对话，工具留 C2；② C1 顺手接一个最小工具（如自动保存草稿）；③ C1 先把 FC 协议层搭好但不开放工具。
2. **选了哪个 + 为什么**：**只做对话**。蓝图已把 C2 独立成 change，且 FC 在本仓库属未验证能力（F10）；一旦 C1 同时验证"多轮 Runtime"和"provider FC 行为"，出问题时无法定位。此外「自动保存草稿」触及记录写路径，会与「建议不代决」和封存不变性产生边界，必须有独立的白名单与确认设计。
3. **放弃的代价**：C1 的 Agent 只能说不能做，用户要手动落地建议，体验不完整。若选 ②，最小工具会以隐式方式定义 Tool 权限模型，C2 再改就是契约变更；若选 ③，未使用的协议层是无验证的死代码，还会诱导后续绕过 C2 直接开工具。

### 决策 7：为什么 C1 不做后置输出过滤（护栏深度边界）

1. **面临的选择**：① C1 仅 system prompt + 长度硬裁剪；② C1 就加诊断关键词后置拦截；③ C1 完全不做护栏，全部留 C4。
2. **选了哪个 + 为什么**：**prompt + 长度裁剪**。蓝图 D19 明确 C1 必须内嵌最小护栏、C4 才做系统化 hardening；长度裁剪是唯一确定性强、不会误伤语义的机械约束，先做它成本极低。关键词拦截需要配套的兜底回复策略与边界用例集，属 C4 的完整工作，半做会留下一套没有回归测试保护的规则。
3. **放弃的代价**：C1 上线后仍可能在边界输入下滑向诊断式表达，且无法自动发现。这是**已接受风险**，靠手验三项与 C4 补齐。若选 ③ 则违反 D19，Agent 首版就可能明显越界；若选 ②，会在没有测试集的前提下堆规则，C4 大概率要推翻重写。

### 决策 8：对话 UI 是浮层还是独立页面

1. **面临的选择**：① 记录编辑页内半屏浮层；② 独立页面 `pages/agent-chat/`；③ 编辑器页内内联区块。
2. **选了哪个 + 为什么**：**半屏浮层**。用户写作时的上下文（已写的正文）需要保持在视野与内存里，浮层可随时收起且不新增路由；也天然满足「被动召唤、可随时中断」。三 Tab 结构与编辑器主路径视觉均不受影响。
3. **放弃的代价**：浮层可用高度小，长对话滚动体验不如整页。选 ② 会引入返回栈与草稿态跨页同步问题，且更像"一个新功能页"而非"随手找朋友聊两句"，与气质不合；选 ③ 会挤压编辑器主路径布局，逼近禁止的视觉重构。

### 决策 9：spec delta 落在新 capability 还是塞进 backend-core

1. **面临的选择**：① 新建 `openspec/specs/agent-runtime/` 承载主契约，另在 `backend-core`/`miniapp-core`/`v2-product-scope` 各加最小 delta；② 全部塞进 `backend-core`；③ 只写 `agent-runtime`，不动三份 baseline。
2. **选了哪个 + 为什么**：**方案 ①**。蓝图 §5 已建议新建 `agent-runtime`；Agent 契约会在 C2–C5 持续增长，独立 capability 便于演进。但端点鉴权/隐私属后端通用约束、对话入口属小程序契约、被动召唤与气质属产品范围，这三处必须在原 baseline 留可检索的最小条款，否则未来读 `backend-core` 的人看不到 `/api/agent/**` 的存在。
3. **放弃的代价**：分四处写会有少量交叉引用维护成本。选 ② 会让 `backend-core` 混入大量 Agent 细节，C2–C5 继续追加后难以阅读；选 ③ 则三份 baseline 对 Agent 端点完全失明，违背「baseline 是已接受契约总览」的定位。
