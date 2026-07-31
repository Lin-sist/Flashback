# Active Task

## Status

`IDLE`

当前无活动 Type C change。开始新的重大实现前，必须先创建 OpenSpec change 并更新本文件。

**Phase 1（M4 → C1 → C2 → C4 → C3a → C3b → C5）已全部完成。**
**Phase 2 第一刀 C6 `agent-eval-framework` 已于 2026-07-31 归档。**
下一动作是 **C7 `agent-reflection-loop` 规划闸（闸门 1）**，见下文 Direction Layer。

## Previous Completed

- Change：`agent-eval-framework`（C6，**Phase 2 第一刀**）
- 位置：`openspec/changes/archive/2026-07-31-agent-eval-framework/`
- 开工锚点 `486ca95`｜实现提交 `aedab6c`（+ `f2c998b` hash 补录）
- 结果：**离线、零外调、`src/main` 零改动**的评测框架落地并归档。
  轨迹级不变量（硬失败）+ 快照回归比对（需人确认），23 条合成用例、23 条带留痕基线。
  delta 已接受进 baseline：`agent-runtime`（1 条 MODIFIED——C5「范围内的评估能力」改为指向 C6，
  保留范围声明不删 + 6 条新增）、`backend-core`（5 条）、`agent-collaboration`（3 条）；
  `v2-product-scope` 与 `miniapp-core` 无 delta
- 验证：后端 **606 tests PASS / 4 skipped，BUILD SUCCESS**
  （536 基线 + 70 新增，**零回归、既有断言零修改**；4 skipped 仍是原有环境门控探针，未新增跳过）
- **闸门 3：未申请**（外调预算 0，全程未启用任何真实 provider 探针，实测外调 0 次）。
  用户 2026-07-31 表述为「闸门 3 通过」，但本刀并无外调可授权，故如实记为**未申请**
- **三项未完成项如实登记**（详见 closeout §3，**不得在后续文档写成已完成**）：
  ① 快照指标在真实 provider 下的稳定性**未验证**；
  ② 话术质量人评锚点**为空**（结构已就位，建议顺带在 C7 闸门 3 填）；
  ③ 仓库**无 CI**，交付的是「一条 maven 命令可跑」，**不是** CI 门槛
- **实现期五处发现**（详见 closeout §4）：基线实测 536/4 非 534/3；护栏一处 n-gram 覆盖边界
  （未校准，只钉成回归）；修掉一条自己写的「恒成立所以什么都没测」的假用例；
  snakeyaml 自动把 timestamp 转 Date；`ArgumentCaptor` 不适合多轮取轨迹
- 更早：`agent-observability`（C5，Phase 1 收官刀，位置
  `openspec/changes/archive/2026-07-30-agent-observability/`；闸门 3 真实调用 6 次，
  耗时 min 4571 / avg 6476 / max 8467ms；归档后修掉三个 Type B）、
  `agent-review-chat`（C3b）、`agent-memory-retrieval`（C3a）、`agent-guardrails-hardening`（C4）、
  `agent-tool-calling`（C2）、`agent-runtime-mvp`（C1）、`m4-real-capability-completion`

## Direction Layer

- **当前权威蓝图**：`Docs/agent-iteration/roadmap/iteration-blueprint.md` **v1.2 已冻结**（2026-07-30）
- 主线进度：M4 → C1 → C2 → C4 → C3a → C3b → C5（Phase 1 收官）→ **C6 已归档，Phase 2 开局**
- **Phase 2 定案序**：~~C6 agent-eval-framework~~（**已归档**）→ **`C7 agent-reflection-loop`（下一刀）** →
  `C8 agent-resilience` → `C9 agent-temporal-intelligence`（一次一个 ACTIVE）
- **下一动作：C7 规划闸**。建 `openspec/changes/agent-reflection-loop/`，写
  proposal / design / tasks + delta，**闸门 1 待用户批准**。开工前读蓝图 §4.3 意图卡片
