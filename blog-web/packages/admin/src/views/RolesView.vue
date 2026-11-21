<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import {
  listRoles, listPermissions, saveRole, deleteRole,
  type SysRole, type SysPermission
} from '@/api/admin'
import { useAuthStore } from '@/stores/auth'
import { confirmAction } from '@/utils/confirm'

const auth = useAuthStore()
const loading = ref(false)
const records = ref<SysRole[]>([])
const permissions = ref<SysPermission[]>([])
const open = ref(false)
const saving = ref(false)
const form = reactive({ id: undefined as number | undefined, code: '', name: '', description: '', sortNo: 0, permissions: [] as string[] })

const menuPerms = computed(() => permissions.value.filter(p => p.type === 'MENU'))
// 「数据权限」即操作级权限（article:create 等），按权限码前缀归类展示，便于授权时定位。
const dataPerms = computed(() => permissions.value.filter(p => p.type === 'BUTTON'))
const dataGroups = computed(() => {
  const labels: Record<string, string> = {
    article: '文章', category: '分类', tag: '标签', comment: '评论', media: '媒体库',
    theme: '主题', setting: '站点设置', log: '操作日志', stat: '统计',
    user: '账号', role: '角色'
  }
  const order = ['article', 'category', 'tag', 'comment', 'media', 'theme', 'setting', 'log', 'stat', 'user', 'role']
  const groups = new Map<string, SysPermission[]>()
  for (const p of dataPerms.value) {
    const key = p.code.split(':')[0]
    if (!groups.has(key)) groups.set(key, [])
    groups.get(key)!.push(p)
  }
  return order.filter(k => groups.has(k)).map(k => ({ key: k, label: labels[k] || k, items: groups.get(k)! }))
})

async function load() {
  loading.value = true
  try {
    const [roles, perms] = await Promise.all([listRoles(), listPermissions()])
    records.value = roles
    permissions.value = perms
  } finally {
    loading.value = false
  }
}

function add() {
  form.id = undefined
  Object.assign(form, { code: '', name: '', description: '', sortNo: 0, permissions: [] })
  open.value = true
}

function edit(row: SysRole) {
  Object.assign(form, {
    id: row.id, code: row.code, name: row.name, description: row.description || '',
    sortNo: row.sortNo, permissions: [...row.permissions]
  })
  open.value = true
}

async function submit() {
  if (!form.code.trim() || !form.name.trim()) { message.warning('角色编码与名称必填'); return }
  saving.value = true
  try {
    await saveRole({ ...form, code: form.code.trim().toUpperCase() })
    message.success('保存成功')
    open.value = false
    await load()
  } finally {
    saving.value = false
  }
}

function remove(row: SysRole) {
  confirmAction({
    title: '删除角色',
    content: `确定删除角色「${row.name}」吗？关联账号会同时失去该角色。`,
    okType: 'danger',
    onOk: async () => { await deleteRole(row.id); message.success('已删除'); await load() }
  })
}

function toggle(code: string, checked: boolean) {
  const set = new Set(form.permissions)
  if (checked) set.add(code); else set.delete(code)
  form.permissions = [...set]
}

/** 批量勾选/取消一组权限码。 */
function toggleGroup(codes: string[], checked: boolean) {
  const set = new Set(form.permissions)
  for (const code of codes) {
    if (checked) set.add(code); else set.delete(code)
  }
  form.permissions = [...set]
}

const allMenuCodes = computed(() => menuPerms.value.map(p => p.code))
const allDataCodes = computed(() => dataPerms.value.map(p => p.code))

onMounted(load)
</script>

