# Vibecoding Playbook · Flashback 协作开发手册

> 适用项目：《时光回序》Flashback V2.0  
> 文档性质：长期沿用的协作方法论——**蓝图管「往哪走」，本手册管「怎么走得稳」**  
> 状态日期：2026-07-26  
> 冲突处理：与 `AGENTS.md` 冲突时，以 `AGENTS.md` 为准  
> **双轨说明**：`AGENTS.md` / `.agent/skills` 为 Agent 执行层；本手册为完整参考层。强制摘要已注入 `AGENTS.md`，细节与 checklist 在此展开。  
> 灵感来源：RAG 项目 playbook（已本地化）

---

## 0. 一句话原则

把人工审查点从「事后看 diff」前移到「事前审计划」；  
为每个改动建立可干净回滚的 git 锚点；  
**AGENT_LOG 记「做了什么」**，**design 决策记录记「为什么」**，**OpenSpec 记「必须满足什么」**，三者配合形成可追溯、可回退的迭代闭环。

### 0.1 与 AGENTS.md 的关系（防双写冲突）

| 内容 | 权威位置 |
|---|---|
| 产品 Non-Negotiable、M4 边界、secret、mock 隔离 | **`AGENTS.md` only**（本手册不复述全文） |
| Type A/B/C、三道闸门、提交默认、Required Output | `AGENTS.md` 强制摘要 + 本手册展开 |
| 决策记录格式、Type B/C 勾选清单 | `prompt-snippets/*` |
| 长期序列 | `../roadmap/`（蓝图待写） |

---

## 1. 核心工作流（每个 change 通用）

标准闭环六步，**串行**推进；Type C **一次只激活一个** change：

```text
① 开工前置
   工作区状态清楚；记录当前 commit（或 pre-<id> 锚点）；
   先读 AGENTS.md → ACTIVE_TASK → 相关 OpenSpec。

② 事前闸门（关键）
   Type B：待改清单（文件 + 现状 + 改法）+ 提交责任；
   Type C：proposal / design（含决策记录）/ tasks / spec delta；
   人确认前禁止改业务代码。

③ 执行
   按已确认计划改代码；边做边追加 AGENT_LOG（Commit: pending）。

④ 收尾报告
   改动摘要 + 验证结果（通过 / 失败 / SKIPPED+原因）；
   输出 AGENTS.md 要求的完成字段。

⑤ 人工审 diff
   重点：是否超 scope、是否破坏产品初心、secret 是否泄漏。

⑥ 定版
   Type C：验收 → delta 合入 baseline（若适用）→ archive → ACTIVE_TASK=IDLE；
   Type B：AGENT_LOG 即可；
   按事前约定完成中文 commit；下次写操作开始时补录真实 hash（推荐）。
```

回到 **IDLE**（Type C）或干净工作区（Type B）后，才允许开下一个主线 change。

**操作性清单（优先勾选）：**

- Type C：`prompt-snippets/type-c-checklist.md`
- Type B：`prompt-snippets/type-b-checklist.md`

### 节奏图

```text
开工锚点 → 事前闸门(计划) → 人确认 → 执行+LOG → 验证报告
    → 人审 diff → 通过则收口/commit → IDLE
    → 超界或改崩 → reset 回锚点 → 重新规划
```

### 1.1 跨会话交接（强制习惯）

一个 Type C 常跨多个 Agent 会话。交接权威顺序：

1. `.ai/ACTIVE_TASK.md` → **Current Progress**
2. active change `tasks.md` 勾选状态
3. `.ai/AGENT_LOG.md` 最近相关条目

**Current Progress** 最低字段：

```markdown
## Current Progress
- Last session: YYYY-MM-DD
- Completed tasks: …
- Blocked on: …
- Next step: …
- SKIPPED verifications to revisit: …
```

新会话若 Progress 与 tasks / 用户指令冲突 → 停止写操作，先修正指针。

---

## 2. Type 分级速查

### Type A · 只读

- **用途**：现状扫描、对照 specs、解释、规划讨论、蓝图起草研讨。
- **允许**：读代码与文档、提问、输出分析。
- **禁止**：创建/修改/归档 `openspec/changes`、改 `ACTIVE_TASK`、改业务代码、假装已验证。
- **证据**：默认不强制 AGENT_LOG；若结论将长期影响方向，可在讨论文档或后续 change 中引用。

