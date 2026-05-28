<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { computed, ref } from 'vue'
import { recordService } from '../../services'
import { useUserStore } from '../../stores'
import { RecordStatus } from '../../types'
import { hasAuthenticatedSession } from '../../utils'

type SettingKey = 'record' | 'tag' | 'privacy' | 'notify' | 'about'

interface SettingItem {
  key: SettingKey
  title: string
  iconClass: string
  meta?: string
}

interface SettingGroup {
  label: string
  items: SettingItem[]
}

const userStore = useUserStore()
const appVersion = 'v2.0.0'

const routeMap: Record<SettingKey, string> = {
  record: '/pages/user-center/archive-preference/index',
  tag: '/pages/user-center/tag-manage/index',
  privacy: '/pages/user-center/notify-settings/index',
  notify: '/pages/user-center/notify-settings/index',
  about: '/pages/user-center/about/index',
}

const settingGroups: SettingGroup[] = [
  {
    label: '档 案 设 置',
    items: [
      { key: 'record', title: '整理偏好', iconClass: 'icon-archive' },
      { key: 'tag', title: '视觉外观', iconClass: 'icon-appearance' },
    ],
  },
  {
    label: '隐 私 与 安 全',
    items: [
      { key: 'privacy', title: '访问控制', iconClass: 'icon-lock' },
      { key: 'notify', title: '数据备份', iconClass: 'icon-backup' },
    ],
  },
  {
    label: '关 于',
    items: [
      { key: 'about', title: '版本信息', iconClass: 'icon-info', meta: appVersion },
    ],
  },
]

const nickname = ref('访客')
const signature = ref('在静默中整理岁月的碎片')
const avatarUrl = ref('')
const savedCount = ref(0)
const archiveDays = ref(0)
const centerLoading = ref(false)
const centerLoadFailed = ref(false)
const profileReady = ref(false)

const avatarInitial = computed(() => (nickname.value.trim() || '访').slice(0, 1))
const displaySavedCount = computed(() => String(savedCount.value).replace(/\B(?=(\d{3})+(?!\d))/g, ','))
const displayArchiveDays = computed(() => String(archiveDays.value))
const showInitialLoading = computed(() => centerLoading.value && !profileReady.value)
const showInitialFailure = computed(() => centerLoadFailed.value && !profileReady.value)
const showStaleNotice = computed(() => centerLoadFailed.value && profileReady.value)

const ensureLogin = () => {
  if (!hasAuthenticatedSession()) {
    uni.reLaunch({ url: '/pages/login/index' })
    return false
  }
  return true
}

const handleItemTap = (key: SettingKey) => {
  const targetUrl = routeMap[key]
  if (!targetUrl) {
    uni.showToast({ title: '页面开发中', icon: 'none' })
    return
  }
  uni.navigateTo({ url: targetUrl })
}

const loadCenterData = async () => {
  if (!ensureLogin()) return
  centerLoading.value = true
  centerLoadFailed.value = false
  try {
    const [user, sealedPage, unlockedPage] = await Promise.all([
      userStore.fetchUserInfo(),
      recordService.getRecordList(RecordStatus.SEALED, { pageNum: 1, pageSize: 1 }),
      recordService.getUnlockedRecords(1, 1),
    ])
    nickname.value = user?.nickname || user?.username || '访客'
    signature.value = user?.email?.trim() || '在静默中整理岁月的碎片'
    avatarUrl.value = user?.avatar?.trim() || ''
    savedCount.value = sealedPage.total + unlockedPage.total
    archiveDays.value = sealedPage.total
    profileReady.value = true
  } catch {
    centerLoadFailed.value = true
    uni.showToast({ title: '网络有点慢，请稍后重试', icon: 'none' })
  } finally {
    centerLoading.value = false
  }
}

const logout = () => { userStore.logout() }

onShow(() => {
  uni.hideTabBar({ animation: false })
  loadCenterData()
})
</script>

