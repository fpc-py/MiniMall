<template>
  <div class="page home-page">
    <!-- 顶部搜索栏：悬浮毛玻璃胶囊 -->
    <header class="topbar">
      <div class="search-bar" @click="$router.push('/product/list')">
        <van-icon name="search" size="16" color="#a5abc7" />
        <span class="search-text">搜一搜心仪好物</span>
      </div>
      <div class="coupon-entry" @click="$router.push('/coupons')">
        <span class="coupon-orb">
          <van-icon name="coupon-o" size="18" color="#fff" />
        </span>
        <span class="coupon-label">领券</span>
      </div>
    </header>

    <!-- 轮播图（本地静态运营位，无 CMS） -->
    <section class="hero rv" style="--i: 0">
      <van-swipe :autoplay="3000" indicator-color="#ff2e93" lazy-render>
        <van-swipe-item v-for="(banner, i) in banners" :key="i">
          <div class="hero-card" :style="{ background: banner.bg }">
            <span class="hero-eyebrow">{{ banner.eyebrow }}</span>
            <span class="hero-title">{{ banner.title }}</span>
            <span class="hero-sub">{{ banner.sub }}</span>
            <span class="hero-sheen"></span>
          </div>
        </van-swipe-item>
      </van-swipe>
    </section>

    <!-- 金刚区 -->
    <section class="nav-grid-section rv" style="--i: 1">
      <van-skeleton v-if="loadingCategories" title :row="2" style="padding: 8px" />
      <div v-else class="nav-grid">
        <div
          class="nav-item"
          v-for="(item, i) in categories"
          :key="item.id"
          :style="{ '--i': i }"
          @click="goCategory(item.id)"
        >
          <div class="nav-icon">
            <van-icon :name="item.icon || 'apps-o'" :size="22" color="#fff" />
          </div>
          <span class="nav-label">{{ item.name }}</span>
        </div>
      </div>
      <van-empty v-if="!loadingCategories && !categories.length" description="暂无分类" image-size="60" />
    </section>

    <!-- 热销榜 -->
    <section class="hot-section rv" style="--i: 2">
      <div class="hot-header">
        <div class="hot-title-left">
          <span class="eyebrow">HOT RANKING</span>
          <span class="hot-label">热销榜</span>
        </div>
        <span class="hot-more" @click="$router.push('/product/list')">更多 ›</span>
      </div>
      <van-skeleton v-if="loadingHot" title :row="1" style="padding: 8px" />
      <div v-else-if="hotList.length" class="hot-list">
        <div
          class="hot-item"
          v-for="(item, i) in hotList"
          :key="item.id"
          :style="{ '--i': i }"
          @click="goProduct(item.id)"
        >
          <div class="hot-img">
            <img v-if="item.mainImage" :src="item.mainImage" class="hot-img-el" alt="" />
            <van-icon v-else name="photo-o" :size="36" color="#6d7490" />
            <span class="hot-rank" :class="'rank-' + (i + 1)">{{ String(i + 1).padStart(2, '0') }}</span>
          </div>
          <div class="hot-price">
            <span class="price-symbol">¥</span>
            <span class="price-val num">{{ money(item.minPrice) }}</span>
          </div>
        </div>
      </div>
      <van-empty v-else description="暂无热销商品" image-size="60" />
    </section>

    <!-- 推荐商品 -->
    <section class="recommend-section">
      <div class="section-title rv" style="--i: 3">
        <span class="eyebrow">FOR YOU</span>
        <span class="title-text">为你推荐</span>
      </div>
      <van-skeleton v-if="loadingRecommend" title :row="3" style="padding: 8px" />
      <div v-else-if="recommendList.length" class="product-grid">
        <div
          class="product-card"
          v-for="(item, i) in recommendList"
          :key="item.id"
          :style="{ '--i': i }"
          @click="goProduct(item.id)"
        >
          <div class="product-img">
            <img v-if="item.mainImage" :src="item.mainImage" class="product-img-el" alt="" />
            <van-icon v-else name="photo-o" :size="48" color="#6d7490" />
          </div>
          <div class="product-info">
            <div class="product-name van-multi-ellipsis--l2">{{ item.name }}</div>
            <div class="product-price-row">
              <span class="product-price">
                <span class="price-symbol">¥</span>
                <span class="price-val num">{{ money(item.minPrice) }}</span>
              </span>
              <span v-if="showMaxPrice(item)" class="product-old-price num">
                最高 ¥{{ money(item.maxPrice) }}
              </span>
            </div>
            <div class="product-sales">
              <span class="sales-dot"></span>已售 {{ item.totalSales || 0 }}
            </div>
          </div>
        </div>
      </div>
      <van-empty v-else description="暂无推荐商品" image-size="60" />
    </section>

    <!-- 智能客服悬浮入口 -->
    <div class="cs-fab" @click="$router.push('/customer-service')">
      <span class="cs-fab-ring" aria-hidden="true"></span>
      <van-icon name="service-o" size="22" color="#fff" />
      <span class="cs-fab-text">客服</span>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getCategoryTree, getProductList } from '@/api/product'

