import type { LifeNodeType, RecordReminderStatus, RecordStatus, RecordType } from './enums'
import type { TagVO } from './tag'

export type DateTimeValue = string | number
export type RecordLocationSource = 'CURRENT_LOCATION' | 'MAP_PICKER' | 'MANUAL'

export interface RecordLocationVO {
  source: RecordLocationSource
  name?: string | null
  address?: string | null
  latitude?: number | null
  longitude?: number | null
}

export interface UpdateRecordLocationDTO extends RecordLocationVO { }

export interface UpdateRecordCoverDTO {
  attachmentId: number | null
}

export type RecordAttachmentType = 'IMAGE' | 'VOICE'
export type RecordAttachmentStatus = 'AVAILABLE' | 'DELETED'

export interface RecordAttachmentVO {
  id: number
  recordId: number
  type: RecordAttachmentType
  status: RecordAttachmentStatus
  fileName: string
  mimeType: string
  sizeBytes: number
  width?: number | null
  height?: number | null
  durationSeconds?: number | null
  sortOrder: number
  createdAt: DateTimeValue
  accessUrl?: string | null
}

export interface CreateAttachmentUploadTokenDTO {
  type: RecordAttachmentType
  fileName: string
  mimeType: string
  sizeBytes: number
}

export interface AttachmentUploadTokenVO {
  provider: 'QINIU'
  bucket: string
  key: string
  uploadToken: string
  uploadUrl: string
  expiresAt: DateTimeValue
  maxFileSizeBytes: number
}

export interface CommitRecordAttachmentDTO extends CreateAttachmentUploadTokenDTO {
  key: string
  width?: number | null
  height?: number | null
  durationSeconds?: number | null
}

export interface AttachmentAccessUrlVO {
  attachmentId: number
  url: string
  expiresAt: DateTimeValue
}

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
  cover?: RecordAttachmentVO | null
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
  location?: RecordLocationVO | null
  attachments?: RecordAttachmentVO[]
  cover?: RecordAttachmentVO | null
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
  cover?: RecordAttachmentVO | null
}

export interface TimelineGroupVO {
  yearMonth: string
  items: TimelineItemVO[]
}
