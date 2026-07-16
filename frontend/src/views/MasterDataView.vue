<template>
  <div class="page">
    <div class="table-panel">
      <div class="toolbar query-toolbar">
        <el-input v-model="query.keyword" placeholder="综合查询：任意字段" clearable style="width: 220px" />
        <el-input v-model="query.code" :placeholder="`请输入${config.name}编号`" clearable style="width: 180px" />
        <el-input v-model="query.name" :placeholder="`请输入${config.name}名称`" clearable style="width: 180px" />
        <el-select v-model="query.status" placeholder="请选择状态" clearable style="width: 150px">
          <el-option label="启用" value="ENABLED" />
          <el-option label="禁用" value="DISABLED" />
        </el-select>
        <span class="query-break" aria-hidden="true"></span>
        <template v-if="type === 'product'">
          <el-input v-model="query.categoryName" placeholder="请输入商品分类" clearable style="width: 170px" />
          <el-input v-model="query.brandName" placeholder="请输入商品品牌" clearable style="width: 170px" />
          <el-input v-model="query.unitName" placeholder="请输入商品单位" clearable style="width: 170px" />
          <el-input v-model="query.purchasePrice" placeholder="请输入建议采购价" clearable style="width: 170px" />
          <el-input v-model="query.salePrice" placeholder="请输入建议零售价" clearable style="width: 170px" />
        </template>
        <template v-else>
          <el-input v-model="query.phone" placeholder="请输入联系电话" clearable style="width: 170px" />
          <el-input v-model="query.address" placeholder="请输入地址" clearable style="width: 220px" />
        </template>
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
        <el-date-picker
          v-model="query.updateDateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          start-placeholder="更新开始日期"
          end-placeholder="更新结束日期"
          range-separator="至"
          style="width: 250px"
        />
        <span class="query-break" aria-hidden="true"></span>
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="reset">重置</el-button>
        <el-button type="primary" plain @click="openCreate">+ 新增</el-button>
        <el-button v-if="type === 'product'" @click="openImport">批量导入</el-button>
        <el-button v-if="type === 'product'" @click="downloadTemplate">下载模板</el-button>
      </div>
      <el-table :data="pagedRows" border empty-text="暂无数据">
        <el-table-column type="index" label="序号" width="70" />
        <el-table-column prop="code" label="编号" min-width="130" />
        <el-table-column v-if="type === 'product'" label="商品图片" width="100">
          <template #default="{ row }">
            <el-image
              v-if="row.imageData"
              :src="row.imageData"
              class="product-thumb"
              fit="cover"
              :preview-src-list="[row.imageData]"
              preview-teleported
            />
            <span v-else>无图片</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" :label="`${config.name}名称`" min-width="150" />
        <el-table-column v-if="type === 'product'" prop="categoryName" label="商品分类" />
        <el-table-column v-if="type === 'product'" prop="brandName" label="商品品牌" />
        <el-table-column v-if="type === 'product'" prop="unitName" label="商品单位" />
        <el-table-column v-if="type === 'product'" prop="purchasePrice" label="建议采购价（元）" width="160" />
        <el-table-column v-if="type === 'product'" prop="salePrice" label="建议零售价（元）" width="160" />
        <el-table-column v-if="type !== 'product'" prop="phone" label="联系电话" />
        <el-table-column v-if="type !== 'product'" prop="address" label="地址" min-width="160" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'">{{ row.status === 'ENABLED' ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="openDetail(row)">
              <el-icon><View /></el-icon>
              详情
            </el-button>
            <el-button text type="primary" @click="openEdit(row)">修改</el-button>
            <el-button text type="warning" @click="toggle(row)">{{ row.status === 'ENABLED' ? '禁用' : '启用' }}</el-button>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="120px">
        <el-form-item :label="`${config.name}名称`" prop="name"><el-input v-model="form.name" :placeholder="`请输入${config.name}名称`" /></el-form-item>
        <template v-if="type === 'product'">
          <el-form-item label="商品分类" prop="categoryName">
            <el-select v-model="form.categoryName" filterable placeholder="请选择商品分类" style="width: 100%">
              <el-option
                v-for="option in productReferenceOptions.category"
                :key="option.id"
                :label="referenceOptionLabel(option)"
                :value="option.name"
                :disabled="option.status !== 'ENABLED'"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="商品品牌" prop="brandName">
            <el-select v-model="form.brandName" filterable placeholder="请选择商品品牌" style="width: 100%">
              <el-option
                v-for="option in productReferenceOptions.brand"
                :key="option.id"
                :label="referenceOptionLabel(option)"
                :value="option.name"
                :disabled="option.status !== 'ENABLED'"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="商品单位" prop="unitName">
            <el-select v-model="form.unitName" filterable placeholder="请选择商品单位" style="width: 100%">
              <el-option
                v-for="option in productReferenceOptions.unit"
                :key="option.id"
                :label="referenceOptionLabel(option)"
                :value="option.name"
                :disabled="option.status !== 'ENABLED'"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="建议采购价（元）" prop="purchasePrice">
            <el-input-number v-model="form.purchasePrice" :min="0" :precision="2" :step="1" controls-position="right" style="width: 100%" />
          </el-form-item>
          <el-form-item label="建议零售价（元）" prop="salePrice">
            <el-input-number v-model="form.salePrice" :min="0" :precision="2" :step="1" controls-position="right" style="width: 100%" />
          </el-form-item>
          <el-form-item label="商品图片">
            <div class="image-upload-box image-upload-box-large">
              <el-upload
                :auto-upload="false"
                :show-file-list="false"
                accept=".jpg,.jpeg,.png,image/jpeg,image/png"
                :on-change="handleProductImageChange"
              >
                <div class="product-image-uploader">
                  <el-image v-if="form.imageData" :src="form.imageData" class="image-preview" fit="cover" />
                  <div v-else class="empty-preview">
                    <el-icon><Plus /></el-icon>
                    <span>上传图片</span>
                  </div>
                </div>
              </el-upload>
              <el-button v-if="form.imageData" @click="removeProductImage">删除图片</el-button>
            </div>
          </el-form-item>
        </template>
        <template v-if="['warehouse', 'customer', 'supplier'].includes(type)">
          <el-form-item label="联系人" prop="contact"><el-input v-model="form.contact" placeholder="请输入联系人" /></el-form-item>
          <el-form-item label="联系电话" prop="phone"><el-input v-model="form.phone" placeholder="请输入联系电话" /></el-form-item>
          <el-form-item label="地址" prop="address"><el-input v-model="form.address" placeholder="请输入地址" /></el-form-item>
        </template>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="save">保存</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" :title="`${config.name}详情`" width="680px">
      <div v-if="detailRow" class="master-detail">
        <div class="master-detail-header">
          <el-image
            v-if="type === 'product' && detailRow.imageData"
            :src="detailRow.imageData"
            class="master-detail-image"
            fit="cover"
            :preview-src-list="[detailRow.imageData]"
            preview-teleported
          />
          <div class="master-detail-heading">
            <strong>{{ detailRow.name }}</strong>
            <span>{{ detailRow.code || '暂无编号' }}</span>
          </div>
          <el-tag :type="detailRow.status === 'ENABLED' ? 'success' : 'info'">
            {{ detailRow.status === 'ENABLED' ? '启用' : '禁用' }}
          </el-tag>
        </div>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="编号">{{ detailRow.code || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="`${config.name}名称`">{{ detailRow.name }}</el-descriptions-item>
          <template v-if="type === 'product'">
            <el-descriptions-item label="商品分类">{{ detailRow.categoryName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="商品品牌">{{ detailRow.brandName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="商品单位">{{ detailRow.unitName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="建议采购价">{{ formatMoney(detailRow.purchasePrice) }}</el-descriptions-item>
            <el-descriptions-item label="建议零售价">{{ formatMoney(detailRow.salePrice) }}</el-descriptions-item>
          </template>
          <template v-if="['warehouse', 'customer', 'supplier'].includes(type)">
            <el-descriptions-item label="联系人">{{ detailRow.contact || '-' }}</el-descriptions-item>
            <el-descriptions-item label="联系电话">{{ detailRow.phone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="地址" :span="2">{{ detailRow.address || '-' }}</el-descriptions-item>
          </template>
          <el-descriptions-item label="创建时间">{{ formatDateTime(detailRow.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ formatDateTime(detailRow.updateTime) }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button type="primary" @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="importVisible" title="批量导入" width="420px">
      <el-form label-width="100px">
        <el-form-item label="批量导入文件">
          <el-upload
            :auto-upload="false"
            :show-file-list="false"
            accept=".csv,.txt,.xls,.xlsx"
            :on-change="handleImportFileChange"
          >
            <div class="doc-upload-drop">
              <div class="doc-upload-inner">
                <el-icon class="doc-upload-icon"><UploadFilled /></el-icon>
                <div>将文件拖到此处，或 <span class="doc-upload-link">点击上传</span></div>
              </div>
            </div>
          </el-upload>
          <div class="import-template-link" @click="downloadTemplate">下载模板</div>
          <div class="dialog-tip">提示：仅允许导入“xls”或“xlsx”格式文件！</div>
        </el-form-item>
        <el-form-item label="导入内容">
          <el-input
            v-model="importText"
            type="textarea"
            :rows="8"
            placeholder="商品名称	商品分类	商品品牌	商品单位	建议采购价（元）	建议零售价（元）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="importVisible = false">取消</el-button>
        <el-button type="primary" @click="saveImport">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules, UploadFile } from 'element-plus'
import { Plus, UploadFilled, View } from '@element-plus/icons-vue'
import {
  changeMasterStatus,
  createMaster,
  importProducts,
  listMaster,
  updateMaster,
  type MasterRecord
} from '../api'
import { dedupeMasterOptions } from '../utils/businessOptions'
import { readValidDocumentImage } from '../utils/imageUpload'

interface MasterForm {
  name: string
  categoryName: string
  brandName: string
  unitName: string
  purchasePrice?: number
  salePrice?: number
  imageData: string
  contact: string
  phone: string
  address: string
  [key: string]: string | number | undefined
}

function emptyMasterForm(): MasterForm {
  return {
    name: '',
    categoryName: '',
    brandName: '',
    unitName: '',
    purchasePrice: undefined,
    salePrice: undefined,
    imageData: '',
    contact: '',
    phone: '',
    address: ''
  }
}

const route = useRoute()
const rows = ref<MasterRecord[]>([])
const dialogVisible = ref(false)
const detailVisible = ref(false)
const detailRow = ref<MasterRecord | null>(null)
const importVisible = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const productReferenceOptions = reactive({
  category: [] as MasterRecord[],
  brand: [] as MasterRecord[],
  unit: [] as MasterRecord[]
})
const query = reactive({
  keyword: '',
  code: '',
  name: '',
  status: '',
  categoryName: '',
  brandName: '',
  unitName: '',
  purchasePrice: '',
  salePrice: '',
  phone: '',
  address: '',
  createDateRange: [] as string[],
  updateDateRange: [] as string[]
})
const form = reactive<MasterForm>(emptyMasterForm())
const importText = ref('')
const currentPage = ref(1)
const pageSize = ref(10)

const configs: Record<string, { name: string }> = {
  brand: { name: '商品品牌' },
  category: { name: '商品分类' },
  unit: { name: '商品单位' },
  product: { name: '商品' },
  warehouse: { name: '仓库' },
  customer: { name: '客户' },
  supplier: { name: '供应商' }
}

const type = computed(() => String(route.params.type))
const config = computed(() => configs[type.value] || { name: '资料' })
const dialogTitle = computed(() => `${editingId.value ? '修改' : '新增'}${config.value.name}`)
const formRules = computed<FormRules>(() => ({
  name: [{ required: true, message: `${config.value.name}名称必填，请重新输入。`, trigger: 'blur' }],
  categoryName: [
    { required: type.value === 'product', message: '商品分类必填，请重新输入。', trigger: 'change' },
    {
      validator: (_rule: unknown, value: unknown, callback: (error?: Error) => void) =>
        validateEnabledReference(productReferenceOptions.category, value, '商品分类', callback),
      trigger: 'change'
    }
  ],
  brandName: [
    { required: type.value === 'product', message: '商品品牌必填，请重新输入。', trigger: 'change' },
    {
      validator: (_rule: unknown, value: unknown, callback: (error?: Error) => void) =>
        validateEnabledReference(productReferenceOptions.brand, value, '商品品牌', callback),
      trigger: 'change'
    }
  ],
  unitName: [
    { required: type.value === 'product', message: '商品单位必填，请重新输入。', trigger: 'change' },
    {
      validator: (_rule: unknown, value: unknown, callback: (error?: Error) => void) =>
        validateEnabledReference(productReferenceOptions.unit, value, '商品单位', callback),
      trigger: 'change'
    }
  ],
  purchasePrice: [{ required: type.value === 'product', message: '建议采购价必填，请重新输入。', trigger: 'blur' }],
  salePrice: [{ required: type.value === 'product', message: '建议零售价必填，请重新输入。', trigger: 'blur' }],
  contact: [{ required: ['warehouse', 'customer', 'supplier'].includes(type.value), message: '联系人必填，请重新输入。', trigger: 'blur' }],
  phone: [{ required: ['warehouse', 'customer', 'supplier'].includes(type.value), message: '联系电话必填，请重新输入。', trigger: 'blur' }],
  address: [{ required: ['warehouse', 'customer', 'supplier'].includes(type.value), message: '地址必填，请重新输入。', trigger: 'blur' }]
}))
const filteredRows = computed(() => rows.value.filter((row) => {
  const matchKeyword = matchRecordByKeyword(row, query.keyword)
  const matchCode = matchesText(row.code, query.code)
  const matchName = matchesText(row.name, query.name)
  const matchStatus = !query.status || row.status === query.status
  const matchCategory = type.value !== 'product' || matchesText(row.categoryName, query.categoryName)
  const matchBrand = type.value !== 'product' || matchesText(row.brandName, query.brandName)
  const matchUnit = type.value !== 'product' || matchesText(row.unitName, query.unitName)
  const matchPurchasePrice = type.value !== 'product' || matchesText(row.purchasePrice, query.purchasePrice)
  const matchSalePrice = type.value !== 'product' || matchesText(row.salePrice, query.salePrice)
  const matchPhone = type.value === 'product' || matchesText(row.phone, query.phone)
  const matchAddress = type.value === 'product' || matchesText(row.address, query.address)
  const matchCreateDate = inDateRange(row.createTime, query.createDateRange)
  const matchUpdateDate = inDateRange(row.updateTime, query.updateDateRange)
  return matchKeyword && matchCode && matchName && matchStatus && matchCategory && matchBrand && matchUnit && matchPurchasePrice && matchSalePrice && matchPhone && matchAddress && matchCreateDate && matchUpdateDate
}))
const pagedRows = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredRows.value.slice(start, start + pageSize.value)
})

async function load() {
  rows.value = await listMaster(type.value, { status: query.status })
  currentPage.value = 1
}

async function loadProductReferences(selectedProduct?: MasterRecord) {
  const params: Record<string, string> = selectedProduct ? {} : { status: 'ENABLED' }
  const [categories, brands, units] = await Promise.all([
    listMaster('category', params),
    listMaster('brand', params),
    listMaster('unit', params)
  ])
  productReferenceOptions.category = dedupeMasterOptions(categories, undefined, selectedProduct?.categoryName || undefined)
  productReferenceOptions.brand = dedupeMasterOptions(brands, undefined, selectedProduct?.brandName || undefined)
  productReferenceOptions.unit = dedupeMasterOptions(units, undefined, selectedProduct?.unitName || undefined)
}

function reset() {
  query.keyword = ''
  query.code = ''
  query.name = ''
  query.status = ''
  query.categoryName = ''
  query.brandName = ''
  query.unitName = ''
  query.purchasePrice = ''
  query.salePrice = ''
  query.phone = ''
  query.address = ''
  query.createDateRange = []
  query.updateDateRange = []
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

function inDateRange(value: string | null | undefined, range: string[]) {
  if (!range?.length || range.length !== 2) return true
  if (!value) return false
  const day = value.slice(0, 10)
  return day >= range[0] && day <= range[1]
}

function referenceOptionLabel(option: MasterRecord) {
  const codeLabel = option.code ? `${option.name} · ${option.code}` : option.name
  return option.status === 'ENABLED' ? codeLabel : `${codeLabel} · 已停用`
}

function validateEnabledReference(
  options: MasterRecord[],
  value: unknown,
  label: string,
  callback: (error?: Error) => void
) {
  if (type.value !== 'product' || value === null || value === undefined || value === '') return callback()
  const option = options.find((item) => item.name === String(value))
  if (!option || option.status !== 'ENABLED') {
    return callback(new Error(`${label}已停用或不可用，请重新选择。`))
  }
  callback()
}

function formatMoney(value: string | number | null | undefined) {
  if (value === null || value === undefined || value === '') return '-'
  return `¥${Number(value).toFixed(2)}`
}

function formatDateTime(value: string | null | undefined) {
  return value ? value.replace('T', ' ').slice(0, 19) : '-'
}

async function openCreate() {
  editingId.value = null
  Object.assign(form, emptyMasterForm())
  if (type.value === 'product') await loadProductReferences()
  dialogVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()
}

async function openEdit(row: MasterRecord) {
  editingId.value = row.id
  Object.assign(form, emptyMasterForm(), row, {
    purchasePrice: row.purchasePrice === null || row.purchasePrice === undefined ? undefined : Number(row.purchasePrice),
    salePrice: row.salePrice === null || row.salePrice === undefined ? undefined : Number(row.salePrice)
  })
  if (type.value === 'product') await loadProductReferences(row)
  dialogVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()
}

function openDetail(row: MasterRecord) {
  detailRow.value = row
  detailVisible.value = true
}

async function save() {
  await formRef.value?.validate()
  if (editingId.value) {
    await updateMaster(type.value, editingId.value, form)
  } else {
    await createMaster(type.value, form)
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  await load()
}

async function handleProductImageChange(file: UploadFile) {
  const imageData = await readValidDocumentImage(file.raw)
  if (!imageData) return
  form.imageData = imageData
}

function removeProductImage() {
  form.imageData = ''
}

async function toggle(row: MasterRecord) {
  const next = row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  await ElMessageBox.confirm(`确认${next === 'ENABLED' ? '启用' : '禁用'}该数据？`, '操作确认', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
  await changeMasterStatus(type.value, row.id, next)
  ElMessage.success('操作成功')
  await load()
}

function openImport() {
  importText.value = ''
  importVisible.value = true
}

function downloadTemplate() {
  const header = '商品名称\t商品分类\t商品品牌\t商品单位\t建议采购价（元）\t建议零售价（元）'
  const example = '示例商品\t办公设备\t连想\t台\t1000\t1200'
  const blob = new Blob([`${header}\n${example}`], { type: 'text/plain;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = '商品批量导入模板.txt'
  link.click()
  URL.revokeObjectURL(url)
  ElMessage.success('模板下载成功')
}

async function handleImportFileChange(file: UploadFile) {
  if (!file.raw) return
  importText.value = await file.raw.text()
}

function parseImportRows() {
  const lines = importText.value.split(/\r?\n/).map((line) => line.trim()).filter(Boolean)
  if (lines.length <= 1) return []
  const delimiter = lines[0].includes('\t') ? '\t' : ','
  const headers = lines[0].split(delimiter).map((item) => item.trim())
  return lines.slice(1).map((line) => {
    const values = line.split(delimiter).map((item) => item.trim())
    const row: Record<string, string> = {}
    headers.forEach((header, index) => {
      row[header] = values[index] || ''
    })
    return {
      name: row['商品名称'] || row.name || '',
      categoryName: row['商品分类'] || row.categoryName || '',
      brandName: row['商品品牌'] || row.brandName || '',
      unitName: row['商品单位'] || row.unitName || '',
      purchasePrice: row['建议采购价（元）'] || row.purchasePrice || '',
      salePrice: row['建议零售价（元）'] || row.salePrice || ''
    }
  })
}

async function saveImport() {
  const importRows = parseImportRows()
  await importProducts(importRows)
  ElMessage.success('导入成功')
  importVisible.value = false
  await load()
}

watch(type, load)
watch([
  () => query.keyword,
  () => query.code,
  () => query.name,
  () => query.status,
  () => query.categoryName,
  () => query.brandName,
  () => query.unitName,
  () => query.purchasePrice,
  () => query.salePrice,
  () => query.phone,
  () => query.address,
  () => query.createDateRange.join('|'),
  () => query.updateDateRange.join('|'),
  pageSize
], () => {
  currentPage.value = 1
})
onMounted(load)
</script>

<style scoped>
.product-thumb,
.image-preview,
.empty-preview {
  width: 64px;
  height: 64px;
  border-radius: 6px;
}

.product-thumb {
  display: block;
}

.image-upload-box {
  display: flex;
  align-items: center;
  gap: 12px;
}

.image-upload-box-large {
  align-items: flex-start;
}

.product-image-uploader,
.empty-preview {
  width: 120px;
  height: 120px;
  border: 1px dashed #dcdfe6;
  border-radius: 2px;
}

.product-image-uploader {
  cursor: pointer;
}

.product-image-uploader .image-preview {
  width: 100%;
  height: 100%;
  border-radius: 2px;
}

.empty-preview {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: #909399;
  font-size: 12px;
}

.empty-preview .el-icon {
  font-size: 28px;
  color: #8c9aad;
}

.image-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
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

.master-detail {
  display: grid;
  gap: 18px;
}

.master-detail-header {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 14px;
}

.master-detail-image {
  width: 72px;
  height: 72px;
  border-radius: 6px;
}

.master-detail-heading {
  display: grid;
  gap: 5px;
  min-width: 0;
}

.master-detail-heading strong {
  overflow-wrap: anywhere;
  color: #1f2d3d;
  font-size: 18px;
}

.master-detail-heading span {
  color: #7b8794;
  font-size: 13px;
}

.import-template-link {
  width: 100%;
  margin-top: 10px;
  color: #606266;
  cursor: pointer;
}
</style>
