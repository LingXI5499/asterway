<!-- 渲染成长助手Markdown界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
/* 引入 vue 提供的组件、状态函数或请求方法。 */
import { computed } from "vue";
/* 引入 markdown-it 提供的组件、状态函数或请求方法。 */
import MarkdownIt from "markdown-it";

// 只渲染受控 Markdown：禁止原始 HTML，链接统一新窗口打开，表格允许横向滚动。
const /* markdown保存新建 MarkdownIt 实例。 */ markdown = new MarkdownIt({
  /* html字段使用假，表示条件不成立或开关关闭。 */
  html: false,
  /* linkify字段使用真，表示条件成立或开关打开。 */
  linkify: true,
  /* breaks字段使用真，表示条件成立或开关打开。 */
  breaks: true,
  /* typographer字段使用假，表示条件不成立或开关关闭。 */
  typographer: false,
});
/* 把调用 (tokens, index, options, env, self) => { tokens[index].attrSet 完成attr设置处理写入 markdown.renderer.rules.link_open。 */
markdown.renderer.rules.link_open = (tokens, index, options, env, self) => {
  /* 调用 tokens[index].attrSet 完成attr设置处理。 */
  tokens[index].attrSet("target", "_blank");
  /* 调用 tokens[index].attrSet 完成attr设置处理。 */
  tokens[index].attrSet("rel", "noopener noreferrer nofollow");
  /* 返回渲染 身份凭据（Token），当前函数到此结束。 */
  return self.renderToken(tokens, index, options);
};
/* 把表达式 () => '<div class="md-table"><table>\n' 的结果写入 markdown.renderer.rules.table_open。 */
markdown.renderer.rules.table_open = () => '<div class="md-table"><table>\n';
/* 把按表达式计算得到的值：() => "</table></div>\n"写入 markdown.renderer.rules.table_close。 */
markdown.renderer.rules.table_close = () => "</table></div>\n";
/* 把调用 (tokens, index, options, env, self) => { const token = tokens[index]; token.attrSet 完成attr设置处理写入 markdown.renderer.rules.image。 */
markdown.renderer.rules.image = (tokens, index, options, env, self) => {
  const /* 身份凭据（token）保存表达式 tokens[index] 的结果。 */ token = tokens[index];
  /* 调用 token.attrSet 完成attr设置处理。 */
  token.attrSet(
    "alt",
    self.renderInlineAsText(token.children, options, env),
  );
  /* 调用 token.attrSet 完成attr设置处理。 */
  token.attrSet("loading", "lazy");
  /* 调用 token.attrSet 完成attr设置处理。 */
  token.attrSet("decoding", "async");
  /* 调用 token.attrSet 完成attr设置处理。 */
  token.attrSet("referrerpolicy", "no-referrer");
  /* 返回渲染 身份凭据（Token），当前函数到此结束。 */
  return self.renderToken(tokens, index, options);
};

const /* 父组件传入的数据（props）保存声明父组件可以传入的属性及其类型或默认值。 */ props = defineProps({
  /* 内容（content）字段使用表达式 { type: String, default: "" } 的结果。 */
  content: { /* 类型（type）字段使用可为空的文字（String）。 */ type: String, /* 默认值（default）字段使用固定文字 ''。 */ default: "" },
});
const /* html保存随相关状态变化自动重算的值。 */ html = computed(() =>
  props.content ? markdown.render(props.content) : "",
);
</script>

<template>
  <!-- 显示一个布局容器；用样式类 agent-markdown 控制外观；显示 html 提供的已处理 HTML 内容。 -->
  <div class="agent-markdown" v-html="html"></div>
</template>

