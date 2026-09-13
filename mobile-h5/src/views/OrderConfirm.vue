<template>
  <div class="order-confirm-page">
    <van-nav-bar title="确认订单" left-arrow fixed placeholder @click-left="$router.back()" />

    <!-- 收货地址 -->
    <div class="address-section" @click="showAddressPicker = true">
      <div class="address-info" v-if="address.name">
        <div class="address-top">
          <span class="address-name">{{ address.name }}</span>
          <span class="address-phone">{{ address.phone }}</span>
          <van-icon name="arrow" color="#ccc" />
        </div>
        <div class="address-detail">{{ address.fullAddress }}</div>
      </div>
      <div class="address-empty" v-else>
        <van-icon name="add" color="#FF2E93" />
        <span>添加收货地址</span>
        <van-icon name="arrow" color="#ccc" />
      </div>
    </div>

    <!-- 商品列表 -->
    <div class="order-goods">
      <div class="goods-item" v-for="item in orderItems" :key="item.id">
        <div class="goods-img">
          <van-icon :name="item.icon" :size="36" :color="item.color" />
        </div>
        <div class="goods-info">
          <div class="goods-name van-multi-ellipsis--l2">{{ item.name }}</div>
          <div class="goods-spec">{{ item.spec }}</div>
          <div class="goods-bottom">
            <span class="goods-price">
              <span class="sym">¥</span>{{ item.price }}
            </span>
            <span class="goods-qty">x{{ item.quantity }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 配送方式 -->
    <div class="section-block">
      <div class="section-row">
        <span class="section-label">配送方式</span>
        <span class="section-value">快递配送</span>
      </div>
      <div class="section-row">
        <span class="section-label">运费</span>
        <span class="section-value" style="color:#FF2E93">免运费</span>
      </div>
    </div>

    <!-- 备注 -->
    <div class="section-block">
      <div class="section-row">
        <span class="section-label">订单备注</span>
        <input
          v-model="remark"
          class="remark-input"
          placeholder="选填：备注信息"
        />
      </div>
    </div>

    <!-- 优惠 -->
    <div class="section-block">
      <div class="section-row">
        <span class="section-label">优惠券</span>
        <span class="section-value" style="color:#999">无可用</span>
      </div>
    </div>

    <!-- 底部金额 -->
    <div class="order-summary">
      <div class="summary-row">
        <span>商品小计</span>
        <span>¥{{ subtotal }}</span>
      </div>
      <div class="summary-row">
        <span>运费</span>
        <span style="color:#FF2E93">免运费</span>
      </div>
    </div>

    <!-- 底部提交栏 -->
    <div class="bottom-bar">
      <div class="bar-total">
        <span class="bar-label">合计：</span>
        <span class="bar-price">
          <span class="sym">¥</span>{{ subtotal }}
        </span>
      </div>
      <div class="submit-btn" @click="handleSubmit">提交订单</div>
    </div>

    <!-- 地址选择弹窗 -->
    <van-action-sheet v-model:show="showAddressPicker" title="选择收货地址" round>
      <div class="address-list">
        <div
          class="address-card"
          v-for="(addr, i) in addressList"
          :key="i"
          :class="{ active: address.name === addr.name }"
          @click="selectAddress(addr)"
        >
          <div class="addr-top">
            <span class="addr-name">{{ addr.name }}</span>
            <span class="addr-phone">{{ addr.phone }}</span>
            <van-tag v-if="addr.default" plain type="danger" size="small">默认</van-tag>
          </div>
          <div class="addr-detail">{{ addr.fullAddress }}</div>
        </div>
      </div>
    </van-action-sheet>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'

const router = useRouter()
const remark = ref('')
const showAddressPicker = ref(false)

// 模拟地址
const address = ref({
  name: '张三',
  phone: '138****8888',
  fullAddress: '广东省深圳市南山区科技园南区XX大厦A座1201',
})
const addressList = ref([
  { name: '张三', phone: '138****8888', fullAddress: '广东省深圳市南山区科技园南区XX大厦A座1201', default: true },
  { name: '李四', phone: '139****9999', fullAddress: '北京市朝阳区建国路88号XX中心B座501', default: false },
])

// 模拟商品
const orderItems = ref([
  { id: 1, icon: 'phone-o', color: '#FF2E93', name: '旗舰智能手机 5G全网通 高清拍照 长续航', spec: '曜石黑 / 12GB+256GB', price: '3999', quantity: 1 },
  { id: 2, icon: 'tv-o', color: '#667eea', name: '智能4K电视 65英寸 全面屏 家庭影院', spec: '65英寸 / 4K超清', price: '3299', quantity: 1 },
])

const subtotal = computed(() => {
  return orderItems.value.reduce((s, i) => s + parseFloat(i.price) * i.quantity, 0).toFixed(2)
})

function selectAddress(addr) {
  address.value = addr
  showAddressPicker.value = false
}

function handleSubmit() {
  showConfirmDialog({
    title: '确认下单',
    message: `合计支付 ¥${subtotal}，确认提交订单？`,
    confirmButtonColor: '#FF2E93',
  }).then(() => {
    showToast('下单成功')
    // 跳转到支付页
    router.push('/order/payment?orderNo=' + Date.now())
  }).catch(() => {})
}
</script>

<style scoped>
.order-confirm-page {
  background: #f5f5f5;
  min-height: 100vh;
  padding-bottom: 60px;
}

/* 地址 */
.address-section {
  background: #fff;
  margin-bottom: 10px;
  padding: 14px 16px;
}
.address-info {
  cursor: pointer;
}
.address-top {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}
.address-name {
  font-size: 15px;
  font-weight: 600;
  color: #222;
}
.address-phone {
  font-size: 13px;
  color: #666;
}
.address-detail {
  font-size: 13px;
  color: #999;
}
.address-empty {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #333;
}

/* 商品 */
.order-goods {
  background: #fff;
  padding: 0 16px;
  margin-bottom: 10px;
}
.goods-item {
  display: flex;
  gap: 10px;
  padding: 12px 0;
  border-bottom: 1px solid #f5f5f5;
}
.goods-item:last-child {
  border-bottom: none;
}
.goods-img {
  width: 72px;
  height: 72px;
  background: #fafafa;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.goods-info {
  flex: 1;
  min-width: 0;
}
.goods-name {
  font-size: 13px;
  color: #222;
  line-height: 1.4;
}
.goods-spec {
  font-size: 11px;
  color: #999;
  margin-top: 2px;
}
.goods-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 6px;
}
.goods-price {
  color: #FF2E93;
  font-weight: 700;
  font-size: 16px;
}
.goods-price .sym { font-size: 11px; }
.goods-qty {
  font-size: 12px;
  color: #999;
}

