import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
// Pretendard ships with the app (OFL-1.1); only the glyph ranges a page uses are downloaded.
import 'pretendard/dist/web/variable/pretendardvariable-dynamic-subset.css'
import './styles/main.css'
import './styles/cottage.css'
import './styles/workspace.css'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.mount('#app')
