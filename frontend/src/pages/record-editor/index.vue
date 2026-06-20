<script setup lang="ts">
import { onLoad, onUnload } from '@dcloudio/uni-app'
import { computed, reactive, ref } from 'vue'
import { hasPreviewSession, showPreviewReadonlyToast } from '../../features/preview/preview-session'
import ImmersiveEditorTopBar from './components/ImmersiveEditorTopBar.vue'
import DateTimeWheelPicker from '../../components/common/DateTimeWheelPicker.vue'
import { aiService, attachmentService, recordService } from '../../services'
import { useRecordStore, useTagStore } from '../../stores'
import {
  LifeNodeType,
  RecordReminderStatus,
  RecordType,
  type RecordAttachmentVO,
  type RecordLocationVO,
  type UpdateRecordLocationDTO,
} from '../../types'
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
const aiOrganizing = ref(false)
const showUnlockPicker = ref(false)
const showLocationPanel = ref(false)
const locationSaving = ref(false)
const location = ref<RecordLocationVO | null>(null)
const manualLocation = reactive({ name: '', address: '' })
const attachments = ref<RecordAttachmentVO[]>([])
const imageAccessUrls = reactive<Record<number, string>>({})
const imageAccessErrors = reactive<Record<number, string>>({})
const imageUploading = ref(false)
const voiceAccessErrors = reactive<Record<number, string>>({})
const voiceUploading = ref(false)
const voiceStarting = ref(false)
const voiceRecording = ref(false)
const voiceStopping = ref(false)
const recordingSeconds = ref(0)
const playingVoiceId = ref<number | null>(null)
const voicePlaybackLoadingId = ref<number | null>(null)
const unlockReminderTemplateId = import.meta.env.VITE_WECHAT_UNLOCK_REMINDER_TEMPLATE_ID || ''

const MAX_IMAGE_COUNT = 9
const MAX_VOICE_COUNT = 9
const MAX_FILE_SIZE_BYTES = 40 * 1024 * 1024
const MAX_TOTAL_SIZE_BYTES = 300 * 1024 * 1024

type ImageUploadStatus = 'compressing' | 'uploading' | 'verifying' | 'failed'
type VoiceUploadStatus = 'uploading' | 'verifying' | 'failed'

interface PendingImageUpload {
  localId: string
  originalPath: string
  filePath: string
  fileName: string
  mimeType: 'image/jpeg'
  status: ImageUploadStatus
  error: string
  prepared: boolean
  sizeBytes: number
  width: number | null
  height: number | null
  uploadedKey?: string
}

const pendingImageUploads = ref<PendingImageUpload[]>([])
let imageSequence = 0

interface PendingVoiceUpload {
  localId: string
  filePath: string
  fileName: string
  mimeType: 'audio/mpeg'
  status: VoiceUploadStatus
  error: string
  sizeBytes: number
  durationSeconds: number
  uploadedKey?: string
}

interface RecorderStopResult {
  tempFilePath?: string
  duration?: number
  fileSize?: number
}

interface RecorderManagerWithOff {
  offStart?: (callback: (result: unknown) => void) => void
  offStop?: (callback: (result: unknown) => void) => void
  offError?: (callback: (result: unknown) => void) => void
}

const pendingVoiceUploads = ref<PendingVoiceUpload[]>([])
const recorderManager = uni.getRecorderManager()
let voiceSequence = 0
let recordingTimer: ReturnType<typeof setInterval> | null = null
let activeAudioContext: ReturnType<typeof uni.createInnerAudioContext> | null = null
let voicePlaybackRequest = 0
let pageActive = true

interface EditorSnapshot {
  title: string
  content: string
  recordType: RecordType
  coreQuestion: string
  unlockAtInput: string
  aiSummary: string
  aiPromptResults: string[]
  beliefThen: string
  lifeNodeType: LifeNodeType | null
  lifeNodeCustomLabel: string
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
  beliefThen: '',
  lifeNodeType: null as LifeNodeType | null,
  lifeNodeCustomLabel: '',
  tagIds: [] as number[],
})

const recordTypeOptions = [
  { value: RecordType.FUTURE_LETTER, label: '写给未来' },
  { value: RecordType.NODE_RECORD, label: '人生节点' },
  { value: RecordType.EMOTION_NOTE, label: '情绪片段' },
]

const lifeNodeOptions = [
  { value: LifeNodeType.GRADUATION, label: '毕业' },
  { value: LifeNodeType.WORK, label: '工作' },
  { value: LifeNodeType.MOVE, label: '搬家' },
  { value: LifeNodeType.RELATIONSHIP, label: '关系' },
  { value: LifeNodeType.HEALTH, label: '健康' },
  { value: LifeNodeType.FAMILY, label: '家庭' },
  { value: LifeNodeType.TURNING_POINT, label: '转折' },
  { value: LifeNodeType.OTHER, label: '其他' },
]

const isLifeNodeRecord = computed(() => form.recordType === RecordType.NODE_RECORD)

const wordCount = computed(() => form.content.replace(/\s/g, '').length)
const imageAttachments = computed(() => attachments.value.filter(
  (attachment) => attachment.type === 'IMAGE' && attachment.status === 'AVAILABLE'
))
const voiceAttachments = computed(() => attachments.value.filter(
  (attachment) => attachment.type === 'VOICE' && attachment.status === 'AVAILABLE'
))
const occupiedImageCount = computed(() => imageAttachments.value.length + pendingImageUploads.value.length)
const occupiedVoiceCount = computed(() => voiceAttachments.value.length + pendingVoiceUploads.value.length)
const availableAttachmentBytes = computed(() => attachments.value
  .filter((attachment) => attachment.status === 'AVAILABLE')
  .reduce((sum, attachment) => sum + attachment.sizeBytes, 0))
const firstImageUploadError = computed(() => pendingImageUploads.value.find(
  (item) => item.status === 'failed' && item.error
)?.error || '')
const firstVoiceUploadError = computed(() => pendingVoiceUploads.value.find(
  (item) => item.status === 'failed' && item.error
)?.error || '')
const mediaOperationActive = computed(() => imageUploading.value
  || voiceUploading.value
  || voiceStarting.value
  || voiceRecording.value
  || voiceStopping.value)
const locationLabel = computed(() => {
  if (!location.value) return ''
  const textLabel = location.value.name?.trim() || location.value.address?.trim()
  if (textLabel) return textLabel
  if (typeof location.value.latitude === 'number' && typeof location.value.longitude === 'number') {
    return `${location.value.latitude.toFixed(6)}, ${location.value.longitude.toFixed(6)}`
  }
  return '已保存地点'
})

