# Tasks：Witness Agent Alignment（P4.1）

> 当前阶段：Gate 1 / Gate 2 已于 2026-08-12 获批，离线实现与回归已完成；用户于 2026-08-17 批准 Gate 3a/3b/3c、delta acceptance、归档与 Agent 本地提交。push、PR、部署与发布仍未授权。

## 阶段 0：Readiness 与规划闸

- [x] **T-01** 读取 `AGENTS.md`、`.ai/ACTIVE_TASK.md`、冻结蓝图 v2.0、核心产品定义 v0.1 与 Type C checklist
- [x] **T-02** 确认 P3.2 已归档、`ACTIVE_TASK=IDLE`、工作树 clean、HEAD=`0aea558`
- [x] **T-03** 确认 P4.1 硬依赖 P3.1 与 C6 已归档/接受，同名 change 不存在
- [x] **T-04** 核对 accepted agent-runtime、backend-core、miniapp-core、v2-product-scope、agent-collaboration
- [x] **T-05** 核对当前 Prompt、阶段机、session DTO/mapper、frontend entry/sheet、C6/reflection/resilience 事实
- [x] **T-06** 记录 confirmed / partial / planned / out_of_scope / unknown 五态
- [x] **T-07** 创建 proposal、design、tasks 与五份 delta
- [x] **T-08** 记录规划期外部调用预算 0、默认用户手动提交
- [x] **T-09** 确认 OpenSpec CLI 未安装，采用仓库既有文件级 scaffold 并记 CLI validation `SKIPPED`
- [x] **T-10** 文件级核对 artifacts、delta operation、Requirement/Scenario、链接、范围与隐私边界
- [x] **T-11 GATE 1** 用户批准 proposal / design / tasks / 五份 delta 与 design 决策 1–12（2026-08-12，“批准决策”）
- [x] **T-12** 用户如修改 exact enum/API/schema/legacy migration、0/1 问题上限或 Gate 3a 预算，同步全部 artifacts 后重新请求 Gate 1（N/A：按推荐决策获批）

## 阶段 1：实现授权与 baseline

- [x] **T-13 GATE 2** 用户明确授权按本 tasks 修改业务代码（2026-08-12，“可以进入实现”）
- [x] **T-14** 更新 proposal Gate State、ACTIVE_TASK Current Progress 与 AGENT_LOG，只记录已获授权范围
- [x] **T-15** 运行 backend focused/full Maven baseline，记录 suites/tests/failures/errors/skipped（99 suites / 703 tests / 0 failures / 0 errors / 11 skipped）
- [ ] **T-16** 运行 frontend type-check、standard 与 Preview mp-weixin build baseline
- [x] **T-17** 建立 P4.1 路径 allowlist，确认 package/lockfile、deployment、monitoring、admin、P4.2 与 archive 不在范围
- [x] **T-18** 确认 Gate 3a provider、Gate 3b 微信、Gate 3c MySQL 分离，未授权时对应探针默认 skip

> T-16 PARTIAL：实现前 frontend type-check PASS；standard/Preview baseline 当时未单独留证，故不倒签。实现后 T-62 双构建与 type-check 均 PASS。

实现 allowlist：

- backend Agent intent/domain/DTO/VO/session mapper/MySQL/H2 schema、witness policy、prompt、question guard、reflection/trace/eval 与直接测试；
- frontend agent service/types/store、record editor entry、AgentChatSheet 与直接测试；
- C6 fixtures/baseline 仅限 P4.1 逐项归属变化；
- 本 change artifacts、`.ai/ACTIVE_TASK.md`、append-only `.ai/AGENT_LOG.md`。

denylist：package/lockfile、deployment、monitoring、admin、工具扩面、P4.2 memory authorization/retrieval、图/画像/评分/诊断、major visual reconstruction、accepted specs、archive、冻结蓝图、未授权真实外调。

## 阶段 2：Intent 与 session contract（TDD）

- [x] **T-19 RED** `AgentConversationIntent` 仅接受 LISTEN/UNTANGLE，未知值 fail-closed
- [x] **T-20 RED** start WRITING_GUIDANCE 接受 intent，缺省 LISTEN；REVIEW_CHAT 携带 intent 被拒绝
- [x] **T-21 RED** session VO：写作会话返回 intent，回看为 null；frontend type exact-match
- [x] **T-22** 新增 MySQL 增量脚本并同步 baseline/H2 schema：nullable `conversation_intent`、历史 WRITING_GUIDANCE 回填 LISTEN、结构化 pre/postflight
- [x] **T-23 GREEN** domain/DTO/VO/mapper/service 持久化和恢复 intent
- [x] **T-24 RED/GREEN** `PUT /api/agent/sessions/{id}/intent` owner、ACTIVE、purpose、幂等与未知值边界
- [x] **T-25 RED/GREEN** intent switch 不调用 provider、不推进 turn/stage、不改 tool/material；pending failed turn 时明确冲突
- [x] **T-26 RED/GREEN** legacy ACTIVE WRITING_GUIDANCE 归一为 LISTEN/WITNESS/reask=0；历史 message/trace 不回写

