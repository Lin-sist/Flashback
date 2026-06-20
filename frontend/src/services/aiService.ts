import { httpRequest } from './httpClient'
import type { RecordType } from '../types'
import { hasPreviewSession } from '../features/preview/preview-session'
import { getToken } from '../utils'

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

const shouldBlockRealIntegrationInPreview = () => !getToken() && hasPreviewSession()

const rejectPreviewAiRequest = <T>() => Promise.reject<T>(new Error('演示模式不访问真实 AI 服务'))

export const aiService = {
  getWritingPrompts(payload: WritingPromptsPayload) {
    if (shouldBlockRealIntegrationInPreview()) {
      return rejectPreviewAiRequest<WritingPromptsResponse>()
    }
    return httpRequest<WritingPromptsResponse>({
      url: '/api/ai/writing-prompts',
      method: 'POST',
      data: payload,
    })
  },
  summarizeRecord(payload: SummarizeRecordPayload) {
    if (shouldBlockRealIntegrationInPreview()) {
      return rejectPreviewAiRequest<SummarizeRecordResponse>()
    }
    return httpRequest<SummarizeRecordResponse>({
      url: '/api/ai/summarize-record',
      method: 'POST',
      data: payload,
    })
  },
}
