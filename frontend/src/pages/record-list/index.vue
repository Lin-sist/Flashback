<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { computed, ref } from 'vue'
import PreviewModeNotice from '../../components/common/PreviewModeNotice.vue'
import { hasPreviewSession, showPreviewReadonlyToast } from '../../features/preview/preview-session'
import { useWechatNavMetrics } from '../../composables/useWechatNavMetrics'
import { useRecordStore, useTimeChapterStore } from '../../stores'
import {
  RecordStatus,
  type DateTimeValue,
  type RecordListItemVO,
  type TimeChapterSummaryVO,
} from '../../types'
import {
  formatDayText,
  hasAuthenticatedSession,
} from '../../utils'

const { statusBarHeight } = useWechatNavMetrics()

const recordStore = useRecordStore()
const chapterStore = useTimeChapterStore()
const selectedStatus = ref<RecordStatus | 'ALL'>('ALL')
const appliedStatus = ref<RecordStatus | 'ALL'>('ALL')
const keyword = ref('')
const listLoadFailed = ref(false)
const chapterLoadFailed = ref(false)
const viewMode = ref<'records' | 'chapters'>('records')
const selectingRecords = ref(false)
const selectedRecordIds = ref<number[]>([])
const composeOpen = ref(false)
const chapterName = ref('')
const chapterNote = ref('')
const chapterSubmitting = ref(false)

const statusOptions: { label: string; value: RecordStatus | 'ALL' }[] = [
  { label: '全部', value: 'ALL' },
  { label: '已留下', value: RecordStatus.SAVED },
  { label: '已封存', value: RecordStatus.SEALED },
  { label: '已解锁', value: RecordStatus.UNLOCKED },
]

const statusLabelMap: Record<RecordStatus | 'ALL', string> = {
  ALL: '全部',
  [RecordStatus.DRAFT]: '草稿',
  [RecordStatus.SAVED]: '已留下',
  [RecordStatus.SEALED]: '已封存',
  [RecordStatus.UNLOCKED]: '已解锁',
}

const filteredList = computed(() => {
  if (!keyword.value.trim()) {
    return recordStore.list
  }
  const q = keyword.value.trim().toLowerCase()
  return recordStore.list.filter((item) => {
    const title = (item.title || '').toLowerCase()
    const preview = (item.contentPreview || '').toLowerCase()
    return title.includes(q) || preview.includes(q)
  })
})

const hasContextMismatch = computed(
  () => selectedStatus.value !== appliedStatus.value
)

const selectedStatusLabel = computed(() => statusLabelMap[selectedStatus.value])
const appliedStatusLabel = computed(() => statusLabelMap[appliedStatus.value])

const visibleCount = computed(() => filteredList.value.length)

const overviewCountText = computed(() => {
  if (recordStore.loading && visibleCount.value === 0) return '整理中'
  if (listLoadFailed.value && visibleCount.value === 0) return '暂未同步'
  if (keyword.value.trim()) return `当前页 ${visibleCount.value} 个匹配`
  return `共 ${recordStore.total} 份记录`
})

const showLoadFailureState = computed(
  () =>
    !recordStore.loading &&
    listLoadFailed.value &&
    (recordStore.list.length === 0 || hasContextMismatch.value)
)
const showEmptyState = computed(
  () =>
    !recordStore.loading &&
    !listLoadFailed.value &&
    !hasContextMismatch.value &&
    filteredList.value.length === 0
)
const showStaleNotice = computed(
  () =>
    !recordStore.loading &&
    listLoadFailed.value &&
    recordStore.list.length > 0 &&
    !hasContextMismatch.value
)

const emptyStateText = computed(() => {
  if (keyword.value.trim()) return '没有找到匹配的记录'
  if (selectedStatus.value !== 'ALL') return '当前筛选下还没有记录'
  return '档案还空着，去写下第一份记忆吧'
})

const topNavPadStyle = computed(() => ({
  paddingTop: `${statusBarHeight.value}px`,
}))

const ensureLogin = () => {
  if (!hasAuthenticatedSession() && !hasPreviewSession()) {
    uni.reLaunch({ url: '/pages/login/index' })
    return false
  }
  return true
}

const loadChapters = async () => {
  if (!ensureLogin()) return
  chapterLoadFailed.value = false
  try {
    await chapterStore.fetchList(undefined, 1, 50)
  } catch {
    chapterLoadFailed.value = true
    uni.showToast({ title: '篇章目录加载失败，请稍后重试', icon: 'none' })
  }
}

const loadMoreChapters = async () => {
  if (chapterStore.loading || chapterStore.list.length >= chapterStore.total) return
  const nextPage = chapterStore.pageNum + 1
  try {
    await chapterStore.fetchList(undefined, nextPage, 50, true)
  } catch {
    uni.showToast({ title: '更多篇章加载失败，请稍后重试', icon: 'none' })
  }
}

const setViewMode = (mode: 'records' | 'chapters') => {
  if (viewMode.value === mode) return
  viewMode.value = mode
  selectingRecords.value = false
  composeOpen.value = false
  if (mode === 'chapters') loadChapters()
  else loadList()
}

