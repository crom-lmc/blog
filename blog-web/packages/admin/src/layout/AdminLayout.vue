<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Modal } from 'ant-design-vue'
import {
  BgColorsOutlined,
  DashboardOutlined,
  DownOutlined,
  FileSearchOutlined,
  FileTextOutlined,
  FolderOutlined,
  KeyOutlined,
  LogoutOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  MessageOutlined,
  PictureOutlined,
  SafetyCertificateOutlined,
  SettingOutlined,
  TagsOutlined,
  UserOutlined
} from '@ant-design/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useThemeStore } from '@/stores/theme'
import ThemeToggleIcon from '@/components/ThemeToggleIcon.vue'
import ChangePasswordModal from '@/components/ChangePasswordModal.vue'
import { getWebOrigin } from '@/utils/origin'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const theme = useThemeStore()

const collapsed = ref(false)
const appName = ref('博客管理后台')
const changePwdOpen = ref(false)

/** 菜单码与页面权限码都在博客自己库里维护；有其一即显示，最终拦截在后端 @Permit。 */
const allMenus = [
  { path: '/dashboard', title: '仪表盘', icon: DashboardOutlined, menu: 'dashboard', perm: 'stat:view' },
  { path: '/articles', title: '文章管理', icon: FileTextOutlined, menu: 'article', perm: 'article:list' },
  { path: '/categories', title: '分类管理', icon: FolderOutlined, menu: 'category', perm: 'category:list' },
  { path: '/tags', title: '标签管理', icon: TagsOutlined, menu: 'tag', perm: 'tag:list' },
  { path: '/comments', title: '评论管理', icon: MessageOutlined, menu: 'comment', perm: 'comment:list' },
  { path: '/media', title: '媒体库', icon: PictureOutlined, menu: 'media', perm: 'media:list' },
  { path: '/theme', title: '主题编辑器', icon: BgColorsOutlined, menu: 'theme', perm: 'theme:list' },
  { path: '/settings', title: '站点设置', icon: SettingOutlined, menu: 'setting', perm: 'setting:view' },
  { path: '/logs', title: '操作日志', icon: FileSearchOutlined, menu: 'log', perm: 'log:list' },
  { path: '/users', title: '账号管理', icon: UserOutlined, menu: 'user', perm: 'user:list' },
  { path: '/roles', title: '角色权限', icon: SafetyCertificateOutlined, menu: 'role', perm: 'role:list' }
]

const menus = computed(() => allMenus.filter((m) => auth.can(m.menu) || auth.can(m.perm)))
const selectedKeys = computed(() => [route.path.startsWith('/articles') ? '/articles' : route.path])
const breadcrumb = computed(() => route.matched.filter((r) => r.meta?.title).map((r) => String(r.meta.title)))
const webOrigin = getWebOrigin()

function handleMenuClick({ key }: { key: string }) {
  router.push(key)
}

function onUserMenuClick(event: { key: string }) {
  if (event.key === 'logout') logout()
  else if (event.key === 'changePassword') changePwdOpen.value = true
}

function logout() {
  Modal.confirm({
    title: '退出登录',
    content: '退出后需要重新登录才能继续管理博客，确定吗？',
    okText: '确定退出',
    cancelText: '取消',
    okButtonProps: { danger: true },
    centered: true,
    onOk: async () => {
      await auth.logout()
      router.push('/login')
    }
  })
}
</script>

