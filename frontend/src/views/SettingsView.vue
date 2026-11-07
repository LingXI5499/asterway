<script setup>
import { ref, onMounted } from "vue";
import { ElMessage } from "element-plus";
import { api, hasPermission } from "../api/http";
const form = ref(null),
  busy = ref(false),
  error = ref("");
async function load() {
  busy.value = true;
  try {
    form.value = await api("/settings/account");
    error.value = "";
  } catch (e) {
    error.value = e.message;
  } finally {
    busy.value = false;
  }
}
async function save() {
  if (!form.value.siteName.trim()) {
    error.value = "站点名称不能为空";
    return;
  }
  busy.value = true;
  try {
    await api("/settings/account", { method: "PUT", body: form.value });
    ElMessage.success("设置已保存");
    await load();
  } catch (e) {
    error.value = e.message;
  } finally {
    busy.value = false;
  }
}
onMounted(load);
</script>
<template>
  <section class="card">
    <el-alert
      v-if="error"
      :title="error"
      type="error"
      :closable="false"
    /><el-button v-if="error && !form" @click="load">重新加载设置</el-button
    ><el-skeleton v-if="!form && !error" :rows="3" /><el-form
      v-else-if="form"
      label-position="top"
      class="form-narrow"
      @submit.prevent="save"
      ><el-form-item label="站点名称"
        ><el-input v-model="form.siteName" maxlength="64" /></el-form-item
      ><el-form-item label="开放账户注册"
        ><el-switch v-model="form.registrationEnabled"
      /></el-form-item>
      <p class="hint">关闭注册后，管理员仍可在用户管理中创建账户。</p>
      <el-button
        v-if="hasPermission('identity:setting:update')"
        native-type="submit"
        type="primary"
        :loading="busy"
        >保存设置</el-button
      ><el-button @click="load" :disabled="busy">重新加载</el-button></el-form
    >
  </section>
</template>
