<template>
  <div class="page cart-page">
    <van-nav-bar title="购物车" fixed placeholder />

    <van-skeleton v-if="loading" title :row="4" style="padding: 16px" />

    <!-- 空购物车 -->
    <van-empty
      v-else-if="!allItems.length"
      description="购物车是空的"
    >
      <template #image>
        <van-icon name="cart-o" size="80" color="#2a2f42" />
      </template>
      <van-button round type="danger" color="#FF2E93" size="small" @click="$router.push('/')">去逛逛</van-button>
    </van-empty>

    <!-- 购物车列表 -->
    <template v-else>
      <div class="cart-list">
        <div class="shop-group rv" v-for="(shop, si) in cart.shops" :key="shop.shopId" :style="{ '--i': si }">
          <div class="shop-title">
            <span class="shop-mark">
              <van-icon name="shop-o" size="13" color="#fff" />
            </span>
            <span class="eyebrow">STORE</span>
            <span class="shop-name">店铺 {{ shop.shopId }}</span>
          </div>

          <div
            class="cart-item"
            v-for="item in shop.items"
            :key="item.skuId"
          >
            <div class="item-check" @click="toggleCheck(item)">
              <van-icon
                :name="item.selected ? 'success' : 'circle'"
                :color="item.selected ? '#ff2e93' : '#3a3f52'"
                size="20"
              />
            </div>
            <div class="item-img">
              <img v-if="item.image" :src="item.image" class="item-img-el" alt="" />
              <van-icon v-else name="photo-o" :size="36" color="#6d7490" />
            </div>
            <div class="item-info">
              <div class="item-name van-multi-ellipsis--l2">{{ item.spuName }}</div>
              <div class="item-spec">{{ item.skuName }}</div>
              <div class="item-stock" v-if="!item.inStock">库存不足</div>
              <div class="item-bottom">
                <span class="item-price">
                  <span class="sym">¥</span><span class="num">{{ money(item.price) }}</span>
                </span>
                <van-stepper
                  :model-value="item.quantity"
                  :min="1"
                  :max="item.stock || 99"
                  :disabled="!item.inStock"
                  size="24"
                  theme="round"
                  button-size="20"
                  @change="(v) => handleQtyChange(item, v)"
                />
              </div>
            </div>
            <van-icon
              class="item-del"
              name="delete-o"
              size="17"
              color="#6d7490"
              @click="handleRemove(item)"
            />
          </div>
        </div>
      </div>

      <!-- 底部结算栏 -->
      <div class="cart-bottom-bar">
        <div class="check-all" @click="toggleAll">
          <van-icon
            :name="allChecked ? 'success' : 'circle'"
            :color="allChecked ? '#ff2e93' : '#3a3f52'"
            size="20"
          />
          <span>全选</span>
        </div>
        <div class="total-section">
          <div class="total-price">
            <span class="total-label">合计</span>
            <span class="total-val">
              <span class="sym">¥</span><span class="num">{{ money(cart.selectedAmount) }}</span>
            </span>
          </div>
          <div class="total-count">已选 {{ cart.selectedQuantity || 0 }} 件</div>
        </div>
        <div
          class="settle-btn"
          :class="{ disabled: !cart.selectedQuantity }"
          @click="handleSettle"
        >
          结算
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import { getCart, updateCartItem, removeCartItem, selectAll } from '@/api/cart'

const router = useRouter()

const loading = ref(false)
const cart = ref({ shops: [], totalQuantity: 0, selectedQuantity: 0, selectedAmount: 0 })

const allItems = computed(() => (cart.value.shops || []).flatMap((s) => s.items || []))
const allChecked = computed(
  () => allItems.value.length > 0 && allItems.value.every((i) => i.selected)
)

function money(v) {
  return Number(v || 0).toFixed(2)
}

