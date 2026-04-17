<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { ref } from 'vue'
import AppTopBar from '../../components/common/AppTopBar.vue'
import BottomNavBar from '../../components/common/BottomNavBar.vue'
import EmptyState from '../../components/common/EmptyState.vue'
import PaperContainer from '../../components/common/PaperContainer.vue'
import PrimaryButton from '../../components/common/PrimaryButton.vue'
import SettingGroupCard, { type SettingItem } from '../../components/common/SettingGroupCard.vue'
import { recordService } from '../../services'
import { useUserStore } from '../../stores'
import { RecordStatus } from '../../types'
import { getToken } from '../../utils'

const userStore = useUserStore()

const nickname = ref('访客')
const signature = ref('把经历写给未来的自己')
const savedCount = ref(0)
const waitingUnlockCount = ref(0)
const centerLoading = ref(false)
const centerLoadFailed = ref(false)
const profileReady = ref(false)
const statsLoadFailed = ref(false)
const statsReady = ref(false)

const groupArchive: SettingItem[] = [
  { key: 'record', title: '档案偏好', subtitle: '记录类型与封存习惯' },
  { key: 'tag', title: '标签管理', subtitle: '情绪标签 / 主题标签' },
]

const groupPrivacy: SettingItem[] = [
  { key: 'privacy', title: '隐私与安全', subtitle: '登录状态与访问权限' },
  { key: 'notify', title: '提醒设置', subtitle: '解锁提醒与通知偏好' },
]

const groupAbout: SettingItem[] = [
  { key: 'about', title: '关于时光回序', subtitle: '版本信息与项目说明' },
]

const ensureLogin = () => {
  if (!getToken()) {
    uni.reLaunch({ url: '/pages/login/index' })
    return false
  }
  return true
}

const handleGroupTap = (key: string) => {
  const routeMap: Record<string, string> = {
    record: '/pages/user-center/archive-preference/index',
    tag: '/pages/user-center/tag-manage/index',
    privacy: '/pages/user-center/notify-settings/index',
    notify: '/pages/user-center/notify-settings/index',
    about: '/pages/user-center/about/index',
  }

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
  statsLoadFailed.value = false

  const [userResult, sealedResult, unlockedResult] = await Promise.allSettled([
    userStore.fetchUserInfo(),
    recordService.getRecordList(RecordStatus.SEALED, { pageNum: 1, pageSize: 1 }),
    recordService.getUnlockedRecords(1, 1),
  ])

  if (userResult.status === 'fulfilled') {
    const user = userResult.value
    nickname.value = user?.nickname || user?.username || '访客'
    signature.value = user?.email ? `${user.email}` : '把经历写给未来的自己'
    profileReady.value = true
    centerLoadFailed.value = false
  } else {
    centerLoadFailed.value = true
  }

  if (sealedResult.status === 'fulfilled' && unlockedResult.status === 'fulfilled') {
    savedCount.value = sealedResult.value.total + unlockedResult.value.total
    waitingUnlockCount.value = sealedResult.value.total
    statsReady.value = true
    statsLoadFailed.value = false
  } else {
    statsLoadFailed.value = true
  }

  if (centerLoadFailed.value && !profileReady.value) {
    uni.showToast({ title: '网络有点慢，请稍后重试', icon: 'none' })
  }

  centerLoading.value = false
}

const logout = () => userStore.logout()

onShow(() => {
  uni.hideTabBar({ animation: false })
  loadCenterData()
})
</script>

<template>
  <view class="page">
    <AppTopBar title="Flashback" right-text="更多" @right-tap="handleGroupTap('more')" />

    <view v-if="centerLoading && !profileReady" class="state-wrap">
      <EmptyState text="正在加载个人信息..." />
    </view>

    <view v-else-if="centerLoadFailed && !profileReady" class="state-wrap">
      <EmptyState text="网络有点慢，个人信息暂时没加载出来" />
      <PrimaryButton text="重试加载" ghost @tap="loadCenterData" />
    </view>

    <PaperContainer v-else radius="xl" class="profile-card">
      <view class="avatar">{{ nickname.slice(0, 1) || '访' }}</view>
      <view>
        <view class="name">{{ nickname || '访客' }}</view>
        <view class="signature">{{ signature || '把经历写给未来的自己' }}</view>
      </view>
    </PaperContainer>

    <view v-if="centerLoadFailed && profileReady" class="inline-error">
      网络有点慢，当前展示的是上次同步的信息
      <text class="inline-retry" @tap="loadCenterData">重试</text>
    </view>

    <view class="stats">
      <PaperContainer radius="xl" class="stat-card">
        <view class="stat-num">{{ statsLoadFailed && !statsReady ? '--' : savedCount }}</view>
        <view class="stat-label">已存记忆</view>
      </PaperContainer>
      <PaperContainer radius="xl" warm class="stat-card">
        <view class="stat-num">{{ statsLoadFailed && !statsReady ? '--' : waitingUnlockCount }}</view>
        <view class="stat-label">待解封</view>
      </PaperContainer>
    </view>

    <view v-if="statsLoadFailed" class="inline-error">
      统计信息暂时不可用
      <text class="inline-retry" @tap="loadCenterData">重试</text>
    </view>

    <view class="groups">
      <SettingGroupCard title="档案设置" :items="groupArchive" @item-tap="handleGroupTap" />
      <SettingGroupCard title="隐私与安全" :items="groupPrivacy" @item-tap="handleGroupTap" />
      <SettingGroupCard title="关于" :items="groupAbout" @item-tap="handleGroupTap" />
    </view>

    <view class="logout" @tap="logout">退出登录</view>

    <BottomNavBar current="user-center" />
  </view>
</template>

<style scoped>
.page {
  min-height: 100vh;
  padding: 20rpx 24rpx 180rpx;
  background: #f8fafb;
}

.profile-card {
  margin-top: 20rpx;
  display: flex;
  align-items: center;
  gap: 20rpx;
}

.avatar {
  width: 98rpx;
  height: 98rpx;
  border-radius: 49rpx;
  background: linear-gradient(145deg, #e8f1f5 0%, #cfdde6 100%);
  color: #3b647a;
  font-size: 42rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
}

.name {
  font-size: 40rpx;
  color: #1a1a1a;
  font-weight: 600;
}

.signature {
  margin-top: 6rpx;
  font-size: 24rpx;
  color: #7f8c93;
}

.stats {
  margin-top: 20rpx;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14rpx;
}

.state-wrap {
  margin-top: 24rpx;
}

.inline-error {
  margin-top: 14rpx;
  color: #7f8c93;
  font-size: 24rpx;
}

.inline-retry {
  margin-left: 10rpx;
  color: #3b647a;
}

.stat-card {
  text-align: center;
}

.stat-num {
  font-size: 64rpx;
  color: #3b647a;
  font-weight: 600;
}

.stat-label {
  margin-top: 8rpx;
  color: #7f8c93;
  font-size: 28rpx;
}

.groups {
  margin-top: 24rpx;
  display: flex;
  flex-direction: column;
  gap: 14rpx;
}

.logout {
  margin-top: 30rpx;
  text-align: center;
  color: #9ba7ae;
  font-size: 28rpx;
}
</style>

