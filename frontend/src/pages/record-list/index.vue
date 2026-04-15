<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { ref } from 'vue'
import EmptyState from '../../components/common/EmptyState.vue'
import RecordCard from '../../components/card/RecordCard.vue'
import { useRecordStore } from '../../stores'
import { RecordStatus } from '../../types'
import { getToken, toUserMessage } from '../../utils'

const recordStore = useRecordStore()
const status = ref<RecordStatus | 'ALL'>('ALL')

const ensureLogin = () => {
  if (!getToken()) {
    uni.reLaunch({ url: '/pages/login/index' })
    return false
  }
  return true
}

const loadList = async () => {
  if (!ensureLogin()) {
    return
  }
  try {
    await recordStore.fetchList(status.value)
  } catch (error) {
    uni.showToast({ title: toUserMessage(error), icon: 'none' })
  }
}

onShow(() => {
  loadList()
})

const setStatus = (value: RecordStatus | 'ALL') => {
  status.value = value
  loadList()
}

const openDetail = (id: string) => {
  uni.navigateTo({ url: `/pages/record-detail/index?id=${id}` })
}
</script>

<template>
  <view class="page">
    <scroll-view scroll-x class="filters">
      <view class="chips">
        <view class="chip" :class="{ active: status === 'ALL' }" @tap="setStatus('ALL')">All</view>
        <view class="chip" :class="{ active: status === RecordStatus.DRAFT }" @tap="setStatus(RecordStatus.DRAFT)">Draft</view>
        <view class="chip" :class="{ active: status === RecordStatus.SEALED }" @tap="setStatus(RecordStatus.SEALED)">Sealed</view>
        <view class="chip" :class="{ active: status === RecordStatus.UNLOCKED }" @tap="setStatus(RecordStatus.UNLOCKED)">Unlocked</view>
      </view>
    </scroll-view>

    <view v-if="recordStore.loading" class="loading">Loading...</view>
    <view v-else-if="recordStore.list.length === 0">
      <EmptyState text="No records" />
    </view>
    <view v-else class="list">
      <view v-for="item in recordStore.list" :key="item.id" @tap="openDetail(item.id)">
        <RecordCard :item="item" />
      </view>
    </view>
  </view>
</template>

<style scoped>
.page {
  min-height: 100vh;
  padding: 24rpx;
}

.filters {
  white-space: nowrap;
  margin-bottom: 18rpx;
}

.chips {
  display: inline-flex;
  gap: 12rpx;
}

.chip {
  padding: 10rpx 20rpx;
  border-radius: 9999rpx;
  background: #eef2f6;
  color: #344054;
  font-size: 24rpx;
}

.chip.active {
  background: #0ea5e9;
  color: #ffffff;
}

.loading {
  color: #667085;
}

.list {
  display: flex;
  flex-direction: column;
  gap: 14rpx;
}
</style>
