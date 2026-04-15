import { RecordStatus, RecordType } from '../types'

export const mapRecordStatus = (status: RecordStatus) => {
  if (status === RecordStatus.DRAFT) {
    return 'Draft'
  }
  if (status === RecordStatus.SEALED) {
    return 'Sealed'
  }
  return 'Unlocked'
}

export const mapRecordType = (recordType: RecordType) => {
  if (recordType === RecordType.FUTURE_LETTER) {
    return 'Future Letter'
  }
  if (recordType === RecordType.NODE_RECORD) {
    return 'Node Record'
  }
  return 'Emotion Note'
}
