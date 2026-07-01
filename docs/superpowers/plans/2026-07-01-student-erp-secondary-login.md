# Student ERP Secondary Login Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a post-login student ERP workspace login where a student selects a role and enters a password instead of typing a full `student01_*` account.

**Architecture:** Keep the first login on `/login`. Add `/student-erp-login` as a protected route that is only available to a logged-in `STUDENT` main account. The secondary page calls `/auth/login` directly with an auto-composed username, validates the returned username belongs to the current student, then commits the ERP subaccount session to the auth store.

**Tech Stack:** Vue 3, Vue Router, Pinia, Element Plus, TypeScript, Vitest file-content tests, existing Spring Boot auth API.

---

## File Structure

- Modify `frontend/src/stores/auth.ts`
  - Add a reusable `applySession()` action so the secondary login page can validate a raw login response before committing it.
  - Keep existing `auth.login(username, password)` behavior, but return the login data after committing it.
- Modify `frontend/src/router/index.ts`
  - Register `/student-erp-login`.
  - Protect it so only a logged-in `STUDENT` main account can open it.
- Modify `frontend/src/views/LoginView.vue`
  - Redirect `STUDENT` main accounts to `/competition/reports` after first login.
- Create `frontend/src/views/StudentErpLoginView.vue`
  - Render the secondary login page.
  - Show current student workspace name.
  - Let the user select one of four roles and enter only the password.
  - Compose `studentUsername + '_' + suffix` internally.
  - Commit the session only after ownership validation passes.
- Modify `frontend/src/views/CompetitionView.vue`
  - Add `进入我的 ERP 工作区` button for `STUDENT` main accounts.
- Modify `frontend/src/tests/chinese-ui.spec.ts`
  - Add static coverage for route, page text, role suffix mapping, auto username composition, and competition entry button.
- Modify `README.md`, `README.en.md`, `docs/operation-manual.md`
  - Document the first login and secondary ERP workspace login flow.

---

### Task 1: Add tests for secondary ERP login flow

**Files:**
- Modify: `frontend/src/tests/chinese-ui.spec.ts`
- Expected new file later: `frontend/src/views/StudentErpLoginView.vue`

- [ ] **Step 1: Write failing tests**

Add these assertions inside the existing `测试竞赛界面覆盖缺陷发布、学员报告和排行榜` test:

```ts
expect(competitionView).toContain('进入我的 ERP 工作区')
```

Add a new test near the existing login-page tests:

```ts
it('学员 ERP 工作区二次登录通过岗位自动匹配子账号', () => {
  const router = readFileSync(join(process.cwd(), 'src/router/index.ts'), 'utf8')
  const loginView = readFileSync(join(process.cwd(), 'src/views/LoginView.vue'), 'utf8')
  const studentErpLoginView = readFileSync(join(process.cwd(), 'src/views/StudentErpLoginView.vue'), 'utf8')

  expect(router).toContain("path: '/student-erp-login'")
  expect(router).toContain("auth.user?.role !== 'STUDENT'")
  expect(loginView).toContain("auth.user?.role === 'STUDENT'")
  expect(loginView).toContain("router.push('/competition/reports')")
  expect(studentErpLoginView).toContain('我的 ERP 工作区')
  expect(studentErpLoginView).toContain('请选择岗位并输入密码')
  expect(studentErpLoginView).toContain('采购专员')
  expect(studentErpLoginView).toContain('仓库专员')
  expect(studentErpLoginView).toContain('销售专员')
  expect(studentErpLoginView).toContain('结算主管')
  expect(studentErpLoginView).toContain("suffix: 'purchase_staff'")
  expect(studentErpLoginView).toContain("suffix: 'warehouse_staff'")
  expect(studentErpLoginView).toContain("suffix: 'sales_staff'")
  expect(studentErpLoginView).toContain("suffix: 'settlement_manager'")
  expect(studentErpLoginView).toContain('targetUsername')
  expect(studentErpLoginView).toContain('`${studentUsername.value}_${form.roleSuffix}`')
  expect(studentErpLoginView).toContain('只能登录当前学员的 ERP 子账号')
  expect(studentErpLoginView).toContain('startsWith(`${studentUsername.value}_`)')
})
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
cd frontend
npm.cmd test -- --run src/tests/chinese-ui.spec.ts
```

Expected: FAIL because `StudentErpLoginView.vue` and `/student-erp-login` do not exist yet, and `CompetitionView.vue` does not contain `进入我的 ERP 工作区`.

---

### Task 2: Make auth store support validated secondary login

