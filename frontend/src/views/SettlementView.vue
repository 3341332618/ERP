<template>
  <div class="page">
    <div class="table-panel">
      <div class="toolbar">
        <el-input v-model="keyword" placeholder="请输入结算单号查询" clearable style="width: 260px" />
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="keyword = ''; load()">重置</el-button>
      </div>
      <el-table :data="filteredRows" border empty-text="暂无数据">
        <el-table-column type="index" label="序号" width="70" />
        <el-table-column prop="settlementNo" :label="`${title}单号`" min-width="190" />
        <el-table-column prop="documentType" label="单据类型" />
        <el-table-column prop="amount" label="金额（元）" />
        <el-table-column prop="relatedDocumentNo" label="关联单据号" min-width="190" />
        <el-table-column prop="createTime" label="创建时间" min-width="180" />
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { listSettlement } from '../api'

const route = useRoute()
const rows = ref<any[]>([])
const keyword = ref('')
const direction = computed(() => String(route.params.type) === 'income' ? 'income' : 'expense')
const title = computed(() => direction.value === 'income' ? '收入结算' : '支出结算')
const filteredRows = computed(() => rows.value.filter((row) => !keyword.value || row.settlementNo.includes(keyword.value)))

async function load() {
  rows.value = await listSettlement(direction.value)
}

watch(direction, load)
onMounted(load)
</script>

