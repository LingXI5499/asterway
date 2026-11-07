<script setup>
// 同一父资源下的当前页排序，批量提交每条记录的版本。
import { ref, onMounted } from "vue";
import { api } from "../../api/http";
import { query, resources } from "../../api/growth";
const props = defineProps({ kind: String, parentId: [String, Number] });
const emit = defineEmits(["close", "saved"]);
const rows = ref([]),
  page = ref(1),
  total = ref(0),
  busy = ref(false),
  error = ref("");
async function load() {
  busy.value = true;
  try {
    const path =
      props.kind === "stage" ? `/goals/${props.parentId}/stages` : "/tasks";
    const data = await query(path, {
      stageId: props.kind === "task" ? props.parentId : null,
      page: page.value,
      size: 100,
    });
    rows.value = data.items;
    total.value = data.total;
    error.value = "";
  } catch (e) {
    error.value = e.message;
  } finally {
    busy.value = false;
  }
}
function move(index, delta) {
  const target = index + delta;
  if (target < 0 || target >= rows.value.length) return;
  [rows.value[index], rows.value[target]] = [
    rows.value[target],
    rows.value[index],
  ];
  rows.value.forEach((r, i) => (r.sortOrder = (page.value - 1) * 100 + i));
}
async function save() {
  busy.value = true;
  try {
    const parent = props.kind === "stage" ? "goals" : "stages";
    await api(`/${parent}/${props.parentId}/${resources[props.kind]}/order`, {
      method: "PUT",
      body: {
        items: rows.value.map((r) => ({
          id: r.id,
          version: r.version,
          sortOrder: r.sortOrder,
        })),
      },
    });
    emit("saved");
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
    title="调整顺序"
    width="min(640px,94vw)"
    :close-on-click-modal="false"
    ><el-alert
      title="本次保存只修改当前页排序，未加载的记录保持原值。排序值越小越靠前。"
      type="info"
      :closable="false"
    /><el-alert
      v-if="error"
      :title="error"
      type="error"
      :closable="false"
    /><el-table :data="rows" v-loading="busy"
      ><template #empty
        ><AppEmptyState
          :description="
            error
              ? '加载失败，请查看提示后重试。'
              : '暂无记录，可调整筛选条件或从第一条记录开始。'
          " /></template
      ><el-table-column prop="name" label="名称" /><el-table-column
        label="排序值"
        width="180"
        ><template #default="{ row }"
          ><el-input-number
            v-model="row.sortOrder"
            :min="0"
            :max="2147483647"
            :precision="0"
            size="small" /></template></el-table-column
      ><el-table-column label="调整" width="120"
        ><template #default="{ $index }"
          ><el-button text :disabled="$index === 0" @click="move($index, -1)"
            >上移</el-button
          ><el-button
            text
            :disabled="$index === rows.length - 1"
            @click="move($index, 1)"
            >下移</el-button
          ></template
        ></el-table-column
      ></el-table
    ><el-pagination
      v-model:current-page="page"
      :page-size="100"
      :total="total"
      layout="total,prev,pager,next"
      @current-change="load"
    /><template #footer
      ><el-button @click="emit('close')">取消</el-button
      ><el-button
        type="primary"
        @click="save"
        :disabled="!rows.length"
        :loading="busy"
        >保存当前页</el-button
      ></template
    ></el-dialog
  >
</template>
