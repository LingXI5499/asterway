import type { AppData, AgentProposal, Task } from '../domain/types'
import { now, uid, validDate } from '../domain/dates'
import { makeTask } from '../domain/mutations'
export function applyProposal(data: AppData, proposal: AgentProposal): void {
  if (!proposal.changes.length) throw new Error('此方案没有需要应用的变更。')
  const touched = new Set<string>()
  for (const change of proposal.changes) {
    if (change.op === 'reschedule') {
      const task = data.tasks.find((t) => t.id === change.taskId)
      if (
        !task ||
        task.deletedAt ||
        task.status !== 'todo' ||
        task.scheduledDate !== change.from ||
        !validDate(change.to) ||
        touched.has(task.id)
      )
        throw new Error('任务状态已变化或日期不合法，请重新生成方案。')
      touched.add(task.id)
    } else {
      const task = change.task
      if (
        !data.goals.some((g) => g.id === task.goalId && g.status === 'active') ||
        task.status !== 'todo' ||
        task.completedAt ||
        (task.scheduledDate && !validDate(task.scheduledDate)) ||
        task.durationMinutes < 1 ||
        task.title.length < 2 ||
        (task.milestoneId &&
          !data.milestones.some((m) => m.id === task.milestoneId && m.goalId === task.goalId))
      )
        throw new Error('目标或任务草案已失效，请重新生成。')
    }
  }
  if (data.agentActionLog.some((l) => l.proposalId === proposal.id))
    throw new Error('此方案已经应用，不能重复提交。')
  const beforeTasks: Task[] = [],
    afterTasks: Task[] = [],
    createdTaskIds: string[] = [],
    timestamp = now()
  proposal.changes.forEach((change) => {
    if (change.op === 'reschedule') {
      const task = data.tasks.find((t) => t.id === change.taskId)!
      beforeTasks.push(structuredClone(task))
      task.scheduledDate = change.to
      delete task.startTime
      task.updatedAt = timestamp
      afterTasks.push(structuredClone(task))
    } else {
      const task = makeTask(change.task)
      data.tasks.push(task)
      createdTaskIds.push(task.id)
      afterTasks.push(structuredClone(task))
    }
  })
  data.agentActionLog.push({
    id: uid(),
    proposalId: proposal.id,
    title: proposal.title,
    appliedAt: timestamp,
    beforeTasks,
    afterTasks,
    createdTaskIds,
  })
}
export function undoProposal(data: AppData): void {
  const action = data.agentActionLog.at(-1)
  if (!action || action.undoneAt) throw new Error('没有可撤销的最近一次方案。')
  for (const after of action.afterTasks) {
    const current = data.tasks.find((t) => t.id === after.id)
    if (!current || JSON.stringify(current) !== JSON.stringify(after))
      throw new Error('受影响任务已被修改，为保留你的新操作，不能撤销。')
  }
  action.beforeTasks.forEach((before) => {
    const index = data.tasks.findIndex((t) => t.id === before.id)
    data.tasks[index] = structuredClone(before)
  })
  data.tasks = data.tasks.filter((t) => !action.createdTaskIds.includes(t.id))
  action.undoneAt = now()
}
