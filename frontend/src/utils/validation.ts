export const validateUsername = (value: string) => {
  return value.trim().length >= 3
}

export const validatePassword = (value: string) => {
  return value.trim().length >= 6
}

export const validateNickname = (value: string) => {
  return value.trim().length > 0 && value.trim().length <= 50
}

export const validateRecordContent = (value: string) => {
  const length = value.trim().length
  return length > 0 && length <= 5000
}
