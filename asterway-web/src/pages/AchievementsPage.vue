<script setup lang="ts">
import { computed } from 'vue';
import { useAppStore } from '../store/app';
import { usePrototypeStore } from '../store/prototype';
import { rankTiers } from '../domain/prototype';
import { streak } from '../domain/selectors';
import PageHeader from '../components/PageHeader.vue';
import Icon from '../components/Icon.vue';
const app = useAppStore(),
  proto = usePrototypeStore();
const done = computed(() => app.data.tasks.filter((t) => t.status === 'done').length),
  longest = computed(() =>
    Math.max(0, ...app.data.habits.map((h) => streak(app.data, h, app.today))),
  );
const badges = computed(() => [
  {
    title: '迈出第一步',
    text: '完成 1 项真实任务',
    icon: 'sprout',
    earned: done.value >= 1,
    progress: `${Math.min(done.value, 1)}/1`,
  },
  {
    title: '行动积累者',
    text: '累计完成 10 项任务',
    icon: 'target',
    earned: done.value >= 10,
    progress: `${Math.min(done.value, 10)}/10`,
  },
  {
    title: '习惯萌芽',
    text: '累计打卡 7 次',
    icon: 'flame',
    earned: app.data.habitLogs.length >= 7,
    progress: `${Math.min(app.data.habitLogs.length, 7)}/7`,
  },
  {
    title: '连续七天',
    text: '每日习惯连续达到 7 天',
    icon: 'calendar',
    earned: longest.value >= 7,
    progress: `${Math.min(longest.value, 7)}/7`,
  },
  {
    title: '专注一刻',
    text: '主动记录 25 分钟专注',
    icon: 'clock',
    earned: app.data.focusSessions.reduce((n, s) => n + s.minutes, 0) >= 25,
    progress: `${app.data.focusSessions.reduce((n, s) => n + s.minutes, 0)}/25`,
  },
  {
    title: '善于回顾',
    text: '保存 1 篇有内容的周复盘',
    icon: 'notebook',
    earned: app.data.weeklyReviews.some((r) => r.wins || r.blockers || r.changes),
    progress: `${app.data.weeklyReviews.filter((r) => r.wins || r.blockers || r.changes).length}/1`,
  },
  {
    title: '分享微光',
    text: '发布 1 篇成长内容',
    icon: 'users',
    earned: proto.data.posts.some((p) => p.authorId === 'me'),
    progress: `${proto.data.posts.filter((p) => p.authorId === 'me').length}/1`,
  },
  {
    title: '挑战完成者',
    text: '领取 1 次挑战奖励',
    icon: 'trophy',
    earned: proto.data.rewards.length > 0,
    progress: `${proto.data.rewards.length}/1`,
  },
]);
const next = computed(() => rankTiers.find((t) => t.min > proto.score)),
  currentProgress = computed(() =>
    next.value
      ? Math.round(((proto.score - proto.tier.min) / (next.value.min - proto.tier.min)) * 100)
      : 100,
  );
</script>
<template>
  <PageHeader
    title="段位与成就"
    subtitle="给认真行动的自己，一份看得见的成长反馈。"
    eyebrow="LEVEL UP, YOUR WAY"
    ><span class="local-badge"
      >已解锁 {{ badges.filter((b) => b.earned).length }}/{{ badges.length }} 枚徽章</span
    ></PageHeader
  >
  <section class="rank-profile">
    <div class="rank-emblem" :style="{ '--tier-color': proto.tier.color }">
      <Icon name="award" :size="43" /><strong>{{ proto.tier.mark }}</strong
      ><span>{{ proto.tier.title }}</span>
    </div>
    <div>
      <span class="eyebrow">YOUR DISCIPLINE RANK</span>
      <h2>
        {{ proto.tier.title }}<small>Lv. {{ app.level }}</small>
      </h2>
      <p>{{ proto.tier.description }}</p>
      <div class="progress-caption">
        <span>{{ proto.score }} 成长总分</span
        ><span>{{
          next ? `距离${next.title}还差 ${next.min - proto.score} 分` : '已达到当前最高段位'
        }}</span>
      </div>
      <div class="progress-track large"><span :style="{ width: `${currentProgress}%` }" /></div>
      <small class="muted">行动 XP + 挑战积分；撤销行动后会重新计算，不惩罚漏打卡。</small>
    </div>
  </section>
  <section>
    <div class="section-heading">
      <h2>成长徽章墙</h2>
      <span class="subtle-label">成就来自真实行动</span>
    </div>
    <div class="badge-grid">
      <article
        v-for="badge in badges"
        :key="badge.title"
        :class="['card badge-card', { earned: badge.earned }]"
      >
        <span class="badge-medal"><Icon :name="badge.icon" :size="32" /></span>
        <h3>{{ badge.title }}</h3>
        <p>{{ badge.text }}</p>
        <span class="tag">{{ badge.earned ? '已解锁' : badge.progress }}</span>
      </article>
    </div>
  </section>
  <section class="card rank-path">
    <h2>六段成长旅程</h2>
    <div class="tier-grid">
      <div
        v-for="tier in rankTiers"
        :key="tier.title"
        :class="{ reached: proto.score >= tier.min }"
        :style="{ '--tier-color': tier.color }"
      >
        <span>{{ tier.mark }}</span
        ><strong>{{ tier.title }}</strong
        ><small>{{ tier.min }} 成长总分</small
        ><Icon v-if="proto.score >= tier.min" name="checkCircle" :size="17" />
      </div>
    </div>
  </section>
  <p class="prototype-footnote">
    段位阈值、积分规则和徽章名称都用于原型讨论，可按你的反馈继续调整。
  </p>
</template>
