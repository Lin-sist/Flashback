# 《时光回序》Flashback｜迭代蓝图（Iteration Blueprint）· v1.1

> 文档性质：长期迭代的母文档 / 宪章，不是可执行 OpenSpec change  
> 状态日期：2026-07-27  
> 状态：**已冻结 v1.1**（终验通过；方向层生效）  
> 作者：人机协作（用户主导设计决策，Claude 执笔）  
> **冻结含义**：批准 C1–C5 方向、依赖与气质约束。  
> **仍不授权直接改业务代码**——实施须新建 Type C change，并走 `AGENTS.md` 三道闸门与 `type-c-checklist.md`。

---

## 0. 给 Agent 的阅读与执行约定

### 0.1 优先级

1. `AGENTS.md` Non-Negotiable → 本蓝图方向 → OpenSpec active change → baseline specs → 代码 → 旧 Docs。
2. 本蓝图**不授权直接改业务代码**。正式实施某一项时，须在 `openspec/changes/<change-id>/` 建 proposal/design/tasks/delta，走三道闸门（`AGENTS.md` Gates）。
3. 本蓝图与 `AGENTS.md` 冲突时，以 `AGENTS.md` 为准。

### 0.2 Type 分级适用

| 类型 | 与蓝图的关系 |
|---|---|
| **Type A** | 可引用蓝图做规划讨论、现状扫描。不改文件。 |
| **Type B** | 不需要蓝图授权。按 `AGENTS.md` Type B 流程走。 |
| **Type C** | 蓝图只提供方向与意图卡片。实施须建 OpenSpec change，走三道闸门。开工清单见 `Docs/agent-iteration/workflow/prompt-snippets/type-c-checklist.md`。 |

### 0.3 一次一个 active change

同时最多一个 active Type C。上一个 change 验收归档（`ACTIVE_TASK=IDLE`）后，才可开启下一个。  
执行顺序与调整规则见 §3.2。

### 0.4 诚实性要求

- 意图卡片中的「现状事实」必须可核对（代码路径 / OpenSpec 引用），禁止空话。
- 标注为「待确认」的条目，不得在 change proposal 中当作已确认使用。
- 能力五态：`confirmed / partial / planned / out_of_scope / unknown`。禁止把 `unknown` 写成 `confirmed`。

### 0.5 外调与隐私

- AI provider、对象存储等真实外调须在 change proposal 中披露预算并单独授权。
- 用户日记原文是高敏数据，不得进入普通日志、telemetry、tracked files。
- AI API key、provider secret 只能存在于 backend-side config。

### 0.6 与治理文件的交叉引用

| 文件 | 蓝图中的角色 |
|---|---|
| `AGENTS.md` | 硬规则（Type/Gates/Handoff/Non-Negotiable）；与本文冲突时 AGENTS 优先 |
| `Docs/agent-iteration/workflow/prompt-snippets/type-c-checklist.md` | 每个 C1–C5 change 开工时的操作清单 |
| `Docs/agent-iteration/workflow/vibecoding-playbook.md` | 六步闭环协作方法论 |
| `Docs/agent-iteration/workflow/agent-control-model.md` | 四层控制架构与三道闸门展开 |
| `Docs/agent-iteration/项目初始分析.md` | 产品方向评估草稿（**不是**已批准 scope；P0 表不得直接当执行序列） |
| M1 / M3 历史目录 | 历史参考；**不是** active 实现源；清理属旁支治理，不在 Agent 主线 |

---

## 1. 迭代总方向

### 一句话总目标

**将 Flashback 从「单轮 prompt → response 的 AI 工具调用」演进为「被动召唤、深度共情、有记忆的陪伴式写作 Agent」。**

### 叙事定位

| 项目 | 证明什么 |
|---|---|
| RAG 项目 | AI 知道什么：检索、分块、可信、评测 |
| **Flashback** | **AI 能做什么：在克制产品中做共情式 Agent（对话、工具、记忆、护栏、可观测）** |

两个项目覆盖 AI 应用层两大核心方向：知识密集型（ToB）与交互密集型（ToC）。

### 主干依赖链

