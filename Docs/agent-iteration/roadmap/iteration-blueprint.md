# 《时光回序》Flashback｜迭代蓝图（Iteration Blueprint）· v1.2

> 文档性质：长期迭代的母文档 / 宪章，不是可执行 OpenSpec change
> 状态日期：2026-07-30
> 状态：**已冻结 v1.2**（2026-07-30 用户确认冻结；修改序列须显式修订并更新 §12）
> 作者：人机协作（用户主导设计决策，Claude 执笔）
> **冻结含义**：Phase 1（C1–C5）已收官成为事实；批准 Phase 2（C6–C9）方向、依赖与气质约束。
> **仍不授权直接改业务代码**——实施须新建 Type C change，并走 `AGENTS.md` 三道闸门与 `type-c-checklist.md`。
> 校准依据：2026-07-30 v1.2 校准会（Type A 讨论，十问逐支定案），全部结论已核对代码。

---

## 0. 给 Agent 的阅读与执行约定

### 0.1 优先级

1. `AGENTS.md` Non-Negotiable → 本蓝图方向 → OpenSpec active change → baseline specs → 代码 → 旧 Docs。
2. 本蓝图**不授权直接改业务代码**。正式实施某一项时，须在 `openspec/changes/<change-id>/` 建 proposal/design/tasks/delta，走三道闸门（`AGENTS.md` Gates）。
3. 本蓝图与 `AGENTS.md` 冲突时，以 `AGENTS.md` 为准。

### 0.2 Type 分级适用

| 类型 | 与蓝图的关系 |
|---|---|
| **Type A** | 可引用蓝图做规划讨论、现状扫描。不改文件。 |
| **Type B** | 不需要蓝图授权。按 `AGENTS.md` Type B 流程走。 |
| **Type C** | 蓝图只提供方向与意图卡片。实施须建 OpenSpec change，走三道闸门。开工清单见 `Docs/agent-iteration/workflow/prompt-snippets/type-c-checklist.md`。 |

### 0.3 一次一个 active change

同时最多一个 active Type C。上一个 change 验收归档（`ACTIVE_TASK=IDLE`）后，才可开启下一个。执行顺序与调整规则见 §3.2。

### 0.4 诚实性要求（v1.2 加强）

- 意图卡片中的「现状事实」必须可核对（代码路径 / OpenSpec 引用），禁止空话。
- 标注为「待确认」的条目，不得在 change proposal 中当作已确认使用。
- 能力五态：`confirmed / partial / planned / out_of_scope / unknown`。**禁止把 `unknown` 写成 `confirmed`。**
- **规划期的「须同步 N 处」「某端点可行」类判断，实现期必须复核**（C5 教训，见 §2.3）。
- **H2 全绿不构成「真实联调」。** 涉及锁 / 外键 / 事务边界的改动，联调必须打真实 MySQL（C5 教训）。

### 0.5 外调与隐私

- AI provider、对象存储等真实外调须在 change proposal 中披露预算并单独授权。
- 用户日记原文是高敏数据，不得进入普通日志、telemetry、tracked files。
- AI API key、provider secret 只能存在于 backend-side config。

### 0.6 与治理文件的交叉引用

| 文件 | 蓝图中的角色 |
|---|---|
| `AGENTS.md` | 硬规则（Type/Gates/Handoff/Non-Negotiable）；与本文冲突时 AGENTS 优先 |
| `workflow/prompt-snippets/type-c-checklist.md` | 每个 change 开工时的操作清单 |
| `workflow/vibecoding-playbook.md` | 六步闭环协作方法论 |
| `workflow/agent-control-model.md` | 四层控制架构与三道闸门展开 |
| `architecture/agent-architecture-constitution.md` | 分层、端口、反推倒原则 |
| `architecture/tech-selection-draft.md` | 选型事实、ADR、漂移登记 |
| `narrative/agent-tech-story.md` | 对外叙事（面试向）；见 §10 |
| `项目初始分析.md` | 产品方向评估草稿（**不是**已批准 scope） |

---

## 1. 迭代总方向

### 1.1 一句话

**在克制的 ToC 日记产品里，用可演进的自研 Backend Agent Loop，做到「被动召唤、有记忆、可护栏、可观测、可回归、能自我修正」的共情友人；差异化来自时间智能与工程诚实度，而不是堆 Multi-Agent、图框架或企业 RAG。**

### 1.2 双项目叙事

| 项目 | 证明什么 |
|---|---|
| RAG 项目 | AI **知道**什么（检索、分块、可信、评测） |
| Flashback | AI **能做什么**（Loop、Tool、Memory、Guardian、Trace、Eval、Reflection、韧性、时间感） |

两个项目覆盖 AI 应用层两大核心方向：知识密集型（ToB）与交互密集型（ToC）。

### 1.3 Phase 2 的驱动目标（v1.2 定案）

**以「能力叙事」为主驱动**（2026-07-30 用户确认）：项目未上线、无真实流量，Phase 2 的价值在于把 Agent 工程能力做全、做得能讲清楚，用于面试与双项目叙事。上线后的体验优化细节不在 Phase 2 范围内。

**由此推出的两条硬约束：**

1. **「前沿」指 Agent 层的能力与概念前沿，不指平台版本前沿**（D26）。实现保持自研，只对齐行业术语与契约形状。平台升级（Spring Boot 4.x / Java 21）降为 Optional C0。
2. **凡「知道但不做」的技术，必须留下可讲述的 ADR**（§9）。行业面试考「什么时候某种编排是多余的复杂度」——能论证不做，与能做出来同等重要。

### 1.4 能力分层（与架构宪法对齐）

```text
L5 Eval & Governance     → C4 基线 ✅ + C6 ✦下一刀
L4 Resilience            → C8
L3 Intelligence Context  → C3a/C3b ✅ + C9（+ 可选 C11）
L2 Tools                 → C2 ✅
L1 Loop & Session        → C1 ✅ + C7（为 Loop 引入受控环）
L0 Platform              → Spring Boot / MySQL / Mini Program ✅（Optional C0 升级）
```

