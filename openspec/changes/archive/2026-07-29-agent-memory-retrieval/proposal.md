# Agent Memory Retrieval（C3 前半刀）

> Change ID：`agent-memory-retrieval`
> Type：**C**
> 阶段：**规划闸（闸门 1）—— 待用户批准**
> 开工锚点：`9e747fd`
> 上游方向：`Docs/agent-iteration/roadmap/iteration-blueprint.md` v1.1 §4 C3（已冻结，含拆分退路）
> 前置：C1 `agent-runtime-mvp`（2026-07-27 归档）、C2 `agent-tool-calling`、C4 `agent-guardrails-hardening`（2026-07-28 归档），`ACTIVE_TASK=IDLE`
> **拆分声明**：蓝图 C3 `agent-memory-and-review` 依 §4「可选拆分退路」拆为两刀（2026-07-29 用户批准）：
> 本刀 `agent-memory-retrieval`（检索 + 写作引导注入 + MemoryPort + 跨记录关联能力），后一刀 `agent-review-chat`（回看会话 + UI，消费同一 Memory）。蓝图 §7 修订记录随本 change 批准一并更新。

---

## 1. Why Now

C1 建了 Runtime，C2 给了工具，C4 把「会进用户正文的文本」锁成确定性判定。三刀之后 Agent 仍然**每次都像初次见面**：`AgentPromptBuilder` 的上下文只有本会话消息 + 当前草稿摘录 + 工具补充，`AgentSourceCorpus.of()` 只吃 `role=USER` 的本会话消息。用户写过三十条关于同一件事的记录，Agent 一条都不知道。

蓝图给 Flashback 的叙事定位是「AI 能做什么：在克制产品中做共情式 Agent」，其中「有记忆」是与 RAG 项目分工后**留给本项目**的核心卖点（D7：简单检索，不做企业 RAG 中台）。没有 Memory，Agent 就只是一个多轮版的写作提示器。

**为什么先做检索这半刀。** 拆分后的两刀不是平级切法，而是**依赖切法**：友人回看对话的价值恰恰在于「Agent 记得你」，它必须消费 Memory 才有意义。若先做回看会话，会得到一个只能就当前这一条记录空聊的浮层——那正是今天 `record-detail` 静态摘要的加长版。所以 Memory 是回看的地基，不是它的补充。

**为什么现在做是安全的。** C4 proposal §1 曾明确写过一句反向的话：「不能等 C3……C3 会把更多历史文本注入上下文、让 Agent 有更多素材可供整理，在忠实度闸缺位的情况下先做 C3，等于在漏水的管子上加压」。这句话今天已经反过来成立——忠实度闸已落地并归档，管子补好了。但它同时留下了本刀**必须正面回答**的债：C4 的来源集合被显式约束为「只含当前会话的用户消息」，且这条约束已经作为 scenario 进入 baseline spec。Memory 一旦注入，Agent 引用历史原话就会被判无来源。**本刀的核心工作不是「加个检索」，而是「在不削弱 C4 的前提下扩一层来源」。**

---

## 2. 现状事实（能力五态）

> 事实来源：`backend/src/main/java/com/flashback/agent/**`、`service/impl/AgentChatServiceImpl.java`、`service/impl/RecordServiceImpl.java`、`config/AppAgentProperties.java`、`backend/sql/mysql/*.sql`、`frontend/src/**`、`openspec/specs/agent-runtime/spec.md`

### 2.1 上下文组装与注入点

