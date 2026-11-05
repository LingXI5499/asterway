<script setup>
import {
  ref,
  reactive,
  onMounted,
  onBeforeUnmount,
  defineAsyncComponent,
} from "vue";
import { useRouter } from "vue-router";
import {
  statisticsApi,
  statisticsParams,
  duration,
  rateText,
  kindNames,
} from "../../api/personal";
import { today, states } from "../../api/growth";
import { usePaged } from "../../composables/usePaged";
import PeriodFilters from "../../components/personal/PeriodFilters.vue";
import PagePagination from "../../components/personal/PagePagination.vue";
import LoadState from "../../components/personal/LoadState.vue";
const ReportChart = defineAsyncComponent(
  () => import("../../components/personal/ReportChart.vue"),
);
const router = useRouter(),
  filters = reactive({
    period: "MONTH",
    anchorDate: today(),
    dates: [],
    goalId: "",
    stageId: "",
    taskId: "",
  });
const summary = ref(null),
  objects = ref([]),
  error = ref(""),
  busy = ref(false),
  groupBy = ref("HABIT"),
  habitReport = ref(null);
let applied = statisticsParams(filters),
  sequence = 0,
  groupSequence = 0,
  controller;
const trends = usePaged(
  (params, signal) => statisticsApi.trends({ ...applied, ...params }, signal),
  { immediate: false, size: 20 },
);
const groups = usePaged(
  async (params, signal) => {
    const seq = ++groupSequence;
    const result = await statisticsApi.habits(
      { ...applied, ...params, groupBy: groupBy.value },
      signal,
    );
    if (seq === groupSequence && !signal.aborted) habitReport.value = result;
    return result.groups;
  },
  { immediate: false },
);
async function search() {
  let params;
  try {
    params = statisticsParams(filters);
  } catch (e) {
    error.value = e.message;
    return;
  }
  const seq = ++sequence;
  controller?.abort();
  controller = new AbortController();
  busy.value = true;
  error.value = "";
  trends.cancel();
  groups.cancel();
  try {
    const result = await Promise.all([
      statisticsApi.summary(params, controller.signal),
      statisticsApi.objects(params, controller.signal),
    ]);
    if (seq !== sequence) return;
    applied = params;
    summary.value = result[0];
    objects.value = result[1];
    habitReport.value = null;
    trends.cancel(true);
    groups.cancel(true);
    await Promise.all([trends.load(true), groups.load(true)]);
  } catch (e) {
    if (seq === sequence && e.name !== "AbortError") error.value = e.message;
  } finally {
    if (seq === sequence) busy.value = false;
  }
}
function reset() {
  Object.assign(filters, {
    period: "MONTH",
    anchorDate: today(),
    dates: [],
    goalId: "",
    stageId: "",
    taskId: "",
  });
  search();
}
function regroup(value) {
  if (busy.value || !summary.value) return;
  groupBy.value = value;
  habitReport.value = null;
  groups.cancel(true);
  groups.load(true);
}
onMounted(search);
onBeforeUnmount(() => {
  sequence++;
  controller?.abort();
});
</script>
<template>
  <section class="card">
    <div class="section-heading">
      <h2>从真实投入里，看见自己的节奏</h2>
      <el-button @click="router.push('/statistics/timeline')"
        >查看成长时间线</el-button
      >
    </div>
    <PeriodFilters
      :filters="filters"
      :busy="busy"
      @search="search"
      @reset="reset"
    />
    <p v-if="summary" class="hint">
      查询日期：{{ summary.dateFrom }} 至 {{ summary.dateTo }} ·
      {{
        summary.timeZone
      }}。当前周期截至今天，习惯保持账户维度，不跟随目标筛选。
    </p>
    <LoadState :loading="busy && !summary" :error="error" @retry="search">
      <div v-if="summary" class="metrics-grid four">
        <article class="card metric inset-metric">
          <span>区间实际投入</span
          ><strong class="metric-duration">{{
            duration(summary.execution.rangeActualSeconds)
          }}</strong>
          <p class="hint">
            累计 {{ duration(summary.execution.totalActualSeconds) }}
          </p>
        </article>
        <article class="card metric inset-metric">
          <span>参与执行会话</span
          ><strong>{{ summary.execution.rangeSessions }}</strong>
          <p class="hint">
            区间已结束 {{ summary.execution.rangeFinishedSessions }}
          </p>
        </article>
        <article class="card metric inset-metric">
          <span>习惯完成率</span
          ><strong>{{ rateText(summary.habits.completionRate) }}</strong>
          <p class="hint">
            {{ summary.habits.doneCount }} /
            {{ summary.habits.dueCount }} 次应做
          </p>
        </article>
        <article class="card metric inset-metric">
          <span>真实任务完成动作</span
          ><strong>{{ summary.states.taskCompletionActions }}</strong>
          <p class="hint">
            去重完成任务 {{ summary.states.completedDistinctTasks }}
          </p>
        </article>
      </div>
    </LoadState>
  </section>
  <section class="card">
    <div class="section-heading">
      <h2>逐日趋势</h2>
      <span class="hint">图表与表格均为当前日期页，不将无应做显示为 0%</span>
    </div>
    <LoadState
      :loading="trends.busy && !trends.loaded"
      :error="trends.error"
      @retry="trends.load"
    >
      <div v-if="trends.rows.length" class="report-charts">
        <div class="chart-panel wide">
          <h3>实际投入</h3>
          <ReportChart
            :rows="trends.rows"
            kind="effort"
            label="当前日期页每日实际投入，单位小时"
          />
        </div>
        <div class="chart-panel">
          <h3>真实完成动作</h3>
          <ReportChart
            :rows="trends.rows"
            kind="actions"
            label="目标阶段任务实际完成动作数量"
          />
        </div>
        <div class="chart-panel">
          <h3>习惯完成率</h3>
          <ReportChart
            :rows="trends.rows"
            kind="habits"
            label="每日习惯完成率，没有应做时留空"
          />
        </div>
      </div>
      <el-table
        :data="trends.rows"
        v-loading="trends.busy"
        empty-text="所选周期暂无日期数据"
      >
        <el-table-column
          prop="businessDate"
          label="日期"
          min-width="130"
        /><el-table-column label="实际投入" min-width="120"
          ><template #default="{ row }">{{
            duration(row.actualSeconds)
          }}</template></el-table-column
        >
        <el-table-column label="习惯" min-width="160"
          ><template #default="{ row }"
            >{{ row.habitDone }} / {{ row.habitDue }} ·
            {{ rateText(row.habitCompletionRate) }}</template
          ></el-table-column
        >
        <el-table-column
          prop="taskCompletionActions"
          label="任务完成动作"
          min-width="130"
        /><el-table-column
          prop="completedDistinctTasks"
          label="去重完成任务"
          min-width="130"
        /><el-table-column
          prop="sessions"
          label="参与会话"
          width="110"
        /> </el-table
      ><PagePagination :state="trends" @change="trends.load" />
    </LoadState>
  </section>
  <section class="card">
    <div class="section-heading">
      <h2>习惯统计</h2>
      <el-radio-group
        :model-value="groupBy"
        :disabled="busy || !summary"
        @update:model-value="regroup"
        ><el-radio-button value="HABIT">按习惯</el-radio-button
        ><el-radio-button value="CATEGORY"
          >按历史分类归属</el-radio-button
        ></el-radio-group
      >
    </div>
    <p class="hint">
      总完成数 /
      总应做数，不平均各习惯的百分比；已删除习惯及已停用分类保留历史。
    </p>
    <p v-if="habitReport" class="hint">
      区间总计 {{ habitReport.totals.doneCount }} /
      {{ habitReport.totals.dueCount }} ·
      {{ rateText(habitReport.totals.completionRate) }}
    </p>
    <LoadState
      :loading="groups.busy && !groups.loaded"
      :error="groups.error"
      @retry="groups.load"
    >
      <el-table
        :data="groups.rows"
        v-loading="groups.busy"
        empty-text="当前周期没有应做或有效打卡"
      >
        <el-table-column label="名称" min-width="200"
          ><template #default="{ row }"
            >{{ row.name }}
            <el-tag v-if="row.deleted" type="info" size="small"
              >历史保留</el-tag
            ></template
          ></el-table-column
        >
        <el-table-column
          prop="doneCount"
          label="完成"
          width="110"
        /><el-table-column
          prop="dueCount"
          label="应做"
          width="110"
        /><el-table-column label="完成率" width="150"
          ><template #default="{ row }">{{
            rateText(row.completionRate)
          }}</template></el-table-column
        > </el-table
      ><PagePagination :state="groups" @change="groups.load" />
    </LoadState>
  </section>
  <section class="card">
    <h2>当前对象概况</h2>
    <p class="hint">仅统计当前有效未归档对象，与上方历史完成动作分开。</p>
    <el-table :data="objects" empty-text="当前没有有效对象"
      ><el-table-column label="对象"
        ><template #default="{ row }">{{
          kindNames[row.kind]
        }}</template></el-table-column
      ><el-table-column label="当前状态"
        ><template #default="{ row }">{{
          states[row.status]
        }}</template></el-table-column
      ><el-table-column prop="objectCount" label="数量"
    /></el-table>
  </section>
</template>
