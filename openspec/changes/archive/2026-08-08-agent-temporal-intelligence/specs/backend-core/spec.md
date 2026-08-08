# Backend Core Spec Delta：agent-temporal-intelligence（C9）

> 规划草案。范围：backend-side temporal policy/config、现有 memory port 复用、外部契约与存储零扩张。

## ADDED Requirements

### Requirement: Temporal Policy Must Be A Backend Side L3 Module

#### Scenario: 职责位置

- GIVEN C9 被实现
- WHEN 审查时间距离、衰减与 recurrence eligibility
- THEN 这些决策 SHALL 位于独立 backend `agent/temporal` 策略模块
- AND service 编排 SHALL 消费类型化策略结果而非散落计算阈值
- AND frontend SHALL NOT 负责推导时间距离或模式证据

#### Scenario: 时钟来源

- GIVEN temporal policy 需要当前时间
- WHEN 计算日期差
- THEN SHALL 使用注入的 `Clock`
- AND SHALL NOT 在策略内部散落调用系统时间

### Requirement: Temporal Configuration Must Be Backend Side And Credential Free

#### Scenario: 配置范围

- GIVEN temporal policy 存在总开关、距离边界、衰减比例、最小字符预算与 recurrence 时间跨度
- WHEN 配置被声明
- THEN SHALL 位于 `app.agent.temporal` backend-side config
- AND SHALL 具有安全默认值与 Bean Validation
- AND SHALL NOT 引入 credential、endpoint 或 frontend-visible setting

#### Scenario: 开关关闭

- GIVEN `temporal.enabled=false`
- WHEN Agent 对话被处理
- THEN 行为 SHALL 等价于 C8 已接受行为
- AND backend SHALL 留无内容结构化痕迹说明 temporal policy 未生效
- AND SHALL NOT 静默伪装成“无历史模式”

### Requirement: C9 Must Reuse Existing Memory Port And Record Time Fields

#### Scenario: 检索与时间来源

- GIVEN C9 需要历史片段与发生时间
- WHEN backend 构造 temporal context
- THEN SHALL 复用现有 `MemoryPort` / `MemoryFragment.occurredAt`
- AND 回看目标记录 SHALL 继续以 `record.created_at` 作为“那时”的发生时间
- AND SHALL NOT 另建平行检索实现、统计查询或分析表

#### Scenario: 相关性边界

- GIVEN 当前 memory query 只证明记录命中标签或说明性字段线索
- WHEN recurrence hint 被形成
- THEN backend SHALL 将其视为有限重复证据而非相关性分数、周期或因果证明
- AND SHALL NOT 把 recency 排序表述为 relevance score

### Requirement: Existing Agent API DTO And Storage Contracts Must Remain Stable

#### Scenario: API 与 DTO

- GIVEN C9 返回时间感知回复或 temporal fallback
- WHEN 构造现有 Agent response
- THEN SHALL 继续使用既有 API、status、message、messages 与 review sheet 消费方式
- AND SHALL NOT 新增 distance/pattern/score/trend 等外部字段或 endpoint

#### Scenario: 数据与依赖

- GIVEN C9 被实现
- WHEN 审查 schema 与构建文件
- THEN SHALL NOT 新增数据库表、列、索引或 DDL
- AND SHALL NOT 新增 Maven/npm dependency、package 或 lockfile 改动
- AND SHALL NOT 修改记录封存后的 location、attachments 或 cover 约束

### Requirement: Temporal Decisions Must Be Observable Without Content Leakage

#### Scenario: 结构化 trace

- GIVEN temporal policy 处理了一轮记忆上下文
- WHEN 写 C5 trace 或 C6 eval 证据
- THEN SHALL 只记录 policy version、距离桶计数、衰减前后字符数、pattern eligible/used 与违规枚举
- AND SHALL NOT 记录记录时间清单、关键词、日记、对话、片段、prompt 或 provider response

#### Scenario: policy fingerprint

- GIVEN temporal policy 的边界、比例、词表或提示约束发生变化
- WHEN 生成 trace version anchor
- THEN policy fingerprint SHALL 变化
- AND 该变化 SHALL 可与对应 OpenSpec change 与 snapshot note 对齐
