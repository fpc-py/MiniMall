<template>
  <div class="merchant-audit-page">
    <!-- 统计卡片 -->
    <el-row :gutter="16" style="margin-bottom:16px">
      <el-col :span="6">
        <el-card shadow="never">
          <div class="stat-item">
            <div class="stat-num">{{ stats.pending }}</div>
            <div class="stat-label">待审核</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <div class="stat-item">
            <div class="stat-num" style="color:#07c160">{{ stats.approved }}</div>
            <div class="stat-label">已通过</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <div class="stat-item">
            <div class="stat-num" style="color:#999">{{ stats.total }}</div>
            <div class="stat-label">全部申请</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 筛选 -->
    <el-card shadow="never" style="margin-bottom:16px">
      <el-form :model="search" inline>
        <el-form-item label="审核状态">
          <el-select v-model="search.status" placeholder="全部" clearable style="width:130px">
            <el-option label="待审核" :value="0" />
            <el-option label="已通过" :value="1" />
            <el-option label="已驳回" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="danger" @click="resetAndLoad">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 入驻申请列表 -->
    <el-card shadow="never">
      <template #header>
        <span>商家入驻审核</span>
      </template>
      <el-table :data="list" border stripe v-loading="loading">
        <el-table-column label="商家名称" prop="shopName" min-width="160" />
        <el-table-column label="联系人" prop="contactName" width="100" />
        <el-table-column label="联系电话" prop="contactPhone" width="130" />
        <el-table-column label="申请时间" prop="createTime" width="170" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="showDetail(row)">查看资料</el-button>
            <el-button
              v-if="row.status === 0"
              type="success"
              size="small"
              @click="openApprove(row)"
            >通过</el-button>
            <el-button
              v-if="row.status === 0"
              type="danger"
              size="small"
              @click="handleReject(row)"
            >驳回</el-button>
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

    <!-- 申请资料弹窗 -->
    <el-dialog v-model="detailVisible" title="商家入驻资料" width="640px">
      <template v-if="detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="商家名称">{{ detail.shopName }}</el-descriptions-item>
          <el-descriptions-item label="联系人">{{ detail.contactName }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ detail.contactPhone }}</el-descriptions-item>
          <el-descriptions-item label="申请时间">{{ detail.createTime }}</el-descriptions-item>
          <el-descriptions-item label="所在地" :span="2">{{ fullAddress(detail) }}</el-descriptions-item>
          <el-descriptions-item label="营业执照" :span="2">
            <el-image
              v-if="detail.businessLicense"
              :src="detail.businessLicense"
              :preview-src-list="[detail.businessLicense]"
              fit="contain"
              style="width:120px;height:90px"
            />
            <span v-else class="empty-text">未上传</span>
          </el-descriptions-item>
          <el-descriptions-item label="身份证正面">
            <el-image
              v-if="detail.idCardFront"
              :src="detail.idCardFront"
              :preview-src-list="[detail.idCardFront]"
              fit="contain"
              style="width:120px;height:90px"
            />
            <span v-else class="empty-text">未上传</span>
          </el-descriptions-item>
          <el-descriptions-item label="身份证反面">
            <el-image
              v-if="detail.idCardBack"
              :src="detail.idCardBack"
              :preview-src-list="[detail.idCardBack]"
              fit="contain"
              style="width:120px;height:90px"
            />
            <span v-else class="empty-text">未上传</span>
          </el-descriptions-item>
          <el-descriptions-item label="审核备注" :span="2">
            {{ detail.auditRemark || '无' }}
          </el-descriptions-item>
        </el-descriptions>
        <div style="margin-top:16px;text-align:center" v-if="detail.status === 0">
          <el-button type="success" size="large" @click="openApprove(detail)" style="width:120px">审核通过</el-button>
          <el-button type="danger" size="large" @click="handleReject(detail)" style="width:120px;margin-left:16px">驳回</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 通过审核：设置商家登录账号 -->
    <el-dialog v-model="approveVisible" title="通过入驻审核" width="480px">
      <el-form :model="approveForm" label-width="110px">
        <el-form-item label="商家账号" required>
          <el-input v-model="approveForm.username" placeholder="用于商家后台登录，需全局唯一" />
        </el-form-item>
        <el-form-item label="初始密码" required>
          <el-input v-model="approveForm.password" type="password" show-password placeholder="至少 6 位" />
        </el-form-item>
        <el-form-item label="审核备注">
          <el-input v-model="approveForm.auditRemark" type="textarea" :rows="2" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approveVisible = false">取消</el-button>
        <el-button type="success" :loading="submitting" @click="submitApprove">确认通过</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getApplyList, auditShop } from '@/api/merchant'

