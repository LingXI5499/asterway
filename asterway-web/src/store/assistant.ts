import { defineStore } from 'pinia';

import { computed, ref, watch, toRaw } from 'vue';
import { useRoute } from 'vue-router';
import { useAppStore } from '../store/app';
import { ruleAgent } from '../agent/rules';
import type { AgentProposal } from '../domain/types';
import { shortDate, now, uid } from '../domain/dates';
import { usePrototypeStore } from '../store/prototype';
import { growthTemplates, type GrowthTemplate } from '../data/templates';
import { templateBundle, knowledgeBundle } from '../agent/bundles';
import type { GrowthBundle } from '../domain/prototype';
import { makeTask } from '../domain/mutations';
import { createDailyPlan } from '../domain/productivity';
import { ui } from './ui';
export const useAssistantStore = defineStore('assistant', () => {
  const store = useAppStore(),
    route = useRoute(),
    minutes = ref(40),
    goalId = ref(''),
    input = ref(''),
    proposal = ref<AgentProposal | null>(null),
    messages = ref<{ role: 'assistant' | 'user'; text: string }[]>([
      {
        role: 'assistant',
        text: '你好，我是你的成长伙伴。计划可以随生活调整。我可以帮你精简今天、拆解目标，或回顾这一周。每次改动都会先给你看，确认后才应用。',
      },
    ]);
  const proto = usePrototypeStore(),
    bundle = ref<GrowthBundle | null>(null),
    conversationId = ref(proto.data.conversations[0]?.id || '');
  const pendingReminder = ref<{ title: string; time: string } | null>(null);
  watch(
    messages,
    (value) => {
      const current = proto.data.conversations.find((c) => c.id === conversationId.value);
      if (current) current.messages = value;
    },
    { deep: true },
  );
  function selectConversation(id: string) {
    conversationId.value = id;
    messages.value = proto.data.conversations.find((c) => c.id === id)?.messages || [];
    bundle.value = null;
    proposal.value = null;
    pendingReminder.value = null;
  }
  selectConversation(conversationId.value);
  function newConversation() {
    const conversation = {
      id: uid(),
      title: `成长对话 ${proto.data.conversations.length + 1}`,
      messages: [
        {
          role: 'assistant' as const,
          text: '今天想从哪个方向开始？我可以建立计划、添加行动、记录偏好或帮你复盘。当前为本地规则演示。',
        },
      ],
    };
    proto.data.conversations.unshift(conversation);
    selectConversation(conversation.id);
  }
  function presentBundle(value: GrowthBundle, prompt: string) {
    if (value.goals.length) {
      const memory = proto.data.notes.find((note) => /每天.*\d+\s*分钟/.test(note.content));
      const budget = Number(memory?.content.match(/每天[^\d]*(\d+)\s*分钟/)?.[1]);
      if (memory && budget >= 5 && budget <= 240) {
        value.tasks.forEach((task) => {
          task.durationMinutes = Math.min(task.durationMinutes, budget);
        });
        value.goals.forEach((goal) => {
          goal.weeklyHours = Math.round(((budget * 7) / 60) * 10) / 10;
        });
        value.explanation += ` 参考知识库「${memory.title}」：单次行动不超过 ${budget} 分钟。`;
      }
    }
    bundle.value = value;
    proposal.value = null;
    messages.value.push(
      { role: 'user', text: prompt },
      {
        role: 'assistant',
        text: `${value.title}。我准备了 ${value.goals.length} 个目标、${value.tasks.length} 项任务、${value.habits.length} 个习惯和 ${value.notes.length} 条记忆。${proto.data.assistantAutoAdd ? '你已开启新增自动执行，我会直接建立这些内容。' : '检查右侧草案，确认后即可一起建立，不需要逐项填写表单。'}`,
      },
    );
    if (proto.data.assistantAutoAdd) applyNewBundle();
  }
  function applyNewBundle() {
    if (bundle.value && proto.applyBundle(toRaw(bundle.value))) {
      bundle.value = null;
      messages.value.push({
        role: 'assistant',
        text: '已完成创建，个人工作台与知识库已同步。你可以继续调整，或撤销最近一次批量创建。',
      });
    }
  }
  function assistantPlan(text: string): boolean {
    pendingReminder.value = null;
    if (/提醒/.test(text)) {
      const match = text.match(/([01]?\d|2[0-3])[:：]([0-5]\d)/);
      if (!match) {
        reply(
          text,
          '请告诉我时间和事项，例如“每天 20:30 提醒我复盘”。提醒会保存在本地，仅应用打开时显示站内提示。',
        );
        return true;
      }
      pendingReminder.value = {
        title: (
          text.match(/[「“](.*?)[」”]/)?.[1] ||
          text.replace(/每天|提醒我|提醒|[\d:：]/g, '').trim() ||
          '回顾今日成长'
        ).slice(0, 60),
        time: `${match[1]!.padStart(2, '0')}:${match[2]}`,
      };
      bundle.value = null;
      proposal.value = null;
      reply(
        text,
        `准备每天 ${pendingReminder.value.time} 提醒你「${pendingReminder.value.title}」。请确认提醒草案；当前为应用打开时的站内提醒。`,
      );
      return true;
    }
    if (/怎么用|如何使用|帮助|能做什么|这个页面/.test(text)) {
      reply(
        text,
        `你正在「${route.meta.title || '成长工作台'}」。${pageHelp[route.path] || '可以用全局伙伴创建目标、添加任务和习惯、记录个人知识、安排时间或回顾成长。'}\n我可以操作本地成长数据；联网社区、支付和开放问答需要后续接入真实服务。`,
      );
      return true;
    }
    if (/分析.*(记忆|知识|记录)|回忆|回顾.*(记忆|知识|记录)/.test(text)) {
      const notes = proto.data.notes.slice(0, 5);
      reply(
        text,
        notes.length
          ? `根据你主动保存的 ${proto.data.notes.length} 条个人记录，先回看这些线索：\n${notes.map((n) => `「${n.title}」：${n.content.slice(0, 160)}`).join('\n')}\n可以从其中一个方向制定目标，把记录转成具体行动。这里引用原始记录，不推断未记录的个人经历。`
          : '目前还没有个人记录。可以说“记住：我每天有 30 分钟学习”，先建立一条真实的成长记忆。',
      );
      return true;
    }
    if (/完成情况|今天.*(情况|进度)|我的.*进度/.test(text)) {
      const tasks = store.tasks.filter((t) => t.scheduledDate === store.today);
      reply(
        text,
        `今天完成 ${tasks.filter((t) => t.status === 'done').length} / ${tasks.length} 项任务，还有 ${todayTasks.value.length} 项待办；活跃目标 ${activeGoals.value.length} 个。可以让我安排今日三件事，或一起复盘本周。`,
      );
      return true;
    }
    if (/四象限|三件事|时间分块|按优先级/.test(text) && /安排|计划|整理|制定/.test(text)) {
      try {
        const settings = store.data.profile.planning;
        const minutes = Number(text.match(/(\d+)\s*分钟/)?.[1] || settings?.dailyMinutes || 120);
        const result = createDailyPlan(toRaw(store.data), {
          date: store.today,
          minutes,
          startTime: settings?.startTime || '09:00',
          buffer: settings?.bufferMinutes ?? 5,
          reserve: settings?.reservePercent ?? 15,
          limit: /时间分块/.test(text) ? 6 : 3,
          energy: store.data.profile.dailyEnergyByDate[store.today] || 'normal',
        });
        bundle.value = null;
        proposal.value = result.proposal;
        messages.value.push(
          { role: 'user', text },
          {
            role: 'assistant',
            text: `我按重要性、紧急性和可用时间准备了 ${result.slots.length} 项行动，保留 ${result.reserved} 分钟留白。${result.skipped.length ? '装不下的任务保留原安排，不自动延后。' : ''}检查右侧具体时段，确认后排入日程。改期始终需要确认。`,
          },
        );
      } catch (error) {
        store.toast(error instanceof Error ? error.message : '无法生成计划', true);
      }
      return true;
    }
    if (/记住|记下|记录我的/.test(text)) {
      presentBundle(knowledgeBundle(text), text);
      return true;
    }
    const keys: Record<string, string[]> = {
      soft: ['软件设计师', '软考'],
      graduate: ['考研'],
      civil: ['公务员', '公考'],
      cpa: ['CPA', '注册会计师'],
      law: ['法考'],
      teacher: ['教师资格'],
      cet4: ['四级'],
      cet6: ['六级'],
      ielts: ['雅思', 'IELTS'],
      toefl: ['托福', 'TOEFL'],
      jlpt: ['JLPT'],
      english: ['英语口语'],
      japanese: ['日语'],
      french: ['法语'],
      spanish: ['西班牙语'],
      reading: ['英语阅读'],
      frontend: ['Vue', 'vue', '前端项目'],
      java: ['Java', 'java'],
      job: ['求职', '面试'],
      writing: ['写作'],
      fitness: ['运动', '健身'],
      sleep: ['作息', '早睡'],
      books: ['读书', '阅读计划'],
      digital: ['刷手机'],
      finance: ['收支', '记账'],
      drawing: ['绘画'],
    };
    const template = growthTemplates.find((t) => keys[t.id]?.some((word) => text.includes(word)));
    if (/添加|新建|创建|建立/.test(text) && /习惯/.test(text) && !/计划|目标/.test(text)) {
      const time = now(),
        title = (
          text.match(/[「“](.*?)[」”]/)?.[1] ||
          text.replace(/请|帮我|添加|新建|创建|建立|一个|习惯|[：:,，]/g, '').trim() ||
          '每天记录一个成长行动'
        ).slice(0, 60);
      presentBundle(
        {
          id: uid(),
          title: '添加一个配套习惯',
          explanation: '通过对话创建习惯，之后可以在习惯页面调整频率。',
          goals: [],
          milestones: [],
          tasks: [],
          notes: [],
          habits: [
            {
              id: uid(),
              title,
              category: 'life',
              frequency: { kind: 'daily' },
              startDate: store.today,
              status: 'active',
              createdAt: time,
            },
          ],
        },
        text,
      );
      return true;
    }
    if (/添加|新建|创建/.test(text) && /任务/.test(text) && !template) {
      const title = (
        text.match(/[「“](.*?)[」”]/)?.[1] ||
        text.replace(/请|帮我|添加|新建|创建|一个|任务|[：:,，]/g, '').trim() ||
        '完成今天的一个小行动'
      ).slice(0, 60);
      presentBundle(
        {
          id: uid(),
          title: '添加今日任务',
          explanation: '通过伙伴创建行动，关联当前选择的目标；可在任务编辑器进一步修改。',
          goals: [],
          milestones: [],
          habits: [],
          notes: [],
          tasks: [
            makeTask({
              title,
              goalId: goalId.value || undefined,
              scheduledDate: store.today,
              durationMinutes: 25,
            }),
          ],
        },
        text,
      );
      return true;
    }
    if (template && /计划|目标|备考|安排|学习|制定/.test(text)) {
      const days = Number(text.match(/(\d+)\s*天/)?.[1] || template.days);
      presentBundle(templateBundle(template, store.today, Math.max(7, Math.min(days, 365))), text);
      return true;
    }
    if (/制定|创建|新建|建立/.test(text) && /目标|计划/.test(text)) {
      const title = (
        text.match(/[「“](.*?)[」”]/)?.[1] ||
        text.replace(/请|帮我|制定|创建|新建|建立|一个|目标|计划|[：:,，]/g, '').trim() ||
        '我的新成长方向'
      ).slice(0, 60);
      const generic: GrowthTemplate = {
        id: 'custom',
        title,
        group: '个人成长',
        category: 'life',
        audience: '成长探索者',
        days: 30,
        description: '从明确结果到行动，再回顾调整。',
        tags: [],
        stages: ['明确结果', '实践行动', '回顾调整'],
        actions: [
          '梳理当前基础和期望结果',
          '挑选一项最小行动',
          '完成第一次实践',
          '记录过程中的问题',
          '做一次阶段检查',
          '调整下一阶段计划',
        ],
        habit: '每天记录一次进步',
      };
      presentBundle(templateBundle(generic, store.today), text);
      return true;
    }
    if (/我的情况|我的信息|知识库|记忆|偏好/.test(text)) {
      const notes = proto.data.notes
        .filter((n) => n.tags.some((tag) => text.includes(tag)) || text.includes(n.title))
        .slice(0, 3);
      const relevant = notes.length ? notes : proto.data.notes.slice(0, 2);
      messages.value.push(
        { role: 'user', text },
        {
          role: 'assistant',
          text: relevant.length
            ? `从你的个人知识库中找到了这些记录：\n${relevant.map((n) => `「${n.title}」：${n.content}`).join('\n')}\n这些内容来自你主动保存的本地记录。你可以让我基于一个具体方向创建计划。`
            : '你的知识库还没有记录。试试“记住：我每天有 40 分钟学习”。',
        },
      );
      return true;
    }
    return false;
  }
  const activeGoals = computed(() => store.data.goals.filter((g) => g.status === 'active'));
  const todayTasks = computed(() =>
    store.tasks.filter((t) => t.scheduledDate === store.today && t.status === 'todo'),
  );
  const latest = computed(() => store.data.agentActionLog.at(-1));
  watch(
    activeGoals,
    (goals) => {
      if (!goals.some((g) => g.id === goalId.value)) goalId.value = goals[0]?.id || '';
    },
    { immediate: true },
  );
  function generate(kind: 'time' | 'goal' | 'review') {
    pendingReminder.value = null;
    bundle.value = null;
    try {
      const data = toRaw(store.data);
      const value =
        kind === 'time'
          ? ruleAgent.reschedule(data, Number(minutes.value), store.today)
          : kind === 'goal'
            ? ruleAgent.decompose(data, goalId.value, store.today)
            : ruleAgent.review(data, store.today);
      proposal.value = value;
      messages.value.push(
        {
          role: 'user',
          text:
            kind === 'time'
              ? `今天只有 ${minutes.value} 分钟，帮我调整计划。`
              : kind === 'goal'
                ? '把这个目标拆成具体行动。'
                : '看看我本周的情况。',
        },
        {
          role: 'assistant',
          text: `${value.title}。${value.changes.length ? '右侧准备好了变更草案，先检查一下，再决定是否应用。' : value.explanation}`,
        },
      );
    } catch (error) {
      store.toast(error instanceof Error ? error.message : '生成失败', true);
    }
  }
  function send() {
    const text = input.value.trim();
    if (!text) return;
    if (!proto.data.conversations.some((c) => c.id === conversationId.value)) newConversation();
    input.value = '';
    pendingReminder.value = null;
    if (assistantPlan(text)) return;
    const number = text.match(/(\d+)\s*分钟/);
    if (number || /时间|不够|疲惫/.test(text)) {
      if (number) minutes.value = Number(number[1]);
      generate('time');
    } else if (/拆|目标/.test(text)) generate('goal');
    else if (/复盘|本周|总结/.test(text)) generate('review');
    else
      messages.value.push(
        { role: 'user', text },
        {
          role: 'assistant',
          text: '当前是本地规则演示，暂不支持开放问答。试试「今天只有 40 分钟」「拆解目标」或「看看本周情况」。',
        },
      );
  }
  function apply() {
    if (proposal.value && store.apply(toRaw(proposal.value))) {
      messages.value.push({
        role: 'assistant',
        text: '方案已应用，今日和日程已同步。你可以撤销最近一次方案。',
      });
      proposal.value = null;
    }
  }
  watch(
    () => route.query,
    (query) => {
      if (route.path !== '/assistant') return;
      if (typeof query.goal === 'string') {
        goalId.value = query.goal;
        generate('goal');
      } else if (query.mode === 'time') generate('time');
      else if (query.mode === 'review') generate('review');
    },
    { immediate: true },
  );
  const taskTitle = (id: string) =>
    store.data.tasks.find((t) => t.id === id)?.title || '任务已删除';
  function cancelProposal() {
    proposal.value = null;
    store.toast('已取消，任务没有改变');
  }

  const opened = ref(false);
  const pageHelp: Record<string, string> = {
    '/methods':
      '新建方法计划时先选方法：四象限分别判断重要与紧急；每日三件事收敛重点；时间分块安排时段；番茄法关联专注；GTD先收集下一步。',
    '/community':
      '推荐流浏览图文与视频；图文页只看笔记，视频页可进入沉浸播放。点击内容点赞、评论、收藏，也可以收入知识库。',
    '/knowledge':
      '保存个人情况、经验与复盘；伙伴能引用这些本地记录。可以让我记住偏好，或回顾已有记忆。',
    '/goals': '一个目标下可建立阶段与多个任务。可以让我制定新目标，或选择活跃目标拆解行动。',
    '/habits': '按每日或每周频率打卡，支持补打最近七天、暂停和归档。可以让我添加一个习惯。',
    '/planner': '查看周日程和待安排任务。时间分块会先给你变更草案，确认后才修改安排。',
  };
  function reply(prompt: string, text: string) {
    messages.value.push({ role: 'user', text: prompt }, { role: 'assistant', text });
  }
  function ask(text: string) {
    input.value = text;
    send();
  }
  function open(prompt?: string) {
    opened.value = true;
    if (prompt) ask(prompt);
  }
  function applyReminder() {
    if (!pendingReminder.value) return;
    store.data.profile.reminder = true;
    (proto.data.reminders ??= []).push({
      id: uid(),
      ...pendingReminder.value,
      enabled: true,
      createdAt: now(),
    });
    pendingReminder.value = null;
    messages.value.push({
      role: 'assistant',
      text: '每日提醒已保存。你可以在全局伙伴的提醒列表中暂停或删除；应用打开时到点显示站内提示。',
    });
  }
  watch(
    () => proto.data,
    (data) => {
      selectConversation(
        data.conversations.some((c) => c.id === conversationId.value)
          ? conversationId.value
          : data.conversations[0]?.id || '',
      );
      pendingReminder.value = null;
    },
    { flush: 'sync' },
  );
  return {
    minutes,
    goalId,
    input,
    proposal,
    messages,
    bundle,
    conversationId,
    activeGoals,
    todayTasks,
    latest,
    selectConversation,
    newConversation,
    assistantPlan,
    generate,
    send,
    apply,
    applyNewBundle,
    cancelProposal,
    taskTitle,
    opened,
    open,
    ask,
    pendingReminder,
    applyReminder,
  };
});
