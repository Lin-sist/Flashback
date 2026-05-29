<script setup lang="ts">
import { onLoad } from '@dcloudio/uni-app'
import { hasAuthenticatedSession } from '../../../utils'

const ensureLogin = () => {
  if (!hasAuthenticatedSession()) {
    uni.reLaunch({ url: '/pages/login/index' })
    return false
  }
  return true
}

const goBack = () => {
  uni.navigateBack({
    delta: 1,
    fail: () => {
      uni.switchTab({ url: '/pages/user-center/index' })
    },
  })
}

onLoad(() => {
  ensureLogin()
})
</script>

<template>
  <view class="page">
    <scroll-view class="sub-page" scroll-y enhanced :show-scrollbar="false">
      <view class="top-nav">
        <view class="back-btn" @tap="goBack">
          <view class="back-arr" />
          <text class="back-label">我 的</text>
        </view>
        <view class="page-title">版 本 信 息</view>
        <view class="top-right-space" />
      </view>

      <!-- 版本号展示 -->
      <view style="text-align: center; margin-bottom: 56rpx;">
        <view style="font-family: var(--serif); font-size: 22rpx; font-weight: 300; color: var(--ink-light); letter-spacing: 0.35em; margin-bottom: 24rpx;">时 光 回 序</view>
        <view class="deco-line" style="margin: 0 auto 28rpx; width: 64rpx;" />
        <view style="font-family: var(--serif); font-size: 64rpx; font-weight: 300; color: var(--ink-mid); letter-spacing: 0.1em;">v 2.4.0</view>
        <view style="font-family: var(--sans); font-size: 20rpx; font-weight: 300; color: var(--ink-light); letter-spacing: 0.08em; margin-top: 12rpx;">2026年5月20日 发布</view>
      </view>

      <!-- 更新日志 -->
      <view class="section">
        <view class="section-label">更 新 日 志</view>
        <view class="group-card" style="padding: 0 44rpx 0 56rpx;">
          <view class="ver-entry">
            <view class="ver-num">v 2.4.0 <text style="font-size: 20rpx; color: var(--vermilion); opacity: 0.75; letter-spacing: 0.06em; margin-left: 8rpx;">当前版本</text></view>
            <view class="ver-date">2026 · 05 · 20</view>
            <view class="ver-items">
              <view class="ver-item">新增「纸色基调」四种主题切换</view>
              <view class="ver-item">优化首页信笺卡片入场动画节奏</view>
              <view class="ver-item">封存倒计时精度提升至分钟级</view>
              <view class="ver-item">修复部分设备下字重渲染异常</view>
            </view>
          </view>
          <view class="ver-entry">
            <view class="ver-num">v 2.3.1</view>
            <view class="ver-date">2026 · 03 · 08</view>
            <view class="ver-items">
              <view class="ver-item">时光轴新增「按情绪筛选」功能</view>
              <view class="ver-item">书写页面支持横屏模式</view>
              <view class="ver-item">数据备份稳定性优化</view>
            </view>
          </view>
          <view class="ver-entry">
            <view class="ver-num">v 2.0.0</view>
            <view class="ver-date">2025 · 11 · 01</view>
            <view class="ver-items">
              <view class="ver-item">全新宣纸视觉语言系统上线</view>
              <view class="ver-item">重构时光轴为瀑布流布局</view>
              <view class="ver-item">加入「封存期」核心功能</view>
            </view>
          </view>
        </view>
      </view>

      <!-- 寄语 -->
      <view class="quote-card">
        <view class="quote-text">时光不会倒流，但可以回序。<br>每一个被封存的瞬间，<br>都在等待被未来的你重新读懂。</view>
        <view class="quote-src">时光回序 制作团队</view>
      </view>

      <!-- 底部版权 -->
      <view style="margin-top: 56rpx; text-align: center;">
        <view style="font-family: var(--sans); font-size: 20rpx; font-weight: 300; color: var(--ink-light); letter-spacing: 0.08em; line-height: 2;">
          © 2025–2026 时光回序<br>以宣纸之名，留岁月之迹
        </view>
      </view>

    </scroll-view>
  </view>
</template>

<style scoped>
.page {
  position: relative;
  min-height: 100vh;
  background: linear-gradient(170deg, #faf7f2 0%, #f5f0e8 55%, #f0ebe0 100%);
  overflow: hidden;
  --ink: #302e29;
  --ink-mid: #6b6560;
  --ink-light: #9e9890;
  --ink-faint: #c8c2b8;
  --paper: #faf7f2;
  --vermilion: #b5352a;
  --serif: 'Noto Serif SC', 'Songti SC', Georgia, serif;
  --sans: 'Noto Sans SC', 'PingFang SC', sans-serif;
}

.page::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='500' height='500'%3E%3Cfilter id='f'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.55' numOctaves='6' stitchTiles='stitch'/%3E%3CfeColorMatrix type='saturate' values='0.15'/%3E%3C/filter%3E%3Crect width='500' height='500' filter='url(%23f)' opacity='0.055'/%3E%3C/svg%3E");
  pointer-events: none;
  z-index: 1;
}

