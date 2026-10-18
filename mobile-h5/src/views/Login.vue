<template>
  <div class="login-page">
    <van-nav-bar title="登录" />

    <div class="login-header">
      <div class="logo">
        <van-icon name="shop-o" size="40" color="#fff" />
      </div>
      <div class="welcome">欢迎来到 Mall 商城</div>
      <div class="sub">登录后即可享受完整购物体验</div>
    </div>

    <van-form @submit="handleLogin" class="login-form">
      <van-cell-group inset>
        <van-field
          v-model="phone"
          type="tel"
          label="手机号"
          placeholder="请输入手机号"
          maxlength="11"
          :rules="[
            { required: true, message: '请输入手机号' },
            { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确' },
          ]"
        />
        <van-field
          v-model="code"
          label="验证码"
          placeholder="请输入验证码"
          maxlength="6"
          :rules="[{ required: true, message: '请输入验证码' }]"
        >
          <template #button>
            <van-button
              size="small"
              type="danger"
              color="#FF2E93"
              :disabled="counting"
              @click.stop="handleSendCode"
            >
              {{ counting ? countdown + 's后重发' : '发送验证码' }}
            </van-button>
          </template>
        </van-field>
      </van-cell-group>

      <div class="login-btn-section">
        <van-button
          round
          block
          type="danger"
          color="#FF2E93"
          native-type="submit"
          :loading="loading"
          loading-text="登录中..."
        >
          登录
        </van-button>
      </div>
    </van-form>
  </div>
</template>

<script setup>
import { ref, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { sendSmsCode, smsLogin } from '@/api/auth'
import { setToken, setUser } from '@/utils/auth'

const route = useRoute()
const router = useRouter()

const phone = ref('')
const code = ref('')
const loading = ref(false)

const counting = ref(false)
const countdown = ref(60)
let timer = null

function startCountdown() {
  counting.value = true
  countdown.value = 60
  timer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(timer)
      timer = null
      counting.value = false
    }
  }, 1000)
}

async function handleSendCode() {
  if (!/^1[3-9]\d{9}$/.test(phone.value)) {
    showToast('请输入正确的手机号')
    return
  }
  try {
    await sendSmsCode(phone.value)
    showToast('验证码已发送')
    startCountdown()
  } catch (e) {
    showToast(e.message || '发送失败')
  }
}

async function handleLogin() {
  if (!/^1[3-9]\d{9}$/.test(phone.value)) {
    showToast('请输入正确的手机号')
    return
  }
  loading.value = true
  try {
    const res = await smsLogin(phone.value, code.value)
    const data = res.data || {}
    setToken(data.token)
    setUser(data.userInfo || data)
    showToast('登录成功')
    router.replace(route.query.redirect || '/')
  } catch (e) {
    showToast(e.message || '登录失败')
  } finally {
    loading.value = false
  }
}

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.login-page {
  background: #f5f5f5;
  min-height: 100vh;
}

.login-header {
  padding: 40px 0 30px;
  text-align: center;
}
.logo {
  width: 72px;
  height: 72px;
  border-radius: 18px;
  background: linear-gradient(135deg, #FF2E93, #ff6b9d);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
}
.welcome {
  font-size: 20px;
  font-weight: 700;
  color: #222;
}
.sub {
  font-size: 13px;
  color: #999;
  margin-top: 6px;
}

.login-form {
  margin-top: 8px;
}
.login-btn-section {
  padding: 24px 24px 0;
}
</style>
