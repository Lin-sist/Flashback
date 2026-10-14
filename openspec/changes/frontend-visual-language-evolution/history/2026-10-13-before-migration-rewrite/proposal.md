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

## 2026-10-13 增补：五页 demo 精修与本地文字演示（已授权实施，待验收）

本节是既有 change 的规划增补，不覆盖历史授权或将旧任务标为完成。当前主方向依最新用户裁决为「信笺纸 × 微液态玻璃」，不重新启动四方向生成。

### Why Now / 改前与改后

当前十四页 demo 已有统一纸面与玻璃组件，但首页书写入口为静态 div、书写内容为静态 h2/p、保存按钮无行为。用户无法实际比较输入、保存反馈与回看的连续体验。用户本轮请求先完整规划，再精修 demo，并明确选择五核心页先行、其余九页回归、接通本地输入/保存/回看、使用 sessionStorage。

批准与实现后，用户可在同一标签页输入测试文字、保存至隔离的演示存储、继续补充并在时光轴/回看读取同一条演示记录；失败明确保留输入，不能显示假成功。暖纸、宋体、朱砂、近直角信卡、三项纯文字导航与现有四枚动效继续保留。

### Scope / Non-goals

- 详细可执行规划：`demo/REFINEMENT_PLAN.md` v1.0。五页为 home、writing、saved、timeline、look；index 仅补说明/清除入口。
- 共享 tokens/components/motion 微调与必要的原生 demo JS；其余九页默认只回归，局部修复须明确新增 allowlist。
- 同源同标签页 sessionStorage、独立 demo key；只存一条最新演示记录与草稿。正文不进入 URL、日志、tracked files 或外部服务。
- 不修改正式小程序/后端/API/DTO/权限/封存/Agent/production Preview/依赖/部署/accepted baseline/archive；登录页继续暂缓。
- 本地文字演示与既有只读 Preview 是两个不同表面；不放开 Preview mutation，不调用真实业务。媒体、录音、位置、AI、真实封存/删除/导出不实现。

### 能力五态

| 能力 | 状态 | 证据 |
|---|---|---|
| 十四页静态 demo 与共享材质代码 | confirmed | 本轮目录与源码阅读 |
| 动效运行、阅读舒适度、全页响应式 | partial / unknown | CSS 存在；本轮无实际本地画面证据 |
| 五页精修与文字演示闭环 | planned | 用户确认范围；实现未开始 |
| 真实业务保存/AI/对象存储/微信迁移 | out_of_scope | 本轮独立 demo 范围 |

### Gates / 提交责任

- 本轮属于 Type C 规划增补；首轮范围、交互方向与 sessionStorage 的问答确认不等于完整规划批准。
- Gate 1 增补规划批准：2026-10-13 用户在完整规划交付后明确要求开始 demo 实施，接受该具体规划。
- Gate 2 新增 demo 实现授权：2026-10-13 用户明确「可以开始在demo实现了」；单独登记该实现指令，不从旧生产前端授权推导。
- Gate 3：真实 AI/对象存储/业务 API 调用预算 0；公开技术/设计资料只读研究已在本轮请求范围内。无素材上传、push、部署或发布授权。
- 本轮默认用户手动提交；不执行 git add/commit/push，不接受 delta、不归档。

## 2026-10-13 动效样本范围增补

- 用户接受前轮「回看信纸开启＋玻璃药丸回应」具体切片，并明确「可以先尝试落地几页看看落实效果」。Gate 1记录切片接受；Gate 2记录明确demo实施请求，二者不从旧生产授权推导。
- 本批仅look/writing加载新motion-samples CSS/JS，试验双面开启、移动阴影、主动重播与玻璃轻压/恢复/边缘光；不改变业务状态、保存接线或存储契约。
- 仅两页放开旧四枚动作上限；不用软页库、SVG折射、WebGL、全屏导航翻页、持续渲染或新依赖。
- 正式前端/backend/accepted specs/archive/其它demo页/素材不在范围。保留实际视觉验证待补，不将合成测试视为观感验收；不执行git add/commit/push。

## 2026-10-13 登录页与薄雾入场规划增补（待批准）

用户解除登录页暂缓，偏向第二构图并要求减艳色/笔触/按钮填色，提出淡粉雾扩散、留白、进入首页。本轮已生成修订图并形成`demo/LOGIN_PLAN.md`；只做规划，不新增页面或改行为。Gate 1修订图与完整增补范围待确认；Gate 2实施授权单独登记，旧R/M授权不自动覆盖登录页。

拟新增独立登录HTML/CSS/JS与原创场景/透明雾资产；index只加入口，home只接一次预览入场。保持既有会话文字演示、其它页、正式认证与依赖不变。输入只用于明确本地预览，不验证账号、不存密码、不发送请求；注册/微信仅展示说明。扩散只在进入条件成立后触发，减动态直接终态，实际跨页与流畅度unknown。

能力：修订图confirmed（图稿）；登录/转场planned；实际绘制/键盘/性能unknown；真实认证/注册/微信/SMS out_of_scope。默认用户手动提交；无push/部署/发布或真实外调授权。

