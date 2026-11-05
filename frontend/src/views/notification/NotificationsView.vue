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
      <el-table
        :data="list.rows"
        v-loading="list.busy"
        empty-text="暂无站内消息，目标达标后会在这里提醒你"
      >
        <el-table-column label="消息" min-width="260"
          ><template #default="{ row }"
            ><el-button
              v-if="canGrow('notification:message:read')"
              link
              type="primary"
              @click="detail(row)"
              >{{ row.title }}</el-button
            ><span v-else>{{ row.title }}</span>
            <p class="table-excerpt">{{ row.content }}</p></template
          ></el-table-column
        >
        <el-table-column label="已读状态" width="100"
          ><template #default="{ row }"
            ><el-tag :type="row.readAt ? 'info' : 'primary'">{{
              row.readAt ? "已读" : "未读"
            }}</el-tag></template
          ></el-table-column
        >
        <el-table-column label="业务处理" width="130"
          ><template #default="{ row }"
            ><el-tag
              :type="
                row.handleStatus === 'PENDING'
                  ? 'warning'
                  : row.handleStatus === 'CONFIRMED'
                    ? 'success'
                    : 'info'
              "
              >{{ handleStates[row.handleStatus] }}</el-tag
            ></template
          ></el-table-column
        >
        <el-table-column label="发送时间" min-width="180"
          ><template #default="{ row }">{{
            instantText(row.createdAt)
          }}</template></el-table-column
        >
        <el-table-column label="操作" width="210" fixed="right"
          ><template #default="{ row }">
            <el-button
              v-if="canGrow('notification:message:read')"
              link
              type="primary"
              @click="detail(row)"
              >详情与处理</el-button
            >
            <el-button
              v-if="!row.readAt && canGrow('notification:message:mark_read')"
              link
              :disabled="!!pending"
              @click="read(row)"
              >标为已读</el-button
            >
          </template></el-table-column
        > </el-table
      ><PagePagination :state="list" @change="list.load" />
    </LoadState>
  </section>
  <NotificationDetails v-model="opened" :id="detailId" @changed="list.load" />
</template>
