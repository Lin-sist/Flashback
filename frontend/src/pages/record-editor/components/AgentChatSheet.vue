<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import type {
  AgentConversationIntent,
  AgentMemorySource,
  AgentMessage,
  AgentSession,
  AgentToolDecision,
} from '../../../services'
import { formatYearMonth } from '../../../utils'

const props = defineProps<{
  visible: boolean
  session: AgentSession | null
  loading: boolean
  sending: boolean
  finishing: boolean
  confirmingToolCall: boolean
  switchingIntent: boolean
  switchingMemoryAuthorization: boolean
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
  switchMemoryAuthorization: [enabled: boolean]
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

const memoryEnabled = computed(() => props.session?.crossRecordMemoryEnabled === true)

const canEnableMemory = computed(() => !awaitingRetry.value || memoryEnabled.value)

const memoryHint = computed(() => memoryEnabled.value
  ? '只在这次对话里，参考你可见且未排除的过去记录'
  : '默认只看当前记录和这次对话')

const toggleMemoryAuthorization = () => {
  if (!props.session || props.switchingMemoryAuthorization || isEnded.value) return
  if (!memoryEnabled.value && !canEnableMemory.value) return
  emit('switchMemoryAuthorization', !memoryEnabled.value)
}

const assistantSources = (message: AgentMessage): AgentMemorySource[] => {
  if (message.role !== 'ASSISTANT' || !message.memorySources?.length) return []
  return message.memorySources
}

const sourceKicker = (sources: AgentMemorySource[]) => {
  const hasCross = sources.some(source => source.sourceKind === 'CROSS_RECORD')
  return hasCross ? '参考了过去的记录' : '正在回看这条记录'
}

const sourceLabel = (source: AgentMemorySource) => {
  if (!source.available) return '来源记录已删除或不可用'
  return source.displayTitle?.trim() || formatYearMonth(source.occurredAt) || '过去的记录'
}

const openSource = (source: AgentMemorySource) => {
  if (!source.available || !source.recordId) return
  uni.navigateTo({ url: `/pages/record-detail/index?id=${source.recordId}&source=archive` })
}

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
          <text class="head-kicker">时光的见证者</text>
          <text class="head-title">{{ session?.stage === 'CLOSING' || isEnded ? phaseText : '把此刻说给它听' }}</text>
        </view>
        <view class="close-btn" aria-label="关闭对话" @tap="emit('close')">×</view>
      </view>

      <view v-if="session && !isEnded" class="intent-switch" aria-label="切换对话方式">
        <view
          class="intent-switch-item"
          :class="{
            'intent-switch-item--active': currentIntent === 'LISTEN',
            'intent-switch-item--disabled': switchingIntent,
          }"
          @tap="emit('switchIntent', 'LISTEN')"
        >倾诉</view>
        <view class="intent-divider" aria-hidden="true" />
        <view
          class="intent-switch-item"
          :class="{
            'intent-switch-item--active': currentIntent === 'UNTANGLE',
            'intent-switch-item--disabled': switchingIntent,
          }"
          @tap="emit('switchIntent', 'UNTANGLE')"
        >{{ switchingIntent ? '正在切换...' : '理一理' }}</view>
      </view>

      <scroll-view
        class="message-list"
        scroll-y
        :scroll-into-view="scrollAnchor"
        :show-scrollbar="false"
      >
        <view class="privacy-note">这里只陪你慢慢说，不会替你改写或做决定。</view>

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
          <view
            v-if="assistantSources(message).length"
            class="source-region"
          >
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
          :class="{
            'memory-switch--on': memoryEnabled,
            'memory-switch--busy': switchingMemoryAuthorization,
          }"
        >
          <view class="memory-switch-knob" />
        </view>
      </view>

      <view v-if="!isEnded" class="composer">
        <view class="composer-meta">
          <text class="composer-kicker">供它回看时参考</text>
          <view class="finish-link" :class="{ 'finish-link--disabled': finishing }" @tap="emit('finish')">
            {{ finishing ? '正在收束...' : '先聊到这里' }}
          </view>
        </view>
        <view class="composer-bar">
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
            :placeholder="awaitingRetry ? '请先重试上一轮回复' : '写点什么…'"
            placeholder-class="composer-placeholder"
            @confirm="submit"
          />
          <view class="send-btn" aria-label="发送" :aria-disabled="!canSend" :class="{ 'send-btn--disabled': !canSend }" @tap="submit">
            <image class="send-icon" aria-hidden="true" src="data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='16' height='16' viewBox='0 0 16 16' fill='none' stroke='%236b6560' stroke-width='1.1' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M14.2 1.8L7 9M14.2 1.8L9.6 14.2 7 9 1.8 6.4z'/%3E%3C/svg%3E" />
          </view>
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
  background: rgba(96, 89, 79, 0.42);
}

/*
 * 高度固定而非 max-height：小程序里 scroll-view 需要确定高度才会内部滚动，
 * 否则消息累积后会撑开容器并把 composer 顶出可视区（发送按钮点不到）。
 */
