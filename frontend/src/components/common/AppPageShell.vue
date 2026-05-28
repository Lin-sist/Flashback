<script setup lang="ts">
import { computed } from 'vue'
import AppTopSafeBar from './AppTopSafeBar.vue'
import BottomNavBar from './BottomNavBar.vue'
import { useWechatNavMetrics } from '../../composables/useWechatNavMetrics'

type TabKey = 'home' | 'timeline' | 'user-center'

const props = withDefaults(
  defineProps<{
    title?: string
    current?: TabKey
    topBarTransparent?: boolean
    paddingX?: string
    contentBottom?: string
    topSideWidth?: string
  }>(),
  {
    title: '',
    current: undefined,
    topBarTransparent: true,
    paddingX: '40rpx',
    contentBottom: '260rpx',
    topSideWidth: '88rpx',
  }
)

const { cssVars, navBarHeight, navBarTotalHeight, rightSafeWidth, statusBarHeight } =
  useWechatNavMetrics()

const shellStyle = computed(() => ({
  ...cssVars.value,
  '--app-shell-status-bar-height': `${statusBarHeight.value}px`,
  '--app-shell-nav-bar-height': `${navBarHeight.value}px`,
  '--app-shell-nav-total-height': `${navBarTotalHeight.value}px`,
  '--app-shell-right-safe-width': `${rightSafeWidth.value}px`,
}))

const bodyStyle = computed(() => ({
  paddingLeft: props.paddingX,
  paddingRight: props.paddingX,
  paddingBottom: props.contentBottom,
}))
</script>

<template>
  <view class="app-page-shell" :style="shellStyle">
    <slot name="background" />

    <AppTopSafeBar
      :title="props.title"
      :transparent="props.topBarTransparent"
      :padding-x="props.paddingX"
      :side-width="props.topSideWidth"
    >
      <template #left>
        <slot name="top-left" />
      </template>

      <template #title>
        <slot name="top-title">
          <text class="app-page-shell__title">{{ props.title }}</text>
        </slot>
      </template>

      <template #right>
        <slot name="top-right" />
      </template>
    </AppTopSafeBar>

    <view class="app-page-shell__body" :style="bodyStyle">
      <slot />
    </view>

    <slot name="floating" />

    <BottomNavBar v-if="props.current" :current="props.current || 'home'" />
  </view>
</template>

<style scoped>
.app-page-shell {
  position: relative;
  min-height: 100vh;
  background-color: var(--fb-paper);
}

.app-page-shell__body {
  position: relative;
  z-index: 1;
}

.app-page-shell__title {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  text-align: center;
  font-size: 34rpx;
  font-weight: 500;
  letter-spacing: 4rpx;
  color: var(--fb-vermilion);
  font-family: var(--fb-font-serif);
}
</style>
