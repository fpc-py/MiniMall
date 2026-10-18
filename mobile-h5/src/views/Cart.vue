<template>
  <div class="cart-page">
    <van-nav-bar title="购物车" fixed placeholder />

    <van-skeleton v-if="loading" title :row="4" style="padding: 16px" />

    <!-- 空购物车 -->
    <van-empty
      v-else-if="!allItems.length"
      description="购物车是空的"
    >
      <template #image>
        <van-icon name="cart-o" size="80" color="#eee" />
      </template>
      <van-button round type="danger" color="#FF2E93" size="small" @click="$router.push('/')">去逛逛</van-button>
    </van-empty>

    <!-- 购物车列表 -->
    <template v-else>
      <div class="cart-list">
        <div class="shop-group" v-for="shop in cart.shops" :key="shop.shopId">
          <div class="shop-title">
            <van-icon name="shop-o" size="14" color="#FF2E93" />
            <span>店铺 {{ shop.shopId }}</span>
          </div>

          <div
            class="cart-item"
            v-for="item in shop.items"
            :key="item.skuId"
          >
            <div class="item-check" @click="toggleCheck(item)">
              <van-icon
                :name="item.selected ? 'success' : 'circle'"
                :color="item.selected ? '#FF2E93' : '#ddd'"
                size="20"
              />
            </div>
            <div class="item-img">
              <img v-if="item.image" :src="item.image" class="item-img-el" alt="" />
              <van-icon v-else name="photo-o" :size="36" color="#ccc" />
            </div>
            <div class="item-info">
              <div class="item-name van-multi-ellipsis--l2">{{ item.spuName }}</div>
              <div class="item-spec">{{ item.skuName }}</div>
              <div class="item-stock" v-if="!item.inStock">库存不足</div>
              <div class="item-bottom">
                <span class="item-price">
                  <span class="sym">¥</span>{{ money(item.price) }}
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
              size="18"
              color="#ccc"
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
            :color="allChecked ? '#FF2E93' : '#ddd'"
            size="20"
          />
          <span>全选</span>
        </div>
        <div class="total-section">
          <div class="total-price">
            <span class="total-label">合计：</span>
            <span class="total-val">
              <span class="sym">¥</span>{{ money(cart.selectedAmount) }}
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
  background: #f5f5f5;
  min-height: 100vh;
  padding-bottom: 60px;
}

.cart-list {
  padding: 10px 12px;
}
.shop-group {
  margin-bottom: 10px;
}
.shop-title {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  font-weight: 600;
  color: #333;
  padding: 6px 2px 8px;
}
.cart-item {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #fff;
  border-radius: 10px;
  padding: 12px;
  margin-bottom: 8px;
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
  color: #222;
  line-height: 1.4;
}
.item-spec {
  font-size: 11px;
  color: #999;
  margin-top: 2px;
}
.item-stock {
  font-size: 11px;
  color: #ee0a24;
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
.item-del {
  flex-shrink: 0;
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
