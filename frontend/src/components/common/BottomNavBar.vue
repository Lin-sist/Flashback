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
  { key: 'user-center', label: '个人中心', path: '/pages/user-center/index' },
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
        <text class="nav-item-text">{{ item.label }}</text>
      </view>
    </view>
  </view>
</template>

<style scoped>
.bottom-nav-shell {
  position: fixed;
  left: var(--fb-space-page);
  right: var(--fb-space-page);
  bottom: calc(env(safe-area-inset-bottom) + 20rpx);
  z-index: 80;
}

.bottom-nav {
  height: var(--fb-bottom-nav-height);
  padding: 10rpx;
  border-radius: var(--fb-radius-pill);
  border: 1rpx solid var(--fb-bottom-nav-border-color);
  background: var(--fb-bottom-nav-bg);
  box-shadow: var(--fb-shadow-soft);
  display: flex;
  align-items: center;
}

.nav-item {
  flex: 1;
  height: 100%;
  border-radius: var(--fb-radius-pill);
  display: flex;
  align-items: center;
  justify-content: center;
}

.nav-item-text {
  font-size: var(--fb-font-body-sub);
  color: var(--fb-bottom-nav-text-color);
}

.nav-item.active {
  background: var(--fb-bottom-nav-active-bg);
}

.nav-item.active .nav-item-text {
  color: var(--fb-bottom-nav-active-text-color);
  font-weight: 600;
}
</style>