<template>
  <div class="page">
    <div class="table-panel">
      <div class="toolbar">
        <el-button type="primary" @click="load">刷新</el-button>
      </div>
      <el-table :data="rows" border empty-text="暂无数据">
        <el-table-column type="index" label="序号" width="70" />
        <el-table-column prop="warehouseId" label="仓库标识" />
        <el-table-column prop="productId" label="商品标识" />
        <el-table-column prop="actualQuantity" label="实际库存数量" />
        <el-table-column prop="availableQuantity" label="可用库存数量" />
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { listStock } from '../api'

const rows = ref<any[]>([])
async function load() {
  rows.value = await listStock()
}
onMounted(load)
</script>

