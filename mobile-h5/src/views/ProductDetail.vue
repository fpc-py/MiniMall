<template>
  <div class="detail-page">
    <!-- 顶部导航 -->
    <van-nav-bar
      :title="title"
      left-arrow
      fixed
      placeholder
      @click-left="$router.back()"
    />

    <!-- 商品图片轮播 -->
    <van-swipe
      :autoplay="3000"
      indicator-color="#FF2E93"
      class="detail-swipe"
    >
      <van-swipe-item v-for="(img, i) in product.images" :key="i">
        <div class="detail-swipe-item" :style="{ background: img.bg }">
          <van-icon :name="img.icon" :size="64" :color="img.color" />
        </div>
      </van-swipe-item>
    </van-swipe>

    <!-- 商品信息 -->
    <div class="info-section">
      <div class="price-row">
        <span class="price-current">
          <span class="symbol">¥</span>
          <span class="val">{{ product.price }}</span>
        </span>
        <span class="price-old">¥{{ product.oldPrice }}</span>
        <span class="sales-badge">已售 {{ product.sales }}</span>
      </div>
      <h1 class="product-name">{{ product.name }}</h1>
      <div class="extra-row">
        <span>运费：免运费</span>
        <span>{{ product.location }}</span>
      </div>
    </div>

    <!-- 规格选择 -->
    <div class="section-block" @click="showSku = true">
      <div class="section-row">
        <span class="section-label">已选</span>
        <span class="section-value">{{ selectedSku || '请选择规格' }}</span>
        <van-icon name="arrow" color="#ccc" />
      </div>
    </div>

    <!-- 商品评价 -->
    <div class="section-block">
      <div class="section-row" @click="showReviews = true">
        <span class="section-label">商品评价</span>
        <span class="section-value">查看全部 {{ product.reviewCount }} 条</span>
        <van-icon name="arrow" color="#ccc" />
      </div>
      <div class="review-preview" v-if="product.reviews && product.reviews.length">
        <div class="review-item" v-for="(r, i) in product.reviews.slice(0, 2)" :key="i">
          <div class="review-user">
            <span class="review-name">{{ r.user }}</span>
            <span class="review-time">{{ r.time }}</span>
          </div>
          <div class="review-content">{{ r.content }}</div>
        </div>
      </div>
    </div>

    <!-- 商品详情 -->
    <div class="section-block">
      <div class="section-title">商品详情</div>
      <div class="desc-content">
        <div
          class="desc-img"
          v-for="(desc, i) in product.description"
          :key="i"
          :style="{ background: desc.bg }"
        >
          <van-icon :name="desc.icon" :size="40" :color="desc.color" />
        </div>
      </div>
    </div>

    <!-- SKU 选择弹窗 -->
    <van-action-sheet v-model:show="showSku" title="选择规格" round>
      <div class="sku-panel">
        <div class="sku-product-info">
          <div class="sku-img">
            <van-icon :name="product.images[0]?.icon || 'photo-o'" :size="48" color="#FF2E93" />
          </div>
          <div class="sku-price-info">
            <div class="sku-price">¥{{ product.price }}</div>
            <div class="sku-stock">库存 {{ product.stock }} 件</div>
          </div>
        </div>
        <div class="sku-group" v-for="(spec, si) in product.specs" :key="si">
          <div class="sku-group-name">{{ spec.name }}</div>
          <div class="sku-options">
            <span
              v-for="(opt, oi) in spec.options"
              :key="oi"
              class="sku-option"
              :class="{ active: spec.selected === oi }"
              @click="spec.selected = oi; updateSku()"
            >{{ opt }}</span>
          </div>
        </div>
        <div class="sku-quantity">
          <span class="qty-label">数量</span>
          <van-stepper v-model="quantity" :min="1" :max="product.stock" />
        </div>
        <div class="sku-confirm">
          <van-button type="danger" block round @click="confirmSku">确定</van-button>
        </div>
      </div>
    </van-action-sheet>

    <!-- 评价弹窗 -->
    <van-action-sheet v-model:show="showReviews" title="商品评价" round>
      <div class="review-panel">
        <div class="review-full-item" v-for="(r, i) in product.reviews" :key="i">
          <div class="review-user">
            <span class="review-name">{{ r.user }}</span>
            <span class="review-time">{{ r.time }}</span>
          </div>
          <div class="review-content">{{ r.content }}</div>
        </div>
        <van-empty v-if="!product.reviews?.length" description="暂无评价" />
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
      <div class="bar-btn bar-btn-cart" @click="showSku = true">加入购物车</div>
      <div class="bar-btn bar-btn-buy" @click="handleBuy">立即购买</div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'

