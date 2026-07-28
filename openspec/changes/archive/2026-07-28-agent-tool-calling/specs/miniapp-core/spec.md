# Miniapp Core Spec Delta（C2 `agent-tool-calling`）

> 本 delta 覆盖 C2 的前端确认交互契约。
> 待规划闸批准；批准并实现验收后才接受进 `openspec/specs/miniapp-core/spec.md`。

## ADDED Requirements

### Requirement: Tool Proposal Must Be Presented As An Explicit Confirmation Affordance

前端 SHALL 在既有 Agent 对话浮层内以显式确认控件呈现工具提议，SHALL NOT 自动执行。

#### Scenario: 收到工具提议

- GIVEN 用户正在与 Agent 对话
- WHEN 后端返回一个待确认的工具提议
- THEN 前端 SHALL 在对话浮层内展示该提议与接受、拒绝两个选项
- AND 前端 SHALL NOT 在用户未选择前发起执行请求

#### Scenario: 用户接受提议

- GIVEN 一个待确认提议已展示
- WHEN 用户点击接受
- THEN 前端 SHALL 调用工具确认端点
- AND 执行成功后前端 SHALL 使编辑器中的正文与标签与后端状态保持一致

#### Scenario: 用户拒绝提议

- GIVEN 一个待确认提议已展示
- WHEN 用户点击拒绝
- THEN 前端 SHALL 通知后端该提议被拒绝
- AND 编辑器中的正文与标签 SHALL 保持不变

#### Scenario: 用户重复点击接受

- GIVEN 一次确认请求正在进行中
- WHEN 用户再次点击接受
- THEN 前端 SHALL NOT 重复发起执行请求

### Requirement: Tool Execution Failure Must Be Surfaced To The User

工具执行失败 SHALL 对用户明确可见，SHALL NOT 静默或显示为成功。

#### Scenario: 执行因记录已封存而失败

- GIVEN 目标记录在提议之后已被封存
- WHEN 用户接受该提议
- THEN 前端 SHALL 展示明确的失败原因
- AND 前端 SHALL NOT 提示操作已完成

#### Scenario: 执行因服务不可用而失败

- GIVEN 后端返回不可用或失败状态
- WHEN 前端处理该响应
- THEN 前端 SHALL 展示可读的失败提示
- AND 对话浮层 SHALL 保持可用，已产生的素材 SHALL NOT 丢失

### Requirement: Tool Confirmation Must Stay Within Existing Editor Surface And Preview Isolation

工具确认交互 SHALL 保持在既有记录编辑页对话浮层内，并遵循既有 preview 隔离约定。

#### Scenario: 确认交互的界面位置

- GIVEN 工具确认能力已上线
- WHEN 用户在记录编辑页与 Agent 对话
- THEN 确认交互 SHALL 出现在既有对话浮层内
- AND SHALL NOT 新增一级 Tab、页面路由或全局弹窗

#### Scenario: preview 会话下的工具确认

- GIVEN 当前为未认证的 preview 会话
- WHEN 触发工具确认请求
- THEN 前端 SHALL 拒绝该请求且 SHALL NOT 访问真实服务
- AND 前端 SHALL NOT 以本地伪造结果冒充真实执行成功
