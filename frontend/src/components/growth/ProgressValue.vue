<script setup>
defineProps({ value: [Number, String] });
</script>
<template>
  <span v-if="value === null || value === undefined" class="muted"
    >暂无可计算进度</span
  ><el-progress
    v-else
    :percentage="Math.min(100, Math.max(0, Number(value)))"
    :stroke-width="8"
    :format="(p) => p + '%'"
  />
</template>
