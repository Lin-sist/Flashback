# Tasks：Agent Observability（C5）

> Change ID：`agent-observability`
> 阶段：**闸门 1 已批准 + 闸门 2 已授权（2026-07-30），实现进行中**
> N1–N7 定稿（用户按推荐批准）：MySQL 表 / 每轮一条聚合 + 单一落库出口 / 全量不采样 / 只落库不加端点 / 补齐 V4·V5 / 版本由内容哈希派生 / 级联删除 + 保留期 + 手动清理
> 提交责任：**用户手动提交**（除当轮明确授权外，Agent 不执行 git 写操作）
> 回归基线：**496 tests PASS / 2 skipped**

---

## 闸门检查点

- [x] **闸门 1 · 规划批准**：proposal / design（11 条决策）/ tasks / delta 落点，N1–N7 定稿（2026-07-30）
- [x] **闸门 2 · 实现授权**：用户 2026-07-30 明确授权按本文件实现
- [ ] **闸门 3 · 外调授权**：**未授权**。实现期全部走 mock provider；T-35~T-37 不执行，如实记录

---

## 阶段 0：DDL 前置（C3b 流程教训，若 N1 定 a）

- [x] **T-01** 新增 `backend/sql/mysql/c5-agent-turn-trace.sql`
  - 幂等（`CREATE TABLE IF NOT EXISTS`），外键随 session / user / record 级联删除
  - 头部注释写明隐私边界与禁止写入清单
  - 附手动清理语句（N7），不引入定时任务
- [x] **T-02** **已在本地 MySQL 执行并验证**（2026-07-30）
  - 表已创建，20 列与设计一致；三个索引 `idx_agent_turn_trace_session(session_id,turn_no,attempt_no)` /
    `idx_agent_turn_trace_user_id(user_id,id)` / `idx_agent_turn_trace_created_at` 均在位
  - **幂等已验证**：重复执行脚本 `exit=0`，无报错
  - 按 C3b 流程教训，这一步是实现期第一步，未推迟到联调前
- [x] **T-03** schema 同步 —— **V19 的判断在实现期被证伪，实际只需两处**
  - `backend/sql/mysql/c5-agent-turn-trace.sql`（增量）✅
  - `backend/src/test/resources/schema.sql`（测试，注意既有 DROP 顺序）✅
  - **`backend/sql/mysql/schema.mysql.sql` 刻意不动**：实测该文件只到 C1——**既没有 `agent_tool_call`（C2），也没有 `agent_session.purpose`（C3）**。
    即项目的既有约定是「全量脚本不随增量维护」，而不是我规划时以为的三处同步。
    只往里加 C5 的表会造出更奇怪的状态：一个有 C5 表却没有 C2 表的「全量」脚本。
    补齐它需要同时补 C2 + C3，属于本刀范围外的顺手改动，**如实记为发现，交由用户决定是否另开 Type B**
  - 验证：`@SpringBootTest` 集成测试能正常建表启动（见 T-28）

---

## 阶段 1：收集器与单一落库出口（N2 硬约束）

- [x] **T-04** `AgentTraceCollector` per-turn 收集器
  - 收集方法只接受基础类型与既有枚举；`step(...)` 这个可变参数入口是**私有**的，外部拿不到
  - 允许 `String` 形参的 7 个方法各自承载配置值 / 异常类名 / 结构化常量，**已列成白名单并由测试守护**
  - 验证：`AgentTraceCollectorTest.collectorMustNotAcceptArbitraryTextParameters` 用反射遍历全部公开方法，
    发现白名单外的 `String` 形参即失败——将来有人加 `replyText(String)` 会被当场拦住
- [x] **T-05** `AgentTraceSink` 单一落库出口
  - `sendMessage` 用 try/finally 包住整轮编排，`persist` 是唯一 insert 点
  - 未在 `AgentToolCoordinator` / checker 里增加任何 insert
  - 验证：`shouldRecordTraceWhenProviderUnavailable` 断言早退路径仍产生一条记录
