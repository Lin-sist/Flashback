<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { ref } from 'vue'
import EmptyState from '../../components/common/EmptyState.vue'
import RecordCard from '../../components/card/RecordCard.vue'
import { recordService } from '../../services'
import type { RecordListItemVO } from '../../types'
import { getToken } from '../../utils'

const list = ref<RecordListItemVO[]>([])
const loading = ref(false)

const ensureLogin = () => {
  if (!getToken()) {
    uni.reLaunch({ url: '/pages/login/index' })
    return false
  }
  return true
}

const loadLatestUnlocked = async () => {
  if (!ensureLogin()) {
    return
  }
  loading.value = true
  try {
    const result = await recordService.getUnlockedRecords(1, 5)
    list.value = result.list
  } catch {
    list.value = []
  } finally {
    loading.value = false
  }
}

onShow(() => {
  loadLatestUnlocked()
})

const goEditor = () => {
  uni.navigateTo({ url: '/pages/record-editor/index' })
}
</script>

<template>
  <view class="page">
    <view class="hero">
      <view>
        <view class="hero-title">Welcome back</view>
        <view class="hero-subtitle">Capture now, unlock later.</view>
      </view>
      <button class="new-btn" @tap="goEditor">New Record</button>
    </view>

    <view class="section-title">Recently Unlocked</view>
    <view v-if="loading" class="loading">Loading...</view>
    <view v-else-if="list.length === 0">
      <EmptyState text="No unlocked records yet" />
    </view>
    <view v-else class="list">
      <RecordCard v-for="item in list" :key="item.id" :item="item" />
    </view>
  </view>
</template>

<style scoped>
.page {
  min-height: 100vh;
  padding: 28rpx;
}

.hero {
  background: linear-gradient(120deg, #d6f3ff 0%, #eef9f5 100%);
  border-radius: 24rpx;
  padding: 28rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.hero-title {
  font-size: 34rpx;
  font-weight: 700;
}

.hero-subtitle {
  margin-top: 8rpx;
  color: #475467;
  font-size: 24rpx;
}

.new-btn {
  border-radius: 9999rpx;
  background: #0ea5e9;
  color: #ffffff;
  padding: 0 28rpx;
  font-size: 24rpx;
}

.section-title {
  margin: 28rpx 0 18rpx;
  font-size: 30rpx;
  font-weight: 600;
}

.loading {
  color: #667085;
}

.list {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
}
</style>
