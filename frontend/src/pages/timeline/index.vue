<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'

/**
 * 纯静态视觉母版：不处理接口、不处理数据请求、不处理筛选/路由逻辑。
 * 仅用于还原《时光回序 · 时间长廊》页面的高保真视觉走查。
 */

type NodeType = 'sealed-text' | 'anchor' | 'memory-card' | 'reading-card'

interface TimelineNodeVO {
  type: NodeType
  date?: string
  title?: string
  body?: string
  tags?: string[]
  image?: string
  locked?: boolean
  anchorLabel?: string
}

const nodes: TimelineNodeVO[] = [
  {
    type: 'sealed-text',
    date: '2025.12.24',
    title: '致未来的信件',
    locked: true,
  },
  {
    type: 'anchor',
    anchorLabel: '此时此刻',
  },
  {
    type: 'memory-card',
    date: '2023.08.15',
    title: '夏末的萤火之旅',
    body: '溪水边的微光，在掌心停留了三秒。那是那个夏天留给我最后的温存。',
    image: '/static/memory-mountain.jpg',
  },
  {
    type: 'reading-card',
    date: '2023.05.02',
    title: '旧书店里的午后',
    body: '"时间不是流逝的，而是堆积的。" 在那本发黄的诗集扉页，我读到了这一句。',
    tags: ['READING', 'MEMORY'],
  },
  {
    type: 'sealed-text',
    date: '2022.12.31',
    title: '那一年的终章',
    locked: true,
  },
]

const tabs = [
  { key: 'home', label: '首页', path: '/pages/home/index' },
  { key: 'timeline', label: '时间轴', path: '/pages/timeline/index' },
  { key: 'user-center', label: '个人', path: '/pages/user-center/index' },
] as const

type TabKey = (typeof tabs)[number]['key']
const currentTab: TabKey = 'timeline'

const switchTab = (key: TabKey, path: string) => {
  if (key === currentTab) return
  uni.switchTab({ url: path })
}

onShow(() => {
  uni.hideTabBar({ animation: false })
})
</script>

