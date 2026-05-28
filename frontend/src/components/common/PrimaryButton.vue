<script setup lang="ts">
const props = withDefaults(
  defineProps<{
    text: string
    disabled?: boolean
    loading?: boolean
    ghost?: boolean
  }>(),
  {
    disabled: false,
    loading: false,
    ghost: false,
  }
)

const emit = defineEmits<{
  (event: 'tap'): void
}>()

const onTap = () => {
  if (props.disabled || props.loading) {
    return
  }
  emit('tap')
}
</script>

<template>
  <button
    class="primary-btn"
    :class="{ ghost: props.ghost }"
    :disabled="props.disabled"
    :loading="props.loading"
    @tap="onTap"
  >
    {{ props.text }}
  </button>
</template>

<style scoped>
.primary-btn {
  width: 100%;
  border: none;
  border-radius: var(--fb-radius-pill);
  background: var(--fb-vermilion);
  color: #ffffff;
  font-size: 32rpx;
  font-weight: 400;
  letter-spacing: 0.18em;
  height: 88rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--fb-font-serif);
}

.primary-btn::after {
  border: none;
}

.primary-btn.ghost {
  background: transparent;
  color: var(--fb-vermilion);
  border: 1rpx solid var(--fb-vermilion);
}
</style>