.page::after {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(ellipse 80% 50% at 18% 10%, rgba(200,185,158,0.09) 0%, transparent 70%),
    radial-gradient(ellipse 60% 40% at 82% 25%, rgba(185,168,140,0.06) 0%, transparent 65%),
    radial-gradient(ellipse 50% 35% at 50% 45%, rgba(250,245,238,0.18) 0%, transparent 75%);
  pointer-events: none;
  z-index: 1;
}

.sub-page {
  width: 100%;
  height: 100vh;
  box-sizing: border-box;
  padding: 0 56rpx 80rpx;
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
}

/* ── top nav ── */
.top-nav {
  padding-top: calc(env(safe-area-inset-top) + 96rpx);
  display: flex;
  align-items: center;
  gap: 24rpx;
  margin-bottom: 64rpx;
}
.back-btn {
  display: flex;
  align-items: center;
  gap: 12rpx;
  cursor: pointer;
  opacity: 0.7;
  transition: opacity 0.2s;
}
.back-btn:active {
  opacity: 1;
}
.back-arr {
  width: 28rpx;
  height: 28rpx;
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 14 14' stroke='%236b6560' fill='none' stroke-width='1.5' stroke-linecap='round' stroke-linejoin='round'><polyline points='9,2 4,7 9,12'/></svg>");
  background-size: contain;
  background-repeat: no-repeat;
}
.back-label {
  font-family: var(--serif);
  font-size: 24rpx;
  font-weight: 300;
  color: var(--ink-mid);
  letter-spacing: 0.12em;
}
.page-title {
  font-family: var(--serif);
  font-size: 32rpx;
  font-weight: 400;
  color: var(--ink);
  letter-spacing: 0.18em;
  flex: 1;
  text-align: center;
}
.top-right-space {
  width: 112rpx;
}

/* ── section group ── */
.section {
  margin-bottom: 56rpx;
}
.section-label {
  font-family: var(--sans);
  font-size: 20rpx;
  font-weight: 300;
  color: var(--ink-light);
  letter-spacing: 0.14em;
  margin-bottom: 20rpx;
  padding-left: 4rpx;
}
.group-card {
  background: rgba(252, 249, 244, 0.72);
  border: 1rpx solid rgba(188, 174, 152, 0.28);
  border-radius: 2rpx;
  backdrop-filter: blur(24rpx);
  box-shadow: 0 2rpx 0 rgba(255, 255, 255, 0.6) inset, 0 4rpx 24rpx rgba(140, 120, 90, 0.06);
  overflow: hidden;
  position: relative;
}
.group-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 32rpx;
  bottom: 32rpx;
  width: 3rpx;
  background: linear-gradient(to bottom, transparent, rgba(181, 53, 42, 0.3) 25%, rgba(181, 53, 42, 0.3) 75%, transparent);
  border-radius: 2rpx;
}

/* ── version log ── */
.ver-entry {
  padding: 28rpx 0;
  position: relative;
}
.ver-entry + .ver-entry {
  border-top: 1rpx solid rgba(188, 174, 152, 0.22);
}
.ver-num {
  font-family: var(--serif);
  font-size: 26rpx;
  font-weight: 400;
  color: var(--ink);
  letter-spacing: 0.1em;
  margin-bottom: 12rpx;
}
.ver-date {
  font-family: var(--sans);
  font-size: 20rpx;
  font-weight: 300;
  color: var(--ink-light);
  letter-spacing: 0.06em;
  margin-bottom: 16rpx;
}
.ver-items {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}
.ver-item {
  font-family: var(--serif);
  font-size: 24rpx;
  font-weight: 300;
  color: var(--ink-mid);
  letter-spacing: 0.03em;
  padding-left: 24rpx;
  position: relative;
}
.ver-item::before {
  content: '';
  position: absolute;
  left: 0;
  top: 14rpx;
  width: 8rpx;
  height: 1rpx;
  background: var(--ink-faint);
}

/* ── quote ── */
.quote-card {
  position: relative;
  background: rgba(252, 249, 244, 0.55);
  border: 1rpx solid rgba(188, 174, 152, 0.22);
  border-radius: 2rpx;
  padding: 40rpx 44rpx 40rpx 52rpx;
  margin-top: 16rpx;
}
.quote-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 32rpx;
  bottom: 32rpx;
  width: 3rpx;
  background: linear-gradient(to bottom, transparent, rgba(181, 53, 42, 0.3) 25%, rgba(181, 53, 42, 0.3) 75%, transparent);
  border-radius: 2rpx;
}
.quote-text {
  font-family: var(--serif);
  font-size: 26rpx;
  font-weight: 300;
  color: var(--ink-mid);
  letter-spacing: 0.04em;
  line-height: 1.9;
}
.quote-src {
  font-family: var(--sans);
  font-size: 20rpx;
  font-weight: 300;
  color: var(--ink-light);
  letter-spacing: 0.08em;
  margin-top: 20rpx;
  text-align: right;
}

/* ── deco line ── */
.deco-line {
  height: 1rpx;
  background: var(--ink-faint);
}
</style>
