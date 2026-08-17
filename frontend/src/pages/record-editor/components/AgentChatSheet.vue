<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import type {
  AgentConversationIntent,
  AgentMessage,
  AgentSession,
  AgentToolDecision,
} from '../../../services'

const props = defineProps<{
  visible: boolean
  session: AgentSession | null
  loading: boolean
  sending: boolean
  finishing: boolean
  confirmingToolCall: boolean
  switchingIntent: boolean
  errorMessage: string
}>()

const emit = defineEmits<{
  close: []
  send: [content: string]
  finish: []
  retry: []
  useMaterial: [material: string]
  discardMaterial: []
  confirmToolCall: [decision: AgentToolDecision]
  switchIntent: [intent: AgentConversationIntent]
}>()

const input = ref('')
const scrollAnchor = ref('')

const messages = computed<AgentMessage[]>(() => props.session?.messages || [])
const material = computed(() => props.session?.materialDraft?.trim() || '')
const isEnded = computed(() => props.session?.sessionStatus === 'ENDED')
const awaitingRetry = computed(() => Boolean(
  props.errorMessage && messages.value[messages.value.length - 1]?.role === 'USER'
))
const canSend = computed(() => Boolean(input.value.trim())
  && !props.sending
  && !isEnded.value
  && !awaitingRetry.value)
const phaseText = computed(() => {
  if (!props.session) return '写下此刻'
  if (props.session.stage === 'CLOSING' || props.session.stage === 'ENDED') return '说到这里已经很好'
  return props.session.conversationIntent === 'UNTANGLE' ? '一起理一理' : '先听你说'
})

const currentIntent = computed<AgentConversationIntent>(() =>
  props.session?.conversationIntent === 'UNTANGLE' ? 'UNTANGLE' : 'LISTEN'
)

/** C2：仅待确认的提议才展示确认条。 */
const pendingToolCall = computed(() => {
  const pending = props.session?.pendingToolCall
  return pending && pending.status === 'PROPOSED' ? pending : null
})

/** C2：执行失败时给出明确原因，成功时给一句轻量确认。 */
const toolCallNotice = computed(() => {
  const result = props.session?.lastToolCallResult
  if (!result) return ''
  if (result.status === 'FAILED') return result.resultSummary || '这一步没有成功'
  if (result.status === 'EXECUTED') return result.resultSummary || '已经帮你做好了'
  return ''
})

const toolCallFailed = computed(() => props.session?.lastToolCallResult?.status === 'FAILED')

const messageKey = (message: AgentMessage, index: number) => `${message.id || 'pending'}-${message.turnNo}-${message.role}-${index}`