const unlockDisplayText = computed(() => {
  if (!form.unlockAtInput) return ''
  const parts = form.unlockAtInput.split(' ')
  if (parts.length === 2) {
    const dateParts = parts[0].split('-')
    const timeParts = parts[1].split(':')
    if (dateParts.length === 3 && timeParts.length === 2) {
      return `${dateParts[0]}年${dateParts[1]}月${dateParts[2]}日 ${timeParts[0]}:${timeParts[1]}`
    }
  }
  return form.unlockAtInput
})

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
    beliefThen: form.beliefThen,
    lifeNodeType: form.lifeNodeType,
    lifeNodeCustomLabel: form.lifeNodeCustomLabel,
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

  if (mediaOperationActive.value) {
    uni.showToast({ title: '媒体正在处理，请稍候', icon: 'none' })
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
  form.beliefThen = detail.beliefThen || ''
  form.lifeNodeType = detail.lifeNodeType || null
  form.lifeNodeCustomLabel = detail.lifeNodeCustomLabel || ''
  form.tagIds = detail.tags.map((tag) => Number(tag.id))
  form.unlockAtInput = detail.unlockAt ? formatDateTime(detail.unlockAt) : ''
  location.value = detail.location || null
  manualLocation.name = detail.location?.name || ''
  manualLocation.address = detail.location?.address || ''
  attachments.value = (detail.attachments || []).filter((attachment) => attachment.status === 'AVAILABLE')
  Object.keys(imageAccessUrls).forEach((key) => delete imageAccessUrls[Number(key)])
  Object.keys(imageAccessErrors).forEach((key) => delete imageAccessErrors[Number(key)])
  Object.keys(voiceAccessErrors).forEach((key) => delete voiceAccessErrors[Number(key)])
  void loadImageAccessUrls(imageAttachments.value)
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
    beliefThen: form.beliefThen || null,
    lifeNodeType: isLifeNodeRecord.value ? form.lifeNodeType : null,
    lifeNodeCustomLabel: isLifeNodeRecord.value ? form.lifeNodeCustomLabel || null : null,
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

const selectRecordType = (recordType: RecordType) => {
  if (loading.value) return
  form.recordType = recordType
  if (recordType !== RecordType.NODE_RECORD) {
    form.lifeNodeType = null
    form.lifeNodeCustomLabel = ''
    return
  }
  if (!form.lifeNodeType) {
    form.lifeNodeType = LifeNodeType.TURNING_POINT
  }
}

const selectLifeNodeType = (lifeNodeType: LifeNodeType) => {
  if (loading.value) return
  form.lifeNodeType = lifeNodeType
  if (lifeNodeType !== LifeNodeType.OTHER) {
    form.lifeNodeCustomLabel = ''
  }
}

const organizeBeliefThen = async () => {
  if (aiOrganizing.value) return
  if (!validateRecordContent(form.content)) {
    uni.showToast({ title: '先写下正文，再整理你当时以为', icon: 'none' })
    return
  }
  if (!getToken() && hasPreviewSession()) {
    showPreviewReadonlyToast()
    return
  }

  aiOrganizing.value = true
  try {
    const result = await aiService.summarizeRecord({
      content: form.content,
      coreQuestion: form.coreQuestion || undefined,
    })
    if (result.status !== 'SUCCESS') {
      uni.showToast({ title: result.message || 'AI整理暂时不可用', icon: 'none' })
      return
    }
    form.aiSummary = result.summary || form.aiSummary
    form.beliefThen = result.beliefThen || result.coreQuestion || result.summary || ''
    uni.showToast({ title: '已整理你当时以为', icon: 'success' })
  } catch (error) {
    uni.showToast({ title: toUserMessage(error), icon: 'none' })
  } finally {
    aiOrganizing.value = false
  }
}

const ensureDraftForAuxiliaryEdit = async (subject: '地点' | '图片' | '语音') => {
  if (recordId.value) return recordId.value
  if (!validateRecordContent(form.content)) {
    throw new Error(`先写下正文，再添加${subject}`)
  }
  const draft = await persistDraft()
  markSnapshot()
  return draft.id
}

const saveLocation = async (payload: UpdateRecordLocationDTO) => {
  if (locationSaving.value) return
  if (!getToken() && hasPreviewSession()) {
    showPreviewReadonlyToast()
    return
  }

  locationSaving.value = true
  try {
    const id = await ensureDraftForAuxiliaryEdit('地点')
    const detail = await recordService.updateLocation(id, payload)
    location.value = detail.location || payload
    manualLocation.name = location.value.name || ''
    manualLocation.address = location.value.address || ''
    recordStore.detail = detail
    showLocationPanel.value = false
    uni.showToast({ title: '地点已保存', icon: 'success' })
  } catch (error) {
    uni.showToast({ title: toUserMessage(error), icon: 'none' })
  } finally {
    locationSaving.value = false
  }
}

const useCurrentLocation = () => {
  if (locationSaving.value) return
  uni.getLocation({
    type: 'gcj02',
    isHighAccuracy: true,
    success: (result) => {
      saveLocation({
        source: 'CURRENT_LOCATION',
        latitude: result.latitude,
        longitude: result.longitude,
      })
    },
    fail: () => {
      uni.showToast({ title: '定位未授权，可选择地图或手动填写', icon: 'none' })
    },
  })
}

const chooseMapLocation = () => {
  if (locationSaving.value) return
  uni.chooseLocation({
    latitude: location.value?.latitude || undefined,
    longitude: location.value?.longitude || undefined,
    success: (result) => {
      saveLocation({
        source: 'MAP_PICKER',
        name: result.name || null,
        address: result.address || null,
        latitude: result.latitude,
        longitude: result.longitude,
      })
    },
    fail: () => {
      uni.showToast({ title: '未选择地点，也可以手动填写', icon: 'none' })
    },
  })
}

const saveManualLocation = () => {
  const name = manualLocation.name.trim()
  const address = manualLocation.address.trim()
  if (!name && !address) {
    uni.showToast({ title: '请填写地点名称或地址', icon: 'none' })
    return
  }
  saveLocation({
    source: 'MANUAL',
    name: name || null,
    address: address || null,
  })
}

const deleteLocation = () => {
  if (!recordId.value || !location.value || locationSaving.value) return
  if (!getToken() && hasPreviewSession()) {
    showPreviewReadonlyToast()
    return
  }
  uni.showModal({
    title: '移除地点？',
    content: '只会移除这条草稿关联的地点。',
    confirmText: '移除',
    success: async (result) => {
      if (!result.confirm || !recordId.value) return
      locationSaving.value = true
      try {
        const detail = await recordService.deleteLocation(recordId.value)
        location.value = null
        manualLocation.name = ''
        manualLocation.address = ''
        recordStore.detail = detail
        uni.showToast({ title: '地点已移除', icon: 'success' })
      } catch (error) {
        uni.showToast({ title: toUserMessage(error), icon: 'none' })
      } finally {
        locationSaving.value = false
      }
    },
  })
}

const chooseImagePaths = () => new Promise<string[]>((resolve, reject) => {
  const remaining = MAX_IMAGE_COUNT - occupiedImageCount.value
  uni.chooseImage({
    count: remaining,
    sizeType: ['original'],
    sourceType: ['album', 'camera'],
    success: (result) => resolve(Array.isArray(result.tempFilePaths) ? result.tempFilePaths : [result.tempFilePaths]),
    fail: (error) => {
      if (String(error.errMsg || '').includes('cancel')) {
        resolve([])
        return
      }
      reject(new Error('图片选择失败，请稍后重试'))
    },
  })
})

const compressImage = (filePath: string) => new Promise<string>((resolve, reject) => {
  uni.compressImage({
    src: filePath,
    quality: 80,
    success: (result) => resolve(result.tempFilePath),
    fail: () => reject(new Error('图片压缩失败，请更换图片后重试')),
  })
})

const getFileSize = (filePath: string) => new Promise<number>((resolve, reject) => {
  uni.getFileInfo({
    filePath,
    success: (result) => resolve(result.size),
    fail: () => reject(new Error('无法读取压缩后的图片大小')),
  })
})

const getImageDimensions = (filePath: string) => new Promise<{ width: number; height: number }>((resolve, reject) => {
  uni.getImageInfo({
    src: filePath,
    success: (result) => resolve({ width: result.width, height: result.height }),
    fail: () => reject(new Error('无法读取压缩后的图片信息')),
  })
})

const imageUploadStatusLabel = (status: ImageUploadStatus) => {
  if (status === 'compressing') return '压缩中'
  if (status === 'uploading') return '上传中'
  if (status === 'verifying') return '校验中'
  return '上传失败'
}

const createPendingImage = (filePath: string): PendingImageUpload => {
  imageSequence += 1
  const stamp = Date.now()
  return {
    localId: `${stamp}-${imageSequence}`,
    originalPath: filePath,
    filePath,
    fileName: `image-${stamp}-${imageSequence}.jpg`,
    mimeType: 'image/jpeg',
    status: 'compressing',
    error: '',
    prepared: false,
    sizeBytes: 0,
    width: null,
    height: null,
  }
}

const reservedAttachmentBytes = (excludedLocalId?: string) => availableAttachmentBytes.value
  + pendingImageUploads.value
    .filter((item) => item.localId !== excludedLocalId)
    .reduce((sum, item) => sum + item.sizeBytes, 0)
  + pendingVoiceUploads.value
    .filter((item) => item.localId !== excludedLocalId)
    .reduce((sum, item) => sum + item.sizeBytes, 0)

const preparePendingImage = async (item: PendingImageUpload) => {
  if (item.prepared) return
  item.status = 'compressing'
  item.error = ''
  const compressedPath = await compressImage(item.originalPath)
  const [sizeBytes, dimensions] = await Promise.all([
    getFileSize(compressedPath),
    getImageDimensions(compressedPath),
  ])
  if (sizeBytes > MAX_FILE_SIZE_BYTES) {
    throw new Error('压缩后图片仍超过 40 MB，请更换图片')
  }
  if (reservedAttachmentBytes(item.localId) + sizeBytes > MAX_TOTAL_SIZE_BYTES) {
    throw new Error('这条记录的附件总大小不能超过 300 MB')
  }
  item.filePath = compressedPath
  item.sizeBytes = sizeBytes
  item.width = dimensions.width
  item.height = dimensions.height
  item.prepared = true
}

const syncDetailAttachments = (deletedAttachmentId?: number) => {
  if (!recordStore.detail || recordStore.detail.id !== recordId.value) return
  recordStore.detail = {
    ...recordStore.detail,
    attachments: [...attachments.value],
    cover: deletedAttachmentId && recordStore.detail.cover?.id === deletedAttachmentId
      ? null
      : recordStore.detail.cover,
  }
}

const loadImageAccessUrl = async (attachment: RecordAttachmentVO, forceRefresh = false) => {
  if (!forceRefresh && imageAccessUrls[attachment.id]) {
    return imageAccessUrls[attachment.id]
  }
  if (!recordId.value) {
    throw new Error('记录尚未保存，无法获取图片')
  }
  try {
    const access = await attachmentService.createAccessUrl(recordId.value, attachment.id)
    imageAccessUrls[attachment.id] = access.url
    delete imageAccessErrors[attachment.id]
    return access.url
  } catch (error) {
    delete imageAccessUrls[attachment.id]
    imageAccessErrors[attachment.id] = toUserMessage(error)
    throw error
  }
}

const loadImageAccessUrls = async (items: RecordAttachmentVO[]) => {
  await Promise.allSettled(items.map((attachment) => loadImageAccessUrl(attachment)))
}

const commitPendingImage = async (id: number, item: PendingImageUpload) => {
  if (!item.uploadedKey) {
    item.status = 'uploading'
    const authorization = await attachmentService.createUploadToken(id, {
      type: 'IMAGE',
      fileName: item.fileName,
      mimeType: item.mimeType,
      sizeBytes: item.sizeBytes,
    })
    if (item.sizeBytes > authorization.maxFileSizeBytes) {
      throw new Error('图片超过存储服务允许的大小')
    }
    await attachmentService.uploadToQiniu(item.filePath, authorization)
    item.uploadedKey = authorization.key
  }

  item.status = 'verifying'
  const attachment = await attachmentService.commit(id, {
    type: 'IMAGE',
    key: item.uploadedKey,
    fileName: item.fileName,
    mimeType: item.mimeType,
    sizeBytes: item.sizeBytes,
    width: item.width,
    height: item.height,
    durationSeconds: null,
  })
  if (attachment.status !== 'AVAILABLE') {
    throw new Error('图片尚未通过存储校验')
  }
  attachments.value.push(attachment)
  pendingImageUploads.value = pendingImageUploads.value.filter((pending) => pending.localId !== item.localId)
  syncDetailAttachments()
  try {
    await loadImageAccessUrl(attachment, true)
  } catch {
    // Attachment is committed; the visible placeholder provides a retry path for signed URL failures.
  }
}

const processPendingImage = async (id: number, item: PendingImageUpload) => {
  try {
    await preparePendingImage(item)
    await commitPendingImage(id, item)
    return true
  } catch (error) {
    item.status = 'failed'
    item.error = toUserMessage(error)
    return false
  }
}

const selectAndUploadImages = async () => {
  if (imageUploading.value) return
  if (voiceUploading.value || voiceStarting.value || voiceRecording.value || voiceStopping.value) {
    uni.showToast({ title: '请先结束当前语音操作', icon: 'none' })
    return
  }
  if (!getToken() && hasPreviewSession()) {
    showPreviewReadonlyToast()
    return
  }
  if (occupiedImageCount.value >= MAX_IMAGE_COUNT) {
    uni.showToast({ title: '每条记录最多添加 9 张图片', icon: 'none' })
    return
  }
  if (!validateRecordContent(form.content)) {
    uni.showToast({ title: '先写下正文，再添加图片', icon: 'none' })
    return
  }

  try {
    const paths = await chooseImagePaths()
    if (!paths.length) return
    imageUploading.value = true
    const id = await ensureDraftForAuxiliaryEdit('图片')
    let successCount = 0
    for (const path of paths) {
      const pending = createPendingImage(path)
      pendingImageUploads.value.push(pending)
      if (await processPendingImage(id, pending)) {
        successCount += 1
      }
    }
    const failedCount = paths.length - successCount
    if (failedCount > 0) {
      uni.showToast({ title: `${failedCount} 张图片上传失败，可重试`, icon: 'none' })
    } else {
      uni.showToast({ title: `${successCount} 张图片已上传`, icon: 'success' })
    }
  } catch (error) {
    uni.showToast({ title: toUserMessage(error), icon: 'none' })
  } finally {
    imageUploading.value = false
  }
}

const retryPendingImage = async (item: PendingImageUpload) => {
  if (mediaOperationActive.value || item.status !== 'failed') return
  imageUploading.value = true
  try {
    const id = await ensureDraftForAuxiliaryEdit('图片')
    if (await processPendingImage(id, item)) {
      uni.showToast({ title: '图片已上传', icon: 'success' })
      return
    }
    uni.showToast({ title: item.error || '图片上传失败', icon: 'none' })
  } catch (error) {
    item.status = 'failed'
    item.error = toUserMessage(error)
    uni.showToast({ title: item.error, icon: 'none' })
  } finally {
    imageUploading.value = false
  }
}

const removePendingImage = (item: PendingImageUpload) => {
  if (imageUploading.value) return
  pendingImageUploads.value = pendingImageUploads.value.filter((pending) => pending.localId !== item.localId)
}

const previewImage = async (attachment: RecordAttachmentVO) => {
  try {
    const refreshed = await Promise.allSettled(imageAttachments.value.map(
      (item) => loadImageAccessUrl(item, true)
    ))
    const urls = refreshed
      .filter((result): result is PromiseFulfilledResult<string> => result.status === 'fulfilled')
      .map((result) => result.value)
    const current = imageAccessUrls[attachment.id]
    if (!current || !urls.includes(current)) {
      throw new Error('图片访问地址暂不可用，请稍后重试')
    }
    uni.previewImage({ current, urls })
  } catch (error) {
    uni.showToast({ title: toUserMessage(error), icon: 'none' })
  }
}

const markImageLoadFailed = (attachmentId: number) => {
  delete imageAccessUrls[attachmentId]
  imageAccessErrors[attachmentId] = '图片加载失败，点击重试'
}

const deleteImage = (attachment: RecordAttachmentVO) => {
  if (!recordId.value || mediaOperationActive.value) return
  if (!getToken() && hasPreviewSession()) {
    showPreviewReadonlyToast()
    return
  }
  uni.showModal({
    title: '删除图片？',
    content: '图片将从这条草稿中移除。',
    confirmText: '删除',
    success: async (result) => {
      if (!result.confirm || !recordId.value) return
      imageUploading.value = true
      try {
        await attachmentService.delete(recordId.value, attachment.id)
        attachments.value = attachments.value.filter((item) => item.id !== attachment.id)
        delete imageAccessUrls[attachment.id]
        delete imageAccessErrors[attachment.id]
        syncDetailAttachments(attachment.id)
        uni.showToast({ title: '图片已删除', icon: 'success' })
      } catch (error) {
        uni.showToast({ title: toUserMessage(error), icon: 'none' })
      } finally {
        imageUploading.value = false
      }
    },
  })
}

const voiceUploadStatusLabel = (status: VoiceUploadStatus) => {
  if (status === 'uploading') return '上传中'
  if (status === 'verifying') return '校验中'
  return '上传失败'
}

const formatVoiceDuration = (durationSeconds?: number | null) => {
  const total = Math.max(0, Math.round(durationSeconds || 0))
  const minutes = Math.floor(total / 60)
  const seconds = total % 60
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
}

const clearRecordingTimer = () => {
  if (recordingTimer) {
    clearInterval(recordingTimer)
    recordingTimer = null
  }
}

const stopActiveAudio = () => {
  voicePlaybackRequest += 1
  voicePlaybackLoadingId.value = null
  if (!activeAudioContext) return
  const context = activeAudioContext
  activeAudioContext = null
  playingVoiceId.value = null
  context.stop()
  context.destroy()
}

const createPendingVoice = (
  filePath: string,
  sizeBytes: number,
  durationSeconds: number
): PendingVoiceUpload => {
  voiceSequence += 1
  const stamp = Date.now()
  return {
    localId: `voice-${stamp}-${voiceSequence}`,
    filePath,
    fileName: `voice-${stamp}-${voiceSequence}.mp3`,
    mimeType: 'audio/mpeg',
    status: 'uploading',
    error: '',
    sizeBytes,
    durationSeconds,
  }
}

const commitPendingVoice = async (id: number, item: PendingVoiceUpload) => {
  if (!item.uploadedKey) {
    item.status = 'uploading'
    const authorization = await attachmentService.createUploadToken(id, {
      type: 'VOICE',
      fileName: item.fileName,
      mimeType: item.mimeType,
      sizeBytes: item.sizeBytes,
    })
    if (item.sizeBytes > authorization.maxFileSizeBytes) {
      throw new Error('语音超过存储服务允许的大小')
    }
    await attachmentService.uploadToQiniu(item.filePath, authorization)
    item.uploadedKey = authorization.key
  }

  item.status = 'verifying'
  const attachment = await attachmentService.commit(id, {
    type: 'VOICE',
    key: item.uploadedKey,
    fileName: item.fileName,
    mimeType: item.mimeType,
    sizeBytes: item.sizeBytes,
    width: null,
    height: null,
    durationSeconds: item.durationSeconds,
  })
  if (attachment.status !== 'AVAILABLE') {
    throw new Error('语音尚未通过存储校验')
  }
  attachments.value.push(attachment)
  pendingVoiceUploads.value = pendingVoiceUploads.value.filter((pending) => pending.localId !== item.localId)
  syncDetailAttachments()
}

const processPendingVoice = async (id: number, item: PendingVoiceUpload) => {
  try {
    await commitPendingVoice(id, item)
    return true
  } catch (error) {
    item.status = 'failed'
    item.error = toUserMessage(error)
    return false
  }
}

const uploadRecordedVoice = async (result: RecorderStopResult) => {
  const filePath = result.tempFilePath || ''
  if (!filePath) {
    uni.showToast({ title: '没有取得录音文件，请重试', icon: 'none' })
    return
  }

  voiceUploading.value = true
  let pending: PendingVoiceUpload | null = null
  try {
    const sizeBytes = typeof result.fileSize === 'number' && result.fileSize > 0
      ? result.fileSize
      : await getFileSize(filePath)
    if (sizeBytes > MAX_FILE_SIZE_BYTES) {
      throw new Error('语音文件超过 40 MB，请缩短录音后重试')
    }
    if (reservedAttachmentBytes() + sizeBytes > MAX_TOTAL_SIZE_BYTES) {
      throw new Error('这条记录的附件总大小不能超过 300 MB')
    }
    const durationSeconds = Math.max(1, Math.ceil((result.duration || recordingSeconds.value * 1000) / 1000))
    pending = createPendingVoice(filePath, sizeBytes, durationSeconds)
    pendingVoiceUploads.value.push(pending)
    const id = await ensureDraftForAuxiliaryEdit('语音')
    if (await processPendingVoice(id, pending)) {
      uni.showToast({ title: '语音已上传', icon: 'success' })
      return
    }
    uni.showToast({ title: pending.error || '语音上传失败，可重试', icon: 'none' })
  } catch (error) {
    if (pending) {
      pending.status = 'failed'
      pending.error = toUserMessage(error)
    }
    uni.showToast({ title: toUserMessage(error), icon: 'none' })
  } finally {
    voiceUploading.value = false
  }
}

const handleRecorderStart = (_result: unknown) => {
  if (!pageActive) return
  voiceStarting.value = false
  voiceStopping.value = false
  voiceRecording.value = true
  recordingSeconds.value = 0
  clearRecordingTimer()
  recordingTimer = setInterval(() => {
    recordingSeconds.value += 1
  }, 1000)
}

const handleRecorderStop = (result: unknown) => {
  clearRecordingTimer()
  voiceStarting.value = false
  voiceStopping.value = false
  voiceRecording.value = false
  if (!pageActive) return
  void uploadRecordedVoice((result || {}) as RecorderStopResult)
}

const handleRecorderError = (result: unknown) => {
  clearRecordingTimer()
  voiceStarting.value = false
  voiceStopping.value = false
  voiceRecording.value = false
  if (!pageActive) return
  const message = String((result as { errMsg?: string } | null)?.errMsg || '')
  const title = /auth|permission|authorize|denied/i.test(message)
    ? '录音权限不可用，请在小程序设置中开启麦克风'
    : '录音失败，请稍后重试'
  uni.showToast({ title, icon: 'none' })
}

recorderManager.onStart(handleRecorderStart)
recorderManager.onStop(handleRecorderStop)
recorderManager.onError(handleRecorderError)

const startVoiceRecording = () => {
  if (mediaOperationActive.value) return
  if (!getToken() && hasPreviewSession()) {
    showPreviewReadonlyToast()
    return
  }
  if (occupiedVoiceCount.value >= MAX_VOICE_COUNT) {
    uni.showToast({ title: '每条记录最多添加 9 条语音', icon: 'none' })
    return
  }
  if (!validateRecordContent(form.content)) {
    uni.showToast({ title: '先写下正文，再添加语音', icon: 'none' })
    return
  }
  stopActiveAudio()
  voiceStarting.value = true
  recorderManager.start({
    duration: 600000,
    sampleRate: 16000,
    numberOfChannels: 1,
    encodeBitRate: 64000,
    format: 'mp3',
  })
}

const stopVoiceRecording = () => {
  if (!voiceRecording.value || voiceStopping.value) return
  voiceStopping.value = true
  recorderManager.stop()
}

const retryPendingVoice = async (item: PendingVoiceUpload) => {
  if (mediaOperationActive.value || item.status !== 'failed') return
  voiceUploading.value = true
  try {
    if (reservedAttachmentBytes(item.localId) + item.sizeBytes > MAX_TOTAL_SIZE_BYTES) {
      throw new Error('这条记录的附件总大小不能超过 300 MB')
    }
    const id = await ensureDraftForAuxiliaryEdit('语音')
    if (await processPendingVoice(id, item)) {
      uni.showToast({ title: '语音已上传', icon: 'success' })
      return
    }
    uni.showToast({ title: item.error || '语音上传失败', icon: 'none' })
  } catch (error) {
    item.status = 'failed'
    item.error = toUserMessage(error)
    uni.showToast({ title: item.error, icon: 'none' })
  } finally {
    voiceUploading.value = false
  }
}

const removePendingVoice = (item: PendingVoiceUpload) => {
  if (mediaOperationActive.value) return
  pendingVoiceUploads.value = pendingVoiceUploads.value.filter((pending) => pending.localId !== item.localId)
}

const playVoice = async (attachment: RecordAttachmentVO) => {
  if (mediaOperationActive.value) return
  if (playingVoiceId.value === attachment.id && activeAudioContext) {
    stopActiveAudio()
    return
  }

  stopActiveAudio()
  const requestId = ++voicePlaybackRequest
  voicePlaybackLoadingId.value = attachment.id
  try {
    if (!recordId.value) {
      throw new Error('记录尚未保存，无法播放语音')
    }
    const access = await attachmentService.createAccessUrl(recordId.value, attachment.id)
    if (!pageActive || requestId !== voicePlaybackRequest) return
    delete voiceAccessErrors[attachment.id]
    const context = uni.createInnerAudioContext()
    activeAudioContext = context
    context.src = access.url
    context.onPlay(() => {
      voicePlaybackLoadingId.value = null
      playingVoiceId.value = attachment.id
    })
    const release = () => {
      if (activeAudioContext !== context) return
      activeAudioContext = null
      voicePlaybackLoadingId.value = null
      playingVoiceId.value = null
      context.destroy()
    }
    context.onEnded(release)
    context.onStop(release)
    context.onError(() => {
      voiceAccessErrors[attachment.id] = '语音播放失败，请重试'
      release()
      uni.showToast({ title: '语音播放失败，请稍后重试', icon: 'none' })
    })
    context.play()
  } catch (error) {
    if (requestId !== voicePlaybackRequest) return
    voicePlaybackLoadingId.value = null
    voiceAccessErrors[attachment.id] = toUserMessage(error)
    uni.showToast({ title: voiceAccessErrors[attachment.id], icon: 'none' })
  }
}

const deleteVoiceAttachment = async (attachment: RecordAttachmentVO) => {
  if (!recordId.value) return false
  voiceUploading.value = true
  try {
    if (playingVoiceId.value === attachment.id) {
      stopActiveAudio()
    }
    await attachmentService.delete(recordId.value, attachment.id)
    attachments.value = attachments.value.filter((item) => item.id !== attachment.id)
    delete voiceAccessErrors[attachment.id]
    syncDetailAttachments(attachment.id)
    return true
  } catch (error) {
    uni.showToast({ title: toUserMessage(error), icon: 'none' })
    return false
  } finally {
    voiceUploading.value = false
  }
}

const deleteVoice = (attachment: RecordAttachmentVO) => {
  if (mediaOperationActive.value) return
  if (!getToken() && hasPreviewSession()) {
    showPreviewReadonlyToast()
    return
  }
  uni.showModal({
    title: '删除语音？',
    content: '语音将从这条草稿中移除。',
    confirmText: '删除',
    success: async (result) => {
      if (!result.confirm) return
      if (await deleteVoiceAttachment(attachment)) {
        uni.showToast({ title: '语音已删除', icon: 'success' })
      }
    },
  })
}

const reRecordVoice = (attachment: RecordAttachmentVO) => {
  if (mediaOperationActive.value) return
  if (!getToken() && hasPreviewSession()) {
    showPreviewReadonlyToast()
    return
  }
  uni.showModal({
    title: '重新录制？',
    content: '将先删除这条语音，删除成功后开始新录音。',
    confirmText: '重新录制',
    success: async (result) => {
      if (!result.confirm) return
      if (await deleteVoiceAttachment(attachment)) {
        startVoiceRecording()
      }
    },
  })
}

const requestUnlockReminderAuthorization = () => new Promise<'accepted' | 'rejected' | 'skipped'>((resolve) => {
  if (!unlockReminderTemplateId || typeof uni.requestSubscribeMessage !== 'function') {
    resolve('skipped')
    return
  }
  uni.requestSubscribeMessage({
    tmplIds: [unlockReminderTemplateId],
    success: (res) => {
      const result = res as unknown as Record<string, string>
      resolve(result[unlockReminderTemplateId] === 'accept' ? 'accepted' : 'rejected')
    },
    fail: () => resolve('skipped'),
  })
})

const toUnlockReminderAuthorizationStatus = (result: 'accepted' | 'rejected' | 'skipped') => {
  if (result === 'accepted') return RecordReminderStatus.AUTHORIZED
  if (result === 'rejected') return RecordReminderStatus.DENIED
  return RecordReminderStatus.REQUESTED
}

const reportUnlockReminderAuthorization = async (id: number) => {
  try {
    const authorizationResult = await requestUnlockReminderAuthorization()
    const updated = await recordStore.updateUnlockReminderAuthorization(
      id,
      toUnlockReminderAuthorizationStatus(authorizationResult)
    )
    recordStore.detail = updated
  } catch {
    // The record is already sealed; reminder authorization reporting must not undo it.
  }
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

  if (mediaOperationActive.value) {
    uni.showToast({ title: '请先结束录音或等待媒体上传完成', icon: 'none' })
    return
  }

  if (pendingImageUploads.value.length || pendingVoiceUploads.value.length) {
    uni.showToast({ title: '请先重试或移除未完成的媒体', icon: 'none' })
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
    await reportUnlockReminderAuthorization(draft.id)
    uni.showToast({ title: '已封存这一刻', icon: 'success' })
    setTimeout(() => returnToSource(), 300)
  } catch (error) {
    uni.showToast({ title: toUserMessage(error), icon: 'none' })
  } finally {
    loading.value = false
  }
}

const onAuxTap = (name: '地点' | '图片' | '语音') => {
  if (name === '地点') {
    if (!getToken() && hasPreviewSession()) {
      showPreviewReadonlyToast()
      return
    }
    showLocationPanel.value = !showLocationPanel.value
    return
  }
  if (name === '图片') {
    void selectAndUploadImages()
    return
  }
  if (voiceRecording.value) {
    stopVoiceRecording()
    return
  }
  startVoiceRecording()
}

const onUnlockBarTap = () => {
  showUnlockPicker.value = true
}

const onUnlockConfirm = (datetime: string) => {
  form.unlockAtInput = datetime
  showUnlockPicker.value = false
}

onLoad(async (query) => {
  if (!ensureLogin()) {
    return
  }

  source.value = resolveSource(typeof query?.source === 'string' ? query.source : undefined)
  latestQuery.value = query as Record<string, unknown>
  await runInitialization(latestQuery.value)
})

onUnload(() => {
  pageActive = false
  const managerWithOff = recorderManager as unknown as RecorderManagerWithOff
  managerWithOff.offStart?.(handleRecorderStart)
  managerWithOff.offStop?.(handleRecorderStop)
  managerWithOff.offError?.(handleRecorderError)
  if (voiceStarting.value || voiceRecording.value || voiceStopping.value) {
    recorderManager.stop()
  }
  clearRecordingTimer()
  voiceStarting.value = false
  voiceRecording.value = false
  voiceStopping.value = false
  stopActiveAudio()
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
                <text class="captured-label">记录于</text>
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

            <view class="m3-panel">
              <view class="m3-panel-label">记录类型</view>
              <view class="type-row">
                <view
                  v-for="option in recordTypeOptions"
                  :key="option.value"
                  class="type-chip"
                  :class="{ 'type-chip--active': form.recordType === option.value }"
                  @tap="selectRecordType(option.value)"
                >
                  <text>{{ option.label }}</text>
                </view>
              </view>

              <view v-if="isLifeNodeRecord" class="life-node-block">
                <view class="m3-panel-label">人生节点</view>
                <view class="life-node-grid">
                  <view
                    v-for="option in lifeNodeOptions"
                    :key="option.value"
                    class="life-node-chip"
                    :class="{ 'life-node-chip--active': form.lifeNodeType === option.value }"
                    @tap="selectLifeNodeType(option.value)"
                  >
                    <text>{{ option.label }}</text>
                  </view>
                </view>
                <input
                  v-if="form.lifeNodeType === LifeNodeType.OTHER"
                  v-model="form.lifeNodeCustomLabel"
                  class="life-node-input"
                  placeholder="写下这个节点的名字"
                  placeholder-class="unlock-placeholder"
                  maxlength="50"
                />
              </view>

              <view class="belief-block">
                <view class="belief-head">
                  <text class="m3-panel-label">你当时以为</text>
                  <view class="belief-action" :class="{ 'belief-action--loading': aiOrganizing }" @tap="organizeBeliefThen">
                    {{ aiOrganizing ? '整理中' : 'AI整理' }}
                  </view>
                </view>
                <textarea
                  v-model="form.beliefThen"
                  class="belief-textarea"
                  auto-height
                  maxlength="1000"
                  placeholder="可由 AI 帮你整理，也可以自己微调"
                  placeholder-class="editor-placeholder"
                />
              </view>
            </view>

            <!-- 解封时间设置区 -->
            <view class="unlock-bar" @tap="onUnlockBarTap">
              <text class="unlock-label">解封时间</text>
              <view class="unlock-display">
                <text
                  class="unlock-text"
                  :class="{ 'unlock-text--placeholder': !form.unlockAtInput }"
                >{{ form.unlockAtInput ? unlockDisplayText : '选择未来开启的时间' }}</text>
                <text class="unlock-arrow">›</text>
              </view>
            </view>

            <view v-if="location" class="location-summary">
              <view class="location-summary-text">
                <text class="location-summary-label">已选地点</text>
                <text class="location-summary-value">{{ locationLabel }}</text>
              </view>
              <view class="location-remove" @tap="deleteLocation">移除</view>
            </view>

            <view v-if="showLocationPanel" class="location-panel">
              <view class="location-modes">
                <view class="location-mode" @tap="useCurrentLocation">当前位置</view>
                <view class="location-mode" @tap="chooseMapLocation">地图选择</view>
                <view class="location-mode location-mode--active">手动填写</view>
              </view>
              <input
                v-model="manualLocation.name"
                class="location-input"
                maxlength="100"
                placeholder="地点名称"
                placeholder-class="location-placeholder"
              />
              <input
                v-model="manualLocation.address"
                class="location-input"
                maxlength="255"
                placeholder="详细地址（可选）"
                placeholder-class="location-placeholder"
              />
              <view class="location-save" :class="{ 'location-save--disabled': locationSaving }" @tap="saveManualLocation">
                {{ locationSaving ? '保存中...' : '保存手动地点' }}
              </view>
            </view>

            <view v-if="imageAttachments.length || pendingImageUploads.length" class="image-panel">
              <view class="image-panel-head">
                <text class="image-panel-title">图片附件</text>
                <text class="image-panel-count">{{ occupiedImageCount }}/{{ MAX_IMAGE_COUNT }}</text>
              </view>
              <view class="image-grid">
                <view v-for="attachment in imageAttachments" :key="attachment.id" class="image-tile">
                  <image
                    v-if="imageAccessUrls[attachment.id]"
                    class="image-thumb"
                    :src="imageAccessUrls[attachment.id]"
                    mode="aspectFill"
                    @tap="previewImage(attachment)"
                    @error="markImageLoadFailed(attachment.id)"
                  />
                  <view v-else class="image-access-failed" @tap="previewImage(attachment)">
                    <text>{{ imageAccessErrors[attachment.id] ? '加载失败' : '取图中' }}</text>
                    <text v-if="imageAccessErrors[attachment.id]" class="image-access-retry">点击重试</text>
                  </view>
                  <view class="image-delete" aria-label="删除图片" @tap.stop="deleteImage(attachment)">×</view>
                </view>

                <view v-for="item in pendingImageUploads" :key="item.localId" class="image-tile">
                  <image class="image-thumb" :src="item.filePath" mode="aspectFill" />
                  <view class="image-upload-mask">
                    <text>{{ imageUploadStatusLabel(item.status) }}</text>
                  </view>
                  <view v-if="item.status === 'failed'" class="image-pending-actions">
                    <view class="image-retry" @tap.stop="retryPendingImage(item)">重试</view>
                    <view class="image-pending-remove" aria-label="移除待上传图片" @tap.stop="removePendingImage(item)">×</view>
                  </view>
                </view>
              </view>
              <text v-if="firstImageUploadError" class="image-upload-error">{{ firstImageUploadError }}</text>
            </view>

            <view
              v-if="voiceAttachments.length || pendingVoiceUploads.length || voiceStarting || voiceRecording || voiceStopping"
              class="voice-panel"
            >
              <view class="voice-panel-head">
                <text class="voice-panel-title">语音附件</text>
                <text class="voice-panel-count">{{ occupiedVoiceCount }}/{{ MAX_VOICE_COUNT }}</text>
              </view>

              <view v-if="voiceStarting || voiceRecording || voiceStopping" class="voice-recording-row">
                <view class="voice-recording-dot" aria-hidden="true" />
                <text class="voice-recording-label">
                  {{ voiceStarting ? '正在启动录音' : voiceStopping ? '正在结束录音' : '正在录音' }}
                </text>
                <text class="voice-recording-time">{{ formatVoiceDuration(recordingSeconds) }}</text>
              </view>

              <view v-for="(attachment, index) in voiceAttachments" :key="attachment.id" class="voice-row">
                <view
                  class="voice-play"
                  :class="{
                    'voice-play--active': playingVoiceId === attachment.id,
                    'voice-play--loading': voicePlaybackLoadingId === attachment.id,
                  }"
                  :aria-label="playingVoiceId === attachment.id
                    ? '停止播放'
                    : voicePlaybackLoadingId === attachment.id
                      ? '正在加载语音'
                      : '播放语音'"
                  @tap="playVoice(attachment)"
                >
                  {{ playingVoiceId === attachment.id ? '■' : voicePlaybackLoadingId === attachment.id ? '…' : '▶' }}
                </view>
                <view class="voice-info">
                  <text class="voice-name">语音记录 {{ index + 1 }}</text>
                  <text class="voice-meta">{{ formatVoiceDuration(attachment.durationSeconds) }}</text>
                  <text v-if="voiceAccessErrors[attachment.id]" class="voice-error">
                    {{ voiceAccessErrors[attachment.id] }}
                  </text>
                </view>
                <view class="voice-actions">
                  <view class="voice-action" @tap="reRecordVoice(attachment)">重录</view>
                  <view class="voice-delete" aria-label="删除语音" @tap="deleteVoice(attachment)">×</view>
                </view>
              </view>

              <view v-for="item in pendingVoiceUploads" :key="item.localId" class="voice-row voice-row--pending">
                <view class="voice-play voice-play--disabled" aria-hidden="true">▶</view>
                <view class="voice-info">
                  <text class="voice-name">{{ voiceUploadStatusLabel(item.status) }}</text>
                  <text class="voice-meta">{{ formatVoiceDuration(item.durationSeconds) }}</text>
                </view>
                <view v-if="item.status === 'failed'" class="voice-actions">
                  <view class="voice-action" @tap="retryPendingVoice(item)">重试</view>
                  <view class="voice-delete" aria-label="移除待上传语音" @tap="removePendingVoice(item)">×</view>
                </view>
              </view>
              <text v-if="firstVoiceUploadError" class="voice-upload-error">{{ firstVoiceUploadError }}</text>
            </view>

            <!-- 附件栏 MAP / IMAGE / VOICE -->
            <view class="attach-bar">
              <view class="attach-item" :class="{ 'attach-item--active': showLocationPanel || location }" @tap="onAuxTap('地点')">
                <view class="attach-icon attach-icon--map" aria-hidden="true" />
                <text class="attach-label">{{ location ? '已选地点' : '地点' }}</text>
              </view>
              <view class="attach-sep" aria-hidden="true" />
              <view
                class="attach-item"
                :class="{ 'attach-item--active': occupiedImageCount > 0, 'attach-item--disabled': imageUploading }"
                @tap="onAuxTap('图片')"
              >
                <view class="attach-icon attach-icon--image" aria-hidden="true" />
                <text class="attach-label">{{ imageUploading ? '处理中' : occupiedImageCount ? `图片 ${occupiedImageCount}/9` : '图片' }}</text>
              </view>
              <view class="attach-sep" aria-hidden="true" />
              <view
                class="attach-item"
                :class="{
                  'attach-item--active': occupiedVoiceCount > 0 || voiceStarting || voiceRecording,
                  'attach-item--disabled': imageUploading || voiceUploading || voiceStarting || voiceStopping,
                }"
                @tap="onAuxTap('语音')"
              >
                <view class="attach-icon attach-icon--voice" aria-hidden="true" />
                <text class="attach-label">
                  {{ voiceRecording
                    ? `停止 ${formatVoiceDuration(recordingSeconds)}`
                    : voiceStarting
                      ? '启动录音'
                      : voiceStopping || voiceUploading
                        ? '处理中'
                        : occupiedVoiceCount
                          ? `语音 ${occupiedVoiceCount}/9`
                          : '语音' }}
                </text>
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

  <DateTimeWheelPicker
    :visible="showUnlockPicker"
    :initial-value="form.unlockAtInput || null"
    @confirm="onUnlockConfirm"
    @cancel="showUnlockPicker = false"
  />
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
  font-family: 'Noto Serif SC', 'Songti SC', Georgia, serif;
  font-size: 20rpx;
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

.m3-panel {
  margin: 0 40rpx 28rpx 48rpx;
  padding: 24rpx;
  border: 1rpx solid rgba(192, 182, 165, 0.32);
  background: rgba(253, 251, 247, 0.74);
}

.m3-panel-label {
  font-family: 'Noto Serif SC', 'Songti SC', Georgia, serif;
  font-size: 22rpx;
  color: #857b6d;
}

.type-row,
.life-node-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
  margin-top: 18rpx;
}