| # | 事实 | 状态 |
|---|---|---|
| M1 | 上下文组装唯一入口是 `AgentPromptBuilder.buildConversationMessages(targetStage, history, draftExcerpt, toolSupplement)`；system 段由 `buildSystemPrompt` 拼 ROLE_SETTING + 护栏文案 + 长度上限 + 阶段目标 + 草稿只读引用 | `confirmed` |
| M2 | C2 已用 `toolSupplement` 打过样——一个 system 段追加位 + 一个重载。**Memory 注入可沿用同一形态，不需要改动既有签名语义** | `confirmed` |
| M3 | `AgentPromptBuilder` 类注释里 C1 自己写着「历史消息按滑动窗口截取，C1 不做摘要压缩（留给 C3 Memory）」——注入点是 C1 就预留的 | `confirmed` |
| M4 | 编排方是 `AgentChatServiceImpl.sendMessage`，历史来自 `agentMessageMapper.selectBySessionId(sessionId)`；`draftExcerptOf(userId, recordId)` 是既有的「按需取记录内容」范例 | `confirmed` |
| M5 | 会话内消息全部落 `agent_message`（`content TEXT`，`role` / `turn_no` / `stage`），`contextMessageWindow=12` 只作用于 prompt，不作用于忠实度语料 | `confirmed` |

### 2.2 C4 忠实度闸与来源集合（本刀的主要约束面）

| # | 事实 | 状态 |
|---|---|---|
| M6 | 来源集合构造点是 `AgentChatServiceImpl.corpusOf(history)` → `AgentSourceCorpus.of(history, ngramSize)`，**只收 `role=USER`**，注释明确写「不含跨记录的历史检索结果——那是 C3 的边界，C4 不越」 | `confirmed` |
| M7 | `AgentSourceCorpus` 是**扁平 n-gram 集合**（`Set<String> ngrams`），**不携带来源出处**。因此现结构无法回答「这个片段来自本会话还是来自三个月前那条记录」 | `confirmed` |
| M8 | `AgentSourceCorpus.ofTexts(List<String>, ngramSize)` 已存在，供「直接指定来源」使用。Memory 片段可由此入语料，但同样丢失出处 | `confirmed` |
| M9 | corpus 的消费方有三处：`AgentFaithfulnessChecker.check`（素材）、`AgentToolValidator.validate`（工具参数 `text` 与 `askText`）、`AgentContentChecker.check` / `checkQuotes`（诊断 / 代决 / 伪引用）。**扩来源会同时影响这三条路径**，不是单点改动 | `confirmed` |
| M10 | baseline `agent-runtime` spec 有**四条**显式把跨记录检索划为「后续 change」的 scenario：C1「范围内的记忆能力」、C2「范围内的记忆能力」、C4「范围内的记忆能力」，以及 C4 忠实度条款下的「来源集合的边界」。后者措辞是 `来源集合 SHALL NOT 包含跨记录的历史检索结果`。**本刀必须以 MODIFIED 修订这四条，不能绕** | `confirmed` |
| M11 | `AgentGuardrailVerdict` 携带的是结构化指标（覆盖率、最长未覆盖片段、长度），痕迹里不含文本内容。扩来源后痕迹格式若要区分来源层级，需新增结构化字段而非文本 | `confirmed` |
| M12 | 忠实度阈值已被真实样本校准过：`minCoverage=0.35`（实测合法整理仅 0.500）、`maxUncoveredRun=12`、`ngramSize=4`、`minCheckedLength=12`；引号片段另有 `QUOTE_MIN_COVERAGE=0.80` 硬编码常量 | `confirmed` |

### 2.3 可检索的数据现状

| # | 事实 | 状态 |
|---|---|---|
| M13 | `record` 表可用于检索的字段：`title VARCHAR(100)`、`content TEXT`、`core_question VARCHAR(255)`、`ai_summary TEXT`、`belief_then TEXT`、`record_type`、`status`、`created_at` / `sealed_at` / `unlocked_at` / `unlock_at` | `confirmed` |
| M14 | 索引全为 B-tree：`idx_record_user_created_id`、`idx_record_user_status_created`、`idx_record_status_unlock_at` 等。**全库无任何 FULLTEXT 索引，无 ngram 分词配置** | `confirmed` |
| M15 | `ai_summary` **不是后端自动为每条记录生成的**——它由 `CreateRecordRequest` / `UpdateRecordRequest` 携带（`@Size(max=2000)`），`RecordServiceImpl` 落库。`AiServiceImpl.summarizeRecord` 产出的 `AiSummaryVO` 是否被写回记录取决于前端是否回传 | `confirmed` |
| M16 | 因此「历史记录普遍带结构化摘要」这一蓝图假设的**实际数据覆盖率未知**，须实测 | `unknown` |
| M17 | 标签是**全局共享表** `tag`（`uk_tag_name_type`，含 `status=ENABLED`）+ 关联表 `record_tag`，不是用户私有词表。标签检索天然跨用户共享词表，但 `record_tag → record.user_id` 保证结果隔离 | `confirmed` |
| M18 | 本机 MySQL80 当前 **Stopped**（StartType=Manual，`ACTIVE_TASK` Residual 已记）。覆盖率统计需先手动启动服务，本规划阶段**未执行**，不得据此写权重 | `confirmed` |

