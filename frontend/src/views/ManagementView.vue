<!-- 渲染Management页面界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
/* 引入 vue 提供的组件、状态函数或请求方法。 */
import { ref, onMounted, computed } from "vue";
/* 引入 element-plus 提供的组件、状态函数或请求方法。 */
import { ElMessage } from "element-plus";
/* 引入 ../api/http 提供的组件、状态函数或请求方法。 */
import {
  api,
  session,
  hasPermission as can,
  allPermissions,
  loadCurrent,
} from "../api/http";
/* 引入 ../api/ui 提供的组件、状态函数或请求方法。 */
import {
  report,
  confirmAction,
  browserZone,
  validateAccount,
  validatePassword,
} from "../api/ui";
/* 引入 ../components/DetailsDrawer.vue 提供的组件、状态函数或请求方法。 */
import DetailsDrawer from "../components/DetailsDrawer.vue";
/* 引入 ../components/TimeZoneSelect.vue 提供的组件、状态函数或请求方法。 */
import TimeZoneSelect from "../components/TimeZoneSelect.vue";
/* 引入 ../components/UserCharts.vue 提供的组件、状态函数或请求方法。 */
import UserCharts from "../components/UserCharts.vue";
const /* 父组件传入的数据（props）保存声明父组件可以传入的属性及其类型或默认值。 */ props = defineProps({ /* resource字段使用可为空的文字（String）。 */ resource: String });
const /* 是否用户（isUser）保存条件判断的结果：props.resource === "users"。 */ isUser = props.resource === "users",
  /* 是否菜单（isMenu）保存条件判断的结果：props.resource === "menus"。 */
  isMenu = props.resource === "menus";