### Type B · 小范围维护

- **用途**：文案、注释、链接、已有契约内低风险 bugfix、纯文档方法说明（如本目录）。
- **允许**：小范围改文件；完成后追加 `AGENT_LOG`。
- **禁止**：借机改 API/权限/持久化/AI 语义；创建空 Type C 目录「走形式」。
- **提交**：事前明确 `Agent 提交` 或 `用户手动提交`；默认后者。

### Type C · 重大变更

- **触发任一即 Type C**：
  - 新增或删除用户可见能力；
  - 修改 API、DTO、持久化、状态机、权限；
  - 修改 AI provider 行为语义、真实/ mock 边界；
  - 对象存储 / 附件 / 位置 / 封存不变性等核心契约；
  - 跨模块重构或多个独立提交才能完成的主题；
  - 未来产品 Agent runtime / tool / memory / guardrails / eval 主线能力。
- **必须**：`openspec/changes/<change-id>/` 下 proposal、design、tasks、必要的 spec delta；`ACTIVE_TASK` 指向它。
- **禁止**：未批准 design 就写业务代码；并行第二个 active Type C。

**拿不准时：按 Type C 处理，或先 Type A 问用户定级。**

---

## 3. 七条加固纪律

### 3.1 事前闸门：先出计划，确认后再动手

- Type B 最低交付：**待改清单**。
- Type C 最低交付：**proposal + design + tasks + delta 草案**。
- proposal 建议包含：
  - Why Now / Goals / Non-goals；
  - **用户故事（大白话）**：改前坏事 → 改后不同；
  - 能力五态：`confirmed / partial / planned / out_of_scope / unknown`；
  - 外调预算（若有）；
  - 提交责任。
- design 必须含 **决策记录**（见 `prompt-snippets/design-decision-record.md`）。

### 3.2 git 锚点：可回滚

正式改代码前记录当前 HEAD（或打 `pre-<change-id>` 轻量 tag）。  
改崩或严重超界时，回到锚点重新规划，而不是在脏工作区上无限打补丁。

### 3.3 禁止清单：写清「不许碰什么」

产品级硬禁止 **以 `AGENTS.md` Non-Negotiable 为准**（三 Tab、mock 隔离、secret、封存不可变、M4 非部署等）。  
每个任务仍应在 tasks/out_of_scope 中写清**本轮额外**不许碰的模块，避免「没说不能做」。

### 3.4 验收报告事实，不空口达标

- 验证语义：`PASS` / `FAIL` / `SKIPPED`+原因（见 `AGENTS.md` Required Evidence）。
- 后端 / 前端 / 微信手验 / AI·存储：按 `ACTIVE_TASK` Verification 与任务约定执行。
- **禁止**用「应该没问题」代替命令或手验结果；**禁止** mock 冒充真实成功（`AGENTS.md`）。

### 3.5 提交责任前置

见 `AGENTS.md` Gates。摘要：默认 `用户手动提交`；`push`/PR/部署始终另授权。

### 3.6 验证与外调授权

见 `AGENTS.md` Gates 第 3 道。披露外调时写清：类型、次数、是否出站用户内容、模型/供应商、费用或零费用依据、限流/超时。

### 3.7 AGENT_LOG 纪律

见 `AGENTS.md` Required Evidence 与 `.ai/AGENT_LOG.md` 顶部**结构化模板**。  
决策取舍在 `design.md`，不进 log。

---

## 4. Type C 分阶段模板

**请直接使用勾选清单：** `prompt-snippets/type-c-checklist.md`（规划 / 实现 / 收口 / 会话恢复）。

摘要：

| 阶段 | 关键产出 | 禁止 |
|---|---|---|
| 规划 | proposal + design（决策记录）+ tasks + delta；ACTIVE + Current Progress | 业务代码 |
| 实现 | tasks 勾选、LOG、验证；会话末更新 Progress | 未授权外调、超 scope |
| 收口 | 验收、归档、IDLE、commit | 未授权 push/部署 |

---

## 5. Bug 处理规程

