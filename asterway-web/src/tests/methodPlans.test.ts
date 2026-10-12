import { describe, it, expect } from 'vitest';
import { emptyData } from '../data/seed';
import { localDate } from '../domain/dates';
import { methodPlanBundle, type MethodPlanInput } from '../domain/methodPlans';
import { makeTask } from '../domain/mutations';
import { validateData } from '../domain/schema';
import { validatePrototype } from '../domain/prototypeSchema';
import { socialSeed } from '../data/socialSeed';
const date = localDate();
const input: MethodPlanInput = {
  method: 'matrix',
  title: '我的成长计划',
  date,
  startTime: '09:00',
  minutes: 120,
  buffer: 5,
  reserve: 15,
  focusMinutes: 25,
  rows: [
    { title: '先开始一个行动', minutes: 25, quadrant: 'batch' },
    { title: '推进长期学习', minutes: 25, quadrant: 'schedule' },
  ],
};
describe('按方法建立计划', () => {
  it('四象限建立真实目标和任务，重要与紧急独立保存，兼容成长校验', () => {
    const data = emptyData(),
      bundle = methodPlanBundle(data, input);
    data.goals.push(...bundle.goals);
    data.tasks.push(...bundle.tasks);
    const checked = validateData(data);
    expect(checked.goals[0]?.planningMethod).toBe('matrix');
    expect(checked.tasks[0]).toMatchObject({
      important: false,
      urgent: true,
      planningMethod: 'matrix',
    });
    expect(checked.tasks[1]).toMatchObject({ important: true, urgent: false });
  });
  it('三件事限定数量，时间分块绕开固定安排且保留缓冲和留白', () => {
    const data = emptyData();
    data.tasks.push(
      makeTask({ title: '已有安排', scheduledDate: date, startTime: '09:00', durationMinutes: 30 }),
    );
    const bundle = methodPlanBundle(data, { ...input, method: 'blocks' });
    expect(bundle.tasks.map((t) => t.startTime)).toEqual(['09:35', '10:05']);
    expect(data.tasks[0]?.startTime).toBe('09:00');
    expect(() =>
      methodPlanBundle(data, { ...input, method: 'top3', rows: [...input.rows, ...input.rows] }),
    ).toThrow();
    expect(() => methodPlanBundle(data, { ...input, method: 'blocks', minutes: 30 })).toThrow();
  });
  it('GTD保留待安排状态，关键难事仅第一项为重点，番茄采用专注时长', () => {
    expect(
      methodPlanBundle(emptyData(), { ...input, method: 'gtd' }).tasks.every(
        (t) => !t.scheduledDate && !t.startTime && !t.isTodayFocus,
      ),
    ).toBe(true);
    const frog = methodPlanBundle(emptyData(), { ...input, method: 'frog' });
    expect(frog.tasks[0]).toMatchObject({ isTodayFocus: true, energy: 'high', important: true });
    expect(frog.tasks[1]?.isTodayFocus).toBe(false);
    expect(
      methodPlanBundle(emptyData(), { ...input, method: 'focus', focusMinutes: 45 }).tasks.every(
        (t) => t.durationMinutes === 45,
      ),
    ).toBe(true);
  });
  it('非法数据和跨午夜排程拒绝，空标题与不支持的方法不产生内容', () => {
    const data = emptyData();
    for (const value of [
      { ...input, title: '' },
      { ...input, method: 'unknown' },
      { ...input, rows: [] },
      { ...input, rows: [{ title: 'a', minutes: 0, quadrant: 'do' }] },
      { ...input, method: 'blocks', startTime: '23:50' },
    ])
      expect(() => methodPlanBundle(data, value as MethodPlanInput)).toThrow();
    expect(data.goals).toHaveLength(0);
  });
  it('旧原型备份可读取；站内提醒字段导出导入保留并拒绝非法时间', () => {
    const old = socialSeed();
    expect(validatePrototype(old).version).toBe(2);
    old.reminders = [
      {
        id: 'reminder-1',
        title: '每日复盘',
        time: '20:30',
        enabled: true,
        createdAt: new Date().toISOString(),
      },
    ];
    expect(validatePrototype(old).reminders?.[0]?.time).toBe('20:30');
    old.reminders[0]!.time = '25:00';
    expect(() => validatePrototype(old)).toThrow();
  });
});
