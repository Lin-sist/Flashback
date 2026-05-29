<script setup lang="ts">
import { computed, onUnmounted, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import PaperContainer from '../../components/common/PaperContainer.vue'
import PrimaryButton from '../../components/common/PrimaryButton.vue'
import EmptyState from '../../components/common/EmptyState.vue'
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
const showReplySheet = ref(false)

// ── 倒计时 ──
const countdownH = ref('--')
const countdownM = ref('--')
const countdownS = ref('--')
let countdownTimer: ReturnType<typeof setInterval> | null = null

const startCountdown = (unlockAt: string | number | null | undefined) => {
  if (countdownTimer) clearInterval(countdownTimer)
  if (!unlockAt) return
  const target = new Date(unlockAt as string | number).getTime()
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

// ── Archive meta ──
const archiveNo = computed(() => {
  if (!detail.value?.id) return ''
  return `Archive No. ${String(detail.value.id).padStart(3, '0')}`
})

// 封存页用英文格式；解锁页用中文格式
const archiveNoCN = computed(() => {
  if (!detail.value?.id) return ''
  const cn = '〇一二三四五六七八九'
  const cnId = String(detail.value.id).split('').map(n => cn[Number(n)]).join('')
  return `存档第 ${cnId} 号`
})

const archiveSeason = computed(() => {
  if (!detail.value?.createdAt) return ''
  const d = new Date(detail.value.createdAt as string)
  const seasons = ['Winter','Winter','Spring','Spring','Spring','Summer','Summer','Summer','Autumn','Autumn','Autumn','Winter']
  return `${seasons[d.getMonth()]}, ${d.getFullYear()}`
})

// 中文季节格式（回看.html 用）
const archiveSeasonCN = computed(() => {
  if (!detail.value?.createdAt) return ''
  const d = new Date(detail.value.createdAt as string | number)
  const cn = '〇一二三四五六七八九'
  const year = String(d.getFullYear()).split('').map(n => cn[Number(n)]).join('')
  const seasons = ['冬','冬','春','春','春','夏','夏','夏','秋','秋','秋','冬']
  return `${year}年 · ${seasons[d.getMonth()]}季`
})

// 位置（若记录有 location 字段则展示）
const archiveLocation = computed(() => {
  if (!detail.value) return ''
  const d = detail.value as Record<string, unknown>
  return typeof d.location === 'string' ? d.location : ''
})

// "过去的你，写于X年前"
const archiveWrittenText = computed(() => {
  if (!detail.value?.createdAt) return '过去的你写下'
  const d = new Date(detail.value.createdAt as string | number)
  const yearsAgo = new Date().getFullYear() - d.getFullYear()
  if (yearsAgo <= 0) return '过去的你，刚刚写下'
  return `过去的你，写于${yearsAgo}年前`
})

// 解锁大引句：优先使用标题，否则取正文首句
const unlockQuote = computed(() => {
  if (!detail.value) return ''
  if (detail.value.title) return `"${detail.value.title}"`
  const content = String(detail.value.content || '').trim()
  const first = content.split(/[。！？\n]/)[0]
  return first ? `"${first.slice(0, 40)}"` : ''
})

const { cssVars, navBarHeight, navBarTotalHeight, rightSafeWidth, statusBarHeight } =
  useWechatNavMetrics()

const pageStyle = computed(() => ({ ...cssVars.value }))

const topSafeStyle = computed(() => ({
  minHeight: `calc(${navBarTotalHeight.value}px + 24rpx)`,
  paddingTop: `${statusBarHeight.value}px`,
}))

const topNavStyle = computed(() => ({
  height: `${navBarHeight.value}px`,
}))

const closeRailStyle = computed(() => ({
  left: '56rpx',
}))

const detail = computed(() => {
  if (!currentRecordId.value || !recordStore.detail) return null
  return Number(recordStore.detail.id) === currentRecordId.value ? recordStore.detail : null
})

const hasDetailError = computed(() => detailErrorState.value !== 'NONE')

const detailErrorText = computed(() => {
  if (detailErrorState.value === 'INVALID_ID') return '记录地址不完整，请返回后重试'
  if (detailErrorState.value === 'NOT_FOUND') return '这条记录可能已不存在或暂时不可见'
  if (detailErrorState.value === 'LOAD_FAILED') return '网络有点慢，记录详情暂时没加载出来'
  return '记录暂时不可用'
})

const isDraft    = computed(() => detail.value?.status === RecordStatus.DRAFT)
const isSealed   = computed(() => detail.value?.status === RecordStatus.SEALED)
const isUnlocked = computed(() => detail.value?.status === RecordStatus.UNLOCKED)
const canSubmitReply   = computed(() => Boolean(detail.value?.canReply && !detail.value?.hasReply))
const hasSubmittedReply = computed(() => Boolean(detail.value?.hasReply))

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
  if (source.value === 'timeline') { uni.switchTab({ url: '/pages/timeline/index' }); return }
  if (source.value === 'archive')  { uni.navigateTo({ url: '/pages/record-list/index' }); return }
  uni.switchTab({ url: '/pages/home/index' })
}

const closePage = () => {
  uni.navigateBack({
    delta: 1,
    fail: () => { fallbackBySource() },
  })
}

const resolveSource = (value: unknown): EditorSource | null => {
  if (value === 'archive' || value === 'timeline' || value === 'home') return value
  return null
}

const inferSourceFromPrevPage = (): EditorSource => {
  const pages = getCurrentPages()
  if (pages.length < 2) return 'home'
  const prevRoute = pages[pages.length - 2]?.route
  if (prevRoute === 'pages/record-list/index') return 'archive'
  if (prevRoute === 'pages/timeline/index') return 'timeline'
  return 'home'
}

const resolveDetailErrorState = (error: unknown): 'NOT_FOUND' | 'LOAD_FAILED' => {
  const message = toUserMessage(error).toLowerCase()
  if (message.includes('not found') || message.includes('不存在')) return 'NOT_FOUND'
  return 'LOAD_FAILED'
}

const openEditor = () => {
  if (!detail.value) return
  uni.navigateTo({ url: `/pages/record-editor/index?id=${detail.value.id}&source=${source.value}` })
}

const loadReplyResult = async (recordId: number, hasReply: boolean) => {
  if (!hasReply) { replyResult.value = null; replyLoadFailed.value = false; return }
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
  if (!detail.value?.id || !detail.value.hasReply) return
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
  if (!currentRecordId.value) { closePage(); return }
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
    showReplySheet.value = false
    await refreshUnlockState(detail.value.id)
  } catch (error) {
    uni.showToast({ title: toUserMessage(error), icon: 'none' })
  } finally {
    submittingReply.value = false
  }
}

