<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { computed, ref } from 'vue'
import AppTopBar from '../../../components/common/AppTopBar.vue'
import EmptyState from '../../../components/common/EmptyState.vue'
import PaperContainer from '../../../components/common/PaperContainer.vue'
import PrimaryButton from '../../../components/common/PrimaryButton.vue'
import { useTagStore } from '../../../stores'
import { hasAuthenticatedSession } from '../../../utils'

const tagStore = useTagStore()
const loading = ref(true)
const loadFailed = ref(false)
const tagsReady = ref(false)

const moodTags = computed(() => tagStore.moodTags)
const topicTags = computed(() => tagStore.topicTags)
const totalTags = computed(() => tagStore.tags.length)
const hasTags = computed(() => totalTags.value > 0)
const showLoadingState = computed(() => loading.value && !tagsReady.value)
const showFailureState = computed(() => !loading.value && loadFailed.value && !tagsReady.value)
const showEmptyState = computed(() => !loading.value && !loadFailed.value && tagsReady.value && !hasTags.value)
const showTagState = computed(() => tagsReady.value && hasTags.value)
const showStaleNotice = computed(() => !loading.value && loadFailed.value && tagsReady.value)

const ensureLogin = () => {
  if (!hasAuthenticatedSession()) {
    uni.reLaunch({ url: '/pages/login/index' })
    return false
  }
  return true
}

const goBack = () => {
  uni.navigateBack({
    delta: 1,
    fail: () => {
      uni.switchTab({ url: '/pages/user-center/index' })
    },
  })
}

const loadTags = async () => {
  if (!ensureLogin()) {
    return
  }

  loading.value = true
  loadFailed.value = false

  try {
    await tagStore.fetchTags()
    tagsReady.value = true
  } catch {
    loadFailed.value = true
    uni.showToast({ title: '网络有点慢，请稍后重试', icon: 'none' })
  } finally {
    loading.value = false
  }
}

const onReservedAction = (action: string) => {
  uni.showToast({ title: `${action} 将在后续版本开放`, icon: 'none' })
}

onShow(() => {
  loadTags()
})
</script>

<template>
  <view class="page">
    <AppTopBar title="标签管理" show-back @back="goBack" />

    <view v-if="showLoadingState" class="state-wrap">
      <EmptyState text="正在加载标签数据..." />
    </view>

    <view v-else-if="showFailureState" class="state-wrap">
      <EmptyState text="标签暂时没加载出来" />
      <PrimaryButton text="重试加载" ghost @tap="loadTags" />
    </view>

    <template v-else>
      <PaperContainer radius="xl" class="section">
        <view class="section-title">当前标签总数 {{ totalTags }}</view>
        <view class="section-subtitle">标签数据来自当前账号真实标签接口，分为情绪与主题两类。</view>
      </PaperContainer>

      <view v-if="showStaleNotice" class="inline-error">
        网络有点慢，当前展示的是上次同步的标签
        <text class="inline-retry" @tap="loadTags">重试</text>
      </view>

      <PaperContainer v-if="showTagState" radius="xl" class="section">
        <view class="group-title">情绪标签</view>
        <view v-if="moodTags.length" class="tag-wrap">
          <view class="tag-chip mood" v-for="tag in moodTags" :key="tag.id">{{ tag.name }}</view>
        </view>
        <view v-else class="empty-tip">暂无情绪标签</view>

        <view class="divider"></view>

        <view class="group-title">主题标签</view>
        <view v-if="topicTags.length" class="tag-wrap">
          <view class="tag-chip topic" v-for="tag in topicTags" :key="tag.id">{{ tag.name }}</view>
        </view>
        <view v-else class="empty-tip">暂无主题标签</view>
      </PaperContainer>

      <PaperContainer v-else-if="showEmptyState" radius="xl" class="section">
        <EmptyState text="当前还没有标签" />
        <PrimaryButton text="刷新标签" ghost @tap="loadTags" />
      </PaperContainer>

      <PaperContainer radius="xl" class="section">
        <view class="group-title">后续管理能力（预留）</view>
        <view class="plan-item" @tap="onReservedAction('新增标签')">
          <view>
            <view class="plan-title">新增标签</view>
            <view class="plan-subtitle">后续支持在端内创建自定义标签</view>
          </view>
          <text class="arrow">›</text>
        </view>
        <view class="plan-item" @tap="onReservedAction('编辑与停用标签')">
          <view>
            <view class="plan-title">编辑与停用标签</view>
            <view class="plan-subtitle">后续支持重命名、停用、排序</view>
          </view>
          <text class="arrow">›</text>
        </view>
      </PaperContainer>

      <view class="footer-action">
        <PrimaryButton text="刷新标签" ghost :loading="loading" @tap="loadTags" />
      </view>
    </template>

  </view>
</template>

<style scoped>
.page {
  min-height: 100vh;
  padding: 20rpx 24rpx 40rpx;
  background: #f8fafb;
}

.section {
  margin-top: 18rpx;
}

.state-wrap {
  margin-top: 18rpx;
}

.inline-error {
  margin-top: 12rpx;
  color: #7f8c93;
  font-size: 24rpx;
}

.inline-retry {
  margin-left: 10rpx;
  color: #3b647a;
}

.section-title {
  font-size: 36rpx;
  color: #1a1a1a;
  font-weight: 600;
}

.section-subtitle {
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #7f8c93;
  line-height: 1.7;
}

.group-title {
  font-size: 28rpx;
  color: #7f8c93;
}

.tag-wrap {
  margin-top: 12rpx;
  display: flex;
  flex-wrap: wrap;
  gap: 10rpx;
}

.tag-chip {
  padding: 10rpx 18rpx;
  border-radius: 999rpx;
  font-size: 24rpx;
}

.tag-chip.mood {
  background: #fdf1eb;
  color: #a55a3d;
}

.tag-chip.topic {
  background: #edf3f6;
  color: #3b647a;
}

.empty-tip {
  margin-top: 10rpx;
  color: #7f8c93;
  font-size: 24rpx;
}

.divider {
  margin: 18rpx 0;
  height: 1rpx;
  background: #eef2f4;
}

.plan-item {
  min-height: 96rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1rpx solid #eef2f4;
}

.plan-item:last-child {
  border-bottom: none;
}

.plan-title {
  font-size: 32rpx;
  color: #1a1a1a;
}

.plan-subtitle {
  margin-top: 4rpx;
  font-size: 24rpx;
  color: #7f8c93;
}

.arrow {
  color: #7f8c93;
  font-size: 30rpx;
}

.footer-action {
  margin-top: 20rpx;
}
</style>

