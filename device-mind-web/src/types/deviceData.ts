export interface DeviceDataVO {
  time: number
  deviceId: string
  attrName: string
  /** 数值型属性值；非数值（字符串/枚举）属性此字段为 null，取 valueText */
  value: number | null
  /** 字符串/枚举型属性值；数值属性此字段为 null */
  valueText: string | null
}

export interface DeviceDataQueryDTO {
  deviceId: string
  attrName?: string
  start?: number
  end?: number
  pageNum?: number
  pageSize?: number
}

export interface ShadowVO {
  deviceId: string
  reported: Record<string, unknown> | null
  desired: Record<string, unknown> | null
  reportedVersion: number
  desiredVersion: number
  updatedDate: string
}

export interface ShadowUpdateDTO {
  desired: Record<string, unknown>
}
