# Backend Core Spec Delta：agent-review-chat（C3 后半刀）

> 本文件是 delta。范围：会话用途的行为分支、回看配置、端点参数。

---

## MODIFIED Requirements

### 修订：C3a 条款「本刀未实现的用途」

#### Scenario: C3a 未实现的用途

> C3b 修订：C3a 原文「除写作引导以外的用途 SHALL NOT 存在任何行为分支」
> 已由 C3b 落实为回看行为，改写为 C3a 阶段范围声明。

- GIVEN 仅 C3 前半刀实现存在
- WHEN 审查后端行为
- THEN 该阶段除写作引导以外的用途 SHALL NOT 存在任何行为分支
- AND 回看用途的行为 SHALL 自 C3 后半刀起由本 spec 的回看条款约束

---

## ADDED Requirements

### Requirement: Session Purpose Must Drive Behaviour Through A Single Derived Mode

会话用途 SHALL 经由单一派生模式影响行为，SHALL NOT 在多处各自判断。

#### Scenario: 模式的派生与使用

- GIVEN 一个会话携带用途标识
- WHEN 后端需要决定记录状态要求、阶段推进方式、工具可用性或素材产出
- THEN 这些决定 SHALL 来自同一个由用途派生的模式
- AND 后端 SHALL NOT 在各处重复判断用途

#### Scenario: 未知或缺失用途

- GIVEN 会话的用途为空或无法识别
- WHEN 模式被派生
- THEN 模式 SHALL 回退为写作引导
- AND 后端 SHALL NOT 因此进入无模式状态

### Requirement: Session Start Endpoint Must Accept An Optional Purpose

开启会话的端点 SHALL 接受可选的用途参数，且 SHALL 保持向后兼容。

#### Scenario: 未指定用途

- GIVEN 请求未携带用途
- WHEN 后端开启会话
- THEN 会话 SHALL 为写作引导用途
- AND 既有客户端调用 SHALL NOT 需要修改

#### Scenario: 指定回看用途

- GIVEN 请求携带回看用途与记录标识
- WHEN 后端开启会话
- THEN 会话 SHALL 为回看用途

#### Scenario: 回看用途缺少记录标识

- GIVEN 请求携带回看用途但未指定记录
- WHEN 后端处理该请求
- THEN 后端 SHALL 拒绝该请求

#### Scenario: 会话读取、追加与结束端点

- GIVEN 回看会话已建立
- WHEN 客户端读取会话、追加消息或结束会话
- THEN 后端 SHALL 复用既有的会话端点
- AND 后端 SHALL NOT 为回看另建一套等价端点

### Requirement: Active Session Lookup Must Be Scoped By Purpose

进行中会话的查询 SHALL 按用途隔离。

#### Scenario: 同一记录上的不同用途会话

- GIVEN 同一条记录上存在不同用途的进行中会话
- WHEN 后端按用途查询进行中会话
- THEN 查询 SHALL 只返回该用途下的会话
- AND 查询 SHALL NOT 依赖记录状态互斥这一巧合来避免串会话

### Requirement: Review Chat Configuration Must Come From Backend Side Config

回看对话的参数 SHALL 来自 backend-side 配置。

#### Scenario: 配置项范围

- GIVEN 回看对话存在轮次上限与记录内容注入长度上限
- WHEN 配置被声明
- THEN 这些配置 SHALL 独立于写作引导的同类配置
- AND 配置 SHALL 位于 backend-side 配置中
- AND 配置 SHALL NOT 引入任何新的凭证字段

#### Scenario: 回看轮次上限与写作引导互不影响

- GIVEN 回看轮次上限被调整
- WHEN 写作引导会话进行
- THEN 写作引导的轮次上限 SHALL 保持不变
