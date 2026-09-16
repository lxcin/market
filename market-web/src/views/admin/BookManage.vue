<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getAdminBooks, addBook, updateBook, deleteBook, updateBookStatus, type Book
} from '../../api/books'
import { getAdminCategoryTree, type Category } from '../../api/categories'

const loading = ref(false)
const books = ref<any[]>([])
const total = ref(0)
const query = ref({ page: 1, size: 10, keyword: '', categoryId: undefined as number | undefined, status: undefined as number | undefined })

const categories = ref<Category[]>([])
const flatCategories = computed(() => {
  const list: { id: number; name: string }[] = []
  const walk = (nodes: Category[], prefix = '') => {
    for (const n of nodes) {
      list.push({ id: n.id!, name: prefix + n.name })
      if (n.children?.length) walk(n.children, prefix + n.name + ' / ')
    }
  }
  walk(categories.value)
  return list
})

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const form = ref<Book>(emptyForm())

function emptyForm(): Book {
  return {
    title: '', author: '', isbn: '', publisher: '', publishDate: '',
    categoryId: undefined as any, price: 0, coverImage: '', description: '', status: 1
  }
}

async function load() {
  loading.value = true
  try {
    const res = await getAdminBooks(query.value)
    const page = res.data?.data
    books.value = page?.records || []
    total.value = page?.total || 0
  } finally { loading.value = false }
}

async function loadCategories() {
  const res = await getAdminCategoryTree()
  categories.value = res.data?.data || []
}

function openCreate() {
  editingId.value = null
  form.value = emptyForm()
  dialogVisible.value = true
}

function openEdit(row: any) {
  editingId.value = row.id
  form.value = { ...row }
  dialogVisible.value = true
}

async function submit() {
  if (!form.value.title || !form.value.author) {
    ElMessage.warning('请填写书名和作者')
    return
  }
  if (!form.value.categoryId) {
    ElMessage.warning('请选择分类')
    return
  }
  if (editingId.value) {
    await updateBook(editingId.value, form.value)
    ElMessage.success('更新成功')
  } else {
    await addBook(form.value)
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  load()
}

async function toggleStatus(row: any) {
  const next = row.status === 1 ? 0 : 1
  await updateBookStatus(row.id, next)
  ElMessage.success(next === 1 ? '已上架' : '已下架')
  load()
}

async function remove(row: any) {
  await ElMessageBox.confirm(`确定删除《${row.title}》？`, '提示', { type: 'warning' })
  await deleteBook(row.id)
  ElMessage.success('已删除')
  load()
}

function resetQuery() {
  query.value = { page: 1, size: 10, keyword: '', categoryId: undefined, status: undefined }
  load()
}

onMounted(() => { loadCategories(); load() })
</script>

<template>
  <div>
    <h2>图书管理</h2>

    <el-card class="filter-card">
      <el-form inline>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="书名/作者" clearable @keyup.enter="load" style="width:180px" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="query.categoryId" clearable placeholder="全部" style="width:180px">
            <el-option v-for="c in flatCategories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部" style="width:120px">
            <el-option label="在售" :value="1" />
            <el-option label="下架" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="query.page = 1; load()">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
          <el-button type="success" @click="openCreate">新增图书</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <el-table :data="books" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="封面" width="80">
          <template #default="{ row }">
            <el-image v-if="row.coverImage" :src="row.coverImage" fit="cover" style="width:44px;height:60px" />
            <span v-else>📖</span>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="书名" min-width="180" show-overflow-tooltip />
        <el-table-column prop="author" label="作者" width="140" show-overflow-tooltip />
        <el-table-column label="分类" width="120">
          <template #default="{ row }">{{ flatCategories.find(c => c.id === row.categoryId)?.name || row.categoryId }}</template>
        </el-table-column>
        <el-table-column prop="price" label="价格" width="90">
          <template #default="{ row }">¥{{ Number(row.price).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="sales" label="销量" width="80" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '在售' : '下架' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link :type="row.status === 1 ? 'warning' : 'success'" @click="toggleStatus(row)">
              {{ row.status === 1 ? '下架' : '上架' }}
            </el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pager"
        background
        layout="total, prev, pager, next"
        :total="total"
        :page-size="query.size"
        :current-page="query.page"
        @current-change="(p:number) => { query.page = p; load() }"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑图书' : '新增图书'" width="620px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="书名" required><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="作者" required><el-input v-model="form.author" /></el-form-item>
        <el-form-item label="分类" required>
          <el-select v-model="form.categoryId" placeholder="请选择" style="width:100%">
            <el-option v-for="c in flatCategories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="价格"><el-input-number v-model="form.price" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="ISBN"><el-input v-model="form.isbn" /></el-form-item>
        <el-form-item label="出版社"><el-input v-model="form.publisher" /></el-form-item>
        <el-form-item label="出版日期">
          <el-date-picker v-model="form.publishDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="封面URL"><el-input v-model="form.coverImage" /></el-form-item>
        <el-form-item label="简介"><el-input v-model="form.description" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">在售</el-radio>
            <el-radio :value="0">下架</el-radio>
          </el-radio-group>
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
.filter-card { margin-bottom: 16px; }
.pager { margin-top: 16px; justify-content: flex-end; }
</style>
