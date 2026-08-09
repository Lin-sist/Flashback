# Agent 控制模型 · Flashback

> 目的：说清「人如何控制协作 Agent 推进项目」——证据分工、权限闸门、与产品 Agent 的边界  
> 状态日期：2026-07-26  
> 配套：`iteration-approach.md`、`vibecoding-playbook.md`  
> **执行层 vs 参考层**：强制摘要在 `AGENTS.md` 与 openspec skills；本文件与 playbook 为完整参考。

---

## 1. 你要控制的是什么

在 Flashback 仓库里谈 Agent，默认有两层：

| 层 | 是什么 | 控制手段 |
|---|---|---|
| **L1 协作 Agent** | Cursor / Claude / Codex 等帮你改仓库 | OpenSpec + ACTIVE_TASK + 决策记录 + AGENT_LOG + 提交/外调闸 |
| **L2 产品 Agent** | 小程序里的写作陪伴 runtime | 未来独立 OpenSpec change + 产品 guardrails + 运行时可观测 |

本文件主要定义 **L1**。L2 只在蓝图与产品 change 中展开，避免把「开发工作流」写成「应用内 ReAct」。

**控制目标（L1）：**

1. Agent **不能**在你未批准时扩大范围；
2. 你 **能**回答「现在唯一在做什么」；
3. 你 **能**回答「关键技术分岔为什么选 A 不选 B」；
4. 你 **能**回答「哪一步验过、哪一步跳过」；
5. 出问题 **能**回滚到明确 git 锚点。

---

## 2. 四层控制架构

```text
┌─────────────────────────────────────────────┐
│  L-方向  iteration-blueprint（roadmap，v2.0 已冻结）│  序列、依赖、非目标总表
├─────────────────────────────────────────────┤
│  L-规则  AGENTS.md + agent-collaboration spec │  硬禁止、必读、角色
├─────────────────────────────────────────────┤
│  L-本轮  ACTIVE_TASK + openspec/changes/<id>  │  唯一范围与验收
├─────────────────────────────────────────────┤
│  L-证据  design 决策记录 + AGENT_LOG + git    │  为何 / 做了啥 / 如何退
└─────────────────────────────────────────────┘
```

### 2.1 方向层

- **写什么**：主线顺序、旁支、依赖、每个意图卡片的非目标。
- **不写什么**：本周具体 diff、临时 bug 清单。
- **谁维护**：人类 + 规划 Agent（Claude 写蓝图）；冻结后改序列需显式修订。

### 2.2 规则层

- **写什么**：永远禁止项（三 Tab、secret、封存不可变、mock 隔离等）。
- **权威**：与方向层冲突时，**规则层优先**（安全与产品初心不可被蓝图口号覆盖）。

### 2.3 本轮层

- **写什么**：proposal/design/tasks/delta。
- **铁律**：同时最多一个 Type C active；实现不得超出已确认 scope。
- **指针**：`.ai/ACTIVE_TASK.md` 是唯一入口，防止 Agent「选对自己方便的旧文档」。

### 2.4 证据层

| 载体 | 回答的问题 | 禁止变成 |
|---|---|---|
| `design.md` → 决策记录 | 为什么选这个方案？ | 测试日志粘贴板 |
| `.ai/AGENT_LOG.md` | 做了什么、怎么验、跳过什么？ | 需求规格书 |
| git commit / 锚点 | 如何回退？ | 与 log 互相覆盖历史 |
| 测试 / 手验输出 | 是否真的工作？ | 用叙述代替命令 |

---

## 3. 三道闸门（比「信任 Agent」更重要）

```text
闸门 1 · 规划批准
  人审 proposal/design/决策/delta/tasks
  → 未过：禁止业务代码

闸门 2 · 实现授权
  人明确「可以按 tasks 写代码」
  → 规划批准 ≠ 自动实现

闸门 3 · 外调 / 副作用授权
  真实 AI 批量调用、对象存储联调、push、部署等
  → 实现授权 ≠ 外调授权 ≠ 发布授权
```

RAG 项目证明：把 **offline/TDD** 与 **live provider** 拆开，才能既推进契约，又不在费用与隐私上失控。  
Flashback 对用户日记内容更敏感：**默认假设用户正文是高敏**，日志与 telemetry 不得承载原文。

---

## 4. 能力五态：防止 Agent「脑补已完成」

任何现状扫描或 proposal 的 Current Status 应使用：

| 状态 | 定义 | 可写进验收吗 |
|---|---|---|
| `confirmed` | 有可指路径的实现 + 证据 | 可作为基线事实 |
| `partial` | 有代码或 UI，但契约/证据不全 | 只能当缺口 |
| `planned` | 本 change 将做 | 验收对象 |
| `out_of_scope` | 刻意不做 | 不得实现 |
| `unknown` | 未读到或未验证 | **不得当 confirmed** |

