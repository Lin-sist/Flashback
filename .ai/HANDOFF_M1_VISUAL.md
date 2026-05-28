# M1 Visual Handoff

## Purpose

M1 is high-fidelity frontend visual translation for the WeChat Mini Program.

## Current Visual Language

- 宣纸浅米背景
- 宋体 / serif 气质
- 墨色文字
- 朱砂红点缀
- 轻纸张卡片
- 细线、折角、印章、朱砂痣
- 克制、安静、私密、有时间感

## Implementation Rules

- canonical HTML / PNG wins over existing frontend.
- prefer page-level structure and styles when fidelity matters.
- extract tokens only after page-level stability.
- do not preserve old skeletons when they conflict with canonical references.
- do not implement real MAP / IMAGE / VOICE even if visual hints exist.
- do not implement subscription messages in M1.

## Primary Pages

| Page | Route | Canonical HTML | Screenshot |
| --- | --- | --- | --- |
| Home | `pages/home/index` | `Docs/design/home-v2/首页.html` | `Docs/design/home-v2/首页.png` |
| Timeline | `pages/timeline/index` | `Docs/design/home-v2/时光轴.html` | `Docs/design/home-v2/时光轴.png` |
| User Center | `pages/user-center/index` | `Docs/design/home-v2/个人主页.html` | `Docs/design/home-v2/个人主页.png` |

## Secondary Pages

| Page | Route | Canonical HTML | Screenshot | Status |
| --- | --- | --- | --- | --- |
| Login | `pages/login/index` | `Docs/design/home-v2/登录.html` | `Docs/design/home-v2/登录.png` | confirmed |
| New Record / Record Editor | `pages/record-editor/index` | `Docs/design/home-v2/新建.html` | `Docs/design/home-v2/新建.png` | confirmed |
| Seal / Archive / Review page | `pages/record-detail/index` (SEALED state) | `Docs/design/home-v2/解锁.html` | `Docs/design/home-v2/解锁.png` | confirmed |
| My Records | `pages/record-list/index` | `Docs/design/home-v2/我的档案.html` | `Docs/design/home-v2/我的档案.png` | confirmed |
| Record Detail / Time Review | `pages/record-detail/index` (UNLOCKED state) | `Docs/design/home-v2/回看.html` | `Docs/design/home-v2/回看.png` | confirmed |
| Settings-style pages | `pages/user-center/archive-preference/index`, `pages/user-center/tag-manage/index`, `pages/user-center/notify-settings/index`, `pages/user-center/about/index` | NEEDS_OWNER_CONFIRMATION | NEEDS_OWNER_CONFIRMATION | pending |

## Current Known Workflow

- Claude Code / Antigravity：前端视觉实现
- Codex：后端、代码审查、文档整理、diff 守门
- Gemini：大上下文 HTML / screenshot 参数提取
- Claude Desktop：设计审查、提示词规划
- GitHub Copilot：IDE 内联补全、小范围后端或局部代码
