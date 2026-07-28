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
- [ ] T-02 Q1–Q7 定稿（**待用户确认**）
  - [ ] Q1 忠实度判定机制 →（推荐：机械覆盖率双指标闸；否决 LLM-as-judge）
  - [ ] Q2 忠实度闸覆盖范围 →（推荐：`text` + 素材 + `askText` 宽判定）
  - [ ] Q3 违规处置方式 →（推荐：分路径——提议拒绝 / 素材丢弃 / 回复兜底）
  - [ ] Q4 阈值与可配置性 →（推荐：`app.agent.guardrail.*` + 保守初值 + 实现期校准）
  - [ ] Q5 诊断 / 代决 / 相称性检查形态 →（推荐：规则单一声明源 + 只查新增区段；相称性不纳入）
  - [ ] Q6 降级痕迹落点与前端可见性 →（推荐：复用 `agent_tool_call.failure_type` + 结构化日志；前端不可见）
  - [ ] Q7 spec delta 落点 →（推荐：`agent-runtime` + `backend-core` + `v2-product-scope`；Q6 若选可见则补 `miniapp-core`）
  - [ ] 附带：R5 `propose_unlock_at` 去留（推荐保留）
  - [ ] 附带：R3 C2 遗留真机手验是否在 C4 闸门 3 一并补齐（推荐是）
- [ ] T-02b 更新 `.ai/ACTIVE_TASK.md`：Status=ACTIVE、指针指向本 change、初始化 Current Progress
- [ ] T-02c 更新蓝图 `Docs/agent-iteration/roadmap/iteration-blueprint.md` §7 修订记录：登记 C4 前移（依据 §3.2 + 2026-07-28 用户批准 + R1 实证）
- [ ] **T-02d 闸门 1：用户批准规划放行**

> **本阶段结束标志**：零业务代码改动。

---

## Gate 1 · 实现授权后（闸门 2）

- [ ] T-03 取得用户明确 **实现授权**（闸门 2）
  - 注意：闸门 3（真实 provider 外调）需另行授权 → 实现期全部走 mock provider，零外调

### A. 忠实度判定核心（纯逻辑，零外调；先做，是全 change 地基）

- [ ] T-04 新建 `agent/guardrail/AgentTextNormalizer`：归一化（去空白、全半角统一、去标点、大小写折叠）
- [ ] T-05 新建 `agent/guardrail/AgentSourceCorpus`：由本会话 `role=USER` 消息构造归一化字符序列 + n-gram 集合；**全量不设窗口**（design §3.1）；只在内存中，不落库
- [ ] T-06 新建 `agent/guardrail/AgentFaithfulnessChecker`：逐位覆盖标记 → `coverage` + `maxUncoveredRun` 双指标判定；返回 `AgentGuardrailVerdict`（含结构化指标）
- [ ] T-07 新建 `agent/guardrail/AgentGuardrailVerdict`：`PASS` / `VIOLATION(type, reason, metrics)`；违规类型至少含 `UNFAITHFUL` / `DIAGNOSTIC` / `FAKE_ACTION`
- [ ] T-08 单测：**R1 真实样本回归**——两句真话 + 约 45 字虚构句 → 判 UNFAITHFUL
- [ ] T-09 单测：**不误伤**——语序调整、删口头语、标点变化、多条用户消息拼接 → 判 FAITHFUL
- [ ] T-10 单测：**双指标必要性**（验收标准 4 的证据）——R1 样本在「仅 coverage」判据下通过、加上 `maxUncoveredRun` 后被拦
- [ ] T-11 单测：归一化等价性（全半角 / 标点 / 空白 / 大小写）
- [ ] T-12 单测：短文本（< `min-checked-length`）不做覆盖率判定，避免小样本抖动
- [ ] T-13 单测：**fail-closed**——检查内部抛异常时返回 VIOLATION 而非 PASS

### B. 护栏规则声明源与配置

- [ ] T-14 新建 `agent/guardrail/AgentGuardrailRules`：唯一声明源，承载 prompt 文案 + 检查规则 + **正向行为表达**（蓝图 §6.4）
- [ ] T-15 `AgentGuardrailPolicy.guardrailClause()` 改为委托 `AgentGuardrailRules`；**`enforceReplyLength` 行为不动**
- [ ] T-16 `AgentPromptBuilder.buildToolSupplement()` / `buildMaterialMessages()` 的护栏文案改为取自 `AgentGuardrailRules`；**prompt 实际文字内容不改**（避免踩 R2 延后边界）
- [ ] T-17 `AppAgentProperties` 新增 `guardrail` 子配置：`faithfulness-enabled` / `faithfulness-ngram-size=4` / `min-coverage=0.60` / `max-uncovered-run=12` / `min-checked-length=12`；无凭证字段
- [ ] T-18 单测：既有 `AgentGuardrailPolicyTest` / `AgentPromptBuilderTest` 保持通过（**若必须改断言则停下请示**）

