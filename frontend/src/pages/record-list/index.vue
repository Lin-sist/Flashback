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

const archiveCountDisplay = computed(() => {
  if (recordStore.loading && totalCount.value === 0) {
    return '··'
  }
  if (listLoadFailed.value && totalCount.value === 0) {
    return '--'
  }
  if (totalCount.value > 99) {
    return '99+'
  }
  return String(totalCount.value).padStart(2, '0')
})

const archiveLeadText = computed(() => {
  if (recordStore.loading && totalCount.value === 0) {
    return '新的目录正在整理，请稍候片刻'
  }

  if (listLoadFailed.value && totalCount.value === 0) {
    return '目录暂未同步完成，稍后再来翻阅'
  }

  if (keyword.value.trim()) {
    return `按“${keyword.value.trim()}”检索到 ${totalCount.value} 份记录`
  }

  if (selectedStatus.value === 'ALL') {
    return '把草稿、封存与已解锁的记忆，静静收在同一处'
  }

  return `当前展示 ${selectedStatusLabel.value} 目录`
})

const archiveSublineText = computed(() => {
  if (hasContextMismatch.value) {
    return `网络未完成切换，当前仍停留在「${appliedStatusLabel.value}」筛选`
  }

  if (keyword.value.trim()) {
    return '可按标题与正文片段继续缩小范围'
  }

  return overviewCountText.value
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
  if (status === RecordStatus.DRAFT) return '待整理'
  if (status === RecordStatus.SEALED) return '静置中'
  return '可翻阅'
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

const collectionText = (item: RecordListItemVO) => {
  if (item.tagNames?.length) {
    return item.tagNames.slice(0, 2).join(' / ')
  }
  if (item.status === RecordStatus.DRAFT) return '手稿夹'
  if (item.status === RecordStatus.SEALED) return '封存箱'
  return '已启封卷宗'
}

const accessHintText = (status: RecordStatus) => {
  if (status === RecordStatus.DRAFT) return '继续补写'
  if (status === RecordStatus.SEALED) return '暂不可翻阅'
  return '翻阅全文'
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
    padding-x="32rpx"
    content-bottom="120rpx"
    top-side-width="96rpx"
  >
    <template #background>
      <view class="archive-glow archive-glow--left" />
      <view class="archive-glow archive-glow--right" />
      <view class="archive-texture" />
    </template>

    <template #top-left>
      <view class="nav-back" @tap="goBack">
        <text class="nav-back__icon">‹</text>
      </view>
    </template>

    <view class="archive-hero">
      <text class="archive-hero__eyebrow">PRIVATE ARCHIVE</text>
      <view class="archive-hero__main">
        <text class="archive-hero__count">{{ archiveCountDisplay }}</text>
        <view class="archive-hero__copy">
          <text class="archive-hero__title">档案目录</text>
          <text class="archive-hero__desc">{{ archiveLeadText }}</text>
        </view>
      </view>
      <text class="archive-hero__subline">{{ archiveSublineText }}</text>
    </view>

    <view class="catalog-tools">
      <view class="catalog-tools__section">
        <text class="catalog-tools__label">检索档案</text>
        <view class="search-bar">
          <view class="search-bar__icon">
            <view class="icon-search" />
          </view>
          <input
            class="search-bar__input"
            :value="keyword"
            placeholder="搜索信件标题或正文片段"
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

      <view class="catalog-tools__divider" />

      <view class="catalog-tools__section">
        <view class="filter-head">
          <text class="catalog-tools__label">筛选目录</text>
          <text class="filter-head__value">{{ selectedStatusLabel }}</text>
        </view>
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
      </view>
    </view>

    <view class="section-head">
      <view class="section-head__copy">
        <text class="section-head__title">档案条目</text>
        <text class="section-head__caption">Archive Register</text>
      </view>
      <text class="section-head__meta">{{ overviewCountText }}</text>
    </view>

    <view v-if="showStaleNotice" class="inline-notice">
      <text class="inline-notice__text">
        网络稍慢，先为你摊开上次整理好的目录
      </text>
      <text class="inline-notice__retry" @tap="loadList()">重新整理</text>
    </view>

    <view
      v-if="recordStore.loading && filteredList.length === 0"
      class="state-card"
    >
      <view class="state-card__marker" />
      <text class="state-card__title">正在翻阅档案</text>
      <text class="state-card__desc">
        目录页正在轻轻展开，请稍候片刻
      </text>
    </view>

    <view v-else-if="showLoadFailureState" class="state-card state-card--center">
      <view class="state-card__marker" />
      <text class="state-card__title">目录暂时没有打开</text>
      <text v-if="hasContextMismatch" class="state-card__desc">
        当前仍停留在「{{ appliedStatusLabel }}」筛选
      </text>
      <EmptyState text="网络有点慢，档案暂时没加载出来" />
      <view class="state-card__action">
        <PrimaryButton text="重试加载" ghost @tap="loadList()" />
      </view>
    </view>

    <view v-else-if="showEmptyState" class="state-card state-card--center">
      <view class="state-card__marker" />
      <text class="state-card__title">这里还很安静</text>
      <EmptyState :text="emptyStateText" />
      <view class="state-card__action">
        <PrimaryButton
          v-if="keyword.trim()"
          text="清空搜索"
          ghost
          @tap="clearKeyword"
        />
        <PrimaryButton v-else text="刷新列表" ghost @tap="loadList()" />
      </view>
    </view>

    <view v-else class="list-wrap">
      <view
        v-for="item in filteredList"
        :key="item.id"
        class="archive-card"
        @tap="openRecord(item)"
      >
        <view class="archive-card__trace" :class="iconClass(item.status)" />

        <view class="archive-card__inner">
          <view class="archive-card__topline">
            <text class="archive-card__collection">
              {{ collectionText(item) }}
            </text>
            <view
              class="archive-card__badge"
              :class="statusBadgeClass(item.status)"
            >
              {{ statusBadgeText(item.status) }}
            </view>
          </view>

          <view class="archive-card__headline">
            <text class="archive-card__title">
              {{ item.title || '未命名草稿' }}
            </text>
            <view class="archive-card__icon" :class="iconClass(item.status)">
              <view class="icon-inner" :class="`icon-inner--${item.status}`" />
            </view>
          </view>

          <view class="archive-card__rule" />

          <view class="archive-card__preview">
            {{ item.contentPreview || '还没写下内容…' }}
          </view>

          <view class="archive-card__footer">
            <view class="archive-card__meta">
              <text class="archive-card__meta-date">{{ metaLine(item).left }}</text>
              <text
                v-if="metaLine(item).right"
                class="archive-card__meta-dot"
              >
                ·
              </text>
              <text
                v-if="metaLine(item).right"
                class="archive-card__meta-right"
              >
                {{ metaLine(item).right }}
              </text>
            </view>

            <text class="archive-card__hint">
              {{ accessHintText(item.status) }}
            </text>
          </view>
        </view>
      </view>
    </view>

    <view
      v-if="!showLoadFailureState && !showEmptyState"
      class="tail-deco"
    >
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
    radial-gradient(circle at 18% 0%, rgba(255, 255, 255, 0.92) 0%, rgba(255, 255, 255, 0) 40%),
    linear-gradient(180deg, #f7f5f1 0%, #eff1f3 52%, #eceff1 100%);
}

.archive-glow {
  position: absolute;
  border-radius: 50%;
  opacity: 0.72;
  pointer-events: none;
}

.archive-glow--left {
  top: 96rpx;
  left: -120rpx;
  width: 360rpx;
  height: 360rpx;
  background: radial-gradient(circle, rgba(231, 223, 208, 0.7) 0%, rgba(231, 223, 208, 0) 70%);
}

.archive-glow--right {
  top: 260rpx;
  right: -140rpx;
  width: 320rpx;
  height: 320rpx;
  background: radial-gradient(circle, rgba(216, 226, 232, 0.7) 0%, rgba(216, 226, 232, 0) 70%);
}

.archive-texture {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.16) 1rpx, transparent 1rpx),
    linear-gradient(90deg, rgba(255, 255, 255, 0.14) 1rpx, transparent 1rpx);
  background-size: 24rpx 24rpx;
  opacity: 0.18;
  pointer-events: none;
}

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

.archive-hero {
  padding: 18rpx 4rpx 0;
}

.archive-hero__eyebrow {
  display: block;
  font-size: 20rpx;
  letter-spacing: 6rpx;
  color: #9aa1a5;
}

.archive-hero__main {
  margin-top: 20rpx;
  display: flex;
  align-items: flex-end;
  gap: 20rpx;
}

.archive-hero__count {
  flex-shrink: 0;
  font-size: 108rpx;
  line-height: 0.92;
  color: #1d252a;
  font-weight: 600;
  letter-spacing: 2rpx;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
}

.archive-hero__copy {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
  padding-bottom: 8rpx;
}

.archive-hero__title {
  font-size: 58rpx;
  line-height: 1.04;
  color: #1b2024;
  font-weight: 600;
  letter-spacing: 2rpx;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
}

.archive-hero__desc {
  font-size: 25rpx;
  line-height: 1.8;
  color: #6b757b;
  letter-spacing: 0.5rpx;
}

.archive-hero__subline {
  display: block;
  margin-top: 24rpx;
  font-size: 23rpx;
  line-height: 1.8;
  color: #8c969c;
}

.catalog-tools {
  margin-top: 36rpx;
  padding: 28rpx 24rpx 24rpx;
  border-radius: 32rpx;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.82) 0%, rgba(255, 255, 255, 0.7) 100%);
  border: 1rpx solid rgba(224, 228, 231, 0.86);
  box-shadow: 0 20rpx 40rpx rgba(101, 112, 119, 0.06);
}

