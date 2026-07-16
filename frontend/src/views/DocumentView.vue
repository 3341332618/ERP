<template>
  <div class="page">
    <div class="table-panel">
      <div class="toolbar query-toolbar">
        <el-input v-model="query.keyword" placeholder="综合查询：任意字段" clearable style="width: 220px" />
        <el-input v-model="query.documentNo" :placeholder="`请输入${config.name}单号`" clearable style="width: 210px" />
        <el-input v-model="query.warehouseCode" placeholder="请输入仓库编号" clearable style="width: 170px" />
        <el-input v-model="query.warehouseName" placeholder="请输入仓库名称" clearable style="width: 170px" />
        <span class="query-break" aria-hidden="true"></span>
        <template v-if="isTransfer">
          <el-input v-model="query.targetWarehouseCode" placeholder="请输入调入仓库编号" clearable style="width: 180px" />
          <el-input v-model="query.targetWarehouseName" placeholder="请输入调入仓库名称" clearable style="width: 180px" />
        </template>
        <template v-else>
          <el-input v-model="query.partnerCode" :placeholder="`请输入${config.partnerCode}`" clearable style="width: 180px" />
          <el-input v-model="query.partnerName" :placeholder="`请输入${config.partnerName}`" clearable style="width: 180px" />
        </template>
        <el-input v-model="query.itemCount" placeholder="请输入商品种类数" clearable style="width: 160px" />
        <el-input v-model="query.totalAmount" :placeholder="`请输入${config.totalLabel}`" clearable style="width: 170px" />
        <span class="query-break" aria-hidden="true"></span>
        <el-input v-if="config.showRelated" v-model="query.relatedDocumentNo" placeholder="请输入关联单据号" clearable style="width: 210px" />
        <el-select v-model="query.status" placeholder="请选择审核状态" clearable style="width: 150px">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-input v-model="query.creatorName" placeholder="请输入发起人" clearable style="width: 150px" />
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
        <el-descriptions-item v-if="config.showRelated" label="关联单据号">
          <span>{{ current?.relatedDocumentNo || '暂无' }}</span>
          <el-button
            v-if="current?.relatedDocumentNo"
            text
            type="primary"
            class="source-detail-link"
            @click="openSourceDetail(current.relatedDocumentNo)"
          >
            <el-icon><View /></el-icon>
            查看原单
          </el-button>
        </el-descriptions-item>
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
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="130px">
        <template v-if="isReturn">
          <el-form-item label="关联原单" prop="relatedDocumentNo">
            <div class="related-source-control">
              <el-select
                v-model="form.relatedDocumentNo"
                filterable
                :placeholder="`请选择${config.relatedLabel}`"
                no-data-text="暂无可退原单"
                @change="handleReturnSourceChange"
              >
                <el-option
                  v-for="option in returnOptions"
                  :key="option.documentId"
                  :label="`${option.documentNo} · ${option.warehouseName} · ${option.partnerName}`"
                  :value="option.documentNo"
                />
              </el-select>
              <el-button
                :icon="View"
                circle
                title="查看原单"
                :disabled="!form.relatedDocumentNo"
                @click="openSourceDetail(form.relatedDocumentNo)"
              />
            </div>
          </el-form-item>
          <el-form-item label="仓库" prop="warehouseId">
            <el-input :model-value="selectedReturnSource?.warehouseName || ''" disabled />
          </el-form-item>
          <el-form-item :label="config.partnerName" prop="partnerId">
            <el-input :model-value="selectedReturnSource?.partnerName || ''" disabled />
          </el-form-item>
        </template>
        <template v-else>
          <el-form-item :label="isTransfer ? '调出仓库' : '仓库'" prop="warehouseId">
            <el-select
              v-model="form.warehouseId"
              filterable
              :placeholder="isTransfer ? '请选择调出仓库' : '请选择仓库'"
              @change="handleWarehouseChange"
            >
              <el-option
                v-for="warehouse in sourceWarehouseOptions"
                :key="warehouse.id"
                :label="masterOptionLabel(warehouse)"
                :value="warehouse.id"
                :disabled="warehouse.status !== 'ENABLED'"
              />
            </el-select>
          </el-form-item>
          <el-form-item v-if="isTransfer" label="调入仓库" prop="targetWarehouseId">
            <el-select v-model="form.targetWarehouseId" filterable placeholder="请选择调入仓库">
              <el-option
                v-for="warehouse in targetWarehouseOptions"
                :key="warehouse.id"
                :label="masterOptionLabel(warehouse)"
                :value="warehouse.id"
                :disabled="warehouse.status !== 'ENABLED'"
              />
            </el-select>
          </el-form-item>
          <el-form-item v-if="!isTransfer" :label="config.partnerName" prop="partnerId">
            <el-select v-model="form.partnerId" filterable :placeholder="`请选择${config.partnerName}`">
              <el-option
                v-for="partner in associationOptions.partners"
                :key="partner.id"
                :label="masterOptionLabel(partner)"
                :value="partner.id"
                :disabled="partner.status !== 'ENABLED'"
              />
            </el-select>
          </el-form-item>
        </template>
        <el-form-item label="商品" prop="productId">
          <el-select
            v-model="form.productId"
            filterable
            placeholder="请选择商品"
            no-data-text="暂无可选商品"
            @change="handleProductChange"
          >
            <el-option
              v-for="product in selectableProducts"
              :key="product.id"
              :label="productOptionLabel(product)"
              :value="product.id"
              :disabled="product.status !== undefined && product.status !== 'ENABLED'"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="config.quantityLabel" prop="quantity">
          <el-input-number
            v-model="form.quantity"
            :min="0.01"
            :max="quantityMax ?? Number.MAX_SAFE_INTEGER"
            :precision="2"
            :step="1"
            controls-position="right"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item v-if="!isTransfer" :label="config.priceLabel" prop="price">
          <el-input-number
            v-model="form.price"
            :min="0"
            :precision="2"
            :step="1"
            :disabled="isReturn"
            controls-position="right"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
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

    <el-dialog v-model="sourceDetailVisible" title="原单详情" width="860px" append-to-body>
      <template v-if="sourceDetail">
        <div class="dialog-section-title">基础信息</div>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="单据号">{{ sourceDetail.documentNo }}</el-descriptions-item>
          <el-descriptions-item label="审核状态">{{ statusLabel(sourceDetail.status) }}</el-descriptions-item>
          <el-descriptions-item label="仓库">{{ sourceDetail.warehouseCode }} · {{ sourceDetail.warehouseName }}</el-descriptions-item>
          <el-descriptions-item :label="config.partnerName">{{ sourceDetail.partnerCode }} · {{ sourceDetail.partnerName }}</el-descriptions-item>
          <el-descriptions-item label="发起人">{{ sourceDetail.creatorName }}</el-descriptions-item>
          <el-descriptions-item label="操作时间">{{ sourceDetail.operationTime }}</el-descriptions-item>
        </el-descriptions>
        <div class="dialog-section-title">商品明细</div>
        <el-table :data="sourceDetail.items" border empty-text="暂无数据">
          <el-table-column type="index" label="序号" width="70" />
          <el-table-column prop="productCode" label="商品编号" />
          <el-table-column prop="productName" label="商品名称" />
          <el-table-column prop="quantity" label="原单数量" />
          <el-table-column prop="unitName" label="商品单位" />
          <el-table-column prop="price" label="原单单价" />
          <el-table-column prop="amount" label="原单金额" />
        </el-table>
      </template>
      <template #footer>
        <el-button type="primary" @click="sourceDetailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { View } from '@element-plus/icons-vue'
