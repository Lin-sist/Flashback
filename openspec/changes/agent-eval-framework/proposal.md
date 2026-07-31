# Agent Eval Framework（C6）

> Change ID：`agent-eval-framework`
> Type：**C**
> 阶段：**闸门 1 已批准 + 闸门 2 已授权（2026-07-31，N1–N8 按推荐定稿）；实现进行中**
> 开工锚点：`486ca95`
> 上游方向：`Docs/agent-iteration/roadmap/iteration-blueprint.md` **v1.2 已冻结** §4.2（C6 意图卡片）+ §9.1（不做 Judge）
> 架构约定：`Docs/agent-iteration/architecture/agent-architecture-constitution.md` §3.6 `EvalPort`、§7.3
> 前置：Phase 1（C1 / C2 / C4 / C3a / C3b / C5）全部归档，delta 均已接受进 baseline
> 回归基线：后端 **536 tests PASS / 4 skipped**（4 skipped 为环境门控的真实 provider / MySQL 探针）
> —— 实现期实测修正：`ACTIVE_TASK` 与蓝图记的「534 / 3」是 C5 归档当时的值，
> 三个 Type B 之后实际为 536 / 4（见 E37）

---

## 1. Why Now

Phase 1 结束时，Agent 有 Loop、有工具、有护栏、有记忆、有轨迹。缺的是**量尺**。

这不是抽象的缺失，它已经造成过一次具体的误判：

R2「引导话术生硬」是 C1 之后一直挂着的残余。修 C5 引入的 50 秒自锁之后，用户手验反馈「自然一些了」——而那一轮**一行 prompt 都没改**。之前的判断样本受 55 秒延迟污染，「话术生硬」的感觉里混着「等太久了」的感觉，没人察觉。蓝图 §2.3 第 4 条已把它登记为被实测证伪的前提，并定案由 C6 重建基线（D30/D32）。

**这件事的教训不是「手验不可靠」，而是「没有量尺时，连自己在测什么都不知道」。**

同一个理由决定了 C6 必须先于 C7。反思环的价值主张是「重写后质量更好」；没有量尺，这个主张只会得到第二个无法证伪的「感觉好了」。架构宪法 §7.3 把「无 Eval 情况下大改 prompt 上线」列为默认禁止的推倒式举动，而反思环本质上就是在改模型输出行为。

**为什么现在做是安全的。** 按下面的方案，C6 **不改任何 main 代码**——既有 22 个协作者全部构造注入（E8），测试可以整套替换替身，不需要为可测性动生产代码。它也不新增依赖、不改 pom。风险集中在三处，都在下面明确列出：一是样本隐私（真实样本不得入库），二是快照沦为橡皮图章，三是「用 mock 评质量」的自欺——第三条是本刀最容易在面试里被问穿的地方，必须在文档里先把界划清。

---

## 2. 现状事实（能力五态）

> 事实来源（均已核对代码，标注行号者为直接读取）：
> `backend/src/test/java/com/flashback/agent/guardrail/AgentGuardrailBoundaryCaseTest.java`、
> `backend/src/test/java/com/flashback/service/impl/AgentGuardrailTraceCorrelationTest.java`、
> `backend/src/main/java/com/flashback/agent/AgentMockResponder.java`、
> `backend/src/main/java/com/flashback/agent/AgentModelClient.java`、
> `backend/src/main/java/com/flashback/agent/trace/*`、
> `backend/src/main/java/com/flashback/service/impl/AgentChatServiceImpl.java`、
> `backend/src/main/java/com/flashback/config/AppAgentProperties.java`、
> `backend/src/main/java/com/flashback/agent/AgentStageMachine.java`、
> `backend/pom.xml`、`backend/src/test/resources/`、`.gitignore`

### 2.1 既有黄金集资产

| # | 事实 | 状态 |
|---|---|---|
| E1 | `AgentGuardrailBoundaryCaseTest` **219 行 / 15 个 `@Test` / 5 个 `@Nested` 场景组**（诊断输入、注入尝试、过长输出、代决尝试、R1 真实样本回归），每组含正例与反例。**纯 JUnit，无 Spring、无 Mockito、零 provider 调用** | `confirmed` |
| E2 | 隐私断言已存在：`verdictMetricsMustNotLeakContent`（L211–218）。**但它是负向子串断言**（`doesNotContain("软件工程", "后端", ...)`），不是结构化格式校验——换一批用例文本就守不住了 | `confirmed` |
| E3 | 用例输入全部是**内联字符串字面量**。全仓 `@ParameterizedTest` / `@MethodSource` / `@CsvSource` / `@ValueSource` / `@EnumSource` **零命中**——C6 会是本仓库第一次使用参数化测试 | `confirmed` |
| E4 | `AgentGuardrailVerdict.metrics()` 的格式是 `coverage=%.3f maxUncoveredRun=%d checkedLength=%d`，**按构造只含数字**。→ C6 的隐私断言可做成**结构化格式校验**，比 E2 的子串断言强一个量级 | `confirmed` |

