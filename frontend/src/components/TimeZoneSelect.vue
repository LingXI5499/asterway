<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { apiRequest } from '../api/http'
import { errorText } from '../api/account'

defineProps({ modelValue: { type: String, default: '' }, disabled: Boolean })
const emit = defineEmits(['update:modelValue'])
const zones = ref([])
const errorMessage = ref('')
let timer = null
let requestGeneration = 0

async function search(value = '') {
  const generation = ++requestGeneration
  try {
    const query = value ? `&q=${encodeURIComponent(value)}` : ''
    const result = await apiRequest(`/public/time-zones?limit=100${query}`)
    if (generation === requestGeneration) {
      zones.value = result.items
      errorMessage.value = ''
    }
  } catch (error) {
    if (generation === requestGeneration) {
      errorMessage.value = errorText(error)
    }
  }
}

function input(event) {
  const value = event.target.value
  emit('update:modelValue', value)
  clearTimeout(timer)
  timer = setTimeout(() => search(value), 250)
}

onMounted(() => search())
onUnmounted(() => {
  clearTimeout(timer)
  requestGeneration++
})
</script>

<template>
  <label>
    初始时区
    <input
      :value="modelValue"
      :disabled="disabled"
      list="account-time-zones"
      placeholder="例如 Asia/Shanghai；可输入名称筛选"
      required
      @input="input"
    />
    <datalist id="account-time-zones">
      <option v-for="zone in zones" :key="zone.zoneId" :value="zone.zoneId" />
    </datalist>
    <span v-if="errorMessage" class="error">{{ errorMessage }}</span>
  </label>
</template>
