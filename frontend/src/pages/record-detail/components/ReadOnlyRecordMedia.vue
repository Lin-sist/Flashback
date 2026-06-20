<script setup lang="ts">
import { computed, onUnmounted, reactive, ref, watch } from 'vue'
import { attachmentService } from '../../../services'
import type { RecordAttachmentVO } from '../../../types'
import { getToken, toUserMessage } from '../../../utils'

const props = withDefaults(defineProps<{
  recordId: number
  attachments?: RecordAttachmentVO[]
  coverId?: number | null
  variant?: 'sealed' | 'unlocked'
}>(), {
  attachments: () => [],
  coverId: null,
  variant: 'unlocked',
})

const imageAccessUrls = reactive<Record<number, string>>({})
const imageAccessErrors = reactive<Record<number, string>>({})
const voiceAccessErrors = reactive<Record<number, string>>({})
const playingVoiceId = ref<number | null>(null)
const voiceLoadingId = ref<number | null>(null)

const availableAttachments = computed(() => props.attachments.filter(
  (attachment) => attachment.status === 'AVAILABLE'
))
const imageAttachments = computed(() => availableAttachments.value.filter(
  (attachment) => attachment.type === 'IMAGE'
))
const voiceAttachments = computed(() => availableAttachments.value.filter(
  (attachment) => attachment.type === 'VOICE'
))

let imageLoadGeneration = 0
let voicePlaybackRequest = 0
let activeAudioContext: ReturnType<typeof uni.createInnerAudioContext> | null = null
let componentActive = true

const resolveAccessUrl = async (attachment: RecordAttachmentVO) => {
  if (!getToken()) {
    const embeddedUrl = attachment.accessUrl?.trim()
    if (embeddedUrl) return embeddedUrl
    throw new Error('媒体访问地址暂不可用')
  }
  const access = await attachmentService.createAccessUrl(props.recordId, attachment.id)
  return access.url
}

const loadImageAccessUrl = async (attachment: RecordAttachmentVO, generation: number) => {
  try {
    const url = await resolveAccessUrl(attachment)
    if (!componentActive || generation !== imageLoadGeneration) return undefined
    imageAccessUrls[attachment.id] = url
    delete imageAccessErrors[attachment.id]
    return url
  } catch (error) {
    if (!componentActive || generation !== imageLoadGeneration) return undefined
    delete imageAccessUrls[attachment.id]
    imageAccessErrors[attachment.id] = toUserMessage(error)
    return undefined
  }
}

const loadImages = async () => {
  imageLoadGeneration += 1
  const generation = imageLoadGeneration
  Object.keys(imageAccessUrls).forEach((key) => delete imageAccessUrls[Number(key)])
  Object.keys(imageAccessErrors).forEach((key) => delete imageAccessErrors[Number(key)])
  await Promise.allSettled(imageAttachments.value.map(
    (attachment) => loadImageAccessUrl(attachment, generation)
  ))
}

watch(
  () => [props.recordId, props.attachments] as const,
  () => { void loadImages() },
  { immediate: true, deep: true }
)

const previewImage = async (attachment: RecordAttachmentVO) => {
  imageLoadGeneration += 1
  const generation = imageLoadGeneration
  const refreshed = await Promise.allSettled(imageAttachments.value.map(
    (item) => loadImageAccessUrl(item, generation)
  ))
  const urls = refreshed
    .filter((result): result is PromiseFulfilledResult<string | undefined> => result.status === 'fulfilled')
    .map((result) => result.value)
    .filter((url): url is string => Boolean(url))
  const current = imageAccessUrls[attachment.id]
  if (!current || !urls.includes(current)) {
    uni.showToast({ title: imageAccessErrors[attachment.id] || '图片暂不可用', icon: 'none' })
    return
  }
  uni.previewImage({ current, urls })
}

const markImageFailed = (attachmentId: number) => {
  delete imageAccessUrls[attachmentId]
  imageAccessErrors[attachmentId] = '图片加载失败，点击重试'
}

const formatVoiceDuration = (durationSeconds?: number | null) => {
  const total = Math.max(0, Math.round(durationSeconds || 0))
  return `${String(Math.floor(total / 60)).padStart(2, '0')}:${String(total % 60).padStart(2, '0')}`
}

const stopVoice = () => {
  voicePlaybackRequest += 1
  voiceLoadingId.value = null
  if (!activeAudioContext) return
  const context = activeAudioContext
  activeAudioContext = null
  playingVoiceId.value = null
  context.stop()
  context.destroy()
}

