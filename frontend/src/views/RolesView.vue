<!-- 渲染角色列表页面界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
/* 引入 vue 提供的组件、状态函数或请求方法。 */
import { ref, onMounted } from "vue";
/* 引入 element-plus 提供的组件、状态函数或请求方法。 */
import { ElMessage } from "element-plus";
/* 引入 ../api/http 提供的组件、状态函数或请求方法。 */
import {
  api,
  allPermissions,
  loadCurrent,
  hasPermission as can,
} from "../api/http";
/* 引入 ../api/ui 提供的组件、状态函数或请求方法。 */
import { report } from "../api/ui";
const /* 成长业务单条数据（growthItem）保存沿 (item) => /^(goal|stage|task|planning):/.test(item.code || "") || item.code?.startsWith("growth_") || item.componen… 读取数据；中间对象为空时返回空值。 */ growthItem = (item) =>
  /^(goal|stage|task|planning):/.test(item.code || "") ||
  item.code?.startsWith("growth_") ||
  item.componentKey?.startsWith("growth/") ||
  item.componentKey?.startsWith("planning/");
const /* 角色列表（roles）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ roles = ref([]),
  /* 已选（selected）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  selected = ref(null),
  /* mode保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  mode = ref(""),
  /* form保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  form = ref({}),
  /* 选项（options）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  options = ref([]),
  /* 已核对（checked）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  checked = ref([]),
  /* 正在处理（busy）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  busy = ref(false),
  /* 错误（error）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  error = ref("");
/* 加载（load）：读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */
async function load() {
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    /* 把等待 调用 api 完成接口处理 完成后取得结果写入 roles.value。 */
    roles.value = await api("/roles");
    /* 把固定文字 ''写入 error.value。 */
    error.value = "";
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 把表达式 e.message 的结果写入 error.value。 */
    error.value = e.message;
  }
}
/* 是否展开（open）：调用 open 完成是否展开处理。 */
async function open(row, action) {
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    /* 把等待 调用 api 完成接口处理 完成后取得结果写入 selected.value。 */
    selected.value = await api("/roles/" + row.id);
    /* 把表达式 { name: selected.value.name, description: selected.value.description, } 的结果写入 form.value。 */
    form.value = {
      /* 名称（name）字段使用表达式 selected.value.name 的结果。 */
      name: selected.value.name,
      /* 说明（description）字段使用表达式 selected.value.description 的结果。 */
      description: selected.value.description,
    };
    /* 检查当前用户是否具有需要的权限：action === "permissions"，成立时走这一分支。 */
    if (action === "permissions") {
      /* 把等待 调用 allPermissions 完成全部权限列表处理 完成后取得结果写入 options.value。 */
      options.value = await allPermissions();
      /* 把等待 调用 api 完成接口处理 完成后取得结果写入 checked.value。 */
      checked.value = await api(`/roles/${row.id}/permissions`);
    }
    /* 检查条件 action === "menus" 是否成立，成立时走这一分支。 */
    if (action === "menus") {
      /* 把等待 调用 api 完成接口处理 完成后取得结果写入 options.value。 */
      options.value = await api("/menus");
      /* 把等待 调用 api 完成接口处理 完成后取得结果写入 checked.value。 */
      checked.value = await api(`/roles/${row.id}/menus`);
    }
    /* 把操作（action）写入 mode.value。 */
    mode.value = action;
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 调用 report 完成复盘报告处理。 */
    report(e);
  }
}
/* mandatory：调用 mandatory 完成mandatory处理。 */
function mandatory(item) {
  /* 返回根据前面的条件，从两个候选结果中选择一个：( item.builtin && !growthItem(item) && (selected.value.code === "SUPER_ADMIN" || (selected.value.cod…，当前函数到此结束。 */
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
/* selectable：调用 selectable 完成selectable处理。 */
function selectable(item) {
  /* 检查条件 growthItem(item) 是否成立，成立时走这一分支。 */
  if (growthItem(item)) /* 返回条件判断的结果：selected.value.code === "USER" && item.enabled，当前函数到此结束。 */ return selected.value.code === "USER" && item.enabled;
  /* 检查条件 mandatory(item) 是否成立，成立时走这一分支。 */
  if (mandatory(item)) /* 返回假，表示条件不成立或开关关闭，当前函数到此结束。 */ return false;
  /* 检查条件 !item.enabled 是否成立，成立时走这一分支。 */
  if (!item.enabled) /* 返回假，表示条件不成立或开关关闭，当前函数到此结束。 */ return false;
  /* 检查当前用户是否具有需要的权限：mode.value !== "permissions" || selected.value.code === "SUPER_ADMIN"，成立时走这一分支。 */
  if (mode.value !== "permissions" || selected.value.code === "SUPER_ADMIN")
    /* 返回真，表示条件成立或开关打开，当前函数到此结束。 */
    return true;
  const /* 个人（personal）保存调用 /identity:(profile|password|timezone|device):/.test 完成test处理。 */ personal = /identity:(profile|password|timezone|device):/.test(
    item.code,
  );
  /* 返回根据前面的条件，从两个候选结果中选择一个：selected.value.code === "USER" ? personal : personal || /identity:(session|login_log|operation_log):…，当前函数到此结束。 */
  return selected.value.code === "USER"
    ? personal
    : personal ||
        /identity:(session|login_log|operation_log):/.test(item.code) ||
        (item.code.startsWith("identity:user:") &&
          !item.code.endsWith(":assign_role"));
}
/* 保存（save）：调用 save 完成保存处理。 */
async function save() {
  /* 把真，表示条件成立或开关打开写入 busy.value。 */
  busy.value = true;
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    const /* 后缀（suffix）保存根据前面的条件，从两个候选结果中选择一个：mode.value === "edit" ? "" : "/" + mode.value。 */ suffix = mode.value === "edit" ? "" : "/" + mode.value;
    let /* 请求正文（body）保存根据前面的条件，从两个候选结果中选择一个：mode.value === "edit" ? { ...form.value, version: selected.value.version } : { [mode.value === "perm…。 */ body =
      mode.value === "edit"
        ? { ...form.value, /* 版本号（version）字段使用表达式 selected.value.version 的结果。 */ version: selected.value.version }
        : {
            /* mode值权限列表权限编号列表（[mode.value === "permissions" ? "permissionIds"）字段使用表达式 "menuIds"]: checked.value 的结果。 */
            [mode.value === "permissions" ? "permissionIds" : "menuIds"]:
              checked.value,
            /* 版本号（version）字段使用表达式 selected.value.version 的结果。 */
            version: selected.value.version,
          };
    /* 检查条件 mode.value === "edit" && !form.value.name?.trim() 是否成立，成立时走这一分支。 */
    if (mode.value === "edit" && !form.value.name?.trim())
      /* 抛出新建 错误（Error） 实例，当前操作按失败处理。 */
      throw new Error("请填写角色名称");
    /* 等待调用 api 完成接口处理。 */
    await api("/roles/" + selected.value.id + suffix, {
      /* 方法（method）字段使用根据前面的条件，从两个候选结果中选择一个：mode.value === "edit" ? "PATCH" : "PUT"。 */
      method: mode.value === "edit" ? "PATCH" : "PUT",
      /* 直接使用已有的请求正文（body）作为同名字段。 */
      body,
    });
    /* 把固定文字 ''写入 mode.value。 */
    mode.value = "";
    /* 调用 ElMessage.success 完成成功处理。 */
    ElMessage.success("角色已更新");
    /* 等待读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */
    await load();
    /* 等待加载 当前（Current）。 */
    await loadCurrent();
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 调用 report 完成复盘报告处理。 */
    report(e);
  } finally {
    /* 把假，表示条件不成立或开关关闭写入 busy.value。 */
    busy.value = false;
  }
}
/* 注册组件首次挂载完成后的处理。 */
onMounted(load);
</script>
<template>
  <!-- 显示一个有独立含义的内容区；用样式类 card 控制外观。 -->
  <section class="card">
    <!-- 显示Element Plus 的 alert 组件；仅当 error 成立时渲染；title 取自 error；类型设为 error；closable 取自 false。 -->
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <!-- 显示一个布局容器；用样式类 toolbar 控制外观。 -->
    <div class="toolbar">
      <!-- 显示Element Plus 的 button 组件；click 事件触发时执行 load。 -->
      <el-button @click="load">刷新角色</el-button
      ><!-- 显示行内文字区域；用样式类 muted 控制外观。 --><span class="muted">系统使用用户、管理员、超级管理员三种角色。</span>
    </div>
    <!-- 显示Element Plus 的 table 组件；data 取自 roles。 -->
    <el-table :data="roles"
      ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 empty 插槽。 --><template #empty
        ><!-- 显示应用空状态当前状态（AppEmptyState）组件；description 取自 error ? '加载失败，请查看提示后重试。' : '暂无记录，可调整筛选条件或从第一条记录开始。'。 --><AppEmptyState
          :description="
            error
              ? '加载失败，请查看提示后重试。'
              : '暂无记录，可调整筛选条件或从第一条记录开始。'
          " /></template
      ><!-- 显示Element Plus 的 table-column 组件；prop 固定为 name；label 固定为 角色名称。 --><el-table-column prop="name" label="角色名称" /><!-- 显示Element Plus 的 table-column 组件；prop 固定为 code；label 固定为 角色编码。 --><el-table-column
        prop="code"
        label="角色编码"
      /><!-- 显示Element Plus 的 table-column 组件；prop 固定为 description；label 固定为 说明；min-width 固定为 200。 --><el-table-column
        prop="description"
        label="说明"
        min-width="200"
      /><!-- 显示Element Plus 的 table-column 组件；label 固定为 操作；width 固定为 300。 --><el-table-column label="操作" width="300"
        ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"
          ><!-- 显示Element Plus 的 button 组件；仅当 can('identity:role:update') 成立时渲染；启用 link 属性；类型设为 primary；click 事件触发时执行 open(row, 'edit')。 --><el-button
            v-if="can('identity:role:update')"
            link
            type="primary"
            @click="open(row, 'edit')"
            >编辑</el-button
          ><!-- 显示Element Plus 的 button 组件；仅当 can('identity:role:grant_permission') 成立时渲染；启用 link 属性；类型设为 primary；click 事件触发时执行 open(row, 'permissions')。 --><el-button
            v-if="can('identity:role:grant_permission')"
            link
            type="primary"
            @click="open(row, 'permissions')"
            >分配权限</el-button
          ><!-- 显示Element Plus 的 button 组件；仅当 can('identity:role:grant_menu') 成立时渲染；启用 link 属性；类型设为 primary；click 事件触发时执行 open(row, 'menus')。 --><el-button
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
  <!-- 显示Element Plus 的 drawer 组件；model-value 取自 !!mode；update:model-value 事件触发时执行 !$event && (mode = '')；title 取自 selected?.name + ' · ' + ({ edit: '角色资料', permissions: '分配权限', menus: '分配菜单' }[mode] || '')；size 固定为 min(680px, 100%)；close-on-click-modal 取自 false。 -->
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
    ><!-- 显示Element Plus 的 form 组件；仅当 mode === 'edit' 成立时渲染；label-position 固定为 top。 --><el-form v-if="mode === 'edit'" label-position="top"
      ><!-- 显示Element Plus 的 form-item 组件；label 固定为 名称。 --><el-form-item label="名称"
        ><!-- 显示Element Plus 的 input 组件；与 form.name 双向同步；maxlength 固定为 64。 --><el-input v-model="form.name" maxlength="64" /></el-form-item
      ><!-- 显示Element Plus 的 form-item 组件；label 固定为 说明。 --><el-form-item label="说明"
        ><!-- 显示Element Plus 的 input 组件；与 form.description 双向同步；类型设为 textarea；maxlength 固定为 255。 --><el-input
          v-model="form.description"
          type="textarea"
          maxlength="255" /></el-form-item></el-form
    ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；前面条件均不成立时显示这个备用分支。 --><template v-else
      ><!-- 显示Element Plus 的 alert 组件；title 固定为 核心账户权限和内置菜单受到保护。请先分配动作权限，再分配对应菜单。；类型设为 info；closable 取自 false。 --><el-alert
        title="核心账户权限和内置菜单受到保护。请先分配动作权限，再分配对应菜单。"
        type="info"
        :closable="false"
      /><!-- 显示Element Plus 的 checkbox-group 组件；与 checked 双向同步；用样式类 grant-list 控制外观。 --><el-checkbox-group v-model="checked" class="grant-list"
        ><!-- 显示Element Plus 的 checkbox 组件；按 item in options 为每条数据重复生成内容；key 取自 item.id；value 取自 item.id；disabled 取自 !selectable(item)。 --><el-checkbox
          v-for="item in options"
          :key="item.id"
          :value="item.id"
          :disabled="!selectable(item)"
          ><!-- 显示行内文字区域。 --><span><!-- 把 item.name 的当前值转成文字显示。 -->{{ item.name }}</span
          ><!-- 显示辅助说明文字。 --><small><!-- 把 item.code 的当前值转成文字显示。 -->{{ item.code }}</small></el-checkbox
        ></el-checkbox-group
      ></template
    ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 footer 插槽。 --><template #footer
      ><!-- 显示Element Plus 的 button 组件；click 事件触发时执行 mode = ''。 --><el-button @click="mode = ''">取消</el-button
      ><!-- 显示Element Plus 的 button 组件；类型设为 primary；loading 取自 busy；click 事件触发时执行 save。 --><el-button type="primary" :loading="busy" @click="save"
        >保存</el-button
      ></template
    ></el-drawer
  >
</template>
