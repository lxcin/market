<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getAllCoupons, getUserCouponList, acquireCoupon } from '../../api/coupon'

const templates = ref<any[]>([])
const myCoupons = ref<any[]>([])
const activeTab = ref('templates')
const loading = ref(false)

async function loadTemplates() {
  loading.value = true
  try {
    const res = await getAllCoupons()
    templates.value = res.data?.data || []
  } finally { loading.value = false }
}

async function loadMyCoupons() {
  loading.value = true
  try {
    const res = await getUserCouponList()
    const list = res.data?.data || []
    const templates = await getAllCoupons()
    const tplMap = new Map((templates.data?.data || []).map((t:any) => [t.id, t]))
    myCoupons.value = list.map((c:any) => ({
      ...c,
      template: tplMap.get(c.couponTemplateId)
    }))
  } finally { loading.value = false }
}

async function claim(templateId: number) {
  await acquireCoupon(templateId)
  ElMessage.success('领取请求已受理，优惠券发放中...')
  // 异步落库，轮询“我的优惠券”直到到账
  const deadline = Date.now() + 6000
  while (Date.now() < deadline) {
    await new Promise(r => setTimeout(r, 700))
    try {
      const res = await getUserCouponList()
      const list = res.data?.data || []
      if (list.some((c: any) => c.couponTemplateId === templateId)) break
    } catch {}
  }
  await loadTemplates()
  await loadMyCoupons()
}

function typeLabel(t: number) { return ['', '满减', '折扣', '直减'][t] || '未知' }
function typeTag(t: number) { return ['', 'warning', 'success', 'primary'][t] || 'info' }

onMounted(() => { loadTemplates(); loadMyCoupons() })
</script>

<template>
  <div class="coupon-page">
    <h2 style="padding:20px">优惠券中心</h2>
    <el-tabs v-model="activeTab" @tab-change="(t:any) => t==='templates' ? loadTemplates() : loadMyCoupons()">
      <el-tab-pane label="领券中心" name="templates">
        <div class="coupon-grid" v-loading="loading">
          <el-card v-for="t in templates" :key="t.id" class="coupon-card" shadow="hover">
            <div class="coupon-left">
              <div class="coupon-value">
                <template v-if="t.type===1">减{{ t.discountAmount }}元</template>
                <template v-else-if="t.type===2">{{ (t.discountRate*10).toFixed(0) }}折</template>
                <template v-else>减{{ t.discountAmount }}元</template>
              </div>
              <div class="coupon-threshold" v-if="t.thresholdAmount>0">满{{ t.thresholdAmount }}元可用</div>
              <div class="coupon-threshold" v-else>无门槛</div>
            </div>
            <div class="coupon-right">
              <div class="coupon-name">{{ t.name }}</div>
              <el-tag :type="typeTag(t.type)" size="small">{{ typeLabel(t.type) }}</el-tag>
              <div class="coupon-count">剩余 {{ t.remainingCount }} / {{ t.totalCount }}</div>
              <div class="coupon-time">有效期至 {{ t.endTime?.substring(0,10) }}</div>
              <el-button type="primary" size="small" @click="claim(t.id)" :disabled="t.remainingCount<=0">立即领取</el-button>
            </div>
          </el-card>
          <el-empty v-if="!loading && !templates.length" description="暂无可领优惠券" />
        </div>
      </el-tab-pane>

      <el-tab-pane label="我的优惠券" name="my">
        <div class="coupon-grid" v-loading="loading">
          <el-card v-for="c in myCoupons" :key="c.id" class="coupon-card" :class="{used:c.status===1,expired:c.status===2}" shadow="hover">
            <div class="coupon-left">
              <div class="coupon-value">
                <template v-if="c.template?.type===1">减{{ c.template?.discountAmount }}元</template>
                <template v-else-if="c.template?.type===2">{{ (c.template?.discountRate*10).toFixed(0) }}折</template>
                <template v-else>减{{ c.template?.discountAmount }}元</template>
              </div>
            </div>
            <div class="coupon-right">
              <div class="coupon-name">{{ c.template?.name }}</div>
              <el-tag v-if="c.status===0" type="success" size="small">未使用</el-tag>
              <el-tag v-else-if="c.status===1" type="info" size="small">已使用</el-tag>
              <el-tag v-else type="danger" size="small">已过期</el-tag>
            </div>
          </el-card>
          <el-empty v-if="!loading && !myCoupons.length" description="还没有优惠券" />
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.coupon-page { max-width: 1000px; margin: 0 auto; }
.coupon-grid { display: grid; gap: 16px; padding: 0 20px; }
.coupon-card { display: flex; overflow: hidden; }
.coupon-card.used { opacity: 0.6; }
.coupon-card.expired { opacity: 0.4; }
.coupon-left { width: 140px; background: linear-gradient(135deg,#f56c6c,#e64242); color: #fff; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 20px; font-size: 18px; font-weight: bold; }
.coupon-value { font-size: 22px; }
.coupon-threshold { font-size: 12px; opacity: 0.8; margin-top: 4px; }
.coupon-right { flex: 1; padding: 20px; display: flex; flex-direction: column; gap: 8px; }
.coupon-name { font-size: 16px; font-weight: 600; }
.coupon-count { font-size: 12px; color: #999; }
.coupon-time { font-size: 12px; color: #999; }
</style>
