# Delta Spec：agent-collaboration（P4.2 Memory Agency）

## ADDED Requirements

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