const router = useRouter()

// 轮播数据（本地静态运营位）——视觉层：暗夜霓虹渐变
const banners = [
  {
    bg: 'radial-gradient(120% 150% at 6% 8%, rgba(255,46,147,.95) 0%, rgba(201,5,111,.42) 46%, rgba(10,12,20,.94) 100%)',
    eyebrow: 'SEASON 01',
    title: '夏日焕新季',
    sub: '全场低至 5 折',
  },
  {
    bg: 'radial-gradient(120% 150% at 96% 10%, rgba(36,229,255,.82) 0%, rgba(20,86,150,.38) 46%, rgba(10,12,20,.94) 100%)',
    eyebrow: 'DIGITAL',
    title: '数码狂欢节',
    sub: '爆款直降',
  },
  {
    bg: 'radial-gradient(120% 150% at 18% 92%, rgba(139,92,255,.88) 0%, rgba(88,40,158,.34) 48%, rgba(10,12,20,.94) 100%)',
    eyebrow: 'BEAUTY',
    title: '美妆特卖',
    sub: '大牌满减',
  },
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
  padding-bottom: 66px;
}

/* ---------- 顶部搜索 ---------- */
.topbar {
  position: sticky;
  top: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  gap: var(--sp-3);
  padding: 10px var(--gutter);
  backdrop-filter: blur(var(--blur-lg)) saturate(150%);
  -webkit-backdrop-filter: blur(var(--blur-lg)) saturate(150%);
  background: linear-gradient(180deg, rgba(5, 6, 11, 0.86), rgba(5, 6, 11, 0.4));
}
.search-bar {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
  height: 40px;
  padding: 0 14px;
  border-radius: var(--r-pill);
  background: var(--glass);
  border: 1px solid var(--glass-line);
  backdrop-filter: blur(var(--blur));
}
.search-text {
  font-size: 13px;
  color: var(--text-3);
  letter-spacing: 0.04em;
}
.coupon-entry {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
  flex-shrink: 0;
}
.coupon-orb {
  width: 32px;
  height: 32px;
  border-radius: var(--r-pill);
  display: flex;
  align-items: center;
  justify-content: center;
  background-image: linear-gradient(135deg, var(--neon), var(--violet));
  box-shadow: var(--glow-neon);
}
.coupon-label {
  font-size: 10px;
  color: var(--neon-soft);
  letter-spacing: 0.1em;
}

/* ---------- Hero 轮播 ---------- */
.hero {
  padding: 0 var(--gutter);
  margin-top: 4px;
}
.hero-card {
  position: relative;
  height: 152px;
  border-radius: var(--r-lg);
  padding: 20px 22px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  overflow: hidden;
  border: 1px solid var(--glass-line-2);
  box-shadow: var(--shadow-card);
}
.hero-eyebrow {
  font-family: var(--font-num);
  font-size: 9px;
  letter-spacing: 0.34em;
  color: rgba(255, 255, 255, 0.72);
  margin-bottom: 6px;
}
.hero-title {
  font-family: var(--font-display);
  font-size: 27px;
  font-weight: 700;
  line-height: 1.1;
  color: #fff;
  text-shadow: 0 4px 26px rgba(0, 0, 0, 0.6);
}
.hero-sub {
  font-size: 12px;
  margin-top: 8px;
  color: rgba(255, 255, 255, 0.8);
  letter-spacing: 0.06em;
}
/* 斜向扫光，每张 banner 循环一次 */
.hero-sheen {
  position: absolute;
  top: 0;
  left: 0;
  width: 42%;
  height: 100%;
  background: linear-gradient(100deg, transparent, rgba(255, 255, 255, 0.22), transparent);
  animation: sheen 4.6s var(--ease-out) infinite;
  pointer-events: none;
}

