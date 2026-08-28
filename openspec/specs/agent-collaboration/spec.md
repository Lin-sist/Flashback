# Agent Collaboration Spec / Agent 协作规格

## Purpose / 目的

记录人类与 AI 协作者在 V2.0 规划、规格、执行与审查中的职责分工。

## Requirements

### Requirement: Agents Must Follow Declared Roles / Agent MUST 遵循声明角色

Agent MUST 尊重 `AGENTS.md` 中记录的协作角色。

#### Scenario: Agent receives a V2.0 task / Agent 收到 V2.0 任务

- **WHEN** Agent 收到任务
- **THEN** 它识别任务属于规划、规格、执行、审查、文案还是 UI reference work
- **AND** 在没有用户明确指示时，避免接管其他角色职责

### Requirement: Implementation Must Be Driven By OpenSpec Changes / 实现 MUST 由 OpenSpec changes 驱动

重要的 V2.0 实现 MUST 基于 active OpenSpec change，并具备 proposal、design、tasks 与 spec delta。

#### Scenario: Engineering agent starts work / 工程 Agent 开始工作

- **WHEN** 工程 Agent 开始实现 V2.0 模块
- **THEN** 它读取相关 `openspec/changes/<change-id>/` artifacts
- **AND** 它检查 `openspec/specs` 中已接受的 specs

### Requirement: OpenSpec Must Remain Iterative / OpenSpec MUST 保持可迭代

Agent MUST 将 OpenSpec 视为当前最高优先级事实源，而不是永久冻结的 artifact。

#### Scenario: User confirms a new decision / 用户确认新决策

- **WHEN** 用户确认会改变范围、命名、设计或执行顺序的 V2.0 决策
- **THEN** Agent 在实现继续前更新相关 OpenSpec artifacts
- **AND** 旧文档保持为历史参考，除非被更新或重新确认

### Requirement: Reviews Must Check Scope Drift / 审查 MUST 检查范围漂移

审查 Agent MUST 检查代码或设计变更是否偏离 active OpenSpec scope。

#### Scenario: Codex reviews an implementation / Codex 审查实现

- **WHEN** Codex 审查 V2.0 实现
- **THEN** 它优先报告 scope drift，再讨论风格偏好
- **AND** 它指出非预期的 backend、database、business logic 或 production-launch changes

### Requirement: Inspiration Must Not Override Specs / 灵感材料 MUST NOT 覆盖 specs

Claude、v0 或其他 inspiration/reference outputs MUST NOT 覆盖已接受的 OpenSpec requirements。

#### Scenario: Reference output conflicts with OpenSpec / 参考输出与 OpenSpec 冲突

- **WHEN** 生成的 UI 或文案参考与 OpenSpec 冲突
- **THEN** OpenSpec 仍然是 source of truth
- **AND** 冲突被记录为问题或未来 proposal，而不是被静默实现

## Accepted From C5 agent-observability

> 来源：`openspec/changes/archive/2026-07-30-agent-observability/`（C5，2026-07-30 用户验收）。
> 范围：决策轨迹作为**协作证据**时的脱敏与使用规范（蓝图 v1.1 §5 指定落点）。
> 说明：本 spec 在 C5 之前没有承载过产品 Agent 条款——C1–C3 的 Agent 条款实际全部落在
> `agent-runtime`。以下为本 spec 第一次承载该主题。

### Requirement: Observability Data Must Not Leak Diary Content Into Collaboration Artifacts

决策轨迹用于排查与证据留存时，SHALL NOT 把用户日记原文带入任何协作产物。

#### Scenario: 轨迹被引用进执行证据

- **WHEN** Agent 把某次排查结论写入 `.ai/AGENT_LOG.md` 或 change 文档
- **THEN** 它只引用结构化标识、数值指标与会话轮次
- **AND** 它 SHALL NOT 粘贴日记原文、对话原文或记忆片段内容

#### Scenario: 轨迹被引用进对话回复

- **WHEN** Agent 在与用户交流中说明某轮发生了什么
- **THEN** 它以结构化事实描述，例如命中条数、判定结论、耗时
- **AND** 它 SHALL NOT 复述用户日记内容作为说明材料

#### Scenario: 探针与联调输出

