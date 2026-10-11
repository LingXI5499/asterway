export type Category = 'study' | 'health' | 'work' | 'life' | 'creative' | 'other';
export type Priority = 'high' | 'medium' | 'low';
export interface Goal {
  id: string;
  title: string;
  description: string;
  category: Category;
  status: 'active' | 'paused' | 'completed' | 'archived';
  priority: Priority;
  startDate: string;
  targetDate: string;
  weeklyHours: number;
  createdAt: string;
}
export interface Milestone {
  id: string;
  goalId: string;
  title: string;
  order: number;
  createdAt: string;
}
export interface Task {
  id: string;
  title: string;
  description: string;
  goalId?: string;
  milestoneId?: string;
  priority: Priority;
  durationMinutes: number;
  important?: boolean;
  urgent?: boolean;
  dueDate?: string;
  energy?: 'high' | 'normal' | 'low';
  scheduledDate?: string;
  startTime?: string;
  status: 'todo' | 'done' | 'cancelled';
  completedAt?: string;
  isTodayFocus: boolean;
  createdAt: string;
  updatedAt: string;
  deletedAt?: string;
}
export interface Habit {
  id: string;
  title: string;
  category: Category;
  frequency: { kind: 'daily' } | { kind: 'weekly'; times: number };
  startDate: string;
  status: 'active' | 'paused' | 'archived';
  createdAt: string;
}
export interface HabitLog {
  id: string;
  habitId: string;
  date: string;
  createdAt: string;
}
export interface XpEvent {
  id: string;
  sourceType: 'task' | 'habit';
  sourceId: string;
  sourceDate: string;
  amount: number;
  createdAt: string;
}
export interface Review {
  weekStartDate: string;
  wins: string;
  blockers: string;
  changes: string;
  updatedAt: string;
}
export type NewTask = Omit<Task, 'id' | 'createdAt' | 'updatedAt'>;
export type ProposalChange =
  | {
      op: 'reschedule';
      taskId: string;
      from?: string;
      to: string;
      reason: string;
      toStartTime?: string;
      toFocus?: boolean;
      expectedUpdatedAt?: string;
    }
  | { op: 'create_task'; task: NewTask; reason: string };
export interface AgentProposal {
  id: string;
  type: 'reschedule' | 'create_tasks' | 'weekly_review' | 'daily_plan';
  title: string;
  explanation: string;
  changes: ProposalChange[];
  keptTaskIds: string[];
  createdAt: string;
  planningContext?: { date: string; taskIds: string[]; snapshot: string };
}
export interface AgentAction {
  id: string;
  proposalId: string;
  title: string;
  appliedAt: string;
  beforeTasks: Task[];
  afterTasks: Task[];
  createdTaskIds: string[];
  undoneAt?: string;
}
export interface AppData {
  schemaVersion: 1;
  profile: {
    displayName: string;
    reminder: boolean;
    sidebarCollapsed: boolean;
    onboardingDismissed: boolean;
    dailyEnergyByDate: Record<string, 'high' | 'normal' | 'low'>;
    planning?: {
      dailyMinutes: number;
      startTime: string;
      bufferMinutes: number;
      reservePercent: number;
    };
  };
  goals: Goal[];
  milestones: Milestone[];
  tasks: Task[];
  habits: Habit[];
  habitLogs: HabitLog[];
  xpEvents: XpEvent[];
  weeklyReviews: Review[];
  agentActionLog: AgentAction[];
  focusSessions: {
    id: string;
    date: string;
    minutes: number;
    createdAt: string;
    taskId?: string;
  }[];
}
export const categories: Record<Category, string> = {
  study: '学习',
  health: '健康',
  work: '工作',
  life: '生活',
  creative: '创作',
  other: '其他',
};
export const priorities: Record<Priority, string> = {
  high: '高优先',
  medium: '中优先',
  low: '低优先',
};
export const priorityRank: Record<Priority, number> = { high: 0, medium: 1, low: 2 };
