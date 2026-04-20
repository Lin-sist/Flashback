<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import EmptyState from '../../components/common/EmptyState.vue'
import PaperContainer from '../../components/common/PaperContainer.vue'
import PrimaryButton from '../../components/common/PrimaryButton.vue'
import { useWechatNavMetrics } from '../../composables/useWechatNavMetrics'
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
const replyLoadFailed = ref(false)
const source = ref<EditorSource>('home')
const detailLoading = ref(false)
const currentRecordId = ref<number | null>(null)
const detailErrorState = ref<'NONE' | 'INVALID_ID' | 'NOT_FOUND' | 'LOAD_FAILED'>('NONE')

const { cssVars, navBarHeight, navBarTotalHeight, rightSafeWidth, statusBarHeight } =
  useWechatNavMetrics()

const pageStyle = computed(() => ({
  ...cssVars.value,
}))

const topSafeStyle = computed(() => ({
  minHeight: `calc(${navBarTotalHeight.value}px + 24rpx)`,
  paddingTop: `${statusBarHeight.value}px`,
}))

const topNavStyle = computed(() => ({
  height: `${navBarHeight.value}px`,
}))

const closeRailStyle = computed(() => ({
  right: `calc(${rightSafeWidth.value}px + 12px)`,
}))

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

// 顶部仅保留轻量时间信息
const archiveDateText = computed(() => {
  if (!detail.value?.createdAt) return ''
  return formatDateTime(detail.value.createdAt)
})

const unlockMomentText = computed(() => {
  const raw = detail.value?.unlockedAt || detail.value?.unlockAt
  if (!raw) return ''
  return formatDateTime(raw)
})

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
    replyLoadFailed.value = false
    return
  }

  replyLoading.value = true
  replyLoadFailed.value = false
  try {
    replyResult.value = await replyService.getReply(recordId)
  } catch {
    replyResult.value = null
    replyLoadFailed.value = true
  } finally {
    replyLoading.value = false
  }
}

const refreshUnlockState = async (recordId: number) => {
  const latest = await recordStore.fetchDetail(recordId)

  if (latest.status !== RecordStatus.UNLOCKED) {
    replyResult.value = null
    replyLoadFailed.value = false
    return
  }

  await loadReplyResult(recordId, Boolean(latest.hasReply))
}