### C. 接入工具提议路径（复用 C2 拒绝通道）

- [ ] T-19 `AgentToolValidationResult` 新增拒绝原因常量 `REASON_UNFAITHFUL_ARGS`（加常量，不改结构）
- [ ] T-20 `AgentToolValidator.validate(...)` 扩签名注入来源语料；在长度 / 边界校验之后接忠实度闸（design 决策 2：校验点唯一）
- [ ] T-21 `AgentToolCoordinator`：构造来源语料并传入；UNFAITHFUL 走既有 `persistGuardRejected(...)` 落 `REJECTED_BY_GUARD` + `failureType`
- [ ] T-22 `askText` 宽判定（若 Q2 = c）：诊断 / 代决检查 + **伪引用检查**（引号包裹片段须在来源中有覆盖）
- [ ] T-23 单测：UNFAITHFUL 提议被拒、无 `pendingToolCall`、**本轮 reply 正常返回**（对话不中断）
- [ ] T-24 集成测试（mock provider + H2）：不忠实提议 → 审计落 `REJECTED_BY_GUARD`、记录零变更

### D. 接入素材路径（若 Q2 纳入）

- [ ] T-25 `AgentChatServiceImpl.generateMaterial(...)`：素材产出后过忠实度闸；VIOLATION → `materialDraft = null` + 结构化日志（复用既有「素材可缺失」语义，零前端改动）
- [ ] T-26 集成测试：不忠实素材 → `materialDraft` 为 null、会话仍正常 ENDED、前端无回填入口

### E. 诊断 / 代决后置检查与兜底降级

- [ ] T-27 新建 `agent/guardrail/AgentContentChecker`：复用覆盖标记做「有来源 / 新增」分区，**只在新增区段**匹配规则（design 决策 4）
- [ ] T-28 诊断类规则：病症名 + 判定式表述；命中 → 回复替换为安全兜底
- [ ] T-29 代决类规则：谎报已执行 seal / unlock / delete 的表述；命中 → 回复替换为安全兜底
- [ ] T-30 新建 `agent/guardrail/AgentGuardrailDowngrade`：安全兜底回复为**本地常量**，痕迹中可与真实回复区分（验收标准 12：不冒充 provider 成功）
- [ ] T-31 接入回复路径：`normalizeReplyShape` → `AgentContentChecker` → `enforceReplyLength`（长度硬上限仍生效）
- [ ] T-32 单测：用户原话含病名 → **不误伤**；Agent 新增区段含病名 → 命中兜底
- [ ] T-33 单测：「我已经帮你封存了」被拦并兜底
- [ ] T-34 相称性检查**不实现**（Q5 推荐）；若用户在 Q5 要求纳入则本项转为实现任务

### F. 边界用例测试集（蓝图硬要求）

- [ ] T-35 新建边界用例测试类，四类场景各至少 2 例：诊断性输入、篡改 / prompt injection 尝试、过长输出、代决尝试
- [ ] T-36 R1 忠实度回归用例纳入同一测试集，可独立运行
- [ ] T-37 隐私断言：痕迹与日志只含结构化指标，**无候选文本、无用户原话、无未覆盖片段内容**
- [ ] T-38 回归：`mvn -B test` **339 项基线全绿**，未改动任何 C1/C2 既有断言

### G. 前端

- [ ] T-39 Q6 = 不可见（推荐）→ **前端零改动**，在 AGENT_LOG 说明；若 Q6 选可见则改为在 `AgentChatSheet.vue` 内做克制表达
- [ ] T-40 `type-check` + `build:mp-weixin`（有改动时执行；无改动则说明）

### H. Spec delta 校对

- [ ] T-41 核对实现与 `specs/agent-runtime/spec.md` delta 一致，含 **MODIFIED** 两条「护栏深度留给后续 change」的 scenario（G23）
- [ ] T-42 核对 `backend-core` / `v2-product-scope` delta 与实际契约一致；有偏差先请示再改 spec

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
