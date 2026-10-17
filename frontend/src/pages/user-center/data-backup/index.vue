<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import PreviewModeNotice from '../../../components/common/PreviewModeNotice.vue'
import AppTopSafeBar from '../../../components/common/AppTopSafeBar.vue'
import { dataOwnershipService } from '../../../services'
import type { DataOperationVO, DataOwnershipSummaryVO, SealedContentPolicy } from '../../../types'
import { formatDateTime, hasAuthenticatedSession } from '../../../utils'

const summary = ref<DataOwnershipSummaryVO | null>(null)
const operation = ref<DataOperationVO | null>(null)
const loading = ref(false)
const policy = ref<SealedContentPolicy>('RESPECT_SEAL')
const clearIntent = ref<DataOperationVO | null>(null)
const confirmationInput = ref('')
const activePanel = ref<'export' | 'clear' | null>(null)
const preparingClear = ref(false)
const custodyCount = computed(() => (summary.value?.recordCounts.SEALED ?? 0) + (summary.value?.recordCounts.UNLOCKED ?? 0))
const inTransitCount = computed(() => summary.value?.recordCounts.SEALED ?? 0)
const exportValidityText = computed(() => {
  if (operation.value?.operationType !== 'EXPORT') return '导出文件默认保留 24 小时'
  if (operation.value.status === 'EXPIRED') return '导出文件已过期，可重新生成'
  return operation.value.artifactExpiresAt
    ? `导出文件有效期至 ${formatDateTime(operation.value.artifactExpiresAt)}`
    : '导出文件默认保留 24 小时'
})

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

const openPanel = (panel: 'export' | 'clear') => {
  if (loading.value || preparingClear.value || !summary.value) return
  activePanel.value = panel
}

const closePanel = () => {
  if (loading.value || preparingClear.value) return
  activePanel.value = null
}

const requestClearConfirmation = async () => {
  if (loading.value || preparingClear.value) return
  preparingClear.value = true
  try { await prepareClearAll() }
  finally { preparingClear.value = false }
}

const confirmClearAndClose = async () => {
  if (loading.value || preparingClear.value || !clearIntent.value) return
  await confirmClearAll()
  if (!clearIntent.value) activePanel.value = null
}

const openBuildInfo = () => uni.navigateTo({ url: '/pages/user-center/about/index' })

onShow(loadSummary)
</script>

