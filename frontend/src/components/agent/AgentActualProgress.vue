<script setup>
import { computed } from "vue";
const props = defineProps({ tracking: Object });
const kinds = ['goals', 'stages', 'tasks', 'weeks', 'days', 'habits'];
const labels = { goals: '目标', stages: '阶段', tasks: '任务', weeks: '周计划', days: '日计划', habits: '习惯' };
const states = { NOT_STARTED: '未开始', IN_PROGRESS: '进行中', COMPLETED: '已完成', ACTIVE: '进行中', PAUSED: '已暂停', ENDED: '已结束' };
const completed = computed(() => props.tracking?.groups?.tasks?.filter(row => row.status === 'COMPLETED').length || 0);
const total = computed(() => props.tracking?.groups?.tasks?.length || 0);
const title = row => row.name || row.title || (row.kind === 'weeks' ? `${row.weekStartDate} 起的一周` : row.kind === 'days' ? row.planDate : '跟踪对象');
const link = row => ({ goals: `/growth/goals/${row.id}`, stages: `/growth/stages/${row.id}`, tasks: `/growth/tasks/${row.id}`, weeks: `/planning/weekly-plans?week=${row.weekStartDate}`, days: `/planning/daily-plans?date=${row.planDate}`, habits: '/habits' })[row.kind];
const habitStatus = row => !row.todayKnown ? '今日状态暂未核对' : !row.dueToday ? '今日无需执行' : row.checkedToday ? '今日已打卡' : '今日待完成';
</script>

<template>
  <section v-if="tracking?.ready" class="actual-progress" aria-label="实际执行进度">
    <div class="progress-heading"><h3>实际执行进度</h3><small>{{ tracking.asOf }}</small></div>
    <p v-if="total">跟踪任务已完成 {{ completed }} / {{ total }} 项<span v-if="tracking.partial">（已加载范围）</span></p>
    <p v-else>进度按各类实际记录展示，方案提交成功不代表任务已完成。</p>
    <p v-for="signal in tracking.signals || []" :key="signal.code" class="progress-signal"><AppIcon name="info" :size="14" />{{ signal.message }}</p>
    <template v-for="kind in kinds" :key="kind">
      <details v-if="tracking.groups?.[kind]?.length" :open="kind === 'goals' || kind === 'tasks'" class="progress-group">
        <summary>{{ labels[kind] }}<small>{{ tracking.groups[kind].length }} 项</small></summary>
        <article v-for="row in tracking.groups[kind]" :key="row.id">
          <RouterLink :to="link(row)">{{ title(row) }}<AppIcon name="arrow" :size="12" /></RouterLink>
          <div class="progress-state"><span>{{ kind === 'habits' ? habitStatus(row) : states[row.status] || '按实际记录跟踪' }}</span><strong v-if="['goals', 'stages'].includes(kind) && row.progress != null">{{ row.progress }}%</strong><span v-if="row.overdue" class="overdue">需调整安排</span></div>
          <el-progress v-if="['goals', 'stages'].includes(kind) && row.progress != null" :percentage="Math.max(0, Math.min(100, Number(row.progress)))" :show-text="false" :stroke-width="4" />
          <small v-if="row.taskTotal != null">已完成 {{ row.taskCompleted }} / {{ row.taskTotal }} 项安排<span v-if="row.partial">，已加载 {{ row.taskReturned }} 项</span></small>
          <small v-if="kind === 'tasks' && (row.dueDate || row.expectedEndDate)">截止 {{ row.dueDate || row.expectedEndDate }}</small>
        </article>
      </details>
    </template>
  </section>
</template>

<style scoped>
.actual-progress { margin: 24px 0; }
.progress-heading, .progress-state { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
h3 { font-size: 16px; line-height: 24px; margin: 0; }
p, summary, a { font-size: 14px; line-height: 20px; }
p, small, .progress-state { color: var(--aw-text-secondary); }
p { margin: 8px 0 16px; }
small, .progress-state { font-size: 12px; line-height: 16px; }
.progress-signal { display: flex; gap: 8px; align-items: flex-start; padding: 12px; border: 1px solid var(--aw-border); border-radius: 8px; background: var(--aw-bg); }
.progress-signal :deep(svg) { flex-shrink: 0; margin-top: 2px; }
.progress-group { margin-top: 16px; }
.progress-group summary { cursor: pointer; color: var(--aw-text); }
.progress-group summary small { margin-left: 8px; }
article { margin-top: 16px; }
article a { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.progress-state { margin: 8px 0; }
article > small { display: block; margin-top: 4px; }
.overdue { color: var(--aw-brand); }
</style>
