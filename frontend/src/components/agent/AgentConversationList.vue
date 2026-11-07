<script setup>
import { computed } from "vue";

const props = defineProps({
  rows: { type: Array, default: () => [] },
  activeId: { type: String, default: "" },
  busy: Boolean,
  error: { type: String, default: "" },
  loaded: Boolean,
  hasMore: Boolean,
  total: { type: Number, default: 0 },
  keyword: { type: String, default: "" },
  canCreate: Boolean,
  canUpdate: Boolean,
  canDelete: Boolean,
});
const emit = defineEmits([
  "select",
  "create",
  "refresh",
  "load-more",
  "rename",
  "remove",
  "update:keyword",
]);

const keywordValue = computed({
  get: () => props.keyword,
  set: (value) => emit("update:keyword", value),
});
const hasActions = () => props.canUpdate || props.canDelete;

function run(command, row) {
  emit(command, row);
}
function timeText(value) {
  if (!value) return "尚无消息";
  const time = Date.parse(value);
  if (!Number.isFinite(time)) return "—";
  const diff = Date.now() - time;
  if (diff < 60000) return "刚刚";
  if (diff < 3600000) return `${Math.floor(diff / 60000)} 分钟前`;
  if (diff < 86400000) return `${Math.floor(diff / 3600000)} 小时前`;
  return new Date(time).toLocaleDateString("zh-CN", {
    month: "2-digit",
    day: "2-digit",
  });
}
</script>

<template>
  <div class="agent-list">
    <div class="agent-list-head">
      <div class="agent-list-heading">
        <strong>最近对话</strong>
        <span class="muted">{{ total }}</span>
        <div class="agent-list-heading-actions">
          <el-button
            text
            size="small"
            :loading="busy"
            aria-label="刷新会话列表"
            @click="$emit('refresh')"
            ><AppIcon name="refresh" :size="14" /></el-button
          >
        </div>
      </div>
      <el-input
        v-model="keywordValue"
        size="small"
        clearable
        maxlength="100"
        placeholder="搜索对话"
        aria-label="搜索会话标题"
        @keyup.enter="$emit('refresh')"
        @clear="$emit('refresh')"
        ><template #prefix><AppIcon name="search" :size="14" /></template
      ></el-input>
    </div>

    <div class="agent-list-scroll">
      <el-skeleton v-if="busy && !loaded" :rows="5" animated />
      <template v-else>
        <div v-if="error" class="agent-list-error" role="alert">
          <p>{{ error }}</p>
          <el-button text type="primary" size="small" @click="$emit('refresh')"
            >重新加载</el-button
          >
        </div>
        <div class="agent-list-empty"
          v-if="!rows.length && !error"
        ><AppIcon name="chat" :size="24" /><p>还没有对话</p><span>从一个小目标开始吧</span></div>
        <div v-else-if="rows.length" class="agent-list-rows">
          <div
            v-for="row in rows"
            :key="row.id"
            class="agent-list-row"
            :class="{ active: row.id === activeId }"
          >
            <button
              type="button"
              class="agent-list-main"
              :title="row.title + ' · ' + timeText(row.lastMessageAt)"
              :aria-current="row.id === activeId ? 'true' : undefined"
              @click="$emit('select', row.id)"
            >
              <span class="agent-list-title">{{ row.title }}</span>
            </button>
            <el-dropdown
              v-if="hasActions()"
              trigger="click"
              placement="bottom-end"
              @command="(command) => run(command, row)"
            >
              <button
                type="button"
                class="agent-list-more"
                :aria-label="'管理会话：' + row.title"
              >
                <AppIcon name="more" :size="16" />
              </button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-if="canUpdate" command="rename"
                    ><span class="agent-menu-item"
                      ><AppIcon name="edit" :size="14" />重命名</span
                    ></el-dropdown-item
                  >
                  <el-dropdown-item v-if="canDelete" command="remove" divided
                    ><span class="agent-menu-item"
                      ><AppIcon name="delete" :size="14" />删除会话</span
                    ></el-dropdown-item
                  >
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </template>
    </div>

    <div v-if="hasMore" class="agent-list-foot">
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
.agent-list { display: flex; flex-direction: column; min-width: 0; }
.agent-list-head { display: grid; gap: 12px; padding: 0 8px 12px; }
.agent-list-heading { display: flex; align-items: center; gap: 8px; }
.agent-list-heading strong { color: var(--aw-text-secondary); font-size: 12px; line-height: 16px; font-weight: 500; }
.agent-list-heading .muted { font-size: 12px; line-height: 16px; }
.agent-list-heading-actions { margin-left: auto; }
.agent-list-heading-actions :deep(.el-button) { padding: 0; width: 24px; min-height: 24px; }
.agent-list-head :deep(.el-input__wrapper) { background: transparent; box-shadow: 0 0 0 1px var(--aw-border) inset; padding: 0 8px; }
.agent-list-head :deep(.el-input__inner) { font-size: 12px; line-height: 16px; }
.agent-list-scroll { min-width: 0; }
.agent-list-rows { display: grid; gap: 2px; min-width: 0; }
.agent-list-row { position: relative; min-width: 0; border-radius: 8px; }
.agent-list-row:hover, .agent-list-row:focus-within { background: color-mix(in srgb, var(--aw-text) 5%, transparent); }
.agent-list-row.active { background: color-mix(in srgb, var(--aw-text) 8%, transparent); }
.agent-list-main { display: block; width: 100%; padding: 8px 32px 8px 12px; border: 0; border-radius: 8px; background: transparent; color: var(--aw-text); font: inherit; text-align: left; cursor: pointer; }
.agent-list-main:focus-visible { outline: 2px solid var(--aw-brand); outline-offset: -2px; }
.agent-list-title { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 14px; line-height: 20px; }
.agent-list-row :deep(.el-dropdown) { position: absolute; top: 50%; right: 4px; transform: translateY(-50%); width: 24px; height: 24px; }
.agent-list-more { display: grid; place-items: center; width: 24px; height: 24px; padding: 0; border: 0; border-radius: 4px; background: transparent; color: var(--aw-text-secondary); cursor: pointer; opacity: 0; }
.agent-list-row:hover .agent-list-more, .agent-list-row:focus-within .agent-list-more, .agent-list-row.active .agent-list-more { opacity: 1; }
.agent-list-more:hover { background: var(--aw-border); }
.agent-list-error { padding: 16px 8px; text-align: center; font-size: 12px; line-height: 16px; color: var(--aw-danger); }
.agent-list-empty { padding: 24px 12px; color: var(--aw-text-tertiary); text-align: center; }
.agent-list-empty p { margin: 8px 0; font-size: 14px; line-height: 20px; }
.agent-list-empty span { font-size: 12px; line-height: 16px; }
.agent-list-foot { padding: 8px; text-align: center; }
.agent-menu-item { display: inline-flex; align-items: center; gap: 8px; }
@media (hover: none) { .agent-list-more { opacity: 1; } }
</style>
