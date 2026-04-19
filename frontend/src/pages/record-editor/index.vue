<script setup lang="ts">
// 纯静态视觉母版，无任何接口 / store / 路由参数逻辑
// 所有内容均为写死示例，用于还原设计稿 _03-新建.png

const closePage = () => {
  uni.navigateBack({
    delta: 1,
    fail: () => {
      uni.switchTab({ url: '/pages/home/index' })
    },
  })
}
</script>

<template>
  <view class="page">
    <!-- 顶部: 左侧斜体卷册号 + 右侧关闭 -->
    <view class="topbar">
      <view class="vol-tag">
        <text class="vol-text">Vol. 16</text>
        <text class="vol-dash">——</text>
      </view>
      <view class="close" @tap="closePage">
        <text class="close-icon">×</text>
      </view>
    </view>

    <!-- 主信笺卡 -->
    <view class="paper">
      <view class="paper-inner">
        <!-- 左侧标题区 -->
        <view class="title-block">
          <text class="captured-at">Captured at</text>
          <view class="date-line">二零二四年·暮秋</view>
        </view>

        <!-- 右上竖向烫金封条 -->
        <view class="seal">
          <text class="seal-text">私有档案 · 严禁翻阅</text>
        </view>
      </view>

      <!-- 留白写信区 -->
      <view class="writing-area">
        <text class="placeholder">
          在此刻的宁静中，留下你的记忆碎片...
        </text>
      </view>

      <!-- 底部三个线性图标 -->
      <view class="media-row">
        <view class="media-item">
          <view class="media-circle">
            <view class="icon icon-map"></view>
          </view>
          <text class="media-label">MAP</text>
        </view>
        <view class="media-item">
          <view class="media-circle">
            <view class="icon icon-image"></view>
          </view>
          <text class="media-label">IMAGE</text>
        </view>
        <view class="media-item">
          <view class="media-circle">
            <view class="icon icon-voice"></view>
          </view>
          <text class="media-label">VOICE</text>
        </view>
      </view>
    </view>

    <!-- 底部主操作胶囊 -->
    <view class="seal-action">
      <view class="seal-pill">
        <text class="seal-pill-text">封存这一刻</text>
        <view class="seal-pill-divider"></view>
        <text class="seal-pill-arrow">›</text>
      </view>
    </view>
  </view>
</template>

<style scoped>
.page {
  min-height: 100vh;
  /* 顶部安全区: 系统状态栏 + 微信胶囊高度(约 88rpx) + 呼吸留白 */
  padding:
    calc(env(safe-area-inset-top, 44rpx) + 120rpx)
    40rpx
    80rpx;
  background: #eef2f4;
  position: relative;
}

/* 背景轻雾感 */
.page::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse at 20% 0%, rgba(255, 255, 255, 0.9) 0%, transparent 55%),
    radial-gradient(ellipse at 100% 100%, rgba(214, 224, 230, 0.4) 0%, transparent 60%);
  pointer-events: none;
}

/* 顶栏: 左 Vol. 右 X, X 必须留在微信胶囊按钮左侧并保持安全距离 */
.topbar {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  /* 右侧多预留 200rpx, 避开微信右上角胶囊按钮 (...圆) */
  padding: 0 200rpx 56rpx 12rpx;
}

.vol-tag {
  display: flex;
  align-items: center;
  gap: 18rpx;
}

.vol-text {
  font-family: Georgia, 'Times New Roman', serif;
  font-style: italic;
  font-size: 28rpx;
  color: #8a9096;
  letter-spacing: 1rpx;
}

.vol-dash {
  color: #b9c1c7;
  font-size: 24rpx;
  letter-spacing: 2rpx;
}

