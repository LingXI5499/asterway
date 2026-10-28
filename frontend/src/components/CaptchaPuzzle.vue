<script setup>
import { onMounted, ref } from 'vue'
import { apiRequest } from '../api/http'
import { createIntent, errorText, resultUncertain, sendIntent } from '../api/account'

const props = defineProps({
  purpose: { type: String, required: true },
  modelValue: { type: Object, default: null },
  disabled: Boolean,
})
const emit = defineEmits(['update:modelValue'])
const challenge = ref(null)
const proof = ref(null)
const offset = ref(0)
const busy = ref(false)
const message = ref('')
let verifyIntent = null
let createKey = null

// 验证失败后必须换图；不把滑条拖到底当成验证通过。
async function refresh() {
  if (busy.value) {
    return
  }
  busy.value = true
  proof.value = null
  challenge.value = null
  emit('update:modelValue', null)
  message.value = ''
  verifyIntent = null
  offset.value = 0
  createKey = createKey || crypto.randomUUID()
  try {
    challenge.value = await apiRequest('/auth/captcha-challenges', {
      method: 'POST',
      body: { purpose: props.purpose },
      idempotencyKey: createKey,
    })
    createKey = null
  } catch (error) {
    message.value = errorText(error)
    if (!resultUncertain(error)) {
      createKey = null
    }
  } finally {
    busy.value = false
  }
}

async function verify() {
  if (!challenge.value || busy.value || proof.value || props.disabled) {
    return
  }
  busy.value = true
  message.value = ''
  verifyIntent = verifyIntent || createIntent({ offsetX: Number(offset.value) })
  try {
    const path = `/auth/captcha-challenges/${challenge.value.challengeId}/verify`
    proof.value = await sendIntent(path, 'POST', verifyIntent)
    emit('update:modelValue', proof.value)
    message.value = '验证通过'
  } catch (error) {
    message.value = `${errorText(error)} 请换图重新验证，或重试原验证请求。`
  } finally {
    busy.value = false
  }
}

onMounted(refresh)
defineExpose({ reset: refresh })
</script>

<template>
  <section class="captcha-panel" aria-label="滑动拼图验证">
    <div class="captcha-heading">
      <span>滑动拼图验证</span>
      <el-tag :type="proof ? 'success' : 'info'" size="small" effect="light">
        {{ proof ? '已通过' : '待验证' }}
      </el-tag>
    </div>
    <el-skeleton v-if="busy && !challenge" :rows="3" animated />
    <div v-if="challenge" class="captcha-board">
      <img :src="challenge.backgroundImage" alt="将拼图移动到背景缺口处" />
      <img
        :src="challenge.pieceImage"
        alt="可移动拼图"
        class="captcha-piece"
        :style="{
          width: `${(challenge.pieceWidth / challenge.imageWidth) * 100}%`,
          left: `${(offset / challenge.imageWidth) * 100}%`,
          top: `${(challenge.pieceTop / challenge.imageHeight) * 100}%`,
        }"
      />
    </div>
    <template v-if="challenge">
      <p class="captcha-instruction">拖动滑条，将拼图移至缺口，再点击验证。</p>
      <div class="captcha-slider">
        <el-slider
          v-model="offset"
          :min="0"
          :max="challenge.imageWidth - challenge.pieceWidth"
          :step="0.5"
          :show-tooltip="false"
          :disabled="disabled || busy || Boolean(proof) || Boolean(verifyIntent)"
          aria-label="拼图水平位移"
        />
      </div>
    </template>
    <div class="captcha-actions">
      <el-button
        type="primary"
        plain
        :loading="busy && Boolean(challenge)"
        :disabled="disabled || busy || !challenge || Boolean(proof)"
        @click="verify"
      >
        验证拼图位置
      </el-button>
      <el-button :disabled="disabled || busy" @click="refresh">换一张</el-button>
    </div>
    <el-alert
      v-if="message"
      :title="message"
      :type="proof ? 'success' : 'error'"
      :closable="false"
      show-icon
    />
  </section>
</template>
