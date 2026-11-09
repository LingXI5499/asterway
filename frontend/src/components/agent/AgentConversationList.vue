<!-- 渲染成长助手对话列表界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
/* 引入 vue 提供的组件、状态函数或请求方法。 */
import { computed } from "vue";

const /* 父组件传入的数据（props）保存声明父组件可以传入的属性及其类型或默认值。 */ props = defineProps({
  /* 记录列表（rows）字段使用表达式 { type: Array, default: () => [] } 的结果。 */
  rows: { /* 类型（type）字段使用Array。 */ type: Array, /* 默认值（default）字段使用表达式 () => [] 的结果。 */ default: () => [] },
  /* 当前有效编号（activeId）字段使用表达式 { type: String, default: "" } 的结果。 */
  activeId: { /* 类型（type）字段使用可为空的文字（String）。 */ type: String, /* 默认值（default）字段使用固定文字 ''。 */ default: "" },
  /* 正在处理（busy）字段使用可为空的真假值（Boolean）。 */
  busy: Boolean,
  /* 错误（error）字段使用表达式 { type: String, default: "" } 的结果。 */
  error: { /* 类型（type）字段使用可为空的文字（String）。 */ type: String, /* 默认值（default）字段使用固定文字 ''。 */ default: "" },
  /* 已加载（loaded）字段使用可为空的真假值（Boolean）。 */
  loaded: Boolean,
  /* 是否存在More（hasMore）字段使用可为空的真假值（Boolean）。 */
  hasMore: Boolean,
  /* 总数（total）字段使用表达式 { type: Number, default: 0 } 的结果。 */
  total: { /* 类型（type）字段使用数值（Number）。 */ type: Number, /* 默认值（default）字段使用固定数值 0。 */ default: 0 },
  /* keyword字段使用表达式 { type: String, default: "" } 的结果。 */
  keyword: { /* 类型（type）字段使用可为空的文字（String）。 */ type: String, /* 默认值（default）字段使用固定文字 ''。 */ default: "" },
  /* 是否可以创建（canCreate）字段使用可为空的真假值（Boolean）。 */
  canCreate: Boolean,
  /* 是否可以更新（canUpdate）字段使用可为空的真假值（Boolean）。 */
  canUpdate: Boolean,
  /* 是否可以删除（canDelete）字段使用可为空的真假值（Boolean）。 */
  canDelete: Boolean,
});
const /* 向父组件发送事件的函数（emit）保存声明当前组件可以通知父组件的事件。 */ emit = defineEmits([
  "select",
  "create",
  "refresh",
  "load-more",
  "rename",
  "remove",
  "update:keyword",
]);

const /* keyword值（keywordValue）保存随相关状态变化自动重算的值。 */ keywordValue = computed({
  /* 读取（get）字段使用表达式 () => props.keyword 的结果。 */
  get: () => props.keyword,
  /* 设置（set）字段使用表达式 (value) => emit("update:keyword", value) 的结果。 */
  set: (value) => emit("update:keyword", value),
});
const /* 是否存在Actions（hasActions）保存优先使用前面的有效值，没有时采用后面的备用值：() => props.canUpdate || props.canDelete。 */ hasActions = () => props.canUpdate || props.canDelete;

