# Proposal：Frontend Visual Language Evolution

## Why

当前前端已经完成 M1 `m1-frontend-visual-foundation` 的大部分“宣纸朱砂”高保真转译，但该 change 明确把 `Docs/design/home-v2/` 定稿 HTML 设为唯一视觉答案，也明确禁止 Agent 另造视觉风格。当前用户希望正式探索更多视觉风格，并从 Inspora、Awwwards 吸收更有辨识度的动效；这已超出 M1 的固定参考与精修范围，不能通过回改 M1 或继续局部换肤完成。

本 change 将先比较多套视觉语言，再锁定一个适合 Flashback 的方向。它服务“给想留下的此刻，一个位置”，不把产品改造成设计画廊、内容流、效率仪表盘或以 AI 为中心的聊天界面。

## What Changes

- 在 `Docs/design/frontend-visual-language-evolution/` 建立多方向视觉探索、动效参考、评审矩阵与 Open Design brief。
- 使用同一组固定合成内容，为首页、记录编辑器、时光轴制作四套可比较方向：纸上留白、私人馆藏、生命剪贴、暮色回声。
- 将 Awwwards 的编辑索引、时间滚动、文字揭示、卡片堆叠、图像转场等机制转译为微信小程序可降级的动效，不直接移植 WebGL、3D、鼠标 hover 或重型视差。
- 在任何业务实现前，由产品负责人选择一个主方向，并记录保留元素、拒绝元素、动效等级与页面推进顺序。
- 方向锁定后，以设计 token、页面壳层和三个样板页面为第一实现切片；其它页面分批迁移，避免 major frontend visual reconstruction。
- 保留现有路由、真实业务状态、Preview 隔离、三个一级 Tab、V2.0 用户可见命名与 Agent/数据主权边界。

## Scope

### In scope

- `Docs/design/frontend-visual-language-evolution/**` 的规划、合成原型与视觉评审证据。
- 首页、记录编辑器、时光轴作为共同样板切片。
- 色彩、字体、间距、材质、图文构图、状态表达、页面壳层、底部导航和轻量动效语言。
- 现有 `tokens.css`、共用 presentation components 和样板页面的后续渐进式收口。
- 微信小程序安全区、低端设备、减少动态效果与静态降级方案。
- 仅使用固定合成文字、图片占位和结构化状态进行外部设计原型。

### Out of scope

- 本 Gate 1 直接修改任何前端业务代码。
- 同时上线多套主题或新增生产级主题切换器。
- 改变 API、DTO、持久化、状态机、权限、封存、附件、位置、时间篇章或 Agent 行为。
- 新增第四个一级 Tab，或修改“我的记录”“时光轴”“时间回看”。
- WebGL、3D 场景、自动播放视频、鼠标专属交互、强滚动劫持或依赖动画才能理解的信息。
- package/lockfile、deployment、monitoring、admin portal、SMS、生产通知中心或发布工作。
- 将真实用户日记、prompt、回复、账号、媒体、位置、token、password 或 secret 提供给 Open Design 或任何外部设计服务。

## Relationship To M1

- `m1-frontend-visual-foundation` 保留为既有高保真转译与未完成视觉验收的历史 change，本 change 不修改其 proposal/design/tasks/spec。
- M1 的现有实现是当前代码基线，不是本 change 的永久视觉答案。
- 本 change 是后继探索：先比较，再锁定，再渐进迁移；不得把 M1 未完成的验收项伪写成本 change 已完成。
- 两个 change 不并行实施。当前唯一 active change 为本 change；M1 不因目录仍存在而自动恢复 active。

## Success Criteria

- 四个方向使用同一信息架构、相同合成内容和相同屏幕集合，可进行公平对比。
- 每个方向同时给出首页、记录编辑器、时光轴的静态关键帧，以及至少两项核心动效的静态/低动态降级说明。
- 评审明确选择一个主方向，而不是把四套方向一起带入生产。
- 选定方向仍以“留下此刻”为首页视觉中心，AI、封存和时间回看退居用户主动选择的次级位置。
- 动效不承担唯一语义，不默认播放声音，支持减少动态效果，并能在微信小程序限制下无损降级。
- 后续实现按样板页→token/共享结构→分批页面迁移推进，不进行一次性全站大重构。
- Preview 不产生真实 mutation 或 mock success；真实路径不被视觉层绕过。

## Authorization And Gates

- 当前授权：Gate 1 规划、公开设计案例调研、在 `Docs/design` 建立规划、打开 Open Design 官方下载页。
- 当前未授权：Open Design Cloud 正式生成、前端实现、依赖变更、真实用户素材外发、微信开发者工具/真机验收、commit、push、PR、deploy、release、delta acceptance 与 archive。
- Gate 1 获用户批准且主视觉方向锁定后，仍需单独 Gate 2 实现授权。

