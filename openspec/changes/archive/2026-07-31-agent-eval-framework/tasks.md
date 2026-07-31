# Tasks：Agent Eval Framework（C6）

> Change ID：`agent-eval-framework`
> 阶段：**已完成并归档（2026-07-31）**。T-01 ~ T-34 全部完成
> N1–N8 **全部按推荐定稿**：snakeyaml / 纯 Mockito harness / 替身挂 `AgentModelClient` /
> 快照只能手工更新 / `samples.local.yaml` + 通配 / 普通 JUnit / 话术质量只建结构 / 0 外调
> 提交责任：**用户已授权 Agent 执行 `git commit`**；**`push` / 部署 / 发布仍未授权**
> 回归基线：**536 tests PASS / 4 skipped**（实现期实测修正，规划期沿用的 534 / 3 已过时——见 proposal E37）
> 开工锚点：`486ca95`

---

## 闸门检查点

- [x] **闸门 1 · 规划批准**：proposal / design（12 条决策）/ tasks / delta 落点，N1–N8 按推荐定稿（2026-07-31）
- [x] **闸门 2 · 实现授权**：用户 2026-07-31 明确授权按本文件实现
- [—] **闸门 3 · 外调授权**：**本刀未申请**（预算 0）。实现期未启用任何真实 provider 探针，
  实测外调 **0 次**。用户 2026-07-31 表述为「闸门 3 通过」，但本刀并无外调可授权，
  故如实记为**未申请**而非「已通过」——由此带来的未验证项见 `closeout.md` §3

---

## 阶段 0：隐私前置（顺序不可颠倒）

> 与「含 DDL 的 change 必须先执行 DDL」同型的前置项：一旦真实日记进了 git 历史，删文件也删不掉历史。

- [x] **T-01** `.gitignore` 新增本地样本规则（N5 定 b）
  - 加 `*.local.yaml` + `*.local.yml` 通配，注释写明理由（沿用 C5 把 `*.local.ps1` 从点名改通配的同一教训）
  - 顺带覆盖 `.yml` 后缀：只挡 `.yaml` 会留一个等价的漏口
  - 放行惯例保留既有 `!` 写法说明，未新造机制
- [x] **T-02** 规则生效验证 **PASS**
  - `git check-ignore -v` 命中：`samples.local.yaml` → `.gitignore:49`，`anything.local.yml` → `.gitignore:50`
  - 同一次调用确认 `cases/restraint.yaml`（合成用例）**不**被忽略，即规则没有过度拦截
  - 该验证在任何样本文件创建**之前**完成，顺序未颠倒

---

## 阶段 1：harness（装配与驱动）

- [x] **T-03** 抽出可复用评测 harness → `AgentEvalHarness`
  - 以 `AgentGuardrailTraceCorrelationTest` 的纯 Mockito 装配为种子；**未修改它本身**
  - **实现期加强两处**（比原计划更贴生产）：
    1. 用手写 `RecordingTraceSink` 替代 `@Mock` + `ArgumentCaptor`。**理由是 captor 拿到的是
       同一个可变对象的引用**，多轮时读到的全是最后一轮的终态，前几轮中间状态已被覆盖；
    2. 护栏 / 状态机 / prompt 组装 / **真实 `MySqlMemoryPort`** / 真实 `AgentToolValidator` +
       `AgentToolCoordinator` 全部用生产实现，替身只在两个边界：mapper 与 provider。
       否则「注入预算」断言的会是我写死的 3 条片段，而不是 port 真的会按 limit 收口
  - 消息与会话状态存在内存里并真的增长——否则多轮评测退化成「反复跑第一轮」
  - 验证：`AgentEvalHarnessTest` 7 项 PASS
