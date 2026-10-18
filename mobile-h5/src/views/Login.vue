<template>
  <div class="page login-page">
    <van-nav-bar title="登录" />

    <div class="login-header rv" style="--i: 0">
      <span class="eyebrow">WELCOME BACK</span>
      <div class="logo">
        <van-icon name="shop-o" size="34" color="#fff" />
        <span class="logo-halo"></span>
      </div>
      <div class="welcome">欢迎来到 <em>Mall</em> 商城</div>
      <div class="sub">登录后即可享受完整购物体验</div>
    </div>

    <van-form @submit="handleLogin" class="login-form rv" style="--i: 1">
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

      <p class="login-hint">未注册的手机号将自动创建账号</p>
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
  min-height: 100vh;
}

.login-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 46px 0 34px;
}
.login-header .eyebrow {
  color: rgba(255, 46, 147, 0.7);
  margin-bottom: 18px;
}
.logo {
  position: relative;
  width: 78px;
  height: 78px;
  border-radius: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-image: linear-gradient(140deg, var(--neon), #ff5ea8 50%, var(--violet));
  box-shadow: var(--glow-neon), inset 0 1px 0 rgba(255, 255, 255, 0.35);
  animation: floatY 5.2s var(--ease-out) infinite;
}
.logo-halo {
  position: absolute;
  inset: -14px;
  border-radius: 34px;
  background: radial-gradient(circle, rgba(255, 46, 147, 0.42), transparent 68%);
  filter: blur(14px);
  z-index: -1;
}
.welcome {
  margin-top: 22px;
  font-family: var(--font-display);
  font-size: 22px;
  font-weight: 700;
  color: var(--text-1);
  letter-spacing: 0.04em;
}
.welcome em {
  font-style: normal;
  color: var(--neon);
  text-shadow: 0 0 20px rgba(255, 46, 147, 0.75);
}
.sub {
  font-size: 12px;
  color: var(--text-3);
  margin-top: 8px;
  letter-spacing: 0.06em;
}

.login-form {
  margin-top: 4px;
}
.login-btn-section {
  padding: 26px var(--gutter) 0;
}
.login-hint {
  text-align: center;
  font-size: 11px;
  color: var(--text-3);
  padding-top: 16px;
  letter-spacing: 0.04em;
}
</style>
