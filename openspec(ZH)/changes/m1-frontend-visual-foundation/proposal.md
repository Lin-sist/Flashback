# M1 Frontend Visual Foundation / M1 前端视觉基础

## Summary / 摘要

重构《时光回序》V2.0 的 WeChat Mini Program frontend visual foundation，使用户侧 demo 与“写下此刻”的核心表达，以及安静、私密、有时间感的产品气质保持一致。

该 change 是 V2.0 的 M1 模块。它只准备 visual system 与 page-level presentation foundation；不实现 backend、database、production notification、admin、deployment、monitoring 或 business-rule changes。

## Motivation / 背景动机

V1.0.1 已经具备可用的用户侧基线，但 V2.0 需要更清晰的可演示身份。产品 SHOULD 被理解为一个写下当下、保存当下、等待并在未来回看的地方，而不是一个通用记录管理器。

M1 为后续执行 Agent 提供安全的 frontend 边界：在不改变核心 business logic 的前提下，改善 mini program 的 visual language、navigation consistency、component foundation 与 demo readability。

## Scope / 范围

In scope / 范围内：

- Frontend visual tokens 与 shared visual primitives。
- Page shell、safe-area handling、top bar、bottom navigation，以及 secondary page navigation consistency。
- homepage、record editor、record list、timeline、record detail/review 与 user center 的视觉对齐。
- 以 `Docs/design/home-v2/*.html` 定稿页面和 `Docs/design/home-v2/时光回序_设计规范.md` 为视觉依据进行还原。
- 在局部页面视觉稳定后，再抽取 tokens / shared components。
- draft、sealed、unlocked/arrived records 的 status presentation。
- 当 mini program demo 需要时，补齐 empty、loading、error 与 demo-preview states。
- 为 V2.0 命名与语气服务的页面标题、动作标签、状态标签、empty/demo states 等 microcopy adjustments。
- WeChat Mini Program acceptance checks。

Out of scope / 范围外：

- Backend API changes。
- Database 或 schema changes。
- Record status business-rule changes。
- 真实 WeChat production launch。
- WeChat subscription-message implementation。Subscription messages 规划在后续 V2.0 模块中处理，不属于 M1。
- Admin portal。
- Production deployment、observability 与 online monitoring。
- AI capability changes。
- 真实 MAP / IMAGE / VOICE features。这些可以作为未来能力的视觉提示保留，但 MUST NOT 暗示 V2.0 已具备可工作的真实功能。

## Success Criteria / 成功标准

- mini program 具备围绕“写下此刻”的一致 V2.0 visual foundation。
- mini program 使用已确认的 宣纸朱砂 visual direction。
- mini program 页面视觉可追溯到 `Docs/design/home-v2/*.html` 定稿页面和 `Docs/design/home-v2/时光回序_设计规范.md`，而不是 Agent 自行发挥。
- tokens / shared components 来自局部稳定后的沉淀，未进行一开始的大规模抽象。
- 用户可见命名围绕 我的记录、时光轴、时间回看 对齐。
- shared tokens 与 component/page shell conventions 足够清晰，可供后续模块复用。
- primary 与 secondary page responsibilities 保持完整。
- 从书写到回看的 demo flow 在视觉上清晰可读。
- 不引入 backend、database、production、admin 或 business-rule changes。

## Confirmed Decisions / 已确认决策

- V2.0 OpenSpec 对旧项目文档具有最高优先级。
- M1 使用 宣纸朱砂 visual direction。
- M1 必须以 `Docs/design/home-v2/*.html` 定稿页面和 `Docs/design/home-v2/时光回序_设计规范.md` 为准。
- M1 必须先局部页面还原稳定，再抽取 tokens / shared components。
- 当 microcopy 支撑 V2.0 命名与语气时，M1 MAY 调整。
- V2.0 用户可见名称为 我的记录、时光轴、时间回看。
- 现有 `frontend/src/features/preview` 是初始 demo data path。
- 允许 one-click preview / no-login demo mode。
- V2.0 不包含 management/admin portal。
- H5/Web 用户侧方案仅作为历史参考。
- V2.0 会在后续模块包含 WeChat subscription messages，不在 M1 中实现。
