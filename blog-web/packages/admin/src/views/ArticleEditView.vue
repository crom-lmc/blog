<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { MdEditor } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import type { ArticleItem, CategoryItem, TagItem } from '@blog/shared'
import { createArticle, fetchArticle, fetchCategories, fetchTags, updateArticle } from '@/api/content'
import { isNotified } from '@/api/http'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const id = computed(() => (route.params.id ? Number(route.params.id) : 0))
const categories = ref<CategoryItem[]>([])
const tags = ref<TagItem[]>([])

/**
 * 归一化分类为树：真实后端 /admin/categories 返回树（含 children），
 * mock 返回平铺列表（parentId 关联）。统一重建为树，供 a-tree-select 使用。
 */
function normalizeCategoryTree(input: CategoryItem[]): CategoryItem[] {
  const flat: CategoryItem[] = []
  const walk = (items: CategoryItem[]) => {
    for (const it of items) {
      flat.push({ ...it, parentId: it.parentId ?? 0, children: undefined })
      if (it.children?.length) walk(it.children)
    }
  }
  walk(input || [])
  const map = new Map<number, CategoryItem>()
  flat.forEach((it) => map.set(it.id, { ...it, children: [] }))
  const roots: CategoryItem[] = []
  for (const it of map.values()) {
    const parent = it.parentId ? map.get(it.parentId) : undefined
    if (parent) parent.children!.push(it)
    else roots.push(it)
  }
  return roots
}
const saving = ref(false)
const loading = ref(false)
const dirty = ref(false)
const lastSaved = ref('')

const form = reactive({
  title: '',
  slug: '',
  summary: '',
  cover: '',
  contentMd: '',
  status: 'draft' as ArticleItem['status'],
  type: 'article' as ArticleItem['type'],
  categoryId: undefined as number | undefined,
  tagIds: [] as number[],
  isTop: false,
  allowComment: true
})

/** 字数与阅读时长统计 */
const wordCount = computed(() => form.contentMd.replace(/\s/g, '').length)
const readingTime = computed(() => Math.max(1, Math.round(wordCount.value / 350)))

async function load() {
  if (!id.value) return
  loading.value = true
  try {
    const a = await fetchArticle(id.value)
    Object.assign(form, {
      title: a.title,
      slug: a.slug,
      summary: a.summary,
      cover: a.cover,
      contentMd: a.contentMd || '',
      status: a.status,
      type: a.type,
      categoryId: a.categoryId,
      tagIds: (a.tags || []).map((t) => t.id),
      isTop: a.isTop,
      allowComment: a.allowComment
    })
  } finally {
    loading.value = false
  }
}

