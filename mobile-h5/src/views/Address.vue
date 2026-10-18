<template>
  <div class="address-page">
    <van-nav-bar title="收货地址" left-arrow fixed placeholder @click-left="$router.back()" />

    <van-skeleton v-if="loading" title :row="4" style="padding: 16px" />

    <template v-else>
      <div class="address-list" v-if="list.length">
        <div class="address-card" v-for="addr in list" :key="addr.id">
          <div class="addr-top">
            <span class="addr-name">{{ addr.receiverName }}</span>
            <span class="addr-phone">{{ addr.receiverPhone }}</span>
            <van-tag v-if="addr.isDefault === 1" plain type="danger" size="small">默认</van-tag>
            <span v-if="addr.label" class="addr-label">{{ addr.label }}</span>
          </div>
          <div class="addr-detail">{{ addr.fullAddress }}</div>
          <div class="addr-actions">
            <div
              class="addr-action"
              :class="{ disabled: addr.isDefault === 1 }"
              @click="handleSetDefault(addr)"
            >
              <van-icon :name="addr.isDefault === 1 ? 'checked' : 'circle'" :color="addr.isDefault === 1 ? '#FF2E93' : '#ccc'" />
              设为默认
            </div>
            <div class="addr-action" @click="openEdit(addr)">
              <van-icon name="edit" /> 编辑
            </div>
            <div class="addr-action" @click="handleDelete(addr)">
              <van-icon name="delete-o" /> 删除
            </div>
          </div>
        </div>
      </div>

      <van-empty v-else description="暂无收货地址" />
    </template>

    <div class="bottom-bar">
      <van-button block round type="danger" color="#FF2E93" @click="openAdd">新增收货地址</van-button>
    </div>

    <!-- 新增/编辑弹窗 -->
    <van-popup v-model:show="showForm" position="bottom" round>
      <div class="form-header">
        <span @click="showForm = false">取消</span>
        <span class="title">{{ editingId ? '编辑地址' : '新增地址' }}</span>
        <span class="confirm" @click="submitForm">保存</span>
      </div>
      <div class="form-body">
        <van-field v-model="form.receiverName" label="收货人" placeholder="请输入收货人姓名" />
        <van-field v-model="form.receiverPhone" type="tel" label="手机号" placeholder="请输入手机号" maxlength="11" />
        <van-field v-model="form.province" label="省份" placeholder="如：广东省" />
        <van-field v-model="form.city" label="城市" placeholder="如：深圳市" />
        <van-field v-model="form.district" label="区/县" placeholder="选填" />
        <van-field v-model="form.detail" label="详细地址" placeholder="街道、门牌号等" />
        <van-field v-model="form.zipCode" label="邮编" placeholder="选填" />
        <van-field v-model="form.label" label="标签" placeholder="如：家 / 公司（选填）" />
        <van-cell title="设为默认地址" center>
          <template #right-icon>
            <van-switch v-model="form.isDefault" size="20" active-color="#FF2E93" />
          </template>
        </van-cell>
      </div>
    </van-popup>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { showToast, showConfirmDialog } from 'vant'
import {
  getAddresses,
  createAddress,
  updateAddress,
  deleteAddress,
  setDefaultAddress,
} from '@/api/address'

const loading = ref(true)
const list = ref([])

const showForm = ref(false)
const editingId = ref(null)
const saving = ref(false)

const emptyForm = () => ({
  receiverName: '',
  receiverPhone: '',
  province: '',
  city: '',
  district: '',
  detail: '',
  zipCode: '',
  label: '',
  isDefault: false,
})
const form = ref(emptyForm())

async function loadList() {
  loading.value = true
  try {
    const res = await getAddresses()
    list.value = res.data || []
  } catch (e) {
    list.value = []
  } finally {
    loading.value = false
  }
}

function openAdd() {
  editingId.value = null
  form.value = emptyForm()
  showForm.value = true
}

function openEdit(addr) {
  editingId.value = addr.id
  form.value = {
    receiverName: addr.receiverName || '',
    receiverPhone: addr.receiverPhone || '',
    province: addr.province || '',
    city: addr.city || '',
    district: addr.district || '',
    detail: addr.detail || '',
    zipCode: addr.zipCode || '',
    label: addr.label || '',
    isDefault: addr.isDefault === 1,
  }
  showForm.value = true
}

function validateForm() {
  const f = form.value
  if (!f.receiverName) {
    showToast('请输入收货人姓名')
    return false
  }
  if (!/^1[3-9]\d{9}$/.test(f.receiverPhone)) {
    showToast('请输入正确的手机号')
    return false
  }
  if (!f.province) {
    showToast('请输入省份')
    return false
  }
  if (!f.city) {
    showToast('请输入城市')
    return false
  }
  if (!f.detail) {
    showToast('请输入详细地址')
    return false
  }
  return true
}

async function submitForm() {
  if (!validateForm() || saving.value) return
  saving.value = true
  try {
    if (editingId.value) {
      await updateAddress(editingId.value, { ...form.value })
      showToast('修改成功')
    } else {
      await createAddress({ ...form.value })
      showToast('添加成功')
    }
    showForm.value = false
    await loadList()
  } catch (e) {
    showToast(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

function handleSetDefault(addr) {
  if (addr.isDefault === 1) return
  setDefaultAddress(addr.id)
    .then(() => {
      showToast('已设为默认')
      loadList()
    })
    .catch((e) => showToast(e.message || '操作失败'))
}

function handleDelete(addr) {
  showConfirmDialog({
    title: '提示',
    message: '确定要删除该地址吗？',
    confirmButtonColor: '#FF2E93',
  })
    .then(async () => {
      try {
        await deleteAddress(addr.id)
        showToast('已删除')
        await loadList()
      } catch (e) {
        showToast(e.message || '删除失败')
      }
    })
    .catch(() => {})
}

onMounted(loadList)
</script>

<style scoped>
.address-page {
  background: #f5f5f5;
  min-height: 100vh;
  padding-bottom: 80px;
}

.address-list {
  padding: 10px 12px;
}
.address-card {
  background: #fff;
  border-radius: 10px;
  padding: 14px;
  margin-bottom: 10px;
}
.addr-top {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
  flex-wrap: wrap;
}
.addr-name {
  font-size: 15px;
  font-weight: 600;
  color: #222;
}
.addr-phone {
  font-size: 13px;
  color: #666;
}
.addr-label {
  font-size: 11px;
  color: #FF2E93;
  border: 1px solid #FF2E93;
  border-radius: 4px;
  padding: 0 4px;
}
.addr-detail {
  font-size: 13px;
  color: #999;
  line-height: 1.5;
}
.addr-actions {
  display: flex;
  align-items: center;
  gap: 18px;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid #f5f5f5;
}
.addr-action {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #666;
}
.addr-action.disabled {
  color: #FF2E93;
}

.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 10px 16px;
  background: #fff;
  border-top: 1px solid #f0f0f0;
  z-index: 99;
}

.form-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  font-size: 14px;
  color: #666;
  border-bottom: 1px solid #f5f5f5;
}
.form-header .title {
  font-size: 16px;
  font-weight: 600;
  color: #222;
}
.form-header .confirm {
  color: #FF2E93;
  font-weight: 600;
}
.form-body {
  padding-bottom: 12px;
}
</style>