- **C7 开工前必须注意的三件事**（来自 C6 closeout §9）：
  1. **以实测值重算类大小论证**：`AgentChatServiceImpl` 实测 **1274 行**，
     蓝图 §3.2 记的 1183 行已过时（C6 登记的勘误，蓝图已冻结未改）
  2. **人评锚点建议顺带在 C7 闸门 3 填**——C7 必然要真实联调重写路径，
     同一批真实产出既验重写效果又能当语言质量锚点，比单独申请一轮外调更省
  3. **C7 若改变编排行为，C6 的快照会变，那是预期的**——须在 `baselineNote` 里
     写明由 C7 改的，而不是把数字改成当前值了事
- **C6 已为 C7 备好前置**：反思环的价值主张是「重写后质量更好」，
  这个主张现在第一次可以被证伪（D30 排序决策的兑现）
- **Optional（不排主线）**：C0 平台升级（Boot 4.x/Java 21，Phase 2 完工后再议）、
  C10 语气标定、C11 上下文架构——均需证据触发
- **对外叙事文档**：`Docs/agent-iteration/narrative/agent-tech-story.md`（D33：每刀归档时更新对应段落；
  **§1–§7 已写**，§8 待 C7 补，§9 持续追加）

### v1.2 冻结的关键决策（D25–D33，开 C6 前必读）

| # | 决策 |
|---|---|
| D25 | Phase 2 以**能力叙事**为主驱动；未上线，不为想象中的生产故障提前投入 |
| D26 | 「前沿」限定在 **Agent 层**；平台升级降为 Optional C0 |
| D27 | **不引入图框架**；改为引入受控环 + 留可讲述 ADR |
| D28 | 反思环判定源**复用 C4 确定性护栏**，不新起 LLM 自检器 |
| D29 | 重写指令**只回传违规类型**，不携带候选文本片段 |
| D30 | **Eval 先于反思环**：先建量尺，再改模型输出行为 |
| D31 | **LLM-as-Judge 排除在 C6 之外**（隐私外发 + 预算 + 不可复现） |
| D32 | Eval 覆盖**轨迹不变量 + 回归比对**，不做绝对质量判分 |
| D33 | 叙事文档是每刀**固定收尾产物** |

## Source Of Truth

- `AGENTS.md`
- `Docs/agent-iteration/roadmap/iteration-blueprint.md`（**v1.2 已冻结**；Phase 2 方向与意图卡片）
- `openspec/project.md`
- `openspec/specs/agent-runtime/spec.md`（含 C1 + C2 + C4 + C3a + C3b + **C5**，Agent 核心契约）
- `openspec/specs/backend-core/spec.md`（含 M4 + C1 + C2 + C4 + C3a + C3b + **C5**）
- `openspec/specs/miniapp-core/spec.md`（含 M4 + C1 + C2 + C3b；**C5 无 delta**）
- `openspec/specs/v2-product-scope/spec.md`（含 M4 + C1 + C2 + C4 + C3a + C3b + **C5**）
- `openspec/specs/agent-collaboration/spec.md`（含 **C5**）
- 开工清单：`Docs/agent-iteration/workflow/prompt-snippets/type-c-checklist.md`

## Current Progress

- **Last session**: 2026-07-30 — C5 全流程完成并归档，随后修掉三个 Type B，真机复验全部 PASS
  - 规划：30 条现状事实（V1–V30）、11 条决策、四份 delta；N1–N7 按推荐定稿
  - 实现：`agent/trace/` 五个类 + 实体/mapper/XML + DDL + 9 条排查查询 + 38 项测试
  - 既有缺陷补齐：**V4**（降级痕迹此前恒传 null sessionId/turnNo）；**V5** 改用轨迹解决，未动 checker 签名
  - 闸门 3 执行完毕；delta 接受进 baseline；归档
  - **三个 Type B（均已真机验证 PASS）**：
    1. `ce4638f` 请求超时：前端 30s / 后端 20s，**顺序不可颠倒**
    2. `b6bcdd5` 输入框无法聚焦：关闭手势移到独立背景层（textarea 原生组件事件穿透 catchtap）
    3. `87cb29e` **每轮卡满 50 秒**：C5 自身引入的自锁，轨迹落库延后到事务提交后
