<script setup lang="ts">
import Icon from './Icon.vue'
withDefaults(defineProps<{ title: string; text?: string; icon?: string }>(), { icon: 'map' })
</script>
<template><div class="empty-state"><span class="empty-icon"><Icon :name="icon" :size="28" /></span><h3>{{ title }}</h3><p v-if="text">{{ text }}</p><slot /></div></template>