术语对齐 2026 主流（**不引入同名框架**）：Loop Engineering、TAG、Multi-layer Memory、Context Engineering、Agent Trace、Eval loop、Reflection / Self-correction。

---

## 2. Phase 1 完成事实

> 本表以 archive 与 baseline spec 为准。

| Change | change-id | 归档日期 | 相对 v1.1 意图的重要漂移 |
|---|---|---|---|
| C1 | `agent-runtime-mvp` | 2026-07-27 | 自研 `AgentStageMachine` + MySQL 会话；非 Spring AI 绑定（P2 定为 MySQL） |
| C2 | `agent-tool-calling` | 2026-07-28 | 原生 FC + model 白名单；**无假协议降级**（P1 关闭） |
| C4 | `agent-guardrails-hardening` | 2026-07-28 | **前移至 C3 之前**；忠实度双指标 + 规则单一声明源（P6 关闭） |
| C3a | `agent-memory-retrieval` | 2026-07-29 | C3 拆两刀之一；`MemoryPort` + 关键词/标签/时间检索（P4 关闭：LIKE + 标签，非 FULLTEXT） |
| C3b | `agent-review-chat` | 2026-07-29 | C3 拆两刀之二；复用 `agent_session` + `purpose`；`ReviewChatSheet` 浮层（P5 关闭） |
| C5 | `agent-observability` | 2026-07-30 | **MySQL 表存储**而非 JSON 日志文件；默认全量不采样（P7 关闭，两处已批准偏离） |

**Phase 1 收官验证基线：** 后端 **534 tests PASS / 3 skipped**（3 skipped 为环境门控的真实 provider 探针）。

### 2.1 已确认决策

**继承 v1.1 D1–D19：**

| # | 决策 | 来源 |
|---|---|---|
| D1 | M4 已于 2026-07-27 正式归档；delta 已接受进 baseline | 2026-07-27 归档 |
| D2 | Agent 气质：共情型朋友——不太热情也不冷漠，主动找它聊时它永远是最懂你的朋友 | 蓝图讨论 |
| D3 | Agent 主动性：被动召唤型——不主动弹窗/推送，参与时展现深度共情与记忆 | 蓝图讨论 |
| D4 | 架构方向：Backend 侧 Agent Runtime（Spring Boot 托管状态机/Tool/Memory/对话） | 蓝图讨论 |
| D5 | 拆分粒度：每个能力独立 Type C change，一次只 ACTIVE 一个 | 蓝图讨论 |
| D6 | Runtime MVP 先做「写下此刻」多轮引导；「友人回看」后置 | 蓝图讨论 |
| D7 | Memory 初期简单检索；**不做第二套企业 RAG / 向量中台** | 蓝图讨论 |
| D8 | 回看交互：保留结构化摘要 + 新增「和它聊聊」入口 | 蓝图讨论 |
| D9 | 跨记录关联合并到 Memory change | 蓝图讨论 |
| D10 | 友人回看对话合并到 Memory change | 蓝图讨论 |
| D11 | Eval 定位：Agent 决策链路可观测（工程向，不面向用户） | 蓝图讨论 |
| D12 | 蓝图不含治理卡片——治理已由 workflow 文档解决 | 蓝图讨论 |
| D13 | 三 Tab 不变：首页、时光轴、个人中心 | `AGENTS.md` |
| D14 | 用户可见命名：我的记录、时光轴、时间回看 | `AGENTS.md` |
| D15 | 封存后 location/attachments/cover 不可变 | `AGENTS.md` |
| D16 | secret 仅 backend | `AGENTS.md` |
| D17 | 真实路径不得 mock success 冒充真实成功 | `AGENTS.md` |
| D18 | 不做大规模 backend rewrite | `AGENTS.md` |
| D19 | C1 必须内嵌最小护栏；C4 是系统化 hardening 而非「从零第一次有护栏」 | v1.1 修订 |

**Phase 1 执行期新增 D20–D24：**

| # | 决策 | 来源 |
|---|---|---|
| D20 | C4 允许因气质越界实证前移 | v1.1 §7（2026-07-28） |
| D21 | FC 不可用时退回纯对话，不做自研 tool 协议 | C2 design |
| D22 | Agent 复用 `app.ai` 凭证通道，不新增 secret 字段 | C1/C2 |
| D23 | Phase 2 默认不推翻自研 Runtime | 架构宪法 |
| D24 | 轨迹存 MySQL 表、默认全量不采样 | C5 design（已批准偏离） |

**v1.2 校准会新增 D25–D33：**

| # | 决策 | 来源 |
|---|---|---|
| D25 | **Phase 2 以能力叙事为主驱动**；未上线，不为想象中的生产故障提前投入 | 校准会 Q1 |
| D26 | **「前沿」限定在 Agent 层**；平台升级降为 Optional C0，不排进主线 | 校准会 Q2 |
| D27 | **不引入图框架**（LangGraph4j 等）；改为「引入真正需要环的能力」+ 留可讲述 ADR | 校准会 Q3 |
| D28 | **反思环判定源复用 C4 确定性护栏**，不新起 LLM 自检器 | 校准会 Q4 |
| D29 | **重写指令只回传违规类型**，不携带候选文本片段（隐私面零扩大） | 校准会 Q4 |
| D30 | **Eval 先于反思环**：先建量尺，再改模型输出行为 | 校准会 Q5 |
| D31 | **LLM-as-Judge 明确排除在 C6 之外**（隐私外发 + 预算 + 不可复现三重理由） | 校准会 Q7 |
| D32 | **Eval 覆盖轨迹不变量与回归比对，不做绝对质量判分**；语言质量靠真实探针小样本人评锚定 | 校准会 Q7 |
| D33 | **叙事文档是 Phase 2 每刀的固定收尾产物**，不是最后一次性补写 | 校准会 Q9 |

