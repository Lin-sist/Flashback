import type { LifeNodeType, RecordReminderStatus, RecordStatus, RecordType } from './enums'
import type { TagVO } from './tag'

export type DateTimeValue = string | number

export interface CreateRecordDTO {
  title?: string
  content: string
  recordType: RecordType
  coreQuestion?: string
  aiSummary?: string | null
  aiPromptResults?: string[] | null
  beliefThen?: string | null
  lifeNodeType?: LifeNodeType | null
  lifeNodeCustomLabel?: string | null
  tagIds?: number[] | null
  unlockAt?: string | null
}

export interface UpdateRecordDTO extends CreateRecordDTO { }

export interface TimelineQuery {
  year?: number
  tagId?: number
}

export interface RecordListItemVO {
  id: number
  title: string
  contentPreview: string
  recordType: RecordType
  status: RecordStatus
  lifeNodeLabel?: string
  unlockAt?: DateTimeValue
  createdAt: DateTimeValue
  tagNames: string[]
}

export interface RecordDetailVO {
  id: number
  title: string
  content: string
  recordType: RecordType
  coreQuestion?: string
  status: RecordStatus
  unlockAt?: DateTimeValue
  sealedAt?: DateTimeValue
  unlockedAt?: DateTimeValue
  aiSummary?: string
  aiPromptResults?: string[]
  beliefThen?: string
  realityLater?: string
  realityLaterSubmitCount?: number
  lifeNodeType?: LifeNodeType
  lifeNodeCustomLabel?: string
  lifeNodeLabel?: string
  unlockReminderStatus?: RecordReminderStatus
  tags: TagVO[]
  canReply: boolean
  hasReply: boolean
  createdAt: DateTimeValue
  updatedAt: DateTimeValue
}

export interface TimelineItemVO {
  id: number
  title: string
  status: RecordStatus
  recordType: RecordType
  lifeNodeLabel?: string
  createdAt: DateTimeValue
  tagNames: string[]
}

export interface TimelineGroupVO {
  yearMonth: string
  items: TimelineItemVO[]
}
