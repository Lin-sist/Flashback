<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import AppTopSafeBar from '../../components/common/AppTopSafeBar.vue'
import PreviewModeNotice from '../../components/common/PreviewModeNotice.vue'
import { recordService, tagService } from '../../services'
import { useRecordCoverUrls } from '../../composables/useRecordCoverUrls'
import {
  RecordStatus,
  type TagVO,
  type TimelineGroupVO,
  type TimelineItemVO,
  type TimelineQuery,
} from '../../types'
import { formatDateTime, hasAuthenticatedSession } from '../../utils'

type NodeKind = 'saved' | 'sealed' | 'unlocked' | 'draft' | 'locked'
type DateGranularity = 'ALL' | 'YEAR' | 'MONTH' | 'DAY'

interface DecoratedItem {
  id: number
  raw: TimelineItemVO
  title: string
  kind: NodeKind
  dateText: string
  dateLabel: string
  contextText: string
  hasImage: boolean
}

interface TimelineFilterState {
  tagId?: number
  tagName?: string
  year?: number
  month?: number
  day?: number
}

const PAGE_SIZE = 20
const SHANGHAI_OFFSET_MILLIS = 8 * 60 * 60 * 1000
const dateGranularityOptions: Array<{ value: DateGranularity; label: string }> = [
  { value: 'ALL', label: '全部' },
  { value: 'YEAR', label: '按年' },
  { value: 'MONTH', label: '按月' },
  { value: 'DAY', label: '按日' },
]

const loading = ref(false)
const loadingMore = ref(false)
const timelineGroups = ref<TimelineGroupVO[]>([])
const timelineLoadFailed = ref(false)
const loadMoreFailed = ref(false)
const timelineTotal = ref(0)
const currentPage = ref(0)
const hasMore = ref(false)
const filterPanelVisible = ref(false)
const filterApplying = ref(false)
const filterApplyFailed = ref(false)
const availableTags = ref<TagVO[]>([])
const tagsLoading = ref(false)
const tagsLoadFailed = ref(false)
const appliedFilters = ref<TimelineFilterState>({})
const draftTagId = ref<number | undefined>()
const draftGranularity = ref<DateGranularity>('ALL')
const draftDate = ref('')
let latestRequestId = 0

const { coverUrls, coverErrors, loadCovers, markCoverFailed } = useRecordCoverUrls()

const flatCount = computed(() => timelineGroups.value.reduce((sum, group) => sum + group.items.length, 0))
const hasAppliedFilter = computed(() => Boolean(
  appliedFilters.value.tagId
    || appliedFilters.value.year
    || appliedFilters.value.month
    || appliedFilters.value.day
))
const showLoadFailureState = computed(() => !loading.value && timelineLoadFailed.value && timelineGroups.value.length === 0)
const showEmptyState = computed(() => !loading.value && !timelineLoadFailed.value && timelineGroups.value.length === 0)
const showStaleNotice = computed(() => !loading.value && timelineLoadFailed.value && timelineGroups.value.length > 0)
const pickerFields = computed(() => {
  if (draftGranularity.value === 'YEAR') return 'year'
  if (draftGranularity.value === 'MONTH') return 'month'
  return 'day'
})
const appliedFilterText = computed(() => {
  const parts: string[] = []
  if (appliedFilters.value.tagName) parts.push(appliedFilters.value.tagName)
  if (appliedFilters.value.year) {
    let dateText = `${appliedFilters.value.year} 年`
    if (appliedFilters.value.month) dateText += ` ${appliedFilters.value.month} 月`
    if (appliedFilters.value.day) dateText += ` ${appliedFilters.value.day} 日`
    parts.push(dateText)
  }
  return parts.length ? parts.join(' · ') : '全部'
})
const emptyStateText = computed(() => hasAppliedFilter.value
  ? '没有找到符合这些条件的片段'
  : '时间长廊还没有展开第一段记忆')
const draftDateText = computed(() => {
  if (draftGranularity.value === 'ALL') return '全部日期'
  const [year, month, day] = ensureDraftDate().split('-').map(Number)
  if (draftGranularity.value === 'YEAR') return `${year} 年`
  if (draftGranularity.value === 'MONTH') return `${year} 年 ${month} 月`
  return `${year} 年 ${month} 月 ${day} 日`
})

const shanghaiToday = () => new Date(Date.now() + SHANGHAI_OFFSET_MILLIS).toISOString().slice(0, 10)

function ensureDraftDate() {
  if (!draftDate.value) draftDate.value = shanghaiToday()
  return draftDate.value
}

