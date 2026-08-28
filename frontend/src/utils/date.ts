type DateInput = string | number | Date | null | undefined

const parseDate = (value: DateInput): Date | null => {
  if (!value && value !== 0) {
    return null
  }

  if (value instanceof Date) {
    return Number.isNaN(value.getTime()) ? null : value
  }

  if (typeof value === 'number') {
    const date = new Date(value)
    return Number.isNaN(date.getTime()) ? null : date
  }

  if (typeof value === 'string') {
    const normalized = value.includes('T') ? value : value.replace(' ', 'T')
    const date = new Date(normalized)
    return Number.isNaN(date.getTime()) ? null : date
  }

  return null
}

const toDatetimeText = (date: Date) => {
  const yyyy = date.getFullYear()
  const mm = String(date.getMonth() + 1).padStart(2, '0')
  const dd = String(date.getDate()).padStart(2, '0')
  const hh = String(date.getHours()).padStart(2, '0')
  const min = String(date.getMinutes()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd} ${hh}:${min}`
}

export const formatDateTime = (value?: DateInput) => {
  const date = parseDate(value)
  if (!date) {
    return '-'
  }
  return toDatetimeText(date)
}

export const formatYearMonth = (value?: DateInput) => {
  const date = parseDate(value)
  if (!date) {
    return ''
  }
  return `${date.getFullYear()}年${date.getMonth() + 1}月`
}

export const formatDayText = (value?: DateInput) => {
  const date = parseDate(value)
  if (!date) {
    return '-'
  }
  return `${date.getFullYear()}年${String(date.getMonth() + 1).padStart(2, '0')}月${String(date.getDate()).padStart(2, '0')}日`
}

export const toLocalDateTime = (value: DateInput) => {
  const date = parseDate(value)
  if (!date) {
    return ''
  }

  const yyyy = date.getFullYear()
  const mm = String(date.getMonth() + 1).padStart(2, '0')
  const dd = String(date.getDate()).padStart(2, '0')
  const hh = String(date.getHours()).padStart(2, '0')
  const min = String(date.getMinutes()).padStart(2, '0')
  const ss = String(date.getSeconds()).padStart(2, '0')

  return `${yyyy}-${mm}-${dd}T${hh}:${min}:${ss}`
}

export const isUnlockTimeReached = (unlockAt?: DateInput) => {
  const date = parseDate(unlockAt)
  if (!date) {
    return false
  }
  return Date.now() >= date.getTime()
}

export const calculateRemainingDays = (unlockAt?: DateInput) => {
  const date = parseDate(unlockAt)
  if (!date) {
    return 0
  }

  const diffMs = date.getTime() - Date.now()
  if (diffMs <= 0) {
    return 0
  }

  const dayMs = 24 * 60 * 60 * 1000
  return Math.ceil(diffMs / dayMs)
}
