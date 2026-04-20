<script setup lang="ts">
import { onLoad } from '@dcloudio/uni-app'
import { computed, reactive, ref } from 'vue'
import ImmersiveEditorTopBar from './components/ImmersiveEditorTopBar.vue'
import { useRecordStore, useTagStore } from '../../stores'
import { RecordType } from '../../types'
import {
  formatDateTime,
  formatDayText,
  getToken,
  toLocalDateTime,
  toUserMessage,
  validateRecordContent,
} from '../../utils'

const recordStore = useRecordStore()
const tagStore = useTagStore()

type EditorSource = 'home' | 'archive' | 'timeline'

const loading = ref(false)
const recordId = ref<number | null>(null)
const source = ref<EditorSource>('home')
const closing = ref(false)
const initializing = ref(false)
const initFailed = ref(false)
const initErrorMessage = ref('')
const latestQuery = ref<Record<string, unknown>>({})

interface EditorSnapshot {
  title: string
  content: string
  recordType: RecordType
  coreQuestion: string
  unlockAtInput: string
  aiSummary: string
  aiPromptResults: string[]
  tagIds: number[]
}

const initialSnapshot = ref<EditorSnapshot | null>(null)

const form = reactive({
  volNo: 'Vol. 01',
  title: '',
  content: '',
  recordType: RecordType.FUTURE_LETTER,
  coreQuestion: '',
  unlockAtInput: '',
  aiSummary: '',
  aiPromptResults: [] as string[],
  tagIds: [] as number[],
})

const writingDateText = computed(() => formatDayText(Date.now()))
const writingMomentText = computed(() => formatDateTime(Date.now()))

const ensureLogin = () => {
  if (!getToken()) {
    uni.reLaunch({ url: '/pages/login/index' })
    return false
  }
  return true
}

const resolveSource = (value: unknown): EditorSource => {
  if (value === 'archive' || value === 'timeline' || value === 'home') {
    return value
  }
  return 'home'
}

const returnToSource = () => {
  if (source.value === 'home') {
    uni.switchTab({ url: '/pages/home/index' })
    return
  }

  if (source.value === 'timeline') {
    uni.switchTab({ url: '/pages/timeline/index' })
    return
  }

  uni.navigateBack({
    delta: 1,
    fail: () => {
      uni.navigateTo({ url: '/pages/record-list/index' })
    },
  })
}

const buildSnapshot = (): EditorSnapshot => {
  const sortedTagIds = [...form.tagIds].sort((a, b) => a - b)
  return {
    title: form.title,
    content: form.content,
    recordType: form.recordType,
    coreQuestion: form.coreQuestion,
    unlockAtInput: form.unlockAtInput,
    aiSummary: form.aiSummary,
    aiPromptResults: [...form.aiPromptResults],
    tagIds: sortedTagIds,
  }
}

const markSnapshot = () => {
  initialSnapshot.value = buildSnapshot()
}

const hasDirtyChanges = () => {
  if (!initialSnapshot.value) {
    return false
  }

  return JSON.stringify(buildSnapshot()) !== JSON.stringify(initialSnapshot.value)
}

const confirmDiscardUnsavedChanges = () => {
  const content = recordId.value
    ? '正文为空，当前修改无法保存。是否放弃本次修改并返回？'
    : '正文为空，当前内容无法保存草稿。是否放弃并返回？'

  return new Promise<boolean>((resolve) => {
    uni.showModal({
      title: '放弃修改？',
      content,
      confirmText: '放弃',
      cancelText: '继续编辑',
      success: (res) => resolve(Boolean(res.confirm)),
      fail: () => resolve(false),
    })
  })
}

