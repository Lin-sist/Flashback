import { httpRequest } from './httpClient'
import type {
  CreateRecordDTO,
  PageQuery,
  PaginationResponse,
  RecordDetailVO,
  RecordListItemVO,
  RecordStatus,
} from '../types'

export const recordService = {
  createDraft(payload: CreateRecordDTO) {
    return httpRequest<RecordDetailVO>({
      url: '/api/records',
      method: 'POST',
      data: payload as unknown as Record<string, unknown>,
    })
  },
  updateDraft(id: string, payload: CreateRecordDTO) {
    return httpRequest<RecordDetailVO>({
      url: `/api/records/${id}`,
      method: 'PUT',
      data: payload as unknown as Record<string, unknown>,
    })
  },
  deleteDraft(id: string) {
    return httpRequest<void>({
      url: `/api/records/${id}`,
      method: 'DELETE',
    })
  },
  sealRecord(id: string, unlockAt: number) {
    return httpRequest<void>({
      url: `/api/records/${id}/seal`,
      method: 'POST',
      data: { unlockAt },
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
  getRecordDetail(id: string) {
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
  getTimeline(pageNum = 1, pageSize = 20) {
    const params = new URLSearchParams({ pageNum: String(pageNum), pageSize: String(pageSize) })
    return httpRequest<PaginationResponse<RecordListItemVO>>({
      url: `/api/records/timeline?${params.toString()}`,
    })
  },
}
