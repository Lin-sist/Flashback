<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { computed, ref } from 'vue'
import PreviewModeNotice from '../../components/common/PreviewModeNotice.vue'
import AppTopSafeBar from '../../components/common/AppTopSafeBar.vue'
import { recordService, stageSummaryService, type StageSummaryVO } from '../../services'
import { useUserStore } from '../../stores'
import { RecordStatus } from '../../types'
import { hasPreviewSession, showPreviewReadonlyToast } from '../../features/preview/preview-session'
import { formatDayText, getToken, hasAuthenticatedSession } from '../../utils'

type SettingKey = 'dataOwnership' | 'about'

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
const routeMap: Record<SettingKey, string> = {
  dataOwnership: '/pages/user-center/data-backup/index',
  about: '/pages/user-center/about/index',
}

const settingGroups: SettingGroup[] = [
  {
    label: '数 据',
    items: [
      { key: 'dataOwnership', title: '数据与所有权', iconClass: 'icon-info', meta: '导出与清理' },
    ],
  },
  {
    label: '关 于',
    items: [
      { key: 'about', title: '构建信息', iconClass: 'icon-info', meta: '演示构建' },
    ],
  },
]

const nickname = ref('访客')
const signature = ref('给想留下的此刻，一个位置')
const avatarUrl = ref('')
const archivedCount = ref(0)
const sealedCount = ref(0)
const centerLoading = ref(false)
const centerLoadFailed = ref(false)
const profileReady = ref(false)
const summaryLoading = ref(false)
const stageSummary = ref<StageSummaryVO | null>(null)
const summaryOpen = ref(false)
const summaryRevision = ref(0)

const avatarInitial = computed(() => (nickname.value.trim() || '访').slice(0, 1))
const displayArchivedCount = computed(() => String(archivedCount.value).replace(/\B(?=(\d{3})+(?!\d))/g, ','))
const displaySealedCount = computed(() => String(sealedCount.value))
const showInitialLoading = computed(() => centerLoading.value && !profileReady.value)
const showInitialFailure = computed(() => centerLoadFailed.value && !profileReady.value)
const showStaleNotice = computed(() => centerLoadFailed.value && profileReady.value)
const stageSummarySourceLabel = computed(() =>
  stageSummary.value?.status === 'SUCCESS' ? 'AI 整理' : '本地整理',
)
const summaryDateText = computed(() => stageSummary.value?.generatedAt ? formatDayText(stageSummary.value.generatedAt) : '')

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

const navigateToArchive = () => {
  uni.navigateTo({ url: '/pages/record-list/index' })
}

const goHome = () => {
  uni.switchTab({ url: '/pages/home/index' })
}

const goTimeline = () => {
  uni.switchTab({ url: '/pages/timeline/index' })
}

const generateStageSummary = async () => {
  if (summaryLoading.value) return
  if (!getToken() && hasPreviewSession()) {
    showPreviewReadonlyToast('演示模式不生成真实阶段总结')
    return
  }
  summaryLoading.value = true
  try {
    stageSummary.value = await stageSummaryService.generate()
    summaryRevision.value += 1
    summaryOpen.value = true
    if (stageSummary.value.status !== 'SUCCESS') {
      uni.showToast({
        title: stageSummary.value.message || 'AI暂不可用，已使用本地总结',
        icon: 'none',
      })
    }
  } catch (error) {
    const message = error instanceof Error ? error.message : ''
    uni.showToast({
      title: /timeout/i.test(message) ? 'AI响应超时，请稍后再试' : '阶段总结暂时没有生成出来',
      icon: 'none',
    })
  } finally {
    summaryLoading.value = false
  }
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
    signature.value = '给想留下的此刻，一个位置'
    avatarUrl.value = user?.avatar?.trim() || ''
    archivedCount.value = sealedPage.total + unlockedPage.total
    sealedCount.value = sealedPage.total
    profileReady.value = true
  } catch {
    centerLoadFailed.value = true
    uni.showToast({ title: '网络有点慢，请稍后重试', icon: 'none' })
  } finally {
    centerLoading.value = false
  }
}

const logout = () => { userStore.logout() }

const openStageSummary = () => {
  if (summaryLoading.value) return
  if (!getToken() && hasPreviewSession()) {
    showPreviewReadonlyToast('演示模式不生成真实阶段总结')
    return
  }
  if (stageSummary.value) summaryOpen.value = true
  else return generateStageSummary()
}

const closeStageSummary = () => {
  if (!summaryLoading.value) summaryOpen.value = false
}

