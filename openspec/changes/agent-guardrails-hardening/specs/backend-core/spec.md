# Backend Core Spec Delta（C4 `agent-guardrails-hardening`）

> 承载 C4 在后端侧的实现约束：检查层位置、配置边界、依赖边界与回归底线。
> Agent 行为主契约见本 change 的 `specs/agent-runtime/spec.md`。
> 待规划闸批准；批准并实现验收后才接受进 `openspec/specs/backend-core/spec.md`。

## ADDED Requirements

### Requirement: Guardrail Checks Must Run Backend Side Without External Calls

护栏检查 SHALL 完全在后端本地完成，SHALL NOT 依赖任何外部服务调用。

#### Scenario: 忠实度判定的执行位置

- GIVEN 后端需要判定候选文本是否忠实于用户表达
- WHEN 判定被执行
- THEN 判定 SHALL 在后端进程内完成
- AND 判定 SHALL NOT 发起对 AI provider 或其他外部服务的调用

#### Scenario: 护栏检查的依赖边界

- GIVEN 护栏检查已实现
- WHEN 审查依赖清单
- THEN 检查 SHALL NOT 引入新的第三方分词或文本相似度依赖
- AND 依赖声明文件 SHALL 保持不变

#### Scenario: 前端不承担护栏判定

- GIVEN 护栏判定影响是否向用户呈现内容
- WHEN 审查判定实现位置
- THEN 判定 SHALL NOT 由前端执行
- AND 后端 SHALL NOT 依据客户端提交的判定结论放行内容

### Requirement: Guardrail Rejection Must Reuse Existing Tool Audit Channel

工具路径的护栏拒绝 SHALL 复用既有的提议审计通道，SHALL NOT 新增持久化结构。

#### Scenario: 不忠实提议的落痕

- GIVEN 一条工具提议因内容不忠实被拒绝
- WHEN 后端记录该次拒绝
- THEN 后端 SHALL 使用既有的提议审计记录与其守卫拒绝状态
- AND 后端 SHALL NOT 为此新增数据表

#### Scenario: 拒绝原因的可区分性

- GIVEN 提议可能因白名单、参数边界或内容不忠实被拒绝
- WHEN 审计记录被写出
- THEN 拒绝原因 SHALL 可区分这几类情形
- AND 拒绝原因 SHALL 以结构化常量表达，而非自由文本描述

#### Scenario: 审计内容的隐私边界

- GIVEN 护栏拒绝被审计
- WHEN 审计记录被写出
- THEN 记录 SHALL 只包含结构化摘要与判定指标
- AND 记录 SHALL NOT 包含候选文本原文或用户表达原文

### Requirement: Guardrail Configuration Must Be Backend Side And Credential Free

护栏阈值与开关 SHALL 只来自 backend-side 配置，且 SHALL NOT 引入凭证字段。

#### Scenario: 阈值配置

- GIVEN 忠实度判定存在覆盖比例、连续未覆盖片段长度与最短受检长度等阈值
- WHEN 这些阈值被读取
- THEN 阈值 SHALL 来自后端应用配置
- AND 阈值 SHALL 具备可用的默认值

#### Scenario: 护栏开关关闭时的行为

- GIVEN 忠实度判定被配置关闭
- WHEN 后端处理候选文本
- THEN 后端 SHALL 记录结构化日志说明该判定未生效
- AND 后端 SHALL NOT 静默地表现为判定已通过

#### Scenario: 配置项与凭证

- GIVEN 护栏配置项被新增
- WHEN 审查配置结构
- THEN 配置 SHALL NOT 包含 API key、token 或任何 provider 凭证
- AND provider 凭证 SHALL 仍只来自既有的 AI 配置来源

### Requirement: Guardrail Introduction Must Not Regress Existing Agent Contracts

引入护栏层 SHALL NOT 改变既有 Agent 契约与既有 AI 链路的行为。

#### Scenario: 既有 Agent 端点契约

- GIVEN 护栏层已引入
- WHEN 审查 Agent 会话与工具确认端点
- THEN 既有端点的字段语义 SHALL 保持不变
- AND 任何字段新增 SHALL 向后兼容

#### Scenario: 既有单轮 AI 链路

- GIVEN 后端存在与 Agent 对话无关的单轮 AI 端点
- WHEN 护栏层被引入
- THEN 这些端点的请求与响应链路 SHALL 保持不变

#### Scenario: 阶段与轮次语义

- GIVEN 某轮产出因护栏被拒绝或降级
- WHEN 后端处理该轮
- THEN 会话阶段推进与轮次计数语义 SHALL 与既有契约保持一致
- AND 失败轮重试语义 SHALL NOT 被改变

#### Scenario: 记录生命周期不依赖护栏

- GIVEN 护栏检查不可用或判定失败
- WHEN 用户保存草稿或封存记录
- THEN 记录保存与封存 SHALL 正常完成
- AND 护栏可用性 SHALL NOT 成为记录生命周期的依赖
