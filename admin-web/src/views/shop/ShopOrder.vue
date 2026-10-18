<template>
  <div class="shop-order-page">
    <el-card shadow="never" style="margin-bottom:16px">
      <el-form :model="search" inline>
        <el-form-item label="订单状态">
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
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header><span>店铺订单</span></template>
      <el-table :data="list" border stripe v-loading="loading">
        <el-table-column label="订单号" prop="orderNo" width="200" />
        <el-table-column label="收货人" prop="receiverName" width="110" />
        <el-table-column label="收货电话" prop="receiverPhone" width="140" />
        <el-table-column label="实付金额" width="120" align="right">
          <template #default="{ row }">
            <span style="color:#FF2E93;font-weight:700">¥{{ formatMoney(row.payAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="订单状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.orderStatus)" size="small">{{ statusText(row.orderStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="下单时间" prop="createTime" width="170" />
        <el-table-column label="操作" width="160" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="showDetail(row)">详情</el-button>
            <el-button v-if="row.orderStatus === 20" link type="warning" @click="openShip(row)">发货</el-button>
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
    <el-dialog v-model="detailVisible" title="订单详情" width="760px">
      <template v-if="detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单号">{{ detail.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="订单状态">
            <el-tag :type="statusType(detail.orderStatus)" size="small">{{ statusText(detail.orderStatus) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="实付金额">
            <span style="color:#FF2E93;font-weight:700">¥{{ formatMoney(detail.payAmount) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="运费">¥{{ formatMoney(detail.freightAmount) }}</el-descriptions-item>
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

    <!-- 发货 -->
    <el-dialog v-model="shipVisible" title="订单发货" width="480px">
      <el-form ref="shipFormRef" :model="shipForm" :rules="shipRules" label-width="100px">
        <el-form-item label="订单号">
          <span>{{ shipForm.orderNo }}</span>
        </el-form-item>
        <el-form-item label="快递公司" prop="deliveryCompany">
          <el-select v-model="shipForm.deliveryCompany" placeholder="选择或输入快递公司" filterable allow-create style="width:100%">
            <el-option label="顺丰速运" value="顺丰速运" />
            <el-option label="中通快递" value="中通快递" />
            <el-option label="圆通速递" value="圆通速递" />
            <el-option label="韵达快递" value="韵达快递" />
            <el-option label="京东物流" value="京东物流" />
          </el-select>
        </el-form-item>
        <el-form-item label="快递单号" prop="deliveryNo">
          <el-input v-model="shipForm.deliveryNo" placeholder="请输入快递单号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shipVisible = false">取消</el-button>
        <el-button type="danger" :loading="shipping" @click="submitShip">确认发货</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getShopOrders, getShopOrderDetail, shipOrder } from '@/api/shop'

const search = reactive({ orderStatus: null })
const list = ref([])
const page = ref(1)
const pageSize = 10
const total = ref(0)
const loading = ref(false)
const detailVisible = ref(false)
const detail = ref(null)

const shipVisible = ref(false)
const shipping = ref(false)
const shipFormRef = ref()
const shipForm = reactive({ orderId: null, orderNo: '', deliveryCompany: '', deliveryNo: '' })
const shipRules = {
  deliveryCompany: [{ required: true, message: '请选择快递公司', trigger: 'change' }],
  deliveryNo: [{ required: true, message: '请输入快递单号', trigger: 'blur' }],
}

function statusText(status) {
  const map = { 10: '待付款', 20: '待发货', 30: '待收货', 40: '已完成', 5: '已取消' }
  return map[status] || '未知'
}

function statusType(status) {
  const map = { 10: 'danger', 20: 'warning', 30: 'primary', 40: 'success', 5: 'info' }
  return map[status] || 'info'
}

function formatMoney(v) {
  return Number(v || 0).toFixed(2)
}

async function loadData() {
  loading.value = true
  try {
    const res = await getShopOrders({
      pageNum: page.value,
      pageSize,
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

async function showDetail(row) {
  const res = await getShopOrderDetail(row.id)
  detail.value = res.data
  detailVisible.value = true
}

function openShip(row) {
  Object.assign(shipForm, { orderId: row.id, orderNo: row.orderNo, deliveryCompany: '', deliveryNo: '' })
  shipVisible.value = true
}

function submitShip() {
  shipFormRef.value?.validate(async (valid) => {
    if (!valid) return
    shipping.value = true
    try {
      await shipOrder({
        orderId: shipForm.orderId,
        deliveryCompany: shipForm.deliveryCompany,
        deliveryNo: shipForm.deliveryNo,
      })
      ElMessage.success('发货成功')
      shipVisible.value = false
      loadData()
    } finally {
      shipping.value = false
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