```text
M4 归档（2026-07-27 已完成；archive/2026-07-27-m4-real-capability-completion；ACTIVE_TASK=IDLE）
  → C1: Agent Runtime MVP（对话状态机 + 多轮写作引导 + 最小护栏内嵌）
  → C2: Agent Tool Calling（草稿/标签/封存等工具白名单）
  → C3: Agent Memory & Review（历史记录检索 + 友人回看对话 + 跨记录关联）
  → C4: Agent Guardrails Hardening（系统化多层防御 + 边界用例 + 违规降级）
  → C5: Agent Observability（决策链路 thought→action→observation 可查询）
```

**依赖规则见 §3.2。**

---

## 2. 作者已确认的决策

以下决策来自 M4 用户对话、`AGENTS.md` 禁止项、蓝图编写讨论，已由用户明确确认。

### 2.1 已确认

| # | 决策 | 来源 |
|---|---|---|
| D1 | M4 **已于 2026-07-27 正式归档**（`openspec/changes/archive/2026-07-27-m4-real-capability-completion/`；delta 已接受进 baseline `backend-core` / `miniapp-core` / `v2-product-scope`；`ACTIVE_TASK=IDLE`）。残留仅 timeline MySQL `EXPLAIN` carry-over（MySQL 服务当时无法启动），不阻塞归档，不重开 M4 | 2026-07-27 归档 |
| D2 | Agent 气质：共情型朋友——不太热情也不冷漠，主动和它聊一聊时，它永远是最懂你的朋友 | 蓝图编写讨论 |
| D3 | Agent 主动性：被动召唤型——不主动弹窗/推送，只在用户明确操作时参与，但参与时展现深度共情和记忆能力 | 蓝图编写讨论 |
| D4 | 架构方向：Backend 侧 Agent Runtime（Spring Boot 托管状态机/Tool/Memory/对话） | 蓝图编写讨论 |
| D5 | 拆分粒度：每个能力独立 Type C change，一次只 ACTIVE 一个 | 蓝图编写讨论 |
| D6 | Runtime MVP 场景：先做「写下此刻」多轮引导；「友人回看」后置到 Memory change | 蓝图编写讨论 |
| D7 | Memory 初期：简单检索（MySQL 全文 + 语义摘要匹配），后续可升级；**不做第二套企业 RAG / 向量中台** | 蓝图编写讨论 |
| D8 | 回看交互：保留现有结构化摘要 + 新增「和它聊聊」多轮对话入口 | 蓝图编写讨论 |
| D9 | 跨记录关联合并到 Memory change 中 | 蓝图编写讨论 |
| D10 | 友人回看对话合并到 Memory change 中 | 蓝图编写讨论 |
| D11 | Eval 定位：Agent 决策链路可观测（工程向，不面向用户） | 蓝图编写讨论 |
| D12 | 蓝图不含治理卡片——治理已通过 workflow 文档解决 | 蓝图编写讨论 |
| D13 | 三 Tab 不变：首页、时光轴、个人中心 | `AGENTS.md` |
| D14 | 用户可见命名：我的记录、时光轴、时间回看 | `AGENTS.md` |
| D15 | 封存后 location/attachments/cover 不可变 | `AGENTS.md` |
| D16 | secret 仅 backend | `AGENTS.md` |
| D17 | 真实路径不得 mock success 冒充真实成功 | `AGENTS.md` |
| D18 | 不做大规模 backend rewrite | `AGENTS.md` |
| D19 | C1 必须内嵌最小护栏（不诊断 / 不覆写 / 建议不代决 / 被动召唤 / 输出克制）；C4 是系统化 hardening 而非「从零第一次有护栏」 | v1.1 修订 |

### 2.2 待确认

| # | 待确认事项 | 影响范围 | 决策时机 |
|---|---|---|---|
| P1 | 当前 DeepSeek / OpenAI-compatible provider 是否支持 Function Calling；若不支持，Runtime 是否用 prompt 模拟还是切换 provider | C1 / C2 | C1 proposal 阶段 |
| P2 | Agent 对话状态持久化方式（候选：MySQL 新表 / Redis session / 内存 session） | C1 | C1 design 阶段 |
| P3 | Tool Calling 白名单具体范围（哪些操作允许 Agent 调用，哪些禁止） | C2 | C2 proposal 阶段 |
| P4 | Memory 检索的具体实现（MySQL FULLTEXT / LIKE / 外部搜索引擎） | C3 | C3 design 阶段 |
| P5 | 友人回看对话的 UI 交互形式（浮窗 / 新页面 / 页内展开） | C3 | C3 proposal 阶段 |
| P6 | Guardrails hardening 的实现方式（system prompt 约束 / 后置过滤 / 两者结合） | C4 | C4 design 阶段 |
| P7 | 可观测数据的存储与查询方式（日志文件 / 数据库 / 管理界面） | C5 | C5 design 阶段 |

