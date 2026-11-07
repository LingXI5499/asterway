<script setup>
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { api, clearSession, hasPermission } from "../api/http";
import { report, confirmAction, dateText } from "../api/ui";
import DetailsDrawer from "../components/DetailsDrawer.vue";
const props = defineProps({ kind: String });
const router = useRouter(),
  items = ref([]),
  page = ref(1),
  size = ref(20),
  total = ref(0),
  busy = ref(false),
  error = ref(""),
  filter = ref(""),
  detail = ref(null),
  show = ref(false);
const logs = props.kind.includes("logs"),
  personal = props.kind === "devices";
const path = personal ? "/me/sessions" : "/" + props.kind;
async function load() {
  busy.value = true;
  error.value = "";
  try {
    const q = new URLSearchParams({ page: page.value, size: size.value });
    if (logs && filter.value) q.set("userId", filter.value);
    const result = await api(path + (personal ? "" : "?" + q));
    items.value = personal ? result : result.items;
    total.value = result.total || 0;
  } catch (e) {
    error.value = e.message;
  } finally {
    busy.value = false;
  }
}
async function revoke(row, all = false) {
  try {
    await confirmAction(
      all ? "退出全部设备后需要重新登录，是否继续？" : "确定强制退出该会话？",
      all ? "退出全部设备" : "退出该会话",
    );
    await api(path + (all ? "" : "/" + row.id), { method: "DELETE" });
    ElMessage.success("会话已退出");
    if (all || row.current) {
      clearSession();
      router.replace("/login");
    } else load();
  } catch (e) {
    report(e);
  }
}
onMounted(load);
</script>
<template>
  <section class="card">
    <div class="toolbar">
      <el-input
        v-if="logs"
        v-model="filter"
        placeholder="输入用户 ID 筛选"
        clearable
        @keyup.enter="
          page = 1;
          load();
        "
      /><el-button
        type="primary"
        @click="
          page = 1;
          load();
        "
        >{{ logs ? "查询" : "刷新列表" }}</el-button
      ><el-button
        v-if="logs"
        @click="
          filter = '';
          page = 1;
          load();
        "
        >重置</el-button
      ><el-button
        v-if="personal && hasPermission('identity:device:revoke')"
        type="danger"
        plain
        @click="revoke(null, true)"
        >退出全部设备</el-button
      >
    </div>
    <el-alert
      v-if="error"
      :title="error"
      type="error"
      :closable="false"
    /><el-table :data="items" v-loading="busy" empty-text="暂无记录"
      ><template #empty
        ><AppEmptyState
          :description="
            error ? '加载失败，请查看提示后重试。' : '暂无记录'
          " /></template
      ><el-table-column
        prop="username"
        label="用户"
        min-width="140"
        v-if="!personal"
      /><el-table-column
        v-if="kind === 'operation-logs'"
        prop="actorUsername"
        label="操作者"
        min-width="140"
      /><el-table-column
        v-if="kind === 'operation-logs'"
        prop="actionCode"
        label="操作"
        min-width="220"
      /><el-table-column
        v-if="!logs"
        prop="deviceLabel"
        label="设备"
        min-width="180"
      /><el-table-column
        prop="ipAddress"
        label="IP 地址"
        min-width="140"
      /><el-table-column v-if="logs" prop="outcome" label="结果" min-width="110"
        ><template #default="{ row }"
          ><AppStatusTag :value="row.outcome" /></template></el-table-column
      ><el-table-column
        v-if="logs"
        prop="reasonCode"
        label="原因"
        min-width="160"
      /><el-table-column :label="logs ? '发生时间' : '最近活动'" min-width="180"
        ><template #default="{ row }">{{
          dateText(row.occurredAt || row.lastActiveAt)
        }}</template></el-table-column
      ><el-table-column v-if="personal" label="当前设备"
        ><template #default="{ row }"
          ><el-tag v-if="row.current" type="success">当前会话</el-tag></template
        ></el-table-column
      ><el-table-column label="操作" fixed="right" width="180"
        ><template #default="{ row }"
          ><el-button
            link
            type="primary"
            @click="
              detail = row;
              show = true;
            "
            >详情</el-button
          ><el-button
            v-if="
              !logs &&
              hasPermission(
                personal ? 'identity:device:revoke' : 'identity:session:revoke',
              )
            "
            link
            type="danger"
            @click="revoke(row)"
            >{{ row.current ? "退出当前" : "强制退出" }}</el-button
          ></template
        ></el-table-column
      ></el-table
    ><el-pagination
      v-if="!personal"
      class="pagination"
      v-model:current-page="page"
      v-model:page-size="size"
      :total="total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next"
      @change="load"
    />
  </section>
  <DetailsDrawer v-model="show" :value="detail" />
</template>
