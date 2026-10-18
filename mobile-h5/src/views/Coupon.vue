<template>
  <div class="coupon-page">
    <van-nav-bar title="优惠券" left-arrow fixed placeholder @click-left="router.back()" />

    <!-- 主 tab -->
    <van-tabs v-model:active="mainTab" sticky offset-top="46" color="#FF2E93" line-width="20" @change="onMainTabChange">
      <van-tab title="领券中心" />
      <van-tab title="我的券" />
    </van-tabs>

    <!-- 领券中心 -->
    <div v-show="mainTab === 0" class="center-list">
      <van-empty v-if="!loadingCenter && !centerList.length" description="暂无可领取优惠券" />
      <div
        class="coupon-card"
        v-for="c in centerList"
        :key="c.id"
      >
        <div class="coupon-left">
          <div class="coupon-value">{{ couponValue(c) }}</div>
          <div class="coupon-threshold">满¥{{ money(c.thresholdAmount) }}可用</div>
        </div>
        <div class="coupon-mid">
          <div class="coupon-name">{{ c.name }}</div>
          <div class="coupon-scope">{{ c.shopId == null ? '平台通用' : '店铺专享' }}</div>
          <div class="coupon-expire">{{ formatDate(c.startTime) }} ~ {{ formatDate(c.endTime) }}</div>
          <div class="coupon-remain">剩余 {{ c.remainCount }}/{{ c.totalCount }}</div>
        </div>
        <van-button
          class="coupon-btn"
          round
          size="small"
          type="danger"
          color="#FF2E93"
          :disabled="c.received || (c.remainCount || 0) <= 0"
          @click="handleReceive(c)"
        >
          {{ c.received ? '已领取' : ((c.remainCount || 0) <= 0 ? '已抢光' : '立即领取') }}
        </van-button>
      </div>
    </div>

    <!-- 我的券 -->
    <div v-show="mainTab === 1" class="my-list">
      <van-tabs v-model:active="myStatus" color="#FF2E93" line-width="16" @change="loadMyCoupons">
        <van-tab title="未使用" />
        <van-tab title="已使用" />
        <van-tab title="已过期" />
      </van-tabs>
      <van-empty v-if="!loadingMy && !myList.length" description="暂无优惠券" />
      <div
        class="coupon-card"
        :class="{ grey: myStatus !== 0 }"
        v-for="c in myList"
        :key="c.id"
      >
        <div class="coupon-left">
          <div class="coupon-value">{{ couponValue(c) }}</div>
          <div class="coupon-threshold">满¥{{ money(c.thresholdAmount) }}可用</div>
        </div>
        <div class="coupon-mid">
          <div class="coupon-name">{{ c.name }}</div>
          <div class="coupon-scope">{{ c.shopId == null ? '平台通用' : '店铺专享' }}</div>
          <div class="coupon-expire">有效期至 {{ formatDate(c.expireTime) }}</div>
        </div>
        <div class="coupon-state">{{ myStatusText(c.status) }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getCoupons, receiveCoupon, getMyCoupons } from '@/api/coupon'

const router = useRouter()

const mainTab = ref(0)
const myStatus = ref(0)

const centerList = ref([])
const loadingCenter = ref(false)

const myList = ref([])
const loadingMy = ref(false)

function money(v) {
  return Number(v || 0).toFixed(2)
}

function formatDate(t) {
  if (!t) return '-'
  return String(t).slice(0, 10)
}

function couponValue(c) {
  if (c.type === 2) {
    const rate = Number(c.discountRate)
    return `${Number((rate * 10).toFixed(1))}折`
  }
  return `¥${money(c.discountAmount)}`
}

function myStatusText(status) {
  const map = { 0: '未使用', 1: '已使用', 2: '已过期' }
  return map[status] || ''
}

async function loadCenter() {
  loadingCenter.value = true
  try {
    const res = await getCoupons()
    centerList.value = res.data || []
  } catch (e) {
    showToast(e.message || '加载失败')
    centerList.value = []
  } finally {
    loadingCenter.value = false
  }
}

async function handleReceive(c) {
  try {
    await receiveCoupon(c.id)
    showToast('领取成功')
    await loadCenter()
  } catch (e) {
    showToast(e.message || '领取失败')
  }
}

async function loadMyCoupons() {
  loadingMy.value = true
  try {
    const res = await getMyCoupons({ pageNum: 1, pageSize: 100, status: myStatus.value })
    myList.value = res.data?.list || []
  } catch (e) {
    showToast(e.message || '加载失败')
    myList.value = []
  } finally {
    loadingMy.value = false
  }
}

function onMainTabChange() {
  if (mainTab.value === 1 && !myList.value.length) {
    loadMyCoupons()
  }
}

onMounted(loadCenter)
</script>

<style scoped>
.coupon-page {
  background: #f5f5f5;
  min-height: 100vh;
  padding-bottom: 20px;
}

.center-list,
.my-list {
  padding: 10px 12px;
}

.coupon-card {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #fff;
  border-radius: 10px;
  padding: 14px 12px;
  margin-bottom: 10px;
  border-left: 4px solid #FF2E93;
}
.coupon-card.grey {
  border-left-color: #ccc;
  opacity: 0.7;
}
.coupon-left {
  width: 78px;
  text-align: center;
  flex-shrink: 0;
}
.coupon-value {
  font-size: 20px;
  font-weight: 700;
  color: #FF2E93;
}
.coupon-threshold {
  font-size: 10px;
  color: #999;
  margin-top: 2px;
}
.coupon-mid {
  flex: 1;
  min-width: 0;
}
.coupon-name {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}
.coupon-scope {
  font-size: 11px;
  color: #FF2E93;
  margin-top: 3px;
}
.coupon-expire,
.coupon-remain {
  font-size: 11px;
  color: #999;
  margin-top: 3px;
}
.coupon-btn {
  flex-shrink: 0;
}
.coupon-state {
  flex-shrink: 0;
  font-size: 12px;
  color: #999;
}
</style>
