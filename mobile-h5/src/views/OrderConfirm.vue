<template>
  <div class="page order-confirm-page">
    <van-nav-bar title="确认订单" left-arrow fixed placeholder @click-left="router.back()" />

    <!-- 收货地址 -->
    <section class="address-section rv" style="--i: 0" @click="openAddressSheet">
      <div class="address-info" v-if="selectedAddress">
        <div class="address-top">
          <span class="address-name">{{ selectedAddress.receiverName }}</span>
          <span class="address-phone num">{{ selectedAddress.receiverPhone }}</span>
          <span v-if="selectedAddress.isDefault === 1" class="address-default">默认</span>
          <van-icon name="arrow" color="#6d7490" style="margin-left:auto" />
        </div>
        <div class="address-detail">{{ selectedAddress.fullAddress }}</div>
      </div>
      <div class="address-empty" v-else>
        <van-icon name="add" color="#ff2e93" />
        <span>添加收货地址</span>
        <van-icon name="arrow" color="#6d7490" style="margin-left:auto" />
      </div>
    </section>

    <!-- 商品列表 -->
    <section class="order-goods rv" style="--i: 1">
      <div class="goods-item" v-for="item in confirmData?.items || []" :key="item.skuId">
        <div class="goods-img">
          <img v-if="item.productImage" :src="item.productImage" class="goods-img-el" alt="" />
          <van-icon v-else name="photo-o" :size="36" color="#6d7490" />
        </div>
        <div class="goods-info">
          <div class="goods-name van-multi-ellipsis--l2">{{ item.productName }}</div>
          <div class="goods-spec">{{ item.attrs }}</div>
          <div class="goods-bottom">
            <span class="goods-price">
              <span class="sym">¥</span><span class="num">{{ money(item.price) }}</span>
            </span>
            <span class="goods-qty num">x{{ item.quantity }}</span>
          </div>
        </div>
      </div>
      <van-empty v-if="!confirmData" description="暂无可结算商品" />
    </section>

    <!-- 配送方式 -->
    <section class="section-block rv" style="--i: 2">
      <div class="section-row">
        <span class="section-label">配送方式</span>
        <span class="section-value">快递配送</span>
      </div>
      <div class="section-row">
        <span class="section-label">运费</span>
        <span class="section-value highlight">
          {{ freightText }}
        </span>
      </div>
    </section>

    <!-- 优惠券 -->
    <section class="section-block rv" style="--i: 3">
      <div class="section-row" @click="showCouponSheet = true">
        <span class="section-label">优惠券</span>
        <span class="section-value" :class="{ highlight: selectedCoupon }">
          {{ selectedCoupon ? couponText(selectedCoupon) : (availableCoupons.length ? availableCoupons.length + ' 张可用' : '无可用') }}
        </span>
        <van-icon name="arrow" color="#6d7490" />
      </div>
    </section>

    <!-- 备注 -->
    <section class="section-block rv" style="--i: 4">
      <div class="section-row">
        <span class="section-label">订单备注</span>
        <input
          v-model="remark"
          class="remark-input"
          placeholder="选填：备注信息"
        />
      </div>
    </section>

    <!-- 底部金额 -->
    <section class="order-summary rv" style="--i: 5">
      <div class="summary-row">
        <span>商品总额</span>
        <span class="num">¥{{ money(confirmData?.totalAmount) }}</span>
      </div>
      <div class="summary-row">
        <span>运费</span>
        <span class="num">{{ freightText }}</span>
      </div>
      <div class="summary-row">
        <span>优惠</span>
        <span class="num discount">-¥{{ money(confirmData?.discountAmount) }}</span>
      </div>
    </section>

    <!-- 底部提交栏 -->
    <div class="bottom-bar">
      <div class="bar-total">
        <span class="bar-label">合计</span>
        <span class="bar-price">
          <span class="sym">¥</span><span class="num">{{ money(confirmData?.payAmount) }}</span>
        </span>
      </div>
      <div class="submit-btn" :class="{ disabled: submitting }" @click="handleSubmit">
        {{ submitting ? '提交中...' : '提交订单' }}
      </div>
    </div>

    <!-- 地址选择弹窗 -->
    <van-action-sheet v-model:show="showAddressSheet" title="选择收货地址" round>
      <div class="address-list">
        <div class="manage-address" @click="goManageAddress">管理收货地址 ›</div>
        <div
          class="address-card"
          v-for="addr in addresses"
          :key="addr.id"
          :class="{ active: selectedAddress && selectedAddress.id === addr.id }"
          @click="selectAddress(addr)"
        >
          <div class="addr-top">
            <span class="addr-name">{{ addr.receiverName }}</span>
            <span class="addr-phone num">{{ addr.receiverPhone }}</span>
            <span v-if="addr.isDefault === 1" class="addr-default">默认</span>
          </div>
          <div class="addr-detail">{{ addr.fullAddress }}</div>
        </div>
        <van-empty v-if="!addresses.length" description="暂无收货地址" />
        <van-button block round type="danger" color="#FF2E93" style="margin-top:10px" @click="openAddAddress">
          新增收货地址
        </van-button>
      </div>
    </van-action-sheet>

    <!-- 优惠券选择弹窗 -->
    <van-action-sheet v-model:show="showCouponSheet" title="选择优惠券" round>
      <div class="coupon-list">
        <div
          class="coupon-card"
          v-for="c in availableCoupons"
          :key="c.id"
          :class="{ active: selectedCoupon && selectedCoupon.id === c.id }"
          @click="selectCoupon(c)"
        >
          <div class="coupon-left">
            <div class="coupon-amount num">{{ couponValue(c) }}</div>
            <div class="coupon-threshold num">满¥{{ money(c.thresholdAmount) }}可用</div>
          </div>
          <div class="coupon-right">
            <div class="coupon-name">{{ c.name }}</div>
            <div class="coupon-expire num">有效期至 {{ formatDate(c.expireTime) }}</div>
          </div>
        </div>
        <van-empty v-if="!availableCoupons.length" description="暂无可用优惠券" />
        <van-button block round plain type="danger" color="#999" style="margin-top:10px" @click="clearCoupon">
          不使用优惠券
        </van-button>
      </div>
    </van-action-sheet>

    <!-- 新增地址弹窗 -->
    <van-popup v-model:show="showAddAddress" position="bottom" round :style="{ paddingBottom: '16px' }">
      <div class="add-addr-header">
        <span @click="showAddAddress = false">取消</span>
        <span class="title">新增收货地址</span>
        <span class="confirm" @click="submitAddress">保存</span>
      </div>
      <div class="add-addr-form">
        <van-field v-model="addrForm.receiverName" label="收货人" placeholder="请输入收货人姓名" />
        <van-field v-model="addrForm.receiverPhone" type="tel" label="手机号" placeholder="请输入手机号" maxlength="11" />
        <van-field v-model="addrForm.province" label="省份" placeholder="如：广东省" />
        <van-field v-model="addrForm.city" label="城市" placeholder="如：深圳市" />
        <van-field v-model="addrForm.district" label="区/县" placeholder="选填" />
        <van-field v-model="addrForm.detail" label="详细地址" placeholder="街道、门牌号等" />
        <van-field v-model="addrForm.zipCode" label="邮编" placeholder="选填" />
        <van-field v-model="addrForm.label" label="标签" placeholder="如：家 / 公司（选填）" />
        <van-cell title="设为默认地址" center>
          <template #right-icon>
            <van-switch v-model="addrForm.isDefault" size="20" active-color="#FF2E93" />
          </template>
        </van-cell>
      </div>
    </van-popup>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getAddresses, createAddress } from '@/api/address'
