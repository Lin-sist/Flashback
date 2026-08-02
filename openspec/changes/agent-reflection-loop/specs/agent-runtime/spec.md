# Agent Runtime Spec Delta：agent-reflection-loop（C7）

> 闸门 1 已于 2026-08-02 批准。范围：确定性护栏判定后的受控一次重写环。

## MODIFIED Requirements

### 修订：`Evaluation Must Exclude Judging Reflection Resilience And Temporal Intelligence` 的输出行为 scenario

#### Scenario: C6 范围内的输出行为

> C7 修订：原文“重写能力 SHALL 留给后续独立 change”由 C7 落实；保留 C6 阶段范围事实。

- GIVEN C6 已建立回归基线
- WHEN 审查 C6 自身交付的 Agent 输出行为
- THEN C6 SHALL NOT 引入不合格输出的重写环
- AND 受控重写能力 SHALL 自 C7 起由本 spec 的 reflection 条款约束

## ADDED Requirements

### Requirement: Reflection Must Be A Bounded Backend Controlled Loop

Agent reflection SHALL 是后端控制的单次环，模型不得决定是否继续或继续次数。

#### Scenario: 首次生成通过

- GIVEN initial provider 输出通过当前路径的全部护栏
- WHEN 本轮完成
- THEN 系统 SHALL 直接返回该输出
- AND SHALL NOT 发起 reflection 调用

#### Scenario: 允许恢复的违规

- GIVEN 非 `CLOSING` reply 命中 `MISSING_TIME_ATTRIBUTION`
- WHEN 系统判断该违规允许恢复
- THEN 系统 MAY 发起一次且仅一次 reflection 调用
- AND 最大次数 SHALL 由后端常量约束为 1

#### Scenario: 重写仍不合格

- GIVEN reflection 输出仍未通过完整护栏
- WHEN 系统决定终态
- THEN 系统 SHALL 停止继续调用 provider
- AND reply SHALL 使用既有本地安全兜底

### Requirement: Reflection Eligibility Must Be Path Specific And Closed

允许 reflection 的违规集合 SHALL 是封闭且按路径声明的，不得通过默认分支扩张。

#### Scenario: Reply eligible set

- GIVEN 一条非 `CLOSING` reply 被检查
- WHEN 其违反时间归属要求
- THEN `MISSING_TIME_ATTRIBUTION` MAY 触发一次 reflection
- AND reply SHALL NOT 因 C7 新增全量忠实度检查

#### Scenario: CLOSING 与 material 不开环

- GIVEN `CLOSING` reply 后仍需生成 material，或 material 命中 `UNFAITHFUL`
- WHEN 系统处置该输出
- THEN SHALL NOT 发起 reflection
- AND material SHALL 继续使用既有直接丢弃语义
- AND 单轮 provider 调用预算 SHALL NOT 因 C7 超过 2

#### Scenario: 不可恢复违规

- GIVEN 任一路径命中 `FABRICATED_QUOTE`、`DIAGNOSTIC`、`FAKE_ACTION` 或 `CHECK_ERROR`
- WHEN 系统处置该违规
- THEN SHALL NOT 发起 reflection
- AND SHALL 立即沿用既有 fail-closed 终态

#### Scenario: Tool proposal 不开环

- GIVEN 工具提议因 `UNFAITHFUL` 或其他护栏原因被拒绝
- WHEN 本轮继续处理
- THEN SHALL NOT 为该提议发起 reflection
- AND SHALL 复用既有拒绝与二段确认边界

### Requirement: Reflection Instructions Must Be Typed And Content Free

reflection 指令 SHALL 仅由违规类型映射到固定要求，不得携带待重写文本片段。

#### Scenario: 指令生成

- GIVEN 一个允许恢复的 violation enum
- WHEN 系统构造 reflection 指令
- THEN 指令 SHALL 来自固定后端映射
- AND 映射输入 SHALL NOT 包含候选文本、用户原话、记忆片段、未覆盖片段或 prompt 全文

#### Scenario: 未知或不可恢复类型

- GIVEN violation 不在封闭 eligible 集合中
- WHEN 系统查询重写要求
- THEN SHALL 返回不可重写
- AND SHALL NOT 存在“默认全部重试”的分支

### Requirement: Reflection Must Preserve Existing Guardrail Authority

重写后 SHALL 重新运行当前路径的全部确定性护栏，reflection 不得替代或绕过 checker。

#### Scenario: Reply re-check

- GIVEN reply 已完成 reflection
- WHEN 判定能否展示
- THEN SHALL 重新运行 content 与 time attribution checks
- AND SHALL 继续执行既有长度硬上限

#### Scenario: 护栏边界不被放宽

- GIVEN C7 引入 reflection
- WHEN 审查 C4 契约
- THEN 护栏阈值、词表、来源集合与 fail-closed 语义 SHALL 保持不变

### Requirement: Reflection Must Not Become Error Retry Or Tool Loop

#### Scenario: Provider failure

- GIVEN initial 或 reflection provider 调用失败或返回无效内容
- WHEN 系统处置
- THEN SHALL NOT 因 C7 再次调用 provider
- AND provider 错误分类与一般重试 SHALL 留给 C8

#### Scenario: Reflection call tools

- GIVEN reply 进入 reflection
- WHEN 第二次 provider 调用被构造
- THEN tools SHALL 为空
- AND strict tool calling SHALL 关闭
- AND SHALL NOT 产生第二组工具提议

#### Scenario: 最终本地兜底

- GIVEN reply 最终采用本地安全兜底
- WHEN 处理 initial tool calls
- THEN initial tool calls SHALL 被丢弃
- AND 本地兜底 SHALL NOT 与来自不合格 generation 的提议共同展示

### Requirement: Reflection Must Be Observable Without Becoming A New Attempt

#### Scenario: Attempt identity

- GIVEN 一次业务请求内部发生 reflection
- WHEN 轨迹被持久化
- THEN SHALL 只有一条 turn trace
- AND `turnNo` 与 `attemptNo` SHALL 保持该请求的既有值
- AND reflection SHALL NOT 调用下一 attempt 分配逻辑

#### Scenario: Phase visibility

- GIVEN initial 与 reflection 均调用 provider
- WHEN 开发者读取结构化轨迹
- THEN provider steps SHALL 区分 `initial` 与 `reflection`
- AND SHALL 能区分重写成功、最终兜底、最终丢弃与 provider failure
- AND 轨迹 SHALL NOT 包含任何候选文本

#### Scenario: Outcome semantics

- GIVEN reflection 后输出通过护栏
- WHEN 汇总本轮 outcome
- THEN outcome SHALL 为成功而非降级
- AND reflection 发生事实 SHALL 由 steps 表达

### Requirement: Reflection Must Remain Comparable Through C6 Evaluation

#### Scenario: 回归执行

- GIVEN C7 改变编排行为
- WHEN 后端测试运行
- THEN C6 全部硬不变量 SHALL 继续执行
- AND 既有断言 SHALL NOT 被削弱或删除

#### Scenario: 预期快照变化

- GIVEN 某快照因 C7 合法改变
- WHEN 人工接受该变化
- THEN `baselineNote` SHALL 同步写明 C7 与变化原因
- AND checksum SHALL 按 C6 机制同步更新
- AND 系统 SHALL NOT 提供自动刷新入口
