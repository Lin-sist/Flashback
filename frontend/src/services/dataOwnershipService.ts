import type { DataDeletionScope, DataOperationVO, DataOwnershipSummaryVO, SealedContentPolicy } from '../types'
import { hasPreviewSession } from '../features/preview/preview-session'
import { getToken } from '../utils'
import { rememberDataOperation } from '../features/data-ownership/mutation-state'
import { API_BASE_URL, httpRequest } from './httpClient'

const inPreview = () => !getToken() && hasPreviewSession()
const rejectPreview = <T>() => Promise.reject<T>(new Error('概念预览为只读，不会导出或删除任何数据'))

export const dataOwnershipService = {
  async summary() {
    if (inPreview()) {
      return Promise.resolve<DataOwnershipSummaryVO>({
        recordCounts: { DRAFT: 1, SAVED: 2, SEALED: 2, UNLOCKED: 1 },
        mediaBytes: 0,
        activeOperation: null,
      })
    }
    const result = await httpRequest<DataOwnershipSummaryVO>({ url: '/api/data-ownership/summary' })
    rememberDataOperation(result.activeOperation)
    return result
  },
  async createExport(sealedContentPolicy: SealedContentPolicy) {
    if (inPreview()) return rejectPreview<DataOperationVO>()
    const result = await httpRequest<DataOperationVO>({ url: '/api/data-ownership/export-operations', method: 'POST', data: { sealedContentPolicy }, timeout: 60000 })
    rememberDataOperation(result); return result
  },
  async operation(id: number) {
    if (inPreview()) return rejectPreview<DataOperationVO>()
    const result = await httpRequest<DataOperationVO>({ url: `/api/data-ownership/operations/${id}` })
    rememberDataOperation(result); return result
  },
  prepareDeletion(scope: DataDeletionScope, recordId?: string | number) {
    if (inPreview()) return rejectPreview<DataOperationVO>()
    return httpRequest<DataOperationVO>({ url: '/api/data-ownership/deletion-intents', method: 'POST', data: { scope, recordId: recordId ?? null } })
  },
  async confirmDeletion(intentId: number, confirmationText: string) {
    if (inPreview()) return rejectPreview<DataOperationVO>()
    const result = await httpRequest<DataOperationVO>({ url: '/api/data-ownership/deletion-operations', method: 'POST', data: { intentId, confirmationText }, timeout: 60000 })
    rememberDataOperation(result); return result
  },
  async retry(id: number) {
    if (inPreview()) return rejectPreview<DataOperationVO>()
    const result = await httpRequest<DataOperationVO>({ url: `/api/data-ownership/operations/${id}/retry`, method: 'POST', timeout: 60000 })
    rememberDataOperation(result); return result
  },
  download(id: number): Promise<string> {
    if (inPreview()) return rejectPreview<string>()
    const token = getToken()
    return new Promise((resolve, reject) => {
      uni.downloadFile({
        url: `${API_BASE_URL}/api/data-ownership/export-operations/${id}/download`,
        header: token ? { Authorization: `Bearer ${token}` } : {},
        timeout: 60000,
        success: (result) => result.statusCode === 200 ? resolve(result.tempFilePath) : reject(new Error('导出包下载失败')),
        fail: () => reject(new Error('导出包下载失败')),
      })
    })
  },
}