<template>
  <view class="page">
    <view class="paper-texture" />
    <view class="paper-glow" />

    <scroll-view class="scroll-body" scroll-y enhanced :show-scrollbar="false">

      <!-- top brand bar -->
      <view class="top-bar">
        <view class="logo">时 光 回 序</view>
        <view class="more-btn">
          <view class="more-dot" />
          <view class="more-dot" />
          <view class="more-dot" />
        </view>
      </view>

      <!-- loading state -->
      <view v-if="showInitialLoading" class="state-block">
        <view class="state-title">正在整理你的个人档案</view>
        <view class="state-desc">昵称、统计与设置入口会在这里归位。</view>
      </view>

      <!-- failure state -->
      <view v-else-if="showInitialFailure" class="state-block">
        <view class="state-title">个人信息暂时没有加载出来</view>
        <view class="state-desc">网络有点慢，稍后再试一次。</view>
        <view class="state-action" @tap="loadCenterData">重新加载</view>
      </view>

      <template v-else>
        <!-- profile hero -->
        <view class="profile-hero">
          <view class="avatar-wrap">
            <view class="avatar-ring" />
            <view class="avatar">
              <image v-if="avatarUrl" class="avatar-img" :src="avatarUrl" mode="aspectFill" />
              <text v-else class="avatar-fallback">{{ avatarInitial }}</text>
            </view>
          </view>
          <view class="profile-name">{{ nickname }}</view>
          <view class="profile-bio">{{ signature }}</view>
          <view class="deco-line" />
        </view>

        <!-- stale notice -->
        <view v-if="showStaleNotice" class="stale-notice">
          网络有点慢，当前展示的是上次同步的信息
          <text class="stale-action" @tap="loadCenterData">重试</text>
        </view>

        <!-- stats row -->
        <view class="stats-row">
          <view class="stat-card">
            <view class="stat-label">已存记忆</view>
            <view class="stat-num">{{ displaySavedCount }}</view>
          </view>
          <view class="stat-card stat-card-highlight">
            <view class="stat-label">存档天数</view>
            <view class="stat-num">{{ displayArchiveDays }}</view>
          </view>
        </view>

        <!-- settings groups -->
        <view v-for="group in settingGroups" :key="group.label" class="settings-section">
          <view class="section-label">{{ group.label }}</view>
          <view class="settings-group">
            <view
              v-for="(item, index) in group.items"
              :key="item.key"
              class="setting-item"
              :class="{ 'setting-item-last': index === group.items.length - 1 }"
              @tap="handleItemTap(item.key)"
            >
              <view class="setting-icon">
                <view class="icon" :class="item.iconClass" />
              </view>
              <view class="setting-text">{{ item.title }}</view>
              <view v-if="item.meta" class="setting-badge">{{ item.meta }}</view>
              <view v-else class="setting-arrow" />
            </view>
          </view>
        </view>

        <!-- logout -->
        <view class="logout-wrap">
          <view class="logout-btn" @tap="logout">退出当前账户</view>
        </view>
      </template>

      <!-- nav safe area -->
      <view class="nav-safe-area" />
    </scroll-view>

    <!-- bottom navigation -->
    <view class="bottom-nav-shell">
      <view class="bottom-nav">
        <view class="nav-item" @tap="() => uni.switchTab({ url: '/pages/home/index' })">
          <text class="nav-label">首 页</text>
        </view>
        <view class="nav-item" @tap="() => uni.switchTab({ url: '/pages/timeline/index' })">
          <text class="nav-label">时 光 轴</text>
        </view>
        <view class="nav-item active" @tap="() => {}">
          <text class="nav-label">我 的</text>
          <view class="nav-dot" />
        </view>
      </view>
    </view>
  </view>
</template>

