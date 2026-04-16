<script setup lang="ts">
const props = withDefaults(
  defineProps<{
    modelValue: string
    placeholder?: string
  }>(),
  {
    placeholder: '搜索',
  }
)

const emit = defineEmits<{
  (event: 'update:modelValue', value: string): void
  (event: 'search', value: string): void
}>()

const onInput = (event: Event) => {
  const inputEvent = event as unknown as { detail?: { value?: string } }
  const value = inputEvent.detail?.value || ''
  emit('update:modelValue', value)
}

const onConfirm = () => {
  emit('search', props.modelValue)
}

const clear = () => {
  emit('update:modelValue', '')
  emit('search', '')
}
</script>

<template>
  <view class="search-bar">
    <text class="icon">⌕</text>
    <input
      class="input"
      :value="props.modelValue"
      :placeholder="props.placeholder"
      placeholder-class="placeholder"
      @input="onInput"
      @confirm="onConfirm"
    />
    <text v-if="props.modelValue" class="clear" @tap="clear">清空</text>
  </view>
</template>

<style scoped>
.search-bar {
  width: 100%;
  min-height: 84rpx;
  border-radius: var(--fb-radius-pill);
  background: var(--fb-color-surface);
  padding: 0 24rpx;
  display: flex;
  align-items: center;
  gap: 14rpx;
  box-shadow: var(--fb-shadow-soft);
}

.icon {
  color: var(--fb-color-text-muted);
  font-size: 28rpx;
}

.input {
  flex: 1;
  height: 84rpx;
  font-size: var(--fb-font-body-sub);
  color: var(--fb-color-text);
}

.placeholder {
  color: var(--fb-color-text-muted);
}

.clear {
  color: var(--fb-color-text-muted);
  font-size: var(--fb-font-meta);
}
</style>