### 2.2 待确认（滚动；关闭后移入 design）

| # | 事项 | 时机 | 状态 |
|---|---|---|---|
| P8 | Eval 用例的可编排 mock 替身形态 | C6 design | open |
| P13 | 反思环与轨迹 `attemptNo` 的关系（重写是否算新 attempt） | C7 design | open |
| P14 | C8 韧性可用的超时预算（须扣除反思环已占用部分） | C8 design | open |
| P12 | Temporal 最小记录数阈值 | C9 design | open |
| P15 | Optional C0 的触发条件与验收形态 | C0 proposal（若开） | open |

**已关闭：** P1（FC 白名单）、P2（MySQL 会话）、P3（Tool 白名单）、P4（LIKE + 标签检索）、P5（浮层）、P6（多层确定性检查）、P7（MySQL 轨迹表）、P-F（历史入忠实度来源，C3a 已定）、P9（Judge → D31 排除）。

### 2.3 已被实测证伪 / 修正的前提（五条）

> v1.2 起草期的部分假设已被实测推翻。**冻结前已逐条消化。**

| # | 原假设 | 实测事实 | 影响 |
|---|---|---|---|
| 1 | `/admin/**` 端点可作为工程向查询入口 | **`AuthRole.ADMIN` 全仓无签发路径**（`UserServiceImpl` 固定签 `USER`），该路径在真实环境不可达 | 任何未来 change 若要做 admin 端点，须先解决签发问题 |
| 2 | 新表须同步三处 schema | **`schema.mysql.sql` 只到 C1**（无 C2 表、无 C3 列、无 C5 表）；项目既有约定是全量脚本不随增量维护 | 见 §8；规划期断言须在实现期复核 |
| 3 | H2 集成测试足以验证数据层 | **C5 的 50 秒锁等待在 H2 上不可能复现**（无 InnoDB 行级锁语义），37 项全绿却带致命缺陷进归档 | 「真实联调」定义收紧为**包含真实 MySQL**（已写入 §0.4） |
| 4 | R2「引导话术生硬」判断有效 | 该判断样本受 55 秒延迟污染；修复后用户反馈「自然一些了」而**未改任何 prompt/阈值** | **R2 基线须用 C6 重建**，不靠再手验找感觉 |
| 5 | 认证基于 Spring Security | **`springframework.security` 全仓零匹配**，pom 无 security starter，认证为 jjwt + 自研过滤器 | 已修 `.kiro/steering/tech.md`（校准会新发现） |

**附带登记（非错误，仅事实补充）：** `pom.xml` 含 `spring-boot-starter-data-redis`，`application-dev/prod.yml` 有 redis 配置段，但 **main 代码零消费**（会话持久化走 MySQL）。标记 `partial`，不在 Phase 2 处理。

---

## 3. 总序列

### 3.1 Phase 1（核心能力）— 已收官

```text
M4 ✅ → C1 ✅ → C2 ✅ → C4 ✅ → C3a ✅ → C3b ✅ → C5 ✅
                                              │
                                              ▼
                                    【校准会 2026-07-30 → 冻结 v1.2】
```

### 3.2 Phase 2（能力叙事完备）— v1.2 定案序

| 顺序 | change-id | 一句话目标 | Type | 硬依赖 |
|---|---|---|---|---|
| **C6** | `agent-eval-framework` | 建量尺：轨迹不变量 + 回归比对，可离线跑 | C | C1；强建议 C5（复用 C4 用例种子） |
| **C7** | `agent-reflection-loop` | 加受控环：护栏不合格 → 带类型化要求重写一次 | C | C4（判定源）+ C6（回归基线） |
| **C8** | `agent-resilience` | 错误分类 + 阶段化温暖降级 | C | C1；须扣除 C7 已占用的调用预算 |
| **C9** | `agent-temporal-intelligence` | 时间距离感知 + 记忆衰减 + 克制的周期提及 | C | C3a/C3b；建议 C6 约束后 |

**默认执行顺序：C6 → C7 → C8 → C9**（一次一个 ACTIVE，不得并行）。

**编号说明：** v1.2 起草稿为 C6 Eval → C7 韧性 → C8 时间智能。校准会在 Eval 之后插入 `agent-reflection-loop` 占 **C7**，韧性顺移 **C8**，时间智能顺移 **C9**，Optional 项顺移 C10/C11。理由：编号即执行序，便于叙述。

**为什么 Eval 必须先于反思环（D30，本轮最重要的排序决策）：**

反思环的价值主张是「重写后质量更好」。没有量尺，这个主张只能靠手验找感觉——而**本项目已被证明一次感觉不可靠**（§2.3 第 4 条：R2 判断受延迟污染，用户觉得变好时其实一行 prompt 都没改）。反思环若先做，只会得到第二个无法证伪的「感觉好了」。反之 Eval 先做，反思环上线时天然获得回归基线，正是架构宪法 §7.3「无 Eval 情况下大改 prompt 上线」禁令的正确用法——**反思环本质上就是在改模型输出行为。**

**为什么不合并成一刀：** 反思环要跨越「生成」与「检查」两处、掉转依赖方向，而 `AgentChatServiceImpl` 已 **1183 行 / 48 方法**（后端 main 共 14890 行，单类占 8%）。与全新 Eval 基础设施合并，是最容易重演「37 项全绿却带致命缺陷归档」的组合。

### 3.3 Optional（触发条件明确，不预支工期）

| ID | 名称 | 触发条件 |
|---|---|---|
| C0 | `platform-modernization` | **Phase 2 全部完工后**，或某刀确实需要 Boot 4.x 特性。见 §4.6 |
| C10 | Tone Calibration | C1+C4+C7 仍无法稳住语气冷热/长短，且 C6 能给出量化证据 |
| C11 | Context Architecture | Memory 注入后 token / 噪声 / 费用**可测**恶化 |

### 3.4 旁支（仍不在 Agent 主线）

