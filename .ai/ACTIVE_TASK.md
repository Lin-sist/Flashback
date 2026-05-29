# Active Task

## Task

Complete the remaining M1 User Center settings-style subpage reconstruction, then prepare for M1 frontend polish/acceptance.

The current goal is to high-fidelity restore all five subpages from the existing prototype bundle `Docs/design/home-v2/个人主页_子页面.html`.

That HTML file already contains all five subpage prototypes. In the browser prototype, the five pages are switched by the page buttons. Implementation Agents MUST open this exact HTML bundle and the matching PNG snapshot for each page, then translate the visual structure into Mini Program pages with high fidelity. The old frontend settings pages are routing/business references only; they are not visual references.

1. `整理偏好`
2. `视觉外观`
3. `访问控制`
4. `数据备份`
5. `版本信息`

## Source Of Truth

- `AGENTS.md`
- `openspec/changes/m1-frontend-visual-foundation/visual-reference-map.md`
- `openspec/changes/m1-frontend-visual-foundation/design.md`
- `openspec/changes/m1-frontend-visual-foundation/tasks.md`
- `Docs/design/home-v2/个人主页_子页面.html`
- `Docs/design/home-v2/整理偏好.png`
- `Docs/design/home-v2/视觉外观.png`
- `Docs/design/home-v2/访问控制.png`
- `Docs/design/home-v2/数据备份.png`
- `Docs/design/home-v2/版本信息.png`

## Confirmed Routes

- `整理偏好` -> `frontend/src/pages/user-center/archive-preference/index.vue`
- `视觉外观` -> `frontend/src/pages/user-center/visual-appearance/index.vue`
- `访问控制` -> `frontend/src/pages/user-center/access-control/index.vue`
- `数据备份` -> `frontend/src/pages/user-center/data-backup/index.vue`
- `版本信息` -> `frontend/src/pages/user-center/about/index.vue`

## Allowed Files

Prefer only:

- `frontend/src/pages.json`
- `frontend/src/pages/user-center/index.vue`
- `frontend/src/pages/user-center/archive-preference/index.vue`
- `frontend/src/pages/user-center/about/index.vue`
- `frontend/src/pages/user-center/visual-appearance/index.vue`
- `frontend/src/pages/user-center/access-control/index.vue`
- `frontend/src/pages/user-center/data-backup/index.vue`
- `.ai/AGENT_LOG.md`
- `openspec/changes/m1-frontend-visual-foundation/tasks.md`

If necessary, frontend-only style/token/helper files may be read or modified, but the Agent must explain why.

## Legacy Route Constraint

- Keep `frontend/src/pages/user-center/tag-manage/index.vue` unchanged unless the task explicitly expands to legacy tag management.
- Keep `frontend/src/pages/user-center/notify-settings/index.vue` unchanged unless the task explicitly expands to legacy notification settings.
- Do not silently repurpose `tag-manage` as `视觉外观`.
- Do not silently repurpose `notify-settings` as `访问控制` or `数据备份`.

## Requirements

### High-Fidelity Reconstruction

- Open `Docs/design/home-v2/个人主页_子页面.html` before editing.
- Treat `Docs/design/home-v2/个人主页_子页面.html` as the canonical prototype bundle, not loose inspiration.
- Use the exact PAGE 0-4 structure and matching screenshots as the visual target:
  - PAGE 0 `整理偏好` + `Docs/design/home-v2/整理偏好.png`
  - PAGE 1 `视觉外观` + `Docs/design/home-v2/视觉外观.png`
  - PAGE 2 `访问控制` + `Docs/design/home-v2/访问控制.png`
  - PAGE 3 `数据备份` + `Docs/design/home-v2/数据备份.png`
  - PAGE 4 `版本信息` + `Docs/design/home-v2/版本信息.png`
- Preserve the quiet paper/vermilion visual language.
- Replace old `AppTopBar + PaperContainer` settings-page skeletons when they conflict with the canonical HTML/prototype snapshot.
- Do not use old route names or old page implementations as a reason to keep old visual structure.
- Keep these pages secondary; do not add first-level tabs.

### Routing

- Add confirmed routes for `visual-appearance`、`access-control`、`data-backup` in `frontend/src/pages.json`.
- Update `frontend/src/pages/user-center/index.vue` setting item routing so all five canonical entries navigate to the correct subpages.
- Keep legacy `tag-manage` and `notify-settings` routes available unless a separate task says otherwise.
- New route names are allowed and preferred when they make the canonical subpage ownership clearer.

### Behavior Scope

- Local-only demo/settings state is acceptable for visual controls.
- Do not implement real subscription-message behavior.
- Do not implement real backup/import/export capability.
- Do not implement real access-control/security capability.
- Do not modify backend, database, schema, business rules, record state transition, AI, package, or lockfile.

## Verification Required

- Run `cd frontend; npm run build:mp-weixin` when feasible.
- Confirm all five User Center entries navigate to the intended subpage route.
- Check small-screen and iPhone safe-area behavior.
- Confirm visual-only controls do not imply completed backend/production capability beyond local/demo state.
- Update `openspec/changes/m1-frontend-visual-foundation/tasks.md` checkboxes only for tasks actually completed.
- Write a short handoff in `.ai/AGENT_LOG.md`.

## Output Required

- modified files
- implementation summary
- validation result
- skipped verification reason, if any
- `git diff --stat`
- scope safety check
- unresolved questions
- remaining risks