<template>
  <div class="panel">
    <div class="toolbar">
      <h2>角色权限</h2>
      <a-button v-if="auth.can('role:create')" type="primary" @click="add">新建角色</a-button>
    </div>
    <a-table :data-source="records" :loading="loading" row-key="id" size="middle"
             :pagination="{ pageSize: 10, showTotal: (t: number) => `共 ${t} 条` }">
      <a-table-column title="编码" data-index="code" :width="140" />
      <a-table-column title="名称" data-index="name" />
      <a-table-column title="说明" data-index="description" />
      <a-table-column title="权限数">
        <template #default="{ record }">{{ record.permissions.length }}</template>
      </a-table-column>
      <a-table-column title="操作" :width="140">
        <template #default="{ record }">
          <a-space>
            <a v-if="auth.can('role:edit')" @click="edit(record)">编辑权限</a>
            <a v-if="auth.can('role:delete') && record.code !== 'ADMIN'" class="danger" @click="remove(record)">删除</a>
          </a-space>
        </template>
      </a-table-column>
    </a-table>

    <a-modal v-model:open="open" title="角色授权" :width="720" :confirm-loading="saving" @ok="submit">
      <a-form layout="vertical" class="form">
        <a-form-item label="角色编码"><a-input v-model:value="form.code" :disabled="!!form.id" /></a-form-item>
        <a-form-item label="角色名称"><a-input v-model:value="form.name" /></a-form-item>
        <a-form-item label="说明"><a-input v-model:value="form.description" /></a-form-item>
      </a-form>
      <div class="perm-group">
        <div class="perm-head">
          <b>菜单权限</b>
          <a-space :size="10">
            <a @click="toggleGroup(allMenuCodes, true)">全选</a>
            <a @click="toggleGroup(allMenuCodes, false)">清空</a>
          </a-space>
        </div>
        <div class="perm-grid">
          <label v-for="p in menuPerms" :key="p.code" class="perm">
            <input type="checkbox" :checked="form.permissions.includes(p.code)" @change="(e: any) => toggle(p.code, e.target.checked)" />
            <span>{{ p.name }}</span><small>{{ p.code }}</small>
          </label>
        </div>
      </div>
      <div class="perm-group">
        <div class="perm-head">
          <b>数据权限</b>
          <a-space :size="10">
            <a @click="toggleGroup(allDataCodes, true)">全选</a>
            <a @click="toggleGroup(allDataCodes, false)">清空</a>
          </a-space>
        </div>
        <div v-for="g in dataGroups" :key="g.key" class="perm-subgroup">
          <div class="perm-subhead">
            <span>{{ g.label }}</span>
            <a @click="toggleGroup(g.items.map(i => i.code), !g.items.every(i => form.permissions.includes(i.code)))">
              {{ g.items.every(i => form.permissions.includes(i.code)) ? '取消' : '全选' }}
            </a>
          </div>
          <div class="perm-grid">
            <label v-for="p in g.items" :key="p.code" class="perm">
              <input type="checkbox" :checked="form.permissions.includes(p.code)" @change="(e: any) => toggle(p.code, e.target.checked)" />
              <span>{{ p.name }}</span><small>{{ p.code }}</small>
            </label>
          </div>
        </div>
      </div>
      <p v-if="form.code === 'ADMIN'" class="perm-tip">提示：ADMIN 为内置超级管理员，运行时自动拥有全部权限，此处勾选仅作展示。</p>
    </a-modal>
  </div>
</template>

<style scoped>
.panel { background: var(--login-card-bg); border-radius: 10px; padding: 18px; }
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; }
.toolbar h2 { margin: 0; font-size: 17px; }
.form { margin-top: 8px; }
.perm-group { margin-top: 14px; padding-top: 12px; border-top: 1px solid var(--border-subtle); }
.perm-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.perm-head b { font-size: 14px; }
.perm-subgroup { margin-top: 10px; }
.perm-subhead {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 6px; padding: 4px 8px; border-radius: 5px;
  background: var(--bg-page); font-size: 12px; font-weight: 600; color: var(--muted);
}
.perm-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 4px 10px; }
.perm { display: inline-flex; align-items: center; gap: 5px; font-size: 13px; cursor: pointer; white-space: nowrap; }
.perm span { overflow: hidden; text-overflow: ellipsis; }
.perm small { color: var(--muted); font-size: 11px; }
.perm-tip { margin: 12px 0 0; color: var(--muted); font-size: 12px; }
.danger { color: #ff4d4f; }
</style>
