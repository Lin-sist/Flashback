<script setup lang="ts">
const props = withDefaults(
  defineProps<{
    warm?: boolean
    radius?: 'xl' | 'lg' | 'md' | 'sm'
    noPadding?: boolean
  }>(),
  {
    warm: false,
    radius: 'xl',
    noPadding: false,
  }
)
</script>

<template>
  <view
    class="paper"
    :class="[
      { warm: props.warm, 'no-padding': props.noPadding },
      `radius-${props.radius}`,
    ]"
  >
    <slot />
  </view>
</template>

<style scoped>
.paper {
  background: #ffffff;
  box-shadow: 0 8rpx 24rpx rgba(26, 26, 26, 0.06);
  padding: 32rpx;
}

.paper.warm {
  background: #fffdf5;
}

.paper.no-padding {
  padding: 0;
}

.paper.radius-xl {
  border-radius: 32rpx;
}

.paper.radius-lg {
  border-radius: 28rpx;
}

.paper.radius-md {
  border-radius: 24rpx;
}

.paper.radius-sm {
  border-radius: 8rpx;
}
</style>

