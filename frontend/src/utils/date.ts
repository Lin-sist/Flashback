export const formatDateTime = (timestamp?: number) => {
  if (!timestamp) {
    return '-'
  }

  const date = new Date(timestamp)
  const yyyy = date.getFullYear()
  const mm = String(date.getMonth() + 1).padStart(2, '0')
  const dd = String(date.getDate()).padStart(2, '0')
  const hh = String(date.getHours()).padStart(2, '0')
  const min = String(date.getMinutes()).padStart(2, '0')

  return `${yyyy}-${mm}-${dd} ${hh}:${min}`
}

export const isUnlockTimeReached = (unlockAt?: number) => {
  if (!unlockAt) {
    return false
  }
  return Date.now() >= unlockAt
}
