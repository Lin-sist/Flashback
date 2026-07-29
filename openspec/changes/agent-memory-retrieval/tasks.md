# Tasks：Agent Memory Retrieval（C3 前半刀）

> Change ID：`agent-memory-retrieval`
> 阶段：**实现中（闸门 2 已通过）**；闸门 3 未申请
> 提交责任：**用户手动提交**（Agent 不执行 `git add` / `commit` / `push`）
> N1–N5 定稿（2026-07-29 用户按推荐批准）：N1=(b) 包装式分层、N2=(b) 排除 SEALED、N3=(a) 不做扫 content 开关、N4=(a) 本刀建 purpose 列、N5=(a) 前端零改动

---

## 闸门检查点

- [x] **闸门 1 · 规划批准**：proposal / design / tasks / delta 经用户确认，N1–N5 定稿
- [x] **闸门 2 · 实现授权**：用户明确允许按本文件实现
- [ ] **闸门 3 · 外调授权**：真实 provider 联调（≤ 20 次请求）须单独授权

---

## 阶段 0：事实核对

- [x] **T-01** 统计本地数据覆盖率（2026-07-29 实测，用户提供 DB 凭证后完成）

  | 指标 | 值 | 覆盖率 |
  |---|---|---|
  | `record` 总数 | 26 | — |
  | `ai_summary` 非空 | 16 | **62%** |
  | `belief_then` 非空 | 16 | **62%** |
  | `title` 非空 | 22 | **85%** |
  | `core_question` 非空 | **0** | **0%** |
  | 任一说明性字段非空 | 22 | **85%** |
  | 状态分布 | DRAFT 2 / UNLOCKED 24 / SEALED 0 | — |
  | `tag` 表行数 | **0** | — |
  | `record_tag` 绑定 | **0** | — |

  **三条结论，均已由现有实现覆盖，无需改代码**：
  1. `ai_summary` 覆盖率 62%（非规划期担心的极低值），片段取材主路径可用；`title` 85% 作为兜底足够，「任一字段非空」85% 说明绝大多数记录可被注入为片段。
  2. **`core_question` 实测 0%** —— 它在检索谓词与取材链中都在，但当前数据下完全不贡献。这正是「四字段并列无权重 + 固定优先级降级」设计的价值：某字段为 0 时自动被跳过，不需要按覆盖率调权重。
  3. **标签维度当前完全不可用**（`tag` 表为空）—— 本地检索线索实际只有关键词一条。`MemoryQuery.hasCue()` 的语义因此更关键：无关键词时不查库，不会退化成翻旧账。**这不是缺陷，是本地数据尚未建标签**；标签检索路径已有集成测试覆盖（`shouldMatchBySharedEnabledTag`）。

  **对闸门 3 的影响**：真实联调时若要观察标签关联，需先建标签并绑定记录，否则只能验证关键词路径。

---

## 阶段 1：来源分层（纯离线）

- [x] **T-02** `AgentLayeredCorpus`（N1=(b) 包装式）+ `AgentSourceCorpus.merge`
  - 既有 `AgentSourceCorpus` 方法语义与既有测试**零改动**
  - 验证：`AgentLayeredCorpusTest` 11 项 PASS（两层独立、退化为单层、merge 边界、ngram 不一致快速失败）
- [x] **T-03** `AgentCoverageProfile.longestExclusiveRun`（按层相减识别 memory-only 片段）
  - 既有单层入口 `of(candidate, corpus)` 行为不变
  - 验证：memory-only 片段可被精确识别（直接断言）、共用短语不计入
- [x] **T-04** `AgentTimeAttributionChecker` + `AgentGuardrailRules.TIME_ATTRIBUTION_TERMS`（单一声明源）+ `MISSING_TIME_ATTRIBUTION` 违规类型
  - 阈值 `guardrail.min-memory-only-run-for-attribution=8`（**未经真实样本校准**）
  - 验证：`AgentTimeAttributionCheckerTest` 13 项 PASS（裸复述判违规、6 种时间归属表述放行、措辞巧合放行、无记忆层放行、fail-closed、阈值可配、痕迹不含内容）
- [x] **T-05a** 时间归属接入回复路径 + `askText` 路径，命中走既有 `AgentGuardrailDowngrade`

---

## 阶段 2：检索能力

- [x] **T-06** `MemoryPort` / `MemoryQuery` / `MemoryFragment`
  - `MemoryQuery` 带 `purpose` 维度（供后一刀复用）；`MemoryFragment` 覆写 `toString` 避免原文泄露
  - `MemoryQuery.hasCue()`：无线索不查库，不退化成「按时间倒序翻旧账」
- [x] **T-07** `agent_session.purpose` 列：`backend/sql/mysql/c3-agent-memory.sql`（幂等，`INFORMATION_SCHEMA` 判存在）+ 测试 `schema.sql` 同步
  - **不实现** `REVIEW_CHAT` 行为分支
