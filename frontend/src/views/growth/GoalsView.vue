<script setup>
import { ref, onMounted } from "vue";
import { query, states, canGrow } from "../../api/growth";
import GrowthActions from "../../components/growth/GrowthActions.vue";
import ProgressValue from "../../components/growth/ProgressValue.vue";
const rows = ref([]),
  total = ref(0),
  page = ref(1),
  size = ref(20),
  keyword = ref(""),
  mode = ref("CURRENT"),
  density = ref("cards"),
  range = ref([]),
  busy = ref(false),
  error = ref(""),
  actions = ref(null);
let sequence = 0;
async function load() {
  const seq = ++sequence;
  busy.value = true;
  error.value = "";
  try {
    const data = await query("/goals", {
      page: page.value,
      size: size.value,
      keyword: keyword.value,
      archived: mode.value === "ARCHIVED",
      timing: ["CURRENT", "FUTURE"].includes(mode.value) ? mode.value : "ALL",
      status: mode.value === "COMPLETED" ? "COMPLETED" : null,
      startFrom: range.value?.[0],
      startTo: range.value?.[1],
    });
    if (seq !== sequence) return;
    rows.value = data.items;
    total.value = data.total;
  } catch (e) {
    if (seq === sequence) error.value = e.message;
  } finally {
    if (seq === sequence) busy.value = false;
  }
}
function reset() {
  keyword.value = "";
  range.value = [];
  page.value = 1;
  load();
}
onMounted(load);
</script>
<template>
  <section class="card growth-hero">
    <div>
      <span class="eyebrow">GROWTH · GOALS</span>
      <h2>把想做的事，变成清晰的目标</h2>
      <p class="muted">从目标拆分阶段，再把任务安排到合适的一周与一天。</p>
    </div>
    <el-button
      v-if="canGrow('goal:goal:create')"
      type="primary"
      @click="actions.edit('goal')"
      >新增目标</el-button
    >
  </section>
  <section class="card">
    <el-tabs
      v-model="mode"
      @tab-change="
        page = 1;
        load();
      "
      ><el-tab-pane label="当前目标" name="CURRENT" /><el-tab-pane
        label="未来目标"
        name="FUTURE" /><el-tab-pane label="全部目标" name="ALL" /><el-tab-pane
        label="已完成"
        name="COMPLETED" /><el-tab-pane label="已归档" name="ARCHIVED"
    /></el-tabs>
    <div class="toolbar">
      <el-input
        v-model="keyword"
        clearable
        placeholder="搜索目标名称"
        @keyup.enter="
          page = 1;
          load();
        "
      /><el-date-picker
        v-model="range"
        type="daterange"
        value-format="YYYY-MM-DD"
        start-placeholder="开始日期下限"
        end-placeholder="开始日期上限"
      /><el-button
        type="primary"
        @click="
          page = 1;
          load();
        "
        >查询</el-button
      ><el-button @click="reset">重置</el-button>
      <div class="view-density" role="group" aria-label="目标展示方式">
        <el-button
          text
          :type="density === 'cards' ? 'primary' : ''"
          :aria-pressed="density === 'cards'"
          aria-label="卡片视图"
          title="卡片视图"
          @click="density = 'cards'"
          ><AppIcon name="grid"
        /></el-button>
        <el-button
          text
          :type="density === 'list' ? 'primary' : ''"
          :aria-pressed="density === 'list'"
          aria-label="列表视图"
          title="列表视图"
          @click="density = 'list'"
          ><AppIcon name="list"
        /></el-button>
      </div>
    </div>
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <div v-if="density === 'cards'" v-loading="busy" class="goal-grid">
      <article v-for="row in rows" :key="row.id" class="goal-card">
        <div class="goal-card-heading">
          <el-button
            v-if="canGrow('goal:goal:read')"
            link
            type="primary"
            @click="actions.details('goal', row)"
            >{{ row.name }}</el-button
          ><strong v-else>{{ row.name }}</strong>
          <AppStatusTag :value="row.status" :archived="!!row.archivedAt" />
        </div>
        <div class="goal-card-dates">
          <AppIcon name="day" :size="14" /><span>{{
            row.startDate || "未设开始日"
          }}</span
          ><span>至</span
          ><span>{{ row.expectedEndDate || "未设预计结束" }}</span>
        </div>
        <ProgressValue :value="row.progress" />
        <div class="toolbar">
          <el-button
            v-if="canGrow('goal:goal:read')"
            link
            type="primary"
            @click="actions.details('goal', row)"
            >查看详情<AppIcon name="arrow" :size="14"
          /></el-button>
          <template v-if="!row.archivedAt">
            <el-button
              v-if="canGrow('goal:goal:update')"
              link
              @click="actions.edit('goal', row)"
              >编辑</el-button
            >
            <el-button
              v-if="canGrow('goal:goal:status')"
              link
              @click="actions.status('goal', row)"
              >状态</el-button
            >
            <el-button
              v-if="canGrow('goal:goal:archive')"
              link
              type="warning"
              @click="actions.archive(row)"
              >归档</el-button
            >
          </template>
          <el-button
            v-else-if="canGrow('goal:goal:restore')"
            link
            type="primary"
            @click="actions.restore(row)"
            >恢复</el-button
          >
        </div>
      </article>
    </div>
    <AppEmptyState
      v-if="density === 'cards' && !rows.length && !busy && !error"
      :title="
        keyword || range?.length ? '没有匹配的目标' : '给下一步成长一个方向'
      "
      description="从一个目标开始，再拆分成清晰的阶段与任务。"
      icon="goal"
    >
      <el-button
        v-if="canGrow('goal:goal:create')"
        type="primary"
        @click="actions.edit('goal')"
        >创建目标</el-button
      >
      <el-button v-if="keyword || range?.length" @click="reset"
        >清空筛选</el-button
      >
    </AppEmptyState>
    <el-table
      v-else-if="density === 'list'"
      :data="rows"
      v-loading="busy"
      empty-text="暂无目标，可以先创建一个目标"
      ><template #empty
        ><AppEmptyState
          :description="
            error
              ? '加载失败，请查看提示后重试。'
              : '暂无目标，可以先创建一个目标'
          " /></template
      ><el-table-column prop="name" label="目标" min-width="200"
        ><template #default="{ row }"
          ><el-button
            v-if="canGrow('goal:goal:read')"
            link
            type="primary"
            @click="actions.details('goal', row)"
            >{{ row.name }}</el-button
          ><span v-else>{{ row.name }}</span></template
        ></el-table-column
      ><el-table-column label="进度" min-width="200"
        ><template #default="{ row }"
          ><ProgressValue :value="row.progress" /></template></el-table-column
      ><el-table-column label="状态" width="100"
        ><template #default="{ row }"
          ><AppStatusTag :value="row.status" /></template></el-table-column
      ><el-table-column
        prop="startDate"
        label="开始日期"
        min-width="120"
      /><el-table-column
        prop="expectedEndDate"
        label="预计结束"
        min-width="120"
      /><el-table-column label="操作" width="290" fixed="right"
        ><template #default="{ row }"
          ><template v-if="!row.archivedAt"
            ><el-button
              v-if="canGrow('goal:goal:update')"
              link
              type="primary"
              @click="actions.edit('goal', row)"
              >编辑</el-button
            ><el-button
              v-if="canGrow('goal:goal:status')"
              link
              type="primary"
              @click="actions.status('goal', row)"
              >状态</el-button
            ><el-button
              v-if="canGrow('goal:goal:archive')"
              link
              type="warning"
              @click="actions.archive(row)"
              >归档</el-button
            ></template
          ><el-button
            v-else-if="canGrow('goal:goal:restore')"
            link
            type="primary"
            @click="actions.restore(row)"
            >恢复</el-button
          ></template
        ></el-table-column
      ></el-table
    ><el-pagination
      class="pagination"
      v-model:current-page="page"
      v-model:page-size="size"
      :total="total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total,sizes,prev,pager,next"
      @change="load"
    />
  </section>
  <GrowthActions ref="actions" @changed="load" />
</template>
