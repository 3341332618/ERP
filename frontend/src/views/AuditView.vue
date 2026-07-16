<template>
  <div class="page">
    <div class="table-panel">
      <div class="toolbar query-toolbar">
        <el-input v-model="query.keyword" placeholder="综合查询：任意字段" clearable style="width: 220px" />
        <el-input v-model="query.documentNo" placeholder="请输入单据号" clearable style="width: 200px" />
        <el-input v-model="query.warehouseCode" placeholder="请输入仓库编号" clearable style="width: 170px" />
        <el-input v-model="query.warehouseName" placeholder="请输入仓库名称" clearable style="width: 170px" />
        <span class="query-break" aria-hidden="true"></span>
        <el-input v-model="query.businessType" placeholder="请输入业务类型" clearable style="width: 170px" />
        <el-input v-model="query.itemCount" placeholder="请输入商品种类数" clearable style="width: 160px" />
        <el-input v-model="query.creatorName" placeholder="请输入发起人" clearable style="width: 150px" />
        <el-select v-model="query.status" placeholder="请选择审核状态" clearable style="width: 150px">
          <el-option label="待审核" value="PENDING" />
          <el-option label="审核通过" value="APPROVED" />
          <el-option label="审核拒绝" value="REJECTED" />
        </el-select>
        <el-input v-model="query.auditorName" placeholder="请输入审核人" clearable style="width: 150px" />
        <span class="query-break" aria-hidden="true"></span>
        <el-date-picker
          v-model="query.operationDateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          start-placeholder="操作开始日期"
          end-placeholder="操作结束日期"
          range-separator="至"
          style="width: 250px"
        />
        <el-date-picker
          v-model="query.auditDateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          start-placeholder="审核开始日期"
          end-placeholder="审核结束日期"
          range-separator="至"
          style="width: 250px"
        />
        <span class="query-break" aria-hidden="true"></span>
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </div>
      <el-table :data="filteredRows" border empty-text="暂无数据">
        <el-table-column type="index" label="序号" width="70" />
        <el-table-column prop="documentNo" label="单据号" min-width="190" />
        <el-table-column prop="warehouseCode" label="仓库编号" />
        <el-table-column prop="warehouseName" label="仓库名称" />
        <el-table-column label="业务类型">
          <template #default="{ row }">{{ businessType(row) }}</template>
        </el-table-column>
        <el-table-column prop="items.length" label="商品种类数" width="110" />
        <el-table-column prop="creatorName" label="发起人" />
        <el-table-column label="审核状态">
          <template #default="{ row }"><el-tag>{{ statusLabel(row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="operationTime" label="操作时间" min-width="180" />
        <el-table-column prop="auditorName" label="审核人" />
        <el-table-column prop="auditTime" label="审核时间" min-width="180" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="current = row; dialogVisible = true">查看</el-button>
            <el-button v-if="row.status === 'PENDING'" text type="success" @click="approve(row)">审核通过</el-button>
            <el-button v-if="row.status === 'PENDING'" text type="danger" @click="reject(row)">审核拒绝</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <el-dialog v-model="dialogVisible" :title="`查看${businessType(current)}单据`" width="860px">
      <div class="dialog-section-title">基础信息</div>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="单据号">{{ current?.documentNo }}</el-descriptions-item>
        <el-descriptions-item label="业务类型">{{ businessType(current) }}</el-descriptions-item>
        <el-descriptions-item label="仓库名称">{{ current?.warehouseName }}</el-descriptions-item>
        <el-descriptions-item v-if="current?.targetWarehouseName" label="调入仓库">{{ current?.targetWarehouseName }}</el-descriptions-item>
        <el-descriptions-item label="审核状态">{{ statusLabel(current?.status) }}</el-descriptions-item>
        <el-descriptions-item label="发起人">{{ current?.creatorName }}</el-descriptions-item>
        <el-descriptions-item label="操作时间">{{ current?.operationTime }}</el-descriptions-item>
        <el-descriptions-item label="审核人">{{ current?.auditorName || '暂无' }}</el-descriptions-item>
        <el-descriptions-item label="审核时间">{{ current?.auditTime || '暂无' }}</el-descriptions-item>
      </el-descriptions>
      <div class="dialog-section-title">商品列表</div>
      <el-table :data="current?.items || []" border empty-text="暂无数据">
        <el-table-column type="index" label="序号" width="70" />
        <el-table-column prop="productCode" label="商品编号" />
        <el-table-column prop="productName" label="商品名称" />
        <el-table-column prop="categoryName" label="商品分类" />
        <el-table-column prop="brandName" label="商品品牌" />
        <el-table-column prop="availableQuantity" label="可用库存数量" />
        <el-table-column prop="quantity" :label="quantityLabel" />
        <el-table-column prop="unitName" label="商品单位" />
        <el-table-column prop="remark" label="备注" />
      </el-table>
      <div class="dialog-section-title">操作记录</div>
      <el-table :data="operationRecords" border empty-text="暂无操作记录">
        <el-table-column prop="time" label="操作时间" />
        <el-table-column prop="operator" label="操作人" />
        <el-table-column prop="content" label="操作内容" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { approveAudit, listAudit, rejectAudit } from '../api'

