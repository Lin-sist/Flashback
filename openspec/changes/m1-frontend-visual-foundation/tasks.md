# M1 Frontend Visual Foundation Tasks / M1 前端视觉基础任务

## Current Phase / 当前阶段

- [x] M1 page-level frontend reconstruction is complete for the canonical Home V2 page set and user-center settings subpages.
- [x] M1 has entered frontend polish / acceptance: fix small visual, interaction, routing, safe-area, and Mini Program compatibility defects found during review.
- [ ] After frontend polish passes the acceptance checks in section 6 and section 7, M1 frontend reconstruction can be treated as complete.

## 0. Reference Lock / 定稿参考锁定

- [x] List all HTML / design spec / screenshot files in `Docs/design/home-v2/`.
- [x] Create `visual-reference-map.md`.
- [x] Confirm Home canonical mapping: `pages/home/index` -> `Docs/design/home-v2/首页.html` -> `Docs/design/home-v2/首页.png`.
- [x] Confirm Timeline canonical mapping: `pages/timeline/index` -> `Docs/design/home-v2/时光轴.html` -> `Docs/design/home-v2/时光轴.png`.
- [x] Confirm User Center canonical mapping: `pages/user-center/index` -> `Docs/design/home-v2/个人主页.html` -> `Docs/design/home-v2/个人主页.png`.
- [x] Confirm My Records canonical mapping: `pages/record-list/index` -> `Docs/design/home-v2/我的档案.html` -> `Docs/design/home-v2/我的档案.png`.
- [x] Confirm Time Review canonical mapping: `pages/record-detail/index` (UNLOCKED state) -> `Docs/design/home-v2/回看.html` -> `Docs/design/home-v2/回看.png`.
- [x] Confirm settings subpage canonical bundle: `Docs/design/home-v2/个人主页_子页面.html`.
- [x] Confirm settings subpage screenshots: `整理偏好.png`、`视觉外观.png`、`访问控制.png`、`数据备份.png`、`版本信息.png`.
- [ ] If any later duplicate or newer candidate appears, mark the page as `NEEDS_OWNER_CONFIRMATION` before implementation.

## 1. Current Implementation Audit / 现有实现差异审查

- [x] Open `visual-reference-map.md` before editing pages.
- [x] Open the exact canonical HTML for each primary page.
- [x] Compare current Home、Timeline、User Center frontend pages against canonical HTML.
- [x] Record which existing sections, hierarchy, cards, spacing, navigation, and scroll behavior MUST be replaced rather than restyled.
- [x] Confirm M1 is not treated as a color-only, token-only, or skin-only refactor.

## 2. Primary Page High-Fidelity Restoration / 一级页面高保真还原

### 2.1 Home / 首页

- [x] Restore Home against `Docs/design/home-v2/首页.html`.
- [x] Preserve centered `时光回序`, hero question `今天的你，想留下些什么？`, supporting copy, dominant writing action, arrival/stat summary, paper background, and bottom nav.
- [x] Confirm Home does not keep an old homepage structure with only colors changed.
- [ ] Capture or record visual comparison evidence against `Docs/design/home-v2/首页.png` or finalized HTML.

### 2.2 Timeline / 时光轴

- [x] Restore Timeline against `Docs/design/home-v2/时光轴.html`.
- [x] Preserve top safe area, search/filter affordance placement, `时间长廊`, compact chronological grouping, record status/title hierarchy, vertical marker rhythm, helper copy, and bottom nav.
- [x] Confirm Timeline does not keep older large letter-card / folded-paper timeline if it conflicts with canonical compact grouping.
- [ ] Capture or record visual comparison evidence against `Docs/design/home-v2/时光轴.png` or finalized HTML.

### 2.3 User Center / 个人中心

- [x] Restore User Center against `Docs/design/home-v2/个人主页.html`.
- [x] Preserve finalized identity structure, username/tagline hierarchy, two statistic cards, grouped settings sections, quiet icon containers, row dividers, about/version section, and bottom nav.
- [x] Preserve `PERSONAL CONTROL` style if a later owner-confirmed canonical HTML contains that structure.
- [x] Confirm User Center is not merely a recolored old settings page.
- [ ] Capture or record visual comparison evidence against `Docs/design/home-v2/个人主页.png` or finalized HTML.

## 3. Bottom Navigation And Safe Area / 底部导航与安全区

- [x] Ensure primary tab pages reserve enough bottom space for bottom navigation and device safe area.
- [x] Check Home at bottom state: navigation MUST NOT cover statistics, arrival summary, helper copy, or actions.
- [x] Check Timeline at bottom state: navigation MUST NOT cover the final record, final timeline item, or helper text.
- [x] Check User Center at bottom state: navigation MUST NOT cover settings rows, about/version section, or logout/action area.
- [ ] Test long content states for any floating or translucent bottom navigation.
- [ ] Treat any bottom navigation overlap as M1 failure until fixed.

## 4. Token And Shared Component Extraction / Token 与共享组件沉淀

- [ ] Extract tokens only after Home、Timeline、User Center are page-level stable.
- [ ] Derive paper、ink、vermilion、typography、spacing、radius、shadow、status, and safe-area tokens from restored pages and design spec.
- [ ] Extract shared page shell, bottom navigation, cards, buttons, filters, and empty states only from stable repeated structures.
- [ ] MUST NOT build a component library first and then force old pages into it.
- [ ] MUST NOT use token extraction to justify keeping a page structure that conflicts with canonical HTML.

## 5. Secondary Page Alignment / 二级页面视觉对齐