设置页大改、生产通知中心 / SMS、Admin portal（另见 §2.3 第 1 条：ADMIN 角色无签发路径）、完整 RAG 中台化 Memory、通用多租户 Agent 平台、major 视觉重建、M1/M3 未归档目录清理、MySQL `EXPLAIN` carry-over。

### 3.5 明确不进入主线的「伪前沿」

- Hermes / 自治自改进主架构
- 用户可见 Multi-Agent 团队
- 情绪诊断 dashboard
- Voice / STT
- 主动推送
- 为 MCP / LangGraph / Spring AI 整包重写
- **图框架（LangGraph4j 等）**——D27；理由见 §9.2

---

## 4. 意图卡片

### 4.1 Phase 1 已完成（摘要，细节以 archive 为准）

| Change | 落地能力 |
|---|---|
| C1 ✅ | Loop + 「写下此刻」多轮引导 + 最小护栏 + MySQL 会话持久化 |
| C2 ✅ | Allowlist tools + 原生 FC + 二段式确认 + observation 回注 |
| C4 ✅ | 多层确定性检查 + 忠实度双指标 + 分路径降级 + 规则单一声明源 |
| C3a ✅ | `MemoryPort` + 关键词/标签/时间检索 + 注入预算 + 时间归属护栏 |
| C3b ✅ | 友人回看会话（`purpose` 区分）+ `ReviewChatSheet` 浮层 |
| C5 ✅ | versioned 决策轨迹（thought→action→observation）+ 内容哈希版本锚点 + 9 条排查查询 |

**C5 产出的三项 Phase 2 直接输入：**

1. **provider 耗时首次有数据**：min 4571 / avg 6476 / max 8467ms。C5 之前成功路径的 `startedAt` 被直接丢弃。它是 C7 环预算与 C8 韧性设计的定量基础。
2. **版本锚点由内容哈希派生**，改文案自动变化。C6 的回归比对可按 `prompt_version` / `policy_version` 分组（`c5-trace-queries.sql` 第 7 条）。
3. **每轮已产出完整结构化信号**（`AgentTraceCollector`）：阶段 from/to/reason、记忆 enabled/failed/retrieved/injected、上下文 messageCount 与各补充段、provider model/durationMs/mocked/success、工具 returned/proposed/discarded、各层护栏 passed/violation/coverage、降级 layer/fallback、裁剪 before/after、素材 produced/chars。**这是 C6 的断言对象。**

---

### 4.2 C6 · `agent-eval-framework` ✦ 下一刀

**把已有护栏回归资产升级为跨维度、可回归的评测框架**

#### 现状事实（已核对代码）

- `AgentGuardrailBoundaryCaseTest` **已是结构良好的黄金集**：五个场景分组、每组含正例与反例、全部离线不调 provider、并带一条「metrics 不泄漏原文」隐私断言。`confirmed`
- 它缺的三样：输入未外置、断言只覆盖护栏层不覆盖端到端、无「气质/克制」维度。`confirmed`
- `AgentMockResponder` 是**纯函数**（输出只由 `targetStage` + `userInput` 决定，无随机/时间/外部状态）→ 快照天然稳定。`confirmed`
- **但它永远产不出违规内容**：`reply()` 返回六句写死引导语、`material()` 只拼接用户发言（恒定忠实）、`toolCalls()` 只在 `CORE_QUESTION` 提议一次。**mock 路径跑不出任何降级轨迹。** `confirmed`
- C5 版本锚点与 `c5-trace-queries.sql` 第 7 条已提供按版本分组比对的地基。`confirmed`

#### 目标

1. **用例载体分层**：`AgentGuardrailBoundaryCaseTest` **原地保留不动**（既有断言零修改，符合项目纪律）；新增维度走外置数据文件 + 参数化 runner。
2. **外置文件区分入库与本地**——解决 Java 内联做不到的样本隐私问题：

```text
backend/src/test/resources/eval/
  cases/restraint.yaml          # 克制/长度/不话痨（合成样例，入库）
  cases/empathy.yaml            # 共情语气（合成样例，入库）
  cases/memory-relevance.yaml   # 记忆命中相关性（合成样例，入库）
  cases/local-samples.yaml      # 真实样本，gitignore；缺失时静默跳过
```

3. **维度定义**：

| 维度 | 断言对象 | 类型 |
|---|---|---|
| 阶段推进正确性 | `stage-decision` 的 from/to/reason 序列 | 确定性 |
| 追问克制 | 同阶段 `REASK` ≤ 1、`TURN_LIMIT_REACHED` 可触发 | 确定性 |
| 记忆三态可分 | `enabled/failed/retrievedCount` 组合不混淆 | 确定性 |
| 注入预算 | `injectedCount` ≤ 配置、`injectedChars` ≤ 预算 | 确定性 |
| 护栏有效性 | 各层 `violation` 与期望一致（已有资产） | 确定性 |
| 长度克制 | `afterLength` ≤ `maxReplyChars`、回复/输入长度比 | 确定性 |
| 工具 fail-closed | 无工具模式下 `discardedCount` 行为 | 确定性 |
| **话术质量** | **同版本快照比对 + 少量人评锚点** | **回归型** |

4. **快照分层**：

| 层 | 内容 | 失败语义 |
|---|---|---|
| 不变量 | 阶段序列合法、`REASK ≤ 1`、注入 ≤ 预算、长度 ≤ 上限、该降级必降级、无工具模式必 fail-closed、metrics 不含原文 | **硬失败，不允许刷新**。变了就是 bug |
| 快照 | 每用例轨迹指标摘要（注入条数/字符、provider 调用次数、降级层、阶段路径、回复长度比） | 失败 = **需人确认**，可更新但必须留痕 |

