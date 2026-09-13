<template>
  <div class="dashboard-page">
    <!-- 统计卡片 -->
    <el-row :gutter="16" style="margin-bottom:16px">
      <el-col :span="6">
        <el-card shadow="never">
          <div class="dash-stat">
            <div class="dash-num">1,286</div>
            <div class="dash-label">今日订单</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <div class="dash-stat">
            <div class="dash-num" style="color:#07c160">¥28.6 万</div>
            <div class="dash-label">今日营收</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <div class="dash-stat">
            <div class="dash-num" style="color:#f57c00">3,582</div>
            <div class="dash-label">商品总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <div class="dash-stat">
            <div class="dash-num" style="color:#667eea">86</div>
            <div class="dash-label">入驻商家</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 待办事项 -->
    <el-row :gutter="16">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header><span>待办事项</span></template>
          <div class="todo-item" v-for="item in todos" :key="item.label">
            <el-tag :type="item.type" size="small" effect="plain">{{ item.count }}</el-tag>
            <span class="todo-label">{{ item.label }}</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <template #header><span>快捷操作</span></template>
          <div class="quick-actions">
            <el-button v-for="act in actions" :key="act.label" @click="$router.push(act.path)" style="margin-bottom:8px">
              {{ act.label }}
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 最近订单 -->
    <el-card shadow="never" style="margin-top:16px">
      <template #header><span>最近订单</span></template>
      <el-table :data="recentOrders" border stripe>
        <el-table-column prop="orderNo" label="订单号" width="160" />
        <el-table-column prop="buyer" label="买家" width="100" />
        <el-table-column prop="amount" label="金额" width="100">
          <template #default="{ row }">¥{{ row.amount }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'pending_shipment' ? 'warning' : 'success'" size="small">
              {{ row.status === 'pending_shipment' ? '待发货' : '已完成' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="time" label="时间" width="160" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
const todos = [
  { label: '待审核商家入驻', count: 3, type: 'danger' },
  { label: '待发货订单', count: 12, type: 'warning' },
  { label: '售后申请', count: 2, type: 'primary' },
  { label: '库存预警', count: 5, type: 'info' },
]

const actions = [
  { label: '新增商品', path: '/product-spu' },
  { label: '订单发货', path: '/orders' },
  { label: '商家审核', path: '/merchant-audit' },
  { label: '类目管理', path: '/category' },
]

const recentOrders = [
  { orderNo: '202609130001', buyer: '张三', amount: '7298.00', status: 'pending_shipment', time: '2026-09-13 10:30:00' },
  { orderNo: '202609130002', buyer: '李四', amount: '599.00', status: 'pending_shipment', time: '2026-09-13 09:15:00' },
  { orderNo: '202609120003', buyer: '王五', amount: '239.00', status: 'completed', time: '2026-09-12 16:45:00' },
  { orderNo: '202609120004', buyer: '赵六', amount: '3299.00', status: 'completed', time: '2026-09-12 14:20:00' },
]
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
.todo-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px solid #f5f5f5;
}
.todo-item:last-child { border-bottom: none; }
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
