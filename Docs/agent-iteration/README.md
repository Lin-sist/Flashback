# Flashback Agent 迭代工程文档

> 状态日期：2026-08-09
> 当前状态：M4、Phase 1、Phase 2（C1–C9）均已归档；`ACTIVE_TASK=IDLE`
> 产品宪章 v0.1 已确认；核心体验迭代蓝图 v2.0 已冻结
> 文档性质：人与 Agent 的完整参考层；执行硬规则以仓库根 `AGENTS.md` 为准

## 1. 本目录解决什么问题

本目录把 Flashback 的长期产品方向、OpenSpec 分阶段执行、架构取舍、证据记录与对外叙事分开管理：

- `roadmap/`：产品为什么存在、下一阶段往哪走；
- `workflow/`：每轮 change 如何经过 Type、Gates、决策和验证；
- `architecture/`：C1–C9 形成的端口、分层与技术取舍；
- `narrative/`：可对外使用的工程故事，不是执行契约；
- `.ai/`：当前任务与 append-only 证据账本；
- `openspec/`：当前实现和每刀变更的最高优先级事实源。

## 2. 推荐阅读顺序

| 顺序 | 路径 | 用途 |
|---|---|---|
| 1 | `AGENTS.md` | 硬规则、Type、Gates、Non-Negotiable |
| 2 | `.ai/ACTIVE_TASK.md` | 当前是否存在唯一 active change |
| 3 | `openspec/project.md` + accepted specs | 当前已实现契约 |
| 4 | `roadmap/core-product-definition.md` | 长期产品宪章 v0.1 |
| 5 | `roadmap/iteration-blueprint.md` | 当前冻结蓝图 v2.0 |
| 6 | `workflow/iteration-approach.md` | 六步闭环总览 |
| 7 | `workflow/vibecoding-playbook.md` | 人与 Agent 的完整协作说明 |
| 8 | `workflow/prompt-snippets/` | Type B / C 清单与决策记录模板 |
| 9 | `architecture/README.md` | C1–C9 架构与选型参考 |
| 10 | `roadmap/iteration-blueprint-v1.2.md` | 已完成能力序列的历史依据 |
| 11 | `narrative/agent-tech-story.md` | 对外技术叙事 |

## 3. 目录结构

```text
Docs/agent-iteration/
├─ README.md
├─ 项目初始分析.md                    # 早期方向草稿，非批准 scope
├─ roadmap/
│  ├─ README.md
│  ├─ core-product-definition.md      # 产品宪章 v0.1
│  ├─ iteration-blueprint.md          # 当前 v2.0
│  └─ iteration-blueprint-v1.2.md     # C1–C9 历史快照
├─ workflow/
│  ├─ iteration-approach.md
│  ├─ vibecoding-playbook.md
│  ├─ agent-control-model.md
│  └─ prompt-snippets/
├─ architecture/
│  ├─ README.md
│  ├─ agent-architecture-constitution.md
│  └─ tech-selection-draft.md
└─ narrative/
   └─ agent-tech-story.md
```

## 4. 层级与权威性

| 层级 | 权威性 | 说明 |
|---|---|---|
| `AGENTS.md` | 硬规则 | 冲突时最高优先 |
| accepted OpenSpec | 当前契约 | 旧 Docs 不得改写 |
| active OpenSpec change | 本轮计划 | 只有审批后才能进入实现 |
| `core-product-definition.md` | 长期方向 | 筛选未来能力，不等于已实现 |
| `iteration-blueprint.md` | 方向序列 | 排序和停止条件，不授权代码 |
| `workflow/**` | 方法参考 | 补充操作细节 |
| `architecture/**` | 技术参考 | 冻结原则，不预批具体类名 |
| `narrative/**` | 对外表达 | 不得反向充当实现事实源 |

## 5. 当前方向摘要

Phase 1 / 2 已把 Agent Loop、Tool、Memory、Guardrail、Trace、Eval、Reflection、Resilience 与 Temporal Intelligence 做成能力底座。

v2.0 不继续机械堆 Agent 技术，而按以下方向推进：

```text
诚实的真实产品表面
  → 不依赖 Agent / 封存的“留下此刻”
  → 可带走、可删除的数据主权
  → 有朋友温度的见证者
  → 按次授权、有出处、可撤销的记忆
  → 安全闸
  → 验证后再决定时间篇章
```

## 6. 两套“Agent”不要混谈

| 概念 | 含义 | 主要事实源 |
|---|---|---|
| 协作 Agent / vibecoding | AI 如何受控地规划、实现和验收 | `AGENTS.md`、`workflow/**`、`.ai/**`、OpenSpec |
| 产品 Agent runtime | 小程序内的见证者、工具、记忆与护栏 | accepted specs、`roadmap/**`、各 Type C change |

## 7. 执行纪律摘要

1. 蓝图批准不等于 Type C 规划批准，更不等于实现授权。
2. 一次最多一个 active Type C。
3. 不确定的 API、字段、状态、持久化、权限、Provider、数据权利和安全语义必须先确认。
4. 真实 provider、对象存储、push、部署和发布须单独授权。
5. H2、build、scripted、真实 MySQL、对象存储、provider、微信真机和用户研究分层报告。
6. `.ai/AGENT_LOG.md` 只追加；默认用户手动提交。
7. 不写入用户日记原文、secret 或可识别私人信息。
