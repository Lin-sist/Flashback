# Agent Runtime Spec Delta：agent-eval-framework（C6）

> 本文件是 **delta 建议**，闸门 1 待批准。范围：Agent 编排行为的可回归性——不变量断言、快照回归比对、评测的隐私与诚实边界。
> **本刀不改任何 Agent 行为、不改任何 main 代码**，故不涉及阶段推进、prompt、护栏阈值、记忆检索、工具白名单、回看逻辑、轨迹采集的任何条款。
> C5 的一条「评估能力留给后续独立 change」scenario 因本刀落地而须修订（下文 MODIFIED）。

---

## MODIFIED Requirements

> 修订方式沿用 C5 修订 C2 / C4 / C3a / C3b 四条时的做法：
> **保留阶段范围声明，改为指向本刀条款，不删除。**
> 理由与当时相同——范围声明本身是历史事实，删掉就看不出能力何时到位。

### 修订 1：`Observability Must Exclude Evaluation Resilience And Temporal Intelligence` 的评估 scenario

#### Scenario: C5 范围内的评估能力

> C6 修订：原文「评估能力 SHALL 留给后续独立 change」已由 C6 落实。

- GIVEN 轨迹已携带版本锚点
- WHEN 评估回归能力
- THEN C5 SHALL NOT 提供黄金集、评估运行器或质量评分
- AND 可回归的评测能力 SHALL 自 C6 起由本 spec 的评测条款约束
- AND 绝对质量评分 SHALL 仍然不被提供

---

## ADDED Requirements

### Requirement: Orchestration Invariants Must Be Asserted Offline

Agent 编排行为的不变量 SHALL 由可离线执行的评测断言，SHALL NOT 依赖人工阅读回复判断。

#### Scenario: 评测的执行条件

- GIVEN 评测资产存在
- WHEN 评测被执行
- THEN 评测 SHALL 在不调用真实 provider 的情况下运行
- AND 评测 SHALL NOT 依赖真实数据库或外部服务
- AND 评测 SHALL 随后端既有测试的默认执行路径一并运行

#### Scenario: 断言对象

- GIVEN 一轮对话已由评测驱动完成
- WHEN 评测做出判定
- THEN 评测 SHALL 断言该轮的决策轨迹信号
- AND 评测 SHALL NOT 以回复文本的措辞作为通过条件

#### Scenario: 阶段推进的不变量

- GIVEN 写作引导的一组用例被执行
- WHEN 检查阶段判定序列
- THEN 阶段的来源、去向与判定结论序列 SHALL 合法
- AND 同一阶段的追问 SHALL NOT 超过既有实现约定的上限
- AND 轮次上限到达的收束 SHALL 可被触发

#### Scenario: 记忆注入的不变量

- GIVEN 记忆检索与注入已发生
- WHEN 检查注入规模
- THEN 注入的片段数 SHALL NOT 超过配置的片段上限
- AND 注入的总长度 SHALL NOT 超过由片段上限与单片段长度上限派生的界
- AND 该派生界 SHALL 被如实标注为派生值，SHALL NOT 被表述为一个已存在的配置项

#### Scenario: 记忆三态的不变量

> 实现期实测印证了这条为何必须落在不变量层：「检索成功但无命中」与
> 「能力被关闭」两种情形的**快照指标完全相同**，区别只存在于轨迹的检索状态字段上。
> 仅靠快照回归比对无法区分它们。

- GIVEN 某一轮没有产生记忆片段
- WHEN 评测检查该轮
- THEN 「能力被关闭」「检索失败」「检索成功但无命中」三种情形 SHALL 可被区分
- AND 评测 SHALL NOT 将它们视为同一种通过条件
- AND 该区分 SHALL 由不变量断言承担，SHALL NOT 仅依赖快照比对

#### Scenario: 输出克制的不变量

- GIVEN 某轮回复已产出
- WHEN 检查长度约束
- THEN 回复长度 SHALL NOT 超过配置的回复长度上限

#### Scenario: 护栏与降级的不变量

- GIVEN 一组已知应被拦截的候选输出
- WHEN 评测驱动它们经过护栏
- THEN 各层判定结论 SHALL 与期望一致
- AND 应当降级的路径 SHALL 发生降级
- AND 不下发工具的模式下返回的工具提议 SHALL 被丢弃

#### Scenario: 不变量失败的处置