---

## 3. change 序列总览

### 3.1 主线序列

| 顺序 | 建议 change-id | 一句话目标 | Type | 硬依赖 |
|---|---|---|---|---|
| **C1** | `agent-runtime-mvp` | Backend Agent Runtime 基底 + 「写下此刻」多轮写作引导 + 最小护栏内嵌 | C | M4 归档（已完成） |
| **C2** | `agent-tool-calling` | Agent 可调用工具（草稿/标签/封存等），对话过程自然产生行动 | C | C1 |
| **C3** | `agent-memory-and-review` | 基于历史记录的 Memory 检索 + 友人回看多轮对话 + 跨记录关联 | C | C1 |
| **C4** | `agent-guardrails-hardening` | 系统化多层护栏 hardening：边界用例、违规降级、防御深度 | C | C1（推荐 C3 后） |
| **C5** | `agent-observability` | Agent 决策链路 thought→action→observation 可查询（工程向） | C | C1 |

### 3.2 依赖规则与执行顺序

```text
M4 归档（已完成）
  │
  ▼
 C1 (Runtime MVP + 最小护栏)
  │
  ├──▶ C2 (Tool Calling)
  │
  ├──▶ C3 (Memory & Review)
  │
  ├──▶ C4 (Guardrails Hardening)  ← 推荐 C3 之后
  │
  └──▶ C5 (Observability)
```

**依赖规则（消除歧义）：**

- **硬依赖**：C2 / C3 / C4 / C5 均硬依赖 C1 完成。C2 / C3 / C4 / C5 彼此之间无硬依赖。
- **默认执行顺序**：C1 → C2 → C3 → C4 → C5（一次只 ACTIVE 一个）。
- **可调整**：若 C1 联调中出现 Agent 气质越界或行为失控问题，允许将 **C4 前移至 C2 之前**。顺序调整须更新本文 §7 修订记录。
- 所有 change 不得并行：上一个验收归档后才可开启下一个。

### 3.3 旁支（明确不在主线）

以下事项不在 Agent 化主线中，但可能作为独立 change 在未来提出：

| 事项 | 原因 |
|---|---|
| 设置页增强 | 与 Agent 无关，M4 已明确延后 |
| 生产通知中心 / SMS | 需要微信订阅消息等基础设施，复杂度独立 |
| Admin portal | 与用户端 Agent 无关 |
| 完整 RAG 中台化 Memory | 与 RAG 项目重复，Flashback Memory 叙事重心是「Agent 如何使用记忆」 |
| 通用多租户 Agent 平台 | 过度工程化，不符合个人项目定位 |
| Major 视觉重建 | `AGENTS.md` 禁止 |
| M1 / M3 未归档目录清理 | 旁支治理 Type B；与 Agent 主线脱钩 |
| MySQL `EXPLAIN` carry-over | Type B 补证据；不阻塞 Agent 主线 |

---

## 4. 每个 change 的意图卡片

### C1 · `agent-runtime-mvp`

**Backend Agent Runtime 基底 + 「写下此刻」多轮写作引导 + 最小护栏内嵌**

#### 现状事实

- 当前所有 AI 能力均为单轮 `prompt → response` 模式（`AiServiceImpl.java`）：构造 prompt → 调用 `invokeChatCompletion` → 解析 JSON → 返回 VO → 结束。**无对话状态、无多轮交互**。`confirmed`
- 现有 `generateWritingPrompts` 一次性生成写作提示列表，用户看到后自主选择。**不是**引导式对话。`confirmed`
- 后端已有 AI provider 适配层（MOCK / DEEPSEEK / OPENAI_COMPATIBLE）。`confirmed`
- Provider 是否支持 Function Calling：`unknown`（待 C1 proposal 调研）。C1 多轮对话不依赖 FC；FC 放到 C2。
- 对话状态持久化方案：`unknown`（候选：MySQL 新表 / Redis session / 内存 session），**待 C1 design 阶段确认**。蓝图不冻结技术选型。

#### 目标

