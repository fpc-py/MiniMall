<template>
  <div class="page product-list-page">
    <van-nav-bar title="商品列表" left-arrow fixed placeholder @click-left="$router.back()" />

    <!-- 搜索框 -->
    <van-search
      v-model="keyword"
      placeholder="搜索商品名称 / 品牌"
      shape="round"
      background="transparent"
      @search="applyQuery"
      @clear="applyQuery"
    />

    <!-- 排序栏 -->
    <div class="sort-bar">
      <div
        class="sort-item"
        :class="{ active: activeSort === 'all' }"
        @click="changeSort('all')"
      >综合</div>
      <div
        class="sort-item"
        :class="{ active: activeSort === 'sales' }"
        @click="changeSort('sales')"
      >销量</div>
      <div
        class="sort-item"
        :class="{ active: activeSort === 'price' }"
        @click="changeSort('price')"
      >
        价格
        <span class="sort-arrow">
          <van-icon :name="priceAsc ? 'arrow-up' : 'arrow-down'" size="10" :color="activeSort === 'price' ? '#fff' : '#6d7490'" />
        </span>
      </div>
    </div>

    <!-- 商品列表 -->
    <van-list
      :key="listKey"
      v-model:loading="loading"
      :finished="finished"
      :error="error"
      finished-text="没有更多了"
      error-text="加载失败，点击重试"
      @load="onLoad"
    >
      <div class="product-grid">
        <div
          class="product-card"
          v-for="(item, i) in list"
          :key="item.id"
          :style="{ '--i': i % 8 }"
          @click="goDetail(item.id)"
        >
          <div class="product-img">
            <img v-if="item.mainImage" :src="item.mainImage" class="product-img-el" alt="" />
            <van-icon v-else name="photo-o" :size="48" color="#6d7490" />
            <span v-if="item.brand" class="brand-chip">{{ item.brand }}</span>
          </div>
          <div class="product-info">
            <div class="product-name van-multi-ellipsis--l2">{{ item.name }}</div>
            <div class="product-price-row">
              <span class="product-price">
                <span class="price-symbol">¥</span>
                <span class="price-val num">{{ money(item.minPrice) }}</span>
              </span>
            </div>
            <div class="product-sales">
              <span class="sales-dot"></span>已售 {{ item.totalSales || 0 }}
            </div>
          </div>
        </div>
      </div>
      <van-empty
        v-if="!loading && finished && !list.length"
        description="没有找到相关商品"
      />
    </van-list>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getProductList } from '@/api/product'

const route = useRoute()
const router = useRouter()

const PAGE_SIZE = 10

const keyword = ref(route.query.keyword ? String(route.query.keyword) : '')
const categoryId = ref(route.query.categoryId ? Number(route.query.categoryId) : null)

const activeSort = ref('all')
const priceAsc = ref(true)
const listKey = ref(0)

const list = ref([])
const pageNum = ref(1)
const loading = ref(false)
const finished = ref(false)
const error = ref(false)

const sortParam = computed(() => {
  if (activeSort.value === 'sales') return 'sales'
  if (activeSort.value === 'price') return priceAsc.value ? 'priceAsc' : 'priceDesc'
  return undefined
})

function money(v) {
  return Number(v || 0).toFixed(2)
}

function buildParams(page) {
  const params = { pageNum: page, pageSize: PAGE_SIZE }
  if (keyword.value) params.keyword = keyword.value
  if (categoryId.value) params.categoryId = categoryId.value
  if (sortParam.value) params.sort = sortParam.value
  return params
}

async function onLoad() {
  try {
    const res = await getProductList(buildParams(pageNum.value))
    const data = res.data || {}
    const items = data.list || []
    list.value = pageNum.value === 1 ? items : list.value.concat(items)
    pageNum.value += 1
    const total = data.total != null ? data.total : list.value.length
    if (list.value.length >= total || items.length === 0) {
      finished.value = true
    }
    error.value = false
  } catch (e) {
    error.value = true
    finished.value = false
  } finally {
    loading.value = false
  }
}

function applyQuery() {
  resetAndReload()
}

function changeSort(type) {
  if (type === 'price') {
    if (activeSort.value === 'price') {
      priceAsc.value = !priceAsc.value
    } else {
      activeSort.value = 'price'
      priceAsc.value = true
    }
  } else {
    activeSort.value = type
  }
  resetAndReload()
}

function resetAndReload() {
  pageNum.value = 1
  list.value = []
  finished.value = false
  error.value = false
  loading.value = false
  listKey.value += 1
}

function goDetail(id) {
  router.push(`/product/${id}`)
}

// 路由 query 变化（如从首页不同分类进入）时同步并重载
watch(
  () => route.query,
  (q) => {
    keyword.value = q.keyword ? String(q.keyword) : ''
    categoryId.value = q.categoryId ? Number(q.categoryId) : null
    resetAndReload()
  }
)
</script>

<style scoped>
.product-list-page {
  padding-bottom: 24px;
}

/* ---------- 排序栏：玻璃胶囊分段器 ---------- */
.sort-bar {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 4px var(--gutter) 0;
  padding: 4px;
  border-radius: var(--r-pill);
  background: var(--glass);
  border: 1px solid var(--glass-line);
  backdrop-filter: blur(var(--blur)) saturate(150%);
}
.sort-item {
  flex: 1;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 3px;
  border-radius: var(--r-pill);
  font-size: 13px;
  color: var(--text-2);
  letter-spacing: 0.04em;
  transition: color var(--dur-fast) var(--ease-out), background var(--dur-fast) var(--ease-out);
}
.sort-item.active {
  color: #fff;
  font-weight: 600;
  background-image: linear-gradient(135deg, var(--neon), rgba(139, 92, 255, 0.92));
  box-shadow: 0 6px 18px -8px rgba(255, 46, 147, 0.9);
}
.sort-arrow {
  display: inline-flex;
  flex-direction: column;
}

/* ---------- 列表 ---------- */
.product-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  padding: 14px var(--gutter) 0;
}
.product-card {
  border-radius: var(--r-md);
  overflow: hidden;
  background-image: linear-gradient(160deg, rgba(255, 255, 255, 0.07), rgba(255, 255, 255, 0.02));
  border: 1px solid var(--glass-line);
  box-shadow: var(--shadow-card);
  animation: riseIn 0.55s var(--ease-out) both;
  animation-delay: calc(var(--i, 0) * 62ms);
  transition: transform var(--dur-fast) var(--ease-spring), border-color var(--dur-fast);
}
.product-card:active {
  transform: scale(0.974);
  border-color: rgba(255, 46, 147, 0.42);
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
.product-img::after {
  content: "";
  position: absolute;
  inset: auto 0 0 0;
  height: 44%;
  background: linear-gradient(180deg, transparent, rgba(5, 6, 11, 0.72));
  pointer-events: none;
}
/* 品牌角标：贴图左上，制造非对称信息层级 */
.brand-chip {
  position: absolute;
  left: 8px;
  top: 8px;
  z-index: 1;
  padding: 2px 8px;
  font-family: var(--font-num);
  font-size: 10px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--cyan-soft);
  background: rgba(5, 6, 11, 0.62);
  border: 1px solid rgba(36, 229, 255, 0.36);
  border-radius: var(--r-pill);
  backdrop-filter: blur(8px);
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
  display: flex;
  align-items: baseline;
  gap: 1px;
  color: var(--neon);
  font-weight: 700;
}
.product-price .price-symbol {
  font-size: 11px;
}
.product-price .price-val {
  font-size: 17px;
  text-shadow: 0 0 14px rgba(255, 46, 147, 0.6);
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
</style>