.catalog-tools__section {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
}

.catalog-tools__label {
  font-size: 22rpx;
  letter-spacing: 4rpx;
  color: #95a0a6;
}

.catalog-tools__divider {
  height: 1rpx;
  margin: 24rpx 4rpx;
  background: linear-gradient(
    90deg,
    rgba(201, 208, 212, 0) 0%,
    rgba(201, 208, 212, 0.9) 18%,
    rgba(201, 208, 212, 0.9) 82%,
    rgba(201, 208, 212, 0) 100%
  );
}

.search-bar {
  width: 100%;
  min-height: 104rpx;
  border-radius: 28rpx;
  background: rgba(251, 251, 250, 0.96);
  padding: 0 28rpx;
  display: flex;
  align-items: center;
  gap: 18rpx;
  border: 1rpx solid rgba(228, 232, 235, 0.9);
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
  color: #1f2529;
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

.filter-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
}

.filter-head__value {
  font-size: 22rpx;
  color: #727d84;
  letter-spacing: 2rpx;
}

.filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}

.filter-chip {
  flex: 1 1 calc(50% - 8rpx);
  min-width: 0;
  height: 76rpx;
  border-radius: 18rpx;
  background: rgba(238, 241, 243, 0.9);
  color: #6f7a82;
  font-size: 26rpx;
  letter-spacing: 1rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1rpx solid transparent;
}

