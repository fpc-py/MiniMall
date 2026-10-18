<template>
  <div class="shop-product-page">
    <el-card shadow="never" style="margin-bottom:16px">
      <el-form :model="search" inline>
        <el-form-item label="商品名称">
          <el-input v-model="search.keyword" placeholder="搜索我的商品" clearable style="width:220px" @keyup.enter="resetAndLoad" />
        </el-form-item>
        <el-form-item>
          <el-button type="danger" @click="resetAndLoad">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>我的商品</span>
          <el-button type="danger" size="small" @click="openCreate">+ 发布商品</el-button>
        </div>
      </template>
      <el-table :data="list" border stripe v-loading="loading">
        <el-table-column label="商品图片" width="90">
          <template #default="{ row }">
            <el-image v-if="row.mainImage" :src="row.mainImage" fit="cover" style="width:48px;height:48px;border-radius:6px" />
            <div v-else class="thumb"><el-icon :size="24" color="#FF2E93"><Picture /></el-icon></div>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="商品名称" min-width="220" show-overflow-tooltip />
        <el-table-column prop="brand" label="品牌" width="110" show-overflow-tooltip />
        <el-table-column prop="unit" label="单位" width="70" align="center" />
        <el-table-column label="价格区间" width="160">
          <template #default="{ row }">¥{{ formatMoney(row.minPrice) }} ~ ¥{{ formatMoney(row.maxPrice) }}</template>
        </el-table-column>
        <el-table-column prop="totalSales" label="销量" width="80" align="center" />
        <el-table-column label="操作" width="160" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="changeStatus(row, 1)">上架</el-button>
            <el-button link type="warning" @click="changeStatus(row, 0)">下架</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="page"
          :page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" title="发布商品" width="760px" top="5vh">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="商品名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入商品名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="副标题">
              <el-input v-model="form.subtitle" placeholder="卖点/副标题" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="类目" prop="categoryId">
              <el-tree-select
                v-model="form.categoryId"
                :data="categoryTree"
                :props="{ label: 'name', value: 'id' }"
                placeholder="选择类目"
                check-strictly
                style="width:100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="品牌">
              <el-input v-model="form.brand" placeholder="品牌名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="单位" prop="unit">
              <el-input v-model="form.unit" placeholder="如 件 / 台" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="主图 URL" prop="mainImage">
              <el-input v-model="form.mainImage" placeholder="上传后自动填充，或手动填写" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="商品图片">
          <div class="upload-wrap">
            <el-upload action="#" :show-file-list="false" :auto-upload="true" :http-request="doUpload" accept="image/*">
              <el-button :loading="uploading" type="primary" plain size="small">上传图片</el-button>
            </el-upload>
            <div class="img-list">
              <div v-for="(url, i) in form.images" :key="i" class="img-item">
                <el-image :src="url" fit="cover" style="width:64px;height:64px;border-radius:6px" />
                <el-icon class="img-del" @click="form.images.splice(i, 1)"><CircleCloseFilled /></el-icon>
              </div>
            </div>
          </div>
        </el-form-item>

        <el-form-item label="商品描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="商品详情描述（可选）" />
        </el-form-item>

        <el-form-item label="SKU 规格">
          <div style="width:100%">
            <el-table :data="form.skus" border stripe size="small">
              <el-table-column label="规格" min-width="160">
                <template #default="{ row }">
                  <el-input v-model="row.spec" placeholder="如 曜石黑/8GB+128GB" size="small" />
                </template>
              </el-table-column>
              <el-table-column label="售价(元)" width="130">
                <template #default="{ row }">
                  <el-input-number v-model="row.price" :min="0.01" :precision="2" size="small" controls-position="right" style="width:110px" />
                </template>
              </el-table-column>
              <el-table-column label="库存" width="120">
                <template #default="{ row }">
                  <el-input-number v-model="row.stock" :min="0" size="small" controls-position="right" style="width:100px" />
                </template>
              </el-table-column>
              <el-table-column label="SKU 编码" min-width="140">
                <template #default="{ row }">
                  <el-input v-model="row.skuCode" placeholder="编码" size="small" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="70" align="center">
                <template #default="{ $index }">
                  <el-button link type="danger" @click="form.skus.splice($index, 1)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-button size="small" style="margin-top:8px" @click="addSkuRow">+ 添加规格</el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="submitting" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Picture, CircleCloseFilled } from '@element-plus/icons-vue'
