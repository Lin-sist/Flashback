import type { TagType } from './enums'

export interface TagVO {
  id: number
  name: string
  type: TagType
  status?: 'ENABLED' | 'DISABLED'
}
