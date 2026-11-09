<!-- 能力说明直接来自服务端，上传新文档与新版本共用。 -->
<script setup>
defineProps({ capabilities: Object, error: String });
</script>
<template>
  <div class="knowledge-format-help" role="note">
    <template v-if="capabilities">
      <p><strong>支持格式：</strong>{{ capabilities.formats.map(f => f.name + '（' + f.extension + '）').join('、') }}</p>
      <p>单文件最多 {{ (capabilities.maxFileBytes / 1024 / 1024).toFixed(0) }} MB，每批最多 {{ capabilities.maxBatchFiles }} 个文件。</p>
      <details><summary>查看解析范围</summary><ul><li v-for="format in capabilities.formats" :key="format.extension">{{ format.name }}：{{ format.description }}</li></ul></details>
      <p>{{ capabilities.scanNotice }}</p>
    </template>
    <p v-else>{{ error || '正在读取支持格式与大小限制…' }}</p>
  </div>
</template>
<style scoped>
.knowledge-format-help { padding:16px; border:1px solid var(--aw-border); border-radius:8px; background:var(--aw-surface-muted); color:var(--aw-text-secondary); font-size:14px; line-height:20px; }
p { margin:0 0 8px; } p:last-child { margin-bottom:0; } summary { cursor:pointer; color:var(--aw-text); } ul { padding-left:24px; margin:8px 0; } details { margin:8px 0; }
</style>