### 2.2 mock provider 能与不能

| # | 事实 | 状态 |
|---|---|---|
| E5 | `AgentMockResponder` 是 `@Component`、无接口、无字段、无 `Random` / `Clock` → **纯函数**，输出只由 `targetStage` + `userInput` 决定，快照天然稳定 | `confirmed` |
| E6 | **但它永远产不出违规**：`reply()` 返回六句写死的合规文案；`material()` 只拼接用户发言（coverage 恒为 1.0）；`toolCalls()` 只在 `CORE_QUESTION` 提议一次且 `text` 取用户原话。**mock provider 路径跑不出任何降级轨迹** | `confirmed` |
| E7 | 更关键的一条：**mock 分支根本不组装 prompt**。`generateReply` 在 `if (modelClient.isMockProvider())` 处直接返回，因此 mock 路径的轨迹里**永远没有 `prompt` 步骤**（这不是采集遗漏，C5 delta 的 scenario 已把它条件化）。→ **只走 `AgentMockResponder` 的评测天然评不到上下文组装维度** | `confirmed` |
| E8 | provider 选择**无 factory、无 `@ConditionalOnProperty`**，只是 `generateReply` 里一个内联 `if`；`AgentModelClient` 是**具体类**（非接口），但已被既有测试用 `@Mock` 整体替身 | `confirmed` |
| E9 | `AgentChatServiceImpl` 只有**一个 public 构造，22 个协作者全部构造注入** → 测试可整套替换，**不需要为可测性改 main 代码** | `confirmed` |
| E10 | **离线端到端范式已经存在，且正是 C6 需要的那个**：`AgentGuardrailTraceCorrelationTest`（在 `service/impl/`）用纯 Mockito 装配（**无 Spring、无 H2**）驱动完整 `sendMessage`，并用替身 checker 制造违规。它就是评测 harness 的种子 | `confirmed` |

### 2.3 C6 的断言对象（C5 产出）

| # | 事实 | 状态 |
|---|---|---|
| E11 | `AgentTraceCollector` 的 **19 个步骤类型**已就位：`mode` / `stage-decision` / `stage-retained` / `memory-retrieval` / `memory-injected` / `prompt` / `provider` / `provider-failed` / `provider-invalid-content` / `provider-unavailable` / `tools` / `tools-fail-closed` / `tool-rejected` / `guardrail` / `downgrade` / `reply-clipped` / `material` / `material-failed` / `session-ended`；全部 public 读取器（含 `steps()`）可用 | `confirmed` |
| E12 | `AgentTraceSink.persist(collector)` 是**唯一落库出口**且可被 `@Mock` 拦下 → **评测可直接断言内存中的收集器，不必落库** | `confirmed` |
| E13 | 落库路径也能离线验证：H2 `MODE=MySQL` + `src/test/resources/schema.sql` 已含 `agent_turn_trace`（L238–268）。**但** `persist` 走 `TransactionSynchronization.afterCompletion`，故 C5 的集成测试**刻意不能加 `@Transactional`** | `confirmed` |
| E14 | 版本锚点：`AgentTraceVersions.promptVersion()` = `"p"` + SHA-256 前 8 位（源自 `promptTemplateFingerprintSource()`）、`policyVersion()` = `"g"` + 同法；**刻意不缓存**，改文案即变 | `confirmed` |
| E15 | `backend/sql/mysql/c5-trace-queries.sql` 第 7 条已提供按 `prompt_version` / `policy_version` 分组比对 | `confirmed` |

### 2.4 测试基础设施现状

