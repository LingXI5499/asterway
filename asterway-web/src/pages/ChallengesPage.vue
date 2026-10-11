<script setup lang="ts">
import { computed, ref } from 'vue';
import { usePrototypeStore } from '../store/prototype';
import { useAppStore } from '../store/app';
import PageHeader from '../components/PageHeader.vue';
import Icon from '../components/Icon.vue';
import { getTier } from '../domain/prototype';
const proto = usePrototypeStore(),
  app = useAppStore(),
  tab = ref('挑战广场'),
  board = ref('成长总榜');
const challenges = computed(() =>
  proto.data.challenges.filter(
    (c) => tab.value !== '我的挑战' || proto.data.joinedChallengeIds.includes(c.id),
  ),
);
const ranking = computed(() =>
  [
    ...proto.data.users.map((u) => ({
      ...u,
      score: board.value === '习惯榜' ? Math.floor(u.score * 0.6) : u.score,
    })),
    { ...proto.me, score: board.value === '习惯榜' ? app.data.habitLogs.length * 10 : proto.score },
  ].sort((a, b) => b.score - a.score),
);
</script>
<template>
  <PageHeader
    title="挑战与榜单"
    subtitle="以行动参加，以成长获胜。给每天的坚持一点游戏感。"
    eyebrow="PLAY THE LONG GAME"
    ><RouterLink class="button secondary" to="/achievements"
      ><Icon name="award" :size="17" />我的段位与徽章</RouterLink
    ></PageHeader
  >
  <section class="challenge-hero">
    <div>
      <span class="eyebrow">DAILY GROWTH CHALLENGE</span>
      <h2>每天一场小小的比赛。<br />对手，是昨天的自己。</h2>
      <p>完成真实任务和习惯，赢取挑战积分。先建立节奏，再谈排名。</p>
      <div class="hero-score">
        <strong>{{ proto.score }}</strong
        ><span>成长总分 · {{ app.xp }} 行动 XP + {{ proto.rewardPoints }} 挑战积分</span>
      </div>
    </div>
    <div class="rank-emblem" :style="{ '--tier-color': proto.tier.color }">
      <Icon name="trophy" :size="44" /><strong>{{ proto.tier.mark }}</strong
      ><span>{{ proto.tier.title }}</span>
    </div>
  </section>
  <div class="community-tabs">
    <button
      v-for="item in ['挑战广场', '我的挑战']"
      :key="item"
      :class="{ active: tab === item }"
      @click="tab = item"
    >
      {{ item }}</button
    ><span class="push-right muted small-text">奖励只能领取一次，每日赛每天一次</span>
  </div>
  <div class="challenge-grid">
    <article v-for="challenge in challenges" :key="challenge.id" class="card challenge-card">
      <div :class="['challenge-art', challenge.cover]">
        <Icon
          :name="
            challenge.kind === 'daily' ? 'trophy' : challenge.kind === 'habit' ? 'flame' : 'clock'
          "
          :size="38"
        /><span>{{
          challenge.kind === 'daily'
            ? '每日大赛'
            : challenge.kind === 'habit'
              ? '习惯挑战'
              : '专注挑战'
        }}</span
        ><strong>+{{ challenge.reward }}<small>积分</small></strong>
      </div>
      <h3>{{ challenge.title }}</h3>
      <p>{{ challenge.description }}</p>
      <div class="challenge-progress">
        <span>{{ proto.challengeProgress(challenge).label }}</span
        ><strong
          >{{
            Math.round(
              (proto.challengeProgress(challenge).count /
                proto.challengeProgress(challenge).target) *
                100,
            )
          }}%</strong
        >
      </div>
      <div class="progress-track large">
        <span
          :style="{
            width: `${(proto.challengeProgress(challenge).count / proto.challengeProgress(challenge).target) * 100}%`,
          }"
        />
      </div>
      <footer>
        <small
          >{{
            challenge.participants + Number(proto.data.joinedChallengeIds.includes(challenge.id))
          }}
          位示例参加者</small
        ><button
          v-if="!proto.data.joinedChallengeIds.includes(challenge.id)"
          class="button primary"
          @click="proto.joinChallenge(challenge.id)"
        >
          报名挑战</button
        ><button
          v-else
          class="button secondary"
          :disabled="
            proto.rewardClaimed(challenge) ||
            proto.challengeProgress(challenge).count < proto.challengeProgress(challenge).target
          "
          @click="proto.claim(challenge)"
        >
          {{ proto.rewardClaimed(challenge) ? '奖励已领取' : '领取奖励' }}
        </button>
      </footer>
    </article>
  </div>
  <p v-if="!challenges.length" class="muted prototype-footnote">
    还没有报名，选择一个符合当前节奏的挑战吧。
  </p>
  <section class="card leaderboard">
    <div class="section-heading">
      <h2><Icon name="trophy" :size="20" />成长榜单</h2>
      <div class="segmented">
        <button
          v-for="item in ['成长总榜', '习惯榜']"
          :key="item"
          :class="{ active: board === item }"
          @click="board = item"
        >
          {{ item }}
        </button>
      </div>
    </div>
    <p class="muted small-text">
      示例同行者的分数用于展示样式；你的分数来自本地真实记录。习惯榜按打卡次数 × 10。
    </p>
    <div
      v-for="(user, index) in ranking"
      :key="user.id"
      :class="['leader-row', { self: user.id === 'me' }]"
    >
      <span class="leader-number">{{ String(index + 1).padStart(2, '0') }}</span
      ><RouterLink :to="`/profile/${user.id}`"
        ><span :class="['social-avatar', user.color]">{{ user.name.slice(0, 1) }}</span>
        <div>
          <strong>{{ user.name }}<small v-if="user.id === 'me'">你</small></strong
          ><span>{{ user.persona }}</span>
        </div></RouterLink
      ><span class="tier-label">{{
        getTier(
          user.id === 'me' ? proto.score : proto.data.users.find((u) => u.id === user.id)!.score,
        ).title
      }}</span
      ><strong>{{ user.score }}</strong>
    </div>
  </section>
</template>
