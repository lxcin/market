<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { router } from '../../router'
import { getAddressList, addAddress, updateAddress } from '../../api/addresses'
import { getAllCartItems } from '../../api/cart'
import { bestFitCoupons, previewCoupon } from '../../api/coupon'
import { createOrder } from '../../api/orders'

const addresses = ref<any[]>([])
const selectedAddressId = ref<number | null>(null)
const orderItems = ref<any[]>([])
const coupons = ref<any[]>([])
const selectedCouponId = ref<number | null>(null)
const discountAmount = ref(0)
const loading = ref(false)
const submitting = ref(false)

// Address dialog
const showAddrDialog = ref(false)
const addrForm = ref({ receiverName: '', phone: '', province: '', city: '', district: '', detail: '', isDefault: 0 })
const editingAddr = ref<any>(null)

const totalAmount = computed(() => orderItems.value.reduce((s, i) => s + i.price * i.quantity, 0))
const payAmount = computed(() => totalAmount.value - discountAmount.value)

async function loadAddresses() {
  const res = await getAddressList()
  const list = res.data?.data || []
  addresses.value = list
  const def = list.find((a: any) => a.isDefault === 1)
  if (def) selectedAddressId.value = def.id
  else if (list.length) selectedAddressId.value = list[0].id
}

async function loadCartItems() {
  loading.value = true
  try {
    const res = await getAllCartItems()
    const items = (res.data?.data || []).filter((i: any) => i.checked)
    orderItems.value = items
    if (items.length) loadCoupons()
  } finally { loading.value = false }
}

async function loadCoupons() {
  const res = await bestFitCoupons(totalAmount.value)
  const list = res.data?.data || []
  coupons.value = list
  if (list.length) {
    selectedCouponId.value = list[0].userCouponId
    discountAmount.value = list[0].discountAmount || 0
  }
}

async function handleCouponChange(val: number | null) {
  if (!val) { discountAmount.value = 0; return }
  const res = await previewCoupon(val, totalAmount.value)
  discountAmount.value = res.data?.data?.discountAmount || 0
}

async function saveAddress() {
  if (editingAddr.value) {
    await updateAddress(editingAddr.value.id, addrForm.value)
  } else {
    await addAddress(addrForm.value)
  }
  showAddrDialog.value = false
  loadAddresses()
  addrForm.value = { receiverName: '', phone: '', province: '', city: '', district: '', detail: '', isDefault: 0 }
}

function editAddress(addr: any) {
  editingAddr.value = addr
  addrForm.value = { ...addr }
  showAddrDialog.value = true
}

async function submitOrder() {
  if (!selectedAddressId.value) { ElMessage.warning('请选择收货地址'); return }
  submitting.value = true
  try {
    const key = crypto.randomUUID?.() || Date.now().toString(36) + Math.random().toString(36)
    await createOrder({
      idempotentKey: key,
      addressId: selectedAddressId.value,
      userCouponId: selectedCouponId.value || null
    })
    ElMessage.success('下单成功')
    router.push('/orders')
  } catch { } finally { submitting.value = false }
}

onMounted(async () => {
  await loadCartItems()
  await loadAddresses()
})
</script>

<template>
  <div class="confirm-page" v-loading="loading">
    <h2 style="padding:20px">确认订单</h2>
    <div class="content" v-if="orderItems.length">
      <el-card class="section">
        <template #header>收货地址</template>
        <el-radio-group v-model="selectedAddressId">
          <el-radio v-for="a in addresses" :key="a.id" :label="a.id" border class="addr-radio">
            <b>{{ a.receiverName }}</b> {{ a.phone }}<br/>
            <span style="font-size:12px;color:#666">{{ a.province }}{{ a.city }}{{ a.district }} {{ a.detail }}</span>
          </el-radio>
        </el-radio-group>
        <el-button type="primary" plain size="small" style="margin-top:8px" @click="editingAddr=null;addrForm={receiverName:'',phone:'',province:'',city:'',district:'',detail:'',isDefault:0};showAddrDialog=true">添加新地址</el-button>
      </el-card>

      <el-card class="section">
        <template #header>商品明细</template>
        <div v-for="item in orderItems" :key="item.bookId" class="order-item">
          <span>{{ item.title }}</span>
          <span>×{{ item.quantity }}</span>
          <span class="price">¥{{ (item.price * item.quantity).toFixed(2) }}</span>
        </div>
      </el-card>

      <el-card class="section" v-if="coupons.length">
        <template #header>优惠券</template>
        <el-select v-model="selectedCouponId" @change="handleCouponChange" clearable placeholder="选择优惠券" style="width:300px">
          <el-option v-for="c in coupons" :key="c.userCouponId" :label="`${c.templateName} (-¥${c.discountAmount})`" :value="c.userCouponId" />
        </el-select>
        <div v-if="discountAmount" style="color:#67c23a;margin-top:8px">优惠: -¥{{ discountAmount }}</div>
      </el-card>

      <div class="bottom-bar">
        <span>合计: ¥{{ totalAmount.toFixed(2) }} <template v-if="discountAmount">- ¥{{ discountAmount }} = <b class="pay">¥{{ payAmount.toFixed(2) }}</b></template></span>
        <el-button type="primary" size="large" :loading="submitting" @click="submitOrder">提交订单</el-button>
      </div>
    </div>
    <el-empty v-else description="没有待结算商品" />
  </div>

  <el-dialog v-model="showAddrDialog" :title="editingAddr ? '编辑' : '新增'" width="450px">
    <el-form>
      <el-form-item label="收货人"><el-input v-model="addrForm.receiverName" /></el-form-item>
      <el-form-item label="手机号"><el-input v-model="addrForm.phone" /></el-form-item>
      <el-form-item label="省份"><el-input v-model="addrForm.province" /></el-form-item>
      <el-form-item label="城市"><el-input v-model="addrForm.city" /></el-form-item>
      <el-form-item label="区/县"><el-input v-model="addrForm.district" /></el-form-item>
      <el-form-item label="详细地址"><el-input v-model="addrForm.detail" type="textarea" /></el-form-item>
    </el-form>
    <template #footer><el-button @click="showAddrDialog=false">取消</el-button><el-button type="primary" @click="saveAddress">保存</el-button></template>
  </el-dialog>
</template>

<style scoped>
.confirm-page { max-width: 800px; margin: 0 auto; }
.content { padding: 0 20px; }
.section { margin-bottom: 16px; }
.addr-radio { display: block; margin: 4px 0; padding: 8px; width: 100%; height: auto; }
.order-item { display: flex; justify-content: space-between; align-items: center; padding: 8px 0; border-bottom: 1px solid #f0f0f0; }
.price { color: #f56c6c; font-weight: bold; }
.bottom-bar { display: flex; justify-content: space-between; align-items: center; padding: 16px 0; }
.pay { font-size: 22px; color: #f56c6c; }
</style>
