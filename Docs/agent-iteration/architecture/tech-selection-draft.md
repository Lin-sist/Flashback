# Flashback Agent 技术选型（v0.2·已校准）

> 文档性质：选型事实 + ADR 式推荐 + 演进路径  
> 状态日期：2026-07-30  
> 状态：**v0.2——已按 Phase 1 全部归档事实校准**（随蓝图 v1.2 冻结同步）  
> 不授权改业务代码；具体 change 的最终选型以该 change 的 `design.md` 决策记录为准  
> 配套：`agent-architecture-constitution.md`、`roadmap/iteration-blueprint.md`（v1.2 已冻结）

---

## 0. 怎么用这份文档

| 读者 | 用法 |
|---|---|
| 正在做 C6 的你 / Agent | Eval 形态对照 §3.8（**注意：Judge 已由 D31 否决，§3.8 已更新**） |
| 正在做 C7 的你 / Agent | 反思环边界对照 §3.5 与 §3.10；判定源复用 C4，不新起自检器 |
| 做 C8 / C9 时 | 韧性对照 §3.9（须扣除 C7 已占用预算）、时间智能对照 §3.11 |
| 未来想换框架时 | 先读 §1 原则与 §5 不选清单，再决定是否真值得 |

### 状态标记

- `confirmed`：已在主干代码/配置落地  
- `partial`：有雏形，未系统化  
- `planned`：蓝图/草案中，未实现  
- `deferred`：有意后置  
- `rejected`：已否决，需强理由才能翻案  

---

## 1. 选型总原则（防推倒）

1. **契约稳定，实现可换**：优先 OpenAI-compatible messages/tools 形状、内部 Port，而不是某编排框架版本。  
2. **产品语义留在自研层**：阶段机、确认写操作、共情边界——框架替代不了。  
3. **先 MySQL 真相源，再考虑旁路索引**：日记场景要隔离、可审计、少移动数据。  
4. **可测性优先于演示性**：无单测/无用例的「智能」默认不进主线。  
5. **一条演进缝**：每个易变点只暴露一个接口（MemoryPort、LlmPort…）。  
6. **拒绝静默假成功**：FC 不可用就纯对话；真实路径禁止 mock success。  

---

## 2. 现状快照（Phase 1 收官后，已按代码校准）

> 本表已于 2026-07-30 以代码为准重写，不再是 v1.1 意图卡片的转述。

| 主题 | v1.1 表述倾向 | 当前主干事实（2026-07-30） | 标记 |
|---|---|---|---|
| 执行顺序 | 默认 C1→C2→C3→C4→C5 | 实际 **C1→C2→C4→C3a→C3b→C5**（C4 前移 + C3 拆两刀，均已登记） | confirmed |
| AI 接口层 | 分析文档常写 Spring AI ChatClient | **自研 `AgentModelClient`**（`java.net.http.HttpClient` + OpenAI-compatible `/chat/completions`） | confirmed |
| Runtime | 待选 AgentFlow4J / LangGraph4J / 自建 | **自建** `AgentStageMachine` + Service 编排 | confirmed |
| 会话持久化 | P2 候选含 Redis/内存 | **MySQL 会话/消息表**（MyBatis） | confirmed |
| Tool | FC + 可能 prompt 模拟降级 | **原生 FC + model 白名单**；不可用则纯对话，**无自研提议协议降级** | confirmed |
| Guardrails | P6 待选 | **规则源 + 内容检查 + 忠实度双指标 + 降级**（`agent/guardrail`） | confirmed |
| Memory | C3 planned | **`MemoryPort` + `MySqlMemoryPort`**：关键词 + 标签 + 时间回溯；注入有条数/字符预算（P4 关闭：LIKE + 标签，非 FULLTEXT） | confirmed |
| 回看会话 | C3 planned | **复用 `agent_session` + `purpose`**，不经阶段机；前端 `ReviewChatSheet` 浮层（P5 关闭） | confirmed |
| Observability | C5 planned | **`agent/trace/*` + `agent_turn_trace` 表**；内容哈希版本锚点；9 条排查查询（P7 关闭：MySQL 表而非 JSON 文件） | confirmed |
| Eval | 未提 | `AgentGuardrailBoundaryCaseTest` 为离线黄金集雏形；无跨维度 runner | partial → C6 |
| Resilience | 未提 | 有超时与 fail-open 兜底；无错误分类 / 阶段化降级 / 多 provider | partial → C8 |
| 认证 | 分析文档曾写 Spring Security | **jjwt + 自研过滤器**；`springframework.security` 全仓零匹配，pom 无 security starter | confirmed |
| Redis | pom 含 starter | dev/prod yml 有配置段，但 **main 代码零消费**（会话走 MySQL） | partial |
| Provider | DeepSeek / compatible | `app.ai.provider`: mock / deepseek / openai-compatible；Agent 复用同一 secret 通道 | confirmed |