1. **先复现 → 再定位根因 → 最后才改。**
2. 当前 change 引入的小 bug：在本 change 内修，并补回归若可行。
3. 当前 change 已改乱：回锚点，重新规划。
4. 历史已归档问题：
   - 小 → Type B + AGENT_LOG；
   - 涉契约 → 新 Type C，**不改写旧 archive 冒充从未出错**。

---

## 6. 可复用提示词要点（给人类粘贴）

### 6.1 Type A

- 声明只读；禁止改 openspec/ACTIVE_TASK/代码。
- 关键结论附路径；能力用五态标注。
- 结束复述：未改文件、未建 change。

### 6.2 Type B

- 先输出待改清单与提交责任，等待确认。
- 确认后只改清单内文件；跑约定验证；写 AGENT_LOG。
- 禁止升级为偷偷 Type C 范围。

### 6.3 Type C · 仅规划

- 生成 proposal/design/tasks/delta；ACTIVE_TASK=ACTIVE。
- 决策记录完整；外调写 0 或预算。
- 明确：本阶段不改业务代码。

### 6.4 Type C · 仅实现

- 引用已批准 artifacts；只做 tasks 勾选范围。
- 禁止未授权外调与无关重构。
- 完成后保持 ACTIVE 直至人验收归档（除非任务明确要求 Agent 归档且已授权）。

---

## 7. 提交信息约定

格式：`<type>(<scope>): <中文说明>`

**type 常用：** `feat` / `fix` / `docs` / `test` / `refactor` / `chore`

**scope 枚举（优先从表中选，避免每次生造）：**

| scope | 含义 |
|---|---|
| `记录` | 记录 CRUD、封存、解锁 |
| `时光轴` | timeline 筛选、分页 |
| `回看` | 时间回看 / 解锁后阅读 |
| `AI` | AI provider、写作提示、整理、阶段总结 |
| `附件` | 图片、语音、封面 |
| `位置` | 地理位置 |
| `小程序` | uni-app / 微信端壳与请求 |
| `后端` | Spring Boot / MyBatis 通用 |
| `治理` | AGENTS、工作流、AGENT_LOG 模板 |
| `openspec` | change / delta / 归档 |

示例：

- `docs(治理): 注入vibecoding强制摘要到AGENTS`
- `feat(附件): 封存后拒绝修改封面`
- `chore(openspec): 验收并归档m4-real-capability-completion`

一个 commit 对应一个可解释最小单元；提交前确认暂存区无无关改动。

---

## 8. 与治理文件的关系

| 文件 | 职责 |
|---|---|
| `AGENTS.md` | **执行层硬规则**（自动注入 / 冲突时最高） |
| `.agent/skills/openspec-*`、`.claude/skills/openspec-*` | **Skill 执行层**；应引用本工作流关键纪律 |
| `openspec/**` | 契约与 change 生命周期 |
| `.ai/ACTIVE_TASK.md` | 唯一活动指针 + **Current Progress** |
| `.ai/AGENT_LOG.md` | 执行证据链 |
| `Docs/agent-iteration/workflow/**` | **完整参考层**（人类阅读 / 面试叙事 / checklist） |
| `Docs/agent-iteration/roadmap/**` | 方向层（蓝图待写） |
| `Docs/agent-iteration/项目初始分析.md` | 产品 Agent 化草稿（非批准 scope） |

---

## 9. 每轮自问（产品硬项见 AGENTS.md）

在实现结束前快速核对（细则以 `AGENTS.md` Non-Negotiable 为准）：

- [ ] 是否触碰 Non-Negotiable？
- [ ] 真实路径是否可能 mock 成功？
- [ ] 是否把未批准的产品 Agent / 蓝图项偷渡进当前 change？
- [ ] Current Progress 与 AGENT_LOG 是否已更新？
- [ ] 不确定契约是否已问用户？

---

## 10. 轻量回顾（每 2–3 个 change）

个人项目不需要正式 sprint retro。完成 2–3 个 Type C（或等价大切片）后自问：

1. 哪些闸门/清单实际被跳过了？要改流程还是加固纪律？
2. 决策记录是否帮到复盘或面试叙事？
3. AGENT_LOG 是否仍可在 2 分钟内定位上次阻塞？
4. 是否出现「流程过重不想写」的疲劳？若有，优先保留 `AGENTS.md` 最小强制集。
