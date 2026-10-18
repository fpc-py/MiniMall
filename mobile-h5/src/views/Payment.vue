<template>
  <div class="page payment-page">
    <van-nav-bar title="支付订单" left-arrow fixed placeholder @click-left="handleBack" />

    <!-- 金额 -->
    <section class="amount-section rv" style="--i: 0">
      <span class="eyebrow">AMOUNT DUE</span>
      <div class="amount-val">
        <span class="sym">¥</span><span class="num">{{ amount }}</span>
      </div>
      <div class="order-no num">订单号 {{ orderNo || '-' }}</div>
    </section>

    <!-- 支付方式 -->
    <section class="pay-methods rv" style="--i: 1">
      <div class="pay-title">选择支付方式</div>

      <div
        class="pay-item"
        v-for="method in payMethods"
        :key="method.value"
        :class="{ active: selectedMethod === method.value }"
        @click="selectedMethod = method.value"
      >
        <div class="pay-item-left">
          <span class="pay-icon" :style="{ boxShadow: `0 0 18px -2px ${method.color}66` }">
            <van-icon :name="method.icon" :size="20" :color="method.color" />
          </span>
          <div class="pay-item-info">
            <span class="pay-item-name">{{ method.name }}</span>
            <span class="pay-item-desc">{{ method.desc }}</span>
          </div>
        </div>
        <span class="pay-radio" :class="{ checked: selectedMethod === method.value }"></span>
      </div>
    </section>

    <!-- 倒计时 -->
    <div class="timeout-info">
      <van-icon name="clock-o" color="#6d7490" size="14" />
      <span>请在 <em class="num">{{ timeLeft }}</em> 内完成支付</span>
    </div>

    <!-- 支付按钮 -->
    <div class="pay-btn-section">
      <van-button
        round
        block
        type="danger"
        color="#FF2E93"
        size="large"
        :loading="paying"
        :loading-text="paying ? '支付处理中...' : ''"
        @click="handlePay"
      >
        确认支付 ¥{{ amount }}
      </van-button>
    </div>

    <!-- 支付结果弹窗 -->
    <van-dialog
      v-model:show="showResult"
      :title="paySuccess ? '支付成功' : '支付失败'"
      :confirm-button-color="paySuccess ? '#FF2E93' : '#6d7490'"
      confirm-button-text="查看订单"
      @confirm="viewOrder"
      :closeable="false"
    >
      <div class="result-icon">
        <van-icon
          :name="paySuccess ? 'smile-o' : 'warning-o'"
          :size="48"
          :color="paySuccess ? '#34e39a' : '#ff4d6d'"
        />
      </div>
      <div class="result-msg">
        {{ paySuccess ? '您的订单已支付成功' : '支付未完成，请重试' }}
      </div>
    </van-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getOrderDetail } from '@/api/order'
import { createPay, payCallback } from '@/api/pay'

const route = useRoute()
const router = useRouter()

// 后端 Long 以字符串下发（雪花 ID 超出 JS 安全整数范围），此处必须保持字符串，禁止 Number() 转换
const orderId = route.query.id ? String(route.query.id) : null
const order = ref(null)

const selectedMethod = ref(3)
const paying = ref(false)
const showResult = ref(false)
const paySuccess = ref(false)

const payMethods = [
  { value: 3, icon: 'chat-o', name: '微信支付', desc: '推荐使用微信支付', color: '#34e39a' },
  { value: 2, icon: 'paid', name: '支付宝', desc: '支付宝安全支付', color: '#4d9bff' },
  { value: 1, icon: 'balance-o', name: '余额支付', desc: '账户余额支付', color: '#ff2e93' },
]

const amount = computed(() => Number(order.value?.payAmount || 0).toFixed(2))
const orderNo = computed(() => order.value?.orderNo || '')

// 倒计时
const totalSeconds = 15 * 60
const remainSeconds = ref(totalSeconds)
let timer = null

const timeLeft = computed(() => {
  const m = Math.floor(remainSeconds.value / 60)
  const s = remainSeconds.value % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
})

function startTimer() {
  timer = setInterval(() => {
    if (remainSeconds.value > 0) {
      remainSeconds.value--
    } else {
      clearInterval(timer)
      timer = null
      showToast('支付超时')
    }
  }, 1000)
}

async function loadOrder() {
  if (!orderId) {
    showToast('缺少订单信息')
    return
  }
  try {
    const res = await getOrderDetail(orderId)
    order.value = res.data
  } catch (e) {
    showToast(e.message || '订单加载失败')
  }
}