import { getMyCoupons } from '@/api/coupon'
import { getCart } from '@/api/cart'
import { confirmOrder, submitOrder } from '@/api/order'

const route = useRoute()
const router = useRouter()

const skuId = route.query.skuId ? Number(route.query.skuId) : null
const buyNow = skuId != null
const fromCart = !buyNow
const quantity = buyNow ? (Number(route.query.quantity) || 1) : undefined

const addresses = ref([])
const selectedAddress = ref(null)
const showAddressSheet = ref(false)

const orderShopIds = ref([])
const coupons = ref([])
const selectedCoupon = ref(null)
const showCouponSheet = ref(false)

const confirmData = ref(null)
const remark = ref('')
const submitting = ref(false)

const showAddAddress = ref(false)
const savingAddr = ref(false)
const addrForm = ref({
  receiverName: '',
  receiverPhone: '',
  province: '',
  city: '',
  district: '',
  detail: '',
  zipCode: '',
  label: '',
  isDefault: false,
})

const freightText = computed(() => {
  const f = Number(confirmData.value?.freightAmount || 0)
  return f > 0 ? `¥${f.toFixed(2)}` : '免运费'
})

const availableCoupons = computed(() => {
  const subtotal = confirmData.value?.totalAmount
  if (subtotal == null) return []
  const shops = orderShopIds.value
  return coupons.value.filter((c) => {
    if (Number(c.thresholdAmount || 0) > Number(subtotal)) return false
    if (c.shopId != null && shops.length > 0 && !shops.includes(c.shopId)) return false
    return true
  })
})