const loadList = async (
  targetStatus: RecordStatus | 'ALL' = selectedStatus.value
) => {
  if (!ensureLogin()) return
  listLoadFailed.value = false
  try {
    await recordStore.fetchList(targetStatus)
    appliedStatus.value = targetStatus
  } catch {
    listLoadFailed.value = true
    uni.showToast({ title: '网络有点慢，请稍后重试', icon: 'none' })
  }
}

const startRecordSelection = () => {
  if (hasPreviewSession()) {
    showPreviewReadonlyToast('概念预览为只读，不能组成篇章')
    return
  }
  selectingRecords.value = !selectingRecords.value
  if (!selectingRecords.value) {
    selectedRecordIds.value = []
    composeOpen.value = false
  }
}

const canSelectRecord = (item: RecordListItemVO) =>
  item.status !== RecordStatus.DRAFT && !item.chapter

const toggleRecordSelection = (item: RecordListItemVO) => {
  if (!canSelectRecord(item)) return
  const current = new Set(selectedRecordIds.value)
  if (current.has(item.id)) current.delete(item.id)
  else current.add(item.id)
  selectedRecordIds.value = [...current]
}

const openCompose = () => {
  if (selectedRecordIds.value.length === 0) {
    uni.showToast({ title: '至少选择一条记录', icon: 'none' })
    return
  }
  composeOpen.value = true
}

const cancelCompose = () => {
  composeOpen.value = false
  chapterName.value = ''
  chapterNote.value = ''
}

const submitChapter = async () => {
  if (hasPreviewSession()) {
    showPreviewReadonlyToast('概念预览为只读，不能保存篇章')
    return
  }
  const name = chapterName.value.trim()
  if (!name) {
    uni.showToast({ title: '请先写下篇章名称', icon: 'none' })
    return
  }
  chapterSubmitting.value = true
  try {
    await chapterStore.create({
      name,
      note: chapterNote.value.trim() || null,
      recordIds: [...selectedRecordIds.value],
    })
    selectingRecords.value = false
    selectedRecordIds.value = []
    cancelCompose()
    viewMode.value = 'chapters'
    await loadChapters()
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '篇章保存失败', icon: 'none' })
  } finally {
    chapterSubmitting.value = false
  }
}

const chapterGroups = computed(() => ({
  active: chapterStore.list.filter((chapter) => chapter.status === 'ACTIVE'),
  ended: chapterStore.list.filter((chapter) => chapter.status === 'ENDED'),
}))

const chapterStatusLabel = (chapter: TimeChapterSummaryVO) =>
  chapter.status === 'ACTIVE' ? '进行中' : '已结束'

const chapterCoverageText = (chapter: TimeChapterSummaryVO) => {
  if (!chapter.coverageStartAt || !chapter.coverageEndAt) return '片段覆盖时间待形成'
  const start = formatDayText(chapter.coverageStartAt)
  const end = formatDayText(chapter.coverageEndAt)
  return start === end ? `片段覆盖 ${start}` : `片段覆盖 ${start} — ${end}`
}

const openChapter = (chapter: TimeChapterSummaryVO) => {
  uni.navigateTo({ url: `/pages/time-chapter-detail/index?id=${chapter.id}&source=archive` })
}

const onStatusChange = (value: RecordStatus | 'ALL') => {
  if (selectedStatus.value === value) return
  selectedStatus.value = value
  loadList(value)
}

const onSearchInput = (event: Event) => {
  const inputEvent = event as unknown as { detail?: { value?: string } }
  keyword.value = inputEvent.detail?.value || ''
}

const clearKeyword = () => {
  keyword.value = ''
}

const openRecord = (item: RecordListItemVO) => {
  if (item.status === RecordStatus.DRAFT) {
    uni.navigateTo({
      url: `/pages/record-editor/index?id=${item.id}&source=archive`,
    })
    return
  }
  uni.navigateTo({
    url: `/pages/record-detail/index?id=${item.id}&source=archive`,
  })
}

const goBack = () => uni.navigateBack({ delta: 1 })

const goWrite = () => uni.navigateTo({ url: '/pages/record-editor/index' })

const statusBadgeText = (status: RecordStatus) => {
  if (status === RecordStatus.DRAFT) return '草稿'
  if (status === RecordStatus.SAVED) return '已留下'
  if (status === RecordStatus.SEALED) return '封存中'
  return '已解锁'
}

const statusBadgeClass = (status: RecordStatus) => {
  if (status === RecordStatus.DRAFT) return 'badge-draft'
  if (status === RecordStatus.SAVED) return 'badge-draft'
  if (status === RecordStatus.SEALED) return 'badge-sealed'
  return 'badge-unlocked'
}

const cardStateClass = (status: RecordStatus) => {
  if (status === RecordStatus.DRAFT) return 'is-draft'
  if (status === RecordStatus.SAVED) return ''
  if (status === RecordStatus.SEALED) return 'is-sealed'
  return ''
}

const iconWrapClass = (status: RecordStatus) => {
  if (status === RecordStatus.DRAFT) return 'card-icon-wrap--draft'
  if (status === RecordStatus.SAVED) return 'card-icon-wrap--unlocked'
  if (status === RecordStatus.SEALED) return 'card-icon-wrap--sealed'
  return 'card-icon-wrap--unlocked'
}

