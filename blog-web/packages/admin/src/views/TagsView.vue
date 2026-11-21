<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import type { TagItem } from '@blog/shared'
import { colorFromName } from '@blog/shared'
import { createTag, deleteTag, fetchTags, mergeTags, updateTag } from '@/api/content'
import { isNotified } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import { confirmAction } from '@/utils/confirm'

const auth = useAuthStore()
const list = ref<TagItem[]>([])
const loading = ref(false)
const dialog = ref(false)
const editing = ref<TagItem | null>(null)
const form = ref({ name: '', slug: '', color: '#4f46e5' })

async function load() {
  loading.value = true
  try {
    list.value = await fetchTags()
  } finally {
    loading.value = false
  }
}

function openDialog(row?: TagItem) {
  editing.value = row || null
  form.value = row
    ? { name: row.name, slug: row.slug, color: row.color }
    : { name: '', slug: '', color: colorFromName(String(Date.now())) }
  dialog.value = true
}

async function submit() {
  if (!form.value.name.trim()) {
    message.warning('请填写名称')
    return
  }
  const payload = { ...form.value, slug: form.value.slug || form.value.name.toLowerCase().replace(/\s+/g, '-') }
  if (editing.value) await updateTag(editing.value.id, payload)
  else await createTag(payload)
  message.success('已保存')
  dialog.value = false
  void load()
}

function remove(row: TagItem) {
  confirmAction({
    title: '删除标签',
    content: `确定删除标签「${row.name}」？`,
    okType: 'danger',
    onOk: async () => {
      try {
        await deleteTag(row.id)
        message.success('已删除')
        void load()
      } catch (e: any) {
        // 被文章引用时后端返回 2010，把具体原因提示出来（拦截器已弹的不再重复）
        if (!isNotified(e)) message.error(e?.message || '删除失败')
      }
    }
  })
}

/** 合并标签：sourceId 下的文章全部迁移到 targetId 后删除 sourceId */
function merge() {
  if (list.value.length < 2) return
  const [source, target] = list.value
  confirmAction({
    title: '合并标签',
    content: `将「${source.name}」合并到「${target.name}」并删除前者？`,
    okType: 'danger',
    onOk: async () => {
      await mergeTags(source.id, target.id)
      message.success('已合并')
      void load()
    }
  })
}

onMounted(load)
</script>

<template>
  <div class="panel">
    <div class="toolbar">
      <h2>标签管理</h2>
      <a-button v-if="auth.can('tag:edit')" type="primary" @click="openDialog()"><PlusOutlined /> 新建标签</a-button>
      <a-button v-if="auth.can('tag:edit')" :disabled="list.length < 2" @click="merge">合并标签</a-button>
      <div class="spacer" />
      <span class="text-muted">共 {{ list.length }} 个标签</span>
    </div>

    <a-table :data-source="list" :loading="loading" row-key="id" size="middle" :pagination="false">
      <a-table-column title="名称" :width="180">
        <template #default="{ record }">
          <a-tag :color="record.color">{{ record.name }}</a-tag>
        </template>
      </a-table-column>
      <a-table-column title="别名" data-index="slug" :width="180" />
      <a-table-column title="颜色" data-index="color" :width="130">
        <template #default="{ record }">
          <span class="color-dot" :style="{ background: record.color }" />{{ record.color }}
        </template>
      </a-table-column>
      <a-table-column title="文章数" data-index="articleCount" :width="100" align="right" />
      <a-table-column title="操作" :width="140" fixed="right">
        <template #default="{ record }">
          <a-space>
            <a v-if="auth.can('tag:edit')" @click="openDialog(record)">编辑</a>
            <a v-if="auth.can('tag:delete')" class="danger" @click="remove(record)">删除</a>
          </a-space>
        </template>
      </a-table-column>
    </a-table>

    <a-modal v-model:open="dialog" :title="editing ? '编辑标签' : '新建标签'" :width="420" @ok="submit">
      <a-form layout="vertical" size="small" class="form">
        <a-form-item label="名称">
          <a-input v-model:value="form.name" @change="form.color = form.color || colorFromName(form.name)" />
        </a-form-item>
        <a-form-item label="别名"><a-input v-model:value="form.slug" placeholder="留空自动生成" /></a-form-item>
        <a-form-item label="颜色">
          <div class="color-row">
            <a-color-picker v-model:value="form.color" show-alpha />
            <span class="color-dot" :style="{ background: form.color }" />
          </div>
        </a-form-item>
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
.color-row { display: flex; align-items: center; gap: 8px; }
.color-dot { display: inline-block; width: 12px; height: 12px; border-radius: 50%; vertical-align: middle; }
.text-muted { color: var(--muted); }
.danger { color: #ff4d4f; }
</style>
