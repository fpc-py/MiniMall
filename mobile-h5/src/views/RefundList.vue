<template>
  <div class="refund-list-page">
    <van-nav-bar title="退款 / 售后" left-arrow fixed placeholder @click-left="$router.back()" />

    <van-tabs v-model:active="activeTab" sticky offset-top="46" color="#FF2E93" line-width="20" @change="resetAndReload">
      <van-tab title="全部" />
      <van-tab title="待审核" />
      <van-tab title="已退款" />
      <van-tab title="已拒绝" />
      <van-tab title="已撤销" />
    </van-tabs>

    <van-list
      :key="listKey"
      v-model:loading="loading"
      :finished="finished"
      :error="error"
      finished-text="没有更多了"
      error-text="加载失败，点击重试"
      @load="onLoad"
    >
      <div class="refund-list" v-if="list.length">
        <div class="refund-card" v-for="r in list" :key="r.id">
          <div class="refund-header">
            <span class="refund-no">退款单号：{{ r.refundNo }}</span>
            <span class="refund-status" :style="{ color: statusColor(r.status) }">
              {{ statusText(r.status) }}
            </span>
          </div>
          <div class="refund-body">
            <div class="refund-row">
              <span class="row-label">订单编号</span>
              <span class="row-value">{{ r.orderNo }}</span>
            </div>
            <div class="refund-row">
              <span class="row-label">退款金额</span>
              <span class="row-value amount">¥{{ money(r.refundAmount) }}</span>
            </div>
            <div class="refund-row">
              <span class="row-label">退款原因</span>
              <span class="row-value">{{ r.reason }}</span>
            </div>
            <div class="refund-row" v-if="r.description">
              <span class="row-label">补充说明</span>
              <span class="row-value">{{ r.description }}</span>
            </div>
            <div class="refund-row" v-if="r.auditRemark">
              <span class="row-label">审核备注</span>
              <span class="row-value">{{ r.auditRemark }}</span>
            </div>
            <div class="refund-row">
              <span class="row-label">申请时间</span>
              <span class="row-value">{{ r.createTime }}</span>
            </div>
          </div>
          <div class="refund-actions" v-if="r.status === 0">
            <van-button
              round
              plain
              size="small"
              color="#999"
              @click="handleCancel(r)"
            >撤销申请</van-button>
          </div>
        </div>
      </div>

      <van-empty
        v-if="!loading && finished && !list.length"
        description="暂无退款记录"
      />
    </van-list>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { showToast, showConfirmDialog } from 'vant'
import { getMyRefunds, cancelRefund } from '@/api/refund'

const PAGE_SIZE = 10
const tabStatusMap = [null, 0, 2, 3, 4]

const activeTab = ref(0)
const listKey = ref(0)

const list = ref([])
const pageNum = ref(1)
const loading = ref(false)
const finished = ref(false)
const error = ref(false)

const statusParam = computed(() => tabStatusMap[activeTab.value])

function money(v) {
  return Number(v || 0).toFixed(2)
}

function statusText(status) {
  const map = { 0: '待审核', 1: '退款中', 2: '已退款', 3: '已拒绝', 4: '已撤销' }
  return map[status] || '未知'
}

function statusColor(status) {
  const map = { 0: '#f57c00', 1: '#1677ff', 2: '#07c160', 3: '#ee0a24', 4: '#999' }
  return map[status] || '#999'
}

async function onLoad() {
  try {
    const params = { pageNum: pageNum.value, pageSize: PAGE_SIZE }
    if (statusParam.value != null) params.status = statusParam.value
    const res = await getMyRefunds(params)
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

function resetAndReload() {
  pageNum.value = 1
  list.value = []
  finished.value = false
  error.value = false
  loading.value = false
  listKey.value += 1
}

function handleCancel(r) {
  showConfirmDialog({
    title: '撤销退款',
    message: '确定要撤销该退款申请吗？',
    confirmButtonColor: '#FF2E93',
  })
    .then(async () => {
      try {
        await cancelRefund(r.id)
        showToast('已撤销')
        resetAndReload()
      } catch (e) {
        showToast(e.message || '撤销失败')
      }
    })
    .catch(() => {})
}

onMounted(resetAndReload)
</script>

<style scoped>
.refund-list-page {
  background: #f5f5f5;
  min-height: 100vh;
  padding-bottom: 20px;
}

.refund-list {
  padding: 10px 12px;
}
.refund-card {
  background: #fff;
  border-radius: 10px;
  margin-bottom: 10px;
  overflow: hidden;
}
.refund-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  border-bottom: 1px solid #f5f5f5;
}
.refund-no {
  font-size: 12px;
  color: #999;
}
.refund-status {
  font-size: 12px;
  font-weight: 600;
}
.refund-body {
  padding: 8px 14px;
}
.refund-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 5px 0;
  font-size: 13px;
}
.row-label {
  color: #999;
  flex-shrink: 0;
}
.row-value {
  color: #333;
  text-align: right;
}
.row-value.amount {
  color: #FF2E93;
  font-weight: 700;
}
.refund-actions {
  display: flex;
  justify-content: flex-end;
  padding: 8px 14px 12px;
}
</style>
