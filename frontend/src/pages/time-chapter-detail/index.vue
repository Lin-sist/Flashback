<script setup lang="ts">
import { onLoad, onShow } from '@dcloudio/uni-app'
import { computed, ref } from 'vue'
import EmptyState from '../../components/common/EmptyState.vue'
import AppTopSafeBar from '../../components/common/AppTopSafeBar.vue'
import PreviewModeNotice from '../../components/common/PreviewModeNotice.vue'
import PrimaryButton from '../../components/common/PrimaryButton.vue'
import { hasPreviewSession, showPreviewReadonlyToast } from '../../features/preview/preview-session'
import { recordService, timeChapterService } from '../../services'
import { useTimeChapterStore } from '../../stores'
import type { RecordListItemVO, TimeChapterOrder } from '../../types'
import { TimeChapterStatus } from '../../types'
import { formatDateTime, formatDayText, hasAuthenticatedSession, toUserMessage } from '../../utils'

const chapterStore = useTimeChapterStore()
const chapterId = ref<number | null>(null)
const order = ref<TimeChapterOrder>('DESC')
const loadFailed = ref(false)
const editing = ref(false)
const editName = ref('')
const editNote = ref('')
const mutationLoading = ref(false)

const chapter = computed(() => {
  if (!chapterId.value || !chapterStore.detail) return null
  return Number(chapterStore.detail.id) === chapterId.value ? chapterStore.detail : null
})

const members = computed(() => chapter.value?.members.list || [])
const statusText = computed(() => chapter.value?.status === TimeChapterStatus.ACTIVE ? '进行中' : '已结束')
const coverageText = computed(() => {
  if (!chapter.value?.coverageStartAt || !chapter.value.coverageEndAt) return '片段覆盖时间待形成'
  const start = formatDayText(chapter.value.coverageStartAt)
  const end = formatDayText(chapter.value.coverageEndAt)
  return start === end ? `片段覆盖 ${start}` : `片段覆盖 ${start} — ${end}`
})

const ensureVisible = () => {
  if (!hasAuthenticatedSession() && !hasPreviewSession()) {
    uni.reLaunch({ url: '/pages/login/index' })
    return false
  }
  return true
}

const loadDetail = async () => {
  if (!ensureVisible() || !chapterId.value) return
  loadFailed.value = false
  try {
    await chapterStore.fetchDetail(chapterId.value, order.value, 1, 50)
  } catch (error) {
    loadFailed.value = true
    uni.showToast({ title: toUserMessage(error), icon: 'none' })
  }
}

const loadMoreMembers = async () => {
  if (!chapter.value || chapterStore.loading || members.value.length >= chapter.value.members.total) return
  const nextPage = chapter.value.members.pageNum + 1
  try {
    await chapterStore.fetchDetail(chapter.value.id, order.value, nextPage, 50, true)
  } catch (error) {
    uni.showToast({ title: toUserMessage(error), icon: 'none' })
  }
}

const retry = () => loadDetail()
const closePage = () => uni.navigateBack({ delta: 1 })

const toggleOrder = () => {
  order.value = order.value === 'DESC' ? 'ASC' : 'DESC'
  loadDetail()
}

const openRecord = (item: RecordListItemVO) => {
  const target = item.status === 'DRAFT'
    ? `/pages/record-editor/index?id=${item.id}&source=archive`
    : `/pages/record-detail/index?id=${item.id}&source=archive`
  uni.navigateTo({ url: target })
}

const beginEdit = () => {
  if (!chapter.value) return
  editName.value = chapter.value.name
  editNote.value = chapter.value.note || ''
  editing.value = true
}

const cancelEdit = () => {
  editing.value = false
  editName.value = ''
  editNote.value = ''
}

const saveEdit = async () => {
  if (!chapter.value) return
  if (!editName.value.trim()) {
    uni.showToast({ title: '篇章名称不能为空', icon: 'none' })
    return
  }
  mutationLoading.value = true
  try {
    await timeChapterService.update(chapter.value.id, {
      name: editName.value.trim(),
      note: editNote.value.trim() || null,
      expectedVersion: chapter.value.version,
    })
    cancelEdit()
    await loadDetail()
  } catch (error) {
    uni.showToast({ title: toUserMessage(error), icon: 'none' })
  } finally {
    mutationLoading.value = false
  }
}