import {
  createDocument,
  deleteDocument,
  documentDetail,
  listDocuments,
  listMaster,
  listReturnOptions,
  listStock,
  submitDocument,
  updateDocument,
  type DocumentRecord,
  type MasterRecord,
  type ReturnDocumentOption,
  type StockRow
} from '../api'
import {
  dedupeMasterOptions,
  stockProductOptions,
  stockWarehouseOptions,
  transferTargetOptions
} from '../utils/businessOptions'

interface DocumentForm {
  warehouseId?: number
  targetWarehouseId?: number
  partnerId?: number
  relatedDocumentNo: string
  productId?: number
  quantity?: number
  price?: number
  remark: string
  [key: string]: string | number | undefined
}

interface ProductSelectOption {
  id: number
  code: string
  name: string
  unitName?: string | null
  purchasePrice?: string | number | null
  salePrice?: string | number | null
  availableQuantity?: number
  remainingQuantity?: number
  price?: number
  status?: string
}

function emptyDocumentForm(): DocumentForm {
  return {
    warehouseId: undefined,
    targetWarehouseId: undefined,
    partnerId: undefined,
    relatedDocumentNo: '',
    productId: undefined,
    quantity: undefined,
    price: undefined,
    remark: ''
  }
}

const route = useRoute()
const rows = ref<DocumentRecord[]>([])
const query = reactive({
  keyword: '',
  documentNo: '',
  warehouseCode: '',
  warehouseName: '',
  targetWarehouseCode: '',
  targetWarehouseName: '',
  partnerCode: '',
  partnerName: '',
  itemCount: '',
  totalAmount: '',
  relatedDocumentNo: '',
  status: '',
  creatorName: '',
  operationDateRange: [] as string[],
  auditDateRange: [] as string[]
})
const detailVisible = ref(false)
const sourceDetailVisible = ref(false)
const sourceDetail = ref<DocumentRecord | null>(null)
const formVisible = ref(false)
const editingId = ref<number | null>(null)
const current = ref<DocumentRecord | null>(null)
const formRef = ref<FormInstance>()
const form = reactive<DocumentForm>(emptyDocumentForm())
const associationOptions = reactive({
  warehouses: [] as MasterRecord[],
  partners: [] as MasterRecord[],
  products: [] as MasterRecord[],
  stock: [] as StockRow[]
})
const returnOptions = ref<ReturnDocumentOption[]>([])
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
const isReturn = computed(() => config.value.showRelated)
const sourceDocumentApi = computed(() => config.value.api === 'purchase-return' ? 'purchase-inbound' : 'sales-outbound')
const selectedReturnSource = computed(() => returnOptions.value.find((option) => option.documentNo === form.relatedDocumentNo))
const sourceWarehouseOptions = computed(() => isTransfer.value
  ? stockWarehouseOptions(associationOptions.warehouses, associationOptions.stock, form.warehouseId)
  : associationOptions.warehouses)
