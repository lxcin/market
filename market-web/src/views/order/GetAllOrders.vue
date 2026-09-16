<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getOrders, getAllOrdersByAdmin, updateOrderStatusByAdmin } from '../../api/orders'

interface OrderItem {
  bookId: number
  bookTitle: string
  bookCover?: string
  quantity: number
  price: number
}

interface OrderVO {
  id: number
  orderNo: string
  totalAmount: number
  discountAmount: number
  payAmount: number
  status: number
  statusDesc: string
  items?: OrderItem[]
  receiverName?: string
  createdAt: string
}

const orders = ref<OrderVO[]>([])
const loading = ref(false)
const router = useRouter()
const searchKeyword = ref('')
const role = sessionStorage.getItem('role') || ''
const isAdmin = computed(() => role === 'ADMIN')

const filteredOrders = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  if (!keyword) return orders.value
  return orders.value.filter(o => o.orderNo.toLowerCase().includes(keyword))
})

onMounted(fetchOrders)

async function fetchOrders() {
  loading.value = true
  try {
    const res = isAdmin.value
      ? await getAllOrdersByAdmin({ page: 1, size: 50 })
      : await getOrders({ page: 1, size: 50 })
    const page = res.data?.data
    orders.value = page?.records || page || []
  } catch {
    ElMessage.error('获取订单失败')
  } finally {
    loading.value = false
  }
}

function viewOrderDetail(orderId: number) {
  router.push(`/orders/${orderId}`)
}

function goPay(orderId: number) {
  router.push(`/order/${orderId}/payment`)
}

async function ship(order: OrderVO) {
  await updateOrderStatusByAdmin(order.id, 2)
  ElMessage.success('已发货')
  fetchOrders()
}

function getStatusText(status: number): string {
  return ['待支付', '已支付', '已发货', '已完成', '已取消', '已退款'][status] || '未知'
}

function getStatusTagType(status: number): 'success' | 'danger' | 'info' | 'warning' | '' {
  return (['warning', 'success', 'primary', 'success', 'info', 'danger'] as const)[status] || 'info'
}
</script>

<template>
  <div class="order-list-container">
    <router-link to="/dashboard" v-slot="{navigate}">
      <el-button type="primary" @click="navigate">
        <span>返回个人主页</span>
      </el-button>
    </router-link>
    <el-card class="card-form">
      <h2 class="title">{{ isAdmin ? '全部订单' : '我的订单' }}</h2>

      <div class="search-bar">
        <el-input v-model="searchKeyword" placeholder="请输入订单编号" clearable style="width: 300px;" />
      </div>
      <el-table empty-text="暂无订单" :data="filteredOrders" class="order-list" v-loading="loading" stripe>
        <el-table-column prop="orderNo" label="订单编号" width="200" align="center" />
        <el-table-column prop="createdAt" label="下单时间" width="180" align="center" />
        <el-table-column v-if="isAdmin" prop="receiverName" label="收货人" width="120" align="center" />
        <el-table-column label="实付金额" width="120" align="center">
          <template #default="{ row }">¥{{ row.payAmount?.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="订单状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)" effect="dark" disable-transitions>
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="viewOrderDetail(row.id)">查看</el-button>
            <el-button v-if="row.status === 0 && !isAdmin" type="success" size="small" @click="goPay(row.id)">去支付</el-button>
            <el-button v-if="row.status === 1 && isAdmin" type="warning" size="small" @click="ship(row)">发货</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.order-list-container {
  padding: 24px;
  display: flex;
  gap: 16px;
  justify-content: center;
}
.title {
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 16px;
  text-align: center;
}
.search-bar {
  margin-bottom: 16px;
}
.card-form {
  width: 80%;
}
</style>
