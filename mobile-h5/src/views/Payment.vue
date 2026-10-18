<template>
  <div class="payment-page">
    <van-nav-bar title="支付订单" left-arrow fixed placeholder @click-left="handleBack" />

    <!-- 金额 -->
    <div class="amount-section">
      <div class="amount-label">支付金额</div>
      <div class="amount-val">
        <span class="sym">¥</span>{{ amount }}
      </div>
      <div class="order-no">订单号：{{ orderNo || '-' }}</div>
    </div>

    <!-- 支付方式 -->
    <div class="pay-methods">
      <div class="pay-title">选择支付方式</div>

      <div
        class="pay-item"
        v-for="method in payMethods"
        :key="method.value"
        @click="selectedMethod = method.value"
      >
        <div class="pay-item-left">
          <van-icon :name="method.icon" :size="24" :color="method.color" />
          <div class="pay-item-info">
            <span class="pay-item-name">{{ method.name }}</span>
            <span class="pay-item-desc">{{ method.desc }}</span>
          </div>
        </div>
        <van-icon
          :name="selectedMethod === method.value ? 'success' : 'circle'"
          :color="selectedMethod === method.value ? '#FF2E93' : '#ddd'"
          size="22"
        />
      </div>
    </div>

    <!-- 倒计时 -->
    <div class="timeout-info">
      <van-icon name="clock-o" color="#999" />
      <span>请在 <em>{{ timeLeft }}</em> 内完成支付</span>
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
      :confirm-button-color="paySuccess ? '#FF2E93' : '#666'"
      confirm-button-text="查看订单"
      @confirm="viewOrder"
      :closeable="false"
    >
      <div class="result-icon">
        <van-icon
          :name="paySuccess ? 'smile-o' : 'warning-o'"
          :size="48"
          :color="paySuccess ? '#07c160' : '#ee0a24'"
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

const orderId = route.query.id ? Number(route.query.id) : null
const order = ref(null)

const selectedMethod = ref(3)
const paying = ref(false)
const showResult = ref(false)
const paySuccess = ref(false)

const payMethods = [
  { value: 3, icon: 'chat-o', name: '微信支付', desc: '推荐使用微信支付', color: '#07c160' },
  { value: 2, icon: 'paid', name: '支付宝', desc: '支付宝安全支付', color: '#1677ff' },
  { value: 1, icon: 'balance-o', name: '余额支付', desc: '账户余额支付', color: '#FF2E93' },
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
  background: #f5f5f5;
  min-height: 100vh;
  padding-bottom: 30px;
}

.amount-section {
  background: #fff;
  text-align: center;
  padding: 30px 16px 24px;
  margin-bottom: 10px;
}
.amount-label {
  font-size: 13px;
  color: #999;
  margin-bottom: 8px;
}
.amount-val {
  font-size: 40px;
  font-weight: 700;
  color: #222;
}
.amount-val .sym {
  font-size: 24px;
}
.order-no {
  font-size: 12px;
  color: #ccc;
  margin-top: 8px;
}

.pay-methods {
  background: #fff;
  padding: 0 16px;
  margin-bottom: 10px;
}
.pay-title {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  padding: 14px 0;
}
.pay-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 0;
  border-top: 1px solid #f5f5f5;
}
.pay-item-left {
  display: flex;
  align-items: center;
  gap: 10px;
}
.pay-item-info {
  display: flex;
  flex-direction: column;
}
.pay-item-name {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}
.pay-item-desc {
  font-size: 11px;
  color: #999;
  margin-top: 2px;
}

.timeout-info {
  text-align: center;
  padding: 20px 16px;
  font-size: 13px;
  color: #999;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}
.timeout-info em {
  font-style: normal;
  color: #FF2E93;
  font-weight: 600;
}

.pay-btn-section {
  padding: 0 16px;
}

.result-icon {
  text-align: center;
  padding: 10px 0;
}
.result-msg {
  text-align: center;
  font-size: 14px;
  color: #666;
  padding-bottom: 10px;
}
</style>