- [x] **T-06** fail-open 保证（决策 7）
  - `persist` 标 `@Transactional(REQUIRES_NEW)`——**不进 `sendMessage` 的事务**，
    否则痕迹写失败会把用户消息与 Agent 回复一起回滚，那是 C1「用户输入不丢」的反面
  - `persist` 内部 try-catch 只记 warn，不向上抛
  - 验证：`disabledObservabilityMustStillDowngradeButLeaveNoTrace` 覆盖关闭路径；
    REQUIRES_NEW 的必要性由集成测试的组织方式反证（该测试类因此不能用 `@Transactional`，已在类注释说明）
- [x] **T-07** `traceId` 与 `attemptNo`
  - `traceId` 为 32 位无连字符 UUID；`attemptNo` 由 `AgentTraceSink.nextAttemptNo` 推导（首次不查库）
  - 验证：`retriedTurnShouldBeDistinguishableFromNewTurn` —— provider 先不可用再重试原消息，
    断言两条轨迹 `turnNo` 同为 1 而 `attemptNo` 为 1/2，且第二条含 `stage-retained`（未再次推进阶段机）

---

## 阶段 2：thought 侧（C5 之前完全空白，V17）

- [x] **T-08** 模式与阶段判定 reason
  - 复用既有 `AgentStageDecision.Reason`（V6：该枚举注释早写着「便于后续可观测（C5）复用」，
    **本刀是它第一次真正被使用**）
  - 回看**不调** `stageDecision`，改记 `stage-retained` —— 不伪造一个不存在的判定结论
  - 验证：`traceShouldCarryThoughtActionObservationSegments`（写作引导有 reason）
    + `reviewChatTurnShouldBeDistinguishableInTrace`（回看 `stageReason` 为 null）
    + `stageRetainedShouldNotFabricateAReason`
- [x] **T-09** 记忆检索规模 —— **实现期拆成两个步骤**
  - `memory-retrieval`：enabled / failed / hasCue / cueCount / tagCount / retrievedCount
  - `memory-injected`：injectedCount / injectedChars
  - **为什么拆开**：回看模式下被回看记录自身的片段也进 MEMORY 层（C3b 决策 4），
    它们**不来自检索**。合成一条会让「检索命中 0 但注入 3 条」看起来自相矛盾
  - **三种情形可区分**（开关关闭 / 检索失败 / 成功但无命中），比原计划的两种更细
  - 验证：`shouldDistinguishMemoryDisabledFailedAndEmpty`
- [x] **T-10** prompt 组装规模
  - 记消息条数 + 是否含工具补充段 / 记忆补充段 / 草稿摘录；**不记 prompt 全文**
  - **已知边界**：mock provider 路径不组装 prompt，因此该步骤只在真实 provider 路径出现。
    这不是采集遗漏，已写进 delta 的 scenario 条件（「GIVEN 后端为某一轮组装提示词」）
  - 验证：`traceMustNotContainAnyDiaryOrConversationContent` 断言轨迹无原文

---

## 阶段 3：action 侧

- [x] **T-11** provider 调用结果与耗时
  - **成功路径的耗时不再被丢弃**（V7：C5 之前 `startedAt` 在成功路径被直接扔掉，
    于是「正常一轮有多慢」完全不可见）
  - 记 model / durationMs / mocked / success；失败记 causeType（**异常类名，不记异常消息**——
    消息可能回带请求内容）
  - mock 路径也记，且标 `mocked=true`——否则「没有 provider 步骤」会被误读成调用丢了
  - 新增 `AgentModelClient.model()`（只读配置值，不涉凭证）
  - 验证：`traceShouldCarryThoughtActionObservationSegments` 断言 `providerDurationMs` 非 null；
    `shouldRecordProviderDurationOnSuccess` / `providerFailureShouldRecordExceptionTypeOnly`
- [x] **T-12** tool_calls 处置
  - `tools` 步骤记 toolsEnabled / returnedCount / proposedCount；
    `tool-rejected` 复用既有 10 个 rejectReason 常量
  - `AgentToolCoordinator.handleProposals` 新增带 trace 的重载，**旧重载保留并委托**——
    既有调用点与测试零改动
  - 验证：`shouldCollectModeAndToolSteps` + 既有 `AgentToolCoordinatorTest` 全绿
