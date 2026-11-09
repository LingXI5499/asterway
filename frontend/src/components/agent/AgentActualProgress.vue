<!-- 显示已提交方案关联对象的真实完成与逾期状态。 -->
<script setup>
/* 引入 vue 提供的组件、状态函数或请求方法。 */
import { computed } from "vue";
const /* 父组件传入的数据（props）保存声明父组件可以传入的属性及其类型或默认值。 */ props = defineProps({ /* 实际进度（tracking）字段使用对象（Object）。 */ tracking: Object });
const /* kinds保存表达式 ['goals', 'stages', 'tasks', 'weeks', 'days', 'habits'] 的结果。 */ kinds = ['goals', 'stages', 'tasks', 'weeks', 'days', 'habits'];
const /* 名称对应表（labels）保存表达式 { goals: '目标', stages: '阶段', tasks: '任务', weeks: '周计划', days: '日计划', habits: '习惯' } 的结果。 */ labels = { /* 目标列表（goals）字段使用固定文字 '目标'。 */ goals: '目标', /* 阶段列表（stages）字段使用固定文字 '阶段'。 */ stages: '阶段', /* 任务列表（tasks）字段使用固定文字 '任务'。 */ tasks: '任务', /* 周计划列表（weeks）字段使用固定文字 '周计划'。 */ weeks: '周计划', /* 日列表（days）字段使用固定文字 '日计划'。 */ days: '日计划', /* 习惯列表（habits）字段使用固定文字 '习惯'。 */ habits: '习惯' };
const /* states保存表达式 { NOT_STARTED: '未开始', IN_PROGRESS: '进行中', COMPLETED: '已完成', ACTIVE: '进行中', PAUSED: '已暂停', ENDED: '已结束' } 的结果。 */ states = { /* NOT开始时（NOT_STARTED）字段使用固定文字 '未开始'。 */ NOT_STARTED: '未开始', /* IN进度（IN_PROGRESS）字段使用固定文字 '进行中'。 */ IN_PROGRESS: '进行中', /* 已完成（COMPLETED）字段使用固定文字 '已完成'。 */ COMPLETED: '已完成', /* 页面是否仍处于活动状态（ACTIVE）字段使用固定文字 '进行中'。 */ ACTIVE: '进行中', /* 已暂停（PAUSED）字段使用固定文字 '已暂停'。 */ PAUSED: '已暂停', /* 已结束（ENDED）字段使用固定文字 '已结束'。 */ ENDED: '已结束' };
const /* 已完成（completed）保存随依赖自动重算的筛选结果或数量。 */ completed = computed(() => props.tracking?.groups?.tasks?.filter(row => row.status === 'COMPLETED').length || 0);
const /* 总数（total）保存随相关状态变化自动重算的值。 */ total = computed(() => props.tracking?.groups?.tasks?.length || 0);
const /* 标题（title）保存根据前面的条件，从两个候选结果中选择一个：row => row.name || row.title || (row.kind === 'weeks' ? `${row.weekStartDate} 起的一周` : row.kind === '…。 */ title = row => row.name || row.title || (row.kind === 'weeks' ? `${row.weekStartDate} 起的一周` : row.kind === 'days' ? row.planDate : '跟踪对象');
const /* link保存按表达式计算得到的值：row => ({ goals: `/growth/goals/${row.id}`, stages: `/growth/stages/${row.id}`, tasks: `/growth/tasks/${row.id}`, weeks:…。 */ link = row => ({ /* 目标列表（goals）字段使用按表达式计算得到的值：`/growth/goals/${row.id}`。 */ goals: `/growth/goals/${row.id}`, /* 阶段列表（stages）字段使用按表达式计算得到的值：`/growth/stages/${row.id}`。 */ stages: `/growth/stages/${row.id}`, /* 任务列表（tasks）字段使用按表达式计算得到的值：`/growth/tasks/${row.id}`。 */ tasks: `/growth/tasks/${row.id}`, /* 周计划列表（weeks）字段使用按表达式计算得到的值：`/planning/weekly-plans?week=${row.weekStartDate}`。 */ weeks: `/planning/weekly-plans?week=${row.weekStartDate}`, /* 日列表（days）字段使用按表达式计算得到的值：`/planning/daily-plans?date=${row.planDate}`。 */ days: `/planning/daily-plans?date=${row.planDate}`, /* 习惯列表（habits）字段使用固定文字 '/habits'。 */ habits: '/habits' })[row.kind];
const /* 习惯状态（habitStatus）保存根据前面的条件，从两个候选结果中选择一个：row => !row.todayKnown ? '今日状态暂未核对' : !row.dueToday ? '今日无需执行' : row.checkedToday ? '今日已打卡' : '今日待完成…。 */ habitStatus = row => !row.todayKnown ? '今日状态暂未核对' : !row.dueToday ? '今日无需执行' : row.checkedToday ? '今日已打卡' : '今日待完成';
</script>