const getYear = (value?: DateTimeValue) => {
  if (value === undefined || value === null) return ''
  const normalized =
    typeof value === 'string' && !value.includes('T')
      ? value.replace(' ', 'T')
      : value
  const date = new Date(normalized as string | number)
  if (Number.isNaN(date.getTime())) return ''
  return String(date.getFullYear())
}

const approximateWordCount = (preview: string) =>
  preview ? preview.replace(/\s/g, '').length : 0

const metaLine = (item: RecordListItemVO) => {
  const dateText = formatDayText(item.createdAt)
  if (item.status === RecordStatus.SEALED && item.unlockAt) {
    const year = getYear(item.unlockAt)
    if (year) {
      return { left: dateText, right: `预计 ${year} 年解锁`, isUnlock: true }
    }
  }
  if (item.status === RecordStatus.DRAFT) {
    const count = approximateWordCount(item.contentPreview)
    return { left: dateText, right: count > 0 ? `${count} 字` : '待续写', isUnlock: false }
  }
  const count = approximateWordCount(item.contentPreview)
  return {
    left: dateText,
    right: count > 0 ? `${count.toLocaleString()} 字` : '',
    isUnlock: false,
  }
}

onShow(() => (viewMode.value === 'chapters' ? loadChapters() : loadList()))
</script>

