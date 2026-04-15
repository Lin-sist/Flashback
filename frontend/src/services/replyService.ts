import { httpRequest } from './httpClient'
import type { CreateReplyDTO, ReplyVO } from '../types'

export const replyService = {
  submitReply(recordId: string, payload: Omit<CreateReplyDTO, 'recordId'>) {
    return httpRequest<ReplyVO>({
      url: `/api/records/${recordId}/reply`,
      method: 'POST',
      data: payload as unknown as Record<string, unknown>,
    })
  },
  getReply(recordId: string) {
    return httpRequest<ReplyVO>({
      url: `/api/records/${recordId}/reply`,
      method: 'GET',
    })
  },
}
