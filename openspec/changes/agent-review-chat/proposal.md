# Agent Review Chat（C3 后半刀）

> Change ID：`agent-review-chat`
> Type：**C**
> 阶段：**规划闸（闸门 1）—— 待用户批准**
> 开工锚点：`b76f221`
> 上游方向：`Docs/agent-iteration/roadmap/iteration-blueprint.md` v1.1 §4 C3（已冻结，§7 已登记拆两刀）
> 前置：C1 / C2 / C4 均已归档；**C3a `agent-memory-retrieval` 2026-07-29 归档，delta 已接受进 baseline**
> 闸门 3：**与 C3a 合并进行**（用户 2026-07-29 决定）。C3a 的 T-20~T-23 顺延到本刀

---

## 1. Why Now

C3a 让 Agent 有了记忆，但记忆目前只用在一个地方——用户**正在写**的时候。而蓝图 D8/D10 定的另一半是「友人回看」：记录解锁那一刻，用户面对的是一段一次性生成的结构化摘要，读完就没了。

这半刀要补的是产品闭环里最有分量的一格。`openspec/project.md` 的定位是「让未来的自己重新理解写下这一刻的自己」——**解锁时刻是这句话真正发生的时刻**。今天那里只有静态文本：`record-detail` 的 `isUnlocked` 分支渲染 `detail.beliefThen` 等字段（`confirmed`，见 §2.4）。用户读完自己三个月前的话，没有人可以说一句。

**为什么必须在 C3a 之后。** 回看对话的价值全部来自「Agent 记得你」。如果先做这一刀，得到的是一个只能就当前这一条记录空聊的浮层——那是今天静态摘要的加长版。现在 `MemoryPort` 已经在，签名里就带着 `purpose` 维度（C3a design 决策 8 的预留），回看传 `REVIEW_CHAT` 即可复用，不需要另起检索。

**为什么现在做是安全的，以及一处必须先讲清的不安全。** 时间归属护栏已经在 C3a 落地——回看对话通篇都在谈过去，是全项目最需要这层护栏的场景，它已经就位。但 C3a 归档时跳过了闸门 3，**R8「时间归属阈值 8 未经真实样本校准」是带着未验证状态进 baseline 的**。回看会把这个未验证的阈值放到最高频的使用场景上：写作引导里 memory 是偶发注入，回看里几乎每一轮都在复述过去。所以本刀的合并联调不是「顺便验一下 C3a」，而是**第一次真正给时间归属护栏压力**。这一点在验收标准里单列。

---

## 2. 现状事实（能力五态）

> 事实来源：`service/impl/AgentChatServiceImpl.java`、`agent/**`、`sql/mysql/c1-agent-runtime.sql`、`c3-agent-memory.sql`、`frontend/src/pages/record-detail/index.vue`、`frontend/src/pages/record-editor/components/AgentChatSheet.vue`、`services/agentService.ts`、`openspec/specs/**`

### 2.1 会话模型（C3a 已铺好的地基）

| # | 事实 | 状态 |
|---|---|---|
| V1 | `agent_session.purpose` 列已存在（`VARCHAR(30) NOT NULL DEFAULT 'WRITING_GUIDANCE'`，`c3-agent-memory.sql` 幂等 DDL），`AgentSessionPurpose` 枚举含 `WRITING_GUIDANCE` / `REVIEW_CHAT`。**本刀无需任何 DDL** | `confirmed` |
| V2 | `REVIEW_CHAT` 当前是**纯声明**：全仓库无任何依赖该值的行为分支 | `confirmed` |
| V3 | 存在测试 `AgentMemoryIntegrationTest.shouldNotCreateAnyReviewChatSession` 断言「不存在任何 `purpose='REVIEW_CHAT'` 的会话」。**本刀实现时必须修改该断言**——预期变更非回归，但须显式披露 | `confirmed` |
| V4 | `agent_session` 与 `agent_message` 已支持任意 `record_id`（`record_id` 可空，外键 CASCADE），无状态限制在表层 | `confirmed` |
| V5 | `MemoryPort.retrieve(MemoryQuery)` 签名已含 `purpose`；`MemoryQuery.hasCue()` 保证无线索不查库 | `confirmed` |
| V6 | `AgentTimeAttributionChecker` / `AgentLayeredCorpus` 已就位且与 purpose 无关，可直接复用 | `confirmed` |

