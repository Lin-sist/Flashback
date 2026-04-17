<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import EmptyState from '../../components/common/EmptyState.vue'
import PaperContainer from '../../components/common/PaperContainer.vue'
import PrimaryButton from '../../components/common/PrimaryButton.vue'
import { replyService } from '../../services'
import { useRecordStore } from '../../stores'
import { RecordStatus, ReplyType, type ReplyVO } from '../../types'
import { formatDateTime, getToken, toUserMessage } from '../../utils'

type EditorSource = 'home' | 'archive' | 'timeline'

const recordStore = useRecordStore()
const replyContent = ref('')
const submittingReply = ref(false)
const replyLoading = ref(false)
const replyResult = ref<ReplyVO | null>(null)
const source = ref<EditorSource>('home')
const detailLoading = ref(false)
const currentRecordId = ref<number | null>(null)
const detailErrorState = ref<'NONE' | 'INVALID_ID' | 'NOT_FOUND' | 'LOAD_FAILED'>('NONE')

const detail = computed(() => {
  if (!currentRecordId.value || !recordStore.detail) {
    return null
  }

  return Number(recordStore.detail.id) === currentRecordId.value ? recordStore.detail : null
})

const hasDetailError = computed(() => detailErrorState.value !== 'NONE')

const detailErrorText = computed(() => {
  if (detailErrorState.value === 'INVALID_ID') {
    return '记录地址不完整，请返回后重试'
  }

  if (detailErrorState.value === 'NOT_FOUND') {
    return '这条记录可能已不存在或暂时不可见'
  }

  if (detailErrorState.value === 'LOAD_FAILED') {
    return '网络有点慢，记录详情暂时没加载出来'
  }

  return '记录暂时不可用'
})

const isDraft = computed(() => detail.value?.status === RecordStatus.DRAFT)
const isSealed = computed(() => detail.value?.status === RecordStatus.SEALED)
const isUnlocked = computed(() => detail.value?.status === RecordStatus.UNLOCKED)
const canSubmitReply = computed(() => Boolean(detail.value?.canReply && !detail.value?.hasReply))
const hasSubmittedReply = computed(() => Boolean(detail.value?.hasReply))

const ensureLogin = () => {
  if (!getToken()) {
    uni.reLaunch({ url: '/pages/login/index' })
    return false
  }
  return true
}

const fallbackBySource = () => {
  if (source.value === 'timeline') {
    uni.switchTab({ url: '/pages/timeline/index' })
    return
  }

  if (source.value === 'archive') {
    uni.navigateTo({ url: '/pages/record-list/index' })
    return
  }

  uni.switchTab({ url: '/pages/home/index' })
}

const closePage = () => {
  uni.navigateBack({
    delta: 1,
    fail: () => {
      fallbackBySource()
    },
  })
}

const resolveSource = (value: unknown): EditorSource | null => {
  if (value === 'archive' || value === 'timeline' || value === 'home') {
    return value
  }

  return null
}

const inferSourceFromPrevPage = (): EditorSource => {
  const pages = getCurrentPages()
  if (pages.length < 2) {
    return 'home'
  }

  const prevRoute = pages[pages.length - 2]?.route
  if (prevRoute === 'pages/record-list/index') {
    return 'archive'
  }
  if (prevRoute === 'pages/timeline/index') {
    return 'timeline'
  }

  return 'home'
}

const resolveDetailErrorState = (error: unknown): 'NOT_FOUND' | 'LOAD_FAILED' => {
  const message = toUserMessage(error).toLowerCase()
  if (message.includes('not found') || message.includes('不存在')) {
    return 'NOT_FOUND'
  }

  return 'LOAD_FAILED'
}

const openEditor = () => {
  if (!detail.value) {
    return
  }
  uni.navigateTo({ url: `/pages/record-editor/index?id=${detail.value.id}&source=${source.value}` })
}

const loadReplyResult = async (recordId: number, hasReply: boolean) => {
  if (!hasReply) {
    replyResult.value = null
    return
  }

  replyLoading.value = true
  try {
    replyResult.value = await replyService.getReply(recordId)
  } catch {
    replyResult.value = null
    uni.showToast({ title: '回应暂时加载失败', icon: 'none' })
  } finally {
    replyLoading.value = false
  }
}

const refreshUnlockState = async (recordId: number) => {
  const latest = await recordStore.fetchDetail(recordId)

  if (latest.status !== RecordStatus.UNLOCKED) {
    replyResult.value = null
    return
  }

  await loadReplyResult(recordId, Boolean(latest.hasReply))
}

