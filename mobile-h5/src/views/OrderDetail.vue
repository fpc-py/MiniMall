<template>
  <div class="order-detail-page">
    <van-nav-bar title="订单详情" left-arrow fixed placeholder @click-left="router.back()" />

    <van-skeleton v-if="loading" title :row="6" style="padding: 16px" />

    <template v-else-if="order">
      <!-- 状态 -->
      <div class="status-section">
        <div class="status-text">{{ statusText(order.orderStatus) }}</div>
        <div class="status-desc">{{ statusDesc(order.orderStatus) }}</div>
      </div>

      <!-- 收货信息 -->
      <div class="block">
        <div class="receiver-row">
          <span class="receiver-name">{{ order.receiverName }}</span>
          <span class="receiver-phone">{{ order.receiverPhone }}</span>
        </div>
        <div class="receiver-address">{{ order.receiverAddress }}</div>
      </div>

      <!-- 快递信息 -->
      <div class="block" v-if="order.deliveryCompany || order.deliveryNo">
        <div class="block-title">物流信息</div>
        <div class="kv-row">
          <span class="kv-label">快递公司</span>
          <span class="kv-value">{{ order.deliveryCompany || '-' }}</span>
        </div>
        <div class="kv-row">
          <span class="kv-label">快递单号</span>
          <span class="kv-value">{{ order.deliveryNo || '-' }}</span>
        </div>
      </div>

      <!-- 商品项 -->
      <div class="block goods-block">
        <div class="goods-item" v-for="item in order.items || []" :key="item.skuId">
          <div class="goods-img">
            <img v-if="item.productImage" :src="item.productImage" class="goods-img-el" alt="" />
            <van-icon v-else name="photo-o" :size="36" color="#ccc" />
          </div>
          <div class="goods-info">
            <div class="goods-name van-multi-ellipsis--l2">{{ item.productName }}</div>
            <div class="goods-spec">{{ item.attrs }}</div>
            <div class="goods-bottom">
              <span class="goods-price"><span class="sym">¥</span>{{ money(item.price) }}</span>
              <span class="goods-qty">x{{ item.quantity }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 金额明细 -->
      <div class="block">
        <div class="block-title">金额明细</div>
        <div class="kv-row">
          <span class="kv-label">商品总额</span>
          <span class="kv-value">¥{{ money(order.totalAmount) }}</span>
        </div>
        <div class="kv-row">
          <span class="kv-label">运费</span>
          <span class="kv-value">{{ Number(order.freightAmount || 0) > 0 ? '¥' + money(order.freightAmount) : '免运费' }}</span>
        </div>
        <div class="kv-row">
          <span class="kv-label">优惠</span>
          <span class="kv-value" style="color:#FF2E93">-¥{{ money(order.discountAmount) }}</span>
        </div>
        <div class="kv-row">
          <span class="kv-label">实付金额</span>
          <span class="kv-value pay">¥{{ money(order.payAmount) }}</span>
        </div>
      </div>

      <!-- 订单信息 -->
      <div class="block">
        <div class="block-title">订单信息</div>
        <div class="kv-row">
          <span class="kv-label">订单编号</span>
          <span class="kv-value">{{ order.orderNo }}</span>
        </div>
        <div class="kv-row" v-if="order.memberRemark">
          <span class="kv-label">买家备注</span>
          <span class="kv-value">{{ order.memberRemark }}</span>
        </div>
        <div class="kv-row" v-if="order.cancelReason">
          <span class="kv-label">取消原因</span>
          <span class="kv-value">{{ order.cancelReason }}</span>
        </div>
      </div>

      <!-- 时间线 -->
      <div class="block">
        <div class="block-title">订单进度</div>
        <div class="kv-row" v-for="t in timeline" :key="t.label">
          <span class="kv-label">{{ t.label }}</span>
          <span class="kv-value">{{ t.value }}</span>
        </div>
      </div>

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
  background: #f5f5f5;
  min-height: 100vh;
  padding-bottom: 70px;
}

.status-section {
  background: linear-gradient(135deg, #FF2E93, #ff6b9d);
  color: #fff;
  padding: 20px 16px;
}
.status-text {
  font-size: 20px;
  font-weight: 700;
}
.status-desc {
  font-size: 12px;
  margin-top: 6px;
  opacity: 0.9;
}

.block {
  background: #fff;
  margin: 10px 12px;
  border-radius: 10px;
  padding: 14px;
}
.block-title {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin-bottom: 10px;
}
.receiver-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}
.receiver-name {
  font-size: 15px;
  font-weight: 600;
  color: #222;
}
.receiver-phone {
  font-size: 13px;
  color: #666;
}
.receiver-address {
  font-size: 13px;
  color: #999;
}

.goods-block {
  padding: 0 14px;
}
.goods-item {
  display: flex;
  gap: 10px;
  padding: 12px 0;
  border-bottom: 1px solid #f5f5f5;
}
.goods-item:last-child {
  border-bottom: none;
}
.goods-img {
  width: 72px;
  height: 72px;
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
.goods-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 6px;
}
.goods-price {
  color: #FF2E93;
  font-weight: 700;
  font-size: 15px;
}
.goods-price .sym { font-size: 11px; }
.goods-qty {
  font-size: 12px;
  color: #999;
}

.kv-row {
  display: flex;
  justify-content: space-between;
  padding: 6px 0;
  font-size: 13px;
}
.kv-label {
  color: #999;
}
.kv-value {
  color: #333;
  text-align: right;
}
.kv-value.pay {
  color: #FF2E93;
  font-weight: 700;
  font-size: 15px;
}

.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 56px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  padding: 0 12px;
  border-top: 1px solid #f0f0f0;
  z-index: 99;
}
.bottom-bar :deep(.van-button) {
  width: 110px;
}
</style>
