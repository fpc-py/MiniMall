<template>
  <div class="home-page">
    <!-- 顶部搜索栏 -->
    <div class="search-header">
      <div class="search-bar" @click="$router.push('/product/list')">
        <van-icon name="search" size="16" color="#999" />
        <span class="search-text">搜一搜心仪好物</span>
      </div>
      <div class="coupon-entry" @click="$router.push('/coupons')">
        <van-icon name="coupon-o" size="18" color="#FF2E93" />
        <span>领券</span>
      </div>
    </div>

    <!-- 轮播图（本地静态运营位，无 CMS） -->
    <div class="swiper-section">
      <van-swipe :autoplay="3000" indicator-color="#FF2E93" lazy-render>
        <van-swipe-item v-for="(banner, i) in banners" :key="i">
          <div class="swiper-item" :style="{ background: banner.bg }">
            <span class="swiper-title">{{ banner.title }}</span>
            <span class="swiper-sub">{{ banner.sub }}</span>
          </div>
        </van-swipe-item>
      </van-swipe>
    </div>

    <!-- 金刚区 -->
    <div class="nav-grid-section">
      <van-skeleton v-if="loadingCategories" title :row="2" style="padding: 8px" />
      <div v-else class="nav-grid">
        <div
          class="nav-item"
          v-for="item in categories"
          :key="item.id"
          @click="goCategory(item.id)"
        >
          <div class="nav-icon">
            <van-icon :name="item.icon || 'apps-o'" :size="24" color="#fff" />
          </div>
          <span class="nav-label">{{ item.name }}</span>
        </div>
      </div>
      <van-empty v-if="!loadingCategories && !categories.length" description="暂无分类" image-size="60" />
    </div>

    <!-- 热销榜 -->
    <div class="hot-section">
      <div class="hot-header">
        <div class="hot-title-left">
          <span class="hot-label">热销榜</span>
          <span class="hot-tag">销量精选</span>
        </div>
        <span class="hot-more" @click="$router.push('/product/list')">更多 ›</span>
      </div>
      <van-skeleton v-if="loadingHot" title :row="1" style="padding: 8px" />
      <div v-else-if="hotList.length" class="hot-list">
        <div
          class="hot-item"
          v-for="item in hotList"
          :key="item.id"
          @click="goProduct(item.id)"
        >
          <div class="hot-img">
            <img v-if="item.mainImage" :src="item.mainImage" class="hot-img-el" alt="" />
            <van-icon v-else name="photo-o" :size="36" color="#ccc" />
          </div>
          <div class="hot-price">
            <span class="price-symbol">¥</span>
            <span class="price-val">{{ money(item.minPrice) }}</span>
          </div>
        </div>
      </div>
      <van-empty v-else description="暂无热销商品" image-size="60" />
    </div>

    <!-- 推荐商品 -->
    <div class="recommend-section">
      <div class="section-title">
        <span class="title-line"></span>
        <span class="title-text">为你推荐</span>
        <span class="title-line"></span>
      </div>
      <van-skeleton v-if="loadingRecommend" title :row="3" style="padding: 8px" />
      <div v-else-if="recommendList.length" class="product-grid">
        <div
          class="product-card"
          v-for="item in recommendList"
          :key="item.id"
          @click="goProduct(item.id)"
        >
          <div class="product-img">
            <img v-if="item.mainImage" :src="item.mainImage" class="product-img-el" alt="" />
            <van-icon v-else name="photo-o" :size="48" color="#ccc" />
          </div>
          <div class="product-info">
            <div class="product-name van-multi-ellipsis--l2">{{ item.name }}</div>
            <div class="product-price-row">
              <span class="product-price">
                <span class="price-symbol">¥</span>
                <span class="price-val">{{ money(item.minPrice) }}</span>
              </span>
              <span
                v-if="showMaxPrice(item)"
                class="product-old-price"
              >最高 ¥{{ money(item.maxPrice) }}</span>
            </div>
            <div class="product-sales">已售 {{ item.totalSales || 0 }}</div>
          </div>
        </div>
      </div>
      <van-empty v-else description="暂无推荐商品" image-size="60" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getCategoryTree, getProductList } from '@/api/product'

const router = useRouter()

// 轮播数据（本地静态运营位）
const banners = [
  { bg: 'linear-gradient(135deg, #FF2E93 0%, #FF6B9D 100%)', title: '夏日焕新季', sub: '全场低至5折' },
  { bg: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)', title: '数码狂欢节', sub: '爆款直降' },
  { bg: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)', title: '美妆特卖', sub: '大牌满减' },
]

const categories = ref([])
const hotList = ref([])
const recommendList = ref([])

const loadingCategories = ref(false)
const loadingHot = ref(false)
const loadingRecommend = ref(false)

function money(v) {
  return Number(v || 0).toFixed(2)
}

function showMaxPrice(item) {
  return item.maxPrice != null && Number(item.maxPrice) > Number(item.minPrice || 0)
}

