<script setup lang="ts">
defineProps<{ title: string; subtitle: string; eyebrow?: string }>()
</script>
<template>
  <header class="page-header">
    <div>
      <div v-if="eyebrow" class="eyebrow">{{ eyebrow }}</div>
      <h1>{{ title }}</h1>
      <p>{{ subtitle }}</p>
    </div>
    <div class="header-actions"><slot /></div>
  </header>
</template>
