<template>
  <div class="page">
    <div class="table-panel">
      <div class="toolbar">
        <el-input v-model="query.keyword" :placeholder="`请输入${config.name}编号/名称查询`" clearable style="width: 260px" />
        <template v-if="type === 'product'">
          <span class="filter-label">商品分类</span>
          <el-input v-model="query.categoryName" placeholder="请输入商品分类" clearable style="width: 170px" />
          <span class="filter-label">商品品牌</span>
          <el-input v-model="query.brandName" placeholder="请输入商品品牌" clearable style="width: 170px" />
        </template>
        <el-select v-model="query.status" placeholder="请选择状态" clearable style="width: 150px">
          <el-option label="启用" value="ENABLED" />
          <el-option label="禁用" value="DISABLED" />
        </el-select>
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="reset">重置</el-button>
        <el-button type="success" @click="openCreate">新增</el-button>
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
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
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
      <el-form :model="form" label-width="110px">
        <el-form-item :label="`${config.name}名称`"><el-input v-model="form.name" :placeholder="`请输入${config.name}名称`" /></el-form-item>
        <template v-if="type === 'product'">
          <el-form-item label="商品分类"><el-input v-model="form.categoryName" placeholder="请输入商品分类" /></el-form-item>
          <el-form-item label="商品品牌"><el-input v-model="form.brandName" placeholder="请输入商品品牌" /></el-form-item>
          <el-form-item label="商品单位"><el-input v-model="form.unitName" placeholder="请输入商品单位" /></el-form-item>
          <el-form-item label="建议采购价"><el-input v-model="form.purchasePrice" placeholder="请输入建议采购价" /></el-form-item>
          <el-form-item label="建议零售价"><el-input v-model="form.salePrice" placeholder="请输入建议零售价" /></el-form-item>
          <el-form-item label="商品图片">
            <div class="image-upload-box">
              <el-image v-if="form.imageData" :src="form.imageData" class="image-preview" fit="cover" />
              <div v-else class="empty-preview">无图片</div>
              <div class="image-actions">
                <el-upload
                  :auto-upload="false"
                  :show-file-list="false"
                  accept=".jpg,.jpeg,.png,image/jpeg,image/png"
                  :on-change="handleProductImageChange"
                >
                  <el-button type="primary">{{ form.imageData ? '更换图片' : '上传图片' }}</el-button>
                </el-upload>
                <el-button v-if="form.imageData" @click="removeProductImage">删除图片</el-button>
              </div>
            </div>
          </el-form-item>
        </template>
        <template v-if="['warehouse', 'customer', 'supplier'].includes(type)">
          <el-form-item label="联系人"><el-input v-model="form.contact" placeholder="请输入联系人" /></el-form-item>
          <el-form-item label="联系电话"><el-input v-model="form.phone" placeholder="请输入联系电话" /></el-form-item>
          <el-form-item label="地址"><el-input v-model="form.address" placeholder="请输入地址" /></el-form-item>
        </template>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="save">保存</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="importVisible" title="批量导入" width="680px">
      <el-form label-width="100px">
        <el-form-item label="上传文件">
          <el-upload
            :auto-upload="false"
            :show-file-list="false"
            accept=".csv,.txt,.xls,.xlsx"
            :on-change="handleImportFileChange"
          >
            <el-button type="primary">上传文件</el-button>
          </el-upload>
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
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { UploadFile } from 'element-plus'
import { changeMasterStatus, createMaster, importProducts, listMaster, updateMaster } from '../api'
import { readValidDocumentImage } from '../utils/imageUpload'

const route = useRoute()
const rows = ref<any[]>([])
const dialogVisible = ref(false)
const importVisible = ref(false)
const editingId = ref<number | null>(null)
const query = reactive({ keyword: '', status: '', categoryName: '', brandName: '' })
const form = reactive<Record<string, string>>({})
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
const filteredRows = computed(() => rows.value.filter((row) => {
  const matchCategory = type.value !== 'product' || !query.categoryName || String(row.categoryName || '').includes(query.categoryName)
  const matchBrand = type.value !== 'product' || !query.brandName || String(row.brandName || '').includes(query.brandName)
  return matchCategory && matchBrand
}))
const pagedRows = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredRows.value.slice(start, start + pageSize.value)
})

async function load() {
  rows.value = await listMaster(type.value, query)
  currentPage.value = 1
}

function reset() {
  query.keyword = ''
  query.status = ''
  query.categoryName = ''
  query.brandName = ''
  load()
}

function openCreate() {
  editingId.value = null
  Object.keys(form).forEach((key) => delete form[key])
  if (type.value === 'product') {
    form.imageData = ''
  }
  dialogVisible.value = true
}

function openEdit(row: any) {
  editingId.value = row.id
  Object.assign(form, row)
  dialogVisible.value = true
}

async function save() {
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

async function toggle(row: any) {
  const next = row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  await ElMessageBox.confirm(`确认${next === 'ENABLED' ? '启用' : '禁用'}该数据？`, '操作确认', { confirmButtonText: '确定', cancelButtonText: '取消' })
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
watch([() => query.categoryName, () => query.brandName, pageSize], () => {
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

.empty-preview {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px dashed #dcdfe6;
  color: #909399;
  font-size: 12px;
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
</style>
