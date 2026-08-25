# Design：Witness Agent Alignment（P4.1）

## 1. Design Intent

P4.1 不是给 Agent 换一句自我介绍，而是把对话控制权还给用户：Agent 可以在场、回应、帮助梳理，但不能把每段表达送进预设采访流程，也不能以“朋友”名义索取持续互动。

设计原则：

1. **用户意图先于 Agent 流程**：先听还是一起梳理，由用户明确选择并可撤回。
2. **问题是受限手段，不是默认动作**：`LISTEN` 不提问；`UNTANGLE` 每轮也至多一个问题。
3. **理解是暂时假设**：先回应已表达内容，允许误解，不把行为固化成人格或人生结论。
4. **旧能力只继承边界，不扩大范围**：工具、素材、记忆、reflection、韧性、时间智能继续受已接受契约约束。

## 2. Current Facts

- system prompt 首句仍是“你是《时光回序》里的一个朋友”。
- 每个非收束 turn instruction 默认“围绕当前引导目标问一个……问题”。
- `AgentStageMachine` 固定顺序为 `EMOTION / CONFUSION / CORE_QUESTION / EXPECTATION`；去空白长度 `<=4` 被视为 evasive，并允许同阶段 reask 一次。
- frontend `AgentChatSheet` 按阶段展示“从此刻的感受开始”“真正想问的事”“未来期待”等标题。
- 既有 finish API 可在 ACTIVE 会话中结束；material、tool confirmation、faithfulness、retry 与 Preview 边界已存在。
- C6 可以验证离线编排不变量与 baseline 变更归属，但不能替代真实模型语言质量人评。

## 3. Recommended Contract For Gate 1

以下 exact contract 只有在用户批准 Gate 1 后才成为实现依据。

### 3.1 Conversation intent

```text
AgentConversationIntent
├─ LISTEN      用户文案：先听我说
└─ UNTANGLE    用户文案：帮我理一理
```

- `LISTEN`：默认 `REFLECT_ONLY`，问题上限为 0；可以承认、复述要点、留白或说明用户可以继续/停下，不给建议清单。
- `UNTANGLE`：正常输入允许 `MAY_ASK_ONE`，问题上限为 1；先回应再问，问题必须具体、可跳过，不要求情绪标签、核心问题或结论。
- 任一 intent 遇到极短输入、明确结束、收束阶段时问题上限为 0。
- intent 属于会话业务状态，不从 Prompt 文本或用户日记内容猜测。

### 3.2 Stage compatibility

- 新增 `AgentStage.WITNESS`；新建 WRITING_GUIDANCE 会话从 `WITNESS` 开始并在普通轮次保持该值。
- `CLOSING`、`ENDED`、`REVIEW` 保持现有语义。
- `OPENING / EMOTION / CONFUSION / CORE_QUESTION / EXPECTATION` 暂留 enum，仅用于读取历史 session/message/trace；P4.1 生产路径不再产生这些值。
- migration 把 ACTIVE WRITING_GUIDANCE session 归一为 `conversation_intent=LISTEN`、`stage=WITNESS`、`stage_reask_count=0`；历史 message/trace stage 不回写。
- 后续独立清理刀才可以删除 legacy enum/column；P4.1 不做破坏性 schema 收缩。

### 3.3 Turn policy

`AgentWitnessTurnPolicy` 为无 IO 的后端纯逻辑：

| 条件 | policy | maxQuestions | nextStage |
|---|---|---:|---|
| 明确结束意图 | `CLOSE` | 0 | `CLOSING` |
| 达到 maxTurns | `CLOSE` | 0 | `CLOSING` |
| `LISTEN` | `REFLECT_ONLY` | 0 | `WITNESS` |
| `UNTANGLE` 且去空白长度 `<=4` | `REFLECT_ONLY` | 0 | `WITNESS` |
| `UNTANGLE` 正常输入 | `MAY_ASK_ONE` | 1 | `WITNESS` |

- 复用现有 finish keyword 集合与 `<=4` 阈值，避免同时引入新的语言分类器。
- `stage_reask_count` 对新 session 恒为 0，不再参与 witness turn。
- policy 与上限进入结构化 trace；用户文本不进入 trace。

### 3.4 API and persistence

#### Start or resume

`POST /api/agent/sessions`

```json
{
  "recordId": 123,
  "purpose": "WRITING_GUIDANCE",
  "conversationIntent": "LISTEN"
}
```

