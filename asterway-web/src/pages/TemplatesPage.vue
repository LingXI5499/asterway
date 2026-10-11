<script setup lang="ts">
import { computed, ref } from 'vue';
import { growthTemplates, type GrowthTemplate } from '../data/templates';
import { templateBundle } from '../agent/bundles';
import { useAppStore } from '../store/app';
import { usePrototypeStore } from '../store/prototype';
import { useRouter } from 'vue-router';
import type { GrowthBundle } from '../domain/prototype';
import PageHeader from '../components/PageHeader.vue';
import Icon from '../components/Icon.vue';
import Modal from '../components/Modal.vue';
import BundlePreview from '../components/BundlePreview.vue';
import EmptyState from '../components/EmptyState.vue';
const app = useAppStore(),
  proto = usePrototypeStore(),
  router = useRouter(),
  query = ref(''),
  group = ref('全部'),
  selected = ref<GrowthTemplate | null>(null),
  bundle = ref<GrowthBundle | null>(null),
  start = ref(app.today),
  days = ref(30);
const groups = ['全部', ...new Set(growthTemplates.map((t) => t.group))];
const templates = computed(() =>
  growthTemplates.filter(
    (t) =>
      (group.value === '全部' || t.group === group.value) &&
      `${t.title}${t.audience}${t.tags.join('')}`
        .toLowerCase()
        .includes(query.value.trim().toLowerCase()),
  ),
);
function preview(template: GrowthTemplate) {
  selected.value = template;
  days.value = template.days;
  start.value = app.today;
  refresh();
}
function refresh() {
  if (selected.value)
    bundle.value = templateBundle(selected.value, start.value, Number(days.value));
}
function apply() {
  if (bundle.value && proto.applyBundle(bundle.value)) {
    selected.value = null;
    bundle.value = null;
    router.push('/goals');
  }
}
</script>
<template>
  <PageHeader
    title="成长模板库"
    subtitle="考试、语言、技能和生活习惯。找到一个方向，再把它改成自己的计划。"
    eyebrow="A STARTING POINT FOR EVERY PATH"
    ><span class="local-badge">{{ growthTemplates.length }} 个可用模板</span></PageHeader
  >
  <section class="template-hero">
    <div>
      <span class="eyebrow">从一个真实命题开始</span>
      <h2>不必从空白开始。<br />你的下一段成长，有迹可循。</h2>
      <p>一个模板 = 一个目标 + 多个阶段与任务 + 配套习惯</p>
    </div>
    <div class="template-hero-art">
      <Icon name="map" :size="64" /><span>方向 / 行动 / 积累</span>
    </div>
  </section>
  <div class="catalog-toolbar">
    <div class="chip-row">
      <button
        v-for="item in groups"
        :key="item"
        :class="['chip', { active: group === item }]"
        @click="group = item"
      >
        {{ item }}
      </button>
    </div>
    <label class="catalog-search"
      ><Icon name="search" :size="17" /><input
        v-model="query"
        aria-label="搜索成长模板"
        placeholder="搜索四级、雅思、Vue、阅读…"
    /></label>
  </div>
  <div class="template-grid">
    <article v-for="template in templates" :key="template.id" class="card template-card">
      <div class="section-heading">
        <span :class="['category-icon', template.category]"><Icon :name="template.category" /></span
        ><span class="subtle-label">{{ template.days }} 天 · {{ template.group }}</span>
      </div>
      <h3>{{ template.title }}</h3>
      <p>{{ template.description }}</p>
      <div class="chip-row">
        <span class="tag">{{ template.audience }}</span
        ><span class="tag">{{ template.actions.length }} 个行动</span>
      </div>
      <div class="template-stages">
        <span v-for="(stage, i) in template.stages" :key="stage">{{ i + 1 }}. {{ stage }}</span>
      </div>
      <button class="button secondary full-width" @click="preview(template)">
        预览并使用<Icon name="arrow" :size="15" />
      </button>
    </article>
  </div>
  <EmptyState
    v-if="!templates.length"
    title="没有找到这个方向"
    text="换一个关键词，或手动创建属于自己的目标。"
  />
  <p class="prototype-footnote">
    模板是用于讨论需求的通用行动示例，不提供官方考试日期、报考规则或成绩保证。后续可按不同人群和实际课程继续扩充。
  </p>
  <Modal v-if="selected && bundle" title="把模板变成自己的计划" wide @close="selected = null"
    ><div class="form-grid">
      <label>开始日期<input v-model="start" type="date" required @change="refresh" /></label
      ><label
        >计划天数<input v-model="days" type="number" min="7" max="365" @change="refresh"
      /></label>
    </div>
    <BundlePreview :bundle="bundle" />
    <div class="modal-actions">
      <button class="button secondary" @click="selected = null">取消</button
      ><button class="button primary" :disabled="!start || days < 7 || days > 365" @click="apply">
        加入我的目标
      </button>
    </div></Modal
  >
</template>
