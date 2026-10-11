<script setup lang="ts">
import { computed } from 'vue';
import type { Post } from '../domain/prototype';
import Icon from './Icon.vue';
const props = defineProps<{ post: Post; large?: boolean }>();
const poster = computed(() =>
  props.post.type === 'video' && props.post.media?.startsWith('/media/')
    ? props.post.media.replace('.webm', '.png')
    : '',
);
</script>
<template>
  <div
    :class="[
      'post-cover',
      post.cover,
      { large, vertical: post.type === 'video', 'has-poster': poster },
    ]"
  >
    <img v-if="post.media && post.type === 'article'" :src="post.media" :alt="post.title" />
    <template v-else-if="poster"
      ><img :src="poster" :alt="post.title" /><span class="cover-play"
        ><Icon name="play" :size="27" /></span
      ><span class="video-label">00:06 · 原创演示</span></template
    >
    <template v-else>
      <span class="cover-kicker">{{ post.topic }}</span>
      <div class="cover-motif">
        <Icon
          :name="post.type === 'plan' ? 'map' : post.type === 'video' ? 'play' : 'study'"
          :size="large ? 56 : 38"
        />
      </div>
      <h3>
        {{
          post.type === 'plan'
            ? '把方向\n变成行动'
            : post.type === 'video'
              ? '慢一点\n更专注'
              : post.topic === '语言成长'
                ? '从今天\n开始表达'
                : post.topic === '创作与阅读'
                  ? '留一点时间\n给喜欢的事'
                  : '一小步\n也算数'
        }}
      </h3>
      <div v-if="post.type === 'plan'" class="cover-plan-steps">
        <span>01 / 确定方向</span><span>02 / 拆解行动</span><span>03 / 持续复盘</span>
      </div>
      <span class="cover-footer">ASTERWAY · GROW YOUR WAY</span>
      <span v-if="post.type === 'video'" class="video-label"
        ><Icon name="video" :size="12" />视频分享</span
      >
    </template>
  </div>
</template>
