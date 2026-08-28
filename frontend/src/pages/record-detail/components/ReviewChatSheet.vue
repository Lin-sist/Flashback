<script setup lang="ts">
/**
 * 友人回看对话浮层（C3b agent-review-chat）。
 *
 * 刻意**不复用** `record-editor` 的 AgentChatSheet：那个组件与工具确认、
 * 素材回填深度耦合（pendingToolCall / confirmToolCall / useMaterial / discardMaterial），
 * 而回看完全无工具、不产可回填素材。复用会带进三块永远走不到的死逻辑，
 * 也会让「回看不能改动这条记录」这件事从结构保证退化成运行时判断。
 *
 * 视觉沿用 record-detail 既有语言与 reply-overlay 的交互范式，不做视觉重建。
 */
import { computed, nextTick, ref, watch } from 'vue'
import type { AgentMemorySource, AgentMessage, AgentSession } from '../../../services'
import { formatYearMonth } from '../../../utils'

const props = defineProps<{
  visible: boolean
  session: AgentSession | null
  loading: boolean
  sending: boolean
  switchingMemoryAuthorization: boolean
  errorMessage: string
}>()

const emit = defineEmits<{
  close: []
  send: [content: string]
  retry: []
  switchMemoryAuthorization: [enabled: boolean]
}>()

const input = ref('')
const scrollAnchor = ref('')

const messages = computed<AgentMessage[]>(() => props.session?.messages || [])
const isEnded = computed(() => props.session?.sessionStatus === 'ENDED')
const canSend = computed(() => Boolean(
  input.value.trim() && !props.sending && !isEnded.value && !props.errorMessage,
))
const memoryEnabled = computed(() => props.session?.crossRecordMemoryEnabled === true)
const awaitingRetry = computed(() => Boolean(
  props.errorMessage && messages.value[messages.value.length - 1]?.role === 'USER',
))
const memoryHint = computed(() => memoryEnabled.value
  ? '只在这次对话里，另外参考你可见且未排除的其他记录'
  : '默认只看当前这条记录和这次对话')

const toggleMemoryAuthorization = () => {
  if (!props.session || props.switchingMemoryAuthorization || isEnded.value) return
  if (!memoryEnabled.value && awaitingRetry.value) return
  emit('switchMemoryAuthorization', !memoryEnabled.value)
}

const assistantSources = (message: AgentMessage): AgentMemorySource[] => {
  if (message.role !== 'ASSISTANT' || !message.memorySources?.length) return []
  return message.memorySources
}

const sourceKicker = (sources: AgentMemorySource[]) => {
  const hasCross = sources.some(source => source.sourceKind === 'CROSS_RECORD')
  return hasCross ? '另外参考了其他记录' : '正在回看这条记录'
}

const sourceLabel = (source: AgentMemorySource) => {
  if (!source.available) return '来源记录已删除或不可用'
  return source.displayTitle?.trim() || formatYearMonth(source.occurredAt) || '过去的记录'
}

const openSource = (source: AgentMemorySource) => {
  if (!source.available || !source.recordId) return
  uni.navigateTo({ url: `/pages/record-detail/index?id=${source.recordId}&source=archive` })
}

const messageKey = (message: AgentMessage, index: number) =>
  `${message.id || 'pending'}-${message.turnNo}-${message.role}-${index}`

const scrollToLatest = async () => {
  await nextTick()
  scrollAnchor.value = messages.value.length ? `review-message-${messages.value.length - 1}` : ''
}

watch(
  () => [props.visible, messages.value.length],
  ([visible]) => {
    if (visible) void scrollToLatest()
  },
)

const submit = () => {
  if (!canSend.value) return
  const content = input.value.trim()
  input.value = ''
  emit('send', content)
}
</script>

