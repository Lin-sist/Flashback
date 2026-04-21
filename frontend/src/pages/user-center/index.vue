<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { computed, ref } from 'vue'
import AppPageShell from '../../components/common/AppPageShell.vue'
import { recordService } from '../../services'
import { useUserStore } from '../../stores'
import { RecordStatus } from '../../types'
import { hasAuthenticatedSession } from '../../utils'

type SettingKey = 'record' | 'tag' | 'privacy' | 'notify' | 'about'
type SettingTone = 'archive' | 'privacy' | 'about'
type SettingIcon = 'book' | 'tag' | 'lock' | 'bell' | 'info'

interface SettingItem {
  key: SettingKey
  title: string
  subtitle: string
  icon: SettingIcon
  meta?: string
}

interface SettingGroup {
  title: string
  tone: SettingTone
  items: SettingItem[]
}

const userStore = useUserStore()

const appVersion = 'MVP 0.1.0'

const routeMap: Record<SettingKey, string> = {
  record: '/pages/user-center/archive-preference/index',
  tag: '/pages/user-center/tag-manage/index',
  privacy: '/pages/user-center/notify-settings/index',
  notify: '/pages/user-center/notify-settings/index',
  about: '/pages/user-center/about/index',
}

const settingGroups: SettingGroup[] = [
  {
    title: '档案整理',
    tone: 'archive',
    items: [
      { key: 'record', title: '档案偏好', subtitle: '记录类型与封存习惯', icon: 'book' },
      { key: 'tag', title: '标签管理', subtitle: '情绪标签与主题标签', icon: 'tag' },
    ],
  },
  {
    title: '提醒与权限',
    tone: 'privacy',
    items: [
      { key: 'privacy', title: '隐私与安全', subtitle: '登录状态与访问权限', icon: 'lock' },
      { key: 'notify', title: '提醒设置', subtitle: '解锁提醒与通知偏好', icon: 'bell' },
    ],
  },
  {
    title: '关于',
    tone: 'about',
    items: [
      { key: 'about', title: '关于时光回序', subtitle: '版本说明与项目边界', icon: 'info', meta: appVersion },
    ],
  },
]

const nickname = ref('访客')
const signature = ref('把经历写给未来的自己')
const avatarUrl = ref('')
const savedCount = ref(0)
const waitingUnlockCount = ref(0)
const centerLoading = ref(false)
const centerLoadFailed = ref(false)
const profileReady = ref(false)

const avatarInitial = computed(() => (nickname.value.trim() || '访').slice(0, 1))
const displaySavedCount = computed(() => formatCount(savedCount.value))
const displayWaitingUnlockCount = computed(() => formatCount(waitingUnlockCount.value))
const showInitialLoading = computed(() => centerLoading.value && !profileReady.value)
const showInitialFailure = computed(() => centerLoadFailed.value && !profileReady.value)
const showStaleNotice = computed(() => centerLoadFailed.value && profileReady.value)

function formatCount(value: number) {
  return String(value).replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}

const ensureLogin = () => {
  if (!hasAuthenticatedSession()) {
    uni.reLaunch({ url: '/pages/login/index' })
    return false
  }
  return true
}

const handleGroupTap = (key: SettingKey) => {
  const targetUrl = routeMap[key]
  if (!targetUrl) {
    uni.showToast({ title: '页面开发中', icon: 'none' })
    return
  }

  uni.navigateTo({ url: targetUrl })
}

const loadCenterData = async () => {
  if (!ensureLogin()) {
    return
  }

  centerLoading.value = true
  centerLoadFailed.value = false

  try {
    const [user, sealedPage, unlockedPage] = await Promise.all([
      userStore.fetchUserInfo(),
      recordService.getRecordList(RecordStatus.SEALED, { pageNum: 1, pageSize: 1 }),
      recordService.getUnlockedRecords(1, 1),
    ])

    nickname.value = user?.nickname || user?.username || '访客'
    signature.value = user?.email?.trim() || '把经历写给未来的自己'
    avatarUrl.value = user?.avatar?.trim() || ''
    savedCount.value = sealedPage.total + unlockedPage.total
    waitingUnlockCount.value = sealedPage.total
    profileReady.value = true
  } catch {
    centerLoadFailed.value = true
    uni.showToast({ title: '网络有点慢，请稍后重试', icon: 'none' })
  } finally {
    centerLoading.value = false
  }
}

const logout = () => {
  userStore.logout()
}