<style scoped>
.page {
  position: relative;
  min-height: 100vh;
  background: linear-gradient(170deg, #faf7f2 0%, #f5f0e8 55%, #f0ebe0 100%);
  overflow: hidden;
}

.paper-texture {
  position: fixed;
  inset: 0;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='500' height='500'%3E%3Cfilter id='f'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.55' numOctaves='6' stitchTiles='stitch'/%3E%3CfeColorMatrix type='saturate' values='0.15'/%3E%3C/filter%3E%3Crect width='500' height='500' filter='url(%23f)' opacity='0.055'/%3E%3C/svg%3E");
  pointer-events: none;
  z-index: 0;
}

.paper-glow {
  position: fixed;
  inset: 0;
  background:
    radial-gradient(ellipse 80% 50% at 18% 10%, rgba(200, 185, 158, 0.09) 0%, transparent 70%),
    radial-gradient(ellipse 60% 40% at 82% 25%, rgba(185, 168, 140, 0.06) 0%, transparent 65%),
    radial-gradient(ellipse 45% 55% at 70% 78%, rgba(178, 162, 135, 0.07) 0%, transparent 65%),
    radial-gradient(ellipse 50% 35% at 50% 45%, rgba(250, 245, 238, 0.18) 0%, transparent 75%);
  pointer-events: none;
  z-index: 0;
}

.scroll-body {
  position: relative;
  z-index: 1;
  height: 100vh;
}

/* ── top bar ── */
.top-bar {
  padding-top: calc(env(safe-area-inset-top) + 52px);
  padding-left: 56rpx;
  padding-right: 56rpx;
  text-align: center;
  position: relative;
}

.logo {
  font-family: var(--fb-font-serif);
  font-size: 24rpx;
  font-weight: 300;
  letter-spacing: 0.55em;
  color: var(--fb-ink-light);
}

.more-btn {
  position: absolute;
  right: 56rpx;
  top: calc(env(safe-area-inset-top) + 52px);
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 8rpx;
  height: 36rpx;
}

.more-dot {
  width: 6rpx;
  height: 6rpx;
  border-radius: 50%;
  background: var(--fb-ink-light);
  margin: 0 auto;
}

/* ── state blocks ── */
.state-block {
  margin: 80rpx 56rpx 0;
  padding: 48rpx 42rpx;
  border-radius: 4rpx;
  background: rgba(252, 249, 244, 0.72);
  border: 1rpx solid rgba(188, 174, 152, 0.28);
}

.state-title {
  font-family: var(--fb-font-serif);
  font-size: 36rpx;
  color: var(--fb-ink);
  line-height: 1.4;
}

.state-desc {
  margin-top: 18rpx;
  font-size: 26rpx;
  color: var(--fb-ink-mid);
  line-height: 1.7;
}

.state-action {
  margin-top: 34rpx;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 80rpx;
  padding: 0 40rpx;
  border-radius: 4rpx;
  background: var(--fb-vermilion);
  color: #ffffff;
  font-size: 28rpx;
}

/* ── profile hero ── */
.profile-hero {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 72rpx;
}

.avatar-wrap {
  position: relative;
  width: 152rpx;
  height: 152rpx;
  margin-bottom: 28rpx;
}

.avatar-ring {
  position: absolute;
  inset: -8rpx;
  border-radius: 50%;
  border: 1rpx solid var(--fb-ink-faint);
}

.avatar {
  width: 152rpx;
  height: 152rpx;
  border-radius: 50%;
  background: linear-gradient(145deg, #3a5a5c 0%, #1e3535 60%, #2a4a3a 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.avatar-img {
  width: 100%;
  height: 100%;
  display: block;
}

.avatar-fallback {
  font-family: var(--fb-font-serif);
  font-size: 64rpx;
  font-weight: 400;
  color: rgba(255, 255, 255, 0.85);
  letter-spacing: 4rpx;
}

.profile-name {
  font-family: var(--fb-font-serif);
  font-size: 40rpx;
  font-weight: 400;
  color: var(--fb-ink);
  letter-spacing: 0.12em;
  margin-bottom: 12rpx;
}

.profile-bio {
  font-family: var(--fb-font-serif);
  font-size: 22rpx;
  font-weight: 300;
  color: var(--fb-ink-light);
  letter-spacing: 0.06em;
}

.deco-line {
  width: 64rpx;
  height: 1rpx;
  background: var(--fb-ink-faint);
  margin-top: 56rpx;
}

/* ── stale notice ── */
.stale-notice {
  margin: 32rpx 56rpx 0;
  font-size: 24rpx;
  color: var(--fb-ink-mid);
  display: flex;
  gap: 16rpx;
}

.stale-action {
  color: var(--fb-vermilion);
}

/* ── stats row ── */
.stats-row {
  display: flex;
  gap: 24rpx;
  margin: 48rpx 56rpx 0;
}

.stat-card {
  flex: 1;
  position: relative;
  background: rgba(252, 249, 244, 0.72);
  border: 1rpx solid rgba(188, 174, 152, 0.28);
  border-radius: 2rpx;
  padding: 36rpx 40rpx 32rpx;
  box-shadow:
    0 2rpx 0 rgba(255, 255, 255, 0.6) inset,
    0 4rpx 24rpx rgba(140, 120, 90, 0.06),
    0 2rpx 6rpx rgba(140, 120, 90, 0.04);
  overflow: hidden;
}

/* vermilion left accent */
.stat-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 32rpx;
  bottom: 32rpx;
  width: 3rpx;
  background: linear-gradient(
    to bottom,
    transparent,
    rgba(181, 53, 42, 0.35) 25%,
    rgba(181, 53, 42, 0.35) 75%,
    transparent
  );
  border-radius: 2rpx;
}

/* corner fold */
.stat-card::after {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  width: 20rpx;
  height: 20rpx;
  background: linear-gradient(225deg, rgba(230, 218, 200, 0.9) 0%, rgba(230, 218, 200, 0.9) 48%, rgba(252, 249, 244, 0) 50%);
  border-left: 1rpx solid rgba(188, 174, 152, 0.22);
  border-bottom: 1rpx solid rgba(188, 174, 152, 0.22);
}

.stat-label {
  font-family: var(--fb-font-sans);
  font-size: 20rpx;
  font-weight: 300;
  color: var(--fb-ink-light);
  letter-spacing: 0.08em;
  margin-bottom: 16rpx;
}

.stat-num {
  font-family: var(--fb-font-serif);
  font-size: 56rpx;
  font-weight: 300;
  color: var(--fb-ink-mid);
  letter-spacing: 0.02em;
  line-height: 1;
}

.stat-card-highlight .stat-num {
  color: #8a6a3a;
}

/* ── settings sections ── */
.settings-section {
  margin-top: 64rpx;
  padding: 0 56rpx;
}

.section-label {
  font-family: var(--fb-font-sans);
  font-size: 20rpx;
  font-weight: 300;
  color: var(--fb-ink-light);
  letter-spacing: 0.12em;
  margin-bottom: 20rpx;
  padding-left: 4rpx;
}

.settings-group {
  position: relative;
  background: rgba(252, 249, 244, 0.72);
  border: 1rpx solid rgba(188, 174, 152, 0.28);
  border-radius: 2rpx;
  box-shadow:
    0 2rpx 0 rgba(255, 255, 255, 0.6) inset,
    0 4rpx 24rpx rgba(140, 120, 90, 0.06);
  overflow: hidden;
}

.setting-item {
  display: flex;
  align-items: center;
  gap: 28rpx;
  padding: 30rpx 36rpx;
  position: relative;
}

.setting-item:not(.setting-item-last)::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 92rpx;
  right: 36rpx;
  height: 1rpx;
  background: rgba(188, 174, 152, 0.22);
}