const handleCloseWithAutoSave = async () => {
  if (loading.value || closing.value) {
    return
  }

  if (!hasDirtyChanges()) {
    returnToSource()
    return
  }

  if (!validateRecordContent(form.content)) {
    const shouldDiscard = await confirmDiscardUnsavedChanges()
    if (shouldDiscard) {
      returnToSource()
    }
    return
  }

  closing.value = true
  loading.value = true
  try {
    await persistDraft()
    markSnapshot()
    returnToSource()
  } catch (error) {
    uni.showToast({ title: toUserMessage(error), icon: 'none' })
  } finally {
    loading.value = false
    closing.value = false
  }
}

const fillByDetail = async (id: number) => {
  const detail = await recordStore.fetchDetail(id)
  if (!detail) {
    return
  }

  form.title = detail.title || ''
  form.content = detail.content || ''
  form.recordType = detail.recordType
  form.coreQuestion = detail.coreQuestion || ''
  form.aiSummary = detail.aiSummary || ''
  form.aiPromptResults = detail.aiPromptResults || []
  form.tagIds = detail.tags.map((tag) => Number(tag.id))
  form.unlockAtInput = detail.unlockAt ? formatDateTime(detail.unlockAt) : ''
}

const resolveRecordId = (value: unknown) => {
  if (typeof value !== 'string') {
    return null
  }

  const id = Number(value)
  if (Number.isNaN(id) || id <= 0) {
    return null
  }

  return id
}

const runInitialization = async (query: Record<string, unknown>) => {
  initializing.value = true
  initFailed.value = false
  initErrorMessage.value = ''

  try {
    await tagStore.fetchTags()

    const id = resolveRecordId(query.id)
    if (id) {
      recordId.value = id
      form.volNo = `Vol. ${String(id).padStart(2, '0')}`
      await fillByDetail(id)
    }

    markSnapshot()
  } catch (error) {
    initFailed.value = true
    initErrorMessage.value = toUserMessage(error)
  } finally {
    initializing.value = false
  }
}

const retryInitialization = async () => {
  await runInitialization(latestQuery.value)
}

const persistDraft = async () => {
  const unlockAt = toLocalDateTime(form.unlockAtInput)
  const payload = {
    title: form.title || undefined,
    content: form.content,
    recordType: form.recordType,
    coreQuestion: form.coreQuestion || undefined,
    aiSummary: form.aiSummary || null,
    aiPromptResults: form.aiPromptResults,
    tagIds: form.tagIds,
    unlockAt: unlockAt || null,
  }

  if (recordId.value) {
    return recordStore.updateDraft(recordId.value, payload)
  }

  const created = await recordStore.createDraft(payload)
  recordId.value = created.id
  form.volNo = `Vol. ${String(created.id).padStart(2, '0')}`
  return created
}

const saveDraft = async () => {
  if (loading.value) {
    return
  }

  if (!validateRecordContent(form.content)) {
    uni.showToast({ title: '请先写下正文内容', icon: 'none' })
    return
  }

  loading.value = true
  try {
    await persistDraft()
    markSnapshot()
    uni.showToast({ title: '草稿已保存', icon: 'success' })
  } catch (error) {
    uni.showToast({ title: toUserMessage(error), icon: 'none' })
  } finally {
    loading.value = false
  }
}

const sealRecord = async () => {
  if (loading.value) {
    return
  }

  if (!validateRecordContent(form.content)) {
    uni.showToast({ title: '请先写下正文内容', icon: 'none' })
    return
  }

  const unlockAt = toLocalDateTime(form.unlockAtInput)
  if (!unlockAt || new Date(unlockAt).getTime() <= Date.now()) {
    uni.showToast({ title: '请设置未来的解锁时间', icon: 'none' })
    return
  }

  loading.value = true
  try {
    const draft = await persistDraft()
    await recordStore.sealRecord(draft.id)
    uni.showToast({ title: '已封存这一刻', icon: 'success' })
    setTimeout(() => returnToSource(), 300)
  } catch (error) {
    uni.showToast({ title: toUserMessage(error), icon: 'none' })
  } finally {
    loading.value = false
  }
}

const onAuxTap = (name: 'MAP' | 'IMAGE' | 'VOICE') => {
  uni.showToast({ title: `${name} 功能将在后续版本开放`, icon: 'none' })
}