async function loadCategories() {
  loadingCategories.value = true
  try {
    const res = await getCategoryTree()
    const tree = res.data || []
    categories.value = tree.filter((c) => c.level === 1 || c.parentId === 0 || c.parentId == null)
  } catch (e) {
    categories.value = []
  } finally {
    loadingCategories.value = false
  }
}

async function loadHot() {
  loadingHot.value = true
  try {
    const res = await getProductList({ pageNum: 1, pageSize: 6, sort: 'sales' })
    hotList.value = res.data?.list || []
  } catch (e) {
    hotList.value = []
  } finally {
    loadingHot.value = false
  }
}

async function loadRecommend() {
  loadingRecommend.value = true
  try {
    const res = await getProductList({ pageNum: 1, pageSize: 10 })
    recommendList.value = res.data?.list || []
  } catch (e) {
    recommendList.value = []
  } finally {
    loadingRecommend.value = false
  }
}

function goCategory(id) {
  router.push({ path: '/product/list', query: { categoryId: id } })
}

function goProduct(id) {
  if (!id) {
    showToast('商品信息异常')
    return
  }
  router.push(`/product/${id}`)
}

onMounted(() => {
  loadCategories()
  loadHot()
  loadRecommend()
})
</script>

<style scoped>
.home-page {
  padding-bottom: 50px;
  background: #f5f5f5;
  min-height: 100vh;
}

/* 顶部搜索栏 */
.search-header {
  display: flex;
  align-items: center;
  padding: 10px 16px;
  background: #fff;
  gap: 12px;
  position: sticky;
  top: 0;
  z-index: 100;
}
.search-bar {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 6px;
  height: 36px;
  background: #f5f5f5;
  border-radius: 18px;
  padding: 0 14px;
}
.search-text {
  color: #999;
  font-size: 13px;
}
.coupon-entry {
  display: flex;
  flex-direction: column;
  align-items: center;
  font-size: 10px;
  color: #FF2E93;
  flex-shrink: 0;
}

/* 轮播 */
.swiper-section {
  padding: 0 12px;
  margin-top: 8px;
}
.swiper-item {
  height: 140px;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: #fff;
}
.swiper-title {
  font-size: 22px;
  font-weight: 700;
}
.swiper-sub {
  font-size: 14px;
  margin-top: 6px;
  opacity: 0.9;
}

/* 金刚区 */
.nav-grid-section {
  background: #fff;
  margin: 10px 12px;
  border-radius: 12px;
  padding: 16px 8px;
}
.nav-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px 0;
}
.nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}
.nav-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #FF2E93, #ff6b9d);
}
.nav-label {
  font-size: 12px;
  color: #333;
}

/* 热销榜 */
.hot-section {
  background: #fff;
  margin: 0 12px 10px;
  border-radius: 12px;
  padding: 14px 12px;
}
.hot-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}
.hot-title-left {
  display: flex;
  align-items: center;
  gap: 8px;
}
.hot-label {
  font-size: 16px;
  font-weight: 700;
  color: #FF2E93;
}
.hot-tag {
  font-size: 10px;
  background: #FF2E93;
  color: #fff;
  padding: 1px 6px;
  border-radius: 4px;
}
.hot-more {
  margin-left: auto;
  font-size: 12px;
  color: #999;
}
.hot-list {
  display: flex;
  gap: 12px;
  overflow-x: auto;
  padding-bottom: 4px;
}
.hot-list::-webkit-scrollbar {
  display: none;
}
.hot-item {
  flex-shrink: 0;
  width: 80px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}
.hot-img {
  width: 64px;
  height: 64px;
  background: #fef0f5;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}
.hot-img-el {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.hot-price {
  color: #FF2E93;
  font-weight: 700;
}
.hot-price .price-symbol {
  font-size: 10px;
}
.hot-price .price-val {
  font-size: 16px;
}

/* 推荐商品 */
.recommend-section {
  padding: 0 12px;
}
.section-title {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 16px 0 12px;
}
.title-line {
  display: block;
  width: 30px;
  height: 2px;
  background: linear-gradient(90deg, transparent, #FF2E93, transparent);
}
.title-text {
  font-size: 15px;
  font-weight: 700;
  color: #333;
}

.product-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}
.product-card {
  background: #fff;
  border-radius: 10px;
  overflow: hidden;
}
.product-img {
  height: 150px;
  background: #fafafa;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}
.product-img-el {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.product-info {
  padding: 8px 10px 10px;
}
.product-name {
  font-size: 13px;
  color: #222;
  line-height: 1.4;
  min-height: 36px;
}
.product-price-row {
  display: flex;
  align-items: baseline;
  gap: 6px;
  margin-top: 4px;
}
.product-price {
  color: #FF2E93;
  font-weight: 700;
}
.product-price .price-symbol {
  font-size: 11px;
}
.product-price .price-val {
  font-size: 16px;
}
.product-old-price {
  font-size: 11px;
  color: #ccc;
}
.product-sales {
  font-size: 11px;
  color: #999;
  margin-top: 2px;
}
</style>