| # | 事实 | 状态 |
|---|---|---|
| E16 | junit-jupiter **5.10.5**（**含 params，参数化开箱可用，无需改 pom**）、assertj 3.25.3、mockito 5.11.0、h2；**surefire 无任何 `<configuration>`，零 excludes / 零 includes** | `confirmed` |
| E17 | **snakeyaml 2.2 在测试 classpath 上**（经 `spring-boot-starter` 传递）；**`jackson-dataformat-yaml` 不在**（pom 未声明，classpath 零命中） | `confirmed` |
| E18 | `backend/src/test/resources/` 目前**只有 2 个文件**（`application-test.yml`、`schema.sql`），**无 fixtures / golden 目录约定**——C6 会是第一个 | `confirmed` |
| E19 | 全仓**无任何 snapshot / approval 测试机制**：无 approvaltests 依赖、无 `.approved.*`、无 `.snap`、无 fixture 比对助手。C6 会是第一个 | `confirmed` |
| E20 | **仓库无 CI**：无 `.github/`，全仓 workflow 文件零命中，`*.yml` 只有 4 个 Spring 配置文件。→ 架构宪法 §3.6 写的「**CI 可跑子集**」当前**无处兑现** | `confirmed` |
| E21 | `.gitignore` **无通用 `*.local.*` 规则**；现有本地规则均为扩展名特化（`*.local.ps1` / `*.local.ps1.bak` / `/backend/*.local.env` / `/frontend/.env.*.local`）。**蓝图 §4.2 写的 `local-samples.yaml` 当前不被任何规则覆盖**，需新增 | `confirmed` |
| E22 | 放行惯例已存在可复用：`!/backend/secrets.local.env.example`、`!/backend/*.local.env.example` | `confirmed` |
| E23 | canonical 命令为 `backend` 目录下 `mvn -q test`；**无 maven wrapper** | `confirmed` |
| E37 | **回归基线实测为 536 tests PASS / 4 skipped，不是规划期沿用的 534 / 3。** 534 / 3 是 C5 归档当时的值；其后三个 Type B 又加了测试与一个探针（`C5MysqlTraceProbeTest`，随 `b6bcdd5` 进来），AGENT_LOG 里已记录 536 / 4，但 `ACTIVE_TASK` 顶部与蓝图 §2 的摘要没跟着更新。**按 §0.4「规划期判断实现期必须复核」登记，并已修正 `ACTIVE_TASK`** | `confirmed` |

### 2.5 断言可及性的三处硬边界（决定维度怎么写）

| # | 事实 | 状态 |
|---|---|---|
| E24 | **追问上限不是配置项**：`AgentStageMachine.MAX_REASK_PER_STAGE = 1` 是代码常量（L21）。→ 评测可断言它，**但不能按用例调它** | `confirmed` |
| E25 | **无聚合记忆字符预算配置**：只有 `memory.maxFragments=3` 与 `memory.maxFragmentChars=120`；轨迹的 `injectedChars` 是**实测值而非配置上限**。→「注入预算」不变量只能表达为 `injectedCount ≤ maxFragments` 且 `injectedChars ≤ maxFragments × maxFragmentChars`（**派生上限**，须在 spec 里如实这么写） | `confirmed` |
| E26 | 相关默认阈值：`maxReplyChars=120`、`maxTurnsPerSession=8`、`review.maxTurnsPerSession=6`、`guardrail.minCoverage=0.35`、`maxUncoveredRun=12`、`faithfulnessNgramSize=4`、`minMemoryOnlyRunForAttribution=8` | `confirmed` |
| E27 | `minMemoryOnlyRunForAttribution=8` 的代码注释**明写「未经真实样本校准」**；`minCoverage=0.35` 反之有真实样本校准记录。→ 前者是 C6 建好量尺**之后**才可能校准的遗留项，**本刀不校准任何阈值** | `confirmed` |

### 2.6 顺带发现的一处事实修正

| # | 事实 | 状态 |
|---|---|---|
| E28 | **`AgentChatServiceImpl` 现为 1274 行**，不是蓝图 §3.2 记的 1183 行（C5 之后增长了 91 行）。蓝图那个数字是 v1.2 校准会当时的实测值，现已过时。**C7 规划时须以实测值重新计算「与 Eval 合并的风险」论证**，不要沿用旧数字 | `confirmed` |

### 2.7 范围外

| # | 事实 | 状态 |
|---|---|---|
| E29 | LLM-as-Judge / 第二模型打分 | `out_of_scope`（D31，理由见 §5） |
| E30 | 绝对质量判分、A/B testing 框架、质量看板、商业评分 dashboard | `out_of_scope`（D32 + `AGENTS.md`） |
| E31 | 反思环（重写一次） | `out_of_scope`（C7） |
| E32 | 错误分类 / 降级模板 / 多 provider 路由 | `out_of_scope`（C8） |
| E33 | 时间距离话术 / 记忆衰减 | `out_of_scope`（C9） |
| E34 | R2 引导话术与素材合成质量的**实际优化** | `out_of_scope`——C6 只**重建可比对的基线**，优化本身留到基线就位之后（宪法 §7.3） |
| E35 | R9 检索相关性升级、阈值校准（含 E27） | `out_of_scope`（独立 change） |
| E36 | 快照指标在**真实 provider** 下的稳定性 | `unknown`——本刀不跑真实 provider（§9），故无法回答。**不得在文档里写成已验证** |

> **诚实性声明**：E7 与 E20 是本刀最重要的两条事实发现。
> E7 意味着「用 `AgentMockResponder` 跑评测」这条最直觉的路**天然缺一个维度**（上下文组装），且**跑不出任何降级轨迹**——这直接决定 N3 的答案。
> E20 意味着架构宪法写的「CI 可跑子集」在本仓库**没有落点**：C6 能交付的是「一条 maven 命令可跑」，**不是 CI 门槛**。这一点必须如实写进验收标准，不得含糊过去。
> E28 是对已冻结蓝图中一个数字的修正，按 §0.4「规划期判断实现期必须复核」的要求登记。