<template>
  <view class="page">
    <PreviewModeNotice />
    <!-- 宣纸纹理层 -->
    <view class="page-noise" />
    <!-- 光晕层 -->
    <view class="page-glow" />

    <!-- 顶部导航（含状态栏补偿） -->
    <view class="top-nav" :style="topNavPadStyle">
      <view class="back-btn" @tap="goBack">
        <view class="back-arrow" />
        <text class="back-btn-label">返回</text>
      </view>
      <text class="page-title">我的记录</text>
    </view>

    <!-- 滚动内容区 -->
    <scroll-view scroll-y class="scroll-area" enhanced show-scrollbar="false">
      <view class="scroll-inner">

        <!-- 二级切换：仍属于“我的记录”一级入口，不新增 Tab -->
        <view class="secondary-tabs">
          <view
            class="secondary-tab"
            :class="{ 'secondary-tab--active': viewMode === 'records' }"
            @tap="setViewMode('records')"
          >记录</view>
          <view
            class="secondary-tab"
            :class="{ 'secondary-tab--active': viewMode === 'chapters' }"
            @tap="setViewMode('chapters')"
          >篇章</view>
        </view>

        <template v-if="viewMode === 'records'">

        <!-- 搜索框 -->
        <view class="search-wrap">
          <view class="search-icon-wrap">
            <view class="search-circle" />
            <view class="search-handle" />
          </view>
          <input
            class="search-input"
            :value="keyword"
            placeholder="搜索当前已载入的标题或内容"
            placeholder-class="search-placeholder"
            @input="onSearchInput"
          />
          <text v-if="keyword" class="search-clear" @tap="clearKeyword">✕</text>
        </view>

        <!-- 筛选 Tabs -->
        <view class="filter-tabs">
          <view
            v-for="option in statusOptions"
            :key="option.value"
            class="tab"
            :class="{ 'tab-active': option.value === selectedStatus }"
            @tap="onStatusChange(option.value)"
          >{{ option.label }}</view>
        </view>

        <!-- 档案概览标题 -->
        <view class="section-header">
          <text class="section-title">档案概览</text>
          <view class="section-actions">
            <text class="section-count">{{ selectingRecords ? `已选 ${selectedRecordIds.length} 条` : overviewCountText }}</text>
            <text class="chapter-select-action" @tap="startRecordSelection">
              {{ selectingRecords ? '取消选择' : '组成篇章' }}
            </text>
          </view>
        </view>
        <view class="deco-line-left" />

        <!-- 缓存陈旧提示 -->
        <view v-if="showStaleNotice" class="stale-notice">
          <text class="stale-text">网络稍慢，先摊开上次整理好的目录</text>
          <text class="stale-retry" @tap="loadList()">重新整理</text>
        </view>

        <!-- Loading -->
        <view v-if="recordStore.loading && filteredList.length === 0" class="state-row">
          <text class="state-text">整理中…</text>
        </view>

        <!-- 加载失败 -->
        <view v-else-if="showLoadFailureState" class="state-row" @tap="loadList()">
          <text class="state-text">网络有点慢，轻触重试</text>
        </view>

        <!-- 空列表 -->
        <view v-else-if="showEmptyState" class="state-row">
          <text class="state-text">{{ emptyStateText }}</text>
        </view>

        <!-- 卡片列表 -->
        <view v-else class="cards-list">
          <view
            v-for="item in filteredList"
            :key="item.id"
            class="card"
            :class="[cardStateClass(item.status), { 'card--selected': selectedRecordIds.includes(item.id) }]"
            @tap="selectingRecords ? toggleRecordSelection(item) : openRecord(item)"
          >
            <!-- 左侧朱砂竖线（替代 ::before） -->
            <view class="card-vline" />
            <!-- 右上折角（替代 ::after） -->
            <view class="card-corner" />

            <view class="card-top">
              <view v-if="selectingRecords" class="record-select-mark" :class="{ 'record-select-mark--on': selectedRecordIds.includes(item.id), 'record-select-mark--disabled': !canSelectRecord(item) }">
                <text>{{ selectedRecordIds.includes(item.id) ? '✓' : (item.chapter ? '·' : '') }}</text>
              </view>
              <!-- 图标圆圈 -->
              <view class="card-icon-wrap" :class="iconWrapClass(item.status)">
                <view class="card-icon" :class="iconWrapClass(item.status) + '__icon'" />
              </view>

              <!-- 标题 + 摘要 -->
              <view class="card-title-area">
                <text class="card-title">{{ item.title || '未命名片段' }}</text>
                <text class="card-excerpt">{{ item.contentPreview || '一段由图片或声音留下的记录' }}</text>
              </view>

              <!-- 状态徽章 -->
              <view class="card-badge" :class="statusBadgeClass(item.status)">
                <text class="card-badge-text">{{ statusBadgeText(item.status) }}</text>
                <view v-if="item.status === 'SEALED'" class="pulse-dot" />
              </view>
            </view>

            <!-- 元信息行 -->
            <view class="card-footer">
              <text class="card-date">{{ metaLine(item).left }}</text>
              <view v-if="metaLine(item).right" class="card-dot" />
              <text
                v-if="metaLine(item).right"
                :class="metaLine(item).isUnlock ? 'card-unlock-date' : 'card-word-count'"
              >{{ metaLine(item).right }}</text>
              <text v-if="item.chapter" class="card-chapter-name">{{ item.chapter.name }}</text>
            </view>
          </view>
        </view>

        <view v-if="selectingRecords && selectedRecordIds.length > 0" class="chapter-compose">
          <view class="chapter-compose__heading">把这些片段放在一起</view>
          <input
            v-model="chapterName"
            class="chapter-compose__input"
            maxlength="100"
            placeholder="篇章名称（必填）"
            placeholder-class="chapter-compose__placeholder"
          />
          <textarea
            v-model="chapterNote"
            class="chapter-compose__note"
            maxlength="1000"
            auto-height
            placeholder="写一句自述（可选）"
            placeholder-class="chapter-compose__placeholder"
          />
          <view class="chapter-compose__actions">
            <text class="chapter-compose__cancel" @tap="cancelCompose">稍后再写</text>
            <view class="chapter-compose__submit" :class="{ 'chapter-compose__submit--loading': chapterSubmitting }" @tap="submitChapter">
              {{ chapterSubmitting ? '保存中…' : '保存篇章' }}
            </view>
          </view>
        </view>

        </template>

        <template v-else>
          <view class="section-header chapter-section-header">
            <text class="section-title">时间篇章</text>
            <text class="section-count">{{ chapterStore.loading ? '整理中' : `共 ${chapterStore.total} 个篇章` }}</text>
          </view>
          <view class="deco-line-left" />

          <view v-if="chapterStore.loading && chapterStore.list.length === 0" class="state-row">
            <text class="state-text">正在摊开篇章目录…</text>
          </view>
          <view v-else-if="chapterLoadFailed" class="state-row" @tap="loadChapters">
            <text class="state-text">篇章目录加载失败，轻触重试</text>
          </view>
          <view v-else-if="chapterStore.list.length === 0" class="state-row">
            <text class="state-text">还没有篇章，可以从记录中组成一段时间</text>
          </view>
          <view v-else class="chapters-list">
            <view v-if="chapterGroups.active.length" class="chapter-group">
              <text class="chapter-group__label">进行中</text>
              <view v-for="chapter in chapterGroups.active" :key="chapter.id" class="chapter-card" @tap="openChapter(chapter)">
                <view class="chapter-card__top">
                  <text class="chapter-card__name">{{ chapter.name }}</text>
                  <text class="chapter-card__status">{{ chapterStatusLabel(chapter) }}</text>
                </view>
                <text v-if="chapter.note" class="chapter-card__note">{{ chapter.note }}</text>
                <view class="chapter-card__bottom">
                  <text>{{ chapter.memberCount }} 个片段</text>
                  <text>{{ chapterCoverageText(chapter) }}</text>
                </view>
              </view>
            </view>
            <view v-if="chapterGroups.ended.length" class="chapter-group">
              <text class="chapter-group__label">已结束</text>
              <view v-for="chapter in chapterGroups.ended" :key="chapter.id" class="chapter-card chapter-card--ended" @tap="openChapter(chapter)">
                <view class="chapter-card__top">
                  <text class="chapter-card__name">{{ chapter.name }}</text>
                  <text class="chapter-card__status">{{ chapterStatusLabel(chapter) }}</text>
                </view>
                <text v-if="chapter.note" class="chapter-card__note">{{ chapter.note }}</text>
                <view class="chapter-card__bottom">
                  <text>{{ chapter.memberCount }} 个片段</text>
                  <text>{{ chapterCoverageText(chapter) }}</text>
                </view>
              </view>
            </view>
            <view
              v-if="chapterStore.list.length < chapterStore.total"
              class="state-row"
              @tap="loadMoreChapters"
            >
              <text class="state-text">{{ chapterStore.loading ? '正在加载…' : '加载更多篇章' }}</text>
            </view>
          </view>
        </template>

        <!-- 封存新的记忆 CTA -->
        <view
          v-if="viewMode === 'records' && !showLoadFailureState && !showEmptyState"
          class="write-btn-wrap"
        >
          <view class="write-btn" @tap="goWrite">
            <view class="write-btn-corner-tl" />
            <view class="write-btn-corner-br" />
            <view class="btn-dot" />
            <text class="write-btn-text">封存新的记忆</text>
          </view>
        </view>

        <!-- 页脚 -->
        <text class="footnote">时间在此处静静回溯</text>
        <view class="footnote-dots">
          <view class="footnote-dot footnote-dot--active" />
          <view class="footnote-dot" />
          <view class="footnote-dot" />
        </view>

      </view>
    </scroll-view>
  </view>