const openReplySheet = () => {
  if (!canSubmitReply.value) {
    uni.showToast({ title: '当前无法留下回应', icon: 'none' })
    return
  }
  showReplySheet.value = true
}

const closeReplySheet = () => {
  showReplySheet.value = false
}

const addToTimeline = () => {
  uni.showToast({ title: '已记录到时光轴', icon: 'none' })
}

const onSealedCtaTap = () => {
  uni.showToast({ title: '解封后方可留下回应', icon: 'none' })
}

onLoad(async (query) => {
  if (!ensureLogin()) return
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
  if (Number.isNaN(id)) { detailErrorState.value = 'INVALID_ID'; return }
  currentRecordId.value = id
  await loadDetail(id)
})
</script>

<template>
  <view class="archive-page" :style="pageStyle">
    <!-- 宣纸底纹 -->
    <view class="archive-backdrop" />

    <!-- 顶部安全区：品牌名 + 关闭按钮 -->
    <view class="archive-top-safe" :style="topSafeStyle">
      <view class="archive-top-safe__mist" />
      <view class="archive-top-safe__nav" :style="topNavStyle">
        <!-- 品牌名居中（所有状态统一显示） -->
        <view class="archive-top-safe__logo">
          <text class="archive-top-safe__logo-text">时 光 回 序</text>
        </view>
        <!-- 关闭按钮 -->
        <view class="archive-close" :style="closeRailStyle" @tap="closePage">
          <view class="archive-close__icon">
            <view class="archive-close__line archive-close__line--a" />
            <view class="archive-close__line archive-close__line--b" />
          </view>
        </view>
      </view>
    </view>

    <!-- 主内容区 -->
    <view class="archive-main">

      <!-- Loading / Error -->
      <view v-if="detailLoading" class="state-wrap">
        <EmptyState text="正在加载记录详情..." />
        <PrimaryButton text="返回上一页" ghost @tap="closePage" />
      </view>

      <view v-else-if="hasDetailError" class="state-wrap">
        <EmptyState :text="detailErrorText" />
        <PrimaryButton
          :text="detailErrorState === 'INVALID_ID' ? '返回上一页' : '重试加载'"
          ghost
          @tap="detailErrorState === 'INVALID_ID' ? closePage() : retryLoadDetail()"
        />
      </view>

      <!-- 有详情内容 -->
      <view v-else-if="detail" class="archive-stage">

        <!-- ══════ DRAFT ══════ -->
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

        <!-- ══════ SEALED：封存页（封存.html 结构） ══════ -->
        <view v-else-if="isSealed" class="sealed-hero">

          <!-- Archive meta（居中） -->
          <view class="sealed-meta">
            <text class="sealed-meta__no">{{ archiveNo }}</text>
            <text class="sealed-meta__season">{{ archiveSeason }}</text>
          </view>

          <!-- 装饰横线 -->
          <view class="sealed-deco-line" />

          <!-- 信件卡片 -->
          <view class="sealed-card">
            <view class="sealed-card__vline" />
            <view class="sealed-card__corner" />

            <!-- 卡片 meta 行 -->
            <view class="sealed-card__meta">
              <view class="sealed-card__meta-left">
                <view class="sealed-seal">
                  <text class="sealed-seal__char">封</text>
                </view>
                <text class="sealed-card__tag">过去的你</text>
              </view>
              <view class="sealed-card__location">
                <view class="sealed-card__loc-dot" />
                <text class="sealed-card__loc-text">{{ archiveLocation || detail.title || '未命名档案' }}</text>
              </view>
            </view>

            <!-- 引句（使用标题或正文首句） -->
            <view class="sealed-quote">
              <text class="sealed-quote__text">{{ unlockQuote || '"时间，是最温柔的旅人。"' }}</text>
            </view>

            <!-- 模糊正文 -->
            <view class="sealed-body-wrap">
              <text class="sealed-body">{{ detail.content || '内容已封存，等待解锁后方可阅读。' }}</text>
              <view class="sealed-body__veil" />
            </view>

            <!-- 星形装饰 -->
            <view class="sealed-sparkle">✦</view>
          </view>

          <!-- 倒计时区 -->
          <view class="sealed-lock">
            <view class="sealed-lock__live">
              <view class="sealed-lock__pulse" />
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
            <view class="sealed-deco-line-sm" />
          </view>

          <!-- 留下回应 CTA（封存中禁用） -->
          <view class="sealed-cta-wrap">
            <view class="sealed-cta" @tap="onSealedCtaTap">
              <view class="sealed-cta__corner sealed-cta__corner--tl" />
              <view class="sealed-cta__corner sealed-cta__corner--br" />
              <view class="sealed-cta__dot" />
              <text class="sealed-cta__text">留 下 回 应</text>
            </view>
          </view>

          <text class="sealed-sub-hint">解封后，过去的你将读到这封信</text>
        </view>

        <!-- ══════ UNLOCKED：时间回看（回看.html 结构） ══════ -->
        <view v-else-if="isUnlocked" class="unlock-hero">

          <!-- 存档元信息行 -->
          <view class="unlock-archive">
            <text class="unlock-archive-no">{{ archiveNoCN }}</text>
            <view v-if="archiveLocation" class="unlock-archive-loc">
              <view class="unlock-loc-dot" />
              <text class="unlock-loc-text">{{ archiveLocation }}</text>
            </view>
          </view>

          <!-- 季节 -->
          <text class="unlock-season">{{ archiveSeasonCN }}</text>

          <!-- 装饰横线 -->
          <view class="unlock-deco" />

          <!-- 大引句 -->
          <text class="unlock-quote">{{ unlockQuote }}</text>

          <!-- 印章行 -->
          <view class="unlock-seal-row">
            <view class="unlock-seal">
              <text class="unlock-seal-char">阅</text>
            </view>
            <text class="unlock-seal-label">{{ archiveWrittenText }}</text>
          </view>

          <!-- 信件卡片（可读） -->
          <view class="unlock-card">
            <view class="unlock-card-vline" />
            <view class="unlock-card-corner" />
            <view class="unlock-card-body">
              <text class="unlock-card-text">{{ detail.content }}</text>
            </view>
            <text class="unlock-sparkle">✦</text>
          </view>

          <!-- 操作区 -->
          <view class="unlock-actions">
            <text class="unlock-reply-hint">此刻，想对当时的自己说句什么…</text>

            <!-- 已提交回应 -->
            <template v-if="hasSubmittedReply">
              <view class="unlock-replied-slot">
                <view v-if="replyLoading" class="unlock-replied-loading">
                  <text class="unlock-replied-placeholder">正在载入你留下的那句话…</text>
                </view>
                <view v-else-if="replyLoadFailed" class="unlock-replied-fail">
                  <text class="unlock-replied-placeholder">回应内容暂时加载失败</text>
                  <text class="unlock-replied-retry" @tap="retryLoadReply">重新加载</text>
                </view>
                <view v-else class="unlock-replied-content">
                  <text class="unlock-replied-label">你曾回应</text>
                  <text class="unlock-replied-text">{{ replyResult?.content }}</text>
                </view>
              </view>
            </template>

            <!-- 可留回应 -->
            <view v-else-if="canSubmitReply" class="unlock-cta" @tap="openReplySheet">
              <view class="unlock-cta-corner unlock-cta-corner--tl" />
              <view class="unlock-cta-corner unlock-cta-corner--br" />
              <view class="unlock-cta-dot" />
              <text class="unlock-cta-text">留 下 回 应</text>
            </view>

            <!-- 收入时光轴 -->
            <view class="unlock-sec-link" @tap="addToTimeline">
              <text class="unlock-sec-link-text">收入时光轴</text>
            </view>
          </view>
        </view>

      </view>

      <!-- 无 detail 兜底 -->
      <view v-else class="state-wrap">
        <EmptyState text="记录暂时不可用" />
        <PrimaryButton text="重试加载" ghost @tap="retryLoadDetail" />
      </view>

    </view>

    <!-- 回应浮层（UNLOCKED 可回应状态） -->
    <view
      v-if="isUnlocked && canSubmitReply"
      class="reply-overlay"
      :class="{ 'reply-overlay--open': showReplySheet }"
      @tap.stop="closeReplySheet"
    >
      <view class="reply-sheet" @tap.stop>
        <text class="reply-sheet-label">回 应</text>
        <textarea
          class="reply-textarea"
          v-model="replyContent"
          placeholder="此刻的你，想说…"
          placeholder-class="reply-placeholder"
          :disabled="submittingReply"
          auto-height
        />
        <view class="reply-actions-row">
          <view class="reply-cancel" @tap="closeReplySheet">
            <text>取 消</text>
          </view>
          <view
            class="reply-send"
            :class="{ 'reply-send--disabled': submittingReply }"
            @tap="submitReply"
          >
            <view class="reply-send-corner reply-send-corner--tl" />
            <view class="reply-send-corner reply-send-corner--br" />
            <view class="unlock-cta-dot" />
            <text>{{ submittingReply ? '寄出中…' : '寄 出' }}</text>
          </view>
        </view>
      </view>
    </view>

  </view>
