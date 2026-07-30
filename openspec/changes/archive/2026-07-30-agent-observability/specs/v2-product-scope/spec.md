# V2 Product Scope Spec Delta：agent-observability（C5）

> 本文件是 delta。范围：可观测能力的产品边界。
> C5 **不新增任何用户可见能力**，因此本 delta 全部是「不做什么」的边界声明。
> `miniapp-core` **无 delta**（前端零改动）。

---

## ADDED Requirements

### Requirement: Agent Observability Must Stay Invisible To Product Users

Agent 决策轨迹 SHALL 是工程能力，SHALL NOT 改变任何用户可见的产品体验。

#### Scenario: 引入可观测后的用户体验

- GIVEN 决策轨迹已落地
- WHEN 用户使用写作引导或时间回看
- THEN 用户可见的界面、文案与交互 SHALL 与引入之前完全一致
- AND 用户 SHALL NOT 看到任何轨迹、指标或判定过程

#### Scenario: 三个一级 Tab 与用户可见命名

- GIVEN 可观测能力已落地
- WHEN 审查产品结构
- THEN 首页、时光轴、个人中心三个一级 Tab SHALL 保持不变
- AND 我的记录、时光轴、时间回看的用户可见命名 SHALL 保持不变

#### Scenario: Agent 的气质

- GIVEN Agent 的每一步现在都被记录
- WHEN 用户与 Agent 对话
- THEN Agent 的回复长度、语气与克制程度 SHALL 与引入之前一致
- AND Agent SHALL NOT 因为可观测而变得更像一个系统组件

### Requirement: Observability Must Not Become A Product Analytics Surface

可观测能力 SHALL NOT 演化为面向用户的分析或诊断能力。

#### Scenario: 情绪与行为分析

- GIVEN 轨迹包含每轮的判定与指标
- WHEN 评估可以从中衍生什么
- THEN 产品 SHALL NOT 由此生成情绪轨迹、趋势评分或用户画像
- AND 产品 SHALL NOT 由此生成任何诊断性结论

#### Scenario: 使用数据的呈现

- GIVEN 轨迹记录了对话轮次、耗时与失败
- WHEN 评估是否呈现给用户
- THEN 产品 SHALL NOT 向用户呈现使用统计或效率看板
- AND 这与产品「不是效率仪表盘」的定位一致

#### Scenario: 主动打扰

- GIVEN 轨迹可识别出用户的对话频率与模式
- WHEN 评估是否据此触达用户
- THEN 产品 SHALL NOT 据此发起推送、弹窗或提醒
- AND 被动召唤的约束 SHALL 保持不变