- GIVEN 某条不变量断言失败
- WHEN 开发者处理该失败
- THEN 该失败 SHALL 被视为缺陷
- AND 系统 SHALL NOT 提供任何刷新或接受该失败的手段

#### Scenario: 不依赖用例声明的通用不变量

> 存在理由：写在用例文件里的期望依赖作者记得声明。
> 一批通用底线放在断言层统一执行，可使任何新增用例自动受其保护。

- GIVEN 任意一条评测用例被执行
- WHEN 不变量被校验
- THEN 长度上限、追问上限、注入派生上限、判定结论取值范围、轮次序号单调性
  与版本锚点在位 SHALL 被无条件校验
- AND 这些校验 SHALL NOT 依赖用例逐条声明

#### Scenario: 期望键的拼写

> 守的是「静默失效」：拼错的期望键若被忽略，用例文件里写着一条期望，
> 实际什么都没验，而测试是绿的。

- GIVEN 某条用例声明了一个未被断言层消费的期望键
- WHEN 该用例被执行
- THEN 评测 SHALL 失败
- AND 评测 SHALL NOT 静默忽略该期望

### Requirement: Regression Baselines Must Be Comparable And Traceable

评测 SHALL 提供可比对的基线，且基线的变更 SHALL 留下可审计的说明。

#### Scenario: 基线比对

- GIVEN 某次改动前已存在基线
- WHEN 评测在改动后被执行
- THEN 评测 SHALL 报告哪些指标相对基线发生变化
- AND 报告 SHALL 同时给出基线值与当前值

#### Scenario: 基线的版本归属

- GIVEN 某条基线被记录
- WHEN 检查该基线
- THEN 该基线 SHALL 记录定基线时的提示词版本与护栏规则版本
- AND 基线 SHALL 可按版本锚点分组比对

#### Scenario: 基线变更须留痕

- GIVEN 某条基线的指标值被更新
- WHEN 该更新被提交
- THEN 对应的基线说明 SHALL 一并更新
- AND 只更新指标值而不更新说明的情形 SHALL 被评测拦截
- AND 该拦截 SHALL 由机制保证，SHALL NOT 仅依赖文档约定

#### Scenario: 基线变更留痕机制的完整性

> 若校验值只由指标派生，则「改指标并同步改说明」这一**正确**流程
> 与「只改指标」会得到相同的校验值，机制形同虚设。

- GIVEN 基线变更留痕由校验值机制保证
- WHEN 只有基线说明被修改而指标未变
- THEN 校验值 SHALL 同样发生变化
- AND 该机制自身 SHALL 被测试直接验证

#### Scenario: 基线更新的操作方式

- GIVEN 某条基线需要更新
- WHEN 开发者执行更新
- THEN 系统 SHALL NOT 提供自动重写全部基线的开关
- AND 更新 SHALL 保持为一个需要人工判断的动作
- AND 评测 SHALL 在失败信息中给出可供人工确认后粘贴的新值

#### Scenario: 基线的孤儿条目

- GIVEN 某条用例已被删除而其基线条目仍存在
- WHEN 评测被执行
- THEN 评测 SHALL 失败
- AND 理由 SHALL 是孤儿基线会让「该维度仍有回归守护」成为假象

### Requirement: Evaluation Must Not Change Agent Behavior

评测 SHALL 只观测编排行为，SHALL NOT 为可评测性而改变它。

#### Scenario: 评测与生产路径的边界

- GIVEN 评测需要驱动 mock 路径产不出的场景
- WHEN 评测装配替身
- THEN 替身 SHALL 只存在于测试范围
- AND 评测 SHALL NOT 修改在生产路径上被使用的组件

#### Scenario: 评测发现的问题

- GIVEN 评测揭示了某个编排行为的缺陷
- WHEN 处理该缺陷
- THEN 建立评测的同一变更 SHALL NOT 顺手修改 Agent 行为、提示词或护栏阈值
- AND 行为修改 SHALL 由后续独立变更承担

#### Scenario: 阈值校准的边界

- GIVEN 评测已能支撑阈值校准
- WHEN 审查本阶段范围
- THEN 建立评测的变更 SHALL NOT 校准任何护栏阈值
- AND 基线 SHALL 记录校准前的当前状态

### Requirement: Evaluation Must Not Leak Diary Or Case Content

评测产物 SHALL 只包含结构化标识与数值指标。

#### Scenario: 基线与报告的内容边界

