import { createRouter, createWebHistory } from 'vue-router'
import AppLayout from '@/components/layout/AppLayout.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: AppLayout,
      redirect: '/dashboard',
      children: [
        { path: '/dashboard', name: 'Dashboard', component: () => import('@/views/dashboard/DashboardView.vue'), meta: { title: '仪表盘', icon: 'Odometer' } },
        { path: '/products', name: 'Products', component: () => import('@/views/product/ProductListView.vue'), meta: { title: '产品管理', icon: 'Goods' } },
        { path: '/things', name: 'ThingModel', component: () => import('@/views/product/ThingModelView.vue'), meta: { title: '物模型', hidden: true } },
        { path: '/devices', name: 'Devices', component: () => import('@/views/device/DeviceListView.vue'), meta: { title: '设备管理', icon: 'Monitor' } },
        { path: '/device-detail', name: 'DeviceDetail', component: () => import('@/views/device/DeviceDetailView.vue'), meta: { title: '设备详情', hidden: true } },
        { path: '/monitor', name: 'Monitor', component: () => import('@/views/monitor/MonitorView.vue'), meta: { title: '实时监控', icon: 'TrendCharts' } },
        { path: '/alerts', name: 'Alerts', component: () => import('@/views/alert/AlertListView.vue'), meta: { title: '告警列表', icon: 'Bell' } },
        { path: '/alert-rules', name: 'AlertRules', component: () => import('@/views/alert/AlertRuleView.vue'), meta: { title: '告警规则', icon: 'Setting' } },
        { path: '/command-logs', name: 'CommandLogs', component: () => import('@/views/command/CommandLogView.vue'), meta: { title: '指令日志', icon: 'Document' } },
      ],
    },
  ],
})

export default router