1. 在 Spring Boot 后端建立 Agent Runtime：对话状态机、轮次管理、上下文维护。
2. 实现第一个用户场景：用户新建记录时，Agent 以多轮对话形式引导用户展开内容（从情绪 → 困惑 → 核心问题 → 期望，逐步引导而非一次性提问）。
3. 前端提供对话式 UI 入口——**被动触发**：用户主动点击「让它帮我写」或类似入口，**不弹窗、不自动展开**。
4. **内嵌最小护栏**（摘要级，不等待 C4 才首次出现护栏）：
   - 不诊断：system prompt 明确禁止心理诊断和医学建议
   - 不覆写：不篡改或替换用户原文
   - 建议不代决：不代替用户执行封存/解锁/删除
   - 被动召唤：不主动推送或弹窗
   - 输出克制：回复长度与用户表达相称
5. 对话可随时中断，已产生的内容可保留为草稿素材。

#### 用户故事

- **改前**：用户新建记录后面对空白页面，点击 AI 写作提示得到一组静态提示词列表，仍然不知道从何写起。Agent 不了解用户此刻状态。
- **改后**：用户点击「让它帮我写」后，Agent 以温和的问题引导对话——"今天是什么让你想写下这一刻？"——用户回答后 Agent 继续追问，逐步帮用户展开思绪。对话可随时中断，已产生的内容可保留为草稿素材。Agent 不会做心理诊断，回复简洁温暖。

#### 非目标 / out_of_scope

- 不在此 change 中实现 Tool Calling（留 C2）。
- 不在此 change 中实现 Memory / 历史记录检索（留 C3）。
- 不做系统化 Guardrails hardening（多层防御 / 边界用例测试 / 违规降级——留 C4）；C1 仅做 system prompt 级最小护栏。
- 不做主动推送 / 弹窗。
- 不改三 Tab 结构。
- 不做大规模 backend rewrite——增量新增 Agent 模块。
- 不做前端视觉大改——对话 UI 保持克制。

#### 验收证据类型

- [ ] 后端 Agent 状态机单元测试
- [ ] 多轮对话 API 集成测试（mock provider）
- [ ] 真实 AI provider 联调（外调授权后）
- [ ] 微信小程序手验：对话流程、中断恢复、草稿素材保留
- [ ] 最小护栏验证：至少验证不诊断、不覆写、输出克制三项的基本行为

#### 关键风险

| 风险 | 缓解 |
|---|---|
| Provider 不支持 Function Calling | C1 仅需多轮对话，不需要 FC；FC 在 C2 调研 |
| 对话状态持久化方案复杂 | MVP 可用内存/session，design 阶段决策（P2 待确认） |
| 前端对话 UI 与现有记录编辑器冲突 | 对话 UI 可作为独立入口/浮层，不改编辑器主路径 |
| 最小护栏仅靠 system prompt，防御深度不足 | C1 接受此风险；系统化 hardening 在 C4 |

---

### C2 · `agent-tool-calling`

**Agent 可调用工具，对话过程自然产生行动**

#### 现状事实

- C1 建立了 Agent Runtime 和多轮对话能力。`planned`（依赖 C1 完成）
- 当前草稿保存、标签添加、封存等操作均为用户手动触发的前端操作 → 后端 API 调用。`confirmed`
- Agent 目前无法在对话中触发任何后端操作。`confirmed`

#### 目标

1. 定义 Tool 白名单：Agent 可调用哪些工具、禁止调用哪些工具。
2. 实现 Tool 执行层：Agent 在对话中识别需要行动时，调用后端 Tool 完成操作（如自动保存草稿、建议添加标签）。
3. Tool 调用结果反馈到对话上下文中。

#### 用户故事

- **改前**：Agent 和用户聊完后说"你可以给这条记录加个'焦虑'标签"，但用户需要退出对话、找到标签功能、手动操作。
- **改后**：Agent 说"我觉得这条记录和你的工作焦虑有关，要不要加个'工作焦虑'标签？"——用户同意后，Agent 直接调用 Tool 完成标签添加，用户无需离开对话。

#### 非目标 / out_of_scope

- Agent 不得**代替**用户做封存/解锁等重要决策，只能**建议**（C1 最小护栏延续）。
- 不开放危险操作（如批量删除记录、修改已封存记录）。
- 不实现支付、通知等与 Agent 无关的 Tool。
- 不改写用户原文。

#### 验收证据类型

- [ ] Tool 白名单文档与权限审查
- [ ] Tool 执行的后端集成测试
- [ ] 真实 AI provider Function Calling 联调（外调授权后；若 provider 不支持 FC 则验证 prompt 模拟方案）
- [ ] 微信小程序手验：对话中触发 Tool → 确认操作 → 结果反馈

