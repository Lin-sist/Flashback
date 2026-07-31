# Backend Core Spec Delta：agent-eval-framework（C6）

> 本文件是 **delta 建议**，闸门 1 待批准。范围：评测资产在后端工程中的位置、离线约束、依赖边界与「不改生产代码」的硬约束。
> 评测的行为语义（不变量、快照、诚实边界）在 `agent-runtime` delta，本文件只写工程侧约束。

---

## ADDED Requirements

### Requirement: Evaluation Assets Must Live In Test Scope Only

评测资产 SHALL 完全位于后端测试范围内。

#### Scenario: 评测资产的位置

- GIVEN 评测被实现
- WHEN 审查改动范围
- THEN 评测的代码与数据 SHALL 只存在于测试源与测试资源目录
- AND 生产源码 SHALL NOT 被修改

#### Scenario: 为可测性修改生产代码

- GIVEN 评测需要驱动某条既有路径
- WHEN 该路径难以从测试装配抵达
- THEN 后端 SHALL 优先复用既有的构造注入与既有的单一落库出口
- AND 若确实必须修改生产代码，该修改 SHALL 先经用户确认，SHALL NOT 由实现方自行扩大范围

#### Scenario: 生产路径上的组件

- GIVEN 某组件在 mock provider 配置下仍运行于生产路径
- WHEN 评测需要该组件产出它当前产不出的内容
- THEN 后端 SHALL 以测试范围的替身实现该需求
- AND 该组件本身 SHALL NOT 被修改

#### Scenario: 替身的最小化

> 替身替掉的越多，被评测覆盖的生产代码就越少。
> 极端情形下断言的是替身自身的行为，评测因此失去意义。

- GIVEN 评测需要在测试范围内替换某个协作者
- WHEN 设计该替身
- THEN 替身 SHALL 只覆盖真正跨越进程边界的行为
- AND 判定逻辑、状态推进、上下文组装与检索收口 SHALL 使用生产实现

#### Scenario: 评测必须驱动真实的生成分支

> mock provider 分支在组装上下文之前即返回，因此只走该分支的评测
> 无法观测上下文组装，也无法产生任何降级轨迹。

- GIVEN 评测需要覆盖上下文组装与降级路径
- WHEN 评测驱动一轮对话
- THEN 评测 SHALL 走真实 provider 的生成分支
- AND 评测 SHALL 能证明该分支确实被走到

### Requirement: Evaluation Must Not Introduce Dependencies Or Schema Changes

评测 SHALL 在既有依赖与既有 schema 下实现。

#### Scenario: 依赖边界

- GIVEN 评测需要解析用例数据文件
- WHEN 选择解析方式
- THEN 后端 SHALL 使用测试 classpath 上已存在的能力
- AND 构建配置与依赖清单 SHALL NOT 被修改

#### Scenario: 依赖为传递引入时的失败语义

- GIVEN 评测所依赖的解析能力来自传递依赖
- WHEN 该能力不可用
- THEN 评测 SHALL 明确失败
- AND 评测 SHALL NOT 静默跳过用例

#### Scenario: 持久化边界

- GIVEN 评测断言的是每轮的决策轨迹
- WHEN 评测被执行
- THEN 评测 SHALL 断言轨迹收集器的内存状态
- AND 评测 SHALL NOT 要求新增表、新增列或修改既有 schema
- AND 轨迹落库的正确性 SHALL 仍由既有的集成测试承担

#### Scenario: 配置边界

- GIVEN 评测需要特定的限值组合
- WHEN 评测装配配置对象
- THEN 评测 SHALL 在测试范围内构造配置
- AND 生产配置文件的默认值 SHALL NOT 被修改

### Requirement: Evaluation Must Run Without External Services

评测 SHALL 可在无网络、无数据库的环境下完成。

#### Scenario: 外部调用

- GIVEN 评测被执行
- WHEN 检查外部交互
- THEN 评测 SHALL NOT 发起任何真实 AI provider 调用
- AND 评测 SHALL NOT 依赖对象存储或其他外部服务

#### Scenario: 环境门控

- GIVEN 评测离线且无外调
- WHEN 决定其执行条件
- THEN 评测 SHALL NOT 被环境变量门控为默认跳过
- AND 既有的真实 provider 探针 SHALL 保持默认跳过

#### Scenario: 执行能力的如实表述

- GIVEN 评测可由后端既有的测试命令执行
- WHEN 描述其强制力
- THEN 后端 SHALL 如实表述为「可由既有测试命令执行」
- AND 后端 SHALL NOT 表述为已具备持续集成门槛
- AND 本变更 SHALL NOT 引入持续集成配置

### Requirement: Local Real Samples Must Be Excluded From Version Control

真实样本 SHALL 不进入版本控制。

#### Scenario: 忽略规则的形态

- GIVEN 评测支持使用真实样本作为本地输入
- WHEN 配置版本控制忽略规则
- THEN 该规则 SHALL 以通配形式覆盖同类样本文件
- AND 该规则 SHALL NOT 仅点名单个文件

#### Scenario: 忽略规则的落地顺序

- GIVEN 真实样本文件尚未创建
- WHEN 实现评测
- THEN 忽略规则 SHALL 先于任何真实样本文件落地并被验证
- AND 该顺序 SHALL NOT 被颠倒

#### Scenario: 入库用例的内容

- GIVEN 用例文件将进入版本控制
- WHEN 编写这些用例
- THEN 它们 SHALL 只包含合成内容
- AND 它们 SHALL NOT 包含用户真实日记内容

### Requirement: Existing Test Assets Must Not Be Weakened

评测的引入 SHALL NOT 削弱既有测试资产。

#### Scenario: 既有断言

- GIVEN 既有后端测试已通过
- WHEN 评测被引入
- THEN 既有断言 SHALL NOT 被修改
- AND 既有测试 SHALL 保持全部通过
- AND 既有的环境门控探针 SHALL 保持默认跳过，跳过数量 SHALL NOT 增加

#### Scenario: 既有护栏用例集

- GIVEN 既有的护栏边界用例集已覆盖确定性场景
- WHEN 新增维度以外置数据文件表达
- THEN 既有用例集 SHALL 原地保留
- AND 确定性护栏用例 SHALL NOT 被迁移出既有用例集

#### Scenario: 既有隐私断言

- GIVEN 既有测试已包含一条判定指标不泄漏内容的断言
- WHEN 新增结构化的同类断言
- THEN 既有断言 SHALL 保持不变
- AND 新增断言 SHALL 以整体形状校验取代逐词列举
