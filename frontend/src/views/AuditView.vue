<template>
  <div class="page">
    <div class="table-panel">
      <div class="toolbar">
        <el-button type="primary" @click="load">刷新</el-button>
      </div>
      <el-table :data="rows" border empty-text="暂无数据">
        <el-table-column type="index" label="序号" width="70" />
        <el-table-column prop="documentNo" label="单据号" min-width="190" />
        <el-table-column prop="warehouseName" label="仓库名称" />
        <el-table-column prop="creatorName" label="发起人" />
        <el-table-column label="审核状态">
          <template #default="{ row }"><el-tag>{{ statusLabel(row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="operationTime" label="操作时间" min-width="180" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="current = row; dialogVisible = true">查看</el-button>
            <el-button v-if="row.status === 'PENDING'" text type="success" @click="approve(row)">审核通过</el-button>
            <el-button v-if="row.status === 'PENDING'" text type="danger" @click="reject(row)">审核拒绝</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <el-dialog v-model="dialogVisible" title="查看单据" width="760px">
      <el-table :data="current?.items || []" border empty-text="暂无数据">
        <el-table-column prop="productCode" label="商品编号" />
        <el-table-column prop="productName" label="商品名称" />
        <el-table-column prop="quantity" label="数量" />
        <el-table-column prop="unitName" label="单位" />
        <el-table-column prop="remark" label="备注" />
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

function statusLabel(status: string) {
  return ({ PENDING: '待审核', APPROVED: '审核通过', REJECTED: '审核拒绝' } as Record<string, string>)[status] || '待审核'
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

