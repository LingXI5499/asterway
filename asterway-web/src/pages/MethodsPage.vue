<script setup lang="ts">
import { computed, ref, watch, toRaw } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAppStore } from '../store/app';
import { ui } from '../store/ui';
import { now } from '../domain/dates';
import {
  quadrants,
  quadrantOf,
  rankTasks,
  createDailyPlan,
  priorityReason,
  timeMinutes,
  type Quadrant,
  type DailyDraft,
} from '../domain/productivity';
import type { Task } from '../domain/types';
import PageHeader from '../components/PageHeader.vue';
import Icon from '../components/Icon.vue';
import EmptyState from '../components/EmptyState.vue';
import Modal from '../components/Modal.vue';
import MethodPlanBuilder from '../components/MethodPlanBuilder.vue';
import { planningMethods } from '../domain/methodPlans';
const app = useAppStore(),
  route = useRoute(),
  router = useRouter();
const tab = ref('matrix'),
  query = ref(''),
  goal = ref('all'),
  date = ref(app.today),
  scope = ref('all');
const settings = app.data.profile.planning;
const minutes = ref(settings?.dailyMinutes ?? 120),
  start = ref(settings?.startTime ?? '09:00'),
  buffer = ref(settings?.bufferMinutes ?? 5),
  reserve = ref(settings?.reservePercent ?? 15);
const selected = ref<string[]>([]),
  building = ref(false),
  buildingMethod = ref('matrix'),
  draft = ref<DailyDraft | null>(null),
  dragging = ref(''),
  pickMode = ref('auto');