import { getShopProducts, createShopProduct, updateShopProductStatus } from '@/api/shop'
import { getCategoryTree } from '@/api/category'
import { uploadImage } from '@/api/product'

const search = reactive({ keyword: '' })
const list = ref([])
const page = ref(1)
const pageSize = 10
const total = ref(0)
const loading = ref(false)
const categoryTree = ref([])

const dialogVisible = ref(false)
const submitting = ref(false)
const uploading = ref(false)
const formRef = ref()

const form = reactive({
  name: '',
  subtitle: '',
  categoryId: null,
  brand: '',
  unit: '件',
  mainImage: '',
  description: '',
  images: [],
  skus: [],
})

const rules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择类目', trigger: 'change' }],
  unit: [{ required: true, message: '请输入单位', trigger: 'blur' }],
  mainImage: [{ required: true, message: '请上传或填写主图', trigger: 'blur' }],
}

function formatMoney(v) {
  if (v === null || v === undefined) return '0.00'
  return Number(v).toFixed(2)
}

async function loadCategories() {
  const res = await getCategoryTree()
  categoryTree.value = res.data || []
}

async function loadData() {
  loading.value = true
  try {
    const res = await getShopProducts({
      pageNum: page.value,
      pageSize,
      keyword: search.keyword || undefined,
    })
    list.value = res.data?.list || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function resetAndLoad() {
  page.value = 1
  loadData()
}

function addSkuRow() {
  form.skus.push({ spec: '', price: 0.01, stock: 0, skuCode: '' })
}

function openCreate() {
  Object.assign(form, {
    name: '',
    subtitle: '',
    categoryId: null,
    brand: '',
    unit: '件',
    mainImage: '',
    description: '',
    images: [],
    skus: [],
  })
  addSkuRow()
  dialogVisible.value = true
}

async function doUpload(opt) {
  uploading.value = true
  try {
    const res = await uploadImage(opt.file)
    if (res.data) {
      form.images.push(res.data)
      if (!form.mainImage) form.mainImage = res.data
    }
    opt.onSuccess?.(res.data)
  } catch (e) {
    opt.onError?.(e)
  } finally {
    uploading.value = false
  }
}

function handleSave() {
  formRef.value?.validate(async (valid) => {
    if (!valid) return
    if (!form.skus.length || form.skus.some((s) => !s.price || s.stock === null || s.stock === undefined)) {
      ElMessage.warning('请完整填写至少一个 SKU（售价与库存）')
      return
    }
    submitting.value = true
    try {
      await createShopProduct({
        name: form.name,
        subtitle: form.subtitle,
        categoryId: form.categoryId,
        brand: form.brand,
        unit: form.unit,
        mainImage: form.mainImage,
        description: form.description,
        images: form.images,
        skus: form.skus.map((s, i) => ({
          spec: s.spec,
          price: s.price,
          stock: s.stock,
          skuCode: s.skuCode || undefined,
          sort: i,
        })),
      })
      ElMessage.success('发布成功')
      dialogVisible.value = false
      loadData()
    } finally {
      submitting.value = false
    }
  })
}

async function changeStatus(row, status) {
  await updateShopProductStatus({ spuId: row.id, status })
  ElMessage.success(status === 1 ? '已上架' : '已下架')
  loadData()
}

onMounted(() => {
  loadCategories()
  loadData()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.thumb {
  width: 48px;
  height: 48px;
  background: #fafafa;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
.upload-wrap {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.img-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.img-item {
  position: relative;
}
.img-del {
  position: absolute;
  top: -6px;
  right: -6px;
  color: #f56c6c;
  cursor: pointer;
  font-size: 16px;
  background: #fff;
  border-radius: 50%;
}
</style>
