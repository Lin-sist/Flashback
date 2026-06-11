import { defineStore } from 'pinia'
import { recordService } from '../services'
import type {
  CreateRecordDTO,
  PageQuery,
  PaginationResponse,
  RecordDetailVO,
  RecordListItemVO,
  RecordReminderStatus,
  RecordStatus,
  UpdateRecordDTO,
} from '../types'

interface RecordState {
  list: RecordListItemVO[]
  detail: RecordDetailVO | null
  total: number
  loading: boolean
}

export const useRecordStore = defineStore('record', {
  state: (): RecordState => ({
    list: [],
    detail: null,
    total: 0,
    loading: false,
  }),
  actions: {
    clearCache() {
      this.list = []
      this.detail = null
      this.total = 0
      this.loading = false
    },
    async fetchList(status: RecordStatus | 'ALL', query: PageQuery = { pageNum: 1, pageSize: 10 }) {
      this.loading = true
      try {
        const result: PaginationResponse<RecordListItemVO> = await recordService.getRecordList(status, query)
        this.list = result.list
        this.total = result.total
        return result
      } finally {
        this.loading = false
      }
    },
    async fetchDetail(id: string | number) {
      this.loading = true
      try {
        const result = await recordService.getRecordDetail(id)
        this.detail = result
        return result
      } finally {
        this.loading = false
      }
    },
    async createDraft(payload: CreateRecordDTO) {
      return recordService.createDraft(payload)
    },
    async updateDraft(id: string | number, payload: UpdateRecordDTO) {
      return recordService.updateDraft(id, payload)
    },
    async sealRecord(id: string | number) {
      return recordService.sealRecord(id)
    },
    async updateUnlockReminderAuthorization(id: string | number, status: RecordReminderStatus) {
      const result = await recordService.updateUnlockReminderAuthorization(id, status)
      this.detail = result
      return result
    },
  },
})
