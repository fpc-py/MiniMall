<template>
  <div class="order-list-page">
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
        <van-icon name="orders-o" size="80" color="#eee" />
      </template>
      <van-button round type="danger" color="#FF2E93" size="small" @click="$router.push('/')">去逛逛</van-button>
    </van-empty>

    <!-- 订单列表 -->
    <div class="order-list" v-else>
      <div class="order-card" v-for="order in orders" :key="order.id" @click="goDetail(order.id)">
        <div class="order-header">
          <span class="order-no">{{ order.orderNo }}</span>
          <span class="order-status" :style="{ color: statusColor(order.orderStatus) }">
            {{ statusText(order.orderStatus) }}
          </span>
        </div>

        <div class="order-goods" v-for="item in order.items || []" :key="item.skuId">
          <div class="order-goods-img">
            <img v-if="item.productImage" :src="item.productImage" class="order-goods-img-el" alt="" />
            <van-icon v-else name="photo-o" :size="36" color="#ccc" />
          </div>
          <div class="order-goods-info">
            <div class="order-goods-name van-multi-ellipsis--l2">{{ item.productName }}</div>
            <div class="order-goods-spec">{{ item.attrs }}</div>
            <div class="order-goods-bottom">
              <span class="order-goods-price">
                <span class="sym">¥</span>{{ money(item.price) }}
              </span>
              <span class="order-goods-qty">x{{ item.quantity }}</span>
            </div>
          </div>
        </div>

        <div class="order-footer">
          <div class="order-total">
            实付：
            <span class="total-price">
              <span class="sym">¥</span>{{ money(order.payAmount) }}
            </span>
          </div>
          <div class="order-time">{{ order.createTime }}</div>
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
  const map = { 10: '#FF2E93', 20: '#f57c00', 30: '#1677ff', 40: '#07c160', 5: '#999' }
  return map[status] || '#999'
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
  background: #f5f5f5;
  min-height: 100vh;
  padding-bottom: 20px;
}

.order-list {
  padding: 10px 12px;
}
.order-card {
  background: #fff;
  border-radius: 10px;
  margin-bottom: 10px;
  overflow: hidden;
}
.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 14px;
  border-bottom: 1px solid #f5f5f5;
}
.order-no {
  font-size: 12px;
  color: #999;
}
.order-status {
  font-size: 12px;
  font-weight: 600;
}

.order-goods {
  display: flex;
  gap: 10px;
  padding: 10px 14px;
  border-bottom: 1px solid #f5f5f5;
}
.order-goods:last-child {
  border-bottom: none;
}
.order-goods-img {
  width: 64px;
  height: 64px;
  background: #fafafa;
  border-radius: 8px;
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
  color: #222;
  line-height: 1.4;
}
.order-goods-spec {
  font-size: 11px;
  color: #999;
  margin-top: 2px;
}
.order-goods-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 6px;
}
.order-goods-price {
  color: #FF2E93;
  font-weight: 700;
  font-size: 14px;
}
.order-goods-price .sym { font-size: 10px; }
.order-goods-qty {
  font-size: 12px;
  color: #999;
}

.order-footer {
  padding: 10px 14px;
}
.order-total {
  text-align: right;
  font-size: 12px;
  color: #666;
}
.total-price {
  color: #FF2E93;
  font-weight: 700;
  font-size: 15px;
}
.total-price .sym { font-size: 11px; }
.order-time {
  text-align: right;
  font-size: 11px;
  color: #ccc;
  margin-top: 2px;
}
.order-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}
</style>