async function loadCart() {
  loading.value = true
  try {
    const res = await getCart()
    cart.value = res.data || { shops: [], totalQuantity: 0, selectedQuantity: 0, selectedAmount: 0 }
  } catch (e) {
    showToast(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function toggleCheck(item) {
  try {
    await updateCartItem(item.skuId, { selected: !item.selected })
    await loadCart()
  } catch (e) {
    showToast(e.message || '操作失败')
  }
}

async function toggleAll() {
  try {
    await selectAll(!allChecked.value)
    await loadCart()
  } catch (e) {
    showToast(e.message || '操作失败')
  }
}

async function handleQtyChange(item, value) {
  const qty = Number(value)
  if (!qty || qty === item.quantity) return
  try {
    await updateCartItem(item.skuId, { quantity: qty })
    await loadCart()
  } catch (e) {
    showToast(e.message || '修改数量失败')
    await loadCart()
  }
}

function handleRemove(item) {
  showConfirmDialog({
    title: '提示',
    message: '确定要删除该商品吗？',
    confirmButtonColor: '#FF2E93',
  })
    .then(async () => {
      try {
        await removeCartItem(item.skuId)
        showToast('已删除')
        await loadCart()
      } catch (e) {
        showToast(e.message || '删除失败')
      }
    })
    .catch(() => {})
}

function handleSettle() {
  if (!cart.value.selectedQuantity) {
    showToast('请选择商品')
    return
  }
  router.push('/order/confirm')
}

onMounted(loadCart)
</script>

<style scoped>
.cart-page {
  padding-bottom: 74px;
}

.cart-list {
  padding: 14px var(--gutter) 0;
}
.shop-group {
  margin-bottom: 12px;
  padding: 12px 12px 4px;
  border-radius: var(--r-lg);
  background-image: linear-gradient(158deg, rgba(255, 255, 255, 0.06), rgba(255, 255, 255, 0.015));
  border: 1px solid var(--glass-line);
  backdrop-filter: blur(var(--blur)) saturate(150%);
  box-shadow: var(--shadow-card);
  animation: riseIn 0.55s var(--ease-out) both;
  animation-delay: calc(var(--i, 0) * 70ms);
}
.shop-title {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 2px 2px 12px;
}
.shop-mark {
  width: 24px;
  height: 24px;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-image: linear-gradient(135deg, var(--neon), var(--violet));
  box-shadow: 0 6px 16px -8px rgba(255, 46, 147, 0.95);
  flex-shrink: 0;
}
.shop-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-1);
  letter-spacing: 0.04em;
}
.cart-item {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 12px 2px;
  border-top: 1px solid var(--glass-line);
}
.item-check {
  flex-shrink: 0;
}
.item-img {
  width: 74px;
  height: 74px;
  border-radius: var(--r-sm);
  background: var(--glass);
  border: 1px solid var(--glass-line);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
}
.item-img-el {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.item-info {
  flex: 1;
  min-width: 0;
}
.item-name {
  font-size: 13px;
  color: var(--text-1);
  line-height: 1.42;
}
.item-spec {
  font-size: 11px;
  color: var(--text-3);
  margin-top: 3px;
}
.item-stock {
  font-size: 11px;
  color: var(--danger);
  margin-top: 3px;
}
.item-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 9px;
}
.item-price {
  display: flex;
  align-items: baseline;
  gap: 1px;
  color: var(--neon);
  font-weight: 700;
}
.item-price .sym {
  font-size: 11px;
}
.item-price .num {
  font-size: 17px;
}
.item-del {
  flex-shrink: 0;
  align-self: flex-start;
  padding: 4px;
}

/* ---------- 底部结算栏 ---------- */
.cart-bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 64px;
  display: flex;
  align-items: center;
  padding: 0 var(--gutter);
  gap: 10px;
  backdrop-filter: blur(var(--blur-lg)) saturate(160%);
  -webkit-backdrop-filter: blur(var(--blur-lg)) saturate(160%);
  background-image: linear-gradient(180deg, rgba(12, 14, 22, 0.72), rgba(8, 9, 15, 0.96));
  border-top: 1px solid var(--glass-line);
  box-shadow: var(--shadow-float);
  z-index: 99;
}
.check-all {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: var(--text-2);
  flex-shrink: 0;
}
.total-section {
  flex: 1;
  text-align: right;
  min-width: 0;
}
.total-price {
  display: flex;
  align-items: baseline;
  justify-content: flex-end;
  gap: 5px;
}
.total-label {
  font-size: 11px;
  color: var(--text-3);
  letter-spacing: 0.1em;
}
.total-val {
  display: flex;
  align-items: baseline;
  gap: 1px;
  color: var(--neon);
  font-weight: 700;
}
.total-val .sym {
  font-size: 12px;
}
.total-val .num {
  font-size: 21px;
  text-shadow: 0 0 16px rgba(255, 46, 147, 0.6);
}
.total-count {
  font-size: 11px;
  color: var(--text-3);
  margin-top: 1px;
}
.settle-btn {
  width: 104px;
  height: 42px;
  line-height: 42px;
  text-align: center;
  border-radius: var(--r-pill);
  background-image: linear-gradient(135deg, var(--neon), #ff5ea8 55%, var(--violet));
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.06em;
  flex-shrink: 0;
  box-shadow: var(--glow-neon);
}
.settle-btn.disabled {
  background-image: none;
  background: var(--glass-2);
  border: 1px solid var(--glass-line);
  color: var(--text-3);
  box-shadow: none;
}
</style>
