<!-- 渲染通知详情界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
/* 引入 vue 提供的组件、状态函数或请求方法。 */
import { ref, computed, watch, onBeforeUnmount } from "vue";
/* 引入 vue-router 提供的组件、状态函数或请求方法。 */
import { useRouter } from "vue-router";
/* 引入 ../../api/personal 提供的组件、状态函数或请求方法。 */
import {
  notificationApi,
  handleStates,
  instantText,
  signalChange,
} from "../../api/personal";
/* 引入 ../../api/growth 提供的组件、状态函数或请求方法。 */
import { canGrow } from "../../api/growth";
/* 引入 ../../stores/personal 提供的组件、状态函数或请求方法。 */
import { refreshUnread } from "../../stores/personal";
/* 引入 ../../api/ui 提供的组件、状态函数或请求方法。 */
import { confirmAction, report } from "../../api/ui";
/* 引入 element-plus 提供的组件、状态函数或请求方法。 */
import { ElMessage } from "element-plus";
/* 引入 ./LoadState.vue 提供的组件、状态函数或请求方法。 */
import LoadState from "./LoadState.vue";
const /* 父组件传入的数据（props）保存声明父组件可以传入的属性及其类型或默认值。 */ props = defineProps({ /* 模型值（modelValue）字段使用可为空的真假值（Boolean）。 */ modelValue: Boolean, /* 编号（id）字段使用表达式 [String, Number] 的结果。 */ id: [String, Number] });
const /* 向父组件发送事件的函数（emit）保存声明当前组件可以通知父组件的事件。 */ emit = defineEmits(["update:modelValue", "changed"]);
const /* HTTP 路由登记器（router）保存取得用于切换页面的路由器。 */ router = useRouter(),
  /* 当前记录（row）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  row = ref(null),
  /* 正在处理（busy）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  busy = ref(false),
  /* 等待处理（pending）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  pending = ref(""),
  /* 错误（error）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  error = ref("");
const /* 是否显示（visible）保存随相关状态变化自动重算的值。 */ visible = computed({
  /* 读取（get）字段使用表达式 () => props.modelValue 的结果。 */
  get: () => props.modelValue,
  /* 设置（set）字段使用表达式 (v) => emit("update:modelValue", v) 的结果。 */
  set: (v) => emit("update:modelValue", v),
});
let /* 当前最新请求序号（sequence）保存固定数值 0。 */ sequence = 0,
  /* 声明 用于取消旧请求的控制器（controller），稍后再填入处理结果。 */
  controller;
