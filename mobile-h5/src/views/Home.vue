<template>
  <div class="home-page">
    <!-- 顶部搜索栏 -->
    <div class="search-header">
      <div class="search-bar" @click="$router.push('/search')">
        <van-icon name="search" size="16" color="#999" />
        <span class="search-text">搜一搜心仪好物</span>
      </div>
      <van-icon name="chat-o" size="22" color="#333" />
    </div>

    <!-- 轮播图 -->
    <div class="swiper-section">
      <van-swipe :autoplay="3000" indicator-color="#FF2E93" lazy-render>
        <van-swipe-item v-for="(banner, i) in banners" :key="i">
          <div class="swiper-item" :style="{ background: banner.bg }">
            <span class="swiper-title">{{ banner.title }}</span>
            <span class="swiper-sub">{{ banner.sub }}</span>
          </div>
        </van-swipe-item>
      </van-swipe>
    </div>

    <!-- 金刚区 -->
    <div class="nav-grid-section">
      <div class="nav-grid">
        <div
          class="nav-item"
          v-for="(item, i) in navItems" @click="$router.push('/product/' + (i+1))"
          :key="i"
        >
          <div class="nav-icon" :style="{ background: item.bg }">
            <van-icon :name="item.icon" :size="24" color="#fff" />
          </div>
          <span class="nav-label">{{ item.label }}</span>
        </div>
      </div>
    </div>

    <!-- 限时秒杀 -->
    <div class="flash-section">
      <div class="flash-header">
        <div class="flash-title-left">
          <span class="flash-label">限时秒杀</span>
          <span class="flash-tag">爆款返场</span>
        </div>
        <div class="flash-countdown">
          <span class="countdown-label">距结束</span>
          <van-count-down
            :time="flashTime"
            format="HH:mm:ss"
            :auto-start="true"
          >
            <template #default="timeData">
              <span class="time-block">{{ pad(timeData.hours) }}</span>
              <span class="time-colon">:</span>
              <span class="time-block">{{ pad(timeData.minutes) }}</span>
              <span class="time-colon">:</span>
              <span class="time-block">{{ pad(timeData.seconds) }}</span>
            </template>
          </van-count-down>
        </div>
        <span class="flash-more">更多 ›</span>
      </div>
      <div class="flash-list">
        <div class="flash-item" v-for="(item, i) in flashItems" :key="i" @click="$router.push('/product/' + (i+1))">
          <div class="flash-img">
            <van-icon :name="item.icon" :size="36" :color="item.color" />
          </div>
          <div class="flash-price">
            <span class="price-symbol">¥</span>
            <span class="price-val">{{ item.price }}</span>
          </div>
          <div class="flash-old-price">¥{{ item.oldPrice }}</div>
        </div>
      </div>
    </div>

    <!-- 推荐商品 -->
    <div class="recommend-section">
      <div class="section-title">
        <span class="title-line"></span>
        <span class="title-text">为你推荐</span>
        <span class="title-line"></span>
      </div>
      <div class="product-grid">
        <div
          class="product-card"
          v-for="(item, i) in recommendList" @click="$router.push('/product/' + (i+1))"
          :key="i"
        >
          <div class="product-img">
            <van-icon :name="item.icon" :size="48" :color="item.color" />
          </div>
          <div class="product-info">
            <div class="product-name van-multi-ellipsis--l2">{{ item.name }}</div>
            <div class="product-price-row">
              <span class="product-price">
                <span class="price-symbol">¥</span>
                <span class="price-val">{{ item.price }}</span>
              </span>
              <span class="product-old-price">¥{{ item.oldPrice }}</span>
            </div>
            <div class="product-sales">已售 {{ item.sales }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const pad = (num) => String(num).padStart(2, '0')

// 轮播数据
const banners = [
  { bg: 'linear-gradient(135deg, #FF2E93 0%, #FF6B9D 100%)', title: '夏日焕新季', sub: '全场低至5折' },
  { bg: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)', title: '数码狂欢节', sub: '爆款直降' },
  { bg: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)', title: '美妆特卖', sub: '大牌满减' },
]

// 金刚区
const navItems = [
  { icon: 'phone-o', label: '手机', bg: '#FF2E93' },
  { icon: 'computer-o', label: '电脑', bg: '#667eea' },
  { icon: 'tv-o', label: '家电', bg: '#f5576c' },
  { icon: 'coupon-o', label: '数码', bg: '#4facfe' },
  { icon: 'more-o', label: '服饰', bg: '#43e97b' },
  { icon: 'shop-o', label: '超市', bg: '#fa709a' },
  { icon: 'gem-o', label: '生鲜', bg: '#fee140' },
  { icon: 'like-o', label: '美妆', bg: '#f093fb' },
  { icon: 'fire-o', label: '运动', bg: '#4facfe' },
  { icon: 'wap-nav', label: '更多', bg: '#a18cd1' },
]

// 秒杀倒计时（剩余 2 小时）
const flashTime = ref(2 * 60 * 60 * 1000)

// 秒杀商品
const flashItems = [
  { icon: 'phone-o', color: '#FF2E93', price: '1999', oldPrice: '2999' },
  { icon: 'tv-o', color: '#667eea', price: '2499', oldPrice: '3999' },
  { icon: 'sound-o', color: '#f5576c', price: '399', oldPrice: '699' },
  { icon: 'gem-o', color: '#4facfe', price: '89', oldPrice: '199' },
  { icon: 'clock-o', color: '#43e97b', price: '599', oldPrice: '999' },
  { icon: 'music-o', color: '#fa709a', price: '129', oldPrice: '299' },
]

// 推荐商品
const recommendList = [
  { icon: 'phone-o', color: '#FF2E93', name: '旗舰智能手机 5G全网通 高清拍照 长续航', price: '3999', oldPrice: '5999', sales: '1.2万' },
  { icon: 'laptop-o', color: '#667eea', name: '轻薄笔记本电脑 14英寸 商务办公 学生本', price: '4599', oldPrice: '5299', sales: '8563' },
  { icon: 'tv-o', color: '#f5576c', name: '智能4K电视 65英寸 全面屏 家庭影院', price: '3299', oldPrice: '4999', sales: '6321' },
  { icon: 'gem-o', color: '#4facfe', name: '蓝牙降噪耳机 无线 长续航 HiFi音质', price: '599', oldPrice: '999', sales: '2.1万' },
  { icon: 'more-o', color: '#43e97b', name: '运动跑鞋 透气网面 轻便减震 男女同款', price: '299', oldPrice: '599', sales: '1.8万' },
  { icon: 'like-o', color: '#fa709a', name: '精华液护肤品 保湿补水 提亮肤色', price: '239', oldPrice: '399', sales: '3.4万' },
]
</script>

<style scoped>
.home-page {
  padding-bottom: 50px;
  background: #f5f5f5;
  min-height: 100vh;
}

/* 顶部搜索栏 */
.search-header {
  display: flex;
  align-items: center;
  padding: 10px 16px;
  background: #fff;
  gap: 12px;
  position: sticky;
  top: 0;
  z-index: 100;
}
.search-bar {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 6px;
  height: 36px;
  background: #f5f5f5;
  border-radius: 18px;
  padding: 0 14px;
}
.search-text {
  color: #999;
  font-size: 13px;
}

/* 轮播 */
.swiper-section {
  padding: 0 12px;
  margin-top: 8px;
}
.swiper-item {
  height: 140px;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: #fff;
}
.swiper-title {
  font-size: 22px;
  font-weight: 700;
}
.swiper-sub {
  font-size: 14px;
  margin-top: 6px;
  opacity: 0.9;
}

/* 金刚区 */
.nav-grid-section {
  background: #fff;
  margin: 10px 12px;
  border-radius: 12px;
  padding: 16px 8px;
}
.nav-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px 0;
}
.nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}
.nav-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.nav-label {
  font-size: 12px;
  color: #333;
}

