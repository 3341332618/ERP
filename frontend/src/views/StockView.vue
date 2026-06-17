<template>
  <div class="page">
    <div class="table-panel">
      <div class="toolbar">
        <el-input v-model="keyword" placeholder="请输入商品编号/名称查询" clearable style="width: 240px" />
        <span class="filter-label">商品分类</span>
        <el-select v-model="categoryName" placeholder="请选择" clearable style="width: 240px">
          <el-option v-for="item in categoryOptions" :key="item" :label="item" :value="item" />
        </el-select>
        <span class="filter-label">商品品牌</span>
        <el-select v-model="brandName" placeholder="请选择" clearable style="width: 240px">
          <el-option v-for="item in brandOptions" :key="item" :label="item" :value="item" />
        </el-select>
      </div>
      <div class="toolbar">
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
      <el-table :data="pagedRows" border empty-text="暂无数据">
        <el-table-column type="index" label="序号" width="70" />
        <el-table-column prop="productCode" label="商品编号" />
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
        <el-table-column prop="productName" label="商品名称" />
        <el-table-column prop="categoryName" label="商品分类" />
        <el-table-column prop="brandName" label="商品品牌" />
        <el-table-column prop="unitName" label="商品单位" />
        <el-table-column prop="warehouseName" label="仓库" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="viewWarehouse(row)">查看仓库</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-bar">
        <span>共 {{ filteredRows.length }} 条</span>
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 30, 50]"
          layout="sizes, prev, pager, next, jumper"
          :total="filteredRows.length"
          small
        />
      </div>
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
import { computed, onMounted, ref, watch } from 'vue'
import { listStock } from '../api'

const rows = ref<any[]>([])
const keyword = ref('')
const categoryName = ref('')
const brandName = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const detailVisible = ref(false)
const current = ref<any>()

const categoryOptions = computed(() => [...new Set(rows.value.map((row) => row.categoryName).filter(Boolean))])
const brandOptions = computed(() => [...new Set(rows.value.map((row) => row.brandName).filter(Boolean))])
const filteredRows = computed(() => rows.value.filter((row) => {
  const matchKeyword = !keyword.value || [row.productCode, row.productName]
    .some((value) => String(value || '').includes(keyword.value))
  const matchCategory = !categoryName.value || row.categoryName === categoryName.value
  const matchBrand = !brandName.value || row.brandName === brandName.value
  return matchKeyword && matchCategory && matchBrand
}))
const pagedRows = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredRows.value.slice(start, start + pageSize.value)
})

async function load() {
  rows.value = await listStock()
  currentPage.value = 1
}

function reset() {
  keyword.value = ''
  categoryName.value = ''
  brandName.value = ''
  load()
}

function viewWarehouse(row: any) {
  current.value = row
  detailVisible.value = true
}

onMounted(load)
watch([keyword, categoryName, brandName, pageSize], () => {
  currentPage.value = 1
})
</script>

<style scoped>
.stock-thumb {
  width: 56px;
  height: 56px;
  border-radius: 6px;
}

.filter-label {
  font-weight: 700;
  color: #1f2d3d;
}

.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 14px;
  padding-top: 14px;
  color: #606266;
}
</style>
