# Delta Spec：miniapp-core（P4.2 Memory Agency）

## ADDED Requirements

### Requirement: Mini Program Must Present Session Scoped Memory Authorization

小程序 SHALL 在现有 Agent sheet 内显示“本次可参考过去记录”，默认关闭且不诱导开启。

#### Scenario: 默认状态

- GIVEN 用户开始或恢复一个未授权 session
- WHEN Agent sheet 展示
- THEN 控件 SHALL 显示关闭
- AND 文案 SHALL 说明默认只看当前记录与这次对话

#### Scenario: 开启或关闭

- GIVEN 用户主动切换授权
- WHEN backend 成功保存
- THEN UI SHALL 显示返回的真实状态
- AND 切换本身 SHALL 不生成 Agent message 或调用 provider

#### Scenario: 切换失败

- GIVEN authorization 请求失败
- WHEN UI 收到错误
- THEN 原状态 SHALL 保持
- AND SHALL 给出可理解且不暴露基础设施细节的提示

### Requirement: Mini Program Must Show Only Actual Per Message Sources

小程序 SHALL 只在 assistant message 有实际来源时展示来源，不展示内部检索信号。

#### Scenario: 有实际来源

- GIVEN assistant message 返回非空 memorySources
- WHEN message bubble 渲染
- THEN SHALL 显示轻量“参考了过去的记录”与 source chip
- AND available source SHALL 可回到对应 owned record

#### Scenario: 无来源

- GIVEN assistant message 的 memorySources 为空
- WHEN UI 渲染
- THEN SHALL 不显示来源区
- AND SHALL NOT 根据回复文字猜测或伪造来源

#### Scenario: 来源不可用

- GIVEN source record 已删除或当前不可访问
- WHEN UI 渲染历史 message
- THEN SHALL 显示不可点击的不可用状态
- AND SHALL NOT 显示旧标题、片段、摘要、关键词、分数或命中数

### Requirement: Record Detail Must Offer Memory Exclusion And User Context

记录详情 SHALL 提供“不再供 Agent 参考”与用户时间语境说明的轻量控制。

#### Scenario: 排除记录

- GIVEN owner 查看自己的记录详情
- WHEN 开启“不再供 Agent 参考”并保存
- THEN UI SHALL 显示 backend 返回的真实 policy
- AND 文案 SHALL 只承诺未来轮次不再参考

#### Scenario: 补充说明

- GIVEN 用户认为过去表达不代表现在
- WHEN 用户输入并保存 context note
- THEN UI SHALL 保存用户原文且不调用 provider
- AND SHALL NOT 自动扩写、总结或生成画像

#### Scenario: SEALED record

- GIVEN 记录尚未解锁
- WHEN 用户修改 memory policy
- THEN UI MAY 提供同意元数据控制
- AND SHALL NOT 展示或解锁被封存内容

### Requirement: Memory Agency UI Must Preserve Existing Product Surface

P4.2 SHALL 是现有对话与记录详情的局部增量，不得形成分析或设置中心。

#### Scenario: 导航与角色

- GIVEN memory agency UI 已实现
- WHEN 审查小程序结构
- THEN 首页、时光轴、个人中心三个一级 Tab 与 canonical naming SHALL 保持
- AND witness role、LISTEN/UNTANGLE、结束/重试/工具与素材确认 SHALL 保持

#### Scenario: 无分析表面

- GIVEN 用户查看来源或 policy
- WHEN 页面渲染
- THEN SHALL NOT 展示跨记录清单、情绪趋势、模式、评分、画像或建议 dashboard

#### Scenario: Preview

- GIVEN 当前为无真实登录的 Preview
- WHEN 用户浏览 Agent/record 表面
- THEN Agent、authorization、policy、source 的真实 backend 请求数 SHALL 为 0
- AND UI SHALL NOT 伪造来源或授权成功
