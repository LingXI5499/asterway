<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { usePrototypeStore } from '../store/prototype';
import { useAppStore } from '../store/app';
import { ui } from '../store/ui';
import type { KnowledgeNote } from '../domain/prototype';
import PageHeader from '../components/PageHeader.vue';
import Icon from '../components/Icon.vue';
import Modal from '../components/Modal.vue';
import EmptyState from '../components/EmptyState.vue';
const proto = usePrototypeStore(),
  app = useAppStore(),
  query = ref(''),
  filter = ref('全部'),
  editing = ref(false),
  noteId = ref(''),
  title = ref(''),
  content = ref(''),
  tags = ref('');
const sourceNames = { manual: '手动记录', assistant: '伙伴记忆', community: '社区收集' };
const notes = computed(() =>
  proto.data.notes.filter(
    (n) =>
      (filter.value === '全部' || sourceNames[n.source] === filter.value) &&
      `${n.title}${n.content}${n.tags.join('')}`.includes(query.value.trim()),
  ),
);
function edit(note?: KnowledgeNote) {
  noteId.value = note?.id || '';
  title.value = note?.title || '';
  content.value = note?.content || '';
  tags.value = note?.tags.join('，') || '';
  editing.value = true;
}
const route = useRoute();
watch(
  () => route.query.note,
  (id) => {
    const note = proto.data.notes.find((n) => n.id === id);
    if (note) edit(note);
  },
  { immediate: true },
);
function save() {
  const source = proto.data.notes.find((n) => n.id === noteId.value)?.source || 'manual';
  proto.saveNote(
    {
      title: title.value.trim(),
      content: content.value.trim(),
      tags: tags.value
        .split(/[，,]/)
        .map((t) => t.trim())
        .filter(Boolean)
        .slice(0, 8),
      source,
    },
    noteId.value || undefined,
  );
  editing.value = false;
}
function remove(note: KnowledgeNote) {
  ui.confirm('删除这条知识与记忆？', '伙伴将不再引用这条内容。只影响本地知识库。', () => {
    proto.data.notes = proto.data.notes.filter((n) => n.id !== note.id);
    app.toast('记录已删除');
  });
}
</script>
<template>
  <PageHeader
    title="个人知识库"
    subtitle="记录你的情况、经验和方法，让伙伴更了解你的成长方向。"
    eyebrow="YOUR CONTEXT, YOUR KNOWLEDGE"
    ><button class="button primary" @click="edit()">
      <Icon name="plus" :size="17" />新建记录
    </button></PageHeader
  >
  <div class="knowledge-banner">
    <Icon name="notebook" :size="34" />
    <div>
      <h2>成长，有自己的上下文。</h2>
      <p>手动写下偏好，通过对话记录信息，或把社区的实用经验收入这里。你可以随时修改和删除。</p>
    </div>
    <RouterLink class="button secondary" to="/assistant"
      >和伙伴聊聊<Icon name="arrow" :size="15"
    /></RouterLink>
  </div>
  <div class="catalog-toolbar">
    <div class="chip-row">
      <button
        v-for="item in ['全部', '手动记录', '伙伴记忆', '社区收集']"
        :key="item"
        :class="['chip', { active: filter === item }]"
        @click="filter = item"
      >
        {{ item }}
      </button>
    </div>
    <label class="catalog-search"
      ><Icon name="search" :size="16" /><input
        v-model="query"
        aria-label="搜索知识库"
        placeholder="搜索记录和标签"
    /></label>
  </div>
  <div class="knowledge-grid">
    <article v-for="note in notes" :key="note.id" class="card knowledge-card">
      <div class="section-heading">
        <span class="tag">{{ sourceNames[note.source] }}</span
        ><button class="icon-button" :aria-label="`删除记录：${note.title}`" @click="remove(note)">
          <Icon name="trash" :size="15" />
        </button>
      </div>
      <button class="knowledge-open" @click="edit(note)">
        <h3>{{ note.title }}</h3>
        <p>{{ note.content }}</p>
      </button>
      <div class="chip-row">
        <span v-for="tag in note.tags" :key="tag" class="tag"># {{ tag }}</span>
      </div>
      <footer>
        {{ new Date(note.updatedAt).toLocaleDateString('zh-CN')
        }}<button class="text-button" @click="edit(note)">
          编辑<Icon name="pencil" :size="13" />
        </button>
      </footer>
    </article>
  </div>
  <EmptyState
    v-if="!notes.length"
    title="先留下一点关于自己的线索"
    text="记录一条个人偏好、一次复盘或一个实用方法。"
    icon="notebook"
  />
  <p class="prototype-footnote">
    本地知识库原型：按关键词引用，不包含向量检索、RAG 或云端长期记忆。
  </p>
  <Modal v-if="editing" :title="noteId ? '编辑知识记录' : '新建知识记录'" @close="editing = false"
    ><form class="editor-form" @submit.prevent="save">
      <label>记录标题<input v-model="title" maxlength="60" minlength="2" required /></label
      ><label
        >内容<textarea
          v-model="content"
          maxlength="5000"
          rows="7"
          required
          placeholder="我的情况、学习偏好、方法和经验…"
        /></label
      ><label
        >标签<input v-model="tags" maxlength="120" placeholder="学习，个人偏好（用逗号分隔）"
      /></label>
      <div class="modal-actions">
        <button type="button" class="button secondary" @click="editing = false">取消</button
        ><button class="button primary" type="submit">保存记录</button>
      </div>
    </form></Modal
  >
</template>
