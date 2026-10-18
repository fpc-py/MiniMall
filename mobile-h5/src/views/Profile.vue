<template>
  <div class="page profile-page">
    <van-nav-bar title="个人中心" fixed placeholder />

    <!-- 会员头部 -->
    <section class="profile-header rv" style="--i: 0">
      <van-skeleton v-if="loading" title avatar :row="2" style="padding: 16px" />
      <div v-else class="member-info">
        <div class="avatar">
          <span class="avatar-ring" aria-hidden="true"></span>
          <img v-if="member.avatar" :src="member.avatar" class="avatar-el" alt="" />
          <van-icon v-else name="user-o" size="30" color="#fff" />
        </div>
        <div class="member-detail">
          <div class="member-name">{{ member.nickname || member.username || '商城会员' }}</div>
          <div class="member-phone num">{{ member.phone || '-' }}</div>
        </div>
        <div class="member-level">
          <span class="level-chip num">LV.{{ member.level || 0 }}</span>
        </div>
      </div>
      <div v-if="!loading" class="points-row">
        <span class="points-label">积分</span>
        <span class="points-val num">{{ member.points || 0 }}</span>
      </div>
    </section>

    <!-- 功能入口 -->
    <van-cell-group inset class="menu-group rv" style="--i: 1">
      <van-cell title="我的订单" icon="orders-o" is-link @click="go('/order/list')" />
      <van-cell title="我的优惠券" icon="coupon-o" is-link @click="go('/coupons')" />
      <van-cell title="收货地址" icon="location-o" is-link @click="go('/address')" />
      <van-cell title="退款 / 售后" icon="after-sale" is-link @click="go('/refund/list')" />
    </van-cell-group>

    <div class="logout-section rv" style="--i: 2">
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
  padding-bottom: 34px;
}

/* ---------- 会员卡片 ---------- */
.profile-header {
  position: relative;
  margin: 0 var(--gutter);
  padding: 20px 18px 18px;
  border-radius: var(--r-lg);
  overflow: hidden;
  background-image:
    radial-gradient(120% 140% at 0% 0%, rgba(255, 46, 147, 0.34), transparent 58%),
    linear-gradient(158deg, rgba(255, 255, 255, 0.09), rgba(255, 255, 255, 0.02));
  border: 1px solid var(--glass-line-2);
  backdrop-filter: blur(var(--blur)) saturate(160%);
  box-shadow: var(--shadow-card);
  animation: riseIn 0.6s var(--ease-out) both;
}
.member-info {
  display: flex;
  align-items: center;
  gap: 13px;
}
.avatar {
  position: relative;
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: var(--glass-2);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  border: 1px solid var(--glass-line-2);
}
/* 头像外圈霓虹光环 */
.avatar-ring {
  position: absolute;
  inset: -5px;
  border-radius: 50%;
  background: conic-gradient(from 140deg, var(--neon), var(--cyan), var(--violet), var(--neon));
  opacity: 0.85;
  filter: blur(6px);
  z-index: 0;
}
.avatar-el {
  position: relative;
  z-index: 1;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}
.avatar .van-icon {
  position: relative;
  z-index: 1;
}
.member-detail {
  flex: 1;
  min-width: 0;
}
.member-name {
  font-family: var(--font-display);
  font-size: 19px;
  font-weight: 700;
  color: var(--text-1);
  letter-spacing: 0.02em;
}
.member-phone {
  font-size: 12px;
  color: var(--text-3);
  margin-top: 5px;
  letter-spacing: 0.04em;
}
.member-level {
  flex-shrink: 0;
}
.level-chip {
  display: inline-block;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: #fff;
  padding: 4px 11px;
  border-radius: var(--r-pill);
  background-image: linear-gradient(135deg, var(--neon), var(--violet));
  box-shadow: var(--glow-neon);
}
.points-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-top: 18px;
  padding-top: 14px;
  border-top: 1px solid var(--glass-line);
}
.points-label {
  font-size: 11px;
  color: var(--text-3);
  letter-spacing: 0.18em;
}
.points-val {
  font-family: var(--font-display);
  font-size: 24px;
  font-weight: 700;
  color: var(--cyan);
  text-shadow: 0 0 20px rgba(36, 229, 255, 0.55);
}

/* ---------- 菜单 ---------- */
.menu-group {
  margin-top: var(--sp-3);
}
.menu-group :deep(.van-cell__left-icon) {
  color: var(--neon-soft);
  font-size: 18px;
}

.logout-section {
  padding: 26px var(--gutter) 0;
}
</style>