#### 关键风险

| 风险 | 缓解 |
|---|---|
| Provider Function Calling 能力不确定 | 备选方案：prompt 模拟 + 结构化输出解析 |
| Tool 执行失败处理 | 需要优雅降级：告知用户失败而非静默 |
| Agent 越权执行危险操作 | 白名单机制 + 重要操作须用户确认 |

---

### C3 · `agent-memory-and-review`

**基于历史记录的 Memory 检索 + 友人回看多轮对话 + 跨记录关联**

#### 体量说明与可选拆分退路

C3 包含三个子能力（Memory 检索、友人回看、跨记录关联），体量较大。默认作为单一 change 实施。**若实现过程中发现体量过重**，允许拆分为：
- `agent-memory-retrieval`：Memory 检索 + 写作引导中的记忆注入
- `agent-review-chat`：友人回看多轮对话 + 跨记录关联

拆分须新建 change 并更新本文 §7 修订记录。**不做第二套企业 RAG / 向量中台**——与 RAG 项目叙事分工。

#### 现状事实

- 当前 AI 能力无记忆——每次调用独立，不检索用户历史记录。`confirmed`
- 现有 `summarizeRecord` 已对记录生成结构化摘要（情绪/困惑/核心问题/期望），这些摘要可作为 Memory 检索的索引。`confirmed`
- 时间回看（解锁后）目前是一次性生成的结构化摘要页面。`confirmed`
- 后端 MySQL 存储用户所有记录数据。`confirmed`

#### 目标

1. 实现基于历史记录的 Memory 检索：Agent 能找到与当前对话相关的历史记录（初期用 MySQL 简单检索，后续可升级）。
2. 在「写下此刻」对话中注入 Memory 上下文——Agent 能说"三个月前你也写过类似的焦虑"。
3. 实现「友人回看」多轮对话：解锁后，用户可在现有结构化摘要基础上点击「和它聊聊」，与 Agent 进行基于该记录和历史的多轮对话。
4. 跨记录关联：Agent 在对话中主动关联相关历史记录。

#### 用户故事

- **改前**：Agent 每次都像初次见面——用户写了三十条关于工作焦虑的记录，Agent 完全不记得。解锁回看时只能看到一段固定的结构化摘要。
- **改后**：用户写新记录时提到工作压力，Agent 说"我记得你 3 月份也写过类似的感受，那时候你说是因为项目截止日期。这次也是类似的原因吗？"。解锁回看时，用户点击「和它聊聊」，Agent 像老朋友一样说"那时候你写下这些的时候，一定很不容易吧。现在回过头来看，你觉得当时的担心后来怎么样了？"

#### 非目标 / out_of_scope

- 不建完整的向量数据库 / RAG pipeline——与 RAG 项目叙事分工。
- 不做情绪轨迹可视化 dashboard——与 `AGENTS.md` 禁止项一致。
- 不做用户画像 / 标签自动归类。
- Memory 不跨用户——严格用户隔离。

#### 验收证据类型

- [ ] Memory 检索后端测试（关键词/时间/摘要匹配）
- [ ] 对话中注入 Memory 的 Agent 行为测试
- [ ] 友人回看多轮对话端到端测试
- [ ] 跨记录关联的相关性验证
- [ ] 微信小程序手验：写作引导 + Memory 注入 + 友人回看对话

#### 关键风险

| 风险 | 缓解 |
|---|---|
| 简单检索相关性不足 | 初期可接受；后续独立 change 升级检索能力 |
| Memory 上下文过长导致 token 消耗 | 限制注入的历史记录数量和摘要长度 |
| 跨记录关联准确性 | 基于已有结构化摘要的关键词匹配，而非复杂语义 |
| 用户隐私——检索结果是否可能泄露 | Memory 严格用户隔离；日记原文不进日志 |
| C3 体量过重 | 见「可选拆分退路」；实施中发现过重则拆分 |

---

### C4 · `agent-guardrails-hardening`

**系统化多层护栏 hardening：边界用例、违规降级、防御深度**

> C1 已内嵌最小护栏（system prompt 级）。C4 不是「从零第一次有护栏」，而是在已有基础上做系统化加固。

#### 现状事实（C4 启动时预期）