/* 方案运行（run）：调用 run 完成方案运行处理。 */
function run(command, row) {
  /* 向父组件发送事件，通知数据或界面状态发生变化。 */
  emit(command, row);
}
/* 时间文字（timeText）：调用 timeText 完成时间文字处理。 */
function timeText(value) {
  /* 检查条件 !value 是否成立，成立时走这一分支。 */
  if (!value) /* 返回固定文字 '尚无消息'，当前函数到此结束。 */ return "尚无消息";
  const /* 时间与耗时工具库（time）保存调用 Date.parse 完成解析处理。 */ time = Date.parse(value);
  /* 检查条件 !Number.isFinite(time) 是否成立，成立时走这一分支。 */
  if (!Number.isFinite(time)) /* 返回固定文字 '—'，当前函数到此结束。 */ return "—";
  const /* diff保存当前真实时间，用于记录操作发生的时刻。 */ diff = Date.now() - time;
  /* 检查条件 diff < 60000 是否成立，成立时走这一分支。 */
  if (diff < 60000) /* 返回固定文字 '刚刚'，当前函数到此结束。 */ return "刚刚";
  /* 检查条件 diff < 3600000 是否成立，成立时走这一分支。 */
  if (diff < 3600000) /* 返回调用 `${Math.floor 完成floor处理，当前函数到此结束。 */ return `${Math.floor(diff / 60000)} 分钟前`;
  /* 检查条件 diff < 86400000 是否成立，成立时走这一分支。 */
  if (diff < 86400000) /* 返回调用 `${Math.floor 完成floor处理，当前函数到此结束。 */ return `${Math.floor(diff / 3600000)} 小时前`;
  /* 返回新建 日期（Date） 实例，当前函数到此结束。 */
  return new Date(time).toLocaleDateString("zh-CN", {
    /* 月（month）字段使用固定文字 '2-digit'。 */
    month: "2-digit",
    /* 日（day）字段使用固定文字 '2-digit'。 */
    day: "2-digit",
  });
}
</script>

