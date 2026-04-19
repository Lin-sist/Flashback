<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import BottomNavBar from '../../components/common/BottomNavBar.vue'

// 纯静态视觉母版：不做接口调用、不做状态管理、不做真实路由。
// 所有展示数据写死，仅用于走查视觉与排版还原。

const userName = '林清川'
const userSignature = '在静默中整理岁月的碎片'
const savedCount = '1,284'
const archivedDays = '412'
const appVersion = 'v2.4.0'

const noop = () => {
  // 静态母版：点击交互不做任何处理
}

onShow(() => {
  uni.hideTabBar({ animation: false })
})
</script>

<template>
  <view class="page">
    <!-- 顶部品牌栏：居中衬线品牌 + 右侧轻量更多图标 -->
    <view class="top-bar">
      <view class="top-bar-side" />
      <view class="brand">时光回序</view>
      <view class="top-bar-side right">
        <view class="icon icon-more" />
      </view>
    </view>

    <!-- 中央头像区 -->
    <view class="identity">
      <view class="avatar-wrap">
        <image class="avatar" src="/static/avatar.jpg" mode="aspectFill" />
      </view>
      <view class="identity-name">{{ userName }}</view>
      <view class="identity-signature">{{ userSignature }}</view>
    </view>

    <!-- 双统计卡 -->
    <view class="stats-row">
      <view class="stat-card stat-card-cool">
        <view class="stat-label">已存记忆</view>
        <view class="stat-value">{{ savedCount }}</view>
      </view>
      <view class="stat-card stat-card-warm">
        <view class="stat-label">存档天数</view>
        <view class="stat-value">{{ archivedDays }}</view>
      </view>
    </view>

    <!-- 档案设置组 -->
    <view class="group">
      <view class="group-title">档案设置</view>
      <view class="group-card">
        <view class="group-row" @tap="noop">
          <view class="row-icon-wrap">
            <view class="icon icon-book" />
          </view>
          <view class="row-label">整理偏好</view>
          <view class="row-arrow">›</view>
        </view>
        <view class="group-divider" />
        <view class="group-row" @tap="noop">
          <view class="row-icon-wrap">
            <view class="icon icon-gear" />
          </view>
          <view class="row-label">视觉外观</view>
          <view class="row-arrow">›</view>
        </view>
      </view>
    </view>

    <!-- 隐私与安全组 -->
    <view class="group">
      <view class="group-title">隐私与安全</view>
      <view class="group-card">
        <view class="group-row" @tap="noop">
          <view class="row-icon-wrap">
            <view class="icon icon-lock" />
          </view>
          <view class="row-label">访问控制</view>
          <view class="row-arrow">›</view>
        </view>
        <view class="group-divider" />
        <view class="group-row" @tap="noop">
          <view class="row-icon-wrap">
            <view class="icon icon-cloud" />
          </view>
          <view class="row-label">数据备份</view>
          <view class="row-arrow">›</view>
        </view>
      </view>
    </view>

    <!-- 关于组：版本信息独立条目，右侧带版本号标记 -->
    <view class="group">
      <view class="group-title">关于</view>
      <view class="group-card">
        <view class="group-row" @tap="noop">
          <view class="row-icon-wrap">
            <view class="icon icon-info" />
          </view>
          <view class="row-label">版本信息</view>
          <view class="row-version">{{ appVersion }}</view>
        </view>
      </view>
    </view>

    <!-- 弱化的退出文本操作 -->
    <view class="logout" @tap="noop">退出当前账户</view>

    <!-- 保留现有共享底部导航 -->
    <BottomNavBar current="user-center" />
  </view>
</template>

<style scoped>
.page {
  min-height: 100vh;
  padding: 20rpx 40rpx 260rpx;
  background: #f8fafb;
}

/* ---------- 顶部栏 ---------- */
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
  justify-content: flex-start;
}

.top-bar-side.right {
  justify-content: flex-end;
}

.brand {
  flex: 1;
  text-align: center;
  font-size: 34rpx;
  font-weight: 500;
  letter-spacing: 4rpx;
  color: #1a1a1a;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
}

