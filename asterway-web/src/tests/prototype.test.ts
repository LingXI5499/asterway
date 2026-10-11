import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';
import { useAppStore } from '../store/app';
import { usePrototypeStore } from '../store/prototype';
import { growthTemplates } from '../data/templates';
import { socialSeed } from '../data/socialSeed';
import { templateBundle, knowledgeBundle } from '../agent/bundles';
import { validatePrototype } from '../domain/prototypeSchema';
import { validateBackup } from '../domain/backup';
import { validateData } from '../domain/schema';
import { localDate } from '../domain/dates';
import { setTaskDone, setHabitDone, makeTask } from '../domain/mutations';
import { getTier } from '../domain/prototype';

beforeEach(() => {
  vi.useFakeTimers();
  const values = new Map<string, string>();
  vi.stubGlobal('localStorage', {
    getItem: (key: string) => values.get(key) ?? null,
    setItem: (key: string, value: string) => values.set(key, value),
  });
  setActivePinia(createPinia());
});
afterEach(() => {
  vi.clearAllTimers();
  vi.useRealTimers();
  vi.unstubAllGlobals();
});

describe('目标模板与伙伴批量创建', () => {
  it('所有模板都能创建合法关联，行动分布在整个计划周期', () => {
    const app = useAppStore();
    for (const template of growthTemplates) {
      const bundle = templateBundle(template, app.today);
      const data = structuredClone(JSON.parse(JSON.stringify(app.data)));
      data.goals.push(...bundle.goals);
      data.milestones.push(...bundle.milestones);
      data.tasks.push(...bundle.tasks);
      data.habits.push(...bundle.habits);
      expect(() => validateData(data), template.id).not.toThrow();
      expect(bundle.tasks[0]?.scheduledDate).toBe(app.today);
      expect(new Set(bundle.tasks.map((task) => task.scheduledDate)).size).toBeGreaterThan(1);
    }
  });
  it('一次创建同步目标任务习惯，重复应用受阻，撤销移除完整计划', () => {
    const app = useAppStore(),
      proto = usePrototypeStore(),
      before = JSON.stringify(app.data);
    const bundle = templateBundle(growthTemplates[0]!);
    expect(proto.applyBundle(bundle)).toBe(true);
    expect(app.data.tasks.filter((t) => t.goalId === bundle.goals[0]!.id)).toHaveLength(
      bundle.tasks.length,
    );
    expect(proto.applyBundle(bundle)).toBe(false);
    proto.undoBundle();
    expect(JSON.parse(JSON.stringify(app.data))).toEqual(JSON.parse(before));
  });
  it('已完成或新增的关联任务阻止撤销，保留后续真实记录', () => {
    const app = useAppStore(),
      proto = usePrototypeStore(),
      bundle = templateBundle(growthTemplates[1]!);
    proto.applyBundle(bundle);
    app.data.tasks.push(
      makeTask({ title: '后来增加的行动', goalId: bundle.goals[0]!.id, scheduledDate: app.today }),
    );
    proto.undoBundle();
    expect(proto.data.bundleLogs.at(-1)?.undone).toBe(false);
    app.data.tasks.pop();
    setTaskDone(app.data, bundle.tasks[0]!.id, true);
    proto.undoBundle();
    expect(proto.data.bundleLogs.at(-1)?.undone).toBe(false);
  });
  it('无效计划不产生部分写入，知识记忆可批量撤销', () => {
    const app = useAppStore(),
      proto = usePrototypeStore(),
      bundle = templateBundle(growthTemplates[0]!);
    const before = JSON.stringify(app.data);
    bundle.tasks[0]!.goalId = 'invalid';
    expect(proto.applyBundle(bundle)).toBe(false);
    expect(JSON.stringify(app.data)).toBe(before);
    const note = knowledgeBundle('记住：我每天有 20 分钟学习');
    expect(proto.applyBundle(note)).toBe(true);
    expect(proto.data.notes.at(-1)?.content).toBe('我每天有 20 分钟学习');
    proto.undoBundle();
    expect(proto.data.notes.some((n) => n.id === note.notes[0]!.id)).toBe(false);
  });
});

describe('社区挑战和备份', () => {
  it('初始社区合法，点赞收藏可逆，社群加入后才可发送群消息', () => {
    const proto = usePrototypeStore();
    expect(() => validatePrototype(socialSeed())).not.toThrow();
    const post = proto.data.posts[0]!,
      initial = post.likes;
    proto.toggleLike(post);
    proto.toggleLike(post);
    expect(post.likes).toBe(initial);
    proto.toggleSave(post);
    expect(post.saved).toBe(true);
    const circle = proto.data.circles[0]!;
    proto.joinCircle(circle.id);
    proto.sendMessage(circle.roomId, '今天完成一次练习');
    const room = proto.data.rooms.find((r) => r.id === circle.roomId)!;
    expect(room.messages.at(-1)?.text).toContain('模拟回复');
    const count = room.messages.length;
    proto.joinCircle(circle.id);
    proto.sendMessage(circle.roomId, '退出后无法发送');
    expect(room.messages).toHaveLength(count);
  });
  it('挑战必须报名并完成任务+打卡，同日奖励不能重复领取', () => {
    const app = useAppStore(),
      proto = usePrototypeStore(),
      challenge = proto.data.challenges[0]!;
    app.reset(false);
    proto.claim(challenge);
    expect(proto.rewardPoints).toBe(0);
    proto.joinChallenge(challenge.id);
    const bundle = templateBundle(growthTemplates[0]!);
    proto.applyBundle(bundle);
    setTaskDone(app.data, bundle.tasks[0]!.id, true);
    proto.claim(challenge);
    expect(proto.rewardPoints).toBe(0);
    setHabitDone(app.data, bundle.habits[0]!.id, localDate(), true);
    proto.claim(challenge);
    proto.claim(challenge);
    expect(proto.rewardPoints).toBe(40);
    expect(proto.score).toBe(app.xp + 40);
  });
  it('成长和原型备份一起校验，旧版兼容，非法奖励阻止导入', () => {
    const app = useAppStore(),
      proto = usePrototypeStore();
    expect(validateBackup(app.data).prototype).toBeNull();
    const backup = JSON.parse(JSON.stringify({ ...app.data, prototype: proto.data }));
    expect(validateBackup(backup).prototype?.posts).toHaveLength(8);
    backup.prototype.rewards.push({ challengeId: 'daily', date: localDate(), amount: 999 });
    expect(() => validateBackup(backup)).toThrow('挑战奖励');
  });
  it('段位按个人积分计算，不从样例用户继承', () => {
    expect(getTier(0).title).toContain('青铜');
    expect(getTier(800).title).toContain('黄金');
    expect(getTier(6000).title).toContain('大师');
  });
});
