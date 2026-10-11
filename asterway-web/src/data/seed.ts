import type { AppData, Goal, Task } from '../domain/types';
import { addDays, localDate, now, uid } from '../domain/dates';
import { makeTask } from '../domain/mutations';
export function emptyData(): AppData {
  return {
    schemaVersion: 1,
    profile: {
      displayName: '星行者',
      reminder: false,
      sidebarCollapsed: false,
      onboardingDismissed: false,
      dailyEnergyByDate: {},
    },
    goals: [],
    milestones: [],
    tasks: [],
    habits: [],
    habitLogs: [],
    xpEvents: [],
    weeklyReviews: [],
    agentActionLog: [],
    focusSessions: [],
  };
}
export function seedDemo(today = localDate()): AppData {
  const data = emptyData(),
    timestamp = now();
  const goals: Array<Pick<Goal, 'title' | 'description' | 'category' | 'weeklyHours'>> = [
    {
      title: '软件设计师备考',
      description: '每天进步一点，把知识变成自己的能力。',
      category: 'study',
      weeklyHours: 7,
    },
    {
      title: '每周运动三次',
      description: '建立适合自己的运动节奏，让身体和生活都轻盈一点。',
      category: 'health',
      weeklyHours: 2,
    },
    {
      title: '提升英语阅读能力',
      description: '从短文精读开始，自信地读懂更大的世界。',
      category: 'study',
      weeklyHours: 3,
    },
  ];
  data.goals = goals.map((g, i) => ({
    ...g,
    id: `goal-${i}`,
    status: 'active',
    priority: i === 0 ? 'high' : 'medium',
    startDate: addDays(today, -6),
    targetDate: addDays(today, i === 2 ? 45 : 30),
    createdAt: timestamp,
  }));
  const names = [
    ['基础知识', '专题训练', '真题模拟'],
    ['建立运动节奏', '坚持执行'],
    ['基础词汇', '短文精读', '总结输出'],
  ];
  data.milestones = names.flatMap((list, i) =>
    list.map((title, order) => ({
      id: `milestone-${i}-${order}`,
      goalId: `goal-${i}`,
      title,
      order,
      createdAt: timestamp,
    })),
  );
  const entries: Array<Partial<Task> & { title: string }> = [
    {
      title: '复习 CPU 与计算机组成原理',
      goalId: 'goal-0',
      priority: 'high',
      durationMinutes: 40,
      startTime: '09:00',
      isTodayFocus: true,
      important: true,
      dueDate: today,
      energy: 'high',
    },
    {
      title: '精读一篇英文短文',
      goalId: 'goal-2',
      durationMinutes: 20,
      startTime: '11:00',
      isTodayFocus: true,
    },
    {
      title: '晚间快走，给自己一点留白',
      goalId: 'goal-1',
      durationMinutes: 20,
      startTime: '18:30',
      isTodayFocus: true,
    },
    {
      title: '整理本周的学习笔记',
      important: false,
      urgent: true,
      energy: 'low',
      goalId: 'goal-0',
      priority: 'low',
      durationMinutes: 25,
      startTime: '20:00',
    },
    {
      title: '完成 10 道基础练习题',
      goalId: 'goal-0',
      durationMinutes: 15,
      status: 'done',
      startTime: '08:00',
    },
    {
      title: '复习操作系统进程调度',
      goalId: 'goal-0',
      scheduledDate: addDays(today, 1),
      durationMinutes: 40,
      startTime: '09:00',
    },
    {
      title: '轻量运动与拉伸',
      goalId: 'goal-1',
      scheduledDate: addDays(today, 2),
      durationMinutes: 30,
      startTime: '18:00',
    },
    {
      title: '阅读一篇技术文章',
      goalId: 'goal-0',
      scheduledDate: addDays(today, -1),
      status: 'done',
    },
    {
      title: '总结短文里的新词汇',
      goalId: 'goal-2',
      scheduledDate: addDays(today, -2),
      status: 'done',
    },
    {
      title: '完成一组真题练习',
      goalId: 'goal-0',
      scheduledDate: addDays(today, -4),
      status: 'done',
    },
    {
      title: '一次舒适的户外散步',
      goalId: 'goal-1',
      scheduledDate: addDays(today, -5),
      status: 'done',
    },
    {
      title: '整理数据库知识框架',
      goalId: 'goal-0',
      scheduledDate: undefined,
      durationMinutes: 35,
    },
    {
      title: '挑选下一篇精读文章',
      important: false,
      urgent: false,
      goalId: 'goal-2',
      scheduledDate: undefined,
      durationMinutes: 15,
    },
  ];
  data.tasks = entries.map((entry, i) => {
    const task = makeTask({
      scheduledDate: today,
      ...entry,
      id: `task-${i}`,
      milestoneId: `milestone-${entry.goalId!.slice(-1)}-0`,
    });
    if (task.status === 'done') {
      task.completedAt = new Date(`${task.scheduledDate}T08:30:00`).toISOString();
      data.xpEvents.push({
        id: uid(),
        sourceType: 'task',
        sourceId: task.id,
        sourceDate: task.scheduledDate!,
        amount: 20,
        createdAt: task.completedAt,
      });
    }
    return task;
  });
  data.habits = [
    {
      id: 'habit-0',
      title: '每天阅读 20 分钟',
      category: 'study',
      frequency: { kind: 'daily' },
      startDate: addDays(today, -6),
      status: 'active',
      createdAt: timestamp,
    },
    {
      id: 'habit-1',
      title: '每周运动三次',
      category: 'health',
      frequency: { kind: 'weekly', times: 3 },
      startDate: addDays(today, -6),
      status: 'active',
      createdAt: timestamp,
    },
    {
      id: 'habit-2',
      title: '睡前整理明日计划',
      category: 'life',
      frequency: { kind: 'daily' },
      startDate: addDays(today, -6),
      status: 'active',
      createdAt: timestamp,
    },
  ];
  data.habits.forEach((habit, index) => {
    for (let offset = -6; offset <= 0; offset++) {
      if ((offset + index) % (index === 1 ? 3 : 4) === 0 || (offset === 0 && index !== 0)) continue;
      const date = addDays(today, offset),
        createdAt = new Date(`${date}T08:30:00`).toISOString();
      data.habitLogs.push({ id: uid(), habitId: habit.id, date, createdAt });
      data.xpEvents.push({
        id: uid(),
        sourceType: 'habit',
        sourceId: habit.id,
        sourceDate: date,
        amount: 10,
        createdAt,
      });
    }
  });
  return data;
}
