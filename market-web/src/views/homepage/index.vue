<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { router } from '../../router'
import { getHotBooks } from '../../api/recommend'

const books = ref<any[]>([])
const loading = ref(true)

onMounted(async () => {
  try {
    const res = await getHotBooks(10)
    books.value = res.data?.data || []
  } catch (e) {}
  loading.value = false
})

function goTo(id: number) { router.push(`/bookdetail/${id}`) }
</script>

<template>
  <div class="homepage">
    <div class="banner"><h1>📚 欢迎来到书城</h1><p>发现你的下一本好书</p></div>
    <div v-if="loading" style="text-align:center;padding:60px">加载中...</div>
    <div v-else-if="books.length">
      <h2>🔥 畅销热榜</h2>
      <div class="book-grid">
        <div class="book-card" v-for="b in books" :key="b.id" @click="goTo(b.id)">
          <div class="cover">📖</div>
          <div class="info">
            <div class="title">{{ b.title }}</div>
            <div class="author">{{ b.author }}</div>
            <div class="price-row"><span class="price">¥{{ b.price }}</span><span class="rate">⭐{{ b.rate }}</span></div>
          </div>
        </div>
      </div>
    </div>
    <div v-else style="text-align:center;padding:60px;color:#999">暂无数据</div>
  </div>
</template>

<style scoped>
.homepage { max-width: 1100px; margin: 0 auto; padding: 0 15px; }
.banner { text-align:center; padding:50px 20px 30px; background:linear-gradient(135deg,#667eea,#764ba2); color:#fff; border-radius:12px; margin:20px 0 30px; }
.banner h1 { font-size:28px; margin:0 0 8px; }
.banner p { opacity:0.9; margin:0; }
h2 { font-size:20px; }
.book-grid { display:grid; grid-template-columns:repeat(5,1fr); gap:16px; }
.book-card { background:#fff; border-radius:8px; overflow:hidden; box-shadow:0 2px 12px rgba(0,0,0,.08); cursor:pointer; transition:transform .2s; }
.book-card:hover { transform:translateY(-3px); }
.cover { height:160px; background:#f0f2f5; display:flex; align-items:center; justify-content:center; font-size:40px; }
.info { padding:10px 14px 14px; }
.title { font-size:14px; font-weight:600; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.author { font-size:12px; color:#999; margin:3px 0; }
.price-row { display:flex; justify-content:space-between; align-items:center; margin-top:8px; }
.price { color:#f56c6c; font-weight:bold; font-size:16px; }
.rate { color:#e6a23c; font-size:12px; }
</style>