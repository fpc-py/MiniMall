<template>
  <div class="cart-page">
    <van-nav-bar title="购物车" fixed placeholder />

    <!-- 空购物车 -->
    <van-empty
      v-if="!cartItems.length"
      description="购物车是空的"
    >
      <template #image>
        <van-icon name="cart-o" size="80" color="#eee" />
      </template>
      <van-button round type="danger" size="small" @click="$router.push('/')">去逛逛</van-button>
    </van-empty>

    <!-- 购物车列表 -->
    <template v-else>
      <div class="cart-list">
        <div
          class="cart-item"
          v-for="(item, i) in cartItems"
          :key="item.id"
        >
          <div class="item-check" @click="toggleCheck(i)">
            <van-icon
              :name="item.checked ? 'success' : 'circle'"
              :color="item.checked ? '#FF2E93' : '#ddd'"
              size="20"
            />
          </div>
          <div class="item-img">
            <van-icon :name="item.icon" :size="36" :color="item.color" />
          </div>
          <div class="item-info">
            <div class="item-name van-multi-ellipsis--l2">{{ item.name }}</div>
            <div class="item-spec">{{ item.spec }}</div>
            <div class="item-bottom">
              <span class="item-price">
                <span class="sym">¥</span>{{ item.price }}
              </span>
              <van-stepper
                v-model="item.quantity"
                :min="1"
                :max="99"
                size="24"
                theme="round"
                button-size="20"
                @change="updateCart"
              />
            </div>
          </div>
        </div>
      </div>

      <!-- 底部结算栏 -->
      <div class="cart-bottom-bar">
        <div class="check-all" @click="toggleAll">
          <van-icon
            :name="allChecked ? 'success' : 'circle'"
            :color="allChecked ? '#FF2E93' : '#ddd'"
            size="20"
          />
          <span>全选</span>
        </div>
        <div class="total-section">
          <div class="total-price">
            <span class="total-label">合计：</span>
            <span class="total-val">
              <span class="sym">¥</span>{{ totalPrice }}
            </span>
          </div>
          <div class="total-count">已选 {{ checkedCount }} 件</div>
        </div>
        <div
          class="settle-btn"
          :class="{ disabled: checkedCount === 0 }"
          @click="handleSettle"
        >
          结算
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'

const router = useRouter()

const cartItems = ref([
  {
    id: 1,
    icon: 'phone-o',
    color: '#FF2E93',
    name: '旗舰智能手机 5G全网通 高清拍照 长续航',
    spec: '曜石黑 / 12GB+256GB',
    price: '3999',
    quantity: 1,
    checked: true,
  },
  {
    id: 2,
    icon: 'tv-o',
    color: '#667eea',
    name: '智能4K电视 65英寸 全面屏 家庭影院',
    spec: '65英寸 / 4K超清',
    price: '3299',
    quantity: 1,
    checked: true,
  },
  {
    id: 3,
    icon: 'sound-o',
    color: '#f5576c',
    name: '蓝牙降噪耳机 无线 长续航 HiFi音质',
    spec: '黑色 / 标准版',
    price: '599',
    quantity: 2,
    checked: false,
  },
])

const allChecked = computed(() => cartItems.value.length > 0 && cartItems.value.every(i => i.checked))
const checkedItems = computed(() => cartItems.value.filter(i => i.checked))
const checkedCount = computed(() => checkedItems.value.length)
const totalPrice = computed(() => {
  return checkedItems.value.reduce((sum, i) => sum + parseFloat(i.price) * i.quantity, 0).toFixed(2)
})

function toggleCheck(index) {
  cartItems.value[index].checked = !cartItems.value[index].checked
}

function toggleAll() {
  const newVal = !allChecked.value
  cartItems.value.forEach(i => { i.checked = newVal })
}

function updateCart() {
  // 后续可同步到后端
}

function handleSettle() {
  if (checkedCount.value === 0) {
    showToast('请选择商品')
    return
  }
  // 跳转到确认订单页，带选中商品参数
  router.push({
    path: '/order/confirm',
    query: { items: checkedItems.value.map(i => i.id).join(',') }
  })
}
</script>

<style scoped>
.cart-page {
  background: #f5f5f5;
  min-height: 100vh;
  padding-bottom: 60px;
}

.cart-list {
  padding: 10px 12px;
}
.cart-item {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #fff;
  border-radius: 10px;
  padding: 12px;
  margin-bottom: 10px;
}
.item-check {
  flex-shrink: 0;
}
.item-img {
  width: 72px;
  height: 72px;
  background: #fafafa;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.item-info {
  flex: 1;
  min-width: 0;
}
.item-name {
  font-size: 13px;
  color: #222;
  line-height: 1.4;
}
.item-spec {
  font-size: 11px;
  color: #999;
  margin-top: 2px;
}
.item-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
}
.item-price {
  color: #FF2E93;
  font-weight: 700;
  font-size: 16px;
}
.item-price .sym {
  font-size: 11px;
}

/* 底部结算栏 */
.cart-bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 56px;
  background: #fff;
  display: flex;
  align-items: center;
  padding: 0 12px;
  gap: 10px;
  border-top: 1px solid #f0f0f0;
  z-index: 99;
}
.check-all {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #333;
  flex-shrink: 0;
}
.total-section {
  flex: 1;
  text-align: right;
}
.total-price {
  display: flex;
  align-items: baseline;
  justify-content: flex-end;
  gap: 2px;
}
.total-label {
  font-size: 12px;
  color: #666;
}
.total-val {
  color: #FF2E93;
  font-weight: 700;
  font-size: 18px;
}
.total-val .sym {
  font-size: 12px;
}
.total-count {
  font-size: 11px;
  color: #999;
}
.settle-btn {
  width: 96px;
  height: 40px;
  line-height: 40px;
  text-align: center;
  background: linear-gradient(135deg, #FF2E93, #ff6b9d);
  color: #fff;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
  flex-shrink: 0;
}
.settle-btn.disabled {
  background: #ddd;
  color: #999;
}
</style>
