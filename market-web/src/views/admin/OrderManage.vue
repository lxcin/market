<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getAllOrdersByAdmin, getAdminOrderDetail, updateOrderStatusByAdmin } from '../../api/orders'

const loading = ref(false)
const orders = ref<any[]>([])
const total = ref(0)
const query = ref({ page: 1, size: 10, status: undefined as number | undefined, keyword: '' })

const detailVisible = ref(false)
const detail = ref<any>(null)

const statusMap: Record<number, { text: string; type: string }> = {
  0: { text: '待支付', type: 'warning' },
  1: { text: '已支付', type: 'success' },
  2: { text: '已发货', type: 'primary' },
  3: { text: '已完成', type: 'success' },
  4: { text: '已取消', type: 'info' },
  5: { text: '已退款', type: 'danger' }
}

async function load() {
  loading.value = true
  try {
    const res = await getAllOrdersByAdmin(query.value)
    const page = res.data?.data
    orders.value = page?.records || []
    total.value = page?.total || 0
  } finally { loading.value = false }
}

async function openDetail(row: any) {
  const res = await getAdminOrderDetail(row.id)
  detail.value = res.data?.data
  detailVisible.value = true
}

async function setStatus(row: any, status: number) {
  await updateOrderStatusByAdmin(row.id, status)
  ElMessage.success('操作成功')
  load()
}

onMounted(load)
</script>

<template>
  <div>
    <h2>订单管理</h2>
    <el-card class="filter-card">
      <el-form inline>
        <el-form-item label="订单号/收货人">
          <el-input v-model="query.keyword" clearable @keyup.enter="load" style="width:200px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部" style="width:140px">
            <el-option v-for="(v, k) in statusMap" :key="k" :label="v.text" :value="Number(k)" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="query.page = 1; load()">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <el-table :data="orders" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="orderNo" label="订单号" width="200" />
        <el-table-column prop="receiverName" label="收货人" width="120" />
        <el-table-column prop="payAmount" label="实付" width="110">
          <template #default="{ row }">¥{{ Number(row.payAmount).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="下单时间" width="180" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="(statusMap[row.status]?.type as any) || 'info'">{{ statusMap[row.status]?.text || row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button v-if="row.status === 1" link type="warning" @click="setStatus(row, 2)">发货</el-button>
            <el-button v-if="row.status === 2" link type="success" @click="setStatus(row, 3)">完成</el-button>
            <el-button v-if="row.status === 0" link type="danger" @click="setStatus(row, 4)">取消</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        class="pager" background layout="total, prev, pager, next"
        :total="total" :page-size="query.size" :current-page="query.page"
        @current-change="(p:number) => { query.page = p; load() }"
      />
    </el-card>

    <el-dialog v-model="detailVisible" title="订单详情" width="640px">
      <template v-if="detail">
        <p><strong>订单号：</strong>{{ detail.orderNo }}</p>
        <p><strong>收货人：</strong>{{ detail.receiverName }} {{ detail.receiverPhone }}</p>
        <p><strong>地址：</strong>{{ detail.receiverAddress }}</p>
        <p><strong>商品总额：</strong>¥{{ Number(detail.totalAmount).toFixed(2) }}
          <span v-if="detail.discountAmount">（优惠 -¥{{ Number(detail.discountAmount).toFixed(2) }}）</span>
          ，<strong>实付：</strong>¥{{ Number(detail.payAmount).toFixed(2) }}</p>
        <el-table :data="detail.items" size="small" style="margin-top:10px">
          <el-table-column prop="bookTitle" label="图书" />
          <el-table-column prop="price" label="单价" width="100">
            <template #default="{ row }">¥{{ Number(row.price).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="80" />
        </el-table>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
h2 { margin-top: 0; }
.filter-card { margin-bottom: 16px; }
.pager { margin-top: 16px; justify-content: flex-end; }
</style>
