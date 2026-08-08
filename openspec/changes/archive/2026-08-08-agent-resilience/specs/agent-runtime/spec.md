# Agent Runtime Spec Delta：agent-resilience（C8）

> 规划草案。范围：provider failure taxonomy、整轮 deadline、零自动 retry 与显式失败终态。

## MODIFIED Requirements

### 修订：`Reflection Must Not Become Error Retry Or Tool Loop` 的 Provider failure scenario

#### Scenario: Provider failure

> C8 修订：C7 的“不因 reflection 自动重试”继续成立；C8 增加分类与用户主动 retry，但第一阶段仍不做自动 provider retry。

- GIVEN initial 或 reflection provider 调用失败或返回无效内容
- WHEN 系统处置
- THEN SHALL 按 C8 封闭 taxonomy 分类
- AND SHALL NOT 在同一请求自动再次调用 provider
- AND 用户 MAY 通过既有入口主动重试同一轮

## ADDED Requirements

### Requirement: Provider Failures Must Use A Closed Typed Taxonomy

#### Scenario: 已知错误来源

- GIVEN provider 调用发生 timeout、throttling、auth/config、upstream unavailable、invalid response、request rejected 或 interrupted
- WHEN backend 分类该失败
- THEN SHALL 使用封闭的稳定 category
- AND SHALL NOT 通过异常 message 或 response body 关键词完成核心分类

#### Scenario: 未知错误

- GIVEN 错误无法映射到已知类别
- WHEN 形成失败终态
- THEN category SHALL 为 `UNKNOWN`
- AND SHALL NOT 因 unknown 自动重试 provider

### Requirement: Every Agent Orchestration Must Share One Provider Work Deadline

#### Scenario: 多 provider 子调用

- GIVEN 一次 Agent HTTP 编排可能包含 initial/reflection 或 reply/material
- WHEN 发起每个 provider 子调用
- THEN 所有子调用 SHALL 共享同一 request-scope provider-work budget
- AND 每次 timeout SHALL NOT 超过当前剩余预算与单次 provider 上限的较小值

#### Scenario: 预算耗尽

- GIVEN 整轮剩余预算已不足以安全发起下一次调用
- WHEN reflection 或 material 准备调用 provider
- THEN 系统 SHALL 不发起该调用
- AND SHALL 形成类型化 timeout/deadline-exhausted 终态

### Requirement: C8 Must Not Add Automatic Provider Retry

#### Scenario: 暂态错误

- GIVEN provider 返回 timeout、429、连接失败或 5xx
- WHEN C8 第一阶段处置
- THEN SHALL NOT 在同一请求自动 retry
- AND 用户 MAY 通过既有同轮重试入口稍后主动重试

#### Scenario: 调用数上限

- GIVEN C7 reflection 与 CLOSING material 语义保持不变
- WHEN C8 执行
- THEN 非 `CLOSING` reply provider calls SHALL 不超过 2
- AND `CLOSING` reply + material provider calls SHALL 不超过 2
- AND finish material-only provider calls SHALL 不超过 1

### Requirement: Provider Failure Must Remain Distinct From Guardrail Fallback

#### Scenario: provider 调用失败

- GIVEN provider 未生成可用回复
- WHEN 返回该轮结果
- THEN status SHALL 为 `FAILED` 或 `UNAVAILABLE`
- AND Assistant message SHALL NOT 被持久化
- AND 本地失败模板 SHALL NOT 被冒充为 provider 正常回复

#### Scenario: 护栏本地兜底

- GIVEN provider 已返回内容但确定性护栏决定降级
- WHEN 用户收到既有安全兜底
- THEN 既有 `DOWNGRADED` 轨迹语义 SHALL 保持
- AND provider failure taxonomy SHALL NOT 覆盖或弱化护栏违规类型

### Requirement: Failure Presentation Must Be Stage Aware And Backend Controlled

#### Scenario: 固定模板映射

- GIVEN opening、普通 turn 或 closing/material 路径失败
- WHEN backend 生成用户可见 message
- THEN SHALL 仅按 operation/stage/category 从固定映射选择
- AND SHALL NOT 拼接用户文本、prompt、异常 message 或 provider response

#### Scenario: 产品气质

- GIVEN 用户看到失败提示
- WHEN 阅读该提示
- THEN 文案 SHALL 克制、温和且说明已提交内容未丢（适用时）
- AND SHALL NOT 使用诊断、技术术语、后台恢复承诺或虚假共情

### Requirement: Failure Classification Must Be Observable Without Content Leakage

#### Scenario: turn trace

- GIVEN provider failure 发生在 initial/reflection/material
- WHEN 写结构化轨迹
- THEN SHALL 记录 phase、category、transient 与 budget 状态
- AND SHALL NOT 记录 exception message、HTTP body、prompt、用户文本或 provider response

#### Scenario: opening

- GIVEN opening 发生在 turnNo=0 且不属于用户轮次
- WHEN 记录失败
- THEN SHALL 使用脱敏结构化日志
- AND SHALL NOT 为可观测性伪造 turn trace row