- [x] **T-04** scripted 替身 → `ScriptedAgentModelClient`（N3 / P8 关闭）
  - **继承 `AgentModelClient` 只覆写两个真正发网络请求的方法**（`completeWithTools` / `complete`），
    其余（`unavailableReason` / `toolCallingUnavailableReason` / `isFunctionCallingModel` /
    `useStrictMode` / `model` / `provider` / `readArgumentText` / `extractText`）全走生产实现。
    **不用整体 mock 的理由**：整体 mock 会把「配置是否可用」「model 是否在 FC 白名单内」
    一并 stub 掉，于是断言的是我的 stub 而非生产逻辑——与 C5「H2 全绿≠验证」同型
  - 配置成一个真实可用的 openai-compatible 客户端（provider / model / apiKey 三者都必须对，
    否则分别会走 mock 分支、不下发 tools、或调用前早退）。apiKey 是显式假值且永不参与请求构造
  - `complete` 返回 `{"material":"..."}` 与生产同构，使素材路径的护栏真被走到
  - 剧本用尽时**明确失败**，不悄悄兜一个回复——那会让「多跑一轮」表现为一条看起来正常的轨迹
  - **`AgentMockResponder` 一行未改**（diff 可证）
- [x] **T-05** 四类 mock 产不出的路径全部覆盖（验收 6）**PASS**
  - 走真实分支的证据：轨迹含 `prompt` 步骤，且**与 provider 边界实际收到的消息条数一致**
    （两者对不上就说明采集点与真实调用脱节）
  - 回复被降级（真实 checker 判 `diagnostic`）/ 上下文组装 / 工具提议被拒
    （真实 validator 判 `unfaithful-args`）/ provider 失败（只记异常类型）
  - 额外覆盖回看 fail-closed（R10 那条 9 轮未活体触发的分支）与真实 MemoryPort

---

## 阶段 2：用例文件与参数化 runner

- [x] **T-06** 用例文件格式与解析（N1 定 a）→ `AgentEvalCaseLoader`
  - YAML + snakeyaml，**零 pom 改动**；4 个入库用例文件共 **23 条用例**
  - `NoClassDefFoundError` 单独兜底并明确失败，错误信息写明「不要跳过用例」——
    传递依赖消失时若静默跳过，就会变成最坏形态：**绿灯但什么都没测**
  - 格式错误 / 缺 cases 键 / 条目畸形 / caseId 重复全部硬失败
    （caseId 重复会让两条用例共用一条基线，其中一条的回归静默失效）
  - **实现期发现一个坑**：snakeyaml 按 YAML timestamp 规范把未加引号的
    `2026-03-14T21:00:00` **自动转成 `java.util.Date`**，`LocalDateTime.parse` 会拿到
    `"Sun Mar 15 05:00:00 SGT 2026"` 而失败。已两头处理：解析层兼容 `Date`（含时区还原），
    同时给用例里的时间加引号。只做后者等于留一条「靠人记得加引号」的规矩
- [x] **T-07** 参数化 runner → `AgentEvalRunnerTest`
  - `@ParameterizedTest` + `@MethodSource`（**本仓库首次使用参数化测试**，proposal E3）
  - 用例显示名只输出 caseId 与维度，**不输出用例输入文本**——测试报告也是一种产物
  - 额外三条元测试：基线不得有孤儿条目（否则「还有回归守着」是假象）、本地样本可选、入库用例缺失必失败
- [x] **T-08** 本地样本双份输入
  - `eval/samples.local.yaml`：缺失时**静默跳过**，同一 runner 两套输入，未另写通路
  - 验证：当前该文件不存在，套件全绿（这正是别人 clone 仓库时的状态）
  - 入库的 23 条用例全部为合成内容，**无任何用户真实日记**（验收 24）

---

## 阶段 3：八个维度（不变量层）

> 全部为**硬失败层，不允许刷新**。落点：`AgentEvalInvariants`。
>
> 除用例声明的期望外，另有 **6 条通用不变量**对每条用例都跑（长度上限、追问上限、
> 注入派生上限、reason 取自既有枚举、轮次不回退、版本锚点在位）。
> 通用不变量不写进 YAML，这样**将来任何新用例都自动受保护**，不依赖作者记得声明。
>
> 另有一条元保护：**期望键必须被消费**。写错键名（如 `downgrade_path`）会明确失败，
> 而不是静默忽略那条期望——后者会让用例看起来通过了，实际什么都没验。

- [x] **T-09** 阶段推进正确性：`stagePath` / `stageReasons` 序列断言（4 条用例，含回看不伪造判定）
- [x] **T-10** 追问克制
  - 判据用「**连续** REASK 的最长长度」而非 REASK 总数——不同阶段各追问一次是合法的
  - 断言 `MAX_REASK_PER_STAGE`（代码常量，不按用例调，proposal E24）；
    `TURN_LIMIT_REACHED` 与 `USER_FINISH_INTENT` 各有用例
