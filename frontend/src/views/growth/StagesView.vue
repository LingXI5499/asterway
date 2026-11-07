<script setup>
import { ref, onMounted } from "vue";
import { query, getResource, states, canGrow } from "../../api/growth";
import GrowthSelector from "../../components/growth/GrowthSelector.vue";
import GrowthActions from "../../components/growth/GrowthActions.vue";
import ProgressValue from "../../components/growth/ProgressValue.vue";
const stageStates = Object.fromEntries(
  Object.entries(states).filter(([code]) => code !== "PAUSED"),
);
const goal = ref(""),
  keyword = ref(""),
  status = ref(""),
  rows = ref([]),
  page = ref(1),
  size = ref(20),
  total = ref(0),
  busy = ref(false),
  error = ref(""),
  actions = ref(null),
  archived = ref(false);
let sequence = 0;
async function load() {
  const seq = ++sequence;
  rows.value = [];
  total.value = 0;
  if (!goal.value) return;
  busy.value = true;
  try {
    const parent = await getResource("goal", goal.value);
    const data = await query(`/goals/${goal.value}/stages`, {
      page: page.value,
      size: size.value,
      keyword: keyword.value,
      status: status.value,
    });
    if (seq !== sequence) return;
    archived.value = !!parent.archivedAt;
    rows.value = data.items;
    total.value = data.total;
    error.value = "";
  } catch (e) {
    if (seq === sequence) error.value = e.message;
  } finally {
    if (seq === sequence) busy.value = false;
  }
}
function reset() {
  keyword.value = "";
  status.value = "";
  page.value = 1;
  load();
}
</script>
<template>
  <section class="card">
    <div class="toolbar">
      <GrowthSelector
        v-model:goal-id="goal"
        goal-only
        @update:goal-id="
          page = 1;
          $nextTick(load);
        "
      /><el-input
        v-model="keyword"
        clearable
        placeholder="阶段名称"
        @keyup.enter="
          page = 1;
          load();
        "
      /><el-select v-model="status" clearable placeholder="全部状态"
        ><el-option
          v-for="(label, code) in stageStates"
          :key="code"
          :label="label"
          :value="code" /></el-select
      ><el-button
        type="primary"
        @click="
          page = 1;
          load();
        "
        >查询</el-button
      ><el-button @click="reset">重置</el-button
      ><el-button
        v-if="canGrow('stage:stage:create')"
        :disabled="!goal || archived"
        type="primary"
        plain
        @click="actions.edit('stage', null, goal)"
        >新增阶段</el-button
      ><el-button
        v-if="canGrow('stage:stage:sort')"
        :disabled="!goal || archived"
        @click="actions.sort('stage', goal)"
        >调整顺序</el-button
      >
    </div>
    <el-alert
      v-if="archived"
      title="来源目标已归档，阶段只读"
      type="info"
      :closable="false"
    /><el-alert
      v-if="error"
      :title="error"
      type="error"
      :closable="false"
    /><AppEmptyState
      v-if="!goal"
      description="请先选择目标，查看其阶段"
    /><template v-else
      ><el-table :data="rows" v-loading="busy" empty-text="该目标暂无阶段"
        ><template #empty
          ><AppEmptyState
            :description="
              error ? '加载失败，请查看提示后重试。' : '该目标暂无阶段'
            " /></template
        ><el-table-column label="阶段" min-width="200"
          ><template #default="{ row }"
            ><el-button
              v-if="canGrow('stage:stage:read')"
              link
              type="primary"
              @click="actions.details('stage', row)"
              >{{ row.name }}</el-button
            ><span v-else>{{ row.name }}</span></template
          ></el-table-column
        ><el-table-column label="加权进度" min-width="200"
          ><template #default="{ row }"
            ><ProgressValue :value="row.progress" /></template></el-table-column
        ><el-table-column label="状态"
          ><template #default="{ row }"
            ><AppStatusTag :value="row.status" /></template></el-table-column
        ><el-table-column
          prop="startDate"
          label="开始日期"
          min-width="120"
        /><el-table-column
          prop="endDate"
          label="结束日期"
          min-width="120"
        /><el-table-column
          prop="sortOrder"
          label="排序"
          width="80"
        /><el-table-column label="操作" width="220" fixed="right"
          ><template #default="{ row }"
            ><el-button
              v-if="canGrow('stage:stage:update')"
              :disabled="archived"
              link
              type="primary"
              @click="actions.edit('stage', row)"
              >编辑</el-button
            ><el-button
              v-if="canGrow('stage:stage:status')"
              :disabled="archived"
              link
              type="primary"
              @click="actions.status('stage', row)"
              >状态</el-button
            ><el-button
              v-if="canGrow('stage:stage:delete')"
              :disabled="archived"
              link
              type="danger"
              @click="actions.remove('stage', row)"
              >删除</el-button
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
    /></template>
  </section>
  <GrowthActions ref="actions" @changed="load" />
</template>
