<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import PreviewModeNotice from '../../../components/common/PreviewModeNotice.vue'
import { dataOwnershipService } from '../../../services'
import type { DataOperationVO, DataOwnershipSummaryVO, SealedContentPolicy } from '../../../types'
import { hasAuthenticatedSession } from '../../../utils'

const summary = ref<DataOwnershipSummaryVO | null>(null)
const operation = ref<DataOperationVO | null>(null)
const loading = ref(false)
const policy = ref<SealedContentPolicy>('RESPECT_SEAL')
const clearIntent = ref<DataOperationVO | null>(null)
const confirmationInput = ref('')

const totalRecords = computed(() => Object.values(summary.value?.recordCounts ?? {}).reduce((sum, count) => sum + count, 0))
const mediaSize = computed(() => {
  const bytes = summary.value?.mediaBytes ?? 0
  return bytes < 1024 * 1024 ? `${Math.ceil(bytes / 1024)} KB` : `${(bytes / 1024 / 1024).toFixed(1)} MB`
})
const operationText = computed(() => {
  if (!operation.value) return ''
  const labels: Record<string, string> = {
    PREPARED: '等待确认', PENDING: '等待处理', RUNNING: '正在处理', RETRY_REQUIRED: '仍有数据未完成清理',
    SUCCEEDED: operation.value.operationType === 'EXPORT' ? '导出包已生成' : '清理已完成', FAILED: '操作未完成', EXPIRED: '操作已过期',
  }
  return labels[operation.value.status] ?? operation.value.status
})

const goBack = () => uni.navigateBack({ delta: 1, fail: () => uni.switchTab({ url: '/pages/user-center/index' }) })
const notifyError = (error: unknown) => uni.showToast({ title: error instanceof Error ? error.message : '操作暂时没有完成', icon: 'none' })

const loadSummary = async () => {
  if (!hasAuthenticatedSession()) { uni.reLaunch({ url: '/pages/login/index' }); return }
  loading.value = true
  try {
    summary.value = await dataOwnershipService.summary()
    operation.value = summary.value.activeOperation ?? operation.value
  } catch (error) { notifyError(error) }
  finally { loading.value = false }
}

const startExport = async () => {
  if (loading.value) return
  loading.value = true
  try { operation.value = await dataOwnershipService.createExport(policy.value); await loadSummary() }
  catch (error) { notifyError(error) }
  finally { loading.value = false }
}

const downloadExport = async () => {
  if (!operation.value?.downloadable) return
  try {
    const tempFilePath = await dataOwnershipService.download(operation.value.id)
    uni.saveFile({
      tempFilePath,
      success: () => uni.showToast({ title: '导出包已保存', icon: 'success' }),
      fail: () => uni.showToast({ title: '文件未保存，请稍后重试', icon: 'none' }),
    })
  } catch (error) { notifyError(error) }
}

const prepareClearAll = async () => {
  if (totalRecords.value === 0) { uni.showToast({ title: '当前没有可清理的记录', icon: 'none' }); return }
  try {
    clearIntent.value = await dataOwnershipService.prepareDeletion('ALL_RECORDS')
    operation.value = clearIntent.value
    confirmationInput.value = ''
  } catch (error) { notifyError(error) }
}

const confirmClearAll = async () => {
  const intent = clearIntent.value
  if (!intent?.confirmationText) return
  if (confirmationInput.value.trim() !== intent.confirmationText) {
    uni.showToast({ title: '确认短语不匹配', icon: 'none' }); return
  }
  loading.value = true
  try {
    operation.value = await dataOwnershipService.confirmDeletion(intent.id, confirmationInput.value.trim())
    clearIntent.value = null; confirmationInput.value = ''; await loadSummary()
  } catch (error) { notifyError(error) }
  finally { loading.value = false }
}

