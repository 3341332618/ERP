<template>
  <div class="page">
    <div class="table-panel">
      <div class="toolbar">
        <el-input v-model="keyword" :placeholder="keywordPlaceholder" clearable style="width: 260px" />
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="keyword = ''; load()">重置</el-button>
        <el-button type="success" @click="openCreate">新增{{ config.name }}单</el-button>
      </div>
      <el-table :data="pagedRows" border empty-text="暂无数据">
        <el-table-column type="index" label="序号" width="70" />
        <el-table-column prop="documentNo" :label="`${config.name}单号`" min-width="190" />
        <el-table-column prop="warehouseCode" label="仓库编号" />
        <el-table-column prop="warehouseName" label="仓库名称" />
        <el-table-column v-if="isTransfer" prop="targetWarehouseCode" label="调入仓库编号" />
        <el-table-column v-if="isTransfer" prop="targetWarehouseName" label="调入仓库名称" />
        <el-table-column v-if="!isTransfer" prop="partnerCode" :label="config.partnerCode" />
        <el-table-column v-if="!isTransfer" prop="partnerName" :label="config.partnerName" />
        <el-table-column prop="items.length" label="商品种类数" width="110" />
        <el-table-column prop="totalAmount" :label="config.totalLabel" min-width="120" />
        <el-table-column v-if="config.showRelated" prop="relatedDocumentNo" label="关联单据号" min-width="180" />
        <el-table-column label="审核状态" width="110">
          <template #default="{ row }"><el-tag>{{ statusLabel(row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="creatorName" label="发起人" />
        <el-table-column prop="operationTime" label="操作时间" min-width="180" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button v-if="['DRAFT', 'REJECTED'].includes(row.status)" text type="primary" @click="openEdit(row)">修改</el-button>
            <el-button text type="primary" @click="view(row)">查看</el-button>
            <el-button v-if="['DRAFT', 'REJECTED'].includes(row.status)" text type="success" @click="submit(row)">提交</el-button>
            <el-button v-if="['DRAFT', 'REJECTED'].includes(row.status)" text type="danger" @click="remove(row)">删除</el-button>
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

    <el-dialog v-model="detailVisible" :title="`查看${config.name}单`" width="920px">
      <div class="dialog-section-title">基础信息</div>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="单据号">{{ current?.documentNo }}</el-descriptions-item>
        <el-descriptions-item label="审核状态">{{ statusLabel(current?.status) }}</el-descriptions-item>
        <el-descriptions-item label="仓库名称">{{ current?.warehouseName }}</el-descriptions-item>
        <el-descriptions-item v-if="isTransfer" label="调入仓库">{{ current?.targetWarehouseName }}</el-descriptions-item>
        <el-descriptions-item v-else :label="config.partnerName">{{ current?.partnerName }}</el-descriptions-item>
        <el-descriptions-item v-if="config.showRelated" label="关联单据号">{{ current?.relatedDocumentNo || '暂无' }}</el-descriptions-item>
        <el-descriptions-item label="发起人">{{ current?.creatorName }}</el-descriptions-item>
        <el-descriptions-item label="审核人">{{ current?.auditorName || '暂无' }}</el-descriptions-item>
        <el-descriptions-item label="审核时间">{{ current?.auditTime || '暂无' }}</el-descriptions-item>
      </el-descriptions>

      <div class="dialog-section-title">商品明细</div>
      <el-table :data="current?.items || []" border empty-text="暂无数据">
        <el-table-column type="index" label="序号" width="70" />
        <el-table-column prop="productCode" label="商品编号" />
        <el-table-column prop="productName" label="商品名称" />
        <el-table-column prop="categoryName" label="商品分类" />
        <el-table-column prop="brandName" label="商品品牌" />
        <el-table-column prop="availableQuantity" label="可用库存数量" />
        <el-table-column prop="quantity" :label="config.quantityLabel" />
        <el-table-column prop="unitName" label="商品单位" />
        <el-table-column prop="price" :label="config.priceLabel" />
        <el-table-column prop="amount" :label="config.amountLabel" />
        <el-table-column prop="remark" label="备注" />
      </el-table>

      <div class="dialog-section-title">操作记录</div>
      <el-table :data="operationRecords" border empty-text="暂无操作记录">
        <el-table-column prop="time" label="操作时间" />
        <el-table-column prop="operator" label="操作人" />
        <el-table-column prop="content" label="操作内容" />
      </el-table>
    </el-dialog>

    <el-dialog v-model="formVisible" :title="formTitle" width="720px">
      <el-form :model="form" :rules="formRules" label-width="130px">
        <el-form-item :label="isTransfer ? '调出仓库标识' : '仓库标识'" prop="warehouseId">
          <el-input v-model="form.warehouseId" :placeholder="isTransfer ? '请输入调出仓库标识' : '请输入仓库标识'" />
        </el-form-item>
        <el-form-item v-if="isTransfer" label="调入仓库标识" prop="targetWarehouseId">
          <el-input v-model="form.targetWarehouseId" placeholder="请输入调入仓库标识" />
        </el-form-item>
        <el-form-item v-if="!isTransfer" :label="config.partnerIdLabel" prop="partnerId">
          <el-input v-model="form.partnerId" :placeholder="`请输入${config.partnerIdLabel}`" />
        </el-form-item>
        <el-form-item v-if="config.showRelated" label="关联单据号" prop="relatedDocumentNo">
          <el-input v-model="form.relatedDocumentNo" :placeholder="`请输入${config.relatedLabel}`" />
        </el-form-item>
        <el-form-item label="商品标识" prop="productId">
          <el-input v-model="form.productId" placeholder="请输入商品标识" />
        </el-form-item>
        <el-form-item :label="config.quantityLabel" prop="quantity">
          <el-input v-model="form.quantity" :placeholder="`请输入${config.quantityLabel}`" />
        </el-form-item>
        <el-form-item v-if="!isTransfer" :label="config.priceLabel" prop="price">
          <el-input v-model="form.price" :placeholder="`请输入${config.priceLabel}`" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="formVisible = false">取消</el-button>
          <el-button type="primary" @click="saveDocument">保存</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox, type FormRules } from 'element-plus'
