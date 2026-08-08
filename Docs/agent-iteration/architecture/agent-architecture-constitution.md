# Flashback Agent 架构宪法（Draft v0.1）

> 文档性质：长期架构约束（Constitution），**不是**可执行 OpenSpec change  
> 状态日期：2026-07-30  
> 状态：**v0.2——已按 Phase 1 全部归档事实校准**（随蓝图 v1.2 冻结同步）  
> 优先级：`AGENTS.md` Non-Negotiable → 已冻结蓝图 / 已接受 specs → 本文 → 旧分析文档  
> 冲突处理：与 `AGENTS.md` 或已接受 OpenSpec 冲突时，**以 AGENTS / OpenSpec 为准**，并回头修订本文

---

## 0. 本文存在的理由

Flashback 的产品 Agent 走的是 **Backend 托管、被动召唤、单 Agent 共情** 路线。  
行业工具链 12 个月一变，但下列东西不应随框架潮流被推翻：

1. **产品语义**（阶段引导、建议不代决、不诊断、日记隐私）  
2. **稳定端口**（模型、工具、记忆、护栏、轨迹、评估）  
3. **可演进实现**（检索引擎、Judge、缓存、trace 存储可替换）

**宪法原则一句话：** 换插件，不换信仰；演进实现，不推倒 Runtime。

---

## 1. 产品级硬约束（不可被「更前沿架构」推翻）

继承 `AGENTS.md` 与蓝图气质，上升为架构红线：

| # | 约束 | 架构含义 |
|---|---|---|
| P1 | 被动召唤 | Runtime 不得有主动推送/弹窗调度器作为主路径 |
| P2 | 单 Agent「一个朋友」 | 不做面向用户的 Multi-Agent 角色扮演编排 |
| P3 | 建议不代决 | 高风险写操作必须人确认；Tool 层强制 |
| P4 | 不覆写用户原文 | 素材仅为候选；忠实度/内容闸可拦截「伪造用户话」 |
| P5 | 不诊断 / 无复杂评分 dashboard | Guardrail + Eval 禁止诊断话术与情绪大盘 |
| P6 | 日记高敏 | Trace/Eval/日志禁止原文落盘；Memory 严格 user 隔离 |
| P7 | secret 仅 backend | 前端与 tracked 文件零凭证 |
| P8 | 不做第二套企业 RAG 中台 | Memory 是「Agent 如何用记忆」，不是通用知识库平台 |
| P9 | 不做大规模 backend rewrite | 增量模块；禁止为追框架整包迁移 |
| P10 | 一次一个 Type C | 架构演进也必须切片，禁止并行改核心语义 |

---

## 2. 目标架构分层（Logical Layers）

逻辑分层与「是否引入某 Java 库」无关。实现可以单仓模块，但**依赖方向必须单向**：

```text
┌─────────────────────────────────────────────────────────────┐
│ L5  Eval & Governance                                       │
│     黄金集 / Judge / 回归门槛 / 护栏规则治理（C4 基线 → C6）   │
├─────────────────────────────────────────────────────────────┤
│ L4  Resilience & Operations                                 │
│     超时·熔断·降级文案·多 provider 路由（C7，可后置）          │
├─────────────────────────────────────────────────────────────┤
│ L3  Intelligence Context                                    │
│     Memory 注入 / 时间感知 / Context 预算（C3 → C8 / C10）    │
├─────────────────────────────────────────────────────────────┤
│ L2  Capabilities                                            │
│     Tool Registry · Allowlist · Confirm · Observation（C2） │
├─────────────────────────────────────────────────────────────┤
│ L1  Agent Loop & Session                                    │
│     Stage machine · turn budget · prompt assembly（C1）     │
├─────────────────────────────────────────────────────────────┤
│ L0  Platform                                                │
│     Spring Boot · MySQL · Provider HTTP · Mini Program UI   │
└─────────────────────────────────────────────────────────────┘
```

**依赖规则：**

- 上层可调用下层端口；下层**不得**依赖上层业务语义（例如 `AgentModelClient` 不知「友人回看」文案）  
- 新能力优先加在正确层，禁止在 L0 写死 L3 规则  
- 跨层传递用 DTO / 领域对象，禁止把 provider 原始 JSON 泄漏到 Controller 之外无边界地乱传

### 2.1 与已落地代码的映射（2026-07-30 基线，Phase 1 收官后）

