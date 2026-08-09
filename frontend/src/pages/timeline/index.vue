<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
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

type NodeKind = 'sealed' | 'unlocked' | 'draft' | 'locked'
type DateGranularity = 'ALL' | 'YEAR' | 'MONTH' | 'DAY'

interface DecoratedItem {
  id: number
  raw: TimelineItemVO
  title: string
  kind: NodeKind
  dateText: string
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
  if (status === RecordStatus.UNLOCKED) return 'unlocked'
  if (status === RecordStatus.SEALED) return 'sealed'
  return 'draft'
}

const decoratedGroups = computed(() =>
  timelineGroups.value.map((group) => ({
    yearMonth: group.yearMonth,
    items: group.items.map<DecoratedItem>((item) => ({
      id: item.id,
      raw: item,
      title: item.title?.trim() || '未命名片段',
      kind: resolveNodeKind(item.status),
      dateText: formatDateTime(item.createdAt),
      hasImage: Boolean(item.cover),
    })),
  }))
)

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
  <view class="page">
    <PreviewModeNotice />
    <view class="paper-texture" />
    <view class="paper-glow" />

    <!-- filter overlay -->
    <view v-if="filterPanelVisible" class="filter-layer" @tap="closeFilterPanel">
      <view class="filter-sheet" @tap.stop>
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
      </view>
    </view>

    <scroll-view
      class="scroll-body"
      scroll-y
      enhanced
      :show-scrollbar="false"
      :lower-threshold="160"
      @scrolltolower="loadMoreTimeline"
    >

      <!-- topbar -->
      <view class="topbar">
        <view
          class="filter-trigger"
          :class="{ active: hasAppliedFilter }"
          hover-class="filter-trigger--pressed"
          @tap="openFilterPanel"
        >筛选</view>
        <text class="logo">时 光 回 序</text>
        <view class="topbar-placeholder" />
      </view>

      <!-- page header -->
      <view class="page-header">
        <text class="page-title">时 间 长 廊</text>
        <text class="page-subtitle">在此处，凝视那些被封存的往昔<text>\n</text>与尚未开启的明日。</text>
        <view class="deco-line" />
        <view v-if="showStaleNotice" class="stale-notice">
          <text>同步稍慢，当前仍显示 {{ appliedFilterText }}</text>
          <text class="stale-action" @tap="retryTimeline">重试</text>
        </view>
      </view>

      <!-- timeline body -->
      <view class="timeline-wrap">
        <view class="timeline-track" />

        <!-- loading skeleton -->
        <view v-if="loading" class="tl-content">
          <view v-for="n in 3" :key="`sk-${n}`" class="tl-item">
            <view class="tl-dot"><view class="tl-dot-inner" /></view>
            <view class="skeleton-date" />
            <view class="skeleton-title" />
          </view>
        </view>

        <!-- load failure -->
        <view v-else-if="showLoadFailureState" class="tl-content">
          <view class="state-block">
            <text class="state-title">暂时没有展开</text>
            <text class="state-desc">网络稍慢，请再试一次</text>
            <view class="state-action" @tap="retryTimeline">重新整理</view>
          </view>
        </view>

        <!-- empty -->
        <view v-else-if="showEmptyState" class="tl-content">
          <view class="state-block">
            <text class="state-title">这一段还很安静</text>
            <text class="state-desc">{{ emptyStateText }}</text>
          </view>
        </view>

        <!-- timeline groups -->
        <view v-else class="tl-content">
          <!-- "此时此刻" node at top -->
          <view class="tl-item">
            <view class="tl-dot tl-dot-now">
              <view class="tl-dot-inner tl-dot-inner-now" />
            </view>
            <text class="now-tag">此时此刻</text>
          </view>

          <view v-for="group in decoratedGroups" :key="group.yearMonth">
            <view v-for="item in group.items" :key="item.id" class="tl-item" @tap="openNode(item.raw)">

              <!-- sealed / arriving card -->
              <template v-if="item.kind === 'sealed'">
                <view class="tl-dot tl-dot-sealed">
                  <view class="tl-dot-inner tl-dot-inner-sealed" />
                </view>
                <text class="tl-date">{{ item.dateText }}</text>
                <view class="card-locked card-arriving">
                  <view v-if="item.hasImage" class="card-img-placeholder card-img-placeholder--compact">
                    <image
                      v-if="coverUrls[item.id]"
                      class="card-cover-image"
                      :src="coverUrls[item.id]"
                      mode="aspectFill"
                      @error="markCoverFailed(item.id)"
                    />
                    <view v-else class="card-img-fallback">
                      <view class="card-img-icon" />
                      <text v-if="coverErrors[item.id]" class="card-img-error">封面暂不可用</text>
                    </view>
                  </view>
                  <view class="card-meta">
                    <view class="seal"><text class="seal-char">待</text></view>
                    <text class="card-tag">即将抵达</text>
                  </view>
                  <text class="card-title card-title-dim">{{ item.title }}</text>
                  <view class="countdown-badge">
                    <view class="countdown-dot" />
                    <text class="countdown-text">封存中</text>
                  </view>
                </view>
              </template>

              <!-- unlocked card with image placeholder -->
              <template v-else-if="item.kind === 'unlocked'">
                <view class="tl-dot tl-dot-open">
                  <view class="tl-dot-inner tl-dot-inner-open" />
                </view>
                <text class="tl-date">{{ item.dateText }}</text>
                <view class="card">
                  <view class="card-img-placeholder">
                    <image
                      v-if="item.hasImage && coverUrls[item.id]"
                      class="card-cover-image"
                      :src="coverUrls[item.id]"
                      mode="aspectFill"
                      @error="markCoverFailed(item.id)"
                    />
                    <view v-else class="card-img-fallback">
                      <view class="card-img-icon" />
                      <text v-if="item.hasImage && coverErrors[item.id]" class="card-img-error">封面暂不可用</text>
                    </view>
                  </view>
                  <view class="card-meta">
                    <view class="seal seal-open"><text class="seal-char seal-char-open">封</text></view>
                    <text class="card-tag">{{ item.hasImage ? '已解封 · 图文记忆' : '已解封 · 时间回看' }}</text>
                  </view>
                  <text class="card-title">{{ item.title }}</text>
                  <view class="card-footer">
                    <text class="card-footer-tag">MEMORY</text>
                  </view>
                </view>
              </template>

              <!-- draft / locked card -->
              <template v-else>
                <view class="tl-dot tl-dot-locked">
                  <view class="tl-dot-inner tl-dot-inner-locked" />
                </view>
                <text class="tl-date">{{ item.dateText }}</text>
                <view class="card-locked">
                  <view v-if="item.hasImage" class="card-img-placeholder card-img-placeholder--compact">
                    <image
                      v-if="coverUrls[item.id]"
                      class="card-cover-image"
                      :src="coverUrls[item.id]"
                      mode="aspectFill"
                      @error="markCoverFailed(item.id)"
                    />
                    <view v-else class="card-img-fallback">
                      <view class="card-img-icon" />
                      <text v-if="coverErrors[item.id]" class="card-img-error">封面暂不可用</text>
                    </view>
                  </view>
                  <view class="card-locked-title">
                    <view class="lock-icon" />
                    <text>{{ item.title }}</text>
                  </view>
                  <view class="countdown-badge countdown-badge-dim">
                    <view class="countdown-dot countdown-dot-dim" />
                    <text class="countdown-text">草稿</text>
                  </view>
                </view>
              </template>

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
          <text class="nav-label">首 页</text>
        </view>
        <view class="nav-item active" @tap="() => {}">
          <text class="nav-label">时光轴</text>
          <view class="nav-dot" />
        </view>
        <view class="nav-item" @tap="goUserCenter">
          <text class="nav-label">我 的</text>
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
  background: linear-gradient(170deg, #faf7f2 0%, #f5f0e8 55%, #f0ebe0 100%);
  overflow: hidden;
}

