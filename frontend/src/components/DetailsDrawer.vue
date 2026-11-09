<!-- 渲染详情抽屉界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
/* 引入 vue 提供的组件、状态函数或请求方法。 */
import { computed } from "vue";
/* 引入 ../api/ui 提供的组件、状态函数或请求方法。 */
import { dateText } from "../api/ui";
const /* 父组件传入的数据（props）保存声明父组件可以传入的属性及其类型或默认值。 */ props = defineProps({ /* 模型值（modelValue）字段使用可为空的真假值（Boolean）。 */ modelValue: Boolean, /* 值（value）字段使用对象（Object）。 */ value: Object });
/* 声明当前组件可以通知父组件的事件。 */
defineEmits(["update:modelValue"]);
const /* 名称对应表（labels）保存表达式 { id: "编号", username: "用户名", nickname: "昵称", enabled: "启用状态", roles: "角色", roleIds: "角色编号", version: "数据版本", timeZone: "… 的结果。 */ labels = {
  /* 编号（id）字段使用固定文字 '编号'。 */
  id: "编号",
  /* 用户名（username）字段使用固定文字 '用户名'。 */
  username: "用户名",
  /* 昵称（nickname）字段使用固定文字 '昵称'。 */
  nickname: "昵称",
  /* 是否启用（enabled）字段使用固定文字 '启用状态'。 */
  enabled: "启用状态",
  /* 角色列表（roles）字段使用固定文字 '角色'。 */
  roles: "角色",
  /* 角色编号列表（roleIds）字段使用固定文字 '角色编号'。 */
  roleIds: "角色编号",
  /* 版本号（version）字段使用固定文字 '数据版本'。 */
  version: "数据版本",
  /* 用户使用的时区（timeZone）字段使用固定文字 '当前时区'。 */
  timeZone: "当前时区",
  /* 等待处理时间时区（pendingTimeZone）字段使用固定文字 '待生效时区'。 */
  pendingTimeZone: "待生效时区",
  /* 时间时区生效At（timeZoneEffectiveAt）字段使用固定文字 '时区生效时间'。 */
  timeZoneEffectiveAt: "时区生效时间",
  /* 创建时At（createdAt）字段使用固定文字 '创建时间'。 */
  createdAt: "创建时间",
  /* 首次登录是否必须改密码（mustChangePassword）字段使用固定文字 '需修改密码'。 */
  mustChangePassword: "需修改密码",
  /* 编码（code）字段使用固定文字 '编码'。 */
  code: "编码",
  /* 名称（name）字段使用固定文字 '名称'。 */
  name: "名称",
  /* 说明（description）字段使用固定文字 '说明'。 */
  description: "说明",
  /* builtin字段使用固定文字 '内置'。 */
  builtin: "内置",
  /* 上级编号（parentId）字段使用固定文字 '上级编号'。 */
  parentId: "上级编号",
  /* 菜单类型（menuType）字段使用固定文字 '菜单类型'。 */
  menuType: "菜单类型",
  /* route路径（routePath）字段使用固定文字 '路由'。 */
  routePath: "路由",
  /* component键（componentKey）字段使用固定文字 '组件'。 */
  componentKey: "组件",
  /* 权限编号（permissionId）字段使用固定文字 '权限编号'。 */
  permissionId: "权限编号",
  /* icon字段使用固定文字 '图标'。 */
  icon: "图标",
  /* 排序顺序（sortOrder）字段使用固定文字 '排序'。 */
  sortOrder: "排序",
  /* 是否显示（visible）字段使用固定文字 '菜单可见'。 */
  visible: "菜单可见",
  /* 用户编号（userId）字段使用固定文字 '用户编号'。 */
  userId: "用户编号",
  /* 设备显示名称（deviceLabel）字段使用固定文字 '设备'。 */
  deviceLabel: "设备",
  /* ipAddress字段使用固定文字 'IP 地址'。 */
  ipAddress: "IP 地址",
  /* 用户成长助手（userAgent）字段使用固定文字 '浏览器信息'。 */
  userAgent: "浏览器信息",
  /* 最后一次当前有效At（lastActiveAt）字段使用固定文字 '最近活动'。 */
  lastActiveAt: "最近活动",
  /* 到期At（expiresAt）字段使用固定文字 '过期时间'。 */
  expiresAt: "过期时间",
  /* 当前（current）字段使用固定文字 '当前会话'。 */
  current: "当前会话",
  /* outcome字段使用固定文字 '结果'。 */
  outcome: "结果",
  /* 原因编码（reasonCode）字段使用固定文字 '原因'。 */
  reasonCode: "原因",
  /* 会话编号（sessionId）字段使用固定文字 '会话编号'。 */
  sessionId: "会话编号",
  /* 本次请求的追踪编号（traceId）字段使用固定文字 '追踪编号'。 */
  traceId: "追踪编号",
  /* occurredAt字段使用固定文字 '发生时间'。 */
  occurredAt: "发生时间",
  /* actor编号（actorId）字段使用固定文字 '操作者编号'。 */
  actorId: "操作者编号",
  /* actor用户名（actorUsername）字段使用固定文字 '操作者'。 */
  actorUsername: "操作者",
  /* 目标值用户编号（targetUserId）字段使用固定文字 '目标用户'。 */
  targetUserId: "目标用户",
  /* resource类型（resourceType）字段使用固定文字 '资源类型'。 */
  resourceType: "资源类型",
  /* resource编号（resourceId）字段使用固定文字 '资源编号'。 */
  resourceId: "资源编号",
  /* 操作编码（actionCode）字段使用固定文字 '操作'。 */
  actionCode: "操作",
  /* 修改汇总（changeSummary）字段使用固定文字 '变更摘要'。 */
  changeSummary: "变更摘要",
};
const /* entries保存随依赖自动重算的筛选结果或数量。 */ entries = computed(() =>
  Object.entries(props.value || {}).filter(
    ([k]) => !["children", "permissionIds", "menuIds"].includes(k),
  ),
);
/* 格式（format）：调用 format 完成格式处理。 */
function format(k, v) {
  /* 检查v == null || v === ""，即对应内容是否不存在，成立时走这一分支。 */
  if (v == null || v === "") /* 返回固定文字 '—'，当前函数到此结束。 */ return "—";
  /* 检查条件 typeof v === "boolean" 是否成立，成立时走这一分支。 */
  if (typeof v === "boolean") /* 返回根据前面的条件，从两个候选结果中选择一个：v ? "是" : "否"，当前函数到此结束。 */ return v ? "是" : "否";
  /* 检查条件 /(At)$/.test(k) 是否成立，成立时走这一分支。 */
  if (/(At)$/.test(k)) /* 返回调用 dateText 完成日期文字处理，当前函数到此结束。 */ return dateText(v);
  /* 返回根据前面的条件，从两个候选结果中选择一个：typeof v === "object" ? JSON.stringify(v, null, 2) : v，当前函数到此结束。 */
  return typeof v === "object" ? JSON.stringify(v, null, 2) : v;
}
</script>
<template>
  <!-- 显示Element Plus 的 drawer 组件；model-value 取自 modelValue；update:model-value 事件触发时执行 $emit('update:modelValue', $event)；title 固定为 记录详情；size 固定为 min(640px, 100%)。 -->
  <el-drawer
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    title="记录详情"
    size="min(640px, 100%)"
    ><!-- 显示Element Plus 的 descriptions 组件；column 取自 1；启用 border 属性。 --><el-descriptions :column="1" border
      ><!-- 显示Element Plus 的 descriptions-item 组件；按 [key, value] in entries 为每条数据重复生成内容；key 取自 key；label 取自 labels[key] || key。 --><el-descriptions-item
        v-for="[key, value] in entries"
        :key="key"
        :label="labels[key] || key"
      >
        <!-- 显示提前（pre）组件；用样式类 detail-value 控制外观。 -->
        <pre class="detail-value"><!-- 把 format(key, value) 的当前值转成文字显示。 -->{{ format(key, value) }}</pre>
      </el-descriptions-item></el-descriptions
    ></el-drawer
  >
</template>
