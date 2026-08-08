# Tasks：Agent Temporal Intelligence（C9）

> Type C。**闸门 1 已批准、闸门 2 已授权；闸门 3 未授权**。
> 提交责任：Agent 提交（2026-08-08 已授权）；不含 push / deploy / release。

## 阶段 0：readiness 与规划闸

- [x] **T-01** 读取 `AGENTS.md`、`.ai/ACTIVE_TASK.md`、C8 archive 与冻结蓝图，确认一次一个 ACTIVE
- [x] **T-02** 核对开刀前 Git clean、HEAD `544e9ea`、C9 无目录冲突
- [x] **T-03** 核对 C3a/C3b accepted memory/review contract、C6 eval 与 C8 budget 前置
- [x] **T-04** 核对 `MemoryFragment`、`MemoryPort`、回看 focal fragment、`Clock` 与现有 config 实现事实
- [x] **T-05** 创建 proposal / design / tasks 与五份 spec delta
- [x] **T-06** 明确提交责任为“用户手动提交”，规划期外调预算为 0
- [x] **T-07** 用户审阅并批准 N1–N8 与五份 delta（闸门 1）
- [x] **T-08** 用户明确授权按本 tasks 实现（闸门 2）
- [x] **T-09** 闸门 2 后先运行实现前 baseline：focused + 后端全量；81 suites / 645 tests / 6 skipped PASS

## 阶段 1：Temporal policy 核心（TDD）

- [x] **T-10 RED** fixed Clock 覆盖 30/31、180/181 天、同日、null 与 future 时间
- [x] **T-11 GREEN** 新增 `TemporalDistanceBand` / `TemporalDistance`，实现确定性日期级距离计算
- [x] **T-12 RED** config invalid cases：边界逆序、百分比越界、最小字符/跨度非正数
- [x] **T-13 GREEN** 在 `AppAgentProperties` / `application.yml` 新增 backend-only temporal config 与 fail-fast validation
- [x] **T-14 RED** `temporal.enabled=false` 应等价 C8 行为且留下无内容 metadata
- [x] **T-15 GREEN** 新增 `AgentTemporalPolicy` / `TemporalPolicyResult` 与稳定 fingerprint source

## 阶段 2：旁支记忆注入衰减（TDD）

- [x] **T-16 RED** 120 字 base 在 RECENT/DISTANT/LONG_AGO/UNKNOWN 下得到 120/90/60/60
- [x] **T-17 RED** base < 40 时不得扩张；中文截断、空文本、null fragment 安全处理
- [x] **T-18 RED** focal review fragment 不衰减，ancillary retrieved fragment 才衰减
- [x] **T-19 GREEN** 实现 deterministic ancillary fragment budget decay
- [x] **T-20 RED** prompt 注入列表与 `AgentLayeredCorpus` memory source 必须是同一份衰减后列表
- [x] **T-21 GREEN** 在合并前接入 focal/ancillary 分流；不改 `MemoryPort`、mapper SQL 或 24 个月窗口
- [x] **T-22** 回归 owner/status/excludeRecordId/无线索不查库/检索失败 fail-open，确保 C3 边界不变

## 阶段 3：时间 supplement 与回看锚点

- [x] **T-23 RED** 无 memory/temporal result 时 supplement 为空，不输出“未发现规律”
- [x] **T-24 RED** supplement 同时保留 exact month label 与允许的 distance wording，不包含 band/阈值/比例
- [x] **T-25 GREEN** 新增 `AgentPromptBuilder.buildTemporalSupplement(...)`，只供 reply prompt
- [x] **T-26** 验证 temporal supplement 不进入 material/tool prompt、正文、agent_message 或新持久化
- [x] **T-27** 验证 WRITING_GUIDANCE 与 REVIEW_CHAT 均可获得距离语义，但不改变 mode/stage/tool/material 契约

## 阶段 4：克制 recurrence hint（TDD）

- [x] **T-28 RED** 仅 REVIEW_CHAT + 显式比较/反复 cue + 2 个 distinct ancillary record + span≥90 天 eligible
- [x] **T-29 RED** 89/90 天边界、重复 recordId、unknown time、仅 focal、非 REVIEW、无 cue 均有反例
- [x] **T-30 GREEN** 实现窄 cue matcher 与 `TemporalPatternEvidence`；每轮最多一个 hint
- [x] **T-31 RED** hint 只含无内容约束，不含 record count、span days、关键词或片段
- [x] **T-32 GREEN** 将 hint 接入 reply prompt；不跨轮持久化 pattern profile
- [x] **T-33** 验证用户没有比较意图时不主动归纳；进入页面但未开对话时零行为

## 阶段 5：Temporal overreach 护栏（TDD）