onLoad(async (query) => {
  if (!ensureLogin()) {
    return
  }

  source.value = resolveSource(typeof query?.source === 'string' ? query.source : undefined)
  latestQuery.value = query as Record<string, unknown>
  await runInitialization(latestQuery.value)
})
</script>

<template>
  <view class="page">
    <view class="page-bg" aria-hidden="true" />

    <ImmersiveEditorTopBar :vol-no="form.volNo" @close="handleCloseWithAutoSave" />

    <view class="page-body">
      <view v-if="initializing" class="state-paper">
        <text class="state-kicker">Preparing the archive page</text>
        <text class="state-title">正在初始化写作页...</text>
        <text class="state-desc">我们正在取回当前草稿与类型信息，稍候就能继续落笔。</text>
      </view>

      <view v-else-if="initFailed" class="state-paper">
        <text class="state-kicker">Initialization interrupted</text>
        <text class="state-title">写作页暂时没有打开</text>
        <text class="state-desc">{{ initErrorMessage || '初始化失败，请检查网络后重试' }}</text>
        <view class="retry-btn" @tap="retryInitialization">重试初始化</view>
      </view>

      <template v-else>
        <view class="paper">
          <view class="paper-inner">
            <view class="paper-head">
              <view class="head-left">
                <text class="captured-at">Captured at</text>
                <text class="date-title">{{ writingDateText }}</text>
                <text class="date-sub">{{ writingMomentText }}</text>
              </view>

              <view class="head-right">
                <view class="seal">
                  <text class="seal-text">私有档案·严禁翻阅</text>
                </view>
              </view>
            </view>

            <view class="title-wrap">
              <input
                v-model="form.title"
                class="title-input"
                placeholder="给这一页轻轻落一个题目"
                placeholder-class="title-placeholder"
              />
            </view>

            <view class="content-wrap">
              <textarea
                v-model="form.content"
                class="content-area"
                auto-height
                maxlength="5000"
                placeholder="写下此刻想被未来自己看到的内容..."
                placeholder-class="content-placeholder"
              />
            </view>

            <view class="unlock-row">
              <text class="unlock-label">Unlock after</text>
              <input
                v-model="form.unlockAtInput"
                class="unlock-input"
                placeholder="解锁时间，如 2026-12-31 20:00"
                placeholder-class="unlock-placeholder"
              />
            </view>

            <view class="aux-divider" aria-hidden="true" />
            <view class="aux-row">
              <view class="aux-item" @tap="onAuxTap('MAP')">
                <view class="aux-circle">
                  <view class="ic ic-map" />
                </view>
                <text class="aux-label">MAP</text>
              </view>

              <view class="aux-item" @tap="onAuxTap('IMAGE')">
                <view class="aux-circle">
                  <view class="ic ic-image" />
                </view>
                <text class="aux-label">IMAGE</text>
              </view>

              <view class="aux-item" @tap="onAuxTap('VOICE')">
                <view class="aux-circle">
                  <view class="ic ic-voice" />
                </view>
                <text class="aux-label">VOICE</text>
              </view>
            </view>
          </view>
        </view>

        <view class="action-area">
          <view class="seal-btn" :class="{ disabled: loading }" @tap="sealRecord">
            <text class="seal-btn-text">{{ loading ? '封存中...' : '封存这一刻' }}</text>
            <view class="seal-btn-rule" />
            <text class="seal-btn-arrow">›</text>
          </view>

          <view class="draft-btn" :class="{ disabled: loading }" @tap="saveDraft">
            {{ closing ? '自动保存中...' : loading ? '保存中...' : '保存草稿' }}
          </view>
        </view>
      </template>
    </view>
  </view>
</template>

<style scoped>
.page {
  position: relative;
  min-height: 100vh;
  background: #eef1f3;
  overflow: hidden;
}