- [x] Confirm Login canonical mapping: `pages/login/index` -> `Docs/design/home-v2/登录.html` -> `Docs/design/home-v2/登录.png`.
- [x] Confirm New Record canonical mapping: `pages/record-editor/index` -> `Docs/design/home-v2/新建.html` -> `Docs/design/home-v2/新建.png`.
- [x] Confirm Archive Detail canonical mapping: `pages/record-detail/index` (SEALED state) -> `Docs/design/home-v2/解锁.html` -> `Docs/design/home-v2/解锁.png`.
- [x] Confirm My Records canonical mapping: `pages/record-list/index` -> `Docs/design/home-v2/我的档案.html` -> `Docs/design/home-v2/我的档案.png`.
- [x] Confirm Time Review canonical mapping: `pages/record-detail/index` (UNLOCKED state) -> `Docs/design/home-v2/回看.html` -> `Docs/design/home-v2/回看.png`.
- [x] Confirm Settings subpage bundle mapping: `Docs/design/home-v2/个人主页_子页面.html` contains PAGE 0 整理偏好, PAGE 1 视觉外观, PAGE 2 访问控制, PAGE 3 数据备份, PAGE 4 版本信息.
- [x] Restore Login page against `Docs/design/home-v2/登录.html`: paper bg, 时光回序 logo, 久违了/时间的旅人 headline, tab row, minimal underline inputs, 进入档案馆 CTA with corner marks + vermilion dot, 忘记密码 text.
- [x] Restore New Record editor against `Docs/design/home-v2/新建.html`: Vol.N topbar, letter-body card with topline, Captured at + date + archive-tag, side-rule, ruled lines, editor textarea, attach-bar MAP/IMAGE/VOICE, word count + 封存这一刻 button + hint.
- [x] Restore Archive Detail SEALED state against `Docs/design/home-v2/解锁.html`: 时光回序 logo, close btn, Archive No. + season, deco line, letter-card with seal/过去的你/location/blurred body/sparkle, countdown digits, 留下回应 CTA, sub-hint.
- [x] Restore My Records page against `Docs/design/home-v2/我的档案.html`: top back/title, search, filter tabs, 档案概览, status cards, write CTA, and footnote.
- [x] Restore Time Review / record detail UNLOCKED state against `Docs/design/home-v2/回看.html`: archive no/location, season, quote, readable letter card, 留下回应 CTA, 收入时光轴 secondary action, and reply sheet visual state.
- [x] Restore Archive Preference / 整理偏好 against `Docs/design/home-v2/个人主页_子页面.html` PAGE 0 and `Docs/design/home-v2/整理偏好.png`.
- [x] Restore Visual Appearance / 视觉外观 against `Docs/design/home-v2/个人主页_子页面.html` PAGE 1 and `Docs/design/home-v2/视觉外观.png`; add/use route `pages/user-center/visual-appearance/index`.
- [x] Restore Access Control / 访问控制 against `Docs/design/home-v2/个人主页_子页面.html` PAGE 2 and `Docs/design/home-v2/访问控制.png`; add/use route `pages/user-center/access-control/index`.
- [x] Restore Data Backup / 数据备份 against `Docs/design/home-v2/个人主页_子页面.html` PAGE 3 and `Docs/design/home-v2/数据备份.png`; add/use route `pages/user-center/data-backup/index`.
- [x] Restore Version Info / 版本信息 against `Docs/design/home-v2/个人主页_子页面.html` PAGE 4 and `Docs/design/home-v2/版本信息.png`.
- [x] Confirm each settings subpage implementation uses the HTML bundle and matching PNG snapshot as the high-fidelity source, not the old settings page skeleton or old route name.
- [x] Update User Center settings entry routing so `整理偏好`、`视觉外观`、`访问控制`、`数据备份`、`版本信息` each navigate to their confirmed canonical subpage route.
- [x] Keep legacy `tag-manage` and `notify-settings` routes unchanged unless a task explicitly maps them to a confirmed canonical subpage.
- [x] Keep secondary pages outside first-level tab navigation.
- [x] Use existing `frontend/src/features/preview` as the initial demo data path unless a later OpenSpec change replaces it.
- [x] Do not implement subscription-message behavior, AI changes, or real MAP / IMAGE / VOICE capabilities while aligning secondary pages.

## 6. Visual Review And Acceptance / 视觉审查与验收

- [ ] Provide visual comparison evidence for Home.
- [ ] Provide visual comparison evidence for Timeline.
- [ ] Provide visual comparison evidence for User Center.
- [ ] Provide visual comparison evidence for secondary canonical pages after polish fixes.
- [ ] Confirm visual evidence documents any Mini Program rendering differences.
- [ ] Run WeChat Mini Program build when feasible, or record why it was skipped.
- [ ] Confirm bottom navigation does not overlap scroll content at bottom state.
- [ ] Confirm tasks were executed in order: reference lock / page restoration before tokens.
- [ ] Confirm no backend、database、schema、admin、deployment、monitoring、business-rule、AI、subscription-message, package, lockfile, or real MAP / IMAGE / VOICE implementation files were modified.
- [ ] Confirm M1 would fail if only colors, typography tokens, radius, shadows, or old-page skinning changed.

## 7. Frontend Polish Completion Gate / 前端精修完成门槛

- [ ] Fix page-level polish defects reported during review without expanding beyond M1 frontend visual scope.
- [ ] Confirm all canonical routes still navigate correctly after polish.
- [ ] Confirm visual-only controls remain local/demo-only and do not imply real backend capability.
- [ ] Re-run `cd frontend; npm run build:mp-weixin` after polish fixes, or record why it was skipped.
- [ ] Record the final polish handoff in `.ai/AGENT_LOG.md`.
- [ ] When all checks in section 6 and section 7 pass, mark M1 frontend reconstruction complete and ready for final acceptance / archive.
