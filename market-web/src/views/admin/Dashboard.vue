<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getDashboardStats } from '../../api/admin'

const stats = ref<Record<string, any>>({})
const loading = ref(false)

const cards = [
  { key: 'totalUsers', label: '用户总数', color: '#409eff', icon: '👤' },
  { key: 'totalBooks', label: '图书总数', color: '#67c23a', icon: '📚' },
  { key: 'totalOrders', label: '订单总数', color: '#e6a23c', icon: '🧾' },
  { key: 'todayOrders', label: '今日订单', color: '#f56c6c', icon: '📈' },
  { key: 'totalRevenue', label: '总营收(元)', color: '#909399', icon: '💰' }
]

onMounted(async () => {
  loading.value = true
  try {
    const res = await getDashboardStats()
    stats.value = res.data?.data || {}
  } finally { loading.value = false }
})
</script>

<template>
  <div v-loading="loading">
    <h2>数据看板</h2>
    <div class="stat-grid">
      <el-card v-for="c in cards" :key="c.key" class="stat-card" shadow="hover">
        <div class="stat-icon" :style="{ background: c.color }">{{ c.icon }}</div>
        <div class="stat-info">
          <div class="stat-value">
            {{ c.key === 'totalRevenue'
              ? ('¥' + Number(stats[c.key] || 0).toFixed(2))
              : (stats[c.key] ?? 0) }}
          </div>
          <div class="stat-label">{{ c.label }}</div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
h2 { margin-top: 0; }
.stat-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 16px; }
.stat-card { display: flex; align-items: center; }
.stat-card :deep(.el-card__body) { display: flex; align-items: center; gap: 16px; width: 100%; }
.stat-icon { width: 56px; height: 56px; border-radius: 10px; display: flex; align-items: center; justify-content: center; font-size: 26px; color: #fff; }
.stat-value { font-size: 24px; font-weight: 700; }
.stat-label { color: #909399; font-size: 13px; margin-top: 4px; }
</style>
