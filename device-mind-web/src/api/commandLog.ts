import request from './request'
import type { CommandLogVO, CommandLogPageQueryDTO } from '@/types/commandLog'
import type { PageResult } from '@/types/api'

export function getCommandLogList(data: CommandLogPageQueryDTO) {
  return request.post<any, PageResult<CommandLogVO>>('/device-mind/core/command-logs/list', data)
}
export function getCommandLogById(id: number) {
  return request.get<any, CommandLogVO>('/device-mind/core/command-logs/detail', { params: { id } })
}
