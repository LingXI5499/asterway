<script setup>
defineProps({ state: { type: Object, required: true } });
defineEmits(["change"]);
</script>
<template>
  <el-pagination
    class="pagination"
    v-model:current-page="state.page"
    v-model:page-size="state.size"
    :total="state.total"
    :page-sizes="[10, 20, 50, 100]"
    layout="total,sizes,prev,pager,next"
    @change="$emit('change')"
  />
</template>