### 2.2 阻碍回看的既有实现（本刀要改的точки）

| # | 事实 | 状态 |
|---|---|---|
| V7 | `AgentChatServiceImpl.startOrResume` 调 `requireOwnedRecordIfPresent`，其中硬校验 `record.getStatus() != RecordStatus.DRAFT` 即抛 `BizException`。回看作用于 UNLOCKED，**必然要改这里** | `confirmed` |
| V8 | baseline `agent-runtime` 有条款 `Writing Guidance Must Target Draft Records Only`，且 C3a 新增了 scenario「C3a 范围内的回看对话」（明写后端不提供作用于已解锁记录的对话行为）。**两处都须 MODIFIED** | `confirmed` |
| V9 | `sendMessage` 无条件走 `stageMachine.decide(...)` 推进六阶段并写 `stage` / `stageReaskCount`。回看无阶段机（Q4 定稿），**需要一条不经状态机的轮次路径** | `confirmed` |
| V10 | **`buildToolContext(session)` 只按 `session.getRecordId() == null` 判断是否下发 tools**。回看会话恰好绑定一条记录 → **「回看无工具」不会自动成立，必须按 purpose 显式短路** | `confirmed` |
| V11 | `startOrResume` 用 `selectActiveByUserAndRecord(userId, recordId)` 找既有 ACTIVE 会话，**该查询不含 purpose 条件**。若同一条记录先后有写作会话与回看会话，会互相串（实际上 DRAFT 与 UNLOCKED 互斥使串的概率低，但契约上不该依赖这个巧合） | `confirmed` |
| V12 | `generateReply` 的 system prompt 由 `AgentPromptBuilder.buildSystemPrompt(targetStage, draftExcerpt)` 组装，含「当前引导目标」与「用户已经写下的正文（只读参考）」。回看的角色设定与阶段目标语义都不同 | `confirmed` |
| V13 | `CLOSING` 阶段会触发 `generateMaterial` 产出可回填正文的素材。回看对象是已解锁记录，**不应产出可回填素材**（封存后不可变契约 + 正文只认会话层） | `confirmed` |

### 2.3 前端现状

| # | 事实 | 状态 |
|---|---|---|
| V14 | 回看渲染在 `record-detail/index.vue` 的 `isUnlocked` 分支，静态区块 + `detail.beliefThen` 等字段；无独立回看组件 | `confirmed` |
| V15 | 该页已有一个 bottom-sheet 浮层 `reply-overlay`，由 `showReplySheet` 控制，且 `v-if="isUnlocked && canSubmitReply"`。新浮层须与它**互斥**（Q5 定稿） | `confirmed` |
| V16 | `AgentChatSheet.vue` 的 props/emits 与工具确认深度耦合（`pendingToolCall` 计算属性、`confirmToolCall` emit、素材回填 `useMaterial`/`discardMaterial`）。回看无工具无素材，**直接复用会带进三块死逻辑** | `confirmed` |
| V17 | `agentService.ts` 现有 5 个端点：`POST /api/agent/sessions`、`GET /sessions/{id}`、`POST /sessions/{id}/messages`、`POST /sessions/{id}/finish`、`POST /sessions/{id}/tool-calls/{id}/confirm` | `confirmed` |
| V18 | baseline `miniapp-core` 已有 4 条 Agent UI 条款（被动入口、中断恢复、素材须确认、显式失败态），均以「记录编辑页」为主语。回看 UI 需**新增**条款而非改写 | `confirmed` |

### 2.4 范围外

