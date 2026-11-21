<script setup lang="ts">
import type { ThemeConfig } from '@blog/shared'
import { FONT_STACKS, SCALE_RATIOS } from '@blog/shared'

const props = defineProps<{ config: ThemeConfig }>()

const weights = [400, 500, 600, 700, 800]

/** 正文字体 / 代码字体选项 */
const fontOptions = FONT_STACKS.map((f) => ({ value: f.value, label: f.label }))
/** 标题字体多一个「继承正文」 */
const headingOptions = [{ value: 'inherit', label: '继承正文' }, ...fontOptions]
/** 标题缩放比 / 字重选项 */
const ratioOptions = SCALE_RATIOS.map((r) => ({ value: r, label: String(r) }))
const weightOptions = weights.map((w) => ({ value: w, label: String(w) }))
</script>

<template>
  <div class="panel">
    <a-form layout="vertical" size="small">
      <a-form-item label="正文字体">
        <a-select v-model:value="props.config.font.familyBody" style="width: 100%" :options="fontOptions" />
      </a-form-item>

      <a-form-item label="标题字体">
        <a-select v-model:value="props.config.font.familyHeading" style="width: 100%" :options="headingOptions" />
      </a-form-item>

      <a-form-item label="代码字体">
        <a-select v-model:value="props.config.font.familyCode" style="width: 100%" :options="fontOptions" />
      </a-form-item>

      <a-form-item :label="`基准字号：${props.config.font.sizeBase}px`">
        <div class="slider-row">
          <a-slider v-model:value="props.config.font.sizeBase" :min="12" :max="22" :step="1" />
          <a-input-number v-model:value="props.config.font.sizeBase" :min="12" :max="22" class="slider-input" />
        </div>
      </a-form-item>

      <a-form-item label="标题缩放比">
        <a-select v-model:value="props.config.font.scaleRatio" style="width: 100%" :options="ratioOptions" />
      </a-form-item>

      <a-form-item :label="`行高：${props.config.font.lineHeight}`">
        <div class="slider-row">
          <a-slider v-model:value="props.config.font.lineHeight" :min="1.3" :max="2.2" :step="0.05" />
          <a-input-number v-model:value="props.config.font.lineHeight" :min="1.3" :max="2.2" :step="0.05" class="slider-input" />
        </div>
      </a-form-item>

      <a-form-item :label="`字间距：${props.config.font.letterSpacing}px`">
        <div class="slider-row">
          <a-slider v-model:value="props.config.font.letterSpacing" :min="-0.5" :max="3" :step="0.1" />
          <a-input-number v-model:value="props.config.font.letterSpacing" :min="-0.5" :max="3" :step="0.1" class="slider-input" />
        </div>
      </a-form-item>

      <a-form-item label="标题字重">
        <a-select v-model:value="props.config.font.headingWeight" style="width: 100%" :options="weightOptions" />
      </a-form-item>
    </a-form>

    <div class="preview-text">
      <p class="preview-title">标题预览 The quick brown fox</p>
      <p class="preview-body">正文预览：这是一段用于观察字号、行高与字间距变化的示例文本。</p>
    </div>
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

.slider-input {
  width: 76px;
  flex-shrink: 0;
}

.preview-text {
  margin-top: 8px;
  padding: 10px;
  border-radius: 8px;
  background: var(--bg-page);
}

.preview-title {
  margin: 0 0 6px;
  font-weight: var(--heading-weight, 700);
  font-size: calc(16px * 1.25);
}

.preview-body {
  margin: 0;
  font-size: 13px;
  line-height: 1.7;
}
</style>
