<script setup>
defineProps({
  loading: Boolean,
  error: String,
  rows: { type: Number, default: 5 },
});
defineEmits(["retry"]);
</script>
<template>
  <div v-if="error" class="inline-error" role="alert">
    <span>{{ error }}</span
    ><el-button text type="primary" @click="$emit('retry')">重新加载</el-button>
  </div>
  <el-skeleton v-if="loading" :rows="rows" animated />
  <slot v-else />
</template>
