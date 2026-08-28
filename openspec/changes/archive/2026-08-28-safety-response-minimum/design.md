# Design：Safety Response Minimum（R1）

## 1. Context

`AgentChatServiceImpl.sendMessageTraced` 当前在持久化用户消息后检查 provider、检索 memory、生成 reply、工具与素材。R1 在用户消息已可靠保存、但任何 provider/memory/tool/material 行为发生前插入 `AgentSafetyPolicy`。

## 2. Decision Records

### D1｜只判当前输入，不读历史或记忆

安全判定只读取本轮用户输入。原因是避免把过去表达重新解释成当前风险，也不建立长期风险标签。

### D2｜高精度规则，不做诊断或评分

仅匹配第一人称与明确自伤/自杀意图、正在实施或近时计划的组合；否定、第三人称、引用、假设、研究讨论、历史回忆和常见比喻 fail-open 到普通路径。规则返回封闭枚举与 ruleId，不返回概率。

### D3｜本地固定响应优先于 provider

命中后不调用 provider、不做 reflection、不检索跨记录 memory、不生成工具/素材。响应由 backend 常量拥有，避免 provider 不可用或生成漂移。

### D4｜地区资源不推断位置

固定文案写“若在中国大陆”，列 `120`/`110` 与 `12356`；否则联系当地紧急服务。当前核验依据为国家卫健委与工信部门官方页面。号码不从用户定位推断。

### D5｜不声称人工接管

响应明确“我不是专业救援人员，也无法替你通知任何人”。系统不发消息、不报警、不创建后台任务。

### D6｜不持久化风险标签

只按既有消息契约保存用户原文与本地 assistant response；trace 仅写 `decision/ruleId/local=true`，不写命中文本、号码拨打结果或用户身份扩展字段。

### D7｜会话保持可继续

安全响应不自动结束 session，不产 CLOSING material。用户可以继续发言；每轮重新只看当前输入。

## 3. Classification

封闭结果：

- `NONE`：普通路径；
- `IMMEDIATE_SELF_HARM`：高置信当前/近时自伤风险，走本地安全响应。

正例至少覆盖：明确“我要/准备/决定自杀或伤害自己”、带“现在/马上/今晚”的直接意图、已经吞药/正在割伤等正在实施表达。

负例至少覆盖：普通低落、失败、迷茫、“累死了”等比喻；“我不会/没有想自杀”等否定；“朋友说他想死”等转述；“以前想过”历史表达；新闻/研究/假设讨论。

## 4. Runtime Flow

```text
normalize input -> persist USER -> safety assess
  NONE -> existing provider/memory/tool/material flow
  IMMEDIATE -> persist local ASSISTANT -> trace enum -> return SUCCESS
```

安全分支必须保持 source count=0、provider calls=0、tool proposals=0、material=null，并保留 retry、owner scope 与事务语义。

## 5. Response Contract

固定响应必须：

- 先停止普通整理；
- 建议远离危险物、去有人的地方、联系可信任对象；
- 中国大陆紧急风险优先 `120`/`110`，`12356` 为心理援助；
- 非中国大陆联系当地紧急服务；
- 明确不是专业救援人员，无法代为通知；
- 最多一个直接、可回答的安全问题；
- 不诊断、不说教、不浪漫化、不承诺一直陪伴。

## 6. Verification

- `AgentSafetyPolicyTest`：正负边界、无自由文本 reason、固定文案关键承诺；
- `AgentChatServiceImplTest`：provider/memory/tool/material 均为 0，本地消息持久化，session 可继续；
- C6 合成 safety matrix 与 trace 不变量；
- backend focused/full、frontend 回归（无前端改动时至少 type-check/build）；
- 固定合成真实 provider 小样本仅用于确认 R1 前置分支不调用 provider，以及普通边界仍可调用；真实危机效果不做实验。