| # | 事实 | 状态 |
|---|---|---|
| V19 | 决策链路查询端点与可观测面板 | `out_of_scope`（C5） |
| V20 | R2 引导话术与素材合成质量优化 | `out_of_scope`（用户已明确延后到 C1–C5 完工后） |
| V21 | 检索能力升级（权重、分词、向量） | `out_of_scope`（复用 C3a 的 `MemoryPort`，蓝图 D7） |
| V22 | 情绪轨迹可视化、用户画像、标签自动归类 | `out_of_scope`（蓝图 C3 非目标 + `AGENTS.md`） |
| V23 | 修改回应（reply）功能本身、「后来其实」二次反思 | `out_of_scope`（既有 M4 能力，本刀只保证互斥不冲突） |

> **诚实性声明**：V10 是本刀最容易被漏掉的一条——它不是「需要新增判断」，而是「既有判断恰好会给出错误答案」。V3 是唯一一处需要**修改既有测试断言**的地方，属预期变更，但按 `AGENTS.md` 必须披露而非静默改掉。V11 标 `confirmed` 但风险实际较低（DRAFT/UNLOCKED 互斥），仍应在契约层修掉，不依赖巧合。

---

## 3. 核心问题：回看对话与写作引导是同一条链路还是两条

这是本刀唯一的架构级岔路，其余决策都由它派生。

### 3.1 问题形状

两者共享的：会话与消息持久化、归属校验、失败重试语义、provider 调用、来源分层、时间归属护栏、回复长度上限、诊断/代决检查、Memory 检索。

两者不同的：
- **作用对象**：DRAFT vs UNLOCKED（V7）
- **阶段语义**：六阶段引导 vs 无阶段自由多轮（V9、Q4 定稿）
- **工具**：白名单三工具 vs 完全无工具（V10、Q6 定稿）
- **产物**：素材草稿可回填正文 vs 无产物（V13）
- **prompt 角色设定**：帮你写下此刻 vs 陪你回看那时（V12）
- **前端入口**：编辑器浮层 vs 回看页浮层（V15）

共享面明显大于差异面，但差异全部落在「行为分支」而非「数据结构」上。

### 3.2 三个候选（N1，待确认）

| 方向 | 做法 | 代价 |
|---|---|---|
| **A｜同一 Service + purpose 分支** | `AgentChatServiceImpl` 内按 `session.getPurpose()` 分支：开会话校验、阶段推进、工具上下文、素材生成四处各加一个分支 | `AgentChatServiceImpl` 已有 900 行，再加四处分支会让「这个方法在两种模式下分别做什么」变难读；但改动面最小，不动既有编排骨架 |
| **B｜独立 Service，共享底层组件** | 新建 `AgentReviewChatServiceImpl`，复用 mapper / PromptBuilder / 护栏 / MemoryPort，但自己实现开会话与轮次推进 | 两条链路的失败重试、归属校验、护栏接入顺序会出现两份相似代码，**未来任一护栏改动要改两处**——这正是 C4 决策 5 花力气避免的「规则分散」 |
| **C｜抽公共编排基类/模板** | 抽 `AbstractAgentChatService` 承载共享流程，两个子类实现差异 | 需要重构既有 C1/C2/C4 编排代码，**触碰 `AGENTS.md`「不做大规模 backend rewrite」的边界**，且重构风险由本刀承担 |

**我的推荐：A**，但附一条硬约束——**四处分支必须收敛到一个显式的「模式」判定点**，而不是散落四个 `if (purpose == REVIEW_CHAT)`。

理由：B 的代价是护栏分叉，而护栏分叉是本项目吃过教训的方向（C4 决策 5 就是为了把分散在三处的护栏规则收敛到一个声明源）。回看对话同样要过忠实度、诊断、代决、时间归属四层，一旦两条链路各接一遍，下次改护栏就会漏一边。C 的收益在更远的将来（第三种 purpose 出现时），但要用本刀的风险去买，且明确触碰 rewrite 边界。

取舍全文见 `design.md` 决策 1。

