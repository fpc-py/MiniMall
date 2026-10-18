<template>
  <div class="shop-dashboard" v-loading="loading">
    <el-row :gutter="16" style="margin-bottom:16px">
      <el-col :span="6">
        <el-card shadow="never">
          <div class="stat">
            <div class="stat-num">{{ stats.productCount }}</div>
            <div class="stat-label">商品总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <div class="stat">
            <div class="stat-num" style="color:#667eea">{{ stats.orderCount }}</div>
            <div class="stat-label">订单总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <div class="stat">
            <div class="stat-num" style="color:#f57c00">{{ stats.pendingShipOrderCount }}</div>
            <div class="stat-label">待发货订单</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <div class="stat">
            <div class="stat-num" style="color:#07c160">¥{{ formatMoney(stats.salesAmount) }}</div>
            <div class="stat-label">销售额</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header><span>待办事项</span></template>
          <div class="todo-item" @click="$router.push('/shop/orders')">
            <el-tag type="warning" size="small" effect="plain">{{ stats.pendingShipOrderCount }}</el-tag>
            <span class="todo-label">待发货订单</span>
          </div>
          <div class="todo-item" @click="$router.push('/shop/refunds')">
            <el-tag type="primary" size="small" effect="plain">{{ stats.pendingRefundCount }}</el-tag>
            <span class="todo-label">待处理退款</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <template #header><span>快捷操作</span></template>
          <div class="quick-actions">
            <el-button @click="$router.push('/shop/products')">发布商品</el-button>
            <el-button @click="$router.push('/shop/orders')">处理订单</el-button>
            <el-button @click="$router.push('/shop/refunds')">审核退款</el-button>
            <el-button @click="$router.push('/shop/info')">店铺信息</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getShopStats } from '@/api/stats'

const loading = ref(false)
const stats = reactive({
  productCount: 0,
  orderCount: 0,
  pendingShipOrderCount: 0,
  pendingRefundCount: 0,
  salesAmount: 0,
})

function formatMoney(v) {
  return Number(v || 0).toFixed(2)
}

async function loadData() {
  loading.value = true
  try {
    const res = await getShopStats()
    Object.assign(stats, res.data || {})
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.stat {
  text-align: center;
  padding: 12px 0;
}
.stat-num {
  font-size: 32px;
  font-weight: 700;
  color: #FF2E93;
}
.stat-label {
  font-size: 14px;
  color: #999;
  margin-top: 4px;
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