onShow(() => {
  uni.hideTabBar({ animation: false })
  loadCenterData()
})
</script>

<template>
  <AppPageShell class="page" title="时光回序" current="user-center">
    <template #background>
      <view class="page-glow page-glow-left" />
      <view class="page-glow page-glow-right" />
    </template>

    <view v-if="showInitialLoading" class="state-card">
      <view class="state-kicker">PERSONAL CONTROL</view>
      <view class="state-title">正在整理你的个人档案</view>
      <view class="state-desc">昵称、统计与设置入口会在这里归位。</view>
    </view>

    <view v-else-if="showInitialFailure" class="state-card state-card-failed">
      <view class="state-kicker">PERSONAL CONTROL</view>
      <view class="state-title">个人信息暂时没有加载出来</view>
      <view class="state-desc">网络有点慢，稍后再试一次。</view>
      <view class="state-action" @tap="loadCenterData">重新加载</view>
    </view>

    <template v-else>
      <view class="identity">
        <view class="identity-ring">
          <view class="avatar-wrap">
            <image v-if="avatarUrl" class="avatar" :src="avatarUrl" mode="aspectFill" />
            <text v-else class="avatar-fallback">{{ avatarInitial }}</text>
          </view>
        </view>
        <view class="identity-kicker">PERSONAL CONTROL</view>
        <view class="identity-name">{{ nickname }}</view>
        <view class="identity-signature">{{ signature }}</view>
      </view>

      <view v-if="showStaleNotice" class="inline-notice">
        网络有点慢，当前展示的是上次同步的信息
        <text class="inline-notice-action" @tap="loadCenterData">重试</text>
      </view>

      <view class="stats-row">
        <view class="stat-card stat-card-cool">
          <view class="stat-label">已存记忆</view>
          <view class="stat-value">{{ displaySavedCount }}</view>
          <view class="stat-meta">已封存与已解锁记录总数</view>
        </view>
        <view class="stat-card stat-card-warm">
          <view class="stat-label">待解封</view>
          <view class="stat-value">{{ displayWaitingUnlockCount }}</view>
          <view class="stat-meta">仍在等待开启的封存记录</view>
        </view>
      </view>

      <view class="group-list">
        <view v-for="group in settingGroups" :key="group.title" class="group">
          <view class="group-title">{{ group.title }}</view>
          <view class="group-card" :class="`group-card-${group.tone}`">
            <view
              v-for="(item, index) in group.items"
              :key="item.key"
              class="group-row"
              :class="{ 'group-row-subdued': item.key === 'about' }"
              @tap="handleGroupTap(item.key)"
            >
              <view class="row-icon-wrap" :class="`row-icon-wrap-${group.tone}`">
                <view class="icon" :class="`icon-${item.icon}`" />
              </view>
              <view class="row-copy">
                <view class="row-label">{{ item.title }}</view>
                <view class="row-subtitle">{{ item.subtitle }}</view>
              </view>
              <view class="row-meta">
                <view v-if="item.meta" class="row-meta-text">{{ item.meta }}</view>
                <view class="row-arrow">›</view>
              </view>
              <view v-if="index < group.items.length - 1" class="group-divider" />
            </view>
          </view>
        </view>
      </view>

      <view class="logout-wrap">
        <view class="logout-tip">账户操作</view>
        <view class="logout-button" @tap="logout">退出登录</view>
      </view>
    </template>
  </AppPageShell>
</template>

<style scoped>
.page {
  position: relative;
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgba(214, 224, 230, 0.48) 0, rgba(214, 224, 230, 0) 36%),
    radial-gradient(circle at top right, rgba(244, 232, 210, 0.72) 0, rgba(244, 232, 210, 0) 34%),
    linear-gradient(180deg, #fbfcfd 0%, #f6f8f9 100%);
  overflow: hidden;
}

.page-glow {
  position: absolute;
  width: 320rpx;
  height: 320rpx;
  border-radius: 999rpx;
  filter: blur(16rpx);
  opacity: 0.65;
  pointer-events: none;
}

.page-glow-left {
  top: 160rpx;
  left: -140rpx;
  background: rgba(215, 226, 232, 0.5);
}

.page-glow-right {
  top: 420rpx;
  right: -120rpx;
  background: rgba(243, 223, 187, 0.46);
}

