<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { apiRequest } from '../api/http'
import { errorText } from '../api/account'

defineProps({ modelValue: { type: String, default: '' }, disabled: Boolean })
const emit = defineEmits(['update:modelValue'])
const zones = ref([])
const errorMessage = ref('')
const loading = ref(false)
let timer = null
let requestGeneration = 0

async function search(value = '') {
  const generation = ++requestGeneration
  loading.value = true
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
  } finally {
    if (generation === requestGeneration) {
      loading.value = false
    }
  }
}

function queueSearch(value) {
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
  <el-form-item label="初始时区" for="register-zone" required>
    <el-select
      id="register-zone"
      class="zone-select"
      :model-value="modelValue"
      :disabled="disabled"
      :loading="loading"
      filterable
      remote
      :remote-method="queueSearch"
      placeholder="输入时区名称筛选，例如 Asia/Shanghai"
      @update:model-value="emit('update:modelValue', $event)"
    >
      <el-option
        v-if="modelValue && !zones.some((zone) => zone.zoneId === modelValue)"
        :label="modelValue"
        :value="modelValue"
      />
      <el-option
        v-for="zone in zones"
        :key="zone.zoneId"
        :label="zone.zoneId"
        :value="zone.zoneId"
      />
    </el-select>
    <p class="field-note">已尝试识别设备时区，也可以搜索后手动选择。</p>
    <el-alert v-if="errorMessage" :title="errorMessage" type="error" :closable="false" />
  </el-form-item>
</template>
