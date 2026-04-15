import { defineStore } from 'pinia'
import { recordService } from '../services'
import type {
  CreateRecordDTO,
  PageQuery,
  PaginationResponse,
  RecordDetailVO,
  RecordListItemVO,
  RecordStatus,
  SealRecordDTO,
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
    async fetchDetail(id: string) {
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
    async updateDraft(id: string, payload: CreateRecordDTO) {
      return recordService.updateDraft(id, payload)
    },
    async sealRecord(payload: SealRecordDTO) {
      return recordService.sealRecord(payload.id, payload.unlockAt)
    },
  },
})
