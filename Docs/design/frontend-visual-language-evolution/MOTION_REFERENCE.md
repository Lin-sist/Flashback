# 动效参考与转译边界

## 已核验公开参考

- Inspora：首页公开分类包含 Web、Branding、Product、Motion 等，但动态画廊被 Vercel checkpoint 限制，本轮未完成逐项案例核验：<https://www.inspora.design/>
- Memesi Diary：Mobile、Adaptive Menu、Encyclopedic Notes、Index of Images 等内容索引机制：<https://www.awwwards.com/inspiration/diary-memesi>
- Elektra Virtual Museum：极简菜单、移动端编辑排版、艺术家索引/hover 揭示：<https://www.awwwards.com/inspiration/exhibition-page>
- Sperotto History Scroll：scroll + animation + timeline，含移动端和 3D 时间推进：<https://www.awwwards.com/inspiration/history-scroll-animation-sperotto-s-p-a>
- Aerleum：clean / fluid / reveal，包含文字揭示、页面过渡和液体效果：<https://www.awwwards.com/inspiration/page-scroll-aerleum>
- Charles Leclerc：移动画廊、日历排版、图像转场和 overlay gallery：<https://www.awwwards.com/inspiration/homepage-scroll-charles-leclerc>
- Repeat：横纵布局与卡片 stacking effect：<https://www.awwwards.com/inspiration/horizontal-and-vertical-scroll>

## 可迁移机制

| 网页机制 | Flashback 转译 | 静态/低动态降级 |
| --- | --- | --- |
| Fluid text reveal | 保存后整块文字/标题由 opacity + clip 显现 | 文字即时出现 |
| History scroll | 可见时间线延伸到当前节点 | 完整时间线与节点常显 |
| Artist/index hover | 点击/展开记录索引，显示媒体预览 | 行内摘要或固定缩略图 |
| Stacking effect | 篇章/媒体预览轻量堆叠 | 普通纵向卡片 |
| Image transition | 遮罩/淡化切换当前片段媒体 | 直接替换图片 |
| Calendar typography | 年/月/日作为稳定信息层级 | 固定日期排版 |
| Liquid/3D | 只提取“由模糊到明确/由远到近”的叙事意图 | opacity/scale 或静态层次 |

## 不直接迁移

- WebGL、Three.js、3D 场景和持续 GPU 渲染。
- 鼠标 hover 才能发现的信息。
- 横向滚动劫持、全页滚动锁定和长时间 preloader。
- 自动播放视频、背景声音或循环光效。
- 依赖 blur/filter 的长列表动效。
- 页面打开后超过必要范围的逐项延迟入场。

## 推荐优先验证的四项动效

1. 保存此刻：页面内轻量确认，不跳页、不发声。
2. 时间轴推进：节点与线段随浏览轻量建立时间方向。
3. 媒体展开：图片/声音从正文次级入口展开，不抢写作焦点。
4. 时间回看显现：过去内容清晰出现，同时保留“那时/此刻”边界。

