<template>
  <div class="order-list-page">
    <van-nav-bar title="我的订单" fixed placeholder />

    <!-- 订单标签 -->
    <van-tabs v-model:active="activeTab" sticky offset-top="46" color="#FF2E93" line-width="20">
      <van-tab title="全部" />
      <van-tab title="待付款" />
      <van-tab title="待发货" />
      <van-tab title="待收货" />
      <van-tab title="已完成" />
    </van-tabs>

    <!-- 空状态 -->
    <van-empty
      v-if="!filteredOrders.length"
      description="暂无相关订单"
    >
      <template #image>
        <van-icon name="orders-o" size="80" color="#eee" />
      </template>
      <van-button round type="danger" size="small" @click="$router.push('/')">去逛逛</van-button>
    </van-empty>

    <!-- 订单列表 -->
    <div class="order-list" v-else>
      <div class="order-card" v-for="order in filteredOrders" :key="order.id">
        <div class="order-header">
          <span class="order-store">
            <van-icon name="shop-o" size="14" color="#FF2E93" />
            {{ order.store }}
          </span>
          <span class="order-status" :style="{ color: statusColor(order.status) }">
            {{ statusText(order.status) }}
          </span>
        </div>

        <div
          class="order-goods"
          v-for="good in order.goods"
          :key="good.id"
          @click="goDetail(order.id)"
        >
          <div class="order-goods-img">
            <van-icon :name="good.icon" :size="36" :color="good.color" />
          </div>
          <div class="order-goods-info">
            <div class="order-goods-name van-multi-ellipsis--l2">{{ good.name }}</div>
            <div class="order-goods-spec">{{ good.spec }}</div>
            <div class="order-goods-bottom">
              <span class="order-goods-price">
                <span class="sym">¥</span>{{ good.price }}
              </span>
              <span class="order-goods-qty">x{{ good.quantity }}</span>
            </div>
          </div>
        </div>

        <div class="order-footer">
          <div class="order-total">
            共 {{ order.totalQty }} 件 合计：
            <span class="total-price">
              <span class="sym">¥</span>{{ order.totalPrice }}
            </span>
          </div>
          <div class="order-actions">
            <van-button
              v-if="order.status === 'pending_payment'"
              round
              type="danger"
              size="small"
              @click="goPay(order.id)"
            >去支付</van-button>
            <van-button
              v-if="order.status === 'pending_receipt'"
              round
              size="small"
              color="#FF2E93"
              @click="confirmReceipt(order.id)"
            >确认收货</van-button>
            <van-button
              v-if="order.status === 'completed'"
              round
              plain
              size="small"
              color="#999"
            >删除订单</van-button>
            <van-button
              v-if="order.status === 'pending_payment'"
              round
              plain
              size="small"
              color="#999"
            >取消订单</van-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'

const router = useRouter()
const activeTab = ref(0)

const orders = ref([
  {
    id: '202609130001',
    store: 'Mall 旗舰店',
    status: 'pending_payment',
    totalQty: 2,
    totalPrice: '7298.00',
    goods: [
      { id: 1, icon: 'phone-o', color: '#FF2E93', name: '旗舰智能手机 5G全网通', spec: '曜石黑 / 12GB+256GB', price: '3999', quantity: 1 },
      { id: 2, icon: 'tv-o', color: '#667eea', name: '智能4K电视 65英寸', spec: '65英寸 / 4K超清', price: '3299', quantity: 1 },
    ],
  },
  {
    id: '202609120002',
    store: 'Mall 数码店',
    status: 'pending_shipment',
    totalQty: 1,
    totalPrice: '599.00',
    goods: [
      { id: 3, icon: 'sound-o', color: '#f5576c', name: '蓝牙降噪耳机 无线 长续航', spec: '黑色 / 标准版', price: '599', quantity: 1 },
    ],
  },
  {
    id: '202609100003',
    store: 'Mall 美妆店',
    status: 'pending_receipt',
    totalQty: 2,
    totalPrice: '478.00',
    goods: [
      { id: 4, icon: 'like-o', color: '#fa709a', name: '精华液护肤品 保湿补水', spec: '30ml / 经典版', price: '239', quantity: 2 },
    ],
  },
  {
    id: '202609050004',
    store: 'Mall 运动店',
    status: 'completed',
    totalQty: 1,
    totalPrice: '299.00',
    goods: [
      { id: 5, icon: 'more-o', color: '#43e97b', name: '运动跑鞋 透气网面 轻便减震', spec: '42码 / 白色', price: '299', quantity: 1 },
    ],
  },
])

const tabStatusMap = ['', 'pending_payment', 'pending_shipment', 'pending_receipt', 'completed']

const filteredOrders = computed(() => {
  const status = tabStatusMap[activeTab.value]
  if (!status) return orders.value
  return orders.value.filter(o => o.status === status)
})

function statusText(status) {
  const map = { pending_payment: '待付款', pending_shipment: '待发货', pending_receipt: '待收货', completed: '已完成', cancelled: '已取消' }
  return map[status] || status
}

function statusColor(status) {
  const map = { pending_payment: '#FF2E93', pending_shipment: '#f57c00', pending_receipt: '#1677ff', completed: '#07c160' }
  return map[status] || '#999'
}

function goDetail(orderId) {
  router.push(`/product/1`)
}

function goPay(orderId) {
  router.push(`/order/payment?orderNo=${orderId}`)
}

function confirmReceipt(orderId) {
  showConfirmDialog({
    title: '确认收货',
    message: '是否确认已收到商品？',
    confirmButtonColor: '#FF2E93',
  }).then(() => {
    const order = orders.value.find(o => o.id === orderId)
    if (order) order.status = 'completed'
    showToast('已确认收货')
  }).catch(() => {})
}
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
.order-store {
  font-size: 13px;
  font-weight: 600;
  color: #333;
  display: flex;
  align-items: center;
  gap: 4px;
}
.order-status {
  font-size: 12px;
  font-weight: 500;
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
  margin-bottom: 8px;
}
.total-price {
  color: #FF2E93;
  font-weight: 700;
  font-size: 15px;
}
.total-price .sym { font-size: 11px; }
.order-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
