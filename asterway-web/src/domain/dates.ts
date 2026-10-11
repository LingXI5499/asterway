export function localDate(value = new Date()): string {
  return `${value.getFullYear()}-${String(value.getMonth() + 1).padStart(2, '0')}-${String(value.getDate()).padStart(2, '0')}`
}
export function parseDate(date: string): Date {
  return new Date(`${date}T12:00:00`)
}
export function addDays(date: string, count: number): string {
  const value = parseDate(date)
  value.setDate(value.getDate() + count)
  return localDate(value)
}
export function weekStart(date: string): string {
  return addDays(date, -((parseDate(date).getDay() + 6) % 7))
}
export function weekDays(date: string): string[] {
  return Array.from({ length: 7 }, (_, i) => addDays(weekStart(date), i))
}
export function shortDate(date: string): string {
  return parseDate(date).toLocaleDateString('zh-CN', { month: 'numeric', day: 'numeric' })
}
export function validDate(date: string): boolean {
  return (
    /^\d{4}-\d{2}-\d{2}$/.test(date) &&
    !Number.isNaN(parseDate(date).getTime()) &&
    localDate(parseDate(date)) === date
  )
}
export function dayLabel(date: string): string {
  return parseDate(date).toLocaleDateString('zh-CN', { weekday: 'short' })
}
export function now(): string {
  return new Date().toISOString()
}
export function uid(): string {
  return crypto.randomUUID()
}
