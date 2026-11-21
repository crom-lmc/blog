<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { DeleteOutlined, RedoOutlined, UndoOutlined } from '@ant-design/icons-vue'
import { storeToRefs } from 'pinia'
import { useThemeEditorStore } from '@/stores/themeEditor'
import { isNotified } from '@/api/http'
import { confirmAction, promptAction } from '@/utils/confirm'
import { useAuthStore } from '@/stores/auth'
import { getWebOrigin } from '@/utils/origin'
import PanelColor from './theme/PanelColor.vue'
import PanelFont from './theme/PanelFont.vue'
import PanelRadiusSpace from './theme/PanelRadiusSpace.vue'
import PanelLayout from './theme/PanelLayout.vue'
import PanelHomeBlocks from './theme/PanelHomeBlocks.vue'
import PanelDark from './theme/PanelDark.vue'
import PanelCustom from './theme/PanelCustom.vue'

/**
 * 可视化主题编辑器（两栏 + 顶部工具栏）：
 * 左：配置面板（预设/颜色/字体/圆角间距/布局/首页区块/深色/自定义代码）
 * 右：iframe 加载真实前台页面，实时预览
 * 右侧主题列表与操作（撤销重做/草稿/发布/预览令牌/导入导出）放在顶部工具栏
 */
const store = useThemeEditorStore()
const auth = useAuthStore()
const { config, themes, presets, name, dirty, version, canUndo, canRedo, activePresetKey } = storeToRefs(store)

const activeTab = ref('preset')
const iframeEl = ref<HTMLIFrameElement | null>(null)
/** 预览目标：对应前台 /preview 承载页的各页面类型 */
const previewTarget = ref<'home' | 'posts' | 'post' | 'categories' | 'archives' | 'tags'>('home')
const previewSlug = ref('')
const importInput = ref<HTMLInputElement | null>(null)

/** 预览目标选项：label 展示在工具栏，value 传给前台 /preview?target= */
const previewTargets = [
  { label: '首页', value: 'home' },
  { label: '文章列表', value: 'posts' },
  { label: '文章详情', value: 'post' },
  { label: '分类', value: 'categories' },
  { label: '归档', value: 'archives' },
  { label: '标签', value: 'tags' }
] as const

/** 预览 iframe 地址：带 token 时前台先拉取后端快照 */
const iframeSrc = ref('')

function rebuildIframeSrc() {
  const origin = getWebOrigin()
  const token = store.previewToken
  const params = new URLSearchParams({ preview: '1', target: previewTarget.value })
  if (previewTarget.value === 'post' && previewSlug.value) params.set('slug', previewSlug.value)
  if (token) params.set('token', token)
  iframeSrc.value = `${origin}/preview?${params.toString()}`
}

function refreshPreviewTarget() {
  store.newPreviewToken().finally(rebuildIframeSrc)
}

/** 把最新配置实时下发给 iframe（输入即生效，无需刷新） */
function sendPreview() {
  // 必须发「纯对象快照」：config.value 是 Vue 响应式代理，直接 postMessage 在部分浏览器
  // 会因 structured clone 失败抛 DataCloneError，导致消息静默丢失——表现为「初始预览正常、
  // 但点预设/改任何配置预览都没反应」。转成纯 JSON 快照即可消除该问题。
  const snapshot = JSON.parse(JSON.stringify(config.value))
  iframeEl.value?.contentWindow?.postMessage({ type: 'blog:theme:preview', config: snapshot }, '*')
}

let commitTimer: ReturnType<typeof setTimeout> | undefined

// 配置变化：立即下发预览；撤销重做快照做防抖（避免滑块拖动产生几十步历史）
watch(
  config,
  () => {
    sendPreview()
    clearTimeout(commitTimer)
    commitTimer = setTimeout(() => store.commit(), 800)
  },
  { deep: true }
)

async function onSaveDraft() {
  await store.saveDraft()
  message.success('草稿已保存（未发布，前台不受影响）')
}

function onPublish() {
  confirmAction({
    title: '发布确认',
    content: '发布后立即覆盖线上样式（version +1），确认发布？',
    onOk: async () => {
      const vo = await store.publish()
      message.success(`已发布，版本号：v${vo?.version ?? version.value}`)
      refreshPreviewTarget()
    }
  })
}

function onReset() {
  confirmAction({
    title: '重置确认',
    content: '将配置重置为内置默认主题，当前修改丢弃（可用撤销恢复）？',
    onOk: async () => {
      await store.reset()
    }
  })
}

function onExport() {
  store.downloadJson()
}

function onImportClick() {
  importInput.value?.click()
}

async function onImportFile(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return
  try {
    await store.importFromFile(file)
    message.success('导入成功，已创建新主题')
    rebuildIframeSrc()
  } catch (err: any) {
    if (!isNotified(err)) message.error(err?.message || '导入失败，请检查 JSON 格式')
  } finally {
    ;(e.target as HTMLInputElement).value = ''
  }
}

async function onSelectTheme(id: number) {
  await store.select(id)
  rebuildIframeSrc()
}

