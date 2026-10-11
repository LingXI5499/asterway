<script setup lang="ts">
import { computed, ref } from 'vue'
import { useAppStore } from '../store/app'
import { ui } from '../store/ui'
import { goalProgress } from '../domain/selectors'
import { categories } from '../domain/types'
import { shortDate, now, uid } from '../domain/dates'
import Modal from './Modal.vue'
import Icon from './Icon.vue'
import TaskRow from './TaskRow.vue'
const store = useAppStore(), id = ui.modal!.id
const goal = computed(() => store.data.goals.find(g => g.id === id)!)
const progress = computed(() => goalProgress(store.data, id!))
const milestones = computed(() => store.data.milestones.filter(m => m.goalId === id).sort((a, b) => a.order - b.order))
const tasks = computed(() => store.tasks.filter(t => t.goalId === id))
const newTitle = ref(''), editing = ref(''), editTitle = ref('')
function addMilestone() { if (newTitle.value.trim().length < 2) return; store.data.milestones.push({ id: uid(), goalId: id!, title: newTitle.value.trim(), order: milestones.value.length, createdAt: now() }); newTitle.value = ''; store.toast('里程碑已添加') }
function move(milestoneId: string, delta: number) { const index = milestones.value.findIndex(m => m.id === milestoneId), next = milestones.value[index + delta]; if (!next) return; const values = [...milestones.value]; [values[index], values[index + delta]] = [values[index + delta]!, values[index]!]; values.forEach((m, i) => m.order = i) }
function deleteMilestone(milestoneId: string) { ui.confirm('删除里程碑？', '任务会保留在当前目标下，解除此里程碑关联。', () => { store.data.milestones = store.data.milestones.filter(m => m.id !== milestoneId); store.data.tasks.forEach(t => { if (t.milestoneId === milestoneId) { delete t.milestoneId; t.updatedAt = now() } }); ui.open('goalDetail', { id }) }) }
</script>
<template><Modal v-if="goal" :title="goal.title" wide @close="ui.close()">
  <div class="goal-detail-summary"><span :class="['category-label', goal.category]">{{ categories[goal.category] }}</span><span>{{ shortDate(goal.startDate) }} → {{ shortDate(goal.targetDate) }}</span><span>每周 {{ goal.weeklyHours }} 小时</span></div>
  <p class="muted">{{ goal.description }}</p><div class="progress-caption"><strong>{{ progress.percent === null ? '待规划' : `${progress.percent}%` }}</strong><span>{{ progress.done }}/{{ progress.total }} 项行动已完成</span></div><div class="progress-track large"><span :style="{ width: `${progress.percent || 0}%` }" /></div>
  <div class="chip-row detail-actions"><button class="button secondary" @click="ui.open('goal', { id })"><Icon name="pencil" :size="16" />编辑目标</button><button class="button secondary" @click="goal.status = goal.status === 'active' ? 'paused' : 'active'">{{ goal.status === 'active' ? '暂停目标' : '恢复目标' }}</button><button v-if="goal.status !== 'completed'" class="button secondary" @click="goal.status = 'completed'; store.toast('已标记目标完成；行动进度仍按实际记录计算')">标记完成</button></div>
  <div class="detail-grid"><section><div class="section-heading"><h3>成长路线</h3><span class="muted">{{ milestones.length }} 个里程碑</span></div>
    <div v-for="(milestone, index) in milestones" :key="milestone.id" class="milestone-item"><span class="milestone-number">{{ String(index + 1).padStart(2, '0') }}</span><div class="milestone-main"><form v-if="editing === milestone.id" @submit.prevent="milestone.title = editTitle.trim(); editing = ''"><input v-model="editTitle" minlength="2" maxlength="60" required aria-label="里程碑名称" /><button class="text-button">保存</button></form><strong v-else>{{ milestone.title }}</strong><small>{{ tasks.filter(t => t.milestoneId === milestone.id && t.status === 'done').length }}/{{ tasks.filter(t => t.milestoneId === milestone.id).length }} 项行动完成</small><button class="text-button" @click="ui.open('task', { goalId: id, milestoneId: milestone.id, date: store.today })">+ 添加行动</button></div><div class="milestone-tools"><button class="icon-button" aria-label="编辑里程碑" @click="editing = milestone.id; editTitle = milestone.title"><Icon name="pencil" :size="14" /></button><button class="icon-button" aria-label="上移里程碑" :disabled="index === 0" @click="move(milestone.id, -1)"><Icon name="up" :size="14" /></button><button class="icon-button" aria-label="下移里程碑" :disabled="index === milestones.length - 1" @click="move(milestone.id, 1)"><Icon name="down" :size="14" /></button><button class="icon-button danger" aria-label="删除里程碑" @click="deleteMilestone(milestone.id)"><Icon name="trash" :size="14" /></button></div></div>
    <form class="inline-form" @submit.prevent="addMilestone"><input v-model="newTitle" required minlength="2" maxlength="60" placeholder="添加一个里程碑" aria-label="新里程碑名称" /><button class="button secondary" type="submit"><Icon name="plus" :size="16" /></button></form>
  </section><section><div class="section-heading"><h3>目标行动</h3><button class="text-button" @click="ui.open('task', { goalId: id, date: store.today })">+ 新建</button></div><TaskRow v-for="task in tasks" :key="task.id" :task="task" compact /><p v-if="!tasks.length" class="muted">方向已经有了，添加第一个行动吧。</p><RouterLink class="button primary full-width" :to="`/assistant?goal=${id}`" @click="ui.close()"><Icon name="sparkles" :size="16" />让伙伴帮我拆解</RouterLink></section></div>
</Modal></template>
