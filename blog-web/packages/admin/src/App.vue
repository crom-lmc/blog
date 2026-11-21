<script setup lang="ts">
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
import { useThemeStore } from '@/stores/theme'

dayjs.locale('zh-cn')

const themeStore = useThemeStore()
</script>

<template>
  <a-config-provider :theme="themeStore.antTheme" :locale="zhCN">
    <RouterView />
  </a-config-provider>
</template>
