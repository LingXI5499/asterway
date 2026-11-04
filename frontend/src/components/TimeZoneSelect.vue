<script setup>
import { ref, onMounted } from "vue";
import { api } from "../api/http";
import { browserZone, report } from "../api/ui";
const props = defineProps({ modelValue: String });
const emit = defineEmits(["update:modelValue"]);
const zones = ref([]),
  busy = ref(false);
let sequence = 0;
async function search(keyword = "") {
  const turn = ++sequence;
  busy.value = true;
  try {
    const data = await api(
      "/time-zones?" + new URLSearchParams({ keyword, page: 1, size: 100 }),
    );
    if (turn === sequence) zones.value = data.items;
  } catch (e) {
    report(e);
  } finally {
    if (turn === sequence) busy.value = false;
  }
}
onMounted(search);
</script>
<template>
  <div class="zone-control">
    <el-select
      :model-value="props.modelValue"
      @update:model-value="emit('update:modelValue', $event)"
      filterable
      remote
      :remote-method="search"
      :loading="busy"
      placeholder="搜索 IANA 时区"
      ><el-option
        v-if="modelValue"
        :value="modelValue"
        :label="modelValue" /><el-option
        v-for="zone in zones.filter((z) => z !== modelValue)"
        :key="zone"
        :value="zone"
        :label="zone" /></el-select
    ><el-button @click="emit('update:modelValue', browserZone())"
      >识别设备时区</el-button
    >
  </div>
</template>
