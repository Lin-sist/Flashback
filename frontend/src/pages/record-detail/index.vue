<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import PaperContainer from '../../components/common/PaperContainer.vue'
import PrimaryButton from '../../components/common/PrimaryButton.vue'
import { replyService } from '../../services'
import { useRecordStore } from '../../stores'
import { RecordStatus, ReplyType } from '../../types'
import { formatDateTime, getToken, toUserMessage } from '../../utils'

const recordStore = useRecordStore()
const replyContent = ref('')
const submittingReply = ref(false)

const detail = computed(() => recordStore.detail)

const isDraft = computed(() => detail.value?.status === RecordStatus.DRAFT)
const isSealed = computed(() => detail.value?.status === RecordStatus.SEALED)
const isUnlocked = computed(() => detail.value?.status === RecordStatus.UNLOCKED)

const ensureLogin = () => {
  if (!getToken()) {
    uni.reLaunch({ url: '/pages/login/index' })
    return false
  }
  return true
}

const closePage = () => uni.navigateBack({ delta: 1 })

const openEditor = () => {
  if (!detail.value) {
    return
  }
  uni.navigateTo({ url: `/pages/record-editor/index?id=${detail.value.id}` })
}

const submitReply = async () => {
  if (!detail.value?.id || !replyContent.value.trim()) {
    uni.showToast({ title: '请输入回应内容', icon: 'none' })
    return
  }
  submittingReply.value = true
  try {
    await replyService.submitReply(detail.value.id, {
      content: replyContent.value.trim(),
      replyType: ReplyType.SHORT_REPLY,
    })
    uni.showToast({ title: '回应已保存', icon: 'success' })
    replyContent.value = ''
    await recordStore.fetchDetail(detail.value.id)
  } catch (error) {
    uni.showToast({ title: toUserMessage(error), icon: 'none' })
  } finally {
    submittingReply.value = false
  }
}

onLoad(async (query) => {
  if (!ensureLogin()) {
    return
  }
  if (!query?.id || typeof query.id !== 'string') {
    uni.showToast({ title: '记录ID无效', icon: 'none' })
    return
  }

  const id = Number(query.id)
  if (Number.isNaN(id)) {
    uni.showToast({ title: '记录ID无效', icon: 'none' })
    return
  }

  try {
    await recordStore.fetchDetail(id)
  } catch (error) {
    uni.showToast({ title: toUserMessage(error), icon: 'none' })
  }
})
</script>

<template>
  <view class="page" v-if="detail">
    <view class="meta-head">
      <view>
        <view class="archive-no">ARCHIVE NO. {{ detail.id }}</view>
        <view class="archive-meta">{{ formatDateTime(detail.createdAt) }}</view>
      </view>
      <view class="close-btn" @tap="closePage">✕</view>
    </view>

    <view v-if="isDraft" class="status-panel">
      <PaperContainer radius="xl">
        <view class="panel-title">草稿状态</view>
        <view class="panel-content">这封信仍在草稿箱中，你可以继续修改正文、标签与解锁时间后再封存。</view>
        <view class="panel-time">计划解锁：{{ formatDateTime(detail.unlockAt) }}</view>
      </PaperContainer>
      <PrimaryButton text="继续编辑草稿" @tap="openEditor" />
    </view>

    <view v-else-if="isSealed" class="status-panel">
      <PaperContainer radius="lg" warm>
        <view class="panel-title">已封存 · 等待解锁</view>
        <view class="panel-content">信件已被封存，只有在到达解锁时间后才会进入阅读态。</view>
        <view class="panel-time">封存时间：{{ formatDateTime(detail.sealedAt) }}</view>
        <view class="panel-time">解锁时间：{{ formatDateTime(detail.unlockAt) }}</view>
      </PaperContainer>
    </view>

    <view v-else-if="isUnlocked" class="letter-layout">
      <PaperContainer radius="sm" warm class="letter-paper">
        <view class="letter-title">{{ detail.title || '未命名来信' }}</view>
        <view class="letter-text">{{ detail.content }}</view>
      </PaperContainer>

      <PaperContainer radius="md" class="reply-shell">
        <view class="reply-title">给当时的自己留一句回应</view>
        <textarea
          v-model="replyContent"
          class="reply-area"
          :disabled="detail.hasReply || !detail.canReply"
          :placeholder="detail.hasReply || !detail.canReply ? '当前状态不可继续回应' : '写下这一刻的回应...'"
        />
        <PrimaryButton
          text="留下回应"
          :disabled="detail.hasReply || !detail.canReply"
          :loading="submittingReply"
          @tap="submitReply"
        />
      </PaperContainer>
    </view>
  </view>
</template>

<style scoped>
.page {
  min-height: 100vh;
  padding: 18rpx var(--fb-space-page) 40rpx;
  background: radial-gradient(circle at 20% 10%, #f3f7fa 0%, #f8fafb 35%, #f8fafb 100%);
}

.meta-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding-top: 6rpx;
}

.close-btn {
  color: var(--fb-color-text-muted);
  font-size: 40rpx;
  line-height: 1;
  padding: 8rpx;
}

.archive-no {
  color: var(--fb-color-text-muted);
  font-size: var(--fb-font-meta);
  letter-spacing: 2rpx;
}

.archive-meta {
  margin-top: 8rpx;
  color: var(--fb-color-text-muted);
  font-size: var(--fb-font-meta);
}

.status-panel {
  margin-top: 20rpx;
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.panel-title {
  color: var(--fb-color-text);
  font-size: var(--fb-font-title-sub);
  font-weight: 600;
}

.panel-content {
  margin-top: 12rpx;
  line-height: 1.8;
  color: var(--fb-color-text-muted);
  font-size: var(--fb-font-body-sub);
}

.panel-time {
  margin-top: 10rpx;
  color: var(--fb-color-primary);
  font-size: var(--fb-font-meta);
}

.letter-layout {
  margin-top: 20rpx;
  display: flex;
  flex-direction: column;
  gap: 18rpx;
}

.letter-paper {
  min-height: 520rpx;
}

.letter-title {
  font-size: 40rpx;
  color: #3f3a31;
  font-weight: 500;
}

.letter-text {
  margin-top: 16rpx;
  line-height: 1.9;
  color: #463f35;
  font-size: var(--fb-font-body);
  white-space: pre-wrap;
}

.reply-shell {
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(8rpx);
}

.reply-title {
  font-size: var(--fb-font-body-sub);
  color: var(--fb-color-text);
  margin-bottom: 10rpx;
}

.reply-area {
  width: 100%;
  min-height: 170rpx;
  border-radius: var(--fb-radius-md);
  background: rgba(248, 250, 251, 0.85);
  padding: 18rpx;
  margin-bottom: 14rpx;
  font-size: var(--fb-font-body-sub);
  line-height: 1.7;
}
</style>
