# Miniapp Core Spec Delta：agent-temporal-intelligence（C9）

> 规划草案。范围：复用既有 Agent/回看表面呈现时间感知话术，禁止分析 UI 与新契约字段。

## ADDED Requirements

### Requirement: Temporal Replies Must Reuse Existing Conversation Surfaces

#### Scenario: 时间感知回复

- GIVEN backend 返回一条通过护栏的时间感知 Agent 回复
- WHEN小程序展示该回复
- THEN SHALL 复用既有写作引导或回看对话消息表面
- AND SHALL NOT 为 distance、pattern、score 或 trend 新增卡片、徽标、图表、字段或页面

#### Scenario: temporal overreach 降级

- GIVEN backend 因 `TEMPORAL_OVERREACH` 返回既有安全兜底
- WHEN 小程序展示结果
- THEN SHALL 按现有消息/状态契约呈现
- AND SHALL NOT 暴露内部违规类型、匹配词或检索证据

### Requirement: Time Review UI Must Remain Quiet And User Initiated

#### Scenario: 回看入口与浮层

- GIVEN C9 已启用
- WHEN 用户打开一条 UNLOCKED 记录
- THEN 既有回看入口、回应主动作与浮层互斥行为 SHALL 保持
- AND 小程序 SHALL NOT 自动开启对话、主动展示模式分析或新增时间智能入口

#### Scenario: 无新增分析表面

- GIVEN temporal policy 识别到有限重复证据
- WHEN 页面渲染
- THEN SHALL NOT 展示情绪趋势、周期、评分、成长报告或跨记录清单
- AND 三个一级 Tab 与 V2.0 用户可见命名 SHALL 保持不变
