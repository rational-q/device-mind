import request from './request'
import type { DeviceDataVO, DeviceDataQueryDTO, ShadowVO, ShadowUpdateDTO } from '@/types/deviceData'
import type { PageResult } from '@/types/api'

export function getDeviceDataList(data: DeviceDataQueryDTO) {
  return request.post<any, PageResult<DeviceDataVO>>('/device-mind/core/device-data/list', data)
}

export function getDeviceShadow(deviceId: string) {
  return request.get<any, ShadowVO>('/device-mind/core/shadows', { params: { deviceId } })
}

export function updateDeviceShadow(deviceId: string, data: ShadowUpdateDTO) {
  return request.put('/device-mind/core/shadows', data, { params: { deviceId } })
}