.filter-chip--active {
  background: #f7f8f8;
  color: #22343f;
  border-color: rgba(47, 74, 90, 0.14);
  box-shadow: 0 10rpx 22rpx rgba(47, 74, 90, 0.08);
}

.section-head {
  margin-top: 52rpx;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24rpx;
  padding: 0 4rpx 0 2rpx;
}

.section-head__copy {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.section-head__title {
  font-size: 44rpx;
  line-height: 1.08;
  color: #1d252a;
  letter-spacing: 1rpx;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
}

.section-head__caption {
  font-size: 20rpx;
  color: #99a2a8;
  letter-spacing: 4rpx;
}

.section-head__meta {
  flex-shrink: 0;
  font-size: 23rpx;
  color: #8c969c;
  padding-bottom: 6rpx;
}

.inline-notice {
  margin-top: 20rpx;
  padding: 16rpx 24rpx;
  border-radius: 22rpx;
  background: rgba(255, 255, 255, 0.72);
  border: 1rpx solid rgba(225, 230, 233, 0.7);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.inline-notice__text {
  flex: 1;
  color: #7f8c93;
  font-size: 24rpx;
  line-height: 1.7;
}

.inline-notice__retry {
  color: var(--fb-color-primary);
  font-size: 24rpx;
}

.state-card {
  margin-top: 24rpx;
  padding: 40rpx 32rpx;
  border-radius: 32rpx;
  background: rgba(255, 255, 255, 0.82);
  border: 1rpx solid rgba(225, 230, 233, 0.82);
  box-shadow: 0 16rpx 36rpx rgba(82, 104, 120, 0.05);
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.state-card--center {
  align-items: center;
  text-align: center;
}

.state-card__marker {
  width: 64rpx;
  height: 6rpx;
  border-radius: 999rpx;
  background: rgba(59, 100, 122, 0.18);
}

.state-card__title {
  font-size: 38rpx;
  line-height: 1.2;
  color: #1f2529;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
}

.state-card__desc {
  font-size: 25rpx;
  line-height: 1.75;
  color: #869197;
}

.state-card__action {
  width: 100%;
  margin-top: 8rpx;
}

.state-card :deep(.empty-state) {
  padding: 8rpx 0 0;
}

.state-card :deep(.empty-text) {
  color: #98a2aa;
  line-height: 1.7;
}

.state-card :deep(.primary-btn) {
  height: 84rpx;
  border-radius: 24rpx;
  background: rgba(59, 100, 122, 0.08);
  color: #3b647a;
  font-size: 28rpx;
  letter-spacing: 1rpx;
}

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
  padding: 0;
  box-shadow: 0 14rpx 36rpx rgba(82, 104, 120, 0.07);
  border: 1rpx solid rgba(224, 230, 234, 0.7);
  overflow: hidden;
}

.archive-card__trace {
  height: 10rpx;
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

.archive-card__inner {
  padding: 28rpx 28rpx 26rpx;
  display: flex;
  flex-direction: column;
  gap: 18rpx;
}

.archive-card__topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.archive-card__collection {
  font-size: 21rpx;
  color: #9aa3a8;
  letter-spacing: 3rpx;
}

.archive-card__badge {
  flex-shrink: 0;
  min-width: 96rpx;
  height: 42rpx;
  padding: 0 16rpx;
  border-radius: 999rpx;
  font-size: 20rpx;
  letter-spacing: 1rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.archive-card__headline {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18rpx;
}

.archive-card__title {
  flex: 1;
  font-size: 36rpx;
  line-height: 1.45;
  color: #1b2024;
  font-weight: 600;
  letter-spacing: 1rpx;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
  word-break: break-all;
}

.archive-card__icon {
  width: 76rpx;
  height: 76rpx;
  border-radius: 999rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
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

.archive-card__rule {
  width: 100%;
  height: 1rpx;
  background: linear-gradient(
    90deg,
    rgba(220, 226, 230, 0.9) 0%,
    rgba(220, 226, 230, 0.4) 70%,
    rgba(220, 226, 230, 0) 100%
  );
}

.badge--unlocked {
  background: #e7eef2;
  color: #4b6a7c;
}

.badge--sealed {
  background: #eceef0;
  color: #5f6770;
}

.badge--draft {
  background: #f5eee0;
  color: #8a6f3d;
}

.archive-card__preview {
  font-size: 26rpx;
  line-height: 1.7;
  color: #7b878f;
  letter-spacing: 0.5rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  min-height: 88rpx;
}

.archive-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.archive-card__meta {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
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

.archive-card__hint {
  flex-shrink: 0;
  font-size: 22rpx;
  color: #79868e;
  letter-spacing: 1rpx;
}

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