const route = useRoute()
const router = useRouter()
const title = ref('商品详情')
const showSku = ref(false)
const showReviews = ref(false)
const selectedSku = ref('')
const quantity = ref(1)

// 模拟商品数据
const product = reactive({
  id: route.params.id || 1,
  name: '旗舰智能手机 5G全网通 高清拍照 长续航 12GB+256GB 超强性能',
  price: '3999',
  oldPrice: '5999',
  sales: '1.2万',
  stock: 998,
  location: '广东深圳',
  reviewCount: 286,
  images: [
    { icon: 'phone-o', color: '#FF2E93', bg: 'linear-gradient(135deg, #fff5f5 0%, #ffe8f0 100%)' },
    { icon: 'photo-o', color: '#667eea', bg: 'linear-gradient(135deg, #f5f0ff 0%, #e8e0ff 100%)' },
    { icon: 'fire-o', color: '#f5576c', bg: 'linear-gradient(135deg, #fff5f0 0%, #ffe0e0 100%)' },
  ],
  specs: [
    {
      name: '颜色',
      selected: 0,
      options: ['曜石黑', '陶瓷白', '渐变蓝'],
    },
    {
      name: '内存',
      selected: 0,
      options: ['8GB+128GB', '12GB+256GB', '16GB+512GB'],
    },
  ],
  reviews: [
    { user: '小***明', time: '2026-09-10', content: '手机很好用，拍照清晰，运行流畅，电池耐用，性价比很高！' },
    { user: '购***家', time: '2026-09-08', content: '第二次购买了，给家人买的，质量一如既往的好，物流也快。' },
    { user: '数***迷', time: '2026-09-05', content: '外观漂亮，手感很好，系统流畅不卡顿，推荐购买！' },
  ],
  description: [
    { icon: 'photo-o', color: '#FF2E93', bg: '#fff5f5' },
    { icon: 'more-o', color: '#667eea', bg: '#f5f0ff' },
    { icon: 'fire-o', color: '#43e97b', bg: '#f0fff5' },
  ],
})

function updateSku() {
  const selections = product.specs.map(s => s.options[s.selected]).join(' / ')
  selectedSku.value = selections
}

function confirmSku() {
  updateSku()
  showSku.value = false
  showToast('已选择：' + selectedSku.value)
}

function handleBuy() {
  showSku.value = true
}

onMounted(() => {
  updateSku()
})
</script>

<style scoped>
.detail-page {
  background: #f5f5f5;
  min-height: 100vh;
  padding-bottom: 56px;
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

/* 评价预览 */
.review-preview {
  padding-bottom: 14px;
}
.review-item {
  margin-top: 10px;
}
.review-user {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
}
.review-name {
  font-size: 13px;
  color: #666;
}
.review-time {
  font-size: 11px;
  color: #ccc;
}
.review-content {
  font-size: 13px;
  color: #333;
  line-height: 1.5;
}

/* 详情图片 */
.desc-content {
  padding-bottom: 14px;
}
.desc-img {
  height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 8px;
  border-radius: 8px;
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

/* 评价弹窗 */
.review-panel {
  padding: 16px;
  max-height: 60vh;
  overflow-y: auto;
}
.review-full-item {
  padding: 12px 0;
  border-bottom: 1px solid #f5f5f5;
}
.review-full-item:last-child {
  border-bottom: none;
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
