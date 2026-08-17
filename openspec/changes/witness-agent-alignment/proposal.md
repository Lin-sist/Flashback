# Proposal：Witness Agent Alignment（P4.1）

## 1. Summary

P4.1 将 Flashback 当前“朋友式阶段访谈者”的写作引导，收敛为用户主导的温柔见证者：用户先选择“先听我说”或“帮我理一理”，Agent 不再把情绪、困惑、核心问题、期待当作必经采访提纲，也不要求用户得出结论。

本 change 规划以下能力：

- 角色与入口文案统一为“见证者”，不作关系承诺；
- `LISTEN` / `UNTANGLE` 两种会话意图可由用户明确选择和切换；
- `LISTEN` 默认不提问，`UNTANGLE` 每轮最多一个问题；
- 极短回答不触发重复盘问，用户可随时结束；
- DRAFT / SAVED 写作帮助、素材确认、工具确认、忠实度、韧性与时间边界继续成立；
- 通过 C6 离线回归、固定合成场景和单独授权的小样本真人评审验证变化。

本轮只建立 Gate 1 规划工件。规划批准不自动授权业务实现、真实 provider、真实 MySQL、微信真机、push、部署、发布、delta acceptance 或归档。

## 2. Why Now

- P3.2 `data-ownership-foundation` 已归档，`.ai/ACTIVE_TASK.md` 已回到 `IDLE`；冻结蓝图的下一候选为 P4.1。
- P4.1 的硬依赖 P3.1 `present-moment-capture` 与 C6 `agent-eval-framework` 均已归档并进入 accepted specs。
- 当前 `AgentPromptBuilder` 仍自称“一个朋友”；这会暗示主动关心、关系发展、长期陪伴和人格一致性等产品无法诚实兑现的期待。
- 当前 `AgentStageMachine` 固定推进 `EMOTION → CONFUSION → CORE_QUESTION → EXPECTATION`，极短回答仍允许同阶段再问一次；用户只想留下一句话时，也可能被带入预设访谈。
- 当前小程序用阶段标题暴露这条预设路径，进一步强化“你应该被引导到一个问题和期待”的感觉。
- 现有 C2–C9 工具、记忆、护栏、轨迹、评测、reflection、韧性与时间边界已经形成稳定底座，适合在窄范围内调整角色、编排和用户表面，而不是重写 Runtime。

## 3. User Story

> 改前，用户主动打开 Agent 后，会进入由系统预设的情绪、困惑、核心问题和期待阶段；即使只想说一句，也可能继续被问。
>
> 改后，用户先决定“先听我说”或“帮我理一理”。前者让 Agent 以回应和留白为主，不主动发问；后者允许每轮至多一个具体问题。无论哪一种，用户都可以短答、换意图或随时结束，不必交出解释权，也不必得出结论。

## 4. Goals

1. 把 Agent 产品角色从“朋友”改为“有温度的见证者”：在场但不抢表达，理解可以试探但必须允许自己可能理解错。
2. 为 WRITING_GUIDANCE 会话建立 `LISTEN` 与 `UNTANGLE` 两种显式意图，并持久化、返回和允许用户主动切换。
3. 停止为新写作引导会话产生固定阶段序列；新会话使用单一 `WITNESS` 阶段，历史阶段只为兼容读取与迁移保留。
4. 后端按每轮策略硬性约束问题数量：`LISTEN` 为 0；`UNTANGLE` 正常输入至多 1；极短输入、结束或收束为 0。
5. 极短回答不再被视为应当“同阶段再问一次”的回避；Agent 应给用户继续、停下或换方式的空间。
6. 用户在任意进行中状态都能通过既有结束动作收束；结束不挽留、不产生新问题。
7. 保持 DRAFT / SAVED、owner scope、Preview 隔离、工具显式确认、素材显式确认、用户原文忠实度、失败可恢复和内容隐私边界。
8. 扩展 C6 固定合成用例与结构化人评锚点，对任何 baseline 变化写明 `baselineNote=P4.1 witness-agent-alignment: <reason>`，不静默刷新。

## 5. Non-goals

