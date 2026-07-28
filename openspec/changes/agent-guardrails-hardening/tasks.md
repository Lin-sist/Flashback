# Agent Guardrails Hardening（C4）· Tasks

> Change ID：`agent-guardrails-hardening`
> 阶段：**规划闸（闸门 1）—— Q1–Q7 待用户确认**
> 核心机制：忠实度双指标机械判定（覆盖率 + 最长连续未覆盖片段），零外调、可回归
> 规则：闸门 1 未批准前，**T-03 之后的一切业务代码任务禁止开工**；闸门 2 未取得前不写业务代码；闸门 3 未取得前不做真实外调。
> 完成即把 `- [ ]` 改为 `- [x]`，并按 `AGENTS.md` 要求追加 `.ai/AGENT_LOG.md`。

---

## Gate 0 · 规划闸（本阶段）

- [x] T-00 创建 `openspec/changes/agent-guardrails-hardening/`，产出 proposal / design / tasks / spec delta
- [x] T-01 记录开工锚点 `b64296d`；确认工作树干净（仅未跟踪 `.kiro/skills/`，非本 change 产物）
- [x] T-02 Q1–Q7 定稿（2026-07-28 用户按推荐全部同意）
  - [x] Q1 忠实度判定机制 → **机械覆盖率双指标闸**；否决 LLM-as-judge
  - [x] Q2 忠实度闸覆盖范围 → `text` + 素材 + `askText`（后者宽判定）
  - [x] Q3 违规处置方式 → 分路径：提议拒绝 / 素材丢弃 / 回复兜底
  - [x] Q4 阈值与可配置性 → `app.agent.guardrail.*`；**初值已在实现期实测校准**（见 design §3.3）
  - [x] Q5 诊断 / 代决 / 相称性检查形态 → 规则单一声明源 + 只查新增区段；相称性不纳入
  - [x] Q6 降级痕迹落点与前端可见性 → 复用 `agent_tool_call.failure_type` + 结构化日志；前端不可见
  - [x] Q7 spec delta 落点 → `agent-runtime` + `backend-core` + `v2-product-scope`
  - [x] 附带：R5 `propose_unlock_at` **保留**
  - [x] 附带：R3 C2 遗留真机手验在 C4 闸门 3 一并补齐
- [x] T-02b 更新 `.ai/ACTIVE_TASK.md`：Status=ACTIVE、指针指向本 change、初始化 Current Progress
- [x] T-02c 更新蓝图 `Docs/agent-iteration/roadmap/iteration-blueprint.md` §3.2 + §7：登记 C4 前移
- [x] **T-02d 闸门 1：用户批准规划放行**（2026-07-28）

> **本阶段结束标志**：零业务代码改动。

---

## Gate 1 · 实现授权后（闸门 2）

- [x] T-03 取得用户明确 **实现授权**（闸门 2）（2026-07-28；同时授权实现期自主执行，无需逐条请示）
  - 闸门 3（真实 provider 外调）**未授权** → 实现期全部走 mock provider，**零外调**

### A. 忠实度判定核心（纯逻辑，零外调；先做，是全 change 地基）

- [x] T-04 新建 `agent/guardrail/AgentTextNormalizer`：归一化（去空白、全半角统一、去标点、大小写折叠）
- [x] T-05 新建 `agent/guardrail/AgentSourceCorpus`：由本会话 `role=USER` 消息构造归一化字符序列 + n-gram 集合；**全量不设窗口**（design §3.1）；只在内存中，不落库
- [x] T-06 新建 `agent/guardrail/AgentFaithfulnessChecker`：逐位覆盖标记 → `coverage` + `maxUncoveredRun` 双指标判定；返回 `AgentGuardrailVerdict`（含结构化指标）
  - 实现期新增 `checkQuotedFragment(...)`：引号片段专用严判据（design **决策 13**）
- [x] T-06b 新建 `agent/guardrail/AgentCoverageProfile`：逐字覆盖画像，供忠实度判定与诊断分区共用（design 决策 4）
- [x] T-07 新建 `AgentGuardrailVerdict` + `AgentGuardrailViolation`：违规类型含 `UNFAITHFUL` / `FABRICATED_QUOTE` / `DIAGNOSTIC` / `FAKE_ACTION` / `CHECK_ERROR`
- [x] T-08 单测：**R1 真实样本回归**——两句真话 + 45 字虚构句 → 判 UNFAITHFUL
- [x] T-09 单测：**不误伤**——语序调整、删口头语、标点变化、多条用户消息拼接、接缝插入连接词 → 判 FAITHFUL
- [x] T-10 单测：**双指标必要性**（验收标准 4 的证据）——R1 样本在「仅 coverage」判据下通过、加上 `maxUncoveredRun` 后被拦
- [x] T-11 单测：归一化等价性（全半角 / 标点 / 空白 / 大小写 / 确定性）
- [x] T-12 单测：短文本不做覆盖率判定，但连续未覆盖片段判据仍生效
- [x] T-13 单测：**fail-closed**——检查内部抛异常时返回 `CHECK_ERROR` 而非 PASS
- [x] T-13b 单测：Agent 自己的消息**不得**成为合法来源（否则忠实度闸自我失效）