- **WHEN** Agent 运行真实 provider 探针并记录结果
- **THEN** 输出只包含指标与判定结论
- **AND** 输出 SHALL NOT 包含候选文本原文或用户真实日记

### Requirement: Observability Must Serve Developers Not Product Users

可观测能力 SHALL 只服务于开发者排查，SHALL NOT 被转化为用户可见能力。

#### Scenario: 有人提议把轨迹展示给用户

- **WHEN** 出现「让用户看到 Agent 的思考过程」之类的提议
- **THEN** 该提议被记录为独立 proposal 而非直接实现
- **AND** 既有约束「用户 SHALL NOT 被告知护栏内部判定过程」保持有效

### Requirement: Observability Findings Must Not Trigger Unauthorized Behavior Changes

轨迹揭示的问题 SHALL NOT 成为在同一刀内顺手修改 Agent 行为的依据。

#### Scenario: 轨迹揭示引导话术或检索质量问题

- **WHEN** 轨迹显示某层判定频繁触发或检索命中率低
- **THEN** Agent 把结论记为残余风险或后续 change 的输入
- **AND** 它 SHALL NOT 在可观测 change 内调整提示词、阈值或检索策略

#### Scenario: 轨迹揭示的问题需要立即处理

- **WHEN** 轨迹揭示的问题严重到需要立即修复
- **THEN** Agent 停下并向用户说明，由用户决定是否开新 change 或扩大当前范围
- **AND** 它 SHALL NOT 自行扩大已批准的范围

---

## Accepted From C6 agent-eval-framework

> 来源：`openspec/changes/archive/2026-07-31-agent-eval-framework/`（C6，2026-07-31 用户验收）。
> 范围：评测与样本的协作规范——真实样本的处置、基线更新的协作约束、
> 以及「评测结果不得成为顺手改行为的依据」。
> 延续上文 C5 段落的风格：本 spec 自 C5 起承载产品 Agent 条款。

### Requirement: Evaluation Samples Must Not Enter Collaboration Artifacts

评测用到的真实样本 SHALL NOT 进入任何协作产物。

#### Scenario: 真实样本的处置

- GIVEN 某次评测使用了用户真实日记作为本地输入
- WHEN 记录执行证据
- THEN 执行日志 SHALL 只记录用例标识、维度与判定结论
- AND 执行日志 SHALL NOT 包含样本内容

#### Scenario: 基线与报告作为协作产物

- GIVEN 基线文件与失败报告会被审查
- WHEN 它们被写出
- THEN 它们 SHALL 只包含结构化标识与数值指标
- AND 它们 SHALL NOT 包含用例输入文本

#### Scenario: 对外叙事中的评测内容

- GIVEN 评测能力会被写入对外叙事文档
- WHEN 描述评测
- THEN 描述 SHALL 只使用维度名称、机制与结论
- AND 描述 SHALL NOT 包含真实样本、secret 或本机绝对路径

### Requirement: Baseline Updates Must Be Deliberate And Attributed

基线的更新 SHALL 是一个有归属、可审查的动作。

#### Scenario: 更新基线时的协作义务

- GIVEN 某次改动导致基线指标变化
- WHEN 更新基线
- THEN 更新方 SHALL 在基线说明中记录本次更新归属于哪一个 change
- AND 更新方 SHALL 在执行证据中说明该变化为预期

#### Scenario: 不得静默刷新

- GIVEN 评测因基线不符而失败
- WHEN 处理该失败
- THEN 更新方 SHALL NOT 在未说明原因的情况下把基线改成当前值
- AND 更新方 SHALL NOT 通过绕过或禁用评测使其通过

#### Scenario: 不变量失败的处理义务

- GIVEN 某条不变量断言失败
- WHEN 处理该失败
- THEN 处理方 SHALL 将其视为缺陷并定位原因
- AND 处理方 SHALL NOT 通过放宽该不变量使其通过

### Requirement: Evaluation Findings Must Not Trigger Unauthorized Behavior Changes

评测揭示的问题 SHALL NOT 成为在同一刀内顺手修改 Agent 行为的依据。

#### Scenario: 评测发现缺陷时的范围约束

- GIVEN 建立评测的过程中发现了某个编排缺陷
- WHEN 决定如何处置
- THEN 处置 SHALL 限于如实记录该发现
- AND 修复 SHALL 由独立变更承担，除非用户明确纳入当前变更

