<script setup lang="ts">
import type { ColorTokens, ThemeConfig } from '@blog/shared'

const props = defineProps<{ config: ThemeConfig }>()

const fields: { key: keyof ColorTokens; label: string }[] = [
  { key: 'primary', label: '主色' },
  { key: 'bg', label: '页面背景' },
  { key: 'bgSubtle', label: '次级背景' },
  { key: 'surface', label: '卡片背景' },
  { key: 'text', label: '正文文字' },
  { key: 'textMuted', label: '次要文字' },
  { key: 'border', label: '边框' }
]

const modes = [
  { value: 'light', label: '浅色' },
  { value: 'dark', label: '深色' },
  { value: 'system', label: '跟随系统' }
]
const modeOptions = modes.map((m) => ({ value: m.value, label: m.label }))

/** 原生取色器只认 #rrggbb，rgba()/hsl() 先转 hex 用于回填，不改写原值 */
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

/** 未配置的深色令牌按主色派生一个基础值 */
function ensureToken(key: keyof ColorTokens) {
  if (!props.config.dark.tokens[key]) {
    ;(props.config.dark.tokens as any)[key] = props.config.color[key]
  }
}

fields.forEach((f) => ensureToken(f.key))
</script>

<template>
  <div class="panel">
    <a-form layout="vertical" size="small">
      <a-form-item label="启用深色模式">
        <a-switch v-model:checked="props.config.dark.enabled" />
      </a-form-item>

      <a-form-item label="默认模式">
        <a-radio-group v-model:value="props.config.dark.defaultMode" size="small" :options="modeOptions" />
      </a-form-item>

      <a-form-item label="深色令牌覆盖">
        <div class="color-grid">
          <div v-for="f in fields" :key="f.key" class="color-item">
            <span class="color-label">{{ f.label }}</span>
            <input
              type="color"
              class="color-native"
              :value="toHex((props.config.dark.tokens as any)[f.key])"
              @input="(props.config.dark.tokens as any)[f.key] = ($event.target as HTMLInputElement).value"
            />
            <a-input
              :value="(props.config.dark.tokens as any)[f.key]"
              size="small"
              class="color-text"
              @change="(e: any) => ((props.config.dark.tokens as any)[f.key] = e.target.value)"
            />
          </div>
        </div>
      </a-form-item>
    </a-form>
  </div>
</template>

<style scoped>
.panel {
  font-size: 13px;
}

.color-grid {
  display: flex;
  flex-direction: column;
  gap: 6px;
  width: 100%;
}

.color-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.color-label {
  width: 88px;
  flex-shrink: 0;
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
