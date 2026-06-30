export interface AlertVO {
  id: string
  deviceId: string
  ruleId: string
  ruleName: string
  level: string
  metric: string
  currentValue: number
  threshold: number
  triggeredAt: string
  confirmedAt: string
  resolvedAt: string
  status: string
  aiAnalysis: string
}

export interface AlertPageQueryDTO {
  deviceId?: string
  status?: string
  level?: string
  startTime?: number
  endTime?: number
  pageNum?: number
  pageSize?: number
}

export interface AlertRuleVO {
  id: string
  ruleName: string
  deviceType: string
  attrName: string
  operator: string
  threshold: number
  durationSeconds: number
  level: string
  enabled: boolean
  createdDate: string
}

export interface AlertRuleCreateDTO {
  ruleName: string
  deviceType: string
  attrName: string
  operator: string
  threshold: number
  durationSeconds?: number
  level: string
  enabled?: boolean
}

export interface AlertRuleUpdateDTO {
  ruleName?: string
  deviceType?: string
  attrName?: string
  operator?: string
  threshold?: number
  durationSeconds?: number
  level?: string
  enabled?: boolean
}

export interface AlertRulePageQueryDTO {
  deviceType?: string
  level?: string
  enabled?: boolean
  pageNum?: number
  pageSize?: number
}
