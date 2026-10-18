<template>
  <div id="app">
    <router-view />
    <van-tabbar
      v-if="showTabbar"
      v-model="active"
      route
      active-color="#FF2E93"
      inactive-color="#999"
      border
      placeholder
    >
      <van-tabbar-item icon="home-o" to="/">首页</van-tabbar-item>
      <van-tabbar-item icon="cart-o" to="/cart">购物车</van-tabbar-item>
      <van-tabbar-item icon="description-o" to="/order/list">订单</van-tabbar-item>
      <van-tabbar-item icon="user-o" to="/profile">我的</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
const route = useRoute()
const tabRoutes = ['/', '/cart', '/order/list', '/profile']
const active = ref(0)

const showTabbar = computed(() => {
  return tabRoutes.includes(route.path)
})
</script>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
body { background: #f5f5f5; }
</style>