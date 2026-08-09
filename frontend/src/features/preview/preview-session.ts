import { isPreviewModeEnabled } from '../../config/app-env'

const PREVIEW_SESSION_KEY = 'flashback:preview-session'
const PREVIEW_TOAST_TITLE = '概念预览为只读，不会保存修改'

interface PreviewSession {
  enabled: true
  enteredAt: number
}

export const createPreviewSession = () => {
  if (!isPreviewModeEnabled) {
    return
  }

  const session: PreviewSession = {
    enabled: true,
    enteredAt: Date.now(),
  }

  uni.setStorageSync(PREVIEW_SESSION_KEY, session)
}

export const getPreviewSession = (): PreviewSession | null => {
  if (!isPreviewModeEnabled) {
    return null
  }

  const session = uni.getStorageSync(PREVIEW_SESSION_KEY) as PreviewSession | undefined
  if (!session || typeof session !== 'object' || session.enabled !== true) {
    return null
  }

  return session
}

export const hasPreviewSession = () => Boolean(getPreviewSession())

export const clearPreviewSession = () => {
  uni.removeStorageSync(PREVIEW_SESSION_KEY)
}

export const showPreviewReadonlyToast = (title = PREVIEW_TOAST_TITLE) => {
  uni.showToast({ title, icon: 'none' })
}
