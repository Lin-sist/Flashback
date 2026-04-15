export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp?: number
}

export interface PaginationResponse<T> {
  list: T[]
  total: number
  pageNum: number
  pageSize: number
}

export interface PageQuery {
  pageNum: number
  pageSize: number
}