5. **防橡皮图章机制**：快照文件每用例带 `baselineNote` 字段，记录该基线由哪一刀、哪个 `policyVersion` 定下。更新快照必须同步更新此字段——**改数字不改说明，diff 里一眼可见**。配合 AGENT_LOG 纪律形成可审计落点。
6. **新增可编排 mock 替身**（P8）：**不得改 `AgentMockResponder`**（它是 `@Component`，mock provider 下在生产路径使用，改它会污染真实路径）。需另建测试替身，支持按轮次编排响应序列，以覆盖降级轨迹。

#### 用户故事（开发者视角）

- **改前**：改了引导话术或护栏阈值，只能手验读几条回复判断好坏。R2 的判断因此被 55 秒延迟污染而失效，且无人察觉。
- **改后**：改动后跑一次离线评测，得到「哪些不变量被破坏」与「哪些指标相对基线变了」两类结论。R2 优化第一次有了可比对的前后基线。

#### 非目标 / out_of_scope

- **LLM-as-Judge**（D31）。三条具体理由：① 日记原文送第二个模型打分属未授权外发，每跑一次评测都要闸门 3；② 真实调用预算在「6 次/预算 10」量级，Judge 跑几十条即爆表；③ 分数不可复现，违反宪法 §3.6。**列为显式非目标，并在叙事文档 §9 记录「知道但没用」的理由。**
- 不做商业评分 dashboard、不绑定外部 SaaS。
- **不改既有 `AgentGuardrailBoundaryCaseTest` 断言。**
- 不做 A/B testing 框架。

#### 必须写进文档的边界（防面试被问穿）

用 mock provider 跑评测，评的是**编排逻辑**，不是模型的语言质量。诚实表述为：**「Eval 覆盖轨迹不变量与回归比对；语言质量靠真实探针小样本人评锚定，没有假装用 Judge 覆盖它。」**

#### 验收证据类型

- [ ] 外置用例 + 参数化 runner，离线零外调
- [ ] 不变量断言集（硬失败层）
- [ ] 快照比对 + `baselineNote` 留痕机制
- [ ] 快照不含任何用例输入文本（同 `verdictMetricsMustNotLeakContent` 套路的隐私断言）
- [ ] 可编排 mock 替身能产出降级轨迹
- [ ] 既有 534 tests 零回归、既有断言零修改

#### 关键风险

| 风险 | 缓解 |
|---|---|
| 快照沦为橡皮图章 | 不变量层禁止刷新 + `baselineNote` 强制留痕 |
| 用 mock 评质量的自欺 | 文档显式划界；语言质量走人评锚点 |
| 用例膨胀难维护 | 外置文件；确定性护栏用例留 Java 不迁移 |
| 真实样本进 tracked file | `local-samples.yaml` 走 gitignore，缺失静默跳过 |

---

### 4.3 C7 · `agent-reflection-loop` 🆕

**为 Loop 引入一个受控的环：护栏判定不合格 → 带类型化改写要求重生成一次**

#### 为什么是「环」而不是「图」（D27）

`AgentStageMachine` 现状（已读代码）：线性序列 `EMOTION → CONFUSION → CORE_QUESTION → EXPECTATION → CLOSING → ENDED`，加两条抢占边（结束意图、轮次上限）、一条自环（回避型追问上限 1）；REVIEW 刻意不经此机器。**这已经是一张图**，只是用 `List` + `indexOf` 表达。

把它重构成 `Node`/`Edge` 类而行为不变，是给概念找地方贴——**零收益、满风险、且讲不出东西**。真正缺的是**环**：现在护栏只能「拦下并降级」，不能「让模型重来一次」。补上环才是引入图语义的真实动因，对应行业术语 reflection / self-correction。

#### 现状事实（已核对代码）

- `AgentGuardrailVerdict` 是 record：`violation` + `coverage` + `maxUncoveredRun` + `checkedLength`。**类注释明写不得携带候选文本、用户原话或未覆盖片段内容**（它会进审计与日志）。`confirmed`
- `AgentGuardrailDowngrade` 按路径分流：工具提议→拒绝、素材→丢弃、回复→**本地常量兜底**，且明写兜底不得伪装成模型正常输出。`confirmed`
- 护栏挂点在 `applyReplyGuardrail`(L915) 与 `applyMaterialGuardrail`(L991)，均为**同步、单次、失败即 return**；provider 调用在 `generateReply`(L563/L767)。`confirmed`
- 后置检查**不持有调 provider 的能力**——加环需掉转依赖方向。`confirmed`
- `AgentChatServiceImpl` 已 **1183 行 / 48 方法**。`confirmed`

#### 目标

1. **判定源复用 C4 确定性护栏**（D28），不新起 LLM 自检器。
2. **重写指令只回传违规类型**（D29）：把 violation 映射为固定的中文改写要求，不携带任何候选文本片段。**隐私面零扩大，`AgentGuardrailVerdict` 不动。**
3. **只对两类违规开环**：

| 违规 | 现处置 | 开环 | 理由 |
|---|---|---|---|
| `UNFAITHFUL` | 回复→兜底 | **开** | 最高频、最可挽救，兜底对体验损失最大 |
| `MISSING_TIME_ATTRIBUTION` | 回复→兜底 | **开** | 修法明确（加时间锚点），一次即可改对 |
| `FABRICATED_QUOTE` | 拒绝/兜底 | 不开 | 伪造引用说明本轮已跑偏，重写风险高 |
| `DIAGNOSTIC` | 兜底 | 不开 | 越界行为，不给第二次机会 |
| `FAKE_ACTION` | 兜底 | 不开 | 谎称已封存属严重越界 |
| `CHECK_ERROR` | fail-closed | **绝不开** | 判定器自身异常，重试等于不设防再喂一次 |

4. **预算：最多重写 1 次。** 重写后仍违规 → 走现有降级，行为与今日完全一致。最坏 2 次调用 ≈ 13s，仍在后端 20s 内——**不需要动超时**（超时刚经真机验证，不要再碰）。
5. 顺势把「生成 + 检查 + 重写」抽为独立协作者（如 `AgentReplyPipeline`）——**这是掉转依赖方向的必要手段，不是顺手重构**。

