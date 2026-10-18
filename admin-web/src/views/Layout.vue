<template>
  <el-container class="layout-container">
    <!-- 左侧菜单 -->
    <el-aside :width="isCollapse ? '64px' : '220px'" class="layout-aside">
      <div class="aside-header">
        <span v-if="!isCollapse" class="aside-title">{{ isMerchant ? 'Mall 商家后台' : 'Mall 管理后台' }}</span>
        <span v-else class="aside-title-short">M</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :collapse-transition="false"
        background-color="#001529"
        text-color="#ffffffa6"
        active-text-color="#FF2E93"
        router
      >
        <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <template #title>{{ item.title }}</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <!-- 右侧内容 -->
    <el-container class="layout-main">
      <!-- 顶部导航 -->
      <el-header class="layout-header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="isCollapse = !isCollapse" size="20">
            <Fold v-if="!isCollapse" />
            <Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: defaultPath }">{{ isMerchant ? '商家后台' : '管理后台' }}</el-breadcrumb-item>
            <el-breadcrumb-item>{{ pageTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-tag :type="isMerchant ? 'warning' : 'danger'" size="small" effect="light" class="role-tag">
            {{ isMerchant ? '商家' : '平台' }}
          </el-tag>
          <el-dropdown trigger="click">
            <span class="user-info">
              <el-avatar size="small" icon="UserFilled" style="background:#FF2E93" />
              <span class="user-name">{{ userStore.realName }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="handleLogout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 内容区 -->
      <el-main class="layout-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import {
  Odometer, FolderOpened, Goods, List, UserFilled, Shop, User, Ticket, RefreshLeft, DataLine, Setting,
  Fold, Expand, ArrowDown, SwitchButton,
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const isCollapse = ref(false)

const isMerchant = computed(() => userStore.isMerchant)
const defaultPath = computed(() => (isMerchant.value ? '/shop/dashboard' : '/dashboard'))

const platformMenus = [
  { path: '/dashboard', title: '仪表盘', icon: Odometer },
  { path: '/category', title: '类目管理', icon: FolderOpened },
  { path: '/product-spu', title: 'SPU 管理', icon: Goods },
  { path: '/orders', title: '订单管理', icon: List },
  { path: '/merchant-audit', title: '商家审核', icon: UserFilled },
  { path: '/shop-list', title: '店铺管理', icon: Shop },
  { path: '/members', title: '会员管理', icon: User },
  { path: '/coupons', title: '优惠券', icon: Ticket },
  { path: '/refunds', title: '退款管理', icon: RefreshLeft },
]

const merchantMenus = [
  { path: '/shop/dashboard', title: '店铺概览', icon: DataLine },
  { path: '/shop/products', title: '我的商品', icon: Goods },
  { path: '/shop/orders', title: '店铺订单', icon: List },
  { path: '/shop/refunds', title: '退款审核', icon: RefreshLeft },
  { path: '/shop/coupons', title: '店铺优惠券', icon: Ticket },
  { path: '/shop/info', title: '店铺信息', icon: Setting },
]

const menus = computed(() => (isMerchant.value ? merchantMenus : platformMenus))

const activeMenu = computed(() => route.path)
const pageTitle = computed(() => route.meta?.title || '页面')

function handleLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}
.layout-aside {
  background: #001529;
  transition: width 0.3s;
  overflow: hidden;
}
.aside-header {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 2px;
  border-bottom: 1px solid rgba(255,255,255,0.08);
}
.aside-title-short {
  font-size: 22px;
}
.el-menu {
  border-right: none;
}
.layout-main {
  display: flex;
  flex-direction: column;
}
.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  padding: 0 20px;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.collapse-btn {
  cursor: pointer;
  color: #666;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.role-tag {
  font-weight: 600;
}
.header-right .user-info {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
}
.user-name {
  font-size: 14px;
  color: #333;
}
.layout-content {
  background: #f5f6fa;
  padding: 20px;
  overflow-y: auto;
}
</style>
