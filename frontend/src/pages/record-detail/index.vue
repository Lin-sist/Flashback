<script setup lang="ts">
import { computed, onUnmounted, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import EmptyState from '../../components/common/EmptyState.vue'
import PaperContainer from '../../components/common/PaperContainer.vue'
import PrimaryButton from '../../components/common/PrimaryButton.vue'
import { useWechatNavMetrics } from '../../composables/useWechatNavMetrics'
import { hasPreviewSession, showPreviewReadonlyToast } from '../../features/preview/preview-session'
import { replyService } from '../../services'
import { useRecordStore } from '../../stores'
import { RecordStatus, ReplyType, type ReplyVO } from '../../types'
import { formatDateTime, getToken, hasAuthenticatedSession, toUserMessage } from '../../utils'

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

// 倒计时
const countdownH = ref('--')
const countdownM = ref('--')
const countdownS = ref('--')
let countdownTimer: ReturnType<typeof setInterval> | null = null

const startCountdown = (unlockAt: string | null | undefined) => {
  if (countdownTimer) clearInterval(countdownTimer)
  if (!unlockAt) return
  const target = new Date(unlockAt).getTime()
  const tick = () => {
    const diff = Math.max(0, target - Date.now())
    const h = Math.floor(diff / 3600000)
    const m = Math.floor((diff % 3600000) / 60000)
    const s = Math.floor((diff % 60000) / 1000)
    const pad = (n: number) => String(n).padStart(2, '0')
    countdownH.value = pad(h)
    countdownM.value = pad(m)
    countdownS.value = pad(s)
  }
  tick()
  countdownTimer = setInterval(tick, 1000)
}

onUnmounted(() => { if (countdownTimer) clearInterval(countdownTimer) })

// Archive meta（封存页顶部 Archive No. / 季节）
const archiveNo = computed(() => {
  if (!detail.value?.id) return ''
  return `Archive No. ${String(detail.value.id).padStart(3, '0')}`
})

const archiveSeason = computed(() => {
  if (!detail.value?.createdAt) return ''
  const d = new Date(detail.value.createdAt)
  const seasons = ['Winter','Winter','Spring','Spring','Spring','Summer','Summer','Summer','Autumn','Autumn','Autumn','Winter']
  const years = ['2018','2019','2020','2021','2022','2023','2024','2025','2026']
  const year = years.find(y => String(d.getFullYear()) === y) || String(d.getFullYear())
  return `${seasons[d.getMonth()]}, ${year}`
})

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
  if (!hasAuthenticatedSession()) {
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
    if (detail.value?.status === RecordStatus.SEALED) {
      startCountdown(detail.value.unlockAt)
    }
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

  if (!getToken() && hasPreviewSession()) {
    showPreviewReadonlyToast()
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
    <!-- 宣纸底纹 -->
    <view class="archive-backdrop" />

    <!-- 顶部安全区：SEALED 态显示品牌名 + 关闭 -->
    <view class="archive-top-safe" :style="topSafeStyle">
      <view class="archive-top-safe__mist" />

      <view class="archive-top-safe__nav" :style="topNavStyle">
        <!-- SEALED / DRAFT：品牌名居中 -->
        <view v-if="!isUnlocked" class="archive-top-safe__logo">
          <text class="archive-top-safe__logo-text">时 光 回 序</text>
        </view>

        <!-- UNLOCKED：日期 meta -->
        <view v-if="isUnlocked && detail && archiveDateText" class="archive-top-safe__meta">
          <view class="archive-top-safe__subline">{{ archiveDateText }}</view>
        </view>

        <!-- 关闭按钮 -->
        <view v-if="!isUnlocked" class="archive-close" :style="closeRailStyle" @tap="closePage">
          <view class="archive-close__icon">
            <view class="archive-close__line archive-close__line--a" />
            <view class="archive-close__line archive-close__line--b" />
          </view>
        </view>
        <view v-if="isUnlocked" class="archive-close archive-close--content" @tap="closePage">
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

        <!-- DRAFT -->
        <view v-if="isDraft" class="archive-intro">
          <view class="archive-intro__copy">这封信还在写作途中，可以继续补完后再封存。</view>
        </view>
        <view v-if="isDraft" class="fallback-panel">
          <PaperContainer radius="xl" class="status-card">
            <view class="panel-title">继续完善后再封存</view>
            <view class="panel-content">这封信仍处于草稿阶段，尚未进入解锁阅读态。</view>
            <view class="panel-time">计划解锁：{{ formatDateTime(detail.unlockAt) }}</view>
          </PaperContainer>
          <PrimaryButton text="继续编辑草稿" @tap="openEditor" />
        </view>

        <!-- SEALED：定稿视觉 -->
        <view v-else-if="isSealed" class="sealed-stage">
          <!-- Archive meta -->
          <view class="sealed-meta">
            <text class="sealed-meta__no">{{ archiveNo }}</text>
            <text class="sealed-meta__season">{{ archiveSeason }}</text>
          </view>

          <!-- 短横线 -->
          <view class="sealed-deco-line" aria-hidden="true" />

          <!-- 信件卡片 -->
          <view class="sealed-card">
            <!-- 左侧朱砂竖线 -->
            <view class="sealed-card__vline" aria-hidden="true" />
            <!-- 右上折角 -->
            <view class="sealed-card__corner" aria-hidden="true" />

            <!-- 卡片 meta 行 -->
            <view class="sealed-card__meta">
              <view class="sealed-card__meta-left">
                <view class="sealed-seal">
                  <text class="sealed-seal__char">封</text>
                </view>
                <text class="sealed-card__tag">过去的你</text>
              </view>
              <view class="sealed-card__location">
                <view class="sealed-card__loc-dot" aria-hidden="true" />
                <text class="sealed-card__loc-text">{{ detail.title || '未命名档案' }}</text>
              </view>
            </view>

            <!-- 引句 -->
            <view class="sealed-quote">
              <text class="sealed-quote__text">"那时的风，似乎比现在要慢一些。"</text>
            </view>

            <!-- 模糊正文 -->
            <view class="sealed-body-wrap">
              <text class="sealed-body">{{ detail.content || '内容已封存，等待解锁后方可阅读。' }}</text>
              <view class="sealed-body__veil" aria-hidden="true" />
            </view>

            <!-- 星形装饰 -->
            <view class="sealed-sparkle" aria-hidden="true">✦</view>
          </view>

          <!-- 倒计时区 -->
          <view class="sealed-lock">
            <view class="sealed-lock__live">
              <view class="sealed-lock__pulse" aria-hidden="true" />
              <text class="sealed-lock__live-text">即将抵达，封印未解</text>
            </view>

            <view class="sealed-countdown">
              <text class="sealed-countdown__label">还 有</text>
              <view class="sealed-countdown__digits">
                <view class="digit-block">
                  <text class="digit-num">{{ countdownH }}</text>
                  <text class="digit-unit">时</text>
                </view>
                <text class="digit-sep">:</text>
                <view class="digit-block">
                  <text class="digit-num">{{ countdownM }}</text>
                  <text class="digit-unit">分</text>
                </view>
                <text class="digit-sep">:</text>
                <view class="digit-block">
                  <text class="digit-num">{{ countdownS }}</text>
                  <text class="digit-unit">秒</text>
                </view>
              </view>
            </view>

            <view class="sealed-deco-line-sm" aria-hidden="true" />
          </view>

          <!-- 留下回应 CTA -->
          <view class="sealed-cta-wrap">
            <view class="sealed-cta" @tap="() => uni.showToast({ title: '解封后方可留下回应', icon: 'none' })">
              <view class="sealed-cta__corner sealed-cta__corner--tl" aria-hidden="true" />
              <view class="sealed-cta__corner sealed-cta__corner--br" aria-hidden="true" />
              <view class="sealed-cta__dot" aria-hidden="true" />
              <text class="sealed-cta__text">留 下 回 应</text>
            </view>
          </view>

          <!-- 底部说明 -->
          <text class="sealed-sub-hint">解封后，过去的你将读到这封信</text>
        </view>

        <!-- UNLOCKED：档案信件阅读页（保持原有结构） -->
        <view v-else-if="isUnlocked" class="archive-intro">
          <view class="archive-intro__copy">{{ unlockMomentText || archiveDateText }}</view>
        </view>
        <view v-if="isUnlocked" class="letter-stage">
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
                    <view class="present-retry" @tap="retryLoadReply"><text>重新加载</text></view>
                  </view>
                </template>
                <template v-else>
                  <view class="present-slot present-slot--submitted">
                    <view class="present-slot__header">
                      <text class="present-slot__label">你曾回应</text>
                      <text v-if="replyResult?.createdAt" class="present-slot__time">{{ formatDateTime(replyResult.createdAt) }}</text>
                    </view>
                    <view class="present-slot__content">{{ replyResult?.content }}</view>
                  </view>
                  <view class="present-action">
                    <view class="present-note">这句回应已经和旧信一起存档。</view>
                  </view>
                </template>
              </template>

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
                  <view class="present-btn" :class="{ 'present-btn--disabled': submittingReply }" @tap="submitReply">
                    <text>{{ submittingReply ? '发送中…' : '留下回应' }}</text>
                  </view>
                </view>
              </template>

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
  --font-reading: 'Noto Serif SC', 'Songti SC', Georgia, serif;
  --font-secondary: 'Noto Sans SC', 'PingFang SC', sans-serif;
  padding-left: 56rpx;
  padding-right: 56rpx;
  padding-bottom: 80rpx;
  background: linear-gradient(170deg, #faf7f2 0%, #f5f0e8 55%, #f0ebe0 100%);
  overflow: hidden;
}

.archive-page--unlocked {
  background: linear-gradient(170deg, #faf7f2 0%, #f5f0e8 55%, #f0ebe0 100%);
}

/* 宣纸底纹 */
.archive-backdrop {
  position: absolute;
  inset: 0;
  z-index: 0;
  background-image:
    radial-gradient(ellipse 80% 50% at 18% 10%, rgba(200, 185, 158, 0.09) 0%, transparent 70%),
    radial-gradient(ellipse 60% 40% at 82% 25%, rgba(185, 168, 140, 0.06) 0%, transparent 65%),
    radial-gradient(ellipse 50% 35% at 50% 45%, rgba(250, 245, 238, 0.18) 0%, transparent 75%);
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
    radial-gradient(120% 130% at 50% 0%, rgba(250, 247, 242, 0.82) 0%, rgba(250, 247, 242, 0) 60%),
    linear-gradient(180deg, rgba(245, 240, 232, 0.92) 0%, rgba(245, 240, 232, 0) 100%);
}

.archive-top-safe__nav {
  position: relative;
}

/* 品牌名（SEALED / DRAFT 态居中） */
.archive-top-safe__logo {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
}

.archive-top-safe__logo-text {
  font-family: 'Noto Serif SC', 'Songti SC', Georgia, serif;
  font-size: 24rpx;
  font-weight: 300;
  letter-spacing: 0.55em;
  color: #9e9890;
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

/* ========== SEALED 定稿视觉 ========== */
.sealed-stage {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0;
}

/* Archive meta */
.sealed-meta {
  text-align: center;
  margin-bottom: 56rpx;
}

.sealed-meta__no {
  display: block;
  font-family: 'Noto Sans SC', 'PingFang SC', sans-serif;
  font-size: 20rpx;
  font-weight: 300;
  letter-spacing: 0.25em;
  color: #c8c2b8;
  text-transform: uppercase;
  margin-bottom: 8rpx;
}

.sealed-meta__season {
  display: block;
  font-family: 'Noto Serif SC', 'Songti SC', Georgia, serif;
  font-size: 26rpx;
  font-weight: 300;
  letter-spacing: 0.08em;
  color: #9e9890;
  font-style: italic;
}

/* 短横线 */
.sealed-deco-line {
  width: 64rpx;
  height: 1rpx;
  background: #c8c2b8;
  margin: 0 auto 64rpx;
}

.sealed-deco-line-sm {
  width: 48rpx;
  height: 1rpx;
  background: #c8c2b8;
  margin: 0 auto;
}

/* 信件卡片 */
.sealed-card {
  position: relative;
  width: 100%;
  background: rgba(252, 249, 244, 0.72);
  border: 1rpx solid rgba(188, 174, 152, 0.28);
  border-radius: 2rpx;
  padding: 56rpx 48rpx 48rpx 64rpx;
  box-shadow:
    0 2rpx 0 rgba(255, 255, 255, 0.6) inset,
    0 4rpx 24rpx rgba(140, 120, 90, 0.06),
    0 2rpx 6rpx rgba(140, 120, 90, 0.04);
  margin-bottom: 56rpx;
}

/* 左侧朱砂竖线 */
.sealed-card__vline {
  position: absolute;
  left: 0;
  top: 40rpx;
  bottom: 40rpx;
  width: 3rpx;
  background: linear-gradient(
    to bottom,
    transparent,
    rgba(181, 53, 42, 0.35) 25%,
    rgba(181, 53, 42, 0.35) 75%,
    transparent
  );
  border-radius: 2rpx;
}

/* 右上折角 */
.sealed-card__corner {
  position: absolute;
  top: 0;
  right: 0;
  width: 28rpx;
  height: 28rpx;
  background: linear-gradient(
    225deg,
    rgba(230, 218, 200, 0.9) 0%,
    rgba(230, 218, 200, 0.9) 48%,
    rgba(252, 249, 244, 0) 50%
  );
  border-left: 1rpx solid rgba(188, 174, 152, 0.22);
  border-bottom: 1rpx solid rgba(188, 174, 152, 0.22);
}

/* 卡片 meta 行 */
.sealed-card__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 40rpx;
}

.sealed-card__meta-left {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

/* 封印章 */
.sealed-seal {
  width: 52rpx;
  height: 52rpx;
  border-radius: 50%;
  border: 2rpx solid #b5352a;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0.75;
  flex-shrink: 0;
}

.sealed-seal__char {
  font-family: 'Noto Serif SC', 'Songti SC', Georgia, serif;
  font-size: 20rpx;
  color: #b5352a;
}

.sealed-card__tag {
  font-family: 'Noto Sans SC', 'PingFang SC', sans-serif;
  font-size: 20rpx;
  font-weight: 300;
  color: #9e9890;
  letter-spacing: 0.1em;
}

.sealed-card__location {
  display: flex;
  align-items: center;
  gap: 8rpx;
}

.sealed-card__loc-dot {
  width: 8rpx;
  height: 8rpx;
  border-radius: 50%;
  background: #c8c2b8;
}

.sealed-card__loc-text {
  font-family: 'Noto Sans SC', 'PingFang SC', sans-serif;
  font-size: 20rpx;
  font-weight: 300;
  color: #c8c2b8;
  letter-spacing: 0.06em;
  max-width: 200rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 引句 */
.sealed-quote {
  margin-bottom: 36rpx;
}

.sealed-quote__text {
  font-family: 'Noto Serif SC', 'Songti SC', Georgia, serif;
  font-size: 40rpx;
  font-weight: 300;
  color: #302e29;
  line-height: 1.6;
  letter-spacing: 0.04em;
}

/* 模糊正文 */
.sealed-body-wrap {
  position: relative;
  margin-bottom: 20rpx;
}

.sealed-body {
  font-family: 'Noto Serif SC', 'Songti SC', Georgia, serif;
  font-size: 28rpx;
  font-weight: 300;
  color: #6b6560;
  line-height: 1.85;
  letter-spacing: 0.03em;
  filter: blur(3px);
}

.sealed-body__veil {
  position: absolute;
  inset: 0;
  background: linear-gradient(to bottom, transparent 0%, rgba(252, 249, 244, 0.82) 80%);
  pointer-events: none;
}

/* 星形装饰 */
.sealed-sparkle {
  text-align: right;
  opacity: 0.35;
  font-size: 32rpx;
  color: #c8c2b8;
}

/* 倒计时区 */
.sealed-lock {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 36rpx;
  margin-bottom: 44rpx;
}

.sealed-lock__live {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.sealed-lock__pulse {
  width: 8rpx;
  height: 8rpx;
  border-radius: 50%;
  background: #b5352a;
  opacity: 0.6;
}

.sealed-lock__live-text {
  font-family: 'Noto Sans SC', 'PingFang SC', sans-serif;
  font-size: 20rpx;
  font-weight: 300;
  color: #9e9890;
  letter-spacing: 0.1em;
}

.sealed-countdown {
  text-align: center;
}

.sealed-countdown__label {
  display: block;
  font-family: 'Noto Sans SC', 'PingFang SC', sans-serif;
  font-size: 20rpx;
  font-weight: 300;
  color: #9e9890;
  letter-spacing: 0.12em;
  margin-bottom: 16rpx;
}

.sealed-countdown__digits {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 4rpx;
}

.digit-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 76rpx;
}

.digit-num {
  font-family: 'Noto Serif SC', 'Songti SC', Georgia, serif;
  font-size: 56rpx;
  font-weight: 300;
  color: #302e29;
  letter-spacing: 0.02em;
  line-height: 1;
}

.digit-unit {
  font-family: 'Noto Sans SC', 'PingFang SC', sans-serif;
  font-size: 18rpx;
  font-weight: 300;
  color: #c8c2b8;
  letter-spacing: 0.1em;
  margin-top: 6rpx;
}

.digit-sep {
  font-family: 'Noto Serif SC', 'Songti SC', Georgia, serif;
  font-size: 44rpx;
  font-weight: 300;
  color: #c8c2b8;
  padding-bottom: 16rpx;
  margin: 0 4rpx;
}

/* 留下回应 CTA */
.sealed-cta-wrap {
  display: flex;
  justify-content: center;
  margin-top: 44rpx;
  margin-bottom: 0;
}

.sealed-cta {
  position: relative;
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 26rpx 72rpx;
  font-family: 'Noto Serif SC', 'Songti SC', Georgia, serif;
  font-size: 28rpx;
  font-weight: 400;
  letter-spacing: 0.18em;
  color: #302e29;
  background: transparent;
  border: 1rpx solid #c8c2b8;
  border-radius: 4rpx;
}

.sealed-cta__corner {
  position: absolute;
  width: 12rpx;
  height: 12rpx;
  border-color: #9e9890;
  border-style: solid;
}

.sealed-cta__corner--tl {
  top: -2rpx;
  left: -2rpx;
  border-width: 2rpx 0 0 2rpx;
}

.sealed-cta__corner--br {
  bottom: -2rpx;
  right: -2rpx;
  border-width: 0 2rpx 2rpx 0;
}

.sealed-cta__dot {
  width: 10rpx;
  height: 10rpx;
  border-radius: 50%;
  background: #b5352a;
  opacity: 0.7;
  flex-shrink: 0;
}

.sealed-cta__text {
  font-family: 'Noto Serif SC', 'Songti SC', Georgia, serif;
  font-size: 28rpx;
  font-weight: 400;
  letter-spacing: 0.18em;
  color: #302e29;
}

/* 底部说明 */
.sealed-sub-hint {
  display: block;
  text-align: center;
  font-family: 'Noto Sans SC', 'PingFang SC', sans-serif;
  font-size: 20rpx;
  font-weight: 300;
  color: #c8c2b8;
  letter-spacing: 0.08em;
  margin-top: 32rpx;
}
</style>
