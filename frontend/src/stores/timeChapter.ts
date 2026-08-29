import { defineStore } from 'pinia'
import { timeChapterService } from '../services'
import type {
  ChangeTimeChapterMembersDTO,
  CreateTimeChapterDTO,
  TimeChapterDetailVO,
  TimeChapterOrder,
  TimeChapterSummaryVO,
  TimeChapterStatus,
  UpdateTimeChapterDTO,
} from '../types'

interface TimeChapterState {
  list: TimeChapterSummaryVO[]
  detail: TimeChapterDetailVO | null
  total: number
  pageNum: number
  pageSize: number
  loading: boolean
  error: string | null
}

export const useTimeChapterStore = defineStore('timeChapter', {
  state: (): TimeChapterState => ({
    list: [],
    detail: null,
    total: 0,
    pageNum: 1,
    pageSize: 50,
    loading: false,
    error: null,
  }),
  actions: {
    clearCache() {
      this.list = []
      this.detail = null
      this.total = 0
      this.pageNum = 1
      this.pageSize = 50
      this.loading = false
      this.error = null
    },
    async fetchList(status?: TimeChapterStatus, pageNum = 1, pageSize = 50, append = false) {
      this.loading = true
      this.error = null
      try {
        const result = await timeChapterService.getList(status, pageNum, pageSize)
        this.list = append ? [...this.list, ...result.list] : result.list
        this.total = result.total
        this.pageNum = result.pageNum
        this.pageSize = result.pageSize
        return result
      } catch (error) {
        this.error = error instanceof Error ? error.message : '篇章目录加载失败'
        throw error
      } finally {
        this.loading = false
      }
    },
    async fetchAll(status?: TimeChapterStatus) {
      const pageSize = 200
      const first = await timeChapterService.getList(status, 1, pageSize)
      const list = [...first.list]
      for (let pageNum = 2; list.length < first.total; pageNum += 1) {
        const page = await timeChapterService.getList(status, pageNum, pageSize)
        if (page.list.length === 0) break
        list.push(...page.list)
      }
      return { ...first, list, pageNum: 1, pageSize, total: first.total }
    },
    async fetchDetail(id: string | number, order: TimeChapterOrder = 'DESC', pageNum = 1, pageSize = 50, append = false) {
      this.loading = true
      this.error = null
      try {
        const result = await timeChapterService.getDetail(id, order, pageNum, pageSize)
        this.detail = append && this.detail && Number(this.detail.id) === Number(result.id)
          ? { ...result, members: { ...result.members, list: [...this.detail.members.list, ...result.members.list] } }
          : result
        return result
      } catch (error) {
        this.error = error instanceof Error ? error.message : '篇章详情加载失败'
        throw error
      } finally {
        this.loading = false
      }
    },
    create(payload: CreateTimeChapterDTO) {
      return timeChapterService.create(payload)
    },
    update(id: string | number, payload: UpdateTimeChapterDTO) {
      return timeChapterService.update(id, payload)
    },
    addMembers(id: string | number, payload: ChangeTimeChapterMembersDTO) {
      return timeChapterService.addMembers(id, payload)
    },
    removeMembers(id: string | number, payload: ChangeTimeChapterMembersDTO) {
      return timeChapterService.removeMembers(id, payload)
    },
    end(id: string | number, expectedVersion: number) {
      return timeChapterService.end(id, { expectedVersion })
    },
    reopen(id: string | number, expectedVersion: number) {
      return timeChapterService.reopen(id, { expectedVersion })
    },
    delete(id: string | number, expectedVersion: number) {
      return timeChapterService.delete(id, { expectedVersion })
    },
  },
})