const targetWarehouseOptions = computed(() => transferTargetOptions(
  associationOptions.warehouses,
  form.warehouseId,
  form.targetWarehouseId
))
const selectableProducts = computed<ProductSelectOption[]>(() => {
  if (isReturn.value) {
    return (selectedReturnSource.value?.items || []).map((item) => ({
      id: item.productId,
      code: item.productCode,
      name: item.productName,
      unitName: item.unitName,
      remainingQuantity: Number(item.remainingQuantity),
      price: Number(item.price)
    }))
  }
  if (config.value.api === 'sales-outbound' || isTransfer.value) {
    const stockOptions = stockProductOptions(associationOptions.stock, form.warehouseId)
    const availableByProduct = new Map(stockOptions.map((item) => [Number(item.productId), Number(item.availableQuantity)]))
    return associationOptions.products
      .filter((product) => availableByProduct.has(product.id))
      .map((product) => ({
        id: product.id,
        code: product.code,
        name: product.name,
        unitName: product.unitName,
        purchasePrice: product.purchasePrice,
        salePrice: product.salePrice,
        availableQuantity: availableByProduct.get(product.id),
        status: product.status
      }))
  }
  return associationOptions.products.map((product) => ({
    id: product.id,
    code: product.code,
    name: product.name,
    unitName: product.unitName,
    purchasePrice: product.purchasePrice,
    salePrice: product.salePrice,
    status: product.status
  }))
})
const selectedProduct = computed(() => selectableProducts.value.find((product) => product.id === form.productId))
const quantityMax = computed(() => {
  if (isReturn.value) return selectedProduct.value?.remainingQuantity
  if (config.value.api === 'sales-outbound' || isTransfer.value) return selectedProduct.value?.availableQuantity
  return undefined
})
const keywordPlaceholder = computed(() => isTransfer.value ? '请输入库存调拨单号查询' : `请输入${config.value.name}单号查询`)
const statusOptions = computed(() => isTransfer.value
  ? [
      { value: 'DRAFT', label: '未调拨' },
      { value: 'PENDING', label: '待调拨' },
      { value: 'APPROVED', label: '已调拨' },
      { value: 'REJECTED', label: '无法调拨' }
    ]
  : [
      { value: 'DRAFT', label: '待提交' },
      { value: 'PENDING', label: '待审核' },
      { value: 'APPROVED', label: '审核通过' },
      { value: 'REJECTED', label: '审核拒绝' }
    ])
