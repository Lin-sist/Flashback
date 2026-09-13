# Flashback Demo 原型实现指南

> **版本**：v1.0（2026-09-13）
> **定位**：指导 `demo/` 静态 HTML/CSS 原型的实现，衔接「定稿图片 → 代码原型 → 未来正式前端」。
> **边界**：只动 `demo/`，不触碰 miniprogram 业务代码、backend、package/lockfile 与 accepted baseline；业务代码实现仍走 active change `frontend-visual-language-evolution` 的既有 Gate 2 授权。
> **事实源优先级**：产品宪章 > 蓝图 §9.4 > `UI_DESIGN_CONTEXT.md` 设计系统 > `OPENDESIGN_BRIEF.md` v3.0 > 本指南 > 定稿图片。
> **图片的角色**：气质与结构参照。图片与规范冲突时，以本指南 tokens 与 brief 为准（已知偏差见 §6）。

---

## 0. 图片定稿基线（每定稿一张更新此表）

| 素材 | 路径（`2.0页面效果/概念图/`） | 状态 | 实现用途 |
|---|---|---|---|
| 首页 V3 | `首页/V3.png` | ✅ **定稿·材质锚点**（demo 已实现 `pages/home.html`） | 全局 tokens 来源＋首页蓝本 |
| 书写页 V2 | `书写页/V2.png` | ✅ **定稿**（demo 已实现 `pages/writing.html`） | 书写页蓝本＋玻璃药丸参照 |
| 材质① 玻璃药丸＋取景框 | `玻璃药丸-取景框.png` | ⏸ 暂不定稿 | 特写参照；玻璃实现以书写页 V2 为准 |
| 回看页 V1 | `回看页/V1.png` | ✅ **定稿·小修已并入**（demo 已实现 `pages/look.html`） | 回看页蓝本 |
| 封存页 V1 | `封存页/V1.png` | ✅ **定稿·小修已并入**（demo 已实现 `pages/sealed.html`） | 封存页＋霜材质蓝本 |
| 时间轴 V1 | `时间轴/V1.png` | ✅ **并入代码·节点语义在代码层纠正**（demo 已实现 `pages/timeline.html`） | 时光轴蓝本 |
| 我的记录 V1 | `我的记录/V1.png` | ✅ **定稿**（demo 已实现 `pages/records.html`） | 我的记录蓝本 |
| 篇章页 V1 | `篇章页/V1.png` | ✅ **定稿**（demo 已实现 `pages/chapter.html`） | 时间篇章蓝本 |
| 个人中心 V1 | `个人中心/V1.png` | ✅ **定稿·小修已并入**（demo 已实现 `pages/user-center.html`） | 个人中心蓝本 |
| 数据与所有权 | `个人中心/数据与所有权.png` | ✅ **定稿**（demo 已实现 `pages/ownership.html`） | 托管凭证蓝本 |
| 整理·近况 | `个人中心/整理.png` | ✅ **定稿**（demo 已实现 `pages/summary.html`） | 整理成一封近况·结果态蓝本 |
| Agent 选择浮层 | `书写页/Agent选择浮层.png` | ✅ **定稿**（demo 已实现 `pages/agent-choose.html`） | 对话入口蓝本 |
| Agent 对话浮层 | `书写页/Agent对话浮层.png` | ✅ **定稿**（demo 已实现 `pages/agent-writing.html`） | 书写对话蓝本 |
| 回看 Agent 对话 | `回看页/Agent 对话浮层.png` | ✅ **定稿**（demo 已实现 `pages/agent-review.html`） | 回看对话＋来源引用蓝本 |
| 登录页 | `登录页/` | ⏸ **用户明确本轮暂缓**（原型未定稿，不实现） | 登录蓝本（暂缓） |
| 材质② 霜卡两态 | `霜封卡融霜前后两态.png` | 待重roll | 不阻塞；动效按 brief §5 文字规范先行 |
| 材质③ 落印瞬间 | `落印瞬间.png` | 待补 UI 态 | 现有照片版仅作品牌氛围参照 |

⚠️ 该目录已 gitignore（含疑似真实内容截图），仅限本地参考；生成物一律使用 brief §11 合成内容，不复述截图真实文字。

> **2026-09-13 定稿裁决**（更新）：首轮仅首页 V3／书写页 V2 定稿开写；本批 时间轴／我的记录／篇章／个人中心／所有权／整理／三张 Agent 对话浮层 已定稿并入代码，**登录页用户明确暂缓**；材质②③仍待重roll，不阻塞。

