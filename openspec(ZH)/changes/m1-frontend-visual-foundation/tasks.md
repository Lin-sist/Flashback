# M1 Frontend Visual Foundation Tasks / M1 前端视觉基础任务

## 1. Scope Confirmation / 范围确认

- [x] 确认已选择的 V2.0 visual direction：宣纸朱砂风格。
- [x] 确认 M1 允许在支撑 V2.0 命名与语气时调整 copy/microcopy。
- [x] 确认 canonical initial demo data path：现有 `frontend/src/features/preview`。
- [x] 确认 V2.0 演示允许 one-click preview / no-login demo mode。
- [x] 确认 V2.0 visible names：我的记录、时光轴、时间回看。

## 2. Visual Token Foundation / 视觉 Token 基础

- [ ] 审计现有 `frontend/src/styles/tokens.css` 与 page-level one-off visual values。
- [ ] 对照 `Docs/design/home-v2/*.html` 定稿页面和 `Docs/design/home-v2/时光回序_设计规范.md`，确认 M1 视觉还原依据。
- [ ] 先完成局部页面 / 局部区域的视觉还原与稳定，禁止一开始大规模抽象。
- [ ] 定义 M1 paper、ink、vermilion、typography、spacing、radius、shadow 与 status token set。
- [ ] 从已稳定的局部页面中沉淀 tokens，再应用到 shared components。

## 3. Shared Components And Shell / 共享组件与壳层

- [ ] 对齐 app page shell、safe area、top bar 与 bottom navigation。
- [ ] 对齐 primary 与 secondary action styles。
- [ ] 对齐 card/paper container、record card、timeline node、filter segment、search 与 empty state visuals。
- [ ] 保持 primary tabs 固定为 首页、时光轴、个人中心。
- [ ] 仅抽取已经在局部页面中稳定复用的结构，避免先搭组件库再套页面。

## 4. Page-Level Visual Pass / 页面级视觉处理

- [ ] Home：强调“写下此刻”与私人时间档案入口。
- [ ] Record editor：明确 record type、writing target、content、保存此刻 与 交给时间 的呈现。
- [ ] Record list：让 draft、sealed 与 unlocked/arrived records 易于扫描。
- [ ] Timeline：保留单列纵向时间结构与状态清晰度。
- [ ] Record detail/review：支撑“那时的我 / 现在的我”与 optional reply。
- [ ] User center 与 secondary settings pages：保持安静的 settings/project-boundary presentation。

## 5. Verification And Review / 验证与审查

- [ ] 可行时运行 frontend type check。
- [ ] 可行时运行 WeChat Mini Program build。
- [ ] 对核心页面执行 mini program visual smoke check，或记录无法执行的原因。
- [ ] 核对 M1 页面视觉是否可追溯到 `Docs/design/home-v2/*.html` 与 `Docs/design/home-v2/时光回序_设计规范.md`。
- [ ] 核对组件 / tokens 是否来自局部稳定后的沉淀，而不是提前大规模抽象。
- [ ] 确认没有 backend、database、admin、deployment、monitoring 或 business-rule files 被修改。
- [ ] 确认没有引入 subscription-message implementation、AI capability change 或真实 MAP / IMAGE / VOICE feature。
- [ ] 为下一个 OpenSpec change 记录剩余视觉或产品问题。
