# Backend Core Spec Delta（C2 `agent-tool-calling`）

> 本 delta 只在 `backend-core` 留最小可检索条款；Agent 工具完整契约见 `specs/agent-runtime/spec.md`。
> 待规划闸批准；批准并实现验收后才接受进 `openspec/specs/backend-core/spec.md`。

## ADDED Requirements

### Requirement: Agent Tool Confirmation Endpoint Must Be Authenticated And Owner Scoped

后端 SHALL 在 `/api/agent/**` 下提供工具提议确认端点，纳入既有 `/api/**` 鉴权链路，并使用既有统一响应与错误码体系。

#### Scenario: 已登录用户确认工具提议

- GIVEN 一个携带有效凭证的用户拥有目标会话与目标提议
- WHEN 该用户确认或拒绝该提议
- THEN 后端 SHALL 校验会话与提议均归属于该用户
- AND 响应 SHALL 使用既有统一响应包装

#### Scenario: 未登录或跨用户确认工具提议

- GIVEN 请求没有有效凭证，或目标提议属于其他用户
- WHEN 访问工具确认端点
- THEN 后端 SHALL 返回未授权、拒绝或安全的未找到响应
- AND 后端 SHALL NOT 泄露其他用户的提议内容

### Requirement: Agent Tool Calling Must Not Change Existing Endpoint Contracts

引入工具调用 SHALL NOT 改变既有端点的对外契约。

#### Scenario: 既有 Agent 对话端点被调用

- GIVEN 工具调用能力已上线
- WHEN 客户端调用既有的开启会话、读取会话、追加消息或结束会话端点
- THEN 既有请求字段与响应字段语义 SHALL 保持不变
- AND 新增的工具相关字段 SHALL 为向后兼容的追加

#### Scenario: 既有单轮 AI 端点被调用

- GIVEN 工具调用能力已上线
- WHEN 客户端调用既有写作提示、记录整理或阶段总结端点
- THEN 请求与响应契约 SHALL 保持不变

#### Scenario: 既有记录端点被调用

- GIVEN 为工具执行新增了记录侧业务方法
- WHEN 客户端调用既有记录创建、更新、封存、位置、封面、附件端点
- THEN 这些端点的对外行为 SHALL 保持不变

### Requirement: Agent Tool Writes Must Reuse Record Business Layer

Agent 触发的记录写入 SHALL 经由记录业务层完成，SHALL NOT 直接绕过其归属与状态校验。

#### Scenario: 工具写入通过业务层

- GIVEN 一次已确认的工具执行需要修改草稿记录
- WHEN 后端执行该写入
- THEN 写入 SHALL 经由记录业务层方法完成
- AND 归属校验与草稿状态校验 SHALL 生效

#### Scenario: 审查数据访问路径

- GIVEN 工具执行层实现完成
- WHEN 审查其数据访问路径
- THEN SHALL NOT 存在跳过记录业务层校验的 Agent 专用写入路径

### Requirement: Provider Function Calling Configuration Must Be Backend Side And Explicit

工具调用相关的 provider 配置 SHALL 只存在于 backend-side 配置，并对能力可用性做显式判定。

#### Scenario: 工具调用可用性配置

- GIVEN 后端需要判断当前 provider 与模型是否支持工具调用
- WHEN 后端读取配置
- THEN 判定 SHALL 基于显式配置的受支持模型范围
- AND 后端 SHALL NOT 假设任意兼容 provider 或任意模型都支持工具调用

#### Scenario: 严格模式的独立地址配置

- GIVEN 严格模式需要不同于默认的服务地址
- WHEN 后端启用严格模式
- THEN 该地址 SHALL 来自独立的 backend-side 配置项
- AND 启用严格模式但地址缺失时 后端 SHALL 视为配置错误而非静默降级

#### Scenario: 凭证边界

- GIVEN 工具调用相关配置存在
- WHEN 检查前端代码与 tracked files
- THEN provider 凭证 SHALL NOT 出现在其中
- AND 新增配置项 SHALL NOT 引入新的凭证字段

### Requirement: Agent Tool Call Persistence Must Follow Owner Scoped Business Storage

工具提议与执行记录 SHALL 作为归属用户的业务数据持久化。

#### Scenario: 工具调用数据被持久化

- GIVEN 用户会话中产生工具提议
- WHEN 后端持久化提议与执行状态
- THEN 每条记录 SHALL 携带所属用户标识
- AND 时间语义 SHALL 使用既有业务时区约定
- AND 用户被删除时其工具调用记录 SHALL 被级联清理

#### Scenario: 工具调用数据出现在日志中

- GIVEN 后端记录工具相关日志
- WHEN 日志被写出
- THEN 日志 SHALL NOT 包含对话原文或用户日记原文