## 阶段 3：Witness orchestration（TDD）

- [x] **T-27 RED** 新会话从 WITNESS 开始，普通 turn 保持 WITNESS，不产生四阶段推进
- [x] **T-28 GREEN** 新增 `AgentWitnessTurnPolicy`，输出 REFLECT_ONLY/MAY_ASK_ONE/CLOSE 与 maxQuestions
- [x] **T-29 RED/GREEN** LISTEN 恒为 REFLECT_ONLY/0 问题
- [x] **T-30 RED/GREEN** UNTANGLE 正常输入 MAY_ASK_ONE/至多 1 问题
- [x] **T-31 RED/GREEN** 去空白长度 <=4 时 REFLECT_ONLY/0 问题，不增加 reask
- [x] **T-32 RED/GREEN** 明确停止或 maxTurns 进入 CLOSING/0 问题；finish 可从任意 ACTIVE WITNESS 结束
- [x] **T-33 RED/GREEN** CLOSING/ENDED/REVIEW 既有语义与 append-after-ended 拒绝保持
- [x] **T-34** legacy stage enums 保持只读兼容，新增生产代码不得产生 OPENING/EMOTION/CONFUSION/CORE_QUESTION/EXPECTATION

## 阶段 4：Prompt、问题上限与 reflection（TDD）

- [x] **T-35 RED** Prompt snapshot 不再包含“一个朋友”、固定四阶段目标或默认要求提问
- [x] **T-36 GREEN** system role 改为有温度的见证者，加入无关系承诺/无人格固化/无强迫结论边界
- [x] **T-37 GREEN** typed turn instruction 映射 REFLECT_ONLY/MAY_ASK_ONE/CLOSE，不拼用户或候选文本
- [x] **T-38 RED/GREEN** question counter 覆盖中文、英文、混合与连续问号；按 policy 校验 0/1
- [x] **T-39 RED/GREEN** `EXCESSIVE_QUESTIONS` 只允许一次 typed reflection，共享 deadline/budget/attempt/trace
- [x] **T-40 RED/GREEN** reflection 仍越界时 backend fallback 无问题且不冒充 provider success
- [x] **T-41 RED/GREEN** trace 记录 intent/policy/questionLimit/violation/outcome，不记录输入、候选、回复或 prompt
- [x] **T-42** prompt/rule/trace version anchors 升级，版本测试与旧 trace 读取通过

## 阶段 5：既有能力边界回归

- [x] **T-43** DRAFT/SAVED 可用性、SEALED/UNLOCKED 写作入口拒绝与 owner scope 保持
- [x] **T-44** tool definitions/allowlist/explicit confirmation/fail-closed 不扩大；LISTEN 不获得新工具
- [x] **T-45** material 只在写作结束后生成且须显式确认；用户原文与忠实度边界不变
- [x] **T-46** provider failure、pending turn retry、零自动 retry、统一 deadline 与 failure taxonomy 不变
- [x] **T-47** C9 time attribution/overreach 与 C3 memory privacy 不变；P4.1 不修改 retrieval scope/switch
- [x] **T-48** REVIEW_CHAT 保持 REVIEW stage、无工具、无素材；继承 witness role/问题上限但无 conversationIntent
- [x] **T-49** Preview 真实 Agent 请求数 0；不提供本地假对话或假切换成功

## 阶段 6：Mini Program 交互

- [x] **T-50 RED/GREEN** 点击既有入口后先显示两项同权重选择，选择前 provider 调用 0
- [x] **T-51** 接入 start intent 与 session VO，旧 session 恢复显示真实当前 intent
- [x] **T-52** 浮层提供轻量 intent switch；请求中禁用重复操作，失败保留原 intent 并提示
- [x] **T-53** 移除阶段旅程标题，使用“先听你说/一起理一理/说到这里已经很好”
- [x] **T-54** 保留关闭、“先聊到这里”、retry、tool/material confirmation；不新增一级 Tab/AI 页面
- [x] **T-55** 验证 textarea/keyboard/scroll/ended/error 状态，不做 major visual reconstruction
- [x] **T-56** Preview 选择/切换全部 fail-closed；真实登录替换 Preview 后才可调用 backend

> T-55 DEVTOOLS PASS：2026-08-25 Standard automation 在微信开发者工具中验证 textarea 输入、长消息 scroll、intent switch error/原值保留、重试成功与 ended/composer 移除；物理真机键盘与触感仍 SKIPPED，不把模拟器写成真机。

