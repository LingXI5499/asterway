<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { useAppStore } from '../store/app';
import { ui } from '../store/ui';
import { addDays, weekStart, weekDays, shortDate, dayLabel, now, validDate } from '../domain/dates';
import { sortTasks, conflictingIds } from '../domain/selectors';
import PageHeader from '../components/PageHeader.vue';
import TaskRow from '../components/TaskRow.vue';
import Icon from '../components/Icon.vue';
import EmptyState from '../components/EmptyState.vue';
const store = useAppStore(),
  current = ref(weekStart(store.today)),
  view = ref('week'),
  poolFilter = ref('all');
const route = useRoute();
watch(
  () => route.query.date,
  (value) => {
    if (typeof value === 'string' && validDate(value)) current.value = weekStart(value);
  },
  { immediate: true },
);
const days = computed(() => weekDays(current.value)),
  conflicts = computed(() => conflictingIds(store.tasks));
const pool = computed(() =>
  store.tasks.filter(
    (t) =>
      !t.scheduledDate &&
      t.status === 'todo' &&
      (poolFilter.value !== 'high' || t.priority === 'high'),
  ),
);
const overdue = computed(() =>
  store.tasks.filter(
    (t) => t.scheduledDate && t.scheduledDate < store.today && t.status === 'todo',
  ),
);
const dayTasks = (date: string) => sortTasks(store.tasks.filter((t) => t.scheduledDate === date));
function move(id: string, date: string) {
  const task = store.data.tasks.find((t) => t.id === id)!;
  task.scheduledDate = date;
  delete task.startTime;
  task.updatedAt = now();
  store.toast('任务已重新安排');
}
</script>
<template>
  <PageHeader
    title="计划日程"
    subtitle="给重要的事留出位置，也给生活留一点空白。"
    eyebrow="MAKE ROOM FOR WHAT MATTERS"
    ><RouterLink class="button secondary" to="/methods?tab=blocks"
      ><Icon name="sparkles" :size="17" />按方法安排</RouterLink
    ><button class="button primary" @click="ui.open('task')">
      <Icon name="plus" :size="17" />新建任务
    </button></PageHeader
  >
  <div class="planner-toolbar">
    <div class="week-control">
      <button class="icon-button" aria-label="上一周" @click="current = addDays(current, -7)">
        <Icon name="left" /></button
      ><strong>{{ shortDate(days[0]!) }} — {{ shortDate(days[6]!) }}</strong
      ><button class="icon-button" aria-label="下一周" @click="current = addDays(current, 7)">
        <Icon name="right" /></button
      ><button class="chip" @click="current = weekStart(store.today)">本周</button>
    </div>
    <div class="segmented">
      <button :class="{ active: view === 'week' }" @click="view = 'week'">周视图</button
      ><button :class="{ active: view === 'list' }" @click="view = 'list'">列表</button>
    </div>
  </div>
  <div v-if="conflicts.size" class="warning-banner">
    <Icon name="clock" :size="17" />有
    {{ conflicts.size }} 项任务的时段重叠。点击任务调整，留出舒适的节奏。
  </div>
  <div class="planner-layout">
    <aside class="card task-pool">
      <div class="section-heading">
        <h3>待安排任务</h3>
        <span class="count-badge">{{ pool.length }}</span>
      </div>
      <select v-model="poolFilter" aria-label="任务池筛选">
        <option value="all">全部优先级</option>
        <option value="high">高优先级</option>
      </select>
      <div v-for="task in pool" :key="task.id" class="pool-item">
        <TaskRow :task="task" compact /><button
          class="text-button"
          @click="ui.open('task', { id: task.id })"
        >
          安排日期<Icon name="arrow" :size="14" />
        </button>
      </div>
      <p v-if="!pool.length" class="muted">暂时没有待安排的行动。</p>
      <button class="add-task-row" @click="ui.open('task', { date: '' })">
        <Icon name="plus" :size="16" />添加待安排任务
      </button>
      <div v-if="overdue.length" class="overdue-section">
        <h3>
          需要重新安排 <small>{{ overdue.length }} 项</small>
        </h3>
        <div v-for="task in overdue" :key="task.id" class="overdue-item">
          <button class="plain-title" @click="ui.open('task', { id: task.id })">
            {{ task.title }}
          </button>
          <div>
            <button class="text-button" @click="move(task.id, store.today)">移到今天</button
            ><button class="text-button" @click="move(task.id, addDays(task.scheduledDate!, 1))">
              延后一天
            </button>
          </div>
        </div>
      </div>
    </aside>
    <section :class="['week-board', { 'list-view': view === 'list' }]">
      <div
        v-for="date in days"
        :key="date"
        :class="['day-column', { 'is-today': date === store.today }]"
      >
        <header>
          <span>{{ dayLabel(date) }}</span
          ><strong>{{ date.slice(-2) }}<small v-if="date === store.today">今天</small></strong
          ><span>{{ dayTasks(date).length }} 项</span>
        </header>
        <div class="day-tasks">
          <TaskRow
            v-for="task in dayTasks(date)"
            :key="task.id"
            :task="task"
            compact
            :conflict="conflicts.has(task.id)"
          />
          <p v-if="!dayTasks(date).length" class="day-empty">留一点空白</p>
        </div>
        <button
          class="day-add"
          :aria-label="`为 ${date} 添加任务`"
          @click="ui.open('task', { date })"
        >
          <Icon name="plus" :size="16" />
        </button>
      </div>
    </section>
  </div>
  <div class="gentle-note">
    <Icon name="calendar" :size="18" />
    <p>这里展示计划时长，不代表实际专注。过期行动会保留，直到你主动调整。</p>
  </div>
</template>
