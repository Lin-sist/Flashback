# Closeout：Agent Memory Retrieval（C3 前半刀）

> Change ID：`agent-memory-retrieval`
> 状态：**已归档**（2026-07-29 用户验收，delta 已接受进 baseline）
> **闸门 3 未执行**：用户明确同意跳过并延后至 C3 两刀全部完成后合并进行。
> 后果诚实记录：R8「时间归属阈值未经真实样本校准」带着**未验证**状态进入 baseline。
> 日期：2026-07-29
> 提交责任：用户手动提交（Agent 未执行任何 git 写操作）

---

## 1. 交付了什么

| 目标（proposal §4） | 状态 | 落点 |
|---|---|---|
| 1 Memory 检索能力 | **done** | `MySqlMemoryPort` + `RecordMapper.selectMemoryCandidates` |
| 2 MemoryPort 抽象 | **done** | `agent/memory/MemoryPort`（带 `purpose` 维度，供后一刀复用） |
| 3 写作引导中的 Memory 注入 | **done** | `AgentPromptBuilder.buildMemorySupplement` + 五参重载 |
| 4 分层来源集合 | **done** | `AgentLayeredCorpus` + `AgentCoverageProfile.longestExclusiveRun` |
| 5 时间归属护栏 | **done**（阈值未校准） | `AgentTimeAttributionChecker` + `MISSING_TIME_ATTRIBUTION` |
| 6 跨记录关联能力 | **done** | 检索 + 注入契约；话术未改（R2 边界） |
| 7 隐私守护 | **done** | 片段不落库 / 不入日志 / 不进审计；`MemoryFragment` 覆写 `toString` |

新增 8 个生产类、1 个 DDL 脚本、7 个测试类。

---

## 2. 验证结果

- 后端全量：**472 tests PASS / 0 failures / 0 errors / 1 skipped**
  - 397 基线未回归；新增 75 项
  - skipped = `C4RealProviderProbeTest`（`C4_REAL_PROBE=1` 门控，C4 遗留）
- **既有断言零修改**。测试侧改动仅三类，均在文件内注释说明理由：
  1. 构造参数补齐（新增依赖注入）
  2. import 补充
  3. H2 `schema.sql` 增加 `agent_session.purpose` 列
- 前端：**零改动**（N5=(a)），已用 `git status` 核对，故未跑 `type-check` / `build:mp-weixin`
- 外调：实现阶段实际 **0 次**（预算 0）

### 关键验收项的证据位置

| 验收标准 | 证据 |
|---|---|
| 4 跨用户隔离 | `RecordMemoryRetrievalIntegrationTest.mustNotReturnOtherUsersRecordsEvenWhenHighlyRelevant`（真实 SQL，构造用户 B 的高相关记录断言零命中） |
| 2 不匹配 content | `RecordMemoryRetrievalIntegrationTest.mustNotMatchAgainstRecordContent`（关键词只存在于 content，断言零命中） |
| 5 排除 SEALED | `RecordMemoryRetrievalIntegrationTest.mustExcludeSealedRecords` |
| 11 memory-only 片段可识别 | `AgentLayeredCorpusTest.shouldDetectMemoryOnlyRunForVerbatimMemoryRestatement` |
| 12 只含本轮实际注入 | `AgentLayeredCorpusTest.memoryLayerMustOnlyContainInjectedFragments` + `AgentMemoryIntegrationTest.notInjectedHistoryMustNotBecomeLegalSource` |
| 13/14 时间归属正反例 | `AgentTimeAttributionCheckerTest`（裸复述判违规 + 6 种表述放行） |
| 15 阈值未放宽 | `AgentMemoryReplyGuardrailTest.guardrailThresholdsMustKeepTheirCalibratedDefaults`（直接断言默认值） |
| 16 memory 不得成为正文 | `AgentMemoryIntegrationTest.mustRejectMemoryTextAsRecordContent`（判 `memory-as-content`） |
| 18 fail-closed | `AgentTimeAttributionCheckerTest.shouldFailClosedWhenCheckThrows` |
| 20 隐私 | `MySqlMemoryPortTest.fragmentToStringMustNotLeakText` |

---

## 3. 实现期偏离规划的地方（诚实记录）

### 3.1 T-01 一度受阻 → 改为「不依赖覆盖率」的设计（事后实测验证了这个选择）

规划把 T-01 列为第一个 task，理由是覆盖率结论决定检索权重。实现期一度受阻（`root` 空密码被拒，DB 密码由启动脚本参数提供），故先把「需要权重」这个前提去掉：
- 检索 SQL 对 `title` / `core_question` / `ai_summary` / `belief_then` **并列 LIKE，无字段权重**；
- 片段取材按信息密度**固定优先级**降级：`ai_summary → belief_then → core_question → title`。

**用户提供凭证后已补测**（详见 tasks T-01 表格），结果反过来证明这个选择是对的：
- `ai_summary` 62%、`title` 85%、任一说明性字段 85% —— 主路径可用；
- **`core_question` 实测 0%** —— 若当初按覆盖率配权重，就会为一个恒为空的字段调参；固定优先级降级则自动跳过它；
- **`tag` 表为空、`record_tag` 零绑定** —— 标签维度当前完全不可用，本地检索线索实际只有关键词一条。这不是缺陷（数据尚未建标签），但意味着闸门 3 若要观察标签关联，须先建标签并绑定记录。

### 3.2 保留了 `AgentSourceCorpus` 单层签名重载

规划只说「分层」，实现时给 `AgentToolValidator.validate` 与 `AgentToolCoordinator.handleProposals` 各保留了一个接受单层 `AgentSourceCorpus` 的重载。

理由不是兼容测试，而是**「无记忆」是一个真实且常见的运行状态**：检索无命中、检索失败、记忆开关关闭三种情况都会走到它。让调用方每次先包一层 `sessionOnly` 是噪音。

