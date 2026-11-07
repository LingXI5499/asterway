<script setup>
import { computed } from "vue";
import MarkdownIt from "markdown-it";

// 只渲染受控 Markdown：禁止原始 HTML，链接统一新窗口打开，表格允许横向滚动。
const markdown = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true,
  typographer: false,
});
markdown.renderer.rules.link_open = (tokens, index, options, env, self) => {
  tokens[index].attrSet("target", "_blank");
  tokens[index].attrSet("rel", "noopener noreferrer nofollow");
  return self.renderToken(tokens, index, options);
};
markdown.renderer.rules.table_open = () => '<div class="md-table"><table>\n';
markdown.renderer.rules.table_close = () => "</table></div>\n";
markdown.renderer.rules.image = (tokens, index, options, env, self) => {
  const token = tokens[index];
  token.attrSet(
    "alt",
    self.renderInlineAsText(token.children, options, env),
  );
  token.attrSet("loading", "lazy");
  token.attrSet("decoding", "async");
  token.attrSet("referrerpolicy", "no-referrer");
  return self.renderToken(tokens, index, options);
};

const props = defineProps({
  content: { type: String, default: "" },
});
const html = computed(() =>
  props.content ? markdown.render(props.content) : "",
);
</script>

<template>
  <div class="agent-markdown" v-html="html"></div>
</template>

<style>
/* 内容由 v-html 生成，样式统一挂在独占类名下，不依赖 scoped 属性。 */
.agent-markdown {
  font-size: 16px;
  line-height: 24px;
  overflow-wrap: anywhere;
  text-wrap: pretty;
}
.agent-markdown > :first-child {
  margin-top: 0;
}
.agent-markdown > :last-child {
  margin-bottom: 0;
}
.agent-markdown h1,
.agent-markdown h2,
.agent-markdown h3,
.agent-markdown h4,
.agent-markdown h5,
.agent-markdown h6 {
  margin: 24px 0 8px;
  font-weight: 600;
  line-height: 1.4;
  letter-spacing: -0.01em;
}
.agent-markdown h1 {
  font-size: 20px;
  line-height: 28px;
}
.agent-markdown h2 {
  font-size: 18px;
  line-height: 28px;
}
.agent-markdown h3 {
  font-size: 16px;
  line-height: 24px;
}
.agent-markdown h4,
.agent-markdown h5,
.agent-markdown h6 {
  font-size: 14px;
  line-height: 20px;
}
.agent-markdown p {
  margin: 0 0 12px;
}
.agent-markdown ul,
.agent-markdown ol {
  margin: 0 0 12px;
  padding-left: 24px;
}
.agent-markdown li + li {
  margin-top: 4px;
}
.agent-markdown li > p {
  margin: 4px 0;
}
.agent-markdown li > ul,
.agent-markdown li > ol {
  margin: 4px 0;
}
.agent-markdown strong {
  font-weight: 600;
}
.agent-markdown em {
  font-style: normal;
}
.agent-markdown del {
  color: var(--aw-text-secondary);
}
.agent-markdown a {
  color: var(--el-color-primary);
  text-decoration: none;
}
.agent-markdown a:hover {
  text-decoration: underline;
}
.agent-markdown code {
  padding: 2px 4px;
  border-radius: 4px;
  background: var(--el-fill-color);
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 0.9em;
}
.agent-markdown pre {
  margin: 0 0 12px;
  padding: 12px 16px;
  border-radius: 8px;
  background: var(--el-fill-color-light);
  overflow-x: auto;
}
.agent-markdown pre code {
  padding: 0;
  background: transparent;
  font-size: 14px;
  line-height: 20px;
}
.agent-markdown blockquote {
  margin: 0 0 12px;
  padding: 4px 0 4px 12px;
  border-left: 3px solid var(--el-border-color);
  color: var(--aw-text-secondary);
}
.agent-markdown blockquote > :last-child {
  margin-bottom: 0;
}
.agent-markdown hr {
  margin: 16px 0;
  border: 0;
  border-top: 1px solid var(--el-border-color-lighter);
}
.agent-markdown .md-table {
  margin: 0 0 12px;
  overflow-x: auto;
}
.agent-markdown .md-table table {
  border-collapse: collapse;
  width: 100%;
  font-size: 14px;
  line-height: 20px;
}
.agent-markdown .md-table th,
.agent-markdown .md-table td {
  border: 1px solid var(--el-border-color-light);
  padding: 8px 12px;
  text-align: left;
  vertical-align: top;
}
.agent-markdown .md-table th {
  background: var(--el-fill-color-light);
  font-weight: 600;
}
.agent-markdown img {
  max-width: 100%;
  border-radius: 8px;
}
</style>
