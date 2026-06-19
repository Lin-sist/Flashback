import { httpRequest } from './httpClient'
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

export const attachmentService = {
  createUploadToken(recordId: string | number, payload: CreateAttachmentUploadTokenDTO) {
    return httpRequest<AttachmentUploadTokenVO>({
      url: `/api/records/${recordId}/attachments/upload-token`,
      method: 'POST',
      data: payload,
    })
  },

  uploadToQiniu(filePath: string, authorization: AttachmentUploadTokenVO) {
    return new Promise<void>((resolve, reject) => {
      uni.uploadFile({
        url: authorization.uploadUrl,
        filePath,
        name: 'file',
        formData: {
          token: authorization.uploadToken,
          key: authorization.key,
        },
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
          if (payload.key !== authorization.key) {
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
    return httpRequest<RecordAttachmentVO>({
      url: `/api/records/${recordId}/attachments/commit`,
      method: 'POST',
      data: payload,
    })
  },

  createAccessUrl(recordId: string | number, attachmentId: string | number) {
    return httpRequest<AttachmentAccessUrlVO>({
      url: `/api/records/${recordId}/attachments/${attachmentId}/access-url`,
    })
  },

  delete(recordId: string | number, attachmentId: string | number) {
    return httpRequest<void>({
      url: `/api/records/${recordId}/attachments/${attachmentId}`,
      method: 'DELETE',
    })
  },
}