.state-card {
  position: relative;
  z-index: 1;
  margin-top: 80rpx;
  padding: 48rpx 42rpx;
  border-radius: 40rpx;
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 24rpx 52rpx rgba(26, 26, 26, 0.06);
  backdrop-filter: blur(10rpx);
}

.state-card-failed {
  background: rgba(253, 249, 247, 0.92);
}

.state-kicker {
  font-size: 22rpx;
  color: #9aa3a9;
  letter-spacing: 5rpx;
}

.state-title {
  margin-top: 18rpx;
  font-size: 42rpx;
  line-height: 1.3;
  color: #1a1a1a;
  font-weight: 600;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
}

.state-desc {
  margin-top: 18rpx;
  font-size: 28rpx;
  line-height: 1.7;
  color: #7f8c93;
}

.state-action {
  margin-top: 34rpx;
  width: 220rpx;
  height: 84rpx;
  border-radius: 999rpx;
  background: #3b647a;
  color: #ffffff;
  font-size: 28rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.identity {
  position: relative;
  z-index: 1;
  margin-top: 36rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.identity-ring {
  width: 220rpx;
  height: 220rpx;
  padding: 14rpx;
  border-radius: 999rpx;
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.92) 0%, rgba(234, 239, 242, 0.76) 100%);
  box-shadow: 0 18rpx 40rpx rgba(31, 45, 54, 0.09);
}

