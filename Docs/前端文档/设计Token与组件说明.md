>这版以已经冻结的 6 张定稿样式和页面规格稿为依据，目标是让 **Copilot 能稳定出代码、Codex 能按规则 review**，而不是继续做抽象设计描述。

---

# 《时光回序》设计 Token 与组件说明（开发版 / P0）

## 0. 文档目的

本文档用于冻结《时光回序》前端开发中的：

* 全局设计 Token
* 页面级例外规则
* 基础组件清单
* 业务组件清单
* 组件状态定义
* 组件复用边界

适用技术栈：

* Uniapp
* Vue 3
* SCSS
* 自定义 Tabbar
* 可配合 Pinia / 本地缓存 / 接口层

本文档优先服务于：

* Copilot 页面骨架生成
* Codex 工程规划与 review
* 前端样式统一
* 组件抽象与复用

---

## 1. 实现总原则

### 1.1 设计系统优先级

当不同文档之间出现冲突时，优先级按以下顺序处理：

1. 页面定稿视觉
2. 页面级设计样式与工程规格说明
3. 本文档（Token 与组件说明）
4. 页面内临时样式实现

### 1.2 首版目标

首版优先保证：

* 页面职责正确
* 导航显隐正确
* 页面结构正确
* 关键视觉元素在位
* 样式风格统一

首版不以这些为阻塞项：

* 高级情绪化动效
* 自定义字体精细还原
* 所有辅助入口完整能力
* 所有状态的最终化微交互

### 1.3 组件化原则

样式不应长期散写在页面内。
建议按以下层级组织：

* **Token 层**：颜色、字号、间距、圆角、阴影、层级
* **基础组件层**：TopAppBar、BottomNavBar、Button、Input、Tag 等
* **业务组件层**：首页卡片、档案条目、时间轴节点、旧信纸、设置分组等
* **页面层**：6 个定稿页面

---

## 2. Token 命名约定

### 2.1 推荐命名方式

建议在 `uni.scss` 或 `theme.scss` 中统一定义为 SCSS 变量。
推荐命名：

```scss
$color-bg-main:
$color-surface:
$color-primary:
$radius-lg:
$space-page-x:
$font-size-title-xl:
$shadow-card-home:
```

如需运行时主题能力，可同步暴露为 CSS Variables：

```css
--color-bg-main:
--radius-lg:
--space-page-x:
```

### 2.2 命名原则

* 使用语义命名，不用纯视觉命名
* 先全局语义，再页面例外
* 不直接使用 `$blue1`、`$gray2` 这类弱语义命名

---

## 3. 颜色 Token

## 3.1 全局基础色

```scss
$color-bg-main: #F8FAFB;        // 页面主背景
$color-surface: #FFFFFF;        // 白色卡片 / 纸张主容器
$color-surface-warm: #FFFDF5;   // 解锁页旧信纸暖色容器
$color-primary: #3B647A;        // 主品牌蓝灰
$color-primary-press: #33586B;  // 主按钮按压态
$color-accent-warm: #FDF2D9;    // 极淡暖色高亮
$color-text-primary: #1A1A1A;   // 主标题 / 正文重点
$color-text-secondary: #7F8C93; // 次级说明 / 时间 / 辅助文案
$color-text-tertiary: #A3ADB3;  // 更弱辅助文案
$color-border: rgba(172, 179, 182, 0.18); // 轻边框 / 分割线
$color-overlay-light: rgba(255, 255, 255, 0.6); // 半透明容器
$color-overlay-dark: rgba(26, 26, 26, 0.08);    // 弱遮罩 / 浅投影辅助
```

## 3.2 状态辅助色

首版避免强功能色系统，只保留弱提示层级：

```scss
$color-tag-draft-bg: #F6E8D8;
$color-tag-draft-text: #8A6B4A;

$color-tag-sealed-bg: #EEF2F5;
$color-tag-sealed-text: #5E6B73;

$color-tag-unlocked-bg: #EAF1F4;
$color-tag-unlocked-text: #3B647A;
```

## 3.3 使用规则

* `#F8FAFB` 是全局主背景，不可被大面积替换
* `#FFFFFF` 用于首页卡片、列表容器、新建页纸张、设置卡
* `#FFFDF5` 仅用于解锁页旧信纸及极少量暖色高亮节点
* 主蓝灰色只用于：

  * 主按钮
  * 激活态 Tab
  * 少量重点文字 / 图标