.setting-icon {
  width: 36rpx;
  height: 36rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.icon {
  width: 32rpx;
  height: 32rpx;
  background-repeat: no-repeat;
  background-position: center;
  background-size: contain;
}

.icon-archive {
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%236b6560' stroke-width='1.5' stroke-linecap='round' stroke-linejoin='round'><path d='M4 19V6.5A2.5 2.5 0 0 1 6.5 4h11A2.5 2.5 0 0 1 20 6.5V19l-8-2-8 2Z'/><line x1='8' y1='10' x2='16' y2='10'/><line x1='8' y1='14' x2='13' y2='14'/></svg>");
}

.icon-appearance {
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%236b6560' stroke-width='1.5' stroke-linecap='round' stroke-linejoin='round'><circle cx='12' cy='12' r='3'/><path d='M12 2v2M12 20v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M2 12h2M20 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42'/></svg>");
}

.icon-lock {
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%236b6560' stroke-width='1.5' stroke-linecap='round' stroke-linejoin='round'><rect x='3' y='11' width='18' height='11' rx='2'/><path d='M7 11V7a5 5 0 0 1 10 0v4'/></svg>");
}

.icon-backup {
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%236b6560' stroke-width='1.5' stroke-linecap='round' stroke-linejoin='round'><polyline points='22 12 16 12 14 15 10 15 8 12 2 12'/><path d='M5.45 5.11L2 12v6a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-6l-3.45-6.89A2 2 0 0 0 16.76 4H7.24a2 2 0 0 0-1.79 1.11z'/></svg>");
}

.icon-info {
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%239e9890' stroke-width='1.5' stroke-linecap='round' stroke-linejoin='round'><circle cx='12' cy='12' r='10'/><line x1='12' y1='8' x2='12' y2='12'/><line x1='12' y1='16' x2='12.01' y2='16'/></svg>");
}

.setting-arrow {
  width: 26rpx;
  height: 26rpx;
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 13 13' fill='none' stroke='%23c8c2b8' stroke-width='1.5' stroke-linecap='round'><polyline points='4,2 9,6.5 4,11'/></svg>");
  background-repeat: no-repeat;
  background-position: center;
  background-size: contain;
  flex-shrink: 0;
}

.setting-text {
  flex: 1;
  font-family: var(--fb-font-serif);
  font-size: 28rpx;
  font-weight: 300;
  color: var(--fb-ink-mid);
  letter-spacing: 0.03em;
}

.setting-badge {
  font-family: var(--fb-font-sans);
  font-size: 20rpx;
  font-weight: 300;
  color: var(--fb-ink-light);
  letter-spacing: 0.06em;
}

/* ── logout ── */
.logout-wrap {
  text-align: center;
  margin: 56rpx 0 8rpx;
}

.logout-btn {
  font-family: var(--fb-font-serif);
  font-size: 26rpx;
  font-weight: 300;
  color: var(--fb-vermilion);
  opacity: 0.75;
  letter-spacing: 0.08em;
  display: inline-block;
}

/* ── nav safe area ── */
.nav-safe-area {
  height: calc(128rpx + env(safe-area-inset-bottom));
}

/* ── bottom navigation ── */
.bottom-nav-shell {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 80;
  padding: 0 0 env(safe-area-inset-bottom);
  border-top: 1rpx solid rgba(200, 194, 184, 0.3);
  background: rgba(250, 247, 242, 0.96);
  box-shadow: 0 -8rpx 24rpx rgba(48, 46, 41, 0.04);
}

.bottom-nav {
  height: 104rpx;
  display: flex;
  align-items: center;
  justify-content: space-around;
}

.nav-item {
  flex: 1;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6rpx;
}

.nav-label {
  font-family: var(--fb-font-serif);
  font-size: 24rpx;
  font-weight: 300;
  letter-spacing: 0.2em;
  color: var(--fb-ink-light);
}

.nav-item.active .nav-label {
  color: var(--fb-ink);
  font-weight: 400;
}

.nav-dot {
  width: 6rpx;
  height: 6rpx;
  border-radius: 50%;
  background: var(--fb-vermilion);
  opacity: 0.9;
}
</style>
