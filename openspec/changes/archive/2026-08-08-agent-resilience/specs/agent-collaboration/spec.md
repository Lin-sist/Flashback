# Agent Collaboration Spec Delta：agent-resilience（C8）

> 规划草案。范围：故障证据、离线注入、真实探针与 scope 纪律。

## ADDED Requirements

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
