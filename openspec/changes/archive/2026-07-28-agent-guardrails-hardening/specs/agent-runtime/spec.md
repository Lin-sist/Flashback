# Agent Runtime Spec Delta（C4 `agent-guardrails-hardening`）

> 承载 C4 的系统化护栏主契约。C1 / C2 已接受条款除本文 MODIFIED 段落外全部保持不变。
> 待规划闸批准；批准并实现验收后才接受进 `openspec/specs/agent-runtime/spec.md`。
> 术语：**候选文本**指模型产出且会进入用户记录正文的文本（工具正文参数、素材草稿）；
> **来源集合**指当前会话中该用户自己发出的全部消息。

## ADDED Requirements

### Requirement: Model Produced Text Entering User Content Must Pass A Deterministic Faithfulness Check

任何将进入用户记录正文的模型产出文本 SHALL 先通过确定性的来源忠实度判定；判定 SHALL NOT 依赖再一次模型调用。

#### Scenario: 候选文本增写了用户没有表达过的内容

- GIVEN 一个 `ACTIVE` 会话已绑定可编辑草稿记录
- WHEN 模型产出的候选文本中包含来源集合里没有对应来源的连续内容
- THEN 后端 SHALL 判定该候选文本不忠实
- AND 该候选文本 SHALL NOT 进入用户记录正文
- AND 该候选文本 SHALL NOT 作为待确认提议下发给前端

#### Scenario: 候选文本只是整理了用户已表达的内容

- GIVEN 候选文本对用户已表达内容做了语序调整、口头语删减或标点变化
- WHEN 后端执行忠实度判定
- THEN 后端 SHALL 判定该候选文本忠实
- AND 该候选文本 SHALL 正常进入既有的用户确认流程

#### Scenario: 判定依据

- GIVEN 后端执行忠实度判定
- WHEN 判定结果被计算
- THEN 判定 SHALL 同时考量候选文本被来源集合覆盖的整体比例
- AND 判定 SHALL 同时考量候选文本中最长的连续未被覆盖片段
- AND 单一的整体比例 SHALL NOT 作为唯一判据

#### Scenario: 判定的可复现性

- GIVEN 同一候选文本与同一来源集合
- WHEN 忠实度判定被重复执行
- THEN 判定结果 SHALL 保持一致
- AND 判定 SHALL NOT 产生对外部服务的调用

#### Scenario: 来源集合的边界

- GIVEN 后端构造忠实度判定的来源集合
- WHEN 来源集合被组装
- THEN 来源集合 SHALL 只包含当前会话中该用户发出的消息
- AND 来源集合 SHALL NOT 包含跨记录的历史检索结果
- AND 来源集合 SHALL NOT 包含 Agent 自己产出的表达

#### Scenario: 判定过程本身失败

- GIVEN 忠实度判定过程发生异常
- WHEN 后端处理该候选文本
- THEN 后端 SHALL 按不忠实处理
- AND 后端 SHALL NOT 因判定失败而放行未经判定的候选文本

### Requirement: Unfaithful Tool Proposals Must Be Rejected Without Breaking The Turn

不忠实的工具提议 SHALL 在落为待确认提议之前被拒绝，且 SHALL NOT 导致该轮对话失败。

#### Scenario: 工具正文参数不忠实

- GIVEN 模型在某轮返回了一个白名单内的工具提议
- WHEN 该提议的正文参数被判定不忠实
- THEN 后端 SHALL 拒绝该提议
- AND 后端 SHALL 以结构化审计记录该次拒绝及其原因
- AND 该轮 Agent 自然语言回复 SHALL 正常返回
- AND 前端 SHALL NOT 收到待确认提议

#### Scenario: 忠实度校验的位置

- GIVEN 后端校验一条工具提议
- WHEN 校验流程执行
- THEN 忠实度判定 SHALL 与白名单、参数类型和业务边界校验处于同一校验环节
- AND 后端 SHALL NOT 存在跳过忠实度判定即可产生待确认提议的路径

#### Scenario: 提议话术中的伪引用

- GIVEN 提议话术中包含引号包裹的、声称来自用户的片段
- WHEN 该片段在来源集合中没有对应来源
- THEN 后端 SHALL 拒绝该提议
- AND 后端 SHALL NOT 在确认入口上展示该片段

### Requirement: Unfaithful Material Draft Must Not Be Offered To The User