| 逻辑层 | 现状锚点（可随实现更新） | 状态 |
|---|---|---|
| L1 | `AgentStageMachine`、`AgentPromptBuilder`、`AgentChatServiceImpl`、`agent_session` / `agent_message` 表 | confirmed（C1） |
| L2 | `agent/tool/*`（Registry / Coordinator / Validator / Executor）、`agent_tool_call` 表 | confirmed（C2） |
| L3 | `agent/memory/*`（`MemoryPort` / `MySqlMemoryPort` / `MemoryCueExtractor` / `MemoryQuery` / `MemoryFragment`）；回看会话由 `agent_session.purpose` 区分 | confirmed（C3a + C3b） |
| Guardrail（跨 L1–L5） | `AgentGuardrailPolicy` + `agent/guardrail/*`（含 `AgentTimeAttributionChecker`，C3a 补入） | confirmed（C4 前移 + C3a 增补） |
| Trace（支撑 L4/L5） | `agent/trace/*`（`AgentTraceCollector` / `AgentTraceSink` / `AgentTraceVersions` / `AgentTraceLayer` / `AgentTraceOutcome`）、`agent_turn_trace` 表 | confirmed（C5） |
| L4 Resilience | `agent/resilience/*` 封闭失败分类、request-scope 24000ms provider-work budget、零自动 retry、阶段化失败模板；多 provider / 熔断 / 缓存仍 deferred | confirmed（C8） |
| L5 Eval | `agent/eval/*`（harness / scripted provider 替身 / 用例加载 / 不变量 / 快照 / 基线）、`eval/cases/*.yaml` 23 条用例、`eval/baseline/snapshots.yaml` 23 条基线；既有 `AgentGuardrailBoundaryCaseTest` 原地保留 | confirmed（C6）；**语言质量维度仍 partial**——人评锚点结构就位但为空 |
| L0 | `AgentModelClient`（OpenAI-compatible HTTP）、MyBatis、MySQL、Uniapp | confirmed |

> 校准义务：**每刀归档后**更新本表「现状锚点」，删除过时类名。
> 本表最近一次校准：**2026-08-08（C8 归档，后端 645 tests PASS / 6 skipped）**。
> 注：上一次校准写的「534 tests」是 C5 归档当时的值，其后三个 Type B 使基线成为 536 / 4
> ——C6 实现期复核时发现并修正（摘要类数字要定期复核）。

---

## 3. 六大稳定端口（Stable Ports）

端口是**反淘汰核心**。实现类可换，方法语义应保持可测、可 mock。

### 3.1 `LlmPort`（模型调用）

| 项 | 约定 |
|---|---|
| 职责 | chat / chatWithTools（及未来可选 embed） |
| 现状 | `AgentModelClient` + `AppAiProperties`（MOCK / DEEPSEEK / OPENAI_COMPATIBLE） |
| 必须 | model-agnostic；FC 以 **model 白名单** 门控；不可用时退回纯对话，**禁止**自研假 FC 协议静默顶替 |
| 禁止 | 业务 Service 直接拼第二套 HTTP 客户端；secret 新通道 |

### 3.2 `ToolPort`（工具）

| 项 | 约定 |
|---|---|
| 职责 | 注册、schema 暴露、参数校验、执行、observation 回注 |
| 现状 | `agent/tool/*`（Registry / Coordinator / Validator / Executor / SchemaFactory）；痕迹落 `agent_tool_call` |
| 必须 | allowlist；危险写操作 confirm；结果进上下文窗口可配置 |
| 演进 | 若未来需要互操作，在 Registry **外**加 MCP adapter，不反客为主 |

### 3.3 `MemoryPort`（记忆）

| 项 | 约定 |
|---|---|
| 职责 | 按 userId 检索 / 组装可注入片段（含时间元数据） |
| 现状 | `MemoryPort` + `MySqlMemoryPort`（关键词 + 标签 + 时间回溯窗口）；`MemoryQuery` / `MemoryFragment` / `MemoryCueExtractor` |
| 语义分层 | Working（会话内滑窗）/ Episodic（历史记录命中）/ Semantic（记录上的结构化摘要字段） |
| 必须 | 严格用户隔离；注入有条数 / 字符预算；可解释的命中理由 |
| 禁止 | 跨用户检索；把完整日记无预算塞进 prompt；为 Memory 单独立企业向量中台 |
| 已知短板 | 检索为 LIKE + 标签，**无权重 / 分词 / 向量**（R9）。升级留独立 change，同一 Port 下换实现 |

### 3.4 `GuardrailPort`（护栏）