- 把 Agent 做成 AI 朋友、伴侣、人格角色、关系养成或主动关心产品；
- 主动推送、主动召回、连续签到、焦虑驱动留存或“它一直在等你”等拟人关系文案；
- P4.2 的跨记录记忆默认关闭、单条授权或记忆授权 UI；本刀不改变现有检索范围与开关；
- P4.2 的记忆检索重写、向量化、知识图谱、用户画像或长期人格模型；
- 建议清单、诊断、评分、成长报告、情绪轨迹、管理仪表盘；
- 新增工具、扩大工具白名单、取消工具/素材确认或让 Agent 执行封存、删除、位置、附件、封面；
- 修改用户原文、自动把 Agent 回复写入正文、改变 SEALED 不可变性；
- 大规模 backend rewrite、major frontend visual reconstruction、package/lockfile、deployment、monitoring、admin portal、SMS 或生产通知中心。

## 6. Current Capability Classification

### confirmed

- Agent 只由用户主动召唤；WRITING_GUIDANCE 限于 active DRAFT / SAVED，REVIEW_CHAT 限于 UNLOCKED。
- 用户可以关闭浮层，也可以调用既有 finish 端点结束进行中会话；写作素材与工具执行都需要显式确认。
- owner scope、Preview fail-closed、用户原文忠实度、后置护栏、结构化轨迹、C6 eval、一次 reflection、C8 failure taxonomy 与 C9 temporal policy 已存在。
- C6 已能验证编排硬不变量、快照与 `baselineNote` / checksum 归属，且明确离线替身不评价自然语言质量。

### partial

- Prompt 具备克制、短回复、不得诊断等边界，但角色仍写为“朋友”，且轮次指令默认要求提一个问题。
- 状态机对明确结束意图能收束，但正常路径仍固定推进四个阶段，极短回答仍会同阶段再问一次。
- 小程序有“先聊到这里”和恢复能力，但标题直接映射阶段，且没有用户意图选择。
- 现有后置护栏能处理诊断、伪执行、长度与时间越界，但没有按本轮策略校验问题数量。

### planned

- `AgentConversationIntent=LISTEN|UNTANGLE` 的 API、持久化、VO、前端类型与切换契约；
- 新写作引导 `WITNESS` 阶段与历史阶段兼容迁移；
- witness turn policy、问题数量检查与一次 typed reflection / 最终本地降级；
- 见证者 Prompt、开场、入口选择、会话内意图切换与非阶段化标题；
- P4.1 C6 fixtures、baseline 归属和小样本人评锚点。

### unknown

- 当前真实 provider 在见证者 Prompt 下的自然度、克制度、误解承认和“不抢结论”表现；须在 Gate 3a 授权后用固定合成样本小规模人评。
- 微信开发者工具/真机上入口选择、键盘、切换和结束动作的节奏与可读性；须 Gate 3b 人工验证，build 不能替代。
- 当前真实 MySQL 是否存在未结束的历史写作引导阶段值及其分布；须 Gate 3c 只读聚合 preflight，不读取会话或日记原文。

### out_of_scope

- 关系型 AI、P4.2 跨记录记忆授权、图框架重写、工具扩面、分析/建议产品、生产 SLA 与发布工作。

## 7. Proposed Scope

### 7.1 Backend contract

- 新增 `AgentConversationIntent`：`LISTEN`、`UNTANGLE`；
- `POST /api/agent/sessions` 对 WRITING_GUIDANCE 接受 `conversationIntent`，缺省按 `LISTEN` 处理以保护旧客户端；REVIEW_CHAT 携带该字段时拒绝；
- 新增 owner-scoped `PUT /api/agent/sessions/{sessionId}/intent`，仅 ACTIVE WRITING_GUIDANCE 可切换，不推进轮次、不调用 provider、不执行工具或生成素材；
- session VO 返回 `conversationIntent`；数据库 `agent_session.conversation_intent` 使用 nullable `VARCHAR(24)`，WRITING_GUIDANCE 仅允许 `LISTEN|UNTANGLE`，REVIEW_CHAT 保持 `null`；
- 旧 active 写作会话在迁移或首次恢复时归一为 `LISTEN + WITNESS + stage_reask_count=0`，历史 message stage 不改写。

### 7.2 Orchestration and guardrails

- 新写作引导使用 `WITNESS`，不再生成固定四阶段推进；`CLOSING / ENDED / REVIEW` 语义保持；
- `AgentWitnessTurnPolicy` 根据 intent、极短输入、结束意图与轮次上限给出 `REFLECT_ONLY / MAY_ASK_ONE / CLOSE`；
- 复用既有去空白长度 `<=4` 的极短判定，但语义由“允许 reask”改为“禁止继续提问”；
- Prompt 和 turn instruction 显式携带策略；后置检查按中文/英文问号与问句边界验证 0/1 上限；越界进入一次 typed reflection，仍越界则使用不含问题的本地克制降级；
- 轨迹只记录 intent、turn policy、question limit、violation 与 outcome，不记录用户/模型原文。