const retryOperation = async () => {
  if (!operation.value?.retryable) return
  loading.value = true
  try { operation.value = await dataOwnershipService.retry(operation.value.id); await loadSummary() }
  catch (error) { notifyError(error) }
  finally { loading.value = false }
}

onShow(loadSummary)
</script>

<template>
  <view class="page">
    <PreviewModeNotice />
    <scroll-view class="scroll" scroll-y enhanced :show-scrollbar="false">
      <view class="nav">
        <view class="back" @tap="goBack">‹ 我的</view>
        <view class="title">数 据 与 所 有 权</view>
        <view class="nav-space" />
      </view>

      <view class="intro">
        <view class="eyebrow">YOUR DATA</view>
        <view class="intro-title">把自己留下的内容，拿回手中</view>
        <view class="intro-copy">这里提供可离线阅读的数据副本，也允许你清理记录。导出不是云备份，清理不可恢复。</view>
      </view>

      <view class="summary-card">
        <view class="metric"><text class="metric-value">{{ loading && !summary ? '—' : totalRecords }}</text><text class="metric-label">全部记录</text></view>
        <view class="metric"><text class="metric-value small">{{ loading && !summary ? '—' : mediaSize }}</text><text class="metric-label">媒体估算</text></view>
        <view class="state-list">
          <text>未完成 {{ summary?.recordCounts.DRAFT ?? 0 }}</text>
          <text>已保存 {{ summary?.recordCounts.SAVED ?? 0 }}</text>
          <text>封存中 {{ summary?.recordCounts.SEALED ?? 0 }}</text>
          <text>已抵达 {{ summary?.recordCounts.UNLOCKED ?? 0 }}</text>
        </view>
      </view>

      <view v-if="operation" class="operation-card" :class="{ warning: operation.retryable }">
        <view class="operation-title">{{ operationText }}</view>
        <view class="operation-meta">{{ operation.processedItems }} / {{ operation.totalItems }} 已处理<span v-if="operation.failedItems"> · {{ operation.failedItems }} 项待重试</span></view>
        <view v-if="operation.failureCode" class="operation-code">原因：{{ operation.failureCode }}</view>
        <button v-if="operation.retryable" class="secondary-button" @tap="retryOperation">重试原操作</button>
      </view>

      <view class="section">
        <view class="section-kicker">导 出 副 本</view>
        <view class="section-title">选择如何处理仍在封存中的内容</view>
        <view class="option" :class="{ selected: policy === 'RESPECT_SEAL' }" @tap="policy = 'RESPECT_SEAL'">
          <view><view class="option-title">尊重封存</view><view class="option-copy">默认选择。封存中的正文、位置、媒体与 Agent 内容暂不写入副本。</view></view><view class="radio" />
        </view>
        <view class="option" :class="{ selected: policy === 'FULL_CONTENT' }" @tap="policy = 'FULL_CONTENT'">
          <view><view class="option-title">完整取回</view><view class="option-copy">完整导出你拥有的数据，但不会改变产品内的封存与解锁状态。</view></view><view class="radio" />
        </view>
        <button class="primary-button" :disabled="loading" @tap="startExport">{{ loading ? '处理中' : '生成离线副本' }}</button>
        <button v-if="operation?.downloadable" class="secondary-button" @tap="downloadExport">保存导出包</button>
        <view class="fine-print">导出包默认保留 24 小时；包含 records、media、agent、manifest 与离线索引。</view>
      </view>

      <view class="section danger-section">
        <view class="section-kicker danger">危 险 操 作</view>
        <view class="section-title">清除全部记录</view>
        <view class="danger-copy">将清理未完成、已保存、封存中和已抵达记录，以及它们关联的位置、媒体、回信与 Agent 数据。建议先导出。</view>
        <button class="danger-button" :disabled="loading" @tap="prepareClearAll">准备清除 {{ totalRecords }} 条记录</button>
        <view v-if="clearIntent" class="confirm-box">
          <view class="confirm-label">请输入下面的完整确认短语</view>
          <view class="phrase">{{ clearIntent.confirmationText }}</view>
          <input v-model="confirmationInput" class="confirm-input" placeholder="输入确认短语" />
          <button class="danger-button solid" :disabled="loading" @tap="confirmClearAll">确认且不可恢复</button>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<style scoped>
