<template>
  <div class="page order-detail-page">
    <van-nav-bar title="订单详情" left-arrow fixed placeholder @click-left="router.back()" />

    <van-skeleton v-if="loading" title :row="6" style="padding: 16px" />

    <template v-else-if="order">
      <!-- 状态 -->
      <section class="status-section rv" style="--i: 0">
        <span class="eyebrow status-eyebrow">ORDER STATUS</span>
        <div class="status-text">{{ statusText(order.orderStatus) }}</div>
        <div class="status-desc">{{ statusDesc(order.orderStatus) }}</div>
      </section>

      <!-- 收货信息 -->
      <section class="block rv" style="--i: 1">
        <div class="receiver-row">
          <span class="receiver-name">{{ order.receiverName }}</span>
          <span class="receiver-phone num">{{ order.receiverPhone }}</span>
        </div>
        <div class="receiver-address">{{ order.receiverAddress }}</div>
      </section>

      <!-- 快递信息 -->
      <section class="block rv" style="--i: 2" v-if="order.deliveryCompany || order.deliveryNo">
        <div class="block-title">物流信息</div>
        <div class="kv-row">
          <span class="kv-label">快递公司</span>
          <span class="kv-value">{{ order.deliveryCompany || '-' }}</span>
        </div>
        <div class="kv-row">
          <span class="kv-label">快递单号</span>
          <span class="kv-value num">{{ order.deliveryNo || '-' }}</span>
        </div>
      </section>

      <!-- 商品项 -->
      <section class="block goods-block rv" style="--i: 3">
        <div class="goods-item" v-for="item in order.items || []" :key="item.skuId">
          <div class="goods-img">
            <img v-if="item.productImage" :src="item.productImage" class="goods-img-el" alt="" />
            <van-icon v-else name="photo-o" :size="36" color="#6d7490" />
          </div>
          <div class="goods-info">
            <div class="goods-name van-multi-ellipsis--l2">{{ item.productName }}</div>
            <div class="goods-spec">{{ item.attrs }}</div>
            <div class="goods-bottom">
              <span class="goods-price"><span class="sym">¥</span><span class="num">{{ money(item.price) }}</span></span>
              <span class="goods-qty num">x{{ item.quantity }}</span>
            </div>
          </div>
        </div>
      </section>

      <!-- 金额明细 -->
      <section class="block rv" style="--i: 4">
        <div class="block-title">金额明细</div>
        <div class="kv-row">
          <span class="kv-label">商品总额</span>
          <span class="kv-value num">¥{{ money(order.totalAmount) }}</span>
        </div>
        <div class="kv-row">
          <span class="kv-label">运费</span>
          <span class="kv-value num">{{ Number(order.freightAmount || 0) > 0 ? '¥' + money(order.freightAmount) : '免运费' }}</span>
        </div>
        <div class="kv-row">
          <span class="kv-label">优惠</span>
          <span class="kv-value discount num">-¥{{ money(order.discountAmount) }}</span>
        </div>
        <div class="kv-row">
          <span class="kv-label">实付金额</span>
          <span class="kv-value pay num">¥{{ money(order.payAmount) }}</span>
        </div>
      </section>

      <!-- 订单信息 -->
      <section class="block rv" style="--i: 5">
        <div class="block-title">订单信息</div>
        <div class="kv-row">
          <span class="kv-label">订单编号</span>
          <span class="kv-value num">{{ order.orderNo }}</span>
        </div>
        <div class="kv-row" v-if="order.memberRemark">
          <span class="kv-label">买家备注</span>
          <span class="kv-value">{{ order.memberRemark }}</span>
        </div>
        <div class="kv-row" v-if="order.cancelReason">
          <span class="kv-label">取消原因</span>
          <span class="kv-value">{{ order.cancelReason }}</span>
        </div>
      </section>

      <!-- 时间线 -->
      <section class="block rv" style="--i: 6">
        <div class="block-title">订单进度</div>
        <div class="timeline">
          <div class="tl-item" v-for="t in timeline" :key="t.label">
            <span class="tl-dot"></span>
            <span class="kv-label">{{ t.label }}</span>
            <span class="kv-value num">{{ t.value }}</span>
          </div>
        </div>
      </section>

      <!-- 底部操作 -->
      <div class="bottom-bar" v-if="showActions">
        <van-button
          v-if="order.orderStatus === 10"
          round
          type="danger"
          color="#FF2E93"
          @click="goPay"
        >去支付</van-button>
        <van-button
          v-if="order.orderStatus === 10"
          round
          plain
          color="#999"
          @click="handleCancel"
        >取消订单</van-button>
        <van-button
          v-if="order.payStatus === 1"
          round
          plain
          color="#FF2E93"
          @click="goRefund"
        >申请退款</van-button>
        <van-button
          v-if="order.orderStatus === 30"
          round
          type="danger"
          color="#FF2E93"
          @click="handleReceipt"
        >确认收货</van-button>
      </div>
    </template>

    <van-empty v-else description="订单不存在" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import { getOrderDetail, cancelOrder, confirmReceipt } from '@/api/order'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const order = ref(null)

