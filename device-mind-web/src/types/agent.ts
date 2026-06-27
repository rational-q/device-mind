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

export interface Nl2SqlRequest {
  question: string
  productKey?: string
  deviceId?: string
  execute?: boolean
}

export interface Nl2SqlResponse {
  success: boolean
  errorMsg?: string
  sql?: string
  explanation?: string
  results?: Record<string, any>[]
  resultCount?: number
}
