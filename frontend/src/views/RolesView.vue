<script setup>
import { ref, onMounted } from "vue";
import { ElMessage } from "element-plus";
import {
  api,
  allPermissions,
  loadCurrent,
  hasPermission as can,
} from "../api/http";
import { report } from "../api/ui";
const growthItem = (item) =>
  /^(goal|stage|task|planning):/.test(item.code || "") ||
  item.code?.startsWith("growth_") ||
  item.componentKey?.startsWith("growth/") ||
  item.componentKey?.startsWith("planning/");
const roles = ref([]),
  selected = ref(null),
  mode = ref(""),
  form = ref({}),
  options = ref([]),
  checked = ref([]),
  busy = ref(false),
  error = ref("");
async function load() {
  try {
    roles.value = await api("/roles");
    error.value = "";
  } catch (e) {
    error.value = e.message;
  }
}
async function open(row, action) {
  try {
    selected.value = await api("/roles/" + row.id);
    form.value = {
      name: selected.value.name,
      description: selected.value.description,
    };
    if (action === "permissions") {
      options.value = await allPermissions();
      checked.value = await api(`/roles/${row.id}/permissions`);
    }
    if (action === "menus") {
      options.value = await api("/menus");
      checked.value = await api(`/roles/${row.id}/menus`);
    }
    mode.value = action;
  } catch (e) {
    report(e);
  }
}
function mandatory(item) {
  return (
    item.builtin &&
    !growthItem(item) &&
    (selected.value.code === "SUPER_ADMIN" ||
      (selected.value.code === "USER" &&
        (mode.value === "permissions"
          ? /identity:(profile|password|timezone|device):/.test(item.code)
          : ["account_root", "profile", "devices"].includes(item.code))))
  );
}
function selectable(item) {
  if (growthItem(item)) return selected.value.code === "USER" && item.enabled;
  if (mandatory(item)) return false;
  if (!item.enabled) return false;
  if (mode.value !== "permissions" || selected.value.code === "SUPER_ADMIN")
    return true;
  const personal = /identity:(profile|password|timezone|device):/.test(
    item.code,
  );
  return selected.value.code === "USER"
    ? personal
    : personal ||
        /identity:(session|login_log|operation_log):/.test(item.code) ||
        (item.code.startsWith("identity:user:") &&
          !item.code.endsWith(":assign_role"));
}
async function save() {
  busy.value = true;
  try {
    const suffix = mode.value === "edit" ? "" : "/" + mode.value;
    let body =
      mode.value === "edit"
        ? { ...form.value, version: selected.value.version }
        : {
            [mode.value === "permissions" ? "permissionIds" : "menuIds"]:
              checked.value,
            version: selected.value.version,
          };
    if (mode.value === "edit" && !form.value.name?.trim())
      throw new Error("请填写角色名称");
    await api("/roles/" + selected.value.id + suffix, {
      method: mode.value === "edit" ? "PATCH" : "PUT",
      body,
    });
    mode.value = "";
    ElMessage.success("角色已更新");
    await load();
    await loadCurrent();
  } catch (e) {
    report(e);
  } finally {
    busy.value = false;
  }
}
onMounted(load);
</script>
<template>
  <section class="card">
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <div class="toolbar">
      <el-button @click="load">刷新角色</el-button
      ><span class="muted">系统使用用户、管理员、超级管理员三种角色。</span>
    </div>
    <el-table :data="roles"
      ><template #empty
        ><AppEmptyState
          :description="
            error
              ? '加载失败，请查看提示后重试。'
              : '暂无记录，可调整筛选条件或从第一条记录开始。'
          " /></template
      ><el-table-column prop="name" label="角色名称" /><el-table-column
        prop="code"
        label="角色编码"
      /><el-table-column
        prop="description"
        label="说明"
        min-width="200"
      /><el-table-column label="操作" width="300"
        ><template #default="{ row }"
          ><el-button
            v-if="can('identity:role:update')"
            link
            type="primary"
            @click="open(row, 'edit')"
            >编辑</el-button
          ><el-button
            v-if="can('identity:role:grant_permission')"
            link
            type="primary"
            @click="open(row, 'permissions')"
            >分配权限</el-button
          ><el-button
            v-if="can('identity:role:grant_menu')"
            link
            type="primary"
            @click="open(row, 'menus')"
            >分配菜单</el-button
          ></template
        ></el-table-column
      ></el-table
    >
  </section>
  <el-drawer
    :model-value="!!mode"
    @update:model-value="!$event && (mode = '')"
    :title="
      selected?.name +
      ' · ' +
      ({ edit: '角色资料', permissions: '分配权限', menus: '分配菜单' }[mode] ||
        '')
    "
    size="min(680px, 100%)"
    :close-on-click-modal="false"
    ><el-form v-if="mode === 'edit'" label-position="top"
      ><el-form-item label="名称"
        ><el-input v-model="form.name" maxlength="64" /></el-form-item
      ><el-form-item label="说明"
        ><el-input
          v-model="form.description"
          type="textarea"
          maxlength="255" /></el-form-item></el-form
    ><template v-else
      ><el-alert
        title="核心账户权限和内置菜单受到保护。请先分配动作权限，再分配对应菜单。"
        type="info"
        :closable="false"
      /><el-checkbox-group v-model="checked" class="grant-list"
        ><el-checkbox
          v-for="item in options"
          :key="item.id"
          :value="item.id"
          :disabled="!selectable(item)"
          ><span>{{ item.name }}</span
          ><small>{{ item.code }}</small></el-checkbox
        ></el-checkbox-group
      ></template
    ><template #footer
      ><el-button @click="mode = ''">取消</el-button
      ><el-button type="primary" :loading="busy" @click="save"
        >保存</el-button
      ></template
    ></el-drawer
  >
</template>
