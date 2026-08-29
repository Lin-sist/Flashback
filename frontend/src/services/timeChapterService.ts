import { getPreviewTimeChapterDetail, getPreviewTimeChapterList } from '../features/preview/data/preview-data'
import { hasPreviewSession, showPreviewReadonlyToast } from '../features/preview/preview-session'
import type {
  ChangeTimeChapterMembersDTO,
  CreateTimeChapterDTO,
  PaginationResponse,
  TimeChapterDetailVO,
  TimeChapterOrder,
  TimeChapterSummaryVO,
  TimeChapterStatus,
  TimeChapterVersionDTO,
  UpdateTimeChapterDTO,
} from '../types'
import { httpRequest } from './httpClient'

const inPreview = () => hasPreviewSession()

const buildQueryString = (params: Record<string, string | number | undefined>) => {
  const entries = Object.entries(params).filter(([, value]) => value !== undefined && value !== null && value !== '')
  if (entries.length === 0) return ''
  return `?${entries.map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(String(value))}`).join('&')}`
}

const rejectPreview = <T>() => {
  showPreviewReadonlyToast()
  return Promise.reject<T>(new Error('概念预览为只读，不会保存任何修改'))
}

export const timeChapterService = {
  getList(status?: TimeChapterStatus, pageNum = 1, pageSize = 50) {
    if (inPreview()) return Promise.resolve(getPreviewTimeChapterList(status, { pageNum, pageSize }))
    return httpRequest<PaginationResponse<TimeChapterSummaryVO>>({
      url: `/api/time-chapters${buildQueryString({ status, pageNum, pageSize })}`,
    })
  },
  getDetail(id: string | number, order: TimeChapterOrder = 'DESC', pageNum = 1, pageSize = 50) {
    if (inPreview()) {
      const result = getPreviewTimeChapterDetail(id, order, { pageNum, pageSize })
      return result ? Promise.resolve(result) : Promise.reject(new Error('Chapter not found'))
    }
    return httpRequest<TimeChapterDetailVO>({
      url: `/api/time-chapters/${id}${buildQueryString({ order, pageNum, pageSize })}`,
    })
  },
  create(payload: CreateTimeChapterDTO) {
    if (inPreview()) return rejectPreview<TimeChapterSummaryVO>()
    return httpRequest<TimeChapterSummaryVO>({ url: '/api/time-chapters', method: 'POST', data: payload })
  },
  update(id: string | number, payload: UpdateTimeChapterDTO) {
    if (inPreview()) return rejectPreview<TimeChapterSummaryVO>()
    return httpRequest<TimeChapterSummaryVO>({ url: `/api/time-chapters/${id}`, method: 'PUT', data: payload })
  },
  addMembers(id: string | number, payload: ChangeTimeChapterMembersDTO) {
    if (inPreview()) return rejectPreview<TimeChapterSummaryVO>()
    return httpRequest<TimeChapterSummaryVO>({ url: `/api/time-chapters/${id}/members`, method: 'POST', data: payload })
  },
  removeMembers(id: string | number, payload: ChangeTimeChapterMembersDTO) {
    if (inPreview()) return rejectPreview<TimeChapterSummaryVO>()
    return httpRequest<TimeChapterSummaryVO>({ url: `/api/time-chapters/${id}/members/remove`, method: 'POST', data: payload })
  },
  end(id: string | number, payload: TimeChapterVersionDTO) {
    if (inPreview()) return rejectPreview<TimeChapterSummaryVO>()
    return httpRequest<TimeChapterSummaryVO>({ url: `/api/time-chapters/${id}/end`, method: 'POST', data: payload })
  },
  reopen(id: string | number, payload: TimeChapterVersionDTO) {
    if (inPreview()) return rejectPreview<TimeChapterSummaryVO>()
    return httpRequest<TimeChapterSummaryVO>({ url: `/api/time-chapters/${id}/reopen`, method: 'POST', data: payload })
  },
  delete(id: string | number, payload: TimeChapterVersionDTO) {
    if (inPreview()) return rejectPreview<void>()
    return httpRequest<void>({ url: `/api/time-chapters/${id}/delete`, method: 'POST', data: payload })
  },
}