const resolveNodeKind = (status: RecordStatus): NodeKind => {
  if (status === RecordStatus.SAVED) return 'saved'
  if (status === RecordStatus.UNLOCKED) return 'unlocked'
  if (status === RecordStatus.SEALED) return 'sealed'
  return 'draft'
}

// 仅整理展示日期；保留服务端分组/顺序和既有时区口径。
const decorateYear = (yearMonth: string) => {
  const match = /^(\d{4})-(\d{1,2})$/.exec(yearMonth)
  const month = match ? Number(match[2]) : 0
  return match && month >= 1 && month <= 12
    ? match[1]
    : '日期待确认'
}
const decorateItem = (item: TimelineItemVO): DecoratedItem => {
  const dateText = formatDateTime(item.createdAt)
  const dateParts = /^\d{4}-(\d{2})-(\d{2}) (\d{2}:\d{2})$/.exec(dateText)
  const context = [item.lifeNodeLabel?.trim(), ...(item.tagNames || []).map((tag) => tag.trim())]
    .filter((text): text is string => Boolean(text))
  return {
    id: item.id,
    raw: item,
    title: item.title?.trim() || '未命名片段',
    kind: resolveNodeKind(item.status),
    dateText,
    dateLabel: dateParts ? `${Number(dateParts[1])}月${Number(dateParts[2])}日 · ${dateParts[3]}` : '日期待确认',
    contextText: [...new Set(context)].join(' · '),
    hasImage: Boolean(item.cover),
  }
}
const decoratedGroups = computed(() => {
  const groups = timelineGroups.value.filter((group) => group.items.length > 0)
  return groups.map((group, index) => ({
    yearMonth: group.yearMonth,
    yearText: decorateYear(group.yearMonth),
    showYear: index === 0 || decorateYear(group.yearMonth) !== decorateYear(groups[index - 1].yearMonth),
    isLastGroup: index === groups.length - 1,
    items: group.items.map(decorateItem),
  }))
})

const ensureLogin = () => {
  if (!hasAuthenticatedSession()) {
    uni.reLaunch({ url: '/pages/login/index' })
    return false
  }
  return true
}

const loadTags = async () => {
  if (tagsLoading.value || availableTags.value.length) return
  tagsLoading.value = true
  tagsLoadFailed.value = false
  try {
    availableTags.value = await tagService.getTags()
  } catch {
    tagsLoadFailed.value = true
  } finally {
    tagsLoading.value = false
  }
}

const appliedGranularity = () => {
  if (appliedFilters.value.day) return 'DAY'
  if (appliedFilters.value.month) return 'MONTH'
  if (appliedFilters.value.year) return 'YEAR'
  return 'ALL'
}

