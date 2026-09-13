import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
  },
  {
    path: '/',
    component: () => import('@/views/Layout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
      },
      {
        path: 'category',
        name: 'Category',
        component: () => import('@/views/Category.vue'),
      },
      {
        path: 'product-spu',
        name: 'ProductSpu',
        component: () => import('@/views/ProductSpu.vue'),
      },
      {
        path: 'orders',
        name: 'OrderManage',
        component: () => import('@/views/OrderManage.vue'),
      },
      {
        path: 'merchant-audit',
        name: 'MerchantAudit',
        component: () => import('@/views/MerchantAudit.vue'),
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