const /* subject保存根据前面的条件，从两个候选结果中选择一个：isUser ? "user" : isMenu ? "menu" : "permission"。 */ subject = isUser ? "user" : isMenu ? "menu" : "permission";
const /* 显示名称（label）保存根据前面的条件，从两个候选结果中选择一个：isUser ? "用户" : isMenu ? "菜单" : "权限"。 */ label = isUser ? "用户" : isMenu ? "菜单" : "权限";
const /* permit保存表达式 (action) => can(`identity:${subject}:${action}`) 的结果。 */ permit = (action) => can(`identity:${subject}:${action}`);
const /* 数据列表（items）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ items = ref([]),
  /* 总数（total）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  total = ref(0),
  /* 当前页（page）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  page = ref(1),
  /* 每页条数（size）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  size = ref(20),
  /* keyword保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  keyword = ref(""),
  /* 是否启用（enabled）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  enabled = ref(null),
  /* 角色（role）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  role = ref(""),
  /* 错误（error）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  error = ref(""),
  /* 正在处理（busy）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  busy = ref(false),
  /* submitting保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  submitting = ref(false),
  /* 弹出层（modal）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  modal = ref(""),
  /* form保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  form = ref({}),
  /* 详情（detail）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  detail = ref(null),
  /* show详情（showDetail）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  showDetail = ref(false),
  /* 权限列表（permissions）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  permissions = ref([]),
  /* 角色列表（roles）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  roles = ref([]),
  /* assigned保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  assigned = ref([]),
  /* stats保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  stats = ref(null),
  /* expanded保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  expanded = ref(true),
  /* tree键（treeKey）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  treeKey = ref(0);
const /* 路径（path）保存按表达式计算得到的值："/" + props.resource。 */ path = "/" + props.resource;
const /* filtered保存随依赖自动重算的筛选结果或数量。 */ filtered = computed(() =>
  isMenu && keyword.value
    ? items.value.filter((row) =>
        JSON.stringify(row).toLowerCase().includes(keyword.value.toLowerCase()),
      )
    : items.value,
);
/* 加载（load）：读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */
async function load() {
  /* 把真，表示条件成立或开关打开写入 busy.value。 */
  busy.value = true;
  /* 把固定文字 ''写入 error.value。 */
  error.value = "";
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    const /* 查询条件（query）保存新建 地址搜索参数集合（URLSearchParams） 实例。 */ query = new URLSearchParams({ /* 当前页（page）字段使用表达式 page.value 的结果。 */ page: page.value, /* 每页条数（size）字段使用表达式 size.value 的结果。 */ size: size.value });
    /* 检查条件 keyword.value && !isMenu 是否成立，成立时走这一分支。 */
    if (keyword.value && !isMenu) /* 调用 query.set 完成设置处理。 */ query.set("keyword", keyword.value);
    /* 检查enabled.value !== null && enabled.value !== ""，即对应内容是否不存在，成立时走这一分支。 */
    if (enabled.value !== null && enabled.value !== "")
      /* 调用 query.set 完成设置处理。 */
      query.set("enabled", enabled.value);
    /* 检查条件 role.value 是否成立，成立时走这一分支。 */
    if (role.value) /* 调用 query.set 完成设置处理。 */ query.set("role", role.value);
    const /* 数据（data）保存等待 根据前面的条件，从两个候选结果中选择一个：api(path + (isMenu ? "/tree" : "?" + query)) 完成后取得结果。 */ data = await api(path + (isMenu ? "/tree" : "?" + query));
    /* 把根据前面的条件，从两个候选结果中选择一个：isMenu ? data : data.items写入 items.value。 */
    items.value = isMenu ? data : data.items;
    /* 把优先使用前面的有效值，没有时采用后面的备用值：data.total || 0写入 total.value。 */
    total.value = data.total || 0;
    /* 检查条件 isUser && can("identity:user:count") 是否成立，成立时走这一分支。 */
    if (isUser && can("identity:user:count"))
      /* 把等待 调用 api 完成接口处理 完成后取得结果写入 stats.value。 */
      stats.value = await api("/user-statistics");
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 把表达式 e.message 的结果写入 error.value。 */
    error.value = e.message;
  } finally {
    /* 把假，表示条件不成立或开关关闭写入 busy.value。 */
    busy.value = false;
  }
}
/* edit：调用 edit 完成edit处理。 */
async function edit(row) {
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    const /* 数据（data）保存根据前面的条件，从两个候选结果中选择一个：row ? await api(`${path}/${row.id}`) : {}。 */ data = row ? await api(`${path}/${row.id}`) : {};
    /* 检查条件 isUser 是否成立，成立时走这一分支。 */
    if (isUser)
      /* 把优先使用前面的有效值，没有时采用后面的备用值：{ id: data.id, username: data.username || "", nickname: data.nickname || "", version: data.version, initialPas…写入 form.value。 */
      form.value = {
        /* 编号（id）字段使用表达式 data.id 的结果。 */
        id: data.id,
        /* 用户名（username）字段使用优先使用前面的有效值，没有时采用后面的备用值：data.username || ""。 */
        username: data.username || "",
        /* 昵称（nickname）字段使用优先使用前面的有效值，没有时采用后面的备用值：data.nickname || ""。 */
        nickname: data.nickname || "",
        /* 版本号（version）字段使用表达式 data.version 的结果。 */
        version: data.version,
        /* 初始密码（initialPassword）字段使用固定文字 ''。 */
        initialPassword: "",
        /* 确认密码（confirmPassword）字段使用固定文字 ''。 */
        confirmPassword: "",
        /* 用户使用的时区（timeZone）字段使用调用 browserZone 完成browser时区处理。 */
        timeZone: browserZone(),
      };
    else /* 检查条件 isMenu 是否成立，成立时走这一分支。 */ if (isMenu) {
      /* 把优先使用前面的有效值，没有时采用后面的备用值：{ id: data.id, code: data.code || "", name: data.name || "", parentId: data.parentId || null, menuType: data.m…写入 form.value。 */
      form.value = {
        /* 编号（id）字段使用表达式 data.id 的结果。 */
        id: data.id,
        /* 编码（code）字段使用优先使用前面的有效值，没有时采用后面的备用值：data.code || ""。 */
        code: data.code || "",
        /* 名称（name）字段使用优先使用前面的有效值，没有时采用后面的备用值：data.name || ""。 */
        name: data.name || "",
        /* 上级编号（parentId）字段使用优先使用前面的有效值，没有时采用后面的备用值：data.parentId || null。 */
        parentId: data.parentId || null,
        /* 菜单类型（menuType）字段使用优先使用前面的有效值，没有时采用后面的备用值：data.menuType || "PAGE"。 */
        menuType: data.menuType || "PAGE",
        /* route路径（routePath）字段使用优先使用前面的有效值，没有时采用后面的备用值：data.routePath || ""。 */
        routePath: data.routePath || "",
        /* component键（componentKey）字段使用优先使用前面的有效值，没有时采用后面的备用值：data.componentKey || ""。 */
        componentKey: data.componentKey || "",
        /* 权限编号（permissionId）字段使用优先使用前面的有效值，没有时采用后面的备用值：data.permissionId || null。 */
        permissionId: data.permissionId || null,
        /* icon字段使用优先使用前面的有效值，没有时采用后面的备用值：data.icon || ""。 */
        icon: data.icon || "",
        /* 排序顺序（sortOrder）字段使用优先使用前面的有效值，没有时采用后面的备用值：data.sortOrder || 0。 */
        sortOrder: data.sortOrder || 0,
        /* 是否显示（visible）字段使用前面的值为空时才使用后面的默认值：data.visible ?? true。 */
        visible: data.visible ?? true,
        /* 是否启用（enabled）字段使用前面的值为空时才使用后面的默认值：data.enabled ?? true。 */
        enabled: data.enabled ?? true,
        /* 版本号（version）字段使用表达式 data.version 的结果。 */
        version: data.version,
        /* builtin字段使用表达式 data.builtin 的结果。 */
        builtin: data.builtin,
      };
      /* 把等待 调用 allPermissions 完成全部权限列表处理 完成后取得结果写入 permissions.value。 */
      permissions.value = await allPermissions();
    } else
      /* 把优先使用前面的有效值，没有时采用后面的备用值：{ id: data.id, code: data.code || "", name: data.name || "", description: data.description || "", enabled: dat…写入 form.value。 */
      form.value = {
        /* 编号（id）字段使用表达式 data.id 的结果。 */
        id: data.id,
        /* 编码（code）字段使用优先使用前面的有效值，没有时采用后面的备用值：data.code || ""。 */
        code: data.code || "",
        /* 名称（name）字段使用优先使用前面的有效值，没有时采用后面的备用值：data.name || ""。 */
        name: data.name || "",
        /* 说明（description）字段使用优先使用前面的有效值，没有时采用后面的备用值：data.description || ""。 */
        description: data.description || "",
        /* 是否启用（enabled）字段使用前面的值为空时才使用后面的默认值：data.enabled ?? true。 */
        enabled: data.enabled ?? true,
        /* 版本号（version）字段使用表达式 data.version 的结果。 */
        version: data.version,
        /* builtin字段使用表达式 data.builtin 的结果。 */
        builtin: data.builtin,
      };
    /* 把固定文字 'edit'写入 modal.value。 */
    modal.value = "edit";
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 调用 report 完成复盘报告处理。 */
    report(e);
  }
}
/* 保存（save）：调用 save 完成保存处理。 */
async function save() {
  /* 检查条件 submitting.value 是否成立，成立时走这一分支。 */
  if (submitting.value) /* 返回空内容，当前函数到此结束。 */ return;
  /* 把真，表示条件成立或开关打开写入 submitting.value。 */
  submitting.value = true;
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    const /* f保存表达式 form.value 的结果。 */ f = form.value;
    let /* 声明 请求正文（body），稍后再填入处理结果。 */ body;
    /* 检查条件 isUser 是否成立，成立时走这一分支。 */
    if (isUser) {
      /* 检查 账号（Account）。 */
      validateAccount(f, f.id ? null : "initialPassword");
      /* 把根据前面的条件，从两个候选结果中选择一个：f.id ? { username: f.username, nickname: f.nickname, version: f.version } : { username: f.username, …写入 body。 */
      body = f.id
        ? { /* 用户名（username）字段使用表达式 f.username 的结果。 */ username: f.username, /* 昵称（nickname）字段使用表达式 f.nickname 的结果。 */ nickname: f.nickname, /* 版本号（version）字段使用表达式 f.version 的结果。 */ version: f.version }
        : {
            /* 用户名（username）字段使用表达式 f.username 的结果。 */
            username: f.username,
            /* 昵称（nickname）字段使用表达式 f.nickname 的结果。 */
            nickname: f.nickname,
            /* 初始密码（initialPassword）字段使用表达式 f.initialPassword 的结果。 */
            initialPassword: f.initialPassword,
            /* 确认密码（confirmPassword）字段使用表达式 f.confirmPassword 的结果。 */
            confirmPassword: f.confirmPassword,
            /* 用户使用的时区（timeZone）字段使用表达式 f.timeZone 的结果。 */
            timeZone: f.timeZone,
          };
    } else {
      /* 检查条件 !f.name.trim() || !f.code.trim() 是否成立，成立时走这一分支。 */
      if (!f.name.trim() || !f.code.trim()) /* 抛出新建 错误（Error） 实例，当前操作按失败处理。 */ throw new Error("请填写名称和编码");
      /* 检查条件 isMenu 是否成立，成立时走这一分支。 */
      if (isMenu)
        /* 把根据前面的条件，从两个候选结果中选择一个：{ code: f.code, name: f.name, parentId: f.parentId || null, menuType: f.menuType, routePath: f.menuT…写入 body。 */
        body = {
          /* 编码（code）字段使用表达式 f.code 的结果。 */
          code: f.code,
          /* 名称（name）字段使用表达式 f.name 的结果。 */
          name: f.name,
          /* 上级编号（parentId）字段使用优先使用前面的有效值，没有时采用后面的备用值：f.parentId || null。 */
          parentId: f.parentId || null,
          /* 菜单类型（menuType）字段使用表达式 f.menuType 的结果。 */
          menuType: f.menuType,
          /* route路径（routePath）字段使用根据前面的条件，从两个候选结果中选择一个：f.menuType === "BUTTON" ? null : f.routePath。 */
          routePath: f.menuType === "BUTTON" ? null : f.routePath,
          /* component键（componentKey）字段使用根据前面的条件，从两个候选结果中选择一个：f.menuType === "PAGE" ? f.componentKey : null。 */
          componentKey: f.menuType === "PAGE" ? f.componentKey : null,
          /* 权限编号（permissionId）字段使用根据前面的条件，从两个候选结果中选择一个：f.menuType === "DIRECTORY" ? null : f.permissionId。 */
          permissionId: f.menuType === "DIRECTORY" ? null : f.permissionId,
          /* icon字段使用优先使用前面的有效值，没有时采用后面的备用值：f.icon || null。 */
          icon: f.icon || null,
          /* 排序顺序（sortOrder）字段使用表达式 f.sortOrder 的结果。 */
          sortOrder: f.sortOrder,
          /* 是否显示（visible）字段使用表达式 f.visible 的结果。 */
          visible: f.visible,
          /* 是否启用（enabled）字段使用表达式 f.enabled 的结果。 */
          enabled: f.enabled,
          /* 版本号（version）字段使用表达式 f.version 的结果。 */
          version: f.version,
        };
      else
        /* 把表达式 { code: f.code, name: f.name, description: f.description, enabled: f.enabled, version: f.version, } 的结果写入 body。 */
        body = {
          /* 编码（code）字段使用表达式 f.code 的结果。 */
          code: f.code,
          /* 名称（name）字段使用表达式 f.name 的结果。 */
          name: f.name,
          /* 说明（description）字段使用表达式 f.description 的结果。 */
          description: f.description,
          /* 是否启用（enabled）字段使用表达式 f.enabled 的结果。 */
          enabled: f.enabled,
          /* 版本号（version）字段使用表达式 f.version 的结果。 */
          version: f.version,
        };
    }
    /* 等待调用 api 完成接口处理。 */
    await api(path + (f.id ? "/" + f.id : ""), {
      /* 方法（method）字段使用根据前面的条件，从两个候选结果中选择一个：f.id ? "PATCH" : "POST"。 */
      method: f.id ? "PATCH" : "POST",
      /* 直接使用已有的请求正文（body）作为同名字段。 */
      body,
    });
    /* 把固定文字 ''写入 modal.value。 */
    modal.value = "";
    /* 调用 ElMessage.success 完成成功处理。 */
    ElMessage.success("保存成功");
    /* 等待读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */
    await load();
    /* 检查条件 !isUser 是否成立，成立时走这一分支。 */
    if (!isUser) /* 等待加载 当前（Current）。 */ await loadCurrent();
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 调用 report 完成复盘报告处理。 */
    report(e);
  } finally {
    /* 把假，表示条件不成立或开关关闭写入 submitting.value。 */
    submitting.value = false;
  }
}
/* 移除（remove）：调用 remove 完成移除处理。 */
async function remove(row) {
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    /* 等待确认 操作（Action）。 */
    await confirmAction(`确定删除${label}“${row.username || row.name}”？`);
    /* 等待调用 api 完成接口处理。 */
    await api(`${path}/${row.id}?version=${row.version}`, { /* 方法（method）字段使用固定状态或编码 DELETE（删除）。 */ method: "DELETE" });
    /* 调用 ElMessage.success 完成成功处理。 */
    ElMessage.success("已删除");
    /* 等待读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */
    await load();
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 调用 report 完成复盘报告处理。 */
    report(e);
  }
}
/* toggle：调用 toggle 完成toggle处理。 */
async function toggle(row) {
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    /* 等待确认 操作（Action）。 */
    await confirmAction(`确定${row.enabled ? "禁用" : "启用"}该用户？`);
    /* 等待调用 api 完成接口处理。 */
    await api(`${path}/${row.id}/status`, {
      /* 方法（method）字段使用固定状态或编码 PATCH（PATCH）。 */
      method: "PATCH",
      /* 请求正文（body）字段使用条件判断的结果：{ enabled: !row.enabled, version: row.version }。 */
      body: { /* 是否启用（enabled）字段使用条件判断的结果：!row.enabled。 */ enabled: !row.enabled, /* 版本号（version）字段使用表达式 row.version 的结果。 */ version: row.version },
    });
    /* 等待读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */
    await load();
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 调用 report 完成复盘报告处理。 */
    report(e);
  }
}
/* reset密码（resetPassword）：调用 resetPassword 完成reset密码处理。 */
async function resetPassword(row) {
  /* 把表达式 { id: row.id, version: row.version, newPassword: "", confirmPassword: "", } 的结果写入 form.value。 */
  form.value = {
    /* 编号（id）字段使用表达式 row.id 的结果。 */
    id: row.id,
    /* 版本号（version）字段使用表达式 row.version 的结果。 */
    version: row.version,
    /* 新密码（newPassword）字段使用固定文字 ''。 */
    newPassword: "",
    /* 确认密码（confirmPassword）字段使用固定文字 ''。 */
    confirmPassword: "",
  };
  /* 把固定文字 'password'写入 modal.value。 */
  modal.value = "password";
}
/* 安排角色列表（assignRoles）：调用 assignRoles 完成安排角色列表处理。 */
async function assignRoles(row) {
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    /* 把等待 调用 api 完成接口处理 完成后取得结果写入 roles.value。 */
    roles.value = await api("/roles");
    /* 把等待 调用 api 完成接口处理 完成后取得结果写入 assigned.value。 */
    assigned.value = await api(`/users/${row.id}/roles`);
    /* 把表达式 { id: row.id, version: row.version } 的结果写入 form.value。 */
    form.value = { /* 编号（id）字段使用表达式 row.id 的结果。 */ id: row.id, /* 版本号（version）字段使用表达式 row.version 的结果。 */ version: row.version };
    /* 把固定文字 'roles'写入 modal.value。 */
    modal.value = "roles";
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 调用 report 完成复盘报告处理。 */
    report(e);
  }
}
/* 保存Extra（saveExtra）：保存 Extra。 */
async function saveExtra() {
  /* 把真，表示条件成立或开关打开写入 submitting.value。 */
  submitting.value = true;
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    const /* f保存表达式 form.value 的结果。 */ f = form.value;
    /* 检查条件 modal.value === "password" 是否成立，成立时走这一分支。 */
    if (modal.value === "password") {
      /* 检查 密码（Password）。 */
      validatePassword(f.newPassword, f.confirmPassword);
      /* 等待调用 api 完成接口处理。 */
      await api(`/users/${f.id}/password`, {
        /* 方法（method）字段使用固定状态或编码 PUT（PUT）。 */
        method: "PUT",
        /* 请求正文（body）字段使用表达式 { newPassword: f.newPassword, confirmPassword: f.confirmPassword, version: f.version, } 的结果。 */
        body: {
          /* 新密码（newPassword）字段使用表达式 f.newPassword 的结果。 */
          newPassword: f.newPassword,
          /* 确认密码（confirmPassword）字段使用表达式 f.confirmPassword 的结果。 */
          confirmPassword: f.confirmPassword,
          /* 版本号（version）字段使用表达式 f.version 的结果。 */
          version: f.version,
        },
      });
    } else {
      /* 等待调用 api 完成接口处理。 */
      await api(`/users/${f.id}/roles`, {
        /* 方法（method）字段使用固定状态或编码 PUT（PUT）。 */
        method: "PUT",
        /* 请求正文（body）字段使用表达式 { roleIds: assigned.value, version: f.version } 的结果。 */
        body: { /* 角色编号列表（roleIds）字段使用表达式 assigned.value 的结果。 */ roleIds: assigned.value, /* 版本号（version）字段使用表达式 f.version 的结果。 */ version: f.version },
      });
    }
    /* 把固定文字 ''写入 modal.value。 */
    modal.value = "";
    /* 调用 ElMessage.success 完成成功处理。 */
    ElMessage.success("操作成功");
    /* 等待读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */
    await load();
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 调用 report 完成复盘报告处理。 */
    report(e);
  } finally {
    /* 把假，表示条件不成立或开关关闭写入 submitting.value。 */
    submitting.value = false;
  }
}
/* show：调用 show 完成show处理。 */
async function show(row) {
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    /* 把等待 调用 api 完成接口处理 完成后取得结果写入 detail.value。 */
    detail.value = await api(`${path}/${row.id}`);
    /* 把真，表示条件成立或开关打开写入 showDetail.value。 */
    showDetail.value = true;
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 调用 report 完成复盘报告处理。 */
    report(e);
  }
}
/* parents：调用 parents 完成parents处理。 */
function parents(nodes, depth = 0) {
  /* 返回优先使用前面的有效值，没有时采用后面的备用值：nodes.flatMap((n) => [ { ...n, name: " ".repeat(depth) + n.name }, ...parents(n.children || [], depth + 1), ])，当前函数到此结束。 */
  return nodes.flatMap((n) => [
    { ...n, /* 名称（name）字段使用调用 " ".repeat 完成repeat处理。 */ name: "　".repeat(depth) + n.name },
    ...parents(n.children || [], depth + 1),
  ]);
}
const /* components保存按表达式计算得到的值：[ "account/profile", "account/devices", "system/users", "system/roles", "system/permissions", "system/menus", "system/se…。 */ components = [
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
  "growth/goals",
  "growth/stages",
  "growth/tasks",
  "planning/weekly",
  "planning/daily",
  "planning/pending",
  "execution/index",
  "habit/index",
  "habit/categories",
  "notification/index",
  "dashboard/index",
  "statistics/index",
];
/* 注册组件首次挂载完成后的处理。 */
onMounted(load);
</script>
<template>
  <!-- 显示用户Charts（UserCharts）组件；仅当 stats 成立时渲染；statistics 取自 stats；refresh 事件触发时执行 load。 -->
  <UserCharts v-if="stats" :statistics="stats" @refresh="load" />
  <!-- 显示一个有独立含义的内容区；用样式类 card 控制外观。 -->
  <section class="card">
    <!-- 显示一个布局容器；用样式类 toolbar 控制外观。 -->
    <div class="toolbar">
      <!-- 显示Element Plus 的 input 组件；与 keyword 双向同步；未输入时提示 名称或编码关键字；启用 clearable 属性；keyup 事件触发时执行 page = 1; load();。 -->
      <el-input
        v-model="keyword"
        placeholder="名称或编码关键字"
        clearable
        @keyup.enter="
          page = 1;
          load();
        "
      /><!-- 显示Element Plus 的 select 组件；仅当 !isMenu 成立时渲染；与 enabled 双向同步；启用 clearable 属性；未输入时提示 全部状态。 --><el-select
        v-if="!isMenu"
        v-model="enabled"
        clearable
        placeholder="全部状态"
        ><!-- 显示Element Plus 的 option 组件；label 固定为 启用；value 取自 true。 --><el-option label="启用" :value="true" /><!-- 显示Element Plus 的 option 组件；label 固定为 禁用；value 取自 false。 --><el-option
          label="禁用"
          :value="false" /></el-select
      ><!-- 显示Element Plus 的 select 组件；仅当 isUser && session.user.roles.includes('SUPER_ADMIN') 成立时渲染；与 role 双向同步；启用 clearable 属性；未输入时提示 全部角色。 --><el-select
        v-if="isUser && session.user.roles.includes('SUPER_ADMIN')"
        v-model="role"
        clearable
        placeholder="全部角色"
        ><!-- 显示Element Plus 的 option 组件；label 固定为 用户；value 固定为 USER。 --><el-option label="用户" value="USER" /><!-- 显示Element Plus 的 option 组件；label 固定为 管理员；value 固定为 ADMIN。 --><el-option
          label="管理员"
          value="ADMIN" /><!-- 显示Element Plus 的 option 组件；label 固定为 超级管理员；value 固定为 SUPER_ADMIN。 --><el-option
          label="超级管理员"
          value="SUPER_ADMIN" /></el-select
      ><!-- 显示Element Plus 的 button 组件；类型设为 primary；click 事件触发时执行 page = 1; load();。 --><el-button
        type="primary"
        @click="
          page = 1;
          load();
        "
        >查询</el-button
      ><!-- 显示Element Plus 的 button 组件；click 事件触发时执行 keyword = ''; enabled = null; role = ''; page = 1; load();。 --><el-button
        @click="
          keyword = '';
          enabled = null;
          role = '';
          page = 1;
          load();
        "
        >重置</el-button
      ><!-- 显示Element Plus 的 button 组件；仅当 permit('create') 成立时渲染；类型设为 primary；启用 plain 属性；click 事件触发时执行 edit()。 --><el-button v-if="permit('create')" type="primary" plain @click="edit()"
        >新增<!-- 把 label 的当前值转成文字显示。 -->{{ label }}</el-button
      ><!-- 显示Element Plus 的 button 组件；仅当 isMenu 成立时渲染；click 事件触发时执行 expanded = !expanded; treeKey++;。 --><el-button
        v-if="isMenu"
        @click="
          expanded = !expanded;
          treeKey++;
        "
        >展开／折叠</el-button
      >
    </div>
    <!-- 显示Element Plus 的 alert 组件；仅当 error 成立时渲染；title 取自 error；类型设为 error；closable 取自 false。 -->
    <el-alert
      v-if="error"
      :title="error"
      type="error"
      :closable="false"
    /><!-- 显示Element Plus 的 table 组件；key 取自 treeKey；data 取自 filtered；row-key 固定为 id；default-expand-all 取自 expanded；loading 指令使用 busy；empty-text 固定为 暂无符合条件的记录。 --><el-table
      :key="treeKey"
      :data="filtered"
      row-key="id"
      :default-expand-all="expanded"
      v-loading="busy"
      empty-text="暂无符合条件的记录"
      ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 empty 插槽。 --><template #empty
        ><!-- 显示应用空状态当前状态（AppEmptyState）组件；description 取自 error ? '加载失败，请查看提示后重试。' : '暂无符合条件的记录'。 --><AppEmptyState
          :description="
            error ? '加载失败，请查看提示后重试。' : '暂无符合条件的记录'
          " /></template
      ><!-- 显示Element Plus 的 table-column 组件；prop 取自 isUser ? 'username' : 'name'；label 取自 isUser ? '用户名' : '名称'；min-width 固定为 180。 --><el-table-column
        :prop="isUser ? 'username' : 'name'"
        :label="isUser ? '用户名' : '名称'"
        min-width="180"
      /><!-- 显示Element Plus 的 table-column 组件；仅当 isUser 成立时渲染；prop 固定为 nickname；label 固定为 昵称；min-width 固定为 130。 --><el-table-column
        v-if="isUser"
        prop="nickname"
        label="昵称"
        min-width="130"
      /><!-- 显示Element Plus 的 table-column 组件；前面条件均不成立时显示这个备用分支；prop 固定为 code；label 固定为 编码；min-width 固定为 220。 --><el-table-column
        v-else
        prop="code"
        label="编码"
        min-width="220"
      /><!-- 显示Element Plus 的 table-column 组件；仅当 isMenu 成立时渲染；prop 固定为 menuType；label 固定为 类型；width 固定为 120。 --><el-table-column
        v-if="isMenu"
        prop="menuType"
        label="类型"
        width="120"
      /><!-- 显示Element Plus 的 table-column 组件；仅当 isMenu 成立时渲染；prop 固定为 routePath；label 固定为 路径；min-width 固定为 180。 --><el-table-column
        v-if="isMenu"
        prop="routePath"
        label="路径"
        min-width="180"
      /><!-- 显示Element Plus 的 table-column 组件；仅当 isUser 成立时渲染；label 固定为 角色；min-width 固定为 160。 --><el-table-column v-if="isUser" label="角色" min-width="160"
        ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"
          ><!-- 显示Element Plus 的 tag 组件；按 r in row.roles 为每条数据重复生成内容；key 取自 r。 --><el-tag v-for="r in row.roles" :key="r"><!-- 把 r 的当前值转成文字显示。 -->{{ r }}</el-tag></template
        ></el-table-column
      ><!-- 显示Element Plus 的 table-column 组件；label 固定为 状态；width 固定为 90。 --><el-table-column label="状态" width="90"
        ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"
          ><!-- 显示Element Plus 的 tag 组件；type 取自 row.enabled ? 'success' : 'info'。 --><el-tag :type="row.enabled ? 'success' : 'info'"><!-- 把 row.enabled ? "启用" : "禁用" 的当前值转成文字显示。 -->{{
            row.enabled ? "启用" : "禁用"
          }}</el-tag></template
        ></el-table-column
      ><!-- 显示Element Plus 的 table-column 组件；label 固定为 操作；min-width 固定为 330；fixed 固定为 right。 --><el-table-column label="操作" min-width="330" fixed="right"
        ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"
          ><!-- 显示Element Plus 的 button 组件；仅当 permit('read') 成立时渲染；启用 link 属性；类型设为 primary；click 事件触发时执行 show(row)。 --><el-button
            v-if="permit('read')"
            link
            type="primary"
            @click="show(row)"
            >详情</el-button
          ><!-- 显示Element Plus 的 button 组件；仅当 permit('update') 成立时渲染；启用 link 属性；类型设为 primary；click 事件触发时执行 edit(row)。 --><el-button
            v-if="permit('update')"
            link
            type="primary"
            @click="edit(row)"
            >编辑</el-button
          ><!-- 显示Element Plus 的 button 组件；仅当 isUser && permit('status') 成立时渲染；启用 link 属性；类型设为 warning；click 事件触发时执行 toggle(row)。 --><el-button
            v-if="isUser && permit('status')"
            link
            type="warning"
            @click="toggle(row)"
            ><!-- 把 row.enabled ? "禁用" : "启用" 的当前值转成文字显示。 -->{{ row.enabled ? "禁用" : "启用" }}</el-button
          ><!-- 显示Element Plus 的 button 组件；仅当 isUser && permit('reset_password') 成立时渲染；启用 link 属性；类型设为 primary；click 事件触发时执行 resetPassword(row)。 --><el-button
            v-if="isUser && permit('reset_password')"
            link
            type="primary"
            @click="resetPassword(row)"
            >重置密码</el-button
          ><!-- 显示Element Plus 的 button 组件；仅当 isUser && permit('assign_role') 成立时渲染；启用 link 属性；类型设为 primary；click 事件触发时执行 assignRoles(row)。 --><el-button
            v-if="isUser && permit('assign_role')"
            link
            type="primary"
            @click="assignRoles(row)"
            >角色</el-button
          ><!-- 显示Element Plus 的 button 组件；仅当 permit('delete') 成立时渲染；disabled 取自 row.builtin；启用 link 属性；类型设为 danger；click 事件触发时执行 remove(row)。 --><el-button
            v-if="permit('delete')"
            :disabled="row.builtin"
            link
            type="danger"
            @click="remove(row)"
            >删除</el-button
          ></template
        ></el-table-column
      ></el-table
    ><!-- 显示Element Plus 的 pagination 组件；仅当 !isMenu 成立时渲染；用样式类 pagination 控制外观；与 page 双向同步（current-page）；与 size 双向同步（page-size）；total 取自 total；page-sizes 取自 [10, 20, 50, 100]；layout 固定为 total, sizes, prev, pager, next；change 事件触发时执行 load。 --><el-pagination
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
  <!-- 显示详情抽屉（DetailsDrawer）组件；与 showDetail 双向同步；value 取自 detail。 -->
  <DetailsDrawer v-model="showDetail" :value="detail" /><!-- 显示Element Plus 的 dialog 组件；model-value 取自 !!modal；update:model-value 事件触发时执行 !$event && (modal = '')；title 取自 modal === 'roles' ? '分配角色' : modal === 'password' ? '重置密码' : (form.id ? '编辑' : '新增') + label；width 固定为 min(600px, 94vw)；close-on-click-modal 取自 false。 --><el-dialog
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
    ><!-- 显示Element Plus 的 form 组件；label-position 固定为 top；submit 事件触发时执行 modal === 'edit' ? save() : saveExtra()。 --><el-form
      label-position="top"
      @submit.prevent="modal === 'edit' ? save() : saveExtra()"
      ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；仅当 modal === 'edit' 成立时渲染。 --><template v-if="modal === 'edit'"
        ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；仅当 isUser 成立时渲染。 --><template v-if="isUser"
          ><!-- 显示Element Plus 的 form-item 组件；label 固定为 用户名。 --><el-form-item label="用户名"
            ><!-- 显示Element Plus 的 input 组件；与 form.username 双向同步；maxlength 固定为 30。 --><el-input v-model="form.username" maxlength="30" /></el-form-item
          ><!-- 显示Element Plus 的 form-item 组件；label 固定为 昵称。 --><el-form-item label="昵称"
            ><!-- 显示Element Plus 的 input 组件；与 form.nickname 双向同步；maxlength 固定为 30。 --><el-input v-model="form.nickname" maxlength="30" /></el-form-item
          ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；仅当 !form.id 成立时渲染。 --><template v-if="!form.id"
            ><!-- 显示Element Plus 的 form-item 组件；label 固定为 初始密码。 --><el-form-item label="初始密码"
              ><!-- 显示Element Plus 的 input 组件；与 form.initialPassword 双向同步；类型设为 password；启用 show-password 属性。 --><el-input
                v-model="form.initialPassword"
                type="password"
                show-password /></el-form-item
            ><!-- 显示Element Plus 的 form-item 组件；label 固定为 确认密码。 --><el-form-item label="确认密码"
              ><!-- 显示Element Plus 的 input 组件；与 form.confirmPassword 双向同步；类型设为 password；启用 show-password 属性。 --><el-input
                v-model="form.confirmPassword"
                type="password"
                show-password /></el-form-item
            ><!-- 显示Element Plus 的 form-item 组件；label 固定为 初始时区。 --><el-form-item label="初始时区"
              ><!-- 显示时间时区选择（TimeZoneSelect）组件；与 form.timeZone 双向同步。 --><TimeZoneSelect
                v-model="form.timeZone" /></el-form-item></template></template
        ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；前面条件均不成立时显示这个备用分支。 --><template v-else
          ><!-- 显示Element Plus 的 form-item 组件；label 固定为 编码。 --><el-form-item label="编码"
            ><!-- 显示Element Plus 的 input 组件；与 form.code 双向同步；disabled 取自 form.builtin；maxlength 固定为 100。 --><el-input
              v-model="form.code"
              :disabled="form.builtin"
              maxlength="100" /></el-form-item
          ><!-- 显示Element Plus 的 form-item 组件；label 固定为 名称。 --><el-form-item label="名称"
            ><!-- 显示Element Plus 的 input 组件；与 form.name 双向同步；maxlength 固定为 64。 --><el-input v-model="form.name" maxlength="64" /></el-form-item
          ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；仅当 isMenu 成立时渲染。 --><template v-if="isMenu"
            ><!-- 显示Element Plus 的 form-item 组件；label 固定为 类型。 --><el-form-item label="类型"
              ><!-- 显示Element Plus 的 select 组件；与 form.menuType 双向同步。 --><el-select v-model="form.menuType"
                ><!-- 显示Element Plus 的 option 组件；按 t in ['DIRECTORY', 'PAGE', 'BUTTON'] 为每条数据重复生成内容；key 取自 t；value 取自 t；label 取自 { DIRECTORY: '目录', PAGE: '页面', BUTTON: '按钮' }[t]。 --><el-option
                  v-for="t in ['DIRECTORY', 'PAGE', 'BUTTON']"
                  :key="t"
                  :value="t"
                  :label="
                    { DIRECTORY: '目录', PAGE: '页面', BUTTON: '按钮' }[t]
                  " /></el-select></el-form-item
            ><!-- 显示Element Plus 的 form-item 组件；label 固定为 上级菜单。 --><el-form-item label="上级菜单"
              ><!-- 显示Element Plus 的 select 组件；与 form.parentId 双向同步；启用 clearable 属性；启用 filterable 属性。 --><el-select v-model="form.parentId" clearable filterable
                ><!-- 显示Element Plus 的 option 组件；按 m in parents(items).filter((m) => m.id !== form.id) 为每条数据重复生成内容；key 取自 m.id；label 取自 m.name；value 取自 m.id。 --><el-option
                  v-for="m in parents(items).filter((m) => m.id !== form.id)"
                  :key="m.id"
                  :label="m.name"
                  :value="m.id" /></el-select></el-form-item
            ><!-- 显示Element Plus 的 form-item 组件；仅当 form.menuType !== 'BUTTON' 成立时渲染；label 固定为 路由路径。 --><el-form-item v-if="form.menuType !== 'BUTTON'" label="路由路径"
              ><!-- 显示Element Plus 的 input 组件；与 form.routePath 双向同步。 --><el-input v-model="form.routePath" /></el-form-item
            ><!-- 显示Element Plus 的 form-item 组件；仅当 form.menuType === 'PAGE' 成立时渲染；label 固定为 页面组件。 --><el-form-item v-if="form.menuType === 'PAGE'" label="页面组件"
              ><!-- 显示Element Plus 的 select 组件；与 form.componentKey 双向同步。 --><el-select v-model="form.componentKey"
                ><!-- 显示Element Plus 的 option 组件；按 c in components 为每条数据重复生成内容；key 取自 c；value 取自 c；label 取自 c。 --><el-option
                  v-for="c in components"
                  :key="c"
                  :value="c"
                  :label="c" /></el-select></el-form-item
            ><!-- 显示Element Plus 的 form-item 组件；仅当 form.menuType !== 'DIRECTORY' 成立时渲染；label 固定为 关联权限。 --><el-form-item v-if="form.menuType !== 'DIRECTORY'" label="关联权限"
              ><!-- 显示Element Plus 的 select 组件；与 form.permissionId 双向同步；启用 filterable 属性。 --><el-select v-model="form.permissionId" filterable
                ><!-- 显示Element Plus 的 option 组件；按 p in permissions 为每条数据重复生成内容；key 取自 p.id；value 取自 p.id；label 取自 p.name + ' · ' + p.code。 --><el-option
                  v-for="p in permissions"
                  :key="p.id"
                  :value="p.id"
                  :label="p.name + ' · ' + p.code" /></el-select></el-form-item
            ><!-- 显示Element Plus 的 form-item 组件；label 固定为 排序。 --><el-form-item label="排序"
              ><!-- 显示Element Plus 的 input-number 组件；与 form.sortOrder 双向同步；min 取自 0。 --><el-input-number
                v-model="form.sortOrder"
                :min="0" /></el-form-item
            ><!-- 显示Element Plus 的 form-item 组件；label 固定为 图标标识。 --><el-form-item label="图标标识"
              ><!-- 显示Element Plus 的 input 组件；与 form.icon 双向同步；maxlength 固定为 64。 --><el-input v-model="form.icon" maxlength="64" /></el-form-item
            ><!-- 显示Element Plus 的 form-item 组件；label 固定为 显示菜单。 --><el-form-item label="显示菜单"
              ><!-- 显示Element Plus 的 switch 组件；与 form.visible 双向同步。 --><el-switch v-model="form.visible" /></el-form-item></template
          ><!-- 显示Element Plus 的 form-item 组件；前面条件均不成立时显示这个备用分支；label 固定为 说明。 --><el-form-item v-else label="说明"
            ><!-- 显示Element Plus 的 input 组件；与 form.description 双向同步；类型设为 textarea；maxlength 固定为 255。 --><el-input
              v-model="form.description"
              type="textarea"
              maxlength="255" /></el-form-item
          ><!-- 显示Element Plus 的 form-item 组件；label 固定为 启用。 --><el-form-item label="启用"
            ><!-- 显示Element Plus 的 switch 组件；与 form.enabled 双向同步。 --><el-switch
              v-model="form.enabled" /></el-form-item></template></template
      ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；前一条件不成立且 modal === 'password' 成立时渲染。 --><template v-else-if="modal === 'password'"
        ><!-- 显示Element Plus 的 alert 组件；title 固定为 重置后该用户的所有会话将退出，并要求下次登录修改密码；类型设为 warning；closable 取自 false。 --><el-alert
          title="重置后该用户的所有会话将退出，并要求下次登录修改密码"
          type="warning"
          :closable="false" /><!-- 显示Element Plus 的 form-item 组件；label 固定为 新密码。 --><el-form-item label="新密码"
          ><!-- 显示Element Plus 的 input 组件；与 form.newPassword 双向同步；类型设为 password；启用 show-password 属性。 --><el-input
            v-model="form.newPassword"
            type="password"
            show-password /></el-form-item
        ><!-- 显示Element Plus 的 form-item 组件；label 固定为 确认密码。 --><el-form-item label="确认密码"
          ><!-- 显示Element Plus 的 input 组件；与 form.confirmPassword 双向同步；类型设为 password；启用 show-password 属性。 --><el-input
            v-model="form.confirmPassword"
            type="password"
            show-password /></el-form-item></template
      ><!-- 显示Element Plus 的 checkbox-group 组件；前一条件不成立且 modal === 'roles' 成立时渲染；与 assigned 双向同步。 --><el-checkbox-group v-else-if="modal === 'roles'" v-model="assigned"
        ><!-- 显示Element Plus 的 checkbox 组件；按 r in roles 为每条数据重复生成内容；key 取自 r.id；value 取自 r.id；disabled 取自 r.code !== 'ADMIN'。 --><el-checkbox
          v-for="r in roles"
          :key="r.id"
          :value="r.id"
          :disabled="r.code !== 'ADMIN'"
          ><!-- 把 r.name 的当前值转成文字显示。 -->{{ r.name }}</el-checkbox
        ></el-checkbox-group
      >
      <!-- 显示一个布局容器；用样式类 dialog-actions 控制外观。 -->
      <div class="dialog-actions">
        <!-- 显示Element Plus 的 button 组件；click 事件触发时执行 modal = ''。 -->
        <el-button @click="modal = ''">取消</el-button
        ><!-- 显示Element Plus 的 button 组件；类型设为 primary；native-type 固定为 submit；loading 取自 submitting。 --><el-button type="primary" native-type="submit" :loading="submitting"
          >保存</el-button
        >
      </div></el-form
    ></el-dialog
  >
</template>
