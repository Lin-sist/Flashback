# Agent Collaboration Spec Delta：agent-temporal-intelligence（C9）

> 规划草案。范围：固定时钟证据、时间策略隐私、C6 baseline 纪律与预算化真实探针。

## ADDED Requirements

### Requirement: Temporal Evidence Must Be Reproducible And Content Free

#### Scenario: 固定时钟验证

- GIVEN C9 测试距离边界、衰减或 recurrence eligibility
- WHEN Agent 运行离线验证
- THEN SHALL 使用 fixed/fake `Clock` 与固定合成记录
- AND SHALL 明确覆盖 30/31、180/181 天边界、null/future 时间与 90 天跨度边界
- AND SHALL NOT 依赖测试运行当天的真实日期

#### Scenario: 证据记录

- GIVEN temporal policy 结果被写入 trace、AGENT_LOG、测试报告或 closeout
- WHEN 形成证据
- THEN SHALL 只记录策略版本、距离桶、字符数、eligible/used、违规枚举与调用次数
- AND SHALL NOT 记录用户日记、对话、关键词、片段、prompt、provider response 或精确记录时间清单

### Requirement: Temporal Changes Must Extend C6 Without Rubber Stamping

#### Scenario: 新增 C9 用例

- GIVEN C9 改变 prompt、guardrail 或 trace shape
- WHEN 扩展 C6 eval
- THEN SHALL 新增时间距离、衰减、recurrence 正反例、overreach、无额外调用与隐私用例
- AND 既有非 C9 行为用例 SHALL 保持

#### Scenario: snapshot 变化

- GIVEN 某条既有 snapshot 因 C9 合法行为发生变化
- WHEN 接受差异
- THEN SHALL 逐条人工确认并同步写明 C9 原因的 `baselineNote` 与 checksum
- AND SHALL NOT 自动重写、批量刷新、削弱不变量或修改既有断言换绿

### Requirement: Real Temporal Probes Require A Separate Synthetic Data Gate

#### Scenario: 未授权

- GIVEN 闸门 2 已允许实现但闸门 3 未批准
- WHEN 验证 C9
- THEN SHALL 只运行离线/fake-clock 测试与不产生外调的本地验证
- AND SHALL NOT 调用真实 provider、连接真实 MySQL 探针或进行真机验收

#### Scenario: 授权后的 provider 预算

- GIVEN 用户单独批准真实 provider 验收
- WHEN 执行 C9 探针
- THEN provider 总调用 SHALL 不超过 6
- AND SHALL 只使用固定合成短文本，覆盖距离、回看锚点、recurrence 与不足证据反例
- AND SHALL NOT 发送真实日记、真实回看记录或未脱敏用户数据

#### Scenario: 授权后的 MySQL 与真机

- GIVEN 用户单独批准真实 MySQL / 真机验收
- WHEN 执行验证
- THEN MySQL SHALL 使用可清理的合成用户与不同年龄记录
- AND 真机 SHALL 只验证现有表面的时间话术、长度与无分析 UI
- AND 无可控环境时 SHALL 记录 `SKIPPED + 原因`，不得以 H2/build/scripted provider 冒充

### Requirement: Temporal Findings Must Not Expand Into Analytics Or Retrieval Rewrite

#### Scenario: 发现相关性或产品分析需求

- GIVEN C9 观察到向量检索、相关性打分、统计周期、dashboard、提醒或长期画像需求
- WHEN 决定是否实现
- THEN SHALL 记录为 residual 或另起独立 OpenSpec change
- AND SHALL NOT 在 C9 顺手修改 memory SQL、引入分析存储、外部引擎、C10 语气标定或 C11 上下文架构
