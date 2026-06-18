<template>
  <div class="page">
    <el-row :gutter="12">
      <el-col :span="10">
        <div class="table-panel profile-card">
          <h2>个人信息</h2>
          <div class="profile-header">
            <div class="avatar-preview">
              <div class="avatar-title">头像预览</div>
              <el-avatar :size="76" :src="auth.user?.avatar || undefined">{{ avatarFallback }}</el-avatar>
            </div>
            <div class="avatar-actions">
              <el-upload
                :auto-upload="false"
                :show-file-list="false"
                accept=".jpg,.jpeg,.png,image/jpeg,image/png"
                :on-change="handleAvatarChange"
              >
                <el-button type="primary">上传头像</el-button>
              </el-upload>
              <el-button v-if="auth.user?.avatar" @click="removeAvatar">删除头像</el-button>
            </div>
          </div>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="姓名">{{ auth.user?.name }}</el-descriptions-item>
            <el-descriptions-item label="联系电话">{{ auth.user?.phone }}</el-descriptions-item>
            <el-descriptions-item label="所属角色">{{ auth.user?.roleName }}</el-descriptions-item>
            <el-descriptions-item label="创建日期">{{ auth.user?.createTime }}</el-descriptions-item>
          </el-descriptions>
        </div>
      </el-col>
      <el-col :span="10">
        <div class="table-panel profile-card">
          <h2>修改密码</h2>
          <el-form :model="form" label-width="90px">
            <el-form-item label="旧密码"><el-input v-model="form.oldPassword" type="password" show-password /></el-form-item>
            <el-form-item label="新密码"><el-input v-model="form.newPassword" type="password" show-password /></el-form-item>
            <el-form-item label="确认密码"><el-input v-model="form.confirmPassword" type="password" show-password /></el-form-item>
            <el-form-item>
              <el-button type="primary" @click="save">保存</el-button>
              <el-button @click="reset">关闭</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import type { UploadFile } from 'element-plus'
import { changePassword, uploadAvatar } from '../api'
import { useAuthStore } from '../stores/auth'
import { readValidDocumentImage } from '../utils/imageUpload'

const auth = useAuthStore()
const form = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const avatarFallback = computed(() => auth.user?.name?.slice(0, 1) || '用')

async function handleAvatarChange(file: UploadFile) {
  const avatar = await readValidDocumentImage(file.raw)
  if (!avatar) return
  auth.user = await uploadAvatar(avatar)
  ElMessage.success('头像上传成功')
}

async function removeAvatar() {
  auth.user = await uploadAvatar('')
  ElMessage.success('头像已删除')
}

async function save() {
  await changePassword(form)
  ElMessage.success('修改成功，下次登录生效')
  reset()
}

function reset() {
  form.oldPassword = ''
  form.newPassword = ''
  form.confirmPassword = ''
}
</script>

<style scoped>
h2 {
  margin: 0 0 12px;
  font-size: 16px;
}

.profile-card {
  min-height: 300px;
}

.profile-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.avatar-preview {
  width: 132px;
  padding: 14px 12px;
  text-align: center;
  border: 1px solid #e8edf3;
  background: #fafcff;
}

.avatar-title {
  margin-bottom: 10px;
  color: #606266;
  font-size: 12px;
}

.avatar-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex-wrap: wrap;
}
</style>
