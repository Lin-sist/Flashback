<script setup lang="ts">
import { computed, ref, watch } from 'vue'

const props = defineProps<{
  visible: boolean
  initialValue: string | null
}>()

const emit = defineEmits<{
  confirm: [value: string]
  cancel: []
}>()

// ── 数据源 ──
const getYearRange = () => {
  const cur = new Date().getFullYear()
  const arr: number[] = []
  for (let i = 0; i <= 10; i++) arr.push(cur + i)
  return arr
}

const months = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]
const hours = Array.from({ length: 24 }, (_, i) => i)
const minutes = Array.from({ length: 60 }, (_, i) => i)

const getDaysInMonth = (year: number, month: number) =>
  new Date(year, month, 0).getDate()

// ── 状态 ──
const years = ref<number[]>(getYearRange())
const yearIndex = ref(0)
const monthIndex = ref(0)
const dayIndex = ref(0)
const hourIndex = ref(0)
const minuteIndex = ref(0)

const days = computed(() => {
  const y = years.value[yearIndex.value]
  const m = months[monthIndex.value]
  return Array.from({ length: getDaysInMonth(y, m) }, (_, i) => i + 1)
})

const pickerValue = computed(() => [
  yearIndex.value,
  monthIndex.value,
  dayIndex.value,
  hourIndex.value,
  minuteIndex.value,
])

// ── 工具函数 ──
const parseInitial = (val: string) => {
  const parts = val.split(' ')
  if (parts.length !== 2) return null
  const dateParts = parts[0].split('-')
  const timeParts = parts[1].split(':')
  if (dateParts.length !== 3 || timeParts.length !== 2) return null
  const year = parseInt(dateParts[0])
  const month = parseInt(dateParts[1])
  const day = parseInt(dateParts[2])
  const hour = parseInt(timeParts[0])
  const minute = parseInt(timeParts[1])
  if (isNaN(year) || isNaN(month) || isNaN(day) || isNaN(hour) || isNaN(minute)) return null
  return { year, month, day, hour, minute }
}

const roundUpNextHour = () => {
  const now = new Date()
  now.setHours(now.getHours() + 1, 0, 0, 0)
  return {
    year: now.getFullYear(),
    month: now.getMonth() + 1,
    day: now.getDate(),
    hour: now.getHours(),
    minute: 0,
  }
}

const initIndices = () => {
  let target: { year: number; month: number; day: number; hour: number; minute: number }

  if (props.initialValue) {
    const parsed = parseInitial(props.initialValue)
    target = parsed ?? roundUpNextHour()
  } else {
    target = roundUpNextHour()
  }

  years.value = getYearRange()

  yearIndex.value = years.value.indexOf(target.year)
  if (yearIndex.value === -1) {
    yearIndex.value = 0
  }

  monthIndex.value = target.month - 1
  const maxDay = getDaysInMonth(years.value[yearIndex.value], target.month)
  dayIndex.value = Math.min(target.day, maxDay) - 1
  hourIndex.value = target.hour
  minuteIndex.value = target.minute
}

// ── 事件处理 ──
const onPickerChange = (e: { detail: { value: number[] } }) => {
  const [yIdx, mIdx, dIdx, hIdx, miIdx] = e.detail.value
  const prevYearIdx = yearIndex.value
  const prevMonthIdx = monthIndex.value

  yearIndex.value = yIdx
  monthIndex.value = mIdx

  if (yIdx !== prevYearIdx || mIdx !== prevMonthIdx) {
    const maxDay = getDaysInMonth(years.value[yIdx], months[mIdx])
    dayIndex.value = Math.min(dIdx, maxDay - 1)
  } else {
    dayIndex.value = dIdx
  }

  hourIndex.value = hIdx
  minuteIndex.value = miIdx
}

const onConfirm = () => {
  const y = years.value[yearIndex.value]
  const m = String(months[monthIndex.value]).padStart(2, '0')
  const d = String(days.value[dayIndex.value]).padStart(2, '0')
  const h = String(hours[hourIndex.value]).padStart(2, '0')
  const mi = String(minutes[minuteIndex.value]).padStart(2, '0')
  emit('confirm', `${y}-${m}-${d} ${h}:${mi}`)
}

const onOverlayTap = () => {
  emit('cancel')
}

// ── 监听 visible ──
watch(() => props.visible, (val) => {
  if (val) {
    initIndices()
  }
})
</script>

<template>
  <view v-if="visible" class="dt-overlay" @tap.stop="onOverlayTap">
    <view class="dt-sheet">
      <!-- 头部按钮 -- 放在滚轮上方 -->
      <cover-view class="dt-header">
        <cover-view class="dt-btn dt-btn--cancel" @tap="onOverlayTap">取消</cover-view>
        <cover-view class="dt-title">设定解封时间</cover-view>
        <cover-view class="dt-btn dt-btn--confirm" @tap="onConfirm">确定</cover-view>
      </cover-view>

      <view class="dt-wheel-wrap">
        <picker-view
          class="dt-wheel"
          :value="pickerValue"
          indicator-class="dt-indicator"
          @change="onPickerChange"
        >
          <picker-view-column>
            <view class="dt-item" v-for="y in years" :key="y">{{ y }}年</view>
          </picker-view-column>
          <picker-view-column>
            <view class="dt-item" v-for="m in months" :key="m">{{ m }}月</view>
          </picker-view-column>
          <picker-view-column>
            <view class="dt-item" v-for="d in days" :key="d">{{ d }}日</view>
          </picker-view-column>
          <picker-view-column>
            <view class="dt-item dt-item--narrow" v-for="h in hours" :key="h">{{ h }}时</view>
          </picker-view-column>
          <picker-view-column>
            <view class="dt-item dt-item--narrow" v-for="mi in minutes" :key="mi">{{ mi }}分</view>
          </picker-view-column>
        </picker-view>
      </view>
    </view>
  </view>
</template>

<style scoped>
/* 遮罩层 */
.dt-overlay {
  position: fixed;
  inset: 0;
  z-index: 200;
  background: rgba(48, 46, 41, 0.45);
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
}

/* 底部面板 */
.dt-sheet {
  background: rgba(252, 249, 244, 0.97);
  border-radius: 24rpx 24rpx 0 0;
  padding-bottom: env(safe-area-inset-bottom, 48rpx);
  position: relative;
}

/* 头部 — 位于滚轮上方 */
.dt-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 32rpx 48rpx 24rpx;
  border-bottom: 1rpx solid rgba(192, 182, 165, 0.2);
}

.dt-btn {
  font-size: 28rpx;
  line-height: 1.4;
  min-width: 80rpx;
}

.dt-btn--cancel {
  color: #9e9890;
}

.dt-btn--confirm {
  color: #b5352a;
  text-align: right;
  font-weight: 500;
}

.dt-title {
  font-size: 26rpx;
  color: #302e29;
}

/* 滚轮容器 */
.dt-wheel-wrap {
  position: relative;
  padding: 0 16rpx 32rpx;
}

.dt-wheel {
  width: 100%;
  height: 420rpx;
}

/* 选中指示线 */
:deep(.dt-indicator) {
  height: 72rpx;
  border-top: 1rpx solid rgba(192, 182, 165, 0.35);
  border-bottom: 1rpx solid rgba(192, 182, 165, 0.35);
}

/* 滚轮选项 */
.dt-item {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 72rpx;
  font-size: 34rpx;
  color: #302e29;
}

.dt-item--narrow {
  font-size: 30rpx;
}
</style>
