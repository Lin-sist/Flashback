<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { computed, ref } from 'vue'
import AppPageShell from '../../components/common/AppPageShell.vue'
import EmptyState from '../../components/common/EmptyState.vue'
import PrimaryButton from '../../components/common/PrimaryButton.vue'
import { useRecordStore } from '../../stores'
import {
  RecordStatus,
  type DateTimeValue,
  type RecordListItemVO,
} from '../../types'
import {
  calculateRemainingDays,
  formatDayText,
  getToken,
} from '../../utils'

const recordStore = useRecordStore()
const selectedStatus = ref<RecordStatus | 'ALL'>('ALL')
const appliedStatus = ref<RecordStatus | 'ALL'>('ALL')
const keyword = ref('')
const listLoadFailed = ref(false)

const statusOptions: { label: string; value: RecordStatus | 'ALL' }[] = [
  { label: '全部', value: 'ALL' },
  { label: '草稿', value: RecordStatus.DRAFT },
  { label: '已封存', value: RecordStatus.SEALED },
  { label: '已解锁', value: RecordStatus.UNLOCKED },
]

const statusLabelMap: Record<RecordStatus | 'ALL', string> = {
  ALL: '全部',
  [RecordStatus.DRAFT]: '草稿',
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

const totalCount = computed(() => filteredList.value.length)

const overviewCountText = computed(() => {
  if (recordStore.loading && totalCount.value === 0) {
    return '整理中'
  }
  if (listLoadFailed.value && totalCount.value === 0) {
    return '暂未同步'
  }
  return `共 ${totalCount.value} 份记录`
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
  if (keyword.value.trim()) {
    return '没有找到匹配的记录'
  }
  if (selectedStatus.value !== 'ALL') {
    return '当前筛选下还没有记录'
  }
  return '档案还空着，去写下第一份记忆吧'
})

const ensureLogin = () => {
  if (!getToken()) {
    uni.reLaunch({ url: '/pages/login/index' })
    return false
  }
  return true
}

const loadList = async (
  targetStatus: RecordStatus | 'ALL' = selectedStatus.value
) => {
  if (!ensureLogin()) {
    return
  }

  listLoadFailed.value = false

  try {
    await recordStore.fetchList(targetStatus)
    appliedStatus.value = targetStatus
  } catch {
    listLoadFailed.value = true
    uni.showToast({ title: '网络有点慢，请稍后重试', icon: 'none' })
  }
}

const onStatusChange = (value: RecordStatus | 'ALL') => {
  if (selectedStatus.value === value) {
    return
  }
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

  if (item.status === RecordStatus.SEALED) {
    const remainingDays = calculateRemainingDays(item.unlockAt)
    uni.showToast({
      title:
        remainingDays > 0
          ? `距离解封还有 ${remainingDays} 天`
          : '已到解封时间，请稍后查看',
      icon: 'none',
    })
    return
  }

  uni.navigateTo({
    url: `/pages/record-detail/index?id=${item.id}&source=archive`,
  })
}

const goBack = () => uni.navigateBack({ delta: 1 })

const statusBadgeText = (status: RecordStatus) => {
  if (status === RecordStatus.DRAFT) return '草稿'
  if (status === RecordStatus.SEALED) return '封存中'
  return '已解锁'
}

const statusBadgeClass = (status: RecordStatus) => {
  if (status === RecordStatus.DRAFT) return 'badge--draft'
  if (status === RecordStatus.SEALED) return 'badge--sealed'
  return 'badge--unlocked'
}

const iconClass = (status: RecordStatus) => {
  if (status === RecordStatus.DRAFT) return 'icon-bubble--draft'
  if (status === RecordStatus.SEALED) return 'icon-bubble--sealed'
  return 'icon-bubble--unlocked'
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

// 视觉母版使用：正文预览长度作为近似字数（真实字数字段由后续接口下发后替换）
const approximateWordCount = (preview: string) =>
  preview ? preview.replace(/\s/g, '').length : 0

const metaLine = (item: RecordListItemVO) => {
  const dateText = formatDayText(item.createdAt)
  if (item.status === RecordStatus.SEALED && item.unlockAt) {
    const year = getYear(item.unlockAt)
    if (year) {
      return { left: dateText, right: `预计 ${year} 年解锁` }
    }
  }
  if (item.status === RecordStatus.DRAFT) {
    const count = approximateWordCount(item.contentPreview)
    return { left: dateText, right: count > 0 ? `${count} 字` : '待续写' }
  }
  const count = approximateWordCount(item.contentPreview)
  return {
    left: dateText,
    right: count > 0 ? `${count.toLocaleString()} 字` : '',
  }
}

onShow(loadList)
</script>

<template>
  <AppPageShell
    class="archive-page"
    title="我的档案"
    :top-bar-transparent="true"
    content-bottom="120rpx"
  >
    <template #top-left>
      <view class="nav-back" @tap="goBack">
        <text class="nav-back__icon">‹</text>
      </view>
    </template>

    <!-- 搜索区 -->
    <view class="search-wrap">
      <view class="search-bar">
        <view class="search-bar__icon">
          <view class="icon-search" />
        </view>
        <input
          class="search-bar__input"
          :value="keyword"
          placeholder="搜索信件标题或内容"
          placeholder-class="search-bar__placeholder"
          confirm-type="search"
          @input="onSearchInput"
        />
        <text
          v-if="keyword"
          class="search-bar__clear"
          @tap="clearKeyword"
        >
          清空
        </text>
      </view>
    </view>

    <!-- 状态筛选 -->
    <view class="filter-row">
      <view
        v-for="option in statusOptions"
        :key="option.value"
        class="filter-chip"
        :class="{ 'filter-chip--active': option.value === selectedStatus }"
        @tap="onStatusChange(option.value)"
      >
        {{ option.label }}
      </view>
    </view>

    <!-- 档案概览标题 -->
    <view class="overview-head">
      <text class="overview-head__title">档案概览</text>
      <text class="overview-head__count">{{ overviewCountText }}</text>
    </view>

    <!-- 网络异常提示 -->
    <view v-if="showStaleNotice" class="inline-error">
      网络有点慢，正在显示上次加载的列表
      <text class="inline-error__retry" @tap="loadList()">重试</text>
    </view>

    <!-- 加载态 -->
    <view
      v-if="recordStore.loading && filteredList.length === 0"
      class="state-text"
    >
      正在翻阅档案…
    </view>

    <!-- 加载失败 -->
    <view v-else-if="showLoadFailureState" class="state-wrap">
      <EmptyState text="网络有点慢，档案暂时没加载出来" />
      <view v-if="hasContextMismatch" class="state-hint">
        当前仍停留在「{{ appliedStatusLabel }}」筛选
      </view>
      <PrimaryButton text="重试加载" ghost @tap="loadList()" />
    </view>

    <!-- 空状态 -->
    <view v-else-if="showEmptyState" class="state-wrap">
      <EmptyState :text="emptyStateText" />
      <PrimaryButton
        v-if="keyword.trim()"
        text="清空搜索"
        ghost
        @tap="clearKeyword"
      />
      <PrimaryButton v-else text="刷新列表" ghost @tap="loadList()" />
    </view>

    <!-- 档案卡片列表 -->
    <view v-else class="list-wrap">
      <view
        v-for="item in filteredList"
        :key="item.id"
        class="archive-card"
        @tap="openRecord(item)"
      >
        <view class="archive-card__icon" :class="iconClass(item.status)">
          <view class="icon-inner" :class="`icon-inner--${item.status}`" />
        </view>

        <view class="archive-card__body">
          <view class="archive-card__header">
            <text class="archive-card__title">
              {{ item.title || '未命名草稿' }}
            </text>
            <view class="archive-card__badge" :class="statusBadgeClass(item.status)">
              {{ statusBadgeText(item.status) }}
            </view>
          </view>

          <view class="archive-card__preview">
            {{ item.contentPreview || '还没写下内容…' }}
          </view>

          <view class="archive-card__meta">
            <text class="archive-card__meta-date">{{ metaLine(item).left }}</text>
            <text v-if="metaLine(item).right" class="archive-card__meta-dot">·</text>
            <text v-if="metaLine(item).right" class="archive-card__meta-right">
              {{ metaLine(item).right }}
            </text>
          </view>
        </view>
      </view>
    </view>

    <!-- 页面底部装饰 -->
    <view v-if="!showLoadFailureState && !showEmptyState" class="tail-deco">
      <view class="tail-deco__line" />
      <view class="tail-deco__dots">
        <view class="tail-deco__dot" />
        <view class="tail-deco__dot" />
        <view class="tail-deco__dot" />
      </view>
      <text class="tail-deco__text">时间在此处静静回溯</text>
    </view>
  </AppPageShell>
</template>

<style scoped>
.archive-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at 20% 0%, rgba(255, 255, 255, 0.9) 0%, rgba(255, 255, 255, 0) 40%),
    linear-gradient(180deg, #f4f7f9 0%, #eef2f5 100%);
}

/* ---------- 顶部返回按钮 ---------- */
.nav-back {
  width: 72rpx;
  height: 72rpx;
  display: flex;
  align-items: center;
  justify-content: flex-start;
}

.nav-back__icon {
  font-size: 48rpx;
  line-height: 1;
  color: var(--fb-color-primary);
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
  font-weight: 300;
  margin-top: -4rpx;
}

/* ---------- 搜索区 ---------- */
.search-wrap {
  margin-top: 24rpx;
}

.search-bar {
  width: 100%;
  height: 104rpx;
  border-radius: 999rpx;
  background: #ffffff;
  padding: 0 32rpx;
  display: flex;
  align-items: center;
  gap: 18rpx;
  box-shadow: 0 12rpx 30rpx rgba(82, 104, 120, 0.07);
  border: 1rpx solid rgba(222, 228, 233, 0.8);
}

.search-bar__icon {
  width: 36rpx;
  height: 36rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.icon-search {
  width: 32rpx;
  height: 32rpx;
  background-repeat: no-repeat;
  background-position: center;
  background-size: contain;
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%239aa6ad' stroke-width='1.8' stroke-linecap='round' stroke-linejoin='round'><circle cx='11' cy='11' r='7'/><line x1='21' y1='21' x2='16.65' y2='16.65'/></svg>");
}

.search-bar__input {
  flex: 1;
  height: 104rpx;
  font-size: 28rpx;
  color: #1a1a1a;
  font-style: italic;
  letter-spacing: 1rpx;
}

.search-bar__placeholder {
  color: #a9b2b7;
  font-size: 28rpx;
  font-style: italic;
}

.search-bar__clear {
  color: #8b969c;
  font-size: 24rpx;
}

/* ---------- 状态筛选 ---------- */
.filter-row {
  margin-top: 40rpx;
  display: flex;
  gap: 16rpx;
}

.filter-chip {
  flex: 1;
  min-width: 0;
  height: 72rpx;
  border-radius: 16rpx;
  background: #eaeef1;
  color: #6d7880;
  font-size: 26rpx;
  letter-spacing: 1rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.filter-chip--active {
  background: #2f4a5a;
  color: #ffffff;
  box-shadow: 0 10rpx 24rpx rgba(47, 74, 90, 0.22);
}

/* ---------- 档案概览标题 ---------- */
.overview-head {
  margin-top: 56rpx;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  padding: 0 4rpx;
}

.overview-head__title {
  font-size: 56rpx;
  line-height: 1.1;
  color: #1b2024;
  font-weight: 600;
  letter-spacing: 2rpx;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
}

.overview-head__count {
  font-size: 24rpx;
  color: #8c969c;
  letter-spacing: 1rpx;
  padding-bottom: 6rpx;
}

/* ---------- 提示条 ---------- */
.inline-error {
  margin-top: 20rpx;
  padding: 16rpx 24rpx;
  border-radius: 20rpx;
  background: rgba(255, 255, 255, 0.75);
  color: #7f8c93;
  font-size: 24rpx;
}

.inline-error__retry {
  margin-left: 10rpx;
  color: var(--fb-color-primary);
}

.state-text {
  margin-top: 80rpx;
  text-align: center;
  color: #8c969c;
  font-size: 26rpx;
}

.state-wrap {
  margin-top: 40rpx;
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.state-hint {
  margin: -6rpx 0 6rpx;
  text-align: center;
  color: #8c969c;
  font-size: 24rpx;
}

/* ---------- 档案卡片 ---------- */
.list-wrap {
  margin-top: 28rpx;
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.archive-card {
  position: relative;
  background: #ffffff;
  border-radius: 32rpx;
  padding: 32rpx 28rpx 28rpx;
  display: flex;
  gap: 24rpx;
  box-shadow: 0 14rpx 36rpx rgba(82, 104, 120, 0.07);
  border: 1rpx solid rgba(224, 230, 234, 0.7);
}

.archive-card__icon {
  width: 80rpx;
  height: 80rpx;
  border-radius: 999rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-top: 4rpx;
}

.icon-bubble--unlocked {
  background: #dfe8ee;
}

.icon-bubble--sealed {
  background: #e3e6e9;
}

.icon-bubble--draft {
  background: #efe9dd;
}

.icon-inner {
  width: 40rpx;
  height: 40rpx;
  background-repeat: no-repeat;
  background-position: center;
  background-size: contain;
}

.icon-inner--UNLOCKED {
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%234e6d7e' stroke-width='1.6' stroke-linecap='round' stroke-linejoin='round'><path d='M4 7.5L12 13l8-5.5'/><path d='M4 7.5v9A1.5 1.5 0 0 0 5.5 18h13a1.5 1.5 0 0 0 1.5-1.5v-9'/><path d='M4 7.5L12 3l8 4.5'/><path d='M9 12l-4 5'/><path d='M15 12l4 5'/></svg>");
}

.icon-inner--SEALED {
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23566069' stroke-width='1.6' stroke-linecap='round' stroke-linejoin='round'><rect x='5' y='11' width='14' height='10' rx='2'/><path d='M8 11V7.5a4 4 0 0 1 8 0V11'/></svg>");
}

.icon-inner--DRAFT {
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23917852' stroke-width='1.6' stroke-linecap='round' stroke-linejoin='round'><path d='M4 20h4l10-10-4-4L4 16v4z'/><path d='M14 6l4 4'/></svg>");
}

.archive-card__body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.archive-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16rpx;
}

.archive-card__title {
  flex: 1;
  font-size: 34rpx;
  line-height: 1.4;
  color: #1b2024;
  font-weight: 600;
  letter-spacing: 1rpx;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
  word-break: break-all;
}

.archive-card__badge {
  flex-shrink: 0;
  height: 40rpx;
  padding: 0 14rpx;
  border-radius: 10rpx;
  font-size: 20rpx;
  letter-spacing: 1rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  max-width: 80rpx;
  text-align: center;
  line-height: 1.2;
  word-break: keep-all;
}

.badge--unlocked {
  background: #e3ecf1;
  color: #4b6a7c;
}

.badge--sealed {
  background: #e6e7ea;
  color: #5f6770;
}

.badge--draft {
  background: #f3ecdb;
  color: #8a6f3d;
}

.archive-card__preview {
  font-size: 26rpx;
  line-height: 1.7;
  color: #8b969c;
  letter-spacing: 0.5rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
}

.archive-card__meta {
  display: flex;
  align-items: center;
  gap: 12rpx;
  font-size: 22rpx;
  color: #9ba5ab;
  letter-spacing: 0.5rpx;
}

.archive-card__meta-dot {
  color: #c7ccd0;
}

.archive-card__meta-date,
.archive-card__meta-right {
  color: #9ba5ab;
}

/* ---------- 尾部装饰 ---------- */
.tail-deco {
  margin-top: 80rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20rpx;
  padding-bottom: 40rpx;
}

.tail-deco__line {
  width: 2rpx;
  height: 64rpx;
  background: linear-gradient(
    180deg,
    rgba(155, 165, 171, 0) 0%,
    rgba(155, 165, 171, 0.35) 50%,
    rgba(155, 165, 171, 0) 100%
  );
}

.tail-deco__dots {
  display: flex;
  gap: 10rpx;
}

.tail-deco__dot {
  width: 8rpx;
  height: 8rpx;
  border-radius: 50%;
  background: #c7ccd0;
  opacity: 0.7;
}

.tail-deco__text {
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #a9b2b7;
  letter-spacing: 4rpx;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
}
</style>