#### Scenario: 阈值校准的授权边界

- GIVEN 评测使某个未校准的阈值第一次具备校准条件
- WHEN 决定是否校准
- THEN 建立评测的变更 SHALL NOT 同时校准该阈值
- AND 校准 SHALL 作为独立事项交由用户决定

#### Scenario: 评测能力的如实描述

- GIVEN 评测运行在替身与 mock 路径上
- WHEN 向用户或对外描述其覆盖范围
- THEN 描述 SHALL 明确它覆盖编排逻辑而非模型语言质量
- AND 未验证项与未填充项 SHALL 被如实标注
- AND 描述 SHALL NOT 让读者以为修改提示词已因此变得安全
## Accepted From C7 agent-reflection-loop

> Accepted on 2026-08-03. 范围：重写指令、评测证据、外调与基线更新纪律。

### Requirement: Reflection Evidence Must Remain Content Free

#### Scenario: 记录重写原因

- GIVEN 一次 reflection 被触发
- WHEN Agent 写 trace、日志、AGENT_LOG、测试报告或 closeout
- THEN SHALL 只记录路径、违规类型、调用次数、耗时、终态与受控评级
- AND SHALL NOT 记录候选输出、用户日记、对话原文、记忆片段、prompt 或 provider response

#### Scenario: 类型化指令审查

- GIVEN 开发者审查 reflection 指令
- WHEN 判断是否符合 D29
- THEN SHALL 能从固定 violation mapping 完整审计
- AND SHALL NOT 依赖“调用方记得不要拼文本”的约定

### Requirement: Reflection Baseline Changes Must Be Reviewed Not Refreshed

#### Scenario: C6 快照变更

- GIVEN C7 导致 C6 snapshot mismatch
- WHEN 处理该差异
- THEN 开发者 SHALL 先判断变化是否来自批准的 C7 行为
- AND 只有确认后才可手工更新指标、`baselineNote` 与 checksum
- AND SHALL NOT 批量自动接受当前输出

#### Scenario: 不变量失败

- GIVEN C6 硬不变量失败
- WHEN C7 实现者处理失败
- THEN SHALL 将其视为实现缺陷或契约冲突
- AND SHALL NOT 通过修改既有断言、阈值或 eligible 集合换取通过

### Requirement: Real Reflection Probes Must Follow A Separate Budgeted Gate

#### Scenario: 未授权

- GIVEN 闸门 2 已允许实现但闸门 3 未批准
- WHEN 验证 C7
- THEN SHALL 只运行离线与本地数据库验证
- AND SHALL NOT 启用真实 provider 探针

#### Scenario: 授权后的调用预算

- GIVEN 用户单独批准闸门 3
- WHEN 执行真实探针
- THEN 总调用数 SHALL NOT 超过 6
- AND SHALL 先执行最多 2 次调用的 canary
- AND 超时、identity/config 漂移、调用超限、错误路径重试或敏感内容入证据 SHALL 立即停止后续调用

#### Scenario: 人评锚点

- GIVEN 同批真实输出被人工观察
- WHEN 填写 C6 narrative anchors
- THEN SHALL 只保存受控等级与版本/路径元数据
- AND SHALL 标明样本规模与非绝对质量结论
- AND SHALL NOT 保存样本文本

### Requirement: Reflection Findings Must Not Expand C7 Scope

#### Scenario: 实现中发现其他问题

- GIVEN C7 轨迹或评测暴露阈值、检索、韧性、时间智能或 UI 问题
- WHEN 决定是否修复
- THEN SHALL 记录为 residual 或另起 change
- AND SHALL NOT 在 C7 顺手修改 C4 阈值、C8 错误策略、C9 时间策略或前端体验

## Accepted From C8 agent-resilience

> Accepted on 2026-08-08. C8 的失败证据保持结构化、无内容；真实 provider 探针须独立授权、
> 预算化执行，且不得借发现扩张本刀范围。

### Requirement: Resilience Evidence Must Be Structured And Content Free

#### Scenario: 记录 provider failure

- GIVEN timeout、throttling、auth/config、upstream、invalid 或 unknown failure 被测试或观察
- WHEN Agent 写 trace、日志、AGENT_LOG、测试报告或 closeout
- THEN SHALL 只记录 category、phase、transient、调用次数、耗时/预算桶与终态
- AND SHALL NOT 记录 exception message、HTTP body、prompt、用户日记、对话、memory、candidate 或 provider response

