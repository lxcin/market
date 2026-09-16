<script setup lang="ts">
import { ElMessage } from "element-plus"
import { ref, computed } from 'vue'
import { router } from '../../router'
import { registerAccount } from "../../api/accounts"

const username = ref('')
const email = ref('')
const phone = ref('')
const password = ref('')
const confirmPassword = ref('')

const PHONE_RE = /^1[3-9]\d{9}$/

const hasUsernameInput = computed(() => username.value != '')
const hasEmailInput = computed(() => email.value != '')
const hasPhoneInput = computed(() => phone.value != '')
const hasPasswordInput = computed(() => password.value != '')
const hasConfirmPasswordInput = computed(() => confirmPassword.value != '')
const phoneValid = computed(() => PHONE_RE.test(phone.value))
const passwordsMatch = computed(() => password.value === confirmPassword.value)

const registerDisabled = computed(() =>
  !(hasUsernameInput.value && hasEmailInput.value && hasPhoneInput.value &&
    hasPasswordInput.value && hasConfirmPasswordInput.value &&
    phoneValid.value && passwordsMatch.value)
)

async function handleRegister() {
  if (!phoneValid.value) {
    ElMessage({ message: '手机号格式不正确', type: 'error', center: true })
    return
  }
  if (!passwordsMatch.value) {
    ElMessage({ message: '两次密码不一致', type: 'error', center: true })
    return
  }
  try {
    const res = await registerAccount({
      username: username.value,
      password: password.value,
      email: email.value,
      phone: phone.value
    })
    if (res.data.code === '200') {
      ElMessage({ message: '注册成功！', type: 'success', center: true })
      const data = res.data.data
      sessionStorage.setItem('token', data.token)
      sessionStorage.setItem('username', data.username)
      sessionStorage.setItem('role', data.role || 'USER')
      sessionStorage.setItem('id', data.userId)
      router.push('/')
    } else {
      ElMessage({ message: res.data.msg, type: 'error', center: true })
    }
  } catch (e) {}
}
</script>

<template>
  <el-main class="main-frame bgimage">
    <el-card class="login-card">
      <div>
        <h1>创建新账户</h1>
        <el-form>
          <el-form-item>
            <label>用户名</label>
            <el-input v-model="username" required placeholder="请输入用户名"/>
          </el-form-item>
          <el-form-item>
            <label>邮箱</label>
            <el-input v-model="email" required placeholder="请输入邮箱"/>
          </el-form-item>
          <el-form-item>
            <label>手机号</label>
            <el-input v-model="phone" required maxlength="11" placeholder="请输入11位手机号"/>
            <span v-if="hasPhoneInput && !phoneValid" style="color:red;font-size:12px">手机号格式不正确</span>
          </el-form-item>
          <el-form-item>
            <label>密码</label>
            <el-input v-model="password" type="password" required placeholder="••••••••"/>
          </el-form-item>
          <el-form-item>
            <label>确认密码</label>
            <el-input v-model="confirmPassword" type="password" required placeholder="••••••••"/>
            <span v-if="confirmPassword && !passwordsMatch" style="color:red;font-size:12px">两次密码不一致</span>
          </el-form-item>
          <span class="button-group">
            <el-button @click.prevent="handleRegister" :disabled="registerDisabled" type="primary">注册</el-button>
            <router-link to="/login" v-slot="{navigate}">
              <el-button @click="navigate">去登录</el-button>
            </router-link>
          </span>
        </el-form>
      </div>
    </el-card>
  </el-main>
</template>

<style scoped>
.main-frame {
  width: 100%; height: 100%;
  display: flex; align-items: center; justify-content: center;
}
.bgimage {
  background-image: url("../../assets/shopping-1s-1084px.svg");
}
.login-card {
  width: 60%; padding: 10px;
}
.button-group {
  padding-top: 10px; display: flex; flex-direction: row;
  gap: 30px; align-items: center; justify-content: right;
}
</style>
