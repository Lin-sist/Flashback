# M1 Visual Reference Map / M1 视觉参考映射

## Purpose / 目的

This file locks the exact visual references for M1. M1 is a high-fidelity frontend visual translation from finalized HTML in `Docs/design/home-v2/` into the WeChat Mini Program pages.

M1 MUST NOT be treated as a theme skin, color replacement, token-only refactor, or minor polish pass over the existing frontend pages.

## Files Found In `Docs/design/home-v2/` / 已发现设计文件

| File | Type | Notes |
| --- | --- | --- |
| `Docs/design/home-v2/首页.html` | finalized HTML | Home / 首页 canonical reference |
| `Docs/design/home-v2/首页.png` | screenshot | Home / 首页 screenshot reference |
| `Docs/design/home-v2/时光回序_时光轴页面.html` | finalized HTML | Timeline / 时光轴 canonical reference |
| `Docs/design/home-v2/时光轴.png` | screenshot | Timeline / 时光轴 screenshot reference |
| `Docs/design/home-v2/个人主页.html` | finalized HTML | User Center / 我的 canonical reference |
| `Docs/design/home-v2/个人中心.png` | screenshot | User Center / 我的 screenshot reference |
| `Docs/design/home-v2/时光回序_设计规范.md` | design spec | Shared paper/vermilion visual language |
| `Docs/design/home-v2/新建.html` | finalized HTML |  |
| `Docs/design/home-v2/新建.png` | screenshot |  |
| `Docs/design/home-v2/解锁.html` | finalized HTML |  |
| `Docs/design/home-v2/解锁.png` | screenshot |  |
| `Docs/design/home-v2/登录.html` | finalized HTML |  |
| `Docs/design/home-v2/登录.png` | screenshot |  |

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
| Timeline / 时光轴 | `pages/timeline/index` | `Docs/design/home-v2/时光回序_时光轴页面.html` | `Docs/design/home-v2/时光轴.png` | High fidelity | confirmed |
| User Center / 我的 | `pages/user-center/index` | `Docs/design/home-v2/个人主页.html` | `Docs/design/home-v2/个人中心.png` | High fidelity | confirmed |

## Secondary Pages / 二级页面

Secondary pages MAY be visually aligned after the primary pages are stable, but they MUST NOT block primary page high-fidelity completion.

| Page | Mini Program target | Canonical HTML | Reference screenshot | Required fidelity | Status |
| --- | --- | --- | --- | --- | --- |
| Login / 登录 | `pages/login/index` | `Docs/design/home-v2/登录.html` | `Docs/design/home-v2/登录.png` | High fidelity | confirmed |
| New Record / 新建记录 | `pages/record-editor/index` | `Docs/design/home-v2/新建.html` | `Docs/design/home-v2/新建.png` | High fidelity | confirmed |
| Archive Detail / 封存回看 | `pages/record-detail/index` | `Docs/design/home-v2/解锁.html` | `Docs/design/home-v2/解锁.png` | High fidelity (SEALED state) | confirmed |
| My Records / 我的记录 | `pages/record-list/index` | — | — | align after primary pages | pending |
| Archive Preference / 整理偏好 | `pages/user-center/archive-preference/index` | — | — | align after primary pages | pending |
| Tag Manage / 标签管理 | `pages/user-center/tag-manage/index` | — | — | align after primary pages | pending |
| Notify Settings / 通知设置 | `pages/user-center/notify-settings/index` | — | — | visual-only, no subscription-message implementation | pending |
| About / 关于 | `pages/user-center/about/index` | — | — | align after primary pages | pending |

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
| Timeline / 时光轴 | `Docs/design/home-v2/时光回序_时光轴页面.html` | `Docs/design/home-v2/时光轴.png` | `pages/timeline/index` | safe topbar, `时间长廊` title, compact vertical timeline, record status hierarchy, bottom helper copy |
| User Center / 我的 | `Docs/design/home-v2/个人主页.html` | `Docs/design/home-v2/个人中心.png` | `pages/user-center/index` | PERSONAL CONTROL identity when present in canonical reference, username/tagline hierarchy, two stat cards, grouped settings sections, about/version section, calm bottom nav |
| Login / 登录 | `Docs/design/home-v2/登录.html` | `Docs/design/home-v2/登录.png` | `pages/login/index` | paper bg, 时光回序 logo, 久违了/时间的旅人 headline, login/register tab row, minimal underline input fields, 进入档案馆 CTA with corner marks + vermilion dot, 忘记密码 text |
| New Record / 新建 | `Docs/design/home-v2/新建.html` | `Docs/design/home-v2/新建.png` | `pages/record-editor/index` | Vol.N topbar + close, letter-body card with topline, Captured at + date + archive-tag, side-rule, ruled lines, editor textarea, attach-bar MAP/IMAGE/VOICE, word count + 封存这一刻 button + hint |
| Archive Detail / 封存回看 | `Docs/design/home-v2/解锁.html` | `Docs/design/home-v2/解锁.png` | `pages/record-detail/index` (SEALED state) | 时光回序 logo, close btn, Archive No. + season, deco line, letter-card with seal/过去的你/location/blurred body/sparkle, countdown digits, 留下回应 CTA, sub-hint |