.agent-sheet {
  position: relative;
  z-index: 1;
  width: 100%;
  height: 72vh;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  padding: 20rpx 46rpx calc(24rpx + env(safe-area-inset-bottom));
  border-radius: 28rpx 28rpx 0 0;
  background: #f9f5ed;
  box-shadow: 0 -20rpx 60rpx rgba(80, 65, 45, .2);
  font-family: 'Noto Serif SC', 'Songti SC', 'STSong', 'SimSun', serif;
}

.sheet-handle {
  flex-shrink: 0;
  width: 70rpx;
  height: 8rpx;
  margin: 4rpx auto 0;
  border-radius: 4rpx;
  background: #c8c1b7;
}

/* 头部与底部输入区不参与压缩，只有消息区可伸缩。 */
.sheet-head {
  flex-shrink: 0;
  margin-top: 56rpx;
  text-align: center;
}

.head-copy {
  display: flex;
  flex-direction: column;
  gap: 26rpx;
}

.head-kicker {
  color: #aba59d;
  font-size: 22rpx;
  letter-spacing: 8rpx;
  text-indent: 8rpx;
  line-height: 1.5;
}

.head-title {
  color: #403c37;
  font-size: 44rpx;
  line-height: 1.5;
  letter-spacing: 4rpx;
  text-indent: 4rpx;
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
  color: #6b6560;
  font-family: sans-serif;
  font-size: 44rpx;
  font-weight: 300;
  line-height: 1;
}

.privacy-note {
  margin: 0 0 20rpx;
  color: #8a847c;
  font-size: 21rpx;
  line-height: 1.7;
  text-align: center;
}

.memory-consent {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  margin-top: 12rpx;
  padding: 12rpx 0;
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
  color: #6b6560;
  font-size: 22rpx;
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
  background: #b5352a;
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
  max-width: 82%;
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

.intent-switch {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 44rpx;
  margin-top: 36rpx;
}

.intent-switch-item {
  position: relative;
  padding: 12rpx 6rpx 16rpx;
  color: #aba59d;
  font-size: 28rpx;
  letter-spacing: 4rpx;
  text-align: center;
}

.intent-switch-item--active {
  color: #403c37;
}

.intent-divider {
  width: 1rpx;
  height: 28rpx;
  background: #c8c1b7;
}
.intent-switch-item--active::after {
  content: '';
  position: absolute;
  left: 12%;
  right: 12%;
  bottom: 0;
  height: 2rpx;
  background: #b5352a;
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
  padding: 24rpx 2rpx 0;
  box-sizing: border-box;
}

.message-row {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  margin: 0 0 40rpx;
}

.message-row--assistant {
  justify-content: flex-start;
}

.message-row--user {
  align-items: flex-end;
}

.message-bubble {
  width: 88%;
  max-width: 100%;
  box-sizing: border-box;
  padding: 30rpx 36rpx;
  border: 1rpx solid #e3dacc;
  border-radius: 2rpx;
  background: #fbf8f1;
  box-shadow: 0 4rpx 16rpx rgba(125, 105, 75, .1);
}

.message-row--assistant .message-bubble {
  color: #8a847c;
}

.message-row--user .message-bubble {
  width: 82%;
  padding: 24rpx 30rpx;
  border-color: rgba(107, 101, 96, .45);
  background: transparent;
  box-shadow: none;
}

.message-text {
  color: #8a847c;
  font-size: 28rpx;
  line-height: 1.85;
  letter-spacing: 1rpx;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
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
  border-radius: 2rpx;
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
  border-radius: 2rpx;
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
  padding-top: 4rpx;
  background: #f9f5ed;
}

.composer-input {
  flex: 1;
  width: 0;
  min-width: 0;
  min-height: 48rpx;
  max-height: 144rpx;
  box-sizing: border-box;
  padding: 0;
  background: transparent;
  color: #6b6560;
  font-size: 26rpx;
  line-height: 1.85;
}

.composer-placeholder {
  color: rgba(64, 55, 47, 0.36);
}

.finish-link {
  padding: 12rpx 0 12rpx 20rpx;
  color: #8a847c;
  font-size: 21rpx;
}

.finish-link--disabled {
  opacity: 0.4;
}

.send-btn {
  flex-shrink: 0;
  width: 80rpx;
  height: 80rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.composer-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}
.composer-kicker {
  color: #aba59d;
  font-size: 21rpx;
}
.composer-bar {
  display: flex;
  align-items: center;
  gap: 16rpx;
  min-height: 96rpx;
  padding: 6rpx 10rpx 6rpx 36rpx;
  box-sizing: border-box;
  border-radius: 50rpx;
  border: 2rpx solid rgba(255, 255, 255, .86);
  background: #fcfaf6;
  box-shadow: inset 0 2rpx 0 rgba(255, 255, 255, .8);
}
.send-icon {
  width: 60rpx;
  height: 60rpx;
  padding: 15rpx;
  box-sizing: border-box;
  border: 1rpx solid rgba(107, 101, 96, .5);
  border-radius: 50%;
}

.send-btn--disabled {
  opacity: 0.36;
}
</style>
