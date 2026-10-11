import type { AgentProposal, AppData, NewTask } from '../domain/types';
import { addDays, localDate, now, uid, weekStart } from '../domain/dates';
import { rangeStats, visibleTasks } from '../domain/selectors';
import { rankTasks } from '../domain/productivity';
export interface AgentAdapter {
  reschedule(data: AppData, minutes: number, today?: string): AgentProposal;
  decompose(data: AppData, goalId: string, today?: string): AgentProposal;
  review(data: AppData, today?: string): AgentProposal;
}
const proposal = (
  type: AgentProposal['type'],
  title: string,
  explanation: string,
): AgentProposal => ({
  id: uid(),
  type,
  title,
  explanation,
  changes: [],
  keptTaskIds: [],
  createdAt: now(),
});
export const ruleAgent: AgentAdapter = {
  reschedule(data, minutes, today = localDate()) {
    if (!Number.isInteger(minutes) || minutes < 1 || minutes > 1440)
      throw new Error('可用时间应为 1–1440 分钟。');
    const tasks = rankTasks(
      visibleTasks(data).filter((t) => t.scheduledDate === today && t.status === 'todo'),
      today,
      data.profile.dailyEnergyByDate[today] || 'normal',
    );
    const result = proposal(
      'reschedule',
      `让今天适应 ${minutes} 分钟`,
      '按优先级和预估时长保留能容纳的行动，其余延后一天。完成记录不会改变。',
    );
    let remaining = minutes;
    tasks.forEach((task) => {
      if (task.durationMinutes <= remaining) {
        result.keptTaskIds.push(task.id);
        remaining -= task.durationMinutes;
      } else
        result.changes.push({
          op: 'reschedule',
          taskId: task.id,
          from: task.scheduledDate,
          to: addDays(today, 1),
          reason: `预估 ${task.durationMinutes} 分钟，超过当前剩余 ${remaining} 分钟。延后后取消原开始时间，避免沿用失效时段。`,
        });
    });
    const tomorrowCount = visibleTasks(data).filter(
      (t) => t.scheduledDate === addDays(today, 1) && t.status === 'todo',
    ).length;
    if (!tasks.length)
      result.explanation = '今天没有未完成任务，可以休息，或先从目标添加一个行动。';
    else if (!result.changes.length)
      result.explanation = '今天的任务已经能放进可用时间，不需要调整。';
    else if (!result.keptTaskIds.length)
      result.explanation += ' 当前没有任务能完整放进可用时间，建议先手动拆成更小的行动，再应用。';
    result.explanation += ` 明天原有 ${tomorrowCount} 项待办；延后任务将不指定时段，请在日程中重新安排。`;
    return result;
  },
  decompose(data, goalId, today = localDate()) {
    const goal = data.goals.find((g) => g.id === goalId && g.status === 'active');
    if (!goal) throw new Error('请先选择一个活跃目标。');
    const result = proposal(
      'create_tasks',
      `为「${goal.title}」迈出下一步`,
      '根据目标分类的预置行动模板生成草案，可确认或取消。第一项安排今天，其余安排到随后几天。',
    );
    const labels =
      goal.category === 'health'
        ? ['制定适合自己的本周运动节奏', '完成一次轻量活动', '记录这次活动的感受']
        : goal.category === 'study'
          ? ['梳理资料与学习范围', '学习一个核心知识点', '完成练习并检验理解', '整理难点与复盘笔记']
          : ['明确本阶段的交付结果', '完成一个最小可行步骤', '检查结果并记录下一步'];
    const milestone = data.milestones
      .filter((m) => m.goalId === goalId)
      .sort((a, b) => a.order - b.order)[0];
    labels.forEach((label, i) => {
      const task: NewTask = {
        title: `${goal.title} · ${label}`.slice(0, 60),
        description: '由本地规则模板生成，可按需要编辑。',
        priority: i === 0 ? 'high' : 'medium',
        durationMinutes: i === 0 ? 15 : 25,
        goalId,
        milestoneId: milestone?.id,
        scheduledDate: addDays(today, i),
        status: 'todo',
        isTodayFocus: i === 0,
      };
      result.changes.push({
        op: 'create_task',
        task,
        reason: `把长期目标分成第 ${i + 1} 个具体行动。`,
      });
    });
    return result;
  },
  review(data, today = localDate()) {
    const start = weekStart(today),
      stats = rangeStats(data, start, today);
    const unfinished = visibleTasks(data).filter(
      (t) =>
        t.scheduledDate &&
        t.scheduledDate >= start &&
        t.scheduledDate <= today &&
        t.status === 'todo',
    ).length;
    return proposal(
      'weekly_review',
      '这一周，每一步都算数',
      `本周至今完成了 ${stats.completed} 项任务，记录了 ${stats.habitCount} 次习惯打卡，净获得 ${stats.xp} XP。还有 ${unfinished} 项已到期任务未完成。${unfinished ? '建议把高优先级任务拆成更短的时段，给变化留一点缓冲。' : '保持当前节奏，下一周继续选择少量重要的行动。'}${!stats.completed && !stats.habitCount ? ' 当前记录较少，可以先完成一个小行动再来复盘。' : ''}`,
    );
  },
};
