export interface DeviceVO {
  id: string
  deviceId: string
  productId: number
  productName: string
  name: string
  location: string
  status: string
  lastOnlineTime: string
  firmwareVersion: string
  tags: string
  createdDate: string
}

export interface DeviceCreateDTO {
  deviceId: string
  productId: number
  name?: string
  location?: string
  firmwareVersion?: string
  tags?: string
}

export interface DeviceUpdateDTO {
  name?: string
  location?: string
  firmwareVersion?: string
  tags?: string
}

export interface DevicePageQueryDTO {
  deviceId?: string
  productId?: number
  status?: string
  pageNum?: number
  pageSize?: number
}

export interface DeviceStatusUpdateDTO {
  status: string
}
