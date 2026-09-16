<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getAdminCoupons, createCoupon, updateCoupon, deleteCoupon, updateCouponStatus,
  type CouponTemplateInfo
} from '../../api/coupon'

const loading = ref(false)
const coupons = ref<any[]>([])
const total = ref(0)
const query = ref({ page: 1, size: 10 })

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const form = ref<CouponTemplateInfo>(emptyForm())

const typeMap: Record<number, string> = { 1: '满减', 2: '折扣', 3: '直减' }

function emptyForm(): CouponTemplateInfo {
  return {
    name: '', type: 1, thresholdAmount: 0, discountAmount: 0, discountRate: 1,
    totalCount: 100, perUserLimit: 1, startTime: '', endTime: ''
  }
}

async function load() {
  loading.value = true
  try {
    const res = await getAdminCoupons(query.value)
    const page = res.data?.data
    coupons.value = page?.records || []
    total.value = page?.total || 0
  } finally { loading.value = false }
}

function openCreate() {
  editingId.value = null
  form.value = emptyForm()
  dialogVisible.value = true
}

function openEdit(row: any) {
  editingId.value = row.id
  form.value = {
    id: row.id, name: row.name, type: row.type,
    thresholdAmount: Number(row.thresholdAmount), discountAmount: Number(row.discountAmount),
    discountRate: Number(row.discountRate), totalCount: row.totalCount,
    perUserLimit: row.perUserLimit, startTime: row.startTime, endTime: row.endTime
  }
  dialogVisible.value = true
}

async function submit() {
  if (!form.value.name) { ElMessage.warning('请输入名称'); return }
  if (!form.value.totalCount) { ElMessage.warning('请输入发行总量'); return }
  if (editingId.value) {
    await updateCoupon(editingId.value, form.value)
    ElMessage.success('更新成功')
  } else {
    await createCoupon(form.value)
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  load()
}

async function toggleStatus(row: any) {
  const next = row.status === 1 ? 0 : 1
  await updateCouponStatus(row.id, next)
  ElMessage.success(next === 1 ? '已启用' : '已停用')
  load()
}

async function remove(row: any) {
  await ElMessageBox.confirm(`确定删除「${row.name}」？`, '提示', { type: 'warning' })
  await deleteCoupon(row.id)
  ElMessage.success('已删除')
  load()
}

function valueText(row: any) {
  if (row.type === 2) return `${(Number(row.discountRate) * 10).toFixed(1)}折`
  return `减¥${Number(row.discountAmount).toFixed(2)}`
}

onMounted(load)
</script>

<template>
  <div>
    <h2>优惠券管理</h2>
    <el-card>
      <div style="margin-bottom:12px">
        <el-button type="success" @click="openCreate">新增优惠券</el-button>
      </div>
      <el-table :data="coupons" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="名称" min-width="160" show-overflow-tooltip />
        <el-table-column label="类型" width="90">
          <template #default="{ row }">{{ typeMap[row.type] }}</template>
        </el-table-column>
        <el-table-column label="优惠" width="100">
          <template #default="{ row }">{{ valueText(row) }}</template>
        </el-table-column>
        <el-table-column label="门槛" width="110">
          <template #default="{ row }">{{ Number(row.thresholdAmount) > 0 ? '满¥' + Number(row.thresholdAmount).toFixed(0) : '无门槛' }}</template>
        </el-table-column>
        <el-table-column label="剩余/总量" width="120">
          <template #default="{ row }">{{ row.remainingCount }} / {{ row.totalCount }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link :type="row.status === 1 ? 'warning' : 'success'" @click="toggleStatus(row)">
              {{ row.status === 1 ? '停用' : '启用' }}
            </el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        class="pager" background layout="total, prev, pager, next"
        :total="total" :page-size="query.size" :current-page="query.page"
        @current-change="(p:number) => { query.page = p; load() }"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑优惠券' : '新增优惠券'" width="520px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="名称" required><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="类型" required>
          <el-select v-model="form.type" style="width:100%">
            <el-option label="满减" :value="1" />
            <el-option label="折扣" :value="2" />
            <el-option label="直减" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="使用门槛">
          <el-input-number v-model="form.thresholdAmount" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item v-if="form.type !== 2" label="减免金额">
          <el-input-number v-model="form.discountAmount" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item v-if="form.type === 2" label="折扣率">
          <el-input-number v-model="form.discountRate" :min="0.01" :max="1" :step="0.05" :precision="2" />
          <span style="margin-left:8px;color:#909399">如 0.8 = 8折</span>
        </el-form-item>
        <el-form-item label="发行总量" required>
          <el-input-number v-model="form.totalCount" :min="1" />
        </el-form-item>
        <el-form-item label="每人限领">
          <el-input-number v-model="form.perUserLimit" :min="1" />
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width:100%" />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
h2 { margin-top: 0; }
.pager { margin-top: 16px; justify-content: flex-end; }
</style>
