<script setup>
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { api, session, loadCurrent, clearSession } from "../api/http";
import {
  report,
  confirmAction,
  dateText,
  validateAccount,
  validatePassword,
} from "../api/ui";
import TimeZoneSelect from "../components/TimeZoneSelect.vue";
const props = defineProps({ forced: Boolean });
const router = useRouter(),
  tab = ref("profile"),
  busy = ref(false),
  error = ref(""),
  profile = ref({}),
  password = ref({}),
  zone = ref(null),
  selectedZone = ref("");
const user = computed(() => session.user || {});
async function load() {
  try {
    await loadCurrent();
    profile.value = {
      username: user.value.username,
      nickname: user.value.nickname,
      version: user.value.version,
    };
    if (!props.forced) {
      zone.value = await api("/me/time-zone");
      selectedZone.value = zone.value.pendingTimeZone || zone.value.timeZone;
    }
  } catch (e) {
    error.value = e.message;
  }
}
async function save(kind) {
  if (busy.value) return;
  busy.value = true;
  error.value = "";
  try {
    if (kind === "profile") {
      validateAccount(profile.value);
      await api("/me", { method: "PATCH", body: profile.value });
    }
    if (kind === "password") {
      validatePassword(
        password.value.newPassword,
        password.value.confirmPassword,
      );
      if (!password.value.currentPassword) throw new Error("请输入当前密码");
      await confirmAction("修改密码后全部设备都需要重新登录，是否继续？");
      await api("/me/password", {
        method: "PUT",
        body: { ...password.value, version: user.value.version },
      });
      clearSession();
      ElMessage.success("密码已修改，请重新登录");
      return router.replace("/login");
    }
    if (kind === "zone" || kind === "cancelZone") {
      if (!selectedZone.value) throw new Error("请选择时区");
      await api("/me/time-zone", {
        method: "PUT",
        body: {
          timeZone:
            kind === "cancelZone" ? zone.value.timeZone : selectedZone.value,
          version: zone.value.version,
        },
      });
    }
    ElMessage.success("保存成功");
    await load();
  } catch (e) {
    if (!["cancel", "close"].includes(e)) error.value = e.message;
  } finally {
    busy.value = false;
  }
}
onMounted(load);
</script>
<template>
  <section class="card">
    <el-alert
      v-if="forced"
      title="首次登录或密码被重置，请先修改密码"
      type="warning"
      :closable="false"
    /><el-alert
      v-if="error"
      :title="error"
      type="error"
      :closable="false"
    /><el-tabs v-if="!forced" v-model="tab"
      ><el-tab-pane label="基本资料" name="profile" /><el-tab-pane
        label="密码安全"
        name="password" /><el-tab-pane label="时区设置" name="zone" /></el-tabs
    ><el-form
      v-if="!forced && tab === 'profile'"
      label-position="top"
      class="form-narrow"
      @submit.prevent="save('profile')"
      ><el-form-item label="用户名"
        ><el-input v-model="profile.username" maxlength="30" /></el-form-item
      ><el-form-item label="昵称"
        ><el-input v-model="profile.nickname" maxlength="30" /></el-form-item
      ><el-form-item label="当前角色"
        ><el-tag v-for="role in user.roles" :key="role">{{
          role
        }}</el-tag></el-form-item
      ><el-button type="primary" native-type="submit" :loading="busy"
        >保存资料</el-button
      ></el-form
    ><el-form
      v-else-if="forced || tab === 'password'"
      label-position="top"
      class="form-narrow"
      @submit.prevent="save('password')"
      ><el-form-item label="当前密码"
        ><el-input
          v-model="password.currentPassword"
          type="password"
          show-password
          autocomplete="current-password" /></el-form-item
      ><el-form-item label="新密码（8～50 个字符）"
        ><el-input
          v-model="password.newPassword"
          type="password"
          show-password
          autocomplete="new-password" /></el-form-item
      ><el-form-item label="确认新密码"
        ><el-input
          v-model="password.confirmPassword"
          type="password"
          show-password
          autocomplete="new-password" /></el-form-item
      ><el-button type="primary" native-type="submit" :loading="busy"
        >修改密码</el-button
      ></el-form
    >
    <div v-else-if="zone" class="form-narrow">
      <p>当前时区：{{ zone.timeZone }}</p>
      <el-alert
        v-if="zone.pendingTimeZone"
        :title="`待生效时区：${zone.pendingTimeZone}，生效时间：${dateText(zone.effectiveAt)}`"
        type="info"
        :closable="false"
      /><TimeZoneSelect v-model="selectedZone" />
      <p class="hint">修改将在原时区次日午夜生效。</p>
      <el-button type="primary" :loading="busy" @click="save('zone')"
        >保存时区</el-button
      ><el-button
        v-if="zone.pendingTimeZone"
        :loading="busy"
        @click="save('cancelZone')"
        >取消待生效修改</el-button
      >
    </div>
  </section>
</template>