## 1. 画布与文件结构

```text
demo/
  index.html               ← 原型导航/画廊（2026-09-13 已改造：旧联屏模拟器退役，为十四页信笺导航）
  brand-spec.md            ← 既有品牌规范（保留）
  IMPLEMENTATION_GUIDE.md  ← 本指南
  assets/
    tokens.css             ← 全局设计变量与排版类
    components.css         ← 共享组件
    motion.css             ← 四枚签名动效 + 降级
  pages/
    home.html  writing.html  saved.html  look.html  sealed.html  user.html  timeline.html
```

- 基准画布 **390×844**，左右安全边距 `28px`（内容区 334px）；展示用 `.device` 外框居中，不影响页面本体尺寸。
- 每页独立 HTML，只引 `tokens.css` + `components.css`（+需要动效的页引 `motion.css`），页内只写结构与页级微调。
- 实现顺序即依赖顺序：**tokens → 首页 → 书写页 → 已保存态 → 回看/封存 → 个人中心 → 时间轴**。
- 进度（2026-09-13·第二批完成）：`tokens.css`＋`components.css`（含本批追加的时刻卡/排印块/对话浮层套件/遮罩）＋`motion.css` 已落地；14 页全部实现并通过浏览器截图验收——home / writing / saved / look / sealed / timeline / records / chapter / user-center / ownership / summary / agent-choose / agent-writing / agent-review。页面互链接通：三 Tab 页互转，user-center→ownership/summary/records，records↔chapter、records→look/sealed、chapter→look，writing→agent-choose→agent-writing，look→agent-review（来源药丸→records）。**登录页按用户指示暂缓**。`index.html` 画廊已于 2026-09-13 改造为十四页信笺导航（挂在共享 tokens/components 上，自动继承宣纸三层系统；旧 V3.0 联屏模拟器整体退役，功能由 pages/*.html＋motion.css＋本指南 §7 承接）。本地预览：在 `demo/` 下起任意静态服务（如 `python -m http.server 8931`）后访问 `/pages/*.html`。

## 2. 全局 Tokens（assets/tokens.css）

```css
:root {
  /* 纸 —— 世界的材质：宣纸三层系统＝光晕（.device 背景层）＋纤维颗粒（::before）＋暖纸渐变 */
  --paper-grad: linear-gradient(170deg, #FAF7F2 0%, #F5F0E8 55%, #F0EBE0 100%);
  --paper-card: rgba(252, 249, 244, 0.92);   /* 信卡体：高不透明暖纸白，不用 backdrop-filter */
  --paper-deep: #EDE8DF;                      /* 凹陷面（书写空位、取景框）深半档 */
  --paper-grain: url("data:image/svg+xml;charset=utf-8,%3Csvg xmlns='http://www.w3.org/2000/svg' width='240' height='240'%3E%3Cfilter id='g'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='2' stitchTiles='stitch'/%3E%3CfeColorMatrix type='saturate' values='0'/%3E%3CfeComponentTransfer%3E%3CfeFuncA type='linear' slope='0.07'/%3E%3C/feComponentTransfer%3E%3C/filter%3E%3Crect width='240' height='240' filter='url(%23g)'/%3E%3C/svg%3E");          /* 纤维颗粒：极轻，勿调高 */
  --paper-grain-strong: url("data:image/svg+xml;charset=utf-8,%3Csvg xmlns='http://www.w3.org/2000/svg' width='240' height='240'%3E%3Cfilter id='gs'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='2' stitchTiles='stitch'/%3E%3CfeColorMatrix type='saturate' values='0'/%3E%3CfeComponentTransfer%3E%3CfeFuncA type='linear' slope='0.16'/%3E%3C/feComponentTransfer%3E%3C/filter%3E%3Crect width='240' height='240' filter='url(%23gs)'/%3E%3C/svg%3E");   /* 霜面凝霜颗粒 */
  /* 墨阶 */
  --ink: #302E29; --ink-mid: #6B6560; --ink-light: #9E9890; --ink-faint: #C8C2B8;
  /* 朱砂 —— 唯一有彩色，单屏 ≤3 处 */
  --vermilion: #B5352A; --verm-dim: #8A271F;
  /* 线与霜 */
  --hairline: rgba(188, 174, 152, 0.28);
  --frost: rgba(255, 255, 255, 0.65);
  /* 影（贴地接触影＋环境柔影双层，总量仍在 10–14% 档；禁 4–6% 脏平中间态） */
  --shadow-card: 0 1px 2px rgba(125, 105, 75, 0.1), 0 10px 26px rgba(125, 105, 75, 0.13);
  --inset-light: inset 0 1px 0 rgba(255, 255, 255, 0.6);
  /* 玻璃 —— 只属于动作与暂时之物 */
  --glass-bg: rgba(255, 255, 255, 0.42);
  --glass-border: rgba(255, 255, 255, 0.65);
  --glass-blur: blur(14px) saturate(1.15);
  /* 字 */
  --font-serif: "Noto Serif SC", "Songti SC", "STSong", Georgia, serif;
  --font-sans: "PingFang SC", "Noto Sans SC", sans-serif;   /* 仅 ≤11px 元数据 */
  /* 节奏（8pt 网格，递进留白） */
  --page-x: 28px; --gap-xs: 6px; --gap-s: 12px; --gap-m: 28px; --gap-l: 48px;
  --radius-card: 1px; --radius-sheet: 12px; --radius-pill: 999px;
}
```

排版层级（宋体做主角，黑体退居 ≤11px 元数据）：

| 类 | 规格 |
|---|---|
| `.t-brand` | 12px / 300 / 字距 0.55em / `--ink-light`（「时 光 回 序」） |
| `.t-h1` | 32px / 300 / 字距 0.06em（第一行）＋ 28px（第二行），`--ink` |
| `.t-sub` | 14px / 300 / 字距 0.04em / `--ink-mid`（副句） |
| `.t-section` | 17px / 400 / 行高 1.5（卡片标题、章节题） |
| `.t-body` | 15px / 300 / 行高 1.85 / 字距 0.03em / `--ink-mid` |
| `.t-meta` | 11px / `--font-sans` / 字距 0.08em / `--ink-light` |

⚠️ 定稿图为海报级字重字号，实现一律回到本表数值（见 §6 偏差清单）。

## 3. 共享组件（assets/components.css）

按依赖序实现，一次写好全局复用：

0. **纸面材质（2026-09-13 补齐）**：`.device` 背景＝天光/晕影 radial 光晕（3–4 层，≤34% 透明度）叠 `--paper-grad`；`.device::before`＝全局纤维颗粒层（`--paper-grain`，z-index 0，内容之下）；`.letter-card` / `.chat-note` / `.chat-note-user.papered` / `.chat-sheet` / `.viewfinder` 自带纸纹——`isolation: isolate` ＋ 负层级 `::before`（z-index:-1，纹理在卡体背景之上、文字之下）。feTurbulence 用 `stitchTiles='stitch'` 保证 240px 无缝平铺。
1. **信笺卡 `.letter-card`**：`--radius-card` 直角；`--paper-card` 底＋`--hairline` 0.5px 边＋`--inset-light`＋`--shadow-card`；左侧 1.5px 朱砂渐变竖线（`linear-gradient(to bottom, transparent, rgba(181,53,42,.5) 15% 85%, transparent)`），占位在 padding 结构里预留。**禁 backdrop-filter**（纸禁玻璃，浮起感全靠影与内描光）。
2. **折角 `.fold`**：右上角 18px（2026-09-13 由 14px 上调，对齐首页 V3 比例），`::after` 对角渐变或 clip-path，折痕面比卡面深半档＋1px 内投影。**只允许出现在"信"形态**（信卡、书信页、已保存卡、回信小卡）；面板、表单、入口卡禁用。
3. **圆印 `.seal`**：26px 圆、1px `rgba(181,53,42,.75)` 描边、透明底、宋体 11px 单字、透明度 ~0.85。字符语义锁定：**封**＝封存与保存（落印），**阅**＝阅读态，**待**＝等待抵达，**守**＝托管凭证。书写页不出现印章。
4. **取景框空位 `.viewfinder`**：`--paper-deep` 凹陷面、近直角、四角极细墨色括线（两个伪元素＋4 段渐变线，或角标 SVG）；压凹用 letterpress 双边影（内上缘暗 `inset 0 2px 4px rgba(120,100,70,.16)`、内下缘亮 `inset 0 -1px 0 rgba(255,255,255,.65)`、外唇一线受光 `0 1px 0 rgba(255,255,255,.55)`）；placeholder 用 **`--ink-mid`（正文色，非灰）**，面板内垂直居中。
5. **玻璃药丸 `.glass-pill`**：高 48px、`--radius-pill`；`--glass-bg`＋`--glass-blur`＋1px `--glass-border` 高光描边＋顶部微光泽渐变；左端 5px 朱砂起笔点；按压态 `translateY(1px)`＋底色透明度上调。**硬性降级**：`@supports not (backdrop-filter: blur(1px))` 时改 `background:#F3EEE4` 暖纸实色（brief §3）。
6. **TabBar `.tabbar`**：底部悬浮轻玻璃片（左右 inset 12px、顶圆角 ≤12px、`--glass-blur`、顶缘 0.5px 发丝线）；三项纯宋体 13px 字距 0.2em，激活项 `--ink`＋正下方 3px 朱砂静点，非激活 `--ink-light`；**无任何图形图标**。降级同药丸。
7. **霜遮罩 `.frost`**：绝对定位覆盖卡体，白雾渐变（上浅下重）＋凝霜颗粒（`--paper-grain-strong` 叠在渐变上层，雾状非雪花晶体，见 §6.7）＋边缘凝霜加重（inset 白色 box-shadow）＋`backdrop-filter: blur(7px)`；语义性玻璃，允许使用。两态类：`.is-sealed` / `.is-thawing`。
8. **竖排侧注 `.side-note`**：`writing-mode: vertical-rl`、11px 宋体、字距 0.35em、`--vermilion`、0.5px 细框；文案用低语式（「只写给自己」），禁「严禁翻阅」式硬话。
9. **时刻卡 `.moment-card`**：上半无裁切自然色彩照片（**禁压灰滤镜**，照片是纸上唯一色彩生命），下半窄纸色信息条（宋体小标题）；不带朱砂竖线（红线是"信"的家具）。
10. **排印块 `.text-block`**（时光轴纯文字条目）：无卡无底，日期 meta＋宋体正文直接排在纸上。
11. **轻玻璃小药丸 `.chip`**：筛选、状态标签用，半透明＋微模糊＋0.5px 边。