const scrollToLatest = async () => {
  await nextTick()
  scrollAnchor.value = messages.value.length ? `agent-message-${messages.value.length - 1}` : ''
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
  <view v-if="visible" class="agent-layer">
    <view class="agent-mask" @tap="emit('close')" />
    <view class="agent-sheet">
      <view class="sheet-handle" aria-hidden="true" />
      <view class="sheet-head">
        <view class="head-copy">
          <text class="head-kicker">和它聊一会儿</text>
          <text class="head-title">{{ phaseText }}</text>
        </view>
        <view class="close-btn" aria-label="关闭对话" @tap="emit('close')">×</view>
      </view>

      <view class="privacy-note">这里只陪你慢慢说，不会替你改写或做决定。</view>

      <view v-if="session && !isEnded" class="intent-switch" aria-label="切换对话方式">
        <view
          class="intent-switch-item"
          :class="{
            'intent-switch-item--active': currentIntent === 'LISTEN',
            'intent-switch-item--disabled': switchingIntent,
          }"
          @tap="emit('switchIntent', 'LISTEN')"
        >先听我说</view>
        <view
          class="intent-switch-item"
          :class="{
            'intent-switch-item--active': currentIntent === 'UNTANGLE',
            'intent-switch-item--disabled': switchingIntent,
          }"
          @tap="emit('switchIntent', 'UNTANGLE')"
        >{{ switchingIntent ? '正在切换...' : '帮我理一理' }}</view>
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
          :id="`agent-message-${index}`"
          :key="messageKey(message, index)"
          class="message-row"
          :class="`message-row--${message.role.toLowerCase()}`"
        >
          <view class="message-bubble">
            <text class="message-text">{{ message.content }}</text>
          </view>
        </view>

        <view v-if="sending" class="message-row message-row--assistant">
          <view class="message-bubble message-bubble--thinking">
            <text class="thinking-dot">·</text>
            <text class="thinking-dot thinking-dot--delay">·</text>
            <text class="thinking-dot thinking-dot--late">·</text>
          </view>
        </view>

        <view v-if="errorMessage" class="error-card">
          <text class="error-text">{{ errorMessage }}</text>
          <view class="retry-link" @tap="emit('retry')">再试一次</view>
        </view>

        <!--
          C2 工具提议确认条。保持克制：一行征询 + 两个选项，
          不做成待办清单（单轮至多一个提议）。
        -->
        <view v-if="pendingToolCall" class="tool-card">
          <text class="tool-ask">{{ pendingToolCall.askText || '要不要我帮你做这一步？' }}</text>
          <view class="tool-actions">
            <view
              class="tool-secondary"
              :class="{ 'tool-action--disabled': confirmingToolCall }"
              @tap="emit('confirmToolCall', 'REJECT')"
            >先不用</view>
            <view
              class="tool-primary"
              :class="{ 'tool-action--disabled': confirmingToolCall }"
              @tap="emit('confirmToolCall', 'ACCEPT')"
            >{{ confirmingToolCall ? '正在处理...' : '好' }}</view>
          </view>
        </view>

        <view
          v-else-if="toolCallNotice"
          class="tool-notice"
          :class="{ 'tool-notice--failed': toolCallFailed }"
        >
          <text class="tool-notice-text">{{ toolCallNotice }}</text>
        </view>

        <view v-if="material" class="material-card">
          <text class="material-label">从刚才的话里，留下这段素材</text>
          <text class="material-content">{{ material }}</text>
          <view class="material-actions">
            <view class="material-secondary" @tap="emit('discardMaterial')">先不用</view>
            <view class="material-primary" @tap="emit('useMaterial', material)">用作正文</view>
          </view>
        </view>

        <view class="message-bottom-space" />
      </scroll-view>

      <view v-if="!isEnded" class="composer">
        <!--
          cursor-spacing 让键盘弹起时输入框不被遮住（小程序 textarea 默认贴键盘顶边）。
          adjust-position 保持默认 true，由小程序自动上推页面。
        -->
        <textarea
          v-model="input"
          class="composer-input"
          auto-height
          :disabled="sending || loading || awaitingRetry"
          :maxlength="1000"
          :show-confirm-bar="false"
          :cursor-spacing="24"
          :placeholder="awaitingRetry ? '请先重试上一轮回复' : '想说什么都可以...'"
          placeholder-class="composer-placeholder"
          @confirm="submit"
        />
        <view class="composer-actions">
          <view class="finish-link" :class="{ 'finish-link--disabled': finishing }" @tap="emit('finish')">
            {{ finishing ? '正在收束...' : '先聊到这里' }}
          </view>
          <view class="send-btn" :class="{ 'send-btn--disabled': !canSend }" @tap="submit">发送</view>
        </view>
      </view>

      <view v-else-if="!material" class="ended-note">这次就聊到这里。你写下的内容仍然在这里。</view>
    </view>
  </view>
</template>

<style scoped>
.agent-layer {
  position: fixed;
  inset: 0;
  z-index: 1200;
  display: flex;
  align-items: flex-end;
}

/* 背景层独立成兄弟节点：承载遮罩视觉与关闭手势，不包裹内容。 */
.agent-mask {
  position: absolute;
  inset: 0;
  z-index: 0;
  background: rgba(25, 22, 18, 0.32);
  backdrop-filter: blur(3px);
}