### B. 护栏规则声明源与配置

- [x] T-14 新建 `agent/guardrail/AgentGuardrailRules`：唯一声明源，承载 prompt 文案 + 检查规则 + **正向行为表达**（蓝图 §6.4）
- [x] T-15 `AgentGuardrailPolicy.guardrailClause()` 改为委托 `AgentGuardrailRules`；**`enforceReplyLength` 行为未动**
- [x] T-16 `AgentPromptBuilder.buildToolSupplement()` / `buildMaterialMessages()` 的文案改为取自 `AgentGuardrailRules`；prompt 实际文字内容未改（仅工具段补一句「不能补写没说过的内容」，与既有约束同向）
- [x] T-17 `AppAgentProperties` 新增 `guardrail` 子配置；无凭证字段
  - **阈值经实测校准**：`min-coverage` 0.60 → **0.35**（实测合法整理覆盖率仅 0.500，0.60 会误伤），见 design §3.3
- [x] T-18 既有 `AgentGuardrailPolicyTest`（5）/ `AgentPromptBuilderTest`（10）全部通过，**未改动任何断言**（仅按新构造签名补参）

### C. 接入工具提议路径（复用 C2 拒绝通道）

- [x] T-19 `AgentToolValidationResult` 新增 `REASON_UNFAITHFUL_ARGS` / `REASON_FABRICATED_QUOTE` / `REASON_ASK_TEXT_VIOLATION`（加常量，未改结构）
- [x] T-20 `AgentToolValidator.validate(...)` 扩签名注入来源语料；忠实度闸接在长度 / 边界校验之后（design 决策 2：校验点唯一）
  - **刻意不提供无语料重载**——那会造出一条绕过忠实度检查即可产生提议的路径
- [x] T-21 `AgentToolCoordinator`：构造来源语料并传入；UNFAITHFUL 走既有 `persistGuardRejected(...)` 落 `REJECTED_BY_GUARD` + `failureType`
- [x] T-22 `askText` 宽判定：诊断 / 代决检查 + **伪引用检查**（引号片段须在来源中有覆盖）
- [x] T-23 单测：UNFAITHFUL 提议被拒、无 `pendingToolCall`、**本轮 reply 正常返回**、执行层零触碰
- [x] T-24 `AgentToolValidatorTest` 新增 C4 段：R1 参数被拒、合法整理放行、伪引用被拒、诊断 askText 被拒；`AgentToolCoordinatorTest` 断言审计落 `REJECTED_BY_GUARD`

### D. 接入素材路径

- [x] T-25 `AgentChatServiceImpl.generateMaterial(...)`：素材产出后过忠实度闸 + 内容检查；VIOLATION → `materialDraft = null` + 结构化痕迹（复用既有「素材可缺失」语义，零前端改动）
- [x] T-26 `AgentMaterialGuardrailTest`：仅拼接用户原话 → 放行；加入 Agent 共情总结 → 拒绝；抄 Agent 回复 → 拒绝

### E. 诊断 / 代决后置检查与兜底降级

- [x] T-27 新建 `agent/guardrail/AgentContentChecker`：复用覆盖画像做「有来源 / 新增」分区，**只在新增区段**匹配规则（design 决策 4）
- [x] T-28 诊断类规则：病症名 + 判定式表述 + 医疗建议；命中 → 回复替换为安全兜底
- [x] T-29 代决类规则：谎报已执行 seal / unlock / delete 的表述；命中 → 回复替换为安全兜底
- [x] T-30 新建 `agent/guardrail/AgentGuardrailDowngrade`：兜底回复为**本地常量**，痕迹标记 `fallback=local` 以与真实回复区分（验收标准 12：不冒充 provider 成功）
- [x] T-31 接入回复路径：`normalizeReplyShape` → `AgentContentChecker` → `enforceReplyLength`（长度硬上限仍生效）
- [x] T-32 单测：用户原话含病名 → **不误伤**；Agent 新增区段含病名 → 命中兜底
- [x] T-33 单测：「我已经帮你封存了」被拦并兜底；「要你自己确认」正常放行
- [x] T-34 相称性检查**未实现**（Q5 定稿：不纳入 C4）

### F. 边界用例测试集（蓝图硬要求）