- [x] **T-11** 记忆三态可分（3 条用例）
  - **实测印证了这个维度为什么需要不变量层**：「无命中」与「开关关闭」两条用例的
    **快照指标完全相同**，区别只在轨迹的 `memory-retrieval.enabled` 上。
    光靠快照比对分不出它们——已写进两条 baselineNote
- [x] **T-12** 注入预算（2 条用例 + 通用不变量）
  - 走真实 `MySqlMemoryPort`：5 条候选收口到 `maxFragments=3`
  - **实现期修掉一条自己写的假用例**：截断用例最初的样本只有 111 字（< 120 上限），
    于是「不超过 120」恒成立而**什么都没验**。改成远超上限的文本，并新增
    `injectedCharsExactly` 断言长度**恰好等于** 120，才真的证明截断发生
  - 派生上限已在断言消息与 spec 中如实标注（proposal E25）
- [x] **T-13** 护栏有效性（8 条用例）：诊断 / 谎称已封存 / 时间归属缺失各有正反例
- [x] **T-14** 长度克制：裁剪用例断言 `beforeLength > afterLength` 且 **outcome 不因裁剪变成 DOWNGRADED**
  - 长度比只进快照层不做不变量：`REASK` 用例实测 ratio=25.0，那是「嗯」这类极短输入的
    必然结果而非话痨，做成硬上限会逼出一堆无意义失败
- [x] **T-15** 工具 fail-closed（2 条用例）：回看模式丢弃 + 提议被护栏拒绝
  - R10 的事实**未被改变**：本刀不让它活体触发，只是让它的正确性多一层常驻回归

---

## 阶段 4：快照层与防橡皮图章

- [x] **T-16** 快照文件与比对 → `eval/baseline/snapshots.yaml`（**23 条基线全部就位**）
  - 13 个指标：turns / stagePath / stageReasons / outcome / providerCalls / materialCalls /
    injectedCount / injectedChars / promptMessageCount / downgradeLayer / violation /
    replyLength / replyToInputRatio
  - 失败时打印「基线 vs 当前」+ 可粘贴片段。**提供可粘贴片段不等于自动写回**——
    人需要做的判断是「这个变化对不对」，不是「把十个数字抄对」
  - **快照字段无一承载文本**（派生自 C5 已在类型层堵死文本的收集器）
- [x] **T-17** `baselineNote` 机制
  - 23 条全部写明由哪一刀定、当时状态、以及**这条基线为什么长这样**
  - 文件头显式记录：本批基线定于 R2 优化**之前**，因此它记的是「当前状态」而非「理想状态」
  - 缺 `baselineNote` 或缺 `checksum` 的条目一律拒绝加载
- [x] **T-18** 拦住「只改数字不改说明」→ `AgentEvalBaselineGuardTest`（6 项 PASS）
  - checksum 由「指标 + baselineNote」共同派生。**两个方向都测**：
    只改数字→checksum 变（拦住橡皮图章）；**只改说明→checksum 也必须变**。
    后者容易被漏，但少了它机制是残缺的——若只把指标纳入指纹，
    「改数字+改说明」这个**正确**流程与「只改数字」会算出同一个值
  - 另加一条反射测试：`write` / `save` / `update` / `rewrite` / `accept` / `approve`
    命名的方法一律不得出现。**写在文档里的禁令挡不住一个赶时间的下午，一条测试可以**
  - 手法与 C5 决策 6 同源：把「人必须记得同步」变成「不同步就报错」

---

## 阶段 5：隐私断言

> 落点：`AgentEvalPrivacyTest`（5 项 PASS）。

- [x] **T-19** 结构化格式校验（验收 22）
  - 用正则断言 `metrics()` **整串**匹配
    `coverage=\d+\.\d{3} maxUncoveredRun=\d+ checkedLength=\d+`
  - **白名单式（只允许这些）而非黑名单式（不允许那些）**，比既有那条子串断言强一个量级：
    子串断言只能守住当初列举的四个词，换一批用例文本就守不住了
  - **既有 `verdictMetricsMustNotLeakContent` 一行未动**（决策 12：删旧的丢历史，改旧的破纪律）
