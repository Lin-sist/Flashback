# Agent Runtime Spec Delta：agent-temporal-intelligence（C9）

> 规划草案。范围：确定性时间距离、旁支记忆注入衰减、克制重复主题提示与反分析硬边界。

## MODIFIED Requirements

### 修订：`Memory References Must Carry Explicit Time Attribution`

#### Scenario: 带距离感地引用记忆

> C9 修订：C3 的月份级时间归属继续是硬约束；C9 在不替换准确锚点的前提下补充距离层级。

- GIVEN Agent 回复引用了一条带 `occurredAt` 的记忆片段
- WHEN backend 组装时间上下文
- THEN SHALL 保留该片段既有月份级 `timeLabel`
- AND SHALL 以确定性的距离层级补充“最近、数月前或更早”的语义
- AND SHALL NOT 让相对距离替代或模糊准确的来源锚点

#### Scenario: 时间未知或位于未来

- GIVEN 记忆片段缺少发生时间或发生时间晚于当前 `Clock`
- WHEN backend 计算时间距离
- THEN 距离层级 SHALL 为 `UNKNOWN`
- AND Agent SHALL NOT 获得虚假的天数、月份或“已经过去多久”结论
- AND 既有“以前/那时候”安全归属仍可使用

## ADDED Requirements

### Requirement: Temporal Distance Must Be Deterministic And Clock Driven

#### Scenario: 距离层级边界

- GIVEN 固定 `Clock` 与一条已知发生时间的记忆片段
- WHEN backend 计算时间距离
- THEN 0–30 天 SHALL 为 `RECENT`
- AND 31–180 天 SHALL 为 `DISTANT`
- AND 181 天以上 SHALL 为 `LONG_AGO`
- AND 同一输入与时钟 SHALL 始终得到同一结果

#### Scenario: 零额外调用

- GIVEN temporal policy 被启用
- WHEN 一轮写作引导或回看对话被编排
- THEN 时间距离 SHALL 在 backend 本地计算
- AND SHALL NOT 为分类、衰减或 pattern eligibility 新增 provider 调用

### Requirement: Older Ancillary Memories Must Receive A Smaller Injection Budget

#### Scenario: 旁支记忆衰减

- GIVEN memory port 返回一条关联历史片段
- WHEN temporal policy 形成最终注入片段
- THEN `RECENT` SHALL 使用既有单片段字符上限的 100%
- AND `DISTANT` SHALL 使用 75%
- AND `LONG_AGO` / `UNKNOWN` SHALL 使用 50%
- AND 任一片段预算 SHALL 不低于 40 字且不超过既有配置上限

#### Scenario: 用户主动打开的回看目标记录

- GIVEN 用户主动打开某条 UNLOCKED 记录并进入回看对话
- WHEN backend 注入该目标记录自身的片段
- THEN 目标记录 SHALL 保持既有 `review.record-excerpt-chars` 预算
- AND SHALL NOT 因距离久远而被当作旁支记忆衰减

#### Scenario: 业务记录与检索范围

- GIVEN temporal 衰减已启用
- WHEN 审查记录与检索行为
- THEN SHALL NOT 删除、修改、过期或隐藏任何业务记录
- AND SHALL NOT 改变 memory port 的 24 个月窗口、owner/status 隔离、匹配字段或无线索不查询语义

### Requirement: Recurrence Hints Must Be Evidence Gated And User Invited

#### Scenario: 允许形成重复主题提示

- GIVEN 会话模式为 `REVIEW_CHAT`
- AND 当前用户消息明确表达比较、再次发生或回看变化的意图
- AND 至少两个不同的旁支历史记录具有已知发生时间且跨度不小于 90 天
- WHEN temporal policy 评估 pattern eligibility
- THEN MAY 形成每轮最多一个无内容的 recurrence hint
- AND hint SHALL 只允许 Agent 说“似乎不止一次出现过”并邀请用户自己判断

#### Scenario: 证据不足

- GIVEN 会话不是回看模式、用户未表达比较意图、仅有目标记录、旁支记录少于两个或时间跨度不足
- WHEN temporal policy 评估 pattern eligibility
- THEN SHALL NOT 形成 recurrence hint
- AND Agent SHALL NOT 为显得有时间智能而主动归纳模式

#### Scenario: 不得声称周期结论

- GIVEN recurrence hint 已形成
- WHEN Agent 回应
- THEN SHALL NOT 声称“每年、每隔固定时间、总是、必然、越来越严重/改善”
- AND SHALL NOT 推断原因、预测未来、给出评分或替用户定义成长/退步

### Requirement: Temporal Overreach Must Fail Closed Without Expanding Reflection

#### Scenario: 检出分析化时间话术

- GIVEN provider 回复包含百分比/评分、绝对频率、必然因果、心理诊断、趋势结论或预测式时间话术
- WHEN deterministic temporal checker 执行
- THEN SHALL 返回内部 `TEMPORAL_OVERREACH` 违规
- AND SHALL 使用既有安全兜底替换该回复
- AND SHALL NOT 持久化越界 Assistant 内容

#### Scenario: reflection 与调用预算

- GIVEN `TEMPORAL_OVERREACH` 被检出
- WHEN backend 决定后续路径
- THEN SHALL NOT 进入 C7 reflection
- AND 非 CLOSING、CLOSING+material 与 finish 的既有 provider 调用上限 SHALL 保持不变

### Requirement: Temporal Context Must Preserve Existing Source And Content Boundaries

#### Scenario: temporal hint 的生命周期

- GIVEN backend 为一轮对话形成距离层级、衰减预算或 recurrence hint
- WHEN prompt、来源集合与持久化被组装
- THEN temporal hint SHALL 只存在于本轮内存与 provider prompt
- AND SHALL NOT 进入用户记录、素材、工具参数、agent message、日志或新的持久化位置

#### Scenario: 既有护栏 authority

- GIVEN temporal policy 已启用
- WHEN Agent 回复或工具提议被检查
- THEN 忠实度、诊断、代决、伪引用、时间归属、工具白名单与长度上限 SHALL 全部保持
- AND temporal policy SHALL NOT 扩大 memory layer 成为用户正文合法来源
