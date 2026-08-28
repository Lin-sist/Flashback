# Delta Spec：backend-core（R1 Safety Response Minimum）

## ADDED Requirements

### Requirement: Backend Safety Policy Must Be Deterministic And Content Minimal

后端 SHALL 使用封闭枚举与固定规则识别窄安全例外，不使用诊断、概率评分或额外模型判断。

#### Scenario: Safety match

- GIVEN 当前输入命中明确规则
- WHEN policy 返回 decision
- THEN 结果 SHALL 只包含 enum 与 ruleId
- AND SHALL NOT 复制输入、建立诊断或写长期风险字段

#### Scenario: Safety response persistence

- GIVEN 本轮进入安全分支
- WHEN assistant response 持久化
- THEN SHALL 复用既有 message/session 事务与 owner scope
- AND provider/memory/tool/material/source 调用 SHALL 为 0

### Requirement: Backend Must Not Claim Rescue Actions It Cannot Perform

安全响应 SHALL 明确系统能力边界。

#### Scenario: No human handoff

- GIVEN 系统没有人工坐席、自动报警或联系人通知能力
- WHEN 返回安全响应
- THEN SHALL 明确无法替用户通知任何人
- AND SHALL NOT 声称报警、呼叫救护车或人工接管已经发生
