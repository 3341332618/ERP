<template>
  <div class="page">
    <el-row :gutter="12">
      <el-col :span="6" v-for="item in cards" :key="item.title">
        <el-card shadow="never">
          <div class="metric-title">{{ item.title }}</div>
          <div class="metric-value">{{ item.value }}</div>
        </el-card>
      </el-col>
    </el-row>
    <div class="table-panel dashboard-panel">
      <h2>业务提醒</h2>
      <el-alert title="请优先处理待审核入库、待审核出库和库存调拨单据。" type="info" show-icon :closable="false" />
    </div>
  </div>
</template>

<script setup lang="ts">
const cards = [
  { title: '采购单据', value: '采购入库 / 采购退货' },
  { title: '销售单据', value: '销售出库 / 销售退货' },
  { title: '库存审核', value: '入库审核 / 出库审核' },
  { title: '结算记录', value: '收入结算 / 支出结算' }
]
</script>

<style scoped>
.metric-title {
  color: #6b7280;
  font-size: 13px;
}

.metric-value {
  margin-top: 10px;
  font-size: 18px;
  font-weight: 700;
}

.dashboard-panel {
  margin-top: 14px;
}

h2 {
  font-size: 16px;
  margin: 0 0 12px;
}
</style>

