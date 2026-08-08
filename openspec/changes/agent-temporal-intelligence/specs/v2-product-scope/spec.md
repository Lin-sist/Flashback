# V2 Product Scope Spec Delta：agent-temporal-intelligence（C9）

> 规划草案。范围：朋友式时间感、克制重复主题提及、反分析/反诊断与产品表面不扩张。

## MODIFIED Requirements

### 修订：`Agent Memory Must Feel Like A Friend Remembering, Not A System Analyzing`

#### Scenario: Agent 带时间距离关联历史感受

> C9 修订：C3a 的月份归属继续成立；C9 允许在准确锚点之外表达距离，但不允许据此给用户下结论。

- GIVEN 用户此刻表达的内容与其过去记录相关
- WHEN Agent 以朋友方式提起那时候的事
- THEN Agent MAY 用“最近、几个月前、更早”表达时间距离
- AND SHALL 保留过去记录的准确月份锚点
- AND SHALL NOT 把时间距离转成画像、阶段论、诊断或变化结论

## ADDED Requirements

### Requirement: Temporal Intelligence Must Return Interpretation To The User

#### Scenario: 用户主动比较过去与现在

- GIVEN 用户在回看对话中主动询问“是不是又这样”或“和以前相比呢”
- WHEN 存在有限但足够的跨时间重复证据
- THEN Agent MAY 温和指出“似乎不止一次出现过”
- AND SHALL 以问题邀请用户自己判断是否有关
- AND SHALL NOT 替用户定义原因、规律、成长、退步或未来走向

#### Scenario: 用户没有比较意图

- GIVEN 用户只是在读或讲述一条记录
- WHEN Agent 回应
- THEN Agent SHALL 顺着用户当前表达陪伴
- AND SHALL NOT 主动输出跨记录分析、历史总结或模式提醒

### Requirement: Temporal Intelligence Must Not Become An Analysis Product

#### Scenario: 禁止量化与诊断

- GIVEN Agent 使用了多条历史记录
- WHEN 生成用户可见回复
- THEN SHALL NOT 输出百分比、频率评分、情绪趋势、人格标签、心理诊断或风险判断
- AND SHALL NOT 使用“每年都会、总是如此、必然导致、以后还会”等确定性结论

#### Scenario: 产品表面

- GIVEN C9 被实现
- WHEN 审查小程序用户界面
- THEN SHALL NOT 新增 dashboard、图表、评分卡、分析页、设置页、推送或弹窗
- AND 首页、时光轴、个人中心三个一级 Tab SHALL 保持
- AND “我的记录、时光轴、时间回看”命名 SHALL 保持

### Requirement: Older Memories Must Be Quieter Not Erased

#### Scenario: 较旧的关联记忆

- GIVEN 一条较旧历史记录与当前表达相关
- WHEN backend 组装对话上下文
- THEN 该记录 MAY 以更小的注入预算参与理解
- AND 业务记录、时光轴展示与用户主动打开能力 SHALL 不受影响
- AND 产品 SHALL NOT 暗示旧记录已失效、不重要或被系统遗忘

#### Scenario: 用户主动回看旧记录

- GIVEN 用户主动打开一条年代较久的已解锁记录
- WHEN 开启回看对话
- THEN 该记录 SHALL 仍是对话主体
- AND Agent SHALL NOT 因“记忆衰减”缩减用户主动选择阅读的那条记录

### Requirement: Temporal Intelligence Must Remain Passive And Private

#### Scenario: 被动召唤

- GIVEN temporal policy 发现了重复主题证据
- WHEN 用户没有主动开启 Agent 或回看对话
- THEN 系统 SHALL NOT 推送、弹窗、自动展开对话或生成提醒

#### Scenario: 检索与策略内部状态

- GIVEN 一轮使用了距离层级、衰减或 recurrence hint
- WHEN 用户查看回复
- THEN 用户 SHALL NOT 看到命中数、距离桶、衰减比例、pattern eligible、score 或策略版本
- AND 内部判断 SHALL NOT 被包装成“系统分析结果”
