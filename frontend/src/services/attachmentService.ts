import { httpRequest } from './httpClient'
import { hasPreviewSession } from '../features/preview/preview-session'
import { getToken } from '../utils'
import type {
  AttachmentAccessUrlVO,
  AttachmentUploadTokenVO,
  CommitRecordAttachmentDTO,
  CreateAttachmentUploadTokenDTO,
  RecordAttachmentVO,
} from '../types'

interface QiniuUploadResponse {
  key?: string
  error?: string
}

const parseQiniuResponse = (data: string): QiniuUploadResponse => {
  try {
    return JSON.parse(data) as QiniuUploadResponse
  } catch {
    throw new Error('存储服务返回内容无效')
  }
}

const shouldBlockRealIntegrationInPreview = () => !getToken() && hasPreviewSession()

const rejectPreviewMediaRequest = <T>() => Promise.reject<T>(new Error('演示模式不访问真实媒体服务'))

const readFile = (filePath: string) => new Promise<ArrayBuffer>((resolve, reject) => {
  uni.getFileSystemManager().readFile({
    filePath,
    success: ({ data }) => {
      if (typeof data === 'string') {
        reject(new Error('读取上传文件失败'))
        return
      }
      resolve(data)
    },
    fail: () => reject(new Error('读取上传文件失败')),
  })
})

const uploadWithPut = async (filePath: string, authorization: AttachmentUploadTokenVO) => {
  const data = await readFile(filePath)
  return new Promise<void>((resolve, reject) => {
    uni.request({
      url: authorization.uploadUrl,
      method: 'PUT',
      header: authorization.uploadHeaders,
      data,
      success: (result) => {
        if (result.statusCode < 200 || result.statusCode >= 300) {
          reject(new Error('文件上传失败'))
          return
        }
        resolve()
      },
      fail: () => reject(new Error('文件上传失败，请稍后重试')),
    })
  })
}

export const attachmentService = {
  createUploadToken(recordId: string | number, payload: CreateAttachmentUploadTokenDTO) {
    if (shouldBlockRealIntegrationInPreview()) {
      return rejectPreviewMediaRequest<AttachmentUploadTokenVO>()
    }
    return httpRequest<AttachmentUploadTokenVO>({
      url: `/api/records/${recordId}/attachments/upload-token`,
      method: 'POST',
      data: payload,
    })
  },

  upload(filePath: string, authorization: AttachmentUploadTokenVO) {
    if (shouldBlockRealIntegrationInPreview()) {
      return rejectPreviewMediaRequest<void>()
    }
    if (authorization.uploadMethod === 'PUT') {
      return uploadWithPut(filePath, authorization)
    }
    if (authorization.uploadMethod !== 'POST_MULTIPART') {
      return Promise.reject<void>(new Error('存储服务上传方式不受支持'))
    }
    return new Promise<void>((resolve, reject) => {
      uni.uploadFile({
        url: authorization.uploadUrl,
        filePath,
        name: authorization.fileFieldName || 'file',
        header: authorization.uploadHeaders,
        formData: authorization.uploadFormData,
        success: (result) => {
          let payload: QiniuUploadResponse
          try {
            payload = parseQiniuResponse(result.data)
          } catch (error) {
            reject(error)
            return
          }
          if (result.statusCode < 200 || result.statusCode >= 300 || payload.error) {
            reject(new Error(payload.error || '文件上传失败'))
            return
          }
          if (authorization.provider === 'QINIU' && payload.key !== authorization.key) {
            reject(new Error('存储对象校验失败'))
            return
          }
          resolve()
        },
        fail: () => reject(new Error('文件上传失败，请稍后重试')),
      })
    })
  },

  commit(recordId: string | number, payload: CommitRecordAttachmentDTO) {
    if (shouldBlockRealIntegrationInPreview()) {
      return rejectPreviewMediaRequest<RecordAttachmentVO>()
    }
    return httpRequest<RecordAttachmentVO>({
      url: `/api/records/${recordId}/attachments/commit`,
      method: 'POST',
      data: payload,
    })
  },

  createAccessUrl(recordId: string | number, attachmentId: string | number) {
    if (shouldBlockRealIntegrationInPreview()) {
      return rejectPreviewMediaRequest<AttachmentAccessUrlVO>()
    }
    return httpRequest<AttachmentAccessUrlVO>({
      url: `/api/records/${recordId}/attachments/${attachmentId}/access-url`,
    })
  },

  delete(recordId: string | number, attachmentId: string | number) {
    if (shouldBlockRealIntegrationInPreview()) {
      return rejectPreviewMediaRequest<void>()
    }
    return httpRequest<void>({
      url: `/api/records/${recordId}/attachments/${attachmentId}`,
      method: 'DELETE',
    })
  },
}
