<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock } from '@element-plus/icons-vue'
import { login as loginApi } from '../api'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const form = reactive({
  roleSuffix: 'admin',
  password: '123456'
})

const roleOptions = [
  { label: '管理员', suffix: 'admin', description: '基础资料和ERP全模块' },
  { label: '采购专员', suffix: 'purchase_staff', description: '采购入库、采购退货' },
  { label: '仓库专员', suffix: 'warehouse_staff', description: '入库审核、出库审核、库存调拨' },
  { label: '销售专员', suffix: 'sales_staff', description: '销售出库、销售退货' },
  { label: '结算主管', suffix: 'settlement_manager', description: '收入结算、支出结算' }
]

const studentUsername = computed(() => auth.user?.role === 'STUDENT' ? auth.user.username : '')
const studentName = computed(() => auth.user?.name || studentUsername.value || '当前学员')
const targetUsername = computed(() => studentUsername.value ? `${studentUsername.value}_${form.roleSuffix}` : '')

function selectRole(suffix: string) {
  form.roleSuffix = suffix
}

async function submit() {
  if (!studentUsername.value) {
    ElMessage.warning('请先使用学员主账号登录')
    router.push('/login')
    return
  }
  if (!form.password) {
    ElMessage.warning('请输入密码')
    return
  }
  loading.value = true
  try {
    auth.savePrimaryStudentSession()
    const data = await loginApi({ username: targetUsername.value, password: form.password })
    if (data.user.role === 'STUDENT' || !data.user.username.startsWith(`${studentUsername.value}_`)) {
      ElMessage.error('只能登录当前学员的 ERP 子账号')
      return
    }
    auth.applySession(data)
    ElMessage.success('已进入我的 ERP 工作区')
    router.push('/home')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (!auth.user) {
    router.replace('/login')
    return
  }
  if (auth.user.role !== 'STUDENT') {
    router.replace('/home')
  }
})
</script>

<template>
  <main class="student-erp-login">
    <section class="student-erp-card" aria-label="学员ERP工作区登录">
      <div class="student-erp-card__header">
        <span>Student ERP Workspace</span>
        <h2>我的 ERP 工作区</h2>
        <p>{{ studentName }}，请选择岗位并输入密码，系统会自动匹配当前学员的 ERP 子账号。</p>
      </div>

      <div class="role-grid" role="list" aria-label="ERP岗位">
        <button
          v-for="item in roleOptions"
          :key="item.suffix"
          type="button"
          :class="{ active: form.roleSuffix === item.suffix }"
          @click="selectRole(item.suffix)"
        >
          <strong>{{ item.label }}</strong>
          <small>{{ item.description }}</small>
        </button>
      </div>

      <div class="account-preview">
        当前将登录：<strong>{{ targetUsername }}</strong>
      </div>

      <form class="student-erp-form" @submit.prevent="submit">
        <label class="student-erp-field">
          <span>密码</span>
          <div class="student-erp-field__control">
            <input v-model="form.password" type="password" autocomplete="current-password" placeholder="请输入岗位账号密码" />
            <Lock class="student-erp-field__icon" />
          </div>
        </label>
        <button class="student-erp-submit" type="submit" :disabled="loading">
          {{ loading ? '进入中...' : '进入 ERP 工作区' }}
        </button>
      </form>

      <div class="student-erp-links">
        <span>默认密码：123456</span>
        <button type="button" @click="router.push('/competition/reports')">返回测试竞赛</button>
      </div>
    </section>
  </main>
</template>

<style scoped>
.student-erp-login,
.student-erp-login * {
  box-sizing: border-box;
}

.student-erp-login {
  --student-erp-panel-bg: rgba(231, 246, 255, 0.96);
  --student-erp-border: rgba(88, 160, 218, 0.72);
  --student-erp-primary: #2c78b8;
  --student-erp-text: #123c5c;
  --student-erp-muted: rgba(18, 60, 92, 0.68);
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: clamp(20px, 7vw, 92px);
  color: var(--student-erp-text);
  background:
    linear-gradient(90deg, rgba(232, 245, 255, 0.05), rgba(241, 250, 255, 0.18)),
    url("/images/login-current-bg.png") center / cover no-repeat,
    #e8f7ff;
}

.student-erp-card {
  width: min(100%, 440px);
  padding: 28px 26px 24px;
  border: 1px solid var(--student-erp-border);
  border-radius: 24px;
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.82), rgba(199, 229, 250, 0.34)),
    var(--student-erp-panel-bg);
  box-shadow: 0 28px 70px rgba(44, 120, 184, 0.32);
  backdrop-filter: blur(18px);
}

.student-erp-card__header {
  margin-bottom: 20px;
}

.student-erp-card__header span {
  font-size: 12px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--student-erp-primary);
}

.student-erp-card__header h2 {
  margin: 8px 0;
  font-size: 28px;
}

.student-erp-card__header p {
  margin: 0;
  color: var(--student-erp-muted);
  line-height: 1.7;
}

.role-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 16px;
}

.role-grid button {
  border: 1px solid rgba(88, 160, 218, 0.35);
  border-radius: 16px;
  padding: 12px;
  text-align: left;
  color: var(--student-erp-text);
  background: rgba(255, 255, 255, 0.68);
  cursor: pointer;
}

.role-grid button.active {
  border-color: var(--student-erp-primary);
  background: rgba(85, 167, 223, 0.16);
}

.role-grid strong,
.role-grid small {
  display: block;
}

.role-grid small {
  margin-top: 4px;
  color: var(--student-erp-muted);
}

.account-preview {
  margin-bottom: 16px;
  padding: 10px 12px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.58);
  color: var(--student-erp-muted);
}

.account-preview strong {
  color: var(--student-erp-text);
}

.student-erp-form {
  display: grid;
  gap: 16px;
}

.student-erp-field span {
  display: block;
  margin-bottom: 8px;
  font-weight: 700;
}

.student-erp-field__control {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 14px;
  border: 1px solid rgba(88, 160, 218, 0.48);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.7);
}

.student-erp-field__control input {
  min-width: 0;
  flex: 1;
  height: 46px;
  border: 0;
  outline: 0;
  background: transparent;
  color: var(--student-erp-text);
}

.student-erp-field__icon {
  width: 18px;
  color: var(--student-erp-primary);
}

.student-erp-submit {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 48px;
  border: 0;
  border-radius: 16px;
  color: #fff;
  font-weight: 800;
  background: linear-gradient(135deg, #55a7df, var(--student-erp-primary));
  cursor: pointer;
}

.student-erp-submit:disabled {
  opacity: 0.65;
  cursor: wait;
}

.student-erp-links {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-top: 16px;
  color: var(--student-erp-muted);
  font-size: 13px;
}

.student-erp-links button {
  border: 0;
  background: transparent;
  color: var(--student-erp-primary);
  cursor: pointer;
}
</style>
