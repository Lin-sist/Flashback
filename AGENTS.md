# AGENTS.md

## Project

- 项目：时光回序 Flashback V2.0。
- 技术栈：Uniapp + Vue 3 + Pinia + WeChat Mini Program。
- 当前阶段：M1 frontend visual foundation / high-fidelity visual translation。
- 产品初心：帮助用户写下当下，并让未来的自己重新理解这一刻；不是效率仪表盘、社交流或管理后台。

## Source of Truth

- OpenSpec 是事实源；旧 `Docs/**` 只在不冲突时参考。
- M1 相关事实源：
  - `openspec/project.md`
  - `openspec/changes/m1-frontend-visual-foundation/visual-reference-map.md`
  - `openspec/changes/m1-frontend-visual-foundation/design.md`
  - `openspec/changes/m1-frontend-visual-foundation/tasks.md`
  - `Docs/design/home-v2/`

## Non-Negotiable Rules

- finalized HTML / PNG 是视觉目标，不是灵感参考。
- 不允许旧页面换肤；不允许先大规模抽象组件。
- 页面级高保真优先，稳定后再抽取 tokens / shared components。
- 现有 frontend 只作为 routing、data shape、state display、business-safe behavior 参考。
- 保留三个一级 Tab： 首页、时光轴、个人中心。
- V2.0 用户可见命名：我的记录、时光轴、时间回看。
- 不改 backend、database、schema、business rules、AI、subscription-message、deployment、monitoring、real MAP / IMAGE / VOICE。
- 不改 package / lockfile，除非任务明确要求并说明原因。

## Context Budget Rules

- 不要全仓库扫描。
- 先读 `.ai/ACTIVE_TASK.md`。
- 只读本轮任务列出的文件。
- 需要额外文件时先说明原因。
- 每轮只解决当前 task，不顺手重构。

## Required Output

每个 Agent 完成后必须输出：

- modified files
- what changed
- verification result
- skipped verification reason, if any
- `git diff --stat`
- scope safety check
- remaining risks

## OpenSpec Boundary

- 只有范围、canonical mapping、验收标准发生变化时才修改 OpenSpec。
- 普通 bugfix / 样式精修不需要修改 OpenSpec，只需要写入 `.ai/AGENT_LOG.md`。
