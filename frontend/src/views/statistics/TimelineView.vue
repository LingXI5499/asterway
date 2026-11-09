<!-- 渲染成长时间线页面界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
/* 引入 vue 提供的组件、状态函数或请求方法。 */
import { reactive, ref, onMounted } from "vue";
/* 引入 vue-router 提供的组件、状态函数或请求方法。 */
import { useRouter } from "vue-router";
/* 引入 ../../api/personal 提供的组件、状态函数或请求方法。 */
import {
  statisticsApi,
  statisticsParams,
  instantText,
  actions,
  reasons,
  kindNames,
} from "../../api/personal";
/* 引入 ../../api/growth 提供的组件、状态函数或请求方法。 */
import { today } from "../../api/growth";
/* 引入 ../../composables/usePaged 提供的组件、状态函数或请求方法。 */
import { usePaged } from "../../composables/usePaged";
/* 引入 ../../components/personal/PeriodFilters.vue 提供的组件、状态函数或请求方法。 */
import PeriodFilters from "../../components/personal/PeriodFilters.vue";
/* 引入 ../../components/personal/PagePagination.vue 提供的组件、状态函数或请求方法。 */
import PagePagination from "../../components/personal/PagePagination.vue";
/* 引入 ../../components/personal/LoadState.vue 提供的组件、状态函数或请求方法。 */
import LoadState from "../../components/personal/LoadState.vue";
const /* HTTP 路由登记器（router）保存取得用于切换页面的路由器。 */ router = useRouter(),
  /* 错误（error）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  error = ref("");
const /* 筛选条件（filters）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ filters = reactive({
  /* 统计周期（period）字段使用固定状态或编码 MONTH（月）。 */
  period: "MONTH",
  /* anchor日期（anchorDate）字段使用调用 today 完成今天处理。 */
  anchorDate: today(),
  /* dates字段使用一个空列表，用来保存后续多条数据。 */
  dates: [],
  /* 目标编号（goalId）字段使用固定文字 ''。 */
  goalId: "",
  /* 阶段编号（stageId）字段使用固定文字 ''。 */
  stageId: "",
  /* 任务编号（taskId）字段使用固定文字 ''。 */
  taskId: "",
});
let /* applied保存调用 statisticsParams 完成统计参数集合处理。 */ applied = statisticsParams(filters);
const /* 列表（list）保存调用 usePaged( (params, signal) => statisticsApi.timeline 完成成长时间线处理。 */ list = usePaged(
  (params, signal) => statisticsApi.timeline({ ...applied, ...params }, signal),
  { /* immediate字段使用假，表示条件不成立或开关关闭。 */ immediate: false },
);
/* 搜索（search）：调用 search 完成搜索处理。 */
function search() {
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    /* 把调用 statisticsParams 完成统计参数集合处理写入 applied。 */
    applied = statisticsParams(filters);
    /* 把固定文字 ''写入 error.value。 */
    error.value = "";
    /* 读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */
    list.load(true);
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 把表达式 e.message 的结果写入 error.value。 */
    error.value = e.message;
  }
}
/* reset：调用 reset 完成reset处理。 */
function reset() {
  /* 调用 Object.assign 完成安排处理。 */
  Object.assign(filters, {
    /* 统计周期（period）字段使用固定状态或编码 MONTH（月）。 */
    period: "MONTH",
    /* anchor日期（anchorDate）字段使用调用 today 完成今天处理。 */
    anchorDate: today(),
    /* dates字段使用一个空列表，用来保存后续多条数据。 */
    dates: [],
    /* 目标编号（goalId）字段使用固定文字 ''。 */
    goalId: "",
    /* 阶段编号（stageId）字段使用固定文字 ''。 */
    stageId: "",
    /* 任务编号（taskId）字段使用固定文字 ''。 */
    taskId: "",
  });
  /* 调用 search 完成搜索处理。 */
  search();
}
/* 注册组件首次挂载完成后的处理。 */
onMounted(search);
</script>
<template>
  <!-- 显示一个有独立含义的内容区；用样式类 card 控制外观。 -->
  <section class="card">
    <!-- 显示一个布局容器；用样式类 section-heading 控制外观。 -->
    <div class="section-heading">
      <!-- 显示二级标题。 -->
      <h2>留下发生过的事</h2>
      <!-- 显示Element Plus 的 button 组件；click 事件触发时执行 router.push('/statistics')。 -->
      <el-button @click="router.push('/statistics')">返回历史统计</el-button>
    </div>
    <!-- 显示Period筛选条件（PeriodFilters）组件；filters 取自 filters；busy 取自 list.busy；search 事件触发时执行 search；reset 事件触发时执行 reset。 -->
    <PeriodFilters
      :filters="filters"
      :busy="list.busy"
      @search="search"
      @reset="reset"
    />
    <!-- 显示Element Plus 的 alert 组件；仅当 error 成立时渲染；title 取自 error；类型设为 error；closable 取自 false。 -->
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <!-- 显示一段文字；用样式类 hint 控制外观。 -->
    <p class="hint">
      目标、阶段、任务的真实状态，执行时间段、打卡与计划安排各自保留。习惯独立于目标筛选。
    </p>
    <!-- 显示加载当前状态（LoadState）组件；loading 取自 list.busy && !list.loaded；error 取自 list.error；retry 事件触发时执行 list.load。 -->
    <LoadState
      :loading="list.busy && !list.loaded"
      :error="list.error"
      @retry="list.load"
    >
      <!-- 显示Element Plus 的 timeline 组件；仅当 list.rows.length 成立时渲染；用样式类 fact-timeline 控制外观。 -->
      <el-timeline v-if="list.rows.length" class="fact-timeline">
        <!-- 显示Element Plus 的 timeline-item 组件；按 row in list.rows 为每条数据重复生成内容；key 取自 row.kind + ':' + row.factId + ':' + row.eventType；timestamp 取自 instantText(row.occurredAt)；placement 固定为 top；type 取自 row.eventType === 'BASELINE' ? 'info' : 'primary'。 -->
        <el-timeline-item
          v-for="row in list.rows"
          :key="row.kind + ':' + row.factId + ':' + row.eventType"
          :timestamp="instantText(row.occurredAt)"
          placement="top"
          :type="row.eventType === 'BASELINE' ? 'info' : 'primary'"
        >
          <!-- 显示一条独立内容；用样式类 timeline-fact 控制外观。 -->
          <article class="timeline-fact">
            <!-- 显示一个布局容器；用样式类 toolbar 控制外观。 -->
            <div class="toolbar">
              <!-- 显示Element Plus 的 tag 组件；size 固定为 small；类型设为 info。 -->
              <el-tag size="small" type="info"><!-- 把 kindNames[row.kind] 的当前值转成文字显示。 -->{{ kindNames[row.kind] }}</el-tag
              ><!-- 显示重点文字。 --><strong><!-- 把 actions[row.eventType] || reasons[row.eventType] || "历史事件" 的当前值转成文字显示。 -->{{
                actions[row.eventType] || reasons[row.eventType] || "历史事件"
              }}</strong>
            </div>
            <!-- 显示一段文字。 -->
            <p>
              <!-- 把 row.label || (row.kind === "EXECUTION" || row.kind === "ASSIGNMENT" ? "任务 " : "对象 ") + row.objectId 的当前值转成文字显示。 -->
              {{
                row.label ||
                (row.kind === "EXECUTION" || row.kind === "ASSIGNMENT"
                  ? "任务 "
                  : "对象 ") + row.objectId
              }}
            </p>
            <!-- 显示一段文字；仅当 row.eventType === 'BASELINE' 成立时渲染；用样式类 hint 控制外观。 -->
            <p v-if="row.eventType === 'BASELINE'" class="hint">
              这是初始化时可得的状态，不代表该日完成过这个对象。
            </p>
            <!-- 显示一段文字；仅当 row.eventType === 'COMPLETION_IMPORTED' 成立时渲染；用样式类 hint 控制外观。 -->
            <p v-if="row.eventType === 'COMPLETION_IMPORTED'" class="hint">
              依据旧任务可信完成时间导入，未知的过去没有补造。
            </p>
          </article>
        </el-timeline-item> </el-timeline
      ><!-- 显示应用空状态当前状态（AppEmptyState）组件；前面条件均不成立时显示这个备用分支；description 固定为 当前区间还没有真实事件记录。 --><AppEmptyState v-else description="当前区间还没有真实事件记录" />
      <!-- 显示当前页Pagination（PagePagination）组件；state 取自 list；change 事件触发时执行 list.load。 -->
      <PagePagination :state="list" @change="list.load" />
    </LoadState>
  </section>
</template>
