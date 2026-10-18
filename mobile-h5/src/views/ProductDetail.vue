<template>
  <div class="detail-page">
    <!-- 顶部导航 -->
    <van-nav-bar
      :title="product?.name || '商品详情'"
      left-arrow
      fixed
      placeholder
      @click-left="$router.back()"
    />

    <van-skeleton v-if="loading" title :row="6" style="padding: 16px" />

    <!-- 加载失败 -->
    <div v-else-if="error" class="error-wrap">
      <van-empty description="商品加载失败">
        <van-button round type="danger" color="#FF2E93" size="small" @click="loadProduct">重新加载</van-button>
      </van-empty>
    </div>

    <!-- 商品不存在 -->
    <van-empty v-else-if="!product" description="商品不存在" />

    <template v-else>
      <!-- 商品图片轮播 -->
      <van-swipe :autoplay="3000" indicator-color="#FF2E93" class="detail-swipe">
        <van-swipe-item v-for="(img, i) in carouselImages" :key="i">
          <div class="detail-swipe-item">
            <img :src="img.url" class="detail-swipe-img" alt="" />
          </div>
        </van-swipe-item>
        <van-swipe-item v-if="!carouselImages.length">
          <div class="detail-swipe-item">
            <van-icon name="photo-o" :size="64" color="#ccc" />
          </div>
        </van-swipe-item>
      </van-swipe>

      <!-- 商品信息 -->
      <div class="info-section">
        <div class="price-row">
          <span class="price-current">
            <span class="symbol">¥</span>
            <span class="val">{{ displayPrice }}</span>
          </span>
          <span class="price-old" v-if="displayOldPrice">¥{{ displayOldPrice }}</span>
          <span class="sales-badge">已售 {{ product.totalSales || 0 }}</span>
        </div>
        <h1 class="product-name">{{ product.name }}</h1>
        <div class="extra-row">
          <span>运费：免运费</span>
          <span v-if="product.brand">{{ product.brand }}</span>
        </div>
      </div>

      <!-- 规格选择 -->
      <div class="section-block" @click="openSku('cart')">
        <div class="section-row">
          <span class="section-label">已选</span>
          <span class="section-value">{{ selectedSkuLabel || '请选择规格' }}</span>
          <van-icon name="arrow" color="#ccc" />
        </div>
      </div>

      <!-- 商品详情 -->
      <div class="section-block">
        <div class="section-title">商品详情</div>
        <div class="desc-content">
          <div class="desc-subtitle" v-if="product.subtitle">{{ product.subtitle }}</div>
          <div class="desc-text" v-if="product.description" v-html="product.description"></div>
          <div class="desc-images" v-if="detailImages.length">
            <img
              v-for="(img, i) in detailImages"
              :key="i"
              :src="img.url"
              class="desc-img-el"
              alt=""
            />
          </div>
          <van-empty
            v-if="!product.subtitle && !product.description && !detailImages.length"
            description="暂无商品详情"
            image-size="60"
          />
        </div>
      </div>

      <!-- SKU 选择弹窗 -->
      <van-action-sheet v-model:show="showSku" title="选择规格" round>
        <div class="sku-panel">
          <div class="sku-product-info">
            <div class="sku-img">
              <img v-if="currentSkuImage" :src="currentSkuImage" class="sku-img-el" alt="" />
              <van-icon v-else name="photo-o" :size="48" color="#ccc" />
            </div>
            <div class="sku-price-info">
              <div class="sku-price">¥{{ displayPrice }}</div>
              <div class="sku-stock">库存 {{ displayStock }} 件</div>
            </div>
          </div>
          <div class="sku-group">
            <div class="sku-group-name">选择规格</div>
            <div class="sku-options">
              <span
                v-for="sku in product.skus"
                :key="sku.id"
                class="sku-option"
                :class="{ active: sku.id === selectedSkuId, disabled: (sku.stock || 0) <= 0 }"
                @click="selectSku(sku)"
              >{{ skuLabel(sku) }}</span>
            </div>
          </div>
          <div class="sku-quantity">
            <span class="qty-label">数量</span>
            <van-stepper v-model="quantity" :min="1" :max="displayStock || 1" />
          </div>
          <div class="sku-confirm">
            <van-button type="danger" color="#FF2E93" block round @click="confirmSku">确定</van-button>
          </div>
        </div>
      </van-action-sheet>

      <!-- 底部操作栏 -->
      <div class="bottom-bar">
        <div class="bar-icon" @click="$router.push('/')">
          <van-icon name="home-o" size="22" />
          <span>首页</span>
        </div>
        <div class="bar-icon" @click="$router.push('/cart')">
          <van-icon name="cart-o" size="22" />
          <span>购物车</span>
        </div>
        <div class="bar-btn bar-btn-cart" @click="openSku('cart')">加入购物车</div>
        <div class="bar-btn bar-btn-buy" @click="openSku('buy')">立即购买</div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getProductDetail } from '@/api/product'