.type-chip,
.life-node-chip {
  min-width: 132rpx;
  height: 56rpx;
  padding: 0 18rpx;
  border: 1rpx solid rgba(166, 150, 124, 0.36);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22rpx;
  color: #6b6257;
  background: rgba(255, 252, 246, 0.82);
  box-sizing: border-box;
}

.type-chip--active,
.life-node-chip--active {
  border-color: rgba(181, 53, 42, 0.48);
  color: #9a332a;
  background: rgba(181, 53, 42, 0.06);
}

.life-node-block,
.belief-block {
  margin-top: 24rpx;
}

.life-node-input {
  margin-top: 18rpx;
  height: 64rpx;
  padding: 0 18rpx;
  border-bottom: 1rpx solid rgba(166, 150, 124, 0.38);
  font-size: 24rpx;
  color: #4a4640;
}

.belief-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
}

.belief-action {
  min-width: 112rpx;
  height: 48rpx;
  padding: 0 18rpx;
  border: 1rpx solid rgba(181, 53, 42, 0.36);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22rpx;
  color: #9a332a;
}

.belief-action--loading {
  opacity: 0.66;
}

.belief-textarea {
  width: 100%;
  min-height: 92rpx;
  margin-top: 14rpx;
  font-family: 'Noto Serif SC', 'Songti SC', Georgia, serif;
  font-size: 25rpx;
  line-height: 1.75;
  color: #4a4640;
}

