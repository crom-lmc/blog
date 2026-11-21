<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { DeleteOutlined, EditOutlined } from '@ant-design/icons-vue'
import type { MediaFolderNode, MediaItem } from '@blog/shared'
import { copyText } from '@blog/shared'
import {
  createMediaFolder,
  deleteMedia,
  deleteMediaFolder,
  fetchMedia,
  fetchMediaFolderTree,
  renameMediaFolder,
  updateMedia,
  uploadMedia
} from '@/api/content'
import { isNotified } from '@/api/http'
import { confirmAction, promptAction } from '@/utils/confirm'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const apiOrigin = (import.meta.env.VITE_API_TARGET as string) || ''
const list = ref<MediaItem[]>([])
const total = ref(0)
const loading = ref(false)
const mode = ref<'grid' | 'list'>('grid')
const selected = ref<MediaItem | null>(null)

function resolveAssetUrl(rawUrl: string) {
  if (!rawUrl) return ''
  if (/^(https?:)?\/\//i.test(rawUrl) || rawUrl.startsWith('data:')) return rawUrl
  if (!apiOrigin) return rawUrl
  return new URL(rawUrl, apiOrigin).toString()
}

/** 目录树 + 当前选中目录：-1=全部文件，>0=真实目录 */
const folderTree = ref<MediaFolderNode[]>([])
const currentFolderId = ref<number | undefined>(undefined)

const query = reactive({ folderId: undefined as number | undefined, keyword: '', page: 1, size: 50 })

/** 搜索：回到第一页再查（keyword 由后端按 originalName / fileName 模糊匹配） */
function onSearch() {
  query.page = 1
  load()
}

/** 是否选中了具体目录（上传必须落到具体目录，全部文件视图不可上传） */
const hasCurrentFolder = computed(() => (query.folderId ?? -1) > 0)

const pageSizeOptions = [50, 100]
function onSizeChange(_current: number, size: number) {
  query.size = size
  query.page = 1
  load()
}

/** 目录树数据：固定伪节点「全部文件」+ 真实目录树 */
const displayTree = computed<MediaFolderNode[]>(() => [
  { id: -1, name: '全部文件', parentId: -1, sort: 0, mediaCount: 0 },
  ...folderTree.value
])

/** 列表视图「目录」列显示用：id → 名称 */
const folderNameMap = computed<Record<number, string>>(() => {
  const map: Record<number, string> = {}
  const walk = (nodes: MediaFolderNode[]) => {
    for (const n of nodes) {
      map[n.id] = n.name
      if (n.children?.length) walk(n.children)
    }
  }
  walk(folderTree.value)
  return map
})

const currentNodeKey = computed(() => query.folderId ?? -1)

/** a-tree 需要 { key, title, children } 结构，额外用 key 反查原始节点 */
interface TreeNode { key: string; title: string; children?: TreeNode[] }

const treeData = computed(() => {
  const build = (nodes: MediaFolderNode[]): TreeNode[] =>
    nodes.map((n) => ({
      key: String(n.id),
      title: n.name,
      children: n.children?.length ? build(n.children) : undefined
    }))
  return build(displayTree.value)
})

const folderById = computed(() => {
  const map = new Map<number, MediaFolderNode>()
  const walk = (nodes: MediaFolderNode[]) => {
    for (const n of nodes) {
      map.set(n.id, n)
      if (n.children?.length) walk(n.children)
    }
  }
  walk(displayTree.value)
  return map
})

const folderOf = (key: unknown) => folderById.value.get(Number(key))

async function loadTree() {
  try {
    folderTree.value = await fetchMediaFolderTree()
  } catch (e) {
    if (!isNotified(e)) message.error('目录加载失败')
  }
}

function onNodeClick(id: number) {
  // -1=全部（不过滤）、>0=真实目录
  query.folderId = id > 0 ? id : undefined
  query.page = 1
  load()
}

/** 再次点击已选中节点时 antd 会取消选中（keys 为空），这里忽略以保持当前目录 */
function onTreeSelect(keys: Array<string | number>) {
  if (!keys.length) return
  onNodeClick(Number(keys[0]))
}

async function load() {
  loading.value = true
  try {
    const res = await fetchMedia({ ...query, folderId: query.folderId })
    list.value = (res.list || []).map((item) => ({
      ...item,
      url: resolveAssetUrl(item.url)
    }))
    total.value = res.total
  } finally {
    loading.value = false
  }
}

/* ---------- 上传：a-upload 仅作文件选择入口，与拖拽复用同一套上传逻辑 ---------- */

/** 同一批选中的文件合并成一次上传，保留「上传成功 N 个文件」的汇总提示 */
let batchFiles: File[] = []
let batchTimer: ReturnType<typeof setTimeout> | undefined

function beforeUpload(file: File) {
  batchFiles.push(file)
  clearTimeout(batchTimer)
  batchTimer = setTimeout(() => {
    const files = batchFiles
    batchFiles = []
    void upload(files)
  }, 0)
  // 阻止 antd 默认 XHR，实际上传走自有接口
  return false
}

/** 拖拽 / 选择上传：必须先选中具体目录，上传到该目录 */
async function upload(files: FileList | File[]) {
  if (!hasCurrentFolder.value) {
    message.warning('请先在左侧选择一个具体目录再上传')
    return
  }
  const folderId = query.folderId
  let ok = 0
  for (const file of Array.from(files)) {
    try {
      await uploadMedia(file, '', folderId)
      ok += 1
    } catch (e: any) {
      // 拦截器已提示后端返回的原因，这里补充「哪个文件」的上下文
      if (!isNotified(e)) message.error(`「${file.name}」上传失败`)
    }
  }
  if (ok) message.success(`上传成功 ${ok} 个文件`)
  load()
  loadTree()
}

function onDrop(e: DragEvent) {
  e.preventDefault()
  if (e.dataTransfer?.files?.length) void upload(e.dataTransfer.files)
}

async function copyUrl(item: MediaItem) {
  const resolvedUrl = resolveAssetUrl(item.url)
  if (await copyText(resolvedUrl)) {
    message.success('链接已复制')
  } else {
    message.info(resolvedUrl)
  }
}

function remove(item: MediaItem) {
  confirmAction({
    title: '删除媒体文件',
    content: '确定删除该媒体文件？',
    okType: 'danger',
    onOk: async () => {
      await deleteMedia(item.id)
      message.success('已删除')
      load()
      loadTree()
    }
  })
}

/* ---------- 目录管理 ---------- */

/** 新增目录：选中真实目录时作为其子目录，否则建在顶级 */
async function onCreateFolder() {
  const value = await promptAction({ title: '新增目录', placeholder: '目录名称', defaultValue: '新建目录' })
  if (!value?.trim()) return
  try {
    const parentId = query.folderId !== undefined && query.folderId > 0 ? query.folderId : 0
    await createMediaFolder(value.trim(), parentId)
    message.success('目录已创建')
    await loadTree()
  } catch (e) {
    if (!isNotified(e)) message.error('创建失败')
  }
}

async function onRenameFolder(node: MediaFolderNode) {
  const value = await promptAction({ title: '重命名目录', placeholder: '新的目录名称', defaultValue: node.name })
  if (!value?.trim() || value.trim() === node.name) return
  try {
    await renameMediaFolder(node.id, value.trim())
    message.success('已重命名')
    await loadTree()
  } catch (e) {
    if (!isNotified(e)) message.error('重命名失败')
  }
}

function onDeleteFolder(node: MediaFolderNode) {
  confirmAction({
    title: '删除目录',
    content: `确定删除目录「${node.name}」吗？（仅空目录可删除）`,
    okType: 'danger',
    onOk: async () => {
      try {
        await deleteMediaFolder(node.id)
        message.success('目录已删除')
        // 删除的是当前所在目录时退回「全部文件」
        if (query.folderId === node.id) query.folderId = undefined
        await loadTree()
        load()
      } catch (e) {
        if (!isNotified(e)) message.error('删除失败')
      }
    }
  })
}

/* ---------- 媒体文件：重命名 / 移动 ---------- */

/** 重命名（只改展示名 originalName，磁盘文件不变；扩展名不可修改） */
async function onRename(item: MediaItem) {
  const ext = /\.[^.]+$/.exec(item.originalName)?.[0] ?? ''
  const base = ext ? item.originalName.slice(0, -ext.length) : item.originalName
  const value = await promptAction({
    title: ext ? '重命名（后缀不支持修改）' : '重命名',
    placeholder: '文件名',
    defaultValue: base
  })
  if (!value?.trim()) return
  let next = value.trim()
  // 用户若手动输了原后缀，去掉，统一拼回原后缀
  if (ext && next.toLowerCase().endsWith(ext.toLowerCase())) {
    next = next.slice(0, -ext.length)
  }
  next = next + ext
  if (next === item.originalName) return
  try {
    await updateMedia(item.id, { name: next })
    message.success('已重命名')
    load()
  } catch (e) {
    if (!isNotified(e)) message.error('重命名失败')
  }
}

/** 移动目标下拉：仅真实目录树（按层级缩进），不含「未分组」 */
const folderOptions = computed(() => {
  const out: { id: number; name: string }[] = []
  const walk = (nodes: MediaFolderNode[], depth: number) => {
    for (const n of nodes) {
      out.push({ id: n.id, name: '　'.repeat(depth) + n.name })
      if (n.children?.length) walk(n.children, depth + 1)
    }
  }
  walk(folderTree.value, 0)
  return out
})

const moveDialog = ref(false)
const moveTarget = ref<MediaItem | null>(null)
const moveFolderId = ref<number | undefined>(undefined)

function openMove(item: MediaItem) {
  moveTarget.value = item
  moveFolderId.value = item.folderId ?? undefined
  moveDialog.value = true
}

async function confirmMove() {
  if (!moveTarget.value) return
  if (moveFolderId.value === undefined) {
    message.warning('请选择目标目录')
    return
  }
  const id = moveTarget.value.id
  const folderId = moveFolderId.value
  try {
    await updateMedia(id, { folderId })
    message.success('已移动')
    moveDialog.value = false
    load()
    loadTree()
  } catch (e) {
    if (!isNotified(e)) message.error('移动失败')
  }
}

function formatSize(size: number) {
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

/** 关闭大图预览：slot 里用隐藏的 a-image 承载，预览层由 antd 自己 portal 到 body */
function onPreviewVisibleChange(visible: boolean) {
  if (!visible) selected.value = null
}

onMounted(async () => {
  loadTree()
  load()
})
</script>

<template>
  <div class="panel media-page">
    <!-- 左：目录树 -->
    <aside class="folder-panel">
      <div class="folder-head">
        <span class="folder-title">目录</span>
        <a v-if="auth.can('media:edit')" @click="onCreateFolder">新增目录</a>
      </div>
      <a-tree
        :tree-data="treeData"
        :selected-keys="[String(currentNodeKey)]"
        default-expand-all
        block-node
        @select="onTreeSelect"
      >
        <template #title="{ key }">
          <span class="folder-node" :class="{ active: Number(key) === currentNodeKey }">
            <span class="folder-name">
              {{ folderOf(key)?.name }}
              <span v-if="folderOf(key)?.mediaCount" class="folder-count">（{{ folderOf(key)?.mediaCount }}）</span>
            </span>
            <span v-if="Number(key) > 0" class="folder-ops" @click.stop>
              <EditOutlined v-if="auth.can('media:edit')" title="重命名" @click="onRenameFolder(folderOf(key)!)" />
              <DeleteOutlined v-if="auth.can('media:delete')" title="删除目录" @click="onDeleteFolder(folderOf(key)!)" />
            </span>
          </span>
        </template>
      </a-tree>
    </aside>

    <!-- 右：媒体内容 -->
    <section class="content-panel">
      <div class="toolbar">
        <a-radio-group v-model:value="mode" size="small">
          <a-radio-button value="grid">网格</a-radio-button>
          <a-radio-button value="list">列表</a-radio-button>
        </a-radio-group>
        <a-input
          v-model:value="query.keyword"
          placeholder="搜索文件名"
          allow-clear
          size="small"
          style="width: 200px"
          @press-enter="onSearch"
          @change="(e: any) => { if (!e.target.value) onSearch() }"
        />
        <div class="spacer" />
        <a-upload v-if="auth.can('media:upload')" multiple :show-upload-list="false" :before-upload="beforeUpload">
          <a-button type="primary">上传</a-button>
        </a-upload>
      </div>

      <div class="drop-zone" @dragover.prevent @drop="onDrop">
        <span class="text-muted">
          {{
            hasCurrentFolder
              ? '拖拽图片到此处上传（单文件 ≤ 5MB，白名单：jpg/png/gif/webp）'
              : '请先在左侧选择一个目录，再拖拽图片到此处上传'
          }}
        </span>
      </div>

      <a-spin :spinning="loading">
        <!-- 网格视图 -->
        <div v-if="mode === 'grid'" class="media-grid">
          <div v-for="m in list" :key="m.id" class="media-item" @click="selected = m">
            <img :src="m.url" :alt="m.originalName" />
            <div class="media-mask">
              <a-button size="small" @click.stop="copyUrl(m)">复制链接</a-button>
              <a-button v-if="auth.can('media:edit')" size="small" type="primary" @click.stop="onRename(m)">重命名</a-button>
              <a-button v-if="auth.can('media:edit')" size="small" type="primary" @click.stop="openMove(m)">移动</a-button>
              <a-button v-if="auth.can('media:delete')" size="small" danger @click.stop="remove(m)">删除</a-button>
            </div>
            <p class="media-name">{{ m.originalName }}</p>
          </div>
          <a-empty v-if="!list.length" class="media-empty" description="暂无媒体文件" />
        </div>

        <!-- 列表视图 -->
        <a-table v-else :data-source="list" :pagination="false" size="middle" row-key="id">
          <a-table-column title="文件名" data-index="originalName" :ellipsis="true" />
          <a-table-column title="目录" :width="130">
            <template #default="{ record }">
              {{ record.folderId ? folderNameMap[record.folderId] || '—' : '—' }}
            </template>
          </a-table-column>
          <a-table-column title="类型" data-index="mimeType" :width="140" />
          <a-table-column title="大小" :width="100" align="right">
            <template #default="{ record }">{{ formatSize(record.size) }}</template>
          </a-table-column>
          <a-table-column title="上传时间" data-index="createdAt" :width="170" />
          <a-table-column title="操作" :width="240" fixed="right">
            <template #default="{ record }">
              <a-space :size="4" wrap>
                <a @click="copyUrl(record)">复制链接</a>
                <a v-if="auth.can('media:edit')" @click="onRename(record)">重命名</a>
                <a v-if="auth.can('media:edit')" @click="openMove(record)">移动</a>
                <a v-if="auth.can('media:delete')" class="danger" @click="remove(record)">删除</a>
              </a-space>
            </template>
          </a-table-column>
        </a-table>
      </a-spin>

      <!-- 分页条：不写死高度，随内容自然排布 -->
      <a-pagination
        v-model:current="query.page"
        v-model:page-size="query.size"
        :page-size-options="pageSizeOptions"
        :total="total"
        show-size-changer
        :show-total="(t: number) => `共 ${t} 条`"
        class="pager"
        @change="load"
        @show-size-change="onSizeChange"
      />

      <!-- 移动到目录 -->
      <a-modal v-model:open="moveDialog" title="移动到目录" :width="380" ok-text="确定" cancel-text="取消" @ok="confirmMove">
        <a-form layout="vertical" size="small">
          <a-form-item label="目标目录">
            <a-select
              v-model:value="moveFolderId"
              placeholder="请选择目标目录"
              style="width: 100%"
              :options="folderOptions.map((o) => ({ value: o.id, label: o.name }))"
            />
          </a-form-item>
          <p class="text-muted">仅改变文件所属目录，磁盘上的物理文件位置不变。</p>
        </a-form>
      </a-modal>

      <!-- 点击图片预览大图（复制链接/删除按钮已 stop 冒泡，不会触发）；预览层由 a-image portal 到 body -->
      <a-image
        v-if="selected"
        :src="selected.url"
        :preview="{ visible: true, onVisibleChange: onPreviewVisibleChange }"
        class="hidden-preview"
      />
    </section>
  </div>
</template>

<style scoped>
.panel {
  background: var(--login-card-bg);
  border-radius: 10px;
  padding: 16px;
}

.media-page {
  display: flex;
  gap: 12px;
  align-items: stretch;
  min-height: 100%;
}

/* 左：目录树面板 */
.folder-panel {
  flex: 0 0 220px;
  background: var(--login-card-bg);
  border: 1px solid var(--border-subtle);
  border-radius: 10px;
  padding: 10px;
  align-self: stretch;
  overflow-y: auto;
}

.folder-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 2px 4px 10px;
  border-bottom: 1px solid var(--border-subtle);
  margin-bottom: 8px;
}

.folder-title {
  font-size: 13px;
  font-weight: 600;
}

.folder-node {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  flex: 1;
  min-width: 0;
  padding-right: 4px;
}

.folder-node.active .folder-name {
  color: #2f54eb;
}

.folder-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
}

