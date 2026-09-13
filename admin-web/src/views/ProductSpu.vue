<template>
  <div class="spu-page">
    <!-- 搜索栏 -->
    <el-card shadow="never" style="margin-bottom:16px">
      <el-form :model="search" inline>
        <el-form-item label="商品名称">
          <el-input v-model="search.name" placeholder="搜索商品" clearable style="width:200px" />
        </el-form-item>
        <el-form-item label="类目">
          <el-select v-model="search.category" placeholder="选择类目" clearable style="width:140px">
            <el-option label="手机数码" value="phone" />
            <el-option label="家用电器" value="appliance" />
            <el-option label="服饰鞋包" value="fashion" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="danger" @click="loadData">查询</el-button>
          <el-button @click="search = { name: '', category: '' }; loadData()">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>SPU 商品列表</span>
          <el-button type="danger" size="small" @click="showSpuDialog(null)">+ 新增商品</el-button>
        </div>
      </template>
      <el-table :data="list" border stripe>
        <el-table-column label="商品图片" width="80">
          <template #default>
            <div class="thumb">
              <el-icon :size="28" color="#FF2E93"><Picture /></el-icon>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="商品名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="category" label="类目" width="100" />
        <el-table-column prop="brand" label="品牌" width="100" />
        <el-table-column label="价格区间" width="120">
          <template #default="{ row }">¥{{ row.minPrice }} ~ ¥{{ row.maxPrice }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === '上架' ? 'success' : 'info'" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="showSpuDialog(row)">编辑</el-button>
            <el-button link type="primary" @click="showSkuDialog(row)">SKU 管理</el-button>
            <el-popconfirm title="确认上架/下架？" @confirm="toggleStatus(row)">
              <template #reference>
                <el-button link type="primary">
                  {{ row.status === '上架' ? '下架' : '上架' }}
                </el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="page"
          :page-size="10"
          :total="total"
          layout="total, prev, pager, next"
        />
      </div>
    </el-card>

    <!-- SPU 编辑弹窗 -->
    <el-dialog v-model="spuVisible" :title="spuEditId ? '编辑商品' : '新增商品'" width="800px" top="5vh">
      <el-form ref="spuFormRef" :model="spuForm" :rules="spuRules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="商品名称" prop="name">
              <el-input v-model="spuForm.name" placeholder="请输入商品名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="类目" prop="categoryId">
              <el-tree-select
                v-model="spuForm.categoryId"
                :data="categoryTree"
                :props="{ label: 'name', value: 'id' }"
                placeholder="选择类目"
                style="width:100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="品牌">
              <el-input v-model="spuForm.brand" placeholder="品牌名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序">
              <el-input-number v-model="spuForm.sort" :min="0" :max="9999" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 商品图片上传 -->
        <el-form-item label="商品图片">
          <el-upload
            action="#"
            list-type="picture-card"
            :auto-upload="false"
            :file-list="spuForm.images"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
        </el-form-item>

        <!-- 富文本描述 -->
        <el-form-item label="商品描述">
          <div class="editor-wrap">
            <div class="editor-toolbar">
              <el-button size="small" @click="execCmd('bold')"><b>B</b></el-button>
              <el-button size="small" @click="execCmd('italic')"><i>I</i></el-button>
              <el-button size="small" @click="execCmd('underline')"><u>U</u></el-button>
              <el-button size="small" @click="execCmd('insertUnorderedList')">列表</el-button>
              <el-button size="small" @click="insertImage">插入图片</el-button>
            </div>
            <div
              ref="editorRef"
              class="editor-content"
              contenteditable="true"
              v-html="spuForm.description"
              @input="onEditorInput"
              placeholder="请输入商品描述..."
            ></div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="spuVisible = false">取消</el-button>
        <el-button type="danger" @click="saveSpu">保存</el-button>
      </template>
    </el-dialog>

    <!-- SKU 管理弹窗 -->
    <el-dialog v-model="skuVisible" title="SKU 管理" width="700px">
      <template v-if="skuSpuName">
        <div class="sku-spu-name">商品：{{ skuSpuName }}</div>
        <el-table :data="skuList" border stripe>
          <el-table-column label="规格" prop="spec" width="160" />
          <el-table-column label="价格(元)" width="120">
            <template #default="{ row, $index }">
              <el-input-number v-model="row.price" :min="0.01" :precision="2" size="small" controls-position="right" style="width:110px" />
            </template>
          </el-table-column>
          <el-table-column label="库存" width="120">
            <template #default="{ row, $index }">
              <el-input-number v-model="row.stock" :min="0" size="small" controls-position="right" style="width:100px" />
            </template>
          </el-table-column>
          <el-table-column label="编码" prop="skuCode" min-width="120" />
          <el-table-column label="操作" width="80">
            <template #default="{ $index }">
              <el-popconfirm title="确认删除？" @confirm="skuList.splice($index, 1)">
                <template #reference>
                  <el-button link type="danger">删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
        <div style="margin-top:12px">
          <el-button size="small" @click="addSkuRow">+ 添加规格</el-button>
        </div>
      </template>
      <template #footer>
        <el-button @click="skuVisible = false">关闭</el-button>
        <el-button type="danger" @click="saveSku">保存 SKU</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Picture, Plus } from '@element-plus/icons-vue'