- [x] **T-13** fail-closed 丢弃事件（**C3b 残余的补位**）
  - 回看模式下模型仍返回 tool_calls → 轨迹记 `tools-fail-closed` 与丢弃条数
  - C3b 归档时该分支未活体触发，正确性仅由单测覆盖；现在它真发生的那一次能被记下
  - `c5-trace-queries.sql` 第 8 条查询专门用来找它
  - 验证：`shouldCollectModeAndToolSteps` 断言 `discardedCount`

---

## 阶段 4：observation 侧 + 既有缺口补齐

- [x] **T-14** 六层护栏结论与指标
  - 新增 `AgentTraceLayer` 枚举标明「这是哪一道闸」：`reply-content` / `reply-attribution` /
    `material-faithfulness` / `material-content` / `reply-length` / `tool-arguments`
  - **为什么需要它**：`AgentGuardrailVerdict` 只说判定结论，不说哪道闸。
    只看到一个 `unfaithful` 分不清被拦的是回复、素材还是工具参数——而三者处置方式完全不同
  - 复用 `AgentGuardrailVerdict` 的 coverage / maxUncoveredRun / checkedLength（V9：已是脱敏形态）
  - 验证：`guardrailStepShouldCarryMetricsNotText` + `traceShouldCarryThoughtActionObservationSegments`
    + `checkErrorMustBeCorrelatableToSession`
- [x] **T-15** 降级可区分本地兜底与 provider 产出
  - `outcome=DOWNGRADED` + `fallback=local`。**新增 `DOWNGRADED` 而不复用 SUCCESS 的理由**：
    对用户这是一次成功返回（他确实收到了回复），但排查时必须一眼看出这句话不是 provider 的产出。
    混成 SUCCESS 会让轨迹丢掉 C5 最想观测的那类事件
  - 已有失败不被随后的降级覆盖（`downgradeMustNotOverrideAnEarlierFailure`）
  - 验证：`downgradedTurnMustBeDistinguishableFromNormalSuccess`
- [x] **T-16** 补齐 V4：`AgentGuardrailDowngrade.trace` 不再传 null
  - 两个调用点（`applyReplyGuardrail` / `applyMaterialGuardrail`）改为传入真实 sessionId 与 turnNo
  - **属既有缺陷补齐，已在 AGENT_LOG 单列披露**（决策 5）
  - 验证：`replyDowngradeTraceMustCarryRealSessionAndTurn`（ArgumentCaptor 直接断言捕获到的
    sessionId 等于真实会话）+ `attributionDowngradeTraceMustCarryRealSessionAndTurn`
  - **诚实边界**：可观测关闭时这两个参数仍为 null（没有轨迹可取值）。
    这是刻意的——关闭可观测就该完全不产生采集开销。已由
    `disabledObservabilityMustStillDowngradeButLeaveNoTrace` 固定该行为并说明
- [x] **T-17** 补齐 V5：checker 内部 fail-closed 可关联会话 —— **用轨迹解决，未改 checker 签名**
  - 原计划是把 sessionId 传进三个 checker。**实现期改为不动它们**，理由是有更小的解法：
    `CHECK_ERROR` 本来就以 `AgentGuardrailVerdict` 返回给调用方，而调用方现在会
    `trace.guardrail(layer, verdict)`——轨迹里就有 `layer=reply-content, violation=check-error`
    以及所属 sessionId / turnNo / attemptNo。**关联已经成立**，不需要改护栏签名
  - 这样做同时守住了范围：C5 承诺「不改 Agent 行为」，给三个 checker 加参数会让
    它们的全部调用点与单测跟着改，diff 会混进一批与可观测无关的护栏改动
  - 保留的已知缺口：checker 内部那行 `log.warn(... cause=...)` 本身仍无 sessionId。
    它现在是轨迹的**冗余**副本而非唯一线索，**如实记录，不再单独处理**
  - 验证：见 T-14 的 checker 异常用例（断言轨迹含 `violation=check-error` 且可关联会话）

---

## 阶段 5：版本字段与配置

