<script setup lang="ts">
type TabKey = 'home' | 'timeline' | 'user-center'

interface BottomNavItem {
  key: TabKey
  label: string
  path: string
}

const props = defineProps<{
  current: TabKey
}>()

const navItems: BottomNavItem[] = [
  { key: 'home', label: '首页', path: '/pages/home/index' },
  { key: 'timeline', label: '时间轴', path: '/pages/timeline/index' },
  { key: 'user-center', label: '我的', path: '/pages/user-center/index' },
]

const switchTo = (item: BottomNavItem) => {
  if (item.key === props.current) return
  uni.switchTab({ url: item.path })
}
</script>

<template>
  <view class="bottom-nav-shell">
    <view class="bottom-nav">
      <!-- 首页 -->
      <view
        class="nav-item"
        :class="{ active: props.current === 'home' }"
        @tap="switchTo(navItems[0])"
      >
        <svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.85" stroke-linecap="round" stroke-linejoin="round">
          <path d="M3.5 11.2 12 4l8.5 7.2"/>
          <path d="M5.8 10.2v8.2a1.6 1.6 0 0 0 1.6 1.6h9.2a1.6 1.6 0 0 0 1.6-1.6v-8.2"/>
          <path d="M9.5 20v-5.2h5V20"/>
        </svg>
        <text class="nav-item-text">首页</text>
      </view>

      <!-- 时间轴 -->
      <view
        class="nav-item"
        :class="{ active: props.current === 'timeline' }"
        @tap="switchTo(navItems[1])"
      >
        <svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.85" stroke-linecap="round" stroke-linejoin="round">
          <path d="M4.5 5.6A2.6 2.6 0 0 1 7.1 3H11v16H7.1a2.6 2.6 0 0 0-2.6 2.6z"/>
          <path d="M19.5 5.6A2.6 2.6 0 0 0 16.9 3H13v16h3.9a2.6 2.6 0 0 1 2.6 2.6z"/>
          <path d="M11 6.2H8.2"/>
          <path d="M13 6.2h2.8"/>
        </svg>
        <text class="nav-item-text">时间轴</text>
      </view>

      <!-- 我的 -->
      <view
        class="nav-item"
        :class="{ active: props.current === 'user-center' }"
        @tap="switchTo(navItems[2])"
      >
        <svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.85" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="8.2" r="3.1"/>
          <path d="M5.4 20.2a6.6 4.3 0 0 1 13.2 0"/>
        </svg>
        <text class="nav-item-text">我的</text>
      </view>
    </view>
  </view>
</template>

<style scoped>
.bottom-nav-shell {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 80;
  padding: 14rpx 48rpx calc(env(safe-area-inset-bottom) + 10rpx);
  border-top: 1rpx solid var(--fb-bottom-nav-border-color);
  background: var(--fb-bottom-nav-bg);
  box-shadow: 0 -8rpx 24rpx rgba(48, 46, 41, 0.04);
}

.bottom-nav {
  height: 104rpx;
  display: flex;
  align-items: center;
}

.nav-item {
  flex: 1;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10rpx;
  color: var(--fb-bottom-nav-text-color);
}

.nav-item.active {
  color: var(--fb-bottom-nav-active-text-color);
}

.nav-icon {
  width: 42rpx;
  height: 42rpx;
}

.nav-item-text {
  font-size: 23rpx;
  line-height: 1;
  font-weight: 400;
}

.nav-item.active .nav-item-text {
  font-weight: 500;
}
</style>