| 项 | 约定 |
|---|---|
| 职责 | 输出（及必要的输入）检查 → verdict → 降级 |
| 现状 | 规则单一声明源 + 内容检查 + 忠实度双指标 + 按路径分流的 downgrade；六类 violation |
| 必须 | fail-closed 可配置边界清晰；规则可测；与 prompt 文案分离 |
| 隐私（已落地） | `AgentGuardrailVerdict` **类型层不承载文本**——只有 violation 类型与三个数值指标，因为它进审计与日志 |
| C3 交互 | **已关闭（P-F）**：历史记录纳入忠实度合法来源，并由 `MISSING_TIME_ATTRIBUTION` 强制时间锚点 |
| C7 交互 | 反思环**复用本端口的 verdict 作判定源**，只回传 violation 类型，不扩大隐私面（D28/D29） |

### 3.5 `TracePort`（可观测）

| 项 | 约定 |
|---|---|
| 职责 | 追加结构化事件：stage / tool / guardrail / memory / provider 元数据 |
| 现状 | `AgentTraceCollector`（内存收集）→ `AgentTraceSink`（唯一出口落 `agent_turn_trace`）；`AgentTraceVersions` 提供内容哈希版本锚点 |
| 目标 | 支撑调试与 Eval，**不**面向终端用户。C6 已兑现：评测直接断言收集器的内存状态（`persist` 是唯一出口，可拦下），因此**不需要落库也能评** |
| 必须 | 无日记原文；可按 session 查询；事件 schema 版本化 |
| 隐私（已落地） | 收集方法**只接受基础类型与既有枚举**——想把原文传进轨迹，在编译期就做不到 |
| 关键约束 | 采集点集中在单一出口，**早退路径（provider 失败 / 护栏降级 / fail-closed 丢弃）必须同样留痕** |
| 已知取舍 | 落库在业务事务**提交之后**（`TransactionSynchronization`），进程崩溃时可能丢轨迹。原方案用 `REQUIRES_NEW` 会与外层事务争父行锁，每轮卡满 50 秒 |
| 禁止 | 为接 SaaS 把原文打到第三方 |

### 3.6 `EvalPort`（评估）

| 项 | 约定 |
|---|---|
| 职责 | 加载用例 → 跑 Agent 路径（确定性替身）→ **断言轨迹不变量 + 快照回归比对** → 报告 |
| 现状 | **`confirmed`（C6 已归档）**：外置 YAML 用例 + 参数化 runner + 两层断言 + 23 条带留痕基线；既有 `AgentGuardrailBoundaryCaseTest` 原地保留、断言零修改 |
| 实现要点（C6 实测） | 替身**只替边界**（mapper 与 provider HTTP），护栏 / 状态机 / 上下文组装 / 检索收口全走生产实现——替身替掉的越多，被覆盖的生产代码越少。且替身**须走非 mock 分支**：mock 分支在组装 prompt 前即返回，只走它评不到上下文组装、也产不出降级轨迹 |
| 必须 | 维度稳定；离线零外调；**不变量层禁止刷新**，快照层变更须留 `baselineNote` 且校验值由「指标 + 说明」共同派生（只改数字会被拦住）；**不提供自动重写开关** |
| 「CI 可跑子集」的现状 | **无落点**：仓库无 `.github/`、workflow 零命中。C6 交付的是「一条 maven 命令可跑」，**不是** CI 门槛。建 CI 属独立决策 |
| 断言对象 | **优先轨迹级信号而非只看最终回复**——阶段序列、注入规模、护栏 verdict、降级层、长度比 |
| 禁止（D31） | **LLM-as-Judge 不进 C6**：原文外发需授权 + 预算不足 + 判定不可复现 |
| 诚实边界（D32） | mock 路径评的是**编排逻辑**，不是语言质量。语言质量靠真实探针小样本人评锚定，不假装用 Judge 覆盖 |

---

## 4. Agent Loop 不变式（Runtime Invariants）

无论内部类如何改名，下列不变式应被测试钉死：

1. **有界**：`maxTurns` / 阶段逼问上限 / 回复长度上限存在且生效  
2. **可恢复**：同一 user+record 可 resume ACTIVE 会话（checkpoint 语义），不靠浏览器内存当唯一真相  
3. **可停止**：用户结束意图与系统上限都能进入收束，而非死循环 tool  
4. **观察闭环**：tool 结果必须能回到下一轮上下文（或明确声明本轮不回注的原因）  
5. **失败可见**：provider/tool 失败不得 mock success 冒充真实成功（真实路径）  
6. **所有者隔离**：session / message / memory / tool 作用对象均 owner-scoped  