const loadDetail = async (recordId: number) => {
  detailLoading.value = true
  detailErrorState.value = 'NONE'

  try {
    await refreshUnlockState(recordId)
  } catch (error) {
    detailErrorState.value = resolveDetailErrorState(error)
  } finally {
    detailLoading.value = false
  }
}

const retryLoadDetail = () => {
  if (!currentRecordId.value) {
    closePage()
    return
  }

  loadDetail(currentRecordId.value)
}

const submitReply = async () => {
  if (!detail.value?.id || !canSubmitReply.value) {
    uni.showToast({ title: hasSubmittedReply.value ? '已提交过回应' : '当前状态不可继续回应', icon: 'none' })
    return
  }

  if (!replyContent.value.trim()) {
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
    await refreshUnlockState(detail.value.id)
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

  const querySource = resolveSource(typeof query?.source === 'string' ? query.source : undefined)
  source.value = querySource || inferSourceFromPrevPage()
  recordStore.detail = null
  detailErrorState.value = 'NONE'
  replyResult.value = null

  if (!query?.id || typeof query.id !== 'string') {
    detailErrorState.value = 'INVALID_ID'
    return
  }

  const id = Number(query.id)
  if (Number.isNaN(id)) {
    detailErrorState.value = 'INVALID_ID'
    return
  }

  currentRecordId.value = id
  await loadDetail(id)
})
</script>

<template>
  <view class="page">
    <view v-if="detailLoading" class="state-wrap">
      <EmptyState text="正在加载记录详情..." />
      <PrimaryButton text="返回上一页" ghost @tap="closePage" />
    </view>

    <view v-else-if="hasDetailError" class="state-wrap">
      <EmptyState :text="detailErrorText" />
      <PrimaryButton :text="detailErrorState === 'INVALID_ID' ? '返回上一页' : '重试加载'" ghost @tap="detailErrorState === 'INVALID_ID' ? closePage : retryLoadDetail" />
    </view>

    <view v-else-if="detail">
      <view class="meta-head">
        <view>
          <view class="archive-no">ARCHIVE NO. {{ detail.id }}</view>
          <view class="archive-meta">{{ formatDateTime(detail.createdAt) }}</view>
        </view>
        <view class="close-btn" @tap="closePage">✕</view>
      </view>

      <view v-if="isDraft" class="status-panel">
        <PaperContainer radius="xl" class="status-card">
          <view class="state-head">
            <view class="state-badge badge-draft">DRAFT 草稿</view>
            <view class="state-kicker">可继续编辑</view>
          </view>
          <view class="panel-title">继续完善后再封存</view>
          <view class="panel-content">这封信仍处于草稿阶段，当前不会进入解锁阅读态。</view>
          <view class="panel-time">计划解锁：{{ formatDateTime(detail.unlockAt) }}</view>
        </PaperContainer>
        <PrimaryButton text="继续编辑草稿" @tap="openEditor" />
      </view>

      <view v-else-if="isSealed" class="status-panel status-panel-sealed">
        <PaperContainer radius="lg" warm class="status-card">
          <view class="state-head">
            <view class="state-badge badge-sealed">SEALED 已封存</view>
            <view class="state-kicker">等待解锁中</view>
          </view>
          <view class="panel-title">信件已封存，暂不可编辑</view>
          <view class="panel-content">到达解锁时间之前，内容保持封存状态，仅展示时间信息。</view>
          <view class="time-grid">
            <view class="time-item">
              <view class="time-label">封存时间</view>
              <view class="time-value">{{ formatDateTime(detail.sealedAt) }}</view>
            </view>
            <view class="time-item">
              <view class="time-label">解锁时间</view>
              <view class="time-value">{{ formatDateTime(detail.unlockAt) }}</view>
            </view>
          </view>
        </PaperContainer>
      </view>

      <view v-else-if="isUnlocked" class="letter-layout">
        <view class="unlock-head">
          <view class="state-badge badge-unlocked">UNLOCKED 已解锁</view>
          <view class="unlock-time">解锁时间：{{ formatDateTime(detail.unlockedAt || detail.unlockAt) }}</view>
        </view>

        <PaperContainer radius="sm" warm class="letter-paper">
          <view class="letter-title">{{ detail.title || '未命名来信' }}</view>
          <view class="letter-text">{{ detail.content }}</view>
        </PaperContainer>

        <PaperContainer radius="md" class="reply-shell">
          <view class="reply-title">给当时的自己留一句回应</view>
          <view class="reply-subtitle">已解锁后可阅读原文并留下一句回应</view>
          <view v-if="detail.hasReply" class="reply-result">
            <view v-if="replyLoading" class="reply-result-loading">正在载入已提交回应...</view>
            <template v-else>
              <view class="reply-state-marker marker-submitted">回应已提交</view>
              <view class="reply-result-content">{{ replyResult?.content || '回应已保存' }}</view>
              <view v-if="replyResult?.createdAt" class="reply-result-time">
                提交时间：{{ formatDateTime(replyResult.createdAt) }}
              </view>
            </template>
          </view>
          <template v-else-if="detail.canReply">
            <view class="reply-state-marker marker-pending">可写一条回信</view>
            <textarea
              v-model="replyContent"
              class="reply-area"
              :disabled="submittingReply"
              placeholder="写下这一刻的回应..."
            />
            <PrimaryButton
              text="留下回应"
              :disabled="submittingReply"
              :loading="submittingReply"
              @tap="submitReply"
            />
          </template>
          <view v-else class="reply-disabled-tip">
            <view class="reply-state-marker marker-locked">暂不可回应</view>
            <view class="reply-disabled-text">当前状态不可继续回应</view>
          </view>
        </PaperContainer>
      </view>
    </view>

    <view v-else class="state-wrap">
      <EmptyState text="记录暂时不可用" />
      <PrimaryButton text="重试加载" ghost @tap="retryLoadDetail" />
    </view>
  </view>