import { addToCart } from '@/api/cart'
import { isLoggedIn } from '@/utils/auth'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const error = ref(false)
const product = ref(null)

const showSku = ref(false)
const skuMode = ref('cart')
const selectedSkuId = ref(null)
const quantity = ref(1)

const carouselImages = computed(() => {
  const imgs = (product.value?.images || []).filter((i) => i.type === 0)
  if (imgs.length) return imgs
  if (product.value?.mainImage) return [{ url: product.value.mainImage }]
  return []
})

const detailImages = computed(() => (product.value?.images || []).filter((i) => i.type === 1))

const currentSku = computed(() => {
  return (product.value?.skus || []).find((s) => s.id === selectedSkuId.value) || null
})

const displayPrice = computed(() => {
  if (currentSku.value?.price != null) return Number(currentSku.value.price).toFixed(2)
  if (product.value?.minPrice != null) return Number(product.value.minPrice).toFixed(2)
  return '0.00'
})

const displayOldPrice = computed(() => {
  if (currentSku.value?.originalPrice != null) return Number(currentSku.value.originalPrice).toFixed(2)
  return ''
})

const displayStock = computed(() => currentSku.value?.stock ?? 0)

const currentSkuImage = computed(
  () => currentSku.value?.image || carouselImages.value[0]?.url || ''
)

const selectedSkuLabel = computed(() => (currentSku.value ? skuLabel(currentSku.value) : ''))

function attrsText(sku) {
  if (sku?.attrs && typeof sku.attrs === 'object') {
    return Object.values(sku.attrs).filter(Boolean).join(' / ')
  }
  return ''
}

function skuLabel(sku) {
  return sku?.name || attrsText(sku) || '默认规格'
}

function selectSku(sku) {
  if ((sku.stock || 0) <= 0) {
    showToast('该规格库存不足')
    return
  }
  selectedSkuId.value = sku.id
}

function openSku(mode) {
  skuMode.value = mode
  showSku.value = true
}

function requireLogin() {
  if (!isLoggedIn()) {
    router.push('/login?redirect=' + encodeURIComponent(route.fullPath))
    return false
  }
  return true
}

async function confirmSku() {
  showSku.value = false
  if (!requireLogin()) return
  if (!selectedSkuId.value) {
    showToast('请选择规格')
    return
  }
  if (skuMode.value === 'cart') {
    try {
      await addToCart(selectedSkuId.value, quantity.value)
      showToast('已加入购物车')
    } catch (e) {
      showToast(e.message || '加入购物车失败')
    }
  } else {
    router.push(`/order/confirm?skuId=${selectedSkuId.value}&quantity=${quantity.value}&fromCart=false`)
  }
}

async function loadProduct() {
  loading.value = true
  error.value = false
  try {
    const res = await getProductDetail(route.params.id)
    const d = res.data
    if (!d) {
      product.value = null
      return
    }
    product.value = d
    const skus = d.skus || []
    if (skus.length) {
      const first = skus.find((s) => (s.stock || 0) > 0) || skus[0]
      selectedSkuId.value = first.id
    }
  } catch (e) {
    error.value = true
  } finally {
    loading.value = false
  }
}