<template>
  <!-- 显示一个布局容器；用样式类 agent-list 控制外观。 -->
  <div class="agent-list">
    <!-- 显示一个布局容器；用样式类 agent-list-head 控制外观。 -->
    <div class="agent-list-head">
      <!-- 显示一个布局容器；用样式类 agent-list-heading 控制外观。 -->
      <div class="agent-list-heading">
        <!-- 显示重点文字。 -->
        <strong>最近对话</strong>
        <!-- 显示行内文字区域；用样式类 muted 控制外观。 -->
        <span class="muted"><!-- 把 total 的当前值转成文字显示。 -->{{ total }}</span>
        <!-- 显示一个布局容器；用样式类 agent-list-heading-actions 控制外观。 -->
        <div class="agent-list-heading-actions">
          <!-- 显示Element Plus 的 button 组件；启用 text 属性；size 固定为 small；loading 取自 busy；aria-label 提供无障碍说明 刷新会话列表；click 事件触发时执行 $emit('refresh')。 -->
          <el-button
            text
            size="small"
            :loading="busy"
            aria-label="刷新会话列表"
            @click="$emit('refresh')"
            ><!-- 显示应用Icon（AppIcon）组件；name 固定为 refresh；size 取自 14。 --><AppIcon name="refresh" :size="14" /></el-button
          >
        </div>
      </div>
      <!-- 显示Element Plus 的 input 组件；与 keywordValue 双向同步；size 固定为 small；启用 clearable 属性；maxlength 固定为 100；未输入时提示 搜索对话；aria-label 提供无障碍说明 搜索会话标题；keyup 事件触发时执行 $emit('refresh')；clear 事件触发时执行 $emit('refresh')。 -->
      <el-input
        v-model="keywordValue"
        size="small"
        clearable
        maxlength="100"
        placeholder="搜索对话"
        aria-label="搜索会话标题"
        @keyup.enter="$emit('refresh')"
        @clear="$emit('refresh')"
        ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 prefix 插槽。 --><template #prefix><!-- 显示应用Icon（AppIcon）组件；name 固定为 search；size 取自 14。 --><AppIcon name="search" :size="14" /></template
      ></el-input>
    </div>

    <!-- 显示一个布局容器；用样式类 agent-list-scroll 控制外观。 -->
    <div class="agent-list-scroll">
      <!-- 显示Element Plus 的 skeleton 组件；仅当 busy && !loaded 成立时渲染；rows 取自 5；启用 animated 属性。 -->
      <el-skeleton v-if="busy && !loaded" :rows="5" animated />
      <!-- 显示只组织渲染规则、不额外生成普通容器的模板；前面条件均不成立时显示这个备用分支。 -->
      <template v-else>
        <!-- 显示一个布局容器；仅当 error 成立时渲染；用样式类 agent-list-error 控制外观；role 固定为 alert。 -->
        <div v-if="error" class="agent-list-error" role="alert">
          <!-- 显示一段文字。 -->
          <p><!-- 把 error 的当前值转成文字显示。 -->{{ error }}</p>
          <!-- 显示Element Plus 的 button 组件；启用 text 属性；类型设为 primary；size 固定为 small；click 事件触发时执行 $emit('refresh')。 -->
          <el-button text type="primary" size="small" @click="$emit('refresh')"
            >重新加载</el-button
          >
        </div>
        <!-- 显示一个布局容器；用样式类 agent-list-empty 控制外观；仅当 !rows.length && !error 成立时渲染。 -->
        <div class="agent-list-empty"
          v-if="!rows.length && !error"
        ><!-- 显示应用Icon（AppIcon）组件；name 固定为 chat；size 取自 24。 --><AppIcon name="chat" :size="24" /><!-- 显示一段文字。 --><p>还没有对话</p><!-- 显示行内文字区域。 --><span>从一个小目标开始吧</span></div>
        <!-- 显示一个布局容器；前一条件不成立且 rows.length 成立时渲染；用样式类 agent-list-rows 控制外观。 -->
        <div v-else-if="rows.length" class="agent-list-rows">
          <!-- 显示一个布局容器；按 row in rows 为每条数据重复生成内容；key 取自 row.id；用样式类 agent-list-row 控制外观；class 取自 { active: row.id === activeId }。 -->
          <div
            v-for="row in rows"
            :key="row.id"
            class="agent-list-row"
            :class="{ active: row.id === activeId }"
          >
            <!-- 显示可点击按钮；类型设为 button；用样式类 agent-list-main 控制外观；title 取自 row.title + ' · ' + timeText(row.lastMessageAt)；aria-current 取自 row.id === activeId ? 'true' : undefined；click 事件触发时执行 $emit('select', row.id)。 -->
            <button
              type="button"
              class="agent-list-main"
              :title="row.title + ' · ' + timeText(row.lastMessageAt)"
              :aria-current="row.id === activeId ? 'true' : undefined"
              @click="$emit('select', row.id)"
            >
              <!-- 显示行内文字区域；用样式类 agent-list-title 控制外观。 -->
              <span class="agent-list-title"><!-- 把 row.title 的当前值转成文字显示。 -->{{ row.title }}</span>
            </button>
            <!-- 显示Element Plus 的 dropdown 组件；仅当 hasActions() 成立时渲染；trigger 固定为 click；placement 固定为 bottom-end；command 事件触发时执行 (command) => run(command, row)。 -->
            <el-dropdown
              v-if="hasActions()"
              trigger="click"
              placement="bottom-end"
              @command="(command) => run(command, row)"
            >
              <!-- 显示可点击按钮；类型设为 button；用样式类 agent-list-more 控制外观；aria-label 取自 '管理会话：' + row.title。 -->
              <button
                type="button"
                class="agent-list-more"
                :aria-label="'管理会话：' + row.title"
              >
                <!-- 显示应用Icon（AppIcon）组件；name 固定为 more；size 取自 16。 -->
                <AppIcon name="more" :size="16" />
              </button>
              <!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 dropdown 插槽。 -->
              <template #dropdown>
                <!-- 显示Element Plus 的 dropdown-menu 组件。 -->
                <el-dropdown-menu>
                  <!-- 显示Element Plus 的 dropdown-item 组件；仅当 canUpdate 成立时渲染；command 固定为 rename。 -->
                  <el-dropdown-item v-if="canUpdate" command="rename"
                    ><!-- 显示行内文字区域；用样式类 agent-menu-item 控制外观。 --><span class="agent-menu-item"
                      ><!-- 显示应用Icon（AppIcon）组件；name 固定为 edit；size 取自 14。 --><AppIcon name="edit" :size="14" />重命名</span
                    ></el-dropdown-item
                  >
                  <!-- 显示Element Plus 的 dropdown-item 组件；仅当 canDelete 成立时渲染；command 固定为 remove；启用 divided 属性。 -->
                  <el-dropdown-item v-if="canDelete" command="remove" divided
                    ><!-- 显示行内文字区域；用样式类 agent-menu-item 控制外观。 --><span class="agent-menu-item"
                      ><!-- 显示应用Icon（AppIcon）组件；name 固定为 delete；size 取自 14。 --><AppIcon name="delete" :size="14" />删除会话</span
                    ></el-dropdown-item
                  >
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </template>
    </div>

    <!-- 显示一个布局容器；仅当 hasMore 成立时渲染；用样式类 agent-list-foot 控制外观。 -->
    <div v-if="hasMore" class="agent-list-foot">
      <!-- 显示Element Plus 的 button 组件；仅当 hasMore 成立时渲染；启用 text 属性；size 固定为 small；loading 取自 busy；click 事件触发时执行 $emit('load-more')。 -->
      <el-button
        v-if="hasMore"
        text
        size="small"
        :loading="busy"
        @click="$emit('load-more')"
        >加载更多会话</el-button
      >
    </div>
  </div>
