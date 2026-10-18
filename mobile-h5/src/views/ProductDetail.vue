<template>
  <div class="page detail-page">
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
      <section class="gallery">
        <van-swipe :autoplay="3000" indicator-color="#ff2e93" class="detail-swipe">
          <van-swipe-item v-for="(img, i) in carouselImages" :key="i">
            <div class="detail-swipe-item">
              <img :src="img.url" class="detail-swipe-img" alt="" />
            </div>
          </van-swipe-item>
          <van-swipe-item v-if="!carouselImages.length">
            <div class="detail-swipe-item">
              <van-icon name="photo-o" :size="64" color="#6d7490" />
            </div>
          </van-swipe-item>
        </van-swipe>
        <div class="gallery-vignette" aria-hidden="true"></div>
      </section>

      <!-- 商品信息 -->
      <section class="info-section rv" style="--i: 0">
        <span class="eyebrow">PRODUCT</span>
        <div class="price-row">
          <span class="price-current">
            <span class="symbol">¥</span>
            <span class="val num">{{ displayPrice }}</span>
          </span>
          <span class="price-old num" v-if="displayOldPrice">¥{{ displayOldPrice }}</span>
          <span class="sales-badge">已售 {{ product.totalSales || 0 }}</span>
        </div>
        <h1 class="product-name">{{ product.name }}</h1>
        <div class="extra-row">
          <span class="extra-item">
            <van-icon name="logistics" size="12" color="#24e5ff" />免运费
          </span>
          <span v-if="product.brand" class="extra-item">
            <van-icon name="shield-o" size="12" color="#24e5ff" />{{ product.brand }}
          </span>
        </div>
      </section>

      <!-- 规格选择 -->
      <section class="section-block rv" style="--i: 1" @click="openSku('cart')">
        <div class="section-row">
          <span class="section-label">已选</span>
          <span class="section-value">{{ selectedSkuLabel || '请选择规格' }}</span>
          <van-icon name="arrow" color="#6d7490" />
        </div>
      </section>

      <!-- 商品详情 -->
      <section class="section-block rv" style="--i: 2">
        <div class="section-head">
          <span class="eyebrow">DETAILS</span>
          <span class="section-title">商品详情</span>
        </div>
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
      </section>

      <!-- SKU 选择弹窗 -->
      <van-action-sheet v-model:show="showSku" title="选择规格" round>
        <div class="sku-panel">
          <div class="sku-product-info">
            <div class="sku-img">
              <img v-if="currentSkuImage" :src="currentSkuImage" class="sku-img-el" alt="" />
              <van-icon v-else name="photo-o" :size="48" color="#6d7490" />
            </div>
            <div class="sku-price-info">
              <div class="sku-price">
                <span class="sym">¥</span><span class="num">{{ displayPrice }}</span>
              </div>
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
          <van-icon name="wap-home-o" size="21" />
          <span>首页</span>
        </div>
        <div class="bar-icon" @click="$router.push('/cart')">
          <van-icon name="cart-o" size="21" />
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
  padding-bottom: 68px;
}

.error-wrap {
  padding-top: 40px;
}

/* ---------- 图集 ---------- */
.gallery {
  position: relative;
}
.detail-swipe {
  height: 380px;
}
.detail-swipe-item {
  height: 380px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: var(--ink-800);
}
.detail-swipe-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
/* 底部暗角，让下一屏的玻璃卡自然衔接 */
.gallery-vignette {
  position: absolute;
  inset: auto 0 -1px 0;
  height: 96px;
  background: linear-gradient(180deg, transparent, rgba(5, 6, 11, 0.92));
  pointer-events: none;
}

/* ---------- 信息区 ---------- */
.info-section {
  position: relative;
  margin: -34px var(--gutter) 0;
  padding: 18px 16px 16px;
  border-radius: var(--r-lg);
  background-image: linear-gradient(158deg, rgba(255, 255, 255, 0.09), rgba(255, 255, 255, 0.03));
  border: 1px solid var(--glass-line-2);
  backdrop-filter: blur(var(--blur)) saturate(160%);
  box-shadow: var(--shadow-card);
}
.price-row {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin: 8px 0 10px;
}
.price-current {
  display: flex;
  align-items: baseline;
  gap: 2px;
  color: var(--neon);
  font-weight: 700;
}
.price-current .symbol {
  font-size: 15px;
}
.price-current .val {
  font-size: 32px;
  line-height: 1;
  text-shadow: 0 0 22px rgba(255, 46, 147, 0.6);
}
.price-old {
  font-size: 13px;
  color: var(--text-3);
  text-decoration: line-through;
}
.sales-badge {
  margin-left: auto;
  font-size: 11px;
  color: var(--text-2);
  background: var(--glass-2);
  border: 1px solid var(--glass-line);
  padding: 3px 10px;
  border-radius: var(--r-pill);
}
.product-name {
  font-family: var(--font-display);
  font-size: 17px;
  color: var(--text-1);
  line-height: 1.5;
  font-weight: 600;
  letter-spacing: 0.01em;
  margin-bottom: 10px;
}
.extra-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.extra-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: var(--text-2);
  padding: 3px 10px;
  border-radius: var(--r-pill);
  background: var(--glass);
  border: 1px solid var(--glass-line);
}

