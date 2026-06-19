import { httpRequest } from './httpClient'
import type { RecordType } from '../types'

export type AiResultStatus = 'SUCCESS' | 'UNAVAILABLE' | 'FAILED' | 'FALLBACK'

export interface WritingPromptsPayload {
  content?: string
  recordType?: RecordType
  coreQuestion?: string
}

export interface SummarizeRecordPayload {
  content: string
  coreQuestion?: string
}

export interface WritingPromptsResponse {
  prompts: string[]
  source: string
  status: AiResultStatus
  message?: string | null
}

export interface SummarizeRecordResponse {
  summary: string
  confusion: string
  emotion: string
  coreQuestion: string
  beliefThen?: string
  desiredOutcome?: string
  source: string
  status: AiResultStatus
  message?: string | null
}

export const aiService = {
  getWritingPrompts(payload: WritingPromptsPayload) {
    return httpRequest<WritingPromptsResponse>({
      url: '/api/ai/writing-prompts',
      method: 'POST',
      data: payload,
    })
  },
  summarizeRecord(payload: SummarizeRecordPayload) {
    return httpRequest<SummarizeRecordResponse>({
      url: '/api/ai/summarize-record',
      method: 'POST',
      data: payload,
    })
  },
}
