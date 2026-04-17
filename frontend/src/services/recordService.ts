import { httpRequest } from './httpClient'
import type {
  CreateRecordDTO,
  PageQuery,
  PaginationResponse,
  RecordDetailVO,
  RecordListItemVO,
  RecordStatus,
  TimelineGroupVO,
  TimelineQuery,
  UpdateRecordDTO,
} from '../types'

const buildQueryString = (params: Record<string, string | number | undefined>) => {
  const entries = Object.entries(params).filter(([, value]) => value !== undefined && value !== null && value !== '')
  if (entries.length === 0) {
    return ''
  }

  return `?${entries
    .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(String(value))}`)
    .join('&')}`
}

const toDraftPayload = (payload: CreateRecordDTO | UpdateRecordDTO) => ({
  title: payload.title ?? null,
  content: payload.content,
  recordType: payload.recordType,
  coreQuestion: payload.coreQuestion ?? null,
  aiSummary: payload.aiSummary ?? null,
  aiPromptResults: payload.aiPromptResults ?? [],
  unlockAt: payload.unlockAt ?? null,
  tagIds: payload.tagIds ?? [],
})

export const recordService = {
  createDraft(payload: CreateRecordDTO) {
    return httpRequest<RecordDetailVO>({
      url: '/api/records',
      method: 'POST',
      data: toDraftPayload(payload),
    })
  },
  updateDraft(id: string | number, payload: UpdateRecordDTO) {
    return httpRequest<RecordDetailVO>({
      url: `/api/records/${id}`,
      method: 'PUT',
      data: toDraftPayload(payload),
    })
  },
  deleteDraft(id: string | number) {
    return httpRequest<void>({
      url: `/api/records/${id}`,
      method: 'DELETE',
    })
  },
  sealRecord(id: string | number) {
    return httpRequest<RecordDetailVO>({
      url: `/api/records/${id}/seal`,
      method: 'POST',
    })
  },
  getRecordList(status: RecordStatus | 'ALL', query: PageQuery) {
    return httpRequest<PaginationResponse<RecordListItemVO>>({
      url: `/api/records${buildQueryString({
        pageNum: query.pageNum,
        pageSize: query.pageSize,
        ...(status === 'ALL' ? {} : { status }),
      })}`,
    })
  },
  getRecordDetail(id: string | number) {
    return httpRequest<RecordDetailVO>({
      url: `/api/records/${id}`,
    })
  },
  getUnlockedRecords(pageNum = 1, pageSize = 10) {
    return httpRequest<PaginationResponse<RecordListItemVO>>({
      url: `/api/records/unlocked${buildQueryString({ pageNum, pageSize })}`,
    })
  },
  getTimeline(query: TimelineQuery = {}) {
    return httpRequest<TimelineGroupVO[]>({
      url: `/api/records/timeline${buildQueryString({
        year: query.year,
        tagId: query.tagId,
      })}`,
    })
  },
}
