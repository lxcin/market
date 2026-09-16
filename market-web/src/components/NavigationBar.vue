<script setup>
import {ElMenu, ElMenuItem, ElMessage, ElMessageBox} from "element-plus";
import {router} from "../router/index";
import {ref, computed } from "vue";
import {House, ShoppingCart, Notebook, User, Search} from '@element-plus/icons-vue';
import { logout as logoutApi } from "../api/accounts";
import { getSuggest } from "../api/books";


const role = ref(sessionStorage.getItem("role") || '')

function logout() {
  ElMessageBox.confirm(
      '是否要退出登录？',
      '提示',
      {
        customClass: "customDialog",
        confirmButtonText: '是',
        cancelButtonText: '否',
        type: "warning",
        showClose: false,
        roundButton: true,
        center: true
      }
  ).then(async () => {
    try { await logoutApi() } catch (e) {}
    sessionStorage.setItem('token', '')
    sessionStorage.setItem('user', '')
    sessionStorage.setItem('role', '')
    sessionStorage.setItem('id', '')
    sessionStorage.setItem('username', '')

    router.push({path: "/login"})
  })
}

const searchQuery = ref('')
const searchField = ref('all')
const searchPlaceholder = computed(() => {
  if (searchField.value === 'title') return '请输入书名'
  if (searchField.value === 'author') return '请输入作者'
  return '请输入书名或作者'
})
function performSearch(){
  if (!searchQuery.value.trim()) return
  router.push({ path: "/search", query: { keyword: searchQuery.value.trim(), field: searchField.value } })
  searchQuery.value = ''
}

async function fetchSuggestions(queryString, cb){
  const q = (queryString || '').trim()
  if (!q) { cb([]); return }
  try {
    const res = await getSuggest(q, 8)
    const list = (res.data?.data || []).map((s) => ({ value: s }))
    cb(list)
  } catch (e) {
    cb([])
  }
}
function onSelect(item){
  searchQuery.value = item.value
  performSearch()
}

// 计算属性：判断是否是搜索页
const isSearchPage = ref(false)
// 监听路由变化
router.afterEach((to) => {
  isSearchPage.value = to.path === '/search'
})

function gotoManger() {
  router.push({path: '/admin'})
}

</script>

<template>
  <el-header>
  <div class="nav-bar-container">
    <el-button v-if="role === 'ADMIN'" type="primary" @click="gotoManger()">管理员界面</el-button>
    <el-menu
        class="nav-bar"
        mode="horizontal"
        :router="true"
    >
      <el-menu-item index="/recommendation"><el-icon><House/></el-icon>首页</el-menu-item>
      <el-menu-item index="/cart"><el-icon><ShoppingCart /></el-icon>购物车</el-menu-item>
      <el-menu-item index="/dashboard"><el-icon><User /></el-icon>个人主页</el-menu-item>
    </el-menu>

    <div class="search-group" v-if="!isSearchPage">
      <el-select v-model="searchField" class="scope-select">
        <el-option label="全部" value="all" />
        <el-option label="书名" value="title" />
        <el-option label="作者" value="author" />
      </el-select>
      <el-autocomplete
          v-model="searchQuery"
          :fetch-suggestions="fetchSuggestions"
          :placeholder="searchPlaceholder"
          clearable
          class="custom-input"
          @keyup.enter="performSearch"
          @select="onSelect"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-autocomplete>
      <el-button
          type="primary"
          class="search-button"
          @click="performSearch"
      >
        搜索
      </el-button>
    </div>
  </div>
  </el-header>
</template>

<style scoped>
.search-group {
  display: flex;
  align-items: center;
  justify-content: center;
  max-width: 520px;
}

.scope-select {
  width: 92px;
  flex-shrink: 0;
  margin-right: -1px;
}
.scope-select :deep(.el-select__wrapper) {
  border-top-right-radius: 0;
  border-bottom-right-radius: 0;
  box-shadow: 0 0 0 1px #71a2d3 inset;
}

.custom-input {
  flex: 1;
  margin: 0;
}
.custom-input :deep(.el-input__wrapper) {
  border-top-left-radius: 0;
  border-bottom-left-radius: 0;
}

/* 修改输入框样式 */
.custom-input ::v-deep .el-input__inner {
  border-radius: 8px 0 0 8px !important;
  border-right: none;
  height: 30px;
  font-size: 14px;
}

/* 按钮样式 */
.search-button {
  margin-left: -1px; /* 紧贴输入框 */
  border-top-left-radius: 0;
  border-bottom-left-radius: 0;
  background-color: #72a3d5;
  color: white;
  border: 1px solid #71a2d3;
  height: 30px;
  padding: 0 16px;
  font-size: 14px;
}
.nav-bar-container {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  z-index: 1000;
  height: 60px;
  display: flex;
  flex-direction: row;
  justify-content: center; /* 水平居中 */
  align-items: center; /* 垂直居中 */
  background-color: #fff;
}

.nav-bar {
  display: flex;
  justify-content: center; /* 水平居中 */
  align-items: center; /* 垂直居中 */
  width: 600px;
  height: 100%;
}
</style>