### 2.4 前端现状（本刀预期零改动）

| # | 事实 | 状态 |
|---|---|---|
| M19 | Agent 对话 UI 是 `frontend/src/pages/record-editor/components/AgentChatSheet.vue`（编辑器内浮层），走 `services/agentService.ts` 的 5 个端点，状态在 `stores/agentChat.ts` | `confirmed` |
| M20 | 时间回看渲染在 `frontend/src/pages/record-detail/index.vue` 的 `isUnlocked` 分支（静态区块 + `detail.beliefThen` 等字段），无独立回看组件；该页已有一个回应浮层 `reply-overlay` | `confirmed` |
| M21 | Memory 注入是纯后端上下文行为，`AgentSession` VO 无需新增用户可见字段 → **本刀前端零改动**。回看浮层 `ReviewChatSheet` 属后一刀 | `planned` |

### 2.5 范围外

| # | 事实 | 状态 |
|---|---|---|
| M22 | 友人回看多轮对话、`purpose=REVIEW_CHAT` 会话、`ReviewChatSheet` 浮层 | `out_of_scope`（后一刀 `agent-review-chat`） |
| M23 | 决策链路 thought→action→observation 查询端点与面板 | `out_of_scope`（C5） |
| M24 | R2「引导问题突兀 + 素材拼接生硬」的引导策略与素材合成质量优化 | `out_of_scope`（用户已明确延后到 C1–C5 全部完工后统一优化；**本刀与后一刀均不动**） |
| M25 | 向量数据库 / embedding / RAG pipeline / 外部搜索引擎 | `out_of_scope`（蓝图 D7 + 用户 Q3 定稿） |
| M26 | 情绪轨迹可视化、用户画像、标签自动归类 | `out_of_scope`（蓝图 C3 非目标 + `AGENTS.md`） |

> **诚实性声明**：M16 标 `unknown` 而非乐观假设——若本地 `ai_summary` 覆盖率极低，基于它的检索权重就是空转，这会直接改变 T-02 的实现。M18 说明该数字**本规划阶段没有测**，批准后第一件事就是补测（T-01）。M7/M9/M10 是本刀最贵的三条：来源集合的结构、消费方数量、以及已接受 spec 的四处约束，共同决定「扩来源」不是加一个参数。

---

## 3. 核心问题：Memory 进来之后，忠实度闸怎么办

用户已定 **Q2=B（精化）**。本节把这个选择的机制含义写清，细节见 `design.md` §3。

### 3.1 定稿的来源边界

合法来源集合 = **当前会话 USER 全文** ∪ **本轮实际注入的 memory 片段原文**。

三个限定，每个都排除一种滑坡：

1. **「本轮实际注入」而非全库历史**——若把用户全部历史记录都当来源，忠实度闸会退化成「只要用户这辈子说过类似的话就放行」，等于废掉。来源必须与本轮 prompt 里真实出现的片段一一对应。
2. **片段携带 `recordId` + 时间锚点**——这是「防止把三个月前的话当成此刻说的」的物理基础。`AgentSourceCorpus` 当前是扁平集合（M7），必须**分层**。
3. **引用须时间归属**——Agent 复述 memory 原话时必须带时间指示（「三月份你写过……」），不能裸引。这条是本刀新增的一类护栏判定。