### 3.3 「无阶段机」的具体含义（N2，待确认）

Q4 定稿「回看无阶段机，自由多轮 + 轮次上限，stage 固定常量」。落到实现需要回答三个子问题：

1. **`stage` 存什么**：候选 (a) 固定存 `OPENING`；(b) 固定存 `CLOSING`；(c) 新增一个专用常量。`agent_message.stage` 与 `agent_session.stage` 都是 `VARCHAR(30)`，加值不需要 DDL，但 `AgentStage` 枚举加值会影响既有 switch。
2. **怎么结束**：无阶段机就没有 `CLOSING` 收束。候选：轮次达上限即 `ENDED`；用户主动 `finish`；表达结束意图时由 Agent 温和收束但不自动 END。
3. **轮次上限用哪个配置**：复用 `maxTurnsPerSession=8`，还是给回看单列一个（回看是读后闲聊，可能比写作引导更短或更长）。

### 3.4 回看对话读什么（N3，待确认）

写作引导注入的是「当前草稿摘录 + memory 片段」。回看的对象是一条已解锁记录，它有更多可读字段：`content`（正文原文）、`ai_summary`、`belief_then`、`reality_later`、`reply`。

问题在于**注入哪些、以及它们进哪一层来源**：

- 当前记录的 `content` 是用户自己写的，但**是三个月前写的**——按 C3a 的分层语义，它属于「过去的表达」，应进 MEMORY 层，因此 Agent 复述它时须带时间归属。这个结论看起来严格，但恰好符合产品语义：回看对话里 Agent 引用那时的话，本来就该说清「你那时写的」。
- 但这带来一个副作用：回看对话中**几乎每一轮都会触发时间归属判定**，而该阈值未经校准（R8）。误伤会导致 Agent 频繁降级为兜底回复，观感是「突然失忆」。**这是本刀最大的实现风险**，见 §8 与 design 决策 4。

### 3.5 素材与工具的边界（已由 Q6 定稿，此处只交代实现含义）

回看完全无工具，且不产出可回填正文的素材。两条都要在代码层硬保证，不能靠「恰好没配」：
- Runtime 不挂 tools（按 purpose 短路 `buildToolContext`，V10）；
- 若模型仍返回 tool_calls → **fail-closed**：丢弃并留审计，不下发确认条；
- `CLOSING` 触发的 `generateMaterial` 在回看路径不执行（V13）。

---

## 4. Goals

本刀 SHALL 实现：

1. **回看会话**：用户在已解锁记录上主动开启对话，后端以 `purpose=REVIEW_CHAT` 承载，复用既有会话与消息持久化。
2. **无阶段自由多轮**：不经写作引导状态机，有轮次上限与明确结束语义。
3. **回看上下文**：注入当前记录的可读内容与 Memory 检索到的相关历史，均带时间锚点。
4. **完全无工具**：Runtime 不下发 tools；模型仍返回提议时 fail-closed。
5. **不产出可回填素材**：回看对话不生成素材草稿，记录正文与封存后不可变字段完全不受影响。
6. **护栏全量继承**：忠实度、诊断、代决、伪引用、时间归属、长度上限在回看路径全部生效，无一放宽。
7. **前端回看浮层**：`record-detail` 内独立 `ReviewChatSheet`，与回应浮层互斥，气质克制。
8. **R3 补齐**：本刀有 UI 改动，一并补齐 C2 遗留的微信真机工具链路手验。

---

## 5. Non-Goals（本刀明确不做）

