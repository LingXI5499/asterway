<script setup lang="ts">
import { computed } from 'vue'
import type { Task } from '../domain/types'
import { categories, priorities } from '../domain/types'
import { useAppStore } from '../store/app'
import { ui } from '../store/ui'
import Icon from './Icon.vue'
defineProps<{ task: Task; compact?: boolean; conflict?: boolean }>()
const store = useAppStore()
const goals = computed(() => new Map(store.data.goals.map(g => [g.id, g])))
</script>
<template>
  <div :class="['task-row', { done: task.status === 'done', compact, conflict }]">
    <button :class="['task-check', { checked: task.status === 'done' }]" :aria-label="`${task.status === 'done' ? '取消完成' : '完成'}：${task.title}`" :aria-pressed="task.status === 'done'" @click="store.toggleTask(task.id)"><Icon v-if="task.status === 'done'" name="check" :size="14" /></button>
    <button class="task-content" @click="ui.open('task', { id: task.id })"><span class="task-title">{{ task.title }}</span><span class="task-meta"><span v-if="task.goalId && goals.get(task.goalId)" :class="['category-label', goals.get(task.goalId)?.category]">{{ categories[goals.get(task.goalId)!.category] }} · {{ goals.get(task.goalId)?.title }}</span><span>{{ task.durationMinutes }} 分钟 · {{ task.startTime || '时段待定' }}</span><span v-if="conflict" class="danger-text">时段重叠</span></span></button>
    <span v-if="!compact" :class="['priority-tag', task.priority]">{{ priorities[task.priority] }}</span>
    <button v-if="!compact" class="icon-button" :aria-label="`编辑：${task.title}`" @click="ui.open('task', { id: task.id })"><Icon name="more" :size="18" /></button>
  </div>
</template>
