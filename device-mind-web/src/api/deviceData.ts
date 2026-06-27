import request from './request'
import type { DeviceDataVO, DeviceDataQueryDTO, ShadowVO, ShadowUpdateDTO } from '@/types/deviceData'
import type { PageResult } from '@/types/api'

export function getDeviceDataList(data: DeviceDataQueryDTO) {
  return request.post<any, PageResult<DeviceDataVO>>('/device-mind/device-data/list', data)
}

export function getDeviceShadow(deviceId: string) {
  return request.get<any, ShadowVO>('/device-mind/shadows', { params: { deviceId } })
}

export function updateDeviceShadow(deviceId: string, data: ShadowUpdateDTO) {
  return request.put('/device-mind/shadows', data, { params: { deviceId } })
}
