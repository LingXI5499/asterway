<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import type { Category, Priority, Task } from '../domain/types'
import { categories, priorities } from '../domain/types'
import { addDays, now, uid } from '../domain/dates'
import { makeTask } from '../domain/mutations'
import { conflictingIds } from '../domain/selectors'
import { useAppStore } from '../store/app'
import { ui } from '../store/ui'
import Modal from './Modal.vue'
import Icon from './Icon.vue'
const store = useAppStore(), context = { ...ui.modal! }, kind = context.kind, id = context.id
const existing = kind === 'goal' ? store.data.goals.find(v => v.id === id) : kind === 'task' ? store.data.tasks.find(v => v.id === id) : store.data.habits.find(v => v.id === id)
const initial = existing as any
const form = reactive({ title: initial?.title || '', description: initial?.description || '', category: (initial?.category || 'study') as Category, priority: (initial?.priority || 'medium') as Priority, startDate: initial?.startDate || store.today, targetDate: initial?.targetDate || addDays(store.today, 30), weeklyHours: initial?.weeklyHours ?? 3, goalId: initial?.goalId || context.goalId || '', milestoneId: initial?.milestoneId || context.milestoneId || '', durationMinutes: initial?.durationMinutes || 25, scheduledDate: initial?.scheduledDate ?? context.date ?? (id ? '' : store.today), startTime: initial?.startTime || '', isTodayFocus: initial?.isTodayFocus || false, frequency: initial?.frequency.kind || 'daily', times: initial?.frequency.times || 3 })
const error = ref('')
const title = `${id ? '编辑' : '新建'}${kind === 'goal' ? '目标' : kind === 'task' ? '任务' : '习惯'}`
const milestones = computed(() => store.data.milestones.filter(m => m.goalId === form.goalId).sort((a, b) => a.order - b.order))
const completed = kind === 'task' && initial?.status === 'done'
const templateNames = ['备考计划', '阅读提升', '每周运动', '个人项目开发']
function template(index: number) { const titles = ['30 天完成核心知识复习', '养成每日阅读的节奏', '每周运动三次', '完成个人项目 MVP']; form.title = titles[index]!; form.category = (['study', 'study', 'health', 'work'] as Category[])[index]!; form.weeklyHours = [7, 3, 2, 6][index]!; form.description = '把长期目标变成今天可以执行的小行动。' }
function save() {
  error.value = ''
  if (form.title.trim().length < 2 || form.title.trim().length > 60) { error.value = '名称需为 2–60 个字符。'; return }
  const timestamp = now()
  if (kind === 'goal') {
    if (form.targetDate < form.startDate) { error.value = '截止日期不能早于开始日期。'; return }
    const goal = { id: id || uid(), title: form.title.trim(), description: form.description, category: form.category, priority: form.priority, startDate: form.startDate, targetDate: form.targetDate, weeklyHours: Number(form.weeklyHours), status: initial?.status || 'active', createdAt: initial?.createdAt || timestamp }
    if (id) store.data.goals[store.data.goals.findIndex(v => v.id === id)] = goal
    else store.data.goals.push(goal)
    store.toast(id ? '目标已更新' : '目标已创建，添加一个里程碑或行动吧'); ui.open('goalDetail', { id: goal.id }); return
  } else if (kind === 'task') {
    const values: Partial<Task> & { title: string } = { title: form.title.trim(), description: form.description, goalId: form.goalId || undefined, milestoneId: form.milestoneId || undefined, priority: form.priority, durationMinutes: Number(form.durationMinutes), scheduledDate: form.scheduledDate || undefined, startTime: form.startTime || undefined, isTodayFocus: form.isTodayFocus }
    if (id) Object.assign(initial, values, { updatedAt: timestamp })
    else store.data.tasks.push(makeTask(values))
    if (conflictingIds(store.tasks).size) store.toast('已保存；部分任务时段重叠，请在日程中检查', true)
    else store.toast('任务已保存')
  } else {
    const habit = { id: id || uid(), title: form.title.trim(), category: form.category, frequency: form.frequency === 'daily' ? { kind: 'daily' as const } : { kind: 'weekly' as const, times: Number(form.times) }, startDate: form.startDate, status: initial?.status || 'active', createdAt: initial?.createdAt || timestamp }
    if (id) store.data.habits[store.data.habits.findIndex(v => v.id === id)] = habit
    else store.data.habits.push(habit)
    store.toast('习惯已保存')
  }
  ui.close()
}
function remove() { ui.confirm(kind === 'goal' ? '删除这个目标？' : kind === 'habit' ? '归档这个习惯？' : '删除这个任务？', kind === 'goal' ? '关联任务将保留并解除关联，历史 XP 不受影响。' : '从行动列表移除，已有完成记录和 XP 将保留用于历史统计。', () => { if (kind === 'goal') store.deleteGoal(id!); else if (kind === 'habit') initial.status = 'archived'; else initial.deletedAt = now(); store.toast('操作完成') }) }
</script>
<template>
  <Modal :title="title" @close="ui.close()">
    <form class="editor-form" @submit.prevent="save">
      <div v-if="kind === 'goal' && !id" class="template-picker"><span class="field-label">从模板开始</span><div class="chip-row"><button v-for="(name, index) in templateNames" :key="name" type="button" class="chip" @click="template(index)">{{ name }}</button></div></div>
      <label>{{ kind === 'goal' ? '目标名称' : kind === 'task' ? '任务名称' : '习惯名称' }} <span class="required">*</span><input v-model="form.title" required minlength="2" maxlength="60" placeholder="你想向哪个方向迈出一步？" autofocus /></label>
      <label v-if="kind !== 'habit'">{{ kind === 'task' ? '备注' : '目标描述' }}<textarea v-model="form.description" maxlength="5000" rows="2" placeholder="补充一些背景或执行提示（选填）" /></label>
      <div class="form-grid">
        <label v-if="kind !== 'task'">分类<select v-model="form.category"><option v-for="(label, key) in categories" :key="key" :value="key">{{ label }}</option></select></label>
        <label v-if="kind !== 'habit'">重要程度<select v-model="form.priority"><option v-for="(label, key) in priorities" :key="key" :value="key">{{ label }}</option></select></label>
        <label v-if="kind !== 'task'">开始日期<input v-model="form.startDate" type="date" required :max="kind === 'habit' && id ? initial.startDate : undefined" /></label>
        <label v-if="kind === 'goal'">计划完成日期<input v-model="form.targetDate" type="date" required :min="form.startDate" /></label>
        <label v-if="kind === 'goal'">每周计划投入（小时）<input v-model="form.weeklyHours" type="number" min="0" max="168" step="0.5" /></label>
        <template v-if="kind === 'task'">
          <label>归属目标<select v-model="form.goalId" @change="form.milestoneId = ''"><option value="">独立任务</option><option v-for="goal in store.data.goals" :key="goal.id" :value="goal.id">{{ goal.title }}</option></select></label>
          <label>里程碑<select v-model="form.milestoneId" :disabled="!form.goalId"><option value="">暂不关联</option><option v-for="milestone in milestones" :key="milestone.id" :value="milestone.id">{{ milestone.title }}</option></select></label>
          <label>预估分钟<input v-model="form.durationMinutes" type="number" min="1" max="1440" step="1" required /></label>
          <label>安排日期<input v-model="form.scheduledDate" type="date" :disabled="completed" /><span class="field-hint">{{ completed ? '已完成任务保留原排期；取消完成后可改期。' : '留空放入任务池' }}</span></label>
          <label>开始时间<input v-model="form.startTime" type="time" :disabled="!form.scheduledDate || completed" /></label>
        </template>
        <template v-if="kind === 'habit'"><label>执行频率<select v-model="form.frequency"><option value="daily">每天</option><option value="weekly">每周指定次数</option></select></label><label v-if="form.frequency === 'weekly'">每周次数<input v-model="form.times" type="number" min="1" max="7" required /></label></template>
      </div>
      <label v-if="kind === 'task'" class="checkbox-label"><input v-model="form.isTodayFocus" type="checkbox" />设为今日重点任务</label>
      <p v-if="error" class="error-message" role="alert">{{ error }}</p>
      <div class="modal-actions"><button v-if="id" type="button" class="button danger ghost" @click="remove"><Icon name="trash" :size="16" />{{ kind === 'habit' ? '归档' : '删除' }}</button><span class="push-right" /><button type="button" class="button secondary" @click="ui.close()">取消</button><button type="submit" class="button primary">{{ id ? '保存修改' : '创建' }}</button></div>
    </form>
  </Modal>
</template>
