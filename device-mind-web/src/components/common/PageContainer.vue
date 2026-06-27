<template>
  <div class="page-container">
    <div class="page-header" v-if="title">
      <h2>{{ title }}</h2>
      <slot name="actions" />
    </div>
    <slot />
  </div>
</template>

<script setup lang="ts">
defineProps<{ title?: string }>()
</script>

<style scoped>
.page-container {
  background: #fff;
  border-radius: 4px;
  padding: 20px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #ebeef5;
}
.page-header h2 {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}
</style>