function money(v) {
  return Number(v || 0).toFixed(2)
}

function formatDate(t) {
  if (!t) return ''
  return String(t).slice(0, 10)
}

function couponValue(c) {
  if (c.type === 2) {
    const rate = Number(c.discountRate)
    return `${Number((rate * 10).toFixed(1))}折`
  }
  return `¥${money(c.discountAmount)}`
}

function couponText(c) {
  return c.type === 2 ? couponValue(c) : `满减 ¥${money(c.discountAmount)}`
}

function confirmPayload() {
  const p = { fromCart, remark: remark.value }
  if (selectedAddress.value) p.addressId = selectedAddress.value.id
  if (buyNow) {
    p.skuId = skuId
    p.quantity = quantity
  }
  if (selectedCoupon.value) p.couponMemberId = selectedCoupon.value.id
  return p
}

async function loadAddresses() {
  try {
    const res = await getAddresses()
    const list = res.data || []
    addresses.value = list
    if (!selectedAddress.value && list.length) {
      selectedAddress.value = list.find((a) => a.isDefault === 1) || list[0]
    }
  } catch (e) {
    showToast(e.message || '地址加载失败')
  }
}

async function loadCoupons() {
  try {
    const res = await getMyCoupons({ status: 0, pageNum: 1, pageSize: 100 })
    coupons.value = res.data?.list || []
  } catch (e) {
    coupons.value = []
  }
}

async function loadOrderShops() {
  if (!fromCart) return
  try {
    const res = await getCart()
    const shops = res.data?.shops || []
    orderShopIds.value = shops
      .filter((s) => (s.items || []).some((i) => i.selected))
      .map((s) => s.shopId)
  } catch (e) {
    orderShopIds.value = []
  }
}

async function refreshConfirm() {
  if (!selectedAddress.value) {
    confirmData.value = null
    return
  }
  try {
    const res = await confirmOrder(confirmPayload())
    confirmData.value = res.data
  } catch (e) {
    showToast(e.message || '试算失败')
  }
}

async function selectAddress(addr) {
  selectedAddress.value = addr
  showAddressSheet.value = false
  await refreshConfirm()
}

async function selectCoupon(c) {
  selectedCoupon.value = c
  showCouponSheet.value = false
  await refreshConfirm()
}

async function clearCoupon() {
  selectedCoupon.value = null
  showCouponSheet.value = false
  await refreshConfirm()
}

function openAddAddress() {
  showAddressSheet.value = false
  showAddAddress.value = true
}

async function openAddressSheet() {
  showAddressSheet.value = true
  await loadAddresses()
}

function goManageAddress() {
  showAddressSheet.value = false
  router.push('/address')
}

async function submitAddress() {
  const f = addrForm.value
  if (!f.receiverName) {
    showToast('请输入收货人姓名')
    return
  }
  if (!/^1[3-9]\d{9}$/.test(f.receiverPhone)) {
    showToast('请输入正确的手机号')
    return
  }
  if (!f.province) {
    showToast('请输入省份')
    return
  }
  if (!f.city) {
    showToast('请输入城市')
    return
  }
  if (!f.detail) {
    showToast('请输入详细地址')
    return
  }
  savingAddr.value = true
  try {
    const res = await createAddress({ ...f })
    showToast('地址添加成功')
    showAddAddress.value = false
    await loadAddresses()
    if (res.data && res.data.id) selectedAddress.value = res.data
    await refreshConfirm()
  } catch (e) {
    showToast(e.message || '添加失败')
  } finally {
    savingAddr.value = false
  }
}

async function handleSubmit() {
  if (!selectedAddress.value) {
    showToast('请先添加收货地址')
    return
  }
  if (!confirmData.value || !confirmData.value.confirmToken) {
    showToast('暂不可提交，请稍后重试')
    return
  }
  if (submitting.value) return
  submitting.value = true
  try {
    const res = await submitOrder({
      ...confirmPayload(),
      confirmToken: confirmData.value.confirmToken,
    })
    showToast('下单成功')
    router.replace('/order/payment?id=' + res.data.id)
  } catch (e) {
    showToast(e.message || '下单失败')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadAddresses(), loadCoupons(), loadOrderShops()])
  await refreshConfirm()
})
</script>

<style scoped>
.order-confirm-page {
  padding-bottom: 74px;
}

