<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAddressList, addAddress, updateAddress, deleteAddress, setDefaultAddress } from '../../api/addresses'

const addresses = ref<any[]>([])
const loading = ref(false)
const showDialog = ref(false)
const editing = ref<any>(null)
const form = ref({ receiverName: '', phone: '', province: '', city: '', district: '', detail: '', isDefault: 0 })

async function loadAddresses() {
  loading.value = true
  try {
    const res = await getAddressList()
    addresses.value = res.data?.data || []
  } finally { loading.value = false }
}

function edit(addr: any) {
  editing.value = addr
  form.value = { ...addr }
  showDialog.value = true
}

function addNew() {
  editing.value = null
  form.value = { receiverName: '', phone: '', province: '', city: '', district: '', detail: '', isDefault: 0 }
  showDialog.value = true
}

async function save() {
  try {
    if (editing.value) {
      await updateAddress(editing.value.id, form.value)
    } else {
      await addAddress(form.value)
    }
    ElMessage.success('保存成功')
    showDialog.value = false
    loadAddresses()
  } catch {}
}

async function remove(id: number) {
  await ElMessageBox.confirm('确定删除？', '提示', { type: 'warning' })
  await deleteAddress(id)
  ElMessage.success('已删除')
  loadAddresses()
}

async function setDefault(id: number) {
  await setDefaultAddress(id)
  ElMessage.success('已设为默认')
  loadAddresses()
}

onMounted(loadAddresses)
</script>

<template>
  <div class="addr-page">
    <div class="header">
      <h2>收货地址</h2>
      <el-button type="primary" @click="addNew">添加新地址</el-button>
    </div>

    <div v-loading="loading" class="addr-list">
      <el-card v-for="a in addresses" :key="a.id" class="addr-card" :class="{default:a.isDefault===1}">
        <div class="addr-content">
          <div class="addr-line1">
            <b>{{ a.receiverName }}</b>
            <span>{{ a.phone }}</span>
            <el-tag v-if="a.isDefault===1" type="danger" size="small">默认</el-tag>
          </div>
          <div class="addr-line2">{{ a.province }}{{ a.city }}{{ a.district }} {{ a.detail }}</div>
        </div>
        <div class="addr-actions">
          <el-button link type="primary" @click="edit(a)">编辑</el-button>
          <el-button link type="danger" @click="remove(a.id)">删除</el-button>
          <el-button v-if="a.isDefault!==1" link @click="setDefault(a.id)">设为默认</el-button>
        </div>
      </el-card>
      <el-empty v-if="!loading && !addresses.length" description="还没有收货地址" />
    </div>
  </div>

  <el-dialog v-model="showDialog" :title="editing ? '编辑地址' : '新增地址'" width="450px">
    <el-form>
      <el-form-item label="收货人"><el-input v-model="form.receiverName" /></el-form-item>
      <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
      <el-form-item label="省份"><el-input v-model="form.province" /></el-form-item>
      <el-form-item label="城市"><el-input v-model="form.city" /></el-form-item>
      <el-form-item label="区/县"><el-input v-model="form.district" /></el-form-item>
      <el-form-item label="详细地址"><el-input v-model="form.detail" type="textarea" /></el-form-item>
      <el-form-item label="设为默认"><el-switch v-model="form.isDefault" :active-value="1" :inactive-value="0" /></el-form-item>
    </el-form>
    <template #footer><el-button @click="showDialog=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
  </el-dialog>
</template>

<style scoped>
.addr-page { max-width: 800px; margin: 0 auto; padding: 20px; }
.header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.header h2 { margin: 0; }
.addr-list { display: flex; flex-direction: column; gap: 12px; }
.addr-card { border-left: 3px solid #ebeef5; }
.addr-card.default { border-left-color: #f56c6c; }
.addr-content { margin-bottom: 8px; }
.addr-line1 { display: flex; gap: 12px; align-items: center; margin-bottom: 4px; }
.addr-line2 { font-size: 13px; color: #666; }
.addr-actions { display: flex; gap: 12px; }
</style>
