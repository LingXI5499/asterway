import type { AppData, Task } from './types'
import { addDays, localDate, now, uid } from './dates'
export function setTaskDone(data: AppData, id: string, done: boolean, timestamp = now()): void {
  const task = data.tasks.find(t => t.id === id && !t.deletedAt && t.status !== 'cancelled')
  if (!task || (task.status === 'done') === done) return
  if (done) {
    task.status = 'done'; task.completedAt = timestamp
    data.xpEvents.push({ id: uid(), sourceType: 'task', sourceId: id, sourceDate: localDate(new Date(timestamp)), amount: 20, createdAt: timestamp })
  } else {
    const date = localDate(new Date(task.completedAt!))
    data.xpEvents.push({ id: uid(), sourceType: 'task', sourceId: id, sourceDate: date, amount: -20, createdAt: timestamp })
    task.status = 'todo'; delete task.completedAt
  }
  task.updatedAt = timestamp
}
export function canCheckHabit(data: AppData, id: string, date: string, today = localDate()): boolean {
  const habit = data.habits.find(h => h.id === id)
  return !!habit && habit.status === 'active' && date >= habit.startDate && date <= today && date >= addDays(today, -7)
}
export function setHabitDone(data: AppData, id: string, date: string, done: boolean, today = localDate()): void {
  if (!canCheckHabit(data, id, date, today)) throw new Error('仅可为活跃习惯打卡今天或过去 7 天，且不能早于开始日。')
  const existing = data.habitLogs.find(l => l.habitId === id && l.date === date)
  if (!!existing === done) return
  const timestamp = now()
  if (done) data.habitLogs.push({ id: uid(), habitId: id, date, createdAt: timestamp })
  else data.habitLogs = data.habitLogs.filter(l => l.id !== existing!.id)
  data.xpEvents.push({ id: uid(), sourceType: 'habit', sourceId: id, sourceDate: date, amount: done ? 10 : -10, createdAt: timestamp })
}
export function removeGoal(data: AppData, id: string): void {
  data.goals = data.goals.filter(g => g.id !== id)
  data.milestones = data.milestones.filter(m => m.goalId !== id)
  data.tasks.forEach(t => { if (t.goalId === id) { delete t.goalId; delete t.milestoneId; t.updatedAt = now() } })
}
export function makeTask(values: Partial<Task> & { title: string }): Task {
  const timestamp = now()
  return { id: uid(), description: '', priority: 'medium', durationMinutes: 25, status: 'todo', isTodayFocus: false, createdAt: timestamp, updatedAt: timestamp, ...values }
}
