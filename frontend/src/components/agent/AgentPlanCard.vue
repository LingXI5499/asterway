<!-- 渲染成长助手方案Card界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
/* 引入 vue 提供的组件、状态函数或请求方法。 */
import { computed } from "vue";
/* 引入 ../../api/growth 提供的组件、状态函数或请求方法。 */
import { priorities } from "../../api/growth";
const /* 父组件传入的数据（props）保存声明父组件可以传入的属性及其类型或默认值。 */ props = defineProps({ /* 方案（plan）字段使用表达式 { type: Object, required: true } 的结果。 */ plan: { /* 类型（type）字段使用对象（Object）。 */ type: Object, /* 必填（required）字段使用真，表示条件成立或开关打开。 */ required: true } });
const /* 确认时的数据快照（snapshot）保存随相关状态变化自动重算的值。 */ snapshot = computed(() => props.plan.snapshot || {});
const /* 阶段列表（stages）保存随依赖自动重算的筛选结果或数量。 */ stages = computed(() => (snapshot.value.stages || []).map(stage => ({ ...stage,
  /* 下级列表（children）字段使用优先使用前面的有效值，没有时采用后面的备用值：(snapshot.value.tasks || []).filter(task => task.stageKey && task.stageKey === stage.clientKey)。 */
  children: (snapshot.value.tasks || []).filter(task => task.stageKey && task.stageKey === stage.clientKey) })));
const /* loose任务列表（looseTasks）保存随依赖自动重算的筛选结果或数量。 */ looseTasks = computed(() => (snapshot.value.tasks || []).filter(task => !stages.value.some(stage => stage.children.includes(task))));
const /* schedules保存随相关状态变化自动重算的值。 */ schedules = computed(() => snapshot.value.taskAssignments || []);
const /* 任务Names（taskNames）保存随相关状态变化自动重算的值。 */ taskNames = computed(() => Object.fromEntries((snapshot.value.tasks || []).map(task => [task.clientKey, task.title])));
const /* weekday保存表达式 value => ({ 1: '周一', 2: '周二', 3: '周三', 4: '周四', 5: '周五', 6: '周六', 7: '周日' })[value] 的结果。 */ weekday = value => ({ /* 1字段使用固定文字 '周一'。 */ 1: '周一', /* 2字段使用固定文字 '周二'。 */ 2: '周二', /* 3字段使用固定文字 '周三'。 */ 3: '周三', /* 4字段使用固定文字 '周四'。 */ 4: '周四', /* 5字段使用固定文字 '周五'。 */ 5: '周五', /* 6字段使用固定文字 '周六'。 */ 6: '周六', /* 7字段使用固定文字 '周日'。 */ 7: '周日' })[value];
/* dates：调用 dates 完成dates处理。 */
function dates(item) {
  /* 检查条件 item.startDate && item.dueDate 是否成立，成立时走这一分支。 */
  if (item.startDate && item.dueDate) /* 返回按表达式计算得到的值：item.startDate + ' 至 ' + item.dueDate，当前函数到此结束。 */ return item.startDate + ' 至 ' + item.dueDate;
  /* 返回根据前面的条件，从两个候选结果中选择一个：item.startDate ? item.startDate + ' 开始' : item.dueDate ? item.dueDate + ' 截止' : '日期待安排'，当前函数到此结束。 */
  return item.startDate ? item.startDate + ' 开始' : item.dueDate ? item.dueDate + ' 截止' : '日期待安排';
}
/* assigned日期（assignedDate）：调用 assignedDate 完成assigned日期处理。 */
function assignedDate(task) {
  /* 返回只保留符合条件的条目，当前函数到此结束。 */
  return schedules.value.filter(item => item.taskKey === task.clientKey && item.businessDate).map(item => item.businessDate).join('、');
}
</script>

