import { describe, it, expect } from 'vitest'
import { seedDemo, emptyData } from '../data/seed'
import { addDays, localDate, weekStart, validDate } from '../domain/dates'
import { makeTask, setTaskDone, setHabitDone, removeGoal } from '../domain/mutations'
import { totalXp, goalProgress, conflictingIds, rangeStats } from '../domain/selectors'
import { validateData } from '../domain/schema'
import { ruleAgent } from '../agent/rules'
import { applyProposal, undoProposal } from '../agent/actions'
const today = localDate()
describe('日期与共享统计', () => {
  it('使用本地日期，正确处理闰日、跨年、周一', () => {
    expect(addDays('2024-02-28', 1)).toBe('2024-02-29')
    expect(addDays('2025-12-31', 1)).toBe('2026-01-01')
    expect(weekStart('2026-10-11')).toBe('2026-10-05')
    expect(validDate('2026-02-29')).toBe(false)
  })
  it('无任务目标待规划，完成任务后目标进度同步', () => {
    const data = seedDemo()
    expect(goalProgress(data, 'unknown').percent).toBeNull()
    const before = goalProgress(data, 'goal-0').done
    setTaskDone(data, 'task-0', true)
    expect(goalProgress(data, 'goal-0').done).toBe(before + 1)
  })
  it('删除目标保留任务并解除里程碑关联', () => {
    const data = seedDemo(),
      count = data.tasks.length,
      xp = totalXp(data)
    removeGoal(data, 'goal-0')
    expect(data.tasks).toHaveLength(count)
    expect(data.tasks.some((t) => t.goalId === 'goal-0')).toBe(false)
    expect(totalXp(data)).toBe(xp)
    expect(() => validateData(data)).not.toThrow()
  })
  it('完成日期与排期统计保持独立，无分母显示 null', () => {
    const data = emptyData(),
      task = makeTask({ title: '一个小行动', scheduledDate: addDays(today, -1) })
    data.tasks.push(task)
    setTaskDone(data, task.id, true)
    expect(rangeStats(data, today, today)).toMatchObject({ completed: 1, planned: 0, rate: null })
  })
  it('冲突检测按时段重叠，不把相邻任务算作冲突', () => {
    const a = makeTask({
      title: '行动一',
      scheduledDate: today,
      startTime: '09:00',
      durationMinutes: 30,
    })
    const b = makeTask({ title: '行动二', scheduledDate: today, startTime: '09:30' })
    expect(conflictingIds([a, b]).size).toBe(0)
    b.startTime = '09:20'
    expect(conflictingIds([a, b]).size).toBe(2)
  })
})
describe('XP 与习惯约束', () => {
  it('重复完成不重复奖励，取消冲销，重新完成恢复', () => {
    const data = seedDemo(),
      initial = totalXp(data)
    setTaskDone(data, 'task-0', true)
    setTaskDone(data, 'task-0', true)
    expect(totalXp(data)).toBe(initial + 20)
    setTaskDone(data, 'task-0', false)
    setTaskDone(data, 'task-0', false)
    expect(totalXp(data)).toBe(initial)
    setTaskDone(data, 'task-0', true)
    expect(totalXp(data)).toBe(initial + 20)
    expect(() => validateData(data)).not.toThrow()
  })
  it('同日习惯最多一次，撤销冲销 XP', () => {
    const data = seedDemo(),
      initial = totalXp(data)
    setHabitDone(data, 'habit-0', today, true)
    setHabitDone(data, 'habit-0', today, true)
    expect(totalXp(data)).toBe(initial + 10)
    setHabitDone(data, 'habit-0', today, false)
    expect(totalXp(data)).toBe(initial)
  })
  it('拒绝未来日期、超过 7 天和暂停习惯的打卡', () => {
    const data = seedDemo()
    expect(() => setHabitDone(data, 'habit-0', addDays(today, 1), true)).toThrow()
    expect(() => setHabitDone(data, 'habit-0', addDays(today, -8), true)).toThrow()
    data.habits[0]!.status = 'paused'
    expect(() => setHabitDone(data, 'habit-0', today, true)).toThrow()
  })
})
describe('可审查的伙伴方案', () => {
  it('生成草案无副作用，仅确认后改期；可撤销且不影响其他任务', () => {
    const data = seedDemo(),
      original = structuredClone(data.tasks)
    const proposal = ruleAgent.reschedule(data, 40)
    expect(data.tasks).toEqual(original)
    expect(proposal.keptTaskIds).toContain('task-0')
    applyProposal(data, proposal)
    expect(data.tasks.find((t) => t.id === 'task-1')!.scheduledDate).toBe(addDays(today, 1))
    data.tasks.find((t) => t.id === 'task-0')!.description = '与方案无关的手工修改'
    undoProposal(data)
    expect(data.tasks.find((t) => t.id === 'task-1')!.scheduledDate).toBe(today)
    expect(data.tasks.find((t) => t.id === 'task-0')!.description).toBe('与方案无关的手工修改')
  })
  it('受影响任务被编辑后阻止撤销', () => {
    const data = seedDemo(),
      proposal = ruleAgent.reschedule(data, 40)
    applyProposal(data, proposal)
    data.tasks.find((t) => t.id === 'task-1')!.title = '后来修改的标题'
    expect(() => undoProposal(data)).toThrow('已被修改')
    expect(data.tasks.find((t) => t.id === 'task-1')!.title).toBe('后来修改的标题')
  })
  it('应用前验证所有任务，过期方案不发生部分变更', () => {
    const data = seedDemo(),
      proposal = ruleAgent.reschedule(data, 40)
    data.tasks.find((t) => t.id === 'task-3')!.scheduledDate = addDays(today, 2)
    const original = structuredClone(data.tasks)
    expect(() => applyProposal(data, proposal)).toThrow()
    expect(data.tasks).toEqual(original)
  })
  it('拆解生成新任务，重复确认被拒绝，撤销只移除新建项', () => {
    const data = seedDemo(),
      count = data.tasks.length,
      proposal = ruleAgent.decompose(data, 'goal-0')
    applyProposal(data, proposal)
    expect(data.tasks.length).toBe(count + 4)
    expect(() => applyProposal(data, proposal)).toThrow('已经应用')
    undoProposal(data)
    expect(data.tasks.length).toBe(count)
    expect(() => undoProposal(data)).toThrow()
  })
  it('空上下文和足够时间不产生变更', () => {
    expect(ruleAgent.reschedule(emptyData(), 40).changes).toHaveLength(0)
    expect(ruleAgent.reschedule(seedDemo(), 1440).changes).toHaveLength(0)
    expect(ruleAgent.review(emptyData()).explanation).toContain('记录较少')
  })
})
describe('导入与本地数据', () => {
  it('导出导入完整往返，包括已应用方案日志', () => {
    const data = seedDemo()
    applyProposal(data, ruleAgent.reschedule(data, 40))
    const imported = validateData(JSON.parse(JSON.stringify(data)))
    expect(totalXp(imported)).toBe(totalXp(data))
    expect(imported.tasks).toEqual(data.tasks)
    expect(() => undoProposal(imported)).not.toThrow()
  })
  it('拒绝格式、失效关联、重复日志和虚假 XP', () => {
    expect(() => validateData({ schemaVersion: 2 })).toThrow()
    const missing = seedDemo()
    missing.tasks[0]!.goalId = 'missing'
    expect(() => validateData(missing)).toThrow()
    const repeated = seedDemo()
    repeated.habitLogs.push({ ...repeated.habitLogs[0]!, id: 'another-id' })
    expect(() => validateData(repeated)).toThrow()
    const xp = seedDemo()
    xp.xpEvents.push({ ...xp.xpEvents[0]!, id: 'extra-xp' })
    expect(() => validateData(xp)).toThrow()
  })
  it('清空数据是有效的空状态，不需要恢复种子', () => {
    const data = validateData(JSON.parse(JSON.stringify(emptyData())))
    expect(data.tasks).toHaveLength(0)
    expect(totalXp(data)).toBe(0)
  })
})