- [x] **T-20** 产物不含用例输入文本（验收 21）
  - 特征串 `紫罗兰色的旧铁皮盒子` 同时埋进用户输入、记录正文与记忆片段
  - 扫三处：轨迹（**全部顶层字段 + 全部步骤**，只扫 steps 会漏掉 causeType / model）、
    快照的规范化形式、可粘贴片段
  - 另加**结构性**校验：入库基线文件的指标行必须匹配「键名 + 结构化值」形状。
    这条不依赖特征串，因此换一批用例照样成立
  - 顺带守一道：入库用例文件不得含 `api_key` / `secret` / `bearer ` 等凭证形态内容

---

## 阶段 6：话术质量维度（只建结构）

- [x] **T-21** 人评锚点结构 → `eval/baseline/narrative-anchors.yaml` + `AgentEvalNarrativeAnchorTest`（3 项 PASS）
  - **锚点为空，且文件里显式写着「空 ≠ 该维度已覆盖」**
  - **空结构也上了测试**，理由是空文件最大的风险就是被误读成已覆盖：
    测试要求文件里必须留着「空≠已覆盖」那句话、不做 Judge 的三条理由、以及建议填充时机。
    有人把说明删掉或悄悄填占位数据，测试会失败
  - 锚点格式设计成「某版本在某阶段的语气评级」而非语料库：字段用受控词表，
    并有测试禁止出现 `reply` / `text` / `content` / `quote` / `excerpt` 这类承载文本的键
  - 填充时机：建议顺带在 **C7 闸门 3** 做（同一批真实产出既验重写又当锚点）

---

## 阶段 7：基线、回归与 spec delta

- [x] **T-22** R2 基线已建立
  - 23 条基线全部就位；不变量层断言每轮 `promptVersion` / `policyVersion` 在位，
    使基线可按版本锚点分组比对（proposal E14 / E15）
  - **本刀未改任何 prompt 文案或阈值** → 基线记录的是「当前状态」，不是「优化后状态」（验收 27）。
    这一点已写进基线文件头，避免将来被误读成「C6 调过之后的样子」
- [x] **T-23** 后端全量回归：**606 PASS / 4 skipped，BUILD SUCCESS，零回归**
  - 基线 **536** + 新增 **70** = 606（基线实测值是 536 / 4 而非规划期沿用的 534 / 3，见 E37）
  - **既有断言零修改**；4 skipped 仍是那四个环境门控探针，未新增跳过
  - 新增分布：runner 49（不变量 23 + 快照 23 + 3 条元测试）、harness 自检 7、
    防橡皮图章 6、隐私 5、锚点结构 3
- [x] **T-24** 范围守护证明 **PASS**
  - `git diff --name-only -- backend/src/main frontend/src` **输出为空** → `src/main` 零改动（验收 28）
  - 改动仅三个 tracked 文件：`.gitignore`（+7 行）、`.ai/ACTIVE_TASK.md`、`.ai/AGENT_LOG.md`；
    其余全是新增未跟踪目录（change 目录 + `src/test` 下的 eval 资产）
  - `pom.xml` / lockfile 未改；无新增 DDL；生产配置默认值未改；前端零改动
  - 未校准任何阈值（含 proposal E27 那条明标未校准的，也含实测发现的 n-gram 边界）
  - 未引入 CI 配置
  - 清理了验证期两个临时文件（`TempProbe.java`、一个 Python 小脚本），未留残余
- [x] **T-25** `agent-runtime` delta
  - **MODIFIED 1 条**：C5 的「C5 范围内的评估能力」scenario 现写「评估能力 SHALL 留给后续独立 change」，C6 即那个 change → 改为指向本刀条款，**保留阶段范围声明不删**（沿用 C5 修订 C2/C4/C3a/C3b 四条时的做法：范围声明本身是历史事实）
  - **ADDED**：不变量与快照分层、不变量禁止刷新、基线变更须留痕、离线零外调、评测 SHALL NOT 改变 Agent 行为、诚实边界（mock 路径评编排不评语言质量）
  - 实现期按实测补了 6 条 scenario：通用不变量、期望键拼写、三态不能只靠快照区分、
    留痕机制完整性（说明变化也须改校验值）、孤儿基线、锚点内容边界
- [x] **T-26** `backend-core` delta：评测资产位置与离线约束、真实样本不入库、不改 main / 不新增依赖、
  快照不含用例文本；**实现期补 3 条**：替身最小化、评测须驱动真实生成分支、跳过数不得增加
