<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { useAppStore } from '../store/app';
import { addDays, localDate, weekStart, shortDate, now } from '../domain/dates';
import { rangeStats } from '../domain/selectors';
import { categories, type Category } from '../domain/types';
import PageHeader from '../components/PageHeader.vue';
import Icon from '../components/Icon.vue';
import EmptyState from '../components/EmptyState.vue';
const store = useAppStore(),
  range = ref(7),
  reviewWeek = ref(weekStart(store.today)),
  saved = ref(false);
const stats = computed(() =>
  rangeStats(store.data, addDays(store.today, 1 - range.value), store.today),
);
const points = computed(() =>
  Array.from({ length: range.value }, (_, i) => {
    const date = addDays(store.today, i + 1 - range.value);
    return { date, count: rangeStats(store.data, date, date).completed };
  }),
);
const maximum = computed(() => Math.max(1, ...points.value.map((p) => p.count)));
const categoryStats = computed(() =>
  (Object.keys(categories) as Category[])
    .map((category) => {
      const goals = store.data.goals.filter((g) => g.category === category).map((g) => g.id);
      const tasks = store.tasks.filter(
        (t) =>
          t.goalId &&
          goals.includes(t.goalId) &&
          t.scheduledDate &&
          t.scheduledDate >= addDays(store.today, 1 - range.value) &&
          t.scheduledDate <= store.today,
      );
      return {
        category,
        label: categories[category],
        done: tasks.filter((t) => t.status === 'done').length,
        total: tasks.length,
      };
    })
    .filter((c) => c.total > 0),
);
const review = reactive({ wins: '', blockers: '', changes: '' });
watch(
  [reviewWeek, () => store.data.weeklyReviews],
  () => {
    const value = store.data.weeklyReviews.find((r) => r.weekStartDate === reviewWeek.value);
    Object.assign(review, {
      wins: value?.wins || '',
      blockers: value?.blockers || '',
      changes: value?.changes || '',
    });
    saved.value = false;
  },
  { immediate: true },
);
function save() {
  const value = { weekStartDate: reviewWeek.value, ...review, updatedAt: now() };
  const index = store.data.weeklyReviews.findIndex((r) => r.weekStartDate === reviewWeek.value);
  if (index >= 0) store.data.weeklyReviews[index] = value;
  else store.data.weeklyReviews.push(value);
  saved.value = true;
  store.toast('这一周的想法已保存');
}
</script>
<template>
  <PageHeader
    title="成长复盘"
    subtitle="回头看看，原来你已经走了这么远。用真实记录找到下一步。"
    eyebrow="EVERY STEP LEAVES A TRACE"
    ><div class="segmented">
      <button
        v-for="days in [7, 30]"
        :key="days"
        :class="{ active: range === days }"
        @click="range = days"
      >
        最近 {{ days }} 天
      </button>
    </div></PageHeader
  >
  <div class="stats-grid four">
    <div class="stat-card">
      <span>已完成任务</span><strong>{{ stats.completed }}<small>项</small></strong
      ><small>按实际完成日期计算</small>
    </div>
    <div class="stat-card">
      <span>习惯打卡</span><strong>{{ stats.habitCount }}<small>次</small></strong
      ><small>那些愿意坚持的小事</small>
    </div>
    <div class="stat-card">
      <span>推进目标</span><strong>{{ stats.goals }}<small>个</small></strong
      ><small>有实际完成行动的目标</small>
    </div>
    <div class="stat-card">
      <span>区间净获得 XP</span><strong>{{ stats.xp }}</strong
      ><small>累计 {{ store.xp }} XP · Lv. {{ store.level }}</small>
    </div>
  </div>
  <div class="insights-grid">
    <section class="card trend-card">
      <div class="section-heading">
        <h2>行动留下的足迹</h2>
        <span class="subtle-label">每日完成任务</span>
      </div>
      <div
        v-if="stats.completed"
        class="bar-chart"
        role="img"
        :aria-label="points.map((p) => `${p.date}: ${p.count} 项完成`).join('；')"
      >
        <div class="chart-grid"><span v-for="n in 4" :key="n" /></div>
        <div v-for="(point, index) in points" :key="point.date" class="bar-column">
          <span class="bar-count">{{ point.count || '' }}</span>
          <div
            class="bar"
            :class="{ today: point.date === store.today }"
            :style="{ height: `${(point.count / maximum) * 155}px` }"
            :title="`${point.date} 完成 ${point.count} 项`"
          />
          <small v-if="range === 7 || index % 5 === 0 || index === range - 1">{{
            shortDate(point.date)
          }}</small
          ><small v-else class="invisible">·</small>
        </div>
      </div>
      <EmptyState v-else title="足迹从第一次行动开始" text="这个时间段还没有已完成任务记录。" />
      <div class="chart-caption">
        <span
          >计划到期任务完成率
          <strong>{{ stats.rate === null ? '—' : `${stats.rate}%` }}</strong></span
        ><small>分母：区间内已安排且未取消、未删除的任务 {{ stats.planned }} 项</small>
      </div>
    </section>
    <section class="card">
      <div class="section-heading">
        <h2>方向与进展</h2>
        <Icon name="target" :size="18" />
      </div>
      <div v-for="category in categoryStats" :key="category.category" class="category-stat">
        <div>
          <span :class="['category-label', category.category]">{{ category.label }}</span
          ><strong>{{ category.done }}/{{ category.total }}</strong>
        </div>
        <div class="progress-track large">
          <span :style="{ width: `${(category.done / category.total) * 100}%` }" />
        </div>
      </div>
      <EmptyState
        v-if="!categoryStats.length"
        title="暂无分类进展"
        text="给任务关联目标，就能看见每个方向的积累。"
        icon="target"
      />
      <p class="muted small-text">按区间内计划到期的关联任务计算。</p>
    </section>
  </div>
  <section class="card review-card">
    <div class="section-heading">
      <div>
        <h2>写给这一周的自己</h2>
        <p class="muted">把经历变成经验，让下周更从容。</p>
      </div>
      <div class="week-control">
        <button
          class="icon-button"
          aria-label="上一周复盘"
          @click="reviewWeek = addDays(reviewWeek, -7)"
        >
          <Icon name="left" :size="17" /></button
        ><strong>{{ shortDate(reviewWeek) }} 这一周</strong
        ><button
          class="icon-button"
          aria-label="下一周复盘"
          @click="reviewWeek = addDays(reviewWeek, 7)"
        >
          <Icon name="right" :size="17" />
        </button>
      </div>
    </div>
    <form @submit.prevent="save">
      <div class="review-fields">
        <label
          >这周做得不错的事<textarea
            v-model="review.wins"
            rows="4"
            maxlength="5000"
            placeholder="哪怕只是一件小事，也值得记下来。"
            @input="saved = false"
          /></label
        ><label
          >遇到的阻碍<textarea
            v-model="review.blockers"
            rows="4"
            maxlength="5000"
            placeholder="什么让计划变得困难？"
            @input="saved = false"
          /></label
        ><label
          >下周准备怎么调整<textarea
            v-model="review.changes"
            rows="4"
            maxlength="5000"
            placeholder="选一个你愿意尝试的小改变。"
            @input="saved = false"
          />
        </label>
      </div>
      <div class="review-actions">
        <span class="muted">{{ saved ? '✓ 已保存在本地' : '每周一条记录，随时可以回来修改' }}</span
        ><RouterLink to="/assistant?mode=review" class="button secondary"
          ><Icon name="sparkles" :size="16" />看看本周建议</RouterLink
        ><button class="button primary" type="submit">保存复盘</button>
      </div>
    </form>
  </section>
</template>
