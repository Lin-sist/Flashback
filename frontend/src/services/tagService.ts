import { httpRequest } from './httpClient'
import type { TagVO } from '../types'

export const tagService = {
  getTags() {
    return httpRequest<TagVO[]>({
      url: '/api/tags',
    })
  },
}
