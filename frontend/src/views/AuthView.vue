<script setup>
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
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
        current.user.mustChangePassword
          ? "/change-password"
          : "/account/profile",
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
  } catch (e) {
    ElMessage.error(e.message);
  }
});
</script>
<template>
  <main class="auth-page">
    <section class="auth-card">
      <div class="brand-mark">A</div>
      <h1>{{ setting.siteName }}</h1>
      <p class="muted">{{ register ? "创建你的账户" : "登录你的账户" }}</p>
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
  </main>
</template>