const changeLifecycle = async (next: TimeChapterStatus) => {
  if (!chapter.value) return
  mutationLoading.value = true
  try {
    if (next === TimeChapterStatus.ENDED) {
      await timeChapterService.end(chapter.value.id, { expectedVersion: chapter.value.version })
    } else {
      await timeChapterService.reopen(chapter.value.id, { expectedVersion: chapter.value.version })
    }
    await loadDetail()
  } catch (error) {
    uni.showToast({ title: toUserMessage(error), icon: 'none' })
  } finally {
    mutationLoading.value = false
  }
}

const confirmLifecycle = (next: TimeChapterStatus) => {
  if (!chapter.value) return
  if (hasPreviewSession()) {
    showPreviewReadonlyToast()
    return
  }
  uni.showModal({
    title: next === TimeChapterStatus.ENDED ? '结束这段时间？' : '重新打开这段时间？',
    content: next === TimeChapterStatus.ENDED ? '结束后仍可浏览和移出成员，之后可以重新打开。' : '重新打开后可以继续加入记录。',
    success: (result) => { if (result.confirm) changeLifecycle(next) },
  })
}

const confirmDelete = () => {
  if (!chapter.value) return
  if (hasPreviewSession()) {
    showPreviewReadonlyToast()
    return
  }
  uni.showModal({
    title: '删除这个篇章？',
    content: `当前篇章包含 ${chapter.value.memberCount} 个成员。\n只删除篇章，不删除记录。`,
    confirmColor: '#b5352a',
    success: async (result) => {
      if (!result.confirm || !chapter.value) return
      mutationLoading.value = true
      try {
        await timeChapterService.delete(chapter.value.id, { expectedVersion: chapter.value.version })
        uni.navigateBack({ delta: 1 })
      } catch (error) {
        uni.showToast({ title: toUserMessage(error), icon: 'none' })
      } finally {
        mutationLoading.value = false
      }
    },
  })
}

// 只组合既有记录查询和篇章成员契约；选择与确认不会提前改变归属。
const memberMode = ref<'add' | 'remove' | null>(null)
const candidateRecords = ref<RecordListItemVO[]>([])
const candidatePage = ref(0)
const candidateTotal = ref(0)
const candidateLoading = ref(false)
const candidateFailed = ref(false)
const confirmingMember = ref(false)
const pageBusy = computed(() => mutationLoading.value || chapterStore.loading || confirmingMember.value)
const pickerRecords = computed(() => memberMode.value === 'remove' ? members.value : candidateRecords.value.filter(
  (item) => item.status !== 'DRAFT' && Number(item.chapter?.id) !== chapter.value?.id,
))
const pickerHasMore = computed(() => memberMode.value === 'remove'
  ? members.value.length < (chapter.value?.members.total || 0)
  : candidateRecords.value.length < candidateTotal.value)

const loadCandidates = async (append = false) => {
  if (candidateLoading.value || memberMode.value !== 'add') return
  candidateLoading.value = true
  candidateFailed.value = false
  const pageNum = append ? candidatePage.value + 1 : 1
  try {
    const result = await recordService.getRecordList('ALL', { pageNum, pageSize: 50 })
    candidateRecords.value = append ? [...candidateRecords.value, ...result.list] : result.list
    candidatePage.value = result.pageNum
    candidateTotal.value = result.total
  } catch (error) {
    candidateFailed.value = true
    uni.showToast({ title: toUserMessage(error), icon: 'none' })
  } finally {
    candidateLoading.value = false
  }
}

const openMemberPicker = async (mode: 'add' | 'remove') => {
  if (!chapter.value || pageBusy.value || editing.value || memberMode.value) return
  if (hasPreviewSession()) { showPreviewReadonlyToast(); return }
  if (!ensureVisible()) return
  if (mode === 'add' && chapter.value.status !== TimeChapterStatus.ACTIVE) return
  if (mode === 'remove' && chapter.value.memberCount === 0) return
  candidateFailed.value = false
  memberMode.value = mode
  if (mode === 'add') {
    candidateRecords.value = []
    candidatePage.value = 0
    candidateTotal.value = 0
    await loadCandidates()
  }
}

