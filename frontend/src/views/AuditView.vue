<template>
  <div class="page">
    <div class="table-panel">
      <div class="toolbar">
        <el-button type="primary" @click="load">刷新</el-button>
      </div>
      <el-table :data="rows" border empty-text="暂无数据">
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
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { approveAudit, listAudit, rejectAudit } from '../api'

const route = useRoute()
const rows = ref<any[]>([])
const current = ref<any>()
const dialogVisible = ref(false)
const direction = computed(() => String(route.params.type).includes('inbound') ? 'inbound' : 'outbound')
const quantityLabel = computed(() => direction.value === 'inbound' ? '入库数量' : '出库数量')
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

async function load() {
  rows.value = await listAudit(direction.value)
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
onMounted(load)
</script>