.page-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(120% 70% at 50% 0%, rgba(255, 255, 255, 0.82) 0%, rgba(255, 255, 255, 0) 50%),
    radial-gradient(88% 48% at 50% 100%, rgba(209, 217, 222, 0.55) 0%, rgba(209, 217, 222, 0) 56%),
    linear-gradient(180deg, #eef1f3 0%, #edf0f2 100%);
}

.page-body {
  position: relative;
  z-index: 1;
  padding: 16rpx 40rpx calc(env(safe-area-inset-bottom) + 40rpx);
}

.paper,
.state-paper {
  position: relative;
  border-radius: 40rpx;
  background: #fcfdfd;
  box-shadow:
    0 2rpx 0 rgba(255, 255, 255, 0.9) inset,
    0 26rpx 56rpx rgba(60, 78, 92, 0.08),
    0 8rpx 18rpx rgba(60, 78, 92, 0.04);
  overflow: hidden;
}

.paper::before,
.state-paper::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.78) 0%, rgba(255, 255, 255, 0) 18%),
    radial-gradient(100% 52% at 100% 0%, rgba(232, 236, 238, 0.26) 0%, rgba(232, 236, 238, 0) 56%);
}

.paper-inner {
  position: relative;
  z-index: 1;
  padding: 58rpx 48rpx 46rpx;
}

.paper-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24rpx;
}

.head-left {
  flex: 1;
  min-width: 0;
  padding-top: 6rpx;
}

.captured-at,
.state-kicker,
.note-kicker,
.aux-label {
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', 'Times New Roman', serif;
}

.captured-at {
  display: block;
  font-size: 24rpx;
  line-height: 1;
  letter-spacing: 2rpx;
  color: #89949a;
  font-style: italic;
}

.date-title {
  display: block;
  margin-top: 20rpx;
  color: #1d2327;
  font-size: 54rpx;
  line-height: 1.2;
  letter-spacing: 2rpx;
  font-weight: 500;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
}

.date-sub {
  display: block;
  margin-top: 16rpx;
  color: #97a0a6;
  font-size: 22rpx;
  letter-spacing: 1rpx;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
}

.head-right {
  display: flex;
  align-items: flex-start;
  flex-shrink: 0;
}

.seal {
  min-height: 176rpx;
  padding: 16rpx 12rpx;
  border: 1rpx solid #d9c79a;
  border-radius: 6rpx;
  background: linear-gradient(180deg, #fbf4df 0%, #f4e8cd 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.seal-text {
  writing-mode: vertical-rl;
  -webkit-writing-mode: vertical-rl;
  color: #9f8a4c;
  font-size: 22rpx;
  letter-spacing: 6rpx;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
}

.title-wrap {
  margin-top: 58rpx;
  padding-bottom: 18rpx;
  border-bottom: 1rpx solid rgba(211, 218, 222, 0.75);
}

.title-input {
  width: 100%;
  min-height: 60rpx;
  color: #1d2327;
  font-size: 42rpx;
  line-height: 1.35;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
}

.content-wrap {
  margin-top: 34rpx;
  min-height: 540rpx;
}

.content-area {
  width: 100%;
  min-height: 540rpx;
  color: #263038;
  font-size: 32rpx;
  line-height: 1.95;
  letter-spacing: 1rpx;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', 'PingFang SC', serif;
}

.title-placeholder {
  color: #abb4b9;
}

.content-placeholder,
.unlock-placeholder {
  color: #96a1a8;
}

.unlock-row {
  display: flex;
  align-items: center;
  gap: 18rpx;
  margin-top: 36rpx;
  padding: 16rpx 0 12rpx;
  border-top: 1rpx solid rgba(215, 221, 225, 0.55);
}

.unlock-label {
  flex-shrink: 0;
  color: #8d989e;
  font-size: 20rpx;
  letter-spacing: 1rpx;
  font-style: italic;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
}

.unlock-input {
  flex: 1;
  min-height: 40rpx;
  color: #5d6971;
  font-size: 24rpx;
  line-height: 1.4;
}

.aux-divider {
  height: 1rpx;
  margin: 26rpx 8rpx 30rpx;
  background: linear-gradient(
    90deg,
    rgba(200, 208, 213, 0) 0%,
    rgba(200, 208, 213, 0.62) 20%,
    rgba(200, 208, 213, 0.62) 80%,
    rgba(200, 208, 213, 0) 100%
  );
}

.aux-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-around;
  padding: 0 16rpx;
}

