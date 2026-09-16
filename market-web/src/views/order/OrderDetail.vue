<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrderDetail, getAdminOrderDetail, cancelOrder, updateOrderStatusByAdmin } from '../../api/orders'
import { refundOrder, getPayment } from '../../api/payment'

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
  items: OrderItem[]
  receiverName?: string
  receiverPhone?: string
  receiverAddress?: string
  couponName?: string
  createdAt: string
}

const route = useRoute()
const router = useRouter()
const order = ref<OrderVO | null>(null)
const role = sessionStorage.getItem('role') || ''
const isAdmin = computed(() => role === 'ADMIN')

const statusText = computed(() => order.value ? getStatusText(order.value.status) : '')

onMounted(async () => {
  const orderId = Number(route.params.orderId)
  try {
    const res = isAdmin.value
      ? await getAdminOrderDetail(orderId)
      : await getOrderDetail(orderId)
    order.value = res.data?.data
  } catch {
    ElMessage.error('请求失败')
  }
})

function getStatusText(status: number): string {
  return ['待支付', '已支付', '已发货', '已完成', '已取消', '已退款'][status] || '未知'
}

function getStatusTagType(status: number): 'success' | 'danger' | 'info' | 'warning' | '' {
  return (['warning', 'success', 'primary', 'success', 'info', 'danger'] as const)[status] || 'info'
}

async function handlePay() {
  if (!order.value) return
  router.push(`/order/${order.value.id}/payment`)
}

async function handleRefund() {
  if (!order.value) return
  const oid = order.value.id
  await ElMessageBox.confirm('确定申请退款？退款后库存将回补。', '提示', { type: 'warning' })
  await refundOrder(oid, '用户申请退款')
  ElMessage.info('退款已受理，等待网关异步通知...')
  const deadline = Date.now() + 12000
  let refunded = false
  while (Date.now() < deadline) {
    await new Promise(r => setTimeout(r, 800))
    try {
      const res = await getPayment(oid)
      if (res.data?.data?.status === 4) { refunded = true; break }
    } catch {}
  }
  await reload()
  if (refunded || order.value?.status === 5) {
    ElMessage.success('退款成功')
  } else {
    ElMessage.warning('退款处理中，请稍后在订单页查看')
  }
}

async function reload() {
  if (!order.value) return
  const res = isAdmin.value
    ? await getAdminOrderDetail(order.value.id)
    : await getOrderDetail(order.value.id)
  order.value = res.data?.data
}

async function handleCancel() {
  if (!order.value) return
  await cancelOrder(order.value.id)
  ElMessage.success('订单已取消')
  order.value.status = 4
  order.value.statusDesc = '已取消'
}

async function handleShip() {
  if (!order.value) return
  await updateOrderStatusByAdmin(order.value.id, 2)
  ElMessage.success('已发货')
  order.value.status = 2
  order.value.statusDesc = '已发货'
}
</script>

<template>
  <el-main v-if="order" class="order-detail-container">
    <el-card class="aside-card">
      <template #header>
        <h2 class="card-header">订单信息</h2>
      </template>
      <p><strong>订单编号：</strong>{{ order.orderNo }}</p>
      <p><strong>下单时间：</strong>{{ order.createdAt }}</p>
      <p><strong>订单状态：</strong>
        <el-tag :type="getStatusTagType(order.status)">{{ statusText }}</el-tag>
      </p>
      <p v-if="order.receiverName"><strong>收货人：</strong>{{ order.receiverName }}</p>
      <p v-if="order.receiverPhone"><strong>手机号：</strong>{{ order.receiverPhone }}</p>
      <p v-if="order.receiverAddress"><strong>收货地址：</strong>{{ order.receiverAddress }}</p>
      <p v-if="order.couponName"><strong>优惠券：</strong>{{ order.couponName }}</p>
      <p><strong>商品总额：</strong>¥{{ order.totalAmount?.toFixed(2) }}</p>
      <p v-if="order.discountAmount"><strong>优惠金额：</strong>-¥{{ order.discountAmount?.toFixed(2) }}</p>
      <p><strong>实付金额：</strong>¥{{ order.payAmount?.toFixed(2) }}</p>

      <div v-if="order.status === 0 && !isAdmin" class="actions">
        <el-button type="success" @click="handlePay">立即支付</el-button>
        <el-button @click="handleCancel">取消订单</el-button>
      </div>
      <div v-if="order.status === 1 && isAdmin" class="actions">
        <el-button type="warning" @click="handleShip">发货</el-button>
      </div>
      <div v-if="order.status === 1 && !isAdmin" class="actions">
        <el-button type="danger" plain @click="handleRefund">申请退款</el-button>
      </div>
    </el-card>

    <el-card class="card-form">
      <template #header>
        <h2 class="card-header">购买商品</h2>
      </template>
      <el-row v-for="item in order.items" :key="item.bookId" class="product-row">
        <img :src="item.bookCover" class="cover" />
        <div class="info">
          <p><strong>{{ item.bookTitle }}</strong></p>
          <p>单价：¥{{ item.price?.toFixed(2) }}</p>
          <p>数量：{{ item.quantity }}</p>
          <p>小计：¥{{ (item.price * item.quantity).toFixed(2) }}</p>
        </div>
      </el-row>
    </el-card>
  </el-main>
</template>

<style scoped>
.order-detail-container {
  display: flex;
  flex-direction: row;
  padding: 15px;
  gap: 20px;
  justify-content: center;
}
.card-form {
  width: 60%;
}
.aside-card {
  width: 30%;
  align-self: flex-start;
}
.product-row {
  display: flex;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}
.cover {
  width: 100px;
  height: 120px;
  object-fit: cover;
  margin-right: 16px;
  background: #f0f2f5;
}
.info p {
  margin: 6px 0;
}
.actions {
  margin-top: 16px;
  display: flex;
  gap: 12px;
}
.card-header {
  text-align: center;
}
</style>
