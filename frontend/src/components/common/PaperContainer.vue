<script setup lang="ts">
const props = withDefaults(
  defineProps<{
    warm?: boolean
    radius?: 'xl' | 'lg' | 'md' | 'sm'
    noPadding?: boolean
  }>(),
  {
    warm: false,
    radius: 'xl',
    noPadding: false,
  }
)
</script>

<template>
  <view
    class="paper"
    :class="[
      { warm: props.warm, 'no-padding': props.noPadding },
      `radius-${props.radius}`,
    ]"
  >
    <slot />
  </view>
</template>

<style scoped>
.paper {
  background: var(--fb-color-surface);
  box-shadow: var(--fb-shadow-soft);
  padding: var(--fb-space-inner);
}

.paper.warm {
  background: var(--fb-color-paper-warm);
}

.paper.no-padding {
  padding: 0;
}

.paper.radius-xl {
  border-radius: var(--fb-radius-xl);
}

.paper.radius-lg {
  border-radius: var(--fb-radius-lg);
}

.paper.radius-md {
  border-radius: var(--fb-radius-md);
}

.paper.radius-sm {
  border-radius: var(--fb-radius-sm);
}
</style>