const closeMemberPicker = () => {
  if (pageBusy.value || candidateLoading.value) return
  memberMode.value = null
}

const loadMorePicker = () => {
  if (pageBusy.value) return
  return memberMode.value === 'remove' ? loadMoreMembers() : loadCandidates(candidatePage.value > 0)
}

const chooseMember = (item: RecordListItemVO) => {
  const current = chapter.value
  const mode = memberMode.value
  if (!current || !mode || pageBusy.value || candidateLoading.value || !pickerRecords.value.some((row) => row.id === item.id)) return
  if (hasPreviewSession()) { showPreviewReadonlyToast(); return }
  if (!ensureVisible()) return
  if (mode === 'add' && (current.status !== TimeChapterStatus.ACTIVE || item.status === 'DRAFT' || Number(item.chapter?.id) === current.id)) return
  confirmingMember.value = true
  const transfers = mode === 'add' && item.chapter ? [{ recordId: item.id, fromChapterId: item.chapter.id }] : []
  uni.showModal({
    title: mode === 'remove' ? '移出这条记录？' : transfers.length ? '确认转移篇章' : '确认加入篇章',
    content: mode === 'remove' ? '只从当前篇章移出，不会删除记录。'
      : item.chapter ? `从“${item.chapter.name}”转移到“${current.name}”？` : `把这条记录加入“${current.name}”？`,
    confirmColor: '#b5352a',
    success: async (result) => {
      confirmingMember.value = false
      if (!result.confirm || mutationLoading.value || hasPreviewSession() || !hasAuthenticatedSession()) return
      mutationLoading.value = true
      try {
        if (mode === 'remove') {
          await timeChapterService.removeMembers(current.id, { recordIds: [item.id], expectedVersion: current.version })
        } else {
          await timeChapterService.addMembers(current.id, { recordIds: [item.id], expectedVersion: current.version, transfers })
        }
        memberMode.value = null
        await loadDetail()
      } catch (error) {
        // 不以本地移除冒充成功；重新读取当前版本，下一次由用户重新选择确认。
        memberMode.value = null
        await loadDetail()
        uni.showToast({ title: toUserMessage(error), icon: 'none' })
      } finally {
        mutationLoading.value = false
      }
    },
    fail: () => { confirmingMember.value = false },
  })
}

const openMore = () => {
  if (!chapter.value || pageBusy.value || editing.value || memberMode.value) return
  uni.showActionSheet({
    itemList: ['编辑篇章', order.value === 'DESC' ? '改为正序浏览' : '改为倒序浏览', '篇章信息', '只删除篇章'],
    success: ({ tapIndex }) => {
      if (pageBusy.value || editing.value || memberMode.value) return
      if (tapIndex === 0) {
        if (hasPreviewSession()) { showPreviewReadonlyToast(); return }
        beginEdit()
      } else if (tapIndex === 1) toggleOrder()
      else if (tapIndex === 2 && chapter.value) uni.showModal({
        title: '篇章信息', showCancel: false,
        content: `${coverageText.value}\n建立于 ${formatDateTime(chapter.value.createdAt)}${chapter.value.endedAt ? `\n结束于 ${formatDateTime(chapter.value.endedAt)}` : ''}`,
      })
      else if (tapIndex === 3) confirmDelete()
    },
  })
}

const submitEdit = () => {
  if (pageBusy.value) return
  if (hasPreviewSession()) { showPreviewReadonlyToast(); return }
  return saveEdit()
}

onLoad((options) => {
  const parsed = Number((options as { id?: string }).id)
  if (!Number.isInteger(parsed) || parsed <= 0) {
    loadFailed.value = true
    return
  }
  chapterId.value = parsed
  loadDetail()
})

onShow(() => {
  if (chapterId.value && !chapter.value) loadDetail()
})
</script>

