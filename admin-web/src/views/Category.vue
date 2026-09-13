<template>
  <div class="category-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>类目管理</span>
          <el-button type="danger" size="small" @click="showDialog(null)">+ 新增类目</el-button>
        </div>
      </template>
      <el-table :data="list" row-key="id" border stripe lazy :tree-props="{ children: 'children' }">
        <el-table-column prop="name" label="类目名称" min-width="180" />
        <el-table-column prop="code" label="编码" width="120" />
        <el-table-column label="层级" width="80">
          <template #default="{ row }">
            <el-tag :type="row.level === 1 ? 'danger' : row.level === 2 ? 'warning' : 'info'" size="small">
              Lv{{ row.level }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" align="center" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-switch v-model="row.enabled" active-color="#FF2E93" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="showDialog(row)">编辑</el-button>
            <el-button v-if="row.level < 3" link type="primary" @click="showDialog(null, row.id)">添加子类</el-button>
            <el-popconfirm title="确定删除？" @confirm="handleDelete(row)">
              <template #reference>
                <el-button link type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑类目' : '新增类目'" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="上级类目" v-if="!parentId">
          <el-tree-select
            v-model="form.parentId"
            :data="list"
            :props="{ label: 'name', value: 'id' }"
            placeholder="顶级类目（留空）"
            clearable
            style="width:100%"
          />
        </el-form-item>
        <el-form-item label="类目名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入类目名称" />
        </el-form-item>
        <el-form-item label="编码" prop="code">
          <el-input v-model="form.code" placeholder="请输入编码" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="form.icon" placeholder="图标类名" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="danger" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const parentId = ref(null)
const formRef = ref()

const list = ref([])

const form = reactive({
  parentId: null,
  name: '',
  code: '',
  sort: 0,
  icon: '',
})

const rules = {
  name: [{ required: true, message: '请输入类目名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入编码', trigger: 'blur' }],
}

function loadData() {
  list.value = [
    { id: 1, name: '手机数码', code: 'PHONE', level: 1, sort: 1, enabled: true, children: [
      { id: 11, name: '智能手机', code: 'SMARTPHONE', level: 2, sort: 1, enabled: true, parentId: 1 },
      { id: 12, name: '手机配件', code: 'PHONE_ACC', level: 2, sort: 2, enabled: true, parentId: 1 },
      { id: 13, name: '平板电脑', code: 'TABLET', level: 2, sort: 3, enabled: true, parentId: 1, children: [
        { id: 131, name: 'iPad', code: 'IPAD', level: 3, sort: 1, enabled: true, parentId: 13 },
        { id: 132, name: '安卓平板', code: 'ANDROID_TAB', level: 3, sort: 2, enabled: true, parentId: 13 },
      ]},
    ]},
    { id: 2, name: '家用电器', code: 'APPLIANCE', level: 1, sort: 2, enabled: true, children: [
      { id: 21, name: '大家电', code: 'BIG_APP', level: 2, sort: 1, enabled: true, parentId: 2 },
      { id: 22, name: '小家电', code: 'SMALL_APP', level: 2, sort: 2, enabled: true, parentId: 2 },
    ]},
    { id: 3, name: '服饰鞋包', code: 'FASHION', level: 1, sort: 3, enabled: true, children: [
      { id: 31, name: '女装', code: 'WOMEN', level: 2, sort: 1, enabled: true, parentId: 3 },
      { id: 32, name: '男装', code: 'MEN', level: 2, sort: 2, enabled: true, parentId: 3 },
    ]},
    { id: 4, name: '美妆个护', code: 'BEAUTY', level: 1, sort: 4, enabled: true },
    { id: 5, name: '食品生鲜', code: 'FOOD', level: 1, sort: 5, enabled: true },
  ]
}

function showDialog(row, pId) {
  parentId.value = pId || null
  if (row) {
    isEdit.value = true
    editId.value = row.id
    Object.assign(form, { parentId: row.parentId || null, name: row.name, code: row.code, sort: row.sort, icon: row.icon || '' })
  } else {
    isEdit.value = false
    editId.value = null
    Object.assign(form, { parentId: parentId.value, name: '', code: '', sort: 0, icon: '' })
  }
  dialogVisible.value = true
}

function handleSave() {
  formRef.value?.validate((valid) => {
    if (!valid) return
    ElMessage.success(isEdit.value ? '编辑成功' : '新增成功')
    dialogVisible.value = false
    loadData()
  })
}

function handleDelete(row) {
  ElMessage.success('已删除：' + row.name)
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
</style>