<template>
  <!-- 显示一个有独立含义的内容区；仅当 tracking?.ready 成立时渲染；用样式类 actual-progress 控制外观；aria-label 提供无障碍说明 实际执行进度。 -->
  <section v-if="tracking?.ready" class="actual-progress" aria-label="实际执行进度">
    <!-- 显示一个布局容器；用样式类 progress-heading 控制外观。 -->
    <div class="progress-heading"><!-- 显示三级标题。 --><h3>实际执行进度</h3><!-- 显示辅助说明文字。 --><small><!-- 把 tracking.asOf 的当前值转成文字显示。 -->{{ tracking.asOf }}</small></div>
    <!-- 显示一段文字；仅当 total 成立时渲染。 -->
    <p v-if="total">跟踪任务已完成 <!-- 把 completed 的当前值转成文字显示。 -->{{ completed }} / <!-- 把 total 的当前值转成文字显示。 -->{{ total }} 项<!-- 显示行内文字区域；仅当 tracking.partial 成立时渲染。 --><span v-if="tracking.partial">（已加载范围）</span></p>
    <!-- 显示一段文字；前面条件均不成立时显示这个备用分支。 -->
    <p v-else>进度按各类实际记录展示，方案提交成功不代表任务已完成。</p>
    <!-- 显示一段文字；按 signal in tracking.signals || [] 为每条数据重复生成内容；key 取自 signal.code；用样式类 progress-signal 控制外观。 -->
    <p v-for="signal in tracking.signals || []" :key="signal.code" class="progress-signal"><!-- 显示应用Icon（AppIcon）组件；name 固定为 info；size 取自 14。 --><AppIcon name="info" :size="14" /><!-- 把 signal.message 的当前值转成文字显示。 -->{{ signal.message }}</p>
    <!-- 显示只组织渲染规则、不额外生成普通容器的模板；按 kind in kinds 为每条数据重复生成内容；key 取自 kind。 -->
    <template v-for="kind in kinds" :key="kind">
      <!-- 显示可展开的详情区域；仅当 tracking.groups?.[kind]?.length 成立时渲染；open 取自 kind === 'goals' || kind === 'tasks'；用样式类 progress-group 控制外观。 -->
      <details v-if="tracking.groups?.[kind]?.length" :open="kind === 'goals' || kind === 'tasks'" class="progress-group">
        <!-- 显示详情区域的展开入口。 -->
        <summary><!-- 把 labels[kind] 的当前值转成文字显示。 -->{{ labels[kind] }}<!-- 显示辅助说明文字。 --><small><!-- 把 tracking.groups[kind].length 的当前值转成文字显示。 -->{{ tracking.groups[kind].length }} 项</small></summary>
        <!-- 显示一条独立内容；按 row in tracking.groups[kind] 为每条数据重复生成内容；key 取自 row.id。 -->
        <article v-for="row in tracking.groups[kind]" :key="row.id">
          <!-- 显示应用内部页面链接；to 取自 link(row)。 -->
          <RouterLink :to="link(row)"><!-- 把 title(row) 的当前值转成文字显示。 -->{{ title(row) }}<!-- 显示应用Icon（AppIcon）组件；name 固定为 arrow；size 取自 12。 --><AppIcon name="arrow" :size="12" /></RouterLink>
          <!-- 显示一个布局容器；用样式类 progress-state 控制外观。 -->
          <div class="progress-state"><!-- 显示行内文字区域。 --><span><!-- 把 kind === 'habits' ? habitStatus(row) : states[row.status] || '按实际记录跟踪' 的当前值转成文字显示。 -->{{ kind === 'habits' ? habitStatus(row) : states[row.status] || '按实际记录跟踪' }}</span><!-- 显示重点文字；仅当 ['goals', 'stages'].includes(kind) && row.progress != null 成立时渲染。 --><strong v-if="['goals', 'stages'].includes(kind) && row.progress != null"><!-- 把 row.progress 的当前值转成文字显示。 -->{{ row.progress }}%</strong><!-- 显示行内文字区域；仅当 row.overdue 成立时渲染；用样式类 overdue 控制外观。 --><span v-if="row.overdue" class="overdue">需调整安排</span></div>
          <!-- 显示Element Plus 的 progress 组件；仅当 ['goals', 'stages'].includes(kind) && row.progress != null 成立时渲染；percentage 取自 Math.max(0, Math.min(100, Number(row.progress)))；show-text 取自 false；stroke-width 取自 4。 -->
          <el-progress v-if="['goals', 'stages'].includes(kind) && row.progress != null" :percentage="Math.max(0, Math.min(100, Number(row.progress)))" :show-text="false" :stroke-width="4" />
          <!-- 显示辅助说明文字；仅当 row.taskTotal != null 成立时渲染。 -->
          <small v-if="row.taskTotal != null">已完成 <!-- 把 row.taskCompleted 的当前值转成文字显示。 -->{{ row.taskCompleted }} / <!-- 把 row.taskTotal 的当前值转成文字显示。 -->{{ row.taskTotal }} 项安排<!-- 显示行内文字区域；仅当 row.partial 成立时渲染。 --><span v-if="row.partial">，已加载 <!-- 把 row.taskReturned 的当前值转成文字显示。 -->{{ row.taskReturned }} 项</span></small>
          <!-- 显示辅助说明文字；仅当 kind === 'tasks' && (row.dueDate || row.expectedEndDate) 成立时渲染。 -->
          <small v-if="kind === 'tasks' && (row.dueDate || row.expectedEndDate)">截止 <!-- 把 row.dueDate || row.expectedEndDate 的当前值转成文字显示。 -->{{ row.dueDate || row.expectedEndDate }}</small>
        </article>
      </details>
    </template>
  </section>
