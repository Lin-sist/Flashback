import type { ReplyType } from './enums'

export interface CreateReplyDTO {
  recordId: number
  content: string
  replyType: ReplyType
}

export interface ReplyVO {
  id: number
  recordId: number
  content: string
  replyType: ReplyType
  createdAt: string | number
}
