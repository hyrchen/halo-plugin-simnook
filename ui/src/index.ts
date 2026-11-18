import { definePlugin } from '@halo-dev/ui-shared'
import IconSimCard from '~icons/ri/sim-card-2-line'
import { markRaw } from 'vue'
import SettingField from './views/SettingField.vue'
import BackupView from './views/BackupView.vue'

export default definePlugin({
  components: { 'simnook-setting-field': markRaw(SettingField) },
  routes: [{
    parentName: 'ToolsRoot',
    route: {
      path: '/simnook',
      name: 'Simnook',
      component: () => import('./views/HomeView.vue'),
      meta: {
        title: 'SIMnook',
        searchable: true,
        permissions: ['plugin:simnook:manage'],
        menu: { name: 'SIMnook', icon: markRaw(IconSimCard), priority: 0 },
      },
    },
  }],
  extensionPoints: {
    // 备份恢复有独立的导出、预览和恢复操作，放在插件详情的自定义选项卡里，不经过设置表单的「保存」。
    'plugin:self:tabs:create': () => [{ id: 'simnook-backup', label: '备份恢复', component: markRaw(BackupView), permissions: ['plugin:simnook:manage'] }],
  },
})
