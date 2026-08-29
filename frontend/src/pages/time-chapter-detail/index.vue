<script setup lang="ts">
import { onLoad, onShow } from '@dcloudio/uni-app'
import { computed, ref } from 'vue'
import EmptyState from '../../components/common/EmptyState.vue'
import PaperContainer from '../../components/common/PaperContainer.vue'
import PreviewModeNotice from '../../components/common/PreviewModeNotice.vue'
import PrimaryButton from '../../components/common/PrimaryButton.vue'
import { hasPreviewSession, showPreviewReadonlyToast } from '../../features/preview/preview-session'
import { useWechatNavMetrics } from '../../composables/useWechatNavMetrics'
import { timeChapterService } from '../../services'
import { useTimeChapterStore } from '../../stores'
import type { RecordListItemVO, TimeChapterOrder } from '../../types'
import { TimeChapterStatus } from '../../types'
import { formatDateTime, formatDayText, hasAuthenticatedSession, toUserMessage } from '../../utils'

const { statusBarHeight } = useWechatNavMetrics()
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

const topStyle = computed(() => ({ paddingTop: `${statusBarHeight.value}px` }))
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
    <PreviewModeNotice />
    <view class="chapter-top" :style="topStyle">
      <view class="chapter-back" @tap="closePage">‹ <text>返回</text></view>
      <text class="chapter-top__title">时间篇章</text>
      <view class="chapter-top__spacer" />
    </view>

    <scroll-view scroll-y class="chapter-scroll" enhanced show-scrollbar="false">
      <view v-if="chapterStore.loading && !chapter" class="chapter-state">
        <EmptyState text="正在打开这段时间…" />
      </view>
      <view v-else-if="loadFailed || !chapter" class="chapter-state">
        <EmptyState text="篇章暂时不可用" />
        <PrimaryButton text="重试加载" ghost @tap="retry" />
      </view>
      <view v-else class="chapter-content">
        <PaperContainer radius="xl" class="chapter-summary">
          <view v-if="!editing" class="chapter-summary__head">
            <view>
              <text class="chapter-name">{{ chapter.name }}</text>
              <text class="chapter-status">{{ statusText }}</text>
            </view>
            <text class="chapter-edit" @tap="beginEdit">编辑</text>
          </view>
          <view v-else class="chapter-edit-form">
            <input v-model="editName" maxlength="100" class="chapter-input" placeholder="篇章名称" />
            <textarea v-model="editNote" maxlength="1000" auto-height class="chapter-note-input" placeholder="写一句自述（可选）" />
            <view class="chapter-edit-actions">
              <text @tap="cancelEdit">取消</text>
              <text class="chapter-edit-actions__save" @tap="saveEdit">{{ mutationLoading ? '保存中…' : '保存' }}</text>
            </view>
          </view>
          <text v-if="!editing && chapter.note" class="chapter-note">{{ chapter.note }}</text>
          <view class="chapter-summary__meta">
            <text>{{ chapter.memberCount }} 个片段</text>
            <text>{{ coverageText }}</text>
          </view>
          <text class="chapter-created">建立于 {{ formatDateTime(chapter.createdAt) }}</text>
          <text v-if="chapter.status === TimeChapterStatus.ENDED && chapter.endedAt" class="chapter-created">
            结束于 {{ formatDateTime(chapter.endedAt) }}
          </text>
        </PaperContainer>

        <view class="chapter-actions">
          <view class="chapter-action" @tap="toggleOrder">{{ order === 'DESC' ? '倒序' : '正序' }}浏览</view>
          <view v-if="chapter.status === 'ACTIVE'" class="chapter-action" @tap="confirmLifecycle(TimeChapterStatus.ENDED)">结束篇章</view>
          <view v-else class="chapter-action" @tap="confirmLifecycle(TimeChapterStatus.ACTIVE)">重新打开</view>
        </view>

        <view class="member-heading">
          <text>成员片段</text>
          <text>{{ members.length }} / {{ chapter.members.total }}</text>
        </view>
        <view v-if="members.length" class="member-list">
          <view v-for="member in members" :key="member.id" class="member-card" @tap="openRecord(member)">
            <view class="member-card__top">
              <text class="member-title">{{ member.title || '未命名片段' }}</text>
              <text class="member-date">{{ formatDayText(member.createdAt) }}</text>
            </view>
            <text class="member-preview">{{ member.contentPreview || '一段由图片或声音留下的记录' }}</text>
          </view>
          <view
            v-if="members.length < chapter.members.total"
            class="chapter-load-more"
            @tap="loadMoreMembers"
          >
            {{ chapterStore.loading ? '正在加载…' : '加载更多片段' }}
          </view>
        </view>
        <EmptyState v-else text="这个篇章暂时没有成员，记录仍然保留在我的记录中" />

        <view class="chapter-delete" @tap="confirmDelete">只删除篇章</view>
      </view>
    </scroll-view>
  </view>
