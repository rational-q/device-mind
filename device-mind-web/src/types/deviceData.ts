export interface DeviceDataVO {
  time: number
  deviceId: string
  attrName: string
  value: number
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