不忠实的素材草稿 SHALL NOT 呈现给用户作为可回填正文的候选。

#### Scenario: 素材草稿增写了用户没有表达过的内容

- GIVEN 会话进入收束并产出素材草稿
- WHEN 该素材草稿被判定不忠实
- THEN 后端 SHALL NOT 返回该素材草稿
- AND 会话 SHALL 正常结束
- AND 用户 SHALL NOT 看到该素材的回填入口

#### Scenario: 素材缺失时的会话生命周期

- GIVEN 素材草稿因不忠实而被丢弃
- WHEN 用户查看会话结果
- THEN 会话状态 SHALL 与素材生成失败时保持一致的语义
- AND 记录正文 SHALL 保持不变

### Requirement: Agent Replies Must Pass Post Generation Content Checks With Downgrade

Agent 回复 SHALL 在返回前经过后置内容检查；检出越界时 SHALL 降级为安全兜底回复。

#### Scenario: Agent 在自己新增的表述中给出诊断

- GIVEN 模型回复中由 Agent 新增的表述包含诊断性判断或医学建议
- WHEN 后端处理该回复
- THEN 后端 SHALL 以安全兜底回复替换该回复
- AND 后端 SHALL 以结构化痕迹记录该次降级

#### Scenario: 用户自己提到病症词而 Agent 共情复述

- GIVEN 用户在自己的表达中使用了某个病症词
- WHEN Agent 回复中该词出现在有来源的区段内
- THEN 后端 SHALL NOT 判定该回复越界
- AND Agent 的共情回应 SHALL 正常返回

#### Scenario: Agent 谎报已代替用户执行不可逆操作

- GIVEN Agent 回复声称已经完成封存、解锁或删除
- WHEN 后端处理该回复
- THEN 后端 SHALL 以安全兜底回复替换该回复
- AND 记录 SHALL 保持不变

#### Scenario: 兜底回复不得冒充模型正常输出

- GIVEN 某轮回复被降级为安全兜底回复
- WHEN 检查该轮的结构化痕迹
- THEN 痕迹 SHALL 可区分该回复来自本地兜底而非 provider 正常产出
- AND 后端 SHALL NOT 将降级报告为一次正常成功的模型回复

#### Scenario: 长度硬上限在多层叠加后仍生效

- GIVEN 后置内容检查已启用
- WHEN 回复经过检查与降级处理
- THEN 回复长度上限 SHALL 仍然生效
- AND 既有的长度裁剪行为 SHALL NOT 被削弱

### Requirement: Guardrail Layers Must Be Additive And Declared In One Source

护栏 SHALL 以叠加方式存在，且规则 SHALL 收敛到单一声明源。

#### Scenario: 新增后置检查后的既有护栏

- GIVEN 后置内容检查与忠实度判定已引入
- WHEN 审查护栏体系
- THEN system prompt 级护栏、工具白名单、二段式用户确认与回复长度上限 SHALL 全部保持原有效力
- AND 后端 SHALL NOT 因引入后置检查而放宽其中任何一层

#### Scenario: 护栏规则的声明位置

- GIVEN 护栏规则同时用于 system prompt 文案与后置检查
- WHEN 审查规则来源
- THEN 两者 SHALL 派生自同一份声明
- AND 护栏规则 SHALL NOT 分散在多处彼此独立维护

#### Scenario: 护栏规则包含正向行为

- GIVEN 护栏规则被声明
- WHEN 审查规则内容
- THEN 规则 SHALL 同时表达 Agent 可以做什么
- AND 规则 SHALL NOT 仅由禁止清单构成

#### Scenario: 护栏阈值的可配置性

- GIVEN 忠实度判定与后置检查存在阈值
- WHEN 阈值被配置
- THEN 阈值 SHALL 来自 backend-side 配置
- AND 配置 SHALL NOT 引入任何新的凭证字段

### Requirement: Guardrail Traces Must Exclude Diary And Conversation Content

护栏判定与降级 SHALL 留下结构化痕迹，且 SHALL NOT 包含用户日记原文或对话原文。

#### Scenario: 忠实度判定留痕

- GIVEN 一次忠实度判定完成
- WHEN 痕迹被写出
- THEN 痕迹 SHALL 只包含结构化指标与判定结论，例如覆盖比例、最长未覆盖片段长度、违规类型
- AND 痕迹 SHALL NOT 包含候选文本、用户原话或未覆盖片段的内容

#### Scenario: 来源集合的生命周期