<template>
  <view class="page">
    <PreviewModeNotice />
    <view class="paper-glow" aria-hidden="true" />
    <AppTopSafeBar :transparent="true" padding-x="30rpx" side-width="128rpx" :center-title="true">
      <template #left><view class="back" role="button" aria-label="返回个人中心" @tap="goBack"><view class="back-chevron" aria-hidden="true" /><text>返回</text></view></template>
      <template #title><text class="nav-title">数据与所有权</text></template>
    </AppTopSafeBar>
    <scroll-view class="scroll" scroll-y enhanced :show-scrollbar="false">
      <view class="ownership-content">
        <view class="custody" aria-live="polite">
          <text v-if="summary">这里替你保管着 {{ custodyCount }} 封信，还有 {{ inTransitCount }} 封在路上。</text>
          <text v-else>{{ loading ? '正在取回你的保管信息…' : '保管信息暂时没有取回来' }}</text>
          <button v-if="!summary && !loading" class="summary-retry" @tap="loadSummary">重新加载</button>
        </view>
        <view class="voucher">
          <view class="voucher-redline" aria-hidden="true" /><view class="voucher-fold" aria-hidden="true" />
          <view class="voucher-head">
            <view class="voucher-copy"><text class="voucher-title">托管凭证</text><text class="voucher-sub">你的数据只属于你</text></view>
            <view class="custody-seal" aria-hidden="true">守</view>
          </view>
          <button class="export-entry" :disabled="loading || !summary" hover-class="export-entry--pressed" @tap="openPanel('export')">导出离线文件</button>
          <button class="clear-entry" :disabled="loading || preparingClear || !summary" @tap="openPanel('clear')">清理全部数据</button>
          <text class="voucher-note">{{ exportValidityText }}</text>
        </view>
        <view v-if="operation" class="operation-card" :class="{ 'operation-card--warning': operation.retryable }" aria-live="polite">
          <text class="operation-title">{{ operationText }}</text>
          <text class="operation-meta">{{ operation.processedItems }} / {{ operation.totalItems }} 已处理<text v-if="operation.failedItems"> · {{ operation.failedItems }} 项待重试</text></text>
          <text v-if="operation.failureCode" class="operation-code">原因：{{ operation.failureCode }}</text>
          <button v-if="operation.retryable" class="secondary-button operation-retry" :disabled="loading || preparingClear" @tap="retryOperation">重试原操作</button>
          <button v-if="operation.downloadable" class="secondary-button download-export" :disabled="loading" @tap="downloadExport">保存导出包</button>
          <button v-if="clearIntent" class="secondary-button resume-clear" :disabled="loading || preparingClear" @tap="openPanel('clear')">继续确认清理</button>
          <button class="operation-refresh" :disabled="loading || preparingClear" @tap="loadSummary">刷新状态</button>
        </view>
        <button class="build-row" hover-class="build-row--pressed" @tap="openBuildInfo"><text>构建信息</text><view class="build-chevron" aria-hidden="true" /></button>
        <text class="motto">时光回序 · 只保管，不拥有</text>
      </view>
    </scroll-view>
    <!-- 独立遮罩不包裹输入，避免原生确认输入的触摸事件触发关闭。 -->
    <view v-if="activePanel" class="action-layer">
      <view class="action-mask" @tap="closePanel" />
      <view class="action-panel" role="dialog" aria-modal="true" :aria-label="activePanel === 'export' ? '导出离线文件' : '清理全部数据'">
        <view class="panel-handle" aria-hidden="true" />
        <button class="panel-close" :disabled="loading || preparingClear" aria-label="关闭" @tap="closePanel">×</button>
        <text class="panel-kicker">{{ activePanel === 'export' ? '取回副本' : '清理确认' }}</text>
        <text class="panel-title">{{ activePanel === 'export' ? '导出离线文件' : '清理全部数据' }}</text>
        <scroll-view class="panel-scroll" scroll-y :show-scrollbar="false">
          <view class="panel-content">
            <view class="summary-card">
              <view class="summary-head"><text>全部记录 {{ totalRecords }}</text><text>媒体估算 {{ mediaSize }}</text></view>
              <view class="state-list"><text>未完成 {{ summary?.recordCounts.DRAFT ?? 0 }}</text><text>已保存 {{ summary?.recordCounts.SAVED ?? 0 }}</text><text>封存中 {{ summary?.recordCounts.SEALED ?? 0 }}</text><text>已抵达 {{ summary?.recordCounts.UNLOCKED ?? 0 }}</text></view>
            </view>
            <template v-if="activePanel === 'export'">
              <text class="panel-description">选择如何处理仍在封存中的内容。导出是可离线阅读的副本，不是云备份。</text>
              <button class="option" :class="{ selected: policy === 'RESPECT_SEAL' }" :disabled="loading" @tap="policy = 'RESPECT_SEAL'"><view class="option-copy"><text class="option-title">尊重封存</text><text class="option-description">默认选择。封存中的正文、位置、媒体与 Agent 内容暂不写入副本。</text></view><view class="radio" /></button>
              <button class="option" :class="{ selected: policy === 'FULL_CONTENT' }" :disabled="loading" @tap="policy = 'FULL_CONTENT'"><view class="option-copy"><text class="option-title">完整取回</text><text class="option-description">完整导出你拥有的数据，但不会改变产品内的封存与解锁状态。</text></view><view class="radio" /></button>
              <button class="primary-button" :disabled="loading" @tap="startExport">{{ loading ? '处理中…' : '生成离线副本' }}</button>
              <view v-if="operation?.operationType === 'EXPORT'" class="panel-operation">
                <text class="panel-operation-text">{{ operationText }}</text>
                <button v-if="operation.downloadable" class="secondary-button download-export" :disabled="loading" @tap="downloadExport">保存导出包</button>
              </view>
              <text class="fine-print">{{ exportValidityText }}</text>
            </template>
            <template v-else>
              <text class="danger-copy">将清理未完成、已保存、封存中和已抵达记录，以及它们关联的位置、媒体、回信与 Agent 数据。清理不可恢复，建议先导出。</text>
              <button v-if="!clearIntent" class="danger-button prepare-clear" :disabled="loading || preparingClear || totalRecords === 0" @tap="requestClearConfirmation">{{ preparingClear ? '正在准备…' : totalRecords ? '准备清除 ' + totalRecords + ' 条记录' : '当前没有可清理的记录' }}</button>
              <view v-if="clearIntent" class="confirm-box">
                <text class="confirm-label">请输入下面的完整确认短语</text><text class="phrase">{{ clearIntent.confirmationText }}</text>
                <input v-model="confirmationInput" class="confirm-input" :disabled="loading" :cursor-spacing="32" :adjust-position="true" placeholder="输入确认短语" />
                <button class="danger-button solid" :disabled="loading" @tap="confirmClearAndClose">{{ loading ? '正在确认…' : '确认且不可恢复' }}</button>
              </view>
            </template>
            <view class="panel-bottom-space" />
          </view>
        </scroll-view>
      </view>
    </view>
  </view>
