<script setup>
defineProps({
  title: { type: String, default: "" },
  description: { type: String, default: "这里还没有记录" },
  icon: { type: String, default: "inbox" },
});
</script>
<template>
  <div class="aw-empty" role="status">
    <div class="aw-empty-icon"><AppIcon :name="icon" :size="26" /></div>
    <strong>{{ title || description }}</strong>
    <p v-if="title && description">{{ description }}</p>
    <div v-if="$slots.default" class="aw-empty-actions"><slot /></div>
  </div>
</template>
