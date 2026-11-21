<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import type { CategoryItem } from '@blog/shared'
import { createCategory, deleteCategory, fetchCategories, updateCategory } from '@/api/content'
import { isNotified } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import { confirmAction } from '@/utils/confirm'

const auth = useAuthStore()
const list = ref<CategoryItem[]>([])
const loading = ref(false)
const dialog = ref(false)
const editing = ref<CategoryItem | null>(null)
const form = ref({ name: '', slug: '', description: '', parentId: 0, sort: 99 })

async function load() {
  loading.value = true
  try {
    list.value = await fetchCategories()
  } finally {
    loading.value = false
  }
}

function openDialog(row?: CategoryItem) {
  editing.value = row || null
  form.value = row
    ? { name: row.name, slug: row.slug, description: row.description, parentId: row.parentId, sort: row.sort }
    : { name: '', slug: '', description: '', parentId: 0, sort: 99 }
  dialog.value = true
}

async function submit() {
  if (!form.value.name.trim()) {
    message.warning('请填写名称')
    return
  }
  const payload = {
    ...form.value,
    slug: form.value.slug || form.value.name.toLowerCase().replace(/\s+/g, '-')
  }
  if (editing.value) await updateCategory(editing.value.id, payload)
  else await createCategory(payload)
  message.success('已保存')
  dialog.value = false
  void load()
}

function remove(row: CategoryItem) {
  confirmAction({
    title: '删除分类',
    content: `确定删除分类「${row.name}」？`,
    okType: 'danger',
    onOk: async () => {
      try {
        await deleteCategory(row.id)
        message.success('已删除')
        void load()
      } catch (e: any) {
        // 被文章引用时后端返回 2009，把具体原因提示出来（拦截器已弹的不再重复）
        if (!isNotified(e)) message.error(e?.message || '删除失败')
      }
    }
  })
}

const parentName = (id: number) => list.value.find((c) => c.id === id)?.name || '—'

onMounted(load)
</script>

<template>
  <div class="panel">
    <div class="toolbar">
      <h2>分类管理</h2>
      <a-button v-if="auth.can('category:edit')" type="primary" @click="openDialog()"><PlusOutlined /> 新建分类</a-button>
      <div class="spacer" />
      <span class="text-muted">共 {{ list.length }} 个分类</span>
    </div>

    <a-table
      :data-source="list"
      :loading="loading"
      row-key="id"
      size="middle"
      children-column-name="children"
      :default-expand-all-rows="true"
      :pagination="false"
    >
      <a-table-column title="名称" data-index="name" :width="180" />
      <a-table-column title="别名" data-index="slug" :width="160" />
      <a-table-column title="父级" :width="120">
        <template #default="{ record }">{{ record.parentId ? parentName(record.parentId) : '顶级' }}</template>
      </a-table-column>
      <a-table-column title="描述" data-index="description" ellipsis />
      <a-table-column title="文章数" data-index="articleCount" :width="90" align="right" />
      <a-table-column title="排序" data-index="sort" :width="90" align="right" />
      <a-table-column title="操作" :width="140" fixed="right">
        <template #default="{ record }">
          <a-space>
            <a v-if="auth.can('category:edit')" @click="openDialog(record)">编辑</a>
            <a v-if="auth.can('category:delete')" class="danger" @click="remove(record)">删除</a>
          </a-space>
        </template>
      </a-table-column>
    </a-table>

    <a-modal v-model:open="dialog" :title="editing ? '编辑分类' : '新建分类'" :width="440" @ok="submit">
      <a-form layout="vertical" size="small" class="form">
        <a-form-item label="名称"><a-input v-model:value="form.name" /></a-form-item>
        <a-form-item label="别名"><a-input v-model:value="form.slug" placeholder="留空自动生成" /></a-form-item>
        <a-form-item label="父级分类">
          <a-select
            v-model:value="form.parentId"
            style="width: 100%"
            :options="[
              { value: 0, label: '顶级分类' },
              ...list.filter((x) => !x.parentId).map((c) => ({ value: c.id, label: c.name }))
            ]"
          />
        </a-form-item>
        <a-form-item label="描述"><a-textarea v-model:value="form.description" :rows="2" /></a-form-item>
        <a-form-item label="排序"><a-input-number v-model:value="form.sort" :min="0" style="width: 100%" /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<style scoped>
.panel { background: var(--login-card-bg); border-radius: 10px; padding: 18px; }
.toolbar { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; margin-bottom: 14px; }
.toolbar h2 { margin: 0 8px 0 0; font-size: 17px; }
.spacer { flex: 1; }
.form { margin-top: 18px; }
.text-muted { color: var(--muted); }
.danger { color: #ff4d4f; }
</style>
