<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import {
  DataLine, Reading, Menu, Box, List, Ticket
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const activeMenu = computed(() => route.path)
const username = sessionStorage.getItem('username') || 'admin'

function logout() {
  ElMessageBox.confirm('确定退出登录？', '提示', { type: 'warning' }).then(() => {
    sessionStorage.clear()
    router.push('/login')
  })
}
</script>

<template>
  <el-container class="admin-layout">
    <el-aside width="210px" class="admin-aside">
      <div class="admin-logo">书城管理后台</div>
      <el-menu :default-active="activeMenu" router class="admin-menu">
        <el-menu-item index="/admin/dashboard"><el-icon><DataLine /></el-icon>数据看板</el-menu-item>
        <el-menu-item index="/admin/books"><el-icon><Reading /></el-icon>图书管理</el-menu-item>
        <el-menu-item index="/admin/categories"><el-icon><Menu /></el-icon>分类管理</el-menu-item>
        <el-menu-item index="/admin/inventory"><el-icon><Box /></el-icon>库存管理</el-menu-item>
        <el-menu-item index="/admin/orders"><el-icon><List /></el-icon>订单管理</el-menu-item>
        <el-menu-item index="/admin/coupons"><el-icon><Ticket /></el-icon>优惠券管理</el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="admin-header">
        <div class="admin-title">在线书城 · 管理端</div>
        <div class="admin-user">
          <el-button link type="primary" @click="router.push('/recommendation')">返回前台</el-button>
          <span>{{ username }}</span>
          <el-button link type="danger" @click="logout">退出</el-button>
        </div>
      </el-header>
      <el-main class="admin-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.admin-layout { height: 100vh; }
.admin-aside { background: #1f2d3d; color: #fff; }
.admin-logo { height: 60px; line-height: 60px; text-align: center; font-size: 16px; font-weight: 600; color: #fff; background: #17222e; }
.admin-menu { border-right: none; background: transparent; }
.admin-menu :deep(.el-menu-item) { color: #c0c4cc; }
.admin-menu :deep(.el-menu-item.is-active) { color: #fff; background: #263445; }
.admin-menu :deep(.el-menu-item:hover) { background: #263445; }
.admin-header { display: flex; align-items: center; justify-content: space-between; background: #fff; border-bottom: 1px solid #ebeef5; }
.admin-title { font-size: 16px; font-weight: 600; }
.admin-user { display: flex; align-items: center; gap: 14px; color: #606266; }
.admin-main { background: #f5f7fa; padding: 20px; }
</style>
