<template>
  <van-config-provider theme="dark">
    <div id="app" class="app-shell">
      <!-- 环境辉光：为"暗夜"提供纵深，纯装饰 -->
      <div class="ambient" aria-hidden="true">
        <span class="orb orb-a"></span>
        <span class="orb orb-b"></span>
        <span class="orb orb-c"></span>
        <span class="grid-lines"></span>
      </div>

      <router-view v-slot="{ Component }">
        <transition name="page-shift" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>

      <van-tabbar
        v-if="showTabbar"
        v-model="active"
        route
        fixed
        placeholder
        safe-area-inset-bottom
      >
        <van-tabbar-item icon="wap-home-o" to="/">首页</van-tabbar-item>
        <van-tabbar-item icon="cart-o" to="/cart">购物车</van-tabbar-item>
        <van-tabbar-item icon="orders-o" to="/order/list">订单</van-tabbar-item>
        <van-tabbar-item icon="user-o" to="/profile">我的</van-tabbar-item>
      </van-tabbar>
    </div>
  </van-config-provider>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const tabRoutes = ['/', '/cart', '/order/list', '/profile']
const active = ref(0)

const showTabbar = computed(() => tabRoutes.includes(route.path))
</script>

<style>
.app-shell {
  position: relative;
  min-height: 100vh;
  isolation: isolate;
}

/* 环境辉光层：贴底、不抢焦点 */
.ambient {
  position: fixed;
  inset: -10% -20% 0;
  z-index: -1;
  pointer-events: none;
  overflow: hidden;
}
.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(58px);
  opacity: 0.5;
  animation: glowPulse 9s var(--ease-out) infinite;
}
.orb-a {
  width: 240px;
  height: 240px;
  left: -60px;
  top: 40px;
  background: radial-gradient(circle, rgba(255, 46, 147, 0.85), transparent 70%);
}
.orb-b {
  width: 220px;
  height: 220px;
  right: -70px;
  top: 180px;
  background: radial-gradient(circle, rgba(36, 229, 255, 0.7), transparent 70%);
  animation-delay: 2.4s;
}
.orb-c {
  width: 300px;
  height: 300px;
  left: 20%;
  bottom: -120px;
  background: radial-gradient(circle, rgba(139, 92, 255, 0.75), transparent 70%);
  animation-delay: 4.6s;
}

/* 极淡的科技网格，替代纯色背景 */
.grid-lines {
  position: absolute;
  inset: 0;
  opacity: 0.28;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.045) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.045) 1px, transparent 1px);
  background-size: 44px 44px;
  mask-image: radial-gradient(90% 60% at 50% 0%, #000 20%, transparent 78%);
  -webkit-mask-image: radial-gradient(90% 60% at 50% 0%, #000 20%, transparent 78%);
}

/* 路由切换：位移 + 淡入，替代生硬跳变 */
.page-shift-enter-active,
.page-shift-leave-active {
  transition: opacity 0.28s var(--ease-out), transform 0.28s var(--ease-out);
}
.page-shift-enter-from {
  opacity: 0;
  transform: translate3d(0, 10px, 0);
}
.page-shift-leave-to {
  opacity: 0;
  transform: translate3d(0, -6px, 0);
}

/* 底部导航：毛玻璃 + 霓虹选中态 */
.van-tabbar {
  --van-tabbar-item-active-color: var(--neon);
  --van-tabbar-item-text-color: var(--text-3);
  border-top: none;
}
.van-tabbar-item__icon {
  font-size: 21px;
}
.van-tabbar-item--active {
  position: relative;
}
.van-tabbar-item--active::after {
  content: "";
  position: absolute;
  top: 6px;
  left: 50%;
  width: 22px;
  height: 3px;
  border-radius: var(--r-pill);
  transform: translateX(-50%);
  background: linear-gradient(90deg, var(--neon), var(--cyan));
  box-shadow: 0 0 10px rgba(255, 46, 147, 0.9);
}
</style>