async function onCreateTheme() {
  const value = await promptAction({ title: '新建主题', placeholder: '主题名称', defaultValue: `主题 ${themes.value.length + 1}` })
  if (!value) return
  try {
    await store.create(value)
    message.success('已创建')
    rebuildIframeSrc()
  } catch (e) {
    if (!isNotified(e)) message.error('创建失败')
  }
}

/** 非内置主题才可删除（内置主题由后端 2008 兜底拦截） */
function isBuiltin(t: { isBuiltin?: number | boolean }) {
  return Number(t.isBuiltin) === 1 || t.isBuiltin === true
}

/** 生效中的主题不可删除（后端 2007 兜底拦截） */
function isActiveTheme(t: { isActive: number | boolean }) {
  return Number(t.isActive) === 1 || t.isActive === true
}

function onDeleteTheme(t: { id: number; name: string; isActive: number | boolean; isBuiltin?: number | boolean }) {
  if (isBuiltin(t)) {
    message.warning('默认主题不支持删除')
    return
  }
  if (isActiveTheme(t)) {
    message.warning('生效中的主题不能删除，请先把其他主题发布上线')
    return
  }
  confirmAction({
    title: '删除主题',
    content: `确定删除主题「${t.name}」吗？删除后不可恢复。`,
    okType: 'danger',
    onOk: async () => {
      try {
        await store.remove(t.id)
        message.success('已删除')
        rebuildIframeSrc()
      } catch (err: any) {
        if (!isNotified(err)) message.error(err?.message || '删除失败')
      }
    }
  })
}

/** iframe 就绪后立即下发一次配置 */
function onMessage(e: MessageEvent) {
  const data = e.data
  if (data && data.type === 'blog:theme:ready') sendPreview()
}

onMounted(async () => {
  window.addEventListener('message', onMessage)
  await store.load()
  await nextTick()
  refreshPreviewTarget()
})

onBeforeUnmount(() => {
  window.removeEventListener('message', onMessage)
  clearTimeout(commitTimer)
})
</script>

<template>
  <a-spin :spinning="store.loading">
    <div class="theme-editor">
      <!-- 顶部工具栏 -->
      <div class="toolbar">
        <a-select :value="store.themeId" style="width: 180px" @change="onSelectTheme">
          <a-select-option v-for="t in themes" :key="t.id" :value="t.id">
            <div class="theme-option">
              <span class="theme-option-name">{{ t.name }}{{ Number(t.isActive) === 1 ? '（生效中）' : '' }}</span>
              <DeleteOutlined
                v-if="!isBuiltin(t) && !isActiveTheme(t) && auth.can('theme:delete')"
                class="theme-option-del"
                title="删除主题"
                @click.stop="onDeleteTheme(t)"
              />
            </div>
          </a-select-option>
        </a-select>
        <a-button v-if="auth.can('theme:edit')" @click="onCreateTheme">新建</a-button>
        <a-divider type="vertical" />
        <a-tooltip title="撤销（最多 20 步）">
          <a-button :disabled="!canUndo" @click="store.undo()"><UndoOutlined /></a-button>
        </a-tooltip>
        <a-tooltip title="重做">
          <a-button :disabled="!canRedo" @click="store.redo()"><RedoOutlined /></a-button>
        </a-tooltip>
        <a-button v-if="auth.can('theme:edit')" @click="onReset">重置默认</a-button>
        <div class="spacer" />
        <span class="text-muted meta">{{ name }} · v{{ version }}{{ dirty ? ' · 有未保存修改' : '' }}</span>
        <a-button v-if="auth.can('theme:edit')" @click="onImportClick">导入</a-button>
        <input ref="importInput" type="file" accept="application/json" style="display: none" @change="onImportFile" />
        <a-button @click="onExport">导出</a-button>
        <a-button v-if="auth.can('theme:edit')" :disabled="!store.themeId" @click="onSaveDraft">保存草稿</a-button>
        <a-button v-if="auth.can('theme:publish')" type="primary" :disabled="!store.themeId" @click="onPublish">发布上线</a-button>
      </div>

      <div class="editor-body">
        <!-- 左：配置面板 -->
        <div class="panel-col">
          <a-tabs v-model:activeKey="activeTab">
            <a-tab-pane key="preset" tab="预设">
              <div class="preset-list">
                <div
                  v-for="p in presets"
                  :key="p.key"
                  class="preset-card"
                  :class="{ active: p.key === activePresetKey }"
                  role="button"
                  tabindex="0"
                  :aria-pressed="p.key === activePresetKey"
                  @click="store.applyPreset(p)"
                  @keydown.enter.prevent="store.applyPreset(p)"
                  @keydown.space.prevent="store.applyPreset(p)"
                >
                  <span class="preset-dot" :style="{ background: p.config?.color?.primary || '#4f46e5' }" />
                  <div class="preset-info">
                    <p class="preset-name">{{ p.name }}</p>
                    <p class="preset-desc text-muted">{{ p.description }}</p>
                  </div>
                  <svg class="preset-check" viewBox="0 0 24 24" width="16" height="16" aria-hidden="true">
                    <path
                      d="M20 6 9 17l-5-5"
                      fill="none"
                      stroke="currentColor"
                      stroke-width="2.4"
                      stroke-linecap="round"
                      stroke-linejoin="round"
                    />
                  </svg>
                </div>
              </div>
            </a-tab-pane>
            <a-tab-pane key="color" tab="颜色"><PanelColor :config="config" /></a-tab-pane>
            <a-tab-pane key="font" tab="字体"><PanelFont :config="config" /></a-tab-pane>
            <a-tab-pane key="space" tab="间距"><PanelRadiusSpace :config="config" /></a-tab-pane>
            <a-tab-pane key="layout" tab="布局"><PanelLayout :config="config" /></a-tab-pane>
            <a-tab-pane key="home" tab="首页"><PanelHomeBlocks :config="config" /></a-tab-pane>
            <a-tab-pane key="dark" tab="深色"><PanelDark :config="config" /></a-tab-pane>
            <a-tab-pane key="custom" tab="自定义"><PanelCustom :config="config" /></a-tab-pane>
          </a-tabs>
        </div>

        <!-- 中/右：真实前台预览 -->
        <div class="preview-col">
          <div class="preview-toolbar">
            <a-radio-group v-model:value="previewTarget" size="small" @change="refreshPreviewTarget">
              <a-radio-button v-for="t in previewTargets" :key="t.value" :value="t.value">{{ t.label }}</a-radio-button>
            </a-radio-group>
            <a-input
              v-if="previewTarget === 'post'"
              v-model:value="previewSlug"
              placeholder="文章 slug，如 frontend-1-从零"
              size="small"
              style="width: 240px"
              @change="refreshPreviewTarget"
            />
            <span class="text-muted">实时预览（真实前台页面）</span>
          </div>
          <div class="iframe-wrap">
            <iframe ref="iframeEl" :src="iframeSrc" class="preview-iframe" title="前台预览" />
          </div>
        </div>
      </div>
    </div>
  </a-spin>