.aux-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14rpx;
}

.aux-circle {
  width: 72rpx;
  height: 72rpx;
  border-radius: 999rpx;
  border: 1rpx solid #cfd6da;
  background: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.aux-label {
  color: #8a949a;
  font-size: 20rpx;
  letter-spacing: 4rpx;
}

.ic {
  width: 32rpx;
  height: 32rpx;
  background-repeat: no-repeat;
  background-position: center;
  background-size: contain;
}

.ic-map {
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%235a6870' stroke-width='1.4' stroke-linecap='round' stroke-linejoin='round'><path d='M12 21s-7-7.5-7-12a7 7 0 1 1 14 0c0 4.5-7 12-7 12z'/><circle cx='12' cy='9' r='2.4'/></svg>");
}

.ic-image {
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%235a6870' stroke-width='1.4' stroke-linecap='round' stroke-linejoin='round'><path d='M4 8.5h3.2l1.6-2.2h6.4l1.6 2.2H20a1 1 0 0 1 1 1v8.3a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V9.5a1 1 0 0 1 1-1z'/><circle cx='12' cy='13.6' r='3.4'/></svg>");
}

.ic-voice {
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%235a6870' stroke-width='1.4' stroke-linecap='round' stroke-linejoin='round'><rect x='9' y='3.5' width='6' height='11' rx='3'/><path d='M6 12a6 6 0 0 0 12 0'/><line x1='12' y1='18' x2='12' y2='21'/><line x1='9' y1='21' x2='15' y2='21'/></svg>");
}

.action-area {
  margin-top: 34rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.seal-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 360rpx;
  width: auto;
  max-width: 520rpx;
  height: 84rpx;
  padding: 0 42rpx;
  border-radius: 999rpx;
  background: #2e5062;
  box-shadow:
    0 12rpx 24rpx rgba(46, 80, 98, 0.22),
    0 2rpx 0 rgba(255, 255, 255, 0.1) inset;
}

.seal-btn-text {
  color: #ffffff;
  font-size: 28rpx;
  letter-spacing: 6rpx;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
}

.seal-btn-rule {
  width: 1rpx;
  height: 24rpx;
  margin: 0 22rpx;
  background: rgba(255, 255, 255, 0.35);
}

.seal-btn-arrow {
  color: #ffffff;
  font-size: 28rpx;
  line-height: 1;
  margin-top: -2rpx;
  font-family: 'Songti SC', 'STSong', 'Times New Roman', serif;
}

.draft-btn {
  margin-top: 18rpx;
  color: #79858d;
  font-size: 22rpx;
  letter-spacing: 2rpx;
  padding: 10rpx 18rpx;
}

.disabled {
  opacity: 0.66;
}

.state-paper {
  position: relative;
  z-index: 1;
  margin-top: 18rpx;
  padding: 72rpx 48rpx 64rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.state-title {
  margin-top: 18rpx;
  color: #1d2327;
  font-size: 40rpx;
  line-height: 1.35;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
}

.state-desc {
  margin-top: 18rpx;
  color: #78858d;
  font-size: 26rpx;
  line-height: 1.8;
}

.retry-btn {
  margin-top: 34rpx;
  min-width: 240rpx;
  padding: 18rpx 30rpx;
  border-radius: 999rpx;
  background: rgba(46, 80, 98, 0.08);
  color: #2e5062;
  font-size: 26rpx;
  letter-spacing: 2rpx;
}
</style>
