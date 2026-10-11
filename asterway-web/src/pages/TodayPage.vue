<script setup lang="ts">
import { computed, ref } from 'vue';
import { useAppStore } from '../store/app';
import { ui } from '../store/ui';
import { weekStart, parseDate, shortDate } from '../domain/dates';
import { priorityRank } from '../domain/types';
import { sortTasks, rangeStats, streak } from '../domain/selectors';
import Icon from '../components/Icon.vue';
import TaskRow from '../components/TaskRow.vue';
import GoalCard from '../components/GoalCard.vue';
import EmptyState from '../components/EmptyState.vue';
const store = useAppStore(),
  mode = ref('list'),
  showDone = ref(true);
const tasks = computed(() => sortTasks(store.tasks.filter((t) => t.scheduledDate === store.today)));
const done = computed(() => tasks.value.filter((t) => t.status === 'done'));
const todo = computed(() => tasks.value.filter((t) => t.status === 'todo'));
const focus = computed(() =>
  [...todo.value]
    .sort(
      (a, b) =>
        Number(b.isTodayFocus) - Number(a.isTodayFocus) ||
        priorityRank[a.priority] - priorityRank[b.priority] ||
        (a.startTime || '99').localeCompare(b.startTime || '99'),
    )
    .slice(0, 3),
);
const habits = computed(() =>
  store.data.habits.filter((h) => h.status === 'active' && h.startDate <= store.today),
);
const checked = computed(
  () =>
    habits.value.filter((h) =>
      store.data.habitLogs.some((l) => l.habitId === h.id && l.date === store.today),
    ).length,
);
const longest = computed(() =>
  Math.max(
    0,
    ...store.data.habits
      .filter((h) => h.status === 'active')
      .map((h) => streak(store.data, h, store.today)),
  ),
);
const weekly = computed(() => rangeStats(store.data, weekStart(store.today), store.today));
const goals = computed(() => store.data.goals.filter((g) => g.status === 'active').slice(0, 3));
const milestones = computed(() =>
  store.data.milestones.filter((m) => goals.value.some((g) => g.id === m.goalId)).slice(0, 2),
);
const suggestion = computed(() =>
  todo.value.length > 2
    ? `今天还有 ${todo.value.length} 项行动，预估 ${todo.value.reduce((s, t) => s + t.durationMinutes, 0)} 分钟。给变化留一点空间，先做好最重要的一件事。`
    : todo.value.length
      ? '今天的节奏很轻盈。选择一个重要的小行动，按自己的节奏开始。'
      : '今天的行动已完成，给自己一点休息和回顾的时间吧。',
);
const greeting =
  new Date().getHours() < 12 ? '早上好' : new Date().getHours() < 18 ? '下午好' : '晚上好';