**Files:**
- Modify: `frontend/src/stores/auth.ts`

- [ ] **Step 1: Implement session application action**

Replace the import and actions with this structure:

```ts
import { defineStore } from 'pinia'
import { currentUser, login as loginApi, type CurrentUser, type MenuNode } from '../api'

type LoginSession = Awaited<ReturnType<typeof loginApi>>

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('ERP_TOKEN') || '',
    user: null as CurrentUser | null,
    menus: [] as MenuNode[]
  }),
  actions: {
    applySession(data: LoginSession) {
      this.token = data.token
      this.user = data.user
      this.menus = data.menus
      localStorage.setItem('ERP_TOKEN', data.token)
    },
    async login(username: string, password: string) {
      const data = await loginApi({ username, password })
      this.applySession(data)
      return data
    },
    async loadCurrentUser() {
      if (!this.token) return
      const data = await currentUser()
      this.user = data.user
      this.menus = data.menus
    },
    logout() {
      this.token = ''
      this.user = null
      this.menus = []
      localStorage.removeItem('ERP_TOKEN')
    }
  }
})
```

- [ ] **Step 2: Run type-aware build later**

No standalone test is needed for this file because it is covered by the final `npm.cmd run build`.

---

### Task 3: Route students to the testing side after first login

**Files:**
- Modify: `frontend/src/views/LoginView.vue`

- [ ] **Step 1: Update first-login redirect**

Change the success branch in `submit()` to:

```ts
await auth.login(form.username.trim(), form.password)
ElMessage.success('登录成功')
if (auth.user?.role === 'STUDENT') {
  router.push('/competition/reports')
} else {
  router.push('/home')
}
```

- [ ] **Step 2: Keep existing visual style unchanged**

Do not change the existing CSS variables:

```css
--login-panel-bg: rgba(231, 246, 255, 0.96);
background:
  linear-gradient(90deg, rgba(232, 245, 255, 0.02), rgba(241, 250, 255, 0.1) 52%, rgba(244, 251, 255, 0.22)),
  url("/images/login-current-bg.png") center / cover no-repeat,
  #e8f7ff;
```

---

### Task 4: Add protected `/student-erp-login` route

**Files:**
- Modify: `frontend/src/router/index.ts`

- [ ] **Step 1: Register route**

Add the route after `/login` and before the app layout route:

```ts
{ path: '/student-erp-login', name: '学员ERP工作区登录', component: () => import('../views/StudentErpLoginView.vue') },
```

- [ ] **Step 2: Protect route after loading current user**

Update `beforeEach` to keep `/login` public, load current user for other routes, and reject non-student users from the secondary login page:

```ts
router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (to.path === '/login') return true
  if (!auth.token) return '/login'
  if (!auth.user) {
    try {
      await auth.loadCurrentUser()
    } catch {
      auth.logout()
      return '/login'
    }
  }
  if (to.path === '/student-erp-login' && auth.user?.role !== 'STUDENT') {
    return '/home'
  }
  return true
})
```

---

### Task 5: Build secondary student ERP login page

**Files:**
- Create: `frontend/src/views/StudentErpLoginView.vue`

- [ ] **Step 1: Create complete page**

Create `frontend/src/views/StudentErpLoginView.vue` with this content:

```vue
<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Briefcase, Lock } from '@element-plus/icons-vue'
import { login as loginApi } from '../api'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const form = reactive({
  roleSuffix: 'purchase_staff',
  password: '123456'
})

const roleOptions = [
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
          <Briefcase />
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
```

- [ ] **Step 2: Validate that secondary login does not overwrite student token before validation**

Confirm the page imports `login as loginApi` from `../api` and only calls `auth.applySession(data)` after:

```ts
if (data.user.role === 'STUDENT' || !data.user.username.startsWith(`${studentUsername.value}_`)) {
  ElMessage.error('只能登录当前学员的 ERP 子账号')
  return
}
auth.applySession(data)
```

---

### Task 6: Add entry button in testing competition page

**Files:**
- Modify: `frontend/src/views/CompetitionView.vue`

- [ ] **Step 1: Add router and auth imports**

Change imports:

```ts
import { useRoute, useRouter } from 'vue-router'
```

Add auth store import:

```ts
import { useAuthStore } from '../stores/auth'
```

Add instances after `const route = useRoute()`:

```ts
const router = useRouter()
const auth = useAuthStore()
```

- [ ] **Step 2: Add student-main computed and navigation function**

Add after the page type computed values:

