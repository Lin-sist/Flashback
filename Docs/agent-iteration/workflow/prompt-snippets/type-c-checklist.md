# Type C Change 操作性清单

> 用途：把 playbook 的「六步闭环」落成可勾选步骤。  
> 强制摘要已注入 `AGENTS.md`；完整方法论见 `../vibecoding-playbook.md`。  
> 产品硬禁止项以 `AGENTS.md` Non-Negotiable 为准，本清单不重复全文。

---

## A. 启动规划（事前闸门 · 阶段一）

- [ ] 确认 `.ai/ACTIVE_TASK.md`：**无冲突**（IDLE，或用户明确要求替换/仅指向本 change）
- [ ] M4 仍为 active 时：**不得**并行新开另一主线 Type C（除非用户明确要求先收口/切换）
- [ ] 记录开工锚点：当前 `git rev-parse --short HEAD`（或 `pre-<change-id>` 说明）
- [ ] 创建 `openspec/changes/<change-id>/`
- [ ] 编写 `proposal.md`
  - [ ] Why Now / Goals / Non-goals
  - [ ] 用户故事（改前坏事 → 改后不同）
  - [ ] 能力五态：`confirmed` / `partial` / `planned` / `out_of_scope` / `unknown`
  - [ ] 外调预算（无则写 0 与依据）
  - [ ] 提交责任：`用户手动提交` 或 `Agent 提交`
- [ ] 编写 `design.md`
  - [ ] 架构 / 数据流 / 验证策略
  - [ ] **`## 决策记录`**（见 `design-decision-record.md`）
- [ ] 编写 `tasks.md`
  - [ ] 小步可验证切片
  - [ ] 含「实现授权」检查点（未勾选前不写业务代码）
- [ ] 编写 `specs/<capability>/spec.md` delta（若契约变化）
- [ ] 更新 `.ai/ACTIVE_TASK.md`：Status=ACTIVE、指针、**Current Progress** 初始化
- [ ] **请求并获得规划批准**（闸门 1）——本阶段结束；默认 **零业务代码**

---

## B. 实现（闸门 2 通过后）

- [ ] 用户明确 **实现授权**（闸门 2）
- [ ] 若有真实 AI / OSS / 网络业务调用：单独获得 **外调授权**（闸门 3）
- [ ] 按 `tasks.md` 逐项实现；完成立即 `- [ ]` → `- [x]`
- [ ] 不超出 proposal Non-goals / AGENTS Non-Negotiable
- [ ] 边做边追加 `.ai/AGENT_LOG.md`（结构化模板；`Commit: pending`）
- [ ] 会话暂停/结束：更新 `ACTIVE_TASK` **Current Progress**
- [ ] 验证：PASS / FAIL / SKIPPED+原因；微信手验写清环境
- [ ] 完成输出字段（见 `AGENTS.md` Required Output）

---

## C. 验收与收口

- [ ] 用户审 diff / 验收通过
- [ ]（若约定）delta 接受进 `openspec/specs/`
- [ ] change 归档到 `openspec/changes/archive/`（或项目既有约定）
- [ ] `ACTIVE_TASK` → IDLE；清空或归档 Current Progress
- [ ] 按提交责任 commit；建议另条 AGENT_LOG 补录 hash
- [ ] **未**获 push/PR/部署授权则不执行这些动作

---

## D. 会话间快速恢复（新会话前 60 秒）

1. 读 `AGENTS.md` 强制摘要  
2. 读 `ACTIVE_TASK` 的 Task + **Current Progress**  
3. 读 active `tasks.md` 未勾选项  
4. 读 AGENT_LOG 最近 1–3 条相关记录  
5. 复述：下一步唯一 task、已知阻塞、待补 SKIPPED  

若 Progress / tasks / 用户口头任务冲突 → **停止写操作**，先修正指针。
