<script setup lang="ts">
import { computed, onUnmounted, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import PaperContainer from '../../components/common/PaperContainer.vue'
import PrimaryButton from '../../components/common/PrimaryButton.vue'
import EmptyState from '../../components/common/EmptyState.vue'
import PreviewModeNotice from '../../components/common/PreviewModeNotice.vue'
import { useWechatNavMetrics } from '../../composables/useWechatNavMetrics'
import ReadOnlyRecordMedia from './components/ReadOnlyRecordMedia.vue'
import ReviewChatSheet from './components/ReviewChatSheet.vue'
import { hasPreviewSession, showPreviewReadonlyToast } from '../../features/preview/preview-session'
import { agentService, dataOwnershipService, recordService, replyService, timeChapterService, type AgentSession } from '../../services'
import { useRecordStore, useTimeChapterStore } from '../../stores'
import { RecordReminderStatus, RecordStatus, ReplyType, type ReplyVO, type TimeChapterSummaryVO } from '../../types'
import { formatDateTime, getToken, hasAuthenticatedSession, toUserMessage } from '../../utils'

type EditorSource = 'home' | 'archive' | 'timeline'

const recordStore = useRecordStore()
const chapterStore = useTimeChapterStore()
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
const laterReflectionDraft = ref('')
const submittingLaterReflection = ref(false)
const deletingRecord = ref(false)
const memoryExcluded = ref(false)
const memoryNoteDraft = ref('')
const savingMemoryPolicy = ref(false)
const switchingReviewMemory = ref(false)
const chapterActionLoading = ref(false)
const sealedOptionsOpen = ref(false)
const reviewOptionsOpen = ref(false)
const showMemoryNoteSheet = ref(false)
const memorySheetNote = ref('')
const memorySheetExcluded = ref(false)

// C3b 友人回看对话状态。与回应浮层互斥（见 openReviewChat / openReplySheet）。
const showReviewChat = ref(false)
const reviewSession = ref<AgentSession | null>(null)
const reviewLoading = ref(false)
const reviewSending = ref(false)
const reviewError = ref('')

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

const archiveNoCN = computed(() => {
  if (!detail.value?.id) return ''
  const cn = '〇一二三四五六七八九'
  const cnId = String(detail.value.id).split('').map(n => cn[Number(n)]).join('')
  return `存档第 ${cnId} 号`
})

const archiveLocation = computed(() => detail.value?.location || null)

const archiveLocationCoordinates = computed(() => {
  const current = archiveLocation.value
  if (!current || typeof current.latitude !== 'number' || typeof current.longitude !== 'number') {
    return ''
  }
  return `${current.latitude.toFixed(6)}, ${current.longitude.toFixed(6)}`
})

const archiveLocationPrimary = computed(() => {
  const current = archiveLocation.value
  if (!current) return ''
  return current.name?.trim()
    || current.address?.trim()
    || archiveLocationCoordinates.value
    || '已保存地点'
})

const archiveLocationAddress = computed(() => {
  const current = archiveLocation.value
  if (!current?.address?.trim() || current.address.trim() === archiveLocationPrimary.value) return ''
  return current.address.trim()
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

// 标题保持现状；仅在既有霜层下展示已返回正文的有限片段。
const sealedTitle = computed(() => detail.value?.title?.trim()
  ? '“' + detail.value.title.trim() + '”' : '未命名片段')

const sealedExcerpt = computed(() => {
  const characters = Array.from((detail.value?.content || '').trim())
  const excerpt = characters.slice(0, 72).join('')
  return characters.length > 72 ? excerpt + '…' : excerpt
})

const { cssVars, navBarHeight, navBarTotalHeight, rightSafeWidth, statusBarHeight } =
  useWechatNavMetrics()

const pageStyle = computed(() => ({ ...cssVars.value }))

const topSafeStyle = computed(() => ({
  minHeight: `calc(${navBarTotalHeight.value}px + ${isSealed.value ? 88 : 24}rpx)`,
  paddingTop: `${statusBarHeight.value}px`,
}))

const topNavStyle = computed(() => ({
  height: `${navBarHeight.value}px`,
}))

const closeRailStyle = computed(() => ({
  left: '0',
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
const isSaved    = computed(() => detail.value?.status === RecordStatus.SAVED)
const isSealed   = computed(() => detail.value?.status === RecordStatus.SEALED)
const isUnlocked = computed(() => detail.value?.status === RecordStatus.UNLOCKED)
const canSubmitReply   = computed(() => Boolean(detail.value?.canReply && !detail.value?.hasReply))
const hasSubmittedReply = computed(() => Boolean(detail.value?.hasReply))
const laterReflectionCount = computed(() => Number(detail.value?.realityLaterSubmitCount || 0))
const canEditLaterReflection = computed(() => isUnlocked.value && laterReflectionCount.value < 2)
const currentChapter = computed(() => detail.value?.chapter || null)
const chapterActionText = computed(() => currentChapter.value ? '转移篇章' : '加入篇章')

const unlockReminderStatusText = computed(() => {
  const status = detail.value?.unlockReminderStatus
  if (!status) return '未记录订阅状态'
  const statusMap: Record<RecordReminderStatus, string> = {
    [RecordReminderStatus.REQUESTED]: '已尝试请求订阅，等待后续确认',
    [RecordReminderStatus.AUTHORIZED]: '你已同意接收这条记录的抵达提醒',
    [RecordReminderStatus.DENIED]: '你已拒绝本条记录的订阅提醒',
    [RecordReminderStatus.NOT_CONFIGURED]: '提醒模板暂未配置，未伪造发送成功',
    [RecordReminderStatus.SEND_PENDING]: '提醒已进入发送流程',
    [RecordReminderStatus.SEND_SUCCESS]: '微信订阅提醒已发送',
    [RecordReminderStatus.SEND_FAILED]: '提醒发送失败，记录解锁不受影响',
    [RecordReminderStatus.SKIPPED_NO_OPENID]: '当前账号未绑定微信 OpenID，已跳过发送',
  }
  return statusMap[status] || '订阅提醒状态暂不可识别'
})

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
    if (detail.value) applyMemoryPolicy(detail.value)
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

const submitLaterReflection = async () => {
  if (!detail.value?.id || !canEditLaterReflection.value) {
    uni.showToast({ title: '后来其实已不能继续修改', icon: 'none' })
    return
  }
  const content = laterReflectionDraft.value.trim()
  if (!content) {
    uni.showToast({ title: '先写下后来其实', icon: 'none' })
    return
  }
  if (!getToken() && hasPreviewSession()) {
    showPreviewReadonlyToast()
    return
  }

  submittingLaterReflection.value = true
  try {
    const updated = await recordService.updateLaterReflection(detail.value.id, content)
    recordStore.detail = updated
    laterReflectionDraft.value = updated.realityLater || ''
    applyMemoryPolicy(updated)
    uni.showToast({ title: '后来其实已保存', icon: 'success' })
  } catch (error) {
    uni.showToast({ title: toUserMessage(error), icon: 'none' })
  } finally {
    submittingLaterReflection.value = false
  }
}

/* ══════ C3b 友人回看对话 ══════ */

/** 回看对话只在已解锁记录上可用；与后端的记录状态校验保持一致。 */
const canOpenReviewChat = computed(() => isUnlocked.value && Boolean(detail.value?.id))

const applyReviewSession = (session: AgentSession) => {
  reviewSession.value = session
  // 显式失败与不可用都告知用户，不伪装成功。
  reviewError.value = session.status === 'SUCCESS' ? '' : (session.message || '现在聊不了，待会儿再试试')
}

const applyMemoryPolicy = (record: { agentMemoryExcluded?: boolean, agentMemoryContextNote?: string | null }) => {
  memoryExcluded.value = record.agentMemoryExcluded === true
  memoryNoteDraft.value = record.agentMemoryContextNote || ''
}

const saveAgentMemoryPolicy = async () => {
  if (!detail.value?.id || savingMemoryPolicy.value) return false
  if (!getToken() && hasPreviewSession()) {
    showPreviewReadonlyToast()
    return false
  }
  savingMemoryPolicy.value = true
  try {
    const updated = await recordService.updateAgentMemoryPolicy(
      detail.value.id,
      memoryExcluded.value,
      memoryNoteDraft.value.trim() || null,
    )
    recordStore.detail = updated
    applyMemoryPolicy(updated)
    uni.showToast({ title: memoryExcluded.value ? '之后不再参考这条记录' : '记忆设置已保存', icon: 'none' })
    return true
  } catch (error) {
    if (detail.value) applyMemoryPolicy(detail.value)
    uni.showToast({ title: toUserMessage(error), icon: 'none' })
    return false
  } finally {
    savingMemoryPolicy.value = false
  }
}

// 备注只在确认保存时交给原接口，取消和失败都不丢失已确认的参考设置。
const openMemoryNoteSheet = () => {
  if ((!isUnlocked.value && !isSealed.value) || !detail.value || savingMemoryPolicy.value) return
  memorySheetNote.value = detail.value.agentMemoryContextNote || ''
  memorySheetExcluded.value = detail.value.agentMemoryExcluded === true
  showMemoryNoteSheet.value = true
}

const closeMemoryNoteSheet = () => {
  if (!savingMemoryPolicy.value) showMemoryNoteSheet.value = false
}

const saveMemoryNoteSheet = async () => {
  if (!showMemoryNoteSheet.value || (!isUnlocked.value && !isSealed.value) || !detail.value || savingMemoryPolicy.value) return
  if (!getToken() && hasPreviewSession()) { showPreviewReadonlyToast(); return }
  memoryNoteDraft.value = memorySheetNote.value
  memoryExcluded.value = memorySheetExcluded.value
  if (await saveAgentMemoryPolicy()) showMemoryNoteSheet.value = false
}

const switchReviewMemoryAuthorization = async (enabled: boolean) => {
  const sessionId = reviewSession.value?.sessionId
  if (!sessionId || switchingReviewMemory.value) return
  if (Boolean(reviewSession.value?.crossRecordMemoryEnabled) === enabled) return
  switchingReviewMemory.value = true
  try {
    applyReviewSession(await agentService.switchMemoryAuthorization(sessionId, enabled))
  } catch (error) {
    uni.showToast({ title: toUserMessage(error), icon: 'none' })
  } finally {
    switchingReviewMemory.value = false
  }
}

const openReviewChat = async () => {
  if (!canOpenReviewChat.value || !detail.value?.id) {
    return
  }
  if (hasPreviewSession() && !getToken()) {
    showPreviewReadonlyToast()
    return
  }
  // 与回应浮层互斥：同时开两个 bottom sheet 会叠在一起，交互上也不该同时进行。
  showReplySheet.value = false
  showReviewChat.value = true
  if (reviewSession.value) {
    return
  }
  reviewLoading.value = true
  reviewError.value = ''
  try {
    applyReviewSession(await agentService.startOrResumeReview(detail.value.id))
  } catch (error) {
    reviewError.value = toUserMessage(error)
  } finally {
    reviewLoading.value = false
  }
}

const closeReviewChat = () => {
  showReviewChat.value = false
}

const sendReviewMessage = async (content: string) => {
  const sessionId = reviewSession.value?.sessionId
  if (!sessionId || reviewSending.value) {
    return
  }
  reviewSending.value = true
  reviewError.value = ''
  try {
    applyReviewSession(await agentService.sendMessage(sessionId, content))
  } catch (error) {
    reviewError.value = toUserMessage(error)
  } finally {
    reviewSending.value = false
  }
}

const refreshAfterChapterConflict = async (recordId: number, error: unknown) => {
  if (toUserMessage(error).includes('变更')) await loadDetail(recordId)
}

const chooseChapter = async () => {
  if (!detail.value?.id || isDraft.value || chapterActionLoading.value) return
  if (hasPreviewSession()) {
    showPreviewReadonlyToast()
    return
  }
  chapterActionLoading.value = true
  try {
    const result = await chapterStore.fetchAll()
    const candidates = result.list.filter((item) =>
      item.status === 'ACTIVE' && item.id !== currentChapter.value?.id,
    )
    if (candidates.length === 0) {
      uni.showToast({ title: '还没有可加入的进行中篇章', icon: 'none' })
      return
    }
    uni.showActionSheet({
      itemList: candidates.map((item) => item.name),
      success: (selection) => {
        const target = candidates[selection.tapIndex]
        if (target) confirmChapterChange(target)
      },
    })
  } catch (error) {
    uni.showToast({ title: toUserMessage(error), icon: 'none' })
  } finally {
    chapterActionLoading.value = false
  }
}

const confirmChapterChange = (target: TimeChapterSummaryVO) => {
  if (!detail.value?.id || chapterActionLoading.value) return
  const current = currentChapter.value
  const message = current
    ? `从“${current.name}”转移到“${target.name}”？`
    : `把这条记录加入“${target.name}”？`
  uni.showModal({
    title: current ? '确认转移篇章' : '确认加入篇章',
    content: message,
    confirmColor: '#b5352a',
    success: async (result) => {
      if (!result.confirm || !detail.value?.id) return
      chapterActionLoading.value = true
      try {
        await timeChapterService.addMembers(target.id, {
          recordIds: [detail.value.id],
          expectedVersion: target.version,
          transfers: current ? [{ recordId: detail.value.id, fromChapterId: current.id }] : [],
        })
        await loadDetail(detail.value.id)
      } catch (error) {
        await refreshAfterChapterConflict(detail.value.id, error)
        uni.showToast({ title: toUserMessage(error), icon: 'none' })
      } finally {
        chapterActionLoading.value = false
      }
    },
  })
}

const removeFromChapter = async () => {
  if (!detail.value?.id || !currentChapter.value || chapterActionLoading.value) return
  if (hasPreviewSession()) {
    showPreviewReadonlyToast()
    return
  }
  chapterActionLoading.value = true
  try {
    const result = await chapterStore.fetchAll()
    const current = result.list.find((item) => item.id === currentChapter.value?.id)
    if (!current) throw new Error('篇章不存在或已不可见')
    uni.showModal({
      title: '移出当前篇章？',
      content: `将从“${current.name}”移出，但不会删除这条记录。`,
      confirmColor: '#b5352a',
      success: async (confirmed) => {
        if (!confirmed.confirm || !detail.value?.id) {
          chapterActionLoading.value = false
          return
        }
        try {
          await timeChapterService.removeMembers(current.id, {
            recordIds: [detail.value.id],
            expectedVersion: current.version,
          })
          await loadDetail(detail.value.id)
        } catch (error) {
          await refreshAfterChapterConflict(detail.value.id, error)
          uni.showToast({ title: toUserMessage(error), icon: 'none' })
        } finally {
          chapterActionLoading.value = false
        }
      },
    })
  } catch (error) {
    chapterActionLoading.value = false
    uni.showToast({ title: toUserMessage(error), icon: 'none' })
  }
}

/** 重试：会话尚未建立时重开，否则重发上一轮由后端按同轮重试语义处理。 */
const retryReviewChat = async () => {
  reviewError.value = ''
  if (!reviewSession.value) {
    await openReviewChat()
    return
  }
  const sessionId = reviewSession.value.sessionId
  reviewLoading.value = true
  try {
    applyReviewSession(await agentService.getSession(sessionId))
  } catch (error) {
    reviewError.value = toUserMessage(error)
  } finally {
    reviewLoading.value = false
  }
}

const openReplySheet = () => {
  // 与回看浮层互斥。
  showReviewChat.value = false
  if (!canSubmitReply.value) {
    uni.showToast({ title: '当前无法留下回应', icon: 'none' })
    return
  }
  showReplySheet.value = true
}

const closeReplySheet = () => {
  showReplySheet.value = false
}

const deleteOwnedRecord = () => {
  if (!detail.value || deletingRecord.value) return
  if (!getToken() && hasPreviewSession()) { showPreviewReadonlyToast('演示模式不会删除记录'); return }
  uni.showModal({
    title: '删除这条记录？',
    content: '将同时清理关联媒体、位置、回信与 Agent 数据，且不可恢复。',
    confirmText: '继续',
    confirmColor: '#a23d34',
    success: async (first) => {
      if (!first.confirm || !detail.value) return
      deletingRecord.value = true
      try {
        const intent = await dataOwnershipService.prepareDeletion('RECORD', detail.value.id)
        uni.showModal({
          title: '最后确认',
          content: `确认范围：1 条记录\n确认短语：${intent.confirmationText ?? ''}`,
          confirmText: '确认删除',
          confirmColor: '#a23d34',
          success: async (second) => {
            if (!second.confirm || !intent.confirmationText) { deletingRecord.value = false; return }
            try {
              const result = await dataOwnershipService.confirmDeletion(intent.id, intent.confirmationText)
              if (result.status !== 'SUCCEEDED') throw new Error(result.retryable ? '仍有数据未清理，请到“数据与所有权”重试' : '删除尚未完成')
              uni.showToast({ title: '记录已删除', icon: 'success' })
              setTimeout(closePage, 450)
            } catch (error) { uni.showToast({ title: toUserMessage(error), icon: 'none' }) }
            finally { deletingRecord.value = false }
          },
        })
      } catch (error) { deletingRecord.value = false; uni.showToast({ title: toUserMessage(error), icon: 'none' }) }
    },
  })
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
  laterReflectionDraft.value = detail.value?.realityLater || ''
  if (detail.value) applyMemoryPolicy(detail.value)
})
</script>

<template>
  <view class="archive-page" :class="{ 'archive-page--saved': isSaved, 'archive-page--sealed': isSealed, 'archive-page--review': isUnlocked }" :style="pageStyle">
    <PreviewModeNotice />
    <!-- 宣纸底纹 -->
    <view class="archive-backdrop" />

    <!-- 顶部安全区：品牌名 + 关闭按钮 -->
    <view class="archive-top-safe" :style="topSafeStyle">
      <view class="archive-top-safe__mist" />
      <view class="archive-top-safe__nav" :style="topNavStyle">
        <!-- 品牌名居中（所有状态统一显示） -->
        <view class="archive-top-safe__logo">
          <text class="archive-top-safe__logo-text">{{ isSaved || isSealed || isUnlocked ? '时光回序' : '时 光 回 序' }}</text>
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

        <!-- ══════ SAVED：已留下，仍可补充 ══════ -->
        <view v-else-if="isSaved" class="saved-hero">
          <text class="saved-confirmation">这一刻已经留下</text>
          <view class="saved-letter">
            <view class="saved-letter-redline" aria-hidden="true" />
            <view class="saved-letter-fold" aria-hidden="true" />
            <text v-if="detail.title" class="saved-record-title">{{ detail.title }}</text>
            <text class="saved-record-body">{{ detail.content || '这是一段由图片或声音留下的记录。' }}</text>
            <text class="saved-record-time">留下于 {{ formatDateTime(detail.createdAt) }}</text>
            <view class="saved-stamp" aria-hidden="true">留</view>
          </view>
          <ReadOnlyRecordMedia
            :record-id="detail.id"
            :attachments="detail.attachments || []"
            :cover-id="detail.cover?.id || null"
            variant="unlocked"
          />
          <button class="saved-continue" @tap="openEditor"><view class="saved-action-dot" aria-hidden="true" /><text>继续补充</text></button>
        </view>

        <!-- SEALED：demo信笺布局，保留真实倒计时与只读媒体。 -->
        <view v-else-if="isSealed" class="sealed-hero">
          <view class="sealed-meta">
            <view class="sealed-meta__line">
              <text class="sealed-meta__no">{{ archiveNoCN }}</text>
              <text v-if="archiveLocationPrimary" class="sealed-meta__location" :aria-label="archiveLocationPrimary">· {{ archiveLocationPrimary }}</text>
            </view>
            <text class="sealed-meta__season">{{ archiveSeasonCN }}</text>
          </view>
          <view class="sealed-card">
            <view class="sealed-card__corner" aria-hidden="true" />
            <view class="sealed-card__meta">
              <view class="sealed-seal"><text class="sealed-seal__char">封</text></view>
              <text class="sealed-card__tag">{{ archiveWrittenText }}</text>
            </view>
            <text class="sealed-quote__text">{{ sealedTitle }}</text>
            <view class="sealed-body-ink" :class="{ 'sealed-body-ink--excerpt': sealedExcerpt }" aria-hidden="true">
              <text v-if="sealedExcerpt" class="sealed-body-excerpt" :selectable="false">{{ sealedExcerpt }}</text>
              <template v-else><view /><view /><view /><view /></template>
            </view>
            <view class="sealed-frost" aria-hidden="true" />
          </view>
          <ReadOnlyRecordMedia
            class="sealed-media"
            :record-id="detail.id"
            :attachments="detail.attachments || []"
            :cover-id="detail.cover?.id || null"
            variant="sealed"
          />
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
          </view>
        </view>

        <!-- UNLOCKED：demo阅读主线，次要信息默认收起。 -->
        <view v-else-if="isUnlocked" class="unlock-hero">
          <view class="unlock-archive">
            <text class="unlock-archive-no">{{ archiveNoCN }}</text>
            <text v-if="archiveLocationPrimary" class="unlock-loc-text" :aria-label="archiveLocationPrimary">· {{ archiveLocationPrimary }}</text>
          </view>
          <text class="unlock-season">{{ archiveSeasonCN }}</text>
          <text class="unlock-quote">{{ unlockQuote || '“未命名片段”' }}</text>
          <view class="unlock-seal-row">
            <view class="unlock-seal"><text class="unlock-seal-char">阅</text></view>
            <text class="unlock-seal-label">{{ archiveWrittenText }}</text>
          </view>
          <view class="unlock-card">
            <view class="unlock-card-vline" aria-hidden="true" />
            <view class="unlock-card-corner" aria-hidden="true" />
            <text class="unlock-card-text">{{ detail.content || '这是一段由图片或声音留下的记录。' }}</text>
            <text v-if="archiveLocationPrimary" class="unlock-card-where">当时所在 · {{ archiveLocationPrimary }}</text>
          </view>
          <ReadOnlyRecordMedia class="unlock-media" :record-id="detail.id" :attachments="detail.attachments || []" :cover-id="detail.cover?.id || null" variant="review" />
          <view class="reflection-panel">
            <view class="unlock-card-corner" aria-hidden="true" />
            <view class="reflection-block">
              <view class="reflection-head">
                <text class="reflection-label">现在的我 · 后来其实</text>
                <text class="reflection-count">{{ laterReflectionCount }}/2</text>
              </view>
              <textarea
                v-if="canEditLaterReflection"
                v-model="laterReflectionDraft"
                class="reflection-textarea"
                :disabled="submittingLaterReflection"
                auto-height
                maxlength="1000"
                placeholder="后来的你，想对当时说些什么"
                placeholder-class="reflection-placeholder"
              />
              <text v-else class="reflection-text">{{ detail.realityLater || '还没有写下后来其实。' }}</text>
              <view
                v-if="canEditLaterReflection"
                class="reflection-save"
                :class="{ 'reflection-save--loading': submittingLaterReflection }"
                @tap="!submittingLaterReflection && submitLaterReflection()"
              >
                {{ submittingLaterReflection ? '保存中…' : (detail.realityLater ? '修改' : '保存') }}
              </view>
            </view>
          </view>
          <view v-if="canOpenReviewChat" class="review-entry" @tap="openReviewChat"><text class="review-entry-text">和它聊聊那时候</text></view>
          <view class="unlock-actions">
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
              <view class="unlock-cta-dot" />
              <text class="unlock-cta-text">留 下 回 应</text>
            </view>

          </view>
        </view>

        <view v-if="isSaved" class="chapter-membership-panel">
          <view class="chapter-membership-head">
            <text class="chapter-membership-title">时间篇章</text>
            <text v-if="currentChapter" class="chapter-membership-current">当前：{{ currentChapter.name }}</text>
            <text v-else class="chapter-membership-current">尚未归入篇章</text>
          </view>
          <view class="chapter-membership-actions">
            <view class="chapter-membership-action" @tap="chooseChapter">
              {{ chapterActionLoading ? '处理中…' : chapterActionText }}
            </view>
            <view v-if="currentChapter" class="chapter-membership-action chapter-membership-action--quiet" @tap="removeFromChapter">移出篇章</view>
          </view>
        </view>

        <view v-if="isSealed" class="sealed-reference">
          <view class="sealed-reference-toggle" role="button" aria-label="编辑 Agent 参考设置与备注" aria-haspopup="dialog" :aria-disabled="savingMemoryPolicy" :class="{ 'sealed-reference-toggle--busy': savingMemoryPolicy }" @tap="openMemoryNoteSheet">
            <view class="sealed-reference-switch" :class="{ 'sealed-reference-switch--on': memoryExcluded }"><view /></view>
            <text>{{ savingMemoryPolicy ? '保存中…' : '不再供参考' }} · {{ memoryNoteDraft.trim() || '只代表当时，不代表现在' }}</text>
          </view>
          <button class="sealed-options-toggle" :aria-expanded="sealedOptionsOpen" @tap="sealedOptionsOpen = !sealedOptionsOpen">{{ sealedOptionsOpen ? '收起更多' : '更多 · 篇章与附言' }}</button>
          <view v-if="sealedOptionsOpen" class="sealed-options">
            <view v-if="archiveLocationPrimary" class="sealed-location-details">
              <text>地点：{{ archiveLocationPrimary }}</text>
              <text v-if="archiveLocationAddress">{{ archiveLocationAddress }}</text>
            </view>
            <view class="chapter-membership-panel">
              <view class="chapter-membership-head">
                <text class="chapter-membership-title">时间篇章</text>
                <text v-if="currentChapter" class="chapter-membership-current">当前：{{ currentChapter.name }}</text>
                <text v-else class="chapter-membership-current">尚未归入篇章</text>
              </view>
              <view class="chapter-membership-actions">
                <view class="chapter-membership-action" @tap="chooseChapter">
                  {{ chapterActionLoading ? '处理中…' : chapterActionText }}
                </view>
                <view v-if="currentChapter" class="chapter-membership-action chapter-membership-action--quiet" @tap="removeFromChapter">
                  移出篇章
                </view>
              </view>
            </view>
          </view>
        </view>
        <view v-else-if="isUnlocked" class="review-more">
          <button class="memory-note-entry" aria-label="编辑 Agent 参考设置与备注" @tap="openMemoryNoteSheet">
            <view class="memory-note-indicator" :class="{ 'memory-note-indicator--on': memoryExcluded }" aria-hidden="true"><view /></view>
            <text class="memory-note-entry-label">不再供参考 · {{ memoryNoteDraft || '只代表当时，不代表现在' }}</text>
          </button>
          <button class="review-more-toggle" :aria-expanded="reviewOptionsOpen" @tap="reviewOptionsOpen = !reviewOptionsOpen">{{ reviewOptionsOpen ? '收起更多' : '更多 · 篇章与参考' }}</button>
          <view v-if="reviewOptionsOpen" class="review-more-content">
            <view class="chapter-membership-panel">
              <view class="chapter-membership-head">
                <text class="chapter-membership-title">时间篇章</text>
                <text v-if="currentChapter" class="chapter-membership-current">当前：{{ currentChapter.name }}</text>
                <text v-else class="chapter-membership-current">尚未归入篇章</text>
              </view>
              <view class="chapter-membership-actions">
                <view class="chapter-membership-action" @tap="chooseChapter">
                  {{ chapterActionLoading ? '处理中…' : chapterActionText }}
                </view>
                <view v-if="currentChapter" class="chapter-membership-action chapter-membership-action--quiet" @tap="removeFromChapter">
                  移出篇章
                </view>
              </view>
            </view>
            <view v-if="archiveLocation" class="unlock-location">
              <text class="unlock-location-label">当时所在</text>
              <text class="unlock-location-name">{{ archiveLocationPrimary }}</text>
              <text v-if="archiveLocationAddress" class="unlock-location-detail">{{ archiveLocationAddress }}</text>
              <text
                v-if="archiveLocationCoordinates && archiveLocationCoordinates !== archiveLocationPrimary"
                class="unlock-location-coordinates"
              >{{ archiveLocationCoordinates }}</text>
            </view>
            <view class="reflection-block">
              <text class="reflection-label">订阅提醒</text>
              <text class="reflection-text">{{ unlockReminderStatusText }}</text>
            </view>
            <view class="reflection-block">
              <text class="reflection-label">那时的我 · 你当时以为</text>
              <text class="reflection-text">{{ detail.beliefThen || '当时的想法还没有整理出来。' }}</text>
            </view>

            <view class="ownership-delete" @tap="deleteOwnedRecord">{{ deletingRecord ? '正在确认…' : '删除这条记录' }}</view>
          </view>
        </view>
        <view v-else class="memory-policy">
          <text class="memory-policy-title">Agent 参考</text>
          <text class="memory-policy-hint">之后不再参考，不会撤回已经发出去的那一轮。</text>
          <view class="memory-policy-row" @tap="memoryExcluded = !memoryExcluded">
            <text class="memory-policy-label">不再供 Agent 参考</text>
            <view class="memory-switch" :class="{ 'memory-switch--on': memoryExcluded }">
              <view class="memory-switch-knob" />
            </view>
          </view>
          <textarea
            v-model="memoryNoteDraft"
            class="memory-policy-note"
            auto-height
            maxlength="255"
            placeholder="只代表当时，不代表现在"
            placeholder-class="memory-policy-placeholder"
          />
          <view
            class="memory-policy-save"
            :class="{ 'memory-policy-save--loading': savingMemoryPolicy }"
            @tap="saveAgentMemoryPolicy"
          >{{ savingMemoryPolicy ? '保存中…' : '保存' }}</view>
        </view>

        <view v-if="!isSealed && !isUnlocked" class="ownership-delete" @tap="deleteOwnedRecord">{{ deletingRecord ? '正在确认…' : '删除这条记录' }}</view>
      </view>

      <!-- 无 detail 兜底 -->
      <view v-else class="state-wrap">
        <EmptyState text="记录暂时不可用" />
        <PrimaryButton text="重试加载" ghost @tap="retryLoadDetail" />
      </view>

    </view>

    <!-- 备注小浮层：遮罩与纸面为兄弟节点，原生输入点击不触发关闭。 -->
    <view v-if="(isUnlocked || isSealed) && showMemoryNoteSheet" class="memory-note-layer">
      <view class="memory-note-mask" @tap="closeMemoryNoteSheet" />
      <view class="memory-note-panel" role="dialog" aria-modal="true" aria-label="Agent 参考备注">
        <view class="memory-note-handle" />
        <button class="memory-note-close" :disabled="savingMemoryPolicy" aria-label="关闭备注" @tap="closeMemoryNoteSheet">×</button>
        <text class="memory-note-kicker">供它回看时参考</text>
        <text class="memory-note-title">给当时的自己，留个备注</text>
        <button class="memory-note-toggle" role="switch" :aria-checked="memorySheetExcluded" :disabled="savingMemoryPolicy" @tap="memorySheetExcluded = !memorySheetExcluded">
          <text>不再供 Agent 参考</text>
          <view class="memory-switch" :class="{ 'memory-switch--on': memorySheetExcluded }"><view class="memory-switch-knob" /></view>
        </button>
        <textarea v-model="memorySheetNote" class="memory-note-input" :disabled="savingMemoryPolicy" maxlength="255" :cursor-spacing="24" placeholder="只代表当时，不代表现在" placeholder-class="memory-policy-placeholder" />
        <text class="memory-note-hint">之后不再参考，不会撤回已经发出去的那一轮。</text>
        <button class="memory-note-save" :disabled="savingMemoryPolicy" @tap="saveMemoryNoteSheet"><view class="unlock-cta-dot" /><text>{{ savingMemoryPolicy ? '保存中…' : '保存备注' }}</text></button>
      </view>
    </view>

    <!--
      C3b 回看对话浮层。结构上不存在工具确认条与素材回填入口——
      后端在回看模式下完全不产出它们，不是前端隐藏。
    -->
    <ReviewChatSheet
      :visible="showReviewChat"
      :session="reviewSession"
      :loading="reviewLoading"
      :sending="reviewSending"
      :switching-memory-authorization="switchingReviewMemory"
      :error-message="reviewError"
      @close="closeReviewChat"
      @send="sendReviewMessage"
      @retry="retryReviewChat"
      @switch-memory-authorization="switchReviewMemoryAuthorization"
    />

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
.archive-page--sealed {
  padding: 0 56rpx calc(48rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
  font-family: 'Noto Serif SC', 'Songti SC', 'STSong', 'SimSun', serif;
}
.archive-page--sealed .archive-top-safe__logo-text {
  font-family: inherit;
  font-size: 22rpx;
  letter-spacing: 8rpx;
  color: #9e9890;
}
.archive-page--sealed .archive-close {
  top: calc(100% + 12rpx);
  transform: none;
  width: 72rpx;
  height: 72rpx;
  border: none;
  background: transparent;
}
.archive-page--sealed .archive-close__icon {
  width: 40rpx;
  height: 40rpx;
}
.archive-page--sealed .archive-close__line {
  background: #6b6560;
  height: 2rpx;
}
.archive-page--sealed .archive-main {
  margin-top: 0;
}
.archive-page--sealed .archive-stage {
  gap: 0;
}
.sealed-hero {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  padding: 24rpx 0 0;
}
.sealed-meta {
  text-align: center;
  margin: 0 0 48rpx;
}
.sealed-meta__line, .sealed-meta__season {
  display: block;
  color: #6b6560;
  font-size: 24rpx;
  letter-spacing: 3rpx;
  line-height: 1.8;
  overflow-wrap: anywhere;
}
.sealed-meta__line {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10rpx;
}
.sealed-meta__no {
  flex-shrink: 0;
  max-width: 58%;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.sealed-meta__location {
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.sealed-meta__season {
  margin-top: 14rpx;
}
.sealed-card {
  position: relative;
  width: 100%;
  min-height: 500rpx;
  padding: 52rpx 48rpx 68rpx;
  box-sizing: border-box;
  border: 1rpx solid #e3dacc;
  border-radius: 2rpx;
  background: #fbf8f1;
  box-shadow: 0 4rpx 10rpx rgba(125,105,75,.1), 0 20rpx 40rpx rgba(125,105,75,.12);
}
.sealed-card__corner {
  position: absolute;
  z-index: 3;
  top: 0;
  right: 0;
  width: 34rpx;
  height: 34rpx;
  background: linear-gradient(225deg, #f4efe5 49%, #e3dbc9 50%);
  box-shadow: -3rpx 3rpx 5rpx rgba(120,100,70,.16);
}
.sealed-card__meta {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 24rpx;
}
.sealed-seal {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 50rpx;
  height: 50rpx;
  border: 1rpx solid #b5352a;
  border-radius: 50%;
  opacity: .75;
}
.sealed-seal__char {
  font-size: 24rpx;
  color: #b5352a;
}
.sealed-card__tag {
  color: #8a847c;
  font-size: 24rpx;
  line-height: 1.7;
  letter-spacing: 2rpx;
}
.sealed-quote__text {
  position: relative;
  z-index: 2;
  display: block;
  margin: 28rpx 0 0;
  text-align: center;
  font-size: 38rpx;
  line-height: 1.8;
  letter-spacing: 3rpx;
  color: rgba(107,101,96,.56);
  text-shadow: 0 0 4rpx rgba(107,101,96,.2);
  filter: blur(1.2rpx);
  overflow-wrap: anywhere;
}
.sealed-body-ink {
  height: 184rpx;
  margin-top: 40rpx;
  opacity: .5;
  filter: blur(7rpx);
}
.sealed-body-ink--excerpt {
  position: relative;
  z-index: 1;
}
.sealed-body-ink--excerpt::after {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background: linear-gradient(180deg, rgba(255,255,255,0), rgba(255,255,255,.9));
}
.sealed-body-excerpt {
  display: block;
  max-height: 156rpx;
  overflow: hidden;
  font-size: 28rpx;
  line-height: 1.85;
  color: #6b6560;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
.sealed-body-ink view {
  height: 18rpx;
  margin-bottom: 28rpx;
  background: linear-gradient(180deg, transparent, rgba(107,101,96,.12), transparent);
}
.sealed-body-ink view:nth-child(2n) {
  width: 85%;
}
.sealed-frost {
  position: absolute;
  left: 0;
  right: 0;
  top: 0;
  bottom: 0;
  pointer-events: none;
  background: linear-gradient(180deg, rgba(255,255,255,0) 0%, rgba(255,255,255,.08) 18%, rgba(255,255,255,.7) 42%, rgba(255,255,255,.94) 76%, #fff 100%);
  box-shadow: inset 0 0 52rpx 16rpx rgba(255,255,255,.85), inset 0 0 10rpx 2rpx rgba(255,255,255,.8);
}
.sealed-media {
  display: block;
  width: 100%;
}
.sealed-lock {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 28rpx;
  margin-top: 64rpx;
}
.sealed-lock__live {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16rpx;
}
.sealed-lock__pulse {
  width: 9rpx;
  height: 9rpx;
  border-radius: 50%;
  background: #b5352a;
}
.sealed-lock__live-text {
  color: #8a847c;
  font-size: 24rpx;
  line-height: 1.7;
  letter-spacing: 2rpx;
}
.sealed-countdown {
  text-align: center;
}
.sealed-countdown__label {
  display: block;
  margin-bottom: 16rpx;
  color: #9e9890;
  font-size: 21rpx;
  letter-spacing: 4rpx;
}
.sealed-countdown__digits {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 8rpx;
}
.digit-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 64rpx;
}
.digit-num {
  color: #403c37;
  font-size: 58rpx;
  line-height: 1.2;
}
.digit-unit {
  margin-top: 8rpx;
  color: #9e9890;
  font-size: 20rpx;
}
.digit-sep {
  color: #c8c2b8;
  font-size: 42rpx;
  margin: 0 4rpx;
}
.sealed-reference {
  margin: 80rpx 0 0;
}
.sealed-reference-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16rpx;
  min-height: 88rpx;
  color: #9e9890;
  font-size: 24rpx;
  line-height: 1.8;
  letter-spacing: 1rpx;
  text-align: center;
}
.sealed-reference-toggle text {
  min-width: 0;
  max-width: calc(100% - 80rpx);
  text-align: center;
}
.sealed-location-details {
  margin: 16rpx 0 24rpx;
  color: #9e9890;
  font-size: 22rpx;
  line-height: 1.8;
}
.sealed-location-details text { display: block; overflow-wrap: anywhere; }
.sealed-reference-toggle--busy {
  opacity: .6;
}
.sealed-reference-switch {
  position: relative;
  flex-shrink: 0;
  width: 60rpx;
  height: 36rpx;
  box-sizing: border-box;
  border: 1rpx solid #c8c2b8;
  border-radius: 20rpx;
}
.sealed-reference-switch view {
  position: absolute;
  top: 4rpx;
  left: 4rpx;
  width: 26rpx;
  height: 26rpx;
  border-radius: 50%;
  background: #c8c2b8;
}
.sealed-reference-switch--on {
  border-color: #b5352a;
}
.sealed-reference-switch--on view {
  left: 28rpx;
  background: #b5352a;
}
.sealed-options-toggle {
  margin: 8rpx 0 0;
  padding: 8rpx 0;
  min-height: 72rpx;
  color: #9e9890;
  background: transparent;
  text-align: center;
  font-family: inherit;
  font-size: 21rpx;
  line-height: 2;
  border: none;
}
.sealed-options-toggle::after {
  border: none;
}
.sealed-options {
  padding: 8rpx 0 24rpx;
}
.sealed-reference-note {
  display: block;
  margin-top: 24rpx;
  color: #9e9890;
  font-size: 21rpx;
  line-height: 1.7;
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

.unlock-location {
  margin-top: 32rpx;
  padding: 28rpx 4rpx;
  border-top: 1rpx solid rgba(188, 174, 152, 0.24);
  border-bottom: 1rpx solid rgba(188, 174, 152, 0.24);
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.unlock-location-label {
  font-family: var(--font-secondary);
  font-size: 20rpx;
  color: var(--ink-faint);
  letter-spacing: 0.12em;
}

.unlock-location-name {
  font-family: var(--font-reading);
  font-size: 27rpx;
  color: var(--ink-mid);
  line-height: 1.6;
  word-break: break-word;
}

.unlock-location-detail,
.unlock-location-coordinates {
  font-family: var(--font-secondary);
  font-size: 21rpx;
  color: var(--ink-light);
  line-height: 1.6;
  word-break: break-word;
}

.unlock-location-coordinates {
  font-size: 19rpx;
  color: var(--ink-faint);
}

.reflection-panel {
  margin-top: 32rpx;
  padding: 30rpx;
  border: 1rpx solid rgba(188, 174, 152, 0.26);
  background: rgba(252, 249, 244, 0.58);
}

.reflection-block + .reflection-block {
  margin-top: 28rpx;
  padding-top: 26rpx;
  border-top: 1rpx solid rgba(188, 174, 152, 0.2);
}

.reflection-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
}

.reflection-label {
  display: block;
  font-family: var(--font-serif);
  font-size: 23rpx;
  color: var(--ink-soft);
  margin-bottom: 14rpx;
}

.reflection-count {
  font-size: 22rpx;
  color: var(--ink-soft);
}

.reflection-text,
.reflection-textarea {
  width: 100%;
  font-family: var(--font-reading);
  font-size: 26rpx;
  line-height: 1.85;
  color: var(--ink-mid);
  white-space: pre-wrap;
  word-break: break-word;
}

.reflection-textarea {
  min-height: 116rpx;
}

.reflection-save {
  width: 180rpx;
  height: 58rpx;
  margin-top: 20rpx;
  border: 1rpx solid rgba(181, 53, 42, 0.38);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24rpx;
  color: #9a332a;
}

.reflection-save--loading {
  opacity: 0.66;
}

:deep(.reflection-placeholder) {
  color: rgba(130, 118, 102, 0.48);
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

/* C3b 回看对话入口：比「留下回应」更轻，不抢主动作 */
.review-entry {
  margin-top: 28rpx;
  padding: 18rpx 0;
  text-align: center;
}

.review-entry-text {
  font-size: 25rpx;
  letter-spacing: 2rpx;
  color: rgba(28, 25, 23, 0.42);
  border-bottom: 1rpx solid rgba(28, 25, 23, 0.14);
  padding-bottom: 6rpx;
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
.chapter-membership-panel{margin:28rpx 8rpx 34rpx;padding:24rpx;border-left:3rpx solid var(--vermilion);background:rgba(255,250,242,.72)}
.chapter-membership-head{display:flex;align-items:baseline;justify-content:space-between;gap:18rpx}
.chapter-membership-title{color:var(--ink);font-family:var(--font-reading);font-size:27rpx}
.chapter-membership-current{max-width:64%;overflow:hidden;color:var(--ink-mid);font-size:21rpx;text-overflow:ellipsis;white-space:nowrap}
.chapter-membership-actions{display:flex;gap:18rpx;margin-top:18rpx}
.chapter-membership-action{padding:13rpx 18rpx;border:1rpx solid rgba(181,53,42,.55);color:var(--vermilion);font-size:22rpx}
.chapter-membership-action--quiet{border-color:rgba(107,101,96,.3);color:var(--ink-mid)}
.memory-policy{margin:40rpx 8rpx 0;padding:24rpx;border-radius:18rpx;background:rgba(255,250,242,.78)}
.memory-policy-title{display:block;color:#5b4c40;font-size:24rpx}
.memory-policy-hint{display:block;margin-top:8rpx;color:rgba(64,55,47,.5);font-size:20rpx;line-height:1.55}
.memory-policy-row{display:flex;align-items:center;justify-content:space-between;margin-top:20rpx}
.memory-policy-label{color:#40372f;font-size:26rpx}
.memory-switch{width:72rpx;height:40rpx;border-radius:999rpx;background:rgba(102,85,65,.16);position:relative}
.memory-switch--on{background:#9c6447}
.memory-switch-knob{width:32rpx;height:32rpx;border-radius:50%;background:#fffaf3;position:absolute;top:4rpx;left:4rpx}
.memory-switch--on .memory-switch-knob{left:36rpx}
.memory-policy-note{margin-top:18rpx;width:100%;min-height:88rpx;padding:16rpx;box-sizing:border-box;border-radius:14rpx;background:rgba(255,255,255,.72);color:#40372f;font-size:26rpx}
.memory-policy-placeholder{color:rgba(64,55,47,.38)}
.memory-policy-save{margin-top:16rpx;text-align:right;color:#9d6649;font-size:24rpx}
.memory-policy-save--loading{opacity:.45}
.ownership-delete{margin:60rpx auto 24rpx;text-align:center;color:#9e4a43;font-size:24rpx;letter-spacing:.08em;padding:24rpx}

/* 回看主画面以信笺阅读为主，仅此状态使用新展示。 */
.archive-page--review {
  padding-bottom: calc(64rpx + env(safe-area-inset-bottom));
  font-family: 'Noto Serif SC', 'Songti SC', 'STSong', 'SimSun', serif;
}
.archive-page--review .archive-main {
  margin-top: 0;
}
.archive-page--review .archive-stage {
  gap: 0;
}
.archive-page--review .archive-top-safe__logo-text {
  font-family: inherit;
  font-size: 22rpx;
  letter-spacing: 8rpx;
  color: #9e9890;
}
.archive-page--review .archive-close {
  width: 72rpx;
  height: 72rpx;
  border: none;
  background: transparent;
}
.archive-page--review .archive-close__icon {
  width: 40rpx;
  height: 40rpx;
}
.archive-page--review .archive-close__line {
  background: #6b6560;
  height: 2rpx;
}
.archive-page--review .unlock-hero {
  padding: 40rpx 0 0;
}
.archive-page--review .unlock-archive {
  justify-content: center;
  gap: 10rpx;
  margin: 0;
  font-size: 24rpx;
  line-height: 1.8;
  letter-spacing: 2rpx;
  color: #6b6560;
}
.archive-page--review .unlock-archive-no {
  flex-shrink: 0;
  max-width: 58%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-family: inherit;
  font-size: inherit;
  color: inherit;
}
.archive-page--review .unlock-loc-text {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-family: inherit;
  font-size: inherit;
  color: inherit;
}
.archive-page--review .unlock-season {
  margin: 20rpx 0 0;
  font-family: inherit;
  font-size: 24rpx;
  line-height: 1.8;
  font-style: normal;
  letter-spacing: 3rpx;
  color: #6b6560;
}
.archive-page--review .unlock-quote {
  margin: 56rpx 0 0;
  text-align: center;
  font-family: inherit;
  font-size: 60rpx;
  font-weight: 400;
  letter-spacing: 4rpx;
  color: #302e29;
  line-height: 1.65;
  overflow-wrap: anywhere;
}
.archive-page--review .unlock-seal-row {
  margin: 36rpx 0 0;
  justify-content: center;
  gap: 24rpx;
}
.archive-page--review .unlock-seal {
  width: 50rpx;
  height: 50rpx;
  border: 1rpx solid #b5352a;
}
.archive-page--review .unlock-seal-char {
  font-family: inherit;
  font-size: 24rpx;
  color: #b5352a;
}
.archive-page--review .unlock-seal-label {
  font-family: inherit;
  font-size: 24rpx;
  line-height: 1.8;
  letter-spacing: 2rpx;
  color: #8a847c;
}
.archive-page--review .unlock-card {
  margin-top: 48rpx;
  padding: 48rpx 44rpx 36rpx 52rpx;
  border-color: #e3dacc;
  background: #fbf8f1;
  box-shadow: 0 4rpx 10rpx rgba(125,105,75,.1), 0 20rpx 40rpx rgba(125,105,75,.12);
}
.archive-page--review .unlock-card-text {
  display: block;
  font-family: inherit;
  font-size: 30rpx;
  line-height: 2.05;
  letter-spacing: 1rpx;
  color: #6b6560;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
.archive-page--review .unlock-card-vline {
  top: 0;
  bottom: 0;
  background: linear-gradient(180deg,transparent,rgba(181,53,42,.5) 15%,rgba(181,53,42,.5) 85%,transparent);
}
.archive-page--review .unlock-card-corner {
  width: 34rpx;
  height: 34rpx;
  border: none;
  background: linear-gradient(225deg,#f4efe5 49%,#e3dbc9 50%);
  box-shadow: -3rpx 3rpx 5rpx rgba(120,100,70,.16);
}
.unlock-card-where {
  display: block;
  margin-top: 32rpx;
  color: #9e9890;
  font-size: 20rpx;
  line-height: 1.8;
  letter-spacing: 1rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.unlock-media {
  display: block;
  width: 100%;
}
.archive-page--review .reflection-panel {
  position: relative;
  margin-top: 40rpx;
  padding: 36rpx 44rpx 32rpx;
  border-color: #e3dacc;
  background: #fbf8f1;
  box-shadow: 0 4rpx 10rpx rgba(125,105,75,.1),0 16rpx 32rpx rgba(125,105,75,.1);
}
.archive-page--review .reflection-label {
  font-family: inherit;
  font-size: 24rpx;
  line-height: 1.8;
  color: #9e9890;
}
.archive-page--review .reflection-count {
  font-size: 20rpx;
  color: #9e9890;
}
.archive-page--review .reflection-textarea {
  min-height: 148rpx;
  margin-top: 8rpx;
  padding: 0;
  font-family: inherit;
  font-size: 28rpx;
  line-height: 2.05;
  color: #6b6560;
  background: repeating-linear-gradient(180deg,transparent,transparent 58rpx,rgba(200,194,184,.6) 58rpx,rgba(200,194,184,.6) 60rpx);
}
.archive-page--review :deep(.reflection-placeholder) {
  color: #c8c2b8;
}
.archive-page--review .reflection-text {
  font-family: inherit;
  font-size: 28rpx;
  color: #6b6560;
  line-height: 1.9;
}
.archive-page--review .reflection-save {
  width: auto;
  height: auto;
  min-height: 64rpx;
  justify-content: flex-end;
  margin-top: 8rpx;
  border: none;
  color: #b5352a;
  font-size: 22rpx;
}
.archive-page--review .review-entry {
  margin-top: 48rpx;
  text-align: center;
}
.archive-page--review .review-entry-text {
  display: inline-block;
  padding-bottom: 6rpx;
  font-family: inherit;
  font-size: 26rpx;
  line-height: 1.8;
  color: #6b6560;
  border-bottom: 1rpx solid #bcb5ab;
}
.archive-page--review .unlock-actions {
  margin-top: 56rpx;
  padding-bottom: 0;
  gap: 0;
}
.archive-page--review .unlock-cta {
  min-height: 96rpx;
  box-sizing: border-box;
  justify-content: center;
  padding: 20rpx 52rpx;
  border: 2rpx solid rgba(255,255,255,.9);
  border-radius: 50rpx;
  background: #f5f1e8;
  box-shadow: 0 12rpx 36rpx rgba(125,105,75,.14);
}
.archive-page--review .unlock-cta-text {
  font-family: inherit;
  font-size: 30rpx;
  color: #403c37;
  letter-spacing: 4rpx;
}
.archive-page--review .unlock-replied-slot {
  width: 100%;
  box-sizing: border-box;
  background: #fbf8f1;
  border-color: #e3dacc;
}
.review-more {
  margin-top: 48rpx;
}
.review-more-toggle {
  min-height: 72rpx;
  padding: 8rpx 0;
  border: none;
  background: transparent;
  color: #9e9890;
  font-family: inherit;
  font-size: 21rpx;
  text-align: center;
  line-height: 2;
}
.review-more-toggle::after {
  border: none;
}
.review-more-content {
  padding-top: 16rpx;
}
.review-more-content .reflection-block {
  margin-top: 24rpx;
}

/* 回看参考入口与轻量备注纸面，仅此入口使用。 */
.memory-note-entry {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 20rpx;
  width: 100%;
  min-height: 80rpx;
  padding: 12rpx 0;
  background: transparent;
  color: #aaa398;
  font-family: inherit;
  font-size: 23rpx;
  line-height: 1.6;
  text-align: left;
}
.memory-note-entry::after, .memory-note-panel button::after { border: none; }
.memory-note-entry-label { min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.memory-note-indicator { position: relative; flex-shrink: 0; width: 58rpx; height: 32rpx; border: 1rpx solid #cfc8bd; border-radius: 32rpx; }
.memory-note-indicator > view { position: absolute; top: 4rpx; left: 4rpx; width: 24rpx; height: 24rpx; border-radius: 50%; background: #c7c0b5; }
.memory-note-indicator--on { border-color: #bd9584; }
.memory-note-indicator--on > view { left: 30rpx; background: #9c6447; }
.memory-note-layer { position: fixed; inset: 0; z-index: 1200; display: flex; align-items: flex-end; }
.memory-note-mask { position: absolute; inset: 0; background: rgba(96,89,79,.42); }
.memory-note-panel {
  position: relative;
  width: 100%;
  box-sizing: border-box;
  max-height: 90vh;
  overflow-y: auto;
  padding: 22rpx 46rpx calc(42rpx + env(safe-area-inset-bottom));
  border-radius: 28rpx 28rpx 0 0;
  background: #f9f5ed;
  box-shadow: 0 -12rpx 48rpx rgba(77,65,48,.08);
  font-family: 'Noto Serif SC', 'Songti SC', 'STSong', 'SimSun', serif;
}
.memory-note-handle { width: 70rpx; height: 8rpx; margin: 0 auto 48rpx; border-radius: 8rpx; background: #c9c2b8; }
.memory-note-close { position: absolute; top: 26rpx; right: 24rpx; width: 72rpx; height: 72rpx; padding: 0; background: transparent; color: #6b6560; font-size: 44rpx; font-weight: 300; line-height: 68rpx; }
.memory-note-kicker { display: block; text-align: center; color: #aaa398; font-size: 21rpx; letter-spacing: 5rpx; }
.memory-note-title { display: block; margin: 26rpx 0 38rpx; text-align: center; color: #403c37; font-size: 34rpx; line-height: 1.6; }
.memory-note-toggle { display: flex; align-items: center; justify-content: space-between; min-height: 72rpx; padding: 0; background: transparent; color: #817970; font-family: inherit; font-size: 25rpx; }
.memory-note-input { width: 100%; height: 160rpx; margin-top: 16rpx; padding: 22rpx; box-sizing: border-box; border: 1rpx solid #e5ddd0; background: #fcfaf5; color: #6b6560; font-family: inherit; font-size: 27rpx; line-height: 1.8; }
.memory-note-hint { display: block; margin-top: 18rpx; color: #aaa398; font-size: 20rpx; line-height: 1.7; }
.memory-note-save { display: flex; align-items: center; justify-content: center; gap: 20rpx; width: 280rpx; min-height: 84rpx; margin: 30rpx auto 0; border: 2rpx solid #fff; border-radius: 48rpx; background: #f5f0e7; color: #403c37; font-family: inherit; font-size: 27rpx; letter-spacing: 3rpx; box-shadow: 0 10rpx 26rpx rgba(125,105,75,.12); }
.memory-note-panel button[disabled] { opacity: .5; }

/* 已留下：沿现有暖纸/信笺/浅色动作语言，仅作用于 SAVED。 */
.archive-page--saved {
  box-sizing: border-box;
  padding-bottom: calc(64rpx + env(safe-area-inset-bottom));
  color: #302e29;
  font-family: 'Noto Serif SC', 'Songti SC', 'STSong', 'SimSun', Georgia, serif;
}
.archive-page--saved .archive-main { margin-top: 28rpx; }
.archive-page--saved .archive-stage { gap: 0; }
.archive-page--saved .archive-top-safe__logo-text { color: #9e9890; font-family: inherit; font-size: 22rpx; letter-spacing: 8rpx; }
.archive-page--saved .archive-close { width: 72rpx; height: 72rpx; border: 0; background: transparent; }
.archive-page--saved .archive-close__icon { width: 36rpx; height: 36rpx; }
.archive-page--saved .archive-close__line { height: 2rpx; background: #6b6560; }
.saved-hero { padding-top: 42rpx; }
.saved-confirmation { display: block; text-align: center; color: #4c4842; font-size: 36rpx; font-weight: 400; line-height: 1.7; letter-spacing: .16em; text-indent: .16em; }
.saved-letter { position: relative; margin-top: 46rpx; padding: 48rpx 44rpx 64rpx; box-sizing: border-box; border: 1rpx solid rgba(188,174,152,.28); border-radius: 2rpx; background: #fcf9f4; box-shadow: 0 2rpx 4rpx rgba(125,105,75,.1), 0 18rpx 44rpx rgba(125,105,75,.12); }
.saved-letter-redline { position: absolute; left: 0; top: 0; bottom: 0; width: 3rpx; background: linear-gradient(to bottom, transparent, rgba(181,53,42,.4) 15%, rgba(181,53,42,.4) 85%, transparent); pointer-events: none; }
.saved-letter-fold { position: absolute; top: -1rpx; right: -1rpx; width: 34rpx; height: 34rpx; background: linear-gradient(225deg, #f4efe5 49%, #e3dbc9 50%); box-shadow: -3rpx 3rpx 5rpx rgba(120,100,70,.16); pointer-events: none; }
.saved-record-title { display: block; margin-bottom: 24rpx; padding-right: 10rpx; color: #302e29; font-size: 40rpx; font-weight: 400; line-height: 1.6; letter-spacing: .1em; overflow-wrap: anywhere; }
.saved-record-body { display: block; color: #6b6560; font-size: 30rpx; font-weight: 300; line-height: 2; letter-spacing: .04em; white-space: pre-wrap; overflow-wrap: anywhere; }
.saved-record-time { display: block; margin-top: 34rpx; padding-right: 72rpx; color: #9e9890; font-size: 23rpx; font-weight: 300; line-height: 1.8; letter-spacing: .04em; overflow-wrap: anywhere; }
.saved-stamp { position: absolute; right: 30rpx; bottom: 26rpx; display: flex; align-items: center; justify-content: center; width: 50rpx; height: 50rpx; box-sizing: border-box; border: 1rpx solid rgba(181,53,42,.65); border-radius: 50%; color: #b5352a; font-size: 25rpx; font-weight: 300; }
.saved-continue { display: flex; align-items: center; justify-content: center; gap: 18rpx; width: 100%; min-height: 94rpx; margin: 38rpx 0 0; padding: 18rpx 36rpx; box-sizing: border-box; border: 2rpx solid #fffdfa; border-radius: 99rpx; background: #f3eee4; color: #6b6560; font-family: inherit; font-size: 29rpx; font-weight: 300; line-height: 1.6; letter-spacing: .12em; box-shadow: 0 8rpx 26rpx rgba(125,105,75,.1); }
.saved-continue::after { border: none; }
.saved-action-dot { flex-shrink: 0; width: 8rpx; height: 8rpx; border-radius: 50%; background: #b5352a; opacity: .7; }
.archive-page--saved .chapter-membership-panel { margin: 52rpx 0 0; padding: 28rpx; border: 1rpx solid rgba(188,174,152,.28); border-radius: 2rpx; background: rgba(252,249,244,.6); }
.archive-page--saved .chapter-membership-head { align-items: flex-start; flex-wrap: wrap; gap: 14rpx; }
.archive-page--saved .chapter-membership-title { color: #6b6560; font-family: inherit; font-size: 26rpx; line-height: 1.8; }
.archive-page--saved .chapter-membership-current { max-width: 100%; color: #9e9890; font-size: 23rpx; line-height: 1.8; white-space: normal; overflow-wrap: anywhere; }
.archive-page--saved .chapter-membership-actions { flex-wrap: wrap; margin-top: 20rpx; gap: 18rpx; }
.archive-page--saved .chapter-membership-action { padding: 12rpx 24rpx; border: 1rpx solid rgba(107,101,96,.35); border-radius: 2rpx; color: #6b6560; font-size: 23rpx; line-height: 1.6; }
.archive-page--saved .memory-policy { display: flex; flex-direction: column; margin: 28rpx 0 0; padding: 28rpx; border: 1rpx solid rgba(188,174,152,.28); border-radius: 2rpx; background: rgba(252,249,244,.6); }
.archive-page--saved .memory-policy-title { color: #6b6560; font-size: 24rpx; line-height: 1.8; letter-spacing: .06em; }
.archive-page--saved .memory-policy-hint { color: #9e9890; font-size: 21rpx; line-height: 1.8; }
.archive-page--saved .memory-policy-row { gap: 20rpx; margin-top: 22rpx; }
.archive-page--saved .memory-policy-label { flex: 1; min-width: 0; color: #6b6560; font-size: 26rpx; line-height: 1.8; }
.archive-page--saved .memory-switch { flex-shrink: 0; width: 64rpx; height: 36rpx; box-sizing: border-box; border: 1rpx solid #c8c2b8; background: transparent; }
.archive-page--saved .memory-switch-knob { width: 26rpx; height: 26rpx; top: 4rpx; left: 4rpx; background: #c8c2b8; }
.archive-page--saved .memory-switch--on { border-color: #9e9890; background: #e4ded2; }
.archive-page--saved .memory-switch--on .memory-switch-knob { left: 32rpx; background: #6b6560; }
.archive-page--saved .memory-policy-note { width: 100%; min-height: 132rpx; margin-top: 22rpx; padding: 20rpx; box-sizing: border-box; border: 1rpx solid rgba(188,174,152,.35); border-radius: 2rpx; background: #faf7f2; color: #6b6560; font-family: inherit; font-size: 26rpx; line-height: 1.9; }
.archive-page--saved .memory-policy-save { display: inline-flex; align-items: center; justify-content: center; align-self: flex-end; min-height: 68rpx; margin-top: 20rpx; padding: 10rpx 28rpx; box-sizing: border-box; border: 1rpx solid rgba(188,174,152,.35); border-radius: 99rpx; background: #f3eee4; color: #6b6560; font-size: 24rpx; line-height: 1.8; }
.archive-page--saved .ownership-delete { margin: 44rpx auto 0; padding: 22rpx; min-height: 80rpx; box-sizing: border-box; color: #b5352a; opacity: .75; font-size: 23rpx; line-height: 1.8; letter-spacing: .14em; }
</style>