/* ---------- 通用区块 ---------- */
.section-block {
  margin: var(--sp-3) var(--gutter) 0;
  padding: 0 16px;
  border-radius: var(--r-md);
  background-image: linear-gradient(158deg, rgba(255, 255, 255, 0.06), rgba(255, 255, 255, 0.015));
  border: 1px solid var(--glass-line);
  backdrop-filter: blur(var(--blur)) saturate(150%);
  box-shadow: var(--shadow-card);
}
.section-row {
  display: flex;
  align-items: center;
  padding: 15px 0;
}
.section-label {
  font-size: 13px;
  color: var(--text-3);
  width: 62px;
  flex-shrink: 0;
  letter-spacing: 0.06em;
}
.section-value {
  flex: 1;
  font-size: 14px;
  color: var(--neon-soft);
  text-align: right;
  margin-right: 6px;
}
.section-head {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 16px 0 12px;
}
.section-title {
  font-family: var(--font-display);
  font-size: 16px;
  font-weight: 700;
  color: var(--text-1);
  letter-spacing: 0.05em;
}

/* ---------- 详情 ---------- */
.desc-content {
  padding-bottom: 16px;
}
.desc-subtitle {
  font-size: 13px;
  color: var(--text-2);
  margin-bottom: 8px;
}
.desc-text {
  font-size: 13px;
  color: var(--text-2);
  line-height: 1.7;
}
.desc-text :deep(img) {
  max-width: 100%;
  border-radius: var(--r-sm);
}
.desc-images {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.desc-img-el {
  width: 100%;
  border-radius: var(--r-sm);
  border: 1px solid var(--glass-line);
}

/* ---------- SKU 弹窗 ---------- */
.sku-panel {
  padding: 16px var(--gutter) 12px;
  max-height: 62vh;
  overflow-y: auto;
}
.sku-product-info {
  display: flex;
  gap: 14px;
  margin-bottom: 18px;
}
.sku-img {
  width: 84px;
  height: 84px;
  border-radius: var(--r-md);
  background: var(--glass-2);
  border: 1px solid var(--glass-line);
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
  display: flex;
  align-items: baseline;
  gap: 2px;
  color: var(--neon);
  font-weight: 700;
}
.sku-price .sym {
  font-size: 13px;
}
.sku-price .num {
  font-size: 26px;
  line-height: 1;
  text-shadow: 0 0 18px rgba(255, 46, 147, 0.55);
}
.sku-stock {
  font-size: 12px;
  color: var(--text-3);
  margin-top: 8px;
}
.sku-group {
  margin-bottom: 18px;
}
.sku-group-name {
  font-size: 13px;
  color: var(--text-2);
  margin-bottom: 10px;
  letter-spacing: 0.06em;
}
.sku-options {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.sku-option {
  display: inline-block;
  padding: 7px 18px;
  border: 1px solid var(--glass-line);
  border-radius: var(--r-pill);
  font-size: 13px;
  color: var(--text-2);
  background: var(--glass);
  cursor: pointer;
  transition: all var(--dur-fast) var(--ease-out);
}
.sku-option.active {
  border-color: rgba(255, 46, 147, 0.7);
  color: #fff;
  background-image: linear-gradient(135deg, rgba(255, 46, 147, 0.9), rgba(139, 92, 255, 0.8));
  box-shadow: var(--glow-neon);
}
.sku-option.disabled {
  color: var(--text-3);
  background: rgba(255, 255, 255, 0.02);
  text-decoration: line-through;
}
.sku-quantity {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 22px;
}
.qty-label {
  font-size: 13px;
  color: var(--text-2);
  letter-spacing: 0.06em;
}
.sku-confirm {
  padding-bottom: 14px;
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
  padding: 0 var(--gutter);
  gap: 10px;
  backdrop-filter: blur(var(--blur-lg)) saturate(160%);
  -webkit-backdrop-filter: blur(var(--blur-lg)) saturate(160%);
  background-image: linear-gradient(180deg, rgba(12, 14, 22, 0.72), rgba(8, 9, 15, 0.96));
  box-shadow: var(--shadow-float);
  z-index: 99;
}
.bar-icon {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  font-size: 10px;
  color: var(--text-2);
  width: 46px;
  flex-shrink: 0;
}
.bar-btn {
  flex: 1;
  height: 42px;
  line-height: 42px;
  text-align: center;
  border-radius: var(--r-pill);
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.04em;
}
.bar-btn-cart {
  background: var(--glass-2);
  border: 1px solid rgba(255, 46, 147, 0.5);
  color: var(--neon-soft);
}
.bar-btn-buy {
  background-image: linear-gradient(135deg, var(--neon), #ff5ea8 55%, var(--violet));
  color: #fff;
  box-shadow: var(--glow-neon);
}
</style>
