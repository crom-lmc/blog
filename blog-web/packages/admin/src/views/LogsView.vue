<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import type { OperationLogItem } from '@blog/shared'
import { cleanLogs, fetchLogs } from '@/api/system'
import { confirmAction } from '@/utils/confirm'
import { useSettingsStore } from '@/stores/settings'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const list = ref<OperationLogItem[]>([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ module: '', page: 1, size: 10 })
const settingsStore = useSettingsStore()

const pageSizeOptions = [10, 20, 30, 50, 100]

/** 切换每页条数：回到第一页 */
function onSizeChange(_current: number, size: number) {
  query.size = size
  query.page = 1
  load()
}

const modules = ['article', 'theme', 'comment', 'media', 'setting']

async function load() {
  loading.value = true
  try {
    const res = await fetchLogs(query)
    list.value = res.list
    total.value = res.total
  } finally {
    loading.value = false
  }
}

/** 清理 90 天前的历史日志 */
function clean() {
  confirmAction({
    title: '清理日志',
    content: '确定清理 90 天前的历史日志？',
    okType: 'danger',
    onOk: async () => {
      const count = await cleanLogs(90)
      message.success(`已清理 ${count} 条`)
      load()
    }
  })
}

onMounted(async () => {
  await settingsStore.load()
  query.size = settingsStore.defaultPageSize
  load()
})
</script>

<template>
  <div class="panel">
    <div class="toolbar">
      <h2>操作日志</h2>
      <a-select
        v-model:value="query.module"
        placeholder="模块筛选"
        allow-clear
        style="width: 160px"
        :options="modules.map((m) => ({ value: m, label: m }))"
        @change="load"
      />
      <a-button @click="load">刷新</a-button>
      <a-button v-if="auth.can('log:clean')" danger @click="clean">清理 90 天前日志</a-button>
      <div class="spacer" />
      <span class="text-muted">共 {{ total }} 条</span>
    </div>

    <a-table :data-source="list" :loading="loading" :pagination="false" size="middle" row-key="id">
      <a-table-column title="时间" data-index="createdAt" :width="170" />
      <a-table-column title="操作人 ID" data-index="userId" :width="110" align="center" />
      <a-table-column title="模块" :width="110">
        <template #default="{ record }">
          <a-tag color="blue">{{ record.module }}</a-tag>
        </template>
      </a-table-column>
      <a-table-column title="动作" data-index="action" :width="100" />
      <a-table-column title="详情" data-index="detail" :ellipsis="true" />
      <a-table-column title="IP" data-index="ip" :width="140" />
    </a-table>

    <!-- 分页条：不写死高度，随内容自然排布 -->
    <a-pagination
      v-model:current="query.page"
      v-model:page-size="query.size"
      :page-size-options="pageSizeOptions"
      :total="total"
      show-size-changer
      class="pager"
      @change="load"
      @show-size-change="onSizeChange"
    />
  </div>
</template>

<style scoped>
.panel {
  background: var(--login-card-bg);
  border-radius: 10px;
  padding: 18px;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 14px;
}

.toolbar h2 {
  margin: 0;
  font-size: 17px;
}

.toolbar .spacer {
  flex: 1;
}

.pager {
  margin-top: 12px;
  text-align: right;
}

.text-muted {
  color: var(--muted);
}
</style>