- **不改 C3a 的检索实现**：复用 `MemoryPort`，不新增检索类、不调权重、不加索引。
- **不做决策链路查询端点 / 可观测面板**（C5）。
- **不调引导 prompt 提问策略、不改素材合成策略**（R2）。回看的 prompt 是**新增**的角色设定，不是改写既有引导话术。
- **不给回看加任何工具**；不扩 C2 白名单。
- **不产出可回填正文的素材**；不触碰封存后不可变契约（`location` / `attachments` / `cover`）。
- **不放宽任何 C4 / C3a 护栏阈值**：`minCoverage` / `maxUncoveredRun` / `QUOTE_MIN_COVERAGE` / `minMemoryOnlyRunForAttribution` 初值不动。**若合并联调发现时间归属误伤严重，须作为实测校准单独请示，不得在实现期自行调松。**
- **不做大规模 backend rewrite**：不重构既有 C1/C2/C4 编排骨架（这也是否决 §3.2 方向 C 的原因）。
- **不做前端视觉重建**：新浮层沿用 `record-detail` 既有视觉语言与 `reply-overlay` 的交互范式。
- **不改三 Tab、不改用户可见命名**（我的记录 / 时光轴 / 时间回看）。
- **不改回应（reply）与「后来其实」的既有行为**，只保证浮层互斥。
- **不引入新依赖**、不改 `package` / lockfile / `pom.xml`。
- **不做主动推送 / 弹窗**：回看对话同样是被动召唤。
- **不把记录原文或对话原文写入日志 / 审计 / telemetry**。
- **不做 speech-to-text / voice AI / 情绪评分 / 诊断 dashboard**；不动部署 / 监控 / admin / 通知 / 设置页。

---

## 6. 用户故事

**改前**：三个月前封存的记录解锁了。用户点进去，读到自己那时写的话，下面是一段一次性生成的结构化摘要——「你当时以为……」。读完就到底了。他可能想说「原来我那时候这么在意这件事」，但没有对象可说；也可能想问「后来到底怎么样了」，但页面只有一个「留下回应」的输入框，写完就存档。

**改后**：同一条记录解锁后，摘要下方多了一个克制的入口。点开，Agent 先开口：「三个月前你写下这些的时候，一定挺不容易的。现在回过头看，你觉得当时最担心的那件事后来怎么样了？」用户回答，Agent 记得他三月和五月也写过类似的焦虑，于是接着聊——每次提起过去都会说清是哪个时候的事。聊完就结束，什么都不会被写进那条已经封存的记录里。

---

## 7. 场景边界（气质 + 护栏对齐）

| 场景 | 期望行为 |
|---|---|
| 用户在已解锁记录上点开回看对话 | 后端开启 `purpose=REVIEW_CHAT` 会话，Agent 发出第一句 |
| 用户未点入口 | 不开会话、不弹窗、不自动展开（被动召唤延续） |
| 在 DRAFT 记录上尝试开回看对话 | 拒绝——回看的前提是记录已经抵达 |
| 在 SEALED 未解锁记录上尝试开回看对话 | 拒绝——用户自己都还没到能看的时刻 |
| Agent 引用这条记录里那时写的话 | 须带时间归属（「你那时写的」），否则时间归属检查降级 |
| Agent 关联三个月前另一条记录 | 复用 `MemoryPort`；同样须带时间归属 |
| 模型在回看中返回 tool_calls | **fail-closed**：丢弃并留结构化审计，不下发确认条，本轮回复正常返回 |
| 用户在回看中说「帮我把这段改一下」 | Agent 只能说明自己做不到；记录正文逐字不变（封存后不可变） |
| 用户在回看中要求删除这条记录 | 沿用既有代决护栏：只建议，不执行，且不得谎报已执行 |
| 回看对话中用户描述疑似心理困扰 | 沿用诊断护栏：共情不诊断 |
| 回看会话轮次达上限 | 温和收束并 `ENDED`，不无限延长 |
| provider 不可用 / 失败 | 显式 UNAVAILABLE / FAILED，不本地兜底冒充；用户消息保留可重试 |
| Memory 检索无命中 | 只就这条记录聊，不编造历史关联 |
| 回看浮层与回应浮层 | 互斥：一个打开时另一个不可开启 |
| 回看对话结束后 | 记录正文、封存字段、回应状态全部不变；无素材回填入口 |

---

## 8. 待用户在规划闸确认（N1–N6）

