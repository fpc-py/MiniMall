<template>
  <div class="profile-page">
    <van-nav-bar title="个人中心" fixed placeholder />

    <!-- 会员头部 -->
    <div class="profile-header">
      <van-skeleton v-if="loading" title avatar :row="2" style="padding: 16px" />
      <div v-else class="member-info">
        <div class="avatar">
          <img v-if="member.avatar" :src="member.avatar" class="avatar-el" alt="" />
          <van-icon v-else name="user-o" size="32" color="#fff" />
        </div>
        <div class="member-detail">
          <div class="member-name">{{ member.nickname || member.username || '商城会员' }}</div>
          <div class="member-phone">{{ member.phone || '-' }}</div>
        </div>
        <div class="member-level">
          <van-tag color="#fff" text-color="#FF2E93" round>Lv.{{ member.level || 0 }}</van-tag>
        </div>
      </div>
      <div v-if="!loading" class="points-row">
        <span class="points-label">积分</span>
        <span class="points-val">{{ member.points || 0 }}</span>
      </div>
    </div>

    <!-- 功能入口 -->
    <van-cell-group inset class="menu-group">
      <van-cell title="我的订单" icon="orders-o" is-link @click="go('/order/list')" />
      <van-cell title="我的优惠券" icon="coupon-o" is-link @click="go('/coupons')" />
      <van-cell title="收货地址" icon="location-o" is-link @click="go('/address')" />
      <van-cell title="退款 / 售后" icon="after-sale" is-link @click="go('/refund/list')" />
    </van-cell-group>

    <div class="logout-section">
      <van-button block round plain type="danger" color="#999" @click="handleLogout">退出登录</van-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog } from 'vant'
import { getMemberMe } from '@/api/member'
import { clearAuth } from '@/utils/auth'

const router = useRouter()

const loading = ref(true)
const member = ref({})

async function loadMember() {
  loading.value = true
  try {
    const res = await getMemberMe()
    member.value = res.data || {}
  } catch (e) {
    member.value = {}
  } finally {
    loading.value = false
  }
}

function go(path) {
  router.push(path)
}

function handleLogout() {
  showConfirmDialog({
    title: '提示',
    message: '确定要退出登录吗？',
    confirmButtonColor: '#FF2E93',
  })
    .then(() => {
      clearAuth()
      router.replace('/login')
    })
    .catch(() => {})
}

onMounted(loadMember)
</script>

<style scoped>
.profile-page {
  background: #f5f5f5;
  min-height: 100vh;
  padding-bottom: 30px;
}

.profile-header {
  background: linear-gradient(135deg, #FF2E93, #ff6b9d);
  color: #fff;
  padding: 16px;
}
.member-info {
  display: flex;
  align-items: center;
  gap: 12px;
}
.avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.25);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
}
.avatar-el {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.member-detail {
  flex: 1;
  min-width: 0;
}
.member-name {
  font-size: 18px;
  font-weight: 700;
}
.member-phone {
  font-size: 13px;
  opacity: 0.9;
  margin-top: 4px;
}
.member-level {
  flex-shrink: 0;
}
.points-row {
  display: flex;
  align-items: baseline;
  gap: 6px;
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.25);
}
.points-label {
  font-size: 13px;
  opacity: 0.9;
}
.points-val {
  font-size: 20px;
  font-weight: 700;
}

.menu-group {
  margin-top: 12px;
}

.logout-section {
  padding: 24px 16px 0;
}
</style>
