<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getAuthorBooks } from '../../api/books'

const route = useRoute()
const router = useRouter()

const author = ref(String(route.params.name || ''))
const books = ref<any[]>([])
const categories = ref<{ categoryName: string; count: number }[]>([])
const avgRate = ref(0)
const total = ref(0)
const page = ref(1)
const size = 12
const loading = ref(false)

async function load() {
  if (!author.value) return
  loading.value = true
  try {
    const res = await getAuthorBooks(author.value, page.value, size)
    const d = res.data?.data || {}
    books.value = d.items || []
    categories.value = d.categories || []
    avgRate.value = d.avgRate || 0
    total.value = d.total || 0
  } finally { loading.value = false }
}

function goDetail(id: number) { router.push(`/bookdetail/${id}`) }

onMounted(load)
watch(() => route.params.name, (v) => {
  author.value = String(v || ''); page.value = 1; load()
})
</script>

<template>
  <div class="author-page" v-loading="loading">
    <el-card class="profile" shadow="never">
      <div class="profile-head">
        <el-avatar :size="72">{{ author.charAt(0) }}</el-avatar>
        <div class="meta">
          <h2>{{ author }}</h2>
          <div class="stats">
            <span>作品 <b>{{ total }}</b> 部</span>
            <span>平均评分 <b>{{ Number(avgRate).toFixed(1) }}</b></span>
            <span v-if="categories.length">涉及分类
              <el-tag v-for="c in categories" :key="c.categoryName" size="small" class="cate-tag">
                {{ c.categoryName }} ({{ c.count }})
              </el-tag>
            </span>
          </div>
        </div>
      </div>
    </el-card>

    <h3 class="section-title">全部作品</h3>
    <el-empty v-if="!loading && !books.length" description="暂无该作者的作品" />
    <div class="book-grid">
      <div class="book-card" v-for="b in books" :key="b.id" @click="goDetail(b.id)">
        <div class="cover">
          <el-image v-if="b.coverImage" :src="b.coverImage" fit="cover" style="width:100%;height:100%" />
          <span v-else>📖</span>
        </div>
        <div class="info">
          <div class="title">{{ b.title }}</div>
          <div class="price-row">
            <span class="price">¥{{ b.price }}</span>
            <span class="rate">⭐{{ b.rate }}</span>
          </div>
        </div>
      </div>
    </div>
    <el-pagination
      v-if="total > size"
      class="pager" background layout="prev, pager, next"
      :total="total" :page-size="size" :current-page="page"
      @current-change="(p:number) => { page = p; load() }"
    />
  </div>
</template>

<style scoped>
.author-page { max-width: 1100px; margin: 0 auto; padding: 20px 15px; }
.profile { margin-bottom: 20px; border-radius: 8px; }
.profile-head { display: flex; align-items: center; gap: 20px; }
.meta h2 { margin: 0 0 10px; }
.stats { display: flex; flex-wrap: wrap; gap: 20px; color: #606266; font-size: 14px; align-items: center; }
.cate-tag { margin-left: 6px; }
.section-title { margin: 0 0 14px; }
.book-grid { display: grid; grid-template-columns: repeat(5, 1fr); gap: 16px; }
.book-card { background:#fff; border-radius:8px; overflow:hidden; box-shadow:0 2px 12px rgba(0,0,0,.08); cursor:pointer; transition:transform .2s; }
.book-card:hover { transform:translateY(-3px); }
.cover { height:150px; background:#f0f2f5; display:flex; align-items:center; justify-content:center; font-size:40px; }
.info { padding:10px 12px 12px; }
.title { font-size:14px; font-weight:600; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.price-row { display:flex; justify-content:space-between; align-items:center; margin-top:8px; }
.price { color:#f56c6c; font-weight:bold; font-size:16px; }
.rate { color:#e6a23c; font-size:12px; }
.pager { margin-top: 18px; justify-content: flex-end; }
</style>
