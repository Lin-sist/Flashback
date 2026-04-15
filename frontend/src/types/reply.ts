import type { ReplyType } from './enums'

export interface CreateReplyDTO {
  recordId: string
  content: string
  replyType: ReplyType
}

export interface ReplyVO {
  id: string
  recordId: string
  content: string
  replyType: ReplyType
  createdAt: number
}