**漂移解释：** 上述偏离不是失败，而是 Phase 1 正常收敛。本表已按 v1.2 冻结要求把「意图」改成「事实 + 仍有效的下一步」。

---

## 3. 分层选型

### 3.1 L0 平台

| 决策点 | 推荐 / 现状 | 状态 | 演进 |
|---|---|---|---|
| 后端 | Spring Boot + MyBatis + MySQL | confirmed | 保持；禁止为 Agent 换语言栈 |
| 前端 | Uniapp + Vue3 微信小程序 | confirmed | 对话 UI 增量，不做 major 视觉重建 |
| 配置 | `app.ai.*` + `app.agent.*` | confirmed | Agent 不新增凭证字段 |
| 对象存储 | 既有 S3 兼容配置（记录附件） | confirmed | Agent 不直接持有 AK/SK 新通道 |

### 3.2 L1 Runtime / Loop

| 决策点 | 选择 | 状态 | 理由 |
|---|---|---|---|
| 编排范式 | **Loop Engineering（自研状态机）** | confirmed | 单 Agent、可控、与阶段语义契合 |
| 阶段模型 | OPENING→…→ENDED 显式枚举 | confirmed | 可测、可限轮次 |
| 上下文窗口 | 滑动消息窗口 + 草稿摘录 cap（配置项） | confirmed | Context 预算雏形 |
| Graph / 多 Agent 框架 | **不引入** | rejected | 产品隐喻与复杂度不匹配 |
| 检查点 | DB 会话状态 + 消息持久化 | confirmed | 满足中断恢复；非工作流引擎级 durable execution |

**ADR-R1：为什么不把 Runtime 迁到 LangGraph4J / AgentFlow4J？**

- 收益：通用节点图、社区示例  
- 成本：重写已测状态机、阶段语义失配、依赖升级风险  
- 结论：保持自研 Loop；若未来有复杂分支，优先在现有 Service 内扩展，而不是整图迁移  

### 3.3 L2 Tools

| 决策点 | 选择 | 状态 | 理由 |
|---|---|---|---|
| 协议 | Provider 原生 function calling | confirmed | 行业主路 |
| Schema | 内部 `AgentToolSchemaFactory` + 校验器 | confirmed | strict 无法表达的约束代码层补 |
| 可用性门控 | `function-calling-models` 白名单 | confirmed | 不假设任意 compatible model 都支持 FC |
| 失败策略 | 退回 C1 纯对话 | confirmed | 禁止假协议 |
| MCP | 不作为内核 | deferred | 需要对外工具生态再加 adapter |
| 写操作 | 确认后执行（产品规则） | confirmed | HITL 底线 |

**ADR-R2：MCP 现在做不做？**

- 2026 工具互操作层 MCP 已成事实标准，但 Flashback 工具是**内生业务 API**（草稿、标签等）  
- 现在 MCP 化 = 多一层转换、安全面扩大，无外部消费方  
- 结论：ToolRegistry 保持内部契约；MCP 列为 **可选适配层**，不进 C3–C5 主线  

### 3.4 L3 Memory / Context（C3 焦点）

| 决策点 | Phase 1 推荐 | 状态 | 反淘汰演进 |
|---|---|---|---|
| 语义分层 | Working / Episodic / Semantic 术语对齐 | confirmed | 术语与端口已立住 |
| 真相源 | MySQL 用户记录 + 既有结构化摘要 | confirmed | 不迁数据湖 |
| 检索 v1 | **关键词 + 标签 + 时间回溯窗口的 LIKE 检索**（P4 已关闭：未用 FULLTEXT） | confirmed | 可解释、可测；相关性弱是已知短板（R9） |
| 检索 v2 | 同 `MemoryPort` 下增加 embedding 旁路 | deferred | 不绑死单一向量厂商 |
| 注入 | 条数 + 字符预算；带 recordId/时间 | confirmed | 供护栏与 **C9** 时间智能 |
| 向量中台 / GraphRAG | **不做** | rejected | D7 + 叙事分工 |
| 友人回看 | **`ReviewChatSheet` 浮层 + `agent_session.purpose`**（P5 已关闭） | confirmed | UI 增量，未改三 Tab |