const search = reactive({ status: null })
const list = ref([])
const page = ref(1)
const pageSize = 10
const total = ref(0)
const loading = ref(false)
const submitting = ref(false)

const detailVisible = ref(false)
const detail = ref(null)

const approveVisible = ref(false)
const approveForm = reactive({ applyId: null, username: '', password: '', auditRemark: '' })

const stats = reactive({ pending: 0, approved: 0, total: 0 })

function statusText(status) {
  return status === 1 ? '已通过' : status === 2 ? '已驳回' : '待审核'
}

function statusTagType(status) {
  return status === 1 ? 'success' : status === 2 ? 'danger' : 'warning'
}

function fullAddress(row) {
  return [row.province, row.city, row.district, row.address].filter(Boolean).join('')
}

async function loadData() {
  loading.value = true
  try {
    const res = await getApplyList({
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

async function loadStats() {
  const [pending, approved, all] = await Promise.all([
    getApplyList({ pageNum: 1, pageSize: 1, status: 0 }),
    getApplyList({ pageNum: 1, pageSize: 1, status: 1 }),
    getApplyList({ pageNum: 1, pageSize: 1 }),
  ])
  stats.pending = pending.data?.total || 0
  stats.approved = approved.data?.total || 0
  stats.total = all.data?.total || 0
}

function resetAndLoad() {
  page.value = 1
  loadData()
}

function showDetail(row) {
  detail.value = row
  detailVisible.value = true
}

function openApprove(row) {
  approveForm.applyId = row.id
  approveForm.username = ''
  approveForm.password = ''
  approveForm.auditRemark = ''
  approveVisible.value = true
}

async function submitApprove() {
  if (!approveForm.username.trim()) {
    ElMessage.warning('请填写商家账号')
    return
  }
  if (!approveForm.password || approveForm.password.length < 6) {
    ElMessage.warning('初始密码至少 6 位')
    return
  }
  submitting.value = true
  try {
    await auditShop({
      applyId: approveForm.applyId,
      status: 1,
      auditRemark: approveForm.auditRemark,
      username: approveForm.username.trim(),
      password: approveForm.password,
    })
    ElMessage.success('审核通过，店铺已开通并创建商家账号')
    approveVisible.value = false
    detailVisible.value = false
    refresh()
  } finally {
    submitting.value = false
  }
}

async function handleReject(row) {
  const { value } = await ElMessageBox.prompt('请输入驳回原因', '驳回申请', {
    confirmButtonText: '确认驳回',
    cancelButtonText: '取消',
    confirmButtonColor: '#FF2E93',
    inputPlaceholder: '请填写驳回原因',
    inputValidator: (v) => (v && v.trim() ? true : '驳回原因不能为空'),
  }).catch(() => ({ value: null }))
  if (value === null) return
  await auditShipReject(row.id, value)
}

async function auditShipReject(applyId, remark) {
  await auditShop({ applyId, status: 2, auditRemark: remark })
  ElMessage.success('已驳回')
  detailVisible.value = false
  refresh()
}

function refresh() {
  loadData()
  loadStats()
}

onMounted(refresh)
</script>

<style scoped>
.stat-item {
  text-align: center;
  padding: 16px 0;
}
.stat-num {
  font-size: 36px;
  font-weight: 700;
  color: #FF2E93;
}
.stat-label {
  font-size: 14px;
  color: #999;
  margin-top: 4px;
}
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
.empty-text {
  color: #bbb;
  font-size: 13px;
}
</style>
