<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="no-access">
    <a-result status="warning" title="没有可访问的页面" sub-title="当前账号没有被授予任何权限，请联系管理员分配角色。">
      <template #extra>
        <a-button @click="router.push('/login')">切换账号</a-button>
      </template>
    </a-result>
  </div>
</template>

<style scoped>
.no-access { padding: 60px 0; }
</style>