.paper-texture {
  position: fixed;
  inset: 0;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='500' height='500'%3E%3Cfilter id='f'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.55' numOctaves='6' stitchTiles='stitch'/%3E%3CfeColorMatrix type='saturate' values='0.15'/%3E%3C/filter%3E%3Crect width='500' height='500' filter='url(%23f)' opacity='0.055'/%3E%3C/svg%3E");
  pointer-events: none;
  z-index: 0;
}

.paper-glow {
  position: fixed;
  inset: 0;
  background:
    radial-gradient(ellipse 80% 50% at 18% 10%, rgba(200, 185, 158, 0.09) 0%, transparent 70%),
    radial-gradient(ellipse 60% 40% at 82% 25%, rgba(185, 168, 140, 0.06) 0%, transparent 65%),
    radial-gradient(ellipse 45% 55% at 70% 78%, rgba(178, 162, 135, 0.07) 0%, transparent 65%),
    radial-gradient(ellipse 65% 40% at 30% 85%, rgba(170, 155, 128, 0.05) 0%, transparent 65%),
    radial-gradient(ellipse 50% 35% at 50% 45%, rgba(250, 245, 238, 0.18) 0%, transparent 75%);
  pointer-events: none;
  z-index: 0;
}

.scroll-body {
  position: relative;
  z-index: 1;
  flex: 1;
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
  align-items: flex-start;
  justify-content: center;
  padding: calc(env(safe-area-inset-top) + 120px) 32rpx 0;
}