- [x] **T-27** `agent-collaboration` delta：评估与样本隐私规范、快照更新的协作约束（不得因评测失败顺手改行为或静默刷新基线）
- [x] **T-28** 确认 `v2-product-scope` **无 delta**、`miniapp-core` **无 delta**（蓝图 §5 的 C6 行为「—」；前端零改动）

---

## 收口

- [x] **T-29** 输出 Required Output
- [x] **T-30** 更新 `.ai/ACTIVE_TASK.md` Current Progress；追加 `.ai/AGENT_LOG.md`
- [x] **T-31** 如实登记三项诚实性条目
  - 仓库**无 CI** → 交付的是「一条 maven 命令可跑」，**不是** CI 门槛（验收 34）
  - 快照指标在**真实 provider** 下的稳定性**未验证**（验收 35 / proposal E36）
  - 话术质量维度的锚点**为空**（验收 15）
- [x] **T-32** `closeout.md`
- [x] **T-33** 用户验收 → delta 接受进 baseline → 归档 → `ACTIVE_TASK` → IDLE（2026-07-31 完成）
  - `agent-runtime`：1 MODIFIED（C5「范围内的评估能力」改为指向 C6，保留范围声明不删）+ 6 ADDED
  - `backend-core`：5 ADDED；`agent-collaboration`：3 ADDED
  - `v2-product-scope` 与 `miniapp-core` 确认**无 delta**
  - change 目录用 `git mv` 移入 `archive/2026-07-31-agent-eval-framework/`（保留历史）
- [x] **T-34** 叙事文档收尾（D33，每刀固定收尾项）
  - **§7「怎么保证改了 prompt 不退化」已写**：从 R2 那次误判切入（修锁等待后用户说「自然一些了」
    而一行 prompt 都没改）→ 为什么断言轨迹信号而非回复措辞 → 两层失败语义 →
    三道防橡皮图章机制 → 替身只替边界 → mock 分支评不到上下文组装这个坑 →
    不做 Judge 的三条理由 → **必须自己说清的边界**
  - §9 追加三行：Judge（理由展开为三条独立成立）、快照自动刷新开关、精细诊断载体
  - §4 补一段 C6 实测到的 n-gram 覆盖边界
  - 头部状态与进度行更新为「§1–§7 已写」
  - **硬边界已守**：无用户日记原文、无 secret、无本机绝对路径

---

## 范围守护自检（每个 task 完成时过一遍）

- **`src/main` 零改动**（本刀最大的安全优势；若必须改 → 停下请示）
- 未改 `AgentMockResponder`（`@Component`，跑在生产路径上）
- 未改 `AgentGuardrailBoundaryCaseTest` / `AgentGuardrailTraceCorrelationTest` / `AgentObservabilityIntegrationTest` 的任何断言
- 未给 `AgentTraceCollector` / `AgentTraceSink` / `AgentTraceVersions` 加方法或改签名
- 未改 Agent 对话行为（阶段 / prompt / 阈值 / 检索 / 工具 / 回看 / 轨迹采集点）
- 未校准任何护栏阈值；未动 R2 优化本身；未动 R9
- 未做 Judge / 绝对评分 / A/B / 质量看板
- 未做 C7 反思环 / C8 韧性 / C9 时间智能
- 未引入新依赖；`pom.xml` / `package` / lockfile 未改
- 未新增 DDL、未改数据库、未改配置默认值
- 未引入 CI 配置
- 未启用任何真实 provider 探针（不设置 `C*_REAL_PROBE=1`）；**本刀 0 外调**
- 未把用户真实日记写入 tracked files；样本文件 gitignore 已先行验证（T-02）
- 前端零改动
- 未执行 `git add` / `commit` / `push`
- **未使用波及未跟踪文件的 git 操作**（不用 stash / clean / reset --hard；只用显式 `git add <path>`）
  - 工作区现有未跟踪产物不得擅自提交或移动（2026-07-31 实测）：`.kiro/skills/`、
    三个已 gitignore 的 `backend/*.local.ps1` 及 `.bak`
- 未修改 `openspec/changes/archive/**`（归档即历史）
- 未修改已冻结蓝图（E28 的行号勘误只登记在本 change 内，不改蓝图正文）