- C1 已通过 system prompt 实现最小护栏：不诊断、不覆写、建议不代决、被动召唤、输出克制。`planned`（依赖 C1 完成）
- C1 最小护栏仅为 system prompt 单层防御，无后置检查、无边界用例测试、无违规降级机制。`planned`
- `AGENTS.md` 已禁止「complex AI scoring / diagnosis / dashboard」。`confirmed`
- 产品初心要求「安静、私密、克制、温柔」。`confirmed`

#### 目标

1. 将 C1 的 system prompt 单层护栏升级为**多层防御**：system prompt 约束 + 后置输出检查 + 违规兜底回复。
2. 建立边界用例测试集：
   - 诊断性输入（用户描述疑似心理问题 → Agent 应共情而非诊断）
   - 篡改尝试（prompt injection 试图让 Agent 修改用户原文）
   - 过长输出（用户写两行 → Agent 不应回复长篇大论）
   - 代决尝试（用户问「帮我封存吧」→ Agent 只能建议确认）
3. 定义 Guardrails 违反时的降级行为（检测到越界 → 回退到安全兜底回复）。
4. 为护栏规则产出可维护的配置/文档，而非散落在 system prompt 中。

#### 用户故事

- **改前**（C1 完成后）：Agent 有 system prompt 约束不做诊断，但偶尔在边界场景下仍然滑入「你可能是焦虑症」式的回复，且开发者无法系统性地发现和修复这些越界。
- **改后**：Agent 即使收到诱导性输入也能保持共情而非诊断——后置检查会拦截含诊断关键词的输出并替换为安全兜底回复。开发者可以通过边界用例测试集持续回归验证护栏有效性。

#### 非目标 / out_of_scope

- 不做复杂的 AI 内容审查系统。
- 不做用户行为风控。
- 不做 Agent 行为的自动化评分 dashboard。
- 不重复 C1 最小护栏的基本功能——C4 是在其之上的 hardening。

#### 验收证据类型

- [ ] 多层防御机制实现与设计决策记录
- [ ] 边界用例测试集（至少覆盖上述 4 类场景）
- [ ] 违规降级行为验证
- [ ] 微信小程序手验：边界场景对话

#### 关键风险

| 风险 | 缓解 |
|---|---|
| LLM 固有不可控性——Guardrails 无法 100% 阻止越界 | 多层防御 + 兜底回复，降低概率而非追求绝对 |
| 过度限制导致 Agent 「无话可说」 | 护栏定义正向行为（可以做什么），而不仅是负面清单 |
| Guardrails 实现方式的选择 | design 阶段评估 system prompt + 后置过滤 + 两者结合（P6 待确认） |

---

### C5 · `agent-observability`

**Agent 决策链路 thought→action→observation 可查询（工程向）**

#### 现状事实

- 当前 AI 调用只有基本的 API 请求/响应日志。`confirmed`
- Agent 决策过程对开发者不透明——无法追溯 Agent 为何选择某个回复或工具。`confirmed`

#### 目标

1. 记录 Agent 每次交互的决策链路：thought（Agent 的推理）→ action（选择的行动/工具）→ observation（执行结果）。
2. 提供开发者可查询的接口或日志格式（不面向终端用户）。
3. 为后续的 Agent 行为优化和调试提供基础。

#### 用户故事

- **改前**：Agent 给了一个奇怪的回复，开发者只能看到 API 请求和响应，不知道 Agent 的推理过程。
- **改后**：开发者可以查询决策链路日志——Agent 检索了 3 条历史记录、基于情绪关键词选择了共情回复模板、输出长度被 Guardrails 限制在 100 字以内——清楚地看到问题出在哪一步。

#### 非目标 / out_of_scope

- 不面向终端用户展示（不破坏「朋友」的交互感）。
- 不做实时监控告警系统。
- 不做复杂的 AI 质量评估 dashboard。
- 不做 A/B testing 框架。

#### 验收证据类型

- [ ] 决策链路日志格式定义与实现
- [ ] 查询接口或日志解析工具
- [ ] 至少 3 个场景的链路追踪示例
- [ ] 验证日记原文不出现在可观测日志中

#### 关键风险

| 风险 | 缓解 |
|---|---|
| 日志量膨胀 | 可配置采样率；默认记录关键步骤 |
| 用户隐私泄露到日志 | 可观测数据严格脱敏；日记原文哈希或截断处理 |
| 存储方案复杂度 | MVP 可用结构化 JSON 日志文件，不需要专用存储 |