.close {
  width: 56rpx;
  height: 56rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.close-icon {
  font-size: 44rpx;
  color: #6b7278;
  font-weight: 200;
  line-height: 1;
}

/* 主卡 */
.paper {
  position: relative;
  background: #fcfdfd;
  border-radius: 48rpx;
  padding: 56rpx 48rpx 48rpx;
  box-shadow:
    0 2rpx 1rpx rgba(30, 40, 48, 0.02),
    0 24rpx 60rpx rgba(60, 75, 90, 0.08),
    0 60rpx 120rpx rgba(60, 75, 90, 0.04);
  min-height: 1100rpx;
  display: flex;
  flex-direction: column;
}

.paper-inner {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 32rpx;
}

/* 日期标题 */
.title-block {
  flex: 1;
  padding-top: 8rpx;
}

.captured-at {
  font-family: Georgia, 'Times New Roman', serif;
  font-style: italic;
  font-size: 28rpx;
  color: #6a95a8;
  letter-spacing: 1rpx;
}

.date-line {
  margin-top: 16rpx;
  font-family: 'Source Han Serif', 'Noto Serif SC', 'Songti SC', 'STSong', serif;
  font-size: 64rpx;
  font-weight: 600;
  color: #1c2a31;
  letter-spacing: 2rpx;
  line-height: 1.3;
}

/* 竖向封条 */
.seal {
  width: 72rpx;
  min-height: 300rpx;
  border: 1rpx solid #c9a24a;
  border-radius: 4rpx;
  padding: 20rpx 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(253, 247, 233, 0.4);
}

.seal-text {
  writing-mode: vertical-rl;
  -webkit-writing-mode: vertical-rl;
  font-family: 'Source Han Serif', 'Noto Serif SC', 'Songti SC', serif;
  font-size: 22rpx;
  color: #b5883a;
  letter-spacing: 6rpx;
  line-height: 1.8;
}

/* 写信空白区 */
.writing-area {
  flex: 1;
  margin-top: 40rpx;
  padding: 32rpx 0 60rpx;
  min-height: 480rpx;
}

.placeholder {
  font-size: 32rpx;
  color: #b7c0c6;
  line-height: 1.9;
  letter-spacing: 1rpx;
}

/* 媒体图标行 */
.media-row {
  margin-top: auto;
  padding-top: 32rpx;
  border-top: 1rpx solid rgba(180, 190, 198, 0.25);
  display: flex;
  justify-content: space-around;
  align-items: center;
  padding-bottom: 8rpx;
}

.media-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16rpx;
}

.media-circle {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  border: 1.5rpx solid #c3ccd2;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
}

.icon {
  width: 28rpx;
  height: 28rpx;
  background-repeat: no-repeat;
  background-position: center;
  background-size: contain;
}

.icon-map {
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%239aa3a9' stroke-width='1.6' stroke-linecap='round' stroke-linejoin='round'><path d='M12 21s-7-6.5-7-12a7 7 0 0 1 14 0c0 5.5-7 12-7 12z'/><circle cx='12' cy='9' r='2.5'/></svg>");
}

.icon-image {
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%239aa3a9' stroke-width='1.6' stroke-linecap='round' stroke-linejoin='round'><rect x='3' y='5' width='18' height='14' rx='2'/><circle cx='9' cy='10' r='1.6'/><path d='M21 16l-5-5-8 8'/></svg>");
}

.icon-voice {
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%239aa3a9' stroke-width='1.6' stroke-linecap='round' stroke-linejoin='round'><rect x='9' y='3' width='6' height='12' rx='3'/><path d='M5 11a7 7 0 0 0 14 0'/><path d='M12 18v3'/></svg>");
}

.media-label {
  font-family: Georgia, 'Times New Roman', serif;
  font-size: 22rpx;
  color: #9aa3a9;
  letter-spacing: 4rpx;
}

/* 主操作胶囊 */
.seal-action {
  margin-top: 80rpx;
  display: flex;
  justify-content: center;
}

.seal-pill {
  display: flex;
  align-items: center;
  gap: 28rpx;
  padding: 28rpx 56rpx;
  background: #335367;
  border-radius: 999rpx;
  box-shadow: 0 18rpx 36rpx rgba(51, 83, 103, 0.3);
}

.seal-pill-text {
  font-size: 32rpx;
  color: #f5f8fa;
  letter-spacing: 6rpx;
  font-weight: 500;
}

.seal-pill-divider {
  width: 1rpx;
  height: 28rpx;
  background: rgba(245, 248, 250, 0.35);
}

.seal-pill-arrow {
  font-size: 36rpx;
  color: #f5f8fa;
  line-height: 1;
  font-weight: 300;
}
</style>
</content>
</invoke>