* 禁止引入高饱和品牌色或强烈渐变

---

## 4. 字体 Token

## 4.1 首版字体策略

首版不以自定义 WebFont 为阻塞项，优先使用系统字体栈：

```scss
$font-family-title: serif;
$font-family-body: sans-serif;
$font-family-mono-accent: monospace;
```

说明：

* 标题与旧信正文优先使用系统衬线气质
* 正文、设置项、按钮、输入框使用系统无衬线
* 编号、档案小标签、日期小字可使用等宽感字体或系统 fallback

## 4.2 字号与行高 Token

```scss
$font-size-title-xl: 32px; // 统计大数字 / 局部超大标题
$line-height-title-xl: 1.2;

$font-size-title-lg: 28px; // 新建页日期主标题
$line-height-title-lg: 1.4;

$font-size-title-md: 26px; // 首页 / 时间轴 Hero 标题
$line-height-title-md: 1.35;

$font-size-heading: 18px;  // 分区标题 / 页面二级标题
$line-height-heading: 1.4;

$font-size-body-lg: 16px;  // 解锁页正文 / 新建页正文
$line-height-body-lg: 1.8;

$font-size-body-md: 15px;  // 首页正文 / 普通说明
$line-height-body-md: 1.6;

$font-size-body-sm: 14px;  // 列表摘要 / 输入占位 / 标签
$line-height-body-sm: 1.6;

$font-size-caption: 12px;  // 时间标签 / 节点日期 / 极弱说明
$line-height-caption: 1.5;

$font-size-micro: 10px;    // 竖排标识 / 极小辅助英文字
$line-height-micro: 1.4;
```

## 4.3 页面级使用规则

* 首页 Hero 标题：`$font-size-title-md`
* 新建页日期主标题：`$font-size-title-lg`
* 解锁页正文：`$font-size-body-lg`
* 我的档案摘要：`$font-size-body-sm`
* 个人中心统计数字：`$font-size-title-xl`
* 时间轴日期：`$font-size-caption`

---

## 5. 间距 Token

## 5.1 基础间距

```scss
$space-1: 4px;
$space-2: 8px;
$space-3: 12px;
$space-4: 16px;
$space-5: 20px;
$space-6: 24px;
$space-8: 32px;
$space-10: 40px;
$space-12: 48px;
```

## 5.2 页面级常用间距

```scss
$space-page-x: 24px;         // 页面左右边距
$space-section-gap: 20px;    // 模块间主间距
$space-section-gap-lg: 32px; // Hero 与主内容区间距
$space-card-padding: 20px;   // 普通卡片 padding
$space-paper-padding: 32px;  // 新建页 / 解锁页纸张内边距
$space-list-gap: 16px;       // 列表项间距
$space-timeline-gap: 40px;   // 时间轴节点主间距
```

## 5.3 页面级例外

* 首页 Hero 到摘要区：优先用 `32px`
* 新建页 / 解锁页纸张容器：优先用 `32px`
* 时间轴节点：不低于 `40px`
* 个人中心分组之间：默认 `24px`，允许到 `32px`

---

## 6. 圆角 Token

```scss
$radius-xs: 8px;    // 解锁页旧信纸、小标签
$radius-sm: 12px;   // 小型辅助容器
$radius-md: 20px;   // 普通中型容器
$radius-lg: 28px;   // 管理页、通用大卡默认
$radius-xl: 32px;   // 首页主卡 / 新建页纸张 / 个人中心卡
$radius-full: 999px; // 胶囊按钮 / 搜索框 / 圆形控件
```

## 6.1 使用规则

* **首页主卡、摘要卡、管理入口卡**：`32px`
* **我的档案列表卡**：默认 `28px`
* **新建页纸张**：`32px`
* **解锁页旧信纸**：`8px`
* **搜索框 / 主按钮 / Tab 激活胶囊**：`999px`

---

## 7. 阴影 Token

## 7.1 首版阴影定义

