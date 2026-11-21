<script setup lang="ts">
import type { ThemeConfig } from '@blog/shared'

const props = defineProps<{ config: ThemeConfig }>()

/** 单选卡片组：用图标示意不同布局 */
const homeLayouts = [
  { value: 'list', label: '列表', icon: '▤' },
  { value: 'grid', label: '网格', icon: '▦' },
  { value: 'magazine', label: '杂志', icon: '▥' }
]

const sidebars = [
  { value: 'none', label: '无侧栏', icon: '◻' },
  { value: 'left', label: '左侧栏', icon: '◧' },
  { value: 'right', label: '右侧栏', icon: '◨' }
]

const cardStyles = [
  { value: 'flat', label: '扁平' },
  { value: 'bordered', label: '描边' },
  { value: 'elevated', label: '投影' }
]

const headerStyles = [
  { value: 'fixed', label: '吸顶' },
  { value: 'static', label: '静态' },
  { value: 'transparent', label: '透明' }
]

const coverPositions = [
  { value: 'top', label: '顶部' },
  { value: 'left', label: '左侧' },
  { value: 'background', label: '背景' }
]

const metaFields = [
  { value: 'date', label: '日期' },
  { value: 'category', label: '分类' },
  { value: 'tags', label: '标签' },
  { value: 'views', label: '阅读' },
  { value: 'reading', label: '时长' },
  { value: 'comments', label: '评论' }
]

const cardFields = [
  { value: 'cover', label: '封面' },
  { value: 'title', label: '标题' },
  { value: 'excerpt', label: '摘要' },
  { value: 'meta', label: '元信息' },
  { value: 'tags', label: '标签' }
]

/** a-radio-group / a-checkbox-group 的选项（antd 用 value 作为绑定值） */
const asOptions = (items: Array<{ value: string; label: string }>) => items.map((i) => ({ value: i.value, label: i.label }))
const cardStyleOptions = asOptions(cardStyles)
const headerStyleOptions = asOptions(headerStyles)
const coverPositionOptions = asOptions(coverPositions)
const metaFieldOptions = asOptions(metaFields)
const cardFieldOptions = asOptions(cardFields)
</script>

<template>
  <div class="panel">
    <a-form layout="vertical" size="small">
      <a-form-item label="首页布局">
        <div class="card-group">
          <button
            v-for="item in homeLayouts"
            :key="item.value"
            class="layout-card"
            :class="{ active: props.config.layout.homeLayout === item.value }"
            type="button"
            @click="props.config.layout.homeLayout = item.value as any"
          >
            <span class="layout-icon">{{ item.icon }}</span>
            <span>{{ item.label }}</span>
          </button>
        </div>
      </a-form-item>

      <a-form-item label="侧边栏">
        <div class="card-group">
          <button
            v-for="item in sidebars"
            :key="item.value"
            class="layout-card"
            :class="{ active: props.config.layout.sidebar === item.value }"
            type="button"
            @click="props.config.layout.sidebar = item.value as any"
          >
            <span class="layout-icon">{{ item.icon }}</span>
            <span>{{ item.label }}</span>
          </button>
        </div>
      </a-form-item>

      <a-form-item label="卡片风格">
        <a-radio-group v-model:value="props.config.layout.cardStyle" size="small" :options="cardStyleOptions" />
      </a-form-item>

      <a-form-item label="头部样式">
        <a-radio-group v-model:value="props.config.layout.headerStyle" size="small" :options="headerStyleOptions" />
      </a-form-item>

      <a-form-item label="封面位置">
        <a-radio-group v-model:value="props.config.layout.coverPosition" size="small" :options="coverPositionOptions" />
      </a-form-item>

      <a-form-item label="显示项">
        <a-space :size="12" wrap>
          <a-checkbox v-model:checked="props.config.layout.showToc">目录 TOC</a-checkbox>
          <a-checkbox v-model:checked="props.config.layout.showBreadcrumb">面包屑</a-checkbox>
          <a-checkbox v-model:checked="props.config.layout.showExcerpt">摘要</a-checkbox>
        </a-space>
      </a-form-item>

      <a-form-item label="文章元信息顺序">
        <a-checkbox-group v-model:value="props.config.layout.articleMetaOrder" :options="metaFieldOptions" />
      </a-form-item>

      <a-form-item label="卡片字段">
        <a-checkbox-group v-model:value="props.config.layout.postCardFields" :options="cardFieldOptions" />
      </a-form-item>
    </a-form>
  </div>
</template>

<style scoped>
.panel {
  font-size: 13px;
}

.card-group {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.layout-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  width: 76px;
  padding: 8px 4px;
  border-radius: 8px;
  border: 1px solid var(--header-border);
  background: var(--login-card-bg);
  color: var(--text-base);
  font-size: 12px;
}

.layout-card:hover {
  border-color: #2f54eb;
}

.layout-card.active {
  border-color: #2f54eb;
  color: #2f54eb;
  background: rgba(47, 84, 235, 0.06);
}

.layout-icon {
  font-size: 20px;
  line-height: 1;
}
</style>