## 4. 动效：四枚签名，其余安静（assets/motion.css）

| 动效 | 实现 | 降级 |
|---|---|---|
| **落印**（保存） | 信卡出现后，`.seal` 从 scale(1.6)/opacity 0 → 1，带 -4°→0 微转与一次 box-shadow 朱砂晕开脉冲，300ms `cubic-bezier(.2,.8,.2,1)`；无声音、不自动跳转 | `prefers-reduced-motion`：印章直接显示 |
| **融霜**（抵达） | `.frost` 用 mask/clip-path 从全覆盖 → 向右上角收退，~900ms ease，一次性非循环 | 直接显示解锁态 |
| **脊线生长＋节点点亮**（时光轴） | 进入视口后脊柱 `scaleY 0→1`（transform-origin: top）~1200ms；IntersectionObserver 让节点过中线加 `.lit`；节点两态：空心＝纯文字、实心＝含媒体 | 全部静态显示 |
| **卡片轻浮进入** | 卡片 opacity 0→1＋translateY(12px→0)，500ms，阈值 0.2，仅一次 | 直接显示 |

**禁项**（brief §5.4）：视差堆叠、滚动劫持、循环动画、秒级跳动。全部动效必须有 reduced-motion 静态降级。

## 5. 逐页实现要点（按实现顺序）

