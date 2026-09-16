<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getBook } from '../../api/books'
import { getStockPile } from '../../api/stockpiles'
import { addProductToCart } from '../../api/cart'
import { getCommentsByBook, addComment, deleteComment, type Comment } from '../../api/comment'

const route = useRoute()
const router = useRouter()
const book = ref<any>(null)
const stock = ref(0)
const quantity = ref(1)
const loading = ref(true)

const comments = ref<Comment[]>([])
const newContent = ref('')
const newRate = ref(5)
const submitting = ref(false)
const currentUserId = Number(sessionStorage.getItem('id')) || null

onMounted(async () => {
  try {
    const id = Number(route.params.bookId)
    const [bookRes, invRes] = await Promise.all([
      getBook(id),
      getStockPile(id)
    ])
    book.value = bookRes.data?.data
    stock.value = invRes.data?.data || 0
    await loadComments()
  } catch {} finally { loading.value = false }
})

async function loadComments() {
  try {
    const res = await getCommentsByBook(Number(route.params.bookId))
    comments.value = res.data?.data || []
  } catch {}
}

async function submitComment() {
  if (!newContent.value.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }
  submitting.value = true
  try {
    await addComment({
      bookId: Number(route.params.bookId),
      content: newContent.value.trim(),
      rate: newRate.value
    })
    ElMessage.success('评论成功')
    newContent.value = ''
    newRate.value = 5
    await loadComments()
  } catch {} finally { submitting.value = false }
}

async function removeComment(comment: Comment) {
  await ElMessageBox.confirm('确定删除该评论？', '提示', { type: 'warning' })
  await deleteComment(comment.id!)
  ElMessage.success('已删除')
  await loadComments()
}

async function addToCart() {
  try {
    await addProductToCart({ bookId: book.value.id, quantity: quantity.value })
    ElMessage.success('已加入购物车')
  } catch {}
}

function buyNow() {
  router.push(`/checkout?bookId=${book.value.id}&quantity=${quantity.value}`)
}

function goAuthor() {
  if (!book.value?.author) return
  router.push(`/author/${encodeURIComponent(book.value.author)}`)
}
</script>

<template>
  <div class="detail-page" v-loading="loading">
    <div v-if="book" class="detail-content">
      <div class="cover-section">
        <div class="cover-placeholder">📖</div>
      </div>
      <div class="info-section">
        <h1>{{ book.title }}</h1>
        <div class="meta">
          <span>作者：<el-link type="primary" :underline="false" @click="goAuthor">{{ book.author }}</el-link></span>
          <span v-if="book.publisher">出版社：{{ book.publisher }}</span>
          <span v-if="book.isbn">ISBN：{{ book.isbn }}</span>
          <span v-if="book.publishDate">出版日期：{{ book.publishDate?.substring(0,10) }}</span>
        </div>
        <div class="rating" v-if="book.rate">⭐ {{ book.rate }} 分</div>
        <div class="price">¥{{ book.price }}</div>
        <div class="stock" v-if="stock > 0">库存 {{ stock }} 件</div>
        <div class="stock out" v-else>暂时缺货</div>
        <div class="sales">已售 {{ book.sales || 0 }} 件</div>

        <div class="actions">
          <el-input-number v-model="quantity" :min="1" :max="stock" size="large" />
          <el-button type="primary" size="large" @click="addToCart" :disabled="stock<=0">加入购物车</el-button>
          <el-button type="danger" size="large" @click="buyNow" :disabled="stock<=0">立即购买</el-button>
        </div>
      </div>
    </div>
    <div class="desc-section" v-if="book?.description">
      <h3>内容简介</h3>
      <p>{{ book.description }}</p>
    </div>

    <div class="comment-section" v-if="book">
      <h3>读者评论（{{ comments.length }}）</h3>
      <div class="comment-form">
        <el-rate v-model="newRate" />
        <el-input
          v-model="newContent"
          type="textarea"
          :rows="3"
          maxlength="1000"
          show-word-limit
          placeholder="写下你的读书感受..."
        />
        <el-button type="primary" :loading="submitting" @click="submitComment">发表评论</el-button>
      </div>

      <div class="comment-list">
        <div v-for="c in comments" :key="c.id" class="comment-item">
          <div class="comment-head">
            <b>{{ c.username || '匿名用户' }}</b>
            <el-rate :model-value="c.rate" disabled size="small" />
            <span class="comment-time">{{ c.createdAt }}</span>
            <el-button
              v-if="currentUserId && c.userId === currentUserId"
              link type="danger" size="small"
              @click="removeComment(c)"
            >删除</el-button>
          </div>
          <div class="comment-body">{{ c.content }}</div>
          <div v-for="r in c.replies" :key="r.id" class="comment-reply">
            <b>{{ r.username || '匿名用户' }}：</b>{{ r.content }}
          </div>
        </div>
        <el-empty v-if="!comments.length" description="还没有评论，快来抢沙发" />
      </div>
    </div>

    <el-empty v-if="!loading && !book" description="图书不存在" />
  </div>
</template>

<style scoped>
.detail-page { max-width: 1000px; margin: 0 auto; padding: 20px; }
.detail-content { display: flex; gap: 40px; margin-bottom: 30px; }
.cover-section { flex-shrink: 0; }
.cover-placeholder { width: 260px; height: 340px; background: #f0f2f5; display: flex; align-items: center; justify-content: center; font-size: 80px; border-radius: 8px; }
.info-section h1 { font-size: 24px; margin: 0 0 12px; }
.meta { display: flex; flex-wrap: wrap; gap: 16px; font-size: 14px; color: #666; margin-bottom: 12px; }
.rating { font-size: 16px; color: #e6a23c; margin-bottom: 12px; }
.price { font-size: 28px; color: #f56c6c; font-weight: bold; margin-bottom: 8px; }
.stock { color: #67c23a; margin-bottom: 4px; }
.stock.out { color: #f56c6c; }
.sales { font-size: 13px; color: #999; margin-bottom: 20px; }
.actions { display: flex; gap: 12px; align-items: center; }
.desc-section { background: #fff; padding: 20px; border-radius: 8px; margin-bottom: 20px; }
.desc-section h3 { margin: 0 0 12px; }
.desc-section p { color: #666; line-height: 1.8; }
.comment-section { background: #fff; padding: 20px; border-radius: 8px; }
.comment-section h3 { margin: 0 0 16px; }
.comment-form { display: flex; flex-direction: column; gap: 10px; align-items: flex-start; margin-bottom: 24px; }
.comment-item { padding: 12px 0; border-bottom: 1px solid #f0f0f0; }
.comment-head { display: flex; align-items: center; gap: 12px; }
.comment-time { font-size: 12px; color: #999; }
.comment-body { margin-top: 6px; color: #333; line-height: 1.7; }
.comment-reply { margin: 8px 0 0 24px; padding: 8px 12px; background: #f7f8fa; border-radius: 6px; font-size: 13px; color: #555; }
</style>