### Requirement: Failure Paths Must Be Tested Offline Before Real Probes

#### Scenario: 故障注入

- GIVEN C8 需要验证 401/403/429/5xx/timeout/connect/invalid/interrupted
- WHEN 执行实现期测试
- THEN SHALL 使用 fake HTTP client、scripted provider 与 fake clock 离线注入
- AND SHALL NOT 为制造故障而调用真实 provider 或篡改真实 credential

#### Scenario: baseline 变化

- GIVEN C8 改变 C6 snapshot 或 trace shape
- WHEN 接受差异
- THEN SHALL 逐条说明 approved C8 原因并同步 `baselineNote` + checksum
- AND SHALL NOT 批量刷新、削弱不变量或修改既有断言换绿

### Requirement: Real Resilience Probes Require A Separate Budgeted Gate

#### Scenario: 未授权

- GIVEN 闸门 2 已允许实现但闸门 3 未批准
- WHEN 验证 C8
- THEN SHALL 只运行离线与本地数据库/前端验证
- AND SHALL NOT 启用真实 provider 或真机外调

#### Scenario: 授权后的预算

- GIVEN 用户单独批准真实 provider/真机验收
- WHEN 执行探针
- THEN 总 provider 调用 SHALL NOT 超过 8
- AND SHALL 先执行最多 2 次正常 canary
- AND SHALL NOT 主动制造鉴权失败、限流、provider outage 或发送真实日记内容
- AND 前端先超时、调用超限、identity/config 漂移或敏感内容入证据 SHALL 立即停止

### Requirement: Resilience Findings Must Not Expand C8 Scope

#### Scenario: 发现生产级容灾需求

- GIVEN C8 观察到自动 retry、路由、熔断、缓存、监控或部署需求
- WHEN 决定是否实现
- THEN SHALL 记录为 residual 或另起独立 change
- AND SHALL NOT 在 C8 第一阶段顺手加入这些能力或 C9 时间智能

## Accepted From C9 Agent Temporal Intelligence

### Requirement: Temporal Evidence Must Be Reproducible And Content Free

#### Scenario: 固定时钟验证

- GIVEN C9 测试距离边界、衰减或 recurrence eligibility
- WHEN Agent 运行离线验证
- THEN SHALL 使用 fixed/fake `Clock` 与固定合成记录
- AND SHALL 明确覆盖 30/31、180/181 天边界、null/future 时间与 90 天跨度边界
- AND SHALL NOT 依赖测试运行当天的真实日期

#### Scenario: 证据记录

- GIVEN temporal policy 结果被写入 trace、AGENT_LOG、测试报告或 closeout
- WHEN 形成证据
- THEN SHALL 只记录策略版本、距离桶、字符数、eligible/used、违规枚举与调用次数
- AND SHALL NOT 记录用户日记、对话、关键词、片段、prompt、provider response 或精确记录时间清单

### Requirement: Temporal Changes Must Extend C6 Without Rubber Stamping

#### Scenario: 新增 C9 用例

- GIVEN C9 改变 prompt、guardrail 或 trace shape
- WHEN 扩展 C6 eval
- THEN SHALL 新增时间距离、衰减、recurrence 正反例、overreach、无额外调用与隐私用例
- AND 既有非 C9 行为用例 SHALL 保持

#### Scenario: snapshot 变化

- GIVEN 某条既有 snapshot 因 C9 合法行为发生变化
- WHEN 接受差异
- THEN SHALL 逐条人工确认并同步写明 C9 原因的 `baselineNote` 与 checksum
- AND SHALL NOT 自动重写、批量刷新、削弱不变量或修改既有断言换绿

### Requirement: Real Temporal Probes Require A Separate Synthetic Data Gate

#### Scenario: 未授权

- GIVEN 闸门 2 已允许实现但闸门 3 未批准
- WHEN 验证 C9
- THEN SHALL 只运行离线/fake-clock 测试与不产生外调的本地验证
- AND SHALL NOT 调用真实 provider、连接真实 MySQL 探针或进行真机验收

#### Scenario: 授权后的 provider 预算