function money(v) {
  return Number(v || 0).toFixed(2)
}

function statusText(status) {
  const map = { 10: '待付款', 20: '待发货', 30: '待收货', 40: '已完成', 5: '已取消' }
  return map[status] || '未知'
}

function statusDesc(status) {
  const map = {
    10: '请尽快完成支付',
    20: '商家正在打包，请耐心等待',
    30: '商品已发出，请注意查收',
    40: '订单已完成，感谢您的购买',
    5: '订单已取消',
  }
  return map[status] || ''
}

const timeline = computed(() => {
  const o = order.value || {}
  const list = [
    { label: '下单时间', value: o.createTime },
    { label: '支付时间', value: o.paymentTime },
    { label: '发货时间', value: o.deliveryTime },
    { label: '收货时间', value: o.receiveTime },
    { label: '完成时间', value: o.finishTime },
    { label: '取消时间', value: o.cancelTime },
  ]
  return list.filter((t) => t.value)
})

const showActions = computed(() => {
  const o = order.value
  if (!o) return false
  return o.orderStatus === 10 || o.orderStatus === 30 || o.payStatus === 1
})

async function loadOrder() {
  loading.value = true
  try {
    const res = await getOrderDetail(route.params.id)
    order.value = res.data
  } catch (e) {
    showToast(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function goPay() {
  router.push(`/order/payment?id=${order.value.id}`)
}

function goRefund() {
  router.push(`/refund/apply?orderId=${order.value.id}`)
}

function handleCancel() {
  showConfirmDialog({
    title: '取消订单',
    message: '确定要取消该订单吗？',
    confirmButtonColor: '#FF2E93',
  })
    .then(async () => {
      try {
        await cancelOrder(order.value.id, '买家取消')
        showToast('已取消')
        await loadOrder()
      } catch (e) {
        showToast(e.message || '取消失败')
      }
    })
    .catch(() => {})
}

function handleReceipt() {
  showConfirmDialog({
    title: '确认收货',
    message: '是否确认已收到商品？',
    confirmButtonColor: '#FF2E93',
  })
    .then(async () => {
      try {
        await confirmReceipt(order.value.id)
        showToast('已确认收货')
        await loadOrder()
      } catch (e) {
        showToast(e.message || '操作失败')
      }
    })
    .catch(() => {})
}

onMounted(loadOrder)
</script>

<style scoped>
.order-detail-page {
  padding-bottom: 78px;
}

/* ---------- 状态区：霓虹渐变横带 ---------- */
.status-section {
  position: relative;
  margin: 0 var(--gutter);
  padding: 22px 18px 24px;
  border-radius: var(--r-lg);
  overflow: hidden;
  background-image: linear-gradient(135deg, rgba(255, 46, 147, 0.95), rgba(139, 92, 255, 0.85));
  box-shadow: var(--glow-neon), var(--shadow-card);
  border: 1px solid var(--glass-line-2);
}
/* 斜向扫光 */
.status-section::after {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  width: 46%;
  height: 100%;
  background: linear-gradient(100deg, transparent, rgba(255, 255, 255, 0.24), transparent);
  animation: sheen 5.4s var(--ease-out) infinite;
  pointer-events: none;
}
.status-eyebrow {
  color: rgba(255, 255, 255, 0.72);
}
.status-text {
  font-family: var(--font-display);
  font-size: 25px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 0.03em;
  margin-top: 7px;
  text-shadow: 0 6px 24px rgba(0, 0, 0, 0.35);
}
.status-desc {
  font-size: 12px;
  margin-top: 7px;
  color: rgba(255, 255, 255, 0.82);
  letter-spacing: 0.04em;
}

/* ---------- 通用玻璃区块 ---------- */
.block {
  margin: var(--sp-3) var(--gutter) 0;
  padding: 15px 16px;
  border-radius: var(--r-md);
  background-image: linear-gradient(158deg, rgba(255, 255, 255, 0.06), rgba(255, 255, 255, 0.015));
  border: 1px solid var(--glass-line);
  backdrop-filter: blur(var(--blur)) saturate(150%);
  box-shadow: var(--shadow-card);
}
.block-title {
  position: relative;
  font-family: var(--font-display);
  font-size: 14px;
  font-weight: 700;
  color: var(--text-1);
  letter-spacing: 0.06em;
  margin-bottom: 12px;
  padding-left: 10px;
}
.block-title::before {
  content: "";
  position: absolute;
  left: 0;
  top: 50%;
  width: 3px;
  height: 14px;
  transform: translateY(-50%);
  border-radius: var(--r-pill);
  background: linear-gradient(180deg, var(--neon), var(--cyan));
  box-shadow: 0 0 10px rgba(255, 46, 147, 0.9);
}
.receiver-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 7px;
}
.receiver-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-1);
}
.receiver-phone {
  font-size: 13px;
  color: var(--text-2);
}
.receiver-address {
  font-size: 12px;
  color: var(--text-3);
  line-height: 1.6;
}