async function handlePay() {
  if (!orderId || paying.value) return
  if (!order.value) {
    showToast('订单加载中，请稍后')
    return
  }
  paying.value = true
  try {
    const res = await createPay({ orderId, payMethod: selectedMethod.value })
    const pay = res.data || {}
    if (pay.success === false) {
      showToast(pay.message || '创建支付失败')
      return
    }
    await payCallback({
      transactionNo: pay.transactionNo,
      status: 1,
      amount: Number(order.value.payAmount),
    })
    paySuccess.value = true
    showResult.value = true
  } catch (e) {
    paySuccess.value = false
    showResult.value = true
  } finally {
    paying.value = false
  }
}

function handleBack() {
  router.back()
}

function viewOrder() {
  showResult.value = false
  router.replace('/order/list')
}

onMounted(() => {
  loadOrder()
  startTimer()
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.payment-page {
  padding-bottom: 34px;
}

/* ---------- 金额区 ---------- */
.amount-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin: 0 var(--gutter);
  padding: 26px 16px 24px;
  border-radius: var(--r-lg);
  background-image: linear-gradient(158deg, rgba(255, 46, 147, 0.14), rgba(255, 255, 255, 0.02) 55%, rgba(36, 229, 255, 0.08));
  border: 1px solid var(--glass-line-2);
  backdrop-filter: blur(var(--blur)) saturate(150%);
  box-shadow: var(--shadow-card);
}
.amount-section .eyebrow {
  color: rgba(255, 46, 147, 0.75);
}
.amount-val {
  display: flex;
  align-items: baseline;
  gap: 3px;
  margin-top: 12px;
  color: var(--text-1);
}
.amount-val .sym {
  font-size: 22px;
  color: var(--neon);
}
.amount-val .num {
  font-family: var(--font-display);
  font-size: 46px;
  font-weight: 700;
  line-height: 1;
  letter-spacing: -0.01em;
  color: var(--neon);
  text-shadow: 0 0 28px rgba(255, 46, 147, 0.55);
}
.order-no {
  font-size: 11px;
  color: var(--text-3);
  margin-top: 12px;
  letter-spacing: 0.04em;
}

/* ---------- 支付方式 ---------- */
.pay-methods {
  margin: var(--sp-3) var(--gutter) 0;
  padding: 4px 16px 6px;
  border-radius: var(--r-lg);
  background-image: linear-gradient(158deg, rgba(255, 255, 255, 0.06), rgba(255, 255, 255, 0.015));
  border: 1px solid var(--glass-line);
  backdrop-filter: blur(var(--blur)) saturate(150%);
  box-shadow: var(--shadow-card);
}
.pay-title {
  font-family: var(--font-display);
  font-size: 14px;
  font-weight: 700;
  color: var(--text-1);
  letter-spacing: 0.06em;
  padding: 15px 0 6px;
}
.pay-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 13px 2px;
  border-top: 1px solid var(--glass-line);
  transition: background var(--dur-fast) var(--ease-out);
}
.pay-item.active {
  background-image: linear-gradient(90deg, rgba(255, 46, 147, 0.09), transparent 70%);
}
.pay-item-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.pay-icon {
  width: 38px;
  height: 38px;
  border-radius: var(--r-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--glass-2);
  border: 1px solid var(--glass-line);
}
.pay-item-info {
  display: flex;
  flex-direction: column;
}
.pay-item-name {
  font-size: 14px;
  color: var(--text-1);
  font-weight: 500;
}
.pay-item-desc {
  font-size: 11px;
  color: var(--text-3);
  margin-top: 3px;
}
/* 自绘单选圈：选中时霓虹发光 */
.pay-radio {
  position: relative;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  border: 1px solid var(--glass-line-2);
  flex-shrink: 0;
  transition: all var(--dur-fast) var(--ease-out);
}
.pay-radio.checked {
  border-color: var(--neon);
  box-shadow: 0 0 14px rgba(255, 46, 147, 0.7), inset 0 0 0 3px var(--ink-900);
  background: var(--neon);
}

.timeout-info {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  padding: 22px 16px;
  font-size: 12px;
  color: var(--text-3);
  letter-spacing: 0.04em;
}
.timeout-info em {
  font-style: normal;
  color: var(--neon-soft);
  font-weight: 700;
  font-size: 14px;
}

.pay-btn-section {
  padding: 0 var(--gutter);
}

.result-icon {
  text-align: center;
  padding: 12px 0 4px;
}
.result-msg {
  text-align: center;
  font-size: 13px;
  color: var(--text-2);
  padding-bottom: 12px;
}
</style>
