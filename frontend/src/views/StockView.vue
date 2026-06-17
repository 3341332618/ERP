<template>
  <div class="page">
    <div class="table-panel">
      <div class="toolbar">
        <el-input v-model="keyword" placeholder="请输入仓库编号/名称或商品编号/名称查询" clearable style="width: 320px" />
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="keyword = ''; load()">重置</el-button>
      </div>
      <el-table :data="filteredRows" border empty-text="暂无数据">
        <el-table-column type="index" label="序号" width="70" />
        <el-table-column prop="warehouseCode" label="仓库编号" />
        <el-table-column prop="warehouseName" label="仓库名称" />
        <el-table-column label="商品图片" width="100">
          <template #default="{ row }">
            <el-image
              v-if="row.imageData"
              :src="row.imageData"
              class="stock-thumb"
              fit="cover"
              :preview-src-list="[row.imageData]"
              preview-teleported
            />
            <span v-else>无图片</span>
          </template>
        </el-table-column>
        <el-table-column prop="productCode" label="商品编号" />
        <el-table-column prop="productName" label="商品名称" />
        <el-table-column prop="categoryName" label="商品分类" />
        <el-table-column prop="brandName" label="商品品牌" />
        <el-table-column prop="unitName" label="商品单位" />
        <el-table-column prop="actualQuantity" label="实际库存数量" />
        <el-table-column prop="availableQuantity" label="可用库存数量" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="viewWarehouse(row)">查看仓库</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="detailVisible" title="仓库明细" width="620px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="仓库编号">{{ current?.warehouseCode }}</el-descriptions-item>
        <el-descriptions-item label="仓库名称">{{ current?.warehouseName }}</el-descriptions-item>
        <el-descriptions-item label="商品编号">{{ current?.productCode }}</el-descriptions-item>
        <el-descriptions-item label="商品名称">{{ current?.productName }}</el-descriptions-item>
        <el-descriptions-item label="商品分类">{{ current?.categoryName }}</el-descriptions-item>
        <el-descriptions-item label="商品品牌">{{ current?.brandName }}</el-descriptions-item>
        <el-descriptions-item label="商品单位">{{ current?.unitName }}</el-descriptions-item>
        <el-descriptions-item label="实际库存数量">{{ current?.actualQuantity }}</el-descriptions-item>
        <el-descriptions-item label="可用库存数量">{{ current?.availableQuantity }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { listStock } from '../api'

const rows = ref<any[]>([])
const keyword = ref('')
const detailVisible = ref(false)
const current = ref<any>()

const filteredRows = computed(() => rows.value.filter((row) => {
  if (!keyword.value) return true
  return [row.warehouseCode, row.warehouseName, row.productCode, row.productName]
    .some((value) => String(value || '').includes(keyword.value))
}))

async function load() {
  rows.value = await listStock()
}

function viewWarehouse(row: any) {
  current.value = row
  detailVisible.value = true
}

onMounted(load)
</script>

<style scoped>
.stock-thumb {
  width: 56px;
  height: 56px;
  border-radius: 6px;
}
</style>
