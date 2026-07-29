# V2 Product Scope Spec Delta：agent-review-chat（C3 后半刀）

> 本文件是 delta。范围：友人回看对话的产品行为与气质边界。

---

## ADDED Requirements

### Requirement: Time Review Must Offer A Friend To Talk To, Not Just A Summary

时间回看 SHALL 在既有结构化摘要之外提供一个可以聊聊的对象。

#### Scenario: 解锁后的回看体验

- GIVEN 一条记录已经抵达并解锁
- WHEN 用户读完那时写下的内容
- THEN 用户 MAY 主动开启一段回看对话
- AND 既有的结构化摘要与回应能力 SHALL 保持不变

#### Scenario: 回看对话的气质

- GIVEN 用户在回看对话中说话
- WHEN Agent 回应
- THEN Agent SHALL 以朋友的方式陪他重新理解那时的自己
- AND Agent SHALL NOT 替他下结论
- AND Agent SHALL NOT 做心理诊断或归类
- AND Agent 的回复长度 SHALL 与用户的表达相称

#### Scenario: 被动召唤

- GIVEN 回看对话已上线
- WHEN 用户未主动开启
- THEN 系统 SHALL NOT 弹窗、推送或自动展开对话
- AND 系统 SHALL NOT 因记录解锁而主动发起对话

#### Scenario: 尊重结束

- GIVEN 用户不想继续这段回看对话
- WHEN 用户结束或不再回应
- THEN Agent SHALL 优雅收束
- AND Agent SHALL NOT 追问或挽留

### Requirement: Review Chat Must Not Alter What Was Written Back Then

回看对话 SHALL NOT 改变那时写下的内容。

#### Scenario: 那时的记录与此刻的对话

- GIVEN 用户在回看对话中说了很多此刻的想法
- WHEN 对话结束
- THEN 那条记录的正文 SHALL 逐字保持不变
- AND 此刻的整理 SHALL NOT 被写进那条记录
- AND 封存后不可变的位置、附件与封面 SHALL 保持不变

#### Scenario: 时间感的完整性

- GIVEN 用户几个月后再次打开这条记录
- WHEN 用户阅读它
- THEN 用户 SHALL 能确定读到的是当时写下的内容
- AND 记录中 SHALL NOT 混入回看时补写的句子

#### Scenario: 引用那时的话

- GIVEN Agent 在回看对话中提起那时写下的内容
- WHEN Agent 表达
- THEN Agent SHALL 说清那是过去哪个时候的事
- AND Agent SHALL NOT 让过去的表达听起来像用户此刻说的

### Requirement: Review Chat Scope Must Exclude Analysis And Advice Products

回看对话 SHALL NOT 演变为分析或建议型产品能力。

#### Scenario: 范围外的产品能力

- GIVEN 回看对话已落地
- WHEN 审查产品范围
- THEN 成长报告、情绪轨迹图表、行为建议清单与心理评估 SHALL NOT 被实现
- AND 回看对话 SHALL NOT 产生可分享的对外内容
- AND 三个一级 Tab 与 V2.0 用户可见命名 SHALL 保持不变
