<script setup lang="ts">
import { ref, computed } from 'vue'
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

// Local demo state
const theme = ref(0) // 0: 暖宣, 1: 素灰, 2: 冷青, 3: 夜墨
const fontSize = ref(1) // 0: 小, 1: 标准, 2: 大, 3: 特大
const lhPct = ref(50) // 0, 50, 100
const showFold = ref(true)
const showAnim = ref(true)
const showTexture = ref(true)

const lhLabel = computed(() => {
  if (lhPct.value < 33) return '紧凑（1.5）'
  if (lhPct.value < 66) return '舒适（1.85）'
  return '宽松（2.2）'
})

const handleSliderTap = () => {
  lhPct.value = lhPct.value === 100 ? 0 : lhPct.value + 50
}

const toggle = (refVar: any) => { refVar.value = !refVar.value }

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
        <view class="page-title">视 觉 外 观</view>
        <view class="top-right-space" />
      </view>

      <view class="section">
        <view class="section-label">纸 色 基 调</view>
        <view class="group-card">
          <view class="theme-row">
            <view class="swatch" :class="{ sel: theme === 0 }" @tap="theme = 0">
              <view class="swatch-bg" style="background: linear-gradient(145deg, #faf7f2, #f0ebe0);">
                <view class="swatch-dot" />
              </view>
              <view class="swatch-name">暖 宣</view>
            </view>
            <view class="swatch" :class="{ sel: theme === 1 }" @tap="theme = 1">
              <view class="swatch-bg" style="background: linear-gradient(145deg, #f6f5f0, #e8e5dc);">
                <view class="swatch-dot" />
              </view>
              <view class="swatch-name">素 灰</view>
            </view>
            <view class="swatch" :class="{ sel: theme === 2 }" @tap="theme = 2">
              <view class="swatch-bg" style="background: linear-gradient(145deg, #f2f6f8, #e0eaee);">
                <view class="swatch-dot" />
              </view>
              <view class="swatch-name">冷 青</view>
            </view>
            <view class="swatch" :class="{ sel: theme === 3 }" @tap="theme = 3">
              <view class="swatch-bg" style="background: linear-gradient(145deg, #1e1c18, #2a2720);">
                <view class="swatch-dot" style="background: #c8c2b8;" />
              </view>
              <view class="swatch-name">夜 墨</view>
            </view>
          </view>
        </view>
      </view>

      <view class="section">
        <view class="section-label">字 号 大 小</view>
        <view class="group-card">
          <view class="fontsize-row">
            <view class="fs-opt" :class="{ sel: fontSize === 0 }" @tap="fontSize = 0">
              <view class="fs-preview" style="font-size: 28rpx;">文</view>
              <view class="fs-label">小</view>
            </view>
            <view class="fs-sep" />
            <view class="fs-opt" :class="{ sel: fontSize === 1 }" @tap="fontSize = 1">
              <view class="fs-preview" style="font-size: 36rpx;">文</view>
              <view class="fs-label">标准</view>
            </view>
            <view class="fs-sep" />
            <view class="fs-opt" :class="{ sel: fontSize === 2 }" @tap="fontSize = 2">
              <view class="fs-preview" style="font-size: 44rpx;">文</view>
              <view class="fs-label">大</view>
            </view>
            <view class="fs-sep" />
            <view class="fs-opt" :class="{ sel: fontSize === 3 }" @tap="fontSize = 3">
              <view class="fs-preview" style="font-size: 52rpx;">文</view>
              <view class="fs-label">特大</view>
            </view>
          </view>
        </view>
      </view>

      <view class="section">
        <view class="section-label">行 距 疏 密</view>
        <view class="group-card">
          <view class="slider-wrap">
            <view class="slider-header">
              <view class="slider-label">行间距</view>
              <view class="slider-val">{{ lhLabel }}</view>
            </view>
            <view class="slider-track" style="margin-top: 32rpx;" @tap="handleSliderTap">
              <view class="slider-fill" :style="{ width: lhPct + '%' }" />
              <view class="slider-thumb" :style="{ left: lhPct + '%' }" />
            </view>
            <view class="slider-ticks" style="margin-top: 28rpx;">
              <text class="tick-label">紧凑</text>
              <text class="tick-label">舒适</text>
              <text class="tick-label">宽松</text>
            </view>
          </view>
        </view>
      </view>

      <view class="section">
        <view class="section-label">其 他 显 示</view>
        <view class="group-card">
          <view class="row">
            <view class="row-icon icon-star" />
            <view class="row-label">卡片折角装饰</view>
            <view class="toggle" :class="{ on: showFold }" @tap="toggle(showFold)"><view class="toggle-dot" /></view>
          </view>
          <view class="row">
            <view class="row-icon icon-anim" />
            <view class="row-label">入场动画</view>
            <view class="toggle" :class="{ on: showAnim }" @tap="toggle(showAnim)"><view class="toggle-dot" /></view>
          </view>
          <view class="row">
            <view class="row-icon icon-texture" />
            <view class="row-label">纸面颗粒质感</view>
            <view class="toggle" :class="{ on: showTexture }" @tap="toggle(showTexture)"><view class="toggle-dot" /></view>
          </view>
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

