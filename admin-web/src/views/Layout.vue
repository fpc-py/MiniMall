<template>
  <el-container class="layout-container">
    <!-- 左侧菜单 -->
    <el-aside :width="isCollapse ? '64px' : '220px'" class="layout-aside">
      <div class="aside-header">
        <span v-if="!isCollapse" class="aside-title">Mall 管理后台</span>
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
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <template #title>仪表盘</template>
        </el-menu-item>
        <el-menu-item index="/category">
          <el-icon><FolderOpened /></el-icon>
          <template #title>类目管理</template>
        </el-menu-item>
        <el-menu-item index="/product-spu">
          <el-icon><Goods /></el-icon>
          <template #title>SPU 管理</template>
        </el-menu-item>
        <el-menu-item index="/orders">
          <el-icon><List /></el-icon>
          <template #title>订单管理</template>
        </el-menu-item>
        <el-menu-item index="/merchant-audit">
          <el-icon><UserFilled /></el-icon>
          <template #title>商家审核</template>
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
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ pageTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-dropdown trigger="click">
            <span class="user-info">
              <el-avatar size="small" icon="UserFilled" style="background:#FF2E93" />
              <span class="user-name">{{ userStore.username }}</span>
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
  Odometer, FolderOpened, Goods, List, UserFilled,
  Fold, Expand, ArrowDown, SwitchButton,
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const isCollapse = ref(false)

const activeMenu = computed(() => route.path)

const pageTitle = computed(() => {
  const map = {
    '/dashboard': '仪表盘',
    '/category': '类目管理',
    '/product-spu': 'SPU 管理',
    '/orders': '订单管理',
    '/merchant-audit': '商家审核',
  }
  return map[route.path] || '页面'
})

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