- GIVEN 用户单独批准真实 provider 验收
- WHEN 执行 C9 探针
- THEN provider 总调用 SHALL 不超过 6
- AND SHALL 只使用固定合成短文本，覆盖距离、回看锚点、recurrence 与不足证据反例
- AND SHALL NOT 发送真实日记、真实回看记录或未脱敏用户数据

#### Scenario: 授权后的 MySQL 与真机

- GIVEN 用户单独批准真实 MySQL / 真机验收
- WHEN 执行验证
- THEN MySQL SHALL 使用可清理的合成用户与不同年龄记录
- AND 真机 SHALL 只验证现有表面的时间话术、长度与无分析 UI
- AND 无可控环境时 SHALL 记录 `SKIPPED + 原因`，不得以 H2/build/scripted provider 冒充

### Requirement: Temporal Findings Must Not Expand Into Analytics Or Retrieval Rewrite

#### Scenario: 发现相关性或产品分析需求

- GIVEN C9 观察到向量检索、相关性打分、统计周期、dashboard、提醒或长期画像需求
- WHEN 决定是否实现
- THEN SHALL 记录为 residual 或另起独立 OpenSpec change
- AND SHALL NOT 在 C9 顺手修改 memory SQL、引入分析存储、外部引擎、C10 语气标定或 C11 上下文架构

## Accepted From P3.2 Data Ownership Foundation

### Requirement: Export Acceptance Must Verify Structure Readability And Media Integrity

P3.2 evidence SHALL verify the produced export as a user-owned artifact rather than relying on endpoint or build success alone.

#### Scenario: Offline export evidence is recorded

- **GIVEN** a synthetic export operation succeeds
- **WHEN** acceptance evidence is collected
- **THEN** it SHALL verify exact package structure, offline HTML, readable Markdown, relative links, media byte length, and SHA-256
- **AND** SHALL distinguish H2/synthetic results from real private-object and WeChat delivery evidence

#### Scenario: Sealed and Agent boundaries are reviewed

- **GIVEN** the package contains sealed records or Agent content
- **WHEN** export evidence is inspected
- **THEN** both sealed policies SHALL be tested
- **AND** user original and Agent content SHALL be proven distinguishable

### Requirement: Deletion Acceptance Must Prove Remote Cleanup And Recovery

P3.2 deletion evidence SHALL prove remote-object handling, database association cleanup, and interruption recovery.

#### Scenario: Database cascade passes locally

- **GIVEN** an H2 or MySQL test shows associated rows were cascaded
- **WHEN** deletion is reported
- **THEN** that result SHALL NOT be used as proof that Qiniu or S3-compatible objects were deleted

#### Scenario: Real deletion probe is authorized

- **GIVEN** Gate 3b authorizes synthetic private-object probes
- **WHEN** deletion acceptance runs
- **THEN** it SHALL cover provider success, not-found idempotency, retryable failure, and restart between remote and database deletion
- **AND** synthetic objects and artifacts SHALL be cleaned afterward

#### Scenario: Clear-all is verified

- **GIVEN** a clear-all operation is tested
- **WHEN** evidence is recorded
- **THEN** it SHALL verify fixed owner snapshot, mutation freeze, progress, retry scope, associated-data absence, and no cross-owner impact

### Requirement: Ownership Evidence Must Preserve Privacy And Gate Boundaries

Planning, implementation, and acceptance evidence SHALL minimize sensitive data and keep external side effects separately authorized.

#### Scenario: Planning artifacts are created

- **GIVEN** Gate 1 planning is in progress
- **WHEN** proposal, design, tasks, or deltas are written
- **THEN** real MySQL, object storage, WeChat, provider, push, deployment, and release SHALL remain unauthorized
- **AND** planning SHALL NOT modify business code

#### Scenario: Verification is skipped

- **GIVEN** OpenSpec CLI, real MySQL, private object storage, or WeChat environment is unavailable or unauthorized
- **WHEN** results are reported
- **THEN** the item SHALL be marked `SKIPPED` with a reason
- **AND** file checks, H2, builds, or desktop download SHALL NOT be promoted as equivalent evidence

#### Scenario: Logs and tracked evidence are reviewed

