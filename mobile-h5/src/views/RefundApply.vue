<template>
  <div class="refund-apply-page">
    <van-nav-bar title="申请退款" left-arrow fixed placeholder @click-left="$router.back()" />

    <van-skeleton v-if="loadingOrder" title :row="3" style="padding: 16px" />

    <template v-else>
      <!-- 订单信息 -->
      <div class="order-block" v-if="order">
        <div class="order-header">
          <span class="order-no">订单号：{{ order.orderNo }}</span>
          <span class="order-amount">
            退款金额 <em>¥{{ money(order.payAmount) }}</em>
          </span>
        </div>
        <div class="order-goods">
          <div class="goods-item" v-for="item in order.items || []" :key="item.skuId">
            <div class="goods-img">
              <img v-if="item.productImage" :src="item.productImage" class="goods-img-el" alt="" />
              <van-icon v-else name="photo-o" :size="32" color="#ccc" />
            </div>
            <div class="goods-info">
              <div class="goods-name van-multi-ellipsis--l2">{{ item.productName }}</div>
              <div class="goods-spec">{{ item.attrs }}</div>
            </div>
            <span class="goods-qty">x{{ item.quantity }}</span>
          </div>
        </div>
      </div>

      <!-- 退款原因 -->
      <div class="section-block">
        <div class="section-title">退款原因</div>
        <div class="reason-options">
          <span
            v-for="r in reasons"
            :key="r"
            class="reason-chip"
            :class="{ active: reason === r }"
            @click="reason = r"
          >{{ r }}</span>
        </div>
      </div>

      <!-- 补充说明 -->
      <div class="section-block">
        <div class="section-title">补充说明</div>
        <van-field
          v-model="description"
          type="textarea"
          rows="3"
          maxlength="512"
          show-word-limit
          placeholder="选填：补充退款说明"
        />
      </div>

      <div class="submit-section">
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

const orderId = route.query.orderId ? Number(route.query.orderId) : null

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
  background: #f5f5f5;
  min-height: 100vh;
  padding-bottom: 30px;
}

.order-block {
  background: #fff;
  padding: 14px 16px;
  margin-bottom: 10px;
}
.order-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 10px;
  border-bottom: 1px solid #f5f5f5;
}
.order-no {
  font-size: 12px;
  color: #999;
}
.order-amount {
  font-size: 12px;
  color: #666;
}
.order-amount em {
  font-style: normal;
  color: #FF2E93;
  font-weight: 700;
  font-size: 15px;
}
.order-goods {
  padding-top: 6px;
}
.goods-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
}
.goods-img {
  width: 56px;
  height: 56px;
  background: #fafafa;
  border-radius: 8px;
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
  color: #222;
  line-height: 1.4;
}
.goods-spec {
  font-size: 11px;
  color: #999;
  margin-top: 2px;
}
.goods-qty {
  font-size: 12px;
  color: #999;
  flex-shrink: 0;
}

.section-block {
  background: #fff;
  padding: 14px 16px;
  margin-bottom: 10px;
}
.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin-bottom: 10px;
}
.reason-options {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.reason-chip {
  padding: 6px 14px;
  border: 1px solid #eee;
  border-radius: 20px;
  font-size: 13px;
  color: #333;
  background: #fafafa;
}
.reason-chip.active {
  border-color: #FF2E93;
  color: #FF2E93;
  background: #fff5f8;
}

.submit-section {
  padding: 20px 16px;
}
</style>
