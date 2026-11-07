<script setup>
defineProps({ title: String, description: String, eyebrow: String });
</script>
<template>
  <div class="page-heading aw-page-header">
    <div>
      <span v-if="eyebrow" class="eyebrow">{{ eyebrow }}</span>
      <h1>{{ title }}</h1>
      <p v-if="description" class="muted">{{ description }}</p>
    </div>
    <div v-if="$slots.default" class="toolbar"><slot /></div>
  </div>
</template>
