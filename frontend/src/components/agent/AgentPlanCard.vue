<script setup>
import { computed } from "vue";
import { priorities } from "../../api/growth";
const props = defineProps({ plan: { type: Object, required: true } });
const snapshot = computed(() => props.plan.snapshot || {});
const stages = computed(() => (snapshot.value.stages || []).map(stage => ({ ...stage,
  children: (snapshot.value.tasks || []).filter(task => task.stageKey && task.stageKey === stage.clientKey) })));
const looseTasks = computed(() => (snapshot.value.tasks || []).filter(task => !stages.value.some(stage => stage.children.includes(task))));
const schedules = computed(() => snapshot.value.taskAssignments || []);
const taskNames = computed(() => Object.fromEntries((snapshot.value.tasks || []).map(task => [task.clientKey, task.title])));
const weekday = value => ({ 1: '周一', 2: '周二', 3: '周三', 4: '周四', 5: '周五', 6: '周六', 7: '周日' })[value];
function dates(item) {
  if (item.startDate && item.dueDate) return item.startDate + ' 至 ' + item.dueDate;
  return item.startDate ? item.startDate + ' 开始' : item.dueDate ? item.dueDate + ' 截止' : '日期待安排';
}
function assignedDate(task) {
  return schedules.value.filter(item => item.taskKey === task.clientKey && item.businessDate).map(item => item.businessDate).join('、');
}
</script>

<template>
  <article class="plan-card">
    <header v-if="snapshot.goal" class="plan-goal"><span class="plan-section-label">目标</span><h3>{{ snapshot.goal.title }}</h3><p v-if="snapshot.goal.description">{{ snapshot.goal.description }}</p><div class="plan-date"><AppIcon name="day" :size="14" />{{ dates(snapshot.goal) }}</div><p v-if="snapshot.goal.completionCriteria" class="plan-criteria"><strong>完成标准</strong>{{ snapshot.goal.completionCriteria }}</p></header>
    <header v-else class="plan-goal"><h3>{{ plan.summary }}</h3></header>
    <div v-if="snapshot.stages?.length || snapshot.tasks?.length" class="plan-counts"><span>{{ snapshot.stages?.length || 0 }} 个阶段</span><span>{{ snapshot.tasks?.length || 0 }} 个任务</span><span v-if="snapshot.dailyPlans?.length">{{ snapshot.dailyPlans.length }} 天安排</span></div>
    <section v-for="(stage, index) in stages" :key="stage.clientKey" class="roadmap-stage">
      <div class="stage-heading"><span class="stage-number">{{ index + 1 }}</span><div><h4>{{ stage.title }}</h4><small>{{ dates(stage) }}</small></div></div>
      <div class="stage-content"><p v-if="stage.description">{{ stage.description }}</p><p v-if="stage.completionCriteria" class="stage-criteria">完成标准：{{ stage.completionCriteria }}</p><RouterLink v-if="stage.goalId" class="existing-reference" :to="`/growth/goals/${stage.goalId}`">查看所属目标</RouterLink>
        <ol v-if="stage.children.length" class="roadmap-tasks"><li v-for="task in stage.children" :key="task.clientKey"><span class="task-bullet"></span><div><strong>{{ task.title }}</strong><p v-if="task.description">{{ task.description }}</p><p v-if="task.completionCriteria">完成标准：{{ task.completionCriteria }}</p><div class="task-meta"><span v-if="task.estimatedMinutes">预计 {{ task.estimatedMinutes }} 分钟</span><span v-if="task.priority">{{ priorities[task.priority] }}优先级</span><span v-if="task.weight && task.weight !== 1">权重 {{ task.weight }}</span></div><small v-if="task.startDate || task.dueDate">{{ dates(task) }}</small><small v-if="assignedDate(task)" class="task-arrangement">安排在 {{ assignedDate(task) }}</small></div></li></ol>
      </div>
    </section>
    <section v-if="looseTasks.length" class="plan-section"><h4>任务</h4><ol class="loose-tasks"><li v-for="task in looseTasks" :key="task.clientKey"><strong>{{ task.title }}</strong><p v-if="task.description">{{ task.description }}</p><p v-if="task.completionCriteria">完成标准：{{ task.completionCriteria }}</p><small>{{ dates(task) }}<span v-if="task.estimatedMinutes"> · {{ task.estimatedMinutes }} 分钟</span><span v-if="task.priority"> · {{ priorities[task.priority] }}优先级</span></small><RouterLink v-if="task.stageId" class="existing-reference" :to="`/growth/stages/${task.stageId}`">查看所属阶段</RouterLink></li></ol></section>
    <section v-if="snapshot.weeklyPlans?.length" class="plan-section"><h4>周计划</h4><div v-for="week in snapshot.weeklyPlans" :key="week.clientKey" class="schedule-week"><span>{{ week.weekStart }} 起的一周</span><ul v-if="snapshot.dailyPlans?.some(day => day.weekKey === week.clientKey)"><li v-for="day in snapshot.dailyPlans.filter(item => item.weekKey === week.clientKey)" :key="day.clientKey"><AppIcon name="day" :size="14" /><span>{{ day.businessDate }}</span></li></ul></div></section>
    <section v-if="schedules.length" class="plan-section"><h4>任务安排</h4><ul class="schedule-list"><li v-for="item in schedules" :key="item.clientKey"><div><strong v-if="item.taskKey">{{ taskNames[item.taskKey] || '新任务' }}</strong><RouterLink v-else :to="`/growth/tasks/${item.taskId}`">{{ item.taskTitle || '查看已选任务' }}<AppIcon name="arrow" :size="12" /></RouterLink><small v-if="item.taskId">原安排：{{ item.previousBusinessDate || (item.previousWeekStart ? item.previousWeekStart + ' 起的一周内' : '任务池') }}</small><small>{{ item.taskId ? '调整为：' : '' }}{{ item.businessDate || (snapshot.weeklyPlans?.find(week => week.clientKey === item.weekKey)?.weekStart + ' 起的一周内') }}</small><small v-if="item.reason">原因：{{ item.reason }}</small></div></li></ul></section>
    <section v-if="snapshot.habits?.length" class="plan-section"><h4>习惯</h4><article v-for="habit in snapshot.habits" :key="habit.clientKey" class="plan-habit"><strong>{{ habit.name }}</strong><p v-if="habit.description">{{ habit.description }}</p><p v-if="habit.completionCriteria">完成标准：{{ habit.completionCriteria }}</p><small>{{ habit.cycleType === 'DAILY' ? '每天' : habit.weekdays?.map(weekday).join('、') }} {{ habit.expectedTime || '' }} · {{ habit.startDate }} 开始</small><small v-if="habit.categoryName">分类：{{ habit.categoryName }}</small></article></section>
    <p v-if="plan.diffSummary" class="revision-note"><AppIcon name="history" :size="14" />已按补充要求更新，接受时以这一版为准。</p>
  </article>
