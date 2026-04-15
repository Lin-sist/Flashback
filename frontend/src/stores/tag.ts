import { defineStore } from 'pinia'
import { tagService } from '../services'
import { RecordType, TagType } from '../types'
import type { TagVO } from '../types'

interface TagState {
  tags: TagVO[]
}

export const useTagStore = defineStore('tag', {
  state: (): TagState => ({
    tags: [],
  }),
  getters: {
    moodTags: (state) => state.tags.filter((item) => item.type === TagType.MOOD),
    topicTags: (state) => state.tags.filter((item) => item.type === TagType.TOPIC),
    recordTypeOptions: () => [
      { label: 'Future Letter', value: RecordType.FUTURE_LETTER },
      { label: 'Node Record', value: RecordType.NODE_RECORD },
      { label: 'Emotion Note', value: RecordType.EMOTION_NOTE },
    ],
  },
  actions: {
    async fetchTags() {
      const result = await tagService.getTags()
      this.tags = result
      return result
    },
  },
})
