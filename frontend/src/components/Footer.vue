<script setup lang="ts">
import VisitorInfoCard from '@/components/VisitorInfoCard.vue'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()
</script>

<template>
  <VisitorInfoCard v-if="appStore.visitorInfoCardEnabled" />
</template>
