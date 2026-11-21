<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { UserOutlined, LockOutlined } from '@ant-design/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useThemeStore } from '@/stores/theme'
import ThemeToggleIcon from '@/components/ThemeToggleIcon.vue'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const theme = useThemeStore()

const loading = ref(false)
const form = reactive({ username: '', password: '' })

async function submit() {
  if (!form.username.trim() || !form.password) {
    message.warning('请输入账号和密码')
    return
  }
  loading.value = true
  try {
    await auth.login(form.username.trim(), form.password)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
    router.replace(redirect.startsWith('/') ? redirect : '/dashboard')
    message.success('登录成功')
  } catch (e) {
    message.error(e instanceof Error ? e.message : '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-shell">
    <a-button type="text" class="theme-toggle" @click="theme.toggle()" :title="theme.mode === 'dark' ? '切换到白天模式' : '切换到黑夜模式'">
      <ThemeToggleIcon :mode="theme.mode" />
    </a-button>
    <div class="login-card">
      <div class="brand">
        <span class="brand-mark">B</span>
        <div><h1>博客管理后台</h1><p>独立账号登录</p></div>
      </div>
      <a-form :model="form" layout="vertical" @finish="submit">
        <a-form-item label="账号" name="username" :rules="[{ required: true, message: '请输入账号' }]">
          <a-input v-model:value="form.username" size="large" placeholder="请输入账号" autocomplete="username">
            <template #prefix><UserOutlined /></template>
          </a-input>
        </a-form-item>
        <a-form-item label="密码" name="password" :rules="[{ required: true, message: '请输入密码' }]">
          <a-input-password v-model:value="form.password" size="large" placeholder="请输入密码" autocomplete="current-password">
            <template #prefix><LockOutlined /></template>
          </a-input-password>
        </a-form-item>
        <a-button type="primary" html-type="submit" size="large" block :loading="loading">登录</a-button>
      </a-form>
    </div>
  </div>
</template>

<style scoped>
.login-shell {
  min-height: 100vh; display: grid; place-items: center; padding: 24px;
  background: var(--login-shell-bg);
}
.theme-toggle { position: fixed; top: 18px; right: 18px; }
.login-card {
  width: min(400px, 100%); padding: 32px; border-radius: 12px;
  background: var(--login-card-bg); box-shadow: var(--login-card-shadow);
}
.brand { display: flex; align-items: center; gap: 12px; margin-bottom: 26px; }
.brand-mark {
  width: 42px; height: 42px; border-radius: 10px; display: grid; place-items: center;
  background: #5a67e8; color: #fff; font-size: 20px; font-weight: 700;
}
.brand h1 { margin: 0; font-size: 20px; color: var(--text-base); }
.brand p { margin: 4px 0 0; font-size: 13px; color: var(--muted); }
</style>
