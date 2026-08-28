# Tasks：Memory Agency（P4.2）

> 当前阶段：Gate 2 离线实现审查点。Gate 1 与 Gate 2 已获授权；真实 MySQL、微信、provider、delta acceptance、归档、commit/push/deploy/release 均未授权。

## 阶段 0：Readiness 与规划闸

- [x] **T-01** 读取 `AGENTS.md`、`.ai/ACTIVE_TASK.md`、冻结蓝图、产品宪章与 Type C 边界
- [x] **T-02** 确认 P4.1 已归档、ACTIVE_TASK=IDLE、HEAD=`42548ce`、工作树开工时 clean
- [x] **T-03** 确认 P4.2 硬依赖 P4.1/C3/C9 已接受，同名 change 不存在
- [x] **T-04** 核对 accepted 五域 specs 与当前 MemoryPort/session/record/trace/frontend 事实
- [x] **T-05** 记录 confirmed/partial/planned/out_of_scope/unknown 五态
- [x] **T-06** 创建 proposal、design、tasks 与五份 delta
- [x] **T-07** 记录规划期外部调用预算 0、默认用户手动提交
- [x] **T-08** OpenSpec CLI 缺失，采用文件级 scaffold 并将 CLI validation 记为 SKIPPED
- [x] **T-09** 文件级验证 artifact/delta/Requirement/Scenario/link/scope/privacy
- [x] **T-10 GATE 1** 用户批准 proposal/design/tasks/五份 delta 与 D1–D12
- [x] **T-11** 用户未修改 exact API/schema/transaction/source/delete/provider budget，无需联动改写 artifacts

## 阶段 1：实现授权与 baseline

- [x] **T-12 GATE 2** 用户明确授权按 tasks 离线实现
- [x] **T-13** 更新 Gate state、ACTIVE_TASK progress 与 append-only AGENT_LOG
- [x] **T-14** SKIPPED：接手本轮时实现改动已存在，无法补录实现前 backend baseline；最终 full regression 单独记录
- [x] **T-15** SKIPPED：接手本轮时实现改动已存在，无法补录实现前 frontend baseline；最终 type-check/build 单独记录
- [x] **T-16** 建立路径 allowlist；确认 package/lockfile、deployment、monitoring、admin、R1/E1/P5、向量检索与 dev 日志修复在 denylist
- [x] **T-17** 确认 Gate 3a MySQL、Gate 3b 微信分离；provider 预算 0

实现 allowlist：session/record memory policy DTO/domain/VO/mapper/service/schema/migration；MemoryPort eligibility、source relation、Agent Runtime/trace/eval 直接代码与测试；frontend agent service/types/store/sheet、record detail policy 与直接测试；本 change、ACTIVE_TASK、append-only AGENT_LOG。

## 阶段 2：Schema 与 session authorization（TDD）

- [x] **T-18 RED** 新/旧 session 授权默认 false，VO/frontend exact-match
- [x] **T-19** MySQL 增量脚本：session boolean、record policy、source table；H2 schema 同步，migration 文件级幂等
- [x] **T-20 GREEN** domain/mapper/service 持久化和恢复 session authorization
- [x] **T-21 RED/GREEN** `PUT /sessions/{id}/memory-authorization` owner/ACTIVE/幂等/unknown 边界
- [x] **T-22 RED/GREEN** switch 不调 provider、不推进 turn/stage、不改 intent/tool/material、不产生 source
- [x] **T-23 RED/GREEN** pending retry 允许关闭、拒绝开启；retry 使用执行时授权
- [x] **T-24** config/session 四象限，任一 false 时 cue extractor 与 MemoryPort 调用均为 0

## 阶段 3：Record policy（TDD）

- [x] **T-25 RED/GREEN** owner-scoped 全量 PUT 保存 excluded/contextNote，blank→null、255 上限、控制字符拒绝
- [x] **T-26** policy 允许四状态更新但不读取 SEALED 内容、不改封存不可变字段
- [x] **T-27** record detail/backend/frontend exact-match；列表/时间轴不新增分析表面
- [x] **T-28 RED/GREEN** MemoryPort 无条件排除 excluded、SEALED、删除中/删除后、非 owner/current record
- [x] **T-29** contextNote 不参与 cue/排序；仅随实际 source 以“用户后来说明”语义进入 prompt
- [x] **T-30** AI/provider/工具不得创建或修改 record policy/note

## 阶段 4：Actual source evidence（TDD）

- [x] **T-31 RED** final injected list 与 source rows exact-match，候选/丢弃片段不落库
- [x] **T-32 GREEN** source mapper/service/table；REVIEW_TARGET/CROSS_RECORD 封闭枚举
- [x] **T-33** assistant message + source 同一主事务；source 写入 fail-closed，无 REQUIRES_NEW（真实 rollback 待 Gate 3a）
- [x] **T-34** provider/guardrail/message 失败与空注入不产生 source；retry 不重复
- [x] **T-35** AgentMessageVO `memorySources=[]`；旧 message 不回填
- [x] **T-36** owner-scoped source resolution；删除/不可用后 recordId/title/time/note 不返回，available=false（真实 SET NULL 待 Gate 3a）
- [x] **T-37** source 不持久化 fragment/summary/content/keywords/score/hit reason/prompt/reply
- [x] **T-38** trace 只增结构化授权/来源数量结果，不记录 source ID 列表或内容