<template>
  <!--
    关闭手势挂在**独立的背景层**上，而不是挂在包住内容的外层容器上。
    原先写法是外层 `@tap="close"` + 内层 `@tap.stop`，在小程序里不可靠：
    textarea 是原生组件，它的触摸事件会穿透 `catchtap` 冒泡到外层，
    被当成「点击遮罩」而触发 close —— 输入框因此永远拿不到焦点。
    拆成兄弟层后，内容区与关闭手势没有祖先关系，穿透不再有影响。
  -->
  <view v-if="visible" class="review-layer">
    <view class="review-mask" @tap="emit('close')" />
    <view class="review-sheet">
      <view class="sheet-handle" aria-hidden="true" />
      <view class="sheet-head">
        <view class="head-copy">
          <text class="head-kicker">和它聊聊那时候</text>
          <text class="head-title">时间回看</text>
        </view>
        <view class="close-btn" aria-label="关闭对话" @tap="emit('close')">×</view>
      </view>

      <!-- 明写边界：回看不会动这条已经封存过的记录。 -->
      <view class="privacy-note">这段对话不会改动这条记录，只是陪你重新看看那时候。</view>

      <view
        v-if="session && !isEnded"
        class="memory-consent"
        :class="{ 'memory-consent--disabled': switchingMemoryAuthorization }"
        @tap="toggleMemoryAuthorization"
      >
        <view class="memory-consent-copy">
          <text class="memory-consent-label">本次可参考过去记录</text>
          <text class="memory-consent-hint">{{ memoryHint }}</text>
        </view>
        <view
          class="memory-switch"
          :class="{ 'memory-switch--on': memoryEnabled }"
        >
          <view class="memory-switch-knob" />
        </view>
      </view>

      <scroll-view
        class="message-list"
        scroll-y
        :scroll-into-view="scrollAnchor"
        :show-scrollbar="false"
      >
        <view v-if="loading && !messages.length" class="quiet-state">正在把这段对话取回来...</view>

        <view
          v-for="(message, index) in messages"
          :id="`review-message-${index}`"
          :key="messageKey(message, index)"
          class="message-row"
          :class="`message-row--${message.role.toLowerCase()}`"
        >
          <view class="message-bubble">
            <text class="message-text">{{ message.content }}</text>
          </view>
          <view v-if="assistantSources(message).length" class="source-region">
            <text class="source-kicker">{{ sourceKicker(assistantSources(message)) }}</text>
            <view class="source-chips">
              <view
                v-for="(source, sourceIndex) in assistantSources(message)"
                :key="`${message.id}-${source.sourceKind}-${source.recordId || 'gone'}-${sourceIndex}`"
                class="source-chip"
                :class="{ 'source-chip--unavailable': !source.available }"
                @tap.stop="openSource(source)"
              >{{ sourceLabel(source) }}</view>
            </view>
          </view>
        </view>

        <view v-if="sending" class="message-row message-row--assistant">
          <view class="message-bubble message-bubble--thinking">
            <text class="thinking-dot">·</text>
            <text class="thinking-dot thinking-dot--delay">·</text>
            <text class="thinking-dot thinking-dot--late">·</text>
          </view>
        </view>

        <!-- 失败与不可用都显式告知，不伪装成功。 -->
        <view v-if="errorMessage" class="error-card">
          <text class="error-text">{{ errorMessage }}</text>
          <view class="retry-link" @tap="emit('retry')">再试一次</view>
        </view>

        <view v-if="isEnded" class="quiet-state">这次就聊到这里。</view>

        <view class="message-bottom-space" />
      </scroll-view>

      <view v-if="!isEnded" class="composer">
        <!-- cursor-spacing 让键盘弹起时输入框不被遮住。 -->
        <textarea
          v-model="input"
          class="composer-input"
          placeholder="想说点什么就写下来"
          :maxlength="500"
          :disabled="sending"
          :cursor-spacing="24"
          :show-confirm-bar="false"
          auto-height
        />
        <view
          class="composer-send"
          :class="{ 'composer-send--disabled': !canSend }"
          @tap="submit"
        >{{ sending ? '…' : '说' }}</view>
      </view>
    </view>
  </view>
</template>

<style scoped>
.review-layer {
  position: fixed;
  inset: 0;
  z-index: 60;
  display: flex;
  align-items: flex-end;
}

/* 背景层独立成兄弟节点：承载遮罩视觉与关闭手势，不包裹内容。 */
.review-mask {
  position: absolute;
  inset: 0;
  z-index: 0;
  background: rgba(28, 25, 23, 0.42);
}

.review-sheet {
  position: relative;
  z-index: 1;
  width: 100%;
  max-height: 78vh;
  display: flex;
  flex-direction: column;
  padding: 16rpx 32rpx calc(24rpx + env(safe-area-inset-bottom));
  background: #fbf8f3;
  border-radius: 28rpx 28rpx 0 0;
  box-shadow: 0 -8rpx 32rpx rgba(28, 25, 23, 0.12);
}

.sheet-handle {
  width: 72rpx;
  height: 8rpx;
  margin: 0 auto 20rpx;
  border-radius: 8rpx;
  background: rgba(28, 25, 23, 0.14);
}

.sheet-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 12rpx;
}

.head-copy {
  display: flex;
  flex-direction: column;
}

.head-kicker {
  font-size: 22rpx;
  letter-spacing: 2rpx;
  color: rgba(28, 25, 23, 0.45);
}

