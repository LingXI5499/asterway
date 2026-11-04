<script setup>
import { ref, onMounted, computed } from "vue";
import { ElMessage } from "element-plus";
import {
  api,
  session,
  hasPermission as can,
  allPermissions,
  loadCurrent,
} from "../api/http";
import {
  report,
  confirmAction,
  browserZone,
  validateAccount,
  validatePassword,
} from "../api/ui";
import DetailsDrawer from "../components/DetailsDrawer.vue";
import TimeZoneSelect from "../components/TimeZoneSelect.vue";
import UserCharts from "../components/UserCharts.vue";
const props = defineProps({ resource: String });
const isUser = props.resource === "users",
  isMenu = props.resource === "menus";
const subject = isUser ? "user" : isMenu ? "menu" : "permission";
const label = isUser ? "用户" : isMenu ? "菜单" : "权限";
const permit = (action) => can(`identity:${subject}:${action}`);
const items = ref([]),
  total = ref(0),
  page = ref(1),
  size = ref(20),
  keyword = ref(""),
  enabled = ref(null),
  role = ref(""),
  error = ref(""),
  busy = ref(false),
  submitting = ref(false),
  modal = ref(""),
  form = ref({}),
  detail = ref(null),
  showDetail = ref(false),
  permissions = ref([]),
  roles = ref([]),
  assigned = ref([]),
  stats = ref(null),
  expanded = ref(true),
  treeKey = ref(0);
