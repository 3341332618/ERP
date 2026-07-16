<template>
  <div class="page">
    <div class="table-panel">
      <div class="toolbar query-toolbar">
        <el-input v-model="query.keyword" placeholder="综合查询：任意字段" clearable style="width: 220px" />
        <el-input v-model="query.settlementNo" :placeholder="`请输入${title}单号`" clearable style="width: 210px" />
        <el-select v-model="query.documentType" placeholder="请选择单据类型" clearable style="width: 180px">
          <el-option v-for="item in typeOptions" :key="item" :label="item" :value="item" />
        </el-select>
        <el-input v-model="query.amount" placeholder="请输入金额" clearable style="width: 150px" />
        <el-input v-model="query.relatedDocumentNo" placeholder="请输入关联单据号" clearable style="width: 210px" />
        <span class="query-break" aria-hidden="true"></span>
        <el-date-picker
          v-model="query.createDateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          start-placeholder="创建开始日期"
          end-placeholder="创建结束日期"
          range-separator="至"
          style="width: 250px"
        />
        <span class="query-break" aria-hidden="true"></span>
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
      <el-table :data="pagedRows" border empty-text="暂无数据">
        <el-table-column type="index" label="序号" width="70" />
        <el-table-column prop="settlementNo" :label="`${title}单号`" min-width="190" />
        <el-table-column prop="documentType" label="单据类型" />
        <el-table-column prop="amount" label="金额（元）" />
        <el-table-column prop="relatedDocumentNo" label="关联单据号" min-width="190" />
        <el-table-column prop="createTime" label="创建时间" min-width="180" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="openDetail(row)">查看</el-button>
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

    <el-dialog v-model="detailVisible" :title="`查看${title}单`" width="920px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="结算单号">{{ detail?.settlement?.settlementNo }}</el-descriptions-item>
        <el-descriptions-item label="单据类型">{{ detail?.settlement?.documentType }}</el-descriptions-item>
        <el-descriptions-item label="涉及金额">{{ detail?.settlement?.amount }}</el-descriptions-item>
        <el-descriptions-item label="关联单据号">
          {{ detail?.settlement?.relatedDocumentNo }}
          <el-button text type="primary" @click="relatedVisible = true">查看详情</el-button>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <el-dialog v-model="relatedVisible" :title="`查看${detail?.document?.type?.label || '关联'}单`" width="920px">
      <div class="dialog-section-title">基础信息</div>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="单据号">{{ detail?.document?.documentNo }}</el-descriptions-item>
        <el-descriptions-item label="审核状态">审核通过</el-descriptions-item>
        <el-descriptions-item label="仓库名称">{{ detail?.document?.warehouseName }}</el-descriptions-item>
        <el-descriptions-item label="往来单位">{{ detail?.document?.partnerName || detail?.document?.targetWarehouseName }}</el-descriptions-item>
        <el-descriptions-item label="发起人">{{ detail?.document?.creatorName }}</el-descriptions-item>
        <el-descriptions-item label="审核人">{{ detail?.document?.auditorName }}</el-descriptions-item>
      </el-descriptions>
      <div class="dialog-section-title">商品明细</div>
      <el-table :data="detail?.document?.items || []" border empty-text="暂无数据">
        <el-table-column type="index" label="序号" width="70" />
        <el-table-column prop="productCode" label="商品编号" />
        <el-table-column prop="productName" label="商品名称" />
        <el-table-column prop="categoryName" label="商品分类" />
        <el-table-column prop="brandName" label="商品品牌" />
        <el-table-column prop="quantity" label="数量" />
        <el-table-column prop="unitName" label="商品单位" />
        <el-table-column prop="price" label="实际单价（元）" />
        <el-table-column prop="amount" label="结算金额（元）" />
      </el-table>
      <div class="dialog-section-title">操作记录</div>
      <el-table :data="relatedRecords" border empty-text="暂无操作记录">
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
import { listSettlement, settlementDetail } from '../api'

const route = useRoute()
const rows = ref<any[]>([])
const query = reactive({
  keyword: '',
  settlementNo: '',
  documentType: '',
  amount: '',
  relatedDocumentNo: '',
  createDateRange: [] as string[]
})
const currentPage = ref(1)
const pageSize = ref(10)
const detailVisible = ref(false)
const relatedVisible = ref(false)
const detail = ref<any>()
const direction = computed(() => String(route.params.type) === 'income' ? 'income' : 'expense')
const title = computed(() => direction.value === 'income' ? '收入结算' : '支出结算')
const typeOptions = computed(() => direction.value === 'income' ? ['销出收入', '采退收入'] : ['采入支出', '销退支出', '其他支出'])
const filteredRows = computed(() => rows.value.filter((row) => {
  const matchKeyword = matchRecordByKeyword(row, query.keyword)
  const matchSettlementNo = matchesText(row.settlementNo, query.settlementNo)
  const matchType = !query.documentType || row.documentType === query.documentType
  const matchAmount = matchesText(row.amount, query.amount)
  const matchRelatedNo = matchesText(row.relatedDocumentNo, query.relatedDocumentNo)
  const matchCreateDate = inDateRange(row.createTime, query.createDateRange)
  return matchKeyword && matchSettlementNo && matchType && matchAmount && matchRelatedNo && matchCreateDate
}))
const pagedRows = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredRows.value.slice(start, start + pageSize.value)
})
const relatedRecords = computed(() => {
  const document = detail.value?.document
  if (!document) return []
  return [
    { time: document.auditTime, operator: document.auditorName, content: '审核通过' },
    { time: document.operationTime, operator: document.creatorName, content: '提交单据' }
  ]
})

async function load() {
  rows.value = await listSettlement(direction.value)
  currentPage.value = 1
}

function reset() {
  query.keyword = ''
  query.settlementNo = ''
  query.documentType = ''
  query.amount = ''
  query.relatedDocumentNo = ''
  query.createDateRange = []
  load()
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

async function openDetail(row: any) {
  detail.value = await settlementDetail(direction.value, row.id)
  detailVisible.value = true
  relatedVisible.value = false
}

watch(direction, load)
watch([
  () => query.keyword,
  () => query.settlementNo,
  () => query.documentType,
  () => query.amount,
  () => query.relatedDocumentNo,
  () => query.createDateRange.join('|'),
  pageSize
], () => {
  currentPage.value = 1
})
onMounted(load)
</script>

<style scoped>
.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 14px;
  padding-top: 14px;
  color: #606266;
}
</style>