</script>
<template>
  <div v-if="!store.data.profile.onboardingDismissed" class="onboarding">
    <Icon name="sparkles" :size="17" /><span
      >欢迎来到星行纪。这是演示数据，可以随意探索，设置中支持重置或清空。</span
    ><button class="text-button" @click="store.data.profile.onboardingDismissed = true">
      开始体验<Icon name="arrow" :size="14" />
    </button>
  </div>
  <header class="page-header today-header">
    <div>
      <div class="eyebrow">YOUR DAILY COMPASS</div>
      <h1>
        {{ greeting }}，{{ store.data.profile.displayName }}<span class="greeting-spark">✳</span>
      </h1>
      <p>成长不必急于求成，今天也从重要的事开始。</p>
    </div>
    <label class="energy-picker"
      ><span>今天的精力</span
      ><select
        :value="store.data.profile.dailyEnergyByDate[store.today] || 'normal'"
        aria-label="今日精力"
        @change="
          store.data.profile.dailyEnergyByDate[store.today] = ($event.target as HTMLSelectElement)
            .value as 'high' | 'normal' | 'low'
        "
      >
        <option value="high">☀ 充沛</option>
        <option value="normal">◐ 一般</option>
        <option value="low">☁ 疲惫</option>
      </select></label
    >
  </header>
  <div class="today-grid">
    <div class="today-main">
      <section class="stats-grid four">
        <div class="stat-card">
          <span>今日完成</span
          ><strong
            >{{ done.length }}<small>/ {{ tasks.length }}</small></strong
          ><small
            ><span class="positive"
              >{{ tasks.length ? Math.round((done.length / tasks.length) * 100) : '—'
              }}{{ tasks.length ? '%' : '' }}</span
            >
            今日任务完成率</small
          >
        </div>
        <div class="stat-card">
          <span>本周行动</span><strong>{{ weekly.completed }}<small>项</small></strong
          ><small>每一个行动，都让方向更清晰</small>
        </div>
        <div class="stat-card">
          <span>连续打卡</span
          ><strong
            >{{ longest }}<small>天</small><Icon name="flame" :size="19" class="amber" /></strong
          ><small>每日习惯的当前最长连续</small>
        </div>
        <div class="stat-card">
          <span>今日习惯</span
          ><strong
            >{{ checked }}<small>/ {{ habits.length }}</small></strong
          ><small>用小习惯，积累大改变</small>
        </div>
      </section>
      <section class="focus-section">
        <div class="section-heading">
          <h2>
            <span class="heading-icon"><Icon name="target" :size="19" /></span>今天，先做好这
            {{ focus.length || 3 }} 件事
          </h2>
          <span class="subtle-label">少一点，更专注</span>
        </div>
        <div class="focus-cards">
          <button
            v-for="(task, index) in focus"
            :key="task.id"
            class="focus-card"
            @click="ui.open('task', { id: task.id })"
          >
            <span class="focus-number">0{{ index + 1 }}</span
            ><span :class="['priority-dot', task.priority]" />
            <h3>{{ task.title }}</h3>
            <div>
              <span><Icon name="clock" :size="14" />{{ task.durationMinutes }} 分钟</span
              ><span class="circle-arrow"><Icon name="arrow" :size="17" /></span>
            </div></button
          ><EmptyState
            v-if="!focus.length"
            title="给今天选一个小行动"
            text="你可以创建任务，或从目标拆解行动。"
            ><button class="button secondary" @click="ui.open('task')">创建任务</button></EmptyState
          >
        </div>
      </section>
      <section class="card tasks-card">
        <div class="section-heading">
          <h2>
            今日行动清单<span class="count-badge">{{ tasks.length }}</span>
          </h2>
          <div class="segmented small">
            <button :class="{ active: mode === 'list' }" @click="mode = 'list'">清单</button
            ><button :class="{ active: mode === 'timeline' }" @click="mode = 'timeline'">
              时间线
            </button>
          </div>
        </div>
        <div :class="{ 'timeline-list': mode === 'timeline' }">
          <TaskRow v-for="task in todo" :key="task.id" :task="task" /><button
            v-if="done.length"
            class="completed-toggle"
            :aria-expanded="showDone"
            @click="showDone = !showDone"
          >
            <Icon name="checkCircle" :size="15" />已完成 {{ done.length }} 项<Icon
              :name="showDone ? 'up' : 'down'"
              :size="14"
            /></button
          ><template v-if="showDone"
            ><TaskRow v-for="task in done" :key="task.id" :task="task"
          /></template>
        </div>
        <EmptyState
          v-if="!tasks.length"
          title="今天还是一张白纸"
          text="把一个目标变成今天能做的事。"
        /><button class="add-task-row" @click="ui.open('task', { date: store.today })">
          <Icon name="plus" :size="17" />添加一个行动
        </button>
      </section>
      <section>
        <div class="section-heading">
          <h2>正在走的路</h2>
          <RouterLink to="/goals" class="text-button"
            >全部目标<Icon name="arrow" :size="15"
          /></RouterLink>
        </div>
        <div class="today-goals">
          <GoalCard v-for="goal in goals" :key="goal.id" :goal="goal" compact /><EmptyState
            v-if="!goals.length"
            title="为自己定一个方向"
            ><button class="button secondary" @click="ui.open('goal')">新建目标</button></EmptyState
          >
        </div>
      </section>
    </div>
    <aside class="today-aside">
      <section class="growth-card">
        <div class="section-heading">
          <h3>我的成长档案</h3>
          <Icon name="sparkles" :size="17" />
        </div>
        <div class="level-orbit">
          <svg viewBox="0 0 140 140" aria-hidden="true">
            <circle
              cx="70"
              cy="70"
              r="60"
              fill="none"
              stroke="rgba(255,255,255,.15)"
              stroke-width="7"
            />
            <circle
              cx="70"
              cy="70"
              r="60"
              fill="none"
              stroke="#e8be74"
              stroke-width="7"
              stroke-linecap="round"
              :stroke-dasharray="`${((store.xp % 100) / 100) * 377} 377`"
              transform="rotate(-90 70 70)"
            />
          </svg>
          <div>
            <small>LEVEL</small><strong>{{ String(store.level).padStart(2, '0') }}</strong
            ><span>成长探索者</span>
          </div>
        </div>
        <div class="xp-caption">
          <strong>{{ store.xp % 100 }}<span> / 100 XP</span></strong
          ><small>累计 {{ store.xp }} XP</small>
        </div>
        <p>每个认真生活的瞬间，都值得被记录。</p>
        <RouterLink to="/insights">查看成长足迹<Icon name="arrow" :size="15" /></RouterLink>
      </section>
      <section class="card habit-mini">
        <div class="section-heading">
          <h3>小习惯，大改变</h3>
          <RouterLink to="/habits" aria-label="查看全部习惯"
            ><Icon name="external" :size="16"
          /></RouterLink>
        </div>
        <button
          v-for="habit in habits"
          :key="habit.id"
          :class="[
            'mini-habit',
            {
              checked: store.data.habitLogs.some(
                (l) => l.habitId === habit.id && l.date === store.today,
              ),
            },
          ]"
          :aria-pressed="
            store.data.habitLogs.some((l) => l.habitId === habit.id && l.date === store.today)
          "
          @click="store.toggleHabit(habit.id, store.today)"
        >
          <span :class="['category-icon', habit.category]"
            ><Icon :name="habit.category" :size="17" /></span
          ><span>{{ habit.title }}</span
          ><span class="mini-check"><Icon name="check" :size="13" /></span>
        </button>
        <p v-if="!habits.length" class="muted">添加一个你愿意坚持的小习惯。</p>
      </section>
      <section class="assistant-hint">
        <div class="section-heading">
          <h3><Icon name="sparkles" :size="18" />来自成长伙伴</h3>
          <span class="demo-badge">本地演示</span>
        </div>
        <p>{{ suggestion }}</p>
        <RouterLink to="/assistant?mode=time" class="text-button"
          >帮我调整今天<Icon name="arrow" :size="15"
        /></RouterLink>
      </section>
      <section class="milestone-preview">
        <div class="section-heading">
          <h3>下一段旅程</h3>
          <Icon name="map" :size="17" />
        </div>
        <button
          v-for="milestone in milestones"
          :key="milestone.id"
          @click="ui.open('goalDetail', { id: milestone.goalId })"
        >
          <span class="milestone-pin" /><span
            ><strong>{{ milestone.title }}</strong
            ><small>{{
              store.data.goals.find((g) => g.id === milestone.goalId)?.title
            }}</small></span
          >
        </button>
        <p v-if="!milestones.length" class="muted">在目标中添加你的第一段里程碑。</p>
      </section>
      <button class="focus-start" @click="ui.open('focus')">
        <Icon name="play" :size="17" /><span>开启一段专注</span><small>25 MIN</small>
      </button>
    </aside>
  </div>
  <footer class="page-footer">
    <span>A little progress, every day.</span
    ><span
      >{{ shortDate(store.today) }} ·
      {{ parseDate(store.today).toLocaleDateString('zh-CN', { weekday: 'long' }) }}</span
    >
  </footer>
</template>
