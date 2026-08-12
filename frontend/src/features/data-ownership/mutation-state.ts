import type { DataOperationVO } from '../../types'

let clearAllBlocking = false

export const rememberDataOperation = (operation?: DataOperationVO | null) => {
  clearAllBlocking = Boolean(
    operation?.operationType === 'CLEAR_ALL_RECORDS'
      && ['PENDING', 'RUNNING', 'RETRY_REQUIRED'].includes(operation.status),
  )
}

export const isDataOwnershipMutationBlocked = () => clearAllBlocking