```scss
$shadow-card-home: 0 12px 28px rgba(26, 26, 26, 0.08);
$shadow-card-list: 0 4px 20px rgba(44, 52, 54, 0.02);
$shadow-paper-compose: 0 16px 32px rgba(26, 26, 26, 0.06);
$shadow-paper-unlock: 0 8px 20px rgba(26, 26, 26, 0.04);
$shadow-fab: 0 10px 24px rgba(59, 100, 122, 0.18);
$shadow-tabbar: 0 -6px 20px rgba(26, 26, 26, 0.06);
$shadow-none: none;
```

## 7.2 使用规则

* 首页卡片阴影最强
* 我的档案列表卡阴影最轻
* 新建页纸张阴影弱于首页主卡，但强于普通列表卡
* 解锁页旧信纸阴影必须轻，避免厚重拟物感
* FAB 必须有独立浮层感

---

## 8. 层级 Token

```scss
$z-base: 1;
$z-card: 10;
$z-sticky-header: 20;
$z-tabbar: 30;
$z-fab: 40;
$z-overlay: 50;
$z-modal: 60;
$z-toast: 70;
```

## 8.1 使用规则

* 首页 FAB 高于首页卡片和底图
* Sticky 顶栏高于列表内容
* Tabbar 高于滚动内容
* Toast / 轻提示高于页面层

---

## 9. 动效 Token（首版轻量）

```scss
$motion-fast: 160ms;
$motion-base: 220ms;
$motion-slow: 320ms;

$ease-standard: ease;
$ease-out-soft: ease-out;
```

## 9.1 首版使用范围

* 按钮按压反馈
* 页面轻微淡入淡出
* Tab 激活态位置变化
* Toast 轻提示出现消失

首版不强制实现：

* 纸张展开
* 卡片扩展
* 情绪化收拢转场

---

## 10. 基础组件清单

## 10.1 TopAppBar

### 作用

统一顶部标题区。

### 变体

* `home`
* `archive`
* `timeline`
* `profile`
* `minimal`

### 首版属性建议

```ts
title: string
showBack?: boolean
showHistory?: boolean
showSearch?: boolean
showMore?: boolean
```

### 页面映射

* 首页：`home`
* 我的档案：`archive`
* 时间轴：`timeline`
* 个人中心：`profile`
* 新建页 / 解锁页：不使用标准 TopAppBar

---

## 10.2 BottomNavBar

### 作用

一级页面底部导航。

### 固定项

* 首页
* 时间轴
* 个人中心

### 状态

* 默认态
* 激活态
* 按压态

### 开发要求

* 使用自定义 Tabbar
* 激活态为胶囊高亮
* 非一级页面不得显示

---

## 10.3 CloseXButton

### 作用

新建页、解锁页右上角唯一显性退出入口。

### 状态

* 默认态
* 按压态

### 使用规则

* 仅新建页、解锁页可用
* 不得与返回箭头并存

---

## 10.4 PrimaryButton

### 作用

通用主按钮基类。

### 变体

* `seal`
* `response`

### 状态

* 默认态
* 按压态
* 禁用态
* 加载态（后续可选）

### 首版规范

* 胶囊形
* 蓝灰主色背景
* 白色文字
* 可带轻微箭头图标

---

## 10.5 SearchBar

### 作用

我的档案页搜索入口。

### 状态

* 默认态
* 聚焦态
* 输入态
* 清空态（可后置）

### 首版范围

* 支持输入
* 支持基础筛选触发
* 不强制做关键词高亮

---

## 10.6 FilterSegmentControl

### 作用

我的档案状态筛选。

### 选项

* 全部
* 草稿
* 已封存
* 已解锁

### 状态

* 默认态
* 选中态
* 按压态

---

## 10.7 ToastHint

### 作用

轻提示。

### 使用场景

* 封存条目点击提示“距离解封还有 xx 天”
* 解锁页“留下回应”成功提示

### 状态

* 成功态
* 普通信息态

---

## 11. 业务组件清单

## 11.1 DraftEntryCard

### 页面

首页

### 作用

草稿主入口卡。

### 内容建议

* 标题
* 草稿摘要
* 更新时间 / 时间信息
* 状态标签（可选）

### 视觉特征

* 大圆角
* 首页最强入口感
* 阴影强于普通卡片

---

## 11.2 SummaryStatCard

### 页面

首页

### 用途

* 封存总数卡
* 最近解锁卡

### 状态分流