/* ---------- 地址 ---------- */
.address-section {
  position: relative;
  margin: 0 var(--gutter);
  padding: 16px;
  border-radius: var(--r-lg);
  cursor: pointer;
  background-image: linear-gradient(158deg, rgba(36, 229, 255, 0.12), rgba(255, 255, 255, 0.02) 55%, rgba(255, 46, 147, 0.1));
  border: 1px solid var(--glass-line-2);
  backdrop-filter: blur(var(--blur)) saturate(150%);
  box-shadow: var(--shadow-card);
}
.address-top {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 5px;
}
.address-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-1);
}
.address-phone {
  font-size: 13px;
  color: var(--text-2);
}
.address-default {
  font-size: 10px;
  letter-spacing: 0.1em;
  color: #fff;
  padding: 1px 8px;
  border-radius: var(--r-pill);
  background-image: linear-gradient(135deg, var(--neon), var(--violet));
  box-shadow: 0 0 12px rgba(255, 46, 147, 0.75);
}
.address-detail {
  font-size: 12px;
  color: var(--text-3);
  line-height: 1.6;
}
.address-empty {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 14px;
  color: var(--text-1);
}

/* ---------- 商品 ---------- */
.order-goods {
  margin: var(--sp-3) var(--gutter) 0;
  padding: 2px 16px;
  border-radius: var(--r-md);
  background-image: linear-gradient(158deg, rgba(255, 255, 255, 0.06), rgba(255, 255, 255, 0.015));
  border: 1px solid var(--glass-line);
  backdrop-filter: blur(var(--blur)) saturate(150%);
  box-shadow: var(--shadow-card);
}
.goods-item {
  display: flex;
  gap: 11px;
  padding: 13px 0;
  border-bottom: 1px solid var(--glass-line);
}
.goods-item:last-child {
  border-bottom: none;
}
.goods-img {
  width: 72px;
  height: 72px;
  border-radius: var(--r-sm);
  background: var(--glass);
  border: 1px solid var(--glass-line);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
}
.goods-img-el {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.goods-info {
  flex: 1;
  min-width: 0;
}
.goods-name {
  font-size: 13px;
  color: var(--text-1);
  line-height: 1.42;
}
.goods-spec {
  font-size: 11px;
  color: var(--text-3);
  margin-top: 3px;
}
.goods-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 7px;
}
.goods-price {
  display: flex;
  align-items: baseline;
  gap: 1px;
  color: var(--neon);
  font-weight: 700;
}
.goods-price .sym {
  font-size: 11px;
}
.goods-price .num {
  font-size: 16px;
}
.goods-qty {
  font-size: 12px;
  color: var(--text-3);
}

/* ---------- 区块 ---------- */
.section-block {
  margin: var(--sp-3) var(--gutter) 0;
  padding: 0 16px;
  border-radius: var(--r-md);
  background-image: linear-gradient(158deg, rgba(255, 255, 255, 0.06), rgba(255, 255, 255, 0.015));
  border: 1px solid var(--glass-line);
  backdrop-filter: blur(var(--blur)) saturate(150%);
  box-shadow: var(--shadow-card);
}
.section-row {
  display: flex;
  align-items: center;
  padding: 15px 0;
  border-bottom: 1px solid var(--glass-line);
}
.section-block .section-row:last-child {
  border-bottom: none;
}
.section-label {
  font-size: 13px;
  color: var(--text-3);
  width: 66px;
  flex-shrink: 0;
  letter-spacing: 0.06em;
}
.section-value {
  flex: 1;
  font-size: 14px;
  color: var(--text-1);
  text-align: right;
}
.section-value.highlight {
  color: var(--neon);
  font-weight: 600;
}
.remark-input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: 14px;
  text-align: right;
  color: var(--text-1);
  font-family: inherit;
}
.remark-input::placeholder {
  color: var(--text-3);
}

/* ---------- 汇总 ---------- */
.order-summary {
  margin: var(--sp-3) var(--gutter) 0;
  padding: 6px 16px;
  border-radius: var(--r-md);
  background: var(--glass);
  border: 1px solid var(--glass-line);
  backdrop-filter: blur(var(--blur));
}
.summary-row {
  display: flex;
  justify-content: space-between;
  padding: 9px 0;
  font-size: 13px;
  color: var(--text-2);
}
.summary-row .discount {
  color: var(--ok);
}

