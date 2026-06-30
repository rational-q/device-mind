import request from './request'
import type { CommandSendRequest, CommandSendResponse } from '@/types/command'

const CORE_BASE = '/device-mind/core'

/** 下发设备指令 */
export function sendCommand(data: CommandSendRequest) {
  return request.post<any, CommandSendResponse>(CORE_BASE + '/commands/send', data)
}
