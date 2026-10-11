<script setup lang="ts">
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
import BundlePreview from '../components/BundlePreview.vue';
import PageHeader from '../components/PageHeader.vue';
import Icon from '../components/Icon.vue';
import EmptyState from '../components/EmptyState.vue';
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
  input.value = '';
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
    if (typeof query.goal === 'string') {
      goalId.value = query.goal;
      generate('goal');
    } else if (query.mode === 'time') generate('time');
    else if (query.mode === 'review') generate('review');
  },
  { immediate: true },
);
const taskTitle = (id: string) => store.data.tasks.find((t) => t.id === id)?.title || '任务已删除';
function cancelProposal() {
  proposal.value = null;
  store.toast('已取消，任务没有改变');
}
</script>
<template>
  <PageHeader
    title="成长伙伴"
    subtitle="计划不是一成不变的。让今天的行动，适应真实的你。"
    eyebrow="YOUR PERSONAL GROWTH AGENT"
    ><span class="local-badge"
      ><Icon name="shield" :size="16" />本地规则演示 · 未接入真实 AI</span
    ></PageHeader
  >
  <div class="assistant-layout">
    <section class="card chat-panel">
      <div class="chat-header">
        <span class="brand-symbol"><Icon name="sparkles" :size="21" /></span>
        <div>
          <h3>Aster · 你的成长伙伴</h3>
          <small>对话、执行、记忆 · 本地模拟 AI</small>
        </div>
        <span class="local-status">本地</span>
      </div>
      <div class="assistant-conversations">
        <select
          :value="conversationId"
          aria-label="选择成长会话"
          @change="selectConversation(($event.target as HTMLSelectElement).value)"
        >
          <option
            v-for="conversation in proto.data.conversations"
            :key="conversation.id"
            :value="conversation.id"
          >
            {{ conversation.title }}
          </option></select
        ><button class="text-button" @click="newConversation">+ 新对话</button>
      </div>
      <div class="assistant-suggestions">
        <button class="chip" @click="assistantPlan('为我制定 30 天雅思备考计划')">
          建立雅思计划</button
        ><button class="chip" @click="assistantPlan('添加习惯「每天阅读 20 分钟」')">
          帮我添加习惯</button
        ><button
          class="chip"
          @click="assistantPlan('记住：我每天有 40 分钟学习，想先理解框架再做练习')"
        >
          记住我的情况</button
        ><button class="chip" @click="assistantPlan('看看我的个人知识库')">查看个人记忆</button>
      </div>
      <div class="quick-commands">
        <div class="quick-command">
          <span><Icon name="clock" :size="16" />今天时间不足</span>
          <div>
            <input
              v-model="minutes"
              type="number"
              min="1"
              max="1440"
              aria-label="今天剩余分钟"
            /><small>分钟</small
            ><button class="text-button" @click="generate('time')">
              帮我调整<Icon name="arrow" :size="14" />
            </button>
          </div>
        </div>
        <div class="quick-command">
          <span><Icon name="target" :size="16" />把目标拆成行动</span>
          <div>
            <select v-model="goalId" aria-label="选择拆解目标">
              <option value="" disabled>选择活跃目标</option>
              <option v-for="goal in activeGoals" :key="goal.id" :value="goal.id">
                {{ goal.title }}
              </option></select
            ><button class="text-button" :disabled="!goalId" @click="generate('goal')">
              拆解<Icon name="arrow" :size="14" />
            </button>
          </div>
        </div>
        <button class="quick-review" @click="generate('review')">
          <Icon name="chart" :size="16" />看看我本周的情况<Icon name="arrow" :size="14" />
        </button>
      </div>
      <div class="messages" aria-live="polite">
        <div v-for="(message, index) in messages" :key="index" :class="['message', message.role]">
          <span v-if="message.role === 'assistant'" class="message-avatar"
            ><Icon name="sparkles" :size="17"
          /></span>
          <p>{{ message.text }}</p>
        </div>
      </div>
      <form class="chat-input" @submit.prevent="send">
        <input
          v-model="input"
          maxlength="500"
          placeholder="创建计划、添加任务、记录信息，或一起复盘…"
          aria-label="给成长伙伴的指令"
        /><button class="button primary" type="submit" aria-label="发送指令">
          <Icon name="send" :size="18" />
        </button>
      </form>
      <label class="assistant-auto"
        ><input
          v-model="proto.data.assistantAutoAdd"
          type="checkbox"
        />允许发送后自动执行新增（仅本地演示，可撤销）</label
      >
      <small class="chat-disclaimer"
        >未连接大模型；默认预览后确认。开启自动执行仅影响新增，不自动删除或改期。</small
      >
    </section>
    <section class="proposal-column">
      <div class="context-card">
        <div class="section-heading">
          <h3>现在的你</h3>
          <Icon name="map" :size="17" />
        </div>
        <div class="context-stats">
          <div>
            <strong>{{ todayTasks.length }}</strong
            ><span>今日待办</span>
          </div>
          <div>
            <strong>{{ activeGoals.length }}</strong
            ><span>活跃目标</span>
          </div>
          <div>
            <strong
              >{{ todayTasks.reduce((n, t) => n + t.durationMinutes, 0) }}<small>m</small></strong
            ><span>预估时长</span>
          </div>
        </div>
      </div>
      <div class="card proposal-card">
        <div class="section-heading">
          <h2>计划变更预览</h2>
          <span class="demo-badge">确认后生效</span>
        </div>
        <template v-if="bundle"
          ><BundlePreview :bundle="bundle" />
          <div class="proposal-actions">
            <button class="button secondary" @click="bundle = null">取消新增</button
            ><button class="button primary" @click="applyNewBundle">确认批量创建</button>
          </div></template
        >
        <template v-else-if="proposal"
          ><h3>{{ proposal.title }}</h3>
          <p class="muted proposal-explanation">{{ proposal.explanation }}</p>
          <div v-if="proposal.keptTaskIds.length" class="kept-tasks">
            <strong><Icon name="checkCircle" :size="15" />保留在今天</strong>
            <p v-for="id in proposal.keptTaskIds" :key="id">{{ taskTitle(id) }}</p>
          </div>
          <div v-for="(change, index) in proposal.changes" :key="index" class="proposal-change">
            <span :class="['change-type', change.op === 'reschedule' ? 'reschedule' : 'create']">{{
              change.op === 'reschedule' ? '延后' : '新增'
            }}</span>
            <div>
              <strong>{{
                change.op === 'reschedule' ? taskTitle(change.taskId) : change.task.title
              }}</strong>
              <div class="change-diff">
                <template v-if="change.op === 'reschedule'"
                  ><span>{{ change.from ? shortDate(change.from) : '待安排' }}</span
                  ><Icon name="arrow" :size="14" /><b>{{ shortDate(change.to) }}</b></template
                ><template v-else
                  ><b>{{
                    change.task.scheduledDate ? shortDate(change.task.scheduledDate) : '待安排'
                  }}</b
                  ><span>· {{ change.task.durationMinutes }} 分钟</span></template
                >
              </div>
              <p>{{ change.reason }}</p>
            </div>
          </div>
          <div class="proposal-actions">
            <button class="button secondary" @click="cancelProposal">
              {{ proposal.changes.length ? '取消方案' : '关闭摘要' }}</button
            ><button v-if="proposal.changes.length" class="button primary" @click="apply">
              <Icon name="check" :size="16" />确认应用
            </button>
          </div></template
        ><EmptyState
          v-else
          title="先聊聊，再决定"
          text="选择左侧指令，这里会展示具体改动。每一步都由你确认。"
          icon="sparkles"
        />
      </div>
      <div class="card assistant-memory">
        <div class="section-heading">
          <h3><Icon name="notebook" :size="17" />个人知识库</h3>
          <RouterLink class="text-button" to="/knowledge"
            >管理记忆<Icon name="arrow" :size="13"
          /></RouterLink>
        </div>
        <p v-for="note in proto.data.notes.slice(0, 2)" :key="note.id">
          <strong>{{ note.title }}</strong
          ><span>{{ note.content.slice(0, 90) }}</span>
        </p>
        <small class="muted">伙伴只引用你主动保存在本地的内容。</small>
      </div>
      <div v-if="proto.data.bundleLogs.length" class="card action-log">
        <div class="section-heading">
          <h3>最近一次批量创建</h3>
          <span class="demo-badge">{{
            proto.data.bundleLogs.at(-1)?.undone ? '已撤销' : '已建立'
          }}</span>
        </div>
        <p>{{ proto.data.bundleLogs.at(-1)?.title }}</p>
        <button
          v-if="!proto.data.bundleLogs.at(-1)?.undone"
          class="text-button"
          @click="proto.undoBundle"
        >
          <Icon name="undo" :size="15" />撤销最近一次创建
        </button>
      </div>
      <div v-if="latest" class="card action-log">
        <div class="section-heading">
          <h3>最近一次调整</h3>
          <span class="subtle-label">{{ latest.undoneAt ? '已撤销' : '已应用' }}</span>
        </div>
        <p>{{ latest.title }}</p>
        <small class="muted"
          >{{ new Date(latest.appliedAt).toLocaleString('zh-CN') }} ·
          {{ latest.afterTasks.length }} 项变更</small
        ><button v-if="!latest.undoneAt" class="text-button" @click="store.undo()">
          <Icon name="undo" :size="15" />撤销最近一次方案
        </button>
      </div>
    </section>
  </div>
</template>
