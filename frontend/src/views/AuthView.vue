<!-- 渲染认证页面界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
/* 引入 vue 提供的组件、状态函数或请求方法。 */
import { ref, onMounted, watch } from "vue";
/* 引入 vue-router 提供的组件、状态函数或请求方法。 */
import { useRouter } from "vue-router";
/* 引入 ../router 提供的组件、状态函数或请求方法。 */
import { defaultPath } from "../router";
/* 引入 element-plus 提供的组件、状态函数或请求方法。 */
import { ElMessage } from "element-plus";
/* 引入 ../components/CaptchaPuzzle.vue 提供的组件、状态函数或请求方法。 */
import CaptchaPuzzle from "../components/CaptchaPuzzle.vue";
/* 引入 ../api/http 提供的组件、状态函数或请求方法。 */
import { api, setToken, loadCurrent, clearSession } from "../api/http";
/* 引入 ../api/ui 提供的组件、状态函数或请求方法。 */
import { validateAccount } from "../api/ui";
const /* 父组件传入的数据（props）保存声明父组件可以传入的属性及其类型或默认值。 */ props = defineProps({ /* register字段使用可为空的真假值（Boolean）。 */ register: Boolean });
const /* HTTP 路由登记器（router）保存取得用于切换页面的路由器。 */ router = useRouter(),
  /* nonce保存调用 crypto.randomUUID 完成randomUUID处理。 */
  nonce = crypto.randomUUID(),
  /* captcha保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  captcha = ref(""),
  /* captcha响应式引用（captchaRef）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  captchaRef = ref(null),
  /* 正在处理（busy）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  busy = ref(false);
const /* setting保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ setting = ref({ /* site名称（siteName）字段使用固定文字 'Asterway'。 */ siteName: "Asterway", /* registration是否启用（registrationEnabled）字段使用假，表示条件不成立或开关关闭。 */ registrationEnabled: false });
const /* 配置已就绪（settingsReady）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ settingsReady = ref(false),
  /* 配置错误（settingsError）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  settingsError = ref("");
/* 监听指定状态，在其变化时同步加载数据或清理旧状态。 */
watch(
  () => [props.register, setting.value.siteName],
  () => {
    /* 把根据前面的条件，从两个候选结果中选择一个：`${props.register ? "注册" : "登录"} · ${setting.value.siteName || "Asterway"}`写入 document.title。 */
    document.title = `${props.register ? "注册" : "登录"} · ${setting.value.siteName || "Asterway"}`;
  },
  { /* immediate字段使用真，表示条件成立或开关打开。 */ immediate: true },
);
const /* form保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ form = ref({
  /* 用户名（username）字段使用固定文字 ''。 */
  username: "",
  /* 密码（password）字段使用固定文字 ''。 */
  password: "",
  /* 确认密码（confirmPassword）字段使用固定文字 ''。 */
  confirmPassword: "",
});
/* 提交（submit）：调用 submit 完成提交处理。 */
async function submit() {
  /* 检查条件 busy.value 是否成立，成立时走这一分支。 */
  if (busy.value) /* 返回空内容，当前函数到此结束。 */ return;
  /* 检查条件 !form.value.username || !form.value.password || !captcha.value 是否成立，成立时走这一分支。 */
  if (!form.value.username || !form.value.password || !captcha.value)
    /* 返回调用 ElMessage.warning 完成warning处理，当前函数到此结束。 */
    return ElMessage.warning("请填写账户、密码并完成拼图验证");
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    /* 检查 账号（Account）。 */
    validateAccount(form.value);
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 返回调用 ElMessage.warning 完成warning处理，当前函数到此结束。 */
    return ElMessage.warning(e.message);
  }
  /* 把真，表示条件成立或开关打开写入 busy.value。 */
  busy.value = true;
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    const /* 请求正文（body）保存根据前面的条件，从两个候选结果中选择一个：props.register ? { ...form.value, captchaTicket: captcha.value, clientNonce: nonce } : { username: f…。 */ body = props.register
      ? { ...form.value, /* captcha本次请求序号（captchaTicket）字段使用表达式 captcha.value 的结果。 */ captchaTicket: captcha.value, /* 客户端Nonce（clientNonce）字段使用nonce。 */ clientNonce: nonce }
      : {
          /* 用户名（username）字段使用表达式 form.value.username 的结果。 */
          username: form.value.username,
          /* 密码（password）字段使用表达式 form.value.password 的结果。 */
          password: form.value.password,
          /* captcha本次请求序号（captchaTicket）字段使用表达式 captcha.value 的结果。 */
          captchaTicket: captcha.value,
          /* 客户端Nonce（clientNonce）字段使用nonce。 */
          clientNonce: nonce,
        };
    /* 检查条件 props.register && (form.value.password !== form.value.confirmPassword || Array.from(form.value.password).length < 8 || Array.from(form.value.pass… 是否成立，成立时走这一分支。 */
    if (
      props.register &&
      (form.value.password !== form.value.confirmPassword ||
        Array.from(form.value.password).length < 8 ||
        Array.from(form.value.password).length > 50)
    )
      /* 抛出新建 错误（Error） 实例，当前操作按失败处理。 */
      throw new Error("密码须为 8～50 个字符，两次输入应一致");
    const /* 结果（result）保存等待 根据前面的条件，从两个候选结果中选择一个：api(props.register ? "/registrations" : "/sessions", { method: "POST", body, }) 完成后取得结果。 */ result = await api(props.register ? "/registrations" : "/sessions", {
      /* 方法（method）字段使用固定状态或编码 POST（POST）。 */
      method: "POST",
      /* 直接使用已有的请求正文（body）作为同名字段。 */
      body,
    });
    /* 检查条件 props.register 是否成立，成立时走这一分支。 */
    if (props.register) {
      /* 调用 ElMessage.success 完成成功处理。 */
      ElMessage.success("注册成功，请登录");
      /* 调用 router.push 完成push处理。 */
      router.push("/login");
    } else {
      /* 调用 clearSession 完成清除会话处理。 */
      clearSession();
      /* 设置 身份凭据（Token） 的值。 */
      setToken(result.token);
      const /* 当前（current）保存等待 加载 当前（Current） 完成后取得结果。 */ current = await loadCurrent();
      /* 调用 router.push 完成push处理。 */
      router.push(
        current.user.mustChangePassword ? "/change-password" : defaultPath(),
      );
    }
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 调用 ElMessage.error 完成错误处理。 */
    ElMessage.error(e.message);
    /* 调用 captchaRef.value?.refresh 完成刷新处理。 */
    captchaRef.value?.refresh();
  } finally {
    /* 把假，表示条件不成立或开关关闭写入 busy.value。 */
    busy.value = false;
  }
}
/* 调用 onMounted(async () => { try { setting.value = await api("/public/account-settings"); settingsReady.value = true; } catch (e) { settingsError.value = e.message; ElMessage.error 完成错误处理。 */
onMounted(async () => {
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    /* 把等待 调用 api 完成接口处理 完成后取得结果写入 setting.value。 */
    setting.value = await api("/public/account-settings");
    /* 把真，表示条件成立或开关打开写入 settingsReady.value。 */
    settingsReady.value = true;
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 把表达式 e.message 的结果写入 settingsError.value。 */
    settingsError.value = e.message;
    /* 调用 ElMessage.error 完成错误处理。 */
    ElMessage.error(e.message);
  }
});
</script>
<template>
  <!-- 显示主要内容区；用样式类 auth-page 控制外观。 -->
  <main class="auth-page">
    <!-- 显示一个布局容器；用样式类 auth-theme 控制外观。 -->
    <div class="auth-theme"><!-- 显示应用ThemeSwitch（AppThemeSwitch）组件。 --><AppThemeSwitch /></div>
    <!-- 显示一个布局容器；用样式类 auth-layout 控制外观。 -->
    <div class="auth-layout">
      <!-- 显示侧边区域；用样式类 auth-intro 控制外观；aria-label 提供无障碍说明 Asterway 个人成长工作台。 -->
      <aside class="auth-intro" aria-label="Asterway 个人成长工作台">
        <!-- 显示一个布局容器；用样式类 logo 控制外观。 -->
        <div class="logo">
          <!-- 显示行内文字区域；用样式类 brand-symbol 控制外观。 -->
          <span class="brand-symbol">A</span
          ><!-- 显示行内文字区域；用样式类 brand-copy 控制外观。 --><span class="brand-copy"
            ><!-- 把 setting.siteName 的当前值转成文字显示。 -->{{ setting.siteName }}<!-- 显示辅助说明文字。 --><small>个人成长工作台</small></span
          >
        </div>
        <!-- 显示行内文字区域；用样式类 eyebrow 控制外观。 -->
        <span class="eyebrow">每一步，都有方向</span>
        <!-- 显示二级标题。 -->
        <h2>把重要的目标，<!-- 显示br组件。 --><br />变成每天的行动。</h2>
        <!-- 显示一段文字。 -->
        <p>
          规划目标与阶段，安排任务与计划。在专注中积累真实投入，让每天的坚持留下记录。
        </p>
        <!-- 显示一个布局容器；用样式类 auth-steps 控制外观。 -->
        <div class="auth-steps">
          <!-- 显示行内文字区域。 -->
          <span>明确目标</span><!-- 显示应用Icon（AppIcon）组件；name 固定为 arrow；size 取自 14。 --><AppIcon name="arrow" :size="14" /><!-- 显示行内文字区域。 --><span
            >安排行动</span
          ><!-- 显示应用Icon（AppIcon）组件；name 固定为 arrow；size 取自 14。 --><AppIcon name="arrow" :size="14" /><!-- 显示行内文字区域。 --><span>记录成长</span>
        </div>
      </aside>
      <!-- 显示一个有独立含义的内容区；用样式类 auth-card 控制外观。 -->
      <section class="auth-card">
        <!-- 显示一个布局容器；用样式类 brand-mark 控制外观。 -->
        <div class="brand-mark">A</div>
        <!-- 显示行内文字区域；用样式类 eyebrow 控制外观。 -->
        <span class="eyebrow"><!-- 把 setting.siteName 的当前值转成文字显示。 -->{{ setting.siteName }}</span>
        <!-- 显示一级标题。 -->
        <h1><!-- 把 register ? "从这里开始成长" : "欢迎回来" 的当前值转成文字显示。 -->{{ register ? "从这里开始成长" : "欢迎回来" }}</h1>
        <!-- 显示一段文字；用样式类 muted 控制外观。 -->
        <p class="muted">
          <!-- 把 register ? "创建账户，开启自己的成长工作区。" : "登录账户，继续今天的行动。" 的当前值转成文字显示。 -->
          {{
            register
              ? "创建账户，开启自己的成长工作区。"
              : "登录账户，继续今天的行动。"
          }}
        </p>
        <!-- 显示Element Plus 的 alert 组件；仅当 settingsError 成立时渲染；title 取自 settingsError；类型设为 error；closable 取自 false。 -->
        <el-alert
          v-if="settingsError"
          :title="settingsError"
          type="error"
          :closable="false"
        />
        <!-- 显示Element Plus 的 alert 组件；前一条件不成立且 register && settingsReady && !setting.registrationEnabled 成立时渲染；title 固定为 当前站点暂未开放注册，请联系管理员。；类型设为 info；closable 取自 false。 -->
        <el-alert
          v-else-if="register && settingsReady && !setting.registrationEnabled"
          title="当前站点暂未开放注册，请联系管理员。"
          type="info"
          :closable="false"
        />
        <!-- 显示Element Plus 的 form 组件；label-position 固定为 top；submit 事件触发时执行 submit。 -->
        <el-form label-position="top" @submit.prevent="submit"
          ><!-- 显示Element Plus 的 form-item 组件；label 固定为 用户名。 --><el-form-item label="用户名"
            ><!-- 显示Element Plus 的 input 组件；与 form.username 双向同步；maxlength 固定为 30；autocomplete 固定为 username。 --><el-input
              v-model="form.username"
              maxlength="30"
              autocomplete="username" /></el-form-item
          ><!-- 显示Element Plus 的 form-item 组件；label 固定为 密码。 --><el-form-item label="密码"
            ><!-- 显示Element Plus 的 input 组件；与 form.password 双向同步；类型设为 password；启用 show-password 属性；autocomplete 取自 register ? 'new-password' : 'current-password'。 --><el-input
              v-model="form.password"
              type="password"
              show-password
              :autocomplete="
                register ? 'new-password' : 'current-password'
              " /></el-form-item
          ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；仅当 register 成立时渲染。 --><template v-if="register"
            ><!-- 显示Element Plus 的 form-item 组件；label 固定为 确认密码。 --><el-form-item label="确认密码"
              ><!-- 显示Element Plus 的 input 组件；与 form.confirmPassword 双向同步；类型设为 password；启用 show-password 属性。 --><el-input
                v-model="form.confirmPassword"
                type="password"
                show-password /></el-form-item></template
          ><!-- 显示CaptchaPuzzle组件；把组件或元素引用保存为 captchaRef，供脚本读取；scene 取自 register ? 'REGISTER' : 'LOGIN'；nonce 取自 nonce；verified 事件触发时执行 captcha = $event。 --><CaptchaPuzzle
            ref="captchaRef"
            :scene="register ? 'REGISTER' : 'LOGIN'"
            :nonce="nonce"
            @verified="captcha = $event"
          /><!-- 显示Element Plus 的 button 组件；用样式类 submit 控制外观；类型设为 primary；native-type 固定为 submit；loading 取自 busy；disabled 取自 register && !setting.registrationEnabled。 --><el-button
            class="submit"
            type="primary"
            native-type="submit"
            :loading="busy"
            :disabled="register && !setting.registrationEnabled"
            ><!-- 把 register ? "创建账户" : "登录" 的当前值转成文字显示。 -->{{ register ? "创建账户" : "登录" }}</el-button
          ></el-form
        >
        <!-- 显示一段文字；用样式类 auth-link 控制外观。 -->
        <p class="auth-link">
          <!-- 显示routerlink（router-link）组件；仅当 register 成立时渲染；to 固定为 /login。 -->
          <router-link v-if="register" to="/login">返回登录</router-link
          ><!-- 显示routerlink（router-link）组件；前一条件不成立且 setting.registrationEnabled 成立时渲染；to 固定为 /register。 --><router-link v-else-if="setting.registrationEnabled" to="/register"
            >创建账户</router-link
          >
        </p>
      </section>
    </div>
  </main>
</template>
