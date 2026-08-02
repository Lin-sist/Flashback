# Agent Collaboration Spec Delta：agent-reflection-loop（C7）

> 闸门 1 已于 2026-08-02 批准。范围：重写指令、评测证据、外调与基线更新的协作纪律。

## ADDED Requirements

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
