import { httpRequest } from './httpClient'
import type { AiResultStatus } from './aiService'

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
    return httpRequest<StageSummaryVO>({
      url: '/api/stage-summaries/generate',
      method: 'POST',
    })
  },
}
