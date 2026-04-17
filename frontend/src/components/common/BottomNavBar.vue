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
  left: 24rpx;
  right: 24rpx;
  bottom: calc(env(safe-area-inset-bottom) + 20rpx);
  z-index: 80;
}

.bottom-nav {
  height: 108rpx;
  padding: 10rpx;
  border-radius: 999rpx;
  border: 1rpx solid rgba(172, 179, 182, 0.2);
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 8rpx 24rpx rgba(26, 26, 26, 0.06);
  display: flex;
  align-items: center;
}

.nav-item {
  flex: 1;
  height: 100%;
  border-radius: 999rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.nav-item-text {
  font-size: 28rpx;
  color: #7f8c93;
}

.nav-item.active {
  background: #eef3f6;
}

.nav-item.active .nav-item-text {
  color: #3b647a;
  font-weight: 600;
}
</style>