* 封存总数：点击后轻提示
* 最近解锁：点击后进入解锁页

### 视觉特征

* 两列并排
* 强调信息摘要，不是复杂内容卡

---

## 11.3 ManagementEntryCard

### 页面

首页

### 用途

进入“我的档案”

### 视觉特征

* 通栏
* 大圆角
* 文案与箭头明确

---

## 11.4 ArchiveListItem

### 页面

我的档案

### 用途

展示列表型档案条目。

### 字段建议

```ts
id: string
title: string
excerpt: string
status: 'draft' | 'sealed' | 'unlocked'
timeText: string
```

### 状态分流

* `draft` -> 新建编辑态
* `sealed` -> 轻提示
* `unlocked` -> 解锁页

---

## 11.5 PaperContainer

### 页面

新建页

### 用途

承载书写内容的白色纸张。

### 视觉特征

* 白色
* 32px 圆角
* 柔和阴影
* 大留白

---

## 11.6 WritingInput

### 页面

新建页

### 用途

正文输入区。

### 状态

* 默认态
* 输入态
* 占位态
* 键盘弹出态

### 首版要求

* 无边框
* 多行
* 自然行高
* 可用于编辑已有草稿

---

## 11.7 StatusIconGroup

### 页面

新建页

### 内容

* MAP
* IMAGE
* VOICE

### 定位

视觉与辅助记录入口展示组件。

### 首版要求

* 保留 UI
* 不要求完整能力闭环

---

## 11.8 SealButton

### 页面

新建页

### 用途

“封存这一刻”主操作按钮。

### 状态

* 默认态
* 按压态
* 禁用态（按需）
* 触发态（后续可增强）

---

## 11.9 AncientPaperCard

### 页面

解锁页

### 用途

旧信主容器。

### 视觉特征

* 暖米色
* 8px 圆角
* 主叙事容器
* 阴影轻

---

## 11.10 LetterMetaInfo

### 页面

解锁页

### 内容

* ARCHIVE NO.
* 时间
* 地点

### 定位

解锁页顶部氛围元信息，不承担复杂交互。

---

## 11.11 PostscriptInput

### 页面

解锁页

### 用途

轻量附言输入区。

### 状态

* 默认态
* 输入态
* 占位态

### 首版要求

* 无边框
* 半透明容器内布局
* 输入区与按钮同属回应容器

---

## 11.12 ResponseButton

### 页面

解锁页

### 用途

“留下回应”主按钮。

### 首版行为

点击后只提示成功，不自动跳转。

---

## 11.13 TimelineAxis

### 页面

时间轴

### 用途

纵向单列时间轴线。

### 首版要求

* 细线
* 偏左布局
* 不可断裂成多列结构

---

## 11.14 TimelineNode

### 页面

时间轴

### 类型

* 草稿节点
* 已封存节点
* 已解锁节点

### 跳转规则

* 草稿 -> 新建编辑态
* 已封存 -> 轻提示
* 已解锁 -> 解锁页

---

## 11.15 MomentHighlightCard

### 页面

时间轴

### 用途

展示重点时间节点内容。

### 视觉特征

* 大圆角
* 包裹感
* 内容量低于首页展陈卡

---

## 11.16 ProfileHeader

### 页面

个人中心

### 内容

* 头像
* 昵称
* 签名

### 作用

建立首屏个人识别感。

---

## 11.17 StatisticCard

### 页面

个人中心

### 内容

* 已存记忆
* 存档天数

### 视觉特征

* 双卡布局
* 大圆角
* 右侧可弱暖色偏移

---

## 11.18 SettingGroupCard

### 页面

个人中心

### 用途

设置项分组容器。

### 分组建议

* 档案设置
* 隐私与安全
* 关于

---

## 12. 组件状态说明

## 12.1 按钮类组件

适用于：

* FloatingActionButton
* SealButton
* ResponseButton
* PrimaryButton

### 状态

* 默认态：品牌色背景，白字
* 按压态：背景略深，轻微缩放 `0.98`
* 禁用态：降低对比度与透明度
* 加载态：首版可不做

---

## 12.2 卡片类组件

适用于：

* DraftEntryCard
* SummaryStatCard
* ManagementEntryCard
* ArchiveListItem
* MomentHighlightCard
* StatisticCard
* SettingGroupCard

