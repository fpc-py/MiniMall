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
            <div class="stat-num" style="color:#f57c00">{{ stats.pending }}</div>
            <div class="stat-label">复审中</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <div class="stat-item">
            <div class="stat-num" style="color:#999">{{ stats.total }}</div>
            <div class="stat-label">全部商家</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 筛选 -->
    <el-card shadow="never" style="margin-bottom:16px">
      <el-form :model="search" inline>
        <el-form-item label="商家名称">
          <el-input v-model="search.name" placeholder="搜索商家" clearable style="width:180px" />
        </el-form-item>
        <el-form-item label="审核状态">
          <el-select v-model="search.status" placeholder="全部" clearable style="width:130px">
            <el-option label="待审核" value="pending" />
            <el-option label="已通过" value="approved" />
            <el-option label="已驳回" value="rejected" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="danger" @click="loadData">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 商家列表 -->
    <el-card shadow="never">
      <template #header>
        <span>商家入驻审核</span>
      </template>
      <el-table :data="list" border stripe>
        <el-table-column label="商家名称" prop="name" min-width="160" />
        <el-table-column label="联系人" prop="contact" width="100" />
        <el-table-column label="联系电话" prop="phone" width="130" />
        <el-table-column label="主营类目" prop="category" width="120" />
        <el-table-column label="申请时间" prop="applyTime" width="160" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'approved' ? 'success' : row.status === 'rejected' ? 'danger' : 'warning'" size="small">
              {{ row.status === 'approved' ? '已通过' : row.status === 'rejected' ? '已驳回' : '待审核' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="showDetail(row)">查看资料</el-button>
            <el-button
              v-if="row.status === 'pending'"
              type="success"
              size="small"
              @click="handleApprove(row)"
            >通过</el-button>
            <el-button
              v-if="row.status === 'pending'"
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
          :page-size="10"
          :total="total"
          layout="total, prev, pager, next"
        />
      </div>
    </el-card>

    <!-- 商家详情弹窗 -->
    <el-dialog v-model="detailVisible" title="商家入驻资料" width="600px">
      <template v-if="detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="商家名称">{{ detail.name }}</el-descriptions-item>
          <el-descriptions-item label="联系人">{{ detail.contact }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ detail.phone }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ detail.email }}</el-descriptions-item>
          <el-descriptions-item label="主营类目">{{ detail.category }}</el-descriptions-item>
          <el-descriptions-item label="申请时间">{{ detail.applyTime }}</el-descriptions-item>
          <el-descriptions-item label="营业执照" :span="2">
            <el-tag>营业执照.jpg</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="店铺简介" :span="2">
            <div class="merchant-desc">{{ detail.description || '暂无简介' }}</div>
          </el-descriptions-item>
        </el-descriptions>
        <div style="margin-top:16px;text-align:center" v-if="detail.status === 'pending'">
          <el-button type="success" size="large" @click="handleApprove(detail)" style="width:120px">审核通过</el-button>
          <el-button type="danger" size="large" @click="handleReject(detail)" style="width:120px;margin-left:16px">驳回</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const search = reactive({ name: '', status: '' })
const list = ref([])
const page = ref(1)
const total = ref(0)
const detailVisible = ref(false)
const detail = ref(null)

const stats = reactive({ pending: 0, approved: 0, total: 0 })

function loadData() {
  list.value = [
    { id: 1, name: '小米旗舰店', contact: '王经理', phone: '138****0001', email: 'mi@example.com', category: '手机数码', applyTime: '2026-09-12 10:00:00', status: 'pending', description: '小米官方授权店铺，主营手机、平板、智能家居产品' },
    { id: 2, name: '华为智选店', contact: '李经理', phone: '138****0002', email: 'huawei@example.com', category: '手机数码', applyTime: '2026-09-11 14:30:00', status: 'approved', description: '华为品牌授权经销商' },
    { id: 3, name: '海尔官方旗舰店', contact: '张经理', phone: '138****0003', email: 'haier@example.com', category: '家用电器', applyTime: '2026-09-10 09:00:00', status: 'approved', description: '海尔集团官方直营' },
    { id: 4, name: '李宁官方店', contact: '赵经理', phone: '138****0004', email: 'lining@example.com', category: '服饰鞋包', applyTime: '2026-09-09 16:00:00', status: 'pending', description: '李宁品牌授权，运动服饰鞋包' },
    { id: 5, name: '完美日记', contact: '陈经理', phone: '138****0005', email: 'perfect@example.com', category: '美妆个护', applyTime: '2026-09-08 11:20:00', status: 'rejected', description: '美妆品牌' },
    { id: 6, name: '良品铺子', contact: '刘经理', phone: '138****0006', email: 'lppz@example.com', category: '食品生鲜', applyTime: '2026-09-07 13:40:00', status: 'pending', description: '休闲零食品牌' },
  ]
  total.value = list.value.length
  stats.pending = list.value.filter(i => i.status === 'pending').length
  stats.approved = list.value.filter(i => i.status === 'approved').length
  stats.total = list.value.length
}

function showDetail(row) {
  detail.value = row
  detailVisible.value = true
}

function handleApprove(row) {
  ElMessageBox.confirm(`确认通过「${row.name}」的入驻申请？`, '提示', {
    confirmButtonText: '确认通过',
    cancelButtonText: '取消',
    confirmButtonColor: '#FF2E93',
    type: 'info',
  }).then(() => {
    row.status = 'approved'
    ElMessage.success('审核通过，商家已入驻')
    detailVisible.value = false
    loadData()
  }).catch(() => {})
}

function handleReject(row) {
  ElMessageBox.prompt('请输入驳回原因', '驳回申请', {
    confirmButtonText: '确认驳回',
    cancelButtonText: '取消',
    confirmButtonColor: '#FF2E93',
    inputPlaceholder: '请填写驳回原因',
  }).then(({ value }) => {
    row.status = 'rejected'
    ElMessage.success(`已驳回：${value || '资料不完善'}`)
    detailVisible.value = false
    loadData()
  }).catch(() => {})
}

onMounted(loadData)
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
.merchant-desc {
  font-size: 13px;
  color: #666;
  line-height: 1.6;
}
</style>
