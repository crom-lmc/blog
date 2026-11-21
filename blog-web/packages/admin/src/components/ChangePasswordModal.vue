<script setup lang="ts">
import { reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { changePassword } from '@/api/auth'

/** 当前登录人自助修改密码弹窗。由用户下拉菜单「修改密码」打开。 */
const open = defineModel<boolean>('open', { default: false })

const form = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const saving = ref(false)

function reset() {
  form.oldPassword = ''
  form.newPassword = ''
  form.confirmPassword = ''
}

async function submit() {
  if (!form.oldPassword || !form.newPassword) {
    message.warning('请填写原密码和新密码')
    return
  }
  if (form.newPassword.length < 6) {
    message.warning('新密码至少 6 位')
    return
  }
  if (form.newPassword !== form.confirmPassword) {
    message.warning('两次输入的新密码不一致')
    return
  }
  saving.value = true
  try {
    await changePassword(form.oldPassword, form.newPassword)
    message.success('密码已修改，请使用新密码登录')
    open.value = false
    reset()
  } finally {
    saving.value = false
  }
}

function onCancel() {
  open.value = false
  reset()
}
</script>

<template>
  <a-modal
    :open="open"
    title="修改密码"
    :confirm-loading="saving"
    ok-text="确定修改"
    cancel-text="取消"
    centered
    :mask-closable="false"
    @ok="submit"
    @cancel="onCancel"
  >
    <a-form layout="vertical" class="change-pwd-form">
      <a-form-item label="原密码" required>
        <a-input-password v-model:value="form.oldPassword" placeholder="请输入当前密码" autocomplete="current-password" />
      </a-form-item>
      <a-form-item label="新密码" required>
        <a-input-password v-model:value="form.newPassword" placeholder="至少 6 位" autocomplete="new-password" />
      </a-form-item>
      <a-form-item label="确认新密码" required>
        <a-input-password v-model:value="form.confirmPassword" placeholder="请再次输入新密码" autocomplete="new-password" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<style scoped>
.change-pwd-form { padding-top: 8px; }
</style>