- WRITING_GUIDANCE 缺省 `conversationIntent` 时按 `LISTEN`，兼容旧客户端并采用更少打扰的默认。
- REVIEW_CHAT 不使用该字段；携带时返回参数错误，避免把 P4.1 写作意图误扩到 C3b。
- 恢复 existing ACTIVE WRITING_GUIDANCE 时，显式传入的新 intent 更新会话后返回；不生成第二条 opening、不推进 turn、不调用 provider。

#### Switch intent

`PUT /api/agent/sessions/{sessionId}/intent`

```json
{ "conversationIntent": "UNTANGLE" }
```

- owner-scoped、仅 ACTIVE WRITING_GUIDANCE；幂等。
- 不调用 provider、不推进 turn/stage、不改 message/tool/material。
- 如果存在上一轮未完成、需要重试的用户消息，返回明确冲突；用户先重试或 finish，避免同一 attempt 在两个 policy 下重放。

#### Response and storage

- `AgentSessionVO.conversationIntent`：WRITING_GUIDANCE 必有值，REVIEW_CHAT 为 `null`。
- `agent_session.conversation_intent VARCHAR(24) NULL`：DDL 先允许 REVIEW_CHAT 为 null，并用 CHECK/业务校验保证 WRITING_GUIDANCE 为 LISTEN/UNTANGLE；历史 WRITING_GUIDANCE 回填 LISTEN。
- 选择可为空列而不是全表 `NOT NULL DEFAULT`，避免给 REVIEW_CHAT 伪造并不存在的写作意图。

### 3.5 Prompt assembly

System role：

> 你是《时光回序》里有温度的见证者。你在场，但不替用户解释、决定或完成表达。

共享边界：

- 先回应用户实际说出的内容；可用“我可能理解得不完全”保留误解空间。
- 不自称朋友/伴侣，不承诺一直陪伴、主动关心或最懂用户。
- 不使用“你总是/你就是”，不从单次行为推导人格、阶段或诊断。
- 不强制谈情绪、困惑、核心问题或未来期待；不要求结论。
- 回复与用户表达相称，已有长度、忠实度、时间归属和工具边界继续叠加。

Typed turn instruction 由 policy 生成，不拼接用户文本：

- `REFLECT_ONLY`：回应已听见的内容，留出继续或停下的空间，不提问题。
- `MAY_ASK_ONE`：先回应，再至多问一个具体、可跳过的问题；没有必要就不问。
- `CLOSE`：温和收束，不挽留、不提新问题。

### 3.6 Question-limit enforcement

新增 `AgentQuestionLimitPolicy`，在模型文本通过既有 guardrail 前/后同一 pipeline 内检查：

- 以句末 `？` / `?` 计数，并覆盖连续问号归一化；测试包含中文/英文/混合标点。
- 实际问题数超过 `maxQuestions` 时产生 typed violation `EXCESSIVE_QUESTIONS`。
- 该 violation 可进入既有一次 reflection；reflection instruction 只包含 violation 类型与 0/1 上限，不包含用户或候选文本。
- reflection 后仍越界时，返回 backend-owned、无问题的克制 fallback，并按既有 status/trace 语义标记，不能冒充正常 provider 输出。
- 关系承诺、强迫结论等自然语言质量主要由 Prompt、合成快照与 Gate 3a 人评守护；P4.1 不声称用关键词表完全判定语义。

### 3.7 Mini Program flow

1. 用户点击既有被动入口。
2. 小型选择面板展示两项同等权重的选择，不预选、不自动开启 provider 会话。
3. 选择后才调用 start/resume；Preview 仍在 service boundary fail-closed，真实调用数 0。
4. 浮层 header 展示当前 intent；可从轻量切换控件改变 intent。
5. 标题不再按 stage 显示旅程；分别显示“先听你说”“一起理一理”，结束后为“说到这里已经很好”。
6. “先聊到这里”、关闭、retry、tool/material confirmation 原位保留。

## 4. Compatibility And Migration

- 新增 MySQL 增量脚本并同步 baseline schema / H2 schema；不修改旧 migration。
- 先加 nullable column，再回填 ACTIVE/历史 WRITING_GUIDANCE 为 LISTEN，最后加索引/约束（按当前 MySQL 兼容能力选择 CHECK 或业务断言）。
- 历史阶段 message/trace 是审计事实，不回写；session 当前进度可归一。
- 老前端未传 intent 时得到 LISTEN，而不是继续固定访谈；这是一项刻意的产品安全默认变化，须在 Gate 1 明确批准。
- REVIEW_CHAT 沿用固定 REVIEW stage、无工具、无素材；只继承全局“见证者”角色与问题上限，不获得写作意图字段。

