<template>
  <div class="page order-list-page">
    <van-nav-bar title="我的订单" fixed placeholder />

    <!-- 订单标签 -->
    <van-tabs v-model:active="activeTab" sticky offset-top="46" color="#FF2E93" line-width="20" @change="loadOrders">
      <van-tab title="全部" />
      <van-tab title="待付款" />
      <van-tab title="待发货" />
      <van-tab title="待收货" />
      <van-tab title="已完成" />
    </van-tabs>

    <!-- 空状态 -->
    <van-empty
      v-if="!loading && !orders.length"
      description="暂无相关订单"
    >
      <template #image>
        <van-icon name="orders-o" size="80" color="#2a2f42" />
      </template>
      <van-button round type="danger" color="#FF2E93" size="small" @click="$router.push('/')">去逛逛</van-button>
    </van-empty>

    <!-- 订单列表 -->
    <div class="order-list" v-else>
      <div
        class="order-card rv"
        v-for="(order, i) in orders"
        :key="order.id"
        :style="{ '--i': i }"
        @click="goDetail(order.id)"
      >
        <div class="order-header">
          <span class="order-no num">{{ order.orderNo }}</span>
          <span
            class="order-status"
            :style="{ color: statusColor(order.orderStatus), borderColor: statusColor(order.orderStatus) }"
          >
            {{ statusText(order.orderStatus) }}
          </span>
        </div>

        <div class="order-goods" v-for="item in order.items || []" :key="item.skuId">
          <div class="order-goods-img">
            <img v-if="item.productImage" :src="item.productImage" class="order-goods-img-el" alt="" />
            <van-icon v-else name="photo-o" :size="36" color="#6d7490" />
          </div>
          <div class="order-goods-info">
            <div class="order-goods-name van-multi-ellipsis--l2">{{ item.productName }}</div>
            <div class="order-goods-spec">{{ item.attrs }}</div>
            <div class="order-goods-bottom">
              <span class="order-goods-price">
                <span class="sym">¥</span><span class="num">{{ money(item.price) }}</span>
              </span>
              <span class="order-goods-qty num">x{{ item.quantity }}</span>
            </div>
          </div>
        </div>

        <div class="order-footer">
          <div class="order-foot-top">
            <span class="order-time num">{{ order.createTime }}</span>
            <div class="order-total">
              实付
              <span class="total-price">
                <span class="sym">¥</span><span class="num">{{ money(order.payAmount) }}</span>
              </span>
            </div>
          </div>
          <div class="order-actions">
            <van-button
              v-if="order.orderStatus === 10"
              round
              type="danger"
              color="#FF2E93"
              size="small"
              @click.stop="goPay(order.id)"
            >去支付</van-button>
            <van-button
              v-if="order.orderStatus === 10"
              round
              plain
              size="small"
              color="#999"
              @click.stop="handleCancel(order)"
            >取消订单</van-button>
            <van-button
              v-if="order.orderStatus === 30"
              round
              size="small"
              color="#FF2E93"
              @click.stop="handleReceipt(order)"
            >确认收货</van-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import { getMyOrders, cancelOrder, confirmReceipt } from '@/api/order'

const router = useRouter()
const activeTab = ref(0)
const loading = ref(false)
const orders = ref([])

const tabStatusMap = [null, 10, 20, 30, 40]

function money(v) {
  return Number(v || 0).toFixed(2)
}

function statusText(status) {
  const map = { 10: '待付款', 20: '待发货', 30: '待收货', 40: '已完成', 5: '已取消' }
  return map[status] || '未知'
}

function statusColor(status) {
  const map = { 10: '#ff2e93', 20: '#ffb020', 30: '#24e5ff', 40: '#34e39a', 5: '#6d7490' }
  return map[status] || '#6d7490'
}

async function loadOrders() {
  loading.value = true
  try {
    const params = { pageNum: 1, pageSize: 20 }
    const status = tabStatusMap[activeTab.value]
    if (status != null) params.orderStatus = status
    const res = await getMyOrders(params)
    orders.value = res.data?.list || []
  } catch (e) {
    showToast(e.message || '加载失败')
    orders.value = []
  } finally {
    loading.value = false
  }
}

