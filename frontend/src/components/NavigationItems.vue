<script setup>
defineProps({ nodes: Array });
</script>
<template>
  <template v-for="node in nodes" :key="node.id"
    ><el-sub-menu v-if="node.children?.length" :index="node.id"
      ><template #title
        ><span>{{ node.name }}</span></template
      ><NavigationItems :nodes="node.children" /></el-sub-menu
    ><el-menu-item v-else-if="node.menuType === 'PAGE'" :index="node.routePath"
      ><template #title>{{ node.name }}</template></el-menu-item
    ></template
  >
</template>
