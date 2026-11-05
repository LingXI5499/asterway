<script setup>
// 从当前可安排任务池选择已有任务，不新建或复制任务。
import { ref, onMounted } from "vue";
import { query, priorities } from "../../api/growth";
const emit = defineEmits(["close", "selected"]);
const rows = ref([]),
  keyword = ref(""),
  page = ref(1),
  total = ref(0),
  busy = ref(false),
  error = ref("");
async function load() {
  busy.value = true;
  try {
    const d = await query("/tasks", {
      assignmentStatus: "POOL",
      keyword: keyword.value,
      page: page.value,
      size: 20,
    });
    rows.value = d.items;
    total.value = d.total;
    error.value = "";
  } catch (e) {
    error.value = e.message;
  } finally {
    busy.value = false;
  }
}
onMounted(load);
</script>
<template>
  <el-dialog
    :model-value="true"
    @close="emit('close')"
    title="从任务池选择任务"
    width="min(800px,94vw)"
    ><div class="toolbar">
      <el-input
        v-model="keyword"
        clearable
        placeholder="搜索任务"
        @keyup.enter="
          page = 1;
          load();
        "
      /><el-button
        type="primary"
        @click="
          page = 1;
          load();
        "
        >查询</el-button
      >
    </div>
    <el-alert
      v-if="error"
      :title="error"
      type="error"
      :closable="false" /><el-table :data="rows" v-loading="busy"
      ><el-table-column
        prop="name"
        label="任务"
        min-width="180"
      /><el-table-column prop="goalName" label="目标" /><el-table-column
        prop="stageName"
        label="阶段"
      /><el-table-column label="优先级"
        ><template #default="{ row }">{{
          priorities[row.priority]
        }}</template></el-table-column
      ><el-table-column label="操作"
        ><template #default="{ row }"
          ><el-button link type="primary" @click="emit('selected', row)"
            >选择</el-button
          ></template
        ></el-table-column
      ></el-table
    ><el-pagination
      class="pagination"
      v-model:current-page="page"
      :page-size="20"
      :total="total"
      layout="total,prev,pager,next"
      @current-change="load"
  /></el-dialog>
</template>
