import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/HomePage.vue' as any)  // 添加类型断言
  },
  {
    path: '/chat/:roomId?',
    name: 'ChatRoom', 
    component: () => import('../views/ChatRoom.vue' as any)  // 添加类型断言
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router