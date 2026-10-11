import type { AppData, Task, AgentProposal } from './types';
import { priorityRank } from './types';
import { addDays, now, uid, validDate } from './dates';

export type Quadrant = 'do' | 'schedule' | 'batch' | 'later';
export const quadrants = [
  {
    id: 'do' as const,
    title: '重要 · 紧急',
    action: '优先处理',
    icon: 'flame',
    color: 'coral',
    hint: '临近截止、影响关键结果，先把它完成。',
  },
  {
    id: 'schedule' as const,
    title: '重要 · 不紧急',
    action: '认真安排',
    icon: 'target',
    color: 'green',
    hint: '学习、长期项目和成长积累，主动留出时间。',
  },
  {
    id: 'batch' as const,
    title: '不重要 · 紧急',
    action: '集中处理',
    icon: 'clock',
    color: 'blue',
    hint: '必要的琐事集中处理；也可与他人协商分工。',
  },
  {
    id: 'later' as const,
    title: '不重要 · 不紧急',
    action: '暂缓考虑',
    icon: 'leaf',
    color: 'sand',
    hint: '先放入任务池，看看是否真的值得投入。',
  },
];
export function taskImportance(task: Task) {
  return task.important ?? task.priority !== 'low';
}
export function taskUrgency(task: Task, date: string) {
  return task.urgent ?? !!(task.dueDate && task.dueDate <= addDays(date, 2));
}
export function quadrantOf(task: Task, date: string): Quadrant {
  return taskImportance(task)
    ? taskUrgency(task, date)
      ? 'do'
      : 'schedule'
    : taskUrgency(task, date)
      ? 'batch'
      : 'later';
}
export function priorityReason(task: Task, date: string) {
  if (task.dueDate && task.dueDate < date) return `已超过截止日期 ${task.dueDate.slice(5)}`;
  if (taskUrgency(task, date) && taskImportance(task)) return '重要且紧急，优先推进';
  if (taskImportance(task)) return '推动长期目标，值得主动安排';
  if (taskUrgency(task, date)) return '时效性较强，适合集中处理';
  return '暂缓考虑，先留出空间';
}
export function rankTasks(
  tasks: Task[],
  date: string,
  energy: 'high' | 'normal' | 'low' = 'normal',
) {
  const order: Record<Quadrant, number> = { do: 0, schedule: 1, batch: 2, later: 3 };
  return [...tasks].sort(
    (a, b) =>
      order[quadrantOf(a, date)] - order[quadrantOf(b, date)] ||
      Number(energy === 'low' && a.energy === 'high') -
        Number(energy === 'low' && b.energy === 'high') ||
      (a.dueDate || '9999').localeCompare(b.dueDate || '9999') ||
      priorityRank[a.priority] - priorityRank[b.priority] ||
      a.durationMinutes - b.durationMinutes ||
      a.id.localeCompare(b.id),
  );
}
export function planningSnapshot(data: AppData, date: string, ids: string[]) {
  return JSON.stringify(data.tasks.filter((t) => t.scheduledDate === date || ids.includes(t.id)));
}
export interface PlanOptions {
  date: string;
  minutes: number;
  startTime: string;
  buffer: number;
  reserve: number;
  limit: number;
  energy: 'high' | 'normal' | 'low';
  selectedIds?: string[];
}
export interface PlannedSlot {
  task: Task;
  start: string;
  end: string;
  reason: string;
}
export interface DailyDraft {
  proposal: AgentProposal;
  slots: PlannedSlot[];
  skipped: { task: Task; reason: string }[];
  used: number;
  reserved: number;
  capacity: number;
  fixedCount: number;
}
export function timeMinutes(time: string) {
  const [h, m] = time.split(':').map(Number);
  return h! * 60 + m!;
}
export function minuteTime(minute: number) {
  return `${String(Math.floor(minute / 60)).padStart(2, '0')}:${String(minute % 60).padStart(2, '0')}`;
}
export function createDailyPlan(data: AppData, options: PlanOptions): DailyDraft {
  if (
    !validDate(options.date) ||
    !/^([01]\d|2[0-3]):[0-5]\d$/.test(options.startTime) ||
    !Number.isInteger(options.minutes) ||
    options.minutes < 15 ||
    options.minutes > 720 ||
    !Number.isInteger(options.buffer) ||
    options.buffer < 0 ||
    options.buffer > 30 ||
    !Number.isInteger(options.reserve) ||
    options.reserve < 0 ||
    options.reserve > 50 ||
    !Number.isInteger(options.limit) ||
    options.limit < 1 ||
    options.limit > 6
  )
    throw new Error('请检查日期、开始时间、可用分钟与缓冲设置。');
  const candidates = data.tasks.filter(
    (t) =>
      t.status === 'todo' &&
      !t.deletedAt &&
      (!t.scheduledDate || t.scheduledDate <= options.date) &&
      (!t.goalId || data.goals.some((g) => g.id === t.goalId && g.status === 'active')),
  );
  const selected = options.selectedIds;
  if (
    selected &&
    (new Set(selected).size !== selected.length ||
      selected.length > options.limit ||
      selected.some((id) => !candidates.some((t) => t.id === id)))
  )
    throw new Error('所选任务已变化或超出数量限制，请重新选择。');
  const ranked = rankTasks(candidates, options.date, options.energy);
  const chosen = selected
    ? ranked.filter((t) => selected.includes(t.id))
    : ranked.filter((t) => quadrantOf(t, options.date) !== 'later');
  const fixed = data.tasks.filter(
    (t) =>
      t.status === 'todo' &&
      !t.deletedAt &&
      t.scheduledDate === options.date &&
      t.startTime &&
      !chosen.some((c) => c.id === t.id),
  );
  // Existing timed tasks outside this draft remain fixed. New slots go around them.
  const occupied = data.tasks
    .filter(
      (t) => t.status === 'todo' && !t.deletedAt && t.scheduledDate === options.date && t.startTime,
    )
    .map((t) => ({
      id: t.id,
      start: timeMinutes(t.startTime!),
      end: timeMinutes(t.startTime!) + t.durationMinutes,
    }));
  const start = timeMinutes(options.startTime),
    end = Math.min(start + options.minutes, 1440);
  const reserved = Math.floor(((end - start) * options.reserve) / 100),
    capacity = end - start - reserved;
  const slots: PlannedSlot[] = [],
    skipped: DailyDraft['skipped'] = [];
  let cursor = start,
    used = 0;
  for (const task of chosen) {
    if (slots.length >= options.limit) {
      skipped.push({ task, reason: `最多安排 ${options.limit} 件事，留待下一次` });
      continue;
    }
    const cost = task.durationMinutes + (slots.length ? options.buffer : 0);
    if (used + cost > capacity) {
      skipped.push({ task, reason: '超出可用时间，建议拆小或留到下次' });
      continue;
    }
    let position = cursor + (slots.length ? options.buffer : 0);
    while (true) {
      const collision = occupied.find(
        (slot) =>
          slot.id !== task.id &&
          position < slot.end &&
          position + task.durationMinutes > slot.start,
      );
      if (!collision) break;
      position = collision.end + options.buffer;
    }
    if (position + task.durationMinutes > end - reserved) {
      skipped.push({ task, reason: '剩余连续时段不足，保留原排期' });
      continue;
    }
    const finish = position + task.durationMinutes;
    slots.push({
      task,
      start: minuteTime(position),
      end: minuteTime(finish),
      reason: priorityReason(task, options.date),
    });
    const old = occupied.find((slot) => slot.id === task.id);
    if (old) Object.assign(old, { start: position, end: finish });
    else occupied.push({ id: task.id, start: position, end: finish });
    cursor = finish;
    used += cost;
  }
  const ids = slots.map((slot) => slot.task.id);
  const proposal: AgentProposal = {
    id: uid(),
    type: 'daily_plan',
    title: `${options.date} · ${options.limit === 3 ? '每日三件事' : '时间分块'}计划`,
    explanation: `重要性与紧急性分开排序，参考精力选择任务；留白 ${reserved} 分钟，相邻新任务缓冲 ${options.buffer} 分钟。未选任务保持原排期，不自动延期或删除。`,
    createdAt: now(),
    keptTaskIds: ids,
    planningContext: {
      date: options.date,
      taskIds: ids,
      snapshot: planningSnapshot(data, options.date, ids),
    },
    changes: slots.map((slot) => ({
      op: 'reschedule' as const,
      taskId: slot.task.id,
      from: slot.task.scheduledDate,
      to: options.date,
      toStartTime: slot.start,
      toFocus: true,
      expectedUpdatedAt: slot.task.updatedAt,
      reason: slot.reason,
    })),
  };
  if (slots.length) {
    for (const task of data.tasks.filter(
      (t) =>
        t.scheduledDate === options.date &&
        t.status === 'todo' &&
        !t.deletedAt &&
        t.isTodayFocus &&
        !ids.includes(t.id),
    )) {
      proposal.changes.push({
        op: 'reschedule',
        taskId: task.id,
        from: task.scheduledDate,
        to: options.date,
        toStartTime: task.startTime,
        toFocus: false,
        expectedUpdatedAt: task.updatedAt,
        reason: '同步今日重点；日期、原有时段保持不变。',
      });
    }
    proposal.explanation += ' 今日重点将同步为这次选中的行动。';
  }
  return { proposal, slots, skipped, used, reserved, capacity, fixedCount: fixed.length };
}