- [x] **T-18** `prompt_version` / `policy_version` 由内容哈希派生（N6 / 决策 6）
  - `AgentTraceVersions`：prompt 版本取 `AgentPromptBuilder.promptTemplateFingerprintSource()`，
    policy 版本取护栏条款文案 + 全部规则词表；SHA-256 前 8 位，前缀 `p` / `g`
  - **刻意不缓存**：缓存会让「改文案 → 版本变」依赖重启，等于把手工 bump 的问题换个形式带回来
  - **顺带提取两处内联文案为常量**（`OUTPUT_REQUIREMENT` / `DRAFT_EXCERPT_LABEL`），
    **文字逐字未改**。目的是让它们能被指纹覆盖——留在方法体内则改了它版本号不会变
  - 在 `promptTemplateFingerprintSource` 上写了维护约定：新增进 prompt 的常量文案须一并列入
  - 验证：`policyVersionShouldChangeWhenGuardrailTextChanges`（改文案 → 版本变，还原 → 版本回）
    + `promptVersionShouldNotBeAffectedByGuardrailThresholds`（两个版本号不互相污染，
    否则 C6 无法区分「改了话术」与「改了阈值」）
- [x] **T-19** `model` 字段就位；`usage` / token 用量**未填，只留位置**（决策 8）
  - 新增 `AgentModelClient.model()`，只读配置值
  - **未动响应解析**——那是 C5「只挂痕迹不改行为」的第一个破口
- [x] **T-20** 配置：`app.agent.observability.*`
  - `enabled`（默认 true）+ `retention-days`（默认 90）；命名与 `memory.*` / `review.*` 风格一致
  - **无采样率**（N3 / 决策 3），并在类注释里写明这是已获批的对蓝图缓解措施的偏离
  - `retention-days<=0` 解释为「不清理」而非「删除全部」——后者的误读代价太大
  - 未引入新依赖、未新增 secret 字段
- [x] **T-21** 开关关闭时不静默
  - 关闭 → `traceSink.traceDisabled(sessionId)` 留 info，且不创建收集器
  - 验证：`disabledObservabilityShouldBehaveAsBeforeC5`（对话仍 SUCCESS 且无轨迹）
    + `disabledObservabilityMustStillDowngradeButLeaveNoTrace`（降级照常发生）

---

## 阶段 6：查询侧与场景示例

- [x] **T-22** 按 `sessionId` 取回完整轨迹
  - `AgentTurnTraceMapper.selectBySessionId`（按 turn_no / attempt_no / id 正序）
    + `selectRecentByUserId` + `countBySessionAndTurn` + `deleteCreatedBefore`
  - `backend/sql/mysql/c5-trace-queries.sql`：9 条只读排查查询，**未加任何端点**（N4）
  - 验证：`tracesShouldBeQueryableBySessionInOrder`（3 轮按序取回）
- [x] **T-23** 三个场景的链路示例（蓝图验收项）
  - 正常轮：`traceShouldCarryThoughtActionObservationSegments`
  - 护栏降级轮：`downgradedTurnMustBeDistinguishableFromNormalSuccess`
  - provider 不可用轮：`shouldRecordTraceWhenProviderUnavailable`
  - 额外两条：回看轮 `reviewChatTurnShouldBeDistinguishableInTrace`、
    同轮重试 `retriedTurnShouldBeDistinguishableFromNewTurn`
- [x] **T-24** 保留与清理（N7 / 决策 11）
  - 三个外键 CASCADE（session / user / record）+ `deleteCreatedBefore` + 脚本第 9 条（先 COUNT 再 DELETE）
  - **未引入定时任务**
  - 验证：`tracesShouldBeCascadeDeletedWithUser` / `purgeShouldOnlyRemoveTracesOlderThanRetention`

---

## 阶段 7：隐私断言（本刀最硬的一条）

- [x] **T-25** 痕迹不含原文的**直接断言测试**（验收 14）
  - 用特征串 `紫罗兰色的旧铁皮盒子` 同时埋进记录正文与用户发言，跑完整一轮
  - 断言分两层：① 实体的每个文本字段都不含该串；② **再直接 SQL 查一遍全部文本列**——
    防「实体没带但落库带了」这种映射级泄漏
  - 回看路径单独一条（`reviewTraceMustNotContainRecordContent`）：
    它把记录正文注入 MEMORY 层，是原文最容易漏进轨迹的路径
