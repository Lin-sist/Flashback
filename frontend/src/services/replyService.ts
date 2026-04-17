import { httpRequest } from './httpClient'
import type { CreateReplyDTO, ReplyVO } from '../types'

export const replyService = {
  submitReply(recordId: number, payload: Omit<CreateReplyDTO, 'recordId'>) {
    return httpRequest<ReplyVO>({
      url: `/api/records/${recordId}/reply`,
      method: 'POST',
      data: payload as unknown as Record<string, unknown>,
    })
  },
  getReply(recordId: number) {
    return httpRequest<ReplyVO | null>({
      url: `/api/records/${recordId}/reply`,
      method: 'GET',
    })
  },
}
