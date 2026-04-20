<script setup lang="ts">
/**
 * 新建 / 写作页 —— 静态高保真视觉母版
 *
 * 目标：1:1 贴近定稿图。该页面不处理真实接口、不处理真实数据保存、不处理真实路由。
 * 唯一保留的交互：右上角关闭按钮触发 uni.navigateBack（最小必要行为）。
 *
 * 视觉锚点：
 *   - 冷灰白背景 + 大圆角冷白纸页（主视觉载体）
 *   - 顶部：极轻卷号 Vol. 16 / 关闭按钮 ✕（避让微信胶囊）
 *   - 纸上：Captured at · 二零二四年·暮秋 · 竖向「私有档案·严禁翻阅」封印
 *   - 纸中：柔和引导文字，大面积留白
 *   - 纸底：MAP / IMAGE / VOICE 三枚轻量圆形入口
 *   - 页面底部：深蓝灰胶囊主按钮「封存这一刻 ›」
 */
import { computed } from 'vue'
import { useWechatNavMetrics } from '../../composables/useWechatNavMetrics'

const { cssVars, navBarTotalHeight, rightSafeWidth } = useWechatNavMetrics()

const pageStyle = computed(() => ({
  ...cssVars.value,
  paddingTop: `${navBarTotalHeight.value}px`,
}))

const topBarStyle = computed(() => ({
  // 右侧预留给微信胶囊的安全宽度
  paddingRight: `${rightSafeWidth.value}px`,
}))

const handleClose = () => {
  // 静态母版：仅返回上一页，不触发任何保存或状态变更
  uni.navigateBack({
    delta: 1,
    fail: () => {
      // 如果没有上一页，静默处理，不跳路由
    },
  })
}
</script>

<template>
  <view class="page" :style="pageStyle">
    <!-- 背景纸雾光晕 -->
    <view class="page-bg" aria-hidden="true" />

    <!-- 顶部沉浸式轻顶栏 -->
    <view class="top-bar" :style="topBarStyle">
      <view class="vol">
        <text class="vol-text">Vol. 16</text>
        <view class="vol-rule" />
      </view>
      <view class="close-hit" @tap="handleClose">
        <view class="close-icon">
          <view class="close-line close-line-a" />
          <view class="close-line close-line-b" />
        </view>
      </view>
    </view>

    <!-- 主纸页 -->
    <view class="paper">
      <view class="paper-inner">
        <!-- 纸页上部：日期 + 封印 -->
        <view class="paper-head">
          <view class="head-left">
            <text class="captured-at">Captured at</text>
            <text class="date-title">二零二四年·暮秋</text>
          </view>

          <view class="seal">
            <text class="seal-text">私有档案·严禁翻阅</text>
          </view>
        </view>

        <!-- 纸页正文引导 -->
        <view class="prose">
          <text class="prose-text">在此刻的宁静中，留下你的记忆碎片...</text>
        </view>

        <!-- 留白纸面：不放任何工程化 input / placeholder -->
        <view class="paper-blank" aria-hidden="true" />

        <!-- 纸页底部辅助入口 -->
        <view class="aux-divider" aria-hidden="true" />
        <view class="aux-row">
          <view class="aux-item">
            <view class="aux-circle">
              <view class="ic ic-map" />
            </view>
            <text class="aux-label">MAP</text>
          </view>
          <view class="aux-item">
            <view class="aux-circle">
              <view class="ic ic-image" />
            </view>
            <text class="aux-label">IMAGE</text>
          </view>
          <view class="aux-item">
            <view class="aux-circle">
              <view class="ic ic-voice" />
            </view>
            <text class="aux-label">VOICE</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 页面底部主操作区 -->
    <view class="action-area">
      <view class="seal-btn">
        <text class="seal-btn-text">封存这一刻</text>
        <view class="seal-btn-rule" />
        <text class="seal-btn-arrow">›</text>
      </view>
    </view>
  </view>
</template>

<style scoped>
/* ============ 页面基座 ============ */
.page {
  position: relative;
  min-height: 100vh;
  padding-left: 40rpx;
  padding-right: 40rpx;
  padding-bottom: calc(env(safe-area-inset-bottom) + 40rpx);
  background: #eef1f3;
  font-family: 'PingFang SC', 'Noto Sans SC', 'Microsoft YaHei', sans-serif;
}

.page-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(120% 70% at 50% 0%, rgba(255, 255, 255, 0.75) 0%, rgba(255, 255, 255, 0) 50%),
    radial-gradient(90% 50% at 50% 100%, rgba(214, 222, 227, 0.5) 0%, rgba(214, 222, 227, 0) 55%);
}

/* ============ 顶部轻顶栏 ============ */
.top-bar {
  position: relative;
  z-index: 2;
  height: 88rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-left: 8rpx;
}

.vol {
  display: flex;
  align-items: center;
  gap: 18rpx;
}

.vol-text {
  font-size: 24rpx;
  font-style: italic;
  letter-spacing: 1rpx;
  color: #8a949a;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', 'Times New Roman', serif;
}

.vol-rule {
  width: 56rpx;
  height: 1rpx;
  background: #c9d1d6;
}

