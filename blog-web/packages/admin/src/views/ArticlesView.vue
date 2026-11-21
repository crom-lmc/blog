<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useSettingsStore } from '@/stores/settings'
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { DeleteOutlined, PlusOutlined, ReloadOutlined, SearchOutlined } from '@ant-design/icons-vue'
import type { ArticleItem, CategoryItem, TagItem } from '@blog/shared'
import {
  batchDeleteArticles,
  deleteArticle,
  exportArticle,
  fetchArticles,
  fetchCategories,
  fetchTags,
  publishArticle,
  topArticle
} from '@/api/content'
import { isNotified } from '@/api/http'
import { confirmAction } from '@/utils/confirm'

const router = useRouter()
const settingsStore = useSettingsStore()
const auth = useAuthStore()

const list = ref<ArticleItem[]>([])
const total = ref(0)
const loading = ref(false)
const categories = ref<CategoryItem[]>([])
const tags = ref<TagItem[]>([])
const selected = ref<ArticleItem[]>([])

const query = reactive({
  keyword: '',
  status: '',
  categoryId: undefined as number | undefined,
  tagId: undefined as number | undefined,
  page: 1,
  size: 10
})

const statusOptions = [
  { value: 'published', label: '已发布', type: 'success', color: 'green' },
  { value: 'draft', label: '草稿', type: 'info', color: 'default' },
  { value: 'private', label: '私密', type: 'warning', color: 'orange' }
]