---

## 5. spec delta 建议落点

| Change | backend-core | miniapp-core | v2-product-scope | agent-collaboration | 新建 spec |
|---|---|---|---|---|---|
| C1 `agent-runtime-mvp` | Agent API 端点、对话状态模型 | 对话 UI 组件、入口交互 | 多轮引导产品行为 | Agent 执行规范 | ✦ `agent-runtime` |
| C2 `agent-tool-calling` | Tool 执行层、白名单机制 | Tool 确认 UI | — | Tool 安全约束 | — |
| C3 `agent-memory-and-review` | Memory 检索 API、对话上下文注入 | 友人回看对话 UI、「和它聊聊」入口 | 友人回看产品行为 | Memory 隐私约束 | — |
| C4 `agent-guardrails-hardening` | 多层防御实现 | — | Agent 气质约束 | Guardrails 规则 | — |
| C5 `agent-observability` | 可观测日志、查询接口 | — | — | 可观测规范 | — |

> ✦ 标记建议新建的 spec 模块。C1 可能需要新建 `openspec/specs/agent-runtime/spec.md` 来承载 Agent 核心契约。具体在 C1 proposal 阶段决定。

---

## 6. 产品初心与 Agent 气质约束

### 6.1 产品初心（不可违背）

《时光回序》帮助用户写下当下的情绪、困惑、期待、犹豫与生活片段。未来回看不是产品唯一承诺；它更像是把回答权交给时间，让未来的自己重新理解写下这一刻的自己。

**产品气质：安静、私密、克制、温柔，并带有时间感。**

### 6.2 Agent 气质定义

Agent 是一个**共情型朋友**：

- **不热情也不冷漠**——不会主动弹出来打招呼，但你找它聊天时，它总是在的。
- **最懂你的朋友**——它记得你写过的东西，理解你反复出现的情绪，但不会拿这些来"分析"你。
- **被动召唤**——Agent 只在用户明确操作时参与（点击、触发、发消息）。不弹窗、不推送、不主动干预。
- **输出克制**——回复简洁温暖，长度与用户表达相称。不写长篇大论，不显得比用户"更懂"用户。
- **建议不代决**——可以说"要不要把这条封存？"，但永远不会自动封存。

### 6.3 Agent 绝对禁止

| 禁止 | 原因 |
|---|---|
| 心理诊断 / 医学建议 | 产品不是心理咨询工具 |
| 篡改或覆写用户原文 | 尊重用户原始表达 |
| 代替用户做重要决策（封存/解锁/删除） | Agent 是朋友不是管家 |
| 效率仪表盘 / 诊断 dashboard | 与产品气质冲突 |
| 社交动态 / 分享功能 | 私密性是产品基石 |
| 将日记原文写入日志/telemetry | 隐私保护 |
| 主动推送 / 弹窗 / 未请求的分析 | 被动召唤原则 |
| 话痨——回复比用户写的还长 | 克制原则 |

### 6.4 Agent 应该做到

| 行为 | 示例 |
|---|---|
| 温和引导 | "今天是什么让你想写下这一刻？" |
| 共情回应 | "听起来你最近过得不太容易。" |
| 记忆关联 | "我记得你上个月也写过类似的感受。" |
| 尊重沉默 | 用户不想继续聊时，优雅地结束——"好的，这些已经很好了。" |
| 行动建议 | "要不要给这条记录加个标签？" |

---

## 7. 修订记录

| 版本 | 日期 | 状态 | 说明 |
|---|---|---|---|
| v1 | 2026-07-26 | 草案 | 初版蓝图，基于 grill-me 讨论的设计决策编写 |
| v1.1 | 2026-07-27 | 草案修订 | **相对 v1**：① M4 真相对齐 ② 依赖规则去歧义 ③ C1 最小护栏 + C4 hardening ④ C3 拆分退路 ⑤ C1 意图卡片补强 ⑥ §0.6 治理交叉引用 ⑦ 旁支含 M1/M3 与 EXPLAIN ⑧ C4 id=`agent-guardrails-hardening` |
| v1.1 | 2026-07-27 | **已冻结** | 实现前终验通过：工作流可控、M4 已归档、方向与非目标清晰；P1–P7 保留至各 change design。下一动作：按 checklist 启动 `agent-runtime-mvp` 的 **proposal 规划闸**（非直接写业务代码） |