</template>

<style scoped>
/* ═══════════════════════════════════════
   页面底层
═══════════════════════════════════════ */
.page {
  width: 100%;
  min-height: 100vh;
  background: linear-gradient(170deg, #faf7f2 0%, #f5f0e8 55%, #f0ebe0 100%);
  position: relative;
  overflow-x: hidden;
  box-sizing: border-box;
}

/* 宣纸纸感噪声纹理 */
.page-noise {
  position: absolute;
  inset: 0;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='500' height='500'%3E%3Cfilter id='f'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.55' numOctaves='6' stitchTiles='stitch'/%3E%3CfeColorMatrix type='saturate' values='0.15'/%3E%3C/filter%3E%3Crect width='500' height='500' filter='url(%23f)' opacity='0.055'/%3E%3C/svg%3E");
  pointer-events: none;
  z-index: 1;
}

/* 光晕层 */
.page-glow {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse 80% 50% at 18% 10%, rgba(200,185,158,0.09) 0%, transparent 70%),
    radial-gradient(ellipse 60% 40% at 82% 25%, rgba(185,168,140,0.06) 0%, transparent 65%),
    radial-gradient(ellipse 45% 55% at 70% 78%, rgba(178,162,135,0.07) 0%, transparent 65%),
    radial-gradient(ellipse 65% 40% at 30% 85%, rgba(170,155,128,0.05) 0%, transparent 65%),
    radial-gradient(ellipse 50% 35% at 50% 45%, rgba(250,245,238,0.18) 0%, transparent 75%);
  pointer-events: none;
  z-index: 1;
}

/* ═══════════════════════════════════════
   顶部导航
═══════════════════════════════════════ */
.top-nav {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  padding-left: 56rpx;
  padding-right: 56rpx;
  padding-bottom: 56rpx;
  width: 100%;
  box-sizing: border-box;
  /* padding-top overridden by inline style for status bar */
}

.back-btn {
  position: absolute;
  left: 56rpx;
  display: flex;
  align-items: center;
  gap: 8rpx;
}

/* CSS 构成的箭头 —— view 元素，无伪元素 */
.back-arrow {
  width: 28rpx;
  height: 28rpx;
  border-left: 1rpx solid #c8c2b8;
  border-bottom: 1rpx solid #c8c2b8;
  transform: rotate(45deg);
  flex-shrink: 0;
}

.back-btn-label {
  font-family: 'Noto Serif SC', 'Songti SC', Georgia, serif;
  font-size: 24rpx;
  font-weight: 300;
  color: #9e9890;
  letter-spacing: 0.06em;
}

.page-title {
  font-family: 'Noto Serif SC', 'Songti SC', Georgia, serif;
  font-size: 30rpx;
  font-weight: 300;
  color: #302e29;
  letter-spacing: 0.35em;
}

/* ═══════════════════════════════════════
   滚动区
═══════════════════════════════════════ */
.scroll-area {
  position: relative;
  z-index: 2;
  width: 100%;
  box-sizing: border-box;
  height: calc(100vh - 200rpx); /* 动态补偿，保证 scroll-view 可工作 */
}

.scroll-inner {
  width: 100%;
  padding-left: 56rpx;
  padding-right: 56rpx;
  padding-bottom: 80rpx;
  box-sizing: border-box;
}

/* ═══════════════════════════════════════
   搜索框
═══════════════════════════════════════ */
.search-wrap {
  position: relative;
  margin-bottom: 40rpx;
  width: 100%;
  box-sizing: border-box;
}

/* 搜索图标：圆圈 + 手柄（替代 ::after） */
.search-icon-wrap {
  position: absolute;
  left: 28rpx;
  top: 50%;
  transform: translateY(-50%);
  width: 26rpx;
  height: 26rpx;
  pointer-events: none;
}

.search-circle {
  position: absolute;
  top: 0;
  left: 0;
  width: 26rpx;
  height: 26rpx;
  border: 1rpx solid #9e9890;
  border-radius: 50%;
}

.search-handle {
  position: absolute;
  bottom: -8rpx;
  right: -6rpx;
  width: 8rpx;
  height: 1rpx;
  background: #9e9890;
  transform: rotate(45deg);
  transform-origin: left center;
}

.search-input {
  width: 100%;
  height: 80rpx;
  line-height: 80rpx;
  background: rgba(252, 249, 244, 0.6);
  border: 1rpx solid #c8c2b8;
  border-radius: 2rpx;
  padding: 0 32rpx 0 76rpx;
  font-family: 'Noto Serif SC', 'Songti SC', Georgia, serif;
  font-size: 26rpx;
  font-weight: 300;
  color: #6b6560;
  letter-spacing: 0.06em;
  box-sizing: border-box;
}

.search-placeholder {
  color: #9e9890;
}

