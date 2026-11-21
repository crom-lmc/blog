<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, shallowRef } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { BellOutlined, EyeOutlined, FileTextOutlined, MessageOutlined } from '@ant-design/icons-vue'
import * as echarts from 'echarts'
import type { DashboardStats } from '@blog/shared'
import { fetchStats } from '@/api/system'
import { isNotified } from '@/api/http'
import { getWebOrigin } from '@/utils/origin'

const stats = ref<DashboardStats | null>(null)
const router = useRouter()
const loading = ref(true)
const chartEl = ref<HTMLDivElement | null>(null)
const chart = shallowRef<echarts.ECharts | null>(null)

/** 统计卡片：图标统一用 @ant-design/icons-vue，配色取自 antd 主色板（与主题主色 #2f54eb 协调） */
const cards = computed(() => [
  { label: '文章总数', value: stats.value?.articleCount ?? 0, icon: FileTextOutlined, color: '#2f54eb' },
  { label: '全站阅读', value: stats.value?.viewCount ?? 0, icon: EyeOutlined, color: '#52c41a' },
  { label: '评论总数', value: stats.value?.commentCount ?? 0, icon: MessageOutlined, color: '#faad14' },
  { label: '待审评论', value: stats.value?.pendingCommentCount ?? 0, icon: BellOutlined, color: '#ff4d4f' }
])

/** 与后端 DashboardStatsVO 对齐：articleTrend / commentTrend（{date, count}） */
function renderChart() {
  if (!chartEl.value || !stats.value) return
  chart.value = chart.value || echarts.init(chartEl.value)
  chart.value.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['文章发布', '评论数'] },
    grid: { left: 40, right: 20, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: stats.value.articleTrend.map((t) => t.date.slice(5)), boundaryGap: false },
    yAxis: { type: 'value' },
    series: [
      {
        name: '文章发布',
        type: 'line',
        smooth: true,
        areaStyle: { opacity: 0.15 },
        data: stats.value.articleTrend.map((t) => t.count)
      },
      {
        name: '评论数',
        type: 'line',
        smooth: true,
        data: stats.value.commentTrend.map((t) => t.count)
      }
    ]
  })
}

function resize() {
  chart.value?.resize()
}

/** 前台访问地址：生产同域直接用当前 origin，开发可用 VITE_WEB_ORIGIN 覆盖 */
const webOrigin = getWebOrigin()

/** 在前台新窗口打开文章；用绝对地址，否则会打开后台自己的 /posts/:slug（后台无此路由，被 catch-all 重定向回仪表盘） */
function openPost(slug: string) {
  if (!slug) return
  window.open(`${webOrigin}/posts/${slug}`, '_blank', 'noopener')
}

onMounted(async () => {
  try {
    stats.value = await fetchStats()
    await Promise.resolve()
    renderChart()
  } catch (e) {
    // 拦截器已弹过后端原因，这里只兜底未知错误
    if (!isNotified(e)) message.error('统计信息加载失败')
  } finally {
    loading.value = false
  }
  window.addEventListener('resize', resize)
})

onUnmounted(() => {
  window.removeEventListener('resize', resize)
  chart.value?.dispose()
})
</script>

<template>
  <div class="page-container dashboard-page">
    <!-- 统计卡片 -->
    <a-row :gutter="[12, 12]">
      <a-col v-for="c in cards" :key="c.label" :xs="12" :sm="12" :md="6">
        <a-card class="stat-card" size="small">
          <div class="card-inner">
            <component :is="c.icon" class="stat-icon" :style="{ color: c.color }" />
            <div>
              <p class="card-value">{{ c.value }}</p>
              <p class="card-label text-muted">{{ c.label }}</p>
            </div>
          </div>
        </a-card>
      </a-col>
    </a-row>

    <!-- 近 30 天趋势 -->
    <a-card class="trend-card" title="近 30 天趋势（文章发布 / 评论）">
      <a-spin :spinning="loading">
        <div ref="chartEl" class="chart" />
      </a-spin>
    </a-card>

    <!-- 热门文章 & 待审评论 -->
    <a-row :gutter="[12, 12]" class="bottom-row">
      <a-col :xs="24" :md="14">
        <a-card title="热门文章 TOP10" class="fill-card">
          <a-table :data-source="stats?.topArticles || []" :pagination="false" size="small" row-key="id">
            <a-table-column title="标题" data-index="title" :ellipsis="true" />
            <a-table-column title="阅读" data-index="viewCount" :width="90" align="right" />
            <a-table-column title="操作" :width="80" align="center">
              <template #default="{ record }">
                <a @click="openPost(record.slug)">查看</a>
              </template>
            </a-table-column>
          </a-table>
        </a-card>
      </a-col>

      <a-col :xs="24" :md="10">
        <a-card class="fill-card">
          <template #title>
            <div class="card-head">
              <span>待审评论</span>
              <a-button type="link" size="small" @click="router.push('/comments')">去审核</a-button>
            </div>
          </template>
          <a-empty v-if="!stats?.pendingComments?.length" description="暂无待审评论" />
          <ul v-else class="pending-list">
            <li v-for="c in stats.pendingComments" :key="c.id">
              <p class="pending-meta">
                <strong>{{ c.authorName }}</strong>
                <span v-if="c.articleTitle" class="text-muted"> · {{ c.articleTitle }}</span>
                <span class="text-muted"> · {{ c.createdAt }}</span>
              </p>
              <p class="pending-content">{{ c.content }}</p>
            </li>
          </ul>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<style scoped>
/* 页面至少填满主区域：正常高度无滚动条；窗口过矮内容放不下时自然出现页面滚动 */
.dashboard-page {
  min-height: 100%;
  display: flex;
  flex-direction: column;
}

.trend-card {
  flex: none;
  margin-top: 12px;
}

.bottom-row {
  flex: 1;
  min-height: 0;
  margin-top: 12px;
}

/* 两张底部卡片撑满剩余高度，内容区独立滚动 */
.bottom-row :deep(.fill-card) {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--login-card-bg);
}

.bottom-row :deep(.fill-card > .ant-card-body) {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

@media (max-width: 991px) {
  /* 窄屏纵向堆叠 */
  .bottom-row {
    flex: none;
  }
  .bottom-row :deep(.fill-card) {
    height: auto;
  }
}

.stat-icon {
  font-size: 22px;
}

.card-inner {
  display: flex;
  align-items: center;
  gap: 12px;
}

.card-value {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
}

.card-label {
  margin: 2px 0 0;
  font-size: 12px;
}

.text-muted {
  color: var(--muted);
}

.chart {
  height: 240px;
  width: 100%;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.pending-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.pending-list li {
  padding: 8px 0;
  border-bottom: 1px solid var(--border-subtle);
}

.pending-meta {
  margin: 0;
  font-size: 12px;
}

.pending-content {
  margin: 4px 0 0;
  font-size: 13px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