.close-hit {
  width: 72rpx;
  height: 72rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.close-icon {
  position: relative;
  width: 32rpx;
  height: 32rpx;
}

.close-line {
  position: absolute;
  top: 50%;
  left: 0;
  width: 100%;
  height: 2rpx;
  background: #5a646a;
  border-radius: 2rpx;
}

.close-line-a {
  transform: translateY(-50%) rotate(45deg);
}

.close-line-b {
  transform: translateY(-50%) rotate(-45deg);
}

/* ============ 主纸页 ============ */
.paper {
  position: relative;
  z-index: 1;
  margin-top: 24rpx;
  border-radius: 40rpx;
  background: #fdfefe;
  box-shadow:
    0 2rpx 0 rgba(255, 255, 255, 0.9) inset,
    0 24rpx 56rpx rgba(60, 78, 92, 0.08),
    0 6rpx 14rpx rgba(60, 78, 92, 0.04);
}

.paper-inner {
  padding: 56rpx 48rpx 44rpx;
}

/* ---- 纸页上部：日期 + 封印 ---- */
.paper-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24rpx;
}

.head-left {
  flex: 1;
  min-width: 0;
  padding-top: 4rpx;
}

.captured-at {
  display: block;
  font-size: 24rpx;
  line-height: 1;
  letter-spacing: 1rpx;
  font-style: italic;
  color: #8a949a;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', 'Times New Roman', serif;
}

.date-title {
  display: block;
  margin-top: 22rpx;
  font-size: 54rpx;
  line-height: 1.2;
  letter-spacing: 2rpx;
  color: #1a1a1a;
  font-weight: 500;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
}

.seal {
  flex-shrink: 0;
  min-height: 200rpx;
  margin-top: 4rpx;
  padding: 18rpx 14rpx;
  border: 1rpx solid #d9c79a;
  border-radius: 6rpx;
  background: linear-gradient(180deg, #fbf4df 0%, #f6ead1 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.seal-text {
  writing-mode: vertical-rl;
  -webkit-writing-mode: vertical-rl;
  font-size: 22rpx;
  letter-spacing: 6rpx;
  color: #a08a4b;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
}

/* ---- 纸页正文引导 ---- */
.prose {
  margin-top: 72rpx;
}

.prose-text {
  display: block;
  font-size: 30rpx;
  line-height: 1.6;
  color: #7f8c93;
  letter-spacing: 1rpx;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', 'PingFang SC', serif;
}

/* ---- 留白纸面 ---- */
.paper-blank {
  height: 480rpx;
}

/* ---- 纸底辅助入口 ---- */
.aux-divider {
  height: 1rpx;
  background: linear-gradient(
    90deg,
    rgba(200, 208, 213, 0) 0%,
    rgba(200, 208, 213, 0.6) 20%,
    rgba(200, 208, 213, 0.6) 80%,
    rgba(200, 208, 213, 0) 100%
  );
  margin: 0 8rpx 32rpx;
}

.aux-row {
  display: flex;
  justify-content: space-around;
  align-items: flex-start;
  padding: 0 16rpx;
}

.aux-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14rpx;
}

.aux-circle {
  width: 72rpx;
  height: 72rpx;
  border-radius: 999rpx;
  border: 1rpx solid #cfd6da;
  background: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.aux-label {
  font-size: 20rpx;
  letter-spacing: 4rpx;
  color: #8a949a;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', 'Times New Roman', serif;
}

/* ---- 圆形入口里的细线图标（1.2rpx 描边感） ---- */
.ic {
  width: 32rpx;
  height: 32rpx;
  background-repeat: no-repeat;
  background-position: center;
  background-size: contain;
}

.ic-map {
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%235a6870' stroke-width='1.4' stroke-linecap='round' stroke-linejoin='round'><path d='M12 21s-7-7.5-7-12a7 7 0 1 1 14 0c0 4.5-7 12-7 12z'/><circle cx='12' cy='9' r='2.4'/></svg>");
}

.ic-image {
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%235a6870' stroke-width='1.4' stroke-linecap='round' stroke-linejoin='round'><path d='M4 8.5h3.2l1.6-2.2h6.4l1.6 2.2H20a1 1 0 0 1 1 1v8.3a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V9.5a1 1 0 0 1 1-1z'/><circle cx='12' cy='13.6' r='3.4'/></svg>");
}

.ic-voice {
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%235a6870' stroke-width='1.4' stroke-linecap='round' stroke-linejoin='round'><rect x='9' y='3.5' width='6' height='11' rx='3'/><path d='M6 12a6 6 0 0 0 12 0'/><line x1='12' y1='18' x2='12' y2='21'/><line x1='9' y1='21' x2='15' y2='21'/></svg>");
}

/* ============ 底部主操作 ============ */
.action-area {
  position: relative;
  z-index: 1;
  margin-top: 60rpx;
  display: flex;
  justify-content: center;
}

.seal-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 460rpx;
  height: 100rpx;
  padding: 0 56rpx;
  border-radius: 999rpx;
  background: #2e5062;
  box-shadow:
    0 16rpx 32rpx rgba(46, 80, 98, 0.28),
    0 2rpx 0 rgba(255, 255, 255, 0.1) inset;
}

.seal-btn-text {
  font-size: 30rpx;
  letter-spacing: 8rpx;
  color: #ffffff;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
}

.seal-btn-rule {
  width: 1rpx;
  height: 28rpx;
  margin: 0 28rpx;
  background: rgba(255, 255, 255, 0.35);
}

.seal-btn-arrow {
  font-size: 32rpx;
  line-height: 1;
  color: #ffffff;
  margin-top: -4rpx;
  font-family: 'Songti SC', 'STSong', 'Times New Roman', serif;
}
</style>