- **GIVEN** export or deletion evidence is added
- **WHEN** privacy scanning runs
- **THEN** diary content, media content, location detail, storage keys, signed URLs, download tokens, credentials, prompts, and provider responses SHALL be absent

### Requirement: Witness Alignment Baseline Changes Must Be Deliberate And Attributed

P4.1 引起的 C6 fixture、snapshot、metric 或 baseline 变化 SHALL 逐项审查并明确归属；硬不变量 SHALL NOT 被削弱。

#### Scenario: P4.1 snapshot 变化

- GIVEN witness role、intent 或 turn policy 导致 C6 snapshot mismatch
- WHEN 实现者处理差异
- THEN SHALL 先确认变化来自已批准的 P4.1 契约
- AND 合法变化的 `baselineNote` SHALL 以 `P4.1 witness-agent-alignment:` 开头并说明原因
- AND checksum SHALL 按 C6 机制同步更新

#### Scenario: 不得静默刷新

- GIVEN baseline compare 失败
- WHEN 实现者希望继续
- THEN SHALL NOT 批量接受当前输出、自动重写 baseline 或只修改 checksum
- AND 每项变化 SHALL 能映射到固定 case 与批准 requirement

#### Scenario: 硬不变量失败

- GIVEN 问题上限、长度、工具、忠实度、时间、trace 或 privacy 不变量失败
- WHEN 实现者处理失败
- THEN SHALL 将其视为实现缺陷或契约冲突
- AND SHALL NOT 通过放宽断言、阈值或删除用例换取通过

### Requirement: Witness Alignment Evaluation Must State Its Honest Boundary

离线与 scripted 评测 SHALL 只证明编排、问题上限和既有 guardrail 回归，不得被描述为真实语言质量已经成立。

#### Scenario: 离线评测通过

- GIVEN C6、focused tests 与 scripted fixtures PASS
- WHEN 汇报 P4.1 验证
- THEN SHALL 明确其覆盖 intent、stage、turn policy、question enforcement、fallback 与回归边界
- AND SHALL NOT 宣称自然、温柔、理解准确或“不抢结论”已经由此证明

#### Scenario: 人评锚点未填充

- GIVEN Gate 3a 未授权或未执行
- WHEN 汇报 witness language quality
- THEN SHALL 标为 `SKIPPED` 或未填充
- AND SHALL NOT 把空锚点视为 PASS

#### Scenario: 固定合成样本

- GIVEN P4.1 增加 eval fixtures
- WHEN 样本被 tracked
- THEN SHALL 只使用无真实用户内容的合成文本
- AND baseline/report SHALL 只保存 case id、结构化指标、版本与结论

### Requirement: Real Witness Probes Must Follow A Separate Budgeted Gate

真实 provider 对 witness 气质的验证 SHALL 使用独立 Gate、固定合成样本和硬调用预算。

#### Scenario: 未授权

- GIVEN Gate 2 已允许实现但 Gate 3a 未批准
- WHEN 验证 P4.1
- THEN 真实 provider 调用数 SHALL 为 0
- AND SHALL 只运行离线、mock/scripted 与 build 验证

#### Scenario: Canary 与调用预算

- GIVEN 用户单独批准 Gate 3a
- WHEN 执行真实 provider 探针
- THEN SHALL 先执行最多 2 次 canary
- AND 最多覆盖 6 个固定合成 case
- AND 包括 reflection 在内的 provider 总调用数 SHALL 不超过 8

#### Scenario: 立即停止条件

- GIVEN canary 或正式探针发生 identity/config 漂移、超时、调用超限、错误路径重试、非合成输入或证据泄露
- WHEN 执行者观察到该条件
- THEN 后续真实调用 SHALL 立即停止
- AND 结果 SHALL 如实标为 FAIL、SKIPPED 或 INCONCLUSIVE

#### Scenario: 人评锚点

- GIVEN 同批真实输出被人工观察
- WHEN 填写 P4.1 anchors
- THEN SHALL 只记录 witness role、user control、question restraint、brief-answer restraint、uncertainty humility、no forced conclusion 的等级与版本/路径元数据
- AND SHALL 标明样本规模与非绝对质量结论
- AND SHALL NOT 保存输入、输出、prompt 或用户内容

### Requirement: Witness MySQL And WeChat Acceptance Must Stay In Separate Gates

