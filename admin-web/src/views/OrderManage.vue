<template>
  <div class="order-manage-page">
    <!-- 搜索筛选 -->
    <el-card shadow="never" style="margin-bottom:16px">
      <el-form :model="search" inline>
        <el-form-item label="订单号">
          <el-input v-model="search.orderNo" placeholder="搜索订单号" clearable style="width:200px" @keyup.enter="resetAndLoad" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="search.orderStatus" placeholder="全部状态" clearable style="width:140px">
            <el-option label="待付款" :value="10" />
            <el-option label="待发货" :value="20" />
            <el-option label="待收货" :value="30" />
            <el-option label="已完成" :value="40" />
            <el-option label="已取消" :value="5" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="danger" @click="resetAndLoad">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 订单列表 -->
    <el-card shadow="never">
      <template #header>
        <span>订单管理（只读，发货/审核在商家端）</span>
      </template>
      <el-table :data="list" border stripe v-loading="loading">
        <el-table-column label="订单号" prop="orderNo" width="200" />
        <el-table-column label="收货人" prop="receiverName" width="110" />
        <el-table-column label="收货电话" prop="receiverPhone" width="140" />
        <el-table-column label="实付金额" width="120" align="right">
          <template #default="{ row }">
            <span style="color:#FF2E93;font-weight:700">¥{{ formatMoney(row.payAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="支付状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="payType(row.payStatus)" size="small">{{ payText(row.payStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="订单状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.orderStatus)" size="small">{{ statusText(row.orderStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="下单时间" prop="createTime" width="170" />
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

    <!-- 订单详情弹窗 -->
    <el-dialog v-model="detailVisible" title="订单详情" width="760px">
      <template v-if="detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单号">{{ detail.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="订单状态">
            <el-tag :type="statusType(detail.orderStatus)" size="small">{{ statusText(detail.orderStatus) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="支付状态">
            <el-tag :type="payType(detail.payStatus)" size="small">{{ payText(detail.payStatus) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="实付金额">
            <span style="color:#FF2E93;font-weight:700">¥{{ formatMoney(detail.payAmount) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="商品金额">¥{{ formatMoney(detail.totalAmount) }}</el-descriptions-item>
          <el-descriptions-item label="运费">¥{{ formatMoney(detail.freightAmount) }}</el-descriptions-item>
          <el-descriptions-item label="优惠金额">¥{{ formatMoney(detail.discountAmount) }}</el-descriptions-item>
          <el-descriptions-item label="收货人">{{ detail.receiverName }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ detail.receiverPhone }}</el-descriptions-item>
          <el-descriptions-item label="收货地址" :span="2">{{ detail.receiverAddress }}</el-descriptions-item>
          <el-descriptions-item label="快递公司">{{ detail.deliveryCompany || '-' }}</el-descriptions-item>
          <el-descriptions-item label="快递单号">{{ detail.deliveryNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="买家备注" :span="2">{{ detail.memberRemark || '-' }}</el-descriptions-item>
          <el-descriptions-item label="下单时间">{{ detail.createTime }}</el-descriptions-item>
          <el-descriptions-item label="支付时间">{{ detail.paymentTime || '-' }}</el-descriptions-item>
        </el-descriptions>
        <el-table :data="detail.items" border stripe style="margin-top:12px" size="small">
          <el-table-column label="商品" min-width="200">
            <template #default="{ row }">
              <div class="goods-cell">
                <el-image v-if="row.productImage" :src="row.productImage" fit="cover" style="width:40px;height:40px;border-radius:4px" />
                <div>
                  <div class="goods-name">{{ row.productName }}</div>
                  <div class="goods-attrs">{{ row.attrs || '-' }}</div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="单价" width="100">
            <template #default="{ row }">¥{{ formatMoney(row.price) }}</template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="70" align="center" />
          <el-table-column label="小计" width="110">
            <template #default="{ row }">¥{{ formatMoney(row.subtotal) }}</template>
          </el-table-column>
        </el-table>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getOrderList, getOrderDetail } from '@/api/order'

const search = reactive({ orderNo: '', orderStatus: null })
const list = ref([])
const page = ref(1)
const pageSize = 10
const total = ref(0)
const loading = ref(false)
const detailVisible = ref(false)
const detail = ref(null)

function statusText(status) {
  const map = { 10: '待付款', 20: '待发货', 30: '待收货', 40: '已完成', 5: '已取消' }
  return map[status] || '未知'
}

function statusType(status) {
  const map = { 10: 'danger', 20: 'warning', 30: 'primary', 40: 'success', 5: 'info' }
  return map[status] || 'info'
}

function payText(status) {
  const map = { 0: '未支付', 1: '已支付', 2: '已退款' }
  return map[status] || '未知'
}

function payType(status) {
  const map = { 0: 'info', 1: 'success', 2: 'warning' }
  return map[status] || 'info'
}

function formatMoney(v) {
  return Number(v || 0).toFixed(2)
}

async function loadData() {
  loading.value = true
  try {
    const res = await getOrderList({
      pageNum: page.value,
      pageSize,
      orderNo: search.orderNo || undefined,
      orderStatus: search.orderStatus === null || search.orderStatus === '' ? undefined : search.orderStatus,
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
  search.orderNo = ''
  search.orderStatus = null
  resetAndLoad()
}

async function showDetail(row) {
  const res = await getOrderDetail(row.id)
  detail.value = res.data
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
.goods-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}
.goods-name {
  font-size: 13px;
  color: #333;
}
.goods-attrs {
  font-size: 12px;
  color: #999;
}
</style>
