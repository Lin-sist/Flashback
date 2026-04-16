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
    const params = new URLSearchParams({
      pageNum: String(query.pageNum),
      pageSize: String(query.pageSize),
      ...(status === 'ALL' ? {} : { status }),
    })

    return httpRequest<PaginationResponse<RecordListItemVO>>({
      url: `/api/records?${params.toString()}`,
    })
  },
  getRecordDetail(id: string | number) {
    return httpRequest<RecordDetailVO>({
      url: `/api/records/${id}`,
    })
  },
  getUnlockedRecords(pageNum = 1, pageSize = 10) {
    const params = new URLSearchParams({ pageNum: String(pageNum), pageSize: String(pageSize) })
    return httpRequest<PaginationResponse<RecordListItemVO>>({
      url: `/api/records/unlocked?${params.toString()}`,
    })
  },
  getTimeline(query: TimelineQuery = {}) {
    const params = new URLSearchParams({
      ...(query.year ? { year: String(query.year) } : {}),
      ...(query.tagId ? { tagId: String(query.tagId) } : {}),
    })
    const suffix = params.toString() ? `?${params.toString()}` : ''
    return httpRequest<TimelineGroupVO[]>({
      url: `/api/records/timeline${suffix}`,
    })
  },
}
