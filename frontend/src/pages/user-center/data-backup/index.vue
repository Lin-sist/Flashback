<script setup lang="ts">
import { ref } from 'vue'
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
const autoBackup = ref(true)

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
        <view class="page-title">数 据 备 份</view>
        <view class="top-right-space" />
      </view>

      <!-- 备份状态卡片 -->
      <view class="backup-status-card">
        <view class="backup-status-card-bar" />
        <view class="backup-status-card-fold" />
        <view class="backup-status-header">
          <view class="seal"><text>存</text></view>
          <view>
            <view class="backup-status-label">上次备份</view>
            <view class="backup-status-time">2026年5月27日 · 23:18</view>
          </view>
          <view style="margin-left: auto;">
            <view class="badge-ok">
              <view class="badge-dot" />
              <text class="badge-text">已同步</text>
            </view>
          </view>
        </view>
        <view class="deco-line" style="margin: 0 0 24rpx 0; width: 48rpx;" />
        <view class="stat-container">
          <view>
            <view class="stat-unit" style="margin-bottom: 6rpx;">已备份记忆</view>
            <view class="stat-hero" style="font-size: 48rpx;">1,284</view>
          </view>
          <view>
            <view class="stat-unit" style="margin-bottom: 6rpx;">备份大小</view>
            <view class="stat-hero" style="font-size: 48rpx;">42 <text style="font-size: 28rpx;">MB</text></view>
          </view>
        </view>
      </view>

      <view class="section">
        <view class="section-label">自 动 备 份</view>
        <view class="group-card">
          <view class="row">
            <view class="row-icon icon-autobackup" />
            <view class="row-label">自动备份</view>
            <view class="toggle" :class="{ on: autoBackup }" @tap="toggle(autoBackup)"><view class="toggle-dot" /></view>
          </view>
          <view class="row">
            <view class="row-icon icon-time" />
            <view class="row-label">备份频率</view>
            <view class="row-sub">每日</view>
          </view>
          <view class="row">
            <view class="row-icon icon-cloud" />
            <view class="row-label">备份至</view>
            <view class="row-sub">iCloud</view>
          </view>
        </view>
      </view>

      <view class="section">
        <view class="section-label">手 动 操 作</view>
        <view class="group-card">
          <view class="row" style="cursor: pointer;">
            <view class="pulse" />
            <view class="row-label">立即备份</view>
            <view class="icon-arrow" />
          </view>
          <view class="row" style="cursor: pointer;">
            <view class="row-icon icon-export" />
            <view class="row-label">导出为文件</view>
            <view class="row-sub">PDF / 纯文本</view>
          </view>
          <view class="row" style="cursor: pointer;">
            <view class="row-icon icon-import" />
            <view class="row-label">从备份恢复</view>
            <view class="icon-arrow" />
          </view>
        </view>
      </view>

      <view class="section">
        <view class="section-label">危 险 操 作</view>
        <view class="group-card">
          <view class="row" style="cursor: pointer;">
            <view class="row-icon icon-delete" />
            <view class="row-label" style="color: rgba(181, 53, 42, 0.65);">清除全部数据</view>
            <view class="icon-arrow-danger" />
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

