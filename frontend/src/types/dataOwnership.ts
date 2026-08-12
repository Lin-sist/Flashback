export type DataOperationType = 'EXPORT' | 'DELETE_RECORD' | 'CLEAR_ALL_RECORDS'
export type DataOperationStatus = 'PREPARED' | 'PENDING' | 'RUNNING' | 'RETRY_REQUIRED' | 'SUCCEEDED' | 'FAILED' | 'EXPIRED'
export type DataDeletionScope = 'RECORD' | 'ALL_RECORDS'
export type SealedContentPolicy = 'RESPECT_SEAL' | 'FULL_CONTENT'

export interface DataOperationVO {
  id: number
  operationType: DataOperationType
  status: DataOperationStatus
  sealedContentPolicy?: SealedContentPolicy | null
  totalItems: number
  processedItems: number
  failedItems: number
  failureCode?: string | null
  confirmationExpiresAt?: string | null
  artifactExpiresAt?: string | null
  confirmationText?: string | null
  retryable: boolean
  downloadable: boolean
}

export interface DataOwnershipSummaryVO {
  recordCounts: Record<string, number>
  mediaBytes: number
  activeOperation?: DataOperationVO | null
}
