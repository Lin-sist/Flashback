<script setup lang="ts">
import { reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useRecordStore, useTagStore } from '../../stores'
import { RecordType } from '../../types'
import { getToken, validateRecordContent } from '../../utils'

const recordStore = useRecordStore()
const tagStore = useTagStore()
const loading = ref(false)
const recordId = ref('')

const form = reactive({
  title: '',
  content: '',
  recordType: RecordType.FUTURE_LETTER,
  coreQuestion: '',
  unlockAt: '',
})

const onRecordTypeChange = (event: { detail: { value: number } }) => {
  const nextType = tagStore.recordTypeOptions[event.detail.value]
  if (nextType) {
    form.recordType = nextType.value
  }
}

const ensureLogin = () => {
  if (!getToken()) {
    uni.reLaunch({ url: '/pages/login/index' })
    return false
  }
  return true
}

onLoad((query) => {
  if (!ensureLogin()) {
    return
  }
  if (typeof query?.id === 'string') {
    recordId.value = query.id
  }
  tagStore.fetchTags()
})

const saveDraft = async () => {
  if (!validateRecordContent(form.content)) {
    uni.showToast({ title: 'Content is required', icon: 'none' })
    return
  }
  loading.value = true
  try {
    const payload = {
      title: form.title,
      content: form.content,
      recordType: form.recordType,
      coreQuestion: form.coreQuestion,
    }
    if (recordId.value) {
      await recordStore.updateDraft(recordId.value, payload)
    } else {
      await recordStore.createDraft(payload)
    }
    uni.showToast({ title: 'Draft saved', icon: 'success' })
  } finally {
    loading.value = false
  }
}

const sealRecord = async () => {
  if (!recordId.value) {
    uni.showToast({ title: 'Save draft first', icon: 'none' })
    return
  }
  const unlockAt = Date.parse(form.unlockAt)
  if (!unlockAt || unlockAt <= Date.now()) {
    uni.showToast({ title: 'Unlock time must be future', icon: 'none' })
    return
  }
  loading.value = true
  try {
    await recordStore.sealRecord({ id: recordId.value, unlockAt })
    uni.showToast({ title: 'Record sealed', icon: 'success' })
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <view class="page">
    <input v-model="form.title" class="input" placeholder="Title (optional)" />
    <textarea v-model="form.content" class="textarea" placeholder="Write your thoughts" />
    <picker :range="tagStore.recordTypeOptions" range-key="label" @change="onRecordTypeChange">
      <view class="input">Type: {{ form.recordType }}</view>
    </picker>
    <input v-model="form.coreQuestion" class="input" placeholder="Core question (optional)" />
    <input v-model="form.unlockAt" class="input" placeholder="Unlock datetime, e.g. 2026-12-31 18:00" />
    <button class="btn" :loading="loading" @tap="saveDraft">Save Draft</button>
    <button class="btn primary" :loading="loading" @tap="sealRecord">Seal Record</button>
  </view>
</template>

<style scoped>
.page {
  padding: 24rpx;
}

.input,
.textarea {
  width: 100%;
  background: #ffffff;
  border-radius: 12rpx;
  padding: 20rpx;
  margin-bottom: 18rpx;
  box-sizing: border-box;
}

.textarea {
  min-height: 320rpx;
}

.btn {
  margin-top: 8rpx;
  border-radius: 9999rpx;
}

.btn.primary {
  background: #0ea5e9;
  color: #ffffff;
}
</style>
