# Backend Core Spec Delta：agent-observability（C5）

> 本文件是 delta。范围：决策轨迹的持久化、查询、配置与清理契约。
> 本刀**不新增任何 HTTP 端点**（决策 4），故无端点契约条款。
> 既有 Agent 端点、DTO、失败重试语义均不变。

---

## ADDED Requirements

### Requirement: Agent Turn Trace Must Be Persisted With Owner Scope And Cascade Cleanup

Agent 决策轨迹 SHALL 按用户与会话归属持久化，并随其宿主级联清理。

#### Scenario: 轨迹落库

- GIVEN 一轮 Agent 对话完成或提前返回
- WHEN 后端写出该轮轨迹
- THEN 轨迹 SHALL 记录所属用户、所属会话、轮次与尝试序号
- AND 同一轮的多次尝试 SHALL 可通过尝试序号区分

#### Scenario: 会话被删除

- GIVEN 某会话已产生轨迹
- WHEN 该会话被删除
- THEN 其轨迹 SHALL 被级联清理

#### Scenario: 用户被删除

- GIVEN 某用户已产生轨迹
- WHEN 该用户被删除
- THEN 其轨迹 SHALL 被级联清理

#### Scenario: 轨迹存储不是日记原文的授权存储

- GIVEN 用户日记原文只允许存在于被授权的业务存储
- WHEN 轨迹被写出
- THEN 轨迹存储 SHALL NOT 成为日记原文或对话原文的副本位置

### Requirement: Agent Turn Trace Must Not Share The Message Uniqueness Constraint

轨迹 SHALL 独立于对话消息存储，SHALL NOT 依赖或改动消息表的唯一性约束。

#### Scenario: 一轮多条痕迹

- GIVEN 一轮对话包含多个决策步骤
- WHEN 这些步骤的痕迹被写出
- THEN 痕迹 SHALL NOT 受「同会话同轮次同角色只允许一条」的约束限制

#### Scenario: 既有幂等约束不变

- GIVEN 对话消息表的唯一性约束是失败重试幂等的实现基石
- WHEN 引入轨迹存储
- THEN 该唯一性约束 SHALL 保持不变

### Requirement: Agent Turn Trace Must Be Queryable By Session For Developers

轨迹 SHALL 提供按会话取回的查询能力，且 SHALL NOT 经由产品接口暴露。

#### Scenario: 按会话取回

- GIVEN 某会话已产生多轮轨迹
- WHEN 按会话标识查询轨迹
- THEN 后端 SHALL 返回该会话按轮次与尝试序号有序的轨迹

#### Scenario: 产品接口的边界

- GIVEN 轨迹面向开发者排查
- WHEN 产品接口返回会话数据
- THEN 响应 SHALL NOT 包含轨迹数据

#### Scenario: 未新增对外端点

- GIVEN 轨迹已可查询
- WHEN 审查后端对外端点
- THEN C5 SHALL NOT 新增面向终端用户的轨迹端点
- AND C5 SHALL NOT 修改既有认证与令牌签发逻辑

### Requirement: Agent Turn Trace Content Must Be Structured And Non Reversible

轨迹字段 SHALL 只承载结构化标识、数值与不可还原的摘要。

#### Scenario: 轨迹字段的取值范围

- GIVEN 轨迹被写出
- WHEN 检查其字段取值
- THEN 取值 SHALL 限于结构化枚举标识、数值指标、长度与哈希前缀
- AND 取值 SHALL NOT 包含自由文本形式的用户表达

#### Scenario: 拒绝原因与违规类型的表达

- GIVEN 某轮发生护栏拒绝或降级
- WHEN 轨迹记录其原因
- THEN 原因 SHALL 以既有的结构化常量表达，而非自由文本描述

#### Scenario: 轨迹相关日志

- GIVEN 后端记录轨迹写入相关的运行日志
- WHEN 日志被写出
- THEN 日志 SHALL 只包含结构化元数据
- AND 日志 SHALL NOT 包含对话原文或用户日记原文

### Requirement: Guardrail Downgrade Traces Must Be Correlatable To Session And Turn

护栏降级与判定异常的痕迹 SHALL 可关联到具体会话与轮次。

#### Scenario: 回复路径的降级痕迹

- GIVEN 某轮回复在可观测启用时被护栏降级
- WHEN 该次降级被留痕
- THEN 痕迹 SHALL 携带该轮的会话标识与轮次
- AND 痕迹 SHALL NOT 以空值表达这两项

#### Scenario: 素材路径的降级痕迹

- GIVEN 某次素材产出在可观测启用时被护栏丢弃
- WHEN 该次丢弃被留痕
- THEN 痕迹 SHALL 携带该轮的会话标识与轮次

#### Scenario: 判定自身异常的痕迹

- GIVEN 某层护栏判定过程自身发生异常并按 fail-closed 处理
- WHEN 该情形被留痕
- THEN 轨迹 SHALL 记录该判定结论及其所属闸层
- AND 该记录 SHALL 可关联到发生该情形的会话与轮次

### Requirement: Observability Configuration Must Live In Backend Side Config

可观测能力的开关与保留策略 SHALL 由 backend-side 配置约束。

#### Scenario: 配置项归属

- GIVEN 可观测能力需要开关与保留期
- WHEN 这些配置被声明
- THEN 配置 SHALL 位于 backend-side 配置中
- AND 配置 SHALL NOT 引入新的凭证字段
- AND 配置 SHALL NOT 出现在前端代码或 tracked files 中的明文 secret

#### Scenario: 关闭时的行为

- GIVEN 可观测能力被配置关闭
- WHEN 后端处理一轮对话
- THEN 后端 SHALL 记录结构化痕迹说明该能力未生效
- AND 后端 SHALL NOT 静默表现为轨迹无数据

#### Scenario: 保留期与清理

- GIVEN 轨迹随时间累积
- WHEN 需要控制存储规模
- THEN 后端 SHALL 提供可配置的保留期与可执行的清理手段
- AND C5 SHALL NOT 引入自动调度任务执行清理

### Requirement: Trace Recording Must Not Alter Existing Agent Behavior

引入轨迹 SHALL NOT 改变任何既有 Agent 行为契约。

#### Scenario: 既有对话契约

- GIVEN 轨迹已接入对话链路
- WHEN 审查既有行为
- THEN 阶段推进、上下文组装、护栏阈值、记忆检索、工具白名单与回看逻辑 SHALL 保持不变

#### Scenario: 既有失败语义

- GIVEN provider 调用失败
- WHEN 该轮返回
- THEN 用户消息 SHALL 仍被保留
- AND Agent 回复 SHALL 仍不落库
- AND 同轮重试 SHALL 仍不重复计数