const route = useRoute()
const rows = ref<any[]>([])
const query = reactive({
  keyword: '',
  documentNo: '',
  warehouseCode: '',
  warehouseName: '',
  businessType: '',
  itemCount: '',
  creatorName: '',
  status: '',
  auditorName: '',
  operationDateRange: [] as string[],
  auditDateRange: [] as string[]
})
const current = ref<any>()
const dialogVisible = ref(false)
const direction = computed(() => String(route.params.type).includes('inbound') ? 'inbound' : 'outbound')
const quantityLabel = computed(() => direction.value === 'inbound' ? '入库数量' : '出库数量')
const filteredRows = computed(() => rows.value.filter((row) =>
  matchRecordByKeyword(row, query.keyword) &&
  matchesText(row.documentNo, query.documentNo) &&
  matchesText(row.warehouseCode, query.warehouseCode) &&
  matchesText(row.warehouseName, query.warehouseName) &&
  matchesText(businessType(row), query.businessType) &&
  matchesText(row.items?.length, query.itemCount) &&
  matchesText(row.creatorName, query.creatorName) &&
  (!query.status || row.status === query.status) &&
  matchesText(row.auditorName, query.auditorName) &&
  inDateRange(row.operationTime, query.operationDateRange) &&
  inDateRange(row.auditTime, query.auditDateRange)
))
const operationRecords = computed(() => {
  if (!current.value) return []
  const records = [
    { time: current.value.operationTime, operator: current.value.creatorName, content: '提交单据' }
  ]
  if (current.value.auditorName && current.value.auditTime) {
    records.unshift({
      time: current.value.auditTime,
      operator: current.value.auditorName,
      content: current.value.status === 'REJECTED' ? `审核拒绝：${current.value.rejectReason || '暂无原因'}` : '审核通过'
    })
  }
  return records
})

function statusLabel(status: string) {
  return ({ PENDING: '待审核', APPROVED: '审核通过', REJECTED: '审核拒绝' } as Record<string, string>)[status] || '待审核'
}

function businessType(row?: any) {
  if (!row) return direction.value === 'inbound' ? '入库审核' : '出库审核'
  if (row.type === 'STOCK_TRANSFER') return direction.value === 'inbound' ? '调拨入库' : '调拨出库'
  return ({
    PURCHASE_INBOUND: '采购入库',
    PURCHASE_RETURN: '采购退货',
    SALES_OUTBOUND: '销售出库',
    SALES_RETURN: '销售退货'
  } as Record<string, string>)[row.type] || (direction.value === 'inbound' ? '入库审核' : '出库审核')
}

function collectRecordValues(value: any): string[] {
  if (value === null || value === undefined) return []
  if (Array.isArray(value)) return value.flatMap((item) => collectRecordValues(item))
  if (typeof value === 'object') return Object.values(value).flatMap((item) => collectRecordValues(item))
  return [String(value)]
}

function matchRecordByKeyword(row: any, keyword: string) {
  const trimmed = keyword.trim().toLowerCase()
  if (!trimmed) return true
  return collectRecordValues(row).some((value) => value.toLowerCase().includes(trimmed))
}

function matchesText(value: any, keyword: string) {
  const trimmed = keyword.trim().toLowerCase()
  if (!trimmed) return true
  return String(value ?? '').toLowerCase().includes(trimmed)
}

function inDateRange(value: string | undefined, range: string[]) {
  if (!range?.length || range.length !== 2) return true
  if (!value) return false
  const day = value.slice(0, 10)
  return day >= range[0] && day <= range[1]
}

async function load() {
  rows.value = await listAudit(direction.value)
}

function resetQuery() {
  query.keyword = ''
  query.documentNo = ''
  query.warehouseCode = ''
  query.warehouseName = ''
  query.businessType = ''
  query.itemCount = ''
  query.creatorName = ''
  query.status = ''
  query.auditorName = ''
  query.operationDateRange = []
  query.auditDateRange = []
  load()
}

async function approve(row: any) {
  await ElMessageBox.confirm('确认审核通过该单据？', '审核确认', { confirmButtonText: '确定', cancelButtonText: '取消' })
  await approveAudit(row.id)
  ElMessage.success('审核通过')
  await load()
}

async function reject(row: any) {
  const { value } = await ElMessageBox.prompt('请输入拒绝原因', '审核拒绝原因', { confirmButtonText: '确定拒绝', cancelButtonText: '取消' })
  await rejectAudit(row.id, value)
  ElMessage.success('审核拒绝')
  await load()
}

watch(direction, load)
watch([
  () => query.keyword,
  () => query.documentNo,
  () => query.warehouseCode,
  () => query.warehouseName,
  () => query.businessType,
  () => query.itemCount,
  () => query.creatorName,
  () => query.status,
  () => query.auditorName,
  () => query.operationDateRange.join('|'),
  () => query.auditDateRange.join('|')
], () => {
  current.value = undefined
})

onMounted(load)
</script>