onMounted(loadProduct)
</script>

<style scoped>
.detail-page {
  background: #f5f5f5;
  min-height: 100vh;
  padding-bottom: 56px;
}

.error-wrap {
  padding-top: 40px;
}

/* 商品轮播 */
.detail-swipe {
  height: 375px;
}
.detail-swipe-item {
  height: 375px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: #fafafa;
}
.detail-swipe-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 信息区 */
.info-section {
  background: #fff;
  padding: 14px 16px;
  margin-bottom: 10px;
}
.price-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 8px;
}
.price-current {
  color: #FF2E93;
  font-weight: 700;
}
.price-current .symbol {
  font-size: 14px;
}
.price-current .val {
  font-size: 28px;
}
.price-old {
  font-size: 13px;
  color: #ccc;
  text-decoration: line-through;
}
.sales-badge {
  margin-left: auto;
  font-size: 12px;
  color: #999;
  background: #f5f5f5;
  padding: 2px 8px;
  border-radius: 10px;
}
.product-name {
  font-size: 16px;
  color: #222;
  line-height: 1.5;
  font-weight: 500;
  margin-bottom: 8px;
}
.extra-row {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #999;
}

/* 通用区块 */
.section-block {
  background: #fff;
  padding: 0 16px;
  margin-bottom: 10px;
}
.section-row {
  display: flex;
  align-items: center;
  padding: 14px 0;
  border-bottom: 1px solid #f5f5f5;
}
.section-block .section-row:last-child {
  border-bottom: none;
}
.section-label {
  font-size: 14px;
  color: #666;
  width: 70px;
  flex-shrink: 0;
}
.section-value {
  flex: 1;
  font-size: 14px;
  color: #333;
  text-align: right;
  margin-right: 6px;
}
.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  padding: 14px 0;
}

/* 详情 */
.desc-content {
  padding-bottom: 14px;
}
.desc-subtitle {
  font-size: 13px;
  color: #666;
  margin-bottom: 6px;
}
.desc-text {
  font-size: 13px;
  color: #333;
  line-height: 1.6;
}
.desc-images {
  margin-top: 8px;
}
.desc-img-el {
  width: 100%;
  display: block;
  border-radius: 8px;
  margin-bottom: 8px;
}

/* SKU 弹窗 */
.sku-panel {
  padding: 16px;
  max-height: 60vh;
  overflow-y: auto;
}
.sku-product-info {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}
.sku-img {
  width: 80px;
  height: 80px;
  background: #fef0f5;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
}
.sku-img-el {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.sku-price-info {
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.sku-price {
  color: #FF2E93;
  font-size: 20px;
  font-weight: 700;
}
.sku-stock {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}
.sku-group {
  margin-bottom: 16px;
}
.sku-group-name {
  font-size: 14px;
  color: #333;
  margin-bottom: 8px;
  font-weight: 500;
}
.sku-options {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.sku-option {
  display: inline-block;
  padding: 6px 16px;
  border: 1px solid #eee;
  border-radius: 20px;
  font-size: 13px;
  color: #333;
  background: #fafafa;
  cursor: pointer;
}
.sku-option.active {
  border-color: #FF2E93;
  color: #FF2E93;
  background: #fff5f8;
}
.sku-option.disabled {
  color: #ccc;
  background: #f7f7f7;
  text-decoration: line-through;
}
.sku-quantity {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}
.qty-label {
  font-size: 14px;
  color: #333;
}
.sku-confirm {
  padding-bottom: 20px;
}

/* 底部操作栏 */
.bottom-bar {
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
.bar-icon {
  display: flex;
  flex-direction: column;
  align-items: center;
  font-size: 10px;
  color: #666;
  width: 44px;
  flex-shrink: 0;
}
.bar-btn {
  flex: 1;
  height: 40px;
  line-height: 40px;
  text-align: center;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
}
.bar-btn-cart {
  background: #ffe8f0;
  color: #FF2E93;
}
.bar-btn-buy {
  background: linear-gradient(135deg, #FF2E93, #ff6b9d);
  color: #fff;
}
</style>