- [x] **T-26** 涉及文本的字段只以长度 + 计数表达
  - 收集器在**类型层**拒绝任意文本：`step(...)` 可变参数入口是私有的，
    公开方法的 String 形参只有 7 个且各有白名单说明
  - 验证：`collectorMustNotAcceptArbitraryTextParameters` 用反射守护该白名单——
    将来有人加 `replyText(String)` 会被当场拦住，而不是等某次排查时发现轨迹里躺着日记
  - **实现期修正**：原计划用哈希前缀表达文本规模，实际发现**不需要**——
    轨迹里没有任何需要「指向某段具体文本」的字段，长度与计数已足够。
    不引入哈希是更强的隐私姿态（连不可还原的摘要都没有）
- [x] **T-27** 产品 API 不返回轨迹
  - 验证：`sessionResponseMustNotExposeTrace` 反射断言 `AgentSessionVO` 无任何含 trace 的字段

---

## 阶段 8：回归与 spec delta

- [x] **T-28** 后端全量回归：**533 tests PASS / 2 skipped**，BUILD SUCCESS
  - 496 基线 + 37 新增 = 533，**零回归**；2 skipped 仍是那两个环境门控的真实 provider 探针
  - **既有断言零修改**。`AgentChatServiceImplTest` 的唯一改动是构造签名补两个新依赖
    （已核 diff：只加 import / `@Mock` / 两个构造参数，无任何断言变化）
- [x] **T-29** 范围守护证明：Agent 对话行为零改动
  - 逐行审过 `AgentPromptBuilder` 的 diff：**prompt 文案逐字未改**，
    两处提取只改变声明位置（内联文本块 → 常量），组装结果完全相同
  - 逐行审过 `AgentChatServiceImpl` 的全部删除行：均为「同一语句加了 trace 参数」
    或「分支被重排但语义等价」，**无任何业务逻辑被删除**
  - `sendMessage` 的分支重排是为了让 `turnNo` 在建收集器前先确定；
    阶段推进、落库顺序、失败重试判定与原实现逐条对应
  - 未改护栏阈值 / 记忆检索策略 / 工具白名单 / 回看逻辑；**前端零改动**
- [x] **T-30** `agent-runtime` delta
  - **MODIFIED 四条**「C2/C4/C3a/C3b 范围内的可观测能力」scenario，
    逐条比对 baseline 原文后改为指向本刀条款（保留阶段范围声明，不删——
    范围声明本身是历史事实，删掉就看不出能力何时到位）
  - **ADDED 8 条 Requirement**：每轮一条轨迹（含「开场不构成一轮」）、覆盖早退路径、
    排除原文、fail-open、开关不静默、版本锚点、可按会话查询、C5 范围边界
  - 实现期按实际行为回改了三处 scenario：prompt 组装条件化、回复裁剪、素材失败不改轮次结论
- [x] **T-31** `backend-core` delta：7 条 —— 归属与级联、不共享消息唯一键、可查询且不经产品接口、
  字段结构化、护栏降级痕迹可关联（V4）、配置归属、引入轨迹不改既有行为
- [x] **T-32** `agent-collaboration` delta：3 条。**该 spec 的 baseline 原本没有 Accepted From 段落**
  （C1–C3 的 Agent 条款实际全落在 `agent-runtime`），故本 delta 是它第一次承载产品 Agent 条款
- [x] **T-33** `v2-product-scope` delta：2 条（不可见于用户 / 不演化为分析面）
- [x] **T-34** 确认 `miniapp-core` **无 delta**（前端零改动，`git status` 无 frontend 文件）

---

## 阶段 9：闸门 3 —— **已授权并执行**（2026-07-30）

> 探针：`agent/trace/C5RealProviderProbeTest`，由 `C5_REAL_PROBE=1` 门控，默认跳过（已验证）。
> **真实调用 6 次 / 预算 10**：写作引导 3 轮 + 回看 3 轮。刻意不推进到 CLOSING 以省下调用余量。
> provider=deepseek，model=deepseek-v4-pro，只用自造内容，**未使用用户真实日记**。
> 落库走 H2 测试库，未触碰本地 MySQL 真实数据。