### 7.3 Prompt and product temperament

- system role 改为“《时光回序》里有温度的见证者”；
- 不声称是朋友、最懂用户、一直陪伴或比现实关系更可靠；
- 先回应用户实际表达，再决定是否留白或问一个问题；不将一次行为上升为人格，不用“你总是”，不强求情绪标签、核心问题或未来期待；
- 当前表达优先，历史记忆若由既有 C3/C9 路径出现，仍须带时间距离、不抢解释权；本刀不改变检索权限。

### 7.4 Mini Program

- 在真正开启 provider 会话前提供“先听我说 / 帮我理一理”两项，不默认诱导第二项；
- 会话浮层显示当前意图，并允许用户主动切换；切换失败保持原意图并明确提示；
- 移除阶段旅程标题，改为“先听你说 / 一起理一理 / 说到这里已经很好”；
- 保留“先聊到这里”、关闭、重试、工具确认、素材确认和 Preview 隔离；不增加一级 Tab 或独立 AI 页面。

## 8. Spec Delta Map

- `agent-runtime`：写作引导从固定阶段改为 witness intent/turn policy，问题上限、角色与 guardrail/trace 契约；
- `backend-core`：intent 的 API、owner scope、持久化、迁移与后端权威切换；
- `miniapp-core`：入口选择、非阶段化浮层、切换、极短输入与结束交互；
- `v2-product-scope`：以见证者替换朋友/陪伴关系叙事，并保持不代决、不分析边界；
- `agent-collaboration`：C6 baseline 归属、固定合成样本、独立真实 provider/真机/MySQL Gate 与隐私证据。

## 9. Evidence Plan

- 文件级：artifacts、delta operation、Requirement/Scenario、路径 allowlist、链接、ACTIVE_TASK、隐私与凭证扫描；
- 离线：intent API/persistence、legacy migration、witness policy、0/1 问题检查、reflection/fallback、finish/retry/tool/material/owner/Preview 回归；
- C6：固定合成矩阵覆盖两种 intent、极短回答、显式停止、未知/误解、工具/素材和 review chat；不削弱硬不变量；
- build：backend focused/full Maven，frontend type-check，standard 与 Preview mp-weixin build；
- Gate 3a：最多 6 个固定合成场景、总 provider 调用硬上限 8（含 reflection），先最多 2 次 canary；结构化人评，不保存样本文本；
- Gate 3b：微信开发者工具/真机验证入口选择、切换、短答、结束、失败、Preview；
- Gate 3c：真实 MySQL 只读聚合 preflight 与 migration/恢复验证，只用合成会话，不读取真实内容。

## 10. Responsibility And Gate State

- 开工锚点：`0aea558`。
- 提交责任：用户于 2026-08-17 明确授权 Agent 完成本地 stage/commit；不含 push。
- 规划期外部调用预算：0；未连接真实 provider、MySQL、微信或其他外部服务。
- OpenSpec CLI：当前 shell 未安装；本轮采用仓库既有文件级 scaffold，CLI validation 记为 `SKIPPED`。
- Gate 1：用户于 2026-08-12 明确“批准决策”，批准 proposal、design、tasks、五份 delta 与设计决策 1–12。
- Gate 2：用户于 2026-08-12 明确“可以进入实现”，授权按 `tasks.md` 修改业务代码。
- Gate 2 状态：离线实现与 scripted/build 回归已完成；等待用户审查，未将其描述为真实 provider、MySQL 或微信证据。
- Gate 3a/3b/3c、delta acceptance 与归档：用户于 2026-08-17 明确授权。
- Gate 3a 状态：8 个固定合成场景 / 8 次真实 provider 调用 PASS，结构化人评六项 PASS；调用预算已用尽。
- Gate 3b 状态：首次微信 automation 连接失败，按 fail-stop 边界未自动重试；交互矩阵未执行。
- Gate 3c 状态：MySQL80 服务启动被系统权限拒绝，migration 未执行、数据库未改动。
- push、PR、部署与发布：未授权。