- GIVEN 用例输入包含文本
- WHEN 基线与失败报告被写出
- THEN 它们 SHALL NOT 包含用例输入文本
- AND 它们 SHALL NOT 包含用户日记原文、对话原文或记忆片段内容

#### Scenario: 判定指标的形状

- GIVEN 护栏判定指标被用于断言
- WHEN 评测校验该指标
- THEN 评测 SHALL 断言该指标整体符合纯数值形状
- AND 评测 SHALL NOT 仅以列举若干禁止词的方式校验

#### Scenario: 真实样本的处置

- GIVEN 评测可使用真实样本作为输入
- WHEN 该样本存在于工作区
- THEN 该样本 SHALL NOT 进入 tracked files
- AND 该样本缺失时评测 SHALL 静默跳过相关用例而非失败
- AND 入库的用例 SHALL 只使用合成内容

#### Scenario: 入库用例缺失

- GIVEN 入库的合成用例文件缺失
- WHEN 评测被执行
- THEN 评测 SHALL 失败
- AND 评测 SHALL NOT 静默跳过

#### Scenario: 用例解析不可用

- GIVEN 用例文件的解析能力不可用
- WHEN 评测被执行
- THEN 评测 SHALL 明确失败
- AND 评测 SHALL NOT 表现为「全部用例通过」

### Requirement: Evaluation Must State Its Honest Boundary

评测 SHALL 显式声明它覆盖什么、不覆盖什么。

#### Scenario: 语言质量的覆盖边界

- GIVEN 评测在替身与 mock 路径上运行
- WHEN 描述评测的能力
- THEN 评测 SHALL 被表述为覆盖编排逻辑的不变量与回归比对
- AND 评测 SHALL NOT 被表述为对模型语言质量的判定
- AND 语言质量 SHALL 由真实探针的小样本人评锚定

#### Scenario: 人评锚点尚未填充

- GIVEN 人评锚点的结构已就位而内容为空
- WHEN 审查该维度
- THEN 该维度 SHALL 被如实标注为锚点未填充
- AND 空的锚点 SHALL NOT 被视为该维度已覆盖
- AND 该标注 SHALL 由测试保证其存在，SHALL NOT 仅写在文档中

#### Scenario: 人评锚点的内容边界

- GIVEN 人评锚点将被填充
- WHEN 定义其字段
- THEN 锚点 SHALL 只记录评级、理由标签与当时的版本锚点
- AND 锚点 SHALL NOT 承载 Agent 回复原文或用户日记原文

#### Scenario: 未验证项的表述

- GIVEN 评测未在真实 provider 下验证指标稳定性
- WHEN 记录验证结果
- THEN 该项 SHALL 被如实记为未验证
- AND 该项 SHALL NOT 被表述为已通过

### Requirement: Evaluation Must Exclude Judging Reflection Resilience And Temporal Intelligence

C6 SHALL 限定在不变量断言、回归比对与评测隐私范围内。

#### Scenario: C6 范围内的评分能力

- GIVEN 评测已能断言编排不变量
- WHEN 评估质量判定能力
- THEN C6 SHALL NOT 引入第二个模型对输出打分
- AND C6 SHALL NOT 产出绝对质量分数
- AND C6 SHALL NOT 提供质量看板或对比实验框架

#### Scenario: C6 范围内的输出行为

- GIVEN 评测已建立基线
- WHEN 审查 Agent 的输出行为
- THEN C6 SHALL NOT 引入不合格输出的重写环
- AND 重写能力 SHALL 留给后续独立 change

#### Scenario: C6 范围内的韧性与时间智能

- GIVEN 评测覆盖了失败路径的不变量
- WHEN 评估失败处置与时间感知能力
- THEN C6 SHALL NOT 引入错误分类策略、降级模板或多 provider 路由
- AND C6 SHALL NOT 引入时间距离话术或记忆衰减策略
- AND 两者 SHALL 留给后续独立 change

#### Scenario: C6 范围内的用户可见能力

- GIVEN 评测面向开发者
- WHEN 审查用户可见界面
- THEN C6 SHALL NOT 新增任何用户可见能力

#### Scenario: C6 范围内的引导与素材质量

- GIVEN 评测已能比对引导话术改动前后的指标
- WHEN 评估引导话术与素材合成质量
- THEN C6 SHALL NOT 修改引导阶段的提问策略
- AND C6 SHALL NOT 修改素材合成策略
- AND 质量优化 SHALL 在基线就位之后由独立安排承担
