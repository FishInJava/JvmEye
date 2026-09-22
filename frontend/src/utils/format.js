/** 字节数友好显示。 */
export function formatBytes(bytes, digits = 1) {
  if (bytes === null || bytes === undefined || bytes < 0) return '-'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let value = bytes
  let unit = 0
  while (value >= 1024 && unit < units.length - 1) {
    value /= 1024
    unit += 1
  }
  const fixed = unit === 0 ? 0 : digits
  return `${value.toFixed(fixed)} ${units[unit]}`
}

/** 毫秒数友好显示。 */
export function formatDuration(ms) {
  if (ms === null || ms === undefined || ms < 0) return '-'
  const seconds = Math.floor(ms / 1000)
  const d = Math.floor(seconds / 86400)
  const h = Math.floor((seconds % 86400) / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60
  if (d > 0) return `${d}天 ${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

/** 0~1 的比率转百分比;-1 等无效值返回 null。 */
export function toPercent(value) {
  if (value === null || value === undefined || value < 0) return null
  return value * 100
}

export function formatPercent(value, digits = 1) {
  const percent = toPercent(value)
  return percent === null ? '-' : `${percent.toFixed(digits)}%`
}

/** 整数千分位。 */
export function formatNumber(value) {
  if (value === null || value === undefined || value < 0) return '-'
  return value.toLocaleString('zh-CN')
}

/** 固定小数位,如 LoadAverage;-1 等无效值返回 '-'。 */
export function formatDecimal(value, digits = 2) {
  if (value === null || value === undefined || value < 0) return '-'
  return value.toFixed(digits)
}

export function formatTime(timestamp) {
  if (!timestamp) return '-'
  const date = new Date(timestamp)
  const pad = (n) => String(n).padStart(2, '0')
  return `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}
