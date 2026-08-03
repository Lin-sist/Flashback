# V2 Product Scope Spec Delta：agent-reflection-loop（C7）

> 闸门 1 已于 2026-08-02 批准。范围：不新增界面的用户可见回复/素材终态行为。

## ADDED Requirements

### Requirement: Recoverable Guardrail Failures May Be Rewritten Once Before Fallback

#### Scenario: 回看回复缺少时间归属

- GIVEN Agent 复述了过去记录但没有说明时间归属
- WHEN 后端护栏判定为 `MISSING_TIME_ATTRIBUTION`
- THEN 系统 MAY 在同一轮内要求模型重写一次
- AND 重写要求 SHALL 只要求补明过去时间归属
- AND 用户 SHALL 只看到最终合格回复或既有安全兜底

#### Scenario: 收束素材保持既有边界

- GIVEN Agent 整理的素材包含用户未表达的内容
- WHEN 后端护栏判定为 `UNFAITHFUL`
- THEN 系统 SHALL NOT 在 C7 为素材发起 reflection
- AND 素材 SHALL 被丢弃
- AND 不合格素材 SHALL NOT 进入记录正文

#### Scenario: 严重越界或检查异常

- GIVEN Agent 输出命中诊断、代决、伪引用或检查异常
- WHEN 后端处置
- THEN 系统 SHALL NOT 为该输出提供第二次机会
- AND SHALL 继续使用既有 fail-closed 行为

### Requirement: Reflection Must Not Change Product Surface Or Temperament

#### Scenario: 用户界面

- GIVEN C7 被实现
- WHEN 用户使用小程序
- THEN SHALL NOT 新增 reflection 状态、按钮、页面、提示灯或技术术语
- AND 三个一级 Tab 与 V2 用户可见命名 SHALL 保持不变

#### Scenario: 产品气质

- GIVEN 模型被要求重写
- WHEN 生成最终回复或素材
- THEN 重写 SHALL 只为更忠实或更明确时间归属
- AND SHALL NOT 以更热情、更长、更诊断化或更主动为目标

#### Scenario: 失败终态

- GIVEN reflection 未成功
- WHEN 用户收到终态
- THEN reply SHALL 使用既有本地安全兜底；material SHALL 继续按既有规则保持缺失
- AND 系统 SHALL NOT 把本地兜底伪装成模型正常输出
