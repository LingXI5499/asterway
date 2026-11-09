<!-- 渲染记录列表页面界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
/* 引入 vue 提供的组件、状态函数或请求方法。 */
import { ref, onMounted } from "vue";
/* 引入 vue-router 提供的组件、状态函数或请求方法。 */
import { useRouter } from "vue-router";
/* 引入 element-plus 提供的组件、状态函数或请求方法。 */
import { ElMessage } from "element-plus";
/* 引入 ../api/http 提供的组件、状态函数或请求方法。 */
import { api, clearSession, hasPermission } from "../api/http";
/* 引入 ../api/ui 提供的组件、状态函数或请求方法。 */
import { report, confirmAction, dateText } from "../api/ui";
/* 引入 ../components/DetailsDrawer.vue 提供的组件、状态函数或请求方法。 */
import DetailsDrawer from "../components/DetailsDrawer.vue";
const /* 父组件传入的数据（props）保存声明父组件可以传入的属性及其类型或默认值。 */ props = defineProps({ /* 种类（kind）字段使用可为空的文字（String）。 */ kind: String });
const /* HTTP 路由登记器（router）保存取得用于切换页面的路由器。 */ router = useRouter(),
  /* 数据列表（items）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  items = ref([]),
  /* 当前页（page）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  page = ref(1),
  /* 每页条数（size）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  size = ref(20),
  /* 总数（total）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  total = ref(0),
  /* 正在处理（busy）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  busy = ref(false),
  /* 错误（error）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  error = ref(""),
  /* 筛选（filter）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  filter = ref(""),
  /* 详情（detail）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  detail = ref(null),
  /* show保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  show = ref(false);
const /* logs保存判断列表或文字是否包含指定内容。 */ logs = props.kind.includes("logs"),
  /* 个人（personal）保存条件判断的结果：props.kind === "devices"。 */
  personal = props.kind === "devices";
