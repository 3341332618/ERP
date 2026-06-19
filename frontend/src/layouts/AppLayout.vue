<template>
  <el-container class="layout">
    <el-aside width="200px" class="aside">
      <div class="logo">
        <span class="brand-mark">企</span>
        <span>ERP管理平台</span>
      </div>
      <el-menu router :default-active="$route.path" background-color="#21384a" text-color="#d1d5db" active-text-color="#ffffff">
        <el-menu-item index="/home">
          <el-icon><House /></el-icon>
          <span>首页看板</span>
        </el-menu-item>
        <el-sub-menu v-for="menu in auth.menus" :key="menu.title" :index="menu.title">
          <template #title>
            <el-icon><Menu /></el-icon>
            <span>{{ menu.title }}</span>
          </template>
          <el-menu-item v-for="child in menu.children" :key="child.path" :index="child.path">
            {{ child.title }}
          </el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header" height="52px">
        <div class="breadcrumb">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item v-for="item in breadcrumbNodes" :key="item">{{ item }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="top-actions">
          <el-popover placement="bottom" width="360" trigger="click" @show="loadMessages">
            <template #reference>
              <el-button text>
                <el-icon><Bell /></el-icon>
                消息
              </el-button>
            </template>
            <div class="message-title">系统消息</div>
            <el-empty v-if="messages.length === 0" description="暂无消息内容" />
            <div v-for="item in displayedMessages" :key="item.id" class="message-item">{{ item.content }}</div>
            <el-button v-if="messages.length > 10 && !messageExpanded" text type="primary" @click="messageExpanded = true">
              展开全部消息
            </el-button>
          </el-popover>
          <el-dropdown @command="handleCommand">
            <span class="user-entry">
              <el-avatar :size="34" :src="auth.user?.avatar || undefined">{{ avatarFallback }}</el-avatar>
              <span>{{ auth.user?.name }}（{{ auth.user?.roleName }}）</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <div class="tab-strip">
        <div class="tab-item">{{ routeLabel }}</div>
      </div>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Bell, House, Menu } from '@element-plus/icons-vue'
import { fetchMessages } from '../api'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()
const messages = ref<any[]>([])
const messageExpanded = ref(false)

const matchedMenu = computed(() => {
  for (const menu of auth.menus) {
    const child = menu.children?.find((item) => item.path === route.path)
    if (child) {
      return { parent: menu.title, child: child.title }
    }
    if (menu.path === route.path) {
      return { parent: menu.title, child: menu.title }
    }
  }
  return null
})
const routeLabel = computed(() => matchedMenu.value?.child || String(route.name || '首页看板'))
const breadcrumbNodes = computed(() => {
  if (matchedMenu.value) {
    return matchedMenu.value.parent === matchedMenu.value.child
      ? [matchedMenu.value.child]
      : [matchedMenu.value.parent, matchedMenu.value.child]
  }
  return [String(route.name || '首页看板')]
})
const avatarFallback = computed(() => auth.user?.name?.slice(0, 1) || '用')
const displayedMessages = computed(() => messageExpanded.value ? messages.value : messages.value.slice(0, 10))

async function loadMessages() {
  messages.value = await fetchMessages()
  messageExpanded.value = false
}

function handleCommand(command: string) {
  if (command === 'profile') router.push('/profile')
  if (command === 'logout') {
    auth.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.layout {
  min-height: 100vh;
}

.aside {
  background: #21384a;
  box-shadow: 2px 0 8px rgba(15, 23, 42, 0.18);
}

.logo {
  height: 52px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 18px;
  color: #fff;
  font-weight: 700;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.brand-mark {
  width: 24px;
  height: 24px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #ffffff;
  color: #0f8fe8;
  font-size: 11px;
  font-weight: 800;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  padding: 0 18px;
}

.top-actions {
  display: flex;
  align-items: center;
  gap: 18px;
}

.user-entry {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: #374151;
}

.tab-strip {
  height: 32px;
  display: flex;
  align-items: flex-end;
  padding-left: 16px;
  background: #fff;
  border-bottom: 1px solid #d8dee6;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.08);
}

.tab-item {
  height: 28px;
  min-width: 98px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 18px;
  background: #1296db;
  color: #fff;
  font-size: 13px;
}

.message-title {
  font-weight: 700;
  margin-bottom: 8px;
}

.message-item {
  padding: 8px 0;
  border-bottom: 1px solid #eef2f7;
  font-size: 13px;
}
</style>
