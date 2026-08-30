# Flashback「纸上留白」原型品牌规范

本原型沿用现有暖纸、深墨、淡墨与朱砂来源值，并将表达收敛为更现代、更安静的编辑排版。

```css
:root {
  --bg: oklch(0.9771 0.0074 80.7);
  --surface: oklch(0.9885 0.0057 84.6);
  --fg: oklch(0.3015 0.0092 88.8);
  --muted: oklch(0.5107 0.0109 62.4);
  --border: oklch(0.8161 0.0154 80.7);
  --accent: oklch(0.5200 0.1664 29.0);

  --font-display: "Noto Serif SC", "Songti SC", "STSong", Georgia, serif;
  --font-body: "Noto Sans SC", "PingFang SC", "Microsoft YaHei", system-ui, sans-serif;
  --font-mono: ui-monospace, "SFMono-Regular", Consolas, monospace;
}
```

视觉规则：

1. 纸白保持纯净，不叠加纸纹、折角、印章或怀旧滤镜。
2. 首页以一个主问题和一个主动作占据视觉中心，其他信息主动退后。
3. 编辑器保持无边框写作面；图片、声音与见证者均为次级入口。
4. 时光轴以细线、日期和片段组成，不使用卡片瀑布流或统计面板。
5. 朱砂只用于主动作和当前状态；同一屏幕不超过两处。

