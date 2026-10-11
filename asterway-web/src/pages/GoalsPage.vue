<script setup lang="ts">
import { computed, ref } from 'vue'
import { useAppStore } from '../store/app'
import { ui } from '../store/ui'
import { categories } from '../domain/types'
import PageHeader from '../components/PageHeader.vue'
import Icon from '../components/Icon.vue'
import GoalCard from '../components/GoalCard.vue'
import EmptyState from '../components/EmptyState.vue'
const store = useAppStore(),
  category = ref('all'),
  status = ref('active')
const goals = computed(() =>
  store.data.goals.filter(
    (g) =>
      (category.value === 'all' || g.category === category.value) &&
      (status.value === 'all' || g.status === status.value),
  ),
)
</script>
<template>
  <PageHeader
    title="我的目标"
    subtitle="先有方向，再走好每一步。让长期愿望变成可执行的成长路线。"
    eyebrow="CHOOSE YOUR DIRECTION"
    ><button class="button primary" @click="ui.open('goal')">
      <Icon name="plus" :size="17" />新建目标
    </button></PageHeader
  >
  <div class="filter-bar">
    <div class="chip-row">
      <button
        v-for="(label, key) in { all: '全部分类', ...categories }"
        :key="key"
        :class="['chip', { active: category === key }]"
        @click="category = key"
      >
        {{ label }}
      </button>
    </div>
    <select v-model="status" aria-label="目标状态">
      <option value="active">正在进行</option>
      <option value="paused">已暂停</option>
      <option value="completed">已完成</option>
      <option value="all">全部状态</option>
    </select>
  </div>
  <div class="goals-grid">
    <GoalCard v-for="goal in goals" :key="goal.id" :goal="goal" /><button
      v-if="goals.length"
      class="new-goal-card"
      @click="ui.open('goal')"
    >
      <span><Icon name="plus" :size="26" /></span>
      <h3>下一站，去哪里？</h3>
      <p>为一个新的可能性，设定方向。</p>
    </button>
  </div>
  <EmptyState
    v-if="!goals.length"
    title="每条路，都从一个方向开始"
    text="创建自己的第一个目标，或换个筛选条件看看。"
    ><button class="button primary" @click="ui.open('goal')">创建目标</button></EmptyState
  >
  <div class="gentle-note">
    <Icon name="map" :size="20" />
    <p>目标不必一次完美。先给自己一个方向，再用每一天的行动慢慢校准。</p>
  </div>
</template>
