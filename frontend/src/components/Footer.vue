<script setup lang="ts">
import VisitorInfoCard from '@/components/VisitorInfoCard.vue'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()
</script>

<template>
  <template v-if="appStore.visitorInfoCardEnabled">
    <!-- 信息条固定在底部，按收起状态的高度预留空间，避免挡住页面末尾的内容；展开后仍浮在内容上方。 -->
    <div class="h-14 shrink-0" aria-hidden="true" />
    <VisitorInfoCard />
  </template>
</template>