---

## 3. Goals

本刀 SHALL 实现：

1. **外置用例 + 参数化 runner**：用例输入从 Java 内联字面量移到数据文件，由一个参数化 runner 驱动，**离线零外调**。
2. **入库样本与本地样本双份输入**：合成样例入库供任何人复现；真实样本走 gitignore，**缺失时静默跳过**，同一 runner 两套输入。
3. **可编排替身**：能按轮次编排模型响应，从而覆盖 `AgentMockResponder` 产不出的路径——降级轨迹、上下文组装、工具提议被拒、provider 失败。**不改 `AgentMockResponder`**。
4. **八个维度**（蓝图 §4.2 表）：阶段推进正确性、追问克制、记忆三态可分、注入预算、护栏有效性、长度克制、工具 fail-closed、话术质量（回归型）。
5. **快照分层**：
   - **不变量层**——硬失败，**不允许刷新**。变了就是 bug。
   - **快照层**——失败 = 需人确认，可更新但必须留痕。
6. **防橡皮图章**：每个用例的快照带 `baselineNote`，记录该基线由哪一刀、哪个 `policyVersion` 定下；**改数字必须同步改说明**，diff 里一眼可见。
7. **不提供自动刷新开关**（见 N4）。
8. **隐私**：快照与报告 SHALL NOT 含任何用例输入文本；隐私断言做成**结构化格式校验**而非子串黑名单（E4）。
9. **R2 基线重建**：跑一次得到基线，使「改 prompt 前后可比对」第一次成立。
10. **零 main 代码改动**（见 §7 的范围守护）。

---

## 4. 用户故事

> C6 的「用户」是开发者，不是产品用户。

**改前**：改了引导话术或护栏阈值，只能手验读几条回复判断好坏。R2 的判断因此被 55 秒延迟污染而失效，且**没有任何机制会报警**——用户觉得变好了，实际一行 prompt 都没改。

**改后**：改动后跑一次 `mvn -q test`，得到两类结论：

- **哪些不变量被破坏**——阶段序列不合法、同阶段追问超过 1 次、注入超预算、回复超上限、该降级没降级、无工具模式没 fail-closed、指标里混进了文本。这些**直接判失败**。
- **哪些指标相对基线变了**——注入条数与字符、provider 调用次数、降级层、阶段路径、回复/输入长度比。这些**需要人看一眼**：如果是预期内的改动，更新快照并在 `baselineNote` 写清是哪一刀改的；如果不是，说明刚才那次改动有副作用。

于是 R2 的优化第一次有了「改之前长什么样」的定量记录。

---

## 5. Non-Goals（本刀明确不做）

- **不做 LLM-as-Judge**（D31）。三条具体理由：
  1. 日记原文送第二个模型打分属**未授权外发**，每跑一次评测都要走闸门 3；
  2. 真实调用预算在「6 次 / 预算 10」量级，Judge 跑几十条用例即爆表；
  3. 分数**不可复现**，违反架构宪法 §3.6。
  → 列为显式非目标，并在叙事文档 §9 记录「知道但没用」的理由。
- **不做绝对质量判分**（D32）：不给回复打 1–5 分，不做「共情度」数值化。
- **不做 A/B testing 框架、质量看板、商业评分 dashboard、外部 SaaS 接入。**
- **不改既有 `AgentGuardrailBoundaryCaseTest` 的任何断言**（蓝图 §4.2 明确要求原地保留）。
- **不改 `AgentMockResponder`**：它是 `@Component`，mock provider 下**在生产路径上使用**，改它会污染真实路径（这与 C5「不为可观测改行为」是同一条纪律）。
- **不改任何 main 代码**：不改 Agent 对话行为、prompt 文案、护栏阈值、记忆检索、工具白名单、回看逻辑、轨迹采集点。
- **不校准任何阈值**（含 E27 那条明标未校准的 `minMemoryOnlyRunForAttribution`）——建量尺与用量尺改参数是两件事，混在一刀里就分不清「指标变了」是因为改了阈值还是因为改了别的。
- **不做 R2 的实际优化**：本刀只重建基线。
- **不引入新依赖**：`pom.xml` / `package` / lockfile 不改（N1 的答案受此约束）。
- **不新增 DDL、不改数据库**。
- **不做前端任何改动**、不新增用户可见能力。
- **不引入 CI 配置**：仓库当前无 CI（E20），本刀**不顺手建**——那是独立决策，且撞 `AGENTS.md`「不改 deployment / monitoring」。
- **不把真实用户日记写入 tracked files**：真实样本只走 gitignore 路径。
- **不做 C7 反思环 / C8 韧性 / C9 时间智能。**

