<script setup>
import { ref, reactive, computed, watch, onBeforeUnmount } from "vue";
import {
  stateHistory,
  instantText,
  actions,
  sources,
} from "../../api/personal";
import { states } from "../../api/growth";
import PagePagination from "./PagePagination.vue";
import LoadState from "./LoadState.vue";
const props = defineProps({
  modelValue: Boolean,
  kind: String,
  id: [String, Number],
});
const emit = defineEmits(["update:modelValue"]);
const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit("update:modelValue", v),
});
const list = reactive({ rows: [], total: 0, page: 1, size: 20 }),
  busy = ref(false),
  error = ref("");
let sequence = 0,
  controller;
async function load() {
  if (!props.modelValue || !props.id) return;
  const seq = ++sequence;
  controller?.abort();
  controller = new AbortController();
  busy.value = true;
  error.value = "";
  try {
    const data = await stateHistory(
      props.kind,
      props.id,
      { page: list.page, size: list.size },
      controller.signal,
    );
    if (seq === sequence)
      Object.assign(list, { rows: data.items, total: data.total });
  } catch (e) {
    if (seq === sequence && e.name !== "AbortError") error.value = e.message;
  } finally {
    if (seq === sequence) busy.value = false;
  }
}
watch(
  () => [props.modelValue, props.kind, props.id],
  () => {
    sequence++;
    list.page = 1;
    list.rows = [];
    if (props.modelValue) load();
    else controller?.abort();
  },
  { immediate: true },
);
onBeforeUnmount(() => {
  sequence++;
  controller?.abort();
});
</script>
<template>
  <el-drawer
    v-model="visible"
    title="真实状态历史"
    size="min(800px, 100vw)"
    destroy-on-close
  >
    <LoadState
      :loading="busy && !list.rows.length"
      :error="error"
      @retry="load"
    >
      <el-timeline v-if="list.rows.length" class="fact-timeline"
        ><el-timeline-item
          v-for="row in list.rows"
          :key="row.id"
          :timestamp="instantText(row.occurredAt)"
          placement="top"
        >
          <article class="timeline-fact">
            <strong
              >{{ actions[row.eventType] || "状态事件" }} ·
              {{ row.nameSnapshot }}</strong
            >
            <p>
              {{ row.beforeStatus ? states[row.beforeStatus] + " → " : ""
              }}{{ states[row.afterStatus] }}
            </p>
            <p
              v-if="kind === 'goal' && row.beforeArchived !== null"
              class="hint"
            >
              归档状态：{{ row.beforeArchived ? "已归档" : "未归档" }} →
              {{ row.afterArchived ? "已归档" : "未归档" }}
            </p>
            <p class="hint">
              {{ sources[row.sourceType]
              }}<template v-if="row.eventType === 'BASELINE'">
                · 初始化基线，不计历史完成</template
              >
            </p>
          </article>
        </el-timeline-item></el-timeline
      ><AppEmptyState v-else description="还没有状态历史" />
      <PagePagination :state="list" @change="load" />
    </LoadState>
  </el-drawer>
</template>