- [x] **T-08** `MySqlMemoryPort` + `RecordMapper.selectMemoryCandidates`
  - SQL 无 `content` 谓词、`user_id` 谓词无条件存在、排除 `SEALED`、排除当前草稿、时间窗、条数上限
  - 无线索时 `1 = 0` 恒不命中（第二道防线）
  - 验证：`MySqlMemoryPortTest` 13 项 + `RecordMemoryRetrievalIntegrationTest` 9 项（**真实 SQL**）PASS
- [x] **T-09** 检索失败 / 超时 fail-open：`retrieveMemory` 捕获异常返回空列表，对话继续；未注入即无记忆层，判定照旧严格

---

## 阶段 3：注入接入

- [x] **T-10** `AgentPromptBuilder.buildMemorySupplement` + `AgentGuardrailRules.memoryUsageClause`
  - 无片段返回空串，**不注入占位段**；时间锚点在 prompt 中可读
- [x] **T-11** `buildConversationMessages` 五参重载（沿用 C2 `toolSupplement` 形态）
  - 既有三参 / 四参重载语义不变，既有调用方零改动
- [x] **T-12** `AgentChatServiceImpl.sendMessage` 接入：`retrieveMemory` → `buildMemorySupplement` → `layeredCorpusOf` 用**同一份列表**
  - 既有编排顺序、失败重试语义、轮次计数未改
  - `MemoryCueExtractor`：只取用户消息、最新一句优先占额度、停用片段、最小长度
- [x] **T-13** `app.agent.memory` 配置（7 项）+ `guardrail.min-memory-only-run-for-attribution`，无新增凭证字段

---

## 阶段 4：护栏路径对齐

- [x] **T-14** 三条 corpus 消费路径对齐
  - 素材 + 工具 `text` → 仅会话层；回复 / `askText` / 引号 → 合并层 + 时间归属
  - 保留 `AgentSourceCorpus` 签名重载（无记忆是真实运行状态，非仅为兼容测试）
- [x] **T-15** 「memory 不得成为正文素材」硬拦：新增 `REASON_MEMORY_AS_CONTENT`，与 `REASON_UNFAITHFUL_ARGS` 分开留痕（区分「编了一句」与「搬旧记录」）；不可配置
- [x] **T-16** 隐私复核：memory 片段不入 `agent_message` / `agent_tool_call` / 日志 / 痕迹；检索日志只记 `userId` + 命中条数 + 注入条数

---

## 阶段 5：spec delta 与回归

- [x] **T-17** delta 已在规划闸产出：`agent-runtime`（四条 MODIFIED + 新增条款）、`backend-core`、`v2-product-scope`；`miniapp-core` 未动
- [x] **T-18** 后端全量回归：**472 tests PASS / 1 skipped**（397 基线 + 75 新增）
  - 未修改任何既有断言；改动仅为构造参数补齐 + import + 测试 `schema.sql` 加列（均已在文件内注释说明理由）
- [x] **T-19** 前端零改动确认：`git status` 无 `frontend/**` 变更

---

## 阶段 6：闸门 3（待单独授权）

- [ ] **T-20** 真实 provider 联调（≤ 20 次）：观察 Agent 拿到 memory 后是否自发带时间归属
- [ ] **T-21** 时间归属误伤方向真实样本验证；拦截方向若未复现，**诚实记为未活体验证**
- [ ] **T-22** 观察 memory 是否被错误整理进正文素材；复现则入回归样本
- [ ] **T-23** 检索相关性真实观感记录（含 T-01 覆盖率结论，不粉饰弱相关性）

> 微信真机手验：本刀前端零改动，**不承接** R3（C2 遗留），留到后一刀有 UI 改动时一并手验。

---

## 收口

- [x] **T-24** 输出 Required Output
- [x] **T-25** 更新 `.ai/ACTIVE_TASK.md` Current Progress；追加 `.ai/AGENT_LOG.md`
- [x] **T-26** `closeout.md`：偏离规划的 5 处、残余风险、给 `agent-review-chat` 的 7 条 carry-over、待执行事项
  - T-01 结论与阈值校准结果**未填**（受 DB 凭证与闸门 3 阻塞），已在 closeout §6 列为待执行
- [x] **T-27** 蓝图 §7 已加 C3 拆两刀登记，并附带登记一项事实修正（`ai_summary` 非后端自动生成）
- [ ] **T-28** 用户验收 → delta 接受进 baseline → 归档 → `ACTIVE_TASK` → IDLE

---

## 范围守护自检（已逐条核对）

- [x] 未实现回看会话 / 回看 UI（`purpose=REVIEW_CHAT` 仅声明，有测试断言零该用途会话）
- [x] 未做决策链路查询端点或面板（C5）
- [x] 未改引导 prompt 提问策略、未改素材合成策略（R2）——memory 只新增独立 system 段
- [x] 未加 FULLTEXT、未加 ngram 配置、未扫 `content`、未引外部检索引擎、未引 embedding
- [x] 未引入新依赖、`pom.xml` 未改
- [x] 未放宽任何 C4 阈值（`AgentMemoryReplyGuardrailTest` 直接断言默认值）
- [x] 未扩工具白名单
- [x] 未把 memory 片段写进任何持久化或日志
- [x] 未执行 `git add` / `commit` / `push`
