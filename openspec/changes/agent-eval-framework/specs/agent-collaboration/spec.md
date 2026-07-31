# Agent Collaboration Spec Delta：agent-eval-framework（C6）

> 本文件是 **delta 建议**，闸门 1 待批准。范围：评测与样本的协作规范——真实样本的处置、基线更新的协作约束、以及「评测结果不得成为顺手改行为的依据」。
> 落点依据：蓝图 v1.2 §5 的 C6 行（`agent-collaboration`：评估与样本隐私规范）。
> 该 spec 自 C5 起承载产品 Agent 条款，本 delta 延续同一段落风格。

---

## ADDED Requirements

### Requirement: Evaluation Samples Must Not Enter Collaboration Artifacts

评测用到的真实样本 SHALL NOT 进入任何协作产物。

#### Scenario: 真实样本的处置

- GIVEN 某次评测使用了用户真实日记作为本地输入
- WHEN 记录执行证据
- THEN 执行日志 SHALL 只记录用例标识、维度与判定结论
- AND 执行日志 SHALL NOT 包含样本内容

#### Scenario: 基线与报告作为协作产物

- GIVEN 基线文件与失败报告会被审查
- WHEN 它们被写出
- THEN 它们 SHALL 只包含结构化标识与数值指标
- AND 它们 SHALL NOT 包含用例输入文本

#### Scenario: 对外叙事中的评测内容

- GIVEN 评测能力会被写入对外叙事文档
- WHEN 描述评测
- THEN 描述 SHALL 只使用维度名称、机制与结论
- AND 描述 SHALL NOT 包含真实样本、secret 或本机绝对路径

### Requirement: Baseline Updates Must Be Deliberate And Attributed

基线的更新 SHALL 是一个有归属、可审查的动作。

#### Scenario: 更新基线时的协作义务

- GIVEN 某次改动导致基线指标变化
- WHEN 更新基线
- THEN 更新方 SHALL 在基线说明中记录本次更新归属于哪一个 change
- AND 更新方 SHALL 在执行证据中说明该变化为预期

#### Scenario: 不得静默刷新

- GIVEN 评测因基线不符而失败
- WHEN 处理该失败
- THEN 更新方 SHALL NOT 在未说明原因的情况下把基线改成当前值
- AND 更新方 SHALL NOT 通过绕过或禁用评测使其通过

#### Scenario: 不变量失败的处理义务

- GIVEN 某条不变量断言失败
- WHEN 处理该失败
- THEN 处理方 SHALL 将其视为缺陷并定位原因
- AND 处理方 SHALL NOT 通过放宽该不变量使其通过

### Requirement: Evaluation Findings Must Not Trigger Unauthorized Behavior Changes

评测揭示的问题 SHALL NOT 成为在同一刀内顺手修改 Agent 行为的依据。

#### Scenario: 评测发现缺陷时的范围约束

- GIVEN 建立评测的过程中发现了某个编排缺陷
- WHEN 决定如何处置
- THEN 处置 SHALL 限于如实记录该发现
- AND 修复 SHALL 由独立变更承担，除非用户明确纳入当前变更

#### Scenario: 阈值校准的授权边界

- GIVEN 评测使某个未校准的阈值第一次具备校准条件
- WHEN 决定是否校准
- THEN 建立评测的变更 SHALL NOT 同时校准该阈值
- AND 校准 SHALL 作为独立事项交由用户决定

#### Scenario: 评测能力的如实描述

- GIVEN 评测运行在替身与 mock 路径上
- WHEN 向用户或对外描述其覆盖范围
- THEN 描述 SHALL 明确它覆盖编排逻辑而非模型语言质量
- AND 未验证项与未填充项 SHALL 被如实标注
- AND 描述 SHALL NOT 让读者以为修改提示词已因此变得安全
