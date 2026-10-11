import type { GrowthTemplate } from '../data/templates';
import type { GrowthBundle, KnowledgeNote } from '../domain/prototype';
import type { AppData } from '../domain/types';
import { now, uid, addDays, localDate } from '../domain/dates';
import { makeTask } from '../domain/mutations';
export function templateBundle(
  template: GrowthTemplate,
  start = localDate(),
  days = template.days,
  title = template.title,
): GrowthBundle {
  const time = now(),
    goalId = uid();
  const milestones = template.stages.map((stage, index) => ({
    id: uid(),
    goalId,
    title: stage,
    order: index,
    createdAt: time,
  }));
  return {
    id: uid(),
    title: `建立「${title}」成长计划`,
    explanation:
      '这是可修改的通用行动示例，不是官方考试大纲或结果保证。确认后创建目标、阶段、任务和一个配套习惯。',
    goals: [
      {
        id: goalId,
        title,
        description: template.description,
        category: template.category,
        status: 'active',
        priority: 'high',
        startDate: start,
        targetDate: addDays(start, days),
        weeklyHours: 4,
        createdAt: time,
      },
    ],
    milestones,
    tasks: template.actions.map((action, index) =>
      makeTask({
        title: action,
        goalId,
        milestoneId: milestones[Math.min(milestones.length - 1, Math.floor(index / 2))]!.id,
        priority: index === 0 ? 'high' : 'medium',
        durationMinutes: index === 0 ? 15 : 25,
        scheduledDate: addDays(
          start,
          Math.round((index * (days - 1)) / Math.max(template.actions.length - 1, 1)),
        ),
        isTodayFocus: index === 0,
      }),
    ),
    habits: [
      {
        id: uid(),
        title: `${title} · ${template.habit}`.slice(0, 60),
        category: template.category,
        frequency: { kind: 'daily' },
        startDate: start,
        status: 'active',
        createdAt: time,
      },
    ],
    notes: [],
  };
}
export function knowledgeBundle(text: string): GrowthBundle {
  const content = text.replace(/^(请)?(帮我)?(记住|记录|记下)[：:,，\s]*/, '').trim(),
    time = now();
  return {
    id: uid(),
    title: '记录到个人知识库',
    explanation:
      '将你主动提供的信息保存为本地记忆，后续伙伴会按关键词引用。你可在知识库中编辑或删除。',
    goals: [],
    milestones: [],
    tasks: [],
    habits: [],
    notes: [
      {
        id: uid(),
        title: content.slice(0, 30) || '我的个人信息',
        content,
        tags: ['伙伴记忆', '个人偏好'],
        source: 'assistant',
        createdAt: time,
        updatedAt: time,
      },
    ],
  };
}
export function snapshotBundle(
  bundle: GrowthBundle,
  data: AppData,
  notes: KnowledgeNote[],
): string {
  return JSON.stringify({
    goals: data.goals.filter((g) => bundle.goals.some((b) => b.id === g.id)),
    milestones: data.milestones.filter((m) => bundle.milestones.some((b) => b.id === m.id)),
    tasks: data.tasks.filter((t) => bundle.tasks.some((b) => b.id === t.id)),
    habits: data.habits.filter((h) => bundle.habits.some((b) => b.id === h.id)),
    notes: notes.filter((n) => bundle.notes.some((b) => b.id === n.id)),
  });
}
