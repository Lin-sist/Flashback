<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import BottomNavBar from '../../components/common/BottomNavBar.vue'

// 纯静态视觉母版：不做接口调用、不做状态管理，所有数据写死，仅用于走查视觉还原。
const draftTitle = '冬日午后的光影...'
const sealedTotal = 12
const lastUnlockLabel = '三月十二日'

const goEditor = () => uni.navigateTo({ url: '/pages/record-editor/index?source=home' })
const goArchive = () => uni.navigateTo({ url: '/pages/record-list/index' })
const goHistory = () => uni.navigateTo({ url: '/pages/record-list/index' })

onShow(() => {
  uni.hideTabBar({ animation: false })
})
</script>

<template>
  <view class="page">
    <!-- 顶部栏：居中品牌标题 + 右上角历史入口 -->
    <view class="top-bar">
      <view class="top-bar-side" />
      <view class="brand">时光回序</view>
      <view class="top-bar-side right" @tap="goHistory">
        <view class="icon icon-history" />
      </view>
    </view>

    <!-- 大标题 -->
    <view class="hero">
      <view class="hero-title">那些被封存的<br />碎片</view>
      <view class="hero-subtitle">在时间的灰烬里，寻回那些不曾褪色的真实片段。</view>
    </view>

    <!-- 草稿入口大卡 -->
    <view class="draft-card" @tap="goEditor">
      <view class="draft-head">
        <text class="kicker">DRAFT ENTRY</text>
        <view class="icon icon-edit" />
      </view>
      <view class="draft-title">{{ draftTitle }}</view>
      <view class="draft-chip">
        <view class="icon icon-info" />
        <text class="draft-chip-text">继续完成此篇章</text>
      </view>
    </view>

    <!-- 摘要双卡：封存总数 / 最近解封 -->
    <view class="summary-row">
      <view class="summary-card summary-card-cool" @tap="goArchive">
        <view class="icon icon-archive" />
        <view class="summary-value">{{ sealedTotal }}</view>
        <view class="summary-label">封存记录总数</view>
      </view>
      <view class="summary-card summary-card-warm">
        <view class="icon icon-lock" />
        <view class="summary-value summary-value-date">{{ lastUnlockLabel }}</view>
        <view class="summary-label">最近解封的记忆</view>
      </view>
    </view>

    <!-- 我的档案入口 -->
    <view class="archive-entry" @tap="goArchive">
      <view class="archive-icon-wrap">
        <view class="icon icon-folder" />
      </view>
      <view class="archive-text">
        <view class="archive-title">我的档案</view>
        <view class="archive-meta">管理所有属于你的时光印记</view>
      </view>
      <text class="archive-arrow">›</text>
    </view>

    <!-- 场景图：黑白书桌静物 -->
    <view class="scene-wrap">
      <image class="scene-image" src="/static/home-scene.jpg" mode="aspectFill" />
    </view>

    <!-- 浮动新增按钮 -->
    <view class="fab" @tap="goEditor">
      <text class="fab-plus">+</text>
    </view>

    <BottomNavBar current="home" />
  </view>
</template>

<style scoped>
.page {
  min-height: 100vh;
  padding: 20rpx 40rpx 260rpx;
  background: var(--fb-color-bg);
}

/* ---------- Top Bar ---------- */
.top-bar {
  height: 88rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 8rpx;
}

