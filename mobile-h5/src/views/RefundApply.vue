<template>
  <div class="page refund-apply-page">
    <van-nav-bar title="申请退款" left-arrow fixed placeholder @click-left="$router.back()" />

    <van-skeleton v-if="loadingOrder" title :row="3" style="padding: 16px" />

    <template v-else>
      <!-- 订单信息 -->
      <section class="order-block rv" style="--i: 0" v-if="order">
        <div class="order-header">
          <span class="order-no num">订单号 {{ order.orderNo }}</span>
          <span class="order-amount">
            退款金额 <em class="num">¥{{ money(order.payAmount) }}</em>
          </span>
        </div>
        <div class="order-goods">
          <div class="goods-item" v-for="item in order.items || []" :key="item.skuId">
            <div class="goods-img">
              <img v-if="item.productImage" :src="item.productImage" class="goods-img-el" alt="" />
              <van-icon v-else name="photo-o" :size="32" color="#6d7490" />
            </div>
            <div class="goods-info">
              <div class="goods-name van-multi-ellipsis--l2">{{ item.productName }}</div>
              <div class="goods-spec">{{ item.attrs }}</div>
            </div>
            <span class="goods-qty num">x{{ item.quantity }}</span>
          </div>
        </div>
      </section>

      <!-- 退款原因 -->
      <section class="section-block rv" style="--i: 1">
        <div class="section-title-block">
          <span class="eyebrow">REASON</span>
          <span class="section-title">退款原因</span>
        </div>
        <div class="reason-options">
          <span
            v-for="r in reasons"
            :key="r"
            class="reason-chip"
            :class="{ active: reason === r }"
            @click="reason = r"
          >{{ r }}</span>
        </div>
      </section>

      <!-- 补充说明 -->
      <section class="section-block rv" style="--i: 2">
        <div class="section-title-block">
          <span class="eyebrow">NOTE</span>
          <span class="section-title">补充说明</span>
        </div>
        <van-field
          v-model="description"
          type="textarea"
          rows="3"
          maxlength="512"
          show-word-limit
          placeholder="选填：补充退款说明"
        />
      </section>

      <div class="submit-section rv" style="--i: 3">
        <van-button
          block
          round
          type="danger"
          color="#FF2E93"
          :loading="submitting"
          loading-text="提交中..."
          @click="handleSubmit"
        >提交申请</van-button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getOrderDetail } from '@/api/order'
import { applyRefund } from '@/api/refund'

const route = useRoute()
const router = useRouter()

// 后端 Long 以字符串下发（雪花 ID 超出 JS 安全整数范围），此处必须保持字符串，禁止 Number() 转换
const orderId = route.query.orderId ? String(route.query.orderId) : null

const loadingOrder = ref(true)
const order = ref(null)
const submitting = ref(false)

const reason = ref('')
const description = ref('')

const reasons = [
  '不想要了',
  '拍错 / 多拍',
  '商品质量问题',
  '商品与描述不符',
  '收到商品破损',
  '其他原因',
]

function money(v) {
  return Number(v || 0).toFixed(2)
}

async function loadOrder() {
  if (!orderId) {
    showToast('缺少订单信息')
    loadingOrder.value = false
    return
  }
  loadingOrder.value = true
  try {
    const res = await getOrderDetail(orderId)
    order.value = res.data
  } catch (e) {
    showToast(e.message || '订单加载失败')
  } finally {
    loadingOrder.value = false
  }
}

async function handleSubmit() {
  if (!reason.value) {
    showToast('请选择退款原因')
    return
  }
  if (!orderId || submitting.value) return
  submitting.value = true
  try {
    await applyRefund({
      orderId,
      reason: reason.value,
      description: description.value || undefined,
    })
    showToast('退款申请已提交')
    router.replace('/refund/list')
  } catch (e) {
    showToast(e.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

onMounted(loadOrder)
</script>

<style scoped>
.refund-apply-page {
  padding-bottom: 34px;
}

/* ---------- 订单信息 ---------- */
.order-block {
  margin: 0 var(--gutter);
  padding: 15px 16px;
  border-radius: var(--r-lg);
  background-image: linear-gradient(158deg, rgba(255, 46, 147, 0.13), rgba(255, 255, 255, 0.02) 52%, rgba(36, 229, 255, 0.07));
  border: 1px solid var(--glass-line-2);
  backdrop-filter: blur(var(--blur)) saturate(150%);
  box-shadow: var(--shadow-card);
}
.order-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--glass-line);
}
.order-no {
  font-size: 11px;
  color: var(--text-3);
  letter-spacing: 0.04em;
}
.order-amount {
  font-size: 11px;
  color: var(--text-2);
  letter-spacing: 0.04em;
}
.order-amount em {
  font-style: normal;
  color: var(--neon);
  font-weight: 700;
  font-size: 17px;
  text-shadow: 0 0 16px rgba(255, 46, 147, 0.55);
}
.order-goods {
  padding-top: 8px;
}
.goods-item {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 9px 0;
}
.goods-img {
  width: 58px;
  height: 58px;
  border-radius: var(--r-sm);
  background: var(--glass);
  border: 1px solid var(--glass-line);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
}
.goods-img-el {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.goods-info {
  flex: 1;
  min-width: 0;
}
.goods-name {
  font-size: 13px;
  color: var(--text-1);
  line-height: 1.42;
}
.goods-spec {
  font-size: 11px;
  color: var(--text-3);
  margin-top: 3px;
}
.goods-qty {
  font-size: 12px;
  color: var(--text-3);
  flex-shrink: 0;
}

/* ---------- 区块 ---------- */
.section-block {
  margin: var(--sp-3) var(--gutter) 0;
  padding: 15px 16px;
  border-radius: var(--r-md);
  background-image: linear-gradient(158deg, rgba(255, 255, 255, 0.06), rgba(255, 255, 255, 0.015));
  border: 1px solid var(--glass-line);
  backdrop-filter: blur(var(--blur)) saturate(150%);
  box-shadow: var(--shadow-card);
}
.section-title-block {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 12px;
}
.section-title {
  font-family: var(--font-display);
  font-size: 15px;
  font-weight: 700;
  color: var(--text-1);
  letter-spacing: 0.05em;
}
.reason-options {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.reason-chip {
  padding: 7px 16px;
  border: 1px solid var(--glass-line);
  border-radius: var(--r-pill);
  font-size: 13px;
  color: var(--text-2);
  background: var(--glass);
  transition: all var(--dur-fast) var(--ease-out);
}
.reason-chip.active {
  border-color: rgba(255, 46, 147, 0.7);
  color: #fff;
  background-image: linear-gradient(135deg, rgba(255, 46, 147, 0.92), rgba(139, 92, 255, 0.82));
  box-shadow: var(--glow-neon);
}

.submit-section {
  padding: 22px var(--gutter) 0;
}
</style>
