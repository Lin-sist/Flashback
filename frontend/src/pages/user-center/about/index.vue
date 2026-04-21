<script setup lang="ts">
import { onLoad } from '@dcloudio/uni-app'
import AppTopBar from '../../../components/common/AppTopBar.vue'
import PaperContainer from '../../../components/common/PaperContainer.vue'
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
    <AppTopBar title="关于时光回序" show-back @back="goBack" />

    <PaperContainer radius="xl" class="section">
      <view class="app-name">Flashback · 时光回序</view>
      <view class="meta">版本：MVP 0.1.0（占位）</view>
      <view class="meta">定位：写给未来自己的时间档案</view>
    </PaperContainer>

    <PaperContainer radius="xl" class="section">
      <view class="title">项目说明</view>
      <view class="desc">
        本应用用于沉淀个人经历，支持书写、封存、到时解锁与回信，形成完整的时间记录链路。
      </view>
      <view class="desc">
        当前版本聚焦 MVP 主线：首页 / 编辑 / 封存 / 档案 / 时间轴 / 解锁 / 回信，设置页以基础能力为主。
      </view>
    </PaperContainer>

    <PaperContainer radius="xl" class="section">
      <view class="title">后续规划（占位）</view>
      <view class="line">- 账号级设置同步</view>
      <view class="line">- 标签管理能力完善</view>
      <view class="line">- 提醒渠道扩展（站内 / 邮件）</view>
    </PaperContainer>
  </view>
</template>

<style scoped>
.page {
  min-height: 100vh;
  padding: 20rpx 24rpx 40rpx;
  background: #f8fafb;
}

.section {
  margin-top: 18rpx;
}

.app-name {
  font-size: 42rpx;
  color: #1a1a1a;
  font-weight: 600;
}

.meta {
  margin-top: 10rpx;
  font-size: 28rpx;
  color: #7f8c93;
}

.title {
  font-size: 36rpx;
  color: #1a1a1a;
  font-weight: 600;
}

.desc {
  margin-top: 10rpx;
  font-size: 28rpx;
  color: #7f8c93;
  line-height: 1.75;
}

.line {
  margin-top: 10rpx;
  font-size: 28rpx;
  color: #7f8c93;
}
</style>