.top-bar-side {
  width: 80rpx;
  height: 80rpx;
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.brand {
  flex: 1;
  text-align: center;
  font-size: 34rpx;
  font-weight: 500;
  letter-spacing: 4rpx;
  color: var(--fb-color-primary);
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
}

/* ---------- Hero ---------- */
.hero {
  margin-top: 40rpx;
}

.hero-title {
  font-size: 76rpx;
  line-height: 1.15;
  font-weight: 700;
  color: #111418;
  letter-spacing: 2rpx;
}

.hero-subtitle {
  margin-top: 28rpx;
  font-size: 28rpx;
  line-height: 1.75;
  color: #9aa3a9;
  max-width: 540rpx;
}

/* ---------- Draft Card ---------- */
.draft-card {
  margin-top: 64rpx;
  padding: 36rpx 36rpx 32rpx;
  border-radius: 36rpx;
  background: #eef2f5;
  box-shadow: 0 2rpx 0 rgba(255, 255, 255, 0.6) inset;
}

.draft-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.kicker {
  font-size: 22rpx;
  letter-spacing: 4rpx;
  color: #9aa3a9;
  font-weight: 500;
}

.draft-title {
  margin-top: 14rpx;
  font-size: 42rpx;
  font-weight: 600;
  color: #1a1a1a;
  letter-spacing: 1rpx;
}

.draft-chip {
  margin-top: 28rpx;
  display: inline-flex;
  align-items: center;
  gap: 12rpx;
  padding: 10rpx 0;
}

.draft-chip-text {
  font-size: 26rpx;
  color: #6f7a80;
}

/* ---------- Summary Row ---------- */
.summary-row {
  margin-top: 24rpx;
  display: flex;
  gap: 20rpx;
}

.summary-card {
  flex: 1;
  height: 260rpx;
  padding: 28rpx 28rpx 24rpx;
  border-radius: 32rpx;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.summary-card-cool {
  background: #eef2f5;
}

.summary-card-warm {
  background: #f6d79a;
}

.summary-value {
  font-size: 80rpx;
  line-height: 1;
  font-weight: 300;
  color: #1a1a1a;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
}

.summary-value-date {
  font-size: 44rpx;
  font-weight: 500;
  letter-spacing: 2rpx;
}

.summary-label {
  font-size: 24rpx;
  color: #6f7a80;
}

.summary-card-warm .summary-label {
  color: #7a6a40;
}

/* ---------- Archive Entry ---------- */
.archive-entry {
  margin-top: 24rpx;
  padding: 24rpx 28rpx;
  border-radius: 32rpx;
  background: #eef2f5;
  display: flex;
  align-items: center;
  gap: 24rpx;
}

.archive-icon-wrap {
  width: 76rpx;
  height: 76rpx;
  border-radius: 999rpx;
  background: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.archive-text {
  flex: 1;
  min-width: 0;
}

.archive-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #1a1a1a;
}

.archive-meta {
  margin-top: 6rpx;
  font-size: 24rpx;
  color: #9aa3a9;
}

.archive-arrow {
  font-size: 44rpx;
  color: #9aa3a9;
  line-height: 1;
  padding-right: 8rpx;
}

/* ---------- Scene Image ---------- */
.scene-wrap {
  margin-top: 28rpx;
  border-radius: 32rpx;
  overflow: hidden;
  background: #e8ecef;
}

.scene-image {
  width: 100%;
  height: 360rpx;
  display: block;
}

/* ---------- FAB ---------- */
.fab {
  position: fixed;
  right: 56rpx;
  bottom: calc(env(safe-area-inset-bottom) + 160rpx);
  width: 108rpx;
  height: 108rpx;
  border-radius: 999rpx;
  background: #3a4a55;
  box-shadow: 0 12rpx 28rpx rgba(58, 74, 85, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 90;
}

.fab-plus {
  color: #ffffff;
  font-size: 60rpx;
  line-height: 1;
  font-weight: 300;
  margin-top: -4rpx;
}

/* ---------- Icons (SVG data URI, compatible with mp-weixin) ---------- */
.icon {
  width: 36rpx;
  height: 36rpx;
  background-repeat: no-repeat;
  background-position: center;
  background-size: contain;
}

.icon-history {
  width: 40rpx;
  height: 40rpx;
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%233b647a' stroke-width='1.8' stroke-linecap='round' stroke-linejoin='round'><path d='M3 12a9 9 0 1 0 3-6.7'/><polyline points='3 4 3 9 8 9'/><polyline points='12 7 12 12 15 14'/></svg>");
}

.icon-edit {
  width: 40rpx;
  height: 40rpx;
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%235a6870' stroke-width='1.6' stroke-linecap='round' stroke-linejoin='round'><path d='M4 20h4l10-10-4-4L4 16v4z'/><path d='M14 6l4 4'/><line x1='4' y1='20' x2='12' y2='20'/></svg>");
}

.icon-info {
  width: 32rpx;
  height: 32rpx;
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%235a6870'><circle cx='12' cy='12' r='10'/><circle cx='12' cy='8' r='1.3' fill='white'/><rect x='11' y='11' width='2' height='7' rx='1' fill='white'/></svg>");
}

.icon-archive {
  width: 44rpx;
  height: 44rpx;
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%233b647a' stroke-width='1.6' stroke-linecap='round' stroke-linejoin='round'><rect x='3' y='4' width='18' height='4' rx='1'/><path d='M5 8v11a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1V8'/><line x1='10' y1='13' x2='14' y2='13'/></svg>");
}

.icon-lock {
  width: 44rpx;
  height: 44rpx;
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%237a5a20' stroke-width='1.8' stroke-linecap='round' stroke-linejoin='round'><rect x='5' y='11' width='14' height='10' rx='2'/><path d='M8 11V7a4 4 0 0 1 8 0v4'/></svg>");
}

.icon-folder {
  width: 42rpx;
  height: 42rpx;
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%233b647a' stroke-width='1.6' stroke-linecap='round' stroke-linejoin='round'><path d='M3 6a2 2 0 0 1 2-2h4l2 2h8a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z'/><circle cx='15' cy='13' r='2.4'/><path d='M15 9.8v1.2M15 15v1.2M18.2 13h-1.2M13 13h-1.2M17.3 10.7l-0.85 0.85M13.55 14.45l-0.85 0.85M17.3 15.3l-0.85-0.85M13.55 11.55l-0.85-0.85'/></svg>");
}
</style>