## 5. C6 Evaluation Plan

### 5.1 Fixed synthetic cases

至少覆盖：

- LISTEN + 长表达：0 问题、WITNESS retained；
- LISTEN + 极短回答：0 问题、无 reask；
- UNTANGLE + 正常表达：至多 1 问题；
- UNTANGLE + 极短回答：0 问题、无 reask；
- 任一 intent + 明确停止：CLOSING、0 问题；
- intent switch：不推进 turn、不调用 provider；
- excessive questions：一次 reflection，仍失败则 fallback；
- tool proposal/material/faithfulness/retry/temporal/review chat 既有不变量继续通过。

### 5.2 Baseline attribution

- 不自动刷新 baseline。
- 每个预期变化逐条审查；`baselineNote` 必须以 `P4.1 witness-agent-alignment:` 开头并写明原因。
- checksum 按 C6 机制同步；硬不变量失败不得通过放宽断言解决。
- scripted/mock PASS 只证明编排与守卫，不证明自然语气。

### 5.3 Gate 3a real provider

- 使用最多 6 个固定合成场景，总 provider 调用上限 8（含最多一次 reflection），先执行最多 2 次 canary。
- 观察锚点：witness role、user control、question restraint、brief-answer restraint、uncertainty humility、no forced conclusion。
- 每项只保存等级、case id、版本、次数与结论，不保存 prompt、输入或输出文本。
- identity/config 漂移、超时、调用超限、非合成内容、错误重试或证据泄露立即停止。

## 6. Failure And Privacy Boundaries

- intent 切换失败保留原值，不假装切换成功。
- provider unavailable/failure 沿用 C8；不因 P4.1 新增自动 retry。
- 问题上限 reflection 与 initial 共享 deadline 和 call budget，不新开工具循环。
- 日志、trace、baseline、report、AGENT_LOG 不写用户消息、日记原文、模型回复、记忆片段、prompt 或 secret。
- 真实 provider 样本必须为合成内容；真实用户日记不用于 P4.1 Gate 3a。

## 7. Allowlist And Denylist

### allowlist

- backend Agent intent domain/DTO/VO/session mapper/migration、witness policy、prompt、question guard、pipeline/trace/eval 与直接测试；
- frontend agent service/types/store、record editor entry、AgentChatSheet 与直接测试；
- 本 change artifacts、`.ai/ACTIVE_TASK.md`、append-only `.ai/AGENT_LOG.md`；
- C6 fixtures/baseline 仅在逐项归属后修改。

### denylist

- package/lockfile、deployment、monitoring、admin、SMS、notification center；
- 三个一级 Tab、canonical naming、major visual reconstruction；
- 工具 allowlist 扩面、P4.2 记忆授权/检索重写、图/画像/评分/诊断；
- accepted baseline specs（delta acceptance 前）、archive、冻结蓝图；
- 真实 provider/MySQL/微信（Gate 3 未授权时）。

## 8. 决策记录

### 决策 1：用“见证者”替换“朋友”作为产品角色

1. **面临的选择**：继续沿用朋友式陪伴；改成工具/助手；改成有温度的见证者。
2. **选了哪个 + 为什么**：选见证者。它能保留温度与在场感，又不暗示主动关心、关系发展、长期人格一致性等当前产品无法诚实兑现的承诺。
3. **放弃的代价**：继续叫朋友会制造关系期待；叫工具/助手又会把保存生命现场变成效率任务，丢失产品气质。

### 决策 2：把用户意图做成两个封闭枚举

1. **面临的选择**：完全让模型从文本猜；提供许多细分模式；只提供 LISTEN / UNTANGLE。
2. **选了哪个 + 为什么**：选两个封闭枚举。它们直接对应用户当下最关键的控制权，易懂、可持久化、可测试，也不把表达变成复杂设置。
3. **放弃的代价**：模型猜测会再次夺走控制权；太多模式会增加选择负担并演变成 AI 功能菜单。

### 决策 3：LISTEN 默认零问题，UNTANGLE 才允许至多一问

1. **面临的选择**：两种模式都允许一问；只靠 Prompt 建议少问；由后端给出 0/1 硬上限。
2. **选了哪个 + 为什么**：选后端 0/1 硬上限。它让“先听我说”成为真实可验证的承诺，也让梳理模式保有适量帮助而不变成采访。
3. **放弃的代价**：两种模式都问会让选择名存实亡；只靠 Prompt 无法在 provider 漂移时稳定守住边界。

### 决策 4：新会话使用单一 WITNESS 阶段