.search-clear {
  position: absolute;
  right: 20rpx;
  top: 50%;
  transform: translateY(-50%);
  color: #c8c2b8;
  font-size: 24rpx;
  padding: 8rpx;
}

/* ═══════════════════════════════════════
   筛选 Tabs
═══════════════════════════════════════ */
.filter-tabs {
  display: flex;
  gap: 16rpx;
  margin-bottom: 56rpx;
  flex-wrap: wrap;
  width: 100%;
  box-sizing: border-box;
}

.tab {
  font-family: 'Noto Serif SC', 'Songti SC', Georgia, serif;
  font-size: 24rpx;
  font-weight: 300;
  letter-spacing: 0.1em;
  padding: 12rpx 32rpx;
  border: 1rpx solid #c8c2b8;
  border-radius: 2rpx;
  color: #9e9890;
  background: transparent;
}

.tab-active {
  color: #302e29;
  border-color: #6b6560;
  background: rgba(48, 46, 41, 0.04);
}

/* ═══════════════════════════════════════
   档案概览
═══════════════════════════════════════ */
.section-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 8rpx;
}

.section-title {
  font-family: 'Noto Serif SC', 'Songti SC', Georgia, serif;
  font-size: 40rpx;
  font-weight: 300;
  color: #302e29;
  letter-spacing: 0.04em;
}

.section-count {
  font-family: 'Noto Sans SC', 'PingFang SC', sans-serif;
  font-size: 22rpx;
  font-weight: 300;
  color: #9e9890;
  letter-spacing: 0.08em;
}

.deco-line-left {
  width: 64rpx;
  height: 1rpx;
  background: #c8c2b8;
  margin-bottom: 40rpx;
}

/* ═══════════════════════════════════════
   状态占位
═══════════════════════════════════════ */
.stale-notice {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16rpx 0;
  margin-bottom: 20rpx;
}

.stale-text {
  font-family: 'Noto Serif SC', 'Songti SC', Georgia, serif;
  font-size: 24rpx;
  color: #9e9890;
}

.stale-retry {
  font-family: 'Noto Serif SC', 'Songti SC', Georgia, serif;
  font-size: 24rpx;
  color: rgba(181, 53, 42, 0.8);
}

.state-row {
  padding: 60rpx 0;
  display: flex;
  justify-content: center;
}

.state-text {
  font-family: 'Noto Serif SC', 'Songti SC', Georgia, serif;
  font-size: 26rpx;
  font-weight: 300;
  color: #c8c2b8;
  letter-spacing: 0.08em;
}

/* ═══════════════════════════════════════
   卡片列表
═══════════════════════════════════════ */
.cards-list {
  display: flex;
  flex-direction: column;
  gap: 32rpx;
}

/* ─── 单张卡片 ─── */
.card {
  position: relative;
  width: 100%;
  box-sizing: border-box;
  background: rgba(252, 249, 244, 0.72);
  border: 1rpx solid rgba(188, 174, 152, 0.28);
  border-radius: 2rpx;
  padding: 40rpx 40rpx 36rpx 52rpx;
  box-shadow:
    0 2rpx 0 rgba(255, 255, 255, 0.6) inset,
    0 4rpx 24rpx rgba(140, 120, 90, 0.06),
    0 2rpx 6rpx rgba(140, 120, 90, 0.04);
}