const search = reactive({ name: '', category: '' })
const list = ref([])
const page = ref(1)
const total = ref(0)

// SPU 弹窗
const spuVisible = ref(false)
const spuEditId = ref(null)
const spuFormRef = ref()
const editorRef = ref()
const categoryTree = ref([])
const spuForm = reactive({
  name: '',
  categoryId: null,
  brand: '',
  sort: 0,
  images: [],
  description: '',
})

const spuRules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择类目', trigger: 'change' }],
}

// SKU 弹窗
const skuVisible = ref(false)
const skuSpuName = ref('')
const skuList = ref([])

function loadData() {
  list.value = [
    { id: 1, name: '旗舰智能手机 5G全网通 高清拍照 12GB+256GB', category: '手机数码', brand: '小米', minPrice: 3999, maxPrice: 5999, status: '上架' },
    { id: 2, name: '轻薄笔记本电脑 14英寸 商务办公 学生本', category: '手机数码', brand: '联想', minPrice: 4599, maxPrice: 5299, status: '上架' },
    { id: 3, name: '智能4K电视 65英寸 全面屏 家庭影院', category: '家用电器', brand: '海信', minPrice: 3299, maxPrice: 4999, status: '下架' },
    { id: 4, name: '蓝牙降噪耳机 无线 长续航 HiFi音质', category: '手机数码', brand: '华为', minPrice: 599, maxPrice: 999, status: '上架' },
    { id: 5, name: '运动跑鞋 透气网面 轻便减震 男女同款', category: '服饰鞋包', brand: '李宁', minPrice: 299, maxPrice: 599, status: '上架' },
  ]
  total.value = list.value.length

  categoryTree.value = [
    { id: 1, name: '手机数码', children: [
      { id: 11, name: '智能手机' }, { id: 12, name: '手机配件' }, { id: 13, name: '平板电脑' },
    ]},
    { id: 2, name: '家用电器', children: [
      { id: 21, name: '大家电' }, { id: 22, name: '小家电' },
    ]},
    { id: 3, name: '服饰鞋包', children: [
      { id: 31, name: '女装' }, { id: 32, name: '男装' },
    ]},
  ]
}

function showSpuDialog(row) {
  if (row) {
    spuEditId.value = row.id
    Object.assign(spuForm, { name: row.name, categoryId: null, brand: row.brand, sort: 0, images: [], description: '<p>商品详情描述内容</p>' })
  } else {
    spuEditId.value = null
    Object.assign(spuForm, { name: '', categoryId: null, brand: '', sort: 0, images: [], description: '' })
  }
  spuVisible.value = true
}

function execCmd(cmd) {
  document.execCommand(cmd, false, null)
  editorRef.value?.focus()
}

function insertImage() {
  const url = prompt('请输入图片 URL：')
  if (url) {
    document.execCommand('insertImage', false, url)
  }
}

function onEditorInput() {
  spuForm.description = editorRef.value?.innerHTML || ''
}

function saveSpu() {
  spuFormRef.value?.validate((valid) => {
    if (!valid) return
    ElMessage.success(spuEditId.value ? '编辑成功' : '新增成功')
    spuVisible.value = false
    loadData()
  })
}

function showSkuDialog(row) {
  skuSpuName.value = row.name
  skuList.value = [
    { spec: '曜石黑 / 8GB+128GB', price: 3999, stock: 200, skuCode: row.id + '-BK-8' },
    { spec: '曜石黑 / 12GB+256GB', price: 4299, stock: 150, skuCode: row.id + '-BK-12' },
    { spec: '陶瓷白 / 12GB+256GB', price: 4399, stock: 180, skuCode: row.id + '-WH-12' },
    { spec: '渐变蓝 / 16GB+512GB', price: 4999, stock: 80, skuCode: row.id + '-BL-16' },
  ]
  skuVisible.value = true
}

function addSkuRow() {
  skuList.value.push({ spec: '', price: 0, stock: 0, skuCode: '' })
}

function saveSku() {
  ElMessage.success('SKU 保存成功')
  skuVisible.value = false
}

function toggleStatus(row) {
  row.status = row.status === '上架' ? '下架' : '上架'
  ElMessage.success(row.status === '上架' ? '已上架' : '已下架')
}

onMounted(loadData)
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.thumb {
  width: 50px;
  height: 50px;
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
.editor-wrap {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
}
.editor-toolbar {
  display: flex;
  gap: 4px;
  padding: 6px 8px;
  border-bottom: 1px solid #dcdfe6;
  background: #fafafa;
  flex-wrap: wrap;
}
.editor-content {
  min-height: 200px;
  padding: 12px;
  outline: none;
  line-height: 1.8;
  font-size: 14px;
}
.editor-content:empty::before {
  content: attr(placeholder);
  color: #ccc;
}
.sku-spu-name {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin-bottom: 12px;
  padding: 8px 12px;
  background: #f5f5f5;
  border-radius: 4px;
}
</style>