<template>
  <view class="chapter-page">
    <AppTopSafeBar transparent padding-x="54rpx" side-width="96rpx">
      <template #left><button class="chapter-back" @tap="closePage"><view class="back-chevron" /><text>返回</text></button></template>
      <template #right><button v-if="chapter && !loadFailed" class="chapter-more" :disabled="pageBusy || editing" @tap="openMore">更多</button></template>
    </AppTopSafeBar>
    <PreviewModeNotice />

    <scroll-view scroll-y class="chapter-scroll" enhanced :show-scrollbar="false">
      <view v-if="chapterStore.loading && !chapter" class="chapter-state">
        <EmptyState text="正在打开这段时间…" />
      </view>
      <view v-else-if="loadFailed || !chapter" class="chapter-state">
        <EmptyState text="篇章暂时不可用" />
        <PrimaryButton text="重试加载" ghost @tap="retry" />
      </view>
      <view v-else class="chapter-content">
        <view v-if="!editing" class="chapter-heading">
          <text class="chapter-name">{{ chapter.name }}</text>
          <text class="chapter-status">{{ statusText }}</text>
        </view>
        <view v-else class="chapter-edit-form">
          <input v-model="editName" maxlength="100" :disabled="mutationLoading" class="chapter-input" placeholder="篇章名称" />
          <textarea v-model="editNote" maxlength="1000" auto-height :disabled="mutationLoading" class="chapter-note-input" placeholder="写一句自述（可选）" :adjust-position="true" :cursor-spacing="140" />
          <view class="chapter-edit-actions">
            <button :disabled="mutationLoading" @tap="cancelEdit">取消</button>
            <button class="edit-save" :disabled="pageBusy" @tap="submitEdit">{{ mutationLoading ? '保存中…' : '保存' }}</button>
          </view>
        </view>
        <text v-if="!editing && chapter.note" class="chapter-note">{{ chapter.note }}</text>
        <text class="chapter-meta">始于 {{ formatDayText(chapter.createdAt) }} · 汇集 {{ chapter.memberCount }} 篇此刻</text>

        <view v-if="members.length" class="member-list">
          <button v-for="member in members" :key="member.id" class="member-card" :disabled="pageBusy || editing" @tap="openRecord(member)">
            <view class="member-fold" />
            <text class="member-title">{{ member.title || '未命名片段' }}</text>
            <text class="member-preview">{{ member.contentPreview || '一段由图片或声音留下的记录' }}</text>
          </button>
          <button v-if="members.length < chapter.members.total" class="chapter-load-more" :disabled="chapterStore.loading" @tap="loadMoreMembers">
            {{ chapterStore.loading ? '正在加载…' : '加载更多片段' }}
          </button>
        </view>
        <view v-else class="chapter-empty"><EmptyState text="还没有片段，记录仍然保留在我的记录中" /></view>
      </view>
    </scroll-view>

    <view v-if="chapter && !loadFailed && !editing" class="chapter-actions-shell">
      <view class="chapter-actions">
        <button class="chapter-action add-entry" :disabled="pageBusy || chapter.status !== TimeChapterStatus.ACTIVE" @tap="openMemberPicker('add')">添加记录</button>
        <view class="action-divider" />
        <button class="chapter-action remove-entry" :disabled="pageBusy || chapter.memberCount === 0" @tap="openMemberPicker('remove')">移出</button>
        <view class="action-divider" />
        <button class="chapter-action lifecycle-entry" :disabled="pageBusy" @tap="confirmLifecycle(chapter.status === TimeChapterStatus.ACTIVE ? TimeChapterStatus.ENDED : TimeChapterStatus.ACTIVE)">{{ chapter.status === TimeChapterStatus.ACTIVE ? '结束篇章' : '重新打开' }}</button>
      </view>
    </view>

    <view v-if="memberMode" class="member-layer" @tap="closeMemberPicker">
      <view class="member-sheet" role="dialog" aria-modal="true" :aria-label="memberMode === 'add' ? '添加记录' : '移出记录'" @tap.stop>
        <view class="sheet-handle" />
        <view class="sheet-heading">
          <text class="sheet-title">{{ memberMode === 'add' ? '把此刻放进篇章' : '选择要移出的记录' }}</text>
          <button class="sheet-close" :disabled="pageBusy || candidateLoading" @tap="closeMemberPicker">关闭</button>
        </view>
        <text class="sheet-description">{{ memberMode === 'add' ? '选择已留下的记录，加入前会再向你确认。' : '只移出当前篇章，记录仍然为你保管。' }}</text>
        <scroll-view scroll-y class="picker-scroll" :show-scrollbar="false">
          <view class="picker-content">
            <text v-if="candidateLoading && !candidatePage" class="picker-state">正在翻找记录…</text>
            <text v-else-if="!pickerRecords.length && !candidateFailed" class="picker-state">{{ pickerHasMore ? '这一页没有可选记录，可以继续翻找。' : '暂时没有可选记录' }}</text>
            <button v-for="item in pickerRecords" :key="item.id" class="picker-record" :disabled="pageBusy || candidateLoading" @tap="chooseMember(item)">
              <text class="picker-record-title">{{ item.title || '未命名片段' }}</text>
              <text class="picker-record-meta">{{ formatDayText(item.createdAt) }}{{ memberMode === 'add' && item.chapter ? ' · 来自其他篇章，需确认转移' : '' }}</text>
            </button>
            <button v-if="candidateFailed || pickerHasMore" class="chapter-load-more picker-more" :disabled="pageBusy || candidateLoading" @tap="loadMorePicker">
              {{ candidateLoading || chapterStore.loading ? '正在加载…' : candidateFailed ? '加载失败，轻触重试' : '继续翻找' }}
            </button>
          </view>
        </scroll-view>
      </view>
    </view>
  </view>