<template>
  <!-- 显示一条独立内容；用样式类 plan-card 控制外观。 -->
  <article class="plan-card">
    <!-- 显示顶部区域；仅当 snapshot.goal 成立时渲染；用样式类 plan-goal 控制外观。 -->
    <header v-if="snapshot.goal" class="plan-goal"><!-- 显示行内文字区域；用样式类 plan-section-label 控制外观。 --><span class="plan-section-label">目标</span><!-- 显示三级标题。 --><h3><!-- 把 snapshot.goal.title 的当前值转成文字显示。 -->{{ snapshot.goal.title }}</h3><!-- 显示一段文字；仅当 snapshot.goal.description 成立时渲染。 --><p v-if="snapshot.goal.description"><!-- 把 snapshot.goal.description 的当前值转成文字显示。 -->{{ snapshot.goal.description }}</p><!-- 显示一个布局容器；用样式类 plan-date 控制外观。 --><div class="plan-date"><!-- 显示应用Icon（AppIcon）组件；name 固定为 day；size 取自 14。 --><AppIcon name="day" :size="14" /><!-- 把 dates(snapshot.goal) 的当前值转成文字显示。 -->{{ dates(snapshot.goal) }}</div><!-- 显示一段文字；仅当 snapshot.goal.completionCriteria 成立时渲染；用样式类 plan-criteria 控制外观。 --><p v-if="snapshot.goal.completionCriteria" class="plan-criteria"><!-- 显示重点文字。 --><strong>完成标准</strong><!-- 把 snapshot.goal.completionCriteria 的当前值转成文字显示。 -->{{ snapshot.goal.completionCriteria }}</p></header>
    <!-- 显示顶部区域；前面条件均不成立时显示这个备用分支；用样式类 plan-goal 控制外观。 -->
    <header v-else class="plan-goal"><!-- 显示三级标题。 --><h3><!-- 把 plan.summary 的当前值转成文字显示。 -->{{ plan.summary }}</h3></header>
    <!-- 显示一个布局容器；仅当 snapshot.stages?.length || snapshot.tasks?.length 成立时渲染；用样式类 plan-counts 控制外观。 -->
    <div v-if="snapshot.stages?.length || snapshot.tasks?.length" class="plan-counts"><!-- 显示行内文字区域。 --><span><!-- 把 snapshot.stages?.length || 0 的当前值转成文字显示。 -->{{ snapshot.stages?.length || 0 }} 个阶段</span><!-- 显示行内文字区域。 --><span><!-- 把 snapshot.tasks?.length || 0 的当前值转成文字显示。 -->{{ snapshot.tasks?.length || 0 }} 个任务</span><!-- 显示行内文字区域；仅当 snapshot.dailyPlans?.length 成立时渲染。 --><span v-if="snapshot.dailyPlans?.length"><!-- 把 snapshot.dailyPlans.length 的当前值转成文字显示。 -->{{ snapshot.dailyPlans.length }} 天安排</span></div>
    <!-- 显示一个有独立含义的内容区；按 (stage, index) in stages 为每条数据重复生成内容；key 取自 stage.clientKey；用样式类 roadmap-stage 控制外观。 -->
    <section v-for="(stage, index) in stages" :key="stage.clientKey" class="roadmap-stage">
      <!-- 显示一个布局容器；用样式类 stage-heading 控制外观。 -->
      <div class="stage-heading"><!-- 显示行内文字区域；用样式类 stage-number 控制外观。 --><span class="stage-number"><!-- 把 index + 1 的当前值转成文字显示。 -->{{ index + 1 }}</span><!-- 显示一个布局容器。 --><div><!-- 显示h4组件。 --><h4><!-- 把 stage.title 的当前值转成文字显示。 -->{{ stage.title }}</h4><!-- 显示辅助说明文字。 --><small><!-- 把 dates(stage) 的当前值转成文字显示。 -->{{ dates(stage) }}</small></div></div>
      <!-- 显示一个布局容器；用样式类 stage-content 控制外观。 -->
      <div class="stage-content"><!-- 显示一段文字；仅当 stage.description 成立时渲染。 --><p v-if="stage.description"><!-- 把 stage.description 的当前值转成文字显示。 -->{{ stage.description }}</p><!-- 显示一段文字；仅当 stage.completionCriteria 成立时渲染；用样式类 stage-criteria 控制外观。 --><p v-if="stage.completionCriteria" class="stage-criteria">完成标准：<!-- 把 stage.completionCriteria 的当前值转成文字显示。 -->{{ stage.completionCriteria }}</p><!-- 显示应用内部页面链接；仅当 stage.goalId 成立时渲染；用样式类 existing-reference 控制外观；to 取自 `/growth/goals/${stage.goalId}`。 --><RouterLink v-if="stage.goalId" class="existing-reference" :to="`/growth/goals/${stage.goalId}`">查看所属目标</RouterLink>
        <!-- 显示有序列表；仅当 stage.children.length 成立时渲染；用样式类 roadmap-tasks 控制外观。 -->
        <ol v-if="stage.children.length" class="roadmap-tasks"><!-- 显示一项列表内容；按 task in stage.children 为每条数据重复生成内容；key 取自 task.clientKey。 --><li v-for="task in stage.children" :key="task.clientKey"><!-- 显示行内文字区域；用样式类 task-bullet 控制外观。 --><span class="task-bullet"></span><!-- 显示一个布局容器。 --><div><!-- 显示重点文字。 --><strong><!-- 把 task.title 的当前值转成文字显示。 -->{{ task.title }}</strong><!-- 显示一段文字；仅当 task.description 成立时渲染。 --><p v-if="task.description"><!-- 把 task.description 的当前值转成文字显示。 -->{{ task.description }}</p><!-- 显示一段文字；仅当 task.completionCriteria 成立时渲染。 --><p v-if="task.completionCriteria">完成标准：<!-- 把 task.completionCriteria 的当前值转成文字显示。 -->{{ task.completionCriteria }}</p><!-- 显示一个布局容器；用样式类 task-meta 控制外观。 --><div class="task-meta"><!-- 显示行内文字区域；仅当 task.estimatedMinutes 成立时渲染。 --><span v-if="task.estimatedMinutes">预计 <!-- 把 task.estimatedMinutes 的当前值转成文字显示。 -->{{ task.estimatedMinutes }} 分钟</span><!-- 显示行内文字区域；仅当 task.priority 成立时渲染。 --><span v-if="task.priority"><!-- 把 priorities[task.priority] 的当前值转成文字显示。 -->{{ priorities[task.priority] }}优先级</span><!-- 显示行内文字区域；仅当 task.weight && task.weight !== 1 成立时渲染。 --><span v-if="task.weight && task.weight !== 1">权重 <!-- 把 task.weight 的当前值转成文字显示。 -->{{ task.weight }}</span></div><!-- 显示辅助说明文字；仅当 task.startDate || task.dueDate 成立时渲染。 --><small v-if="task.startDate || task.dueDate"><!-- 把 dates(task) 的当前值转成文字显示。 -->{{ dates(task) }}</small><!-- 显示辅助说明文字；仅当 assignedDate(task) 成立时渲染；用样式类 task-arrangement 控制外观。 --><small v-if="assignedDate(task)" class="task-arrangement">安排在 <!-- 把 assignedDate(task) 的当前值转成文字显示。 -->{{ assignedDate(task) }}</small></div></li></ol>
      </div>
    </section>
    <!-- 显示一个有独立含义的内容区；仅当 looseTasks.length 成立时渲染；用样式类 plan-section 控制外观。 -->
    <section v-if="looseTasks.length" class="plan-section"><!-- 显示h4组件。 --><h4>任务</h4><!-- 显示有序列表；用样式类 loose-tasks 控制外观。 --><ol class="loose-tasks"><!-- 显示一项列表内容；按 task in looseTasks 为每条数据重复生成内容；key 取自 task.clientKey。 --><li v-for="task in looseTasks" :key="task.clientKey"><!-- 显示重点文字。 --><strong><!-- 把 task.title 的当前值转成文字显示。 -->{{ task.title }}</strong><!-- 显示一段文字；仅当 task.description 成立时渲染。 --><p v-if="task.description"><!-- 把 task.description 的当前值转成文字显示。 -->{{ task.description }}</p><!-- 显示一段文字；仅当 task.completionCriteria 成立时渲染。 --><p v-if="task.completionCriteria">完成标准：<!-- 把 task.completionCriteria 的当前值转成文字显示。 -->{{ task.completionCriteria }}</p><!-- 显示辅助说明文字。 --><small><!-- 把 dates(task) 的当前值转成文字显示。 -->{{ dates(task) }}<!-- 显示行内文字区域；仅当 task.estimatedMinutes 成立时渲染。 --><span v-if="task.estimatedMinutes"> · <!-- 把 task.estimatedMinutes 的当前值转成文字显示。 -->{{ task.estimatedMinutes }} 分钟</span><!-- 显示行内文字区域；仅当 task.priority 成立时渲染。 --><span v-if="task.priority"> · <!-- 把 priorities[task.priority] 的当前值转成文字显示。 -->{{ priorities[task.priority] }}优先级</span></small><!-- 显示应用内部页面链接；仅当 task.stageId 成立时渲染；用样式类 existing-reference 控制外观；to 取自 `/growth/stages/${task.stageId}`。 --><RouterLink v-if="task.stageId" class="existing-reference" :to="`/growth/stages/${task.stageId}`">查看所属阶段</RouterLink></li></ol></section>
    <!-- 显示一个有独立含义的内容区；仅当 snapshot.weeklyPlans?.length 成立时渲染；用样式类 plan-section 控制外观。 -->
    <section v-if="snapshot.weeklyPlans?.length" class="plan-section"><!-- 显示h4组件。 --><h4>周计划</h4><!-- 显示一个布局容器；按 week in snapshot.weeklyPlans 为每条数据重复生成内容；key 取自 week.clientKey；用样式类 schedule-week 控制外观。 --><div v-for="week in snapshot.weeklyPlans" :key="week.clientKey" class="schedule-week"><!-- 显示行内文字区域。 --><span><!-- 把 week.weekStart 的当前值转成文字显示。 -->{{ week.weekStart }} 起的一周</span><!-- 显示无序列表；仅当 snapshot.dailyPlans?.some(day => day.weekKey === week.clientKey) 成立时渲染。 --><ul v-if="snapshot.dailyPlans?.some(day => day.weekKey === week.clientKey)"><!-- 显示一项列表内容；按 day in snapshot.dailyPlans.filter(item => item.weekKey === week.clientKey) 为每条数据重复生成内容；key 取自 day.clientKey。 --><li v-for="day in snapshot.dailyPlans.filter(item => item.weekKey === week.clientKey)" :key="day.clientKey"><!-- 显示应用Icon（AppIcon）组件；name 固定为 day；size 取自 14。 --><AppIcon name="day" :size="14" /><!-- 显示行内文字区域。 --><span><!-- 把 day.businessDate 的当前值转成文字显示。 -->{{ day.businessDate }}</span></li></ul></div></section>
    <!-- 显示一个有独立含义的内容区；仅当 schedules.length 成立时渲染；用样式类 plan-section 控制外观。 -->
    <section v-if="schedules.length" class="plan-section"><!-- 显示h4组件。 --><h4>任务安排</h4><!-- 显示无序列表；用样式类 schedule-list 控制外观。 --><ul class="schedule-list"><!-- 显示一项列表内容；按 item in schedules 为每条数据重复生成内容；key 取自 item.clientKey。 --><li v-for="item in schedules" :key="item.clientKey"><!-- 显示一个布局容器。 --><div><!-- 显示重点文字；仅当 item.taskKey 成立时渲染。 --><strong v-if="item.taskKey"><!-- 把 taskNames[item.taskKey] || '新任务' 的当前值转成文字显示。 -->{{ taskNames[item.taskKey] || '新任务' }}</strong><!-- 显示应用内部页面链接；前面条件均不成立时显示这个备用分支；to 取自 `/growth/tasks/${item.taskId}`。 --><RouterLink v-else :to="`/growth/tasks/${item.taskId}`"><!-- 把 item.taskTitle || '查看已选任务' 的当前值转成文字显示。 -->{{ item.taskTitle || '查看已选任务' }}<!-- 显示应用Icon（AppIcon）组件；name 固定为 arrow；size 取自 12。 --><AppIcon name="arrow" :size="12" /></RouterLink><!-- 显示辅助说明文字；仅当 item.taskId 成立时渲染。 --><small v-if="item.taskId">原安排：<!-- 把 item.previousBusinessDate || (item.previousWeekStart ? item.previousWeekStart + ' 起的一周内' : '任务池') 的当前值转成文字显示。 -->{{ item.previousBusinessDate || (item.previousWeekStart ? item.previousWeekStart + ' 起的一周内' : '任务池') }}</small><!-- 显示辅助说明文字。 --><small><!-- 把 item.taskId ? '调整为：' : '' 的当前值转成文字显示。 -->{{ item.taskId ? '调整为：' : '' }}<!-- 把 item.businessDate || (snapshot.weeklyPlans?.find(week => week.clientKey === item.weekKey)?.weekStart + ' 起的一周内') 的当前值转成文字显示。 -->{{ item.businessDate || (snapshot.weeklyPlans?.find(week => week.clientKey === item.weekKey)?.weekStart + ' 起的一周内') }}</small><!-- 显示辅助说明文字；仅当 item.reason 成立时渲染。 --><small v-if="item.reason">原因：<!-- 把 item.reason 的当前值转成文字显示。 -->{{ item.reason }}</small></div></li></ul></section>
    <!-- 显示一个有独立含义的内容区；仅当 snapshot.habits?.length 成立时渲染；用样式类 plan-section 控制外观。 -->
    <section v-if="snapshot.habits?.length" class="plan-section"><!-- 显示h4组件。 --><h4>习惯</h4><!-- 显示一条独立内容；按 habit in snapshot.habits 为每条数据重复生成内容；key 取自 habit.clientKey；用样式类 plan-habit 控制外观。 --><article v-for="habit in snapshot.habits" :key="habit.clientKey" class="plan-habit"><!-- 显示重点文字。 --><strong><!-- 把 habit.name 的当前值转成文字显示。 -->{{ habit.name }}</strong><!-- 显示一段文字；仅当 habit.description 成立时渲染。 --><p v-if="habit.description"><!-- 把 habit.description 的当前值转成文字显示。 -->{{ habit.description }}</p><!-- 显示一段文字；仅当 habit.completionCriteria 成立时渲染。 --><p v-if="habit.completionCriteria">完成标准：<!-- 把 habit.completionCriteria 的当前值转成文字显示。 -->{{ habit.completionCriteria }}</p><!-- 显示辅助说明文字。 --><small><!-- 把 habit.cycleType === 'DAILY' ? '每天' : habit.weekdays?.map(weekday).join('、') 的当前值转成文字显示。 -->{{ habit.cycleType === 'DAILY' ? '每天' : habit.weekdays?.map(weekday).join('、') }} <!-- 把 habit.expectedTime || '' 的当前值转成文字显示。 -->{{ habit.expectedTime || '' }} · <!-- 把 habit.startDate 的当前值转成文字显示。 -->{{ habit.startDate }} 开始</small><!-- 显示辅助说明文字；仅当 habit.categoryName 成立时渲染。 --><small v-if="habit.categoryName">分类：<!-- 把 habit.categoryName 的当前值转成文字显示。 -->{{ habit.categoryName }}</small></article></section>
    <!-- 显示一段文字；仅当 plan.diffSummary 成立时渲染；用样式类 revision-note 控制外观。 -->
    <p v-if="plan.diffSummary" class="revision-note"><!-- 显示应用Icon（AppIcon）组件；name 固定为 history；size 取自 14。 --><AppIcon name="history" :size="14" />已按补充要求更新，接受时以这一版为准。</p>
  </article>
