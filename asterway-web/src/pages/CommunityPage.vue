<script setup lang="ts">
import { computed, ref, watch, onMounted, onBeforeUnmount } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAppStore } from '../store/app';
import { usePrototypeStore } from '../store/prototype';
import { growthTemplates, type GrowthTemplate } from '../data/templates';
import { templateBundle } from '../agent/bundles';
import type { Post, GrowthBundle } from '../domain/prototype';
import { now, uid } from '../domain/dates';
import { ui } from '../store/ui';
import PageHeader from '../components/PageHeader.vue';
import Icon from '../components/Icon.vue';
import Modal from '../components/Modal.vue';
import EmptyState from '../components/EmptyState.vue';
import PostCover from '../components/PostCover.vue';
import BundlePreview from '../components/BundlePreview.vue';
import { postImages } from '../domain/postMedia';
const app = useAppStore(),
  proto = usePrototypeStore(),
  route = useRoute(),
  router = useRouter(),
  tab = ref('推荐'),
  topic = ref('全部'),
  query = ref(''),
  selected = ref<Post | null>(null),
  commentText = ref(''),
  publishing = ref(false),
  bundle = ref<GrowthBundle | null>(null);
const topics = ['全部', '考试备考', '语言成长', '职业与技能', '专注与习惯', '创作与阅读'];
const posts = computed(() =>
  proto.data.posts
    .filter(
      (p) =>
        (tab.value !== '关注' || proto.data.followedIds.includes(p.authorId)) &&
        (tab.value !== '收藏' || p.saved) &&
        (tab.value !== '计划' || p.type === 'plan') &&
        (tab.value !== '视频' || p.type === 'video') &&
        (tab.value !== '图文' || p.type === 'article') &&
        (topic.value === '全部' || p.topic === topic.value) &&
        `${p.title}${p.content}`.includes(query.value.trim()),
    )
    .sort((a, b) =>
      tab.value === '计划' ? b.saves - a.saves : b.createdAt.localeCompare(a.createdAt),
    ),
);
const draft = ref({
  title: '',
  content: '',
  type: 'article' as Post['type'],
  topic: '专注与习惯',
  templateId: growthTemplates[0]!.id,
  goalId: '',
  circleId: '',
  media: '',
  images: [] as string[],
});
function templateFor(post: Post): GrowthTemplate | undefined {
  return post.templateId
    ? growthTemplates.find((t) => t.id === post.templateId)
    : post.planActions
      ? {
          id: post.id,
          title: post.title,
          description: post.content.slice(0, 5000),
          audience: '成长探索者',
          category: post.planCategory || 'life',
          days: post.planDays || 30,
          group: post.topic,
          tags: [post.topic],
          stages: ['准备与开始', '持续行动', '回顾与调整'],
          actions: post.planActions,
          habit: '每天记录一个成长行动',
        }
      : undefined;
}
function previewPlan(post: Post) {
  const template = templateFor(post);
  if (template) bundle.value = templateBundle(template, app.today);
  else app.toast('这份内容没有可复制的行动，请手动建立目标');
}
function applyPlan() {
  if (bundle.value && proto.applyBundle(bundle.value)) {
    bundle.value = null;
    selected.value = null;
    router.push('/goals');
  }
}
function publish() {
  if (draft.value.type === 'video' && !draft.value.media) {
    app.toast('请先选择一段演示视频', true);
    return;
  }
  const post: Post = {
    id: uid(),
    authorId: 'me',
    type: draft.value.type,
    title: draft.value.title.trim(),
    content: draft.value.content.trim(),
    topic: draft.value.topic,
    cover: 'sage',
    createdAt: now(),
    likes: 0,
    saves: 0,
    liked: false,
    saved: false,
    comments: [],
    media: draft.value.media || undefined,
    images:
      draft.value.type === 'article' && draft.value.images.length
        ? [...draft.value.images]
        : undefined,
    circleId: draft.value.circleId || undefined,
  };
  if (post.type === 'plan') {
    const goal = app.data.goals.find((g) => g.id === draft.value.goalId);
    if (goal) {
      post.planActions = app.tasks.filter((t) => t.goalId === goal.id).map((t) => t.title);
      if (!post.planActions.length)
        post.planActions = ['确定本阶段的结果', '安排一个具体行动', '记录并复盘一次行动'];
      post.planCategory = goal.category;
      post.planDays = 30;
    } else post.templateId = draft.value.templateId;
  }
  proto.data.posts.unshift(post);
  publishing.value = false;
  selected.value = post;
  app.toast('已发布到本地社区演示，不会上传至网络');
  draft.value = {
    title: '',
    content: '',
    type: 'article',
    topic: '专注与习惯',
    templateId: growthTemplates[0]!.id,
    goalId: '',
    circleId: '',
    media: '',
    images: [],
  };
}
async function upload(event: Event) {
  const input = event.target as HTMLInputElement,
    files = Array.from(input.files || []);
  if (!files.length) return;
  const video = draft.value.type === 'video';
  if (
    files.length > (video ? 1 : 6) ||
    files.reduce((n, f) => n + f.size, 0) > 1000000 ||
    files.some((f) => !f.type.startsWith(video ? 'video/' : 'image/'))
  ) {
    app.toast('请选择对应类型，最多 6 张图片或 1 段视频，总大小小于 1 MB。', true);
    input.value = '';
    return;
  }
  try {
    const type = draft.value.type;
    const sources = await Promise.all(
      files.map(
        (file) =>
          new Promise<string>((resolve, reject) => {
            const reader = new FileReader();
            reader.onload = () => resolve(String(reader.result));
            reader.onerror = () => reject(new Error('媒体读取失败'));
            reader.readAsDataURL(file);
          }),
      ),
    );
    if (type !== draft.value.type) return;
    draft.value.media = sources[0]!;
    draft.value.images = video ? [] : sources;
  } catch {
    app.toast('无法读取媒体，请重新选择。', true);
  } finally {
    input.value = '';
  }
}
function demoVideo() {
  draft.value.media = '/media/focus-demo.webm';
}
function setPostType(type: Post['type']) {
  if (draft.value.type !== type) {
    draft.value.media = '';
    draft.value.images = [];
  }
  draft.value.type = type;
}
const videoPosts = computed(() => posts.value.filter((post) => post.type === 'video'));
const imageIndex = ref(0),
  touchStart = ref({ x: 0, y: 0 });