#### 用户故事

- **改前**：Agent 一句话增写了用户没说过的内容，护栏拦下，用户收到一句本地常量兜底回复——对话看起来突然变得敷衍。
- **改后**：同样被拦下，但 Agent 拿到「你上一版增写了用户没表达的内容，请只基于用户原话重述」后重生成一次，多数情况用户收到的是一句合格的真实回复而非兜底。

#### 非目标 / out_of_scope

- 不引入图框架。
- 不做 LLM 自检器（成本与不确定性论证见 §9.2）。
- 不携带文本片段做精细诊断（见下）。
- 不放宽任何现有护栏阈值。
- 不修改前后端超时配置。

#### 已评估但不做（写进叙事文档 §9）

**「仅内存、不落盘」的精细诊断载体**：让 checker 额外产出未覆盖片段位置/文本，只用于拼重写 prompt。**不做的理由**：靠后人自觉维持隐私边界，半年后有人为排查方便把它塞进 trace，红线就破了且无测试报警；要做得配「诊断载体不得出现在 trace/log 参数」的约束测试，成本反超收益。与本项目「fail-open 只保证不报错，不保证不阻塞」同型的「以为无害其实有害」陷阱。

#### 验收证据类型

- [ ] 六类违规的开环/不开环行为单测（含 `CHECK_ERROR` 绝不重试）
- [ ] 重写次数上限断言（最多 1 次）
- [ ] 重写指令不含任何候选文本的隐私断言
- [ ] 轨迹能区分「首次生成 / 重写 / 最终降级」（P13）
- [ ] C6 评测集零回归 + 快照变更经 `baselineNote` 留痕
- [ ] 真实 MySQL 联调（含事务边界复核）
- [ ] 真机手验：重写路径的实际耗时

#### 关键风险

| 风险 | 缓解 |
|---|---|
| 耗时翻倍顶到超时 | 硬上限 1 次；最坏 ≈13s < 20s；轨迹记录实测耗时 |
| 在 1183 行类中改依赖方向 | 抽出 `AgentReplyPipeline`；534 tests 作安全网；真实 MySQL 联调 |
| 重写让违规「看似可恢复」，R10 更难关闭 | 轨迹须能区分「重写成功」与「终态降级」，两者分别计数；写进 design 取舍 |
| 环导致死循环 | 上限由代码常量而非模型判断决定（延续 C1「推进权在后端」原则） |

---

### 4.4 C8 · `agent-resilience`

**错误分类 + 阶段化温暖降级；让用户感觉「它话少」而不是「系统炸了」**

**目标**：① 错误分类与超时策略（**须扣除 C7 已占用的调用预算**，P14）；② 阶段化温暖模板降级；③ 可选多 provider 路由（二期，deferred）。

**非目标**：语义缓存不作为第一期阻塞项（冷启动价值有限）；不做复杂熔断中间件，可先复用护栏越界 / 死循环 tool 计数。

**与 C7 的硬耦合（v1.2 新登记）**：C7 最坏 2 次调用 ≈13s。C8 若再加重试，叠加即爆 20s。**C8 design 必须把「C7 已占用的调用预算」作为输入约束**，不得等开工才发现。

---

### 4.5 C9 · `agent-temporal-intelligence`

**目标**：时间距离感知话术、记忆衰减注入策略、克制的周期模式提及、回看场景时间锚点。

**非目标**：诊断式趋势、「焦虑 +40%」、任何情绪 dashboard、新建前端分析页。实现落在 L3 策略模块。

**前置**：C6 的克制维度须已能约束「时间话术不滑向分析」。

---

### 4.6 Optional C0 · `platform-modernization`

**现状事实（已核对）**：`pom.xml` 为 Spring Boot **3.3.5** / Java **17** / mybatis-starter 3.0.3；Jackson 直接 import 仅 20 处；无 security starter（认证手写）；含未消费的 redis starter。

**外部事实（2026-07）**：Spring Boot 4.0 GA 2025-11-20、4.1 GA 2026-06-10；4.0 为代际重置（Jakarta EE 11、Jackson 3、auto-config jar 拆分、JSpecify null safety）；每分支约 13 个月 OSS 补丁。3.3.x 的免费补丁窗口按此节奏推算已关闭——**此项为公开节奏推算，未查到官方 EOL 页面，标记 `unknown`，引用前须先核实**。

**为什么不排进主线（D26）**：重心不在平台层，收益低。其风险性质与 C3/C5 不同——C3/C5 的风险是「设计对不对」，可由测试回答；平台升级的风险是「哪个传递依赖不兼容」，工作量长尾不可预估。

**触发条件**：Phase 2 全部完工后，或某刀确实需要 Boot 4.x 特性（如 Spring AI 2.0 评估，它要求 Framework 7）。

**若开工则须遵守**：它改 `pom.xml`，撞 `AGENTS.md`「不改 package/lockfile 除非明确要求并说明原因」——须用户显式授权，且**不得夹带任何业务改动**。

---

## 5. Spec delta 建议落点（以各刀规划闸为准）

| Change | backend-core | miniapp-core | v2-product-scope | agent-runtime | agent-collaboration |
|---|---|---|---|---|---|
| C6 | Eval runner（test 范围为主） | — | — | 可回归性条款 | 评估与样本隐私规范 |
| C7 | 重写环与预算 | — | 回复质量行为 | **环语义 + 上限不变式** | 重写指令脱敏规范 |
| C8 | 降级/分类/路由 | 平滑错误态（若需） | — | 韧性不变式 | 韧性规范 |
| C9 | 时间策略 | 话术体验 | 时间回看增强 | 时间锚点条款 | 反诊断约束 |

---

## 6. 产品初心与 Agent 气质约束

### 6.1 产品初心（不可违背）

《时光回序》帮助用户写下当下的情绪、困惑、期待、犹豫与生活片段。未来回看不是产品唯一承诺；它更像是把回答权交给时间，让未来的自己重新理解写下这一刻的自己。