/* 区块 */
.section-block {
  background: #fff;
  padding: 0 16px;
  margin-bottom: 10px;
}
.section-row {
  display: flex;
  align-items: center;
  padding: 14px 0;
  border-bottom: 1px solid #f5f5f5;
}
.section-row:last-child { border-bottom: none; }
.section-label {
  font-size: 14px;
  color: #666;
  width: 70px;
  flex-shrink: 0;
}
.section-value {
  flex: 1;
  font-size: 14px;
  color: #333;
  text-align: right;
}
.remark-input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 14px;
  text-align: right;
  color: #333;
}
.remark-input::placeholder {
  color: #ccc;
}

/* 汇总 */
.order-summary {
  background: #fff;
  padding: 0 16px;
  margin-bottom: 10px;
}
.summary-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  font-size: 13px;
  color: #666;
}

/* 底部 */
.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 56px;
  background: #fff;
  display: flex;
  align-items: center;
  padding: 0 12px;
  gap: 10px;
  border-top: 1px solid #f0f0f0;
  z-index: 99;
}
.bar-total {
  flex: 1;
  text-align: right;
}
.bar-label {
  font-size: 13px;
  color: #666;
}
.bar-price {
  color: #FF2E93;
  font-weight: 700;
  font-size: 20px;
}
.bar-price .sym { font-size: 13px; }
.submit-btn {
  width: 120px;
  height: 40px;
  line-height: 40px;
  text-align: center;
  background: linear-gradient(135deg, #FF2E93, #ff6b9d);
  color: #fff;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
}

/* 地址选择 */
.address-list {
  padding: 0 16px 20px;
  max-height: 50vh;
  overflow-y: auto;
}
.address-card {
  padding: 14px;
  border: 1px solid #f0f0f0;
  border-radius: 10px;
  margin-bottom: 10px;
}
.address-card.active {
  border-color: #FF2E93;
  background: #fff5f8;
}
.addr-top {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}
.addr-name {
  font-size: 14px;
  font-weight: 600;
  color: #222;
}
.addr-phone {
  font-size: 12px;
  color: #666;
}
.addr-detail {
  font-size: 12px;
  color: #999;
}
</style>
