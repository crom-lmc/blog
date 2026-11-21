<script setup lang="ts">
import { ref } from 'vue'
import type { ColorTokens, ThemeConfig } from '@blog/shared'
import { derivePrimaryVariants } from '@blog/shared'

const props = defineProps<{ config: ThemeConfig }>()

/** 颜色字段中文名 */
const fields: { key: keyof ColorTokens; label: string }[] = [
  { key: 'primary', label: '主色' },
  { key: 'primaryHover', label: '主色-悬浮' },
  { key: 'primarySubtle', label: '主色-浅底' },
  { key: 'bg', label: '页面背景' },
  { key: 'bgSubtle', label: '次级背景' },
  { key: 'surface', label: '卡片背景' },
  { key: 'text', label: '正文文字' },
  { key: 'textMuted', label: '次要文字' },
  { key: 'textInvert', label: '反色文字' },
  { key: 'border', label: '边框' },
  { key: 'link', label: '链接' },
  { key: 'success', label: '成功' },
  { key: 'warning', label: '警告' },
  { key: 'danger', label: '危险' }
]

/** 主色变体是否自动派生（避免让用户手填三个色值） */
const autoDerive = ref(true)

/**
 * 原生取色器只认 #rrggbb，rgba()/hsl() 等复杂值先转成 hex 用于回填，不改写原值。
 * 旁边保留可编辑输入框，因此 show-alpha 的灵活度不丢失。
 */
function toHex(value: string): string {
  if (!value) return '#000000'
  if (/^#[0-9a-f]{6}$/i.test(value)) return value
  if (/^#[0-9a-f]{3}$/i.test(value)) {
    return '#' + value.slice(1).split('').map((c) => c + c).join('')
  }
  const nums = value.match(/[\d.]+/g)?.map(Number)
  if (nums && nums.length >= 3) {
    return '#' + nums.slice(0, 3)
      .map((n) => Math.max(0, Math.min(255, Math.round(n))).toString(16).padStart(2, '0'))
      .join('')
  }
  return '#000000'
}

/**
 * 用户改色后写回配置。
 * 这里刻意用「事件」驱动派生，而不是 watch(config.color.primary)：
 * 预设 / 切换主题 / 撤销重做 / 导入 都会整体替换 config，
 * 而配置里的变体是预设手工调好的（如极简黑白的 primaryHover=#000000），
 * 用 watcher 会在换主色时被派生值覆盖 —— 表现为「点了预设但选中态立刻消失，得再点一次才高亮」。
 */
function onPick(key: keyof ColorTokens, val: string | null) {
  ;(props.config.color as any)[key] = val ?? ''
  if (key !== 'primary' || !autoDerive.value || !val) return
  const v = derivePrimaryVariants(val)
  props.config.color.primaryHover = v.primaryHover
  props.config.color.primarySubtle = v.primarySubtle
  props.config.color.textInvert = v.onPrimary
  props.config.color.link = val
}
</script>

<template>
  <div class="panel">
    <div class="panel-head">
      <span>主色变体自动派生</span>
      <a-switch v-model:checked="autoDerive" size="small" />
    </div>

    <a-form layout="vertical" size="small" class="color-form">
      <a-form-item v-for="f in fields" :key="f.key" :label="f.label" class="color-item">
        <div class="color-controls">
          <input
            type="color"
            class="color-native"
            :value="toHex((config.color as any)[f.key])"
            @input="onPick(f.key, ($event.target as HTMLInputElement).value)"
          />
          <a-input
            :value="(config.color as any)[f.key]"
            size="small"
            class="color-text"
            @change="(e: any) => onPick(f.key, e.target.value)"
          />
        </div>
      </a-form-item>
    </a-form>
  </div>
</template>

<style scoped>
.panel {
  font-size: 13px;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 8px;
  margin-bottom: 8px;
  border-bottom: 1px solid var(--border-subtle);
}

.color-form {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.color-form :deep(.color-item) {
  margin-bottom: 0;
}

.color-form :deep(.ant-form-item-label) {
  padding-bottom: 2px;
}

.color-form :deep(.ant-form-item-label > label) {
  font-size: 12px;
  color: var(--muted);
}

.color-controls {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.color-native {
  width: 34px;
  height: 24px;
  flex-shrink: 0;
  padding: 0;
  border: 1px solid var(--border-subtle);
  border-radius: 4px;
  background: transparent;
  cursor: pointer;
}

.color-text {
  flex: 1;
  min-width: 0;
  font-size: 12px;
}
</style>