</template>

<style scoped>
.chapter-page {
  min-height: 100vh;
  padding: 0 48rpx;
  background: linear-gradient(170deg, #faf7f2 0%, #f5f0e8 58%, #f0ebe0 100%);
  color: #302e29;
}

.chapter-top {
  display: flex;
  align-items: center;
  min-height: 104rpx;
  gap: 20rpx;
}

.chapter-back {
  width: 150rpx;
  color: #6b6560;
  font-size: 26rpx;
}

.chapter-back:first-letter {
  color: #b5352a;
  font-size: 42rpx;
}

.chapter-top__title {
  flex: 1;
  text-align: center;
  font-family: 'Noto Serif SC', 'Songti SC', Georgia, serif;
  font-size: 30rpx;
  letter-spacing: 0.12em;
}

.chapter-top__spacer {
  width: 150rpx;
}

.chapter-scroll {
  height: calc(100vh - 104rpx);
}

.chapter-content {
  padding: 20rpx 0 80rpx;
}

.chapter-summary {
  background: rgba(255, 252, 247, 0.82);
}

.chapter-summary__head,
.chapter-summary__meta,
.chapter-edit-actions,
.chapter-actions,
.member-heading,
.member-card__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
}

.chapter-name {
  display: block;
  font-family: 'Noto Serif SC', 'Songti SC', Georgia, serif;
  font-size: 38rpx;
}

.chapter-status {
  display: block;
  margin-top: 12rpx;
  color: #b5352a;
  font-size: 22rpx;
}

.chapter-edit {
  color: #9e9890;
  font-size: 23rpx;
}

.chapter-note {
  display: block;
  margin-top: 24rpx;
  color: #6b6560;
  font-size: 25rpx;
  line-height: 1.7;
}

.chapter-summary__meta {
  margin-top: 28rpx;
  color: #817a72;
  font-size: 22rpx;
}

.chapter-created {
  display: block;
  margin-top: 14rpx;
  color: #b7afa5;
  font-size: 20rpx;
}

.chapter-input,
.chapter-note-input {
  box-sizing: border-box;
  width: 100%;
  margin-bottom: 16rpx;
  padding: 16rpx 18rpx;
  border: 1rpx solid #d8d0c5;
  color: #302e29;
  font-size: 26rpx;
}

.chapter-note-input {
  min-height: 90rpx;
}

.chapter-edit-actions {
  justify-content: flex-end;
  color: #9e9890;
  font-size: 23rpx;
}

.chapter-edit-actions__save {
  color: #b5352a;
}

.chapter-actions {
  margin: 26rpx 0 38rpx;
  justify-content: flex-start;
  flex-wrap: wrap;
}

.chapter-action {
  padding: 15rpx 22rpx;
  border: 1rpx solid #c8c2b8;
  color: #6b6560;
  font-size: 22rpx;
}

.member-heading {
  margin-bottom: 18rpx;
  color: #6b6560;
  font-size: 23rpx;
  letter-spacing: 0.08em;
}

.member-heading text:last-child {
  color: #b7afa5;
  font-size: 20rpx;
}

.member-card {
  margin-bottom: 16rpx;
  padding: 24rpx;
  border-left: 3rpx solid #c8c2b8;
  background: rgba(255, 252, 247, 0.76);
}

.member-title {
  max-width: 72%;
  overflow: hidden;
  font-family: 'Noto Serif SC', 'Songti SC', Georgia, serif;
  font-size: 28rpx;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.member-date {
  color: #b7afa5;
  font-size: 20rpx;
}

.member-preview {
  display: block;
  margin-top: 14rpx;
  overflow: hidden;
  color: #817a72;
  font-size: 22rpx;
  line-height: 1.6;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chapter-delete {
  margin-top: 46rpx;
  text-align: center;
  color: #b5352a;
  font-size: 22rpx;
}

.chapter-load-more {
  padding: 28rpx 0 12rpx;
  text-align: center;
  color: #806b5c;
  font-size: 24rpx;
}

.chapter-state {
  padding-top: 120rpx;
}
</style>