</template>

<style scoped>
.plan-card { min-width: 0; color: var(--aw-text); }
h3 { margin: 8px 0 12px; font-size: 20px; line-height: 28px; font-weight: 600; overflow-wrap: anywhere; }
h4 { margin: 0 0 8px; font-size: 14px; line-height: 20px; font-weight: 600; }
p { margin: 8px 0; font-size: 14px; line-height: 20px; color: var(--aw-text-secondary); white-space: pre-wrap; overflow-wrap: anywhere; }
small { display: block; color: var(--aw-text-secondary); font-size: 12px; line-height: 16px; }
.plan-section-label { color: var(--aw-text-secondary); font-size: 12px; line-height: 16px; }
.plan-date { display: flex; align-items: center; gap: 8px; color: var(--aw-text-secondary); font-size: 12px; line-height: 16px; margin: 16px 0; }
.plan-criteria { padding: 12px; background: var(--aw-bg); border-radius: 8px; font-size: 12px; line-height: 16px; }
.plan-criteria strong { display: block; color: var(--aw-text); margin-bottom: 8px; }
.plan-counts { display: flex; gap: 16px; flex-wrap: wrap; margin: 24px 0; color: var(--aw-text-secondary); font-size: 12px; line-height: 16px; }
.roadmap-stage { position: relative; margin-top: 24px; }
.stage-heading { display: flex; gap: 8px; align-items: flex-start; }
.stage-heading h4 { margin-bottom: 4px; }
.stage-number { display: grid; place-items: center; flex-shrink: 0; width: 24px; height: 24px; border-radius: 50%; background: var(--aw-brand-soft); color: var(--aw-brand); font-size: 12px; line-height: 16px; }
.stage-content { margin-left: 32px; padding-bottom: 8px; }
.stage-criteria { font-size: 12px; line-height: 16px; }
.roadmap-tasks { list-style: none; margin: 16px 0 0; padding: 0; }
.roadmap-tasks > li { display: flex; gap: 12px; margin: 16px 0; }
.roadmap-tasks strong, .loose-tasks strong, .plan-habit strong { font-size: 14px; line-height: 20px; font-weight: 500; }
.roadmap-tasks p { font-size: 12px; line-height: 16px; margin: 4px 0; }
.task-bullet { flex-shrink: 0; width: 8px; height: 8px; margin-top: 8px; border: 1px solid var(--aw-border-strong); border-radius: 50%; }
.task-meta { display: flex; flex-wrap: wrap; gap: 8px; margin: 8px 0; font-size: 12px; line-height: 16px; color: var(--aw-text-tertiary); }
.task-arrangement { margin-top: 4px; color: var(--aw-brand); }
.plan-section { margin-top: 24px; padding-top: 16px; border-top: 1px solid var(--aw-border); }
.loose-tasks { padding-left: 16px; margin: 0; }
.loose-tasks li { margin: 16px 0; font-size: 14px; line-height: 20px; }
.schedule-week { margin-top: 12px; font-size: 14px; line-height: 20px; }
.schedule-week ul, .schedule-list { list-style: none; margin: 8px 0; padding: 0; }
.schedule-week li { display: flex; gap: 8px; padding: 4px 0; color: var(--aw-text-secondary); font-size: 12px; line-height: 16px; }
.schedule-list li { padding: 8px 0; font-size: 14px; line-height: 20px; }
.schedule-list strong { font-weight: 500; }
.schedule-list small { margin-top: 4px; }
.schedule-list a { display: inline-flex; align-items: center; gap: 8px; color: var(--aw-brand); }
.plan-habit { margin-top: 16px; }
.plan-habit small + small { margin-top: 4px; }
.existing-reference { display: block; margin-top: 8px; color: var(--aw-brand); font-size: 12px; line-height: 16px; }
.revision-note { display: flex; align-items: center; gap: 8px; margin-top: 24px; font-size: 12px; line-height: 16px; }
</style>