- **This session**: 2026-07-30 — **蓝图 v1.2 校准会完成并冻结**（Type A 讨论 + Type B 文档落地）
  - 十问逐支定案 → D25–D33 九条新决策；序列定为 C6 → C7 → C8 → C9（新增 C7 反思环，编号顺移）
  - 消化五条实测证伪的前提（含新发现：steering 声称 Spring Security，实际全仓零匹配）
  - `iteration-blueprint.md` 升 v1.2 已冻结；11 处活文档引用同步；archive 内的 v1.1 引用**未动**（归档即历史）
  - 新建 `narrative/agent-tech-story.md`（§1–§6、§10 已写，§7/§8 待 C6/C7 补）
  - 未提交（默认用户手动提交）
- **This session**: 2026-07-31 — **C6 `agent-eval-framework` 规划闸执行完毕**（Type C 规划期，零业务代码）
  - 建 `openspec/changes/agent-eval-framework/`：`proposal.md`（36 条现状事实 E1–E36 + 8 个待裁决项 N1–N8 + 37 条验收）、
    `design.md`（12 条决策记录）、`tasks.md`（34 项 + 范围守护自检）、三份 delta 建议
    （`agent-runtime` 1 MODIFIED + 6 ADDED、`backend-core` 5 ADDED、`agent-collaboration` 3 ADDED）
  - delta 落点按蓝图 §5 的 C6 行：`v2-product-scope` 与 `miniapp-core` **无 delta**
  - **规划期核出五条对既有认知的修正**，全部已写进 proposal（详见 AGENT_LOG 同日条目）：
    1. **E7｜mock 分支根本不组装 prompt** → 「用 `AgentMockResponder` 跑评测」这条最直觉的路
       天然缺上下文组装维度，且跑不出任何降级轨迹。**这直接决定 N3 选 `AgentModelClient` 层而非给
       `AgentMockResponder` 抽接口**——后者看着更正统，实际覆盖面更小
    2. **E20｜仓库无 CI**（无 `.github/`，workflow 零命中）→ 架构宪法 §3.6 写的「CI 可跑子集」
       **当前无处兑现**。本刀只能交付「一条 maven 命令可跑」，已列为硬性诚实项（验收 34）
    3. **E21｜蓝图写的 `local-samples.yaml` 当前不被任何 gitignore 规则覆盖**；且仓库无通用
       `*.local.*` 规则。→ N5 建议改用通配命名（C5 已因「点名单个文件」吃过一次教训）
    4. **E24/E25｜两处断言可及性边界**：追问上限是代码常量（`MAX_REASK_PER_STAGE=1`）不可按用例调；
       无聚合记忆字符预算配置项，「注入预算」只能表达为**派生上限**并须如实标注
    5. **E28｜蓝图 §3.2 的 1183 行已过时，实测 1274 行**（C5 后 +91）。按 §0.4「规划期判断实现期须复核」
       登记勘误；**不改已冻结蓝图**，建议 C7 规划时以实测值重算论证
  - **本刀的关键性质：`src/main` 零改动**。四条既有事实刚好够用（22 协作者全构造注入、
    收集器读取器全 public、`persist` 单一出口可 `@Mock`、junit-params 与 snakeyaml 已在测试 classpath）
    → 不改 pom、不加 DDL、不为可测性动生产代码。这是 C5「收集器 + 单一落库出口」纪律的副产品
  - 未提交（默认用户手动提交）
- **This session（续）**: 2026-07-31 — **闸门 1 批准 + 闸门 2 授权后完成全部实现**（T-01 ~ T-28）
  - 实现顺序按 tasks 走：`.gitignore` 先行并验证 → harness → scripted 替身 → 用例与 runner →
    八维度不变量 → 快照与防橡皮图章 → 隐私 → 锚点结构 → 基线 → 回归
  - **实现期发现并处理的五件事**（详见 AGENT_LOG 同日第二条）：
    1. **E37｜回归基线实测是 536 / 4，不是规划期沿用的 534 / 3**。AGENT_LOG 里其实早有
       536 / 4，是 ACTIVE_TASK 顶部与蓝图摘要没跟上。**已修本文件**，蓝图不动（已冻结）
    2. **护栏的一处真实边界**：「用户自己说过的病名可以复述」的成立条件比直觉窄——
       取决于 Agent 是否连带复用了周边 4-gram。同一输入下只改复述措辞即被判 `diagnostic`。
       属 n-gram 方案固有性质与 C4 刻意选的误伤方向，**本刀不校准阈值，只把边界钉成回归**
    3. **自己写的一条假用例**：截断用例最初样本 111 字（< 120 上限），「不超过 120」恒成立
       而什么都没验。已改成远超上限的文本 + `injectedCharsExactly` 断言恰好等于 120
    4. **snakeyaml 会把未加引号的 timestamp 自动转成 `java.util.Date`**，
       `LocalDateTime.parse` 拿到 `"Sun Mar 15 05:00:00 SGT 2026"` 而失败。两头都处理了：
       解析层兼容 `Date`，用例里也加引号——只做后者等于留一条「靠人记得加引号」的规矩
    5. **`ArgumentCaptor` 不适合多轮取轨迹**：captor 拿到的是同一个可变对象的引用，
       多轮时读到的全是最后一轮终态。改用手写 `RecordingTraceSink` 按 persist 顺序存下
  - 未提交（本轮已获授权可提交）
