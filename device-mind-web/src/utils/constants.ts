export const DEVICE_STATUS_MAP: Record<string, string> = {
  ONLINE: '在线',
  OFFLINE: '离线',
}

export const ALERT_LEVEL_MAP: Record<string, string> = {
  WARN: '警告',
  CRITICAL: '严重',
}

export const ALERT_STATUS_MAP: Record<string, string> = {
  TRIGGERED: '已触发',
  CONFIRMED: '已确认',
  RESOLVED: '已恢复',
}

export const COMMAND_STATUS_MAP: Record<string, string> = {
  PENDING: '待发送',
  SENT: '已发送',
  ACKED: '已确认',
  FAILED: '失败',
  EXPIRED: '已过期',
}
