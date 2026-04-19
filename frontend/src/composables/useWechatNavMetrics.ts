import { onShow } from '@dcloudio/uni-app'
import { computed, reactive, toRefs } from 'vue'

interface MenuButtonBoundingRect {
  top: number
  right: number
  bottom: number
  left: number
  width: number
  height: number
}

interface WechatNavMetricsState {
  statusBarHeight: number
  menuButtonBoundingClientRect: MenuButtonBoundingRect
  navBarHeight: number
  navBarTotalHeight: number
  rightSafeWidth: number
  windowWidth: number
}

const DEFAULT_WINDOW_WIDTH = 375
const DEFAULT_STATUS_BAR_HEIGHT = 20
const DEFAULT_MENU_BUTTON_WIDTH = 96
const DEFAULT_MENU_BUTTON_HEIGHT = 32
const DEFAULT_MENU_BUTTON_MARGIN_TOP = 6
const DEFAULT_MENU_BUTTON_MARGIN_RIGHT = 10
const DEFAULT_NAV_BAR_HEIGHT = 44

const isFiniteNumber = (value: unknown): value is number =>
  typeof value === 'number' && Number.isFinite(value)

const normalizeMenuButtonRect = (
  rect: Partial<MenuButtonBoundingRect> | null | undefined
): MenuButtonBoundingRect | null => {
  if (
    !rect ||
    !isFiniteNumber(rect.top) ||
    !isFiniteNumber(rect.right) ||
    !isFiniteNumber(rect.bottom) ||
    !isFiniteNumber(rect.left) ||
    !isFiniteNumber(rect.width) ||
    !isFiniteNumber(rect.height)
  ) {
    return null
  }

  if (rect.width <= 0 || rect.height <= 0) {
    return null
  }

  return {
    top: rect.top,
    right: rect.right,
    bottom: rect.bottom,
    left: rect.left,
    width: rect.width,
    height: rect.height,
  }
}

const getMenuButtonBoundingRect = (): MenuButtonBoundingRect | null => {
  const uniWithMenuButton = uni as typeof uni & {
    getMenuButtonBoundingClientRect?: () => Partial<MenuButtonBoundingRect>
  }

  const rectFromUni = normalizeMenuButtonRect(
    uniWithMenuButton.getMenuButtonBoundingClientRect?.()
  )

  if (rectFromUni) {
    return rectFromUni
  }

  const globalWechat = globalThis as {
    wx?: { getMenuButtonBoundingClientRect?: () => Partial<MenuButtonBoundingRect> }
  }

  return normalizeMenuButtonRect(globalWechat.wx?.getMenuButtonBoundingClientRect?.())
}

function createFallbackMenuButtonRect(
  statusBarHeight: number,
  windowWidth: number
): MenuButtonBoundingRect {
  const top = statusBarHeight + DEFAULT_MENU_BUTTON_MARGIN_TOP
  const height = DEFAULT_MENU_BUTTON_HEIGHT
  const width = DEFAULT_MENU_BUTTON_WIDTH
  const right = Math.max(windowWidth - DEFAULT_MENU_BUTTON_MARGIN_RIGHT, width)
  const left = Math.max(right - width, 0)

  return {
    top,
    bottom: top + height,
    left,
    right,
    width,
    height,
  }
}

const metricsState = reactive<WechatNavMetricsState>({
  statusBarHeight: DEFAULT_STATUS_BAR_HEIGHT,
  menuButtonBoundingClientRect: createFallbackMenuButtonRect(
    DEFAULT_STATUS_BAR_HEIGHT,
    DEFAULT_WINDOW_WIDTH
  ),
  navBarHeight: DEFAULT_NAV_BAR_HEIGHT,
  navBarTotalHeight: DEFAULT_STATUS_BAR_HEIGHT + DEFAULT_NAV_BAR_HEIGHT,
  rightSafeWidth: DEFAULT_MENU_BUTTON_WIDTH + DEFAULT_MENU_BUTTON_MARGIN_RIGHT,
  windowWidth: DEFAULT_WINDOW_WIDTH,
})

function resolveWechatNavMetrics(): WechatNavMetricsState {
  const systemInfo = uni.getSystemInfoSync()
  const windowWidth = systemInfo.windowWidth || systemInfo.screenWidth || DEFAULT_WINDOW_WIDTH
  const statusBarHeight = systemInfo.statusBarHeight || DEFAULT_STATUS_BAR_HEIGHT
  const menuButtonBoundingClientRect =
    getMenuButtonBoundingRect() || createFallbackMenuButtonRect(statusBarHeight, windowWidth)
  const navGap = Math.max(menuButtonBoundingClientRect.top - statusBarHeight, 4)
  const navBarHeight = Math.max(
    menuButtonBoundingClientRect.height + navGap * 2,
    DEFAULT_NAV_BAR_HEIGHT
  )
  const navBarTotalHeight = statusBarHeight + navBarHeight
  const rightSafeWidth = Math.max(windowWidth - menuButtonBoundingClientRect.left, 0)

  return {
    statusBarHeight,
    menuButtonBoundingClientRect,
    navBarHeight,
    navBarTotalHeight,
    rightSafeWidth,
    windowWidth,
  }
}

export const refreshWechatNavMetrics = () => {
  Object.assign(metricsState, resolveWechatNavMetrics())
}

export const useWechatNavMetrics = () => {
  refreshWechatNavMetrics()

  onShow(() => {
    refreshWechatNavMetrics()
  })

  const cssVars = computed(() => ({
    '--wechat-status-bar-height': `${metricsState.statusBarHeight}px`,
    '--wechat-nav-bar-height': `${metricsState.navBarHeight}px`,
    '--wechat-nav-total-height': `${metricsState.navBarTotalHeight}px`,
    '--wechat-right-safe-width': `${metricsState.rightSafeWidth}px`,
  }))

  return {
    ...toRefs(metricsState),
    cssVars,
    refreshWechatNavMetrics,
  }
}
