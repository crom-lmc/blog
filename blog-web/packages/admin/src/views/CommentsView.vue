<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useSettingsStore } from '@/stores/settings'
import { message } from 'ant-design-vue'
import { CheckOutlined, DeleteOutlined, StopOutlined } from '@ant-design/icons-vue'
import type { CommentItem } from '@blog/shared'
import {
  approveComments,
  batchDeleteComments,
  deleteComment,
  fetchComments,
  rejectComments,
  replyComment
} from '@/api/content'
import { useAuthStore } from '@/stores/auth'
import { confirmAction, promptAction } from '@/utils/confirm'

const auth = useAuthStore()
const list = ref<CommentItem[]>([])
const total = ref(0)
const loading = ref(false)
const selected = ref<CommentItem[]>([])
const settingsStore = useSettingsStore()

const query = reactive({ status: 'pending', page: 1, size: 10 })

const tabs = [
  { name: 'pending', label: '待审核' },
  { name: 'approved', label: '已通过' },
  { name: 'spam', label: '垃圾箱' },
  { name: 'deleted', label: '已删除' }
]

/** 表格多选：选中项用于批量审核 / 删除 */
const rowSelection = computed(() => ({
  selectedRowKeys: selected.value.map((i) => i.id),
  onChange: (_keys: (string | number)[], rows: CommentItem[]) => {
    selected.value = rows
  }
}))

/** 分页交给 a-table 托管，切页/切每页条数统一走 @change */
const pagination = computed(() => ({
  current: query.page,
  pageSize: query.size,
  total: total.value,
  showSizeChanger: true,
  pageSizeOptions: ['10', '20', '30', '50', '100'],
  showTotal: (t: number) => `共 ${t} 条`
}))

function onSizeChange(s: number) {
  query.size = s
  query.page = 1
  void load()
}

function onTableChange(pag: { current?: number; pageSize?: number }) {
  if (pag.pageSize && pag.pageSize !== query.size) {
    onSizeChange(pag.pageSize)
    return
  }
  query.page = pag.current ?? 1
  void load()
}

async function load() {
  loading.value = true
  try {
    const res = await fetchComments(query)
    list.value = res.list
    total.value = res.total
  } finally {
    loading.value = false
  }
}

async function approve(row: CommentItem, status: 'approved' | 'spam') {
  if (status === 'approved') await approveComments([row.id])
  else await rejectComments([row.id])
  message.success(status === 'approved' ? '已通过' : '已标记垃圾')
  void load()
}

/** 管理员回复（后端以管理员身份落库并直接通过审核） */
async function reply(row: CommentItem) {
  const content = await promptAction({ title: '回复评论', placeholder: '请输入回复内容' })
  if (!content) return
  await replyComment(row.articleId, row.id, content)
  message.success('回复成功')
  void load()
}

function remove(row: CommentItem) {
  confirmAction({
    title: '删除评论',
    content: '确定删除该评论？',
    okType: 'danger',
    onOk: async () => {
      await deleteComment(row.id)
      message.success('已删除')
      void load()
    }
  })
}

async function batch(type: 'approved' | 'spam' | 'deleted') {
  if (!selected.value.length) return
  const ids = selected.value.map((c) => c.id)
  if (type === 'approved') await approveComments(ids)
  else if (type === 'spam') await rejectComments(ids)
  else await batchDeleteComments(ids)
  message.success('批量处理完成')
  void load()
}

onMounted(async () => {
  await settingsStore.load()
  query.size = settingsStore.defaultPageSize
  void load()
})
</script>

<template>
  <div class="panel">
    <a-tabs v-model:activeKey="query.status" @change="load">
      <a-tab-pane v-for="t in tabs" :key="t.name" :tab="t.label" />
    </a-tabs>

    <div class="toolbar">
      <h2>评论管理</h2>
      <a-button v-if="auth.can('comment:audit')" type="primary" :disabled="!selected.length" @click="batch('approved')">
        <CheckOutlined /> 批量通过
      </a-button>
      <a-button v-if="auth.can('comment:audit')" :disabled="!selected.length" @click="batch('spam')">
        <StopOutlined /> 批量垃圾
      </a-button>
      <a-button v-if="auth.can('comment:delete')" danger :disabled="!selected.length" @click="batch('deleted')">
        <DeleteOutlined /> 批量删除
      </a-button>
      <div class="spacer" />
      <span class="text-muted">共 {{ total }} 条</span>
    </div>

    <a-table
      :data-source="list"
      :loading="loading"
      row-key="id"
      size="middle"
      :row-selection="rowSelection"
      :pagination="pagination"
      @change="onTableChange"
    >
      <a-table-column title="用户" data-index="authorName" :width="140">
        <template #default="{ record }">
          <span>{{ record.authorName }}</span>
          <a-tag v-if="record.isAdmin" color="blue" style="margin-left: 4px">作者</a-tag>
        </template>
      </a-table-column>
      <a-table-column title="内容" data-index="content" ellipsis />
      <a-table-column title="文章" data-index="articleTitle" :width="160" ellipsis />
      <a-table-column title="IP" data-index="ip" :width="140" />
      <a-table-column title="时间" data-index="createdAt" :width="170" />
      <a-table-column title="操作" :width="200" fixed="right">
        <template #default="{ record }">
          <a-space>
            <a v-if="auth.can('comment:audit')" @click="approve(record, 'approved')">通过</a>
            <a v-if="auth.can('comment:reply')" @click="reply(record)">回复</a>
            <a v-if="auth.can('comment:audit')" @click="approve(record, 'spam')">垃圾</a>
            <a v-if="auth.can('comment:delete')" class="danger" @click="remove(record)">删除</a>
          </a-space>
        </template>
      </a-table-column>
    </a-table>
  </div>
</template>

<style scoped>
.panel { background: var(--login-card-bg); border-radius: 10px; padding: 18px; }
.toolbar { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; margin: 14px 0; }
.toolbar h2 { margin: 0 8px 0 0; font-size: 17px; }
.spacer { flex: 1; }
.text-muted { color: var(--muted); }
.danger { color: #ff4d4f; }
</style>