.folder-count {
  color: var(--muted);
  font-size: 12px;
}

.folder-ops {
  display: none;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
  color: var(--muted);
}

.folder-ops .anticon:hover {
  color: #2f54eb;
}

.folder-node:hover .folder-ops {
  display: inline-flex;
}

/* 右：内容面板 */
.content-panel {
  flex: 1;
  min-width: 0;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.toolbar .spacer {
  flex: 1;
}

.drop-zone {
  border: 1px dashed var(--header-border);
  border-radius: 8px;
  padding: 14px;
  text-align: center;
  margin-bottom: 12px;
  background: var(--bg-page);
}

.media-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 12px;
}

/* 空状态横跨整行并居中显示 */
.media-empty {
  grid-column: 1 / -1;
  justify-self: center;
}

.media-item {
  position: relative;
  border: 1px solid var(--border-subtle);
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
}

.media-item img {
  width: 100%;
  height: 110px;
  object-fit: cover;
}

.media-mask {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  /* 四个操作按钮在窄卡片里换行显示 */
  flex-wrap: wrap;
  gap: 6px;
  padding: 8px;
  background: rgba(0, 0, 0, 0.55);
  opacity: 0;
  transition: opacity 0.15s ease;
}

/* 遮罩里的按钮尺寸紧凑一些，避免盖住太多图片 */
.media-mask :deep(.ant-btn + .ant-btn) {
  margin-left: 0;
}

.media-item:hover .media-mask {
  opacity: 1;
}

.media-name {
  margin: 0;
  padding: 6px;
  font-size: 12px;
  text-align: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.pager {
  margin-top: 12px;
  text-align: right;
}

.text-muted {
  color: var(--muted);
}

.danger {
  color: #ff4d4f;
}

/* 只用于承载预览：a-image 的预览层会 portal 到 body */
.hidden-preview {
  display: none;
}

@media (max-width: 1024px) {
  .media-page {
    flex-direction: column;
  }
  .folder-panel {
    flex: none;
    width: 100%;
  }
}
</style>
