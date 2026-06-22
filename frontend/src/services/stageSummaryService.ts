import { httpRequest } from './httpClient'
import type { AiResultStatus } from './aiService'
import { hasPreviewSession } from '../features/preview/preview-session'
import { getToken } from '../utils'

const STAGE_SUMMARY_TIMEOUT_MS = 15000

export interface StageSummaryVO {
  summary: string
  source: string
  status: AiResultStatus
  message?: string | null
  recordCount: number
  unlockedCount: number
  lifeNodeCount: number
  generatedAt: string | number
}

export const stageSummaryService = {
  generate() {
    if (!getToken() && hasPreviewSession()) {
      return Promise.reject<StageSummaryVO>(new Error('演示模式不访问真实 AI 服务'))
    }
    return httpRequest<StageSummaryVO>({
      url: '/api/stage-summaries/generate',
      method: 'POST',
      timeout: STAGE_SUMMARY_TIMEOUT_MS,
    })
  },
}
