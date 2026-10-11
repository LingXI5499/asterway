<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue';
import { useRoute } from 'vue-router';
import { usePrototypeStore } from '../store/prototype';
import PageHeader from '../components/PageHeader.vue';
import Icon from '../components/Icon.vue';
import Modal from '../components/Modal.vue';
import EmptyState from '../components/EmptyState.vue';
const proto = usePrototypeStore(),
  route = useRoute(),
  roomId = ref(''),
  filter = ref('全部'),
  text = ref(''),
  newChat = ref(false),
  thread = ref<HTMLElement>();
const rooms = computed(() =>
  proto.data.rooms.filter(
    (r) =>
      filter.value === '全部' ||
      (filter.value === '私信' ? r.kind === 'direct' : r.kind === 'group'),
  ),
);
const room = computed(() => proto.data.rooms.find((r) => r.id === roomId.value));
watch(
  () => route.query.room,
  (id) => {
    roomId.value = typeof id === 'string' ? id : proto.data.rooms[0]?.id || '';
  },
  { immediate: true },
);
watch(
  room,
  (r) => {
    if (r) r.unread = 0;
  },
  { immediate: true },
);
watch(
  () => room.value?.messages.length,
  async () => {
    await nextTick();
    thread.value?.scrollTo({ top: thread.value.scrollHeight });
  },
);
function send() {
  if (room.value && text.value.trim()) {
    proto.sendMessage(room.value.id, text.value);
    text.value = '';
  }
}
function start(id: string) {
  roomId.value = proto.openDm(id);
  newChat.value = false;
}
</script>
<template>
  <PageHeader
    title="消息中心"
    subtitle="一句鼓励，一份交流，让成长多一点陪伴。"
    eyebrow="A LITTLE CONNECTION"
    ><button class="button secondary" @click="newChat = true">
      <Icon name="plus" :size="16" />发起私信
    </button></PageHeader
  >
  <div class="local-message-notice">
    <Icon name="shield" :size="16" />本地消息演示，不发送至真实用户。回复由预置规则模拟。
  </div>
  <div class="messaging-layout card">
    <aside class="conversation-list">
      <div class="segmented">
        <button
          v-for="item in ['全部', '私信', '群聊']"
          :key="item"
          :class="{ active: filter === item }"
          @click="filter = item"
        >
          {{ item }}
        </button>
      </div>
      <button
        v-for="item in rooms"
        :key="item.id"
        :class="['conversation-row', { active: roomId === item.id }]"
        @click="roomId = item.id"
      >
        <span
          class="social-avatar"
          :class="item.kind === 'group' ? 'blue' : proto.user(item.peerId || 'me').color"
          ><Icon v-if="item.kind === 'group'" name="users" :size="20" /><template v-else>{{
            item.name.slice(0, 1)
          }}</template></span
        >
        <div>
          <strong>{{ item.name }}</strong
          ><small>{{ item.messages.at(-1)?.text || '开启一段成长交流' }}</small>
        </div>
        <span v-if="item.unread" class="unread-dot">{{ item.unread }}</span>
      </button>
      <p v-if="!rooms.length" class="muted small-text">加入社群后会出现群聊。</p>
    </aside>
    <section class="message-thread">
      <template v-if="room"
        ><header>
          <div>
            <h3>{{ room.name }}</h3>
            <small>{{
              room.kind === 'group' ? '成长社群 · 群聊原型' : '成长同行者 · 私信原型'
            }}</small>
          </div>
          <span class="demo-badge">模拟会话</span>
        </header>
        <div ref="thread" class="message-scroll">
          <div
            v-for="message in room.messages"
            :key="message.id"
            :class="['social-message', { mine: message.authorId === 'me' }]"
          >
            <span :class="['social-avatar', proto.user(message.authorId).color]">{{
              proto.user(message.authorId).name.slice(0, 1)
            }}</span>
            <div>
              <small
                >{{ proto.user(message.authorId).name }} ·
                {{
                  new Date(message.createdAt).toLocaleTimeString('zh-CN', {
                    hour: '2-digit',
                    minute: '2-digit',
                  })
                }}</small
              >
              <p>{{ message.text }}</p>
            </div>
          </div>
        </div>
        <form class="message-composer" @submit.prevent="send">
          <textarea
            v-model="text"
            required
            maxlength="1000"
            rows="2"
            aria-label="消息内容"
            placeholder="分享你的行动，或给同行者一点鼓励…"
          />
          <div>
            <small>演示消息仅保存在本机</small
            ><button type="submit" class="button primary">
              <Icon name="send" :size="15" />模拟发送
            </button>
          </div>
        </form></template
      ><EmptyState v-else title="选择一段会话" text="与同行者交换成长经验。" icon="message" />
    </section>
  </div>
  <Modal v-if="newChat" title="选择一位演示同行者" @close="newChat = false"
    ><button
      v-for="user in proto.data.users"
      :key="user.id"
      class="contact-choice"
      @click="start(user.id)"
    >
      <span :class="['social-avatar', user.color]">{{ user.name.slice(0, 1) }}</span>
      <div>
        <strong>{{ user.name }}</strong
        ><small>{{ user.persona }}</small>
      </div>
      <Icon name="arrow" :size="17" /></button
  ></Modal>
</template>
