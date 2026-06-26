import dayjs from 'dayjs'

export function formatDateTime(date: string | number | Date | null | undefined): string {
  if (!date) return '-'
  return dayjs(date).format('YYYY-MM-DD HH:mm:ss')
}

export function formatDate(date: string | number | Date | null | undefined): string {
  if (!date) return '-'
  return dayjs(date).format('YYYY-MM-DD')
}