/* ---------- 底部 ---------- */
.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 64px;
  display: flex;
  align-items: center;
  padding: 0 var(--gutter);
  gap: 10px;
  backdrop-filter: blur(var(--blur-lg)) saturate(160%);
  -webkit-backdrop-filter: blur(var(--blur-lg)) saturate(160%);
  background-image: linear-gradient(180deg, rgba(12, 14, 22, 0.72), rgba(8, 9, 15, 0.96));
  border-top: 1px solid var(--glass-line);
  box-shadow: var(--shadow-float);
  z-index: 99;
}
.bar-total {
  flex: 1;
  display: flex;
  align-items: baseline;
  justify-content: flex-end;
  gap: 5px;
}
.bar-label {
  font-size: 11px;
  color: var(--text-3);
  letter-spacing: 0.1em;
}
.bar-price {
  display: flex;
  align-items: baseline;
  gap: 1px;
  color: var(--neon);
  font-weight: 700;
}
.bar-price .sym {
  font-size: 13px;
}
.bar-price .num {
  font-size: 22px;
  text-shadow: 0 0 18px rgba(255, 46, 147, 0.6);
}
.submit-btn {
  width: 126px;
  height: 42px;
  line-height: 42px;
  text-align: center;
  border-radius: var(--r-pill);
  background-image: linear-gradient(135deg, var(--neon), #ff5ea8 55%, var(--violet));
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.06em;
  box-shadow: var(--glow-neon);
}
.submit-btn.disabled {
  opacity: 0.55;
}

/* ---------- 地址选择 ---------- */
.address-list {
  padding: 0 16px 22px;
  max-height: 52vh;
  overflow-y: auto;
}
.address-card {
  padding: 14px;
  border: 1px solid var(--glass-line);
  border-radius: var(--r-md);
  margin-bottom: 10px;
  background: var(--glass);
  transition: all var(--dur-fast) var(--ease-out);
}
.address-card.active {
  border-color: rgba(255, 46, 147, 0.65);
  background-image: linear-gradient(140deg, rgba(255, 46, 147, 0.14), rgba(255, 255, 255, 0.02));
  box-shadow: var(--glow-neon);
}
.manage-address {
  text-align: right;
  font-size: 12px;
  color: var(--neon-soft);
  padding: 0 2px 12px;
}
.addr-top {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 5px;
}
.addr-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-1);
}
.addr-phone {
  font-size: 12px;
  color: var(--text-2);
}
.addr-default {
  font-size: 10px;
  color: var(--neon-soft);
  border: 1px solid rgba(255, 46, 147, 0.5);
  border-radius: var(--r-pill);
  padding: 0 7px;
}
.addr-detail {
  font-size: 12px;
  color: var(--text-3);
  line-height: 1.55;
}

/* ---------- 优惠券选择 ---------- */
.coupon-list {
  padding: 0 16px 22px;
  max-height: 52vh;
  overflow-y: auto;
}
.coupon-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 13px;
  border: 1px solid var(--glass-line);
  border-radius: var(--r-md);
  margin-bottom: 10px;
  background: var(--glass);
  transition: all var(--dur-fast) var(--ease-out);
}
.coupon-card.active {
  border-color: rgba(255, 46, 147, 0.65);
  background-image: linear-gradient(140deg, rgba(255, 46, 147, 0.14), rgba(255, 255, 255, 0.02));
  box-shadow: var(--glow-neon);
}
.coupon-left {
  width: 92px;
  text-align: center;
  border-right: 1px dashed var(--glass-line-2);
  flex-shrink: 0;
}
.coupon-amount {
  font-family: var(--font-display);
  font-size: 19px;
  font-weight: 700;
  color: var(--neon);
  text-shadow: 0 0 16px rgba(255, 46, 147, 0.55);
}
.coupon-threshold {
  font-size: 10px;
  color: var(--text-3);
  margin-top: 4px;
}
.coupon-right {
  flex: 1;
  min-width: 0;
}
.coupon-name {
  font-size: 13px;
  color: var(--text-1);
  font-weight: 500;
}
.coupon-expire {
  font-size: 11px;
  color: var(--text-3);
  margin-top: 5px;
}

/* ---------- 新增地址 ---------- */
.add-addr-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 15px 16px;
  font-size: 13px;
  color: var(--text-2);
  border-bottom: 1px solid var(--glass-line);
}
.add-addr-header .title {
  font-family: var(--font-display);
  font-size: 16px;
  font-weight: 700;
  color: var(--text-1);
  letter-spacing: 0.04em;
}
.add-addr-header .confirm {
  color: var(--neon);
  font-weight: 600;
}
.add-addr-form {
  padding-bottom: 12px;
}
</style>