1. **首页**（§6.1＋首页 V3）：字标→双行问候→副句→取景框空位→待达信卡（红线＋折角＋「待」印＋"即将抵达 · 2027 年夏"）→最近抵达卡（**无红线**）→TabBar。卡组紧跟取景框 24–28px，余白留给底部；placeholder 垂直居中；首页禁编号/统计/功能名头部。
2. **书写页**（§6.2＋书写页 V2）：×关闭→汉字抬头「记录于 二零二六年 · 暮秋」→折角＋竖排侧注（折角下方沿右缘）→左贯穿朱砂细线渐变淡出→无边框正文（title＋body 直接排印）→照片缩略图（自然色）＋细线语音占位→「附言 · 可选」一行安静折叠（`<details>` 即可）→底部三细线图标＋玻璃药丸「· 留下这一刻」。本页无印章、无字数统计、无输入框边框。
3. **已保存承接态**（§6.3，流程闭环）：正文信卡化＋「这一刻已经留下」＋落印动效在卡右下触发；「继续补充」文字链接；Agent 参考开关排印安静化收次级；底部红字「删除这条记录」。
4. **回看页**（§6.5＋回看 V1）：存档编号＋季节、引号信题、阅印＋「过去的你」、正文信卡＋星芒、当时片段（照片网格＋细线语音）、**回信小卡（比主卡窄、负 margin 微叠主卡下缘）**、下划线链接「和它聊聊那时候」、玻璃药丸「· 留下回应」。信题引号统一弯引号 " "。
5. **封存页**（§6.6＋封存 V1）：霜卡（标题微透、正文不可读、边缘凝霜重）＋封印；片段区在霜外；状态行＋**静态期待语大字「即将抵达 · 2027 年夏」**；底部安抚句。禁任何倒计时数字、进度条。语音占位裸波形＋细线圆圈播放键（去灰底）。
6. **个人中心**（§6.8＋个人中心 V1）：头像（抽象墨韵，无真人脸）＋昵称＋保管事实句「这里替你保管着 14 封信，还有 0 封在路上。」；三张描边扁平稳入卡（保留四角括线——取景框＝入口语法；压扁至 ~52px 高）；托管凭证卡为全页唯一视觉重心（守印＋导出/清理细线小按钮）；「关于」收尾。**红色配额已满（守印/Tab 点/头像红日），不得再引入**。
7. **时间轴**（§6.4，结构与节奏可先搭）：筛选 chip＋双行「时 间 长 廊」＋副句；轻脊柱＋**代码层实现节点语义（空心＝纯文字、实心＝含媒体）**；条目按形态分化：排印块 / 时刻卡 / 霜卡（后续可加波形卡、章节板）；霜感对齐封存页（比时间轴 V1 图更重）。

