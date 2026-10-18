<template>
  <div class="shop-list-page">
    <el-card shadow="never" style="margin-bottom:16px">
      <el-form :model="search" inline>
        <el-form-item label="店铺状态">
          <el-select v-model="search.status" placeholder="全部状态" clearable style="width:150px">
            <el-option label="待审核" :value="0" />
            <el-option label="已上架" :value="1" />
            <el-option label="已下架" :value="2" />
            <el-option label="审核拒绝" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="danger" @click="resetAndLoad">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header><span>店铺列表</span></template>
      <el-table :data="list" border stripe v-loading="loading">
        <el-table-column label="Logo" width="80">
          <template #default="{ row }">
            <el-image
              v-if="row.logo"
              :src="row.logo"
              :preview-src-list="[row.logo]"
              fit="cover"
              style="width:44px;height:44px;border-radius:6px"
            />
            <div v-else class="logo-placeholder">
              <el-icon :size="20" color="#FF2E93"><Shop /></el-icon>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="店铺名称" prop="name" min-width="180" show-overflow-tooltip />
        <el-table-column label="类型" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.type === 0 ? '自营' : '第三方' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="联系人" prop="contactName" width="110" />
        <el-table-column label="联系电话" prop="contactPhone" width="140" />
        <el-table-column label="所在地" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ fullAddress(row) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createTime" width="170" />
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Shop } from '@element-plus/icons-vue'
import { getShopList } from '@/api/merchant'

const search = reactive({ status: null })
const list = ref([])
const page = ref(1)
const pageSize = 10
const total = ref(0)
const loading = ref(false)

function statusText(status) {
  const map = { 0: '待审核', 1: '已上架', 2: '已下架', 3: '审核拒绝' }
  return map[status] || '未知'
}

function statusType(status) {
  const map = { 0: 'warning', 1: 'success', 2: 'info', 3: 'danger' }
  return map[status] || 'info'
}

function fullAddress(row) {
  return [row.province, row.city, row.district, row.address].filter(Boolean).join('')
}

async function loadData() {
  loading.value = true
  try {
    const res = await getShopList({
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

onMounted(loadData)
</script>

<style scoped>
.logo-placeholder {
  width: 44px;
  height: 44px;
  background: #fafafa;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