**ADR-R3：Memory 是否上向量库？——否（已执行）**

- 记录规模与产品阶段用 SQL 检索足以验证「记得你」这个产品命题
- 向量作为 **v2 插件**：同一 `MemoryPort` 下加旁路，Controller / Agent 不感知
- 提前上云向量库会让隐私、成本、本地开发与 Eval 夹具全部变重
- **C3a 实测补充**：本地 `tag` 表 0 行、`core_question` 0% 非空、`ai_summary` 覆盖率约 62%——
  检索设计因此**不依赖任何单一字段**。这条事实也说明「先上向量」在数据稀疏时同样无解

**ADR-R4：历史原文与忠实度来源——已关闭（P-F，C3a 定案）**

原有三个候选（A 历史不算源 / B 历史算源 + 时间锚点 / C 分模式）中，**采用 B**：

- 命中的历史片段进入忠实度合法来源（`AgentLayeredCorpus` 分层承载）
- 同时新增 `MISSING_TIME_ATTRIBUTION` 护栏强制时间锚点

选 B 而非 A 的理由：若维持「仅当前会话」，合法的「我记得你三月份也写过」会被当成 Agent 增写而误伤，
Memory 的产品故事与护栏直接打架。选 B 而非 C 的理由：规则分叉会让 Eval 需要双套用例。

**代价（已接受）**：忠实度来源集合变大，理论上放宽了拦截面。缓解是分层 corpus——
复述部分与 Agent 新增表述分区判定，诊断 / 代决的判断不会因为来源变大而失准。

### 3.5 Guardrails（已落地基线）

| 决策点 | 选择 | 状态 |
|---|---|---|
| 形态 | 规则声明源 + 后置检查 + 分路径降级 | confirmed |
| 忠实度 | 双指标（覆盖比例 + 最长连续未覆盖片段）+ 阈值可配置；引号片段严判 | confirmed |
| violation 集合 | `UNFAITHFUL` / `FABRICATED_QUOTE` / `DIAGNOSTIC` / `FAKE_ACTION` / `MISSING_TIME_ATTRIBUTION` / `CHECK_ERROR` | confirmed |
| 降级分流 | 工具提议→拒绝；素材→丢弃；回复→本地常量兜底且留痕可区分 | confirmed |
| LLM 审核官 | **不做**（D28：延迟撞超时 + 判定不可复现 + 换掉确定性 checker 是净损失） | rejected |
| 与 Trace | `AgentGuardrailVerdict` 已是脱敏形态，可直接落轨迹，不需第二套摘要机制 | confirmed |
| 与 C7 反思环 | **本端口的 verdict 即判定源**；重写指令只回传 violation 类型（D29） | planned → C7 |

阈值类数字（如 min-coverage）以 **实测标定** 为准，允许 change 内修订，须留证据。

**为什么忠实度要两个指标**：单看覆盖比例会漏掉「大部分照抄、中间塞一句编造」；
单看最长未覆盖片段会误伤正常改写。两个一起才拦得住 R1 那类真实样本。

### 3.6 Llm / Provider

| 决策点 | 选择 | 状态 | 演进 |
|---|---|---|---|
| 协议 | OpenAI-compatible Chat Completions + tools | confirmed | 长期主路 |
| 客户端 | 自研 `AgentModelClient` | confirmed | 可内部重构；保持请求形状 |
| Spring AI | **不强制引入** | deferred | 仅当明显降复杂度再评估 |
| 模型 | 配置化；FC 白名单（如 deepseek-v4-*） | confirmed | 换模型 = 配置 + 回归 |
| Embed | 未建 | deferred | 随 Memory v2 |
| 多 provider 路由 | 非系统化 | partial → **C8** | Primary + 模板兜底可先做 |
| 实测耗时 | min 4571 / avg 6476 / max 8467ms（C5 首次取得） | confirmed | 它是 C7 环预算与 C8 超时策略的定量基础 |
| 超时配置 | 前端 30s / 后端 20s，**顺序不可颠倒** | confirmed | 相等时前端先断，后端显式失败语义会被网络层错误覆盖 |