.page{min-height:100vh;background:#f8f4ed;color:#302e29;font-family:'PingFang SC',sans-serif}.scroll{height:100vh;padding:0 44rpx 100rpx;box-sizing:border-box}.nav{padding-top:calc(env(safe-area-inset-top) + 42rpx);display:flex;align-items:center;margin-bottom:58rpx}.back,.nav-space{width:150rpx;color:#776f68;font-size:26rpx}.title{flex:1;text-align:center;font-family:'Songti SC',serif;font-size:29rpx;letter-spacing:.16em}.intro{padding:0 10rpx 38rpx}.eyebrow,.section-kicker{font-size:20rpx;letter-spacing:.2em;color:#9b7a64}.intro-title{font-family:'Songti SC',serif;font-size:43rpx;line-height:1.35;margin:16rpx 0}.intro-copy,.option-copy,.danger-copy,.fine-print{font-size:25rpx;line-height:1.75;color:#756f69}.summary-card,.operation-card,.section{background:rgba(255,255,255,.72);border:1rpx solid rgba(100,80,65,.11);border-radius:22rpx;padding:34rpx;margin-bottom:28rpx}.summary-card{display:flex;flex-wrap:wrap}.metric{width:50%;display:flex;flex-direction:column}.metric-value{font-family:Georgia,serif;font-size:56rpx}.metric-value.small{font-size:40rpx;margin-top:12rpx}.metric-label{font-size:22rpx;color:#8b837b;margin-top:6rpx}.state-list{width:100%;display:flex;justify-content:space-between;border-top:1rpx solid #e9e1d8;margin-top:28rpx;padding-top:25rpx;font-size:21rpx;color:#817970}.operation-card{border-left:6rpx solid #63816f}.operation-card.warning{border-left-color:#b5352a}.operation-title,.section-title{font-family:'Songti SC',serif;font-size:31rpx;margin-bottom:12rpx}.operation-meta,.operation-code{font-size:23rpx;color:#746c65;margin-top:8rpx}.section-kicker{margin-bottom:14rpx}.section-kicker.danger{color:#a34037}.option{display:flex;align-items:center;justify-content:space-between;padding:28rpx 0;border-top:1rpx solid #ebe3d9}.option-title{font-size:28rpx;margin-bottom:8rpx}.option-copy{padding-right:28rpx}.radio{width:28rpx;height:28rpx;border:2rpx solid #9e958c;border-radius:50%;flex:0 0 auto}.option.selected .radio{border:8rpx solid #5e7881;box-sizing:border-box}.primary-button,.secondary-button,.danger-button{margin-top:26rpx;border-radius:10rpx;font-size:27rpx;line-height:86rpx}.primary-button{background:#3e5d68;color:#fff}.secondary-button{background:transparent;color:#3e5d68;border:1rpx solid #8fa1a6}.danger-button{background:transparent;color:#a23d34;border:1rpx solid #c78a84}.danger-button.solid{background:#a23d34;color:#fff}.fine-print{font-size:21rpx;margin-top:20rpx}.danger-section{margin-top:44rpx}.confirm-box{margin-top:28rpx;padding-top:25rpx;border-top:1rpx solid #ead6d2}.confirm-label{font-size:23rpx;color:#776d66}.phrase{font-family:monospace;font-size:28rpx;letter-spacing:.08em;margin:18rpx 0;color:#8f332c}.confirm-input{height:82rpx;background:#fff;border:1rpx solid #ddcec8;border-radius:10rpx;padding:0 22rpx;font-size:25rpx}
</style>