```ts
const isStudentMainAccount = computed(() => auth.user?.role === 'STUDENT')

function enterStudentErpWorkspace() {
  router.push('/student-erp-login')
}
```

- [ ] **Step 3: Add button**

In the `.header-actions` block, before the submit report button, add:

```vue
<el-button v-if="isStudentMainAccount" type="success" @click="enterStudentErpWorkspace">
  进入我的 ERP 工作区
</el-button>
```

---

### Task 7: Update docs for selected-role secondary login

**Files:**
- Modify: `README.md`
- Modify: `README.en.md`
- Modify: `docs/operation-manual.md`

- [ ] **Step 1: Update Chinese README**

In the testing competition description, make the flow explicit:

```md
学员先使用主账号（如 `student01`）登录测试竞赛端，点击“进入我的 ERP 工作区”后选择采购、仓库、销售或结算岗位并输入密码；系统会自动匹配 `student01_*` 子账号进入该学员自己的 ERP 工作区。学员不需要手写 `student01` 前缀。
```

Update the demo account note to say:

```md
`student01_*` 是系统自动生成的 ERP 子账号。实际二次登录时只需选择岗位和输入密码，前端会自动拼接账号。
```

- [ ] **Step 2: Update English README**

Add this flow text:

```md
Students first sign in with the main account such as `student01`. From the testing area they click "Enter my ERP workspace", choose a role, and enter the password. The frontend automatically uses the matching `student01_*` ERP subaccount, so students do not type the prefix manually.
```

- [ ] **Step 3: Update operation manual section 8.3**

Replace the old instruction that directly logs into `student01_purchase_staff` with this flow:

```md
1. 登录 `student01 / 123456`，进入“我的缺陷报告”或“我的提交文件”页面。
2. 点击“进入我的 ERP 工作区”。
3. 在二次登录页选择岗位：
   - 采购专员：系统自动使用 `student01_purchase_staff`
   - 仓库专员：系统自动使用 `student01_warehouse_staff`
   - 销售专员：系统自动使用 `student01_sales_staff`
   - 结算主管：系统自动使用 `student01_settlement_manager`
4. 输入密码 `123456` 后进入对应岗位的 ERP 工作区。
5. 学员在这套独立 ERP 工作区里自行探索异常行为。系统不会提示当前发布了哪些缺陷。
6. 发现异常后，退出 ERP 子账号，重新登录 `student01 / 123456`，提交缺陷报告或测试文件。
```

---

### Task 8: Verify and commit

**Files:**
- All files modified above.

- [ ] **Step 1: Run focused frontend test**

Run:

```powershell
cd frontend
npm.cmd test -- --run src/tests/chinese-ui.spec.ts
```

Expected: `10` or more tests pass, including the new secondary login test.

- [ ] **Step 2: Run frontend build**

Run:

```powershell
cd frontend
npm.cmd run build
```

Expected: Vite build succeeds. Existing chunk-size warnings are acceptable.

- [ ] **Step 3: Check docs for stale two-entry language**

Run:

```powershell
cd ..
rg -n "两个公开登录入口|/student-login|手写 student01|输入 student01_purchase_staff|输入完整子账号" README.md README.en.md docs/operation-manual.md frontend/src
```

Expected: No stale user-facing instructions that require typing the full student ERP account. The spec may still contain “不做两个公开登录入口” as a non-goal; that is acceptable.

- [ ] **Step 4: Check patch formatting**

Run:

```powershell
git diff --check
```

Expected: exit code `0`; CRLF warnings are acceptable on Windows.

- [ ] **Step 5: Commit**

Stage only the intended files:

```powershell
git add frontend/src/stores/auth.ts frontend/src/router/index.ts frontend/src/views/LoginView.vue frontend/src/views/StudentErpLoginView.vue frontend/src/views/CompetitionView.vue frontend/src/tests/chinese-ui.spec.ts README.md README.en.md docs/operation-manual.md docs/superpowers/specs/2026-07-01-student-erp-secondary-login-design.md docs/superpowers/plans/2026-07-01-student-erp-secondary-login.md
git commit -m "Add student ERP secondary login"
```

Do not stage `434494df3d3cbac5e50889f642c17012.png`.

---

## Self-Review

- Spec coverage: The plan implements role selection, automatic username composition, current-student validation, `/student-erp-login`, first-login student redirect, testing-page entry, documentation, and verification.
- Placeholder scan: No unresolved placeholder markers or vague “handle later” items are intentionally left.
- Type consistency: The plan uses `CurrentUser.role`, `CurrentUser.username`, `auth.applySession(data)`, and the existing `login()` API response consistently.
