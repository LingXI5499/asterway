<script setup lang="ts">
import { computed, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { usePrototypeStore } from '../store/prototype';
import { useAppStore } from '../store/app';
import { ui } from '../store/ui';
import PageHeader from '../components/PageHeader.vue';
import Icon from '../components/Icon.vue';
import PostCover from '../components/PostCover.vue';
import EmptyState from '../components/EmptyState.vue';
const route = useRoute(),
  router = useRouter(),
  proto = usePrototypeStore(),
  app = useAppStore(),
  tab = ref('成长动态');
const id = computed(() => String(route.params.id || 'me')),
  self = computed(() => id.value === 'me'),
  exists = computed(() => self.value || proto.data.users.some((u) => u.id === id.value)),
  user = computed(() => proto.user(id.value)),
  posts = computed(() =>
    proto.data.posts.filter((p) =>
      tab.value === '我的收藏' && self.value ? p.saved : p.authorId === id.value,
    ),
  );
function message() {
  router.push(`/messages?room=${proto.openDm(id.value)}`);
}
</script>
<template>
  <template v-if="exists"
    ><PageHeader
      :title="self ? '我的成长名片' : '同行者主页'"
      subtitle="让别人看见你的方向，也记录自己走过的路。"
      eyebrow="YOUR GROWTH IDENTITY"
      ><RouterLink v-if="self" class="button secondary" to="/settings"
        ><Icon name="pencil" :size="16" />编辑档案</RouterLink
      ></PageHeader
    >
    <section class="profile-hero">
      <div :class="['social-avatar profile-avatar', user.color]">{{ user.name.slice(0, 1) }}</div>
      <div class="profile-identity">
        <div>
          <h2>{{ user.name }}</h2>
          <span class="tag">{{ user.tier }}</span
          ><span v-if="self && proto.data.membership !== 'free'" class="member-tag"
            ><Icon name="crown" :size="13" />{{ proto.data.membership.toUpperCase() }} 演示</span
          >
        </div>
        <p>{{ user.bio }}</p>
        <div class="chip-row">
          <span class="tag">{{ user.persona }}</span
          ><span class="tag">{{ self ? '本地成长档案' : '示例用户' }}</span>
        </div>
      </div>
      <div v-if="!self" class="chip-row">
        <button class="button primary" @click="proto.toggleFollow(id)">
          {{ proto.data.followedIds.includes(id) ? '已关注' : '+ 关注' }}</button
        ><button class="button secondary" @click="message">
          <Icon name="message" :size="16" />私信
        </button>
      </div>
      <RouterLink v-else class="button secondary" to="/achievements"
        ><Icon name="award" :size="16" />成就档案</RouterLink
      >
    </section>
    <div class="profile-stat-strip">
      <div>
        <strong>{{
          self ? app.data.goals.filter((g) => g.status === 'active').length : user.score
        }}</strong
        ><span>{{ self ? '活跃目标' : '示例成长分' }}</span>
      </div>
      <div>
        <strong>{{
          self
            ? app.data.tasks.filter((t) => t.status === 'done').length
            : proto.data.posts.filter((p) => p.authorId === id).length
        }}</strong
        ><span>{{ self ? '已完成任务' : '成长分享' }}</span>
      </div>
      <div>
        <strong>{{ user.following }}</strong
        ><span>{{ self ? '已关注' : '示例关注' }}</span>
      </div>
      <div>
        <strong>{{
          self
            ? proto.data.posts.filter((p) => p.authorId === 'me').reduce((n, p) => n + p.likes, 0)
            : user.followers
        }}</strong
        ><span>{{ self ? '内容获赞' : '示例粉丝' }}</span>
      </div>
    </div>
    <div v-if="self" class="profile-shortcuts">
      <RouterLink
        v-for="item in [
          { path: '/templates', icon: 'library', name: '探索模板' },
          { path: '/knowledge', icon: 'notebook', name: '个人知识库' },
          { path: '/achievements', icon: 'award', name: '段位徽章' },
          { path: '/membership', icon: 'crown', name: '会员身份' },
        ]"
        :key="item.path"
        :to="item.path"
        ><Icon :name="item.icon" :size="20" /><span>{{ item.name }}</span
        ><Icon name="arrow" :size="14"
      /></RouterLink>
    </div>
    <div class="community-tabs">
      <button :class="{ active: tab === '成长动态' }" @click="tab = '成长动态'">成长动态</button
      ><button v-if="self" :class="{ active: tab === '我的收藏' }" @click="tab = '我的收藏'">
        我的收藏
      </button>
    </div>
    <div class="community-feed profile-feed">
      <article v-for="post in posts" :key="post.id" class="post-card">
        <RouterLink to="/community"
          ><PostCover :post="post" />
          <div class="post-body">
            <span class="post-type">{{ post.topic }}</span>
            <h3>{{ post.title }}</h3>
            <p>{{ post.content.slice(0, 70) }}</p>
            <span class="muted small-text">{{ post.likes }} 赞 · {{ post.saves }} 收藏</span>
          </div></RouterLink
        >
      </article>
    </div>
    <EmptyState
      v-if="!posts.length"
      title="留下你的第一份成长分享"
      text="分享一份计划、一次复盘，或一个今天完成的小行动。"
      icon="users"
      ><RouterLink class="button secondary" to="/community">去成长社区</RouterLink></EmptyState
    ></template
  ><EmptyState v-else title="未找到这位同行者" text="回到社区寻找一个共同成长的方向。" />
</template>
