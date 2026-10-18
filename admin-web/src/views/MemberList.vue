<template>
  <div class="member-list-page">
    <el-card shadow="never" style="margin-bottom:16px">
      <el-form :model="search" inline>
        <el-form-item label="关键字">
          <el-input
            v-model="search.keyword"
            placeholder="手机号 / 昵称"
            clearable
            style="width:220px"
            @keyup.enter="resetAndLoad"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="danger" @click="resetAndLoad">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header><span>会员列表</span></template>
      <el-table :data="list" border stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="90" />
        <el-table-column prop="username" label="用户名" min-width="120" show-overflow-tooltip />
        <el-table-column prop="nickname" label="昵称" min-width="120" show-overflow-tooltip />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column label="性别" width="70" align="center">
          <template #default="{ row }">{{ genderText(row.gender) }}</template>
        </el-table-column>
        <el-table-column label="等级" prop="level" width="70" align="center" />
        <el-table-column label="积分" prop="points" width="80" align="center" />
        <el-table-column label="余额" width="110" align="right">
          <template #default="{ row }">¥{{ formatMoney(row.balance) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" width="170" />
        <el-table-column label="操作" width="120" fixed="right" align="center">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 1"
              link
              type="danger"
              @click="toggleStatus(row, 0)"
            >禁用</el-button>
            <el-button
              v-else
              link
              type="primary"
              @click="toggleStatus(row, 1)"
            >启用</el-button>
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getMemberList, updateMemberStatus } from '@/api/member'

const search = reactive({ keyword: '' })
const list = ref([])
const page = ref(1)
const pageSize = 10
const total = ref(0)
const loading = ref(false)

function genderText(g) {
  return g === 1 ? '男' : g === 2 ? '女' : '未知'
}

function formatMoney(v) {
  return Number(v || 0).toFixed(2)
}

async function loadData() {
  loading.value = true
  try {
    const res = await getMemberList({
      pageNum: page.value,
      pageSize,
      keyword: search.keyword || undefined,
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
  resetAndLoad()
}

async function toggleStatus(row, status) {
  await updateMemberStatus(row.id, status)
  ElMessage.success(status === 1 ? '已启用' : '已禁用')
  loadData()
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
