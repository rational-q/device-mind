export interface CommandSendRequest {
  deviceId: string
  command: string
  params?: Record<string, any>
  idempotencyKey?: string
}

export interface CommandSendResponse {
  idempotencyKey: string
  deviceId: string
  command: string
  message: string
}
