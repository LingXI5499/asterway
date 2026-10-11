import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';
import { emptyData, seedDemo } from '../data/seed';
import { makeTask } from '../domain/mutations';
import { localDate, addDays } from '../domain/dates';
import { quadrantOf, rankTasks, createDailyPlan, timeMinutes } from '../domain/productivity';
import { applyProposal, undoProposal } from '../agent/actions';
import { conflictingIds } from '../domain/selectors';
import { validateData } from '../domain/schema';
import { useAppStore } from '../store/app';
import { useFocusStore } from '../store/focus';
const date = localDate();
const options = {
  date,
  minutes: 120,
  startTime: '09:00',
  buffer: 5,
  reserve: 15,
  limit: 3,
  energy: 'normal' as const,
};
beforeEach(() => {
  vi.useFakeTimers();
  const storage = new Map<string, string>();
  vi.stubGlobal('localStorage', {
    getItem: (key: string) => storage.get(key) ?? null,
    setItem: (key: string, value: string) => storage.set(key, value),
  });
  setActivePinia(createPinia());
});
afterEach(() => {
  vi.clearAllTimers();
  vi.useRealTimers();
  vi.unstubAllGlobals();
});
describe('优先级、四象限与可执行排程', () => {
  it('重要性与紧急性分开，安排今天不等于紧急，手动判断可覆盖自动规则', () => {
    const task = makeTask({ title: '长期学习', priority: 'high', scheduledDate: date });
    expect(quadrantOf(task, date)).toBe('schedule');
    task.dueDate = addDays(date, 2);
    expect(quadrantOf(task, date)).toBe('do');
    task.important = false;
    expect(quadrantOf(task, date)).toBe('batch');
    task.urgent = false;
    expect(quadrantOf(task, date)).toBe('later');
  });
  it('排序先重要紧急，同象限内结合截止和精力，不把低价值短任务提到最前', () => {
    const tasks = [
      makeTask({ title: '很短但不重要', important: false, urgent: false, durationMinutes: 1 }),
      makeTask({ title: '深度工作', important: true, urgent: false, energy: 'high' }),
      makeTask({ title: '轻量重要行动', important: true, urgent: false, energy: 'low' }),
      makeTask({ title: '紧急交付', important: true, urgent: true, energy: 'high' }),
    ];
    expect(rankTasks(tasks, date, 'low').map((t) => t.title)).toEqual([
      '紧急交付',
      '轻量重要行动',
      '深度工作',
      '很短但不重要',
    ]);
  });
  it('时间预算包含间隔和留白，超长任务不缩短，未来或暂停目标任务不提前挪动', () => {
    const data = emptyData();
    data.tasks = [
      makeTask({ title: '大任务', important: true, urgent: true, durationMinutes: 100 }),
      makeTask({ title: '行动一', durationMinutes: 30 }),
      makeTask({ title: '行动二', durationMinutes: 25 }),
      makeTask({ title: '未来安排', scheduledDate: addDays(date, 1), durationMinutes: 5 }),
    ];
    const draft = createDailyPlan(data, { ...options, minutes: 90, reserve: 20 });
    expect(draft.used).toBe(60);
    expect(draft.reserved).toBe(18);
    expect(draft.slots).toHaveLength(2);
    expect(draft.skipped[0]?.task.durationMinutes).toBe(100);
    expect(draft.slots.some((s) => s.task.title === '未来安排')).toBe(false);
    expect(data.tasks.every((t) => !t.startTime)).toBe(true);
  });
  it('保留未选时段和缓冲，跳过装不下的已定时任务也不会产生重叠', () => {
    const data = emptyData(),
      fixed = makeTask({
        title: '已有约定',
        important: false,
        urgent: false,
        scheduledDate: date,
        startTime: '09:00',
        durationMinutes: 30,
      });
    const selected = makeTask({ title: '新行动', durationMinutes: 20 });
    data.tasks = [fixed, selected];
    const draft = createDailyPlan(data, { ...options, selectedIds: [selected.id] });
    expect(draft.slots[0]?.start).toBe('09:35');
    applyProposal(data, draft.proposal);
    expect(conflictingIds(data.tasks).size).toBe(0);
    expect(data.tasks[0]?.startTime).toBe('09:00');
    const other = emptyData();
    other.tasks = [
      makeTask({ title: '长约定', scheduledDate: date, startTime: '09:00', durationMinutes: 90 }),
      makeTask({ title: '小行动', durationMinutes: 20 }),
    ];
    expect(createDailyPlan(other, { ...options, minutes: 60, reserve: 0 }).slots).toHaveLength(0);
  });
  it('确认同步三个重点及具体时段，完整撤销恢复，不修改 XP 或任务完成状态', () => {
    const data = seedDemo(),
      before = structuredClone(data);
    const ids = ['task-0', 'task-1', 'task-11'];
    const draft = createDailyPlan(data, { ...options, reserve: 0, selectedIds: ids });
    expect(draft.slots).toHaveLength(3);
    applyProposal(data, draft.proposal);
    expect(
      data.tasks
        .filter((t) => t.status === 'todo' && t.scheduledDate === date && t.isTodayFocus)
        .map((t) => t.id)
        .sort(),
    ).toEqual(ids.sort());
    expect(conflictingIds(data.tasks).size).toBe(0);
    expect(data.xpEvents).toEqual(before.xpEvents);
    expect(() => validateData(data)).not.toThrow();
    undoProposal(data);
    expect(data.tasks).toEqual(before.tasks);
  });
  it('草案之后新增固定时段会阻止应用，已有记录不被覆盖', () => {
    const data = emptyData();
    data.tasks = [makeTask({ title: '一个行动' })];
    const draft = createDailyPlan(data, options);
    data.tasks.push(
      makeTask({ title: '新约定', scheduledDate: date, startTime: '09:00', durationMinutes: 30 }),
    );
    const before = structuredClone(data.tasks);
    expect(() => applyProposal(data, draft.proposal)).toThrow('日程已变化');
    expect(data.tasks).toEqual(before);
  });
  it('午夜窗口截断，非法设置受阻，旧备份与新增任务字段能往返', () => {
    const data = emptyData();
    data.tasks = [
      makeTask({
        title: '短行动',
        durationMinutes: 5,
        important: false,
        urgent: true,
        dueDate: date,
        energy: 'low',
      }),
    ];
    const draft = createDailyPlan(data, { ...options, startTime: '23:50', reserve: 0 });
    expect(timeMinutes(draft.slots[0]!.end)).toBeLessThanOrEqual(1440);
    expect(() => createDailyPlan(data, { ...options, buffer: -1 })).toThrow();
    expect(validateData(JSON.parse(JSON.stringify(data))).tasks[0]).toMatchObject({
      important: false,
      urgent: true,
      energy: 'low',
    });
    const old = emptyData();
    old.tasks = [makeTask({ title: '旧版任务' })];
    expect(() => validateData(old)).not.toThrow();
  });
});
describe('跨页面专注计时', () => {
  it('暂停不累计，主动记录后刷新不重复入账，任务不会自动完成', () => {
    const app = useAppStore(),
      timer = useFocusStore();
    timer.data.taskId = 'task-0';
    timer.toggle();
    vi.advanceTimersByTime(65000);
    timer.toggle();
    expect(timer.elapsed).toBe(65);
    vi.advanceTimersByTime(15000);
    expect(timer.elapsed).toBe(65);
    expect(timer.record()).toBe(true);
    expect(timer.record()).toBe(false);
    expect(app.data.focusSessions).toHaveLength(1);
    expect(app.data.focusSessions[0]).toMatchObject({ minutes: 1, taskId: 'task-0' });
    expect(app.data.tasks[0]?.status).toBe('todo');
    timer.dispose();
    setActivePinia(createPinia());
    const restored = useFocusStore();
    expect(restored.data.recorded).toBe(true);
    expect(restored.record()).toBe(false);
    expect(useAppStore().data.focusSessions).toHaveLength(1);
    restored.dispose();
  });
  it('结束时不自动记账，休息不计作专注，记录分钟不超过本段时长', () => {
    const app = useAppStore(),
      timer = useFocusStore();
    timer.toggle();
    vi.advanceTimersByTime(30 * 60 * 1000);
    expect(timer.elapsed).toBe(25 * 60);
    expect(timer.finished).toBe(true);
    expect(app.data.focusSessions).toHaveLength(0);
    timer.record();
    expect(app.data.focusSessions[0]?.minutes).toBe(25);
    timer.configure('rest', 5);
    timer.toggle();
    vi.advanceTimersByTime(60000);
    expect(timer.record()).toBe(false);
    expect(app.data.focusSessions).toHaveLength(1);
    timer.dispose();
  });
});
