<!-- 四种任务来源使用各自的服务端分页，在一个弹窗内保留完整管理功能。 -->
<script setup>
import { ref, computed, watch, onBeforeUnmount } from "vue";
import { api } from "../../api/http";
import { query, canGrow, states, priorities, positions, today } from "../../api/growth";
import { pastAssignment } from "../../api/planning";
import GrowthSelector from "./GrowthSelector.vue";
import PlanTaskCard from "./PlanTaskCard.vue";

const props = defineProps({ weekStart: String, initialTab: { type: String, default: "pool" } });
const emit = defineEmits(["close", "tab", "detail", "edit", "status", "remove", "assign", "unassign", "history", "sort", "changed", "create"]);
const tabs = computed(() => [
  { name: "pool", label: "任务池", permission: "task:task:list" },
  { name: "week", label: "本周任务", permission: "planning:weekly:read" },
  { name: "all", label: "全部任务", permission: "task:task:list" },
  { name: "overdue", label: "逾期待处理", permission: "planning:pending:list" },
].filter(t => canGrow(t.permission)));
const tab = ref(""), filters = ref(emptyFilters()), range = ref([]), undated = ref(false);
const rows = ref([]), total = ref(0), page = ref(1), size = ref(20), busy = ref(false), error = ref("");
let sequence = 0, controller;
function emptyFilters() { return { goalId: "", stageId: "", keyword: "", status: "", priority: "", assignmentStatus: "" }; }
function search() { page.value = 1; load(); }
function reset() { filters.value = emptyFilters(); range.value = []; undated.value = false; search(); }

async function load() {
  const seq = ++sequence;
  controller?.abort(); controller = new AbortController();
  const options = { signal: controller.signal };
  busy.value = true; error.value = "";
  try {
    if (!tab.value || !tabs.value.some(t => t.name === tab.value)) { rows.value = []; total.value = 0; return; }
    const params = { ...filters.value, page: page.value, size: size.value };
    let data;
    if (tab.value === "week") {
      const week = await api(`/weekly-plans/by-week/${props.weekStart}`, options);
      if (seq !== sequence) return;
      data = week ? await query(`/weekly-plans/${week.id}/tasks`, {
        ...params, assignmentStatus: undated.value ? "WEEK" : "",
      }, options) : { items: [], total: 0 };
    } else if (tab.value === "overdue") {
      data = await query("/planning/pending-tasks", {
        ...params, dateFrom: range.value?.[0], dateTo: range.value?.[1],
      }, options);
    } else {
      data = await query("/tasks", { ...params, assignmentStatus: tab.value === "pool" ? "POOL" : filters.value.assignmentStatus }, options);
    }
    if (seq !== sequence) return;
    const lastPage = Math.max(1, Math.ceil(data.total / size.value));
    if (page.value > lastPage) { page.value = lastPage; return load(); }
    rows.value = data.items; total.value = data.total;
  } catch (e) {
    if (seq === sequence && e.name !== "AbortError") { rows.value = []; total.value = 0; error.value = e.message; }
  } finally { if (seq === sequence) busy.value = false; }
}

// 弹窗入口可以直接定位到旧任务管理、逾期待处理或本周任务。
watch(() => [props.initialTab, tabs.value.map(t => t.name).join(",")], () => {
  const target = tabs.value.some(t => t.name === props.initialTab) ? props.initialTab : tabs.value[0]?.name || "";
  if (tab.value !== target) tab.value = target;
}, { immediate: true });
watch(tab, () => { page.value = 1; filters.value = emptyFilters(); range.value = []; undated.value = false; emit("tab", tab.value); load(); }, { immediate: true });
watch(() => props.weekStart, () => { if (tab.value === "week") search(); });
onBeforeUnmount(() => { sequence++; controller?.abort(); });
defineExpose({ load });
</script>

<template>
  <el-dialog :model-value="true" title="任务管理" width="min(1120px,96vw)" align-center append-to-body class="plan-dialog plan-manager" @close="emit('close')">
    <div class="plan-manager-heading">
      <p class="muted">任务池存放尚未安排的任务；逾期任务可在这里重新安排。</p>
      <el-button v-if="canGrow('task:task:create')" type="primary" @click="emit('create')">新建任务</el-button>
    </div>
    <el-tabs v-model="tab"><el-tab-pane v-for="item in tabs" :key="item.name" :label="item.label" :name="item.name" /></el-tabs>
    <form v-if="tabs.length" class="plan-manager-filters" @submit.prevent="search">
      <GrowthSelector v-model:goal-id="filters.goalId" v-model:stage-id="filters.stageId" />
      <el-input v-model="filters.keyword" clearable placeholder="搜索任务名称" aria-label="搜索任务名称" />
      <el-select v-model="filters.status" clearable placeholder="全部状态" aria-label="任务状态"><el-option v-for="(label, code) in states" :key="code" :label="label" :value="code" :disabled="tab === 'overdue' && code === 'COMPLETED'" /></el-select>
      <el-select v-if="tab !== 'overdue'" v-model="filters.priority" clearable placeholder="全部优先级" aria-label="任务优先级"><el-option v-for="(label, code) in priorities" :key="code" :label="label" :value="code" /></el-select>
      <el-select v-if="tab === 'all'" v-model="filters.assignmentStatus" clearable placeholder="全部安排位置" aria-label="安排位置"><el-option v-for="(label, code) in positions" :key="code" :label="label" :value="code" /></el-select>
      <el-checkbox v-if="tab === 'week'" v-model="undated">仅看未定日期</el-checkbox>
      <el-date-picker v-if="tab === 'overdue'" v-model="range" type="daterange" value-format="YYYY-MM-DD" start-placeholder="原安排开始日期" end-placeholder="原安排结束日期" />
      <div class="plan-filter-buttons"><el-button type="primary" native-type="submit">查询</el-button><el-button @click="reset">重置</el-button>
        <el-button v-if="canGrow('task:task:sort') && ['pool','all'].includes(tab)" :disabled="!filters.stageId" @click="emit('sort', filters.stageId)">阶段内排序</el-button>
      </div>
    </form>
    <p v-if="tab === 'week'" class="muted">{{ weekStart }} 当周{{ undated ? ' · 日期待定的任务' : ' · 全部任务' }}</p>
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <el-button v-if="error" @click="load">重新加载</el-button>
    <div v-if="busy" class="plan-task-grid" aria-label="正在加载任务"><el-skeleton v-for="n in 2" :key="n" class="plan-task-skeleton" :rows="4" animated /></div>
    <div v-else-if="!error && rows.length" class="plan-task-grid">
      <PlanTaskCard v-for="row in rows" :key="row.taskId || row.id" :row="row" :read-only="pastAssignment(row, today())"
        @detail="r => emit('detail', r)" @edit="r => emit('edit', r)" @status="r => emit('status', r)" @remove="r => emit('remove', r)"
        @assign="r => emit('assign', r)" @unassign="r => emit('unassign', r)" @history="r => emit('history', r)" @changed="load(); emit('changed')" />
    </div>
    <AppEmptyState v-else-if="!error" :description="tabs.length ? (tab === 'pool' ? '任务池里还没有任务，可以新建一个任务。' : '没有符合条件的任务。') : '当前账号暂无任务管理权限。'" />
    <el-pagination v-if="total > 0" class="pagination" v-model:current-page="page" v-model:page-size="size" :total="total" :page-sizes="[10,20,50,100]" layout="total,sizes,prev,pager,next" @change="load" />
    <template #footer><el-button @click="emit('close')">关闭</el-button></template>
  </el-dialog>
</template>