const retryLoadReply = () => {
  if (!detail.value?.id || !detail.value.hasReply) {
    return
  }

  loadReplyResult(detail.value.id, true)
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
  replyLoadFailed.value = false

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
  <view class="archive-page" :class="{ 'archive-page--unlocked': isUnlocked }" :style="pageStyle">
    <!-- 纸雾 / 点阵档案底纹 -->
    <view class="archive-backdrop" />

    <view class="archive-top-safe" :style="topSafeStyle">
      <view class="archive-top-safe__mist" />

      <view class="archive-top-safe__nav" :style="topNavStyle">
        <view v-if="detail && !isUnlocked && archiveDateText" class="archive-top-safe__meta">
          <view class="archive-top-safe__subline">{{ archiveDateText }}</view>
        </view>

        <view v-if="!isUnlocked" class="archive-close" :style="closeRailStyle" @tap="closePage">
          <view class="archive-close__icon">
            <view class="archive-close__line archive-close__line--a" />
            <view class="archive-close__line archive-close__line--b" />
          </view>
        </view>
      </view>
    </view>

    <view class="archive-main">
      <view v-if="detailLoading" class="state-wrap">
        <EmptyState text="正在加载记录详情..." />
        <PrimaryButton text="返回上一页" ghost @tap="closePage" />
      </view>

      <view v-else-if="hasDetailError" class="state-wrap">
        <EmptyState :text="detailErrorText" />
        <PrimaryButton :text="detailErrorState === 'INVALID_ID' ? '返回上一页' : '重试加载'" ghost @tap="detailErrorState === 'INVALID_ID' ? closePage : retryLoadDetail" />
      </view>

      <view v-else-if="detail" class="archive-stage">
        <view class="archive-intro">
          <view v-if="isUnlocked" class="archive-close archive-close--content" @tap="closePage">
            <view class="archive-close__icon">
              <view class="archive-close__line archive-close__line--a" />
              <view class="archive-close__line archive-close__line--b" />
            </view>
          </view>
          <view v-if="isUnlocked" class="archive-intro__copy">
            {{ unlockMomentText || archiveDateText }}
          </view>
          <view v-else-if="isSealed" class="archive-intro__copy">
            档案仍在封存中，等到约定的时间再翻开。
          </view>
          <view v-else-if="isDraft" class="archive-intro__copy">
            这封信还在写作途中，可以继续补完后再封存。
          </view>
        </view>

        <!-- DRAFT：最小改动保留 -->
        <view v-if="isDraft" class="fallback-panel">
          <PaperContainer radius="xl" class="status-card">
            <view class="panel-title">继续完善后再封存</view>
            <view class="panel-content">这封信仍处于草稿阶段，尚未进入解锁阅读态。</view>
            <view class="panel-time">计划解锁：{{ formatDateTime(detail.unlockAt) }}</view>
          </PaperContainer>
          <PrimaryButton text="继续编辑草稿" @tap="openEditor" />
        </view>

        <!-- SEALED：最小改动保留 -->
        <view v-else-if="isSealed" class="fallback-panel">
          <PaperContainer radius="xl" warm class="status-card">
            <view class="panel-title">信件已封存，暂不可阅读</view>
            <view class="panel-content">到达解锁时间之前，内容保持封存状态。</view>
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

        <!-- UNLOCKED：档案信件阅读页 -->
        <view v-else-if="isUnlocked" class="letter-stage">
          <view class="letter-paper">
            <view class="letter-paper__glow" />

            <view class="letter-paper__header">
              <view class="letter-quote">
                <text class="letter-quote__mark letter-quote__mark--open">&ldquo;</text>
                <text class="letter-quote__text">{{ detail.title || '未命名来信' }}</text>
                <text class="letter-quote__mark letter-quote__mark--close">&rdquo;</text>
              </view>
            </view>

            <view class="letter-body">{{ detail.content }}</view>

            <view class="letter-paper__footer">
              <view v-if="unlockMomentText" class="letter-paper__footnote">
                启封于 {{ unlockMomentText }}
              </view>
            </view>
          </view>

          <view class="present-panel">
            <view class="present-panel__head">
              <view class="present-panel__title">给当时的自己，留下一句现在的话。</view>
            </view>

            <view class="present-area">
              <!-- 已提交回应 -->
              <template v-if="detail.hasReply">
                <view v-if="replyLoading" class="present-slot present-slot--loading">
                  <text class="present-placeholder">正在载入你留下的那句话…</text>
                </view>
                <template v-else-if="replyLoadFailed">
                  <view class="present-slot present-slot--failed">
                    <text class="present-placeholder">回应内容暂时加载失败，稍后再试</text>
                  </view>
                  <view class="present-action">
                    <view class="present-note">这句回应还在档案里，只是暂时没取出来。</view>
                    <view class="present-retry" @tap="retryLoadReply">
                      <text>重新加载</text>
                    </view>
                  </view>
                </template>
                <template v-else>
                  <view class="present-slot present-slot--submitted">
                    <view class="present-slot__header">
                      <text class="present-slot__label">你曾回应</text>
                      <text v-if="replyResult?.createdAt" class="present-slot__time">
                        {{ formatDateTime(replyResult.createdAt) }}
                      </text>
                    </view>
                    <view class="present-slot__content">{{ replyResult?.content }}</view>
                  </view>
                  <view class="present-action">
                    <view class="present-note">这句回应已经和旧信一起存档。</view>
                  </view>
                </template>
              </template>

              <!-- 未回应且可回应 -->
              <template v-else-if="detail.canReply">
                <view class="present-slot present-slot--input">
                  <textarea
                    v-model="replyContent"
                    class="present-textarea"
                    :disabled="submittingReply"
                    placeholder="此刻，想对当时的自己说句什么…"
                    placeholder-class="present-textarea__placeholder"
                    auto-height
                  />
                </view>
                <view class="present-action">
                  <view class="present-note">只写一句也可以，像把今天轻轻放回过去。</view>
                  <view
                    class="present-btn"
                    :class="{ 'present-btn--disabled': submittingReply }"
                    @tap="submitReply"
                  >
                    <text>{{ submittingReply ? '发送中…' : '留下回应' }}</text>
                  </view>
                </view>
              </template>

              <!-- 不可回应 -->
              <template v-else>
                <view class="present-slot present-slot--locked">
                  <text class="present-placeholder">此刻暂不可留下回应</text>
                </view>
                <view class="present-action">
                  <view class="present-note">当前状态下，回应入口保持关闭。</view>
                </view>
              </template>
            </view>
          </view>
        </view>
      </view>

      <view v-else class="state-wrap">
        <EmptyState text="记录暂时不可用" />
        <PrimaryButton text="重试加载" ghost @tap="retryLoadDetail" />
      </view>
    </view>
  </view>
</template>

<style scoped>
/* ========== 页面底 ========== */
.archive-page {
  position: relative;
  min-height: 100vh;
  --font-reading: 'Songti SC', 'STSong', 'Noto Serif SC', 'Source Han Serif SC', serif;
  --font-secondary:
    'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', 'Noto Sans SC', sans-serif;
  padding-left: 48rpx;
  padding-right: 48rpx;
  padding-bottom: 80rpx;
  background:
    radial-gradient(circle at top, rgba(246, 249, 251, 0.95) 0%, rgba(242, 245, 247, 0.94) 36%, #edf1f3 100%);
  overflow: hidden;
}

.archive-page--unlocked {
  background:
    radial-gradient(circle at top, rgba(249, 246, 239, 0.95) 0%, rgba(243, 239, 231, 0.6) 24%, rgba(237, 241, 243, 0.94) 100%);
}

/* 极轻档案底纹：冷灰白 + 点阵 */
.archive-backdrop {
  position: absolute;
  inset: 0;
  z-index: 0;
  background-image:
    radial-gradient(circle at 18% 8%, rgba(255, 253, 248, 0.88) 0%, rgba(244, 246, 248, 0) 56%),
    radial-gradient(rgba(120, 136, 150, 0.1) 1rpx, transparent 1rpx),
    linear-gradient(180deg, rgba(255, 255, 255, 0.55) 0%, rgba(255, 255, 255, 0) 30%);
  background-size: 100% 100%, 20rpx 20rpx, 100% 100%;
  background-position: 0 0, 0 0, 0 0;
  pointer-events: none;
}

.archive-top-safe,
.archive-main,
.state-wrap,
.archive-stage {
  position: relative;
  z-index: 1;
}

.archive-top-safe {
  z-index: 3;
}

.archive-top-safe__mist {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(120% 130% at 50% 0%, rgba(255, 255, 255, 0.82) 0%, rgba(255, 255, 255, 0) 60%),
    linear-gradient(180deg, rgba(239, 242, 244, 0.92) 0%, rgba(239, 242, 244, 0) 100%);
}

.archive-top-safe__nav {
  position: relative;
}

.archive-top-safe__meta {
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  max-width: calc(100% - (var(--wechat-right-safe-width, 96px) + 172rpx));
  padding-right: 12rpx;
}

.archive-top-safe__subline {
  color: #adb4ba;
  font-size: 20rpx;
  letter-spacing: 1rpx;
  font-family: var(--font-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.archive-main {
  margin-top: 16rpx;
}

.state-wrap {
  margin-top: 128rpx;
  display: flex;
  flex-direction: column;
  gap: 24rpx;
  align-items: center;
}

.archive-stage {
  display: flex;
  flex-direction: column;
  gap: 36rpx;
}

.archive-intro {
  position: relative;
  min-height: 76rpx;
  padding-right: 108rpx;
}

.archive-intro__copy {
  color: #a8afb5;
  font-size: 22rpx;
  line-height: 1.7;
  letter-spacing: 1rpx;
  font-family: var(--font-secondary);
}

.archive-close {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 76rpx;
  height: 76rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.34);
  border: 1rpx solid rgba(138, 149, 160, 0.12);
  backdrop-filter: blur(12rpx);
}

.archive-close--content {
  top: 0;
  right: 0;
  transform: none;
  width: 72rpx;
  height: 72rpx;
  background: rgba(255, 252, 247, 0.72);
  border-color: rgba(183, 173, 153, 0.2);
  box-shadow: 0 8rpx 24rpx rgba(97, 90, 79, 0.06);
}

.archive-close__icon {
  position: relative;
  width: 30rpx;
  height: 30rpx;
}

.archive-close__line {
  position: absolute;
  left: 0;
  top: 50%;
  width: 100%;
  height: 2rpx;
  border-radius: 999rpx;
  background: #6d777f;
}

.archive-close__line--a {
  transform: translateY(-50%) rotate(45deg);
}

.archive-close__line--b {
  transform: translateY(-50%) rotate(-45deg);
}

/* ========== Fallback: DRAFT / SEALED ========== */
.fallback-panel {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.status-card {
  box-shadow: 0 12rpx 32rpx rgba(26, 26, 26, 0.05);
}

.panel-title {
  color: #2c3a45;
  font-size: 36rpx;
  font-weight: 500;
  font-family: var(--font-reading);
}

.panel-content {
  margin-top: 16rpx;
  line-height: 1.8;
  color: #8a95a0;
  font-size: 28rpx;
  font-family: var(--font-secondary);
}

.panel-time {
  margin-top: 20rpx;
  color: #3b647a;
  font-size: 24rpx;
  letter-spacing: 1rpx;
  font-family: var(--font-secondary);
}

.time-grid {
  margin-top: 20rpx;
  display: flex;
  flex-direction: column;
  gap: 10rpx;
}

.time-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14rpx 20rpx;
  border-radius: 20rpx;
  background: rgba(255, 255, 255, 0.65);
}

.time-label {
  color: #8a95a0;
  font-size: 24rpx;
  font-family: var(--font-secondary);
}

.time-value {
  color: #3b647a;
  font-size: 24rpx;
  font-family: var(--font-secondary);
}

/* ========== UNLOCKED: 信件舞台 ========== */
.letter-stage {
  display: flex;
  flex-direction: column;
  gap: 40rpx;
}

/* 纸页 */
.letter-paper {
  position: relative;
  overflow: hidden;
  background:
    linear-gradient(180deg, rgba(255, 251, 244, 0.98) 0%, rgba(249, 243, 232, 0.98) 100%);
  border-radius: 42rpx;
  padding: 64rpx 56rpx 56rpx;
  border: 1rpx solid rgba(213, 199, 172, 0.4);
  box-shadow:
    0 1rpx 0 rgba(255, 255, 255, 0.6) inset,
    0 24rpx 60rpx rgba(90, 88, 80, 0.08);
}

.letter-paper__glow {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(circle at top, rgba(255, 255, 255, 0.78) 0%, rgba(255, 255, 255, 0) 36%),
    repeating-linear-gradient(
      180deg,
      rgba(150, 133, 101, 0.04) 0,
      rgba(150, 133, 101, 0.04) 2rpx,
      transparent 2rpx,
      transparent 22rpx
    );
  opacity: 0.7;
}

.letter-paper__header,
.letter-body,
.letter-paper__footer {
  position: relative;
  z-index: 1;
}

.letter-paper__header {
  padding-bottom: 12rpx;
}

/* 引句 */
.letter-quote {
  color: #2f3a44;
  font-size: 44rpx;
  line-height: 1.55;
  font-weight: 500;
  font-style: italic;
  font-family: var(--font-reading);
  letter-spacing: 2rpx;
  word-break: break-word;
}

.letter-quote__mark {
  color: #2f3a44;
  font-size: 44rpx;
  font-style: normal;
  display: inline;
}

.letter-quote__mark--open {
  margin-right: 2rpx;
}

.letter-quote__mark--close {
  margin-left: 2rpx;
}

.letter-quote__text {
  display: inline;
}

/* 正文 */
.letter-body {
  margin-top: 48rpx;
  color: #4a4336;
  font-size: 30rpx;
  line-height: 2.05;
  letter-spacing: 1rpx;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: var(--font-reading);
}

.letter-paper__footer {
  margin-top: 48rpx;
  display: flex;
  align-items: flex-end;
  justify-content: flex-start;
  gap: 20rpx;
}

.letter-paper__footnote {
  color: #a7a096;
  font-size: 22rpx;
  letter-spacing: 1rpx;
  font-family: var(--font-secondary);
}

.present-panel {
  position: relative;
  overflow: hidden;
  border-radius: 36rpx;
  padding: 36rpx;
  background:
    linear-gradient(180deg, rgba(246, 243, 237, 0.68) 0%, rgba(237, 241, 244, 0.58) 100%);
  border: 1rpx solid rgba(183, 193, 201, 0.28);
  box-shadow: 0 12rpx 34rpx rgba(77, 91, 104, 0.05);
}

.present-panel__head,
.present-area {
  position: relative;
  z-index: 1;
}

.present-panel__head {
  display: flex;
  flex-direction: column;
  gap: 10rpx;
}

.present-panel__title {
  color: #4b453d;
  font-size: 30rpx;
  line-height: 1.5;
  font-family: var(--font-reading);
}

.present-area {
  margin-top: 24rpx;
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.present-slot {
  min-height: 120rpx;
  padding: 30rpx 32rpx;
  border-radius: 32rpx;
  background: rgba(233, 237, 240, 0.52);
  display: flex;
  align-items: center;
}

.present-placeholder {
  color: #9aa5b0;
  font-size: 26rpx;
  letter-spacing: 1rpx;
  font-family: var(--font-secondary);
}

.present-slot--input {
  align-items: stretch;
  padding: 28rpx 32rpx;
  background: rgba(248, 244, 237, 0.74);
  border: 1rpx solid rgba(190, 176, 147, 0.18);
}

.present-textarea {
  width: 100%;
  min-height: 80rpx;
  background: transparent;
  color: #2c3a45;
  font-size: 28rpx;
  line-height: 1.7;
  letter-spacing: 1rpx;
  font-family: var(--font-reading);
}

.present-textarea__placeholder {
  color: #9aa5b0;
}

.present-slot--submitted {
  flex-direction: column;
  align-items: stretch;
  background: rgba(251, 247, 239, 0.8);
  padding: 28rpx 32rpx;
  gap: 14rpx;
  border: 1rpx solid rgba(190, 176, 147, 0.16);
}

.present-slot__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.present-slot__label {
  color: #8f97a0;
  font-size: 22rpx;
  letter-spacing: 1rpx;
  font-family: var(--font-secondary);
}

.present-slot__time {
  color: #afb6bc;
  font-size: 22rpx;
  letter-spacing: 1rpx;
  font-family: var(--font-secondary);
}

.present-slot__content {
  color: #4b453d;
  font-size: 28rpx;
  line-height: 1.8;
  white-space: pre-wrap;
  font-family: var(--font-reading);
}

.present-slot--failed,
.present-slot--loading,
.present-slot--locked {
  background: rgba(232, 236, 239, 0.52);
}

/* 底部动作行 */
.present-action {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 8rpx;
  gap: 16rpx;
}

.present-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 64rpx;
  min-width: 160rpx;
  padding: 0 30rpx;
  border-radius: 999rpx;
  border: 1rpx solid rgba(168, 160, 148, 0.24);
  background: rgba(255, 252, 247, 0.62);
  color: #746d64;
  font-size: 24rpx;
  letter-spacing: 2rpx;
  font-family: var(--font-secondary);
}

.present-btn--disabled {
  opacity: 0.6;
}

.present-retry {
  color: #3b647a;
  font-size: 24rpx;
  letter-spacing: 1rpx;
  padding: 8rpx 0;
  font-family: var(--font-secondary);
}

.present-note {
  flex: 1;
  color: #8a95a0;
  font-size: 24rpx;
  line-height: 1.7;
  font-family: var(--font-secondary);
}
</style>