/* 左侧朱砂竖线（::before 替代） */
.card-vline {
  position: absolute;
  left: 0;
  top: 36rpx;
  bottom: 36rpx;
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

.is-sealed .card-vline {
  background: linear-gradient(
    to bottom,
    transparent,
    rgba(107, 101, 96, 0.2) 25%,
    rgba(107, 101, 96, 0.2) 75%,
    transparent
  );
}

.is-draft .card-vline {
  background: linear-gradient(
    to bottom,
    transparent,
    rgba(181, 162, 80, 0.3) 25%,
    rgba(181, 162, 80, 0.3) 75%,
    transparent
  );
}

/* 右上折角（::after 替代） */
.card-corner {
  position: absolute;
  top: 0;
  right: 0;
  width: 24rpx;
  height: 24rpx;
  background: linear-gradient(
    225deg,
    rgba(230, 218, 200, 0.9) 0%,
    rgba(230, 218, 200, 0.9) 48%,
    rgba(252, 249, 244, 0) 50%
  );
  border-left: 1rpx solid rgba(188, 174, 152, 0.22);
  border-bottom: 1rpx solid rgba(188, 174, 152, 0.22);
}

/* 封存态标题变浅 */
.is-sealed .card-title { color: #6b6560; }

/* 卡片顶部行 */
.card-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20rpx;
  margin-bottom: 14rpx;
}

/* ─── 图标圆圈 ─── */
.card-icon-wrap {
  width: 64rpx;
  height: 64rpx;
  flex-shrink: 0;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 8rpx;
}

.card-icon-wrap--unlocked { background: rgba(181, 53, 42, 0.08); }
.card-icon-wrap--sealed   { background: rgba(107, 101, 96, 0.08); }
.card-icon-wrap--draft    { background: rgba(181, 162, 120, 0.12); }

.card-icon {
  width: 32rpx;
  height: 32rpx;
  background-repeat: no-repeat;
  background-size: contain;
  background-position: center;
}

/* SVG 图标 via data URI */
.card-icon-wrap--unlocked__icon {
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 24 24' fill='none' stroke='rgba(181%2C53%2C42%2C0.55)' stroke-width='1.2' stroke-linecap='round' stroke-linejoin='round' xmlns='http://www.w3.org/2000/svg'%3E%3Crect x='3' y='5' width='18' height='14' rx='1'/%3E%3Cpolyline points='3%2C5 12%2C13 21%2C5'/%3E%3C/svg%3E");
}

.card-icon-wrap--sealed__icon {
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 24 24' fill='none' stroke='rgba(107%2C101%2C96%2C0.45)' stroke-width='1.2' stroke-linecap='round' stroke-linejoin='round' xmlns='http://www.w3.org/2000/svg'%3E%3Crect x='5' y='11' width='14' height='10' rx='1'/%3E%3Cpath d='M8 11V7a4 4 0 0 1 8 0v4'/%3E%3C/svg%3E");
}

.card-icon-wrap--draft__icon {
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 24 24' fill='none' stroke='rgba(160%2C130%2C60%2C0.55)' stroke-width='1.2' stroke-linecap='round' stroke-linejoin='round' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M12 20h9'/%3E%3Cpath d='M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4 10.5-10.5z'/%3E%3C/svg%3E");
}

/* ─── 标题区 ─── */
.card-title-area { flex: 1; min-width: 0; }

.card-title {
  display: block;
  font-family: 'Noto Serif SC', 'Songti SC', Georgia, serif;
  font-size: 30rpx;
  font-weight: 300;
  color: #302e29;
  letter-spacing: 0.03em;
  line-height: 1.5;
  margin-bottom: 10rpx;
}

.card-excerpt {
  display: block;
  font-family: 'Noto Serif SC', 'Songti SC', Georgia, serif;
  font-size: 24rpx;
  font-weight: 300;
  color: #9e9890;
  letter-spacing: 0.03em;
  line-height: 1.7;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ─── 状态徽章 ─── */
.card-badge {
  display: flex;
  align-items: center;
  gap: 6rpx;
  font-family: 'Noto Sans SC', 'PingFang SC', sans-serif;
  font-size: 20rpx;
  font-weight: 300;
  letter-spacing: 0.06em;
  padding: 6rpx 14rpx;
  border-radius: 2rpx;
  margin-top: 4rpx;
  flex-shrink: 0;
}

.badge-unlocked {
  color: #b5352a;
  border: 1rpx solid rgba(181, 53, 42, 0.3);
  background: rgba(181, 53, 42, 0.04);
}

.badge-sealed {
  color: #6b6560;
  border: 1rpx solid #c8c2b8;
  background: rgba(107, 101, 96, 0.04);
}

.badge-draft {
  color: rgba(160, 130, 60, 0.9);
  border: 1rpx solid rgba(181, 162, 80, 0.35);
  background: rgba(181, 162, 80, 0.06);
}

.card-badge-text { flex-shrink: 0; }

/* 朱砂脉冲点 */
.pulse-dot {
  width: 8rpx;
  height: 8rpx;
  border-radius: 50%;
  background: #b5352a;
  animation: pulse 2s ease-in-out infinite;
  flex-shrink: 0;
}

@keyframes pulse {
  0%, 100% { opacity: 0.3; transform: scale(0.8); }
  50%       { opacity: 1;   transform: scale(1.2); }
}

/* ─── 卡片底部元信息 ─── */
.card-footer {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-top: 20rpx;
}

.card-date {
  font-family: 'Noto Sans SC', 'PingFang SC', sans-serif;
  font-size: 20rpx;
  font-weight: 300;
  color: #9e9890;
  letter-spacing: 0.06em;
}

.card-dot {
  width: 4rpx;
  height: 4rpx;
  border-radius: 50%;
  background: #c8c2b8;
  flex-shrink: 0;
}

.card-word-count {
  font-family: 'Noto Sans SC', 'PingFang SC', sans-serif;
  font-size: 20rpx;
  font-weight: 300;
  color: #9e9890;
  letter-spacing: 0.06em;
}

.card-unlock-date {
  font-family: 'Noto Sans SC', 'PingFang SC', sans-serif;
  font-size: 20rpx;
  font-weight: 300;
  color: rgba(181, 53, 42, 0.55);
  letter-spacing: 0.06em;
}

/* ═══════════════════════════════════════
   封存新记忆 CTA
═══════════════════════════════════════ */
.write-btn-wrap {
  padding: 16rpx 0 24rpx;
  display: flex;
  justify-content: center;
  margin-top: 20rpx;
}

.write-btn {
  position: relative;
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 24rpx 64rpx;
  background: transparent;
  border: 1rpx solid #c8c2b8;
  border-radius: 4rpx;
}

/* 角标（替代 ::before / ::after） */
.write-btn-corner-tl {
  position: absolute;
  top: -2rpx;
  left: -2rpx;
  width: 12rpx;
  height: 12rpx;
  border-top: 2rpx solid #9e9890;
  border-left: 2rpx solid #9e9890;
}

.write-btn-corner-br {
  position: absolute;
  bottom: -2rpx;
  right: -2rpx;
  width: 12rpx;
  height: 12rpx;
  border-bottom: 2rpx solid #9e9890;
  border-right: 2rpx solid #9e9890;
}

.btn-dot {
  width: 10rpx;
  height: 10rpx;
  border-radius: 50%;
  background: #b5352a;
  opacity: 0.7;
  flex-shrink: 0;
}

.write-btn-text {
  font-family: 'Noto Serif SC', 'Songti SC', Georgia, serif;
  font-size: 28rpx;
  font-weight: 400;
  letter-spacing: 0.18em;
  color: #302e29;
}

/* ═══════════════════════════════════════
   页脚
═══════════════════════════════════════ */
.footnote {
  display: block;
  text-align: center;
  padding-top: 40rpx;
  padding-bottom: 24rpx;
  font-family: 'Noto Serif SC', 'Songti SC', Georgia, serif;
  font-size: 22rpx;
  font-weight: 300;
  color: #c8c2b8;
  letter-spacing: 0.12em;
}

.footnote-dots {
  display: flex;
  justify-content: center;
  gap: 10rpx;
  padding-bottom: 80rpx;
}

.footnote-dot {
  width: 6rpx;
  height: 6rpx;
  border-radius: 50%;
  background: #c8c2b8;
}

.footnote-dot--active {
  background: #b5352a;
  opacity: 0.6;
}

.secondary-tabs {
  display: flex;
  gap: 34rpx;
  margin: 18rpx 0 34rpx;
  border-bottom: 1rpx solid rgba(157, 145, 131, 0.25);
}

.secondary-tab {
  padding: 0 4rpx 14rpx;
  color: #9e9890;
  font-size: 27rpx;
  letter-spacing: 0.12em;
}

.secondary-tab--active {
  color: #302e29;
  border-bottom: 3rpx solid #b5352a;
}

.section-actions {
  display: flex;
  align-items: center;
  gap: 18rpx;
}

.chapter-select-action {
  color: #b5352a;
  font-size: 22rpx;
  letter-spacing: 0.08em;
}

.record-select-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 38rpx;
  height: 38rpx;
  margin-right: 14rpx;
  border: 1rpx solid #c8c2b8;
  border-radius: 50%;
  color: #b5352a;
  font-size: 24rpx;
  flex-shrink: 0;
}

.record-select-mark--on {
  border-color: #b5352a;
  background: rgba(181, 53, 42, 0.08);
}

.record-select-mark--disabled {
  color: #c8c2b8;
  border-color: #ded8ce;
}

.card--selected {
  box-shadow: 0 0 0 2rpx rgba(181, 53, 42, 0.22);
}

.card-chapter-name {
  max-width: 220rpx;
  margin-left: auto;
  overflow: hidden;
  color: #9e9890;
  font-size: 20rpx;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chapter-compose {
  margin: 28rpx 0 10rpx;
  padding: 28rpx;
  border: 1rpx solid rgba(157, 145, 131, 0.35);
  background: rgba(255, 252, 247, 0.56);
}

.chapter-compose__heading {
  margin-bottom: 20rpx;
  color: #302e29;
  font-family: 'Noto Serif SC', 'Songti SC', Georgia, serif;
  font-size: 27rpx;
}

.chapter-compose__input,
.chapter-compose__note {
  box-sizing: border-box;
  width: 100%;
  margin-bottom: 16rpx;
  padding: 18rpx 20rpx;
  border: 1rpx solid #d8d0c5;
  background: rgba(255, 255, 255, 0.42);
  color: #302e29;
  font-size: 25rpx;
}

.chapter-compose__note {
  min-height: 96rpx;
}

.chapter-compose__placeholder {
  color: #b7afa5;
}

.chapter-compose__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 24rpx;
}

.chapter-compose__cancel {
  color: #9e9890;
  font-size: 23rpx;
}

.chapter-compose__submit {
  padding: 16rpx 28rpx;
  background: #b5352a;
  color: #fffaf5;
  font-size: 23rpx;
}

.chapter-compose__submit--loading {
  opacity: 0.55;
}

.chapter-section-header {
  margin-top: 16rpx;
}

.chapter-group {
  margin-top: 28rpx;
}

.chapter-group__label {
  display: block;
  margin-bottom: 14rpx;
  color: #9e9890;
  font-size: 21rpx;
  letter-spacing: 0.12em;
}

.chapter-card {
  margin-bottom: 18rpx;
  padding: 28rpx;
  border-left: 4rpx solid #b5352a;
  background: rgba(255, 252, 247, 0.74);
  box-shadow: 0 8rpx 24rpx rgba(83, 67, 52, 0.06);
}

.chapter-card--ended {
  border-left-color: #aaa197;
  opacity: 0.88;
}

.chapter-card__top,
.chapter-card__bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
}

.chapter-card__name {
  color: #302e29;
  font-family: 'Noto Serif SC', 'Songti SC', Georgia, serif;
  font-size: 30rpx;
}

.chapter-card__status {
  color: #b5352a;
  font-size: 20rpx;
}

.chapter-card--ended .chapter-card__status {
  color: #8f887f;
}

.chapter-card__note {
  display: block;
  margin-top: 16rpx;
  color: #6b6560;
  font-size: 23rpx;
  line-height: 1.6;
}

.chapter-card__bottom {
  margin-top: 22rpx;
  color: #9e9890;
  font-size: 20rpx;
}
</style>
