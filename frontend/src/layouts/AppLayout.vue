<template>
  <el-container class="layout">
    <el-aside width="230px" class="aside">
      <div class="logo">ERP管理平台</div>
      <el-menu router :default-active="$route.path" background-color="#1f2937" text-color="#d1d5db" active-text-color="#ffffff">
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
      <el-header class="header">
        <div class="breadcrumb">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>当前位置</el-breadcrumb-item>
            <el-breadcrumb-item>{{ routeLabel }}</el-breadcrumb-item>
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
            <div v-for="item in messages" :key="item.id" class="message-item">{{ item.content }}</div>
          </el-popover>
          <el-dropdown @command="handleCommand">
            <span class="user-entry">{{ auth.user?.name }}（{{ auth.user?.roleName }}）</span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
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

const routeLabel = computed(() => String(route.name || '首页看板'))

async function loadMessages() {
  messages.value = await fetchMessages()
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
  background: #1f2937;
}

.logo {
  height: 56px;
  display: flex;
  align-items: center;
  padding: 0 18px;
  color: #fff;
  font-weight: 700;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
}

.top-actions {
  display: flex;
  align-items: center;
  gap: 18px;
}

.user-entry {
  cursor: pointer;
  color: #374151;
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

