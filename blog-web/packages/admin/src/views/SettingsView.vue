<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import type { FriendLink, SiteSettings, SocialLink } from '@blog/shared'
import { fetchSettings, saveSettings } from '@/api/system'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const loading = ref(false)
const tab = ref('basic')
const settings = reactive<SiteSettings>({
  site_title: '',
  site_subtitle: '',
  site_logo: '',
  site_favicon: '',
  site_footer: '',
  icp_no: '',
  seo_keywords: '',
  seo_desc: '',
  comment_review_on: 'true',
  page_size: '10',
  friend_links: '[]',
  social_links: '[]'
})

/** 友链 / 社交链接以 JSON 字符串存储，这里用数组编辑后序列化 */
const friendLinks = ref<FriendLink[]>([])
const socialLinks = ref<SocialLink[]>([])

function parse<T>(raw: string): T[] {
  try {
    const v = JSON.parse(raw || '[]')
    return Array.isArray(v) ? v : []
  } catch {
    return []
  }
}

async function load() {
  loading.value = true
  try {
    const data = await fetchSettings()
    Object.assign(settings, data)
    friendLinks.value = parse<FriendLink>(data.friend_links)
    socialLinks.value = parse<SocialLink>(data.social_links)
  } finally {
    loading.value = false
  }
}

async function save() {
  loading.value = true
  try {
    await saveSettings({
      ...settings,
      friend_links: JSON.stringify(friendLinks.value),
      social_links: JSON.stringify(socialLinks.value)
    })
    message.success('设置已保存')
  } finally {
    loading.value = false
  }
}

function addLink(type: 'friend' | 'social') {
  if (type === 'friend') friendLinks.value.push({ name: '', url: '', desc: '' })
  else socialLinks.value.push({ name: '', url: '', icon: '' })
}

function removeLink(type: 'friend' | 'social', index: number) {
  if (type === 'friend') friendLinks.value.splice(index, 1)
  else socialLinks.value.splice(index, 1)
}

onMounted(load)
</script>

<template>
  <div class="panel">
    <div class="toolbar">
      <h2>站点设置</h2>
    </div>
    <a-spin :spinning="loading">
      <a-tabs v-model:activeKey="tab">
        <!-- 基础信息 -->
        <a-tab-pane key="basic" tab="基础信息">
          <a-form layout="vertical" size="small" class="form">
            <a-form-item label="站点标题">
              <a-input v-model:value="settings.site_title" />
            </a-form-item>
            <a-form-item label="副标题">
              <a-input v-model:value="settings.site_subtitle" />
            </a-form-item>
            <a-form-item label="Logo URL">
              <a-input v-model:value="settings.site_logo" placeholder="https://..." />
            </a-form-item>
            <a-form-item label="Favicon URL">
              <a-input v-model:value="settings.site_favicon" placeholder="https://..." />
            </a-form-item>
            <a-form-item label="页脚文案">
              <a-textarea v-model:value="settings.site_footer" :rows="2" />
            </a-form-item>
            <a-form-item label="备案号">
              <a-input v-model:value="settings.icp_no" />
            </a-form-item>
          </a-form>
        </a-tab-pane>

        <!-- SEO -->
        <a-tab-pane key="seo" tab="SEO">
          <a-form layout="vertical" size="small" class="form">
            <a-form-item label="关键词">
              <a-input v-model:value="settings.seo_keywords" placeholder="英文逗号分隔" />
            </a-form-item>
            <a-form-item label="站点描述">
              <a-textarea v-model:value="settings.seo_desc" :rows="3" />
            </a-form-item>
            <a-form-item label="列表每页条数">
              <a-input-number v-model:value="settings.page_size" :min="1" :max="100" />
            </a-form-item>
          </a-form>
        </a-tab-pane>

        <!-- 评论策略 -->
        <a-tab-pane key="comment" tab="评论策略">
          <a-form layout="vertical" size="small" class="form">
            <a-form-item label="评论需审核">
              <a-switch v-model:checked="settings.comment_review_on" checked-value="true" un-checked-value="false" />
              <span class="switch-tip">开启审核 / 关闭即即时展示</span>
            </a-form-item>
          </a-form>
          <p class="text-muted">关闭后评论即时展示；建议开启，配合敏感词过滤使用。</p>
        </a-tab-pane>

        <!-- 友链 -->
        <a-tab-pane key="friend" tab="友情链接">
          <div v-for="(link, i) in friendLinks" :key="i" class="link-row">
            <a-input v-model:value="link.name" placeholder="名称" style="width: 160px" />
            <a-input v-model:value="link.url" placeholder="链接地址" class="link-grow" />
            <a-input v-model:value="link.desc" placeholder="描述（可选）" style="width: 200px" />
            <a-button type="link" danger @click="removeLink('friend', i)">删除</a-button>
          </div>
          <a-button size="small" @click="addLink('friend')">+ 添加友链</a-button>
        </a-tab-pane>

        <!-- 社交链接 -->
        <a-tab-pane key="social" tab="社交链接">
          <div v-for="(item, i) in socialLinks" :key="i" class="link-row">
            <a-input v-model:value="item.name" placeholder="名称" style="width: 160px" />
            <a-input v-model:value="item.url" placeholder="地址" class="link-grow" />
            <a-input v-model:value="item.icon" placeholder="图标标识" style="width: 160px" />
            <a-button type="link" danger @click="removeLink('social', i)">删除</a-button>
          </div>
          <a-button size="small" @click="addLink('social')">+ 添加链接</a-button>
        </a-tab-pane>
      </a-tabs>
    </a-spin>

    <div class="footer-bar">
      <a-button @click="load">恢复</a-button>
      <a-button v-if="auth.can('setting:edit')" type="primary" @click="save">保存设置</a-button>
    </div>
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
  margin-bottom: 14px;
}

.toolbar h2 {
  margin: 0;
  font-size: 17px;
}

.form {
  max-width: 640px;
  margin-top: 18px;
}

.switch-tip {
  margin-left: 10px;
  color: var(--muted);
  font-size: 12px;
}

.link-row {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  align-items: center;
  flex-wrap: wrap;
}

.link-grow {
  flex: 1;
  min-width: 160px;
}

.footer-bar {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.text-muted {
  color: var(--muted);
}
</style>
