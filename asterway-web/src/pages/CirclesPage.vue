<script setup lang="ts">
import { ref, computed, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { usePrototypeStore } from '../store/prototype';
import { useAppStore } from '../store/app';
import type { CircleGroup } from '../domain/prototype';
import { uid } from '../domain/dates';
import PageHeader from '../components/PageHeader.vue';
import Icon from '../components/Icon.vue';
import Modal from '../components/Modal.vue';
import EmptyState from '../components/EmptyState.vue';
const proto = usePrototypeStore(),
  app = useAppStore(),
  route = useRoute(),
  router = useRouter(),
  tab = ref('发现社群'),
  selected = ref<CircleGroup | null>(null),
  creating = ref(false),
  name = ref(''),
  description = ref(''),
  topic = ref('专注与习惯');
const circles = computed(() =>
  proto.data.circles.filter((c) => tab.value !== '我的社群' || c.joined),
);
function chat(circle: CircleGroup) {
  if (!circle.joined) proto.joinCircle(circle.id);
  selected.value = null;
  router.push(`/messages?room=${circle.roomId}`);
}
function create() {
  const circle = {
    id: uid(),
    name: name.value.trim(),
    description: description.value.trim(),
    topic: topic.value,
    cover: 'sage',
    members: 0,
    joined: false,
    roomId: uid(),
  };
  proto.data.circles.unshift(circle);
  proto.joinCircle(circle.id);
  creating.value = false;
  name.value = '';
  description.value = '';
  selected.value = circle;
}
watch(
  () => route.query.circle,
  (id) => {
    if (typeof id === 'string')
      selected.value = proto.data.circles.find((c) => c.id === id) || null;
  },
  { immediate: true },
);
</script>
<template>
  <PageHeader
    title="成长社群"
    subtitle="在共同的目标里遇见同行者，给坚持一点陪伴。"
    eyebrow="FIND YOUR PEOPLE"
    ><button class="button primary" @click="creating = true">
      <Icon name="plus" :size="17" />创建社群
    </button></PageHeader
  >
  <div class="community-tabs">
    <button
      v-for="item in ['发现社群', '我的社群']"
      :key="item"
      :class="{ active: tab === item }"
      @click="tab = item"
    >
      {{ item }}
    </button>
  </div>
  <div class="circle-grid">
    <article v-for="circle in circles" :key="circle.id" class="card circle-card">
      <div :class="['circle-cover', circle.cover]">
        <Icon name="users" :size="45" /><span>{{ circle.topic }}</span>
      </div>
      <div class="section-heading">
        <h3>{{ circle.name }}</h3>
        <span v-if="circle.joined" class="tag">已加入</span>
      </div>
      <p>{{ circle.description }}</p>
      <div class="circle-members">
        <div class="avatar-stack">
          <span
            v-for="user in proto.data.users.slice(0, 3)"
            :key="user.id"
            :class="['social-avatar', user.color]"
            >{{ user.name.slice(0, 1) }}</span
          >
        </div>
        <span>{{ circle.members + Number(circle.joined) }} 位示例成员</span>
      </div>
      <div class="chip-row">
        <button class="button secondary" @click="selected = circle">了解社群</button
        ><button class="button primary" @click="chat(circle)">
          {{ circle.joined ? '进入群聊' : '加入并交流' }}<Icon name="arrow" :size="15" />
        </button>
      </div>
    </article>
  </div>
  <EmptyState
    v-if="!circles.length"
    title="找到适合自己的同行者"
    text="先加入一个社群，或创建属于自己的成长小组。"
    icon="users"
  />
  <p class="prototype-footnote">
    社群成员和群聊为本地示例，加入、退出与创建仅改变当前浏览器里的演示状态。
  </p>
  <Modal v-if="selected" :title="selected.name" @close="selected = null"
    ><div :class="['circle-cover', selected.cover]">
      <Icon name="users" :size="52" /><span>{{ selected.topic }}</span>
    </div>
    <p class="circle-detail-description">{{ selected.description }}</p>
    <div class="community-guideline">
      <h3>社群约定</h3>
      <p>分享真实行动，尊重不同节奏；不鼓励焦虑、攀比、虚假成绩和商业刷屏。</p>
    </div>
    <div class="modal-actions">
      <button
        v-if="selected.joined"
        class="button secondary"
        @click="proto.joinCircle(selected.id)"
      >
        退出社群</button
      ><button class="button primary" @click="chat(selected)">
        {{ selected.joined ? '进入群聊' : '加入这个社群' }}
      </button>
    </div></Modal
  ><Modal v-if="creating" title="建立一个成长社群" @close="creating = false"
    ><form class="editor-form" @submit.prevent="create">
      <label>社群名称<input v-model="name" minlength="2" maxlength="40" required /></label
      ><label
        >共同的成长方向<textarea v-model="description" maxlength="1000" rows="3" required /></label
      ><label
        >话题<select v-model="topic">
          <option
            v-for="item in ['考试备考', '语言成长', '职业与技能', '专注与习惯', '创作与阅读']"
            :key="item"
          >
            {{ item }}
          </option>
        </select></label
      >
      <div class="modal-actions">
        <button type="button" class="button secondary" @click="creating = false">取消</button
        ><button type="submit" class="button primary">创建演示社群</button>
      </div>
    </form></Modal
  >
</template>
