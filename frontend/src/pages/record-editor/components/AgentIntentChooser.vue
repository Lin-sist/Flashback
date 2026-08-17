<script setup lang="ts">
import type { AgentConversationIntent } from '../../../services'

defineProps<{
  visible: boolean
}>()

const emit = defineEmits<{
  close: []
  choose: [intent: AgentConversationIntent]
}>()
</script>

<template>
  <view v-if="visible" class="intent-layer">
    <view class="intent-mask" @tap="emit('close')" />
    <view class="intent-panel">
      <view class="intent-head">
        <view>
          <text class="intent-kicker">这次想怎么聊</text>
          <text class="intent-title">由你决定它怎么在场</text>
        </view>
        <view class="intent-close" aria-label="关闭选择" @tap="emit('close')">×</view>
      </view>

      <view class="intent-options">
        <view class="intent-option" @tap="emit('choose', 'LISTEN')">
          <text class="option-title">先听我说</text>
          <text class="option-desc">以听见和回应为主，不主动提问。</text>
        </view>
        <view class="intent-option" @tap="emit('choose', 'UNTANGLE')">
          <text class="option-title">帮我理一理</text>
          <text class="option-desc">先回应；需要时至多问一个可跳过的问题。</text>
        </view>
      </view>
      <text class="intent-note">两种方式没有高低，也不要求你得出结论。</text>
    </view>
  </view>
</template>

<style scoped>
.intent-layer {
  position: fixed;
  inset: 0;
  z-index: 1190;
  display: flex;
  align-items: flex-end;
}

.intent-mask {
  position: absolute;
  inset: 0;
  background: rgba(25, 22, 18, 0.32);
  backdrop-filter: blur(3px);
}

.intent-panel {
  position: relative;
  z-index: 1;
  width: 100%;
  padding: 34rpx 32rpx calc(38rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
  border-radius: 32rpx 32rpx 0 0;
  background: #f8f4ea;
  box-shadow: 0 -12rpx 40rpx rgba(45, 38, 29, 0.14);
}

.intent-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.intent-kicker,
.intent-title,
.option-title,
.option-desc,
.intent-note {
  display: block;
}

.intent-kicker {
  color: #9b7658;
  font-size: 22rpx;
  letter-spacing: 5rpx;
}

.intent-title {
  margin-top: 8rpx;
  color: #322b24;
  font-family: 'STKaiti', 'KaiTi', serif;
  font-size: 34rpx;
}

.intent-close {
  padding: 0 8rpx;
  color: rgba(50, 43, 36, 0.56);
  font-size: 46rpx;
}

.intent-options {
  display: flex;
  gap: 20rpx;
  margin-top: 30rpx;
}

.intent-option {
  flex: 1;
  min-height: 174rpx;
  padding: 26rpx 24rpx;
  box-sizing: border-box;
  border: 1rpx solid rgba(151, 101, 72, 0.22);
  border-radius: 20rpx;
  background: rgba(255, 250, 242, 0.86);
}

.option-title {
  color: #55483c;
  font-size: 29rpx;
  font-family: 'STKaiti', 'KaiTi', serif;
}

.option-desc {
  margin-top: 14rpx;
  color: rgba(64, 55, 47, 0.62);
  font-size: 22rpx;
  line-height: 1.65;
}

.intent-note {
  margin-top: 20rpx;
  color: rgba(64, 55, 47, 0.5);
  font-size: 21rpx;
  line-height: 1.6;
  text-align: center;
}
</style>
