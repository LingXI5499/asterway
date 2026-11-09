<!-- 一个计划页面负责日期导航和当天任务，完整管理操作在居中弹窗中完成。 -->
<script setup>
import { ref, computed, watch, nextTick, onMounted, onBeforeUnmount } from "vue";
import { useRoute, useRouter } from "vue-router";
import { api } from "../../api/http";
import { today, monday, addDays, validDate, canGrow, query } from "../../api/growth";
import { planningPath, weekLabels, monthDates, shiftMonth } from "../../api/planning";
import { usePlanTasks } from "../../composables/usePlanTasks";
import GrowthActions from "../../components/growth/GrowthActions.vue";
import PlanTaskCard from "../../components/growth/PlanTaskCard.vue";
import TaskManagerDialog from "../../components/growth/TaskManagerDialog.vue";
import TaskPicker from "../../components/growth/TaskPicker.vue";
import "../../styles/planning.css";

const route = useRoute(), router = useRouter();
const selected = ref(readDate()), view = ref(route.query.view === "month" ? "month" : "week");
const month = ref(validDate(String(route.query.month || "") + "-01") ? route.query.month + "-01" : selected.value.slice(0, 7) + "-01"), page = ref(1), size = ref(20);
const actions = ref(null), manager = ref(null), picker = ref(false);
const weekStrip = ref(null);
const managerTab = ref(typeof route.query.manage === "string" ? route.query.manage : "");
const { week, day, rows, total, busy, error, load } = usePlanTasks();
const weekStart = computed(() => monday(selected.value));
const dates = computed(() => Array.from({ length: 7 }, (_, i) => addDays(weekStart.value, i)));
const calendar = computed(() => monthDates(month.value));
const canReadDay = computed(() => canGrow("planning:weekly:read") && canGrow("planning:daily:read"));
const canManageTasks = computed(() => ["task:task:list", "planning:weekly:read", "planning:pending:list"].some(canGrow));
const canPick = computed(() => canGrow("task:task:list") && canGrow("planning:assignment:manage") && canGrow("planning:assignment:read"));
const past = computed(() => selected.value < today());
const title = computed(() => view.value === "month" ? `${month.value.slice(0,4)}年${Number(month.value.slice(5,7))}月` : `${weekStart.value} 至 ${addDays(weekStart.value, 6)}`);
const historyOpen = ref(false), historyRows = ref([]), historyPage = ref(1), historyTotal = ref(0), historyBusy = ref(false), historyError = ref("");
const clearing = ref(""), handling = ref("RETURN_TO_POOL"), saving = ref(false), clearError = ref("");
let historySequence = 0;

function readDate() { return validDate(route.query.date) ? route.query.date : validDate(route.query.week) ? route.query.week : today(); }
// 窄屏只滚动日期条，让选中的日期始终可见，同时保留整页的滚动位置。
function alignSelectedDay() {
  const strip = weekStrip.value, target = strip?.querySelector('[aria-pressed="true"]');
  if (!target || strip.scrollWidth <= strip.clientWidth) return;
  const left = target.getBoundingClientRect().left - strip.getBoundingClientRect().left + strip.scrollLeft - (strip.clientWidth - target.clientWidth) / 2;
  strip.scrollTo({ left, behavior: window.matchMedia("(prefers-reduced-motion: reduce)").matches ? "instant" : "smooth" });
}
const resized = () => nextTick(alignSelectedDay);
onMounted(() => window.addEventListener("resize", resized));
onBeforeUnmount(() => window.removeEventListener("resize", resized));
watch(() => [selected.value, view.value], resized, { immediate: true, flush: "post" });
// 查询参数保存浏览位置，页面组件保持同一个实例，不因日期变化重新挂载。
function updateQuery(values) {
  const next = { ...route.query, ...values };
  Object.keys(next).forEach(key => { if (next[key] === "" || next[key] == null) delete next[key]; });
  if (JSON.stringify(next) !== JSON.stringify(route.query)) router.replace({ path: planningPath, query: next });
}
function selectDate(date) {
  if (!validDate(date)) return;
  selected.value = date; month.value = date.slice(0,7) + "-01"; page.value = 1; view.value = "week";
  updateQuery({ date, view: "week", week: null, month: null });
}
function move(offset) {
  if (view.value === "month") { month.value = shiftMonth(month.value, offset); updateQuery({ month: month.value.slice(0,7) }); }
  else selectDate(addDays(selected.value, offset * 7));
}
function changeView(value) {
  view.value = value;
  if (value === "month") month.value = selected.value.slice(0,7) + "-01";
  updateQuery({ view: value, date: selected.value, month: value === "month" ? month.value.slice(0,7) : null });
}
function refresh() { return load(selected.value, page.value, size.value); }
function changed() { refresh(); manager.value?.load(); }
function openManager(tab = "pool") { managerTab.value = tab; updateQuery({ manage: tab }); }
function closeManager() { managerTab.value = ""; updateQuery({ manage: null }); }
function managerChanged(tab) { if (route.query.manage !== tab) updateQuery({ manage: tab }); }
function createTask() { actions.value.edit("task", null, null, { planDate: selected.value }); }
function addTask(command = "pool") {
  if (command === "new" || !canPick.value) createTask();
  else picker.value = true;
}
function pickTask(row) {
  picker.value = false;
  actions.value.assign(row, { weekStart: weekStart.value, planDate: selected.value });
}
function detailOpened(id) { if (route.query.task !== String(id)) updateQuery({ task: String(id) }); }
function detailClosed() { if (route.query.task) updateQuery({ task: null }); }