.filter-sheet {
  width: 100%;
  padding: 30rpx;
  border-radius: 4rpx;
  background: rgba(250, 247, 242, 0.96);
  box-shadow: 0 24rpx 60rpx rgba(48, 46, 41, 0.14);
}

.filter-sheet-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.filter-sheet-title {
  font-family: var(--fb-font-serif);
  font-size: 34rpx;
  color: var(--fb-ink);
}

.filter-sheet-close {
  font-size: 24rpx;
  color: var(--fb-ink-light);
}

.filter-section {
  margin-top: 28rpx;
}

.filter-section-label {
  display: block;
  margin-bottom: 16rpx;
  font-size: 23rpx;
  color: var(--fb-ink-light);
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
  color: var(--fb-ink-mid);
  font-size: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.filter-chip.active,
.filter-granularity.active {
  border-color: rgba(181, 53, 42, 0.32);
  background: rgba(181, 53, 42, 0.08);
  color: var(--fb-vermilion);
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
  color: var(--fb-ink);
  font-size: 26rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.filter-date-action,
.filter-inline-retry {
  color: var(--fb-vermilion);
}

.filter-inline-state {
  min-height: 62rpx;
  color: var(--fb-ink-light);
  font-size: 24rpx;
  display: flex;
  align-items: center;
  gap: 18rpx;
}

.filter-inline-error {
  color: var(--fb-ink-mid);
}

.filter-sheet-meta {
  display: block;
  margin-top: 18rpx;
  font-size: 23rpx;
  color: var(--fb-ink-light);
}

.filter-apply-error {
  display: block;
  margin-top: 14rpx;
  color: var(--fb-vermilion);
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
  background: var(--fb-vermilion);
  color: #ffffff;
  font-size: 26rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.filter-action-ghost {
  background: rgba(245, 240, 232, 0.96);
  color: var(--fb-ink-mid);
}

.filter-action.disabled {
  opacity: 0.55;
}

/* ── topbar ── */
.topbar {
  padding-top: calc(env(safe-area-inset-top) + 52px);
  padding-left: 56rpx;
  padding-right: 56rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.logo {
  font-family: var(--fb-font-serif);
  font-size: 24rpx;
  font-weight: 300;
  letter-spacing: 0.55em;
  color: var(--fb-ink-light);
}

.filter-trigger {
  width: 104rpx;
  min-height: 60rpx;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1rpx solid rgba(107, 101, 96, 0.28);
  border-radius: 999rpx;
  background: rgba(250, 247, 242, 0.78);
  color: var(--fb-ink-mid);
  font-size: 22rpx;
  letter-spacing: 0.12em;
}

.filter-trigger.active {
  border-color: rgba(181, 53, 42, 0.32);
  background: rgba(181, 53, 42, 0.08);
  color: var(--fb-vermilion);
}

.filter-trigger--pressed {
  opacity: 0.68;
}

.topbar-placeholder {
  width: 104rpx;
  height: 60rpx;
  flex-shrink: 0;
}

/* ── page header ── */
.page-header {
  padding: 56rpx 56rpx 0;
}

.page-title {
  display: block;
  font-family: var(--fb-font-serif);
  font-size: 56rpx;
  font-weight: 300;
  color: var(--fb-ink);
  letter-spacing: 0.04em;
  line-height: 1.3;
}

.page-subtitle {
  display: block;
  margin-top: 16rpx;
  font-family: var(--fb-font-serif);
  font-size: 24rpx;
  font-weight: 300;
  color: var(--fb-ink-light);
  letter-spacing: 0.06em;
  line-height: 1.8;
}

.deco-line {
  width: 64rpx;
  height: 1rpx;
  background: var(--fb-ink-faint);
  margin-top: 36rpx;
}

.stale-notice {
  margin-top: 20rpx;
  font-size: 24rpx;
  color: var(--fb-ink-mid);
  display: flex;
  gap: 16rpx;
}

.stale-action {
  color: var(--fb-vermilion);
}

/* ── timeline ── */
.timeline-wrap {
  position: relative;
  margin-top: 48rpx;
  padding: 0 56rpx 0 80rpx;
}

.timeline-track {
  position: absolute;
  left: 68rpx;
  top: 0;
  bottom: 0;
  width: 1rpx;
  background: linear-gradient(
    to bottom,
    transparent,
    rgba(181, 53, 42, 0.12) 6%,
    rgba(181, 53, 42, 0.12) 94%,
    transparent
  );
}

.tl-content {
  position: relative;
  z-index: 1;
}

/* ── timeline item ── */
.tl-item {
  position: relative;
  padding-left: 0;
  margin-bottom: 72rpx;
}

/* ── dots ── */
.tl-dot {
  position: absolute;
  left: -26rpx;
  top: 8rpx;
  width: 26rpx;
  height: 26rpx;
  border-radius: 50%;
  border: 2rpx solid var(--fb-ink-faint);
  background: #faf7f2;
  display: flex;
  align-items: center;
  justify-content: center;
  transform: translateX(-50%);
}

.tl-dot-now {
  border-color: rgba(181, 53, 42, 0.65);
  background: rgba(181, 53, 42, 0.09);
  animation: breathe-now 3s ease-in-out infinite;
}

.tl-dot-sealed {
  border-color: rgba(181, 53, 42, 0.5);
  background: rgba(181, 53, 42, 0.07);
  animation: breathe-sealed 3.6s ease-in-out infinite 0.4s;
}

.tl-dot-open {
  border-color: var(--fb-ink-faint);
}

.tl-dot-locked {
  border-color: var(--fb-ink-faint);
  opacity: 0.5;
}

.tl-dot-inner {
  width: 10rpx;
  height: 10rpx;
  border-radius: 50%;
  background: var(--fb-ink-faint);
}

.tl-dot-inner-now {
  background: var(--fb-vermilion);
  opacity: 1;
  animation: pulse-inner 2.4s ease-in-out infinite;
}

.tl-dot-inner-sealed {
  background: var(--fb-vermilion);
  opacity: 0.8;
  animation: pulse-inner 3s ease-in-out infinite 0.6s;
}

.tl-dot-inner-open {
  background: var(--fb-ink-mid);
  opacity: 0.8;
  animation: breathe-open 4s ease-in-out infinite 1s;
}

.tl-dot-inner-locked {
  background: var(--fb-ink-faint);
  opacity: 0.6;
}

/* "此时此刻" tag */
.now-tag {
  display: inline-flex;
  align-items: center;
  font-family: var(--fb-font-serif);
  font-size: 20rpx;
  font-weight: 300;
  color: var(--fb-vermilion);
  letter-spacing: 0.1em;
  opacity: 0.9;
  padding-left: 8rpx;
  gap: 10rpx;
}

.now-tag::before {
  content: '';
  display: block;
  width: 7rpx;
  height: 7rpx;
  border-radius: 50%;
  background: var(--fb-vermilion);
  flex-shrink: 0;
  animation: pulse-inner 2.4s ease-in-out infinite;
}

/* date label */
.tl-date {
  display: block;
  font-family: var(--fb-font-sans);
  font-size: 20rpx;
  font-weight: 300;
  color: var(--fb-ink-light);
  letter-spacing: 0.08em;
  margin-bottom: 16rpx;
}

/* ── regular card (unlocked) ── */
.card {
  position: relative;
  background: rgba(252, 249, 244, 0.72);
  border: 1rpx solid rgba(188, 174, 152, 0.28);
  border-radius: 2rpx;
  overflow: hidden;
  box-shadow:
    0 2rpx 0 rgba(255, 255, 255, 0.6) inset,
    0 4rpx 24rpx rgba(140, 120, 90, 0.06),
    0 2rpx 6rpx rgba(140, 120, 90, 0.04);
}

.card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 32rpx;
  bottom: 32rpx;
  width: 3rpx;
  background: linear-gradient(to bottom, transparent, rgba(181, 53, 42, 0.55) 25%, rgba(181, 53, 42, 0.55) 75%, transparent);
  border-radius: 2rpx;
}

.card::after {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  width: 24rpx;
  height: 24rpx;
  background: linear-gradient(225deg, rgba(230, 218, 200, 0.9) 0%, rgba(230, 218, 200, 0.9) 48%, rgba(252, 249, 244, 0) 50%);
  border-left: 1rpx solid rgba(188, 174, 152, 0.22);
  border-bottom: 1rpx solid rgba(188, 174, 152, 0.22);
}

/* image placeholder */
.card-img-placeholder {
  width: 100%;
  height: 240rpx;
  background: linear-gradient(135deg, #e8e0d5 0%, #d0c7ba 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.card-img-placeholder--compact {
  height: 180rpx;
}

.card-cover-image,
.card-img-fallback {
  width: 100%;
  height: 100%;
}

.card-cover-image {
  display: block;
}

.card-img-fallback {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10rpx;
}

.card-img-error {
  font-size: 18rpx;
  color: #7f756a;
}

.card-img-icon {
  width: 96rpx;
  height: 72rpx;
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 48 36' fill='none'><path d='M0 28L12 14L20 22L30 8L48 28' stroke='%239e9890' stroke-width='1' fill='none'/><ellipse cx='38' cy='8' rx='5' ry='5' stroke='%239e9890' stroke-width='1'/></svg>");
  background-repeat: no-repeat;
  background-position: center;
  background-size: contain;
  opacity: 0.25;
}

.card-meta {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 20rpx;
  padding: 36rpx 40rpx 0 44rpx;
}

.seal {
  width: 48rpx;
  height: 48rpx;
  border-radius: 50%;
  border: 2rpx solid var(--fb-vermilion);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  opacity: 0.8;
  box-shadow: 0 0 0 1rpx rgba(181, 53, 42, 0.1);
}

.seal-open {
  border-color: var(--fb-ink-mid);
  opacity: 0.6;
}

.seal-char {
  font-family: var(--fb-font-serif);
  font-size: 18rpx;
  color: var(--fb-vermilion);
}

.seal-char-open {
  color: var(--fb-ink-mid);
}

.card-tag {
  font-family: var(--fb-font-serif);
  font-size: 20rpx;
  font-weight: 300;
  color: var(--fb-ink-light);
  letter-spacing: 0.08em;
}

.card-title {
  display: block;
  font-family: var(--fb-font-serif);
  font-size: 30rpx;
  font-weight: 300;
  color: var(--fb-ink);
  letter-spacing: 0.03em;
  line-height: 1.6;
  margin-bottom: 12rpx;
  padding: 0 40rpx 0 44rpx;
}

.card-title-dim {
  color: var(--fb-ink-mid);
}

.card-footer {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 0 40rpx 36rpx 44rpx;
  margin-top: 20rpx;
}

.card-footer-tag {
  font-family: var(--fb-font-sans);
  font-size: 18rpx;
  font-weight: 300;
  color: var(--fb-ink-light);
  letter-spacing: 0.12em;
  border: 1rpx solid var(--fb-ink-faint);
  padding: 4rpx 16rpx;
  border-radius: 2rpx;
}

/* ── locked / sealed card ── */
.card-locked {
  position: relative;
  background: rgba(250, 247, 242, 0.45);
  border: 1rpx solid rgba(188, 174, 152, 0.18);
  border-radius: 2rpx;
  padding: 28rpx 40rpx 28rpx 44rpx;
  overflow: hidden;
}

.card-locked::before {
  content: '';
  position: absolute;
  left: 0;
  top: 24rpx;
  bottom: 24rpx;
  width: 3rpx;
  background: linear-gradient(to bottom, transparent, rgba(181, 53, 42, 0.3) 25%, rgba(181, 53, 42, 0.3) 75%, transparent);
}

.card-arriving {
  opacity: 0.8;
  background: rgba(252, 249, 244, 0.6);
  border-color: rgba(181, 53, 42, 0.18);
}

.card-arriving::before {
  background: linear-gradient(to bottom, transparent, rgba(181, 53, 42, 0.5) 25%, rgba(181, 53, 42, 0.5) 75%, transparent);
}

.card-arriving .card-meta {
  padding: 0;
  margin-bottom: 12rpx;
}

.card-arriving .card-title {
  padding: 0;
  margin-bottom: 0;
}

.card-locked-title {
  display: flex;
  align-items: center;
  gap: 16rpx;
  font-family: var(--fb-font-serif);
  font-size: 28rpx;
  font-weight: 300;
  color: var(--fb-ink-light);
  letter-spacing: 0.03em;
}

.lock-icon {
  width: 22rpx;
  height: 26rpx;
  position: relative;
  flex-shrink: 0;
  opacity: 0.6;
}

.lock-icon::before {
  content: '';
  position: absolute;
  width: 18rpx;
  height: 12rpx;
  border-radius: 2rpx;
  background: var(--fb-ink-light);
  bottom: 0;
  left: 2rpx;
}

.lock-icon::after {
  content: '';
  position: absolute;
  width: 14rpx;
  height: 12rpx;
  border: 3rpx solid var(--fb-ink-light);
  border-bottom: none;
  border-radius: 8rpx 8rpx 0 0;
  top: 0;
  left: 4rpx;
}

.countdown-badge {
  display: inline-flex;
  align-items: center;
  gap: 8rpx;
  margin-top: 12rpx;
}

.countdown-badge-dim {
  opacity: 0.45;
}

.countdown-dot {
  width: 6rpx;
  height: 6rpx;
  border-radius: 50%;
  background: var(--fb-vermilion);
  opacity: 0.8;
}

.countdown-dot-dim {
  background: var(--fb-ink-faint);
  opacity: 0.7;
}

.countdown-text {
  font-family: var(--fb-font-serif);
  font-size: 20rpx;
  font-weight: 300;
  color: var(--fb-ink-light);
  letter-spacing: 0.06em;
}

/* ── skeleton ── */
.skeleton-date {
  width: 160rpx;
  height: 20rpx;
  border-radius: 999rpx;
  background: rgba(200, 194, 184, 0.3);
  margin-bottom: 16rpx;
}

.skeleton-title {
  width: 80%;
  height: 28rpx;
  border-radius: 999rpx;
  background: rgba(200, 194, 184, 0.2);
}

/* ── state block ── */
.state-block {
  padding: 40rpx 0;
}

.state-title {
  display: block;
  font-family: var(--fb-font-serif);
  font-size: 36rpx;
  color: var(--fb-ink);
}

.state-desc {
  display: block;
  margin-top: 16rpx;
  font-size: 26rpx;
  color: var(--fb-ink-mid);
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
  background: var(--fb-vermilion);
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
  color: var(--fb-ink-light);
  font-size: 22rpx;
}

.load-more-retry {
  display: flex;
  justify-content: center;
  gap: 18rpx;
}

.load-more-action {
  color: var(--fb-vermilion);
}

.tail {
  padding: 48rpx 56rpx 0;
  text-align: center;
}

.tail-text {
  font-family: var(--fb-font-serif);
  font-size: 20rpx;
  font-weight: 300;
  color: var(--fb-ink-faint);
  letter-spacing: 0.12em;
}

/* ── nav safe area ── */
.nav-safe-area {
  height: 24rpx;
}

/* ── bottom navigation ── */
.bottom-nav-shell {
  position: relative;
  z-index: 80;
  padding: 0 0 env(safe-area-inset-bottom);
  background: transparent;
  border-top: 1rpx solid rgba(200, 194, 184, 0.3);
}

.bottom-nav {
  height: auto;
  padding: 32rpx 0 68rpx;
  display: flex;
  align-items: center;
  justify-content: space-around;
}

.nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  padding: 8rpx 32rpx;
}

.nav-label {
  font-family: var(--fb-font-serif);
  font-size: 24rpx;
  font-weight: 300;
  letter-spacing: 0.2em;
  color: var(--fb-ink-light);
  transition: color 0.2s;
}

.nav-item.active .nav-label {
  color: var(--fb-ink);
  font-weight: 300;
}

.nav-dot {
  width: 6rpx;
  height: 6rpx;
  border-radius: 50%;
  background: var(--fb-vermilion);
  margin-top: 4rpx;
}

@keyframes pulse-inner {
  0%, 100% { opacity: 0.3; transform: scale(0.75); }
  50% { opacity: 1; transform: scale(1.2); }
}

@keyframes breathe-now {
  0%, 100% { box-shadow: 0 0 0 0 rgba(181, 53, 42, 0); }
  50% { box-shadow: 0 0 0 3rpx rgba(181, 53, 42, 0.1); }
}

@keyframes breathe-sealed {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.75; }
}

@keyframes breathe-open {
  0%, 100% { opacity: 0.8; }
  50% { opacity: 0.45; }
}
</style>
