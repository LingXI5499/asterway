import type { Category, Goal, Habit, Milestone, Task } from './types';
export interface DemoUser {
  id: string;
  name: string;
  bio: string;
  persona: string;
  color: string;
  tier: string;
  followers: number;
  following: number;
  score: number;
}
export interface Post {
  id: string;
  authorId: string;
  type: 'article' | 'video' | 'plan';
  title: string;
  content: string;
  topic: string;
  cover: string;
  media?: string;
  images?: string[];
  templateId?: string;
  planActions?: string[];
  planCategory?: Category;
  planDays?: number;
  circleId?: string;
  createdAt: string;
  likes: number;
  saves: number;
  liked: boolean;
  saved: boolean;
  comments: { id: string; authorId: string; text: string; createdAt: string }[];
}
export interface CircleGroup {
  id: string;
  name: string;
  description: string;
  topic: string;
  cover: string;
  members: number;
  joined: boolean;
  roomId: string;
}
export interface Message {
  id: string;
  authorId: string;
  text: string;
  createdAt: string;
}
export interface ChatRoom {
  id: string;
  kind: 'direct' | 'group';
  name: string;
  peerId?: string;
  circleId?: string;
  unread: number;
  messages: Message[];
}
export interface KnowledgeNote {
  id: string;
  title: string;
  content: string;
  tags: string[];
  source: 'manual' | 'assistant' | 'community';
  createdAt: string;
  updatedAt: string;
}
export interface GrowthBundle {
  id: string;
  title: string;
  explanation: string;
  goals: Goal[];
  milestones: Milestone[];
  tasks: Task[];
  habits: Habit[];
  notes: KnowledgeNote[];
}
export interface BundleLog {
  id: string;
  title: string;
  createdAt: string;
  goalIds: string[];
  milestoneIds: string[];
  taskIds: string[];
  habitIds: string[];
  noteIds: string[];
  snapshot: string;
  undone: boolean;
}
export interface Challenge {
  id: string;
  title: string;
  description: string;
  kind: 'daily' | 'habit' | 'focus';
  days: number;
  reward: number;
  cover: string;
  participants: number;
}
export interface PrototypeData {
  version: 2;
  users: DemoUser[];
  posts: Post[];
  circles: CircleGroup[];
  rooms: ChatRoom[];
  notes: KnowledgeNote[];
  followedIds: string[];
  challenges: Challenge[];
  joinedChallengeIds: string[];
  challengeJoinedDates: Record<string, string>;
  rewards: { challengeId: string; date: string; amount: number }[];
  membership: 'free' | 'plus' | 'pro';
  persona: string;
  assistantAutoAdd: boolean;
  reminders?: {
    id: string;
    title: string;
    time: string;
    enabled: boolean;
    createdAt: string;
    lastFiredDate?: string;
  }[];
  conversations: {
    id: string;
    title: string;
    messages: { role: 'user' | 'assistant'; text: string }[];
  }[];
  bundleLogs: BundleLog[];
}
export const rankTiers = [
  { title: '启程青铜', min: 0, mark: 'I', color: '#b69273', description: '每次开始，都值得鼓励' },
  { title: '积累白银', min: 300, mark: 'II', color: '#9aaeb2', description: '行动正在成为节奏' },
  {
    title: '自律黄金',
    min: 800,
    mark: 'III',
    color: '#c6a059',
    description: '把承诺变成稳定的行动',
  },
  {
    title: '恒心铂金',
    min: 1600,
    mark: 'IV',
    color: '#74a69f',
    description: '用坚持拓宽自己的边界',
  },
  { title: '成长钻石', min: 3000, mark: 'V', color: '#9c8ab8', description: '找到并走稳自己的路' },
  {
    title: '星行大师',
    min: 6000,
    mark: 'VI',
    color: '#cb986c',
    description: '也为同行的人点亮方向',
  },
];
export const getTier = (score: number) =>
  [...rankTiers].reverse().find((t) => score >= t.min) || rankTiers[0]!;