watch(
  () => selected.value?.id,
  () => {
    imageIndex.value = 0;
  },
);
function swipe(event: TouchEvent) {
  const t = event.changedTouches[0];
  if (!t) return;
  const dy = t.clientY - touchStart.value.y,
    dx = t.clientX - touchStart.value.x;
  if (Math.abs(dy) > 70 && Math.abs(dy) > Math.abs(dx) * 1.5) browseVideo(dy < 0 ? 1 : -1);
}
function videoKeys(event: KeyboardEvent) {
  if (
    selected.value?.type !== 'video' ||
    (event.target as HTMLElement).closest('input,textarea,select,video')
  )
    return;
  if (['ArrowDown', 'ArrowUp'].includes(event.key)) {
    event.preventDefault();
    browseVideo(event.key === 'ArrowDown' ? 1 : -1);
  }
}
onMounted(() => window.addEventListener('keydown', videoKeys));
onBeforeUnmount(() => window.removeEventListener('keydown', videoKeys));
function browseVideo(step: number) {
  const videos = videoPosts.value;
  const index = videos.findIndex((post) => post.id === selected.value?.id);
  selected.value = videos[(index + step + videos.length) % videos.length] || null;
  commentText.value = '';
}
function addComment() {
  if (selected.value) {
    proto.comment(selected.value, commentText.value);
    commentText.value = '';
  }
}
function removePost(post: Post) {
  ui.confirm('删除这篇内容？', '只删除本地社区内容，不会删除关联的成长目标。', () => {
    proto.data.posts = proto.data.posts.filter((p) => p.id !== post.id);
    selected.value = null;
    app.toast('内容已删除');
  });
}
watch(
  () => route.query,
  (q) => {
    if (q.tab === 'saved') tab.value = '收藏';
    if (typeof q.shareGoal === 'string') {
      const goal = app.data.goals.find((g) => g.id === q.shareGoal);
      if (goal) {
        draft.value.goalId = goal.id;
        draft.value.type = 'plan';
        draft.value.title = `我的成长路线：${goal.title}`;
        draft.value.content = goal.description || '分享我的阶段计划和今天的行动。';
        publishing.value = true;
      }
    }
  },
  { immediate: true },
);
</script>
<template>
  <PageHeader
    title="成长社区"
    subtitle="不展示完美生活，分享认真向前的过程。找到和你一起成长的人。"
    eyebrow="GROW TOGETHER"
    ><button class="button primary" @click="publishing = true">
      <Icon name="plus" :size="17" />分享成长
    </button></PageHeader
  >
  <div class="community-hero">
    <div>
      <span class="eyebrow">今日话题 · 自律不是一场独行</span>
      <h2>今天，你为自己<br />完成了哪一小步？</h2>
      <p>计划、练习、读书和复盘，每一种真实行动都值得被看见。</p>
      <RouterLink class="text-button" to="/challenges"
        >参加今日行动赛<Icon name="arrow" :size="16"
      /></RouterLink>
    </div>
    <div class="community-hero-art">
      <span>一步</span><span>又一步</span><Icon name="sprout" :size="66" />
    </div>
  </div>
  <div class="community-layout">
    <section>
      <div class="community-tabs">
        <button
          v-for="item in ['推荐', '图文', '视频', '关注', '计划', '收藏']"
          :key="item"
          :class="{ active: tab === item }"
          @click="tab = item"
        >
          {{ item }}<span v-if="item === '计划'" class="tab-hint">计划榜</span></button
        ><label class="catalog-search"
          ><Icon name="search" :size="16" /><input
            v-model="query"
            aria-label="搜索社区内容"
            placeholder="搜索成长内容"
        /></label>
      </div>
      <div class="chip-row topic-filters">
        <button
          v-for="item in topics"
          :key="item"
          :class="['chip', { active: topic === item }]"
          @click="topic = item"
        >
          {{ item }}
        </button>
      </div>
      <div v-if="tab === '视频'" class="video-room-banner">
        <Icon name="video" :size="30" />
        <div>
          <span class="eyebrow">ASTERWAY FILM ROOM</span>
          <h2>看看别人如何，把一小步坚持下去。</h2>
          <p>纯视频频道 · 点击进入沉浸播放 · 手机上下滑切换</p>
        </div>
      </div>
      <div :class="['community-feed', { 'video-feed': tab === '视频' }]">
        <article v-for="post in posts" :key="post.id" class="post-card">
          <button
            class="post-open"
            :aria-label="`查看内容：${post.title}`"
            @click="
              selected = post;
              commentText = '';
            "
          >
            <PostCover :post="post" />
            <div class="post-body">
              <span class="post-type"
                >{{
                  post.type === 'plan'
                    ? '成长计划'
                    : post.type === 'video'
                      ? '视频分享'
                      : '图文笔记'
                }}
                · {{ post.topic }}</span
              >
              <h3>{{ post.title }}</h3>
              <p>{{ post.content.slice(0, 75) }}</p>
            </div>
          </button>
          <div class="post-author-row">
            <RouterLink :to="`/profile/${post.authorId}`"
              ><span :class="['social-avatar', proto.user(post.authorId).color]">{{
                proto.user(post.authorId).name.slice(0, 1)
              }}</span
              ><span>{{ proto.user(post.authorId).name }}</span></RouterLink
            ><button
              :class="{ selected: post.liked }"
              :aria-label="`点赞：${post.title}`"
              :aria-pressed="post.liked"
              @click="proto.toggleLike(post)"
            >
              <Icon name="heart" :size="15" />{{ post.likes }}</button
            ><button
              :class="{ selected: post.saved }"
              :aria-label="`收藏：${post.title}`"
              :aria-pressed="post.saved"
              @click="proto.toggleSave(post)"
            >
              <Icon name="bookmark" :size="15" />{{ post.saves }}
            </button>
          </div>
        </article>
      </div>
      <EmptyState
        v-if="!posts.length"
        title="这里还有一点空白"
        text="关注一些同行者，收藏喜欢的内容，或分享自己的第一步。"
        icon="users"
      />
    </section>
    <aside class="community-side">
      <div class="card">
        <div class="section-heading">
          <h3>你的同行者</h3>
          <Icon name="users" :size="17" />
        </div>
        <div v-for="user in proto.data.users.slice(0, 3)" :key="user.id" class="people-row">
          <RouterLink :to="`/profile/${user.id}`"
            ><span :class="['social-avatar', user.color]">{{ user.name.slice(0, 1) }}</span>
            <div>
              <strong>{{ user.name }}</strong
              ><small>{{ user.persona }} · {{ user.tier }}</small>
            </div></RouterLink
          ><button class="follow-button" @click="proto.toggleFollow(user.id)">
            {{ proto.data.followedIds.includes(user.id) ? '已关注' : '+ 关注' }}
          </button>
        </div>
      </div>
      <div class="card">
        <div class="section-heading">
          <h3>找到自己的社群</h3>
          <RouterLink class="text-button" to="/circles"
            >全部<Icon name="arrow" :size="13"
          /></RouterLink>
        </div>
        <RouterLink
          v-for="circle in proto.data.circles.slice(0, 3)"
          :key="circle.id"
          class="circle-mini"
          :to="`/circles?circle=${circle.id}`"
          ><span :class="['circle-mini-icon', circle.cover]"><Icon name="users" :size="18" /></span>
          <div>
            <strong>{{ circle.name }}</strong
            ><small>{{ circle.members + Number(circle.joined) }} 位示例成员</small>
          </div>
          <Icon name="right" :size="14"
        /></RouterLink>
      </div>
      <div class="community-guideline">
        <Icon name="health" :size="22" />
        <h3>分享成长，不制造焦虑</h3>
        <p>鼓励真实过程、实用计划与友善回应。这里的核心是自我成长，而不是炫耀和比较。</p>
      </div>
    </aside>
  </div>
  <p class="prototype-footnote">
    前端社区演示：用户和初始互动数为示例，本机点赞、评论、收藏和发布可保存；没有连接真实网络。
  </p>
  <Modal
    v-if="selected && !bundle"
    :title="selected.title"
    wide
    :class="['note-modal', { 'video-modal': selected.type === 'video' }]"
    @close="selected = null"
    ><div class="post-detail note-detail-layout">
      <div
        class="note-media-stage"
        @touchstart="
          (e) => {
            const t = e.touches[0];
            if (t) touchStart = { x: t.clientX, y: t.clientY };
          }
        "
        @touchend="selected.type === 'video' && swipe($event)"
      >
        <div v-if="selected.type === 'video'" class="video-navigation">
          <button
            class="button secondary"
            :disabled="videoPosts.length < 2"
            @click="browseVideo(-1)"
          >
            <Icon name="left" :size="16" />上一个</button
          ><span
            >视频浏览 · {{ videoPosts.findIndex((p) => p.id === selected?.id) + 1 }} /
            {{ videoPosts.length }}</span
          ><button
            class="button secondary"
            :disabled="videoPosts.length < 2"
            @click="browseVideo(1)"
          >
            下一个<Icon name="right" :size="16" />
          </button>
        </div>
        <video
          v-if="selected.type === 'video' && selected.media"
          :key="selected.id"
          :src="selected.media"
          :poster="
            selected.media?.startsWith('/media/')
              ? selected.media.replace('.webm', '.png')
              : undefined
          "
          controls
          loop
          playsinline
          muted
          autoplay
          preload="metadata"
          class="community-video"
        />
        <div v-else-if="selected.type === 'article'" class="note-carousel">
          <img
            :src="postImages(selected)[imageIndex]"
            :alt="`${selected.title} · 第 ${imageIndex + 1} 张`"
          />
          <div v-if="postImages(selected).length > 1" class="note-carousel-controls">
            <button
              class="icon-button"
              aria-label="上一张图片"
              @click="
                imageIndex =
                  (imageIndex - 1 + postImages(selected).length) % postImages(selected).length
              "
            >
              <Icon name="left" /></button
            ><span>{{ imageIndex + 1 }} / {{ postImages(selected).length }}</span
            ><button
              class="icon-button"
              aria-label="下一张图片"
              @click="imageIndex = (imageIndex + 1) % postImages(selected).length"
            >
              <Icon name="right" />
            </button>
          </div>
        </div>
        <PostCover v-else :post="selected" large />
        <span v-if="selected.type === 'video'" class="video-swipe-hint"
          >手机上下滑 · PC ↑ / ↓ 切换 · 可用播放器开启声音</span
        >
      </div>
      <div class="note-discussion">
        <div class="post-detail-author">
          <RouterLink :to="`/profile/${selected.authorId}`" @click="selected = null"
            ><span :class="['social-avatar', proto.user(selected.authorId).color]">{{
              proto.user(selected.authorId).name.slice(0, 1)
            }}</span
            >{{ proto.user(selected.authorId).name }}</RouterLink
          ><span class="tag">{{ selected.topic }}</span>
        </div>
        <h2>{{ selected.title }}</h2>
        <p class="post-content">{{ selected.content }}</p>
        <div class="chip-row">
          <button
            class="button secondary"
            :class="{ selected: selected.liked }"
            @click="proto.toggleLike(selected)"
          >
            <Icon name="heart" :size="16" />{{ selected.liked ? '已点赞' : '点赞' }}
            {{ selected.likes }}</button
          ><button class="button secondary" @click="proto.toggleSave(selected)">
            <Icon name="bookmark" :size="16" />{{ selected.saved ? '已收藏' : '收藏' }}</button
          ><button class="button secondary" @click="proto.collectPost(selected)">
            <Icon name="notebook" :size="16" />收入知识库</button
          ><button
            v-if="selected.type === 'plan'"
            class="button primary"
            @click="previewPlan(selected)"
          >
            加入我的计划</button
          ><button
            v-if="selected.authorId === 'me'"
            class="button danger ghost"
            @click="removePost(selected)"
          >
            删除内容
          </button>
        </div>
        <section class="comments-section">
          <h3>一起聊聊 · {{ selected.comments.length }} 条评论</h3>
          <div v-for="comment in selected.comments" :key="comment.id" class="comment-row">
            <span :class="['social-avatar', proto.user(comment.authorId).color]">{{
              proto.user(comment.authorId).name.slice(0, 1)
            }}</span>
            <div>
              <strong>{{ proto.user(comment.authorId).name }}</strong>
              <p>{{ comment.text }}</p>
            </div>
          </div>
          <form class="inline-form" @submit.prevent="addComment">
            <input
              v-model="commentText"
              maxlength="500"
              required
              placeholder="友善地回应一个真实的成长瞬间…"
              aria-label="评论内容"
            /><button class="button primary" type="submit">发布评论</button>
          </form>
        </section>
      </div>
    </div></Modal
  >
  <Modal v-if="bundle" title="加入这份成长计划" wide @close="bundle = null"
    ><BundlePreview :bundle="bundle" />
    <div class="modal-actions">
      <button class="button secondary" @click="bundle = null">取消</button
      ><button class="button primary" @click="applyPlan">确认加入我的目标</button>
    </div></Modal
  >
  <Modal v-if="publishing" title="分享你的成长" @close="publishing = false"
    ><form class="editor-form" @submit.prevent="publish">
      <div class="segmented">
        <button
          v-for="(label, type) in { article: '图文', video: '视频', plan: '计划' }"
          :key="type"
          type="button"
          :class="{ active: draft.type === type }"
          @click="setPostType(type)"
        >
          {{ label }}
        </button>
      </div>
      <label
        >内容标题<input
          v-model="draft.title"
          required
          minlength="2"
          maxlength="60"
          placeholder="记录一个真实的小进步" /></label
      ><label
        >正文<textarea
          v-model="draft.content"
          required
          maxlength="5000"
          rows="5"
          placeholder="分享过程、收获，或者下一步计划…"
        />
      </label>
      <div class="form-grid">
        <label
          >成长话题<select v-model="draft.topic">
            <option v-for="item in topics.slice(1)" :key="item">{{ item }}</option>
          </select></label
        ><label
          >发布到社群<select v-model="draft.circleId">
            <option value="">公开演示社区</option>
            <option
              v-for="circle in proto.data.circles.filter((c) => c.joined)"
              :key="circle.id"
              :value="circle.id"
            >
              {{ circle.name }}
            </option>
          </select></label
        >
      </div>
      <template v-if="draft.type === 'plan'"
        ><label
          >分享自己的目标<select v-model="draft.goalId">
            <option value="">使用现有模板</option>
            <option v-for="goal in app.data.goals" :key="goal.id" :value="goal.id">
              {{ goal.title }}
            </option>
          </select></label
        ><label v-if="!draft.goalId"
          >关联模板<select v-model="draft.templateId">
            <option v-for="template in growthTemplates" :key="template.id" :value="template.id">
              {{ template.title }}
            </option>
          </select></label
        ></template
      ><label v-else
        >本地{{ draft.type === 'video' ? '视频' : '图片' }}（小于 1 MB）<input
          type="file"
          :accept="draft.type === 'video' ? 'video/*' : 'image/*'"
          :multiple="draft.type === 'article'"
          @change="upload" /></label
      ><button v-if="draft.type === 'video'" type="button" class="text-button" @click="demoVideo">
        使用原创演示片段
      </button>
      <p v-if="draft.media" class="positive small-text">✓ 已选择媒体</p>
      <div v-if="draft.type === 'article' && draft.images.length" class="publish-image-preview">
        <img
          v-for="(source, index) in draft.images"
          :key="index"
          :src="source"
          :alt="`预览图片${index + 1}`"
        />
      </div>
      <p class="field-hint">只在本机发布，不上传、不连接真实用户。</p>
      <div class="modal-actions">
        <button class="button secondary" type="button" @click="publishing = false">取消</button
        ><button class="button primary" type="submit">发布到演示社区</button>
      </div>
    </form></Modal
  >
</template>
