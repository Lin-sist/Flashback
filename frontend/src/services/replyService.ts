import { httpRequest } from './httpClient'
import type { CreateReplyDTO, ReplyVO } from '../types'
import { getPreviewReply } from '../features/preview/data/preview-data'
import { hasPreviewSession } from '../features/preview/preview-session'
import { getToken } from '../utils'

const shouldUsePreviewData = () => !getToken() && hasPreviewSession()

export const replyService = {
  submitReply(recordId: number, payload: Omit<CreateReplyDTO, 'recordId'>) {
    if (shouldUsePreviewData()) {
      return Promise.resolve({
        id: Date.now(),
        recordId,
        content: payload.content,
        replyType: payload.replyType,
        createdAt: Date.now(),
      })
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
