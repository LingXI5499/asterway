<script setup>
import { ref, onMounted, watch } from "vue";
import { useRouter } from "vue-router";
import { defaultPath } from "../router";
import { ElMessage } from "element-plus";
import CaptchaPuzzle from "../components/CaptchaPuzzle.vue";
import { api, setToken, loadCurrent, clearSession } from "../api/http";
import { validateAccount } from "../api/ui";
const props = defineProps({ register: Boolean });
const router = useRouter(),
  nonce = crypto.randomUUID(),
  captcha = ref(""),
  captchaRef = ref(null),
  busy = ref(false);
const setting = ref({ siteName: "Asterway", registrationEnabled: false });
const settingsReady = ref(false),
  settingsError = ref("");
watch(
  () => [props.register, setting.value.siteName],
  () => {
    document.title = `${props.register ? "注册" : "登录"} · ${setting.value.siteName || "Asterway"}`;
  },
  { immediate: true },
);
const form = ref({
  username: "",
  password: "",
  confirmPassword: "",
});
async function submit() {
  if (busy.value) return;
  if (!form.value.username || !form.value.password || !captcha.value)
    return ElMessage.warning("请填写账户、密码并完成拼图验证");
  try {
    validateAccount(form.value);
  } catch (e) {
    return ElMessage.warning(e.message);
  }
  busy.value = true;
  try {
    const body = props.register
      ? { ...form.value, captchaTicket: captcha.value, clientNonce: nonce }
      : {
          username: form.value.username,
          password: form.value.password,
          captchaTicket: captcha.value,
          clientNonce: nonce,
        };
    if (
      props.register &&
      (form.value.password !== form.value.confirmPassword ||
        Array.from(form.value.password).length < 8 ||
        Array.from(form.value.password).length > 50)
    )
      throw new Error("密码须为 8～50 个字符，两次输入应一致");
    const result = await api(props.register ? "/registrations" : "/sessions", {
      method: "POST",
      body,
    });
    if (props.register) {
      ElMessage.success("注册成功，请登录");
      router.push("/login");
    } else {
      clearSession();
      setToken(result.token);
      const current = await loadCurrent();
      router.push(
        current.user.mustChangePassword ? "/change-password" : defaultPath(),
      );
    }
  } catch (e) {
    ElMessage.error(e.message);
    captchaRef.value?.refresh();
  } finally {
    busy.value = false;
  }
}
onMounted(async () => {
  try {
    setting.value = await api("/public/account-settings");
    settingsReady.value = true;
  } catch (e) {
    settingsError.value = e.message;
    ElMessage.error(e.message);
  }
});
</script>
<template>
  <main class="auth-page">
    <div class="auth-theme"><AppThemeSwitch /></div>
    <div class="auth-layout">
      <aside class="auth-intro" aria-label="Asterway 个人成长工作台">
        <div class="logo">
          <span class="brand-symbol">A</span
          ><span class="brand-copy"
            >{{ setting.siteName }}<small>个人成长工作台</small></span
          >
        </div>
        <span class="eyebrow">每一步，都有方向</span>
        <h2>把重要的目标，<br />变成每天的行动。</h2>
        <p>
          规划目标与阶段，安排任务与计划。在专注中积累真实投入，让每天的坚持留下记录。
        </p>
        <div class="auth-steps">
          <span>明确目标</span><AppIcon name="arrow" :size="14" /><span
            >安排行动</span
          ><AppIcon name="arrow" :size="14" /><span>记录成长</span>
        </div>
      </aside>
      <section class="auth-card">
        <div class="brand-mark">A</div>
        <span class="eyebrow">{{ setting.siteName }}</span>
        <h1>{{ register ? "从这里开始成长" : "欢迎回来" }}</h1>
        <p class="muted">
          {{
            register
              ? "创建账户，开启自己的成长工作区。"
              : "登录账户，继续今天的行动。"
          }}
        </p>
        <el-alert
          v-if="settingsError"
          :title="settingsError"
          type="error"
          :closable="false"
        />
        <el-alert
          v-else-if="register && settingsReady && !setting.registrationEnabled"
          title="当前站点暂未开放注册，请联系管理员。"
          type="info"
          :closable="false"
        />
        <el-form label-position="top" @submit.prevent="submit"
          ><el-form-item label="用户名"
            ><el-input
              v-model="form.username"
              maxlength="30"
              autocomplete="username" /></el-form-item
          ><el-form-item label="密码"
            ><el-input
              v-model="form.password"
              type="password"
              show-password
              :autocomplete="
                register ? 'new-password' : 'current-password'
              " /></el-form-item
          ><template v-if="register"
            ><el-form-item label="确认密码"
              ><el-input
                v-model="form.confirmPassword"
                type="password"
                show-password /></el-form-item></template
          ><CaptchaPuzzle
            ref="captchaRef"
            :scene="register ? 'REGISTER' : 'LOGIN'"
            :nonce="nonce"
            @verified="captcha = $event"
          /><el-button
            class="submit"
            type="primary"
            native-type="submit"
            :loading="busy"
            :disabled="register && !setting.registrationEnabled"
            >{{ register ? "创建账户" : "登录" }}</el-button
          ></el-form
        >
        <p class="auth-link">
          <router-link v-if="register" to="/login">返回登录</router-link
          ><router-link v-else-if="setting.registrationEnabled" to="/register"
            >创建账户</router-link
          >
        </p>
      </section>
    </div>
  </main>
</template>
