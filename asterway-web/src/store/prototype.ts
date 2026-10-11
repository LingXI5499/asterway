import { defineStore } from 'pinia';
import { computed, ref, toRaw, watch } from 'vue';
import type { GrowthBundle, Post, KnowledgeNote, Challenge } from '../domain/prototype';
import { getTier } from '../domain/prototype';
import { socialSeed } from '../data/socialSeed';
import { now, uid, localDate } from '../domain/dates';
import { validatePrototype } from '../domain/prototypeSchema';
import { validateData } from '../domain/schema';
import { snapshotBundle } from '../agent/bundles';
import { useAppStore } from './app';
export const PROTOTYPE_KEY = 'asterway:prototype:v2';
export const usePrototypeStore = defineStore('prototype', () => {
  const app = useAppStore(),
    storageError = ref(''),
    recoveryError = ref('');
  let initial = socialSeed();
  try {
    const raw = localStorage.getItem(PROTOTYPE_KEY);
    if (raw) initial = validatePrototype(JSON.parse(raw));
  } catch {
    recoveryError.value = '原型数据读取失败，原数据仍保留，可在设置重置社区演示。';
  }
  const data = ref(initial);
  function persist() {
    if (recoveryError.value) return;
    try {
      localStorage.setItem(PROTOTYPE_KEY, JSON.stringify(data.value));
      storageError.value = '';
    } catch {
      storageError.value =
        '社区原型存储空间不足，请导出备份或移除大媒体。当前操作仅在本次打开有效。';
    }
  }
  watch(data, persist, { deep: true });
  persist();
  const rewardPoints = computed(() => data.value.rewards.reduce((n, r) => n + r.amount, 0));
  const score = computed(() => app.xp + rewardPoints.value),
    tier = computed(() => getTier(score.value));
  const me = computed(() => ({
    id: 'me',
    name: app.data.profile.displayName,
    bio: '找到自己的方向，用行动记录成长。',
    persona: data.value.persona,
    color: 'sage',
    tier: tier.value.title,
    followers: data.value.posts
      .filter((p) => p.authorId === 'me')
      .reduce((n, p) => n + Number(p.liked), 0),
    following: data.value.followedIds.length,
    score: score.value,
  }));
  function user(id: string) {
    return id === 'me' ? me.value : data.value.users.find((u) => u.id === id) || me.value;
  }
  function toggleFollow(id: string) {
    if (id === 'me') return;
    data.value.followedIds = data.value.followedIds.includes(id)
      ? data.value.followedIds.filter((v) => v !== id)
      : [...data.value.followedIds, id];
  }
  function toggleLike(post: Post) {
    post.liked = !post.liked;
    post.likes += post.liked ? 1 : -1;
  }
  function toggleSave(post: Post) {
    post.saved = !post.saved;
    post.saves += post.saved ? 1 : -1;
  }
  function comment(post: Post, text: string) {
    if (!text.trim()) return;
    post.comments.push({ id: uid(), authorId: 'me', text: text.trim(), createdAt: now() });
    app.toast('评论已发布到本地演示');
  }
  function saveNote(
    note: Omit<KnowledgeNote, 'id' | 'createdAt' | 'updatedAt'>,
    existingId?: string,
  ) {
    const time = now();
    if (existingId) {
      const existing = data.value.notes.find((n) => n.id === existingId)!;
      Object.assign(existing, note, { updatedAt: time });
    } else data.value.notes.unshift({ ...note, id: uid(), createdAt: time, updatedAt: time });
    app.toast('已保存到个人知识库');
  }
  function collectPost(post: Post) {
    saveNote({
      title: post.title,
      content: post.content,
      tags: ['社区收藏', post.topic],
      source: 'community',
    });
  }
  function openDm(userId: string) {
    let room = data.value.rooms.find((r) => r.kind === 'direct' && r.peerId === userId);
    if (!room) {
      room = {
        id: uid(),
        name: user(userId).name,
        kind: 'direct',
        peerId: userId,
        unread: 0,
        messages: [],
      };
      data.value.rooms.push(room);
    }
    return room.id;
  }
  function joinCircle(circleId: string) {
    const circle = data.value.circles.find((c) => c.id === circleId)!;
    circle.joined = !circle.joined;
    if (circle.joined && !data.value.rooms.some((r) => r.id === circle.roomId))
      data.value.rooms.push({
        id: circle.roomId,
        kind: 'group',
        name: circle.name,
        circleId,
        unread: 0,
        messages: [
          {
            id: uid(),
            authorId: 'lin',
            text: '欢迎加入。分享一个你今天想完成的小行动吧！（群聊为本地演示）',
            createdAt: now(),
          },
        ],
      });
    app.toast(circle.joined ? '已加入演示社群，群聊已开启' : '已退出演示社群');
  }
  function sendMessage(roomId: string, text: string) {
    const room = data.value.rooms.find((r) => r.id === roomId);
    if (!room || !text.trim()) return;
    if (room.circleId && !data.value.circles.some((c) => c.id === room.circleId && c.joined)) {
      app.toast('请先加入这个社群', true);
      return;
    }
    room.messages.push({ id: uid(), authorId: 'me', text: text.trim(), createdAt: now() });
    room.messages.push({
      id: uid(),
      authorId: room.peerId || 'lin',
      text: '【模拟回复】收到啦。给自己定一个今天可以执行的小行动，一起慢慢向前。',
      createdAt: now(),
    });
    room.unread = 0;
  }
  function applyBundle(bundle: GrowthBundle) {
    try {
      if (data.value.bundleLogs.some((l) => l.id === bundle.id))
        throw new Error('这份计划已经应用。');
      const draft = structuredClone(toRaw(app.data));
      draft.goals.push(...structuredClone(toRaw(bundle.goals)));
      draft.milestones.push(...structuredClone(toRaw(bundle.milestones)));
      draft.tasks.push(...structuredClone(toRaw(bundle.tasks)));
      draft.habits.push(...structuredClone(toRaw(bundle.habits)));
      const checked = validateData(draft);
      app.data = checked;
      data.value.notes.push(...structuredClone(toRaw(bundle.notes)));
      data.value.bundleLogs.push({
        id: bundle.id,
        title: bundle.title,
        createdAt: now(),
        goalIds: bundle.goals.map((g) => g.id),
        milestoneIds: bundle.milestones.map((m) => m.id),
        taskIds: bundle.tasks.map((t) => t.id),
        habitIds: bundle.habits.map((h) => h.id),
        noteIds: bundle.notes.map((n) => n.id),
        snapshot: snapshotBundle(bundle, app.data, data.value.notes),
        undone: false,
      });
      app.toast('计划已建立，目标、任务、习惯和知识库已同步');
      return true;
    } catch (error) {
      app.toast(error instanceof Error ? error.message : '应用失败', true);
      return false;
    }
  }
  function undoBundle() {
    const log = data.value.bundleLogs.at(-1);
    if (!log || log.undone) return;
    const bundle: GrowthBundle = {
      id: log.id,
      title: log.title,
      explanation: '',
      goals: app.data.goals.filter((g) => log.goalIds.includes(g.id)),
      milestones: app.data.milestones.filter((m) => log.milestoneIds.includes(m.id)),
      tasks: app.data.tasks.filter((t) => log.taskIds.includes(t.id)),
      habits: app.data.habits.filter((h) => log.habitIds.includes(h.id)),
      notes: data.value.notes.filter((n) => log.noteIds.includes(n.id)),
    };
    if (
      snapshotBundle(bundle, app.data, data.value.notes) !== log.snapshot ||
      app.data.tasks.some(
        (t) => t.goalId && log.goalIds.includes(t.goalId) && !log.taskIds.includes(t.id),
      ) ||
      app.data.milestones.some(
        (m) => log.goalIds.includes(m.goalId) && !log.milestoneIds.includes(m.id),
      ) ||
      app.data.habitLogs.some((l) => log.habitIds.includes(l.habitId))
    ) {
      app.toast('相关内容已发生新修改，无法撤销，以免覆盖你的新记录。', true);
      return;
    }
    app.data.goals = app.data.goals.filter((g) => !log.goalIds.includes(g.id));
    app.data.milestones = app.data.milestones.filter((m) => !log.milestoneIds.includes(m.id));
    app.data.tasks = app.data.tasks.filter((t) => !log.taskIds.includes(t.id));
    app.data.habits = app.data.habits.filter((h) => !log.habitIds.includes(h.id));
    data.value.notes = data.value.notes.filter((n) => !log.noteIds.includes(n.id));
    log.undone = true;
    app.toast('已撤销最近一次批量创建');
  }
  function challengeProgress(challenge: Challenge) {
    const today = app.today,
      start = data.value.challengeJoinedDates[challenge.id] || today;
    if (challenge.kind === 'daily') {
      const tasks = app.data.tasks.filter(
        (t) => t.status === 'done' && t.completedAt && localDate(new Date(t.completedAt)) === today,
      ).length;
      const habits = app.data.habitLogs.filter((l) => l.date === today).length;
      return {
        count: Number(tasks > 0) + Number(habits > 0),
        target: 2,
        label: `${tasks} 项任务 · ${habits} 次打卡`,
      };
    }
    if (challenge.kind === 'habit') {
      const count = new Set(
        app.data.habitLogs.filter((l) => l.date >= start && l.date <= today).map((l) => l.date),
      ).size;
      return { count: Math.min(count, 7), target: 7, label: `${count}/7 个打卡日期` };
    }
    const count = app.data.focusSessions
      .filter((s) => s.date >= start && s.date <= today)
      .reduce((n, s) => n + s.minutes, 0);
    return { count: Math.min(count, 25), target: 25, label: `${count}/25 分钟实际专注` };
  }
  function rewardClaimed(challenge: Challenge) {
    return data.value.rewards.some(
      (r) => r.challengeId === challenge.id && (challenge.kind !== 'daily' || r.date === app.today),
    );
  }
  function joinChallenge(id: string) {
    if (data.value.joinedChallengeIds.includes(id)) return;
    data.value.joinedChallengeIds.push(id);
    data.value.challengeJoinedDates[id] = app.today;
    app.toast('已报名，完成真实行动后可领取积分');
  }
  function claim(challenge: Challenge) {
    const progress = challengeProgress(challenge);
    if (
      !data.value.joinedChallengeIds.includes(challenge.id) ||
      progress.count < progress.target ||
      rewardClaimed(challenge)
    ) {
      app.toast('尚未达成或奖励已领取', true);
      return;
    }
    data.value.rewards.push({
      challengeId: challenge.id,
      date: app.today,
      amount: challenge.reward,
    });
    app.toast(`已领取 ${challenge.reward} 挑战积分！`);
  }
  function importData(input: unknown) {
    const checked = validatePrototype(input);
    recoveryError.value = '';
    data.value = checked;
    persist();
  }
  function reset(demo = true) {
    recoveryError.value = '';
    data.value = socialSeed();
    if (!demo) {
      data.value.posts = [];
      data.value.notes = [];
      data.value.conversations = [{ id: uid(), title: '新的成长对话', messages: [] }];
    }
    persist();
  }
  return {
    data,
    storageError,
    recoveryError,
    score,
    tier,
    rewardPoints,
    me,
    user,
    toggleFollow,
    toggleLike,
    toggleSave,
    comment,
    saveNote,
    collectPost,
    openDm,
    joinCircle,
    sendMessage,
    applyBundle,
    undoBundle,
    challengeProgress,
    rewardClaimed,
    joinChallenge,
    claim,
    reset,
    importData,
  };
});