const filteredRows = computed(() => rows.value.filter((row) =>
  matchRecordByKeyword(row, query.keyword) &&
  matchesText(row.documentNo, query.documentNo) &&
  matchesText(row.warehouseCode, query.warehouseCode) &&
  matchesText(row.warehouseName, query.warehouseName) &&
  (isTransfer.value || matchesText(row.partnerCode, query.partnerCode)) &&
  (isTransfer.value || matchesText(row.partnerName, query.partnerName)) &&
  (!isTransfer.value || matchesText(row.targetWarehouseCode, query.targetWarehouseCode)) &&
  (!isTransfer.value || matchesText(row.targetWarehouseName, query.targetWarehouseName)) &&
  matchesText(row.items?.length, query.itemCount) &&
  matchesText(row.totalAmount, query.totalAmount) &&
  (!config.value.showRelated || matchesText(row.relatedDocumentNo, query.relatedDocumentNo)) &&
  (!query.status || row.status === query.status) &&
  matchesText(row.creatorName, query.creatorName) &&
  inDateRange(row.operationTime, query.operationDateRange) &&
  inDateRange(row.auditTime, query.auditDateRange)
))
const pagedRows = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredRows.value.slice(start, start + pageSize.value)
})
const formTitle = computed(() => `${editingId.value ? '修改' : '新增'}${config.value.name}单`)
const formRules = computed<FormRules>(() => ({
  warehouseId: [
    { required: true, message: isTransfer.value ? '调出仓库必填，请重新输入。' : '仓库必填，请重新输入。', trigger: 'change' },
    {
      validator: (_rule: unknown, value: unknown, callback: (error?: Error) => void) => {
        if (isReturn.value) return callback()
        validateEnabledMaster(associationOptions.warehouses, value, isTransfer.value ? '调出仓库' : '仓库', callback)
      },
      trigger: 'change'
    }
  ],
  targetWarehouseId: [
    { required: isTransfer.value, message: '调入仓库必填，请重新输入。', trigger: 'change' },
    {
      validator: (_rule: unknown, value: unknown, callback: (error?: Error) => void) => {
        if (!isTransfer.value) return callback()
        validateEnabledMaster(associationOptions.warehouses, value, '调入仓库', callback)
      },
      trigger: 'change'
    }
  ],
  partnerId: [
    { required: !isTransfer.value, message: `${config.value.partnerIdLabel}必填，请重新输入。`, trigger: 'change' },
    {
      validator: (_rule: unknown, value: unknown, callback: (error?: Error) => void) => {
        if (isTransfer.value || isReturn.value) return callback()
        validateEnabledMaster(associationOptions.partners, value, config.value.partnerName, callback)
      },
      trigger: 'change'
    }
  ],
  relatedDocumentNo: [{ required: isReturn.value, message: config.value.relatedRequiredMessage, trigger: 'change' }],
  productId: [
    { required: true, message: '商品必填，请重新输入。', trigger: 'change' },
    {
      validator: (_rule: unknown, value: unknown, callback: (error?: Error) => void) => {
        if (isReturn.value) return callback()
        validateEnabledMaster(associationOptions.products, value, '商品', callback)
      },
      trigger: 'change'
    }
  ],
  quantity: [{ required: true, message: `${config.value.quantityLabel}必填，请重新输入。`, trigger: 'change' }],
  price: [{ required: !isTransfer.value, message: `${config.value.priceLabel}必填，请重新输入。`, trigger: 'change' }],
  remark: [{ required: isTransfer.value, message: '备注必填，请重新输入。', trigger: 'blur' }]
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
  Object.assign(form, emptyDocumentForm())
}

function masterOptionLabel(option: MasterRecord) {
  const disabledLabel = option.status === 'ENABLED' ? '' : ' · 已停用'
  return `${option.code} · ${option.name}${disabledLabel}`
}

function productOptionLabel(product: ProductSelectOption) {
  const stockLabel = product.remainingQuantity !== undefined
    ? ` · 剩余 ${product.remainingQuantity}`
    : product.availableQuantity !== undefined
      ? ` · 可用 ${product.availableQuantity}`
      : ''
  const disabledLabel = product.status === undefined || product.status === 'ENABLED' ? '' : ' · 已停用'
  return `${product.code} · ${product.name}${stockLabel}${disabledLabel}`
}

function validateEnabledMaster(
  options: MasterRecord[],
  value: unknown,
  label: string,
  callback: (error?: Error) => void
) {
  if (value === null || value === undefined || value === '') return callback()
  const option = options.find((item) => Number(item.id) === Number(value))
  if (!option || option.status !== 'ENABLED') {
    return callback(new Error(`${label}已停用或不可用，请重新选择。`))
  }
  callback()
}

function handleWarehouseChange() {
  form.productId = undefined
  form.quantity = undefined
  if (form.targetWarehouseId === form.warehouseId) form.targetWarehouseId = undefined
}

function handleReturnSourceChange() {
  const source = selectedReturnSource.value
  form.warehouseId = source?.warehouseId
  form.partnerId = source?.partnerId
  form.productId = undefined
  form.quantity = undefined
  form.price = undefined
}

function handleProductChange() {
  form.quantity = undefined
  const product = selectedProduct.value
  if (!product || isTransfer.value) {
    form.price = undefined
    return
  }
  if (isReturn.value) {
    form.price = product.price
    return
  }
  const suggestedPrice = config.value.api === 'sales-outbound' ? product.salePrice : product.purchasePrice
  form.price = suggestedPrice === null || suggestedPrice === undefined ? undefined : Number(suggestedPrice)
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

function inDateRange(value: string | null | undefined, range: string[]) {
  if (!range?.length || range.length !== 2) return true
  if (!value) return false
  const day = value.slice(0, 10)
  return day >= range[0] && day <= range[1]
}

async function load() {
  rows.value = await listDocuments(config.value.api)
  currentPage.value = 1
}

async function loadFormOptions(editingDocument?: DocumentRecord) {
  associationOptions.warehouses = []
  associationOptions.partners = []
  associationOptions.products = []
  associationOptions.stock = []
  returnOptions.value = []
  if (isReturn.value) {
    returnOptions.value = await listReturnOptions(config.value.api, editingDocument?.id)
    return
  }
  const partnerType = config.value.api === 'purchase-inbound' ? 'supplier' : 'customer'
  const needsStock = config.value.api === 'sales-outbound' || isTransfer.value
  const masterParams: Record<string, string> = editingDocument ? {} : { status: 'ENABLED' }
  const [warehouses, partners, products, stock] = await Promise.all([
    listMaster('warehouse', masterParams),
    isTransfer.value ? Promise.resolve([] as MasterRecord[]) : listMaster(partnerType, masterParams),
    listMaster('product', masterParams),
    needsStock ? listStock(editingDocument?.id) : Promise.resolve([] as StockRow[])
  ])
  const selectedWarehouseIds = editingDocument
    ? [editingDocument.warehouseId, editingDocument.targetWarehouseId]
        .filter((id): id is number => id !== null && id !== undefined)
    : undefined
  const selectedProductId = editingDocument?.items?.[0]?.productId
  associationOptions.warehouses = dedupeMasterOptions(warehouses, selectedWarehouseIds)
  associationOptions.partners = dedupeMasterOptions(partners, editingDocument?.partnerId)
  associationOptions.products = dedupeMasterOptions(products, selectedProductId)
  associationOptions.stock = stock
}

function resetQuery() {
  query.keyword = ''
  query.documentNo = ''
  query.warehouseCode = ''
  query.warehouseName = ''
  query.targetWarehouseCode = ''
  query.targetWarehouseName = ''
  query.partnerCode = ''
  query.partnerName = ''
  query.itemCount = ''
  query.totalAmount = ''
  query.relatedDocumentNo = ''
  query.status = ''
  query.creatorName = ''
  query.operationDateRange = []
  query.auditDateRange = []
  load()
}

async function openCreate() {
  editingId.value = null
  resetForm()
  await loadFormOptions()
  formVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()
}

async function openEdit(row: DocumentRecord) {
  editingId.value = row.id
  resetForm()
  await loadFormOptions(row)
  const item = row.items?.[0]
  Object.assign(form, {
    warehouseId: row.warehouseId,
    targetWarehouseId: row.targetWarehouseId ?? undefined,
    partnerId: row.partnerId ?? undefined,
    relatedDocumentNo: row.relatedDocumentNo || '',
    productId: item?.productId,
    quantity: item ? Number(item.quantity) : undefined,
    price: item ? Number(item.price) : undefined,
    remark: item?.remark || ''
  })
  formVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()
}

async function saveDocument() {
  await formRef.value?.validate()
  if (editingId.value) {
    await updateDocument(config.value.api, editingId.value, form)
  } else {
    await createDocument(config.value.api, form)
  }
  ElMessage.success('保存成功')
  formVisible.value = false
  await load()
}

async function submit(row: DocumentRecord) {
  await ElMessageBox.confirm('确认提交该单据？', '提交确认', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
  await submitDocument(config.value.api, row.id)
  ElMessage.success('提交成功')
  await load()
}

async function remove(row: DocumentRecord) {
  await ElMessageBox.confirm('确认删除该单据？', '删除确认', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
  await deleteDocument(config.value.api, row.id)
  ElMessage.success('删除成功')
  await load()
}

async function view(row: DocumentRecord) {
  current.value = await documentDetail(config.value.api, row.id)
  detailVisible.value = true
}

async function openSourceDetail(documentNo: string) {
  const sources = await listDocuments(sourceDocumentApi.value)
  const source = sources.find((document) => document.documentNo === documentNo)
  if (!source) {
    ElMessage.error('关联原单不存在或无权查看')
    return
  }
  sourceDetail.value = await documentDetail(sourceDocumentApi.value, source.id)
  sourceDetailVisible.value = true
}

watch(() => route.fullPath, load)
watch([
  () => query.keyword,
  () => query.documentNo,
  () => query.warehouseCode,
  () => query.warehouseName,
  () => query.targetWarehouseCode,
  () => query.targetWarehouseName,
  () => query.partnerCode,
  () => query.partnerName,
  () => query.itemCount,
  () => query.totalAmount,
  () => query.relatedDocumentNo,
  () => query.status,
  () => query.creatorName,
  () => query.operationDateRange.join('|'),
  () => query.auditDateRange.join('|'),
  pageSize
], () => {
  currentPage.value = 1
})
onMounted(load)
</script>

<style scoped>
.related-source-control {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 36px;
  gap: 8px;
  width: 100%;
}

.source-detail-link {
  margin-left: 8px;
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
