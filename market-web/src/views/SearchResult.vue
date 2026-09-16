<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { searchRich } from '../api/books'

const route = useRoute()
const router = useRouter()

const keyword = ref(String(route.query.keyword || ''))
const field = ref(String(route.query.field || 'all'))
const sort = ref('')
const categoryName = ref<string | undefined>(undefined)
const priceRange = ref<string | undefined>(undefined)
const page = ref(1)
const size = 12
const total = ref(0)
const items = ref<any[]>([])
const facets = ref<{ categories: any[]; prices: any[] }>({ categories: [], prices: [] })
const loading = ref(false)

function priceBounds(key: string): { minPrice?: number; maxPrice?: number } {
  if (key === '200+') return { minPrice: 200 }
  const [a, b] = key.split('-').map(Number)
  return { minPrice: a, maxPrice: b }
}

async function doSearch() {
  if (!keyword.value.trim()) { items.value = []; total.value = 0; return }
  loading.value = true
  try {
    const params: any = { keyword: keyword.value.trim(), field: field.value, sort: sort.value || undefined, page: page.value, size }
    if (categoryName.value) params.categoryName = categoryName.value
    if (priceRange.value) Object.assign(params, priceBounds(priceRange.value))
    const res = await searchRich(params)
    const d = res.data?.data || {}
    items.value = d.items || []
    total.value = d.total || 0
    facets.value = d.facets || { categories: [], prices: [] }
  } finally { loading.value = false }
}

function applyFacet() { page.value = 1; doSearch() }
function clearFilters() {
  categoryName.value = undefined; priceRange.value = undefined; sort.value = ''
  page.value = 1; doSearch()
}
function goDetail(id: number) { router.push(`/bookdetail/${id}`) }
/** 点击作者：进入作者主页 */
function goAuthor(name: string) {
  if (!name) return
  router.push(`/author/${encodeURIComponent(name)}`)
}

onMounted(doSearch)
watch(() => route.query.keyword, (v) => {
  keyword.value = String(v || ''); field.value = String(route.query.field || 'all'); page.value = 1; doSearch()
})
</script>

<template>
  <div class="search-page" v-loading="loading">
    <div class="head">
      <h2>搜索“{{ keyword }}” <span class="total">共 {{ total }} 条</span></h2>
      <div class="toolbar">
        <el-select v-model="field" size="small" style="width:100px" @change="applyFacet">
          <el-option label="全部" value="all" />
          <el-option label="书名" value="title" />
          <el-option label="作者" value="author" />
        </el-select>
        <el-select v-model="sort" size="small" style="width:120px" placeholder="综合排序" clearable @change="applyFacet">
          <el-option label="销量" value="sales" />
          <el-option label="评分" value="rate" />
          <el-option label="价格从低到高" value="price_asc" />
          <el-option label="价格从高到低" value="price_desc" />
        </el-select>
        <el-button v-if="categoryName || priceRange || sort" size="small" @click="clearFilters">清空筛选</el-button>
      </div>
    </div>

    <div class="body">
      <aside class="facets">
        <div class="facet-group" v-if="facets.categories.length">
          <div class="facet-title">分类</div>
          <div class="facet-item" v-for="c in facets.categories" :key="c.categoryName">
            <el-link :underline="false" :style="categoryName === c.categoryName ? 'color:#409eff;font-weight:600' : ''"
                     @click="categoryName = (categoryName === c.categoryName ? undefined : c.categoryName); applyFacet()">
              {{ c.categoryName }}
            </el-link>
            <span class="facet-count">{{ c.count }}</span>
          </div>
        </div>
        <div class="facet-group" v-if="facets.prices.length">
          <div class="facet-title">价格</div>
          <div class="facet-item" v-for="p in facets.prices" :key="p.key">
            <el-link :underline="false" :style="priceRange === p.key ? 'color:#409eff;font-weight:600' : ''"
                     @click="priceRange = (priceRange === p.key ? undefined : p.key); applyFacet()">
              ¥{{ p.key }}
            </el-link>
            <span class="facet-count">{{ p.count }}</span>
          </div>
        </div>
      </aside>

      <main class="results">
        <el-empty v-if="!loading && !items.length" description="没有找到相关图书" />
        <div class="book-grid">
          <div class="book-card" v-for="it in items" :key="it.book.id" @click="goDetail(it.book.id)">
            <div class="cover">
              <el-image v-if="it.book.coverImage" :src="it.book.coverImage" fit="cover" style="width:100%;height:100%" />
              <span v-else>📖</span>
            </div>
            <div class="info">
              <div class="title" v-html="it.highlight?.title?.[0] || it.book.title"></div>
              <div class="author">
                <el-tooltip content="查看作者主页" placement="top">
                  <span class="author-name" @click.stop="goAuthor(it.book.author)"
                        v-html="it.highlight?.author?.[0] || it.book.author"></span>
                </el-tooltip>
              </div>
              <div class="price-row">
                <span class="price">¥{{ it.book.price }}</span>
                <span class="rate">⭐{{ it.book.rate }}</span>
              </div>
            </div>
          </div>
        </div>
        <el-pagination
          v-if="total > size"
          class="pager" background layout="prev, pager, next"
          :total="total" :page-size="size" :current-page="page"
          @current-change="(p:number) => { page = p; doSearch() }"
        />
      </main>
    </div>
  </div>
</template>

<style scoped>
.search-page { max-width: 1150px; margin: 0 auto; padding: 20px 15px; }
.head { display: flex; justify-content: space-between; align-items: center; }
h2 { font-size: 20px; }
.total { font-size: 13px; color: #909399; font-weight: normal; margin-left: 8px; }
.toolbar { display: flex; gap: 10px; align-items: center; }
.body { display: flex; gap: 20px; margin-top: 16px; }
.facets { width: 200px; flex-shrink: 0; }
.facet-group { margin-bottom: 18px; border-bottom: 1px solid #f0f0f0; padding-bottom: 10px; }
.facet-title { font-weight: 600; margin-bottom: 8px; }
.facet-item { display: flex; justify-content: space-between; align-items: center; padding: 3px 0; font-size: 13px; }
.facet-count { color: #c0c4cc; font-size: 12px; }
.results { flex: 1; }
.book-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.book-card { background:#fff; border-radius:8px; overflow:hidden; box-shadow:0 2px 12px rgba(0,0,0,.08); cursor:pointer; transition:transform .2s; }
.book-card:hover { transform:translateY(-3px); }
.cover { height:150px; background:#f0f2f5; display:flex; align-items:center; justify-content:center; font-size:40px; }
.info { padding:10px 12px 12px; }
.title { font-size:14px; font-weight:600; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.title :deep(em), .author :deep(em) { color:#f56c6c; font-style: normal; }
.author { font-size:12px; color:#999; margin:3px 0; display:flex; align-items:center; gap:6px; }
.author-name { cursor: pointer; color:#409eff; }
.author-name:hover { text-decoration: underline; }
.author-name :deep(em) { color:#f56c6c; font-style: normal; }
.price-row { display:flex; justify-content:space-between; align-items:center; margin-top:8px; }
.price { color:#f56c6c; font-weight:bold; font-size:16px; }
.rate { color:#e6a23c; font-size:12px; }
.pager { margin-top: 18px; justify-content: flex-end; }
</style>
