<script setup lang="ts">
import { computed, ref, toRaw } from 'vue';
import { useRouter } from 'vue-router';
import { useAppStore } from '../store/app';
import { usePrototypeStore } from '../store/prototype';
import { planningMethods, methodPlanBundle, type MethodPlanInput } from '../domain/methodPlans';
import type { PlanningMethod } from '../domain/types';
import type { GrowthBundle } from '../domain/prototype';
import Modal from './Modal.vue';
import Icon from './Icon.vue';
import BundlePreview from './BundlePreview.vue';
const props = defineProps<{ initial?: string }>(),
  emit = defineEmits<{ close: [] }>();
const app = useAppStore(),
  proto = usePrototypeStore(),
  router = useRouter(),
  step = ref(1),
  error = ref(''),
  bundle = ref<GrowthBundle | null>(null);
const form = ref<MethodPlanInput>({
  method: (planningMethods.some((m) => m.id === props.initial)
    ? props.initial
    : 'matrix') as PlanningMethod,
  title: '',
  date: app.today,
  startTime: '09:00',
  minutes: 120,
  buffer: 5,
  reserve: 15,
  focusMinutes: 25,
  rows: [{ title: '', minutes: 25, quadrant: 'schedule' }],
});
const method = computed(() => planningMethods.find((m) => m.id === form.value.method)!);
const limit = computed(() => (form.value.method === 'top3' ? 3 : 8));
function choose(id: PlanningMethod) {
  form.value.method = id;
  form.value.rows = form.value.rows.slice(0, id === 'top3' ? 3 : 8);
}
function preview() {
  try {
    error.value = '';
    bundle.value = methodPlanBundle(toRaw(app.data), toRaw(form.value));
    step.value = 3;
  } catch (e) {
    error.value = e instanceof Error ? e.message : '请检查输入';
  }
}
function apply() {
  if (bundle.value && proto.applyBundle(toRaw(bundle.value))) {
    const first = bundle.value.tasks[0]!;
    const method = form.value.method;
    emit('close');
    router.push(
      method === 'gtd'
        ? '/planner'
        : method === 'matrix'
          ? '/methods'
          : method === 'focus'
            ? `/focus?task=${first.id}&duration=${form.value.focusMinutes}`
            : `/planner?date=${form.value.date}`,
    );
  }
}
</script>
<template>
  <Modal class="method-builder" title="新建方法计划" wide @close="emit('close')">
    <div class="builder-steps">
      <span :class="{ active: step === 1 }">01 选择方法</span
      ><span :class="{ active: step === 2 }">02 设置行动</span
      ><span :class="{ active: step === 3 }">03 确认建立</span>
    </div>
    <template v-if="step === 1"
      ><p class="muted">同一个成长方向，可以选择不同的行动方式。</p>
      <div class="builder-methods">
        <button
          v-for="item in planningMethods"
          :key="item.id"
          :class="{ selected: form.method === item.id }"
          @click="choose(item.id)"
        >
          <Icon :name="item.icon" :size="23" /><strong>{{ item.name }}</strong
          ><small>{{ item.hint }}</small>
          <p>{{ item.description }}</p>
        </button>
      </div>
      <div class="modal-actions">
        <button class="button primary" @click="step = 2">
          使用{{ method.name }}继续 <Icon name="arrow" :size="16" />
        </button></div
    ></template>
    <form v-else-if="step === 2" class="editor-form" @submit.prevent="preview">
      <div class="builder-chosen">
        <Icon :name="method.icon" />
        <div>
          <strong>{{ method.name }}</strong>
          <p>{{ method.description }}</p>
        </div>
        <button type="button" class="text-button" @click="step = 1">换个方法</button>
      </div>
      <label
        >计划名称<input
          v-model="form.title"
          required
          minlength="2"
          maxlength="60"
          placeholder="例如：下班后的英语成长"
      /></label>
      <div class="form-grid">
        <label>开始日期<input type="date" v-model="form.date" required /></label
        ><label v-if="form.method === 'focus'"
          >每段专注时长<select v-model="form.focusMinutes">
            <option v-for="m in [15, 25, 45, 50]" :key="m" :value="m">{{ m }} 分钟</option>
          </select></label
        >
      </div>
      <div v-if="['blocks', 'top3'].includes(form.method)" class="form-grid">
        <label>计划开始时间<input v-model="form.startTime" type="time" required /></label
        ><label
          >计划可用分钟<input
            v-model="form.minutes"
            type="number"
            min="15"
            max="720"
            required /></label
        ><label>行动间缓冲<input v-model="form.buffer" type="number" min="0" max="30" /></label
        ><label
          >计划留白<select v-model="form.reserve">
            <option v-for="r in [0, 15, 25, 40]" :value="r" :key="r">{{ r }}%</option>
          </select></label
        >
      </div>
      <p class="builder-rule">
        {{
          form.method === 'top3'
            ? '最多三个重点，按输入顺序安排；时间不足时先调整再建立。'
            : form.method === 'gtd'
              ? '全部行动进入待安排任务池，日期作为计划开始日，不强制排到当天。'
              : form.method === 'frog'
                ? '第一项是关键难事，会成为当天重点并标记为较高精力。'
                : form.method === 'matrix'
                  ? '逐项选择象限，建立后在四象限看板中继续整理。'
                  : form.method === 'focus'
                    ? '行动时长采用上方专注时长，建立后可进入第一项行动的专注空间。'
                    : '按输入顺序安排时段，绕开已有任务，不自动改变原安排。'
        }}
      </p>
      <div class="builder-action-list">
        <div v-for="(row, index) in form.rows" :key="index" class="builder-action">
          <span>{{ String(index + 1).padStart(2, '0') }}</span
          ><label
            >行动{{ index + 1
            }}<input
              v-model="row.title"
              required
              minlength="2"
              maxlength="60"
              :placeholder="
                form.method === 'frog' && index === 0
                  ? '最值得优先推进的关键一步'
                  : '一个明确、可以开始的行动'
              " /></label
          ><label v-if="form.method !== 'focus'"
            >预计分钟<input type="number" v-model="row.minutes" min="1" max="240" required /></label
          ><label v-if="form.method === 'matrix'"
            >所属象限<select v-model="row.quadrant">
              <option value="do">重要 · 紧急</option>
              <option value="schedule">重要 · 不紧急</option>
              <option value="batch">不重要 · 紧急</option>
              <option value="later">不重要 · 不紧急</option>
            </select></label
          ><button
            type="button"
            class="icon-button"
            :aria-label="`移除行动${index + 1}`"
            :disabled="form.rows.length === 1"
            @click="form.rows.splice(index, 1)"
          >
            <Icon name="trash" :size="16" />
          </button>
        </div>
      </div>
      <button
        type="button"
        class="text-button"
        :disabled="form.rows.length >= limit"
        @click="form.rows.push({ title: '', minutes: 25, quadrant: 'schedule' })"
      >
        + 添加行动（{{ form.rows.length }} / {{ limit }}）
      </button>
      <p class="error-message" role="alert" v-if="error">{{ error }}</p>
      <div class="modal-actions">
        <button type="button" class="button secondary" @click="step = 1">上一步</button
        ><button type="submit" class="button primary">预览{{ method.name }}计划</button>
      </div>
    </form>
    <template v-else-if="bundle"
      ><BundlePreview :bundle="bundle" />
      <div class="builder-preview-times">
        <div v-for="task in bundle.tasks" :key="task.id">
          <strong>{{ task.title }}</strong
          ><span
            >{{ task.scheduledDate || '待安排' }} {{ task.startTime || '' }} ·
            {{ task.durationMinutes }} 分钟</span
          >
        </div>
      </div>
      <p class="muted">将建立一个目标与以上行动，所有行动保留所选方法。原有目标和日程保持原样。</p>
      <div class="modal-actions">
        <button class="button secondary" @click="step = 2">继续调整</button
        ><button class="button primary" @click="apply">确认建立方法计划</button>
      </div></template
    >
  </Modal>
</template>
