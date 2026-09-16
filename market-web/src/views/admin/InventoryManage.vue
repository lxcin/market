<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getAdminBooks } from '../../api/books'
import { getAdminStock, updateStockPile } from '../../api/stockpiles'

const loading = ref(false)
const books = ref<any[]>([])
const total = ref(0)
const query = ref({ page: 1, size: 10, keyword: '' })

const dialogVisible = ref(false)
const current = ref<any>(null)
const stock = ref(0)
const lockedStock = ref(0)
const saving = ref(false)

async function load() {
  loading.value = true
  try {
    const res = await getAdminBooks(query.value)
    const page = res.data?.data
    books.value = page?.records || []
    total.value = page?.total || 0
  } finally { loading.value = false }
}

async function openStock(row: any) {
  current.value = row
  const res = await getAdminStock(row.id)
  const inv = res.data?.data
  stock.value = inv?.stock ?? 0
  lockedStock.value = inv?.lockedStock ?? 0
  dialogVisible.value = true
}

async function save() {
  saving.value = true
  try {
    await updateStockPile(current.value.id, stock.value)
    ElMessage.success('库存已更新')
    dialogVisible.value = false
  } finally { saving.value = false }
}

onMounted(load)
</script>

<template>
  <div>
    <h2>库存管理</h2>
    <el-card class="filter-card">
      <el-form inline>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="书名/作者" clearable @keyup.enter="load" style="width:200px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="query.page = 1; load()">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <el-table :data="books" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="title" label="书名" min-width="200" show-overflow-tooltip />
        <el-table-column prop="author" label="作者" width="150" show-overflow-tooltip />
        <el-table-column prop="price" label="价格" width="100">
          <template #default="{ row }">¥{{ Number(row.price).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openStock(row)">查看/调整库存</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        class="pager" background layout="total, prev, pager, next"
        :total="total" :page-size="query.size" :current-page="query.page"
        @current-change="(p:number) => { query.page = p; load() }"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" title="库存调整" width="420px">
      <p><strong>图书：</strong>{{ current?.title }}</p>
      <p><strong>锁定库存：</strong>{{ lockedStock }}</p>
      <el-form label-width="90px">
        <el-form-item label="可用库存">
          <el-input-number v-model="stock" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
h2 { margin-top: 0; }
.filter-card { margin-bottom: 16px; }
.pager { margin-top: 16px; justify-content: flex-end; }
</style>