const /* 路径（path）保存根据前面的条件，从两个候选结果中选择一个：personal ? "/me/sessions" : "/" + props.kind。 */ path = personal ? "/me/sessions" : "/" + props.kind;
/* 加载（load）：读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */
async function load() {
  /* 把真，表示条件成立或开关打开写入 busy.value。 */
  busy.value = true;
  /* 把固定文字 ''写入 error.value。 */
  error.value = "";
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    const /* q保存新建 地址搜索参数集合（URLSearchParams） 实例。 */ q = new URLSearchParams({ /* 当前页（page）字段使用表达式 page.value 的结果。 */ page: page.value, /* 每页条数（size）字段使用表达式 size.value 的结果。 */ size: size.value });
    /* 检查条件 logs && filter.value 是否成立，成立时走这一分支。 */
    if (logs && filter.value) /* 调用 q.set 完成设置处理。 */ q.set("userId", filter.value);
    const /* 结果（result）保存等待 根据前面的条件，从两个候选结果中选择一个：api(path + (personal ? "" : "?" + q)) 完成后取得结果。 */ result = await api(path + (personal ? "" : "?" + q));
    /* 把根据前面的条件，从两个候选结果中选择一个：personal ? result : result.items写入 items.value。 */
    items.value = personal ? result : result.items;
    /* 把优先使用前面的有效值，没有时采用后面的备用值：result.total || 0写入 total.value。 */
    total.value = result.total || 0;
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 把表达式 e.message 的结果写入 error.value。 */
    error.value = e.message;
  } finally {
    /* 把假，表示条件不成立或开关关闭写入 busy.value。 */
    busy.value = false;
  }
}
/* revoke：调用 revoke 完成revoke处理。 */
async function revoke(row, all = false) {
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    /* 等待确认 操作（Action）。 */
    await confirmAction(
      all ? "退出全部设备后需要重新登录，是否继续？" : "确定强制退出该会话？",
      all ? "退出全部设备" : "退出该会话",
    );
    /* 等待调用 api 完成接口处理。 */
    await api(path + (all ? "" : "/" + row.id), { /* 方法（method）字段使用固定状态或编码 DELETE（删除）。 */ method: "DELETE" });
    /* 调用 ElMessage.success 完成成功处理。 */
    ElMessage.success("会话已退出");
    /* 检查条件 all || row.current 是否成立，成立时走这一分支。 */
    if (all || row.current) {
      /* 调用 clearSession 完成清除会话处理。 */
      clearSession();
      /* 生成保留原对象其余字段的新副本，只替换明确指定的值。 */
      router.replace("/login");
    } else /* 读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */ load();
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 调用 report 完成复盘报告处理。 */
    report(e);
  }
}
/* 注册组件首次挂载完成后的处理。 */
onMounted(load);
</script>
<template>
  <!-- 显示一个有独立含义的内容区；用样式类 card 控制外观。 -->
  <section class="card">
    <!-- 显示一个布局容器；用样式类 toolbar 控制外观。 -->
    <div class="toolbar">
      <!-- 显示Element Plus 的 input 组件；仅当 logs 成立时渲染；与 filter 双向同步；未输入时提示 输入用户 ID 筛选；启用 clearable 属性；keyup 事件触发时执行 page = 1; load();。 -->
      <el-input
        v-if="logs"
        v-model="filter"
        placeholder="输入用户 ID 筛选"
        clearable
        @keyup.enter="
          page = 1;
          load();
        "
      /><!-- 显示Element Plus 的 button 组件；类型设为 primary；click 事件触发时执行 page = 1; load();。 --><el-button
        type="primary"
        @click="
          page = 1;
          load();
        "
        ><!-- 把 logs ? "查询" : "刷新列表" 的当前值转成文字显示。 -->{{ logs ? "查询" : "刷新列表" }}</el-button
      ><!-- 显示Element Plus 的 button 组件；仅当 logs 成立时渲染；click 事件触发时执行 filter = ''; page = 1; load();。 --><el-button
        v-if="logs"
        @click="
          filter = '';
          page = 1;
          load();
        "
        >重置</el-button
      ><!-- 显示Element Plus 的 button 组件；仅当 personal && hasPermission('identity:device:revoke') 成立时渲染；类型设为 danger；启用 plain 属性；click 事件触发时执行 revoke(null, true)。 --><el-button
        v-if="personal && hasPermission('identity:device:revoke')"
        type="danger"
        plain
        @click="revoke(null, true)"
        >退出全部设备</el-button
      >
    </div>
    <!-- 显示Element Plus 的 alert 组件；仅当 error 成立时渲染；title 取自 error；类型设为 error；closable 取自 false。 -->
    <el-alert
      v-if="error"
      :title="error"
      type="error"
      :closable="false"
    /><!-- 显示Element Plus 的 table 组件；data 取自 items；loading 指令使用 busy；empty-text 固定为 暂无记录。 --><el-table :data="items" v-loading="busy" empty-text="暂无记录"
      ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 empty 插槽。 --><template #empty
        ><!-- 显示应用空状态当前状态（AppEmptyState）组件；description 取自 error ? '加载失败，请查看提示后重试。' : '暂无记录'。 --><AppEmptyState
          :description="
            error ? '加载失败，请查看提示后重试。' : '暂无记录'
          " /></template
      ><!-- 显示Element Plus 的 table-column 组件；prop 固定为 username；label 固定为 用户；min-width 固定为 140；仅当 !personal 成立时渲染。 --><el-table-column
        prop="username"
        label="用户"
        min-width="140"
        v-if="!personal"
      /><!-- 显示Element Plus 的 table-column 组件；仅当 kind === 'operation-logs' 成立时渲染；prop 固定为 actorUsername；label 固定为 操作者；min-width 固定为 140。 --><el-table-column
        v-if="kind === 'operation-logs'"
        prop="actorUsername"
        label="操作者"
        min-width="140"
      /><!-- 显示Element Plus 的 table-column 组件；仅当 kind === 'operation-logs' 成立时渲染；prop 固定为 actionCode；label 固定为 操作；min-width 固定为 220。 --><el-table-column
        v-if="kind === 'operation-logs'"
        prop="actionCode"
        label="操作"
        min-width="220"
      /><!-- 显示Element Plus 的 table-column 组件；仅当 !logs 成立时渲染；prop 固定为 deviceLabel；label 固定为 设备；min-width 固定为 180。 --><el-table-column
        v-if="!logs"
        prop="deviceLabel"
        label="设备"
        min-width="180"
      /><!-- 显示Element Plus 的 table-column 组件；prop 固定为 ipAddress；label 固定为 IP 地址；min-width 固定为 140。 --><el-table-column
        prop="ipAddress"
        label="IP 地址"
        min-width="140"
      /><!-- 显示Element Plus 的 table-column 组件；仅当 logs 成立时渲染；prop 固定为 outcome；label 固定为 结果；min-width 固定为 110。 --><el-table-column v-if="logs" prop="outcome" label="结果" min-width="110"
        ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"
          ><!-- 显示应用状态标签（AppStatusTag）组件；value 取自 row.outcome。 --><AppStatusTag :value="row.outcome" /></template></el-table-column
      ><!-- 显示Element Plus 的 table-column 组件；仅当 logs 成立时渲染；prop 固定为 reasonCode；label 固定为 原因；min-width 固定为 160。 --><el-table-column
        v-if="logs"
        prop="reasonCode"
        label="原因"
        min-width="160"
      /><!-- 显示Element Plus 的 table-column 组件；label 取自 logs ? '发生时间' : '最近活动'；min-width 固定为 180。 --><el-table-column :label="logs ? '发生时间' : '最近活动'" min-width="180"
        ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"><!-- 把 dateText(row.occurredAt || row.lastActiveAt) 的当前值转成文字显示。 -->{{
          dateText(row.occurredAt || row.lastActiveAt)
        }}</template></el-table-column
      ><!-- 显示Element Plus 的 table-column 组件；仅当 personal 成立时渲染；label 固定为 当前设备。 --><el-table-column v-if="personal" label="当前设备"
        ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"
          ><!-- 显示Element Plus 的 tag 组件；仅当 row.current 成立时渲染；类型设为 success。 --><el-tag v-if="row.current" type="success">当前会话</el-tag></template
        ></el-table-column
      ><!-- 显示Element Plus 的 table-column 组件；label 固定为 操作；fixed 固定为 right；width 固定为 180。 --><el-table-column label="操作" fixed="right" width="180"
        ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"
          ><!-- 显示Element Plus 的 button 组件；启用 link 属性；类型设为 primary；click 事件触发时执行 detail = row; show = true;。 --><el-button
            link
            type="primary"
            @click="
              detail = row;
              show = true;
            "
            >详情</el-button
          ><!-- 显示Element Plus 的 button 组件；仅当 !logs && hasPermission( personal ? 'identity:device:revoke' : 'identity:session:revoke', ) 成立时渲染；启用 link 属性；类型设为 danger；click 事件触发时执行 revoke(row)。 --><el-button
            v-if="
              !logs &&
              hasPermission(
                personal ? 'identity:device:revoke' : 'identity:session:revoke',
              )
            "
            link
            type="danger"
            @click="revoke(row)"
            ><!-- 把 row.current ? "退出当前" : "强制退出" 的当前值转成文字显示。 -->{{ row.current ? "退出当前" : "强制退出" }}</el-button
          ></template
        ></el-table-column
      ></el-table
    ><!-- 显示Element Plus 的 pagination 组件；仅当 !personal 成立时渲染；用样式类 pagination 控制外观；与 page 双向同步（current-page）；与 size 双向同步（page-size）；total 取自 total；page-sizes 取自 [10, 20, 50, 100]；layout 固定为 total, sizes, prev, pager, next；change 事件触发时执行 load。 --><el-pagination
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
  <!-- 显示详情抽屉（DetailsDrawer）组件；与 show 双向同步；value 取自 detail。 -->
  <DetailsDrawer v-model="show" :value="detail" />
</template>