function goDetail(id) {
  router.push(`/order/detail/${id}`)
}

function goPay(id) {
  router.push(`/order/payment?id=${id}`)
}

function handleCancel(order) {
  showConfirmDialog({
    title: '取消订单',
    message: '确定要取消该订单吗？',
    confirmButtonColor: '#FF2E93',
  })
    .then(async () => {
      try {
        await cancelOrder(order.id, '买家取消')
        showToast('已取消')
        await loadOrders()
      } catch (e) {
        showToast(e.message || '取消失败')
      }
    })
    .catch(() => {})
}

function handleReceipt(order) {
  showConfirmDialog({
    title: '确认收货',
    message: '是否确认已收到商品？',
    confirmButtonColor: '#FF2E93',
  })
    .then(async () => {
      try {
        await confirmReceipt(order.id)
        showToast('已确认收货')
        await loadOrders()
      } catch (e) {
        showToast(e.message || '操作失败')
      }
    })
    .catch(() => {})
}

onMounted(loadOrders)
</script>

<style scoped>
.order-list-page {
  padding-bottom: 24px;
}

.order-list {
  padding: 14px var(--gutter) 0;
}
.order-card {
  margin-bottom: 12px;
  border-radius: var(--r-lg);
  overflow: hidden;
  background-image: linear-gradient(158deg, rgba(255, 255, 255, 0.06), rgba(255, 255, 255, 0.015));
  border: 1px solid var(--glass-line);
  backdrop-filter: blur(var(--blur)) saturate(150%);
  box-shadow: var(--shadow-card);
  animation: riseIn 0.55s var(--ease-out) both;
  animation-delay: calc(var(--i, 0) * 65ms);
  transition: transform var(--dur-fast) var(--ease-spring), border-color var(--dur-fast);
}
.order-card:active {
  transform: scale(0.985);
  border-color: rgba(255, 46, 147, 0.38);
}
.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 13px 14px;
  border-bottom: 1px solid var(--glass-line);
}
.order-no {
  font-size: 11px;
  color: var(--text-3);
  letter-spacing: 0.04em;
}
/* 状态标签：描边胶囊，颜色来自状态映射 */
.order-status {
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.06em;
  padding: 2px 10px;
  border: 1px solid;
  border-radius: var(--r-pill);
  background: rgba(255, 255, 255, 0.03);
}

.order-goods {
  display: flex;
  gap: 11px;
  padding: 11px 14px;
  border-bottom: 1px solid var(--glass-line);
}
.order-goods:last-child {
  border-bottom: none;
}
.order-goods-img {
  width: 66px;
  height: 66px;
  border-radius: var(--r-sm);
  background: var(--glass);
  border: 1px solid var(--glass-line);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
}
.order-goods-img-el {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.order-goods-info {
  flex: 1;
  min-width: 0;
}
.order-goods-name {
  font-size: 13px;
  color: var(--text-1);
  line-height: 1.42;
}
.order-goods-spec {
  font-size: 11px;
  color: var(--text-3);
  margin-top: 3px;
}
.order-goods-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 7px;
}
.order-goods-price {
  display: flex;
  align-items: baseline;
  gap: 1px;
  color: var(--neon);
  font-weight: 700;
}
.order-goods-price .sym {
  font-size: 10px;
}
.order-goods-price .num {
  font-size: 15px;
}
.order-goods-qty {
  font-size: 12px;
  color: var(--text-3);
}

.order-footer {
  padding: 11px 14px 13px;
}
.order-foot-top {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
}
.order-time {
  font-size: 10px;
  color: var(--text-3);
  letter-spacing: 0.02em;
}
.order-total {
  display: flex;
  align-items: baseline;
  gap: 5px;
  font-size: 11px;
  color: var(--text-3);
  letter-spacing: 0.08em;
}
.total-price {
  display: flex;
  align-items: baseline;
  gap: 1px;
  color: var(--neon);
  font-weight: 700;
}
.total-price .sym {
  font-size: 11px;
}
.total-price .num {
  font-size: 17px;
  text-shadow: 0 0 14px rgba(255, 46, 147, 0.55);
}
.order-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 11px;
}
</style>
