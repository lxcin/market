<script setup lang="ts">
import { router } from '../../router'

const username = sessionStorage.getItem('username') || ''
const role = sessionStorage.getItem('role') || ''

if (!username) {
  router.push('/login')
}
</script>

<template>
  <div class="dashboard">
    <el-card class="profile-card">
      <div class="profile-header">
        <el-avatar :size="80" icon="UserFilled" />
        <div class="profile-info">
          <h2>{{ username }}</h2>
          <el-tag v-if="role === 'ADMIN'" type="danger">管理员</el-tag>
          <el-tag v-else>用户</el-tag>
        </div>
      </div>
    </el-card>

    <div class="menu-grid">
      <el-card class="menu-card" @click="router.push('/orders')">
        <el-icon :size="32"><Document /></el-icon>
        <h3>我的订单</h3>
        <p>查看订单状态</p>
      </el-card>
      <el-card class="menu-card" @click="router.push('/cart')">
        <el-icon :size="32"><ShoppingCart /></el-icon>
        <h3>购物车</h3>
        <p>管理购物车商品</p>
      </el-card>
      <el-card class="menu-card" @click="router.push('/addresses')">
        <el-icon :size="32"><Location /></el-icon>
        <h3>收货地址</h3>
        <p>管理收货地址</p>
      </el-card>
      <el-card class="menu-card" @click="router.push('/coupon/allcoupons')">
        <el-icon :size="32"><Ticket /></el-icon>
        <h3>优惠券</h3>
        <p>查看我的优惠券</p>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.dashboard { max-width: 800px; margin: 30px auto; padding: 0 20px; }
.profile-card { margin-bottom: 30px; }
.profile-header { display: flex; align-items: center; gap: 20px; }
.profile-info h2 { margin: 0 0 8px; }
.menu-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; }
.menu-card { cursor: pointer; text-align: center; padding: 20px; transition: transform 0.2s; }
.menu-card:hover { transform: translateY(-2px); }
.menu-card h3 { margin: 10px 0 5px; }
.menu-card p { margin: 0; font-size: 13px; color: #999; }
</style>