---

## 6. 场景边界（隐私 + 诚实性）

| 场景 | 期望行为 |
|---|---|
| 跑评测 | 全程离线，**零 provider 调用**，`mvn -q test` 内完成 |
| 合成用例文件缺失 | **硬失败**——入库用例是回归基线，缺了就是资产被误删 |
| 本地真实样本文件缺失 | **静默跳过**，不失败（它按设计不入库） |
| 某条不变量被破坏 | **测试失败，不提供刷新手段** |
| 某条快照指标变化 | 测试失败并打印「基线 vs 当前」，**须人确认后手工更新，并同步更新 `baselineNote`** |
| 只改了快照数字没改 `baselineNote` | **测试失败**——这正是橡皮图章的形态，须能被机械拦住 |
| 快照文件里出现用例输入文本 | **不允许**，须有测试直接断言 |
| 评测报告里出现日记原文 | **不允许**，同上 |
| 用例声称覆盖「语言质量」 | **不允许**——mock / 替身路径评的是编排逻辑；语言质量只靠真实探针小样本人评锚定 |
| 阈值被改动 | 快照指标随之变化属**预期**，但须在 `baselineNote` 里写明是哪次阈值改动 |
| 真实 provider 下的指标稳定性 | **本刀不回答**（E36），如实记为 `unknown` |

---

## 7. 范围守护：本刀为什么能做到零 main 改动

这是本刀最重要的风险控制手段，值得单列。

- 断言对象是 `AgentTraceCollector`，而它的**全部读取器已是 public**（E11），且 `AgentTraceSink.persist` 是唯一出口、可被 `@Mock` 拦下（E12）→ 拿到收集器不需要新增任何生产代码的 hook。
- 需要制造违规、失败、prompt 组装等路径时，替换的是 `AgentModelClient`（具体类，但既有测试已在 `@Mock` 它）与各 checker → 不需要为可测性抽接口。
- `AgentChatServiceImpl` 22 个协作者全部构造注入（E9）→ harness 可整套装配。
- 参数化测试与 YAML 解析所需的库**已在测试 classpath 上**（E16 / E17）→ 不改 pom。

**若实现期发现某处非改 main 不可，须停下请示**，不得自行扩大范围——这正是 C5 决策 5 那类「顺手改动必须披露」的场景。

---

## 8. 待用户在规划闸确认（N1–N8）

