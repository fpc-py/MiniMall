<template>
  <div class="product-list-page">
    <van-nav-bar title="商品列表" left-arrow fixed placeholder @click-left="$router.back()" />

    <!-- 搜索框 -->
    <van-search
      v-model="keyword"
      placeholder="搜索商品名称 / 品牌"
      shape="round"
      background="#fff"
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
          <van-icon :name="priceAsc ? 'arrow-up' : 'arrow-down'" :color="activeSort === 'price' ? '#FF2E93' : '#ccc'" />
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
          v-for="item in list"
          :key="item.id"
          @click="goDetail(item.id)"
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
              <span v-if="item.brand" class="product-brand">{{ item.brand }}</span>
            </div>
            <div class="product-sales">已售 {{ item.totalSales || 0 }}</div>
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
  background: #f5f5f5;
  min-height: 100vh;
  padding-bottom: 20px;
}

.sort-bar {
  display: flex;
  align-items: center;
  background: #fff;
  padding: 10px 0;
  border-bottom: 1px solid #f5f5f5;
}
.sort-item {
  flex: 1;
  text-align: center;
  font-size: 13px;
  color: #333;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
}
.sort-item.active {
  color: #FF2E93;
  font-weight: 600;
}
.sort-arrow {
  display: inline-flex;
  flex-direction: column;
}

.product-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  padding: 10px 12px;
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
.product-brand {
  font-size: 11px;
  color: #999;
}
.product-sales {
  font-size: 11px;
  color: #999;
  margin-top: 2px;
}
</style>