- **已提交**: `aedab6c`（用户当轮授权 commit；**未 push**）。提交后已复跑全量测试，
  BUILD SUCCESS——git 对 YAML 做 LF→CRLF 规范化，故提交后必须复验一次解析仍正常
- **This session（收口）**: 2026-07-31 — **C6 验收通过并归档，Phase 2 开局完成**
  - delta 接受进 baseline：`agent-runtime` 1 MODIFIED + 6 ADDED、`backend-core` 5 ADDED、
    `agent-collaboration` 3 ADDED；`v2-product-scope` 与 `miniapp-core` 确认无 delta
  - change 目录用 `git mv` 移入 `archive/2026-07-31-agent-eval-framework/`（保留历史）
  - `closeout.md` 写就；叙事文档 §7 已在实现期补完（D33 固定收尾项）
  - **闸门 3 的处理**：用户表述为「闸门 3 通过」，但本刀**并无外调可授权**（预算 0、
    实测 0 次），故在 closeout 与 tasks 中如实记为**未申请**而非「已通过」。
    由此产生的未验证项（快照指标在真实 provider 下的稳定性）单独登记
- **Blocked on**: none
- **Next step**: **开 C7 `agent-reflection-loop` 规划闸**。建 change 目录写
  proposal / design / tasks + delta 建议 → 闸门 1 待批准。
  开工前读蓝图 §4.3 意图卡片，并注意 Direction Layer 里列的三件事
  （类大小用实测 1274 行、锚点顺带在闸门 3 填、快照变化须写 `baselineNote`）

## C6 的关键结论（对 C7 有直接价值）

- **量尺已就位，R2 第一次可以谈优化了**。基线定于优化**之前**，记的是「当前状态」而非
  「理想状态」——这一点写在 `eval/baseline/snapshots.yaml` 文件头，避免将来被误读
- **C7 改编排行为时，C6 快照会变，那是预期的**。正确做法是确认变化符合预期后手工更新，
  并在 `baselineNote` 写明由 C7 改的；**不得**把数字改成当前值了事
  （checksum 由「指标 + 说明」共同派生，只改数字会被拦住）
- **`AgentChatServiceImpl` 实测 1274 行**（蓝图记 1183 行已过时）。C7 要在这个类里
  掉转依赖方向，论证须用实测值
- **走 mock provider 分支评不到上下文组装**：生产代码在 `isMockProvider()` 处直接 return，
  压根不组装 prompt。C7 若要为重写路径写测试，同样要走非 mock 分支才看得到 `prompt` 步骤
- **替身设计的一条经验**：替身替掉的越多，被覆盖的生产代码越少。C6 的 provider 替身
  继承生产类只覆写两个真正发请求的方法，其余判定（配置可用性、FC 白名单）全走生产逻辑
- **护栏一处未校准的 n-gram 覆盖边界**已钉成回归用例。C7 的重写指令若改变复述措辞的形态，
  这条用例会有反应

## C5 的关键结论（对 v1.2 校准与 C6 有直接价值）

- **provider 耗时首次有数据**：min 4571 / avg 6476 / max 8467ms。这项数据 C5 之前完全不存在
  （成功路径的 `startedAt` 被直接丢弃）。它同时是 Type B 超时缺陷的定位依据，
  也是 **C7 反思环预算**与 **C8 韧性**设计的输入（v1.2 编号：韧性已从 C7 顺移至 C8）
