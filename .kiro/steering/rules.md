# Flashback 项目规则（Kiro Steering）

> 本文件是 `.kiro/steering/` 的核心，在 Kiro vibe coding 模式下自动加载。
> **本文件不替代 `AGENTS.md`**——它是桥接层，确保 Kiro 的 Claude 遵循本仓库已有的工作流。

## 第一步（每次会话必做）

1. 读 `AGENTS.md`（仓库根）——这是最高权威，与本文件冲突时以 AGENTS.md 为准。
2. 读 `.ai/ACTIVE_TASK.md`——确认当前是否有 active Type C change。
3. 若 `ACTIVE_TASK=IDLE`：不得假装在做某个未声明的 change；等用户指令。
4. 若有 active change：读该 change 的 `tasks.md` → 最近相关 `AGENT_LOG` 条目 → 继续。

## 事实源优先级

`AGENTS.md` Non-Negotiable > 已冻结蓝图 > active OpenSpec change > baseline specs > 代码 > 旧 Docs

## 不使用 Kiro Specs

本项目使用 OpenSpec（`openspec/changes/<change-id>/`）管理变更，**不使用** `.kiro/specs/`。
不要创建 `.kiro/specs/` 目录或在其中生成 requirements/design/tasks。

## 任务分级（写代码前必判）

| 类型 | 何时 | 需要什么 |
|---|---|---|
| **A 只读** | 扫描、解释、讨论 | 不改文件 |
| **B 小修** | 文案、注释、低风险 bugfix、文档 | 追加 AGENT_LOG |
| **C 重大** | 用户可见能力、API/持久化/状态机/AI 行为 | 建 OpenSpec change + 三道闸门 |

**拿不准时按 Type C 处理，或先 Type A 向用户确认。**

## 三道闸门（Type C 强制）

1. **规划批准**：proposal/design/tasks 经用户确认前，禁止改业务代码。
2. **实现授权**：规划批准 ≠ 可写代码；须用户明确允许。
3. **外调授权**：真实 AI 调用、对象存储联调、`git push`、部署须单独授权。

## Non-Negotiable 摘要

- 三 Tab 不变：首页、时光轴、个人中心
- 用户可见命名：我的记录、时光轴、时间回看
- 封存后禁止修改 location/attachments/cover
- secret 仅 backend-side config
- 真实路径不得 mock success 冒充真实成功
- 不做大规模 backend rewrite / major frontend visual reconstruction
- 用户日记原文是高敏数据：不入日志/tracked files
- 不做 speech-to-text / voice AI / complex AI diagnosis dashboard
- 不确定的契约先向用户确认，不要猜

完整列表见 `AGENTS.md` § Non-Negotiable Rules。

## 会话结束时必做

- 更新 `.ai/ACTIVE_TASK.md` Current Progress（Type C 时）
- 追加 `.ai/AGENT_LOG.md`（Type B/C 时）
- 输出：modified files / what changed / verification / risks / `git diff --stat`
- **默认不提交**——未明确授权时不得 `git add` / `commit` / `push`

## 参考文件索引

| 用途 | 路径 |
|---|---|
| 硬规则 | `AGENTS.md` |
| 当前任务状态 | `.ai/ACTIVE_TASK.md` |
| 执行证据 | `.ai/AGENT_LOG.md` |
| 已冻结蓝图 | `Docs/agent-iteration/roadmap/iteration-blueprint.md`（**v1.2 已冻结**） |
| 对外叙事（面试向） | `Docs/agent-iteration/narrative/agent-tech-story.md` |
| Type C 开工清单 | `Docs/agent-iteration/workflow/prompt-snippets/type-c-checklist.md` |
| Type B 开工清单 | `Docs/agent-iteration/workflow/prompt-snippets/type-b-checklist.md` |
| 决策记录格式 | `Docs/agent-iteration/workflow/prompt-snippets/design-decision-record.md` |
| 协作方法论 | `Docs/agent-iteration/workflow/vibecoding-playbook.md` |
| 产品身份 | `openspec/project.md` |
| Baseline specs | `openspec/specs/` |