真实 MySQL migration/恢复与微信交互 SHALL 分别授权，不得由 H2、build、截图或 provider 探针代替。

#### Scenario: MySQL 未授权

- GIVEN Gate 3c 未批准
- WHEN 实现者完成 H2 schema 与 migration contract tests
- THEN 真实 MySQL SHALL 标为 SKIPPED
- AND SHALL NOT 声称历史 stage 分布或生产迁移已确认

#### Scenario: MySQL 授权后

- GIVEN 用户批准 Gate 3c
- WHEN 执行 preflight 与 migration probe
- THEN 只读 preflight SHALL 只输出 schema 与 stage/intent 聚合计数
- AND migration/恢复 SHALL 只使用合成 session/message
- AND SHALL NOT 读取或输出真实日记、对话或模型回复

#### Scenario: 微信未授权

- GIVEN Gate 3b 未批准
- WHEN frontend type-check 与 mp-weixin build PASS
- THEN 入口选择、intent switch、键盘、短答和结束体验 SHALL 标为未真机验证
- AND build SHALL NOT 被写成微信交互 PASS

#### Scenario: 微信授权后

- GIVEN 用户批准 Gate 3b
- WHEN 执行开发者工具或真机矩阵
- THEN SHALL 分别记录使用的客户端、入口选择、切换、短答、结束、失败与 Preview 结论
- AND 截图或录屏 SHALL NOT 包含真实用户日记或 secret

### Requirement: Witness Evidence Must Remain Content Free And Scope Bound

P4.1 协作证据 SHALL 只包含结构化决策与结论，且评测发现 SHALL NOT 顺手扩大到 P4.2 或其他 Agent 能力。

#### Scenario: 记录 question violation 或 intent switch

- GIVEN 实现或验证产生 question violation、reflection、fallback 或 intent switch 证据
- WHEN 写入 AGENT_LOG、report 或 closeout
- THEN SHALL 只记录 case id、enum、limit、count、outcome、version 与 PASS/FAIL/SKIPPED
- AND SHALL NOT 记录用户输入、模型文本、prompt、memory fragment 或 secret

#### Scenario: 发现关系或语气问题

- GIVEN 小样本人评发现未被 P4.1 fixed cases 覆盖的语气问题
- WHEN 决定处置
- THEN MAY 在 P4.1 已批准的 witness role/question boundary 内修复并补固定 case
- AND 检索、memory authorization、工具扩面、评分、建议或产品表面问题 SHALL 记录为 residual/独立 change

#### Scenario: 规划工件

- GIVEN P4.1 尚处 Gate 1
- WHEN proposal/design/tasks/deltas 被创建
- THEN SHALL 记录外部调用数 0、提交责任、OpenSpec CLI 边界与 Gate 状态
- AND SHALL NOT 把规划批准写成实现、真实外调或归档授权

### Requirement: Memory Agency Evidence Must Prove Authorization And Source Invariants

P4.2 验收 SHALL 证明用户授权关闭时的零检索、实际来源同源与撤销即时性，配置开关或 UI 截图不得替代。

#### Scenario: Authorization off evidence

- GIVEN session authorization=false
- WHEN 执行离线编排与真实 MySQL 合成探针
- THEN 证据 SHALL 证明 cue extractor/MemoryPort 调用与 cross-record injection 均为 0
- AND `AGENT_MEMORY_ENABLED` 的值 SHALL NOT 单独被写成用户授权证据

#### Scenario: Source exact match evidence

- GIVEN 检索候选、temporal final injection 与 persisted sources 可分别观测
- WHEN 验收一轮合成 turn
- THEN persisted/user-visible sources SHALL 与 final injected list exact-match
- AND 候选数量、关键词、分数或内容 SHALL 不进入报告

#### Scenario: Revocation evidence

- GIVEN session 曾开启并成功使用 source
- WHEN 执行撤销、record exclusion 或 delete 后的下一轮
- THEN 证据 SHALL 显示未来检索/注入被阻止
- AND SHALL 不声称撤回既往 provider 调用

### Requirement: Real MySQL Must Validate Schema Transaction And Deletion Semantics

涉及 source 外键、事务一致性与删除的结论 SHALL 在用户单独授权后由真实 MySQL 合成探针验证。

#### Scenario: MySQL gate

