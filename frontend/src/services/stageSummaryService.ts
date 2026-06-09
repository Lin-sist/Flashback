import { httpRequest } from './httpClient'

export interface StageSummaryVO {
  summary: string
  source: string
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
