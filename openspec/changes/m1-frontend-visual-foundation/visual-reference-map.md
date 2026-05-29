# M1 Visual Reference Map / M1 视觉参考映射

## Purpose / 目的

This file locks the exact visual references for M1. M1 is a high-fidelity frontend visual translation from finalized HTML in `Docs/design/home-v2/` into the WeChat Mini Program pages.

M1 MUST NOT be treated as a theme skin, color replacement, token-only refactor, or minor polish pass over the existing frontend pages.

## Files Found In `Docs/design/home-v2/` / 已发现设计文件

| File | Type | Notes |
| --- | --- | --- |
| `Docs/design/home-v2/首页.html` | finalized HTML | Home / 首页 canonical reference |
| `Docs/design/home-v2/首页.png` | screenshot | Home / 首页 screenshot reference |
| `Docs/design/home-v2/时光轴.html` | finalized HTML | Timeline / 时光轴 canonical reference |
| `Docs/design/home-v2/时光轴.png` | screenshot | Timeline / 时光轴 screenshot reference |
| `Docs/design/home-v2/个人主页.html` | finalized HTML | User Center / 我的 canonical reference |
| `Docs/design/home-v2/个人主页.png` | screenshot | User Center / 我的 screenshot reference |
| `Docs/design/home-v2/时光回序_设计规范.md` | design spec | Shared paper/vermilion visual language |
| `Docs/design/home-v2/登录.html` | finalized HTML | Login / 登录 canonical reference |
| `Docs/design/home-v2/登录.png` | screenshot | Login / 登录 screenshot reference |
| `Docs/design/home-v2/新建.html` | finalized HTML | New Record / 新建记录 canonical reference |
| `Docs/design/home-v2/新建.png` | screenshot | New Record / 新建记录 screenshot reference |
| `Docs/design/home-v2/解锁.html` | finalized HTML | Archive Detail / 封存回看 SEALED state canonical reference |
| `Docs/design/home-v2/解锁.png` | screenshot | Archive Detail / 封存回看 SEALED state screenshot reference |
| `Docs/design/home-v2/我的档案.html` | finalized HTML | My Records / 我的记录 canonical reference |
| `Docs/design/home-v2/我的档案.png` | screenshot | My Records / 我的记录 screenshot reference |
| `Docs/design/home-v2/回看.html` | finalized HTML | Time Review / 时间回看 UNLOCKED state canonical reference |
| `Docs/design/home-v2/回看.png` | screenshot | Time Review / 时间回看 UNLOCKED state screenshot reference |
| `Docs/design/home-v2/个人主页_子页面.html` | finalized HTML bundle | User Center settings-style subpage canonical bundle; contains 整理偏好、视觉外观、访问控制、数据备份、版本信息 |
| `Docs/design/home-v2/整理偏好.png` | screenshot | Settings subpage screenshot for 整理偏好 |
| `Docs/design/home-v2/视觉外观.png` | screenshot | Settings subpage screenshot for 视觉外观 |
| `Docs/design/home-v2/访问控制.png` | screenshot | Settings subpage screenshot for 访问控制 |
| `Docs/design/home-v2/数据备份.png` | screenshot | Settings subpage screenshot for 数据备份 |
| `Docs/design/home-v2/版本信息.png` | screenshot | Settings subpage screenshot for 版本信息 |

## Canonical Priority / 事实优先级

When references conflict, implementation and review MUST use this priority:

1. The exact finalized HTML file listed in this `visual-reference-map.md`.
2. `Docs/design/home-v2/时光回序_设计规范.md`.
3. Target screenshots captured from the finalized HTML.
4. Existing `frontend` implementation, only for routing, data shape, state display, and business-safe behavior.
5. Historical V1, archive, or old `Docs/**` materials.

The glob `Docs/design/home-v2/*.html` is not sufficient as an implementation target. Agents MUST open the exact canonical HTML file for the page they are changing.

## Primary M1 Pages / M1 一级页面

| Page | Mini Program target | Canonical HTML | Reference screenshot | Required fidelity | Status |
| --- | --- | --- | --- | --- | --- |
| Home / 首页 | `pages/home/index` | `Docs/design/home-v2/首页.html` | `Docs/design/home-v2/首页.png` | High fidelity | confirmed |
| Timeline / 时光轴 | `pages/timeline/index` | `Docs/design/home-v2/时光轴.html` | `Docs/design/home-v2/时光轴.png` | High fidelity | confirmed |
| User Center / 我的 | `pages/user-center/index` | `Docs/design/home-v2/个人主页.html` | `Docs/design/home-v2/个人主页.png` | High fidelity | confirmed |