.head-title {
  margin-top: 6rpx;
  font-size: 32rpx;
  color: rgba(28, 25, 23, 0.86);
}

.close-btn {
  width: 56rpx;
  height: 56rpx;
  line-height: 52rpx;
  text-align: center;
  font-size: 36rpx;
  color: rgba(28, 25, 23, 0.4);
}

.privacy-note {
  margin-bottom: 16rpx;
  font-size: 22rpx;
  line-height: 1.6;
  color: rgba(28, 25, 23, 0.4);
}

.memory-consent {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  margin-bottom: 16rpx;
}

.memory-consent--disabled {
  opacity: 0.55;
}

.memory-consent-copy {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}

.memory-consent-label {
  color: #5b4c40;
  font-size: 24rpx;
}

.memory-consent-hint {
  color: rgba(64, 55, 47, 0.5);
  font-size: 20rpx;
  line-height: 1.5;
}

.memory-switch {
  width: 72rpx;
  height: 40rpx;
  flex-shrink: 0;
  border-radius: 999rpx;
  background: rgba(102, 85, 65, 0.16);
  position: relative;
}

.memory-switch--on {
  background: #9c6447;
}

.memory-switch-knob {
  width: 32rpx;
  height: 32rpx;
  border-radius: 50%;
  background: #fffaf3;
  position: absolute;
  top: 4rpx;
  left: 4rpx;
}

.memory-switch--on .memory-switch-knob {
  left: 36rpx;
}

.source-region {
  margin: 8rpx 8rpx 4rpx;
}

.source-kicker {
  display: block;
  margin-bottom: 8rpx;
  color: rgba(64, 55, 47, 0.48);
  font-size: 20rpx;
}

.source-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8rpx;
}

.source-chip {
  padding: 8rpx 16rpx;
  border-radius: 999rpx;
  background: rgba(102, 85, 65, 0.08);
  color: #744c36;
  font-size: 20rpx;
}

.source-chip--unavailable {
  color: rgba(64, 55, 47, 0.42);
  background: rgba(102, 85, 65, 0.05);
}

.message-list {
  flex: 1;
  min-height: 320rpx;
  max-height: 52vh;
}

.message-row {
  display: flex;
  margin-bottom: 18rpx;
}

.message-row--user {
  justify-content: flex-end;
}

.message-row--assistant {
  justify-content: flex-start;
}

.message-bubble {
  max-width: 78%;
  padding: 18rpx 22rpx;
  border-radius: 20rpx;
  background: #ffffff;
  box-shadow: 0 2rpx 10rpx rgba(28, 25, 23, 0.05);
}

.message-row--user .message-bubble {
  background: rgba(163, 143, 118, 0.16);
}

.message-text {
  font-size: 27rpx;
  line-height: 1.7;
  color: rgba(28, 25, 23, 0.82);
}

.message-bubble--thinking {
  display: flex;
  gap: 8rpx;
}

.thinking-dot {
  font-size: 32rpx;
  color: rgba(28, 25, 23, 0.3);
}

.thinking-dot--delay {
  opacity: 0.7;
}

.thinking-dot--late {
  opacity: 0.45;
}

.quiet-state {
  padding: 24rpx 0;
  text-align: center;
  font-size: 24rpx;
  color: rgba(28, 25, 23, 0.38);
}

.error-card {
  padding: 20rpx 22rpx;
  border-radius: 18rpx;
  background: rgba(180, 120, 90, 0.1);
}

.error-text {
  font-size: 24rpx;
  line-height: 1.6;
  color: rgba(150, 90, 60, 0.9);
}

.retry-link {
  margin-top: 10rpx;
  font-size: 24rpx;
  color: rgba(120, 90, 60, 0.9);
}

.message-bottom-space {
  height: 20rpx;
}

.composer {
  display: flex;
  align-items: flex-end;
  gap: 16rpx;
  padding-top: 16rpx;
  border-top: 1rpx solid rgba(28, 25, 23, 0.08);
}

.composer-input {
  flex: 1;
  min-height: 64rpx;
  max-height: 200rpx;
  padding: 16rpx 20rpx;
  font-size: 27rpx;
  line-height: 1.6;
  color: rgba(28, 25, 23, 0.85);
  background: #ffffff;
  border-radius: 18rpx;
}

.composer-send {
  width: 88rpx;
  height: 64rpx;
  line-height: 64rpx;
  text-align: center;
  font-size: 26rpx;
  color: #fbf8f3;
  background: rgba(120, 100, 78, 0.92);
  border-radius: 18rpx;
}

.composer-send--disabled {
  background: rgba(28, 25, 23, 0.18);
}
</style>
