# 《时光回序》Flashback V2.0 Project Context / 项目上下文

## Project Identity / 项目身份

- 项目名称： 《时光回序》Flashback。
- 当前稳定基线： V1.0.1 之上的 **V2.0 M4 已接受能力**（真实 AI / 私有对象存储附件 / 位置 / 封面 / 时光轴筛选分页 / preview 隔离等，见 baseline specs）。
- 当前工程状态： **Phase 1 / Phase 2 均已完成**；C1–C9 已归档，最后一刀 C9 `agent-temporal-intelligence` 于 2026-08-08 归档。`.ai/ACTIVE_TASK.md` 为 `IDLE`。
- 当前目标：在 M4 与 C1–C9 能力底座上转向核心体验与信任兑现。长期产品方向见 `Docs/agent-iteration/roadmap/core-product-definition.md` **v0.1**；当前冻结序列见 `Docs/agent-iteration/roadmap/iteration-blueprint.md` **v2.0**。下一建议动作是 Type B H0 `truth-surface-cleanup`，随后先做 E0 原型；P3.1 及以后任一 Type C 均须独立 OpenSpec change，蓝图不构成实现授权。
- V2.0 核心表达： 写下此刻。
- V2.0 版本性质： 用于展示与评审的 WeChat Mini Program 演示版，不是生产上线版本。
- 当前验收目标： 仅验收 WeChat Mini Program。
- 事实源优先级： V2.0 OpenSpec 文档对 V2.0 工作具有最高优先级。旧的 `Docs/**` 文件与 OpenSpec 冲突时，视为历史参考。
- OpenSpec 是迭代文档：随着后续模块被确认，这些文档 SHOULD 持续演进。

## Product Intention / 产品初心

《时光回序》不是把焦虑寄托给未来的工具，也不是逃避当下的出口。

它帮助用户写下当下的情绪、困惑、期待、犹豫与生活片段。未来回看不是产品唯一承诺；它更像是把回答权交给时间，让未来的自己重新理解写下这一刻的自己。

产品气质 SHOULD 安静、私密、克制、温柔，并带有时间感。它 MUST 尊重表达本身：即使未来的自己没有回信，最初写下的记录依然成立。

## V2.0 Core Loop / V2.0 核心闭环

> 以下是当前 accepted baseline 的既有闭环，不代表蓝图 v2.0 已全部实现。其与“此刻片段 / 已保存 / 封存”目标状态之间的差异，必须通过后续独立 Type C change 才能改变契约与代码。

```text
首页：写下此刻
  -> 新建记录：选择记录类型 / 写给谁 / 写下内容
  -> 保存此刻 或 交给时间
  -> 我的记录 / 时光轴：看到不同状态的记录
  -> 记录抵达：进入时间回看
  -> 时间回看：那时的我 / 现在的我
  -> 可选回信：不是强制流程
```

## Version Boundaries / 版本边界

V2.0 includes / V2.0 包含：

- 在完整 V2.0 计划中，frontend 和 backend 都可以分模块演进。
- 可演示的用户侧 WeChat Mini Program 流程是优先事项。
- 在工程执行前，SHOULD 先明确产品叙事、视觉系统与用户侧页面职责。
- WeChat subscription message capability 属于 V2.0 规划，不属于 M1；M2 可纳入记录到期提醒的最小基础闭环，但不包含生产级通知中心或短信提醒。
- V2.0 演示允许 one-click preview / no-login demo mode；除非后续 spec 修改，否则基于现有 `frontend/src/features/preview` 机制。

V2.0 does not include / V2.0 不包含：

- 真实 WeChat production release。
- Management/admin portal。
- 生产部署加固。
- 线上监控与告警。
- H5 或 Web 用户端验收目标；这些方案在 V2.0 中只作为历史参考。

除非后续 OpenSpec change 明确提前纳入，否则上述事项保留到 V3.0。

## M1 Boundary / M1 边界

M1 不是 theme skin，不是配色替换，不是旧页面结构上的局部美化。M1 是将 `openspec/changes/m1-frontend-visual-foundation/visual-reference-map.md` 中锁定的 exact finalized HTML，高保真转译为 WeChat Mini Program 页面。