</template>

<style scoped>
/* 以下规则应用于 .agent-list 选中的元素。 */
.agent-list { /* 决定元素采用普通块、行内、Flex 或网格布局；取值 flex。 */ display: flex; /* 设置 Flex 条目的排列方向；取值 column。 */ flex-direction: column; /* 限制最小宽度；取值 0。 */ min-width: 0; }
/* 以下规则应用于 .agent-list-head 选中的元素。 */
.agent-list-head { /* 决定元素采用普通块、行内、Flex 或网格布局；取值 grid。 */ display: grid; /* 设置相邻布局条目之间的间隔；取值 12px。 */ gap: 12px; /* 设置内容与边框之间的内侧留白；取值 0 8px 12px。 */ padding: 0 8px 12px; }
/* 以下规则应用于 .agent-list-heading 选中的元素。 */
.agent-list-heading { /* 决定元素采用普通块、行内、Flex 或网格布局；取值 flex。 */ display: flex; /* 设置 Flex 或网格条目在交叉方向上的对齐；取值 center。 */ align-items: center; /* 设置相邻布局条目之间的间隔；取值 8px。 */ gap: 8px; }
/* 以下规则应用于 .agent-list-heading strong 选中的元素。 */
.agent-list-heading strong { /* 设置文字或继承该颜色的图形颜色；取值 var(—aw-text-secondary)。 */ color: var(--aw-text-secondary); /* 设置字号；取值 12px。 */ font-size: 12px; /* 设置每行文字的高度；取值 16px。 */ line-height: 16px; /* 设置字重；取值 500。 */ font-weight: 500; }
/* 以下规则应用于 .agent-list-heading .muted 选中的元素。 */
.agent-list-heading .muted { /* 设置字号；取值 12px。 */ font-size: 12px; /* 设置每行文字的高度；取值 16px。 */ line-height: 16px; }
/* 以下规则应用于 .agent-list-heading-actions 选中的元素。 */
.agent-list-heading-actions { /* 设置左侧外侧留白；取值 auto。 */ margin-left: auto; }
/* 以下规则应用于 .agent-list-heading-actions :deep(.el-button) 选中的元素。 */
.agent-list-heading-actions :deep(.el-button) { /* 设置内容与边框之间的内侧留白；取值 0。 */ padding: 0; /* 设置宽度；取值 24px。 */ width: 24px; /* 限制最小高度；取值 24px。 */ min-height: 24px; }
/* 以下规则应用于 .agent-list-head :deep(.el-input__wrapper) 选中的元素。 */
.agent-list-head :deep(.el-input__wrapper) { /* 设置背景颜色、图片或渐变；取值 transparent。 */ background: transparent; /* 设置元素投影；取值 0 0 0 1px var(—aw-border) inset。 */ box-shadow: 0 0 0 1px var(--aw-border) inset; /* 设置内容与边框之间的内侧留白；取值 0 8px。 */ padding: 0 8px; }
/* 以下规则应用于 .agent-list-head :deep(.el-input__inner) 选中的元素。 */
.agent-list-head :deep(.el-input__inner) { /* 设置字号；取值 12px。 */ font-size: 12px; /* 设置每行文字的高度；取值 16px。 */ line-height: 16px; }
/* 以下规则应用于 .agent-list-scroll 选中的元素。 */
.agent-list-scroll { /* 限制最小宽度；取值 0。 */ min-width: 0; }
/* 以下规则应用于 .agent-list-rows 选中的元素。 */
.agent-list-rows { /* 决定元素采用普通块、行内、Flex 或网格布局；取值 grid。 */ display: grid; /* 设置相邻布局条目之间的间隔；取值 2px。 */ gap: 2px; /* 限制最小宽度；取值 0。 */ min-width: 0; }
/* 以下规则应用于 .agent-list-row 选中的元素。 */
.agent-list-row { /* 决定元素是否参与普通排版，以及按谁定位；取值 relative。 */ position: relative; /* 限制最小宽度；取值 0。 */ min-width: 0; /* 设置边角圆弧大小；取值 8px。 */ border-radius: 8px; }
/* 以下规则应用于 .agent-list-row:hover, .agent-list-row:focus-within 选中的元素。 */
.agent-list-row:hover, .agent-list-row:focus-within { /* 设置背景颜色、图片或渐变；取值 color-mix(in srgb, var(—aw-text) 5%, transparent)。 */ background: color-mix(in srgb, var(--aw-text) 5%, transparent); }
/* 以下规则应用于 .agent-list-row.active 选中的元素。 */
.agent-list-row.active { /* 设置背景颜色、图片或渐变；取值 color-mix(in srgb, var(—aw-text) 8%, transparent)。 */ background: color-mix(in srgb, var(--aw-text) 8%, transparent); }
/* 以下规则应用于 .agent-list-main 选中的元素。 */
.agent-list-main { /* 决定元素采用普通块、行内、Flex 或网格布局；取值 block。 */ display: block; /* 设置宽度；取值 100%。 */ width: 100%; /* 设置内容与边框之间的内侧留白；取值 8px 32px 8px 12px。 */ padding: 8px 32px 8px 12px; /* 设置边框宽度、线型与颜色；取值 0。 */ border: 0; /* 设置边角圆弧大小；取值 8px。 */ border-radius: 8px; /* 设置背景颜色、图片或渐变；取值 transparent。 */ background: transparent; /* 设置文字或继承该颜色的图形颜色；取值 var(—aw-text)。 */ color: var(--aw-text); /* 设置 CSS 属性 font；取值 inherit。 */ font: inherit; /* 设置文字横向对齐方式；取值 left。 */ text-align: left; /* 设置鼠标移上来时的指针形状；取值 pointer。 */ cursor: pointer; }
/* 以下规则应用于 .agent-list-main:focus-visible 选中的元素。 */
.agent-list-main:focus-visible { /* 设置不占布局尺寸的轮廓线；取值 2px solid var(—aw-brand)。 */ outline: 2px solid var(--aw-brand); /* 设置轮廓线与边界的距离；取值 -2px。 */ outline-offset: -2px; }
/* 以下规则应用于 .agent-list-title 选中的元素。 */
.agent-list-title { /* 决定元素采用普通块、行内、Flex 或网格布局；取值 block。 */ display: block; /* 决定内容超出边界时显示、裁切还是滚动；取值 hidden。 */ overflow: hidden; /* 决定文字被截断时是否显示省略号；取值 ellipsis。 */ text-overflow: ellipsis; /* 决定空白和换行如何保留；取值 nowrap。 */ white-space: nowrap; /* 设置字号；取值 14px。 */ font-size: 14px; /* 设置每行文字的高度；取值 20px。 */ line-height: 20px; }
/* 以下规则应用于 .agent-list-row :deep(.el-dropdown) 选中的元素。 */
.agent-list-row :deep(.el-dropdown) { /* 决定元素是否参与普通排版，以及按谁定位；取值 absolute。 */ position: absolute; /* 设置上侧定位距离；取值 50%。 */ top: 50%; /* 设置右侧定位距离；取值 4px。 */ right: 4px; /* 对显示位置、角度或缩放做变换；取值 translateY(-50%)。 */ transform: translateY(-50%); /* 设置宽度；取值 24px。 */ width: 24px; /* 设置高度；取值 24px。 */ height: 24px; }
/* 以下规则应用于 .agent-list-more 选中的元素。 */
.agent-list-more { /* 决定元素采用普通块、行内、Flex 或网格布局；取值 grid。 */ display: grid; /* 设置 CSS 属性 place-items；取值 center。 */ place-items: center; /* 设置宽度；取值 24px。 */ width: 24px; /* 设置高度；取值 24px。 */ height: 24px; /* 设置内容与边框之间的内侧留白；取值 0。 */ padding: 0; /* 设置边框宽度、线型与颜色；取值 0。 */ border: 0; /* 设置边角圆弧大小；取值 4px。 */ border-radius: 4px; /* 设置背景颜色、图片或渐变；取值 transparent。 */ background: transparent; /* 设置文字或继承该颜色的图形颜色；取值 var(—aw-text-secondary)。 */ color: var(--aw-text-secondary); /* 设置鼠标移上来时的指针形状；取值 pointer。 */ cursor: pointer; /* 设置整体透明度；取值 0。 */ opacity: 0; }
/* 以下规则应用于 .agent-list-row:hover .agent-list-more, .agent-list-row:focus-within .agent-list-more, .agent-list-row.active .agent-list-more 选中的元素。 */
.agent-list-row:hover .agent-list-more, .agent-list-row:focus-within .agent-list-more, .agent-list-row.active .agent-list-more { /* 设置整体透明度；取值 1。 */ opacity: 1; }
/* 以下规则应用于 .agent-list-more:hover 选中的元素。 */
.agent-list-more:hover { /* 设置背景颜色、图片或渐变；取值 var(—aw-border)。 */ background: var(--aw-border); }
/* 以下规则应用于 .agent-list-error 选中的元素。 */
.agent-list-error { /* 设置内容与边框之间的内侧留白；取值 16px 8px。 */ padding: 16px 8px; /* 设置文字横向对齐方式；取值 center。 */ text-align: center; /* 设置字号；取值 12px。 */ font-size: 12px; /* 设置每行文字的高度；取值 16px。 */ line-height: 16px; /* 设置文字或继承该颜色的图形颜色；取值 var(—aw-danger)。 */ color: var(--aw-danger); }
/* 以下规则应用于 .agent-list-empty 选中的元素。 */
.agent-list-empty { /* 设置内容与边框之间的内侧留白；取值 24px 12px。 */ padding: 24px 12px; /* 设置文字或继承该颜色的图形颜色；取值 var(—aw-text-tertiary)。 */ color: var(--aw-text-tertiary); /* 设置文字横向对齐方式；取值 center。 */ text-align: center; }
/* 以下规则应用于 .agent-list-empty p 选中的元素。 */
.agent-list-empty p { /* 设置元素外侧留白；取值 8px 0。 */ margin: 8px 0; /* 设置字号；取值 14px。 */ font-size: 14px; /* 设置每行文字的高度；取值 20px。 */ line-height: 20px; }
/* 以下规则应用于 .agent-list-empty span 选中的元素。 */
.agent-list-empty span { /* 设置字号；取值 12px。 */ font-size: 12px; /* 设置每行文字的高度；取值 16px。 */ line-height: 16px; }
/* 以下规则应用于 .agent-list-foot 选中的元素。 */
.agent-list-foot { /* 设置内容与边框之间的内侧留白；取值 8px。 */ padding: 8px; /* 设置文字横向对齐方式；取值 center。 */ text-align: center; }
/* 以下规则应用于 .agent-menu-item 选中的元素。 */
.agent-menu-item { /* 决定元素采用普通块、行内、Flex 或网格布局；取值 inline-flex。 */ display: inline-flex; /* 设置 Flex 或网格条目在交叉方向上的对齐；取值 center。 */ align-items: center; /* 设置相邻布局条目之间的间隔；取值 8px。 */ gap: 8px; }
/* 仅在指定屏幕或设备条件成立时使用这一组样式：(hover: none)。 */
@media (hover: none) { /* 以下规则应用于 .agent-list-more 选中的元素。 */ .agent-list-more { /* 设置整体透明度；取值 1。 */ opacity: 1; } }
</style>
