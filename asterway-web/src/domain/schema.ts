import { z } from 'zod';
import { validDate, localDate } from './dates';
import type { AppData } from './types';
const id = z.string().min(1).max(100);
const date = z.string().refine(validDate, '日期必须是有效的 YYYY-MM-DD');
const timestamp = z.string().refine((v) => !Number.isNaN(Date.parse(v)), '时间戳无效');
const title = z.string().trim().min(2).max(60);
const category = z.enum(['study', 'health', 'work', 'life', 'creative', 'other']);
const priority = z.enum(['high', 'medium', 'low']);
const taskSchema = z
  .object({
    id,
    title,
    description: z.string().max(5000),
    goalId: id.optional(),
    milestoneId: id.optional(),
    priority,
    durationMinutes: z.number().int().min(1).max(1440),
    scheduledDate: date.optional(),
    startTime: z
      .string()
      .regex(/^([01]\d|2[0-3]):[0-5]\d$/)
      .optional(),
    status: z.enum(['todo', 'done', 'cancelled']),
    completedAt: timestamp.optional(),
    isTodayFocus: z.boolean(),
    createdAt: timestamp,
    updatedAt: timestamp,
    deletedAt: timestamp.optional(),
  })
  .refine((t) => (t.status === 'done') === !!t.completedAt, '任务完成状态与记录不一致');
const schema = z.object({
  schemaVersion: z.literal(1),
  profile: z.object({
    displayName: z.string().trim().min(1).max(30),
    reminder: z.boolean(),
    sidebarCollapsed: z.boolean(),
    onboardingDismissed: z.boolean(),
    dailyEnergyByDate: z.record(date, z.enum(['high', 'normal', 'low'])),
  }),
  goals: z.array(
    z
      .object({
        id,
        title,
        description: z.string().max(5000),
        category,
        status: z.enum(['active', 'paused', 'completed', 'archived']),
        priority,
        startDate: date,
        targetDate: date,
        weeklyHours: z.number().min(0).max(168),
        createdAt: timestamp,
      })
      .refine((g) => g.targetDate >= g.startDate, '目标截止日期早于开始日期'),
  ),
  milestones: z.array(
    z.object({ id, goalId: id, title, order: z.number().int().min(0), createdAt: timestamp }),
  ),
  tasks: z.array(taskSchema),
  habits: z.array(
    z.object({
      id,
      title,
      category,
      frequency: z.discriminatedUnion('kind', [
        z.object({ kind: z.literal('daily') }),
        z.object({ kind: z.literal('weekly'), times: z.number().int().min(1).max(7) }),
      ]),
      startDate: date,
      status: z.enum(['active', 'paused', 'archived']),
      createdAt: timestamp,
    }),
  ),
  habitLogs: z.array(z.object({ id, habitId: id, date, createdAt: timestamp })),
  xpEvents: z.array(
    z.object({
      id,
      sourceType: z.enum(['task', 'habit']),
      sourceId: id,
      sourceDate: date,
      amount: z.union([z.literal(20), z.literal(-20), z.literal(10), z.literal(-10)]),
      createdAt: timestamp,
    }),
  ),
  weeklyReviews: z.array(
    z.object({
      weekStartDate: date,
      wins: z.string().max(5000),
      blockers: z.string().max(5000),
      changes: z.string().max(5000),
      updatedAt: timestamp,
    }),
  ),
  agentActionLog: z.array(
    z.object({
      id,
      proposalId: id,
      title: z.string(),
      appliedAt: timestamp,
      beforeTasks: z.array(taskSchema),
      afterTasks: z.array(taskSchema),
      createdTaskIds: z.array(id),
      undoneAt: timestamp.optional(),
    }),
  ),
  focusSessions: z.array(
    z.object({ id, date, minutes: z.number().int().min(1).max(1440), createdAt: timestamp }),
  ),
});
export function validateData(input: unknown): AppData {
  const result = schema.safeParse(input);
  if (!result.success)
    throw new Error(
      `数据格式不正确：${result.error.issues[0]?.path.join('.')} ${result.error.issues[0]?.message}`,
    );
  const data: AppData = result.data;
  for (const values of [
    data.goals,
    data.tasks,
    data.habits,
    data.milestones,
    data.habitLogs,
    data.xpEvents,
    data.agentActionLog,
    data.focusSessions,
  ]) {
    if (new Set(values.map((v) => v.id)).size !== values.length)
      throw new Error('数据中存在重复 ID。');
  }
  const goals = new Set(data.goals.map((g) => g.id)),
    habits = new Set(data.habits.map((h) => h.id)),
    tasks = new Set(data.tasks.map((t) => t.id));
  if (data.milestones.some((m) => !goals.has(m.goalId)))
    throw new Error('里程碑关联了不存在的目标。');
  if (
    data.tasks.some(
      (t) =>
        (t.goalId && !goals.has(t.goalId)) ||
        (t.milestoneId &&
          !data.milestones.some((m) => m.id === t.milestoneId && m.goalId === t.goalId)),
    )
  )
    throw new Error('任务关联不合法。');
  const logKeys = data.habitLogs.map((l) => `${l.habitId}:${l.date}`);
  if (
    new Set(logKeys).size !== logKeys.length ||
    data.habitLogs.some(
      (l) =>
        !habits.has(l.habitId) ||
        l.date > localDate() ||
        l.date < data.habits.find((h) => h.id === l.habitId)!.startDate,
    )
  )
    throw new Error('习惯记录存在重复、未来日期或失效关联。');
  const net = new Map<string, number>();
  for (const event of data.xpEvents) {
    if (
      !(event.sourceType === 'task' ? tasks : habits).has(event.sourceId) ||
      Math.abs(event.amount) !== (event.sourceType === 'task' ? 20 : 10)
    )
      throw new Error('XP 流水关联或奖励金额不合法。');
    const key = `${event.sourceType}:${event.sourceId}:${event.sourceDate}`;
    net.set(key, (net.get(key) || 0) + event.amount);
  }
  const expected = new Map<string, number>();
  data.tasks
    .filter((t) => t.status === 'done')
    .forEach((t) => expected.set(`task:${t.id}:${localDate(new Date(t.completedAt!))}`, 20));
  data.habitLogs.forEach((l) => expected.set(`habit:${l.habitId}:${l.date}`, 10));
  for (const key of new Set([...net.keys(), ...expected.keys()]))
    if ((net.get(key) || 0) !== (expected.get(key) || 0))
      throw new Error('XP 流水与完成记录不一致。');
  if (new Set(data.weeklyReviews.map((r) => r.weekStartDate)).size !== data.weeklyReviews.length)
    throw new Error('同一周有重复复盘。');
  return data;
}