## 阶段 5：Runtime 与既有边界

- [x] **T-39** REVIEW_CHAT target 默认可用且仅 UNLOCKED；其他历史受 session authorization
- [x] **T-40** 撤销授权后下一轮 0 cross-record retrieval/injection；历史 source 保留
- [x] **T-41** 排除/删除 commit 后下一轮立即不使用；不宣称撤回已发生 provider 调用
- [x] **T-42** C3 Port/LIKE fields/weights/index/time window 与 C9 threshold 不变
- [x] **T-43** memory layer 不能成为当前正文/工具正文合法来源；用户原文忠实度不变
- [x] **T-44** P4.1 intent/question/finish/retry，C2 tool/material confirmation，C8 resilience 保持
- [x] **T-45** Preview fail-closed，不提供假来源或假授权成功

## 阶段 6：Mini Program

- [x] **T-46 RED/GREEN** Agent sheet 默认 off 授权控件、说明与 pending/disabled 状态
- [x] **T-47** 开启/关闭失败保持后端真实状态并提示；不触发 provider
- [x] **T-48** assistant message 有实际来源才显示 source chips；available 跳 record detail，unavailable 不可点
- [x] **T-49** record detail 排除开关与用户 note 编辑；保存不调 provider
- [x] **T-50** 文案只承诺“未来轮次/之后不再参考”，不声称删除模型记忆或撤回既往调用
- [x] **T-51** 保留三个一级 Tab、canonical naming、witness role、intent switch、结束/重试/确认；无全局 settings/记忆管理中心
- [x] **T-52** Preview Agent/authorization/policy/source requests 均为 0（微信开发者工具 app-context 计数 total=0、memoryAgency=0）

## 阶段 7：C6 与离线验证

- [ ] **T-53** PARTIAL：fixed fixtures 已覆盖 off/on/exclude/review-target/source；revoke/delete/owner/status 有直接测试，transaction 有结构检查，尚未全部提升为 C6 fixture
- [x] **T-54** 硬不变量加入 auth=false=0 retrieval、source exact-match、无内容持久化
- [x] **T-55** C6 baseline compare；合法变化逐项加 `P4.2 memory-agency: <reason>`，禁止自动刷新
- [x] **T-56** backend focused/full Maven PASS，记录真实统计
- [x] **T-57** frontend type-check、standard/Preview build PASS
- [x] **T-58** MySQL/H2/domain/mapper/DTO/VO/frontend exact-match 文件检查
- [x] **T-59** `git diff --check`、path allowlist、package/lockfile 零变化、privacy/credential scan
- [x] **T-60** OpenSpec 文件级 validation；CLI 缺失如实 SKIPPED
- [x] **T-61** 更新 ACTIVE_TASK progress 与 append-only AGENT_LOG

## 阶段 8：Gate 3（分别授权）

- [x] **T-62 GATE 3a** 用户单独授权真实 MySQL preflight/migration/合成探针
- [x] **T-63** migration 连续两次、schema exact-match、off/on/revoke/exclude/delete/owner/status/rollback/SET NULL PASS
- [x] **T-64** 仅合成数据；finally cleanup 后合成 user/record/session/message/source/operation 均为 0
- [x] **T-65 GATE 3b** 用户单独授权微信开发者工具/真机 Standard/Preview 矩阵
- [x] **T-66** Standard 验授权/失败/source/跳转/policy/note/delete；Preview 请求数 0
- [x] **T-67** 开发者工具与物理真机分开报告；开发者工具 PASS、物理真机 SKIPPED；scripted response 不冒充真实 provider
- [x] **T-68** 本轮真实 provider 调用 0；未新增语言质量目标或 provider 预算

## 阶段 9：验收与归档

- [x] **T-69** 用户审查 diff、离线/真实/跳过证据、baselineNote 与 risks
- [x] **T-70** 用户明确允许 delta acceptance 与归档
- [x] **T-71** 接受五份 delta 进 baseline并 exact-copy/operation 验证
- [x] **T-72** closeout 如实区分 scripted、real MySQL、微信开发者工具/真机与 provider=0
- [x] **T-73** archive change，ACTIVE_TASK 回 IDLE，追加 AGENT_LOG
- [ ] **T-74** 仅在 P4.2 单独授权 Agent commit 时 stage/commit；不 push/deploy/release

## 范围守护自检

- [x] 无向量库/全文索引/分词器/画像/趋势/评分/诊断/建议清单
- [x] 无全局永久授权、默认开启、跨 session 继承或配置开关替代 consent
- [x] 无 source 内容快照、关键词/分数暴露、AI 推测自动持久化
- [x] 无新工具/工具扩面/关系型 AI/R1 安全响应/E1/P5
- [x] 无 package/lockfile/deployment/monitoring/admin/major visual reconstruction
- [x] 无用户日记/对话/note/provider reply/prompt/secret 进入日志、trace、baseline、报告