</template>

<style scoped>
/* 以下规则应用于 .actual-progress 选中的元素。 */
.actual-progress { /* 设置元素外侧留白；取值 24px 0。 */ margin: 24px 0; }
/* 以下规则应用于 .progress-heading, .progress-state 选中的元素。 */
.progress-heading, .progress-state { /* 决定元素采用普通块、行内、Flex 或网格布局；取值 flex。 */ display: flex; /* 设置条目在主方向上的对齐与空余空间分配；取值 space-between。 */ justify-content: space-between; /* 设置 Flex 或网格条目在交叉方向上的对齐；取值 center。 */ align-items: center; /* 设置相邻布局条目之间的间隔；取值 8px。 */ gap: 8px; }
/* 以下规则应用于 h3 选中的元素。 */
h3 { /* 设置字号；取值 16px。 */ font-size: 16px; /* 设置每行文字的高度；取值 24px。 */ line-height: 24px; /* 设置元素外侧留白；取值 0。 */ margin: 0; }
/* 以下规则应用于 p, summary, a 选中的元素。 */
p, summary, a { /* 设置字号；取值 14px。 */ font-size: 14px; /* 设置每行文字的高度；取值 20px。 */ line-height: 20px; }
/* 以下规则应用于 p, small, .progress-state 选中的元素。 */
p, small, .progress-state { /* 设置文字或继承该颜色的图形颜色；取值 var(—aw-text-secondary)。 */ color: var(--aw-text-secondary); }
/* 以下规则应用于 p 选中的元素。 */
p { /* 设置元素外侧留白；取值 8px 0 16px。 */ margin: 8px 0 16px; }
/* 以下规则应用于 small, .progress-state 选中的元素。 */
small, .progress-state { /* 设置字号；取值 12px。 */ font-size: 12px; /* 设置每行文字的高度；取值 16px。 */ line-height: 16px; }
/* 以下规则应用于 .progress-signal 选中的元素。 */
.progress-signal { /* 决定元素采用普通块、行内、Flex 或网格布局；取值 flex。 */ display: flex; /* 设置相邻布局条目之间的间隔；取值 8px。 */ gap: 8px; /* 设置 Flex 或网格条目在交叉方向上的对齐；取值 flex-start。 */ align-items: flex-start; /* 设置内容与边框之间的内侧留白；取值 12px。 */ padding: 12px; /* 设置边框宽度、线型与颜色；取值 1px solid var(—aw-border)。 */ border: 1px solid var(--aw-border); /* 设置边角圆弧大小；取值 8px。 */ border-radius: 8px; /* 设置背景颜色、图片或渐变；取值 var(—aw-bg)。 */ background: var(--aw-bg); }
/* 以下规则应用于 .progress-signal :deep(svg) 选中的元素。 */
.progress-signal :deep(svg) { /* 设置空间不足时条目缩小的比例；取值 0。 */ flex-shrink: 0; /* 设置上方外侧留白；取值 2px。 */ margin-top: 2px; }
/* 以下规则应用于 .progress-group 选中的元素。 */
.progress-group { /* 设置上方外侧留白；取值 16px。 */ margin-top: 16px; }
/* 以下规则应用于 .progress-group summary 选中的元素。 */
.progress-group summary { /* 设置鼠标移上来时的指针形状；取值 pointer。 */ cursor: pointer; /* 设置文字或继承该颜色的图形颜色；取值 var(—aw-text)。 */ color: var(--aw-text); }
/* 以下规则应用于 .progress-group summary small 选中的元素。 */
.progress-group summary small { /* 设置左侧外侧留白；取值 8px。 */ margin-left: 8px; }
/* 以下规则应用于 article 选中的元素。 */
article { /* 设置上方外侧留白；取值 16px。 */ margin-top: 16px; }
/* 以下规则应用于 article a 选中的元素。 */
article a { /* 决定元素采用普通块、行内、Flex 或网格布局；取值 flex。 */ display: flex; /* 设置 Flex 或网格条目在交叉方向上的对齐；取值 center。 */ align-items: center; /* 设置条目在主方向上的对齐与空余空间分配；取值 space-between。 */ justify-content: space-between; /* 设置相邻布局条目之间的间隔；取值 8px。 */ gap: 8px; }
/* 以下规则应用于 .progress-state 选中的元素。 */
.progress-state { /* 设置元素外侧留白；取值 8px 0。 */ margin: 8px 0; }
/* 以下规则应用于 article > small 选中的元素。 */
article > small { /* 决定元素采用普通块、行内、Flex 或网格布局；取值 block。 */ display: block; /* 设置上方外侧留白；取值 4px。 */ margin-top: 4px; }
/* 以下规则应用于 .overdue 选中的元素。 */
.overdue { /* 设置文字或继承该颜色的图形颜色；取值 var(—aw-brand)。 */ color: var(--aw-brand); }
</style>
