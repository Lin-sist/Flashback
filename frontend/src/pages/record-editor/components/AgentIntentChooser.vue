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
      <view class="intent-handle" aria-hidden="true" />
      <view class="intent-head">
        <view>
          <text class="intent-kicker">这次想怎么聊</text>
          <text class="intent-title">由你决定它怎么在场</text>
        </view>
        <view class="intent-close" aria-label="关闭选择" @tap="emit('close')">×</view>
      </view>

      <view class="intent-options">
        <view class="intent-option" @tap="emit('choose', 'LISTEN')">
          <text class="option-title">就想说说</text>
          <text class="option-desc">它安静听着，不打断你</text>
        </view>
        <view class="intent-option" @tap="emit('choose', 'UNTANGLE')">
          <text class="option-title">理一理</text>
          <text class="option-desc">帮我把心里的事捋清楚</text>
        </view>
      </view>
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
  background: rgba(96, 89, 79, .42);
}
.intent-panel {
  position: relative;
  z-index: 1;
  width: 100%;
  box-sizing: border-box;
  padding: 20rpx 46rpx calc(64rpx + env(safe-area-inset-bottom));
  border-radius: 28rpx 28rpx 0 0;
  background: #f9f5ed;
  color: #403c37;
  font-family: 'Noto Serif SC', 'Songti SC', 'STSong', 'SimSun', serif;
  box-shadow: 0 -20rpx 60rpx rgba(80, 65, 45, .2);
}
.intent-handle {
  width: 70rpx;
  height: 8rpx;
  margin: 4rpx auto 0;
  border-radius: 4rpx;
  background: #c8c1b7;
}
.intent-head {
  margin-top: 56rpx;
  text-align: center;
}
.intent-kicker, .intent-title, .option-title, .option-desc {
  display: block;
}
.intent-kicker {
  color: #aba59d;
  font-size: 22rpx;
  letter-spacing: 8rpx;
  text-indent: 8rpx;
  line-height: 1.5;
}
.intent-title {
  margin-top: 26rpx;
  color: #403c37;
  font-size: 44rpx;
  line-height: 1.5;
  letter-spacing: 4rpx;
  text-indent: 4rpx;
}
.intent-close {
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
.intent-options {
  display: flex;
  gap: 24rpx;
  margin-top: 56rpx;
}
.intent-option {
  position: relative;
  flex: 1;
  min-width: 0;
  min-height: 224rpx;
  padding: 56rpx 18rpx 56rpx 44rpx;
  box-sizing: border-box;
  border: 1rpx solid #e3dacc;
  border-radius: 2rpx;
  background: #fbf8f1;
  box-shadow: 0 6rpx 20rpx rgba(125, 105, 75, .12);
}
.intent-option::before {
  content: '';
  position: absolute;
  left: 18rpx;
  top: 34rpx;
  bottom: 34rpx;
  width: 2rpx;
  background: linear-gradient(to bottom, transparent, rgba(181, 53, 42, .5) 15%, rgba(181, 53, 42, .5) 85%, transparent);
}
.option-title {
  color: #403c37;
  font-size: 32rpx;
  letter-spacing: 3rpx;
  line-height: 1.5;
}
.option-desc {
  margin-top: 22rpx;
  color: #8a847c;
  font-size: 22rpx;
  line-height: 1.75;
}
</style>
