<script setup lang="ts">

</script>

<template>
    <div class="pay-success">
      <el-result
          icon="success"
          title="支付成功"
          sub-title="感谢你的购买，我们已收到你的订单">
        <template #extra>
          <el-button type="primary" @click="$router.push('/orders')">查看订单</el-button>
        </template>
      </el-result>
    </div>

</template>

<style scoped>

</style>