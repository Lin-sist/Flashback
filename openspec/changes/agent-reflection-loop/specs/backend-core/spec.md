# Backend Core Spec Delta：agent-reflection-loop（C7）

> 闸门 1 已于 2026-08-02 批准。范围：后端调用预算、轨迹聚合与无契约扩张边界。

## ADDED Requirements

### Requirement: Reflection Must Reuse Existing Backend Contracts

#### Scenario: 外部契约

- GIVEN C7 被实现
- WHEN 审查对外接口
- THEN SHALL NOT 新增或修改 Agent API、DTO 或前端协议
- AND SHALL NOT 新增数据库表、列或 migration
- AND SHALL NOT 修改工具白名单、会话状态机或记录生命周期

#### Scenario: 配置与依赖

- GIVEN reflection 上限固定为一次
- WHEN 审查配置与构建文件
- THEN SHALL NOT 新增 secret、provider credential 或运行时开关
- AND SHALL NOT 修改 package、lockfile 或 Maven dependencies

### Requirement: Reflection Provider Budget Must Be Strictly Bounded

#### Scenario: 非 CLOSING reply 正常路径调用数

- GIVEN initial 输出通过或命中不可恢复违规
- WHEN 本轮结束
- THEN provider 调用数 SHALL NOT 超过 1

#### Scenario: 非 CLOSING reply eligible path 调用数

- GIVEN initial 输出命中允许恢复的违规
- WHEN 本轮结束
- THEN provider 调用数 SHALL NOT 超过 2
- AND 任意异常 SHALL NOT 触发第三次调用

#### Scenario: CLOSING 调用数

- GIVEN CLOSING 一轮需要生成 reply 与 material
- WHEN C7 被执行
- THEN reply SHALL NOT 发起 reflection
- AND 该轮 SHALL 保持既有最多 2 次 provider 调用

#### Scenario: 超时配置

- GIVEN C7 可能执行两次 provider 调用
- WHEN 实现该环
- THEN backend 20s 与 frontend 30s 既有超时 SHALL 保持不变
- AND 若真实 canary 证明预算不可行，系统 SHALL 回到规划而非直接放宽超时

### Requirement: Trace Provider Duration Must Represent Total Turn Cost

#### Scenario: 单次调用

- GIVEN 本轮仅调用 provider 一次
- WHEN 持久化轨迹
- THEN 顶层 provider duration SHALL 等于该次调用耗时

#### Scenario: Reflection 调用

- GIVEN 本轮调用 initial 与 reflection 两次
- WHEN 持久化轨迹
- THEN 顶层 provider duration SHALL 为两次 provider 耗时之和
- AND 每次调用的耗时与 phase SHALL 保留在结构化 steps 中

### Requirement: Reflection Trace Must Preserve One Turn One Row

#### Scenario: 同一请求内部重写

- GIVEN reflection 在一个请求内部发生
- WHEN trace sink 持久化
- THEN SHALL 只写一条 `agent_turn_trace`
- AND SHALL NOT 因 reflection 增加 `attemptNo`
- AND steps SHALL 以受控标识描述 initial/reflection/terminal

#### Scenario: 隐私边界

- GIVEN reflection 轨迹被写出或记录日志
- WHEN 检查字段与 steps
- THEN SHALL NOT 包含候选文本、用户消息、日记、记忆片段、prompt 全文或 provider response

### Requirement: Reflection Must Be Verified Against Real MySQL Semantics

#### Scenario: 本地数据库验证

- GIVEN 含 reflection 的路径已由离线测试通过
- WHEN 进入验收
- THEN SHALL 在真实 MySQL 上验证事务完成与 trace 持久化
- AND H2 结果 SHALL NOT 被表述为真实 MySQL 验证

#### Scenario: 未获外调授权

- GIVEN 闸门 3 未被批准
- WHEN 汇报验证结果
- THEN 真实 provider 与真机重写效果 SHALL 标记为 SKIPPED
- AND SHALL NOT 从 scripted provider 结果推断真实模型质量
