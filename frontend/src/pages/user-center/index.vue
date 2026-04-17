<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { ref } from 'vue'
import AppTopBar from '../../components/common/AppTopBar.vue'
import BottomNavBar from '../../components/common/BottomNavBar.vue'
import PaperContainer from '../../components/common/PaperContainer.vue'
import SettingGroupCard, { type SettingItem } from '../../components/common/SettingGroupCard.vue'
import { recordService } from '../../services'
import { useUserStore } from '../../stores'
import { RecordStatus } from '../../types'
import { getToken, toUserMessage } from '../../utils'

const userStore = useUserStore()

const nickname = ref('访客')
const signature = ref('把经历写给未来的自己')
const savedCount = ref(0)
const archiveDays = ref(0)

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
  uni.showToast({ title: `${key} 配置将于后续版本开放`, icon: 'none' })
}

const loadCenterData = async () => {
  if (!ensureLogin()) {
    return
  }

  try {
    const [user, draftPage, sealedPage, unlockedPage] = await Promise.all([
      userStore.fetchUserInfo(),
      recordService.getRecordList(RecordStatus.DRAFT, { pageNum: 1, pageSize: 1 }),
      recordService.getRecordList(RecordStatus.SEALED, { pageNum: 1, pageSize: 1 }),
      recordService.getUnlockedRecords(1, 1),
    ])

    nickname.value = user?.nickname || user?.username || '访客'
    signature.value = user?.email ? `${user.email}` : '把经历写给未来的自己'
    savedCount.value = sealedPage.total + unlockedPage.total
    archiveDays.value = Math.max(1, Math.ceil((draftPage.total + sealedPage.total + unlockedPage.total) / 2))
  } catch (error) {
    uni.showToast({ title: toUserMessage(error), icon: 'none' })
  }
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

    <PaperContainer radius="xl" class="profile-card">
      <view class="avatar">{{ nickname.slice(0, 1) }}</view>
      <view>
        <view class="name">{{ nickname }}</view>
        <view class="signature">{{ signature }}</view>
      </view>
    </PaperContainer>

    <view class="stats">
      <PaperContainer radius="xl" class="stat-card">
        <view class="stat-num">{{ savedCount }}</view>
        <view class="stat-label">已存记忆</view>
      </PaperContainer>
      <PaperContainer radius="xl" warm class="stat-card">
        <view class="stat-num">{{ archiveDays }}</view>
        <view class="stat-label">存档天数</view>
      </PaperContainer>
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
  padding: 20rpx var(--fb-space-page) 180rpx;
  background: var(--fb-color-bg);
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
  color: var(--fb-color-primary);
  font-size: 42rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
}

.name {
  font-size: 40rpx;
  color: var(--fb-color-text);
  font-weight: 600;
}

.signature {
  margin-top: 6rpx;
  font-size: var(--fb-font-meta);
  color: var(--fb-color-text-muted);
}

.stats {
  margin-top: 20rpx;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14rpx;
}

.stat-card {
  text-align: center;
}

.stat-num {
  font-size: var(--fb-font-number);
  color: var(--fb-color-primary);
  font-weight: 600;
}

.stat-label {
  margin-top: 8rpx;
  color: var(--fb-color-text-muted);
  font-size: var(--fb-font-body-sub);
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
  font-size: var(--fb-font-body-sub);
}
</style>