/* 解封时间设置区 */
.unlock-bar {
  display: flex;
  align-items: center;
  gap: 24rpx;
  padding: 24rpx 40rpx;
  border-top: 1rpx solid rgba(192, 182, 165, 0.15);
}

.unlock-label {
  flex-shrink: 0;
  font-family: 'Noto Serif SC', 'Songti SC', Georgia, serif;
  font-size: 20rpx;
  color: #9e9890;
  letter-spacing: 0.04em;
}

.unlock-display {
  flex: 1;
  display: flex;
  align-items: center;
  min-height: 40rpx;
  gap: 8rpx;
}

.unlock-text {
  flex: 1;
  font-family: 'Noto Serif SC', 'Songti SC', Georgia, serif;
  font-size: 22rpx;
  color: #6b6560;
  letter-spacing: 0.03em;
  line-height: 1.4;
}

.unlock-text--placeholder {
  color: rgba(180, 170, 155, 0.7);
}

.unlock-arrow {
  font-family: 'Noto Serif SC', 'Songti SC', Georgia, serif;
  font-size: 40rpx;
  color: #c8c2b8;
  flex-shrink: 0;
  line-height: 1;
}

.location-summary {
  padding: 20rpx 40rpx;
  border-top: 1rpx solid rgba(192, 182, 165, 0.2);
  display: flex;
  align-items: center;
  gap: 24rpx;
}

