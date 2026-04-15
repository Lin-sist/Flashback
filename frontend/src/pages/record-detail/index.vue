<script setup lang="ts">
import { onLoad } from '@dcloudio/uni-app'
import { ref } from 'vue'
import { useRecordStore } from '../../stores'
import { ReplyType } from '../../types'
import { replyService } from '../../services'
import { formatDateTime, getToken, mapRecordStatus, mapRecordType, toUserMessage } from '../../utils'

const recordStore = useRecordStore()
const replyContent = ref('')
const submittingReply = ref(false)

const ensureLogin = () => {
  if (!getToken()) {
    uni.reLaunch({ url: '/pages/login/index' })
    return false
  }
  return true
}

onLoad(async (query) => {
  if (!ensureLogin()) {
    return
  }
  if (!query?.id || typeof query.id !== 'string') {
    uni.showToast({ title: 'Invalid record id', icon: 'none' })
    return
  }
  try {
    await recordStore.fetchDetail(query.id)
  } catch (error) {
    uni.showToast({ title: toUserMessage(error), icon: 'none' })
  }
})

const submitReply = async () => {
  if (!recordStore.detail?.id || !replyContent.value.trim()) {
    uni.showToast({ title: 'Reply content required', icon: 'none' })
    return
  }
  submittingReply.value = true
  try {
    await replyService.submitReply(recordStore.detail.id, {
      content: replyContent.value,
      replyType: ReplyType.SHORT_REPLY,
    })
    uni.showToast({ title: 'Reply submitted', icon: 'success' })
    replyContent.value = ''
  } catch (error) {
    uni.showToast({ title: toUserMessage(error), icon: 'none' })
  } finally {
    submittingReply.value = false
  }
}
</script>

<template>
  <view class="page" v-if="recordStore.detail">
    <view class="title">{{ recordStore.detail.title || 'Untitled' }}</view>
    <view class="meta">
      <text>{{ mapRecordType(recordStore.detail.recordType) }}</text>
      <text>{{ mapRecordStatus(recordStore.detail.status) }}</text>
      <text>Created {{ formatDateTime(recordStore.detail.createdAt) }}</text>
    </view>
    <view class="content">{{ recordStore.detail.content }}</view>

    <view class="reply-box" v-if="recordStore.detail.canReply && !recordStore.detail.hasReply">
      <textarea v-model="replyContent" class="textarea" placeholder="Leave one sentence to your past self" />
      <button class="btn" :loading="submittingReply" @tap="submitReply">Submit Reply</button>
    </view>
  </view>
</template>

<style scoped>
.page {
  min-height: 100vh;
  padding: 24rpx;
}

.title {
  font-size: 34rpx;
  font-weight: 700;
}

.meta {
  margin-top: 10rpx;
  color: #667085;
  font-size: 24rpx;
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}

.content {
  margin-top: 22rpx;
  background: #ffffff;
  border-radius: 14rpx;
  padding: 20rpx;
  line-height: 1.6;
}

.reply-box {
  margin-top: 20rpx;
}

.textarea {
  width: 100%;
  box-sizing: border-box;
  min-height: 160rpx;
  background: #ffffff;
  border-radius: 12rpx;
  padding: 16rpx;
}

.btn {
  margin-top: 12rpx;
  border-radius: 9999rpx;
  background: #0ea5e9;
  color: #ffffff;
}
</style>
