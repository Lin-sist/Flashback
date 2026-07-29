# Tasks：Agent Review Chat（C3 后半刀）

> Change ID：`agent-review-chat`
> 阶段：**规划闸产出完成，闸门 2 待授权**
> N1–N6 定稿（2026-07-29 用户按推荐批准）：同一 Service + 单一模式判定点 / `stage=REVIEW` + 上限 6 / 注入三字段进 MEMORY 层 / 阈值不动靠实测 / 复用会话端点加 purpose / 补 purpose 谓词
> 提交责任：**用户手动提交**（除当轮明确授权外，Agent 不执行 git 写操作）

---

## 闸门检查点

- [x] **闸门 1 · 规划批准**：proposal / design（11 条决策）/ tasks，N1–N6 定稿
- [ ] **闸门 2 · 实现授权**：用户明确允许按本文件实现。**未勾选前不写任何业务代码**
- [ ] **闸门 3 · 外调授权**：与 C3a 合并联调（本刀 ≤ 20 次，含 C3a 顺延合计 ≤ 40 次），须单独授权

---

## 阶段 1：会话模式与开会话路径

- [ ] **T-01** 新增单一模式判定点（design 决策 1 的硬约束）
  - 形态：一个由 `session.getPurpose()` 派生的模式对象/方法，集中回答四件事——是否走状态机、是否下发 tools、是否产素材、prompt 用哪套角色设定
  - **禁止**在 `sendMessage` 里散落四个 `if (purpose == REVIEW_CHAT)`
  - 验证：单测覆盖两种 purpose 各自的模式取值

- [ ] **T-02** `AgentStage` 新增 `REVIEW` 常量（design 决策 2）
  - 检查既有 `switch`：`AgentPromptBuilder.stageGoal` / `buildTurnInstruction`、`AgentStageMachine`。**逐个确认是否需要补分支**，不靠 default 兜混过去
  - `stage` 列为 `VARCHAR(30)`，**无需 DDL**
  - 验证：新增枚举值后既有 472 项全绿

- [ ] **T-03** 开会话校验按模式放宽（V7）
  - 写作引导：仍只允许 DRAFT（既有行为不变）
  - 回看：只允许 **UNLOCKED**；DRAFT 与 SEALED 均拒绝并给出明确原因
  - 验证：四个组合（写作×DRAFT 通过、写作×UNLOCKED 拒绝、回看×UNLOCKED 通过、回看×DRAFT/SEALED 拒绝）各有测试

- [ ] **T-04** `POST /api/agent/sessions` 请求体加 `purpose`（N5）
  - 缺省 `WRITING_GUIDANCE`，既有调用零改动
  - 回看必须携带 `recordId`（无记录的回看无意义）→ 缺失时拒绝
  - 验证：缺省行为与既有测试一致；回看缺 recordId 被拒

- [ ] **T-05** `selectActiveByUserAndRecord` 补 purpose 谓词（N6 / 决策 9）
  - Mapper XML + 接口 + 所有调用点显式传入
  - 验证：同一条记录上两种 purpose 的 ACTIVE 会话互不串（需构造一条同时有两种会话的数据）

---

## 阶段 2：无阶段轮次路径

- [ ] **T-06** 回看轮次推进不经 `AgentStageMachine`（V9 / N2）
  - `stage` 恒为 `REVIEW`；`stageReaskCount` 不被回看逻辑改写
  - 轮次上限走**新增配置** `app.agent.review.max-turns-per-session`，默认 6
  - 验证：回看会话的 stage 与 reaskCount 在多轮后保持不变

- [ ] **T-07** 回看结束语义（N2）
  - 轮次达上限 → 温和收束并 `ENDED`；用户主动 `finish` → `ENDED`；`ENDED` 后追加被拒
  - 验证：三条路径各有测试

- [ ] **T-08** 失败重试语义与 C1 保持一致
  - provider 失败：用户消息保留、Agent 回复不落库、同轮可重试且不重复计数、改内容被拒
  - 验证：沿用 C1 测试形态，在回看路径复验

---

## 阶段 3：无工具与无素材（本刀最易漏的两处）

- [ ] **T-09** `buildToolContext` 按 purpose 短路（**V10，最易漏**）
  - 现状：只按 `session.getRecordId() == null` 判断。回看**恰好绑定记录**，故「无工具」不会自动成立
  - 验证：**直接断言回看路径不下发 tools**（断言 tools 列表为空，而非「恰好没配」）

- [ ] **T-10** 回看中模型返回 tool_calls 时 fail-closed（决策 7）
  - 丢弃提议、留结构化审计、**不落待确认记录、不下发确认条**，本轮回复正常返回
  - 验证：mock provider 强制返回 tool_calls，断言无 `agent_tool_call` 待确认行且回复正常

- [ ] **T-11** 回看路径不产出素材（V13 / 决策 8）
  - 短路 `generateMaterial`；`materialDraft` 恒为 null
  - 验证：回看会话结束后 `materialDraft` 为空，前端无回填入口

