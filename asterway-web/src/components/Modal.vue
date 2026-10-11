<script setup lang="ts">
import { onMounted, ref, onBeforeUnmount } from 'vue';
import Icon from './Icon.vue';
defineProps<{ title: string; wide?: boolean }>();
const emit = defineEmits<{ close: [] }>();
const dialog = ref<HTMLDialogElement>();
let previous: HTMLElement | null = null;
onMounted(() => {
  previous = document.activeElement as HTMLElement;
  dialog.value?.showModal();
});
onBeforeUnmount(() => previous?.focus());
</script>
<template>
  <dialog
    ref="dialog"
    :class="['modal', { wide }]"
    aria-labelledby="modal-title"
    @cancel.prevent="emit('close')"
    @click="
      (e) => {
        if (e.target === dialog) emit('close');
      }
    "
  >
    <header class="modal-header">
      <h2 id="modal-title">{{ title }}</h2>
      <button class="icon-button" aria-label="关闭弹窗" @click="emit('close')">
        <Icon name="close" />
      </button>
    </header>
    <div class="modal-body"><slot /></div>
  </dialog>
</template>