- **版本锚点由内容哈希派生**，改文案自动变化。C6 的回归比对可直接按
  `prompt_version` / `policy_version` 分组（`c5-trace-queries.sql` 第 7 条）
- **`AuthRole.ADMIN` 全仓无签发路径**（`UserServiceImpl` 固定签 `USER`）→
  `/admin/**` 下的端点在真实环境不可达。**任何未来 change 若打算做 admin 端点，须先解决签发问题**
- **`schema.mysql.sql` 只到 C1**：既无 `agent_tool_call`（C2）也无 `agent_session.purpose`（C3）。
  项目既有约定是全量脚本不随增量维护。**待用户决定是否另开 Type B 补齐**（见 Residual）
- 两处已获批的对已冻结蓝图的偏离：存储用 MySQL 表而非 JSON 日志文件；默认全量不采样

## 实测证伪 / 修正的前提（**已于 v1.2 冻结时消化，见蓝图 §2.3**）

以下五条已写入蓝图 §2.3 并落到相应文档，此处保留供快速查阅：

1. **`/admin` 端点方案不可行**：`AuthRole.ADMIN` 全仓无签发路径（`UserServiceImpl` 固定签 `USER`），
   该路径下的端点在真实环境不可达。任何未来 change 若要做 admin 端点，须先解决签发问题
2. **`schema.mysql.sql` 不随增量维护**：它只到 C1（无 C2 表、无 C3 列、无 C5 表）。
   规划期曾断言「新表需同步三处」，与项目既有约定不符
3. **H2 集成测试不足以验证锁 / 外键 / 事务边界**：C5 的 50 秒锁等待缺陷在 H2 上**不可能复现**。
   → **已写入蓝图 §0.4：「真实联调」定义收紧为包含真实 MySQL**
4. **R2 的优化基线须重建**：此前「引导话术生硬」的判断样本受 55 秒延迟污染。
   → **已定案由 C6 重建基线**（D30/D32），不再靠手验找感觉
5. **认证不基于 Spring Security**（校准会新发现）：`springframework.security` 全仓零匹配、
   pom 无 security starter，实为 jjwt + 自研过滤器。→ **已修 `.kiro/steering/tech.md`**

**附带事实**：`pom.xml` 含 `spring-boot-starter-data-redis` 且 dev/prod yml 有 redis 配置段，
但 main 代码零消费（会话走 MySQL）。标记 `partial`，不在 Phase 2 处理

## Residual / Carry-over

- **[R10] 回看 fail-closed 仍未活体触发**：C3b 3 轮 + C5 6 轮共 9 轮观察，模型均未在无工具模式返回提议。
  属概率性行为，**不单开 change**；C5 做到「它真发生时能被记下」，
  **C6 又让它的正确性多一层常驻回归**（评测里能稳定驱动该分支）。
  但**未活体触发这个事实未变**，不得因为多了回归就写成已验证
- **[待用户决定] `schema.mysql.sql` 落后于增量脚本**：补齐它需要同时补 C2 + C3 + C5 三刀的表与列，
  属独立 Type B。C5 刻意未动（只加 C5 会造出「有 C5 表却无 C2 表」的更怪状态）
- **[新] 轨迹写在业务事务之后**：理论上存在「业务成功但轨迹丢失」的窗口（进程崩溃）。
  可接受——轨迹是辅助设施，而原方案的代价是每轮卡 50 秒
- **[R2｜量尺已就位，可以开始谈优化] 引导话术与素材合成质量**：C6 已建成基线（D30/D32 兑现）。
  **基线定于优化之前**，记的是「当前状态」而非「理想状态」（写在
  `eval/baseline/snapshots.yaml` 文件头）。现在改 prompt 前后第一次可比对——
  宪法 §7.3 那条「无 Eval 情况下大改 prompt 上线」的禁令已具备解除条件。
  改动后快照会变，须确认符合预期再手工更新并在 `baselineNote` 写明由哪一刀改的
- **[新｜C6 未完成项] 快照指标在真实 provider 下的稳定性未验证**：C6 外调预算 0。
  它其实不构成风险——快照的用途是「同一份确定性输入下改动前后是否变化」，
  真实 provider 从来不在这个用途里。**如实登记，不得写成已验证**