- GIVEN 后端为判定构造了来源集合
- WHEN 判定结束
- THEN 来源集合 SHALL NOT 被持久化
- AND 来源集合 SHALL NOT 被写入日志或外发

#### Scenario: 降级痕迹的可见性边界

- GIVEN 某次越界被检出并降级
- WHEN 结果返回给前端
- THEN 用户 SHALL NOT 被告知护栏内部判定过程
- AND 开发者可见的痕迹 SHALL 只存在于后端结构化记录中

### Requirement: Guardrail Boundary Cases Must Be Covered By A Regression Suite

护栏 SHALL 具备可持续回归的边界用例集。

#### Scenario: 边界用例覆盖范围

- GIVEN C4 实现完成
- WHEN 审查测试资产
- THEN 边界用例集 SHALL 覆盖诱导诊断的输入、试图让 Agent 改写用户原文的注入型输入、过长输出与试图让 Agent 代替用户决策的输入
- AND 边界用例集 SHALL 包含已观测到的增写用户原话的真实样本回归

#### Scenario: 回归用例的执行条件

- GIVEN 边界用例集存在
- WHEN 用例被执行
- THEN 用例 SHALL 可在不调用真实 provider 的情况下运行

### Requirement: Guardrails Hardening Must Exclude Memory Observability Queries And Guidance Tuning

C4 SHALL 限定在忠实度判定、后置内容检查、违规降级与护栏可维护化范围内。

#### Scenario: C4 范围内的记忆能力

- GIVEN 用户与 Agent 对话
- WHEN 护栏执行判定
- THEN 判定所用的来源 SHALL 只来自当前会话
- AND 历史记录检索与跨记录关联 SHALL 留给后续独立 change

#### Scenario: C4 范围内的可观测能力

- GIVEN 护栏痕迹已落地
- WHEN 评估可观测能力
- THEN C4 SHALL NOT 提供决策链路查询端点或可观测界面
- AND 决策链路可查询 SHALL 留给后续独立 change

#### Scenario: C4 范围内的引导与素材质量

- GIVEN 护栏已系统化
- WHEN 评估引导话术与素材合成质量
- THEN C4 SHALL NOT 修改引导阶段的提问策略
- AND C4 SHALL NOT 修改素材合成策略
- AND 质量优化 SHALL 留给后续独立安排

#### Scenario: C4 范围内的工具白名单

- GIVEN 护栏加固完成
- WHEN 审查 Agent 可达的后端写操作
- THEN 可达范围 SHALL NOT 超出 C2 已接受的白名单
- AND C4 SHALL NOT 新增任何工具

## MODIFIED Requirements

> 以下两条 scenario 原属已接受条款，其「留给后续独立 change」的指向由 C4 落实，故修订为指向 C4 的现状描述。
> 除这两条 scenario 外，原条款的其余 scenario 保持不变。

### Requirement: Agent Runtime MVP Must Exclude Tools Memory And Post Filtering

> C2 修订：本条款中的「工具调用留给后续独立 change」已由 C2 落实。
> C4 修订：本条款中的「后置输出过滤与违规降级留给后续独立 change」已由 C4 落实。
> 两条历史范围约束均保留为 C1 范围声明。

C1 SHALL 限定在对话 Runtime 与最小护栏范围内；工具调用边界自 C2 起、内容合规防御深度自 C4 起分别由本 spec 的相应条款约束。

#### Scenario: C1 范围内的护栏深度

- GIVEN 仅 C1 实现存在
- WHEN 评估护栏防御深度
- THEN 该阶段的护栏 SHALL 仅由 system prompt 约束与回复长度裁剪构成
- AND 后置输出检查与违规降级 SHALL 自 C4 起由本 spec 的护栏条款约束

### Requirement: Agent Tool Calling Must Exclude Memory Post Filtering And Observability Queries

> C4 修订：本条款中的「系统化 hardening 留给后续独立 change」已由 C4 落实。
> 其余 scenario 保持不变。

C2 SHALL 限定在工具白名单、二段式确认与受控执行范围内。

#### Scenario: C2 范围内的护栏深度

- GIVEN C2 的工具白名单已生效
- WHEN 评估内容合规防御深度
- THEN 该阶段 SHALL NOT 包含后置内容过滤或违规降级机制
- AND 工具参数内容的忠实度判定 SHALL 自 C4 起由本 spec 的忠实度条款约束