/* 限时秒杀 */
.flash-section {
  background: #fff;
  margin: 0 12px 10px;
  border-radius: 12px;
  padding: 14px 12px;
}
.flash-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}
.flash-title-left {
  display: flex;
  align-items: center;
  gap: 8px;
}
.flash-label {
  font-size: 16px;
  font-weight: 700;
  color: #FF2E93;
}
.flash-tag {
  font-size: 10px;
  background: #FF2E93;
  color: #fff;
  padding: 1px 6px;
  border-radius: 4px;
}
.flash-countdown {
  display: flex;
  align-items: center;
  gap: 4px;
}
.countdown-label {
  font-size: 11px;
  color: #999;
}
.time-block {
  display: inline-block;
  background: #222;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  padding: 2px 4px;
  border-radius: 3px;
  min-width: 20px;
  text-align: center;
}
.time-colon {
  color: #222;
  font-weight: 700;
  font-size: 12px;
}
.flash-more {
  margin-left: auto;
  font-size: 12px;
  color: #999;
}
.flash-list {
  display: flex;
  gap: 12px;
  overflow-x: auto;
  padding-bottom: 4px;
}
.flash-list::-webkit-scrollbar {
  display: none;
}
.flash-item {
  flex-shrink: 0;
  width: 80px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}
.flash-img {
  width: 64px;
  height: 64px;
  background: #fef0f5;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.flash-price {
  color: #FF2E93;
  font-weight: 700;
}
.flash-price .price-symbol {
  font-size: 10px;
}
.flash-price .price-val {
  font-size: 16px;
}
.flash-old-price {
  font-size: 11px;
  color: #ccc;
  text-decoration: line-through;
}

/* 推荐商品 */
.recommend-section {
  padding: 0 12px;
}
.section-title {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 16px 0 12px;
}
.title-line {
  display: block;
  width: 30px;
  height: 2px;
  background: linear-gradient(90deg, transparent, #FF2E93, transparent);
}
.title-text {
  font-size: 15px;
  font-weight: 700;
  color: #333;
}

.product-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}
.product-card {
  background: #fff;
  border-radius: 10px;
  overflow: hidden;
}
.product-img {
  height: 150px;
  background: #fafafa;
  display: flex;
  align-items: center;
  justify-content: center;
}
.product-info {
  padding: 8px 10px 10px;
}
.product-name {
  font-size: 13px;
  color: #222;
  line-height: 1.4;
  min-height: 36px;
}
.product-price-row {
  display: flex;
  align-items: baseline;
  gap: 6px;
  margin-top: 4px;
}
.product-price {
  color: #FF2E93;
  font-weight: 700;
}
.product-price .price-symbol {
  font-size: 11px;
}
.product-price .price-val {
  font-size: 16px;
}
.product-old-price {
  font-size: 11px;
  color: #ccc;
  text-decoration: line-through;
}
.product-sales {
  font-size: 11px;
  color: #999;
  margin-top: 2px;
}
</style>
