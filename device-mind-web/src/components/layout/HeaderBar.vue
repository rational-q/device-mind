<template>
  <div class="header-bar">
    <div class="left">
      <el-icon class="toggle-btn" @click="$emit('toggle')">
        <Fold v-if="!isCollapse" />
        <Expand v-else />
      </el-icon>
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item v-if="route.meta?.title">{{ route.meta?.title }}</el-breadcrumb-item>
      </el-breadcrumb>
    </div>
    <div class="right">
      <el-tag type="info" effect="plain">admin</el-tag>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'

defineProps<{ isCollapse: boolean }>()
defineEmits(['toggle'])
const route = useRoute()
</script>

<style scoped>
.header-bar {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.left {
  display: flex;
  align-items: center;
  gap: 16px;
}
.toggle-btn {
  font-size: 20px;
  cursor: pointer;
}
</style>
