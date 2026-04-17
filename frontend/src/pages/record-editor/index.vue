<script setup lang="ts">
import { onLoad } from '@dcloudio/uni-app'
import { reactive, ref } from 'vue'
import AppTopBar from '../../components/common/AppTopBar.vue'
import PaperContainer from '../../components/common/PaperContainer.vue'
import PrimaryButton from '../../components/common/PrimaryButton.vue'
import { useRecordStore, useTagStore } from '../../stores'
import { RecordType } from '../../types'
import { formatDayText, formatDateTime, getToken, toLocalDateTime, toUserMessage, validateRecordContent } from '../../utils'

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

const onRecordTypeChange = (event: { detail: { value: number } }) => {
  const next = tagStore.recordTypeOptions[event.detail.value]
  if (next) {
    form.recordType = next.value
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
    const updated = await recordStore.updateDraft(recordId.value, payload)
    return updated
  }

  const created = await recordStore.createDraft(payload)
  recordId.value = created.id
  form.volNo = `Vol. ${String(created.id).padStart(2, '0')}`
  return created
}

const saveDraft = async () => {
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
    <AppTopBar :title="form.volNo" show-close transparent @close="handleCloseWithAutoSave" />

    <view v-if="initializing" class="state-wrap">
      <view class="state-text">正在初始化编辑页...</view>
    </view>

    <view v-else-if="initFailed" class="state-wrap">
      <view class="state-text">{{ initErrorMessage || '初始化失败，请检查网络后重试' }}</view>
      <view class="state-action">
        <PrimaryButton text="重试初始化" ghost @tap="retryInitialization" />
      </view>
    </view>

    <template v-else>

      <view class="header-copy">
        <view class="captured-at">Captured at</view>
        <view class="main-date">{{ formatDayText(Date.now()) }}</view>
      </view>

      <PaperContainer radius="xl" class="paper">
        <view class="paper-top">
          <picker :range="tagStore.recordTypeOptions" range-key="label" @change="onRecordTypeChange">
            <view class="record-type">{{ form.recordType }}</view>
          </picker>
          <view class="vertical-label">TIME FILE</view>
        </view>

        <input v-model="form.title" class="title-input" placeholder="给这一刻写个标题（可选）" />

        <textarea
          v-model="form.content"
          class="content-area"
          maxlength="5000"
          placeholder="写下此刻想被未来自己看到的内容..."
        />

        <input v-model="form.coreQuestion" class="sub-input" placeholder="这刻最想追问的问题（可选）" />
        <input v-model="form.unlockAtInput" class="sub-input" placeholder="解锁时间，如 2026-12-31 20:00" />
      </PaperContainer>

      <view class="aux-actions">
        <text class="aux-item" @tap="onAuxTap('MAP')">MAP</text>
        <text class="aux-item" @tap="onAuxTap('IMAGE')">IMAGE</text>
        <text class="aux-item" @tap="onAuxTap('VOICE')">VOICE</text>
      </view>

      <view class="actions">
        <PrimaryButton text="保存草稿" :loading="loading" ghost @tap="saveDraft" />
        <PrimaryButton text="封存这一刻" :loading="loading" @tap="sealRecord" />
      </view>
    </template>
  </view>
</template>

<style scoped>
.page {
  min-height: 100vh;
  padding: 24rpx 24rpx 50rpx;
  background: #f8fafb;
}

.header-copy {
  margin-top: 20rpx;
}

.state-wrap {
  margin-top: 30rpx;
}

.state-text {
  text-align: center;
  color: #7f8c93;
  font-size: 24rpx;
}

.state-action {
  margin-top: 16rpx;
}

.captured-at {
  color: #7f8c93;
  font-size: 24rpx;
  letter-spacing: 1rpx;
}

.main-date {
  margin-top: 8rpx;
  color: #1a1a1a;
  font-size: 56rpx;
  line-height: 1.3;
  font-weight: 500;
}

.paper {
  margin-top: 22rpx;
  min-height: 760rpx;
}

.paper-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.record-type {
  color: #3b647a;
  font-size: 24rpx;
  padding: 10rpx 18rpx;
  border-radius: 999rpx;
  background: #eef4f7;
}

.vertical-label {
  writing-mode: vertical-rl;
  letter-spacing: 2rpx;
  color: #b0b8bc;
  font-size: 20rpx;
}

.title-input {
  margin-top: 24rpx;
  width: 100%;
  font-size: 36rpx;
  color: #1a1a1a;
}

.content-area {
  margin-top: 18rpx;
  width: 100%;
  min-height: 460rpx;
  font-size: 32rpx;
  line-height: 1.8;
  color: #1a1a1a;
}

.sub-input {
  margin-top: 12rpx;
  width: 100%;
  font-size: 28rpx;
  color: #7f8c93;
}

.aux-actions {
  margin-top: 22rpx;
  display: flex;
  justify-content: center;
  gap: 36rpx;
}

.aux-item {
  color: #7f8c93;
  font-size: 24rpx;
  letter-spacing: 1rpx;
}

.actions {
  margin-top: 30rpx;
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}
</style>