- [ ] **T-12** 记录不可变验证
  - 回看对话全程结束后，目标记录 `content` / `location` / `attachments` / `cover` 逐字不变
  - 验证：集成测试对比对话前后的记录快照

---

## 阶段 4：回看上下文与护栏

- [ ] **T-13** 回看 prompt 角色设定（新增，不改既有引导话术 —— R2 边界）
  - 新增回看专用角色设定 + 收束指令；文案取自 `AgentGuardrailRules` 单一声明源风格
  - **不修改** `ROLE_SETTING` 与既有 `stageGoal` 文案
  - 验证：既有 `AgentPromptBuilderTest` 全绿

- [ ] **T-14** 注入 `content` + `ai_summary` + `belief_then`（N3 / 决策 3）
  - 按配置截断；**不注入** `reality_later` / `reply`（时间语义不同，决策 3）
  - 验证：注入内容与截断长度可测

- [ ] **T-15** 三字段 + Memory 片段**全部进 MEMORY 层**（N3 / 决策 4）
  - 用 `AgentLayeredCorpus`：SESSION 层 = 本次对话用户消息；MEMORY 层 = 记录三字段 + `MemoryPort` 片段
  - 验证：直接断言记录内容不在 SESSION 层覆盖范围内（这是时间归属生效的前提）

- [ ] **T-16** Memory 复用（不新增检索实现）
  - `MemoryPort.retrieve` 传 `purpose=REVIEW_CHAT`；排除当前记录本身
  - 无命中 → 只就本条记录聊，不编造关联；检索失败 → 对话继续且护栏不放宽
  - 验证：无命中与失败两条路径各有测试；**断言未新增检索类**

- [ ] **T-17** 护栏全量接入验证（不得放宽）
  - 时间归属、忠实度、诊断、代决、伪引用、长度上限在回看路径逐一生效
  - **阈值默认值不动**（决策 5）：`minCoverage` / `maxUncoveredRun` / `minCheckedLength` / `QUOTE_MIN_COVERAGE` / `minMemoryOnlyRunForAttribution`
  - 验证：每层各有测试；阈值默认值直接断言；**时间归属正例（带归属）与负例（裸复述）都要有**

---

## 阶段 5：测试与既有断言处理

- [ ] **T-18** 回看端到端集成测试（mock provider）
  - 开会话 → 多轮 → 收束；状态范围；跨用户隔离；未登录拒绝
- [ ] **T-19** 改写 `AgentMemoryIntegrationTest.shouldNotCreateAnyReviewChatSession`（V3 / 决策 11）
  - 改为正向断言：**写作引导会话不产生 `REVIEW_CHAT` 用途**
  - **须在 AGENT_LOG 显式披露**这是唯一允许修改的既有断言
- [ ] **T-20** 后端全量回归：**472 项基线**（1 项环境门控 skip 除外），除 T-19 外既有断言零修改

---

## 阶段 6：前端

- [ ] **T-21** `agentService.ts` 类型与调用扩展
  - `startOrResume` 支持 `purpose`；`AgentSession` 类型体现回看无 `pendingToolCall` / `materialDraft` 的语义
- [ ] **T-22** `ReviewChatSheet.vue`（`record-detail/components/`）
  - **不复用**带工具确认的 `AgentChatSheet`（V16）；结构上不存在工具确认条与素材回填入口
  - 可抽共享消息壳，但不得把工具/素材逻辑带进来
- [ ] **T-23** UNLOCKED 分支新增克制入口 + 与 `reply-overlay` **互斥**（V15 / Q5）
  - 被动触发：不弹窗、不自动展开
  - 状态互锁：一个打开时另一个不可开启；**不改 `reply-overlay` 既有行为**
- [ ] **T-24** 显式失败态与不可用态（克制表达，不伪装成功）
- [ ] **T-25** `type-check` + `build:mp-weixin` 通过；三 Tab 与用户可见命名不变

---

## 阶段 7：spec delta

- [ ] **T-26** `agent-runtime`：
  - **MODIFIED** `Writing Guidance Must Target Draft Records Only`（V8）
  - **MODIFIED** C3a「C3a 范围内的回看对话」scenario（V8）
  - 新增回看会话、无阶段轮次、无工具 fail-closed、无素材、回看来源分层条款
- [ ] **T-27** `backend-core`：会话 purpose 行为分支、回看轮次配置、端点 purpose 参数
- [ ] **T-28** `miniapp-core`：**新增**回看对话 UI 条款（既有 4 条以「记录编辑页」为主语，不改写）
- [ ] **T-29** `v2-product-scope`：回看对话产品行为与气质边界

---

## 阶段 8：合并闸门 3（单独授权后）