### 3.3 新增了规划里没有的拒绝原因

`REASON_MEMORY_AS_CONTENT` 与既有 `REASON_UNFAITHFUL_ARGS` 分开留痕。两者都不放行，但成因不同：前者是「模型编了一句话」，后者是「模型把三个月前写的句子搬到今天的记录里」。混成一个原因的话，闸门 3 只能看到「又被拒了一次」，无法观察后者是否真的会发生。

### 3.4 删除了一个自己新建的类

规划期一度新建 `AgentSourceLayer` 枚举表达层级，实现后发现层级语义完全由 `AgentLayeredCorpus` 的方法表达，该枚举无任何引用，已删除，不留死代码。

### 3.5 修正了一次自己造成的 diff 污染

三个既有文件（`AgentGuardrailRules.java`、`AgentToolValidatorTest.java`、`AgentToolCoordinatorTest.java`）在编辑保存时被自动格式化，缩进从 4 空格变成 8 空格，导致 diff 虚增到 229 / 529 / 374 行。已从 HEAD 恢复并改用脚本直写重新施加编辑，实际改动回落到 **39 / 5 / 4 行**。

这条值得记：`git diff --stat` 的数字若与预期改动量差一个数量级，先怀疑格式化，不要当成真实改动接受。

---

## 4. 已接受的残余风险

| # | 风险 | 处置 |
|---|---|---|
| **R8** | **时间归属阈值 8 未经真实样本校准**。误伤方向（把正常回忆句判违规）与拦截方向（复述不带时间归属）**均未活体验证**。归档时用户同意跳过闸门 3，故该风险**已随 baseline 生效** | 延后到 C3 两刀合并做闸门 3（T-21）。缓解不是验证而是失败方向的选择：误伤只导致一句兜底回复，不会放行冒充 |
| **R9** | **检索相关性弱**：无字段权重、无分词、无向量。蓝图 C3 风险栏已接受 | 不粉饰；升级留独立 change |
| — | **本地 `tag` 表为空**，标签关联路径在当前数据下不产生任何命中（检索线索实际只有关键词） | 非代码缺陷；已有集成测试覆盖该路径。闸门 3 若要验标签关联须先建标签 |
| — | `core_question` 本地 0% 非空，检索谓词与取材链中该字段恒不贡献 | 固定优先级降级自动跳过，无需处置 |
| — | 时间归属词表是关键词匹配，中文时间指示语形态远多于词表 | 收词偏宽 + 阈值高，误伤优于漏放；词表在 `AgentGuardrailRules` 单一声明源，可增补 |
| **R7**（C4） | 忠实度闸拦截方向仍未活体验证 | 闸门 3 顺带观察，不单开验收项 |
| **R3**（C2） | 微信真机工具链路手验未走通 | 本刀前端零改动故不承接，留给 `agent-review-chat` |

---

## 5. 给 `agent-review-chat`（后一刀）的 carry-over

**可以直接复用的**：
- `MemoryPort.retrieve(MemoryQuery)`：签名已含 `purpose`，回看对话传 `REVIEW_CHAT` 即可，**不得另起一套检索实现**
- `agent_session.purpose` 列已建（默认 `WRITING_GUIDANCE`），后一刀只需加行为分支，**无需再做 DDL**
- `AgentLayeredCorpus` / `AgentTimeAttributionChecker`：回看对话同样会复述历史，时间归属护栏直接适用

**必须自己解决的**：
1. **`REVIEW_CHAT` 目前零行为分支**。本刀有测试断言「不存在任何 `purpose='REVIEW_CHAT'` 的会话」（`AgentMemoryIntegrationTest.shouldNotCreateAnyReviewChatSession`），后一刀实现时**需要修改该断言**，属预期变更，不是回归。
2. **开会话的 DRAFT 硬校验**：`AgentChatServiceImpl.requireOwnedRecordIfPresent` 拒绝非 DRAFT 记录，且 baseline `agent-runtime` 有条款 `Writing Guidance Must Target Draft Records Only`。回看作用于 UNLOCKED，必须以 MODIFIED 修订该条款——**不能悄悄放宽**。本刀的 delta 里已有一条对应的范围声明（「本刀范围内的回看对话」）也需同步修订。
3. **回看无阶段机**（Q4 定稿）：`AgentStageMachine` 是写作引导专用六阶段，回看需要 stage 固定常量 + 轮次上限的另一条路径。
4. **回看完全无工具**（Q6 定稿）：Runtime 不挂 tools，冒出 tool call 走 fail-closed。注意 `buildToolContext` 当前只按「有无 recordId」判断，回看会话**绑定的正是一条记录**，所以必须显式按 purpose 短路，否则会误发 tools。
5. **前端**：`ReviewChatSheet` 挂在 `record-detail`，与既有 `reply-overlay` 互斥；可抽共享消息壳，但不要直接复用带工具确认的 `AgentChatSheet`。
6. **R3 真机手验**在那一刀补齐。
7. **外调预算**：另申请 15–20 次（用户已预告）。

---

## 6. 待执行事项

- [ ] **生产库 DDL**：`backend/sql/mysql/c3-agent-memory.sql` 尚未在本地 MySQL 执行（测试走 H2）。真机联调前需执行
- [x] **T-01 覆盖率实测**：2026-07-29 完成（用户提供凭证）。结论见 tasks T-01 与 §3.1
- [ ] **闸门 3**：T-20~T-23，未申请
- [ ] **蓝图 §7**：C3 拆两刀的修订登记（T-27）
- [ ] **delta 接受 + 归档**：用户验收后（T-28）
- [ ] **[R6] 凭证轮换**：C4 遗留，仍待用户执行
