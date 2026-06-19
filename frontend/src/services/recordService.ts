import { httpRequest } from './httpClient'
import {
  RecordReminderStatus,
  RecordType,
} from '../types'
import type {
  CreateRecordDTO,
  PageQuery,
  PaginationResponse,
  RecordDetailVO,
  RecordListItemVO,
  RecordStatus,
  TimelineGroupVO,
  TimelineQuery,
  UpdateRecordLocationDTO,
  UpdateRecordDTO,
} from '../types'
import {
  getPreviewRecordDetail,
  getPreviewRecordList,
  getPreviewTimeline,
  getPreviewUnlockedRecords,
  getPreviewWriteDraftResult,
} from '../features/preview/data/preview-data'
import { hasPreviewSession } from '../features/preview/preview-session'
import { getToken } from '../utils'

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
  beliefThen: payload.beliefThen ?? null,
  lifeNodeType: payload.lifeNodeType ?? null,
  lifeNodeCustomLabel: payload.lifeNodeCustomLabel ?? null,
  unlockAt: payload.unlockAt ?? null,
  tagIds: payload.tagIds ?? [],
})

const shouldUsePreviewData = () => !getToken() && hasPreviewSession()

export const recordService = {
  createDraft(payload: CreateRecordDTO) {
    if (shouldUsePreviewData()) {
      return Promise.resolve(getPreviewWriteDraftResult(payload))
    }

    return httpRequest<RecordDetailVO>({
      url: '/api/records',
      method: 'POST',
      data: toDraftPayload(payload),
    })
  },
  updateDraft(id: string | number, payload: UpdateRecordDTO) {
    if (shouldUsePreviewData()) {
      return Promise.resolve(getPreviewWriteDraftResult(payload, Number(id) || 9901))
    }

    return httpRequest<RecordDetailVO>({
      url: `/api/records/${id}`,
      method: 'PUT',
      data: toDraftPayload(payload),
    })
  },
  updateLocation(id: string | number, payload: UpdateRecordLocationDTO) {
    if (shouldUsePreviewData()) {
      return Promise.reject(new Error('Preview mode is read-only'))
    }

    return httpRequest<RecordDetailVO>({
      url: `/api/records/${id}/location`,
      method: 'PUT',
      data: payload,
    })
  },
  deleteLocation(id: string | number) {
    if (shouldUsePreviewData()) {
      return Promise.reject(new Error('Preview mode is read-only'))
    }

    return httpRequest<RecordDetailVO>({
      url: `/api/records/${id}/location`,
      method: 'DELETE',
    })
  },
  deleteDraft(id: string | number) {
    if (shouldUsePreviewData()) {
      return Promise.resolve()
    }

    return httpRequest<void>({
      url: `/api/records/${id}`,
      method: 'DELETE',
    })
  },
  sealRecord(id: string | number) {
    if (shouldUsePreviewData()) {
      const detail = getPreviewRecordDetail(id)
      return Promise.resolve(detail || getPreviewWriteDraftResult({ content: '', recordType: RecordType.FUTURE_LETTER }, Number(id) || 9901))
    }

    return httpRequest<RecordDetailVO>({
      url: `/api/records/${id}/seal`,
      method: 'POST',
    })
  },
  updateUnlockReminderAuthorization(id: string | number, status: RecordReminderStatus) {
    if (shouldUsePreviewData()) {
      const detail = getPreviewRecordDetail(id)
      if (!detail) {
        return Promise.reject(new Error('Record not found'))
      }

      return Promise.resolve({
        ...detail,
        unlockReminderStatus: status,
      })
    }

    return httpRequest<RecordDetailVO>({
      url: `/api/records/${id}/unlock-reminder-authorization`,
      method: 'PUT',
      data: { status },
    })
  },
  getRecordList(status: RecordStatus | 'ALL', query: PageQuery) {
    if (shouldUsePreviewData()) {
      return Promise.resolve(getPreviewRecordList(status, query))
    }

    return httpRequest<PaginationResponse<RecordListItemVO>>({
      url: `/api/records${buildQueryString({
        pageNum: query.pageNum,
        pageSize: query.pageSize,
        ...(status === 'ALL' ? {} : { status }),
      })}`,
    })
  },
  getRecordDetail(id: string | number) {
    if (shouldUsePreviewData()) {
      const detail = getPreviewRecordDetail(id)
      if (!detail) {
        return Promise.reject(new Error('Record not found'))
      }

      return Promise.resolve(detail)
    }

    return httpRequest<RecordDetailVO>({
      url: `/api/records/${id}`,
    })
  },
  updateLaterReflection(id: string | number, realityLater: string) {
    if (shouldUsePreviewData()) {
      const detail = getPreviewRecordDetail(id)
      if (!detail) {
        return Promise.reject(new Error('Record not found'))
      }
      return Promise.resolve({
        ...detail,
        realityLater,
        realityLaterSubmitCount: Math.min((detail.realityLaterSubmitCount || 0) + 1, 2),
      })
    }

    return httpRequest<RecordDetailVO>({
      url: `/api/records/${id}/later-reflection`,
      method: 'PUT',
      data: { realityLater },
    })
  },
  getUnlockedRecords(pageNum = 1, pageSize = 10) {
    if (shouldUsePreviewData()) {
      return Promise.resolve(getPreviewUnlockedRecords(pageNum, pageSize))
    }

    return httpRequest<PaginationResponse<RecordListItemVO>>({
      url: `/api/records/unlocked${buildQueryString({ pageNum, pageSize })}`,
    })
  },
  getTimeline(query: TimelineQuery = {}) {
    if (shouldUsePreviewData()) {
      return Promise.resolve(getPreviewTimeline(query))
    }

    return httpRequest<TimelineGroupVO[]>({
      url: `/api/records/timeline${buildQueryString({
        year: query.year,
        tagId: query.tagId,
      })}`,
    })
  },
}