const path = "/" + props.resource;
const filtered = computed(() =>
  isMenu && keyword.value
    ? items.value.filter((row) =>
        JSON.stringify(row).toLowerCase().includes(keyword.value.toLowerCase()),
      )
    : items.value,
);
async function load() {
  busy.value = true;
  error.value = "";
  try {
    const query = new URLSearchParams({ page: page.value, size: size.value });
    if (keyword.value && !isMenu) query.set("keyword", keyword.value);
    if (enabled.value !== null && enabled.value !== "")
      query.set("enabled", enabled.value);
    if (role.value) query.set("role", role.value);
    const data = await api(path + (isMenu ? "/tree" : "?" + query));
    items.value = isMenu ? data : data.items;
    total.value = data.total || 0;
    if (isUser && can("identity:user:count"))
      stats.value = await api("/user-statistics");
  } catch (e) {
    error.value = e.message;
  } finally {
    busy.value = false;
  }
}
async function edit(row) {
  try {
    const data = row ? await api(`${path}/${row.id}`) : {};
    if (isUser)
      form.value = {
        id: data.id,
        username: data.username || "",
        nickname: data.nickname || "",
        version: data.version,
        initialPassword: "",
        confirmPassword: "",
        timeZone: browserZone(),
      };
    else if (isMenu) {
      form.value = {
        id: data.id,
        code: data.code || "",
        name: data.name || "",
        parentId: data.parentId || null,
        menuType: data.menuType || "PAGE",
        routePath: data.routePath || "",
        componentKey: data.componentKey || "",
        permissionId: data.permissionId || null,
        icon: data.icon || "",
        sortOrder: data.sortOrder || 0,
        visible: data.visible ?? true,
        enabled: data.enabled ?? true,
        version: data.version,
        builtin: data.builtin,
      };
      permissions.value = await allPermissions();
    } else
      form.value = {
        id: data.id,
        code: data.code || "",
        name: data.name || "",
        description: data.description || "",
        enabled: data.enabled ?? true,
        version: data.version,
        builtin: data.builtin,
      };
    modal.value = "edit";
  } catch (e) {
    report(e);
  }
}
async function save() {
  if (submitting.value) return;
  submitting.value = true;
  try {
    const f = form.value;
    let body;
    if (isUser) {
      validateAccount(f, f.id ? null : "initialPassword");
      body = f.id
        ? { username: f.username, nickname: f.nickname, version: f.version }
        : {
            username: f.username,
            nickname: f.nickname,
            initialPassword: f.initialPassword,
            confirmPassword: f.confirmPassword,
            timeZone: f.timeZone,
          };
    } else {
      if (!f.name.trim() || !f.code.trim()) throw new Error("请填写名称和编码");
      if (isMenu)
        body = {
          code: f.code,
          name: f.name,
          parentId: f.parentId || null,
          menuType: f.menuType,
          routePath: f.menuType === "BUTTON" ? null : f.routePath,
          componentKey: f.menuType === "PAGE" ? f.componentKey : null,
          permissionId: f.menuType === "DIRECTORY" ? null : f.permissionId,
          icon: f.icon || null,
          sortOrder: f.sortOrder,
          visible: f.visible,
          enabled: f.enabled,
          version: f.version,
        };
      else
        body = {
          code: f.code,
          name: f.name,
          description: f.description,
          enabled: f.enabled,
          version: f.version,
        };
    }
    await api(path + (f.id ? "/" + f.id : ""), {
      method: f.id ? "PATCH" : "POST",
      body,
    });
    modal.value = "";
    ElMessage.success("保存成功");
    await load();
    if (!isUser) await loadCurrent();
  } catch (e) {
    report(e);
  } finally {
    submitting.value = false;
  }
}
async function remove(row) {
  try {
    await confirmAction(`确定删除${label}“${row.username || row.name}”？`);
    await api(`${path}/${row.id}?version=${row.version}`, { method: "DELETE" });
    ElMessage.success("已删除");
    await load();
  } catch (e) {
    report(e);
  }
}
async function toggle(row) {
  try {
    await confirmAction(`确定${row.enabled ? "禁用" : "启用"}该用户？`);
    await api(`${path}/${row.id}/status`, {
      method: "PATCH",
      body: { enabled: !row.enabled, version: row.version },
    });
    await load();
  } catch (e) {
    report(e);
  }
}
async function resetPassword(row) {
  form.value = {
    id: row.id,
    version: row.version,
    newPassword: "",
    confirmPassword: "",
  };
  modal.value = "password";
}
async function assignRoles(row) {
  try {
    roles.value = await api("/roles");
    assigned.value = await api(`/users/${row.id}/roles`);
    form.value = { id: row.id, version: row.version };
    modal.value = "roles";
  } catch (e) {
    report(e);
  }
}
async function saveExtra() {
  submitting.value = true;
  try {
    const f = form.value;
    if (modal.value === "password") {
      validatePassword(f.newPassword, f.confirmPassword);
      await api(`/users/${f.id}/password`, {
        method: "PUT",
        body: {
          newPassword: f.newPassword,
          confirmPassword: f.confirmPassword,
          version: f.version,
        },
      });
    } else {
      await api(`/users/${f.id}/roles`, {
        method: "PUT",
        body: { roleIds: assigned.value, version: f.version },
      });
    }
    modal.value = "";
    ElMessage.success("操作成功");
    await load();
  } catch (e) {
    report(e);
  } finally {
    submitting.value = false;
  }
}
async function show(row) {
  try {
    detail.value = await api(`${path}/${row.id}`);
    showDetail.value = true;
  } catch (e) {
    report(e);
  }
}
function parents(nodes, depth = 0) {
  return nodes.flatMap((n) => [
    { ...n, name: "　".repeat(depth) + n.name },
    ...parents(n.children || [], depth + 1),
  ]);
}
const components = [
  "account/profile",
  "account/devices",
  "system/users",
  "system/roles",
  "system/permissions",
  "system/menus",
  "system/sessions",
  "system/login_logs",
  "system/operation_logs",
  "system/account_settings",
];
onMounted(load);
</script>
<template>
  <UserCharts v-if="stats" :statistics="stats" @refresh="load" />
  <section class="card">
    <div class="toolbar">
      <el-input
        v-model="keyword"
        placeholder="名称或编码关键字"
        clearable
        @keyup.enter="
          page = 1;
          load();
        "
      /><el-select
        v-if="!isMenu"
        v-model="enabled"
        clearable
        placeholder="全部状态"
        ><el-option label="启用" :value="true" /><el-option
          label="禁用"
          :value="false" /></el-select
      ><el-select
        v-if="isUser && session.user.roles.includes('SUPER_ADMIN')"
        v-model="role"
        clearable
        placeholder="全部角色"
        ><el-option label="用户" value="USER" /><el-option
          label="管理员"
          value="ADMIN" /><el-option
          label="超级管理员"
          value="SUPER_ADMIN" /></el-select
      ><el-button
        type="primary"
        @click="
          page = 1;
          load();
        "
        >查询</el-button
      ><el-button
        @click="
          keyword = '';
          enabled = null;
          role = '';
          page = 1;
          load();
        "
        >重置</el-button
      ><el-button v-if="permit('create')" type="primary" plain @click="edit()"
        >新增{{ label }}</el-button
      ><el-button
        v-if="isMenu"
        @click="
          expanded = !expanded;
          treeKey++;
        "
        >展开／折叠</el-button
      >
    </div>
    <el-alert
      v-if="error"
      :title="error"
      type="error"
      :closable="false"
    /><el-table
      :key="treeKey"
      :data="filtered"
      row-key="id"
      :default-expand-all="expanded"
      v-loading="busy"
      empty-text="暂无符合条件的记录"
      ><el-table-column
        :prop="isUser ? 'username' : 'name'"
        :label="isUser ? '用户名' : '名称'"
        min-width="180"
      /><el-table-column
        v-if="isUser"
        prop="nickname"
        label="昵称"
        min-width="130"
      /><el-table-column
        v-else
        prop="code"
        label="编码"
        min-width="220"
      /><el-table-column
        v-if="isMenu"
        prop="menuType"
        label="类型"
        width="120"
      /><el-table-column
        v-if="isMenu"
        prop="routePath"
        label="路径"
        min-width="180"
      /><el-table-column v-if="isUser" label="角色" min-width="160"
        ><template #default="{ row }"
          ><el-tag v-for="r in row.roles" :key="r">{{ r }}</el-tag></template
        ></el-table-column
      ><el-table-column label="状态" width="90"
        ><template #default="{ row }"
          ><el-tag :type="row.enabled ? 'success' : 'info'">{{
            row.enabled ? "启用" : "禁用"
          }}</el-tag></template
        ></el-table-column
      ><el-table-column label="操作" min-width="330" fixed="right"
        ><template #default="{ row }"
          ><el-button
            v-if="permit('read')"
            link
            type="primary"
            @click="show(row)"
            >详情</el-button
          ><el-button
            v-if="permit('update')"
            link
            type="primary"
            @click="edit(row)"
            >编辑</el-button
          ><el-button
            v-if="isUser && permit('status')"
            link
            type="warning"
            @click="toggle(row)"
            >{{ row.enabled ? "禁用" : "启用" }}</el-button
          ><el-button
            v-if="isUser && permit('reset_password')"
            link
            type="primary"
            @click="resetPassword(row)"
            >重置密码</el-button
          ><el-button
            v-if="isUser && permit('assign_role')"
            link
            type="primary"
            @click="assignRoles(row)"
            >角色</el-button
          ><el-button
            v-if="permit('delete')"
            :disabled="row.builtin"
            link
            type="danger"
            @click="remove(row)"
            >删除</el-button
          ></template
        ></el-table-column
      ></el-table
    ><el-pagination
      v-if="!isMenu"
      class="pagination"
      v-model:current-page="page"
      v-model:page-size="size"
      :total="total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next"
      @change="load"
    />
  </section>
  <DetailsDrawer v-model="showDetail" :value="detail" /><el-dialog
    :model-value="!!modal"
    @update:model-value="!$event && (modal = '')"
    :title="
      modal === 'roles'
        ? '分配角色'
        : modal === 'password'
          ? '重置密码'
          : (form.id ? '编辑' : '新增') + label
    "
    width="min(600px, 94vw)"
    :close-on-click-modal="false"
    ><el-form
      label-position="top"
      @submit.prevent="modal === 'edit' ? save() : saveExtra()"
      ><template v-if="modal === 'edit'"
        ><template v-if="isUser"
          ><el-form-item label="用户名"
            ><el-input v-model="form.username" maxlength="30" /></el-form-item
          ><el-form-item label="昵称"
            ><el-input v-model="form.nickname" maxlength="30" /></el-form-item
          ><template v-if="!form.id"
            ><el-form-item label="初始密码"
              ><el-input
                v-model="form.initialPassword"
                type="password"
                show-password /></el-form-item
            ><el-form-item label="确认密码"
              ><el-input
                v-model="form.confirmPassword"
                type="password"
                show-password /></el-form-item
            ><el-form-item label="初始时区"
              ><TimeZoneSelect
                v-model="form.timeZone" /></el-form-item></template></template
        ><template v-else
          ><el-form-item label="编码"
            ><el-input
              v-model="form.code"
              :disabled="form.builtin"
              maxlength="100" /></el-form-item
          ><el-form-item label="名称"
            ><el-input v-model="form.name" maxlength="64" /></el-form-item
          ><template v-if="isMenu"
            ><el-form-item label="类型"
              ><el-select v-model="form.menuType"
                ><el-option
                  v-for="t in ['DIRECTORY', 'PAGE', 'BUTTON']"
                  :key="t"
                  :value="t"
                  :label="
                    { DIRECTORY: '目录', PAGE: '页面', BUTTON: '按钮' }[t]
                  " /></el-select></el-form-item
            ><el-form-item label="上级菜单"
              ><el-select v-model="form.parentId" clearable filterable
                ><el-option
                  v-for="m in parents(items).filter((m) => m.id !== form.id)"
                  :key="m.id"
                  :label="m.name"
                  :value="m.id" /></el-select></el-form-item
            ><el-form-item v-if="form.menuType !== 'BUTTON'" label="路由路径"
              ><el-input v-model="form.routePath" /></el-form-item
            ><el-form-item v-if="form.menuType === 'PAGE'" label="页面组件"
              ><el-select v-model="form.componentKey"
                ><el-option
                  v-for="c in components"
                  :key="c"
                  :value="c"
                  :label="c" /></el-select></el-form-item
            ><el-form-item v-if="form.menuType !== 'DIRECTORY'" label="关联权限"
              ><el-select v-model="form.permissionId" filterable
                ><el-option
                  v-for="p in permissions"
                  :key="p.id"
                  :value="p.id"
                  :label="p.name + ' · ' + p.code" /></el-select></el-form-item
            ><el-form-item label="排序"
              ><el-input-number
                v-model="form.sortOrder"
                :min="0" /></el-form-item
            ><el-form-item label="图标标识"
              ><el-input v-model="form.icon" maxlength="64" /></el-form-item
            ><el-form-item label="显示菜单"
              ><el-switch v-model="form.visible" /></el-form-item></template
          ><el-form-item v-else label="说明"
            ><el-input
              v-model="form.description"
              type="textarea"
              maxlength="255" /></el-form-item
          ><el-form-item label="启用"
            ><el-switch
              v-model="form.enabled" /></el-form-item></template></template
      ><template v-else-if="modal === 'password'"
        ><el-alert
          title="重置后该用户的所有会话将退出，并要求下次登录修改密码"
          type="warning"
          :closable="false" /><el-form-item label="新密码"
          ><el-input
            v-model="form.newPassword"
            type="password"
            show-password /></el-form-item
        ><el-form-item label="确认密码"
          ><el-input
            v-model="form.confirmPassword"
            type="password"
            show-password /></el-form-item></template
      ><el-checkbox-group v-else-if="modal === 'roles'" v-model="assigned"
        ><el-checkbox
          v-for="r in roles"
          :key="r.id"
          :value="r.id"
          :disabled="r.code !== 'ADMIN'"
          >{{ r.name }}</el-checkbox
        ></el-checkbox-group
      >
      <div class="dialog-actions">
        <el-button @click="modal = ''">取消</el-button
        ><el-button type="primary" native-type="submit" :loading="submitting"
          >保存</el-button
        >
      </div></el-form
    ></el-dialog
  >
</template>