// 将原关于/退出收在次级入口，保持门厅默认画面简洁。
const showAboutActions = () => {
  uni.showActionSheet({
    itemList: ['关于时光回序', '退出当前账户'],
    success: ({ tapIndex }) => {
      if (tapIndex === 0) handleItemTap('about')
      else if (tapIndex === 1) logout()
    },
  })
}

onShow(() => {
  uni.hideTabBar({ animation: false })
  loadCenterData()
})
</script>

<template>
  <view class="page">
    <PreviewModeNotice />
    <view class="paper-glow" aria-hidden="true" />
    <scroll-view class="scroll-body" :scroll-y="!summaryOpen" enhanced :show-scrollbar="false">
      <AppTopSafeBar :transparent="true" padding-x="54rpx" :center-title="true">
        <template #title><text class="brand-mark">时光回序</text></template>
      </AppTopSafeBar>
      <view class="center-content">
        <view v-if="showInitialLoading" class="state-block">
          <view class="state-title">正在取回你的个人信息</view>
          <view class="state-desc">稍等片刻，记录与保管信息会在这里归位。</view>
        </view>
        <view v-else-if="showInitialFailure" class="state-block">
          <view class="state-title">个人信息暂时没有加载出来</view>
          <view class="state-desc">网络有点慢，稍后再试一次。</view>
          <button class="state-action" @tap="loadCenterData">重新加载</button>
        </view>
        <template v-else-if="profileReady">
          <view class="profile-hero">
            <view class="avatar-wrap">
              <view class="avatar" :aria-label="avatarUrl ? '个人头像' : '抽象墨韵头像'" role="img">
                <image v-if="avatarUrl" class="avatar-img" :src="avatarUrl" mode="aspectFill" @error="avatarUrl = ''" />
              </view>
            </view>
            <text class="profile-name">{{ nickname }}</text>
            <text class="custody">这里替你保管着 {{ displayArchivedCount }} 封信，还有 {{ displaySealedCount }} 封在路上。</text>
          </view>
          <view v-if="showStaleNotice" class="stale-notice">
            <text>网络有点慢，当前展示的是上次同步的信息</text>
            <button class="stale-action" @tap="loadCenterData">重试</button>
          </view>
          <view class="entrances">
            <button class="entrance entrance--records" hover-class="entrance--pressed" @tap="navigateToArchive">我的记录</button>
            <button class="entrance entrance--review" hover-class="entrance--pressed" @tap="goTimeline">时间回看</button>
            <button class="entrance entrance--summary" :disabled="summaryLoading" hover-class="entrance--pressed" @tap="openStageSummary">{{ summaryLoading ? '正在整理…' : '整理成一封近况' }}</button>
          </view>
          <view class="voucher" role="group" aria-label="托管凭证：你的数据只属于你">
            <view class="voucher-main" role="button" @tap="handleItemTap('dataOwnership')">
              <view class="custody-seal" aria-hidden="true">守</view>
              <view class="voucher-divider" aria-hidden="true" />
              <view class="voucher-copy">
                <text class="voucher-title">托管凭证</text>
                <text class="voucher-sub">你的数据只属于你</text>
              </view>
            </view>
            <view class="voucher-actions">
              <button class="voucher-button voucher-export" hover-class="voucher-button--pressed" @tap="handleItemTap('dataOwnership')">导出</button>
              <button class="voucher-button voucher-clear" hover-class="voucher-button--pressed" @tap="handleItemTap('dataOwnership')">清理</button>
            </view>
          </view>
          <button class="about-entry" @tap="showAboutActions">关于</button>
        </template>
        <view class="nav-safe-area" />
      </view>
    </scroll-view>
    <view class="bottom-nav-shell">
      <view class="bottom-nav">
        <view class="nav-item" role="button" @tap="goHome"><text class="nav-label">首页</text><view class="nav-dot nav-dot--placeholder" aria-hidden="true" /></view>
        <view class="nav-item" role="button" @tap="goTimeline"><text class="nav-label">时光轴</text><view class="nav-dot nav-dot--placeholder" aria-hidden="true" /></view>
        <view class="nav-item active"><text class="nav-label">个人中心</text><view class="nav-dot" /></view>
      </view>
    </view>
    <!-- 只有真实结果到达后打开；关闭保留结果，重新整理须再次明确点击。 -->
    <view v-if="summaryOpen && stageSummary" class="summary-layer" @tap="closeStageSummary">
      <view class="summary-sheet stage-summary-body" role="dialog" aria-modal="true" aria-label="近况书信" @tap.stop>
        <view class="summary-redline" aria-hidden="true" />
        <view class="summary-fold" aria-hidden="true" />
        <text class="summary-title">近况</text>
        <scroll-view :key="summaryRevision" class="summary-scroll" scroll-y enhanced :show-scrollbar="false">
          <view class="summary-content" aria-live="polite">
            <text class="stage-summary-text">{{ stageSummary.summary }}</text>
            <text v-if="summaryDateText" class="summary-date">整理于 {{ summaryDateText }}</text>
            <text class="stage-summary-meta">{{ stageSummary.recordCount }} 条记录 · {{ stageSummary.unlockedCount }} 次抵达 · {{ stageSummary.lifeNodeCount }} 个节点 · {{ stageSummarySourceLabel }}</text>
          </view>
        </scroll-view>
        <view class="summary-actions">
          <button class="summary-button summary-regenerate" :disabled="summaryLoading" @tap="generateStageSummary">{{ summaryLoading ? '正在整理…' : '重新整理' }}</button>
          <button class="summary-button summary-close" :disabled="summaryLoading" @tap="closeStageSummary">收起</button>
        </view>
      </view>
    </view>
  </view>
