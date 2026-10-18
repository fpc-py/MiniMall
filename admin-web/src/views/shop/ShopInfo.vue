<template>
  <div class="shop-info-page" v-loading="loading">
    <el-card shadow="never" style="max-width:720px">
      <template #header><span>店铺信息</span></template>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="店铺名称">
          <span class="readonly-text">{{ shop.name || '-' }}</span>
          <el-tag v-if="shop.status !== undefined" size="small" style="margin-left:8px" :type="statusType(shop.status)">
            {{ statusText(shop.status) }}
          </el-tag>
        </el-form-item>
        <el-form-item label="联系人" prop="contactName">
          <el-input v-model="form.contactName" placeholder="请输入联系人姓名" />
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="form.contactPhone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="省份">
          <el-input v-model="form.province" placeholder="如 广东省" />
        </el-form-item>
        <el-form-item label="城市">
          <el-input v-model="form.city" placeholder="如 深圳市" />
        </el-form-item>
        <el-form-item label="区/县">
          <el-input v-model="form.district" placeholder="如 南山区" />
        </el-form-item>
        <el-form-item label="详细地址">
          <el-input v-model="form.address" placeholder="街道门牌号" />
        </el-form-item>
        <el-form-item label="店铺简介">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="店铺简介（可选）" />
        </el-form-item>
        <el-form-item>
          <el-button type="danger" :loading="submitting" @click="handleSave">保存</el-button>
          <el-button @click="loadData">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getShopInfo, updateShopInfo } from '@/api/shop'

const loading = ref(false)
const submitting = ref(false)
const formRef = ref()
const shop = reactive({ name: '', status: undefined })

const form = reactive({
  contactName: '',
  contactPhone: '',
  province: '',
  city: '',
  district: '',
  address: '',
  description: '',
})

const rules = {
  contactName: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
  contactPhone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }],
}

function statusText(status) {
  const map = { 0: '待审核', 1: '已上架', 2: '已下架', 3: '审核拒绝' }
  return map[status] || '未知'
}

function statusType(status) {
  const map = { 0: 'warning', 1: 'success', 2: 'info', 3: 'danger' }
  return map[status] || 'info'
}

async function loadData() {
  loading.value = true
  try {
    const res = await getShopInfo()
    const d = res.data || {}
    shop.name = d.name || ''
    shop.status = d.status
    Object.assign(form, {
      contactName: d.contactName || '',
      contactPhone: d.contactPhone || '',
      province: d.province || '',
      city: d.city || '',
      district: d.district || '',
      address: d.address || '',
      description: d.description || '',
    })
  } finally {
    loading.value = false
  }
}

function handleSave() {
  formRef.value?.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      await updateShopInfo({
        contactName: form.contactName,
        contactPhone: form.contactPhone,
        province: form.province,
        city: form.city,
        district: form.district,
        address: form.address,
        description: form.description,
      })
      ElMessage.success('保存成功')
      loadData()
    } finally {
      submitting.value = false
    }
  })
}

onMounted(loadData)
</script>

<style scoped>
.readonly-text {
  font-weight: 600;
  color: #333;
}
</style>