</template>

<style scoped>
.page {
  min-height: 100vh;
  padding: 18rpx var(--fb-space-page) 40rpx;
  background: radial-gradient(circle at 20% 10%, #f3f7fa 0%, #f8fafb 35%, #f8fafb 100%);
}

.state-wrap {
  margin-top: 120rpx;
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

.status-card {
  box-shadow: var(--fb-shadow-soft);
}

.status-panel-sealed .status-card {
  border: 1rpx solid rgba(127, 140, 147, 0.26);
  background: linear-gradient(160deg, rgba(255, 253, 245, 0.95) 0%, rgba(248, 250, 251, 0.9) 100%);
}

.state-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
}

.state-badge {
  display: inline-flex;
  align-items: center;
  padding: 4rpx 14rpx;
  border-radius: var(--fb-radius-pill);
  font-size: var(--fb-font-meta);
  letter-spacing: 1rpx;
}

.badge-draft {
  background: #f6e8d8;
  color: #8a6b4a;
}

.badge-sealed {
  background: #eef2f5;
  color: #5e6b73;
}

.badge-unlocked {
  background: #eaf1f4;
  color: var(--fb-color-primary);
}

.state-kicker {
  color: var(--fb-color-text-muted);
  font-size: var(--fb-font-meta);
}

.panel-title {
  margin-top: 12rpx;
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

.time-grid {
  margin-top: 12rpx;
  display: flex;
  flex-direction: column;
  gap: 10rpx;
}

.time-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10rpx 14rpx;
  border-radius: var(--fb-radius-md);
  background: rgba(255, 255, 255, 0.7);
}

.time-label {
  color: var(--fb-color-text-muted);
  font-size: var(--fb-font-meta);
}

.time-value {
  color: var(--fb-color-primary);
  font-size: var(--fb-font-meta);
}

.letter-layout {
  margin-top: 20rpx;
  display: flex;
  flex-direction: column;
  gap: 18rpx;
}

.unlock-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
}

.unlock-time {
  color: var(--fb-color-text-muted);
  font-size: var(--fb-font-meta);
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
}

.reply-subtitle {
  margin-top: 6rpx;
  margin-bottom: 12rpx;
  color: var(--fb-color-text-muted);
  font-size: var(--fb-font-meta);
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

.reply-result {
  border-radius: var(--fb-radius-md);
  background: rgba(248, 250, 251, 0.85);
  padding: 18rpx;
}

.reply-result-loading {
  font-size: var(--fb-font-body-sub);
  color: var(--fb-color-text-muted);
}

.reply-state-marker {
  display: inline-flex;
  align-items: center;
  margin-bottom: 10rpx;
  padding: 4rpx 12rpx;
  border-radius: 999rpx;
  font-size: var(--fb-font-meta);
}

.marker-submitted {
  background: rgba(59, 100, 122, 0.12);
  color: var(--fb-color-primary);
}

.marker-pending {
  background: rgba(246, 232, 216, 0.8);
  color: #8a6b4a;
}

.marker-locked {
  background: rgba(127, 140, 147, 0.14);
  color: #5e6b73;
}

.reply-result-content {
  color: var(--fb-color-text);
  font-size: var(--fb-font-body-sub);
  line-height: 1.8;
  white-space: pre-wrap;
}

.reply-result-time {
  margin-top: 12rpx;
  color: var(--fb-color-text-muted);
  font-size: var(--fb-font-meta);
}

.reply-disabled-tip {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.reply-disabled-text {
  color: var(--fb-color-text-muted);
  font-size: var(--fb-font-body-sub);
}
</style>
