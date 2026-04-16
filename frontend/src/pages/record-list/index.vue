<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { computed, ref } from 'vue'
import AppTopBar from '../../components/common/AppTopBar.vue'
import FilterSegment from '../../components/common/FilterSegment.vue'
import PaperContainer from '../../components/common/PaperContainer.vue'
import SearchBar from '../../components/common/SearchBar.vue'
import { useRecordStore } from '../../stores'
import { RecordStatus } from '../../types'
import { formatDateTime, getToken, toUserMessage } from '../../utils'

const recordStore = useRecordStore()
const selectedStatus = ref<RecordStatus | 'ALL'>('ALL')
const keyword = ref('')

const statusOptions = [
  { label: '全部', value: 'ALL' },
  { label: '草稿', value: RecordStatus.DRAFT },
  { label: '已封存', value: RecordStatus.SEALED },
  { label: '已解锁', value: RecordStatus.UNLOCKED },
]

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

const ensureLogin = () => {
  if (!getToken()) {
    uni.reLaunch({ url: '/pages/login/index' })
    return false
  }
  return true
}

const loadList = async () => {
  if (!ensureLogin()) {
    return
  }
  try {
    await recordStore.fetchList(selectedStatus.value)
  } catch (error) {
    uni.showToast({ title: toUserMessage(error), icon: 'none' })
  }
}

const onStatusChange = (value: string) => {
  selectedStatus.value = value as RecordStatus | 'ALL'
  loadList()
}

const openDetail = (id: number) => {
  uni.navigateTo({ url: `/pages/record-detail/index?id=${id}` })
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

    <view class="summary">当前列表 {{ filteredList.length }} 条</view>

    <view v-if="recordStore.loading" class="state-text">载入中...</view>
    <view v-else-if="filteredList.length === 0" class="state-text">没有符合条件的记录</view>

    <view v-else class="list-wrap">
      <PaperContainer
        v-for="item in filteredList"
        :key="item.id"
        radius="lg"
        class="list-item"
        @tap="openDetail(item.id)"
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
