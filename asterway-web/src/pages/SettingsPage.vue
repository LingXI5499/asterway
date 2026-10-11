<script setup lang="ts">
import { ref, watch } from 'vue'
import { useAppStore } from '../store/app'
import { ui } from '../store/ui'
import { validateData } from '../domain/schema'
import { now } from '../domain/dates'
import type { AppData } from '../domain/types'
import PageHeader from '../components/PageHeader.vue'
import Icon from '../components/Icon.vue'
import Modal from '../components/Modal.vue'
const store = useAppStore(),
  name = ref(store.data.profile.displayName),
  reminder = ref(store.data.profile.reminder),
  importPreview = ref<AppData | null>(null),
  fileInput = ref<HTMLInputElement>()
watch(
  () => store.data.profile,
  (profile) => {
    name.value = profile.displayName
    reminder.value = profile.reminder
  },
)
function save() {
  if (!name.value.trim()) return
  store.data.profile.displayName = name.value.trim()
  store.data.profile.reminder = reminder.value
  store.toast('个人偏好已保存')
}
function exportData() {
  const blob = new Blob([JSON.stringify({ ...store.data, exportedAt: now() }, null, 2)], {
    type: 'application/json',
  })
  const url = URL.createObjectURL(blob),
    link = document.createElement('a')
  link.href = url
  link.download = `asterway-${store.today}.json`
  link.click()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
  store.toast('已导出成长记录')
}
async function readFile(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return
  try {
    if (file.size > 5 * 1024 * 1024) throw new Error('请导入小于 5 MB 的 JSON 文件。')
    importPreview.value = validateData(JSON.parse(await file.text()))
  } catch (error) {
    store.toast(error instanceof Error ? error.message : '无法读取文件', true)
  } finally {
    if (fileInput.value) fileInput.value.value = ''
  }
}
function restore(demo: boolean) {
  ui.confirm(
    demo ? '重置为演示数据？' : '清空我的数据？',
    '这将覆盖当前浏览器中的所有成长记录。建议先导出备份，再确认操作。',
    () => store.reset(demo),
  )
}
function confirmImport() {
  if (!importPreview.value) return
  store.importData(importPreview.value)
  importPreview.value = null
}
</script>
<template>
  <PageHeader
    title="设置与数据"
    subtitle="你的成长，由你保管。随时备份，也可以重新开始。"
    eyebrow="YOUR SPACE, YOUR WAY"
  />
  <div class="settings-layout">
    <section class="card">
      <div class="section-heading">
        <h2>个人档案</h2>
        <Icon name="settings" :size="18" />
      </div>
      <form class="editor-form" @submit.prevent="save">
        <div class="settings-profile">
          <span class="avatar large-avatar">{{ name.slice(0, 1) || '星' }}</span>
          <div>
            <strong>{{ name || '星行者' }}</strong>
            <p class="muted">探索自己的方向，不与别人比较。</p>
          </div>
        </div>
        <label
          >显示名称<input
            v-model="name"
            maxlength="30"
            required
            placeholder="怎样称呼你？" /></label
        ><label class="checkbox-label"
          ><input v-model="reminder" type="checkbox" />保存每日提醒偏好</label
        >
        <p class="field-hint">目前只保存本地偏好，不发送系统通知。</p>
        <button class="button primary align-start" type="submit">保存档案</button>
      </form>
    </section>
    <section class="card">
      <div class="section-heading">
        <h2>成长记录</h2>
        <span class="local-badge">仅浏览器本地</span>
      </div>
      <p class="muted">
        {{ store.data.goals.length }} 个目标 · {{ store.tasks.length }} 项任务 ·
        {{ store.data.habits.filter((h) => h.status !== 'archived').length }} 个习惯
      </p>
      <div class="data-action">
        <span class="data-icon"><Icon name="download" /></span>
        <div>
          <h3>导出数据</h3>
          <p>下载完整 JSON，保留一份属于自己的备份。</p>
        </div>
        <button class="button secondary" @click="exportData">导出</button>
      </div>
      <div class="data-action">
        <span class="data-icon"><Icon name="upload" /></span>
        <div>
          <h3>导入数据</h3>
          <p>校验并预览后，确认覆盖当前记录。</p>
        </div>
        <button class="button secondary" @click="fileInput?.click()">导入</button
        ><input
          ref="fileInput"
          type="file"
          accept=".json,application/json"
          hidden
          aria-label="选择导入 JSON"
          @change="readFile"
        />
      </div>
      <div class="data-action">
        <span class="data-icon"><Icon name="undo" /></span>
        <div>
          <h3>重置演示数据</h3>
          <p>恢复一组相对今天生成的示例记录。</p>
        </div>
        <button class="button secondary" @click="restore(true)">重置</button>
      </div>
      <div class="data-action">
        <span class="data-icon danger"><Icon name="trash" /></span>
        <div>
          <h3>清空我的数据</h3>
          <p>从空白开始，不会自动恢复示例。</p>
        </div>
        <button class="button danger secondary" @click="restore(false)">清空</button>
      </div>
    </section>
    <section class="card about-card">
      <span class="brand-symbol"><Icon name="sparkles" :size="28" /></span>
      <div>
        <h2>Asterway <span class="muted">星行纪</span><small>v0.1.0 · Web MVP</small></h2>
        <p>AI Personal Growth OS · Find your path. Grow your way.</p>
        <p class="muted">
          一个把长期目标变成每日行动的成长工作台。当前为纯前端可交互原型，伙伴使用本地规则，不连接真实
          AI 或服务器。
        </p>
        <a
          class="text-button"
          href="https://github.com/LingXI5499/asterway"
          target="_blank"
          rel="noopener noreferrer"
          >查看 GitHub 仓库<Icon name="external" :size="16"
        /></a>
      </div>
    </section>
  </div>
  <Modal v-if="importPreview" title="确认导入成长记录" @close="importPreview = null"
    ><p>
      文件校验通过。包含 {{ importPreview.goals.length }} 个目标、{{
        importPreview.tasks.length
      }}
      项任务、{{ importPreview.habits.length }} 个习惯和
      {{ importPreview.habitLogs.length }} 次打卡。
    </p>
    <p class="warning-banner">确认后将覆盖当前数据，建议先导出备份。</p>
    <div class="modal-actions">
      <button class="button secondary" @click="importPreview = null">取消</button
      ><button class="button primary" @click="confirmImport">确认覆盖</button>
    </div></Modal
  >
</template>