**ADR-R5：要不要迁移到 Spring AI？——不迁（D26 强化）**

- 契约已经是 industry shape（OpenAI-compatible messages + tools）；Spring AI 是实现便利，不是能力解锁
- **Spring AI 2.0 要求 Spring Boot 4.x + Framework 7**，迁移须先做平台代际升级（Optional C0），风险叠乘
- **更关键**：迁移会同时吃掉 C2 ToolRegistry、C3 `MemoryPort`、C4 护栏挂点、C5 轨迹采集点——
  这四处正是可讲述、可逆向学习的自研资产
- 迁移窗口：仅当多模态 / 流式 / 重试 / 观测中间件的自维护成本明显超过迁移成本
- 在此之前：**禁止**「为了选型看起来新」而整包替换

### 3.7 Observability（C5，已落地）

| 决策点 | 实际选择 | 状态 |
|---|---|---|
| 事件模型 | 自研 versioned schema；一轮内的步骤先收内存，由唯一出口落库 | confirmed |
| 存储 v1 | **MySQL `agent_turn_trace` 表**（P7 关闭；未用 JSON 日志文件） | confirmed |
| 采样 | **默认全量不采样**（已批准偏离原方案） | confirmed |
| 版本锚点 | `prompt_version` / `policy_version` **由内容哈希派生**，改文案自动变化 | confirmed |
| 查询入口 | 9 条排查 SQL（`c5-trace-queries.sql`）；**未做 `/admin` 端点** | confirmed |
| 第三方 LLM obs | 可选，且必须脱敏 | deferred |
| OTel | 字段可映射即可，不强制上全链路 | deferred |
| 原文 | **永不入 trace**——收集方法只接受基础类型与既有枚举，编译期即挡住 | confirmed hard rule |
| 事务位置 | 落库在业务事务**提交之后** | confirmed |

**为什么没做 `/admin` 查询端点**：`AuthRole.ADMIN` 全仓无签发路径（`UserServiceImpl` 固定签 `USER`），
该路径下的端点在真实环境不可达。**任何未来 change 若要做 admin 端点，须先解决签发问题。**

**已为 C6 预留并落地的关联字段**：`trace_id`、`prompt_version`、`policy_version`、`model`、
`purpose`、`turn_no`、`attempt_no`、`outcome`、`provider_duration_ms`。
C6 的回归比对可按 `prompt_version` / `policy_version` 分组（`c5-trace-queries.sql` 第 7 条）。

### 3.8 Eval（C6，下一刀）

> **本节已按 v1.2 校准。**起草期曾写「再可选 LLM-as-Judge」，该选项**已由 D31 否决**，勿再引用。

| 决策点 | 选择 | 状态 |
|---|---|---|
| Runner | 仓内 JUnit 参数化 runner + 外置用例文件 | planned（P8 待定形态） |
| 用例载体 | **混合**：既有确定性护栏用例留 Java 不迁移；新增维度走 YAML | planned |
| 样本隐私 | 合成用例入库；真实样本走 gitignore 的 `local-samples.yaml`，缺失时静默跳过 | planned |
| 断言对象 | **轨迹级信号**（阶段序列、注入规模、护栏 verdict、降级层、长度比），不只看最终回复 | planned |
| 判定分层 | **不变量层**（硬失败，禁止刷新）+ **快照层**（需人确认，`baselineNote` 留痕） | planned |
| LLM-as-Judge | **否决**（D31）：原文外发需授权 + 预算个位数量级 + 判定不可复现 | rejected |
| 平台绑定 | 不绑死 DeepEval / 某 SaaS | confirmed 倾向 |
| 数据种子 | `AgentGuardrailBoundaryCaseTest` 五场景 + C3 误关联用例 | confirmed 可用 |
| mock 替身 | **不得改 `AgentMockResponder`**（`@Component`，mock provider 下在生产路径使用）；须另建可编排替身 | planned（P8） |

**必须自己说清的边界（D32）**：用 mock provider 跑评测，评的是**编排逻辑**，不是语言质量。
诚实表述为「Eval 覆盖轨迹不变量与回归比对；语言质量靠真实探针小样本人评锚定，
没有假装用 Judge 覆盖它」。

