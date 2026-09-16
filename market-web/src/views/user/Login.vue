<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { ref, computed, onMounted } from 'vue'
import { router } from '../../router'
import { loginAccount, getCaptcha } from '../../api/accounts'

const username = ref('')
const password = ref('')
const captchaCode = ref('')
const captchaId = ref('')
const captchaImg = ref('')

const hasUsernameInput = computed(() => username.value != '')
const hasPasswordInput = computed(() => password.value != '')
const hasCaptchaInput = computed(() => captchaCode.value != '')
const loginDisabled = computed(() => {
  return !(hasUsernameInput.value && hasPasswordInput.value && hasCaptchaInput.value)
})

async function loadCaptcha() {
  try {
    const res = await getCaptcha()
    captchaId.value = res.data?.data?.captchaId || ''
    captchaImg.value = res.data?.data?.image || ''
    captchaCode.value = ''
  } catch {}
}

async function handleLogin() {
  try {
    const res = await loginAccount({
      username: username.value,
      password: password.value,
      captchaId: captchaId.value,
      captchaCode: captchaCode.value
    })
    if (res.data.code === '200') {
      ElMessage({ message: '登录成功！', type: 'success', center: true })
      const data = res.data.data
      sessionStorage.setItem('token', data.token)
      sessionStorage.setItem('username', data.username)
      sessionStorage.setItem('role', data.role)
      sessionStorage.setItem('id', data.userId)
      router.push('/')
    } else {
      ElMessage({ message: res.data.msg, type: 'error', center: true })
      password.value = ''
      loadCaptcha()
    }
  } catch (e) {
    password.value = ''
    loadCaptcha()
  }
}

onMounted(loadCaptcha)
</script>

<template>
  <el-main class="main-frame bgimage">
    <el-card class="login-card">
      <div>
        <h1>登入您的账户</h1>
        <el-form>
          <el-form-item>
            <label for="username">用户名 / 手机号</label>
            <el-input id="username" type="text" v-model="username"
                      required
                      placeholder="请输入用户名或手机号"/>
          </el-form-item>

          <el-form-item>
            <label for="password">账户密码</label>
            <el-input id="password" type="password" v-model="password"
                      required
                      placeholder="••••••••"/>
          </el-form-item>

          <el-form-item>
            <label for="captcha">验证码</label>
            <div class="captcha-row">
              <el-input id="captcha" v-model="captchaCode" required
                        placeholder="请输入验证码" @keyup.enter="handleLogin"/>
              <img v-if="captchaImg" :src="captchaImg" class="captcha-img"
                   title="点击刷新" @click="loadCaptcha"/>
              <el-button v-else text @click="loadCaptcha">获取验证码</el-button>
            </div>
          </el-form-item>

          <div class="extra-row">
            <router-link to="/forgot">忘记密码？</router-link>
          </div>

          <span class="button-group">
              <el-button @click.prevent="handleLogin" :disabled="loginDisabled"
                         type="primary">登入</el-button>
              <router-link to="/register" v-slot="{navigate}">
                <el-button @click="navigate">去注册</el-button>
              </router-link>
          </span>
        </el-form>
      </div>
    </el-card>
  </el-main>
</template>

<style scoped>
.main-frame {
  width: 100%;
  height: 100%;

  display: flex;
  align-items: center;
  justify-content: center;
}

.bgimage {
  background-image: url("../../assets/shopping-1s-1084px.svg");
}

.login-card {
  width: 60%;
  padding: 10px;
}

.captcha-row {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}

.captcha-img {
  height: 40px;
  width: 120px;
  cursor: pointer;
  border-radius: 4px;
  border: 1px solid #dcdfe6;
}

.extra-row {
  text-align: right;
  margin-bottom: 6px;
}

.button-group {
  padding-top: 10px;
  display: flex;
  flex-direction: row;
  gap: 30px;
  align-items: center;
  justify-content: right;
}
</style>
