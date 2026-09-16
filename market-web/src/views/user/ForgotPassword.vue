<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { router } from '../../router'
import { sendSmsCode, resetPassword } from '../../api/accounts'

const PHONE_RE = /^1[3-9]\d{9}$/

const phone = ref('')
const code = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const countdown = ref(0)
const sending = ref(false)
const submitting = ref(false)
let timer: number | undefined

const phoneValid = computed(() => PHONE_RE.test(phone.value))
const passwordsMatch = computed(() => newPassword.value !== '' && newPassword.value === confirmPassword.value)
const canSubmit = computed(() =>
  phoneValid.value && code.value.length === 6 && newPassword.value.length >= 6 && passwordsMatch.value
)

async function sendCode() {
  if (!phoneValid.value) {
    ElMessage.warning('请输入正确的手机号')
    return
  }
  sending.value = true
  try {
    const res = await sendSmsCode(phone.value)
    const data = res.data?.data || {}
    if (data.mock && data.code) {
      ElMessage({ message: `演示环境验证码：${data.code}`, type: 'success', duration: 8000 })
    } else {
      ElMessage.success('验证码已发送')
    }
    countdown.value = 60
    timer = window.setInterval(() => {
      countdown.value--
      if (countdown.value <= 0 && timer) { clearInterval(timer); timer = undefined }
    }, 1000)
  } catch {} finally { sending.value = false }
}

async function submit() {
  if (!canSubmit.value) return
  submitting.value = true
  try {
    await resetPassword({ phone: phone.value, code: code.value, newPassword: newPassword.value })
    ElMessage.success('密码重置成功，请使用新密码登录')
    router.push('/login')
  } catch {} finally { submitting.value = false }
}
</script>

<template>
  <el-main class="main-frame bgimage">
    <el-card class="card">
      <h1>找回密码</h1>
      <p class="tip">通过注册手机号 + 短信验证码重置密码</p>
      <el-form>
        <el-form-item>
          <label>手机号</label>
          <el-input v-model="phone" maxlength="11" placeholder="请输入注册手机号" />
          <span v-if="phone && !phoneValid" style="color:red;font-size:12px">手机号格式不正确</span>
        </el-form-item>
        <el-form-item>
          <label>验证码</label>
          <div class="code-row">
            <el-input v-model="code" maxlength="6" placeholder="请输入6位验证码" />
            <el-button :disabled="countdown > 0 || sending" @click="sendCode">
              {{ countdown > 0 ? `${countdown}s 后重发` : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item>
          <label>新密码</label>
          <el-input v-model="newPassword" type="password" placeholder="6-32位新密码" />
        </el-form-item>
        <el-form-item>
          <label>确认密码</label>
          <el-input v-model="confirmPassword" type="password" placeholder="再次输入新密码" />
          <span v-if="confirmPassword && !passwordsMatch" style="color:red;font-size:12px">两次密码不一致</span>
        </el-form-item>
        <span class="button-group">
          <el-button type="primary" :disabled="!canSubmit" :loading="submitting" @click="submit">重置密码</el-button>
          <el-button @click="router.push('/login')">返回登录</el-button>
        </span>
      </el-form>
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
.card {
  width: 55%; padding: 10px;
}
h1 { margin-bottom: 4px; }
.tip { color: #909399; font-size: 13px; margin-top: 0; }
.code-row { display: flex; gap: 10px; width: 100%; }
.button-group {
  padding-top: 10px; display: flex; flex-direction: row;
  gap: 20px; align-items: center; justify-content: flex-end;
}
</style>