**引号严判仍适用**：`checkQuotedFragment` 的 `QUOTE_MIN_COVERAGE=0.80` 对 memory 来源同样生效——引用声称是逐字原话，扩了来源不等于放松严判。

### 3.2 为什么不选 A / C（记录已否决的理由）

- **A（不扩来源）**：Agent 能读 memory 却不能说出来。它一提三月份那条记录的原话，`AgentContentChecker.checkQuotes` 就判伪引用、`AgentFaithfulnessChecker` 判无来源。结果是投入检索成本却买到一个哑巴——蓝图 §6.4「记忆关联」那一行示例行为在代码上被自家护栏禁止。
- **C（只放摘要不放原文）**：泄露面确实更小，但 `ai_summary` 覆盖率未知（M16），且摘要是 AI 二次产出——把 AI 的旧产出当「用户来源」，等于让 Agent 的历史输出成为新一轮增写的合法依据，这正是 `AgentSourceCorpus` 注释里明确警惕的自我失效路径（M6）。

### 3.3 「时间归属」怎么机械判定（本刀的新难点）

C4 的所有判定都是确定性的、零外调、可单测。时间归属检查必须守住同一标准，不能引入 LLM-as-judge（C4 已否决方向 C）。

问题形态：给定 Agent 回复 T、分层来源（本会话层 S_now / memory 层 S_mem），若 T 中存在**仅被 S_mem 覆盖而不被 S_now 覆盖**的连续片段，则 T 必须包含时间归属表述，否则判违规。

这需要 `AgentCoverageProfile` 从「单一 covered 位图」升级为「按层的 covered 位图」。这是本刀对 C4 既有类改动最深的一处，也是风险最高的一处（既有 397 项测试基线中有大量断言依赖现结构）。候选实现与取舍见 `design.md` 决策 3，判定形态的初值见 §3.4。

---

## 4. Goals

本刀 SHALL 实现：

1. **Memory 检索能力**：给定用户与当前对话线索，按标签 + 时间窗 + 结构化字段命中，返回该用户自己的相关历史记录片段。严格用户隔离，默认**不扫 `record.content`**。
2. **MemoryPort 抽象**：检索以接口形式暴露给 Runtime，实现可替换（今天是 MySQL LIKE，将来可换而不动调用方）。后一刀 `agent-review-chat` 消费同一 Port，不得另起一套。
3. **写作引导中的 Memory 注入**：沿用 `toolSupplement` 形态，在 system 段注入带时间锚点的 memory 片段；片段数量与长度有上限，避免 token 膨胀。
4. **分层来源集合**：`AgentSourceCorpus` 支持「本会话层 / memory 层」分层，`AgentCoverageProfile` 支持按层覆盖，三条消费路径（M9）行为明确。
5. **时间归属护栏**：仅 memory 覆盖的连续片段若无时间归属表述，判违规并按既有降级路径处置。确定性、零外调、可单测。
6. **跨记录关联能力**：Agent 可在对话中关联相关历史记录（蓝图 C3 目标 4）。本刀交付**能力**与注入契约；对话话术不改（R2 边界）。
7. **隐私守护**：memory 片段是**其他记录的日记原文**，属最高敏数据。片段不落库、不入日志、不进审计、不外发到非 provider 目的地；检索痕迹只留结构化指标。

---

## 5. Non-Goals（本刀明确不做）

- **不做回看会话**：不新增 `purpose` 枚举值的 `REVIEW_CHAT` 分支行为、不改 `record-detail`、不建 `ReviewChatSheet`。后一刀做。
  - 注：`agent_session.purpose` 列**是否在本刀就建**属待确认 N4，见 §8。
