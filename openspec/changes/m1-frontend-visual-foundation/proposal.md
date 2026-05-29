# M1 Frontend Visual Foundation / M1 前端视觉基础

## Summary / 摘要

M1 refactors the WeChat Mini Program frontend visual foundation by translating the finalized HTML in `Docs/design/home-v2/` with high fidelity.

M1 is now explicitly scoped as: 基于 `Docs/design/home-v2/` 定稿 HTML 的高保真前端视觉转译.

Home、Timeline、User Center are the first primary M1 high-fidelity pages. Their exact canonical HTML and screenshot references MUST be read from `visual-reference-map.md`.

M1 MUST NOT be accepted as an old-structure reskin. It is not a theme skin, color-token replacement, token-only refactor, or minor polish pass over V1 pages.

This change may affect frontend visual presentation, page structure, page shell, visual tokens, shared presentation components, and demo-safe visual states. It MUST NOT implement backend、database、production notification、admin、deployment、monitoring、AI、subscription-message、real MAP / IMAGE / VOICE, or business-rule changes.

## Motivation / 背景动机

V1.0.1 already has a usable user-side baseline, but V2.0 needs a clearer demo identity. The product SHOULD feel like a quiet place to write down the present and later reread it through time, not a generic record-management dashboard.

The first M1 implementation attempt showed that "visual foundation" can be misread as a color/style pass. This proposal tightens M1 so execution Agents MUST rebuild page hierarchy, spacing, typography, card composition, navigation placement, scroll behavior, and safe-area behavior when the existing frontend conflicts with finalized HTML.

## Scope / 范围

In scope / 范围内：

- High-fidelity translation of finalized Home、Timeline、User Center HTML pages into WeChat Mini Program pages.
- High-fidelity translation of confirmed settings-style User Center subpages in `Docs/design/home-v2/个人主页_子页面.html`: 整理偏好、视觉外观、访问控制、数据备份、版本信息.
- Exact canonical mapping through `visual-reference-map.md`.
- Page hierarchy, visual center, spacing rhythm, typography hierarchy, card composition, action priority, navigation placement, scroll behavior, safe-area behavior, and visual density from canonical HTML.
- Replacing existing frontend structure when it conflicts with canonical HTML.
- Bottom navigation and scroll-content safe-area rules that prevent overlap.
- Visual tokens and shared components extracted after primary page restoration is stable.
- Demo-friendly visual states using existing `frontend/src/features/preview` where needed.
- Visible V2.0 naming and microcopy alignment: 我的记录、时光轴、时间回看.
- WeChat Mini Program visual review and build/smoke-check evidence where feasible.

Out of scope / 范围外：

- Backend API changes.
- Database or schema changes.
- Record status business-rule changes.
- WeChat subscription-message implementation.
- Admin portal.
- Production deployment、observability、monitoring, or launch tasks.
- AI capability changes.
- Real MAP / IMAGE / VOICE feature implementation.
- Arbitrary redesign beyond canonical HTML.
- Treating the visual spec as loose inspiration while ignoring canonical HTML.
- Accepting color-only, token-only, skin-only, or old-skeleton refactors as M1 completion.
- Expanding secondary pages until they block Home、Timeline、User Center high-fidelity restoration.
- Reusing legacy `tag-manage` or `notify-settings` as silent substitutes for canonical settings-style subpages.

## Success Criteria / 成功标准

- Home、Timeline、User Center MUST visually match the exact canonical HTML listed in `visual-reference-map.md`.
- Page hierarchy, spacing, typography levels, card/surface composition, navigation placement, scroll behavior, safe-area behavior, and action priority MUST be visibly equivalent to canonical HTML.
- M1 MUST be incomplete if it only changes colors, fonts, shadows, radius, or tokens while page composition remains materially different.
- Bottom navigation MUST NOT overlap scroll content, statistics, helper text, action buttons, cards, settings rows, or about/version sections.
- Each primary page MUST include visual comparison evidence against canonical HTML and/or final screenshot references.
- Any unavoidable Mini Program rendering difference MUST be documented.
- Tokens / shared components MUST be extracted only after Home、Timeline、User Center page-level restoration is stable.
- Diff review MUST confirm no backend、database、schema、admin、deployment、monitoring、AI、subscription-message、business-rule, or real multimedia implementation files were modified.

## Confirmed Decisions / 已确认决策

- V2.0 OpenSpec has highest priority over old project documents.
- M1 visual direction is 宣纸朱砂 / paper-vermilion.
- M1 MUST use exact canonical HTML files listed in `visual-reference-map.md`, not only the glob `Docs/design/home-v2/*.html`.
- Home、Timeline、User Center are the first primary high-fidelity pages.
- Existing frontend MAY provide routing, data shape, state display, and business-safe behavior only.
- When existing frontend structure conflicts with canonical HTML, canonical HTML MUST win.
- M1 MUST restore primary pages before extracting visual tokens or shared components.
- M1 MUST NOT start with a component-library-first implementation.
- V2.0 visible names are 我的记录、时光轴、时间回看.
- Existing `frontend/src/features/preview` remains the initial demo data path.
- M1 confirms dedicated settings-style subpage routes for `视觉外观`、`访问控制`、and `数据备份`; legacy `tag-manage` and `notify-settings` are not canonical M1 targets unless a later task explicitly remaps or retires them.
- One-click preview / no-login demo mode is allowed for V2.0 demo.
- V2.0 does not include management/admin portal.
- H5/Web user-side方案 is historical reference only, not M1 acceptance target.
- WeChat subscription messages belong to later V2.0 modules and MUST NOT be implemented in M1.