/* ── backup status card ── */
.backup-status-card {
  position: relative;
  background: rgba(252, 249, 244, 0.72);
  border: 1rpx solid rgba(188, 174, 152, 0.28);
  border-radius: 2rpx;
  padding: 40rpx 44rpx 40rpx 56rpx;
  margin-bottom: 56rpx;
  box-shadow: 0 2rpx 0 rgba(255, 255, 255, 0.6) inset, 0 4rpx 24rpx rgba(140, 120, 90, 0.06);
}
.backup-status-card-bar {
  position: absolute;
  left: 0;
  top: 32rpx;
  bottom: 32rpx;
  width: 3rpx;
  background: linear-gradient(to bottom, transparent, rgba(181, 53, 42, 0.3) 25%, rgba(181, 53, 42, 0.3) 75%, transparent);
  border-radius: 2rpx;
}
.backup-status-card-fold {
  position: absolute;
  top: 0;
  right: 0;
  width: 20rpx;
  height: 20rpx;
  background: linear-gradient(225deg, rgba(230, 218, 200, 0.9) 0%, rgba(230, 218, 200, 0.9) 48%, rgba(252, 249, 244, 0) 50%);
  border-left: 1rpx solid rgba(188, 174, 152, 0.22);
  border-bottom: 1rpx solid rgba(188, 174, 152, 0.22);
}
.backup-status-header {
  display: flex;
  align-items: center;
  gap: 20rpx;
  margin-bottom: 24rpx;
}
.backup-status-label {
  font-family: var(--sans);
  font-size: 20rpx;
  font-weight: 300;
  color: var(--ink-light);
  letter-spacing: 0.08em;
  margin-bottom: 6rpx;
}
.backup-status-time {
  font-family: var(--serif);
  font-size: 26rpx;
  font-weight: 300;
  color: var(--ink-mid);
  letter-spacing: 0.04em;
}
.stat-container {
  display: flex;
  gap: 48rpx;
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
.icon-autobackup { background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 16 16' stroke='%236b6560' fill='none' stroke-width='1.5' stroke-linecap='round' stroke-linejoin='round'><path d='M13 8a5 5 0 1 1-1-3'/><polyline points='14,2 13,5 10,4'/></svg>"); }
.icon-time { background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 16 16' stroke='%236b6560' fill='none' stroke-width='1.5' stroke-linecap='round' stroke-linejoin='round'><rect x='2' y='3' width='12' height='11' rx='1'/><line x1='2' y1='6' x2='14' y2='6'/><line x1='5' y1='1' x2='5' y2='4'/><line x1='11' y1='1' x2='11' y2='4'/></svg>"); }
.icon-cloud { background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 16 16' stroke='%236b6560' fill='none' stroke-width='1.5' stroke-linecap='round' stroke-linejoin='round'><path d='M13 10v2a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h4'/><polyline points='10,1 15,1 15,6'/><line x1='15' y1='1' x2='9' y2='7'/></svg>"); }
.icon-export { background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 16 16' stroke='%236b6560' fill='none' stroke-width='1.5' stroke-linecap='round' stroke-linejoin='round'><path d='M8 2v8M5 7l3 3 3-3'/><path d='M2 11v1a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2v-1'/></svg>"); }
.icon-import { background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 16 16' stroke='%236b6560' fill='none' stroke-width='1.5' stroke-linecap='round' stroke-linejoin='round'><path d='M8 14V6M5 9l3-3 3 3'/><path d='M2 5V4a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v1'/></svg>"); }
.icon-delete { background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 16 16' stroke='rgba(181,53,42,0.55)' fill='none' stroke-width='1.5' stroke-linecap='round' stroke-linejoin='round'><polyline points='2,4 14,4'/><path d='M5 4V2h6v2'/><rect x='3' y='4' width='10' height='10' rx='1'/></svg>"); }
.icon-arrow { width: 26rpx; height: 26rpx; background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 13 13' stroke='%23c8c2b8' fill='none' stroke-width='1.5' stroke-linecap='round'><polyline points='4,2 9,6.5 4,11'/></svg>"); background-size: contain; background-repeat: no-repeat; background-position: center; flex-shrink: 0; }
.icon-arrow-danger { width: 26rpx; height: 26rpx; background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 13 13' stroke='rgba(181,53,42,0.3)' fill='none' stroke-width='1.5' stroke-linecap='round'><polyline points='4,2 9,6.5 4,11'/></svg>"); background-size: contain; background-repeat: no-repeat; background-position: center; flex-shrink: 0; }

.row-label {
  flex: 1;
  font-family: var(--serif);
  font-size: 28rpx;
  font-weight: 300;
  color: var(--ink-mid);
  letter-spacing: 0.03em;
}
.row-sub {
  font-family: var(--sans);
  font-size: 20rpx;
  font-weight: 300;
  color: var(--ink-light);
  letter-spacing: 0.06em;
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

/* ── seal ── */
.seal {
  width: 52rpx;
  height: 52rpx;
  border-radius: 50%;
  border: 2rpx solid var(--vermilion);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0.72;
  flex-shrink: 0;
}
.seal text {
  font-family: var(--serif);
  font-size: 20rpx;
  color: var(--vermilion);
}

/* ── status badge ── */
.badge-ok {
  display: inline-flex;
  align-items: center;
  gap: 10rpx;
  padding: 6rpx 16rpx;
  border: 1rpx solid rgba(188, 174, 152, 0.35);
  border-radius: 2rpx;
  background: rgba(252, 249, 244, 0.8);
}
.badge-dot {
  width: 8rpx;
  height: 8rpx;
  border-radius: 50%;
  background: rgba(120, 160, 100, 0.7);
}
.badge-text {
  font-family: var(--sans);
  font-size: 20rpx;
  font-weight: 300;
  color: var(--ink-light);
  letter-spacing: 0.06em;
}

/* ── pulse ── */
@keyframes pulse {
  0%, 100% { opacity: 0.3; transform: scale(0.8); }
  50% { opacity: 1; transform: scale(1.2); }
}
.pulse {
  width: 8rpx;
  height: 8rpx;
  border-radius: 50%;
  background: var(--vermilion);
  animation: pulse 2s ease-in-out infinite;
  flex-shrink: 0;
  margin-left: 12rpx;
  margin-right: 12rpx;
}

/* ── deco line ── */
.deco-line {
  height: 1rpx;
  background: var(--ink-faint);
}

/* ── big number ── */
.stat-hero {
  font-family: var(--serif);
  font-weight: 300;
  color: var(--ink-mid);
  letter-spacing: 0.04em;
  margin: 8rpx 0 4rpx;
}
.stat-unit {
  font-family: var(--sans);
  font-size: 20rpx;
  font-weight: 300;
  color: var(--ink-light);
  letter-spacing: 0.08em;
}
</style>
