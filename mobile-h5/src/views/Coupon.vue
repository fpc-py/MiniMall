<template>
  <div class="page coupon-page">
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
        class="coupon-card rv"
        v-for="(c, i) in centerList"
        :key="c.id"
        :style="{ '--i': i }"
      >
        <div class="coupon-left">
          <div class="coupon-value num">{{ couponValue(c) }}</div>
          <div class="coupon-threshold num">满¥{{ money(c.thresholdAmount) }}可用</div>
        </div>
        <div class="coupon-perf" aria-hidden="true"></div>
        <div class="coupon-mid">
          <div class="coupon-name">{{ c.name }}</div>
          <div class="coupon-scope">{{ c.shopId == null ? '平台通用' : '店铺专享' }}</div>
          <div class="coupon-expire num">{{ formatDate(c.startTime) }} ~ {{ formatDate(c.endTime) }}</div>
          <div class="coupon-remain num">剩余 {{ c.remainCount }}/{{ c.totalCount }}</div>
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
        class="coupon-card rv"
        :class="{ grey: myStatus !== 0 }"
        v-for="(c, i) in myList"
        :key="c.id"
        :style="{ '--i': i }"
      >
        <div class="coupon-left">
          <div class="coupon-value num">{{ couponValue(c) }}</div>
          <div class="coupon-threshold num">满¥{{ money(c.thresholdAmount) }}可用</div>
        </div>
        <div class="coupon-perf" aria-hidden="true"></div>
        <div class="coupon-mid">
          <div class="coupon-name">{{ c.name }}</div>
          <div class="coupon-scope">{{ c.shopId == null ? '平台通用' : '店铺专享' }}</div>
          <div class="coupon-expire num">有效期至 {{ formatDate(c.expireTime) }}</div>
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
  padding-bottom: 24px;
}

.center-list,
.my-list {
  padding: 14px var(--gutter) 0;
}

/* ---------- 票券卡片：左侧霓虹票面 + 打孔分隔 ---------- */
.coupon-card {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  padding: 14px 12px 14px 0;
  overflow: hidden;
  border-radius: var(--r-md);
  background-image: linear-gradient(140deg, rgba(255, 46, 147, 0.16), rgba(255, 255, 255, 0.03) 42%, rgba(255, 255, 255, 0.02));
  border: 1px solid var(--glass-line);
  backdrop-filter: blur(var(--blur)) saturate(150%);
  box-shadow: var(--shadow-card);
  animation: riseIn 0.55s var(--ease-out) both;
  animation-delay: calc(var(--i, 0) * 62ms);
}
/* 左侧霓虹竖条 */
.coupon-card::before {
  content: "";
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: linear-gradient(180deg, var(--neon), var(--violet));
  box-shadow: 0 0 14px rgba(255, 46, 147, 0.85);
}
.coupon-card.grey {
  filter: saturate(0.25);
  opacity: 0.72;
}
.coupon-card.grey::before {
  background: var(--text-3);
  box-shadow: none;
}

.coupon-left {
  width: 84px;
  padding-left: 14px;
  text-align: center;
  flex-shrink: 0;
}
.coupon-value {
  font-family: var(--font-display);
  font-size: 23px;
  font-weight: 700;
  line-height: 1.1;
  color: var(--neon);
  text-shadow: 0 0 20px rgba(255, 46, 147, 0.6);
}
.coupon-threshold {
  font-size: 10px;
  color: var(--text-3);
  margin-top: 4px;
}

/* 打孔虚线：上下各留一个缺口 */
.coupon-perf {
  position: relative;
  width: 1px;
  align-self: stretch;
  background-image: linear-gradient(180deg, var(--glass-line-2) 0 6px, transparent 6px 8px, var(--glass-line-2) 8px 100%);
  background-size: 1px 8px;
  background-repeat: repeat-y;
  flex-shrink: 0;
  margin: 2px 0;
}
.coupon-perf::before,
.coupon-perf::after {
  content: "";
  position: absolute;
  left: 50%;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: var(--ink-900);
  border: 1px solid var(--glass-line);
  transform: translateX(-50%);
}
.coupon-perf::before {
  top: -8px;
}
.coupon-perf::after {
  bottom: -8px;
}

.coupon-mid {
  flex: 1;
  min-width: 0;
}
.coupon-name {
  font-size: 14px;
  color: var(--text-1);
  font-weight: 600;
  letter-spacing: 0.02em;
}
.coupon-scope {
  display: inline-block;
  font-size: 10px;
  color: var(--cyan-soft);
  border: 1px solid rgba(36, 229, 255, 0.34);
  border-radius: var(--r-pill);
  padding: 1px 7px;
  margin-top: 5px;
}
.coupon-expire,
.coupon-remain {
  font-size: 10px;
  color: var(--text-3);
  margin-top: 5px;
  letter-spacing: 0.02em;
}
.coupon-btn {
  flex-shrink: 0;
  margin-right: 12px;
}
.coupon-state {
  flex-shrink: 0;
  margin-right: 12px;
  font-size: 11px;
  color: var(--text-3);
  letter-spacing: 0.06em;
}
</style>
