<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { computed, ref } from 'vue'
import AppTopBar from '../../components/common/AppTopBar.vue'
import EmptyState from '../../components/common/EmptyState.vue'
import FilterSegment from '../../components/common/FilterSegment.vue'
import PaperContainer from '../../components/common/PaperContainer.vue'
import PrimaryButton from '../../components/common/PrimaryButton.vue'
import SearchBar from '../../components/common/SearchBar.vue'
import { useRecordStore } from '../../stores'
import { RecordStatus, type RecordListItemVO } from '../../types'
import { calculateRemainingDays, formatDateTime, getToken } from '../../utils'

const recordStore = useRecordStore()
const selectedStatus = ref<RecordStatus | 'ALL'>('ALL')
const appliedStatus = ref<RecordStatus | 'ALL'>('ALL')
const keyword = ref('')
const listLoadFailed = ref(false)

const statusOptions = [
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

const hasContextMismatch = computed(() => selectedStatus.value !== appliedStatus.value)

const selectedStatusLabel = computed(() => statusLabelMap[selectedStatus.value])
const appliedStatusLabel = computed(() => statusLabelMap[appliedStatus.value])

const summaryText = computed(() => {
  if (!recordStore.loading && listLoadFailed.value && hasContextMismatch.value) {
    return `筛选“${selectedStatusLabel.value}”加载失败`
  }

  return `当前${appliedStatusLabel.value}列表 ${filteredList.value.length} 条`
})

const showLoadFailureState = computed(() => !recordStore.loading && listLoadFailed.value && (recordStore.list.length === 0 || hasContextMismatch.value))
const showEmptyState = computed(() => !recordStore.loading && !listLoadFailed.value && !hasContextMismatch.value && filteredList.value.length === 0)
const showStaleNotice = computed(() => !recordStore.loading && listLoadFailed.value && recordStore.list.length > 0 && !hasContextMismatch.value)

const emptyStateText = computed(() => {
  if (keyword.value.trim()) {
    return '没有找到匹配的记录'
  }

  if (selectedStatus.value !== 'ALL') {
    return '当前筛选条件下还没有记录'
  }

  return '还没有记录，去首页写下第一条记忆吧'
})

const ensureLogin = () => {
  if (!getToken()) {
    uni.reLaunch({ url: '/pages/login/index' })
    return false
  }
  return true
}

const loadList = async (targetStatus: RecordStatus | 'ALL' = selectedStatus.value) => {
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

const onStatusChange = (value: string) => {
  const nextStatus = value as RecordStatus | 'ALL'
  selectedStatus.value = nextStatus
  loadList(nextStatus)
}

const clearKeyword = () => {
  keyword.value = ''
}

const openRecord = (item: RecordListItemVO) => {
  if (item.status === RecordStatus.DRAFT) {
    uni.navigateTo({ url: `/pages/record-editor/index?id=${item.id}&source=archive` })
    return
  }

  if (item.status === RecordStatus.SEALED) {
    const remainingDays = calculateRemainingDays(item.unlockAt)
    uni.showToast({
      title: remainingDays > 0 ? `距离解封还有 ${remainingDays} 天` : '已到解封时间，请稍后查看',
      icon: 'none',
    })
    return
  }

  uni.navigateTo({ url: `/pages/record-detail/index?id=${item.id}&source=archive` })
}

const goBack = () => uni.navigateBack({ delta: 1 })

onShow(loadList)
</script>

<template>
  <view class="page">
    <AppTopBar title="我的档案" show-back @back="goBack" />

    <view class="panel-row">
      <SearchBar v-model="keyword" placeholder="搜索标题或正文片段" />
      <FilterSegment :model-value="selectedStatus" :options="statusOptions" @change="onStatusChange" />
    </view>

    <view class="summary">{{ summaryText }}</view>

    <view v-if="showStaleNotice" class="inline-error">
      网络有点慢，正在显示上次加载的列表
      <text class="inline-retry" @tap="loadList">重试</text>
    </view>

    <view v-if="recordStore.loading" class="state-text">正在加载档案...</view>
    <view v-else-if="showLoadFailureState" class="state-wrap">
      <EmptyState text="网络有点慢，档案暂时没加载出来" />
      <view v-if="hasContextMismatch" class="state-hint">当前展示仍属于“{{ appliedStatusLabel }}”筛选</view>
      <PrimaryButton text="重试加载" ghost @tap="loadList" />
    </view>
    <view v-else-if="showEmptyState" class="state-wrap">
      <EmptyState :text="emptyStateText" />
      <PrimaryButton v-if="keyword.trim()" text="清空搜索" ghost @tap="clearKeyword" />
      <PrimaryButton v-else text="刷新列表" ghost @tap="loadList" />
    </view>

    <view v-else class="list-wrap">
      <PaperContainer
        v-for="item in filteredList"
        :key="item.id"
        radius="lg"
        class="list-item"
        @tap="openRecord(item)"
      >
        <view class="item-top">
          <text class="item-title">{{ item.title || '未命名记录' }}</text>
          <text class="item-status">{{ item.status }}</text>
        </view>
        <view class="item-preview">{{ item.contentPreview }}</view>
        <view class="item-meta">{{ formatDateTime(item.createdAt) }}</view>
      </PaperContainer>
    </view>

    <view class="tail-decoration">Archive keeps your private timeline.</view>
  </view>
</template>

<style scoped>
.page {
  min-height: 100vh;
  padding: 20rpx var(--fb-space-page) 60rpx;
  background: var(--fb-color-bg);
}

.panel-row {
  margin-top: var(--fb-space-section);
  display: flex;
  flex-direction: column;
  gap: 14rpx;
}

.summary {
  margin-top: 18rpx;
  color: var(--fb-color-text-muted);
  font-size: var(--fb-font-body-sub);
}

.state-text {
  margin-top: 30rpx;
  color: var(--fb-color-text-muted);
  text-align: center;
  font-size: var(--fb-font-meta);
}

.state-wrap {
  margin-top: 20rpx;
}

.state-hint {
  margin: 0 0 12rpx;
  text-align: center;
  color: var(--fb-color-text-muted);
  font-size: var(--fb-font-meta);
}

.inline-error {
  margin-top: 12rpx;
  color: var(--fb-color-text-muted);
  font-size: var(--fb-font-meta);
}

.inline-retry {
  margin-left: 10rpx;
  color: var(--fb-color-primary);
}

.list-wrap {
  margin-top: 20rpx;
  display: flex;
  flex-direction: column;
  gap: var(--fb-space-list);
}

.list-item {
  box-shadow: var(--fb-shadow-soft);
}

.item-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.item-title {
  font-size: var(--fb-font-title-sub);
  color: var(--fb-color-text);
  font-weight: 600;
}

.item-status {
  color: var(--fb-color-primary);
  font-size: var(--fb-font-meta);
}

.item-preview {
  margin-top: 12rpx;
  color: var(--fb-color-text-muted);
  font-size: var(--fb-font-body-sub);
  line-height: 1.7;
}

.item-meta {
  margin-top: 14rpx;
  font-size: var(--fb-font-meta);
  color: var(--fb-color-text-muted);
}

.tail-decoration {
  margin-top: 26rpx;
  text-align: center;
  font-size: var(--fb-font-meta);
  color: #a4aeb5;
}
</style>
