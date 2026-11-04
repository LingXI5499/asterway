<script setup>
import { computed } from "vue";
import { dateText } from "../api/ui";
const props = defineProps({ modelValue: Boolean, value: Object });
defineEmits(["update:modelValue"]);
const labels = {
  id: "编号",
  username: "用户名",
  nickname: "昵称",
  enabled: "启用状态",
  roles: "角色",
  roleIds: "角色编号",
  version: "数据版本",
  timeZone: "当前时区",
  pendingTimeZone: "待生效时区",
  timeZoneEffectiveAt: "时区生效时间",
  createdAt: "创建时间",
  mustChangePassword: "需修改密码",
  code: "编码",
  name: "名称",
  description: "说明",
  builtin: "内置",
  parentId: "上级编号",
  menuType: "菜单类型",
  routePath: "路由",
  componentKey: "组件",
  permissionId: "权限编号",
  icon: "图标",
  sortOrder: "排序",
  visible: "菜单可见",
  userId: "用户编号",
  deviceLabel: "设备",
  ipAddress: "IP 地址",
  userAgent: "浏览器信息",
  lastActiveAt: "最近活动",
  expiresAt: "过期时间",
  current: "当前会话",
  outcome: "结果",
  reasonCode: "原因",
  sessionId: "会话编号",
  traceId: "追踪编号",
  occurredAt: "发生时间",
  actorId: "操作者编号",
  actorUsername: "操作者",
  targetUserId: "目标用户",
  resourceType: "资源类型",
  resourceId: "资源编号",
  actionCode: "操作",
  changeSummary: "变更摘要",
};
const entries = computed(() =>
  Object.entries(props.value || {}).filter(
    ([k]) => !["children", "permissionIds", "menuIds"].includes(k),
  ),
);
function format(k, v) {
  if (v == null || v === "") return "—";
  if (typeof v === "boolean") return v ? "是" : "否";
  if (/(At)$/.test(k)) return dateText(v);
  return typeof v === "object" ? JSON.stringify(v, null, 2) : v;
}
</script>
<template>
  <el-drawer
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    title="记录详情"
    size="min(640px, 100%)"
    ><el-descriptions :column="1" border
      ><el-descriptions-item
        v-for="[key, value] in entries"
        :key="key"
        :label="labels[key] || key"
      >
        <pre class="detail-value">{{ format(key, value) }}</pre>
      </el-descriptions-item></el-descriptions
    ></el-drawer
  >
</template>