/* 加载（load）：读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */
async function load() {
  /* 检查条件 !props.modelValue || !props.id 是否成立，成立时走这一分支。 */
  if (!props.modelValue || !props.id) /* 返回空内容，当前函数到此结束。 */ return;
  const /* seq保存按表达式计算得到的值：++sequence。 */ seq = ++sequence;
  /* 取消尚未完成的旧请求，避免过时结果覆盖当前页面。 */
  controller?.abort();
  /* 把新建 Abort请求入口（AbortController） 实例写入 controller。 */
  controller = new AbortController();
  /* 把真，表示条件成立或开关打开写入 busy.value。 */
  busy.value = true;
  /* 把固定文字 ''写入 error.value。 */
  error.value = "";
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    const /* 数据（data）保存等待 调用 notificationApi.detail 完成详情处理 完成后取得结果。 */ data = await notificationApi.detail(props.id, controller.signal);
    /* 检查条件 seq === sequence 是否成立，成立时走这一分支。 */
    if (seq === sequence) /* 把数据（data）写入 row.value。 */ row.value = data;
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 检查条件 seq === sequence && e.name !== "AbortError" 是否成立，成立时走这一分支。 */
    if (seq === sequence && e.name !== "AbortError") /* 把表达式 e.message 的结果写入 error.value。 */ error.value = e.message;
  } finally {
    /* 检查条件 seq === sequence 是否成立，成立时走这一分支。 */
    if (seq === sequence) /* 把假，表示条件不成立或开关关闭写入 busy.value。 */ busy.value = false;
  }
}
/* act：调用 act 完成act处理。 */
async function act(action) {
  /* 检查条件 pending.value 是否成立，成立时走这一分支。 */
  if (pending.value) /* 返回空内容，当前函数到此结束。 */ return;
  const /* 预计（expected）保存表达式 { ...row.value } 的结果。 */ expected = { ...row.value };
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    /* 检查条件 action === "confirm" 是否成立，成立时走这一分支。 */
    if (action === "confirm")
      /* 等待确认 操作（Action）。 */
      await confirmAction(
        "将该目标确认完成？会再次检查当前达标条件，已读状态和目标完成分别保存。",
        "确认目标完成",
      );
    /* 检查条件 action === "continue" 是否成立，成立时走这一分支。 */
    if (action === "continue")
      /* 等待确认 操作（Action）。 */
      await confirmAction(
        "继续保持目标当前状态？本轮不再重复提醒，之后回落再达标会产生新提醒。",
        "继续推进目标",
      );
    /* 把操作（action）写入 pending.value。 */
    pending.value = action;
    /* 把固定文字 ''写入 error.value。 */
    error.value = "";
    /* 把根据前面的条件，从两个候选结果中选择一个：action === "read" ? await notificationApi.markRead(expected) : await notificationApi.handle(action, …写入 row.value。 */
    row.value =
      action === "read"
        ? await notificationApi.markRead(expected)
        : await notificationApi.handle(action, expected);
    /* 调用 ElMessage.success 完成成功处理。 */
    ElMessage.success(
      action === "read"
        ? "消息已标记为已读"
        : action === "confirm"
          ? "目标已确认完成"
          : "已选择继续保持",
    );
    /* 调用 signalChange 完成取消信号修改处理。 */
    signalChange(action === "read" ? "notification" : "goal");
    /* 向父组件发送事件，通知数据或界面状态发生变化。 */
    emit("changed");
    /* 等待刷新 Unread。 */
    await refreshUnread();
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 检查条件 ["cancel", "close"].includes(e) 是否成立，成立时走这一分支。 */
    if (["cancel", "close"].includes(e)) /* 返回空内容，当前函数到此结束。 */ return;
    /* 检查条件 e.status === 409 是否成立，成立时走这一分支。 */
    if (e.status === 409) {
      /* 等待读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */
      await load();
      /* 把按表达式计算得到的值：e.message + "，已读取最新状态"写入 error.value。 */
      error.value = e.message + "，已读取最新状态";
      /* 向父组件发送事件，通知数据或界面状态发生变化。 */
      emit("changed");
    } else /* 把优先使用前面的有效值，没有时采用后面的备用值：e.message || "操作失败，请重试"写入 error.value。 */ error.value = e.message || "操作失败，请重试";
  } finally {
    /* 把固定文字 ''写入 pending.value。 */
    pending.value = "";
  }
}
/* 取消尚未完成的旧请求，避免过时结果覆盖当前页面。 */
watch(
  () => [props.modelValue, props.id],
  () => {
    /* 推进这次请求或循环的序号，使较早返回的结果可以被识别并丢弃。 */
    sequence++;
    /* 把空值，表示当前还没有对应内容写入 row.value。 */
    row.value = null;
    /* 检查条件 props.modelValue 是否成立，成立时走这一分支。 */
    if (props.modelValue) /* 读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */ load();
    else /* 取消尚未完成的旧请求，避免过时结果覆盖当前页面。 */ controller?.abort();
  },
  { /* immediate字段使用真，表示条件成立或开关打开。 */ immediate: true },
);
/* 取消尚未完成的旧请求，避免过时结果覆盖当前页面。 */
onBeforeUnmount(() => {
  /* 推进这次请求或循环的序号，使较早返回的结果可以被识别并丢弃。 */
  sequence++;
  /* 取消尚未完成的旧请求，避免过时结果覆盖当前页面。 */
  controller?.abort();
});
</script>
<template>
  <!-- 显示Element Plus 的 drawer 组件；与 visible 双向同步；title 固定为 站内消息；size 固定为 min(720px, 100vw)；启用 destroy-on-close 属性；before-close 取自 (done) => { if (!pending) done(); }。 -->
  <el-drawer
    v-model="visible"
    title="站内消息"
    size="min(720px, 100vw)"
    destroy-on-close
    :before-close="
      (done) => {
        if (!pending) done();
      }
    "
  >
    <!-- 显示加载当前状态（LoadState）组件；loading 取自 busy && !row；error 取自 error；retry 事件触发时执行 load。 -->
    <LoadState :loading="busy && !row" :error="error" @retry="load">
      <!-- 显示只组织渲染规则、不额外生成普通容器的模板；仅当 row 成立时渲染。 -->
      <template v-if="row">
        <!-- 显示二级标题。 -->
        <h2><!-- 把 row.title 的当前值转成文字显示。 -->{{ row.title }}</h2>
        <!-- 显示一个布局容器；用样式类 toolbar 控制外观。 -->
        <div class="toolbar">
          <!-- 显示Element Plus 的 tag 组件；type 取自 row.handleStatus === 'PENDING' ? 'warning' : row.handleStatus === 'CONFIRMED' ? 'success' : 'info'。 -->
          <el-tag
            :type="
              row.handleStatus === 'PENDING'
                ? 'warning'
                : row.handleStatus === 'CONFIRMED'
                  ? 'success'
                  : 'info'
            "
            ><!-- 把 handleStates[row.handleStatus] 的当前值转成文字显示。 -->{{ handleStates[row.handleStatus] }}</el-tag
          ><!-- 显示Element Plus 的 tag 组件；type 取自 row.readAt ? 'info' : 'primary'。 --><el-tag :type="row.readAt ? 'info' : 'primary'"><!-- 把 row.readAt ? "已读" : "未读" 的当前值转成文字显示。 -->{{
            row.readAt ? "已读" : "未读"
          }}</el-tag>
        </div>
        <!-- 显示一段文字；用样式类 preserve-text notice-content 控制外观。 -->
        <p class="preserve-text notice-content"><!-- 把 row.content 的当前值转成文字显示。 -->{{ row.content }}</p>
        <!-- 显示Element Plus 的 descriptions 组件；column 取自 1；启用 border 属性。 -->
        <el-descriptions :column="1" border>
          <!-- 显示Element Plus 的 descriptions-item 组件；label 固定为 发送时间。 -->
          <el-descriptions-item label="发送时间"><!-- 把 instantText(row.createdAt) 的当前值转成文字显示。 -->{{
            instantText(row.createdAt)
          }}</el-descriptions-item>
          <!-- 显示Element Plus 的 descriptions-item 组件；label 固定为 首次已读。 -->
          <el-descriptions-item label="首次已读"><!-- 把 instantText(row.readAt) 的当前值转成文字显示。 -->{{
            instantText(row.readAt)
          }}</el-descriptions-item>
          <!-- 显示Element Plus 的 descriptions-item 组件；label 固定为 业务处理。 -->
          <el-descriptions-item label="业务处理"><!-- 把 instantText(row.handledAt) 的当前值转成文字显示。 -->{{
            instantText(row.handledAt)
          }}</el-descriptions-item>
        </el-descriptions>
        <!-- 显示Element Plus 的 alert 组件；仅当 !row.canHandle && row.handleStatus === 'PENDING' 成立时渲染；用样式类 detail-toolbar 控制外观；title 固定为 来源状态或达标轮次已变化，当前不能确认旧提醒，请重新读取状态；类型设为 info；closable 取自 false。 -->
        <el-alert
          v-if="!row.canHandle && row.handleStatus === 'PENDING'"
          class="detail-toolbar"
          title="来源状态或达标轮次已变化，当前不能确认旧提醒，请重新读取状态"
          type="info"
          :closable="false"
        />
        <!-- 显示Element Plus 的 alert 组件；仅当 row.handleStatus === 'INVALIDATED' 成立时渲染；用样式类 detail-toolbar 控制外观；title 固定为 这条提醒已失效，保留消息历史，不再计入未读数；类型设为 info；closable 取自 false。 -->
        <el-alert
          v-if="row.handleStatus === 'INVALIDATED'"
          class="detail-toolbar"
          title="这条提醒已失效，保留消息历史，不再计入未读数"
          type="info"
          :closable="false"
        />
        <!-- 显示一个布局容器；用样式类 toolbar detail-toolbar 控制外观。 -->
        <div class="toolbar detail-toolbar">
          <!-- 显示Element Plus 的 button 组件；仅当 !row.readAt && canGrow('notification:message:mark_read') 成立时渲染；disabled 取自 !!pending；click 事件触发时执行 act('read')。 -->
          <el-button
            v-if="!row.readAt && canGrow('notification:message:mark_read')"
            :disabled="!!pending"
            @click="act('read')"
            >标为已读</el-button
          >
          <!-- 显示Element Plus 的 button 组件；仅当 canGrow('notification:message:handle') 成立时渲染；类型设为 primary；disabled 取自 !row.canHandle || !!pending；loading 取自 pending === 'confirm'；click 事件触发时执行 act('confirm')。 -->
          <el-button
            v-if="canGrow('notification:message:handle')"
            type="primary"
            :disabled="!row.canHandle || !!pending"
            :loading="pending === 'confirm'"
            @click="act('confirm')"
            >确认目标完成</el-button
          >
          <!-- 显示Element Plus 的 button 组件；仅当 canGrow('notification:message:handle') 成立时渲染；disabled 取自 !row.canHandle || !!pending；loading 取自 pending === 'continue'；click 事件触发时执行 act('continue')。 -->
          <el-button
            v-if="canGrow('notification:message:handle')"
            :disabled="!row.canHandle || !!pending"
            :loading="pending === 'continue'"
            @click="act('continue')"
            >继续保持</el-button
          >
          <!-- 显示Element Plus 的 button 组件；仅当 canGrow('goal:goal:read') 成立时渲染；启用 text 属性；click 事件触发时执行 visible = false; router.push('/growth/goals/' + row.goalId);。 -->
          <el-button
            v-if="canGrow('goal:goal:read')"
            text
            @click="
              visible = false;
              router.push('/growth/goals/' + row.goalId);
            "
            >查看来源目标</el-button
          >
        </div>
        <!-- 显示一段文字；用样式类 hint 控制外观。 -->
        <p class="hint">
          标记已读不会完成目标；确认完成或继续保持也不会伪造已读时间。
        </p>
      </template>
    </LoadState>
  </el-drawer>
</template>
