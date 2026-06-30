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
}

export interface ChatResponse {
  success: boolean
  errorMsg?: string
  answer: string
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

/** @deprecated 已收敛到 /chat，保留类型兼容旧代码 */
export interface Nl2SqlRequest {
  question: string
  productKey?: string
  deviceId?: string
  execute?: boolean
}

/** @deprecated 已收敛到 /chat */
export interface Nl2SqlResponse {
  success: boolean
  errorMsg?: string
  sql?: string
  explanation?: string
  results?: Record<string, any>[]
  resultCount?: number
}