</template>

<style scoped>
.page { position: relative; height: 100vh; display: flex; flex-direction: column; overflow: hidden; background: linear-gradient(170deg, #faf7f2 0%, #f5f0e8 55%, #f0ebe0 100%); color: #302e29; font-family: 'Noto Serif SC', 'Songti SC', 'STSong', 'SimSun', Georgia, serif; }
.paper-glow { position: absolute; inset: 0; pointer-events: none; background: radial-gradient(120% 86% at 20% -4%, rgba(255,255,255,.34), transparent 58%), radial-gradient(80% 60% at 88% 8%, rgba(255,251,242,.16), transparent 62%), radial-gradient(150% 90% at 50% 112%, rgba(197,181,152,.16), transparent 62%), radial-gradient(140% 120% at 50% 50%, transparent 62%, rgba(160,142,112,.08) 100%); }
.nav-title { max-width: 100%; font-size: 29rpx; font-weight: 400; letter-spacing: 8rpx; text-indent: 8rpx; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.back { min-height: 72rpx; margin-left: 24rpx; display: flex; align-items: center; gap: 13rpx; font-size: 27rpx; font-weight: 300; letter-spacing: .1em; color: #6b6560; }
.back-chevron { width: 16rpx; height: 16rpx; flex-shrink: 0; border-left: 2rpx solid #6b6560; border-bottom: 2rpx solid #6b6560; transform: rotate(45deg); }
.scroll { position: relative; z-index: 1; flex: 1; height: 0; min-height: 0; width: 100%; }
.ownership-content { display: flex; flex-direction: column; min-height: 100%; padding: 0 54rpx; box-sizing: border-box; }
.custody { flex-shrink: 0; margin-top: 100rpx; text-align: center; font-size: 30rpx; font-weight: 300; line-height: 1.65; letter-spacing: .04em; color: #6b6560; overflow-wrap: anywhere; }
.summary-retry { margin: 14rpx auto 0; padding: 10rpx 20rpx; background: transparent; font-family: inherit; font-size: 24rpx; color: #6b6560; }
.voucher { position: relative; flex-shrink: 0; margin-top: 65rpx; padding: 58rpx 50rpx 58rpx 58rpx; box-sizing: border-box; border: 1rpx solid rgba(188,174,152,.28); border-radius: 2rpx; background: #fcf9f4; box-shadow: 0 2rpx 4rpx rgba(125,105,75,.1), 0 19rpx 50rpx rgba(125,105,75,.13); }
.voucher-redline { position: absolute; top: 0; bottom: 0; left: 0; width: 2rpx; background: linear-gradient(to bottom, rgba(181,53,42,0), rgba(181,53,42,.35) 12%, rgba(181,53,42,.35) 85%, rgba(181,53,42,0)); pointer-events: none; }
.voucher-fold { position: absolute; top: -1rpx; right: -1rpx; width: 35rpx; height: 35rpx; background: linear-gradient(to bottom left, #f4efe5 0%, #f4efe5 50%, #e3dbc9 50.5%, #e3dbc9 100%); box-shadow: -3rpx 3rpx 5rpx rgba(120,100,70,.16); pointer-events: none; }
.voucher-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 18rpx; }
.voucher-copy { flex: 1; min-width: 0; }
.voucher-title { display: block; font-size: 50rpx; font-weight: 400; line-height: 1.4; letter-spacing: .12em; }
.voucher-sub { display: block; margin-top: 19rpx; font-size: 26rpx; font-weight: 300; line-height: 1.65; letter-spacing: .05em; color: #6b6560; }
.custody-seal { flex: 0 0 108rpx; width: 108rpx; height: 108rpx; display: flex; align-items: center; justify-content: center; box-sizing: border-box; border: 1rpx solid rgba(181,53,42,.75); border-radius: 50%; font-size: 38rpx; font-weight: 400; color: #b5352a; }
.export-entry { display: flex; align-items: center; justify-content: center; min-height: 100rpx; margin: 50rpx 50rpx 0 42rpx; padding: 18rpx 20rpx; box-sizing: border-box; border: 1rpx solid rgba(107,101,96,.5); border-radius: 2rpx; background-color: transparent; background-image: linear-gradient(rgba(107,101,96,.55),rgba(107,101,96,.55)),linear-gradient(rgba(107,101,96,.55),rgba(107,101,96,.55)),linear-gradient(rgba(107,101,96,.55),rgba(107,101,96,.55)),linear-gradient(rgba(107,101,96,.55),rgba(107,101,96,.55)); background-repeat: no-repeat; background-size: 25rpx 1rpx,1rpx 25rpx,25rpx 1rpx,1rpx 25rpx; background-position: 13rpx 13rpx,13rpx 13rpx,calc(100% - 38rpx) 13rpx,calc(100% - 13rpx) 13rpx; font-family: inherit; font-size: 31rpx; font-weight: 400; line-height: 1.6; letter-spacing: .2em; text-indent: .2em; color: #302e29; }
.export-entry--pressed, .build-row--pressed { background-color: #f3eee4; }
.export-entry[disabled], .clear-entry[disabled] { opacity: .55; }
.clear-entry { display: block; min-height: 66rpx; margin: 31rpx auto 0; padding: 15rpx 0; background: transparent; font-family: inherit; font-size: 29rpx; font-weight: 300; line-height: 1.5; letter-spacing: .14em; color: #b5352a; }
.voucher-note { display: block; margin-top: 12rpx; font-size: 21rpx; font-weight: 300; line-height: 1.7; letter-spacing: .08em; color: #9e9890; text-align: center; overflow-wrap: anywhere; }
.build-row { display: flex; align-items: center; justify-content: space-between; flex-shrink: 0; width: 100%; min-height: 112rpx; margin: 58rpx 0 0; padding: 23rpx 42rpx; box-sizing: border-box; border: 1rpx solid rgba(188,174,152,.28); border-radius: 2rpx; background: rgba(252,249,244,.55); color: #302e29; font-family: inherit; font-size: 29rpx; font-weight: 300; line-height: 1.6; letter-spacing: .1em; text-align: left; }
.build-chevron { flex-shrink: 0; width: 12rpx; height: 12rpx; border-top: 2rpx solid #9e9890; border-right: 2rpx solid #9e9890; transform: rotate(45deg); }
.motto { display: block; flex-shrink: 0; margin-top: auto; padding: 80rpx 0 calc(76rpx + env(safe-area-inset-bottom)); text-align: center; color: #9e9890; font-size: 23rpx; font-weight: 300; line-height: 1.7; letter-spacing: .14em; }
.operation-card { margin-top: 32rpx; padding: 28rpx; border: 1rpx solid rgba(188,174,152,.28); background: #fcf9f4; }
.operation-card--warning { border-left: 2rpx solid #b5352a; }
.operation-title { display: block; font-size: 29rpx; line-height: 1.6; }
.operation-meta, .operation-code { display: block; margin-top: 10rpx; color: #6b6560; font-size: 23rpx; line-height: 1.7; overflow-wrap: anywhere; }
.operation-refresh { margin: 12rpx auto 0; padding: 12rpx 16rpx; background: transparent; font-family: inherit; font-size: 23rpx; color: #6b6560; }
.action-layer { position: fixed; inset: 0; z-index: 1100; display: flex; align-items: flex-end; }
.action-mask { position: absolute; inset: 0; background: rgba(96,89,79,.42); }
.action-panel { position: relative; z-index: 1; width: 100%; height: 78vh; display: flex; flex-direction: column; box-sizing: border-box; padding: 20rpx 46rpx calc(20rpx + env(safe-area-inset-bottom)); border-radius: 28rpx 28rpx 0 0; background: #f9f5ed; color: #302e29; }
.panel-handle { flex-shrink: 0; width: 70rpx; height: 8rpx; margin: 4rpx auto 0; border-radius: 4rpx; background: #c8c1b7; }
.panel-close { position: absolute; top: 24rpx; right: 22rpx; width: 72rpx; height: 72rpx; margin: 0; padding: 0; background: transparent; font-size: 44rpx; line-height: 68rpx; color: #6b6560; font-weight: 300; }
.panel-kicker { display: block; flex-shrink: 0; margin-top: 36rpx; text-align: center; font-size: 21rpx; letter-spacing: .25em; color: #9e9890; }
.panel-title { display: block; flex-shrink: 0; margin: 22rpx 0 26rpx; text-align: center; font-size: 37rpx; line-height: 1.5; letter-spacing: .1em; }
.panel-scroll { flex: 1; height: 0; min-height: 0; }
.panel-content { padding: 0 2rpx; }
.summary-card { padding: 22rpx 0; border-top: 1rpx solid #e3dacc; border-bottom: 1rpx solid #e3dacc; }
.summary-head { display: flex; justify-content: space-between; flex-wrap: wrap; gap: 10rpx; font-size: 24rpx; line-height: 1.6; color: #6b6560; }
.state-list { display: flex; justify-content: space-between; flex-wrap: wrap; gap: 12rpx; margin-top: 16rpx; font-size: 21rpx; line-height: 1.6; color: #8a847c; }
.panel-description, .danger-copy { display: block; margin-top: 26rpx; font-size: 25rpx; color: #6b6560; line-height: 1.8; }
.option { display: flex; align-items: center; justify-content: space-between; gap: 20rpx; padding: 26rpx 0; margin: 0; border: 0; border-bottom: 1rpx solid #e3dacc; border-radius: 0; background: transparent; font-family: inherit; text-align: left; }
.option-copy { flex: 1; min-width: 0; }
.option-title { display: block; font-size: 28rpx; line-height: 1.6; }
.option-description { display: block; margin-top: 8rpx; font-size: 24rpx; line-height: 1.8; color: #6b6560; }
.radio { flex: 0 0 28rpx; width: 28rpx; height: 28rpx; box-sizing: border-box; border: 1rpx solid #9e9890; border-radius: 50%; }
.option.selected .radio { border: 8rpx solid #6b6560; }
.primary-button, .secondary-button, .danger-button { min-height: 86rpx; margin: 26rpx 0 0; padding: 18rpx 24rpx; box-sizing: border-box; border: 1rpx solid #c8c2b8; border-radius: 2rpx; background: #fcfaf6; font-family: inherit; font-size: 27rpx; line-height: 1.8; color: #302e29; }
.danger-button { border-color: rgba(181,53,42,.45); color: #b5352a; background: transparent; }
.danger-button.solid { border-color: #b5352a; background: #b5352a; color: #fffaf3; }
.panel-operation { margin-top: 24rpx; }
.panel-operation-text { font-size: 26rpx; line-height: 1.7; }
.fine-print { display: block; margin-top: 20rpx; font-size: 21rpx; line-height: 1.7; color: #8a847c; overflow-wrap: anywhere; }
.confirm-box { margin-top: 26rpx; }
.confirm-label { display: block; font-size: 24rpx; line-height: 1.7; color: #6b6560; }
.phrase { display: block; margin-top: 18rpx; padding: 18rpx; background: #fcf9f4; font-size: 28rpx; line-height: 1.8; color: #b5352a; white-space: pre-wrap; overflow-wrap: anywhere; }
.confirm-input { height: 86rpx; margin-top: 20rpx; padding: 0 22rpx; box-sizing: border-box; border: 1rpx solid #c8c2b8; border-radius: 2rpx; background: #fcfaf6; font-size: 26rpx; color: #302e29; }
.panel-bottom-space { height: 32rpx; }
.export-entry::after, .clear-entry::after, .build-row::after, .summary-retry::after, .operation-refresh::after, .panel-close::after, .option::after, .primary-button::after, .secondary-button::after, .danger-button::after { border: none; }
@media (max-width: 375px) { .nav-title { font-size: 28rpx; letter-spacing: 5rpx; text-indent: 5rpx; } }
@media (max-width: 340px) { .nav-title { font-size: 26rpx; letter-spacing: 2rpx; text-indent: 2rpx; } .voucher-title { font-size: 45rpx; } }
</style>
