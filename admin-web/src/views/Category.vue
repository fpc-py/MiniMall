<template>
  <div class="category-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>类目管理</span>
          <div>
            <el-button size="small" @click="loadData">刷新</el-button>
            <el-button type="danger" size="small" @click="showDialog(null, 0)">+ 新增一级类目</el-button>
          </div>
        </div>
      </template>
      <el-table
        v-loading="loading"
        :data="tree"
        row-key="id"
        border
        default-expand-all
        :tree-props="{ children: 'children' }"
      >
        <el-table-column prop="name" label="类目名称" min-width="200" />
        <el-table-column label="图标/图片" width="90" align="center">
          <template #default="{ row }">
            <el-image v-if="row.image" :src="row.image" fit="cover" style="width:36px;height:36px;border-radius:4px" />
            <span v-else-if="row.icon">{{ row.icon }}</span>
            <span v-else class="empty-text">-</span>
          </template>
        </el-table-column>
        <el-table-column label="层级" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.level === 1 ? 'danger' : row.level === 2 ? 'warning' : 'info'" size="small">
              Lv{{ row.level }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              active-color="#FF2E93"
              size="small"
              @change="(val) => toggleStatus(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="showDialog(row)">编辑</el-button>
            <el-button v-if="row.level < 3" link type="primary" @click="showDialog(null, row.id)">添加子类</el-button>
            <el-popconfirm title="确定删除该分类？" @confirm="handleDelete(row)">
              <template #reference>
                <el-button link type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑类目' : '新增类目'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="上级类目">
          <el-tree-select
            v-model="form.parentId"
            :data="parentOptions"
            :props="{ label: 'name', value: 'id' }"
            placeholder="顶级类目"
            check-strictly
            clearable
            style="width:100%"
          />
        </el-form-item>
        <el-form-item label="类目名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入类目名称" />
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="form.icon" placeholder="图标名称/类名（可选）" />
        </el-form-item>
        <el-form-item label="图片">
          <el-input v-model="form.image" placeholder="图片 URL（可选）" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">显示</el-radio>
            <el-radio :value="0">隐藏</el-radio>
          </el-radio-group>
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
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getCategoryList, createCategory, updateCategory, deleteCategory } from '@/api/category'

const loading = ref(false)
const list = ref([])
const tree = ref([])

const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const editingId = ref(null)
const formRef = ref()

const form = reactive({
  parentId: null,
  name: '',
  icon: '',
  image: '',
  sort: 0,
  status: 1,
})

const rules = {
  name: [{ required: true, message: '请输入类目名称', trigger: 'blur' }],
}

const parentOptions = computed(() => tree.value)

function buildTree(flat) {
  const map = {}
  const roots = []
  flat.forEach((c) => { map[c.id] = { ...c, children: [] } })
  flat.forEach((c) => {
    const node = map[c.id]
    if (c.parentId && c.parentId !== 0 && map[c.parentId]) {
      map[c.parentId].children.push(node)
    } else {
      roots.push(node)
    }
  })
  const sortNodes = (arr) => {
    arr.sort((a, b) => (a.sort || 0) - (b.sort || 0))
    arr.forEach((n) => sortNodes(n.children))
  }
  sortNodes(roots)
  return roots
}

function findNode(id, nodes) {
  for (const n of nodes) {
    if (n.id === id) return n
    const found = findNode(id, n.children || [])
    if (found) return found
  }
  return null
}

async function loadData() {
  loading.value = true
  try {
    const res = await getCategoryList()
    list.value = res.data || []
    tree.value = buildTree(list.value)
  } finally {
    loading.value = false
  }
}

function showDialog(row, presetParentId) {
  if (row) {
    isEdit.value = true
    editingId.value = row.id
    Object.assign(form, {
      parentId: row.parentId && row.parentId !== 0 ? row.parentId : null,
      name: row.name,
      icon: row.icon || '',
      image: row.image || '',
      sort: row.sort || 0,
      status: row.status === 0 ? 0 : 1,
    })
  } else {
    isEdit.value = false
    editingId.value = null
    Object.assign(form, {
      parentId: presetParentId && presetParentId !== 0 ? presetParentId : null,
      name: '',
      icon: '',
      image: '',
      sort: 0,
      status: 1,
    })
  }
  dialogVisible.value = true
}

function resolveLevel(parentId) {
  if (!parentId) return 1
  const parent = findNode(parentId, tree.value)
  return parent ? Math.min((parent.level || 1) + 1, 3) : 2
}

function handleSave() {
  formRef.value?.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    const parentId = form.parentId || 0
    const payload = {
      name: form.name,
      parentId,
      level: resolveLevel(parentId),
      icon: form.icon,
      image: form.image,
      sort: form.sort,
      status: form.status,
    }
    try {
      if (isEdit.value) {
        await updateCategory({ id: editingId.value, ...payload })
        ElMessage.success('编辑成功')
      } else {
        await createCategory(payload)
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      loadData()
    } finally {
      submitting.value = false
    }
  })
}

async function toggleStatus(row, enabled) {
  await updateCategory({
    id: row.id,
    name: row.name,
    parentId: row.parentId,
    level: row.level,
    icon: row.icon,
    image: row.image,
    sort: row.sort,
    status: enabled ? 1 : 0,
  })
  ElMessage.success(enabled ? '已显示' : '已隐藏')
  loadData()
}

async function handleDelete(row) {
  await deleteCategory(row.id)
  ElMessage.success('已删除')
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.empty-text {
  color: #bbb;
}
</style>
