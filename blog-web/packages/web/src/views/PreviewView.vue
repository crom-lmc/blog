<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import HomeView from './HomeView.vue'
import PostView from './PostView.vue'
import PostsView from './PostsView.vue'
import CategoriesView from './CategoriesView.vue'
import ArchivesView from './ArchivesView.vue'
import TagsView from './TagsView.vue'

/**
 * 预览承载页：后台主题编辑器用 <iframe src="/preview?preview=1&target=home|posts|post|categories|archives|tags">
 * 加载真实前台页面，再通过 postMessage 下发主题配置（见 src/theme/preview.ts）。
 *
 * 注意：站点外壳（SiteHeader / SiteFooter / FloatingTools）由根组件 App.vue 统一渲染，
 * 这里只负责被预览的目标页面，否则页头/页尾会重复出现两次。
 */
const route = useRoute()
const target = computed(() => String(route.query.target || 'home'))
const slug = computed(() => String(route.query.slug || ''))
</script>

<template>
  <PostView v-if="target === 'post' && slug" :key="slug" :slug="slug" />
  <PostsView v-else-if="target === 'posts'" />
  <CategoriesView v-else-if="target === 'categories'" />
  <ArchivesView v-else-if="target === 'archives'" />
  <TagsView v-else-if="target === 'tags'" />
  <HomeView v-else />
</template>