const pending = computed(() =>
  app.tasks.filter(
    (t) =>
      t.status === 'todo' &&
      (!t.goalId || app.data.goals.some((g) => g.id === t.goalId && g.status === 'active')),
  ),
);
const tasks = computed(() =>
  rankTasks(
    pending.value.filter(
      (t) =>
        (goal.value === 'all' || t.goalId === goal.value) &&
        `${t.title}${t.description}`.toLowerCase().includes(query.value.toLowerCase().trim()) &&
        (scope.value !== 'today' || !t.scheduledDate || t.scheduledDate <= date.value),
    ),
    date.value,
    app.data.profile.dailyEnergyByDate[date.value] || 'normal',
  ),
);
const eligible = computed(() =>
  tasks.value.filter((t) => !t.scheduledDate || t.scheduledDate <= date.value),
);
const limit = computed(() => (tab.value === 'blocks' ? 6 : 3));
const availableWindow = computed(() =>
  /^([01]\d|2[0-3]):[0-5]\d$/.test(start.value)
    ? Math.max(0, Math.min(Number(minutes.value) || 0, 1440 - timeMinutes(start.value)))
    : 0,
);
const displayedCapacity = computed(
  () => availableWindow.value - Math.floor((availableWindow.value * Number(reserve.value)) / 100),
);
const selectedMinutes = computed(() =>
  eligible.value
    .filter((t) => selected.value.includes(t.id))
    .reduce((sum, t) => sum + t.durationMinutes, 0),
);
const latest = computed(() => app.data.agentActionLog.at(-1));
const methodPlans = computed(() =>
  app.data.goals
    .filter((g) => g.planningMethod && g.status === 'active')
    .slice(-6)
    .reverse(),
);
watch(
  () => route.query.tab,
  (value) => {
    tab.value = ['matrix', 'top3', 'blocks', 'library'].includes(String(value))
      ? String(value)
      : 'matrix';
  },
  { immediate: true },
);
watch([tab, date], () => {
  selected.value = [];
  draft.value = null;
});
function changeTab(value: string) {
  tab.value = value;
  router.replace({ query: { tab: value } });
}
function moveTask(id: string, quadrant: Quadrant) {
  const task = app.data.tasks.find((t) => t.id === id && t.status === 'todo' && !t.deletedAt);
  if (!task) return;
  task.important = ['do', 'schedule'].includes(quadrant);
  task.urgent = ['do', 'batch'].includes(quadrant);
  task.updatedAt = now();
  app.toast('重要性与紧急性已更新，原排期保持不变');
}
function drop(event: DragEvent, id: Quadrant) {
  const taskId = event.dataTransfer?.getData('text/plain') || dragging.value;
  if (taskId) moveTask(taskId, id);
  dragging.value = '';
}
function drag(event: DragEvent, task: Task) {
  dragging.value = task.id;
  event.dataTransfer?.setData('text/plain', task.id);
}
function choose(id: string) {
  if (selected.value.includes(id)) selected.value = selected.value.filter((v) => v !== id);
  else if (selected.value.length < limit.value) selected.value.push(id);
  else app.toast(`最多选择 ${limit.value} 项，先留出空间`, true);
}
function generate() {
  try {
    draft.value = createDailyPlan(toRaw(app.data), {
      date: date.value,
      minutes: Number(minutes.value),
      startTime: start.value,
      buffer: Number(buffer.value),
      reserve: Number(reserve.value),
      limit: limit.value,
      energy: app.data.profile.dailyEnergyByDate[date.value] || 'normal',
      selectedIds: pickMode.value === 'manual' ? selected.value : undefined,
    });
  } catch (error) {
    app.toast(error instanceof Error ? error.message : '无法生成计划', true);
  }
}
function apply() {
  if (draft.value && app.apply(draft.value.proposal)) {
    app.data.profile.planning = {
      dailyMinutes: Number(minutes.value),
      startTime: start.value,
      bufferMinutes: Number(buffer.value),
      reservePercent: Number(reserve.value),
    };
    draft.value = null;
    app.toast('计划已排入日程，今日重点已同步');
    router.push(`/planner?date=${date.value}`);
  }
}
const methods = [
  {
    id: 'matrix',
    name: '四象限',
    label: '先判断值得做什么',
    icon: 'dashboard',
    text: '分别判断重要性和紧急性：优先处理关键紧急事项，给长期目标主动安排时间，集中处理琐事，暂缓低价值事项。',
    scene: '事情很多，但不知道先做什么',
    source: 'https://www.todoist.com/productivity-methods/eisenhower-matrix',
  },
  {
    id: 'top3',
    name: '每日三件事',
    label: '把重点收敛到可完成',
    icon: 'target',
    text: '每天挑选最多三项最值得完成的行动，在时间允许时做完；其他任务保留，不把整个待办清单当作今天的承诺。',
    scene: '清单很长，经常忙完却没有进展',
    source: '',
  },
  {
    id: 'blocks',
    name: '时间分块',
    label: '给行动一个真实位置',
    icon: 'calendar',
    text: '把任务排进具体开始和结束时间，绕开已有安排，给任务之间留缓冲，也为临时变化保留空白。',
    scene: '知道要做什么，却总被打断',
    source: 'https://www.todoist.com/productivity-methods/time-blocking',
  },
  {
    id: 'focus',
    name: '番茄专注',
    label: '一次只推进一件事',
    icon: 'clock',
    text: '从 25 分钟专注和 5 分钟休息开始，也可以选择适合自己的时长。记录实际专注，不把计时自动当作任务完成。',
    scene: '容易拖延，或难以持续投入',
    source: 'https://www.pomodorotechnique.com/',
  },
  {
    id: 'frog',
    name: '先做关键难事',
    label: '把最重要的一步放前面',
    icon: 'flame',
    text: '在精力较好的时段先推进一件重要且容易拖延的事。任务太大时，先拆成一个可以开始的动作。',
    scene: '总把重要任务推到一天最后',
    source: 'https://www.todoist.com/productivity-methods/eat-the-frog',
  },
  {
    id: 'gtd',
    name: '收集与下一步',
    label: '让脑中的杂事有归处',
    icon: 'notebook',
    text: '先把想法收进待安排任务池，再判断需要行动吗、下一步是什么、是否关联目标；通过定期复盘整理，而非立即塞满日程。',
    scene: '事情散在脑中，担心遗漏',
    source: 'https://gettingthingsdone.com/what-is-gtd/',
  },
];
function useMethod(id: string) {
  buildingMethod.value = id;
  building.value = true;
}
</script>
<template>
  <PageHeader
    title="效率计划"
    subtitle="先决定什么值得做，再为它留出时间。让方法成为行动的一部分。"
    eyebrow="LESS NOISE. MORE MEANING."
    ><button
      class="button primary"
      @click="
        buildingMethod = tab;
        building = true;
      "
    >
      <Icon name="plus" :size="17" />新建方法计划
    </button></PageHeader
  >
  <section class="method-hero">
    <div>
      <span class="eyebrow">你的行动，有自己的章法</span>
      <h2>忙碌不是目标。<br />把力气留给真正重要的事。</h2>
      <p>分清轻重缓急 → 挑选关键行动 → 排进时间 → 专注执行 → 回头调整</p>
    </div>
    <div class="method-hero-art">
      <span class="orb orb-one">重要</span><span class="orb orb-two">紧急</span
      ><span class="orb orb-three">留白</span><i></i>
    </div>
  </section>
  <div class="method-tabs" role="tablist" aria-label="计划方法">
    <button
      v-for="item in [
        { id: 'matrix', name: '四象限' },
        { id: 'top3', name: '每日三件事' },
        { id: 'blocks', name: '时间分块' },
        { id: 'library', name: '方法指南' },
      ]"
      :key="item.id"
      role="tab"
      :aria-selected="tab === item.id"
      :class="{ active: tab === item.id }"
      @click="changeTab(item.id)"
    >
      {{ item.name }}
    </button>
  </div>
  <section v-if="methodPlans.length" class="my-method-plans">
    <div class="section-heading">
      <h2>我的方法计划</h2>
      <small>每份计划保留自己的行动方式</small>
    </div>
    <div>
      <button
        v-for="plan in methodPlans"
        :key="plan.id"
        @click="ui.open('goalDetail', { id: plan.id })"
      >
        <span>{{ planningMethods.find((m) => m.id === plan.planningMethod)?.name }}</span
        ><strong>{{ plan.title }}</strong
        ><small
          >{{ app.tasks.filter((t) => t.goalId === plan.id).length }} 项行动
          <Icon name="arrow" :size="13"
        /></small>
      </button>
    </div>
  </section>
  <template v-if="tab === 'matrix'">
    <div class="matrix-toolbar">
      <label class="catalog-search"
        ><Icon name="search" :size="16" /><input
          v-model="query"
          aria-label="搜索象限任务"
          placeholder="搜索任务…" /></label
      ><select v-model="goal" aria-label="按目标筛选">
        <option value="all">全部活跃目标与独立任务</option>
        <option
          v-for="item in app.data.goals.filter((g) => g.status === 'active')"
          :key="item.id"
          :value="item.id"
        >
          {{ item.title }}
        </option>
      </select>
      <div class="segmented">
        <button :class="{ active: scope === 'all' }" @click="scope = 'all'">全部待办</button
        ><button :class="{ active: scope === 'today' }" @click="scope = 'today'">
          今日与待安排
        </button>
      </div>
      <span class="muted">{{ tasks.length }} 项待办</span>
    </div>
    <div class="matrix-board">
      <section
        v-for="quadrant in quadrants"
        :key="quadrant.id"
        :class="['quadrant', quadrant.color, { 'drag-target': dragging }]"
        @dragover.prevent
        @drop.prevent="drop($event, quadrant.id)"
      >
        <header>
          <span class="quadrant-icon"><Icon :name="quadrant.icon" :size="22" /></span>
          <div>
            <small>{{ quadrant.action }}</small>
            <h2>{{ quadrant.title }}</h2>
          </div>
          <span class="count-badge">{{
            tasks.filter((t) => quadrantOf(t, date) === quadrant.id).length
          }}</span>
        </header>
        <p class="quadrant-hint">{{ quadrant.hint }}</p>
        <div class="quadrant-items">
          <article
            v-for="task in tasks.filter((t) => quadrantOf(t, date) === quadrant.id)"
            :key="task.id"
            class="matrix-task"
            draggable="true"
            @dragstart="drag($event, task)"
            @dragend="dragging = ''"
          >
            <div>
              <button
                class="task-check"
                :aria-label="`完成：${task.title}`"
                @click="app.toggleTask(task.id)"
              /><button class="matrix-task-title" @click="ui.open('task', { id: task.id })">
                {{ task.title }}</button
              ><RouterLink
                :to="`/focus?task=${task.id}`"
                class="icon-button"
                :aria-label="`专注：${task.title}`"
                ><Icon name="play" :size="15"
              /></RouterLink>
            </div>
            <p>
              {{ task.durationMinutes }} 分钟
              <span v-if="task.goalId"
                >· {{ app.data.goals.find((g) => g.id === task.goalId)?.title }}</span
              ><span v-if="task.dueDate" :class="{ 'deadline-overdue': task.dueDate < app.today }">
                · 截止 {{ task.dueDate.slice(5) }}</span
              >
            </p>
            <footer>
              <span
                >{{ task.scheduledDate ? `计划 ${task.scheduledDate.slice(5)}` : '待安排'
                }}<small v-if="task.urgent === undefined"> · 自动判断紧急性</small></span
              ><select
                :value="quadrant.id"
                :aria-label="`移动象限：${task.title}`"
                @change="moveTask(task.id, ($event.target as HTMLSelectElement).value as Quadrant)"
              >
                <option v-for="q in quadrants" :key="q.id" :value="q.id">{{ q.action }}</option>
              </select>
            </footer>
          </article>
          <div
            v-if="!tasks.some((t) => quadrantOf(t, date) === quadrant.id)"
            class="quadrant-empty"
          >
            <Icon name="checkCircle" :size="25" /><span>这里暂时没有待办</span>
          </div>
        </div>
      </section>
    </div>
    <div class="matrix-note">
      <Icon name="info" :size="18" />
      <p>
        安排日期与截止日期是两件事。未手动标注时，中/高优先级视为重要；截止日距今天不超过 2
        天视为紧急。可以拖动卡片或用下拉菜单重新判断。
      </p>
      <button class="button secondary" @click="changeTab('top3')">
        挑选今日重点<Icon name="arrow" :size="15" />
      </button>
    </div>
  </template>
  <template v-else-if="tab === 'top3' || tab === 'blocks'">
    <div class="day-plan-layout">
      <section class="card day-plan-settings">
        <span class="eyebrow">01 / 给今天一个边界</span>
        <h2>{{ tab === 'top3' ? '少一点，做扎实' : '为行动安排具体时间' }}</h2>
        <p>只安排能装进时间的任务，未选任务保持原样。</p>
        <div class="form-grid">
          <label>计划日期<input v-model="date" type="date" required /></label
          ><label>从几点开始<input v-model="start" type="time" required /></label
          ><label
            >可用分钟<input v-model="minutes" type="number" min="15" max="720" step="15" /></label
          ><label>任务间缓冲（分钟）<input v-model="buffer" type="number" min="0" max="30" /></label
          ><label
            >留白比例<select v-model="reserve">
              <option :value="0">不留白</option>
              <option :value="15">15% · 推荐</option>
              <option :value="25">25% · 宽松</option>
              <option :value="40">40% · 轻量</option>
            </select></label
          ><label
            >当天精力<select
              :value="app.data.profile.dailyEnergyByDate[date] || 'normal'"
              @change="
                app.data.profile.dailyEnergyByDate[date] = ($event.target as HTMLSelectElement)
                  .value as 'high' | 'normal' | 'low'
              "
            >
              <option value="high">精力充沛</option>
              <option value="normal">正常节奏</option>
              <option value="low">需要轻一点</option>
            </select></label
          >
        </div>
        <div class="plan-capacity">
          <span>实际安排上限</span><strong>{{ displayedCapacity }}<small> 分钟</small></strong>
          <p>最多 {{ limit }} 项任务 · 以日内可用时段为准</p>
        </div>
        <button class="button primary full-width" @click="generate">
          <Icon name="sparkles" :size="17" />生成可执行草案</button
        ><small class="planning-local-hint"
          >本地排程规则：参考轻重缓急与精力，不调用 AI。计划先预览，确认后才改变任务。</small
        >
      </section>
      <section class="card plan-selection">
        <div class="section-heading">
          <div>
            <span class="eyebrow">02 / 选择值得推进的事</span>
            <h2>{{ pickMode === 'auto' ? '让规则帮你收敛重点' : '你来决定今天的重点' }}</h2>
          </div>
          <span class="tag">{{ eligible.length }} 项候选</span>
        </div>
        <div class="segmented">
          <button :class="{ active: pickMode === 'auto' }" @click="pickMode = 'auto'">
            自动建议</button
          ><button :class="{ active: pickMode === 'manual' }" @click="pickMode = 'manual'">
            手动选择
          </button>
        </div>
        <p class="selection-summary">
          {{
            pickMode === 'auto'
              ? '优先重要且紧急，其次长期重要；低精力时同一象限内优先选择较轻任务。'
              : '已选 ' +
                selected.length +
                ' / ' +
                limit +
                ' 项 · 预估 ' +
                selectedMinutes +
                ' 分钟'
          }}<br /><small
            >候选包括计划日期及之前的未完成任务、待安排任务；不提前挪动未来任务。</small
          >
        </p>
        <article
          v-for="task in eligible"
          :key="task.id"
          :class="['plan-candidate', { picked: selected.includes(task.id) }]"
        >
          <button
            v-if="pickMode === 'manual'"
            class="selection-check"
            :aria-label="`选择重点：${task.title}`"
            :aria-pressed="selected.includes(task.id)"
            @click="choose(task.id)"
          >
            <Icon v-if="selected.includes(task.id)" name="check" :size="14" /></button
          ><span
            v-else
            :class="['quadrant-dot', quadrants.find((q) => q.id === quadrantOf(task, date))?.color]"
          />
          <div>
            <button class="plain-title" @click="ui.open('task', { id: task.id })">
              {{ task.title }}
            </button>
            <p>
              {{ priorityReason(task, date)
              }}<span v-if="task.energy === 'high'"> · 需要较高精力</span>
            </p>
          </div>
          <strong>{{ task.durationMinutes }}<small> 分钟</small></strong>
        </article>
        <EmptyState
          v-if="!eligible.length"
          title="给今天留一点空白"
          text="先收集一个待办，或从目标拆出一个具体行动。"
        />
      </section>
    </div>
  </template>
  <div v-else class="method-library">
    <article v-for="(method, index) in methods" :key="method.id" class="card method-guide">
      <div class="section-heading">
        <span class="method-index">0{{ index + 1 }}</span
        ><Icon :name="method.icon" :size="24" />
      </div>
      <span class="eyebrow">{{ method.label }}</span>
      <h2>{{ method.name }}</h2>
      <p>{{ method.text }}</p>
      <div class="method-scene">
        <small>适合这个时候</small><span>{{ method.scene }}</span>
      </div>
      <footer>
        <button class="button secondary" @click="useMethod(method.id)">
          试用这个方法<Icon name="arrow" :size="15" /></button
        ><a
          v-if="method.source"
          :href="method.source"
          target="_blank"
          rel="noopener noreferrer"
          class="text-button"
          >方法来源<Icon name="external" :size="13"
        /></a>
      </footer>
    </article>
  </div>
  <section v-if="latest?.title.includes('计划') && !latest.undoneAt" class="last-plan-bar">
    <div>
      <Icon name="undo" :size="18" /><span>最近一次安排：{{ latest.title }}</span>
    </div>
    <button class="text-button" @click="app.undo">撤销最近一次安排</button>
  </section>
  <Modal v-if="draft" title="确认你的行动计划" wide @close="draft = null"
    ><div class="draft-budget">
      <div>
        <small>安排 {{ draft.slots.length }} 项行动</small
        ><strong
          >{{ draft.used }}<span> / {{ draft.capacity }} 分钟</span></strong
        >
      </div>
      <div>
        <span>留白 {{ draft.reserved }} 分钟</span><small>已绕开未选任务的固定时段</small>
      </div>
    </div>
    <p class="muted">{{ draft.proposal.explanation }}</p>
    <div class="plan-timeline">
      <article v-for="(slot, index) in draft.slots" :key="slot.task.id">
        <div class="slot-time">
          <strong>{{ slot.start }}</strong
          ><span>{{ slot.end }}</span>
        </div>
        <div class="slot-line"><i /></div>
        <div class="slot-content">
          <span class="eyebrow">重点 {{ index + 1 }}</span>
          <h3>{{ slot.task.title }}</h3>
          <p>{{ slot.reason }} · {{ slot.task.durationMinutes }} 分钟</p>
        </div>
      </article>
    </div>
    <div v-if="draft.skipped.length" class="plan-skipped">
      <h3>留给下一次，不强行塞进今天</h3>
      <p v-for="item in draft.skipped" :key="item.task.id">
        <span>{{ item.task.title }}</span
        ><small>{{ item.reason }}</small>
      </p>
    </div>
    <p v-if="!draft.slots.length" class="warning-banner">
      当前没有任务能排入时间。可以调整时长、拆小任务或手动选择。
    </p>
    <div class="modal-actions">
      <button class="button secondary" @click="draft = null">继续调整</button
      ><button class="button primary" :disabled="!draft.slots.length" @click="apply">
        确认排入日程
      </button>
    </div></Modal
  >
  <MethodPlanBuilder v-if="building" :initial="buildingMethod" @close="building = false" />
</template>