| # | 决策项 | 候选 | 我的推荐 |
|---|---|---|---|
| **N1** | 回看与写作引导是同一条链路还是两条（§3.2） | (a) 同一 Service + purpose 分支；(b) 独立 Service 共享组件；(c) 抽公共基类 | **(a)**，附硬约束：四处差异收敛到一个显式模式判定点，不散落 `if`。理由：(b) 会让四层护栏分叉，重演 C4 决策 5 修掉的问题；(c) 要动既有编排骨架，触碰 rewrite 边界 |
| **N2** | 无阶段机的落地细节（§3.3） | `stage` 存什么 / 怎么结束 / 轮次上限配置 | `stage` **新增专用常量 `REVIEW`**（比复用 OPENING/CLOSING 语义清晰，且 `VARCHAR(30)` 无需 DDL）；结束 = 轮次上限或用户 `finish`；**轮次上限单列配置**，默认 6（回看是读后闲聊，比写作引导 8 轮更短） |
| **N3** | 回看注入哪些字段、进哪一层来源（§3.4） | (a) 仅 `content`；(b) `content` + `ai_summary` + `belief_then`；(c) 再加 `reality_later` / `reply` | **(b)，且全部进 MEMORY 层**。理由：这些都是「过去的表达」，进 MEMORY 层才能触发时间归属，符合回看的产品语义。(c) 的 `reality_later` / `reply` 是用户**后来**写的，时间语义更复杂，建议本刀不注入 |
| **N4** | 时间归属误伤的应对（本刀最大风险） | (a) 沿用 C3a 阈值不动，观察为准；(b) 为回看单列一个更宽的阈值；(c) 回看路径关掉时间归属检查 | **(a)**。理由：回看是这层护栏最该生效的场景，(c) 等于在最需要的地方关掉它；(b) 会造出两个阈值、两种行为，且在未校准前就分叉没有依据。若联调实测误伤严重，作为**校准**单独请示 |
| **N5** | 回看是否复用 `POST /api/agent/sessions` 端点 | (a) 复用，请求体加 `purpose`；(b) 新增 `POST /api/agent/review-sessions` | **(a)**。理由：会话生命周期与消息端点完全一致，新增一套端点会让 `getSession` / `sendMessage` / `finish` 也面临要不要复制的问题。`purpose` 缺省为 `WRITING_GUIDANCE`，向后兼容 |
| **N6** | `selectActiveByUserAndRecord` 是否加 purpose 条件（V11） | (a) 加；(b) 不加，依赖 DRAFT/UNLOCKED 互斥 | **(a)**。理由：契约不该依赖状态互斥这个巧合；加一个谓词的成本远低于将来排查「两种会话互相串」 |

### 附带需在规划批准时一并确认的事

- **V3 既有断言需修改**：`AgentMemoryIntegrationTest.shouldNotCreateAnyReviewChatSession` 会失效。属预期变更，实现时会在 AGENT_LOG 显式披露并改为「写作引导会话不产生 REVIEW_CHAT 用途」的等价断言。
- **合并闸门 3 的预算**：C3a 未用的 20 次 + 本刀申请的 15~20 次。是否合并计为 ≤ 35~40 次请确认。
- **R3 手验范围**：本刀补齐 C2 遗留的真机工具链路（T-40~T-42 等价验证），同时手验回看浮层。

---

## 9. 外调预算

| 阶段 | 外调 | 预算 |
|---|---|---|
| 规划闸（本阶段） | 无 | **0** |
| 实现（闸门 2 后） | 默认 `app.ai.provider=mock`，全部走 mock provider + 单元/集成测试 | **0** |
| 合并联调（闸门 3 单独授权后） | 本刀：回看对话真实观感、时间归属在高频复述场景的误伤率、tool_calls fail-closed 是否被触发。**并顺延执行 C3a 的 T-20~T-23** | 本刀 **≤ 20 次**；含 C3a 顺延部分合计 **≤ 40 次**（待 N 附带项确认） |

