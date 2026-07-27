# Flashback Agent 迭代工程文档

> 状态：工作流已建立；**M4 已正式归档（2026-07-27，ACTIVE_TASK=IDLE）**；蓝图 v1 草案待修订冻结  
> 状态日期：2026-07-27  
> 文档性质：完整参考层（人类阅读 + checklist）；**执行硬规则以 `AGENTS.md` 为准**

## 1. 本目录解决什么问题

Flashback 已有 OpenSpec（M1–M4）与 `.ai/` 交接文件。下一步要做的不仅是「再开一个 M5」，而是把 RAG 项目里已经跑通的 **Spec 范式 vibecoding** 固化到本仓库：

- 用 **OpenSpec** 管范围与验收契约
- 用 **决策记录** 管「为什么这样选」
- 用 **AGENT_LOG** 管「做了什么、如何验证」
- 用 **迭代蓝图** 管「长期往哪走、序列依赖是什么」（v1 草案已产出）

本目录是给人类与后续 Agent（尤其是接手写蓝图的 Claude）的**规划参考层**。  
**执行时的硬事实源仍以 `AGENTS.md` → `.ai/ACTIVE_TASK.md` → active OpenSpec change → `openspec/specs/` 为准。**

## 2. 阅读顺序（给 Claude / 规划 Agent）

按顺序阅读，不要先跳进历史 `Docs/archive/**`：

| 顺序 | 路径 | 用途 |
|---|---|---|
| 1 | `AGENTS.md`（仓库根） | 协作硬规则与 M4 边界 |
| 2 | `.ai/ACTIVE_TASK.md` | 当前是否有唯一活动任务 |
| 3 | `openspec/project.md` + 相关 baseline specs | 产品身份与已接受契约 |
| 4 | **本目录** `workflow/iteration-approach.md` | 当前项目迭代思路总览（先读） |
| 5 | **本目录** `workflow/vibecoding-playbook.md` | 每轮 change 怎么走得稳 |
| 6 | **本目录** `workflow/prompt-snippets/design-decision-record.md` | design 决策记录格式 |
| 7 | **本目录** `workflow/agent-control-model.md` | 控制权分层与证据分工 |
| 8 | `项目初始分析.md` | Agent 化产品方向初评（非执行契约） |
| 9 | `roadmap/iteration-blueprint.md` | 长期序列蓝图（**v1 草案已产出，待冻结**） |

## 3. 目录结构

```text
Docs/agent-iteration/
├─ README.md
├─ 项目初始分析.md                     # 产品方向草稿（CAUTION：非批准 scope）
├─ workflow/
│  ├─ iteration-approach.md
│  ├─ vibecoding-playbook.md
│  ├─ agent-control-model.md
│  └─ prompt-snippets/
│     ├─ design-decision-record.md
│     ├─ type-c-checklist.md          # Type C 可勾选清单
│     └─ type-b-checklist.md
└─ roadmap/
   ├─ README.md                       # 蓝图编写规格
   └─ iteration-blueprint.md          # 迭代蓝图 v1 草案（待用户审阅冻结）
```

## 3.1 执行层 vs 参考层（防双轨道落空）

| 层 | 位置 | Agent 如何碰到 |
|---|---|---|
| **硬注入** | `AGENTS.md` | 会话规则 / 必读 |
| **Skill** | `.agent/skills/openspec-*`、`.claude/skills/openspec-*` | `/opsx-*` 或 skill 触发 |
| **交接** | `.ai/ACTIVE_TASK.md` Current Progress、`AGENT_LOG` | 每会话必读 |
| **完整参考** | 本目录 `workflow/**` | 人类指示或规划时按 README 阅读顺序 |

规划 / 实现 OpenSpec 时，skills 须遵守 `AGENTS.md` 闸门，并在需要细节时打开本目录 checklist。

## 4. 与 OpenSpec / 旧 Docs 的关系

| 层级 | 权威性 | 说明 |
|---|---|---|
| `AGENTS.md` + active OpenSpec | **最高** | 实现与验收以之为准 |
| openspec skills | **执行剧本** | 不得绕过 AGENTS 闸门 |
| 本目录 `workflow/**` | **完整参考** | 冲突时以 AGENTS / OpenSpec 为准 |
| `项目初始分析.md` | 方向草稿 | **禁止**当作 ACTIVE scope |
| `Docs/archive/**`、`Docs/design/**` | 历史 / 视觉 | 冲突让位 OpenSpec |
| `roadmap/iteration-blueprint.md` | 方向层 | **v1 草案已产出**（待用户审阅冻结）；冻结后方向层生效 |

## 5. 两套「Agent」勿混谈

| 概念 | 含义 | 主要文档 |
|---|---|---|
| **协作 Agent / vibecoding** | 用 AI 写代码时的控制与证据体系 | 本目录 `workflow/**`、`.ai/AGENT_LOG.md`、OpenSpec |
| **产品 Agent runtime** | 应用内的多轮引导、Tool Calling、Memory 等 | `项目初始分析.md` + 未来 roadmap / OpenSpec change |

后续蓝图应同时服务两者，但 **change 序列不要把「治理加固」和「产品 Agent 能力」搅成一个巨型里程碑**。

## 6. 给 Claude 写蓝图时的硬约束摘要

编写 `roadmap/iteration-blueprint.md` 时必须遵守：

1. **不替代** active OpenSpec；蓝图是方向，change 才是可执行范围。
2. **先收口当前阶段**（M4 准生产能力）再开产品 Agent 主线；与 `项目初始分析.md` 一致。
3. **一次只激活一个 Type C change**；蓝图可列出序列，执行仍串行。
4. 每个意图卡片写清：现状校正、目标、用户故事（改前坏事→改后不同）、非目标、建议落点的 baseline spec。
5. 产品初心不变：安静、私密、克制、温柔；禁止做成话痨效率 Agent 或心理诊断仪表盘。
6. 遵守 `AGENTS.md` Non-Negotiable：三 Tab、命名、secret 不进前端、封存后不可变、不做 major rewrite 等。
7. 借鉴 RAG 的外调闸：批量 AI / embedding / 评测调用前必须披露预算并获授权。

详细编写要求见 `roadmap/README.md`。

## 7. 来源说明

本目录工作流层提炼自姊妹项目 `C:\_01_Code\RAG` 的成熟实践，尤其是：

- `RAG/AGENTS.md`（Type 分级、事实源、验证与提交）
- `RAG/docs/workflow/vibecoding-playbook.md`
- `RAG/docs/workflow/prompt-snippets/design-decision-record.md`
- `RAG/docs/roadmap/iteration-blueprint.md`（结构参考，不复制 RAG 业务序列）
- `RAG/.ai/AGENT_LOG.md` 与 C 系列 change 生命周期

Flashback 本地化时已改写为小程序 / Spring Boot / M 里程碑语境，**不得**把 RAG 的 C1–C16 业务项原样搬入本仓库。