- **[新｜C6 未完成项] 话术质量人评锚点为空**：`eval/baseline/narrative-anchors.yaml`
  结构已就位、内容为空，且「空≠已覆盖」由测试守着。填它需真实产出 + 人评，
  **建议顺带在 C7 闸门 3 做**（同一批真实产出既验重写又当锚点）
- **[C6 登记｜待用户决定] `minMemoryOnlyRunForAttribution=8` 未校准**（代码注释明写；
  对比 `minCoverage=0.35` 有真实样本校准记录）。**另新增一条**：护栏「只在新增区段匹配」
  的成立条件依赖 4-gram 覆盖，复述用户说过的病名时是否放行取决于有没有连带复用周边措辞
  ——已钉成回归用例。两者 C6 都**刻意不校准**：建量尺和用量尺改参数混在一起，
  就分不清指标变化是哪个原因造成的（R2 那次误判的同型错误）。属独立事项
- **[C6 登记｜待用户决定] 仓库无 CI**：无 `.github/`，全仓 workflow 零命中。
  架构宪法 §3.6 的「CI 可跑子集」当前无落点，C6 交付的是「一条 maven 命令可跑」。
  未顺手建（撞「不改 deployment / monitoring」，且涉及跑在哪 / 密钥怎么给 / 失败怎么处理）
- **[新｜C6] 基线手工更新的规模上限**：刻意不提供自动重写开关（有了它，「评测红了」
  的最短路径就变成「跑个命令让它变绿」）。用例大幅增长后批量更新会烦；届时若加开关，
  须**同时**配「说明未变更则失败」的守护——**不得先给出口再补守护**
- **[新｜C6] snakeyaml 是传递依赖**（经 `spring-boot-starter`），非本项目直接声明。
  已加 `NoClassDefFoundError` 兜底明确失败，绝不静默跳过用例——否则某次 starter 升级后
  会变成最坏形态：**绿灯但什么都没测**
- **[已关闭] 三个 Type B 的真机复验**：用户 2026-07-30 执行，全部 PASS
- **[已关闭] MySQL 上轨迹落库未联调**：已联调，并由此发现且修复了 50 秒锁等待根因
- **[R9] 检索相关性弱**：标签 + 说明性字段 LIKE，无权重 / 分词 / 向量。升级留独立 change
- **[R6｜待用户执行] 凭证轮换**：`AI_API_KEY` / `S3_ACCESS_KEY` / `S3_SECRET_KEY` / `S3_SESSION_TOKEN` /
  `WECHAT_MINI_PROGRAM_SECRET`。轮换后建议删除 `backend/start-dev-wechat.local.ps1.bak`（含旧明文，已 gitignore）
- **探针资产**：`C3RealProviderProbeTest`（`C3_REAL_PROBE=1`）、`C4RealProviderProbeTest`（`C4_REAL_PROBE=1`）、
  `C5RealProviderProbeTest`（`C5_REAL_PROBE=1`）、`C5MysqlTraceProbeTest`（`C5_MYSQL_PROBE=1`）。
  **共 4 个，全部默认跳过**（这也是当前基线 4 skipped 的来源）。C5 探针另有一处形态差异：
  它是 `@SpringBootTest` 走完整 `sendMessage`，因为要验的是编排层有没有漏采集。
  **C6 未新增探针**（0 外调）
- **[新｜C6] 评测资产**：`backend/src/test/java/com/flashback/agent/eval/`（10 个类 + 5 个测试类）、
  `backend/src/test/resources/eval/`（4 份用例 + 快照基线 + 空锚点）。
  **离线、零外调、默认随 `mvn -q test` 一起跑**，不需要任何环境变量
- **[新｜C6] 本地真实样本入口**：`backend/src/test/resources/eval/samples.local.yaml`
  （**当前不存在**；已被 `.gitignore` 的 `*.local.yaml` 通配覆盖并用 `git check-ignore` 验证过）。
  缺失时评测静默跳过相关用例，这正是别人 clone 仓库时的状态