- **不做决策链路查询端点 / 可观测面板**（C5）。
- **不调引导 prompt、不改素材合成策略**（R2，用户已明确两刀均不动）。Memory 注入是**新增上下文段**，不是重写既有引导话术。
- **不加 `content` FULLTEXT 索引、不引入 ngram 分词配置、不引入外部搜索引擎**（Q3 定稿）。
- **不默认扫 `record.content`**：正文是最高敏字段，且无索引全表扫代价最高。是否提供一个默认关闭的开关属 N3。
- **不引入 embedding / 向量检索 / 任何相似度第三方库**（`AGENTS.md`：不改 package / lockfile）。
- **不放松任何 C4 护栏**：扩来源是**加一层受约束的来源**，不是降阈值。`minCoverage` / `maxUncoveredRun` / `QUOTE_MIN_COVERAGE` 初值不动。
- **不引入 LLM-as-judge**：时间归属判定必须确定性（C4 决策延续）。
- **不扩工具白名单**：本刀不新增任何工具，Memory 是上下文能力而非工具。
- **不改 C1/C2/C4 已接受端点的既有字段语义**；仅可向后兼容新增（是否新增由 N5 决定）。
- **不改 `complete()` 与三个单轮 AI 端点（`/api/ai/**`、`/api/stage-summaries/**`）链路**。
- **不做大规模 backend rewrite**：`agent` 包内增量新增 + `guardrail` 包内结构升级，不重构 `AgentChatServiceImpl` 编排骨架。
- **不做前端改动**（M21）；若 N5 决定下发字段，也只在 `agentService.ts` 类型上追加，不改 UI。
- **不把 memory 片段写进 `agent_message` / `agent_tool_call` / 日志**。
- **不做 speech-to-text / voice AI / 情绪评分 / 诊断 dashboard**；不改三 Tab、不改用户可见命名；不动部署 / 监控 / admin / 通知 / 设置页。

---

## 6. 用户故事

**改前**：用户第 12 次写下关于工作方向的焦虑。Agent 温和地问「今天是什么让你想写下这一刻？」——和第 1 次一模一样的开场。它不知道用户三月份写过同样的困惑，也不知道那时用户说的原因是项目截止日期。用户每次都要从零解释自己是谁。

**改后**：用户提到工作压力，Agent 在这一轮的上下文里拿到了两条相关历史片段（一条 3 月的、一条 5 月的，各带时间锚点）。它说「我记得你三月份也写过类似的感受，那时候你说是因为项目截止日期。这次也是同一件事吗？」——句子里带了时间归属，所以时间归属检查放行；引用的原话在 memory 层有来源，所以忠实度闸放行。如果模型偷懒说成「你说过这是因为项目截止日期」（没有时间归属，读起来像用户此刻说的），后端检出「仅 memory 覆盖且无时间锚点」，该轮降级为安全兜底回复，用户不会读到一句把三个月前的话冒充成此刻的表述。

---

## 7. 场景边界（Agent 气质 + 护栏对齐）

| 场景 | 期望行为 |
|---|---|
| Agent 引用 memory 原话且带时间归属（「三月份你写过……」） | 忠实度闸放行（memory 层有来源）；时间归属检查放行 |
| Agent 引用 memory 原话但不带时间归属 | 时间归属检查命中 → 该轮降级为安全兜底回复，记结构化痕迹 |
| Agent 引号包裹一段声称来自用户的话，但在两层来源里都无对应 | 沿用 C4 `checkQuotes` + `QUOTE_MIN_COVERAGE=0.80` 严判 → 拒绝 / 降级 |
| 模型把 memory 片段的内容整理进 `append_record_content.text` | **判 UNFAITHFUL 并拒绝**——正文只能追加用户在**本次**表达的内容。memory 是理解材料，不是正文素材。见 design 决策 4 |
| 检索无命中 | 不注入 memory 段，Agent 行为退回 C1/C2/C4 现状。**不得为了「显得有记忆」而编造关联** |
| 检索抛异常 / 超时 | fail-open **仅对能力**（不注入，对话正常继续），fail-closed **对护栏**（未注入即无 memory 层来源，判定照旧严格）。见 design 决策 6 |
| 用户 A 的对话检索命中用户 B 的记录 | 不可能发生——检索 SQL 强制 `user_id` 谓词，且以测试固定。任何跨用户命中视为严重缺陷 |
| memory 片段来自已封存（SEALED）但未解锁的记录 | 属待确认 N2。默认建议**排除未解锁记录**——用户自己都还没到回看时刻，Agent 提前复述等于破坏封存的产品语义 |
| memory 片段过多导致上下文膨胀 | 片段条数与单条字符数双上限，走 `app.agent.memory` 配置 |
| Agent 主动关联历史但用户没兴趣 | 沿用 C1 气质：用户不接话则不追问同一线索（阶段机既有回避判定不变，本刀不改状态机） |

