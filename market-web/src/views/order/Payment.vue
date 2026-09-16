<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { router } from '../../router'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createPayment, getPayment, sandboxPay } from '../../api/payment'

const route = useRoute()
const orderId = Number(route.params.orderId)
const paymentNo = ref('')
const amount = ref(0)
const channel = ref('MOCK')
const orderNo = ref('')
const loading = ref(true)
const paying = ref(false)

const statusText: Record<number, string> = {
  0: '待支付', 1: '支付成功', 2: '支付失败', 3: '已关闭', 4: '已退款'
}

const currentStatus = ref<number>(0)

onMounted(async () => {
  if (!orderId) {
    ElMessage.warning('无效订单')
    await router.push('/cart')
    return
  }
  try {
    const res = await getPayment(orderId)
    const d = res.data?.data || {}
    if (d.status === 1) {
      await router.replace(`/order/${orderId}/result`)
      return
    }
    if (d.paymentNo && d.status === 0) {
      applyPayment(d)
    } else {
      const created = await createPayment(orderId)
      applyPayment(created.data?.data || {})
    }
  } catch {
    ElMessage.error('订单加载失败')
    await router.push('/cart')
  } finally {
    loading.value = false
  }
})

function applyPayment(d: any) {
  paymentNo.value = d.paymentNo || ''
  amount.value = Number(d.amount || 0)
  channel.value = d.channel || 'MOCK'
  orderNo.value = d.orderNo || ''
  currentStatus.value = d.status ?? 0
}

async function waitForStatus(target: number, timeoutMs = 12000): Promise<boolean> {
  const deadline = Date.now() + timeoutMs
  while (Date.now() < deadline) {
    await new Promise(r => setTimeout(r, 800))
    try {
      const res = await getPayment(orderId)
      const d = res.data?.data || {}
      currentStatus.value = d.status ?? currentStatus.value
      if (currentStatus.value === target) return true
    } catch {}
  }
  return false
}

async function goPay() {
  if (!paymentNo.value) return
  paying.value = true
  try {
    // 沙箱支付：网关受理后立即返回，随后异步回调平台
    await sandboxPay(paymentNo.value)
    ElMessage.info('支付请求已提交，等待网关异步通知...')
    const ok = await waitForStatus(1)
    if (ok) {
      ElMessage.success('支付成功')
      await router.replace(`/order/${orderId}/result`)
    } else {
      ElMessage.warning('支付处理中，请稍后在订单页查看结果')
    }
  } catch {
    ElMessage.error('支付失败，请稍后再试')
  } finally {
    paying.value = false
  }
}
</script>

<template>
  <el-card class="payment-card" v-loading="loading">
    <h2 class="card-header">收银台</h2>
    <el-divider />
    <p>订单号：{{ orderNo || orderId }}</p>
    <p>支付单号：{{ paymentNo }}</p>
    <p>支付方式：<el-tag>{{ channel }}</el-tag>（沙箱模拟）</p>
    <p>支付状态：<el-tag :type="currentStatus === 1 ? 'success' : 'warning'">{{ statusText[currentStatus] || currentStatus }}</el-tag></p>
    <p>应付金额：<strong class="amount">¥{{ amount.toFixed(2) }}</strong></p>

    <div class="card-bottom">
      <el-button type="primary" size="large" :loading="paying" :disabled="!paymentNo || currentStatus === 1" @click="goPay">
        立即支付
      </el-button>
      <el-button @click="router.push('/orders')">返回订单</el-button>
    </div>
    <p class="tip">演示说明：点击支付将调用沙箱网关，网关以 RSA 签名异步回调后端完成验签、金额核对与幂等记账。</p>
  </el-card>
</template>

<style scoped>
.payment-card {
  width: 60%;
  margin: 50px auto;
  padding: 20px;
  min-height: 320px;
}
.card-header { text-align: center; }
.amount { color: #f56c6c; font-size: 22px; }
.card-bottom {
  margin-top: 28px;
  display: flex;
  justify-content: center;
  gap: 12px;
}
.tip { margin-top: 20px; color: #909399; font-size: 12px; text-align: center; }
</style>