**产品气质：安静、私密、克制、温柔，并带有时间感。**

### 6.2 Agent 气质定义

Agent 是一个**共情型朋友**：

- **不热情也不冷漠**——不会主动弹出来打招呼，但你找它聊天时，它总是在的。
- **最懂你的朋友**——它记得你写过的东西，理解你反复出现的情绪，但不会拿这些来「分析」你。
- **被动召唤**——只在用户明确操作时参与（点击、触发、发消息）。不弹窗、不推送、不主动干预。
- **输出克制**——回复简洁温暖，长度与用户表达相称。不写长篇大论，不显得比用户「更懂」用户。
- **建议不代决**——可以说「要不要把这条封存？」，但永远不会自动封存。

### 6.3 Agent 绝对禁止

| 禁止 | 原因 |
|---|---|
| 心理诊断 / 医学建议 | 产品不是心理咨询工具 |
| 篡改或覆写用户原文 | 尊重用户原始表达 |
| 代替用户做重要决策（封存/解锁/删除） | Agent 是朋友不是管家 |
| 效率仪表盘 / 诊断 dashboard | 与产品气质冲突 |
| 社交动态 / 分享功能 | 私密性是产品基石 |
| 将日记原文写入日志/telemetry | 隐私保护 |
| 主动推送 / 弹窗 / 未请求的分析 | 被动召唤原则 |
| 话痨——回复比用户写的还长 | 克制原则 |

### 6.4 Agent 应该做到

| 行为 | 示例 |
|---|---|
| 温和引导 | 「今天是什么让你想写下这一刻？」 |
| 共情回应 | 「听起来你最近过得不太容易。」 |
| 记忆关联 | 「我记得你上个月也写过类似的感受。」 |
| 尊重沉默 | 用户不想继续聊时优雅结束——「好的，这些已经很好了。」 |
| 行动建议 | 「要不要给这条记录加个标签？」 |

### 6.5 Phase 2 的气质落点（v1.2 新增）

- **C7 的环不得改变气质**：重写只为「更忠实」，不为「更热情」或「更长」。
- **C6 的克制维度是气质的第一个可回归表达**——此前气质只靠 system prompt 与人工感觉。

---

## 7. 反推倒底线（冻结句）

### 7.1 六条底线

1. 保持自研 Agent Loop 与产品阶段语义
2. 保持 OpenAI-compatible + 配置化 provider
3. 保持 Tool allowlist + 确认写操作
4. Memory / Trace / Eval 以 Port 演进，不绑死单一向量库 / SaaS
5. 禁止第二套企业 RAG 与 Multi-Agent 主架构
6. **禁止用图框架替换 Runtime**；环在自研 Loop 内实现（v1.2 新增，D27）

### 7.2 允许的演进

- Memory 检索 LIKE → FULLTEXT → 旁路 embedding（同一 `MemoryPort`）
- Trace 表 → 可导出 OTel 映射（同一事件语义）
- Provider 切换（配置 + 白名单 + 回归）
- 护栏阈值按实测标定（须留证据）
- ToolRegistry 外包 MCP 适配层
- 平台版本升级（Optional C0，独立成刀不夹带）

### 7.3 需要新 Type C + 显式决策

- 改阶段枚举语义或用户可见对话行为
- 扩大 tool 写权限或取消确认
- 改忠实度来源集合
- **放宽 C7 的重写次数上限或扩大开环违规集合**（v1.2 新增）
- 引入第二模型 Judge 链路（费用 + 隐私策略须先定）

---

## 8. Residual / Carry-over

| ID | 事项 | 处置 |
|---|---|---|
| R2 | 引导话术与素材合成质量 | **基线须由 C6 重建**（§2.3 第 4 条）；不再靠手验找感觉 |
| R9 | 检索相关性弱（LIKE，无权重/分词/向量） | 升级留独立 change；同一 `MemoryPort` 下演进 |
| R10 | 回看 fail-closed 未活体触发（9 轮观察） | 不单开 change；C5 已能记下它真发生的那次；**C7 会改变其观察条件**，见 §4.3 风险表 |
| R6 | 凭证轮换（待用户执行） | `AI_API_KEY` / `S3_*` / `WECHAT_MINI_PROGRAM_SECRET`；轮换后删 `start-dev-wechat.local.ps1.bak` |
| — | `schema.mysql.sql` 落后于增量脚本（只到 C1） | 独立 Type B；补齐需同时补 C2 表 + C3 列 + C5 表；**待用户决定** |
| — | 轨迹写在业务事务之后 | 可接受：进程崩溃时可能丢轨迹，代价远小于每轮卡 50 秒 |
| — | MySQL `EXPLAIN` timeline（M4 carry-over） | Type B，不阻塞主线 |

**探针资产**（默认门控跳过，勿擅自开启）：`C3RealProviderProbeTest`、`C4RealProviderProbeTest`、`C5RealProviderProbeTest`、`C5MysqlTraceProbeTest`。

---

## 9. 「知道但不做」清单（叙事资产）

> 本节是 §10 叙事文档的素材源。行业面试会考「什么时候某种编排是多余的复杂度」——**能论证不做与能做出来同等重要。**

### 9.1 不做 LLM-as-Judge（D31）

日记原文外发需授权 + 真实调用预算在个位数量级 + 分数不可复现（违反宪法 §3.6）。改用「轨迹不变量 + 回归比对 + 人评锚点」。

### 9.2 不引入图框架 / 不做 LLM 自检器（D27/D28）

- **图框架**：现有状态机已具备节点、边、抢占、自环、终态；图框架的增量收益是可视化与 checkpoint，前者不需要，后者已由 MySQL 会话表实现 resume。引入需重写已测阶段语义，成本远超收益。**留了 Port，没留框架。**
- **LLM 自检器**：生成 6.5s + 自检 6.5s + 重写 6.5s ≈ 19.5s，直接撞死 20s 后端超时；且用不确定判定替换 534 测试守着的确定性 checker，是净损失。