/* ── row item ── */
.row {
  display: flex;
  align-items: center;
  gap: 24rpx;
  padding: 28rpx 36rpx;
  position: relative;
}
.row + .row::before {
  content: '';
  position: absolute;
  top: 0;
  left: 92rpx;
  right: 36rpx;
  height: 1rpx;
  background: rgba(188, 174, 152, 0.22);
}
.row-icon {
  width: 32rpx;
  height: 32rpx;
  flex-shrink: 0;
  background-size: contain;
  background-repeat: no-repeat;
  background-position: center;
}
.icon-star { background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 16 16' stroke='%236b6560' fill='none' stroke-width='1.5' stroke-linecap='round' stroke-linejoin='round'><path d='M8 1L10 6H15L11 9.5L12.5 15L8 12L3.5 15L5 9.5L1 6H6Z'/></svg>"); }
.icon-anim { background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 16 16' stroke='%236b6560' fill='none' stroke-width='1.5' stroke-linecap='round' stroke-linejoin='round'><circle cx='8' cy='8' r='5.5'/><line x1='8' y1='5' x2='8' y2='8.5'/><circle cx='8' cy='11' r='0.7' fill='%236b6560' stroke='none'/></svg>"); }
.icon-texture { background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 16 16' stroke='%236b6560' fill='none' stroke-width='1.5' stroke-linecap='round' stroke-linejoin='round'><path d='M2 12 Q8 2 14 12'/><path d='M4 10 Q8 5 12 10'/></svg>"); }

.row-label {
  flex: 1;
  font-family: var(--serif);
  font-size: 28rpx;
  font-weight: 300;
  color: var(--ink-mid);
  letter-spacing: 0.03em;
}

/* ── toggle ── */
.toggle {
  width: 80rpx;
  height: 44rpx;
  background: rgba(188, 174, 152, 0.35);
  border-radius: 22rpx;
  position: relative;
  cursor: pointer;
  transition: background 0.3s;
  flex-shrink: 0;
  border: 1rpx solid rgba(188, 174, 152, 0.4);
}
.toggle.on {
  background: rgba(181, 53, 42, 0.22);
  border-color: rgba(181, 53, 42, 0.3);
}
.toggle-dot {
  position: absolute;
  top: 6rpx;
  left: 6rpx;
  width: 28rpx;
  height: 28rpx;
  background: var(--paper);
  border-radius: 50%;
  box-shadow: 0 2rpx 6rpx rgba(0, 0, 0, 0.12);
  transition: transform 0.3s;
  border: 1rpx solid rgba(188, 174, 152, 0.4);
}
.toggle.on .toggle-dot {
  transform: translateX(36rpx);
  border-color: rgba(181, 53, 42, 0.2);
}

/* ── theme swatch ── */
.theme-row {
  display: flex;
  gap: 24rpx;
  padding: 32rpx 36rpx;
}
.swatch {
  flex: 1;
  border-radius: 2rpx;
  border: 1rpx solid rgba(188, 174, 152, 0.28);
  overflow: hidden;
  cursor: pointer;
  position: relative;
  transition: all 0.2s;
}
.swatch.sel {
  border: 2rpx solid rgba(181, 53, 42, 0.45);
  box-shadow: 0 0 0 4rpx rgba(181, 53, 42, 0.08);
}
.swatch-bg {
  height: 96rpx;
  display: flex;
  align-items: flex-end;
  padding: 12rpx 16rpx;
}
.swatch-dot {
  width: 8rpx;
  height: 8rpx;
  border-radius: 50%;
  background: var(--vermilion);
  opacity: 0;
  position: absolute;
  bottom: 56rpx;
  left: 50%;
  transform: translateX(-50%);
}
.swatch.sel .swatch-dot {
  opacity: 0.75;
}
.swatch-name {
  padding: 12rpx 16rpx 16rpx;
  font-family: var(--sans);
  font-size: 18rpx;
  font-weight: 300;
  color: var(--ink-light);
  letter-spacing: 0.08em;
  text-align: center;
  background: rgba(252, 249, 244, 0.72);
}

/* ── font size preview ── */
.fontsize-row {
  display: flex;
  align-items: center;
  gap: 32rpx;
  padding: 32rpx 36rpx;
}
.fs-opt {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12rpx;
  cursor: pointer;
  padding: 20rpx 12rpx;
  border-radius: 2rpx;
  border: 1rpx solid transparent;
  transition: all 0.2s;
}
.fs-opt.sel {
  border-color: rgba(181, 53, 42, 0.35);
  background: rgba(181, 53, 42, 0.03);
}
.fs-preview {
  font-family: var(--serif);
  color: var(--ink-mid);
  font-weight: 300;
  line-height: 1;
}
.fs-label {
  font-family: var(--sans);
  font-size: 18rpx;
  font-weight: 300;
  color: var(--ink-light);
  letter-spacing: 0.06em;
}
.fs-sep {
  width: 1rpx;
  height: 80rpx;
  background: rgba(188, 174, 152, 0.22);
}

/* ── slider ── */
.slider-wrap {
  padding: 28rpx 36rpx 36rpx;
}
.slider-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 24rpx;
}
.slider-label {
  font-family: var(--serif);
  font-size: 28rpx;
  font-weight: 300;
  color: var(--ink-mid);
  letter-spacing: 0.03em;
}
.slider-val {
  font-family: var(--sans);
  font-size: 20rpx;
  font-weight: 300;
  color: var(--ink-light);
  letter-spacing: 0.06em;
}
.slider-track {
  height: 2rpx;
  background: var(--ink-faint);
  border-radius: 2rpx;
  position: relative;
  margin: 0 8rpx;
  padding: 20rpx 0; /* increase hit area */
}
.slider-fill {
  height: 2rpx;
  background: linear-gradient(to right, transparent, rgba(181, 53, 42, 0.5));
  border-radius: 2rpx;
  position: absolute;
  left: 0;
  top: 20rpx;
}
.slider-thumb {
  width: 36rpx;
  height: 36rpx;
  border-radius: 50%;
  background: var(--paper);
  border: 1rpx solid rgba(188, 174, 152, 0.6);
  box-shadow: 0 2rpx 8rpx rgba(140, 120, 90, 0.14);
  position: absolute;
  top: 21rpx;
  transform: translate(-50%, -50%);
  cursor: pointer;
}
.slider-ticks {
  display: flex;
  justify-content: space-between;
  margin-top: 20rpx;
  padding: 0 8rpx;
}
.tick-label {
  font-family: var(--sans);
  font-size: 18rpx;
  font-weight: 300;
  color: var(--ink-light);
  letter-spacing: 0.04em;
}
</style>
