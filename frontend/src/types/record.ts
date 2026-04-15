import type { RecordStatus, RecordType } from './enums'
import type { TagVO } from './tag'

export interface CreateRecordDTO {
  title?: string
  content: string
  recordType: RecordType
  coreQuestion?: string
  tagIds?: string[]
  unlockAt?: number
}

export interface SealRecordDTO {
  id: string
  unlockAt: number
}

export interface RecordListItemVO {
  id: string
  title: string
  contentPreview: string
  recordType: RecordType
  status: RecordStatus
  unlockAt?: number
  createdAt: number
  tagNames: string[]
}

export interface RecordDetailVO {
  id: string
  title: string
  content: string
  recordType: RecordType
  coreQuestion?: string
  status: RecordStatus
  unlockAt?: number
  sealedAt?: number
  unlockedAt?: number
  aiSummary?: string
  aiPromptResults?: string[]
  tags: TagVO[]
  canReply: boolean
  hasReply: boolean
  createdAt: number
  updatedAt: number
}
