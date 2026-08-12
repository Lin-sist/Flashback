# Agent Runtime Spec Delta：present-moment-capture（P3.1）

> 规划草案。范围：写作引导和可逆工具的记录状态资格；Prompt、provider、memory、guardrails、reflection 与调用预算零扩张。

## MODIFIED Requirements

### Requirement: Writing Guidance Must Target Editable Records Only

写作引导对话 SHALL 只作用于用户拥有的 active DRAFT 或 SAVED 记录。SEALED / UNLOCKED SHALL NOT 进入写作引导；UNLOCKED 继续使用独立 REVIEW_CHAT。

#### Scenario: 对话关联 active DRAFT

- GIVEN 一个已登录用户拥有未过期的 DRAFT
- WHEN 该用户以该记录开启写作引导
- THEN 后端 SHALL 允许开启会话
- AND 写作引导产生的可逆修改 SHALL 保持记录为 DRAFT，直到用户显式保存

#### Scenario: 对话关联 SAVED

- GIVEN 一个已登录用户拥有 SAVED 记录
- WHEN 该用户以该记录开启写作引导
- THEN 后端 SHALL 允许开启会话
- AND 用户显式确认的正文/标签修改 SHALL 保持记录为 SAVED
- AND 修改后 SHALL 继续满足 P3.1 保存成立条件

#### Scenario: 写作引导关联已封存或已解锁记录

- GIVEN 目标记录处于 SEALED 或 UNLOCKED
- WHEN 用户尝试以该记录开启写作引导
- THEN 后端 SHALL 拒绝该操作
- AND 封存后的不可变契约 SHALL 保持不变

#### Scenario: 回看对话的记录状态要求

- GIVEN 目标记录处于 DRAFT、SAVED 或 SEALED
- WHEN 用户尝试以该记录开启 REVIEW_CHAT
- THEN 后端 SHALL 拒绝该操作
- AND 尚未解锁的记录内容 SHALL NOT 经由回看对话被提前读到

#### Scenario: 过期技术草稿

- GIVEN 目标记录处于 DRAFT 但 recovery expiry 已到
- WHEN 用户尝试开启或恢复写作引导
- THEN 后端 SHALL 拒绝或返回安全的不可恢复结果
- AND SHALL NOT 通过 Agent 旁路复活过期草稿

### Requirement: Agent Tool Execution Must Reuse Editable Record Business Paths

工具执行 SHALL 复用 RecordService 的 owner、active/editable state 与 P3.1 eligibility 权威，SHALL NOT 存在仅 Agent 可用的保存、封存或状态绕过。

#### Scenario: 目标记录仍为 active DRAFT

- GIVEN 会话绑定的记录处于未过期 DRAFT
- WHEN 用户确认执行允许的可逆写工具
- THEN 后端 SHALL 通过既有记录业务路径完成写入
- AND SHALL 刷新恢复期限
- AND SHALL NOT 自动将记录转为 SAVED

#### Scenario: 目标记录为 SAVED

- GIVEN 会话绑定的记录处于 SAVED
- WHEN 用户确认执行允许的可逆写工具
- THEN 后端 SHALL 通过既有记录业务路径完成写入
- AND 记录 SHALL 保持 SAVED
- AND 执行后 SHALL 继续满足文字或 AVAILABLE 媒体成立条件

#### Scenario: 目标记录已封存或已解锁

- GIVEN 会话绑定的记录处于 SEALED 或 UNLOCKED
- WHEN 用户确认执行写工具
- THEN 后端 SHALL 拒绝该操作并返回显式失败
- AND SHALL NOT 修改记录、位置、附件、封面或标签

#### Scenario: Agent 不拥有生命周期命令

- GIVEN P3.1 引入 save 与 SAVED -> SEALED 状态迁移
- WHEN 审查 Agent tool registry 与执行器
- THEN Agent SHALL NOT 获得 save、seal、delete、attachment、location 或 cover 工具
- AND 用户 SHALL 亲自完成保存与封存决定

## ADDED Requirements

### Requirement: P3.1 Status Expansion Must Not Change Agent Generation Semantics

#### Scenario: Provider 与 Prompt 边界

- GIVEN 写作引导现在可绑定 DRAFT 或 SAVED
- WHEN Agent 编排一轮回复
- THEN SHALL 复用既有 Prompt、阶段、memory、guardrail、reflection 与 provider contract
- AND SHALL NOT 因 P3.1 新增 provider 调用、自动 retry、分析能力或用户可见状态字段

#### Scenario: Evaluation baseline

- GIVEN P3.1 实现只改变记录状态资格
- WHEN C6 eval 与既有 Agent 回归运行
- THEN provider call count、阶段不变量、护栏与隐私规则 SHALL 保持
- AND 任何 snapshot 文本变化 SHALL 被视为意外 scope drift，除非用户另行批准独立 Agent change

#### Scenario: 用户原文与媒体隐私

- GIVEN Agent 与 DRAFT 或 SAVED 记录交互
- WHEN 日志、trace、eval 或 exception 被写入
- THEN SHALL 继续遵守既有无原文边界
- AND SHALL NOT 记录附件内容、storage key、signed URL 或 P3.1 migration/cleanup 私人数据
