<template>
  <div class="page">
    <div class="table-panel">
      <div class="toolbar">
        <el-input v-model="keyword" :placeholder="keywordPlaceholder" clearable style="width: 260px" />
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="keyword = ''; load()">重置</el-button>
        <el-button type="success" @click="openCreate">新增{{ config.name }}单</el-button>
      </div>
      <el-table :data="filteredRows" border empty-text="暂无数据">
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
    </div>

    <el-dialog v-model="detailVisible" :title="`查看${config.name}单`" width="920px">
      <h3>基础信息</h3>
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

      <h3>商品明细</h3>
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

      <h3>操作记录</h3>
      <el-table :data="operationRecords" border empty-text="暂无操作记录">
        <el-table-column prop="time" label="操作时间" />
        <el-table-column prop="operator" label="操作人" />
        <el-table-column prop="content" label="操作内容" />
      </el-table>
    </el-dialog>

    <el-dialog v-model="formVisible" :title="formTitle" width="720px">
      <el-form :model="form" label-width="130px">
        <el-form-item :label="isTransfer ? '调出仓库标识' : '仓库标识'">
          <el-input v-model="form.warehouseId" :placeholder="isTransfer ? '请输入调出仓库标识' : '请输入仓库标识'" />
        </el-form-item>
        <el-form-item v-if="isTransfer" label="调入仓库标识">
          <el-input v-model="form.targetWarehouseId" placeholder="请输入调入仓库标识" />
        </el-form-item>
        <el-form-item v-if="!isTransfer" :label="config.partnerIdLabel">
          <el-input v-model="form.partnerId" :placeholder="`请输入${config.partnerIdLabel}`" />
        </el-form-item>
        <el-form-item v-if="config.showRelated" label="关联单据号">
          <el-input v-model="form.relatedDocumentNo" :placeholder="`请输入${config.relatedLabel}`" />
        </el-form-item>
        <el-form-item label="商品标识">
          <el-input v-model="form.productId" placeholder="请输入商品标识" />
        </el-form-item>
        <el-form-item :label="config.quantityLabel">
          <el-input v-model="form.quantity" :placeholder="`请输入${config.quantityLabel}`" />
        </el-form-item>
        <el-form-item v-if="!isTransfer" :label="config.priceLabel">
          <el-input v-model="form.price" :placeholder="`请输入${config.priceLabel}`" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="saveDocument">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createDocument, deleteDocument, listDocuments, submitDocument, updateDocument } from '../api'

const route = useRoute()
const rows = ref<any[]>([])
const keyword = ref('')
const detailVisible = ref(false)
const formVisible = ref(false)
const editingId = ref<number | null>(null)
const current = ref<any>()
const form = reactive<Record<string, string>>({})
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
    showRelated: false
  }
})
const keywordPlaceholder = computed(() => isTransfer.value ? '请输入库存调拨单号查询' : `请输入${config.value.name}单号查询`)
const filteredRows = computed(() => rows.value.filter((row) => !keyword.value || row.documentNo.includes(keyword.value)))
const formTitle = computed(() => `${editingId.value ? '修改' : '新增'}${config.value.name}单`)
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
  await ElMessageBox.confirm('确认提交该单据？', '提交确认', { confirmButtonText: '确定', cancelButtonText: '取消' })
  await submitDocument(config.value.api, row.id)
  ElMessage.success('提交成功')
  await load()
}

async function remove(row: any) {
  await ElMessageBox.confirm('确认删除该单据？', '删除确认', { confirmButtonText: '确定', cancelButtonText: '取消' })
  await deleteDocument(config.value.api, row.id)
  ElMessage.success('删除成功')
  await load()
}

function view(row: any) {
  current.value = row
  detailVisible.value = true
}

watch(() => route.fullPath, load)
onMounted(load)
</script>

<style scoped>
h3 {
  margin: 16px 0 10px;
  font-size: 15px;
  font-weight: 700;
  color: #1f2937;
}
</style>