const playVoice = async (attachment: RecordAttachmentVO) => {
  if (playingVoiceId.value === attachment.id && activeAudioContext) {
    stopVoice()
    return
  }

  stopVoice()
  const requestId = ++voicePlaybackRequest
  voiceLoadingId.value = attachment.id
  try {
    const url = await resolveAccessUrl(attachment)
    if (!componentActive || requestId !== voicePlaybackRequest) return
    delete voiceAccessErrors[attachment.id]
    const context = uni.createInnerAudioContext()
    activeAudioContext = context
    context.src = url
    context.onPlay(() => {
      voiceLoadingId.value = null
      playingVoiceId.value = attachment.id
    })
    const release = () => {
      if (activeAudioContext !== context) return
      activeAudioContext = null
      voiceLoadingId.value = null
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
    voiceLoadingId.value = null
    voiceAccessErrors[attachment.id] = toUserMessage(error)
    uni.showToast({ title: voiceAccessErrors[attachment.id], icon: 'none' })
  }
}

onUnmounted(() => {
  componentActive = false
  imageLoadGeneration += 1
  stopVoice()
})
</script>

<template>
  <view
    v-if="availableAttachments.length"
    class="readonly-media"
    :class="`readonly-media--${variant}`"
  >
    <view class="readonly-media-head">
      <text class="readonly-media-title">当时留下的片段</text>
      <text class="readonly-media-count">{{ availableAttachments.length }} 项</text>
    </view>

    <view v-if="imageAttachments.length" class="readonly-image-grid">
      <view v-for="attachment in imageAttachments" :key="attachment.id" class="readonly-image-item">
        <image
          v-if="imageAccessUrls[attachment.id]"
          class="readonly-image"
          :src="imageAccessUrls[attachment.id]"
          mode="aspectFill"
          @tap="previewImage(attachment)"
          @error="markImageFailed(attachment.id)"
        />
        <view v-else class="readonly-image-fallback" @tap="previewImage(attachment)">
          <view class="readonly-image-icon" aria-hidden="true" />
          <text v-if="imageAccessErrors[attachment.id]" class="readonly-media-error">图片暂不可用</text>
        </view>
        <text v-if="coverId === attachment.id" class="readonly-cover-label">封面</text>
      </view>
    </view>

    <view v-if="voiceAttachments.length" class="readonly-voice-list">
      <view v-for="(attachment, index) in voiceAttachments" :key="attachment.id" class="readonly-voice-row">
        <view
          class="readonly-voice-play"
          :class="{
            'readonly-voice-play--active': playingVoiceId === attachment.id,
            'readonly-voice-play--loading': voiceLoadingId === attachment.id,
          }"
          :aria-label="playingVoiceId === attachment.id
            ? '停止播放'
            : voiceLoadingId === attachment.id
              ? '正在加载语音'
              : '播放语音'"
          @tap="playVoice(attachment)"
        >
          {{ playingVoiceId === attachment.id ? '■' : voiceLoadingId === attachment.id ? '…' : '▶' }}
        </view>
        <view class="readonly-voice-info">
          <text class="readonly-voice-name">语音记录 {{ index + 1 }}</text>
          <text class="readonly-voice-duration">{{ formatVoiceDuration(attachment.durationSeconds) }}</text>
          <text v-if="voiceAccessErrors[attachment.id]" class="readonly-media-error">
            {{ voiceAccessErrors[attachment.id] }}
          </text>
        </view>
      </view>
    </view>
  </view>
</template>

<style scoped>
.readonly-media {
  width: 100%;
  margin-top: 32rpx;
  padding: 28rpx 4rpx;
  border-top: 1rpx solid rgba(188, 174, 152, 0.24);
  border-bottom: 1rpx solid rgba(188, 174, 152, 0.24);
}

.readonly-media--sealed {
  margin-top: 0;
  margin-bottom: 48rpx;
}

.readonly-media-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18rpx;
}

.readonly-media-title,
.readonly-media-count {
  font-family: var(--font-secondary);
  font-size: 20rpx;
  color: var(--ink-faint);
}

.readonly-image-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12rpx;
}

.readonly-image-item {
  position: relative;
  aspect-ratio: 1;
  min-width: 0;
  overflow: hidden;
  background: rgba(232, 224, 213, 0.72);
}

.readonly-image,
.readonly-image-fallback {
  width: 100%;
  height: 100%;
}

.readonly-image {
  display: block;
}

.readonly-image-fallback {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  padding: 10rpx;
}

.readonly-image-icon {
  width: 58rpx;
  height: 44rpx;
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 48 36' fill='none'><path d='M0 28L12 14L20 22L30 8L48 28' stroke='%239e9890' stroke-width='1' fill='none'/><ellipse cx='38' cy='8' rx='5' ry='5' stroke='%239e9890' stroke-width='1'/></svg>");
  background-repeat: no-repeat;
  background-position: center;
  background-size: contain;
  opacity: 0.32;
}

.readonly-cover-label {
  position: absolute;
  top: 8rpx;
  left: 8rpx;
  height: 32rpx;
  padding: 0 9rpx;
  display: flex;
  align-items: center;
  font-size: 16rpx;
  color: #fff;
  background: rgba(154, 51, 42, 0.84);
}

.readonly-voice-list {
  margin-top: 18rpx;
}

.readonly-voice-row {
  min-height: 78rpx;
  display: flex;
  align-items: center;
  gap: 16rpx;
  border-top: 1rpx solid rgba(188, 174, 152, 0.2);
}

.readonly-voice-play {
  width: 50rpx;
  height: 50rpx;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1rpx solid rgba(154, 51, 42, 0.4);
  border-radius: 50%;
  font-size: 19rpx;
  color: #9a332a;
}

.readonly-voice-play--active {
  color: #fff;
  background: #9a332a;
}

.readonly-voice-play--loading {
  color: var(--ink-light);
  border-color: var(--ink-faint);
}

.readonly-voice-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4rpx;
  padding: 10rpx 0;
}

.readonly-voice-name {
  font-family: var(--font-reading);
  font-size: 22rpx;
  color: var(--ink-mid);
}

.readonly-voice-duration,
.readonly-media-error {
  font-family: var(--font-secondary);
  font-size: 18rpx;
  color: var(--ink-faint);
}

.readonly-media-error {
  line-height: 1.4;
  color: #9a332a;
  word-break: break-all;
  text-align: center;
}
</style>