**为什么 C6 必须先于 C7**：反思环本质是改模型输出行为，而宪法 §7.3 禁止「无 Eval 情况下大改 prompt 上线」。
且本项目已有一次「主观感受被延迟污染」的实证（R2），先建量尺才能避免第二次。

### 3.9 Resilience（**C8**，预研)

> 编号说明：v1.2 在 Eval 之后插入反思环占 C7，韧性由 C7 顺移至 **C8**。

| 决策点 | 推荐分期 | 状态 |
|---|---|---|
| v1 | 错误分类、超时策略、**温暖模板**按阶段降级 | planned |
| v2 | 多 provider 切换 | deferred |
| v3 | 语义缓存 | deferred（冷启动价值有限） |
| 语义熔断 | 可先复用护栏越界 / 死循环 tool 计数 | planned |
| **调用预算约束** | **须扣除 C7 反思环已占用的部分**（P14） | planned hard constraint |

情感场景优先 **「用户感觉 Agent 话少」** 而不是 **HTTP 错误页**。

**与 C7 的硬耦合**：C7 最坏 2 次调用 ≈13s（按 avg 6476ms 推算），后端上限 20s。
C8 若再加重试就会叠加爆表，**design 必须把 C7 预算作为输入约束**，不得等开工才发现。

### 3.10 Reflection（**C7**，v1.2 新增）

| 决策点 | 选择 | 状态 |
|---|---|---|
| 判定源 | **复用 C4 确定性护栏 verdict**，不新起 LLM 自检器（D28） | planned |
| 重写指令 | **只回传 violation 类型**映射的固定改写要求，不携带候选文本片段（D29） | planned |
| 开环范围 | 仅 `UNFAITHFUL` 与 `MISSING_TIME_ATTRIBUTION`；`CHECK_ERROR` **绝不开** | planned |
| 预算 | **最多重写 1 次**；仍违规则走现有降级，行为与今日一致 | planned |
| 超时影响 | 最坏 ≈13s < 后端 20s，**不需要动超时配置** | planned |
| 落点 | 抽出独立协作者（如 `AgentReplyPipeline`）以掉转「检查 → 生成」的依赖方向 | planned |
| 图框架 | **不引入**（D27）：现有阶段机已具备节点/边/抢占/自环/终态 | rejected |
| 精细诊断载体 | **不做**：靠自觉维持隐私边界不可靠，配约束测试成本反超收益 | rejected |

### 3.11 Temporal（**C9**，预研）

| 决策点 | 推荐 | 状态 |
|---|---|---|
| 依赖 | C3a/C3b 的 Memory 元数据（时间戳、摘要主题） | confirmed 可用 |
| 能力 | 时间距离话术、衰减注入、克制周期提及 | planned |
| 禁止 | 情绪 % 趋势、诊断、dashboard | rejected |
| 实现位置 | L3 策略模块，而不是新前端分析页 | planned |
| 前置 | C6 的克制维度须已能约束「时间话术不滑向分析」 | planned |

---

## 4. 默认技术栈（Phase 1 已落地 + Phase 2 方向）

```text
Language/Runtime : Java 17 · Spring Boot 3.3.5 · MyBatis · MySQL 8.0   ✅
Auth             : jjwt + 自研过滤器（非 Spring Security）              ✅
Agent Loop       : 自研 AgentStageMachine + MySQL session checkpoint    ✅
LLM I/O          : OpenAI-compatible HTTP（AgentModelClient）           ✅
Tools            : Native function calling + allowlist + confirm        ✅
Memory           : MemoryPort · MySQL 关键词/标签/时间检索 · 注入预算   ✅
Guardrails       : 规则源 + 确定性检查 + 忠实度双指标 + 分路径降级      ✅
Trace            : versioned event schema · MySQL 表 · 无原文           ✅
Eval             : 仓内 runner + 轨迹不变量 + 快照回归（C6）            → 下一刀
Reflection       : 护栏驱动的受控环，上限 1 次（C7）                    → 待 C6
Resilience       : 错误分类 + 阶段化温暖降级 → 多 provider（C8）        → 待 C7
Temporal         : 时间元数据策略（C9）                                 → 待 C8
UI               : Uniapp 增量对话入口（未改三 Tab）                    ✅
Platform upgrade : Spring Boot 4.x / Java 21（Optional C0）             → 证据触发
```

