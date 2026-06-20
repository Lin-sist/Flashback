import { reactive } from 'vue'
import { attachmentService } from '../services'
import type { RecordAttachmentVO } from '../types'
import { getToken, toUserMessage } from '../utils'

interface RecordCoverSource {
  recordId: number
  cover?: RecordAttachmentVO | null
}

export const useRecordCoverUrls = () => {
  const coverUrls = reactive<Record<number, string>>({})
  const coverErrors = reactive<Record<number, string>>({})
  const coverLoading = reactive<Record<number, boolean>>({})
  const requestVersions = new Map<number, number>()

  const clearRecord = (recordId: number) => {
    requestVersions.set(recordId, (requestVersions.get(recordId) || 0) + 1)
    delete coverUrls[recordId]
    delete coverErrors[recordId]
    delete coverLoading[recordId]
  }

  const loadCover = async ({ recordId, cover }: RecordCoverSource) => {
    if (!cover) {
      clearRecord(recordId)
      return
    }

    const requestVersion = (requestVersions.get(recordId) || 0) + 1
    requestVersions.set(recordId, requestVersion)
    delete coverErrors[recordId]

    const embeddedUrl = cover.accessUrl?.trim()
    if (embeddedUrl) {
      coverUrls[recordId] = embeddedUrl
      coverLoading[recordId] = false
      return
    }

    if (!getToken()) {
      delete coverUrls[recordId]
      coverLoading[recordId] = false
      return
    }

    delete coverUrls[recordId]
    coverLoading[recordId] = true
    try {
      const access = await attachmentService.createAccessUrl(recordId, cover.id)
      if (requestVersions.get(recordId) !== requestVersion) return
      coverUrls[recordId] = access.url
    } catch (error) {
      if (requestVersions.get(recordId) !== requestVersion) return
      delete coverUrls[recordId]
      coverErrors[recordId] = toUserMessage(error)
    } finally {
      if (requestVersions.get(recordId) === requestVersion) {
        coverLoading[recordId] = false
      }
    }
  }

  const loadCovers = async (sources: RecordCoverSource[]) => {
    const activeIds = new Set(sources.map((source) => source.recordId))
    const knownIds = new Set([
      ...Object.keys(coverUrls).map(Number),
      ...Object.keys(coverErrors).map(Number),
      ...Object.keys(coverLoading).map(Number),
    ])
    knownIds.forEach((recordId) => {
      if (!activeIds.has(recordId)) clearRecord(recordId)
    })
    await Promise.allSettled(sources.map((source) => loadCover(source)))
  }

  const markCoverFailed = (recordId: number) => {
    delete coverUrls[recordId]
    coverErrors[recordId] = '封面加载失败'
  }

  return {
    coverUrls,
    coverErrors,
    coverLoading,
    loadCover,
    loadCovers,
    markCoverFailed,
  }
}
