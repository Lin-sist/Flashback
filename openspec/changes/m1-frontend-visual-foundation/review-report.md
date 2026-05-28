# M1 OpenSpec High-Fidelity Visual Translation Review Report

## 1. Executive Summary / 摘要

This documentation pass tightens M1 from a broad "frontend visual foundation refactor" into a strict "high-fidelity visual translation based on finalized HTML".

M1 now explicitly requires Home、Timeline、User Center to be restored from exact canonical HTML files in `Docs/design/home-v2/`, instead of allowing old frontend pages to pass through color/token-only restyling.

## 2. Files Reviewed / 已审查文件

OpenSpec files reviewed:

- `openspec/project.md`
- `openspec/changes/m1-frontend-visual-foundation/proposal.md`
- `openspec/changes/m1-frontend-visual-foundation/design.md`
- `openspec/changes/m1-frontend-visual-foundation/tasks.md`
- `openspec/changes/m1-frontend-visual-foundation/specs/frontend-visual-foundation/spec.md`

Design reference files reviewed:

- `Docs/design/home-v2/首页.html`
- `Docs/design/home-v2/首页.png`
- `Docs/design/home-v2/时光回序_时光轴页面.html`
- `Docs/design/home-v2/时光轴.png`
- `Docs/design/home-v2/个人主页.html`
- `Docs/design/home-v2/个人中心.png`
- `Docs/design/home-v2/时光回序_设计规范.md`

Routing file reviewed:

- `frontend/src/pages.json`

## 3. Files Changed / 已修改文件

Modified:

- `openspec/project.md`
- `openspec/changes/m1-frontend-visual-foundation/proposal.md`
- `openspec/changes/m1-frontend-visual-foundation/design.md`
- `openspec/changes/m1-frontend-visual-foundation/tasks.md`
- `openspec/changes/m1-frontend-visual-foundation/specs/frontend-visual-foundation/spec.md`

Added:

- `openspec/changes/m1-frontend-visual-foundation/visual-reference-map.md`
- `openspec/changes/m1-frontend-visual-foundation/review-report.md`

## 4. Canonical Reference Status / 定稿参考状态

Canonical HTML mapping is confirmed for all primary M1 pages:

| Page | Mini Program target | Canonical HTML | Screenshot | Status |
| --- | --- | --- | --- | --- |
| Home / 首页 | `pages/home/index` | `Docs/design/home-v2/首页.html` | `Docs/design/home-v2/首页.png` | confirmed |
| Timeline / 时光轴 | `pages/timeline/index` | `Docs/design/home-v2/时光回序_时光轴页面.html` | `Docs/design/home-v2/时光轴.png` | confirmed |
| User Center / 我的 | `pages/user-center/index` | `Docs/design/home-v2/个人主页.html` | `Docs/design/home-v2/个人中心.png` | confirmed |

No `NEEDS_OWNER_CONFIRMATION` item was found because each primary page has one clear HTML candidate in `Docs/design/home-v2/`.

## 5. Key Constraint Improvements / 关键约束增强

- Not skinning: M1 cannot pass as color-only, token-only, or old-structure restyling.
- Exact canonical HTML mapping: implementation must use `visual-reference-map.md`, not only `Docs/design/home-v2/*.html`.
- Page-level high-fidelity contracts: Home、Timeline、User Center now each have explicit required structures and fail conditions.
- Bottom navigation and scroll safety: bottom nav overlap is written as MUST NOT and fail condition.
- Visual comparison evidence: M1 completion requires evidence for each primary page.
- Token extraction after page stabilization: tokens/shared components must follow stable page-level restoration.
- No scope creep: backend、database、schema、admin、deployment、monitoring、business-rule、AI、subscription-message, and real multimedia implementation changes remain out of scope.

## 6. Remaining Risks / 剩余风险

- HTML file name risk: current mapping is clear, but future additional HTML variants must be marked `NEEDS_OWNER_CONFIRMATION` before implementation.
- Mini Program rendering risk: HTML uses browser CSS and fonts that may differ from WeChat Mini Program rendering; acceptable differences must be documented and must not become structural drift.
- Bottom safe-area risk: fixed or floating navigation can still cover content on long states unless tested on bottom scroll states.
- Early abstraction risk: Agents may still try to start from tokens/components; tasks now force reference lock and page restoration first.
- Target screenshot risk: screenshots exist for all primary pages, but future implementation still needs visual comparison evidence from the Mini Program target or an equivalent review.
- User Center terminology risk: current canonical file is `个人主页.html`; if a newer owner-approved personal-control layout appears later, `visual-reference-map.md` must be updated before implementation.

## 7. Acceptance Checklist / 验收清单

- [ ] Home high-fidelity restored against `Docs/design/home-v2/首页.html`.
- [ ] Timeline high-fidelity restored against `Docs/design/home-v2/时光回序_时光轴页面.html`.
- [ ] User Center high-fidelity restored against `Docs/design/home-v2/个人主页.html`.
- [ ] Bottom nav has no overlap on Home、Timeline、User Center bottom scroll states.
- [ ] Visual evidence is complete for all three primary pages.
- [ ] No code / backend / database / schema / admin / deployment / monitoring / business-rule / AI / subscription-message changes.
- [ ] Tokens and shared components are extracted only after stable primary pages.

## 8. Recommendation For Next Implementation Agent / 给下一轮实现 Agent 的建议

Before editing anything, open `visual-reference-map.md` and the exact canonical HTML for the target page. Do not begin by changing tokens or building a component library. Do not preserve old page skeletons and call it a visual pass. Restore Home、Timeline、User Center first, compare them against canonical HTML/screenshots, verify bottom navigation does not overlap content, and only then extract shared tokens/components from stable repeated structures.