**行业对齐标签（叙事用，不等于引入同名框架）：**

- Loop Engineering  
- Tool Augmented Generation  
- Multi-layer Memory  
- Context Engineering  
- Guardian / policy checks  
- Agent Trace + Eval loop  
- **Reflection / Self-correction**（C7 引入）  

---

## 5. 明确不选（Rejected / 高门槛）

| 项 | 状态 | 原因 |
|---|---|---|
| Hermes / 长驻自改进 Agent | rejected | 被动召唤与可预测气质冲突；栈不匹配 |
| Multi-Agent / A2A 主架构 | rejected | 无产品需求；隐喻破坏 |
| 企业向量中台 / 完整第二 RAG | rejected | D7；与 RAG 项目分工 |
| 自研假 FC 协议 | rejected | C2 已否决 |
| 情绪诊断 dashboard | rejected | AGENTS 禁止 |
| Voice / STT Agent | rejected | AGENTS 禁止 |
| 主动推送陪伴 | rejected | 被动召唤 |
| 为 MCP/Spring AI/LangGraph 整包重写 | rejected default | 违反宪法 P9；无强制业务收益 |
| **图框架（LangGraph4j 等）** | rejected | D27：现有阶段机已具备节点/边/抢占/自环/终态；框架增量收益是可视化（不需要）与 checkpoint（已有） |
| **把阶段机重构成 `Node`/`Edge` 而行为不变** | rejected | 给概念找地方贴；零收益、满风险、讲不出东西 |
| **LLM-as-Judge（C6 范围内）** | rejected | D31：原文外发需授权 + 预算不足 + 判定不可复现 |
| **LLM 自检器（C7 范围内）** | rejected | D28：生成+自检+重写 ≈19.5s 撞死 20s 超时；用不确定换确定是净损失 |
| **带文本片段的精细诊断载体** | rejected | 隐私边界靠自觉不可靠；配约束测试成本反超收益 |

翻案条件：独立 Type C proposal + 用户批准 + 证明现有端口无法演进。

**这份清单同时是叙事资产**：面试会考「什么时候某种编排是多余的复杂度」，
成体系的「知道但没做」比「都做了」更能说明判断力。对外表述见 `narrative/agent-tech-story.md` §9。

---

## 6. 实现漂移登记表

> 每发现与本文假设不一致且**有意保留**的实现，追加一行。蓝图版本升级时消化本表。
> **Phase 1 部分已于 v1.2 冻结时消化完毕。**

| 日期 | Change | 原假设 | 实际选择 | 原因 | 已消化 |
|---|---|---|---|---|---|
| 2026-07-28 | C4 | 默认 C3 后 hardening | C4 前移至 C3 之前 | 工具参数增写用户从未说过的整句（闸门 3 实证） | 是（v1.2 §8） |
| 2026-07-28 | C1–C2 | 或用 Spring AI | 自研 `AgentModelClient` | 边界清晰、少依赖、可讲述 | 是（ADR-R5） |
| 2026-07-29 | C3 | 单刀 `agent-memory-and-review` | 拆为 C3a + C3b | 改动面几乎不重叠，合并会产出互相阻塞的 tasks | 是（v1.2 §2） |
| 2026-07-29 | C3a | 检索用 FULLTEXT | 关键词 + 标签 + 时间回溯的 LIKE | 本地数据稀疏（`tag` 0 行、`core_question` 0% 非空），全文索引无从发挥 | 是（P4 关闭） |
| 2026-07-29 | C3a | `record.ai_summary` 可作检索索引 | 不依赖任何单一字段 | 该字段由前端回传写入，后端不自动生成，覆盖率约 62% | 是（v1.1 §7） |
| 2026-07-30 | C5 | Trace 存 JSON 日志文件 | MySQL `agent_turn_trace` 表 | 可按 session 聚合查询，与既有 MyBatis 栈一致 | 是（P7 关闭） |
| 2026-07-30 | C5 | 采样降低开销 | 默认全量不采样 | 单用户量级下开销可忽略，采样会让排查漏样本 | 是（已批准偏离） |
| 2026-07-30 | C5 | `REQUIRES_NEW` 隔离轨迹写入 | 事务提交后回调 | `REQUIRES_NEW` 不释放外层锁，经外键争 `agent_session` 父行锁 → 每轮卡满 50 秒 | 是（v1.2 §8） |
| 2026-07-30 | C5 | 新表须同步三份 schema | 只维护增量脚本 | `schema.mysql.sql` 只到 C1，项目既有约定是全量脚本不随增量维护 | 是（v1.2 §2.3） |
| 2026-07-30 | C5 | `/admin` 端点作查询入口 | 9 条排查 SQL | `AuthRole.ADMIN` 全仓无签发路径，该端点在真实环境不可达 | 是（v1.2 §2.3） |
| 2026-07-30 | 校准会 | 认证基于 Spring Security | jjwt + 自研过滤器 | steering 表述与代码不符（全仓零匹配） | 是（v1.2 §2.3） |

