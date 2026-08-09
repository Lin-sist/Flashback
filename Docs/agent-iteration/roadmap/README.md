# Roadmap 目录说明

> 状态日期：2026-08-09
> 当前产品宪章：`core-product-definition.md` **v0.1 已确认**
> 当前方向蓝图：`iteration-blueprint.md` **v2.0 已冻结**
> 历史方向蓝图：`iteration-blueprint-v1.2.md`（C1–C9，已完成、只读）
> 执行硬规则：仓库根 `AGENTS.md`

---

## 1. 三份文档如何分工

| 文档 | 回答什么 | 是否授权实现 |
|---|---|---:|
| `core-product-definition.md` | 产品为什么存在、长期做与不做什么 | 否 |
| `iteration-blueprint.md` | 当前先做什么、何时继续、何时停止 | 否 |
| `iteration-blueprint-v1.2.md` | C1–C9 能力序列与历史技术取舍 | 否；历史只读 |

执行某项能力仍须：

1. 判定 Type；
2. Type C 在 `openspec/changes/<change-id>/` 建 proposal / design / tasks / delta；
3. 将 `.ai/ACTIVE_TASK.md` 指向唯一 active change；
4. 分别通过规划、实现、真实外调/副作用三道闸门；
5. 以 accepted specs、真实实现与分层证据验收。

## 2. 当前序列

```text
H0 truth-surface-cleanup
  → E0 capture-ritual-prototype
  → P3.1 present-moment-capture
  → P3.2 data-ownership-foundation
  → P4.1 witness-agent-alignment
  → P4.2 memory-agency
  → R1 safety-response-minimum
  → E1 time-chapter-prototype
  → 有正证据才进入 P5.x
```

`ACTIVE_TASK` 当前保持 `IDLE`。蓝图冻结不自动启动 H0 或任一 Type C。

## 3. 规划 Agent 必读顺序

1. `AGENTS.md`
2. `.ai/ACTIVE_TASK.md`
3. `openspec/project.md` 与相关 accepted specs
4. `core-product-definition.md`
5. `iteration-blueprint.md`
6. `../workflow/prompt-snippets/type-b-checklist.md` 或 `type-c-checklist.md`
7. 若涉及 Agent 架构，再读 `../architecture/`
8. 若需要 C1–C9 历史依据，再读 `iteration-blueprint-v1.2.md` 与 archive

## 4. 内容硬边界

- 三个一级 Tab：首页、时光轴、个人中心；
- 用户可见命名：我的记录、时光轴、时间回看；
- Preview 与 authenticated real path 隔离；
- secret 不进入 frontend 或 tracked files；
- 封存后 location / attachments / cover 不可变；
- 不做 STT、诊断评分、管理后台、生产通知中心、major rewrite 或 major visual reconstruction；
- 不把原型、build、H2 或 synthetic PASS 冒充真实 MySQL、对象存储、provider、微信真机或生产能力；
- 不写入私人日记原文、真实 secret 或可识别用户数据。

## 5. 修订纪律

- 产品原则变化先修订 `core-product-definition.md`；
- 方向、顺序、意图卡片变化修订 `iteration-blueprint.md`；
- 用户可见能力、AI 语义、API、状态、持久化、权限、数据权利或安全变化必须走 Type C；
- 历史蓝图与 `openspec/changes/archive/**` 不回改；
- 每次落盘追加 `.ai/AGENT_LOG.md`，并报告 PASS / FAIL / SKIPPED。
