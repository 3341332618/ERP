<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const isShaking = ref(false)
const form = reactive({ username: 'admin', password: '123456' })
const demoAccounts = [
  { label: '管理员', description: '全模块', username: 'admin' },
  { label: '学员', description: '测试端', username: 'student01' },
  { label: '终极管理', description: '评阅端', username: 'superadmin' }
]

function triggerShake() {
  isShaking.value = false
  window.requestAnimationFrame(() => {
    isShaking.value = true
    window.setTimeout(() => {
      isShaking.value = false
    }, 420)
  })
}

function useDemoAccount(username: string) {
  form.username = username
  form.password = '123456'
}

async function submit() {
  if (!form.username.trim() || !form.password) {
    triggerShake()
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    await auth.login(form.username.trim(), form.password)
    ElMessage.success('登录成功')
    if (auth.user?.role === 'STUDENT') {
      router.push('/competition/reports')
    } else {
      router.push('/home')
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-card" :class="{ 'is-shaking': isShaking }" aria-label="登录表单">
      <div class="login-card__header">
        <span>Login</span>
        <h2>ERP管理平台</h2>
        <p>输入账号进入对应角色工作台</p>
      </div>

      <div class="role-tabs" role="tablist" aria-label="演示账号">
        <button
          v-for="item in demoAccounts"
          :key="item.username"
          type="button"
          :class="{ active: form.username === item.username }"
          @click="useDemoAccount(item.username)"
        >
          <strong>{{ item.label }}</strong>
          <small>{{ item.description }}</small>
        </button>
      </div>

      <form class="login-form" @submit.prevent="submit">
        <label class="login-field">
          <span>用户名</span>
          <div class="login-field__control">
            <input v-model.trim="form.username" type="text" autocomplete="username" placeholder="请输入用户名" />
            <User class="login-field__icon" />
          </div>
        </label>
        <label class="login-field">
          <span>密码</span>
          <div class="login-field__control">
            <input v-model="form.password" type="password" autocomplete="current-password" placeholder="请输入密码" />
            <Lock class="login-field__icon" />
          </div>
        </label>
        <button class="login-submit" type="submit" :disabled="loading">
          {{ loading ? '登录中...' : '进入系统' }}
        </button>
      </form>

      <div class="login-links">
        <span>默认密码：123456</span>
        <span>演示账号可直接切换</span>
      </div>
    </section>

    <section class="login-copy" aria-label="系统介绍">
      <span class="login-badge">ERP协同管理平台</span>
      <h1>让进销存流程更清晰</h1>
      <p>采购、库存、销售、结算、测试竞赛一体化闭环演示</p>
      <div class="login-copy__meta">
        <span>业务闭环</span>
        <span>库存联动</span>
        <span>缺陷训练</span>
      </div>
    </section>
  </main>
</template>

<style scoped>
.login-page,
.login-page * {
  box-sizing: border-box;
}

.login-page {
  --login-panel-bg: rgba(231, 246, 255, 0.96);
  --login-panel-border: rgba(88, 160, 218, 0.72);
  --login-primary: #2c78b8;
  --login-primary-dark: #15598e;
  --login-text: #123c5c;
  --login-muted: rgba(18, 60, 92, 0.68);
  position: relative;
  display: grid;
  min-height: 100vh;
  grid-template-columns: minmax(0, 1fr) minmax(300px, 380px);
  align-items: center;
  gap: clamp(24px, 7vw, 96px);
  padding: clamp(20px, 7vw, 92px);
  overflow: hidden;
  color: var(--login-text);
  background:
    linear-gradient(90deg, rgba(232, 245, 255, 0.02), rgba(241, 250, 255, 0.1) 52%, rgba(244, 251, 255, 0.22)),
    url("/images/login-current-bg.png") center / cover no-repeat,
    #e8f7ff;
}

.login-page::before {
  content: "";
  position: absolute;
  inset: 0;
  pointer-events: none;
  background: radial-gradient(circle at 80% 52%, rgba(255, 255, 255, 0.2), transparent 30%);
}

.login-page::after {
  content: "";
  position: absolute;
  inset: 12px;
  border: 1px solid rgba(255, 255, 255, 0.36);
  border-radius: 28px;
  pointer-events: none;
}

.login-copy,
.login-card {
  position: relative;
  z-index: 1;
}

.login-card {
  grid-column: 2;
  grid-row: 1;
  justify-self: center;
  width: min(100%, 356px);
  padding: 27px 24px 23px;
  border: 1px solid var(--login-panel-border);
  border-radius: 24px;
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.78), rgba(199, 229, 250, 0.28)),
    var(--login-panel-bg);
  box-shadow:
    0 28px 70px rgba(44, 120, 184, 0.32),
    0 8px 24px rgba(13, 71, 116, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.76);
  backdrop-filter: blur(18px);
  animation: login-card-in 0.58s ease both;
}

.login-card.is-shaking {
  animation:
    login-card-in 0.58s ease both,
    login-shake 0.38s ease both;
}

.login-card__header {
  text-align: center;
}

.login-card__header span {
  color: rgba(44, 120, 184, 0.86);
  font-size: 14px;
  font-weight: 800;
  letter-spacing: 0.02em;
}

.login-card__header h2 {
  margin: 10px 0 8px;
  color: var(--login-text);
  font-size: 24px;
  font-weight: 820;
  line-height: 1.28;
}

.login-card__header p {
  margin: 0;
  color: var(--login-muted);
  font-size: 13px;
  font-weight: 650;
}

.role-tabs {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 5px;
  margin: 22px 0 18px;
  padding: 4px;
  border: 1px solid rgba(132, 190, 238, 0.42);
  border-radius: 999px;
  background: rgba(211, 237, 255, 0.62);
}

.role-tabs button {
  min-width: 0;
  min-height: 36px;
  padding: 6px 7px;
  border: 0;
  border-radius: 999px;
  color: rgba(18, 60, 92, 0.68);
  background: transparent;
  cursor: pointer;
  transition:
    transform 0.2s ease,
    color 0.2s ease,
    background 0.2s ease,
    box-shadow 0.2s ease;
}

.role-tabs button.active {
  color: var(--login-primary-dark);
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 10px 22px rgba(44, 120, 184, 0.16);
}

.role-tabs button:hover {
  transform: translateY(-1px);
}

.role-tabs strong {
  display: block;
  overflow-wrap: anywhere;
  font-size: 12px;
  font-weight: 820;
}

.role-tabs small {
  display: none;
}

.login-form {
  display: grid;
  gap: 13px;
}

.login-field {
  display: grid;
  gap: 7px;
}

.login-field span {
  padding-left: 4px;
  color: rgba(18, 60, 92, 0.8);
  font-size: 12px;
  font-weight: 760;
}

.login-field__control {
  display: flex;
  align-items: center;
  height: 43px;
  padding: 0 13px 0 15px;
  border: 1px solid rgba(132, 190, 238, 0.58);
  border-radius: 999px;
  background: rgba(242, 250, 255, 0.74);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.78);
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease,
    background 0.2s ease;
}

