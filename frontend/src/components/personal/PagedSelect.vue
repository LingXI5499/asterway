<script setup>
// 远程选项按页加载，不以 100 条截断用户的分类或业务对象。
import { ref, watch, onMounted, onBeforeUnmount } from "vue";
const props = defineProps({
  modelValue: [String, Number],
  label: String,
  placeholder: String,
  loader: Function,
  resourceKey: [String, Number],
  disabled: Boolean,
  selectedDisabled: Boolean,
});
const emit = defineEmits(["update:modelValue"]);
const options = ref([]),
  total = ref(0),
  page = ref(1),
  keyword = ref(""),
  busy = ref(false),
  error = ref("");
let sequence = 0,
  disposed = false,
  controller;
async function search(value = "", more = false) {
  if (props.disabled || !props.loader || disposed) return;
  if (!more) {
    keyword.value = value;
    page.value = 1;
  }
  const seq = ++sequence;
  controller?.abort();
  controller = new AbortController();
  busy.value = true;
  error.value = "";
  try {
    const data = await props.loader(
      { keyword: keyword.value, page: page.value, size: 20 },
      controller.signal,
    );
    if (seq !== sequence || disposed) return;
    const merged = more ? [...options.value, ...data.items] : data.items;
    options.value = [
      ...new Map(merged.map((item) => [String(item.id), item])).values(),
    ];
    total.value = data.total;
  } catch (e) {
    if (seq === sequence && !disposed && e.name !== "AbortError")
      error.value = e.message;
  } finally {
    if (seq === sequence && !disposed) busy.value = false;
  }
}
async function more() {
  if (busy.value) return;
  page.value++;
  await search(keyword.value, true);
}
watch(
  () => props.resourceKey,
  () => {
    options.value = [];
    total.value = 0;
    search();
  },
);
watch(
  () => props.disabled,
  (disabled) => {
    if (!disabled) search();
  },
);
onMounted(() => search());
onBeforeUnmount(() => {
  disposed = true;
  sequence++;
  controller?.abort();
});
</script>
<template>
  <div class="paged-select">
    <el-select
      :model-value="modelValue || ''"
      @update:model-value="emit('update:modelValue', $event || '')"
      :placeholder="placeholder"
      :disabled="disabled"
      filterable
      remote
      clearable
      :remote-method="search"
      :loading="busy"
    >
      <el-option
        v-if="
          modelValue &&
          !options.some((item) => String(item.id) === String(modelValue))
        "
        :value="String(modelValue)"
        :label="label || '已选择 · ' + modelValue"
        :disabled="selectedDisabled"
      />
      <el-option
        v-for="item in options"
        :key="item.id"
        :value="String(item.id)"
        :label="item.name || item.taskName"
        :disabled="selectedDisabled && String(item.id) === String(modelValue)"
      />
      <template #footer>
        <div class="select-footer">
          <span v-if="error" role="alert">{{ error }}</span
          ><span v-else>已加载 {{ options.length }} / {{ total }}</span>
          <el-button v-if="error" text @click.stop="search(keyword)"
            >重试</el-button
          >
          <el-button
            v-else-if="options.length < total"
            text
            type="primary"
            :loading="busy"
            @click.stop="more"
            >加载更多</el-button
          >
        </div>
      </template>
    </el-select>
  </div>
</template>