- GIVEN Gate 3a 已授权
- WHEN migration 连续两次并执行合成矩阵
- THEN SHALL 验 schema/default/owner/status/rollback/delete SET NULL/cleanup
- AND SHALL 不读取或输出真实 record/message/note 内容与标识

#### Scenario: H2 boundary

- GIVEN H2 integration tests 全部通过
- WHEN 报告外键、锁或事务结论
- THEN SHALL 仍将真实 MySQL 标为待验证，直到 Gate 3a PASS

### Requirement: WeChat Evidence Must Separate Real UI Boundaries From Provider Quality

微信验收 SHALL 区分 Standard/Preview、开发者工具/物理真机和 scripted/provider 证据。

#### Scenario: Standard matrix

- GIVEN Gate 3b 已授权且真实登录边界可用
- WHEN 验授权开关、来源、跳转、排除、说明、错误与删除
- THEN SHALL 记录交互结果且不输出用户内容或标识
- AND scripted Agent response 只可证明 UI/请求编排

#### Scenario: Preview matrix

- GIVEN 无真实登录 Preview
- WHEN 执行同一入口检查
- THEN Agent/authorization/policy/source backend 请求数 SHALL 为 0
- AND SHALL 不把假数据写成真实来源成功

#### Scenario: Physical device boundary

- GIVEN 只完成微信开发者工具验证
- WHEN 输出结论
- THEN SHALL 明确写“开发者工具”
- AND 物理真机 SHALL 记为 SKIPPED 而非 PASS

### Requirement: P4.2 Must Keep Provider Calls And Sensitive Evidence Bounded

P4.2 默认 SHALL 使用零真实 provider 调用完成核心授权/来源验收；如需语言质量证据必须重新申请。

#### Scenario: Default provider budget

- GIVEN P4.2 核心 claim 为 consent/source/revocation
- WHEN 执行 Gate 2/3
- THEN 真实 provider 调用预算 SHALL 为 0
- AND P4.1 已用尽的调用预算 SHALL NOT 被继承

#### Scenario: New provider request

- GIVEN 实现后出现独立语言质量问题
- WHEN 团队希望调用真实 provider
- THEN SHALL 先定义固定合成样本、硬上限、人评目标与 fail-stop 条件并取得单独授权
- AND SHALL 不保存用户/模型文本、prompt、secret 或长期画像

### Requirement: Eval Changes Must Carry Explicit P4.2 Attribution

C6 fixtures 与 baseline 的任何变化 SHALL 可审查且不得静默刷新。

#### Scenario: Legitimate baseline change

- GIVEN P4.2 授权门控改变记忆注入指标
- WHEN snapshot 需要更新
- THEN 每项变化 SHALL 带 `baselineNote=P4.2 memory-agency: <reason>`
- AND checksum SHALL 与指标和说明同步

#### Scenario: No quality overclaim

- GIVEN 离线 spy、MySQL 与微信矩阵 PASS
- WHEN closeout 描述结果
- THEN SHALL 只声称授权、来源、撤销和交互边界通过
- AND SHALL NOT 声称 provider 自然度、生产 SLA 或长期隐私效果已验证

### Requirement: Safety Evidence Must Use Synthetic Inputs And Explicit Boundaries

R1 验收 SHALL 使用固定合成输入，并区分分类、编排、provider、地区资源和真实救援效果。

#### Scenario: Synthetic matrix

- GIVEN 正例与普通低落、否定、转述、历史、比喻负例
- WHEN 执行离线与授权的小样本验证
- THEN SHALL 记录结构化 decision/count/status
- AND SHALL NOT 保存真实用户危机内容、provider 文本、prompt、token 或 secret

#### Scenario: Provider boundary

- GIVEN 本地安全分支 PASS
- WHEN closeout 描述结果
- THEN SHALL 只声称高置信输入不调用 provider 且返回固定响应
- AND SHALL NOT 声称临床有效、所有表达可识别或生产救援可用

### Requirement: Regional Resource Claims Must Be Reverified

地区资源 SHALL 使用官方来源并在 closeout 或发布前重新核验。

#### Scenario: Mainland China verification

- GIVEN R1 文案包含 12356、110、120
- WHEN 验收
- THEN SHALL 记录国家卫健委/政府部门来源与核验日期
- AND 二手文章 SHALL NOT 成为唯一依据
