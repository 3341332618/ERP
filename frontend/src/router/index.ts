import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const routes: RouteRecordRaw[] = [
  { path: '/login', name: '登录', component: () => import('../views/LoginView.vue') },
  {
    path: '/',
    component: () => import('../layouts/AppLayout.vue'),
    children: [
      { path: '', redirect: '/home' },
      { path: 'home', name: '首页看板', component: () => import('../views/HomeView.vue') },
      { path: 'profile', name: '个人中心', component: () => import('../views/ProfileView.vue') },
      { path: 'master/:type', name: '基础资料', component: () => import('../views/MasterDataView.vue') },
      { path: 'purchase/:type', name: '采购单据', component: () => import('../views/DocumentView.vue') },
      { path: 'sales/:type', name: '销售单据', component: () => import('../views/DocumentView.vue') },
      { path: 'inventory/stock', name: '库存分布', component: () => import('../views/StockView.vue') },
      { path: 'inventory/transfer', name: '库存调拨', component: () => import('../views/DocumentView.vue') },
      { path: 'inventory/:type', name: '库存业务', component: () => import('../views/AuditView.vue') },
      { path: 'settlement/:type', name: '结算管理', component: () => import('../views/SettlementView.vue') }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (to.path === '/login') return true
  if (!auth.token) return '/login'
  if (!auth.user) {
    try {
      await auth.loadCurrentUser()
    } catch {
      auth.logout()
      return '/login'
    }
  }
  return true
})

export default router