**本地环境提醒**：
- `backend/sql/mysql/c3-agent-memory.sql` **尚未在本地 MySQL 执行**，真机手验前必须执行（否则 `purpose` 列不存在，回看会话无法落库）
- 本地 `tag` 表为空 → Memory 的标签关联路径零命中；若要观察标签关联须先建标签
- MySQL80 StartType=Manual
- **R6 凭证轮换仍待用户执行**，建议闸门 3 前完成

`git push` / 部署 / 发布：本 change **不申请**。

---

## 10. 提交责任

**用户手动提交**（默认）。除用户当轮明确授权外，Agent 不执行 `git add` / `commit` / `push`。

---

## 11. 验收标准

### 回看会话

1. 已登录用户可在自己的 UNLOCKED 记录上开启 `purpose=REVIEW_CHAT` 会话，会话与消息正常落库。
2. DRAFT 与 SEALED（未解锁）记录上开启回看对话被拒绝，有测试。
3. 跨用户开启 / 读取 / 追加回看会话被拒绝或返回安全的未找到，不泄露存在性。
4. 回看会话不经写作引导状态机；`stage` 恒为约定常量，`stageReaskCount` 不被回看逻辑改写。
5. 轮次达上限后温和收束并 `ENDED`；用户可主动 `finish`；`ENDED` 后追加被拒。
6. 失败重试语义与 C1 一致：provider 失败时用户消息保留、Agent 回复不落库、同轮可重试且不重复计数。
7. 写作引导会话与回看会话在同一条记录上不互相串（按 N6 定稿）。

### 完全无工具（Q6）

8. 回看路径**不下发 tools**，有测试直接断言（不能只靠「恰好没配」）。
9. 模型在回看中返回 tool_calls 时 **fail-closed**：不落待确认提议、不下发确认条、留结构化审计，且本轮回复正常返回。
10. 回看对话结束后目标记录的正文、`location`、`attachments`、`cover` 逐字不变，有测试。
11. 回看路径**不产出素材草稿**，前端无回填入口，有测试。

### 护栏继承（不得放宽）

12. 忠实度、诊断、代决、伪引用、长度上限在回看路径全部生效，各有测试。
13. 时间归属检查在回看路径生效：复述记录内容或历史片段不带时间归属时降级为安全兜底回复。
14. 带时间归属的复述不被误伤（正例测试）。
15. `minCoverage` / `maxUncoveredRun` / `minCheckedLength` / `QUOTE_MIN_COVERAGE` / `minMemoryOnlyRunForAttribution` 默认值未被放宽，可由配置断言。
16. 回看注入的记录内容与 Memory 片段**均进 MEMORY 层**；不存在把它们当作正文合法来源的路径（回看本就不写正文，此项为防御性验收）。

### Memory 复用

17. 回看复用 `MemoryPort`，未新增检索实现，未改 C3a 检索代码。
18. 检索以 `purpose=REVIEW_CHAT` 调用；无命中时只就当前记录对话，不编造关联。
19. 检索失败时对话正常继续，且护栏不因此放宽。

### 前端

20. `record-detail` 的 UNLOCKED 分支新增克制的回看对话入口，被动触发，不弹窗、不自动展开。
21. `ReviewChatSheet` 与 `reply-overlay` 互斥。
22. 浮层不展示工具确认条、不展示素材回填入口（结构上不存在，而非隐藏）。
23. 显式失败态与不可用态以克制方式告知；不伪装成功。
24. 三 Tab 与用户可见命名不变；未做视觉重建。
25. `type-check` + `build:mp-weixin` 通过。

### 隐私与范围

26. 记录原文、对话原文、Memory 片段不进日志 / 审计 / 痕迹；痕迹只含结构化指标。
27. 未做 C5 可观测、未调 R2 引导与素材策略、未改 C3a 检索、未扩工具白名单。
28. 无新增依赖，`pom.xml` / `package` / lockfile 未改。
29. secret 未出现在前端代码或 tracked files。

