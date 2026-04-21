import { httpRequest } from './httpClient'
import type { TagVO } from '../types'
import { getPreviewTags } from '../features/preview/data/preview-data'
import { hasPreviewSession } from '../features/preview/preview-session'
import { getToken } from '../utils'

const shouldUsePreviewData = () => !getToken() && hasPreviewSession()

export const tagService = {
  getTags() {
    if (shouldUsePreviewData()) {
      return Promise.resolve(getPreviewTags())
    }

    return httpRequest<TagVO[]>({
      url: '/api/tags',
    })
  },
}
