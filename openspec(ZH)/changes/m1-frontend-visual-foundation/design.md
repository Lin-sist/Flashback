# M1 Frontend Visual Foundation Design / M1 前端视觉基础设计

## Design Intent / 设计意图

M1 SHOULD 让 mini program 像一间安静的房间：用户可以在其中书写，并在未来回到一个被保存的时刻。界面 SHOULD 优先保证平静的可读性、克制的情绪表达与清晰的时间感。

visual foundation SHOULD 服务于 V2.0 loop：

```text
写下此刻 -> 保存此刻 / 交给时间 -> 我的记录 / 时光轴 -> 记录抵达 -> 时间回看 -> 可选回信
```

## Current Baseline / 当前基线

已知 frontend facts：

- Stack：Uniapp + Vue 3 + Pinia。
- Target：WeChat Mini Program。
- Pages 声明在 `frontend/src/pages.json`。
- Shared UI 位于 `frontend/src/components/common`。
- 现有 tokens 位于 `frontend/src/styles/tokens.css`。
- Preview/demo data 已存在于 `frontend/src/features/preview`。
- Record statuses：`DRAFT`、`SEALED`、`UNLOCKED`。
- Record types：`FUTURE_LETTER`、`NODE_RECORD`、`EMOTION_NOTE`。

## Confirmed Visual Direction / 已确认视觉方向

M1 使用 `Docs/design/home-v2/时光回序_设计规范.md` 中的 V2 paper/vermilion direction。

M1 视觉实现 MUST 以 `Docs/design/home-v2/*.html` 定稿页面和 `Docs/design/home-v2/时光回序_设计规范.md` 为准。Agent MUST NOT 自行重新设计视觉风格、布局语言或组件气质。

旧的 V1 blue-gray implementation 仍可作为代码库上下文，但不是 M1 的目标视觉风格。

实现 MUST 先完成局部页面视觉还原并稳定，再抽取 tokens 与 shared components。禁止一开始大规模抽象，禁止先搭组件库再强行套页面。

## Architecture Approach / 架构方法

### 1. Local Visual Restoration / 局部视觉还原

先选择 M1 覆盖页面中的局部页面或局部区域，按定稿 HTML 与设计规范还原视觉。每个局部 SHOULD 在布局、视觉气质、状态表达和命名稳定后，再进入抽象沉淀。

### 2. Token Layer / Token 层

创建或更新 shared visual tokens，用于：

- Paper-like background、surface、ink text、muted ink、divider、vermilion accent 与 status colors。
- Typography scale 与 serif-led font-family strategy。
- Spacing scale。
- Radius scale。
- Shadow/elevation scale。
- draft、sealed、unlocked/arrived 与 replied 的 status semantics。

Tokens SHOULD 从已经稳定的局部页面中沉淀出来，并被 shared components 与 page styles 引用，而不是散落为一次性值。

### 3. Shared Shell And Navigation / 共享壳层与导航

在局部视觉稳定后，再抽取或对齐这些 shared surfaces：

- App page shell。
- Top safe area handling。
- Top bar。
- Bottom navigation。
- Primary button / secondary action。
- Card/paper container。
- Filter segment。
- Empty state。
- Timeline node。
- Record card。

Primary tabs 保持为：

- 首页
- 时光轴
- 个人中心

V2.0 用户可见命名 SHOULD 使用：

- 我的记录
- 时光轴
- 时间回看

Secondary pages 保持在 first-level tab navigation 之外：

- 我的记录
- 新建记录
- 时间回看
- Settings-style user-center pages

### 4. Page-Level Alignment / 页面级对齐

页面视觉工作 SHOULD 按核心闭环顺序推进：

1. Home：让“写下此刻”成为最强动作，并保持 homepage 作为私人时间档案入口。
2. Record editor：支持选择记录类型、写给谁、写下内容、保存此刻与交给时间，但 M1 不实现 subscription-message behavior。
3. Record list：让记录状态可扫描识别，但不变成 admin table。
4. Timeline：保持单列纵向时间结构，不变成 chart dashboard。
5. Record detail/review：让“那时的我 / 现在的我”清晰可读，并保持 reply optional。
6. User center：保持 settings/account/project boundary，不变成 content feed。

### 5. Demo States / 演示状态

M1 SHOULD 使用现有 `frontend/src/features/preview` 路径作为初始 demo data source。V2.0 演示允许 one-click preview / no-login demo mode。Demo states MUST 明确体现 development/demo-safe，且 MUST NOT 暗示 production authentication、notification、deployment 或 monitoring readiness。

## Constraints / 约束

- MUST NOT 修改 backend、database 或 production infrastructure。
- MUST NOT 修改 status transition rules。
- MUST NOT 新增 first-level tabs。
- MUST NOT 将 admin features 纳入 M1。
- MUST NOT 将 desktop web rendering 作为验收目标。
- MUST NOT 脱离 `Docs/design/home-v2/*.html` 定稿页面和 `Docs/design/home-v2/时光回序_设计规范.md` 自行发挥视觉。
- MUST NOT 在局部页面稳定前进行大规模组件抽象。
- MUST NOT 在 M1 中实现 WeChat subscription messages。
- MUST NOT 在 M1 中修改 AI capability。
- MUST NOT 在 M1 中实现真实 MAP / IMAGE / VOICE features。
- 实现 MUST 兼容 WeChat Mini Program limitations。

## Verification Strategy / 验证策略

后续实现 SHOULD 验证：

- 可行时执行 `frontend` type check。
- 可行时在 `frontend` 下执行 WeChat Mini Program build command：`npm run build:mp-weixin`。
- 对 mini program target 中的核心页面进行 visual smoke check，或记录等价验证方式。
- Review diff，确认没有 backend/database/admin/production files 被修改。

任何跳过的验证 MUST 附带原因说明。