import { createDocument, deleteDocument, listDocuments, submitDocument, updateDocument } from '../api'

const route = useRoute()
const rows = ref<any[]>([])
const keyword = ref('')
const detailVisible = ref(false)
const formVisible = ref(false)
const editingId = ref<number | null>(null)
const current = ref<any>()
const form = reactive<Record<string, string>>({})
const currentPage = ref(1)
const pageSize = ref(10)
const isTransfer = computed(() => route.path === '/inventory/transfer')

const config = computed(() => {
  const type = String(route.params.type)
  if (route.path === '/inventory/transfer') {
    return {
      api: 'stock-transfer',
      name: '库存调拨',
      partnerCode: '调入仓库编号',
      partnerName: '调入仓库名称',
      partnerIdLabel: '调入仓库标识',
      quantityLabel: '调出数量',
      priceLabel: '调拨单价',
      amountLabel: '调拨金额',
      totalLabel: '调拨金额',
      relatedLabel: '',
      relatedRequiredMessage: '',
      showRelated: false
    }
  }
  if (route.path.startsWith('/sales') && type === 'return') {
    return {
      api: 'sales-return',
      name: '销售退货',
      partnerCode: '客户编号',
      partnerName: '客户名称',
      partnerIdLabel: '客户标识',
      quantityLabel: '销退数量',
      priceLabel: '实际销售价（元）',
      amountLabel: '销退金额（元）',
      totalLabel: '销退总金额',
      relatedLabel: '关联销售出库单号',
      relatedRequiredMessage: '关联销售出库单必填，请重新输入。',
      showRelated: true
    }
  }
  if (route.path.startsWith('/purchase') && type === 'return') {
    return {
      api: 'purchase-return',
      name: '采购退货',
      partnerCode: '供应商编号',
      partnerName: '供应商名称',
      partnerIdLabel: '供应商标识',
      quantityLabel: '采退数量',
      priceLabel: '实际采购价（元）',
      amountLabel: '采退金额（元）',
      totalLabel: '采退总金额',
      relatedLabel: '关联采购入库单号',
      relatedRequiredMessage: '关联采购入库单必填，请重新输入。',
      showRelated: true
    }
  }
  if (route.path.startsWith('/sales') && type === 'outbound') {
    return {
      api: 'sales-outbound',
      name: '销售出库',
      partnerCode: '客户编号',
      partnerName: '客户名称',
      partnerIdLabel: '客户标识',
      quantityLabel: '销售出库数量',
      priceLabel: '实际销售价（元）',
      amountLabel: '销售结算金额（元）',
      totalLabel: '销出总金额',
      relatedLabel: '',
      relatedRequiredMessage: '',
      showRelated: false
    }
  }
  return {
    api: 'purchase-inbound',
    name: '采购入库',
    partnerCode: '供应商编号',
    partnerName: '供应商名称',
    partnerIdLabel: '供应商标识',
    quantityLabel: '采购入库数量',
    priceLabel: '实际采购价（元）',
    amountLabel: '采购结算金额（元）',
    totalLabel: '采入总金额',
    relatedLabel: '',
    relatedRequiredMessage: '',
    showRelated: false
  }
})
const keywordPlaceholder = computed(() => isTransfer.value ? '请输入库存调拨单号查询' : `请输入${config.value.name}单号查询`)
const filteredRows = computed(() => rows.value.filter((row) => !keyword.value || row.documentNo.includes(keyword.value)))
const pagedRows = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredRows.value.slice(start, start + pageSize.value)
})
const formTitle = computed(() => `${editingId.value ? '修改' : '新增'}${config.value.name}单`)
const formRules = computed<FormRules>(() => ({
  warehouseId: [{ required: true, message: isTransfer.value ? '调出仓库必填，请重新输入。' : '仓库必填，请重新输入。', trigger: 'blur' }],
  targetWarehouseId: [{ required: isTransfer.value, message: '调入仓库必填，请重新输入。', trigger: 'blur' }],
  partnerId: [{ required: !isTransfer.value, message: `${config.value.partnerIdLabel}必填，请重新输入。`, trigger: 'blur' }],
  relatedDocumentNo: [{ required: config.value.showRelated, message: config.value.relatedRequiredMessage, trigger: 'blur' }],
  productId: [{ required: true, message: '商品必填，请重新输入。', trigger: 'blur' }],
  quantity: [{ required: true, message: `${config.value.quantityLabel}必填，请重新输入。`, trigger: 'blur' }],
  price: [{ required: !isTransfer.value, message: `${config.value.priceLabel}必填，请重新输入。`, trigger: 'blur' }]
}))
const operationRecords = computed(() => {
  if (!current.value) return []
  const records = [
    { time: current.value.operationTime, operator: current.value.creatorName, content: statusLabel(current.value.status) === '待提交' ? '保存单据' : '提交单据' }
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

function statusLabel(status?: string) {
  if (isTransfer.value) {
    return ({ DRAFT: '未调拨', PENDING: '待调拨', APPROVED: '已调拨', REJECTED: '无法调拨' } as Record<string, string>)[status || ''] || '未调拨'
  }
  return ({ DRAFT: '待提交', PENDING: '待审核', APPROVED: '审核通过', REJECTED: '审核拒绝' } as Record<string, string>)[status || ''] || '待提交'
}

function resetForm() {
  Object.keys(form).forEach((key) => delete form[key])
  Object.assign(form, {
    warehouseId: '',
    targetWarehouseId: '',
    partnerId: '',
    relatedDocumentNo: '',
    productId: '',
    quantity: '',
    price: '',
    remark: ''
  })
}

async function load() {
  rows.value = await listDocuments(config.value.api)
  currentPage.value = 1
}

function openCreate() {
  editingId.value = null
  resetForm()
  formVisible.value = true
}

function openEdit(row: any) {
  editingId.value = row.id
  resetForm()
  const item = row.items?.[0] || {}
  Object.assign(form, {
    warehouseId: String(row.warehouseId || ''),
    targetWarehouseId: String(row.targetWarehouseId || ''),
    partnerId: String(row.partnerId || ''),
    relatedDocumentNo: row.relatedDocumentNo || '',
    productId: String(item.productId || ''),
    quantity: String(item.quantity || ''),
    price: String(item.price || ''),
    remark: item.remark || ''
  })
  formVisible.value = true
}

async function saveDocument() {
  if (editingId.value) {
    await updateDocument(config.value.api, editingId.value, form)
  } else {
    await createDocument(config.value.api, form)
  }
  ElMessage.success('保存成功')
  formVisible.value = false
  await load()
}

async function submit(row: any) {
  await ElMessageBox.confirm('确认提交该单据？', '提交确认', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
  await submitDocument(config.value.api, row.id)
  ElMessage.success('提交成功')
  await load()
}

async function remove(row: any) {
  await ElMessageBox.confirm('确认删除该单据？', '删除确认', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
  await deleteDocument(config.value.api, row.id)
  ElMessage.success('删除成功')
  await load()
}

function view(row: any) {
  current.value = row
  detailVisible.value = true
}

watch(() => route.fullPath, load)
watch([keyword, pageSize], () => {
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