| # | 决策项 | 候选 | 我的推荐 |
|---|---|---|---|
| **N1** | 用例文件格式与解析方式 | (a) YAML + **直接用 snakeyaml**（已在测试 classpath，零 pom 改动）；(b) YAML + 新增 `jackson-dataformat-yaml`；(c) 改用 JSON，复用已有 jackson-databind | **(a)**。蓝图 §4.2 已把用例文件写成 `.yaml`，且 YAML 对含中文、多行、带注释的用例可读性明显好于 JSON——用例文件是要被人 review 的资产。(b) 会动 `pom.xml`，撞 `AGENTS.md`「不改 package 除非明确要求」，而收益只是一层 API 糖。**须披露的代价**：snakeyaml 是 `spring-boot-starter` 的**传递依赖**而非本项目直接声明，理论上未来某次 starter 升级可能移除它。缓解是加一条断言：解析器不可用时**明确失败**而不是静默跳过用例 |
| **N2** | 评测的执行基座 | (a) **纯 Mockito harness**（无 Spring、无 H2），断言拦到的收集器；(b) `@SpringBootTest` + H2，从 `agent_turn_trace` 读回；(c) 两者都做 | **(a)**。决定性理由有三条：① 既有范式已经存在且被验证过（E10 的 `AgentGuardrailTraceCorrelationTest` 就是这么干的），不用新造；② 快——评测要成为「每次改动都跑」的东西，Spring 上下文启动成本会让它变成没人跑的资产；③ **避开 E13 的坑**：`persist` 走 `afterCompletion`，走 DB 路线就必须处理「测试不能加 `@Transactional`」这条反直觉约束，而这对「断言编排信号」零增量价值。落库正确性已由 C5 的 `AgentObservabilityIntegrationTest` 覆盖，不需要 C6 重复 |
| **N3** | 可编排替身挂在哪一层（P8） | (a) **scripted `AgentModelClient` 替身**：stub `isMockProvider()=false` + 按轮次编排 `completeWithTools` 返回序列；(b) 改 `AgentMockResponder`；(c) 为 `AgentMockResponder` 抽接口再实现一个测试用编排版 | **(a)**。(b) 直接排除：它是 `@Component`，在生产路径上被使用（蓝图 §4.2 已明确禁止）。(a) 相对 (c) 有一个 (c) 拿不到的关键收益——**只有走「非 mock」分支才会组装 prompt**（E7），因此 (a) 能覆盖上下文组装维度与全部降级路径，而 (c) 仍然停在那个直接 return 的分支里，永远评不到 `prompt` 步骤。换句话说：(c) 看起来更「正统」（面向接口），实际覆盖面更小 |
| **N4** | 快照更新的操作方式 | (a) **只能手工编辑**快照文件，且必须同步更新 `baselineNote`（由测试强制）；(b) 提供 `-Deval.snapshot.update=true` 自动重写；(c) 自动重写 + 要求事后补 `baselineNote` | **(a)**。一个 `-Dupdate` 开关就是橡皮图章的最短路径——它会把「停下来想一下这个变化对不对」变成「跑一下命令让它变绿」。手工编辑的摩擦在这里**是特性不是缺陷**。(c) 的「事后补」在实践中等于不补。**代价**：用例多了以后批量更新会烦。缓解是失败信息里直接打印可粘贴的新值，人只需确认后粘贴并写一句说明 |
| **N5** | 本地真实样本文件的命名与 gitignore 形态（**轻微偏离已冻结蓝图**） | (a) 按蓝图原文命名 `local-samples.yaml` + 新增 `**/local-samples.yaml` 规则；(b) 改名为 `samples.local.yaml` + 新增通配 `*.local.yaml`，与仓库既有 `*.local.ps1` / `*.local.env` 命名习惯一致 | **(b)**，但因偏离蓝图写法须您裁决。理由是本仓库刚吃过这个教训：C5 把 `.gitignore` 从「只点名 `start-dev-wechat.local.ps1`」改成通配 `*.local.ps1`，正是因为点名单个文件时**后来新增的本地文件不会被覆盖**。(a) 会重犯同一个错——将来加第二个本地样本文件（比如按维度拆分）就又漏在外面，而漏出去的是**用户真实日记**。(b) 让「凡带 `.local.` 的样本文件一律不入库」成为规则而非逐个登记 |
| **N6** | 评测的运行入口形态 | (a) **就是普通 JUnit 测试类**，`mvn -q test` 全量跑时自动包含；(b) 单独 profile / 环境变量门控，默认跳过 | **(a)**。它离线、零外调、毫秒级，没有任何理由默认跳过；(b) 会让它变成「需要有人记得去跑」的资产，而本刀存在的全部意义就是不再依赖有人记得。**同时须如实记录**：仓库当前无 CI（E20），因此架构宪法 §3.6 的「CI 可跑子集」在本刀只能兑现为「一条 maven 命令可跑」，**不得写成已具备 CI 门槛** |
| **N7** | 「话术质量」维度本刀做到哪一步 | (a) **只建结构**：定义人评锚点文件的形状，入库空模板 + 说明，真实锚点样本走本地文件；(b) 本刀就填入真实锚点样本；(c) 本刀完全不碰这个维度 | **(a)**。(b) 需要真实 provider 产出 + 人评，属闸门 3 范围，而本刀申请 0 外调（见 N8）；(c) 会让蓝图 §4.2 的八维度表少一维，且「语言质量靠人评锚定」这句承诺没有落点。(a) 的产物是：结构就位、界划清楚、锚点待填——**并在文档里如实写明它是空的** |
| **N8** | 是否需要闸门 3（真实外调） | (a) **不需要，本刀预算 0**；(b) 申请 ≤6 次用于填 N7 的人评锚点 | **(a)**。C6 全程走替身与 mock，真实调用对「量尺是否正确」没有验证价值。人评锚点建议**顺带在 C7 的闸门 3 里做**——C7 必然要真实联调重写路径，那时同一批真实产出既验重写又能当锚点，比现在单独申请一轮更省。**若您希望本刀就把锚点填上，则选 (b)** |

### 附带需在规划批准时一并确认的事

- **无 DDL**：本刀不新增表、不改 schema，因此**不涉及**「DDL 必须是实现期第一步」那条流程教训。
- **无真机手验**：前端零改动、Agent 行为零改动，不需要微信真机验证。
- **E28 的处置**：`AgentChatServiceImpl` 实际 1274 行（蓝图记 1183 行）。本刀**不修改蓝图**（它已冻结，改动须走显式修订并更新 §12）；建议在 C7 规划闸时以实测值重算论证。请确认这样处理，或指示本刀顺带做一次蓝图勘误（属 Type B 文档改动）。
- **既有 534 tests 零回归 + 既有断言零修改**是硬约束。若某条既有断言必须改，须停下请示。

---

## 9. 外调预算

| 阶段 | 外调 | 预算 |
|---|---|---|
| 规划闸（本阶段） | 无 | **0** |
| 实现（闸门 2 后） | 全部走替身与 mock provider；不启动任何真实 provider | **0** |
| 联调 | **本刀不申请闸门 3** | **0** |