</template>

<style scoped>
.theme-editor {
  display: flex;
  flex-direction: column;
  /* 顶栏 56 + 面包屑 42 + 内容区下 padding 18 */
  height: calc(100vh - 116px);
  overflow: hidden;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  border-bottom: 1px solid var(--header-border);
  background: var(--login-card-bg);
  flex-wrap: wrap;
}

.toolbar .spacer,
.meta {
  flex: 0 1 auto;
}

.meta {
  margin-left: auto;
  margin-right: 12px;
  font-size: 12px;
}

.editor-body {
  flex: 1;
  display: grid;
  grid-template-columns: 360px minmax(0, 1fr);
  gap: 12px;
  padding: 12px;
  min-height: 0;
}

.panel-col {
  overflow-y: auto;
  background: var(--login-card-bg);
  border: 1px solid var(--border-subtle);
  border-radius: 10px;
  padding: 8px 12px;
}

.preset-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.preset-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border-radius: 8px;
  border: 1px solid var(--border-subtle);
  cursor: pointer;
  transition: border-color 0.15s ease, background-color 0.15s ease;
}

.preset-card:hover {
  border-color: #2f54eb;
}

.preset-card:focus-visible {
  outline: 2px solid #2f54eb;
  outline-offset: 1px;
}

/* 当前配置与该预设完全一致：高亮选中态 */
.preset-card.active {
  border-color: #2f54eb;
  background: rgba(47, 84, 235, 0.06);
}

.preset-card.active .preset-dot {
  box-shadow: 0 0 0 2px var(--login-card-bg), 0 0 0 3px #2f54eb;
}

.preset-dot {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  flex-shrink: 0;
}

.preset-info {
  flex: 1;
  min-width: 0;
}

.preset-name {
  margin: 0;
  font-size: 13px;
  font-weight: 600;
}

.preset-desc {
  margin: 2px 0 0;
  font-size: 12px;
}

.preset-check {
  flex-shrink: 0;
  color: #2f54eb;
  opacity: 0;
  transform: scale(0.7);
  transition: opacity 0.15s ease, transform 0.15s ease;
}

.preset-card.active .preset-check {
  opacity: 1;
  transform: scale(1);
}

.preview-col {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

/* 主题下拉项：名称 + 删除按钮（仅非内置主题显示） */
.theme-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.theme-option-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.theme-option-del {
  flex-shrink: 0;
  color: var(--muted);
  transition: color 0.15s ease;
}

.theme-option-del:hover {
  color: #ff4d4f;
}

.preview-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}

.iframe-wrap {
  flex: 1;
  border: 1px solid var(--border-subtle);
  border-radius: 10px;
  overflow: hidden;
  background: var(--login-card-bg);
  min-height: 0;
}

.preview-iframe {
  width: 100%;
  height: 100%;
  border: none;
}

.text-muted {
  color: var(--muted);
}

@media (max-width: 1024px) {
  .editor-body {
    grid-template-columns: 1fr;
  }
  .iframe-wrap {
    min-height: 480px;
  }
}
</style>