</template>

<style scoped>
/* demo 390px 画布按 750rpx 转译，系统状态栏/胶囊沿原生安全区。 */
.page {
  position: relative;
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: linear-gradient(170deg, #faf7f2 0%, #f5f0e8 55%, #f0ebe0 100%);
  color: #302e29;
  font-family: 'Noto Serif SC', 'Songti SC', 'STSong', 'SimSun', Georgia, serif;
}
.paper-glow {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(120% 86% at 20% -4%, rgba(255,255,255,.34), transparent 58%),
    radial-gradient(80% 60% at 88% 8%, rgba(255,251,242,.16), transparent 62%),
    radial-gradient(150% 90% at 50% 112%, rgba(197,181,152,.16), transparent 62%),
    radial-gradient(140% 120% at 50% 50%, transparent 62%, rgba(160,142,112,.08) 100%);
}
.scroll-body { position: relative; z-index: 1; flex: 1; height: 0; min-height: 0; width: 100%; }
.center-content { padding: 0 54rpx; box-sizing: border-box; }
.brand-mark { font-size: 23rpx; font-weight: 300; letter-spacing: .55em; text-indent: .55em; color: #9e9890; white-space: nowrap; }
.profile-hero { display: flex; flex-direction: column; align-items: center; padding-top: 26rpx; }
.avatar-wrap { width: 192rpx; height: 192rpx; flex-shrink: 0; }
.avatar {
  width: 100%; height: 100%; border-radius: 50%; overflow: hidden;
  background:
    radial-gradient(21rpx 21rpx at 64% 28%, rgba(181,53,42,.72), transparent 72%),
    radial-gradient(69rpx 54rpx at 38% 76%, rgba(96,92,86,.4), transparent 70%),
    radial-gradient(85rpx 46rpx at 64% 88%, rgba(116,110,102,.34), transparent 70%),
    radial-gradient(54rpx 38rpx at 28% 92%, rgba(80,76,70,.3), transparent 70%),
    linear-gradient(200deg, #efe9dd 0%, #e2dacc 60%, #d6ccbc 100%);
  box-shadow: 0 8rpx 27rpx rgba(125,105,75,.18), inset 0 2rpx 0 rgba(255,255,255,.5);
}
.avatar-img { display: block; width: 100%; height: 100%; }
.profile-name { display: block; max-width: 100%; margin-top: 31rpx; font-size: 42rpx; font-weight: 400; line-height: 1.4; letter-spacing: .2em; text-indent: .2em; text-align: center; overflow-wrap: anywhere; }
.custody { display: block; max-width: 100%; margin-top: 23rpx; font-size: 26rpx; font-weight: 300; line-height: 1.6; letter-spacing: .05em; color: #6b6560; text-align: center; overflow-wrap: anywhere; }
.entrance {
  position: relative; display: flex; align-items: center;
  width: 100%; min-height: 112rpx; margin: 29rpx 0 0; padding: 23rpx 42rpx; box-sizing: border-box;
  border: 1rpx solid rgba(188,174,152,.28); border-radius: 2rpx;
  background-color: rgba(252,249,244,.55);
  background-image:
    linear-gradient(rgba(107,101,96,.5), rgba(107,101,96,.5)), linear-gradient(rgba(107,101,96,.5), rgba(107,101,96,.5)),
    linear-gradient(rgba(107,101,96,.5), rgba(107,101,96,.5)), linear-gradient(rgba(107,101,96,.5), rgba(107,101,96,.5)),
    linear-gradient(rgba(107,101,96,.5), rgba(107,101,96,.5)), linear-gradient(rgba(107,101,96,.5), rgba(107,101,96,.5)),
    linear-gradient(rgba(107,101,96,.5), rgba(107,101,96,.5)), linear-gradient(rgba(107,101,96,.5), rgba(107,101,96,.5));
  background-repeat: no-repeat;
  background-size: 25rpx 1rpx, 1rpx 25rpx, 25rpx 1rpx, 1rpx 25rpx, 25rpx 1rpx, 1rpx 25rpx, 25rpx 1rpx, 1rpx 25rpx;
  background-position: 17rpx 17rpx, 17rpx 17rpx, calc(100% - 42rpx) 17rpx, calc(100% - 17rpx) 17rpx, 17rpx calc(100% - 17rpx), 17rpx calc(100% - 42rpx), calc(100% - 42rpx) calc(100% - 17rpx), calc(100% - 17rpx) calc(100% - 42rpx);
  box-shadow: none; font-family: inherit; font-size: 31rpx; font-weight: 400; line-height: 1.65; letter-spacing: .12em; color: #302e29; text-align: left;
}
.entrance::after, .voucher-button::after, .about-entry::after, .state-action::after, .stale-action::after { border: none; }
.entrance--pressed { background-color: #f3eee4; }
.entrance[disabled] { color: #8a847c; }
.voucher { margin-top: 46rpx; padding: 42rpx 42rpx 38rpx; box-sizing: border-box; border: 1rpx solid rgba(188,174,152,.28); border-radius: 2rpx; background: #fcf9f4; box-shadow: 0 2rpx 4rpx rgba(125,105,75,.1), 0 19rpx 50rpx rgba(125,105,75,.13); }
.voucher-main { display: flex; align-items: center; gap: 35rpx; }
.custody-seal { display: flex; align-items: center; justify-content: center; flex: 0 0 100rpx; width: 100rpx; height: 100rpx; box-sizing: border-box; border: 1rpx solid rgba(181,53,42,.75); border-radius: 50%; color: #b5352a; font-size: 37rpx; font-weight: 400; }
.voucher-divider { flex: 0 0 1rpx; width: 1rpx; height: 100rpx; background: rgba(188,174,152,.28); }
.voucher-copy { flex: 1; min-width: 0; }
.voucher-title { display: block; font-size: 37rpx; font-weight: 400; line-height: 1.4; letter-spacing: .1em; }
.voucher-sub { display: block; margin-top: 13rpx; font-size: 24rpx; font-weight: 300; line-height: 1.5; letter-spacing: .06em; color: #6b6560; }
.voucher-actions { display: flex; justify-content: flex-end; gap: 23rpx; margin-top: 31rpx; }
.voucher-button { display: inline-flex; align-items: center; justify-content: center; min-height: 62rpx; min-width: 135rpx; margin: 0; padding: 8rpx 36rpx; box-sizing: border-box; border: 1rpx solid rgba(107,101,96,.5); border-radius: 2rpx; background: transparent; color: #6b6560; font-family: inherit; font-size: 24rpx; font-weight: 300; line-height: 1.5; letter-spacing: .14em; text-indent: .14em; }
.voucher-button--pressed { background: #f3eee4; }
.about-entry { width: 100%; margin: 27rpx 0 0; padding: 15rpx 0; min-height: 76rpx; box-sizing: border-box; background: transparent; font-family: inherit; font-size: 23rpx; font-weight: 300; line-height: 2; letter-spacing: .3em; text-indent: .3em; color: #c8c2b8; }
.summary-layer { position: fixed; inset: 0; z-index: 120; background: rgba(96,89,79,.42); }
.summary-sheet {
  position: absolute; left: 84rpx; right: 84rpx; top: 29.15vh;
  display: flex; flex-direction: column; box-sizing: border-box;
  height: 980rpx; max-height: calc(70.85vh - 38rpx - env(safe-area-inset-bottom));
  padding: 58rpx 50rpx 50rpx 58rpx;
  background: #fcf9f4; border: 1rpx solid rgba(188,174,152,.28); border-radius: 2rpx;
  box-shadow: 0 2rpx 4rpx rgba(125,105,75,.1), 0 19rpx 50rpx rgba(125,105,75,.13), inset 0 2rpx 0 rgba(255,255,255,.6);
}
.summary-redline { position: absolute; left: 0; top: 0; bottom: 0; width: 3rpx; pointer-events: none; background: linear-gradient(to bottom, transparent, rgba(181,53,42,.5) 15%, rgba(181,53,42,.5) 85%, transparent); }
.summary-fold { position: absolute; top: -1rpx; right: -1rpx; width: 35rpx; height: 35rpx; pointer-events: none; background: linear-gradient(to bottom left, #f5f0e8 0%, #f5f0e8 50%, #e4ddca 50.5%, #e4ddca 100%); box-shadow: -4rpx 4rpx 6rpx rgba(120,100,70,.16); }
.summary-title { display: block; flex-shrink: 0; font-size: 50rpx; font-weight: 400; line-height: 1.4; letter-spacing: .3em; text-indent: .3em; color: #302e29; }
.summary-scroll { flex: 1; height: 0; min-height: 0; margin-top: 38rpx; }
.summary-content { padding-bottom: 4rpx; }
.stage-summary-text { display: block; font-size: 27rpx; font-weight: 300; line-height: 2.15; letter-spacing: .05em; color: #6b6560; white-space: pre-wrap; overflow-wrap: anywhere; }
.summary-date { display: block; margin-top: 34rpx; font-size: 23rpx; font-weight: 300; line-height: 1.8; letter-spacing: .08em; color: #9e9890; overflow-wrap: anywhere; }
.stage-summary-meta { display: block; margin-top: 12rpx; font-size: 21rpx; font-weight: 300; line-height: 1.8; color: #9e9890; overflow-wrap: anywhere; }
.summary-actions { display: flex; flex-shrink: 0; justify-content: center; flex-wrap: wrap; gap: 27rpx; margin-top: 34rpx; }
.summary-button { display: inline-flex; align-items: center; justify-content: center; min-height: 73rpx; margin: 0; padding: 10rpx 40rpx; box-sizing: border-box; border: 1rpx solid rgba(107,101,96,.5); border-radius: 2rpx; background: transparent; color: #6b6560; font-family: inherit; font-size: 26rpx; font-weight: 300; line-height: 1.5; letter-spacing: .12em; text-indent: .12em; }
.summary-button::after { border: none; }
.summary-button[disabled] { background: transparent; color: #9e9890; opacity: .6; }
.state-block { margin-top: 64rpx; padding: 42rpx; background: #fcf9f4; border: 1rpx solid rgba(188,174,152,.28); }
.state-title { font-size: 34rpx; line-height: 1.5; }
.state-desc { margin-top: 18rpx; font-size: 26rpx; color: #6b6560; line-height: 1.7; }
.state-action { display: inline-flex; align-items: center; justify-content: center; min-height: 80rpx; margin: 28rpx 0 0; padding: 12rpx 32rpx; border: 1rpx solid #c8c2b8; border-radius: 2rpx; background: transparent; font-family: inherit; font-size: 26rpx; color: #6b6560; }
.stale-notice { display: flex; align-items: center; gap: 12rpx; margin-top: 24rpx; font-size: 23rpx; line-height: 1.6; color: #8a847c; }
.stale-action { flex-shrink: 0; margin: 0; padding: 12rpx; font-size: 23rpx; line-height: 1.6; font-family: inherit; background: transparent; color: #6b6560; }
.nav-safe-area { height: calc(190rpx + env(safe-area-inset-bottom)); }
.bottom-nav-shell { position: fixed; left: 0; right: 0; bottom: 0; z-index: 30; flex-shrink: 0; padding: 0; background: transparent; pointer-events: none; }
.bottom-nav { display: flex; align-items: stretch; min-height: 116rpx; padding-bottom: env(safe-area-inset-bottom); box-sizing: content-box; border: 0; border-top: 1rpx solid rgba(188,174,152,.28); border-radius: 24rpx 24rpx 0 0; background: rgba(247,242,233,.9); box-shadow: 0 -4rpx 36rpx rgba(125,105,75,.1); pointer-events: auto; }
@supports ((backdrop-filter: blur(1px)) or (-webkit-backdrop-filter: blur(1px))) {
  .bottom-nav {
    /* autoprefixer: off */
    background: rgba(250,247,242,.64);
    -webkit-backdrop-filter: blur(18px) saturate(125%);
    backdrop-filter: blur(18px) saturate(125%);
  }
}
.nav-item { flex: 1; min-width: 0; min-height: 88rpx; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8rpx; padding: 14rpx 4rpx 10rpx; box-sizing: border-box; }
.nav-label { font-size: 26rpx; font-weight: 300; line-height: 1.5; letter-spacing: .2em; text-indent: .2em; color: #9e9890; white-space: nowrap; }
.nav-item.active .nav-label { color: #302e29; }
.nav-dot { width: 6rpx; height: 6rpx; border-radius: 50%; background: #b5352a; }
.nav-dot--placeholder { visibility: hidden; }
</style>