---

## 7. 待确认清单（跨 change）

**仍 open（按决策时机排序）：**

| ID | 事项 | 决策时机 | 状态 |
|---|---|---|---|
| P8 | Eval 可编排 mock 替身形态（不得改 `AgentMockResponder`） | C6 design | open |
| P13 | 反思环与轨迹 `attempt_no` 的关系（重写是否算新 attempt） | C7 design | open |
| P14 | C8 可用的超时预算（须扣除 C7 已占用部分） | C8 design | open |
| P10 | 语义缓存是否值得做 | C8 design | open |
| P11 | 备选 provider | C8 | open |
| P12 | Temporal 最小记录数阈值 | C9 | open |
| P15 | Optional C0 的触发条件与验收形态 | C0 proposal（若开） | open |

**已关闭（勿回退，除非新 change 显式翻案）：**

| ID | 事项 | 结论 | 关闭于 |
|---|---|---|---|
| P1 | Provider FC 支持 | 原生 FC + model 白名单；不可用退回纯对话，无假协议 | C2 |
| P2 | 会话持久化 | MySQL 表（非 Redis / 内存） | C1 |
| P3 | Tool 白名单范围 | 草稿 / 标签 / 正文追加等，写操作须确认 | C2 |
| P4 | Memory 检索 | 关键词 + 标签 + 时间回溯 LIKE（非 FULLTEXT） | C3a |
| P5 | 友人回看 UI | `ReviewChatSheet` 浮层 + `purpose` 区分 | C3b |
| P6 | Guardrails 实现方式 | 规则源 + 后置确定性检查 + 分路径降级 | C4 |
| P7 | Trace 存储 | MySQL `agent_turn_trace` 表，默认全量 | C5 |
| P-F | 忠实度是否纳入历史来源 | **方案 B**：历史入源 + `MISSING_TIME_ATTRIBUTION` 强制时间锚点 | C3a |
| P9 | Judge 是否独立 provider | **不做 Judge**（D31） | v1.2 校准会 |

---

## 8. 修订记录

| 版本 | 日期 | 说明 |
|---|---|---|
| v0.1 | 2026-07-28 | 初稿：锚定 C1/C2/C4 事实；为 C3/C5/Phase2 留演进缝；强调 C5 后校准冻结 |
| **v0.2** | **2026-07-30** | 随蓝图 v1.2 冻结校准：① §2 现状快照按代码重写（Memory / 回看 / Observability 转 confirmed，新增 Eval / Resilience / 认证 / Redis 四行）② §3.4 关闭 P4/P5 并补 C3a 数据稀疏实测 ③ **ADR-R4 关闭**（P-F 采用方案 B，附代价说明）④ §3.5 补 violation 集合与降级分流，LLM 审核官转 rejected ⑤ §3.6 补实测耗时与超时顺序；**ADR-R5 强化**（Spring AI 2.0 需 Boot 4.x + 会吃掉四处自研资产）⑥ §3.7 全表转 confirmed，登记 `/admin` 不可达与已落地关联字段 ⑦ **§3.8 删除「可选 LLM-as-Judge」**（D31 否决），改为不变量 + 快照分层 + P8 mock 替身约束 ⑧ 新增 **§3.10 Reflection（C7）**；韧性顺移 §3.9/C8、时间智能顺移 §3.11/C9 ⑨ §4 默认栈标注落地状态 ⑩ §5 新增五项 rejected ⑪ §6 漂移表补齐 Phase 1 全部九行并标记已消化 ⑫ §7 拆为 open / closed 两表 |