- [x] **T-35** 真实链路轨迹完整性 **PASS**
  - **6 轮全部三段齐备**：`mode` / `stage-decision|stage-retained` / `memory-retrieval` /
    `prompt` / `provider` / `guardrail` / `tools` 七项在每一轮均为 true
  - **本探针刻意走完整 `AgentChatService.sendMessage`** 而不是只调 model client
    （C3/C4 探针的做法）。理由：C5 要验的是**采集点有没有漏**，而漏采集恰恰发生在编排层，
    只调 model client 验不到。这一点在真实链路上得到确认
  - 写作引导三轮 `reason` 均为 `ADVANCE`（无 REASK）；版本锚点 6 轮稳定同值
    `promptV=pea5c33ea` / `policyV=gc03e94cf`
  - 回看三轮 `stage=REVIEW` 且 **`stage_reason=null`** —— 无阶段机时确实不伪造判定结论，
    与设计一致
- [x] **T-36** 耗时量级 **PASS**：**min 4571ms / avg 6476ms / max 8467ms**（6 轮）
  - 与 C3b 观察到的秒级响应对齐，量级合理
  - **这项数据在 C5 之前完全不存在**（成功路径的 `startedAt` 被直接丢弃）
- [x] **T-37** fail-closed **未活体触发** —— **如实记为未验证**
  - 6 轮中模型均未在无工具模式下返回 tool_calls，因此该分支未被真实执行
  - 其正确性仍只由单测覆盖（`shouldCollectModeAndToolSteps`）
  - 与 C3b 的同一残余处理方式一致：C5 做到的是「它真发生时能被记下」，
    **不等于「已观察到它发生」**，不得写成已验证
- [x] **T-37b 隐私复核（探针额外项）PASS**
  - 单测已用特征串验过，探针再用**真实模型产出**复核一次——真实回复的措辞不可预测，
    是「实现里某处不小心把文本塞进轨迹」最可能暴露的场景
  - 用 8 字滑窗扫轨迹全部文本列，两个会话均 `leaked=false`

---

## 收口

- [x] **T-38** 输出 Required Output
- [x] **T-39** 更新 `.ai/ACTIVE_TASK.md` Current Progress；追加 `.ai/AGENT_LOG.md`
- [x] **T-40** `closeout.md`
- [x] **T-41** 用户验收 → delta 接受进 baseline → 归档 → `ACTIVE_TASK` → IDLE（2026-07-30 完成）
  - `agent-runtime`：四条 MODIFIED 逐条落（已核对：新措辞 4 处、无残留旧措辞）+ 8 条 ADDED
  - `backend-core`：7 条 + **一条 Type B 条款**（前端超时须大于后端 AI 超时）
  - `agent-collaboration`：3 条；`v2-product-scope`：2 条；`miniapp-core` 无 delta
  - **Phase 1（C1–C5）至此全部完成**；下一步是蓝图 v1.2 校准会（v1.2 草案 §0.2 与 §8 的清单）

---

## 范围守护自检（每个 task 完成时过一遍）

- 未改 Agent 任何对话行为（阶段 / prompt / 阈值 / 检索 / 工具 / 回看）
- 未改前端任何文件
- 未改认证与签发逻辑（`AuthRole.ADMIN` 无签发路径的事实如实记录，不靠改认证绕开）
- 未做 C6 Eval / C7 韧性 / C8 时间智能
- 未动 R2（引导与素材质量）、未动 R9（检索相关性）
- 未放宽任何护栏阈值
- 未改 `uk_agent_message_session_turn_role`
- 未引入 logback 配置 / 日志聚合 / actuator / 定时任务
- 未引入新依赖；`pom.xml` / `package` / lockfile 未改
- 未把日记原文 / 对话原文 / 记忆片段写入痕迹或日志
- 除 T-16/T-17 的既有缺口补齐（已披露）外，未顺手改动无关代码
- 既有断言零修改
- 未执行 `git add` / `commit` / `push`
- **未使用波及未跟踪文件的 git 操作**（不用 stash / clean / reset --hard；只用显式 `git add <path>`）
  - 工作区现有未跟踪产物不得擅自提交或移动：`Docs/agent-iteration/architecture/`、`iteration-blueprint-v1.2-draft.md`、`.kiro/skills/`