</template>

<style scoped>
/* 以下规则应用于 .plan-card 选中的元素。 */
.plan-card { /* 限制最小宽度；取值 0。 */ min-width: 0; /* 设置文字或继承该颜色的图形颜色；取值 var(—aw-text)。 */ color: var(--aw-text); }
/* 以下规则应用于 h3 选中的元素。 */
h3 { /* 设置元素外侧留白；取值 8px 0 12px。 */ margin: 8px 0 12px; /* 设置字号；取值 20px。 */ font-size: 20px; /* 设置每行文字的高度；取值 28px。 */ line-height: 28px; /* 设置字重；取值 600。 */ font-weight: 600; /* 设置 CSS 属性 overflow-wrap；取值 anywhere。 */ overflow-wrap: anywhere; }
/* 以下规则应用于 h4 选中的元素。 */
h4 { /* 设置元素外侧留白；取值 0 0 8px。 */ margin: 0 0 8px; /* 设置字号；取值 14px。 */ font-size: 14px; /* 设置每行文字的高度；取值 20px。 */ line-height: 20px; /* 设置字重；取值 600。 */ font-weight: 600; }
/* 以下规则应用于 p 选中的元素。 */
p { /* 设置元素外侧留白；取值 8px 0。 */ margin: 8px 0; /* 设置字号；取值 14px。 */ font-size: 14px; /* 设置每行文字的高度；取值 20px。 */ line-height: 20px; /* 设置文字或继承该颜色的图形颜色；取值 var(—aw-text-secondary)。 */ color: var(--aw-text-secondary); /* 决定空白和换行如何保留；取值 pre-wrap。 */ white-space: pre-wrap; /* 设置 CSS 属性 overflow-wrap；取值 anywhere。 */ overflow-wrap: anywhere; }
/* 以下规则应用于 small 选中的元素。 */
small { /* 决定元素采用普通块、行内、Flex 或网格布局；取值 block。 */ display: block; /* 设置文字或继承该颜色的图形颜色；取值 var(—aw-text-secondary)。 */ color: var(--aw-text-secondary); /* 设置字号；取值 12px。 */ font-size: 12px; /* 设置每行文字的高度；取值 16px。 */ line-height: 16px; }
/* 以下规则应用于 .plan-section-label 选中的元素。 */
.plan-section-label { /* 设置文字或继承该颜色的图形颜色；取值 var(—aw-text-secondary)。 */ color: var(--aw-text-secondary); /* 设置字号；取值 12px。 */ font-size: 12px; /* 设置每行文字的高度；取值 16px。 */ line-height: 16px; }
/* 以下规则应用于 .plan-date 选中的元素。 */
.plan-date { /* 决定元素采用普通块、行内、Flex 或网格布局；取值 flex。 */ display: flex; /* 设置 Flex 或网格条目在交叉方向上的对齐；取值 center。 */ align-items: center; /* 设置相邻布局条目之间的间隔；取值 8px。 */ gap: 8px; /* 设置文字或继承该颜色的图形颜色；取值 var(—aw-text-secondary)。 */ color: var(--aw-text-secondary); /* 设置字号；取值 12px。 */ font-size: 12px; /* 设置每行文字的高度；取值 16px。 */ line-height: 16px; /* 设置元素外侧留白；取值 16px 0。 */ margin: 16px 0; }
/* 以下规则应用于 .plan-criteria 选中的元素。 */
.plan-criteria { /* 设置内容与边框之间的内侧留白；取值 12px。 */ padding: 12px; /* 设置背景颜色、图片或渐变；取值 var(—aw-bg)。 */ background: var(--aw-bg); /* 设置边角圆弧大小；取值 8px。 */ border-radius: 8px; /* 设置字号；取值 12px。 */ font-size: 12px; /* 设置每行文字的高度；取值 16px。 */ line-height: 16px; }
/* 以下规则应用于 .plan-criteria strong 选中的元素。 */
.plan-criteria strong { /* 决定元素采用普通块、行内、Flex 或网格布局；取值 block。 */ display: block; /* 设置文字或继承该颜色的图形颜色；取值 var(—aw-text)。 */ color: var(--aw-text); /* 设置下方外侧留白；取值 8px。 */ margin-bottom: 8px; }
/* 以下规则应用于 .plan-counts 选中的元素。 */
.plan-counts { /* 决定元素采用普通块、行内、Flex 或网格布局；取值 flex。 */ display: flex; /* 设置相邻布局条目之间的间隔；取值 16px。 */ gap: 16px; /* 决定一行放不下时是否换行；取值 wrap。 */ flex-wrap: wrap; /* 设置元素外侧留白；取值 24px 0。 */ margin: 24px 0; /* 设置文字或继承该颜色的图形颜色；取值 var(—aw-text-secondary)。 */ color: var(--aw-text-secondary); /* 设置字号；取值 12px。 */ font-size: 12px; /* 设置每行文字的高度；取值 16px。 */ line-height: 16px; }
/* 以下规则应用于 .roadmap-stage 选中的元素。 */
.roadmap-stage { /* 决定元素是否参与普通排版，以及按谁定位；取值 relative。 */ position: relative; /* 设置上方外侧留白；取值 24px。 */ margin-top: 24px; }
/* 以下规则应用于 .stage-heading 选中的元素。 */
.stage-heading { /* 决定元素采用普通块、行内、Flex 或网格布局；取值 flex。 */ display: flex; /* 设置相邻布局条目之间的间隔；取值 8px。 */ gap: 8px; /* 设置 Flex 或网格条目在交叉方向上的对齐；取值 flex-start。 */ align-items: flex-start; }
/* 以下规则应用于 .stage-heading h4 选中的元素。 */
.stage-heading h4 { /* 设置下方外侧留白；取值 4px。 */ margin-bottom: 4px; }
/* 以下规则应用于 .stage-number 选中的元素。 */
.stage-number { /* 决定元素采用普通块、行内、Flex 或网格布局；取值 grid。 */ display: grid; /* 设置 CSS 属性 place-items；取值 center。 */ place-items: center; /* 设置空间不足时条目缩小的比例；取值 0。 */ flex-shrink: 0; /* 设置宽度；取值 24px。 */ width: 24px; /* 设置高度；取值 24px。 */ height: 24px; /* 设置边角圆弧大小；取值 50%。 */ border-radius: 50%; /* 设置背景颜色、图片或渐变；取值 var(—aw-brand-soft)。 */ background: var(--aw-brand-soft); /* 设置文字或继承该颜色的图形颜色；取值 var(—aw-brand)。 */ color: var(--aw-brand); /* 设置字号；取值 12px。 */ font-size: 12px; /* 设置每行文字的高度；取值 16px。 */ line-height: 16px; }
/* 以下规则应用于 .stage-content 选中的元素。 */
.stage-content { /* 设置左侧外侧留白；取值 32px。 */ margin-left: 32px; /* 设置底部内侧留白；取值 8px。 */ padding-bottom: 8px; }
/* 以下规则应用于 .stage-criteria 选中的元素。 */
.stage-criteria { /* 设置字号；取值 12px。 */ font-size: 12px; /* 设置每行文字的高度；取值 16px。 */ line-height: 16px; }
/* 以下规则应用于 .roadmap-tasks 选中的元素。 */
.roadmap-tasks { /* 设置 CSS 属性 list-style；取值 none。 */ list-style: none; /* 设置元素外侧留白；取值 16px 0 0。 */ margin: 16px 0 0; /* 设置内容与边框之间的内侧留白；取值 0。 */ padding: 0; }
/* 以下规则应用于 .roadmap-tasks > li 选中的元素。 */
.roadmap-tasks > li { /* 决定元素采用普通块、行内、Flex 或网格布局；取值 flex。 */ display: flex; /* 设置相邻布局条目之间的间隔；取值 12px。 */ gap: 12px; /* 设置元素外侧留白；取值 16px 0。 */ margin: 16px 0; }
/* 以下规则应用于 .roadmap-tasks strong, .loose-tasks strong, .plan-habit strong 选中的元素。 */
.roadmap-tasks strong, .loose-tasks strong, .plan-habit strong { /* 设置字号；取值 14px。 */ font-size: 14px; /* 设置每行文字的高度；取值 20px。 */ line-height: 20px; /* 设置字重；取值 500。 */ font-weight: 500; }
/* 以下规则应用于 .roadmap-tasks p 选中的元素。 */
.roadmap-tasks p { /* 设置字号；取值 12px。 */ font-size: 12px; /* 设置每行文字的高度；取值 16px。 */ line-height: 16px; /* 设置元素外侧留白；取值 4px 0。 */ margin: 4px 0; }
/* 以下规则应用于 .task-bullet 选中的元素。 */
.task-bullet { /* 设置空间不足时条目缩小的比例；取值 0。 */ flex-shrink: 0; /* 设置宽度；取值 8px。 */ width: 8px; /* 设置高度；取值 8px。 */ height: 8px; /* 设置上方外侧留白；取值 8px。 */ margin-top: 8px; /* 设置边框宽度、线型与颜色；取值 1px solid var(—aw-border-strong)。 */ border: 1px solid var(--aw-border-strong); /* 设置边角圆弧大小；取值 50%。 */ border-radius: 50%; }
/* 以下规则应用于 .task-meta 选中的元素。 */
.task-meta { /* 决定元素采用普通块、行内、Flex 或网格布局；取值 flex。 */ display: flex; /* 决定一行放不下时是否换行；取值 wrap。 */ flex-wrap: wrap; /* 设置相邻布局条目之间的间隔；取值 8px。 */ gap: 8px; /* 设置元素外侧留白；取值 8px 0。 */ margin: 8px 0; /* 设置字号；取值 12px。 */ font-size: 12px; /* 设置每行文字的高度；取值 16px。 */ line-height: 16px; /* 设置文字或继承该颜色的图形颜色；取值 var(—aw-text-tertiary)。 */ color: var(--aw-text-tertiary); }
/* 以下规则应用于 .task-arrangement 选中的元素。 */
.task-arrangement { /* 设置上方外侧留白；取值 4px。 */ margin-top: 4px; /* 设置文字或继承该颜色的图形颜色；取值 var(—aw-brand)。 */ color: var(--aw-brand); }
/* 以下规则应用于 .plan-section 选中的元素。 */
.plan-section { /* 设置上方外侧留白；取值 24px。 */ margin-top: 24px; /* 设置顶部内侧留白；取值 16px。 */ padding-top: 16px; /* 设置顶部边框；取值 1px solid var(—aw-border)。 */ border-top: 1px solid var(--aw-border); }
/* 以下规则应用于 .loose-tasks 选中的元素。 */
.loose-tasks { /* 设置左侧内侧留白；取值 16px。 */ padding-left: 16px; /* 设置元素外侧留白；取值 0。 */ margin: 0; }
/* 以下规则应用于 .loose-tasks li 选中的元素。 */
.loose-tasks li { /* 设置元素外侧留白；取值 16px 0。 */ margin: 16px 0; /* 设置字号；取值 14px。 */ font-size: 14px; /* 设置每行文字的高度；取值 20px。 */ line-height: 20px; }
/* 以下规则应用于 .schedule-week 选中的元素。 */
.schedule-week { /* 设置上方外侧留白；取值 12px。 */ margin-top: 12px; /* 设置字号；取值 14px。 */ font-size: 14px; /* 设置每行文字的高度；取值 20px。 */ line-height: 20px; }
/* 以下规则应用于 .schedule-week ul, .schedule-list 选中的元素。 */
.schedule-week ul, .schedule-list { /* 设置 CSS 属性 list-style；取值 none。 */ list-style: none; /* 设置元素外侧留白；取值 8px 0。 */ margin: 8px 0; /* 设置内容与边框之间的内侧留白；取值 0。 */ padding: 0; }
/* 以下规则应用于 .schedule-week li 选中的元素。 */
.schedule-week li { /* 决定元素采用普通块、行内、Flex 或网格布局；取值 flex。 */ display: flex; /* 设置相邻布局条目之间的间隔；取值 8px。 */ gap: 8px; /* 设置内容与边框之间的内侧留白；取值 4px 0。 */ padding: 4px 0; /* 设置文字或继承该颜色的图形颜色；取值 var(—aw-text-secondary)。 */ color: var(--aw-text-secondary); /* 设置字号；取值 12px。 */ font-size: 12px; /* 设置每行文字的高度；取值 16px。 */ line-height: 16px; }
/* 以下规则应用于 .schedule-list li 选中的元素。 */
.schedule-list li { /* 设置内容与边框之间的内侧留白；取值 8px 0。 */ padding: 8px 0; /* 设置字号；取值 14px。 */ font-size: 14px; /* 设置每行文字的高度；取值 20px。 */ line-height: 20px; }
/* 以下规则应用于 .schedule-list strong 选中的元素。 */
.schedule-list strong { /* 设置字重；取值 500。 */ font-weight: 500; }
/* 以下规则应用于 .schedule-list small 选中的元素。 */
.schedule-list small { /* 设置上方外侧留白；取值 4px。 */ margin-top: 4px; }
/* 以下规则应用于 .schedule-list a 选中的元素。 */
.schedule-list a { /* 决定元素采用普通块、行内、Flex 或网格布局；取值 inline-flex。 */ display: inline-flex; /* 设置 Flex 或网格条目在交叉方向上的对齐；取值 center。 */ align-items: center; /* 设置相邻布局条目之间的间隔；取值 8px。 */ gap: 8px; /* 设置文字或继承该颜色的图形颜色；取值 var(—aw-brand)。 */ color: var(--aw-brand); }
/* 以下规则应用于 .plan-habit 选中的元素。 */
.plan-habit { /* 设置上方外侧留白；取值 16px。 */ margin-top: 16px; }
/* 以下规则应用于 .plan-habit small + small 选中的元素。 */
.plan-habit small + small { /* 设置上方外侧留白；取值 4px。 */ margin-top: 4px; }
/* 以下规则应用于 .existing-reference 选中的元素。 */
.existing-reference { /* 决定元素采用普通块、行内、Flex 或网格布局；取值 block。 */ display: block; /* 设置上方外侧留白；取值 8px。 */ margin-top: 8px; /* 设置文字或继承该颜色的图形颜色；取值 var(—aw-brand)。 */ color: var(--aw-brand); /* 设置字号；取值 12px。 */ font-size: 12px; /* 设置每行文字的高度；取值 16px。 */ line-height: 16px; }
/* 以下规则应用于 .revision-note 选中的元素。 */
.revision-note { /* 决定元素采用普通块、行内、Flex 或网格布局；取值 flex。 */ display: flex; /* 设置 Flex 或网格条目在交叉方向上的对齐；取值 center。 */ align-items: center; /* 设置相邻布局条目之间的间隔；取值 8px。 */ gap: 8px; /* 设置上方外侧留白；取值 24px。 */ margin-top: 24px; /* 设置字号；取值 12px。 */ font-size: 12px; /* 设置每行文字的高度；取值 16px。 */ line-height: 16px; }
</style>
