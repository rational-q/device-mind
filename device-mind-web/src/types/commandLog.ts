export interface CommandLogVO {
  id: number
  deviceId: string
  command: string
  params: string
  idempotencyKey: string
  status: string
  retryCount: number
  maxRetries: number
  ackedAt: string
  createdDate: string
}

export interface CommandLogPageQueryDTO {
  deviceId?: string
  command?: string
  status?: string
  pageNum?: number
  pageSize?: number
}