.location-summary-text {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}

.location-summary-label {
  font-size: 19rpx;
  color: #9e9890;
}

.location-summary-value {
  font-size: 23rpx;
  line-height: 1.5;
  color: #5f5850;
  word-break: break-all;
}

.location-remove {
  flex-shrink: 0;
  padding: 10rpx 0 10rpx 20rpx;
  font-size: 21rpx;
  color: #9a332a;
}

.location-panel {
  padding: 22rpx 40rpx 26rpx;
  border-top: 1rpx solid rgba(192, 182, 165, 0.2);
}

.location-modes {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  border: 1rpx solid rgba(166, 150, 124, 0.36);
}

.location-mode {
  min-width: 0;
  height: 56rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 21rpx;
  color: #6b6257;
  border-right: 1rpx solid rgba(166, 150, 124, 0.28);
}

.location-mode:last-child {
  border-right: 0;
}

.location-mode--active {
  color: #9a332a;
  background: rgba(181, 53, 42, 0.06);
}

.location-input {
  height: 64rpx;
  margin-top: 14rpx;
  padding: 0 14rpx;
  border-bottom: 1rpx solid rgba(166, 150, 124, 0.34);
  font-size: 23rpx;
  color: #4a4640;
}

:deep(.location-placeholder) {
  color: rgba(180, 170, 155, 0.76);
}

