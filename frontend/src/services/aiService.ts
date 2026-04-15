import { httpRequest } from './httpClient'

export interface WritingPromptsResponse {
  prompts: string[]
  source: string
}

export interface SummarizeRecordResponse {
  summary: string
  confusion: string
  emotion: string
  coreQuestion: string
  source: string
}

export const aiService = {
  getWritingPrompts(content: string) {
    return httpRequest<WritingPromptsResponse>({
      url: '/api/ai/writing-prompts',
      method: 'POST',
      data: { content },
    })
  },
  summarizeRecord(title: string, content: string) {
    return httpRequest<SummarizeRecordResponse>({
      url: '/api/ai/summarize-record',
      method: 'POST',
      data: { title, content },
    })
  },
}
