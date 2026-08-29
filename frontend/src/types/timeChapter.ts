import type { PaginationResponse } from './api'
import type { DateTimeValue, RecordListItemVO } from './record'
import type { TimeChapterStatus } from './enums'

export interface RecordChapterSummaryVO {
  id: number
  name: string
  status: TimeChapterStatus
}

export interface TimeChapterSummaryVO {
  id: number
  name: string
  note?: string | null
  status: TimeChapterStatus
  memberCount: number
  coverageStartAt?: DateTimeValue | null
  coverageEndAt?: DateTimeValue | null
  endedAt?: DateTimeValue | null
  version: number
  createdAt: DateTimeValue
  updatedAt: DateTimeValue
}

export interface TimeChapterDetailVO extends TimeChapterSummaryVO {
  members: PaginationResponse<RecordListItemVO>
}

export interface TransferConfirmationDTO {
  recordId: number
  fromChapterId: number
}

export interface CreateTimeChapterDTO {
  name: string
  note?: string | null
  recordIds: number[]
  transfers?: TransferConfirmationDTO[]
}

export interface UpdateTimeChapterDTO {
  name: string
  note?: string | null
  expectedVersion: number
}

export interface ChangeTimeChapterMembersDTO {
  recordIds: number[]
  transfers?: TransferConfirmationDTO[]
  expectedVersion: number
}

export interface TimeChapterVersionDTO {
  expectedVersion: number
}

export type TimeChapterOrder = 'ASC' | 'DESC'
