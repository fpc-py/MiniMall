import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录', public: true },
  },
  {
    path: '/',
    component: () => import('@/views/Layout.vue'),
    children: [
      // ==================== 平台端 ====================
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '仪表盘', role: 'platform' },
      },
      {
        path: 'category',
        name: 'Category',
        component: () => import('@/views/Category.vue'),
        meta: { title: '类目管理', role: 'platform' },
      },
      {
        path: 'product-spu',
        name: 'ProductSpu',
        component: () => import('@/views/ProductSpu.vue'),
        meta: { title: 'SPU 管理', role: 'platform' },
      },
      {
        path: 'orders',
        name: 'OrderManage',
        component: () => import('@/views/OrderManage.vue'),
        meta: { title: '订单管理', role: 'platform' },
      },
      {
        path: 'merchant-audit',
        name: 'MerchantAudit',
        component: () => import('@/views/MerchantAudit.vue'),
        meta: { title: '商家审核', role: 'platform' },
      },
      {
        path: 'shop-list',
        name: 'ShopList',
        component: () => import('@/views/ShopList.vue'),
        meta: { title: '店铺管理', role: 'platform' },
      },
      {
        path: 'members',
        name: 'MemberList',
        component: () => import('@/views/MemberList.vue'),
        meta: { title: '会员管理', role: 'platform' },
      },
      {
        path: 'coupons',
        name: 'CouponManage',
        component: () => import('@/views/CouponManage.vue'),
        meta: { title: '优惠券', role: 'platform' },
      },
      {
        path: 'refunds',
        name: 'RefundManage',
        component: () => import('@/views/RefundManage.vue'),
        meta: { title: '退款管理', role: 'platform' },
      },

      // ==================== 商家端 ====================
      {
        path: 'shop/dashboard',
        name: 'ShopDashboard',
        component: () => import('@/views/shop/ShopDashboard.vue'),
        meta: { title: '店铺概览', role: 'merchant' },
      },
      {
        path: 'shop/products',
        name: 'ShopProduct',
        component: () => import('@/views/shop/ShopProduct.vue'),
        meta: { title: '我的商品', role: 'merchant' },
      },
      {
        path: 'shop/orders',
        name: 'ShopOrder',
        component: () => import('@/views/shop/ShopOrder.vue'),
        meta: { title: '店铺订单', role: 'merchant' },
      },
      {
        path: 'shop/refunds',
        name: 'ShopRefund',
        component: () => import('@/views/shop/ShopRefund.vue'),
        meta: { title: '退款审核', role: 'merchant' },
      },
      {
        path: 'shop/coupons',
        name: 'ShopCoupon',
        component: () => import('@/views/shop/ShopCoupon.vue'),
        meta: { title: '店铺优惠券', role: 'merchant' },
      },
      {
        path: 'shop/info',
        name: 'ShopInfo',
        component: () => import('@/views/shop/ShopInfo.vue'),
        meta: { title: '店铺信息', role: 'merchant' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

function readRole() {
  try {
    const user = JSON.parse(localStorage.getItem('admin_user') || '{}')
    return user.userType === 'shop' ? 'merchant' : 'platform'
  } catch (e) {
    return 'platform'
  }
}

router.beforeEach((to) => {
  const token = localStorage.getItem('admin_token')
  const role = readRole()
  const defaultPath = role === 'merchant' ? '/shop/dashboard' : '/dashboard'

  if (to.path === '/') return defaultPath

  if (!token) {
    return to.meta?.public ? true : '/login'
  }

  if (to.meta?.public) return defaultPath

  // 角色不匹配：商家访问平台专属页 / 平台访问商家专属页 → 各自默认页
  const requiredRole = to.meta?.role
  if (requiredRole && requiredRole !== 'both' && requiredRole !== role) {
    return defaultPath
  }

  return true
})

export default router