**本地环境提醒**：

- 本刀**不需要 MySQL**（N2 推荐纯 Mockito harness，不落库）。
- **R6 凭证轮换仍待用户执行**（`AI_API_KEY` / `S3_*` / `WECHAT_MINI_PROGRAM_SECRET`）。与本刀无关，但仍挂在残余里。
- 探针资产（`C3RealProviderProbeTest` / `C4RealProviderProbeTest` / `C5RealProviderProbeTest` / `C5MysqlTraceProbeTest`）默认门控跳过，**本刀不启用任何一个**。

`git push` / 部署 / 发布：本 change **不申请**。

---

## 10. 提交责任

**用户手动提交**（默认）。除用户当轮明确授权外，Agent 不执行 `git add` / `commit` / `push`。

**另**：不得使用波及未跟踪文件的 git 操作（不用 `stash` / `clean` / `reset --hard`），只用显式 `git add <path>`。工作区现有未跟踪产物见 `.ai/ACTIVE_TASK.md`「未跟踪 / 未提交产物」（该节已于 2026-07-31 按 `git status` 实测校正），一律不动。

---

## 11. 验收标准

### 用例载体与 runner

1. 合成用例以外置数据文件表达，由参数化 runner 驱动；用例数与场景在文件中一目可见。
2. 合成用例文件缺失时**硬失败**；本地真实样本文件缺失时**静默跳过**且不影响其他用例。
3. 解析器不可用时**明确失败**，不静默跳过用例（N1 的缓解项）。
4. 全部评测**零 provider 调用**，可在无网络、无 MySQL 的环境下跑完。
5. `AgentGuardrailBoundaryCaseTest` **原地保留，断言零修改**。

### 可编排替身

6. 替身能按轮次编排模型响应，覆盖 `AgentMockResponder` 产不出的路径：至少含**回复被降级**、**上下文组装（`prompt` 步骤存在）**、**工具提议被拒**、**provider 失败**四类。
7. `AgentMockResponder` **未被修改**（diff 可证）。

### 八个维度

8. 阶段推进正确性：`stage-decision` 的 from / to / reason 序列合法。
9. 追问克制：同阶段 `REASK` ≤ 1（E24：该上限是代码常量，断言它而非配置它）；`TURN_LIMIT_REACHED` 可触发。
10. 记忆三态可分：`enabled` / `failed` / `retrievedCount` 三种组合不混淆。
11. 注入预算：`injectedCount ≤ memory.maxFragments`，`injectedChars ≤ maxFragments × maxFragmentChars`（E25：**派生上限**，须在断言与 spec 中如实标注该上限是派生的，不是配置项）。
12. 护栏有效性：各层 `violation` 与期望一致（复用既有资产的判定语义）。
13. 长度克制：`afterLength ≤ maxReplyChars`；回复 / 输入长度比进快照。
14. 工具 fail-closed：无工具模式下模型返回提议时 `discardedCount` 行为正确。
15. 话术质量：结构就位、锚点文件形状定义清楚，并**如实标注锚点当前为空**（N7 选 a 时）。

### 快照分层与防橡皮图章

16. 不变量层失败**无任何刷新手段**（不提供开关、不提供命令）。
17. 快照层失败时打印「基线 vs 当前」，且信息足以支撑人工判断。
18. 每个用例的快照含 `baselineNote`，记录定基线的 change 与 `policyVersion`。
19. **只改快照数字不改 `baselineNote` 会被测试拦住**——须有测试直接验证这个机制本身，而不是只靠约定。
20. **不提供自动重写开关**（N4 选 a）。

### 隐私

21. 快照文件与失败输出**不含任何用例输入文本**，须有测试直接断言。
22. 隐私断言做成**结构化格式校验**（如断言指标串完全匹配 `coverage=… maxUncoveredRun=… checkedLength=…` 的数值形状），而非 E2 那种子串黑名单。
23. 真实样本文件被 gitignore 覆盖，且**规则为通配而非点名单个文件**（N5 选 b 时）；`git check-ignore` 可证。
24. 入库的合成用例中**不含任何用户真实日记内容**。
25. secret 未进入任何新增文件。

### R2 基线

26. 跑一次产出可比对的基线，且基线按 `promptVersion` / `policyVersion` 分组可辨（E14 / E15）。
27. **本刀未修改任何 prompt 文案或阈值**——因此基线记录的是「当前状态」，不是「优化后状态」。

### 范围守护

