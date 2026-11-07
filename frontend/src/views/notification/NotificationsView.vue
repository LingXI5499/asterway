<script setup>
import { ref, reactive } from "vue";
import {
  notificationApi,
  handleStates,
  instantText,
  signalChange,
} from "../../api/personal";
import { canGrow } from "../../api/growth";
import { messages, refreshUnread } from "../../stores/personal";
import { usePaged } from "../../composables/usePaged";
import { report } from "../../api/ui";
import { ElMessage } from "element-plus";
import NotificationDetails from "../../components/personal/NotificationDetails.vue";
import PagePagination from "../../components/personal/PagePagination.vue";
import LoadState from "../../components/personal/LoadState.vue";
const filters = reactive({ unread: "", handleStatus: "" }),
  pending = ref(""),
  detailId = ref(""),
  opened = ref(false);
const list = usePaged((params, signal) =>
  notificationApi.list(
    {
      ...params,
      unread: ["true", "false"].includes(filters.unread)
        ? filters.unread === "true"
        : null,
      handleStatus: filters.handleStatus,
    },
    signal,
  ),
);
function detail(row) {
  detailId.value = row.id;
  opened.value = true;
}
async function read(row) {
  if (pending.value) return;
  pending.value = row.id;
  try {
    await notificationApi.markRead(row);
    signalChange("notification");
    await Promise.all([list.load(), refreshUnread()]);
    ElMessage.success("已标记为已读");
  } catch (e) {
    report(e);
    if (e.status === 409) await list.load();
  } finally {
    pending.value = "";
  }
}
async function allRead() {
  if (pending.value) return;
  pending.value = "all";
  try {
    await notificationApi.markAllRead();
    signalChange("notification");
    await Promise.all([list.load(), refreshUnread()]);
    ElMessage.success("当前有效未读消息已全部标记");
  } catch (e) {
    report(e);
  } finally {
    pending.value = "";
  }
}
</script>
<template>
  <section class="card personal-hero">
    <div>
      <span class="eyebrow">NOTIFICATIONS</span>
      <h2>重要的进展，等你亲自确认</h2>
      <p class="muted">
        目标达标后由你决定完成或继续保持。当前未读 {{ messages.unread }} 条。
      </p>
    </div>
    <el-button
      v-if="canGrow('notification:message:mark_read')"
      :loading="pending === 'all'"
      :disabled="!!pending || messages.unread === 0"
      @click="allRead"
      >全部标为已读</el-button
    >
  </section>
  <section class="card">
    <div class="toolbar">
      <el-select v-model="filters.unread" clearable placeholder="全部已读状态"
        ><el-option label="未读" value="true" /><el-option
          label="已读"
          value="false"
      /></el-select>
      <el-select
        v-model="filters.handleStatus"
        clearable
        placeholder="全部处理状态"
        ><el-option
          v-for="(name, code) in handleStates"
          :key="code"
          :label="name"
          :value="code"
      /></el-select>
      <el-button type="primary" @click="list.load(true)">查询</el-button
      ><el-button
        @click="
          Object.assign(filters, { unread: '', handleStatus: '' });
          list.load(true);
        "
        >重置</el-button
      >
    </div>
    <LoadState
      :loading="list.busy && !list.loaded"
      :error="list.error"
      @retry="list.load"
    >
      <div class="notice-list" v-loading="list.busy">
        <article
          v-for="row in list.rows"
          :key="row.id"
          class="notice-row"
          :class="{ unread: !row.readAt }"
        >
          <div class="notice-icon">
            <AppIcon name="notification" :size="22" />
          </div>
          <div class="notice-copy">
            <h3>
              <el-button
                v-if="canGrow('notification:message:read')"
                link
                type="primary"
                @click="detail(row)"
                >{{ row.title }}</el-button
              ><span v-else>{{ row.title }}</span>
            </h3>
            <p>{{ row.content }}</p>
            <div class="notice-meta">
              <span>{{ instantText(row.createdAt) }}</span
              ><span>{{ row.readAt ? "已读" : "未读" }}</span
              ><AppStatusTag :value="row.handleStatus" />
            </div>
          </div>
          <div class="notice-actions">
            <el-button
              v-if="canGrow('notification:message:read')"
              text
              type="primary"
              @click="detail(row)"
              >详情与处理</el-button
            >
            <el-button
              v-if="!row.readAt && canGrow('notification:message:mark_read')"
              text
              :disabled="!!pending"
              @click="read(row)"
              >标为已读</el-button
            >
          </div>
        </article>
        <AppEmptyState
          v-if="!list.rows.length && !list.busy"
          title="消息都在这里，进展不会错过"
          description="目标达标后会收到提醒，由你亲自决定完成或继续推进。"
          icon="notification"
        />
      </div>
      <PagePagination :state="list" @change="list.load" />
    </LoadState>
  </section>
  <NotificationDetails v-model="opened" :id="detailId" @changed="list.load" />
</template>
