<script setup>
import { ref, computed, onMounted, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { api } from "../../api/http";
import {
  query,
  today,
  monday,
  addDays,
  validDate,
  canGrow,
} from "../../api/growth";
import { confirmAction, report } from "../../api/ui";
import GrowthActions from "../../components/growth/GrowthActions.vue";
import GrowthTaskTable from "../../components/growth/GrowthTaskTable.vue";
import TaskPicker from "../../components/growth/TaskPicker.vue";
const props = defineProps({ daily: Boolean });
const route = useRoute(),
  router = useRouter();
const selected = ref(
    validDate(route.query.date)
      ? route.query.date
      : validDate(route.query.week)
        ? route.query.week
        : today(),
  ),
  week = ref(null),
  plan = ref(null),
  days = ref([]),
  rows = ref([]),
  page = ref(1),
  size = ref(20),
  total = ref(0),
  busy = ref(false),
  saving = ref(false),
  error = ref(""),
  actions = ref(null),
  picker = ref(false),
  history = ref(false),
  historyRows = ref([]),
  historyPage = ref(1),
  historyTotal = ref(0),
  deleteModal = ref(false),
  handling = ref("");
const weekStart = computed(() => monday(selected.value)),
  past = computed(() =>
    props.daily
      ? selected.value < today()
      : addDays(weekStart.value, 6) < today(),
  ),
  weekDays = computed(() =>
    Array.from({ length: 7 }, (_, i) => ({
      date: addDays(weekStart.value, i),
      name: ["周一", "周二", "周三", "周四", "周五", "周六", "周日"][i],
    })),
  );
let sequence = 0;
async function load() {
  const seq = ++sequence;
  busy.value = true;
  error.value = "";
  try {
    const targetWeek = await api(`/weekly-plans/by-week/${weekStart.value}`);
    if (seq !== sequence) return;
    week.value = targetWeek;
    if (!week.value) {
      plan.value = null;
      days.value = [];
      rows.value = [];
      total.value = 0;
      return;
    }
    const target = props.daily
      ? await api(
          `/weekly-plans/${week.value.id}/daily-plans/${selected.value}`,
        )
      : week.value;
    if (seq !== sequence) return;
    plan.value = target;
    if (!props.daily && canGrow("planning:daily:list"))
      days.value = (
        await query(`/weekly-plans/${week.value.id}/daily-plans`, { size: 100 })
      ).items;
    if (!target) {
      rows.value = [];
      total.value = 0;
      return;
    }
    const data = await query(
      `/${props.daily ? "daily-plans" : "weekly-plans"}/${target.id}/tasks`,
      { page: page.value, size: size.value },
    );
    if (seq !== sequence) return;
    rows.value = data.items;
    total.value = data.total;
  } catch (e) {
    if (seq === sequence) error.value = e.message;
  } finally {
    if (seq === sequence) busy.value = false;
  }
}
async function create(kind) {
  saving.value = true;
  error.value = "";
  try {
    if (kind === "week")
      await api(`/weekly-plans/by-week/${weekStart.value}`, { method: "PUT" });
    else
      await api(
        `/weekly-plans/${week.value.id}/daily-plans/${selected.value}`,
        { method: "PUT" },
      );
    await load();
  } catch (e) {
    error.value = e.message;
  } finally {
    saving.value = false;
  }
}
function changeDate(value) {
  if (!validDate(value)) return;
  plan.value = null;
  rows.value = [];
  total.value = 0;
  deleteModal.value = false;
  picker.value = false;
  selected.value = props.daily ? value : monday(value);
  page.value = 1;
  load();
}
async function loadHistory() {
  try {
    const data = await query("/weekly-plans", {
      page: historyPage.value,
      size: 20,
    });
    historyRows.value = data.items;
    historyTotal.value = data.total;
    history.value = true;
  } catch (e) {
    report(e);
  }
}
function pickTask(row) {
  picker.value = false;
  actions.value.assign(row, {
    weekStart: weekStart.value,
    planDate: props.daily ? selected.value : "",
  });
}
async function removePlan() {
  if (!handling.value) return;
  saving.value = true;
  try {
    await api(
      `/${props.daily ? "daily-plans" : "weekly-plans"}/${plan.value.id}?` +
        new URLSearchParams({
          version: plan.value.version,
          handling: handling.value,
        }),
      { method: "DELETE" },
    );
    deleteModal.value = false;
    await load();
  } catch (e) {
    error.value = e.message;
  } finally {
    saving.value = false;
  }
}
function openDay(date) {
  router.push({ path: "/planning/daily-plans", query: { date } });
}
watch(
  () => [route.query.date, route.query.week],
  () => {
    const date = route.query.date || route.query.week;
    if (validDate(date)) {
      selected.value = date;
      page.value = 1;
      load();
    }
  },
);
onMounted(() => {
  if (!props.daily) selected.value = monday(selected.value);
  load();
});
</script>
<template>
  <section class="card">
    <div class="plan-heading">
      <div>
        <span class="eyebrow">{{ daily ? "DAILY PLAN" : "WEEKLY PLAN" }}</span>
        <h2>
          {{ daily ? selected : weekStart + " 至 " + addDays(weekStart, 6) }}
        </h2>
        <p class="muted">
          {{
            daily
              ? "把周计划落实到今天的行动。"
              : "同一周可以安排来自不同目标和阶段的任务。"
          }}
        </p>
      </div>
      <div class="toolbar">
        <el-button @click="changeDate(addDays(selected, daily ? -1 : -7))"
          >上一{{ daily ? "天" : "周" }}</el-button
        ><el-date-picker
          :model-value="selected"
          @update:model-value="changeDate"
          value-format="YYYY-MM-DD"
          :clearable="false"
        /><el-button @click="changeDate(addDays(selected, daily ? 1 : 7))"
          >下一{{ daily ? "天" : "周" }}</el-button
        ><el-button @click="changeDate(today())">{{
          daily ? "今天" : "本周"
        }}</el-button
        ><el-button
          v-if="!daily && canGrow('planning:weekly:list')"
          @click="loadHistory"
          >历史周计划</el-button
        >
      </div>
    </div>
    <div v-if="!daily" class="week-strip">
      <div
        v-for="d in weekDays"
        :key="d.date"
        class="day-tile"
        :class="{ today: d.date === today() }"
      >
        <button
          class="day-tile-open"
          @click="openDay(d.date)"
          :disabled="!canGrow('planning:daily:read')"
          :aria-label="'查看' + d.date + '日计划'"
        >
          <span>{{ d.name }}{{ d.date === today() ? " · 今天" : "" }}</span
          ><strong>{{ d.date.slice(5) }}</strong
          ><small>{{
            days.some((x) => x.planDate === d.date)
              ? "已创建日计划"
              : "按需安排"
          }}</small>
        </button>
        <div
          v-if="rows.some((r) => r.planDate === d.date)"
          class="week-task-list"
        >
          <button
            v-for="task in rows.filter((r) => r.planDate === d.date)"
            :key="task.id"
            class="week-task-chip"
            :disabled="!canGrow('task:task:read')"
            @click="actions.details('task', task)"
          >
            <strong>{{ task.taskName || task.name }}</strong
            ><small
              >{{ task.goalName || "所属目标" }} /
              {{ task.stageName || "所属阶段" }}</small
            ><AppStatusTag :value="task.status" />
          </button>
        </div>
      </div>
    </div>
    <p v-if="!daily && plan" class="hint">
      周视图按当前页安排预览；全部任务及翻页操作见下方列表，未定日任务独立保留。
    </p>
    <div
      v-if="!daily && rows.some((r) => !r.dailyPlanId && !r.planDate)"
      class="inset-metric week-undated"
    >
      <div class="plan-list-heading">
        <AppIcon name="inbox" />
        <h3>本页未定日任务</h3>
      </div>
      <div class="toolbar">
        <el-button
          v-for="task in rows.filter((r) => !r.dailyPlanId && !r.planDate)"
          :key="task.id"
          :disabled="!canGrow('task:task:read')"
          @click="actions.details('task', task)"
          >{{ task.taskName || task.name }}</el-button
        >
      </div>
    </div>
    <el-alert
      v-if="past"
      title="过去的计划只读；未完成任务可通过明确的改期操作安排到当前或未来日期。"
      type="info"
      :closable="false"
    /><el-alert
      v-if="error"
      :title="error"
      type="error"
      :closable="false"
    /><el-skeleton v-if="busy && !plan" :rows="5" /><AppEmptyState
      v-else-if="!plan"
      :description="!week ? '该周尚未创建周计划' : '该日期尚未创建日计划'"
      ><el-button
        v-if="!week && canGrow('planning:weekly:manage')"
        type="primary"
        :disabled="addDays(weekStart, 6) < today()"
        :loading="saving"
        @click="create('week')"
        >创建该周计划</el-button
      ><el-button
        v-else-if="daily && week && canGrow('planning:daily:manage')"
        type="primary"
        :disabled="past"
        :loading="saving"
        @click="create('day')"
        >创建该日日计划</el-button
      ></AppEmptyState
    ><template v-if="plan"
      ><div class="toolbar">
        <el-button
          v-if="
            canGrow('task:task:list') &&
            canGrow('planning:assignment:manage') &&
            canGrow('planning:assignment:read')
          "
          type="primary"
          :disabled="past"
          @click="picker = true"
          >从任务池加入</el-button
        ><el-button
          v-if="
            canGrow(daily ? 'planning:daily:manage' : 'planning:weekly:manage')
          "
          type="danger"
          plain
          :disabled="past"
          @click="
            handling = '';
            deleteModal = true;
          "
          >删除{{ daily ? "日" : "周" }}计划</el-button
        ><el-button @click="load">刷新列表</el-button>
      </div>
      <GrowthTaskTable
        :read-only="past"
        :rows="rows"
        :loading="busy"
        @detail="(r) => actions.details('task', r)"
        @edit="(r) => actions.edit('task', r)"
        @status="(r) => actions.status('task', r)"
        @remove="(r) => actions.remove('task', r)"
        @assign="
          (r) =>
            actions.assign(r, { weekStart, planDate: daily ? selected : '' })
        "
        @unassign="(r) => actions.unassign(r)"
        @history="(r) => actions.history(r)" /><el-pagination
        class="pagination"
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total,sizes,prev,pager,next"
        @change="load"
    /></template>
  </section>
  <TaskPicker
    v-if="picker"
    @close="picker = false"
    @selected="pickTask"
  /><GrowthActions ref="actions" @changed="load" /><el-drawer
    v-model="history"
    title="历史周计划"
    size="min(520px,100%)"
    ><el-table :data="historyRows"
      ><template #empty
        ><AppEmptyState
          :description="
            error
              ? '加载失败，请查看提示后重试。'
              : '暂无记录，可调整筛选条件或从第一条记录开始。'
          " /></template
      ><el-table-column prop="weekStartDate" label="周一日期" /><el-table-column
        label="操作"
        ><template #default="{ row }"
          ><el-button
            link
            type="primary"
            @click="
              history = false;
              changeDate(row.weekStartDate);
            "
            >查看该周</el-button
          ></template
        ></el-table-column
      ></el-table
    ><el-pagination
      v-model:current-page="historyPage"
      :page-size="20"
      :total="historyTotal"
      layout="total,prev,pager,next"
      @current-change="loadHistory" /></el-drawer
  ><el-dialog
    v-model="deleteModal"
    :title="'删除' + (daily ? '日' : '周') + '计划'"
    width="min(500px,94vw)"
    :close-on-click-modal="false"
    ><el-alert
      v-if="error"
      :title="error"
      type="error"
      :closable="false"
    /><el-alert
      title="不会删除任务，安排历史会保留。请选择当前任务安排的处理方式。"
      type="warning"
      :closable="false"
    /><el-radio-group v-model="handling" class="radio-stack"
      ><el-radio value="RETURN_TO_POOL">结束安排并退回任务池</el-radio
      ><el-radio v-if="daily" value="KEEP_IN_WEEK"
        >退回本周未指定日期队列</el-radio
      ></el-radio-group
    >
    <p v-if="!daily" class="hint">
      同时处理有效日计划；若包含过去日计划或归档目标的安排，系统会拒绝普通删除。
    </p>
    <template #footer
      ><el-button @click="deleteModal = false">取消</el-button
      ><el-button
        type="danger"
        @click="removePlan"
        :loading="saving"
        :disabled="!handling"
        >确认删除</el-button
      ></template
    ></el-dialog
  >
</template>