async function loadHistory() {
  const seq = ++historySequence;
  historyBusy.value = true; historyError.value = "";
  try {
    const data = await query("/weekly-plans", { page: historyPage.value, size: 20 });
    if (seq === historySequence) { historyRows.value = data.items; historyTotal.value = data.total; }
  } catch (e) { if (seq === historySequence) historyError.value = e.message; }
  finally { if (seq === historySequence) historyBusy.value = false; }
}
function more(command) {
  if (command === "history") { historyOpen.value = true; loadHistory(); }
  else if (command === "tasks") openManager("week");
  else { clearing.value = command; handling.value = "RETURN_TO_POOL"; clearError.value = ""; }
}
async function clearPlan() {
  if (saving.value) return;
  const target = clearing.value === "day" ? day.value : week.value;
  if (!target) return;
  saving.value = true; clearError.value = "";
  try {
    await api(`/${clearing.value === "day" ? "daily-plans" : "weekly-plans"}/${target.id}?` + new URLSearchParams({ version: target.version, handling: handling.value }), { method: "DELETE" });
    clearing.value = ""; changed();
  } catch (e) { clearError.value = e.message; }
  finally { saving.value = false; }
}

watch(() => [selected.value, page.value, size.value], refresh, { immediate: true });
watch(() => [total.value, busy.value], () => {
  const lastPage = Math.max(1, Math.ceil(total.value / size.value));
  if (!busy.value && !error.value && page.value > lastPage) page.value = lastPage;
});
watch(() => route.query.date, () => {
  const date = readDate();
  if (date !== selected.value) { selected.value = date; month.value = date.slice(0,7) + "-01"; page.value = 1; }
});
watch(() => route.query.view, value => { view.value = value === "month" ? "month" : "week"; });
watch(() => route.query.month, value => { if (validDate(String(value || "") + "-01")) month.value = value + "-01"; });
watch(() => route.query.manage, value => { managerTab.value = typeof value === "string" ? value : ""; });
watch(() => route.query.task, async value => {
  await nextTick();
  if (typeof value === "string" && /^\d+$/.test(value)) actions.value?.details("task", { id: value });
  else actions.value?.closeDetails();
}, { immediate: true });
</script>