**反模式**：把 `unknown` 写成「应该已经支持」然后开始依赖它。

---

## 5. 人机职责边界

| 事项 | 人（你） | 协作 Agent |
|---|---|---|
| 产品气质与优先级 | 最终决定 | 只能建议 |
| 不确定的 API/字段/枚举 | 确认或否决 | **禁止猜测后直接落库**（AGENTS 已要求） |
| 事前计划 | 批准/驳回 | 起草 |
| 代码实现 | 抽查 diff | 按 scope 执行 |
| 验证 | 可要求补跑/手验 | 报告真实结果 |
| 提交 / push | 授权 | 默认不提交；无 push |
| OpenSpec 归档 | 确认验收 | 仅在授权后执行 |

---

## 6. ACTIVE_TASK 状态机（建议）

```text
IDLE
  →（创建 Type C 并写入指针）→ ACTIVE
  →（实现完成，待验收）→ ACTIVE（备注 awaiting acceptance）
  →（验收通过并归档）→ IDLE

异常：
  指针指向不存在 / 已归档 / 与用户口头任务冲突
  → Agent 必须停止写操作，先修正指针
```

Type B **不**要求 ACTIVE；但必须在 log 写清范围，避免与进行中的 Type C 抢改同一核心文件。

### 6.1 Current Progress（跨会话交接）

`ACTIVE_TASK.md` 必须包含可更新的 **Current Progress** 段（见 playbook §1.1 与 Type C checklist §D）。  
权威分工：

| 问题 | 看哪里 |
|---|---|
| 现在唯一 change 是什么？ | ACTIVE_TASK Task / Status |
| 做到哪一步了？ | Current Progress + tasks.md 勾选 |
| 上次阻塞 / SKIPPED？ | Current Progress + AGENT_LOG |
| 为什么选这个方案？ | design.md 决策记录 |
| 长期下一站？ | roadmap 蓝图（未冻结则不得执行） |

---

## 7. 与 Flashback 现有资产的映射

| 已有 | 在控制模型中的角色 | 状态 |
|---|---|---|
| `AGENTS.md` | 规则层 + **强制摘要注入** | 已含 Type/Gates/Handoff/LOG |
| `.agent/skills` / `.claude/skills` openspec-* | Skill 执行层 | 应引用 vibecoding 纪律 |
| `openspec/changes/m4-...` | 本轮层（当前） | M4 未完成前不并行 Agent 大 change |
| `backend-contract-decisions.md` | 契约决议 | 与决策记录并存 |
| `.ai/TASK_CARD_TEMPLATE.md` | 小任务卡 | 已含 Type / Progress 字段 |
| `.ai/AGENT_LOG.md` | 证据层 | 顶部结构化模板 |
| `Docs/agent-iteration/workflow/**` | 完整参考层 | 人类 + checklist |
| `Docs/agent-iteration/项目初始分析.md` | 方向输入草稿 | CAUTION：非批准 scope |
| `roadmap/iteration-blueprint.md` | 方向层 | **v2.0 已冻结**（2026-08-09；核心体验与信任兑现序列） |
| `roadmap/iteration-blueprint-v1.2.md` | 历史方向层 | Phase 1 / Phase 2 Agent 能力叙事，只读 |

---

## 8. 最小可行控制集（若只能先做 5 条）

已写入 `AGENTS.md` 的最小集：

1. 同时只有一个 Type C active  
2. 三道闸门（规划 / 实现 / 外调）  
3. AGENT_LOG 验证与 SKIPPED  
4. 真实路径禁止 mock 冒充成功 + secret 不进前端（Non-Negotiable）  
5. 跨会话读 Current Progress  

完整决策记录、checklist、回顾机制见参考层，可渐进加严。

---

## 9. 成功标准与轻量回顾

连续 2–3 个 change 后，应能不靠聊天记录回答：

1. 现在 ACTIVE 的是哪一个路径？  
2. 上一刀的关键决策与否决方案是什么？  
3. 哪些验证跑了、哪些 SKIPPED？  
4. 若要回滚，git 锚点是哪次提交？  
5. 下一刀依赖是否在**已冻结**蓝图里？  

**每 2–3 个 change 轻量回顾**（见 playbook §10）：哪些环节被跳过、LOG 是否可检索、是否流程疲劳。  
若答不出成功标准问题，说明证据层或闸门仍在漏——优先修交接与 `AGENTS.md` 遵守度，而不是再写更长文档。
