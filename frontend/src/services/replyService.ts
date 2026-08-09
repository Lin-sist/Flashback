import { httpRequest } from './httpClient'
import type { CreateReplyDTO, ReplyVO } from '../types'
import { getPreviewReply } from '../features/preview/data/preview-data'
import { hasPreviewSession } from '../features/preview/preview-session'
import { getToken } from '../utils'

const shouldUsePreviewData = () => !getToken() && hasPreviewSession()

export const replyService = {
  submitReply(recordId: number, payload: Omit<CreateReplyDTO, 'recordId'>) {
    if (shouldUsePreviewData()) {
      return Promise.reject<ReplyVO>(new Error('概念预览为只读，不会保存任何修改'))
    }

    return httpRequest<ReplyVO>({
      url: `/api/records/${recordId}/reply`,
      method: 'POST',
      data: payload as unknown as Record<string, unknown>,
    })
  },
  getReply(recordId: number) {
    if (shouldUsePreviewData()) {
      return Promise.resolve(getPreviewReply(recordId))
    }

    return httpRequest<ReplyVO | null>({
      url: `/api/records/${recordId}/reply`,
      method: 'GET',
    })
  },
}