.avatar-wrap {
  width: 100%;
  height: 100%;
  border-radius: 999rpx;
  overflow: hidden;
  background:
    radial-gradient(circle at 28% 26%, rgba(255, 255, 255, 0.68) 0, rgba(255, 255, 255, 0) 32%),
    linear-gradient(160deg, #6e8796 0%, #314955 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar {
  width: 100%;
  height: 100%;
  display: block;
}

.avatar-fallback {
  color: #ffffff;
  font-size: 72rpx;
  font-weight: 500;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
  letter-spacing: 4rpx;
}

.identity-kicker {
  margin-top: 28rpx;
  font-size: 22rpx;
  color: #9aa3a9;
  letter-spacing: 6rpx;
}

.identity-name {
  margin-top: 18rpx;
  font-size: 52rpx;
  line-height: 1.2;
  font-weight: 600;
  color: #171a1d;
  letter-spacing: 4rpx;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
}

.identity-signature {
  margin-top: 16rpx;
  max-width: 560rpx;
  font-size: 26rpx;
  line-height: 1.8;
  color: #7f8c93;
  letter-spacing: 1rpx;
}

.inline-notice {
  position: relative;
  z-index: 1;
  margin-top: 34rpx;
  padding: 24rpx 30rpx;
  border-radius: 28rpx;
  background: rgba(255, 255, 255, 0.7);
  color: #7d878d;
  font-size: 24rpx;
  line-height: 1.7;
}

.inline-notice-action {
  margin-left: 12rpx;
  color: #3b647a;
}

.stats-row {
  position: relative;
  z-index: 1;
  margin-top: 44rpx;
  display: flex;
  gap: 20rpx;
}

.stat-card {
  flex: 1;
  min-height: 230rpx;
  padding: 34rpx 30rpx 30rpx;
  border-radius: 34rpx;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.stat-card-cool {
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 12rpx 28rpx rgba(26, 26, 26, 0.05);
}

.stat-card-warm {
  background: linear-gradient(180deg, #f4e7cf 0%, #ead5ae 100%);
  box-shadow: 0 12rpx 28rpx rgba(135, 112, 61, 0.08);
}

.stat-label {
  font-size: 24rpx;
  color: #86939a;
  letter-spacing: 2rpx;
}

.stat-card-warm .stat-label {
  color: #8a7750;
}

.stat-value {
  margin-top: 18rpx;
  font-size: 80rpx;
  line-height: 1;
  color: #35586d;
  font-weight: 500;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', 'Times New Roman', serif;
  letter-spacing: 2rpx;
}

.stat-card-warm .stat-value {
  color: #6d582b;
}

.stat-meta {
  margin-top: 18rpx;
  font-size: 22rpx;
  line-height: 1.6;
  color: #8d989e;
}

.stat-card-warm .stat-meta {
  color: #88754e;
}

.group-list {
  position: relative;
  z-index: 1;
  margin-top: 58rpx;
}

.group + .group {
  margin-top: 34rpx;
}

.group-title {
  padding: 0 14rpx 20rpx;
  font-size: 25rpx;
  color: #9aa3a9;
  letter-spacing: 4rpx;
}

.group-card {
  border-radius: 34rpx;
  padding: 8rpx 0;
  overflow: hidden;
  box-shadow: 0 12rpx 30rpx rgba(26, 26, 26, 0.04);
}

.group-card-archive {
  background: rgba(255, 255, 255, 0.92);
}

.group-card-privacy {
  background: rgba(241, 244, 246, 0.95);
}

.group-card-about {
  background: rgba(247, 245, 240, 0.96);
}

.group-row {
  position: relative;
  min-height: 124rpx;
  padding: 0 32rpx;
  display: flex;
  align-items: center;
}

.group-row-subdued .row-label {
  color: #30363a;
}

.group-row-subdued .row-subtitle {
  color: #90989d;
}

.group-divider {
  position: absolute;
  left: 104rpx;
  right: 32rpx;
  bottom: 0;
  height: 1rpx;
  background: rgba(26, 26, 26, 0.08);
}

.row-icon-wrap {
  width: 56rpx;
  height: 56rpx;
  border-radius: 18rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.row-icon-wrap-archive {
  background: rgba(59, 100, 122, 0.1);
}

.row-icon-wrap-privacy {
  background: rgba(90, 109, 120, 0.1);
}

.row-icon-wrap-about {
  background: rgba(122, 111, 88, 0.1);
}

.row-copy {
  flex: 1;
  min-width: 0;
  margin-left: 22rpx;
}

.row-label {
  font-size: 31rpx;
  color: #1a1a1a;
  font-weight: 500;
  letter-spacing: 1rpx;
}

.row-subtitle {
  margin-top: 8rpx;
  font-size: 24rpx;
  line-height: 1.5;
  color: #88949b;
}

.row-meta {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-left: 16rpx;
}

.row-meta-text {
  font-size: 22rpx;
  color: #a08f69;
  letter-spacing: 1rpx;
}

.row-arrow {
  font-size: 38rpx;
  line-height: 1;
  color: #a0a9ae;
}

.logout-wrap {
  position: relative;
  z-index: 1;
  margin-top: 74rpx;
  padding-bottom: 18rpx;
  text-align: center;
}

.logout-tip {
  font-size: 22rpx;
  color: #b0b7bc;
  letter-spacing: 5rpx;
}

.logout-button {
  margin: 22rpx auto 0;
  width: 280rpx;
  height: 88rpx;
  border-radius: 999rpx;
  border: 1rpx solid rgba(200, 83, 74, 0.18);
  background: rgba(255, 255, 255, 0.74);
  color: #b25d57;
  font-size: 28rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  letter-spacing: 2rpx;
}

.icon {
  width: 34rpx;
  height: 34rpx;
  background-repeat: no-repeat;
  background-position: center;
  background-size: contain;
}

.icon-book {
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%233b647a' stroke-width='1.7' stroke-linecap='round' stroke-linejoin='round'><path d='M4 4h6a3 3 0 0 1 3 3v13a2 2 0 0 0-2-2H4z'/><path d='M20 4h-6a3 3 0 0 0-3 3v13a2 2 0 0 1 2-2h7z'/></svg>");
}

.icon-tag {
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%233b647a' stroke-width='1.7' stroke-linecap='round' stroke-linejoin='round'><path d='M20 10 10.5 19.5a2.1 2.1 0 0 1-3 0L4.5 16.5a2.1 2.1 0 0 1 0-3L14 4h6v6z'/><circle cx='16.5' cy='7.5' r='1.2'/></svg>");
}

.icon-lock {
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%2355636b' stroke-width='1.7' stroke-linecap='round' stroke-linejoin='round'><rect x='5' y='11' width='14' height='10' rx='2'/><path d='M8 11V7a4 4 0 0 1 8 0v4'/></svg>");
}

.icon-bell {
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%2355636b' stroke-width='1.7' stroke-linecap='round' stroke-linejoin='round'><path d='M15 17H5l1.4-1.4A2 2 0 0 0 7 14.2V11a5 5 0 0 1 10 0v3.2a2 2 0 0 0 .6 1.4L19 17h-4'/><path d='M10 19a2 2 0 0 0 4 0'/></svg>");
}

.icon-info {
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%237a6f58' stroke-width='1.7' stroke-linecap='round' stroke-linejoin='round'><circle cx='12' cy='12' r='9'/><line x1='12' y1='11' x2='12' y2='16'/><circle cx='12' cy='8' r='0.8' fill='%237a6f58' stroke='none'/></svg>");
}
</style>
