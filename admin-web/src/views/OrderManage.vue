<template>
  <div class="order-manage-page">
    <!-- 搜索筛选 -->
    <el-card shadow="never" style="margin-bottom:16px">
      <el-form :model="search" inline>
        <el-form-item label="订单号">
          <el-input v-model="search.orderNo" placeholder="搜索订单号" clearable style="width:180px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="search.status" placeholder="全部状态" clearable style="width:130px">
            <el-option label="待付款" value="pending_payment" />
            <el-option label="待发货" value="pending_shipment" />
            <el-option label="待收货" value="pending_receipt" />
            <el-option label="已完成" value="completed" />
            <el-option label="已取消" value="cancelled" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="danger" @click="loadData">查询</el-button>
          <el-button @click="search = { orderNo: '', status: '' }; loadData()">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 订单列表 -->
    <el-card shadow="never">
      <template #header>
        <span>订单管理</span>
      </template>
      <el-table :data="list" border stripe>
        <el-table-column label="订单号" prop="orderNo" width="160" />
        <el-table-column label="买家" prop="buyer" width="100" />
        <el-table-column label="商品" min-width="220">
          <template #default="{ row }">
            <div class="order-goods-cell">
              <span class="goods-name" v-for="g in row.goods" :key="g.name">{{ g.name }} x{{ g.qty }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="实付金额" width="120" align="center">
          <template #default="{ row }">
            <span style="color:#FF2E93;font-weight:700">¥{{ row.amount }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="下单时间" prop="time" width="160" />
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="showDetail(row)">详情</el-button>
            <el-button
              v-if="row.status === 'pending_shipment'"
              type="danger"
              size="small"
              @click="handleShip(row)"
            >发货</el-button>
            <el-button
              v-if="row.status === 'pending_payment'"
              link
              type="danger"
              @click="cancelOrder(row)"
            >取消</el-button>
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

    <!-- 订单详情弹窗 -->
    <el-dialog v-model="detailVisible" title="订单详情" width="700px">
      <template v-if="detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单号">{{ detail.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusType(detail.status)" size="small">{{ statusText(detail.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="买家">{{ detail.buyer }}</el-descriptions-item>
          <el-descriptions-item label="实付金额">
            <span style="color:#FF2E93;font-weight:700">¥{{ detail.amount }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="收货人">{{ detail.receiver }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ detail.phone }}</el-descriptions-item>
          <el-descriptions-item label="收货地址" :span="2">{{ detail.address }}</el-descriptions-item>
          <el-descriptions-item label="下单时间" :span="2">{{ detail.time }}</el-descriptions-item>
        </el-descriptions>
        <el-table :data="detail.goods" border stripe style="margin-top:12px">
          <el-table-column label="商品名称" prop="name" min-width="180" />
          <el-table-column label="规格" prop="spec" width="140" />
          <el-table-column label="单价" width="100">
            <template #default="{ row }">¥{{ row.price }}</template>
          </el-table-column>
          <el-table-column label="数量" prop="qty" width="80" align="center" />
          <el-table-column label="小计" width="100">
            <template #default="{ row }">¥{{ (row.price * row.qty).toFixed(2) }}</template>
          </el-table-column>
        </el-table>
      </template>
    </el-dialog>

    <!-- 发货弹窗 -->
    <el-dialog v-model="shipVisible" title="发货" width="500px">
      <el-form :model="shipForm" label-width="80px">
        <el-form-item label="快递公司">
          <el-select v-model="shipForm.company" placeholder="选择快递公司" style="width:100%">
            <el-option label="顺丰速运" value="SF" />
            <el-option label="中通快递" value="ZTO" />
            <el-option label="圆通速递" value="YTO" />
            <el-option label="韵达快递" value="YUNDA" />
            <el-option label="京东物流" value="JD" />
          </el-select>
        </el-form-item>
        <el-form-item label="快递单号">
          <el-input v-model="shipForm.trackingNo" placeholder="请输入快递单号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shipVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmShip">确认发货</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

const search = reactive({ orderNo: '', status: '' })
const list = ref([])
const page = ref(1)
const total = ref(0)
const detailVisible = ref(false)
const detail = ref(null)
const shipVisible = ref(false)
const shipOrderId = ref(null)
const shipForm = reactive({ company: '', trackingNo: '' })

function loadData() {
  list.value = [
    { id: 1, orderNo: '202609130001', buyer: '张三', amount: '7298.00', status: 'pending_shipment', time: '2026-09-13 10:30:00', receiver: '张三', phone: '138****8888', address: '广东省深圳市南山区科技园南区XX大厦A座1201', goods: [{ name: '旗舰智能手机 5G全网通', spec: '曜石黑/12GB+256GB', price: 3999, qty: 1 }, { name: '智能4K电视 65英寸', spec: '65英寸/4K超清', price: 3299, qty: 1 }] },
    { id: 2, orderNo: '202609120002', buyer: '李四', amount: '599.00', status: 'pending_payment', time: '2026-09-12 14:20:00', receiver: '李四', phone: '139****9999', address: '北京市朝阳区建国路88号', goods: [{ name: '蓝牙降噪耳机 无线', spec: '黑色/标准版', price: 599, qty: 1 }] },
    { id: 3, orderNo: '202609110003', buyer: '王五', amount: '239.00', status: 'pending_receipt', time: '2026-09-11 09:15:00', receiver: '王五', phone: '137****7777', address: '上海市浦东新区陆家嘴金融中心', goods: [{ name: '精华液护肤品', spec: '30ml/经典版', price: 239, qty: 1 }] },
    { id: 4, orderNo: '202609100004', buyer: '赵六', amount: '299.00', status: 'completed', time: '2026-09-10 16:45:00', receiver: '赵六', phone: '136****6666', address: '广州市天河区体育西路', goods: [{ name: '运动跑鞋 透气网面', spec: '42码/白色', price: 299, qty: 1 }] },
    { id: 5, orderNo: '202609090005', buyer: '孙七', amount: '4599.00', status: 'pending_shipment', time: '2026-09-09 11:00:00', receiver: '孙七', phone: '135****5555', address: '杭州市西湖区文三路', goods: [{ name: '轻薄笔记本电脑 14英寸', spec: 'i5/16GB/512GB', price: 4599, qty: 1 }] },
  ]
  total.value = list.value.length
}

function statusText(status) {
  const map = { pending_payment: '待付款', pending_shipment: '待发货', pending_receipt: '待收货', completed: '已完成', cancelled: '已取消' }
  return map[status] || status
}

function statusType(status) {
  const map = { pending_payment: 'danger', pending_shipment: 'warning', pending_receipt: 'primary', completed: 'success', cancelled: 'info' }
  return map[status] || 'info'
}

function showDetail(row) {
  detail.value = row
  detailVisible.value = true
}

function handleShip(row) {
  shipOrderId.value = row.id
  shipForm.company = ''
  shipForm.trackingNo = ''
  shipVisible.value = true
}

function confirmShip() {
  if (!shipForm.company || !shipForm.trackingNo) {
    ElMessage.warning('请填写快递信息')
    return
  }
  const order = list.value.find(o => o.id === shipOrderId.value)
  if (order) {
    order.status = 'pending_receipt'
  }
  ElMessage.success('发货成功')
  shipVisible.value = false
}

function cancelOrder(row) {
  row.status = 'cancelled'
  ElMessage.success('订单已取消')
}

onMounted(loadData)
</script>

<style scoped>
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
.order-goods-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.goods-name {
  font-size: 12px;
  color: #666;
}
</style>
