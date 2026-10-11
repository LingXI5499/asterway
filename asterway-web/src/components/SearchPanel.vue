<script setup lang="ts">
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import { useAppStore } from '../store/app';
import { usePrototypeStore } from '../store/prototype';
import { growthTemplates } from '../data/templates';
import { ui } from '../store/ui';
import Modal from './Modal.vue';
import Icon from './Icon.vue';
const query = ref(''),
  store = useAppStore(),
  proto = usePrototypeStore(),
  router = useRouter();
const results = computed(() => {
  const keyword = query.value.trim().toLowerCase();
  if (!keyword) return [];
  return [
    ...[
      {
        id: 'matrix',
        title: '四象限 · 重要与紧急',
        type: '方法',
        path: '/methods',
        icon: 'dashboard',
      },
      {
        id: 'top3',
        title: '每日三件事 · 安排行动重点',
        type: '方法',
        path: '/methods?tab=top3',
        icon: 'target',
      },
      {
        id: 'blocks',
        title: '时间分块 · 具体时段',
        type: '方法',
        path: '/methods?tab=blocks',
        icon: 'calendar',
      },
      { id: 'focus', title: '番茄钟 · 专注空间', type: '方法', path: '/focus', icon: 'clock' },
    ],
    ...proto.data.notes.map((n) => ({
      id: n.id,
      title: n.title,
      type: '知识',
      path: `/knowledge?note=${n.id}`,
      icon: 'notebook',
    })),
    ...growthTemplates.map((t) => ({
      id: t.id,
      title: t.title,
      type: '模板',
      path: `/templates?search=${encodeURIComponent(t.title)}`,
      icon: 'library',
    })),
    ...store.data.goals.map((g) => ({
      id: g.id,
      title: g.title,
      type: '目标',
      path: '/goals',
      icon: 'target',
    })),
    ...store.tasks.map((t) => ({
      id: t.id,
      title: t.title,
      type: '任务',
      path: '/planner',
      icon: 'calendar',
    })),
    ...store.data.habits
      .filter((h) => h.status !== 'archived')
      .map((h) => ({
        id: h.id,
        title: h.title,
        type: '习惯',
        path: '/habits',
        icon: 'checkCircle',
      })),
  ]
    .filter((v) => v.title.toLowerCase().includes(keyword))
    .slice(0, 30);
});
function go(item: (typeof results.value)[number]) {
  router.push(item.path);
  ui.close();
  if (item.type === '目标') ui.open('goalDetail', { id: item.id });
  if (item.type === '任务') ui.open('task', { id: item.id });
}
</script>
<template>
  <Modal title="搜索你的成长轨迹" @close="ui.close()"
    ><input
      v-model="query"
      class="search-input"
      placeholder="搜索目标、任务、知识、模板或计划方法…"
      aria-label="搜索关键词"
      autofocus
    />
    <div class="search-results">
      <button v-for="item in results" :key="`${item.type}-${item.id}`" @click="go(item)">
        <Icon :name="item.icon" /><span>{{ item.title }}</span
        ><small>{{ item.type }}</small>
      </button>
      <p v-if="query && !results.length" class="muted">没有找到相关内容，试试其他关键词。</p>
      <p v-if="!query" class="muted">从一个关键词开始，找到你的下一步。</p>
    </div></Modal
  >
</template>
