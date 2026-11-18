import { createPinia } from 'pinia'
import { createApp } from 'vue'
import { i18n } from '@/i18n'
import { setupIconify } from '@/utils/iconify'
import { message } from '@/utils/message'
import App from './App.vue'
import router from './router'

import './styles/main.css'

window.$message = message

// 早期版本沿用上游项目的缓存键名，改名后清掉访客浏览器里残留的旧缓存。
try {
  for (const key of Object.keys(localStorage)) {
    if (key === 'simerald_finance_exchange_rates_cny_v1' || key.startsWith('komari-theme-emerald:'))
      localStorage.removeItem(key)
  }
}
catch {
  // 隐私模式等禁用存储时无需清理。
}

setupIconify().catch((err) => {
  console.warn('[main] iconify init failed', err)
})

const pinia = createPinia()
const app = createApp(App)

app.use(pinia)
app.use(i18n)
app.use(router)

app.mount('#app')
