export interface AlertAnalysisRequest {
  deviceId: string
  productKey?: string
  productName?: string
  deviceName?: string
  location?: string
  ruleName?: string
  level?: string
  metric?: string
  currentValue?: number
  threshold?: number
  triggeredAt?: number
}

export interface AlertAnalysisResponse {
  success: boolean
  errorMsg?: string
  summary: string
  possibleCauses: string[]
  recommendations: string[]
  severity: string
  rawResponse?: string
}

export interface ChatRequest {
  question: string
  deviceId?: string
  /** 会话ID（后端维护上下文，不传则新会话） */
  sessionId?: string
}

export interface ChatResponse {
  success: boolean
  errorMsg?: string
  answer: string
  /** 会话ID（后端返回，前端持久化以维持多轮上下文） */
  sessionId?: string
  toolsCalled: string[]
  pendingAction?: {
    action: string
    status: string
    deviceId: string
    command: string
    params?: Record<string, any>
    message: string
  } | null
  rawResponse?: string
}