<template>
  <a-layout class="shell">
    <a-layout-header class="header">
      <div class="logo" :class="{ 'is-collapsed': collapsed }">
        <component
          :is="collapsed ? MenuUnfoldOutlined : MenuFoldOutlined"
          class="menu-toggle"
          :title="collapsed ? '展开菜单' : '收起菜单'"
          @click="collapsed = !collapsed"
        />
        <span class="logo-text">{{ appName }}</span>
      </div>
      <div class="right">
        <a-button type="text" :href="webOrigin" target="_blank">查看前台</a-button>
        <a-button type="text" class="theme-toggle" @click="theme.toggle()" :title="theme.mode === 'dark' ? '切换到白天模式' : '切换到黑夜模式'">
          <ThemeToggleIcon :mode="theme.mode" />
        </a-button>
        <a-dropdown :trigger="['hover']">
          <span class="who">
            {{ auth.nickname }}
            <a-tag v-if="auth.isSuperAdmin" color="red" class="role-tag">超管</a-tag>
            <DownOutlined class="caret" />
          </span>
          <template #overlay>
            <a-menu @click="onUserMenuClick">
              <a-menu-item key="changePassword"><KeyOutlined class="mi" />修改密码</a-menu-item>
              <a-menu-divider />
              <a-menu-item key="logout"><LogoutOutlined class="mi" />退出登录</a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
      </div>
    </a-layout-header>

    <a-layout class="body">
      <a-layout-sider
        class="sider"
        :theme="theme.mode === 'dark' ? 'dark' : 'light'"
        width="208"
        :collapsed="collapsed"
        :trigger="null"
      >
        <a-menu mode="inline" :inline-collapsed="collapsed" :selected-keys="selectedKeys" @click="handleMenuClick">
          <a-menu-item v-for="m in menus" :key="m.path">
            <template #icon><component :is="m.icon" /></template>
            {{ m.title }}
          </a-menu-item>
        </a-menu>
      </a-layout-sider>

      <a-layout-content class="content">
        <div class="breadcrumb-bar">
          <span v-for="(b, i) in breadcrumb" :key="b">
            <span v-if="i > 0" class="sep">/</span>{{ b }}
          </span>
        </div>
        <RouterView />
      </a-layout-content>
    </a-layout>
  </a-layout>
  <ChangePasswordModal v-model:open="changePwdOpen" />
</template>

<style scoped>
.shell { min-height: 100vh; background: var(--bg-page); }
.header {
  position: sticky; top: 0; z-index: 20; height: 56px; line-height: normal;
  display: flex; align-items: center; justify-content: space-between;
  padding: 0; background: var(--header-bg); border-bottom: 1px solid var(--header-border);
}
/* 折叠图标与侧栏菜单图标共用 24px 左内边距；折叠态落到 80px 图标列中线（40px） */
.logo {
  display: flex; align-items: center; gap: 6px; padding-left: 24px;
  flex: 0 0 auto; white-space: nowrap; transition: padding-left 0.2s;
}
.logo.is-collapsed { padding-left: 29px; }
.menu-toggle {
  display: inline-flex; align-items: center; justify-content: center;
  width: 22px; height: 22px; font-size: 16px; border-radius: 5px;
  cursor: pointer; color: var(--header-text);
}
.logo-text { font-weight: 700; font-size: 15px; color: #5a67e8; white-space: nowrap; }
/* 菜单项图标与 header 折叠图标统一 24px 起点 */
.sider :deep(.ant-menu-inline.ant-menu-root:not(.ant-menu-inline-collapsed) > .ant-menu-item) {
  padding-inline-start: 24px;
}
.right { display: flex; align-items: center; gap: 8px; padding-right: 16px; }
.theme-toggle { display: inline-flex; align-items: center; }
.who { display: inline-flex; align-items: center; gap: 6px; cursor: pointer; color: var(--header-text); }
.caret { font-size: 10px; }
.mi { margin-right: 6px; }
.body { background: var(--bg-page); }
.sider { background: var(--sider-bg) !important; border-right: 1px solid var(--header-border); }
.sider :deep(.ant-layout-sider-children) { position: sticky; top: 56px; }
.content { min-width: 0; padding: 0 18px 18px; }
.breadcrumb-bar { height: 42px; display: flex; align-items: center; gap: 6px; color: var(--muted); font-size: 13px; }
.sep { margin-right: 6px; color: #c8cdd6; }
.role-tag { margin-left: 2px; }
</style>