### 状态

* 默认态
* 按压态：阴影微调 / 透明度轻降
* 空态：保留容器结构但内容弱化

---

## 12.3 输入类组件

适用于：

* SearchBar
* WritingInput
* PostscriptInput

### 状态

* 默认态
* 聚焦态
* 输入态
* 占位态

---

## 12.4 导航类组件

适用于：

* TopAppBar
* BottomNavBar
* CloseXButton

### 状态

* 默认态
* 激活态（BottomNavBar）
* 按压态
* 隐藏态（按页面规则）

---

## 13. 页面级例外规则

## 13.1 首页

* 圆角最大
* 阴影最强
* 允许静物底图
* 允许更强入口感
* 不允许管理化

## 13.2 我的档案

* 信息密度高于首页
* 阴影最轻
* 不显示底部导航
* 不允许首页式大展陈

## 13.3 新建页

* 无标准顶部栏
* 无底部导航
* 白色纸张 32px 圆角
* 留白最大
* 视觉最安静

## 13.4 解锁页

* 全局最小圆角
* 全局唯一明显暖色主容器
* 正文优先级高于操作区
* 不得工具化

## 13.5 时间轴

* 单列纵向
* 轴线偏左
* 可混合文本节点与高光卡片
* 不可变成图表页

## 13.6 个人中心

* 首屏以头像 + 统计卡为主
* 顶部允许轻量更多入口
* 不显示悬浮新建按钮
* 强控制感，弱内容感

---

## 14. 推荐目录结构

```text
src/
  styles/
    theme.scss
    mixins.scss

  components/
    base/
      TopAppBar.vue
      BottomNavBar.vue
      CloseXButton.vue
      PrimaryButton.vue
      SearchBar.vue
      ToastHint.vue

    business/
      DraftEntryCard.vue
      SummaryStatCard.vue
      ManagementEntryCard.vue
      ArchiveListItem.vue
      PaperContainer.vue
      WritingInput.vue
      StatusIconGroup.vue
      AncientPaperCard.vue
      LetterMetaInfo.vue
      PostscriptInput.vue
      TimelineAxis.vue
      TimelineNode.vue
      MomentHighlightCard.vue
      ProfileHeader.vue
      StatisticCard.vue
      SettingGroupCard.vue
```

---

## 15. 给 Copilot 的实现备注

Copilot 生成代码时应遵守：

1. 先写 Token，再写组件，不要先把样式全部散写进页面
2. 首页 / 新建页 / 解锁页 / 时间轴 / 个人中心的例外规则必须保留
3. BottomNavBar 必须只出现在 3 个一级页面
4. 新建页与解锁页必须共用 `CloseXButton`
5. 我的档案与时间轴不要错误复用首页展陈卡样式
6. 解锁页不能复用首页 32px 主卡语言
7. 组件 props 命名尽量语义化，不用 `type1/type2`

---

## 16. 给 Codex 的 review 备注

Codex review 时优先检查：

* Token 是否被统一定义
* 页面是否大量散写重复样式
* 组件边界是否清晰
* 首页卡片语言是否被滥用到管理页
* 解锁页旧信纸是否错误使用了大圆角
* 新建页 / 解锁页是否错误出现标准导航
* BottomNavBar 是否只存在于一级页面
* SearchBar / FilterSegmentControl / TimelineNode 是否按业务职责拆分

---

## 17. 首版必须冻结的 Token / 组件规则

以下内容进入 P0 后不应随意更改：

1. `#F8FAFB` 作为全局主背景
2. 首页 / 新建页 / 个人中心主大卡使用 32px 圆角语言
3. 解锁页旧信纸使用 8px 小圆角语言
4. 主品牌色使用蓝灰色体系
5. 首页阴影强于管理页
6. BottomNavBar 只存在于首页 / 时间轴 / 个人中心
7. 新建页 / 解锁页都不使用标准 TopAppBar
8. 新建页必须保留 `Vol.`、`Captured at`、竖排标识、MAP / IMAGE / VOICE
9. 解锁页必须保留顶部元信息层、旧信纸主容器、回应容器
10. 时间轴必须保留单列纵向轴线
11. 个人中心必须保留头像 + 双统计卡 + 设置分组的首屏结构