/** 表格多选：选中行数据用于批量删除，受控 keys 由选中项派生 */
const rowSelection = computed(() => ({
  selectedRowKeys: selected.value.map((i) => i.id),
  onChange: (_keys: (string | number)[], rows: ArticleItem[]) => {
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

/**
 * 后端返回 'YYYY-MM-DDTHH:mm:ss'（24 字符），直接塞进表格列会被挤成多行。
 * 列表只展示到分钟，且字符串本身就是本地时间，切片即可，无需 Date 解析。
 */
function formatDateTime(input?: string | null) {
  if (!input) return '—'
  return String(input).replace('T', ' ').slice(0, 16)
}

async function load() {
  loading.value = true
  try {
    const res = await fetchArticles({ ...query, status: query.status || undefined })
    list.value = res.list
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function reset() {
  Object.assign(query, { keyword: '', status: '', categoryId: undefined, tagId: undefined, page: 1 })
  void load()
}

async function onToggle(row: ArticleItem, type: 'publish' | 'top') {
  if (type === 'publish') {
    const publish = row.status !== 'published'
    await publishArticle(row.id, publish)
    message.success(publish ? '已发布' : '已转为草稿')
  } else {
    const top = !row.isTop
    await topArticle(row.id, top)
    message.success(top ? '已置顶' : '已取消置顶')
  }
  void load()
}

/** 导出 Markdown（含 Front Matter） */
async function onExport(row: ArticleItem) {
  const res = await exportArticle(row.id)
  const blob = new Blob([res.markdown], { type: 'text/markdown' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${row.slug || row.id}.md`
  a.click()
  URL.revokeObjectURL(url)
}

/** 已发布的文章不允许删除：需先「转草稿」下线 */
const isPublished = (row: ArticleItem) => row.status === 'published'

function onDelete(row: ArticleItem) {
  if (isPublished(row)) {
    message.warning('已发布的文章不允许删除，请先「转草稿」下线')
    return
  }
  confirmAction({
    title: '删除确认',
    content: `确定删除《${row.title}》吗？`,
    okType: 'danger',
    onOk: async () => {
      try {
        await deleteArticle(row.id)
        message.success('已删除')
        void load()
      } catch (e: any) {
        // 后端 2011 兜底：拦截器已弹的不再重复
        if (!isNotified(e)) message.error(e?.message || '删除失败')
      }
    }
  })
}

function batchDelete() {
  if (!selected.value.length) return
  // 选中项里只要有一篇已发布就整批拒绝（与后端规则一致）
  const published = selected.value.filter(isPublished)
  if (published.length) {
    message.warning(`选中的 ${published.length} 篇已发布，不允许删除，请先「转草稿」下线`)
    return
  }
  confirmAction({
    title: '批量删除',
    content: `确定删除选中的 ${selected.value.length} 篇文章吗？`,
    okType: 'danger',
    onOk: async () => {
      try {
        await batchDeleteArticles(selected.value.map((i) => i.id))
        message.success('批量删除完成')
        void load()
      } catch (e: any) {
        if (!isNotified(e)) message.error(e?.message || '批量删除失败')
      }
    }
  })
}

onMounted(async () => {
  await settingsStore.load()
  query.size = settingsStore.defaultPageSize
  await load()
  categories.value = await fetchCategories()
  tags.value = await fetchTags()
})
</script>

<template>
  <div class="panel">
    <div class="toolbar">
      <h2>文章管理</h2>
      <a-input v-model:value="query.keyword" placeholder="搜索标题/摘要" allow-clear style="width: 200px" @press-enter="load" />
      <a-select
        v-model:value="query.status"
        placeholder="状态"
        allow-clear
        style="width: 120px"
        :options="statusOptions.map((s) => ({ value: s.value, label: s.label }))"
        @change="load"
      />
      <a-tree-select
        v-model:value="query.categoryId"
        :tree-data="categories"
        :field-names="{ title: 'name', value: 'id', key: 'id', children: 'children' }"
        tree-node-filter-prop="name"
        tree-default-expand-all
        placeholder="分类"
        allow-clear
        style="width: 160px"
        @change="load"
      />
      <a-select
        v-model:value="query.tagId"
        placeholder="标签"
        allow-clear
        style="width: 140px"
        :options="tags.map((t) => ({ value: t.id, label: t.name }))"
        @change="load"
      />
      <a-button type="primary" @click="load"><SearchOutlined /> 查询</a-button>
      <a-button @click="reset"><ReloadOutlined /> 重置</a-button>
      <div class="spacer" />
      <a-button v-if="auth.can('article:delete')" danger :disabled="!selected.length" @click="batchDelete">
        <DeleteOutlined /> 批量删除
      </a-button>
      <a-button v-if="auth.can('article:create')" type="primary" @click="router.push('/articles/edit')">
        <PlusOutlined /> 写文章
      </a-button>
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
      <a-table-column title="标题" data-index="title" :width="220" ellipsis>
        <template #default="{ record }">
          <a-tag v-if="record.isTop" color="blue">置顶</a-tag>
          {{ record.title }}
        </template>
      </a-table-column>
      <a-table-column title="分类" data-index="categoryName" :width="110" />
      <a-table-column title="标签" :width="160">
        <template #default="{ record }">
          <a-tag v-for="t in record.tags" :key="t.id" :color="t.color" style="margin-right: 4px">{{ t.name }}</a-tag>
        </template>
      </a-table-column>
      <a-table-column title="状态" :width="90">
        <template #default="{ record }">
          <a-tag :color="statusOptions.find((s) => s.value === record.status)?.color">
            {{ statusOptions.find((s) => s.value === record.status)?.label }}
          </a-tag>
        </template>
      </a-table-column>
      <a-table-column title="阅读" data-index="viewCount" :width="90" align="right" />
      <a-table-column title="发布时间" :width="170">
        <template #default="{ record }">
          <span class="col-nowrap" :class="{ 'text-muted': !record.publishedAt }">{{ formatDateTime(record.publishedAt) }}</span>
        </template>
      </a-table-column>
      <!-- 操作列需同时容纳「取消置顶」这一最宽状态，操作项用 a-space 排列 -->
      <a-table-column title="操作" :width="300" fixed="right">
        <template #default="{ record }">
          <a-space>
            <a v-if="auth.can('article:edit')" @click="router.push(`/articles/edit/${record.id}`)">编辑</a>
            <a v-if="auth.can('article:publish')" @click="onToggle(record, 'publish')">
              {{ record.status === 'published' ? '转草稿' : '发布' }}
            </a>
            <a v-if="auth.can('article:edit')" @click="onToggle(record, 'top')">
              {{ record.isTop ? '取消置顶' : '置顶' }}
            </a>
            <a @click="onExport(record)">导出</a>
            <a
              v-if="auth.can('article:delete')"
              class="danger"
              :class="{ disabled: record.status === 'published' }"
              :title="record.status === 'published' ? '已发布的文章不允许删除，请先「转草稿」下线' : ''"
              @click="onDelete(record)"
            >
              删除
            </a>
          </a-space>
        </template>
      </a-table-column>
    </a-table>
  </div>
</template>

<style scoped>
.panel { background: var(--login-card-bg); border-radius: 10px; padding: 18px; }
.toolbar { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; margin-bottom: 14px; }
.toolbar h2 { margin: 0 8px 0 0; font-size: 17px; }
.spacer { flex: 1; }
.col-nowrap { white-space: nowrap; }
.text-muted { color: var(--muted); }
.danger { color: #ff4d4f; }
.disabled { color: var(--muted); cursor: not-allowed; }
</style>
