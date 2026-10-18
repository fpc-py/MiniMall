<template>
  <div class="shop-coupon-page">
    <el-card shadow="never" style="margin-bottom:16px">
      <el-form :model="search" inline>
        <el-form-item label="名称">
          <el-input v-model="search.keyword" placeholder="优惠券名称" clearable style="width:200px" @keyup.enter="resetAndLoad" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="search.status" placeholder="全部" clearable style="width:120px">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="danger" @click="resetAndLoad">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>店铺优惠券</span>
          <el-button type="danger" size="small" @click="openCreate">+ 新建优惠券</el-button>
        </div>
      </template>
      <el-table :data="list" border stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="名称" min-width="160" show-overflow-tooltip />
        <el-table-column label="类型" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.type === 1 ? '满减' : '折扣' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="优惠内容" min-width="160">
          <template #default="{ row }">{{ couponRule(row) }}</template>
        </el-table-column>
        <el-table-column label="发行/已领/剩余" width="150" align="center">
          <template #default="{ row }">{{ row.totalCount }} / {{ row.receivedCount }} / {{ row.remainCount }}</template>
        </el-table-column>
        <el-table-column label="有效期" min-width="220">
          <template #default="{ row }">{{ row.startTime }} ~ {{ row.endTime }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right" align="center">
          <template #default="{ row }">
            <el-button v-if="row.status === 1" link type="danger" @click="toggleStatus(row, 0)">停用</el-button>
            <el-button v-else link type="primary" @click="toggleStatus(row, 1)">启用</el-button>
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

    <el-dialog v-model="dialogVisible" title="新建优惠券" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入优惠券名称" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio :value="1">满减</el-radio>
            <el-radio :value="2">折扣</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="使用门槛(元)">
          <el-input-number v-model="form.thresholdAmount" :min="0" :precision="2" :step="10" />
        </el-form-item>
        <el-form-item v-if="form.type === 1" label="满减金额(元)" prop="discountAmount">
          <el-input-number v-model="form.discountAmount" :min="0" :precision="2" :step="10" />
        </el-form-item>
        <el-form-item v-if="form.type === 2" label="折扣率" prop="discountRate">
          <el-input-number v-model="form.discountRate" :min="0.01" :max="0.99" :precision="2" :step="0.01" />
          <span class="form-tip">0.01~0.99，如 0.8 表示 8 折</span>
        </el-form-item>
        <el-form-item label="发行总量" prop="totalCount">
          <el-input-number v-model="form.totalCount" :min="1" />
        </el-form-item>
        <el-form-item label="每人限领">
          <el-input-number v-model="form.perUserLimit" :min="1" />
        </el-form-item>
        <el-form-item label="生效时间" prop="startTime">
          <el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择开始时间" style="width:100%" />
        </el-form-item>
        <el-form-item label="失效时间" prop="endTime">
          <el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择结束时间" style="width:100%" />
        </el-form-item>
        <el-form-item label="使用说明">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="submitting" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getShopCouponList, createShopCoupon, updateShopCouponStatus } from '@/api/coupon'

const search = reactive({ keyword: '', status: null })
const list = ref([])
const page = ref(1)
const pageSize = 10
const total = ref(0)
const loading = ref(false)

const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref()

const form = reactive({
  name: '',
  type: 1,
  thresholdAmount: 0,
  discountAmount: 0,
  discountRate: 0.8,
  totalCount: 100,
  perUserLimit: 1,
  startTime: '',
  endTime: '',
  description: '',
})

const rules = {
  name: [{ required: true, message: '请输入优惠券名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  discountAmount: [{ required: true, message: '请输入满减金额', trigger: 'blur' }],
  discountRate: [{ required: true, message: '请输入折扣率', trigger: 'blur' }],
  totalCount: [{ required: true, message: '请输入发行总量', trigger: 'blur' }],
  startTime: [{ required: true, message: '请选择生效时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择失效时间', trigger: 'change' }],
}

function couponRule(row) {
  if (row.type === 2) {
    return `满${row.thresholdAmount}元打${Number(row.discountRate) * 10}折`
  }
  return `满${row.thresholdAmount}元减${row.discountAmount}元`
}

async function loadData() {
  loading.value = true
  try {
    const res = await getShopCouponList({
      pageNum: page.value,
      pageSize,
      keyword: search.keyword || undefined,
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

function handleReset() {
  search.keyword = ''
  search.status = null
  resetAndLoad()
}

function openCreate() {
  Object.assign(form, {
    name: '',
    type: 1,
    thresholdAmount: 0,
    discountAmount: 0,
    discountRate: 0.8,
    totalCount: 100,
    perUserLimit: 1,
    startTime: '',
    endTime: '',
    description: '',
  })
  dialogVisible.value = true
}

function submit() {
  formRef.value?.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const payload = {
        name: form.name,
        type: form.type,
        thresholdAmount: form.thresholdAmount,
        totalCount: form.totalCount,
        perUserLimit: form.perUserLimit,
        startTime: form.startTime,
        endTime: form.endTime,
        description: form.description,
      }
      if (form.type === 1) payload.discountAmount = form.discountAmount
      else payload.discountRate = form.discountRate
      await createShopCoupon(payload)
      ElMessage.success('创建成功')
      dialogVisible.value = false
      loadData()
    } finally {
      submitting.value = false
    }
  })
}

async function toggleStatus(row, status) {
  await updateShopCouponStatus(row.id, status)
  ElMessage.success(status === 1 ? '已启用' : '已停用')
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
.form-tip {
  margin-left: 8px;
  color: #999;
  font-size: 12px;
}
</style>