/*
 * 高度固定而非 max-height：小程序里 scroll-view 需要确定高度才会内部滚动，
 * 否则消息累积后会撑开容器并把 composer 顶出可视区（发送按钮点不到）。
 */
.agent-sheet {
  position: relative;
  z-index: 1;
  width: 100%;
  height: 78vh;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  padding: 12rpx 32rpx calc(28rpx + env(safe-area-inset-bottom));
  border-radius: 32rpx 32rpx 0 0;
  background: #f8f4ea;
  box-shadow: 0 -12rpx 40rpx rgba(45, 38, 29, 0.14);
}

.sheet-handle {
  flex-shrink: 0;
  width: 64rpx;
  height: 6rpx;
  margin: 0 auto 22rpx;
  border-radius: 999rpx;
  background: rgba(86, 74, 58, 0.22);
}

/* 头部与底部输入区不参与压缩，只有消息区可伸缩。 */
.sheet-head {
  flex-shrink: 0;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.head-copy {
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}

.head-kicker {
  color: #9b7658;
  font-size: 22rpx;
  letter-spacing: 6rpx;
}

.head-title {
  color: #322b24;
  font-family: 'STKaiti', 'KaiTi', serif;
  font-size: 34rpx;
  line-height: 1.5;
}

.close-btn {
  width: 56rpx;
  height: 56rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(50, 43, 36, 0.56);
  font-size: 46rpx;
  line-height: 1;
}

.privacy-note {
  flex-shrink: 0;
  margin-top: 14rpx;
  padding-bottom: 18rpx;
  border-bottom: 1rpx solid rgba(111, 91, 67, 0.12);
  color: rgba(68, 58, 48, 0.54);
  font-size: 21rpx;
  line-height: 1.6;
}

.intent-switch {
  flex-shrink: 0;
  display: flex;
  gap: 10rpx;
  margin-top: 16rpx;
  padding: 6rpx;
  border-radius: 999rpx;
  background: rgba(102, 85, 65, 0.07);
}

.intent-switch-item {
  flex: 1;
  padding: 12rpx 14rpx;
  border-radius: 999rpx;
  color: rgba(64, 55, 47, 0.58);
  font-size: 22rpx;
  text-align: center;
}

.intent-switch-item--active {
  color: #744c36;
  background: rgba(255, 250, 242, 0.94);
  box-shadow: 0 2rpx 8rpx rgba(63, 52, 39, 0.07);
}

.intent-switch-item--disabled {
  opacity: 0.45;
}

/*
 * min-height: 0 是关键：flex item 默认 min-height:auto 不会收缩到内容高度以下，
 * 会导致 scroll-view 被内容撑开而不滚动，进而把 composer 顶出 sheet。
 */
.message-list {
  flex: 1 1 auto;
  min-height: 0;
  height: 0;
  padding: 24rpx 0;
  box-sizing: border-box;
}

.message-row {
  display: flex;
  margin: 0 0 20rpx;
}

.message-row--assistant {
  justify-content: flex-start;
}

.message-row--user {
  justify-content: flex-end;
}

.message-bubble {
  max-width: 78%;
  padding: 20rpx 24rpx;
  border-radius: 22rpx;
  background: rgba(255, 255, 255, 0.76);
  box-shadow: 0 4rpx 14rpx rgba(63, 52, 39, 0.05);
}

.message-row--assistant .message-bubble {
  border-bottom-left-radius: 6rpx;
}

.message-row--user .message-bubble {
  border-bottom-right-radius: 6rpx;
  background: #ede1d2;
}

.message-text {
  color: #40372f;
  font-size: 28rpx;
  line-height: 1.75;
  white-space: pre-wrap;
}

.message-bubble--thinking {
  display: flex;
  gap: 5rpx;
  padding: 14rpx 24rpx;
}

.thinking-dot {
  color: #99785c;
  font-size: 34rpx;
  opacity: 0.9;
}

.thinking-dot--delay {
  opacity: 0.6;
}

.thinking-dot--late {
  opacity: 0.35;
}

.ended-note {
  flex-shrink: 0;
}

.quiet-state,
.ended-note {
  padding: 40rpx 24rpx;
  color: rgba(64, 55, 47, 0.55);
  font-size: 24rpx;
  line-height: 1.7;
  text-align: center;
}

.error-card,
.material-card {
  margin: 8rpx 0 20rpx;
  padding: 24rpx;
  border: 1rpx solid rgba(151, 101, 72, 0.16);
  border-radius: 18rpx;
  background: rgba(255, 250, 242, 0.86);
}

.error-text,
.material-content {
  display: block;
  color: #55483c;
  font-size: 25rpx;
  line-height: 1.75;
  white-space: pre-wrap;
}

.retry-link {
  margin-top: 14rpx;
  color: #9d6649;
  font-size: 24rpx;
}

.material-label {
  display: block;
  margin-bottom: 12rpx;
  color: #9b7658;
  font-size: 21rpx;
  letter-spacing: 2rpx;
}

.material-actions {
  display: flex;
  justify-content: flex-end;
  gap: 18rpx;
  margin-top: 20rpx;
}

.material-secondary,
.material-primary {
  padding: 14rpx 24rpx;
  border-radius: 999rpx;
  font-size: 24rpx;
}

.material-secondary {
  color: rgba(64, 55, 47, 0.64);
  background: rgba(102, 85, 65, 0.07);
}

.material-primary {
  color: #fffaf3;
  background: #9c6447;
}

/* C2 工具提议确认条：与素材卡同一视觉家族，不引入新的视觉语言。 */
.tool-card {
  margin: 8rpx 0 20rpx;
  padding: 24rpx;
  border: 1rpx solid rgba(151, 101, 72, 0.16);
  border-radius: 18rpx;
  background: rgba(255, 250, 242, 0.86);
}

.tool-ask {
  display: block;
  color: #55483c;
  font-size: 26rpx;
  line-height: 1.7;
}

.tool-actions {
  display: flex;
  justify-content: flex-end;
  gap: 18rpx;
  margin-top: 20rpx;
}

.tool-secondary,
.tool-primary {
  padding: 14rpx 28rpx;
  border-radius: 999rpx;
  font-size: 24rpx;
}

.tool-secondary {
  color: rgba(64, 55, 47, 0.64);
  background: rgba(102, 85, 65, 0.07);
}

.tool-primary {
  color: #fffaf3;
  background: #9c6447;
}

.tool-action--disabled {
  opacity: 0.4;
}

.tool-notice {
  margin: 4rpx 0 18rpx;
  padding: 14rpx 20rpx;
  border-radius: 14rpx;
  background: rgba(102, 85, 65, 0.06);
}

.tool-notice--failed {
  background: rgba(157, 102, 73, 0.1);
}

.tool-notice-text {
  color: rgba(64, 55, 47, 0.66);
  font-size: 23rpx;
  line-height: 1.65;
}

.message-bottom-space {
  height: 8rpx;
}

.composer {
  flex-shrink: 0;
  padding-top: 20rpx;
  border-top: 1rpx solid rgba(111, 91, 67, 0.12);
  background: #f8f4ea;
}

.composer-input {
  width: 100%;
  min-height: 76rpx;
  max-height: 190rpx;
  box-sizing: border-box;
  padding: 16rpx 20rpx;
  border-radius: 18rpx;
  background: rgba(255, 255, 255, 0.7);
  color: #40372f;
  font-size: 27rpx;
  line-height: 1.65;
}

.composer-placeholder {
  color: rgba(64, 55, 47, 0.36);
}

.composer-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 14rpx;
}

.finish-link {
  padding: 12rpx 0;
  color: rgba(64, 55, 47, 0.58);
  font-size: 23rpx;
}

.finish-link--disabled {
  opacity: 0.4;
}

.send-btn {
  min-width: 112rpx;
  padding: 14rpx 26rpx;
  border-radius: 999rpx;
  background: #9c6447;
  color: #fffaf3;
  font-size: 25rpx;
  text-align: center;
}

.send-btn--disabled {
  opacity: 0.36;
}
</style>