28. **main 代码零改动**（`git diff --stat` 中无 `src/main` 文件）。若实现期发现必须改，须停下请示并记录。
29. **前端零改动**。
30. 未引入新依赖；`pom.xml` / `package` / lockfile 未改。
31. 未新增 DDL、未改数据库、未改配置默认值。
32. 未做 Judge / 评分 / A/B / 看板；未做 C7 / C8 / C9 的任何部分；未动 R2 优化本身、未动 R9、未校准任何阈值（含 E27）。
33. 未引入 CI 配置。
34. **如实记录 E20**：交付的是「一条 maven 命令可跑」，**不是** CI 门槛。
35. **如实记录 E36**：快照指标在真实 provider 下的稳定性未验证。

### 回归

36. 后端既有测试全绿：**536 tests PASS / 4 skipped 基线**（E37 修正后的实测值），既有断言**零修改**。
37. 新增测试数量与分布在 AGENT_LOG 中如实记录。

---

## 12. 建议实现顺序

1. 规划闸批准（N1–N8 定稿）。
2. `.gitignore` 规则先落地（N5）——**在真实样本文件存在之前**就位，顺序不可颠倒。
3. 评测 harness：以 `AgentGuardrailTraceCorrelationTest` 的装配为种子抽出可复用的 harness，产出「跑一轮 → 拿到收集器」的能力。
4. 可编排 `AgentModelClient` 替身（N3），先用它跑通一条降级路径，证明 harness 能覆盖 mock 产不出的场景。
5. 用例文件格式 + 解析（N1）+ 参数化 runner；先迁一个维度跑通。
6. 不变量层八个维度逐条落地（验收 8–15）。
7. 快照层 + `baselineNote` 机制 + **拦住「只改数字」的自验测试**（验收 19）。
8. 隐私断言（验收 21–22）——**做成结构化校验，不复制 E2 的子串写法**。
9. 本地样本双份输入路径（缺失静默跳过）+ 话术质量维度的空结构（N7）。
10. 跑一次得到 R2 基线；回归 534；spec delta；收口。

---

## 13. spec delta 落点

> 依蓝图 §5 的 C6 行。

| spec | 内容 |
|---|---|
| `agent-runtime` | **MODIFIED 1 条**：C5 的「C5 范围内的评估能力」scenario 现写着「评估能力 SHALL 留给后续独立 change」，C6 即那个 change，须改为指向本刀条款（保留阶段范围声明，不删）。**ADDED**：可回归性条款——不变量与快照分层、不变量禁止刷新、基线变更须留痕、评测离线零外调、评测 SHALL NOT 改变 Agent 行为、诚实边界（mock 路径评编排不评语言质量） |
| `backend-core` | 评测资产的位置与离线约束、真实样本不入库、不新增依赖 / 不改 main、快照不含用例文本 |
| `agent-collaboration` | 评估与样本隐私规范：真实样本的处置、快照更新的协作约束（不得因评测失败顺手改行为或静默刷新基线） |
| `v2-product-scope` | **无 delta**（蓝图 §5 的 C6 行为「—」；本刀不新增任何产品可见能力，既有「不演化为分析面」条款已由 C5 覆盖） |
| `miniapp-core` | **无 delta**（前端零改动） |

---

## 14. 关键风险

| 风险 | 缓解 |
|---|---|
| **快照沦为橡皮图章**（本刀最高风险，且是这类框架最常见的死法） | 不变量层禁止刷新；不提供自动重写开关（N4）；`baselineNote` 强制留痕，且**「只改数字」这件事本身有测试拦**（验收 19） |
| **用 mock / 替身评质量的自欺** | 文档显式划界（验收 34 / §6 最后两行）；语言质量只走人评锚点且**如实标注当前为空**；叙事文档 §9 记录不做 Judge 的理由 |
| **真实样本进 tracked file**（后果最严重：泄漏用户日记） | gitignore 规则**先于样本文件落地**（实现顺序第 2 步）；规则用通配不点名（N5）；`git check-ignore` 作为验收证据 |
| 只覆盖 mock 路径而漏掉一半轨迹（E7） | N3 选 scripted `AgentModelClient` 替身，强制走「非 mock」分支；验收 6 明确要求 `prompt` 步骤存在 |
| 为了可测性去改 main 代码，把「零行为风险」这个最大优势弄丢 | §7 已论证零改动可行；验收 28 用 `git diff --stat` 机械核验；若必须改则停下请示 |
| 依赖传递库（snakeyaml）未来消失（N1 的代价） | 解析器不可用时明确失败而非静默跳过（验收 3）；届时换 JSON 的成本是局部的 |
| 用例膨胀难维护 | 确定性护栏用例留在既有 Java 资产里**不迁移**；只有新增维度走外置文件 |
| 把「一条命令可跑」写成「已有 CI 门槛」（E20） | 验收 34 单列为硬要求 |
| 顺手校准阈值（E27 那条明标未校准的很诱人） | 验收 32 明确禁止；理由写进 design 决策：建量尺与用量尺是两件事 |
