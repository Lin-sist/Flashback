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

const { cssVars, statusBarHeight, navBarHeight, rightSafeWidth } = useWechatNavMetrics()

// 顶部安全区：状态栏 + 胶囊按钮高度之下再留一点缓冲
const safeTopStyle = computed(() => ({
  ...cssVars.value,
  paddingTop: `calc(${statusBarHeight.value}px + ${navBarHeight.value}px + 8rpx)`,
  // 右侧避让微信胶囊按钮（≈96px 宽 + 10px margin），X 按钮放在下一行/更下方，保险起见加右安全宽
  '--fb-right-safe': `${rightSafeWidth.value}px`,
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

// 根据 createdAt 推导「季节，年份」档案副标题
const archiveNo = computed(() => {
  if (!detail.value?.id) return '—'
  return String(detail.value.id).padStart(3, '0')
})

const archiveSeason = computed(() => {
  const raw = detail.value?.createdAt
  if (!raw) return ''
  const date = new Date(typeof raw === 'string' && !raw.includes('T') ? raw.replace(' ', 'T') : raw)
  if (Number.isNaN(date.getTime())) return ''
  const m = date.getMonth() + 1
  const year = date.getFullYear()
  let season = ''
  if (m >= 3 && m <= 5) season = 'Spring'
  else if (m >= 6 && m <= 8) season = 'Summer'
  else if (m >= 9 && m <= 11) season = 'Autumn'
  else season = 'Winter'
  return `${season}, ${year}`
})

const archiveDateText = computed(() => {
  if (!detail.value?.createdAt) return ''
  return formatDateTime(detail.value.createdAt)
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
  <view class="archive-page" :style="safeTopStyle">
    <!-- 纸雾 / 点阵档案底纹 -->
    <view class="archive-backdrop" />

    <view v-if="detailLoading" class="state-wrap">
      <EmptyState text="正在加载记录详情..." />
      <PrimaryButton text="返回上一页" ghost @tap="closePage" />
    </view>

    <view v-else-if="hasDetailError" class="state-wrap">
      <EmptyState :text="detailErrorText" />
      <PrimaryButton :text="detailErrorState === 'INVALID_ID' ? '返回上一页' : '重试加载'" ghost @tap="detailErrorState === 'INVALID_ID' ? closePage : retryLoadDetail" />
    </view>

    <view v-else-if="detail" class="archive-stage">
      <!-- 顶部档案标识：左侧档案编号 + 季节年份；右侧关闭 X -->
      <view class="archive-header">
        <view class="archive-ident">
          <view class="archive-ident__no">ARCHIVE NO. {{ archiveNo }}</view>
          <view v-if="archiveSeason" class="archive-ident__season">{{ archiveSeason }}</view>
          <view v-if="archiveDateText" class="archive-ident__date">{{ archiveDateText }}</view>
        </view>
        <view class="archive-close" @tap="closePage">
          <text class="archive-close__icon">✕</text>
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
        <!-- 纸页：引句 + 正文 + 轻装饰 -->
        <view class="letter-paper">
          <view class="letter-quote">
            <text class="letter-quote__mark letter-quote__mark--open">&ldquo;</text>
            <text class="letter-quote__text">{{ detail.title || '未命名来信' }}</text>
            <text class="letter-quote__mark letter-quote__mark--close">&rdquo;</text>
          </view>

          <view class="letter-body">{{ detail.content }}</view>

          <view class="letter-decor">
            <text class="letter-decor__glyph">✦</text>
            <text class="letter-decor__glyph letter-decor__glyph--sm">✧</text>
          </view>
        </view>

        <!-- 此刻回应区：未回应为轻灰蓝输入带；已回应转为纸条样式 -->
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
                <view class="present-label">THE PRESENT MOMENT</view>
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
                <view class="present-label">THE PRESENT MOMENT</view>
                <view class="present-note">已留下回应</view>
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
              <view class="present-label">THE PRESENT MOMENT</view>
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
              <view class="present-label">THE PRESENT MOMENT</view>
            </view>
          </template>
        </view>
      </view>
    </view>

    <view v-else class="state-wrap">
      <EmptyState text="记录暂时不可用" />
      <PrimaryButton text="重试加载" ghost @tap="retryLoadDetail" />
    </view>
  </view>
</template>

<style scoped>
/* ========== 页面底 ========== */
.archive-page {
  position: relative;
  min-height: 100vh;
  /* 顶部由 inline style 提供 paddingTop（安全区 + 胶囊高度） */
  padding-left: 48rpx;
  padding-right: 48rpx;
  padding-bottom: 80rpx;
  background: #f4f6f8;
  overflow: hidden;
}

/* 极轻档案底纹：冷灰白 + 点阵 */
.archive-backdrop {
  position: absolute;
  inset: 0;
  z-index: 0;
  background-color: #f4f6f8;
  background-image:
    radial-gradient(circle at 18% 8%, rgba(231, 235, 239, 0.9) 0%, rgba(244, 246, 248, 0) 55%),
    radial-gradient(rgba(120, 136, 150, 0.12) 1rpx, transparent 1rpx);
  background-size: 100% 100%, 20rpx 20rpx;
  background-position: 0 0, 0 0;
  pointer-events: none;
}

.archive-stage,
.state-wrap {
  position: relative;
  z-index: 1;
}

.state-wrap {
  margin-top: 160rpx;
  display: flex;
  flex-direction: column;
  gap: 24rpx;
  align-items: center;
}

/* ========== 顶部档案标识 ========== */
.archive-header {
  position: relative;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding-top: 8rpx;
  /* 右侧避让微信胶囊按钮宽度 */
  padding-right: calc(var(--fb-right-safe, 96px) - 48rpx + 16rpx);
  min-height: 120rpx;
}

.archive-ident__no {
  color: #8a95a0;
  font-size: 22rpx;
  letter-spacing: 4rpx;
  font-family: 'Georgia', 'Songti SC', 'STSong', serif;
}

.archive-ident__season {
  margin-top: 14rpx;
  color: #8a95a0;
  font-size: 28rpx;
  font-style: italic;
  font-family: 'Georgia', 'Songti SC', 'STSong', serif;
  letter-spacing: 1rpx;
}

.archive-ident__date {
  margin-top: 8rpx;
  color: #a6afb7;
  font-size: 20rpx;
  letter-spacing: 2rpx;
}

.archive-close {
  /* X 放在胶囊按钮下方，即顶部 padding 之后的内容行内 */
  width: 64rpx;
  height: 64rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  /* 绝对定位到右上安全位置，避让胶囊 */
  position: absolute;
  top: 8rpx;
  right: 0;
}

.archive-close__icon {
  color: #a6afb7;
  font-size: 40rpx;
  line-height: 1;
  font-weight: 300;
}

/* ========== Fallback: DRAFT / SEALED ========== */
.fallback-panel {
  margin-top: 48rpx;
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
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
}

.panel-content {
  margin-top: 16rpx;
  line-height: 1.8;
  color: #8a95a0;
  font-size: 28rpx;
}

.panel-time {
  margin-top: 20rpx;
  color: #3b647a;
  font-size: 24rpx;
  letter-spacing: 1rpx;
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
}

.time-value {
  color: #3b647a;
  font-size: 24rpx;
}

/* ========== UNLOCKED: 信件舞台 ========== */
.letter-stage {
  margin-top: 56rpx;
  display: flex;
  flex-direction: column;
  gap: 56rpx;
}

/* 纸页 */
.letter-paper {
  position: relative;
  background: #fbf6e9;
  border-radius: 36rpx;
  padding: 64rpx 56rpx 72rpx;
  box-shadow:
    0 1rpx 0 rgba(255, 255, 255, 0.6) inset,
    0 20rpx 48rpx rgba(67, 82, 96, 0.08);
}

/* 引句 */
.letter-quote {
  color: #2f3a44;
  font-size: 44rpx;
  line-height: 1.55;
  font-weight: 500;
  font-style: italic;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', 'Georgia', serif;
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
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', 'PingFang SC', serif;
}

/* 纸页右下轻装饰 */
.letter-decor {
  margin-top: 56rpx;
  display: flex;
  justify-content: flex-end;
  align-items: flex-end;
  gap: 6rpx;
}

.letter-decor__glyph {
  color: #d9c28a;
  font-size: 32rpx;
  line-height: 1;
  opacity: 0.75;
}

.letter-decor__glyph--sm {
  font-size: 22rpx;
  opacity: 0.55;
  transform: translateY(-10rpx);
}

/* ========== 此刻回应区 ========== */
.present-area {
  display: flex;
  flex-direction: column;
  gap: 40rpx;
}

.present-slot {
  min-height: 120rpx;
  padding: 32rpx 32rpx;
  border-radius: 32rpx;
  background: rgba(218, 226, 233, 0.55);
  display: flex;
  align-items: center;
}

.present-placeholder {
  color: #9aa5b0;
  font-size: 28rpx;
  font-style: italic;
  letter-spacing: 1rpx;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', 'Georgia', serif;
}

.present-slot--input {
  align-items: stretch;
  padding: 28rpx 32rpx;
}

.present-textarea {
  width: 100%;
  min-height: 80rpx;
  background: transparent;
  color: #2c3a45;
  font-size: 28rpx;
  line-height: 1.7;
  letter-spacing: 1rpx;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', 'PingFang SC', serif;
}

.present-textarea__placeholder {
  color: #9aa5b0;
  font-style: italic;
}

.present-slot--submitted {
  flex-direction: column;
  align-items: stretch;
  background: rgba(251, 246, 233, 0.7);
  padding: 28rpx 32rpx;
  gap: 14rpx;
}

.present-slot__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.present-slot__label {
  color: #3b647a;
  font-size: 22rpx;
  letter-spacing: 3rpx;
  font-family: 'Georgia', 'Songti SC', serif;
  text-transform: uppercase;
}

.present-slot__time {
  color: #a6afb7;
  font-size: 22rpx;
  letter-spacing: 1rpx;
}

.present-slot__content {
  color: #2c3a45;
  font-size: 28rpx;
  line-height: 1.8;
  white-space: pre-wrap;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', 'PingFang SC', serif;
}

.present-slot--failed,
.present-slot--loading,
.present-slot--locked {
  background: rgba(218, 226, 233, 0.4);
}

/* 底部动作行 */
.present-action {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 8rpx;
  gap: 16rpx;
}

.present-label {
  color: #a6afb7;
  font-size: 22rpx;
  letter-spacing: 4rpx;
  font-family: 'Georgia', 'Songti SC', serif;
  text-transform: uppercase;
}

.present-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 72rpx;
  min-width: 180rpx;
  padding: 0 36rpx;
  border-radius: 999rpx;
  background: #3b647a;
  color: #fbf6e9;
  font-size: 26rpx;
  letter-spacing: 4rpx;
  box-shadow: 0 10rpx 24rpx rgba(59, 100, 122, 0.2);
}

.present-btn--disabled {
  background: #8ea4b1;
  box-shadow: none;
}

.present-retry {
  color: #3b647a;
  font-size: 24rpx;
  letter-spacing: 1rpx;
  padding: 8rpx 4rpx;
}

.present-note {
  color: #8a95a0;
  font-size: 24rpx;
  letter-spacing: 1rpx;
}
</style>
