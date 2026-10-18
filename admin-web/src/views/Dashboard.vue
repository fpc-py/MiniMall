<template>
  <div class="dashboard-page" v-loading="loading">
    <!-- 统计卡片 -->
    <el-row :gutter="16" style="margin-bottom:16px">
      <el-col :span="6">
        <el-card shadow="never">
          <div class="dash-stat">
            <div class="dash-num">{{ stats.memberCount }}</div>
            <div class="dash-label">会员总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <div class="dash-stat">
            <div class="dash-num" style="color:#667eea">{{ stats.shopCount }}</div>
            <div class="dash-label">店铺总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <div class="dash-stat">
            <div class="dash-num" style="color:#f57c00">{{ stats.orderCount }}</div>
            <div class="dash-label">订单总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <div class="dash-stat">
            <div class="dash-num" style="color:#07c160">¥ {{ formatMoney(stats.totalSalesAmount) }}</div>
            <div class="dash-label">累计销售额</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <!-- 今日 / 待办 -->
      <el-col :span="12">
        <el-card shadow="never">
          <template #header><span>今日概况</span></template>
          <div class="dash-line">
            <span class="dash-line-label">今日订单数</span>
            <span class="dash-line-value">{{ stats.todayOrderCount }}</span>
          </div>
        </el-card>
        <el-card shadow="never" style="margin-top:16px">
          <template #header><span>待办事项</span></template>
          <div class="todo-item" @click="$router.push('/merchant-audit')">
            <el-tag type="danger" size="small" effect="plain">{{ stats.pendingShopApplyCount }}</el-tag>
            <span class="todo-label">待审核商家入驻</span>
          </div>
          <div class="todo-item" @click="$router.push('/orders')">
            <el-tag type="warning" size="small" effect="plain">{{ stats.pendingShipOrderCount }}</el-tag>
            <span class="todo-label">待发货订单</span>
          </div>
          <div class="todo-item" @click="$router.push('/refunds')">
            <el-tag type="primary" size="small" effect="plain">{{ stats.pendingRefundCount }}</el-tag>
            <span class="todo-label">待处理退款</span>
          </div>
        </el-card>
      </el-col>

      <!-- 快捷操作 -->
      <el-col :span="12">
        <el-card shadow="never">
          <template #header><span>快捷操作</span></template>
          <div class="quick-actions">
            <el-button v-for="act in actions" :key="act.label" @click="$router.push(act.path)">
              {{ act.label }}
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getAdminStats } from '@/api/stats'

const loading = ref(false)
const stats = reactive({
  memberCount: 0,
  shopCount: 0,
  orderCount: 0,
  todayOrderCount: 0,
  totalSalesAmount: 0,
  pendingShopApplyCount: 0,
  pendingShipOrderCount: 0,
  pendingRefundCount: 0,
})

const actions = [
  { label: '类目管理', path: '/category' },
  { label: 'SPU 管理', path: '/product-spu' },
  { label: '订单管理', path: '/orders' },
  { label: '商家审核', path: '/merchant-audit' },
]

function formatMoney(v) {
  if (v === null || v === undefined) return '0.00'
  return Number(v).toFixed(2)
}

async function loadData() {
  loading.value = true
  try {
    const res = await getAdminStats()
    Object.assign(stats, res.data || {})
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.dash-stat {
  text-align: center;
  padding: 12px 0;
}
.dash-num {
  font-size: 32px;
  font-weight: 700;
  color: #FF2E93;
}
.dash-label {
  font-size: 14px;
  color: #999;
  margin-top: 4px;
}
.dash-line {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 0;
}
.dash-line-label {
  color: #666;
  font-size: 14px;
}
.dash-line-value {
  font-size: 22px;
  font-weight: 700;
  color: #f57c00;
}
.todo-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 8px;
  border-bottom: 1px solid #f5f5f5;
  cursor: pointer;
}
.todo-item:last-child { border-bottom: none; }
.todo-item:hover { background: #fafafa; }
.todo-label {
  font-size: 14px;
  color: #333;
}
.quick-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
</style>