.location-save {
  height: 60rpx;
  margin-top: 20rpx;
  border: 1rpx solid rgba(181, 53, 42, 0.38);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22rpx;
  color: #9a332a;
}

.location-save--disabled {
  opacity: 0.6;
}

.image-panel {
  padding: 22rpx 40rpx 26rpx;
  border-top: 1rpx solid rgba(192, 182, 165, 0.2);
}

.image-panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16rpx;
}

.image-panel-title,
.image-panel-count {
  font-size: 20rpx;
  color: #847b70;
}

.image-panel-count {
  color: #a39a8e;
}

.image-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14rpx;
}

.image-tile {
  position: relative;
  aspect-ratio: 1;
  min-width: 0;
  overflow: hidden;
  border: 1rpx solid rgba(166, 150, 124, 0.3);
  background: rgba(244, 239, 229, 0.8);
}

.image-thumb {
  width: 100%;
  height: 100%;
  display: block;
}

.image-access-failed,
.image-upload-mask {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6rpx;
  padding: 12rpx;
  text-align: center;
  font-size: 19rpx;
  line-height: 1.4;
  color: #746c62;
  background: rgba(239, 233, 222, 0.92);
}

.image-upload-mask {
  color: #f8f4ec;
  background: rgba(48, 43, 38, 0.56);
}