---

## 8. 本轮新增待确认（N1–N5）

> Q1–Q7 已于 2026-07-29 定稿，不再重复。以下是定稿后在代码层浮出的新决策点，**闸门 1 批准时一并确认**。

| # | 决策项 | 候选 | 我的推荐 |
|---|---|---|---|
| **N1** | 分层来源集合的实现形态 | (a) `AgentSourceCorpus` 内部改为「层 → n-gram 集合」映射，对外保持现有方法 + 新增按层查询；(b) 新建 `AgentLayeredCorpus` 包装两个 `AgentSourceCorpus`；(c) 给 n-gram 打层标记的扁平集合 | **(b)**。理由：既有 397 项测试大量直接构造 `AgentSourceCorpus`，(a) 改内部易牵连断言；(b) 是纯增量，旧路径零风险，代价是多一个类。见 design 决策 3 |
| **N2** | memory 检索的记录状态范围 | (a) 全部状态；(b) 排除 SEALED 未解锁；(c) 仅 UNLOCKED + DRAFT | **(b)**。理由：封存的产品语义是「把回答权交给时间」，Agent 抢在解锁前复述内容等于替时间拆封。DRAFT 自己写的可用 |
| **N3** | 是否提供「扫 `content`」的配置开关 | (a) 完全不做；(b) 做一个默认 `false` 的开关；(c) 默认扫 | **(a)**。理由：留一个默认关闭的开关，等于把「无索引全表扫高敏正文」这条路径写进代码却不测试它。要做就等独立 change 正经做（含索引方案） |
| **N4** | `agent_session.purpose` 列在哪一刀建 | (a) 本刀就建（默认 `WRITING_GUIDANCE`），后一刀只加分支；(b) 留给后一刀 | **(a)**。理由：一次 DDL 胜过两次；且本刀 MemoryPort 的调用方参数里天然要区分用途。但本刀**不实现** `REVIEW_CHAT` 的任何行为 |
| **N5** | 是否向前端下发「本轮用了 memory」的标识 | (a) 不下发；(b) 下发布尔标识；(c) 下发引用的 recordId 列表 | **(a)**。理由：蓝图 §6.2「不会拿这些来分析你」，把「我调用了你的历史」显式化会破坏朋友感；调试需求属 C5 可观测范围。(c) 尤其不做——等于在 UI 上暴露检索命中 |

### 附带需在实现期第一步核对的事实

- **`ai_summary` / 标签覆盖率实测（T-01）**：M16/M18 未知。本机 MySQL80 当前 Stopped，需手动启动后统计。若 `ai_summary` 覆盖率过低，检索权重须以标签 + `core_question` + `title` 为主，`ai_summary` 降为加分项——**这会改变 T-02 的实现**，故列为第一个 task。
- **R7（C4 遗留）**：忠实度闸的拦截方向仍未活体验证。本刀闸门 3 会再次真实联调，可顺带观察，但不为它单开验收项。
- **R3（C2 遗留）**：微信真机工具链路手验未走通。本刀前端零改动，**不承接** R3；建议留到后一刀（有 UI 改动时一并手验）。

---

## 9. 外调预算

