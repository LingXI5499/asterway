<!-- 渲染通知列表页面界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
/* 引入 vue 提供的组件、状态函数或请求方法。 */
import { ref, reactive } from "vue";
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
import { messages, refreshUnread } from "../../stores/personal";
/* 引入 ../../composables/usePaged 提供的组件、状态函数或请求方法。 */
import { usePaged } from "../../composables/usePaged";
/* 引入 ../../api/ui 提供的组件、状态函数或请求方法。 */
import { report } from "../../api/ui";
/* 引入 element-plus 提供的组件、状态函数或请求方法。 */
import { ElMessage } from "element-plus";
/* 引入 ../../components/personal/NotificationDetails.vue 提供的组件、状态函数或请求方法。 */
import NotificationDetails from "../../components/personal/NotificationDetails.vue";
/* 引入 ../../components/personal/PagePagination.vue 提供的组件、状态函数或请求方法。 */
import PagePagination from "../../components/personal/PagePagination.vue";
/* 引入 ../../components/personal/LoadState.vue 提供的组件、状态函数或请求方法。 */
import LoadState from "../../components/personal/LoadState.vue";
const /* 筛选条件（filters）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ filters = reactive({ /* unread字段使用固定文字 ''。 */ unread: "", /* handle状态（handleStatus）字段使用固定文字 ''。 */ handleStatus: "" }),
  /* 等待处理（pending）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  pending = ref(""),
  /* 详情编号（detailId）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  detailId = ref(""),
  /* opened保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  opened = ref(false);
const /* 列表（list）保存根据前面的条件，从两个候选结果中选择一个：usePaged((params, signal) => notificationApi.list( { ...params, unread: ["true", "false"].includes(f…。 */ list = usePaged((params, signal) =>
  notificationApi.list(
    {
      ...params,
      /* unread字段使用根据前面的条件，从两个候选结果中选择一个：["true", "false"].includes(filters.unread) ? filters.unread === "true" : null。 */
      unread: ["true", "false"].includes(filters.unread)
        ? filters.unread === "true"
        : null,
      /* handle状态（handleStatus）字段使用表达式 filters.handleStatus 的结果。 */
      handleStatus: filters.handleStatus,
    },
    signal,
  ),
);
/* 详情（detail）：调用 detail 完成详情处理。 */
function detail(row) {
  /* 把表达式 row.id 的结果写入 detailId.value。 */
  detailId.value = row.id;
  /* 把真，表示条件成立或开关打开写入 opened.value。 */
  opened.value = true;
}
/* 读取（read）：调用 read 完成读取处理。 */
async function read(row) {
  /* 检查条件 pending.value 是否成立，成立时走这一分支。 */
  if (pending.value) /* 返回空内容，当前函数到此结束。 */ return;
  /* 把表达式 row.id 的结果写入 pending.value。 */
  pending.value = row.id;
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    /* 等待调用 notificationApi.markRead 完成mark读取处理。 */
    await notificationApi.markRead(row);
    /* 调用 signalChange 完成取消信号修改处理。 */
    signalChange("notification");
    /* 等待调用 Promise.all 完成全部处理。 */
    await Promise.all([list.load(), refreshUnread()]);
    /* 调用 ElMessage.success 完成成功处理。 */
    ElMessage.success("已标记为已读");
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 调用 report 完成复盘报告处理。 */
    report(e);
    /* 检查条件 e.status === 409 是否成立，成立时走这一分支。 */
    if (e.status === 409) /* 等待读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */ await list.load();
  } finally {
    /* 把固定文字 ''写入 pending.value。 */
    pending.value = "";
  }
}
/* 全部读取（allRead）：调用 allRead 完成全部读取处理。 */
async function allRead() {
  /* 检查条件 pending.value 是否成立，成立时走这一分支。 */
  if (pending.value) /* 返回空内容，当前函数到此结束。 */ return;
  /* 把固定文字 'all'写入 pending.value。 */
  pending.value = "all";
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    /* 等待调用 notificationApi.markAllRead 完成mark全部读取处理。 */
    await notificationApi.markAllRead();
    /* 调用 signalChange 完成取消信号修改处理。 */
    signalChange("notification");
    /* 等待调用 Promise.all 完成全部处理。 */
    await Promise.all([list.load(), refreshUnread()]);
    /* 调用 ElMessage.success 完成成功处理。 */
    ElMessage.success("当前有效未读消息已全部标记");
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 调用 report 完成复盘报告处理。 */
    report(e);
  } finally {
    /* 把固定文字 ''写入 pending.value。 */
    pending.value = "";
  }
}
</script>
<template>
  <!-- 显示一个有独立含义的内容区；用样式类 card personal-hero 控制外观。 -->
  <section class="card personal-hero">
    <!-- 显示一个布局容器。 -->
    <div>
      <!-- 显示行内文字区域；用样式类 eyebrow 控制外观。 -->
      <span class="eyebrow">NOTIFICATIONS</span>
      <!-- 显示二级标题。 -->
      <h2>重要的进展，等你亲自确认</h2>
      <!-- 显示一段文字；用样式类 muted 控制外观。 -->
      <p class="muted">
        目标达标后由你决定完成或继续保持。当前未读 <!-- 把 messages.unread 的当前值转成文字显示。 -->{{ messages.unread }} 条。
      </p>
    </div>
    <!-- 显示Element Plus 的 button 组件；仅当 canGrow('notification:message:mark_read') 成立时渲染；loading 取自 pending === 'all'；disabled 取自 !!pending || messages.unread === 0；click 事件触发时执行 allRead。 -->
    <el-button
      v-if="canGrow('notification:message:mark_read')"
      :loading="pending === 'all'"
      :disabled="!!pending || messages.unread === 0"
      @click="allRead"
      >全部标为已读</el-button
    >
  </section>
  <!-- 显示一个有独立含义的内容区；用样式类 card 控制外观。 -->
  <section class="card">
    <!-- 显示一个布局容器；用样式类 toolbar 控制外观。 -->
    <div class="toolbar">
      <!-- 显示Element Plus 的 select 组件；与 filters.unread 双向同步；启用 clearable 属性；未输入时提示 全部已读状态。 -->
      <el-select v-model="filters.unread" clearable placeholder="全部已读状态"
        ><!-- 显示Element Plus 的 option 组件；label 固定为 未读；value 固定为 true。 --><el-option label="未读" value="true" /><!-- 显示Element Plus 的 option 组件；label 固定为 已读；value 固定为 false。 --><el-option
          label="已读"
          value="false"
      /></el-select>
      <!-- 显示Element Plus 的 select 组件；与 filters.handleStatus 双向同步；启用 clearable 属性；未输入时提示 全部处理状态。 -->
      <el-select
        v-model="filters.handleStatus"
        clearable
        placeholder="全部处理状态"
        ><!-- 显示Element Plus 的 option 组件；按 (name, code) in handleStates 为每条数据重复生成内容；key 取自 code；label 取自 name；value 取自 code。 --><el-option
          v-for="(name, code) in handleStates"
          :key="code"
          :label="name"
          :value="code"
      /></el-select>
      <!-- 显示Element Plus 的 button 组件；类型设为 primary；click 事件触发时执行 list.load(true)。 -->
      <el-button type="primary" @click="list.load(true)">查询</el-button
      ><!-- 显示Element Plus 的 button 组件；click 事件触发时执行 Object.assign(filters, { unread: '', handleStatus: '' }); list.load(true);。 --><el-button
        @click="
          Object.assign(filters, { unread: '', handleStatus: '' });
          list.load(true);
        "
        >重置</el-button
      >
    </div>
    <!-- 显示加载当前状态（LoadState）组件；loading 取自 list.busy && !list.loaded；error 取自 list.error；retry 事件触发时执行 list.load。 -->
    <LoadState
      :loading="list.busy && !list.loaded"
      :error="list.error"
      @retry="list.load"
    >
      <!-- 显示一个布局容器；用样式类 notice-list 控制外观；loading 指令使用 list.busy。 -->
      <div class="notice-list" v-loading="list.busy">
        <!-- 显示一条独立内容；按 row in list.rows 为每条数据重复生成内容；key 取自 row.id；用样式类 notice-row 控制外观；class 取自 { unread: !row.readAt }。 -->
        <article
          v-for="row in list.rows"
          :key="row.id"
          class="notice-row"
          :class="{ unread: !row.readAt }"
        >
          <!-- 显示一个布局容器；用样式类 notice-icon 控制外观。 -->
          <div class="notice-icon">
            <!-- 显示应用Icon（AppIcon）组件；name 固定为 notification；size 取自 22。 -->
            <AppIcon name="notification" :size="22" />
          </div>
          <!-- 显示一个布局容器；用样式类 notice-copy 控制外观。 -->
          <div class="notice-copy">
            <!-- 显示三级标题。 -->
            <h3>
              <!-- 显示Element Plus 的 button 组件；仅当 canGrow('notification:message:read') 成立时渲染；启用 link 属性；类型设为 primary；click 事件触发时执行 detail(row)。 -->
              <el-button
                v-if="canGrow('notification:message:read')"
                link
                type="primary"
                @click="detail(row)"
                ><!-- 把 row.title 的当前值转成文字显示。 -->{{ row.title }}</el-button
              ><!-- 显示行内文字区域；前面条件均不成立时显示这个备用分支。 --><span v-else><!-- 把 row.title 的当前值转成文字显示。 -->{{ row.title }}</span>
            </h3>
            <!-- 显示一段文字。 -->
            <p><!-- 把 row.content 的当前值转成文字显示。 -->{{ row.content }}</p>
            <!-- 显示一个布局容器；用样式类 notice-meta 控制外观。 -->
            <div class="notice-meta">
              <!-- 显示行内文字区域。 -->
              <span><!-- 把 instantText(row.createdAt) 的当前值转成文字显示。 -->{{ instantText(row.createdAt) }}</span
              ><!-- 显示行内文字区域。 --><span><!-- 把 row.readAt ? "已读" : "未读" 的当前值转成文字显示。 -->{{ row.readAt ? "已读" : "未读" }}</span
              ><!-- 显示应用状态标签（AppStatusTag）组件；value 取自 row.handleStatus。 --><AppStatusTag :value="row.handleStatus" />
            </div>
          </div>
          <!-- 显示一个布局容器；用样式类 notice-actions 控制外观。 -->
          <div class="notice-actions">
            <!-- 显示Element Plus 的 button 组件；仅当 canGrow('notification:message:read') 成立时渲染；启用 text 属性；类型设为 primary；click 事件触发时执行 detail(row)。 -->
            <el-button
              v-if="canGrow('notification:message:read')"
              text
              type="primary"
              @click="detail(row)"
              >详情与处理</el-button
            >
            <!-- 显示Element Plus 的 button 组件；仅当 !row.readAt && canGrow('notification:message:mark_read') 成立时渲染；启用 text 属性；disabled 取自 !!pending；click 事件触发时执行 read(row)。 -->
            <el-button
              v-if="!row.readAt && canGrow('notification:message:mark_read')"
              text
              :disabled="!!pending"
              @click="read(row)"
              >标为已读</el-button
            >
          </div>
        </article>
        <!-- 显示应用空状态当前状态（AppEmptyState）组件；仅当 !list.rows.length && !list.busy 成立时渲染；title 固定为 消息都在这里，进展不会错过；description 固定为 目标达标后会收到提醒，由你亲自决定完成或继续推进。；icon 固定为 notification。 -->
        <AppEmptyState
          v-if="!list.rows.length && !list.busy"
          title="消息都在这里，进展不会错过"
          description="目标达标后会收到提醒，由你亲自决定完成或继续推进。"
          icon="notification"
        />
      </div>
      <!-- 显示当前页Pagination（PagePagination）组件；state 取自 list；change 事件触发时执行 list.load。 -->
      <PagePagination :state="list" @change="list.load" />
    </LoadState>
  </section>
  <!-- 显示通知详情（NotificationDetails）组件；与 opened 双向同步；id 取自 detailId；changed 事件触发时执行 list.load。 -->
  <NotificationDetails v-model="opened" :id="detailId" @changed="list.load" />
</template>
