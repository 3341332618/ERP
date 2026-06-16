<template>
  <div class="login-page">
    <el-card class="login-card" shadow="never">
      <h1>ERP管理平台</h1>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="submit">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" clearable />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" placeholder="请输入密码" type="password" show-password clearable />
        </el-form-item>
        <el-button type="primary" class="login-button" :loading="loading" @click="submit">登录</el-button>
      </el-form>
      <div class="seed-tip">演示账号：admin / 123456</div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive({ username: 'admin', password: '123456' })

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function submit() {
  await formRef.value?.validate()
  loading.value = true
  try {
    await auth.login(form.username, form.password)
    ElMessage.success('登录成功')
    router.push('/home')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  background: #eef2f7;
}

.login-card {
  width: 380px;
  border-radius: 6px;
}

h1 {
  margin: 0 0 22px;
  font-size: 24px;
  text-align: center;
}

.login-button {
  width: 100%;
}

.seed-tip {
  margin-top: 12px;
  color: #6b7280;
  font-size: 12px;
  text-align: center;
}
</style>

