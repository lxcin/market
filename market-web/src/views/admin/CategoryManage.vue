<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getAdminCategoryTree, createCategory, updateCategory, deleteCategory, type Category
} from '../../api/categories'

const loading = ref(false)
const tree = ref<Category[]>([])

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const form = ref<{ parentId: number | null; name: string; sort: number }>({ parentId: null, name: '', sort: 0 })

const parentOptions = computed(() => {
  const list: { id: number; name: string }[] = []
  const walk = (nodes: Category[], prefix = '') => {
    for (const n of nodes) {
      if (n.id !== editingId.value) list.push({ id: n.id!, name: prefix + n.name })
      if (n.children?.length) walk(n.children, prefix + n.name + ' / ')
    }
  }
  walk(tree.value)
  return list
})

async function load() {
  loading.value = true
  try {
    const res = await getAdminCategoryTree()
    tree.value = res.data?.data || []
  } finally { loading.value = false }
}

function openCreate(parent?: Category) {
  editingId.value = null
  form.value = { parentId: parent?.id ?? null, name: '', sort: 0 }
  dialogVisible.value = true
}

function openEdit(row: Category) {
  editingId.value = row.id!
  form.value = { parentId: row.parentId && row.parentId !== 0 ? row.parentId : null, name: row.name, sort: row.sort }
  dialogVisible.value = true
}

async function submit() {
  if (!form.value.name) { ElMessage.warning('请输入分类名称'); return }
  const payload: Category = {
    parentId: form.value.parentId ?? 0,
    name: form.value.name,
    sort: form.value.sort
  }
  if (editingId.value) {
    await updateCategory(editingId.value, payload)
    ElMessage.success('更新成功')
  } else {
    await createCategory(payload)
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  load()
}

async function remove(row: Category) {
  await ElMessageBox.confirm(`确定删除分类「${row.name}」？`, '提示', { type: 'warning' })
  await deleteCategory(row.id!)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<template>
  <div>
    <h2>分类管理</h2>
    <el-card>
      <div style="margin-bottom:12px">
        <el-button type="success" @click="openCreate()">新增一级分类</el-button>
      </div>
      <el-table :data="tree" v-loading="loading" row-key="id" default-expand-all
        :tree-props="{ children: 'children' }">
        <el-table-column prop="name" label="分类名称" min-width="220" />
        <el-table-column prop="sort" label="排序" width="100" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <el-button link type="primary" @click="openCreate(row)">新增子类</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑分类' : '新增分类'" width="440px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="上级分类">
          <el-select v-model="form.parentId" clearable placeholder="不选则为一级分类" style="width:100%">
            <el-option v-for="c in parentOptions" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="名称" required><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
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
</style>
