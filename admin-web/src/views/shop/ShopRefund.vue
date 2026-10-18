<template>
  <div class="shop-refund-page">
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
      <template #header><span>退款审核</span></template>
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
        <el-table-column label="操作" width="120" fixed="right" align="center">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" link type="warning" @click="openAudit(row)">审核</el-button>
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
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

    <!-- 详情 -->
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
          <el-descriptions-item label="到账时间">{{ detail.refundTime || '-' }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-dialog>

    <!-- 审核 -->
    <el-dialog v-model="auditVisible" title="退款审核" width="500px">
      <el-form ref="auditFormRef" :model="auditForm" :rules="auditRules" label-width="90px">
        <el-form-item label="退款单号">
          <span>{{ auditForm.refundNo }}</span>
        </el-form-item>
        <el-form-item label="退款金额">
          <span style="color:#FF2E93;font-weight:700">¥{{ formatMoney(auditForm.refundAmount) }}</span>
        </el-form-item>
        <el-form-item label="审核结果" prop="status">
          <el-radio-group v-model="auditForm.status">
            <el-radio :value="1">同意退款</el-radio>
            <el-radio :value="3">拒绝退款</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核备注">
          <el-input v-model="auditForm.auditRemark" type="textarea" :rows="3" placeholder="请填写审核说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditVisible = false">取消</el-button>
        <el-button type="danger" :loading="auditing" @click="submitAudit">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getShopRefundList, auditRefund } from '@/api/refund'

const search = reactive({ status: null })
const list = ref([])
const page = ref(1)
const pageSize = 10
const total = ref(0)
const loading = ref(false)
const detailVisible = ref(false)
const detail = ref(null)

const auditVisible = ref(false)
const auditing = ref(false)
const auditFormRef = ref()
const auditForm = reactive({ refundId: null, refundNo: '', refundAmount: 0, status: 1, auditRemark: '' })
const auditRules = {
  status: [{ required: true, message: '请选择审核结果', trigger: 'change' }],
}

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
    const res = await getShopRefundList({
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

function openDetail(row) {
  detail.value = row
  detailVisible.value = true
}

function openAudit(row) {
  Object.assign(auditForm, {
    refundId: row.id,
    refundNo: row.refundNo,
    refundAmount: row.refundAmount,
    status: 1,
    auditRemark: '',
  })
  auditVisible.value = true
}

function submitAudit() {
  auditFormRef.value?.validate(async (valid) => {
    if (!valid) return
    auditing.value = true
    try {
      await auditRefund({
        refundId: auditForm.refundId,
        status: auditForm.status,
        auditRemark: auditForm.auditRemark,
      })
      ElMessage.success('审核已提交')
      auditVisible.value = false
      loadData()
    } finally {
      auditing.value = false
    }
  })
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