</template>

<style scoped>
/* ═══════════════════════════════════════
   设计令牌 & 页面底层
═══════════════════════════════════════ */
.archive-page {
  position: relative;
  min-height: 100vh;
  --font-reading:    'Noto Serif SC', 'Songti SC', Georgia, serif;
  --font-secondary:  'Noto Sans SC', 'PingFang SC', sans-serif;
  --ink:             #302e29;
  --ink-mid:         #6b6560;
  --ink-light:       #9e9890;
  --ink-faint:       #c8c2b8;
  --vermilion:       #b5352a;
  padding-left: 56rpx;
  padding-right: 56rpx;
  padding-bottom: 80rpx;
  background: linear-gradient(170deg, #faf7f2 0%, #f5f0e8 55%, #f0ebe0 100%);
  overflow: hidden;
}

/* 宣纸底纹 */
.archive-backdrop {
  position: absolute;
  inset: 0;
  z-index: 0;
  background-image:
    url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='500' height='500'%3E%3Cfilter id='f'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.55' numOctaves='6' stitchTiles='stitch'/%3E%3CfeColorMatrix type='saturate' values='0.15'/%3E%3C/filter%3E%3Crect width='500' height='500' filter='url(%23f)' opacity='0.055'/%3E%3C/svg%3E"),
    radial-gradient(ellipse 80% 50% at 18% 10%, rgba(200,185,158,0.09) 0%, transparent 70%),
    radial-gradient(ellipse 60% 40% at 82% 25%, rgba(185,168,140,0.06) 0%, transparent 65%),
    radial-gradient(ellipse 50% 35% at 50% 45%, rgba(250,245,238,0.18) 0%, transparent 75%);
  pointer-events: none;
}

.archive-top-safe,
.archive-main,
.state-wrap,
.archive-stage {
  position: relative;
  z-index: 1;
}

.archive-top-safe { z-index: 3; }

/* ═══════════════════════════════════════
   顶部安全区
═══════════════════════════════════════ */
.archive-top-safe__mist {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(120% 130% at 50% 0%, rgba(250,247,242,0.82) 0%, rgba(250,247,242,0) 60%),
    linear-gradient(180deg, rgba(245,240,232,0.92) 0%, rgba(245,240,232,0) 100%);
}

.archive-top-safe__nav { position: relative; }

/* 品牌名居中 */
.archive-top-safe__logo {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
}

.archive-top-safe__logo-text {
  font-family: var(--font-reading);
  font-size: 24rpx;
  font-weight: 300;
  letter-spacing: 0.55em;
  color: var(--ink-light);
}

/* 关闭按钮 */
.archive-close {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 56rpx;
  height: 56rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.34);
  border: 1rpx solid rgba(138, 149, 160, 0.12);
}