---

## 5. Context Engineering 纪律

2026 行业共识：上下文是稀缺注意力资源。Flashback 固定纪律：

| 规则 | 说明 |
|---|---|
| 预算先于文采 | 每类上下文（system / history / draft / memory / tool outcome）有明确 cap |
| 近详远略 | 近期会话与记录优先完整；远期优先摘要（为 **C9** 衰减预留） |
| 来源可指 | 注入的 memory 应带 recordId/time 等元数据，便于忠实度与产品话术 |
| 禁止填满窗口 | 不得「有多少历史塞多少」 |
| 模板组装 | system 规则与动态上下文分离，便于 Eval 与护栏单测 |

**C11**（Context Architecture，v1.2 编号）仅在出现**可测量痛点**（超长、噪声、费用）时升级为独立 Type C。
Phase 1 已落地的预算项：`contextMessageWindow`、`draftExcerptChars`、`memory.maxFragments` / `maxFragmentChars`、
`toolOutcomeWindow`、`maxReplyChars`。**C6 应把「预算未被突破」写成不变量断言**，
这样痛点出现时是被测出来的，不是靠感觉。

---

## 6. 数据与隐私宪法

| 规则 | 要求 |
|---|---|
| 原文边界 | 用户日记原文仅出现在：授权业务路径、受控 prompt 组装、用户可见 UI |
| 日志 | 普通日志 / AGENT_LOG / trace 导出：禁止原文；可用 hash、长度、阶段、工具名 |
| Memory | 不得跨用户；后台任务不得「全局扫描日记做训练」 |
| Eval 夹具 | 使用合成或脱敏样例；真实样本若用于闸门 3，不入库 tracked 文件。C6 形态：合成用例入库、真实样本走 gitignore 的 `local-samples.yaml`，同一 runner 两套输入 |
| 对外叙事 | `narrative/agent-tech-story.md` 是最可能被复制到外部的文档，隐私等级最高：禁止原文、secret、本机绝对路径 |
| 删除/封存 | 遵守既有封存不可变字段规则；Agent 不得提供绕过路径 |

---

## 7. 演进与「允许的破坏」

### 7.1 鼓励的演进（无需推翻宪法）

- Memory 检索从 LIKE → FULLTEXT → 旁路 embedding（**同一 MemoryPort**）  
- Trace 从表/JSON 文件 → 可导出 OTel 映射（**同一事件语义**）  
- Provider 从 DeepSeek → 其他 OpenAI-compatible（**配置 + 白名单**）  
- Guardrail 阈值按真值标定调整（须记入 design / AGENT_LOG）  
- 在 ToolRegistry 外包一层 MCP **适配**  

### 7.2 需要新 Type C + 显式决策的演进

- 改变阶段枚举语义或用户可见对话产品行为  
- 扩大 tool 写权限或取消确认  
- 改变忠实度来源集合（C3a 已定为「历史入源 + 强制时间锚点」，再改须新 change）  
- **放宽 C7 反思环的重写次数上限，或扩大开环违规集合**（v1.2 新增）  
- 引入第二模型专用 Judge 链路的费用与隐私策略  
- 平台代际升级（Optional C0）：独立成刀，**不得夹带业务改动**  

### 7.3 默认禁止的「推倒式」举动

| 禁止 | 原因 |
|---|---|
| 用 LangGraph/Hermes 等替换整套 Runtime | 产品语义锁在自研状态机；迁移成本 ≫ 收益（D27） |
| 把线性阶段机重构成 `Node`/`Edge` 类而行为不变 | 给概念找地方贴：零收益、满风险、且讲不出东西。**环要靠新能力引入，不靠改写表达形式**（D27） |
| 为追 Multi-Agent 拆「多个用户可见人格」 | 破坏「一个朋友」 |
| 自建伪 FC 协议充当 tool 主路径 | 已在 C2 明确拒绝静默降级 |
| 向量中台化 Memory | 违背 D7 与叙事分工；向量只能作为同一 `MemoryPort` 下的旁路 |
| ~~**无 Eval 情况下大改 prompt 上线**~~ | 质量不可回归。**这条正是 C6 必须先于 C7 的依据**（D30）。**C6 已归档，技术前提解除**：改动前后现在可按快照比对。但流程要求不变——改 prompt 仍属 Type C，且快照变化须在 `baselineNote` 写明由哪一刀改的，不得把数字改成当前值了事 |
| 用 LLM 判定替换已被测试钉死的确定性 checker | 用不确定换确定是净损失（D28/D31） |