- **本地联调脚本**：`backend/run-c5-probe.local.ps1`（已 gitignore；`.gitignore` 的 `*.local.ps1`
  规则由 C5 从「只点名单个文件」改为通配）
- **[C3a 实测] 本地 `tag` 表 0 行**、`core_question` 0% 非空、26 条记录中 `ai_summary` / `belief_then` 各 62%
- **历史数据**：`agent_message` 中 6 条 C2 修复前的 JSON 包裹消息（id 13/15/17/19/20/21）
- **本地环境**：MySQL80 StartType=Manual；C1/C2/C3/**C5** 的增量 DDL 均已执行完毕
- **secret 读取方式**：`backend/secrets.local.env`（gitignore）+ `Get-LocalSecret`，缺键快速失败
- **M4 carry-over**：MySQL `EXPLAIN` timeline（Type B，不阻塞主线）
- **Kiro 诊断误报**：change 的 `design.md` / `tasks.md` 报「缺少 Kiro Spec 章节」。
  本项目按规则使用 OpenSpec 而非 `.kiro/specs/`，**不修**

## 流程教训（后续 change 须遵守）

- **含 DDL 的 change 必须把「本地执行 DDL」列为实现期第一步**，而不是联调前置。
  C3b 曾因此让用户手验报「系统异常: api/agent/sessions」——且因为 mapper 列清单缺列，
  **写作引导对话也一起 500**，波及既有功能，而报错表现只是通用 500。C5 已按此执行
- **不得使用波及未跟踪文件的 git 操作**：曾用 `git stash push --include-untracked` 意外收走用户的
  `iteration-blueprint-v1.2-draft.md`、`Docs/agent-iteration/architecture/`、`.kiro/skills/`（已按字节校验恢复）。
  一律只用显式 `git add <path>`，不使用 stash / clean / reset --hard
- **警惕编辑器自动格式化造成的 diff 污染**：若某文件 `git diff --stat` 比预期改动量大一个数量级，
  先怀疑格式化或行尾变化（用 `--ignore-all-space` / `--ignore-cr-at-eol` 对比），不要当成真实改动接受
- **验证拦截方向必须先确认样本确实处于该被拦的状态**：C3b 曾取「最后一轮」回复做剥离实验，
  而它恰好没在复述（memory-only=0），不翻转是样本选错而非护栏失效
- **[新｜C5] 前后端超时必须有明确的先后关系，不能相等**：相等时前端总是先断，
  后端精心设计的显式失败语义会被网络层错误覆盖。凡新增调用 AI 的前端请求，
  须显式指定超时且大于后端 `app.ai.timeout-millis`
- **[新｜C5] 规划期的「须同步三处」类判断要在实现期复核**：C5 规划时断言新表需同步三份 schema，
  实测发现 `schema.mysql.sql` 只到 C1，项目既有约定与规划假设不符。**不要把假设写成事实**
- **[新｜C5 最重要的一条] 涉及锁 / 外键 / 事务边界的改动，H2 全绿不构成验证**：
  C5 的轨迹落库在 H2 上 37 项测试全绿，却在真实 MySQL 上让每轮对话卡满 50 秒
  （`REQUIRES_NEW` 与外层事务争 `agent_session` 的父行锁）。H2 没有 InnoDB 行级锁语义，
  **该缺陷在 H2 上不可能复现**。此类改动的联调必须打真实 MySQL；
  写回归时优先断言**调用时机 / 不变量**而非结果，这样在 H2 上也能守住
- **[新｜C5] `REQUIRES_NEW` 不等于「不影响外层事务」**：它挂起外层事务但**不释放外层已持有的锁**。
  若新事务要碰同一批行（尤其经由外键），就会与自己的外层事务死等。
  想做「业务提交后再做副作用」应用 `TransactionSynchronization` 回调，不是换传播级别
- **[新] fail-open 只保证不报错，不保证不阻塞**：C5 的轨迹写入确实 fail-open，
  但它在失败**之前**先卡了 50 秒。评估「某个辅助设施失败是否影响用户」时，
  必须同时看**失败前的等待成本**，不能只看失败后的处置
- **[新｜C6] 警惕「恒成立所以什么都没测」的断言**：C6 写的一条截断用例样本只有 111 字，
  而上限是 120——「不超过 120」恒为真，那条用例实际什么都没验。
  验「某个上限真的生效」要断言**恰好等于上限**，不是「不超过上限」。
  同型问题也出现在验证拦截方向时（C3b 的样本选错）：**先确认样本真的处于该被测的状态**
- **[新｜C6] 替身替掉的越多，被覆盖的生产代码越少**：极端情况下断言的是自己的替身。
  C6 的 provider 替身继承生产类只覆写两个真正发网络请求的方法，
  「配置是否可用」「model 是否在白名单内」这些判定仍走生产逻辑；
  记忆检索用真实 port + mock mapper，使「注入不超预算」断言的是 port 真的会收口
- **[新｜C6] 摘要类数字要定期复核**：`ACTIVE_TASK` 顶部与蓝图记的回归基线 534/3
  已过时（实测 536/4），而 AGENT_LOG 里其实早有正确值——**是摘要没跟上明细**。
  每刀开工时顺手核一次自己要引用的数字
- **[新｜C6] `ArgumentCaptor` 不适合取多次调用中的可变对象**：captor 持有的是引用，
  多轮时读到的全是最后一轮的终态。要按顺序看每一轮，用能在调用时刻存快照的替身

## 未跟踪 / 未提交产物（不要擅自提交或移动）

> **2026-07-31 复核修正**：本节此前列的多项已在 `e5722d8` / `486ca95` 两个提交中入库，
> 不再是未跟踪产物。以下为 `git status` 实测的当前状态。

**当前未跟踪（实测）**：

- `.kiro/skills/`（**唯一一项**。C6 提交时刻意未纳入——它是工作区既有产物，不属那一刀范围）

**已 gitignore 的本地脚本（不入库，保留）**：

- `backend/run-c5-probe.local.ps1`（闸门 3 探针运行脚本）
- `backend/probe-turn-latency.local.ps1`（一轮耗时排查脚本，签本地 dev JWT 打真实链路。
  定位 50 秒锁等待时用的就是它，保留供将来复用）
- `backend/start-dev-wechat.local.ps1` 与其 `.bak`（`.bak` 含旧明文，待 R6 轮换后删除）

**已入库，本节不再挂账**：`Docs/agent-iteration/architecture/`（三份）、
`narrative/agent-tech-story.md`、v1.2 冻结带来的 8 处引用同步改动。
`roadmap/iteration-blueprint-v1.2-draft.md` **已不存在**（内容已迁入正式蓝图，草稿已删）。

## Out Of Scope While Idle

- 不要在没有新 Type C 的情况下改 Agent runtime / 工具 / 护栏 / 记忆 / 回看 / 轨迹 / 评测 / AI 业务代码
- **C7 须先过闸门 1**：可以建 change 目录写 proposal / design / tasks，但**规划批准前禁止改业务代码**
- **改 prompt / 护栏阈值现在有条件了，但仍须走 Type C**：C6 的基线让改动可比对，
  这解除的是宪法 §7.3 的技术前提，**不是**流程要求。改动后快照会变，
  须确认符合预期再手工更新并在 `baselineNote` 写明由哪一刀改的
- **不要校准阈值**（含 `minMemoryOnlyRunForAttribution` 与 C6 登记的 n-gram 覆盖边界）——
  属独立事项，待用户决定
- **不要静默刷新 C6 的快照基线**：checksum 由「指标 + 说明」共同派生，只改数字会被拦住；
  也不要为图方便加自动重写开关（若要加，须同时配「说明未变更则失败」的守护）
- 不做 LLM-as-Judge / 绝对评分 / A/B 框架 / 质量看板（D31/D32）
- 不做 C7 反思环 / C8 韧性 / C9 时间智能的任何部分
- 不引入 CI 配置（属独立决策）
- 不要并行复活已归档 change 作为隐式 active change
- 不要在未获授权时发起真实 provider 调用（4 个探针默认门控跳过，勿擅自设置 `C*_REAL_PROBE=1` /
  `C5_MYSQL_PROBE=1`）
- 不要修改 `openspec/changes/archive/**`——归档即历史，含其中的 v1.1 引用与 C6 的勘误登记
- 不修改已冻结蓝图（C6 登记的两处勘误——1274 行、536/4 基线——只留在 change 内）
