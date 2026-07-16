<template>
  <div class="page">
    <div class="table-panel">
      <div class="toolbar query-toolbar">
        <el-input v-model="query.keyword" placeholder="综合查询：任意字段" clearable style="width: 220px" />
        <el-input v-model="query.productCode" placeholder="请输入商品编号" clearable style="width: 180px" />
        <el-input v-model="query.productName" placeholder="请输入商品名称" clearable style="width: 180px" />
        <el-input v-model="query.unitName" placeholder="请输入商品单位" clearable style="width: 170px" />
        <span class="query-break" aria-hidden="true"></span>
        <span class="filter-label">商品分类</span>
        <el-select v-model="query.categoryName" placeholder="请选择" clearable style="width: 180px">
          <el-option v-for="item in categoryOptions" :key="item" :label="item" :value="item" />
        </el-select>
        <span class="filter-label">商品品牌</span>
        <el-select v-model="query.brandName" placeholder="请选择" clearable style="width: 180px">
          <el-option v-for="item in brandOptions" :key="item" :label="item" :value="item" />
        </el-select>
        <el-input v-model="query.warehouseName" placeholder="请输入仓库" clearable style="width: 180px" />
      </div>
      <div class="toolbar query-actions">
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
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { listStock } from '../api'

const rows = ref<any[]>([])
const query = reactive({
  keyword: '',
  productCode: '',
  productName: '',
  categoryName: '',
  brandName: '',
  unitName: '',
  warehouseName: ''
})
const currentPage = ref(1)
const pageSize = ref(10)
const detailVisible = ref(false)
const current = ref<any>()

const categoryOptions = computed(() => [...new Set(rows.value.map((row) => row.categoryName).filter(Boolean))])
const brandOptions = computed(() => [...new Set(rows.value.map((row) => row.brandName).filter(Boolean))])
const filteredRows = computed(() => rows.value.filter((row) => {
  const matchKeyword = matchRecordByKeyword(row, query.keyword)
  const matchProductCode = matchesText(row.productCode, query.productCode)
  const matchProductName = matchesText(row.productName, query.productName)
  const matchCategory = !query.categoryName || row.categoryName === query.categoryName
  const matchBrand = !query.brandName || row.brandName === query.brandName
  const matchUnit = matchesText(row.unitName, query.unitName)
  const matchWarehouse = matchesText(row.warehouseName, query.warehouseName)
  return matchKeyword && matchProductCode && matchProductName && matchCategory && matchBrand && matchUnit && matchWarehouse
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
  query.keyword = ''
  query.productCode = ''
  query.productName = ''
  query.categoryName = ''
  query.brandName = ''
  query.unitName = ''
  query.warehouseName = ''
  load()
}

function collectRecordValues(value: any): string[] {
  if (value === null || value === undefined) return []
  if (Array.isArray(value)) return value.flatMap((item) => collectRecordValues(item))
  if (typeof value === 'object') return Object.values(value).flatMap((item) => collectRecordValues(item))
  return [String(value)]
}

function matchesText(value: any, keywordValue: string) {
  const trimmed = keywordValue.trim().toLowerCase()
  if (!trimmed) return true
  return String(value ?? '').toLowerCase().includes(trimmed)
}

function matchRecordByKeyword(row: any, keywordValue: string) {
  const trimmed = keywordValue.trim().toLowerCase()
  if (!trimmed) return true
  return collectRecordValues(row).some((value) => value.toLowerCase().includes(trimmed))
}

function viewWarehouse(row: any) {
  current.value = row
  detailVisible.value = true
}

onMounted(load)
watch([
  () => query.keyword,
  () => query.productCode,
  () => query.productName,
  () => query.categoryName,
  () => query.brandName,
  () => query.unitName,
  () => query.warehouseName,
  pageSize
], () => {
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
