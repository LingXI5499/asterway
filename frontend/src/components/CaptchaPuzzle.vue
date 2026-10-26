<script setup>
import { onMounted, ref } from 'vue'
import { apiRequest } from '../api/http'
import { createIntent, errorText, resultUncertain, sendIntent } from '../api/account'

const props = defineProps({ purpose: { type: String, required: true } })
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
  if (!challenge.value || busy.value || proof.value) {
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
  <fieldset class="captcha" :disabled="busy">
    <legend>滑动拼图验证</legend>
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
    <label v-if="challenge">
      移动拼图到缺口，再点击验证
      <input
        v-model="offset"
        type="range"
        min="0"
        :max="challenge.imageWidth - challenge.pieceWidth"
        step="0.5"
        :disabled="Boolean(proof) || Boolean(verifyIntent)"
      />
    </label>
    <div class="button-row">
      <button type="button" :disabled="!challenge || Boolean(proof)" @click="verify">
        {{ busy ? '处理中…' : '验证拼图位置' }}
      </button>
      <button type="button" class="secondary" @click="refresh">换一张</button>
    </div>
    <p v-if="message" role="status">{{ message }}</p>
  </fieldset>
</template>
