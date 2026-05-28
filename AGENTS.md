# Flashback Agent Rules / Flashback Agent 协作规则

## Shared Mission / 共同目标

本仓库属于《时光回序》Flashback。当前规划目标是 V2.0：写下此刻 · 时间回看演示版。

Agent MUST 保持产品初心：项目的核心是让用户写下当下，并让未来的自己重新理解这一刻。它不是效率仪表盘、社交流、生产级通知系统，也不是把焦虑寄托给未来的概念噱头。

## Collaboration Roles / 协作角色

- GPT：负责总体规划与阶段编排。
- Codex：负责代码审查、规格审查、OpenSpec 上下文维护；只有在用户明确要求时才执行实现。
- Kiro / Antigravity / Copilot：基于已批准的 OpenSpec 任务进行工程执行。
- Claude：负责灵感、产品语言与文案。
- v0：只作为局部 UI 结构参考；MUST NOT 将 v0 输出当作事实源。

## OpenSpec First / OpenSpec 优先

- 对于 V2.0 工作，OpenSpec 具有最高优先级。旧的 `Docs/**` 文件只有在不与 OpenSpec 冲突时才作为参考。
- OpenSpec 不是冻结文档；当用户确认后续模块决策时，SHOULD 持续更新。
- 使用 `openspec/project.md` 读取项目级上下文。
- 使用 `openspec/specs/**/spec.md` 读取已接受的能力要求。
- 使用 `openspec/changes/<change-id>/` 读取实施前的提案、设计、任务与 spec delta。
- 除非用户明确要求一个很小的直接修复，否则 V2.0 功能或重构工作 MUST NOT 绕过 OpenSpec。
- 当一个 change 已有 proposal、design、tasks 和 spec delta 时，执行 Agent MUST 在编辑代码前完整阅读这些材料。

## Scope Guardrails / 范围护栏

- 当前验收客户端是 WeChat Mini Program。
- V2.0 是演示 / 展示版本，不是真实上线版本。
- H5/Web 用户端方案是历史参考，MUST NOT 作为 V2.0 验收目标。
- V2.0 不包含管理端 / admin portal。
- V2.0 后续模块会包含 WeChat subscription message 工作，但 M1 MUST NOT 实现。
- M1 只做 frontend visual foundation。
- M1 工作中 MUST NOT 修改 backend、database、生产部署、监控或 admin portal 行为。
- 除非后续 OpenSpec change 明确纳入范围，否则 MUST NOT 修改业务状态规则。
- MUST NOT 将 homepage、timeline、record list、record detail 或 user center 改造成无关的管理页、分析页或内容流页面。
- M1 期间 MUST NOT 修改 AI capability。

## Engineering Guardrails / 工程护栏

- 优先沿用 `frontend/src` 中既有的 Uniapp + Vue 3 + Pinia 模式。
- 保持修改小而可追溯，并与当前 active OpenSpec change 对齐。
- 保留三个一级 Tab： 首页、时光轴、个人中心。
- 用户可见命名 MUST 使用 V2.0 名称：我的记录、时光轴、时间回看。
- 我的记录、新建记录、时间回看和 settings-style pages SHOULD 保持为二级页面，除非后续 spec 改变这一点。
- M1 视觉方向是 宣纸朱砂风格。
- M1 视觉实现 MUST 以 `Docs/design/home-v2/*.html` 定稿页面和 `Docs/design/home-v2/时光回序_设计规范.md` 为准；Agent MUST NOT 自行重新设计视觉风格、布局语言或组件气质。
- M1 工程节奏 MUST 先完成局部页面视觉还原并稳定，再抽取 tokens / shared components；MUST NOT 一开始进行大规模抽象或先搭组件库再强行套页面。
- 除非后续 spec 修改，否则使用现有 `frontend/src/features/preview` 作为 V2.0 demo data path。
- V2.0 演示允许 one-click preview / no-login demo mode。
- M1 视觉工作 SHOULD 在可行时验证 WeChat Mini Program build，并明确 demo states。

## Review Expectations / 审查重点

Codex 审查 SHOULD 优先关注：

- 是否偏离 OpenSpec 范围。
- 是否破坏 V2.0 产品叙事。
- 页面职责是否回退或混淆。
- 是否与已选择的 V2.0 视觉方向不一致。
- 是否引入非预期的 backend / database / business-logic 修改。
- 用户可见 UI 变更是否缺少 mini program 验证。