---

## 8. Phase 对齐（与蓝图的关系）

> 编号以蓝图 **v1.2** 为准（Phase 2 在 Eval 之后插入了反思环，韧性与时间智能各顺移一位）。

| 阶段 | Change | 状态 | 宪法关注点 |
|---|---|---|---|
| Phase 1 | C1 → C2 → C4 → C3a → C3b → C5 | **全部归档** | 端口成形：Loop / Tool / Guardrail / Memory / Trace |
| Phase 2 | C6 `agent-eval-framework` | **已归档**（2026-07-31） | `EvalPort` 落地；轨迹不变量 + 回归比对，不做 Judge。**`src/main` 零改动**；「CI 可跑子集」如实记为无落点（仓库无 CI） |
| Phase 2 | C7 `agent-reflection-loop` | **已归档（2026-08-03）** | L1 已引入 reply-only **受控环**；判定源复用 `GuardrailPort`，上限 1 次 |
| Phase 2 | C8 `agent-resilience` | **已归档（2026-08-08）** | L4 已形成封闭分类、共享 deadline、零自动 retry 与阶段化失败边界 |
| Phase 2 | C9 `agent-temporal-intelligence` | **下一刀** | Temporal 是 L3 强化，不新造用户分析后台 |
| Optional | C0 / C10 / C11 | 证据触发 | 平台升级 / 语气标定 / 上下文架构 |

**已发生的合法漂移（写入宪法以免误判为「失败」）：**

- C4 前移至 C3 之前（气质越界实证驱动）——符合「安全闸可前移」
- C3 拆为 C3a + C3b（改动面几乎不重叠，合并会产出互相阻塞的 tasks）
- 模型层采用自研 `AgentModelClient` 而非绑定某版 Spring AI API——符合 model-agnostic
- 轨迹存 MySQL 表而非 JSON 日志文件、默认全量不采样——已批准偏离
- 轨迹落库改在事务提交后（原方案 `REQUIRES_NEW` 引发 50 秒锁等待）

**校准状态**：实现漂移登记表已并入蓝图 v1.2（2026-07-30 冻结）。本文随之升至 v0.2。

---

## 9. 给后续 Agent 的执行检查清单

开始任何 Agent 相关 Type C 前自问：

1. 是否仍只开一个 active change？
2. 改动落在哪一层？是否破坏单向依赖？
3. 是否只换端口实现，却偷偷改了端口语义？
4. 隐私与忠实度规则是否仍成立？
5. 能否用测试/用例表达验收，而不是「聊起来不错」？
6. 若 12 个月后要换检索/Judge/provider，是否仍不必重写 StageMachine？
7. **改动是否触碰锁 / 外键 / 事务边界？若是，H2 全绿不算验证，联调必须打真实 MySQL。**
8. **规划期写下的「须同步 N 处」「某端点可行」类判断，实现期是否已复核？**
9. **新增的辅助设施若失败，失败前的等待成本是多少？**（fail-open 不保证不阻塞）

任一题答不上 → 先补 design 决策，再写代码。

> 第 7–9 条来自 C5 的真实事故：轨迹落库在 H2 上 37 项测试全绿，
> 却在真实 MySQL 上让每轮对话卡满 50 秒。详见 `narrative/agent-tech-story.md` §11。

---

## 10. 修订记录

| 版本 | 日期 | 说明 |
|---|---|---|
| v0.1 | 2026-07-28 | 初稿：基于 C1/C2/C4 已归档事实 + C3/C5 方向；明确 C5 后校准再冻结 |
| **v0.2** | **2026-07-30** | 随蓝图 v1.2 冻结校准：① §2.1 现状锚点表按 Phase 1 六刀归档事实重写（L3 转 confirmed、新增 Trace 行、L4/L5 标 partial 并指向 C8/C6）② §3.3–§3.6 四个端口补「现状 / 已落地隐私约束 / 已知短板 / 已知取舍」③ §3.4 关闭 P-F、登记 C7 复用关系 ④ §3.6 写入 D31/D32 边界 ⑤ §7.2 新增「放宽 C7 上限」与「平台升级独立成刀」⑥ §7.3 新增三条禁止（贴概念式重构、向量中台、用 LLM 判定换确定性 checker）⑦ §8 按 v1.2 编号重排并补齐五条合法漂移 ⑧ §9 检查清单新增 C5 三条事故教训 ⑨ C10 → C11 编号顺移 |
