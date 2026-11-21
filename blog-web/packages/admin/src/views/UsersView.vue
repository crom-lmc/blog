<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import {
  listUsers, saveUser, deleteUser, resetUserPassword, listRoles,
  type SysUser, type SysRole, type UserSavePayload
} from '@/api/admin'
import { useAuthStore } from '@/stores/auth'
import { confirmAction, promptAction } from '@/utils/confirm'

const auth = useAuthStore()
const loading = ref(false)
const records = ref<SysUser[]>([])
const roles = ref<SysRole[]>([])
const open = ref(false)
const editingId = ref<number | null>(null)
const saving = ref(false)
const form = reactive<UserSavePayload>({
  username: '', nickname: '', email: '', password: '', status: 1, roleIds: []
})

async function load() {
  loading.value = true
  try {
    const [users, roleList] = await Promise.all([listUsers(), listRoles()])
    records.value = users
    roles.value = roleList
  } finally {
    loading.value = false
  }
}

function add() {
  editingId.value = null
  Object.assign(form, { username: '', nickname: '', email: '', password: '', status: 1, roleIds: [] })
  open.value = true
}

function edit(row: SysUser) {
  editingId.value = row.id
  Object.assign(form, {
    id: row.id, username: row.username, nickname: row.nickname, email: row.email,
    password: '', status: row.status, roleIds: roles.value
      .filter(r => row.roleCodes.includes(r.code)).map(r => r.id)
  })
  open.value = true
}

async function submit() {
  if (!form.username.trim()) { message.warning('请输入账号'); return }
  if (!editingId.value && !form.password) { message.warning('新建账号必须设置密码'); return }
  saving.value = true
  try {
    await saveUser({ ...form, username: form.username.trim() })
    message.success('保存成功')
    open.value = false
    await load()
  } finally {
    saving.value = false
  }
}

function remove(row: SysUser) {
  confirmAction({
    title: '删除账号',
    content: `确定删除账号「${row.username}」吗？`,
    okType: 'danger',
    onOk: async () => { await deleteUser(row.id); message.success('已删除'); await load() }
  })
}

async function resetPwd(row: SysUser) {
  const pwd = await promptAction({ title: `为「${row.username}」重置密码`, placeholder: '至少 6 位' })
  if (!pwd) return
  if (pwd.length < 6) { message.warning('密码至少 6 位'); return }
  await resetUserPassword(row.id, pwd)
  message.success('密码已重置')
}

onMounted(load)
</script>

<template>
  <div class="panel">
    <div class="toolbar">
      <h2>账号管理</h2>
      <a-button v-if="auth.can('user:create')" type="primary" @click="add">新建账号</a-button>
    </div>
    <a-table :data-source="records" :loading="loading" row-key="id" size="middle"
             :pagination="{ pageSize: 10, showTotal: (t: number) => `共 ${t} 条` }">
      <a-table-column title="ID" data-index="id" :width="70" />
      <a-table-column title="账号" data-index="username" />
      <a-table-column title="昵称" data-index="nickname" />
      <a-table-column title="邮箱" data-index="email" />
      <a-table-column title="角色">
        <template #default="{ record }">
          <a-tag v-for="code in record.roleCodes" :key="code" color="blue">{{ code }}</a-tag>
        </template>
      </a-table-column>
      <a-table-column title="状态">
        <template #default="{ record }">
          <a-tag :color="record.status === 1 ? 'green' : 'default'">{{ record.status === 1 ? '启用' : '停用' }}</a-tag>
        </template>
      </a-table-column>
      <a-table-column title="最近登录" data-index="lastLoginAt" />
      <a-table-column title="操作" :width="220" fixed="right">
        <template #default="{ record }">
          <a-space>
            <a v-if="auth.can('user:edit')" @click="edit(record)">编辑</a>
            <a v-if="auth.can('user:resetPassword')" @click="resetPwd(record)">重置密码</a>
            <a v-if="auth.can('user:delete')" class="danger" @click="remove(record)">删除</a>
          </a-space>
        </template>
      </a-table-column>
    </a-table>

    <a-modal v-model:open="open" :title="editingId ? '编辑账号' : '新建账号'" :confirm-loading="saving" @ok="submit">
      <a-form layout="vertical" class="form">
        <a-form-item label="账号"><a-input v-model:value="form.username" :disabled="!!editingId" /></a-form-item>
        <a-form-item label="昵称"><a-input v-model:value="form.nickname" /></a-form-item>
        <a-form-item label="邮箱"><a-input v-model:value="form.email" /></a-form-item>
        <a-form-item :label="editingId ? '新密码（留空不改）' : '密码'">
          <a-input-password v-model:value="form.password" />
        </a-form-item>
        <a-form-item label="角色">
          <a-select v-model:value="form.roleIds" mode="multiple" placeholder="可多选"
                    :options="roles.map(r => ({ value: r.id, label: r.name }))" />
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="form.status" :options="[{ value: 1, label: '启用' }, { value: 0, label: '停用' }]" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<style scoped>
.panel { background: var(--login-card-bg); border-radius: 10px; padding: 18px; }
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; }
.toolbar h2 { margin: 0; font-size: 17px; }
.form { margin-top: 18px; }
.danger { color: #ff4d4f; }
</style>