<style>
/* 内容由 v-html 生成，样式统一挂在独占类名下，不依赖 scoped 属性。 */
/* 以下规则应用于 .agent-markdown 选中的元素。 */
.agent-markdown {
  /* 设置字号；取值 16px。 */
  font-size: 16px;
  /* 设置每行文字的高度；取值 24px。 */
  line-height: 24px;
  /* 设置 CSS 属性 overflow-wrap；取值 anywhere。 */
  overflow-wrap: anywhere;
  /* 设置 CSS 属性 text-wrap；取值 pretty。 */
  text-wrap: pretty;
}
/* 以下规则应用于 .agent-markdown > :first-child 选中的元素。 */
.agent-markdown > :first-child {
  /* 设置上方外侧留白；取值 0。 */
  margin-top: 0;
}
/* 以下规则应用于 .agent-markdown > :last-child 选中的元素。 */
.agent-markdown > :last-child {
  /* 设置下方外侧留白；取值 0。 */
  margin-bottom: 0;
}
/* 以下规则应用于 .agent-markdown h1, .agent-markdown h2, .agent-markdown h3, .agent-markdown h4, .agent-markdown h5, .agent-markdown h6 选中的元素。 */
.agent-markdown h1,
.agent-markdown h2,
.agent-markdown h3,
.agent-markdown h4,
.agent-markdown h5,
.agent-markdown h6 {
  /* 设置元素外侧留白；取值 24px 0 8px。 */
  margin: 24px 0 8px;
  /* 设置字重；取值 600。 */
  font-weight: 600;
  /* 设置每行文字的高度；取值 1.4。 */
  line-height: 1.4;
  /* 设置字符之间的间距；取值 -0.01em。 */
  letter-spacing: -0.01em;
}
/* 以下规则应用于 .agent-markdown h1 选中的元素。 */
.agent-markdown h1 {
  /* 设置字号；取值 20px。 */
  font-size: 20px;
  /* 设置每行文字的高度；取值 28px。 */
  line-height: 28px;
}
/* 以下规则应用于 .agent-markdown h2 选中的元素。 */
.agent-markdown h2 {
  /* 设置字号；取值 18px。 */
  font-size: 18px;
  /* 设置每行文字的高度；取值 28px。 */
  line-height: 28px;
}
/* 以下规则应用于 .agent-markdown h3 选中的元素。 */
.agent-markdown h3 {
  /* 设置字号；取值 16px。 */
  font-size: 16px;
  /* 设置每行文字的高度；取值 24px。 */
  line-height: 24px;
}
/* 以下规则应用于 .agent-markdown h4, .agent-markdown h5, .agent-markdown h6 选中的元素。 */
.agent-markdown h4,
.agent-markdown h5,
.agent-markdown h6 {
  /* 设置字号；取值 14px。 */
  font-size: 14px;
  /* 设置每行文字的高度；取值 20px。 */
  line-height: 20px;
}
/* 以下规则应用于 .agent-markdown p 选中的元素。 */
.agent-markdown p {
  /* 设置元素外侧留白；取值 0 0 12px。 */
  margin: 0 0 12px;
}
/* 以下规则应用于 .agent-markdown ul, .agent-markdown ol 选中的元素。 */
.agent-markdown ul,
.agent-markdown ol {
  /* 设置元素外侧留白；取值 0 0 12px。 */
  margin: 0 0 12px;
  /* 设置左侧内侧留白；取值 24px。 */
  padding-left: 24px;
}
/* 以下规则应用于 .agent-markdown li + li 选中的元素。 */
.agent-markdown li + li {
  /* 设置上方外侧留白；取值 4px。 */
  margin-top: 4px;
}
/* 以下规则应用于 .agent-markdown li > p 选中的元素。 */
.agent-markdown li > p {
  /* 设置元素外侧留白；取值 4px 0。 */
  margin: 4px 0;
}
/* 以下规则应用于 .agent-markdown li > ul, .agent-markdown li > ol 选中的元素。 */
.agent-markdown li > ul,
.agent-markdown li > ol {
  /* 设置元素外侧留白；取值 4px 0。 */
  margin: 4px 0;
}
/* 以下规则应用于 .agent-markdown strong 选中的元素。 */
.agent-markdown strong {
  /* 设置字重；取值 600。 */
  font-weight: 600;
}
/* 以下规则应用于 .agent-markdown em 选中的元素。 */
.agent-markdown em {
  /* 设置 CSS 属性 font-style；取值 normal。 */
  font-style: normal;
}
/* 以下规则应用于 .agent-markdown del 选中的元素。 */
.agent-markdown del {
  /* 设置文字或继承该颜色的图形颜色；取值 var(—aw-text-secondary)。 */
  color: var(--aw-text-secondary);
}
/* 以下规则应用于 .agent-markdown a 选中的元素。 */
.agent-markdown a {
  /* 设置文字或继承该颜色的图形颜色；取值 var(—el-color-primary)。 */
  color: var(--el-color-primary);
  /* 设置下划线等文字装饰；取值 none。 */
  text-decoration: none;
}
/* 以下规则应用于 .agent-markdown a:hover 选中的元素。 */
.agent-markdown a:hover {
  /* 设置下划线等文字装饰；取值 underline。 */
  text-decoration: underline;
}
/* 以下规则应用于 .agent-markdown code 选中的元素。 */
.agent-markdown code {
  /* 设置内容与边框之间的内侧留白；取值 2px 4px。 */
  padding: 2px 4px;
  /* 设置边角圆弧大小；取值 4px。 */
  border-radius: 4px;
  /* 设置背景颜色、图片或渐变；取值 var(—el-fill-color)。 */
  background: var(--el-fill-color);
  /* 设置字体选择顺序；取值 ui-monospace, SFMono-Regular, Menlo, Consolas, monospace。 */
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  /* 设置字号；取值 0.9em。 */
  font-size: 0.9em;
}
/* 以下规则应用于 .agent-markdown pre 选中的元素。 */
.agent-markdown pre {
  /* 设置元素外侧留白；取值 0 0 12px。 */
  margin: 0 0 12px;
  /* 设置内容与边框之间的内侧留白；取值 12px 16px。 */
  padding: 12px 16px;
  /* 设置边角圆弧大小；取值 8px。 */
  border-radius: 8px;
  /* 设置背景颜色、图片或渐变；取值 var(—el-fill-color-light)。 */
  background: var(--el-fill-color-light);
  /* 决定横向超出时的处理；取值 auto。 */
  overflow-x: auto;
}
/* 以下规则应用于 .agent-markdown pre code 选中的元素。 */
.agent-markdown pre code {
  /* 设置内容与边框之间的内侧留白；取值 0。 */
  padding: 0;
  /* 设置背景颜色、图片或渐变；取值 transparent。 */
  background: transparent;
  /* 设置字号；取值 14px。 */
  font-size: 14px;
  /* 设置每行文字的高度；取值 20px。 */
  line-height: 20px;
}
/* 以下规则应用于 .agent-markdown blockquote 选中的元素。 */
.agent-markdown blockquote {
  /* 设置元素外侧留白；取值 0 0 12px。 */
  margin: 0 0 12px;
  /* 设置内容与边框之间的内侧留白；取值 4px 0 4px 12px。 */
  padding: 4px 0 4px 12px;
  /* 设置左侧边框；取值 3px solid var(—el-border-color)。 */
  border-left: 3px solid var(--el-border-color);
  /* 设置文字或继承该颜色的图形颜色；取值 var(—aw-text-secondary)。 */
  color: var(--aw-text-secondary);
}
/* 以下规则应用于 .agent-markdown blockquote > :last-child 选中的元素。 */
.agent-markdown blockquote > :last-child {
  /* 设置下方外侧留白；取值 0。 */
  margin-bottom: 0;
}
/* 以下规则应用于 .agent-markdown hr 选中的元素。 */
.agent-markdown hr {
  /* 设置元素外侧留白；取值 16px 0。 */
  margin: 16px 0;
  /* 设置边框宽度、线型与颜色；取值 0。 */
  border: 0;
  /* 设置顶部边框；取值 1px solid var(—el-border-color-lighter)。 */
  border-top: 1px solid var(--el-border-color-lighter);
}
/* 以下规则应用于 .agent-markdown .md-table 选中的元素。 */
.agent-markdown .md-table {
  /* 设置元素外侧留白；取值 0 0 12px。 */
  margin: 0 0 12px;
  /* 决定横向超出时的处理；取值 auto。 */
  overflow-x: auto;
}
/* 以下规则应用于 .agent-markdown .md-table table 选中的元素。 */
.agent-markdown .md-table table {
  /* 设置 CSS 属性 border-collapse；取值 collapse。 */
  border-collapse: collapse;
  /* 设置宽度；取值 100%。 */
  width: 100%;
  /* 设置字号；取值 14px。 */
  font-size: 14px;
  /* 设置每行文字的高度；取值 20px。 */
  line-height: 20px;
}
/* 以下规则应用于 .agent-markdown .md-table th, .agent-markdown .md-table td 选中的元素。 */
.agent-markdown .md-table th,
.agent-markdown .md-table td {
  /* 设置边框宽度、线型与颜色；取值 1px solid var(—el-border-color-light)。 */
  border: 1px solid var(--el-border-color-light);
  /* 设置内容与边框之间的内侧留白；取值 8px 12px。 */
  padding: 8px 12px;
  /* 设置文字横向对齐方式；取值 left。 */
  text-align: left;
  /* 设置 CSS 属性 vertical-align；取值 top。 */
  vertical-align: top;
}
/* 以下规则应用于 .agent-markdown .md-table th 选中的元素。 */
.agent-markdown .md-table th {
  /* 设置背景颜色、图片或渐变；取值 var(—el-fill-color-light)。 */
  background: var(--el-fill-color-light);
  /* 设置字重；取值 600。 */
  font-weight: 600;
}
/* 以下规则应用于 .agent-markdown img 选中的元素。 */
.agent-markdown img {
  /* 限制最大宽度；取值 100%。 */
  max-width: 100%;
  /* 设置边角圆弧大小；取值 8px。 */
  border-radius: 8px;
}
</style>