.image-access-retry {
  color: #9a332a;
}

.image-delete,
.image-pending-remove {
  position: absolute;
  top: 8rpx;
  right: 8rpx;
  z-index: 2;
  width: 40rpx;
  height: 40rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  font-size: 30rpx;
  line-height: 1;
  color: #fff;
  background: rgba(45, 39, 34, 0.7);
}

.image-pending-actions {
  position: absolute;
  inset: 0;
  z-index: 3;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  padding: 0 10rpx 12rpx;
  background: rgba(48, 43, 38, 0.5);
}

.image-retry {
  min-width: 76rpx;
  height: 42rpx;
  padding: 0 12rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1rpx solid rgba(255, 255, 255, 0.7);
  font-size: 19rpx;
  color: #fff;
}

.image-upload-error {
  display: block;
  margin-top: 14rpx;
  font-size: 19rpx;
  line-height: 1.5;
  color: #9a332a;
  word-break: break-all;
}

.voice-panel {
  padding: 22rpx 40rpx 26rpx;
  border-top: 1rpx solid rgba(192, 182, 165, 0.2);
}

.voice-panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10rpx;
}

.voice-panel-title,
.voice-panel-count {
  font-size: 20rpx;
  color: #847b70;
}

.voice-panel-count {
  color: #a39a8e;
}