<template>
  <view class="page">
    <!-- Top Bar: search / brand / empty -->
    <view class="top-bar">
      <view class="top-bar-slot left">
        <view class="icon icon-search" />
      </view>
      <view class="brand">时光回序</view>
      <view class="top-bar-slot right" />
    </view>

    <!-- Hero -->
    <view class="hero">
      <view class="hero-title">时间长廊</view>
      <view class="hero-subtitle">在此处，凝视那些被封存的往昔与尚未开启的明日。</view>
    </view>

    <!-- Timeline rail -->
    <view class="rail">
      <view class="rail-line" />

      <view
        v-for="(node, idx) in nodes"
        :key="idx"
        class="rail-row"
        :class="[
          node.type === 'anchor' ? 'rail-row-anchor' : '',
          node.type === 'sealed-text' ? 'rail-row-sealed' : '',
        ]"
      >
        <!-- Dot -->
        <view
          class="dot"
          :class="{
            'dot-active': node.type === 'anchor' || node.type === 'memory-card' || node.type === 'reading-card',
            'dot-dim': node.type === 'sealed-text',
          }"
        />

        <!-- Content -->
        <view class="row-content">
          <!-- Sealed plain text node -->
          <template v-if="node.type === 'sealed-text'">
            <view class="sealed-date">{{ node.date }}</view>
            <view class="sealed-line">
              <text class="sealed-title">{{ node.title }}</text>
              <view class="icon icon-lock" />
            </view>
          </template>

          <!-- "Now" anchor chip -->
          <template v-else-if="node.type === 'anchor'">
            <view class="anchor-chip">{{ node.anchorLabel }}</view>
          </template>

          <!-- White memory card: image + title + body -->
          <template v-else-if="node.type === 'memory-card'">
            <view class="card-date">{{ node.date }}</view>
            <view class="memory-card">
              <image class="memory-image" :src="node.image" mode="aspectFill" />
              <view class="memory-body">
                <view class="memory-title">{{ node.title }}</view>
                <view class="memory-text">{{ node.body }}</view>
              </view>
            </view>
          </template>

          <!-- Warm paper reading card -->
          <template v-else-if="node.type === 'reading-card'">
            <view class="card-date">{{ node.date }}</view>
            <view class="reading-card">
              <view class="reading-title">{{ node.title }}</view>
              <view class="reading-text">{{ node.body }}</view>
              <view class="reading-tags">
                <view class="reading-tag" v-for="t in node.tags" :key="t">{{ t }}</view>
              </view>
            </view>
          </template>
        </view>
      </view>
    </view>

    <!-- Tail: dots + coda line -->
    <view class="coda">
      <view class="coda-dots">
        <view class="coda-dot" />
        <view class="coda-dot" />
        <view class="coda-dot" />
      </view>
      <view class="coda-line">回溯的终点，亦是感知的起点。</view>
    </view>

    <!-- Floating pill tab bar -->
    <view class="tabbar-shell">
      <view class="tabbar">
        <view
          v-for="tab in tabs"
          :key="tab.key"
          class="tab"
          :class="{ 'tab-active': tab.key === currentTab }"
          @tap="switchTab(tab.key, tab.path)"
        >
          <view
            class="tab-icon"
            :class="[
              tab.key === 'home' ? 'tab-icon-home' : '',
              tab.key === 'timeline' ? 'tab-icon-clock' : '',
              tab.key === 'user-center' ? 'tab-icon-user' : '',
              tab.key === currentTab ? 'tab-icon-active' : '',
            ]"
          />
          <text class="tab-label">{{ tab.label }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<style scoped>
.page {
  min-height: 100vh;
  padding: 16rpx 40rpx 260rpx;
  background: #f6f8fa;
}

/* ---------- Top Bar ---------- */
.top-bar {
  height: 88rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.top-bar-slot {
  width: 80rpx;
  height: 80rpx;
  display: flex;
  align-items: center;
}

.top-bar-slot.right {
  justify-content: flex-end;
}

.brand {
  flex: 1;
  text-align: center;
  font-size: 34rpx;
  letter-spacing: 4rpx;
  color: #1a1a1a;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
  font-weight: 500;
}

/* ---------- Hero ---------- */
.hero {
  margin-top: 24rpx;
  padding: 0 8rpx 16rpx;
}

.hero-title {
  font-size: 88rpx;
  line-height: 1.1;
  font-weight: 600;
  color: #2a4c5e;
  letter-spacing: 6rpx;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
}

.hero-subtitle {
  margin-top: 26rpx;
  font-size: 26rpx;
  line-height: 1.9;
  color: #8a959b;
  max-width: 560rpx;
}

/* ---------- Rail ---------- */
.rail {
  position: relative;
  margin-top: 72rpx;
  padding-left: 60rpx;
}

.rail-line {
  position: absolute;
  left: 17rpx;
  top: 8rpx;
  bottom: 20rpx;
  width: 2rpx;
  background: #d5dde2;
}

.rail-row {
  position: relative;
  margin-bottom: 72rpx;
}

.rail-row-sealed {
  margin-bottom: 88rpx;
}

.rail-row-anchor {
  margin-bottom: 88rpx;
  margin-top: 8rpx;
}

/* Dots */
.dot {
  position: absolute;
  left: -50rpx;
  top: 18rpx;
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;
  background: #d5dde2;
}

.dot-active {
  background: #3b647a;
  width: 18rpx;
  height: 18rpx;
  left: -51rpx;
  box-shadow: 0 0 0 6rpx rgba(59, 100, 122, 0.08);
}

.dot-dim {
  background: #c9d1d6;
}

.row-content {
  position: relative;
}

/* ---------- Sealed plain text node ---------- */
.sealed-date {
  font-size: 24rpx;
  color: #b4bcc1;
  letter-spacing: 2rpx;
  font-family: 'Songti SC', 'STSong', serif;
}

.sealed-line {
  margin-top: 14rpx;
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.sealed-title {
  font-size: 38rpx;
  color: #a6afb5;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
  letter-spacing: 2rpx;
}

/* ---------- Anchor chip ---------- */
.anchor-chip {
  display: inline-block;
  padding: 12rpx 28rpx;
  border-radius: 999rpx;
  background: #e3ebf0;
  color: #3b647a;
  font-size: 24rpx;
  letter-spacing: 4rpx;
  font-family: 'Songti SC', 'STSong', serif;
}

/* ---------- Card date tag ---------- */
.card-date {
  font-size: 24rpx;
  color: #8a959b;
  letter-spacing: 2rpx;
  margin-bottom: 18rpx;
  font-family: 'Songti SC', 'STSong', serif;
}

/* ---------- Memory card (white w/ image) ---------- */
.memory-card {
  background: #ffffff;
  border-radius: 36rpx;
  padding: 20rpx 20rpx 30rpx;
  box-shadow: 0 12rpx 28rpx rgba(26, 40, 50, 0.05);
}

.memory-image {
  width: 100%;
  height: 260rpx;
  border-radius: 28rpx;
  display: block;
  background: #eef2f4;
}

.memory-body {
  padding: 24rpx 14rpx 6rpx;
}

.memory-title {
  font-size: 38rpx;
  font-weight: 600;
  color: #1a1a1a;
  letter-spacing: 2rpx;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
}

.memory-text {
  margin-top: 16rpx;
  font-size: 26rpx;
  line-height: 1.8;
  color: #6a757c;
}

/* ---------- Reading card (warm paper) ---------- */
.reading-card {
  background: #f5ead2;
  border-radius: 36rpx;
  padding: 34rpx 30rpx;
}

.reading-title {
  font-size: 38rpx;
  font-weight: 600;
  color: #3b3123;
  letter-spacing: 2rpx;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
}

.reading-text {
  margin-top: 18rpx;
  font-size: 26rpx;
  line-height: 1.9;
  color: #6b5a42;
}

.reading-tags {
  margin-top: 22rpx;
  display: flex;
  gap: 14rpx;
}

.reading-tag {
  padding: 8rpx 22rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.55);
  color: #8a7a5b;
  font-size: 22rpx;
  letter-spacing: 2rpx;
}

/* ---------- Coda ---------- */
.coda {
  margin-top: 40rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 36rpx;
}

.coda-dots {
  display: flex;
  gap: 14rpx;
}

.coda-dot {
  width: 10rpx;
  height: 10rpx;
  border-radius: 50%;
  background: #cfd6da;
}

.coda-line {
  font-size: 24rpx;
  color: #a8b2b7;
  letter-spacing: 2rpx;
  font-family: 'Songti SC', 'STSong', serif;
}

/* ---------- Floating tab bar ---------- */
.tabbar-shell {
  position: fixed;
  left: 40rpx;
  right: 40rpx;
  bottom: calc(env(safe-area-inset-bottom) + 28rpx);
  z-index: 80;
}

.tabbar {
  height: 120rpx;
  padding: 0 30rpx;
  border-radius: 999rpx;
  background: #ffffff;
  border: 1rpx solid rgba(172, 179, 182, 0.18);
  box-shadow: 0 12rpx 28rpx rgba(26, 40, 50, 0.08);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.tab {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6rpx;
  height: 100%;
  position: relative;
}

.tab-icon {
  width: 44rpx;
  height: 44rpx;
  background-repeat: no-repeat;
  background-position: center;
  background-size: 36rpx 36rpx;
}

.tab-label {
  font-size: 22rpx;
  color: #a0a9ae;
  letter-spacing: 2rpx;
}

.tab-active .tab-label {
  color: #8a959b;
}

/* active timeline tab: dark blue filled circle */
.tab-active .tab-icon {
  width: 76rpx;
  height: 76rpx;
  border-radius: 50%;
  background-color: #2f5566;
  background-size: 40rpx 40rpx;
  box-shadow: 0 8rpx 18rpx rgba(47, 85, 102, 0.3);
  transform: translateY(-22rpx);
}

.tab-active .tab-label {
  margin-top: -10rpx;
}

/* ---------- Icons (SVG data URI, mp-weixin safe) ---------- */
.icon {
  width: 36rpx;
  height: 36rpx;
  background-repeat: no-repeat;
  background-position: center;
  background-size: contain;
}

.icon-search {
  width: 40rpx;
  height: 40rpx;
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%231a1a1a' stroke-width='1.8' stroke-linecap='round' stroke-linejoin='round'><circle cx='11' cy='11' r='7'/><line x1='21' y1='21' x2='16.5' y2='16.5'/></svg>");
}

.icon-lock {
  width: 26rpx;
  height: 26rpx;
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23a6afb5' stroke-width='1.8' stroke-linecap='round' stroke-linejoin='round'><rect x='5' y='11' width='14' height='10' rx='2'/><path d='M8 11V7a4 4 0 0 1 8 0v4'/></svg>");
}

.tab-icon-home {
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23a0a9ae' stroke-width='1.7' stroke-linecap='round' stroke-linejoin='round'><path d='M3 10.5 12 3l9 7.5'/><path d='M5 10v10h14V10'/></svg>");
}

.tab-icon-clock {
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23a0a9ae' stroke-width='1.7' stroke-linecap='round' stroke-linejoin='round'><circle cx='12' cy='12' r='9'/><polyline points='12 7 12 12 15.5 14'/></svg>");
}

.tab-icon-user {
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23a0a9ae' stroke-width='1.7' stroke-linecap='round' stroke-linejoin='round'><circle cx='12' cy='8' r='4'/><path d='M4 21c0-4.4 3.6-8 8-8s8 3.6 8 8'/></svg>");
}

/* active clock icon: white stroke on dark circle */
.tab-active.tab .tab-icon-clock {
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23ffffff' stroke-width='1.8' stroke-linecap='round' stroke-linejoin='round'><circle cx='12' cy='12' r='9'/><polyline points='12 7 12 12 15.5 14'/></svg>");
}
</style>
