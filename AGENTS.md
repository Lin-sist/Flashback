# AGENTS.md

## Project

- 项目：时光回序 Flashback V2.0。
- 技术栈：Uniapp + Vue 3 + Pinia + WeChat Mini Program；backend 为 Spring Boot / MyBatis / MySQL 方向。
- 当前阶段：M3 demo core flow hardening / backend rectification。
- 产品初心：帮助用户写下当下，并让未来的自己重新理解这一刻；不是效率仪表盘、社交流或管理后台。

## Source of Truth

- OpenSpec 是事实源；旧 `Docs/**` 只在不冲突时参考。
- 每轮任务必须先读 `.ai/ACTIVE_TASK.md`，以其中声明的 active change 为当前事实源。
- 当前 M3 后端整改事实源：
  - `openspec/project.md`
  - `openspec/specs/backend-core/spec.md`
  - `openspec/specs/miniapp-core/spec.md`
  - `openspec/specs/v2-product-scope/spec.md`
  - `openspec/changes/m3-demo-core-flow-hardening/proposal.md`
  - `openspec/changes/m3-demo-core-flow-hardening/design.md`
  - `openspec/changes/m3-demo-core-flow-hardening/tasks.md`
  - `openspec/changes/m3-demo-core-flow-hardening/specs/backend-core/spec.md`
  - `openspec/changes/m3-demo-core-flow-hardening/specs/miniapp-core/spec.md`
  - `openspec/changes/m3-demo-core-flow-hardening/specs/v2-product-scope/spec.md`

## Non-Negotiable Rules

- 保留三个一级 Tab： 首页、时光轴、个人中心。
- V2.0 用户可见命名：我的记录、时光轴、时间回看。
- M3 是 demo 核心闭环稳固，不是生产上线。
- M3 范围内允许修改 backend、database/schema、business rules、AI fallback/organization、subscription-message reminder 相关代码，但只能服务于 M3 OpenSpec。
- 不改 deployment、monitoring、admin portal、SMS、production notification center、campaign delivery、real MAP / IMAGE / VOICE。
- 不做大规模 backend rewrite。
- 不做 major frontend visual reconstruction。
- 不改 package / lockfile，除非任务明确要求并说明原因。
- 不确定的 backend API 契约、字段命名、持久化方式、枚举语义或前端可见状态，必须先向用户确认，不要主观猜测。

## Context Budget Rules

- 不要全仓库扫描。
- 先读 `.ai/ACTIVE_TASK.md`。
- 只读本轮任务列出的文件。
- 需要额外文件时先说明原因。
- 每轮只解决当前 task，不顺手重构。

## Required Evidence

- 所有实现记录、验证结果、跳过验证原因、手动微信验证结果都必须写入 `.ai/AGENT_LOG.md`。

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

