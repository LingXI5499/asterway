<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from "vue";
import { useRouter } from "vue-router";
import {
  dashboardApi,
  duration,
  rateText,
  canStartTask,
} from "../../api/personal";
import { checkHabit, cancelHabitToday } from "../../api/habit-actions";
import { canGrow, states } from "../../api/growth";
import {
  execution,
  startExecution,
  elapsedSeconds,
} from "../../stores/personal";
import { usePaged } from "../../composables/usePaged";
import { report } from "../../api/ui";
import PagePagination from "../../components/personal/PagePagination.vue";
import LoadState from "../../components/personal/LoadState.vue";
import ProgressValue from "../../components/growth/ProgressValue.vue";
const router = useRouter(),
  overview = ref(null),
  overviewError = ref(""),
  overviewBusy = ref(false),
  group = ref("TODAY"),
  pending = ref("");
const metrics = computed(() => overview.value?.metrics);
const tasks = usePaged(
  (params, signal) =>
    dashboardApi.tasks({ ...params, section: group.value }, signal),
  { size: 10 },
);
const goals = usePaged((params, signal) => dashboardApi.goals(params, signal), {
  size: 10,
});
const habits = usePaged(
  (params, signal) => dashboardApi.habits(params, signal),
  { size: 10 },
);
let sequence = 0;
async function loadOverview() {
  const seq = ++sequence;
  overviewBusy.value = true;
  overviewError.value = "";
  try {
    const result = await dashboardApi.overview();
    if (seq === sequence) overview.value = result;
  } catch (e) {
    if (seq === sequence) overviewError.value = e.message;
  } finally {
    if (seq === sequence) overviewBusy.value = false;
  }
}
async function refresh() {
  await Promise.allSettled([
    loadOverview(),
    tasks.load(),
    goals.load(),
    habits.load(),
  ]);
}
async function start(row) {
  try {
    await startExecution(row);
    await loadOverview();
  } catch (e) {
    report(e);
  }
}
async function habit(action, row) {
  if (pending.value) return;
  pending.value = row.habitId;
  try {
    if (action === "check") await checkHabit(row);
    else await cancelHabitToday(row);
    await Promise.all([habits.load(), loadOverview()]);
  } catch (e) {
    report(e);
    if (e.status === 409 || e.status === 404) await habits.load();
  } finally {
    pending.value = "";
  }
}
watch(group, () => tasks.load(true));
watch(() => execution.revision, loadOverview);
const changed = (event) => {
  if (event.detail === "goal" || event.detail === "notification") {
    loadOverview();
    goals.load();
  }
};
onMounted(() => {
  loadOverview();
  window.addEventListener("asterway:personal-change", changed);
});
onBeforeUnmount(() => {
  sequence++;
  window.removeEventListener("asterway:personal-change", changed);
});
</script>
<template>
  <section class="card personal-hero dashboard-welcome">
    <div>
      <span class="eyebrow">{{ overview?.today || "TODAY" }} · 今日看板</span>
      <h2>今天，先完成一件重要的事</h2>
      <p class="muted">看清安排、开始投入，也给每天的坚持留下一次记录。</p>
    </div>
    <div class="toolbar">
      <el-button :loading="overviewBusy" @click="refresh">刷新今日</el-button
      ><el-button
        v-if="canGrow('notification:message:list')"
        @click="router.push('/notifications')"
        >消息中心</el-button
      >
    </div>
  </section>
  <LoadState
    :loading="overviewBusy && !overview"
    :error="overviewError"
    @retry="loadOverview"
  >
    <div v-if="metrics" class="metrics-grid four">
      <article class="card metric">
        <span>今日计划完成</span
        ><strong
          >{{ metrics.todayCurrentCompletedTasks
          }}<small> / {{ metrics.todayTasks }}</small></strong
        >
        <p class="hint">今日安排任务的当前状态</p>
      </article>
      <article class="card metric">
        <span>今日实际投入</span
        ><strong class="metric-duration">{{
          duration(metrics.actualSeconds)
        }}</strong>
        <p class="hint">已排除暂停间隔</p>
      </article>
      <article class="card metric">
        <span>今日习惯完成</span
        ><strong
          >{{ metrics.habitDone
          }}<small> / {{ metrics.habitDue }}</small></strong
        >
        <p class="hint">{{ rateText(metrics.habitCompletionRate) }}</p>
      </article>
      <article class="card metric">
        <span>本周尚未定日</span><strong>{{ metrics.weekUndatedTasks }}</strong>
        <p class="hint">不计入今日计划任务数</p>
      </article>
    </div>
  </LoadState>
  <section v-if="execution.current" class="card running-summary">
    <div>
      <el-tag type="success">当前执行</el-tag
      ><strong>{{ execution.current.taskName }}</strong
      ><span class="mini-clock">{{ duration(elapsedSeconds()) }}</span>
    </div>
    <el-button type="primary" @click="router.push('/execution')"
      >进入执行工作台</el-button
    >
  </section>
  <div class="dashboard-grid">
    <section class="card">
      <div class="section-heading">
        <h2>任务安排</h2>
        <el-button
          v-if="canGrow('planning:weekly:read')"
          link
          type="primary"
          @click="router.push('/planning/weekly-plans')"
          >查看周计划</el-button
        >
      </div>
      <el-tabs v-model="group"
        ><el-tab-pane label="今日任务" name="TODAY" /><el-tab-pane
          label="本周未定日"
          name="WEEK_UNDATED"
      /></el-tabs>
      <LoadState
        :loading="tasks.busy && !tasks.loaded"
        :error="tasks.error"
        @retry="tasks.load"
      >
        <el-table
          :data="tasks.rows"
          v-loading="tasks.busy"
          empty-text="这个分组暂无安排"
        >
          <el-table-column label="任务" min-width="160"
            ><template #default="{ row }"
              ><el-button
                v-if="canGrow('task:task:read')"
                type="primary"
                link
                @click="router.push('/growth/tasks/' + row.taskId)"
                >{{ row.name }}</el-button
              ><span v-else>{{ row.name }}</span></template
            ></el-table-column
          >
          <el-table-column label="状态" width="100"
            ><template #default="{ row }"
              ><el-tag
                :type="row.status === 'COMPLETED' ? 'success' : 'info'"
                >{{ states[row.status] }}</el-tag
              ></template
            ></el-table-column
          >
          <el-table-column label="操作" width="100"
            ><template #default="{ row }"
              ><el-button
                v-if="canStartTask()"
                type="primary"
                link
                :disabled="
                  row.status === 'COMPLETED' ||
                  !!execution.current ||
                  !!execution.busy
                "
                @click="start(row)"
                >开始执行</el-button
              ></template
            ></el-table-column
          > </el-table
        ><PagePagination :state="tasks" @change="tasks.load" />
      </LoadState>
    </section>
    <section class="card">
      <div class="section-heading">
        <h2>今日习惯</h2>
        <el-button
          v-if="canGrow('habit:habit:list')"
          type="primary"
          link
          @click="router.push('/habits')"
          >管理习惯</el-button
        >
      </div>
      <LoadState
        :loading="habits.busy && !habits.loaded"
        :error="habits.error"
        @retry="habits.load"
      >
        <div v-if="habits.rows.length" class="habit-check-list">
          <article
            v-for="row in habits.rows"
            :key="row.habitId"
            class="habit-check-row"
          >
            <div>
              <strong>{{ row.name }}</strong>
              <p class="hint">
                {{ row.expectedTime || "时间自定" }} ·
                {{ row.doneCount ? "今日已完成" : "今日应做" }}
              </p>
            </div>
            <el-button
              v-if="
                !row.doneCount &&
                canGrow('habit:checkin:create') &&
                canGrow('habit:habit:read')
              "
              type="primary"
              plain
              :disabled="!!pending || !row.canCheckin"
              @click="habit('check', row)"
              >打卡</el-button
            >
            <el-button
              v-else-if="
                row.doneCount &&
                canGrow('habit:checkin:cancel') &&
                canGrow('habit:checkin:read')
              "
              type="success"
              plain
              :disabled="!!pending || !row.canCancel"
              @click="habit('cancel', row)"
              >已完成 · 撤销</el-button
            >
            <el-tag v-else :type="row.doneCount ? 'success' : 'info'">{{
              row.doneCount ? "已完成" : "未完成"
            }}</el-tag>
          </article>
        </div>
        <el-empty v-else description="今天暂无应做习惯" />
        <PagePagination :state="habits" @change="habits.load" />
      </LoadState>
    </section>
  </div>
  <section class="card">
    <div class="section-heading">
      <h2>当前目标</h2>
      <el-button
        v-if="canGrow('goal:goal:list')"
        link
        type="primary"
        @click="router.push('/growth/goals')"
        >目标管理</el-button
      >
    </div>
    <LoadState
      :loading="goals.busy && !goals.loaded"
      :error="goals.error"
      @retry="goals.load"
    >
      <el-table
        :data="goals.rows"
        v-loading="goals.busy"
        empty-text="还没有目标，可以从一个小目标开始"
      >
        <el-table-column label="目标" min-width="200"
          ><template #default="{ row }"
            ><el-button
              v-if="canGrow('goal:goal:read')"
              link
              type="primary"
              @click="router.push('/growth/goals/' + row.id)"
              >{{ row.name }}</el-button
            ><span v-else>{{ row.name }}</span></template
          ></el-table-column
        >
        <el-table-column label="当前进度" min-width="200"
          ><template #default="{ row }"
            ><ProgressValue :value="row.currentProgress" /></template
        ></el-table-column>
        <el-table-column label="有效任务" min-width="120"
          ><template #default="{ row }"
            >{{ row.completedTasks }} / {{ row.taskCount }}</template
          ></el-table-column
        >
        <el-table-column label="状态" width="110"
          ><template #default="{ row }"
            ><el-tag :type="row.status === 'COMPLETED' ? 'success' : 'info'">{{
              states[row.status]
            }}</el-tag></template
          ></el-table-column
        > </el-table
      ><PagePagination :state="goals" @change="goals.load" />
    </LoadState>
  </section>
</template>