## 6. 图片偏差清单（不要照抄进代码）

| # | 图片行为 | 实现裁决 |
|---|---|---|
| 1 | 首页 V3 标题字重偏重、字号海报级 | 一律回 tokens：300 字重、32/28px |
| 2 | TabBar 圆角偏大 | 顶部圆角 ≤12px |
| 3 | 回看页信题用「」、封存页用 " " | 统一弯引号 " " |
| 4 | 时间轴 V1 封存卡用空心节点 | 代码按内容类型定：含媒体＝实心 |
| 5 | 时间轴霜感偏轻 | 对齐封存页 V1 的霜 |
| 6 | 材质① 清水玻璃（实体折射） | CSS 做磨砂半透明＋高光描边，不做实体折射 |
| 7 | 材质② 雪花晶体霜、厚瓷砖卡 | 雾状凝霜；薄信笺卡 1px 直角 |
| 8 | 材质③ 手＋实体印章照片 | 仅作氛围参照；UI 态是无手无道具的印章落卡 |
| 9 | 材质图中的花枝、光斑道具 | 排除在页面之外 |
| 10 | 语音播放键曾出现实心灰圆 | 统一 1px 细线圆圈＋细线三角 |

## 7. 每页验收清单（完成自检，随页面提交）

- [ ] 朱砂 ≤3 处，且只服务动作/时间/信任；书写页无印章
- [ ] 无大圆角（信卡 1px）、无图形图标、无英文装饰标签、无 emoji
- [ ] 时间语法＝「抵达」：无倒计时秒数、无"落后/欠账/连续天数"
- [ ] 玻璃剂量：只允许药丸、TabBar、筛选/浮层；内容卡全纸
- [ ] 折角只在信形态；印章字符与语义正确（封/阅/待/守）
- [ ] 照片自然色彩无压灰滤镜
- [ ] `prefers-reduced-motion` 降级可用；`backdrop-filter` 不支持时降级暖纸实色
- [ ] 与首页 V3 锚点并排对比：纸色、墨阶、留白气质同源
- [ ] 390 基准、左右 28px、8pt 网格、无中部死区（内容紧跟，余白留底）

## 8. 会话工作流

1. 新实现会话开场：读本指南＋`UI_DESIGN_CONTEXT.md`＋`OPENDESIGN_BRIEF.md` v3.0；一次只做一页/一层。
2. 每完成一层或一页：按 §7 自检、更新 §0 基线表与本文档对应小节、写入 AGENT_LOG（Type B 记录）。
3. 提交责任：active change 已授权 Agent 本地 commit（**不含 push**），建议信息风格 `demo(home): 首页原型实现`。
4. 并行线：图片优化（时间轴、材质②③）由用户在独立会话推进，定稿后更新 §0 表——实现不等图。
5. 全部页面完成后：五帧＋三材质稿对照 brief §10 逐项核销，再议是否进入正式前端实现。