- [x] T-35 新建 `AgentGuardrailBoundaryCaseTest`，四类场景各 3 例：诊断性输入、篡改 / prompt injection 尝试、过长输出、代决尝试
- [x] T-36 R1 忠实度回归纳入同一测试集（`R1FaithfulnessRegression` 嵌套类），可独立运行、零外调
- [x] T-37 隐私断言：`metrics()` 只含数值指标，不含候选文本与用户原话片段
- [x] T-38 回归：`mvn -B -o test` → **396 tests / 0 failures**（339 基线不回归，未改动任何 C1/C2 既有断言）

### G. 前端

- [x] T-39 Q6 = 不可见 → **前端零改动**（护栏对用户不可见，不破坏「朋友」感）
- [x] T-40 前端无改动，故未执行 `type-check` / `build:mp-weixin`；理由已在 AGENT_LOG 说明

### H. Spec delta 校对

- [x] T-41 核对实现与 `specs/agent-runtime/spec.md` delta 一致，含 **MODIFIED** 两条 scenario（G23）
- [x] T-42 核对 `backend-core` / `v2-product-scope` delta 与实际契约一致
  - 实现期偏差已补记为 design 决策 13（引号片段专用判据），未偏离 spec 条款语义

---

## Gate 2 · 外调授权后（闸门 3）

- [ ] T-43 取得用户明确 **外调授权**（闸门 3），确认预算上限 ≤ 30 次；**并先确认本地 `AI_PROVIDER` 取值**（C1 流程偏差不得重演）
- [ ] T-43b 建议先完成 R6 的 5 项凭证轮换（用户执行）后再联调
- [ ] T-44 启动本地 MySQL80（StartType=Manual）
- [ ] T-45 真实 provider 复现 R1 场景：观察模型是否仍增写；验证增写被忠实度闸拦下
- [ ] T-46 阈值校准：用真实整理样本验证是否误伤；按 design §5 处置顺序调整（放宽 `max-uncovered-run` → 降 `min-coverage` → 缩 `min-checked-length` 适用范围）；**任何情况下不关闭忠实度闸换通过率**
- [ ] T-47 微信小程序手验：忠实度拒绝后对话正常继续；兜底回复观感克制、不突兀
- [ ] T-48 **补齐 C2 遗留 T-40~T-42**（R3）：提议 → 确认执行 → 拒绝 → 重复确认幂等 → 记录已封存时执行失败；真实库核验 `agent_tool_call` 无原文
- [ ] T-49 全部验证结果（PASS / FAIL / SKIPPED + 原因）与阈值校准结论追加 `.ai/AGENT_LOG.md`

---

## Gate 3 · 收口

- [ ] T-50 输出 `AGENTS.md` Required Output 全字段（modified files / what changed / verification / skipped reason / `git diff --stat` / scope safety / remaining risks）
- [ ] T-51 用户审 diff 与验收
- [ ] T-52 delta 接受进 `openspec/specs/`（含 `agent-runtime` 两条 MODIFIED scenario）
- [ ] T-53 change 归档到 `openspec/changes/archive/<date>-agent-guardrails-hardening/` + 写 `closeout.md`
  - closeout 必须显式记录残余风险：**大量复用原话词汇的虚构可能绕过双指标**（design 决策 1）
- [ ] T-54 `.ai/ACTIVE_TASK.md` → IDLE，Current Progress 归档；R1 关闭、R4 关闭、R5 按 Q 结论关闭、R3 按 T-48 结果关闭
- [ ] T-55 提交责任：**Agent 代为提交**（2026-07-28 用户授权，验收由用户进行）；`push` 未授权，不执行

---

## 范围守护（每个 task 完成时自检）

- 未做 Memory / 历史检索 / 跨记录关联（C3）；忠实度来源集合**只含当前会话用户消息**
- 未做决策链路查询端点 / 可观测面板（C5）
- **未调引导 prompt 话术、未改素材合成策略**（R2 已明确延后；design §3.5 只做关系说明）
- 未引入 LLM-as-judge 作为护栏主路径；忠实度判定零外调
- 未扩大工具白名单（只可能收紧）
- 未放宽任何已接受约束：prompt 护栏、白名单、二段式确认、长度硬裁剪全部保留原效力
- 未改 C1/C2 五个 Agent 端点的既有字段语义
- 未改 `complete()` 与三个单轮 AI 端点的 `json_object` 链路
- 未引入 FC → 自研协议降级路径（C2 决策 1 延续）
- 未引入 MCP / Spring AI / LangChain4j（C2 决策 11 延续）
- **未引入第三方分词 / 相似度依赖；未改 package / lockfile**（design 决策 9）
- 未新增表、未新增凭证字段
- 未把日记原文 / 对话原文 / 未覆盖片段内容写进审计表或日志
- 未改三 Tab、未改用户可见命名、未做视觉大改
- 未为 C4 便利修改 C1/C2 既有测试断言