<template>
  <div class="plans-workspace">
    <section class="card plan-calendar" aria-label="计划日历">
      <div class="plan-page-heading">
        <div><h1>计划</h1><p class="muted">选一天，看看要做些什么。</p></div>
        <div class="plan-header-actions">
          <el-radio-group :model-value="view" aria-label="日历视图" @update:model-value="changeView"><el-radio-button value="week">周</el-radio-button><el-radio-button value="month">月</el-radio-button></el-radio-group>
          <el-button @click="selectDate(today())">今天</el-button>
          <el-button v-if="canManageTasks" @click="openManager()">任务管理</el-button>
          <el-dropdown trigger="click" @command="more"><el-button aria-label="更多计划操作"><AppIcon name="more" :size="18" /></el-button>
            <template #dropdown><el-dropdown-menu>
              <el-dropdown-item v-if="canGrow('planning:weekly:read')" command="tasks">查看本周任务</el-dropdown-item>
              <el-dropdown-item v-if="canGrow('planning:weekly:list')" command="history">计划历史</el-dropdown-item>
              <el-dropdown-item v-if="canGrow('planning:daily:manage')" command="day" :disabled="busy || !day || past">清空当天安排</el-dropdown-item>
              <el-dropdown-item v-if="canGrow('planning:weekly:manage')" command="week" :disabled="busy || !week || addDays(weekStart,6) < today()">清空本周安排</el-dropdown-item>
            </el-dropdown-menu></template>
          </el-dropdown>
        </div>
      </div>
      <div class="plan-calendar-heading">
        <el-button :aria-label="view === 'month' ? '上个月' : '上一周'" @click="move(-1)"><AppIcon name="back" :size="18" /></el-button>
        <h3>{{ title }}</h3>
        <el-button :aria-label="view === 'month' ? '下个月' : '下一周'" @click="move(1)"><AppIcon name="arrow" :size="18" /></el-button>
        <el-date-picker :model-value="selected" type="date" value-format="YYYY-MM-DD" :clearable="false" aria-label="跳转到日期" @update:model-value="selectDate" />
      </div>
      <div v-if="view === 'week'" ref="weekStrip" class="plan-week-strip" aria-label="选择本周日期">
        <button v-for="(date,index) in dates" :key="date" type="button" class="plan-date" :class="{ selected: date === selected, today: date === today() }"
          :aria-label="date + ' ' + weekLabels[index]" :aria-pressed="date === selected" :aria-current="date === today() ? 'date' : undefined" @click="selectDate(date)">
          <span>{{ weekLabels[index] }}</span><strong>{{ Number(date.slice(8)) }}</strong><small>{{ date === today() ? '今天' : date === selected ? '已选' : date.slice(5,7) + '月' }}</small>
        </button>
      </div>
      <div v-else class="plan-month-calendar" aria-label="选择月份内的日期">
        <span v-for="label in weekLabels" :key="label" class="plan-week-label">{{ label }}</span>
        <button v-for="date in calendar" :key="date" type="button" class="plan-date" :class="{ selected: date === selected, today: date === today(), 'other-month': date.slice(0,7) !== month.slice(0,7), 'selected-week': monday(date) === weekStart }"
          :aria-label="date" :aria-pressed="date === selected" :aria-current="date === today() ? 'date' : undefined" @click="selectDate(date)">
          <strong>{{ Number(date.slice(8)) }}</strong><small>{{ date === today() ? '今天' : date === selected ? '已选' : '\u00a0' }}</small>
        </button>
      </div>
    </section>

    <section class="plan-day-section" aria-label="所选日期的任务" :aria-busy="busy">
      <div class="plan-day-heading">
        <div><h2>{{ selected }} <span>{{ weekLabels[dates.indexOf(selected)] }}</span></h2><p class="muted">{{ busy ? '正在读取当天安排' : total ? '共 ' + total + ' 项任务' : '把时间留给重要的事，也可以好好休息。' }}</p></div>
        <el-dropdown v-if="canPick || canGrow('task:task:create')" split-button type="primary" trigger="click" @click="addTask()" @command="addTask">
          添加任务<template #dropdown><el-dropdown-menu><el-dropdown-item v-if="canPick" command="pool">从任务池添加</el-dropdown-item><el-dropdown-item v-if="canGrow('task:task:create')" command="new">新建任务</el-dropdown-item></el-dropdown-menu></template>
        </el-dropdown>
      </div>
      <p v-if="past" class="plan-readonly">这一天已过去，可以查看记录，或通过改期重新安排未完成的任务。</p>
      <el-alert v-if="!canReadDay" title="当前账号没有查看当天安排的完整权限，仍可使用已授权的任务管理功能。" type="info" :closable="false" />
      <el-alert v-if="error" :title="error" type="error" :closable="false" />
      <el-button v-if="error" @click="refresh">重新加载</el-button>
      <div v-if="busy" class="plan-task-grid"><el-skeleton v-for="n in 2" :key="n" class="plan-task-skeleton" :rows="4" animated /></div>
      <div v-else-if="canReadDay && !error && rows.length" :key="selected" class="plan-task-grid plan-tasks-enter">
        <PlanTaskCard v-for="row in rows" :key="row.taskId" :row="row" :read-only="past"
          @detail="r => actions.details('task', r)" @edit="r => actions.edit('task', r)" @status="r => actions.status('task', r)" @remove="r => actions.remove('task', r)"
          @assign="r => actions.assign(r, { weekStart, planDate: selected })" @unassign="r => actions.unassign(r)" @history="r => actions.history(r)" @changed="changed" />
      </div>
      <div v-else-if="canReadDay && !error" class="plan-rest-state"><AppIcon name="day" :size="32" /><h3>当天没有安排，休息一下</h3><p>有新的想法，也可以从上方添加任务。</p></div>
      <el-pagination v-if="!error && total > 0" class="pagination" v-model:current-page="page" v-model:page-size="size" :total="total" :page-sizes="[10,20,50,100]" layout="total,sizes,prev,pager,next" />
    </section>

    <TaskManagerDialog v-if="managerTab" ref="manager" :week-start="weekStart" :initial-tab="managerTab" @close="closeManager" @tab="managerChanged"
      @detail="r => actions.details('task', r)" @edit="r => actions.edit('task', r)" @status="r => actions.status('task', r)" @remove="r => actions.remove('task', r)"
      @assign="r => actions.assign(r, { weekStart, planDate: selected })" @unassign="r => actions.unassign(r)" @history="r => actions.history(r)" @sort="id => actions.sort('task', id)"
      @create="actions.edit('task', null)" @changed="refresh" />
    <TaskPicker v-if="picker" @close="picker = false" @selected="pickTask" />
    <GrowthActions ref="actions" @changed="changed" @detail-open="detailOpened" @detail-close="detailClosed" />

    <el-dialog v-model="historyOpen" title="计划历史" width="min(680px,94vw)" align-center append-to-body class="plan-dialog" @closed="historySequence++">
      <el-alert v-if="historyError" :title="historyError" type="error" :closable="false" /><el-button v-if="historyError" @click="loadHistory">重新加载</el-button>
      <el-table :data="historyRows" v-loading="historyBusy"><el-table-column label="所在周"><template #default="{ row }">{{ row.weekStartDate }} 至 {{ addDays(row.weekStartDate,6) }}</template></el-table-column><el-table-column label="操作" width="100"><template #default="{ row }"><el-button link type="primary" @click="selectDate(row.weekStartDate); historyOpen = false">查看</el-button></template></el-table-column></el-table>
      <el-pagination class="pagination" v-model:current-page="historyPage" :total="historyTotal" :page-size="20" layout="total,prev,pager,next" @current-change="loadHistory" />
    </el-dialog>
    <el-dialog :model-value="!!clearing" :title="clearing === 'day' ? '清空当天安排' : '清空本周安排'" width="min(520px,94vw)" align-center append-to-body :close-on-click-modal="false" :show-close="!saving" :close-on-press-escape="!saving" @close="clearing = ''">
      <p>任务和历史记录会保留，请选择这些任务接下来放在哪里。</p>
      <el-alert v-if="clearError" :title="clearError" type="error" :closable="false" />
      <el-radio-group v-model="handling" :disabled="saving"><el-radio value="RETURN_TO_POOL">退回任务池</el-radio><el-radio v-if="clearing === 'day'" value="KEEP_IN_WEEK">留在本周，日期待定</el-radio></el-radio-group>
      <template #footer><el-button :disabled="saving" @click="clearing = ''">取消</el-button><el-button type="danger" :loading="saving" @click="clearPlan">确认清空安排</el-button></template>
    </el-dialog>
  </div>
</template>
