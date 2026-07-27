# V2 Product Scope Spec Delta（C2 `agent-tool-calling`）

> 本 delta 覆盖 C2 的产品行为与范围边界。
> 待规划闸批准；批准并实现验收后才接受进 `openspec/specs/v2-product-scope/spec.md`。

## ADDED Requirements

### Requirement: Agent May Turn Conversation Into Action Only With User Consent

Agent SHALL 可以在对话中把建议变成行动，但每一次行动 SHALL 由用户当场同意。

#### Scenario: 对话中产生可执行建议

- GIVEN 用户正在与 Agent 进行写作引导对话
- WHEN Agent 判断存在可以帮用户完成的小动作
- THEN Agent SHALL 以一句克制的提议询问用户
- AND 行动 SHALL 只在用户同意后发生

#### Scenario: 用户不想要这个行动

- GIVEN Agent 给出了一个行动提议
- WHEN 用户表示不需要
- THEN 记录 SHALL 保持不变
- AND Agent SHALL NOT 追问同一个提议

### Requirement: Agent Must Remain A Friend Not A Manager

Agent SHALL 保持建议不代决的气质，SHALL NOT 接管属于用户的决定。

#### Scenario: 涉及封存这类不可逆决定

- GIVEN 用户在对话中提到想封存这条记录
- WHEN Agent 回应
- THEN Agent SHALL 建议用户自己去确认
- AND Agent SHALL NOT 代为完成封存

#### Scenario: 涉及用户原文

- GIVEN 用户请求 Agent 修改已经写下的文字
- WHEN Agent 回应
- THEN 用户原文 SHALL 保持不变
- AND Agent SHALL 只能追加用户自己表达过的内容

#### Scenario: 行动提议的表达长度

- GIVEN Agent 给出行动提议
- WHEN 提议呈现给用户
- THEN 提议 SHALL 简短克制
- AND SHALL NOT 附带分析、评分或诊断性说明

### Requirement: Agent Tool Scope Must Stay Within Product Boundaries

Agent 可执行的行动范围 SHALL 限于低风险、可逆的草稿整理动作。

#### Scenario: 审查 Agent 可执行的行动

- GIVEN 工具调用能力已上线
- WHEN 审查 Agent 可执行的行动清单
- THEN 清单 SHALL 只包含作用于草稿的可逆动作
- AND SHALL NOT 包含封存、解锁、删除、位置、封面或附件相关动作

#### Scenario: 三 Tab 与用户可见命名

- GIVEN 工具调用能力已上线
- WHEN 用户浏览小程序
- THEN 首页、时光轴、个人中心三个一级 Tab SHALL 保持不变
- AND 我的记录、时光轴、时间回看的用户可见命名 SHALL 保持不变

#### Scenario: 被动召唤原则

- GIVEN 工具调用能力已上线
- WHEN 用户没有主动与 Agent 对话
- THEN 系统 SHALL NOT 主动弹出行动提议
- AND 系统 SHALL NOT 推送或自动执行任何行动