.goods-block {
  padding: 0 16px;
}
.goods-item {
  display: flex;
  gap: 11px;
  padding: 13px 0;
  border-bottom: 1px solid var(--glass-line);
}
.goods-item:last-child {
  border-bottom: none;
}
.goods-img {
  width: 72px;
  height: 72px;
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
.goods-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 7px;
}
.goods-price {
  display: flex;
  align-items: baseline;
  gap: 1px;
  color: var(--neon);
  font-weight: 700;
}
.goods-price .sym {
  font-size: 11px;
}
.goods-price .num {
  font-size: 16px;
}
.goods-qty {
  font-size: 12px;
  color: var(--text-3);
}

.kv-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 7px 0;
  font-size: 13px;
}
.kv-label {
  color: var(--text-3);
  flex-shrink: 0;
}
.kv-value {
  color: var(--text-1);
  text-align: right;
  word-break: break-all;
}
.kv-value.discount {
  color: var(--ok);
}
.kv-value.pay {
  color: var(--neon);
  font-weight: 700;
  font-size: 16px;
  text-shadow: 0 0 14px rgba(255, 46, 147, 0.5);
}

/* ---------- 时间线 ---------- */
.timeline {
  position: relative;
  padding-left: 4px;
}
.tl-item {
  position: relative;
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 7px 0 7px 16px;
  font-size: 13px;
}
/* 竖向连线 */
.tl-item::before {
  content: "";
  position: absolute;
  left: 4px;
  top: 0;
  bottom: 0;
  width: 1px;
  background: linear-gradient(180deg, rgba(255, 46, 147, 0.7), rgba(36, 229, 255, 0.35), transparent);
}
.tl-item:last-child::before {
  bottom: 50%;
}
.tl-dot {
  position: absolute;
  left: 0;
  top: 12px;
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: var(--ink-900);
  border: 2px solid var(--neon);
  box-shadow: 0 0 10px rgba(255, 46, 147, 0.9);
}

/* ---------- 底部操作栏 ---------- */
.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  padding: 0 var(--gutter);
  backdrop-filter: blur(var(--blur-lg)) saturate(160%);
  -webkit-backdrop-filter: blur(var(--blur-lg)) saturate(160%);
  background-image: linear-gradient(180deg, rgba(12, 14, 22, 0.72), rgba(8, 9, 15, 0.96));
  border-top: 1px solid var(--glass-line);
  box-shadow: var(--shadow-float);
  z-index: 99;
}
.bottom-bar :deep(.van-button) {
  width: 108px;
}
</style>
