import { createRouter, createWebHashHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

/**
 * 路由表。meta.perm 与后端 @Permit 的权限码一一对应；
 * meta.menu 用于左侧菜单显隐（菜单码也是博客自己的权限码）。
 */
const routes: RouteRecordRaw[] = [
  { path: '/login', name: 'login', component: () => import('@/views/LoginView.vue'), meta: { public: true } },
  {
    path: '/',
    component: () => import('@/layout/AdminLayout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'dashboard', component: () => import('@/views/DashboardView.vue'), meta: { title: '仪表盘', menu: 'dashboard', perm: 'stat:view' } },
      { path: 'articles', name: 'articles', component: () => import('@/views/ArticlesView.vue'), meta: { title: '文章管理', menu: 'article', perm: 'article:list' } },
      { path: 'articles/edit/:id?', name: 'article-edit', component: () => import('@/views/ArticleEditView.vue'), meta: { title: '文章编辑', menu: 'article', perm: 'article:list' } },
      { path: 'categories', name: 'categories', component: () => import('@/views/CategoriesView.vue'), meta: { title: '分类管理', menu: 'category', perm: 'category:list' } },
      { path: 'tags', name: 'tags', component: () => import('@/views/TagsView.vue'), meta: { title: '标签管理', menu: 'tag', perm: 'tag:list' } },
      { path: 'comments', name: 'comments', component: () => import('@/views/CommentsView.vue'), meta: { title: '评论管理', menu: 'comment', perm: 'comment:list' } },
      { path: 'media', name: 'media', component: () => import('@/views/MediaView.vue'), meta: { title: '媒体库', menu: 'media', perm: 'media:list' } },
      { path: 'theme', name: 'theme', component: () => import('@/views/ThemeEditorView.vue'), meta: { title: '主题编辑器', menu: 'theme', perm: 'theme:list' } },
      { path: 'settings', name: 'settings', component: () => import('@/views/SettingsView.vue'), meta: { title: '站点设置', menu: 'setting', perm: 'setting:view' } },
      { path: 'logs', name: 'logs', component: () => import('@/views/LogsView.vue'), meta: { title: '操作日志', menu: 'log', perm: 'log:list' } },
      { path: 'users', name: 'users', component: () => import('@/views/UsersView.vue'), meta: { title: '账号管理', menu: 'user', perm: 'user:list' } },
      { path: 'roles', name: 'roles', component: () => import('@/views/RolesView.vue'), meta: { title: '角色权限', menu: 'role', perm: 'role:list' } },
      { path: 'no-access', name: 'no-access', component: () => import('@/views/NoAccessView.vue'), meta: { title: '无访问权限' } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

const pageRoutes = routes[1].children!.filter((r) => r.name !== 'no-access')

function firstAllowedPath(auth: ReturnType<typeof useAuthStore>): string | null {
  const hit = pageRoutes.find((r) => {
    const perm = r.meta?.perm as string | undefined
    return !!perm && auth.can(perm)
  })
  return hit ? '/' + hit.path : null
}

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach(async (to) => {
  if (to.meta.public) return true

  const auth = useAuthStore()
  const loggedIn = await auth.bootstrap()
  if (!loggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  const perm = to.meta.perm as string | undefined
  if (perm && !auth.can(perm)) {
    const fallback = firstAllowedPath(auth)
    if (!fallback || to.path === fallback) {
      return to.path === '/no-access' ? true : { path: '/no-access' }
    }
    return { path: fallback }
  }
  return true
})

export default router
