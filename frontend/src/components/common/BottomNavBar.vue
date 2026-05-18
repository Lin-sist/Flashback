<script setup lang="ts">
type TabKey = 'home' | 'timeline' | 'user-center'

interface BottomNavItem {
  key: TabKey
  label: string
  path: string
  iconClass: string
}

const props = defineProps<{
  current: TabKey
}>()

const navItems: BottomNavItem[] = [
  { key: 'home', label: '首页', path: '/pages/home/index', iconClass: 'nav-icon-home' },
  { key: 'timeline', label: '时间轴', path: '/pages/timeline/index', iconClass: 'nav-icon-book' },
  { key: 'user-center', label: '我的', path: '/pages/user-center/index', iconClass: 'nav-icon-user' },
]

const switchTo = (item: BottomNavItem) => {
  if (item.key === props.current) {
    return
  }

  uni.switchTab({ url: item.path })
}
</script>

<template>
  <view class="bottom-nav-shell">
    <view class="bottom-nav">
      <view
        v-for="item in navItems"
        :key="item.key"
        class="nav-item"
        :class="{ active: item.key === props.current }"
        @tap="switchTo(item)"
      >
        <view class="nav-icon" :class="item.iconClass" />
        <text class="nav-item-text">{{ item.label }}</text>
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
  border-top: 1rpx solid rgba(220, 224, 227, 0.68);
  background: rgba(248, 249, 250, 0.98);
  box-shadow: 0 -8rpx 24rpx rgba(75, 88, 98, 0.025);
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
}

.nav-item-text {
  font-size: 23rpx;
  line-height: 1;
  color: #9aa3a9;
  font-weight: 400;
}

.nav-icon {
  width: 42rpx;
  height: 42rpx;
  background-repeat: no-repeat;
  background-position: center;
  background-size: contain;
  opacity: 0.82;
}

.nav-item.active .nav-item-text {
  color: #82929b;
  font-weight: 500;
}

.nav-item.active .nav-icon {
  opacity: 1;
}

.nav-icon-home {
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%239aa3a9' stroke-width='1.85' stroke-linecap='round' stroke-linejoin='round'><path d='M3.5 11.2 12 4l8.5 7.2'/><path d='M5.8 10.2v8.2a1.6 1.6 0 0 0 1.6 1.6h9.2a1.6 1.6 0 0 0 1.6-1.6v-8.2'/><path d='M9.5 20v-5.2h5V20'/></svg>");
}

.nav-icon-book {
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%239aa3a9' stroke-width='1.85' stroke-linecap='round' stroke-linejoin='round'><path d='M4.5 5.6A2.6 2.6 0 0 1 7.1 3H11v16H7.1a2.6 2.6 0 0 0-2.6 2.6z'/><path d='M19.5 5.6A2.6 2.6 0 0 0 16.9 3H13v16h3.9a2.6 2.6 0 0 1 2.6 2.6z'/><path d='M11 6.2H8.2'/><path d='M13 6.2h2.8'/></svg>");
}

.nav-icon-user {
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%239aa3a9' stroke-width='1.85' stroke-linecap='round' stroke-linejoin='round'><circle cx='12' cy='8.2' r='3.1'/><path d='M5.4 20.2a6.6 4.3 0 0 1 13.2 0'/></svg>");
}

.nav-item.active .nav-icon-home {
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%2382929b' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'><path d='M3.5 11.2 12 4l8.5 7.2'/><path d='M5.8 10.2v8.2a1.6 1.6 0 0 0 1.6 1.6h9.2a1.6 1.6 0 0 0 1.6-1.6v-8.2'/><path d='M9.5 20v-5.2h5V20'/></svg>");
}

.nav-item.active .nav-icon-book {
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%2382929b' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'><path d='M4.5 5.6A2.6 2.6 0 0 1 7.1 3H11v16H7.1a2.6 2.6 0 0 0-2.6 2.6z'/><path d='M19.5 5.6A2.6 2.6 0 0 0 16.9 3H13v16h3.9a2.6 2.6 0 0 1 2.6 2.6z'/><path d='M11 6.2H8.2'/><path d='M13 6.2h2.8'/></svg>");
}

.nav-item.active .nav-icon-user {
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%2382929b' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'><circle cx='12' cy='8.2' r='3.1'/><path d='M5.4 20.2a6.6 4.3 0 0 1 13.2 0'/></svg>");
}
</style>