## Secondary Pages / 二级页面

Secondary pages MAY be visually aligned after the primary pages are stable, but they MUST NOT block primary page high-fidelity completion.

| Page | Mini Program target | Canonical HTML | Reference screenshot | Required fidelity | Status |
| --- | --- | --- | --- | --- | --- |
| Login / 登录 | `pages/login/index` | `Docs/design/home-v2/登录.html` | `Docs/design/home-v2/登录.png` | High fidelity | confirmed |
| New Record / 新建记录 | `pages/record-editor/index` | `Docs/design/home-v2/新建.html` | `Docs/design/home-v2/新建.png` | High fidelity | confirmed |
| Archive Detail / 封存回看 | `pages/record-detail/index` | `Docs/design/home-v2/解锁.html` | `Docs/design/home-v2/解锁.png` | High fidelity (SEALED state) | confirmed |
| My Records / 我的记录 | `pages/record-list/index` | `Docs/design/home-v2/我的档案.html` | `Docs/design/home-v2/我的档案.png` | High fidelity | confirmed |
| Time Review / 时间回看 | `pages/record-detail/index` (UNLOCKED state) | `Docs/design/home-v2/回看.html` | `Docs/design/home-v2/回看.png` | High fidelity | confirmed |
| Archive Preference / 整理偏好 | `pages/user-center/archive-preference/index` | `Docs/design/home-v2/个人主页_子页面.html` (PAGE 0) | `Docs/design/home-v2/整理偏好.png` | High fidelity | confirmed |
| Visual Appearance / 视觉外观 | `pages/user-center/visual-appearance/index` | `Docs/design/home-v2/个人主页_子页面.html` (PAGE 1) | `Docs/design/home-v2/视觉外观.png` | High fidelity | confirmed |
| Access Control / 访问控制 | `pages/user-center/access-control/index` | `Docs/design/home-v2/个人主页_子页面.html` (PAGE 2) | `Docs/design/home-v2/访问控制.png` | High fidelity | confirmed |
| Data Backup / 数据备份 | `pages/user-center/data-backup/index` | `Docs/design/home-v2/个人主页_子页面.html` (PAGE 3) | `Docs/design/home-v2/数据备份.png` | High fidelity | confirmed |
| Version Info / 版本信息 | `pages/user-center/about/index` | `Docs/design/home-v2/个人主页_子页面.html` (PAGE 4) | `Docs/design/home-v2/版本信息.png` | High fidelity | confirmed |
| Tag Manage / 标签管理 | `pages/user-center/tag-manage/index` | — | — | no dedicated canonical target in current M1 references | pending |
| Notify Settings / 通知设置 | `pages/user-center/notify-settings/index` | — | — | no subscription-message implementation in M1; do not map to Access Control without owner confirmation | pending |

## Non-Goals / 非目标

- M1 MUST NOT accept a color-only, token-only, or skin-only refactor.
- M1 MUST NOT keep an old page skeleton when it conflicts with the finalized HTML.
- Agents MUST NOT redesign the 宣纸朱砂 style as a personal interpretation.
- Bottom navigation MUST NOT cover or collide with page content.
- Secondary pages MUST NOT become scope creep that blocks Home、Timeline、User Center high-fidelity restoration.
- M1 MUST NOT implement backend、database、schema、deployment、admin、AI、subscription-message、business-rule, or real MAP / IMAGE / VOICE capability changes.

## Canonical Mapping Table / 定稿映射表

