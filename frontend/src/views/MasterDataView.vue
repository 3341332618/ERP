<template>
  <div class="page">
    <div class="table-panel">
      <div class="toolbar">
        <el-input v-model="query.keyword" :placeholder="`请输入${config.name}编号/名称查询`" clearable style="width: 260px" />
        <el-select v-model="query.status" placeholder="请选择状态" clearable style="width: 150px">
          <el-option label="启用" value="ENABLED" />
          <el-option label="禁用" value="DISABLED" />
        </el-select>
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="reset">重置</el-button>
        <el-button type="success" @click="openCreate">新增</el-button>
      </div>
      <el-table :data="rows" border empty-text="暂无数据">
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
        <el-table-column v-if="type === 'product'" prop="purchasePrice" label="建议采购价（元）" width="140" />
        <el-table-column v-if="type === 'product'" prop="salePrice" label="建议零售价（元）" width="140" />
        <el-table-column v-if="type !== 'product'" prop="phone" label="联系电话" />
        <el-table-column v-if="type !== 'product'" prop="address" label="地址" min-width="160" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'">{{ row.status === 'ENABLED' ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="openEdit(row)">修改</el-button>
            <el-button text type="warning" @click="toggle(row)">{{ row.status === 'ENABLED' ? '禁用' : '启用' }}</el-button>
          </template>
        </el-table-column>
      </el-table>
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
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { UploadFile } from 'element-plus'
import { changeMasterStatus, createMaster, listMaster, updateMaster } from '../api'
import { readValidDocumentImage } from '../utils/imageUpload'

const route = useRoute()
const rows = ref<any[]>([])
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const query = reactive({ keyword: '', status: '' })
const form = reactive<Record<string, string>>({})

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

async function load() {
  rows.value = await listMaster(type.value, query)
}

function reset() {
  query.keyword = ''
  query.status = ''
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

watch(type, load)
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
</style>
