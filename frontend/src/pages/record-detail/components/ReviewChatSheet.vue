<script setup lang="ts">
/**
 * 友人回看对话浮层（C3b agent-review-chat）。
 *
 * 刻意**不复用** `record-editor` 的 AgentChatSheet：那个组件与工具确认、
 * 素材回填深度耦合（pendingToolCall / confirmToolCall / useMaterial / discardMaterial），
 * 而回看完全无工具、不产可回填素材。复用会带进三块永远走不到的死逻辑，
 * 也会让「回看不能改动这条记录」这件事从结构保证退化成运行时判断。
 *
 * 展示映射 demo 回看纸条；会话、来源与记忆授权仍沿用既有契约。
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
          <text class="head-kicker">回看对话</text>
          <text class="head-title">和它聊聊那时候</text>
        </view>
        <view class="close-btn" aria-label="关闭对话" @tap="emit('close')">×</view>
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
            <view v-if="assistantSources(message).length" class="source-region">
              <text class="source-kicker">{{ sourceKicker(assistantSources(message)) }}</text>
              <view class="source-chips">
                <view
                  v-for="(source, sourceIndex) in assistantSources(message)"
                  :key="`${message.id}-${source.sourceKind}-${source.recordId || 'gone'}-${sourceIndex}`"
                  class="source-chip"
                  :class="{ 'source-chip--unavailable': !source.available }"
                  @tap.stop="openSource(source)"
                ><view class="source-dot" aria-hidden="true" /><text class="source-label">来源 · {{ sourceLabel(source) }}</text></view>
              </view>
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

      <view
        v-if="session && !isEnded"
        class="memory-consent"
        :class="{ 'memory-consent--disabled': switchingMemoryAuthorization }"
        role="switch"
        :aria-checked="memoryEnabled"
        :aria-label="`本次可参考过去记录。${memoryHint}`"
        @tap="toggleMemoryAuthorization"
      >
        <text class="memory-consent-label">本次可参考过去记录</text>
        <view class="memory-switch" :class="{ 'memory-switch--on': memoryEnabled }">
          <view class="memory-switch-knob" />
        </view>
        <text class="memory-consent-hint">{{ memoryHint }}</text>
      </view>

      <view v-if="!isEnded" class="composer">
        <!-- cursor-spacing 让键盘弹起时输入框不被遮住。 -->
        <view class="composer-input-bar">
          <textarea
            v-model="input"
            class="composer-input"
            placeholder="写下你想说的…"
            placeholder-class="composer-placeholder"
            :maxlength="500"
            :disabled="sending"
            :cursor-spacing="24"
            :show-confirm-bar="false"
            auto-height
          />
        </view>
        <view
          class="composer-send"
          :class="{ 'composer-send--disabled': !canSend }"
          role="button"
          :aria-disabled="!canSend"
          :aria-label="sending ? '正在发送' : '发送'"
          @tap="submit"
        >
          <text v-if="sending" class="sending-mark">…</text>
          <view v-else class="send-plane" aria-hidden="true">
            <view class="plane-line plane-line--top" />
            <view class="plane-line plane-line--right" />
            <view class="plane-line plane-line--tail" />
            <view class="plane-line plane-line--left" />
            <view class="plane-line plane-line--fold" />
          </view>
        </view>
      </view>
      <view class="privacy-note">对话不会改动这条记录</view>
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
  background: rgba(96, 89, 79, 0.42);
}

.review-sheet {
  position: relative;
  z-index: 1;
  width: 100%;
  height: 72vh;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  padding: 20rpx 46rpx calc(16rpx + env(safe-area-inset-bottom));
  background: #f9f5ed;
  border-radius: 28rpx 28rpx 0 0;
  box-shadow: 0 -20rpx 60rpx rgba(80, 65, 45, 0.2);
  font-family: 'Noto Serif SC', 'Songti SC', 'STSong', 'SimSun', serif;
}

.sheet-handle {
  flex-shrink: 0;
  width: 70rpx;
  height: 8rpx;
  margin: 4rpx auto 0;
  border-radius: 8rpx;
  background: #c8c1b7;
}

.sheet-head {
  flex-shrink: 0;
  margin-top: 56rpx;
  margin-bottom: 8rpx;
  text-align: center;
}

.head-copy {
  display: flex;
  flex-direction: column;
  gap: 26rpx;
}

.head-kicker {
  font-size: 22rpx;
  letter-spacing: 8rpx;
  text-indent: 8rpx;
  line-height: 1.5;
  color: #aba59d;
}

.head-title {
  font-size: 44rpx;
  color: #403c37;
  letter-spacing: 4rpx;
  text-indent: 4rpx;
  line-height: 1.5;
}

.close-btn {
  position: absolute;
  top: 28rpx;
  right: 24rpx;
  width: 72rpx;
  height: 72rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: sans-serif;
  font-size: 44rpx;
  font-weight: 300;
  line-height: 1;
  color: #6b6560;
}

.privacy-note {
  flex-shrink: 0;
  margin-top: 10rpx;
  font-size: 19rpx;
  line-height: 1.5;
  text-align: center;
  color: #8a847c;
}

.memory-consent {
  flex-shrink: 0;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6rpx 18rpx;
  margin-top: 8rpx;
  padding: 12rpx 0;
}

.memory-consent--disabled {
  opacity: 0.55;
}

.memory-consent-label {
  color: #8a847c;
  font-size: 21rpx;
  line-height: 1.6;
}

.memory-consent-hint {
  width: 100%;
  color: #8a847c;
  font-size: 20rpx;
  line-height: 1.5;
}

.memory-switch {
  width: 64rpx;
  height: 36rpx;
  flex-shrink: 0;
  border-radius: 999rpx;
  background: rgba(102, 85, 65, 0.16);
  position: relative;
}

.memory-switch--on {
  background: #b5352a;
}

.memory-switch-knob {
  width: 28rpx;
  height: 28rpx;
  border-radius: 50%;
  background: #fffaf3;
  position: absolute;
  top: 4rpx;
  left: 4rpx;
}

.memory-switch--on .memory-switch-knob {
  left: 32rpx;
}

.source-region {
  margin-top: 20rpx;
}

.source-kicker {
  display: block;
  margin-bottom: 10rpx;
  color: #8a847c;
  font-size: 20rpx;
}

.source-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8rpx;
}

.source-chip {
  display: flex;
  align-items: center;
  gap: 12rpx;
  max-width: 100%;
  box-sizing: border-box;
  padding: 8rpx 16rpx;
  border-radius: 999rpx;
  border: 1rpx solid #c8c1b7;
  background: transparent;
  color: #8a847c;
  font-size: 20rpx;
  line-height: 1.7;
}

.source-dot {
  flex-shrink: 0;
  width: 8rpx;
  height: 8rpx;
  border-radius: 50%;
  background: #b5352a;
}

.source-label {
  min-width: 0;
  overflow-wrap: anywhere;
}

.source-chip--unavailable {
  opacity: 0.65;
}

.source-chip--unavailable .source-dot {
  background: #aba59d;
}

.message-list {
  flex: 1 1 auto;
  min-height: 0;
  height: 0;
  padding: 32rpx 2rpx 0;
  box-sizing: border-box;
}

.message-row {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  margin-bottom: 40rpx;
}

.message-row--user {
  align-items: flex-end;
}

.message-row--assistant {
  justify-content: flex-start;
}

.message-bubble {
  position: relative;
  width: 84%;
  max-width: 100%;
  box-sizing: border-box;
  padding: 32rpx 36rpx;
  border: 1rpx solid #e3dacc;
  border-radius: 2rpx;
  background: #fbf8f1;
  box-shadow: 0 4rpx 16rpx rgba(125, 105, 75, 0.1);
}

.message-bubble::after {
  content: '';
  position: absolute;
  top: -1rpx;
  right: -1rpx;
  width: 28rpx;
  height: 28rpx;
  background: linear-gradient(to bottom left, #f9f5ed 0%, #f9f5ed 50%, #e5dcc9 50.5%, #e5dcc9 100%);
  box-shadow: -3rpx 3rpx 5rpx rgba(120, 100, 70, 0.16);
  pointer-events: none;
}

.message-row--user .message-bubble {
  width: 80%;
  padding: 26rpx 32rpx;
  background: #f6f1e6;
}

.message-text {
  font-size: 28rpx;
  line-height: 1.85;
  letter-spacing: 1rpx;
  color: #8a847c;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.message-bubble--thinking {
  display: flex;
  gap: 8rpx;
  width: auto;
  padding: 14rpx 28rpx;
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
  color: #8a847c;
}

.error-card {
  padding: 20rpx 22rpx;
  border-radius: 2rpx;
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
  padding: 12rpx 0;
}

.message-bottom-space {
  height: 8rpx;
}

.composer {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding-top: 4rpx;
}

.composer-input-bar {
  flex: 1;
  min-width: 0;
  box-sizing: border-box;
  min-height: 96rpx;
  display: flex;
  align-items: center;
  padding: 18rpx 36rpx;
  border: 2rpx solid rgba(255, 255, 255, 0.86);
  border-radius: 50rpx;
  background: #fcfaf6;
  box-shadow: inset 0 2rpx 0 rgba(255, 255, 255, 0.8);
}

.composer-input {
  flex: 1;
  width: 0;
  min-width: 0;
  min-height: 48rpx;
  max-height: 144rpx;
  padding: 0;
  font-size: 26rpx;
  line-height: 1.85;
  color: #6b6560;
  background: transparent;
}

.composer-placeholder {
  color: #aba59d;
}

.composer-send {
  flex-shrink: 0;
  width: 88rpx;
  height: 88rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  font-size: 26rpx;
  color: #6b6560;
  background: #fcfaf6;
  border: 2rpx solid rgba(255, 255, 255, 0.86);
  box-shadow: inset 0 2rpx 0 rgba(255, 255, 255, 0.8);
  border-radius: 50%;
}

.composer-send--disabled {
  opacity: 0.6;
}

/* 与书写浮层一致的原生细线飞机，不依赖内联 SVG 图像。 */
.send-plane { position: relative; width: 32rpx; height: 32rpx; }
.plane-line {
  position: absolute;
  height: 0;
  border-top: 2rpx solid #6b6560;
  transform-origin: 0 0;
}
.plane-line--top { left: 3.6rpx; top: 12.8rpx; width: 26.45rpx; transform: rotate(-20.35deg); }
.plane-line--right { left: 28.4rpx; top: 3.6rpx; width: 26.45rpx; transform: rotate(110.35deg); }
.plane-line--tail { left: 19.2rpx; top: 28.4rpx; width: 11.63rpx; transform: rotate(-116.57deg); }
.plane-line--left { left: 14rpx; top: 18rpx; width: 11.63rpx; transform: rotate(-153.43deg); }
.plane-line--fold { left: 14rpx; top: 18rpx; width: 20.36rpx; transform: rotate(-45deg); }
</style>
