# Delta Spec：agent-runtime（R1 Safety Response Minimum）

## ADDED Requirements

### Requirement: Imminent Safety Risk Must Preempt Ordinary Agent Work

Runtime SHALL 在高置信、紧迫的第一人称自伤风险下优先返回 backend-owned 安全响应。

#### Scenario: High confidence immediate risk

- GIVEN 当前输入明确表示正在实施或将立即实施自伤
- WHEN Runtime 处理该轮
- THEN SHALL 在 provider、memory、tool 与 material 之前进入安全分支
- AND SHALL 返回本地安全响应且保持 session 可继续

#### Scenario: Ordinary distress

- GIVEN 输入只是普通低落、失败、迷茫、比喻、否定、转述或历史表达
- WHEN 安全策略评估
- THEN SHALL NOT 病理化或进入紧急响应
- AND 既有 witness 编排 SHALL 保持

### Requirement: Safety Response Must Not Create Lasting Risk State

R1 SHALL 只处理当前 turn，不建立永久风险标签、画像或跨 session 状态。

#### Scenario: After a safety response

- GIVEN 某轮触发本地安全响应
- WHEN 用户继续下一轮或开始其他 session
- THEN SHALL 重新只按当前输入评估
- AND SHALL NOT 因历史触发自动复用安全标签