async function save(status?: ArticleItem['status']) {
  if (!form.title.trim()) {
    message.warning('请先填写标题')
    return
  }
  if (status) form.status = status
  saving.value = true
  try {
    // 后端 ArticleSaveRequest：isTop / allowComment 为 Integer 0/1
    const payload = {
      title: form.title,
      slug: form.slug || form.title.replace(/\s+/g, '-').toLowerCase(),
      summary: form.summary,
      cover: form.cover,
      contentMd: form.contentMd,
      status: form.status,
      type: form.type,
      categoryId: form.categoryId,
      tagIds: form.tagIds,
      isTop: form.isTop ? 1 : 0,
      allowComment: form.allowComment ? 1 : 0
    }
    if (id.value) {
      await updateArticle(id.value, payload)
    } else {
      const created = await createArticle(payload)
      // 新建：发布时直接去列表页，否则留在编辑器（地址补上 id）
      if (status !== 'published') router.replace(`/articles/edit/${created.id}`)
    }
    dirty.value = false
    lastSaved.value = new Date().toLocaleTimeString()
    message.success(status === 'published' ? '已发布' : '已保存')
    // 发布后稍作停顿（让用户看到提示）再回到文章列表；保存草稿停留在当前页
    if (status === 'published') {
      await new Promise((resolve) => setTimeout(resolve, 600))
      router.push('/articles')
    }
  } catch (e: any) {
    if (!isNotified(e)) message.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

/** 自动保存草稿：每 30 秒 */
let timer: ReturnType<typeof setInterval> | undefined
function startAutoSave() {
  timer = setInterval(() => {
    if (dirty.value && form.title.trim() && id.value) {
      updateArticle(id.value, {
        title: form.title,
        slug: form.slug,
        summary: form.summary,
        cover: form.cover,
        contentMd: form.contentMd,
        status: 'draft',
        type: form.type,
        categoryId: form.categoryId,
        tagIds: form.tagIds,
        isTop: form.isTop ? 1 : 0,
        allowComment: form.allowComment ? 1 : 0
      }).then(() => {
        dirty.value = false
        lastSaved.value = new Date().toLocaleTimeString()
        message.info('已自动保存草稿')
      })
    }
  }, 30000)
}

/** 图片上传（真实后端返回完整 URL） */
function onUploadImg(files: File[], callback: (urls: string[]) => void) {
  const urls = files.map((f) => URL.createObjectURL(f))
  callback(urls)
  message.info('已插入本地预览图，接入后端后请改用上传接口返回的地址')
}

watch(form, () => (dirty.value = true), { deep: true })

onMounted(async () => {
  await load()
  categories.value = normalizeCategoryTree(await fetchCategories())
  tags.value = await fetchTags()
  startAutoSave()
})

onUnmounted(() => clearInterval(timer))
</script>

<template>
  <div class="panel">
    <div class="toolbar">
      <h2>文章编辑</h2>
      <a-input v-model:value="form.title" placeholder="文章标题" style="max-width: 360px" />
      <div class="spacer" />
      <span v-if="lastSaved" class="text-muted">上次保存：{{ lastSaved }}</span>
      <a-button v-if="auth.can(id ? 'article:edit' : 'article:create')" :loading="saving" @click="save()">保存草稿</a-button>
      <a-button v-if="auth.can('article:publish')" type="primary" :loading="saving" @click="save('published')">发布</a-button>
    </div>

    <a-spin :spinning="loading">
      <a-row :gutter="12">
        <a-col :xs="24" :md="17">
          <a-card size="small">
            <MdEditor
              v-model="form.contentMd"
              :style="{ height: '540px' }"
              :toolbars="['bold', 'italic', 'strikeThrough', '-', 'title', 'quote', 'code', 'table', '-', 'link', 'image', '-', 'preview', 'fullscreen']"
              @on-upload-img="onUploadImg"
            />
          </a-card>
        </a-col>

        <a-col :xs="24" :md="7">
          <a-card size="small" title="发布设置">
            <a-form layout="vertical" size="small" class="form">
              <a-form-item label="别名 slug">
                <a-input v-model:value="form.slug" placeholder="留空自动生成" />
              </a-form-item>
              <a-form-item label="摘要">
                <a-textarea v-model:value="form.summary" :rows="3" placeholder="用于列表与 SEO 描述" />
              </a-form-item>
              <a-form-item label="封面图 URL">
                <a-input v-model:value="form.cover" placeholder="https://..." />
                <img v-if="form.cover" :src="form.cover" class="cover-preview" alt="封面预览" />
              </a-form-item>
              <a-form-item label="分类">
                <a-tree-select
                  v-model:value="form.categoryId"
                  :tree-data="categories"
                  :field-names="{ title: 'name', value: 'id', key: 'id', children: 'children' }"
                  tree-node-filter-prop="name"
                  tree-default-expand-all
                  allow-clear
                  placeholder="选择分类（任意层级）"
                  style="width: 100%"
                />
              </a-form-item>
              <a-form-item label="标签">
                <a-select
                  v-model:value="form.tagIds"
                  mode="multiple"
                  placeholder="选择标签"
                  style="width: 100%"
                  :options="tags.map((t) => ({ value: t.id, label: t.name }))"
                />
              </a-form-item>
              <a-form-item label="类型">
                <a-radio-group v-model:value="form.type">
                  <a-radio-button value="article">文章</a-radio-button>
                  <a-radio-button value="page">页面</a-radio-button>
                  <a-radio-button value="note">笔记</a-radio-button>
                </a-radio-group>
              </a-form-item>
              <a-form-item label="开关">
                <div class="switch-row">
                  <span>置顶</span><a-switch v-model:checked="form.isTop" />
                  <span class="switch-label">允许评论</span><a-switch v-model:checked="form.allowComment" />
                </div>
              </a-form-item>
            </a-form>
          </a-card>

          <a-card size="small" class="stat-card">
            <p class="text-muted stat-line">字数：{{ wordCount }} · 预计阅读 {{ readingTime }} 分钟</p>
            <p class="text-muted stat-line">状态：{{ form.status === 'published' ? '已发布' : '草稿' }}</p>
            <p class="text-muted stat-line">SEO 关键词与描述取自「摘要」字段，站点级 SEO 在「站点设置」维护</p>
          </a-card>
        </a-col>
      </a-row>
    </a-spin>
  </div>
</template>

<style scoped>
.panel { background: var(--login-card-bg); border-radius: 10px; padding: 18px; }
.toolbar { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; margin-bottom: 14px; }
.toolbar h2 { margin: 0 8px 0 0; font-size: 17px; }
.spacer { flex: 1; }
.form { margin-top: 8px; }
.stat-card { margin-top: 12px; }
.cover-preview { margin-top: 8px; width: 100%; border-radius: 6px; max-height: 140px; object-fit: cover; }
.stat-line { margin: 4px 0; font-size: 12px; }
.switch-row { display: flex; align-items: center; gap: 8px; }
.switch-label { margin-left: 12px; }
.text-muted { color: var(--muted); }
</style>
