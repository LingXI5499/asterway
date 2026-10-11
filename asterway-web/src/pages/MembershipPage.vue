<script setup lang="ts">
import { usePrototypeStore } from '../store/prototype';
import { useAppStore } from '../store/app';
import PageHeader from '../components/PageHeader.vue';
import Icon from '../components/Icon.vue';
const proto = usePrototypeStore(),
  app = useAppStore();
const plans = [
  {
    id: 'free' as const,
    name: '成长探索者',
    subtitle: '从自己的第一步开始',
    price: '免费',
    features: ['目标、任务与习惯', '成长统计与基础模板', '参与社区和挑战', '本地成长记录'],
  },
  {
    id: 'plus' as const,
    name: '星行 Plus',
    subtitle: '让成长更有方向',
    price: '待定',
    features: [
      '更丰富的专项模板 · 规划',
      '更深入的 AI 伙伴 · 规划',
      '成长档案与主题标识 · 原型',
      '知识库扩展与回顾 · 规划',
    ],
  },
  {
    id: 'pro' as const,
    name: '星行 Pro',
    subtitle: '探索更完整的成长系统',
    price: '待定',
    features: [
      '个性化长期成长路线 · 规划',
      '高级伙伴执行能力 · 规划',
      '专属社群与主题挑战 · 规划',
      '跨设备与进阶统计 · 规划',
    ],
  },
];
function preview(id: 'free' | 'plus' | 'pro') {
  proto.data.membership = id;
  app.toast('已切换演示会员身份，没有扣费或开通真实权益');
}
</script>
<template>
  <PageHeader
    title="会员与权益"
    subtitle="先讨论需要哪些能力，再决定如何提供长期价值。"
    eyebrow="A BETTER GROWTH EXPERIENCE"
    ><span class="local-badge"
      ><Icon name="shield" :size="15" />会员方案原型 · 不含支付</span
    ></PageHeader
  >
  <div class="membership-heading">
    <Icon name="crown" :size="40" />
    <h2>为持续成长，提供更好的陪伴。</h2>
    <p>权益、分层和定价尚未确定。此页用于比较方案与展示会员身份。</p>
  </div>
  <div class="membership-grid">
    <article
      v-for="plan in plans"
      :key="plan.id"
      :class="[
        'card membership-card',
        { featured: plan.id === 'plus', chosen: proto.data.membership === plan.id },
      ]"
    >
      <span class="membership-kicker">{{
        plan.id === 'free'
          ? 'START YOUR WAY'
          : plan.id === 'plus'
            ? 'GROW WITH PURPOSE'
            : 'EXPLORE FURTHER'
      }}</span>
      <h2>{{ plan.name }}</h2>
      <p>{{ plan.subtitle }}</p>
      <div class="membership-price">
        {{ plan.price
        }}<small>{{ plan.id === 'free' ? '当前原型可体验' : '定价与真实权益待确认' }}</small>
      </div>
      <ul>
        <li v-for="feature in plan.features" :key="feature">
          <Icon name="check" :size="15" />{{ feature }}
        </li>
      </ul>
      <button
        :class="['button', plan.id === 'plus' ? 'primary' : 'secondary', 'full-width']"
        @click="preview(plan.id)"
      >
        {{ proto.data.membership === plan.id ? '当前演示身份' : '预览该身份' }}
      </button>
    </article>
  </div>
  <div class="gentle-note">
    <Icon name="shield" :size="20" />
    <p>没有订单、订阅或真实支付。切换身份不会锁住原型功能，也不授予实际服务权益。</p>
  </div>
</template>
