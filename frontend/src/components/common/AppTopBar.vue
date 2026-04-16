<script setup lang="ts">
const props = withDefaults(
  defineProps<{
    title: string
    showBack?: boolean
    showClose?: boolean
    rightText?: string
    leftText?: string
    transparent?: boolean
  }>(),
  {
    showBack: false,
    showClose: false,
    rightText: '',
    leftText: '',
    transparent: false,
  }
)

const emit = defineEmits<{
  (event: 'back'): void
  (event: 'close'): void
  (event: 'rightTap'): void
}>()

const onBack = () => emit('back')
const onClose = () => emit('close')
const onRightTap = () => emit('rightTap')
</script>

<template>
  <view class="top-bar" :class="{ transparent: props.transparent }">
    <view class="side left" @tap="onBack" v-if="props.showBack || props.leftText">
      <text v-if="props.showBack" class="icon">‹</text>
      <text v-else class="text">{{ props.leftText }}</text>
    </view>
    <view class="side left placeholder" v-else></view>

    <view class="title">{{ props.title }}</view>

    <view class="side right" v-if="props.showClose" @tap="onClose">
      <text class="icon">✕</text>
    </view>
    <view class="side right" v-else-if="props.rightText" @tap="onRightTap">
      <text class="text">{{ props.rightText }}</text>
    </view>
    <view class="side right placeholder" v-else></view>
  </view>
</template>

<style scoped>
.top-bar {
  height: 88rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.top-bar.transparent {
  background: transparent;
}

.side {
  width: 120rpx;
  min-height: 52rpx;
  display: flex;
  align-items: center;
}

.side.right {
  justify-content: flex-end;
}

.placeholder {
  opacity: 0;
}

.title {
  flex: 1;
  text-align: center;
  font-size: var(--fb-font-title-sub);
  color: var(--fb-color-text);
  font-weight: 600;
}

.icon {
  font-size: 40rpx;
  color: var(--fb-color-text);
  line-height: 1;
}

.text {
  font-size: var(--fb-font-body-sub);
  color: var(--fb-color-text-muted);
}
</style>