- [x] **T-30** 执行 `backend/sql/mysql/c3-agent-memory.sql`（2026-07-29 完成）
  - **该前置项确实是用户手验报错的根因**：用户手验时记录页与回看页均报「系统异常: api/agent/sessions」。
    原因是 C3a 的 mapper 已把 `purpose` 写进列清单与 insert，而本地库没有该列 → **写作引导对话也一起 500**（不只回看）。
    属部署步骤缺失，非代码缺陷。
  - 已验证：列存在（`varchar(30) NOT NULL DEFAULT 'WRITING_GUIDANCE'`）、脚本幂等（重复执行返回 `exists` 而非报错）、
    真实 MySQL 上回看会话可插入且按 purpose 查询命中
> 探针：`C3RealProviderProbeTest`，由 `C3_REAL_PROBE=1` 门控，默认跳过。
> 真实调用共 **3 轮 × 5 次 = 15 次**（预算 ≤ 20），provider=deepseek，仅用自造内容，未使用用户真实日记，未写库。

- [x] **T-31** 回看对话真实观感 **PASS**
  - 回复长度 29~58 字（上限 120），用户输入 14~18 字。比用户长约 2~3 倍，但均为一句话＋一个问题的形态，**未出现话痨**
  - 三轮均自发提起「那时/四月/去年」，气质符合「陪他看那时的自己」；未见诊断或代决表述（`content=null`）
- [x] **T-32** 时间归属误伤率实测：**误伤 0 次**（3 轮 × 3 次运行 = 9 轮观察，`attributionDowngrades=0`）
  - memory-only 片段实测 0~22 字，多次超过阈值 8 —— 说明判定确实被触发，而非因未达阈值而空过
  - **关键核实**：不止看 `attribution=null` 的汇总，还打印了命中的时间归属词。实测命中的是「那时」「过去」「以前」「你说过」「四月」「去年」，均为**真实的时间归属表述，不是词表偶然命中**。放行理由正确
  - **结论：阈值 8 无需调整**，未发生「为回看单开阈值」或「关掉该检查」的情况
- [x] **T-33** 模型在回看中**未尝试 tool_calls**（三轮均 `toolCalls=0`）
  - 因此 fail-closed 分支**未被真实触发**，其正确性仅由单测覆盖，诚实记为未活体验证
- [x] **T-34** C3a 顺延观察 **PASS**
  - 写作引导注入 memory 后：`memoryOnlyRun=0`，模型未复述历史片段，未产生时间归属问题
  - **memory 未被错误当成正文素材**：`memoryAsContent=false`（素材在会话层单层判定即通过，说明内容确实来自本次对话）
- [x] **T-36 → 拦截方向已活体验证（本轮最有价值的结论）**
  - 前几轮全部放行只证明了「不误伤」，**没有证明护栏有效**——这正是 C4 的 R7 悬而未决的原因
  - 做法：取模型真实产出、且 memory-only 片段最长（15 字）的那条回复，**只删掉其中的时间指示语、其余逐字不动**，重新判定
  - 结果：`original=null → stripped=missing-time-attribution`，**`flipped=true`**。同一句真实模型回复因缺失时间归属而被拦下
  - 被判定的文本仍是模型真实写出的句子，非构造样本。**这同时补上了 R7 缺的那一半（C4 只验到误伤方向）**
  - 期间修正一次自身错误：第一版取「最后一轮」回复，而它恰好 `memoryOnlyRun=0`（没在复述），剥离后自然不翻转。那是**样本选错**而非护栏失效，已改为按 memory-only 片段最长挑选
- [ ] **T-35** **R3 补齐**：微信真机手验回看浮层 + C2 遗留的工具链路端到端
  - **需用户在真机操作**，Agent 无法代替。前置已完成：`c3-agent-memory.sql` 已执行（T-30）

---

## 收口

- [ ] **T-37** 输出 Required Output
- [ ] **T-38** 更新 `ACTIVE_TASK` Current Progress；追加 `AGENT_LOG`
- [ ] **T-39** `closeout.md`：偏离规划处、残余风险、R8 实测结论、给 C5 的 carry-over
- [ ] **T-40** 用户验收 → delta 接受进 baseline → 归档 → `ACTIVE_TASK` → IDLE
  - **C3 两刀至此全部完成**，下一刀为 C5 `agent-observability`

---

## 范围守护自检（每个 task 完成时过一遍）

- 未改 C3a 检索实现（复用 `MemoryPort`，未新增检索类）
- 未做 C5 可观测端点或面板
- 未改引导 prompt 提问策略、未改素材合成策略（R2）
- 未扩工具白名单；回看完全无工具
- 未放宽任何 C4 / C3a 阈值
- 未产出可回填正文的素材；未触碰封存后不可变字段
- 未做视觉重建；未改三 Tab 与用户可见命名
- 未引入新依赖；`pom.xml` / `package` / lockfile 未改
- 除 T-19 外未修改任何既有断言
- 未执行 `git add` / `commit` / `push`
