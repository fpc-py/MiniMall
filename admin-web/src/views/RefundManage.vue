<template>
  <div class="refund-manage-page">
    <el-card shadow="never" style="margin-bottom:16px">
      <el-form :model="search" inline>
        <el-form-item label="退款状态">
          <el-select v-model="search.status" placeholder="全部状态" clearable style="width:150px">
            <el-option label="待审核" :value="0" />
            <el-option label="退款中" :value="1" />
            <el-option label="已退款" :value="2" />
            <el-option label="已拒绝" :value="3" />
            <el-option label="已撤销" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="danger" @click="resetAndLoad">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header><span>退款单列表（全平台，只读）</span></template>
      <el-table :data="list" border stripe v-loading="loading">
        <el-table-column prop="refundNo" label="退款单号" width="200" />
        <el-table-column prop="orderNo" label="订单号" width="180" />
        <el-table-column label="退款金额" width="120" align="right">
          <template #default="{ row }">
            <span style="color:#FF2E93;font-weight:700">¥{{ formatMoney(row.refundAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="退款原因" min-width="150" show-overflow-tooltip />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="申请时间" width="170" />
        <el-table-column label="操作" width="100" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="showDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="page"
          :page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" title="退款详情" width="640px">
      <template v-if="detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="退款单号" :span="2">{{ detail.refundNo }}</el-descriptions-item>
          <el-descriptions-item label="订单号">{{ detail.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="退款金额">
            <span style="color:#FF2E93;font-weight:700">¥{{ formatMoney(detail.refundAmount) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusType(detail.status)" size="small">{{ statusText(detail.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="申请时间">{{ detail.createTime }}</el-descriptions-item>
          <el-descriptions-item label="退款原因" :span="2">{{ detail.reason || '-' }}</el-descriptions-item>
          <el-descriptions-item label="补充说明" :span="2">{{ detail.description || '-' }}</el-descriptions-item>
          <el-descriptions-item label="审核备注" :span="2">{{ detail.auditRemark || '-' }}</el-descriptions-item>
          <el-descriptions-item label="审核时间">{{ detail.auditTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="退款到账时间">{{ detail.refundTime || '-' }}</el-descriptions-item>
        </el-descriptions>
        <div v-if="detail.images && detail.images.length" style="margin-top:12px">
          <div style="margin-bottom:6px;color:#666">凭证图片</div>
          <el-image
            v-for="(img, i) in detail.images"
            :key="i"
            :src="img"
            :preview-src-list="detail.images"
            fit="cover"
            style="width:80px;height:80px;border-radius:6px;margin-right:8px"
          />
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getAdminRefundList } from '@/api/refund'

const search = reactive({ status: null })
const list = ref([])
const page = ref(1)
const pageSize = 10
const total = ref(0)
const loading = ref(false)
const detailVisible = ref(false)
const detail = ref(null)

function statusText(status) {
  const map = { 0: '待审核', 1: '退款中', 2: '已退款', 3: '已拒绝', 4: '已撤销' }
  return map[status] || '未知'
}

function statusType(status) {
  const map = { 0: 'warning', 1: 'primary', 2: 'success', 3: 'danger', 4: 'info' }
  return map[status] || 'info'
}

function formatMoney(v) {
  return Number(v || 0).toFixed(2)
}

async function loadData() {
  loading.value = true
  try {
    const res = await getAdminRefundList({
      pageNum: page.value,
      pageSize,
      status: search.status === null || search.status === '' ? undefined : search.status,
    })
    list.value = res.data?.list || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function resetAndLoad() {
  page.value = 1
  loadData()
}

function showDetail(row) {
  detail.value = row
  detailVisible.value = true
}

onMounted(loadData)
</script>

<style scoped>
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