1. **面临的选择**：保留四阶段但允许跳过；彻底删除所有阶段；新会话使用 WITNESS，历史值只读兼容。
2. **选了哪个 + 为什么**：选 WITNESS + 历史兼容。它从生产路径移除预设访谈，又保留结束、回看、轨迹和旧数据的兼容边界，不需要大规模 Runtime 重写。
3. **放弃的代价**：保留四阶段会继续暗示预设目标；立即删除全部阶段会破坏历史消息、trace、schema 和 C3b REVIEW 契约。

### 决策 5：极短输入复用 <=4 判定，但翻转处置语义

1. **面临的选择**：新增模型分类器；继续 reask；复用现有确定性阈值并改为零问题。
2. **选了哪个 + 为什么**：选复用阈值、改为零问题。P4.1 需要的是减少追问，不需要新外调或不可解释分类器；现有阈值已有测试基础。
3. **放弃的代价**：模型分类会增加调用、隐私和漂移；继续 reask 正是本刀要修正的问题。

### 决策 6：intent 持久化在 session，并允许显式切换

1. **面临的选择**：每轮从请求携带；只存在前端内存；持久化到 session 并提供 owner-scoped 切换。
2. **选了哪个 + 为什么**：选 session 持久化。恢复、重试和后端编排需要一个权威值；显式切换又允许用户当前意图发生变化。
3. **放弃的代价**：每轮携带会被篡改且难以审计；前端内存会在重载后丢失，造成 Prompt 与 UI 漂移。

### 决策 7：旧客户端缺省为 LISTEN

1. **面临的选择**：缺省 UNTANGLE 复刻旧行为；缺省拒绝请求；缺省 LISTEN。
2. **选了哪个 + 为什么**：选 LISTEN。它兼容旧客户端，又采用最少打扰、最不容易强迫用户表达的安全默认。
3. **放弃的代价**：UNTANGLE 会让固定访谈以兼容名义继续；强制报错会让已发布客户端失效。

### 决策 8：问题超限复用一次 typed reflection

1. **面临的选择**：直接截断问句；超限立即 fallback；复用 C7 一次 reflection，仍失败再 fallback。
2. **选了哪个 + 为什么**：选一次 typed reflection。它有机会保留自然回应，又受既有一次上限、deadline、trace 与 fail-closed 约束。
3. **放弃的代价**：截断容易产生残句和语义破坏；直接 fallback 会在轻微格式越界时过度牺牲自然度。

### 决策 9：不试图用关键词表证明“没有关系越界”

1. **面临的选择**：建立庞大禁止词表；只改 Prompt 不验证；Prompt + 窄硬约束 + 固定人评锚点。
2. **选了哪个 + 为什么**：选第三项。问题数适合确定性判断，关系承诺、抢结论等语义需要真实输出的人评；如实分层比制造伪确定性更可靠。
3. **放弃的代价**：大词表会高误伤且易绕过；只改 Prompt 无法给出回归证据。

### 决策 10：P4.1 不修改跨记录记忆授权

1. **面临的选择**：顺便关闭现有记忆；同时实现 P4.2 单条授权；只调整角色/编排并保持记忆范围。
2. **选了哪个 + 为什么**：选只做 P4.1。冻结蓝图把 P4.2 作为独立刀，记忆授权涉及新的隐私与交互决策，不能借角色调整顺手扩大。
3. **放弃的代价**：顺便关闭可能破坏 C3/C9 已接受行为；合并 P4.2 会让验证边界和用户授权不可审查。

### 决策 11：REVIEW_CHAT 继承角色边界，但不获得写作 intent

1. **面临的选择**：只改 WRITING_GUIDANCE 的角色；给回看也加两种 intent；回看继承 witness tone/问题上限但保持 C3b mode。
2. **选了哪个 + 为什么**：选第三项。产品角色应一致，但回看已有无阶段、无工具、无素材契约；写作意图不应改变其用途模型。
3. **放弃的代价**：只改一边会出现人格不一致；给回看加写作 intent 会无必要扩大 UI/API 与 C3b 范围。

### 决策 12：真实语言质量必须单独预算并人工锚定

1. **面临的选择**：用 scripted PASS 直接宣称气质完成；开放式大量 provider 调用；固定合成小样本 + 总调用硬上限 + 结构化人评。
2. **选了哪个 + 为什么**：选固定小样本。C6 负责可复现编排，真实 provider 只补语言质量盲区，并能控制费用、隐私和漂移。
3. **放弃的代价**：scripted 不能证明自然度；开放式调用会失去预算、复现与证据边界。
