<script setup lang="ts">
import { onLoad } from '@dcloudio/uni-app'
import { computed, reactive, ref } from 'vue'
import { hasPreviewSession, showPreviewReadonlyToast } from '../../features/preview/preview-session'
import ImmersiveEditorTopBar from './components/ImmersiveEditorTopBar.vue'
import { useRecordStore, useTagStore } from '../../stores'
import { RecordType } from '../../types'
import {
  formatDateTime,
  getToken,
  hasAuthenticatedSession,
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

const wordCount = computed(() => form.content.replace(/\s/g, '').length)

const writingDateText = computed(() => {
  const nums = ['零','一','二','三','四','五','六','七','八','九']
  const seasons: Record<number, string> = {
    0:'严冬',1:'立春',2:'初春',3:'暮春',
    4:'初夏',5:'仲夏',6:'盛夏',7:'初秋',
    8:'暮秋',9:'深秋',10:'初冬',11:'严冬',
  }
  const d = new Date()
  const yearStr = String(d.getFullYear()).split('').map((c) => nums[+c]).join('')
  return yearStr + '年 · ' + seasons[d.getMonth()]
})

const ensureLogin = () => {
  if (!hasAuthenticatedSession()) {
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

  if (!getToken() && hasPreviewSession()) {
    showPreviewReadonlyToast()
    returnToSource()
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

  if (!getToken() && hasPreviewSession()) {
    showPreviewReadonlyToast()
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

  if (!getToken() && hasPreviewSession()) {
    showPreviewReadonlyToast()
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
    <!-- 宣纸底色光晕 -->
    <view class="page-bg" aria-hidden="true" />

    <!-- 顶部栏：Vol. N + 关闭 -->
    <ImmersiveEditorTopBar :vol-no="form.volNo" @close="handleCloseWithAutoSave" />

    <view class="page-body">
      <!-- 初始化中 -->
      <view v-if="initializing" class="state-paper">
        <text class="state-kicker">Preparing the archive page</text>
        <text class="state-title">正在初始化写作页...</text>
        <text class="state-desc">我们正在取回当前草稿与类型信息，稍候就能继续落笔。</text>
      </view>

      <!-- 初始化失败 -->
      <view v-else-if="initFailed" class="state-paper">
        <text class="state-kicker">Initialization interrupted</text>
        <text class="state-title">写作页暂时没有打开</text>
        <text class="state-desc">{{ initErrorMessage || '初始化失败，请检查网络后重试' }}</text>
        <view class="retry-btn" @tap="retryInitialization">重试初始化</view>
      </view>

      <!-- 正常编辑态 -->
      <template v-else>
        <!-- 信笺主体 -->
        <view class="letter-wrap">
          <view class="letter-body">
            <!-- 天头朱砂横线 -->
            <view class="letter-topline" aria-hidden="true" />

            <!-- 信头 -->
            <view class="letter-head">
              <view class="head-left">
                <text class="captured-label">Captured at</text>
                <text class="letter-date">{{ writingDateText }}</text>
              </view>
              <view class="archive-tag">
                <text class="archive-tag-text">私有档案·严禁翻阅</text>
              </view>
            </view>

            <!-- 正文区：左侧朱砂竖线 + 编辑区 -->
            <view class="letter-content">
              <view class="side-rule" aria-hidden="true" />
              <view class="editor-zone">
                <input
                  v-model="form.title"
                  class="title-input"
                  placeholder="拟定一个标题..."
                  placeholder-class="title-placeholder"
                />
                <!-- 解锁时间行（嵌入信纸内） -->
                <view class="unlock-row">
                  <text class="unlock-label">Unlock after</text>
                  <input
                    v-model="form.unlockAtInput"
                    class="unlock-input"
                    placeholder="解锁时间，如 2026-12-31 20:00"
                    placeholder-class="unlock-placeholder"
                  />
                </view>
                <textarea
                  v-model="form.content"
                  class="editor-field"
                  auto-height
                  maxlength="5000"
                  placeholder="在此刻的宁静中，留下你的记忆碎片..."
                  placeholder-class="editor-placeholder"
                />
              </view>
            </view>

            <!-- 附件栏 MAP / IMAGE / VOICE -->
            <view class="attach-bar">
              <view class="attach-item" @tap="onAuxTap('MAP')">
                <view class="attach-icon attach-icon--map" aria-hidden="true" />
                <text class="attach-label">MAP</text>
              </view>
              <view class="attach-sep" aria-hidden="true" />
              <view class="attach-item" @tap="onAuxTap('IMAGE')">
                <view class="attach-icon attach-icon--image" aria-hidden="true" />
                <text class="attach-label">IMAGE</text>
              </view>
              <view class="attach-sep" aria-hidden="true" />
              <view class="attach-item" @tap="onAuxTap('VOICE')">
                <view class="attach-icon attach-icon--voice" aria-hidden="true" />
                <text class="attach-label">VOICE</text>
              </view>
            </view>
          </view>
        </view>

        <!-- 底部操作区 -->
        <view class="bottom-area">
          <text class="word-count">{{ wordCount }} 字</text>

          <view class="seal-btn" :class="{ 'seal-btn--disabled': loading }" @tap="sealRecord">
            <view class="seal-btn-corner seal-btn-corner--tl" aria-hidden="true" />
            <view class="seal-btn-corner seal-btn-corner--br" aria-hidden="true" />
            <view class="btn-dot" aria-hidden="true" />
            <text class="seal-btn-text">{{ loading ? '封存中...' : '封存这一刻' }}</text>
          </view>

          <view class="seal-hint">
            <view class="hint-line" aria-hidden="true" />
            <text class="hint-text">封存后将锁定，到期方可开启</text>
            <view class="hint-line" aria-hidden="true" />
          </view>

          <view class="draft-btn" :class="{ 'draft-btn--disabled': loading }" @tap="saveDraft">
            <text class="draft-btn-text">{{ closing ? '自动保存中...' : loading ? '保存中...' : '保存草稿' }}</text>
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
  overflow: hidden;
  background: linear-gradient(160deg, #f5f0e8 0%, #ede8dc 45%, #e8e0d2 100%);
}

.page-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(ellipse 70% 55% at 20% 8%, rgba(255, 248, 235, 0.22) 0%, transparent 65%),
    radial-gradient(ellipse 55% 40% at 80% 20%, rgba(220, 208, 185, 0.10) 0%, transparent 60%),
    radial-gradient(ellipse 60% 50% at 50% 55%, rgba(255, 250, 240, 0.12) 0%, transparent 70%),
    radial-gradient(ellipse 45% 35% at 75% 80%, rgba(200, 188, 165, 0.08) 0%, transparent 60%);
}

.page-body {
  position: relative;
  z-index: 1;
  padding: 8rpx 56rpx calc(env(safe-area-inset-bottom) + 68rpx);
  display: flex;
  flex-direction: column;
}

/* ── 信笺容器 ── */
.letter-wrap {
  flex: 1;
  margin-top: 20rpx;
  position: relative;
}

.letter-body {
  position: relative;
  background: #fdfbf7;
  border: 1rpx solid rgba(180, 168, 148, 0.45);
  box-shadow:
    0 0 0 1rpx rgba(255, 253, 248, 0.8) inset,
    4rpx 12rpx 48rpx rgba(120, 100, 70, 0.12),
    -2rpx 4rpx 16rpx rgba(120, 100, 70, 0.06);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 右上折角 */
.letter-body::after {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  width: 32rpx;
  height: 32rpx;
  background: linear-gradient(225deg, #ede8dc 0%, #ede8dc 48%, #fdfbf7 50%);
  border-left: 1rpx solid rgba(180, 168, 148, 0.35);
  border-bottom: 1rpx solid rgba(180, 168, 148, 0.35);
  z-index: 4;
}

/* 天头朱砂横线 */
.letter-topline {
  height: 2rpx;
  background: linear-gradient(
    to right,
    transparent 0%,
    rgba(181, 53, 42, 0.22) 8%,
    rgba(181, 53, 42, 0.28) 50%,
    rgba(181, 53, 42, 0.22) 92%,
    transparent 100%
  );
  flex-shrink: 0;
}

/* 信头 */
.letter-head {
  padding: 36rpx 40rpx 28rpx 48rpx;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  flex-shrink: 0;
  border-bottom: 1rpx solid rgba(192, 182, 165, 0.3);
}

.head-left {
  flex: 1;
}

.captured-label {
  display: block;
  font-family: Georgia, 'Noto Serif SC', 'Songti SC', serif;
  font-size: 20rpx;
  font-style: italic;
  color: #9e9890;
  letter-spacing: 0.06em;
  margin-bottom: 10rpx;
  opacity: 0.8;
}

.letter-date {
  font-family: 'Noto Serif SC', 'Songti SC', Georgia, serif;
  font-size: 40rpx;
  font-weight: 300;
  color: #302e29;
  letter-spacing: 0.06em;
  line-height: 1.3;
}

/* 竖排档案标签 */
.archive-tag {
  margin-top: 4rpx;
  padding: 10rpx 8rpx;
  border: 1rpx solid rgba(192, 182, 165, 0.55);
  display: flex;
  align-items: center;
  justify-content: center;
}

.archive-tag-text {
  font-family: 'Noto Serif SC', 'Songti SC', Georgia, serif;
  font-size: 17rpx;
  font-weight: 300;
  color: #9e9890;
  writing-mode: vertical-rl;
  -webkit-writing-mode: vertical-rl;
  letter-spacing: 0.14em;
  line-height: 1;
  opacity: 0.85;
}

/* 正文区 */
.letter-content {
  flex: 1;
  position: relative;
  display: flex;
  min-height: 480rpx;
}

/* 左侧朱砂竖格线 */
.side-rule {
  width: 72rpx;
  flex-shrink: 0;
  position: relative;
}

.side-rule::after {
  content: '';
  position: absolute;
  right: 0;
  top: 32rpx;
  bottom: 32rpx;
  width: 2rpx;
  background: linear-gradient(
    to bottom,
    transparent,
    rgba(181, 53, 42, 0.2) 15%,
    rgba(181, 53, 42, 0.25) 50%,
    rgba(181, 53, 42, 0.2) 85%,
    transparent
  );
}

/* 编辑区 */
.editor-zone {
  flex: 1;
  padding: 24rpx 36rpx 24rpx 28rpx;
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

/* 标题输入框 */
.title-input {
  width: 100%;
  min-height: 48rpx;
  background: transparent;
  font-family: 'Noto Serif SC', 'Songti SC', Georgia, serif;
  font-size: 32rpx;
  font-weight: 500;
  color: #302e29;
  letter-spacing: 0.04em;
  margin-bottom: 8rpx;
}

:deep(.title-placeholder) {
  color: rgba(180, 170, 155, 0.7);
  font-family: 'Noto Serif SC', 'Songti SC', Georgia, serif;
  font-size: 32rpx;
  font-weight: 500;
}

/* 解锁时间行 */
.unlock-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding-bottom: 16rpx;
  border-bottom: 1rpx solid rgba(192, 182, 165, 0.25);
}

.unlock-label {
  flex-shrink: 0;
  font-family: Georgia, 'Noto Serif SC', 'Songti SC', serif;
  font-size: 20rpx;
  font-style: italic;
  color: #9e9890;
  letter-spacing: 0.04em;
}

.unlock-input {
  flex: 1;
  min-height: 40rpx;
  background: transparent;
  font-family: 'Noto Serif SC', 'Songti SC', Georgia, serif;
  font-size: 22rpx;
  color: #6b6560;
  letter-spacing: 0.03em;
}

:deep(.unlock-placeholder) {
  color: rgba(180, 170, 155, 0.7);
  font-size: 22rpx;
}

/* 正文 textarea */
.editor-field {
  width: 100%;
  min-height: 400rpx;
  background: transparent;
  font-family: 'Noto Serif SC', 'Songti SC', Georgia, serif;
  font-size: 28rpx;
  font-weight: 300;
  color: #6b6560;
  line-height: 2.0;
  letter-spacing: 0.04em;
}

:deep(.editor-placeholder) {
  color: rgba(180, 170, 155, 0.7);
  font-family: 'Noto Serif SC', 'Songti SC', Georgia, serif;
  font-size: 28rpx;
}

/* 附件栏 */
.attach-bar {
  flex-shrink: 0;
  border-top: 1rpx solid rgba(192, 182, 165, 0.28);
  padding: 24rpx 40rpx;
  display: flex;
  align-items: center;
}

.attach-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10rpx;
  padding: 6rpx 0;
}

.attach-icon {
  width: 40rpx;
  height: 40rpx;
  background-repeat: no-repeat;
  background-position: center;
  background-size: contain;
}

.attach-icon--map {
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%239e9890' stroke-width='1.3' stroke-linecap='round' stroke-linejoin='round'><path d='M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z'/><circle cx='12' cy='10' r='3'/></svg>");
}

.attach-icon--image {
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%239e9890' stroke-width='1.3' stroke-linecap='round' stroke-linejoin='round'><rect x='3' y='5' width='18' height='14' rx='1'/><circle cx='12' cy='12' r='3.2'/><circle cx='17.5' cy='8.5' r='0.9' fill='%239e9890' stroke='none'/></svg>");
}

.attach-icon--voice {
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%239e9890' stroke-width='1.3' stroke-linecap='round' stroke-linejoin='round'><path d='M12 2a3 3 0 013 3v7a3 3 0 01-6 0V5a3 3 0 013-3z'/><path d='M19 10v2a7 7 0 01-14 0v-2'/><line x1='12' y1='19' x2='12' y2='22'/><line x1='9' y1='22' x2='15' y2='22'/></svg>");
}

.attach-label {
  font-family: 'Noto Sans SC', 'PingFang SC', sans-serif;
  font-size: 17rpx;
  font-weight: 300;
  color: #9e9890;
  letter-spacing: 0.12em;
}

.attach-sep {
  width: 1rpx;
  height: 48rpx;
  background: rgba(192, 182, 165, 0.4);
}

/* ── 底部操作区 ── */
.bottom-area {
  padding: 36rpx 0 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0;
}

.word-count {
  font-family: 'Noto Sans SC', 'PingFang SC', sans-serif;
  font-size: 20rpx;
  font-weight: 300;
  color: #c8c2b8;
  letter-spacing: 0.08em;
  margin-bottom: 28rpx;
}

/* 封存按钮 */
.seal-btn {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 480rpx;
  height: 96rpx;
  font-family: 'Noto Serif SC', 'Songti SC', Georgia, serif;
  font-size: 30rpx;
  font-weight: 400;
  letter-spacing: 0.18em;
  color: #302e29;
  background: transparent;
  border: 1rpx solid #c8c2b8;
  border-radius: 4rpx;
  gap: 20rpx;
}

.seal-btn--disabled {
  opacity: 0.6;
}

.seal-btn-corner {
  position: absolute;
  width: 14rpx;
  height: 14rpx;
  border-color: #9e9890;
  border-style: solid;
}

.seal-btn-corner--tl {
  top: -2rpx;
  left: -2rpx;
  border-width: 2rpx 0 0 2rpx;
}

.seal-btn-corner--br {
  bottom: -2rpx;
  right: -2rpx;
  border-width: 0 2rpx 2rpx 0;
}

.btn-dot {
  width: 10rpx;
  height: 10rpx;
  border-radius: 50%;
  background: #b5352a;
  opacity: 0.72;
  flex-shrink: 0;
}

.seal-btn-text {
  font-family: 'Noto Serif SC', 'Songti SC', Georgia, serif;
  font-size: 30rpx;
  font-weight: 400;
  letter-spacing: 0.18em;
  color: #302e29;
}

/* 提示文字 */
.seal-hint {
  margin-top: 24rpx;
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.hint-line {
  width: 36rpx;
  height: 1rpx;
  background: #c8c2b8;
}

.hint-text {
  font-family: 'Noto Sans SC', 'PingFang SC', sans-serif;
  font-size: 20rpx;
  font-weight: 300;
  color: #9e9890;
  letter-spacing: 0.06em;
  opacity: 0.8;
}

/* 草稿按钮 */
.draft-btn {
  margin-top: 24rpx;
  padding: 12rpx 32rpx;
}

.draft-btn--disabled {
  opacity: 0.6;
}

.draft-btn-text {
  font-family: 'Noto Sans SC', 'PingFang SC', sans-serif;
  font-size: 22rpx;
  font-weight: 300;
  color: #9e9890;
  letter-spacing: 0.06em;
}

/* ── 状态页（初始化中 / 失败） ── */
.state-paper {
  position: relative;
  z-index: 1;
  margin-top: 36rpx;
  padding: 72rpx 48rpx 64rpx;
  background: rgba(253, 251, 247, 0.9);
  border: 1rpx solid rgba(180, 168, 148, 0.35);
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.state-kicker {
  font-family: Georgia, 'Noto Serif SC', serif;
  font-size: 20rpx;
  font-style: italic;
  color: #9e9890;
  letter-spacing: 0.06em;
}

.state-title {
  margin-top: 18rpx;
  color: #302e29;
  font-size: 40rpx;
  line-height: 1.35;
  font-family: 'Noto Serif SC', 'Songti SC', Georgia, serif;
}

.state-desc {
  margin-top: 18rpx;
  color: #9e9890;
  font-size: 26rpx;
  line-height: 1.8;
  font-family: 'Noto Sans SC', 'PingFang SC', sans-serif;
}

.retry-btn {
  margin-top: 34rpx;
  min-width: 240rpx;
  padding: 18rpx 30rpx;
  border: 1rpx solid rgba(180, 168, 148, 0.5);
  color: #6b6560;
  font-size: 26rpx;
  letter-spacing: 0.06em;
  text-align: center;
  font-family: 'Noto Serif SC', 'Songti SC', Georgia, serif;
}
</style>
