import type { AppData, PlanningMethod } from './types';
import type { GrowthBundle } from './prototype';
import { now, uid, addDays, validDate } from './dates';
import { makeTask } from './mutations';
import { minuteTime, timeMinutes, type Quadrant } from './productivity';
export const planningMethods = [
  {
    id: 'matrix',
    name: '四象限',
    icon: 'dashboard',
    hint: '先判断轻重缓急',
    description: '每项行动分别设置重要性和紧急性，放入对应象限。',
  },
  {
    id: 'top3',
    name: '每日三件事',
    icon: 'target',
    hint: '今天最多三个重点',
    description: '挑选最多三项行动，检查时间预算，作为当天重点。',
  },
  {
    id: 'blocks',
    name: '时间分块',
    icon: 'calendar',
    hint: '为行动安排时段',
    description: '指定开始时间、缓冲和留白，绕开已有日程。',
  },
  {
    id: 'focus',
    name: '番茄专注',
    icon: 'clock',
    hint: '一段时间只做一件事',
    description: '以选定专注时长建立行动，创建后进入关联任务的专注空间。',
  },
  {
    id: 'frog',
    name: '先做关键难事',
    icon: 'flame',
    hint: '从最重要的一步开始',
    description: '第一项设为重要、较高精力的关键行动，其余作为配套下一步。',
  },
  {
    id: 'gtd',
    name: '收集与下一步',
    icon: 'notebook',
    hint: '先收集，再决定何时做',
    description: '先整理想法和下一步，不指定日期，放入待安排任务池。',
  },
] as const;
export interface MethodPlanInput {
  method: PlanningMethod;
  title: string;
  date: string;
  startTime: string;
  minutes: number;
  buffer: number;
  reserve: number;
  focusMinutes: number;
  rows: { title: string; minutes: number; quadrant: Quadrant }[];
}
export function methodPlanBundle(data: AppData, input: MethodPlanInput): GrowthBundle {
  const method = planningMethods.find((m) => m.id === input.method),
    rows = input.rows;
  if (
    !method ||
    input.title.trim().length < 2 ||
    input.title.trim().length > 60 ||
    !validDate(input.date) ||
    !rows.length ||
    rows.length > (input.method === 'top3' ? 3 : 8)
  )
    throw new Error('请填写计划名称、有效日期和对应数量的行动。');
  if (
    rows.some(
      (r) =>
        r.title.trim().length < 2 ||
        r.title.trim().length > 60 ||
        !Number.isInteger(r.minutes) ||
        r.minutes < 1 ||
        r.minutes > 240,
    )
  )
    throw new Error('每项行动需要 2–60 字标题和 1–240 分钟的时长。');
  if (
    ![15, 25, 45, 50].includes(input.focusMinutes) ||
    !/^([01]\d|2[0-3]):[0-5]\d$/.test(input.startTime) ||
    !Number.isInteger(input.minutes) ||
    input.minutes < 15 ||
    input.minutes > 720 ||
    !Number.isInteger(input.buffer) ||
    input.buffer < 0 ||
    input.buffer > 30 ||
    ![0, 15, 25, 40].includes(input.reserve)
  )
    throw new Error('请检查开始时间、可用分钟、缓冲与专注时长。');
  const timestamp = now(),
    goalId = uid();
  const tasks = rows.map((row, index) =>
    makeTask({
      title: row.title.trim(),
      goalId,
      planningMethod: input.method,
      description: `${method.name} · ${index === 0 && input.method === 'frog' ? '关键难事，先推进这一项' : '可按实际进展调整'}`,
      durationMinutes: input.method === 'focus' ? input.focusMinutes : row.minutes,
      scheduledDate: input.method === 'gtd' ? undefined : input.date,
      important: input.method === 'matrix' ? ['do', 'schedule'].includes(row.quadrant) : true,
      urgent: input.method === 'matrix' ? ['do', 'batch'].includes(row.quadrant) : false,
      priority: index === 0 ? 'high' : 'medium',
      energy: input.method === 'frog' && index === 0 ? 'high' : 'normal',
      isTodayFocus: input.method === 'top3' || (input.method === 'frog' && index === 0),
    }),
  );
  if (input.method === 'blocks' || input.method === 'top3') {
    const start = timeMinutes(input.startTime),
      end = Math.min(1440, start + input.minutes),
      reserve = Math.floor(((end - start) * input.reserve) / 100);
    const occupied = data.tasks
      .filter(
        (t) => !t.deletedAt && t.status === 'todo' && t.scheduledDate === input.date && t.startTime,
      )
      .map((t) => ({
        start: timeMinutes(t.startTime!),
        end: timeMinutes(t.startTime!) + t.durationMinutes,
      }));
    let cursor = start;
    for (const task of tasks) {
      while (true) {
        const collision = occupied.find(
          (s) => cursor < s.end && cursor + task.durationMinutes > s.start,
        );
        if (!collision) break;
        cursor = collision.end + input.buffer;
      }
      if (cursor + task.durationMinutes > end - reserve)
        throw new Error('行动超出可用时段，请减少行动、缩短时长或增加可用时间。已有安排会保留。');
      task.startTime = minuteTime(cursor);
      cursor += task.durationMinutes + input.buffer;
    }
  }
  return {
    id: uid(),
    title: `${method.name} · ${input.title.trim()}`,
    explanation: method.description,
    goals: [
      {
        id: goalId,
        title: input.title.trim(),
        planningMethod: input.method,
        description: `采用${method.name}：${method.description}`,
        category: 'life',
        status: 'active',
        priority: 'medium',
        startDate: input.date,
        targetDate: addDays(input.date, 30),
        weeklyHours:
          Math.round((tasks.reduce((sum, t) => sum + t.durationMinutes, 0) / 60) * 10) / 10,
        createdAt: timestamp,
      },
    ],
    milestones: [],
    tasks,
    habits: [],
    notes: [],
  };
}
