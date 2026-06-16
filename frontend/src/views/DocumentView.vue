<template>
  <div class="page">
    <div class="table-panel">
      <div class="toolbar">
        <el-input v-model="keyword" :placeholder="keywordPlaceholder" clearable style="width: 260px" />
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="keyword = ''; load()">重置</el-button>
        <el-button type="success" @click="create">{{ isTransfer ? '新增库存调拨单' : '新增' }}</el-button>
      </div>
      <el-table :data="filteredRows" border empty-text="暂无数据">
        <el-table-column type="index" label="序号" width="70" />
        <el-table-column prop="documentNo" :label="`${config.name}单号`" min-width="190" />
        <el-table-column prop="warehouseCode" label="仓库编号" />
        <el-table-column prop="warehouseName" label="仓库名称" />
        <el-table-column prop="partnerCode" :label="config.partnerCode" />
        <el-table-column prop="partnerName" :label="config.partnerName" />
        <el-table-column prop="totalAmount" label="结算金额" />
        <el-table-column label="审核状态" width="110">
          <template #default="{ row }"><el-tag>{{ statusLabel(row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="creatorName" label="发起人" />
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="view(row)">查看</el-button>
            <el-button v-if="['DRAFT', 'REJECTED'].includes(row.status)" text type="success" @click="submit(row)">提交</el-button>
            <el-button v-if="['DRAFT', 'REJECTED'].includes(row.status)" text type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="`查看${config.name}单`" width="780px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="单据号">{{ current?.documentNo }}</el-descriptions-item>
        <el-descriptions-item label="审核状态">{{ statusLabel(current?.status) }}</el-descriptions-item>
        <el-descriptions-item label="仓库名称">{{ current?.warehouseName }}</el-descriptions-item>
        <el-descriptions-item :label="config.partnerName">{{ current?.partnerName }}</el-descriptions-item>
        <el-descriptions-item label="发起人">{{ current?.creatorName }}</el-descriptions-item>
        <el-descriptions-item label="审核人">{{ current?.auditorName || '暂无' }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="current?.items || []" border empty-text="暂无数据" style="margin-top: 12px">
        <el-table-column prop="productCode" label="商品编号" />
        <el-table-column prop="productName" label="商品名称" />
        <el-table-column prop="quantity" label="数量" />
        <el-table-column prop="unitName" label="单位" />
        <el-table-column prop="price" label="实际单价" />
        <el-table-column prop="amount" label="结算金额" />
      </el-table>
    </el-dialog>

    <el-dialog v-model="transferDialogVisible" title="新增库存调拨单" width="560px">
      <el-form :model="transferForm" label-width="110px">
        <el-form-item label="调出仓库"><el-input v-model="transferForm.warehouseId" placeholder="请输入调出仓库标识" /></el-form-item>
        <el-form-item label="调入仓库"><el-input v-model="transferForm.targetWarehouseId" placeholder="请输入调入仓库标识" /></el-form-item>
        <el-form-item label="商品"><el-input v-model="transferForm.productId" placeholder="请输入商品标识" /></el-form-item>
        <el-form-item label="调出数量"><el-input v-model="transferForm.quantity" placeholder="请输入调出数量" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="transferForm.remark" placeholder="请输入备注" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="transferDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveTransfer">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createDocument, deleteDocument, listDocuments, submitDocument } from '../api'

const route = useRoute()
const rows = ref<any[]>([])
const keyword = ref('')
const dialogVisible = ref(false)
const transferDialogVisible = ref(false)
const current = ref<any>()
const transferForm = reactive({
  warehouseId: '',
  targetWarehouseId: '',
  productId: '',
  quantity: '',
  remark: ''
})
const isTransfer = computed(() => route.path === '/inventory/transfer')

const config = computed(() => {
  const type = String(route.params.type)
  if (route.path === '/inventory/transfer') {
    return { api: 'stock-transfer', name: '库存调拨', partnerCode: '调入仓库编号', partnerName: '调入仓库名称' }
  }
  if (route.path.startsWith('/sales') && type === 'return') {
    return { api: 'sales-return', name: '销售退货', partnerCode: '客户编号', partnerName: '客户名称' }
  }
  if (route.path.startsWith('/purchase') && type === 'return') {
    return { api: 'purchase-return', name: '采购退货', partnerCode: '供应商编号', partnerName: '供应商名称' }
  }
  if (route.path.startsWith('/sales') && type === 'outbound') {
    return { api: 'sales-outbound', name: '销售出库', partnerCode: '客户编号', partnerName: '客户名称' }
  }
  return { api: 'purchase-inbound', name: '采购入库', partnerCode: '供应商编号', partnerName: '供应商名称' }
})
const keywordPlaceholder = computed(() => isTransfer.value ? '请输入库存调拨单号查询' : `请输入${config.value.name}单号查询`)
const filteredRows = computed(() => rows.value.filter((row) => !keyword.value || row.documentNo.includes(keyword.value)))

function statusLabel(status?: string) {
  return ({ DRAFT: '待提交', PENDING: '待审核', APPROVED: '审核通过', REJECTED: '审核拒绝' } as Record<string, string>)[status || ''] || '待提交'
}

async function load() {
  rows.value = await listDocuments(config.value.api)
}

async function create() {
  if (isTransfer.value) {
    transferForm.warehouseId = ''
    transferForm.targetWarehouseId = ''
    transferForm.productId = ''
    transferForm.quantity = ''
    transferForm.remark = ''
    transferDialogVisible.value = true
    return
  }
  await createDocument(config.value.api)
  ElMessage.success('新增成功')
  await load()
}

async function saveTransfer() {
  await createDocument(config.value.api, transferForm)
  ElMessage.success('新增成功')
  transferDialogVisible.value = false
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
  dialogVisible.value = true
}

watch(() => route.fullPath, load)
onMounted(load)
</script>