### 9.3 不迁 Spring AI / 不整包重写（ADR-R5 强化）

契约已是 industry shape（OpenAI-compatible messages/tools）；框架是实现便利，不是能力解锁。且 Spring AI 2.0 要求 Boot 4.x + Framework 7，须先做 Optional C0，风险叠乘。**更关键：迁移会同时吃掉 C2 ToolRegistry、C3 MemoryPort、C4 护栏挂点、C5 轨迹采集点，而这些正是可讲述与可逆向学习的部分。**

### 9.4 不上向量库（D7 / ADR-R3）

当前规模用 SQL 检索足以验证「记得你」；向量作为同一 `MemoryPort` 下的 v2 插件。提前上云向量库会让隐私、成本、本地开发与 Eval 夹具全部变重。

### 9.5 不做 MCP 内核化（ADR-R2）

Flashback 的工具是内生业务 API，无外部消费方；MCP 化只增加转换层与安全面。列为可选适配层。

### 9.6 不做精细诊断载体（v1.2 新增）

见 §4.3「已评估但不做」。

---

## 10. 叙事交付物（D33）

`Docs/agent-iteration/narrative/agent-tech-story.md`，**按面试问题组织，不按分层组织**：

```text
§1  一分钟版本（项目是什么 / Agent 承担什么 / 双项目分工）
§2  为什么自研 Loop 而不用 LangGraph / Spring AI      ← §9.2 §9.3 + 宪法 §7.3
§3  工具调用怎么做的、为什么不做假降级                ← C2
§4  护栏怎么判、为什么不用 LLM 审核官                  ← C4 双指标 + §9.2
§5  记忆分层怎么落地、为什么不上向量库                ← C3a/C3b + §9.4
§6  可观测性做了什么、发现了什么                      ← C5 + 耗时分布 + 锁等待事故
§7  怎么保证改了 prompt 不退化                        ← C6
§8  反思环：为什么有环、环的预算与代价                ← C7
§9  我知道但没做的，及理由                            ← 本文 §9
§10 踩过的坑（H2 全绿≠验证 / REQUIRES_NEW / 超时顺序） ← ACTIVE_TASK 流程教训
```

**时机**：§1–§6、§10 在 v1.2 冻结时即写（素材已齐，C1–C5 全部归档）；§7/§8 随 C6/C7 归档补；§9 持续追加。**每刀归档时更新对应段落是固定收尾项**——C5 那些具体数字（4571/6476/8467ms、`REQUIRES_NEW` 锁等待根因、9 轮未触发 fail-closed）现在还在手上，三个月后补写只会剩「我做了可观测性」。

**硬边界**：本文档禁止出现用户日记原文、真实 secret、本机绝对路径。它是最可能被复制到外部（简历 / 博客 / 白板）的文档，隐私等级最高，约束与 `AgentTraceCollector` 同源。

**为什么不塞进 `tech-selection-draft.md`**：读者冲突。选型草稿的读者是「正在做某一刀的你和 Agent」，靠 `confirmed/planned/rejected` 状态标记工作，且随实现频繁改动；叙事文档的读者是听故事的人。分层读者是本仓库文档体系的既有原则。

---

## 11. 冻结时同步的引用

**活文档：** 本文、`AGENTS.md`、`openspec/project.md`、`.kiro/steering/{rules,structure,product,tech}.md`、`Docs/agent-iteration/README.md`、`architecture/{README,agent-architecture-constitution,tech-selection-draft}.md`、`workflow/agent-control-model.md`、`roadmap/README.md`。

**不得修改（归档即历史）：** `openspec/changes/archive/**` 中六份 proposal 的「上游方向 v1.1」引用——当时确实依据 v1.1，改动等于篡改归档。

---

## 12. 修订记录

| 版本 | 日期 | 状态 | 说明 |
|---|---|---|---|
| v1 | 2026-07-26 | 草案 | 初版蓝图，基于 grill-me 讨论编写 |
| v1.1 | 2026-07-27 | 草案修订 | M4 真相对齐、依赖去歧义、C1 最小护栏 + C4 hardening、C3 拆分退路 |
| v1.1 | 2026-07-27 | **已冻结** | 批准 C1–C5 方向；P1–P7 保留至各 change design |
| v1.1 | 2026-07-28 | 顺序调整 | **C4 前移至 C3 之前**（C2 闸门 3 取得气质越界实证：工具参数增写用户从未说过的整句）。执行序变为 C1 → C2 → C4 → C3 → C5 |
| v1.1 | 2026-07-29 | C3 拆分登记 | C3 `agent-memory-and-review` 依 §4 拆分退路拆为 `agent-memory-retrieval`（C3a）+ `agent-review-chat`（C3b）。同时登记事实修正：`record.ai_summary` 由前端回传写入，后端不自动生成，覆盖率为 `unknown`，检索设计改为不依赖单一字段 |
| **v1.2** | **2026-07-30** | **已冻结** | 校准会十问定案：① Phase 2 以能力叙事为主驱动（D25）② 「前沿」限定 Agent 层，平台升级降 Optional C0（D26）③ 不引入图框架，改为引入受控环 + 可讲述 ADR（D27）④ 新增 C7 `agent-reflection-loop`，序列定为 **C6→C7→C8→C9**，编号顺移（D30）⑤ 反思环判定源复用 C4、指令只回传违规类型、两类开环、上限 1 次（D28/D29）⑥ Eval 用例混合载体 + 入库/本地双份样本 ⑦ Eval 不做 Judge、只做不变量 + 回归比对（D31/D32）⑧ 快照分层 + `baselineNote` 防橡皮图章 ⑨ 新增叙事交付物并作为每刀固定收尾项（D33）⑩ 冻结拆两步执行。**同时消化五条实测证伪的前提（§2.3）**，新增「知道但不做」清单（§9）。Phase 1 C1–C5 全部归档成为事实，534 tests PASS |