.voice-recording-row,
.voice-row {
  min-height: 78rpx;
  display: flex;
  align-items: center;
  gap: 16rpx;
  border-top: 1rpx solid rgba(166, 150, 124, 0.2);
}

.voice-recording-row {
  color: #9a332a;
}

.voice-recording-dot {
  width: 14rpx;
  height: 14rpx;
  flex-shrink: 0;
  border-radius: 50%;
  background: #b5352a;
}

.voice-recording-label {
  flex: 1;
  min-width: 0;
  font-size: 21rpx;
}

.voice-recording-time {
  flex-shrink: 0;
  font-size: 21rpx;
  font-variant-numeric: tabular-nums;
}

.voice-play {
  width: 52rpx;
  height: 52rpx;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1rpx solid rgba(154, 51, 42, 0.42);
  border-radius: 50%;
  font-size: 20rpx;
  color: #9a332a;
}

.voice-play--active {
  color: #fff;
  background: #9a332a;
}

.voice-play--loading {
  color: #7f756a;
  border-color: rgba(127, 117, 106, 0.4);
}

.voice-play--disabled {
  opacity: 0.42;
}

.voice-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4rpx;
  padding: 10rpx 0;
}

.voice-name {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 21rpx;
  color: #5f5850;
}

.voice-meta {
  font-size: 18rpx;
  color: #9e9890;
  font-variant-numeric: tabular-nums;
}

.voice-error,
.voice-upload-error {
  font-size: 18rpx;
  line-height: 1.45;
  color: #9a332a;
  word-break: break-all;
}

.voice-actions {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 14rpx;
}

.voice-action {
  padding: 10rpx 0;
  font-size: 19rpx;
  color: #8a625b;
}

.voice-delete {
  width: 40rpx;
  height: 40rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30rpx;
  line-height: 1;
  color: #9a332a;
}

.voice-row--pending {
  opacity: 0.82;
}

.voice-upload-error {
  display: block;
  margin-top: 12rpx;
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

.attach-item--active .attach-label {
  color: #9a332a;
}

.attach-item--disabled {
  opacity: 0.58;
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
  font-family: 'Noto Serif SC', 'Songti SC', Georgia, serif;
  font-size: 18rpx;
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
