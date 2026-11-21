<script setup lang="ts">
import type { ThemeConfig } from '@blog/shared'
import { DENSITY_UNIT } from '@blog/shared'

const props = defineProps<{ config: ThemeConfig }>()

const densities = [
  { value: 'compact', label: '紧凑', unit: DENSITY_UNIT.compact },
  { value: 'comfortable', label: '适中', unit: DENSITY_UNIT.comfortable },
  { value: 'spacious', label: '宽松', unit: DENSITY_UNIT.spacious }
]
const densityOptions = densities.map((d) => ({ value: d.value, label: `${d.label}·${d.unit}px` }))
</script>

<template>
  <div class="panel">
    <a-form layout="vertical" size="small">
      <a-form-item label="圆角">
        <div class="slider-row">
          <span class="slider-label">小</span>
          <a-slider v-model:value="props.config.radius.sm" :min="0" :max="20" />
          <a-input-number v-model:value="props.config.radius.sm" :min="0" :max="20" class="slider-input" />
        </div>
        <div class="slider-row">
          <span class="slider-label">中</span>
          <a-slider v-model:value="props.config.radius.md" :min="0" :max="28" />
          <a-input-number v-model:value="props.config.radius.md" :min="0" :max="28" class="slider-input" />
        </div>
        <div class="slider-row">
          <span class="slider-label">大</span>
          <a-slider v-model:value="props.config.radius.lg" :min="0" :max="40" />
          <a-input-number v-model:value="props.config.radius.lg" :min="0" :max="40" class="slider-input" />
        </div>
      </a-form-item>

      <a-form-item label="布局密度（覆盖间距单元）">
        <a-radio-group v-model:value="props.config.layout.density" size="small" :options="densityOptions" />
      </a-form-item>

      <a-form-item :label="`间距单元：${DENSITY_UNIT[props.config.layout.density] ?? props.config.space.unit}px`">
        <div class="slider-row">
          <a-slider v-model:value="props.config.space.unit" :min="2" :max="8" :step="1" disabled />
        </div>
      </a-form-item>

      <a-form-item :label="`正文宽度：${props.config.space.contentWidth}px`">
        <div class="slider-row">
          <a-slider v-model:value="props.config.space.contentWidth" :min="600" :max="1000" :step="20" />
          <a-input-number v-model:value="props.config.space.contentWidth" :min="600" :max="1000" :step="20" class="slider-input" />
        </div>
      </a-form-item>

      <a-form-item :label="`容器宽度：${props.config.space.containerWidth}px`">
        <div class="slider-row">
          <a-slider v-model:value="props.config.space.containerWidth" :min="960" :max="1600" :step="20" />
          <a-input-number v-model:value="props.config.space.containerWidth" :min="960" :max="1600" :step="20" class="slider-input" />
        </div>
      </a-form-item>

      <a-form-item :label="`区块间距：${props.config.space.sectionGap}px`">
        <div class="slider-row">
          <a-slider v-model:value="props.config.space.sectionGap" :min="8" :max="80" :step="4" />
          <a-input-number v-model:value="props.config.space.sectionGap" :min="8" :max="80" :step="4" class="slider-input" />
        </div>
      </a-form-item>

      <a-form-item :label="`卡片内边距：${props.config.space.cardPadding}px`">
        <div class="slider-row">
          <a-slider v-model:value="props.config.space.cardPadding" :min="8" :max="48" :step="2" />
          <a-input-number v-model:value="props.config.space.cardPadding" :min="8" :max="48" :step="2" class="slider-input" />
        </div>
      </a-form-item>
    </a-form>
  </div>
</template>

<style scoped>
.panel {
  font-size: 13px;
}

/* a-slider 不带 show-input，用 a-input-number 补上精确输入能力 */
.slider-row {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.slider-row :deep(.ant-slider) {
  flex: 1;
  min-width: 0;
}

.slider-label {
  width: 20px;
  flex-shrink: 0;
}

.slider-input {
  width: 76px;
  flex-shrink: 0;
}
</style>
