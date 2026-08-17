# Delta Spec：agent-collaboration（P4.1 Witness Agent Alignment）

## ADDED Requirements

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