## 阶段 7：C6 与自动化验证

- [x] **T-57** 增加两种 intent、极短输入、停止、切换、question violation、review regression 的固定合成 fixtures/直接测试
- [x] **T-58** 通用硬不变量增加按 turn policy 的问题上限；既有长度、追问、工具、忠实度、时间与 trace 不变量不削弱
- [x] **T-59** 执行 C6 baseline compare；逐项审查合法变化
- [x] **T-60** 合法变化的 `baselineNote` 使用 `P4.1 witness-agent-alignment: <reason>` 并同步 checksum；禁止自动刷新
- [x] **T-61** backend focused/full Maven PASS，记录真实 suites/tests/failures/errors/skipped（102 suites / 726 tests / 0 failures / 0 errors / 11 skipped）
- [x] **T-62** frontend type-check、standard/Preview mp-weixin build PASS
- [x] **T-63** MySQL/H2 schema、domain/mapper/DTO/VO/frontend type exact-match（文件/H2；真实 MySQL 待 Gate 3c）
- [x] **T-64** `git diff --check`、path allowlist、package/lockfile 零变化、credential/privacy 增量扫描
- [x] **T-65** OpenSpec 文件级 validation PASS；CLI 缺失如实 SKIPPED
- [x] **T-66** 更新 ACTIVE_TASK Current Progress 与 append-only AGENT_LOG

## 阶段 8：Gate 3 真实验收（分别授权）

- [x] **T-67 GATE 3a** 用户单独授权固定合成真实 provider 小样本与预算（2026-08-17）
- [x] **T-68** 最多 2 次 canary，通过后执行最多 6 个 case；总 provider 调用 <=8（含 reflection），异常立即停止（8 scenarios / 8 calls / 0 reflection，PASS）
- [x] **T-69** 人评 witness role/user control/question restraint/brief restraint/uncertainty/no conclusion；只存等级/元数据/结论（六项 PASS，不保存样本文本）
- [x] **T-70 GATE 3b** 用户单独授权微信开发者工具/真机入口选择、切换、短答、结束、错误与 Preview 矩阵（2026-08-17）
- [x] **T-71** 微信验收如实区分开发者工具与真机；build/截图不代替交互证据
- [x] **T-72 GATE 3c** 用户单独授权真实 MySQL 只读聚合 preflight、migration 与合成会话恢复（2026-08-17）
- [x] **T-73** MySQL 不读取/输出真实消息或日记；只报告 schema、枚举聚合、迁移与合成数据结论

> T-71 DEVTOOLS PASS：2026-08-25 真实微信登录成功；Standard 在真实构建/登录边界下以合成 Agent response 验入口、两项选择、textarea、scroll、切换失败/成功与结束，新增 provider 调用 0；Preview fail-closed PASS。物理真机 SKIPPED。
>
> T-73 PASS：2026-08-25 MySQL 8.0.41 preflight、migration 连续两次、schema/恢复/switch/review-null/owner-scope 合成探针全部 PASS；finally cleanup 后合成 user/record/session/blocking operation 均为 0。

## 阶段 9：验收、delta acceptance 与归档

- [x] **T-74** 用户审查实现 diff、离线/真实/跳过证据、baselineNote 与 remaining risks
- [x] **T-75** 用户明确允许 delta acceptance 与归档（2026-08-17）
- [x] **T-76** 接受五份 delta 进 baseline，exact-copy/operation 检查 PASS
- [x] **T-77** 写 closeout，如实区分 scripted/C6、real provider、人评、MySQL、微信与生产边界
- [x] **T-78** 归档 change，ACTIVE_TASK 回 IDLE，追加 AGENT_LOG
- [x] **T-79** 仅在 P4.1 单独授权 Agent commit 时 stage/commit；不执行 push/deploy/release（checkpoint `09304a1`；未 push）

## 范围守护自检

- [x] 没有把见证者改写成 AI 朋友、伴侣、关系养成或主动关心产品
- [x] 没有实现 P4.2 memory authorization/retrieval rewrite、图、画像、评分、诊断或建议清单
- [x] 没有新增工具、扩大工具白名单或削弱工具/素材/原文确认
- [x] 没有改变 SEALED location/attachment/cover 不可变性或三个一级 Tab/canonical naming
- [x] 没有修改 package/lockfile、deployment、monitoring、admin 或做大规模 rewrite/major visual reconstruction
- [x] 没有把 C6/scripted/build 冒充真实 provider 自然度、MySQL 或微信真机证据
- [x] 没有把用户/模型/记忆内容、prompt、secret 写入日志、trace、baseline、报告或 tracked evidence
