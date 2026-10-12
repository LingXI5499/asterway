import { z } from 'zod';
import { validDate } from './dates';
import type { PrototypeData } from './prototype';
const text = z.string().max(20000),
  id = z.string().min(1).max(100),
  count = z.number().int().min(0);
const time = z.string().refine((v) => !Number.isNaN(Date.parse(v)));
const date = z.string().refine(validDate);
const message = z.object({ id, authorId: id, text, createdAt: time });
const schema = z.object({
  version: z.literal(2),
  reminders: z
    .array(
      z.object({
        id,
        title: text,
        time: z.string().regex(/^([01]\d|2[0-3]):[0-5]\d$/),
        enabled: z.boolean(),
        createdAt: time,
        lastFiredDate: date.optional(),
      }),
    )
    .optional(),
  users: z.array(
    z.object({
      id,
      name: text,
      bio: text,
      persona: text,
      color: text,
      tier: text,
      followers: count,
      following: count,
      score: count,
    }),
  ),
  posts: z.array(
    z.object({
      id,
      authorId: id,
      type: z.enum(['article', 'video', 'plan']),
      title: text,
      content: text,
      topic: text,
      cover: text,
      media: z.string().max(1800000).optional(),
      images: z.array(z.string().max(1800000)).max(6).optional(),
      templateId: id.optional(),
      planActions: z.array(text).optional(),
      planCategory: z.enum(['study', 'health', 'work', 'life', 'creative', 'other']).optional(),
      planDays: count.optional(),
      circleId: id.optional(),
      createdAt: time,
      likes: count,
      saves: count,
      liked: z.boolean(),
      saved: z.boolean(),
      comments: z.array(message),
    }),
  ),
  circles: z.array(
    z.object({
      id,
      name: text,
      description: text,
      topic: text,
      cover: text,
      members: count,
      joined: z.boolean(),
      roomId: id,
    }),
  ),
  rooms: z.array(
    z.object({
      id,
      kind: z.enum(['direct', 'group']),
      name: text,
      peerId: id.optional(),
      circleId: id.optional(),
      unread: count,
      messages: z.array(message),
    }),
  ),
  notes: z.array(
    z.object({
      id,
      title: text,
      content: text,
      tags: z.array(text),
      source: z.enum(['manual', 'assistant', 'community']),
      createdAt: time,
      updatedAt: time,
    }),
  ),
  followedIds: z.array(id),
  challenges: z.array(
    z.object({
      id,
      title: text,
      description: text,
      kind: z.enum(['daily', 'habit', 'focus']),
      days: count,
      reward: count,
      cover: text,
      participants: count,
    }),
  ),
  joinedChallengeIds: z.array(id),
  challengeJoinedDates: z.record(id, date),
  rewards: z.array(z.object({ challengeId: id, date, amount: count })),
  membership: z.enum(['free', 'plus', 'pro']),
  persona: text,
  assistantAutoAdd: z.boolean(),
  conversations: z.array(
    z.object({
      id,
      title: text,
      messages: z.array(z.object({ role: z.enum(['user', 'assistant']), text })),
    }),
  ),
  bundleLogs: z.array(
    z.object({
      id,
      title: text,
      createdAt: time,
      goalIds: z.array(id),
      milestoneIds: z.array(id),
      taskIds: z.array(id),
      habitIds: z.array(id),
      noteIds: z.array(id),
      snapshot: z.string().max(300000),
      undone: z.boolean(),
    }),
  ),
});
export function validatePrototype(input: unknown): PrototypeData {
  const result = schema.safeParse(input);
  if (!result.success) throw new Error('社区与伙伴原型数据格式不正确。');
  const data = result.data;
  for (const list of [
    data.users,
    data.posts,
    data.circles,
    data.rooms,
    data.notes,
    data.challenges,
    data.conversations,
    data.bundleLogs,
  ])
    if (new Set(list.map((item) => item.id)).size !== list.length)
      throw new Error('原型数据存在重复 ID。');
  if (data.posts.some((p) => p.authorId !== 'me' && !data.users.some((u) => u.id === p.authorId)))
    throw new Error('内容用户关联无效。');
  if (
    data.rewards.some(
      (r) => !data.challenges.some((c) => c.id === r.challengeId && c.reward === r.amount),
    )
  )
    throw new Error('挑战奖励记录不合法。');
  if (new Set(data.rewards.map((r) => `${r.challengeId}:${r.date}`)).size !== data.rewards.length)
    throw new Error('挑战奖励存在重复。');
  return data;
}
