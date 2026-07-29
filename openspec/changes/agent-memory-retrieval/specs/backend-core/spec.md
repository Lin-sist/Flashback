# Backend Core Spec Delta：agent-memory-retrieval（C3 前半刀）

> 本文件是 delta。范围：记忆检索的后端实现约束、配置项、持久化与隔离。

---

## ADDED Requirements

### Requirement: Memory Retrieval Must Be Implemented Behind A Replaceable Port

记忆检索 SHALL 以抽象接口暴露给 Agent Runtime，实现细节 SHALL NOT 泄漏到调用方。

#### Scenario: 调用方依赖

- GIVEN Agent Runtime 需要历史记录片段
- WHEN Runtime 获取片段
- THEN Runtime SHALL 只依赖记忆检索接口
- AND Runtime SHALL NOT 直接依赖检索的持久化实现

#### Scenario: 接口的用途维度

- GIVEN 后续场景也需要消费记忆
- WHEN 检索接口被调用
- THEN 接口 SHALL 接受会话用途作为入参
- AND 后续场景 SHALL 复用同一接口而非另建检索实现

#### Scenario: 片段的结构

- GIVEN 检索返回结果
- WHEN 结果被传递给上下文组装
- THEN 每个片段 SHALL 携带记录标识、发生时间与可读的时间标签
- AND 每个片段的文本长度 SHALL 受配置上限约束

### Requirement: Memory Retrieval Must Not Introduce Full Text Indexes Or External Engines

记忆检索 SHALL 基于既有关系型存储与既有索引结构实现。

#### Scenario: 索引与依赖边界

- GIVEN 记忆检索实现完成
- WHEN 审查数据库结构与项目依赖
- THEN 数据库 SHALL NOT 新增全文索引
- AND 数据库 SHALL NOT 引入分词器配置
- AND 项目 SHALL NOT 新增用于检索、分词或相似度计算的第三方依赖

#### Scenario: 检索谓词的字段范围

- GIVEN 检索查询被构造
- WHEN 查询条件被检查
- THEN 查询 SHALL 包含用户标识谓词
- AND 查询 SHALL NOT 包含记录正文的匹配谓词
- AND 查询结果条数 SHALL 有上限

#### Scenario: 检索排除当前会话绑定的记录

- GIVEN 会话已绑定某条草稿记录
- WHEN 后端为该会话检索历史记录
- THEN 该记录本身 SHALL NOT 出现在检索结果中

### Requirement: Memory Configuration Must Come From Backend Side Config Without New Credentials

记忆能力的开关与阈值 SHALL 来自 backend-side 配置。

#### Scenario: 配置项范围

- GIVEN 记忆能力存在开关、片段条数上限、单片段长度上限、时间范围与时间归属阈值
- WHEN 配置被声明
- THEN 这些配置 SHALL 位于 backend-side 配置中
- AND 配置 SHALL NOT 引入任何新的凭证字段

#### Scenario: 开关关闭时的可见痕迹

- GIVEN 记忆能力开关被关闭
- WHEN 后端处理一轮对话
- THEN 后端 SHALL 记录结构化痕迹说明记忆未生效
- AND 后端 SHALL NOT 静默表现为检索无命中

### Requirement: Agent Session Must Declare Its Purpose

Agent 会话 SHALL 携带用途标识，以便不同场景复用同一会话模型。

#### Scenario: 既有会话的用途

- GIVEN 写作引导会话被创建
- WHEN 会话被持久化
- THEN 会话 SHALL 标记为写作引导用途

#### Scenario: 用途标识的向后兼容

- GIVEN 存在本次变更之前创建的会话
- WHEN 后端读取这些会话
- THEN 后端 SHALL 将其视为写作引导用途
- AND 既有会话的读写行为 SHALL 保持不变

#### Scenario: 本刀未实现的用途

- GIVEN 用途标识已引入
- WHEN 审查后端行为
- THEN 除写作引导以外的用途 SHALL NOT 存在任何行为分支