| 阶段 | 外调 | 预算 |
|---|---|---|
| 规划闸（本阶段） | 无 | **0**（仅读本仓库代码与既有归档） |
| 实现（闸门 2 后） | 默认 `app.ai.provider=mock`；检索与全部护栏判定零外调，单元 / 集成测试覆盖 | **0** |
| 真实联调（闸门 3 单独授权后） | 真实 provider 观察：① Agent 拿到 memory 后是否真的会带时间归属；② 时间归属检查是否误伤自然表述；③ memory 是否被错误整理进正文素材 | 上限 **≤ 20 次请求**，单次超时 ≤ 10s。仅用测试账号自造内容，**不使用用户真实日记** |

后一刀 `agent-review-chat` 另行申请 15–20 次（用户已预告，本 change 不占用）。

`git push` / 部署 / 发布：本 change **不申请**。

**本地环境提醒**：MySQL80 StartType=Manual，当前 Stopped，T-01 与联调前需手动启动。secret 走 `backend/secrets.local.env`（gitignore）+ `Get-LocalSecret`。**R6 的 5 项凭证轮换仍待用户执行**，建议闸门 3 前完成。

---

## 10. 提交责任

**用户手动提交**（2026-07-29 用户定稿）。Agent **不执行** `git add` / `commit` / `push`。

---

## 11. 验收标准

### Memory 检索

1. 存在 `MemoryPort` 抽象与 MySQL 实现；调用方只依赖接口，替换实现不改调用方。
2. 检索命中基于标签 + 时间窗 + `title` / `core_question` / `ai_summary` / `belief_then` 的 LIKE 匹配；**SQL 中不出现 `content` 的匹配谓词**（可由测试断言 SQL 或断言 Mapper 行为）。
3. 未新增 FULLTEXT 索引、未引入 ngram 配置、未引入外部检索依赖。
4. 检索 SQL 强制 `user_id` 谓词；存在跨用户隔离测试（构造用户 B 的高度相关记录，断言用户 A 检索零命中）。
5. 按 N2 定稿排除相应记录状态，有对应测试。
6. 检索无命中时不注入 memory 段，对话行为与 C4 现状一致。
7. 检索异常 / 超时时对话正常继续且未注入 memory 段，有测试。

### 注入与来源分层

8. Memory 注入走 system 段追加位，既有 `buildConversationMessages` 调用方语义未被破坏。
9. 注入片段数量与单条长度受 `app.agent.memory` 配置约束，无新增凭证字段。
10. 每个注入片段携带 `recordId` 与时间锚点，且时间锚点在 prompt 文本中可读。
11. 来源集合分层生效：本会话层与 memory 层可分别查询覆盖情况；**只在 memory 层有来源的片段可被识别出来**（这是时间归属检查的前提，须有直接单测）。
12. 来源集合只含**本轮实际注入**的 memory 片段，不含未注入的检索结果、不含全库历史（须有测试证明未注入的记录内容不构成合法来源）。

### 护栏（不得削弱 C4）

13. 时间归属检查存在：仅 memory 覆盖的连续片段若无时间归属表述则判违规，走既有降级路径并留结构化痕迹。
14. 带时间归属的 memory 引用不被误伤（正例测试）。
15. `minCoverage` / `maxUncoveredRun` / `minCheckedLength` / `QUOTE_MIN_COVERAGE` 初值未被放宽（可由配置默认值断言）。
16. **memory 内容不得成为正文素材**：模型把 memory 片段整理进 `append_record_content.text` 或素材草稿时判 UNFAITHFUL 并拒绝，有测试（design 决策 4）。
17. 伪引用严判对两层来源均生效。
18. 判定异常时 fail-closed，不放行未检文本。
19. 全部新增判定确定性、零外调、可在无真实 provider 下运行。

### 隐私

20. memory 片段不出现在 `agent_message` / `agent_tool_call` / 应用日志 / 审计痕迹中；检索与判定痕迹只含结构化指标（命中条数、片段长度、覆盖率、层级、违规类型）。
21. 来源集合不落库、不写日志、不外发（沿用 C4 `AgentSourceCorpus` 隐私约束）。
22. secret 未出现在前端代码或 tracked files。