| Canonical page | Exact HTML source | Screenshot source | Mini Program route | Key structure that MUST win over existing frontend |
| --- | --- | --- | --- | --- |
| Home / 首页 | `Docs/design/home-v2/首页.html` | `Docs/design/home-v2/首页.png` | `pages/home/index` | centered identity, hero question, dominant writing CTA, bottom archive/stat summary exactly as canonical Home reference, calm bottom nav |
| Timeline / 时光轴 | `Docs/design/home-v2/时光轴.html` | `Docs/design/home-v2/时光轴.png` | `pages/timeline/index` | safe topbar, `时间长廊` title, compact vertical timeline, record status hierarchy, bottom helper copy |
| User Center / 我的 | `Docs/design/home-v2/个人主页.html` | `Docs/design/home-v2/个人主页.png` | `pages/user-center/index` | PERSONAL CONTROL identity when present in canonical reference, username/tagline hierarchy, two stat cards, grouped settings sections, about/version section, calm bottom nav |
| Login / 登录 | `Docs/design/home-v2/登录.html` | `Docs/design/home-v2/登录.png` | `pages/login/index` | paper bg, 时光回序 logo, 久违了/时间的旅人 headline, login/register tab row, minimal underline input fields, 进入档案馆 CTA with corner marks + vermilion dot, 忘记密码 text |
| New Record / 新建 | `Docs/design/home-v2/新建.html` | `Docs/design/home-v2/新建.png` | `pages/record-editor/index` | Vol.N topbar + close, letter-body card with topline, Captured at + date + archive-tag, side-rule, ruled lines, editor textarea, attach-bar MAP/IMAGE/VOICE, word count + 封存这一刻 button + hint |
| Archive Detail / 封存回看 | `Docs/design/home-v2/解锁.html` | `Docs/design/home-v2/解锁.png` | `pages/record-detail/index` (SEALED state) | 时光回序 logo, close btn, Archive No. + season, deco line, letter-card with seal/过去的你/location/blurred body/sparkle, countdown digits, 留下回应 CTA, sub-hint |
| My Records / 我的记录 | `Docs/design/home-v2/我的档案.html` | `Docs/design/home-v2/我的档案.png` | `pages/record-list/index` | back title `我的档案`, search, status filter tabs, 档案概览 summary, paper cards with status badges, write CTA, quiet footnote |
| Time Review / 时间回看 | `Docs/design/home-v2/回看.html` | `Docs/design/home-v2/回看.png` | `pages/record-detail/index` (UNLOCKED state) | 时光回序 logo, close btn, archive no/location, season, quote, seal row, readable letter card, 留下回应 CTA, 收入时光轴 secondary action, reply sheet visual only |
| Archive Preference / 整理偏好 | `Docs/design/home-v2/个人主页_子页面.html` (PAGE 0) | `Docs/design/home-v2/整理偏好.png` | `pages/user-center/archive-preference/index` | top back/title, grouped paper cards, toggle rows, default seal-period radio list, writing assistance rows, save CTA |
| Visual Appearance / 视觉外观 | `Docs/design/home-v2/个人主页_子页面.html` (PAGE 1) | `Docs/design/home-v2/视觉外观.png` | `pages/user-center/visual-appearance/index` | paper tone swatches, font size choices, line-height slider, display toggles, save CTA |
| Access Control / 访问控制 | `Docs/design/home-v2/个人主页_子页面.html` (PAGE 2) | `Docs/design/home-v2/访问控制.png` | `pages/user-center/access-control/index` | lock method radio rows, auto-lock slider, access record summary, privacy protection toggles, save CTA |
| Data Backup / 数据备份 | `Docs/design/home-v2/个人主页_子页面.html` (PAGE 3) | `Docs/design/home-v2/数据备份.png` | `pages/user-center/data-backup/index` | backup status badge, storage summary, auto backup rows, manual export/import actions, danger-zone action |
| Version Info / 版本信息 | `Docs/design/home-v2/个人主页_子页面.html` (PAGE 4) | `Docs/design/home-v2/版本信息.png` | `pages/user-center/about/index` | app mark, version label, update log, about rows, quiet quote card |

## Route Pending Notes / 路由待确认说明

`个人主页_子页面.html` locks visual references for five settings-style subpages. This HTML file is a prototype bundle: the browser prototype switches between the five subpages through page buttons, but all five prototypes live in this one canonical HTML file. The matching PNG snapshots in `Docs/design/home-v2/` are verification references for those same prototype pages.

M1 now confirms dedicated Mini Program routes for all five pages:

- `整理偏好` -> `pages/user-center/archive-preference/index`
- `视觉外观` -> `pages/user-center/visual-appearance/index`
- `访问控制` -> `pages/user-center/access-control/index`
- `数据备份` -> `pages/user-center/data-backup/index`
- `版本信息` -> `pages/user-center/about/index`

Implementation MUST be high-fidelity reconstruction against the HTML bundle and PNG snapshots. Old frontend route names or existing settings-page skeletons MUST NOT be treated as visual references.

Legacy `tag-manage` and `notify-settings` remain non-canonical M1 routes. M1 Agents MUST NOT silently repurpose them for `视觉外观`、`访问控制`、or `数据备份`. If those legacy routes are kept, hidden, or removed later, that decision belongs to a separate task unless explicitly included in the current implementation scope.
