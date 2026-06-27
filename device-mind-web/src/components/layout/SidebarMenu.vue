<template>
  <div class="sidebar">
    <div class="logo">
      <span v-if="!isCollapse">DeviceMind</span>
      <span v-else>DM</span>
    </div>
    <el-menu
      :default-active="route.path"
      :collapse="isCollapse"
      background-color="#304156"
      text-color="#bfcbd9"
      active-text-color="#409EFF"
      router
    >
      <template v-for="item in menuItems" :key="item.path">
        <el-menu-item v-if="!item.meta?.hidden" :index="item.path">
          <el-icon><component :is="item.meta?.icon" /></el-icon>
          <template #title>{{ item.meta?.title }}</template>
        </el-menu-item>
      </template>
    </el-menu>
  </div>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'

defineProps<{ isCollapse: boolean }>()
const route = useRoute()
const router = useRouter()

const rootRoute = router.options.routes.find(r => r.path === '/')
const menuItems = rootRoute?.children || []
</script>

<style scoped>
.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 20px;
  font-weight: bold;
  border-bottom: 1px solid rgba(255,255,255,0.1);
}
.el-menu {
  border-right: none;
}
</style>
