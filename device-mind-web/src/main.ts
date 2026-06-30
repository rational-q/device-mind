import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import './style.css'

// 仅注册用到的图标，避免全量引入 200+ 个图标增加包体积
import {
  Odometer, Goods, Monitor, TrendCharts, Bell,
  Setting, Document, Switch, ChatDotSquare, Fold, Expand
} from '@element-plus/icons-vue'

const app = createApp(App)

for (const icon of [Odometer, Goods, Monitor, TrendCharts, Bell, Setting, Document, Switch, ChatDotSquare, Fold, Expand]) {
  app.component(icon.name!, icon)
}

app.use(createPinia())
app.use(router)
app.use(ElementPlus)
app.mount('#app')