### 回归

30. 后端既有测试全绿（**472 项基线**，1 项环境门控 skip 除外）。**唯一允许修改的既有断言是 V3 的 `shouldNotCreateAnyReviewChatSession`**，须在 AGENT_LOG 显式披露；其余既有断言零修改，若必须改须请示。
31. `c3-agent-memory.sql` 已在本地 MySQL 执行（真机手验前置）。

### 合并闸门 3（单独授权）

32. 回看对话真实观感记录（气质是否越界、是否话痨）。
33. **时间归属护栏在高频复述场景的误伤率实测**——这是本刀对 R8 的核心贡献。若误伤严重，作为校准单独请示，**不在实现期自行调松阈值**。
34. 观察模型是否在回看中尝试 tool_calls，fail-closed 是否被真实触发。
35. 顺延执行 C3a 的 T-20~T-23，结论补记进 C3a 归档的 closeout（新增条目，不改写历史）。
36. **R3 补齐**：微信真机手验回看浮层 + C2 遗留的工具链路端到端。
37. 拦截方向若仍未复现，**诚实记为未活体验证**，不得写成已验证。

---

## 12. 建议实现顺序

1. 规划闸批准（N1–N6 定稿）。
2. 后端：会话用途分支的**单一模式判定点**（N1 硬约束）+ 开会话校验放宽到 UNLOCKED + `selectActiveByUserAndRecord` 加 purpose。
3. 后端：无阶段轮次路径 + 结束语义 + 回看轮次配置。
4. 后端：`buildToolContext` 按 purpose 短路（V10）+ tool_calls fail-closed + 素材路径短路（V13）。
5. 后端：回看 prompt 组装 + 记录内容与 Memory 片段进 MEMORY 层。
6. 后端：护栏全量接入验证（含时间归属正反例）。
7. 测试：回看端到端（mock provider）+ 状态范围 + 隔离 + 无工具 + 记录不变 + V3 断言改写并披露。
8. 前端：`ReviewChatSheet` + 入口 + 与回应浮层互斥 + `agentService` 类型扩展。
9. spec delta：`agent-runtime`（MODIFIED V8 两处 + 新增回看条款）、`backend-core`、`miniapp-core`（新增回看 UI）、`v2-product-scope`。
10. 回归 472 项 → 执行 `c3-agent-memory.sql` → 闸门 3 授权后合并联调 + R3 手验 → 收口。

---

## 13. 关键风险

| 风险 | 缓解 |
|---|---|
| **时间归属护栏在回看中高频触发，未校准阈值导致频繁误伤**（本刀最大风险） | 失败方向是兜底回复而非放行冒充；正例测试先行；合并联调实测误伤率；**不在实现期自行调松**（N4） |
| **`buildToolContext` 的既有判断给出错误答案**（V10），导致回看误发 tools | 已列为独立 task + 独立验收项 8/9；不靠「恰好没配」 |
| **purpose 分支散落导致护栏漏接一边** | N1 硬约束：收敛到单一模式判定点；护栏接入顺序在两种模式下走同一段代码 |
| **既有断言 V3 需修改**，可能掩盖真实回归 | 只允许改这一条，须在 AGENT_LOG 披露；改为等价的正向断言而非删除 |
| **回看注入内容较长导致 token 与延迟上升** | 记录内容按配置截断；Memory 片段沿用 C3a 的条数与长度上限 |
| **前端两个浮层交互冲突** | 互斥由状态互锁保证 + 手验；不改 `reply-overlay` 既有行为 |
| **`c3-agent-memory.sql` 未执行导致真机失败** | 列为验收项 31 与实现顺序第 10 步的前置 |
| **本地 tag 表为空，Memory 标签路径零命中** | 联调前若要验标签关联须先建标签；否则只验关键词路径并如实记录 |
| **合并闸门 3 预算超支** | 预算上限明确；mock 下先把所有确定性行为验完，真实调用只用于观感与误伤率 |