- [x] **T-34 RED** 百分比/评分、绝对频率、必然因果、趋势与预测话术应命中
- [x] **T-35 RED** 合法月份、普通共情、用户自己提到数字但 Assistant 不作分析的反例应放行
- [x] **T-36 GREEN** 新增单一声明源 `AgentTemporalLanguageChecker` 与内部 `TEMPORAL_OVERREACH`
- [x] **T-37 RED** overreach 直接安全兜底、越界 Assistant 不落库、违规类型进入无内容 trace
- [x] **T-38 GREEN** 接入最终 reply pipeline；不得改变 C4 checker authority
- [x] **T-39** 验证 overreach 不进入 C7 reflection，provider call count 不增加

## 阶段 6：Trace / version / C6 eval / privacy

- [x] **T-40 RED** trace 只允许 enabled/version/band counts/chars/pattern flags/violation/call count
- [x] **T-41 GREEN** 接入 C5 trace 与 `AgentTraceVersions` fingerprint，不记录内容或精确时间清单
- [x] **T-42** 扩展 C6 fixed-clock cases：distance、decay、review hint、insufficient evidence、overreach、no-extra-call
- [x] **T-43** 验证既有 MISSING_TIME_ATTRIBUTION、C7 reflection 与 C8 resilience snapshots/不变量不被静默改写
- [x] **T-44** 逐条审查 C9 合法 snapshot 变化；同步 `baselineNote` + checksum，禁止批量/自动刷新
- [x] **T-45** 新增隐私测试：日志/trace/eval/exception 不含日记、对话、关键词、fragment、prompt/provider response
- [x] **T-46** 验证 policy disabled 与 insufficient evidence 均不是“分析结果”，不向前端暴露内部状态

## 阶段 7：全量回归与范围守护

- [x] **T-47** 运行 C9 focused tests，PASS 后再运行后端全量 Maven；记录 suites/tests/failures/errors/skipped
- [x] **T-48** 运行前端 `vue-tsc --noEmit` 与 mp-weixin build，确认现有消费端零契约回归
- [x] **T-49** 验证 API/DTO/DDL/mapper SQL/frontend/src/pom/package/lockfile 零变化
- [x] **T-50** 验证三个 Tab、canonical naming、封存不变性、被动召唤、无 dashboard/推送/设置页
- [x] **T-51** 运行 `git diff --check`、路径 allowlist、增量 sensitive pattern scan
- [x] **T-52** OpenSpec 文件级校验：目录、Requirement/Scenario、delta 落点、链接与实现 exact-match；CLI 缺失记 SKIPPED
- [x] **T-53** 更新 `.ai/ACTIVE_TASK.md` Current Progress 与 append-only `.ai/AGENT_LOG.md`

## 阶段 8：闸门 3（仅另行授权后）

- [ ] **T-54 GATE 3** 用户单独批准真实 provider / MySQL / 真机范围与预算
- [ ] **T-55** provider 先跑固定合成 canary；总调用不超过 6，覆盖 distance/review/recurrence/反例
- [ ] **T-56** 真实 MySQL 用可清理合成用户与不同年龄记录验证 owner/status/time/decay；H2 不冒充
- [ ] **T-57** 微信真机验证现有浮层话术长度、失败态与无分析 UI；无环境则 SKIPPED+原因
- [ ] **T-58** 任何敏感内容、调用超限、timeout、identity/config 漂移立即停止并记账

## 阶段 9：验收与收口

- [ ] **T-59** 用户 review 实现 diff、真实/跳过证据与 remaining risks
- [ ] **T-60** 用户验收后将五份 delta 接受进 baseline，逐字核对 Requirement/Scenario
- [ ] **T-61** 写 `closeout.md`，按 D33 更新叙事 §10；只写 confirmed 与诚实 SKIPPED
- [ ] **T-62** 归档 change 至 `openspec/changes/archive/<date>-agent-temporal-intelligence/`
- [ ] **T-63** `.ai/ACTIVE_TASK.md` → `IDLE`，追加 AGENT_LOG closeout
- [x] **T-64** 按提交责任处理；用户已授权 Agent commit，已提交 `65e18e0`；未 push/deploy

## 范围守护自检

- [x] 没有新增 API、DTO、数据库表/列/索引或修改 memory mapper SQL
- [x] 没有新增页面、Tab、卡片、dashboard、图表、评分、推送、设置页或 frontend-visible config
- [x] 没有删除/过期/降权业务记录；focal review record 不参与衰减
- [x] 没有把 recency/LIKE/tag 命中写成 relevance、周期、因果或诊断结论
- [x] 没有新增 provider 调用、LLM-as-Judge、自动 retry、多 provider 或第三次调用
- [x] 没有修改 C4 阈值、C7 reflection eligibility、C8 budget/call limits、C6 自动更新禁令
- [x] 没有把 temporal hint、日记、对话、记忆片段、prompt/provider response 写入日志/trace/新存储
- [x] 没有修改 archive、冻结蓝图、package/lockfile、deployment、monitoring 或无关代码
