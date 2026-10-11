import type { AppData, Task, Habit } from './types';
import { priorityRank } from './types';
import { addDays, localDate, weekStart } from './dates';
export function visibleTasks(data: AppData): Task[] {
  return data.tasks.filter((t) => !t.deletedAt && t.status !== 'cancelled');
}
export function sortTasks(tasks: Task[]): Task[] {
  return [...tasks].sort(
    (a, b) =>
      (a.startTime || '99:99').localeCompare(b.startTime || '99:99') ||
      priorityRank[a.priority] - priorityRank[b.priority],
  );
}
export function goalProgress(data: AppData, id: string) {
  const tasks = visibleTasks(data).filter((t) => t.goalId === id);
  const done = tasks.filter((t) => t.status === 'done').length;
  return {
    total: tasks.length,
    done,
    percent: tasks.length ? Math.round((done / tasks.length) * 100) : null,
  };
}
export function totalXp(data: AppData): number {
  return Math.max(
    0,
    data.xpEvents.reduce((n, e) => n + e.amount, 0),
  );
}
export function rangeStats(data: AppData, from: string, to: string) {
  const between = (date: string) => date >= from && date <= to;
  const completed = data.tasks.filter(
    (t) => t.status === 'done' && t.completedAt && between(localDate(new Date(t.completedAt))),
  );
  const planned = visibleTasks(data).filter((t) => t.scheduledDate && between(t.scheduledDate));
  const donePlanned = planned.filter((t) => t.status === 'done').length;
  return {
    completed: completed.length,
    planned: planned.length,
    rate: planned.length ? Math.round((donePlanned / planned.length) * 100) : null,
    habitCount: data.habitLogs.filter((l) => between(l.date)).length,
    xp: data.xpEvents.filter((e) => between(e.sourceDate)).reduce((n, e) => n + e.amount, 0),
    goals: new Set(completed.map((t) => t.goalId).filter(Boolean)).size,
  };
}
export function streak(data: AppData, habit: Habit, today = localDate()): number {
  if (habit.frequency.kind !== 'daily') return 0;
  const dates = new Set(data.habitLogs.filter((l) => l.habitId === habit.id).map((l) => l.date));
  let date = dates.has(today) ? today : addDays(today, -1);
  let count = 0;
  while (dates.has(date) && date >= habit.startDate) {
    count++;
    date = addDays(date, -1);
  }
  return count;
}
export function habitWeek(data: AppData, habit: Habit, date: string) {
  const start = weekStart(date),
    end = addDays(start, 6);
  const count = data.habitLogs.filter(
    (l) => l.habitId === habit.id && l.date >= start && l.date <= end,
  ).length;
  const daysAvailable = Array.from({ length: 7 }, (_, i) => addDays(start, i)).filter(
    (d) => d >= habit.startDate,
  ).length;
  const target =
    habit.frequency.kind === 'daily'
      ? daysAvailable
      : Math.min(habit.frequency.times, daysAvailable);
  return {
    count,
    target,
    rate: target ? Math.round((Math.min(count, target) / target) * 100) : null,
  };
}
export function conflictingIds(tasks: Task[]): Set<string> {
  const ids = new Set<string>();
  const timed = tasks.filter(
    (t) => t.scheduledDate && t.startTime && t.status === 'todo' && !t.deletedAt,
  );
  const minute = (t: Task) => {
    const [h, m] = t.startTime!.split(':').map(Number);
    return h! * 60 + m!;
  };
  timed.forEach((a, i) =>
    timed.slice(i + 1).forEach((b) => {
      if (
        a.scheduledDate === b.scheduledDate &&
        minute(a) < minute(b) + b.durationMinutes &&
        minute(b) < minute(a) + a.durationMinutes
      ) {
        ids.add(a.id);
        ids.add(b.id);
      }
    }),
  );
  return ids;
}
