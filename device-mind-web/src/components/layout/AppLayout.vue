<template>
  <el-container class="app-layout">
    <el-aside :width="isCollapse ? '64px' : '220px'" class="app-aside">
      <SidebarMenu :is-collapse="isCollapse" />
    </el-aside>
    <el-container>
      <el-header class="app-header">
        <HeaderBar :is-collapse="isCollapse" @toggle="isCollapse = !isCollapse" />
      </el-header>
      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import SidebarMenu from './SidebarMenu.vue'
import HeaderBar from './HeaderBar.vue'

const isCollapse = ref(false)
</script>

<style scoped>
.app-layout {
  height: 100vh;
}
.app-aside {
  background-color: #304156;
  transition: width 0.3s;
  overflow: hidden;
}
.app-header {
  background: #fff;
  border-bottom: 1px solid #e6e6e6;
  display: flex;
  align-items: center;
  padding: 0 20px;
  height: 60px;
}
.app-main {
  background: #f0f2f5;
  min-height: calc(100vh - 60px);
  padding: 20px;
}
</style>