.archive-close__icon {
  position: relative;
  width: 28rpx;
  height: 28rpx;
}

.archive-close__line {
  position: absolute;
  left: 0;
  top: 50%;
  width: 100%;
  height: 1rpx;
  border-radius: 999rpx;
  background: var(--ink-faint);
}

.archive-close__line--a { transform: translateY(-50%) rotate(45deg); }
.archive-close__line--b { transform: translateY(-50%) rotate(-45deg); }

/* ═══════════════════════════════════════
   主内容
═══════════════════════════════════════ */
.archive-main { margin-top: 16rpx; }

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

/* ═══════════════════════════════════════
   DRAFT 兜底面板
═══════════════════════════════════════ */
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

.fallback-panel {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.status-card { box-shadow: 0 12rpx 32rpx rgba(26, 26, 26, 0.05); }

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

/* ═══════════════════════════════════════
   SEALED：封存页（封存.html）
═══════════════════════════════════════ */
.sealed-hero {
  display: flex;
  flex-direction: column;
  align-items: center;
  /* 尽量撑满剩余视口，内容整体居中 */
  min-height: calc(100vh - 260rpx);
  justify-content: center;
  padding: 48rpx 0 32rpx;
  gap: 0;
}

/* Archive meta */
.sealed-meta {
  text-align: center;
  margin-bottom: 56rpx;
}

.sealed-meta__no {
  display: block;
  font-family: var(--font-secondary);
  font-size: 20rpx;
  font-weight: 300;
  letter-spacing: 0.25em;
  color: var(--ink-faint);
  text-transform: uppercase;
  margin-bottom: 8rpx;
}

.sealed-meta__season {
  display: block;
  font-family: var(--font-reading);
  font-size: 26rpx;
  font-weight: 300;
  letter-spacing: 0.08em;
  color: var(--ink-light);
  font-style: italic;
}

/* 装饰横线 */
.sealed-deco-line {
  width: 64rpx;
  height: 1rpx;
  background: var(--ink-faint);
  margin: 0 auto 64rpx;
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

.sealed-card__vline {
  position: absolute;
  left: 0;
  top: 40rpx;
  bottom: 40rpx;
  width: 3rpx;
  background: linear-gradient(to bottom, transparent, rgba(181,53,42,0.35) 25%, rgba(181,53,42,0.35) 75%, transparent);
  border-radius: 2rpx;
}

.sealed-card__corner {
  position: absolute;
  top: 0;
  right: 0;
  width: 28rpx;
  height: 28rpx;
  background: linear-gradient(225deg, rgba(230,218,200,0.9) 0%, rgba(230,218,200,0.9) 48%, rgba(252,249,244,0) 50%);
  border-left: 1rpx solid rgba(188,174,152,0.22);
  border-bottom: 1rpx solid rgba(188,174,152,0.22);
}

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

.sealed-seal {
  width: 52rpx;
  height: 52rpx;
  border-radius: 50%;
  border: 2rpx solid var(--vermilion);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0.75;
  flex-shrink: 0;
}

.sealed-seal__char {
  font-family: var(--font-reading);
  font-size: 20rpx;
  color: var(--vermilion);
}

.sealed-card__tag {
  font-family: var(--font-secondary);
  font-size: 20rpx;
  font-weight: 300;
  color: var(--ink-light);
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
  background: var(--ink-faint);
}

.sealed-card__loc-text {
  font-family: var(--font-secondary);
  font-size: 20rpx;
  font-weight: 300;
  color: var(--ink-faint);
  letter-spacing: 0.06em;
  max-width: 200rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sealed-quote { margin-bottom: 36rpx; }

.sealed-quote__text {
  font-family: var(--font-reading);
  font-size: 40rpx;
  font-weight: 300;
  color: var(--ink);
  line-height: 1.6;
  letter-spacing: 0.04em;
}

.sealed-body-wrap {
  position: relative;
  margin-bottom: 20rpx;
}

.sealed-body {
  font-family: var(--font-reading);
  font-size: 28rpx;
  font-weight: 300;
  color: var(--ink-mid);
  line-height: 1.85;
  letter-spacing: 0.03em;
  filter: blur(3px);
}

.sealed-body__veil {
  position: absolute;
  inset: 0;
  background: linear-gradient(to bottom, transparent 0%, rgba(252,249,244,0.82) 80%);
  pointer-events: none;
}

.sealed-sparkle {
  text-align: right;
  opacity: 0.35;
  font-size: 32rpx;
  color: var(--ink-faint);
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
  background: var(--vermilion);
  opacity: 0.6;
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 0.3; transform: scale(0.8); }
  50%       { opacity: 1;   transform: scale(1.2); }
}

.sealed-lock__live-text {
  font-family: var(--font-secondary);
  font-size: 20rpx;
  font-weight: 300;
  color: var(--ink-light);
  letter-spacing: 0.1em;
}

.sealed-countdown { text-align: center; }

.sealed-countdown__label {
  display: block;
  font-family: var(--font-secondary);
  font-size: 20rpx;
  font-weight: 300;
  color: var(--ink-light);
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
  font-family: var(--font-reading);
  font-size: 56rpx;
  font-weight: 300;
  color: var(--ink);
  letter-spacing: 0.02em;
  line-height: 1;
}

.digit-unit {
  font-family: var(--font-secondary);
  font-size: 18rpx;
  font-weight: 300;
  color: var(--ink-faint);
  letter-spacing: 0.1em;
  margin-top: 6rpx;
}

.digit-sep {
  font-family: var(--font-reading);
  font-size: 44rpx;
  font-weight: 300;
  color: var(--ink-faint);
  padding-bottom: 16rpx;
  margin: 0 4rpx;
}

.sealed-deco-line-sm {
  width: 48rpx;
  height: 1rpx;
  background: var(--ink-faint);
  margin: 0 auto;
}

/* CTA */
.sealed-cta-wrap {
  display: flex;
  justify-content: center;
  margin-bottom: 32rpx;
}

.sealed-cta {
  position: relative;
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 26rpx 72rpx;
  background: transparent;
  border: 1rpx solid var(--ink-faint);
  border-radius: 4rpx;
}

.sealed-cta__corner {
  position: absolute;
  width: 12rpx;
  height: 12rpx;
  border-color: var(--ink-light);
  border-style: solid;
}

.sealed-cta__corner--tl { top: -2rpx; left: -2rpx; border-width: 2rpx 0 0 2rpx; }
.sealed-cta__corner--br { bottom: -2rpx; right: -2rpx; border-width: 0 2rpx 2rpx 0; }

.sealed-cta__dot {
  width: 10rpx;
  height: 10rpx;
  border-radius: 50%;
  background: var(--vermilion);
  opacity: 0.7;
  flex-shrink: 0;
}

.sealed-cta__text {
  font-family: var(--font-reading);
  font-size: 28rpx;
  font-weight: 400;
  letter-spacing: 0.18em;
  color: var(--ink);
}

.sealed-sub-hint {
  display: block;
  text-align: center;
  font-family: var(--font-secondary);
  font-size: 20rpx;
  font-weight: 300;
  color: var(--ink-faint);
  letter-spacing: 0.08em;
}

/* ═══════════════════════════════════════
   UNLOCKED：时间回看（回看.html）
═══════════════════════════════════════ */
.unlock-hero {
  display: flex;
  flex-direction: column;
  padding-top: 56rpx;
}

/* 存档元信息行 */
.unlock-archive {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 48rpx;
}

.unlock-archive-no {
  font-family: var(--font-secondary);
  font-size: 20rpx;
  font-weight: 300;
  letter-spacing: 0.12em;
  color: var(--ink-faint);
}

.unlock-archive-loc {
  display: flex;
  align-items: center;
  gap: 10rpx;
}

.unlock-loc-dot {
  width: 6rpx;
  height: 6rpx;
  border-radius: 50%;
  background: var(--vermilion);
  opacity: 0.55;
}

.unlock-loc-text {
  font-family: var(--font-secondary);
  font-size: 20rpx;
  font-weight: 300;
  letter-spacing: 0.08em;
  color: var(--ink-faint);
}

/* 季节 */
.unlock-season {
  font-family: var(--font-reading);
  font-size: 24rpx;
  font-weight: 300;
  letter-spacing: 0.15em;
  color: var(--ink-light);
  font-style: italic;
  text-align: center;
  margin-bottom: 20rpx;
}

/* 装饰横线 */
.unlock-deco {
  width: 64rpx;
  height: 1rpx;
  background: var(--ink-faint);
  margin: 0 auto 56rpx;
}

/* 大引句 */
.unlock-quote {
  font-family: var(--font-reading);
  font-size: 44rpx;
  font-weight: 300;
  color: var(--ink);
  line-height: 1.65;
  letter-spacing: 0.05em;
  text-align: center;
  margin-bottom: 56rpx;
}

/* 印章行 */
.unlock-seal-row {
  display: flex;
  align-items: center;
  gap: 20rpx;
  margin-bottom: 36rpx;
}

.unlock-seal {
  width: 52rpx;
  height: 52rpx;
  border-radius: 50%;
  border: 2rpx solid var(--vermilion);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0.75;
  flex-shrink: 0;
}

.unlock-seal-char {
  font-family: var(--font-reading);
  font-size: 20rpx;
  color: var(--vermilion);
}

.unlock-seal-label {
  font-family: var(--font-secondary);
  font-size: 20rpx;
  font-weight: 300;
  color: var(--ink-light);
  letter-spacing: 0.1em;
}

/* 信件卡片（可读） */
.unlock-card {
  position: relative;
  background: rgba(252, 249, 244, 0.72);
  border: 1rpx solid rgba(188, 174, 152, 0.28);
  border-radius: 2rpx;
  padding: 44rpx 44rpx 40rpx 56rpx;
  box-shadow:
    0 2rpx 0 rgba(255, 255, 255, 0.6) inset,
    0 4rpx 24rpx rgba(140, 120, 90, 0.06),
    0 2rpx 6rpx rgba(140, 120, 90, 0.04);
}

.unlock-card-vline {
  position: absolute;
  left: 0;
  top: 40rpx;
  bottom: 40rpx;
  width: 3rpx;
  background: linear-gradient(to bottom, transparent, rgba(181,53,42,0.35) 25%, rgba(181,53,42,0.35) 75%, transparent);
  border-radius: 2rpx;
}

.unlock-card-corner {
  position: absolute;
  top: 0;
  right: 0;
  width: 28rpx;
  height: 28rpx;
  background: linear-gradient(225deg, rgba(230,218,200,0.9) 0%, rgba(230,218,200,0.9) 48%, rgba(252,249,244,0) 50%);
  border-left: 1rpx solid rgba(188,174,152,0.22);
  border-bottom: 1rpx solid rgba(188,174,152,0.22);
}

.unlock-card-body { margin-bottom: 28rpx; }

.unlock-card-text {
  font-family: var(--font-reading);
  font-size: 27rpx;
  font-weight: 300;
  color: var(--ink-mid);
  line-height: 1.95;
  letter-spacing: 0.03em;
  white-space: pre-wrap;
  word-break: break-word;
}

.unlock-sparkle {
  display: block;
  text-align: right;
  opacity: 0.35;
  font-size: 36rpx;
  color: var(--ink-faint);
}

/* 操作区 */
.unlock-actions {
  margin-top: 64rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 32rpx;
  padding-bottom: 40rpx;
}

.unlock-reply-hint {
  font-family: var(--font-secondary);
  font-size: 20rpx;
  font-weight: 300;
  color: var(--ink-faint);
  letter-spacing: 0.1em;
  text-align: center;
}

/* 已提交回应 */
.unlock-replied-slot {
  width: 100%;
  padding: 28rpx 36rpx;
  background: rgba(252, 249, 244, 0.72);
  border: 1rpx solid rgba(188, 174, 152, 0.28);
  border-radius: 2rpx;
}

.unlock-replied-placeholder {
  font-family: var(--font-secondary);
  font-size: 24rpx;
  color: var(--ink-faint);
}

.unlock-replied-retry {
  font-family: var(--font-secondary);
  font-size: 24rpx;
  color: var(--vermilion);
  opacity: 0.8;
  margin-top: 16rpx;
  display: block;
}

.unlock-replied-fail,
.unlock-replied-loading {
  display: flex;
  flex-direction: column;
}

.unlock-replied-label {
  display: block;
  font-family: var(--font-secondary);
  font-size: 20rpx;
  color: var(--ink-faint);
  letter-spacing: 0.1em;
  margin-bottom: 16rpx;
}

.unlock-replied-text {
  font-family: var(--font-reading);
  font-size: 28rpx;
  color: var(--ink-mid);
  line-height: 1.8;
}

/* 留下回应 CTA */
.unlock-cta {
  position: relative;
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 26rpx 80rpx;
  background: transparent;
  border: 1rpx solid var(--ink-faint);
  border-radius: 4rpx;
}

.unlock-cta-corner {
  position: absolute;
  width: 12rpx;
  height: 12rpx;
  border-color: var(--ink-light);
  border-style: solid;
}

.unlock-cta-corner--tl { top: -2rpx; left: -2rpx; border-width: 2rpx 0 0 2rpx; }
.unlock-cta-corner--br { bottom: -2rpx; right: -2rpx; border-width: 0 2rpx 2rpx 0; }

.unlock-cta-dot {
  width: 10rpx;
  height: 10rpx;
  border-radius: 50%;
  background: var(--vermilion);
  opacity: 0.7;
  flex-shrink: 0;
}

.unlock-cta-text {
  font-family: var(--font-reading);
  font-size: 28rpx;
  font-weight: 400;
  letter-spacing: 0.18em;
  color: var(--ink);
}

/* 收入时光轴 */
.unlock-sec-link-text {
  font-family: var(--font-reading);
  font-size: 24rpx;
  font-weight: 300;
  letter-spacing: 0.12em;
  color: var(--ink-faint);
}

/* ═══════════════════════════════════════
   回应浮层（bottom sheet）
═══════════════════════════════════════ */
.reply-overlay {
  position: fixed;
  inset: 0;
  z-index: 100;
  background: rgba(240, 235, 226, 0.88);
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.4s ease;
}

.reply-overlay--open {
  opacity: 1;
  pointer-events: all;
}

.reply-sheet {
  background: rgba(252, 249, 244, 0.96);
  border-top: 1rpx solid rgba(188, 174, 152, 0.35);
  padding: 48rpx 56rpx 96rpx;
}

.reply-sheet-label {
  display: block;
  font-family: var(--font-secondary);
  font-size: 20rpx;
  font-weight: 300;
  color: var(--ink-light);
  letter-spacing: 0.15em;
  margin-bottom: 28rpx;
}

.reply-textarea {
  width: 100%;
  min-height: 240rpx;
  background: transparent;
  border-bottom: 1rpx solid var(--ink-faint);
  font-family: var(--font-reading);
  font-size: 28rpx;
  font-weight: 300;
  color: var(--ink);
  line-height: 1.85;
  letter-spacing: 0.03em;
  padding-bottom: 24rpx;
}

.reply-placeholder {
  color: var(--ink-faint);
  font-family: var(--font-reading);
  font-size: 28rpx;
}

.reply-actions-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 36rpx;
}

.reply-cancel {
  font-family: var(--font-reading);
  font-size: 24rpx;
  font-weight: 300;
  color: var(--ink-faint);
  letter-spacing: 0.1em;
  padding: 8rpx 0;
}

.reply-send {
  position: relative;
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 20rpx 56rpx;
  background: transparent;
  border: 1rpx solid var(--ink-faint);
  border-radius: 4rpx;
}

.reply-send-corner {
  position: absolute;
  width: 10rpx;
  height: 10rpx;
  border-color: var(--ink-light);
  border-style: solid;
}

.reply-send-corner--tl { top: -2rpx; left: -2rpx; border-width: 2rpx 0 0 2rpx; }
.reply-send-corner--br { bottom: -2rpx; right: -2rpx; border-width: 0 2rpx 2rpx 0; }

.reply-send--disabled { opacity: 0.6; }
</style>