</template>

<style scoped>
.chapter-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
  background: linear-gradient(170deg, #faf7f2 0%, #f5f0e8 58%, #f0ebe0 100%);
  color: #302e29;
  font-family: 'Noto Serif SC', 'Songti SC', 'STSong', 'SimSun', Georgia, serif;
  font-weight: 300;
}
button { margin: 0; padding: 0; border: 0; border-radius: 0; background: transparent; color: inherit; font: inherit; line-height: inherit; }
button::after { border: 0; }
button[disabled] { background: transparent; color: #b7afa5; opacity: .55; }
.chapter-back { display: flex; align-items: center; gap: 14rpx; min-height: 80rpx; color: #6b6560; font-size: 27rpx; letter-spacing: 2rpx; }
.back-chevron { width: 18rpx; height: 18rpx; border-left: 2rpx solid #6b6560; border-bottom: 2rpx solid #6b6560; transform: rotate(45deg); }
.chapter-more { min-height: 80rpx; min-width: 80rpx; color: #9e9890; font-size: 23rpx; letter-spacing: 3rpx; }
.chapter-scroll { flex: 1; height: 0; min-height: 0; }
.chapter-content { box-sizing: border-box; padding: 44rpx 54rpx 40rpx; }
.chapter-heading { text-align: center; }
.chapter-name { display: block; font-size: 50rpx; font-weight: 400; line-height: 1.55; letter-spacing: .14em; text-indent: .14em; overflow-wrap: anywhere; }
.chapter-status { display: inline-flex; align-items: center; box-sizing: border-box; margin-top: 30rpx; min-height: 54rpx; padding: 6rpx 31rpx; border: 1rpx solid rgba(107,101,96,.5); border-radius: 99rpx; color: #817a72; font-size: 23rpx; letter-spacing: .16em; text-indent: .16em; }
.chapter-note { display: block; margin-top: 50rpx; font-size: 32rpx; line-height: 2.1; letter-spacing: .05em; color: #817a72; white-space: pre-wrap; overflow-wrap: anywhere; }
.chapter-meta { display: block; margin-top: 38rpx; text-align: center; font-size: 23rpx; line-height: 1.8; letter-spacing: .08em; color: #b7afa5; overflow-wrap: anywhere; }
.member-card { display: block; position: relative; box-sizing: border-box; width: 100%; margin-top: 34rpx; padding: 38rpx 42rpx 38rpx 50rpx; border: 1rpx solid rgba(188,174,152,.28); background: #fcf9f4; box-shadow: 0 16rpx 40rpx rgba(91,72,43,.10), 0 2rpx 4rpx rgba(91,72,43,.04); text-align: left; }
.member-card[disabled] { background: #fcf9f4; opacity: .7; }
.member-fold { position: absolute; top: 0; right: 0; width: 36rpx; height: 36rpx; background: linear-gradient(45deg,#e4ddca 0%,#e4ddca 49%,#f5f0e8 50%,#f5f0e8 100%); box-shadow: -3rpx 3rpx 5rpx rgba(91,72,43,.10); pointer-events: none; }
.member-title { display: block; padding-right: 8rpx; color: #4c4842; font-size: 33rpx; font-weight: 400; line-height: 1.6; letter-spacing: .06em; overflow-wrap: anywhere; }
.member-preview { display: -webkit-box; margin-top: 18rpx; overflow: hidden; -webkit-box-orient: vertical; -webkit-line-clamp: 2; white-space: pre-wrap; font-size: 26rpx; line-height: 1.8; letter-spacing: .03em; color: #9e9890; overflow-wrap: anywhere; }
.chapter-load-more { width: 100%; min-height: 84rpx; margin-top: 24rpx; font-size: 25rpx; line-height: 1.8; color: #9e9890; }
.chapter-state { padding: 120rpx 54rpx 40rpx; }
.chapter-empty { padding: 52rpx 0; }
.chapter-actions-shell { flex-shrink: 0; padding: 0 24rpx calc(30rpx + env(safe-area-inset-bottom)); }
.chapter-actions { display: flex; align-items: center; min-height: 104rpx; border-radius: 24rpx; border: 1rpx solid rgba(188,174,152,.35); background: #f7f2e9; box-shadow: 0 -4rpx 35rpx rgba(125,105,75,.10); }
.chapter-action { display: flex; flex: 1; align-items: center; justify-content: center; min-width: 0; min-height: 104rpx; padding: 12rpx 6rpx; box-sizing: border-box; color: #9e9890; font-size: 27rpx; letter-spacing: .18em; text-indent: .18em; }
.action-divider { flex-shrink: 0; width: 1rpx; height: 38rpx; background: rgba(188,174,152,.35); }
.chapter-edit-form { padding-top: 4rpx; }
.chapter-input, .chapter-note-input { width: 100%; box-sizing: border-box; padding: 18rpx 20rpx; border: 1rpx solid #d8d0c5; background: #fcf9f4; color: #4c4842; font: inherit; font-size: 30rpx; }
.chapter-input { height: 88rpx; }
.chapter-note-input { margin-top: 24rpx; min-height: 192rpx; line-height: 2; }
.chapter-edit-actions { display: flex; justify-content: flex-end; gap: 36rpx; margin-top: 20rpx; color: #9e9890; font-size: 27rpx; }
.chapter-edit-actions button { min-height: 80rpx; min-width: 100rpx; }
.edit-save { color: #b5352a; }
.member-layer { position: fixed; inset: 0; z-index: 100; display: flex; align-items: flex-end; background: rgba(48,46,41,.28); }
.member-sheet { display: flex; flex-direction: column; width: 100%; height: 68vh; box-sizing: border-box; padding: 18rpx 54rpx calc(24rpx + env(safe-area-inset-bottom)); border-radius: 30rpx 30rpx 0 0; background: #faf7f2; box-shadow: 0 -10rpx 48rpx rgba(48,46,41,.10); }
.sheet-handle { flex-shrink: 0; width: 68rpx; height: 6rpx; margin: 0 auto 32rpx; border-radius: 9rpx; background: #c8c2b8; }
.sheet-heading { display: flex; flex-shrink: 0; align-items: center; justify-content: space-between; gap: 20rpx; }
.sheet-title { flex: 1; min-width: 0; font-size: 34rpx; overflow-wrap: anywhere; }
.sheet-close { flex-shrink: 0; min-width: 80rpx; min-height: 80rpx; color: #9e9890; font-size: 25rpx; }
.sheet-description { flex-shrink: 0; display: block; margin: 10rpx 0 24rpx; color: #9e9890; font-size: 24rpx; line-height: 1.8; }
.picker-scroll { flex: 1; height: 0; min-height: 0; }
.picker-content { padding-bottom: 30rpx; }
.picker-state { display: block; padding: 60rpx 0; text-align: center; font-size: 26rpx; color: #9e9890; line-height: 1.8; }
.picker-record { display: block; width: 100%; padding: 28rpx 0; border-bottom: 1rpx solid rgba(188,174,152,.28); text-align: left; }
.picker-record-title { display: block; color: #4c4842; font-size: 30rpx; line-height: 1.6; overflow-wrap: anywhere; }
.picker-record-meta { display: block; margin-top: 10rpx; color: #9e9890; font-size: 22rpx; line-height: 1.8; overflow-wrap: anywhere; }
@media (max-width: 340px) {
  .chapter-action { font-size: 25rpx; letter-spacing: .10em; }
}
</style>