/* ---------- 金刚区 ---------- */
.nav-grid-section {
  margin: var(--sp-3) var(--gutter) 0;
  padding: 16px 6px;
  border-radius: var(--r-lg);
  background-image: linear-gradient(158deg, rgba(255, 255, 255, 0.07), rgba(255, 255, 255, 0.02));
  border: 1px solid var(--glass-line);
  backdrop-filter: blur(var(--blur)) saturate(150%);
  box-shadow: var(--shadow-card);
}
.nav-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 14px 0;
}
.nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 7px;
  animation: riseIn 0.5s var(--ease-out) both;
  animation-delay: calc(var(--i, 0) * 55ms);
}
.nav-icon {
  width: 42px;
  height: 42px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-image: linear-gradient(140deg, rgba(255, 46, 147, 0.9), rgba(139, 92, 255, 0.85));
  box-shadow: 0 8px 22px -10px rgba(255, 46, 147, 0.9), inset 0 1px 0 rgba(255, 255, 255, 0.3);
}
.nav-label {
  font-size: 11px;
  color: var(--text-2);
  letter-spacing: 0.02em;
}

/* ---------- 热销榜 ---------- */
.hot-section {
  margin: var(--sp-3) var(--gutter) 0;
  padding: 14px 14px 12px;
  border-radius: var(--r-lg);
  background-image: linear-gradient(158deg, rgba(255, 255, 255, 0.06), rgba(255, 255, 255, 0.015));
  border: 1px solid var(--glass-line);
  backdrop-filter: blur(var(--blur)) saturate(150%);
  box-shadow: var(--shadow-card);
}
.hot-header {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  margin-bottom: 14px;
}
.hot-title-left {
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.hot-label {
  font-family: var(--font-display);
  font-size: 17px;
  font-weight: 700;
  color: var(--text-1);
  letter-spacing: 0.02em;
}
.hot-more {
  margin-left: auto;
  font-size: 12px;
  color: var(--neon-soft);
  letter-spacing: 0.06em;
}
.hot-list {
  display: flex;
  gap: 14px;
  overflow-x: auto;
  padding-bottom: 4px;
}
.hot-item {
  flex-shrink: 0;
  width: 84px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 7px;
  animation: riseIn 0.5s var(--ease-out) both;
  animation-delay: calc(var(--i, 0) * 60ms);
}
.hot-img {
  position: relative;
  width: 70px;
  height: 70px;
  border-radius: var(--r-sm);
  background: var(--glass-2);
  border: 1px solid var(--glass-line);
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
/* 排名数字：DIN 字腔，前三名给霓虹 */
.hot-rank {
  position: absolute;
  left: 0;
  top: 0;
  padding: 1px 5px 2px;
  font-family: var(--font-num);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: var(--text-2);
  background: rgba(5, 6, 11, 0.72);
  border-bottom-right-radius: var(--r-xs);
}
.rank-1 {
  color: #fff;
  background-image: linear-gradient(135deg, var(--neon), var(--violet));
  box-shadow: 0 0 12px rgba(255, 46, 147, 0.85);
}
.rank-2 {
  color: #05060b;
  background-image: linear-gradient(135deg, var(--cyan-soft), var(--cyan));
}
.rank-3 {
  color: #05060b;
  background-image: linear-gradient(135deg, #ffd479, var(--warn));
}
.hot-price {
  display: flex;
  align-items: baseline;
  gap: 1px;
  color: var(--neon-soft);
}
.hot-price .price-symbol {
  font-size: 10px;
}
.hot-price .price-val {
  font-size: 15px;
  font-weight: 700;
}

/* ---------- 推荐 ---------- */
.recommend-section {
  padding: var(--sp-5) var(--gutter) 0;
}
.section-title {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 0 0 16px;
}
.section-title .eyebrow {
  color: rgba(255, 46, 147, 0.75);
}
.title-text {
  position: relative;
  font-family: var(--font-display);
  font-size: 19px;
  font-weight: 700;
  color: var(--text-1);
  letter-spacing: 0.06em;
}
.title-text::after {
  content: "";
  position: absolute;
  left: 50%;
  bottom: -7px;
  width: 40px;
  height: 2px;
  transform: translateX(-50%);
  border-radius: var(--r-pill);
  background: linear-gradient(90deg, var(--neon), var(--cyan));
  box-shadow: 0 0 10px rgba(255, 46, 147, 0.9);
}

.product-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.product-card {
  border-radius: var(--r-md);
  overflow: hidden;
  background-image: linear-gradient(160deg, rgba(255, 255, 255, 0.07), rgba(255, 255, 255, 0.02));
  border: 1px solid var(--glass-line);
  box-shadow: var(--shadow-card);
  animation: riseIn 0.55s var(--ease-out) both;
  animation-delay: calc(var(--i, 0) * 65ms);
  transition: transform var(--dur-fast) var(--ease-spring), border-color var(--dur-fast);
}
.product-card:active {
  transform: scale(0.976);
  border-color: rgba(255, 46, 147, 0.4);
}
.product-img {
  position: relative;
  height: 158px;
  background: var(--glass);
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
/* 图片压暗，让霓虹价格浮上来 */
.product-img::after {
  content: "";
  position: absolute;
  inset: auto 0 0 0;
  height: 46%;
  background: linear-gradient(180deg, transparent, rgba(5, 6, 11, 0.72));
  pointer-events: none;
}
.product-info {
  padding: 10px 12px 12px;
}
.product-name {
  font-size: 13px;
  color: var(--text-1);
  line-height: 1.42;
  min-height: 37px;
}
.product-price-row {
  display: flex;
  align-items: baseline;
  gap: 6px;
  margin-top: 6px;
}
.product-price {
  color: var(--neon);
  font-weight: 700;
  display: flex;
  align-items: baseline;
  gap: 1px;
}
.product-price .price-symbol {
  font-size: 11px;
}
.product-price .price-val {
  font-size: 17px;
  text-shadow: 0 0 14px rgba(255, 46, 147, 0.6);
}
.product-old-price {
  font-size: 10px;
  color: var(--text-3);
  text-decoration: line-through;
}
.product-sales {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 11px;
  color: var(--text-3);
  margin-top: 5px;
}
.sales-dot {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: var(--cyan);
  box-shadow: 0 0 6px var(--cyan);
}

/* ---------- 智能客服悬浮入口 ---------- */
.cs-fab {
  position: fixed;
  right: var(--gutter);
  bottom: calc(74px + env(safe-area-inset-bottom));
  z-index: 120;
  width: 52px;
  height: 52px;
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 1px;
  background-image: linear-gradient(135deg, var(--neon), #ff5ea8 55%, var(--violet));
  box-shadow: var(--glow-neon);
  animation: floatY 3.6s var(--ease-out) infinite;
  transition: transform var(--dur-fast) var(--ease-spring);
}
.cs-fab:active {
  transform: scale(0.9);
}
.cs-fab-ring {
  position: absolute;
  inset: -7px;
  border-radius: 50%;
  background: conic-gradient(from 140deg, var(--neon), var(--cyan), var(--violet), var(--neon));
  opacity: 0.55;
  filter: blur(9px);
  z-index: 0;
  animation: glowPulse 3.2s var(--ease-out) infinite;
}
.cs-fab .van-icon {
  position: relative;
  z-index: 1;
}
.cs-fab-text {
  position: relative;
  z-index: 1;
  font-size: 10px;
  color: #fff;
  letter-spacing: 0.06em;
}
</style>