### 范围与回归

23. 未做回看会话 / 回看 UI（后一刀）、未做查询端点或面板（C5）、未调引导 prompt 与素材合成策略（R2）。
24. 工具白名单未扩大；C1/C2/C4 端点既有字段语义未变；`complete()` 与三个单轮 AI 端点链路未改。
25. 无新增第三方依赖，`package` / lockfile 未改。
26. 后端既有测试全绿（**397 项基线不回归**，1 项环境门控 skip 除外）；未为本刀便利修改任何既有断言（若必须改，须在 AGENT_LOG 显式披露并请示）。
27. 前端若零改动则显式说明；若因 N5 改动则 `type-check` + `build:mp-weixin` 通过。
28. spec delta 以 MODIFIED 修订 M10 的四条 scenario，未偷偷放宽。

### 闸门 3（真实联调，单独授权）

29. 真实 provider 下观察 Agent 拿到 memory 后的表述形态，记录是否自发带时间归属。
30. 时间归属检查的误伤方向有真实样本证据；拦截方向若未复现，须**诚实记为未活体验证**（不得写成已验证）。
31. 观察 memory 是否被错误整理进正文素材，若复现则作为回归样本入测试集。

---

## 12. 建议实现顺序

1. 规划闸批准（N1–N5 定稿）。
2. **T-01 覆盖率实测**（启 MySQL，统计 `ai_summary` / 标签 / `core_question` 非空率）——结论直接决定检索权重，必须先做。
3. 来源分层：`AgentLayeredCorpus` + `AgentCoverageProfile` 按层覆盖，纯离线可验证，先于检索落地。
4. 时间归属检查 + 降级接入，配正反例测试。
5. `MemoryPort` 抽象 + MySQL 实现 + 隔离 / 状态范围 / 异常测试。
6. 注入接入 `AgentPromptBuilder` + `AgentChatServiceImpl`，`app.agent.memory` 配置。
7. 三条 corpus 消费路径（M9）行为对齐 + 「memory 不得成为正文素材」测试。
8. 回归 397 项 → 闸门 3 授权后真实联调 → 输出 Required Output，更新 `ACTIVE_TASK` / `AGENT_LOG`。

---

## 13. 关键风险

| 风险 | 缓解 |
|---|---|
| **改动 C4 既有 corpus 结构牵连 397 项基线** | N1 选 (b) 纯增量包装；既有 `AgentSourceCorpus` 语义不动；改断言即披露 |
| **时间归属检查误伤自然表述**（中文时间指示语形态多） | 失败方向选「降级为兜底回复」而非「放行冒充」；判定阈值可配；闸门 3 用真实样本校准；正例测试先行 |
| **`ai_summary` 覆盖率过低导致检索空转** | T-01 先测；覆盖率低则以标签 + `core_question` + `title` 为主权重 |
| **无索引 LIKE 检索性能** | 个人级数据量；限定 `user_id` + 时间窗 + `status` 后走既有复合索引缩小扫描面；不扫 `content`；结果条数上限 |
| **memory 注入后 token 与延迟上升** | 片段条数 + 单条长度双上限；注入内容为片段而非整条正文 |
| **memory 原文被当成正文素材写进日记** | 验收 16 单列；正文只认本会话来源（design 决策 4） |
| **memory 片段泄露到日志 / 审计** | 沿用 C4「只落结构化指标」硬约束；验收 20/21 覆盖 |
| **跨用户泄露** | SQL 强制 `user_id`；隔离测试固定；视为严重缺陷等级 |
| **拆两刀后 Memory 契约与后一刀不匹配** | MemoryPort 设计时即预留 `purpose` 维度（N4）；后一刀不得另起检索实现 |
| **范围滑向 R2** | Non-Goals 显式禁止；Memory 只新增上下文段，不改既有引导话术；tasks 逐项范围自检 |