M1 只做 frontend visual foundation refactor。

M1 may define and later implement / M1 可以定义并后续实现：

- WeChat Mini Program visual language 与可复用 visual tokens。
- App page shell、navigation、spacing、typography、color、card、button、status，以及 empty/loading/error 展示规则。
- 面向用户侧 mini program 核心流程的页面级视觉对齐。
- 必要时基于现有 preview/mock 机制实现 demo-friendly visual states。
- 为支撑 V2.0 表达而需要的页面标题、动作、状态标签、空状态 / demo 状态等 microcopy 调整。

M1 must not implement / M1 MUST NOT 实现：

- Backend API 或 database changes。
- 记录状态流转的 business-rule changes。
- 新的 production notification behavior。
- Admin portal behavior。
- 真实 production deployment、monitoring 或 launch tasks。
- AI capability changes。
- 真实 MAP / IMAGE / VOICE features。这些可以作为面向未来的视觉提示保留，但 MUST NOT 暗示 V2.0 已具备可工作的真实功能。

## M1 High-Fidelity Visual Translation Rule / M1 高保真视觉转译规则

M1 is a high-fidelity visual translation of the exact finalized HTML files listed in `openspec/changes/m1-frontend-visual-foundation/visual-reference-map.md` into the WeChat Mini Program frontend.

M1 MUST NOT be treated as a theme skin, color-token replacement, or minor polish pass over the existing V1 page structure.

The existing frontend implementation MAY be used for routing, data shape, state display, and business-safe behavior. However, when the existing page hierarchy, spacing, visual density, card structure, bottom navigation behavior, or component composition conflicts with the finalized HTML, the finalized HTML MUST win.

M1 MUST use:

- `openspec/changes/m1-frontend-visual-foundation/visual-reference-map.md`
- `Docs/design/home-v2/时光回序_设计规范.md`
- the exact finalized HTML files listed in the visual reference map

The glob `Docs/design/home-v2/*.html` is not sufficient by itself for implementation. The exact canonical files MUST be listed before visual implementation begins.

## Current Codebase Facts / 当前代码事实

- Frontend：Uniapp + Vue 3 + Pinia，目标为 WeChat Mini Program。
- Frontend package：`frontend`，应用源码位于 `frontend/src`。
- 当前主要页面包括 login、home、record editor、record list、record detail、timeline 和 user center。
- 当前记录状态为 `DRAFT`、`SEALED` 和 `UNLOCKED`。
- 当前记录类型为 `FUTURE_LETTER`、`NODE_RECORD` 和 `EMOTION_NOTE`。
- 现有 V1.0.1 用户侧基线包含 authentication、record draft/seal/list/detail、unlocked records、replies、tags、timeline、unlock task，以及最小 AI fallback ability。
- V2.0 用户可见命名 SHOULD 使用：我的记录、时光轴、时间回看。
- M1 视觉方向已确认为 `Docs/design/home-v2/时光回序_设计规范.md` 中的 V2 paper/vermilion direction。
- M1 视觉实现 MUST 以 `visual-reference-map.md` 中列出的 exact finalized HTML files 和 `Docs/design/home-v2/时光回序_设计规范.md` 为准。Agent MUST perform high-fidelity visual translation and MUST NOT keep the existing frontend layout when it conflicts with the finalized HTML.
- M1 抽象节奏 MUST 遵循“局部页面稳定后再抽取 tokens / shared components”，禁止一开始大规模抽象。

## Source References / 参考来源

- `Docs/开发文档/V1.0.1收尾说明.md`
- `Docs/前期需求设计文档/需求分析.md`
- `Docs/前期需求设计文档/设计文档.md`
- `Docs/前端文档/flashback_frontend_dev_spec.md`
- `openspec/changes/m1-frontend-visual-foundation/visual-reference-map.md`
- `Docs/design/home-v2/时光回序_设计规范.md`
- `frontend/src/pages.json`
- `frontend/src/types/enums.ts`
- `frontend/src/styles/tokens.css`