const openFilterPanel = () => {
  draftTagId.value = appliedFilters.value.tagId
  draftGranularity.value = appliedGranularity()
  const year = appliedFilters.value.year
  const month = appliedFilters.value.month || 1
  const day = appliedFilters.value.day || 1
  draftDate.value = year
    ? `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
    : shanghaiToday()
  filterApplyFailed.value = false
  filterPanelVisible.value = true
  void loadTags()
}

const closeFilterPanel = () => {
  if (filterApplying.value) return
  filterPanelVisible.value = false
  filterApplyFailed.value = false
}

const chooseTag = (tagId?: number) => {
  draftTagId.value = draftTagId.value === tagId ? undefined : tagId
  filterApplyFailed.value = false
}

const chooseGranularity = (granularity: DateGranularity) => {
  draftGranularity.value = granularity
  if (granularity !== 'ALL') ensureDraftDate()
  filterApplyFailed.value = false
}

const onDateChange = (event: { detail: { value: string } }) => {
  const current = ensureDraftDate().split('-')
  const selected = event.detail.value.split('-')
  draftDate.value = [selected[0], selected[1] || current[1] || '01', selected[2] || current[2] || '01'].join('-')
  filterApplyFailed.value = false
}

const buildDraftFilters = (): TimelineFilterState => {
  const filters: TimelineFilterState = {}
  if (draftTagId.value) {
    filters.tagId = draftTagId.value
    filters.tagName = availableTags.value.find((tag) => tag.id === draftTagId.value)?.name
  }
  if (draftGranularity.value === 'ALL') return filters

  const [year, month, day] = ensureDraftDate().split('-').map(Number)
  filters.year = year
  if (draftGranularity.value === 'MONTH' || draftGranularity.value === 'DAY') filters.month = month
  if (draftGranularity.value === 'DAY') filters.day = day
  return filters
}

const toTimelineQuery = (filters: TimelineFilterState, pageNum: number): TimelineQuery => ({
  tagId: filters.tagId,
  year: filters.year,
  month: filters.month,
  day: filters.day,
  pageNum,
  pageSize: PAGE_SIZE,
})

const mergeTimelineGroups = (incoming: TimelineGroupVO[]) => {
  const groups = timelineGroups.value.map((group) => ({ ...group, items: [...group.items] }))
  const existingIds = new Set(groups.flatMap((group) => group.items.map((item) => item.id)))
  incoming.forEach((incomingGroup) => {
    const target = groups.find((group) => group.yearMonth === incomingGroup.yearMonth)
    const newItems = incomingGroup.items.filter((item) => !existingIds.has(item.id))
    newItems.forEach((item) => existingIds.add(item.id))
    if (target) target.items.push(...newItems)
    else groups.push({ ...incomingGroup, items: [...newItems] })
  })
  return groups
}

const loadTimeline = async (options: { append?: boolean; filters?: TimelineFilterState } = {}) => {
  if (!ensureLogin()) return false
  const append = Boolean(options.append)
  if (append && (loading.value || loadingMore.value || !hasMore.value)) return false

  const filters = options.filters || appliedFilters.value
  const pageNum = append ? currentPage.value + 1 : 1
  const requestId = ++latestRequestId
  if (append) {
    loadingMore.value = true
    loadMoreFailed.value = false
  } else {
    loading.value = true
    timelineLoadFailed.value = false
  }

  try {
    const result = await recordService.getTimeline(toTimelineQuery(filters, pageNum))
    if (requestId !== latestRequestId) return false

    timelineGroups.value = append ? mergeTimelineGroups(result.groups) : result.groups
    timelineTotal.value = result.total
    currentPage.value = result.pageNum
    hasMore.value = result.hasMore
    if (!append) appliedFilters.value = { ...filters }
    void loadCovers(result.groups.flatMap((group) => group.items
      .filter((item) => Boolean(item.cover))
      .map((item) => ({ recordId: item.id, cover: item.cover }))))
    return true
  } catch {
    if (requestId !== latestRequestId) return false
    if (append) loadMoreFailed.value = true
    else timelineLoadFailed.value = true
    return false
  } finally {
    if (requestId === latestRequestId) {
      loading.value = false
      loadingMore.value = false
    }
  }
}

const applyFilters = async () => {
  if (filterApplying.value) return
  filterApplying.value = true
  filterApplyFailed.value = false
  const succeeded = await loadTimeline({ filters: buildDraftFilters() })
  filterApplying.value = false
  if (succeeded) filterPanelVisible.value = false
  else filterApplyFailed.value = true
}

const resetFilters = async () => {
  if (filterApplying.value) return
  draftTagId.value = undefined
  draftGranularity.value = 'ALL'
  filterApplying.value = true
  filterApplyFailed.value = false
  const succeeded = await loadTimeline({ filters: {} })
  filterApplying.value = false
  if (succeeded) filterPanelVisible.value = false
  else filterApplyFailed.value = true
}

const loadMoreTimeline = () => {
  void loadTimeline({ append: true })
}

const retryTimeline = () => {
  void loadTimeline()
}

const retryLoadMore = () => {
  loadMoreFailed.value = false
  void loadTimeline({ append: true })
}

const openNode = (item: TimelineItemVO) => {
  if (item.status === RecordStatus.DRAFT) {
    uni.navigateTo({ url: `/pages/record-editor/index?id=${item.id}&source=timeline` })
    return
  }
  uni.navigateTo({ url: `/pages/record-detail/index?id=${item.id}&source=timeline` })
}

const goHome = () => {
  uni.switchTab({ url: '/pages/home/index' })
}

const goUserCenter = () => {
  uni.switchTab({ url: '/pages/user-center/index' })
}

onShow(() => {
  uni.hideTabBar({ animation: false })
  void loadTags()
  void loadTimeline()
})
</script>

<template>
  <view class="page fb-theme">
    <PreviewModeNotice />
    <AppTopSafeBar title="时光回序" padding-x="56rpx" side-width="104rpx" center-title>
      <template #title><text class="brand-mark">时光回序</text></template>
      <template #left>
        <button class="filter-trigger" :class="{ active: hasAppliedFilter }" hover-class="filter-trigger--pressed" @tap="openFilterPanel">筛选</button>
      </template>
    </AppTopSafeBar>

    <!-- filter overlay -->
    <view v-if="filterPanelVisible" class="filter-layer" @tap="closeFilterPanel">
      <scroll-view class="filter-sheet" scroll-y :show-scrollbar="false" @tap.stop>
        <view class="filter-sheet-head">
          <text class="filter-sheet-title">筛选时光</text>
          <text class="filter-sheet-close" @tap="closeFilterPanel">收起</text>
        </view>

        <view class="filter-section">
          <text class="filter-section-label">标签</text>
          <view v-if="tagsLoading" class="filter-inline-state">正在整理标签...</view>
          <view v-else-if="tagsLoadFailed" class="filter-inline-state filter-inline-error">
            <text>标签暂未展开</text>
            <text class="filter-inline-retry" @tap="loadTags">重试</text>
          </view>
          <scroll-view v-else class="filter-chip-scroll" scroll-x :show-scrollbar="false">
            <view class="filter-chip-row">
              <view
                class="filter-chip"
                :class="{ active: !draftTagId }"
                @tap="chooseTag(undefined)"
              >全部</view>
              <view
                v-for="tag in availableTags"
                :key="tag.id"
                class="filter-chip"
                :class="{ active: draftTagId === tag.id }"
                @tap="chooseTag(tag.id)"
              >{{ tag.name }}</view>
            </view>
          </scroll-view>
        </view>

        <view class="filter-section">
          <text class="filter-section-label">写下的时间</text>
          <view class="filter-granularity-row">
            <view
              v-for="option in dateGranularityOptions"
              :key="option.value"
              class="filter-granularity"
              :class="{ active: draftGranularity === option.value }"
              @tap="chooseGranularity(option.value)"
            >{{ option.label }}</view>
          </view>
          <picker
            v-if="draftGranularity !== 'ALL'"
            mode="date"
            :fields="pickerFields"
            :value="ensureDraftDate()"
            @change="onDateChange"
          >
            <view class="filter-date-picker">
              <text>{{ draftDateText }}</text>
              <text class="filter-date-action">更改</text>
            </view>
          </picker>
        </view>

        <text class="filter-sheet-meta">当前：{{ appliedFilterText }} · 已载入 {{ flatCount }}/{{ timelineTotal }} 则</text>
        <text v-if="filterApplyFailed" class="filter-apply-error">没有筛选成功，原有时光轴仍被保留</text>
        <view class="filter-actions">
          <view
            class="filter-action filter-action-ghost"
            :class="{ disabled: filterApplying }"
            @tap="resetFilters"
          >重置</view>
          <view
            class="filter-action"
            :class="{ disabled: filterApplying }"
            @tap="applyFilters"
          >{{ filterApplying ? '正在展开...' : '应用筛选' }}</view>
        </view>
      </scroll-view>
    </view>

    <scroll-view
      class="scroll-body"
      scroll-y
      enhanced
      :show-scrollbar="false"
      :lower-threshold="160"
      @scrolltolower="loadMoreTimeline"
    >

      <!-- page header -->
      <view class="page-header">
        <text class="page-title">时光轴</text>
        <text class="page-subtitle">沿着时间，读一读曾经留下的此刻。</text>
        <button v-if="hasAppliedFilter" class="applied-filter" @tap="openFilterPanel">
          <text>当前：{{ appliedFilterText }}</text><text class="applied-filter-action">更改</text>
        </button>
        <view v-if="showStaleNotice" class="stale-notice">
          <text>同步稍慢，当前仍显示 {{ appliedFilterText }}</text>
          <text class="stale-action" @tap="retryTimeline">重试</text>
        </view>
      </view>

      <!-- 每条记录是一个阅读单元；时间、状态收在同一行。 -->
      <view class="timeline-wrap">
        <view v-if="loading" class="tl-content" aria-label="正在展开时光轴">
          <view v-for="n in 3" :key="`sk-${n}`" class="skeleton-entry">
            <view class="skeleton-date" /><view class="skeleton-title" />
          </view>
        </view>

        <view v-else-if="showLoadFailureState" class="state-block">
          <text class="state-title">暂时没有展开</text>
          <text class="state-desc">网络稍慢，请再试一次</text>
          <view class="state-action" @tap="retryTimeline">重新整理</view>
        </view>

        <view v-else-if="showEmptyState" class="state-block">
          <text class="state-title">这一段还很安静</text>
          <text class="state-desc">{{ emptyStateText }}</text>
        </view>

        <view v-else class="tl-content">
          <view v-for="group in decoratedGroups" :key="group.yearMonth" class="timeline-group">
            <view v-if="group.showYear" class="year-heading"><text>{{ group.yearText }}</text></view>

            <view
              v-for="(item, itemIndex) in group.items"
              :key="item.id"
              class="tl-item"
              :class="{ 'tl-item--last': group.isLastGroup && itemIndex === group.items.length - 1 }"
              hover-class="tl-item--pressed"
              role="button"
              :aria-label="item.dateText + '，' + item.title + '，' + (item.kind === 'sealed' ? '封存中' : item.kind === 'unlocked' ? '已抵达' : item.kind === 'saved' ? '已留下' : '草稿')"
              @tap="openNode(item.raw)"
            >
              <view class="entry-thread" />
              <view class="entry-marker" />
              <view class="entry-body">
                <view class="card">
                  <view class="entry-meta">
                    <text class="entry-date">{{ item.dateLabel }}</text>
                    <text v-if="item.kind !== 'saved'" class="entry-status" :class="{ 'entry-status--sealed': item.kind === 'sealed' }">{{ item.kind === 'unlocked' ? '已抵达' : item.kind === 'sealed' ? '封存中' : '草稿' }}</text>
                    <text class="entry-arrow" aria-hidden="true">›</text>
                  </view>
                  <text class="card-title">{{ item.title }}</text>
                  <view v-if="item.hasImage" class="card-photo">
                    <image
                      v-if="coverUrls[item.id]"
                      class="card-cover-image"
                      :src="coverUrls[item.id]"
                      mode="aspectFill"
                      @error="markCoverFailed(item.id)"
                    />
                    <view v-else class="card-img-fallback">
                      <view class="card-img-icon" />
                      <text class="card-img-error">{{ coverErrors[item.id] ? '封面暂不可用' : '封面正在展开' }}</text>
                    </view>
                  </view>
                  <text v-if="item.contextText" class="entry-context">{{ item.contextText }}</text>
                </view>
              </view>
            </view>
          </view>
        </view>
      </view>

      <view v-if="timelineGroups.length && !loading" class="load-more-block">
        <text v-if="loadingMore" class="load-more-text">正在继续展开...</text>
        <view v-else-if="loadMoreFailed" class="load-more-retry" @tap="retryLoadMore">
          <text>后面的片段暂未展开</text>
          <text class="load-more-action">重试</text>
        </view>
        <text v-else-if="hasMore" class="load-more-text">继续向下，回到更早的时光</text>
        <text v-else class="load-more-text">已展开 {{ timelineTotal }} 则片段</text>
      </view>

      <!-- tail -->
      <view class="tail">
        <text class="tail-text">回溯的终点，亦是感知的起点</text>
      </view>

      <view class="nav-safe-area" />
    </scroll-view>

    <!-- bottom navigation -->
    <view class="bottom-nav-shell">
      <view class="bottom-nav">
        <view class="nav-item" @tap="goHome">
          <text class="nav-label">首页</text>
        </view>
        <view class="nav-item active" @tap="() => {}">
          <text class="nav-label">时光轴</text>
          <view class="nav-dot" />
        </view>
        <view class="nav-item" @tap="goUserCenter">
          <text class="nav-label">个人中心</text>
        </view>
      </view>
    </view>
  </view>
</template>

<style scoped>
.page {
  position: relative;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--fb-paper, #faf7f2);
  overflow: hidden;
  font-family: var(--fb-letter-font, 'Songti SC', 'STSong', 'SimSun', Georgia, serif);
}
.brand-mark { font-family: var(--fb-letter-font, 'Songti SC', 'STSong', 'SimSun', Georgia, serif); font-size: 24rpx; font-weight: 400; letter-spacing: 8rpx; text-indent: 8rpx; color: var(--fb-ink-light, #9e9890); white-space: nowrap; }





.scroll-body {
  position: relative;
  z-index: 1;
  flex: 1;
  height: 0;
  min-height: 0;
  width: 100%;
}

/* ── filter overlay ── */
.filter-layer {
  position: fixed;
  inset: 0;
  z-index: 40;
  background: rgba(48, 46, 41, 0.18);
  display: flex;
  align-items: flex-end;
}

.filter-sheet {
  width: 100%;
  height: 72vh;
  box-sizing: border-box;
  padding: 36rpx 32rpx calc(env(safe-area-inset-bottom) + 36rpx);
  border-radius: 24rpx 24rpx 0 0;
  border-top: 1rpx solid var(--fb-letter-glass-edge, rgba(255, 255, 255, 0.85));
  background: var(--fb-letter-glass, #f3eee4);
  box-shadow: var(--fb-letter-glass-shadow, inset 0 2rpx 0 rgba(255, 255, 255, 0.9), 0 6rpx 18rpx rgba(125, 105, 75, 0.10));
}

.filter-sheet-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.filter-sheet-title {
  font-family: var(--fb-font-serif, 'Noto Serif SC', 'Songti SC', 'STSong', 'SimSun', Georgia, serif);
  font-size: 34rpx;
  color: var(--fb-ink, #302e29);
}

.filter-sheet-close {
  font-size: 24rpx;
  color: var(--fb-ink-light, #9e9890);
}

.filter-section {
  margin-top: 28rpx;
}

.filter-section-label {
  display: block;
  margin-bottom: 16rpx;
  font-size: 23rpx;
  color: var(--fb-ink-light, #9e9890);
  letter-spacing: 0.08em;
}

.filter-chip-scroll {
  width: 100%;
  white-space: nowrap;
}

.filter-chip-row {
  display: inline-flex;
  gap: 12rpx;
  padding-right: 8rpx;
}

.filter-chip,
.filter-granularity {
  min-height: 62rpx;
  padding: 0 24rpx;
  border: 1rpx solid rgba(200, 194, 184, 0.36);
  border-radius: 4rpx;
  background: rgba(245, 240, 232, 0.76);
  color: var(--fb-ink-mid, #6b6560);
  font-size: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.filter-chip.active,
.filter-granularity.active {
  border-color: rgba(181, 53, 42, 0.32);
  background: rgba(181, 53, 42, 0.08);
  color: var(--fb-vermilion, #b5352a);
}

.filter-granularity-row {
  display: flex;
  gap: 10rpx;
}

.filter-granularity {
  flex: 1;
  padding: 0 12rpx;
}

.filter-date-picker {
  margin-top: 16rpx;
  min-height: 76rpx;
  padding: 0 22rpx;
  border-radius: 4rpx;
  background: rgba(245, 240, 232, 0.76);
  color: var(--fb-ink, #302e29);
  font-size: 26rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.filter-date-action,
.filter-inline-retry {
  color: var(--fb-vermilion, #b5352a);
}

.filter-inline-state {
  min-height: 62rpx;
  color: var(--fb-ink-light, #9e9890);
  font-size: 24rpx;
  display: flex;
  align-items: center;
  gap: 18rpx;
}

.filter-inline-error {
  color: var(--fb-ink-mid, #6b6560);
}

.filter-sheet-meta {
  display: block;
  margin-top: 18rpx;
  font-size: 23rpx;
  color: var(--fb-ink-light, #9e9890);
}

.filter-apply-error {
  display: block;
  margin-top: 14rpx;
  color: var(--fb-vermilion, #b5352a);
  font-size: 23rpx;
}

.filter-actions {
  margin-top: 24rpx;
  display: flex;
  gap: 16rpx;
}

.filter-action {
  flex: 1;
  min-height: 84rpx;
  border-radius: 4rpx;
  background: var(--fb-vermilion, #b5352a);
  color: #ffffff;
  font-size: 26rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.filter-action-ghost {
  background: rgba(245, 240, 232, 0.96);
  color: var(--fb-ink-mid, #6b6560);
}

.filter-action.disabled {
  opacity: 0.55;
}

/* ── topbar ── */




.filter-trigger {
  margin: 0;
  padding: 0;
  line-height: 1.6;
  width: 104rpx;
  min-height: 60rpx;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1rpx solid var(--fb-letter-glass-edge, rgba(255, 255, 255, 0.85));
  border-radius: 999rpx;
  background: var(--fb-letter-glass, #f3eee4);
  box-shadow: var(--fb-letter-glass-shadow, inset 0 2rpx 0 rgba(255, 255, 255, 0.9), 0 6rpx 18rpx rgba(125, 105, 75, 0.10));
  color: var(--fb-ink-mid, #6b6560);
  font-size: 22rpx;
  letter-spacing: 0.12em;
}

.filter-trigger.active {
  border-color: rgba(181, 53, 42, 0.32);
  background: rgba(181, 53, 42, 0.08);
  color: var(--fb-vermilion, #b5352a);
}

.filter-trigger--pressed {
  opacity: 0.68;
}



/* ── page header ── */
.page-header {
  padding: 40rpx 56rpx 0;
}

.page-title {
  display: block;
  font-size: 60rpx;
  font-weight: 400;
  color: var(--fb-ink, #302e29);
  line-height: 1.4;
}

.page-subtitle {
  display: block;
  margin-top: 16rpx;
  font-size: 26rpx;
  color: var(--fb-ink-mid, #6b6560);
  line-height: 1.8;
}

.applied-filter {
  margin: 24rpx 0 0;
  padding: 16rpx 20rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  border: 1rpx solid var(--fb-letter-line, rgba(188, 174, 152, 0.28));
  border-radius: 4rpx;
  background: var(--fb-paper-2, #f5f0e8);
  font-size: var(--fb-font-meta, 24rpx);
  font-weight: 400;
  line-height: 1.6;
  color: var(--fb-ink-mid, #6b6560);
  text-align: left;
  overflow-wrap: break-word;
}
.applied-filter-action { flex-shrink: 0; color: var(--fb-verm-dim, #8a271f); }
.applied-filter::after { border: 0; }

.stale-notice {
  margin-top: 20rpx;
  font-size: 24rpx;
  color: var(--fb-ink-mid, #6b6560);
  display: flex;
  gap: 16rpx;
}

.stale-action {
  color: var(--fb-vermilion, #b5352a);
}

/* 纸片承载完整记录，时间线仅作轻引导；无封面也保持清晰边界。 */
.timeline-wrap {
  margin-top: 40rpx;
  padding: 0 56rpx;
}

.tl-content { min-width: 0; }
.timeline-group {
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.year-heading {
  margin-bottom: 24rpx;
  padding-left: 36rpx;
  font-size: 32rpx;
  font-weight: 400;
  line-height: 1.5;
  font-variant-numeric: tabular-nums;
  color: var(--fb-ink-mid, #6b6560);
}
.tl-item {
  position: relative;
  padding-left: 36rpx;
  margin-bottom: 32rpx;
  min-width: 0;
}
.tl-item--last { margin-bottom: 0; }
.tl-item--pressed .entry-body { opacity: 0.72; }
.entry-marker {
  position: absolute;
  left: 0;
  top: 38rpx;
  width: 16rpx;
  height: 16rpx;
  box-sizing: border-box;
  border: 1rpx solid var(--fb-ink-faint, #c8c2b8);
  border-radius: 50%;
  background: var(--fb-paper, #faf7f2);
  z-index: 1;
}
.entry-thread {
  position: absolute;
  top: 46rpx;
  bottom: -78rpx;
  left: 8rpx;
  width: 1rpx;
  background: var(--fb-letter-line, rgba(188, 174, 152, 0.28));
}
.tl-item--last .entry-thread { display: none; }
.entry-body { min-width: 0; width: 100%; }
.card {
  box-sizing: border-box;
  padding: 28rpx;
  background: var(--fb-letter-paper, #fcf9f4);
  border: 1rpx solid var(--fb-letter-line, rgba(188, 174, 152, 0.28));
  border-radius: 4rpx;
  box-shadow: none;
}
.entry-meta { display: flex; align-items: center; gap: 12rpx; }
.entry-date {
  flex: 1;
  min-width: 0;
  font-family: var(--fb-font-sans, 'Noto Sans SC', 'PingFang SC', sans-serif);
  font-size: var(--fb-font-meta, 24rpx);
  font-weight: 400;
  line-height: 1.5;
  font-variant-numeric: tabular-nums;
  color: var(--fb-ink-mid, #6b6560);
  overflow-wrap: break-word;
}
.entry-status {
  flex-shrink: 0;
  padding: 2rpx 10rpx;
  border-radius: 3rpx;
  background: var(--fb-paper-2, #f5f0e8);
  font-size: var(--fb-font-meta, 24rpx);
  font-weight: 400;
  line-height: 1.5;
  color: var(--fb-ink-mid, #6b6560);
}
.entry-status--sealed { color: var(--fb-verm-dim, #8a271f); }
.entry-arrow { flex-shrink: 0; font-size: 30rpx; line-height: 1; color: var(--fb-ink-mid, #6b6560); }
.card-photo {
  margin-top: 22rpx;
  height: 280rpx;
  width: 100%;
  overflow: hidden;
  background: var(--fb-paper-2, #f5f0e8);
  border-radius: 2rpx;
}
.card-cover-image, .card-img-fallback { display: block; width: 100%; height: 100%; }
.card-img-fallback {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16rpx;
}
.card-img-icon {
  width: 48rpx;
  height: 36rpx;
  border: 1rpx solid var(--fb-ink-faint, #c8c2b8);
  border-radius: 2rpx;
}
.card-img-error {
  font-size: var(--fb-font-meta, 24rpx);
  font-weight: 400;
  line-height: 1.5;
  color: var(--fb-ink-mid, #6b6560);
}
.card-title {
  display: block;
  margin-top: 14rpx;
  font-size: var(--fb-font-title-sub, 36rpx);
  font-weight: 400;
  line-height: 1.55;
  color: var(--fb-ink, #302e29);
  overflow-wrap: break-word;
  word-break: break-all;
}
.entry-context {
  display: block;
  margin-top: 16rpx;
  font-size: var(--fb-font-meta, 24rpx);
  font-weight: 400;
  line-height: 1.6;
  color: var(--fb-ink-mid, #6b6560);
  overflow-wrap: break-word;
  word-break: break-all;
}
.skeleton-entry { margin: 0 0 32rpx 36rpx; padding: 28rpx; background: var(--fb-letter-paper, #fcf9f4); }
.skeleton-date { width: 180rpx; height: 20rpx; background: var(--fb-paper-2, #f5f0e8); }
.skeleton-title { width: 60%; height: 28rpx; margin-top: 22rpx; background: var(--fb-paper-2, #f5f0e8); }

/* ── state block ── */
.state-block {
  padding: 40rpx 0;
}

.state-title {
  display: block;
  font-family: var(--fb-font-serif, 'Noto Serif SC', 'Songti SC', 'STSong', 'SimSun', Georgia, serif);
  font-size: 36rpx;
  color: var(--fb-ink, #302e29);
}

.state-desc {
  display: block;
  margin-top: 16rpx;
  font-size: 26rpx;
  color: var(--fb-ink-mid, #6b6560);
  line-height: 1.7;
}

.state-action {
  margin-top: 24rpx;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 74rpx;
  padding: 0 30rpx;
  border-radius: 4rpx;
  background: var(--fb-vermilion, #b5352a);
  color: #ffffff;
  font-size: 26rpx;
}

/* ── load more / tail ── */
.load-more-block {
  padding: 8rpx 56rpx 0;
  text-align: center;
}

.load-more-text,
.load-more-retry {
  color: var(--fb-ink-mid, #6b6560);
  font-size: var(--fb-font-meta, 24rpx);
  line-height: 1.6;
}

.load-more-retry {
  display: flex;
  justify-content: center;
  gap: 18rpx;
}

.load-more-action {
  color: var(--fb-vermilion, #b5352a);
}

.tail {
  padding: 48rpx 56rpx 0;
  text-align: center;
}

.tail-text {
  font-family: var(--fb-font-serif, 'Noto Serif SC', 'Songti SC', 'STSong', 'SimSun', Georgia, serif);
  font-size: var(--fb-font-meta, 24rpx);
  font-weight: 400;
  color: var(--fb-ink-mid, #6b6560);
  letter-spacing: 0.12em;
}

/* ── nav safe area ── */
.nav-safe-area {
  height: calc(190rpx + env(safe-area-inset-bottom));
}

/* ── bottom navigation ── */
.bottom-nav-shell {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  flex-shrink: 0;
  z-index: 30;
  padding: 0;
  background: transparent;
  pointer-events: none;
}

.bottom-nav {
  min-height: 112rpx;
  padding-bottom: env(safe-area-inset-bottom);
  box-sizing: content-box;
  display: flex;
  align-items: stretch;
  border: 0;
  border-top: 1rpx solid rgba(188,174,152,.35);
  border-radius: 24rpx 24rpx 0 0;
  background: rgba(247,242,233,.9);
  box-shadow: 0 -4rpx 36rpx rgba(125,105,75,.10);
  pointer-events: auto;
}

@supports ((backdrop-filter: blur(1px)) or (-webkit-backdrop-filter: blur(1px))) {
  .bottom-nav {
    /* autoprefixer: off */
    background: rgba(250,247,242,.64);
    -webkit-backdrop-filter: blur(18px) saturate(125%);
    backdrop-filter: blur(18px) saturate(125%);
  }
}

.nav-item {
  flex: 1;
  min-width: 0;
  min-height: 88rpx;
  padding: 14rpx 8rpx;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6rpx;
}

.nav-label {
  font-size: 26rpx;
  color: var(--fb-ink-mid, #6b6560);
  text-align: center;
}

.nav-item.active .nav-label {
  color: var(--fb-ink, #302e29);
  font-weight: 300;
}

.nav-dot {
  width: 6rpx;
  height: 6rpx;
  border-radius: 50%;
  background: var(--fb-vermilion, #b5352a);
  margin-top: 4rpx;
}

.filter-trigger::after { border: none; }

/* 窄屏保留可读字级；不把日期与操作文字一起等比缩小。 */
@media (max-width: 360px) {
  .entry-date, .entry-status, .entry-context, .card-img-error,
  .applied-filter, .load-more-text, .load-more-retry, .tail-text { font-size: 12px; }
  .card-title { font-size: 18px; }
  .year-heading { font-size: 16px; }
}
</style>
