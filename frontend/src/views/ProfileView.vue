<template>
  <div class="page">
    <el-row :gutter="12">
      <el-col :span="10">
        <div class="table-panel">
          <h2>个人信息</h2>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="姓名">{{ auth.user?.name }}</el-descriptions-item>
            <el-descriptions-item label="联系电话">{{ auth.user?.phone }}</el-descriptions-item>
            <el-descriptions-item label="所属角色">{{ auth.user?.roleName }}</el-descriptions-item>
            <el-descriptions-item label="创建日期">{{ auth.user?.createTime }}</el-descriptions-item>
          </el-descriptions>
        </div>
      </el-col>
      <el-col :span="10">
        <div class="table-panel">
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
import { reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { changePassword } from '../api'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const form = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

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
</style>