.login-field__control:focus-within {
  border-color: rgba(44, 120, 184, 0.68);
  background: rgba(255, 255, 255, 0.88);
  box-shadow:
    0 0 0 4px rgba(44, 120, 184, 0.12),
    inset 0 1px 0 rgba(255, 255, 255, 0.86);
}

.login-field__control input {
  width: 100%;
  min-width: 0;
  height: 100%;
  border: 0;
  outline: 0;
  color: var(--login-text);
  background: transparent;
  font-size: 13px;
  font-weight: 650;
}

.login-field__control input::placeholder {
  color: rgba(18, 60, 92, 0.44);
}

.login-field__icon {
  flex: 0 0 16px;
  width: 16px;
  height: 16px;
  color: rgba(44, 120, 184, 0.72);
}

.login-submit {
  height: 42px;
  margin-top: 2px;
  border: 0;
  border-radius: 999px;
  color: #fff;
  background: linear-gradient(135deg, #55a7df, var(--login-primary));
  box-shadow: 0 14px 28px rgba(44, 120, 184, 0.26);
  cursor: pointer;
  font-size: 13px;
  font-weight: 850;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    background 0.2s ease;
}

.login-submit:hover {
  background: linear-gradient(135deg, #6bb8ec, #1f6fae);
  box-shadow: 0 18px 34px rgba(44, 120, 184, 0.34);
  transform: translateY(-2px);
}

.login-submit:active {
  transform: translateY(0);
}

.login-submit:disabled {
  cursor: not-allowed;
  opacity: 0.72;
  transform: none;
}

.login-links {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-top: 12px;
  color: rgba(18, 60, 92, 0.66);
  font-size: 12px;
  font-weight: 720;
}

.login-copy {
  display: none;
  grid-column: 1;
  grid-row: 1;
  max-width: 520px;
  align-self: end;
  justify-self: start;
  padding-bottom: clamp(28px, 8vh, 86px);
  text-shadow: 0 18px 34px rgba(18, 10, 75, 0.4);
}

.login-badge {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 0 13px;
  border: 1px solid rgba(255, 255, 255, 0.28);
  border-radius: 999px;
  color: rgba(255, 255, 255, 0.82);
  background: rgba(255, 255, 255, 0.12);
  font-size: 13px;
  font-weight: 760;
  backdrop-filter: blur(10px);
}

.login-copy h1 {
  max-width: 460px;
  margin: 22px 0 12px;
  color: #fff;
  font-size: clamp(36px, 5vw, 58px);
  font-weight: 850;
  line-height: 1.08;
}

.login-copy p {
  margin: 0;
  color: rgba(255, 255, 255, 0.78);
  font-size: clamp(16px, 1.8vw, 20px);
  font-weight: 650;
}

.login-copy__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 26px;
}

.login-copy__meta span {
  padding: 8px 12px;
  border: 1px solid rgba(255, 255, 255, 0.22);
  border-radius: 999px;
  color: rgba(255, 255, 255, 0.8);
  background: rgba(255, 255, 255, 0.11);
  font-size: 13px;
  font-weight: 720;
  backdrop-filter: blur(10px);
}

@keyframes login-card-in {
  from {
    opacity: 0;
    transform: translateY(16px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes login-shake {
  0%,
  100% {
    transform: translateX(0);
  }

  20%,
  60% {
    transform: translateX(-8px);
  }

  40%,
  80% {
    transform: translateX(8px);
  }
}

@media (max-width: 900px) {
  .login-page {
    grid-template-columns: 1fr;
    align-content: center;
    justify-items: center;
    gap: 22px;
    padding: 22px;
    background-position: center;
  }

  .login-card,
  .login-copy {
    grid-column: 1;
    grid-row: auto;
  }

  .login-copy {
    order: -1;
    max-width: 420px;
    padding-bottom: 0;
    text-align: center;
  }

  .login-copy h1 {
    margin-top: 16px;
    font-size: 34px;
  }

  .login-copy__meta {
    justify-content: center;
  }
}

@media (max-width: 520px) {
  .login-page {
    padding: 18px;
  }

  .login-page::after {
    inset: 8px;
    border-radius: 22px;
  }

  .login-card {
    width: 100%;
    padding: 24px 18px 20px;
    border-radius: 22px;
  }

  .login-card__header h2 {
    font-size: 21px;
  }

  .login-copy h1 {
    font-size: 30px;
  }

  .login-copy p {
    font-size: 15px;
  }

  .login-copy__meta {
    display: none;
  }

  .role-tabs {
    grid-template-columns: 1fr;
    border-radius: 20px;
  }

  .role-tabs button {
    min-height: 34px;
    border-radius: 16px;
  }
}
</style>
