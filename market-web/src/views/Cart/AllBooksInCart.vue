<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { router } from '../../router'
import {
  getAllCartItems,
  toggleCartItemCheck,
  checkAllCartItems,
  updateProductQuantity,
  deleteProductFromCart,
  removeCheckedCartItems
} from '../../api/cart'

interface CartItem {
  bookId: number
  title: string
  author: string
  coverImage: string
  price: number
  quantity: number
  checked: boolean
}

const items = ref<CartItem[]>([])
const loading = ref(true)

async function loadCart() {
  try {
    const res = await getAllCartItems()
    items.value = (res.data?.data || []).map((i: any) => ({ ...i, checked: i.checked ?? true }))
  } catch { } finally { loading.value = false }
}

const checkedItems = computed(() => items.value.filter(i => i.checked))
const totalPrice = computed(() => checkedItems.value.reduce((s, i) => s + i.price * i.quantity, 0))
const totalCount = computed(() => checkedItems.value.reduce((s, i) => s + i.quantity, 0))

async function toggleCheck(item: CartItem) {
  item.checked = !item.checked
  try { await toggleCartItemCheck(item.bookId) } catch { item.checked = !item.checked }
}

function toggleAll() {
  const allChecked = items.value.every(i => i.checked)
  items.value.forEach(i => i.checked = !allChecked)
  checkAllCartItems(!allChecked)
}

async function updateQty(item: CartItem, qty: number) {
  if (qty < 1) return
  item.quantity = qty
  await updateProductQuantity(item.bookId, qty)
}

async function removeItem(item: CartItem) {
  await deleteProductFromCart(item.bookId)
  items.value = items.value.filter(i => i.bookId !== item.bookId)
}

async function removeChecked() {
  await removeCheckedCartItems()
  items.value = items.value.filter(i => !i.checked)
}

function goCheckout() {
  router.push('/checkout')
}

onMounted(loadCart)
</script>

<template>
  <div class="cart-page" v-loading="loading">
    <h2 style="padding: 20px;">购物车</h2>
    <el-empty v-if="!loading && !items.length" description="购物车是空的">
      <el-button type="primary" @click="router.push('/')">去逛逛</el-button>
    </el-empty>

    <div v-else class="cart-content">
      <el-table :data="items" style="width:100%">
        <el-table-column width="50">
          <template #header><el-checkbox :model-value="items.every(i=>i.checked)" @change="toggleAll" /></template>
          <template #default="{ row }"><el-checkbox :model-value="row.checked" @change="toggleCheck(row)" /></template>
        </el-table-column>
        <el-table-column label="图书" min-width="300">
          <template #default="{ row }">
            <div style="display:flex;align-items:center;gap:12px">
              <el-image v-if="row.coverImage" :src="row.coverImage" fit="cover" style="width:60px;height:80px;border-radius:4px" />
              <div v-else style="width:60px;height:80px;background:#f0f2f5;border-radius:4px;display:flex;align-items:center;justify-content:center;font-size:24px">📖</div>
              <div>
                <div style="font-weight:600">{{ row.title }}</div>
                <div style="font-size:12px;color:#999">{{ row.author }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="单价" width="100"><template #default="{ row }">¥{{ row.price }}</template></el-table-column>
        <el-table-column label="数量" width="140">
          <template #default="{ row }">
            <el-input-number :model-value="row.quantity" :min="1" size="small" @change="(v:number)=>updateQty(row,v)" />
          </template>
        </el-table-column>
        <el-table-column label="小计" width="100"><template #default="{ row }">¥{{ (row.price*row.quantity).toFixed(2) }}</template></el-table-column>
        <el-table-column label="操作" width="80">
          <template #default="{ row }"><el-button type="danger" link @click="removeItem(row)">删除</el-button></template>
        </el-table-column>
      </el-table>

      <div class="cart-footer">
        <div class="footer-left">
          <el-checkbox :model-value="items.every(i=>i.checked)" @change="toggleAll">全选</el-checkbox>
          <el-button type="danger" link @click="removeChecked">删除选中</el-button>
        </div>
        <div class="footer-right">
          <span>已选 <b>{{ totalCount }}</b> 件，合计：<span class="total-price">¥{{ totalPrice.toFixed(2) }}</span></span>
          <el-button type="primary" size="large" :disabled="!checkedItems.length" @click="goCheckout">去结算</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.cart-page { max-width: 1000px; margin: 0 auto; }
.cart-content { padding: 0 20px; }
.cart-footer { display: flex; justify-content: space-between; align-items: center; padding: 16px 0; border-top: 1px solid #ebeef5; margin-top: 16px; }
.footer-left { display: flex; gap: 16px; align-items: center; }
.footer-right { display: flex; gap: 16px; align-items: center; }
.total-price { font-size: 20px; color: #f56c6c; font-weight: bold; }
</style>