/* ---------- 头像区 ---------- */
.identity {
  margin-top: 40rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.avatar-wrap {
  width: 180rpx;
  height: 180rpx;
  border-radius: 999rpx;
  overflow: hidden;
  background: #1a1a1a;
  box-shadow: 0 10rpx 30rpx rgba(26, 26, 26, 0.15);
}

.avatar {
  width: 100%;
  height: 100%;
  display: block;
}

.identity-name {
  margin-top: 30rpx;
  font-size: 44rpx;
  font-weight: 700;
  color: #1a1a1a;
  letter-spacing: 4rpx;
}

.identity-signature {
  margin-top: 14rpx;
  font-size: 26rpx;
  color: #7f8c93;
  letter-spacing: 1rpx;
}

/* ---------- 统计双卡 ---------- */
.stats-row {
  margin-top: 64rpx;
  display: flex;
  gap: 20rpx;
}

.stat-card {
  flex: 1;
  height: 200rpx;
  padding: 32rpx 32rpx 28rpx;
  border-radius: 32rpx;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.stat-card-cool {
  background: #ffffff;
  box-shadow: 0 8rpx 24rpx rgba(26, 26, 26, 0.04);
}

.stat-card-warm {
  background: #f4e8d2;
}

.stat-label {
  font-size: 26rpx;
  color: #7f8c93;
  letter-spacing: 1rpx;
}

.stat-card-warm .stat-label {
  color: #8a7a55;
}

.stat-value {
  font-size: 76rpx;
  line-height: 1;
  font-weight: 500;
  color: #3b647a;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', 'Times New Roman', serif;
  font-style: italic;
  letter-spacing: 2rpx;
}

.stat-card-warm .stat-value {
  color: #6e5a2c;
  font-style: normal;
}

/* ---------- 分组设置 ---------- */
.group {
  margin-top: 56rpx;
}

.group-title {
  padding: 0 12rpx 20rpx;
  font-size: 26rpx;
  color: #9aa3a9;
  letter-spacing: 2rpx;
}

.group-card {
  border-radius: 32rpx;
  background: #eef2f5;
  padding: 8rpx 0;
  overflow: hidden;
}

.group-row {
  height: 112rpx;
  padding: 0 32rpx;
  display: flex;
  align-items: center;
  gap: 24rpx;
}

.group-divider {
  height: 1rpx;
  margin-left: 92rpx;
  margin-right: 32rpx;
  background: rgba(26, 26, 26, 0.06);
}

.row-icon-wrap {
  width: 48rpx;
  height: 48rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.row-label {
  flex: 1;
  font-size: 30rpx;
  color: #1a1a1a;
  font-weight: 500;
  letter-spacing: 1rpx;
}

.row-arrow {
  font-size: 40rpx;
  color: #9aa3a9;
  line-height: 1;
}

.row-version {
  font-size: 26rpx;
  color: #9aa3a9;
  letter-spacing: 1rpx;
  font-family: 'Menlo', 'Monaco', monospace;
}

/* ---------- 退出文本 ---------- */
.logout {
  margin-top: 72rpx;
  margin-bottom: 40rpx;
  text-align: center;
  color: #c8534a;
  font-size: 28rpx;
  letter-spacing: 2rpx;
}

/* ---------- Icons (SVG data URI, 兼容 mp-weixin) ---------- */
.icon {
  width: 40rpx;
  height: 40rpx;
  background-repeat: no-repeat;
  background-position: center;
  background-size: contain;
}

.icon-more {
  width: 36rpx;
  height: 36rpx;
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%239aa3a9'><circle cx='12' cy='5.5' r='1.6'/><circle cx='12' cy='12' r='1.6'/><circle cx='12' cy='18.5' r='1.6'/></svg>");
}

.icon-book {
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%233b647a' stroke-width='1.6' stroke-linecap='round' stroke-linejoin='round'><path d='M4 4h6a3 3 0 0 1 3 3v13a2 2 0 0 0-2-2H4z'/><path d='M20 4h-6a3 3 0 0 0-3 3v13a2 2 0 0 1 2-2h7z'/></svg>");
}

.icon-gear {
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%233b647a' stroke-width='1.6' stroke-linecap='round' stroke-linejoin='round'><circle cx='12' cy='12' r='3'/><path d='M19.4 15a1.7 1.7 0 0 0 .34 1.87l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.7 1.7 0 0 0-1.87-.34 1.7 1.7 0 0 0-1.03 1.56V21a2 2 0 0 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.56 1.7 1.7 0 0 0-1.87.34l-.06.06A2 2 0 1 1 4.15 16.9l.06-.06a1.7 1.7 0 0 0 .34-1.87 1.7 1.7 0 0 0-1.56-1.03H3a2 2 0 0 1 0-4h.1a1.7 1.7 0 0 0 1.56-1.1 1.7 1.7 0 0 0-.34-1.87l-.06-.06A2 2 0 1 1 7.09 4.15l.06.06a1.7 1.7 0 0 0 1.87.34h.07a1.7 1.7 0 0 0 1.03-1.56V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1.03 1.56 1.7 1.7 0 0 0 1.87-.34l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.7 1.7 0 0 0-.34 1.87v.07A1.7 1.7 0 0 0 20.9 10H21a2 2 0 0 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1z'/></svg>");
}

.icon-lock {
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%233b647a' stroke-width='1.6' stroke-linecap='round' stroke-linejoin='round'><rect x='5' y='11' width='14' height='10' rx='2'/><path d='M8 11V7a4 4 0 0 1 8 0v4'/></svg>");
}

.icon-cloud {
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%233b647a' stroke-width='1.6' stroke-linecap='round' stroke-linejoin='round'><path d='M7 18a4 4 0 1 1 0.6-7.96 6 6 0 0 1 11.3 2.32A3.5 3.5 0 0 1 18 18z'/><polyline points='9.5 13.5 11.5 15.5 15 12'/></svg>");
}

.icon-info {
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%233b647a' stroke-width='1.6' stroke-linecap='round' stroke-linejoin='round'><circle cx='12' cy='12' r='9'/><line x1='12' y1='11' x2='12' y2='16'/><circle cx='12' cy='8' r='0.8' fill='%233b647a' stroke='none'/></svg>");
}
</style>
</content>
</invoke>
